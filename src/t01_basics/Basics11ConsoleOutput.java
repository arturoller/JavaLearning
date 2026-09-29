package t01_basics;

import helpers.Check;

import java.util.Locale;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Wypisywanie na konsolę — print, println, printf, formatowanie, znaki specjalne
 *        (output = wyjście; print = wypisz; format = sformatuj; escape sequence = sekwencja specjalna)
 *
 * W SKRÓCIE:
 *   println wypisuje linię, print — bez przejścia do nowej, printf/String.format — według szablonu
 *   (%s napis, %d liczba całkowita, %.2f liczba z 2 miejscami po przecinku). Szablony pozwalają wyrównywać kolumny
 *   (%-10s, %5d). Znaki specjalne zapisuje się z ukośnikiem: \n (nowa linia), \" (cudzysłów), \\ (ukośnik).
 *
 * ANALOGIA: formularz z rubrykami.
 *   Szablon "%-10s %5d" to formularz: rubryka na 10 znaków wyrównana do lewej i rubryka na 5 znaków do prawej.
 *   printf wpisuje wartości w rubryki — dzięki temu tabela jest równa, niezależnie od długości danych.
 *
 * JAK TO DZIAŁA:
 *   %s — napis (dowolny obiekt przez toString)      %d — liczba całkowita          %f / %.2f — liczba z ułamkiem
 *   %n — nowa linia (właściwa dla systemu)           %% — znak procentu             %b — boolean, %c — znak, %x — szesnastkowo
 *   %-10s — 10 znaków, do lewej                      %5d — 5 znaków, do prawej      %05d — dopełnij zerami
 *   String.format(Locale.ROOT, "%.2f", x) — z KROPKĄ;  z Locale pl-PL — z PRZECINKIEM
 *
 * SŁÓWKA:
 *   print = wypisz; println (print line) = wypisz linię; printf (print formatted) = wypisz sformatowane;
 *   format = format, szablon; placeholder = symbol zastępczy (%s, %d); width = szerokość; align = wyrównać;
 *   padding = dopełnienie; escape = „ucieczka” (znak specjalny); text block = blok tekstu; standard error = wyjście błędów.
 *
 * ZOBACZ TEŻ: t01_basics/Basics01HelloJvm (pierwszy println), t04_strings/Strings04Formatting (formatowanie dokładnie),
 *             t15_numbers/Numbers04FormattingParsing (polskie formaty liczb i kwot).
 * </pre>
 */
public class Basics11ConsoleOutput {

    /** POLISH = polskie ustawienia regionalne. */
    private static final Locale POLISH = Locale.forLanguageTag("pl-PL");

