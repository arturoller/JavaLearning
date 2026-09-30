package t27_clean_code_pitfalls;

import helpers.Check;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: SOLID na jednym przykładzie — moduł fakturowania krok po kroku
 *        (invoice = faktura; billing = fakturowanie/rozliczenia)
 *
 * W SKRÓCIE:
 *   Inherit08Solid pokazywał pięć zasad SOLID na PIĘCIU osobnych, niezależnych przykładach. Tutaj jest
 *   INACZEJ: jeden moduł fakturowania (liczenie netto/rabatu/VAT, formatowanie, zapis) rozwija się krok po
 *   kroku — każda sekcja to KOLEJNA zasada zastosowana do TEGO SAMEGO kodu, dokładnie tak, jak w prawdziwym
 *   projekcie, gdzie SOLID stosuje się stopniowo do jednego modułu, a nie raz na zawsze przy pierwszym pisaniu.
 *
 * ANALOGIA: rozbudowa domu, nie budowa od zera.
 *   Nie burzysz domu, żeby dodać instalację elektryczną — wzmacniasz jeden fragment na raz (fundamenty, potem
 *   ściany, potem dach), a dom cały czas stoi i działa. SOLID stosowany do istniejącego kodu wygląda tak samo:
 *   po każdym kroku moduł nadal działa i daje TEN SAM wynik — zmienia się tylko to, jak łatwo go ROZBUDOWAĆ.
 *
 * JAK TO DZIAŁA:
 *   Sekcja 0: moduł BEZ SOLID — jedna klasa/metoda robiąca wszystko, z zahardkodowanym rabatem (if/else).
 *   Sekcje 1-5: pięć kroków (S-O-L-I-D), każdy naprawia JEDEN problem widoczny w sekcji 0 lub we wcześniejszym
 *   kroku. Klasy i interfejsy to zagnieżdżone typy statyczne (`static`) wewnątrz tej lekcji — w realnym
 *   projekcie każdy dostałby własny plik.
 *
 * SŁÓWKA:
 *   SRP = Single Responsibility Principle (jedna odpowiedzialność); OCP = Open/Closed Principle (otwarty na
 *   rozszerzenie, zamknięty na modyfikację); LSP = Liskov Substitution Principle (podtyp musi zachowywać się
 *   zgodnie z KONTRAKTEM nadtypu); ISP = Interface Segregation Principle (wąskie interfejsy zamiast "grubych");
 *   DIP = Dependency Inversion Principle (zależność od ABSTRAKCJI, nie konkretnej implementacji); strategy =
 *   strategia (wymienna implementacja algorytmu); constructor injection = wstrzykiwanie przez konstruktor;
 *   test double = zastępnik testowy (fałszywa implementacja użyta w teście).
 *
 * ZOBACZ TEŻ: t07_inheritance_polymorphism/Inherit08Solid (SOLID na pięciu osobnych przykładach — podstawy),
 *             t27_clean_code_pitfalls/CleanCode01Principles (zasady niższego poziomu, na których SOLID się opiera),
 *             t22_design_patterns/Patterns01Strategy (wzorzec Strategia — dokładnie to robimy w sekcji OCP),
 *             t22_design_patterns/Patterns08DependencyInjection (wzorzec DI — dokładnie to robimy w sekcji DIP),
 *             t25_testing/Testing02TestDoubles (zastępniki testowe — dlaczego DIP ułatwia testowanie),
 *             t34_toward_spring/Spring01IocContainer (Spring robi wstrzykiwanie z sekcji DIP automatycznie).
 * </pre>
 */
public class CleanCode02Solid {

    public static void main(String[] args) {
        title("CleanCode02 — SOLID na module fakturowania, krok po kroku");

        startingPoint();       // starting point = punkt wyjścia (moduł bez SOLID)
        srpSplit();             // SRP = jedna odpowiedzialność na klasę
        ocpStrategies();        // OCP = otwarty na rozszerzenie, zamknięty na modyfikację
        lspContractCheck();     // LSP = podtyp musi trzymać się kontraktu nadtypu
        ispNarrowInterfaces();  // ISP = wąskie interfejsy zamiast "grubego" jednego
        dipConstructorInjection(); // DIP = zależność od abstrakcji, wstrzykiwana przez konstruktor
        exercises();             // exercises = ćwiczenia
    }

    // =================================================================================================
    // 0. PUNKT WYJŚCIA: MODUŁ BEZ SOLID
    // =================================================================================================

    /**
     * InvoiceModuleBad = jedna klasa robiąca WSZYSTKO: liczy netto, "na sztywno" decyduje o rabacie wg typu
     * klienta (if/else), liczy VAT, formatuje tekst faktury I zapisuje ją do statycznej listy. Cztery różne
     * odpowiedzialności w jednej metodzie — to właśnie SOLID będzie rozplątywać krok po kroku w sekcjach 1-5.
     */
    static class InvoiceModuleBad {
        static final List<String> savedInvoices = new ArrayList<>();   // "zapis" jako pole statyczne — sztywne

