package t22_design_patterns;

import helpers.Check;
import helpers.model.OrderStatus;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Observer (obserwator) — „daj znać wszystkim zainteresowanym”
 *        (observer = obserwator; subject = podmiot / źródło zdarzeń; listener = słuchacz;
 *         event = zdarzenie; publish = opublikuj; subscribe = zapisz się na powiadomienia)
 *
 * W SKRÓCIE:
 *   Obiekt, w którym coś się zmienia (podmiot), nie wie, KTO chce o tym usłyszeć. Trzyma tylko listę
 *   słuchaczy i każdemu woła tę samą metodę. Dzięki temu dopisanie nowej reakcji (audyt, SMS, magazyn)
 *   to nowa klasa albo lambda — bez ruszania kodu podmiotu.
 *
 * ANALOGIA: subskrypcja newslettera sklepu. Sklep nie wysyła listów do konkretnych osób wpisanych
 *   w kodzie programu — ma listę adresów. Chcesz dostawać wiadomości? Zapisujesz się. Masz dosyć?
 *   Wypisujesz się. Sklep nie zmienia się ani o linijkę, a liczba czytelników może rosnąć i maleć.
 *   Jeśli zapomnisz się wypisać, wiadomości przychodzą dalej (to jest wyciek słuchaczy).
 *
 * JAK TO DZIAŁA:
 *   Role z książki „Design Patterns” (GoF):
 *     Subject (podmiot)    — trzyma stan i listę słuchaczy; metody attach/detach (dołącz/odłącz) i notify (powiadom)
 *     Observer (obserwator) — interfejs z jedną metodą „coś się zmieniło” (u nas: Consumer lub OrderListener)
 *     ConcreteObserver     — konkretna reakcja: e-mail, SMS, rezerwacja w magazynie
 *
 *      zmiana statusu zamówienia
 *               |
 *        +--------------+   notify   +-------------------+
 *        | OrderSubject |----------->| EmailObserver     |
 *        |  - listeners |----------->| SmsObserver       |   kolejność = kolejność zapisów
 *        +--------------+----------->| StockObserver     |   (lista, więc deterministyczna)
 *                                    +-------------------+
 *
 *   Dwa style przekazywania danych: PUSH (podmiot przesyła zdarzenie z danymi — nasz wybór, prostszy
 *   i bezpieczniejszy) oraz PULL (podmiot mówi „coś się zmieniło”, a słuchacz sam dopytuje o szczegóły).
 *   Zdarzenie najlepiej zapisać jako niezmienny record.
 *
 *   Decyzje, które zawsze trzeba podjąć (to tu kryją się błędy):
 *     1. kolejność powiadomień,           2. co, gdy słuchacz rzuci wyjątek,
 *     3. co, gdy słuchacz odpisze się w trakcie powiadamiania,  4. kto i kiedy usuwa słuchaczy,
 *     5. czy powiadamiamy synchronicznie (w tym samym wątku) czy asynchronicznie.
 *
 * SŁÓWKA:
 *   observer = obserwator; subject = podmiot; listener = słuchacz; event = zdarzenie;
 *   publisher = wydawca (ten, kto publikuje); subscriber = subskrybent; subscribe = zapisz się;
 *   unsubscribe = wypisz się; notify = powiadom; leak = wyciek; broker = pośrednik (np. kolejka wiadomości).
 *
 * ZOBACZ TEŻ: t13_lambdas/Lambda02FunctionalInterfaces (Consumer jako słuchacz),
 *   t12_collections/Collections03IterationModification (ConcurrentModificationException),
 *   t21_concurrency/Concurrency04Executors (powiadamianie asynchroniczne),
 *   t22_design_patterns/Patterns09Command (polecenie zamiast zdarzenia),
 *   t27_clean_code_pitfalls/CleanCode02Solid (zasada otwarte–zamknięte),
 *   t34_toward_spring/Spring04WhatSpringGives (ApplicationEventPublisher)
 * </pre>
 */
public class Patterns06Observer {

