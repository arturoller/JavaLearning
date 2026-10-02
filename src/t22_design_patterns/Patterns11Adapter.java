package t22_design_patterns;

import helpers.Check;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.function.DoubleSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Wzorzec Adapter — dopasowanie niezgodnych interfejsów
 *        (adapter = przejściówka; adaptee = to, co adaptujemy; target = interfejs, którego oczekuje klient)
 *
 * W SKRÓCIE:
 *   Masz klasę, która robi to, czego potrzebujesz, ale ma INNY interfejs niż ten, którego używa Twój kod
 *   (inne typy, inne jednostki, inne nazwy metod). Zamiast zmieniać cudzą klasę (często nie możesz) albo
 *   rozsiewać konwersje po całym programie, piszesz jedną małą klasę — adapter — która tłumaczy.
 *
 * ANALOGIA: przejściówka do gniazdka. Wtyczka z podróży (adaptee) nie pasuje do polskiego gniazdka (target).
 *   Nie przerabiasz ani wtyczki, ani instalacji w ścianie — wkładasz przejściówkę. Ładowarka dalej jest
 *   ładowarką, a gniazdko nawet nie wie, że coś jest "po drodze".
 *
 * JAK TO DZIAŁA:
 *   Role z książki GoF (Gang of Four, "Banda Czworga"):
 *
 *     Client (klient) ──▶ Target (cel: interfejs, którego klient oczekuje)
 *                              ▲
 *                              │ implementuje
 *                          Adapter (adapter) ──ma pole──▶ Adaptee (adaptowany: cudzy, niezgodny interfejs)
 *
 *   1. Klient zna tylko Target — np. PaymentGateway.pay(orderId, kwota w złotych).
 *   2. Adaptee ma swój świat — np. makePayment(referencja, kwota w groszach jako long) i kody int.
 *   3. Adapter implementuje Target, trzyma Adaptee w polu (kompozycja = składanie obiektów) i w każdej
 *      metodzie: zamienia argumenty, woła Adaptee, zamienia wynik (i błędy) z powrotem.
 *
 *   Dwie odmiany:
 *     adapter OBIEKTOWY (object adapter) — adaptee w polu (kompozycja)            ← domyślny wybór
 *     adapter KLASOWY   (class adapter)  — adapter dziedziczy po adaptee (extends)  ← rzadziej, ma wady
 *
 * SŁÓWKA: adapter = przejściówka; adaptee = adaptowany; target = cel; client = klient; legacy = stary (odziedziczony);
 *   gateway = bramka; port = port (punkt styku z otoczeniem); anti-corruption layer = warstwa ochronna;
 *   grosze = setne części złotego; boundary = granica; wrapper = opakowanie; delegate = delegat (obiekt, któremu przekazujemy pracę)
 *
 * ZOBACZ TEŻ: t22_design_patterns/Patterns07Decorator (też opakowuje, ale NIE zmienia interfejsu),
 *   t22_design_patterns/Patterns10Facade (upraszcza wiele klas naraz),
 *   t22_design_patterns/Patterns08DependencyInjection (adapter wstrzykujemy jak każdą zależność),
 *   t27_clean_code_pitfalls/CleanCode03Architecture (porty i adaptery, architektura heksagonalna),
 *   t18_io_files/Io12Charsets (InputStreamReader i kodowania znaków)
 * </pre>
 */
public class Patterns11Adapter {

    public static void main(String[] args) {
        title("Patterns11 — Adapter: przejściówka między interfejsami");

        problemDuplicatedConversions();   // problem duplicated conversions = problem zduplikowanych konwersji
        objectAdapterPayments();          // object adapter payments = adapter obiektowy (płatności)
        adapterForUnits();                // adapter for units = adapter jednostek (temperatura)
        adapterForXmlStrings();           // adapter for xml strings = adapter tekstu w stylu XML (kursy walut)
        classAdapterVsObjectAdapter();    // class adapter vs object adapter = adapter klasowy a obiektowy
        modernLambdaAdapters();           // modern lambda adapters = nowoczesne adaptery jako lambdy
        twoWayAdapter();                  // two way adapter = adapter dwukierunkowy
        adaptersInTheJdk();               // adapters in the jdk = adaptery w JDK
        portsAndTestability();            // ports and testability = porty i testowalność
        pitfallsAndWhenToUse();           // pitfalls and when to use = pułapki i kiedy stosować
        exercises();                      // exercises = ćwiczenia
    }

    // =================================================================================================
    // Wspólne typy tej lekcji (cała lekcja musi być w jednym pliku — typy są zagnieżdżone)
    // =================================================================================================

    /** Status płatności w NASZYM świecie (enum = typ wyliczeniowy). */
    enum PaymentStatus { APPROVED, DECLINED, ERROR }  // approved = zatwierdzona; declined = odrzucona; error = błąd

    /** Wynik płatności w naszym świecie (record = rekord, niezmienny nośnik danych). */
    record PaymentResult(PaymentStatus status, String message) { }  // message = komunikat

    /** TARGET — interfejs, którego oczekuje nasz sklep: kwota w złotych jako BigDecimal, wynik jako rekord. */
    interface PaymentGateway {
        PaymentResult pay(String orderId, BigDecimal amountPln);  // pay = zapłać; amount = kwota; PLN = złote
    }

    /**
     * ADAPTEE — stary, cudzy interfejs banku. Nie możemy go zmienić (np. to biblioteka dostawcy):
     * kwota w GROSZACH jako long, wynik jako kod liczbowy (0 = OK, 51 = brak środków, 99 = błąd danych).
     */
    static class LegacyBank {  // legacy = odziedziczony, stary
        private final List<String> journal = new ArrayList<>();  // journal = dziennik operacji

        int makePayment(String reference, long grosze) {  // makePayment = wykonaj płatność; reference = referencja
            journal.add(reference + ":" + grosze);
            if (grosze <= 0) {
                return 99;
            }
            if (grosze > 500_000) {  // limit: 5000 zł
                return 51;
            }
            return 0;
        }