        String process(String customerType, String product, BigDecimal netPrice, int quantity) {
            BigDecimal net = netPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal discount;
            if (customerType.equals("VIP")) {                               // rabat "na sztywno" — nowy typ
                discount = net.multiply(new BigDecimal("0.10"));              // klienta = trzeba EDYTOWAĆ tę metodę
            } else if (customerType.equals("STANDARD")) {
                discount = BigDecimal.ZERO;
            } else {
                discount = BigDecimal.ZERO;
            }
            discount = discount.setScale(2, RoundingMode.HALF_UP);
            BigDecimal afterDiscount = net.subtract(discount);
            BigDecimal vat = afterDiscount.multiply(new BigDecimal("0.23")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal gross = afterDiscount.add(vat);
            String text = String.format(Locale.ROOT, "Faktura: %s x%d netto=%.2f rabat=%.2f VAT=%.2f brutto=%.2f",
                    product, quantity, net, discount, vat, gross);
            savedInvoices.add(text);        // zapis WYMIESZANY z liczeniem i formatowaniem w tej samej metodzie
            return text;
        }
    }

    /**
     * 0. InvoiceModuleBad łamie SRP (liczy + decyduje o rabacie + formatuje + zapisuje w jednej metodzie),
     * OCP (nowy typ klienta = edycja if/else) i utrudnia testowanie (zapis do pola statycznego, brak
     * możliwości podmiany żadnego fragmentu). To punkt wyjścia — reszta lekcji naprawia to krok po kroku.
     */
    static void startingPoint() {
        section("0. Punkt wyjścia: moduł fakturowania bez SOLID");

        InvoiceModuleBad module = new InvoiceModuleBad();
        show("PRZED: process(\"VIP\", \"Kurs Java\", 100.00, 2)", module.process("VIP", "Kurs Java", new BigDecimal("100.00"), 2));
        // WYNIK: PRZED: process("VIP", "Kurs Java", 100.00, 2) → Faktura: Kurs Java x2 netto=200.00 rabat=20.00 VAT=41.40 brutto=221.40

        // dlaczego: wynik jest POPRAWNY — problem nie w tym, że kod nie działa, tylko że KAŻDA przyszła zmiana
        //   (nowy typ klienta, nowa stawka VAT, zmiana formatu wydruku, zmiana sposobu zapisu) wymaga grzebania
        //   w TEJ SAMEJ metodzie, z ryzykiem zepsucia czegoś innego. SOLID to pięć kroków, które to rozdzielają.
    }

    // =================================================================================================
    // 1. SRP — SINGLE RESPONSIBILITY PRINCIPLE
    // =================================================================================================

    /** InvoiceLine = jedna pozycja faktury: produkt, cena netto za sztukę, ilość. */
    record InvoiceLine(String product, BigDecimal netPrice, int quantity) {
    }

    /** InvoiceAmounts = wynik przeliczeń: netto, rabat, VAT, brutto (wszystko w skali 2). */
    record InvoiceAmounts(BigDecimal net, BigDecimal discount, BigDecimal vat, BigDecimal gross) {
    }

    /** InvoiceCalculatorFixedRate = ma JEDNĄ odpowiedzialność: liczy kwoty przy STAŁEJ stawce rabatu (na razie). */
    static class InvoiceCalculatorFixedRate {
        private static final BigDecimal VAT_RATE = new BigDecimal("0.23");

        InvoiceAmounts calculate(InvoiceLine line, BigDecimal discountRate) {
            BigDecimal net = line.netPrice().multiply(BigDecimal.valueOf(line.quantity())).setScale(2, RoundingMode.HALF_UP);
            BigDecimal discount = net.multiply(discountRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal afterDiscount = net.subtract(discount);
            BigDecimal vat = afterDiscount.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
            BigDecimal gross = afterDiscount.add(vat);
            return new InvoiceAmounts(net, discount, vat, gross);
        }
    }

    /** InvoiceTextFormatter = ma JEDNĄ odpowiedzialność: zamienia kwoty na tekst faktury. */
    static class InvoiceTextFormatter {
        String format(InvoiceLine line, InvoiceAmounts amounts) {
            return String.format(Locale.ROOT, "Faktura: %s x%d netto=%.2f rabat=%.2f VAT=%.2f brutto=%.2f",
                    line.product(), line.quantity(), amounts.net(), amounts.discount(), amounts.vat(), amounts.gross());
        }
    }

    /** InvoiceStore = kontrakt "umiem zapisać i odczytać fakturę" — nadtyp dla różnych sposobów zapisu (patrz DIP). */
    interface InvoiceStore {
        void save(String formattedInvoice);

        List<String> all();
    }

    /** InMemoryInvoiceStore = JEDNA odpowiedzialność: przechowuje faktury w pamięci (na razie — patrz DIP). */
    static class InMemoryInvoiceStore implements InvoiceStore {
        private final List<String> invoices = new ArrayList<>();

        @Override
        public void save(String formattedInvoice) {
            invoices.add(formattedInvoice);
        }

        @Override
        public List<String> all() {
            return List.copyOf(invoices);
        }
    }

