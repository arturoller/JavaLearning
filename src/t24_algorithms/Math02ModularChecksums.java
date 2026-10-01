package t24_algorithms;

import helpers.Check;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Locale;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Arytmetyka modularna i sumy kontrolne — od floorMod po PESEL, NIP i IBAN
 *        (modular arithmetic = arytmetyka modularna; checksum = suma kontrolna; check digit = cyfra
 *        kontrolna; exponentiation = potęgowanie)
 *
 * W SKRÓCIE:
 *   Reszta z dzielenia (modulo) to podstawa sum kontrolnych: numerów kart, PESEL-i, NIP-ów i kont
 *   bankowych (IBAN). Każdy z tych numerów ma "nadmiarową" cyfrę, policzoną wzorem — dzięki niej program
 *   wykryje literówkę PRZED wysłaniem danych dalej, bez sprawdzania w żadnej bazie.
 *
 * ANALOGIA: suma kontrolna to jak suma na paragonie: kasjer nie musi pamiętać każdej ceny, żeby
 *   zauważyć, że coś się nie zgadza — wystarczy, że suma pozycji nie pasuje do sumy na dole. Jeśli ktoś
 *   pomyli jedną cyfrę ceny, suma "nie wyjdzie" — dokładnie tak jak błędna cyfra kontrolna.
 *
 * JAK TO DZIAŁA:
 *   Modulo nieujemne:    Math.floorMod(a, b) zawsze zwraca wynik z zakresu 0..b-1 (dla b > 0)
 *   Potęgowanie modularne: square-and-multiply — rozkład wykładnika na bity, O(log wykładnika)
 *   Luhn:                co druga cyfra (od prawa) × 2, przy dwucyfrowym wyniku odejmij 9, suma % 10 == 0
 *   PESEL:               RRMMDD (miesiąc koduje stulecie) + numer porządkowy + cyfra kontrolna (wagi
 *                         1,3,7,9,1,3,7,9,1,3)
 *   NIP:                 9 cyfr firmy + cyfra kontrolna (wagi 6,5,7,2,3,4,5,6,7), suma mod 11
 *   IBAN:                przenieś pierwsze 4 znaki na koniec, litery → liczby (A=10..Z=35), całość mod 97 == 1
 *
 * SŁÓWKA:
 *   checksum = suma kontrolna; check digit = cyfra kontrolna; modular arithmetic = arytmetyka modularna;
 *   exponentiation = potęgowanie; square-and-multiply = „podnieś do kwadratu i pomnóż” (szybkie
 *   potęgowanie); ordinal number = numer porządkowy; prefix = przedrostek (tu: pierwsze cyfry numeru);
 *   remainder = reszta z dzielenia; century = stulecie; tampered = sfałszowany/zmieniony.
 *
 * ZOBACZ TEŻ: t15_numbers/Numbers05IntegerTricks (floorMod kontra %, przepełnienie), t15_numbers/
 *             Numbers03BigInteger (BigInteger.modPow, liczby bez limitu), t24_algorithms/Math01NumberTheory
 *             (poprzednia lekcja — NWD/NWW/liczby pierwsze), t24_algorithms/Math08BigNumbers (duże liczby
 *             dalej w tym rozdziale), t17_datetime/DateTime01LocalDateTime (LocalDate).
 * </pre>
 */
public class Math02ModularChecksums {

