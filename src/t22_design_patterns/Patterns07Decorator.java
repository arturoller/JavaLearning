package t22_design_patterns;

import helpers.Check;
import helpers.SampleData;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Decorator (dekorator) — dokładanie zachowania bez rozrostu podklas
 *        (decorator = dekorator, ozdabiacz; wrap = opakuj; delegate = obiekt, któremu przekazujemy pracę;
 *         layer = warstwa; cache = pamięć podręczna)
 *
 * W SKRÓCIE:
 *   Dekorator ma TEN SAM interfejs co obiekt, który opakowuje, trzyma go w polu i wokół wywołania dodaje
 *   coś od siebie (logowanie, cache, ponowienie, VAT, rabat). Można go nakładać warstwami w dowolnej
 *   kombinacji — w czasie działania programu, a nie przez dziedziczenie z góry ustalone w kodzie.
 *
 * ANALOGIA: kawa w kawiarni. Podstawa to espresso. Dokładasz mleko, syrop, bitą śmietanę — każdy dodatek
 *   „opakowuje” poprzedni napój i zmienia cenę oraz opis, a nadal dostajesz KAWĘ w kubku. Kawiarnia nie
 *   trzyma osobnego przepisu na „espresso z mlekiem i syropem bez śmietany” dla każdej kombinacji.
 *   Kolejność też bywa ważna: syrop przed mlekiem to nie to samo, co mleko przed syropem.
 *
 * JAK TO DZIAŁA:
 *   Role GoF:  Component (komponent) — wspólny interfejs;  ConcreteComponent — prawdziwy obiekt;
 *              Decorator (dekorator) — trzyma Component i sam jest Component;
 *              ConcreteDecorator — dodaje konkretne zachowanie przed/po wywołaniu delegata.
 *
 *      klient --> [Logowanie] --> [Cache] --> [Ponawianie] --> [Prawdziwe źródło]
 *                  każdy element implementuje TEN SAM interfejs i trzyma następny w polu
 *
 *   Wywołanie wchodzi przez najbardziej ZEWNĘTRZNY dekorator, a wynik wraca tą samą drogą. Zewnętrzny
 *   widzi wszystkie zapytania, wewnętrzny tylko te, które przepuściły warstwy nad nim (dlatego kolejność
 *   ma znaczenie).
 *
 * SŁÓWKA:
 *   decorator = dekorator; wrap = opakuj; inner = wewnętrzny; outer = zewnętrzny; delegate = delegat;
 *   surcharge/fee = dopłata/opłata; discount = rabat; retry = ponów; compress = skompresuj, zmniejsz;
 *   encrypt = zaszyfruj; proxy = pośrednik (zastępca); view = widok (nakładka bez kopiowania danych).
 *
 * ZOBACZ TEŻ: t13_lambdas/Lambda05Composition (andThen/compose jako dekoratory funkcji),
 *   t18_io_files/Io08BinaryStreams (strumienie IO to dekoratory),
 *   t19_annotations_reflection/Annotations06DynamicProxy (dekorator generowany w locie),
 *   t12_collections/Collections08ImmutableUnmodifiable (unmodifiableList = widok),
 *   t22_design_patterns/Patterns01Strategy (strategia zmienia algorytm, dekorator go otacza),
 *   t27_clean_code_pitfalls/CleanCode02Solid (zasada otwarte–zamknięte)
 * </pre>
 */
public class Patterns07Decorator {

    public static void main(String[] args) {
        title("Patterns07 — Decorator (dekorator)");

        subclassExplosion();     // subclass explosion = wybuch liczby podklas (problem)
        classicDecorator();      // classic decorator = klasyczny dekorator (ceny)
        dataSourceDecorators();  // data source decorators = dekoratory źródła danych
        orderMatters();          // order matters = kolejność ma znaczenie
        functionalDecorators();  // functional decorators = dekoratory funkcyjne
        jdkDecorators();         // jdk decorators = dekoratory w JDK
        proxyComparison();       // proxy comparison = dekorator a proxy
        pitfalls();              // pitfalls = pułapki
        testability();           // testability = łatwość testowania
        exercises();             // exercises = ćwiczenia
    }

    // =================================================================================================
    // Wspólne typy lekcji
    // =================================================================================================

    /** Komponent (Component): cena produktu w groszach (grosze = setne części złotego, 1 zł = 100 gr). */
    interface PriceSource {
        long priceOf(String sku); // priceOf = cena produktu; sku = kod produktu
    }

    /** Prawdziwy komponent: cena netto z katalogu przykładowych danych. */
    static final class CatalogPrice implements PriceSource {
        @Override
        public long priceOf(String sku) {
            return SampleData.productBySku(sku).price().movePointRight(2).longValueExact(); // zł → grosze
        }

        @Override
        public String toString() {
            return "Katalog";
        }
    }

    /**
     * Klasa bazowa dekoratorów ceny: trzyma opakowany obiekt (inner) i opisuje się razem z nim,
     * np. {@code Vat(Rabat(Katalog))} — to ogromnie pomaga w debugowaniu stosów dekoratorów.
     */
    abstract static class PriceDecorator implements PriceSource {
        protected final PriceSource inner; // inner = wewnętrzny, opakowany obiekt

        PriceDecorator(PriceSource inner) {
            this.inner = Objects.requireNonNull(inner);
        }

        abstract String label(); // label = nazwa do wypisania

        @Override
        public String toString() {
            return label() + "(" + inner + ")";
        }
    }

    /** VAT: cena * (100 + procent) / 100, zaokrąglona w górę od połówki (liczby całkowite, bez double). */
    static final class VatDecorator extends PriceDecorator {
        private final int percent; // percent = procent

