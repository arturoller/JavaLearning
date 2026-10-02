package t22_design_patterns;

import helpers.Check;
import helpers.SampleData;
import helpers.TempDir;
import helpers.model.Order;
import helpers.model.OrderLine;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Facade (fasada) — jedna prosta „recepcja” przed skomplikowanym zapleczem
 *        (facade = fasada, front budynku; subsystem = podsystem; client = klient, ten kto korzysta;
 *         orchestration = koordynacja kroków; compensation = wycofanie skutków, np. zwolnienie towaru)
 *
 * W SKRÓCIE:
 *   Fasada to klasa z KILKOMA prostymi metodami (np. {@code placeOrder}), która w środku woła wiele podsystemów
 *   we właściwej kolejności i z obsługą błędów. Klient zna jedną klasę zamiast pięciu i nie musi wiedzieć,
 *   że przed wysyłką trzeba pobrać płatność, a po nieudanej płatności — zwolnić towar.
 *
 * ANALOGIA: recepcja w hotelu. Chcesz „zameldować się”. Nie biegasz sam do działu rezerwacji, kasy, pokojówek
 *   i sejfu — mówisz recepcjonistce jedno zdanie, a ona załatwia wszystko za kulisami we właściwej kolejności.
 *   Jeśli bardzo chcesz, możesz zejść do kuchni i porozmawiać z kucharzem bezpośrednio (nikt tego nie zabrania) —
 *   ale do codziennych spraw recepcja jest wygodniejsza i mniej się mylisz.
 *
 * JAK TO DZIAŁA:
 *   Role GoF:  Facade (fasada) — prosty interfejs do typowych zastosowań;  Subsystem classes (klasy podsystemu) —
 *              robią właściwą pracę, o fasadzie nic nie wiedzą;  Client (klient) — używa fasady.
 *
 *     PRZED:  klient --> Magazyn, Płatności, Faktury, Wysyłka, Powiadomienia   (zna 5 klas i ich kolejność)
 *     PO:     klient --> OrderFacade --> Magazyn, Płatności, Faktury, Wysyłka, Powiadomienia
 *
 *   Fasada NIE zastępuje podsystemów i nie dodaje logiki biznesowej: tylko KOORDYNUJE (kolejność, błędy,
 *   wycofanie). Reguły (ile kosztuje, czy jest towar) zostają w podsystemach. Inaczej fasada zamienia się
 *   w „klasę-boga” (god class).
 *   Czym różni się od podobnych wzorców: Adapter ZMIENIA interfejs jednej klasy, żeby pasował do oczekiwanego
 *   (t22 Patterns11Adapter); Decorator zachowuje interfejs i dokłada zachowanie (t22 Patterns07Decorator);
 *   Facade tworzy NOWY, prostszy interfejs do całej grupy klas.
 *
 * SŁÓWKA:
 *   facade = fasada; subsystem = podsystem; orchestrate = koordynować; reserve = zarezerwuj;
 *   release = zwolnij; charge = obciąż (kartę); invoice = faktura; shipping = wysyłka;
 *   notifier = powiadamiacz; confirmation = potwierdzenie; god class = klasa-bóg (robi wszystko).
 *
 * ZOBACZ TEŻ: t22_design_patterns/Patterns08DependencyInjection (podsystemy wstrzyknięte do fasady),
 *   t22_design_patterns/Patterns11Adapter (adapter kontra fasada),
 *   t18_io_files/Io01PathFiles (Files jako fasada),
 *   t27_clean_code_pitfalls/CleanCode03Architecture (warstwa serwisów),
 *   t34_toward_spring/Spring02Layers (serwis jako fasada nad repozytoriami)
 * </pre>
 */
public class Patterns10Facade {

