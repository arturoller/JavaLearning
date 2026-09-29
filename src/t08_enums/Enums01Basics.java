package t08_enums;

import helpers.Check;
import helpers.model.OrderStatus;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Enum — podstawy typu wyliczeniowego
 *        (enum / enumeration = wyliczenie; constant = stała; ordinal = numer porządkowy)
 *
 * W SKRÓCIE:
 *   Enum to typ z ZAMKNIĘTĄ listą wartości, np. dni tygodnia. Zamiast napisów "MONDAY" albo liczb 1, 2, 3 (gdzie łatwo
 *   o literówkę i nielegalną wartość) masz stałe sprawdzane przez kompilator: Day.MONDAY. Każda stała istnieje w programie
 *   DOKŁADNIE RAZ, więc porównujemy je przez ==. Enum ma wbudowane metody: values(), valueOf(), name(), ordinal().
 *
 * ANALOGIA: pilot z przyciskami.
 *   Napis to klawiatura — możesz wpisać cokolwiek, także bzdurę („PONIEDZAILEK”). Enum to pilot z kilkoma przyciskami:
 *   nie da się nacisnąć przycisku, którego nie ma. Kompilator „sprawdza, czy przycisk istnieje”.
 *
 * JAK TO DZIAŁA:
 *   enum Day { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }
 *   Day d = Day.FRIDAY;
 *   d.name()        → "FRIDAY"           d.ordinal()      → 4 (pozycja od 0)
 *   Day.values()    → tablica wszystkich stałych w kolejności deklaracji
 *   Day.valueOf("SUNDAY") → Day.SUNDAY    valueOf("sunday") → IllegalArgumentException (wielkość liter!)
 *
 * SŁÓWKA:
 *   enum = wyliczenie, typ wyliczeniowy; constant = stała; values = wartości; value of = wartość z (napisu);
 *   name = nazwa; ordinal = numer porządkowy; exhaustive = wyczerpujący (obejmujący wszystkie przypadki);
 *   weekend = weekend; next = następny.
 *
 * ZOBACZ TEŻ: t08_enums/Enums02FieldsMethods (enum z polami), t02_controlflow/Control02Switch (switch na enumie),
 *             t08_enums/Enums04EnumMapSet (EnumSet, EnumMap), t06_oop_basics/Oop04Static (stałe static final — „stary” sposób).
 * </pre>
 */
public class Enums01Basics {

    /** Day = dzień tygodnia. Enum zagnieżdżony w lekcji (w prawdziwym projekcie — osobny plik Day.java). */
    enum Day {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }

