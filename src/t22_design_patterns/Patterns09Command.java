package t22_design_patterns;

import helpers.Check;
import helpers.model.OrderStatus;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Command (polecenie) — operacja jako obiekt
 *        (command = polecenie, rozkaz; execute = wykonaj; undo = cofnij; redo = ponów cofnięte;
 *         invoker = wywołujący; receiver = odbiorca; history = historia; macro = makro, zestaw poleceń)
 *
 * W SKRÓCIE:
 *   Zamiast WYWOŁAĆ metodę, pakujesz „co zrobić” (i, jeśli trzeba, „jak to cofnąć”) w obiekt. Obiekt-polecenie
 *   można zapisać na liście, odłożyć na później, wykonać ponownie, zalogować, zgrupować w makro i cofnąć.
 *   To podstawa przycisków „Cofnij/Ponów”, kolejek zadań i dziennika operacji.
 *
 * ANALOGIA: kelner w restauracji. Gość nie idzie do kuchni — pisze zamówienie na kartce. Kartka (polecenie)
 *   trafia do kucharza (odbiorcy) później, może leżeć w kolejce, można ją przekazać innemu kucharzowi,
 *   zachować w rachunku jako dowód i — jeśli gość zmieni zdanie — skreślić (cofnąć). Kelner nie umie gotować
 *   i nie musi: tylko nosi kartki.
 *
 * JAK TO DZIAŁA:
 *   Role GoF:  Command (polecenie) — interfejs execute() [+ undo()];  ConcreteCommand — konkretne polecenie,
 *              które zna odbiorcę i parametry;  Receiver (odbiorca) — obiekt, który naprawdę coś robi
 *              (u nas dokument tekstowy, konto);  Invoker (wywołujący) — ten, kto uruchamia polecenia
 *              (przycisk, kolejka, historia cofania);  Client (klient) — tworzy polecenia i je przekazuje.
 *
 *      Invoker (Editor / kolejka)  --- execute() --->  Command  --- wywołuje --->  Receiver (TextDocument)
 *      trzyma historię (stosy undo/redo)               InsertCommand, DeleteCommand, MacroCommand
 *
 *   Cofanie to dwa stosy: wykonane (undo) i cofnięte (redo). execute: polecenie na stos undo, stos redo czyścimy.
 *   undo: zdejmij ze stosu undo, wywołaj undo(), połóż na redo. redo: odwrotnie.
 *   Stos = Deque (ArrayDeque, push/pop) — nie klasa Stack (stara, zsynchronizowana, t12).
 *
 * SŁÓWKA:
 *   command = polecenie; execute = wykonaj; undo = cofnij; redo = ponów; invoker = wywołujący; receiver = odbiorca;
 *   macro = makro; queue = kolejka; deferred = odroczony; journal = dziennik; audit = audyt (dziennik zmian);
 *   rollback = wycofanie zmian; irreversible = nieodwracalny; handler = obsługujący (program obsługi polecenia).
 *
 * ZOBACZ TEŻ: t12_collections/Collections06QueuesDeques (stosy i kolejki),
 *   t13_lambdas/Lambda02FunctionalInterfaces (Runnable, Callable),
 *   t21_concurrency/Concurrency04Executors (kolejka zadań Runnable),
 *   t22_design_patterns/Patterns06Observer (zdarzenie: „coś się stało”, polecenie: „zrób to”),
 *   t22_design_patterns/Patterns07Decorator (dekorator wokół polecenia),
 *   t22_design_patterns/Patterns12Composite (makro to kompozyt poleceń)
 * </pre>
 */
public class Patterns09Command {

    public static void main(String[] args) {
        title("Patterns09 — Command (polecenie)");

        problem();            // problem = problem (kod bez wzorca)
        classicCommand();     // classic command = klasyczne polecenie z cofaniem
        macroCommand();       // macro command = makro (zestaw poleceń)
        queueOfCommands();    // queue of commands = kolejka poleceń (wykonanie odroczone)
        lambdaCommands();     // lambda commands = polecenia jako lambdy (konto bankowe)
        undoPitfalls();       // undo pitfalls = pułapki cofania
        commandsAsData();     // commands as data = polecenia jako dane, obsługa i CQRS
        testability();        // testability = łatwość testowania
        exercises();          // exercises = ćwiczenia
    }

    // =================================================================================================
    // Wspólne typy lekcji
    // =================================================================================================

    /** Polecenie: wykonaj, cofnij, opisz. undoable = „da się cofnąć” (domyślnie tak). */
    interface Command {
        void execute();

        void undo();

        String describe(); // describe = opisz (do dziennika)

        default boolean undoable() { // default = metoda z domyślną treścią w interfejsie (Java 8+)
            return true;
        }
    }

    /** Odbiorca (receiver) nr 1: dokument tekstowy. */
    static final class TextDocument {
        private final StringBuilder text = new StringBuilder();

        void insert(int position, String fragment) { // insert = wstaw; position = pozycja; fragment = kawałek tekstu
            text.insert(position, fragment);
        }

        /** Usuwa tekst od {@code from} do {@code to} (bez {@code to}) i zwraca usunięty fragment. */
        String delete(int from, int to) { // delete = usuń
            int end = Math.min(to, text.length());
            String removed = text.substring(from, end); // removed = usunięty fragment
            text.delete(from, end);
            return removed;
        }

        String text() {
            return text.toString();
        }
    }

    /** Wstaw tekst. Cofnięcie = usuń dokładnie ten fragment. Record: niezmienne dane polecenia. */
    record InsertCommand(TextDocument document, int position, String fragment) implements Command {
        @Override
        public void execute() {
            document.insert(position, fragment);
        }

