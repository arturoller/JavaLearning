package t22_design_patterns;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Product;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Template Method (metoda szablonowa) — stały szkielet algorytmu, zmienne kroki
 *        (template = szablon, wzór; hook = „haczyk”, opcjonalny punkt rozszerzenia; step = krok;
 *         skeleton = szkielet; override = przesłonić metodę w podklasie)
 *
 * W SKRÓCIE:
 *   Klasa bazowa zapisuje algorytm RAZ, jako ciąg kroków (metoda oznaczona {@code final}), a wybrane kroki
 *   zostawia podklasom. Kolejność kroków jest niezmienna i pilnowana w jednym miejscu, a podklasy
 *   dostarczają tylko to, czym się różnią. Wzorzec zamienia kopiowanie kodu na dziedziczenie.
 *
 * ANALOGIA: przepis na ciasto z kuchennej książki „zrób ciasto”: 1) rozgrzej piekarnik, 2) przygotuj masę,
 *   3) upiecz, 4) przełóż na talerz. Kroki i kolejność są stałe. Szarlotka i sernik różnią się tylko
 *   krokiem 2 („przygotuj masę”). Nie wolno zacząć od pieczenia — szkielet tego pilnuje.
 *
 * JAK TO DZIAŁA:
 *   Role (GoF): AbstractClass (klasa abstrakcyjna z metodą szablonową), ConcreteClass (konkretna podklasa).
 *
 *        ┌──────────────────────────────┐
 *        │ ReportGenerator              │
 *        │  final generate(...)         │ ← TEMPLATE METHOD (metoda szablonowa): header → rows → footer
 *        │  abstract header()           │ ← krok OBOWIĄZKOWY (podklasa musi go napisać)
 *        │  abstract row(...)           │ ← krok OBOWIĄZKOWY
 *        │  hook shouldInclude(...)     │ ← HOOK: domyślna implementacja, podklasa MOŻE zmienić
 *        │  hook footer(...)            │ ← HOOK
 *        └──────────────────────────────┘
 *               △                 △
 *          TextReport         CsvReport  (podklasy dopisują tylko różniące się kroki)
 *
 *   Zasada Hollywood („nie dzwoń do nas, my zadzwonimy”): to klasa bazowa woła kod podklasy, nie odwrotnie.
 *   Podklasa nie steruje przebiegiem — jest „wołana” w odpowiednich momentach.
 *
 * SŁÓWKA:
 *   template method = metoda szablonowa; abstract = abstrakcyjna (bez ciała); final = ostateczna
 *   (nie do nadpisania); hook = punkt zaczepienia; Hollywood principle = zasada Hollywood; header = nagłówek;
 *   footer = stopka; row = wiersz; callback = funkcja zwrotna (kod, który ktoś woła za nas);
 *   fragile base class = krucha klasa bazowa; delegate = obiekt, któremu przekazujemy pracę.
 *
 * ZOBACZ TEŻ: t07_inheritance_polymorphism/Inherit03AbstractClasses (klasy abstrakcyjne),
 *   t07_inheritance_polymorphism/Inherit06CompositionVsInheritance (kompozycja kontra dziedziczenie),
 *   t22_design_patterns/Patterns01Strategy (ten sam cel przez kompozycję), t13_lambdas/Lambda07HigherOrderFunctions
 *   (funkcje jako parametry), t18_io_files/Io04Csv (CSV), t12_collections/Collections12CustomIterable,
 *   t22_design_patterns/Patterns03Factory (metoda wytwórcza jako krok szablonu)
 * </pre>
 */
public class Patterns05TemplateMethod {

