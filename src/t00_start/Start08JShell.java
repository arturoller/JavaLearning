package t00_start;

import helpers.Check;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import jdk.jshell.EvalException;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;
import jdk.jshell.SourceCodeAnalysis;
import jdk.jshell.VarSnippet;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: JShell — Java w trybie „wpisz i zobacz wynik”, bez klasy i bez metody main
 *        (JShell = powłoka Javy, REPL: Read-Eval-Print Loop = czytaj-wykonaj-wypisz w pętli;
 *         snippet = fragment kodu wpisany do JShell)
 *
 * W SKRÓCIE:
 *   JShell (od Javy 9, JEP 222) pozwala wpisać jedno wyrażenie lub instrukcję i od razu zobaczyć wynik,
 *   bez pisania klasy, metody main, kompilowania i uruchamiania. To najszybszy sposób na sprawdzenie, co robi
 *   metoda z JDK, ile wynosi działanie albo jak zachowuje się jakaś pułapka. W tej lekcji uruchamiamy JShell
 *   z poziomu programu (pakiet jdk.jshell), więc zobaczysz, co dokładnie dzieje się po wpisaniu fragmentu.
 *
 * ANALOGIA: kalkulator z pamięcią zamiast arkusza z formularzem.
 *   Do jednego działania nie zakładasz dokumentu, tylko naciskasz klawisze i widzisz wynik. Kalkulator pamięta
 *   poprzednie wyniki (zmienne $1, $2...) i możesz do nich wracać. Program z metodą main to arkusz: dłużej,
 *   ale zostaje po nim gotowy, powtarzalny plik.
 *
 * JAK TO DZIAŁA:
 *   Uruchomienie:  w terminalu wpisz  jshell   (polecenie jest w katalogu bin Twojego JDK),
 *                  w IntelliJ: Tools → JShell Console...  (konsola z podpowiedziami i kolorami)
 *
 *     jshell  int x = 2 + 3;          wypisze:  x {@code ==>} 5
 *     jshell  x * 10                  wypisze:  $2 {@code ==>} 50          ($2 = zmienna tymczasowa, nadana automatycznie)
 *     jshell  "ala".toUpperCase()     wypisze:  $3 {@code ==>} "ALA"
 *     jshell  /exit                   kończy pracę
 *
 *   Każdy wpisany fragment (snippet) przechodzi: analiza → kompilacja → wykonanie → wynik. Wynik ma STATUS:
 *     VALID (przyjęty), REJECTED (odrzucony: błąd kompilacji), RECOVERABLE_DEFINED (przyjęty, ale odwołuje się
 *     do czegoś jeszcze niezdefiniowanego), OVERWRITTEN (zastąpiony nowszą definicją), DROPPED (usunięty).
 *   W programie robi to metoda eval (od evaluate = oceń, wykonaj), która zwraca listę zdarzeń (SnippetEvent).
 *
 * SŁÓWKA: shell = powłoka; snippet = fragment kodu; evaluate = wykonaj, oceń; value = wartość; status = stan;
 *   scratch variable = zmienna tymczasowa (brudnopis); forward reference = odwołanie „w przód” (do czegoś
 *   zdefiniowanego później); redefine = zdefiniuj na nowo; diagnostic = diagnoza (komunikat kompilatora);
 *   completion = uzupełnianie; drop = porzuć; reset = zacznij od zera; import = import (dostęp do klas z pakietu)
 *
 * ZOBACZ TEŻ: t00_start/Start05Debugging (inne sposoby sprawdzania kodu), t04_strings/Strings02Methods (metody
 *   String, które warto sprawdzić w JShell), t15_numbers/Numbers01BigDecimal (liczby i pułapki, które wypróbujesz w JShell)
 * </pre>
 */
public class Start08JShell {

    public static void main(String[] args) {
        title("Start08 — JShell");

        firstSnippets();      // first snippets = pierwsze fragmenty
        scratchVariables();   // scratch variables = zmienne tymczasowe ($1, $2...)
        methodsAndForward();  // methods and forward references = metody i odwołania w przód
        errors();             // errors = błędy
        imports();            // imports = importy
        experiments();        // experiments = eksperymenty z API
        analysis();           // analysis = analiza kodu i uzupełnianie
        dropAndReset();       // drop and reset = porzucanie i zaczynanie od zera
        commands();           // commands = polecenia narzędzia a API
        exercises();          // exercises = ćwiczenia
    }

    // =================================================================================================
    // NARZĘDZIA LEKCJI
    // =================================================================================================

