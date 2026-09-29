package t01_basics;

import helpers.Check;

import java.util.ArrayList;
import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Klasy opakowujące (Integer, Double...) i autoboxing
 *        (wrapper = opakowanie; boxing = pakowanie do pudełka; unboxing = wyjmowanie z pudełka)
 *
 * W SKRÓCIE:
 *   Każdy typ prosty ma „obiektową” wersję: int → Integer, long → Long, double → Double, boolean → Boolean,
 *   char → Character. Są potrzebne tam, gdzie Java wymaga obiektów — np. w kolekcjach ({@code List<Integer>} zamiast
 *   {@code List<int>}, która nie istnieje). Java sama zamienia int ↔ Integer (autoboxing), ale to rodzi pułapki:
 *   == na obiektach, null przy rozpakowaniu, remove(int) vs remove(Object).
 *
 * ANALOGIA: moneta i moneta w kopercie.
 *   int to sama moneta. Integer to moneta włożona do koperty z etykietą. Kopertę można wrzucić do segregatora
 *   (kolekcji), można mieć PUSTĄ kopertę (null) — ale dwie koperty z tą samą monetą to nadal DWIE różne koperty.
 *
 * JAK TO DZIAŁA:
 *   Integer boxed = 5;       ← autoboxing: Java robi  Integer.valueOf(5)
 *   int raw = boxed;         ← unboxing:   Java robi  boxed.intValue()   (NullPointerException, gdy boxed == null!)
 *   Integer.parseInt("42")   → int          Integer.valueOf("42") → Integer
 *   a.equals(b)              → porównuje WARTOŚĆ;   a == b → porównuje, czy to TEN SAM obiekt
 *
 * SŁÓWKA:
 *   wrapper = klasa opakowująca; boxing = opakowanie; unboxing = rozpakowanie; parse = przetwórz (tekst na liczbę);
 *   valueOf = wartość z (utwórz z); cache = pamięć podręczna; equals = równa się (porównanie wartości);
 *   intValue = wartość jako int; digit = cyfra; letter = litera; remove = usuń; index = indeks (pozycja).
 *
 * ZOBACZ TEŻ: t01_basics/Basics02PrimitiveTypes (typy proste), t12_collections/Collections02Lists (listy),
 *             t16_streams/Streams08PrimitiveStreams (IntStream zamiast {@code Stream<Integer>}), t04_strings/Strings01Basics (== na napisach).
 * </pre>
 */
public class Basics06Wrappers {

