package t32_junit_mockito;

import helpers.Check;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.platform.engine.Filter;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TagFilter;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntUnaryOperator;

import static helpers.Console.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * <pre>
 * TEMAT: JUnit 5 — podstawy prawdziwego frameworka testowego
 *        (framework = szkielet: gotowa biblioteka, która sama woła NASZ kod)
 *
 * W SKRÓCIE:
 *   W t25_testing pisaliśmy asercje ręcznie (if + throw). JUnit 5 robi to za nas: sam znajduje metody
 *   z adnotacją {@code @Test}, uruchamia każdą osobno, łapie wyjątki i składa raport „zaliczone / niezaliczone”.
 *   Ta lekcja: budowa testu, asercje, cykl życia, grupowanie, pomijanie i uruchamianie testów.
 *
 * ANALOGIA:
 *   Egzamin na prawo jazdy. Egzaminator (JUnit) ma listę zadań (metody testowe). Każde zadanie zaczyna od
 *   ustawienia auta na starcie (przygotowanie), patrzy, co robisz (działanie), i odhacza wynik (sprawdzenie).
 *   Oblanie jednego zadania nie przerywa egzaminu — na końcu dostajesz protokół z listą wszystkich punktów.
 *
 * JAK TO DZIAŁA:
 *   1. Piszemy klasę testową: metody z {@code @Test}, w środku asercje (assertEquals, assertThrows, ...).
 *   2. Silnik JUnit Jupiter (engine = silnik) wyszukuje takie metody (discovery = wykrywanie).
 *   3. Dla KAŻDEGO testu tworzy NOWY obiekt klasy testowej i woła kolejno: {@code @BeforeEach}, test, {@code @AfterEach}.
 *   4. Asercja, która się nie zgadza, rzuca AssertionFailedError → test „niezaliczony” (✘), reszta biegnie dalej.
 *   5. Platforma JUnit (Launcher = „odpalacz”) zbiera wyniki i przekazuje je słuchaczom (listener = słuchacz):
 *      IntelliJ rysuje zielone/czerwone paski, Maven Surefire pisze raport, a w tym kursie — nasz main wypisuje linie.
 *
 *      warstwa                  co robi                               kto jej używa
 *      ----------------------   -----------------------------------   ---------------------------
 *      JUnit Jupiter API        adnotacje i asercje (@Test, assert*)  my, w kodzie testów
 *      JUnit Jupiter Engine     wykrywa i wykonuje testy Jupitera     platforma
 *      JUnit Platform Launcher  uruchamia silniki, zbiera wyniki      IntelliJ, Maven, nasz main
 *
 *   JAK URUCHAMIAMY TESTY W TEJ LEKCJI: klasy testowe są zagnieżdżone (static class ...Tests) w tej lekcji,
 *   a metoda runTests(...) na dole pliku woła Launcher i wypisuje po jednej linii na test:
 *   „✔ nazwa” (zaliczony), „✘ nazwa — Wyjątek: pierwsza linia komunikatu” (niezaliczony), „⊘ ...” (pominięty).
 *   W IntelliJ możesz też kliknąć zieloną strzałkę obok klasy ...Tests albo pojedynczej metody — zadziała tak samo.
 *
 * SŁÓWKA:
 *   assert = zapewniać, twierdzić (asercja = sprawdzenie); expected = oczekiwany; actual = faktyczny;
 *   before/after = przed/po; each = każdy; all = wszystkie; nested = zagnieżdżony; disabled = wyłączony;
 *   tag = etykieta; assumption = założenie; display name = nazwa wyświetlana; suite = zestaw testów;
 *   arrange-act-assert = przygotuj–działaj–sprawdź; given-when-then = mając–gdy–wtedy
 *
 * ZOBACZ TEŻ: t25_testing/Testing01Concepts (po co testy, ręczne asercje), t25_testing/Testing03TestableDesign
 *             (kod łatwy do testowania), t32_junit_mockito/JUnit02Parameterized (jeden test — wiele danych),
 *             t30_build_modules/Build06QualityToolsJavadoc (Maven, pokrycie kodu)
 * </pre>
 */
public class JUnit01Basics {

    public static void main(String[] args) {
        title("JUnit01 — podstawy JUnit 5");

        anatomy();            // anatomy = anatomia, budowa testu
        assertions();         // assertions = asercje
        groupedAssertions();  // grouped assertions = asercje zgrupowane
        lifecycle();          // lifecycle = cykl życia
        skippingAndTags();    // skipping and tags = pomijanie i etykiety
        nestedGroups();       // nested groups = zagnieżdżone grupy
        independence();       // independence = niezależność testów
        whereTestsLive();     // where tests live = gdzie mieszkają testy
        exercises();          // exercises = ćwiczenia
    }

    // =================================================================================================
    // KOD PRODUKCYJNY — to testujemy
    // =================================================================================================

    /**
     * Proste konto bankowe (Account = konto). Kwoty w całych złotych, żeby nie zaciemniać przykładów
     * (pieniądze w prawdziwym kodzie: BigDecimal — t15_numbers/Numbers01BigDecimal).
     */
    static class Account {
        private final String owner;        // owner = właściciel
        private int balance;               // balance = saldo
        private String lastOperation;      // last operation = ostatnia operacja (null, dopóki nic nie zrobiono)

        Account(String owner) {
            if (owner == null || owner.isBlank()) {   // isBlank = czy pusty lub same spacje (Java 11+)
                throw new IllegalArgumentException("właściciel nie może być pusty");
            }
            this.owner = owner;
        }

        String owner() { return owner; }
        int balance() { return balance; }
        String lastOperation() { return lastOperation; }

        void deposit(int amount) {             // deposit = wpłać; amount = kwota
            requirePositive(amount);
            balance += amount;
            lastOperation = "wpłata " + amount;
        }

        void withdraw(int amount) {            // withdraw = wypłać
            requirePositive(amount);
            if (amount > balance) {
                throw new IllegalStateException("brak środków: saldo " + balance + ", żądano " + amount);
            }
            balance -= amount;
            lastOperation = "wypłata " + amount;
        }

        private static void requirePositive(int amount) {   // require positive = wymagaj dodatniej
            if (amount <= 0) {
                throw new IllegalArgumentException("kwota musi być dodatnia: " + amount);
            }
        }
    }

    /** Koszt dostawy (delivery fee = opłata za dostawę): od 200 zł darmowa, poniżej 15 zł, ujemna wartość = błąd. */
    static int deliveryFee(int cartValue) {   // cart value = wartość koszyka
        if (cartValue < 0) {
            throw new IllegalArgumentException("wartość koszyka nie może być ujemna: " + cartValue);
        }
        return cartValue >= 200 ? 0 : 15;
    }

