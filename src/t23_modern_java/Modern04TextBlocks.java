package t23_modern_java;

import helpers.Check;
import java.util.List;
import java.util.Locale;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Bloki tekstowe — wieloliniowe napisy bez bałaganu (Java 15, JEP 378)
 *        (text block = blok tekstowy; incidental whitespace = przypadkowe białe znaki)
 *
 * W SKRÓCIE:
 *   Blok tekstowy zapisujesz między {@code """} a {@code """}. Może mieć wiele linii, zawierać cudzysłowy bez
 *   cudzysłowów ucieczki i nie wymaga {@code \n} ani plusów. To zwykły {@code String} — tylko wygodniej zapisany.
 *   Idealny na JSON, SQL, HTML i oczekiwane wyniki w testach.
 *
 * ANALOGIA:
 *   Stary sposób to dyktowanie listu przez telefon: „cudzysłów, Ala, cudzysłów, plus, backslash-n, plus...”.
 *   Blok tekstowy to położenie kartki na stole: piszesz tak, jak ma wyglądać wynik. Kompilator sam odetnie
 *   marginesy (wcięcie całego bloku w kodzie), tak jak drukarka odcina puste brzegi kartki.
 *
 * JAK TO DZIAŁA:
 *   1. Po otwierającym {@code """} MUSI być koniec linii — treść zaczyna się w następnej linii.
 *   2. Kompilator usuwa „przypadkowe” wcięcie: bierze najmniejsze wcięcie spośród wszystkich linii z treścią
 *      ORAZ linii z zamykającym {@code """} (jeśli stoi samotnie w swojej linii) i obcina je wszędzie.
 *      Dlatego położenie zamykającego {@code """} decyduje o wcięciu całości.
 *   3. Spacje na końcu każdej linii są usuwane (zachowasz je przez {@code \s}).
 *   4. Dopiero potem są interpretowane sekwencje ucieczki: {@code \n}, {@code \t}, {@code \"}, {@code \\},
 *      {@code \s} (spacja), {@code \<koniec linii>} (sklej z następną linią).
 *   5. Końce linii są zawsze zamieniane na {@code \n}, niezależnie od systemu i pliku źródłowego.
 *   6. Jeśli zamykające {@code """} stoi w osobnej linii, napis kończy się znakiem nowej linii.
 *
 *   Historia: podgląd w Javie 13 (JEP 355) i 14 (JEP 368); wersja ostateczna w Javie 15 (JEP 378).
 *
 * SŁÓWKA:
 *   text block = blok tekstowy; indentation = wcięcie; delimiter = ogranicznik (tu: potrójny cudzysłów);
 *   trailing = końcowy; escape sequence = sekwencja ucieczki; line terminator = koniec linii;
 *   line continuation = kontynuacja linii; strip = obetnij; translate = przetłumacz; formatted = sformatowany.
 *
 * ZOBACZ TEŻ: t04_strings/Strings01Basics (napisy), t04_strings/Strings04Formatting (formatowanie),
 *   t18_io_files/Io05JsonManual (JSON ręcznie), t29_jdbc_databases/Jdbc03PreparedStatement (SQL i bezpieczeństwo),
 *   t23_modern_java/Modern06ApiAdditions (indent, transform, lines)
 * </pre>
 */
public class Modern04TextBlocks {

    public static void main(String[] args) {
        title("Modern04 — bloki tekstowe");

        syntaxBeforeAfter();         // syntax before/after = składnia przed i po
        incidentalIndentation();     // incidental indentation = przypadkowe wcięcie
        trailingSpaces();            // trailing spaces = spacje na końcu linii
        lineContinuation();          // line continuation = kontynuacja linii
        quotesAndEscapes();          // quotes and escapes = cudzysłowy i sekwencje ucieczki
        lineEndings();               // line endings = końce linii
        stringMethods();             // string methods = metody klasy String
        realUses();                  // real uses = praktyczne zastosowania
        pitfalls();                  // pitfalls = pułapki
        exercises();                 // exercises = ćwiczenia
    }