        @Override
        public void undo() {
            document.delete(position, position + fragment.length());
        }

        @Override
        public String describe() {
            return "wstaw '" + fragment + "' na pozycji " + position;
        }
    }

    /**
     * Usuń zakres. Żeby dało się cofnąć, polecenie MUSI zapamiętać usunięty tekst — robi to w execute
     * (dopiero wtedy wiadomo, co faktycznie było w dokumencie).
     */
    static final class DeleteCommand implements Command {
        private final TextDocument document;
        private final int from;
        private final int to;
        private String removed; // removed = usunięty tekst; null, dopóki polecenie nie zostało wykonane

        DeleteCommand(TextDocument document, int from, int to) {
            this.document = document;
            this.from = from;
            this.to = to;
        }

        @Override
        public void execute() {
            removed = document.delete(from, to);
        }

        @Override
        public void undo() {
            if (removed == null) {
                throw new IllegalStateException("nie ma czego cofać — polecenie nie zostało wykonane");
            }
            document.insert(from, removed);
        }

        @Override
        public String describe() {
            return "usuń znaki " + from + "–" + to;
        }
    }

    /**
     * Wywołujący (invoker) z historią. Reguły: polecenie trafia na stos DOPIERO po udanym wykonaniu;
     * undo zdejmuje ze stosu dopiero po udanym cofnięciu; limit chroni pamięć; dziennik zapisuje operacje.
     */
    static final class History {
        private final Deque<Command> undoStack = new ArrayDeque<>(); // Deque jako stos: push/pop na początku
        private final Deque<Command> redoStack = new ArrayDeque<>();
        private final List<String> journal = new ArrayList<>();      // journal = dziennik operacji
        private final int limit;                                      // limit = ile ostatnich poleceń pamiętamy

        History() {
            this(100);
        }

        History(int limit) {
            this.limit = limit;
        }

        void execute(Command command) {
            command.execute(); // jeśli rzuci wyjątek — do historii nic nie trafia
            journal.add("wykonano: " + command.describe());
            if (command.undoable()) {
                undoStack.push(command);
                while (undoStack.size() > limit) {
                    undoStack.removeLast(); // zapomnij najstarsze
                }
            } else {
                undoStack.clear(); // po operacji nieodwracalnej wcześniejszych nie cofniemy „przez nią”
            }
            redoStack.clear(); // nowa operacja unieważnia „ponów”
        }

        boolean undo() {
            Command command = undoStack.peek(); // peek = podejrzyj (nie zdejmuj)
            if (command == null) {
                return false;
            }
            command.undo(); // dopiero po sukcesie zdejmujemy ze stosu
            undoStack.pop();
            redoStack.push(command);
            journal.add("cofnięto: " + command.describe());
            return true;
        }

        boolean redo() {
            Command command = redoStack.peek();
            if (command == null) {
                return false;
            }
            command.execute();
            redoStack.pop();
            undoStack.push(command);
            journal.add("ponowiono: " + command.describe());
            return true;
        }

        int undoSize() {
            return undoStack.size();
        }

        List<String> journal() {
            return journal;
        }
    }

    // =================================================================================================
    // 1. PROBLEM
    // =================================================================================================

    /** Wersja BEZ wzorca: edytor wykonuje akcje wprost; cofanie to jedna kopia całego tekstu. */
    static final class NaiveEditor {
        private String text = "";
        private String backup = ""; // backup = kopia zapasowa jednego poprzedniego stanu

        void append(String fragment) { // append = dopisz na końcu
            backup = text;
            text = text + fragment;
        }

        void undo() {
            text = backup;
        }

        String text() {
            return text;
        }
    }

    /**
     * 1. Problem: akcje są wywołaniami metod, więc nie da się ich zapisać, odłożyć, powtórzyć ani cofnąć
     * więcej niż jednego kroku. Przycisk, skrót klawiszowy i menu wołają ten sam kod w trzech miejscach.
     */
    static void problem() {
        section("1. Problem — akcje jako wywołania metod");

        NaiveEditor editor = new NaiveEditor();
        editor.append("A");
        editor.append("B");
        editor.append("C");
        show("tekst", editor.text());
        // WYNIK: tekst → ABC
        editor.undo();
        show("po pierwszym undo", editor.text());
        // WYNIK: po pierwszym undo → AB
        editor.undo();
        show("po drugim undo", editor.text());
        // WYNIK: po drugim undo → AB

        // PUŁAPKA: jedna kopia zapasowa = cofnąć można dokładnie jeden krok; drugie undo „cofa” do tego samego
        //   stanu. Zapamiętywanie CAŁEGO tekstu przy każdej zmianie to marnotrawstwo pamięci (dokument 10 MB × 100
        //   kroków). Polecenie zapamiętuje tylko RÓŻNICĘ: „wstawiono 'B' na pozycji 1”.
        // Dalsze braki: nie ma redo, nie ma makr („wykonaj 5 operacji jako jedną”), nie ma kolejki ani dziennika
        //   („kto i kiedy usunął akapit?”), a każdą akcję trzeba duplikować dla przycisku, skrótu i menu.
    }

    // =================================================================================================
    // 2. KLASYCZNE POLECENIE Z COFANIEM
    // =================================================================================================

