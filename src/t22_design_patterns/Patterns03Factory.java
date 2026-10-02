package t22_design_patterns;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Factory (fabryka, metoda wytwórcza) — kto i jak tworzy obiekty
 *        (factory = fabryka; static factory method = statyczna metoda wytwórcza; Simple Factory = prosta fabryka;
 *         Factory Method = metoda wytwórcza; Abstract Factory = fabryka abstrakcyjna; registry = rejestr)
 *
 * W SKRÓCIE:
 *   Samo „new” przywiązuje kod do KONKRETNEJ klasy i nie ma nazwy. Fabryka to metoda (albo obiekt),
 *   która tworzy obiekty za nas: ma czytelną nazwę, może zwrócić podtyp, może użyć pamięci podręcznej
 *   i ukrywa, którą klasę faktycznie tworzymy. Wzorców „fabrycznych” jest kilka — każdy odpowiada
 *   na inne pytanie.
 *
 * ANALOGIA: zamawiasz taksówkę przez aplikację. Nie mówisz „chcę auto Toyota Corolla, rocznik 2019,
 *   kierowca Zenon” (to jest new). Mówisz „taksówka na lotnisko” (fabryka), a aplikacja sama dobiera
 *   pojazd i kierowcę. Jutro w flocie pojawi się samochód elektryczny, a Twoje zamówienie się nie zmieni.
 *
 * JAK TO DZIAŁA:
 *   Pięć rodzajów „fabryk” (od najprostszej):
 *   1. Static factory method (statyczna metoda wytwórcza): {@code Money.parse("12,50")} — nazwa zamiast new.
 *      To NIE jest wzorzec z książki GoF, ale najczęściej używane narzędzie (Effective Java, pozycja 1).
 *   2. Simple Factory (prosta fabryka): jedna metoda ze switch, w jednym miejscu, zwraca interfejs.
 *      Też nie z GoF — idiom.
 *   3. Factory Method (metoda wytwórcza, GoF): klasa bazowa woła abstrakcyjną metodę create...(),
 *      a PODKLASA decyduje, jaki obiekt powstanie.
 *
 *        ┌───────────────────────┐          ┌───────────────┐
 *        │ ReportExporter        │  tworzy  │ RowFormatter  │  ← Product (produkt)
 *        │ (Creator = twórca)    │─────────▶│               │
 *        │ createFormatter()     │          └───────────────┘
 *        └───────────────────────┘                  △
 *             △              △              CsvFormatter, TextFormatter
 *        CsvReportExporter  TextReportExporter  ← ConcreteCreator (konkretny twórca)
 *
 *   4. Abstract Factory (fabryka abstrakcyjna, GoF): fabryka tworzy CAŁĄ RODZINĘ pasujących do siebie
 *      obiektów (np. polskie nagłówki + polskie formatowanie kwot, albo angielskie).
 *   5. Registry (rejestr fabryk): mapa nazwa → dostawca (Supplier), do którego można dopisać nowe pozycje.
 *
 * SŁÓWKA:
 *   factory = fabryka; create = utwórz; of = z (skład z podanych elementów); from = z (konwersja);
 *   valueOf = wartość z; parse = rozbierz tekst na wartość; supplier = dostawca (obiekt z metodą get());
 *   creator = twórca; product = produkt; family = rodzina; cache = pamięć podręczna; hide = ukryć;
 *   sealed = „zapieczętowany” (zamknięta lista podtypów, Java 17); constructor reference = referencja
 *   do konstruktora (Klasa::new).
 *
 * ZOBACZ TEŻ: t06_oop_basics/Oop02Constructors (zwykłe konstruktory), t08_enums/Enums02FieldsMethods (valueOf),
 *   t07_inheritance_polymorphism/Inherit07SealedClasses (sealed), t13_lambdas/Lambda04MethodReferences (Klasa::new),
 *   t22_design_patterns/Patterns02Builder (inna droga do złożonych obiektów),
 *   t22_design_patterns/Patterns05TemplateMethod (metoda wytwórcza bywa krokiem metody szablonowej),
 *   t22_design_patterns/Patterns08DependencyInjection (kto wstrzykuje fabrykę)
 * </pre>
 */
public class Patterns03Factory {

    public static void main(String[] args) {
        title("Patterns03 — Factory (fabryki)");

        problemNew();             // problem new = problem rozsianego new
        staticFactoryMethods();   // static factory methods = statyczne metody wytwórcze
        simpleFactory();          // simple factory = prosta fabryka
        factoryMethod();          // factory method = metoda wytwórcza (GoF)
        abstractFactory();        // abstract factory = fabryka abstrakcyjna
        registryOfSuppliers();    // registry of suppliers = rejestr dostawców
        hidingSealedHierarchy();  // hiding sealed hierarchy = ukrywanie hierarchii za fabryką
        testableFactory();        // testable factory = fabryka łatwa do testowania
        pitfalls();               // pitfalls = pułapki i kiedy używać
        exercises();              // exercises = ćwiczenia
    }

    // =================================================================================================
    // WSPÓLNE TYPY: powiadomienia (kanały: email, sms, push)
    // =================================================================================================

    /** Powiadomienie: send = wyślij. Zamiast prawdziwej wysyłki zwraca tekst — wynik jest przewidywalny. */
    interface Notification {
        String send(String message);
    }

    record EmailNotification(String address) implements Notification {
        @Override // Override = przesłoń metodę interfejsu
        public String send(String message) {
            return "EMAIL → " + address + ": " + message;
        }
    }

    record SmsNotification(String phone) implements Notification {
        @Override
        public String send(String message) {
            return "SMS → " + phone + ": " + message;
        }
    }

    record PushNotification(String deviceId) implements Notification {
        @Override
        public String send(String message) {
            return "PUSH → " + deviceId + ": " + message;
        }
    }

    // =================================================================================================
    // 1. PROBLEM: new rozsiane po programie
    // =================================================================================================