    public static void main(String[] args) {
        title("Patterns06 — Observer (obserwator)");

        problem();                  // problem = problem (kod bez wzorca)
        classicObserver();          // classic = klasyczny
        modernObserver();           // modern = nowoczesny
        unsubscribing();            // unsubscribing = wypisywanie się
        failingListener();          // failing listener = słuchacz, który zawodzi
        modifyDuringNotify();       // modify during notify = zmiana listy w trakcie powiadamiania
        listenerLeak();             // listener leak = wyciek słuchaczy
        jdkObservers();             // jdk observers = obserwatory w JDK
        eventBusAndSpring();        // event bus = szyna zdarzeń; Spring
        testability();              // testability = łatwość testowania
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // Wspólne typy lekcji (lekcje nie importują się nawzajem, więc wszystko jest tutaj)
    // =================================================================================================

    /** Zdarzenie: zamówienie zmieniło status. Record = niezmienny nośnik danych (t05). */
    record OrderEvent(String orderId, OrderStatus from, OrderStatus to) { }

    /** Klasyczny interfejs obserwatora (observer). on...Changed = „przy zmianie...”. */
    interface OrderListener {
        void onOrderChanged(OrderEvent event);
    }

    /** Uchwyt zapisu: close() = wypisz się. AutoCloseable pozwala użyć try-with-resources. */
    interface Subscription extends AutoCloseable {
        @Override
        void close(); // zawężamy: bez throws Exception, żeby nie wymuszać try/catch
    }

    /**
     * Uniwersalny, typowany wydawca zdarzeń (publisher). Decyzje projektowe:
     * kolejność = kolejność zapisów (ArrayList), wyjątek słuchacza nie zatrzymuje pozostałych
     * (trafia do errorHandler), a powiadamiamy po KOPII listy (migawce), więc słuchacz może się
     * bezpiecznie wypisać w trakcie powiadamiania.
     */
    static final class Publisher<E> {
        private final List<Consumer<? super E>> listeners = new ArrayList<>(); // listeners = słuchacze
        private final Consumer<Throwable> errorHandler; // errorHandler = obsługa błędów

        Publisher(Consumer<Throwable> errorHandler) {
            this.errorHandler = Objects.requireNonNull(errorHandler);
        }

        /** subscribe = zapisz się; zwraca uchwyt do wypisania się. */
        Subscription subscribe(Consumer<? super E> listener) {
            Objects.requireNonNull(listener);
            listeners.add(listener);
            return () -> listeners.remove(listener);
        }

        /** unsubscribe po samej referencji — wygodne, ale zob. sekcję 4 (łatwo się pomylić). */
        boolean unsubscribe(Consumer<? super E> listener) {
            return listeners.remove(listener);
        }

        /** subscribeOnce = zapisz się na jedno zdarzenie: po pierwszym powiadomieniu sam się wypisuje. */
        Subscription subscribeOnce(Consumer<? super E> listener) {
            Subscription[] holder = new Subscription[1]; // tablica, bo lambda nie widzi zmiennej przypisanej później
            holder[0] = subscribe(event -> {
                holder[0].close();
                listener.accept(event);
            });
            return holder[0];
        }

        /** publish = opublikuj: powiadom wszystkich zapisanych (po migawce listy). */
        void publish(E event) {
            for (Consumer<? super E> listener : List.copyOf(listeners)) { // copyOf = kopia niezmienna (Java 10+)
                try {
                    listener.accept(event);
                } catch (RuntimeException ex) {
                    errorHandler.accept(ex);
                }
            }
        }

        int size() {
            return listeners.size();
        }
    }

    // =================================================================================================
    // 1. PROBLEM — wszystko „na sztywno” w jednej klasie
    // =================================================================================================

    /** Wersja BEZ wzorca: serwis zna wszystkich, którzy chcą wiedzieć o zmianie statusu. */
    static final class BadOrderService {
        private final List<String> log;

        BadOrderService(List<String> log) {
            this.log = log;
        }

        void changeStatus(String orderId, OrderStatus newStatus) { // changeStatus = zmień status
            log.add("e-mail: zamówienie " + orderId + " ma status " + newStatus);
            if (newStatus == OrderStatus.WYSLANE) {
                log.add("SMS: paczka " + orderId + " jest w drodze");
            }
            if (newStatus == OrderStatus.ANULOWANE) {
                log.add("magazyn: zwolnij towar z " + orderId);
            }
            // NOWE WYMAGANIE: „dopisz wpis do dziennika audytu”, „powiadom program lojalnościowy”...
            // → znowu edytujemy TĘ SAMĄ metodę (łamiemy zasadę otwarte–zamknięte, t27 CleanCode02Solid),
            //   a test serwisu musi znać e-mail, SMS i magazyn naraz.
        }
    }

    /**
     * 1. Problem: serwis, który sam woła e-mail, SMS i magazyn. Każda nowa reakcja = edycja serwisu,
     * rosnące if-y i sprzężenie (serwis zależy od wszystkich odbiorców).
     */
    static void problem() {
        section("1. Problem — serwis zna wszystkich odbiorców");

        List<String> log = new ArrayList<>();
        BadOrderService service = new BadOrderService(log);
        service.changeStatus("ZAM-001", OrderStatus.WYSLANE);
        service.changeStatus("ZAM-002", OrderStatus.ANULOWANE);
        for (String entry : log) {
            note(entry);
        }
        // WYNIK: ℹ e-mail: zamówienie ZAM-001 ma status WYSLANE
        // WYNIK: ℹ SMS: paczka ZAM-001 jest w drodze
        // WYNIK: ℹ e-mail: zamówienie ZAM-002 ma status ANULOWANE
        // WYNIK: ℹ magazyn: zwolnij towar z ZAM-002

        // PUŁAPKA: „kolejny odbiorca = kolejna linijka w serwisie”. Po roku metoda ma 15 if-ów i nikt nie wie,
        //   która reakcja jest ważna, a która dodana „na chwilę”. Dlaczego to boli: zmiana w jednym miejscu
        //   (audyt) może zepsuć drugie (SMS), bo wszystko siedzi w jednej metodzie.
    }

    // =================================================================================================
    // 2. KLASYCZNY OBSERVER
    // =================================================================================================

    /** Konkretny obserwator 1: wysyła e-mail (u nas: zapisuje linię do dziennika). */
    static final class EmailObserver implements OrderListener {
        private final List<String> log;

        EmailObserver(List<String> log) {
            this.log = log;
        }

        @Override
        public void onOrderChanged(OrderEvent event) {
            log.add("e-mail: " + event.orderId() + " " + event.from() + " → " + event.to());
        }
    }

    /** Konkretny obserwator 2: SMS tylko, gdy paczka wyszła ze sklepu. */
    static final class SmsObserver implements OrderListener {
        private final List<String> log;

        SmsObserver(List<String> log) {
            this.log = log;
        }

        @Override
        public void onOrderChanged(OrderEvent event) {
            if (event.to() == OrderStatus.WYSLANE) {
                log.add("SMS: paczka " + event.orderId() + " w drodze");
            }
        }
    }

    /** Konkretny obserwator 3: magazyn zwalnia towar po anulowaniu. */
    static final class StockObserver implements OrderListener {
        private final List<String> log;

        StockObserver(List<String> log) {
            this.log = log;
        }

        @Override
        public void onOrderChanged(OrderEvent event) {
            if (event.to() == OrderStatus.ANULOWANE) {
                log.add("magazyn: zwolnij towar z " + event.orderId());
            }
        }
    }

    /** Podmiot (subject) z GoF: pamięta bieżący status i listę obserwatorów. */
    static final class OrderSubject {
        private final String orderId;
        private OrderStatus status = OrderStatus.NOWE; // status = stan zamówienia
        private final List<OrderListener> observers = new ArrayList<>();

        OrderSubject(String orderId) {
            this.orderId = orderId;
        }

        void attach(OrderListener observer) { // attach = dołącz
            observers.add(observer);
        }

        void detach(OrderListener observer) { // detach = odłącz
            observers.remove(observer);
        }

        void changeStatus(OrderStatus newStatus) {
            OrderEvent event = new OrderEvent(orderId, status, newStatus);
            status = newStatus;
            for (OrderListener observer : observers) { // notify = powiadom (kolejność dołączenia)
                observer.onOrderChanged(event);
            }
        }
    }

    /**
     * 2. Klasyczny Observer: interfejs + podmiot + konkretni obserwatorzy. Podmiot zna tylko
     * interfejs OrderListener — nie zna e-maila, SMS-a ani magazynu.
     */
    static void classicObserver() {
        section("2. Klasyczny Observer — interfejs, podmiot, obserwatorzy");

        List<String> log = new ArrayList<>();
        OrderSubject order = new OrderSubject("ZAM-003");
        order.attach(new EmailObserver(log));
        order.attach(new SmsObserver(log));
        order.attach(new StockObserver(log));

        order.changeStatus(OrderStatus.OPLACONE);
        order.changeStatus(OrderStatus.WYSLANE);
        order.changeStatus(OrderStatus.ANULOWANE);
        for (String entry : log) {
            note(entry);
        }
        // WYNIK: ℹ e-mail: ZAM-003 NOWE → OPLACONE
        // WYNIK: ℹ e-mail: ZAM-003 OPLACONE → WYSLANE
        // WYNIK: ℹ SMS: paczka ZAM-003 w drodze
        // WYNIK: ℹ e-mail: ZAM-003 WYSLANE → ANULOWANE
        // WYNIK: ℹ magazyn: zwolnij towar z ZAM-003

        // Nowa reakcja (audyt) to nowa klasa + jedno attach w miejscu składania programu. Podmiot bez zmian.
        // Kolejność wpisów w dzienniku = kolejność attach, bo trzymamy ArrayList. DOBRA PRAKTYKA: zapisz tę
        //   kolejność w dokumentacji klasy, ale nie buduj na niej logiki — słuchacze powinni być od siebie niezależni.
        // Gdyby zamiast listy użyć HashSet, kolejność byłaby przypadkowa i testy migałyby (t12 Collections04Sets).
    }

    // =================================================================================================
    // 3. NOWOCZESNY OBSERVER — Consumer i lambdy
    // =================================================================================================

    /**
     * 3. Interfejs z jedną metodą to interfejs funkcyjny (t13), więc słuchacz może być lambdą albo
     * referencją do metody. Nie trzeba pisać klasy na każdą drobną reakcję.
     */
    static void modernObserver() {
        section("3. Nowocześnie — słuchacz jako lambda (Consumer)");

        List<String> log = new ArrayList<>();
        Publisher<OrderEvent> publisher = new Publisher<>(error -> log.add("BŁĄD: " + error.getMessage()));

        publisher.subscribe(event -> log.add("e-mail: " + event.orderId() + " → " + event.to()));
        publisher.subscribe(event -> {
            if (event.to() == OrderStatus.WYSLANE) {
                log.add("SMS: " + event.orderId());
            }
        });
        publisher.subscribe(event -> log.add("audyt: " + event)); // zwykła lambda

        publisher.publish(new OrderEvent("ZAM-004", OrderStatus.NOWE, OrderStatus.WYSLANE));
        for (String entry : log) {
            note(entry);
        }
        // WYNIK: ℹ e-mail: ZAM-004 → WYSLANE
        // WYNIK: ℹ SMS: ZAM-004
        // WYNIK: ℹ audyt: OrderEvent[orderId=ZAM-004, from=NOWE, to=WYSLANE]

        // Referencja do metody jako słuchacz: Consumer<OrderEvent> może być też `this::handle` albo `audit::record`
        // (t13 Lambda04MethodReferences). Zdarzenie jako record daje za darmo czytelny toString.

        // Kiedy klasa, a kiedy lambda? Lambda — krótka, bezstanowa reakcja. Klasa — gdy słuchacz ma własny stan,
        // zależności (np. klienta SMS), nazwę wartą testowania osobno albo ma być wypisywany po instancji.
        // DOBRA PRAKTYKA: słuchacz robi MAŁO i szybko; ciężką pracę (wysyłka maili) oddaj do kolejki lub
        //   executora (t21 Concurrency04Executors), żeby jeden wolny słuchacz nie blokował całego powiadamiania.
    }

    // =================================================================================================
    // 4. WYPISYWANIE SIĘ
    // =================================================================================================

    /**
     * 4. Wypisanie się: najbezpieczniej przez uchwyt (Subscription) zwrócony przy zapisie. Wypisanie
     * „po lambdzie” zawodzi, bo drugi zapis tej samej lambdy to INNY obiekt.
     */
    static void unsubscribing() {
        section("4. Wypisywanie się — uchwyt zamiast „tej samej” lambdy");

        List<String> log = new ArrayList<>();
        Publisher<String> publisher = new Publisher<>(error -> log.add("BŁĄD"));

        Subscription subscription = publisher.subscribe(text -> log.add("A dostał " + text));
        publisher.publish("pierwsze");
        subscription.close(); // close = zamknij (wypisz się)
        publisher.publish("drugie");
        show("po wypisaniu się", log);
        // WYNIK: po wypisaniu się → [A dostał pierwsze]

        // try-with-resources (t10): zapis trwa tylko w bloku, a wypisanie dzieje się automatycznie,
        // także gdy w bloku poleci wyjątek.
        try (Subscription temporary = publisher.subscribe(text -> log.add("B dostał " + text))) {
            publisher.publish("trzecie");
        }
        publisher.publish("czwarte");
        show("po bloku try", log);
        // WYNIK: po bloku try → [A dostał pierwsze, B dostał trzecie]

        // PUŁAPKA: próba wypisania „tą samą” lambdą. Każde wystąpienie zapisu `x -> ...` w kodzie tworzy
        //   osobny obiekt, a remove szuka po equals (dla lambd to tożsamość), więc nic nie znajduje.
        //   Dlaczego groźne: metoda zwraca false, nikt nie rzuca błędu, a słuchacz dalej dostaje zdarzenia.
        publisher.subscribe(text -> log.add("C dostał " + text));
        boolean removed = publisher.unsubscribe(text -> log.add("C dostał " + text));
        show("czy usunięto „taką samą” lambdę", removed);
        // WYNIK: czy usunięto „taką samą” lambdę → false
        show("słuchaczy nadal", publisher.size());
        // WYNIK: słuchaczy nadal → 1

        // Referencje do metod (`this::handle`) są jeszcze podstępniejsze: dwa zapisy tego samego wyrażenia
        // mogą, ale nie muszą dać ten sam obiekt — specyfikacja języka niczego tu nie gwarantuje.
        // DOBRA PRAKTYKA: trzymaj referencję do słuchacza w zmiennej albo (lepiej) używaj uchwytu Subscription.
    }

    // =================================================================================================
    // 5. SŁUCHACZ, KTÓRY ZAWODZI
    // =================================================================================================

    /**
     * 5. Wyjątek w jednym słuchaczu. Naiwna pętla przerywa powiadamianie — kolejni słuchacze (u nas: magazyn)
     * nic się nie dowiedzą. Nasza decyzja: izolujemy słuchaczy, błąd trafia do errorHandler.
     */
    static void failingListener() {
        section("5. Wyjątek w słuchaczu — czy reszta ma się dowiedzieć?");

        List<String> log = new ArrayList<>();
        List<Consumer<String>> naive = new ArrayList<>(); // naive = naiwny
        naive.add(text -> log.add("e-mail: " + text));
        naive.add(text -> {
            throw new IllegalStateException("brama SMS niedostępna");
        });
        naive.add(text -> log.add("magazyn: " + text));

        expectThrows("naiwna pętla", () -> {
            for (Consumer<String> listener : naive) {
                listener.accept("ZAM-005 anulowane");
            }
        });
        // WYNIK: ✔ naiwna pętla → rzucono IllegalStateException: brama SMS niedostępna
        show("co się wykonało", log);
        // WYNIK: co się wykonało → [e-mail: ZAM-005 anulowane]
        // PUŁAPKA: awaria SMS-a sprawiła, że magazyn nigdy nie zwolnił towaru — dane niespójne, a winny
        //   (słuchacz SMS) jest „daleko” od skutku. Dlaczego: pętla for przerywa się na pierwszym wyjątku.

        log.clear();
        Publisher<String> safe = new Publisher<>(error -> log.add("błąd słuchacza: " + error.getMessage()));
        safe.subscribe(text -> log.add("e-mail: " + text));
        safe.subscribe(text -> {
            throw new IllegalStateException("brama SMS niedostępna");
        });
        safe.subscribe(text -> log.add("magazyn: " + text));
        safe.publish("ZAM-005 anulowane");
        show("z izolacją", log);
        // WYNIK: z izolacją → [e-mail: ZAM-005 anulowane, błąd słuchacza: brama SMS niedostępna, magazyn: ZAM-005 anulowane]

        // Decyzja projektowa (są dwie szkoły):
        //   • izolacja — jak wyżej: awaria jednego słuchacza nie psuje pozostałych (typowe dla zdarzeń „informujących”);
        //   • przerwanie — gdy słuchacz to WALIDATOR i jego sprzeciw ma zatrzymać operację (wtedy to nie jest
        //     czysty Observer, tylko łańcuch odpowiedzialności — t22 Patterns14ChainOfResponsibility).
        // DOBRA PRAKTYKA: złap RuntimeException (nie Throwable/Error — błędów typu OutOfMemoryError nie połykamy),
        //   zawsze zaloguj błąd i nigdy nie ukrywaj go po cichu.
    }

    // =================================================================================================
    // 6. ZMIANA LISTY W TRAKCIE POWIADAMIANIA
    // =================================================================================================

    /**
     * 6. Słuchacz „jednorazowy” wypisuje się w trakcie powiadamiania. Pętla po żywej liście rzuca
     * ConcurrentModificationException; pętla po kopii (migawce) działa poprawnie.
     */
    static void modifyDuringNotify() {
        section("6. Wypisanie w trakcie powiadamiania — iteracja po migawce");

        List<Runnable> live = new ArrayList<>(); // live = „żywa” lista
        List<String> log = new ArrayList<>();
        Runnable[] selfRemoving = new Runnable[1];
        selfRemoving[0] = () -> {
            log.add("jednorazowy");
            live.remove(selfRemoving[0]); // usuwa sam siebie w środku iteracji
        };
        live.add(selfRemoving[0]);
        live.add(() -> log.add("stały 1"));
        live.add(() -> log.add("stały 2"));

        expectThrows("pętla po żywej liście", () -> {
            for (Runnable listener : live) {
                listener.run();
            }
        });
        // WYNIK: ✔ pętla po żywej liście → rzucono ConcurrentModificationException: (brak komunikatu)
        show("zdążył się wykonać", log);
        // WYNIK: zdążył się wykonać → [jednorazowy]
        // PUŁAPKA: usunięcie elementu w trakcie for-each psuje licznik modyfikacji listy (modCount), więc
        //   następny krok iteratora rzuca ConcurrentModificationException (t12 Collections03IterationModification).
        //   Co gorsza „stały 1” i „stały 2” nie zostały powiadomione — przez błąd cudzego słuchacza.

        Publisher<String> publisher = new Publisher<>(error -> log.add("BŁĄD"));
        log.clear();
        publisher.subscribeOnce(text -> log.add("jednorazowy dostał " + text));
        publisher.subscribe(text -> log.add("stały dostał " + text));
        publisher.publish("pierwsze");
        publisher.publish("drugie");
        show("z migawką", log);
        // WYNIK: z migawką → [jednorazowy dostał pierwsze, stały dostał pierwsze, stały dostał drugie]
        show("słuchaczy po wszystkim", publisher.size());
        // WYNIK: słuchaczy po wszystkim → 1

        // Migawka ma swoją cenę: słuchacz wypisany w trakcie rundy MOŻE jeszcze dostać bieżące zdarzenie,
        // jeśli był już w kopii. Dla większości zastosowań to akceptowalne — ale warto o tym wiedzieć.
        // W kodzie wielowątkowym zamiast ArrayList użyj CopyOnWriteArrayList (t21 Concurrency06ConcurrentCollections):
        // jego iterator zawsze pracuje na migawce, więc nie ma ConcurrentModificationException.
    }

    // =================================================================================================
    // 7. WYCIEK SŁUCHACZY
    // =================================================================================================

    /** Okno/widok, który zapisuje się na zdarzenia w konstruktorze. */
    static final class StatusScreen {
        private final List<String> shown = new ArrayList<>(); // shown = wyświetlone
        private final Subscription subscription;

        StatusScreen(Publisher<OrderEvent> publisher) {
            this.subscription = publisher.subscribe(event -> shown.add(event.orderId() + ":" + event.to()));
        }

        /** Poprawne zamknięcie widoku: wypisuje się ze źródła zdarzeń. */
        void close() {
            subscription.close();
        }

        List<String> shown() {
            return shown;
        }
    }

    /**
     * 7. Wyciek słuchaczy (listener leak): „zamknięty” widok, który zapomniał się wypisać, jest nadal
     * trzymany przez wydawcę — nie zbierze go odśmiecacz (garbage collector) i dalej reaguje na zdarzenia.
     */
    static void listenerLeak() {
        section("7. Wyciek słuchaczy — zapomniane wypisanie");

        Publisher<OrderEvent> publisher = new Publisher<>(error -> { });
        StatusScreen forgotten = new StatusScreen(publisher);
        StatusScreen tidy = new StatusScreen(publisher); // tidy = porządny
        tidy.close();
        // „forgotten” przestajemy używać, ale nie wołamy close()
        publisher.publish(new OrderEvent("ZAM-006", OrderStatus.NOWE, OrderStatus.OPLACONE));
        show("zapomniany widok dostał", forgotten.shown());
        // WYNIK: zapomniany widok dostał → [ZAM-006:OPLACONE]
        show("zamknięty widok dostał", tidy.shown());
        // WYNIK: zamknięty widok dostał → []
        show("słuchaczy u wydawcy", publisher.size());
        // WYNIK: słuchaczy u wydawcy → 1

        // PUŁAPKA: wydawca trzyma silną referencję do słuchacza (a lambda trzyma `shown`, więc i cały widok).
        //   Jeśli wydawca żyje długo (singleton, serwis), a widoki powstają i giną — lista rośnie, pamięć
        //   się zapełnia, a „martwe” widoki dalej zużywają procesor. Typowy wyciek pamięci w Javie —
        //   nie brak free(), lecz zapomniane referencje.
        // DOBRA PRAKTYKA: kto się zapisał, ten się wypisuje (symetria: subscribe ↔ close, najlepiej
        //   w try-with-resources lub w metodzie close/dispose obiektu). Alternatywa: słabe referencje
        //   (WeakReference, WeakHashMap) — działają, ale zachowanie zależy od odśmiecacza, więc trudniej
        //   je testować; używaj świadomie.
    }

    // =================================================================================================
    // 8. OBSERVER W JDK
    // =================================================================================================

    /** Model z jedną właściwością (property) i standardowym wsparciem dla słuchaczy. */
    static final class OrderModel {
        private final PropertyChangeSupport support = new PropertyChangeSupport(this); // support = wsparcie
        private OrderStatus status = OrderStatus.NOWE;

        void addPropertyChangeListener(PropertyChangeListener listener) {
            support.addPropertyChangeListener(listener);
        }

        void setStatus(OrderStatus newStatus) {
            OrderStatus old = this.status;
            this.status = newStatus;
            support.firePropertyChange("status", old, newStatus); // fire = wystrzel (opublikuj) zmianę właściwości
        }
    }

    /**
     * 8. Obserwatory w JDK. java.beans.PropertyChangeSupport to gotowy podmiot: słuchacz dostaje nazwę
     * właściwości, starą i nową wartość. Stare java.util.Observable / Observer jest przestarzałe (deprecated)
     * od Javy 9 — dlatego tylko o nich opowiadamy, nie używamy.
     */
    static void jdkObservers() {
        section("8. Observer w JDK — PropertyChangeSupport i przestarzałe Observable");

        List<String> log = new ArrayList<>();
        OrderModel model = new OrderModel();
        model.addPropertyChangeListener(event -> log.add(
                event.getPropertyName() + ": " + event.getOldValue() + " → " + event.getNewValue()));
        model.setStatus(OrderStatus.OPLACONE);
        model.setStatus(OrderStatus.OPLACONE); // ta sama wartość
        model.setStatus(OrderStatus.WYSLANE);
        show("zdarzenia", log);
        // WYNIK: zdarzenia → [status: NOWE → OPLACONE, status: OPLACONE → WYSLANE]

        // Fakty o PropertyChangeSupport:
        //   • jeśli stara i nowa wartość są równe (equals) i różne od null, zdarzenie NIE jest wysyłane — stąd tylko dwa wpisy;
        //   • leży w pakiecie java.beans (moduł java.desktop), ale działa w zwykłej aplikacji konsolowej;
        //   • PropertyChangeListener ma jedną metodę, więc pasuje lambda;
        //   • używają go komponenty Swing i JavaFX (własne odmiany) — tam model „nasłuchuje” widoków.
        //
        // Dlaczego java.util.Observable (od Javy 1.0) jest od Javy 9 oznaczone @Deprecated:
        //   • Observable to KLASA — nie możesz po niej dziedziczyć, jeśli już po czymś dziedziczysz
        //     (a dziedziczenie po to, by dostać listę słuchaczy, to zły powód, t06);
        //   • metoda setChanged() jest chroniona, więc nie da się „użyć Observable przez kompozycję”;
        //   • zdarzenie to goły Object — brak typów, trzeba rzutować;
        //   • kolejność powiadamiania jest w dokumentacji NIEOKREŚLONA;
        //   • stan podmiotu nie musi odpowiadać jeden do jednego powiadomieniom (kilka zmian = jedno powiadomienie).
        //   Dokumentacja JDK poleca: java.beans (bogatszy model zdarzeń), java.util.concurrent (niezawodna
        //   komunikacja między wątkami) albo interfejs Flow (Java 9+, tzw. reactive streams).
        // Inne obserwatory, które znasz z JDK: Swing ActionListener (reakcja na kliknięcie),
        //   CompletableFuture.whenComplete (obserwator zakończenia obliczenia — t21).
    }

    // =================================================================================================
    // 9. SZYNA ZDARZEŃ, SPRING, PUB/SUB
    // =================================================================================================

    record OrderPaid(String orderId) { }

    record OrderCancelled(String orderId, String reason) { }

    /**
     * Prosta szyna zdarzeń (event bus): słuchacze zapisują się na TYP zdarzenia. Nadawca nie zna
     * odbiorców, odbiorca nie zna nadawcy — zna tylko typ zdarzenia. To krok w stronę pub/sub.
     */
    static final class EventBus {
        private final Map<Class<?>, List<Consumer<Object>>> handlers = new HashMap<>(); // handlers = obsługa

        <E> void subscribe(Class<E> type, Consumer<? super E> handler) {
            handlers.computeIfAbsent(type, key -> new ArrayList<>())
                    .add(event -> handler.accept(type.cast(event))); // cast = rzutowanie z kontrolą typu
        }

        void publish(Object event) {
            for (Consumer<Object> handler : handlers.getOrDefault(event.getClass(), List.of())) {
                handler.accept(event);
            }
        }
    }

    /**
     * 9. Szyna zdarzeń i Spring. Obserwator „po typie zdarzenia” to w praktyce to, co robi
     * ApplicationEventPublisher w Springu. Pub/sub z brokerem to ten sam pomysł rozciągnięty między procesami.
     */
    static void eventBusAndSpring() {
        section("9. Szyna zdarzeń, Spring i pub/sub");

        List<String> log = new ArrayList<>();
        EventBus bus = new EventBus();
        bus.subscribe(OrderPaid.class, event -> log.add("księgowość: faktura dla " + event.orderId()));
        bus.subscribe(OrderPaid.class, event -> log.add("magazyn: pakuj " + event.orderId()));
        bus.subscribe(OrderCancelled.class, event -> log.add("magazyn: zwolnij " + event.orderId() + " (" + event.reason() + ")"));

        bus.publish(new OrderPaid("ZAM-007"));
        bus.publish(new OrderCancelled("ZAM-008", "brak płatności"));
        bus.publish("zdarzenie bez słuchaczy"); // nikt nie nasłuchuje typu String — nic się nie dzieje
        for (String entry : log) {
            note(entry);
        }
        // WYNIK: ℹ księgowość: faktura dla ZAM-007
        // WYNIK: ℹ magazyn: pakuj ZAM-007
        // WYNIK: ℹ magazyn: zwolnij ZAM-008 (brak płatności)

        // Spring (SpringLearning, t34): to samo, tylko oficjalnie.
        //   • publikowanie:  applicationEventPublisher.publishEvent(new OrderPaid("ZAM-007"));
        //   • nasłuchiwanie: metoda z adnotacją @EventListener i parametrem typu zdarzenia — Spring sam
        //     znajduje ją po typie parametru i woła. Nie trzeba implementować interfejsu ani się zapisywać.
        //   • domyślnie powiadamianie jest SYNCHRONICZNE: w tym samym wątku, więc wyjątek słuchacza wraca
        //     do wydawcy, a wolny słuchacz spowalnia nadawcę. Asynchroniczność włącza się osobno (@Async).
        //   • @TransactionalEventListener pozwala reagować dopiero po zatwierdzeniu transakcji — ważne, gdy
        //     e-mail „zamówienie przyjęte” nie może wyjść przed zapisem zamówienia w bazie.
        //
        // OBSERVER a PUB/SUB — czym się różnią:
        //   | cecha            | Observer                         | Pub/Sub (broker)                       |
        //   | znajomość        | podmiot ma listę słuchaczy       | wydawca i subskrybent nie wiedzą o sobie |
        //   | pośrednik        | brak (podmiot sam woła)          | broker/kolejka (np. Kafka, RabbitMQ)    |
        //   | zasięg           | jeden proces JVM                 | wiele procesów, maszyn                  |
        //   | czas             | zwykle synchronicznie            | zwykle asynchronicznie, z buforem       |
        //   | gwarancje        | brak (wyjątek leci do nadawcy)   | zależy od brokera (ponawianie, trwałość)|
        // Nasza EventBus leży pośrodku: ma pośrednika, ale w obrębie jednego procesu i synchronicznie.
        //
        // PUŁAPKA: nadużycie zdarzeń. Gdy wszystko jest zdarzeniem, nie widać, co się dzieje po wywołaniu
        //   metody („kto na to reaguje?”) — przepływ programu staje się niewidzialny, a debugowanie koszmarem.
        //   DOBRA PRAKTYKA: zdarzenia dla skutków POBOCZNYCH (powiadomienia, audyt, odświeżenie cache);
        //   główny, wymagany krok biznesowy wołaj zwykłą metodą, żeby było go widać w kodzie.
    }

    // =================================================================================================
    // 10. TESTOWALNOŚĆ
    // =================================================================================================

    /** Serwis zamówień, który tylko ogłasza zmianę statusu — nic nie wie o e-mailach. */
    static final class OrderService {
        private final Publisher<OrderEvent> publisher;
        private final Map<String, OrderStatus> statuses = new HashMap<>();

        OrderService(Publisher<OrderEvent> publisher) {
            this.publisher = publisher;
        }

        void changeStatus(String orderId, OrderStatus newStatus) {
            OrderStatus old = statuses.getOrDefault(orderId, OrderStatus.NOWE);
            statuses.put(orderId, newStatus);
            publisher.publish(new OrderEvent(orderId, old, newStatus));
        }
    }

    /**
     * 10. Test serwisu bez e-maila, SMS-a i magazynu: wystarczy „nagrywający” słuchacz (lista).
     * Obserwator rozdziela odpowiedzialności, więc każdą część testujemy osobno.
     */
    static void testability() {
        section("10. Testowanie — nagrywający słuchacz");

        Publisher<OrderEvent> publisher = new Publisher<>(error -> { });
        OrderService service = new OrderService(publisher);

        List<OrderEvent> recorded = new ArrayList<>(); // recorded = nagrane
        publisher.subscribe(recorded::add);            // słuchacz-atrapa (fake) — tylko zapisuje

        service.changeStatus("ZAM-009", OrderStatus.OPLACONE);
        service.changeStatus("ZAM-009", OrderStatus.WYSLANE);

        Check.equal("dwa zdarzenia", 2, () -> recorded.size());
        Check.equal("pierwsze zdarzenie", new OrderEvent("ZAM-009", OrderStatus.NOWE, OrderStatus.OPLACONE), () -> recorded.get(0));
        Check.equal("drugie: poprzedni status", OrderStatus.OPLACONE, () -> recorded.get(1).from());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 3 OK, ✘ 0 BŁĄD

        // Testy osobnych słuchaczy: `new SmsObserver(log).onOrderChanged(new OrderEvent(...))` i sprawdzamy log —
        // bez serwisu, bez wątków, bez czekania. Kolejność powiadomień jest deterministyczna (lista), więc
        // testy się nie „migają”. Odwrotnie niż przy kodzie z sekcji 1, gdzie test serwisu wymagał trzech zależności.

        // KIEDY UŻYWAĆ / KIEDY NIE:
        //   | używaj, gdy                                     | nie używaj, gdy                                  |
        //   | jedna zmiana, wielu zainteresowanych            | jest jeden odbiorca na stałe (zwykłe wywołanie)  |
        //   | odbiorców przybywa lub zmieniają się w runtime  | kolejność i wynik MUSZĄ być pod ścisłą kontrolą  |
        //   | skutki poboczne: powiadomienia, audyt, cache    | krok jest wymagany do poprawności operacji       |
        //   | chcesz oddzielić moduły (niski coupling)        | przepływ staje się nie do prześledzenia          |
        // Pamiętaj o YAGNI: dwa słuchacze znane z góry i nigdy się niezmieniające — czasem prosta metoda wystarczy.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Observer = podmiot trzyma listę słuchaczy i woła każdego przy zmianie; podmiot nie zna szczegółów odbiorców.
     *   • Nowocześnie: Consumer<Zdarzenie> + lambdy; zdarzenie jako niezmienny record; push zamiast pull.
     *   • Zapisz się → dostań uchwyt Subscription (AutoCloseable); wypisuj się symetrycznie (try-with-resources).
     *   • Decyzje: kolejność (lista), wyjątek słuchacza (izoluj i loguj), wypisanie w trakcie (migawka listy),
     *     synchroniczność (domyślnie ten sam wątek — wolny słuchacz spowalnia nadawcę).
     *   • Wyciek słuchaczy: wydawca trzyma silne referencje — zapomniane wypisanie = pamięć + zbędna praca.
     *   • JDK: PropertyChangeSupport (java.beans); java.util.Observable/Observer przestarzałe od Javy 9.
     *   • Spring: ApplicationEventPublisher + @EventListener (synchronicznie, jeśli nie dodasz @Async).
     *   • Observer (w procesie) ≠ pub/sub z brokerem (między procesami, zwykle asynchronicznie).
     *   • Nie rób z wszystkiego zdarzenia — kluczowy krok biznesowy niech będzie widoczny jako zwykłe wywołanie.
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie trzy role występują w klasycznym wzorcu Observer (nazwy GoF) i co robi każda?
     *   2. Czym różni się model push od pull? Który wybraliśmy i dlaczego?
     *   3. Dlaczego słuchacza wypisujemy uchwytem, a nie „taką samą” lambdą?
     *   4. Co wypisze:
     *        Publisher<String> p = new Publisher<>(e -> { });
     *        List<String> log = new ArrayList<>();
     *        p.subscribe(t -> log.add("A" + t));
     *        p.subscribeOnce(t -> log.add("B" + t));
     *        p.publish("1"); p.publish("2");
     *        System.out.println(log);
     *   5. ZNAJDŹ BŁĄD:  for (Consumer<String> l : listeners) { l.accept(event); }
     *        — słuchacz A rzuca wyjątek i B nigdy nie dostaje zdarzenia. Co zmienić i co zdecydować?
     *   6. Co wypisze (PropertyChangeSupport): setStatus(OPLACONE); setStatus(OPLACONE); — ile zdarzeń dostanie słuchacz?
     *   7. Dlaczego „zamknięty” widok może dalej zużywać pamięć i procesor? Jak temu zapobiec?
     *   8. Czym różni się Observer od pub/sub z brokerem? Podaj dwie różnice.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: suma zdarzeń", 10, () -> exercise1());
        Check.equal("ćw. 2: wypisanie się", List.of(1), () -> exercise2());
        Check.equal("ćw. 3: SMS tylko po wysłaniu", List.of("SMS: ZAM-1"), () -> exercise3());
        Check.equal("ćw. 4: izolacja błędów", List.of("A:x", "błąd: boom", "C:x"), () -> exercise4());
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 10, () -> solution1());
        Check.equal("ćw. 2 (wzorzec)", List.of(1), () -> solution2());
        Check.equal("ćw. 3 (wzorzec)", List.of("SMS: ZAM-1"), () -> solution3());
        Check.equal("ćw. 4 (wzorzec)", List.of("A:x", "błąd: boom", "C:x"), () -> solution4());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): Utwórz {@code Publisher<Integer>}, zapisz słuchacza sumującego otrzymane liczby,
     * opublikuj 1, 2, 3, 4 i zwróć sumę.
     * Podpowiedź: sumę trzymaj w tablicy {@code int[1]} (lambda nie zmieni zwykłej zmiennej lokalnej).
     */
    static int exercise1() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): Zapisz słuchacza zbierającego liczby do listy, opublikuj 1, wypisz się
     * (close na uchwycie), opublikuj 2 i zwróć listę zebranych liczb.
     * Podpowiedź: {@code Subscription s = publisher.subscribe(...)}.
     */
    static List<Integer> exercise2() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie, PRZEPISZ): Poniżej stary kod, w którym serwis sam wysyła SMS.
     * Przepisz go na Observer: serwis publikuje {@code OrderEvent}, a osobny słuchacz dopisuje
     * {@code "SMS: " + orderId} do listy tylko dla zdarzeń z {@code to == WYSLANE}.
     * Opublikuj dwa zdarzenia dla ZAM-1 (NOWE → OPLACONE oraz OPLACONE → WYSLANE) i zwróć listę.
     * <pre>{@code
     * // PRZED:
     * void changeStatus(String id, OrderStatus s) {
     *     if (s == OrderStatus.WYSLANE) { smsSender.send("SMS: " + id); }
     * }
     * }</pre>
     * Podpowiedź: słuchacz to lambda z if-em na {@code event.to()}.
     */
    static List<String> exercise3() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): Zbuduj wydawcę {@code Publisher<String>}, którego errorHandler dopisuje do listy
     * {@code "błąd: " + komunikat}. Zapisz trzech słuchaczy w kolejności: A dopisuje {@code "A:" + tekst};
     * B rzuca {@code IllegalStateException("boom")}; C dopisuje {@code "C:" + tekst}. Opublikuj "x" i zwróć listę.
     * Podpowiedź: wynik ma zawierać wpisy A, błąd i C — dokładnie w tej kolejności.
     */
    static List<String> exercise4() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1() {
        Publisher<Integer> publisher = new Publisher<>(error -> { });
        int[] sum = {0};
        publisher.subscribe(number -> sum[0] += number);
        for (int i = 1; i <= 4; i++) {
            publisher.publish(i);
        }
        return sum[0];
    }

    static List<Integer> solution2() {
        Publisher<Integer> publisher = new Publisher<>(error -> { });
        List<Integer> received = new ArrayList<>();
        Subscription subscription = publisher.subscribe(received::add);
        publisher.publish(1);
        subscription.close();
        publisher.publish(2);
        return received;
    }

    static List<String> solution3() {
        Publisher<OrderEvent> publisher = new Publisher<>(error -> { });
        List<String> sms = new ArrayList<>();
        publisher.subscribe(event -> {
            if (event.to() == OrderStatus.WYSLANE) {
                sms.add("SMS: " + event.orderId());
            }
        });
        publisher.publish(new OrderEvent("ZAM-1", OrderStatus.NOWE, OrderStatus.OPLACONE));
        publisher.publish(new OrderEvent("ZAM-1", OrderStatus.OPLACONE, OrderStatus.WYSLANE));
        return sms;
    }

    static List<String> solution4() {
        List<String> log = new ArrayList<>();
        Publisher<String> publisher = new Publisher<>(error -> log.add("błąd: " + error.getMessage()));
        publisher.subscribe(text -> log.add("A:" + text));
        publisher.subscribe(text -> {
            throw new IllegalStateException("boom");
        });
        publisher.subscribe(text -> log.add("C:" + text));
        publisher.publish("x");
        return log;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Subject (podmiot) — trzyma stan i listę słuchaczy, powiadamia; Observer (obserwator) — interfejs
     *      z metodą „coś się zmieniło”; ConcreteObserver — konkretna reakcja (e-mail, SMS, magazyn).
     *   2. Push: podmiot przesyła zdarzenie z danymi. Pull: podmiot mówi tylko „zmieniło się”, a słuchacz
     *      sam dopytuje o szczegóły. Wybraliśmy push (zdarzenie jako record) — prostsze, a słuchacz nie
     *      musi znać podmiotu.
     *   3. Każde wystąpienie lambdy w kodzie to osobny obiekt, a remove porównuje przez equals (dla lambd =
     *      tożsamość), więc „taka sama” lambda niczego nie usunie (metoda zwróci false, bez błędu).
     *   4. [A1, B1, A2] — jednorazowy B wypisuje się po pierwszym zdarzeniu.
     *   5. Naiwna pętla przerywa się na wyjątku. Opakuj wywołanie w try/catch (RuntimeException), przekaż błąd
     *      do errorHandler/loga i kontynuuj; zdecyduj też, czy izolacja jest właściwa (nie dla walidatorów).
     *   6. Jedno zdarzenie — przy drugiej wartości równej poprzedniej PropertyChangeSupport niczego nie wysyła.
     *   7. Wydawca trzyma silną referencję do słuchacza (lambdy), więc odśmiecacz nie zbierze widoku, a jego
     *      lambda dalej się wykonuje. Zapobiega temu symetryczny close()/wypisanie (try-with-resources) albo
     *      słabe referencje.
     *   8. Observer: jeden proces, podmiot sam woła słuchaczy, zwykle synchronicznie. Pub/sub: broker
     *      pośredniczy, nadawca i odbiorca nie znają się, zasięg to wiele procesów, zwykle asynchronicznie.
     */
    // </editor-fold>
}