        List<String> journal() {
            return List.copyOf(journal);  // copyOf = kopia niezmienna (Java 10+)
        }
    }

    // =================================================================================================
    // 1. PROBLEM: konwersje rozsiane po kodzie klienta
    // =================================================================================================

    /** Pierwszy fragment sklepu (strona WWW) — sam przelicza złotówki na grosze. */
    static int webShopCheckout(LegacyBank bank, String orderId, BigDecimal amountPln) {
        long grosze = amountPln.multiply(BigDecimal.valueOf(100)).longValue();  // longValue UCINA ułamek!
        return bank.makePayment(orderId, grosze);
    }

    /** Drugi fragment sklepu (aplikacja mobilna) — napisany przez kogoś innego, przelicza po swojemu. */
    static int mobileCheckout(LegacyBank bank, String orderId, BigDecimal amountPln) {
        long grosze = Math.round(amountPln.doubleValue() * 100);  // round = zaokrąglij; double do pieniędzy = zły pomysł
        return bank.makePayment(orderId, grosze);
    }

    /**
     * 1. Bez wzorca każdy klient starego API musi znać jego "dialekt": grosze, kody liczbowe. Konwersja
     * kopiuje się do każdego miejsca użycia — i w każdej kopii może wyglądać inaczej (tu: ucinanie kontra
     * zaokrąglanie). Gdy bank zmieni API albo zmienimy dostawcę, trzeba poprawiać WSZYSTKIE kopie.
     */
    static void problemDuplicatedConversions() {
        section("1. Problem: konwersja złotych na grosze rozsiana po kodzie");

        LegacyBank bank = new LegacyBank();
        BigDecimal amount = new BigDecimal("19.999");  // trzy miejsca po przecinku — to już nie są pełne grosze

        webShopCheckout(bank, "ZAM-1", amount);
        mobileCheckout(bank, "ZAM-1", amount);
        show("dziennik banku", bank.journal());
        // WYNIK: dziennik banku → [ZAM-1:1999, ZAM-1:2000]

        // PUŁAPKA: dwie kopie "tej samej" konwersji dały dwie RÓŻNE kwoty za to samo zamówienie
        // (1999 gr kontra 2000 gr). Skoro kod konwertujący jest rozsiany, nikt nie odpowiada za jego poprawność.
        // Dodatkowo klient musi pamiętać, że wynik 51 znaczy "brak środków" — magiczne liczby wszędzie.
        // DOBRA PRAKTYKA: konwersję między światami trzymaj w JEDNYM miejscu — w adapterze.
        note("różne wyniki: 1999 gr (ucięte) i 2000 gr (zaokrąglone) — jedno zamówienie, dwie kwoty");
        // WYNIK: ℹ różne wyniki: 1999 gr (ucięte) i 2000 gr (zaokrąglone) — jedno zamówienie, dwie kwoty
    }

    // =================================================================================================
    // 2. ADAPTER OBIEKTOWY: stary bank za naszym interfejsem
    // =================================================================================================

    /**
     * ADAPTER obiektowy: implementuje Target (PaymentGateway), trzyma Adaptee (LegacyBank) w polu.
     * Cała wiedza o groszach i kodach jest TYLKO tutaj.
     */
    static class LegacyBankPaymentAdapter implements PaymentGateway {
        private final LegacyBank bank;  // adaptee w polu = kompozycja

        LegacyBankPaymentAdapter(LegacyBank bank) {
            this.bank = bank;
        }

        @Override
        public PaymentResult pay(String orderId, BigDecimal amountPln) {
            long grosze = toGrosze(amountPln);                 // 1) tłumaczymy argumenty
            int code = bank.makePayment(orderId, grosze);      // 2) wołamy adaptee
            return toResult(code, grosze);                     // 3) tłumaczymy wynik
        }

        /** Ścisła konwersja: ułamek groszy = wyjątek, zamiast cichej utraty informacji. */
        static long toGrosze(BigDecimal amountPln) {
            return amountPln.movePointRight(2).longValueExact();  // movePointRight = przesuń przecinek w prawo;
            // longValueExact = jako long, ale ArithmeticException, gdy trzeba by uciąć ułamek
        }

        /** Zamiana kodu liczbowego na nasz enum — "magiczne liczby" nie wychodzą poza adapter. */
        static PaymentResult toResult(int code, long grosze) {
            return switch (code) {  // switch jako wyrażenie (Java 14+)
                case 0 -> new PaymentResult(PaymentStatus.APPROVED, "zaksięgowano " + grosze + " gr");
                case 51 -> new PaymentResult(PaymentStatus.DECLINED, "brak środków");
                default -> new PaymentResult(PaymentStatus.ERROR, "kod banku " + code);
            };
        }
    }

    /** Klient zna tylko Target — nie wie nic o groszach ani kodach. */
    static String checkout(PaymentGateway gateway, String orderId, String amount) {
        PaymentResult result = gateway.pay(orderId, new BigDecimal(amount));
        return result.status() + " (" + result.message() + ")";
    }