    /**
     * 1. Trzy oddzielne klasy zamiast jednej: InvoiceCalculatorFixedRate liczy, InvoiceTextFormatter formatuje,
     * InMemoryInvoiceStore zapisuje. KAŻDA ma JEDEN powód do zmiany: zmiana wzoru liczenia dotyka tylko
     * kalkulatora; zmiana wyglądu faktury — tylko formattera; zmiana sposobu przechowywania — tylko store'a.
     * Żadna zmiana nie "rozlewa się" na pozostałe dwie klasy (w przeciwieństwie do sekcji 0).
     */
    static void srpSplit() {
        section("1. SRP: rozdzielenie liczenia, formatowania i zapisu na osobne klasy");

        InvoiceLine line = new InvoiceLine("Kurs Java", new BigDecimal("100.00"), 2);
        InvoiceCalculatorFixedRate calculator = new InvoiceCalculatorFixedRate();
        InvoiceAmounts amounts = calculator.calculate(line, new BigDecimal("0.10"));   // 10% rabatu, jak VIP w sekcji 0

        InvoiceTextFormatter formatter = new InvoiceTextFormatter();
        String text = formatter.format(line, amounts);

        InvoiceStore store = new InMemoryInvoiceStore();
        store.save(text);

        show("PO: sformatowana faktura (ten sam wynik co w sekcji 0)", text);
        show("PO: store.all().size() po jednym zapisie", store.all().size());
        // WYNIK: PO: sformatowana faktura (ten sam wynik co w sekcji 0) → Faktura: Kurs Java x2 netto=200.00 rabat=20.00 VAT=41.40 brutto=221.40
        // WYNIK: PO: store.all().size() po jednym zapisie → 1

        // dlaczego: ten sam wynik co PRZED — ale teraz liczenie, formatowanie i zapis można TESTOWAĆ, ZMIENIAĆ
        //   i PONOWNIE UŻYWAĆ niezależnie. Discount wciąż jest "na sztywno" przekazywanym parametrem — to
        //   naprawi dopiero OCP w sekcji 2.
    }

    // =================================================================================================
    // 2. OCP — OPEN/CLOSED PRINCIPLE (STRATEGIE RABATU I PODATKU)
    // =================================================================================================

    /** DiscountPolicy = strategia rabatu. Kontrakt: rateFor(...) MUSI zwrócić wartość z przedziału [0.00, 1.00]. */
    interface DiscountPolicy {
        BigDecimal rateFor(InvoiceLine line);
    }

    static class NoDiscount implements DiscountPolicy {
        @Override
        public BigDecimal rateFor(InvoiceLine line) {
            return BigDecimal.ZERO;
        }
    }

    static class VipDiscount implements DiscountPolicy {
        private final BigDecimal rate;

        VipDiscount(BigDecimal rate) {
            this.rate = rate;
        }

        @Override
        public BigDecimal rateFor(InvoiceLine line) {
            return rate;
        }
    }

    /** TaxPolicy = strategia podatku VAT — druga oś rozszerzalności, niezależna od rabatu. */
    interface TaxPolicy {
        BigDecimal rateFor(InvoiceLine line);
    }

    static class StandardVat implements TaxPolicy {
        private final BigDecimal rate;

        StandardVat(BigDecimal rate) {
            this.rate = rate;
        }

        @Override
        public BigDecimal rateFor(InvoiceLine line) {
            return rate;
        }
    }

    static class ZeroVat implements TaxPolicy {           // np. faktura eksportowa — 0% VAT
        @Override
        public BigDecimal rateFor(InvoiceLine line) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * InvoiceCalculator = wersja OTWARTA na rozszerzenie: przyjmuje DiscountPolicy i TaxPolicy przez konstruktor
     * (to już jest wstrzykiwanie przez konstruktor — wrócimy do tego świadomie w sekcji 5, DIP). Waliduje rate
     * z DiscountPolicy — dlaczego, wyjaśnia sekcja 3 (LSP). Od tej pory KAŻDA sekcja używa TEJ klasy.
     */
    static class InvoiceCalculator {
        private final DiscountPolicy discountPolicy;
        private final TaxPolicy taxPolicy;

        InvoiceCalculator(DiscountPolicy discountPolicy, TaxPolicy taxPolicy) {
            this.discountPolicy = discountPolicy;
            this.taxPolicy = taxPolicy;
        }

        InvoiceAmounts calculate(InvoiceLine line) {
            BigDecimal net = line.netPrice().multiply(BigDecimal.valueOf(line.quantity())).setScale(2, RoundingMode.HALF_UP);
            BigDecimal rate = discountPolicy.rateFor(line);
            if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
                // fail fast (CleanCode01, sekcja 7): rate spoza kontraktu [0,1] — patrz sekcja 3 (LSP)
                throw new IllegalStateException("DiscountPolicy zwróciło rate spoza [0,1]: " + rate);
            }
            BigDecimal discount = net.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal afterDiscount = net.subtract(discount);
            BigDecimal vat = afterDiscount.multiply(taxPolicy.rateFor(line)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal gross = afterDiscount.add(vat);
            return new InvoiceAmounts(net, discount, vat, gross);
        }
    }

