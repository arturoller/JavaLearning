package t25_testing;

import helpers.Check;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Projektowanie pod testowalność — PRZED (trudny kod) / PO (łatwy kod)
 *        (testable design = projekt przyjazny testom; functional core / imperative shell = czysty rdzeń
 *        obliczeniowy / "rozkazująca powłoka" z We/Wy dookoła)
 *
 * W SKRÓCIE:
 *   Klasę da się przetestować albo NIE — zależy to od DECYZJI PROJEKTOWYCH podjętych, zanim jeszcze napiszesz
 *   pierwszy test. Cztery klasyczne błędy: wołanie LocalDate.now()/new Random() wprost w logice, tworzenie
 *   zależności przez `new` W ŚRODKU metody, globalny mutowalny singleton oraz drukowanie wyniku zamiast jego
 *   zwracania. Lekarstwo: wstrzykiwanie zależności (constructor injection), Clock jako parametr, i rozdzielenie
 *   CZYSTEGO obliczenia (functional core) od We/Wy (imperative shell) — obliczenie testujesz wprost, na wartościach.
 *
 * ANALOGIA: silnik i karoseria samochodu.
 *   Silnik (functional core) testujesz na hamowni: dajesz paliwo, mierzysz moc — bez jazdy po mieście, bez ruchu
 *   ulicznego, bez pogody. Karoseria, opony i kierowca (imperative shell) reagują na ŚWIAT ZEWNĘTRZNY. Gdyby silnik
 *   był NA STAŁE wspawany w konkretny samochód, żeby go przetestować, musiałbyś za każdym razem jechać w korku.
 *
 * JAK TO DZIAŁA:
 *   PRZED: void remind(...) { ...LocalDate.now()...; new EmailSenderImpl()...; Counter.INSTANCE...; println(...); }
 *   PO:    ReminderDecision decide(name, days)                         ← functional core: same wartości, zero IO
 *          class Good { Good(EmailSender es, Clock c) { ... } }        ← imperative shell: IO i czas WSTRZYKNIĘTE
 *   Test funkcji czystej: decide("Jan", 400) → porównujesz zwróconą wartość. Bez zegara, bez sieci, bez "atrap".
 *
 * SŁÓWKA:
 *   testable design = projekt przyjazny testom; functional core = czysty rdzeń (obliczenia, bez efektów ubocznych);
 *   imperative shell = rozkazująca powłoka (We/Wy na zewnątrz rdzenia); singleton = pojedynczak (jedna, globalna
 *   instancja); shared mutable state = współdzielony, mutowalny stan; pure function = funkcja czysta (bez efektów
 *   ubocznych, ten sam wynik dla tych samych argumentów); testability smell = zapach nietestowalności.
 *
 * ZOBACZ TEŻ: t25_testing/Testing01Concepts (mini-runner), t25_testing/Testing02TestDoubles (stub/fake/spy/mock
 *             użyte tu jako EmailSender), t07_inheritance_polymorphism/Inherit08Solid (DIP — to ten sam mechanizm),
 *             t17_datetime/DateTime01LocalDateTime (Clock, LocalDate).
 * </pre>
 */
public class Testing03TestableDesign {