    /**
     * 2. Adapter obiektowy krok po kroku. Klient dostaje {@code PaymentGateway}; za interfejsem stoi stary bank.
     * Zmiana dostawcy = nowy adapter, klient bez zmian (zasada otwarte-zamknięte, OCP — CleanCode02Solid).
     */
    static void objectAdapterPayments() {
        section("2. Adapter obiektowy: stary bank za naszym interfejsem");

        LegacyBank bank = new LegacyBank();
        PaymentGateway gateway = new LegacyBankPaymentAdapter(bank);  // klient widzi tylko typ PaymentGateway

        show("płatność 120.50", checkout(gateway, "ZAM-10", "120.50"));
        // WYNIK: płatność 120.50 → APPROVED (zaksięgowano 12050 gr)
        show("płatność 9999.00", checkout(gateway, "ZAM-11", "9999.00"));
        // WYNIK: płatność 9999.00 → DECLINED (brak środków)
        show("płatność -5.00", checkout(gateway, "ZAM-12", "-5.00"));
        // WYNIK: płatność -5.00 → ERROR (kod banku 99)
        show("dziennik banku", bank.journal());
        // WYNIK: dziennik banku → [ZAM-10:12050, ZAM-11:999900, ZAM-12:-500]

        // Adapter jest ścisły: kwota 19.999 to nie są pełne grosze — odmawia zamiast po cichu zgadywać.
        expectThrows("kwota 19.999", () -> gateway.pay("ZAM-13", new BigDecimal("19.999")));
        // WYNIK: ✔ kwota 19.999 → rzucono ArithmeticException: Rounding necessary
        // DOBRA PRAKTYKA: konwersja stratna (ucięcie, zaokrąglenie) powinna być decyzją biznesową, widoczną w
        // jednym miejscu — nie przypadkiem ukrytym w kopii kodu. Do pieniędzy używaj BigDecimal, nie double
        // (t15_numbers/Numbers01BigDecimal).
    }

    // =================================================================================================
    // 3. ADAPTER JEDNOSTEK: Fahrenheit → Celsjusz
    // =================================================================================================

    /** TARGET: nasz system alarmowy rozumie tylko stopnie Celsjusza. */
    interface TemperatureSource {
        double celsius();  // celsius = stopnie Celsjusza
    }

    /** ADAPTEE: czujnik z zagranicznego katalogu — zwraca stopnie Fahrenheita. */
    static class FahrenheitSensor {
        private final double reading;  // reading = odczyt

        FahrenheitSensor(double reading) {
            this.reading = reading;
        }

        double readFahrenheit() {  // read = odczytaj
            return reading;
        }
    }

    /** ADAPTER: przelicza °F na °C ze wzoru C = (F − 32) · 5/9. */
    static class FahrenheitSensorAdapter implements TemperatureSource {
        private final FahrenheitSensor sensor;

        FahrenheitSensorAdapter(FahrenheitSensor sensor) {
            this.sensor = sensor;
        }

        @Override
        public double celsius() {
            return (sensor.readFahrenheit() - 32) * 5 / 9;
        }
    }

    /** Klient: zna tylko Celsjusza. */
    static String describe(TemperatureSource source) {  // describe = opisz
        double c = source.celsius();
        if (c <= 0) {
            return "mróz";
        }
        if (c >= 100) {
            return "wrzenie wody";
        }
        return "normalnie";
    }

    /**
     * 3. Adapter bardzo często tłumaczy JEDNOSTKI. To klasyczne źródło katastrof (sonda Mars Climate Orbiter
     * zginęła przy Marsie w 1999 roku, bo jeden zespół liczył w jednostkach imperialnych, a drugi w metrycznych) — dlatego
     * granica między jednostkami powinna być jedna i nazwana.
     */
    static void adapterForUnits() {
        section("3. Adapter jednostek: Fahrenheit → Celsjusz");

        for (double f : new double[] {212.0, 32.0, -40.0}) {
            TemperatureSource source = new FahrenheitSensorAdapter(new FahrenheitSensor(f));
            show(f + " °F", source.celsius() + " °C → " + describe(source));
            // WYNIK: 212.0 °F → 100.0 °C → wrzenie wody
            // WYNIK: 32.0 °F → 0.0 °C → mróz
            // WYNIK: -40.0 °F → -40.0 °C → mróz
        }
        // Ciekawostka: -40 °F to dokładnie -40 °C — jedyny punkt, w którym obie skale się spotykają.

        // PUŁAPKA: adapter, który zmienia JEDNOSTKĘ, ale zostawia ten sam TYP (double → double), jest podstępny:
        // kompilator nie zauważy pomyłki. Gdy się da, nazwij jednostkę w nazwie metody (celsius(), readFahrenheit())
        // albo w typie (np. własny rekord Celsius).
        note("nazwa metody z jednostką (celsius, readFahrenheit) chroni przed pomyłką bardziej niż komentarz");
        // WYNIK: ℹ nazwa metody z jednostką (celsius, readFahrenheit) chroni przed pomyłką bardziej niż komentarz
    }

    // =================================================================================================
    // 4. ADAPTER TEKSTU W STYLU XML: kursy walut
    // =================================================================================================

    /** TARGET: nasz kod chce kursu jako BigDecimal. */
    interface ExchangeRates {
        BigDecimal rate(String from, String to);  // rate = kurs; from = z waluty; to = na walutę
    }

    /** ADAPTEE: zewnętrzny serwis zwraca KAWAŁEK TEKSTU w stylu XML. */
    interface XmlRateService {
        String fetch(String from, String to);  // fetch = pobierz
    }

    /** ADAPTER: wyciąga liczbę z tekstu i zamienia na BigDecimal; błąd serwisu zamienia na wyjątek. */
    static class XmlRateAdapter implements ExchangeRates {
        private static final Pattern VALUE = Pattern.compile("value=\"(\\d+\\.\\d+)\"");  // pattern = wzorzec (regex)
        private final XmlRateService service;

        XmlRateAdapter(XmlRateService service) {
            this.service = service;
        }

        @Override
        public BigDecimal rate(String from, String to) {
            String xml = service.fetch(from, to);
            Matcher matcher = VALUE.matcher(xml);  // matcher = dopasowywacz
            if (!matcher.find()) {                 // find = znajdź
                throw new IllegalArgumentException("brak kursu " + from + "/" + to + " w odpowiedzi: " + xml);
            }
            return new BigDecimal(matcher.group(1));  // group(1) = tekst z pierwszego nawiasu wzorca
        }
    }