    /**
     * 2. Klasyczne polecenie: konkretne klasy (Insert, Delete) znają odbiorcę i parametry; historia (invoker)
     * zna tylko interfejs Command. Dwa stosy dają cofanie i ponawianie na dowolną głębokość.
     */
    static void classicCommand() {
        section("2. Klasyczne polecenia — cofanie i ponawianie");

        TextDocument document = new TextDocument();
        History history = new History();
        history.execute(new InsertCommand(document, 0, "Ala ma kota"));
        history.execute(new InsertCommand(document, 11, " i psa"));
        history.execute(new DeleteCommand(document, 0, 4));
        show("po trzech poleceniach", document.text());
        // WYNIK: po trzech poleceniach → ma kota i psa

        history.undo();
        show("undo usunięcia", document.text());
        // WYNIK: undo usunięcia → Ala ma kota i psa
        history.undo();
        show("undo wstawienia", document.text());
        // WYNIK: undo wstawienia → Ala ma kota
        history.redo();
        show("redo wstawienia", document.text());
        // WYNIK: redo wstawienia → Ala ma kota i psa

        history.execute(new InsertCommand(document, 0, ">> ")); // nowa operacja czyści redo
        show("po nowej operacji", document.text());
        // WYNIK: po nowej operacji → >> Ala ma kota i psa
        show("czy da się ponowić", history.redo());
        // WYNIK: czy da się ponowić → false
        showEach("dziennik", history.journal());
        // WYNIK: dziennik (liczba elementów: 7):
        // WYNIK: • wykonano: wstaw 'Ala ma kota' na pozycji 0
        // WYNIK: • wykonano: wstaw ' i psa' na pozycji 11
        // WYNIK: • wykonano: usuń znaki 0–4
        // WYNIK: • cofnięto: usuń znaki 0–4
        // WYNIK: • cofnięto: wstaw ' i psa' na pozycji 11
        // WYNIK: • ponowiono: wstaw ' i psa' na pozycji 11
        // WYNIK: • wykonano: wstaw '>> ' na pozycji 0

        // Dlaczego nowa operacja czyści „ponów”: stan dokumentu rozminął się z tym, który polecenie redo
        // zakładało (np. pozycje znaków się przesunęły) — ponowienie mogłoby zepsuć tekst. Tak działa każdy edytor.
        // Dziennik wychodzi „za darmo”: wszystkie operacje przechodzą przez jedno miejsce (historię), więc
        // audyt (kto, kiedy, co) to dopisanie jednej linijki, a nie zmiana każdej akcji.
        // DOBRA PRAKTYKA: polecenie ma jedną odpowiedzialność i jedną parę execute/undo, które są dla siebie
        //   lustrem. Dlaczego: gdy undo nie odwraca dokładnie execute, historia zaczyna „psuć” dane po cichu.
    }

    // =================================================================================================
    // 3. MAKRO
    // =================================================================================================

    /**
     * Makro (macro) = polecenie złożone z poleceń (kompozyt, t22 Patterns12Composite). Wykonuje kroki po kolei,
     * cofa w ODWROTNEJ kolejności, a gdy któryś krok zawiedzie — wycofuje już wykonane (rollback).
     */
    record MacroCommand(String name, List<Command> steps) implements Command {
        MacroCommand { // kompaktowy konstruktor rekordu (Java 16+): kopia niezmienna listy
            steps = List.copyOf(steps);
        }

        @Override
        public void execute() {
            int done = 0; // done = ile kroków wykonano
            try {
                for (Command step : steps) {
                    step.execute();
                    done++;
                }
            } catch (RuntimeException ex) {
                for (int i = done - 1; i >= 0; i--) { // wycofaj wykonane kroki od ostatniego
                    steps.get(i).undo();
                }
                throw ex;
            }
        }

        @Override
        public void undo() {
            for (int i = steps.size() - 1; i >= 0; i--) {
                steps.get(i).undo();
            }
        }

        @Override
        public String describe() {
            return name + " (" + steps.size() + " kroki)";
        }
    }

    /** Konto bankowe jako odbiorca (receiver) nr 2; kwoty w groszach. */
    static final class Account {
        private long balance; // balance = saldo

        Account(long balance) {
            this.balance = balance;
        }

        void deposit(long amount) { // deposit = wpłać
            balance += amount;
        }

        void withdraw(long amount) { // withdraw = wypłać
            if (amount > balance) {
                throw new IllegalStateException("niewystarczające środki: saldo " + balance + ", potrzeba " + amount);
            }
            balance -= amount;
        }

        long balance() {
            return balance;
        }
    }

    /** Polecenie zbudowane z dwóch lambd: „zrób” i „cofnij”. */
    record LambdaCommand(String name, Runnable action, Runnable reverse) implements Command {
        @Override
        public void execute() {
            action.run();
        }

        @Override
        public void undo() {
            reverse.run();
        }

        @Override
        public String describe() {
            return name;
        }
    }

    /** Fabryka poleceń (t22 Patterns03Factory): przelew między kontami z cofnięciem. */
    static Command transfer(Account from, Account to, long amount) { // transfer = przelew
        return new LambdaCommand("przelew " + amount + " gr",
                () -> {
                    from.withdraw(amount);
                    to.deposit(amount);
                },
                () -> {
                    to.withdraw(amount);
                    from.deposit(amount);
                });
    }