    /**
     * 2. Zamiast if/else po customerType (sekcja 0), InvoiceCalculator przyjmuje STRATEGIĘ rabatu i podatku.
     * Nowa reguła biznesowa (np. faktura eksportowa bez VAT) to NOWA klasa implementująca interfejs — kod
     * InvoiceCalculator NIE ZMIENIA SIĘ (closed for modification), a system ROŚNIE o nowe możliwości (open
     * for extension). To dosłownie wzorzec Strategia (t22_design_patterns/Patterns01Strategy).
     */
    static void ocpStrategies() {
        section("2. OCP: rabat i VAT jako wymienne strategie, bez edytowania kalkulatora");

        InvoiceLine line = new InvoiceLine("Kurs Java", new BigDecimal("100.00"), 2);

        InvoiceCalculator vipCalculator = new InvoiceCalculator(new VipDiscount(new BigDecimal("0.10")), new StandardVat(new BigDecimal("0.23")));
        show("PO: VIP + standardowy VAT (ten sam wynik co w sekcjach 0 i 1)", vipCalculator.calculate(line));
        // WYNIK: PO: VIP + standardowy VAT (ten sam wynik co w sekcjach 0 i 1) → InvoiceAmounts[net=200.00, discount=20.00, vat=41.40, gross=221.40]

        InvoiceCalculator exportCalculator = new InvoiceCalculator(new NoDiscount(), new ZeroVat());
        show("PO: NOWA kombinacja (eksport: bez rabatu, bez VAT) — ZERO zmian w InvoiceCalculator", exportCalculator.calculate(line));
        // WYNIK: PO: NOWA kombinacja (eksport: bez rabatu, bez VAT) — ZERO zmian w InvoiceCalculator → InvoiceAmounts[net=200.00, discount=0.00, vat=0.00, gross=200.00]

        // dlaczego: gdyby jutro dział marketingu chciał rabatu "za pierwsze zamówienie", dopisujemy KLASĘ
        //   FirstOrderDiscount implements DiscountPolicy — bez dotykania InvoiceCalculator, a więc bez ryzyka
        //   zepsucia rabatu VIP, który już działa i jest przetestowany.
    }

    // =================================================================================================
    // 3. LSP — LISKOV SUBSTITUTION PRINCIPLE
    // =================================================================================================

    /** BrokenDiscount = implementacja DiscountPolicy, która TECHNICZNIE spełnia interfejs, ale ŁAMIE jego kontrakt. */
    static class BrokenDiscount implements DiscountPolicy {
        @Override
        public BigDecimal rateFor(InvoiceLine line) {
            return new BigDecimal("1.50");   // "150% rabatu" — kompiluje się, bo sygnatura się zgadza!
        }
    }

