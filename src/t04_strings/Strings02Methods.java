package t04_strings;

import helpers.Check;

import java.util.Locale;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Najważniejsze metody klasy String
 *        (method = metoda; substring = podnapis; index = indeks, pozycja; split = podziel; replace = zamień)
 *
 * W SKRÓCIE:
 *   String ma dziesiątki metod, ale na co dzień wystarcza kilkanaście: długość i znaki (length, charAt), fragmenty
 *   (substring), szukanie (indexOf, contains, startsWith), zmiany (toUpperCase, strip, replace), dzielenie i łączenie
 *   (split, join, repeat). Pamiętaj: indeksy liczą się od 0, a koniec w substring NIE wchodzi do wyniku.
 *
 * ANALOGIA: linijka z podziałką.
 *   Napis „JAVA” to litery położone na linijce: J na pozycji 0, A na 1, V na 2, A na 3. substring(1, 3) to „wytnij od
 *   kreski 1 do kreski 3” — dostajesz AV (litery z pozycji 1 i 2), bo kreska 3 to już początek kolejnej litery.
 *
 * JAK TO DZIAŁA:
 *   "Kraków".length()          → 6          "Kraków".charAt(0)       → 'K'
 *   "Kraków".substring(2)      → "aków"      "Kraków".substring(0, 3) → "Kra"   (od 0 do 3, bez 3)
 *   "Kraków".indexOf("ak")     → 2           "Kraków".indexOf("x")    → -1    (-1 = nie znaleziono)
 *   "a,b,c".split(",")         → [a, b, c]   String.join("-", "a", "b") → "a-b"
 *
 * SŁÓWKA:
 *   length = długość; char at = znak na (pozycji); substring = podnapis; index of = indeks (pozycja) czegoś;
 *   contains = zawiera; starts with / ends with = zaczyna się od / kończy się na; strip / trim = obierz / przytnij;
 *   replace = zamień; split = podziel; join = połącz; repeat = powtórz; leading / trailing = początkowy / końcowy.
 *
 * ZOBACZ TEŻ: t04_strings/Strings01Basics (niezmienność), t04_strings/Strings05Regex (split i replaceAll to regex!),
 *             t04_strings/Strings03StringBuilder (wydajne składanie napisów).
 * </pre>
 */
public class Strings02Methods {