    public static void main(String[] args) {
        title("Patterns10 — Facade (fasada)");

        problem();              // problem = problem (klient zna wszystkie podsystemy)
        facadeStepByStep();     // facade step by step = fasada krok po kroku
        facadeAndFailures();    // facade and failures = fasada i błędy (wycofanie)
        directAccessAndLevels();// direct access and levels = dostęp bezpośredni i kilka fasad
        facadeVsGodClass();     // facade vs god class = fasada a klasa-bóg
        jdkFacades();           // jdk facades = fasady w JDK (Files, SLF4J)
        springFacades();        // spring facades = fasady w Springu
        testability();          // testability = łatwość testowania
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // Podsystemy (w prawdziwym programie to osobne moduły; tu proste klasy w pamięci)
    // =================================================================================================

    /** Podsystem 1: magazyn. */
    interface Inventory {
        void reserve(String sku, int quantity); // reserve = zarezerwuj; sku = kod produktu

        void release(String sku, int quantity); // release = zwolnij rezerwację

        int available(String sku);              // available = dostępne sztuki
    }

    /** Podsystem 2: płatności. Zwraca numer płatności. */
    interface Payments {
        String charge(String customer, long amountInGrosze); // charge = obciąż; grosze = setne części złotego
    }

    /** Podsystem 3: faktury. Zwraca numer faktury. */
    interface Invoices {
        String issue(String orderId, long amountInGrosze); // issue = wystaw
    }

    /** Podsystem 4: wysyłka. Zwraca numer przesyłki. */
    interface Shipping {
        String schedule(String orderId, String city); // schedule = zaplanuj
    }

    /** Podsystem 5: powiadomienia. */
    interface Notifier {
        void send(String email, String text);
    }

    /** Wynik zamówienia: dane, które klient faktycznie potrzebuje (nie wystawiamy wnętrzności podsystemów). */
    record Confirmation(String orderId, String invoiceNumber, String trackingNumber) { }

    /** Magazyn w pamięci: zapasy z przykładowych produktów; zapisuje ślad (trace) wywołań. */
    static final class InMemoryInventory implements Inventory {
        private final Map<String, Integer> stock = new LinkedHashMap<>(); // stock = stan magazynu
        private final List<String> trace;

        InMemoryInventory(List<String> trace) {
            this.trace = trace;
            SampleData.products().forEach(product -> stock.put(product.sku(), product.stock()));
        }

        @Override
        public void reserve(String sku, int quantity) {
            int have = available(sku);
            if (have < quantity) {
                throw new IllegalStateException("brak towaru " + sku + ": jest " + have + ", potrzeba " + quantity);
            }
            stock.put(sku, have - quantity);
            trace.add("magazyn: rezerwacja " + sku + " x" + quantity);
        }

        @Override
        public void release(String sku, int quantity) {
            stock.merge(sku, quantity, Integer::sum); // merge = dodaj do istniejącej wartości
            trace.add("magazyn: zwolnienie " + sku + " x" + quantity);
        }

        @Override
        public int available(String sku) {
            return stock.getOrDefault(sku, 0);
        }
    }

    /** Płatności: odrzuca kwoty powyżej limitu 5000 zł (500000 gr). */
    static final class LimitedPayments implements Payments {
        private final List<String> trace;
        private int counter; // counter = licznik płatności

        LimitedPayments(List<String> trace) {
            this.trace = trace;
        }

        @Override
        public String charge(String customer, long amountInGrosze) {
            if (amountInGrosze > 500_000) {
                throw new IllegalStateException("płatność odrzucona: " + amountInGrosze + " gr przekracza limit 500000 gr");
            }
            counter++;
            trace.add("płatność: obciążono " + customer + " kwotą " + amountInGrosze + " gr");
            return "PAY-" + counter;
        }
    }

    /** Faktury: numeruje kolejno FV/2026/001, FV/2026/002... */
    static final class SequentialInvoices implements Invoices {
        private final List<String> trace;
        private int counter;

        SequentialInvoices(List<String> trace) {
            this.trace = trace;
        }

        @Override
        public String issue(String orderId, long amountInGrosze) {
            counter++;
            String number = String.format(Locale.ROOT, "FV/2026/%03d", counter); // Locale.ROOT = bez ustawień regionalnych
            trace.add("faktura: " + number + " dla " + orderId);
            return number;
        }
    }

    /** Wysyłka: numer przesyłki zbudowany z numeru zamówienia. */
    static final class CourierShipping implements Shipping {
        private final List<String> trace;

        CourierShipping(List<String> trace) {
            this.trace = trace;
        }

        @Override
        public String schedule(String orderId, String city) {
            trace.add("wysyłka: " + orderId + " do miasta " + city);
            return "TRK-" + orderId;
        }
    }

    /** Powiadomienia: tylko zapisuje, co by wysłano. */
    static final class TraceNotifier implements Notifier {
        private final List<String> trace;

        TraceNotifier(List<String> trace) {
            this.trace = trace;
        }

        @Override
        public void send(String email, String text) {
            trace.add("powiadomienie do " + email + ": " + text);
        }
    }

    /** Pomocnik: kwota zamówienia w groszach (total ma skalę 2, więc przesunięcie przecinka daje liczbę całkowitą). */
    static long grosze(Order order) {
        return order.total().movePointRight(2).longValueExact();
    }

    // =================================================================================================
    // Fasada
    // =================================================================================================

    /**
     * Fasada zamówień: {@code placeOrder} (złóż zamówienie) i {@code cancelOrder} (anuluj). Zależności dostaje
     * w konstruktorze (t22 Patterns08DependencyInjection) jako interfejsy, więc w teście można je podmienić.
     */
    static final class OrderFacade {
        private final Inventory inventory;
        private final Payments payments;
        private final Invoices invoices;
        private final Shipping shipping;
        private final Notifier notifier;

        OrderFacade(Inventory inventory, Payments payments, Invoices invoices, Shipping shipping, Notifier notifier) {
            this.inventory = inventory;
            this.payments = payments;
            this.invoices = invoices;
            this.shipping = shipping;
            this.notifier = notifier;
        }

        /** Cały proces w jednym wywołaniu: rezerwacja → płatność → faktura → wysyłka → powiadomienie. */
        Confirmation placeOrder(Order order) { // placeOrder = złóż zamówienie
            List<OrderLine> reserved = new ArrayList<>(); // reserved = zarezerwowane pozycje (do ewentualnego zwolnienia)
            try {
                for (OrderLine line : order.lines()) {
                    inventory.reserve(line.product().sku(), line.quantity());
                    reserved.add(line);
                }
                payments.charge(order.customer().name(), grosze(order));
            } catch (RuntimeException ex) {
                for (OrderLine line : reserved) { // wycofanie (kompensacja): oddaj towar do magazynu
                    inventory.release(line.product().sku(), line.quantity());
                }
                throw ex;
            }
            String invoiceNumber = invoices.issue(order.id(), grosze(order));
            String trackingNumber = shipping.schedule(order.id(), order.customer().city());
            order.customer().findEmail().ifPresent(email ->
                    notifier.send(email, "zamówienie " + order.id() + " przyjęte, paczka " + trackingNumber));
            return new Confirmation(order.id(), invoiceNumber, trackingNumber);
        }

        /** Drugi przypadek użycia: anulowanie zwalnia towar i informuje klienta. */
        void cancelOrder(Order order) { // cancelOrder = anuluj zamówienie
            for (OrderLine line : order.lines()) {
                inventory.release(line.product().sku(), line.quantity());
            }
            order.customer().findEmail().ifPresent(email -> notifier.send(email, "zamówienie " + order.id() + " anulowane"));
        }
    }

    /** Jedyne miejsce, które zna konkretne klasy podsystemów (korzeń kompozycji, t22 Patterns08). */
    static OrderFacade standardFacade(Inventory inventory, List<String> trace) {
        return new OrderFacade(inventory, new LimitedPayments(trace), new SequentialInvoices(trace),
                new CourierShipping(trace), new TraceNotifier(trace));
    }

    // =================================================================================================
    // 1. PROBLEM
    // =================================================================================================

    /**
     * Kod klienta BEZ fasady: zna pięć podsystemów, ich kolejność i sposób reagowania na błędy.
     * Taki kod pojawia się w kontrolerze WWW, w imporcie wsadowym, w konsoli administratora...
     */
    static void manualCheckout(Order order, Inventory inventory, Payments payments, Invoices invoices,
                               Shipping shipping, Notifier notifier) {
        for (OrderLine line : order.lines()) {
            inventory.reserve(line.product().sku(), line.quantity());
        }
        payments.charge(order.customer().name(), grosze(order));
        String invoice = invoices.issue(order.id(), grosze(order));
        String tracking = shipping.schedule(order.id(), order.customer().city());
        order.customer().findEmail().ifPresent(email ->
                notifier.send(email, "zamówienie " + order.id() + " przyjęte, faktura " + invoice + ", paczka " + tracking));
    }

    /** Kopia tego samego kodu w imporcie wsadowym — ktoś „na szybko” pominął fakturę i zwolnienie towaru. */
    static void batchCheckout(Order order, Inventory inventory, Payments payments, Shipping shipping) {
        for (OrderLine line : order.lines()) {
            inventory.reserve(line.product().sku(), line.quantity());
        }
        payments.charge(order.customer().name(), grosze(order));
        shipping.schedule(order.id(), order.customer().city());
    }

    /**
     * 1. Problem: logika procesu jest rozsiana po klientach. Każdy musi znać wszystkie podsystemy, a jedna
     * pomyłka (pominięta faktura, brak zwolnienia towaru po odrzuconej płatności) kończy się błędem danych.
     */
    static void problem() {
        section("1. Problem — klient zna wszystkie podsystemy");

        List<String> trace = new ArrayList<>();
        InMemoryInventory inventory = new InMemoryInventory(trace);
        Order okOrder = SampleData.orders().get(2); // ZAM-003: 307,00 zł, klient z e-mailem

        manualCheckout(okOrder, inventory, new LimitedPayments(trace), new SequentialInvoices(trace),
                new CourierShipping(trace), new TraceNotifier(trace));
        showEach("klient WWW (poprawny)", trace);
        // WYNIK: klient WWW (poprawny) (liczba elementów: 7):
        // WYNIK: • magazyn: rezerwacja KSI-001 x1
        // WYNIK: • magazyn: rezerwacja KSI-002 x1
        // WYNIK: • magazyn: rezerwacja KSI-003 x1
        // WYNIK: • płatność: obciążono Jan Kowalski kwotą 30700 gr
        // WYNIK: • faktura: FV/2026/001 dla ZAM-003
        // WYNIK: • wysyłka: ZAM-003 do miasta Warszawa
        // WYNIK: • powiadomienie do jan@example.com: zamówienie ZAM-003 przyjęte, faktura FV/2026/001, paczka TRK-ZAM-003

        trace.clear();
        batchCheckout(SampleData.orders().get(1), inventory, new LimitedPayments(trace), new CourierShipping(trace));
        showEach("import wsadowy (bez faktury!)", trace);
        // WYNIK: import wsadowy (bez faktury!) (liczba elementów: 4):
        // WYNIK: • magazyn: rezerwacja SPO-001 x3
        // WYNIK: • magazyn: rezerwacja SPO-002 x10
        // WYNIK: • płatność: obciążono Maria Nowak kwotą 26987 gr
        // WYNIK: • wysyłka: ZAM-002 do miasta Kraków

        trace.clear();
        Order bigOrder = SampleData.orders().get(0); // ZAM-001: 6199,79 zł — powyżej limitu płatności
        int before = inventory.available("ELE-001");
        try {
            batchCheckout(bigOrder, inventory, new LimitedPayments(trace), new CourierShipping(trace));
        } catch (IllegalStateException ex) {
            show("płatność nieudana", ex.getMessage());
            // WYNIK: płatność nieudana → płatność odrzucona: 619979 gr przekracza limit 500000 gr
        }
        show("laptopów przed / po nieudanym zamówieniu", before + " / " + inventory.available("ELE-001"));
        // WYNIK: laptopów przed / po nieudanym zamówieniu → 7 / 6

        // PUŁAPKA: kopiowanie procesu do wielu klientów. Dlaczego: (1) każda kopia może „zapomnieć” krok
        //   (tu: fakturę i powiadomienie), (2) po zmianie procesu (np. nowy krok „sprawdź oszustwo”) trzeba
        //   poprawić N miejsc i któreś pominiemy, (3) obsługa błędów (zwolnienie towaru) bywa pomijana —
        //   ELE-001 „zniknął” z magazynu mimo odrzuconej płatności. To właśnie ten ból leczy fasada.
    }

    // =================================================================================================
    // 2. FASADA KROK PO KROKU
    // =================================================================================================

    /**
     * 2. Fasada: klient woła jedną metodę. Kolejność kroków, wycofanie po błędzie i budowanie powiadomienia
     * są w jednym miejscu — każdy klient dostaje ten sam, sprawdzony proces.
     */
    static void facadeStepByStep() {
        section("2. Fasada — jedna metoda zamiast pięciu podsystemów");

        List<String> trace = new ArrayList<>();
        OrderFacade facade = standardFacade(new InMemoryInventory(trace), trace);

        Confirmation confirmation = facade.placeOrder(SampleData.orders().get(2));
        show("potwierdzenie", confirmation);
        // WYNIK: potwierdzenie → Confirmation[orderId=ZAM-003, invoiceNumber=FV/2026/001, trackingNumber=TRK-ZAM-003]
        showEach("ślad kroków", trace);
        // WYNIK: ślad kroków (liczba elementów: 7):
        // WYNIK: • magazyn: rezerwacja KSI-001 x1
        // WYNIK: • magazyn: rezerwacja KSI-002 x1
        // WYNIK: • magazyn: rezerwacja KSI-003 x1
        // WYNIK: • płatność: obciążono Jan Kowalski kwotą 30700 gr
        // WYNIK: • faktura: FV/2026/001 dla ZAM-003
        // WYNIK: • wysyłka: ZAM-003 do miasta Warszawa
        // WYNIK: • powiadomienie do jan@example.com: zamówienie ZAM-003 przyjęte, paczka TRK-ZAM-003

        // Klient (kontroler, import, konsola) wygląda teraz tak: `facade.placeOrder(order)` — i nic więcej.
        // Fasada zwraca Confirmation (record z trzema polami), a nie obiekty podsystemów: gdyby metody fasady
        // wystawiały typy z wnętrza (np. wynik magazynu), klient znowu zależałby od podsystemów i cały pomysł
        // by przepadł. DOBRA PRAKTYKA: sygnatura fasady używa typów domeny/DTO (nośników danych), nie typów podsystemów.
        // DOBRA PRAKTYKA: fasada przyjmuje zależności w konstruktorze (jako interfejsy) — podsystemy da się
        //   podmienić w teście (sekcja 8) bez zmiany ani jednej linii fasady.
    }

    // =================================================================================================
    // 3. FASADA I BŁĘDY
    // =================================================================================================

    /**
     * 3. Błędy: fasada jest jedynym miejscem, które wie, co wycofać po nieudanym kroku. Odrzucona płatność
     * zwalnia zarezerwowany towar; brak towaru w trakcie rezerwacji zwalnia to, co już zarezerwowano.
     */
    static void facadeAndFailures() {
        section("3. Błędy — wycofanie w jednym miejscu");

        List<String> trace = new ArrayList<>();
        InMemoryInventory inventory = new InMemoryInventory(trace);
        OrderFacade facade = standardFacade(inventory, trace);

        int before = inventory.available("ELE-001");
        expectThrows("płatność odrzucona (ZAM-001)", () -> facade.placeOrder(SampleData.orders().get(0)));
        // WYNIK: ✔ płatność odrzucona (ZAM-001) → rzucono IllegalStateException: płatność odrzucona: 619979 gr przekracza limit 500000 gr
        show("laptopów przed / po", before + " / " + inventory.available("ELE-001"));
        // WYNIK: laptopów przed / po → 7 / 7
        showEach("ślad", trace);
        // WYNIK: ślad (liczba elementów: 4):
        // WYNIK: • magazyn: rezerwacja ELE-001 x1
        // WYNIK: • magazyn: rezerwacja ELE-003 x2
        // WYNIK: • magazyn: zwolnienie ELE-001 x1
        // WYNIK: • magazyn: zwolnienie ELE-003 x2

        trace.clear();
        expectThrows("brak towaru w magazynie (ZAM-004: Smartfon X)", () -> facade.placeOrder(SampleData.orders().get(3)));
        // WYNIK: ✔ brak towaru w magazynie (ZAM-004: Smartfon X) → rzucono IllegalStateException: brak towaru ELE-002: jest 0, potrzeba 1
        show("ślad po braku towaru (pusty = nic nie obciążono)", trace);
        // WYNIK: ślad po braku towaru (pusty = nic nie obciążono) → []

        // Nie ma czego wycofywać, gdy pierwsza rezerwacja się nie uda, i nikt nie obciążył karty — kolejność
        // kroków (najpierw rezerwacja, potem płatność) jest decyzją fasady i dzięki temu klient jej nie psuje.
        // Granica fasady: jeśli błąd wystąpi PO pobraniu pieniędzy (np. padnie system faktur), potrzebny
        //   byłby zwrot płatności — to już temat transakcji rozproszonych i wzorca „saga”. Fasada z jednym
        //   try/catch tego nie rozwiąże; nie udawaj, że rozwiązuje.
        // PUŁAPKA: połknięcie błędu w fasadzie (`catch (Exception e) { return null; }`) ukrywa przyczynę przed
        //   klientem. Zwolnij zasoby i RZUĆ wyjątek dalej (tu: throw ex), żeby klient wiedział, że zamówienie nie przeszło.
    }

    // =================================================================================================
    // 4. DOSTĘP BEZPOŚREDNI I KILKA FASAD
    // =================================================================================================

    /**
     * 4. Fasada nie zabrania dostępu do podsystemów: ekran administratora może pytać magazyn bezpośrednio.
     * Fasada upraszcza TYPOWE scenariusze, a rzadkie nadal są możliwe. Gdy fasada puchnie, dzielimy ją
     * na kilka (po przypadkach użycia), a nie dokładamy metod bez końca.
     */
    static void directAccessAndLevels() {
        section("4. Fasada nie blokuje dostępu — i nie musi być jedna");

        List<String> trace = new ArrayList<>();
        InMemoryInventory inventory = new InMemoryInventory(trace);
        OrderFacade facade = standardFacade(inventory, trace);

        show("dostępne sztuki Słuchawek BT (wprost z magazynu)", inventory.available("ELE-003"));
        // WYNIK: dostępne sztuki Słuchawek BT (wprost z magazynu) → 25
        Order order = SampleData.orders().get(8); // ZAM-009: Słuchawki BT x1, klient Maria Nowak (bez e-maila)
        facade.placeOrder(order);
        show("po złożeniu zamówienia", inventory.available("ELE-003"));
        // WYNIK: po złożeniu zamówienia → 24
        facade.cancelOrder(order);
        show("po anulowaniu", inventory.available("ELE-003"));
        // WYNIK: po anulowaniu → 25

        // Jak wymusić używanie fasady, gdy to ważne? Widocznością: podsystemy w pakiecie z modyfikatorem
        // package-private (bez public), a publiczna tylko fasada; albo moduły (t30 Modules: exports tylko pakietu fasady).
        // To wzorzec „pakiet jako moduł”: wnętrze jest ukryte, wejście jest jedno. W małych programach
        // zwykle wystarcza umowa zespołu — fasada jest ścieżką domyślną, nie więzieniem.
        //
        // Wiele fasad: OrderFacade (składanie i anulowanie), ReturnsFacade (zwroty), ReportingFacade (raporty).
        // Każda ma kilka metod jednego obszaru. Zasada: jeśli fasada ma 25 metod i nowe wpisujesz co tydzień,
        // to już nie fasada, tylko klasa-bóg — podziel ją według przypadków użycia.
    }

    // =================================================================================================
    // 5. FASADA A KLASA-BÓG
    // =================================================================================================

    /**
     * 5. Fasada kontra klasa-bóg (god class). Fasada KOORDYNUJE i deleguje, a reguły siedzą w podsystemach.
     * Klasa-bóg liczy podatki, odpytuje bazę, formatuje fakturę i wysyła maile sama. Test praktyczny:
     * czy po usunięciu fasady cała wiedza o procesie jest w innym miejscu? Jeśli nie — to nie fasada.
     */
    static void facadeVsGodClass() {
        section("5. Fasada a klasa-bóg — jak je odróżnić");

        List<String> trace = new ArrayList<>();
        OrderFacade facade = standardFacade(new InMemoryInventory(trace), trace);
        facade.placeOrder(SampleData.orders().get(2));
        show("kroków w śladzie (wszystkie wykonały podsystemy)", trace.size());
        // WYNIK: kroków w śladzie (wszystkie wykonały podsystemy) → 7
        // Cienka fasada ma garść metod. Sprawdźmy refleksją (t19), jakie metody (nie prywatne, nie syntetyczne)
        // widzi klient: dostajemy tylko dwa przypadki użycia.
        List<String> methods = Arrays.stream(OrderFacade.class.getDeclaredMethods())
                .filter(method -> !method.isSynthetic() && !Modifier.isPrivate(method.getModifiers()))
                .map(Method::getName)
                .sorted()
                .toList();
        show("metody fasady dla klienta", methods);
        // WYNIK: metody fasady dla klienta → [cancelOrder, placeOrder]

        // Porównanie:
        //   | cecha                  | Fasada                               | Klasa-bóg (God class)                     |
        //   | co robi                | koordynuje: kolejność, błędy         | robi wszystko sama: reguły, SQL, formaty  |
        //   | logika biznesowa       | w podsystemach                       | w jednej klasie                           |
        //   | zależność od niej      | klient zna 1 klasę                   | wszyscy znają 1 klasę, a ona zna wszystko |
        //   | zmiana reguły (VAT)    | w podsystemie, fasada bez zmian      | edycja wielkiej klasy                     |
        //   | wielkość               | cienka (kilka metod, kilkadziesiąt linii) | tysiące linii                        |
        // Fasada ≠ Adapter: adapter dopasowuje JEDEN cudzy interfejs do oczekiwanego (t22 Patterns11Adapter);
        // fasada UPRASZCZA wiele klas, których interfejsów nie zmienia. Fasada ≠ Mediator: w mediatorze podsystemy
        // rozmawiają PRZEZ pośrednika między sobą, a w fasadzie podsystemy nie wiedzą o fasadzie.
        // PUŁAPKA: fasada, do której „na chwilę” dopisano obliczanie rabatów i walidację adresów. Po roku
        //   to najcięższa klasa projektu, której nikt nie chce ruszać. Reguła: logika → do podsystemu,
        //   w fasadzie zostaje tylko sekwencja wywołań i obsługa błędów.
    }

    // =================================================================================================
    // 6. FASADY W JDK
    // =================================================================================================

    /**
     * 6. W JDK fasadą jest klasa java.nio.file.Files: jedna linijka zamiast kombinacji strumieni, kodowania
     * i zamykania zasobów. Nazwa „fasada” pada też w SLF4J.
     */
    static void jdkFacades() {
        section("6. Fasady w JDK — Files i SLF4J");

        Path dir = TempDir.create("pat10");
        try {
            Path file = dir.resolve("zamowienia.txt");
            Files.writeString(file, "ZAM-001\nZAM-002", StandardCharsets.UTF_8); // writeString = zapisz tekst (Java 11+)
            String text = Files.readString(file, StandardCharsets.UTF_8);        // readString = wczytaj cały tekst (Java 11+)
            show("Files.readString → linie", text.lines().toList()); // lines = linie (Java 11+); toList (Java 16+)
            // WYNIK: Files.readString → linie → [ZAM-001, ZAM-002]

            // To samo „po staremu”, bez fasady — trzeba znać strumień bajtów, dekoder znaków, bufor i zamykanie:
            List<String> classic = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file.toFile()), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    classic.add(line);
                }
            }
            show("po staremu (wynik ten sam)", classic);
            // WYNIK: po staremu (wynik ten sam) → [ZAM-001, ZAM-002]
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            TempDir.deleteRecursively(dir);
        }

        // Files.* ukrywa: otwieranie kanału, wybór kodowania, bufory, zamykanie zasobu i opcje (nadpisz/dopisz).
        // Fasada nie zabrania zejścia niżej: gdy potrzebujesz własnego bufora albo odczytu fragmentu pliku,
        // używasz Files.newInputStream / FileChannel (t18 Io01PathFiles).
        // Inne fasady w JDK: Executors (fabryki pul wątków — ukrywają konfigurację ThreadPoolExecutor, t21),
        //   java.net.http.HttpClient (jedna klasa nad gniazdami i protokołem), javax.xml / JAXP (fabryki parserów).
        //
        // SLF4J (Simple Logging Facade for Java = prosta fasada logowania dla Javy): to nie biblioteka
        // logowania, tylko wspólny interfejs `Logger` nad Logback, Log4j2, java.util.logging. Twój kod pisze
        // `LoggerFactory.getLogger(Klasa.class).info("...")`, a konkretną implementację wybiera się w zależnościach
        // programu. Dlaczego to dobre: biblioteka, którą ktoś dołącza do projektu, nie narzuca mu systemu logowania.
        // (Jak stoi w nazwie — „facade”: jedna prosta fasada nad różnymi wnętrzami.)
        note("SLF4J = fasada logowania; konkretny silnik (Logback, Log4j2...) wybiera się osobno");
        // WYNIK: ℹ SLF4J = fasada logowania; konkretny silnik (Logback, Log4j2...) wybiera się osobno
    }

    // =================================================================================================
    // 7. FASADY W SPRINGU
    // =================================================================================================

    /**
     * 7. Spring: warstwa serwisów to fasada nad repozytoriami i innymi komponentami. Kontroler woła jeden serwis
     * z metodą „zrób przypadek użycia”, a serwis koordynuje wiele repozytoriów.
     */
    static void springFacades() {
        section("7. Fasady w Springu — warstwa serwisów");

        // Typowy układ (SpringLearning, t34 Spring02Layers):
        //   @RestController OrderController  -->  @Service OrderService  -->  @Repository OrderRepository
        //                                                                 -->  @Repository StockRepository
        //                                                                 -->  PaymentClient, MailClient
        //   Kontroler zna tylko OrderService. OrderService.placeOrder(...) to nasz OrderFacade: sekwencja kroków,
        //   obsługa błędów, granica transakcji (adnotacja @Transactional na metodzie serwisu obejmuje wszystkie
        //   wywołania repozytoriów w jednej transakcji bazy danych).
        // Inne „fasady” Springa: JdbcTemplate (ukrywa połączenia, wyjątki i zamykanie zasobów JDBC — t29),
        //   RestTemplate/RestClient (ukrywa protokół HTTP), Spring Data (JpaRepository ukrywa EntityManager).
        // PUŁAPKA: „serwis-bóg” — jedna klasa @Service z 40 metodami i 15 zależnościami. To objaw, że fasada
        //   urosła ponad jeden obszar. Dziel serwisy według przypadków użycia (OrderService, ReturnService).
        // Transakcja bazy danych (@Transactional) rozwiązuje wycofanie dla BAZY; nie wycofa wysłanego maila ani
        // obciążonej karty — dlatego sekwencję i kompensację (jak w naszej fasadzie) nadal projektuje się świadomie.
        note("Spring: @Service + @Transactional = fasada z granicą transakcji");
        // WYNIK: ℹ Spring: @Service + @Transactional = fasada z granicą transakcji
    }

    // =================================================================================================
    // 8. TESTOWANIE
    // =================================================================================================

    /**
     * 8. Testowanie: podsystemy to interfejsy, więc fasadę testujemy z atrapami — odrzucającą płatności,
     * zliczającą wywołania, bez prawdziwego magazynu i poczty.
     */
    static void testability() {
        section("8. Testowanie — fasada z atrapami podsystemów");

        List<String> trace = new ArrayList<>();
        InMemoryInventory inventory = new InMemoryInventory(trace);
        Payments declining = (customer, amount) -> { // declining = odrzucająca (zawsze)
            throw new IllegalStateException("odrzucone");
        };
        OrderFacade facade = new OrderFacade(inventory, declining, new SequentialInvoices(trace),
                new CourierShipping(trace), new TraceNotifier(trace));
        Order order = SampleData.orders().get(2); // ZAM-003: KSI-001, KSI-002, KSI-003 po 1 sztuce

        int stockBefore = inventory.available("KSI-001");
        Check.throwsException("płatność odrzucona → wyjątek", IllegalStateException.class, () -> facade.placeOrder(order));
        Check.equal("towar wrócił do magazynu", stockBefore, () -> inventory.available("KSI-001"));
        Check.equal("nie wystawiono faktury", false, () -> trace.stream().anyMatch(line -> line.startsWith("faktura:")));
        Check.equal("nie zaplanowano wysyłki", false, () -> trace.stream().anyMatch(line -> line.startsWith("wysyłka:")));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD

        // Gdyby fasada tworzyła podsystemy sama (`new SmtpMailer()` w środku), tego testu nie dałoby się napisać
        // bez prawdziwego serwera poczty (t22 Patterns08DependencyInjection). Atrapa-lambda `(customer, amount) -> ...`
        // pasuje, bo Payments ma jedną metodę (interfejs funkcyjny, t13).
        // Test samej fasady sprawdza SEKWENCJĘ i błędy; reguły (limit płatności, stan magazynu) testuje się
        // osobno, w testach podsystemów.

        // KIEDY UŻYWAĆ / KIEDY NIE:
        //   | używaj, gdy                                          | nie używaj, gdy                                   |
        //   | proces dotyka kilku klas i ma ustaloną kolejność     | to jedno wywołanie jednej klasy                   |
        //   | wielu klientów powtarza ten sam kod koordynujący     | chcesz ukryć nieudolny projekt zamiast go naprawić |
        //   | chcesz oddzielić warstwy (UI od logiki)              | fasada miałaby zawierać reguły biznesowe          |
        //   | biblioteka ma „nieznośnie” rozbudowane API           | klienci i tak będą potrzebować pełnej kontroli    |
        // YAGNI: fasada „na zapas”, która tylko przekazuje jedno wywołanie dalej, to pusta warstwa — usuń ją.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Fasada = prosty interfejs (kilka metod przypadków użycia) przed grupą klas; koordynuje kroki i błędy.
     *   • Klient zna jedną klasę zamiast wielu i nie musi pamiętać kolejności ani wycofania.
     *   • Fasada nie zawiera reguł biznesowych — te zostają w podsystemach; inaczej powstaje klasa-bóg.
     *   • Nie zabrania dostępu do podsystemów (można używać wprost), ale można wymusić widocznością pakietu/modułu.
     *   • Sygnatury fasady: typy domeny/DTO, nie typy podsystemów; błędy przepuszczaj dalej po wycofaniu zasobów.
     *   • Fasada ≠ Adapter (zmienia jeden interfejs) ≠ Decorator (ten sam interfejs + dodatek) ≠ Mediator.
     *   • JDK: Files (fasada nad strumieniami), Executors, HttpClient. SLF4J = fasada nad systemami logowania.
     *   • Spring: warstwa @Service jako fasada nad repozytoriami; @Transactional ustawia granicę transakcji.
     *   • Zależności wstrzykuj interfejsami — fasada staje się łatwa do testowania z atrapami.
     *
     * PYTANIA KONTROLNE:
     *   1. Jaki problem rozwiązuje fasada w naszym procesie zamówień? Podaj dwa konkretne.
     *   2. Czym fasada różni się od adaptera?
     *   3. Dlaczego fasada zwraca Confirmation, a nie wyniki podsystemów?
     *   4. Magazyn ma 7 laptopów (ELE-001). Ile ich zostanie po odrzuconej płatności za ZAM-001, jeśli zamówienie
     *        idzie przez fasadę, a ile — jeśli przez batchCheckout (bez wycofania)?
     *   5. ZNAJDŹ BŁĄD:  try { payments.charge(...); } catch (Exception e) { return null; }  w metodzie placeOrder fasady.
     *   6. Co wypisze:  show("kolejność", List.of("rezerwacja", "płatność"))  — a co się zmieni, jeśli zamienimy
     *        te kroki miejscami w fasadzie? Jakie ryzyko to niesie?
     *   7. Jak odróżnić fasadę od klasy-boga? Podaj dwie cechy.
     *   8. Co to jest SLF4J i dlaczego nazywa się „facade”?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: potwierdzenie", "ZAM-003|FV/2026/001|TRK-ZAM-003", () -> exercise1());
        Check.equal("ćw. 2: złożenie i anulowanie", "25|5", () -> exercise2());
        Check.equal("ćw. 3: skrót kolejności", List.of("magazyn", "płatność", "faktura", "wysyłka"), () -> exercise3());
        Check.equal("ćw. 4: fasada z odrzuconą płatnością", "odrzucone|7", () -> exercise4());
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "ZAM-003|FV/2026/001|TRK-ZAM-003", () -> solution1());
        Check.equal("ćw. 2 (wzorzec)", "25|5", () -> solution2());
        Check.equal("ćw. 3 (wzorzec)", List.of("magazyn", "płatność", "faktura", "wysyłka"), () -> solution3());
        Check.equal("ćw. 4 (wzorzec)", "odrzucone|7", () -> solution4());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): Zbuduj fasadę przez {@code standardFacade(new InMemoryInventory(trace), trace)},
     * złóż zamówienie ZAM-003 (indeks 2 w {@code SampleData.orders()}) i zwróć tekst
     * {@code orderId + "|" + invoiceNumber + "|" + trackingNumber}.
     * Podpowiedź: {@code Confirmation} to record, więc ma metody {@code orderId()}, {@code invoiceNumber()}, {@code trackingNumber()}.
     */
    static String exercise1() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): Zbuduj fasadę (jak w ćwiczeniu 1; zapamiętaj magazyn w zmiennej), złóż zamówienie
     * ZAM-009 (indeks 8: jedna para Słuchawek BT, klientka bez e-maila), a potem anuluj je metodą
     * {@code cancelOrder}. Zwróć tekst {@code dostępne_ELE_003 + "|" + liczba_wpisów_w_śladzie}.
     * Podpowiedź: po anulowaniu magazyn ma znowu tyle sztuk, co na początku (25); w śladzie jest po jednym
     * wpisie na krok: rezerwacja, płatność, faktura, wysyłka, zwolnienie (bez powiadomienia — brak e-maila).
     */
    static String exercise2() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie, PRZEPISZ): Poniżej proces zapisany po staremu (klient zna cztery podsystemy). Przepisz
     * go tak, aby klient wołał tylko fasadę, a potem ze śladu zwróć listę prefiksów kroków (tekst przed pierwszym
     * dwukropkiem) — bez powtórzeń, w kolejności pierwszego wystąpienia, pomijając powiadomienie.
     * <pre>{@code
     * // PRZED:
     * inventory.reserve(...); payments.charge(...); invoices.issue(...); shipping.schedule(...);
     * }</pre>
     * Podpowiedź: złóż ZAM-002 (indeks 1, klientka bez e-maila — więc powiadomienia nie będzie); prefiks
     * wpisu {@code "magazyn: rezerwacja ..."} to {@code "magazyn"}; użyj {@code distinct()} na strumieniu.
     */
    static List<String> exercise3() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): Zbuduj {@code OrderFacade} z atrapą płatności, która zawsze rzuca
     * {@code IllegalStateException("odrzucone")} (pozostałe podsystemy jak w {@code standardFacade}). Złóż ZAM-001
     * (indeks 0), złap wyjątek i zwróć tekst {@code komunikat + "|" + dostępne_ELE_001}.
     * Podpowiedź: magazyn ma na początku 7 laptopów (ELE-001); fasada zwalnia rezerwację po nieudanej płatności,
     * więc po próbie nadal powinno być 7. Atrapa-lambda: {@code (customer, amount) -> { throw ...; }}.
     */
    static String exercise4() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1() {
        List<String> trace = new ArrayList<>();
        OrderFacade facade = standardFacade(new InMemoryInventory(trace), trace);
        Confirmation confirmation = facade.placeOrder(SampleData.orders().get(2));
        return confirmation.orderId() + "|" + confirmation.invoiceNumber() + "|" + confirmation.trackingNumber();
    }

    static String solution2() {
        List<String> trace = new ArrayList<>();
        InMemoryInventory inventory = new InMemoryInventory(trace);
        OrderFacade facade = standardFacade(inventory, trace);
        Order order = SampleData.orders().get(8);
        facade.placeOrder(order);
        facade.cancelOrder(order);
        return inventory.available("ELE-003") + "|" + trace.size();
    }

    static List<String> solution3() {
        List<String> trace = new ArrayList<>();
        OrderFacade facade = standardFacade(new InMemoryInventory(trace), trace);
        facade.placeOrder(SampleData.orders().get(1));
        return trace.stream()
                .filter(entry -> !entry.startsWith("powiadomienie"))
                .map(entry -> entry.substring(0, entry.indexOf(':')))
                .distinct()
                .toList();
    }

    static String solution4() {
        List<String> trace = new ArrayList<>();
        InMemoryInventory inventory = new InMemoryInventory(trace);
        Payments declining = (customer, amount) -> {
            throw new IllegalStateException("odrzucone");
        };
        OrderFacade facade = new OrderFacade(inventory, declining, new SequentialInvoices(trace),
                new CourierShipping(trace), new TraceNotifier(trace));
        try {
            facade.placeOrder(SampleData.orders().get(0));
            return "brak błędu";
        } catch (IllegalStateException ex) {
            return ex.getMessage() + "|" + inventory.available("ELE-001");
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Klient nie musi znać pięciu podsystemów ani ich kolejności; jeden kod procesu zamiast kopii w wielu
     *      miejscach (mniej pominiętych kroków); wycofanie (zwolnienie towaru po odrzuconej płatności) w jednym
     *      miejscu.
     *   2. Adapter dopasowuje JEDEN interfejs do oczekiwanego; fasada tworzy NOWY, prostszy interfejs do wielu klas.
     *   3. Żeby klient nie zależał od typów podsystemów — inaczej fasada niczego by nie ukrywała.
     *   4. Przez fasadę 7 (zwalnia rezerwację po odrzuconej płatności), przez batchCheckout 6 (rezerwacja zostaje).
     *   5. Połknięty wyjątek: klient dostaje null i nie wie, że zamówienie nie przeszło; towar mógł zostać
     *      zarezerwowany. Trzeba wycofać zasoby i rzucić wyjątek dalej.
     *   6. Wypisze listę [rezerwacja, płatność]. Zamiana kroków: płatność przed rezerwacją pobrałaby pieniądze,
     *      zanim wiadomo, czy towar jest — przy braku towaru trzeba by zwracać pieniądze.
     *   7. Fasada koordynuje i deleguje, jest cienka, reguły siedzą w podsystemach, a klasa-bóg robi wszystko sama,
     *      jest ogromna i zmienia się z każdym wymaganiem.
     *   8. Simple Logging Facade for Java — wspólny interfejs Logger nad różnymi bibliotekami logowania
     *      (Logback, Log4j2...); wybór implementacji jest poza kodem aplikacji.
     */
    // </editor-fold>
}