    /** Pomocnicza: pokazuje niewidoczne znaki jako tekst (escape = ucieczka). */
    static String escape(String text) {
        return text.replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r");
    }

    /** Pomocnicza: drukuje każdą linię w pionowych kreskach, żeby było widać początek i koniec. */
    static void printLines(String text) {   // print lines = drukuj linie
        for (String line : text.lines().toList()) {   // lines (Java 11+) = linie; toList (Java 16+) = do listy
            System.out.println("   |" + line + "|");
        }
    }

    // =================================================================================================
    // 1. SKŁADNIA — PRZED I PO
    // =================================================================================================

    /**
     * 1. Ten sam JSON zapisany dwoma sposobami. Wersja ze sklejaniem ma plusy, cudzysłowy ucieczki
     * i ręczne {@code \n} — trudno ją czytać i łatwo w niej zrobić literówkę.
     */
    static void syntaxBeforeAfter() {
        section("1. Składnia: przed i po");

        // PRZED: sklejanie, \" i \n w każdej linii.
        String before = "{\n"
                + "  \"name\": \"Ala\",\n"
                + "  \"age\": 30\n"
                + "}\n";

        // PO (Java 15+): piszesz tak, jak ma wyglądać wynik.
        String after = """
                {
                  "name": "Ala",
                  "age": 30
                }
                """;
        printLines(after);
        // WYNIK: |{|
        // WYNIK: |  "name": "Ala",|
        // WYNIK: |  "age": 30|
        // WYNIK: |}|
        show("to ten sam napis?", before.equals(after));
        // WYNIK: to ten sam napis? → true
        show("typ", after.getClass().getSimpleName());
        // WYNIK: typ → String

        // Blok tekstowy to zwykły String: te same metody, ten sam typ, ta sama stała w puli napisów.
        // Pierwsza linia treści zaczyna się PO końcu linii z otwierającym """ — wcięcie samego „{” (16 spacji
        // w pliku) zostało obcięte, bo to margines kodu, a nie część tekstu.

        // PUŁAPKA: po otwierającym """ nie wolno pisać treści w tej samej linii:  String s = """abc""";  to błąd kompilacji.
        // DOBRA PRAKTYKA: wcinaj blok tekstowy o jeden poziom względem instrukcji, a zamykające """ ustaw
        //   w tej samej kolumnie co treść — wtedy wynik jest wyrównany do lewej, a kod czytelny.
    }

    // =================================================================================================
    // 2. PRZYPADKOWE WCIĘCIE
    // =================================================================================================

