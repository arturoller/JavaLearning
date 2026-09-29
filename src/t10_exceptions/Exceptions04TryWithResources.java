package t10_exceptions;

import helpers.Check;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: try-with-resources — automatyczne zamykanie zasobów (Java 7+)
 *        (resource = zasób; AutoCloseable = automatycznie zamykalny; suppressed = stłumiony)
 *
 * W SKRÓCIE:
 *   Plik, połączenie z bazą, gniazdo sieciowe — to ZASOBY: po użyciu trzeba je zamknąć (close), inaczej „wyciekają”
 *   (system ma limit otwartych plików/połączeń). try (Zasób z = ...) { ... } zamyka zasób AUTOMATYCZNIE po wyjściu
 *   z bloku — normalnym czy przez wyjątek. Kilka zasobów zamyka się w odwrotnej kolejności. Jeśli błąd poleci
 *   i w bloku, i przy zamykaniu — główny wyjątek zostaje, a ten z close() jest do niego dołączony jako „stłumiony”.
 *
 * ANALOGIA: drzwi z samozamykaczem.
 *   Zwykłe drzwi (finally) trzeba pamiętać zamknąć — łatwo zapomnieć, zwłaszcza gdy wybiegasz w pośpiechu (wyjątek).
 *   Drzwi z samozamykaczem (try-with-resources) zamykają się same, jakkolwiek wyjdziesz.
 *
 * JAK TO DZIAŁA:
 *   try (BufferedReader in = new BufferedReader(...)) {   ← zasób musi implementować AutoCloseable
 *       ... in.readLine() ...
 *   }                                                      ← tu Java sama wywoła in.close()
 *   Kompilator zamienia to na try/finally z close() i obsługą wyjątków stłumionych (addSuppressed).
 *
 * SŁÓWKA:
 *   resource = zasób; close = zamknij; AutoCloseable = automatycznie zamykalny (interfejs z metodą close);
 *   Closeable = zamykalny (starszy interfejs dla IO); suppressed = stłumiony; leak = wyciek; open = otwórz;
 *   reader = czytnik; buffered = buforowany; idempotent = idempotentny (wielokrotne wywołanie = jak jedno).
 *
 * ZOBACZ TEŻ: t10_exceptions/Exceptions01Basics (finally), t18_io_files/Io02ReadingText (czytanie plików),
 *             t16_streams/Streams02Creation (Files.lines też trzeba zamykać), t10_exceptions/Exceptions03MultiCatch.
 * </pre>
 */
public class Exceptions04TryWithResources {

    /**
     * Resource = zasób do ćwiczeń. Zapisuje do dziennika (log), kiedy jest otwierany, używany i zamykany.
     * close() nie deklaruje throws — dzięki temu użycie nie wymaga łapania wyjątku sprawdzanego.
     */
    static final class Resource implements AutoCloseable {
        private final String name;
        private final List<String> log;
        private boolean closed;

        Resource(String name, List<String> log) {
            this.name = name;
            this.log = log;
            log.add("otwieram " + name);
        }

        /** use = użyj. Użycie zamkniętego zasobu to błąd stanu obiektu. */
        void use(String action) {
            if (closed) {
                throw new IllegalStateException("Zasób " + name + " jest już zamknięty");
            }
            log.add(name + ": " + action);
        }

        @Override
        public void close() {
            if (!closed) {                    // idempotentne: drugie close() nic nie robi
                closed = true;
                log.add("zamykam " + name);
            }
        }
    }

    /** FailingResource = zasób, którego zamykanie się nie udaje. Do pokazania wyjątków stłumionych. */
    static final class FailingResource implements AutoCloseable {
        private final String name;

        FailingResource(String name) {
            this.name = name;
        }

        String name() {
            return name;
        }

        @Override
        public void close() {
            throw new IllegalStateException("błąd przy zamykaniu " + name);
        }
    }

    public static void main(String[] args) {
        title("Exceptions04 — try-with-resources");

        beforeManualFinally();      // before: manual finally = przed: ręczne finally
        afterTryWithResources();    // after: try-with-resources = po: automatyczne zamykanie
        severalResources();         // several resources = kilka zasobów
        suppressedExceptions();     // suppressed exceptions = wyjątki stłumione
        java9EffectivelyFinal();    // Java 9: effectively final = zasób zadeklarowany wcześniej
        realReader();               // real reader = prawdziwy czytnik (BufferedReader)
        returningClosedResource();  // returning closed resource = zwracanie zamkniętego zasobu
        exercises();                // exercises = ćwiczenia
    }

    /** printLog = wypisz dziennik, każdy wpis w osobnej linii z wcięciem. */
    static void printLog(List<String> log) {
        for (String entry : log) {
            System.out.println("   " + entry);
        }
    }

