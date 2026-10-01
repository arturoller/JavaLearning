package t24_algorithms;

import helpers.Check;

import java.util.Map;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Systemy liczbowe — konwersje, kod uzupełnień do dwóch, liczby rzymskie
 *        (positional system = system pozycyjny; radix/base = podstawa; two's complement = kod
 *        uzupełnień do dwóch; Roman numerals = liczby rzymskie)
 *
 * W SKRÓCIE:
 *   Ta sama liczba wygląda inaczej w zależności od PODSTAWY zapisu: 1994 dziesiętnie to "11111001010"
 *   dwójkowo, "3712" ósemkowo i "7ca" szesnastkowo — to zawsze ta sama wartość, tylko inny "język".
 *   Java ma gotowe konwersje, ale warto rozumieć mechanizm — to podstawa bitowych sztuczek i częste
 *   pytanie rekrutacyjne (konwersje, liczby rzymskie).
 *
 * ANALOGIA: to jak zapis godziny 14:00 kontra "2 PM" — ta sama chwila, inny system zapisu. Nikt nie
 *   "przelicza czasu", tylko zmienia NOTACJĘ. Systemy liczbowe działają identycznie: wartość się nie
 *   zmienia, zmienia się tylko sposób jej zapisania cyframi.
 *
 * JAK TO DZIAŁA:
 *   Wartość w systemie o podstawie r:  suma  cyfra_i × r^i  (cyfry od 0 do r-1, i = pozycja od prawa, od 0)
 *   Konwersja LICZBA → cyfry:   dziel kolejno przez r, zbieraj RESZTY (od najmłodszej do najstarszej)
 *   Konwersja cyfry → LICZBA:   value = value × r + kolejna_cyfra, idąc od lewej do prawej
 *   Liczby ujemne (int, U2):    -n = ~n + 1 (neguj wszystkie bity, dodaj 1)
 *   Rzymskie → int:             dodawaj wartość symbolu; jeśli mniejszy od NASTĘPNEGO — odejmij (IV = 5-1)
 *
 * SŁÓWKA:
 *   positional system = system pozycyjny; radix / base = podstawa systemu liczbowego; digit = cyfra;
 *   binary = dwójkowy; octal = ósemkowy; hexadecimal = szesnastkowy; leading zero = wiodące zero;
 *   two's complement = kod uzupełnień do dwóch (U2); bitwise negation = negacja bitowa; Roman numerals
 *   = cyfry/liczby rzymskie.
 *
 * ZOBACZ TEŻ: t15_numbers/Numbers05IntegerTricks (literały 0b/0x/0, bity, sekcje 7-8 — tu idziemy głębiej
 *             w konwersje i liczby rzymskie), t24_algorithms/Math01NumberTheory (poprzednia lekcja),
 *             t24_algorithms/Math02ModularChecksums (arytmetyka modularna), t04_strings/Strings02Methods
 *             (StringBuilder.reverse używany tu do konwersji ręcznej).
 * </pre>
 */
public class Math04NumberSystems {

