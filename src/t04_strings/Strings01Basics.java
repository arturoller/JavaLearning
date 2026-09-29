package t04_strings;

import helpers.Check;

import java.util.Objects;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: String — niezmienność, pula napisów, == kontra equals
 *        (string = napis; immutable = niezmienny; string pool = pula napisów; equals = równa się)
 *
 * W SKRÓCIE:
 *   String to obiekt przechowujący tekst. Jest NIEZMIENNY: każda „zmiana” (toUpperCase, concat, +) tworzy NOWY napis.
 *   Napisy porównujemy przez equals (treść), a NIE przez == (czy to ten sam obiekt). Identyczne literały ("Java")
 *   Java trzyma w jednym egzemplarzu w puli napisów — dlatego == czasem „działa”, co jest bardzo zdradliwe.
 *
 * ANALOGIA: tablica pamiątkowa wykuta w kamieniu.
 *   Napisu na kamieniu nie zmienisz. Jeśli chcesz inną treść, kujesz NOWĄ tablicę — stara zostaje, jaka była.
 *   Dwie tablice z identycznym napisem to nadal dwie różne tablice (==), choć treść mają tę samą (equals).
 *
 * JAK TO DZIAŁA:
 *   String a = "Java";            ← literał: trafia do puli napisów
 *   String b = "Java";            ← ten sam obiekt z puli:  a == b → true (ale nie polegaj na tym!)
 *   String c = new String("Java"); ← NOWY obiekt:  a == c → false,  a.equals(c) → true
 *   a.toUpperCase();              ← tworzy "JAVA", ale a nadal to "Java" (wynik trzeba przypisać)
 *
 * SŁÓWKA:
 *   immutable = niezmienny; literal = literał (napis wpisany w cudzysłowie); pool = pula; intern = wprowadź do puli;
 *   equals = równa się; equalsIgnoreCase = równe, ignorując wielkość liter; compareTo = porównaj z (kolejność);
 *   empty = pusty; blank = pusty lub same białe znaki; concatenate = połączyć, skleić.
 *
 * ZOBACZ TEŻ: t04_strings/Strings02Methods (metody String), t04_strings/Strings03StringBuilder (sklejanie w pętli),
 *             t01_basics/Basics09PassByValue (referencje), t06_oop_basics/Oop05ObjectMethods (equals w Twoich klasach).
 * </pre>
 */
public class Strings01Basics {