    public static void main(String[] args) {
        title("Basics06 — klasy opakowujące i autoboxing");

        whyWrappers();          // why wrappers = po co klasy opakowujące
        autoboxing();           // autoboxing = automatyczne pakowanie
        integerCache();         // integer cache = pamięć podręczna Integer
        nullUnboxing();         // null unboxing = rozpakowanie null
        parsing();              // parsing = zamiana tekstu na liczbę
        differentTypes();       // different types = różne typy opakowań
        listRemovePitfall();    // list remove pitfall = pułapka remove na liście
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PO CO KLASY OPAKOWUJĄCE
    // =================================================================================================

    /** 1. Kolekcje przechowują OBIEKTY. Typ prosty int nie jest obiektem — dlatego piszemy {@code List<Integer>}. */
    static void whyWrappers() {
        section("1. Po co Integer, skoro jest int?");

        List<Integer> grades = new ArrayList<>();     // List<int> — błąd kompilacji („unexpected type”)
        grades.add(5);                                // 5 (int) zostaje automatycznie opakowane w Integer
        grades.add(4);
        show("lista ocen", grades);
        // WYNIK: lista ocen → [5, 4]

        Integer missing = null;                       // obiekt może być null — int nie może
        show("Integer może być null", missing);
        // WYNIK: Integer może być null → null

        // DOBRA PRAKTYKA: w zwykłych obliczeniach używaj int/double (szybsze, bez null). Integer tylko tam,
        //   gdzie trzeba obiektu: kolekcje, typy generyczne, „brak wartości” (a jeszcze lepiej Optional — t14_optional).
    }

    // =================================================================================================
    // 2. AUTOBOXING I UNBOXING
    // =================================================================================================

    /** 2. Java sama pakuje (int → Integer) i rozpakowuje (Integer → int) tam, gdzie trzeba. */
    static void autoboxing() {
        section("2. Autoboxing i unboxing");

        Integer boxed = 42;                 // autoboxing: Integer.valueOf(42)
        int raw = boxed;                    // unboxing: boxed.intValue()
        int sum = boxed + 8;                // unboxing do działania, wynik int
        show("boxed", boxed);
        show("raw", raw);
        show("boxed + 8", sum);
        // WYNIK: boxed → 42
        // WYNIK: raw → 42
        // WYNIK: boxed + 8 → 50
    }

    // =================================================================================================
    // 3. PAMIĘĆ PODRĘCZNA INTEGER I PUŁAPKA ==
    // =================================================================================================

    /**
     * 3. Integer.valueOf (używany przy autoboxingu) trzyma gotowe obiekty dla liczb od -128 do 127 (cache).
     * Dlatego == „działa” dla małych liczb i NIE działa dla większych. Zawsze porównuj przez equals.
     */
    static void integerCache() {
        section("3. Pułapka: == na Integer");

        Integer a = 127;
        Integer b = 127;
        Integer c = 128;
        Integer d = 128;
        show("127 == 127", a == b);
        show("128 == 128", c == d);
        show("128 equals 128", c.equals(d));
        // WYNIK: 127 == 127 → true     ← ten sam obiekt z cache
        // WYNIK: 128 == 128 → false    ← dwa RÓŻNE obiekty!
        // WYNIK: 128 equals 128 → true

        // PUŁAPKA: kod z == na Integer przechodzi testy na małych liczbach, a psuje się na produkcji przy większych.
        // DOBRA PRAKTYKA: obiekty porównuj przez equals (albo Objects.equals, gdy mogą być null);
        //   == zostaw dla typów prostych (int, double...).
    }

    // =================================================================================================
    // 4. ROZPAKOWANIE NULL → NullPointerException
    // =================================================================================================

    /** 4. Rozpakowanie null do int jest niemożliwe — int nie ma wartości „brak”. Java rzuca NullPointerException. */
    static void nullUnboxing() {
        section("4. Pułapka: null rozpakowany do int");

        Integer stock = null;             // np. „brak danych o stanie magazynu”
        expectThrows("int x = stock (null)", () -> {
            int x = stock;                // ukryte stock.intValue() na null
            System.out.println(x);
        });
        // WYNIK: ✔ int x = stock (null) → rzucono NullPointerException: Cannot invoke "java.lang.Integer.intValue()" because "stock" is null

        // PUŁAPKA: tego nie widać w kodzie — nie ma żadnej kropki ani wywołania metody, a NPE i tak leci.
    }

    // =================================================================================================
    // 5. ZAMIANA TEKSTU NA LICZBĘ
    // =================================================================================================

    /** 5. parseInt zwraca int, valueOf zwraca Integer. Niepoprawny tekst → NumberFormatException. */
    static void parsing() {
        section("5. parseInt, valueOf, NumberFormatException");

        int parsed = Integer.parseInt("42");
        Integer boxed = Integer.valueOf("42");
        double price = Double.parseDouble("19.99");
        show("parseInt(\"42\")", parsed);
        show("valueOf(\"42\")", boxed);
        show("parseDouble(\"19.99\")", price);
        // WYNIK: parseInt("42") → 42
        // WYNIK: valueOf("42") → 42
        // WYNIK: parseDouble("19.99") → 19.99

        expectThrows("parseInt(\" 42\") ze spacją", () -> Integer.parseInt(" 42"));
        // WYNIK: ✔ parseInt(" 42") ze spacją → rzucono NumberFormatException: For input string: " 42"
        expectThrows("parseDouble(\"19,99\") z przecinkiem", () -> Double.parseDouble("19,99"));
        // WYNIK: ✔ parseDouble("19,99") z przecinkiem → rzucono NumberFormatException: For input string: "19,99"

        // DOBRA PRAKTYKA: dane od użytkownika najpierw oczyść (trim — usuń spacje z brzegów), a przecinek zamień
        //   na kropkę albo użyj NumberFormat dla polskiego zapisu (t15_numbers/Numbers04FormattingParsing).
    }

    // =================================================================================================
    // 6. RÓŻNE TYPY OPAKOWAŃ NIE SĄ SOBIE RÓWNE
    // =================================================================================================

    /** 6. Integer 5 i Long 5 to różne klasy — equals zwraca false, nawet gdy wartość wygląda tak samo. */
    static void differentTypes() {
        section("6. Integer 5 ≠ Long 5");

        Integer five = 5;
        Long fiveLong = 5L;
        show("five.equals(fiveLong)", five.equals(fiveLong));
        show("five.longValue() == fiveLong", five.longValue() == fiveLong);
        // WYNIK: five.equals(fiveLong) → false    ← różne klasy
        // WYNIK: five.longValue() == fiveLong → true    ← porównanie wartości long

        // Przydatne metody statyczne: Integer.compare(a, b), Integer.max(a, b), Integer.sum(a, b),
        //   Character.isDigit(c) (czy cyfra), Character.isLetter(c) (czy litera), Character.toUpperCase(c).
        show("Character.isDigit('7')", Character.isDigit('7'));
        show("Integer.compare(3, 9)", Integer.compare(3, 9));
        // WYNIK: Character.isDigit('7') → true
        // WYNIK: Integer.compare(3, 9) → -1    ← ujemne: pierwsza mniejsza
    }

    // =================================================================================================
    // 7. PUŁAPKA: remove(int) KONTRA remove(Object)
    // =================================================================================================

    /**
     * 7. Lista ma DWIE metody remove: remove(int index) usuwa element na POZYCJI, remove(Object o) usuwa element
     * o danej WARTOŚCI. Przy {@code List<Integer>} łatwo się pomylić.
     */
    static void listRemovePitfall() {
        section("7. List<Integer>: remove(1) kontra remove(Integer.valueOf(1))");

        List<Integer> byIndex = new ArrayList<>(List.of(10, 1, 20));
        byIndex.remove(1);                              // usuwa element o INDEKSIE 1 (czyli wartość 1 — przypadkiem)
        show("remove(1)", byIndex);
        // WYNIK: remove(1) → [10, 20]

        List<Integer> numbers = new ArrayList<>(List.of(5, 10, 15));
        numbers.remove(1);                              // indeks 1 → usuwa 10, a nie „liczbę 1”!
        show("[5, 10, 15].remove(1)", numbers);
        // WYNIK: [5, 10, 15].remove(1) → [5, 15]

        List<Integer> values = new ArrayList<>(List.of(5, 10, 15));
        values.remove(Integer.valueOf(15));             // usuwa WARTOŚĆ 15
        show("remove(Integer.valueOf(15))", values);
        // WYNIK: remove(Integer.valueOf(15)) → [5, 10]

        // PUŁAPKA: remove(15) na liście 3-elementowej → IndexOutOfBoundsException (nie ma indeksu 15).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • int ↔ Integer, long ↔ Long, double ↔ Double, boolean ↔ Boolean, char ↔ Character.
     *   • Autoboxing: Integer x = 5;  unboxing: int y = x;  (null → NullPointerException!).
     *   • Obiekty porównuj equals; == na Integer działa tylko „przypadkiem” dla -128..127 (cache).
     *   • Integer.parseInt("42") → int; Integer.valueOf("42") → Integer; zły tekst → NumberFormatException.
     *   • Integer 5 nie equals Long 5 (różne klasy).
     *   • List<Integer>.remove(1) usuwa INDEKS 1; wartość: remove(Integer.valueOf(1)).
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego nie można napisać List<int>?
     *   2. Co wypisze:  Integer a = 1000, b = 1000; System.out.println(a == b);  ?
     *   3. ZNAJDŹ BŁĄD:  Integer bonus = map.get("Jan");  int total = salary + bonus;   (gdy w mapie nie ma „Jan”)
     *   4. Co wypisze:  List<Integer> l = new ArrayList<>(List.of(3, 2, 1)); l.remove(2); System.out.println(l);  ?
     *   5. Czym różni się Integer.parseInt od Integer.valueOf?
     *   6. Co zwróci  Long.valueOf(7).equals(7)  i dlaczego?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: parseOrDefault(\"42\")", 42, () -> exercise1("42", -1));
        Check.equal("ćw. 1b: parseOrDefault(\"abc\")", -1, () -> exercise1("abc", -1));
        Check.equal("ćw. 2: liczba cyfr w \"a1b22c333\"", 6, () -> exercise2("a1b22c333"));
        Check.equal("ćw. 3: czy 1000 i 1000 są równe", true, () -> exercise3(1000, 1000));
        Check.equal("ćw. 4: usuń wszystkie trójki", List.of(1, 2), () -> exercise4(new ArrayList<>(List.of(3, 1, 3, 2)), 3));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", 42, () -> solution1("42", -1));
        Check.equal("ćw. 1b (wzorzec)", -1, () -> solution1("abc", -1));
        Check.equal("ćw. 2 (wzorzec)", 6, () -> solution2("a1b22c333"));
        Check.equal("ćw. 3 (wzorzec)", true, () -> solution3(1000, 1000));
        Check.equal("ćw. 4 (wzorzec)", List.of(1, 2), () -> solution4(new ArrayList<>(List.of(3, 1, 3, 2)), 3));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zamień tekst na int; gdy się nie da — zwróć wartość domyślną.
     * Podpowiedź (try/catch dokładnie w t10_exceptions):
     * <pre>{@code
     *   try { return Integer.parseInt(text); } catch (NumberFormatException e) { return defaultValue; }
     * }</pre>
     */
    static int exercise1(String text, int defaultValue) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (średnie): policz cyfry w napisie ("a1b22c333" → 6).
     * Podpowiedź: pętla po indeksach, {@code text.charAt(i)} i {@code Character.isDigit(...)}.
     */
    static int exercise2(String text) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć, czy dwie liczby Integer mają tę samą WARTOŚĆ (także dla dużych liczb, np. 1000).
     * Podpowiedź: nie ==. Użyj equals (a jeśli mogą być null — Objects.equals).
     */
    static boolean exercise3(Integer a, Integer b) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): usuń z listy WSZYSTKIE wystąpienia wartości {@code value} i zwróć listę.
     * [3, 1, 3, 2] bez trójek → [1, 2].
     * Podpowiedź: {@code list.remove(Integer.valueOf(value))} usuwa JEDNO wystąpienie i zwraca true, gdy coś usunęło —
     * powtarzaj w pętli while, dopóki zwraca true. Uwaga: remove(value) z int usunąłby INDEKS!
     */
    static List<Integer> exercise4(List<Integer> list, int value) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(String text, int defaultValue) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    static int solution2(String text) {
        int digits = 0;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) {
                digits++;
            }
        }
        return digits;
    }

    static boolean solution3(Integer a, Integer b) {
        return a.equals(b);
    }

    static List<Integer> solution4(List<Integer> list, int value) {
        while (list.remove(Integer.valueOf(value))) {
            // pusta pętla: samo remove robi robotę; kręcimy, dopóki coś się usuwa
        }
        return list;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Kolekcje (i typy generyczne) przechowują tylko obiekty, a int jest typem prostym. Stąd List<Integer>.
     *   2. false — 1000 jest poza cache (-128..127), więc a i b to dwa różne obiekty. Porównuj equals.
     *   3. map.get zwraca null dla brakującego klucza; salary + bonus rozpakowuje null → NullPointerException.
     *      Poprawnie np.: int bonus = map.getOrDefault("Jan", 0);
     *   4. [3, 2] — remove(2) usuwa element o INDEKSIE 2 (wartość 1).
     *   5. parseInt zwraca typ prosty int, valueOf zwraca obiekt Integer (z cache dla małych liczb).
     *   6. false — 7 zostaje opakowane w Integer, a Long.equals zwraca true tylko dla obiektów Long.
     */
    // </editor-fold>
}
