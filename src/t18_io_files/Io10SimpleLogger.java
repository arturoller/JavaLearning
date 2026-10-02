package t18_io_files;

import helpers.Check;
import helpers.TempDir;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Prosty logger — dziennik zdarzeń programu zamiast System.out.println
 *        (logger = rejestrator zdarzeń, „dziennik pokładowy”; log = wpis w dzienniku; level = poziom ważności)
 *
 * W SKRÓCIE:
 *   Logger zapisuje komunikaty o tym, co robi program: z czasem, poziomem ważności i nazwą źródła, w wybranym
 *   miejscu (konsola, plik). Poziomy pozwalają jednym ustawieniem wyłączyć szczegóły albo je włączyć, gdy szukasz
 *   błędu. W tej lekcji piszemy własny, mały logger — żeby zrozumieć, co robią Logback i SLF4J w Spring Boot.
 *
 * ANALOGIA: dziennik pokładowy statku i czarna skrzynka samolotu.
 *   Kapitan nie krzyczy o każdej fali (TRACE), ale zapisuje kurs (INFO), dziwne zachowanie silnika (WARN) i awarie
 *   (ERROR), każdy wpis z godziną. Po wypadku nie pytasz „co kto pamięta”, tylko czytasz dziennik. System.out.println
 *   to krzyk na pokładzie: bez godziny, bez ważności, nie da się go ściszyć i nikt go potem nie przeczyta.
 *
 * JAK TO DZIAŁA:
 *   Poziomy (od najmniej do najbardziej ważnego):
 *     TRACE (ślad)    najdrobniejsze szczegóły, tylko do diagnozy
 *     DEBUG (debug)   informacje dla programisty: wartości zmiennych, decyzje
 *     INFO  (info)    normalne, ważne zdarzenia: „serwer wystartował”, „zamówienie złożone”
 *     WARN  (ostrz.)  coś dziwnego, ale program radzi sobie dalej
 *     ERROR (błąd)    operacja się nie udała, ktoś powinien zareagować
 *   Logger ma poziom MINIMALNY: wpisy poniżej niego są odrzucane. Minimalny INFO → przechodzą INFO, WARN, ERROR.
 *
 *   log.info("Zamówienie {} od {}", id, klient)
 *        │                │
 *        │                └ argumenty wstawiane za {} dopiero, gdy poziom jest włączony
 *        └ wiadomość jest sklejana tylko dla włączonego poziomu → szybciej
 *   wynik: "2026-05-04 12:00:00.000 [INFO ] Zamowienia — Zamówienie 17 od Ala"
 *
 *   Własny logger = filtr poziomu + format + miejsce docelowe (sink) + wstrzyknięty zegar (Clock).
 *   Wstrzyknięty zegar sprawia, że w testach czas jest stały i wydruk jest powtarzalny.
 *
 * SŁÓWKA:
 *   sink = ujście (dokąd trafia wpis); placeholder = miejsce na argument ("{}"); rolling = rotacja (zamiana pełnego pliku
 *   logu na stary i otwarcie nowego); handler = obsługa wpisu; formatter = formatowanie wpisu; append = dopisz
 *   na końcu; lazy = leniwy (obliczany dopiero, gdy potrzebny); mask = zamaskuj (ukryj część).
 *
 * ZOBACZ TEŻ: t17_datetime/DateTime01LocalDateTime (czas), t18_io_files/Io03WritingText (zapis i dopisywanie),
 *   t18_io_files/Io06Properties (konfiguracja jak application.properties), t18_io_files/Io11IoExceptions (wyjątki IO),
 *   t10_exceptions/Exceptions06ChainingWrapping (przyczyny wyjątków)
 * </pre>
 */
public class Io10SimpleLogger {

    public static void main(String[] args) throws IOException {
        // throws IOException = „rzuca IOException”: main przekazuje wyjątki IO wyżej
        title("Io10 — Prosty logger");

        // Pliki lekcji powstają w katalogu tymczasowym i znikają w finally (blok finally = zawsze się wykona).
        Path dir = TempDir.create("io10"); // create = utwórz
        try {
            levels();                                  // levels = poziomy
            basicLogger();                             // basic logger = podstawowy logger
            placeholders();                            // placeholders = miejsca na argumenty
            exceptions();                              // exceptions = wyjątki w logu
            injectedClock();                           // injected clock = wstrzyknięty zegar
            lazyMessages();                            // lazy messages = leniwe komunikaty
            logToFile(dir.resolve("s7"));              // log to file = log do pliku
            rollingFiles(dir.resolve("s8"));           // rolling files = rotacja plików
            javaUtilLogging();                         // java.util.logging = logger z JDK
            safeLogging();                             // safe logging = bezpieczne logowanie
            exercises(dir.resolve("cw"));              // exercises = ćwiczenia
        } finally {
            TempDir.deleteRecursively(dir);
        }
    }

    // =================================================================================================
    // Pomocnicze: wspólne dla lekcji o plikach
    // =================================================================================================

    /**
     * rel = ścieżka względem katalogu bazowego, zawsze z ukośnikiem "/".
     * Po co? Katalog tymczasowy ma losową nazwę (innego nie wypisujemy), a Windows pisze ścieżki z "\"
     * (np. "a\b.txt"), Linux z "/". Zamiana na "/" sprawia, że wydruk jest taki sam wszędzie.
     */
    private static String rel(Path base, Path p) {
        return base.relativize(p).toString().replace('\\', '/');
    }

    /** Akcja, która może rzucić IOException (zwykły Runnable nie może). */
    @FunctionalInterface
    private interface ThrowingIo {
        void run() throws IOException;
    }

    /** Wartość, której obliczenie może rzucić dowolny wyjątek (używana w ćwiczeniach). */
    @FunctionalInterface
    private interface Risky<T> {
        T get() throws Exception;
    }

    /**
     * ioFails = „IO zawodzi”. Wykonuje akcję i oczekuje wyjątku IO. Wypisuje nazwę klasy wyjątku i nazwę pliku,
     * a NIE komunikat (getMessage): komunikaty IO zawierają ścieżkę i tekst zależny od systemu.
     */
    private static void ioFails(String label, ThrowingIo action) {
        try {
            action.run();
            System.out.println("✘ " + label + " → NIE rzucono wyjątku (a spodziewaliśmy się go)");
        } catch (IOException e) {
            String file = "";
            if (e instanceof FileSystemException fse && fse.getFile() != null) { // instanceof ze wzorcem (Java 16+)
                file = " (plik: " + Path.of(fse.getFile()).getFileName() + ")";
            }
            System.out.println("✔ " + label + " → rzucono " + e.getClass().getSimpleName() + file);
        }
    }