    // =================================================================================================
    // 1. ANATOMIA TESTU
    // =================================================================================================

    /**
     * Test = zwykła metoda bez parametrów, zwracająca void, oznaczona {@code @Test}. Nie musi być publiczna
     * (w JUnit 5 wystarczy widoczność pakietowa), nie może być prywatna ani statyczna.
     * Środek układamy w trzy akapity: PRZYGOTUJ (arrange/given) — DZIAŁAJ (act/when) — SPRAWDŹ (assert/then).
     */
    static void anatomy() {
        section("1. Anatomia testu — @Test, przygotuj/działaj/sprawdź, @DisplayName");

        RunResult result = runTests(AnatomyTests.class);   // run tests = uruchom testy; result = wynik
        // WYNIK: ✔ nowe konto ma saldo 0
        // WYNIK: ✔ withdraw_moreThanBalance_throwsIllegalState()
        // WYNIK: ✔ wpłata 100 zł zwiększa saldo do 100
        // WYNIK: znalezione 3, zaliczone 3, niezaliczone 0, pominięte 0, przerwane 0
        show("czy wszystko zaliczone", result.failed() == 0);
        // WYNIK: czy wszystko zaliczone → true

        // DOBRA PRAKTYKA: nazwa testu mówi, CO sprawdza i W JAKICH warunkach — gdy test padnie za pół roku,
        // sama nazwa w raporcie ma powiedzieć, co się zepsuło. Dwa popularne style:
        //   • metoda_warunek_oczekiwanie: withdraw_moreThanBalance_throwsIllegalState (bez @DisplayName raport
        //     pokazuje nazwę metody z nawiasami — patrz druga linia wyniku),
        //   • zdanie po polsku w @DisplayName — czytelne dla każdego, także w IntelliJ.
        // PUŁAPKA: nazwy test1(), test2(), testDeposit() nic nie mówią — po czerwonym pasku i tak musisz czytać kod.
    }

    /** Trzy testy konta. Kolejność wypisywania: alfabetycznie po nazwie wyświetlanej (ustawia to runTests). */
    static class AnatomyTests {

        @Test
        @DisplayName("nowe konto ma saldo 0")
        void newAccountHasZeroBalance() {
            // przygotuj (arrange / given)
            Account account = new Account("Ola");
            // działaj (act / when) — tu „działaniem” jest samo odczytanie salda
            int balance = account.balance();
            // sprawdź (assert / then)
            assertEquals(0, balance);   // assertEquals = sprawdź równość (oczekiwane, faktyczne) — NAJPIERW oczekiwane!
        }

        @Test
        @DisplayName("wpłata 100 zł zwiększa saldo do 100")
        void depositIncreasesBalance() {
            Account account = new Account("Ola");        // given = mając konto Oli
            account.deposit(100);                        // when = gdy wpłaci 100 zł
            assertEquals(100, account.balance());        // then = wtedy saldo wynosi 100
        }

        @Test
        void withdraw_moreThanBalance_throwsIllegalState() {
            Account account = new Account("Ola");
            account.deposit(50);
            assertThrows(IllegalStateException.class, () -> account.withdraw(80));   // assertThrows = sprawdź, że rzuca
        }
    }

    // =================================================================================================
    // 2. ASERCJE JUnit
    // =================================================================================================

    /**
     * Najważniejsze asercje z klasy {@code org.junit.jupiter.api.Assertions} (importujemy je statycznie).
     * Każda nieudana asercja rzuca AssertionFailedError i KOŃCZY test — dalsze linie testu się nie wykonają.
     */
    static void assertions() {
        section("2. Asercje — assertEquals, assertTrue/False, assertNull, assertThrows");

        runTests(AssertionsTests.class);
        // WYNIK: ✔ assertEquals z komunikatem
        // WYNIK: ✘ assertEquals: oczekiwane 70, jest 50 — AssertionFailedError: saldo po wypłacie ==> expected: <70> but was: <50>
        // WYNIK: ✔ assertNull / assertNotNull
        // WYNIK: ✔ assertThrows zwraca wyjątek — sprawdzamy komunikat
        // WYNIK: ✔ assertTrue / assertFalse
        // WYNIK: ✘ odwrócone argumenty — mylący komunikat — AssertionFailedError: expected: <50> but was: <70>
        // WYNIK: znalezione 6, zaliczone 4, niezaliczone 2, pominięte 0, przerwane 0

        // Komunikat porażki ma postać:  <twój opis> ==> expected: <oczekiwane> but was: <faktyczne>
        // (expected = oczekiwano, but was = a było). Dlatego KOLEJNOŚĆ argumentów ma znaczenie.
        // PUŁAPKA: assertEquals(account.balance(), 70) — test i tak wykryje błąd, ale raport kłamie:
        // „oczekiwano 50, a było 70”, choć to KOD dał 50. Zawsze: assertEquals(OCZEKIWANE, FAKTYCZNE).
        // PUŁAPKA: assertEquals(0.3, 0.1 + 0.2) nie przejdzie (błąd zaokrąglenia double) — użyj wersji z deltą:
        // assertEquals(0.3, 0.1 + 0.2, 1e-9)  (delta = dopuszczalna różnica).
        // DOBRA PRAKTYKA: kosztowny opis (np. sklejany z wielu pól) podawaj jako lambdę: assertEquals(a, b, () -> "...") —
        // wtedy tekst powstaje tylko wtedy, gdy test faktycznie padnie.
    }

    /** Dwa testy padają CELOWO — żeby zobaczyć, jak wygląda komunikat porażki. */
    static class AssertionsTests {

        @Test
        @DisplayName("assertEquals z komunikatem")
        void equalsWithMessage() {
            Account account = new Account("Ola");
            account.deposit(100);
            account.withdraw(30);
            assertEquals(70, account.balance(), "saldo po wypłacie");   // 3. argument = opis pokazywany przy porażce
        }

        @Test
        @DisplayName("assertEquals: oczekiwane 70, jest 50")
        void equalsFailing() {
            Account account = new Account("Ola");
            account.deposit(100);
            account.withdraw(50);                                       // „pomyłka” — wypłaciliśmy 50, nie 30
            assertEquals(70, account.balance(), "saldo po wypłacie");
        }

        @Test
        @DisplayName("odwrócone argumenty — mylący komunikat")
        void swappedArguments() {
            Account account = new Account("Ola");
            account.deposit(100);
            account.withdraw(50);
            assertEquals(account.balance(), 70);                         // ŹLE: faktyczne na miejscu oczekiwanego
        }