    public static void main(String[] args) {
        title("Math04 — systemy liczbowe: konwersje, U2, liczby rzymskie");

        positionalSystemsBasics();         // positional systems basics = podstawy systemów pozycyjnych
        builtinRadixConversion();          // builtin radix conversion = gotowe konwersje podstawy
        manualBinaryConversion();          // manual binary conversion = ręczna konwersja dwójkowa
        literalsAndLeadingZeroPitfall();   // literals and leading zero pitfall = literały i pułapka wiodącego zera
        twosComplementNegatives();         // two's complement negatives = liczby ujemne w U2
        romanNumeralsToRoman();            // Roman numerals to Roman = int na zapis rzymski
        romanNumeralsFromRoman();          // Roman numerals from Roman = zapis rzymski na int
        exercises();                       // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. SYSTEMY POZYCYJNE — CO ZNACZY "PODSTAWA" (RADIX)
    // =================================================================================================

    /**
     * 1. W systemie pozycyjnym wartość liczby to suma cyfr pomnożonych przez kolejne POTĘGI podstawy
     * (radix). Dziesiętnie znamy to machinalnie (setki, dziesiątki, jedności) — dwójkowo działa identycznie,
     * tylko podstawa to 2 zamiast 10.
     */
    static void positionalSystemsBasics() {
        section("1. Systemy pozycyjne — co znaczy \"podstawa\" (radix)");

        show("1994 jako 1*10^3 + 9*10^2 + 9*10^1 + 4*10^0", 1 * 1000 + 9 * 100 + 9 * 10 + 4);
        show("1011 (binarnie) jako 1*2^3 + 0*2^2 + 1*2^1 + 1*2^0", 1 * 8 + 0 * 4 + 1 * 2 + 1 * 1);
        // WYNIK: 1994 jako 1*10^3 + 9*10^2 + 9*10^1 + 4*10^0 → 1994
        // WYNIK: 1011 (binarnie) jako 1*2^3 + 0*2^2 + 1*2^1 + 1*2^0 → 11

        // Każdy system pozycyjny działa TAK SAMO — różni się tylko podstawa: liczba różnych cyfr
        //   (0..podstawa-1) i waga każdej pozycji (podstawa^pozycja, licząc od 0 od prawej).
    }

    // =================================================================================================
    // 2. Integer.toString(n, radix) I Integer.parseInt(s, radix)
    // =================================================================================================

    /**
     * 2. Java ma gotowe konwersje w obie strony dla dowolnej podstawy od 2 do 36 (Character.MIN_RADIX..
     * MAX_RADIX). Powyżej cyfry 9 używane są litery (10=a, 11=b, ..., 35=z) — toString zwraca je małymi
     * literami, a parseInt akceptuje obie wielkości.
     */
    static void builtinRadixConversion() {
        section("2. Integer.toString(n, radix) i Integer.parseInt(s, radix)");

        show("Integer.toString(1994, 2)", Integer.toString(1994, 2));
        show("Integer.toString(1994, 8)", Integer.toString(1994, 8));
        show("Integer.toString(1994, 16)", Integer.toString(1994, 16));
        show("Integer.toString(1994, 36)", Integer.toString(1994, 36));
        // WYNIK: Integer.toString(1994, 2) → 11111001010
        // WYNIK: Integer.toString(1994, 8) → 3712
        // WYNIK: Integer.toString(1994, 16) → 7ca
        // WYNIK: Integer.toString(1994, 36) → 1je

        show("Integer.parseInt(\"11111001010\", 2)", Integer.parseInt("11111001010", 2));
        show("Integer.parseInt(\"7ca\", 16)", Integer.parseInt("7ca", 16));
        show("Integer.parseInt(\"7CA\", 16) — wielkość liter bez znaczenia", Integer.parseInt("7CA", 16));
        // WYNIK: Integer.parseInt("11111001010", 2) → 1994
        // WYNIK: Integer.parseInt("7ca", 16) → 1994
        // WYNIK: Integer.parseInt("7CA", 16) — wielkość liter bez znaczenia → 1994
    }

    // =================================================================================================
    // 3. RĘCZNA KONWERSJA DO DWÓJKOWEGO I Z POWROTEM
    // =================================================================================================

    /**
     * 3. Konwersja LICZBA → cyfry: dziel kolejno przez podstawę, zbieraj RESZTY — pierwsza reszta to
     * najmłodsza cyfra, więc na końcu trzeba odwrócić kolejność. Konwersja cyfry → LICZBA: przechodząc
     * od lewej do prawej, za każdym razem {@code value = value * podstawa + kolejna_cyfra}.
     */
    static void manualBinaryConversion() {
        section("3. Ręczna konwersja do dwójkowego i z powrotem");

        show("toBinaryManual(1994)", toBinaryManual(1994));
        show("toBinaryManual(0)", toBinaryManual(0));
        show("toBinaryManual(1)", toBinaryManual(1));
        // WYNIK: toBinaryManual(1994) → 11111001010
        // WYNIK: toBinaryManual(0) → 0
        // WYNIK: toBinaryManual(1) → 1

        show("fromBinaryManual(\"11111001010\")", fromBinaryManual("11111001010"));
        show("fromBinaryManual(\"0\")", fromBinaryManual("0"));
        show("fromBinaryManual(toBinaryManual(1994)) == 1994 ?", fromBinaryManual(toBinaryManual(1994)) == 1994);
        // WYNIK: fromBinaryManual("11111001010") → 1994
        // WYNIK: fromBinaryManual("0") → 0
        // WYNIK: fromBinaryManual(toBinaryManual(1994)) == 1994 ? → true
    }

    /** toBinaryManual = zamień nieujemne n na zapis dwójkowy, dzieląc kolejno przez 2 i zbierając reszty. */
    static String toBinaryManual(int n) {
        if (n == 0) return "0";
        StringBuilder sb = new StringBuilder();
        while (n > 0) {
            sb.append(n % 2);                                         // reszta z dzielenia przez 2 = kolejny bit
            n /= 2;
        }
        return sb.reverse().toString();                               // zebraliśmy od najmłodszego bitu — odwróć
    }

    /** fromBinaryManual = zamień zapis dwójkowy na liczbę: value = value*2 + cyfra, idąc od lewej do prawej. */
    static long fromBinaryManual(String binary) {
        long value = 0;
        for (char c : binary.toCharArray()) {
            value = value * 2 + (c - '0');
        }
        return value;
    }

    // =================================================================================================
    // 4. LITERAŁY HEX/OCTAL/BINARNE W KODZIE — I PUŁAPKA WIODĄCEGO ZERA
    // =================================================================================================

    /**
     * 4. W kodzie Java można wpisać liczbę wprost w innym systemie: {@code 0x} (szesnastkowo),
     * {@code 0b} (dwójkowo, Java 7+) albo SAMO wiodące zero bez litery — a to oznacza system ÓSEMKOWY,
     * nie dziesiętny! To częsta pułapka dla początkujących.
     */
    static void literalsAndLeadingZeroPitfall() {
        section("4. Literały hex/octal/binarne w kodzie — i pułapka wiodącego zera");

        show("0x7CA", 0x7CA);
        show("0b11111001010", 0b11111001010);
        show("03712 (literał z wiodącym zerem — to ÓSEMKOWO!)", 03712);
        // WYNIK: 0x7CA → 1994
        // WYNIK: 0b11111001010 → 1994
        // WYNIK: 03712 (literał z wiodącym zerem — to ÓSEMKOWO!) → 1994

        // PUŁAPKA: 03712 w kodzie Java to NIE "trzy tysiące siedemset dwanaście" — to zapis ósemkowy,
        //   który wart jest dokładnie tyle, co 1994 (sprawdź: sekcja 2, Integer.toString(1994, 8) = "3712").
        //   Numer pocztowy, telefon czy identyfikator z wiodącym zerem trzymaj jako String, nigdy jako int!
        //   (szerzej: t15_numbers/Numbers05IntegerTricks, sekcja 7).
    }

    // =================================================================================================
    // 5. LICZBY UJEMNE W KODZIE UZUPEŁNIEŃ DO DWÓCH (U2)
    // =================================================================================================

    /**
     * 5. Java zapisuje liczby całkowite w kodzie uzupełnień do dwóch (U2): najstarszy bit NIE jest "plus/
     * minus", tylko częścią normalnej arytmetyki binarnej. Wzór: {@code -n = ~n + 1} — zanegować
     * (odwrócić) wszystkie bity i dodać 1.
     */
    static void twosComplementNegatives() {
        section("5. Liczby ujemne w kodzie uzupełnień do dwóch (U2)");

        show("Integer.toBinaryString(-1)", Integer.toBinaryString(-1));
        show("Integer.toBinaryString(-1994)", Integer.toBinaryString(-1994));
        show("Integer.toHexString(-1)", Integer.toHexString(-1));
        // WYNIK: Integer.toBinaryString(-1) → 11111111111111111111111111111111
        // WYNIK: Integer.toBinaryString(-1994) → 11111111111111111111100000110110
        // WYNIK: Integer.toHexString(-1) → ffffffff

        show("~1994 (negacja bitowa wszystkich bitów)", ~1994);
        show("~1994 + 1", ~1994 + 1);
        show("-1994 == ~1994 + 1 ?", -1994 == (~1994 + 1));
        // WYNIK: ~1994 (negacja bitowa wszystkich bitów) → -1995
        // WYNIK: ~1994 + 1 → -1994
        // WYNIK: -1994 == ~1994 + 1 ? → true

        // DOBRA PRAKTYKA: dzięki U2 procesor dodaje liczby dodatnie i ujemne TYM SAMYM obwodem co zwykłe
        //   dodawanie — nie musi "wiedzieć", że któraś jest ujemna. Stąd też Integer.MIN_VALUE nie ma
        //   dodatniego odpowiednika (t15_numbers/Numbers05IntegerTricks, sekcja 4).
    }

    // =================================================================================================
    // 6. LICZBY RZYMSKIE: int → ZAPIS RZYMSKI
    // =================================================================================================

    static final int[] ROMAN_VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    static final String[] ROMAN_SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    /**
     * 6. Zachłanny algorytm: bierzemy NAJWIĘKSZĄ pasującą wartość (z listy obejmującej też "oszustwa" jak
     * CM = 900, żeby ładnie obsłużyć odejmowanie) i doklejamy jej symbol, dopóki się mieści — potem
     * przechodzimy do mniejszej. PYTANIE REKRUTACYJNE: "zamień liczbę na zapis rzymski (i odwrotnie)" to
     * klasyczne zadanie programistyczne.
     */
    static void romanNumeralsToRoman() {
        section("6. Liczby rzymskie: int → zapis rzymski");

        show("toRoman(1994)", toRoman(1994));
        show("toRoman(4)", toRoman(4));
        show("toRoman(9)", toRoman(9));
        show("toRoman(2026)", toRoman(2026));
        show("toRoman(3999)", toRoman(3999));
        // WYNIK: toRoman(1994) → MCMXCIV
        // WYNIK: toRoman(4) → IV
        // WYNIK: toRoman(9) → IX
        // WYNIK: toRoman(2026) → MMXXVI
        // WYNIK: toRoman(3999) → MMMCMXCIX

        // PUŁAPKA: klasyczny zapis rzymski nie ma cyfry zero ani liczb ujemnych i tradycyjnie kończy się
        //   na 3999 (MMMCMXCIX) — większe liczby wymagają dodatkowych konwencji (np. kreski nad literami,
        //   oznaczającej mnożenie przez 1000), których ta lekcja nie implementuje.
    }

    /** toRoman = zamień dodatnią liczbę (1..3999) na zapis rzymski, algorytmem zachłannym. */
    static String toRoman(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ROMAN_VALUES.length; i++) {
            while (n >= ROMAN_VALUES[i]) {
                sb.append(ROMAN_SYMBOLS[i]);
                n -= ROMAN_VALUES[i];
            }
        }
        return sb.toString();
    }

