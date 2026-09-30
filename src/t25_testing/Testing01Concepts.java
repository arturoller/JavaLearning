package t25_testing;

import helpers.Check;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Testowanie — po co, piramida testów, AAA/FIRST, własny mini-runner
 *        (test pyramid = piramida testów; Arrange-Act-Assert = przygotuj-wykonaj-sprawdź; test runner = program
 *        uruchamiający testy)
 *
 * W SKRÓCIE:
 *   Test automatyczny to kawałek kodu, który sprawdza INNY kawałek kodu i sam mówi PASS/FAIL. Testy pełnią dwie
 *   role: są SIECIĄ BEZPIECZEŃSTWA (safety net — po zmianie kodu wiesz, czy nic się nie zepsuło) i ŻYWĄ
 *   DOKUMENTACJĄ (nazwa testu opisuje, jak metoda ma się zachować w konkretnym przypadku — dokumentacja, która nie
 *   może się „zdezaktualizować”, bo inaczej test nie przejdzie). W tej lekcji budujemy WŁASNY, minimalny runner
 *   testowy — bez żadnego frameworka — żeby zrozumieć, co framework (JUnit, dział t32_junit_mockito) robi
 *   „pod maską”.
 *
 * ANALOGIA: kontrola techniczna samochodu.
 *   Diagnosta sprawdza hamulce, światła, zawieszenie — każdy punkt osobno, wg listy kontrolnej (checklist).
 *   Wynik to PASS albo FAIL przy KAŻDYM punkcie, nie ogólne „samochód jakoś jeździ”. Testy jednostkowe działają
 *   tak samo: każdy sprawdza JEDNĄ rzecz i sam ocenia wynik — nikt nie czyta ręcznie logów, żeby zgadnąć, czy jest OK.
 *
 * JAK TO DZIAŁA:
 *   1. Kod produkcyjny: metoda, którą chcemy sprawdzić (np. isPalindrome).
 *   2. Kod testowy: TestCase — nazwa + treść testu, która wywołuje kod produkcyjny i PORÓWNUJE wynik z oczekiwanym.
 *   3. Asercja (assert-) — gdy porównanie się nie zgadza, kod testowy RZUCA AssertionError z komunikatem.
 *   4. Runner — pętla po testach: łapie AssertionError, liczy ✔/✘, wypisuje podsumowanie.
 *   Warstwy (piramida testów, od dołu, najwięcej → najmniej):
 *     unit (jednostkowe)       — jedna metoda/klasa w izolacji, bez bazy/sieci — najszybsze, najwięcej.
 *     integration (integracyjne) — kilka klas/warstw razem (np. serwis + repozytorium) — wolniejsze.
 *     end-to-end (e2e)         — cała aplikacja jak użytkownik — najwolniejsze i najdroższe, najmniej.
 *
 * SŁÓWKA:
 *   test double = test podwójny (zamiennik zależności, temat Testing02); assertion = asercja (sprawdzenie);
 *   suite = zestaw (testów); fixture = przygotowane dane testowe; edge case = przypadek brzegowy; regression =
 *   regresja (błąd, który wrócił po zmianie kodu); coverage = pokrycie (testami); flaky test = niestabilny test
 *   (czasem przechodzi, czasem nie — zły znak).
 *
 * ZOBACZ TEŻ: t25_testing/Testing02TestDoubles (testy podwójne), t25_testing/Testing03TestableDesign
 *             (projektowanie pod testy), t10_exceptions/Exceptions07BestPractices (własne asercje vs expectThrows
 *             z tego kursu).
 * </pre>
 */
public class Testing01Concepts {