    /** Potwierdzenie zamówienia — łańcuch if z new wewnątrz. */
    static String legacyOrderConfirmation(String channel, String address) {
        Notification n;
        if (channel.equals("email")) {
            n = new EmailNotification(address);
        } else if (channel.equals("sms")) {
            n = new SmsNotification(address);
        } else {
            throw new IllegalArgumentException("Nieznany kanał: " + channel);
        }
        return n.send("Zamówienie przyjęte");
    }

    /** Przypomnienie o płatności — TEN SAM łańcuch if skopiowany do drugiej metody. */
    static String legacyPaymentReminder(String channel, String address) {
        Notification n;
        if (channel.equals("email")) {
            n = new EmailNotification(address);
        } else if (channel.equals("sms")) {
            n = new SmsNotification(address);
        } else {
            throw new IllegalArgumentException("Nieznany kanał: " + channel);
        }
        return n.send("Zapłać do piątku");
    }

    /**
     * 1. Co się stanie, gdy dojdzie kanał „push”? Trzeba znaleźć KAŻDE miejsce z łańcuchem if
     * i dopisać gałąź. Jedno pominięte = błąd w działającym programie.
     */
    static void problemNew() {
        section("1. Problem: new i if-y rozsiane po programie");

        show("potwierdzenie", legacyOrderConfirmation("email", "jan@example.com"));
        // WYNIK: potwierdzenie → EMAIL → jan@example.com: Zamówienie przyjęte
        show("przypomnienie", legacyPaymentReminder("sms", "600100200"));
        // WYNIK: przypomnienie → SMS → 600100200: Zapłać do piątku
        expectThrows("kanał push (jeszcze nie dopisany)", () -> legacyPaymentReminder("push", "tel-7"));
        // WYNIK: ✔ kanał push (jeszcze nie dopisany) → rzucono IllegalArgumentException: Nieznany kanał: push

        // PUŁAPKA: logika „który kanał → która klasa” jest skopiowana. Dopisanie push w jednym miejscu
        // (w potwierdzeniu) i zapomnienie o drugim (w przypomnieniu) daje błąd dopiero w produkcji.
        // Zasada DRY (Don't Repeat Yourself = nie powtarzaj się): wiedzę o tworzeniu trzymaj w JEDNYM miejscu.
    }

    // =================================================================================================
    // 2. STATYCZNE METODY WYTWÓRCZE (Effective Java, pozycja 1)
    // =================================================================================================

    /** Kwota w groszach. Prywatny konstruktor — instancje powstają tylko przez nazwane metody statyczne. */
    static final class Money {
        private static final Money ZERO = new Money(0);   // jedna współdzielona instancja zera

        private final long grosze;   // grosze = setne części złotego (najmniejsza jednostka)

        private Money(long grosze) {
            this.grosze = grosze;
        }

        /** Nazwa mówi, co podajemy: grosze. Dla zera zwraca tę samą, współdzieloną instancję (cache). */
        static Money ofGrosze(long grosze) {
            if (grosze < 0) {
                throw new IllegalArgumentException("Kwota nie może być ujemna: " + grosze);
            }
            return grosze == 0 ? ZERO : new Money(grosze);
        }

        /** Nazwa mówi, co podajemy: pełne złote. */
        static Money zloty(long zl) {
            return ofGrosze(zl * 100);
        }

        /** parse = rozbierz tekst: "12,50" albo "7". Błędny tekst → czytelny wyjątek. */
        static Money parse(String text) {
            try {
                String[] parts = text.trim().split(",");
                long zl = Long.parseLong(parts[0]);
                long gr = 0;
                if (parts.length > 2 || (parts.length == 2 && parts[1].length() != 2)) {
                    throw new NumberFormatException(text);
                }
                if (parts.length == 2) {
                    gr = Long.parseLong(parts[1]);
                }
                return ofGrosze(zl * 100 + gr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Niepoprawna kwota: " + text, e);
            }
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "%d,%02d zł", grosze / 100, grosze % 100);
        }
    }

    /** Temperatura: trzy sposoby podania tej samej liczby (double) — konstruktor nie rozróżni ich po nazwie. */
    static final class Temperature {
        private final double celsius;

        private Temperature(double celsius) {
            this.celsius = celsius;
        }

        static Temperature ofCelsius(double celsius) {
            if (celsius < -273.15) {
                throw new IllegalArgumentException("Poniżej zera absolutnego: " + celsius);
            }
            return new Temperature(celsius);
        }

        static Temperature ofKelvin(double kelvin) {
            return ofCelsius(kelvin - 273.15);
        }

        double celsius() {
            return celsius;
        }

        double fahrenheit() {
            return celsius * 9 / 5 + 32;
        }
    }