        VatDecorator(PriceSource inner, int percent) {
            super(inner);
            this.percent = percent;
        }

        @Override
        public long priceOf(String sku) {
            return (inner.priceOf(sku) * (100 + percent) + 50) / 100;
        }

        @Override
        String label() {
            return "Vat" + percent;
        }
    }

    /** Rabat procentowy: cena * (100 - procent) / 100 (dzielenie całkowite, obcina grosze). */
    static final class DiscountDecorator extends PriceDecorator {
        private final int percent;

        DiscountDecorator(PriceSource inner, int percent) {
            super(inner);
            this.percent = percent;
        }

        @Override
        public long priceOf(String sku) {
            return inner.priceOf(sku) * (100 - percent) / 100;
        }

        @Override
        String label() {
            return "Rabat" + percent;
        }
    }

    /** Stała opłata doliczana do ceny (np. pakowanie). */
    static final class FeeDecorator extends PriceDecorator {
        private final long fee; // fee = opłata w groszach

        FeeDecorator(PriceSource inner, long fee) {
            super(inner);
            this.fee = fee;
        }

        @Override
        public long priceOf(String sku) {
            return inner.priceOf(sku) + fee;
        }

        @Override
        String label() {
            return "Opłata" + fee;
        }
    }

    /** Źródło wartości po kluczu (np. pobranie z wolnej bazy lub z sieci). */
    interface ValueSource {
        String read(String key); // read = odczytaj
    }

    /** Prawdziwe, „wolne” źródło: liczy, ile razy je naprawdę zapytano, i potrafi zawieść na początku. */
    static final class SlowSource implements ValueSource {
        int realCalls;           // realCalls = prawdziwe wywołania
        private int failuresLeft; // failuresLeft = ile razy jeszcze zawiedzie

        SlowSource(int failuresFirst) {
            this.failuresLeft = failuresFirst;
        }

        @Override
        public String read(String key) {
            realCalls++;
            if (failuresLeft > 0) {
                failuresLeft--;
                throw new IllegalStateException("awaria połączenia");
            }
            return "wartość:" + key;
        }

        @Override
        public String toString() {
            return "Źródło";
        }
    }

    /** Dekorator: zapisuje do dziennika każde zapytanie, które do niego dotarło. */
    static final class LoggingSource implements ValueSource {
        private final ValueSource inner;
        private final List<String> log;
        private final String tag; // tag = znacznik w dzienniku

        LoggingSource(ValueSource inner, List<String> log, String tag) {
            this.inner = inner;
            this.log = log;
            this.tag = tag;
        }

        @Override
        public String read(String key) {
            log.add("[" + tag + "] odczyt " + key);
            return inner.read(key);
        }
    }

    /** Dekorator: pamięta wyniki (cache) — to samo pytanie nie trafia drugi raz do źródła. */
    static final class CachingSource implements ValueSource {
        private final ValueSource inner;
        private final Map<String, String> cache = new HashMap<>(); // nigdy go nie wypisujemy

        CachingSource(ValueSource inner) {
            this.inner = inner;
        }

        @Override
        public String read(String key) {
            String cached = cache.get(key); // cached = z pamięci podręcznej
            if (cached == null) {
                cached = inner.read(key);
                cache.put(key, cached);
            }
            return cached;
        }
    }

    /** Dekorator: ponawia odczyt do {@code attempts} razy, gdy źródło rzuci IllegalStateException. */
    static final class RetryingSource implements ValueSource {
        private final ValueSource inner;
        private final int attempts; // attempts = liczba prób

        RetryingSource(ValueSource inner, int attempts) {
            this.inner = inner;
            this.attempts = attempts;
        }

        @Override
        public String read(String key) {
            IllegalStateException last = null; // last = ostatni błąd
            for (int i = 0; i < attempts; i++) {
                try {
                    return inner.read(key);
                } catch (IllegalStateException ex) {
                    last = ex;
                }
            }
            throw Objects.requireNonNull(last);
        }
    }

    // =================================================================================================
    // 1. PROBLEM — wybuch liczby podklas
    // =================================================================================================

    /**
     * 1. Problem: chcemy cenę z VAT, z rabatem, z opłatą — w KAŻDEJ kombinacji. Dziedziczenie wymaga osobnej
     * klasy na kombinację (VatPrice, DiscountPrice, VatDiscountPrice, ...), a lista flag w jednej klasie
     * kończy jako drabina if-ów.
     */
    static void subclassExplosion() {
        section("1. Problem — wybuch liczby podklas");

        // Cech: VAT, rabat, opłata... Każda kombinacja niepustego podzbioru to osobna podklasa: 2^n - 1.
        for (int features = 1; features <= 5; features++) { // features = liczba cech
            show(features + " cech", ((1 << features) - 1) + " podklas");
        }
        // WYNIK: 1 cech → 1 podklas
        // WYNIK: 2 cech → 3 podklas
        // WYNIK: 3 cech → 7 podklas
        // WYNIK: 4 cech → 15 podklas
        // WYNIK: 5 cech → 31 podklas

        // Dziedziczenie jest wybierane przy kompilacji: nie zmienisz „klasy ceny” już istniejącego obiektu,
        // a kolejność dodatków jest zaszyta w hierarchii (VatThenFee ≠ FeeThenVat — to dwie osobne klasy).
        // Alternatywa „flagi w jednej klasie” (new PriceCalc(true, false, true)) daje bezsensowną drabinę if-ów
        // i boolean trap (nie wiadomo, co znaczy trzecie true) — por. t22 Patterns02Builder.
        // DOBRA PRAKTYKA: kompozycja zamiast dziedziczenia (t06) — dodawane zachowania składaj z małych
        //   klocków, które można układać w dowolnej kolejności — to właśnie robi dekorator.
    }