    /**
     * 2. Jak kompilator liczy wcięcie do obcięcia: bierze NAJMNIEJSZE wcięcie ze wszystkich linii z treścią
     * i z samotnej linii zamykającego ogranicznika. Przesuwając zamykające {@code """} w lewo, „zostawiasz”
     * część wcięcia w tekście; w prawo — nic się nie zmienia, bo i tak decyduje treść.
     */
    static void incidentalIndentation() {
        section("2. Przypadkowe wcięcie i pozycja zamykającego \"\"\"");

        // a) zamykające """ w tej samej kolumnie co treść → tekst wyrównany do lewej, na końcu \n
        String flush = """
                alfa
                  beta
                """;                                               // flush = równo (do lewej)
        show("a) zamykające na poziomie treści", escape(flush));
        // WYNIK: a) zamykające na poziomie treści → alfa\n  beta\n

        // b) zamykające """ cztery spacje w lewo od treści → te 4 spacje ZOSTAJĄ w każdej linii
        String keep = """
                alfa
                  beta
            """;                                                   // keep = zachowaj
        show("b) zamykające 4 spacje w lewo", escape(keep));
        // WYNIK: b) zamykające 4 spacje w lewo →     alfa\n      beta\n

        // c) zamykające """ na końcu ostatniej linii treści → brak końcowego \n
        String noNewline = """
                alfa
                  beta""";                                         // no newline = bez nowej linii
        show("c) zamykające w linii treści", escape(noNewline));
        // WYNIK: c) zamykające w linii treści → alfa\n  beta

        // d) zamykające """ głębiej niż treść → nie ma wpływu na wcięcie (liczy się najmniejsze wcięcie treści)
        String deeper = """
                alfa
                  beta
                        """;                                       // deeper = głębiej
        show("d) zamykające głębiej niż treść", escape(deeper));
        // WYNIK: d) zamykające głębiej niż treść → alfa\n  beta\n

        // Linie puste (same białe znaki) nie biorą udziału w liczeniu wcięcia i stają się pustymi liniami.
        String withBlank = """
                góra

                dół
                """;                                               // with blank = z pustą linią
        show("e) pusta linia w środku", escape(withBlank));
        // WYNIK: e) pusta linia w środku → góra\n\ndół\n

        // PUŁAPKA: wcięcia tabulatorami i spacjami NIE mieszaj. Kompilator liczy każdy biały znak jako 1 znak,
        //   więc tabulator „wygląda” jak 4 spacje w edytorze, a liczy się jako jeden. Wynik bywa zaskakujący.
        // DOBRA PRAKTYKA: ustaw zamykające """ w tej samej kolumnie co treść i dopisz próbkę w teście,
        //   jeśli dokładny kształt tekstu jest ważny (np. oczekiwany JSON).
    }

    // =================================================================================================
    // 3. SPACJE NA KOŃCU LINII
    // =================================================================================================

    /**
     * 3. Spacje na końcu linii są usuwane — to celowe, bo edytory i systemy kontroli wersji często same je
     * obcinają, a ich obecność w kodzie jest niewidoczna. Jeśli spacja na końcu naprawdę jest częścią tekstu,
     * zapisz ją jako {@code \s} (znak spacji) — jest widoczna i przetrwa.
     */
    static void trailingSpaces() {
        section("3. Spacje na końcu linii i \\s");

        String kept = """
                |ab\s\s|
                |cd\s|
                """;                                               // kept = zachowane
        printLines(kept);
        // WYNIK: ||ab  ||
        // WYNIK: ||cd ||
        // Wewnątrz kreski pokazują, że spacje przed zamknięciem linii (z \s) zostały zachowane.

        // Sama sekwencja \s zamienia się w jedną spację, ale co ważniejsze — chroni też spacje stojące przed nią.
        String padded = """
                abc\s
                """;                                               // padded = dopełnione
        show("abc + \\s", escape(padded) + " (długość " + padded.length() + ")");
        // WYNIK: abc + \s → abc \n (długość 5)

        // PUŁAPKA: spacje wpisane na końcu linii BEZ \s znikną po cichu — nawet jeśli je widzisz w edytorze.
        //   To częsty powód „dziwnie obciętych” tekstów, np. dopełnienia kolumn w tabelkach.
        // DOBRA PRAKTYKA: potrzebujesz końcowych spacji? Użyj \s (albo kodu ósemkowego \040) w ostatniej pozycji —
        //   wtedy intencja jest widoczna dla każdego, kto czyta kod.
    }

    // =================================================================================================
    // 4. KONTYNUACJA LINII
    // =================================================================================================

    /**
     * 4. Ukośnik odwrotny na końcu linii skleja ją z następną (bez znaku nowej linii). Używamy go, gdy tekst
     * ma być jedną długą linią, ale w kodzie chcemy go złamać na czytelne kawałki (np. zapytanie SQL, komunikat).
     */
    static void lineContinuation() {
        section("4. Kontynuacja linii: \\ na końcu linii");

        String oneLine = """
                Ala ma kota, \
                a kot ma Alę, \
                i tak od lat.\
                """;                                               // one line = jedna linia
        show("jedna linia", escape(oneLine));
        // WYNIK: jedna linia → Ala ma kota, a kot ma Alę, i tak od lat.
        show("liczba linii", oneLine.lines().count());
        // WYNIK: liczba linii → 1

        // Ostatni ukośnik tuż przed zamykającym """ usuwa też końcowy znak nowej linii.

        // PUŁAPKA: ukośnik musi być OSTATNIM znakiem w linii. Spacja po nim = błąd kompilacji („nielegalna sekwencja ucieczki”).
        //   Zdarza się, gdy edytor dodaje spację na końcu linii.
        // DOBRA PRAKTYKA: spacje oddzielające słowa wstaw PRZED ukośnikiem (jak wyżej), bo po sklejeniu
        //   wcięcie następnej linii jest odcinane i spacji by zabrakło.
    }