    /**
     * 3. Makro: kilka poleceń jako jedno. Jedno „Cofnij” cofa całe makro, a błąd w środku nie zostawia
     * półwykonanych zmian.
     */
    static void macroCommand() {
        section("3. Makro — kilka poleceń jako jedno");

        TextDocument document = new TextDocument();
        MacroCommand format = new MacroCommand("dodaj nagłówek i stopkę", List.of(
                new InsertCommand(document, 0, "[NAGŁÓWEK]"),
                new InsertCommand(document, 10, "[STOPKA]")));
        History history = new History();
        history.execute(format);
        show("po makrze", document.text());
        // WYNIK: po makrze → [NAGŁÓWEK][STOPKA]
        history.undo();
        show("jedno undo cofa całe makro (pusty tekst w [ ])", "[" + document.text() + "]");
        // WYNIK: jedno undo cofa całe makro (pusty tekst w [ ]) → []

        // Kolejność cofania ma znaczenie: ostatni krok cofamy pierwszy (LIFO — „ostatni wszedł, pierwszy wyszedł”).
        Command first = new InsertCommand(document, 0, "A");
        Command second = new InsertCommand(document, 1, "B");
        first.execute();
        second.execute();
        first.undo();  // ŹLE: cofamy najpierw pierwszy krok...
        second.undo(); // ...a drugi zakłada pozycje sprzed cofnięcia
        show("cofanie w złej kolejności", document.text());
        // WYNIK: cofanie w złej kolejności → B
        // PUŁAPKA: cofnięcie kroków w kolejności wykonania zostawiło 'B' w dokumencie. Dlaczego: polecenie nr 2
        //   zapamiętało pozycje obowiązujące PO kroku nr 1 — po jego cofnięciu pozycje się przesunęły i usunęliśmy
        //   nie ten fragment (albo nic). Reguła: cofaj w odwrotnej kolejności.
        Account sender = new Account(10_000);
        Account receiver = new Account(0);
        MacroCommand twoTransfers = new MacroCommand("dwa przelewy", List.of(
                transfer(sender, receiver, 6_000),
                transfer(sender, receiver, 6_000))); // drugi się nie uda: zostanie tylko 4000 gr
        expectThrows("makro z błędem w drugim kroku", () -> twoTransfers.execute());
        // WYNIK: ✔ makro z błędem w drugim kroku → rzucono IllegalStateException: niewystarczające środki: saldo 4000, potrzeba 6000
        show("nadawca po wycofaniu", sender.balance());
        // WYNIK: nadawca po wycofaniu → 10000
        show("odbiorca po wycofaniu", receiver.balance());
        // WYNIK: odbiorca po wycofaniu → 0
        // Rollback w makrze przypomina transakcję: wszystko albo nic. Dla prawdziwych baz danych użyj transakcji
        // JDBC (t29), a nie własnego cofania — to demonstracja idei, nie zamiennik transakcji.
    }

    // =================================================================================================
    // 4. KOLEJKA POLECEŃ
    // =================================================================================================

    /**
     * 4. Kolejka poleceń: polecenie jest obiektem, więc można je odłożyć i wykonać później (np. w nocy, w innym
     * wątku). Runnable i Callable z JDK to najprostsze polecenia.
     */
    static void queueOfCommands() {
        section("4. Kolejka poleceń — wykonanie odroczone, Runnable, Callable");

        List<String> log = new ArrayList<>();
        Queue<Runnable> nightJobs = new ArrayDeque<>(); // nightJobs = zadania nocne; Queue = kolejka FIFO
        nightJobs.add(() -> log.add("kopia zapasowa"));
        nightJobs.add(() -> log.add("raport sprzedaży"));
        nightJobs.add(() -> log.add("czyszczenie plików tymczasowych"));
        show("zakolejkowano, wykonano", nightJobs.size() + ", " + log.size());
        // WYNIK: zakolejkowano, wykonano → 3, 0

        while (!nightJobs.isEmpty()) {
            nightJobs.poll().run(); // poll = zdejmij z początku kolejki; run = uruchom
        }
        show("po uruchomieniu", log);
        // WYNIK: po uruchomieniu → [kopia zapasowa, raport sprzedaży, czyszczenie plików tymczasowych]

        // Runnable = polecenie bez wyniku, Callable<V> = polecenie Z WYNIKIEM, które może rzucić wyjątek kontrolowany.
        List<Callable<String>> reports = List.of(() -> "raport A", () -> "raport B");
        List<String> results = new ArrayList<>();
        try {
            for (Callable<String> job : reports) {
                results.add(job.call()); // call = wywołaj
            }
        } catch (Exception ex) { // Callable.call() deklaruje throws Exception
            throw new IllegalStateException(ex);
        }
        show("wyniki Callable", results);
        // WYNIK: wyniki Callable → [raport A, raport B]

        // W JDK polecenie to wszędzie, gdzie przekazujesz „kod do wykonania później”: Runnable w new Thread(...),
        // ExecutorService.submit(...) (t21 Concurrency04Executors: kolejka zadań + pula wątków = invoker),
        // TimerTask, ScheduledExecutorService, CompletableFuture.runAsync. Spring: TaskExecutor przyjmuje Runnable,
        // a @Async zamienia wywołanie metody na polecenie wykonane w innym wątku (SpringLearning).
        // PUŁAPKA: odłożone polecenie widzi stan z chwili WYKONANIA, a nie z chwili stworzenia. Jeśli lambda
        //   przechwytuje zmienny obiekt (np. listę, którą ktoś potem zmieni), wykona się na zmienionych danych.
        //   DOBRA PRAKTYKA: polecenia do odłożenia buduj z niezmiennych danych (record, String, liczby) —
        //   wtedy wynik nie zależy od tego, kiedy kolejka ruszy.
    }

    // =================================================================================================
    // 5. LAMBDY JAKO POLECENIA
    // =================================================================================================

