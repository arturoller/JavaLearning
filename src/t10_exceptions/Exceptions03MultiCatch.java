package t10_exceptions;

import helpers.Check;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Kilka bloków catch, multi-catch, ponowne rzucanie, wyjątek w catch/finally, przetwarzanie wsadowe
 *        (multi-catch = łapanie kilku typów naraz; rethrow = rzuć ponownie; batch = wsad, partia danych)
 *
 * W SKRÓCIE:
 *   Jeden try może mieć kilka bloków catch — Java sprawdza je OD GÓRY i wybiera PIERWSZY pasujący. Dlatego typy
 *   szczegółowe piszemy przed ogólnymi. Gdy kilka typów obsługujemy tak samo — multi-catch: catch (A | B e) (Java 7+).
 *   Złapany wyjątek można rzucić dalej (throw e), np. po zapisaniu informacji. Uwaga: wyjątek rzucony w catch albo
 *   finally ZASTĘPUJE oryginalny — oryginał ginie, jeśli go nie dołączysz.
 *
 * ANALOGIA: sortownia paczek.
 *   Paczka (wyjątek) jedzie taśmą obok kolejnych stanowisk (catch). Stanowisko „szkło” bierze tylko szkło,
 *   „elektronika” tylko elektronikę, a ostatnie „wszystko inne” bierze resztę. Gdyby „wszystko inne” stało
 *   PIERWSZE, pozostałe stanowiska nigdy by nic nie dostały.
 *
 * JAK TO DZIAŁA:
 *   try { ... }
 *   catch (NumberFormatException e)                          { ... }  ← szczegółowy
 *   catch (ArithmeticException | ArrayIndexOutOfBoundsException e) { ... }  ← multi-catch: jedna obsługa, dwa typy
 *   catch (RuntimeException e)                               { ... }  ← ogólny — na końcu
 *
 * SŁÓWKA:
 *   multi-catch = łapanie wielu typów; rethrow = rzuć ponownie; batch = partia (wsad); compute = oblicz;
 *   classify = sklasyfikuj; input = dane wejściowe; error = błąd; report = raport; last resort = ostatnia deska ratunku;
 *   precise rethrow = precyzyjne ponowne rzucenie; override = nadpisać (tu: zastąpić wyjątek).
 *
 * ZOBACZ TEŻ: t10_exceptions/Exceptions01Basics (hierarchia), t10_exceptions/Exceptions04TryWithResources (wyjątki stłumione),
 *             t10_exceptions/Exceptions06ChainingWrapping (dołączanie przyczyny), t10_exceptions/Exceptions07BestPractices.
 * </pre>
 */
public class Exceptions03MultiCatch {

    /** DATA = tablica kwadratów 0, 1, 4, 9 ... 81 (indeksy 0..9). */
    static final int[] DATA = {0, 1, 4, 9, 16, 25, 36, 49, 64, 81};

    /**
     * compute = oblicz. Trzy różne rzeczy mogą się nie udać:
     * zły tekst → NumberFormatException; zero → ArithmeticException; indeks poza tablicą → ArrayIndexOutOfBoundsException.
     */
    static int compute(String input) {
        int n = Integer.parseInt(input);   // "x" → NumberFormatException
        int index = 100 / n;                // 0 → ArithmeticException
        return DATA[index];                 // np. 100 / 5 = 20 → ArrayIndexOutOfBoundsException
    }