    public static void main(String[] args) {
        title("Enums01 — enum: podstawy");

        whyEnum();              // why enum = po co enum
        builtInMethods();       // built-in methods = wbudowane metody
        valueOfFromText();      // value of from text = valueOf z napisu
        comparing();            // comparing = porównywanie
        switchOnEnum();         // switch on enum = switch na enumie
        ordinalPitfall();       // ordinal pitfall = pułapka ordinal
        enumsFromCourseModel(); // enums from course model = enumy z modelu kursu
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PO CO ENUM
    // =================================================================================================

    /** 1. PRZED: statusy jako napisy — literówka kompiluje się bez błędu. PO: enum — literówka to błąd kompilacji. */
    static void whyEnum() {
        section("1. Napis kontra enum");

        String statusText = "SHIPED";                 // literówka (powinno być SHIPPED) — kompilator nie zauważy
        show("napis z literówką przechodzi", "SHIPPED".equals(statusText));
        // WYNIK: napis z literówką przechodzi → false    ← błąd wyjdzie dopiero w działaniu programu (albo wcale)

        Day payday = Day.FRIDAY;                      // Day.FRIDEY — błąd kompilacji „cannot find symbol”
        show("enum", payday);
        // WYNIK: enum → FRIDAY

        // DOBRA PRAKTYKA: gdy zmienna może mieć tylko kilka z góry znanych wartości (status, typ, kategoria, rozmiar),
        //   użyj enuma zamiast String lub int. Dodatkowo IntelliJ podpowiada wszystkie możliwe wartości (Ctrl+Spacja).
    }

    // =================================================================================================
    // 2. WBUDOWANE METODY
    // =================================================================================================

    /** 2. Każdy enum ma values() (wszystkie stałe), name() (nazwa), ordinal() (pozycja od 0) i toString() (domyślnie = name()). */
    static void builtInMethods() {
        section("2. values(), name(), ordinal()");

        show("Day.values().length", Day.values().length);
        show("FRIDAY.name()", Day.FRIDAY.name());
        show("FRIDAY.ordinal()", Day.FRIDAY.ordinal());
        // WYNIK: Day.values().length → 7
        // WYNIK: FRIDAY.name() → FRIDAY
        // WYNIK: FRIDAY.ordinal() → 4    ← liczone od 0: MONDAY=0, TUESDAY=1...

        StringBuilder sb = new StringBuilder();
        for (Day day : Day.values()) {                // values() zwraca stałe w kolejności deklaracji
            sb.append(day.name(), 0, 3).append(' ');   // pierwsze 3 litery nazwy
        }
        show("skróty dni", sb.toString().strip());
        // WYNIK: skróty dni → MON TUE WED THU FRI SAT SUN
    }

    // =================================================================================================
    // 3. valueOf — ENUM Z NAPISU
    // =================================================================================================

    /** 3. valueOf zamienia napis na stałą. Nazwa musi pasować DOKŁADNIE (wielkość liter!), inaczej wyjątek. */
    static void valueOfFromText() {
        section("3. valueOf — z napisu na enum");

        show("Day.valueOf(\"SUNDAY\")", Day.valueOf("SUNDAY"));
        // WYNIK: Day.valueOf("SUNDAY") → SUNDAY

        expectThrows("Day.valueOf(\"sunday\")", () -> Day.valueOf("sunday"));
        // WYNIK: ✔ Day.valueOf("sunday") → rzucono IllegalArgumentException: No enum constant t08_enums.Enums01Basics.Day.sunday

        // DOBRA PRAKTYKA: dane z zewnątrz (formularz, plik) najpierw oczyść: Day.valueOf(text.strip().toUpperCase(Locale.ROOT))
        //   i obsłuż wyjątek (ćwiczenie 3).
    }

    // =================================================================================================
    // 4. PORÓWNYWANIE
    // =================================================================================================

    /** 4. Stałe enum istnieją w jednym egzemplarzu, więc == jest poprawne (i bezpieczne dla null). compareTo — wg kolejności. */
    static void comparing() {
        section("4. == i compareTo");

        Day today = Day.SATURDAY;
        show("today == Day.SATURDAY", today == Day.SATURDAY);
        show("MONDAY przed FRIDAY?", Day.MONDAY.compareTo(Day.FRIDAY) < 0);
        // WYNIK: today == Day.SATURDAY → true
        // WYNIK: MONDAY przed FRIDAY? → true    ← compareTo porównuje kolejność deklaracji

        Day unknown = null;
        show("unknown == Day.MONDAY (bez NPE)", unknown == Day.MONDAY);
        // WYNIK: unknown == Day.MONDAY (bez NPE) → false    ← == nie wywołuje metody, więc null nie szkodzi
    }

    // =================================================================================================
    // 5. SWITCH NA ENUMIE
    // =================================================================================================

    /** typeOfDay = rodzaj dnia. Wyrażenie switch (Java 14+) na enumie — w case piszemy SAME nazwy stałych. */
    static String typeOfDay(Day day) {
        return switch (day) {
            case SATURDAY, SUNDAY -> "weekend";
            case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY -> "dzień roboczy";
        };                                             // wszystkie stałe obsłużone → default niepotrzebny
    }

    /**
     * 5. W wyrażeniu switch na enumie, które obsługuje WSZYSTKIE stałe, default nie jest potrzebny. Gdy dodasz nową
     * stałą do enuma, kompilator zgłosi błąd we wszystkich takich switchach — nie zapomnisz jej obsłużyć.
     */
    static void switchOnEnum() {
        section("5. switch na enumie (wyczerpujący)");

        show("SUNDAY", typeOfDay(Day.SUNDAY));
        show("WEDNESDAY", typeOfDay(Day.WEDNESDAY));
        // WYNIK: SUNDAY → weekend
        // WYNIK: WEDNESDAY → dzień roboczy

        expectThrows("switch na null", () -> typeOfDay(null));
        // WYNIK: ✔ switch na null → rzucono NullPointerException: Cannot invoke "t08_enums.Enums01Basics$Day.ordinal()" because "day" is null
        // Ciekawostka z komunikatu: switch na enumie pod spodem wywołuje day.ordinal() — stąd NPE dla null.
        // PUŁAPKA: switch na enumie, który jest null, rzuca NullPointerException — sprawdź null wcześniej.
    }

    // =================================================================================================
    // 6. PUŁAPKA: ordinal
    // =================================================================================================

    /**
     * 6. ordinal() zależy od KOLEJNOŚCI deklaracji. Dopisanie stałej w środku enuma przesuwa numery wszystkich dalszych.
     * Jeśli zapisałeś ordinal w bazie danych albo pliku — dane „przeskakują” na inne wartości.
     */
    static void ordinalPitfall() {
        section("6. Pułapka: nie zapisuj ordinal()");

        int saved = Day.FRIDAY.ordinal();              // zapisane „na później”, np. do pliku: 4
        show("zapisany ordinal FRIDAY", saved);
        show("odczyt po ordinal", Day.values()[saved]);
        // WYNIK: zapisany ordinal FRIDAY → 4
        // WYNIK: odczyt po ordinal → FRIDAY
        // Gdyby ktoś dopisał stałą przed FRIDAY, odczyt values()[4] dałby THURSDAY — dane by się „pomieszały”.

        // DOBRA PRAKTYKA: zapisuj name() (i odczytuj przez valueOf) albo własny, stały kod (Enums02FieldsMethods).
        //   ordinal() zostaw do obliczeń „następny/poprzedni” w obrębie działającego programu.
    }

    // =================================================================================================
    // 7. ENUMY Z MODELU KURSU
    // =================================================================================================

    /** 7. W helpers.model kursu są enumy Category, Department, OrderStatus — takie same jak tu, tylko w osobnych plikach. */
    static void enumsFromCourseModel() {
        section("7. Enumy z modelu kursu (helpers.model)");

        OrderStatus status = OrderStatus.WYSLANE;
        show("status / czy końcowy", status + " / " + status.isFinal());
        show("wszystkie statusy", java.util.Arrays.toString(OrderStatus.values()));
        // WYNIK: status / czy końcowy → WYSLANE / false
        // WYNIK: wszystkie statusy → [NOWE, OPLACONE, WYSLANE, DOSTARCZONE, ANULOWANE]
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • enum X { A, B, C } — zamknięta lista stałych; kompilator pilnuje poprawnych wartości.
     *   • values() — wszystkie stałe (kolejność deklaracji); name() — nazwa; ordinal() — pozycja od 0.
     *   • valueOf("A") — z napisu; zła nazwa (także inna wielkość liter) → IllegalArgumentException.
     *   • Porównuj przez == (jeden egzemplarz każdej stałej; bezpieczne dla null); compareTo — kolejność deklaracji.
     *   • switch na enumie: w case same nazwy; wyrażenie switch z wszystkimi stałymi — bez default; null → NPE.
     *   • Nie zapisuj ordinal() do plików/baz — zapisuj name() albo własny kod.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego enum jest lepszy od stałych typu String dla statusu zamówienia?
     *   2. Co wypisze:  System.out.println(Day.values()[0] + " " + Day.SUNDAY.ordinal());  ?
     *   3. ZNAJDŹ BŁĄD:  Day d = Day.valueOf(scanner.nextLine());   (użytkownik wpisuje „piątek” albo „friday”)
     *   4. Czy enumy można porównywać przez ==? Dlaczego?
     *   5. Co się stanie z wyczerpującym wyrażeniem switch (bez default), gdy do enuma dopiszesz nową stałą?
     *   6. Dlaczego zapisanie ordinal() w bazie danych jest złym pomysłem?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: SATURDAY to weekend", true, () -> exercise1(Day.SATURDAY));
        Check.equal("ćw. 1b: MONDAY to nie weekend", false, () -> exercise1(Day.MONDAY));
        Check.equal("ćw. 2a: dzień po WEDNESDAY", Day.THURSDAY, () -> exercise2(Day.WEDNESDAY));
        Check.equal("ćw. 2b: dzień po SUNDAY", Day.MONDAY, () -> exercise2(Day.SUNDAY));
        Check.equal("ćw. 3a: \" friday \" → FRIDAY", Day.FRIDAY, () -> exercise3(" friday "));
        Check.equal("ćw. 3b: \"piątek\" → MONDAY (domyślny)", Day.MONDAY, () -> exercise3("piątek"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", true, () -> solution1(Day.SATURDAY));
        Check.equal("ćw. 1b (wzorzec)", false, () -> solution1(Day.MONDAY));
        Check.equal("ćw. 2a (wzorzec)", Day.THURSDAY, () -> solution2(Day.WEDNESDAY));
        Check.equal("ćw. 2b (wzorzec)", Day.MONDAY, () -> solution2(Day.SUNDAY));
        Check.equal("ćw. 3a (wzorzec)", Day.FRIDAY, () -> solution3(" friday "));
        Check.equal("ćw. 3b (wzorzec)", Day.MONDAY, () -> solution3("piątek"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
    }

    /** ĆWICZENIE 1 (łatwe): czy dzień to weekend (sobota albo niedziela)? Podpowiedź: == i ||, albo switch. */
    static boolean exercise1(Day day) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć NASTĘPNY dzień tygodnia; po SUNDAY jest MONDAY.
     * Podpowiedź: {@code Day.values()[(day.ordinal() + 1) % Day.values().length]} — % „zawija” na początek.
     */
    static Day exercise2(Day day) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): zamień tekst od użytkownika na Day: usuń spacje z brzegów, zamień na wielkie litery
     * (Locale.ROOT) i użyj valueOf. Gdy tekst nie pasuje do żadnej stałej — zwróć Day.MONDAY (wartość domyślną).
     * Podpowiedź: try { return Day.valueOf(...); } catch (IllegalArgumentException e) { return Day.MONDAY; }
     */
    static Day exercise3(String text) {
        // TODO: twoje rozwiązanie
        return null;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static boolean solution1(Day day) {
        return day == Day.SATURDAY || day == Day.SUNDAY;
    }

    static Day solution2(Day day) {
        Day[] all = Day.values();
        return all[(day.ordinal() + 1) % all.length];
    }

    static Day solution3(String text) {
        try {
            return Day.valueOf(text.strip().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Day.MONDAY;
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Kompilator pilnuje poprawnych wartości (literówka = błąd kompilacji), IntelliJ podpowiada możliwe stałe,
     *      a switch może sprawdzić, czy obsłużono wszystkie przypadki.
     *   2. „MONDAY 6”.
     *   3. valueOf wymaga dokładnej nazwy stałej („FRIDAY”) — „piątek” czy „friday” rzucą IllegalArgumentException.
     *      Trzeba oczyścić tekst (strip, toUpperCase) i obsłużyć wyjątek albo zrobić własne wyszukiwanie (np. po polskiej nazwie).
     *   4. Tak — każda stała istnieje w programie dokładnie raz, więc == porównuje ten sam obiekt; dodatkowo == nie rzuca
     *      NullPointerException, gdy zmienna jest null.
     *   5. Kompilator zgłosi błąd, że switch nie obsługuje wszystkich przypadków — i przypomni o nowej stałej.
     *   6. ordinal zależy od kolejności deklaracji; dopisanie stałej w środku enuma zmienia numery i odczytane dane
     *      wskażą złe wartości.
     */
    // </editor-fold>
}