    public static void main(String[] args) {
        title("Math02 — arytmetyka modularna i sumy kontrolne");

        modularArithmeticBasics();         // modular arithmetic basics = podstawy arytmetyki modularnej
        fastModularExponentiation();       // fast modular exponentiation = szybkie potęgowanie modularne
        luhnCheckDigitDemo();              // Luhn check digit = cyfra kontrolna Luhna
        peselCheckDigitAndDecoding();      // PESEL check digit and decoding = cyfra kontrolna i dekodowanie PESEL
        nipCheckDigitDemo();               // NIP check digit = cyfra kontrolna NIP
        ibanMod97();                       // IBAN mod 97
        checksumIsNotExistenceProof();     // checksum is not existence proof = suma kontrolna to nie dowód istnienia
        exercises();                       // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. MODULO LICZB UJEMNYCH — floorMod KONTRA % (PRZYPOMNIENIE)
    // =================================================================================================

    /**
     * 1. Operator {@code %} ma znak DZIELNEJ (może dać liczbę ujemną), a {@code Math.floorMod(a, b)} dla
     * dodatniego b zawsze zwraca wynik z zakresu 0..b-1. Sumy kontrolne zawsze chcą tego DRUGIEGO —
     * cyfra kontrolna to konkretna cyfra 0-9, nigdy liczba ujemna.
     */
    static void modularArithmeticBasics() {
        section("1. Modulo liczb ujemnych — floorMod kontra % (przypomnienie)");

        show("-7 % 3", -7 % 3);
        show("Math.floorMod(-7, 3)", Math.floorMod(-7, 3));
        show("-1 % 10", -1 % 10);
        show("Math.floorMod(-1, 10)", Math.floorMod(-1, 10));
        // WYNIK: -7 % 3 → -1
        // WYNIK: Math.floorMod(-7, 3) → 2
        // WYNIK: -1 % 10 → -1
        // WYNIK: Math.floorMod(-1, 10) → 9

        // PUŁAPKA: gdybyśmy policzyli cyfrę kontrolną wzorem z operatorem % i wynik pośredni wyszedł
        //   ujemny, porównanie "cyfraKontrolna == -1" NIGDY nie zrówna się z prawdziwą cyfrą 0-9 — kod
        //   zgłosi fałszywy błąd. Dlatego w tej lekcji wszystkie wzory używają floorMod albo dodania
        //   modułu przed resztą. Szerzej o różnicy: t15_numbers/Numbers05IntegerTricks, sekcja 6.
    }

    // =================================================================================================
    // 2. SZYBKIE POTĘGOWANIE MODULARNE (SQUARE-AND-MULTIPLY)
    // =================================================================================================

    /**
     * 2. Naiwnie {@code base^exponent mod modulus} to exponent mnożeń — dla RSA z wykładnikiem rzędu
     * 2^16 to zbyt wolno. Square-and-multiply rozkłada wykładnik na bity: przy KAŻDYM bicie podnosimy
     * podstawę do kwadratu, a mnożymy wynik tylko wtedy, gdy bit jest 1. To O(log exponent) mnożeń.
     * PYTANIE REKRUTACYJNE: "zaimplementuj szybkie potęgowanie modularne" pojawia się w rozmowach
     * dotyczących kryptografii i algorytmów.
     */
    static void fastModularExponentiation() {
        section("2. Szybkie potęgowanie modularne (square-and-multiply)");

        show("modPow(7, 128, 13) — naiwnie byłoby 128 mnożeń, tu ok. 7", modPow(7, 128, 13));
        show("modPow(2, 10, 1000)", modPow(2, 10, 1000));
        show("modPow(3, 13, 7)", modPow(3, 13, 7));
        // WYNIK: modPow(7, 128, 13) — naiwnie byłoby 128 mnożeń, tu ok. 7 → 3
        // WYNIK: modPow(2, 10, 1000) → 24
        // WYNIK: modPow(3, 13, 7) → 3

        BigInteger reference = BigInteger.valueOf(7).modPow(BigInteger.valueOf(128), BigInteger.valueOf(13));
        show("BigInteger.modPow(7, 128, 13) — kontrola", reference.longValueExact());
        // WYNIK: BigInteger.modPow(7, 128, 13) — kontrola → 3

        // DOBRA PRAKTYKA: w prawdziwym kodzie kryptograficznym używaj BigInteger.modPow (gotowe,
        //   przetestowane) — własną wersję piszemy tu, żeby zrozumieć MECHANIZM square-and-multiply.
    }

    /** modPow = base^exponent mod modulus, metodą square-and-multiply (zakłada exponent >= 0, modulus >= 1). */
    static long modPow(long base, long exponent, long modulus) {
        if (modulus == 1) return 0;
        long result = 1;
        long b = base % modulus;
        long e = exponent;
        while (e > 0) {
            if ((e & 1) == 1) {                                       // najmłodszy bit ustawiony → mnożymy
                result = (result * b) % modulus;
            }
            e >>= 1;                                                  // przesuń do następnego bitu
            b = (b * b) % modulus;                                    // podnieś podstawę do kwadratu
        }
        return result;
    }

    // =================================================================================================
    // 3. LUHN — SUMA KONTROLNA NUMERÓW KART PŁATNICZYCH
    // =================================================================================================

    /**
     * 3. Algorytm Luhna: idąc OD PRAWEJ, co drugą cyfrę podwajamy; jeśli wynik ma dwie cyfry, odejmujemy
     * 9 (równoważne zsumowaniu jego cyfr). Suma wszystkich (zmodyfikowanych i niezmodyfikowanych) cyfr
     * musi być podzielna przez 10. PYTANIE REKRUTACYJNE: "zaimplementuj walidację numeru karty" to
     * klasyczne zadanie na rozmowach rekrutacyjnych dla programistów.
     */
    static void luhnCheckDigitDemo() {
        section("3. Luhn — suma kontrolna numerów kart płatniczych");

        // Numery TESTOWE, powszechnie używane w samouczkach do nauki algorytmu Luhna — nie są
        //   przypisane do żadnej prawdziwej karty ani osoby.
        show("luhnValid(\"4539148803436467\")", luhnValid("4539148803436467"));
        show("luhnValid(\"4539148803436468\")", luhnValid("4539148803436468")); // ostatnia cyfra zmieniona o 1
        show("luhnValid(\"79927398713\")", luhnValid("79927398713"));           // klasyczny przykład testowy
        // WYNIK: luhnValid("4539148803436467") → true
        // WYNIK: luhnValid("4539148803436468") → false
        // WYNIK: luhnValid("79927398713") → true

        // PUŁAPKA: zmiana JEDNEJ cyfry prawie zawsze psuje sumę kontrolną, ale Luhn NIE wykrywa
        //   wszystkich błędów (np. niektórych zamian sąsiednich cyfr, jak 09 ↔ 90). To filtr pierwszej
        //   linii (literówka przy przepisywaniu), a nie dowód, że karta istnieje i jest aktywna.
    }

    /** luhnValid = czy ciąg cyfr spełnia sumę kontrolną Luhna (sum % 10 == 0). */
    static boolean luhnValid(String digits) {
        int sum = 0;
        boolean doubleIt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (doubleIt) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }

    /** luhnCheckDigit = oblicz BRAKUJĄCĄ cyfrę kontrolną, którą trzeba dopisać na końcu prefixDigits. */
    static int luhnCheckDigit(String prefixDigits) {
        int sum = 0;
        boolean doubleIt = true;                                      // ostatnia cyfra prefiksu będzie DRUGA od prawa
        for (int i = prefixDigits.length() - 1; i >= 0; i--) {
            int d = prefixDigits.charAt(i) - '0';
            if (doubleIt) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
            doubleIt = !doubleIt;
        }
        return (10 - sum % 10) % 10;
    }

    // =================================================================================================
    // 4. PESEL — CYFRA KONTROLNA I DEKODOWANIE DATY URODZENIA / PŁCI
    // =================================================================================================

    static final int[] PESEL_WEIGHTS = {1, 3, 7, 9, 1, 3, 7, 9, 1, 3};

    /**
     * 4. PESEL ma 11 cyfr: RRMMDD (data urodzenia; MIESIĄC koduje stulecie), 3 cyfry numeru porządkowego,
     * 1 cyfra płci (nieparzysta = mężczyzna, parzysta = kobieta) i cyfra kontrolna z wagami
     * 1,3,7,9,1,3,7,9,1,3 dla pierwszych 10 cyfr: suma ważona, cyfra = (10 - suma mod 10) mod 10.
     * UWAGA: poniższe numery PESEL są W PEŁNI ZMYŚLONE — wygenerowane tu w kodzie dla fikcyjnych osób.
     * Nigdy nie wpisuj do kodu (ani do żadnego przykładu) prawdziwego numeru PESEL, nawet własnego.
     */
    static void peselCheckDigitAndDecoding() {
        section("4. PESEL — cyfra kontrolna i dekodowanie daty urodzenia / płci");

        String fictionalMale = buildPesel(LocalDate.of(2000, 1, 1), "0425");   // fikcyjny mężczyzna
        show("fikcyjny PESEL (mężczyzna)", fictionalMale);
        show("suma kontrolna poprawna?", peselChecksumValid(fictionalMale));
        show("odczytana data urodzenia", peselBirthDate(fictionalMale));
        show("odczytana płeć", peselSex(fictionalMale));
        // WYNIK: fikcyjny PESEL (mężczyzna) → 00210104251
        // WYNIK: suma kontrolna poprawna? → true
        // WYNIK: odczytana data urodzenia → 2000-01-01
        // WYNIK: odczytana płeć → mężczyzna

        String tampered = fictionalMale.substring(0, 10) + ((fictionalMale.charAt(10) - '0' + 1) % 10);
        show("ten sam numer z BŁĘDNĄ cyfrą kontrolną", tampered);
        show("suma kontrolna poprawna?", peselChecksumValid(tampered));
        // WYNIK: ten sam numer z BŁĘDNĄ cyfrą kontrolną → 00210104252
        // WYNIK: suma kontrolna poprawna? → false

        // DOBRA PRAKTYKA: sprawdzaj sumę kontrolną PRZED próbą odczytania daty/płci — dla losowego
        //   11-cyfrowego ciągu peselBirthDate może nawet rzucić wyjątek (zły zakres miesiąca/dnia).
    }

    /** encodeMonth = zakoduj miesiąc razem ze stuleciem urodzenia wg reguł PESEL. */
    static int encodeMonth(int year, int month) {
        if (year >= 1900 && year <= 1999) return month;
        if (year >= 2000 && year <= 2099) return month + 20;
        if (year >= 1800 && year <= 1899) return month + 80;
        if (year >= 2100 && year <= 2199) return month + 40;
        if (year >= 2200 && year <= 2299) return month + 60;
        throw new IllegalArgumentException("Rok poza obsługiwanym zakresem PESEL: " + year);
    }

    /** buildPesel = złóż (fikcyjny!) PESEL z daty urodzenia i 4-cyfrowego numeru porządkowego. */
    static String buildPesel(LocalDate birthDate, String fourDigitOrdinal) {
        int year = birthDate.getYear() % 100;
        int month = encodeMonth(birthDate.getYear(), birthDate.getMonthValue());
        int day = birthDate.getDayOfMonth();
        String first10 = String.format(Locale.ROOT, "%02d%02d%02d%s", year, month, day, fourDigitOrdinal);
        return first10 + peselCheckDigit(first10);
    }

    /** peselCheckDigit = cyfra kontrolna dla pierwszych 10 cyfr PESEL. */
    static int peselCheckDigit(String first10) {
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += (first10.charAt(i) - '0') * PESEL_WEIGHTS[i];
        }
        return (10 - sum % 10) % 10;
    }