    public static void main(String[] args) {
        title("Exceptions03 — kilka catch, multi-catch, ponowne rzucanie");

        severalCatchBlocks();       // several catch blocks = kilka bloków catch
        multiCatch();               // multi-catch = łapanie wielu typów naraz
        catchAllAtTheTop();         // catch all at the top = łap wszystko na samej górze
        rethrow();                  // rethrow = rzuć ponownie
        exceptionInCatchOrFinally(); // exception in catch or finally = wyjątek w catch lub finally
        batchProcessing();          // batch processing = przetwarzanie wsadowe
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. KILKA BLOKÓW catch
    // =================================================================================================

    /** describe = opisz. Każdy typ błędu ma inną obsługę. */
    static String describe(String input) {
        try {
            return "wynik " + compute(input);
        } catch (NumberFormatException e) {
            return "to nie liczba";
        } catch (ArithmeticException e) {
            return "dzielenie przez zero";
        } catch (ArrayIndexOutOfBoundsException e) {
            return "poza tablicą (" + e.getMessage() + ")";
        }
    }

    /** 1. Java sprawdza catch od góry i wybiera PIERWSZY pasujący typ. Pozostałe są pomijane. */
    static void severalCatchBlocks() {
        section("1. Kilka bloków catch");

        for (String in : List.of("20", "x", "0", "5")) {
            show(in, describe(in));
        }
        // WYNIK: 20 → wynik 25
        // WYNIK: x → to nie liczba
        // WYNIK: 0 → dzielenie przez zero
        // WYNIK: 5 → poza tablicą (Index 20 out of bounds for length 10)

        // PUŁAPKA: catch (RuntimeException e) PRZED catch (ArithmeticException e) → błąd kompilacji
        //   („exception ArithmeticException has already been caught”) — ogólny typ „zjadłby” szczegółowy.
    }

    // =================================================================================================
    // 2. MULTI-CATCH
    // =================================================================================================

    /** 2. catch (A | B e) — jedna obsługa dla kilku typów (Java 7+). Mniej powielonego kodu. */
    static void multiCatch() {
        section("2. Multi-catch: catch (A | B e)");

        for (String in : List.of("0", "5", "abc")) {
            try {
                show(in, compute(in));
            } catch (ArithmeticException | ArrayIndexOutOfBoundsException e) {
                show(in, "błąd obliczeń: " + e.getClass().getSimpleName());
            } catch (NumberFormatException e) {
                show(in, "błąd danych");
            }
        }
        // WYNIK: 0 → błąd obliczeń: ArithmeticException
        // WYNIK: 5 → błąd obliczeń: ArrayIndexOutOfBoundsException
        // WYNIK: abc → błąd danych

        // PUŁAPKA: w multi-catch typy nie mogą być ze sobą spokrewnione (podklasa i nadklasa), np.
        //   catch (NumberFormatException | IllegalArgumentException e) → błąd kompilacji — wystarczy sam nadtyp.
        // Zmienna e w multi-catch jest niejawnie final — nie można do niej przypisać innego wyjątku.
    }

    // =================================================================================================
    // 3. catch (Exception e) — TYLKO NA SAMEJ GÓRZE
    // =================================================================================================

    /**
     * 3. Ogólne catch (Exception e) ma sens w jednym miejscu: na najwyższym poziomie (pętla główna programu, obsługa
     * jednego żądania), żeby JEDEN błąd nie wyłączył całej aplikacji. Tam zapisujemy błąd i idziemy dalej.
     */
    static void catchAllAtTheTop() {
        section("3. catch (Exception e) na najwyższym poziomie");

        List<String> commands = List.of("20", "0", "50");
        for (String cmd : commands) {                         // „pętla główna” — każdy rozkaz osobno
            try {
                show("rozkaz " + cmd, compute(cmd));
            } catch (Exception e) {                           // ostatnia deska ratunku: zapisz i działaj dalej
                show("rozkaz " + cmd, "BŁĄD " + e.getClass().getSimpleName() + " — pomijam, działam dalej");
            }
        }
        // WYNIK: rozkaz 20 → 25
        // WYNIK: rozkaz 0 → BŁĄD ArithmeticException — pomijam, działam dalej
        // WYNIK: rozkaz 50 → 4

        // PUŁAPKA: catch (Exception e) GŁĘBOKO w kodzie (w zwykłej metodzie) ukrywa błędy programisty (NPE, zły indeks)
        //   i łapie wyjątki, których nikt nie planował obsłużyć. Łap możliwie konkretne typy.
    }

    // =================================================================================================
    // 4. PONOWNE RZUCANIE (rethrow)
    // =================================================================================================

    /** save = zapisz. Udaje zapis, który się nie udaje (IOException). */
    static void save(String data) throws IOException {
        throw new IOException("Dysk pełny przy zapisie: " + data);
    }

    /**
     * saveWithLog = zapisz z logowaniem. Łapie, dopisuje informację i rzuca TEN SAM wyjątek dalej.
     * Precise rethrow (Java 7+): choć łapiemy Exception, kompilator wie, że z try może polecieć tylko IOException —
     * wystarczy throws IOException, a nie throws Exception.
     */
    static void saveWithLog(String data, List<String> log) throws IOException {
        try {
            save(data);
        } catch (Exception e) {
            log.add("nieudany zapis: " + data);
            throw e;                                          // rzuć dalej — niech zdecyduje ktoś wyżej
        }
    }

    /** 4. Czasem chcesz coś zrobić z wyjątkiem (zapisać w logu, posprzątać) i przekazać go dalej. */
    static void rethrow() {
        section("4. Złap, zanotuj, rzuć dalej");

        List<String> log = new ArrayList<>();
        try {
            saveWithLog("raport.csv", log);
        } catch (IOException e) {
            show("wyżej złapano", e.getMessage());
        }
        show("log", log);
        // WYNIK: wyżej złapano → Dysk pełny przy zapisie: raport.csv
        // WYNIK: log → [nieudany zapis: raport.csv]

        // PUŁAPKA: „log i rzuć” w KAŻDEJ warstwie daje ten sam błąd zapisany 5 razy. Zasada: albo obsłuż (i zaloguj),
        //   albo przekaż dalej — logowanie zwykle w jednym miejscu, tam, gdzie wyjątek jest ostatecznie obsłużony.
    }

    // =================================================================================================
    // 5. WYJĄTEK W catch LUB finally ZASTĘPUJE ORYGINAŁ
    // =================================================================================================

    /** failingCatch = catch, który sam rzuca wyjątek. Wyjątek z catch zastępuje oryginalny. */
    static void failingCatch() {
        try {
            throw new IllegalStateException("ORYGINAŁ: błąd przetwarzania");
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("z catch: nowy wyjątek");   // oryginał e przepada (brak cause)
        }
    }

    /**
     * 5. Jeśli w catch (albo finally) poleci nowy wyjątek, to on „wychodzi” z metody. Pierwotna przyczyna ginie —
     * a to ona była najważniejsza przy szukaniu błędu.
     */
    static void exceptionInCatchOrFinally() {
        section("5. Wyjątek w catch zastępuje oryginał");

        try {
            failingCatch();
        } catch (RuntimeException e) {
            show("dotarł", e.getMessage());
            show("przyczyna (getCause)", e.getCause());
        }
        // WYNIK: dotarł → z catch: nowy wyjątek
        // WYNIK: przyczyna (getCause) → null    ← informacja o ORYGINALE przepadła

        // DOBRA PRAKTYKA: rzucając nowy wyjątek w catch, przekaż stary jako przyczynę:
        //   throw new IllegalArgumentException("...", e);  — Exceptions06ChainingWrapping.
        // PUŁAPKA: to samo dzieje się, gdy wyjątek poleci w finally — dlatego finally ma być proste i „bezpieczne”;
        //   zamykanie zasobów rób przez try-with-resources (ono zachowuje oba wyjątki — Exceptions04).
    }

    // =================================================================================================
    // 6. PRZETWARZANIE WSADOWE: błędy zbierane, praca trwa
    // =================================================================================================

    /** 6. Przy imporcie wielu rekordów jeden zły nie powinien zatrzymać całego wsadu. Błędy zbieramy do raportu. */
    static void batchProcessing() {
        section("6. Przetwarzanie wsadowe: zbieraj błędy, nie przerywaj");

        List<String> inputs = List.of("20", "x", "100", "0", "50");
        int sum = 0;
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            try {
                sum += compute(inputs.get(i));
            } catch (RuntimeException e) {
                errors.add("wiersz " + (i + 1) + ": " + e.getClass().getSimpleName());
            }
        }
        show("suma poprawnych", sum);
        show("błędy", errors);
        // WYNIK: suma poprawnych → 30
        // WYNIK: błędy → [wiersz 2: NumberFormatException, wiersz 4: ArithmeticException]
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Kilka catch: sprawdzane od góry, wygrywa PIERWSZY pasujący; szczegółowe przed ogólnymi (inaczej błąd kompilacji).
     *   • Multi-catch: catch (A | B e) — typy niespokrewnione; e jest final.
     *   • catch (Exception e) tylko na najwyższym poziomie (pętla główna, jedno żądanie) — zapisz i działaj dalej.
     *   • Rethrow: catch (...) { zanotuj; throw e; } — precyzyjne: kompilator zna faktyczne typy z try.
     *   • Wyjątek z catch/finally zastępuje oryginał — przekazuj stary jako cause: new X("...", e).
     *   • Wsad: try/catch WEWNĄTRZ pętli, błędy do listy, suma/raport na końcu.
     *
     * PYTANIA KONTROLNE:
     *   1. Co wypisze:  System.out.println(describe("4"));  ?  (100 / 4 = 25)
     *   2. ZNAJDŹ BŁĄD:  catch (IllegalArgumentException | NumberFormatException e) { ... }
     *   3. Dlaczego catch (Exception e) w każdej metodzie to zły pomysł, a w pętli głównej programu — dobry?
     *   4. Co wypisze:
     *          try { throw new IllegalStateException("A"); }
     *          catch (IllegalStateException e) { throw new RuntimeException("B"); }
     *      (widziane z metody wywołującej, która łapie RuntimeException i wypisuje getMessage())
     *   5. Gdzie postawić try/catch przy przetwarzaniu 1000 wierszy, żeby jeden zły wiersz nie zatrzymał reszty?
     *   6. Czym jest „precise rethrow”?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: klasyfikacja", "ok,format,zero,zakres",
                () -> exercise1("20") + "," + exercise1("x") + "," + exercise1("0") + "," + exercise1("5"));
        Check.equal("ćw. 2: raport", "ok=3, błędy=2", () -> exercise2(List.of("20", "x", "11", "0", "100")));
        Check.equal("ćw. 3a: \"10/2\"", 5, () -> exercise3("10/2"));
        Check.equal("ćw. 3b: błędne wyrażenia → -1", "-1,-1,-1",
                () -> exercise3("10/0") + "," + exercise3("dziesięć/2") + "," + exercise3("10"));
        Check.equal("ćw. 4: lista błędów", List.of("#1: NumberFormatException", "#3: ArithmeticException"),
                () -> exercise4(List.of("50", "?", "25", "0")));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "ok,format,zero,zakres",
                () -> solution1("20") + "," + solution1("x") + "," + solution1("0") + "," + solution1("5"));
        Check.equal("ćw. 2 (wzorzec)", "ok=3, błędy=2", () -> solution2(List.of("20", "x", "11", "0", "100")));
        Check.equal("ćw. 3a (wzorzec)", 5, () -> solution3("10/2"));
        Check.equal("ćw. 3b (wzorzec)", "-1,-1,-1",
                () -> solution3("10/0") + "," + solution3("dziesięć/2") + "," + solution3("10"));
        Check.equal("ćw. 4 (wzorzec)", List.of("#1: NumberFormatException", "#3: ArithmeticException"),
                () -> solution4(List.of("50", "?", "25", "0")));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): wywołaj compute(input) i zwróć "ok", albo — zależnie od wyjątku —
     * "format" (NumberFormatException), "zero" (ArithmeticException), "zakres" (ArrayIndexOutOfBoundsException).
     */
    static String exercise1(String input) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /** ĆWICZENIE 2 (średnie): przetwórz wszystkie dane compute i zwróć raport "ok=N, błędy=M". Podpowiedź: sekcja 6. */
    static String exercise2(List<String> inputs) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 3 (średnie): oblicz wyrażenie "a/b" (dzielenie całkowite). Jakikolwiek błąd — zły format liczby,
     * dzielenie przez zero, brak "/" (split da tablicę z jednym elementem → parts[1] poza tablicą) — ma dać -1.
     * Użyj JEDNEGO multi-catch z trzema typami. Podpowiedź: String[] parts = text.split("/");
     */
    static int exercise3(String text) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zwróć listę opisów błędów w formacie "#INDEKS: TypWyjątku" (indeks od 0) dla danych,
     * dla których compute rzuca wyjątek. Dane poprawne pomiń. Przykład: ["50", "?", "25", "0"] →
     * ["#1: NumberFormatException", "#3: ArithmeticException"].
     */
    static List<String> exercise4(List<String> inputs) {
        // TODO: twoje rozwiązanie
        return new ArrayList<>();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(String input) {
        try {
            compute(input);
            return "ok";
        } catch (NumberFormatException e) {
            return "format";
        } catch (ArithmeticException e) {
            return "zero";
        } catch (ArrayIndexOutOfBoundsException e) {
            return "zakres";
        }
    }

    static String solution2(List<String> inputs) {
        int ok = 0;
        int errors = 0;
        for (String in : inputs) {
            try {
                compute(in);
                ok++;
            } catch (RuntimeException e) {
                errors++;
            }
        }
        return "ok=" + ok + ", błędy=" + errors;
    }

    static int solution3(String text) {
        try {
            String[] parts = text.split("/");
            return Integer.parseInt(parts[0]) / Integer.parseInt(parts[1]);
        } catch (NumberFormatException | ArithmeticException | ArrayIndexOutOfBoundsException e) {
            return -1;
        }
    }

    static List<String> solution4(List<String> inputs) {
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            try {
                compute(inputs.get(i));
            } catch (RuntimeException e) {
                errors.add("#" + i + ": " + e.getClass().getSimpleName());
            }
        }
        return errors;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. „poza tablicą (Index 25 out of bounds for length 10)” — 100 / 4 = 25, a tablica ma indeksy 0..9.
     *   2. NumberFormatException jest podklasą IllegalArgumentException — w multi-catch typy nie mogą być spokrewnione
     *      (błąd kompilacji). Wystarczy catch (IllegalArgumentException e).
     *   3. W zwykłej metodzie ukrywa błędy programisty i łapie wyjątki, których nikt nie umie obsłużyć — program działa
     *      dalej w złym stanie. W pętli głównej chroni całą aplikację przed upadkiem z powodu jednego żądania,
     *      a błąd zostaje zapisany.
     *   4. „B” — wyjątek z catch zastąpił oryginalny „A” (który przepadł, bo nie został podany jako przyczyna).
     *   5. WEWNĄTRZ pętli, wokół przetwarzania jednego wiersza; błędy zbierane do listy/raportu.
     *   6. catch (Exception e) { ...; throw e; } — kompilator wie, jakie wyjątki SPRAWDZANE naprawdę mogą polecieć z try,
     *      więc metoda deklaruje tylko je (np. throws IOException), a nie throws Exception (Java 7+).
     */
    // </editor-fold>
}