    public static void main(String[] args) {
        title("Basics11 — wypisywanie na konsolę");

        printVsPrintln();       // print vs println = print kontra println
        escapeSequences();      // escape sequences = znaki specjalne
        printfBasics();         // printf basics = podstawy printf
        widthAndAlignment();    // width and alignment = szerokość i wyrównanie
        otherSpecifiers();      // other specifiers = inne symbole formatu
        textBlocks();           // text blocks = bloki tekstu
        standardError();        // standard error = wyjście błędów
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. print KONTRA println
    // =================================================================================================

    /** 1. println kończy linię, print nie. Puste println() wypisuje pustą linię. */
    static void printVsPrintln() {
        section("1. print kontra println");

        System.out.print("Raz, ");
        System.out.print("dwa, ");
        System.out.println("trzy!");
        // WYNIK: Raz, dwa, trzy!

        System.out.println(42);                  // println przyjmuje też liczby, znaki, obiekty...
        System.out.println(true);
        // WYNIK: 42
        // WYNIK: true
    }

    // =================================================================================================
    // 2. ZNAKI SPECJALNE
    // =================================================================================================

    /** 2. Znaki, których nie da się wpisać wprost do napisu, zapisuje się z ukośnikiem (backslash). */
    static void escapeSequences() {
        section("2. Znaki specjalne: \\n, \\t, \\\", \\\\");

        System.out.println("Pierwsza linia\nDruga linia");          // \n = nowa linia
        // WYNIK: Pierwsza linia
        // WYNIK: Druga linia

        System.out.println("Powiedział: \"Cześć!\"");               // \" = cudzysłów w środku napisu
        // WYNIK: Powiedział: "Cześć!"

        System.out.println("Ścieżka: C:\\Users\\Ala");                // \\ = jeden ukośnik
        // WYNIK: Ścieżka: C:\Users\Ala

        System.out.println("Imię:\tAla");                              // \t = tabulator
        // (tabulator: w konsoli wygląda jak kilka spacji; jego szerokość zależy od programu, który go wyświetla)

        // PUŁAPKA: ścieżka "C:\nowy" to NIE katalog „nowy” — \n zamieni się w nową linię! Pisz "C:\\nowy"
        //   albo używaj ukośników w przód: "C:/nowy" (Java je rozumie także na Windows).
    }

    // =================================================================================================
    // 3. PRINTF I STRING.FORMAT
    // =================================================================================================

    /**
     * 3. printf wypisuje według szablonu; String.format zwraca gotowy napis (nie wypisuje). formatted (Java 15+)
     * to to samo co String.format, tylko wywołane na szablonie. Liczby z ułamkiem formatuj z JAWNYM Locale.
     */
    static void printfBasics() {
        section("3. printf, String.format, formatted");

        System.out.printf("%s ma %d lat%n", "Ala", 30);            // %n = nowa linia
        // WYNIK: Ala ma 30 lat

        String line = String.format(Locale.ROOT, "Cena: %.2f zł", 19.5);
        show("String.format", line);
        // WYNIK: String.format → Cena: 19.50 zł

        show("formatted (Java 15+)", "%s: %d szt.".formatted("Kawa", 3));
        // WYNIK: formatted (Java 15+) → Kawa: 3 szt.

        show("%.2f z Locale.ROOT", String.format(Locale.ROOT, "%.2f", 3.14159));
        show("%.2f po polsku", String.format(POLISH, "%.2f", 3.14159));
        // WYNIK: %.2f z Locale.ROOT → 3.14
        // WYNIK: %.2f po polsku → 3,14

        // PUŁAPKA: String.format("%.2f", x) BEZ Locale używa ustawień komputera — na polskim Windows da „3,14”,
        //   na angielskim „3.14”. Gdy wynik trafia do pliku, CSV albo innego programu — podaj Locale jawnie.
        // PUŁAPKA: zła liczba argumentów albo zły typ (%d dla napisu) → wyjątek w czasie działania:
        expectThrows("String.format(\"%d\", \"tekst\")", () -> String.format("%d", "tekst"));
        // WYNIK: ✔ String.format("%d", "tekst") → rzucono IllegalFormatConversionException: d != java.lang.String
    }

    // =================================================================================================
    // 4. SZEROKOŚĆ I WYRÓWNANIE
    // =================================================================================================

    /** 4. Liczba między % a literą to szerokość pola. Minus = wyrównaj do lewej. Dzięki temu kolumny są równe. */
    static void widthAndAlignment() {
        section("4. Szerokość i wyrównanie kolumn");

        System.out.printf(Locale.ROOT, "|%-15s|%5s|%10s|%n", "Produkt", "Szt.", "Cena");
        System.out.printf(Locale.ROOT, "|%-15s|%5d|%10.2f|%n", "Kawa", 3, 64.99);
        System.out.printf(Locale.ROOT, "|%-15s|%5d|%10.2f|%n", "Laptop Pro 14", 1, 5499.99);
        // WYNIK: |Produkt        | Szt.|      Cena|
        // WYNIK: |Kawa           |    3|     64.99|
        // WYNIK: |Laptop Pro 14  |    1|   5499.99|

        show("%05d (zera z przodu)", String.format("%05d", 42));
        show("%,d (grupowanie, ROOT)", String.format(Locale.ROOT, "%,d", 1234567));
        // WYNIK: %05d (zera z przodu) → 00042
        // WYNIK: %,d (grupowanie, ROOT) → 1,234,567
        // Po polsku grupy oddziela spacja niełamliwa (niewidoczny, specjalny znak) — szczegóły i pułapki:
        //   t15_numbers/Numbers04FormattingParsing.
    }

    // =================================================================================================
    // 5. INNE SYMBOLE FORMATU
    // =================================================================================================

    /** 5. %b (boolean), %c (znak), %x (szesnastkowo), %% (sam znak procentu). */
    static void otherSpecifiers() {
        section("5. %b, %c, %x, %%");

        show("%b", String.format("%b", 5 > 3));
        show("%c", String.format("%c", 'Z'));
        show("%x (255 szesnastkowo)", String.format("%x", 255));
        show("%d%% (procent)", String.format("%d%%", 75));
        // WYNIK: %b → true
        // WYNIK: %c → Z
        // WYNIK: %x (255 szesnastkowo) → ff
        // WYNIK: %d%% (procent) → 75%
    }

    // =================================================================================================
    // 6. BLOKI TEKSTU (Java 15+)
    // =================================================================================================

    /**
     * 6. Blok tekstu zaczyna się od trzech cudzysłowów i nowej linii, kończy trzema cudzysłowami. Zachowuje podział
     * na linie, a wspólne wcięcie (wynikające z formatowania kodu) jest automatycznie usuwane.
     */
    static void textBlocks() {
        section("6. Bloki tekstu (Java 15+)");

        String receipt = """
                PARAGON
                Kawa ziarnista   64.99
                Czekolada         7.49
                RAZEM            72.48""";
        System.out.println(receipt);
        // WYNIK: PARAGON
        // WYNIK: Kawa ziarnista   64.99
        // WYNIK: Czekolada         7.49
        // WYNIK: RAZEM            72.48

        // DOBRA PRAKTYKA: bloki tekstu świetnie nadają się do wielolinijkowych szablonów (JSON, SQL, raporty) —
        //   nie trzeba sklejać linii plusami ani wstawiać \n. Można je łączyć z formatted(...).
    }

    // =================================================================================================
    // 7. WYJŚCIE BŁĘDÓW — System.err
    // =================================================================================================

    /**
     * 7. System.out to „normalne” wyjście, System.err — wyjście błędów. W IntelliJ tekst z System.err jest czerwony.
     * Oba strumienie są niezależne, więc ich linie mogą się w konsoli wymieszać w nieoczekiwanej kolejności.
     */
    static void standardError() {
        section("7. System.err — wyjście błędów");

        // System.err.println("Nie znaleziono pliku!");   ← tak wypisuje się komunikat o błędzie (w IntelliJ na czerwono)
        // W tym kursie nie wypisujemy na System.err, bo automatyczny sprawdzian traktuje każdy tekst na wyjściu błędów
        //   jako błąd programu. W prawdziwych aplikacjach zamiast System.out/err używa się loggera (t18_io_files/Io10SimpleLogger).
        note("System.err.println(...) wypisuje na czerwono w IntelliJ — do komunikatów o błędach");
        // WYNIK: ℹ System.err.println(...) wypisuje na czerwono w IntelliJ — do komunikatów o błędach
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • println — z nową linią; print — bez; printf — według szablonu (koniec linii: %n).
     *   • String.format / "...".formatted(...) (Java 15+) — zwracają napis zamiast wypisywać.
     *   • %s napis, %d liczba całkowita, %.2f dwa miejsca po przecinku, %b, %c, %x, %% (znak %).
     *   • %-10s (do lewej), %5d (do prawej), %05d (zera), %,d (grupowanie).
     *   • Liczby z ułamkiem: podawaj Locale (ROOT → kropka, pl-PL → przecinek).
     *   • \n nowa linia, \t tabulator, \" cudzysłów, \\ ukośnik; ścieżki: "C:\\dir" albo "C:/dir".
     *   • Bloki tekstu """ ... """ (Java 15+) do tekstów wielolinijkowych.
     *
     * PYTANIA KONTROLNE:
     *   1. Co wypisze:  System.out.print("A"); System.out.println(); System.out.print("B");  ?
     *   2. Co wypisze:  System.out.printf(Locale.ROOT, "[%6.1f]%n", 3.14159);  ?
     *   3. ZNAJDŹ BŁĄD:  String path = "C:\temp\nowy.txt";
     *   4. Co wypisze:  System.out.println(String.format("%-5s|", "ab"));  ?
     *   5. Dlaczego String.format("%.2f", 2.5) daje różny wynik na różnych komputerach?
     *   6. Jak wypisać tekst: 50% rabatu  przy użyciu printf?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: cena 19.5 → \"19.50 zł\"", "19.50 zł", () -> exercise1(19.5));
        Check.equal("ćw. 2: wiersz tabeli", "Kawa      |   3|   64.99", () -> exercise2("Kawa", 3, 64.99));
        Check.equal("ćw. 3: pasek postępu 3/10", "[###-------] 30%", () -> exercise3(3, 10));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "19.50 zł", () -> solution1(19.5));
        Check.equal("ćw. 2 (wzorzec)", "Kawa      |   3|   64.99", () -> solution2("Kawa", 3, 64.99));
        Check.equal("ćw. 3 (wzorzec)", "[###-------] 30%", () -> solution3(3, 10));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 3 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć cenę jako napis z dwoma miejscami po KROPCE i dopiskiem „ zł”: 19.5 → "19.50 zł".
     * Podpowiedź: {@code String.format(Locale.ROOT, "%.2f zł", price)}.
     */
    static String exercise1(double price) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć wiersz tabeli: nazwa na 10 znaków do lewej, pionowa kreska, ilość na 4 znaki,
     * pionowa kreska, cena na 8 znaków z 2 miejscami po kropce. Dla ("Kawa", 3, 64.99): "Kawa      |   3|   64.99".
     * Podpowiedź: szablon {@code "%-10s|%4d|%8.2f"} z Locale.ROOT.
     */
    static String exercise2(String name, int quantity, double price) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): zwróć pasek postępu: done z total kroków. Dla (3, 10): "[###-------] 30%".
     * Znaków '#' tyle, ile zrobionych kroków, '-' — reszta; na końcu procent (liczba całkowita).
     * Podpowiedź: {@code "#".repeat(done)} (Java 11+), {@code "-".repeat(total - done)}, procent: {@code done * 100 / total},
     * a znak % w szablonie formatu to {@code %%}.
     */
    static String exercise3(int done, int total) {
        // TODO: twoje rozwiązanie
        return "";
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(double price) {
        return String.format(Locale.ROOT, "%.2f zł", price);
    }

    static String solution2(String name, int quantity, double price) {
        return String.format(Locale.ROOT, "%-10s|%4d|%8.2f", name, quantity, price);
    }

    static String solution3(int done, int total) {
        String bar = "#".repeat(done) + "-".repeat(total - done);
        return String.format("[%s] %d%%", bar, done * 100 / total);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. „A” w pierwszej linii, „B” na początku drugiej (println() bez argumentu kończy linię).
     *   2. [   3.1] — pole na 6 znaków, jedno miejsce po kropce, wyrównanie do prawej.
     *   3. \t i \n w napisie to tabulator i nowa linia, a nie litery katalogu. Poprawnie: "C:\\temp\\nowy.txt"
     *      albo "C:/temp/nowy.txt".
     *   4. „ab   |” — napis dopełniony spacjami do 5 znaków, wyrównany do lewej.
     *   5. Bez podanego Locale używane są ustawienia komputera: przecinek (Polska) albo kropka (np. USA).
     *   6. System.out.printf("%d%% rabatu%n", 50);  — %% to sam znak procentu.
     */
    // </editor-fold>
}