    public static void main(String[] args) {
        title("Strings02 — metody klasy String");

        lengthAndCharAt();      // length and charAt = długość i znak na pozycji
        substrings();           // substrings = fragmenty napisu
        searching();            // searching = wyszukiwanie
        changingCase();         // changing case = zmiana wielkości liter
        stripping();            // stripping = obieranie z białych znaków
        replacing();            // replacing = zamienianie
        splitting();            // splitting = dzielenie
        joiningAndRepeating();  // joining and repeating = łączenie i powtarzanie
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. DŁUGOŚĆ I ZNAKI
    // =================================================================================================

    /** 1. length() — liczba znaków; charAt(i) — znak na pozycji i (od 0 do length() - 1). */
    static void lengthAndCharAt() {
        section("1. length() i charAt()");

        String word = "Kraków";
        show("length()", word.length());
        show("charAt(0) / ostatni znak", word.charAt(0) + " / " + word.charAt(word.length() - 1));
        // WYNIK: length() → 6
        // WYNIK: charAt(0) / ostatni znak → K / w

        expectThrows("charAt(6) — poza napisem", () -> word.charAt(6));
        // WYNIK: ✔ charAt(6) — poza napisem → rzucono StringIndexOutOfBoundsException: String index out of range: 6
        // PUŁAPKA: ostatni znak ma indeks length() - 1, nie length().
    }

    // =================================================================================================
    // 2. FRAGMENTY — substring
    // =================================================================================================

    /** 2. substring(od) — od pozycji do końca; substring(od, do) — od „od” (włącznie) do „do” (BEZ niego). */
    static void substrings() {
        section("2. substring — koniec NIE wchodzi");

        String date = "2026-09-29";
        show("rok substring(0, 4)", date.substring(0, 4));
        show("miesiąc substring(5, 7)", date.substring(5, 7));
        show("dzień substring(8)", date.substring(8));
        // WYNIK: rok substring(0, 4) → 2026
        // WYNIK: miesiąc substring(5, 7) → 09
        // WYNIK: dzień substring(8) → 29

        // DOBRA PRAKTYKA: długość wyniku substring(od, do) to zawsze do - od. Dzięki temu łatwo policzyć granice.
        // (Do dat masz lepsze narzędzia niż substring — LocalDate.parse, t17_datetime.)
    }

    // =================================================================================================
    // 3. WYSZUKIWANIE
    // =================================================================================================

    /** 3. indexOf zwraca pozycję pierwszego wystąpienia albo -1. contains/startsWith/endsWith zwracają boolean. */
    static void searching() {
        section("3. indexOf, lastIndexOf, contains, startsWith, endsWith");

        String file = "raport.final.pdf";
        show("indexOf('.')", file.indexOf('.'));
        show("lastIndexOf('.')", file.lastIndexOf('.'));
        show("indexOf(\"xyz\")", file.indexOf("xyz"));
        // WYNIK: indexOf('.') → 6
        // WYNIK: lastIndexOf('.') → 12
        // WYNIK: indexOf("xyz") → -1    ← -1 znaczy „nie znaleziono”

        show("contains(\"final\")", file.contains("final"));
        show("startsWith(\"rap\") / endsWith(\".pdf\")", file.startsWith("rap") + " / " + file.endsWith(".pdf"));
        // WYNIK: contains("final") → true
        // WYNIK: startsWith("rap") / endsWith(".pdf") → true / true

        // PUŁAPKA: przed użyciem wyniku indexOf sprawdź, czy nie jest -1 — substring(-1) rzuci wyjątek.
    }

    // =================================================================================================
    // 4. WIELKOŚĆ LITER
    // =================================================================================================

    /**
     * 4. toUpperCase / toLowerCase zależą od języka (Locale). W tureckim „i” zamienia się w „İ” (i z kropką), więc
     * porównania „po zamianie” potrafią się rozjechać. Dla danych technicznych (kody, klucze) używaj Locale.ROOT.
     */
    static void changingCase() {
        section("4. toUpperCase / toLowerCase (z Locale)");

        show("\"Łódź\".toUpperCase(Locale.ROOT)", "Łódź".toUpperCase(Locale.ROOT));
        show("\"KOD-abc\".toLowerCase(Locale.ROOT)", "KOD-abc".toLowerCase(Locale.ROOT));
        // WYNIK: "Łódź".toUpperCase(Locale.ROOT) → ŁÓDŹ
        // WYNIK: "KOD-abc".toLowerCase(Locale.ROOT) → kod-abc

        show("\"title\" po turecku", "title".toUpperCase(Locale.forLanguageTag("tr")));
        // WYNIK: "title" po turecku → TİTLE    ← turecka duża litera İ (z kropką)!
    }

    // =================================================================================================
    // 5. BIAŁE ZNAKI: strip / trim
    // =================================================================================================

    /** 5. strip (Java 11+) usuwa białe znaki z brzegów (zna wszystkie spacje Unicode); trim — tylko proste spacje i znaki sterujące. */
    static void stripping() {
        section("5. strip, stripLeading, stripTrailing");

        String input = "   Anna Nowak  ";
        show("strip()", "[" + input.strip() + "]");
        show("stripLeading()", "[" + input.stripLeading() + "]");
        show("stripTrailing()", "[" + input.stripTrailing() + "]");
        // WYNIK: strip() → [Anna Nowak]
        // WYNIK: stripLeading() → [Anna Nowak  ]
        // WYNIK: stripTrailing() → [   Anna Nowak]

        // DOBRA PRAKTYKA: dane od użytkownika zawsze „obierz” (strip) przed zapisem i porównaniem.
        //   Spacje W ŚRODKU zostają — strip działa tylko na brzegach.
    }

    // =================================================================================================
    // 6. ZAMIENIANIE: replace kontra replaceAll
    // =================================================================================================

    /** 6. replace zamienia DOSŁOWNY tekst (wszystkie wystąpienia!). replaceAll traktuje pierwszy argument jako REGEX. */
    static void replacing() {
        section("6. replace (dosłownie) kontra replaceAll (regex)");

        String version = "1.2.3";
        show("replace(\".\", \"-\")", version.replace(".", "-"));
        show("replaceAll(\".\", \"-\")", version.replaceAll(".", "-"));
        // WYNIK: replace(".", "-") → 1-2-3
        // WYNIK: replaceAll(".", "-") → -----    ← w regexie kropka to „dowolny znak” — zamieniło WSZYSTKO!

        // PUŁAPKA: nazwa sugeruje, że replace zamienia tylko pierwsze wystąpienie — NIE, zamienia wszystkie.
        //   Różnica między replace a replaceAll to regex, nie „ile razy”. (Tylko pierwsze: replaceFirst — też regex.)
    }

    // =================================================================================================
    // 7. DZIELENIE: split
    // =================================================================================================

    /** 7. split dzieli napis na tablicę. Argument to REGEX. Puste kawałki na KOŃCU są domyślnie usuwane. */
    static void splitting() {
        section("7. split — dzielenie na tablicę");

        show("\"a,b,,c\".split(\",\")", "a,b,,c".split(","));
        show("\"a,b,,\".split(\",\")", "a,b,,".split(","));
        show("\"a,b,,\".split(\",\", -1)", "a,b,,".split(",", -1));
        // WYNIK: "a,b,,c".split(",") → [a, b, , c]    ← pusty kawałek w środku zostaje
        // WYNIK: "a,b,,".split(",") → [a, b]    ← puste na końcu znikają!
        // WYNIK: "a,b,,".split(",", -1) → [a, b, , ]    ← limit -1: zachowaj wszystkie

        show("\"1.2.3\".split(\".\").length", "1.2.3".split(".").length);
        show("\"1.2.3\".split(\"\\\\.\")", "1.2.3".split("\\."));
        // WYNIK: "1.2.3".split(".").length → 0    ← kropka w regexie = każdy znak jest separatorem
        // WYNIK: "1.2.3".split("\\.") → [1, 2, 3]    ← \\. = dosłowna kropka

        show("słowa oddzielone wieloma spacjami", " Ala  ma   kota ".strip().split("\\s+"));
        // WYNIK: słowa oddzielone wieloma spacjami → [Ala, ma, kota]    ← \\s+ = jeden lub więcej białych znaków
    }

    // =================================================================================================
    // 8. ŁĄCZENIE I POWTARZANIE
    // =================================================================================================

    /** 8. String.join łączy elementy separatorem; repeat (Java 11+) powtarza napis; concat / + sklejają. */
    static void joiningAndRepeating() {
        section("8. join, repeat");

        show("String.join(\", \", ...)", String.join(", ", "chleb", "masło", "ser"));
        show("\"=\".repeat(10)", "=".repeat(10));
        show("\"ab\".repeat(3)", "ab".repeat(3));
        // WYNIK: String.join(", ", ...) → chleb, masło, ser
        // WYNIK: "=".repeat(10) → ==========
        // WYNIK: "ab".repeat(3) → ababab
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • length(), charAt(i) (0..length-1), substring(od) / substring(od, do) — „do” bez niego, długość = do - od.
     *   • indexOf / lastIndexOf → pozycja albo -1; contains, startsWith, endsWith → boolean.
     *   • toUpperCase(Locale.ROOT) / toLowerCase(Locale.ROOT) dla danych technicznych.
     *   • strip / stripLeading / stripTrailing (Java 11+) — białe znaki z brzegów.
     *   • replace — dosłownie, WSZYSTKIE wystąpienia; replaceAll / replaceFirst / split — REGEX (kropka to „dowolny znak”).
     *   • split: puste końcowe kawałki znikają (split(sep, -1) je zachowuje); "\\s+" = białe znaki.
     *   • String.join(separator, ...), repeat(n) (Java 11+).
     *
     * PYTANIA KONTROLNE:
     *   1. Co zwróci  "programowanie".substring(3, 7)  ?
     *   2. Co zwróci  "banan".replace("a", "o")  — „bonan” czy „bonon”?
     *   3. ZNAJDŹ BŁĄD:  String[] parts = "10.25".split(".");  double value = Double.parseDouble(parts[0]);
     *   4. Ile elementów ma  "x;y;;".split(";")  ?
     *   5. Co zwróci  "abc".indexOf("d")  i dlaczego warto to sprawdzić przed substring?
     *   6. Dlaczego do porównywania kodów lepiej  toUpperCase(Locale.ROOT)  niż samo  toUpperCase()?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: rozszerzenie \"raport.final.pdf\"", "pdf", () -> exercise1("raport.final.pdf"));
        Check.equal("ćw. 1b: rozszerzenie \"README\"", "", () -> exercise1("README"));
        Check.equal("ćw. 2: \"jAVA\" z wielkiej litery", "Java", () -> exercise2("jAVA"));
        Check.equal("ćw. 3: liczba słów", 3, () -> exercise3("  Ala  ma   kota "));
        Check.equal("ćw. 4: zamaskowany e-mail", "j***@example.com", () -> exercise4("jan.kowalski@example.com"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", "pdf", () -> solution1("raport.final.pdf"));
        Check.equal("ćw. 1b (wzorzec)", "", () -> solution1("README"));
        Check.equal("ćw. 2 (wzorzec)", "Java", () -> solution2("jAVA"));
        Check.equal("ćw. 3 (wzorzec)", 3, () -> solution3("  Ala  ma   kota "));
        Check.equal("ćw. 4 (wzorzec)", "j***@example.com", () -> solution4("jan.kowalski@example.com"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć rozszerzenie pliku (tekst po OSTATNIEJ kropce); gdy kropki nie ma — pusty napis.
     * Podpowiedź: {@code lastIndexOf('.')}; jeśli -1 → ""; w przeciwnym razie substring(pozycja + 1).
     */
    static String exercise1(String fileName) {
        // TODO: twoje rozwiązanie
        return "?";
    }

    /**
     * ĆWICZENIE 2 (średnie): pierwsza litera wielka, reszta małe: "jAVA" → "Java".
     * Podpowiedź: {@code substring(0, 1).toUpperCase(Locale.ROOT) + substring(1).toLowerCase(Locale.ROOT)}.
     */
    static String exercise2(String word) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 3 (średnie): policz słowa w tekście, w którym słowa mogą być rozdzielone wieloma spacjami,
     * a na brzegach też są spacje. "  Ala  ma   kota " → 3.
     * Podpowiedź: najpierw strip(), potem split("\\s+"), a wynik to długość tablicy. (Uwaga na pusty tekst — tu go nie ma.)
     */
    static int exercise3(String text) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zamaskuj e-mail — zostaw pierwszą literę, potem "***", potem część od znaku @:
     * "jan.kowalski@example.com" → "j***@example.com".
     * Podpowiedź: {@code indexOf('@')}, {@code charAt(0)}, {@code substring(pozycjaMałpy)}.
     */
    static String exercise4(String email) {
        // TODO: twoje rozwiązanie
        return "";
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot == -1 ? "" : fileName.substring(dot + 1);
    }

    static String solution2(String word) {
        return word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1).toLowerCase(Locale.ROOT);
    }

    static int solution3(String text) {
        return text.strip().split("\\s+").length;
    }

    static String solution4(String email) {
        int at = email.indexOf('@');
        return email.charAt(0) + "***" + email.substring(at);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. "gram" — znaki z pozycji 3, 4, 5, 6 (pozycja 7 już nie).
     *   2. "bonon" — replace zamienia WSZYSTKIE wystąpienia.
     *   3. split(".") traktuje kropkę jak „dowolny znak” — tablica jest pusta i parts[0] rzuci wyjątek.
     *      Poprawnie: split("\\.").
     *   4. 2 — [x, y]; puste kawałki na końcu są usuwane (chyba że split(";", -1)).
     *   5. -1 (nie znaleziono). substring(-1) albo substring(-1 + 1) dałyby wyjątek albo zły wynik — trzeba to obsłużyć.
     *   6. Bo bez Locale wynik zależy od języka systemu (np. turecki zamienia i na İ), więc porównanie może zawieść.
     */
    // </editor-fold>
}