    // =================================================================================================
    // 5. CUDZYSŁOWY I SEKWENCJE UCIECZKI
    // =================================================================================================

    /**
     * 5. Cudzysłowy wolno pisać wprost (pojedynczy i podwójny), potrójny trzeba zabezpieczyć. Sekwencje ucieczki
     * działają jak w zwykłych napisach — i są przetwarzane DOPIERO po obcięciu wcięcia.
     */
    static void quotesAndEscapes() {
        section("5. Cudzysłowy i sekwencje ucieczki");

        String quotes = """
                Powiedział: "cześć" i 'pa'.
                Potrójny cudzysłów: \""" lub ""\"
                """;                                               // quotes = cudzysłowy
        printLines(quotes);
        // WYNIK: |Powiedział: "cześć" i 'pa'.|
        // WYNIK: |Potrójny cudzysłów: """ lub """|

        String escapes = """
                kolumna1\tkolumna2
                ścieżka C:\\Temp
                """;                                               // escapes = sekwencje ucieczki
        show("\\t i \\\\", escape(escapes));
        // WYNIK: \t i \\ → kolumna1\tkolumna2\nścieżka C:\Temp\n

        // Kolejność przetwarzania: (1) usunięcie wcięcia i końcowych spacji, (2) sekwencje ucieczki.
        // Dlatego \n wpisane w środku linii DZIELI tekst na linie dopiero po obliczeniu wcięcia:
        String inlineBreak = """
                góra\ndół
                """;                                               // inline break = łamanie w linii
        show("\\n w linii", inlineBreak.lines().count());
        // WYNIK: \n w linii → 2

        // PUŁAPKA: wyrażenia regularne nadal potrzebują podwójnego ukośnika. W bloku tekstowym zapis \d to błąd
        //   kompilacji (nielegalna sekwencja ucieczki), poprawnie: \\d. Blok tekstowy nie jest surowym napisem.
        String regex = """
                \\d+-\\d+""";                                      // regex = wyrażenie regularne
        show("regex w bloku", regex + " pasuje do 12-34: " + "12-34".matches(regex));
        // WYNIK: regex w bloku → \d+-\d+ pasuje do 12-34: true
        // DOBRA PRAKTYKA: dla typowego tekstu (JSON, SQL, HTML) blok tekstowy oszczędza prawie wszystkie ucieczki,
        //   ale dla regexów zysk jest mały — rozważ zwykły napis z komentarzem opisującym wzór.
    }

    // =================================================================================================
    // 6. KOŃCE LINII
    // =================================================================================================