    // =================================================================================================
    // 2. KLASYCZNY DEKORATOR
    // =================================================================================================

    /**
     * 2. Klasyczny dekorator: ten sam interfejs, opakowany obiekt w polu, dodatek wokół wywołania.
     * Trzy małe klasy zastępują siedem podklas — a kombinacje składamy w miejscu użycia.
     */
    static void classicDecorator() {
        section("2. Klasyczny dekorator — ceny");

        PriceSource catalog = new CatalogPrice();
        show("netto (Czysty kod, gr)", catalog.priceOf("KSI-001"));
        // WYNIK: netto (Czysty kod, gr) → 7900

        PriceSource withDiscount = new DiscountDecorator(catalog, 10);
        show("rabat 10%", withDiscount.priceOf("KSI-001"));
        // WYNIK: rabat 10% → 7110

        PriceSource withVatToo = new VatDecorator(withDiscount, 23);
        show("rabat 10% i VAT 23%", withVatToo.priceOf("KSI-001"));
        // WYNIK: rabat 10% i VAT 23% → 8745
        show("stos dekoratorów", withVatToo);
        // WYNIK: stos dekoratorów → Vat23(Rabat10(Katalog))

        // Każdy dekorator jest PriceSource, więc klient (np. koszyk) nie wie, ile warstw ma przed sobą.
        // Dopisanie nowego dodatku (np. kod rabatowy) to nowa klasa — istniejące zostają bez zmian,
        // czyli zasada otwarte–zamknięte (t27 CleanCode02Solid).
        // UWAGA na liczby: pieniądze liczymy tu na groszach w long, bez double (t15 Numbers01BigDecimal) —
        //   1 zł = 100 gr; zaokrąglenie VAT w górę od połówki robi „+ 50” przed dzieleniem przez 100.

        // Kiedy klasyczna wersja jest lepsza od lambdy: dekorator ma STAN (licznik, cache), parametry,
        // nazwę wartą testowania osobno, albo opakowywany interfejs ma kilka metod (lambda wymaga interfejsu
        // z jedną metodą).
    }

    // =================================================================================================
    // 3. LOGOWANIE, CACHE, PONAWIANIE
    // =================================================================================================

    /**
     * 3. Realistyczny stos: wolne źródło danych owinięte w pamięć podręczną, ponawianie i logowanie.
     * Źródło o tym nie wie — każda warstwa robi jedną rzecz (zasada jednej odpowiedzialności).
     */
    static void dataSourceDecorators() {
        section("3. Źródło danych — logowanie, cache, ponawianie");

        List<String> log = new ArrayList<>();
        SlowSource real = new SlowSource(2); // zawiedzie dwa razy
        ValueSource stack = new LoggingSource(new CachingSource(new RetryingSource(real, 3)), log, "log");

        show("pierwszy odczyt", stack.read("kawa"));
        // WYNIK: pierwszy odczyt → wartość:kawa
        show("prawdziwych wywołań źródła", real.realCalls);
        // WYNIK: prawdziwych wywołań źródła → 3
        show("drugi odczyt (z cache)", stack.read("kawa"));
        // WYNIK: drugi odczyt (z cache) → wartość:kawa
        show("prawdziwych wywołań po cache", real.realCalls);
        // WYNIK: prawdziwych wywołań po cache → 3
        show("dziennik", log);
        // WYNIK: dziennik → [[log] odczyt kawa, [log] odczyt kawa]

        // Pierwszy odczyt kosztował 3 prawdziwe wywołania (dwie awarie + sukces) — o tym wie tylko Retry.
        // Drugi nie dotarł do źródła w ogóle — odpowiedział cache. Logowanie na wierzchu widzi OBA zapytania.

        ValueSource weak = new RetryingSource(new SlowSource(5), 3);
        expectThrows("za mało prób", () -> weak.read("x"));
        // WYNIK: ✔ za mało prób → rzucono IllegalStateException: awaria połączenia
        // PUŁAPKA: ponawianie bez limitu i bez przerwy to „burza ponowień”: padnięty serwer dostaje coraz więcej
        //   zapytań i nie może wstać. Dlatego ponawiamy kilka razy, zwykle z rosnącą przerwą, i NIGDY operacji,
        //   które nie są bezpieczne do powtórzenia (np. „pobierz opłatę” bez klucza idempotentności).
        // Spring: @Cacheable (cache), @Retryable (ponawianie), @Transactional — to te same dekoratory, tylko
        //   generowane automatycznie jako proxy wokół beana (SpringLearning).
    }

    // =================================================================================================
    // 4. KOLEJNOŚĆ MA ZNACZENIE
    // =================================================================================================

    /** Komponent tekstowy: zwraca to, co faktycznie „poszło w świat” (na przewód). */
    interface TextChannel {
        String send(String text); // send = wyślij
    }

    /** Prawdziwy komponent: nic nie zmienia, tylko zwraca otrzymany tekst. */
    record PlainChannel() implements TextChannel {
        @Override
        public String send(String text) {
            return text;
        }
    }

    /** Dekorator: kompresja (tu zabawkowa), zanim tekst pójdzie dalej do wnętrza. */
    record CompressingChannel(TextChannel inner) implements TextChannel {
        @Override
        public String send(String text) {
            return inner.send(compress(text));
        }
    }

    /** Dekorator: szyfrowanie (tu zabawkowe), zanim tekst pójdzie dalej do wnętrza. */
    record EncryptingChannel(TextChannel inner) implements TextChannel {
        @Override
        public String send(String text) {
            return inner.send(encrypt(text));
        }
    }