    /** peselChecksumValid = czy 11-cyfrowy PESEL ma poprawną cyfrę kontrolną. */
    static boolean peselChecksumValid(String pesel) {
        if (pesel.length() != 11) return false;
        int expected = peselCheckDigit(pesel.substring(0, 10));
        int actual = pesel.charAt(10) - '0';
        return expected == actual;
    }

    /** peselBirthDate = odczytaj datę urodzenia z PESEL (nie sprawdza sumy kontrolnej). */
    static LocalDate peselBirthDate(String pesel) {
        int year = Integer.parseInt(pesel.substring(0, 2));
        int month = Integer.parseInt(pesel.substring(2, 4));
        int day = Integer.parseInt(pesel.substring(4, 6));
        int century;
        int realMonth;
        if (month >= 1 && month <= 12) { century = 1900; realMonth = month; }
        else if (month >= 21 && month <= 32) { century = 2000; realMonth = month - 20; }
        else if (month >= 81 && month <= 92) { century = 1800; realMonth = month - 80; }
        else if (month >= 41 && month <= 52) { century = 2100; realMonth = month - 40; }
        else if (month >= 61 && month <= 72) { century = 2200; realMonth = month - 60; }
        else throw new IllegalArgumentException("Nieprawidłowy miesiąc w PESEL: " + month);
        return LocalDate.of(century + year, realMonth, day);
    }