    private static BigDecimal unsafeAfterDiscount(BigDecimal net, BigDecimal rate) {
        return net.subtract(net.multiply(rate)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 3. DiscountPolicy.rateFor deklaruje w komentarzu kontrakt: wynik MUSI być z przedziału [0.00, 1.00].
     * BrokenDiscount jest-a DiscountPolicy w sensie TYPÓW (kompiluje się, implementuje metodę poprawnej
     * sygnatury) — ale ŁAMIE kontrakt behawioralny, którego kompilator NIE SPRAWDZA. To właśnie jest LSP:
     * podtyp musi dać się PODSTAWIĆ za nadtyp BEZ ZASKOCZEŃ, nie tylko przejść kompilację.
     */
    static void lspContractCheck() {
        section("3. LSP: podtyp zgodny typowo, ale łamiący kontrakt zachowania");

        show("Bez ochrony: net 200.00 przy rate 1.50 (150% — poza kontraktem [0,1])",
                unsafeAfterDiscount(new BigDecimal("200.00"), new BigDecimal("1.50")));
        // WYNIK: Bez ochrony: net 200.00 przy rate 1.50 (150% — poza kontraktem [0,1]) → -100.00

        note("Ujemna kwota 'po rabacie' to faktura, na której SPRZEDAWCA dopłaca klientowi — realny błąd biznesowy.");
        // WYNIK:    ℹ Ujemna kwota 'po rabacie' to faktura, na której SPRZEDAWCA dopłaca klientowi — realny błąd biznesowy.

        line();

        InvoiceLine invoiceLine = new InvoiceLine("Kurs Java", new BigDecimal("100.00"), 2);
        InvoiceCalculator brokenCalculator = new InvoiceCalculator(new BrokenDiscount(), new StandardVat(new BigDecimal("0.23")));
        expectThrows("InvoiceCalculator z BrokenDiscount — guard z sekcji 2 wyłapuje złamany kontrakt",
                () -> brokenCalculator.calculate(invoiceLine));
        // WYNIK: ✔ InvoiceCalculator z BrokenDiscount — guard z sekcji 2 wyłapuje złamany kontrakt → rzucono IllegalStateException: DiscountPolicy zwróciło rate spoza [0,1]: 1.50

        // dlaczego: kompilator NIE zna kontraktu napisanego w komentarzu — dlatego InvoiceCalculator dodatkowo
        //   WALIDUJE wynik strategii (fail fast) i rzuca WYJĄTEK zamiast po cichu wypuścić ujemną fakturę.
        //   PRAWDZIWA naprawa to napisanie POPRAWNEJ implementacji DiscountPolicy — walidacja to tylko SIEĆ
        //   BEZPIECZEŃSTWA na wypadek, gdy ktoś (albo Ty za rok) o kontrakcie zapomni.
    }

    // =================================================================================================
    // 4. ISP — INTERFACE SEGREGATION PRINCIPLE
    // =================================================================================================

    /** InvoiceOperationsBad = "gruby" interfejs — każdy implementujący MUSI dostarczyć WSZYSTKIE metody. */
    interface InvoiceOperationsBad {
        InvoiceAmounts calculate(InvoiceLine line);

        String toPlainText(InvoiceLine line, InvoiceAmounts amounts);

        String toHtml(InvoiceLine line, InvoiceAmounts amounts);

        void sendByEmail(String address, String content);

        void archiveToAccountingSystem(String content);
    }

    /**
     * QuickEstimateCalculatorBad = potrzebuje TYLKO liczenia (np. widget "orientacyjna cena" na stronie), ale
     * musi implementować WSZYSTKIE pięć metod grubego interfejsu — cztery z nich to atrapy rzucające wyjątek.
     */
    static class QuickEstimateCalculatorBad implements InvoiceOperationsBad {
        private final InvoiceCalculator calculator = new InvoiceCalculator(new NoDiscount(), new StandardVat(new BigDecimal("0.23")));

        @Override
        public InvoiceAmounts calculate(InvoiceLine line) {
            return calculator.calculate(line);
        }

        @Override
        public String toPlainText(InvoiceLine line, InvoiceAmounts amounts) {
            throw new UnsupportedOperationException("nieużywane w szybkiej wycenie");
        }

        @Override
        public String toHtml(InvoiceLine line, InvoiceAmounts amounts) {
            throw new UnsupportedOperationException("nieużywane w szybkiej wycenie");
        }

        @Override
        public void sendByEmail(String address, String content) {
            throw new UnsupportedOperationException("nieużywane w szybkiej wycenie");
        }

        @Override
        public void archiveToAccountingSystem(String content) {
            throw new UnsupportedOperationException("nieużywane w szybkiej wycenie");
        }
    }

    // --- PO: wąskie interfejsy, każdy implementator bierze TYLKO to, czego naprawdę potrzebuje ---

    interface InvoiceCalculating {
        InvoiceAmounts calculate(InvoiceLine line);
    }

    interface InvoiceRendering {
        String toPlainText(InvoiceLine line, InvoiceAmounts amounts);
    }

    interface InvoiceDispatching {
        void sendByEmail(String address, String content);
    }

    /** QuickEstimateCalculatorGood = implementuje TYLKO to, czego używa — żadnych atrap rzucających wyjątek. */
    static class QuickEstimateCalculatorGood implements InvoiceCalculating {
        private final InvoiceCalculator calculator = new InvoiceCalculator(new NoDiscount(), new StandardVat(new BigDecimal("0.23")));

        @Override
        public InvoiceAmounts calculate(InvoiceLine line) {
            return calculator.calculate(line);
        }
    }

    /**
     * 4. QuickEstimateCalculatorBad implementuje InvoiceOperationsBad w CAŁOŚCI, choć realnie używa tylko
     * calculate — reszta to atrapy rzucające UnsupportedOperationException, czyli błąd ujawnia się DOPIERO
     * przy próbie wywołania (w RUNTIME), nie przy kompilacji. QuickEstimateCalculatorGood implementuje wąski
     * InvoiceCalculating — nie ma CZEGO przypadkowo wywołać niepoprawnie, bo zbędnych metod po prostu NIE MA.
     */
    static void ispNarrowInterfaces() {
        section("4. ISP: wąskie interfejsy zamiast jednego 'grubego'");

        InvoiceLine line = new InvoiceLine("Kurs Java", new BigDecimal("100.00"), 1);

        QuickEstimateCalculatorBad bad = new QuickEstimateCalculatorBad();
        show("PRZED: bad.calculate(line) — jedyna metoda, której faktycznie używamy", bad.calculate(line));
        expectThrows("PRZED: bad.toHtml(...) — metoda 'na siłę' zaimplementowana, wybucha w RUNTIME",
                () -> bad.toHtml(line, bad.calculate(line)));
        // WYNIK: PRZED: bad.calculate(line) — jedyna metoda, której faktycznie używamy → InvoiceAmounts[net=100.00, discount=0.00, vat=23.00, gross=123.00]
        // WYNIK: ✔ PRZED: bad.toHtml(...) — metoda 'na siłę' zaimplementowana, wybucha w RUNTIME → rzucono UnsupportedOperationException: nieużywane w szybkiej wycenie

        line();

        QuickEstimateCalculatorGood good = new QuickEstimateCalculatorGood();
        show("PO: good.calculate(line) — ten sam wynik, bez atrap w klasie", good.calculate(line));
        // WYNIK: PO: good.calculate(line) — ten sam wynik, bez atrap w klasie → InvoiceAmounts[net=100.00, discount=0.00, vat=23.00, gross=123.00]

        // dlaczego: implementator "grubego" interfejsu ZALEŻY od metod, których nie potrzebuje (musi je
        //   znać, skompilować, czasem przypadkowo wywołać) — to koszt utrzymania bez żadnej korzyści.
        //   Wąskie interfejsy (InvoiceCalculating, InvoiceRendering, InvoiceDispatching) pozwalają klasie
        //   zadeklarować DOKŁADNIE to, co robi — nic więcej, nic mniej.
    }

    // =================================================================================================
    // 5. DIP — DEPENDENCY INVERSION PRINCIPLE
    // =================================================================================================

    /** CountingInvoiceStore = zastępnik testowy (test double): liczy zapisy, bez żadnego prawdziwego zapisu. */
    static class CountingInvoiceStore implements InvoiceStore {
        private int savedCount = 0;

        @Override
        public void save(String formattedInvoice) {
            savedCount++;
        }

        @Override
        public List<String> all() {
            throw new UnsupportedOperationException("CountingInvoiceStore tylko liczy zapisy, nie przechowuje treści");
        }

        int savedCount() {
            return savedCount;
        }
    }

    /**
     * BillingService = SPINA wszystkie poprzednie kroki: zależy od InvoiceCalculator, InvoiceTextFormatter
     * i — kluczowe dla DIP — od INTERFEJSU InvoiceStore, a nie konkretnej klasy InMemoryInvoiceStore. Wszystkie
     * trzy zależności są WSTRZYKIWANE przez konstruktor (constructor injection), a nie tworzone przez `new`
     * wewnątrz metody — to jest DIP w praktyce.
     */
    static class BillingService {
        private final InvoiceCalculator calculator;
        private final InvoiceTextFormatter formatter;
        private final InvoiceStore store;

        BillingService(InvoiceCalculator calculator, InvoiceTextFormatter formatter, InvoiceStore store) {
            this.calculator = calculator;
            this.formatter = formatter;
            this.store = store;
        }

        String issueInvoice(InvoiceLine line) {
            InvoiceAmounts amounts = calculator.calculate(line);
            String text = formatter.format(line, amounts);
            store.save(text);
            return text;
        }
    }

    /**
     * 5. BillingService NIE WIE, czy InvoiceStore zapisuje w pamięci, w bazie danych, czy tylko LICZY wywołania
     * (jak CountingInvoiceStore poniżej) — zna tylko interfejs. Dzięki temu w produkcji podstawiamy
     * InMemoryInvoiceStore (albo w przyszłości DatabaseInvoiceStore), a w TESTACH — lekki zastępnik, bez
     * dotykania dysku czy bazy. To jest sedno DIP: moduł WYSOKIEGO poziomu (BillingService) i moduł NISKIEGO
     * poziomu (konkretny store) zależą OBA od abstrakcji (InvoiceStore), a nie jeden od drugiego wprost.
     */
    static void dipConstructorInjection() {
        section("5. DIP: BillingService zależy od interfejsu InvoiceStore, nie od konkretnej klasy");

        InvoiceLine line = new InvoiceLine("Kurs Java", new BigDecimal("100.00"), 2);

        BillingService productionService = new BillingService(
                new InvoiceCalculator(new VipDiscount(new BigDecimal("0.10")), new StandardVat(new BigDecimal("0.23"))),
                new InvoiceTextFormatter(),
                new InMemoryInvoiceStore());
        show("PO: issueInvoice z InMemoryInvoiceStore (ten sam wynik co sekcje 0-2)", productionService.issueInvoice(line));
        // WYNIK: PO: issueInvoice z InMemoryInvoiceStore (ten sam wynik co sekcje 0-2) → Faktura: Kurs Java x2 netto=200.00 rabat=20.00 VAT=41.40 brutto=221.40

        line();

        CountingInvoiceStore countingStore = new CountingInvoiceStore();
        BillingService testService = new BillingService(
                new InvoiceCalculator(new NoDiscount(), new StandardVat(new BigDecimal("0.23"))),
                new InvoiceTextFormatter(),
                countingStore);      // ta sama klasa BillingService, INNY store — bez zmiany jednej linii kodu w BillingService
        testService.issueInvoice(line);
        testService.issueInvoice(line);
        show("PO: countingStore.savedCount() po dwóch wywołaniach — BillingService się nie zmienił", countingStore.savedCount());
        // WYNIK: PO: countingStore.savedCount() po dwóch wywołaniach — BillingService się nie zmienił → 2

        note("Dokładnie to (podmianę zależności na lekki zastępnik) wykorzystuje się w testach jednostkowych —");
        note("patrz t25_testing/Testing02TestDoubles. Framework Spring (t34_toward_spring/Spring01IocContainer)");
        note("robi to samo wstrzykiwanie AUTOMATYCZNIE, na podstawie konfiguracji, zamiast ręcznego 'new' jak tu.");
        // WYNIK:    ℹ Dokładnie to (podmianę zależności na lekki zastępnik) wykorzystuje się w testach jednostkowych —
        // WYNIK:    ℹ patrz t25_testing/Testing02TestDoubles. Framework Spring (t34_toward_spring/Spring01IocContainer)
        // WYNIK:    ℹ robi to samo wstrzykiwanie AUTOMATYCZNIE, na podstawie konfiguracji, zamiast ręcznego 'new' jak tu.

        // dlaczego: gdyby BillingService tworzył InMemoryInvoiceStore sam (przez `new` w środku), nie dałoby się
        //   PODMIENIĆ go w teście bez zmiany kodu produkcyjnego — zależność od ABSTRAKCJI, wstrzyknięta z zewnątrz,
        //   to właśnie to, co czyni klasę TESTOWALNĄ i ELASTYCZNĄ na przyszłe wymagania (nowy sposób zapisu).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • SRP: jedna klasa = jeden powód do zmiany (InvoiceCalculatorFixedRate / InvoiceTextFormatter / InvoiceStore).
     *   • OCP: nowa reguła biznesowa = nowa klasa implementująca strategię (DiscountPolicy/TaxPolicy), ZERO zmian
     *     w InvoiceCalculator — otwarty na rozszerzenie, zamknięty na modyfikację.
     *   • LSP: podtyp (implementacja interfejsu) musi trzymać się KONTRAKTU nadtypu, nie tylko sygnatury metody —
     *     kompilator sprawdza typy, nie zachowanie; złamany kontrakt to realny błąd biznesowy (ujemna faktura).
     *   • ISP: wąski interfejs (InvoiceCalculating) zamiast grubego (InvoiceOperationsBad) — implementator
     *     deklaruje DOKŁADNIE to, co robi, bez atrap rzucających UnsupportedOperationException.
     *   • DIP: moduł wysokiego poziomu (BillingService) zależy od ABSTRAKCJI (InvoiceStore), wstrzykiwanej przez
     *     konstruktor — nie od konkretnej implementacji tworzonej przez `new` w środku.
     *   • SOLID wspiera TESTOWANIE (t25_testing): małe klasy o jednej odpowiedzialności i zależności wstrzykiwane
     *     przez konstruktor łatwo podmienić na zastępniki testowe (test doubles) bez modyfikacji kodu produkcyjnego.
     *   • Spring (t34_toward_spring) automatyzuje DOKŁADNIE wzorzec z sekcji DIP: kontener sam tworzy obiekty
     *     i wstrzykuje zależności zgodnie z deklarowanymi typami (interfejsami), bez ręcznego `new new new`.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego InvoiceModuleBad z sekcji 0 jest trudny do PRZETESTOWANIA (nie tylko trudny do czytania)?
     *   2. Co wypisze:  System.out.println(new InvoiceCalculator(new NoDiscount(), new ZeroVat())
     *          .calculate(new InvoiceLine("X", new BigDecimal("50.00"), 1)));  ?
     *   3. Dlaczego dodanie NOWEJ strategii rabatu (np. FirstOrderDiscount) nie wymaga zmiany ani jednej linii
     *      w InvoiceCalculator? Jaką zasadę SOLID to ilustruje?
     *   4. ZNAJDŹ BŁĄD (kod kompiluje się, ale łamie kontrakt DiscountPolicy z sekcji 3):
     *          static class NegativeDiscount implements DiscountPolicy {
     *              public BigDecimal rateFor(InvoiceLine line) { return new BigDecimal("-0.20"); }
     *          }
     *      Co konkretnie się stanie, gdy InvoiceCalculator dostanie tę implementację?
     *   5. Czym różni się implementowanie InvoiceCalculating (ISP) od implementowania InvoiceOperationsBad,
     *      gdy klasa potrzebuje TYLKO liczenia?
     *   6. Dlaczego BillingService przyjmujący InvoiceStore przez KONSTRUKTOR jest łatwiejszy do przetestowania
     *      niż wersja, która sama tworzy `new InMemoryInvoiceStore()` w środku metody?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** SeasonalDiscount = przykładowa NOWA strategia rabatu do ćwiczenia 1 (OCP) — stały rabat sezonowy. */
    static class SeasonalDiscount implements DiscountPolicy {
        private final BigDecimal rate;

        SeasonalDiscount(BigDecimal rate) {
            this.rate = rate;
        }

        @Override
        public BigDecimal rateFor(InvoiceLine line) {
            return rate;
        }
    }

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");

        InvoiceLine sampleLine = new InvoiceLine("Kubek", new BigDecimal("20.00"), 5);
        Check.equal("ćw. 1: netto po SeasonalDiscount(0.05) i StandardVat(0.23)",
                new InvoiceAmounts(new BigDecimal("100.00"), new BigDecimal("5.00"), new BigDecimal("21.85"), new BigDecimal("116.85")),
                () -> exercise1(sampleLine));
        Check.equal("ćw. 2a: safeRateFor — poprawny rabat w zakresie", new BigDecimal("0.20"), () -> exercise2(new BigDecimal("0.20")));
        Check.equal("ćw. 2b: safeRateFor — za duży rabat obcięty do 1.00", BigDecimal.ONE, () -> exercise2(new BigDecimal("1.50")));
        Check.equal("ćw. 2c: safeRateFor — ujemny rabat obcięty do 0.00", BigDecimal.ZERO, () -> exercise2(new BigDecimal("-0.30")));
        Check.equal("ćw. 3: issueInvoice przez BillingService z CountingInvoiceStore", 1, () -> exercise3(sampleLine));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)",
                new InvoiceAmounts(new BigDecimal("100.00"), new BigDecimal("5.00"), new BigDecimal("21.85"), new BigDecimal("116.85")),
                () -> solution1(sampleLine));
        Check.equal("ćw. 2 (wzorzec, 1.50 → 1.00)", BigDecimal.ONE, () -> solution2(new BigDecimal("1.50")));
        Check.equal("ćw. 3 (wzorzec)", 1, () -> solution3(sampleLine));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 3 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe, OCP): policz kwoty dla sampleLine, używając InvoiceCalculator z gotową klasą
     * SeasonalDiscount(0.05) (5% rabatu) i StandardVat(0.23) — NIE modyfikuj InvoiceCalculator ani DiscountPolicy.
     * Podpowiedź: to dokładnie ten sam wzorzec co VipDiscount w sekcji 2, tylko inna stawka.
     */
    static InvoiceAmounts exercise1(InvoiceLine line) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie, LSP): napisz safeRateFor(rate), które "naprawia" wynik implementacji DiscountPolicy
     * ZAMIAST rzucać wyjątek jak guard w InvoiceCalculator — obcina (clamp) wartość do przedziału [0.00, 1.00]
     * (rate < 0 → 0.00; rate > 1 → 1.00; w przeciwnym razie bez zmian). Podpowiedź: BigDecimal.compareTo.
     */
    static BigDecimal exercise2(BigDecimal rate) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze, DIP): zbuduj BillingService z InvoiceCalculator(NoDiscount, StandardVat(0.23)),
     * InvoiceTextFormatter i NOWYM obiektem CountingInvoiceStore, wywołaj issueInvoice(line) RAZ i zwróć
     * countingStore.savedCount(). Podpowiedź: to ten sam wzorzec co testService w sekcji 5 — jedna zależność
     * (store) jest ZASTĘPNIKIEM testowym, a BillingService o tym nie wie.
     */
    static int exercise3(InvoiceLine line) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static InvoiceAmounts solution1(InvoiceLine line) {
        InvoiceCalculator calculator = new InvoiceCalculator(new SeasonalDiscount(new BigDecimal("0.05")), new StandardVat(new BigDecimal("0.23")));
        return calculator.calculate(line);
    }