    /**
     * 2. Statyczna metoda wytwórcza zamiast (albo obok) konstruktora. Cztery zalety:
     * (a) ma NAZWĘ, (b) nie musi tworzyć nowego obiektu, (c) może zwrócić podtyp, (d) może walidować i
     * wybierać implementację. Wada: klasa bez publicznego konstruktora nie nadaje się do dziedziczenia.
     */
    static void staticFactoryMethods() {
        section("2. Statyczne metody wytwórcze: nazwa zamiast new");

        // (a) Nazwa: new Temperature(36.6) nie powie, czy to stopnie Celsjusza czy kelwiny.
        show("273.15 K w °C", Temperature.ofKelvin(273.15).celsius());
        // WYNIK: 273.15 K w °C → 0.0
        show("100 °C w °F", Temperature.ofCelsius(100).fahrenheit());
        // WYNIK: 100 °C w °F → 212.0
        expectThrows("-300 °C", () -> Temperature.ofCelsius(-300));
        // WYNIK: ✔ -300 °C → rzucono IllegalArgumentException: Poniżej zera absolutnego: -300.0

        show("Money.zloty(12)", Money.zloty(12));
        // WYNIK: Money.zloty(12) → 12,00 zł
        show("Money.parse(\"12,50\")", Money.parse("12,50"));
        // WYNIK: Money.parse("12,50") → 12,50 zł
        show("Money.parse(\"7\")", Money.parse("7"));
        // WYNIK: Money.parse("7") → 7,00 zł
        expectThrows("Money.parse(\"12,5\")", () -> Money.parse("12,5"));
        // WYNIK: ✔ Money.parse("12,5") → rzucono IllegalArgumentException: Niepoprawna kwota: 12,5

        // (b) Bez nowego obiektu: ta sama instancja zera (obiekt niezmienny, więc współdzielenie jest bezpieczne).
        show("ofGrosze(0) == ofGrosze(0)", Money.ofGrosze(0) == Money.ofGrosze(0));
        // WYNIK: ofGrosze(0) == ofGrosze(0) → true
        show("ofGrosze(5) == ofGrosze(5)", Money.ofGrosze(5) == Money.ofGrosze(5));
        // WYNIK: ofGrosze(5) == ofGrosze(5) → false
        // To samo robi Integer.valueOf: małe liczby (-128..127) pochodzą z pamięci podręcznej, duże są nowe.
        show("Integer.valueOf(100) == Integer.valueOf(100)", Integer.valueOf(100) == Integer.valueOf(100));
        // WYNIK: Integer.valueOf(100) == Integer.valueOf(100) → true
        show("Integer.valueOf(1000) == Integer.valueOf(1000)", Integer.valueOf(1000) == Integer.valueOf(1000));
        // WYNIK: Integer.valueOf(1000) == Integer.valueOf(1000) → false

        // (c) Podtyp: List.of(...) zwraca różne, niepubliczne implementacje zależnie od liczby elementów,
        // a Ty znasz tylko interfejs List — autor biblioteki może je zmienić bez łamania Twojego kodu.
        // Przykład z sekcji 3 poniżej: jedna metoda zwraca EmailNotification, SmsNotification albo PushNotification.

        // Przykład z JDK: konstruktor kontra metoda wytwórcza.
        show("new BigDecimal(0.1)", new BigDecimal(0.1));   // dokładna wartość double, nie 0.1!
        // WYNIK: new BigDecimal(0.1) → 0.1000000000000000055511151231257827021181583404541015625
        show("BigDecimal.valueOf(0.1)", BigDecimal.valueOf(0.1));
        // WYNIK: BigDecimal.valueOf(0.1) → 0.1

        // Konwencje nazw (kojarz nazwę z rolą):
        //   of(a, b, c)      — złóż z elementów:             List.of(1, 2), EnumSet.of(A, B), Path.of("a", "b")
        //   from(x)          — konwertuj z innego typu:      Instant.from(temporal)
        //   valueOf(x)       — jak from/of (starsza nazwa):  Integer.valueOf("5"), enum: Color.valueOf("RED")
        //   parse(text)      — rozbierz tekst:               LocalDate.parse("2026-01-05")
        //   copyOf(x)        — kopia niezmienna:             List.copyOf(lista)
        //   getInstance / newInstance / create — dostęp do instancji lub nowa instancja: Calendar.getInstance()
        // DOBRA PRAKTYKA: pisz prywatny konstruktor + nazwane metody statyczne, gdy istnieje więcej niż jeden sensowny
        // sposób zbudowania obiektu z tych samych typów (Celsjusz/kelwin, grosze/złote).
        // PUŁAPKA: pamięć podręczna (cache) wolno stosować tylko dla obiektów NIEZMIENNYCH. Współdzielenie zmiennego
        // obiektu przez fabrykę to ukryta zmienna globalna — każdy, kto go zmieni, zmieni go wszystkim.
        // PUŁAPKA: porównywanie ofGrosze(5) == ofGrosze(5) zwraca false — porównuj przez equals() (tu: dopisz equals).
    }

    // =================================================================================================
    // 3. SIMPLE FACTORY: jeden switch w jednym miejscu
    // =================================================================================================

    /** Prosta fabryka: wiedza „kanał → klasa” w JEDNYM miejscu. Zwraca INTERFEJS, klient nie zna klas. */
    static final class NotificationFactory {
        private NotificationFactory() {
        }

        static Notification create(String channel, String address) {
            return switch (channel) {   // switch expression = wyrażenie switch (Java 14+)
                case "email" -> new EmailNotification(address);
                case "sms" -> new SmsNotification(address);
                case "push" -> new PushNotification(address);
                default -> throw new IllegalArgumentException("Nieznany kanał: " + channel);
            };
        }
    }

    /** Nowa wersja potwierdzenia — nie wie nic o klasach powiadomień. */
    static String orderConfirmation(String channel, String address) {
        return NotificationFactory.create(channel, address).send("Zamówienie przyjęte");
    }

    /** Nowa wersja przypomnienia. */
    static String paymentReminder(String channel, String address) {
        return NotificationFactory.create(channel, address).send("Zapłać do piątku");
    }

    /**
     * 3. Simple Factory (prosta fabryka): obie metody używają tej samej fabryki, więc kanał „push”
     * dopisujemy w jednym miejscu i działa wszędzie.
     */
    static void simpleFactory() {
        section("3. Simple Factory: jedno miejsce z switch");

        show("potwierdzenie przez push", orderConfirmation("push", "tel-7"));
        // WYNIK: potwierdzenie przez push → PUSH → tel-7: Zamówienie przyjęte
        show("przypomnienie przez push", paymentReminder("push", "tel-7"));
        // WYNIK: przypomnienie przez push → PUSH → tel-7: Zapłać do piątku
        expectThrows("kanał fax", () -> orderConfirmation("fax", "123"));
        // WYNIK: ✔ kanał fax → rzucono IllegalArgumentException: Nieznany kanał: fax

        // Zauważ: nadal jest switch (łamie OCP — każdy nowy kanał to edycja fabryki), ale tylko JEDEN i w
        // miejscu, które za to odpowiada. Dla wielu kanałów albo kanałów z wtyczek przejdź na rejestr (sekcja 6).
        // DOBRA PRAKTYKA: fabryka zwraca interfejs (Notification), a nie konkretną klasę — klient nie musi
        // wiedzieć, co powstało. Dzięki temu możesz zmienić klasę bez dotykania klientów.
    }