    // =================================================================================================
    // 1. PRZED: ręczne zamykanie w finally
    // =================================================================================================

    /** 1. PRZED Javą 7: zmienna przed try, null-check i close() w finally. Dużo kodu i łatwo o błąd. */
    static void beforeManualFinally() {
        section("1. PRZED: ręczne zamykanie w finally");

        List<String> log = new ArrayList<>();
        Resource r = null;
        try {
            r = new Resource("plik", log);
            r.use("czytam");
        } finally {
            if (r != null) {                  // konstruktor mógł rzucić wyjątek — wtedy r == null
                r.close();
            }
        }
        printLog(log);
        // WYNIK:    otwieram plik
        // WYNIK:    plik: czytam
        // WYNIK:    zamykam plik

        // PUŁAPKA: przy dwóch zasobach potrzeba zagnieżdżonych try/finally — a jeśli close() pierwszego rzuci wyjątek,
        //   drugi się nie zamknie. try-with-resources rozwiązuje oba problemy.
    }

    // =================================================================================================
    // 2. PO: try-with-resources
    // =================================================================================================

    /** 2. Zasób deklarowany w nawiasie po try zostaje zamknięty automatycznie — także gdy poleci wyjątek. */
    static void afterTryWithResources() {
        section("2. PO: try-with-resources");

        List<String> log = new ArrayList<>();
        try (Resource r = new Resource("plik", log)) {
            r.use("czytam");
        }
        printLog(log);
        // WYNIK:    otwieram plik
        // WYNIK:    plik: czytam
        // WYNIK:    zamykam plik

        List<String> log2 = new ArrayList<>();
        try (Resource r = new Resource("baza", log2)) {
            r.use("zapytanie");
            throw new IllegalStateException("zapytanie się nie udało");
        } catch (IllegalStateException e) {
            log2.add("catch: " + e.getMessage());   // catch wykonuje się PO zamknięciu zasobu
        }
        printLog(log2);
        // WYNIK:    otwieram baza
        // WYNIK:    baza: zapytanie
        // WYNIK:    zamykam baza
        // WYNIK:    catch: zapytanie się nie udało
    }

    // =================================================================================================
    // 3. KILKA ZASOBÓW
    // =================================================================================================

    /** 3. Zasoby oddzielone średnikiem. Zamykane w ODWROTNEJ kolejności — jak zdejmowanie talerzy ze stosu. */
    static void severalResources() {
        section("3. Kilka zasobów: zamykanie w odwrotnej kolejności");

        List<String> log = new ArrayList<>();
        try (Resource in = new Resource("wejście", log);
             Resource out = new Resource("wyjście", log)) {
            in.use("czytam");
            out.use("zapisuję");
        }
        printLog(log);
        // WYNIK:    otwieram wejście
        // WYNIK:    otwieram wyjście
        // WYNIK:    wejście: czytam
        // WYNIK:    wyjście: zapisuję
        // WYNIK:    zamykam wyjście
        // WYNIK:    zamykam wejście

        // Dlaczego odwrotnie? Drugi zasób często ZALEŻY od pierwszego (np. czytnik zbudowany na strumieniu pliku) —
        //   najpierw zamyka się ten „na wierzchu”.
    }

    // =================================================================================================
    // 4. WYJĄTKI STŁUMIONE
    // =================================================================================================

    /**
     * 4. Wyjątek w bloku + wyjątek w close(). Przy ręcznym finally wyjątek z close() ZASTĄPIŁBY główny (Exceptions03).
     * try-with-resources zachowuje GŁÓWNY, a ten z close() dokleja jako stłumiony: e.getSuppressed().
     */
    static void suppressedExceptions() {
        section("4. Wyjątki stłumione (suppressed)");

        try (FailingResource r = new FailingResource("drukarka")) {
            throw new IllegalArgumentException("zły format dokumentu dla " + r.name());
        } catch (RuntimeException e) {
            show("główny", e.getMessage());
            show("stłumione", Arrays.toString(e.getSuppressed()));
        }
        // WYNIK: główny → zły format dokumentu dla drukarka
        // WYNIK: stłumione → [java.lang.IllegalStateException: błąd przy zamykaniu drukarka]

        try (FailingResource r = new FailingResource("skaner")) {
            show("praca bez błędu z", r.name());
        } catch (IllegalStateException e) {
            show("tylko błąd zamykania", e.getMessage());
        }
        // WYNIK: praca bez błędu z → skaner
        // WYNIK: tylko błąd zamykania → błąd przy zamykaniu skaner    ← gdy blok się udał, błąd close() jest główny
    }

    // =================================================================================================
    // 5. JAVA 9+: ZASÓB ZADEKLAROWANY WCZEŚNIEJ
    // =================================================================================================

