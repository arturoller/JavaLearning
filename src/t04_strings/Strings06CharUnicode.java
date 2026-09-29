package t04_strings;

import helpers.Check;

import java.text.Normalizer;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Znaki (char), klasa Character, Unicode i polskie litery
 *        (character = znak; code point = punkt kodowy, numer znaku w Unicode; diacritic = znak diakrytyczny, „ogonek”)
 *
 * W SKRÓCIE:
 *   char to jeden znak zapisany jako liczba (kod UTF-16), np. 'A' = 65, 'ł' = 322. Klasa Character ma metody do
 *   sprawdzania znaków (isDigit, isLetter...). Większość znaków — także polskie litery — to jeden char. Ale emoji
 *   i niektóre rzadkie znaki zajmują DWA chary (para zastępcza), więc length() nie zawsze równa się liczbie znaków.
 *
 * ANALOGIA: szatnia z numerkami.
 *   Każdy znak ma swój numerek (kod). 'A' wisi pod numerem 65, 'ł' pod 322. Emoji się nie mieszczą na jednym
 *   wieszaku — dostają dwa numerki naraz. Kto liczy wieszaki (length), a nie płaszcze (codePointCount), policzy źle.
 *
 * JAK TO DZIAŁA:
 *   char c = 'A';        (int) c → 65        (char) 66 → 'B'        'a' + 1 → 98 (int!)
 *   Character.isDigit('7') → true            '7' - '0' → 7           (int) '7' → 55 (kod, nie cyfra!)
 *   "Łódź".length() → 4                       "😀".length() → 2       "😀".codePointCount(0, 2) → 1
 *
 * SŁÓWKA:
 *   character = znak; digit = cyfra; letter = litera; whitespace = biały znak; upper / lower case = wielka / mała litera;
 *   code point = punkt kodowy (numer znaku); surrogate pair = para zastępcza (dwa chary na jeden znak);
 *   normalize = normalizować (sprowadzić do postaci standardowej); diacritic = znak diakrytyczny; cipher = szyfr; shift = przesunięcie.
 *
 * ZOBACZ TEŻ: t01_basics/Basics02PrimitiveTypes (char jako liczba), t04_strings/Strings07TextAlgorithms,
 *             t16_streams/Streams02Creation (chars() i codePoints() w streamach).
 * </pre>
 */
public class Strings06CharUnicode {

