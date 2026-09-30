package t07_inheritance_polymorphism;

import helpers.Check;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: SOLID — pięć zasad projektowania klas, na małych przykładach PRZED/PO
 *        (SRP, OCP, LSP, ISP, DIP — pięć liter, pięć zasad)
 *
 * W SKRÓCIE:
 *   SOLID to pięć zasad (akronim od pierwszych liter), które pomagają pisać klasy łatwe do ZMIANY
 *   i TESTOWANIA bez psucia reszty systemu. Nie są to sztywne prawa fizyki — to praktyczne wskazówki,
 *   które warto znać i świadomie łamać, gdy prostota jest ważniejsza niż elastyczność.
 *
 * ANALOGIA: dobrze zaprojektowany warsztat.
 *   Każde narzędzie robi JEDNĄ rzecz dobrze (SRP). Dokładasz nasadki do wkrętarki zamiast kupować nową
 *   wkrętarkę za każdym razem (OCP). Każda nasadka pasuje tam, gdzie pasuje "uniwersalna" (LSP). Nie
 *   dźwigasz całej szafki z narzędziami, gdy potrzebujesz tylko śrubokręta (ISP). Elektryk podłącza się
 *   do GNIAZDKA (kontraktu), nie do KONKRETNEJ elektrowni za ścianą (DIP).
 *
 * JAK TO DZIAŁA:
 *   (skrót każdej litery — pełne przykłady PRZED/PO w sekcjach 1-5)
 *   S — Single Responsibility:    jedna klasa = jeden powód do zmiany.
 *   O — Open/Closed:              otwarte na rozszerzenie (nowy wariant), zamknięte na modyfikację (stary kod).
 *   L — Liskov Substitution:      podklasa musi dać się użyć wszędzie tam, gdzie klasa bazowa, bez zaskoczeń.
 *   I — Interface Segregation:    małe, wyspecjalizowane interfejsy zamiast jednego "wora" metod.
 *   D — Dependency Inversion:     zależ od interfejsu (abstrakcji), nie od konkretnej implementacji.
 *
 * SŁÓWKA:
 *   responsibility = odpowiedzialność (powód do zmiany klasy); open/closed = otwarte/zamknięte;
 *   substitution = podstawienie; segregation = rozdzielenie (na mniejsze części); dependency = zależność;
 *   inversion = odwrócenie; repository = repozytorium (miejsce przechowywania danych, tu: w pamięci);
 *   in-memory = w pamięci (bez bazy danych/pliku).
 *
 * ZOBACZ TEŻ: t07_inheritance_polymorphism/Inherit04Interfaces (interfejsy — podstawa ISP i DIP),
 *             t07_inheritance_polymorphism/Inherit06CompositionVsInheritance (LSP, kompozycja — pełny obraz),
 *             t22_design_patterns/Patterns08DependencyInjection (DIP jako pełny wzorzec: wstrzykiwanie zależności).
 * </pre>
 */
public class Inherit08Solid {