    /**
     * 6. Końce linii w bloku tekstowym są ZAWSZE znakiem {@code \n}, nawet jeśli plik źródłowy zapisano
     * w Windows (CRLF = {@code \r\n}). Dzięki temu ten sam kod daje ten sam napis na każdym komputerze.
     */
    static void lineEndings() {
        section("6. Końce linii: zawsze \\n");

        String text = """
                jeden
                dwa
                trzy
                """;                                               // text = tekst
        show("zawiera \\r?", text.contains("\r"));
        // WYNIK: zawiera \r? → false
        show("liczba znaków \\n", text.chars().filter(c -> c == '\n').count());   // chars = znaki (liczby)
        // WYNIK: liczba znaków \n → 3
        show("długość napisu", text.length());
        // WYNIK: długość napisu → 15

        // Dlaczego to ważne? Przed Javą 15 napis złożony z kilku linii pliku źródłowego mógłby zależeć od
        // zapisu pliku. Teraz nie zależy: kompilator normalizuje CRLF i CR do \n.
        // PUŁAPKA: jeśli piszesz plik, który MUSI mieć \r\n (np. ściśle wymagany format), zamień jawnie:
        //   text.replace("\n", "\r\n"). Systemowy separator daje System.lineSeparator() (Windows: \r\n).
        // PUŁAPKA: %n w formacie (formatted/String.format) wstawia separator SYSTEMOWY — na Windows to \r\n,
        //   więc w bloku tekstowym wynik zależałby od komputera. W blokach pisz zwykłe znaki nowej linii (po prostu
        //   złam linię), a nie %n.
        // DOBRA PRAKTYKA: dla porównań w testach nie zakładaj \r\n; porównuj z blokiem tekstowym (zawsze \n).
    }

    // =================================================================================================
    // 7. METODY KLASY STRING
    // =================================================================================================

    /**
     * 7. Metody powiązane z blokami tekstowymi (dodane w Javie 15): {@code formatted}, {@code stripIndent},
     * {@code translateEscapes}. Ułatwiają użycie bloków tekstowych także z napisami, które nie są literałami.
     */
    static void stringMethods() {
        section("7. formatted, stripIndent, translateEscapes");

        // formatted (Java 15) = „sformatowany”: skrót od String.format(this, args).
        String template = """
                Imię: %s
                Wiek: %s
                """;                                               // template = szablon
        show("formatted", escape(template.formatted("Ola", 28)));
        // WYNIK: formatted → Imię: Ola\nWiek: 28\n
        // Z jawnym Locale (formatowanie liczb zależy od ustawień regionalnych) użyj String.format(Locale.ROOT, ...):
        String price = String.format(Locale.ROOT, """
                Cena: %.2f zł
                """, 1234.5);                                      // price = cena
        show("String.format z Locale.ROOT", escape(price));
        // WYNIK: String.format z Locale.ROOT → Cena: 1234.50 zł\n

        // stripIndent (Java 15) = „odetnij wcięcie”: ta sama reguła co dla literału, ale dla dowolnego napisu.
        String indented = "    pierwsza\n      druga\n    trzecia";      // indented = wcięty
        show("stripIndent", escape(indented.stripIndent()));
        // WYNIK: stripIndent → pierwsza\n  druga\ntrzecia

        // translateEscapes (Java 15) = „przetłumacz ucieczki”: zamienia tekst \n, \t, \\ w prawdziwe znaki.
        String raw = "a\\tb\\nc";                                    // raw = surowy; zawiera ukośniki jako zwykłe znaki
        show("przed translateEscapes", raw.length());
        // WYNIK: przed translateEscapes → 7
        show("po translateEscapes", raw.translateEscapes().length());
        // WYNIK: po translateEscapes → 5

        // PUŁAPKA: % w treści bloku używanego z formatted() musi być zapisane jako %% (inaczej dostaniesz wyjątek z rodziny
        //   IllegalFormatException albo błędny wynik). Dotyczy np. tekstu „rabat 10%”; tu spacja po % zostaje
        //   odczytana jako flaga formatu.
        expectThrows("% bez pary", () -> "Rabat 10% na wszystko".formatted());
        // WYNIK: ✔ % bez pary → rzucono IllegalFormatFlagsException: Flags = ' '
        show("poprawnie %%", "Rabat 10%% na wszystko".formatted());
        // WYNIK: poprawnie %% → Rabat 10% na wszystko
        // DOBRA PRAKTYKA: dla większych szablonów trzymaj blok tekstowy w stałej (private static final String)
        //   i wołaj .formatted(...) w miejscu użycia — szablon jest wtedy w jednym miejscu.
    }

    // =================================================================================================
    // 8. ZASTOSOWANIA
    // =================================================================================================

