package t01_basics;

import helpers.Check;

import java.util.ArrayList;
import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Typy proste (prymitywne) — osiem „cegiełek” danych w Javie
 *        (primitive type = typ prosty, prymitywny; literal = literał, wartość wpisana wprost w kodzie)
 *
 * W SKRÓCIE:
 *   Java ma 8 typów prostych: byte, short, int, long (liczby całkowite), float, double (liczby z ułamkiem),
 *   char (znak) i boolean (prawda/fałsz). Każdy ma stały rozmiar i zakres. Zmienna typu prostego przechowuje
 *   SAMĄ WARTOŚĆ — nie obiekt. Na co dzień używasz głównie: int, long, double, boolean, char.
 *
 * ANALOGIA: pojemniki w kuchni.
 *   byte to kieliszek, short szklanka, int dzbanek, long wiadro. Im większy pojemnik, tym więcej się zmieści,
 *   ale też więcej miejsca zajmuje. Jeśli do kieliszka wlejesz wiadro wody — przeleje się (przepełnienie, Basics05Casting).
 *
 * JAK TO DZIAŁA:
 *   Typ       Bity  Zakres (w przybliżeniu)                 Przykład literału
 *   byte        8   -128 … 127                               (byte) 10
 *   short      16   -32 768 … 32 767                         (short) 1000
 *   int        32   ok. ±2,1 miliarda                        42, 1_000_000
 *   long       64   ok. ±9,2 × 10^18                         10_000_000_000L   ← L na końcu!
 *   float      32   ok. 7 cyfr znaczących                    3.14f             ← f na końcu!
 *   double     64   ok. 15–16 cyfr znaczących                3.14, 1e3
 *   char       16   0 … 65 535 (jeden znak UTF-16)           'A', '\u0041'
 *   boolean     —   true / false                             true
 *
 * SŁÓWKA:
 *   primitive = prosty, prymitywny; integer = liczba całkowita; floating point = zmiennoprzecinkowy;
 *   character (char) = znak; boolean = logiczny; MIN_VALUE / MAX_VALUE = najmniejsza / największa wartość;
 *   SIZE = rozmiar (w bitach); default value = wartość domyślna; literal = literał; hex = szesnastkowy;
 *   binary = dwójkowy; octal = ósemkowy; precision = precyzja.
 *
 * ZOBACZ TEŻ: t01_basics/Basics03Variables (zmienne), t01_basics/Basics05Casting (zamiana typów, przepełnienie),
 *             t01_basics/Basics06Wrappers (Integer, Double... — obiektowe wersje typów prostych),
 *             t01_basics/Basics08FloatingPoint (pułapki double), t15_numbers/Numbers01BigDecimal (dokładne kwoty).
 * </pre>
 */
public class Basics02PrimitiveTypes {