    /** peselSex = odczytaj płeć z PESEL: 10. cyfra nieparzysta = mężczyzna, parzysta = kobieta. */
    static String peselSex(String pesel) {
        int sexDigit = pesel.charAt(9) - '0';
        return sexDigit % 2 == 1 ? "mężczyzna" : "kobieta";
    }

    // =================================================================================================
    // 5. NIP — CYFRA KONTROLNA (WAGI 6, 5, 7, 2, 3, 4, 5, 6, 7)
    // =================================================================================================

    static final int[] NIP_WEIGHTS = {6, 5, 7, 2, 3, 4, 5, 6, 7};

    /**
     * 5. NIP ma 10 cyfr: pierwsze 9 to numer firmy, 10. to cyfra kontrolna = (suma ważona) mod 11, z
     * wagami 6,5,7,2,3,4,5,6,7. Gdy wynik wychodzi 10 — taki prefiks jest NIEUŻYWALNY (nie da się
     * zapisać jedną cyfrą) i NIP z takim początkiem nigdy nie zostanie nadany.
     */
    static void nipCheckDigitDemo() {
        section("5. NIP — cyfra kontrolna (wagi 6, 5, 7, 2, 3, 4, 5, 6, 7)");

        show("nipChecksum(\"123456789\")", nipChecksum("123456789"));
        show("nipCheckDigitOrInvalid(\"123456789\")", nipCheckDigitOrInvalid("123456789"));
        // WYNIK: nipChecksum("123456789") → 10
        // WYNIK: nipCheckDigitOrInvalid("123456789") → -1

        show("nipChecksum(\"123456780\")", nipChecksum("123456780"));
        int checkDigit = nipCheckDigitOrInvalid("123456780");
        String fictionalNip = "123456780" + checkDigit;
        show("fikcyjny poprawny NIP", fictionalNip);
        show("nipValid(fikcyjny NIP)", nipValid(fictionalNip));
        // WYNIK: nipChecksum("123456780") → 2
        // WYNIK: fikcyjny poprawny NIP → 1234567802
        // WYNIK: nipValid(fikcyjny NIP) → true

        // UWAGA: oba przykłady są W PEŁNI FIKCYJNE (wygenerowane w kodzie do nauki wzoru) — nie
        //   odpowiadają żadnej zarejestrowanej firmie.
        // PUŁAPKA: -1 jako "cyfra kontrolna" to sygnał dla programisty (prefiks nieużywalny), nie wynik
        //   do wypisania użytkownikowi — zawsze sprawdzaj ten przypadek PRZED użyciem wyniku.
    }

