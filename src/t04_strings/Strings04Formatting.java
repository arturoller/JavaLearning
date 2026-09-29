package t04_strings;

import helpers.Check;

import java.util.Locale;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Formatowanie napisów — String.format, formatted, flagi, Locale, bloki tekstu
 *        (format = formatować, szablon; specifier = specyfikator, symbol formatu; flag = flaga; text block = blok tekstu)
 *
 * W SKRÓCIE:
 *   Szablon formatu to tekst z „dziurami” (%s, %d, %.2f), w które wstawiane są wartości. Między % a literą można
 *   dodać flagi (-, 0, +, przecinek), szerokość i precyzję. Argumenty można numerować (%1$s, %2$s). Liczby formatuj
 *   z jawnym Locale, a teksty wielolinijkowe pisz jako bloki tekstu (""" ... """).
 *
 * ANALOGIA: formularz urzędowy.
 *   Szablon to wydrukowany formularz z rubrykami („Imię: ____  Wiek: __”). format wpisuje wartości w rubryki.
 *   Flagi i szerokość to wielkość i wyrównanie rubryk; numerowanie argumentów — „wpisz nazwisko z pola nr 2”.
 *
 * JAK TO DZIAŁA:
 *   %[numer$][flagi][szerokość][.precyzja]litera
 *   %s  napis        %d  liczba całkowita    %f  liczba z ułamkiem    %x  szesnastkowo   %b  boolean   %%  znak %
 *   %1$s  pierwszy argument     %-8s  8 znaków do lewej     %08.2f  8 znaków, zera z przodu, 2 miejsca po kropce
 *   %+d  zawsze ze znakiem (+5)  %,d  grupowanie tysięcy    %.3s  napis ucięty do 3 znaków
 *
 * SŁÓWKA:
 *   format = formatuj; specifier = specyfikator (np. %d); width = szerokość; precision = precyzja; flag = flaga;
 *   argument index = numer argumentu; locale = ustawienia regionalne; grouping = grupowanie (tysięcy);
 *   text block = blok tekstu; indent = wcięcie; missing = brakujący.
 *
 * ZOBACZ TEŻ: t01_basics/Basics11ConsoleOutput (printf od podstaw), t15_numbers/Numbers04FormattingParsing
 *             (NumberFormat, kwoty po polsku), t17_datetime/DateTime03Formatting (formatowanie dat).
 * </pre>
 */
public class Strings04Formatting {

    /** POLISH = polskie ustawienia regionalne. */
    private static final Locale POLISH = Locale.forLanguageTag("pl-PL");