    /** risky = „ryzykowne”: opakowuje wyjątki w IllegalStateException, żeby pasowało do Supplier w Check.equal. */
    private static <T> Supplier<T> risky(Risky<T> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                throw new IllegalStateException(e.getClass().getSimpleName(), e);
            }
        };
    }

    // =================================================================================================
    // Nasz logger (klasy zagnieżdżone, bo lekcje nie importują się nawzajem)
    // =================================================================================================

    /** Poziomy ważności. Kolejność deklaracji = kolejność ważności (ordinal rośnie). */
    enum LogLevel { TRACE, DEBUG, INFO, WARN, ERROR }

    /** Zegar, który przy każdym odczycie przesuwa się o stały krok — deterministyczny „upływ czasu” do demonstracji. */
    static final class StepClock extends Clock {
        private Instant now; // now = teraz (zmienny stan zegara)
        private final Duration step; // step = krok

        StepClock(Instant start, Duration step) {
            this.now = start;
            this.step = step;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this; // strefa jest stała: wystarczy do demonstracji
        }

        @Override
        public Instant instant() {
            Instant result = now;
            now = now.plus(step);
            return result;
        }
    }

    /**
     * SimpleLogger = prosty logger. Składa wpis: czas z Clock, poziom, nazwa, wiadomość (z "{}"), przyczyny wyjątku.
     * Gotową linię przekazuje do ujścia (sink), czyli do dowolnego Consumer: listy, konsoli, pliku.
     */
    static final class SimpleLogger {
        // Format czasu: rok-miesiąc-dzień godzina:minuta:sekunda.milisekundy. Locale.ROOT = bez ustawień regionalnych.
        private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT);
        private final String name;
        private final Clock clock;
        private final Consumer<String> sink;
        private LogLevel minLevel; // minLevel = poziom minimalny

        SimpleLogger(String name, LogLevel minLevel, Clock clock, Consumer<String> sink) {
            this.name = name;
            this.minLevel = minLevel;
            this.clock = clock;
            this.sink = sink;
        }

        void setLevel(LogLevel level) {
            this.minLevel = level;
        }

        boolean isEnabled(LogLevel level) { // isEnabled = czy włączony
            return level.compareTo(minLevel) >= 0;
        }

        /** fill = wypełnij: wstawia kolejne argumenty za kolejne "{}" (jak w SLF4J). Nadmiar "{}" zostaje bez zmian. */
        static String fill(String template, Object... args) {
            StringBuilder sb = new StringBuilder();
            int from = 0;
            int next = 0;
            while (true) {
                int at = template.indexOf("{}", from);
                if (at < 0 || next >= args.length) {
                    sb.append(template.substring(from));
                    break;
                }
                sb.append(template, from, at).append(args[next++]); // append(Object) zamienia null na "null"
                from = at + 2;
            }
            return sb.toString();
        }

        /** Główna metoda. Gdy ostatni argument to wyjątek, nie wstawiamy go za "{}", tylko dopisujemy jego opis. */
        void log(LogLevel level, String template, Object... args) {
            if (!isEnabled(level)) {
                return; // odrzucone: nic nie sklejamy, nic nie piszemy
            }
            Throwable error = null;
            Object[] params = args;
            if (args.length > 0 && args[args.length - 1] instanceof Throwable t) {
                error = t;
                params = Arrays.copyOf(args, args.length - 1); // copyOf = skopiuj bez ostatniego
            }
            StringBuilder line = new StringBuilder(header(level)).append(fill(template, params));
            // Z wyjątku bierzemy TYLKO klasę i komunikat (łańcuch przyczyn), bez numerów linii stosu.
            for (Throwable t = error; t != null; t = t.getCause()) {
                line.append("\n    ").append(t == error ? "wyjątek: " : "przyczyna: ")
                        .append(t.getClass().getName()).append(": ").append(t.getMessage());
            }
            sink.accept(line.toString());
        }

        /** Wersja leniwa: wiadomość powstaje (Supplier.get) tylko dla włączonego poziomu. */
        void log(LogLevel level, Supplier<String> message) {
            if (isEnabled(level)) {
                sink.accept(header(level) + message.get());
            }
        }

        private String header(LogLevel level) {
            String time = TIME.format(LocalDateTime.ofInstant(clock.instant(), clock.getZone()));
            return time + " [" + String.format(Locale.ROOT, "%-5s", level) + "] " + name + " — ";
        }

        void trace(String template, Object... args) { log(LogLevel.TRACE, template, args); }
        void debug(String template, Object... args) { log(LogLevel.DEBUG, template, args); }
        void info(String template, Object... args) { log(LogLevel.INFO, template, args); }
        void warn(String template, Object... args) { log(LogLevel.WARN, template, args); }
        void error(String template, Object... args) { log(LogLevel.ERROR, template, args); }
        void debug(Supplier<String> message) { log(LogLevel.DEBUG, message); }
    }

    /** Ujście do pliku: dopisuje linię na końcu pliku, za każdym razem otwierając i zamykając plik. */
    static final class FileSink implements Consumer<String> {
        private final Path file;

        FileSink(Path file) {
            this.file = file;
        }

        @Override
        public void accept(String line) {
            try {
                // writeString = zapisz napis (Java 11+). CREATE = utwórz, gdy brak; APPEND = dopisz na końcu.
                // Jawne "\n": rozmiar i treść są takie same wszędzie.
                Files.writeString(file, line + "\n", StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new UncheckedIOException(e); // Consumer nie może rzucać IOException (sprawdzanego)
            }
        }
    }

    /** Ujście z rotacją po rozmiarze: app.log, app.log.1 (poprzedni), app.log.2 (starszy)... */
    static final class RollingFileSink implements Consumer<String> {
        private final Path file;
        private final long maxBytes; // maxBytes = maksymalny rozmiar pliku w bajtach
        private final int keep;      // keep = ile starych plików zachować

        RollingFileSink(Path file, long maxBytes, int keep) {
            this.file = file;
            this.maxBytes = maxBytes;
            this.keep = keep;
        }

        private Path numbered(int i) { // numbered = ponumerowany: app.log → app.log.1
            return file.resolveSibling(file.getFileName() + "." + i);
        }

        @Override
        public void accept(String line) {
            try {
                byte[] data = (line + "\n").getBytes(StandardCharsets.UTF_8);
                if (Files.exists(file) && Files.size(file) + data.length > maxBytes) {
                    rotate();
                }
                Files.write(file, data, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private void rotate() throws IOException {
            Files.deleteIfExists(numbered(keep)); // najstarszy wypada z gry
            for (int i = keep - 1; i >= 1; i--) { // od końca, żeby niczego nie nadpisać przedwcześnie
                if (Files.exists(numbered(i))) {
                    Files.move(numbered(i), numbered(i + 1), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            Files.move(file, numbered(1), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Stały zegar używany w większości demonstracji: zawsze 4 maja 2026, godzina 12:00. */
    private static Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-05-04T12:00:00Z"), ZoneOffset.UTC); // fixed = stały
    }

    // =================================================================================================
    // 1. POZIOMY
    // =================================================================================================

    private static List<LogLevel> enabledFor(LogLevel min) {
        List<LogLevel> result = new ArrayList<>();
        for (LogLevel level : LogLevel.values()) { // values = wszystkie stałe enuma
            if (level.compareTo(min) >= 0) {
                result.add(level);
            }
        }
        return result;
    }

    /**
     * 1. Poziomy to enum. Wpis przechodzi, gdy jego poziom jest co najmniej taki jak minimalny.
     * Dzięki temu jedna zmiana (np. z INFO na DEBUG) włącza szczegóły bez dotykania kodu.
     */
    static void levels() {
        section("1. Poziomy ważności");

        show("wszystkie poziomy", Arrays.toString(LogLevel.values()));
        // WYNIK: wszystkie poziomy → [TRACE, DEBUG, INFO, WARN, ERROR]
        show("minimalny INFO przepuszcza", enabledFor(LogLevel.INFO));
        // WYNIK: minimalny INFO przepuszcza → [INFO, WARN, ERROR]
        show("minimalny DEBUG przepuszcza", enabledFor(LogLevel.DEBUG));
        // WYNIK: minimalny DEBUG przepuszcza → [DEBUG, INFO, WARN, ERROR]
        show("minimalny ERROR przepuszcza", enabledFor(LogLevel.ERROR));
        // WYNIK: minimalny ERROR przepuszcza → [ERROR]
        show("TRACE jest mniej ważny niż WARN", LogLevel.TRACE.compareTo(LogLevel.WARN) < 0);
        // WYNIK: TRACE jest mniej ważny niż WARN → true

        // PRZED: System.out.println("Zamówienie " + id)
        //   • bez godziny i bez ważności, • nie da się wyłączyć bez usuwania kodu, • leci na konsolę i znika,
        //   • w aplikacji serwerowej nikt konsoli nie ogląda.
        // PO: log.info("Zamówienie {}", id) — poziom, czas, nazwa źródła, miejsce docelowe do wyboru w konfiguracji.

        // DOBRA PRAKTYKA: wybieraj poziom według odbiorcy. ERROR = „człowiek musi zareagować”, WARN = „dziwne, ale
        //   obsłużone”, INFO = „co się dzieje w skrócie”, DEBUG/TRACE = „dla programisty przy szukaniu błędu”.
        //   Dlaczego: jeśli wszystko jest ERROR, nikt nie wierzy alarmom; jeśli wszystko INFO, log jest nieczytelny.
        // PUŁAPKA: błędy użytkownika (np. zły PIN) to nie ERROR aplikacji. Zalewanie logu błędami, które są normalnym
        //   działaniem, ukrywa prawdziwe awarie.
    }

    // =================================================================================================
    // 2. PODSTAWOWY LOGGER
    // =================================================================================================

    /**
     * 2. Logger z ujściem do listy: wpisy poniżej poziomu minimalnego znikają, reszta dostaje czas, poziom i nazwę.
     * Zegar jest stały, więc wydruk jest taki sam przy każdym uruchomieniu.
     */
    static void basicLogger() {
        section("2. Własny SimpleLogger");

        List<String> lines = new ArrayList<>(); // lines = linie; lista jest naszym „ujściem”
        SimpleLogger log = new SimpleLogger("Zamowienia", LogLevel.INFO, fixedClock(), lines::add);

        log.trace("najdrobniejszy szczegół");
        log.debug("szczegół dla programisty");
        log.info("start przetwarzania");
        log.warn("brak adresu e-mail klienta");
        log.error("nie udało się zapisać zamówienia");
        for (String line : lines) {
            System.out.println(line);
        }
        // WYNIK: 2026-05-04 12:00:00.000 [INFO ] Zamowienia — start przetwarzania
        // WYNIK: 2026-05-04 12:00:00.000 [WARN ] Zamowienia — brak adresu e-mail klienta
        // WYNIK: 2026-05-04 12:00:00.000 [ERROR] Zamowienia — nie udało się zapisać zamówienia
        show("liczba wpisów (TRACE i DEBUG odrzucone)", lines.size());
        // WYNIK: liczba wpisów (TRACE i DEBUG odrzucone) → 3

        lines.clear(); // clear = wyczyść
        log.setLevel(LogLevel.DEBUG); // zmiana poziomu „w locie”, bez zmiany kodu logowania
        log.debug("teraz widać szczegóły");
        System.out.println(lines.get(0));
        // WYNIK: 2026-05-04 12:00:00.000 [DEBUG] Zamowienia — teraz widać szczegóły

        // Poziom jest wyrównany do 5 znaków ("INFO " ma spację), żeby kolumny w pliku były równe.
        // W prawdziwym programie nazwa loggera to zwykle pełna nazwa klasy (np. pl.firma.sklep.ZamowieniaService),
        // a poziomy ustawia się osobno dla pakietów: pakiet pl.firma.sklep na DEBUG, reszta na INFO.

        // DOBRA PRAKTYKA: loger jako pole klasy (jedno na klasę), nazwany od klasy. Dlaczego: z nazwy w linii od razu
        //   wiadomo, skąd pochodzi wpis, a poziom da się ustawić dla jednej klasy bez ruszania reszty.
    }

    // =================================================================================================
    // 3. MIEJSCA NA ARGUMENTY
    // =================================================================================================

    /**
     * 3. Miejsca "{}" jak w SLF4J. Wiadomość składamy dopiero dla włączonego poziomu, a typy (liczby, obiekty)
     * zamienia na tekst sam logger.
     */
    static void placeholders() {
        section("3. Miejsca na argumenty {}");

        show("jedno {}", SimpleLogger.fill("Zamówienie {} przyjęte", 17));
        // WYNIK: jedno {} → Zamówienie 17 przyjęte
        show("dwa {}", SimpleLogger.fill("Klient {} kupił {} sztuk", "Ala", 3));
        // WYNIK: dwa {} → Klient Ala kupił 3 sztuk
        show("null", SimpleLogger.fill("Wartość: {}", (Object) null));
        // WYNIK: null → Wartość: null
        show("za mało argumentów", SimpleLogger.fill("a={} b={}", 1));
        // WYNIK: za mało argumentów → a=1 b={}
        show("za dużo argumentów", SimpleLogger.fill("a={}", 1, 2, 3));
        // WYNIK: za dużo argumentów → a=1
        show("lista jako argument", SimpleLogger.fill("Produkty: {}", List.of("kawa", "herbata")));
        // WYNIK: lista jako argument → Produkty: [kawa, herbata]

        List<String> lines = new ArrayList<>();
        SimpleLogger log = new SimpleLogger("Sklep", LogLevel.INFO, fixedClock(), lines::add);
        log.info("Klient {} kupił {} sztuk za {} zł", "Zażółć", 3, 59.97);
        System.out.println(lines.get(0));
        // WYNIK: 2026-05-04 12:00:00.000 [INFO ] Sklep — Klient Zażółć kupił 3 sztuk za 59.97 zł

        // PRZED: log.info("Klient " + imie + " kupił " + ile + " sztuk")  — sklejanie PRZED sprawdzeniem poziomu.
        // PO:    log.info("Klient {} kupił {} sztuk", imie, ile)           — sklejanie dopiero dla włączonego poziomu.
        // Dlaczego: gdy poziom jest wyłączony (np. DEBUG na produkcji), napis z plusami i tak by powstał i poszedł
        //   do kosza. Przy milionach wywołań to realny koszt. Poza tym szablon jest czytelniejszy.

        // PUŁAPKA: liczba "{}" musi zgadzać się z liczbą argumentów. Nadmiar jest po cichu ignorowany (jak wyżej),
        //   więc literówka w logu nie zgłosi błędu — widać ją dopiero w pliku.
        // PUŁAPKA: format liczb (59.97 czy 59,97) zależy od toString, a nie od ustawień języka. Gdy zależy Ci na
        //   formacie, sformatuj wartość sam (String.format(Locale.ROOT, ...)) przed wywołaniem.
    }

    // =================================================================================================
    // 4. WYJĄTKI W LOGU
    // =================================================================================================

    /**
     * 4. Ostatni argument typu Throwable (wyjątek) logger dopisuje jako klasę i komunikat (z łańcuchem przyczyn).
     * Prawdziwe loggery zapisują cały stos wywołań; my pomijamy go, bo numery linii zmieniałyby wydruk lekcji.
     */
    static void exceptions() {
        section("4. Wyjątki w logu");

        List<String> lines = new ArrayList<>();
        SimpleLogger log = new SimpleLogger("Import", LogLevel.INFO, fixedClock(), lines::add);

        try {
            Integer.parseInt("abc"); // parseInt = zamień napis na liczbę (rzuci NumberFormatException)
        } catch (NumberFormatException e) {
            // Opakowujemy w nasz wyjątek z kontekstem (zobacz t10_exceptions/Exceptions06ChainingWrapping).
            log.error("Nie udało się wczytać pliku {}", "dane.csv", new IllegalStateException("linia 3 jest niepoprawna", e));
        }
        for (String line : lines) {
            System.out.println(line);
        }
        // WYNIK: 2026-05-04 12:00:00.000 [ERROR] Import — Nie udało się wczytać pliku dane.csv
        // WYNIK: wyjątek: java.lang.IllegalStateException: linia 3 jest niepoprawna
        // WYNIK: przyczyna: java.lang.NumberFormatException: For input string: "abc"

        // Co robi prawdziwy logger (Logback): zapisuje wiadomość, a pod nią CAŁY stos wywołań wraz z łańcuchem
        // „Caused by:”. To najcenniejsza informacja przy szukaniu błędu — zawsze przekazuj wyjątek jako argument.

        // PUŁAPKA: log.error("Błąd: " + e.getMessage()) gubi klasę wyjątku, przyczynę i stos. Często zostaje samo
        //   „null” albo „abc”, z którego nic nie wynika. Przekaż cały wyjątek: log.error("Błąd", e).
        // PUŁAPKA: e.printStackTrace() wypisuje na System.err — poza logiem, bez godziny i poziomu; w aplikacji
        //   serwerowej ginie. Do tej lekcji też nie pasuje: weryfikator traktuje wydruk na System.err jako błąd.
        // PUŁAPKA: „zaloguj i rzuć dalej” (catch → log.error → throw e) daje ten sam błąd w logu kilka razy
        //   (na każdej warstwie). Zasada: wyjątek logujesz w JEDNYM miejscu, zwykle na szczycie (zobacz
        //   t18_io_files/Io11IoExceptions); niżej dodajesz kontekst i rzucasz dalej.
        // PUŁAPKA: pusty catch (połknięcie wyjątku) jest jeszcze gorszy niż brak logu: błąd istnieje, a nikt o nim nie wie.
    }

    // =================================================================================================
    // 5. WSTRZYKNIĘTY ZEGAR
    // =================================================================================================

    /**
     * 5. Logger nie woła Instant.now() ani LocalDateTime.now() na twardo. Dostaje Clock z zewnątrz, więc w testach
     * podstawiamy zegar stały albo przesuwany o krok.
     */
    static void injectedClock() {
        section("5. Wstrzyknięty zegar (Clock)");

        List<String> lines = new ArrayList<>();
        Clock ticking = new StepClock(Instant.parse("2026-05-04T12:00:00Z"), Duration.ofMillis(250));
        SimpleLogger log = new SimpleLogger("Zegar", LogLevel.INFO, ticking, lines::add);
        log.info("pierwszy");
        log.info("drugi");
        log.info("trzeci");
        for (String line : lines) {
            System.out.println(line);
        }
        // WYNIK: 2026-05-04 12:00:00.000 [INFO ] Zegar — pierwszy
        // WYNIK: 2026-05-04 12:00:00.250 [INFO ] Zegar — drugi
        // WYNIK: 2026-05-04 12:00:00.500 [INFO ] Zegar — trzeci

        // W produkcji podajesz Clock.systemUTC() albo Clock.systemDefaultZone() (czas rzeczywisty), w testach
        // Clock.fixed(...) — i wydruk jest zawsze taki sam. Gdybyśmy w lekcji wypisywali „prawdziwy” czas, żaden
        // zapis WYNIK nie mógłby się zgadzać.

        // DOBRA PRAKTYKA: przyjmuj Clock jako parametr (konstruktor), zamiast wołać now() w środku klasy.
        //   Dlaczego: kod zależny od zegara staje się testowalny (t17_datetime/DateTime01LocalDateTime).
        // DOBRA PRAKTYKA: w logach serwerowych używaj jednej strefy, najlepiej UTC, i zapisuj ją w formacie.
        //   Dlaczego: porównywanie logów z różnych serwerów i zmiana czasu letniego/zimowego nie zamieni się w zagadkę.
    }

    // =================================================================================================
    // 6. LENIWE KOMUNIKATY
    // =================================================================================================

    private static int expensiveCalls; // ile razy policzyliśmy „drogą” wartość

    /** expensive = drogi (kosztowny): udaje wolne obliczenie, np. zbudowanie opisu wielkiego obiektu. */
    private static String expensive() {
        expensiveCalls++;
        return "wynik drogiego obliczenia";
    }

    /** Obiekt, którego toString (zamiana na tekst) liczymy. */
    static final class Costly {
        static int toStringCalls;

        @Override
        public String toString() {
            toStringCalls++;
            return "obiekt";
        }
    }

    /**
     * 6. Koszt logowania wyłączonego poziomu. Samo "log.debug(...)" przy wyłączonym DEBUG jest tanie, ale jeśli
     * argument to wywołanie drogiej metody, Java policzy go ZAWSZE, bo argumenty liczą się przed wejściem do metody.
     * Rozwiązanie: Supplier (lambda) — liczona tylko wtedy, gdy poziom jest włączony.
     */
    static void lazyMessages() {
        section("6. Leniwe komunikaty (Supplier)");

        List<String> lines = new ArrayList<>();
        SimpleLogger log = new SimpleLogger("Leniwy", LogLevel.INFO, fixedClock(), lines::add); // DEBUG wyłączony

        expensiveCalls = 0;
        log.debug("dane: " + expensive()); // sklejanie plusem: expensive() wołane, choć DEBUG wyłączony
        show("po sklejaniu plusem — wywołań expensive()", expensiveCalls);
        // WYNIK: po sklejaniu plusem — wywołań expensive() → 1

        expensiveCalls = 0;
        log.debug(() -> "dane: " + expensive()); // Supplier: lambda nie została wykonana
        show("po Supplier — wywołań expensive()", expensiveCalls);
        // WYNIK: po Supplier — wywołań expensive() → 0

        Costly.toStringCalls = 0;
        log.debug("dane: {}", new Costly()); // sam toString jest odroczony do czasu, aż poziom będzie włączony
        show("po {} — wywołań toString()", Costly.toStringCalls);
        // WYNIK: po {} — wywołań toString() → 0
        note("Miejsce {} odracza toString(), ale sam argument (tu: new Costly()) i tak powstał. Wołanie metody w argumencie wykona się zawsze.");
        // WYNIK: ℹ Miejsce {} odracza toString(), ale sam argument (tu: new Costly()) i tak powstał. Wołanie metody w argumencie wykona się zawsze.

        log.setLevel(LogLevel.DEBUG);
        expensiveCalls = 0;
        log.debug(() -> "dane: " + expensive()); // teraz poziom włączony: lambda się wykona (raz)
        show("DEBUG włączony — wywołań expensive()", expensiveCalls);
        // WYNIK: DEBUG włączony — wywołań expensive() → 1
        System.out.println(lines.get(lines.size() - 1));
        // WYNIK: 2026-05-04 12:00:00.000 [DEBUG] Leniwy — dane: wynik drogiego obliczenia

        // Ten sam cel osiągniesz strażnikiem: if (log.isEnabled(LogLevel.DEBUG)) { log.debug(...drogie...); }.
        // W SLF4J strażnik wygląda tak: if (log.isDebugEnabled()) { log.debug("dane: {}", drogie()); }, a od wersji 2.0
        // jest też interfejs fluent: log.atDebug().addArgument(() -> drogie()).log("dane: {}") — Supplier jako argument.

        // DOBRA PRAKTYKA: do prostych wartości używaj {}; do wywołań kosztownych metod — Supplier albo strażnik
        //   isEnabled. Dlaczego: unikasz pracy, której wynik i tak wyląduje w koszu.
        // PUŁAPKA: nie wkładaj do logu wyrażeń z efektem ubocznym (np. licznik++, pobranie z kolejki). Przy wyłączonym
        //   poziomie nie wykonają się i program zacznie zachowywać się inaczej niż przy włączonym.
    }

    // =================================================================================================
    // 7. LOG DO PLIKU
    // =================================================================================================

    /**
     * 7. Ujście do pliku: każdy wpis dopisujemy na końcu (APPEND). Przy drugim uruchomieniu programu log się
     * wydłuża zamiast zaczynać od zera. Zawsze UTF-8, żeby polskie litery przetrwały (Windows domyślnie bywa inny).
     */
    static void logToFile(Path base) throws IOException {
        section("7. Log do pliku (dopisywanie)");
        Files.createDirectory(base);
        Path logFile = base.resolve("app.log");

        SimpleLogger first = new SimpleLogger("Start", LogLevel.INFO, fixedClock(), new FileSink(logFile));
        first.info("program uruchomiony");
        first.warn("Zażółć gęślą jaźń — polskie litery w logu");
        for (String line : Files.readAllLines(logFile, StandardCharsets.UTF_8)) { // readAllLines = wczytaj wszystkie linie
            System.out.println(line);
        }
        // WYNIK: 2026-05-04 12:00:00.000 [INFO ] Start — program uruchomiony
        // WYNIK: 2026-05-04 12:00:00.000 [WARN ] Start — Zażółć gęślą jaźń — polskie litery w logu

        // „Drugie uruchomienie programu”: nowy logger, ten sam plik. Plik NIE jest nadpisywany.
        SimpleLogger second = new SimpleLogger("Start", LogLevel.INFO, fixedClock(), new FileSink(logFile));
        second.info("program uruchomiony ponownie");
        show("liczba linii po drugim uruchomieniu", Files.readAllLines(logFile, StandardCharsets.UTF_8).size());
        // WYNIK: liczba linii po drugim uruchomieniu → 3

        // PUŁAPKA: samo APPEND bez CREATE nie utworzy pliku. Gdy go brak, dostajesz NoSuchFileException.
        Path missing = base.resolve("nie-ma.log");
        ioFails("APPEND bez CREATE na brakującym pliku", () -> Files.writeString(missing, "x\n", StandardCharsets.UTF_8,
                StandardOpenOption.APPEND));
        // WYNIK: ✔ APPEND bez CREATE na brakującym pliku → rzucono NoSuchFileException (plik: nie-ma.log)
        // Dlaczego? Gdy podajesz własne opcje, domyślne (CREATE, TRUNCATE_EXISTING) przestają obowiązywać.

        show("rel(base, logFile)", rel(base, logFile));
        // WYNIK: rel(base, logFile) → app.log

        // Nasze ujście otwiera i zamyka plik przy KAŻDYM wpisie: proste i bezpieczne (nic nie zostaje otwarte,
        // więc w Windows plik da się usunąć), ale wolne. Prawdziwe loggery trzymają otwarty BufferedWriter,
        // zapisują porcjami (flush co jakiś czas) i pracują w osobnym wątku, żeby zapis nie spowalniał programu.
        // Cena: przy nagłym zatrzymaniu programu ostatnie wpisy mogą nie dotrzeć na dysk (lekcja Io03WritingText:
        // dane są w buforze, dopóki nie zrobisz flush albo close).

        // DOBRA PRAKTYKA: kodowanie UTF-8 ustaw jawnie (tu: StandardCharsets.UTF_8). Dlaczego: domyślne kodowanie
        //   w Javie 17 zależy od systemu (windows-1250 w polskim Windows), więc log z ą, ę, ł mógłby się zepsuć.
        // DOBRA PRAKTYKA: log piszesz do plików poza katalogiem projektu (np. /var/log/aplikacja albo katalog logs
        //   w .gitignore). Dlaczego: logi rosną, mogą zawierać dane wrażliwe i nie należą do repozytorium.
    }

    // =================================================================================================
    // 8. ROTACJA PLIKÓW
    // =================================================================================================

    /**
     * 8. Log rośnie bez końca, więc po przekroczeniu rozmiaru zmieniamy nazwy: app.log → app.log.1 → app.log.2,
     * a najstarszy plik usuwamy. Dzięki temu logi nigdy nie zapchają dysku.
     */
    static void rollingFiles(Path base) throws IOException {
        section("8. Rotacja po rozmiarze");
        Files.createDirectory(base);
        Path logFile = base.resolve("app.log");

        // Każda linia "wpis NN" ma 7 znaków + "\n" = 8 bajtów. Limit 30 bajtów mieści 3 linie (24), czwarta by go przekroczyła.
        // Zapisujemy surowe linie bez nagłówka (consumer wprost), żeby rozmiary były łatwe do policzenia.
        RollingFileSink sink = new RollingFileSink(logFile, 30, 2); // zachowujemy 2 stare pliki
        for (int i = 1; i <= 10; i++) {
            sink.accept(String.format(Locale.ROOT, "wpis %02d", i)); // %02d = liczba dwucyfrowa z zerem z przodu
        }

        List<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.list(base)) { // strumień z Files.list trzeba zamknąć (try-with-resources)
            stream.forEach(files::add);
        }
        files.sort((a, b) -> rel(base, a).compareTo(rel(base, b))); // kolejność z Files.list jest przypadkowa
        for (Path file : files) {
            List<String> content = Files.readAllLines(file, StandardCharsets.UTF_8);
            System.out.println(rel(base, file) + " → " + content);
        }
        // WYNIK: app.log → [wpis 10]
        // WYNIK: app.log.1 → [wpis 07, wpis 08, wpis 09]
        // WYNIK: app.log.2 → [wpis 04, wpis 05, wpis 06]
        note("Wpisy 01-03 zniknęły: to najstarszy plik, który wypadł z rotacji (zachowujemy tylko 2 stare).");
        // WYNIK: ℹ Wpisy 01-03 zniknęły: to najstarszy plik, który wypadł z rotacji (zachowujemy tylko 2 stare).

        // Kolejność w rotate(): najpierw kasujemy najstarszy, potem przesuwamy od końca (1→2, żeby 1 zwolnić),
        // na końcu bieżący plik staje się „.1”. Gdybyśmy przesuwali od początku, nadpisalibyśmy dane.
        // W Windows przeniesienie pliku, który ktoś trzyma otwarty, kończy się błędem — nasze ujście zamyka plik
        // po każdym wpisie, więc to działa. Prawdziwy logger przed rotacją zamyka bieżący plik.

        // W praktyce: Logback (SizeAndTimeBasedRollingPolicy: rotacja po rozmiarze i po dniu, kompresja starych
        // plików do .gz, limit łącznego rozmiaru), java.util.logging (FileHandler z limitem i liczbą plików).
        // DOBRA PRAKTYKA: zawsze ustaw rotację i limit. Dlaczego: log bez limitu w końcu zapcha dysk serwera i zatrzyma
        //   całą aplikację — a zdarza się to najczęściej w nocy i w weekend.
    }

    // =================================================================================================
    // 9. JAVA.UTIL.LOGGING
    // =================================================================================================

    /** Obsługa wpisów (handler) piszący na System.out. Wszystkie wpisy trafiają na standardowe wyjście, NIGDY na System.err. */
    static final class StdoutHandler extends Handler {
        @Override
        public void publish(LogRecord record) { // publish = opublikuj; LogRecord = pojedynczy wpis
            if (isLoggable(record)) { // isLoggable = czy przechodzi poziom i filtr
                System.out.println(getFormatter().format(record));
            }
        }

        @Override
        public void flush() {
            System.out.flush();
        }

        @Override
        public void close() {
            // nic do zamknięcia: System.out zostaje otwarty
        }
    }

    /** Formatowanie wpisu: bez czasu (żeby wydruk był powtarzalny), z poziomem, nazwą loggera i wiadomością. */
    static final class PlainFormatter extends java.util.logging.Formatter {
        @Override
        public String format(LogRecord record) {
            String text = "[" + record.getLevel().getName() + "] " + record.getLoggerName() + " — " + formatMessage(record);
            if (record.getThrown() != null) { // getThrown = pobierz wyjątek
                text += " | wyjątek: " + record.getThrown().getClass().getName() + ": " + record.getThrown().getMessage();
            }
            return text;
        }
    }

    /**
     * 9. W JDK jest gotowy logger: java.util.logging (JUL). Poziomy mają inne nazwy, a miejsca na argumenty to {0}, {1}.
     * Domyślnie pisze na konsolę przez System.err, dlatego tu wyłączamy rodzica i dodajemy własny handler.
     */
    static void javaUtilLogging() {
        section("9. java.util.logging (JUL) z JDK");

        // Odpowiedniki: TRACE=FINEST, DEBUG=FINE, INFO=INFO, WARN=WARNING, ERROR=SEVERE (jeszcze FINER i CONFIG).
        show("INFO ma wagę", Level.INFO.intValue());
        // WYNIK: INFO ma wagę → 800
        show("FINE ma wagę", Level.FINE.intValue());
        // WYNIK: FINE ma wagę → 500
        show("SEVERE ma wagę", Level.SEVERE.intValue());
        // WYNIK: SEVERE ma wagę → 1000

        Logger logger = Logger.getLogger("t18.io10.demo"); // getLogger = pobierz logger o danej nazwie
        // Trzymamy odwołanie do loggera w zmiennej: menedżer logowania trzyma loggery „słabo” i ustawienia mogłyby zniknąć.
        logger.setUseParentHandlers(false); // false = nie przekazuj wpisów „rodzicowi” (korzeń drukuje na System.err!)
        Handler handler = new StdoutHandler();
        handler.setFormatter(new PlainFormatter());
        logger.addHandler(handler);
        try {
            show("poziom nie ustawiony (dziedziczy po korzeniu)", logger.getLevel() == null);
            // WYNIK: poziom nie ustawiony (dziedziczy po korzeniu) → true
            logger.setLevel(Level.INFO);
            logger.info("start");
            // WYNIK: [INFO] t18.io10.demo — start
            logger.fine("to jest za mało ważne: nie zobaczysz");
            logger.setLevel(Level.FINE);
            logger.fine("teraz FINE jest widoczny");
            // WYNIK: [FINE] t18.io10.demo — teraz FINE jest widoczny
            logger.log(Level.WARNING, "Użytkownik {0} ma {1} punktów", new Object[] {"Ala", 5});
            // WYNIK: [WARNING] t18.io10.demo — Użytkownik Ala ma 5 punktów
            logger.log(Level.SEVERE, "Awaria zapisu", new IllegalStateException("brak miejsca"));
            // WYNIK: [SEVERE] t18.io10.demo — Awaria zapisu | wyjątek: java.lang.IllegalStateException: brak miejsca
            logger.fine(() -> "komunikat budowany leniwo (Supplier, Java 8+)");
            // WYNIK: [FINE] t18.io10.demo — komunikat budowany leniwo (Supplier, Java 8+)
        } finally {
            logger.removeHandler(handler); // sprzątamy po sobie: loger JUL jest globalny dla całego programu
            handler.close();
        }

        // Dlaczego setUseParentHandlers(false)? Każdy logger ma „rodzica”, a korzeń ma domyślny ConsoleHandler,
        // który pisze na System.err (nie na System.out!). Bez tego wpisy poszłyby podwójnie: do naszego handlera
        // i na System.err — a weryfikator lekcji uznaje wydruk na System.err za błąd uruchomienia.

        // PUŁAPKA: JUL używa składni MessageFormat: {0}, {1} — nie {}. Zapis "{}" zostałby bez zmian. Liczby formatuje
        //   według ustawień regionalnych (np. 1000 jako "1 000" albo "1,000"), więc ten sam log wygląda różnie.
        // PUŁAPKA: domyślny poziom (INFO) pochodzi z konfiguracji JDK; logger.fine(...) nic nie pokaże, dopóki nie
        //   ustawisz poziomu ORAZ poziomu handlera (nasz handler ma domyślnie ALL, więc wystarczył poziom loggera).

        // W prawdziwych projektach: SLF4J (interfejs, „fasada”) + Logback (implementacja). Spring Boot dołącza je sam
        // (spring-boot-starter-logging). Kod: private static final Logger log = LoggerFactory.getLogger(Klasa.class);
        // log.info("Zamówienie {}", id). Poziomy ustawia się w application.properties (t18_io_files/Io06Properties):
        //   logging.level.root=INFO   i   logging.level.pl.firma.sklep=DEBUG
        // Dodatkowo: MDC (kontekst doklejany do każdego wpisu, np. numer żądania), logi w JSON, zapis asynchroniczny.
    }

    // =================================================================================================
    // 10. BEZPIECZNE LOGOWANIE
    // =================================================================================================

    /** maskAllButLast = zamaskuj wszystko oprócz ostatnich znaków: ("12345678901", 2) → "*********01". */
    private static String maskAllButLast(String value, int visible) {
        if (value.length() <= visible) {
            return "*".repeat(value.length()); // repeat = powtórz (Java 11+)
        }
        return "*".repeat(value.length() - visible) + value.substring(value.length() - visible);
    }

    /** oneLine = jedna linia: wstawia podkreślenia w miejsce znaków nowej linii. */
    private static String oneLine(String text) {
        return text.replace('\n', '_').replace('\r', '_');
    }

    /**
     * 10. Czego NIE wpisywać do logów i jak się bronić. Logi czyta wielu ludzi (administratorzy, programiści,
     * zewnętrzne systemy zbierające logi), przechowywane są długo i trudno je potem „odkręcić”.
     */
    static void safeLogging() {
        section("10. Bezpieczne logowanie: sekrety i fałszywe wpisy");

        List<String> lines = new ArrayList<>();
        SimpleLogger log = new SimpleLogger("Konto", LogLevel.INFO, fixedClock(), lines::add);

        // 1) Dane wrażliwe maskujemy albo w ogóle nie logujemy (przykładowy, zmyślony numer):
        log.info("Założono konto dla numeru {}", maskAllButLast("12345678901", 2));
        // 2) Dane od użytkownika mogą zawierać znaki nowej linii: „wstrzyknięcie wpisu do logu” (log injection).
        String hostile = "ala\n2026-05-04 12:00:00.000 [INFO ] Konto — ZALOGOWANO JAKO ADMIN"; // hostile = wrogi
        log.info("Próba logowania użytkownika {}", oneLine(hostile));
        for (String line : lines) {
            System.out.println(line);
        }
        // WYNIK: 2026-05-04 12:00:00.000 [INFO ] Konto — Założono konto dla numeru *********01
        // WYNIK: 2026-05-04 12:00:00.000 [INFO ] Konto — Próba logowania użytkownika ala_2026-05-04 12:00:00.000 [INFO ] Konto — ZALOGOWANO JAKO ADMIN
        show("wpisów w logu (a nie 3)", lines.size());
        // WYNIK: wpisów w logu (a nie 3) → 2

        // Czego nie logować: haseł, tokenów i kluczy API, numerów kart, PESEL-u, pełnych danych osobowych, treści
        // wiadomości prywatnych. W Polsce i UE log z danymi osobowymi podlega RODO (przechowywanie, usuwanie, dostęp).
        // PUŁAPKA: log.info("Użytkownik: {}", user) wypisze wszystko, co ma toString() — także hasło, jeśli ktoś je tam
        //   dopisał. Obiekty z danymi wrażliwymi niech mają toString() bez tych pól (rekord wypisuje WSZYSTKIE składowe!).
        // PUŁAPKA: bez oneLine znak nowej linii z danych użytkownika pozwala sfałszować wpis (drugi wiersz „z godziną”).
        //   Prawdziwe loggery oferują do tego kodowanie albo format JSON (nowa linia staje się \\n w napisie).

        // Poziomy w produkcji: zwykle INFO (czasem WARN dla bibliotek hałaśliwych). DEBUG włączasz tymczasowo, na
        // wybranej klasie, gdy szukasz błędu — i wyłączasz. TRACE prawie nigdy. Poziom da się zmienić bez restartu
        // (np. Spring Boot Actuator), co jest ogromną zaletą w porównaniu z println.
        // DOBRA PRAKTYKA: loguj ZDARZENIA z kontekstem (kto, co, jaki identyfikator), a nie „doszło tutaj”.
        //   Dlaczego: w logu nie masz debuggera — wpis musi sam opowiedzieć, co się stało.
        // DOBRA PRAKTYKA: wpis nie powinien zakładać, że ktoś zna kod. Dlaczego: czytać go będzie administrator o 3 w nocy.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Poziomy: TRACE < DEBUG < INFO < WARN < ERROR; poziom minimalny odrzuca mniej ważne wpisy.
     *   • Logger = filtr poziomu + format (czas, poziom, nazwa, wiadomość) + ujście (konsola, plik) + Clock.
     *   • Miejsca {} (SLF4J) — wiadomość składana dopiero dla włączonego poziomu; JUL używa {0}, {1}.
     *   • Wyjątek przekazuj jako ostatni argument (log.error("opis", e)), nie e.getMessage(); loguj w jednym miejscu.
     *   • Clock wstrzykiwany z zewnątrz → powtarzalny wynik w testach.
     *   • Argumenty są liczone ZAWSZE: kosztowne wartości przez Supplier albo strażnik isEnabled.
     *   • Plik logu: UTF-8 jawnie, APPEND razem z CREATE, rotacja po rozmiarze i limit liczby plików.
     *   • JUL: setUseParentHandlers(false) + własny Handler/Formatter; konsola JUL pisze na System.err.
     *   • W praktyce: SLF4J + Logback (Spring Boot ma je domyślnie), poziomy w application.properties.
     *   • Nie loguj haseł, tokenów, PESEL-u; maskuj; odfiltruj nowe linie z danych użytkownika.
     *
     * PYTANIA KONTROLNE:
     *   1. Podaj trzy powody, dla których logger jest lepszy niż System.out.println.
     *   2. Co wypisze:  SimpleLogger log = new SimpleLogger("A", LogLevel.WARN, zegar, lista::add);
     *                   log.info("x"); log.warn("y"); log.error("z");
     *                   System.out.println(lista.size());  ?
     *   3. Dlaczego log.debug("dane: " + drogaMetoda()) bywa kosztowne, mimo że DEBUG jest wyłączony?
     *      Podaj dwa sposoby, jak to naprawić.
     *   4. ZNAJDŹ BŁĄD:  catch (IOException e) { log.error("Błąd: " + e.getMessage()); }
     *   5. Co wypisze:  SimpleLogger.fill("a={} b={} c={}", 1, 2);  ?
     *   6. ZNAJDŹ BŁĄD:  Files.writeString(plikLogu, linia + "\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
     *      — program działał w testach (plik był), a na świeżym serwerze zawodzi.
     *   7. Po co przy java.util.logging wołamy setUseParentHandlers(false) i co by się stało, gdybyśmy tego nie zrobili?
     *   8. Dlaczego do logu nie wpisujemy surowego tekstu od użytkownika bez usunięcia znaków nowej linii?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** Licznik wywołań „drogiej” metody w ćwiczeniu 3. */
    static final class Counter {
        int calls;

        String expensive() {
            calls++;
            return "dane";
        }
    }

    static void exercises(Path base) throws IOException {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Files.createDirectory(base);

        // Przygotowanie pliku logu dla ćwiczenia 2: wpisy zapisane naszym loggerem.
        Path logFile = base.resolve("przyklad.log");
        SimpleLogger prepared = new SimpleLogger("Dysk", LogLevel.DEBUG, fixedClock(), new FileSink(logFile));
        prepared.info("start");
        prepared.warn("Dysk prawie pełny: {}%", 91);
        prepared.error("Awaria zapisu", new IllegalStateException("brak miejsca"));
        prepared.debug("koniec");

        Check.equal("ćw. 1: miejsca {}", List.of("Witaj Ala, masz 5 punktów", "x i {}"),
                risky(() -> List.of(exercise1("Witaj {}, masz {} punktów", "Ala", 5), exercise1("{} i {}", "x"))));
        Check.equal("ćw. 2: ostrzeżenia i błędy z pliku", List.of("Dysk prawie pełny: 91%", "Awaria zapisu"),
                risky(() -> exercise2(logFile)));
        Check.equal("ćw. 3: leniwe logowanie", List.of(0, 1), risky(() -> {
            Counter off = new Counter();
            Counter on = new Counter();
            SimpleLogger info = new SimpleLogger("L", LogLevel.INFO, fixedClock(), line -> { });
            SimpleLogger debug = new SimpleLogger("L", LogLevel.DEBUG, fixedClock(), line -> { });
            return List.of(exercise3(info, off), exercise3(debug, on));
        }));
        Check.equal("ćw. 4: rotacja plików", List.of("app.log.1=C", "app.log.2=B"), risky(() -> rotatedState(base.resolve("r1"), false)));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of("Witaj Ala, masz 5 punktów", "x i {}"),
                risky(() -> List.of(solution1("Witaj {}, masz {} punktów", "Ala", 5), solution1("{} i {}", "x"))));
        Check.equal("ćw. 2 (wzorzec)", List.of("Dysk prawie pełny: 91%", "Awaria zapisu"),
                risky(() -> solution2(logFile)));
        Check.equal("ćw. 3 (wzorzec)", List.of(0, 1), risky(() -> {
            Counter off = new Counter();
            Counter on = new Counter();
            SimpleLogger info = new SimpleLogger("L", LogLevel.INFO, fixedClock(), line -> { });
            SimpleLogger debug = new SimpleLogger("L", LogLevel.DEBUG, fixedClock(), line -> { });
            return List.of(solution3(info, off), solution3(debug, on));
        }));
        Check.equal("ćw. 4 (wzorzec)", List.of("app.log.1=C", "app.log.2=B"), risky(() -> rotatedState(base.resolve("r2"), true)));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * Przygotowuje katalog (app.log = "C", app.log.1 = "B", app.log.2 = "A"), wywołuje rotację na zachowanie 2 starych
     * plików i zwraca posortowaną listę "nazwa=treść" (pliki, które istnieją po rotacji).
     */
    private static List<String> rotatedState(Path dir, boolean reference) throws IOException {
        Files.createDirectory(dir);
        Path log = dir.resolve("app.log");
        Files.writeString(log, "C", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("app.log.1"), "B", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("app.log.2"), "A", StandardCharsets.UTF_8);
        if (reference) {
            solution4(log, 2);
        } else {
            exercise4(log, 2);
        }
        List<String> result = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            for (Path p : stream.toList()) { // toList = zbierz do listy niezmiennej (Java 16+)
                result.add(rel(dir, p) + "=" + Files.readString(p, StandardCharsets.UTF_8)); // readString = wczytaj napis (Java 11+)
            }
        }
        result.sort(null); // null = porządek naturalny (alfabetyczny)
        return result;
    }

    /**
     * ĆWICZENIE 1 (łatwe): zaimplementuj wstawianie argumentów za "{}" (bez używania SimpleLogger.fill!).
     * Kolejne "{}" dostają kolejne argumenty; gdy argumentów zabraknie, pozostałe "{}" zostają bez zmian.
     * Podpowiedź: pętla z template.indexOf("{}", od) i StringBuilder; argument dołącz przez append(Object).
     */
    static String exercise1(String template, Object... args) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): plik zawiera linie logu w formacie z tej lekcji, np.
     * "2026-05-04 12:00:00.000 [WARN ] Dysk — Dysk prawie pełny: 91%". Wczytaj plik (UTF-8) i zwróć same wiadomości
     * (tekst po " — ") z wpisów o poziomie WARN i ERROR, w kolejności z pliku. Linie ciągu dalszego (zaczynają się
     * od spacji, to opis wyjątku) pomiń.
     * Podpowiedź: poziom jest między '[' a ']'; użyj trim(); wiadomość zaczyna się po indexOf(" — ") + 3.
     */
    static List<String> exercise2(Path logFile) throws IOException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ kosztowne logowanie na leniwe, tak żeby przy wyłączonym DEBUG metoda
     * counter.expensive() NIE była wywołana, a przy włączonym — wywołana raz. Zwróć counter.calls.
     * <pre>{@code
     * // PRZED:
     * log.debug("wynik: " + counter.expensive());
     * // PO: wiadomość w lambdzie (Supplier)
     * }</pre>
     * Podpowiedź: log.debug(() -> ...). Oczekiwane wartości: 0 dla loggera INFO, 1 dla loggera DEBUG.
     */
    static int exercise3(SimpleLogger log, Counter counter) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zaimplementuj rotację plików logu. Dla pliku app.log i keep = 2: usuń najstarszy
     * app.log.2, przenieś app.log.1 → app.log.2, a app.log → app.log.1. Po rotacji pliku app.log nie ma
     * (nowy powstanie przy następnym wpisie). Pliki mogą nie istnieć (np. nie ma jeszcze app.log.1) — nie rzucaj wtedy wyjątku.
     * Podpowiedź: nazwa numerowana to file.resolveSibling(file.getFileName() + "." + i); kolejność: od najstarszego,
     * Files.deleteIfExists i Files.move z REPLACE_EXISTING; przesuwaj od końca (i = keep - 1 w dół do 1).
     */
    static void exercise4(Path file, int keep) throws IOException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(String template, Object... args) {
        StringBuilder sb = new StringBuilder();
        int from = 0;
        int next = 0;
        while (true) {
            int at = template.indexOf("{}", from);
            if (at < 0 || next >= args.length) {
                sb.append(template.substring(from));
                return sb.toString();
            }
            sb.append(template, from, at).append(args[next++]);
            from = at + 2;
        }
    }

    static List<String> solution2(Path logFile) throws IOException {
        List<String> result = new ArrayList<>();
        for (String line : Files.readAllLines(logFile, StandardCharsets.UTF_8)) {
            if (line.startsWith(" ") || !line.contains("[")) {
                continue; // continue = pomiń tę linię (ciąg dalszy wyjątku)
            }
            String level = line.substring(line.indexOf('[') + 1, line.indexOf(']')).trim();
            if (level.equals("WARN") || level.equals("ERROR")) {
                result.add(line.substring(line.indexOf(" — ") + 3));
            }
        }
        return result;
    }

    static int solution3(SimpleLogger log, Counter counter) {
        log.debug(() -> "wynik: " + counter.expensive());
        return counter.calls;
    }

    static void solution4(Path file, int keep) throws IOException {
        Path oldest = file.resolveSibling(file.getFileName() + "." + keep);
        Files.deleteIfExists(oldest);
        for (int i = keep - 1; i >= 1; i--) {
            Path from = file.resolveSibling(file.getFileName() + "." + i);
            if (Files.exists(from)) {
                Files.move(from, file.resolveSibling(file.getFileName() + "." + (i + 1)), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        if (Files.exists(file)) {
            Files.move(file, file.resolveSibling(file.getFileName() + ".1"), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Np.: (a) wpis ma czas, poziom i źródło; (b) poziom można zmienić w konfiguracji bez zmiany kodu, a mniej
     *      ważne wpisy wyłączyć; (c) miejsce docelowe (plik, rotacja, system zbierania logów) wybiera się osobno;
     *      (d) wyjątki zapisują się razem ze stosem; (e) println idzie tylko na konsolę, która na serwerze znika.
     *   2. 2 — poziom minimalny WARN przepuszcza WARN i ERROR (info jest odrzucone).
     *   3. Argumenty liczą się PRZED wejściem do metody, więc drogaMetoda() jest wołana zawsze (i napis jest sklejany),
     *      choć logger potem wpis odrzuci. Naprawa: Supplier (log.debug(() -> "dane: " + drogaMetoda())) albo strażnik
     *      if (log.isEnabled(DEBUG)) {...}.
     *   4. getMessage() gubi klasę wyjątku, przyczynę i stos (często zostaje sam tekst albo null). Poprawnie:
     *      log.error("Nie udało się odczytać pliku {}", nazwa, e) — wyjątek jako ostatni argument.
     *   5. "a=1 b=2 c={}" — argumentów jest za mało, więc ostatnie {} zostaje bez zmian.
     *   6. APPEND bez CREATE: gdy pliku nie ma, Files.writeString rzuca NoSuchFileException. Podając własne opcje
     *      tracimy domyślne (CREATE), więc trzeba dodać StandardOpenOption.CREATE.
     *   7. Korzeń loggerów ma domyślny ConsoleHandler piszący na System.err. Bez wyłączenia rodzica każdy wpis trafiłby
     *      i do naszego handlera, i na System.err (podwójnie, a w tej lekcji — błąd uruchomienia).
     *   8. Znak nowej linii pozwala sfałszować wpis (drugą linię z własną godziną i poziomem) i oszukać osobę, która
     *      analizuje log (log injection). Dlatego usuwamy lub kodujemy te znaki.
     */
    // </editor-fold>
}