    private static final String JSON_TEMPLATE = """
            {
              "customer": "%s",
              "vip": %s,
              "orders": %s
            }
            """;   // JSON template = szablon JSON-a

    /**
     * 8. Typowe zastosowania: JSON, SQL, HTML i oczekiwany wynik w teście. Wszędzie tam tekst ma kilka linii
     * i znaki specjalne, więc blok tekstowy jest najczytelniejszy.
     */
    static void realUses() {
        section("8. Zastosowania: JSON, SQL, HTML, test");

        // JSON z szablonu
        String json = JSON_TEMPLATE.formatted("Jan Kowalski", true, 3);
        printLines(json);
        // WYNIK: |{|
        // WYNIK: |  "customer": "Jan Kowalski",|
        // WYNIK: |  "vip": true,|
        // WYNIK: |  "orders": 3|
        // WYNIK: |}|

        // SQL: przed — sklejanie, każda linia z plusem i spacją na końcu (łatwo zapomnieć o spacji!).
        String sqlBefore = "SELECT id, name " +
                "FROM customers " +
                "WHERE vip = true " +
                "ORDER BY name";
        // SQL: po — kontynuacja linii \ robi z bloku jedną linię, a spacje mamy widoczne przed ukośnikiem.
        String sqlAfter = """
                SELECT id, name \
                FROM customers \
                WHERE vip = true \
                ORDER BY name""";
        show("SQL jedna linia", sqlAfter);
        // WYNIK: SQL jedna linia → SELECT id, name FROM customers WHERE vip = true ORDER BY name
        show("SQL przed == po", sqlBefore.equals(sqlAfter));
        // WYNIK: SQL przed == po → true

        // HTML — znaki < > " bez ucieczek
        String html = """
                <ul class="menu">
                  <li>Start</li>
                  <li>Kontakt</li>
                </ul>
                """;                                               // html = fragment strony
        show("liczba linii HTML", html.lines().count());
        // WYNIK: liczba linii HTML → 4
        show("pierwsza linia HTML", html.lines().findFirst().orElse(""));   // find first = znajdź pierwszy
        // WYNIK: pierwsza linia HTML → <ul class="menu">

        // Test: oczekiwany wielolinijkowy wynik w czytelnej postaci.
        String actual = String.join("\n", List.of("Anna", "Piotr")) + "\n";   // actual = rzeczywisty
        String expected = """
                Anna
                Piotr
                """;                                               // expected = oczekiwany
        show("wynik zgodny z oczekiwanym", actual.equals(expected));
        // WYNIK: wynik zgodny z oczekiwanym → true

        // PUŁAPKA: wklejanie danych użytkownika do bloku SQL przez formatted() otwiera drogę do ataku SQL injection
        //   (wstrzyknięcie SQL). Blok tekstowy to tylko napis — nie zabezpiecza przed niczym.
        // DOBRA PRAKTYKA: SQL trzymaj w bloku tekstowym z parametrami (?) i wykonuj przez PreparedStatement
        //   (zobacz t29_jdbc_databases/Jdbc03PreparedStatement). formatted() użyj tylko dla stałych fragmentów
        //   (np. nazwa tabeli z listy dozwolonych), nigdy dla wartości od użytkownika.
    }

    // =================================================================================================
    // 9. PUŁAPKI
    // =================================================================================================