    public static void main(String[] args) {
        title("Strings01 — String: podstawy");

        creatingStrings();      // creating strings = tworzenie napisów
        immutability();         // immutability = niezmienność
        stringPool();           // string pool = pula napisów
        equalsVsDoubleEquals(); // equals vs == = equals kontra ==
        nullSafety();           // null safety = bezpieczeństwo przy null
        compareTo();            // compare to = porównywanie kolejności
        emptyAndBlank();        // empty and blank = pusty i „biały”
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. TWORZENIE NAPISÓW
    // =================================================================================================

    /** 1. Napis tworzymy literałem w cudzysłowie. length() (długość) to METODA — z nawiasami (w tablicach length bez nawiasów!). */
    static void creatingStrings() {
        section("1. Tworzenie napisów, length()");

        String city = "Kraków";
        String empty = "";
        show("city.length()", city.length());
        show("empty.length()", empty.length());
        // WYNIK: city.length() → 6
        // WYNIK: empty.length() → 0

        show("String.valueOf(42) + 1", String.valueOf(42) + 1);     // valueOf = wartość jako napis
        // WYNIK: String.valueOf(42) + 1 → 421    ← "42" + 1 to sklejanie, nie dodawanie

        // DOBRA PRAKTYKA: nie twórz napisów przez new String("...") — to niepotrzebny dodatkowy obiekt.
    }

    // =================================================================================================
    // 2. NIEZMIENNOŚĆ
    // =================================================================================================

    /** 2. Żadna metoda String nie zmienia napisu — zwraca NOWY. Wynik trzeba przypisać. */
    static void immutability() {
        section("2. Niezmienność — metody zwracają NOWY napis");

        String name = "anna";
        name.toUpperCase();                       // wynik wyrzucony!
        show("po name.toUpperCase()", name);
        // WYNIK: po name.toUpperCase() → anna

        name = name.toUpperCase();                // przypisz nowy napis do zmiennej
        show("po name = name.toUpperCase()", name);
        // WYNIK: po name = name.toUpperCase() → ANNA

        // Dlaczego niezmienność jest dobra? Napis można bezpiecznie przekazać do metody albo innego wątku — nikt go
        //   „po cichu” nie zmieni. Dzięki temu działa pula napisów i String może być kluczem w mapie (t12_collections).
    }

    // =================================================================================================
    // 3. PULA NAPISÓW
    // =================================================================================================

    /**
     * 3. Literały o tej samej treści Java przechowuje w JEDNYM egzemplarzu (pula napisów, string pool). Napisy
     * tworzone w czasie działania programu (new, sklejanie zmiennych, wczytywanie) są osobnymi obiektami.
     */
    static void stringPool() {
        section("3. Pula napisów (string pool)");

        String a = "Java";
        String b = "Java";
        String c = new String("Java");
        show("a == b (oba literały)", a == b);
        show("a == c (new String)", a == c);
        // WYNIK: a == b (oba literały) → true    ← ten sam obiekt z puli
        // WYNIK: a == c (new String) → false    ← nowy obiekt

        String part = "Ja";
        String built = part + "va";               // sklejanie w czasie DZIAŁANIA → nowy obiekt
        show("a == part + \"va\"", a == built);
        show("a == \"Ja\" + \"va\"", a == "Ja" + "va");   // dwa literały sklejane przez KOMPILATOR → z puli
        // WYNIK: a == part + "va" → false
        // WYNIK: a == "Ja" + "va" → true

        show("c.intern() == a", c.intern() == a);  // intern = oddaj egzemplarz z puli
        // WYNIK: c.intern() == a → true

        // PUŁAPKA: to, czy == zwróci true, zależy od SPOSOBU utworzenia napisu. Kod z == „działa” na literałach
        //   w testach, a psuje się na danych z formularza albo pliku. Dlatego zawsze equals (sekcja 4).
    }

    // =================================================================================================
    // 4. == KONTRA equals
    // =================================================================================================

    /** 4. equals porównuje TREŚĆ, == porównuje, czy to TEN SAM obiekt. Do napisów zawsze equals (albo equalsIgnoreCase). */
    static void equalsVsDoubleEquals() {
        section("4. equals (treść) kontra == (ten sam obiekt)");

        String fromUser = new String("tak");      // symulacja napisu wczytanego od użytkownika
        show("fromUser == \"tak\"", fromUser == "tak");
        show("fromUser.equals(\"tak\")", fromUser.equals("tak"));
        show("\"TAK\".equalsIgnoreCase(\"tak\")", "TAK".equalsIgnoreCase("tak"));
        // WYNIK: fromUser == "tak" → false    ← ten błąd w warunku if łatwo przeoczyć!
        // WYNIK: fromUser.equals("tak") → true
        // WYNIK: "TAK".equalsIgnoreCase("tak") → true
    }

    // =================================================================================================
    // 5. null I BEZPIECZNE PORÓWNANIA
    // =================================================================================================

    /** 5. Wywołanie metody na null → NullPointerException. Bezpieczne: literał po lewej albo Objects.equals. */
    static void nullSafety() {
        section("5. null — bezpieczne porównania");

        String answer = null;                      // np. użytkownik nic nie wpisał
        expectThrows("answer.equals(\"tak\") gdy answer == null", () -> answer.equals("tak"));
        // WYNIK: ✔ answer.equals("tak") gdy answer == null → rzucono NullPointerException: Cannot invoke "String.equals(Object)" because "answer" is null

        show("\"tak\".equals(answer)", "tak".equals(answer));             // literał po lewej — nie ma NPE
        show("Objects.equals(answer, \"tak\")", Objects.equals(answer, "tak"));
        // WYNIK: "tak".equals(answer) → false
        // WYNIK: Objects.equals(answer, "tak") → false

        // DOBRA PRAKTYKA: porównując zmienną ze stałym tekstem, pisz "tekst".equals(zmienna) — działa też dla null.
    }

    // =================================================================================================
    // 6. compareTo — KOLEJNOŚĆ
    // =================================================================================================

    /**
     * 6. compareTo zwraca liczbę: ujemną (pierwszy „przed”), 0 (równe) albo dodatnią (pierwszy „po”). Porównuje KODY
     * znaków (Unicode) — więc wielkie litery są przed małymi, a polskie litery za łacińskimi.
     */
    static void compareTo() {
        section("6. compareTo — kto pierwszy w kolejności");

        show("\"ala\".compareTo(\"ola\") < 0", "ala".compareTo("ola") < 0);
        show("\"Zenek\".compareTo(\"adam\") < 0", "Zenek".compareTo("adam") < 0);
        show("\"łódź\".compareTo(\"zamość\") > 0", "łódź".compareTo("zamość") > 0);
        // WYNIK: "ala".compareTo("ola") < 0 → true
        // WYNIK: "Zenek".compareTo("adam") < 0 → true    ← wielka litera Z ma mniejszy kod niż mała a!
        // WYNIK: "łódź".compareTo("zamość") > 0 → true    ← ł ma większy kod niż z — to NIE jest polski alfabet

        show("compareToIgnoreCase", "Zenek".compareToIgnoreCase("adam") > 0);
        // WYNIK: compareToIgnoreCase → true

        // Sortowanie po polsku (a, ą, b, c, ć...) wymaga klasy Collator — t16_streams/Streams05SortDistinctLimit.
    }

    // =================================================================================================
    // 7. PUSTY, „BIAŁY”, null
    // =================================================================================================

    /** 7. isEmpty — długość 0; isBlank (Java 11+) — pusty ALBO same białe znaki (spacje, tabulatory). null to brak napisu. */
    static void emptyAndBlank() {
        section("7. isEmpty kontra isBlank");

        show("\"\".isEmpty()", "".isEmpty());
        show("\"   \".isEmpty()", "   ".isEmpty());
        show("\"   \".isBlank()", "   ".isBlank());
        // WYNIK: "".isEmpty() → true
        // WYNIK: "   ".isEmpty() → false    ← trzy spacje to 3 znaki
        // WYNIK: "   ".isBlank() → true

        // DOBRA PRAKTYKA: do sprawdzania danych od użytkownika używaj isBlank — „   ” to w praktyce też „nic nie wpisano”.
        //   Pełna kontrola: text == null || text.isBlank().
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • String jest NIEZMIENNY — metody zwracają nowy napis: s = s.toUpperCase();
     *   • Porównuj treść: equals / equalsIgnoreCase; == tylko sprawdza, czy to ten sam obiekt.
     *   • Literały trafiają do puli (== czasem true); new, sklejanie w czasie działania, dane z zewnątrz — nowe obiekty.
     *   • null: "tekst".equals(zmienna) albo Objects.equals(a, b) — bez NullPointerException.
     *   • compareTo: <0 / 0 / >0 według kodów Unicode (wielkie przed małymi, polskie litery na końcu).
     *   • isEmpty (długość 0) kontra isBlank (Java 11+, też same spacje).
     *   • length() w String — z nawiasami; length w tablicy — bez.
     *
     * PYTANIA KONTROLNE:
     *   1. Co wypisze:  String s = "java"; s.toUpperCase(); System.out.println(s);  ?
     *   2. Co wypisze:  String a = "hi"; String b = "h"; b = b + "i"; System.out.println(a == b);  ?
     *   3. ZNAJDŹ BŁĄD:  if (command == "exit") { ... }   (command wczytany ze Scannera)
     *   4. Jak porównać napis ze stałym tekstem, żeby nie dostać NullPointerException, gdy napis jest null?
     *   5. Dlaczego "Zenek".compareTo("adam") jest ujemne?
     *   6. Czym różni się "  ".isEmpty() od "  ".isBlank()?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: \"Java\" i \"JAVA\"", true, () -> exercise1("Java", "JAVA"));
        Check.equal("ćw. 1b: null i \"x\"", false, () -> exercise1(null, "x"));
        Check.equal("ćw. 2a: długość null", 0, () -> exercise2(null));
        Check.equal("ćw. 2b: długość \"  ab  \" bez spacji z brzegów", 2, () -> exercise2("  ab  "));
        Check.equal("ćw. 3: inicjały \"Jan Kowalski\"", "J.K.", () -> exercise3("Jan Kowalski"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", true, () -> solution1("Java", "JAVA"));
        Check.equal("ćw. 1b (wzorzec)", false, () -> solution1(null, "x"));
        Check.equal("ćw. 2a (wzorzec)", 0, () -> solution2(null));
        Check.equal("ćw. 2b (wzorzec)", 2, () -> solution2("  ab  "));
        Check.equal("ćw. 3 (wzorzec)", "J.K.", () -> solution3("Jan Kowalski"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): czy dwa napisy są równe, ignorując wielkość liter? Gdy któryś jest null → false.
     * Podpowiedź: {@code a != null && a.equalsIgnoreCase(b)} (equalsIgnoreCase(null) zwraca false, nie rzuca wyjątku).
     */
    static boolean exercise1(String a, String b) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć długość napisu bez białych znaków na brzegach; dla null zwróć 0.
     * Podpowiedź: {@code strip()} (Java 11+) usuwa białe znaki z początku i końca; najpierw sprawdź null.
     */
    static int exercise2(String text) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): z „Imię Nazwisko” zrób inicjały „I.N.” — np. "Jan Kowalski" → "J.K.".
     * Podpowiedź: pierwsza litera to {@code charAt(0)}; pozycja spacji: {@code indexOf(' ')} (Strings02Methods),
     * litera nazwiska jest na pozycji spacja + 1. Uwaga: char + char to liczba — zacznij od napisu: {@code "" + ...}.
     */
    static String exercise3(String fullName) {
        // TODO: twoje rozwiązanie
        return "";
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static boolean solution1(String a, String b) {
        return a != null && a.equalsIgnoreCase(b);
    }

    static int solution2(String text) {
        if (text == null) {
            return 0;
        }
        return text.strip().length();
    }

    static String solution3(String fullName) {
        int space = fullName.indexOf(' ');
        return "" + fullName.charAt(0) + "." + fullName.charAt(space + 1) + ".";
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. java — toUpperCase zwrócił nowy napis, który nie został przypisany.
     *   2. false — b powstał przez sklejanie w czasie działania (nowy obiekt), a == porównuje obiekty.
     *   3. == porównuje obiekty, a napis ze Scannera to nowy obiekt — warunek będzie fałszywy nawet dla „exit”.
     *      Poprawnie: "exit".equals(command).
     *   4. "tekst".equals(zmienna) albo Objects.equals(zmienna, "tekst").
     *   5. compareTo porównuje kody znaków: 'Z' (90) ma mniejszy kod niż 'a' (97).
     *   6. isEmpty zwraca false (dwa znaki spacji), isBlank — true (same białe znaki).
     */
    // </editor-fold>
}