    public static void main(String[] args) {
        title("Patterns05 — Template Method (metoda szablonowa)");

        problemDuplication();      // problem duplication = problem powielonego szkieletu
        templateMethod();          // template method = metoda szablonowa
        hooksAndHollywood();       // hooks and Hollywood = haczyki i zasada Hollywood
        constructorPitfall();      // constructor pitfall = pułapka wywołania z konstruktora
        templateWithLambdas();     // template with lambdas = szablon z lambdami
        executeAround();           // execute around = „wykonaj dookoła” (jak JdbcTemplate)
        abstractListInJdk();       // AbstractList in JDK = AbstractList w bibliotece standardowej
        fragileBaseClass();        // fragile base class = krucha klasa bazowa
        whenToUse();               // when to use = kiedy używać
        exercises();               // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PROBLEM: ten sam szkielet skopiowany w kilku miejscach
    // =================================================================================================

    /** Raport tekstowy BEZ wzorca: szkielet „nagłówek → wiersze → stopka” zapisany na sztywno. */
    static List<String> legacyTextReport(List<Product> products) {
        List<String> lines = new ArrayList<>();
        lines.add("RAPORT MAGAZYNOWY");                                 // nagłówek
        for (Product p : products) {
            lines.add(p.name() + ": " + p.stock() + " szt.");          // wiersz
        }
        lines.add("Razem pozycji: " + products.size());                // stopka
        return lines;
    }

    /** Raport CSV BEZ wzorca: TEN SAM szkielet skopiowany, zmieniły się tylko trzy napisy. */
    static List<String> legacyCsvReport(List<Product> products) {
        List<String> lines = new ArrayList<>();
        lines.add("sku;nazwa;zapas");
        for (Product p : products) {
            lines.add(p.sku() + ";" + p.name() + ";" + p.stock());
        }
        lines.add("Razem pozycji: " + products.size());
        return lines;
    }

    /**
     * 1. Dwa raporty mają identyczny porządek kroków. Gdy biznes mówi „dodajmy pustą linię przed stopką”
     * albo „raport ma pomijać pozycje bez zapasu”, trzeba zmienić OBA miejsca — i nie pomylić się w żadnym.
     */
    static void problemDuplication() {
        section("1. Problem: powielony szkielet algorytmu");

        List<Product> two = SampleData.products().subList(0, 2);   // subList = fragment listy (od 0 do 2, bez 2)
        show("raport tekstowy", legacyTextReport(two));
        // WYNIK: raport tekstowy → [RAPORT MAGAZYNOWY, Laptop Pro 14: 7 szt., Smartfon X: 0 szt., Razem pozycji: 2]
        show("raport CSV", legacyCsvReport(two));
        // WYNIK: raport CSV → [sku;nazwa;zapas, ELE-001;Laptop Pro 14;7, ELE-002;Smartfon X;0, Razem pozycji: 2]

        // PUŁAPKA: kopiowanie szkieletu daje „dryf”: po miesiącu jeden raport ma stopkę z liczbą, a drugi bez,
        // bo ktoś poprawił tylko jedną kopię. Zasada DRY (Don't Repeat Yourself = nie powtarzaj się):
        // wiedza „w jakiej kolejności budujemy raport” powinna istnieć w jednym miejscu.
    }

    // =================================================================================================
    // 2. METODA SZABLONOWA
    // =================================================================================================

    /** Klasa bazowa z metodą szablonową. abstract = nie da się jej utworzyć przez new, trzeba podklasy. */
    abstract static class ReportGenerator {

        /** TEMPLATE METHOD. final = podklasa NIE może zmienić kolejności kroków. */
        final List<String> generate(List<Product> products) {
            List<String> lines = new ArrayList<>();
            lines.add(header());                         // krok 1 (abstrakcyjny)
            int count = 0;
            for (Product p : products) {
                if (shouldInclude(p)) {                  // krok 2 (hook: domyślnie „tak”)
                    lines.add(row(p));                   // krok 3 (abstrakcyjny)
                    count++;
                }
            }
            String footer = footer(count);               // krok 4 (hook: domyślnie brak)
            if (!footer.isEmpty()) {                     // isEmpty = czy pusty
                lines.add(footer);
            }
            return lines;
        }

        protected abstract String header();              // protected = widoczna dla podklas, nie dla świata

        protected abstract String row(Product product);

        /** HOOK: ma domyślną, „neutralną” implementację — podklasa nadpisuje tylko, jeśli chce. */
        protected boolean shouldInclude(Product product) {
            return true;
        }

        /** HOOK: domyślnie brak stopki (pusty napis). */
        protected String footer(int count) {
            return "";
        }
    }

    static class TextReport extends ReportGenerator {
        @Override // Override = przesłoń (nadpisz metodę klasy bazowej)
        protected String header() {
            return "RAPORT MAGAZYNOWY";
        }

        @Override
        protected String row(Product product) {
            return product.name() + ": " + product.stock() + " szt.";
        }

        @Override
        protected String footer(int count) {
            return "Razem pozycji: " + count;
        }
    }

    static class CsvReport extends ReportGenerator {
        @Override
        protected String header() {
            return "sku;nazwa;zapas";
        }

        @Override
        protected String row(Product product) {
            return product.sku() + ";" + product.name() + ";" + product.stock();
        }
    }

    /** Podklasa podklasy: zmienia tylko HOOK shouldInclude — resztę dziedziczy z CsvReport. */
    static final class InStockCsvReport extends CsvReport {
        @Override
        protected boolean shouldInclude(Product product) {
            return product.inStock();   // inStock = jest w magazynie (zapas większy od zera)
        }
    }

    /**
     * 2. Szkielet jest w jednym miejscu (generate); podklasy opisują tylko to, co je odróżnia.
     * Dodanie „pustej linii przed stopką” to jedna zmiana w klasie bazowej — działa dla wszystkich raportów.
     */
    static void templateMethod() {
        section("2. Metoda szablonowa: szkielet w klasie bazowej");

        List<Product> four = SampleData.products().subList(0, 4);
        show("TextReport", new TextReport().generate(four));
        // WYNIK: TextReport → [RAPORT MAGAZYNOWY, Laptop Pro 14: 7 szt., Smartfon X: 0 szt., Słuchawki BT: 25 szt., Monitor 27 cali: 4 szt., Razem pozycji: 4]
        show("CsvReport", new CsvReport().generate(four));
        // WYNIK: CsvReport → [sku;nazwa;zapas, ELE-001;Laptop Pro 14;7, ELE-002;Smartfon X;0, ELE-003;Słuchawki BT;25, ELE-004;Monitor 27 cali;4]
        show("InStockCsvReport (hook pomija Smartfon X, zapas 0)", new InStockCsvReport().generate(four));
        // WYNIK: InStockCsvReport (hook pomija Smartfon X, zapas 0) → [sku;nazwa;zapas, ELE-001;Laptop Pro 14;7, ELE-003;Słuchawki BT;25, ELE-004;Monitor 27 cali;4]

        // DOBRA PRAKTYKA: metodę szablonową oznacz final — inaczej podklasa może zmienić kolejność kroków i
        // złamać gwarancje, na których polega reszta kodu.
        // DOBRA PRAKTYKA: kroki obowiązkowe = abstract (kompilator zmusi do implementacji); kroki opcjonalne =
        // hook z sensowną domyślną wersją. Kroki oznaczaj protected, żeby nie były częścią publicznego API.
        // DOBRA PRAKTYKA: nazwij hooki wprost (shouldInclude, footer) i opisz w Javadoc, KIEDY są wołane.
    }

    // =================================================================================================
    // 3. HOOKI I ZASADA HOLLYWOOD
    // =================================================================================================

    /** Raport, który zapisuje kolejność własnych wywołań — widać, KTO kogo woła. */
    static final class TracingReport extends ReportGenerator {
        private final List<String> trace = new ArrayList<>();   // trace = ślad

        @Override
        protected String header() {
            trace.add("header");
            return "NAGŁÓWEK";
        }

        @Override
        protected boolean shouldInclude(Product product) {
            trace.add("shouldInclude(" + product.name() + ")");
            return true;
        }

        @Override
        protected String row(Product product) {
            trace.add("row(" + product.name() + ")");
            return product.name();
        }

        @Override
        protected String footer(int count) {
            trace.add("footer(" + count + ")");
            return "";
        }

        List<String> trace() {
            return trace;
        }
    }

    /**
     * 3. Zasada Hollywood (Hollywood principle): „nie dzwoń do nas, my zadzwonimy”. Klient wywołuje JEDNĄ metodę
     * ({@code generate}); resztę woła klasa bazowa, w kolejności, którą sama ustaliła. Podklasa nie wywołuje kroków.
     */
    static void hooksAndHollywood() {
        section("3. Zasada Hollywood: kto kogo woła");

        TracingReport report = new TracingReport();
        report.generate(SampleData.products().subList(0, 2));
        showEach("kolejność wywołań", report.trace());
        // WYNIK: kolejność wywołań (liczba elementów: 6):
        // WYNIK:    • header
        // WYNIK:    • shouldInclude(Laptop Pro 14)
        // WYNIK:    • row(Laptop Pro 14)
        // WYNIK:    • shouldInclude(Smartfon X)
        // WYNIK:    • row(Smartfon X)
        // WYNIK:    • footer(2)
        // Zauważ: klient wywołał tylko generate(). Kroki zostały wywołane przez klasę bazową.

        // To ta sama „odwrócona kontrola” (inversion of control, IoC), którą znasz z frameworków:
        //   • JUnit sam woła metody z adnotacją @BeforeEach, @Test, @AfterEach w ustalonej kolejności;
        //   • Servlet API: HttpServlet.service() woła doGet() lub doPost() napisane przez Ciebie;
        //   • Spring: OncePerRequestFilter.doFilter() woła Twoje doFilterInternal();
        //   • Spring: JdbcTemplate prowadzi całe połączenie i woła Twój RowMapper (sekcja 6).
        // Framework = klasa bazowa lub szablon, a Ty piszesz „klocki”, które zostaną wywołane.
    }

    // =================================================================================================
    // 4. PUŁAPKA: wywołanie nadpisywalnej metody z konstruktora
    // =================================================================================================

    /** Dziennik do pokazania kolejności inicjalizacji. */
    static final List<String> CTOR_LOG = new ArrayList<>();

    /** Klasa bazowa woła z KONSTRUKTORA metodę, którą nadpisuje podklasa — to jest błąd projektowy. */
    abstract static class Greeter {   // greeter = ten, kto wita
        Greeter() {
            CTOR_LOG.add("konstruktor bazowy widzi prefiks: " + prefix());   // wołanie nadpisywalnej metody
        }

        abstract String prefix();   // prefix = przedrostek
    }

    static final class PoliteGreeter extends Greeter {   // polite = grzeczny
        private final String word;

        PoliteGreeter(String word) {
            this.word = word;   // przypisanie następuje PO konstruktorze klasy bazowej
        }

        @Override
        String prefix() {
            return word;
        }
    }

    /**
     * 4. Konstruktor klasy bazowej wykonuje się PRZED inicjalizacją pól podklasy. Jeśli woła metodę, którą
     * nadpisuje podklasa, ta metoda zobaczy pola jeszcze niezainicjalizowane (null, 0, false).
     */
    static void constructorPitfall() {
        section("4. Pułapka: metoda nadpisywalna wołana z konstruktora");

        CTOR_LOG.clear();
        PoliteGreeter greeter = new PoliteGreeter("Szanowny");
        show("dziennik konstruktora", CTOR_LOG);
        // WYNIK: dziennik konstruktora → [konstruktor bazowy widzi prefiks: null]
        show("po zakończeniu konstruktora", greeter.prefix());
        // WYNIK: po zakończeniu konstruktora → Szanowny

        // PUŁAPKA: w metodzie szablonowej NIE wołaj kroków z konstruktora klasy bazowej — podklasa nie jest jeszcze gotowa.
        // Wołaj je z zwykłej metody (jak generate), którą klient wywołuje PO utworzeniu obiektu.
        // DOBRA PRAKTYKA: w konstruktorze wołaj tylko metody prywatne albo final.
    }

    // =================================================================================================
    // 5. NOWOCZESNA WERSJA: szablon z lambdami
    // =================================================================================================

    /**
     * Szablon bez dziedziczenia: kroki zmienne to PARAMETRY-FUNKCJE (Function = funkcja przyjmująca wartość i
     * zwracająca wynik; IntFunction = funkcja przyjmująca int). Szkielet jest ten sam, co w ReportGenerator.
     */
    static List<String> buildReport(List<Product> products, String header,
                                    Function<Product, String> row, IntFunction<String> footer) {
        List<String> lines = new ArrayList<>();
        lines.add(header);
        for (Product p : products) {
            lines.add(row.apply(p));      // apply = zastosuj funkcję
        }
        String footerText = footer.apply(products.size());
        if (!footerText.isEmpty()) {
            lines.add(footerText);
        }
        return lines;
    }

    /** Wynik importu: ile wierszy przyjęto, ile odrzucono (record = rekord, t09_records). */
    record ImportResult(int imported, int rejected) {
    }

    /**
     * Szablon importu: szkielet „parsuj → zapisz, zlicz błędy” jest stały, a Ty podajesz PARSER i ZAPIS jako
     * lambdy (Consumer = odbiorca: przyjmuje wartość, nic nie zwraca). Szablon nie czyta plików ani bazy —
     * dostaje linie z pamięci, więc test jest szybki i deterministyczny.
     */
    static <T> ImportResult importAll(List<String> lines, Function<String, T> parser, Consumer<T> saver) {
        int imported = 0;
        int rejected = 0;
        for (String line : lines) {
            T item;
            try {
                item = parser.apply(line);
            } catch (IllegalArgumentException e) {   // parser sygnalizuje zły wiersz wyjątkiem
                rejected++;
                continue;                            // continue = przejdź do następnego wiersza
            }
            saver.accept(item);                      // accept = przyjmij; błąd zapisu NIE jest „złym wierszem”
            imported++;
        }
        return new ImportResult(imported, rejected);
    }

    /** Jedna sprzedaż z pliku CSV (uproszczona: data, SKU, ilość). */
    record Sale(LocalDate date, String sku, int quantity) {
    }

    /** Parser jednej linii CSV; każdy błąd zamieniony na IllegalArgumentException. */
    static Sale parseSale(String line) {
        String[] fields = line.split(",");   // split = podziel napis według separatora
        if (fields.length != 6) {
            throw new IllegalArgumentException("Zła liczba pól: " + fields.length);
        }
        try {
            return new Sale(LocalDate.parse(fields[0].trim()), fields[1].trim(), Integer.parseInt(fields[4].trim()));
        } catch (DateTimeParseException e) {   // DateTimeParseException NIE dziedziczy po IllegalArgumentException
            throw new IllegalArgumentException("Zła data: " + fields[0], e);
        }
    }

    /**
     * 5. Ten sam cel co klasa abstrakcyjna, ale przez kompozycję: zmienne kroki to funkcje, nie metody podklas.
     * To dokładnie podejście Springa w {@code JdbcTemplate.query(sql, rowMapper)}: szablon prowadzi całą procedurę,
     * Ty dostarczasz jedną funkcję (RowMapper — „jak zamienić wiersz bazy na obiekt”).
     */
    static void templateWithLambdas() {
        section("5. Szablon z lambdami zamiast dziedziczenia");

        List<Product> two = SampleData.products().subList(0, 2);
        show("raport (lambdy)", buildReport(two, "NAZWY", Product::name, count -> "Razem: " + count));
        // WYNIK: raport (lambdy) → [NAZWY, Laptop Pro 14, Smartfon X, Razem: 2]
        show("raport bez stopki", buildReport(two, "SKU", Product::sku, count -> ""));
        // WYNIK: raport bez stopki → [SKU, ELE-001, ELE-002]

        // Import z listy linii (SampleData.salesCsvLines: nagłówek + 12 linii, w tym 4 błędne):
        List<String> csv = SampleData.salesCsvLines();
        List<Sale> saved = new ArrayList<>();   // saved = zapisane
        ImportResult result = importAll(csv.subList(1, csv.size()), Patterns05TemplateMethod::parseSale, saved::add);
        show("wynik importu", result);
        // WYNIK: wynik importu → ImportResult[imported=8, rejected=4]
        show("zapisane sprzedaże", saved.size());
        // WYNIK: zapisane sprzedaże → 8

        // Test bez plików i bazy: dane w pamięci, „zapis” do listy.
        List<Integer> stored = new ArrayList<>();
        show("mini-test importu", importAll(List.of("1", "x", "3"), Integer::parseInt, stored::add));
        // WYNIK: mini-test importu → ImportResult[imported=2, rejected=1]
        show("zapisane w teście", stored);
        // WYNIK: zapisane w teście → [1, 3]

        // KLASA CZY LAMBDY?
        //   lambdy        — gdy zmiennych kroków jest 1–3, nie dzielą stanu i nie potrzebują nazwy; mniej klas, łatwiejszy test;
        //   klasa bazowa  — gdy kroków jest wiele, mają wspólny stan roboczy, tworzą nazwane warianty
        //                   (TextReport, CsvReport) albo piszesz framework, który ma być rozszerzany przez dziedziczenie.
        // PUŁAPKA: łapanie zbyt ogólnego wyjątku (np. Exception) w szablonie ukryłoby błędy programisty (NullPointerException)
        // jako „zły wiersz”. Dlatego parser zgłasza wyłącznie IllegalArgumentException, a szablon łapie tylko ją.
    }

    // =================================================================================================
    // 6. EXECUTE AROUND — szablon zarządzający zasobem (jak JdbcTemplate)
    // =================================================================================================

    /** Udawane połączenie z bazą: zapisuje w dzienniku otwarcie, zapytania i zamknięcie. */
    static final class FakeConnection implements AutoCloseable {
        private final List<String> log;

        FakeConnection(List<String> log) {
            this.log = log;
            log.add("otwarto połączenie");
        }

        String query(String sql) {
            log.add("zapytanie: " + sql);
            return "wynik(" + sql + ")";
        }

        @Override
        public void close() {   // close = zamknij; woła ją try-with-resources (t10_exceptions/Exceptions04TryWithResources)
            log.add("zamknięto połączenie");
        }
    }

    /** Szablon: otwiera zasób, wykonuje DOWOLNĄ pracę przekazaną jako lambda, i ZAWSZE zamyka zasób. */
    static final class MiniJdbcTemplate {
        private final List<String> log;

        MiniJdbcTemplate(List<String> log) {
            this.log = log;
        }

        <T> T execute(Function<FakeConnection, T> work) {
            try (FakeConnection connection = new FakeConnection(log)) {
                return work.apply(connection);
            }
        }
    }

    /**
     * 6. Wariant szablonu zwany „execute around” (wykonaj dookoła): szablon robi to, co NUDNE i łatwe do
     * zepsucia (otwórz, zamknij, obsłuż błędy), a Ty dostarczasz tylko to, co ciekawe. Nie da się zapomnieć o zamknięciu.
     */
    static void executeAround() {
        section("6. Execute around: szablon zarządzający zasobem");

        List<String> log = new ArrayList<>();
        MiniJdbcTemplate template = new MiniJdbcTemplate(log);
        String result = template.execute(connection -> connection.query("SELECT 1"));
        show("wynik pracy", result);
        // WYNIK: wynik pracy → wynik(SELECT 1)
        show("dziennik", log);
        // WYNIK: dziennik → [otwarto połączenie, zapytanie: SELECT 1, zamknięto połączenie]

        // Gdy praca rzuci wyjątek, zasób i tak zostaje zamknięty:
        List<String> failLog = new ArrayList<>();
        MiniJdbcTemplate failing = new MiniJdbcTemplate(failLog);
        expectThrows("praca rzuca wyjątek", () -> failing.<String>execute(connection -> {
            throw new IllegalStateException("błąd SQL");
        }));
        // WYNIK: ✔ praca rzuca wyjątek → rzucono IllegalStateException: błąd SQL
        show("dziennik po błędzie", failLog);
        // WYNIK: dziennik po błędzie → [otwarto połączenie, zamknięto połączenie]

        // W Springu: JdbcTemplate, RestTemplate, TransactionTemplate działają dokładnie tak: Ty podajesz lambdę
        // (RowMapper, callback), szablon pilnuje połączeń, transakcji i zamykania zasobów (SpringLearning).
        // DOBRA PRAKTYKA: jeśli w kodzie pojawia się powtarzalna para „przygotuj … posprzątaj”, zamknij ją w szablonie
        // z lambdą — klient nie może zapomnieć o sprzątaniu, bo go nie wykonuje.
    }

    // =================================================================================================
    // 7. ABSTRACTLIST W JDK
    // =================================================================================================

    /** Zakres liczb od-do (bez końca) jako lista tylko do odczytu. Piszemy TYLKO get i size. */
    static final class Range extends AbstractList<Integer> {
        private final int from;
        private final int to;

        Range(int from, int to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public Integer get(int index) {
            Objects.checkIndex(index, size());   // checkIndex = sprawdź indeks (Java 9+); zły indeks → wyjątek
            return from + index;
        }

        @Override
        public int size() {
            return to - from;
        }
    }

    /**
     * 7. {@code AbstractList} to metoda szablonowa w JDK: Ty piszesz dwa kroki (get i size), a klasa bazowa dorzuca
     * iterator, contains, indexOf, equals, hashCode, toString, subList... — wszystkie zbudowane z tych dwóch.
     * Podobnie InputStream.read(byte[]) woła Twoją metodę read(), a AbstractMap daje całą mapę z entrySet().
     */
    static void abstractListInJdk() {
        section("7. AbstractList: dwie metody, cała lista");

        Range range = new Range(1, 6);
        show("zakres", range);   // toString dostarczone przez AbstractCollection
        // WYNIK: zakres → [1, 2, 3, 4, 5]
        show("contains(3)", range.contains(3));
        // WYNIK: contains(3) → true
        show("indexOf(4)", range.indexOf(4));
        // WYNIK: indexOf(4) → 3
        show("subList(1, 3)", range.subList(1, 3));
        // WYNIK: subList(1, 3) → [2, 3]
        show("equals(List.of(1,2,3,4,5))", range.equals(List.of(1, 2, 3, 4, 5)));
        // WYNIK: equals(List.of(1,2,3,4,5)) → true
        show("suma przez stream (t16_streams)", range.stream().mapToInt(Integer::intValue).sum());
        // WYNIK: suma przez stream (t16_streams) → 15
        int sum = 0;
        for (int number : range) {   // pętla for-each korzysta z iteratora z AbstractList
            sum += number;
        }
        show("suma przez for-each", sum);
        // WYNIK: suma przez for-each → 15
        expectThrows("add (domyślnie niewspierane)", () -> range.add(99));
        // WYNIK: ✔ add (domyślnie niewspierane) → rzucono UnsupportedOperationException: (brak komunikatu)

        // DOBRA PRAKTYKA: gdy piszesz własną kolekcję, rozszerz AbstractList / AbstractMap / AbstractSet — dostajesz
        // dziesiątki poprawnych metod za darmo. Lista tylko do odczytu = get + size; modyfikowalna = dodatkowo
        // set, add(int, E), remove(int). Zobacz też t12_collections/Collections12CustomIterable.
    }

    // =================================================================================================
    // 8. KRUCHA KLASA BAZOWA (fragile base class)
    // =================================================================================================

    /** Licznik dodanych elementów przez DZIEDZICZENIE po HashSet — klasyczny błąd z „Effective Java”. */
    static final class CountingHashSet extends HashSet<String> {
        private static final long serialVersionUID = 1L;
        private int addCount;   // ile elementów dodano (wg naszego liczenia)

        @Override
        public boolean add(String element) {
            addCount++;
            return super.add(element);
        }

        @Override
        public boolean addAll(Collection<? extends String> elements) {
            addCount += elements.size();   // liczymy hurtowo...
            return super.addAll(elements); // ...a klasa bazowa może wołać add() dla każdego elementu
        }

        int addCount() {
            return addCount;
        }
    }

    /** To samo przez KOMPOZYCJĘ: trzymamy zwykły Set w polu i delegujemy do niego. */
    static final class CountingSet {
        private final Set<String> delegate = new HashSet<>();   // delegate = obiekt, któremu przekazujemy pracę
        private int addCount;

        boolean add(String element) {
            addCount++;
            return delegate.add(element);
        }

        boolean addAll(Collection<String> elements) {
            addCount += elements.size();
            return delegate.addAll(elements);   // wewnętrzne add() delegata NIE wraca do naszej metody add
        }

        int addCount() {
            return addCount;
        }
    }

    /**
     * 8. Dziedziczenie wiąże podklasę z DETALAMI implementacji klasy bazowej. W OpenJDK 17 {@code HashSet.addAll}
     * (odziedziczone z AbstractCollection) woła {@code add} dla każdego elementu — więc nasz licznik liczy dwa razy.
     * To szczegół implementacji, który autor JDK mógłby zmienić bez ostrzeżenia — i właśnie dlatego klasa bazowa jest „krucha”.
     */
    static void fragileBaseClass() {
        section("8. Krucha klasa bazowa: dziedziczenie wiąże ze szczegółami");

        CountingHashSet inherited = new CountingHashSet();
        inherited.addAll(List.of("a", "b", "c"));
        show("dziedziczenie: addCount po addAll(3 elementy)", inherited.addCount());
        // WYNIK: dziedziczenie: addCount po addAll(3 elementy) → 6

        CountingSet composed = new CountingSet();
        composed.addAll(List.of("a", "b", "c"));
        show("kompozycja: addCount po addAll(3 elementy)", composed.addCount());
        // WYNIK: kompozycja: addCount po addAll(3 elementy) → 3

        // Jak ograniczyć kruchość w metodzie szablonowej:
        //   • metoda szablonowa jest final, a hooki są dokumentowane („wywoływany raz na wiersz, przed row()”);
        //   • dokumentuj, które metody wołają które (tzw. self-use: samoużycie) — to część kontraktu klasy bazowej;
        //   • mało kroków protected; nie wołaj kroków z konstruktora (sekcja 4);
        //   • klasę, która nie jest zaprojektowana do dziedziczenia, oznacz final (albo nie dziedzicz po niej).
        // PUŁAPKA: zmiana klasy bazowej (nowy krok, inna kolejność) psuje podklasy, które zakładały starą kolejność —
        // i nie zobaczysz tego przy kompilacji, tylko w zachowaniu.
        // DOBRA PRAKTYKA: (Effective Java, pozycja 18) „kompozycja zamiast dziedziczenia” — dziedzicz tylko wtedy, gdy klasa
        // bazowa jest do tego zaprojektowana i udokumentowana (jak AbstractList). Zobacz Inherit06CompositionVsInheritance.
    }

    // =================================================================================================
    // 9. KIEDY UŻYWAĆ I RÓŻNICA WZGLĘDEM STRATEGY
    // =================================================================================================

    /**
     * 9. Template Method i Strategy rozwiązują podobny problem różnymi środkami.
     * <pre>
     *                      TEMPLATE METHOD                   STRATEGY
     *   mechanizm          dziedziczenie (extends)           kompozycja (obiekt w polu)
     *   zmienna część      kroki w podklasie                 cały algorytm w obiekcie
     *   kiedy wybrana      przy tworzeniu obiektu            można zmienić w czasie działania
     *   stały szkielet     tak, pilnuje go klasa bazowa      nie, strategia jest całością
     *   typowy użytek      frameworki, jedna rodzina wariantów  wymienne reguły (rabaty, sortowanie)
     * </pre>
     *
     * <pre>
     *   UŻYJ METODY SZABLONOWEJ, GDY:                      NIE UŻYWAJ, GDY:
     *   • kilka algorytmów ma ten sam szkielet             • różni się tylko jedna drobna rzecz (wystarczy lambda)
     *   • kolejność kroków ma być wymuszona                • wariant ma zmieniać się w czasie działania programu
     *   • piszesz framework z punktami rozszerzenia        • potrzebujesz wielu niezależnych wymiarów zmian
     *   • zmienne kroki mają wspólny stan roboczy          • klasa bazowa nie jest Twoja i nie jest przygotowana
     * </pre>
     */
    static void whenToUse() {
        section("9. Kiedy używać; Template Method a Strategy");

        note("Template Method: stały szkielet w klasie bazowej, zmienne kroki w podklasach (dziedziczenie).");
        // WYNIK:    ℹ Template Method: stały szkielet w klasie bazowej, zmienne kroki w podklasach (dziedziczenie).
        note("Strategy i szablon z lambdami: ten sam cel przez kompozycję — częściej wygrywają dziś.");
        // WYNIK:    ℹ Strategy i szablon z lambdami: ten sam cel przez kompozycję — częściej wygrywają dziś.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Template Method (metoda szablonowa) = klasa bazowa ma final metodę z kolejnością kroków; podklasy piszą kroki.
     *   • Role: metoda szablonowa (final), kroki abstrakcyjne (obowiązkowe), hooki (opcjonalne, z domyślną wersją).
     *   • Zasada Hollywood: „nie dzwoń do nas, my zadzwonimy” — klasa bazowa woła podklasę (odwrócona kontrola).
     *   • Nie wołaj nadpisywalnych metod z konstruktora klasy bazowej: pola podklasy są jeszcze puste.
     *   • Nowocześnie: zmienne kroki jako funkcje (Function, Consumer) — szablon z lambdami, jak JdbcTemplate + RowMapper.
     *   • Execute around: szablon otwiera i zamyka zasób, a Ty podajesz pracę w lambdzie — nie zapomnisz o sprzątaniu.
     *   • W JDK: AbstractList (get + size → reszta), AbstractMap, InputStream.read(byte[]).
     *   • Pułapka: krucha klasa bazowa — podklasa zależy od szczegółów implementacji (HashSet.addAll woła add).
     *   • Kompozycja (Strategy, lambdy) jest bezpieczniejsza; dziedziczenie tylko dla klas zaprojektowanych do rozszerzania.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego metoda szablonowa powinna być oznaczona final?
     *   2. Czym różni się krok abstrakcyjny od hooka?
     *   3. Na czym polega zasada Hollywood? Podaj przykład z frameworku.
     *   4. Co wypisze: new Range(2, 5).indexOf(4)  (Range jak w lekcji, lista [2, 3, 4])  ?
     *   5. Co wypisze:  CountingHashSet s = new CountingHashSet();  s.add("x");  s.addAll(List.of("y", "z"));
     *                   System.out.println(s.addCount());   ?   (uwaga na sekcję 8)
     *   6. ZNAJDŹ BŁĄD: konstruktor klasy bazowej woła abstrakcyjną metodę name(), a podklasa zwraca wartość z własnego pola
     *      ustawionego w swoim konstruktorze. Co zobaczy konstruktor bazowy?
     *   7. ZNAJDŹ BŁĄD: ktoś rozszerza klasę ReportGenerator i NADPISUJE metodę generate(...), żeby zmienić kolejność
     *      nagłówka i stopki. Co by pomogło temu zapobiec?
     *   8. Kiedy lepsza jest strategia (lub szablon z lambdami) od klasycznej metody szablonowej?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        List<Product> two = SampleData.products().subList(0, 2);
        List<Product> four = SampleData.products().subList(0, 4);
        List<Product> pair = SampleData.products().subList(4, 6);   // Kawa ziarnista 1kg, Czekolada gorzka

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: raport SKU | NAZWA", List.of("SKU | NAZWA", "ELE-001 | Laptop Pro 14", "ELE-002 | Smartfon X"),
                () -> new Exercise1Report().generate(two));
        Check.equal("ćw. 2: raport tylko z zapasem",
                List.of("NAZWA;ZAPAS", "Laptop Pro 14;7", "Słuchawki BT;25", "Monitor 27 cali;4", "Razem: 3"),
                () -> new Exercise2Report().generate(four));
        Check.equal("ćw. 3: szablon z lambdami",
                List.of("PRODUKTY", "Kawa ziarnista 1kg: 64.99", "Czekolada gorzka: 7.49", "Liczba: 2"),
                () -> exercise3(pair));
        Check.equal("ćw. 4: Countdown(5)", List.of(5, 4, 3, 2, 1), () -> new Exercise4Countdown(5));
        Check.equal("ćw. 4: indexOf(2)", 3, () -> new Exercise4Countdown(5).indexOf(2));
        Check.equal("ćw. 4: contains(5)", true, () -> new Exercise4Countdown(5).contains(5));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of("SKU | NAZWA", "ELE-001 | Laptop Pro 14", "ELE-002 | Smartfon X"),
                () -> new Solution1Report().generate(two));
        Check.equal("ćw. 2 (wzorzec)",
                List.of("NAZWA;ZAPAS", "Laptop Pro 14;7", "Słuchawki BT;25", "Monitor 27 cali;4", "Razem: 3"),
                () -> new Solution2Report().generate(four));
        Check.equal("ćw. 3 (wzorzec)",
                List.of("PRODUKTY", "Kawa ziarnista 1kg: 64.99", "Czekolada gorzka: 7.49", "Liczba: 2"),
                () -> solution3(pair));
        Check.equal("ćw. 4 (wzorzec): Countdown(5)", List.of(5, 4, 3, 2, 1), () -> new Solution4Countdown(5));
        Check.equal("ćw. 4 (wzorzec): indexOf(2)", 3, () -> new Solution4Countdown(5).indexOf(2));
        Check.equal("ćw. 4 (wzorzec): contains(5)", true, () -> new Solution4Countdown(5).contains(5));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): dokończ podklasę ReportGenerator. Nagłówek ma brzmieć {@code "SKU | NAZWA"},
     * a wiersz {@code sku + " | " + name}. Stopki ani filtra nie zmieniaj (hooki zostają domyślne).
     * Podpowiedź: nadpisz dwie metody abstrakcyjne (header i row).
     */
    static final class Exercise1Report extends ReportGenerator {
        @Override
        protected String header() {
            // TODO: zwróć nagłówek
            throw new UnsupportedOperationException("TODO");
        }

