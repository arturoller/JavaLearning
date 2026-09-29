package t04_strings;

import helpers.Check;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Wyrażenia regularne (regex) — wzorce opisujące tekst
 *        (regular expression = wyrażenie regularne; pattern = wzorzec; matcher = dopasowywacz; group = grupa)
 *
 * W SKRÓCIE:
 *   Regex to mini-język do opisywania tekstu: „dwie cyfry, myślnik, trzy cyfry” to  \d{2}-\d{3}  (kod pocztowy).
 *   Używasz go do SPRAWDZANIA (matches), SZUKANIA (Pattern + Matcher + find), ZAMIENIANIA (replaceAll) i DZIELENIA
 *   (split). W napisie Javy ukośnik trzeba podwoić: regex \d zapisujesz jako "\\d".
 *
 * ANALOGIA: szablon do wycinania ciasteczek.
 *   Wzorzec to foremka o określonym kształcie. matches sprawdza, czy CAŁE ciasto ma dokładnie ten kształt; find szuka
 *   w cieście wszystkich miejsc, gdzie foremka pasuje; replaceAll wycina te kawałki i wkłada w ich miejsce coś innego.
 *
 * JAK TO DZIAŁA:
 *   \d  cyfra          \w  litera/cyfra/_     \s  biały znak      .  dowolny znak      [abc]  jeden z: a, b, c
 *   [a-z]  zakres      [^0-9]  wszystko oprócz cyfr               ^  początek        $  koniec
 *   +  1 lub więcej    *  0 lub więcej         ?  0 albo 1         {3}  dokładnie 3    {2,4}  od 2 do 4
 *   ( )  grupa — zapamiętuje dopasowany fragment; w zamianie: $1, $2...
 *
 * SŁÓWKA:
 *   regex = wyrażenie regularne; pattern = wzorzec; compile = skompiluj; matcher = dopasowywacz; match = dopasowanie;
 *   find = znajdź; group = grupa; quantifier = kwantyfikator (ile razy); greedy = zachłanny; lazy = leniwy;
 *   escape = „ucieczka” (poprzedzenie znaku ukośnikiem); quote = zacytuj (potraktuj dosłownie).
 *
 * ZOBACZ TEŻ: t04_strings/Strings02Methods (split i replaceAll to regex!), t04_strings/Strings07TextAlgorithms,
 *             t19_annotations_reflection/Annotations04Validator (walidacja danych).
 * </pre>
 */
public class Strings05Regex {

    /** NUMBER = wzorzec liczby. Pattern kompilujemy RAZ i używamy wielokrotnie (kompilacja kosztuje). */
    private static final Pattern NUMBER = Pattern.compile("\\d+");