    // =================================================================================================
    // 7. LICZBY RZYMSKIE: ZAPIS RZYMSKI → int
    // =================================================================================================

    static final Map<Character, Integer> ROMAN_DIGIT_VALUES =
            Map.of('I', 1, 'V', 5, 'X', 10, 'L', 50, 'C', 100, 'D', 500, 'M', 1000);

    /**
     * 7. Idąc od lewej do prawej, dodajemy wartość każdego symbolu — ALE jeśli symbol jest MNIEJSZY niż
     * jego sąsiad z prawej, to jest to zapis odejmujący (np. IV = V - I), więc go ODEJMUJEMY zamiast
     * dodawać. Dzięki temu "IV" (4) i "VI" (6) poprawnie wychodzą różne.
     */
    static void romanNumeralsFromRoman() {
        section("7. Liczby rzymskie: zapis rzymski → int");

        show("fromRoman(\"MCMXCIV\")", fromRoman("MCMXCIV"));
        show("fromRoman(\"IV\")", fromRoman("IV"));
        show("fromRoman(\"IX\")", fromRoman("IX"));
        show("fromRoman(\"MMXXVI\")", fromRoman("MMXXVI"));
        // WYNIK: fromRoman("MCMXCIV") → 1994
        // WYNIK: fromRoman("IV") → 4
        // WYNIK: fromRoman("IX") → 9
        // WYNIK: fromRoman("MMXXVI") → 2026

        show("fromRoman(toRoman(1994)) == 1994 ?", fromRoman(toRoman(1994)) == 1994);
        // WYNIK: fromRoman(toRoman(1994)) == 1994 ? → true
    }