    // =================================================================================================
    // 4. FACTORY METHOD (GoF): podklasa decyduje
    // =================================================================================================

    /** Produkt: zamienia produkt sklepowy na linię raportu. */
    @FunctionalInterface
    interface RowFormatter {
        String format(Product product);
    }

    /**
     * Twórca (Creator): zna algorytm eksportu, ale NIE wie, w jakim formacie — tego decyduje podklasa przez
     * metodę wytwórczą createFormatter(). abstract = abstrakcyjna (bez ciała, trzeba nadpisać).
     */
    abstract static class ReportExporter {
        protected abstract RowFormatter createFormatter();   // ← FACTORY METHOD

        final List<String> export(List<Product> products) {   // final = podklasa nie nadpisze algorytmu
            RowFormatter formatter = createFormatter();
            return products.stream().map(formatter::format).toList();   // toList = do listy (Java 16+)
        }
    }

    static final class CsvReportExporter extends ReportExporter {
        @Override
        protected RowFormatter createFormatter() {
            return p -> p.sku() + ";" + p.name() + ";" + p.price();
        }
    }

    static final class TextReportExporter extends ReportExporter {
        @Override
        protected RowFormatter createFormatter() {
            return p -> p.name() + " — " + p.price() + " zł";
        }
    }

    /**
     * 4. Factory Method (metoda wytwórcza): klasa bazowa napisała algorytm raz; podklasy dostarczają tylko
     * „jakiego obiektu użyć”. Ten sam wzorzec jest w JDK: {@code Iterable.iterator()} — każda kolekcja
     * decyduje, jaki iterator utworzy, a pętla for-each działa dla wszystkich tak samo.
     */
    static void factoryMethod() {
        section("4. Factory Method: podklasa wybiera produkt");

        List<Product> first = SampleData.products().subList(0, 2);   // subList = fragment listy (od 0 do 2, bez 2)
        show("CSV", new CsvReportExporter().export(first));
        // WYNIK: CSV → [ELE-001;Laptop Pro 14;5499.99, ELE-002;Smartfon X;2999.00]
        show("tekst", new TextReportExporter().export(first));
        // WYNIK: tekst → [Laptop Pro 14 — 5499.99 zł, Smartfon X — 2999.00 zł]

        // Inne metody wytwórcze z JDK: Collection.stream(),
        // Calendar.getInstance(), NumberFormat.getInstance(). W Springu: metody @Bean w klasach konfiguracji
        // to „metody wytwórcze” beanów, a cały ApplicationContext (kontener) jest fabryką obiektów.
        // Metoda wytwórcza bywa jednym z kroków metody szablonowej (Patterns05TemplateMethod):
        // szkielet algorytmu (export) jest stały, a jeden krok (createFormatter) zmienia podklasa.
        // PUŁAPKA: tworzenie podklasy tylko po to, by zmienić jeden obiekt, to dziś często przesada.
        // Jeśli różni się tylko ta jedna rzecz, przekaż ją jako argument (Supplier lub RowFormatter) —
        // zobacz sekcję 6 i „template z lambdami” w Patterns05TemplateMethod.
    }

    // =================================================================================================
    // 5. ABSTRACT FACTORY: rodzina obiektów
    // =================================================================================================

    @FunctionalInterface
    interface TitleFormatter {
        String format(int number);
    }

    @FunctionalInterface
    interface MoneyFormatter {
        String format(long grosze);
    }

    /** Fabryka abstrakcyjna: tworzy KOMPLET pasujących do siebie formatterów jednej wersji językowej. */
    interface InvoiceKit {
        TitleFormatter titleFormatter();

        MoneyFormatter moneyFormatter();
    }

    /** Rodzina polska: „Faktura nr 17” i „1234,50 zł”. */
    static final class PolishInvoiceKit implements InvoiceKit {
        @Override
        public TitleFormatter titleFormatter() {
            return number -> "Faktura nr " + number;
        }

        @Override
        public MoneyFormatter moneyFormatter() {
            return grosze -> String.format(Locale.ROOT, "%d,%02d zł", grosze / 100, grosze % 100);
        }
    }

    /** Rodzina angielska: „Invoice no. 17” i „PLN 1234.50”. */
    static final class EnglishInvoiceKit implements InvoiceKit {
        @Override
        public TitleFormatter titleFormatter() {
            return number -> "Invoice no. " + number;
        }

        @Override
        public MoneyFormatter moneyFormatter() {
            return grosze -> String.format(Locale.ROOT, "PLN %d.%02d", grosze / 100, grosze % 100);
        }
    }

    /** Klient: dostaje komplet od fabryki i nie wie, czy to wersja polska, czy angielska. */
    static String printHeader(InvoiceKit kit, int number, long grosze) {
        return kit.titleFormatter().format(number) + ", do zapłaty: " + kit.moneyFormatter().format(grosze);
    }

    /**
     * 5. Abstract Factory (fabryka abstrakcyjna): gwarantuje SPÓJNOŚĆ rodziny — nie da się przypadkiem
     * połączyć polskiego tytułu z angielskim formatem kwoty, bo oba pochodzą z jednej fabryki.
     */
    static void abstractFactory() {
        section("5. Abstract Factory: rodzina pasujących obiektów");

        show("wersja polska", printHeader(new PolishInvoiceKit(), 17, 123450));
        // WYNIK: wersja polska → Faktura nr 17, do zapłaty: 1234,50 zł
        show("wersja angielska", printHeader(new EnglishInvoiceKit(), 17, 123450));
        // WYNIK: wersja angielska → Invoice no. 17, do zapłaty: PLN 1234.50

        // W JDK: javax.xml.parsers.DocumentBuilderFactory to fabryka, która sama dobiera implementację
        // parsera XML (raczej fabryka z wymienną implementacją niż pełna „rodzina” produktów).
        // W praktyce wzorzec jest rzadki w czystej postaci — dziś często zastępuje go obiekt konfiguracji
        // z kilkoma polami albo kontener DI. Zapamiętaj ideę: „rodzina produktów z jednego źródła”.
        // PUŁAPKA: dodanie NOWEGO produktu do rodziny (np. formatDate) wymusza zmianę interfejsu InvoiceKit
        // i WSZYSTKICH jego implementacji. Fabryka abstrakcyjna ułatwia dodawanie rodzin, nie produktów.
    }