    public static void main(String[] args) {
        title("Strings05 — wyrażenia regularne");

        matchesWholeText();     // matches whole text = dopasowanie całego tekstu
        characterClasses();     // character classes = klasy znaków
        findAll();              // find all = znajdź wszystkie
        groups();               // groups = grupy
        replaceWithGroups();    // replace with groups = zamiana z grupami
        splitWithRegex();       // split with regex = dzielenie wyrażeniem
        escaping();             // escaping = znaki specjalne
        greedyVsLazy();         // greedy vs lazy = zachłanny kontra leniwy
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. matches — CAŁY TEKST
    // =================================================================================================

    /** 1. matches sprawdza, czy CAŁY napis pasuje do wzorca (nie wystarczy fragment). */
    static void matchesWholeText() {
        section("1. matches — czy cały tekst pasuje");

        show("\"31-150\" kod pocztowy?", "31-150".matches("\\d{2}-\\d{3}"));
        show("\"31150\" kod pocztowy?", "31150".matches("\\d{2}-\\d{3}"));
        show("\"12345\" same cyfry?", "12345".matches("\\d+"));
        show("\"123a\" same cyfry?", "123a".matches("\\d+"));
        // WYNIK: "31-150" kod pocztowy? → true
        // WYNIK: "31150" kod pocztowy? → false
        // WYNIK: "12345" same cyfry? → true
        // WYNIK: "123a" same cyfry? → false    ← „a” psuje dopasowanie CAŁOŚCI

        // PUŁAPKA: w Javie ukośnik w napisie trzeba podwoić: regex \d to w kodzie "\\d". Pojedyncze "\d" to błąd kompilacji.
    }

    // =================================================================================================
    // 2. KLASY ZNAKÓW I KWANTYFIKATORY
    // =================================================================================================

    /** 2. Klasa znaków [ ] to „jeden z tych znaków”; kwantyfikator { } / + / * / ? mówi „ile razy”. */
    static void characterClasses() {
        section("2. Klasy znaków i kwantyfikatory");

        String phone = "\\d{3}-\\d{3}-\\d{3}";                    // np. 600-100-200
        show("\"600-100-200\" telefon?", "600-100-200".matches(phone));
        // WYNIK: "600-100-200" telefon? → true

        String sku = "[A-Z]{3}-\\d{3}";                           // np. ELE-001
        show("\"ELE-001\" kod produktu?", "ELE-001".matches(sku));
        show("\"ele-001\" kod produktu?", "ele-001".matches(sku));
        // WYNIK: "ELE-001" kod produktu? → true
        // WYNIK: "ele-001" kod produktu? → false    ← [A-Z] to tylko wielkie litery

        show("\"Ala1\" — litery, potem cyfra?", "Ala1".matches("[a-zA-Z]+\\d"));
        // WYNIK: "Ala1" — litery, potem cyfra? → true

        // PUŁAPKA: [a-z] nie obejmuje polskich liter (ą, ć, ę...). Dla liter dowolnego języka: \p{L} (w kodzie "\\p{L}").
        show("\"Łódź\" pasuje do [a-zA-Z]+?", "Łódź".matches("[a-zA-Z]+"));
        show("\"Łódź\" pasuje do \\p{L}+?", "Łódź".matches("\\p{L}+"));
        // WYNIK: "Łódź" pasuje do [a-zA-Z]+? → false
        // WYNIK: "Łódź" pasuje do \p{L}+? → true
    }

    // =================================================================================================
    // 3. SZUKANIE WSZYSTKICH WYSTĄPIEŃ
    // =================================================================================================

    /** 3. Pattern.compile tworzy wzorzec, matcher(tekst) — dopasowywacz; find() szuka NASTĘPNEGO pasującego fragmentu. */
    static void findAll() {
        section("3. Pattern + Matcher + find — wszystkie wystąpienia");

        Matcher m = NUMBER.matcher("Zamówienie 12: 7 sztuk, dostawa w 2026 roku");
        List<String> found = new ArrayList<>();          // lista (t12_collections) — tu tylko do zebrania wyników
        while (m.find()) {
            found.add(m.group());                        // group() = cały dopasowany fragment
        }
        show("liczby w tekście", found);
        // WYNIK: liczby w tekście → [12, 7, 2026]

        // DOBRA PRAKTYKA: wzorzec używany wiele razy trzymaj jako stałą (static final Pattern) — kompilacja wzorca
        //   jest kosztowna, a "tekst".matches(regex) kompiluje go od nowa przy każdym wywołaniu.
    }

    // =================================================================================================
    // 4. GRUPY
    // =================================================================================================

    /** 4. Nawiasy ( ) tworzą grupy — zapamiętane fragmenty. group(1) to pierwsza grupa, group(2) druga... Grupy można nazwać. */
    static void groups() {
        section("4. Grupy: wyciąganie części tekstu");

        Matcher m = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})").matcher("2026-09-29");
        if (m.matches()) {
            show("rok / miesiąc / dzień", m.group(1) + " / " + m.group(2) + " / " + m.group(3));
        }
        // WYNIK: rok / miesiąc / dzień → 2026 / 09 / 29