    /**
     * 9. Najczęstsze potknięcia przy blokach tekstowych: niespodziewany znak nowej linii na końcu, wcięcie,
     * zapomniane {@code %%} i zależność od edytora.
     */
    static void pitfalls() {
        section("9. Pułapki");

        // 1) Końcowy \n: zamykające """ w osobnej linii dodaje znak nowej linii na końcu.
        String withNewline = """
                abc
                """;                                               // with newline = z nową linią
        String without = """
                abc""";                                            // without = bez
        show("długość z \\n na końcu", withNewline.length());
        // WYNIK: długość z \n na końcu → 4
        show("długość bez", without.length());
        // WYNIK: długość bez → 3
        show("equals(\"abc\")", withNewline.equals("abc"));
        // WYNIK: equals("abc") → false
        show("po strip()", withNewline.strip().equals("abc"));          // strip = obetnij białe znaki z obu końców
        // WYNIK: po strip() → true

        // 2) Dwa warianty mogą wyglądać identycznie w konsoli, a różnić się znakiem nowej linii:
        //    println(withNewline) wypisze pustą linię po „abc” (bo napis sam kończy się \n, a println dodaje swój).
        printLines(withNewline);
        // WYNIK: |abc|

        // 3) Zmiana wcięcia całego bloku (np. przeniesienie do innej metody) NIE zmienia napisu — to zaleta.
        //    Ale zmiana pozycji zamykającego """ — zmienia (sekcja 2).
        // 4) Nie mieszaj tabulatorów i spacji w wcięciu bloku (sekcja 2).
        // 5) Blok tekstowy jest stałą czasu kompilacji, więc można go łączyć ze stałymi i używać w adnotacjach
        //    oraz jako etykiety switch; ale tekst z formatted() jest już zwykłym napisem (nie stałą).
        // 6) Nie wklejaj do bloków tekstowych sekretów (haseł, kluczy) — trafią do kodu i repozytorium.

        // DOBRA PRAKTYKA: napisz test sprawdzający kształt najważniejszych bloków (JSON/SQL) — kontrola
        //   końcowych znaków nowej linii i wcięć w czytaniu kodu jest zawodna.
        note("Zasada kciuka: blok tekstowy kończy się \\n, jeśli zamykające \"\"\" stoi w osobnej linii.");
        // WYNIK: ℹ Zasada kciuka: blok tekstowy kończy się \n, jeśli zamykające """ stoi w osobnej linii.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Blok tekstowy (Java 15, JEP 378): """ + koniec linii + treść + """; zwykły String.
     *   • Wcięcie: odcina się najmniejsze wcięcie z linii z treścią i z zamykającego """ (jeśli stoi samotnie).
     *   • Zamykające """ w osobnej linii → napis kończy się \n; na końcu ostatniej linii → bez \n.
     *   • Spacje na końcu linii znikają; zachowasz je przez \s.
     *   • \ na końcu linii skleja linie (musi być ostatnim znakiem). \n, \t, \" i \\ działają jak zwykle.
     *   • Potrójny cudzysłów w treści: \""" ; pojedyncze cudzysłowy bez ucieczek.
     *   • Końce linii zawsze \n (niezależnie od systemu); %n w formatted jest ZALEŻNE od systemu.
     *   • formatted, stripIndent, translateEscapes (Java 15); % trzeba pisać jako %%.
     *   • Bloki tekstowe nie chronią przed SQL injection — używaj PreparedStatement.
     *
     * PYTANIA KONTROLNE:
     *   1. Co musi stać zaraz po otwierającym """ i dlaczego?
     *   2. Od czego zależy wcięcie odcinane w bloku tekstowym?
     *   3. Co wypisze:  String s = """
     *                       ab
     *                       cd""";  System.out.println(s.length());  ?
     *   4. Co wypisze:  String s = """
     *                       ab
     *                       cd
     *                       """;  System.out.println(s.length());  ?
     *   5. ZNAJDŹ BŁĄD:  String s = """abc""";
     *   6. Jak zapisać w bloku tekstowym spację na końcu linii, żeby nie została usunięta?
     *   7. Dlaczego %n w szablonie z formatted() jest złym pomysłem w bloku tekstowym?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: blok zamiast sklejania", "Raz\ndwa\ntrzy\n", () -> exercise1());
        Check.equal("ćw. 2: JSON przez formatted", "{\n  \"name\": \"Ala\",\n  \"age\": 30\n}\n",
                () -> exercise2("Ala", 30));
        Check.equal("ćw. 3: wcięcie", "  wcięte\ntekst\n", () -> exercise3());
        Check.equal("ćw. 4: SQL w jednej linii", "SELECT id, name FROM customers WHERE vip = true",
                () -> exercise4(List.of("id", "name"), "customers"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "Raz\ndwa\ntrzy\n", () -> solution1());
        Check.equal("ćw. 2 (wzorzec)", "{\n  \"name\": \"Ala\",\n  \"age\": 30\n}\n",
                () -> solution2("Ala", 30));
        Check.equal("ćw. 3 (wzorzec)", "  wcięte\ntekst\n", () -> solution3());
        Check.equal("ćw. 4 (wzorzec)", "SELECT id, name FROM customers WHERE vip = true",
                () -> solution4(List.of("id", "name"), "customers"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): PRZEPISZ na blok tekstowy. Napis ma mieć trzy linie: „Raz”, „dwa”, „trzy”, z \n na końcu.
     * <pre>{@code
     * // PRZED:
     * return "Raz\n" + "dwa\n" + "trzy\n";
     * }</pre>
     * Podpowiedź: zamykające """ w osobnej linii, w tej samej kolumnie co treść.
     */
    static String exercise1() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (łatwe): zbuduj JSON {"name": ..., "age": ...} (każde pole w osobnej linii, wcięte dwiema spacjami,
     * z \n na końcu) z użyciem bloku tekstowego i formatted.
     * Podpowiedź: szablon z %s dla imienia (w cudzysłowach) i %s dla wieku.
     */
    static String exercise2(String name, int age) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć blok tekstowy o dokładnej treści „  wcięte\ntekst\n”: pierwsza linia wcięta
     * dwiema spacjami względem drugiej. Zapisz w kodzie obie linie z różnym wcięciem.
     * Podpowiedź: o wcięciu decyduje najmniejsze wcięcie — linia „tekst” i zamykające """ muszą być w tej samej kolumnie,
     * a „wcięte” dwie spacje dalej.
     */
    static String exercise3() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zbuduj zapytanie SELECT jako JEDNĄ linię bez \n, z blokiem tekstowym,
     * kontynuacją linii \ i formatted: SELECT kolumny FROM tabela WHERE vip = true (kolumny rozdzielone „, ”).
     * Podpowiedź: String.join(", ", columns); spacje przed ukośnikiem; ostatnia linia bez końcowego \n.
     */
    static String exercise4(List<String> columns, String table) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1() {
        return """
                Raz
                dwa
                trzy
                """;
    }

    static String solution2(String name, int age) {
        return """
                {
                  "name": "%s",
                  "age": %s
                }
                """.formatted(name, age);
    }

    static String solution3() {
        return """
                  wcięte
                tekst
                """;
    }

    static String solution4(List<String> columns, String table) {
        return """
                SELECT %s \
                FROM %s \
                WHERE vip = true""".formatted(String.join(", ", columns), table);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Koniec linii. Treść bloku zaczyna się dopiero od następnej linii; tekst w tej samej linii co otwierający
     *      ogranicznik to błąd kompilacji.
     *   2. Od najmniejszego wcięcia spośród wszystkich linii z treścią oraz samotnej linii z zamykającym """.
     *      Przesunięcie zamykającego """ w lewo zostawia część wcięcia w tekście.
     *   3. 5 („ab\ncd” = 2 + 1 + 2 znaki, bez końcowego \n).
     *   4. 6 („ab\ncd\n” — zamykające """ w osobnej linii dodaje końcowy \n).
     *   5. Treść nie może zaczynać się w linii otwierającego """ — po nim musi być koniec linii.
     *   6. Zapisać \s na końcu (np. abc\s) — zwykłe końcowe spacje są usuwane.
     *   7. %n wstawia separator zależny od systemu (Windows: \r\n), a blok tekstowy sam gwarantuje stałe \n —
     *      mieszanie ich daje wynik zależny od komputera. W bloku po prostu złam linię.
     */
    // </editor-fold>
}