        @Override
        protected String row(Product product) {
            // TODO: zwróć wiersz
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * ĆWICZENIE 2 (średnie): użyj HOOKÓW. Raport ma pomijać produkty bez zapasu (shouldInclude) i kończyć się
     * stopką {@code "Razem: N"} (footer). Nagłówek: {@code "NAZWA;ZAPAS"}, wiersz: {@code name + ";" + stock}.
     * Podpowiedź: Product ma metodę inStock(); N to liczba dołączonych wierszy (dostajesz ją w parametrze footer).
     */
    static final class Exercise2Report extends ReportGenerator {
        @Override
        protected String header() {
            // TODO: nagłówek
            throw new UnsupportedOperationException("TODO");
        }

        @Override
        protected String row(Product product) {
            // TODO: wiersz
            throw new UnsupportedOperationException("TODO");
        }

        @Override
        protected boolean shouldInclude(Product product) {
            // TODO: tylko produkty z zapasem
            throw new UnsupportedOperationException("TODO");
        }

        @Override
        protected String footer(int count) {
            // TODO: stopka „Razem: N”
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ podklasę na szablon z lambdami. Stary kod:
     * <pre>{@code
     * class ProductReport extends ReportGenerator {
     *     protected String header() { return "PRODUKTY"; }
     *     protected String row(Product p) { return p.name() + ": " + p.price(); }
     *     protected String footer(int count) { return "Liczba: " + count; }
     * }
     * }</pre>
     * Nowy kod: wywołaj {@code buildReport(products, ..., ..., ...)} z odpowiednimi lambdami.
     */
    static List<String> exercise3(List<Product> products) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): odliczanie jako lista. {@code new Exercise4Countdown(5)} ma być listą
     * [5, 4, 3, 2, 1]. Napisz TYLKO get i size — reszta (indexOf, contains, equals) dojdzie z AbstractList.
     * Podpowiedź: get(i) = start - i; size() = start; wzoruj się na klasie Range z sekcji 7.
     */
    static final class Exercise4Countdown extends AbstractList<Integer> {
        private final int start;

        Exercise4Countdown(int start) {
            this.start = start;
        }

        @Override
        public Integer get(int index) {
            // TODO: element o danym indeksie
            throw new UnsupportedOperationException("TODO");
        }

        @Override
        public int size() {
            // TODO: rozmiar listy (na razie 0, więc lista wygląda na pustą i sprawdzenia nie przechodzą)
            return 0;
        }
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static final class Solution1Report extends ReportGenerator {
        @Override
        protected String header() {
            return "SKU | NAZWA";
        }

        @Override
        protected String row(Product product) {
            return product.sku() + " | " + product.name();
        }
    }

    static final class Solution2Report extends ReportGenerator {
        @Override
        protected String header() {
            return "NAZWA;ZAPAS";
        }

        @Override
        protected String row(Product product) {
            return product.name() + ";" + product.stock();
        }

        @Override
        protected boolean shouldInclude(Product product) {
            return product.inStock();
        }

        @Override
        protected String footer(int count) {
            return "Razem: " + count;
        }
    }

    static List<String> solution3(List<Product> products) {
        return buildReport(products, "PRODUKTY", p -> p.name() + ": " + p.price(), count -> "Liczba: " + count);
    }

    static final class Solution4Countdown extends AbstractList<Integer> {
        private final int start;

        Solution4Countdown(int start) {
            this.start = start;
        }

        @Override
        public Integer get(int index) {
            Objects.checkIndex(index, size());
            return start - index;
        }

        @Override
        public int size() {
            return start;
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Żeby podklasa nie mogła zmienić kolejności kroków ani pominąć któregoś — szkielet jest gwarancją klasy bazowej.
     *   2. Krok abstrakcyjny nie ma implementacji i podklasa MUSI go napisać; hook ma domyślną implementację i podklasa
     *      MOŻE ją nadpisać.
     *   3. „Nie dzwoń do nas, my zadzwonimy”: klasa bazowa (framework) woła kod podklasy, a nie odwrotnie.
     *      Przykłady: JUnit woła metody @Test, HttpServlet.service() woła doGet(), JdbcTemplate woła RowMapper.
     *   4. 2 — lista [2, 3, 4] i element 4 jest na indeksie 2.
     *   5. 5 — add("x") liczy 1; addAll(2 elementy) dolicza 2 hurtowo, a odziedziczone addAll woła jeszcze add()
     *      dwa razy (po 1) = 1 + 2 + 2 = 5, zamiast oczekiwanych 3.
     *   6. Konstruktor bazowy wykona się przed ustawieniem pola podklasy, więc zobaczy null (albo 0/false). Nie wołaj
     *      nadpisywalnych metod z konstruktora.
     *   7. Oznaczenie metody generate jako final (i zostawienie podklasom tylko kroków/hooków).
     *   8. Gdy wariant ma się zmieniać w czasie działania, gdy zmienny jest jeden mały krok (wystarczy lambda) albo
     *      gdy nie chcesz wiązać klas dziedziczeniem — strategia i lambdy dają większą elastyczność.
     */
    // </editor-fold>
}