    /**
     * 5. Lambdy jako polecenia: prosta operacja to dwie lambdy („zrób” i „cofnij”), bez nowej klasy.
     * Pokazujemy to na koncie bankowym (kwoty w groszach).
     */
    static void lambdaCommands() {
        section("5. Lambdy jako polecenia — konto bankowe");

        Account main = new Account(10_000);
        Account savings = new Account(0); // savings = oszczędności
        History history = new History();

        history.execute(transfer(main, savings, 3_000));
        history.execute(transfer(main, savings, 2_000));
        show("saldo konta głównego / oszczędności", main.balance() + " / " + savings.balance());
        // WYNIK: saldo konta głównego / oszczędności → 5000 / 5000
        history.undo();
        show("po cofnięciu drugiego przelewu", main.balance() + " / " + savings.balance());
        // WYNIK: po cofnięciu drugiego przelewu → 7000 / 3000
        history.redo();
        show("po ponowieniu", main.balance() + " / " + savings.balance());
        // WYNIK: po ponowieniu → 5000 / 5000

        expectThrows("przelew ponad stan", () -> history.execute(transfer(main, savings, 99_000)));
        // WYNIK: ✔ przelew ponad stan → rzucono IllegalStateException: niewystarczające środki: saldo 5000, potrzeba 99000
        show("polecenia w historii (nieudane nie wchodzi)", history.undoSize());
        // WYNIK: polecenia w historii (nieudane nie wchodzi) → 2

        // Kiedy lambdy, a kiedy klasa? Lambda/LambdaCommand — krótkie operacje z prostym cofnięciem. Klasa
        // (DeleteCommand) — gdy polecenie musi coś ZAPAMIĘTAĆ przy wykonaniu (usunięty tekst), ma własny stan
        // albo wymaga osobnych testów. Gdy polecenia NIE trzeba cofać, wystarczy Runnable lub własny interfejs
        // funkcyjny z jedną metodą — wtedy Command to po prostu „nazwa dla lambdy” (t13 Lambda02FunctionalInterfaces).
        // Ważne: nieudane polecenie (wyjątek w execute) NIE trafia do historii — inaczej „cofanie” cofałoby coś,
        // co się nie wydarzyło. Dlatego History.execute dodaje do stosu dopiero po udanym wykonaniu.
    }

    // =================================================================================================
    // 6. PUŁAPKI COFANIA
    // =================================================================================================

    /** Prosty odbiorca: status zamówienia w jednym polu. */
    static final class StatusHolder {
        OrderStatus status; // status = bieżący status zamówienia (pole dostępne bezpośrednio — uproszczenie lekcji)

        StatusHolder(OrderStatus status) {
            this.status = status;
        }
    }

    /** ŹLE: cofanie „na sztywno” — zakłada, że poprzednim statusem było NOWE. */
    record BadSetStatus(StatusHolder holder, OrderStatus newStatus) implements Command {
        @Override
        public void execute() {
            holder.status = newStatus;
        }

        @Override
        public void undo() {
            holder.status = OrderStatus.NOWE; // zgadujemy poprzedni stan
        }

        @Override
        public String describe() {
            return "ustaw status " + newStatus;
        }
    }

    /** DOBRZE: poprzedni status zapamiętany w momencie wykonania (execute), nie tworzenia polecenia. */
    static final class SetStatus implements Command {
        private final StatusHolder holder;
        private final OrderStatus newStatus;
        private OrderStatus previous; // previous = poprzedni status, znany dopiero przy execute

        SetStatus(StatusHolder holder, OrderStatus newStatus) {
            this.holder = holder;
            this.newStatus = newStatus;
        }

        @Override
        public void execute() {
            previous = holder.status;
            holder.status = newStatus;
        }

        @Override
        public void undo() {
            holder.status = previous;
        }

        @Override
        public String describe() {
            return "ustaw status " + newStatus;
        }
    }

    /** Polecenie nieodwracalne: wysłany e-mail nie wraca. */
    record SendMail(List<String> outbox, String text) implements Command { // outbox = skrzynka wysłanych
        @Override
        public void execute() {
            outbox.add(text);
        }

        @Override
        public void undo() {
            throw new UnsupportedOperationException("wysłanego maila nie da się cofnąć");
        }

        @Override
        public String describe() {
            return "wyślij mail '" + text + "'";
        }

        @Override
        public boolean undoable() {
            return false;
        }
    }