    public static void main(String[] args) {
        title("Basics02 — typy proste");

        ranges();               // ranges = zakresy
        whichTypeToChoose();    // which type to choose = który typ wybrać
        integerLiterals();      // integer literals = literały całkowite
        charIsANumber();        // char is a number = char to liczba
        floatVsDouble();        // float vs double = float kontra double
        defaultValues();        // default values = wartości domyślne
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. ZAKRESY TYPÓW
    // =================================================================================================

    /**
     * 1. Każdy typ prosty ma stałe MIN_VALUE i MAX_VALUE w swojej „klasie opakowującej” (Byte, Short, Integer, Long...).
     * Nie ucz się zakresów na pamięć — wystarczy wiedzieć, gdzie je sprawdzić.
     */
    static void ranges() {
        section("1. Zakresy typów całkowitych");

        show("byte ", Byte.MIN_VALUE + " … " + Byte.MAX_VALUE);
        show("short", Short.MIN_VALUE + " … " + Short.MAX_VALUE);
        show("int  ", Integer.MIN_VALUE + " … " + Integer.MAX_VALUE);
        show("long ", Long.MIN_VALUE + " … " + Long.MAX_VALUE);
        // WYNIK: byte  → -128 … 127
        // WYNIK: short → -32768 … 32767
        // WYNIK: int   → -2147483648 … 2147483647
        // WYNIK: long  → -9223372036854775808 … 9223372036854775807

        // SIZE = liczba bitów. Bit to najmniejsza porcja informacji (0 albo 1); 8 bitów = 1 bajt.
        show("bity int / long", Integer.SIZE + " / " + Long.SIZE);
        // WYNIK: bity int / long → 32 / 64

        // Dlaczego ujemnych jest o jeden WIĘCEJ (-128 … 127)? Bo zero zajmuje jedno miejsce po „dodatniej” stronie.
    }

    // =================================================================================================
    // 2. KTÓRY TYP WYBRAĆ
    // =================================================================================================

    /**
     * 2. W praktyce wybór jest prosty:
     * <pre>
     *   int      — domyślny typ dla liczb całkowitych (wiek, liczba sztuk, indeks)
     *   long     — gdy liczby mogą przekroczyć ~2 miliardy (populacja, czas w milisekundach, kwota w groszach)
     *   double   — liczby z ułamkiem w obliczeniach (średnia, fizyka) — ale NIE pieniądze (t15_numbers)
     *   boolean  — tak/nie (isActive, hasDiscount)
     *   char     — pojedynczy znak (rzadko; do tekstu używamy String)
     *   byte/short/float — tylko w specjalnych przypadkach (duże tablice danych, pliki binarne, grafika)
     * </pre>
     */
    static void whichTypeToChoose() {
        section("2. Który typ wybrać");

        int quantity = 25;                          // quantity = ilość
        long worldPopulation = 8_100_000_000L;      // world population = populacja świata — NIE zmieści się w int!
        double averageGrade = 4.25;                 // average grade = średnia ocen
        boolean inStock = true;                     // in stock = na stanie
        char grade = 'A';                           // grade = ocena (litera)
        show("int", quantity);
        show("long", worldPopulation);
        show("double", averageGrade);
        show("boolean", inStock);
        show("char", grade);
        // WYNIK: int → 25
        // WYNIK: long → 8100000000
        // WYNIK: double → 4.25
        // WYNIK: boolean → true
        // WYNIK: char → A

        // DOBRA PRAKTYKA: nie oszczędzaj pamięci na siłę (byte zamiast int) — różnica jest znikoma,
        //   a ryzyko przepełnienia duże. Domyślnie: int, long, double, boolean.
    }

    // =================================================================================================
    // 3. LITERAŁY LICZB CAŁKOWITYCH
    // =================================================================================================

    /**
     * 3. Literał to wartość wpisana wprost w kodzie. Liczba bez przyrostka to int; z L na końcu — long.
     * Można pisać w systemie dwójkowym (0b), szesnastkowym (0x) i — uwaga — ósemkowym (0 na początku!).
     */
    static void integerLiterals() {
        section("3. Literały: _ w liczbach, L, 0x, 0b, 0 na początku");

        int million = 1_000_000;                    // podkreślniki tylko dla czytelności (Java 7+)
        long big = 3_000_000_000L;                  // L = long; bez L: błąd „integer number too large”
        show("1_000_000", million);
        show("3_000_000_000L", big);
        // WYNIK: 1_000_000 → 1000000
        // WYNIK: 3_000_000_000L → 3000000000

        show("0x1F (szesnastkowo)", 0x1F);          // 1×16 + 15 = 31
        show("0b1010 (dwójkowo)", 0b1010);          // 8 + 2 = 10
        // WYNIK: 0x1F (szesnastkowo) → 31
        // WYNIK: 0b1010 (dwójkowo) → 10

        // PUŁAPKA: zero na początku liczby oznacza system ÓSEMKOWY! 017 to NIE siedemnaście.
        show("017 (ósemkowo!)", 017);               // 1×8 + 7 = 15
        // WYNIK: 017 (ósemkowo!) → 15
        //   Nigdy nie dopisuj zer „dla wyrównania” (np. kod pocztowy 00950 jako liczba) — trzymaj takie dane w String.

        // DOBRA PRAKTYKA: przyrostek L pisz WIELKĄ literą — małe l łatwo pomylić z cyfrą 1 (10l wygląda jak 101).
    }

    // =================================================================================================
    // 4. CHAR TO LICZBA
    // =================================================================================================

    /**
     * 4. char przechowuje znak jako liczbę (kod UTF-16). Dlatego da się do niego dodawać — ale wynik działania
     * to int, więc żeby dostać znak, trzeba rzutować z powrotem na char (rzutowanie: Basics05Casting).
     */
    static void charIsANumber() {
        section("4. char to liczba (kod znaku)");

        char letter = 'A';
        show("'A' jako liczba", (int) letter);            // (int) = rzutowanie na int
        show("'A' + 1", letter + 1);                      // char + int → int
        show("(char) ('A' + 1)", (char) (letter + 1));    // z powrotem na znak
        show("'\\u0041' (zapis Unicode)", '\u0041');
        // WYNIK: 'A' jako liczba → 65
        // WYNIK: 'A' + 1 → 66
        // WYNIK: (char) ('A' + 1) → B
        // WYNIK: '\u0041' (zapis Unicode) → A

        // PUŁAPKA: 'A' (apostrofy) to char, "A" (cudzysłów) to String — to zupełnie różne typy.
        //   char c = "A";  → błąd kompilacji (incompatible types = niezgodne typy).
    }

    // =================================================================================================
    // 5. FLOAT KONTRA DOUBLE
    // =================================================================================================

    /**
     * 5. float i double przechowują liczby z ułamkiem w PRZYBLIŻENIU (system dwójkowy nie zapisze dokładnie np. 0,1).
     * float ma ok. 7 cyfr znaczących, double ok. 15–16. Literał z kropką (3.14) to double; float wymaga f (3.14f).
     */
    static void floatVsDouble() {
        section("5. float kontra double — precyzja");

        show("1.0f / 3 (float)", 1.0f / 3);
        show("1.0 / 3 (double)", 1.0 / 3);
        // WYNIK: 1.0f / 3 (float) → 0.33333334
        // WYNIK: 1.0 / 3 (double) → 0.3333333333333333

        show("0.1 + 0.2 (double)", 0.1 + 0.2);
        // WYNIK: 0.1 + 0.2 (double) → 0.30000000000000004    ← przybliżenie, nie błąd Javy (tak działa każdy język)

        show("1e3 (notacja naukowa)", 1e3);                // 1 × 10^3
        // WYNIK: 1e3 (notacja naukowa) → 1000.0

        // PUŁAPKA: float f = 3.14;  → błąd kompilacji („possible lossy conversion from double to float”
        //   = możliwa utrata danych) — trzeba napisać 3.14f.
        // DOBRA PRAKTYKA: do liczb z ułamkiem używaj double; do pieniędzy — BigDecimal (t15_numbers/Numbers01BigDecimal).
    }

    // =================================================================================================
    // 6. WARTOŚCI DOMYŚLNE
    // =================================================================================================

    /**
     * Defaults = wartości domyślne. Pola klasy (nie zmienne lokalne!) dostają automatycznie wartości domyślne.
     * To klasa zagnieżdżona tylko do tej demonstracji (klasy zagnieżdżone: t06_oop_basics/Oop07NestedClasses).
     */
    static class Defaults {
        static int anInt;          // 0
        static long aLong;         // 0
        static double aDouble;     // 0.0
        static boolean aBoolean;   // false
        static char aChar;         // znak o kodzie 0 (niewidoczny)
    }

    /**
     * 6. Pola klas mają wartości domyślne (0, 0.0, false, znak o kodzie 0). Zmienne LOKALNE (w metodzie) — NIE:
     * trzeba im nadać wartość przed pierwszym użyciem, inaczej kompilator zgłosi błąd.
     */
    static void defaultValues() {
        section("6. Wartości domyślne pól");

        show("int", Defaults.anInt);
        show("long", Defaults.aLong);
        show("double", Defaults.aDouble);
        show("boolean", Defaults.aBoolean);
        show("char (jako liczba)", (int) Defaults.aChar);
        // WYNIK: int → 0
        // WYNIK: long → 0
        // WYNIK: double → 0.0
        // WYNIK: boolean → false
        // WYNIK: char (jako liczba) → 0

        // PUŁAPKA: zmienna lokalna bez wartości:
        //   int count;
        //   System.out.println(count);  → błąd kompilacji „variable count might not have been initialized”
        //   (zmienna mogła nie zostać zainicjalizowana). Dobrze: int count = 0;
        // Ciekawostka: boolean nie jest liczbą — w Javie NIE ma zamiany 0/1 ↔ false/true (w C/C++ jest).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Całkowite: byte (8 b), short (16), int (32, domyślny), long (64, przyrostek L).
     *   • Z ułamkiem: float (32, przyrostek f, ~7 cyfr), double (64, domyślny, ~15–16 cyfr) — oba przybliżone.
     *   • char (16 bitów, kod znaku, 'A'), boolean (true/false, bez zamiany na 0/1).
     *   • Zakresy: Integer.MIN_VALUE / MAX_VALUE itd.; rozmiar: Integer.SIZE.
     *   • Literały: 1_000_000, 10L, 3.14f, 1e3, 0x1F, 0b1010; 017 = ÓSEMKOWO 15!
     *   • 'A' + 1 = 66 (int); (char) ('A' + 1) = 'B'.
     *   • Pola mają wartości domyślne; zmienne lokalne trzeba zainicjalizować.
     *   • Pieniądze: nie double, tylko BigDecimal (albo long w groszach).
     *
     * PYTANIA KONTROLNE:
     *   1. Który typ wybierzesz na liczbę sztuk w magazynie, a który na liczbę mieszkańców Ziemi? Dlaczego?
     *   2. Co wypisze:  System.out.println(010 + 1);  ?
     *   3. Co wypisze:  System.out.println('A' + 'B');  a co  System.out.println("" + 'A' + 'B');  ?
     *   4. ZNAJDŹ BŁĄD:  long distance = 5_000_000_000;   float price = 9.99;
     *   5. Dlaczego 0.1 + 0.2 nie daje dokładnie 0.3?
     *   6. Która zmienna ma wartość domyślną: pole klasy  static int total;  czy zmienna lokalna  int total;  w metodzie?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        List<String> expected4 = List.of("byte", "short", "int", "long", "short");
        List<Long> values4 = List.of(100L, 1000L, 100_000L, 10_000_000_000L, -129L);

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: Integer.MAX_VALUE + 1 bez przepełnienia", 2147483648L, () -> exercise1());
        Check.equal("ćw. 2: następna litera po 'A'", 'B', () -> exercise2('A'));
        Check.equal("ćw. 2b: następna litera po 'y'", 'z', () -> exercise2('y'));
        Check.equal("ćw. 3: sekundy w 100 latach (365-dniowych)", 3_153_600_000L, () -> exercise3(100));
        Check.equal("ćw. 4: najmniejszy pasujący typ", expected4, () -> exercise4(values4));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 2147483648L, () -> solution1());
        Check.equal("ćw. 2 (wzorzec)", 'B', () -> solution2('A'));
        Check.equal("ćw. 2b (wzorzec)", 'z', () -> solution2('y'));
        Check.equal("ćw. 3 (wzorzec)", 3_153_600_000L, () -> solution3(100));
        Check.equal("ćw. 4 (wzorzec)", expected4, () -> solution4(values4));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć wartość Integer.MAX_VALUE powiększoną o 1 — POPRAWNIE (2147483648), bez przepełnienia.
     * Podpowiedź: jeśli dodasz 1 jako int, wynik „przekręci się” na liczbę ujemną. Dodaj 1L (long).
     */
    static long exercise1() {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć następną literę alfabetu (łacińskiego) po podanej: 'A' → 'B', 'y' → 'z'.
     * Podpowiedź: c + 1 daje int — rzutuj wynik na char: {@code (char) (c + 1)}.
     */
    static char exercise2(char c) {
        // TODO: twoje rozwiązanie
        return ' ';
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć liczbę sekund w podanej liczbie lat (przyjmij rok = 365 dni).
     * Dla 100 lat to 3 153 600 000 — WIĘCEJ niż zmieści int!
     * Podpowiedź: licz od początku na long: {@code years * 365L * 24 * 60 * 60}. Pomnożenie samych intów przepełni się,
     * zanim wynik trafi do long (dokładnie o tym: t01_basics/Basics05Casting).
     */
    static long exercise3(int years) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): dla każdej liczby zwróć nazwę NAJMNIEJSZEGO typu całkowitego, w którym się zmieści:
     * "byte", "short", "int" albo "long". Np. 100 → "byte", 1000 → "short", -129 → "short".
     * Podpowiedź: porównuj z Byte.MIN_VALUE/Byte.MAX_VALUE, potem Short..., potem Integer...; wyniki dodawaj do
     * {@code new ArrayList<String>()} w pętli for-each.
     */
    static List<String> exercise4(List<Long> values) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static long solution1() {
        return Integer.MAX_VALUE + 1L;          // int + long → long (bez przepełnienia)
    }

    static char solution2(char c) {
        return (char) (c + 1);
    }

    static long solution3(int years) {
        return years * 365L * 24 * 60 * 60;     // 365L sprawia, że całe mnożenie liczy się na long
    }

    static List<String> solution4(List<Long> values) {
        List<String> result = new ArrayList<>();
        for (long v : values) {
            if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
                result.add("byte");
            } else if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
                result.add("short");
            } else if (v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE) {
                result.add("int");
            } else {
                result.add("long");
            }
        }
        return result;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Liczba sztuk — int (małe liczby, domyślny typ). Mieszkańcy Ziemi — long (ok. 8 miliardów > 2,1 miliarda).
     *   2. 9 — 010 to ósemkowo 8, plus 1.
     *   3. 131 (kody znaków 65 + 66, bo char + char to int) oraz „AB” (łączenie z napisem "" daje napis).
     *   4. 5_000_000_000 nie mieści się w int — trzeba 5_000_000_000L; 9.99 to double — trzeba 9.99f
     *      (a do cen i tak lepiej BigDecimal).
     *   5. Bo 0,1 i 0,2 nie mają dokładnego zapisu w systemie dwójkowym — double przechowuje ich przybliżenia.
     *   6. Tylko pole klasy (static int total → 0). Zmiennej lokalnej trzeba nadać wartość przed użyciem.
     */
    // </editor-fold>
}