    /** 5. Od Javy 9 w nawiasie można podać istniejącą zmienną — jeśli jest final albo efektywnie final. */
    static void java9EffectivelyFinal() {
        section("5. try (zmienna) — Java 9+");

        List<String> log = new ArrayList<>();
        Resource shared = new Resource("połączenie", log);   // efektywnie final (nie zmieniamy przypisania)
        try (shared) {
            shared.use("wysyłam");
        }
        printLog(log);
        // WYNIK:    otwieram połączenie
        // WYNIK:    połączenie: wysyłam
        // WYNIK:    zamykam połączenie
    }

    // =================================================================================================
    // 6. PRAWDZIWY CZYTNIK
    // =================================================================================================

    /** countLines = policz linie. BufferedReader (buforowany czytnik) jest zasobem — zamykamy go przez try-with-resources. */
    static int countLines(String text) throws IOException {
        int lines = 0;
        try (BufferedReader reader = new BufferedReader(new StringReader(text))) {
            while (reader.readLine() != null) {     // readLine = czytaj linię; null = koniec danych
                lines++;
            }
        }
        return lines;
    }

    /**
     * 6. Klasy IO (BufferedReader, FileInputStream...) implementują Closeable — podtyp AutoCloseable.
     * StringReader czyta z napisu, więc przykład działa bez plików (pliki: t18_io_files).
     */
    static void realReader() {
        section("6. BufferedReader w try-with-resources");

        try {
            show("linie w tekście", countLines("pierwsza\ndruga\ntrzecia"));
        } catch (IOException e) {
            show("błąd odczytu", e.getMessage());
        }
        // WYNIK: linie w tekście → 3

        // DOBRA PRAKTYKA: KAŻDY obiekt implementujący AutoCloseable/Closeable, który sam utworzyłeś — otwieraj
        //   w try-with-resources. Dotyczy też Files.lines(...) i Files.list(...) (zwracają Stream do zamknięcia).
    }

    // =================================================================================================
    // 7. PUŁAPKA: zwracanie zasobu z bloku try
    // =================================================================================================

    /** openBroken = zła metoda: zwraca zasób, który try-with-resources zamknie przy wyjściu z metody. */
    static Resource openBroken(List<String> log) {
        try (Resource r = new Resource("raport", log)) {
            r.use("przygotowanie");
            return r;                           // return → wyjście z bloku → close() — zwracamy ZAMKNIĘTY zasób
        }
    }