    /**
     * 6. Pułapki cofania: (1) undo, które nie przywraca prawdziwego stanu, (2) operacje nieodwracalne,
     * (3) cofnięcie, które samo się nie udaje, (4) nieograniczona historia.
     */
    static void undoPitfalls() {
        section("6. Pułapki cofania — co trzeba zapamiętać");

        StatusHolder badHolder = new StatusHolder(OrderStatus.OPLACONE);
        Command bad = new BadSetStatus(badHolder, OrderStatus.WYSLANE);
        bad.execute();
        bad.undo();
        show("źle: po undo (było OPLACONE)", badHolder.status);
        // WYNIK: źle: po undo (było OPLACONE) → NOWE

        StatusHolder goodHolder = new StatusHolder(OrderStatus.NOWE);
        Command good = new SetStatus(goodHolder, OrderStatus.WYSLANE);
        goodHolder.status = OrderStatus.OPLACONE; // ktoś zmienił status ZANIM polecenie zostało wykonane
        good.execute();
        good.undo();
        show("dobrze: po undo", goodHolder.status);
        // WYNIK: dobrze: po undo → OPLACONE
        // PUŁAPKA: undo, które „zgaduje” poprzedni stan. Dlaczego groźne: wygląda poprawnie w prostych
        //   testach, a w produkcji po cichu psuje dane. Polecenie musi zapamiętać to, co potrzebne do cofnięcia,
        //   i to w chwili execute — stan mógł się zmienić między utworzeniem polecenia a jego wykonaniem.

        List<String> outbox = new ArrayList<>();
        History history = new History();
        history.execute(new InsertCommand(new TextDocument(), 0, "x"));
        history.execute(new SendMail(outbox, "Zamówienie przyjęte"));
        show("czy da się cofnąć po wysłaniu maila", history.undo());
        // WYNIK: czy da się cofnąć po wysłaniu maila → false
        // Kolejna pułapka — operacje, których nie da się odwrócić (mail, płatność kartą, usunięcie pliku bez kosza).
        //   Nie udawaj cofania. Rozwiązania: oznacz polecenie jako nieodwracalne (nasze undoable() == false —
        //   historia się zeruje), albo zrób polecenie KOMPENSUJĄCE („wyślij sprostowanie”, „zwrot pieniędzy”),
        //   albo poproś o potwierdzenie PRZED wykonaniem.

        Account from = new Account(10_000);
        Account to = new Account(0);
        History bank = new History();
        bank.execute(transfer(from, to, 6_000));
        to.withdraw(5_000); // odbiorca wydał pieniądze poza historią
        expectThrows("cofnięcie przelewu bez środków", () -> bank.undo());
        // WYNIK: ✔ cofnięcie przelewu bez środków → rzucono IllegalStateException: niewystarczające środki: saldo 1000, potrzeba 6000
        show("polecenie nadal w historii", bank.undoSize());
        // WYNIK: polecenie nadal w historii → 1
        // Kolejna pułapka — cofnięcie samo może się nie udać (stan świata zmienił się poza historią). Nasza historia
        //   zdejmuje polecenie ze stosu dopiero po udanym undo (peek, nie pop), więc po błędzie nie jest
        //   w stanie „pół na pół”. Gdyby było odwrotnie (pop, potem undo), polecenie zniknęłoby, a stan zostałby zepsuty.

        History limited = new History(2); // limited = z limitem
        TextDocument document = new TextDocument();
        for (String fragment : List.of("a", "b", "c")) {
            limited.execute(new InsertCommand(document, document.text().length(), fragment));
        }
        boolean u1 = limited.undo();
        boolean u2 = limited.undo();
        boolean u3 = limited.undo();
        show("trzy próby undo przy limicie 2", u1 + ", " + u2 + ", " + u3);
        // WYNIK: trzy próby undo przy limicie 2 → true, true, false
        show("zostało w dokumencie", document.text());
        // WYNIK: zostało w dokumencie → a
        // Kolejna pułapka — nieograniczona historia = wyciek pamięci w długo żyjącej aplikacji (każde polecenie trzyma
        //   odbiorcę i dane). DOBRA PRAKTYKA: ogranicz głębokość historii i zapamiętuj różnice, nie całe kopie.
    }

    // =================================================================================================
    // 7. POLECENIA JAKO DANE
    // =================================================================================================

    /** Polecenie jako czyste dane: „zrób coś” (rozkaz), bez logiki — to ją ma obsługujący. */
    record PlaceOrder(String customer, String sku, int quantity) { } // PlaceOrder = złóż zamówienie

    record CancelOrder(String orderId) { } // CancelOrder = anuluj zamówienie

    /** Obsługujący polecenie (handler). Dla każdego typu polecenia jest DOKŁADNIE jeden. */
    interface CommandHandler<C> {
        void handle(C command); // handle = obsłuż
    }

    /** Magistrala poleceń: wybiera obsługującego po typie polecenia. */
    static final class CommandBus {
        private final Map<Class<?>, CommandHandler<Object>> handlers = new HashMap<>(); // wyszukiwanie po typie

        <C> void register(Class<C> type, CommandHandler<? super C> handler) {
            handlers.put(type, command -> handler.handle(type.cast(command)));
        }

        void dispatch(Object command) { // dispatch = wyślij do obsługi
            CommandHandler<Object> handler = handlers.get(command.getClass());
            if (handler == null) {
                throw new IllegalArgumentException("brak obsługi dla " + command.getClass().getSimpleName());
            }
            handler.handle(command);
        }
    }

    /**
     * 7. Polecenie jako dane: record opisuje ZAMIAR, a osobny obsługujący go wykonuje. Takie polecenia łatwo
     * zalogować, zapisać do bazy, wysłać przez sieć i odtworzyć. To też podstawa CQRS.
     */
    static void commandsAsData() {
        section("7. Polecenia jako dane — record + obsługujący + CQRS");

        List<String> audit = new ArrayList<>(); // audit = dziennik audytu
        CommandBus bus = new CommandBus();
        bus.register(PlaceOrder.class, command -> audit.add(
                "zamówiono " + command.quantity() + " x " + command.sku() + " dla " + command.customer()));
        bus.register(CancelOrder.class, command -> audit.add("anulowano " + command.orderId()));

        bus.dispatch(new PlaceOrder("Jan Kowalski", "ELE-003", 2));
        bus.dispatch(new CancelOrder("ZAM-010"));
        showEach("dziennik audytu", audit);
        // WYNIK: dziennik audytu (liczba elementów: 2):
        // WYNIK: • zamówiono 2 x ELE-003 dla Jan Kowalski
        // WYNIK: • anulowano ZAM-010
        expectThrows("polecenie bez obsługi", () -> bus.dispatch("kup coś"));
        // WYNIK: ✔ polecenie bez obsługi → rzucono IllegalArgumentException: brak obsługi dla String

        // Różnica wobec Observera (t22 Patterns06Observer): zdarzenie mówi „coś się STAŁO” (przeszłość, wielu
        // słuchaczy, nadawca nie czeka na nic), polecenie mówi „zrób to” (rozkaz, DOKŁADNIE jeden obsługujący,
        // nadawca zwykle oczekuje wykonania). Zdarzenia nazywamy w czasie przeszłym (OrderPlaced),
        // polecenia w trybie rozkazującym (PlaceOrder).
        // CQRS (Command Query Responsibility Segregation = rozdzielenie odpowiedzialności poleceń i zapytań):
        // operacje ZMIENIAJĄCE stan (polecenia) i operacje CZYTAJĄCE (zapytania) mają osobne modele i ścieżki.
        // Polecenie niczego nie zwraca (poza potwierdzeniem), zapytanie niczego nie zmienia. To jedynie krewny
        // wzorca Command — nacisk jest na rozkaz jako wiadomość, nie na cofanie.
        // (Java 21+) Obsługę wielu typów poleceń można zapisać jednym switchem po typie z zapieczętowanym
        // interfejsem — kompilator sprawdzi, czy obsłużono każdy rekord; w Javie 17 używamy mapy lub instanceof.
        // PUŁAPKA: „anemiczne” polecenia i obsługa w jednej wielkiej klasie-bogu (switch na 40 typów).
        //   Jeden handler = jedna klasa/lambda = jedno polecenie; wtedy dopisanie nowego nie rusza istniejących.
    }