    // =================================================================================================
    // 6. REJESTR DOSTAWCÓW: Map<String, Supplier<T>>
    // =================================================================================================

    /** Rejestr: nazwa → dostawca nowego eksportera. CsvReportExporter::new = referencja do konstruktora. */
    static Map<String, Supplier<ReportExporter>> exporterRegistry() {
        Map<String, Supplier<ReportExporter>> registry = new TreeMap<>();   // TreeMap = klucze posortowane
        registry.put("csv", CsvReportExporter::new);
        registry.put("txt", TextReportExporter::new);
        return registry;
    }

    static ReportExporter createExporter(Map<String, Supplier<ReportExporter>> registry, String name) {
        Supplier<ReportExporter> supplier = registry.get(name);   // Supplier = dostawca, metoda get()
        if (supplier == null) {
            throw new IllegalArgumentException("Nieznany eksporter: " + name + " (dostępne: " + registry.keySet() + ")");
        }
        return supplier.get();   // KAŻDE wywołanie get() tworzy NOWY obiekt
    }

    /**
     * 6. Rejestr zamienia switch na dane: fabryka się nie zmienia, a nowe pozycje „dopisuje się” z zewnątrz
     * (z innego modułu, z wtyczki, z testu). Zachowuje zasadę otwarte/zamknięte (OCP, CleanCode02Solid).
     */
    static void registryOfSuppliers() {
        section("6. Rejestr dostawców: Map z Supplier");

        Map<String, Supplier<ReportExporter>> registry = exporterRegistry();
        show("dostępne eksportery", registry.keySet());
        // WYNIK: dostępne eksportery → [csv, txt]

        List<Product> first = SampleData.products().subList(0, 1);
        show("csv", createExporter(registry, "csv").export(first));
        // WYNIK: csv → [ELE-001;Laptop Pro 14;5499.99]
        expectThrows("eksporter xml", () -> createExporter(registry, "xml"));
        // WYNIK: ✔ eksporter xml → rzucono IllegalArgumentException: Nieznany eksporter: xml (dostępne: [csv, txt])

        // Dopisujemy nowy eksporter bez zmiany istniejącego kodu (anonimowa podklasa = klasa bez nazwy):
        registry.put("sku", () -> new ReportExporter() {
            @Override
            protected RowFormatter createFormatter() {
                return Product::sku;   // Product::sku = referencja do metody (t13_lambdas/Lambda04MethodReferences)
            }
        });
        show("po dopisaniu sku", registry.keySet());
        // WYNIK: po dopisaniu sku → [csv, sku, txt]
        show("sku", createExporter(registry, "sku").export(SampleData.products().subList(0, 3)));
        // WYNIK: sku → [ELE-001, ELE-002, ELE-003]

        // DOBRA PRAKTYKA: klucze w TreeMap (albo LinkedHashMap), gdy je wypisujesz — wynik jest powtarzalny.
        // DOBRA PRAKTYKA: nieznana nazwa → wyjątek z listą dostępnych (jak wyżej), nie null i nie „cichy domyślny”.
        // Dlaczego Supplier, a nie gotowa instancja? Jeśli eksporter trzyma stan (bufor), każdy użytkownik powinien
        // dostać własny obiekt. Jeśli obiekt jest bezstanowy i niezmienny, w rejestrze może leżeć sama instancja.
        // W Springu: kontener zbiera wszystkie beany danego interfejsu w mapę „nazwa beana → obiekt”
        // — to ten sam rejestr, tylko budowany automatycznie (SpringLearning).
    }

    // =================================================================================================
    // 7. FABRYKA UKRYWAJĄCA ZAMKNIĘTĄ HIERARCHIĘ (sealed)
    // =================================================================================================

    /** Metoda płatności: lista podtypów jest ZAMKNIĘTA (sealed ... permits = „zapieczętowany, dozwolone: ...”). */
    sealed interface PaymentMethod permits CardPayment, BlikPayment, TransferPayment {
        String describe();   // describe = opisz
    }

    private record CardPayment(String number) implements PaymentMethod {
        @Override
        public String describe() {
            return "karta ****" + number.substring(number.length() - 4);   // substring = fragment napisu
        }
    }

    private record BlikPayment(String code) implements PaymentMethod {
        @Override
        public String describe() {
            return "BLIK (kod jednorazowy)";
        }
    }

    private record TransferPayment(String account) implements PaymentMethod {
        @Override
        public String describe() {
            return "przelew na konto " + account;
        }
    }

    /** Publiczne API: metody wytwórcze zwracają INTERFEJS i sprawdzają dane; klasy implementacji są prywatne. */
    static final class PaymentMethods {
        private PaymentMethods() {
        }

        static PaymentMethod card(String number) {
            if (!number.matches("\\d{16}")) {   // matches = czy pasuje do wzorca; \\d{16} = dokładnie 16 cyfr
                throw new IllegalArgumentException("Numer karty musi mieć 16 cyfr");
            }
            return new CardPayment(number);
        }

        static PaymentMethod blik(String code) {
            if (!code.matches("\\d{6}")) {
                throw new IllegalArgumentException("Kod BLIK musi mieć 6 cyfr");
            }
            return new BlikPayment(code);
        }

        static PaymentMethod transfer(String account) {
            return new TransferPayment(account);
        }
    }