    /** Klient: przelicza kwotę, nie wiedząc nic o XML. */
    static BigDecimal convert(ExchangeRates rates, String from, String to, String amount) {
        return rates.rate(from, to).multiply(new BigDecimal(amount)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 4. Adapter zamienia też FORMAT: z tekstu na typ. Fałszywy serwis (lambda) jest deterministyczny — nie
     * łączymy się z internetem. To ta sama korzyść, co w testach: adaptee można podmienić.
     */
    static void adapterForXmlStrings() {
        section("4. Adapter formatu: tekst w stylu XML → BigDecimal");

        XmlRateService fakeService = (from, to) -> switch (from + "/" + to) {
            case "EUR/PLN" -> "<rate from=\"EUR\" to=\"PLN\" value=\"4.3250\"/>";
            case "USD/PLN" -> "<rate from=\"USD\" to=\"PLN\" value=\"4.0150\"/>";
            default -> "<error code=\"404\"/>";
        };
        ExchangeRates rates = new XmlRateAdapter(fakeService);

        show("kurs EUR/PLN", rates.rate("EUR", "PLN"));
        // WYNIK: kurs EUR/PLN → 4.3250
        show("100 EUR w złotych", convert(rates, "EUR", "PLN", "100"));
        // WYNIK: 100 EUR w złotych → 432.50
        show("10.50 USD w złotych", convert(rates, "USD", "PLN", "10.50"));
        // WYNIK: 10.50 USD w złotych → 42.16
        expectThrows("kurs CHF/PLN", () -> rates.rate("CHF", "PLN"));
        // WYNIK: ✔ kurs CHF/PLN → rzucono IllegalArgumentException: brak kursu CHF/PLN w odpowiedzi: <error code="404"/>

        // DOBRA PRAKTYKA: adapter zamienia też BŁĘDY świata zewnętrznego (kod 404, null, tekst bez wartości) na
        // błędy NASZEGO świata (wyjątek z jasnym komunikatem). Wtedy reszta programu nie musi znać dialektu dostawcy.
        // PUŁAPKA: parsowanie XML wyrażeniem regularnym działa w prostej lekcji, ale w prawdziwym kodzie użyj parsera
        // XML (t18_io_files/Io14Xml) — regex nie zrozumie atrybutów w innej kolejności ani cudzysłowów ' '.
    }

    // =================================================================================================
    // 5. ADAPTER KLASOWY a OBIEKTOWY
    // =================================================================================================

    /**
     * ADAPTER KLASOWY: dziedziczy po adaptee (extends) i jednocześnie implementuje Target.
     * W Javie klasa ma tylko JEDNĄ klasę bazową, więc Target musi być interfejsem.
     */
    static class LegacyBankClassAdapter extends LegacyBank implements PaymentGateway {
        @Override
        public PaymentResult pay(String orderId, BigDecimal amountPln) {
            long grosze = LegacyBankPaymentAdapter.toGrosze(amountPln);
            return LegacyBankPaymentAdapter.toResult(makePayment(orderId, grosze), grosze);  // wołamy odziedziczoną metodę
        }
    }

    /**
     * 5. Porównanie. Adapter klasowy jest krótszy, ale: (a) WYSTAWIA stary interfejs na zewnątrz (każdy może
     * wywołać makePayment), (b) działa tylko dla jednej konkretnej klasy adaptee — nie opakujesz instancji
     * stworzonej gdzie indziej ani jej podklasy, (c) wiąże Cię z implementacją klasy bazowej (kruchość
     * dziedziczenia). Adapter obiektowy bierze dowolny obiekt — także współdzielony albo atrapę z testu.
     */
    static void classAdapterVsObjectAdapter() {
        section("5. Adapter klasowy a obiektowy");

        LegacyBankClassAdapter classAdapter = new LegacyBankClassAdapter();
        PaymentGateway viaTarget = classAdapter;
        show("klasowy, przez Target", checkout(viaTarget, "K-1", "10.00"));
        // WYNIK: klasowy, przez Target → APPROVED (zaksięgowano 1000 gr)

        // Wada (a): ten sam obiekt pozwala ominąć adapter i wołać stare API z groszami i kodami.
        int rawCode = classAdapter.makePayment("K-2", -1);  // stare API "przecieka" na zewnątrz
        show("surowy kod starego API", rawCode);
        // WYNIK: surowy kod starego API → 99
        show("dziennik klasowego", classAdapter.journal());
        // WYNIK: dziennik klasowego → [K-1:1000, K-2:-1]

        // Adapter obiektowy: opakowuje DOWOLNĄ instancję — tu jedną, współdzieloną przez dwa adaptery.
        LegacyBank sharedBank = new LegacyBank();
        PaymentGateway first = new LegacyBankPaymentAdapter(sharedBank);
        PaymentGateway second = new LegacyBankPaymentAdapter(sharedBank);
        first.pay("O-1", new BigDecimal("1.00"));
        second.pay("O-2", new BigDecimal("2.00"));
        show("dziennik współdzielony", sharedBank.journal());
        // WYNIK: dziennik współdzielony → [O-1:100, O-2:200]

        // ...albo podklasę zrobioną na potrzeby testu (atrapa, ang. fake = podróbka).
        LegacyBank alwaysDeclining = new LegacyBank() {  // klasa anonimowa = bez nazwy, tworzona w miejscu użycia
            @Override
            int makePayment(String reference, long grosze) {
                return 51;  // zawsze "brak środków"
            }
        };
        show("atrapa banku", checkout(new LegacyBankPaymentAdapter(alwaysDeclining), "T-1", "1.00"));
        // WYNIK: atrapa banku → DECLINED (brak środków)

        // DOBRA PRAKTYKA: domyślnie wybieraj adapter OBIEKTOWY (kompozycja zamiast dziedziczenia — t06_oop_basics).
        // Klasowy ma sens tylko wtedy, gdy musisz nadpisać zachowanie chronione adaptee i nic więcej.
    }

    // =================================================================================================
    // 6. NOWOCZESNY ADAPTER: lambda
    // =================================================================================================

    /** Adapter jako funkcja: przyjmuje źródło w °F (DoubleSupplier = "dostawca liczby double"), zwraca nasz Target. */
    static TemperatureSource fromFahrenheit(DoubleSupplier fahrenheit) {
        return () -> (fahrenheit.getAsDouble() - 32) * 5 / 9;  // TemperatureSource ma jedną metodę → lambda
    }

    /**
     * 6. Gdy Target ma jedną metodę, adapter nie musi być osobną klasą — wystarczy lambda lub referencja do
     * metody (t13_lambdas/Lambda04MethodReferences). To krótkie i wygodne przy prostych tłumaczeniach.
     */
    static void modernLambdaAdapters() {
        section("6. Nowoczesny adapter: lambda zamiast klasy");

        FahrenheitSensor sensor = new FahrenheitSensor(212.0);
        TemperatureSource viaReference = fromFahrenheit(sensor::readFahrenheit);  // sensor::readFahrenheit = referencja do metody
        show("lambda + referencja", viaReference.celsius());
        // WYNIK: lambda + referencja → 100.0

        // Target jako lambda, a adaptee to cokolwiek: tu "płatność" zbudowana wprost z lambdy.
        PaymentGateway offline = (orderId, amount) ->
                new PaymentResult(PaymentStatus.DECLINED, "tryb offline: " + orderId + " " + amount);
        show("bramka jako lambda", checkout(offline, "L-1", "5.00"));
        // WYNIK: bramka jako lambda → DECLINED (tryb offline: L-1 5.00)

        // Kiedy KLASA jest lepsza od lambdy: gdy adapter ma stan (cache, licznik), kilka metod, nazwę przydatną w
        // logach i w testach, albo konwersje są na tyle duże, że lambda byłaby nieczytelna.
        // PUŁAPKA: lambda-adapter z logiką biznesową "bo jest krótko" szybko rośnie — wtedy zasłużyła na klasę.
    }

    // =================================================================================================
    // 7. ADAPTER DWUKIERUNKOWY (krótko)
    // =================================================================================================

    /** Interfejs STAREGO klienta: odczyt w Fahrenheitach. */
    interface OldFahrenheitReader {
        double fahrenheit();
    }

    /**
     * Adapter DWUKIERUNKOWY (two-way): jedna klasa implementuje OBA interfejsy, więc stary i nowy kod
     * mogą używać tego samego obiektu. Stan trzyma w jednej jednostce (Celsjusz), drugą wylicza.
     */
    static class TwoWayThermometer implements TemperatureSource, OldFahrenheitReader {
        private final double celsius;

        TwoWayThermometer(double celsius) {
            this.celsius = celsius;
        }

        @Override
        public double celsius() {
            return celsius;
        }

        @Override
        public double fahrenheit() {
            return celsius * 9 / 5 + 32;
        }
    }

    /**
     * 7. Przydaje się przy stopniowej migracji: część programu już pisze "po nowemu", część jeszcze "po
     * staremu", a źródło prawdy jest jedno. Koszt: dwa interfejsy w jednej klasie — używaj tylko na czas migracji.
     */
    static void twoWayAdapter() {
        section("7. Adapter dwukierunkowy (na czas migracji)");

        TwoWayThermometer thermometer = new TwoWayThermometer(100.0);
        TemperatureSource newClient = thermometer;
        OldFahrenheitReader oldClient = thermometer;
        show("nowy klient (°C)", newClient.celsius());
        // WYNIK: nowy klient (°C) → 100.0
        show("stary klient (°F)", oldClient.fahrenheit());
        // WYNIK: stary klient (°F) → 212.0

        // PUŁAPKA: adapter dwukierunkowy łatwo zamienia się w "worek na wszystko" — gdy migracja się skończy,
        // usuń stary interfejs i klasa znów ma jedną odpowiedzialność (SRP — CleanCode02Solid).
    }

    // =================================================================================================
    // 8. ADAPTERY W JDK
    // =================================================================================================

    /** Czyta cały strumień bajtów jako tekst w podanym kodowaniu — przez adapter InputStreamReader. */
    static String readAll(InputStream in, Charset charset) {
        StringBuilder text = new StringBuilder();
        try (Reader reader = new InputStreamReader(in, charset)) {  // adapter: bajty (InputStream) → znaki (Reader)
            int c;
            while ((c = reader.read()) != -1) {
                text.append((char) c);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);  // unchecked = niekontrolowany: opakowujemy wyjątek kontrolowany
        }
        return text.toString();
    }

    /**
     * 8. Adaptery spotkałeś w JDK, choć nikt ich tak nie nazwał:
     * {@code Arrays.asList} (tablica → List), {@code InputStreamReader} (bajty → znaki),
     * {@code Collections.enumeration/list} (stary Enumeration ↔ Iterator/List),
     * {@code Executors.callable} (Runnable → Callable).
     */
    static void adaptersInTheJdk() {
        section("8. Adaptery w JDK");

        // a) Arrays.asList: tablica widziana jako List. To WIDOK (view), nie kopia — zapis idzie do tablicy.
        String[] array = {"a", "b", "c"};
        List<String> view = Arrays.asList(array);  // asList = jako lista
        view.set(0, "X");
        show("tablica po view.set(0,\"X\")", Arrays.toString(array));
        // WYNIK: tablica po view.set(0,"X") → [X, b, c]
        expectThrows("view.add(\"d\")", () -> view.add("d"));  // rozmiar tablicy jest stały
        // WYNIK: ✔ view.add("d") → rzucono UnsupportedOperationException: (brak komunikatu)
        // PUŁAPKA: adapter oddaje tylko to, co potrafi adaptee — lista zbudowana na tablicy ma stały rozmiar.

        // b) InputStreamReader: bajty → znaki. Ten sam ciąg bajtów, dwa kodowania, dwie długości tekstu.
        byte[] bytes = "zażółć".getBytes(StandardCharsets.UTF_8);  // 6 liter, ale 4 z nich zajmują po 2 bajty
        show("liczba bajtów", bytes.length);
        // WYNIK: liczba bajtów → 10
        show("znaki jako UTF-8", readAll(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8).length());
        // WYNIK: znaki jako UTF-8 → 6
        show("znaki jako ISO-8859-1", readAll(new ByteArrayInputStream(bytes), StandardCharsets.ISO_8859_1).length());
        // WYNIK: znaki jako ISO-8859-1 → 10
        // Adapter nie jest magiczny: źle dobrane kodowanie daje "krzaczki" (t18_io_files/Io12Charsets).

        // c) Collections.enumeration / Collections.list: most między starym Enumeration a nowymi kolekcjami.
        Enumeration<String> old = Collections.enumeration(List.of("kawa", "herbata"));  // enumeration = wyliczenie (stary iterator)
        List<String> asList = Collections.list(old);  // list = zbierz do ArrayList
        show("Collections.list", asList);
        // WYNIK: Collections.list → [kawa, herbata]
        Iterator<String> iterator = Collections.enumeration(List.of("x", "y")).asIterator();  // asIterator = jako iterator (Java 9+)
        show("asIterator → następny", iterator.next());
        // WYNIK: asIterator → następny → x

        // d) Executors.callable: Runnable (nic nie zwraca) → Callable (zwraca wynik).
        List<String> log = new ArrayList<>();
        Callable<String> callable = Executors.callable(() -> log.add("zadanie wykonane"), "gotowe");  // callable = "wywoływalne"
        try {
            show("wynik callable.call()", callable.call());
            // WYNIK: wynik callable.call() → gotowe
        } catch (Exception e) {  // Callable.call() deklaruje throws Exception
            throw new IllegalStateException(e);
        }
        show("efekt uboczny w log", log);
        // WYNIK: efekt uboczny w log → [zadanie wykonane]
        // W Springu: DispatcherServlet nie zna wszystkich rodzajów kontrolerów — używa interfejsu HandlerAdapter,
        // a każdy adapter "umie wywołać" inny rodzaj obsługi żądania (SpringLearning).
    }

    // =================================================================================================
    // 9. PORTY I TESTOWALNOŚĆ
    // =================================================================================================

    /** Serwis sklepu zależy od PORTU (interfejsu), nie od starego banku. */
    static class OrderService {
        private final PaymentGateway gateway;
        private final List<String> confirmations = new ArrayList<>();  // confirmations = potwierdzenia

        OrderService(PaymentGateway gateway) {
            this.gateway = gateway;
        }

        boolean placeOrder(String orderId, String amount) {  // placeOrder = złóż zamówienie
            PaymentResult result = gateway.pay(orderId, new BigDecimal(amount));
            if (result.status() == PaymentStatus.APPROVED) {
                confirmations.add("potwierdzenie " + orderId);
                return true;
            }
            return false;
        }

        List<String> confirmations() {
            return List.copyOf(confirmations);
        }
    }

    /** Atrapa na potrzeby testu: zapamiętuje wywołania i zatwierdza wszystko. Zero banku, zero sieci. */
    static class RecordingGateway implements PaymentGateway {
        final List<String> calls = new ArrayList<>();  // calls = wywołania

        @Override
        public PaymentResult pay(String orderId, BigDecimal amountPln) {
            calls.add(orderId + "=" + amountPln);
            return new PaymentResult(PaymentStatus.APPROVED, "ok");
        }
    }

    /**
     * 9. Architektura heksagonalna (porty i adaptery): domena definiuje PORT — interfejs, którego potrzebuje —
     * a świat zewnętrzny (bank, baza, e-mail) podłącza się przez ADAPTERY. Adapter pełni rolę warstwy
     * ochronnej (anti-corruption layer): cudze modele danych i błędy nie wchodzą do domeny.
     * Więcej: t27_clean_code_pitfalls/CleanCode03Architecture.
     */
    static void portsAndTestability() {
        section("9. Porty i adaptery, czyli testowalność za darmo");

        // Produkcja: adapter starego banku.
        OrderService production = new OrderService(new LegacyBankPaymentAdapter(new LegacyBank()));
        show("zamówienie 50.00 (stary bank)", production.placeOrder("P-1", "50.00"));
        // WYNIK: zamówienie 50.00 (stary bank) → true
        show("zamówienie 6000.00 (stary bank)", production.placeOrder("P-2", "6000.00"));
        // WYNIK: zamówienie 6000.00 (stary bank) → false

        // Test: ta sama klasa OrderService, atrapa zamiast banku — szybko i deterministycznie.
        RecordingGateway fake = new RecordingGateway();
        OrderService underTest = new OrderService(fake);  // underTest = testowany obiekt
        boolean accepted = underTest.placeOrder("T-1", "10.00");
        show("test: przyjęte", accepted);
        // WYNIK: test: przyjęte → true
        show("test: wywołania bramki", fake.calls);
        // WYNIK: test: wywołania bramki → [T-1=10.00]
        show("test: potwierdzenia", underTest.confirmations());
        // WYNIK: test: potwierdzenia → [potwierdzenie T-1]

        // DOBRA PRAKTYKA: wstrzykuj port przez konstruktor (Patterns08DependencyInjection). Adapter produkcyjny i
        // atrapa testowa są wymienne, bo oba implementują ten sam Target.
    }

    // =================================================================================================
    // 10. PUŁAPKI I KIEDY STOSOWAĆ
    // =================================================================================================

    /**
     * 10. Najczęstsze błędy i porównanie z sąsiadami.
     * <pre>
     *   Wzorzec   | zmienia interfejs? | po co
     *   Adapter   | TAK (A → B)        | dopasować niezgodne API
     *   Decorator | NIE (A → A)        | dodać zachowanie (Patterns07Decorator)
     *   Facade    | TAK (wiele → jeden)| uprościć podsystem (Patterns10Facade)
     *   Proxy     | NIE (A → A)        | kontrolować dostęp do obiektu
     * </pre>
     */
    static void pitfallsAndWhenToUse() {
        section("10. Pułapki i kiedy stosować");

        // PUŁAPKA: adapter z logiką biznesową. Adapter TŁUMACZY (typy, jednostki, błędy) — nie liczy rabatów
        // ani nie decyduje, czy zamówienie jest poprawne. Logika biznesowa w adapterze utknie w "warstwie
        // technicznej" i nie da się jej użyć z innym adapterem.
        // PUŁAPKA: wyciek typów adaptee. Jeśli metoda adaptera zwraca LegacyBank.CośTam, klient znów zna stary świat
        // i cały sens adaptera przepadł.
        // PUŁAPKA: adapter, gdzie wystarczy zmienić własny kod (YAGNI = "nie będziesz tego potrzebował").
        // Jeśli to TY jesteś właścicielem obu stron, popraw interfejs zamiast dopisywać przejściówkę.
        // PUŁAPKA: łańcuch adapterów (A → B → C → D). Każde tłumaczenie to szansa na stratę (zaokrąglenie,
        // kodowanie). Dąż do jednego adaptera na granicę.
        // PUŁAPKA: adapter, który połyka błędy ("w razie czego zwróć 0") — ukrywa awarie dostawcy.
        note("adapter tłumaczy: typy, jednostki, błędy — ale NIE liczy reguł biznesowych");
        // WYNIK: ℹ adapter tłumaczy: typy, jednostki, błędy — ale NIE liczy reguł biznesowych

        // KIEDY UŻYWAĆ / KIEDY NIE:
        //   używaj:  cudza biblioteka/API o innym interfejsie; stary kod, którego nie wolno zmienić; granica systemu
        //            (bank, baza, dostawca); chcesz móc podmienić dostawcę albo podstawić atrapę w testach.
        //   nie używaj: gdy możesz zmienić obie strony (zrób je zgodnymi); gdy różnica to jedna linia w jednym
        //            miejscu (zwykła metoda pomocnicza wystarczy); gdy naprawdę potrzebujesz uproszczenia wielu
        //            klas (to Facade), a nie tłumaczenia jednej.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Adapter = przejściówka: implementuje interfejs Target, w środku woła Adaptee (cudzy, niezgodny interfejs).
     *   • Role: Client (klient), Target (cel), Adapter, Adaptee (adaptowany).
     *   • Domyślnie adapter OBIEKTOWY (kompozycja); KLASOWY (extends) wystawia stare API i wiąże z klasą bazową.
     *   • Adapter tłumaczy: typy (BigDecimal ↔ long), jednostki (°F ↔ °C), formaty (tekst ↔ obiekt), błędy (kody ↔ wyjątki).
     *   • Konwersję trzymaj w JEDNYM miejscu; stratną konwersję rób świadomie (longValueExact zamiast cichego ucięcia).
     *   • Gdy Target ma jedną metodę — adapter może być lambdą; gdy ma stan lub logikę — klasą.
     *   • W JDK: Arrays.asList, InputStreamReader, Collections.enumeration/list, Executors.callable.
     *   • Na granicach systemu: port (nasz interfejs) + adapter (cudza technologia) = architektura heksagonalna.
     *   • Adapter zmienia interfejs, Decorator go zachowuje, Facade upraszcza wiele klas.
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie cztery role występują we wzorcu Adapter? Która z nich jest "cudza"?
     *   2. Dlaczego adapter obiektowy jest zwykle lepszy od klasowego? Podaj dwa powody.
     *   3. Co wypisze:  new LegacyBankPaymentAdapter(new LegacyBank()).pay("A", new BigDecimal("0.00")).status()  ?
     *      (przypomnij sobie, co stary bank robi z kwotą 0 groszy)
     *   4. Co wypisze:  String[] t = {"a", "b"};  List<String> l = Arrays.asList(t);  t[1] = "Z";  System.out.println(l);  ?
     *   5. ZNAJDŹ BŁĄD:  adapter zwraca LegacyBank.makePayment(...) jako int i klient porównuje wynik z 51.
     *      Co jest nie tak z punktu widzenia wzorca?
     *   6. ZNAJDŹ BŁĄD:  amount.doubleValue() * 100 jako kwota w groszach przy płatności 0.29 zł. Co może pójść źle?
     *   7. Czym adapter różni się od dekoratora? A od fasady?
     *   8. Kiedy NIE warto pisać adaptera?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: 212 °F = 100 °C", 100.0, () -> exercise1(212.0));
        Check.equal("ćw. 1: -40 °F = -40 °C", -40.0, () -> exercise1(-40.0));
        Check.equal("ćw. 2: adapter płatności", List.of("APPROVED|W-1:1234", "DECLINED|W-2:600000"), () -> exercise2Run());
        Check.equal("ćw. 3: Enumeration → posortowana lista", List.of("a", "b", "c"),
                () -> exercise3(Collections.enumeration(List.of("c", "a", "b"))));
        Check.equal("ćw. 4: kurs z tekstu", new BigDecimal("4.0150"),
                () -> exercise4((from, to) -> "<rate from=\"" + from + "\" to=\"" + to + "\" value=\"4.0150\"/>").rate("USD", "PLN"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 100.0, () -> solution1(212.0));
        Check.equal("ćw. 1b (wzorzec)", -40.0, () -> solution1(-40.0));
        Check.equal("ćw. 2 (wzorzec)", List.of("APPROVED|W-1:1234", "DECLINED|W-2:600000"), () -> solution2Run());
        Check.equal("ćw. 3 (wzorzec)", List.of("a", "b", "c"),
                () -> solution3(Collections.enumeration(List.of("c", "a", "b"))));
        Check.equal("ćw. 4 (wzorzec)", new BigDecimal("4.0150"),
                () -> solution4((from, to) -> "<rate from=\"" + from + "\" to=\"" + to + "\" value=\"4.0150\"/>").rate("USD", "PLN"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): napisz konwersję Fahrenheit → Celsjusz: C = (F − 32) · 5 / 9.
     * Podpowiedź: uważaj na dzielenie całkowite — tu masz double, więc 5 / 9 zapisz jako "* 5 / 9" na końcu.
     */
    static double exercise1(double fahrenheit) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): dopisz adapter {@code PaymentGateway} dla {@code LegacyBank}: kwotę zamień na grosze
     * (movePointRight(2).longValueExact()), kod 0 → APPROVED, 51 → DECLINED, inny → ERROR.
     * Podpowiedź: możesz użyć gotowych LegacyBankPaymentAdapter.toGrosze/toResult — ale spróbuj najpierw napisać
     * własną klasę zagnieżdżoną (zwróć ją z exercise2).
     */
    static PaymentGateway exercise2(LegacyBank bank) {
        throw new UnsupportedOperationException("TODO");
    }

    /** Pomocnik testu ćwiczenia 2: dwie płatności przez adapter, wynik jako "STATUS|dziennik". */
    static List<String> exercise2Run() {
        return runPayments(exercise2(new LegacyBank()));
    }

    /**
     * ĆWICZENIE 3 (średnie, PRZEPISZ stary sposób na nowy): stary kod iterował po Enumeration pętlą while:
     * <pre>{@code
     * List<String> result = new ArrayList<>();
     * while (e.hasMoreElements()) { result.add(e.nextElement()); }
     * Collections.sort(result);
     * }</pre>
     * Przepisz to: Collections.list(...) i sortowanie przez {@code List.sort(null)} (null = porządek naturalny).
     * Podpowiedź: Collections.list zwraca ArrayList, którą wolno sortować.
     */
    static List<String> exercise3(Enumeration<String> e) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): napisz {@code ExchangeRates} jako adapter na {@code XmlRateService} — tu usługa
     * zwraca tekst typu {@code <rate from="USD" to="PLN" value="4.0150"/>}, a Ty masz zwrócić BigDecimal.
     * Wyłuskaj liczbę z atrybutu value (regex albo indexOf/substring). Gdy brak value — IllegalArgumentException.
     * Podpowiedź: wzorzec {@code value="(\\d+\\.\\d+)"} (w Javie z podwojonymi ukośnikami) i Matcher.find().
     */
    static ExchangeRates exercise4(XmlRateService service) {
        throw new UnsupportedOperationException("TODO");
    }

    // Wspólny kod testowy ćwiczenia 2 — dwie płatności: 12.34 zł (zatwierdzona) i 6000.00 zł (ponad limit banku).
    private static List<String> runPayments(PaymentGateway gateway) {
        PaymentResult ok = gateway.pay("W-1", new BigDecimal("12.34"));
        PaymentResult declined = gateway.pay("W-2", new BigDecimal("6000.00"));
        // W-1 zapisano w dzienniku jako grosze: 1234, W-2: 600000
        return List.of(ok.status() + "|W-1:1234", declined.status() + "|W-2:600000");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static double solution1(double fahrenheit) {
        return (fahrenheit - 32) * 5 / 9;
    }

    static PaymentGateway solution2(LegacyBank bank) {
        return new LegacyBankPaymentAdapter(bank);
    }

    static List<String> solution2Run() {
        return runPayments(solution2(new LegacyBank()));
    }

    static List<String> solution3(Enumeration<String> e) {
        List<String> result = Collections.list(e);
        result.sort(null);  // null = porządek naturalny (String.compareTo)
        return result;
    }

    static ExchangeRates solution4(XmlRateService service) {
        Pattern value = Pattern.compile("value=\"(\\d+\\.\\d+)\"");
        return (from, to) -> {
            Matcher matcher = value.matcher(service.fetch(from, to));
            if (!matcher.find()) {
                throw new IllegalArgumentException("brak kursu " + from + "/" + to);
            }
            return new BigDecimal(matcher.group(1));
        };
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Client (klient), Target (interfejs, którego klient oczekuje), Adapter (implementuje Target i woła Adaptee),
     *      Adaptee (adaptowany — cudza klasa o niezgodnym interfejsie). "Cudzy" jest Adaptee.
     *   2. (a) nie wystawia starego API na zewnątrz — klient widzi tylko Target; (b) opakowuje dowolną instancję
     *      (współdzieloną, podklasę, atrapę z testu) i nie wiąże z implementacją klasy bazowej.
     *   3. ERROR — stary bank zwraca kod 99 dla kwoty 0 groszy (grosze <= 0), a adapter zamienia 99 na ERROR.
     *      (Uruchom i sprawdź: wypisze się nazwa enuma, czyli ERROR.)
     *   4. [a, Z] — Arrays.asList to widok tablicy, zapis do tablicy jest widoczny w liście.
     *   5. Cudzy kod liczbowy (51 = brak środków) przecieka do klienta: klient znów zna dialekt banku. Adapter powinien
     *      zwrócić swój typ (PaymentResult z enumem), a magiczne liczby zostać w adapterze.
     *   6. double nie jest dokładny dla ułamków dziesiętnych: 0.29 * 100 może dać 28.999999999999996, a zaokrąglenie
     *      lub ucięcie da różne grosze. Używaj BigDecimal i movePointRight(2).longValueExact().
     *   7. Dekorator ma TEN SAM interfejs na wejściu i wyjściu i dodaje zachowanie; adapter ZMIENIA interfejs.
     *      Fasada upraszcza wiele klas podsystemu w jeden prosty punkt wejścia, adapter dopasowuje jedną klasę.
     *   8. Gdy możesz zmienić obie strony (wystarczy ujednolicić interfejsy), gdy różnica jest drobna i występuje
     *      w jednym miejscu, albo gdy potrzebujesz uproszczenia (wtedy fasada), a nie tłumaczenia.
     */
    // </editor-fold>
}