    /**
     * Tworzy JShell działający W TYM SAMYM procesie ("local" = lokalny silnik wykonywania). Wydruki
     * i błędy narzędzia trafiają do podanego bufora (ByteArrayOutputStream = bufor w pamięci), więc nic
     * nie miesza się z wydrukami lekcji. Zwykłe narzędzie jshell uruchamia kod w OSOBNYM procesie.
     */
    static JShell newShell() {
        PrintStream sink = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);   // sink = ujście
        return JShell.builder().executionEngine("local").out(sink).err(sink).build();   // build = zbuduj
    }

    /** Wykonuje fragment i opisuje PIERWSZE zdarzenie jako „STATUS wartość” (value = null, gdy brak wartości). */
    static String run(JShell js, String code) {
        SnippetEvent event = js.eval(code).get(0);
        return event.status() + " " + event.value();
    }

    /** Ładuje importy, które narzędzie jshell dodaje samo (skrypt startowy DEFAULT w JDK 17). */
    static void useDefaultImports(JShell js) {
        for (String pkg : List.of("java.io", "java.math", "java.net", "java.nio.file", "java.util",
                "java.util.concurrent", "java.util.function", "java.util.prefs", "java.util.regex", "java.util.stream")) {
            js.eval("import " + pkg + ".*;");
        }
    }

    // =================================================================================================
    // 1. PIERWSZE FRAGMENTY
    // =================================================================================================

    /**
     * 1. Bez klasy i bez main: deklaracja zmiennej, wyrażenie, wywołanie metody. Dla każdego fragmentu dostajemy
     * status i wartość. Średnik na końcu jest w JShell opcjonalny. Błędny fragment nie przerywa pracy — jest
     * tylko odrzucony, a Ty piszesz dalej.
     */
    static void firstSnippets() {
        section("1. Pierwsze fragmenty: status i wartość");

        try (JShell js = newShell()) {                  // try-with-resources = zamknij JShell automatycznie (jak /exit)
            show("int x = 2 + 3;", run(js, "int x = 2 + 3;"));
            // WYNIK: int x = 2 + 3; → VALID 5
            show("x * 10", run(js, "x * 10"));
            // WYNIK: x * 10 → VALID 50
            show("\"ala\".toUpperCase()", run(js, "\"ala\".toUpperCase()"));
            // WYNIK: "ala".toUpperCase() → VALID "ALA"
            show("int y = ;", run(js, "int y = ;"));
            // WYNIK: int y = ; → REJECTED null
            show("x + 1 (x nadal istnieje)", run(js, "x + 1"));
            // WYNIK: x + 1 (x nadal istnieje) → VALID 6
        }

        // Zwróć uwagę: łańcuchy znaków wartość podaje W CUDZYSŁOWIE ("ALA"), a znaki w apostrofach ('b') —
        // tak jak zapisałbyś je w kodzie. To pomaga odróżnić napis "5" od liczby 5.
        //
        // PUŁAPKA: silnik "local" wykonuje Twój kod w TYM samym procesie. Fragment z System.exit(0) zakończyłby
        // cały program, a pętla bez końca zawiesiłaby go. Narzędzie jshell z terminala działa bezpieczniej: kod idzie
        // do osobnego procesu (silnik „jdi”), który można zatrzymać. Dlatego w lekcji nie wypisujemy niczego
        // przez System.out.println wewnątrz fragmentów — w trybie local wydruk z fragmentu trafia na System.out
        // CAŁEGO programu, nie do bufora narzędzia.
        //
        // Skąd wynik dla fragmentu bez zmiennej? JShell sam tworzy zmienną tymczasową ($1, $2...) — o tym w sekcji 2.
    }

    // =================================================================================================
    // 2. ZMIENNE TYMCZASOWE
    // =================================================================================================

    /**
     * 2. Wyrażenie bez przypisania dostaje zmienną tymczasową {@code $1}, {@code $2}... — możesz jej użyć
     * w kolejnych fragmentach. To jak klawisz „pamięć” w kalkulatorze. Polecenie {@code /vars} pokazuje
     * wszystkie zmienne; w API odpowiada mu {@code js.variables()}.
     */
    static void scratchVariables() {
        section("2. Zmienne tymczasowe $1, $2... i polecenie /vars");

        try (JShell js = newShell()) {
            js.eval("int x = 5;");
            SnippetEvent first = js.eval("x * 2").get(0);
            show("nazwa zmiennej dla 'x * 2'", ((VarSnippet) first.snippet()).name());   // snippet() = fragment
            // WYNIK: nazwa zmiennej dla 'x * 2' → $1
            show("wartość", first.value());
            // WYNIK: wartość → 10
            show("$1 + 1", run(js, "$1 + 1"));
            // WYNIK: $1 + 1 → VALID 11
            js.eval("String s = \"ala\";");

            List<String> vars = new ArrayList<>();      // odpowiednik polecenia /vars
            js.variables().forEach(v -> vars.add(v.name() + " : " + v.typeName() + " = " + js.varValue(v)));
            // variables = zmienne; typeName = nazwa typu; varValue = wartość zmiennej
            vars.sort(null);                            // sort(null) = porządek naturalny (alfabetyczny)
            showEach("/vars", vars);
            // WYNIK: /vars (liczba elementów: 4):
            // WYNIK: • $1 : int = 10
            // WYNIK: • $2 : int = 11
            // WYNIK: • s : String = "ala"
            // WYNIK: • x : int = 5
        }

        // W prawdziwym jshell wygląda to tak (| na początku to prefiks komunikatów narzędzia):
        //     jshell> int x = 5
        //     x ==> 5
        //     jshell> x * 2
        //     $2 ==> 10
        // (W narzędziu numer przy $ to numer fragmentu w sesji: po `int x = 5` pierwsze wyrażenie dostaje $2.
        // W API, którego używamy w lekcji, domyślny generator numeruje zmienne tymczasowe po kolei od $1 — stąd
        // różnica. Nigdy nie zakładaj konkretnego numeru — czytaj go z ekranu.)
        //
        // Przydatne: strzałka w górę przywołuje poprzednie polecenia; /! powtarza ostatnie, a /-2 — przedostatnie.
        //
        // PUŁAPKA: zmienne $N są zwykłymi zmiennymi sesji, ale numery zależą od kolejności wpisywania. Do rzeczy,
        // których będziesz używać dłużej, nadaj własną nazwę: Shift+Tab, potem V zamienia ostatnie wyrażenie w
        // zmienną z nazwą, którą wpiszesz. (Shift+Tab, M robi z wyrażenia metodę, Shift+Tab, I dodaje brakujący import.)
    }

    // =================================================================================================
    // 3. METODY, ODWOŁANIA W PRZÓD, REDEFINICJA
    // =================================================================================================

    /**
     * 3. W JShell definiujesz metody wprost, bez klasy. Można odwołać się do metody, której jeszcze nie ma
     * (odwołanie w przód): fragment dostaje status RECOVERABLE_DEFINED, a gdy brakującą metodę dopiszesz,
     * zaczyna działać. Metodę można też zdefiniować na nowo — wszystko, co jej używa, zobaczy nową wersję.
     */
    static void methodsAndForward() {
        section("3. Metody, odwołania w przód i definiowanie na nowo");

        try (JShell js = newShell()) {
            show("f2 używa g2, którego jeszcze nie ma", run(js, "int f2() { return g2() + 1; }"));
            // WYNIK: f2 używa g2, którego jeszcze nie ma → RECOVERABLE_DEFINED null
            SnippetEvent call = js.eval("f2()").get(0);
            show("wywołanie f2() przed g2", call.exception() == null ? "bez wyjątku" : call.exception().getClass().getSimpleName());
            // WYNIK: wywołanie f2() przed g2 → UnresolvedReferenceException
            show("zdarzenia po dodaniu g2", js.eval("int g2() { return 41; }").stream().map(e -> e.status().toString()).toList());
            // WYNIK: zdarzenia po dodaniu g2 → [VALID, VALID]
            show("f2() po dodaniu g2", run(js, "f2()"));
            // WYNIK: f2() po dodaniu g2 → VALID 42
            show("zdarzenia po ZMIANIE g2", js.eval("int g2() { return 99; }").stream().map(e -> e.status().toString()).toList());
            // WYNIK: zdarzenia po ZMIANIE g2 → [VALID, VALID, OVERWRITTEN]
            show("f2() po zmianie g2", run(js, "f2()"));
            // WYNIK: f2() po zmianie g2 → VALID 100

            List<String> methods = new ArrayList<>();   // odpowiednik polecenia /methods
            js.methods().forEach(m -> methods.add(m.name() + m.signature()));    // signature = podpis: (parametry)wynik
            methods.sort(null);
            show("/methods", methods);
            // WYNIK: /methods → [f2()int, g2()int]
        }

        // W jshell:
        //     jshell> int f2() { return g2() + 1; }
        //     |  created method f2(), however, it cannot be invoked until method g2() is declared
        //     (utworzono metodę f2(), ale nie można jej wywołać, dopóki nie zadeklarujesz g2())
        //
        // Dzięki temu kolejność definicji nie ma znaczenia — możesz najpierw opisać „co”, a „jak” dopisać później.
        // (W zwykłej klasie kolejność metod też nie ma znaczenia; w JShell dotyczy to także fragmentów wpisywanych po kolei.)
        //
        // W JShell zmienne i metody są „na górze” (poza klasą), więc modyfikatory private, static, public są ignorowane
        // (narzędzie ostrzega i pomija). To ułatwienie jest TYLKO w JShell; w pliku .java kod musi być w klasie.
        // Zmienna zdefiniowana na nowo (int x = 1; ... int x = 2;) zastępuje starą i traci wartość; metoda
        // zdefiniowana na nowo — jak wyżej — działa dla wszystkich wywołań, także z wcześniej napisanych metod.
        //
        // DOBRA PRAKTYKA: zrób metodę w JShell, żeby ją sprawdzić na szybko, a gdy działa — przenieś do pliku .java
        // i do testów (t32_junit_mockito). W JShell polecenie /edit otwiera okno edycji wszystkich fragmentów, a
        // /save plik.jsh zapisuje je do pliku (/open plik.jsh wczytuje — skrypt startowy bywa wygodny).
    }

    // =================================================================================================
    // 4. BŁĘDY I WYJĄTKI
    // =================================================================================================

    /**
     * 4. Są dwa rodzaje kłopotów: błąd KOMPILACJI (fragment odrzucony — REJECTED, a powód podaje diagnoza)
     * i WYJĄTEK w trakcie działania (fragment przyjęty — VALID, ale zdarzenie niesie wyjątek). W obu
     * przypadkach sesja działa dalej.
     */
    static void errors() {
        section("4. Błędy kompilacji i wyjątki");

        try (JShell js = newShell()) {
            SnippetEvent bad = js.eval("int twice(int n) { return n * 2 }").get(0);     // brak średnika w środku metody
            show("metoda bez średnika", bad.status());
            // WYNIK: metoda bez średnika → REJECTED
            List<String> codes = new ArrayList<>();
            js.diagnostics(bad.snippet()).forEach(d -> codes.add(d.getCode() + (d.isError() ? " (błąd)" : " (ostrzeżenie)")));
            show("kod diagnozy kompilatora", codes);    // diagnostics = diagnozy; getCode = kod komunikatu; isError = czy błąd
            // WYNIK: kod diagnozy kompilatora → [compiler.err.expected (błąd)]
            show("literówka w nazwie", js.eval("Math.sqroot(4)").get(0).status());
            // WYNIK: literówka w nazwie → REJECTED

            SnippetEvent div = js.eval("1 / 0").get(0);
            show("1 / 0: status", div.status());
            // WYNIK: 1 / 0: status → VALID
            if (div.exception() instanceof EvalException ex) {      // EvalException = wyjątek zgłoszony w trakcie wykonania fragmentu
                show("klasa wyjątku", ex.getExceptionClassName());
                // WYNIK: klasa wyjątku → java.lang.ArithmeticException
                show("komunikat", ex.getMessage());
                // WYNIK: komunikat → / by zero
            }
            SnippetEvent parse = js.eval("Integer.parseInt(\"x\")").get(0);
            show("parseInt(\"x\"): wyjątek", ((EvalException) parse.exception()).getExceptionClassName());
            // WYNIK: parseInt("x"): wyjątek → java.lang.NumberFormatException
            show("po wyjątku sesja działa dalej", run(js, "6 * 7"));
            // WYNIK: po wyjątku sesja działa dalej → VALID 42
        }

        // Błąd kompilacji: JShell pokazuje komunikat tak jak javac (z wskazaniem miejsca). Komunikaty kompilatora
        // uczą się czytać od początku — w kodzie diagnozy (compiler.err....) jest nazwa rodzaju błędu.
        // Wyjątek w czasie działania: w narzędziu zobaczysz stos wywołań, ale zamiast numerów linii w pliku jest
        // oznaczenie fragmentu, np.  at (#3:1)  — fragment numer 3, linia 1.
        //
        // PUŁAPKA: wartość fragmentu, który rzucił wyjątek, jest null (nic nie zwrócił), a zmienna tymczasowa
        // powstała mimo to. Nie myl „VALID” z „udało się” — VALID znaczy tylko „skompilowało się”.
        //
        // DOBRA PRAKTYKA: komunikat kompilatora czytaj od góry, pierwszy błąd bywa przyczyną pozostałych. A
        // wyjątki z JShell traktuj jak wskazówkę: skoro tu rzuca, to w programie też rzuci.
    }

    // =================================================================================================
    // 5. IMPORTY
    // =================================================================================================

    /**
     * 5. Narzędzie jshell uruchamia się z gotowymi importami (java.util.*, java.io.*, java.util.stream.* i kilka
     * innych), dlatego możesz od razu pisać {@code new ArrayList<>()}. Ale dzieje się to dzięki SKRYPTOWI STARTOWEMU —
     * silnik z API (jdk.jshell) zaczyna z pustą listą importów, więc musisz je dodać sam.
     */
    static void imports() {
        section("5. Importy: skąd JShell wie, co to ArrayList");

        try (JShell js = newShell()) {
            show("liczba importów na starcie (API)", js.imports().count());
            // WYNIK: liczba importów na starcie (API) → 0
            show("new ArrayList<String>() bez importu", run(js, "var list = new ArrayList<String>();"));
            // WYNIK: new ArrayList<String>() bez importu → REJECTED null
            show("import java.util.*;", run(js, "import java.util.*;"));
            // WYNIK: import java.util.*; → VALID null
            show("to samo z importem", run(js, "var list = new ArrayList<String>();"));
            // WYNIK: to samo z importem → VALID []
            show("list.add(\"b\")", run(js, "list.add(\"b\")"));
            // WYNIK: list.add("b") → VALID true
            show("list", run(js, "list"));
            // WYNIK: list → VALID [b]
            show("liczba importów po dodaniu", js.imports().count());
            // WYNIK: liczba importów po dodaniu → 1
        }

        // Domyślny skrypt startowy narzędzia jshell (JDK 17) to dziesięć importów:
        //   java.io.*  java.math.*  java.net.*  java.nio.file.*  java.util.*  java.util.concurrent.*
        //   java.util.function.*  java.util.prefs.*  java.util.regex.*  java.util.stream.*
        // Pozostałe klasy (np. java.time.LocalDate) dopisz: import java.time.*; — albo użyj pełnej nazwy
        // java.time.LocalDate.now(). Polecenie /imports pokazuje bieżącą listę. Własne importy na stałe: skrypt
        // startowy (jshell --startup plik) albo polecenie /set start.
        //
        // PUŁAPKA: „w jshell działa, a w mojej klasie nie” — w pliku .java importy MUSISZ napisać sam (IntelliJ:
        // Alt+Enter na czerwonej nazwie → Import class, albo Ctrl+Alt+O porządkuje importy).
        //
        // CIEKAWOSTKA: wartość listy to jej toString(), więc [b], a łańcuchy znaków dostają cudzysłów — poznasz po
        // tym, że wynik jest napisem. Dla tablic JShell pokazuje czytelny opis (int[2][] { ... }), a nie zapis z kodem.
    }

    // =================================================================================================
    // 6. SPRAWDZANIE ZACHOWANIA API — NAJLEPSZE ZASTOSOWANIE
    // =================================================================================================

    /**
     * 6. Najlepsze zastosowanie JShell: „zamiast zgadywać, sprawdź”. Szybkie pytania o zachowanie metody, o
     * kolejność działań, o zaokrąglanie. Każdy wiersz poniżej to jedno wpisanie w JShell.
     */
    static void experiments() {
        section("6. Zamiast zgadywać — sprawdź");

        try (JShell js = newShell()) {
            useDefaultImports(js);                      // tak jak robi to narzędzie jshell
            for (String expression : List.of("10 / 4", "10 / 4.0", "0.1 + 0.2", "Integer.MAX_VALUE + 1", "Math.round(-2.5)",
                    "\"a,b,,\".split(\",\").length", "\"x\" + 1 + 2", "'a' + 1", "(char) ('a' + 1)",
                    "List.of(3, 1, 2).stream().sorted().toList()")) {
                show(expression, run(js, expression));
            }
            // WYNIK: 10 / 4 → VALID 2
            // WYNIK: 10 / 4.0 → VALID 2.5
            // WYNIK: 0.1 + 0.2 → VALID 0.30000000000000004
            // WYNIK: Integer.MAX_VALUE + 1 → VALID -2147483648
            // WYNIK: Math.round(-2.5) → VALID -2
            // WYNIK: "a,b,,".split(",").length → VALID 2
            // WYNIK: "x" + 1 + 2 → VALID "x12"
            // WYNIK: 'a' + 1 → VALID 98
            // WYNIK: (char) ('a' + 1) → VALID 'b'
            // WYNIK: List.of(3, 1, 2).stream().sorted().toList() → VALID [1, 2, 3]
        }

        // Co z tego wynika (każda linia to „pułapka z życia”):
        //   10 / 4 = 2 (dzielenie CAŁKOWITE ucina część ułamkową), 10 / 4.0 = 2.5
        //   0.1 + 0.2 = 0.30000000000000004 (liczby double są przybliżone; do pieniędzy BigDecimal — t15_numbers)
        //   Integer.MAX_VALUE + 1 = -2147483648 (przepełnienie int zawija się bez ostrzeżenia)
        //   Math.round(-2.5) = -2 (zaokrągla w górę w stronę plus nieskończoności, nie „od zera”)
        //   "a,b,,".split(",").length = 2 (puste elementy na końcu są usuwane!)
        //   "x" + 1 + 2 = "x12" (dodawanie od lewej: napis + liczba = napis), a 'a' + 1 = 98 (znak to liczba)
        //
        // DOBRA PRAKTYKA: gdy w kodzie masz wątpliwość („czy split zostawia puste elementy?”), NIE zgaduj i nie
        // szukaj godzinę w Internecie — wpisz to do JShell. Dwie minuty, pewna odpowiedź i zapamiętasz ją
        // lepiej niż z dokumentacji.
        //
        // KIEDY JSHELL, A KIEDY NIE:
        //   TAK:  eksperyment z jedną metodą / klasą JDK, szybkie obliczenia, nauka (sprawdzam własne odpowiedzi
        //         na pytania „Co wypisze?”), próby wyrażeń regularnych i formatów, prototyp 3–10 linii.
        //   NIE:  kod, który ma zostać (nie ma pliku, nie ma testów, brak kontroli wersji), program z wieloma
        //         klasami, praca z debuggerem (Start05Debugging), pomiary szybkości (kod w JShell nie jest
        //         reprezentatywny), cokolwiek, co zależy od struktury projektu i bibliotek (Maven).
        //   Z bibliotekami: jshell --class-path ścieżka_do.jar (albo polecenie /env --class-path ...).
    }

    // =================================================================================================
    // 7. ANALIZA KODU I UZUPEŁNIANIE (jak Tab)
    // =================================================================================================

    /**
     * 7. JShell potrafi ocenić, czy wpisany tekst jest KOMPLETNYM fragmentem (to dzięki temu narzędzie wie,
     * kiedy czekać na kolejną linię) i podpowiada uzupełnienia — to ono działa po naciśnięciu Tab.
     */
    static void analysis() {
        section("7. Analiza kodu: kompletność i podpowiedzi (Tab)");

        try (JShell js = newShell()) {
            SourceCodeAnalysis analyzer = js.sourceCodeAnalysis();      // analyzer = analizator
            for (String input : List.of("int x = 1", "int x = 1;", "if (true) {", "x + 1", "int a = 1; int b = 2;")) {
                SourceCodeAnalysis.CompletionInfo info = analyzer.analyzeCompletion(input);   // completeness = kompletność
                show(input, info.completeness() + (info.remaining().isBlank() ? "" : "  (reszta: " + info.remaining().strip() + ")"));
                // remaining = pozostały tekst; strip = obetnij białe znaki (Java 11+)
            }
            // WYNIK: int x = 1 → COMPLETE_WITH_SEMI
            // WYNIK: int x = 1; → COMPLETE
            // WYNIK: if (true) { → DEFINITELY_INCOMPLETE  (reszta: if (true) {)
            // WYNIK: x + 1 → COMPLETE
            // WYNIK: int a = 1; int b = 2; → COMPLETE  (reszta: int b = 2;)

            TreeSet<String> suggestions = new TreeSet<>();       // TreeSet = zbiór posortowany, bez powtórzeń
            analyzer.completionSuggestions("Math.ab", 7, new int[1]).forEach(s -> suggestions.add(s.continuation()));
            // 7 = pozycja kursora na końcu tekstu; continuation = proponowane dokończenie
            show("podpowiedzi po 'Math.ab' + Tab", suggestions);
            // WYNIK: podpowiedzi po 'Math.ab' + Tab → [abs(, absExact(]
        }

        // Statusy kompletności: COMPLETE (gotowy), COMPLETE_WITH_SEMI (gotowy po dopisaniu średnika — dlatego w JShell
        // średnik jest opcjonalny), DEFINITELY_INCOMPLETE (na pewno brak ciągu dalszego: otwarty nawias klamrowy,
        // narzędzie pokaże znak zachęty ...> i czeka na kolejną linię), CONSIDERED_INCOMPLETE (wygląda na niekompletny)
        // i UNKNOWN (nie da się ocenić). Gdy wpiszesz kilka instrukcji w jednej linii, są wykonywane jako osobne
        // fragmenty — „reszta” trafia do następnego.
        //
        // TAB w narzędziu:  Tab = uzupełnij nazwę (lub pokaż możliwości),  Tab Tab = dokumentacja metody/klasy,
        // Shift+Tab, V (zmienna) · M (metoda) · I (import) — jak w sekcji 2.
        //
        // DOBRA PRAKTYKA: używaj Tab bez wstydu — jedno uzupełnienie „Math.” pokaże ci wszystkie metody klasy Math,
        // zanim sięgniesz po dokumentację.
    }

    // =================================================================================================
    // 8. PORZUCANIE I ZACZYNANIE OD ZERA
    // =================================================================================================

    /**
     * 8. {@code /drop} usuwa wskazany fragment (np. zmienną), {@code /reset} czyści całą sesję. W API: drop
     * dla jednego fragmentu, a reset to po prostu NOWA instancja JShell.
     */
    static void dropAndReset() {
        section("8. /drop i /reset");

        try (JShell js = newShell()) {
            Snippet zz = js.eval("int zz = 1;").get(0).snippet();
            show("zz istnieje", run(js, "zz + 1"));
            // WYNIK: zz istnieje → VALID 2
            show("/drop zz", js.drop(zz).get(0).status());      // drop = porzuć fragment
            // WYNIK: /drop zz → DROPPED
            show("zz po /drop", run(js, "zz + 1"));
            // WYNIK: zz po /drop → REJECTED null
        }
        try (JShell fresh = newShell()) {               // „/reset” = nowa sesja bez żadnych zmiennych
            show("zmiennych w nowej sesji", fresh.variables().count());
            // WYNIK: zmiennych w nowej sesji → 0
        }

        // Dobry zwyczaj: po dłuższej sesji /reset, żeby stare zmienne nie zafałszowały wyniku — „u mnie w JShell
        // działa” często znaczy „bo została stara zmienna z godzinę temu”.
        //
        // PUŁAPKA: stan sesji (zmienne, metody) ZNIKA po /exit. Jeśli chcesz coś zachować: /save plik.jsh (ew.
        // /save -history), a potem /open plik.jsh. Ale prawdziwy kod — do pliku .java w projekcie i pod kontrolę
        // wersji (Start06Git).
    }

    // =================================================================================================
    // 9. POLECENIA NARZĘDZIA A API
    // =================================================================================================

    /** Polecenie narzędzia jshell i jego odpowiednik w programie. */
    record Command(String command, String api) { }

    static final List<Command> COMMANDS = List.of(
            new Command("/vars", "js.variables() — lista zmiennych"),
            new Command("/methods", "js.methods() — lista metod"),
            new Command("/types", "js.types() — klasy, interfejsy, rekordy, enum"),
            new Command("/imports", "js.imports() — importy"),
            new Command("/list", "js.snippets() — wszystkie fragmenty sesji"),
            new Command("/drop nazwa", "js.drop(fragment) — usuń fragment"),
            new Command("/reset", "nowa instancja JShell"),
            new Command("/exit", "js.close() — zamknij sesję"),
            new Command("/open plik, /save plik", "zwykłe czytanie i zapisywanie plików (API tego nie robi)"),
            new Command("/help, /edit, /history, /set", "tylko w narzędziu (sama pomoc, edytor, historia, ustawienia)"));

    /**
     * 9. Zestawienie poleceń, które zaczynają się od ukośnika. Odróżnij je od kodu Javy: wszystko, co zaczyna się
     * od {@code /}, to polecenie narzędzia, a nie fragment kodu.
     */
    static void commands() {
        section("9. Polecenia /... i ich odpowiedniki w API");

        List<String> lines = COMMANDS.stream().map(c -> c.command() + " → " + c.api()).toList();
        showEach("polecenia jshell", lines);
        // WYNIK: polecenia jshell (liczba elementów: 10):
        // WYNIK: • /vars → js.variables() — lista zmiennych
        // WYNIK: • /methods → js.methods() — lista metod
        // WYNIK: • /types → js.types() — klasy, interfejsy, rekordy, enum
        // WYNIK: • /imports → js.imports() — importy
        // WYNIK: • /list → js.snippets() — wszystkie fragmenty sesji
        // WYNIK: • /drop nazwa → js.drop(fragment) — usuń fragment
        // WYNIK: • /reset → nowa instancja JShell
        // WYNIK: • /exit → js.close() — zamknij sesję
        // WYNIK: • /open plik, /save plik → zwykłe czytanie i zapisywanie plików (API tego nie robi)
        // WYNIK: • /help, /edit, /history, /set → tylko w narzędziu (sama pomoc, edytor, historia, ustawienia)

        // Polecenia warte zapamiętania na start: /help (pomoc), /vars, /methods, /list, /reset, /exit.
        // /set feedback concise — krótsze komunikaty;  /set feedback verbose — pełne;  /set feedback normal — domyślne.
        //
        // PUŁAPKA: uruchamiasz jshell, a system pisze, że nie zna polecenia — katalog bin Twojego JDK nie jest
        // w zmiennej PATH. Uruchom pełną ścieżką do jshell (w katalogu bin JDK) albo użyj IntelliJ: Tools → JShell Console.
        // Jeśli JShell w IntelliJ nie startuje, sprawdź, czy projekt używa JDK (nie samego JRE) i czy jest to
        // JDK 9 lub nowszy (nasz kurs używa 17).
        //
        // DOBRA PRAKTYKA: JShell uzupełnia, a nie zastępuje pliku .java. Traktuj go jak brudnopis: sprawdzasz
        // pomysł w minutę, a potem piszesz porządny kod z komentarzami, testami i historią w gicie.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • JShell = wpisz fragment, zobacz wynik: bez klasy, bez main, bez kompilowania. Start: jshell lub IntelliJ
     *     Tools → JShell Console.
     *   • Wyrażenie bez przypisania dostaje zmienną tymczasową $1, $2...; własną nazwę da Shift+Tab, V.
     *   • Statusy: VALID (przyjęty), REJECTED (błąd kompilacji), RECOVERABLE_DEFINED (brakuje czegoś), OVERWRITTEN.
     *   • VALID nie znaczy „bez błędu”: wyjątek w trakcie działania nie odrzuca fragmentu, tylko jest w zdarzeniu.
     *   • Metodę można odwołać „w przód” i zdefiniować na nowo — zależne fragmenty widzą nową wersję.
     *   • Narzędzie startuje z 10 gotowymi importami (java.util.*...); inne klasy dopisz importem.
     *   • Polecenia z ukośnikiem: /vars /methods /list /drop /reset /imports /help /edit /save /open /exit.
     *   • Najlepsze zastosowanie: sprawdzić zachowanie API i obliczenia. Nie: kod, który ma zostać.
     *   • Silnik "local" w API działa w tym samym procesie (System.exit zabije program).
     *
     * PYTANIA KONTROLNE:
     *   1. Czym jest zmienna $1 i skąd się bierze?
     *   2. Jaki status dostanie fragment int f() { return g(); } wpisany, zanim istnieje g()? Co się stanie
     *      przy wywołaniu f() i co po dopisaniu g()?
     *   3. Co wypisze JShell (jako wartość po wpisaniu kolejno):  int a = 7;  a / 2  a / 2.0  ?
     *   4. Co wypisze JShell (wartość):  "x" + 1 + 2  oraz  1 + 2 + "x"  ?
     *   5. ZNAJDŹ BŁĄD:  Wpisałeś w JShell  int twice(int n) { return n * 2 }  i dostałeś odrzucenie. Dlaczego?
     *   6. ZNAJDŹ BŁĄD:  Kod  var list = new ArrayList<String>();  wykonany przez API (JShell.builder()...)
     *      zostaje odrzucony, choć w narzędziu jshell działa. Dlaczego i jak naprawić?
     *   7. Czym różni się błąd kompilacji od wyjątku w JShell — jaki status mają ich fragmenty?
     *   8. Kiedy lepiej zrobić eksperyment w JShell, a kiedy w pliku .java?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: wartość 6 * 7", "42", () -> exercise1("6 * 7"));
        Check.equal("ćw. 1: wartość napisu", "\"ALA\"", () -> exercise1("\"ala\".toUpperCase()"));
        Check.equal("ćw. 2: statusy kolejnych fragmentów", List.of("VALID", "VALID", "REJECTED", "VALID"),
                () -> exercise2(List.of("int n = 4;", "n * n", "int m = ;", "n + 1")));
        Check.equal("ćw. 3: wyjątek", "java.lang.NumberFormatException", () -> exercise3("Integer.parseInt(\"x\")"));
        Check.equal("ćw. 3: brak wyjątku", "brak", () -> exercise3("1 + 1"));
        Check.equal("ćw. 4: sesja z redefinicją", "11", () -> exercise4(
                List.of("int f() { return 1; }", "int g() { return f() + 1; }", "int f() { return 10; }", "g()")));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "42", () -> solution1("6 * 7"));
        Check.equal("ćw. 1 (wzorzec, napis)", "\"ALA\"", () -> solution1("\"ala\".toUpperCase()"));
        Check.equal("ćw. 2 (wzorzec)", List.of("VALID", "VALID", "REJECTED", "VALID"),
                () -> solution2(List.of("int n = 4;", "n * n", "int m = ;", "n + 1")));
        Check.equal("ćw. 3 (wzorzec)", "java.lang.NumberFormatException", () -> solution3("Integer.parseInt(\"x\")"));
        Check.equal("ćw. 3 (wzorzec, brak)", "brak", () -> solution3("1 + 1"));
        Check.equal("ćw. 4 (wzorzec)", "11", () -> solution4(
                List.of("int f() { return 1; }", "int g() { return f() + 1; }", "int f() { return 10; }", "g()")));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): PRZEPISZ „program” na fragment JShell.
     * <pre>{@code
     * // PRZED (klasa + main + kompilacja + uruchomienie):
     * public class Test { public static void main(String[] args) { System.out.println(6 * 7); } }
     * // PO (jeden fragment):  6 * 7
     * }</pre>
     * Napisz metodę, która wykonuje podane wyrażenie w NOWEJ sesji JShell i zwraca samą wartość
     * (zdarzenie.value()), np. "42" albo "\"ALA\"".
     * Podpowiedź: try (JShell js = newShell()) { ... js.eval(expression).get(0).value() }.
     */
    static String exercise1(String expression) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): wykonaj fragmenty po kolei w JEDNEJ sesji (każdy widzi zmienne poprzednich) i zwróć
     * listę statusów pierwszego zdarzenia każdego fragmentu, np. ["VALID", "REJECTED"].
     * Podpowiedź: status() zamień na napis przez toString() lub String.valueOf.
     */
    static List<String> exercise2(List<String> snippets) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): wykonaj fragment i zwróć pełną nazwę klasy wyjątku zgłoszonego w trakcie
     * wykonania (np. "java.lang.NumberFormatException") albo napis "brak", gdy wyjątku nie było.
     * Podpowiedź: zdarzenie.exception() jest null, gdy brak wyjątku; w przeciwnym razie rzutuj na EvalException
     * i użyj getExceptionClassName().
     */
    static String exercise3(String code) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): wykonaj fragmenty w jednej sesji i zwróć WARTOŚĆ ostatniego z nich. Fragmenty
     * mogą definiować metody na nowo, np. ["int f() { return 1; }", "int g() { return f() + 1; }",
     * "int f() { return 10; }", "g()"] → "11" (g widzi nową wersję f). Zadbaj, by sesja została zamknięta.
     * Podpowiedź: zapamiętuj value() ostatniego eval; jedno wykonanie może dać kilka zdarzeń — wartość
     * zwróconą przez ostatni fragment daje PIERWSZE zdarzenie (get(0)) jego eval.
     */
    static String exercise4(List<String> session) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(String expression) {
        try (JShell js = newShell()) {
            return js.eval(expression).get(0).value();
        }
    }

    static List<String> solution2(List<String> snippets) {
        List<String> statuses = new ArrayList<>();
        try (JShell js = newShell()) {
            for (String snippet : snippets) {
                statuses.add(js.eval(snippet).get(0).status().toString());
            }
        }
        return statuses;
    }

    static String solution3(String code) {
        try (JShell js = newShell()) {
            SnippetEvent event = js.eval(code).get(0);
            if (event.exception() instanceof EvalException ex) {
                return ex.getExceptionClassName();
            }
            return "brak";
        }
    }

    static String solution4(List<String> session) {
        String last = null;
        try (JShell js = newShell()) {
            for (String snippet : session) {
                last = js.eval(snippet).get(0).value();
            }
        }
        return last;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. To zmienna tymczasowa: JShell tworzy ją automatycznie dla wyrażenia wpisanego bez przypisania
     *      (np. x * 10 → $2), żebyś mógł użyć wyniku w następnych fragmentach. Numer zależy od kolejności fragmentów.
     *   2. RECOVERABLE_DEFINED (zdefiniowana, ale niewykonywalna). Wywołanie f() rzuci wyjątek o nierozwiązanym
     *      odwołaniu (UnresolvedReferenceException). Po dopisaniu g() fragment f staje się VALID i działa.
     *   3. 7 (po deklaracji zmiennej wartość to 7), potem 3 (dzielenie całkowite), potem 3.5.
     *   4. "x12" (od lewej: napis + 1 = "x1", potem + 2 = "x12"), a  1 + 2 + "x" to "3x" (najpierw 1 + 2 = 3).
     *   5. Brakuje średnika po  n * 2  w ciele metody. Średnik jest opcjonalny tylko na końcu CAŁEGO fragmentu,
     *      a nie wewnątrz instrukcji w metodzie: return n * 2; wymaga średnika.
     *   6. Silnik z API zaczyna bez importów (ArrayList nie jest znany); skrypt startowy z importami to dodatek
     *      narzędzia jshell. Naprawa: najpierw wykonaj  import java.util.*;  (albo pełna nazwa java.util.ArrayList).
     *   7. Błąd kompilacji: fragment odrzucony (REJECTED), nic nie powstaje; diagnoza podaje powód. Wyjątek w
     *      trakcie działania: fragment jest VALID (skompilował się), a zdarzenie niesie wyjątek.
     *   8. JShell: krótki eksperyment, sprawdzenie zachowania metody JDK, obliczenie. Plik .java: wszystko, co ma
     *      zostać, ma testy, kilka klas, bibliotekę (Maven) albo wymaga debuggera.
     */
    // </editor-fold>
}