    /** Zabawkowa kompresja RLE (run-length encoding = kodowanie długości serii): "aaab" → "a3b"; serie do 9. */
    static String compress(String text) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            int run = 1; // run = długość serii takich samych znaków
            while (i + run < text.length() && text.charAt(i + run) == text.charAt(i) && run < 9) {
                run++;
            }
            out.append(text.charAt(i));
            if (run > 1) {
                out.append(run);
            }
            i += run;
        }
        return out.toString();
    }

    /** Zabawkowe „szyfrowanie”: znak na pozycji i przesuwamy o (i % 3) + 1. NIE do prawdziwych tajemnic! */
    static String encrypt(String text) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            out.append((char) (text.charAt(i) + (i % 3) + 1));
        }
        return out.toString();
    }

    /**
     * 4. Kolejność dekoratorów. Najbardziej zewnętrzny działa na dane jako pierwszy. Dwa przykłady:
     * ceny (opłata stała i VAT) oraz kompresja i szyfrowanie.
     */
    static void orderMatters() {
        section("4. Kolejność warstw ma znaczenie");

        PriceSource catalog = new CatalogPrice();
        PriceSource feeThenVat = new VatDecorator(new FeeDecorator(catalog, 1000), 23); // VAT na (cena + opłata)
        PriceSource vatThenFee = new FeeDecorator(new VatDecorator(catalog, 23), 1000); // opłata po VAT-cie
        show(feeThenVat.toString(), feeThenVat.priceOf("DOM-002"));
        // WYNIK: Vat23(Opłata1000(Katalog)) → 17097
        show(vatThenFee.toString(), vatThenFee.priceOf("DOM-002"));
        // WYNIK: Opłata1000(Vat23(Katalog)) → 16867
        // Ten sam zestaw dekoratorów, inna kolejność → inna cena (VAT od opłaty czy sama opłata).
        // To nie błąd, tylko decyzja biznesowa: kolejność trzeba zapisać i przetestować.

        String text = "aaaaaaaabbbbbbbb"; // 8 razy 'a' i 8 razy 'b'
        TextChannel encryptThenCompress = new EncryptingChannel(new CompressingChannel(new PlainChannel()));
        TextChannel compressThenEncrypt = new CompressingChannel(new EncryptingChannel(new PlainChannel()));
        // Uwaga: ZEWNĘTRZNY dekorator działa na tekście pierwszy. Dlatego Encrypting(Compressing(...)) to
        // „najpierw szyfruj, potem kompresuj”, a Compressing(Encrypting(...)) — „najpierw kompresuj, potem szyfruj”.
        // Czytaj więc kod OD ZEWNĄTRZ DO ŚRODKA — to odwrotnie niż kolejność nawiasów w intuicji „co jest pierwsze”.
        String a = encryptThenCompress.send(text); // szyfr → kompresja
        String b = compressThenEncrypt.send(text); // kompresja → szyfr
        show("długość oryginału", text.length());
        // WYNIK: długość oryginału → 16
        show("kompresja → szyfr, długość", b.length());
        // WYNIK: kompresja → szyfr, długość → 4
        show("szyfr → kompresja, długość", a.length());
        // WYNIK: szyfr → kompresja, długość → 16

        // Dlaczego tak wyszło: po szyfrowaniu znaki przestają się powtarzać, więc kompresja nic nie znajduje.
        // To prawda także dla prawdziwej kryptografii: dobrze zaszyfrowane dane wyglądają jak losowe i nie
        // dają się skompresować — więc kompresuje się PRZED szyfrowaniem, nigdy po nim.
        // Przy ODBIORZE kolejność jest odwrotna (najpierw odszyfruj, potem rozpakuj) — dekoratory odbioru
        // trzeba składać w lustrzanej kolejności. Gubienie tej symetrii to klasyczny błąd.
        // PUŁAPKA: ktoś „porządkuje” kod i zamienia miejscami dwie warstwy. Program nadal się kompiluje i działa,
        //   tylko wynik jest gorszy albo zły. DOBRA PRAKTYKA: stos buduj w JEDNYM miejscu (metoda fabryczna
        //   albo konfiguracja) z komentarzem o kolejności i testem, który ją chroni.

        // Podobnie przy logowaniu i cache (sekcja 3): logowanie NA WIERZCHU widzi wszystkie zapytania,
        // logowanie POD cache — tylko te, które cache przepuścił.
        List<String> outerLog = new ArrayList<>();
        List<String> innerLog = new ArrayList<>();
        ValueSource loggingOutside = new LoggingSource(new CachingSource(new SlowSource(0)), outerLog, "na wierzchu");
        ValueSource loggingInside = new CachingSource(new LoggingSource(new SlowSource(0), innerLog, "pod cache"));
        for (int i = 0; i < 3; i++) {
            loggingOutside.read("k");
            loggingInside.read("k");
        }
        show("wpisów (logowanie na wierzchu)", outerLog.size());
        // WYNIK: wpisów (logowanie na wierzchu) → 3
        show("wpisów (logowanie pod cache)", innerLog.size());
        // WYNIK: wpisów (logowanie pod cache) → 1
    }

    // =================================================================================================
    // 5. DEKORATORY FUNKCYJNE
    // =================================================================================================

    /** Dekorator jako funkcja: opakowuje PriceSource w lambdę (PriceSource ma jedną metodę). */
    static PriceSource withFee(PriceSource inner, long fee) {
        return sku -> inner.priceOf(sku) + fee;
    }

    /** Dekorator z pamięcią: computeIfAbsent = oblicz, jeśli brakuje klucza (Java 8+). */
    static PriceSource memoized(PriceSource inner) { // memoized = z zapamiętywaniem wyników
        Map<String, Long> cache = new HashMap<>();
        return sku -> cache.computeIfAbsent(sku, inner::priceOf);
    }

    /** Składa listę kroków w jedną funkcję: kroki wykonują się po kolei, pierwszy jako pierwszy. */
    static <T> UnaryOperator<T> chain(List<UnaryOperator<T>> steps) { // chain = łańcuch
        return input -> {
            T result = input;
            for (UnaryOperator<T> step : steps) {
                result = step.apply(result);
            }
            return result;
        };
    }

    /**
     * 5. Funkcyjnie: dla interfejsów z jedną metodą dekorator to często zwykła funkcja przyjmująca i zwracająca
     * ten sam typ. W JDK służą do tego andThen i compose (t13 Lambda05Composition) oraz UnaryOperator.
     */
    static void functionalDecorators() {
        section("5. Dekoratory funkcyjne — lambdy, andThen, UnaryOperator");

        int[] realCalls = {0};
        PriceSource counting = sku -> { // counting = liczący wywołania (efekt uboczny tylko do demonstracji)
            realCalls[0]++;
            return 10_000L;
        };
        PriceSource decorated = memoized(withFee(counting, 500));
        show("cena", decorated.priceOf("X"));
        // WYNIK: cena → 10500
        show("cena drugi raz", decorated.priceOf("X"));
        // WYNIK: cena drugi raz → 10500
        show("wywołań źródła", realCalls[0]);
        // WYNIK: wywołań źródła → 1

        // andThen i compose — dwa kierunki składania:
        Function<String, String> trim = String::trim;           // trim = obetnij spacje z brzegów
        Function<String, String> upper = s -> s.toUpperCase(java.util.Locale.ROOT); // upper = wielkie litery
        show("trim.andThen(upper)", trim.andThen(upper).apply("  kawa "));
        // WYNIK: trim.andThen(upper) → KAWA
        show("upper.compose(trim)", upper.compose(trim).apply("  kawa "));
        // WYNIK: upper.compose(trim) → KAWA
        // f.andThen(g) = g(f(x)) — najpierw f.   f.compose(g) = f(g(x)) — najpierw g. Wynik tu ten sam,
        // ale przy krokach niezamiennych (np. „dodaj opłatę” i „doliczVAT”) kolejność zmienia wynik.

        UnaryOperator<String> exclaim = s -> s + "!"; // exclaim = dodaj wykrzyknik
        UnaryOperator<String> pipeline = chain(List.of(String::trim, exclaim, exclaim));
        show("potok z listy kroków", pipeline.apply("  hej "));
        // WYNIK: potok z listy kroków → hej!!

        // Lista kroków (List<UnaryOperator<T>>) ma jedną zaletę nad klasami: potok można zbudować z konfiguracji
        // (np. nazwy kroków z pliku .properties), bez zmiany kodu. Wada: trudniej o nazwę i stan kroku.
        // DOBRA PRAKTYKA: dla jednej-dwóch warstw wystarczy lambda; gdy dekorator rośnie (stan, parametry,
        //   kilka metod w interfejsie) — zrób z niego klasę z nazwą.
    }

    // =================================================================================================
    // 6. DEKORATORY W JDK
    // =================================================================================================

    /**
     * 6. JDK jest pełne dekoratorów. Strumienie java.io: każdy wrapper dokłada jedną zdolność
     * (Buffered = bufor, Data = typy proste). Collections.unmodifiableList to dekorator-widok.
     */
    static void jdkDecorators() {
        section("6. Dekoratory w JDK — strumienie i kolekcje");

        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(); // bytes = bajty w pamięci
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(bytes))) {
                out.writeInt(258);       // 4 bajty
                out.writeUTF("kawa");    // 2 bajty długości + 4 bajty tekstu
            } // zamknięcie zewnętrznego strumienia zamyka (i opróżnia) wszystkie warstwy
            show("zapisano bajtów", bytes.size());
            // WYNIK: zapisano bajtów → 10

            try (DataInputStream in = new DataInputStream(new BufferedInputStream(new ByteArrayInputStream(bytes.toByteArray())))) {
                show("odczytano liczbę", in.readInt());
                // WYNIK: odczytano liczbę → 258
                show("odczytano tekst", in.readUTF());
                // WYNIK: odczytano tekst → kawa
            }

            try (BufferedReader reader = new BufferedReader(new StringReader("linia 1\nlinia 2"))) {
                show("BufferedReader.readLine", reader.readLine() + " | " + reader.readLine());
                // WYNIK: BufferedReader.readLine → linia 1 | linia 2
            }
        } catch (IOException e) { // IOException = błąd wejścia-wyjścia (kontrolowany)
            throw new UncheckedIOException(e);
        }
        // Każda warstwa to dekorator tego samego typu bazowego (InputStream / OutputStream / Reader): Buffered
        // dokłada bufor, Data — odczyt int/UTF, GZIP — kompresję, CipherOutputStream — szyfrowanie (t18 Io08BinaryStreams).
        // Stos można składać w dowolnej kolejności: BufferedInputStream(GZIPInputStream(plik)) ≠ odwrotnie.
        // PUŁAPKA: wołanie close() tylko na wewnętrznym strumieniu pomija opróżnienie bufora zewnętrznego i dane
        //   zostają w buforze (nigdy nie trafiają do celu). DOBRA PRAKTYKA: try-with-resources na ZEWNĘTRZNYM
        //   strumieniu — zamknięcie propaguje się do środka.

        List<Integer> original = new ArrayList<>(List.of(1, 2));
        List<Integer> view = Collections.unmodifiableList(original); // view = widok tylko do odczytu
        original.add(3);
        show("widok widzi zmianę oryginału", view);
        // WYNIK: widok widzi zmianę oryginału → [1, 2, 3]
        expectThrows("add przez widok", () -> view.add(4));
        // WYNIK: ✔ add przez widok → rzucono UnsupportedOperationException: (brak komunikatu)
        // unmodifiableList to dekorator-WIDOK: nie kopiuje danych, tylko blokuje metody modyfikujące.
        // Kto trzyma oryginał, nadal go zmienia i widok to pokaże! Kopia niezmienna: List.copyOf (Java 10+),
        // por. t12 Collections08ImmutableUnmodifiable.
        // Collections.synchronizedList opakowuje każdą metodę w synchronized (blokada na jednym obiekcie) —
        // ale iteracja po takiej liście nadal wymaga ręcznej synchronizacji (t21 Concurrency06ConcurrentCollections).
        // Podobne dekoratory: Collections.checkedList, Collections.synchronizedMap, BufferedReader(InputStreamReader).
    }

    // =================================================================================================
    // 7. DEKORATOR, PROXY, DZIEDZICZENIE
    // =================================================================================================

    /**
     * 7. Dekorator a proxy a dziedziczenie. Proxy (pośrednik) z java.lang.reflect to dekorator generowany
     * w czasie działania dla DOWOLNEGO interfejsu — jedna klasa logująca zamiast jednej na każdy interfejs.
     */
    static void proxyComparison() {
        section("7. Dekorator a proxy a dziedziczenie");

        PriceSource real = new CatalogPrice();
        List<String> log = new ArrayList<>();
        PriceSource logged = (PriceSource) Proxy.newProxyInstance( // newProxyInstance = utwórz proxy
                PriceSource.class.getClassLoader(),
                new Class<?>[] {PriceSource.class},
                (proxy, method, methodArgs) -> {
                    log.add("wywołano " + method.getName() + Arrays.toString(methodArgs));
                    return method.invoke(real, methodArgs);
                });
        show("cena przez proxy", logged.priceOf("SPO-002"));
        // WYNIK: cena przez proxy → 749
        show("dziennik proxy", log);
        // WYNIK: dziennik proxy → [wywołano priceOf[SPO-002]]

        // Porównanie:
        //   | cecha            | Dekorator (ręczny)             | Proxy dynamiczne / Spring AOP      | Dziedziczenie        |
        //   | kiedy powstaje   | kod pisany przez nas           | generowane w czasie działania      | w czasie kompilacji  |
        //   | wiele metod      | trzeba napisać każdą (nudne)   | jedna obsługa dla wszystkich       | dziedziczysz wszystko|
        //   | zmiana kolejności| tak, w miejscu składania       | tak (kolejność aspektów)           | nie                  |
        //   | typowy cel       | nowe zachowanie / składanie    | przekrojowe: log, transakcje, cache| specjalizacja typu   |
        // Różnica intencji: dekorator DODAJE zachowanie, a proxy KONTROLUJE dostęp (leniwe tworzenie, uprawnienia,
        // zdalne wywołanie) — kod bywa identyczny, dlatego granica jest miękka. Spring używa proxy do dekorowania
        // beanów (@Transactional, @Cacheable); przy braku interfejsu — podklasy generowanej biblioteką CGLIB.
        // Dlaczego nie dziedziczyć: klasa bazowa jest ustalona raz, nie da się jej opakować kilka razy
        //   ani zmienić kolejności; podklasa zależy od szczegółów implementacji (kruche, t06).
    }

    // =================================================================================================
    // 8. PUŁAPKI
    // =================================================================================================

    /**
     * 8. Pułapki: dekorator to INNY obiekt niż opakowany. Przestaje działać porównanie przez equals,
     * a sprawdzenie typu (instanceof) widzi dekorator, nie prawdziwy obiekt.
     */
    static void pitfalls() {
        section("8. Pułapki — equals, instanceof, zbyt wiele warstw");

        PriceSource catalog = new CatalogPrice();
        PriceSource wrapped = new FeeDecorator(catalog, 0); // opłata zero — zachowuje się tak samo jak katalog
        show("te same ceny", catalog.priceOf("ELE-003") == wrapped.priceOf("ELE-003"));
        // WYNIK: te same ceny → true
        show("catalog.equals(wrapped)", catalog.equals(wrapped));
        // WYNIK: catalog.equals(wrapped) → false
        show("wrapped instanceof CatalogPrice", wrapped instanceof CatalogPrice);
        // WYNIK: wrapped instanceof CatalogPrice → false
        show("catalog instanceof CatalogPrice", catalog instanceof CatalogPrice);
        // WYNIK: catalog instanceof CatalogPrice → true

        // PUŁAPKA: identyczność i equals. Dekorator nie jest obiektem opakowanym: domyślne equals porównuje
        //   referencje, więc „ten sam” obiekt po owinięciu przestaje być równy sobie — wrzucony do Set czy
        //   użyty jako klucz Map może „zniknąć”. Dlaczego: to dwa różne obiekty. Nie nadpisuj equals dekoratora
        //   „na odwagę” — łatwo złamać symetrię (a.equals(b) ≠ b.equals(a)); ustal, kto jest kluczem (np. SKU).
        // Kolejna pułapka — instanceof i rzutowanie na typ konkretny. Kod, który robi `if (source instanceof CatalogPrice)`,
        //   po dodaniu dekoratora przestaje działać — stąd zasada: klient zna tylko interfejs. Metody dodatkowe
        //   prawdziwej klasy (spoza interfejsu) są po owinięciu niewidoczne.
        // Kolejna pułapka — zbyt wiele warstw. Stos z 12 dekoratorów jest nieczytelny, a każdy błąd ma stos wywołań
        //   (stack trace) długi jak powieść. Obrona: toString opisujący stos (patrz sekcja 2),
        //   jedno miejsce składania, rozsądny limit warstw i nazwy mówiące, co robią.
        // Kolejna pułapka — dekorator zapomina przekazać jedną z metod interfejsu — w interfejsie z 10 metodami
        //   łatwo o pominięcie; wtedy dekorowany obiekt „zgubił” część zachowania. (Dlatego dla szerokich
        //   interfejsów lepsze jest proxy dynamiczne lub klasa bazowa z domyślnym przekazywaniem wszystkiego.)
        // Kolejna pułapka — nadużycie — dekorator tam, gdzie wystarczy parametr albo jeden if. YAGNI: jeśli dodatek
        //   jest jeden i na zawsze, wpisz go do klasy.
    }

    // =================================================================================================
    // 9. TESTOWANIE
    // =================================================================================================

    /**
     * 9. Testowanie: każdy dekorator testujemy z atrapą (fake) w środku — bez bazy i sieci. To jedna z głównych
     * zalet wzorca: warstwy są małe i niezależne.
     */
    static void testability() {
        section("9. Testowanie — dekorator z atrapą w środku");

        int[] calls = {0};
        ValueSource fake = key -> { // fake = atrapa: licznik i stała odpowiedź
            calls[0]++;
            return "A-" + key;
        };
        ValueSource cached = new CachingSource(fake);

        Check.equal("pierwsza odpowiedź", "A-k", () -> cached.read("k"));
        Check.equal("druga odpowiedź", "A-k", () -> cached.read("k"));
        Check.equal("atrapa wywołana raz", 1, () -> calls[0]);
        Check.equal("nowy klucz wywołuje atrapę", "A-z", () -> cached.read("z"));
        Check.equal("razem dwa wywołania", 2, () -> calls[0]);
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD

        // Test dekoratora: sprawdzamy TYLKO jego zadanie (cache), bez prawdziwego źródła. Test całego stosu
        // to osobny, rzadszy test integracyjny. Podobnie RetryingSource testuje się atrapą, która zawodzi
        // n razy — deterministycznie, bez czekania na prawdziwą awarię.

        // KIEDY UŻYWAĆ / KIEDY NIE:
        //   | używaj, gdy                                         | nie używaj, gdy                                  |
        //   | dodatki mają się łączyć w dowolnych kombinacjach    | dodatek jest jeden i zawsze włączony             |
        //   | zachowanie ma być dokładane w czasie działania      | interfejs ma bardzo wiele metod (nudne przekazy) |
        //   | przekrojowe sprawy: log, cache, retry, metryki      | potrzebujesz zmienić WEWNĘTRZNY algorytm (strategia) |
        //   | nie możesz lub nie chcesz zmieniać klasy bazowej    | wystarczy parametr lub zwykły if                 |
        // Strategia kontra dekorator: strategia ZASTĘPUJE algorytm (zmienia „jak”), dekorator OTACZA go
        // (dodaje „coś jeszcze”) — t22 Patterns01Strategy.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Dekorator = ten sam interfejs + opakowany obiekt w polu + dodatek przed/po wywołaniu delegata.
     *   • Zastępuje wybuch podklas (2^n - 1) kombinacją małych klocków składanych w miejscu użycia.
     *   • Kolejność warstw zmienia wynik (opłata i VAT; kompresja przed szyfrowaniem; log na wierzchu czy pod cache).
     *   • Dla interfejsów jednometodowych dekorator może być lambdą/funkcją; andThen/compose, UnaryOperator.
     *   • W JDK: strumienie java.io (Buffered, Data, GZIP), Collections.unmodifiableList/synchronizedList (widoki).
     *   • Proxy dynamiczne (t19) to dekorator generowany w locie; Spring: @Transactional, @Cacheable.
     *   • Pułapki: equals/instanceof po owinięciu, zbyt wiele warstw, zapomniana metoda, brak toString stosu.
     *   • Strategia zmienia algorytm, dekorator go otacza.
     *
     * PYTANIA KONTROLNE:
     *   1. Ile podklas potrzeba przy 4 niezależnych cechach, jeśli każdą kombinację robimy dziedziczeniem?
     *   2. Czym różni się dekorator od strategii? Podaj przykład dla ceny.
     *   3. Co wypisze:
     *        PriceSource p = new FeeDecorator(new DiscountDecorator(new CatalogPrice(), 50), 100);
     *        System.out.println(p.priceOf("KSI-001"));   // Czysty kod kosztuje 79,00 zł; rabat dzieli całkowicie
     *   4. Dlaczego kompresuje się PRZED szyfrowaniem, a nie po?
     *   5. ZNAJDŹ BŁĄD:  Set<PriceSource> set = new HashSet<>(); set.add(src); ... set.contains(new FeeDecorator(src, 0));
     *        — programista oczekuje true. Co się stanie i dlaczego?
     *   6. Co wypisze:   List<Integer> o = new ArrayList<>(List.of(1)); var v = Collections.unmodifiableList(o);
     *        o.add(2); System.out.println(v);
     *   7. ZNAJDŹ BŁĄD:  var out = new DataOutputStream(new BufferedOutputStream(file));
     *        out.writeInt(1);  file.close();   // file to FileOutputStream — dlaczego liczba może nie trafić do pliku?
     *   8. Który wariant da więcej wpisów w dzienniku: logowanie na wierzchu cache czy pod nim, i dlaczego?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: rabat i VAT", 8745L, () -> exercise1());
        Check.equal("ćw. 2: opłata jako lambda", 8400L, () -> exercise2(new CatalogPrice()).priceOf("KSI-001"));
        Check.equal("ćw. 3: cache + dziennik", List.of("odczyt k", "odczyt z"), () -> exercise3());
        Check.equal("ćw. 4: ponawianie (sukces)", "wartość:k", () -> exercise4(new SlowSource(2), 3).read("k"));
        Check.throwsException("ćw. 4: ponawianie (za mało prób)", IllegalStateException.class,
                () -> exercise4(new SlowSource(2), 2).read("k"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 8745L, () -> solution1());
        Check.equal("ćw. 2 (wzorzec)", 8400L, () -> solution2(new CatalogPrice()).priceOf("KSI-001"));
        Check.equal("ćw. 3 (wzorzec)", List.of("odczyt k", "odczyt z"), () -> solution3());
        Check.equal("ćw. 4 (wzorzec, sukces)", "wartość:k", () -> solution4(new SlowSource(2), 3).read("k"));
        Check.throwsException("ćw. 4 (wzorzec, za mało prób)", IllegalStateException.class,
                () -> solution4(new SlowSource(2), 2).read("k"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): Zbuduj stos: katalog → rabat 10% → VAT 23% (użyj gotowych klas dekoratorów)
     * i zwróć cenę produktu KSI-001 w groszach.
     * Podpowiedź: zewnętrzny dekorator tworzysz jako ostatni: {@code new VatDecorator(new DiscountDecorator(...), 23)}.
     */
    static long exercise1() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): Napisz dekorator jako lambdę: zwraca cenę z {@code base} powiększoną o stałą opłatę
     * 500 groszy.
     * Podpowiedź: {@code PriceSource} ma jedną metodę, więc wystarczy {@code sku -> ...}.
     */
    static PriceSource exercise2(PriceSource base) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie, PRZEPISZ): Stary kod ma jedną klasę „LoggingCachingSource extends SlowSource”
     * (dziedziczenie, nie da się zmienić kolejności ani użyć z innym źródłem). Przepisz to na dekoratory:
     * zbuduj źródło z {@code CachingSource} na zewnątrz i {@code LoggingSource} w środku (tag "log"),
     * a następnie odczytaj klucze k, k, z, z, k. Zwróć dziennik z wpisami bez znacznika
     * (czyli tylko zapytania, które dotarły do logowania, np. {@code "odczyt k"}).
     * <pre>{@code
     * // PRZED:
     * class LoggingCachingSource extends SlowSource { ... log + cache wymieszane w jednej klasie ... }
     * }</pre>
     * Podpowiedź: dziennik zawiera linie typu {@code "[log] odczyt k"} — usuń prefiks {@code "[log] "}.
     */
    static List<String> exercise3() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): Napisz dekorator ponawiania jako lambdę: {@code attempts} razy próbuje
     * {@code source.read(key)}; po IllegalStateException próbuje znowu; jeśli wszystkie próby zawiodą,
     * rzuca ostatni błąd.
     * Podpowiedź: pętla for z try/catch, zapamiętaj ostatni wyjątek w zmiennej {@code last}.
     */
    static ValueSource exercise4(ValueSource source, int attempts) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static long solution1() {
        PriceSource stack = new VatDecorator(new DiscountDecorator(new CatalogPrice(), 10), 23);
        return stack.priceOf("KSI-001");
    }

    static PriceSource solution2(PriceSource base) {
        return sku -> base.priceOf(sku) + 500;
    }

    static List<String> solution3() {
        List<String> log = new ArrayList<>();
        ValueSource source = new CachingSource(new LoggingSource(new SlowSource(0), log, "log"));
        for (String key : List.of("k", "k", "z", "z", "k")) {
            source.read(key);
        }
        List<String> cleaned = new ArrayList<>(); // cleaned = oczyszczone z prefiksu
        for (String entry : log) {
            cleaned.add(entry.replace("[log] ", ""));
        }
        return cleaned;
    }

    static ValueSource solution4(ValueSource source, int attempts) {
        return key -> {
            IllegalStateException last = null;
            for (int i = 0; i < attempts; i++) {
                try {
                    return source.read(key);
                } catch (IllegalStateException ex) {
                    last = ex;
                }
            }
            throw Objects.requireNonNull(last);
        };
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. 2^4 - 1 = 15 podklas (każdy niepusty podzbiór cech).
     *   2. Strategia ZASTĘPUJE sposób liczenia (np. rabat procentowy zamiast kwotowego); dekorator OTACZA
     *      istniejące liczenie dodatkiem (np. dolicz opłatę po policzeniu rabatu).
     *   3. 4050 — rabat 50% z 7900 gr daje 3950 (7900 * 50 / 100), a opłata 100 gr daje 4050.
     *   4. Zaszyfrowane dane nie mają powtórzeń, więc kompresja nic nie zyskuje; skompresowane dane da się
     *      zaszyfrować bez straty. Kompresja przed szyfrowaniem.
     *   5. contains zwróci false: FeeDecorator nie nadpisuje equals/hashCode, więc nowy dekorator to inny obiekt
     *      (a samo src też nie jest „równe” dekoratorowi). Dekorator nie jest tożsamy z opakowanym obiektem.
     *   6. [1, 2] — unmodifiableList to widok na oryginał, a nie kopia.
     *   7. Zamknięto tylko WEWNĘTRZNY strumień (file), a bufor BufferedOutputStream nigdy nie został opróżniony —
     *      bajty zostały w pamięci. Trzeba zamknąć (lub opróżnić flush) strumień ZEWNĘTRZNY: jego close()
     *      opróżnia bufor i zamyka warstwy pod spodem (najlepiej try-with-resources).
     *   8. Na wierzchu cache: loguje każde zapytanie (3 wpisy dla 3 odczytów); pod cache: tylko pierwszy (1 wpis),
     *      bo kolejne odczyty cache obsługuje sam i nie woła warstwy pod spodem.
     */
    // </editor-fold>
}