    public static void main(String[] args) {
        title("Testing03 — projektowanie pod testowalność");

        badDesignDemo();          // bad design demo = pokaz złego projektu
        whyItHurts();               // why it hurts = dlaczego to boli
        functionalCore();          // functional core = czysty rdzeń obliczeniowy
        imperativeShell();         // imperative shell = "rozkazująca powłoka" (IO wokół rdzenia)
        testTheGoodVersion();      // test the good version = testujemy dobrą wersję
        testabilitySmells();       // testability smells = zapachy nietestowalności
        exercises();                 // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PRZED: CZTERY PUŁAPKI TESTOWALNOŚCI NARAZ
    // =================================================================================================

    /** EmailSenderImpl = "prawdziwa" (na potrzeby lekcji: udawana) wysyłka e-maila — w realu: SMTP/API zewnętrzne. */
    static class EmailSenderImpl {
        void send(String to, String subject) {
            // w prawdziwym projekcie: połączenie sieciowe do serwera pocztowego — tu celowo puste (demo)
        }
    }

    /** ReminderCounter = statyczny singleton (pojedynczak): globalny, WSPÓŁDZIELONY licznik wysłanych przypomnień. */
    static final class ReminderCounter {
        static final ReminderCounter INSTANCE = new ReminderCounter();
        private int count = 0;

        private ReminderCounter() {
        }

        void increment() {
            count++;
        }

        int get() {
            return count;
        }
    }

    /**
     * PRZED — cztery pułapki naraz:
     * (1) LocalDate.now() wprost w logice — wynik zależy od DNIA uruchomienia.
     * (2) new EmailSenderImpl() W ŚRODKU metody — nie da się podstawić testu podwójnego bez zmiany kodu.
     * (3) ReminderCounter.INSTANCE — globalny, mutowalny stan (singleton) współdzielony przez WSZYSTKIE wywołania.
     * (4) println zamiast return — wynik trafia TYLKO na konsolę, test nie ma jak go programowo sprawdzić.
     */
    static class MembershipReminderBad {
        void remind(String memberName, LocalDate joinDate) {
            long days = ChronoUnit.DAYS.between(joinDate, LocalDate.now());     // (1)
            EmailSenderImpl sender = new EmailSenderImpl();                       // (2)
            if (days >= 365) {
                sender.send(memberName, "Twoje członkostwo trwa już rok!");
                ReminderCounter.INSTANCE.increment();                              // (3)
                System.out.println(memberName + ": przypomnienie wysłane (dni: " + days + ")");   // (4)
            } else {
                System.out.println(memberName + ": jeszcze za wcześnie (dni: " + days + ")");      // (4)
            }
        }
    }

    /** 1. Wywołanie PRZED — działa, ale zobacz w sekcji 2, ile z tego NIE da się sprawdzić testem. */
    static void badDesignDemo() {
        section("1. PRZED: cztery pułapki testowalności naraz");

        MembershipReminderBad bad = new MembershipReminderBad();
        bad.remind("Jan Kowalski", LocalDate.now().minusYears(10));  // na pewno ≥ 365 dni, niezależnie od dnia uruchomienia
        // (wynik zależy od uruchomienia — dokładna liczba dni w println się zmienia; to właśnie jest ten problem!)
        bad.remind("Ewa Nowak", LocalDate.now().minusDays(5));        // na pewno < 365 dni
        // (wynik zależy od uruchomienia)

        show("ReminderCounter.INSTANCE.get() po dwóch wywołaniach", ReminderCounter.INSTANCE.get());
        // WYNIK: ReminderCounter.INSTANCE.get() po dwóch wywołaniach → 1

        // Ta jedna liczba (1) JEST deterministyczna, bo tylko jedno z dwóch wywołań przekracza próg 365 dni —
        //   ale to przypadek tego konkretnego przykładu, nie zasługa projektu. Dwa wiersze println powyżej
        //   pokazują RÓŻNY tekst za każdym razem, gdy uruchomisz program innego dnia — i NIC nie da się z tym zrobić
        //   bez zmiany kodu MembershipReminderBad.
    }

    // =================================================================================================
    // 2. DLACZEGO PRZED JEST TRUDNY DO PRZETESTOWANIA
    // =================================================================================================

    static void whyItHurts() {
        section("2. Dlaczego PRZED jest trudny do przetestowania");

        List<String> problems = List.of(
                "LocalDate.now() w środku logiki — nie można ustawić 'testowego dnia'; wynik zmienia się codziennie",
                "new EmailSenderImpl() W ŚRODKU metody — nie da się podstawić testu podwójnego bez zmiany kodu źródłowego",
                "ReminderCounter.INSTANCE (singleton) — globalny, współdzielony stan; testy przestają być Independent (FIRST)",
                "println zamiast return — wynik trafia TYLKO na konsolę; test nie ma jak PROGRAMOWO sprawdzić rezultatu");
        for (int i = 0; i < problems.size(); i++) {
            System.out.println("   " + (i + 1) + ". " + problems.get(i));
        }
        // WYNIK:    1. LocalDate.now() w środku logiki — nie można ustawić 'testowego dnia'; wynik zmienia się codziennie
        // WYNIK:    2. new EmailSenderImpl() W ŚRODKU metody — nie da się podstawić testu podwójnego bez zmiany kodu źródłowego
        // WYNIK:    3. ReminderCounter.INSTANCE (singleton) — globalny, współdzielony stan; testy przestają być Independent (FIRST)
        // WYNIK:    4. println zamiast return — wynik trafia TYLKO na konsolę; test nie ma jak PROGRAMOWO sprawdzić rezultatu

        // PUŁAPKA: żaden z tych czterech problemów nie jest widoczny w kompilacji — kod się kompiluje i "działa".
        //   Ujawniają się dopiero, gdy próbujesz go OTESTOWAĆ — i wtedy jest już drożej to naprawić.
    }

    // =================================================================================================
    // 3. PO — FUNCTIONAL CORE: CZYSTA FUNKCJA decide(...)
    // =================================================================================================

    /** ReminderDecision = WYNIK czystego obliczenia: czy przypomnieć i z jakim komunikatem. Zwykły, niemutowalny rekord. */
    record ReminderDecision(boolean shouldRemind, String message) {
    }

    /**
     * decide = CZYSTA funkcja (functional core = czysty rdzeń): bez zegara, bez sieci, bez efektów ubocznych —
     * te same argumenty ZAWSZE dają ten sam wynik. Testuje się ją wprost, na wartościach, bez żadnych atrap.
     */
    static ReminderDecision decide(String memberName, long daysSinceJoin) {
        if (daysSinceJoin >= 365) {
            return new ReminderDecision(true, memberName + ": przypomnienie wysłane (dni: " + daysSinceJoin + ")");
        }
        return new ReminderDecision(false, memberName + ": jeszcze za wcześnie (dni: " + daysSinceJoin + ")");
    }

    static void functionalCore() {
        section("3. PO — functional core: czysta funkcja decide(...)");

        show("decide(\"Jan\", 400)", decide("Jan", 400));
        show("decide(\"Jan\", 365)", decide("Jan", 365));
        show("decide(\"Jan\", 364)", decide("Jan", 364));
        // WYNIK: decide("Jan", 400) → ReminderDecision[shouldRemind=true, message=Jan: przypomnienie wysłane (dni: 400)]
        // WYNIK: decide("Jan", 365) → ReminderDecision[shouldRemind=true, message=Jan: przypomnienie wysłane (dni: 365)]
        // WYNIK: decide("Jan", 364) → ReminderDecision[shouldRemind=false, message=Jan: jeszcze za wcześnie (dni: 364)]

        note("decide(...) NIE zna zegara, NIE wysyła e-maili, NIC nie wypisuje — tylko liczy i ZWRACA wynik.");
        note("Taką funkcję testuje się WPROST: podajesz argumenty, porównujesz zwróconą wartość — żadnych testów podwójnych!");
        // WYNIK:    ℹ decide(...) NIE zna zegara, NIE wysyła e-maili, NIC nie wypisuje — tylko liczy i ZWRACA wynik.
        // WYNIK:    ℹ Taką funkcję testuje się WPROST: podajesz argumenty, porównujesz zwróconą wartość — żadnych testów podwójnych!
    }

    // =================================================================================================
    // 4. PO — IMPERATIVE SHELL: MembershipReminderGood
    // =================================================================================================

    /** EmailSender = interfejs (wysyłacz e-maili) — MOŻNA podstawić test podwójny, w przeciwieństwie do (2) z sekcji 1. */
    interface EmailSender {
        void send(String to, String subject);
    }

    /**
     * MembershipReminderGood = imperative shell (rozkazująca powłoka): TU i TYLKO TU jest We/Wy (EmailSender)
     * i czas (Clock) — oba WSTRZYKNIĘTE przez konstruktor. Sama decyzja liczona jest w czystej funkcji decide(...).
     */
    static class MembershipReminderGood {
        private final EmailSender emailSender;
        private final Clock clock;

        MembershipReminderGood(EmailSender emailSender, Clock clock) {
            this.emailSender = emailSender;
            this.clock = clock;
        }

        ReminderDecision remind(String memberName, LocalDate joinDate) {
            long days = ChronoUnit.DAYS.between(joinDate, LocalDate.now(clock));
            ReminderDecision decision = decide(memberName, days);
            if (decision.shouldRemind()) {
                emailSender.send(memberName, "Twoje członkostwo trwa już rok!");
            }
            return decision;
        }
    }

    static void imperativeShell() {
        section("4. PO — imperative shell: MembershipReminderGood (Clock + EmailSender wstrzyknięte)");

        List<String> sentTo = new ArrayList<>();
        EmailSender spyEmail = (to, subject) -> sentTo.add(to + " | " + subject);   // spy = szpieg (Testing02)

        LocalDate today = LocalDate.of(2026, 6, 1);
        Clock fixedClock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        MembershipReminderGood good = new MembershipReminderGood(spyEmail, fixedClock);

        LocalDate joinDate = today.minusDays(400);    // dokładnie 400 dni przed 'today' — bez ręcznego liczenia kalendarza
        ReminderDecision decision = good.remind("Piotr Zieliński", joinDate);
        show("decision", decision);
        showEach("sentTo (spy)", sentTo);
        // WYNIK: decision → ReminderDecision[shouldRemind=true, message=Piotr Zieliński: przypomnienie wysłane (dni: 400)]
        // WYNIK: sentTo (spy) (liczba elementów: 1):
        // WYNIK:    • Piotr Zieliński | Twoje członkostwo trwa już rok!

        note("Żadnego LocalDate.now(), żadnego 'new EmailSenderImpl()', żadnego singletona, żadnego println z decyzją —");
        note("wszystko 'niepewne' weszło przez konstruktor i da się PODMIENIĆ w teście (patrz sekcja 5).");
        // WYNIK:    ℹ Żadnego LocalDate.now(), żadnego 'new EmailSenderImpl()', żadnego singletona, żadnego println z decyzją —
        // WYNIK:    ℹ wszystko 'niepewne' weszło przez konstruktor i da się PODMIENIĆ w teście (patrz sekcja 5).
    }

    // =================================================================================================
    // 5. TEST WERSJI PO — MINI-RUNNER (kopia z Testing01, w skróconej wersji)
    // =================================================================================================

    /** TestCase = przypadek testowy: nazwa + treść (szczegóły w Testing01Concepts, sekcja 5). */
    record TestCase(String name, Runnable body) {
    }

    static void assertEquals(String message, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " — oczekiwano: " + expected + ", jest: " + actual);
        }
    }

    static void assertTrue(String message, boolean condition) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static int runSuite(List<TestCase> tests) {
        int passed = 0;
        int failed = 0;
        for (TestCase test : tests) {
            try {
                test.body().run();
                System.out.println("   ✔ " + test.name());
                passed++;
            } catch (AssertionError e) {
                System.out.println("   ✘ " + test.name() + " — " + e.getMessage());
                failed++;
            }
        }
        note("wynik: " + passed + " OK, " + failed + " BŁĄD");
        return failed;
    }

    /** 5. Cztery testy: dwa na czystej funkcji decide(...), dwa na całej klasie MembershipReminderGood. */
    static void testTheGoodVersion() {
        section("5. Testujemy wersję PO");

        LocalDate today = LocalDate.of(2026, 6, 1);
        Clock fixedClock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);

        List<TestCase> tests = List.of(
                new TestCase("decide: 365 dni → przypomnienie", () -> assertTrue("shouldRemind", decide("X", 365).shouldRemind())),
                new TestCase("decide: 364 dni → za wcześnie", () -> assertTrue("!shouldRemind", !decide("X", 364).shouldRemind())),
                new TestCase("MembershipReminderGood: stary członek → jeden e-mail", () -> {
                    List<String> sent = new ArrayList<>();
                    MembershipReminderGood good = new MembershipReminderGood((to, subject) -> sent.add(to), fixedClock);
                    good.remind("Ala", today.minusDays(400));
                    assertEquals("liczba e-maili", 1, sent.size());
                }),
                new TestCase("MembershipReminderGood: nowy członek → brak e-maila", () -> {
                    List<String> sent = new ArrayList<>();
                    MembershipReminderGood good = new MembershipReminderGood((to, subject) -> sent.add(to), fixedClock);
                    good.remind("Bartek", today.minusDays(10));
                    assertTrue("brak e-maili", sent.isEmpty());
                }));
        runSuite(tests);
        // WYNIK:    ✔ decide: 365 dni → przypomnienie
        // WYNIK:    ✔ decide: 364 dni → za wcześnie
        // WYNIK:    ✔ MembershipReminderGood: stary członek → jeden e-mail
        // WYNIK:    ✔ MembershipReminderGood: nowy członek → brak e-maila
        // WYNIK:    ℹ wynik: 4 OK, 0 BŁĄD

        // DOBRA PRAKTYKA: zauważ, że ŻADEN z tych czterech testów nie czeka na sieć, nie dotyka dysku i da ten
        //   sam wynik o KAŻDEJ porze dnia i roku — bo fixedClock i spy zastąpiły "prawdziwy" świat zewnętrzny.
    }

    // =================================================================================================
    // 6. LISTA KONTROLNA: ZAPACHY NIETESTOWALNOŚCI
    // =================================================================================================

    static void testabilitySmells() {
        section("6. Lista kontrolna: zapachy nietestowalności (testability smells)");

        List<String> smells = List.of(
                "bezpośrednie LocalDate.now()/Instant.now()/new Random() w logice biznesowej → wstrzyknij Clock/Random/Supplier",
                "`new KonkretnaKlasa()` zależności W ŚRODKU metody/konstruktora → wstrzyknij interfejs przez konstruktor",
                "statyczny singleton z mutowalnym stanem (X.INSTANCE) → przekaż instancję jako zwykłą zależność",
                "metoda void, która tylko wypisuje wynik (println) → zwróć wartość, wypisywanie zostaw wywołującemu",
                "mieszanie obliczeń z We/Wy w jednej metodzie → rozdziel: functional core (obliczenia) / imperative shell (IO)",
                "metoda, której nie da się wywołać bez uruchomienia całej aplikacji (baza, serwer) → wydziel logikę osobno",
                "zbyt długa lista parametrów konstruktora ukrywająca zbyt wiele odpowiedzialności → SRP (t27_clean_code_pitfalls)");
        for (int i = 0; i < smells.size(); i++) {
            System.out.println("   " + (i + 1) + ". " + smells.get(i));
        }
        // WYNIK:    1. bezpośrednie LocalDate.now()/Instant.now()/new Random() w logice biznesowej → wstrzyknij Clock/Random/Supplier
        // WYNIK:    2. `new KonkretnaKlasa()` zależności W ŚRODKU metody/konstruktora → wstrzyknij interfejs przez konstruktor
        // WYNIK:    3. statyczny singleton z mutowalnym stanem (X.INSTANCE) → przekaż instancję jako zwykłą zależność
        // WYNIK:    4. metoda void, która tylko wypisuje wynik (println) → zwróć wartość, wypisywanie zostaw wywołującemu
        // WYNIK:    5. mieszanie obliczeń z We/Wy w jednej metodzie → rozdziel: functional core (obliczenia) / imperative shell (IO)
        // WYNIK:    6. metoda, której nie da się wywołać bez uruchomienia całej aplikacji (baza, serwer) → wydziel logikę osobno
        // WYNIK:    7. zbyt długa lista parametrów konstruktora ukrywająca zbyt wiele odpowiedzialności → SRP (t27_clean_code_pitfalls)
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Testowalność to decyzja PROJEKTOWA — podejmowana przed napisaniem pierwszego testu.
     *   • Cztery pułapki: now()/Random() wprost, `new` zależności w środku metody, singleton, println zamiast return.
     *   • Lekarstwo: constructor injection (Clock, EmailSender jako parametry konstruktora).
     *   • functional core (czyste obliczenia, np. decide(...)) — testujesz WPROST na wartościach.
     *   • imperative shell (We/Wy, np. MembershipReminderGood) — testujesz przez testy podwójne (Testing02).
     *   • Zamiast globalnego licznika (singleton) — zwracaj wartości i licz je PO STRONIE wywołującego.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego println w MembershipReminderBad.remind(...) jest problemem, skoro program działa poprawnie?
     *   2. Co wypisze (niezależnie od dnia uruchomienia)?
     *          System.out.println(decide("Ola", 30).shouldRemind());
     *   3. ZNAJDŹ BŁĄD — ta "refaktoryzacja" NADAL jest trudna do przetestowania. Dlaczego?
     *          static class MembershipReminderAlmostGood {
     *              private final EmailSender emailSender = new EmailSenderImpl2();
     *              ReminderDecision remind(String name, LocalDate joinDate) { ... }
     *          }
     *   4. Dlaczego decide(String, long) NIE potrzebuje żadnego testu podwójnego (stuba/spy'a/mocka), żeby go przetestować?
     *   5. Jaki problem wprowadza ReminderCounter.INSTANCE, gdyby w jednym uruchomieniu programu (albo zestawie
     *      testów) wywołać remind(...) wiele razy w RÓŻNEJ kolejności w różnych testach?
     *   6. Które trzy zależności MembershipReminderGood dostaje przez konstruktor, a której NIE potrzebuje wcale
     *      (bo jest przekazywana jako zwykły argument metody remind)?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");

        Check.equal("ćw. 1a: classify(0)", "NOWY", () -> exercise1(0));
        Check.equal("ćw. 1b: classify(29)", "NOWY", () -> exercise1(29));
        Check.equal("ćw. 1c: classify(30)", "STALY", () -> exercise1(30));
        Check.equal("ćw. 1d: classify(364)", "STALY", () -> exercise1(364));
        Check.equal("ćw. 1e: classify(365)", "WETERAN", () -> exercise1(365));
        Check.equal("ćw. 2a: po roku → true", true, () -> exercise2(LocalDate.of(2020, 1, 1),
                Clock.fixed(LocalDate.of(2021, 6, 1).atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)));
        Check.equal("ćw. 2b: przed rokiem → false", false, () -> exercise2(LocalDate.of(2020, 1, 1),
                Clock.fixed(LocalDate.of(2020, 6, 1).atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)));
        Check.equal("ćw. 3: dwie wiadomości", List.of("A: przypomnienie wysłane (dni: 400)", "B: jeszcze za wcześnie (dni: 10)"),
                () -> exercise3(List.of("A", "B"),
                        List.of(FIXED_TODAY.minusDays(400), FIXED_TODAY.minusDays(10)), FIXED_CLOCK_EX));
        Check.equal("ćw. 4: liczba przypomnień w liście decyzji", 2, () -> exercise4(List.of(
                decide("A", 400), decide("B", 10), decide("C", 365), decide("D", 364))));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec, 0)", "NOWY", () -> solution1(0));
        Check.equal("ćw. 1 (wzorzec, 365)", "WETERAN", () -> solution1(365));
        Check.equal("ćw. 2a (wzorzec)", true, () -> solution2(LocalDate.of(2020, 1, 1),
                Clock.fixed(LocalDate.of(2021, 6, 1).atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)));
        Check.equal("ćw. 2b (wzorzec)", false, () -> solution2(LocalDate.of(2020, 1, 1),
                Clock.fixed(LocalDate.of(2020, 6, 1).atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)));
        Check.equal("ćw. 3 (wzorzec)", List.of("A: przypomnienie wysłane (dni: 400)", "B: jeszcze za wcześnie (dni: 10)"),
                () -> solution3(List.of("A", "B"),
                        List.of(FIXED_TODAY.minusDays(400), FIXED_TODAY.minusDays(10)), FIXED_CLOCK_EX));
        Check.equal("ćw. 4 (wzorzec)", 2, () -> solution4(List.of(
                decide("A", 400), decide("B", 10), decide("C", 365), decide("D", 364))));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
    }

    static final LocalDate FIXED_TODAY = LocalDate.of(2026, 6, 1);
    static final Clock FIXED_CLOCK_EX = Clock.fixed(FIXED_TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);

    /**
     * ĆWICZENIE 1 (łatwe): czysta funkcja (functional core) klasyfikująca staż członkostwa: "NOWY" (mniej niż 30 dni),
     * "STALY" (30–364 dni), "WETERAN" (365 dni lub więcej).
     */
    static String exercise1(long daysSinceJoin) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie, „PRZEPISZ”): stara wersja była nie do przetestowania:
     * <pre>{@code
     * boolean isLongTimeMemberOld(LocalDate joinDate) {
     *     long days = ChronoUnit.DAYS.between(joinDate, LocalDate.now());   // LocalDate.now() — nie do opanowania w teście!
     *     return days >= 365;
     * }
     * }</pre>
     * Przepisz ją tak, by przyjmowała Clock jako parametr i używała {@code LocalDate.now(clock)} zamiast
     * {@code LocalDate.now()} — dzięki temu w teście podstawiasz Clock.fixed(...) i wynik jest deterministyczny.
     */
    static boolean exercise2(LocalDate joinDate, Clock clock) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): dla równoległych list names i joinDates (ten sam indeks = ta sama osoba),
     * zbuduj JEDEN MembershipReminderGood z podanym clock i "cichym" EmailSender (lambda, która nic nie robi),
     * wywołaj remind(...) dla każdej pary i zwróć listę komunikatów (decision.message()) w kolejności wejściowej.
     */
    static List<String> exercise3(List<String> names, List<LocalDate> joinDates, Clock clock) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (średnie): policz, ile decyzji z listy ma shouldRemind() == true — BEZ żadnego globalnego
     * licznika (jak ReminderCounter z sekcji 1) — po prostu zlicz elementy listy WYNIKÓW.
     */
    static int exercise4(List<ReminderDecision> decisions) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(long daysSinceJoin) {
        if (daysSinceJoin >= 365) {
            return "WETERAN";
        }
        if (daysSinceJoin >= 30) {
            return "STALY";
        }
        return "NOWY";
    }

    static boolean solution2(LocalDate joinDate, Clock clock) {
        long days = ChronoUnit.DAYS.between(joinDate, LocalDate.now(clock));
        return days >= 365;
    }

    static List<String> solution3(List<String> names, List<LocalDate> joinDates, Clock clock) {
        MembershipReminderGood good = new MembershipReminderGood((to, subject) -> {
        }, clock);
        List<String> messages = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            messages.add(good.remind(names.get(i), joinDates.get(i)).message());
        }
        return messages;
    }

    static int solution4(List<ReminderDecision> decisions) {
        int count = 0;
        for (ReminderDecision decision : decisions) {
            if (decision.shouldRemind()) {
                count++;
            }
        }
        return count;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo test nie ma jak PROGRAMOWO sprawdzić tego, co poszło tylko na konsolę — musiałby ręcznie czytać
     *      wydruk (a to łamie Self-validating z FIRST, Testing01). Zwrócona wartość da się porównać kodem.
     *   2. true — decide(...) jest funkcją czystą: daysSinceJoin=30 daje zawsze ten sam wynik, niezależnie od dnia.
     *   3. `new EmailSenderImpl2()` nadal jest tworzony W ŚRODKU klasy (tu: w inicjalizatorze pola) zamiast być
     *      wstrzyknięty przez konstruktor — nie da się podstawić spy'a/mocka bez modyfikacji kodu źródłowego.
     *   4. Bo NIE MA żadnych zależności — przyjmuje proste wartości (String, long) i zwraca wartość. Nie ma czego
     *      podmieniać: to jest właśnie functional core.
     *   5. Testy przestają być Independent (FIRST): licznik "pamięta" wywołania z POPRZEDNICH testów, więc wynik
     *      zależy od KOLEJNOŚCI uruchamiania testów — coś, co powinno być niemożliwe.
     *   6. Przez konstruktor: emailSender i clock. LocalDate joinDate NIE jest zależnością środowiska (jak czas
     *      systemowy czy sieć) — to zwykła DANA WEJŚCIOWA, więc trafia jako parametr metody remind(...).
     */
    // </editor-fold>
}