    /** 7. Zasób zamyka ten, kto go otworzył i UŻYWA. Metoda, która ma go zwrócić, nie może go zamykać. */
    static void returningClosedResource() {
        section("7. Pułapka: zwrócony zasób jest już zamknięty");

        List<String> log = new ArrayList<>();
        Resource r = openBroken(log);
        expectThrows("r.use(\"druk\")", () -> r.use("druk"));
        // WYNIK: ✔ r.use("druk") → rzucono IllegalStateException: Zasób raport jest już zamknięty

        // DOBRA PRAKTYKA: metoda „otwierająca” zwraca zasób bez try-with-resources, a try-with-resources stawia
        //   WYWOŁUJĄCY: try (Resource r = open()) { ... }. Ten sam błąd zdarza się z return Files.lines(...) wewnątrz try.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • try (Typ z = new Typ(...)) { ... } — z.close() wywoła się sam, także przy wyjątku i przy return.
     *   • Zasób musi implementować AutoCloseable (IO: Closeable). close() najlepiej bez throws i idempotentne.
     *   • Kilka zasobów: try (A a = ...; B b = ...) — zamykane w odwrotnej kolejności (b, potem a).
     *   • catch/finally przy try-with-resources wykonują się PO zamknięciu zasobów.
     *   • Błąd w bloku + błąd w close() → główny zostaje, drugi w e.getSuppressed().
     *   • Java 9+: try (istniejącaZmienna) — gdy jest efektywnie final.
     *   • Nie zwracaj zasobu z wnętrza try-with-resources — będzie zamknięty.
     *
     * PYTANIA KONTROLNE:
     *   1. Co musi implementować klasa, żeby można jej było użyć w try-with-resources?
     *   2. Co wypisze (w jakiej kolejności) try (Resource a = ...("A"); Resource b = ...("B")) { } — same wpisy otwieram/zamykam?
     *   3. ZNAJDŹ BŁĄD:
     *          static Stream<String> lines(Path p) throws IOException {
     *              try (Stream<String> s = Files.lines(p)) { return s; }
     *          }
     *   4. Co to jest wyjątek stłumiony i jak go odczytać?
     *   5. Co wykona się pierwsze: close() zasobu czy blok catch tego samego try?
     *   6. Dlaczego ręczne zamykanie w finally jest gorsze od try-with-resources? Podaj dwa powody.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: jeden zasób", List.of("otwieram plik", "plik: czytam", "zamykam plik"), () -> exercise1());
        Check.equal("ćw. 2: dwa zasoby", List.of("otwieram A", "otwieram B", "A: czytam", "B: piszę", "zamykam B", "zamykam A"),
                () -> exercise2());
        Check.equal("ćw. 3a: linie", 4, () -> exercise3("a\nb\nc\nd"));
        Check.equal("ćw. 3b: pusty tekst", 0, () -> exercise3(""));
        Check.equal("ćw. 4: główny i stłumiony", "główny: praca na x, stłumione: 1", () -> exercise4());
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of("otwieram plik", "plik: czytam", "zamykam plik"), () -> solution1());
        Check.equal("ćw. 2 (wzorzec)", List.of("otwieram A", "otwieram B", "A: czytam", "B: piszę", "zamykam B", "zamykam A"),
                () -> solution2());
        Check.equal("ćw. 3a (wzorzec)", 4, () -> solution3("a\nb\nc\nd"));
        Check.equal("ćw. 3b (wzorzec)", 0, () -> solution3(""));
        Check.equal("ćw. 4 (wzorzec)", "główny: praca na x, stłumione: 1", () -> solution4());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): utwórz listę log, w try-with-resources otwórz Resource("plik", log), wywołaj use("czytam")
     * i zwróć log (po bloku try).
     */
    static List<String> exercise1() {
        // TODO: twoje rozwiązanie
        return new ArrayList<>();
    }

    /**
     * ĆWICZENIE 2 (średnie): dwa zasoby A i B w JEDNYM try-with-resources; w bloku a.use("czytam"), potem b.use("piszę").
     * Zwróć log — zobaczysz odwrotną kolejność zamykania.
     */
    static List<String> exercise2() {
        // TODO: twoje rozwiązanie
        return new ArrayList<>();
    }

    /**
     * ĆWICZENIE 3 (średnie): policz linie tekstu BufferedReaderem na StringReader w try-with-resources.
     * IOException złap i zwróć -1. Uwaga: dla "" readLine od razu zwraca null → 0 linii.
     */
    static int exercise3(String text) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): w try-with-resources z FailingResource("x") rzuć w bloku new IllegalStateException("praca na " + r.name()).
     * Złap RuntimeException i zwróć "główny: " + e.getMessage() + ", stłumione: " + liczba wyjątków stłumionych.
     * Podpowiedź: sekcja 4. (Zasób trzeba użyć w bloku — przy nieużywanym kompilator ostrzega: -Xlint:try.)
     */
    static String exercise4() {
        // TODO: twoje rozwiązanie
        return null;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solution1() {
        List<String> log = new ArrayList<>();
        try (Resource r = new Resource("plik", log)) {
            r.use("czytam");
        }
        return log;
    }

    static List<String> solution2() {
        List<String> log = new ArrayList<>();
        try (Resource a = new Resource("A", log);
             Resource b = new Resource("B", log)) {
            a.use("czytam");
            b.use("piszę");
        }
        return log;
    }

    static int solution3(String text) {
        try (BufferedReader reader = new BufferedReader(new StringReader(text))) {
            int lines = 0;
            while (reader.readLine() != null) {
                lines++;
            }
            return lines;
        } catch (IOException e) {
            return -1;
        }
    }

    static String solution4() {
        try (FailingResource r = new FailingResource("x")) {
            throw new IllegalStateException("praca na " + r.name());
        } catch (RuntimeException e) {
            return "główny: " + e.getMessage() + ", stłumione: " + e.getSuppressed().length;
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Interfejs AutoCloseable (albo jego podtyp Closeable) — czyli metodę close().
     *   2. otwieram A, otwieram B, zamykam B, zamykam A.
     *   3. Stream zostanie zamknięty przy wyjściu z metody (return opuszcza blok try) — wywołujący dostanie zamknięty
     *      strumień i IllegalStateException przy pierwszej operacji. Metoda powinna zwrócić Files.lines(p) bez try,
     *      a try-with-resources postawić u wywołującego.
     *   4. Wyjątek z close(), który poleciał, gdy już „leciał” wyjątek z bloku. Nie zastępuje głównego — jest do niego
     *      dołączony; odczyt: e.getSuppressed() (tablica Throwable).
     *   5. close() — catch i finally tego try wykonują się po zamknięciu zasobów.
     *   6. Np.: dużo kodu (zmienna przed try, null-check); przy kilku zasobach zagnieżdżanie; wyjątek z close()
     *      w finally zastępuje (gubi) wyjątek główny; łatwo zapomnieć o zamknięciu.
     */
    // </editor-fold>
}