    public static void main(String[] args) {
        title("Testing01 — podstawy testowania");

        whyTest();                // why test = po co testować
        testPyramid();             // test pyramid = piramida testów
        arrangeActAssert();        // Arrange-Act-Assert = przygotuj-wykonaj-sprawdź
        firstPrinciples();         // FIRST principles = zasady FIRST
        buildRunner();             // build runner = budowa runnera
        runExampleTests();         // run example tests = uruchom przykładowe testy
        edgeCases();                // edge cases = przypadki brzegowe
        assertKeywordPitfall();    // assert keyword pitfall = pułapka słowa kluczowego assert
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // KOD POD TESTAMI (przykładowe metody, które będziemy testować)
    // =================================================================================================

    /**
     * isPalindrome = czy napis czytany od tyłu jest taki sam (bez rozróżniania wielkości liter, bez spacji).
     * {@code Locale.ROOT} — jawna lokalizacja, żeby wynik toLowerCase nie zależał od ustawień systemu (t15/t04).
     */
    static boolean isPalindrome(String text) {
        String cleaned = text.toLowerCase(Locale.ROOT).replace(" ", "");
        return cleaned.contentEquals(new StringBuilder(cleaned).reverse());
    }

    /** square = podnieś do kwadratu. Prosta funkcja pomocnicza używana w przykładach i ćwiczeniach. */
    static int square(int n) {
        return n * n;
    }

    // =================================================================================================
    // 1. PO CO TESTOWAĆ?
    // =================================================================================================

    /** 1. Test jako dokumentacja (nazwa mówi CO powinno się stać) i jako sieć bezpieczeństwa (łapie regresje). */
    static void whyTest() {
        section("1. Po co testować?");

        show("isPalindrome(\"kajak\")", isPalindrome("kajak"));
        show("isPalindrome(\"Kobyła ma mały bok\")", isPalindrome("Kobyła ma mały bok"));
        show("isPalindrome(\"Java\")", isPalindrome("Java"));
        // WYNIK: isPalindrome("kajak") → true
        // WYNIK: isPalindrome("Kobyła ma mały bok") → true
        // WYNIK: isPalindrome("Java") → false

        note("Testy to ŻYWA DOKUMENTACJA: nazwa testu mówi, jak metoda ma się zachować w konkretnym przypadku.");
        note("Testy to SIEĆ BEZPIECZEŃSTWA (safety net): po zmianie kodu uruchamiasz je i od razu wiesz, czy nic się nie zepsuło.");
        // WYNIK:    ℹ Testy to ŻYWA DOKUMENTACJA: nazwa testu mówi, jak metoda ma się zachować w konkretnym przypadku.
        // WYNIK:    ℹ Testy to SIEĆ BEZPIECZEŃSTWA (safety net): po zmianie kodu uruchamiasz je i od razu wiesz, czy nic się nie zepsuło.

        // DOBRA PRAKTYKA: pisz test PRZED naprawą zgłoszonego błędu (regression test). Test, który najpierw pokazuje
        //   ✘ (błąd istnieje), a po poprawce ✔, dowodzi, że błąd naprawdę zniknął — i że nie wróci niezauważony.
    }

    // =================================================================================================
    // 2. PIRAMIDA TESTÓW
    // =================================================================================================

    /** 2. Trzy warstwy testów, od najliczniejszej i najtańszej do najrzadszej i najdroższej. */
    static void testPyramid() {
        section("2. Piramida testów");

        note("unit (jednostkowe) — NAJWIĘCEJ: jedna metoda/klasa w izolacji, bez bazy/sieci/plików — milisekundy.");
        note("integration (integracyjne) — ŚREDNIO: kilka klas/warstw razem, np. serwis + repozytorium w pamięci.");
        note("end-to-end (e2e) — NAJMNIEJ: cała aplikacja jak użytkownik (UI, HTTP, prawdziwa baza) — sekundy, kruche.");
        // WYNIK:    ℹ unit (jednostkowe) — NAJWIĘCEJ: jedna metoda/klasa w izolacji, bez bazy/sieci/plików — milisekundy.
        // WYNIK:    ℹ integration (integracyjne) — ŚREDNIO: kilka klas/warstw razem, np. serwis + repozytorium w pamięci.
        // WYNIK:    ℹ end-to-end (e2e) — NAJMNIEJ: cała aplikacja jak użytkownik (UI, HTTP, prawdziwa baza) — sekundy, kruche.

        // JAK TO DZIAŁA: im wyżej w piramidzie, tym wolniejszy i droższy test w utrzymaniu (więcej się może zepsuć
        //   z powodów niezwiązanych z testowaną logiką — np. sieć). Dlatego większość testów pisze się jako unit.

        // PUŁAPKA: „odwrócona piramida” (dużo powolnych testów e2e, mało testów jednostkowych) — zestaw testów
        //   trwa długo, jest kruchy (flaky) i programiści przestają go uruchamiać. Dąż do kształtu trójkąta.
    }

    // =================================================================================================
    // 3. ARRANGE-ACT-ASSERT (GIVEN-WHEN-THEN)
    // =================================================================================================

    /** 3. Struktura pojedynczego testu: trzy wyraźne kroki, zawsze w tej kolejności. */
    static void arrangeActAssert() {
        section("3. Arrange-Act-Assert (Given-When-Then)");

        note("ARRANGE (Given) — przygotuj dane wejściowe i zależności (fixture).");
        note("ACT (When) — wykonaj TĘ JEDNĄ operację, którą testujesz.");
        note("ASSERT (Then) — sprawdź wynik. Jeden test = jedno logiczne sprawdzenie.");
        // WYNIK:    ℹ ARRANGE (Given) — przygotuj dane wejściowe i zależności (fixture).
        // WYNIK:    ℹ ACT (When) — wykonaj TĘ JEDNĄ operację, którą testujesz.
        // WYNIK:    ℹ ASSERT (Then) — sprawdź wynik. Jeden test = jedno logiczne sprawdzenie.

        // Przykład AAA na papierze (kod w sekcji 6):
        //   Arrange: String text = "kajak";
        //   Act:     boolean result = isPalindrome(text);
        //   Assert:  assertTrue("kajak jest palindromem", result);

        // DOBRA PRAKTYKA: puste linie między krokami (jak wyżej w komentarzu) ułatwiają odnalezienie ACT w dużym
        //   teście na pierwszy rzut oka — nawet bez czytania nazw zmiennych.
    }

    // =================================================================================================
    // 4. ZASADY FIRST
    // =================================================================================================

    /** 4. FIRST = pięć cech dobrego zestawu testów jednostkowych (akronim). */
    static void firstPrinciples() {
        section("4. Zasady FIRST");

        List<String> first = List.of(
                "Fast (szybkie) — cały zestaw w sekundach, nie minutach — uruchamiasz go po KAŻDEJ zmianie",
                "Independent (niezależne) — kolejność i wynik jednego testu nie wpływa na inny",
                "Repeatable (powtarzalne) — ten sam wynik zawsze: bez sieci, bez zegara systemowego, bez losowości bez ziarna",
                "Self-validating (samosprawdzające) — test sam mówi PASS/FAIL, bez ręcznego czytania logów",
                "Timely (na czas) — piszesz test blisko w czasie z kodem, najlepiej od razu albo tuż przed nim");
        for (int i = 0; i < first.size(); i++) {
            System.out.println("   " + (i + 1) + ". " + first.get(i));
        }
        // WYNIK:    1. Fast (szybkie) — cały zestaw w sekundach, nie minutach — uruchamiasz go po KAŻDEJ zmianie
        // WYNIK:    2. Independent (niezależne) — kolejność i wynik jednego testu nie wpływa na inny
        // WYNIK:    3. Repeatable (powtarzalne) — ten sam wynik zawsze: bez sieci, bez zegara systemowego, bez losowości bez ziarna
        // WYNIK:    4. Self-validating (samosprawdzające) — test sam mówi PASS/FAIL, bez ręcznego czytania logów
        // WYNIK:    5. Timely (na czas) — piszesz test blisko w czasie z kodem, najlepiej od razu albo tuż przed nim

        // PUŁAPKA: test korzystający wprost z LocalDate.now() albo z new Random() (bez ziarna) łamie Repeatable —
        //   dziś przechodzi, jutro może nie. Rozwiązanie (Clock, Random z ziarnem) pokazuje Testing02TestDoubles.
    }

    // =================================================================================================
    // 5. WŁASNY MINI-RUNNER TESTOWY — BUDOWA
    // =================================================================================================

    /**
     * TestCase = przypadek testowy: nazwa (co sprawdzamy) + treść testu. {@code Runnable} = zadanie bez wyniku
     * (interfejs funkcyjny z jedną metodą {@code run()}) — treść testu, która ma rzucić AssertionError, gdy coś
     * jest nie tak, albo zwyczajnie się zakończyć, gdy wszystko gra.
     */
    record TestCase(String name, Runnable body) {
    }

    /** assertEquals = własna asercja równości. {@code Objects.equals} bezpiecznie obsługuje null po obu stronach. */
    static void assertEquals(String message, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " — oczekiwano: " + expected + ", jest: " + actual);
        }
    }

    /** assertTrue = własna asercja prawdziwości warunku logicznego. */
    static void assertTrue(String message, boolean condition) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /**
     * assertThrows = własna asercja: treść testu MUSI rzucić wyjątek typu expectedType (albo jego podklasy).
     * {@code Class.isInstance(obj)} = sprawdź w runtime, czy obj jest instancją tej klasy (odpowiednik instanceof,
     * ale gdy klasa jest zmienną, a nie literałem w kodzie).
     */
    static void assertThrows(String message, Class<? extends Throwable> expectedType, Runnable body) {
        try {
            body.run();
        } catch (Throwable t) {
            if (expectedType.isInstance(t)) {
                return;
            }
            throw new AssertionError(message + " — spodziewano się " + expectedType.getSimpleName()
                    + ", rzucono " + t.getClass().getSimpleName(), t);
        }
        throw new AssertionError(message + " — spodziewano się wyjątku " + expectedType.getSimpleName() + ", nic nie rzucono");
    }

    /** runSuite = uruchom zestaw testów: wypisuje ✔/✘ dla każdego, na końcu podsumowanie, zwraca liczbę niepowodzeń. */
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

    /** 5. Trzy klocki runnera: TestCase (dane), assert... (sprawdzenie), runSuite (pętla + zliczanie). */
    static void buildRunner() {
        section("5. Budowa mini-runnera: TestCase, assertEquals, assertThrows");

        note("TestCase(name, body) — nazwa testu + kod, który go wykonuje (tak jak metoda @Test w JUnit, tylko ręcznie).");
        note("assertEquals/assertTrue/assertThrows rzucają AssertionError — runner go łapie i liczy jako ✘.");
        // WYNIK:    ℹ TestCase(name, body) — nazwa testu + kod, który go wykonuje (tak jak metoda @Test w JUnit, tylko ręcznie).
        // WYNIK:    ℹ assertEquals/assertTrue/assertThrows rzucają AssertionError — runner go łapie i liczy jako ✘.

        assertEquals("2 + 2 = 4", 4, 2 + 2);   // przechodzi bez wyjątku — nic się nie dzieje, to dobry znak
        note("assertEquals(\"2 + 2 = 4\", 4, 2 + 2) przeszła BEZ wyjątku — to jest właśnie sukces asercji.");
        // WYNIK:    ℹ assertEquals("2 + 2 = 4", 4, 2 + 2) przeszła BEZ wyjątku — to jest właśnie sukces asercji.

        // DOBRA PRAKTYKA: komunikat asercji (pierwszy argument) powinien identyfikować PRZYPADEK, nie duplikować
        //   oczekiwanej/rzeczywistej wartości — te dwie i tak są w komunikacie AssertionError automatycznie.
    }

    // =================================================================================================
    // 6. URUCHOMIENIE TESTÓW
    // =================================================================================================

    /** 6. Zestaw testów dla isPalindrome — w tym JEDEN celowo zły, żeby zobaczyć jak wygląda ✘. */
    static void runExampleTests() {
        section("6. Uruchomienie testów: isPalindrome");

        List<TestCase> tests = List.of(
                new TestCase("kajak jest palindromem", () -> assertTrue("kajak", isPalindrome("kajak"))),
                new TestCase("zdanie z polskimi znakami jest palindromem",
                        () -> assertTrue("Kobyła ma mały bok", isPalindrome("Kobyła ma mały bok"))),
                new TestCase("Java NIE jest palindromem", () -> assertTrue("Java", !isPalindrome("Java"))),
                new TestCase("celowo zły test — pokazuje ✘", () -> assertEquals("isPalindrome(\"ab\")", true, isPalindrome("ab"))));
        runSuite(tests);
        // WYNIK:    ✔ kajak jest palindromem
        // WYNIK:    ✔ zdanie z polskimi znakami jest palindromem
        // WYNIK:    ✔ Java NIE jest palindromem
        // WYNIK:    ✘ celowo zły test — pokazuje ✘ — isPalindrome("ab") — oczekiwano: true, jest: false
        // WYNIK:    ℹ wynik: 3 OK, 1 BŁĄD

        // Ten czwarty test jest ZŁY CELOWO (isPalindrome("ab") naprawdę zwraca false) — tak wygląda test, który
        //   wykrył niezgodność między oczekiwaniem a rzeczywistością. W prawdziwym projekcie taki wynik oznacza:
        //   albo kod ma błąd, albo test ma złe oczekiwanie — trzeba zdecydować, które.
    }

    // =================================================================================================
    // 7. PRZYPADKI BRZEGOWE (EDGE CASES)
    // =================================================================================================

    /** 7. Dobry test kompletu obejmuje nie tylko "typowy" przypadek, ale i granice zachowania. */
    static void edgeCases() {
        section("7. Przypadki brzegowe (edge cases)");

        List<TestCase> tests = List.of(
                new TestCase("pusty napis jest palindromem", () -> assertTrue("\"\"", isPalindrome(""))),
                new TestCase("jeden znak jest palindromem", () -> assertTrue("\"x\"", isPalindrome("x"))),
                new TestCase("null rzuca NullPointerException",
                        () -> assertThrows("isPalindrome(null)", NullPointerException.class, () -> isPalindrome(null))),
                new TestCase("bardzo długi napis (10 000 znaków) jest palindromem",
                        () -> assertTrue("długi napis", isPalindrome("a".repeat(10_000)))));  // repeat = powtórz (Java 11+)
        runSuite(tests);
        // WYNIK:    ✔ pusty napis jest palindromem
        // WYNIK:    ✔ jeden znak jest palindromem
        // WYNIK:    ✔ null rzuca NullPointerException
        // WYNIK:    ✔ bardzo długi napis (10 000 znaków) jest palindromem
        // WYNIK:    ℹ wynik: 4 OK, 0 BŁĄD

        note("Klasy przypadków do sprawdzenia: pusty/null, jeden element, granica (np. Integer.MAX_VALUE), ujemna, bardzo duża.");
        // WYNIK:    ℹ Klasy przypadków do sprawdzenia: pusty/null, jeden element, granica (np. Integer.MAX_VALUE), ujemna, bardzo duża.

        // DOBRA PRAKTYKA: programiści piszą chętnie testy „typowego” przypadku i zapominają o brzegach — a to
        //   właśnie na brzegach (0, -1, null, pusta kolekcja, MAX_VALUE) najczęściej kryją się prawdziwe błędy.
    }

    // =================================================================================================
    // 8. PUŁAPKA: SŁOWO KLUCZOWE assert
    // =================================================================================================

    /** 8. Java ma wbudowane słowo kluczowe assert — ale domyślnie jest WYŁĄCZONE. */
    static void assertKeywordPitfall() {
        section("8. PUŁAPKA: assert jest domyślnie wyłączony");

        int x = -5;
        assert x > 0 : "x powinien być dodatni, jest: " + x;    // BEZ flagi -ea ten warunek nie jest w ogóle liczony
        note("Program NIE zatrzymał się, mimo że warunek assert był fałszywy — JVM domyślnie ignoruje 'assert'.");
        // WYNIK:    ℹ Program NIE zatrzymał się, mimo że warunek assert był fałszywy — JVM domyślnie ignoruje 'assert'.

        // PUŁAPKA: 'assert' wymaga uruchomienia JVM z flagą -ea (enable assertions); bez niej cały warunek jest
        //   POMIJANY (nawet nie jest liczony!). Dlatego język 'assert' nadaje się do sprawdzania własnych założeń
        //   podczas programowania/debugowania, ale NIE do testów — stąd własne assertEquals/assertThrows powyżej,
        //   które działają ZAWSZE, niezależnie od flag uruchomienia JVM.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Test = kod, który sam mówi PASS/FAIL. Rola: sieć bezpieczeństwa (regresje) + żywa dokumentacja.
     *   • Piramida: dużo unit (szybkie, tanie) → mniej integration → najmniej e2e (wolne, drogie, kruche).
     *   • AAA/Given-When-Then: Arrange (przygotuj) → Act (wykonaj JEDNĄ rzecz) → Assert (sprawdź).
     *   • FIRST: Fast, Independent, Repeatable, Self-validating, Timely.
     *   • Własny runner: TestCase(name, body) + assert... (rzuca AssertionError) + pętla licząca ✔/✘.
     *   • assert (słowo kluczowe) jest domyślnie WYŁĄCZONY (wymaga -ea) — nie nadaje się do testów.
     *   • Testuj też przypadki brzegowe: pusty/null, jeden element, granica, ujemna, bardzo duża wartość.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się test jednostkowy od integracyjnego? Podaj przykład każdego (isPalindrome vs np. serwis
     *      zamówień korzystający z repozytorium).
     *   2. Dlaczego piramida testów ma kształt trójkąta, a nie prostokąta (dlaczego testów e2e jest najmniej)?
     *   3. Co wypisze ten fragment (bez flagi -ea)?
     *          int y = 10;
     *          assert y < 0 : "y powinno być ujemne";
     *          System.out.println("dalej");
     *   4. ZNAJDŹ BŁĄD w tym teście — dlaczego runner NIGDY nie pokaże dla niego ✘, nawet gdy suma jest zła?
     *          new TestCase("suma", () -> {
     *              int result = 2 + 2;
     *              if (result != 5) System.out.println("błąd!");
     *          });
     *   5. Którą z zasad FIRST łamie test korzystający wprost z LocalDate.now() bez wstrzykiwania Clock?
     *   6. Co się stanie, gdy treść testu w runSuite rzuci IllegalStateException zamiast AssertionError? (podpowiedź:
     *      runSuite łapie tylko catch (AssertionError e))
     *   7. Zapisz w trzech linijkach (Arrange/Act/Assert) test sprawdzający, że square(-3) == 9.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** Pomocnicza: uruchamia ćwiczenie 2 i mówi, czy RZUCIŁO AssertionError (false), czy nie (true). */
    static boolean exercise2NoThrow(Object unexpected, Object actual) {
        try {
            exercise2("test", unexpected, actual);
            return true;
        } catch (AssertionError e) {
            return false;
        }
    }

    static boolean solution2NoThrow(Object unexpected, Object actual) {
        try {
            solution2("test", unexpected, actual);
            return true;
        } catch (AssertionError e) {
            return false;
        }
    }

    /** Pomocnicza: uruchamia ćwiczenie 3 i zwraca "OK" albo komunikat błędu asercji. */
    static String checkSquareOutcome(int n, int expected) {
        try {
            exercise3(n, expected);
            return "OK";
        } catch (AssertionError e) {
            return "BŁĄD: " + e.getMessage();
        }
    }

    static String checkSquareOutcomeSolution(int n, int expected) {
        try {
            solution3(n, expected);
            return "OK";
        } catch (AssertionError e) {
            return "BŁĄD: " + e.getMessage();
        }
    }

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");

        Check.equal("ćw. 1: 2 z 3 testów nieudane", 2, () -> exercise1(List.of(
                new TestCase("OK1", () -> assertTrue("a", true)),
                new TestCase("FAIL1", () -> assertEquals("b", 1, 2)),
                new TestCase("FAIL2", () -> assertTrue("c", false)))));
        Check.throwsException("ćw. 2a: równe wartości → AssertionError", AssertionError.class, () -> exercise2("test", 5, 5));
        Check.equal("ćw. 2b: różne wartości → brak wyjątku", true, () -> exercise2NoThrow(5, 6));
        Check.equal("ćw. 3a: square(5)=25 zgodne", "OK", () -> checkSquareOutcome(5, 25));
        Check.equal("ćw. 3b: square(5)=99 niezgodne → wykryte", "BŁĄD: square(5) — oczekiwano: 99, jest: 25",
                () -> checkSquareOutcome(5, 99));
        Check.equal("ćw. 4: podsumowanie 3 OK, 2 BŁĄD", "3 OK, 2 BŁĄD", () -> exercise4(List.of(
                new TestCase("t1", () -> assertTrue("a", true)),
                new TestCase("t2", () -> assertTrue("b", true)),
                new TestCase("t3", () -> assertTrue("c", true)),
                new TestCase("t4", () -> assertEquals("d", 1, 2)),
                new TestCase("t5", () -> assertTrue("e", false)))));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 2, () -> solution1(List.of(
                new TestCase("OK1", () -> assertTrue("a", true)),
                new TestCase("FAIL1", () -> assertEquals("b", 1, 2)),
                new TestCase("FAIL2", () -> assertTrue("c", false)))));
        Check.throwsException("ćw. 2a (wzorzec)", AssertionError.class, () -> solution2("test", 5, 5));
        Check.equal("ćw. 2b (wzorzec)", true, () -> solution2NoThrow(5, 6));
        Check.equal("ćw. 3a (wzorzec)", "OK", () -> checkSquareOutcomeSolution(5, 25));
        Check.equal("ćw. 3b (wzorzec)", "BŁĄD: square(5) — oczekiwano: 99, jest: 25", () -> checkSquareOutcomeSolution(5, 99));
        Check.equal("ćw. 4 (wzorzec)", "3 OK, 2 BŁĄD", () -> solution4(List.of(
                new TestCase("t1", () -> assertTrue("a", true)),
                new TestCase("t2", () -> assertTrue("b", true)),
                new TestCase("t3", () -> assertTrue("c", true)),
                new TestCase("t4", () -> assertEquals("d", 1, 2)),
                new TestCase("t5", () -> assertTrue("e", false)))));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): policz, ile testów z listy NIE przechodzi — uruchom każdy CICHO (bez wypisywania,
     * bez korzystania z runSuite) i zwróć liczbę tych, których treść rzuciła AssertionError.
     * Podpowiedź: pętla + try/catch (AssertionError e), tak jak w runSuite, ale bez println.
     */
    static int exercise1(List<TestCase> tests) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (łatwe): napisz assertNotEquals — asercję ODWROTNĄ do assertEquals. Ma rzucić AssertionError
     * z komunikatem {@code message + " — nie powinno być równe: " + actual}, gdy unexpected i actual SĄ równe
     * (Objects.equals). Gdy są różne — nic nie rzucaj.
     */
    static void exercise2(String message, Object unexpected, Object actual) {
        // TODO: twoje rozwiązanie
    }

    /**
     * ĆWICZENIE 3 (średnie, „PRZEPISZ”): stara, ręczna wersja sprawdzenia wyglądała tak:
     * <pre>{@code
     * int result = square(n);
     * if (result != expected) {
     *     System.out.println("BŁĄD: oczekiwano " + expected + ", jest " + result);
     * }
     * }</pre>
     * Przepisz to jako checkSquare, które używa assertEquals(String, Object, Object) z sekcji 5 z komunikatem
     * {@code "square(" + n + ")"} — zamiast wypisywać błąd, MA GO ZGŁOSIĆ przez wyjątek (tak jak prawdziwy test).
     */
    static void exercise3(int n, int expected) {
        // TODO: twoje rozwiązanie
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): napisz summarize — jak runSuite z sekcji 5, ale BEZ wypisywania czegokolwiek
     * (żadnego ✔/✘/ℹ); zamiast tego zwróć podsumowanie jako tekst w formacie {@code "X OK, Y BŁĄD"}.
     * Podpowiedź: policz passed i failed tak jak w runSuite, na końcu zbuduj jeden String.
     */
    static String exercise4(List<TestCase> tests) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(List<TestCase> tests) {
        int failed = 0;
        for (TestCase test : tests) {
            try {
                test.body().run();
            } catch (AssertionError e) {
                failed++;
            }
        }
        return failed;
    }

    static void solution2(String message, Object unexpected, Object actual) {
        if (Objects.equals(unexpected, actual)) {
            throw new AssertionError(message + " — nie powinno być równe: " + actual);
        }
    }

    static void solution3(int n, int expected) {
        assertEquals("square(" + n + ")", expected, square(n));
    }

    static String solution4(List<TestCase> tests) {
        int passed = 0;
        int failed = 0;
        for (TestCase test : tests) {
            try {
                test.body().run();
                passed++;
            } catch (AssertionError e) {
                failed++;
            }
        }
        return passed + " OK, " + failed + " BŁĄD";
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Jednostkowy: isPalindrome("kajak") — jedna metoda, bez zależności zewnętrznych. Integracyjny: np. test
     *      OrderService, który naprawdę korzysta z repozytorium (choćby w pamięci) i sprawdza WSPÓŁPRACĘ obu klas.
     *   2. Testy wyższych warstw są wolniejsze i bardziej kruche (więcej zależności może się „posypać” z powodów
     *      niezwiązanych z testowaną logiką, np. sieć, czas uruchomienia). Chcemy szybkiej, częstej pętli
     *      zwrotnej — dlatego większość testów to tanie testy jednostkowe.
     *   3. Wypisze tylko "dalej" — assert bez flagi -ea jest w ogóle nie liczony, program leci dalej normalnie.
     *   4. Test nigdy nie zgłosi ✘ rannerowi, bo błąd tylko WYPISUJE komunikat (println), zamiast rzucić
     *      AssertionError. Runner łapie wyjątki, a nie treść wypisaną na konsolę — narusza to Self-validating z FIRST.
     *   5. Repeatable — wynik testu zależałby od DNIA URUCHOMIENIA (LocalDate.now() zmienia się codziennie),
     *      więc ten sam test raz przejdzie, raz nie, bez zmiany kodu produkcyjnego.
     *   6. IllegalStateException NIE zostanie złapany przez runSuite (catch obejmuje tylko AssertionError) —
     *      wyjątek wyleci z metody runSuite i przerwie CAŁY program, zanim policzone zostaną kolejne testy.
     *   7. Arrange: int n = -3;  Act: int result = square(n);  Assert: assertEquals("square(-3)", 9, result);
     */
    // </editor-fold>
}