    /**
     * 7. Fabryka + sealed: klient widzi tylko interfejs PaymentMethod i metody card/blik/transfer. Autor może
     * zmienić lub dodać klasę implementacji, nie łamiąc klientów. Uwaga: w tym jednym pliku prywatne rekordy
     * i tak są widoczne dla każdej metody lekcji — w prawdziwym projekcie leżałyby w osobnym pakiecie.
     */
    static void hidingSealedHierarchy() {
        section("7. Fabryka ukrywająca zamkniętą hierarchię (sealed)");

        List<PaymentMethod> methods = List.of(
                PaymentMethods.card("4111111111111111"),
                PaymentMethods.blik("123456"),
                PaymentMethods.transfer("PL61 1090 1014 0000 0712 1981 2874"));
        showEach("metody płatności", methods.stream().map(PaymentMethod::describe).toList());
        // WYNIK: metody płatności (liczba elementów: 3):
        // WYNIK:    • karta ****1111
        // WYNIK:    • BLIK (kod jednorazowy)
        // WYNIK:    • przelew na konto PL61 1090 1014 0000 0712 1981 2874
        expectThrows("karta z 3 cyframi", () -> PaymentMethods.card("411"));
        // WYNIK: ✔ karta z 3 cyframi → rzucono IllegalArgumentException: Numer karty musi mieć 16 cyfr

        // Java 17: pełną listę podtypów sprawdzasz przez instanceof (pattern matching, Java 16+):
        PaymentMethod first = methods.get(0);
        if (first instanceof CardPayment card) {
            show("pierwsza to karta kończąca się na", card.number().substring(12));
            // WYNIK: pierwsza to karta kończąca się na → 1111
        }
        // (Java 21+) switch z dopasowaniem wzorca nie wymaga default — kompilator zna wszystkie podtypy sealed:
        //   String text = switch (method) {
        //       case CardPayment c     -> "karta";
        //       case BlikPayment b     -> "blik";
        //       case TransferPayment t -> "przelew";
        //   };   // dodanie czwartego podtypu = błąd kompilacji tutaj, a nie cichy błąd w czasie działania

        // DOBRA PRAKTYKA: walidację danych (16 cyfr) umieść w fabryce albo w konstruktorze — nie rozsiewaj jej po kliencie.
        // Dane karty nigdy nie trafiają do logów w całości: describe() maskuje numer.
    }

    // =================================================================================================
    // 8. TESTOWALNOŚĆ: wstrzyknięta fabryka identyfikatorów
    // =================================================================================================

    /** Fabryka numerów zamówień: źródło liczb (IntSupplier = dostawca int, metoda getAsInt) dostaje z zewnątrz. */
    static final class OrderIdFactory {
        private final IntSupplier sequence;   // sequence = ciąg kolejnych liczb

        OrderIdFactory(IntSupplier sequence) {
            this.sequence = sequence;
        }

        String next() {   // next = następny
            return String.format(Locale.ROOT, "ZAM-%03d", sequence.getAsInt());
        }
    }

    /**
     * 8. Statyczna metoda „zaszyta na stałe” (np. losowy identyfikator albo bieżący czas) utrudnia test.
     * Fabryka, której źródło liczb dostaje się z zewnątrz, daje w teście stały, przewidywalny wynik.
     */
    static void testableFactory() {
        section("8. Testowalność: fabryka z wstrzykniętym źródłem");

        // Produkcja: kolejny numer z licznika (AtomicInteger = licznik bezpieczny dla wielu wątków).
        AtomicInteger counter = new AtomicInteger();
        OrderIdFactory production = new OrderIdFactory(counter::incrementAndGet);   // incrementAndGet = zwiększ i podaj
        show("pierwszy numer", production.next());
        // WYNIK: pierwszy numer → ZAM-001
        show("drugi numer", production.next());
        // WYNIK: drugi numer → ZAM-002

        // Test: stały numer 42 — wynik nie zależy od niczego innego.
        OrderIdFactory underTest = new OrderIdFactory(() -> 42);   // under test = testowana
        show("numer w teście", underTest.next());
        // WYNIK: numer w teście → ZAM-042

        // DOBRA PRAKTYKA: wszystko, co niedeterministyczne (losowość, czas, liczniki), przekazuj do fabryki
        // jako zależność (Supplier, IntSupplier, Clock) — to wstęp do Patterns08DependencyInjection.
        // PUŁAPKA: statyczna fabryka wołająca inną statyczną rzecz (UUID.randomUUID(), LocalDate.now()) jest
        // w praktyce niemożliwa do podmiany w teście — wynik różni się przy każdym uruchomieniu.
    }

    // =================================================================================================
    // 9. PUŁAPKI I KIEDY UŻYWAĆ
    // =================================================================================================