    public static void main(String[] args) {
        title("Strings06 — znaki, Character, Unicode");

        charIsANumber();        // char is a number = char to liczba
        characterMethods();     // character methods = metody klasy Character
        digitPitfall();         // digit pitfall = pułapka z cyframi
        countingInText();       // counting in text = liczenie w tekście
        polishLetters();        // polish letters = polskie litery
        emojiAndSurrogates();   // emoji and surrogates = emoji i pary zastępcze
        removingDiacritics();   // removing diacritics = usuwanie ogonków
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. char TO LICZBA
    // =================================================================================================

    /** 1. char przechowuje kod znaku. Działania na char dają int — z powrotem na znak trzeba rzutować (char). */
    static void charIsANumber() {
        section("1. char to kod znaku");

        char letter = 'A';
        show("(int) 'A'", (int) letter);
        show("'A' + 1 (to int!)", letter + 1);
        show("(char) ('A' + 1)", (char) (letter + 1));
        show("(char) 322", (char) 322);
        // WYNIK: (int) 'A' → 65
        // WYNIK: 'A' + 1 (to int!) → 66
        // WYNIK: (char) ('A' + 1) → B
        // WYNIK: (char) 322 → ł

        // PUŁAPKA: 'a' (apostrofy) to char, "a" (cudzysłów) to String. char + char to LICZBA: 'a' + 'b' = 195.
        show("'a' + 'b'", 'a' + 'b');
        show("\"\" + 'a' + 'b'", "" + 'a' + 'b');
        // WYNIK: 'a' + 'b' → 195
        // WYNIK: "" + 'a' + 'b' → ab
    }

    // =================================================================================================
    // 2. METODY KLASY Character
    // =================================================================================================

    /** 2. Character (klasa opakowująca char) ma statyczne metody do sprawdzania i zamiany znaków. */
    static void characterMethods() {
        section("2. Character.isDigit, isLetter, isWhitespace, toUpperCase");

        show("isDigit('7') / isDigit('x')", Character.isDigit('7') + " / " + Character.isDigit('x'));
        show("isLetter('ż') / isLetter('!')", Character.isLetter('ż') + " / " + Character.isLetter('!'));
        show("isLetterOrDigit('_')", Character.isLetterOrDigit('_'));
        show("isWhitespace(' ') / isWhitespace('\\t')", Character.isWhitespace(' ') + " / " + Character.isWhitespace('\t'));
        show("isUpperCase('Ą')", Character.isUpperCase('Ą'));
        show("toUpperCase('ę')", Character.toUpperCase('ę'));
        // WYNIK: isDigit('7') / isDigit('x') → true / false
        // WYNIK: isLetter('ż') / isLetter('!') → true / false
        // WYNIK: isLetterOrDigit('_') → false
        // WYNIK: isWhitespace(' ') / isWhitespace('\t') → true / true
        // WYNIK: isUpperCase('Ą') → true
        // WYNIK: toUpperCase('ę') → Ę
    }

    // =================================================================================================
    // 3. PUŁAPKA: ZNAK CYFRY TO NIE CYFRA
    // =================================================================================================

    /** 3. Znak '7' ma kod 55. Żeby dostać liczbę 7: '7' - '0' albo Character.getNumericValue('7'). */
    static void digitPitfall() {
        section("3. '7' to nie 7");

        char digit = '7';
        show("(int) '7'", (int) digit);
        show("'7' - '0'", digit - '0');
        show("Character.getNumericValue('7')", Character.getNumericValue(digit));
        // WYNIK: (int) '7' → 55    ← kod znaku, nie wartość cyfry!
        // WYNIK: '7' - '0' → 7    ← kody cyfr idą po kolei: '0' = 48, '1' = 49...
        // WYNIK: Character.getNumericValue('7') → 7
    }

    // =================================================================================================
    // 4. LICZENIE ZNAKÓW W TEKŚCIE
    // =================================================================================================

    /** 4. Typowe zadanie: przejdź po znakach napisu (charAt w pętli) i policz litery, cyfry, spacje. */
    static void countingInText() {
        section("4. Liczenie liter, cyfr i spacji");

        String text = "Zamówienie nr 42 z 29 września";
        int letters = 0;
        int digits = 0;
        int spaces = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c)) {
                letters++;
            } else if (Character.isDigit(c)) {
                digits++;
            } else if (Character.isWhitespace(c)) {
                spaces++;
            }
        }
        show("litery / cyfry / spacje", letters + " / " + digits + " / " + spaces);
        // WYNIK: litery / cyfry / spacje → 21 / 4 / 5
    }

    // =================================================================================================
    // 5. POLSKIE LITERY
    // =================================================================================================

    /** 5. Polskie litery to zwykłe znaki Unicode — jeden char każda. Działają z isLetter, toUpperCase itd. */
    static void polishLetters() {
        section("5. Polskie litery w Unicode");

        show("\"Łódź\".length()", "Łódź".length());
        show("kod 'ł' / 'Ł'", (int) 'ł' + " / " + (int) 'Ł');
        show("\"zażółć\".toUpperCase()", "zażółć".toUpperCase());
        // WYNIK: "Łódź".length() → 4
        // WYNIK: kod 'ł' / 'Ł' → 322 / 321
        // WYNIK: "zażółć".toUpperCase() → ZAŻÓŁĆ

        // PUŁAPKA: kody polskich liter są daleko za łacińskimi (ł = 322, z = 122), więc zwykłe sortowanie stawia „łódź”
        //   za „zamość”. Po polsku sortuje Collator (t16_streams/Streams05SortDistinctLimit).
    }

    // =================================================================================================
    // 6. EMOJI I PARY ZASTĘPCZE
    // =================================================================================================

    /**
     * 6. char ma 16 bitów — mieści 65 536 kodów. Emoji i rzadkie znaki mają wyższe numery, więc zajmują DWA chary
     * (parę zastępczą). length() liczy chary, codePointCount — prawdziwe znaki.
     */
    static void emojiAndSurrogates() {
        section("6. Emoji: jeden znak, dwa chary");

        String smile = "😀";
        show("length()", smile.length());
        show("codePointCount", smile.codePointCount(0, smile.length()));
        show("\"Hej😀\".length() / liczba znaków", "Hej😀".length() + " / " + "Hej😀".codePointCount(0, "Hej😀".length()));
        // WYNIK: length() → 2
        // WYNIK: codePointCount → 1
        // WYNIK: "Hej😀".length() / liczba znaków → 5 / 4

        // PUŁAPKA: charAt(0) na emoji zwraca POŁOWĘ znaku (bezużyteczną), a substring może go „przeciąć” na pół.
        //   Gdy tekst może zawierać emoji (czaty, komentarze), licz i przechodź po codePoints(), nie po charach.
    }

    // =================================================================================================
    // 7. USUWANIE POLSKICH ZNAKÓW
    // =================================================================================================

    /**
     * 7. Normalizer rozkłada litery z ogonkami na literę + znak ogonka (NFD), a regex \p{M} usuwa same ogonki.
     * Wyjątek: „ł” nie jest „l z ogonkiem” w Unicode — trzeba ją zamienić ręcznie.
     */
    static void removingDiacritics() {
        section("7. Usuwanie ogonków (np. do adresów URL, loginów)");

        String text = "Zażółć gęślą jaźń";
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);   // rozłóż: ż → z + kropka
        String noMarks = decomposed.replaceAll("\\p{M}", "");                  // usuń same „ogonki” (Mark)
        show("po NFD i usunięciu \\p{M}", noMarks);
        show("po zamianie ł/Ł", noMarks.replace('ł', 'l').replace('Ł', 'L'));
        // WYNIK: po NFD i usunięciu \p{M} → Zazołc gesla jazn    ← „ł” zostało!
        // WYNIK: po zamianie ł/Ł → Zazolc gesla jazn
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • char = kod znaku: (int) 'A' = 65, (char) 66 = 'B'; char + char / char + int → int.
     *   • 'a' to char, "a" to String; "" + 'a' + 'b' = "ab".
     *   • Character.isDigit / isLetter / isLetterOrDigit / isWhitespace / isUpperCase / toUpperCase.
     *   • Cyfra ze znaku: c - '0' albo Character.getNumericValue(c); (int) '7' = 55!
     *   • Polskie litery: 1 char, działają ze wszystkimi metodami; sortowanie po polsku → Collator.
     *   • Emoji = 2 chary: length() ≠ liczba znaków → codePointCount / codePoints().
     *   • Bez ogonków: Normalizer NFD + replaceAll("\\p{M}", "") + ręcznie ł → l.
     *
     * PYTANIA KONTROLNE:
     *   1. Co wypisze:  System.out.println('b' + 1);  a co  System.out.println((char) ('b' + 1));  ?
     *   2. ZNAJDŹ BŁĄD:  int value = (int) '9';   // „chcę dostać 9”
     *   3. Ile wynosi  "Żółw".length()  a ile  "👍".length()  ?
     *   4. Dlaczego zwykłe sortowanie stawia „łosoś” za „zebra”?
     *   5. Co zwróci  Character.isLetter('_')  ?
     *   6. Dlaczego po usunięciu ogonków przez Normalizer w „Łódź” zostaje „Ł”?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: 'E' to samogłoska", true, () -> exercise1('E'));
        Check.equal("ćw. 1b: 'ą' to samogłoska", true, () -> exercise1('ą'));
        Check.equal("ćw. 1c: 'k' to nie samogłoska", false, () -> exercise1('k'));
        Check.equal("ćw. 2: szyfr Cezara \"abcz\" +1", "bcda", () -> exercise2("abcz", 1));
        Check.equal("ćw. 3: \"Łódź Źródło\" bez ogonków", "Lodz Zrodlo", () -> exercise3("Łódź Źródło"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", true, () -> solution1('E'));
        Check.equal("ćw. 1b (wzorzec)", true, () -> solution1('ą'));
        Check.equal("ćw. 1c (wzorzec)", false, () -> solution1('k'));
        Check.equal("ćw. 2 (wzorzec)", "bcda", () -> solution2("abcz", 1));
        Check.equal("ćw. 3 (wzorzec)", "Lodz Zrodlo", () -> solution3("Łódź Źródło"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): czy znak to polska samogłoska (a ą e ę i o ó u y), bez względu na wielkość litery?
     * Podpowiedź: {@code "aąeęioóuy".indexOf(Character.toLowerCase(c)) >= 0}.
     */
    static boolean exercise1(char c) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): szyfr Cezara dla małych liter a–z: każdą literę przesuń o shift pozycji w alfabecie,
     * „zawijając” za z: ("abcz", 1) → "bcda".
     * Podpowiedź: {@code (char) ('a' + (c - 'a' + shift) % 26)} — % 26 robi „zawinięcie”.
     */
    static String exercise2(String text, int shift) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): usuń polskie znaki diakrytyczne: "Łódź Źródło" → "Lodz Zrodlo".
     * Podpowiedź: sekcja 7 — Normalizer.normalize(text, Normalizer.Form.NFD), replaceAll("\\p{M}", ""),
     * a na końcu ręcznie ł → l i Ł → L.
     */
    static String exercise3(String text) {
        // TODO: twoje rozwiązanie
        return "";
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static boolean solution1(char c) {
        return "aąeęioóuy".indexOf(Character.toLowerCase(c)) >= 0;
    }

    static String solution2(String text, int shift) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            sb.append((char) ('a' + (c - 'a' + shift) % 26));
        }
        return sb.toString();
    }

    static String solution3(String text) {
        String noMarks = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noMarks.replace('ł', 'l').replace('Ł', 'L');
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. 99 (char + int to int: 'b' ma kod 98) oraz c (po rzutowaniu na char).
     *   2. (int) '9' to KOD znaku (57), nie cyfra 9. Poprawnie: '9' - '0' albo Character.getNumericValue('9').
     *   3. 4 (polskie litery to po jednym char) oraz 2 (emoji to para zastępcza — dwa chary).
     *   4. Bo porównuje kody znaków: 'ł' ma kod 322, a 'z' — 122.
     *   5. false — podkreślnik nie jest literą (choć jest dozwolony w nazwach zmiennych w Javie).
     *   6. Bo w Unicode „Ł” nie jest „L z ogonkiem” do rozłożenia, tylko osobną literą — trzeba ją zamienić ręcznie.
     */
    // </editor-fold>
}