    static BigDecimal solution2(BigDecimal rate) {
        if (rate.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (rate.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }
        return rate;
    }

    static int solution3(InvoiceLine line) {
        CountingInvoiceStore store = new CountingInvoiceStore();
        BillingService service = new BillingService(
                new InvoiceCalculator(new NoDiscount(), new StandardVat(new BigDecimal("0.23"))),
                new InvoiceTextFormatter(),
                store);
        service.issueInvoice(line);
        return store.savedCount();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo miesza liczenie, decyzję o rabacie, formatowanie i zapis w JEDNEJ metodzie, a zapis trafia do pola
     *      STATYCZNEGO — nie da się przetestować samego liczenia bez uruchomienia formatowania i zapisu, ani
     *      podmienić żadnego fragmentu na zastępnik testowy.
     *   2. Wypisze InvoiceAmounts[net=50.00, discount=0.00, vat=0.00, gross=50.00] — NoDiscount daje 0% rabatu,
     *      ZeroVat daje 0% VAT, więc netto = brutto.
     *   3. Bo InvoiceCalculator zależy od INTERFEJSU DiscountPolicy, nie od konkretnych klas rabatów — nowa
     *      implementacja interfejsu to nowa, niezależna klasa. To ilustruje OCP (otwarty na rozszerzenie przez
     *      dodanie klasy, zamknięty na modyfikację istniejącego kodu).
     *   4. NegativeDiscount zwraca -0.20, czyli spoza kontraktu [0.00, 1.00] opisanego przy DiscountPolicy —
     *      guard w InvoiceCalculator.calculate wykryje to i rzuci IllegalStateException("DiscountPolicy zwróciło
     *      rate spoza [0,1]: -0.20"), zamiast po cichu policzyć fakturę z ujemnym "rabatem" (czyli DOPŁATĄ).
     *   5. InvoiceCalculating ma TYLKO metodę calculate — klasa implementuje dokładnie to, czego używa, bez
     *      żadnych dodatkowych metod. InvoiceOperationsBad wymusza zaimplementowanie CZTERECH dodatkowych metod
     *      (toPlainText, toHtml, sendByEmail, archiveToAccountingSystem), których klasa nie potrzebuje i nie umie
     *      sensownie zrealizować — więc rzucają wyjątek, co jest ryzykiem odkrywanym dopiero w RUNTIME.
     *   6. Bo w teście można PODAĆ przez konstruktor lekki zastępnik (np. CountingInvoiceStore) zamiast prawdziwego
     *      zapisu — wersja z `new InMemoryInvoiceStore()` "zaszytym" w środku metody ZAWSZE używa tej jednej,
     *      konkretnej implementacji i nie da się jej podmienić bez zmiany kodu klasy BillingService.
     */
    // </editor-fold>
}