    /**
     * 9. Typowe błędy i tabela decyzji.
     * <pre>
     *   UŻYJ FABRYKI, GDY:                                 NIE UŻYWAJ, GDY:
     *   • konstruktor nie ma sensownej nazwy               • fabryka tylko woła new bez żadnej logiki
     *   • logika wyboru klasy jest w kilku miejscach       • jest jedna klasa i nie widać następnej
     *   • chcesz zwrócić podtyp lub współdzielić obiekt    • zwykły konstruktor jest czytelny
     *   • walidacja i tworzenie mają być razem             • (Spring) wystarczy @Component i wstrzyknięcie
     *   • klient ma nie znać konkretnych klas
     * </pre>
     */
    static void pitfalls() {
        section("9. Pułapki i kiedy używać");

        // PUŁAPKA: fabryka, która tylko woła new (YAGNI). Metoda  createProduct(a, b) { return new Product(a, b); }
        // dokłada warstwę bez żadnej korzyści: nie waliduje, nie ma nazwy lepszej niż konstruktor, nie wybiera
        // typu. Fabryka ma sens dopiero, gdy ukrywa decyzję (która klasa, skąd, z jakim cache) albo daje nazwę.
        // PUŁAPKA: „wielka fabryka” z dziesiątkami if-ów dla każdego typu w aplikacji, wiedząca o wszystkim.
        // Podziel ją na małe, albo użyj rejestru (sekcja 6).
        // PUŁAPKA: fabryka zwracająca null przy błędzie. Rzuć wyjątek albo zwróć Optional (t14_optional).
        // PUŁAPKA: statyczna fabryka trudna do podmiany w teście — przekaż ją jako zależność (sekcja 8).
        // PUŁAPKA: klasa tylko z prywatnymi konstruktorami nie może mieć podklas (wada metod wytwórczych).

        note("Fabryka ma sens, gdy ukrywa decyzję lub daje nazwę — samo new w osobnej metodzie to szum.");
        // WYNIK:    ℹ Fabryka ma sens, gdy ukrywa decyzję lub daje nazwę — samo new w osobnej metodzie to szum.
        note("Switch w jednym miejscu (Simple Factory) jest lepszy niż w pięciu; rejestr jest lepszy niż switch.");
        // WYNIK:    ℹ Switch w jednym miejscu (Simple Factory) jest lepszy niż w pięciu; rejestr jest lepszy niż switch.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Statyczna metoda wytwórcza: nazwa (of/from/valueOf/parse/copyOf), może cache'ować, zwrócić podtyp.
     *   • Simple Factory: jedna metoda ze switch w jednym miejscu, zwraca interfejs (idiom, nie GoF).
     *   • Factory Method (GoF): klasa bazowa woła abstrakcyjne create...(), podklasa wybiera produkt.
     *   • Abstract Factory (GoF): fabryka tworzy spójną RODZINĘ obiektów; łatwo dodać rodzinę, trudno produkt.
     *   • Rejestr: Map<String, Supplier<T>> — nowe pozycje bez zmiany fabryki (OCP); klucze sortuj do wydruku.
     *   • Fabryka + sealed: klient zna interfejs i metody wytwórcze, klasy implementacji pozostają ukryte.
     *   • Cache w fabryce tylko dla obiektów niezmiennych.
     *   • Testy: źródło losowości/czasu/liczników przekaż do fabryki jako Supplier/IntSupplier/Clock.
     *   • W JDK: List.of, Integer.valueOf, Optional.of, LocalDate.parse, Calendar.getInstance, Iterable.iterator().
     *   • W Springu: ApplicationContext = fabryka beanów, @Bean = metoda wytwórcza.
     *   • Pułapka: fabryka, która tylko woła new; fabryka zwracająca null; „boski” switch w wielu miejscach.
     *
     * PYTANIA KONTROLNE:
     *   1. Wymień trzy zalety statycznej metody wytwórczej względem konstruktora.
     *   2. Czym różni się Simple Factory od Factory Method (GoF)?
     *   3. Dlaczego rejestr Map z Supplier lepiej realizuje zasadę otwarte/zamknięte niż switch?
     *   4. Co wypisze:  System.out.println(Money.parse("3,05"));  ?
     *   5. Co wypisze:  System.out.println(Money.ofGrosze(7) == Money.ofGrosze(7));  ?
     *   6. ZNAJDŹ BŁĄD: fabryka trzyma w polu static final jedną instancję ZMIENNEGO obiektu (np. StringBuilder)
     *      i zwraca ją z każdego wywołania create(). Co pójdzie źle?
     *   7. ZNAJDŹ BŁĄD: metoda  static Notification create(String channel)  zwraca null dla nieznanego kanału.
     *      Co się stanie u klienta i jak to poprawić?
     *   8. Kiedy fabryka jest zbędna?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: 212 °F → °C", 100.0, () -> exercise1(212.0).celsius());
        Check.equal("ćw. 1: 32 °F → °C", 0.0, () -> exercise1(32.0).celsius());
        Check.throwsException("ćw. 1: -1000 °F", IllegalArgumentException.class, () -> exercise1(-1000.0));
        Check.equal("ćw. 2: email", "EMAIL → a@x.pl: Hej", () -> exercise2("email", "a@x.pl").send("Hej"));
        Check.equal("ćw. 2: sms", "SMS → 600100200: Hej", () -> exercise2("sms", "600100200").send("Hej"));
        Check.equal("ćw. 2: push", "PUSH → tel-1: Hej", () -> exercise2("push", "tel-1").send("Hej"));
        Check.throwsException("ćw. 2: fax", IllegalArgumentException.class, () -> exercise2("fax", "1"));
        Check.equal("ćw. 3: JSON", List.of("{\"sku\":\"ELE-001\",\"name\":\"Laptop Pro 14\"}",
                "{\"sku\":\"ELE-002\",\"name\":\"Smartfon X\"}"),
                () -> new Exercise3Exporter().export(SampleData.products().subList(0, 2)));
        Check.equal("ćw. 4: kolejne numery", List.of("FV-005", "FV-006", "FV-007"), () -> {
            Supplier<String> ids = exercise4("FV", 5);
            return List.of(ids.get(), ids.get(), ids.get());
        });
        Check.equal("ćw. 4: niezależne liczniki", List.of("A-001", "B-001"), () -> {
            Supplier<String> a = exercise4("A", 1);
            Supplier<String> b = exercise4("B", 1);
            return List.of(a.get(), b.get());
        });
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec): 212 °F", 100.0, () -> solution1(212.0).celsius());
        Check.equal("ćw. 1 (wzorzec): 32 °F", 0.0, () -> solution1(32.0).celsius());
        Check.throwsException("ćw. 1 (wzorzec): -1000 °F", IllegalArgumentException.class, () -> solution1(-1000.0));
        Check.equal("ćw. 2 (wzorzec): email", "EMAIL → a@x.pl: Hej", () -> solution2("email", "a@x.pl").send("Hej"));
        Check.equal("ćw. 2 (wzorzec): sms", "SMS → 600100200: Hej", () -> solution2("sms", "600100200").send("Hej"));
        Check.equal("ćw. 2 (wzorzec): push", "PUSH → tel-1: Hej", () -> solution2("push", "tel-1").send("Hej"));
        Check.throwsException("ćw. 2 (wzorzec): fax", IllegalArgumentException.class, () -> solution2("fax", "1"));
        Check.equal("ćw. 3 (wzorzec): JSON", List.of("{\"sku\":\"ELE-001\",\"name\":\"Laptop Pro 14\"}",
                "{\"sku\":\"ELE-002\",\"name\":\"Smartfon X\"}"),
                () -> new Solution3Exporter().export(SampleData.products().subList(0, 2)));
        Check.equal("ćw. 4 (wzorzec): kolejne numery", List.of("FV-005", "FV-006", "FV-007"), () -> {
            Supplier<String> ids = solution4("FV", 5);
            return List.of(ids.get(), ids.get(), ids.get());
        });
        Check.equal("ćw. 4 (wzorzec): niezależne liczniki", List.of("A-001", "B-001"), () -> {
            Supplier<String> a = solution4("A", 1);
            Supplier<String> b = solution4("B", 1);
            return List.of(a.get(), b.get());
        });
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 10 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): dopisz „fabrykę” dla stopni Fahrenheita: zwróć temperaturę utworzoną przez
     * {@code Temperature.ofCelsius(...)}. Wzór: C = (F - 32) * 5 / 9. Wartość poniżej zera absolutnego
     * ma zgłosić wyjątek (robi to już ofCelsius).
     * Podpowiedź: używaj 5.0 / 9 lub mnóż przed dzieleniem, żeby uniknąć dzielenia całkowitego.
     */
    static Temperature exercise1(double fahrenheit) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ switch na rejestr (mapa nazwa → funkcja tworząca). Stary kod:
     * <pre>{@code
     * static Notification create(String channel, String address) {
     *     switch (channel) {
     *         case "email": return new EmailNotification(address);
     *         case "sms":   return new SmsNotification(address);
     *         case "push":  return new PushNotification(address);
     *         default: throw new IllegalArgumentException("Nieznany kanał: " + channel);
     *     }
     * }
     * }</pre>
     * Podpowiedź: {@code Map<String, Function<String, Notification>>} z wpisami typu
     * {@code "email", EmailNotification::new}; nieznany kanał → IllegalArgumentException.
     */
    static Notification exercise2(String channel, String address) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): Factory Method — dokończ eksporter JSON. createFormatter() ma zwrócić formatter,
     * który dla produktu daje napis w postaci {@code {"sku":"ELE-001","name":"Laptop Pro 14"}} (bez spacji).
     * Podpowiedź: {@code p -> "{\"sku\":\"" + p.sku() + "\",\"name\":\"" + p.name() + "\"}"}.
     */
    static final class Exercise3Exporter extends ReportExporter {
        @Override
        protected RowFormatter createFormatter() {
            // TODO: zwróć formatter JSON
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): fabryka numerów dokumentów. Zwróć Supplier, którego kolejne get() daje
     * {@code prefix + "-" + numer trzycyfrowy}, zaczynając od {@code startFrom} (np. "FV-005", "FV-006").
     * Każdy zwrócony Supplier ma WŁASNY licznik (dwa Supplier-y nie dzielą numeracji).
     * Podpowiedź: licznik w {@code AtomicInteger} utworzony wewnątrz metody; format {@code "%s-%03d"}
     * z {@code Locale.ROOT}; lambda przechwytuje licznik (t13_lambdas/Lambda06ClosuresScope).
     */
    static Supplier<String> exercise4(String prefix, int startFrom) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static Temperature solution1(double fahrenheit) {
        return Temperature.ofCelsius((fahrenheit - 32) * 5 / 9);
    }

    static Notification solution2(String channel, String address) {
        Map<String, Function<String, Notification>> registry = new TreeMap<>();
        registry.put("email", EmailNotification::new);
        registry.put("sms", SmsNotification::new);
        registry.put("push", PushNotification::new);
        Function<String, Notification> creator = registry.get(channel);
        if (creator == null) {
            throw new IllegalArgumentException("Nieznany kanał: " + channel);
        }
        return creator.apply(address);   // apply = zastosuj funkcję
    }

    static final class Solution3Exporter extends ReportExporter {
        @Override
        protected RowFormatter createFormatter() {
            return p -> "{\"sku\":\"" + p.sku() + "\",\"name\":\"" + p.name() + "\"}";
        }
    }

    static Supplier<String> solution4(String prefix, int startFrom) {
        AtomicInteger counter = new AtomicInteger(startFrom);
        return () -> String.format(Locale.ROOT, "%s-%03d", prefix, counter.getAndIncrement());   // getAndIncrement = podaj i zwiększ
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Ma nazwę (of, parse, ofKelvin...), nie musi tworzyć nowego obiektu (cache, singleton), może zwrócić podtyp
     *      lub ukrytą implementację; może też walidować dane przed utworzeniem obiektu.
     *   2. Simple Factory to jedna metoda (zwykle ze switch), która wybiera klasę. Factory Method (GoF) to
     *      abstrakcyjna metoda w klasie bazowej, którą nadpisują PODKLASY — wybór odbywa się przez dziedziczenie.
     *   3. Nowa pozycja to dopisanie wpisu do mapy (z dowolnego miejsca), bez edycji kodu fabryki. Switch
     *      wymaga zmiany istniejącej metody.
     *   4. 3,05 zł
     *   5. false — dla 7 groszy powstają dwa różne obiekty, a == porównuje referencje (zero ma jedną instancję).
     *   6. Wszyscy dostają TEN SAM zmienny obiekt: jeden zmieni zawartość i zepsuje innym wynik (współdzielony
     *      stan globalny). Cache tylko dla obiektów niezmiennych albo za każdym razem nowy obiekt.
     *   7. Klient dostaje NullPointerException daleko od przyczyny. Popraw: rzuć IllegalArgumentException z nazwą
     *      kanału i listą dostępnych albo zwróć Optional.
     *   8. Gdy tylko wywołuje new bez logiki, nie ma drugiej klasy ani potrzeby nazwy — wystarczy konstruktor.
     */
    // </editor-fold>
}