    public static void main(String[] args) {
        title("Strings04 — formatowanie napisów");

        argumentIndex();        // argument index = numerowanie argumentów
        flags();                // flags = flagi
        precisionOnStrings();   // precision on strings = precyzja na napisach
        localeMatters();        // locale matters = Locale ma znaczenie
        formattedMethod();      // formatted method = metoda formatted
        textBlocks();           // text blocks = bloki tekstu
        formatErrors();         // format errors = błędy formatowania
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. NUMEROWANIE ARGUMENTÓW
    // =================================================================================================

    /** 1. %1$s = „pierwszy argument”, %2$s = „drugi”. Ten sam argument można użyć kilka razy i w innej kolejności. */
    static void argumentIndex() {
        section("1. Numerowane argumenty: %1$s, %2$s");

        show("kolejność odwrócona", String.format("%2$s, %1$s", "Jan", "Kowalski"));
        show("ten sam argument dwa razy", String.format("%1$s-%1$s-%2$s", "tak", "nie"));
        // WYNIK: kolejność odwrócona → Kowalski, Jan
        // WYNIK: ten sam argument dwa razy → tak-tak-nie
    }

    // =================================================================================================
    // 2. FLAGI
    // =================================================================================================

    /** 2. Flagi między % a literą zmieniają wygląd: - (do lewej), 0 (zera), + (znak), , (grupowanie), ( (ujemne w nawiasie). */
    static void flags() {
        section("2. Flagi: - 0 + , (");

        show("[%-6d]", String.format("[%-6d]", 42));
        show("[%06d]", String.format("[%06d]", 42));
        show("%+d i %+d", String.format("%+d i %+d", 5, -5));
        show("%,d (ROOT)", String.format(Locale.ROOT, "%,d", 1234567));
        show("%(.2f (księgowo)", String.format(Locale.ROOT, "%(.2f", -99.5));
        // WYNIK: [%-6d] → [42    ]
        // WYNIK: [%06d] → [000042]
        // WYNIK: %+d i %+d → +5 i -5
        // WYNIK: %,d (ROOT) → 1,234,567
        // WYNIK: %(.2f (księgowo) → (99.50)
    }

    // =================================================================================================
    // 3. PRECYZJA NA NAPISACH
    // =================================================================================================

    /** 3. Dla %s precyzja oznacza MAKSYMALNĄ liczbę znaków — dłuższy napis zostanie ucięty. Przydatne w tabelach. */
    static void precisionOnStrings() {
        section("3. %.Ns — ucinanie napisu");

        show("[%.3s]", String.format("[%.3s]", "Programowanie"));
        show("[%-8.8s] (dokładnie 8 znaków)", String.format("[%-8.8s]", "Programowanie"));
        show("[%-8.8s] krótszy", String.format("[%-8.8s]", "Java"));
        // WYNIK: [%.3s] → [Pro]
        // WYNIK: [%-8.8s] (dokładnie 8 znaków) → [Programo]
        // WYNIK: [%-8.8s] krótszy → [Java    ]
        // DOBRA PRAKTYKA: w tabelach tekstowych %-N.Ns trzyma kolumnę w stałej szerokości niezależnie od długości danych.
    }

    // =================================================================================================
    // 4. LOCALE MA ZNACZENIE
    // =================================================================================================

    /** visible = uwidocznij: zamienia spację niełamliwą (kod 160) i wąską niełamliwą (kod 8239) na „_”, żeby było je widać. */
    static String visible(String text) {
        return text.replace((char) 160, '_').replace((char) 8239, '_');
    }

    /**
     * 4. Separator dziesiętny i separator tysięcy zależą od Locale. Po polsku grupy oddziela SPACJA NIEŁAMLIWA —
     * wygląda jak spacja, ale to inny znak (kod 160). Tu zamieniamy ją na „_”, żeby ją zobaczyć.
     */
    static void localeMatters() {
        section("4. Locale: kropka czy przecinek, jaka spacja");

        show("ROOT %,.2f", String.format(Locale.ROOT, "%,.2f", 1234567.891));
        show("US %,.2f", String.format(Locale.US, "%,.2f", 1234567.891));
        show("pl-PL %,.2f (spacje jako _)", visible(String.format(POLISH, "%,.2f", 1234567.891)));
        // WYNIK: ROOT %,.2f → 1,234,567.89
        // WYNIK: US %,.2f → 1,234,567.89
        // WYNIK: pl-PL %,.2f (spacje jako _) → 1_234_567,89

        // PUŁAPKA: polski wynik „1 234 567,89” zawiera spację NIEŁAMLIWĄ. Porównanie z tekstem wpisanym zwykłą spacją
        //   da false, a Double.parseDouble go nie przeczyta. Szczegóły i rozwiązania: t15_numbers/Numbers04FormattingParsing.
    }

    // =================================================================================================
    // 5. METODA formatted (Java 15+)
    // =================================================================================================

    /** 5. "szablon".formatted(args) to to samo co String.format("szablon", args) — często czytelniej. Locale domyślne! */
    static void formattedMethod() {
        section("5. formatted (Java 15+)");

        String template = "%s kupił(a) %d szt.";
        show("formatted", template.formatted("Ola", 3));
        // WYNIK: formatted → Ola kupił(a) 3 szt.

        // PUŁAPKA: formatted NIE przyjmuje Locale — używa ustawień komputera. Do liczb z ułamkiem, które muszą mieć
        //   konkretny separator, użyj String.format(Locale.ROOT, ...).
    }

    // =================================================================================================
    // 6. BLOKI TEKSTU (Java 15+)
    // =================================================================================================

    /**
     * 6. Blok tekstu: otwierające """ i nowa linia, zamykające """. Wspólne wcięcie jest usuwane automatycznie
     * (tzw. incidental whitespace). Zamykające """ w osobnej linii = napis kończy się znakiem nowej linii.
     * Sekwencja \ na końcu linii łączy linie (bez przejścia do nowej).
     */
    static void textBlocks() {
        section("6. Bloki tekstu + formatted");

        String card = """
                Imię:     %s
                Punkty:   %d
                """.formatted("Anna", 42);
        System.out.print(card);                   // print, bo blok kończy się już nową linią
        // WYNIK: Imię:     Anna
        // WYNIK: Punkty:   42

        String oneLine = """
                To jest bardzo długie zdanie, \
                pisane w dwóch liniach kodu.""";
        show("z \\ na końcu linii", oneLine);
        // WYNIK: z \ na końcu linii → To jest bardzo długie zdanie, pisane w dwóch liniach kodu.

        // DOBRA PRAKTYKA: bloki tekstu świetnie nadają się na szablony JSON, SQL i wielolinijkowe komunikaty.
    }

    // =================================================================================================
    // 7. BŁĘDY FORMATOWANIA
    // =================================================================================================

    /** 7. Błędy w szablonie wychodzą dopiero w czasie DZIAŁANIA (kompilator ich nie sprawdza). */
    static void formatErrors() {
        section("7. Błędy formatowania — wyjątki w czasie działania");

        expectThrows("za mało argumentów", () -> String.format("%s i %s", "tylko jeden"));
        // WYNIK: ✔ za mało argumentów → rzucono MissingFormatArgumentException: Format specifier '%s'
        expectThrows("zły typ (%d dla napisu)", () -> String.format("%d", "sto"));
        // WYNIK: ✔ zły typ (%d dla napisu) → rzucono IllegalFormatConversionException: d != java.lang.String
        expectThrows("%.2f dla int", () -> String.format("%.2f", 5));
        // WYNIK: ✔ %.2f dla int → rzucono IllegalFormatConversionException: f != java.lang.Integer

        // PUŁAPKA: %.2f wymaga liczby z ułamkiem (double/float/BigDecimal). Dla int użyj %d albo podaj 5.0.
        //   IntelliJ podkreśla wiele takich błędów w szablonach — zwracaj uwagę na żółte ostrzeżenia.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • %[numer$][flagi][szerokość][.precyzja]litera — np. %1$-10.3s, %+08.2f.
     *   • Flagi: - (do lewej), 0 (zera), + (znak), , (grupowanie), ( (ujemne w nawiasie).
     *   • %.Ns — ucina napis do N znaków; %-N.Ns — kolumna o stałej szerokości.
     *   • Locale: ROOT/US → 1,234.56; pl-PL → 1 234,56 ze SPACJĄ NIEŁAMLIWĄ (kod 160).
     *   • "...".formatted(args) (Java 15+) — bez Locale; String.format(Locale, ...) — z Locale.
     *   • Bloki tekstu """ (Java 15+): wspólne wcięcie usuwane; \ na końcu linii łączy linie.
     *   • Błędy szablonu → wyjątki w czasie działania (MissingFormatArgument..., IllegalFormatConversion...).
     *
     * PYTANIA KONTROLNE:
     *   1. Co wypisze:  System.out.println(String.format("%2$s-%1$s", "A", "B"));  ?
     *   2. Co wypisze:  System.out.println(String.format("[%5s][%-5s]", "ab", "ab"));  ?
     *   3. ZNAJDŹ BŁĄD:  String.format("Średnia: %.2f", 4);
     *   4. Dlaczego  String.format(POLISH, "%,d", 1000).equals("1 000")  może zwrócić false?
     *   5. Co wypisze:  System.out.println(String.format("%.4s", "Kraków"));  ?
     *   6. Czym różni się  "%d".formatted(5)  od  String.format(Locale.ROOT, "%d", 5)?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: \"Kowalski, Jan\"", "Kowalski, Jan", () -> exercise1("Jan", "Kowalski"));
        Check.equal("ćw. 2a: zmiana +3.456", "+3.46%", () -> exercise2(3.456));
        Check.equal("ćw. 2b: zmiana -1.2", "-1.20%", () -> exercise2(-1.2));
        Check.equal("ćw. 3: paragon z bloku tekstu", "PARAGON\nKawa: 64.99\nRAZEM: 64.99", () -> exercise3("Kawa", 64.99));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "Kowalski, Jan", () -> solution1("Jan", "Kowalski"));
        Check.equal("ćw. 2a (wzorzec)", "+3.46%", () -> solution2(3.456));
        Check.equal("ćw. 2b (wzorzec)", "-1.20%", () -> solution2(-1.2));
        Check.equal("ćw. 3 (wzorzec)", "PARAGON\nKawa: 64.99\nRAZEM: 64.99", () -> solution3("Kawa", 64.99));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): używając NUMEROWANYCH argumentów zwróć „Nazwisko, Imię”: ("Jan", "Kowalski") → "Kowalski, Jan".
     * Podpowiedź: {@code String.format("%2$s, %1$s", firstName, lastName)}.
     */
    static String exercise1(String firstName, String lastName) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 2 (średnie): zmiana procentowa ZAWSZE ze znakiem, 2 miejsca po kropce i znak %: 3.456 → "+3.46%",
     * -1.2 → "-1.20%".
     * Podpowiedź: flaga + i %% — {@code String.format(Locale.ROOT, "%+.2f%%", change)}.
     */
    static String exercise2(double change) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): zwróć trzylinijkowy paragon (BEZ nowej linii na końcu) z bloku tekstu i formatted:
     * <pre>
     * PARAGON
     * Kawa: 64.99
     * RAZEM: 64.99
     * </pre>
     * Podpowiedź: zamykające """ zaraz po ostatniej linii (w tej samej linii kodu) — wtedy nie ma końcowego \n.
     * Cenę formatuj przez %.2f; uwaga: formatted używa Locale komputera — na polskim systemie da przecinek!
     * Bezpieczniej: {@code String.format(Locale.ROOT, """ ... """, name, price, price)}.
     */
    static String exercise3(String name, double price) {
        // TODO: twoje rozwiązanie
        return "";
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(String firstName, String lastName) {
        return String.format("%2$s, %1$s", firstName, lastName);
    }

    static String solution2(double change) {
        return String.format(Locale.ROOT, "%+.2f%%", change);
    }

    static String solution3(String name, double price) {
        return String.format(Locale.ROOT, """
                PARAGON
                %s: %.2f
                RAZEM: %.2f""", name, price, price);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. B-A — %2$s to drugi argument, %1$s pierwszy.
     *   2. [   ab][ab   ] — szerokość 5, domyślnie do prawej; z minusem do lewej.
     *   3. %.2f wymaga liczby z ułamkiem, a 4 to int → IllegalFormatConversionException. Poprawnie: 4.0 albo %d.
     *   4. Bo polski separator tysięcy to spacja NIEŁAMLIWA (kod 160), a w "1 000" wpisano zwykłą spację (kod 32).
     *   5. Krak — precyzja .4 dla %s ucina napis do 4 znaków.
     *   6. formatted używa Locale komputera; String.format(Locale.ROOT, ...) — zawsze tych samych, neutralnych reguł.
     *      Dla %d bez grupowania wynik będzie ten sam, ale dla %,d albo %.2f może się różnić.
     */
    // </editor-fold>
}