    /** nipChecksum = suma ważona mod 11 dla pierwszych 9 cyfr NIP. */
    static int nipChecksum(String firstNine) {
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (firstNine.charAt(i) - '0') * NIP_WEIGHTS[i];
        }
        return sum % 11;
    }

    /** nipCheckDigitOrInvalid = cyfra kontrolna NIP, albo -1 gdy prefiks jest nieużywalny (suma mod 11 == 10). */
    static int nipCheckDigitOrInvalid(String firstNine) {
        int checksum = nipChecksum(firstNine);
        return checksum == 10 ? -1 : checksum;
    }

    /** nipValid = czy 10-cyfrowy NIP ma poprawną cyfrę kontrolną. */
    static boolean nipValid(String tenDigits) {
        if (tenDigits.length() != 10) return false;
        int expected = nipCheckDigitOrInvalid(tenDigits.substring(0, 9));
        if (expected == -1) return false;
        return expected == (tenDigits.charAt(9) - '0');
    }

    // =================================================================================================
    // 6. IBAN — SUMA KONTROLNA MOD 97
    // =================================================================================================

    /**
     * 6. Walidacja IBAN: usuń spacje, PRZENIEŚ pierwsze 4 znaki (kod kraju + cyfry kontrolne IBAN) na
     * koniec, zamień litery na liczby ({@code A=10, B=11, ..., Z=35}), potraktuj całość jako JEDNĄ wielką
     * liczbę i policz resztę z dzielenia przez 97 — dla poprawnego IBAN wynosi ona dokładnie 1. Liczba ma
     * za dużo cyfr na long, więc liczymy ją przez BigInteger (t15_numbers/Numbers03BigInteger).
     */
    static void ibanMod97() {
        section("6. IBAN — suma kontrolna mod 97");

        // GB82 WEST 1234 5698 7654 32 — oficjalny przykładowy IBAN ze standardu ISO 13616 (używany
        //   w dokumentacji i testach bibliotek bankowych na całym świecie).
        String iban = "GB82WEST12345698765432";
        show("IBAN (bez spacji)", iban);
        show("po przestawieniu i zamianie liter na cyfry", ibanToNumericString(iban));
        show("mod 97 (poprawny IBAN → 1)", ibanRemainder(iban));
        show("ibanValid(GB82...)", ibanValid(iban));
        // WYNIK: IBAN (bez spacji) → GB82WEST12345698765432
        // WYNIK: po przestawieniu i zamianie liter na cyfry → 3214282912345698765432161182
        // WYNIK: mod 97 (poprawny IBAN → 1) → 1
        // WYNIK: ibanValid(GB82...) → true

        String brokenIban = "GB83WEST12345698765432";                 // zmieniona cyfra kontrolna 82 → 83
        show("ibanValid(GB83... — zmieniona cyfra)", ibanValid(brokenIban));
        // WYNIK: ibanValid(GB83... — zmieniona cyfra) → false
    }

    /** ibanToNumericString = IBAN po przestawieniu pierwszych 4 znaków na koniec i zamianie liter na cyfry. */
    static String ibanToNumericString(String iban) {
        String rearranged = iban.substring(4) + iban.substring(0, 4);
        StringBuilder sb = new StringBuilder();
        for (char c : rearranged.toCharArray()) {
            if (Character.isLetter(c)) {
                sb.append(Character.getNumericValue(c));               // A=10, B=11, ..., Z=35
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** ibanRemainder = (IBAN jako wielka liczba) mod 97. */
    static int ibanRemainder(String iban) {
        String numeric = ibanToNumericString(iban);
        return new BigInteger(numeric).mod(BigInteger.valueOf(97)).intValue();
    }

    /** ibanValid = czy IBAN ma poprawną sumę kontrolną (reszta z mod 97 równa dokładnie 1). */
    static boolean ibanValid(String iban) {
        return ibanRemainder(iban) == 1;
    }

    // =================================================================================================
    // 7. SUMA KONTROLNA TO NIE DOWÓD ISTNIENIA
    // =================================================================================================

    /**
     * 7. Poprawna suma kontrolna oznacza tylko, że numer ma DOBRY FORMAT — NIE że taka osoba, firma czy
     * konto istnieją naprawdę. To jak poprawnie zbudowane zdanie, które może być kłamstwem: gramatyka się
     * zgadza, ale treść może być fałszywa. Prawdziwą odpowiedź daje baza danych (rejestr PESEL, baza
     * podatkowa, system bankowy), a nie sama arytmetyka.
     */
    static void checksumIsNotExistenceProof() {
        section("7. Suma kontrolna to NIE dowód istnienia");

        show("fikcyjny PESEL \"00210104251\" ma poprawną sumę kontrolną?", peselChecksumValid("00210104251"));
        // WYNIK: fikcyjny PESEL "00210104251" ma poprawną sumę kontrolną? → true

        // Ten PESEL jest w 100% wymyślony na potrzeby tej lekcji — mimo poprawnej arytmetyki prawie na
        //   pewno NIE figuruje w rejestrze PESEL. To samo dotyczy fikcyjnego NIP z sekcji 5.
        // DOBRA PRAKTYKA: suma kontrolna to szybki filtr literówek PRZED wysłaniem zapytania do
        //   prawdziwej bazy (rejestru, banku) — nigdy nie zastępuje tej bazy ani weryfikacji tożsamości.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   MODULO:          Math.floorMod(a, b) → zawsze 0..b-1 (dla b > 0); zwykłe % może dać wynik ujemny
     *   SQUARE-AND-MULTIPLY: kwadrat przy każdym bicie wykładnika, mnożenie tylko gdy bit == 1  [O(log e)]
     *   LUHN:            od prawa co druga cyfra ×2 (wynik > 9 → odejmij 9), suma % 10 == 0
     *   PESEL:           RRMMDD (miesiąc + 20/40/60/80 wg stulecia) + numer porządkowy + cyfra kontrolna
     *                     (wagi 1,3,7,9,1,3,7,9,1,3); 10. cyfra: nieparzysta = M, parzysta = K
     *   NIP:             9 cyfr firmy + cyfra kontrolna (wagi 6,5,7,2,3,4,5,6,7) mod 11; wynik 10 → NIEUŻYWALNE
     *   IBAN:            przenieś pierwsze 4 znaki na koniec, litery → cyfry (A=10..Z=35), mod 97 == 1
     *   WAŻNE:           poprawna suma kontrolna = dobry FORMAT, nie dowód, że numer istnieje naprawdę
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego do sum kontrolnych używamy Math.floorMod, a nie zwykłego operatora %?
     *   2. Co wypisze:  System.out.println(Math.floorMod(-1, 10));  ?
     *   3. ZNAJDŹ BŁĄD (złożoność): metoda potęgowania modularnego
     *          static long slowModPow(long base, long exponent, long modulus) {
     *              long result = 1;
     *              for (long i = 0; i < exponent; i++) result = (result * base) % modulus;
     *              return result;
     *          }
     *      Dlaczego dla exponent rzędu miliardów ta wersja jest bezużyteczna, mimo że daje poprawny wynik?
     *   4. Dlaczego algorytm Luhna NIE wykrywa wszystkich pomyłek (np. nie zawsze złapie zamianę
     *      sąsiednich cyfr, jak 09 ↔ 90)?
     *   5. Co wypisze:  System.out.println(peselSex("00210104251"));  ?
     *   6. ZNAJDŹ BŁĄD: ktoś liczy cyfrę kontrolną NIP, dostaje sumę mod 11 równą 10 i wpisuje "10" jako
     *      dziesiątą (ostatnią) cyfrę numeru. Co jest nie tak z takim NIP-em?
     *   7. Dlaczego poprawna suma kontrolna numeru karty, PESEL czy NIP nie oznacza, że taki numer
     *      naprawdę istnieje (że jest przypisany do realnej karty, osoby czy firmy)?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** Uruchamia ćwiczenia: najpierw TWOJE rozwiązania (✘ dopóki nie uzupełnisz), potem wzorcowe (✔). */
    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: floorMod(-7, 3)", 2L, () -> exercise1(-7, 3));
        Check.equal("ćw. 1b: floorMod(7, 3)", 1L, () -> exercise1(7, 3));
        Check.equal("ćw. 1c: floorMod(-1, 10)", 9L, () -> exercise1(-1, 10));
        Check.equal("ćw. 1d: floorMod(0, 5)", 0L, () -> exercise1(0, 5));
        Check.equal("ćw. 2a: modPow(2, 10, 1000)", 24L, () -> exercise2(2, 10, 1000));
        Check.equal("ćw. 2b: modPow(5, 0, 7)", 1L, () -> exercise2(5, 0, 7));
        Check.equal("ćw. 2c: modPow(3, 13, 7)", 3L, () -> exercise2(3, 13, 7));
        Check.equal("ćw. 3a: cyfra kontrolna Luhna (15 cyfr)", 7, () -> exercise3("453914880343646"));
        Check.equal("ćw. 3b: cyfra kontrolna Luhna (10 cyfr)", 3, () -> exercise3("7992739871"));
        Check.equal("ćw. 3c: cyfra kontrolna Luhna (same zera)", 0, () -> exercise3("0000000000000"));
        Check.equal("ćw. 4a: NIP poprawny", true, () -> exercise4("1234567802"));
        Check.equal("ćw. 4b: NIP błędna cyfra kontrolna", false, () -> exercise4("1234567801"));
        Check.equal("ćw. 4c: NIP z nieużywalnym prefiksem", false, () -> exercise4("1234567891"));
        Check.equal("ćw. 5a: opis PESEL (mężczyzna)", "2000-01-01, mężczyzna", () -> exercise5("00210104251"));
        Check.equal("ćw. 5b: opis PESEL (kobieta)", "1985-06-15, kobieta", () -> exercise5("85061512347"));
        Check.equal("ćw. 5c: opis PESEL (zła suma kontrolna)", "SUMA KONTROLNA NIEPOPRAWNA", () -> exercise5("00210104252"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", 2L, () -> solution1(-7, 3));
        Check.equal("ćw. 1b (wzorzec)", 1L, () -> solution1(7, 3));
        Check.equal("ćw. 1c (wzorzec)", 9L, () -> solution1(-1, 10));
        Check.equal("ćw. 1d (wzorzec)", 0L, () -> solution1(0, 5));
        Check.equal("ćw. 2a (wzorzec)", 24L, () -> solution2(2, 10, 1000));
        Check.equal("ćw. 2b (wzorzec)", 1L, () -> solution2(5, 0, 7));
        Check.equal("ćw. 2c (wzorzec)", 3L, () -> solution2(3, 13, 7));
        Check.equal("ćw. 3a (wzorzec)", 7, () -> solution3("453914880343646"));
        Check.equal("ćw. 3b (wzorzec)", 3, () -> solution3("7992739871"));
        Check.equal("ćw. 3c (wzorzec)", 0, () -> solution3("0000000000000"));
        Check.equal("ćw. 4a (wzorzec)", true, () -> solution4("1234567802"));
        Check.equal("ćw. 4b (wzorzec)", false, () -> solution4("1234567801"));
        Check.equal("ćw. 4c (wzorzec)", false, () -> solution4("1234567891"));
        Check.equal("ćw. 5a (wzorzec)", "2000-01-01, mężczyzna", () -> solution5("00210104251"));
        Check.equal("ćw. 5b (wzorzec)", "1985-06-15, kobieta", () -> solution5("85061512347"));
        Check.equal("ćw. 5c (wzorzec)", "SUMA KONTROLNA NIEPOPRAWNA", () -> solution5("00210104252"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 16 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): policz resztę z dzielenia a przez b tak, jak Math.floorMod — bez wywoływania
     * Math.floorMod. Zakładamy b > 0.
     * Podpowiedź: policz zwykłe {@code a % b}; jeśli wynik jest ujemny, dodaj b.
     */
    static long exercise1(long a, long b) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 2 (łatwe): zaimplementuj potęgowanie modularne metodą square-and-multiply — nie
     * wywołuj modPow z sekcji 2, napisz własną pętlę.
     * Podpowiedź: przy każdym obrocie pętli podnieś podstawę do kwadratu (mod modulus); jeśli najmłodszy
     * bit wykładnika to 1, pomnóż wynik przez bieżącą podstawę (mod modulus).
     */
    static long exercise2(long base, long exponent, long modulus) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 3 (średnie): mając PIERWSZE n-1 cyfr numeru karty, policz BRAKUJĄCĄ (ostatnią) cyfrę
     * kontrolną Luhna, tak żeby cały numer (prefix + ta cyfra) był poprawny wg luhnValid.
     * Podpowiedź: licz jak w luhnValid, ale zaczynając podwajanie od OSTATNIEJ cyfry prefiksu (bo po
     * dopisaniu cyfry kontrolnej to ona będzie drugą cyfrą od prawa); cyfra = (10 - suma mod 10) mod 10.
     */
    static int exercise3(String prefixDigits) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 4 (średnie): sprawdź, czy 10-cyfrowy NIP ma poprawną cyfrę kontrolną — uwzględnij
     * przypadek, gdy prefiks jest nieużywalny (suma mod 11 == 10).
     * Podpowiedź: policz nipChecksum pierwszych 9 cyfr; jeśli to 10, zwróć false; w przeciwnym razie
     * porównaj z 10. cyfrą.
     */
    static boolean exercise4(String tenDigits) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): dla 11-cyfrowego PESEL zwróć opis "data urodzenia, płeć"
     * (np. "2000-01-01, mężczyzna"), a gdy suma kontrolna jest niepoprawna — napis
     * "SUMA KONTROLNA NIEPOPRAWNA" (bez próby odczytania daty).
     * Podpowiedź: najpierw peselChecksumValid; dopiero potem peselBirthDate + ", " + peselSex.
     */
    static String exercise5(String pesel) {
        // TODO: twoje rozwiązanie
        return "";
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static long solution1(long a, long b) {
        long r = a % b;
        return r < 0 ? r + b : r;
    }

    static long solution2(long base, long exponent, long modulus) {
        if (modulus == 1) return 0;
        long result = 1;
        long b = base % modulus;
        long e = exponent;
        while (e > 0) {
            if ((e & 1) == 1) result = (result * b) % modulus;
            e >>= 1;
            b = (b * b) % modulus;
        }
        return result;
    }

    static int solution3(String prefixDigits) {
        return luhnCheckDigit(prefixDigits);
    }

    static boolean solution4(String tenDigits) {
        return nipValid(tenDigits);
    }

    static String solution5(String pesel) {
        if (!peselChecksumValid(pesel)) return "SUMA KONTROLNA NIEPOPRAWNA";
        return peselBirthDate(pesel) + ", " + peselSex(pesel);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Cyfra kontrolna musi być liczbą 0-9 (albo wynikiem w ustalonym zakresie, jak 0..96 dla IBAN).
     *      Zwykłe % może dać wynik ujemny dla ujemnej dzielnej, co psuje porównanie z cyfrą 0-9 i indeksy
     *      tablic. Math.floorMod zawsze mieści się w 0..b-1 dla dodatniego b.
     *   2. "9" — floorMod(-1, 10) liczy w kierunku -∞: -1 = (-1)*10 + 9, więc reszta to 9.
     *   3. Pętla wykonuje DOKŁADNIE exponent obrotów — dla exponent = 2^31 to ponad 2 miliardy mnożeń,
     *      podczas gdy square-and-multiply potrzebuje ok. 31 (log2 z 2^31). Dla kryptografii (wykładniki
     *      rzędu 2^16 czy większe) ta różnica to sekundy kontra ułamki milisekundy.
     *   4. Luhn sumuje cyfry niezależnie od ich KOLEJNOŚCI w parze — zamiana miejscami dwóch cyfr o sumie
     *      cyfr dającej ten sam wynik (np. 09 i 90: 0+9=9 i 9+0=9 po ewentualnym podwojeniu i redukcji)
     *      może dać tę samą sumę kontrolną mimo błędu. Luhn wykrywa pojedyncze złe cyfry i WIĘKSZOŚĆ
     *      zamian sąsiednich cyfr, ale nie wszystkie.
     *   5. "mężczyzna" — 10. cyfra PESEL "00210104251" to '5' (nieparzysta).
     *   6. Cyfra kontrolna musi być JEDNĄ cyfrą (0-9). Gdy suma mod 11 wychodzi 10, nie da się tego
     *      zapisać jedną cyfrą — taki prefiks (pierwsze 9 cyfr) jest w ogóle NIEUŻYWALNY jako NIP, nie
     *      wolno "obciąć" do zera ani wpisać dwucyfrowej wartości. Trzeba zmienić numer firmy.
     *   7. Suma kontrolna to czysta ARYTMETYKA na cyfrach — sprawdza tylko, czy numer ma poprawny WZÓR.
     *      Nie wie nic o tym, czy taka karta została kiedykolwiek wydana, czy taki PESEL nadano realnej
     *      osobie, czy taki NIP zarejestrowano w urzędzie. To sprawdza dopiero zapytanie do prawdziwej
     *      bazy danych (banku, rejestru PESEL, systemu podatkowego).
     */
    // </editor-fold>
}