    // =================================================================================================
    // 8. TESTOWALNOŚĆ I KIEDY UŻYWAĆ
    // =================================================================================================

    /**
     * 8. Testowanie: polecenia testujemy bez interfejsu użytkownika — wystarczy odbiorca i sprawdzenie stanu
     * po execute i po undo. Historię testujemy atrapami poleceń.
     */
    static void testability() {
        section("8. Testowanie — polecenie bez interfejsu użytkownika");

        TextDocument document = new TextDocument();
        Command insert = new InsertCommand(document, 0, "kawa");
        Command delete = new DeleteCommand(document, 1, 3);
        List<String> trace = new ArrayList<>(); // trace = ślad wywołań atrapy
        Command fake = new LambdaCommand("atrapa", () -> trace.add("execute"), () -> trace.add("undo"));
        History history = new History();

        Check.equal("insert → execute", "kawa", () -> {
            insert.execute();
            return document.text();
        });
        Check.equal("delete → execute", "ka", () -> {
            delete.execute();
            return document.text();
        });
        Check.equal("delete → undo", "kawa", () -> {
            delete.undo();
            return document.text();
        });
        Check.equal("historia: execute, undo, redo wołają atrapę", List.of("execute", "undo", "execute"), () -> {
            history.execute(fake);
            history.undo();
            history.redo();
            return trace;
        });
        Check.throwsException("undo przed execute", IllegalStateException.class,
                () -> new DeleteCommand(document, 0, 1).undo());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD

        // KIEDY UŻYWAĆ / KIEDY NIE:
        //   | używaj, gdy                                          | nie używaj, gdy                                  |
        //   | potrzebne cofnij/ponów                               | operacja jest prostym, jednorazowym wywołaniem   |
        //   | operacje mają być kolejkowane, planowane, powtarzane | wystarczy zwykła metoda lub lambda (Runnable)    |
        //   | potrzebny dziennik/audyt operacji                    | cofanie jest niemożliwe, a nikt go nie potrzebuje|
        //   | operacje składają się w makra (kilka kroków = jedna) | klasa na każdą drobnostkę rozdyma kod (YAGNI)    |
        // Command to „nazwa dla lambdy” plus opcjonalne undo: gdy cofanie niepotrzebne, nie buduj hierarchii klas.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Command = operacja jako obiekt: execute() [+ undo()]; invoker uruchamia, receiver robi pracę.
     *   • Cofanie = dwa stosy (undo, redo) w Deque; nowa operacja czyści redo; polecenie trafia do historii po SUKCESIE.
     *   • Polecenie zapamiętuje różnicę (co usunięto, jaki był status) W CHWILI execute — nie zgaduje stanu.
     *   • Makro = polecenie z poleceń: wykonuje po kolei, cofa w odwrotnej kolejności, przy błędzie robi rollback.
     *   • Kolejka poleceń = odroczone wykonanie; Runnable/Callable to polecenia z JDK; Executor = invoker.
     *   • Operacje nieodwracalne: oznacz je, skompensuj albo potwierdź przed wykonaniem; ogranicz długość historii.
     *   • Polecenia jako dane (record) + handler = rozkaz-wiadomość (jeden obsługujący); CQRS: polecenia zmieniają, zapytania czytają.
     *   • Zdarzenie (Observer): „stało się”, wielu słuchaczy. Polecenie: „zrób”, jeden obsługujący.
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie role pełnią Command, Receiver i Invoker w naszym edytorze tekstu?
     *   2. Dlaczego nowe polecenie wykonane po undo czyści stos „ponów”?
     *   3. Dlaczego makro cofa kroki w odwrotnej kolejności?
     *   4. Co wypisze:
     *        TextDocument d = new TextDocument();
     *        History h = new History();
     *        h.execute(new InsertCommand(d, 0, "ab"));
     *        h.execute(new InsertCommand(d, 2, "cd"));
     *        h.undo();
     *        h.execute(new InsertCommand(d, 2, "X"));
     *        System.out.println(d.text() + " " + h.redo());
     *   5. ZNAJDŹ BŁĄD:  class Remove implements Command { Remove(TextDocument d, int from, int to) { ... }
     *        public void undo() { d.insert(from, "?"); } }  — co jest nie tak z cofnięciem?
     *   6. Co wypisze:  Queue<Runnable> q = new ArrayDeque<>(); q.add(() -> System.out.print("A")); q.add(() -> System.out.print("B"));
     *        System.out.println(q.size()); q.poll().run(); System.out.println(q.size());
     *   7. Czym różni się polecenie od zdarzenia? Podaj po jednym przykładzie nazwy.
     *   8. Dlaczego historia cofania musi mieć limit i dlaczego nieudane polecenie nie trafia na stos?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: dwa wstawienia", "Hello world", () -> exercise1());
        Check.equal("ćw. 2: undo, undo, redo", "AB", () -> exercise2());
        Check.equal("ćw. 3: przelew i cofnięcie", "70/30/100/0", () -> exercise3());
        Check.equal("ćw. 4: status z cofnięciem", "WYSLANE,OPLACONE", () -> exercise4());
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "Hello world", () -> solution1());
        Check.equal("ćw. 2 (wzorzec)", "AB", () -> solution2());
        Check.equal("ćw. 3 (wzorzec)", "70/30/100/0", () -> solution3());
        Check.equal("ćw. 4 (wzorzec)", "WYSLANE,OPLACONE", () -> solution4());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): Utwórz {@code TextDocument} i {@code History}. Wykonaj przez historię dwa polecenia
     * {@code InsertCommand}: "Hello" na pozycji 0 oraz " world" na pozycji 5. Zwróć tekst dokumentu.
     * Podpowiedź: {@code history.execute(new InsertCommand(document, pozycja, tekst))}.
     */
    static String exercise1() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): Wstaw przez historię kolejno "A", "B", "C" (każdy na końcu dokumentu), potem wykonaj
     * undo, undo i redo. Zwróć tekst dokumentu.
     * Podpowiedź: pozycja na końcu to {@code document.text().length()}.
     */
    static String exercise2() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie, PRZEPISZ): Poniżej stary kod przelewu wykonywany wprost. Zamień go na polecenie
     * z cofnięciem (użyj {@code transfer(...)} z lekcji albo własnego {@code LambdaCommand}). Konta: nadawca 100 gr,
     * odbiorca 0 gr. Wykonaj przelew 30 gr przez historię, zapisz salda, wykonaj undo, zapisz salda.
     * Zwróć tekst {@code "nadawca/odbiorca/nadawca/odbiorca"}, czyli salda po wykonaniu i po cofnięciu
     * (oczekiwane: 70/30/100/0).
     * <pre>{@code
     * // PRZED:
     * from.withdraw(30);
     * to.deposit(30);
     * // (nie da się cofnąć, zapisać do dziennika ani odłożyć)
     * }</pre>
     * Podpowiedź: {@code history.execute(transfer(from, to, 30))}, potem {@code history.undo()}.
     */
    static String exercise3() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): Napisz polecenie zmiany statusu {@code Command} (klasa anonimowa lub własna),
     * które w execute zapamiętuje poprzedni status, a w undo go przywraca. Użyj go tak: holder zaczyna od NOWE,
     * utwórz polecenie "ustaw WYSLANE", zmień status wprost na OPLACONE (przed wykonaniem!), wykonaj polecenie,
     * zapisz status, wykonaj undo, zapisz status. Zwróć {@code "status1,status2"}.
     * Podpowiedź: poprzedni status czytasz W execute, nie w konstruktorze — inaczej undo przywróci NOWE.
     */
    static String exercise4() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1() {
        TextDocument document = new TextDocument();
        History history = new History();
        history.execute(new InsertCommand(document, 0, "Hello"));
        history.execute(new InsertCommand(document, 5, " world"));
        return document.text();
    }

    static String solution2() {
        TextDocument document = new TextDocument();
        History history = new History();
        for (String fragment : List.of("A", "B", "C")) {
            history.execute(new InsertCommand(document, document.text().length(), fragment));
        }
        history.undo();
        history.undo();
        history.redo();
        return document.text();
    }

    static String solution3() {
        Account from = new Account(100);
        Account to = new Account(0);
        History history = new History();
        history.execute(transfer(from, to, 30));
        String after = from.balance() + "/" + to.balance();
        history.undo();
        return after + "/" + from.balance() + "/" + to.balance();
    }

    static String solution4() {
        StatusHolder holder = new StatusHolder(OrderStatus.NOWE);
        Command command = new SetStatus(holder, OrderStatus.WYSLANE); // gotowa klasa z lekcji robi dokładnie to
        holder.status = OrderStatus.OPLACONE;
        command.execute();
        OrderStatus afterExecute = holder.status;
        command.undo();
        return afterExecute + "," + holder.status;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Command — InsertCommand/DeleteCommand (co zrobić i jak cofnąć); Receiver — TextDocument (faktycznie
     *      zmienia tekst); Invoker — History (uruchamia polecenia, trzyma stosy undo/redo i dziennik).
     *   2. Stan dokumentu rozminął się z tym, który polecenia „ponów” zakładały (np. przesunęły się pozycje) —
     *      ich wykonanie mogłoby uszkodzić tekst.
     *   3. Każdy krok zakłada stan z chwili po poprzednich krokach. Cofnięcie od końca przywraca te stany po kolei;
     *      cofnięcie od początku zostawia późniejszym krokom nieaktualne pozycje.
     *   4. abX false — po undo i nowej operacji stos redo jest czyszczony, więc redo() zwraca false.
     *   5. Cofnięcie wstawia znak zapytania zamiast usuniętego tekstu: polecenie nie zapamiętało usuniętego
     *      fragmentu w execute (jak robi DeleteCommand).
     *   6. Pierwszy println wypisuje 2; potem poll().run() wypisuje A (print, bez końca linii), a drugi println
     *      dopisuje 1. Na ekranie: linia „2”, potem linia „A1”.
     *   7. Zdarzenie: „coś się stało” (czas przeszły, wielu słuchaczy), np. OrderPlaced. Polecenie: „zrób to”
     *      (tryb rozkazujący, jeden obsługujący), np. PlaceOrder.
     *   8. Limit chroni pamięć (polecenia trzymają odbiorców i dane). Nieudane polecenie nie wydarzyło się, więc
     *      cofanie go cofałoby zmianę, której nie było.
     */
    // </editor-fold>
}