    public static void main(String[] args) {
        title("Inherit08 — SOLID: pięć zasad na małych przykładach PRZED/PO");

        singleResponsibility();   // single responsibility = zasada pojedynczej odpowiedzialności (SRP)
        openClosed();              // open/closed = zasada otwarte/zamknięte (OCP)
        liskovSubstitution();      // Liskov substitution = zasada podstawienia Liskov (LSP)
        interfaceSegregation();    // interface segregation = zasada rozdzielenia interfejsów (ISP)
        dependencyInversion();     // dependency inversion = zasada odwrócenia zależności (DIP)
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. S — SINGLE RESPONSIBILITY PRINCIPLE (SRP)
    // =================================================================================================

    /**
     * PRZED: SalesReportOld robi TRZY różne rzeczy — liczy sumę, formatuje tekst I "zapisuje" (tu:
     * symulacja). Zmiana formatu wydruku, zmiana sposobu zapisu i zmiana wzoru liczenia to TRZY różne
     * powody do zmiany TEJ SAMEJ klasy — a to właśnie SRP każe rozdzielić.
     */
    static class SalesReportOld {
        List<BigDecimal> amounts;

        SalesReportOld(List<BigDecimal> amounts) {
            this.amounts = amounts;
        }

        BigDecimal computeTotal() {
            BigDecimal total = BigDecimal.ZERO;
            for (BigDecimal a : amounts) {
                total = total.add(a);
            }
            return total;
        }

        String formatReport() {
            return "Suma sprzedaży: " + computeTotal() + " zł";
        }

        String save() {
            return "ZAPISANO: " + formatReport();   // symulacja zapisu (np. do pliku) — bez prawdziwego I/O
        }
    }

    /** PO: trzy małe klasy, każda z JEDNYM powodem do zmiany — liczenie, formatowanie, zapis osobno. */
    static class SalesCalculator {
        BigDecimal total(List<BigDecimal> amounts) {
            BigDecimal total = BigDecimal.ZERO;
            for (BigDecimal a : amounts) {
                total = total.add(a);
            }
            return total;
        }
    }

    static class SalesFormatter {
        String format(BigDecimal total) {
            return "Suma sprzedaży: " + total + " zł";
        }
    }

    static class SalesSaver {
        String save(String formatted) {
            return "ZAPISANO: " + formatted;
        }
    }

    static void singleResponsibility() {
        section("1. S — Single Responsibility Principle (jedna klasa, jeden powód do zmiany)");

        List<BigDecimal> amounts = List.of(new BigDecimal("100.00"), new BigDecimal("250.50"));

        SalesReportOld old = new SalesReportOld(amounts);
        show("PRZED: SalesReportOld.save() (liczy + formatuje + zapisuje w JEDNEJ klasie)", old.save());
        // WYNIK: PRZED: SalesReportOld.save() (liczy + formatuje + zapisuje w JEDNEJ klasie) → ZAPISANO: Suma sprzedaży: 350.50 zł

        BigDecimal total = new SalesCalculator().total(amounts);
        String formatted = new SalesFormatter().format(total);
        String saved = new SalesSaver().save(formatted);
        show("PO: trzy klasy, każda z jedną odpowiedzialnością", saved);
        // WYNIK: PO: trzy klasy, każda z jedną odpowiedzialnością → ZAPISANO: Suma sprzedaży: 350.50 zł

        // DOBRA PRAKTYKA: pytanie kontrolne SRP: "z ilu POWODÓW mogę musieć zmienić tę klasę?" (zmiana
        //   wzoru liczenia? zmiana formatu wydruku? zmiana miejsca zapisu?) — więcej niż jeden = rozważ podział.
    }

    // =================================================================================================
    // 2. O — OPEN/CLOSED PRINCIPLE (OCP)
    // =================================================================================================

    /**
     * PRZED: switch wewnątrz discountOld — dopisanie NOWEGO rodzaju zniżki wymaga EDYCJI tej metody
     * (i ryzyka zepsucia istniejących gałęzi). Klasa jest "otwarta" na modyfikację, a nie na rozszerzenie.
     */
    static BigDecimal discountOld(String customerType, BigDecimal amount) {
        return switch (customerType) {
            case "STUDENT" -> amount.multiply(new BigDecimal("0.90"));
            case "SENIOR" -> amount.multiply(new BigDecimal("0.80"));
            default -> amount;
        };
    }

    /** PO: DiscountRule to interfejs — nowy rodzaj zniżki to NOWA klasa, zero zmian w istniejącym kodzie. */
    interface DiscountRule {
        BigDecimal apply(BigDecimal amount);
    }

    static class StudentDiscount implements DiscountRule {
        @Override
        public BigDecimal apply(BigDecimal amount) {
            return amount.multiply(new BigDecimal("0.90"));
        }
    }

    static class SeniorDiscount implements DiscountRule {
        @Override
        public BigDecimal apply(BigDecimal amount) {
            return amount.multiply(new BigDecimal("0.80"));
        }
    }

    /** VipDiscount = NOWY wariant, dopisany PO fakcie — żaden istniejący kod nie musiał się zmienić. */
    static class VipDiscount implements DiscountRule {
        @Override
        public BigDecimal apply(BigDecimal amount) {
            return amount.multiply(new BigDecimal("0.70"));
        }
    }

    static void openClosed() {
        section("2. O — Open/Closed Principle (otwarte na rozszerzenie, zamknięte na modyfikację)");

        BigDecimal amount = new BigDecimal("200.00");
        show("PRZED: discountOld(\"STUDENT\", 200.00)", discountOld("STUDENT", amount));
        // WYNIK: PRZED: discountOld("STUDENT", 200.00) → 180.0000

        DiscountRule vip = new VipDiscount();   // NOWY wariant — discountOld musiałby dostać kolejny "case"
        show("PO: nowy VipDiscount, ZERO zmian w istniejącym kodzie", vip.apply(amount));
        // WYNIK: PO: nowy VipDiscount, ZERO zmian w istniejącym kodzie → 140.0000

        // DOBRA PRAKTYKA: gdy widzisz switch/if-else rozgałęziający się po "rodzaju czegoś", a lista
        //   rodzajów będzie rosnąć — DiscountRule (interfejs) + osobna klasa na wariant to zwykle lepszy
        //   wybór niż dopisywanie kolejnych gałęzi (ten sam wzorzec co PRZEPISZ w Inherit05Polymorphism).
    }

    // =================================================================================================
    // 3. L — LISKOV SUBSTITUTION PRINCIPLE (LSP)
    // =================================================================================================

    /** ReadOnlyListException = sygnalizuje próbę modyfikacji listy tylko do odczytu. */
    static class ReadOnlyListException extends UnsupportedOperationException {
        ReadOnlyListException(String message) {
            super(message);
        }
    }

    /**
     * ReadOnlyList is-a List (dziedziczy po ArrayList), ale ŁAMIE oczekiwania kodu, który zna tylko
     * List.add(...) i zakłada, że ZAWSZE można dodać element. To DOKŁADNIE ten sam problem co
     * Square extends Rectangle z Inherit06 — podklasa ZWĘŻA kontrakt bazy.
     */
    static class ReadOnlyList<T> extends ArrayList<T> {
        ReadOnlyList(List<T> source) {
            super(source);
        }

        @Override
        public boolean add(T element) {
            throw new ReadOnlyListException("Ta lista jest tylko do odczytu");   // ZASKOCZENIE dla wołającego!
        }
    }

    /** Kod napisany dla "dowolnej" List — zgodnie z LSP powinien działać dla KAŻDEJ implementacji List. */
    static int addAllAndCount(List<String> target, List<String> toAdd) {
        for (String s : toAdd) {
            target.add(s);
        }
        return target.size();
    }

    static void liskovSubstitution() {
        section("3. L — Liskov Substitution Principle (podklasa bez niespodzianek)");

        List<String> normal = new ArrayList<>(List.of("a"));
        show("addAllAndCount(normalna lista, [b, c])", addAllAndCount(normal, List.of("b", "c")));
        // WYNIK: addAllAndCount(normalna lista, [b, c]) → 3

        List<String> readOnly = new ReadOnlyList<>(List.of("a"));
        expectThrows("addAllAndCount(ReadOnlyList, [b, c]) — ZASKOCZENIE, kod tego nie oczekiwał",
                () -> addAllAndCount(readOnly, List.of("b", "c")));
        // WYNIK: ✔ addAllAndCount(ReadOnlyList, [b, c]) — ZASKOCZENIE, kod tego nie oczekiwał → rzucono ReadOnlyListException: Ta lista jest tylko do odczytu

        // PUŁAPKA: addAllAndCount jest napisane dla "dowolnej List<String>" — zgodnie z kontraktem List
        //   powinno działać dla każdej implementacji. ReadOnlyList udaje zwykłą listę (is-a List), ale
        //   RZUCA wyjątek tam, gdzie kod tego nie przewiduje. To właśnie łamie LSP.
        // DOBRA PRAKTYKA: jeśli podklasa musi ODMÓWIĆ wykonania metody z kontraktu bazy, to sygnał, że
        //   dziedziczenie jest złym wyborem — lepiej osobny typ (np. immutable List.copyOf(...) daje
        //   PODOBNY efekt, ale przez jasny, udokumentowany kontrakt List.of/List.copyOf, znany z góry).
    }

    // =================================================================================================
    // 4. I — INTERFACE SEGREGATION PRINCIPLE (ISP)
    // =================================================================================================

    /**
     * PRZED: jeden "gruby" interfejs z metodami, których NIE KAŻDA implementacja potrzebuje. Prosta
     * drukarka musi i tak "coś" zrobić z scan()/fax(), nawet jeśli fizycznie tego nie potrafi.
     */
    interface MultiFunctionDeviceOld {
        void print(String doc);

        void scan(String doc);

        void fax(String doc);
    }

    /** SimplePrinterOld MUSI zaimplementować scan/fax, mimo że fizyczna drukarka tego nie potrafi. */
    static class SimplePrinterOld implements MultiFunctionDeviceOld {
        @Override
        public void print(String doc) {
            System.out.println("    drukuję: " + doc);
        }

        @Override
        public void scan(String doc) {
            throw new UnsupportedOperationException("Ta drukarka nie skanuje");   // "wymuszona" implementacja
        }

        @Override
        public void fax(String doc) {
            throw new UnsupportedOperationException("Ta drukarka nie faksuje");   // to samo
        }
    }

    /** PO: małe, wyspecjalizowane interfejsy — klasa implementuje TYLKO to, co naprawdę potrafi. */
    interface Printer {
        void print(String doc);
    }

    interface Scanner2 {
        void scan(String doc);
    }

    static class SimplePrinter implements Printer {
        @Override
        public void print(String doc) {
            System.out.println("    drukuję: " + doc);
        }
    }

    /** MultiFunctionPrinter implementuje OBA małe interfejsy naraz — dokładnie to, co potrafi, nic więcej. */
    static class MultiFunctionPrinter implements Printer, Scanner2 {
        @Override
        public void print(String doc) {
            System.out.println("    (MFP) drukuję: " + doc);
        }

        @Override
        public void scan(String doc) {
            System.out.println("    (MFP) skanuję: " + doc);
        }
    }

    static void interfaceSegregation() {
        section("4. I — Interface Segregation Principle (małe, wyspecjalizowane interfejsy)");

        SimplePrinterOld oldPrinter = new SimplePrinterOld();
        oldPrinter.print("Faktura.pdf");
        expectThrows("PRZED: SimplePrinterOld.scan(...) — metoda z interfejsu, której nie da się sensownie zaimplementować",
                () -> oldPrinter.scan("Faktura.pdf"));
        // WYNIK:     drukuję: Faktura.pdf
        // WYNIK: ✔ PRZED: SimplePrinterOld.scan(...) — metoda z interfejsu, której nie da się sensownie zaimplementować → rzucono UnsupportedOperationException: Ta drukarka nie skanuje

        Printer simple = new SimplePrinter();
        simple.print("Umowa.pdf");
        // WYNIK:     drukuję: Umowa.pdf

        MultiFunctionPrinter mfp = new MultiFunctionPrinter();
        mfp.print("Raport.pdf");
        mfp.scan("Raport.pdf");
        // WYNIK:     (MFP) drukuję: Raport.pdf
        // WYNIK:     (MFP) skanuję: Raport.pdf

        // DOBRA PRAKTYKA: kod, który potrzebuje TYLKO drukowania, niech przyjmuje typ Printer, nie
        //   MultiFunctionDeviceOld czy nawet MultiFunctionPrinter — węższy interfejs = mniej niepotrzebnych
        //   zależności i łatwiejsze podstawienie prostej implementacji w testach.
    }

    // =================================================================================================
    // 5. D — DEPENDENCY INVERSION PRINCIPLE (DIP)
    // =================================================================================================

    /** PRZED: OrderServiceOld zależy WPROST od KONKRETNEJ, "twardo zakodowanej" mapy w pamięci. */
    static class OrderServiceOld {
        private final Map<String, BigDecimal> ordersInMemoryDb = new HashMap<>();   // KONKRETNA implementacja

        void save(String orderId, BigDecimal total) {
            ordersInMemoryDb.put(orderId, total);
        }

        BigDecimal find(String orderId) {
            return ordersInMemoryDb.getOrDefault(orderId, BigDecimal.ZERO);
        }
    }

    /** PO: OrderRepository to ABSTRAKCJA (interfejs) — OrderService zależy od NIEJ, nie od konkretu. */
    interface OrderRepository {
        void save(String orderId, BigDecimal total);

        BigDecimal find(String orderId);
    }

    /** InMemoryOrderRepository to JEDNA z możliwych implementacji — łatwo podmienić na inną (np. bazę danych). */
    static class InMemoryOrderRepository implements OrderRepository {
        private final Map<String, BigDecimal> data = new HashMap<>();

        @Override
        public void save(String orderId, BigDecimal total) {
            data.put(orderId, total);
        }

        @Override
        public BigDecimal find(String orderId) {
            return data.getOrDefault(orderId, BigDecimal.ZERO);
        }
    }

    /** OrderService zależy TYLKO od interfejsu OrderRepository — wstrzykniętego przez konstruktor. */
    static class OrderService {
        private final OrderRepository repository;   // zależność od ABSTRAKCJI, nie od konkretnej klasy

        OrderService(OrderRepository repository) {   // "wstrzykiwanie zależności" (dependency injection)
            this.repository = repository;
        }

        void placeOrder(String orderId, BigDecimal total) {
            repository.save(orderId, total);
        }

        BigDecimal totalOf(String orderId) {
            return repository.find(orderId);
        }
    }

    static void dependencyInversion() {
        section("5. D — Dependency Inversion Principle (zależ od abstrakcji, nie od konkretu)");

        OrderServiceOld old = new OrderServiceOld();
        old.save("ZAM-1", new BigDecimal("99.00"));
        show("PRZED: OrderServiceOld — na stałe zrośnięty z HashMap", old.find("ZAM-1"));
        // WYNIK: PRZED: OrderServiceOld — na stałe zrośnięty z HashMap → 99.00

        OrderService service = new OrderService(new InMemoryOrderRepository());   // konkret podany Z ZEWNĄTRZ
        service.placeOrder("ZAM-2", new BigDecimal("150.00"));
        show("PO: OrderService zależy od OrderRepository (interfejsu)", service.totalOf("ZAM-2"));
        // WYNIK: PO: OrderService zależy od OrderRepository (interfejsu) → 150.00

        // DOBRA PRAKTYKA: dzięki DIP w testach można podać FAŁSZYWĄ implementację OrderRepository
        //   (np. rzucającą wyjątek albo zawsze zwracającą tę samą wartość) BEZ dotykania OrderService —
        //   pełny wzorzec (kontener, automatyczne wstrzykiwanie): t22_design_patterns/Patterns08DependencyInjection.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   S — Single Responsibility:  jedna klasa = jeden powód do zmiany (SalesCalculator/Formatter/Saver).
     *   O — Open/Closed:            nowy wariant = nowa klasa (DiscountRule), nie edycja starego switcha.
     *   L — Liskov Substitution:    podklasa nie może ZASKAKIWAĆ kodu napisanego dla klasy bazowej
     *                                (ReadOnlyList.add() rzucające wyjątek tam, gdzie List tego nie obiecuje).
     *   I — Interface Segregation:  małe interfejsy (Printer, Scanner2) zamiast jednego "wora" metod.
     *   D — Dependency Inversion:   zależ od interfejsu (OrderRepository), konkret podaj Z ZEWNĄTRZ
     *                                (przez konstruktor) — łatwiej podmienić i testować.
     *   Wspólny mianownik: wszystkie pięć zasad ułatwia ZMIANĘ i TESTOWANIE bez psucia reszty systemu.
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie pytanie kontrolne zadajesz sobie, żeby sprawdzić, czy klasa łamie SRP?
     *   2. Co wypisze:  System.out.println(new VipDiscount().apply(new BigDecimal("100.00")));  ?
     *   3. ZNAJDŹ BŁĄD (projektowy): dlaczego SimplePrinterOld.scan(...) rzucające wyjątek jest sygnałem złamania ISP?
     *   4. Czym różni się DIP od zwykłego "użyj interfejsu zamiast klasy"? (podpowiedź: SKĄD bierze się implementacja)
     *   5. Co wypisze:  System.out.println(discountOld("SENIOR", new BigDecimal("100.00")));  ?
     *   6. Dlaczego ReadOnlyList extends ArrayList łamie LSP, mimo że kod się kompiluje i "działa"?
     *   7. Podaj przykład sytuacji, w której OrderService (DIP) ułatwia pisanie testów.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");

        List<DiscountRule> rules = List.of(new StudentDiscount(), new SeniorDiscount(), new VipDiscount());
        OrderService service = new OrderService(new InMemoryOrderRepository());
        service.placeOrder("ZAM-X", new BigDecimal("300.00"));

        Check.equal("ćw. 1: najtańszy wynik po zastosowaniu WSZYSTKICH zniżek po kolei (OCP)",
                new BigDecimal("100.00").multiply(new BigDecimal("0.90")).multiply(new BigDecimal("0.80"))
                        .multiply(new BigDecimal("0.70")),
                () -> applyAll(rules, new BigDecimal("100.00")));
        Check.equal("ćw. 2: czy repozytorium zna zamówienie ZAM-X (DIP — przez interfejs)", true,
                () -> knowsOrder(service, "ZAM-X"));
        Check.equal("ćw. 3 (PRZEPISZ, ISP): lista urządzeń, które POTRAFIĄ skanować", 1,
                () -> countScanners(List.of(new SimplePrinter(), new MultiFunctionPrinter())));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)",
                new BigDecimal("100.00").multiply(new BigDecimal("0.90")).multiply(new BigDecimal("0.80"))
                        .multiply(new BigDecimal("0.70")),
                () -> solution1(rules, new BigDecimal("100.00")));
        Check.equal("ćw. 2 (wzorzec)", true, () -> solution2(service, "ZAM-X"));
        Check.equal("ćw. 3 (wzorzec)", 1, () -> solution3(List.of(new SimplePrinter(), new MultiFunctionPrinter())));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 3 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe, OCP): zastosuj WSZYSTKIE reguły zniżek z listy po kolei (w kolejności z listy)
     * do kwoty amount i zwróć wynik końcowy.
     */
    static BigDecimal applyAll(List<DiscountRule> rules, BigDecimal amount) {
        // TODO: twoje rozwiązanie
        return BigDecimal.ZERO;
    }

    /**
     * ĆWICZENIE 2 (średnie, DIP): sprawdź, czy service.totalOf(orderId) jest większe od zera — czyli
     * czy repozytorium (dowolna implementacja OrderRepository, wstrzyknięta do service) "zna" to zamówienie.
     */
    static boolean knowsOrder(OrderService service, String orderId) {
        // stub boolean: neutralny "return false" mógłby przypadkiem zdać test, więc zaczynamy od wyjątku
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze, PRZEPISZ, ISP): sekcja 4 pokazała, że "gruby" interfejs zmusza do
     * implementowania metod, których część klas nie potrafi sensownie obsłużyć:
     * <pre>{@code
     * // PRZED (łamie ISP): jeden interfejs, nie każda klasa potrafi wszystko
     * interface MultiFunctionDeviceOld { void print(String d); void scan(String d); void fax(String d); }
     * class SimplePrinterOld implements MultiFunctionDeviceOld {
     *     public void scan(String d) { throw new UnsupportedOperationException(...); }   // wymuszone!
     * }
     * }</pre>
     * PO: dzięki małym interfejsom (Printer, Scanner2) możemy bez rzucania wyjątków SPRAWDZIĆ, które
     * urządzenia z listy POTRAFIĄ skanować (implementują Scanner2), i policzyć ich liczbę.
     */
    static int countScanners(List<Printer> devices) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static BigDecimal solution1(List<DiscountRule> rules, BigDecimal amount) {
        BigDecimal result = amount;
        for (DiscountRule rule : rules) {
            result = rule.apply(result);
        }
        return result;
    }

    static boolean solution2(OrderService service, String orderId) {
        return service.totalOf(orderId).compareTo(BigDecimal.ZERO) > 0;
    }

    static int solution3(List<Printer> devices) {
        int count = 0;
        for (Printer p : devices) {
            if (p instanceof Scanner2) {
                count++;
            }
        }
        return count;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. "Z ilu RÓŻNYCH powodów mógłbym musieć zmienić tę klasę?" (np. zmiana wzoru liczenia, zmiana
     *      formatu wydruku, zmiana sposobu zapisu — to TRZY różne powody, więc SRP każe je rozdzielić).
     *   2. "70.00" — VipDiscount mnoży przez 0.70, więc 100.00 × 0.70 = 70.0000 (BigDecimal pokaże pełną
     *      skalę wynikającą z mnożenia, np. "70.0000").
     *   3. Bo interfejs MultiFunctionDeviceOld WYMUSZA na SimplePrinterOld zaimplementowanie scan()/fax(),
     *      mimo że fizyczna prosta drukarka tego nie potrafi — jedyna "uczciwa" implementacja to rzucenie
     *      wyjątku, co jest sygnałem, że interfejs jest za gruby (miesza role, które nie zawsze idą razem).
     *   4. Samo "użyj interfejsu" mówi TYLKO o typie zależności. DIP mówi też, SKĄD bierze się KONKRETNA
     *      implementacja — nie tworzy jej sama klasa wewnątrz siebie (jak OrderServiceOld tworzące własny
     *      HashMap), tylko dostaje ją "z zewnątrz" (przez konstruktor) — klasa nie decyduje, z CZYM dokładnie pracuje.
     *   5. "80.0000" — SENIOR to rabat ×0.80, więc 100.00 × 0.80 = 80.0000.
     *   6. Bo List (kontrakt, który ReadOnlyList obiecuje spełniać przez "is-a") mówi, że add(...) DODAJE
     *      element — ReadOnlyList zamiast tego rzuca wyjątek. Kod napisany dla "dowolnej List" (jak
     *      addAllAndCount) przestaje działać poprawnie, mimo że kompiluje się bez ostrzeżeń.
     *   7. W teście można podać WŁASNĄ, fałszywą implementację OrderRepository (np. taką, która ZAWSZE
     *      zwraca ustaloną wartość, albo rzuca wyjątek przy save()), żeby sprawdzić zachowanie OrderService
     *      W IZOLACJI — bez prawdziwej bazy danych czy pliku (pełny obraz testowania: t25_testing).
     */
    // </editor-fold>
}