        @Test
        @DisplayName("assertTrue / assertFalse")
        void trueFalse() {
            Account account = new Account("Ola");
            assertTrue(account.owner().startsWith("O"), "imię zaczyna się od O");   // assertTrue = sprawdź, że prawda
            assertFalse(account.balance() > 0);                                      // assertFalse = sprawdź, że fałsz
        }

        @Test
        @DisplayName("assertNull / assertNotNull")
        void nullChecks() {
            Account account = new Account("Ola");
            assertNull(account.lastOperation());        // assertNull = sprawdź, że null; jeszcze nic się nie wydarzyło
            account.deposit(10);
            assertNotNull(account.lastOperation());     // assertNotNull = sprawdź, że nie null
            assertEquals("wpłata 10", account.lastOperation());
        }

        @Test
        @DisplayName("assertThrows zwraca wyjątek — sprawdzamy komunikat")
        void throwsReturnsException() {
            Account account = new Account("Ola");
            // assertThrows(typ, kod) → jeśli kod NIE rzuci (albo rzuci wyjątek innego typu) — test pada;
            // jeśli rzuci — dostajemy ten wyjątek i możemy sprawdzić jego komunikat.
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> account.deposit(-5));
            assertEquals("kwota musi być dodatnia: -5", e.getMessage());
            // Uwaga: assertThrows akceptuje też PODKLASY podanego typu; dokładny typ sprawdza assertThrowsExactly.
        }
    }

    // =================================================================================================
    // 3. assertAll I assertTimeoutPreemptively
    // =================================================================================================

    /**
     * {@code assertAll} wykonuje WSZYSTKIE przekazane sprawdzenia i zgłasza wszystkie porażki naraz
     * (MultipleFailuresError = błąd wielokrotny). Bez niego pierwsza nieudana asercja przerywa test,
     * a o kolejnych błędach dowiadujesz się dopiero po poprawieniu pierwszego.
     */
    static void groupedAssertions() {
        section("3. assertAll (wszystkie błędy naraz) i assertTimeoutPreemptively");

        runTests(GroupedTests.class);
        // WYNIK: ✘ assertAll: dwa pola złe — MultipleFailuresError: konto po operacjach (2 failures)
        // WYNIK: ✔ assertAll: wszystko się zgadza
        // WYNIK: ✔ assertTimeoutPreemptively: szybka operacja
        // WYNIK: ✘ assertTimeoutPreemptively: za wolno — AssertionFailedError: execution timed out after 50 ms
        // WYNIK: znalezione 4, zaliczone 2, niezaliczone 2, pominięte 0, przerwane 0

        // „(2 failures)” = 2 porażki; dalsze linie komunikatu (tu pominięte) wymieniają obie po kolei.
        // PUŁAPKA: assertTimeoutPreemptively (preemptively = z wywłaszczeniem) uruchamia kod w INNYM wątku i przerywa go
        // po czasie. Wszystko, co zależy od wątku (ThreadLocal, transakcje bazy w Springu), tam nie działa.
        // Zwykłe assertTimeout czeka, aż kod się skończy, i dopiero potem porównuje czas — bezpieczniejsze, ale wolne.
        // DOBRA PRAKTYKA: limity czasu w testach jednostkowych stosuj rzadko i z dużym zapasem — wolny komputer
        // albo obciążony serwer CI dają „migające” testy (flaky = niestabilny: raz ✔, raz ✘).
    }

    static class GroupedTests {

        @Test
        @DisplayName("assertAll: wszystko się zgadza")
        void allGood() {
            Account account = new Account("Ola");
            account.deposit(100);
            assertAll("konto po wpłacie",                                   // assertAll = sprawdź wszystkie
                    () -> assertEquals("Ola", account.owner()),
                    () -> assertEquals(100, account.balance()),
                    () -> assertEquals("wpłata 100", account.lastOperation()));
        }

        @Test
        @DisplayName("assertAll: dwa pola złe")
        void twoWrong() {
            Account account = new Account("Ola");
            account.deposit(100);
            account.withdraw(40);
            assertAll("konto po operacjach",
                    () -> assertEquals("Ola", account.owner()),                 // ✔
                    () -> assertEquals(70, account.balance()),                  // ✘ jest 60
                    () -> assertEquals("wypłata 30", account.lastOperation())); // ✘ jest „wypłata 40”
        }

        @Test
        @DisplayName("assertTimeoutPreemptively: szybka operacja")
        void fastEnough() {
            int fee = assertTimeoutPreemptively(Duration.ofSeconds(2), () -> deliveryFee(150));  // zwraca wynik kodu
            assertEquals(15, fee);
        }

        @Test
        @DisplayName("assertTimeoutPreemptively: za wolno")
        void tooSlow() {
            // Kod „usnąłby” na 5 s, ale po 50 ms JUnit go przerywa i zgłasza porażkę — test nie trwa 5 s.
            assertTimeoutPreemptively(Duration.ofMillis(50), () -> Thread.sleep(5_000));
        }
    }

    // =================================================================================================
    // 4. CYKL ŻYCIA
    // =================================================================================================

    /**
     * {@code @BeforeAll}/{@code @AfterAll} — raz na klasę (metody statyczne), {@code @BeforeEach}/{@code @AfterEach} —
     * przed/po KAŻDYM teście. Kluczowa reguła: JUnit tworzy NOWY obiekt klasy testowej dla każdego testu,
     * więc pola instancji zawsze startują od zera — testy nie dzielą stanu przez pola.
     */
    static void lifecycle() {
        section("4. Cykl życia — @BeforeAll, @BeforeEach, @AfterEach, @AfterAll");

        LifecycleTests.staticCounter = 0;
        runTests(LifecycleTests.class);
        // WYNIK: [BeforeAll] raz, przed wszystkimi testami
        // WYNIK: [konstruktor] nowy obiekt klasy testowej
        // WYNIK: [BeforeEach] świeże konto
        // WYNIK: [test A] licznik w polu = 1, licznik statyczny = 1
        // WYNIK: [AfterEach] sprzątanie
        // WYNIK: ✔ test A
        // WYNIK: [konstruktor] nowy obiekt klasy testowej
        // WYNIK: [BeforeEach] świeże konto
        // WYNIK: [test B] licznik w polu = 1, licznik statyczny = 2
        // WYNIK: [AfterEach] sprzątanie
        // WYNIK: ✔ test B
        // WYNIK: [AfterAll] raz, po wszystkich testach
        // WYNIK: znalezione 2, zaliczone 2, niezaliczone 0, pominięte 0, przerwane 0

        // Licznik w polu = 1 w OBU testach (nowy obiekt), statyczny rośnie (static należy do klasy, nie do obiektu).
        // PUŁAPKA: pole static zmieniane w testach = ukryta zależność między testami (patrz sekcja 7).
        // DOBRA PRAKTYKA: wspólne PRZYGOTOWANIE (np. świeże konto) — w @BeforeEach; drogie zasoby tylko do odczytu
        // (np. uruchomiony serwer testowy) — w @BeforeAll. Adnotacja @TestInstance(Lifecycle.PER_CLASS) zmienia regułę
        // na „jeden obiekt na klasę” (wtedy @BeforeAll nie musi być static) — używaj świadomie.
    }

    /** {@code @TestMethodOrder(OrderAnnotation)} + {@code @Order} — jawna kolejność, tylko dla czytelności demo. */
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    static class LifecycleTests {
        static int staticCounter;        // static counter = licznik statyczny (wspólny dla klasy)
        int instanceCounter;             // instance counter = licznik w polu obiektu
        Account account;

        LifecycleTests() {
            System.out.println("[konstruktor] nowy obiekt klasy testowej");
        }

        @BeforeAll
        static void beforeAll() { System.out.println("[BeforeAll] raz, przed wszystkimi testami"); }

        @BeforeEach
        void setUp() {                   // set up = przygotuj
            account = new Account("Ola");
            System.out.println("[BeforeEach] świeże konto");
        }

        @Test
        @Order(1)
        @DisplayName("test A")
        void testA() {
            instanceCounter++;
            staticCounter++;
            System.out.println("[test A] licznik w polu = " + instanceCounter + ", licznik statyczny = " + staticCounter);
            account.deposit(500);        // zmieniamy konto — test B i tak dostanie nowe
        }

        @Test
        @Order(2)
        @DisplayName("test B")
        void testB() {
            instanceCounter++;
            staticCounter++;
            System.out.println("[test B] licznik w polu = " + instanceCounter + ", licznik statyczny = " + staticCounter);
            assertEquals(0, account.balance());   // 500 z testu A tu NIE dotarło
        }

        @AfterEach
        void tearDown() { System.out.println("[AfterEach] sprzątanie"); }   // tear down = rozebrać, posprzątać

        @AfterAll
        static void afterAll() { System.out.println("[AfterAll] raz, po wszystkich testach"); }
    }

    // =================================================================================================
    // 5. @Disabled, ZAŁOŻENIA I @Tag
    // =================================================================================================

    /**
     * {@code @Disabled("powód")} — test w ogóle się nie uruchamia (pominięty). {@code assumeTrue(warunek)} — test startuje,
     * ale gdy założenie jest fałszywe, zostaje PRZERWANY (aborted), a nie oblany. {@code @Tag("etykieta")} pozwala
     * uruchomić tylko wybraną grupę testów (np. szybkie przy każdej zmianie, wolne raz na noc).
     */
    static void skippingAndTags() {
        section("5. @Disabled, założenia (assumeTrue) i @Tag");

        runTests(SkippingTests.class);
        // WYNIK: ⊘ baza danych dostępna — przerwany: Assumption failed: brak bazy testowej — pomijam
        // WYNIK: ✔ szybki: wpłata
        // WYNIK: ✔ wolny: tysiąc operacji
        // WYNIK: ⊘ wypłata walut — pominięty: czeka na zgłoszenie KONTO-42
        // WYNIK: znalezione 4, zaliczone 2, niezaliczone 0, pominięte 1, przerwane 1

        note("uruchamiam tylko testy z etykietą „szybki”:");
        // WYNIK: ℹ uruchamiam tylko testy z etykietą „szybki”:
        run(SkippingTests.class, true, Map.of(), TagFilter.includeTags("szybki"));   // include tags = dołącz etykiety
        // WYNIK: ✔ szybki: wpłata
        // WYNIK: znalezione 1, zaliczone 1, niezaliczone 0, pominięte 0, przerwane 0

        // PUŁAPKA: @Disabled bez powodu i bez terminu = test martwy na zawsze. Zawsze podaj powód (np. numer zgłoszenia).
        // PUŁAPKA: assumeTrue to nie sposób na „niewygodny” test — przerwany test nie jest zielony, po prostu nic nie
        // sprawdził. Używaj założeń dla warunków ŚRODOWISKA (system operacyjny, dostępna baza, zmienna środowiskowa).
        // Maven: mvn test -Dgroups=szybki (tylko ta etykieta) albo -DexcludedGroups=wolny; IntelliJ: konfiguracja „Tags”.
    }

    static class SkippingTests {

        @Test
        @Tag("szybki")
        @DisplayName("szybki: wpłata")
        void fastDeposit() {
            Account account = new Account("Ola");
            account.deposit(1);
            assertEquals(1, account.balance());
        }

        @Test
        @Tag("wolny")
        @DisplayName("wolny: tysiąc operacji")
        void slowManyOperations() {
            Account account = new Account("Ola");
            for (int i = 0; i < 1000; i++) {
                account.deposit(2);
                account.withdraw(1);
            }
            assertEquals(1000, account.balance());
        }

        @Test
        @Disabled("czeka na zgłoszenie KONTO-42")
        @DisplayName("wypłata walut")
        void currencyWithdrawal() {
            throw new UnsupportedOperationException("jeszcze nie zaimplementowano");
        }

        @Test
        @DisplayName("baza danych dostępna")
        void needsDatabase() {
            // Boolean.getBoolean("x") = czy właściwość systemowa x ma wartość "true" (tu nikt jej nie ustawia → false)
            assumeTrue(Boolean.getBoolean("lekcja.bazaTestowa"), "brak bazy testowej — pomijam");   // assume = załóż
            throw new IllegalStateException("tu łączylibyśmy się z bazą");   // ta linia się nie wykona
        }
    }

    // =================================================================================================
    // 6. @Nested — GRUPOWANIE TESTÓW
    // =================================================================================================

    /**
     * {@code @Nested} = klasa WEWNĘTRZNA (bez static) w klasie testowej. Każda taka klasa to „kontekst”
     * (np. „gdy konto jest puste”) z własnym {@code @BeforeEach}. Przed testem z klasy zagnieżdżonej JUnit woła
     * najpierw {@code @BeforeEach} klasy zewnętrznej, potem wewnętrznej — przygotowanie „narasta” warstwami.
     */
    static void nestedGroups() {
        section("6. @Nested — testy pogrupowane w konteksty");

        runTests(AccountNestedTests.class);
        // WYNIK: ✔ konstruktor odrzuca pustego właściciela
        // WYNIK: ✔ gdy konto jest puste › saldo wynosi 0
        // WYNIK: ✔ gdy konto jest puste › wypłata rzuca IllegalStateException
        // WYNIK: ✔ gdy na koncie jest 100 zł › wypłata 30 zł zostawia 70 zł
        // WYNIK: ✔ gdy na koncie jest 100 zł › wypłata całości zeruje saldo
        // WYNIK: znalezione 5, zaliczone 5, niezaliczone 0, pominięte 0, przerwane 0

        // JUnit wykonuje najpierw testy klasy zewnętrznej, potem kolejne klasy @Nested (tu: alfabetycznie po nazwie).
        // W raporcie (i w IntelliJ) testy układają się w drzewko: kontekst › zachowanie. Czyta się to jak specyfikację.
        // PUŁAPKA: klasa @Nested NIE może być static — klasa statyczna nie jest klasą wewnętrzną, więc JUnit nie
        // potraktuje jej jako kontekstu (i nie miałaby dostępu do pól obiektu zewnętrznego, np. account).
    }

    @DisplayName("Konto")
    static class AccountNestedTests {
        Account account;

        @BeforeEach
        void createAccount() { account = new Account("Ola"); }   // create = utwórz

        @Test
        @DisplayName("konstruktor odrzuca pustego właściciela")
        void rejectsBlankOwner() {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Account("  "));
            assertEquals("właściciel nie może być pusty", e.getMessage());
        }

        @Nested
        @DisplayName("gdy konto jest puste")
        class WhenEmpty {                                      // when empty = gdy puste

            @Test
            @DisplayName("saldo wynosi 0")
            void zeroBalance() { assertEquals(0, account.balance()); }

            @Test
            @DisplayName("wypłata rzuca IllegalStateException")
            void withdrawFails() { assertThrows(IllegalStateException.class, () -> account.withdraw(1)); }
        }

        @Nested
        @DisplayName("gdy na koncie jest 100 zł")
        class WithHundred {                                    // with hundred = ze stówką

            @BeforeEach
            void depositHundred() { account.deposit(100); }    // wołane PO createAccount() klasy zewnętrznej

            @Test
            @DisplayName("wypłata 30 zł zostawia 70 zł")
            void partialWithdraw() {
                account.withdraw(30);
                assertEquals(70, account.balance());
            }

            @Test
            @DisplayName("wypłata całości zeruje saldo")
            void fullWithdraw() {
                account.withdraw(100);
                assertEquals(0, account.balance());
            }
        }
    }

    // =================================================================================================
    // 7. NIEZALEŻNOŚĆ TESTÓW — PUŁAPKA KOLEJNOŚCI
    // =================================================================================================

    /**
     * Domyślna kolejność testów w JUnit 5 jest deterministyczna, ale CELOWO nieoczywista (to NIE jest kolejność
     * w pliku). Test, który działa tylko po innym teście, to bomba z opóźnionym zapłonem. Pokazujemy to, uruchamiając
     * TĘ SAMĄ klasę dwa razy z różnymi porządkami (configuration parameter = parametr konfiguracji).
     */
    static void independence() {
        section("7. Niezależność testów — wspólny stan i kolejność");

        SharedStateTests.HISTORY.clear();
        note("porządek wg nazw metod (MethodName): a_addsEntry, potem b_hasOneEntry");
        // WYNIK: ℹ porządek wg nazw metod (MethodName): a_addsEntry, potem b_hasOneEntry
        run(SharedStateTests.class, true,
                Map.of("junit.jupiter.testmethod.order.default", "org.junit.jupiter.api.MethodOrderer$MethodName"));
        // WYNIK: ✔ zapisuje operację w historii
        // WYNIK: ✔ historia ma dokładnie jeden wpis
        // WYNIK: znalezione 2, zaliczone 2, niezaliczone 0, pominięte 0, przerwane 0

        SharedStateTests.HISTORY.clear();
        note("porządek wg nazw wyświetlanych (DisplayName): „historia...” przed „zapisuje...”");
        // WYNIK: ℹ porządek wg nazw wyświetlanych (DisplayName): „historia...” przed „zapisuje...”
        runTests(SharedStateTests.class);
        // WYNIK: ✘ historia ma dokładnie jeden wpis — AssertionFailedError: expected: <1> but was: <0>
        // WYNIK: ✔ zapisuje operację w historii
        // WYNIK: znalezione 2, zaliczone 1, niezaliczone 1, pominięte 0, przerwane 0

        // Ten sam kod, inny porządek → inny wynik. W prawdziwym projekcie objawia się to tak: „u mnie przechodzi,
        // na serwerze CI pada” albo „pojedynczo przechodzi, w całym zestawie pada”.
        // DOBRA PRAKTYKA: każdy test sam przygotowuje swój świat (@BeforeEach) i nie zostawia śladów (@AfterEach).
        // Żadnych zmiennych static modyfikowanych przez testy. @TestMethodOrder/@Order służą czytelności albo testom
        // „scenariuszowym” — nie łataniu zależności między testami.
    }

    /** ŹLE: dwa testy dzielą statyczną listę — wynik drugiego zależy od tego, czy pierwszy już się wykonał. */
    static class SharedStateTests {
        static final List<String> HISTORY = new ArrayList<>();   // history = historia (wspólna — to błąd!)

        @Test
        @DisplayName("zapisuje operację w historii")
        void a_addsEntry() {                                     // adds entry = dodaje wpis
            HISTORY.add("wpłata 100");
            assertFalse(HISTORY.isEmpty());
        }

        @Test
        @DisplayName("historia ma dokładnie jeden wpis")
        void b_hasOneEntry() {                                   // has one entry = ma jeden wpis
            assertEquals(1, HISTORY.size());                      // zakłada, że a_addsEntry już był — ŹLE
        }
    }

    // =================================================================================================
    // 8. GDZIE MIESZKAJĄ TESTY — MAVEN, src/test/java, IntelliJ
    // =================================================================================================

    /**
     * W normalnym projekcie Maven testy leżą osobno:
     * <pre>{@code
     * src/main/java/pl/sklep/Account.java          ← kod produkcyjny (trafia do jara)
     * src/test/java/pl/sklep/AccountTest.java      ← testy (ten sam pakiet, NIE trafiają do jara)
     * }</pre>
     * Polecenie {@code mvn test} kompiluje oba katalogi i uruchamia wtyczkę Maven Surefire,
     * która przez JUnit Platform Launcher uruchamia klasy o nazwach pasujących do wzorców Test*, *Test, *Tests, *TestCase.
     * Niezaliczony test = przerwane budowanie („BUILD FAILURE”) — błąd nie trafi na produkcję.
     */
    static void whereTestsLive() {
        section("8. Gdzie mieszkają testy — Maven Surefire, src/test/java, IntelliJ");

        // Ten kurs trzyma testy W LEKCJI i uruchamia je z main, bo: (1) cały materiał tematu jest w jednym pliku,
        // (2) weryfikator kursu uruchamia main i porównuje linie WYNIK, (3) widzisz kod i jego testy obok siebie.
        // To wyjątek dydaktyczny — w projekcie testy zawsze w src/test/java, a JUnit jako zależność z zakresem test:
        //   <dependency> org.junit.jupiter : junit-jupiter : 5.11.4 : <scope>test</scope> </dependency>

        RunResult all = runQuietly(AccountNestedTests.class);   // run quietly = uruchom po cichu
        show("ciche uruchomienie (bez listy testów)", all.brief());
        // WYNIK: ciche uruchomienie (bez listy testów) → znalezione 5, zaliczone 5, niezaliczone 0, pominięte 0, przerwane 0

        // IntelliJ: zielona strzałka przy klasie/metodzie testowej (albo Ctrl+Shift+F10 z kursorem w teście) → okno
        // „Run” z drzewkiem testów. Ctrl+Shift+T w klasie produkcyjnej = przejdź do testu / utwórz test.
        // PUŁAPKA: test, który nigdy nie był CZERWONY, mógł nic nie sprawdzać (np. brak asercji). Zepsuj na chwilę kod
        // produkcyjny i zobacz, czy test to wyłapie — dokładnie tak działa sprawdzarka ćwiczenia 3 (mutanty).
        // DOBRA PRAKTYKA: testy jednostkowe mają być szybkie (milisekundy) — wtedy uruchamiasz je po każdej zmianie.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Test = metoda void z @Test (nieprywatna, niestatyczna). Układ: przygotuj → działaj → sprawdź.
     *   • @DisplayName("zdanie po polsku") albo nazwa metoda_warunek_oczekiwanie. Bez test1/test2.
     *   • assertEquals(OCZEKIWANE, FAKTYCZNE, "opis") — kolejność ma znaczenie dla komunikatu; double → z deltą.
     *   • assertTrue/assertFalse, assertNull/assertNotNull, assertThrows(Typ.class, () -> ...) → zwraca wyjątek.
     *   • assertAll("nazwa", () -> ..., () -> ...) — wszystkie porażki w jednym raporcie.
     *   • assertTimeoutPreemptively — inny wątek, przerywa po czasie; limity czasu z dużym zapasem albo wcale.
     *   • Cykl: @BeforeAll (static) → [konstruktor → @BeforeEach → test → @AfterEach] × N → @AfterAll (static).
     *   • NOWY obiekt klasy testowej na każdy test — pola nie przenoszą stanu; static przenosi (unikaj!).
     *   • @Disabled("powód") = pominięty; assumeTrue(...) fałszywe = przerwany; @Tag + filtr = wybrana grupa.
     *   • @Nested (bez static) = kontekst „gdy ...”; najpierw @BeforeEach zewnętrzny, potem wewnętrzny.
     *   • Kolejność testów nieoczywista → testy niezależne. Maven: src/test/java, mvn test, Surefire.
     *
     * PYTANIA KONTROLNE:
     *   1. Ile obiektów klasy testowej utworzy JUnit dla klasy z 4 metodami @Test (bez @TestInstance)? Po co tak robi?
     *   2. Co wypisze raport dla:  assertEquals(10, 2 + 3, "suma");  ?
     *   3. ZNAJDŹ BŁĄD:  @BeforeAll void init() { ... }   (w zwykłej klasie testowej)
     *   4. Czym różni się test pominięty (@Disabled) od przerwanego (assumeTrue) i od niezaliczonego?
     *   5. ZNAJDŹ BŁĄD:
     *        @Test void withdraw() {
     *            try { account.withdraw(1000); } catch (IllegalStateException e) { }
     *        }
     *   6. Dlaczego assertAll jest lepsze niż trzy kolejne assertEquals przy sprawdzaniu pól jednego obiektu?
     *   7. Co wypisze podsumowanie, jeśli klasę z 3 testami (jeden z @Tag("wolny"), pozostałe bez etykiet)
     *      uruchomimy z TagFilter.includeTags("szybki")?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: trzy zaliczone testy konta",
                "znalezione 3, zaliczone 3, niezaliczone 0, pominięte 0, przerwane 0",
                () -> runQuietly(Exercise1Tests.class).brief());
        Check.equal("ćw. 2: PRZEPISZ ręczny test na JUnit",
                "znalezione 2, zaliczone 2, niezaliczone 0, pominięte 0, przerwane 0",
                () -> runQuietly(Exercise2Tests.class).brief());
        Check.equal("ćw. 3: testy wyłapują wszystkie mutanty",
                "poprawna wersja: bez błędów; wykryte mutanty: 3 z 3",
                () -> mutationReport(Exercise3Tests.class));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)",
                "znalezione 3, zaliczone 3, niezaliczone 0, pominięte 0, przerwane 0",
                () -> runQuietly(Solution1Tests.class).brief());
        Check.equal("ćw. 2 (wzorzec)",
                "znalezione 2, zaliczone 2, niezaliczone 0, pominięte 0, przerwane 0",
                () -> runQuietly(Solution2Tests.class).brief());
        Check.equal("ćw. 3 (wzorzec)",
                "poprawna wersja: bez błędów; wykryte mutanty: 3 z 3",
                () -> mutationReport(Solution3Tests.class));
        Check.summary();
        // WYNIK: ✔ OK    ćw. 1 (wzorzec)
        // WYNIK: ✔ OK    ćw. 2 (wzorzec)
        // WYNIK: ✔ OK    ćw. 3 (wzorzec)
        // WYNIK: PODSUMOWANIE: ✔ 3 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): napisz DOKŁADNIE 3 zaliczone testy klasy Account (metodę todo() usuń):
     * (a) nowe konto pamięta właściciela, (b) wpłaty 40 i 60 dają saldo 100, (c) wpłata 0 rzuca IllegalArgumentException.
     * Każdy z @DisplayName po polsku i w układzie przygotuj–działaj–sprawdź.
     * Podpowiedź: wzoruj się na AnatomyTests z sekcji 1; w (c) użyj assertThrows.
     */
    static class Exercise1Tests {
        @Test
        void todo() {
            // TODO: zastąp tę metodę trzema testami
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ ręczny test w stylu t25_testing na DOKŁADNIE 2 testy JUnit:
     * <pre>{@code
     * Account a = new Account("Ola");
     * a.deposit(100);
     * a.withdraw(30);
     * if (a.balance() != 70) throw new AssertionError("saldo");
     * if (!"wypłata 30".equals(a.lastOperation())) throw new AssertionError("operacja");
     * try { a.withdraw(1000); throw new AssertionError("brak wyjątku"); }
     * catch (IllegalStateException e) { if (!e.getMessage().startsWith("brak środków")) throw new AssertionError("opis"); }
     * }</pre>
     * Test 1: saldo i ostatnia operacja w jednym assertAll. Test 2: assertThrows + sprawdzenie, że komunikat
     * zaczyna się od „brak środków”.
     * Podpowiedź: e.getMessage().startsWith("brak środków") w assertTrue; wspólne przygotowanie konta daj do @BeforeEach.
     */
    static class Exercise2Tests {
        @Test
        void todo() {
            // TODO: zastąp tę metodę dwoma testami
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): napisz testy funkcji kosztu dostawy, wołając ją WYŁĄCZNIE przez
     * {@code feeUnderTest.applyAsInt(wartość)} (apply as int = zastosuj i zwróć int). Sprawdzarka uruchomi Twoje
     * testy najpierw na poprawnej wersji (wszystkie muszą przejść), a potem na 3 „mutantach” — wersjach z celowym
     * błędem: granica {@code > 200} zamiast {@code >= 200}, opłata 10 zamiast 15, brak wyjątku dla wartości ujemnej.
     * Każdy mutant musi oblać co najmniej jeden Twój test.
     * Podpowiedź: testuj wartości graniczne 199, 200, 0 oraz -1 (o wyborze przypadków więcej w JUnit02Parameterized).
     */
    static class Exercise3Tests {
        @Test
        void todo() {
            // TODO: zastąp tę metodę swoimi testami (feeUnderTest.applyAsInt(...))
            throw new UnsupportedOperationException("TODO");
        }
    }

    /** Funkcja testowana w ćwiczeniu 3 (fee under test = opłata pod testem) — sprawdzarka podmienia ją na mutanty. */
    static IntUnaryOperator feeUnderTest = JUnit01Basics::deliveryFee;

    /** Mutanty = celowo zepsute wersje deliveryFee. Dobre testy „zabijają” (wykrywają) każdego z nich. */
    private static final List<IntUnaryOperator> MUTANTS = List.of(
            value -> { if (value < 0) throw new IllegalArgumentException("ujemna"); return value > 200 ? 0 : 15; },
            value -> { if (value < 0) throw new IllegalArgumentException("ujemna"); return value >= 200 ? 0 : 10; },
            value -> value >= 200 ? 0 : 15);

    /** Uruchamia testy na poprawnej wersji i na każdym mutancie, zwraca krótki raport (mutation report). */
    static String mutationReport(Class<?> testClass) {
        feeUnderTest = JUnit01Basics::deliveryFee;
        RunResult onCorrect = runQuietly(testClass);
        int killed = 0;                                            // killed = „zabite” (wykryte) mutanty
        for (IntUnaryOperator mutant : MUTANTS) {
            feeUnderTest = mutant;
            if (runQuietly(testClass).failed() > 0) {
                killed++;
            }
        }
        feeUnderTest = JUnit01Basics::deliveryFee;
        String correct = onCorrect.failed() == 0 && onCorrect.succeeded() > 0
                ? "bez błędów" : "niezaliczone " + onCorrect.failed() + " z " + onCorrect.found();
        return "poprawna wersja: " + correct + "; wykryte mutanty: " + killed + " z " + MUTANTS.size();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static class Solution1Tests {
        @Test
        @DisplayName("nowe konto pamięta właściciela")
        void remembersOwner() {
            Account account = new Account("Ola");
            assertEquals("Ola", account.owner());
        }

        @Test
        @DisplayName("wpłaty 40 i 60 dają saldo 100")
        void twoDeposits() {
            Account account = new Account("Ola");
            account.deposit(40);
            account.deposit(60);
            assertEquals(100, account.balance());
        }

        @Test
        @DisplayName("wpłata 0 rzuca IllegalArgumentException")
        void zeroDepositRejected() {
            Account account = new Account("Ola");
            assertThrows(IllegalArgumentException.class, () -> account.deposit(0));
        }
    }

    static class Solution2Tests {
        Account account;

        @BeforeEach
        void setUp() {
            account = new Account("Ola");
            account.deposit(100);
            account.withdraw(30);
        }

        @Test
        @DisplayName("po wypłacie 30 ze 100 saldo 70 i zapisana operacja")
        void balanceAndOperation() {
            assertAll("konto po wypłacie",
                    () -> assertEquals(70, account.balance(), "saldo"),
                    () -> assertEquals("wypłata 30", account.lastOperation(), "operacja"));
        }

        @Test
        @DisplayName("wypłata ponad saldo rzuca IllegalStateException z opisem")
        void overdraftRejected() {
            IllegalStateException e = assertThrows(IllegalStateException.class, () -> account.withdraw(1000));
            assertTrue(e.getMessage().startsWith("brak środków"), () -> "komunikat: " + e.getMessage());
        }
    }

    static class Solution3Tests {
        @Test
        @DisplayName("199 zł — tuż pod progiem — płatna dostawa 15 zł")
        void justBelowThreshold() { assertEquals(15, feeUnderTest.applyAsInt(199)); }

        @Test
        @DisplayName("200 zł — dokładnie próg — dostawa darmowa")
        void exactlyThreshold() { assertEquals(0, feeUnderTest.applyAsInt(200)); }

        @Test
        @DisplayName("0 zł — pusty koszyk — 15 zł")
        void emptyCart() { assertEquals(15, feeUnderTest.applyAsInt(0)); }

        @Test
        @DisplayName("-1 zł — rzuca IllegalArgumentException")
        void negativeRejected() {
            assertThrows(IllegalArgumentException.class, () -> feeUnderTest.applyAsInt(-1));
        }
    }

    // </editor-fold>

    // =================================================================================================
    // NARZĘDZIE LEKCJI: uruchamianie testów z main przez JUnit Platform Launcher
    // (to samo robią IntelliJ i Maven Surefire; tu wypisujemy wynik po swojemu, w stałej kolejności)
    // =================================================================================================

    /** Liczniki jednego uruchomienia (found = znalezione, succeeded = zaliczone, failed = niezaliczone, ...). */
    record RunResult(long found, long succeeded, long failed, long skipped, long aborted) {
        String brief() {   // brief = krótki opis
            return String.format(Locale.ROOT, "znalezione %d, zaliczone %d, niezaliczone %d, pominięte %d, przerwane %d",
                    found, succeeded, failed, skipped, aborted);
        }
    }

    /** Uruchamia testy klasy i wypisuje po jednej linii na test oraz podsumowanie. */
    static RunResult runTests(Class<?> testClass) {
        return run(testClass, true, Map.of());
    }

    /** Uruchamia testy bez wypisywania — tylko liczniki (używane przez sprawdzarki ćwiczeń). */
    static RunResult runQuietly(Class<?> testClass) {
        return run(testClass, false, Map.of());
    }

    /**
     * Serce narzędzia. Domyślny porządek: metody i klasy {@code @Nested} alfabetycznie po nazwie wyświetlanej
     * (DisplayName) — stały na każdym komputerze. {@code config} może go nadpisać, {@code filters} zawęża wybór testów.
     */
    static RunResult run(Class<?> testClass, boolean print, Map<String, String> config, Filter<?>... filters) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()   // request = żądanie
                .selectors(selectClass(testClass))                                     // select class = wybierz klasę
                .configurationParameter("junit.jupiter.testmethod.order.default",
                        "org.junit.jupiter.api.MethodOrderer$DisplayName")
                .configurationParameter("junit.jupiter.testclass.order.default",
                        "org.junit.jupiter.api.ClassOrderer$DisplayName")
                .configurationParameters(config)
                .filters(filters)
                .build();
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener summaryListener = new SummaryGeneratingListener();  // słuchacz zliczający wyniki
        if (print) {
            launcher.execute(request, summaryListener, new PrintingListener());
        } else {
            launcher.execute(request, summaryListener);
        }
        TestExecutionSummary s = summaryListener.getSummary();
        RunResult result = new RunResult(s.getTestsFoundCount(), s.getTestsSucceededCount(), s.getTestsFailedCount(),
                s.getTestsSkippedCount(), s.getTestsAbortedCount());
        if (print) {
            System.out.println(result.brief());
        }
        return result;
    }

    /** Słuchacz wypisujący jedną linię na test — bez czasów i śladów stosu, żeby wynik był zawsze taki sam. */
    static class PrintingListener implements TestExecutionListener {
        private TestPlan plan;   // plan = drzewo wykrytych testów

        @Override
        public void testPlanExecutionStarted(TestPlan testPlan) { plan = testPlan; }

        @Override
        public void executionSkipped(TestIdentifier id, String reason) {
            System.out.println("⊘ " + path(id) + " — pominięty: " + reason);
        }

        @Override
        public void executionFinished(TestIdentifier id, TestExecutionResult result) {
            if (!id.isTest() && result.getStatus() == TestExecutionResult.Status.SUCCESSFUL) {
                return;   // kontener (klasa, grupa) zakończony bez błędu — nie wypisujemy
            }
            String name = (id.isTest() ? "" : "[kontener] ") + path(id);
            Optional<Throwable> error = result.getThrowable();
            switch (result.getStatus()) {
                case SUCCESSFUL -> System.out.println("✔ " + name);
                case ABORTED -> System.out.println("⊘ " + name + " — przerwany: " + firstLine(error));
                case FAILED -> System.out.println("✘ " + name + " — "
                        + error.map(e -> e.getClass().getSimpleName()).orElse("?") + ": " + firstLine(error));
            }
        }

        /** Nazwa z kontekstami: „grupa › test” (bez nazwy samej klasy testowej). */
        private String path(TestIdentifier id) {
            List<String> parts = new ArrayList<>();
            TestIdentifier current = id;
            while (true) {
                Optional<TestIdentifier> parent = plan.getParent(current);
                if (parent.isEmpty() || plan.getParent(parent.get()).isEmpty()) {   // current = klasa główna albo silnik
                    if (parts.isEmpty()) {
                        parts.add(current.getDisplayName());
                    }
                    break;
                }
                parts.add(0, current.getDisplayName());
                current = parent.get();
            }
            return String.join(" › ", parts);
        }

        /** Pierwsza niepusta linia komunikatu wyjątku (komunikaty AssertJ i assertAll mają wiele linii). */
        private static String firstLine(Optional<Throwable> error) {
            return error.map(Throwable::getMessage)
                    .flatMap(m -> m.lines().map(String::strip).filter(l -> !l.isEmpty()).findFirst())
                    .orElse("(brak komunikatu)");
        }
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Cztery — po jednym na test (domyślny tryb PER_METHOD). Dzięki temu pola instancji zaczynają od zera
     *      i testy nie mogą sobie nawzajem psuć stanu przez pola.
     *   2. ✘ ... AssertionFailedError: suma ==> expected: <10> but was: <5>   (opis, potem oczekiwane i faktyczne).
     *   3. W domyślnym trybie metoda @BeforeAll musi być static (JUnit woła ją, zanim powstanie jakikolwiek obiekt
     *      klasy testowej). Bez static uruchomienie klasy kończy się błędem kontenera i żaden jej test się nie wykona.
     *      Wyjątek: klasa z @TestInstance(Lifecycle.PER_CLASS).
     *   4. Pominięty (@Disabled) — w ogóle nie startuje. Przerwany (assumeTrue z fałszem) — startuje, ale zatrzymuje się,
     *      bo nie spełniono warunku środowiska; nie jest ani zielony, ani czerwony. Niezaliczony — asercja się nie
     *      zgodziła albo poleciał nieoczekiwany wyjątek.
     *   5. Test przejdzie, nawet gdy withdraw NIE rzuci wyjątku — nie ma żadnej asercji. Poprawnie:
     *      assertThrows(IllegalStateException.class, () -> account.withdraw(1000));
     *   6. Pierwsze nieudane assertEquals kończy test — o błędach w kolejnych polach nie wiesz. assertAll wykona
     *      wszystkie sprawdzenia i zgłosi wszystkie porażki naraz („(2 failures)”).
     *   7. znalezione 0, zaliczone 0, niezaliczone 0, pominięte 0, przerwane 0 — filtr odrzuca testy bez etykiety
     *      „szybki” jeszcze przed uruchomieniem. Uwaga: „0 niezaliczonych” nie znaczy, że kod jest dobry!
     */
    // </editor-fold>
}