    /** fromRoman = zamień POPRAWNY zapis rzymski na int (nie waliduje błędnych zapisów). */
    static int fromRoman(String roman) {
        int result = 0;
        for (int i = 0; i < roman.length(); i++) {
            int value = ROMAN_DIGIT_VALUES.get(roman.charAt(i));
            if (i + 1 < roman.length() && value < ROMAN_DIGIT_VALUES.get(roman.charAt(i + 1))) {
                result -= value;                                       // mniejszy przed większym = odejmujemy
            } else {
                result += value;
            }
        }
        return result;
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   WARTOŚĆ:        suma cyfra_i × podstawa^i (cyfry od 0 do podstawa-1, i = pozycja od prawa, od 0)
     *   GOTOWE:          Integer.toString(n, radix) / Integer.parseInt(s, radix)   (radix: 2..36)
     *   RĘCZNIE n→cyfry: dziel przez podstawę, zbieraj RESZTY, na końcu odwróć kolejność
     *   RĘCZNIE cyfry→n: value = value * podstawa + kolejna_cyfra  (od lewej do prawej)
     *   LITERAŁY:        0x... (szesnastkowo), 0b... (dwójkowo), SAMO wiodące 0 (ósemkowo!), 1_000 (podkreślenia)
     *   U2:              -n = ~n + 1; najstarszy bit to część arytmetyki, nie osobna "flaga znaku"
     *   RZYMSKIE →:      zachłannie, od największej wartości (z "oszustwami" CM, XC, IX itd. do 900/90/9...)
     *   RZYMSKIE ←:      dodawaj; gdy symbol < następny — odejmij (IV = 5 - 1); klasycznie max 3999
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego Integer.parseInt obsługuje podstawy tylko od 2 do 36? Co reprezentują litery w zapisie
     *      z podstawą większą niż 10?
     *   2. Co wypisze:  System.out.println(Integer.toString(255, 16));  ?
     *   3. ZNAJDŹ BŁĄD: ktoś pisze {@code int kodPocztowy = 00501;}, myśląc że zmienna będzie miała
     *      wartość 501. Co faktycznie się stanie?
     *   4. Dlaczego liczbę ujemną w U2 liczymy jako {@code ~n + 1}, a nie po prostu "przez zmianę znaku
     *      najstarszego bitu"?
     *   5. Co wypisze:  System.out.println(fromRoman("IX") + fromRoman("IV"));  ?
     *   6. ZNAJDŹ BŁĄD: nasza metoda fromRoman dla napisu "IIII" (zamiast poprawnego zapisu "IV") zwraca
     *      4 zamiast zgłosić błąd. Dlaczego kod tego nie wykrywa i czy to w ogóle problem?
     *   7. Jaka jest największa liczba, którą da się jednoznacznie zapisać klasycznymi cyframi rzymskimi
     *      (bez dodatkowych konwencji jak kreski nad literami)?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** Uruchamia ćwiczenia: najpierw TWOJE rozwiązania (✘ dopóki nie uzupełnisz), potem wzorcowe (✔). */
    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: 1994 w systemie 2", "11111001010", () -> exercise1(1994, 2));
        Check.equal("ćw. 1b: 255 w systemie 16", "FF", () -> exercise1(255, 16));
        Check.equal("ćw. 1c: 8 w systemie 8", "10", () -> exercise1(8, 8));
        Check.equal("ćw. 2a: \"FF\" z systemu 16", 255L, () -> exercise2("FF", 16));
        Check.equal("ćw. 2b: \"11111001010\" z systemu 2", 1994L, () -> exercise2("11111001010", 2));
        Check.equal("ćw. 2c: \"10\" z systemu 8", 8L, () -> exercise2("10", 8));
        Check.equal("ćw. 3a: liczba jedynek w 1994", 7, () -> exercise3(1994));
        Check.equal("ćw. 3b: liczba jedynek w 255", 8, () -> exercise3(255));
        Check.equal("ćw. 3c: liczba jedynek w 0", 0, () -> exercise3(0));
        Check.equal("ćw. 4a: rzymskie z 1994", "MCMXCIV", () -> exercise4(1994));
        Check.equal("ćw. 4b: rzymskie z 58", "LVIII", () -> exercise4(58));
        Check.equal("ćw. 4c: rzymskie z 3", "III", () -> exercise4(3));
        Check.equal("ćw. 5a: liczba z \"LVIII\"", 58, () -> exercise5("LVIII"));
        Check.equal("ćw. 5b: liczba z \"MCMXCIV\"", 1994, () -> exercise5("MCMXCIV"));
        Check.equal("ćw. 5c: liczba z \"III\"", 3, () -> exercise5("III"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", "11111001010", () -> solution1(1994, 2));
        Check.equal("ćw. 1b (wzorzec)", "FF", () -> solution1(255, 16));
        Check.equal("ćw. 1c (wzorzec)", "10", () -> solution1(8, 8));
        Check.equal("ćw. 2a (wzorzec)", 255L, () -> solution2("FF", 16));
        Check.equal("ćw. 2b (wzorzec)", 1994L, () -> solution2("11111001010", 2));
        Check.equal("ćw. 2c (wzorzec)", 8L, () -> solution2("10", 8));
        Check.equal("ćw. 3a (wzorzec)", 7, () -> solution3(1994));
        Check.equal("ćw. 3b (wzorzec)", 8, () -> solution3(255));
        Check.equal("ćw. 3c (wzorzec)", 0, () -> solution3(0));
        Check.equal("ćw. 4a (wzorzec)", "MCMXCIV", () -> solution4(1994));
        Check.equal("ćw. 4b (wzorzec)", "LVIII", () -> solution4(58));
        Check.equal("ćw. 4c (wzorzec)", "III", () -> solution4(3));
        Check.equal("ćw. 5a (wzorzec)", 58, () -> solution5("LVIII"));
        Check.equal("ćw. 5b (wzorzec)", 1994, () -> solution5("MCMXCIV"));
        Check.equal("ćw. 5c (wzorzec)", 3, () -> solution5("III"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 15 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zamień n (n ≥ 0) na zapis w podanej podstawie (2..16), UŻYWAJĄC WIELKICH liter
     * dla cyfr 10-15 (A-F) — bez wywoływania Integer.toString.
     * Podpowiedź: tablica znaków "0123456789ABCDEF"; dziel przez radix, zbieraj resztę jako znak z tej
     * tablicy, na końcu odwróć kolejność (StringBuilder.reverse()).
     */
    static String exercise1(int n, int radix) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 2 (łatwe): zamień napis s (zapisany w podanej podstawie, cyfry 0-9 i A-F/a-f) z powrotem
     * na liczbę — bez wywoływania Integer.parseInt.
     * Podpowiedź: {@code value = value * radix + cyfra}, idąc od lewej do prawej; Character.digit(znak,
     * radix) zamienia JEDEN znak (cyfrę albo literę) na jego wartość liczbową.
     */
    static long exercise2(String s, int radix) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 3 (średnie): policz, ile jedynek ma zapis dwójkowy liczby n (n ≥ 0) — bez wywoływania
     * Integer.bitCount.
     * Podpowiedź: skorzystaj z toBinaryManual z sekcji 3 i policz znaki '1' w wyniku.
     */
    static int exercise3(int n) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 4 (średnie): zamień n (1..3999) na zapis rzymski — bez wywoływania toRoman z sekcji 6.
     * Podpowiedź: ta sama tablica wartości/symboli (M, CM, D, CD, C, XC, L, XL, X, IX, V, IV, I) i ten sam
     * algorytm zachłanny: dopóki n ≥ wartość, doklejaj symbol i odejmuj.
     */
    static String exercise4(int n) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): zamień POPRAWNY zapis rzymski na int — bez wywoływania fromRoman
     * z sekcji 7.
     * Podpowiedź: idź od lewej do prawej, dodając wartość symbolu; gdy symbol jest MNIEJSZY niż następny
     * (np. I przed V) — odejmij go zamiast dodać.
     */
    static int exercise5(String roman) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(int n, int radix) {
        if (n == 0) return "0";
        String digits = "0123456789ABCDEF";
        StringBuilder sb = new StringBuilder();
        while (n > 0) {
            sb.append(digits.charAt(n % radix));
            n /= radix;
        }
        return sb.reverse().toString();
    }

    static long solution2(String s, int radix) {
        long value = 0;
        for (char c : s.toCharArray()) {
            value = value * radix + Character.digit(c, radix);
        }
        return value;
    }

    static int solution3(int n) {
        int count = 0;
        for (char c : toBinaryManual(n).toCharArray()) {
            if (c == '1') count++;
        }
        return count;
    }

    static String solution4(int n) {
        return toRoman(n);
    }

    static int solution5(String roman) {
        return fromRoman(roman);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Alfabet łaciński ma 26 liter, a cyfr jest 10 (0-9) — razem 36 możliwych "symboli cyfr", więc
     *      36 to maksymalna podstawa, dla której każdy symbol ma jednoznaczny znak. Litery A-Z reprezentują
     *      wartości 10-35 (A=10, B=11, ..., Z=35).
     *   2. "ff" — Integer.toString zwraca litery MAŁE, niezależnie od tego, jak zapiszesz literał w kodzie.
     *   3. Kod SIĘ SKOMPILUJE, ale wartość zmiennej NIE będzie wynosić 501 — wiodące zero (bez x/b po nim)
     *      oznacza literał ÓSEMKOWY. "00501" to zapis ósemkowy liczby 0*8^3+0*8^2+5*8+0+1 = 41 (dziesiętnie).
     *      Numery z wiodącym zerem (kody pocztowe, PESEL, numery kart) zawsze trzymaj jako String.
     *   4. "Zmiana znaku najstarszego bitu" nie działałaby spójnie z dodawaniem — procesor musiałby mieć
     *      OSOBNĄ logikę dla liczb ujemnych. U2 (~n + 1) sprawia, że dodawanie liczby dodatniej i ujemnej
     *      używa DOKŁADNIE tego samego układu binarnego dodawania co dla liczb dodatnich — prościej i szybciej.
     *   5. "13" — fromRoman("IX") = 9, fromRoman("IV") = 4, suma = 13.
     *   6. Nasza implementacja NIE waliduje poprawności zapisu — po prostu sumuje/odejmuje wartości wg
     *      reguły "mniejszy przed większym = odejmij". Dla "IIII" żaden symbol nie jest mniejszy od
     *      następnego (I, I, I, I — wszystkie równe), więc wszystkie cztery są DODAWANE: 1+1+1+1 = 4. To
     *      "przypadkiem" daje poprawną wartość, mimo że "IIII" nie jest poprawnym zapisem rzymskim
     *      (powinno być "IV") — pełna walidacja wymagałaby dodatkowych reguł (np. maks. 3 powtórzenia z rzędu).
     *   7. 3999, zapisywane jako "MMMCMXCIX" (3×M + CM + XC + IX = 3000+900+90+9). Klasyczny zapis rzymski
     *      nie ma symbolu dla zera ani sposobu na jednoznaczne przedstawienie 4000 i więcej bez dodatkowych
     *      konwencji (np. kreski nad literą oznaczającej × 1000).
     */
    // </editor-fold>
}