        Matcher named = Pattern.compile("(?<user>[\\w.]+)@(?<domain>[\\w.]+)").matcher("anna.nowak@example.com");
        if (named.matches()) {
            show("użytkownik / domena", named.group("user") + " / " + named.group("domain"));
        }
        // WYNIK: użytkownik / domena → anna.nowak / example.com
    }

    // =================================================================================================
    // 5. ZAMIANA Z UŻYCIEM GRUP
    // =================================================================================================

    /** 5. W replaceAll odwołujesz się do grup przez $1, $2... — np. do zmiany formatu daty. */
    static void replaceWithGroups() {
        section("5. replaceAll z grupami $1, $2...");

        show("2026-09-29 → format polski", "2026-09-29".replaceAll("(\\d{4})-(\\d{2})-(\\d{2})", "$3.$2.$1"));
        // WYNIK: 2026-09-29 → format polski → 29.09.2026

        show("wiele spacji → jedna", "Ala   ma    kota".replaceAll("\\s+", " "));
        // WYNIK: wiele spacji → jedna → Ala ma kota
    }

    // =================================================================================================
    // 6. DZIELENIE WYRAŻENIEM
    // =================================================================================================

    /** 6. split z klasą znaków dzieli po KILKU różnych separatorach naraz. */
    static void splitWithRegex() {
        section("6. split po kilku separatorach");

        show("[,;\\s]+", "chleb, masło;ser  mleko".split("[,;\\s]+"));
        // WYNIK: [,;\s]+ → [chleb, masło, ser, mleko]
    }

    // =================================================================================================
    // 7. ZNAKI SPECJALNE
    // =================================================================================================

    /**
     * 7. Znaki . * + ? ( ) [ ] { } ^ $ | \ mają w regexie specjalne znaczenie. Żeby dopasować je dosłownie —
     * poprzedź ukośnikiem ("\\.") albo użyj Pattern.quote (zacytuj cały tekst).
     */
    static void escaping() {
        section("7. Znaki specjalne: \\. i Pattern.quote");

        show("\"1x5\" pasuje do \\d.\\d?", "1x5".matches("\\d.\\d"));
        show("\"1x5\" pasuje do \\d\\.\\d?", "1x5".matches("\\d\\.\\d"));
        show("\"1.5\" pasuje do \\d\\.\\d?", "1.5".matches("\\d\\.\\d"));
        // WYNIK: "1x5" pasuje do \d.\d? → true    ← kropka bez ukośnika to „dowolny znak” — przepuściła „x”!
        // WYNIK: "1x5" pasuje do \d\.\d? → false    ← \. to dosłowna kropka
        // WYNIK: "1.5" pasuje do \d\.\d? → true

        show("split(Pattern.quote(\"|\"))", "a|b|c".split(Pattern.quote("|")));
        // WYNIK: split(Pattern.quote("|")) → [a, b, c]
        // PUŁAPKA: "a|b|c".split("|") dzieli na POJEDYNCZE ZNAKI — | w regexie to „lub”.
    }

    // =================================================================================================
    // 8. ZACHŁANNY KONTRA LENIWY
    // =================================================================================================

    /**
     * 8. Kwantyfikatory * i + są ZACHŁANNE (greedy) — biorą jak najwięcej. Dopisanie ? (*?, +?) czyni je LENIWYMI
     * (lazy) — biorą jak najmniej.
     */
    static void greedyVsLazy() {
        section("8. Zachłanny .* kontra leniwy .*?");

        String html = "<b>raz</b> i <b>dwa</b>";
        show("zachłanny <b>.*</b>", firstMatch("<b>.*</b>", html));
        show("leniwy <b>.*?</b>", firstMatch("<b>.*?</b>", html));
        // WYNIK: zachłanny <b>.*</b> → <b>raz</b> i <b>dwa</b>    ← połknął wszystko do OSTATNIEGO </b>
        // WYNIK: leniwy <b>.*?</b> → <b>raz</b>

        // DOBRA PRAKTYKA: regex jest świetny do prostych wzorców. Do HTML, JSON czy XML używaj bibliotek-parserów,
        //   a do adresów e-mail — prostego sprawdzenia i wysłania wiadomości weryfikacyjnej (pełny wzorzec e-mail jest ogromny).
    }

    /** firstMatch = pierwsze dopasowanie. Zwraca pierwszy pasujący fragment albo pusty napis. */
    private static String firstMatch(String regex, String text) {
        Matcher m = Pattern.compile(regex).matcher(text);
        return m.find() ? m.group() : "";
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • "tekst".matches(regex) — czy CAŁY tekst pasuje. W kodzie ukośniki podwójnie: "\\d".
     *   • \d cyfra, \w znak słowa, \s biały znak, . dowolny, [a-z] zakres, [^...] negacja, \p{L} litera (też polska).
     *   • + (1+), * (0+), ? (0/1), {n}, {n,m}; ^ początek, $ koniec.
     *   • Pattern p = Pattern.compile(regex) (jako stała!); Matcher m = p.matcher(tekst); while (m.find()) m.group().
     *   • Grupy ( ): group(1)...; nazwane (?<nazwa>...) → group("nazwa"); w replaceAll: $1, $2.
     *   • Znaki specjalne dosłownie: "\\." albo Pattern.quote(...). split("|") i split(".") to pułapki.
     *   • .* zachłanne, .*? leniwe.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się matches od find?
     *   2. Co zwróci  "2026".matches("\\d{2}")  ?
     *   3. ZNAJDŹ BŁĄD:  String[] parts = "10|20|30".split("|");
     *   4. Jak zamienić „29.09.2026” na „2026-09-29” jednym replaceAll?
     *   5. Dlaczego  "Żółw".matches("[a-zA-Z]+")  zwraca false i jak to poprawić?
     *   6. Dlaczego wzorzec używany w pętli warto skompilować raz jako stałą?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: \"31-150\" to kod pocztowy", true, () -> exercise1("31-150"));
        Check.equal("ćw. 1b: \"31150\" to nie kod", false, () -> exercise1("31150"));
        Check.equal("ćw. 2: suma liczb w tekście", 15, () -> exercise2("Ala ma 2 koty i 13 rybek"));
        Check.equal("ćw. 3: data 2026-09-29 po polsku", "29.09.2026", () -> exercise3("2026-09-29"));
        Check.equal("ćw. 4: zamaskowana karta", "**** **** **** 3456", () -> exercise4("1234 5678 9012 3456"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", true, () -> solution1("31-150"));
        Check.equal("ćw. 1b (wzorzec)", false, () -> solution1("31150"));
        Check.equal("ćw. 2 (wzorzec)", 15, () -> solution2("Ala ma 2 koty i 13 rybek"));
        Check.equal("ćw. 3 (wzorzec)", "29.09.2026", () -> solution3("2026-09-29"));
        Check.equal("ćw. 4 (wzorzec)", "**** **** **** 3456", () -> solution4("1234 5678 9012 3456"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /** ĆWICZENIE 1 (łatwe): czy tekst to polski kod pocztowy (2 cyfry, myślnik, 3 cyfry)? Podpowiedź: matches("\\d{2}-\\d{3}"). */
    static boolean exercise1(String text) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zsumuj wszystkie liczby całkowite występujące w tekście ("Ala ma 2 koty i 13 rybek" → 15).
     * Podpowiedź: Matcher na wzorcu "\\d+", pętla while (m.find()), sum += Integer.parseInt(m.group()).
     */
    static int exercise2(String text) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /** ĆWICZENIE 3 (średnie): zamień datę RRRR-MM-DD na DD.MM.RRRR jednym replaceAll z grupami ($1, $2, $3). */
    static String exercise3(String isoDate) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zamaskuj numer karty — każdą grupę 4 cyfr ZAKOŃCZONĄ SPACJĄ zamień na "**** ",
     * ostatnia grupa zostaje: "1234 5678 9012 3456" → "**** **** **** 3456".
     * Podpowiedź: replaceAll("\\d{4} ", "**** ").
     */
    static String exercise4(String cardNumber) {
        // TODO: twoje rozwiązanie
        return "";
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static boolean solution1(String text) {
        return text.matches("\\d{2}-\\d{3}");
    }

    static int solution2(String text) {
        Matcher m = NUMBER.matcher(text);
        int sum = 0;
        while (m.find()) {
            sum += Integer.parseInt(m.group());
        }
        return sum;
    }

    static String solution3(String isoDate) {
        return isoDate.replaceAll("(\\d{4})-(\\d{2})-(\\d{2})", "$3.$2.$1");
    }

    static String solution4(String cardNumber) {
        return cardNumber.replaceAll("\\d{4} ", "**** ");
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. matches wymaga, żeby CAŁY tekst pasował do wzorca; find szuka kolejnych FRAGMENTÓW, które pasują.
     *   2. false — tekst ma 4 cyfry, a wzorzec \d{2} dopasowuje dokładnie 2, a matches sprawdza całość.
     *   3. | w regexie oznacza „lub” (tu: „nic lub nic”), więc split podzieli na pojedyncze znaki.
     *      Poprawnie: split("\\|") albo split(Pattern.quote("|")).
     *   4. "29.09.2026".replaceAll("(\\d{2})\\.(\\d{2})\\.(\\d{4})", "$3-$2-$1").
     *   5. [a-zA-Z] to tylko litery łacińskie bez ogonków. Poprawnie: "\\p{L}+" (dowolne litery Unicode).
     *   6. Bo kompilacja wzorca kosztuje; "tekst".matches(regex) i replaceAll kompilują go przy każdym wywołaniu.
     */
    // </editor-fold>
}
