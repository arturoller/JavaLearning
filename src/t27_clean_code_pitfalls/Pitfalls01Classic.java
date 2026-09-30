package t27_clean_code_pitfalls;

import helpers.Check;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Klasyczne pułapki Javy — katalog 22 zaskoczeń
 *        (pitfall = pułapka; surprising result = zaskakujący wynik)
 *
 * W SKRÓCIE:
 *   Java ma kilkanaście zachowań, które WYGLĄDAJĄ oczywiście, a działają inaczej, niż podpowiada intuicja —
 *   nawet doświadczonym programistom. Ta lekcja to katalog: każda pułapka to mały, samodzielny pokaz (kod +
 *   zaskakujący wynik + jedno zdanie „dlaczego”). Celem NIE jest zapamiętanie 22 faktów na pamięć, tylko
 *   wyrobienie ODRUCHU: „to miejsce w kodzie WYGLĄDA niewinnie — czy to jedna z tych pułapek?”.
 *
 * ANALOGIA: fałszywe przyjacielskie słowa (false friends) w obcym języku.
 *   Angielskie „eventually” nie znaczy „ewentualnie”, tylko „w końcu”. Wygląda znajomo, ale prowadzi do błędnego
 *   tłumaczenia. Kod Javy ma swoje „false friends”: == wygląda jak porównanie wartości, remove(1) wygląda jak
 *   usunięcie liczby 1, equals na BigDecimal wygląda jak porównanie wartości — a działają inaczej.
 *
 * JAK TO DZIAŁA:
 *   Każdy numerowany pokaz ma tę samą strukturę: 2-4 linie kodu → show()/expectThrows() z wynikiem → jedno zdanie
 *   „dlaczego”, zaczynające się od komentarza `// dlaczego:`. Pułapki są pogrupowane tematycznie (porównania,
 *   arytmetyka, sterowanie, kolekcje, BigDecimal, napisy/Locale, Optional/Stream) — nie muszą być czytane po kolei.
 *
 * SŁÓWKA:
 *   autoboxing = automatyczne pakowanie (int → Integer); unboxing = rozpakowanie (Integer → int); cache = pamięć
 *   podręczna (tu: pula gotowych obiektów Integer); overload = przeciążenie (dwie metody o tej samej nazwie,
 *   różne parametry); fall-through = "przeciekanie" (switch bez break leci do kolejnego case); off-by-one =
 *   pomyłka o jeden (indeks za mało/za dużo); mutable = zmienny (zmienny po utworzeniu).
 *
 * ZOBACZ TEŻ: t27_clean_code_pitfalls/Pitfalls02CodeReview (kolejne typowe błędy, tym razem z PRZED/PO),
 *             t15_numbers/Numbers01BigDecimal (BigDecimal szczegółowo), t14_optional/Optional01Basics (Optional),
 *             t12_collections/Collections03IterationModification (ConcurrentModificationException).
 * </pre>
 */
public class Pitfalls01Classic {

    public static void main(String[] args) {
        title("Pitfalls01 — klasyczne pułapki Javy");

        comparisons();            // comparisons = porównania
        arithmetic();               // arithmetic = arytmetyka
        controlFlow();              // control flow = sterowanie przepływem
        collectionsPitfalls();     // collections pitfalls = pułapki kolekcji
        bigDecimalPitfalls();      // BigDecimal pitfalls = pułapki BigDecimal
        stringsAndLocale();        // strings and locale = napisy i lokalizacja
        optionalAndStream();       // Optional and Stream = Optional i strumień
        exercises();                 // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PORÓWNANIA
    // =================================================================================================

    static void comparisons() {
        section("1. Porównania: == na String, cache Integer");

        // 1. == na String
        String a = new String("java");   // celowo new String(...) — omija string pool
        String b = "java";
        show("a == b (new String vs literał)", a == b);
        show("a.equals(b)", a.equals(b));
        // WYNIK: a == b (new String vs literał) → false
        // WYNIK: a.equals(b) → true
        // dlaczego: == porównuje REFERENCJE (adresy), nie treść. Literały trafiają do wspólnej puli (string pool),
        //   ale `new String(...)` ZAWSZE tworzy nowy obiekt POZA pulą. Do treści zawsze equals, nigdy ==.

        line();

        // 2. cache Integer (autoboxing -128..127)
        Integer x1 = 127, x2 = 127;
        Integer y1 = 128, y2 = 128;
        show("Integer 127 == 127", x1 == x2);
        show("Integer 128 == 128", y1 == y2);
        // WYNIK: Integer 127 == 127 → true
        // WYNIK: Integer 128 == 128 → false
        // dlaczego: autoboxing dla wartości -128..127 korzysta z WEWNĘTRZNEGO cache'a (Integer.valueOf) i zwraca
        //   TEN SAM obiekt. Poza tym zakresem powstają różne obiekty. Do wartości Integer używaj equals.
    }

    // =================================================================================================
    // 2. ARYTMETYKA
    // =================================================================================================

    static void arithmetic() {
        section("2. Arytmetyka: ułamki, dzielenie, przepełnienie, char+int, modulo");

        // 3. 0.1 + 0.2
        show("0.1 + 0.2", 0.1 + 0.2);
        show("0.1 + 0.2 == 0.3", 0.1 + 0.2 == 0.3);
        // WYNIK: 0.1 + 0.2 → 0.30000000000000004
        // WYNIK: 0.1 + 0.2 == 0.3 → false
        // dlaczego: double to binarne PRZYBLIŻENIE (IEEE-754) — 0.1 nie da się zapisać dokładnie dwójkowo.
        //   Do pieniędzy używaj BigDecimal (t15_numbers/Numbers01BigDecimal), nigdy double.

        line();

        // 4. dzielenie całkowitoliczbowe
        show("7 / 2", 7 / 2);
        show("7 / 2.0", 7 / 2.0);
        // WYNIK: 7 / 2 → 3
        // WYNIK: 7 / 2.0 → 3.5
        // dlaczego: int / int jest CAŁKOWITOLICZBOWE (ucina resztę, bez zaokrąglania) — żeby dostać ułamek,
        //   rzutuj choć JEDEN operand na double.

        line();

        // 5. przepełnienie int
        show("Integer.MAX_VALUE + 1", Integer.MAX_VALUE + 1);
        // WYNIK: Integer.MAX_VALUE + 1 → -2147483648
        // dlaczego: int ma STAŁY rozmiar (32 bity) i "zawija się" (wrap-around) BEZ WYJĄTKU. Do wykrycia
        //   przepełnienia użyj Math.addExact (t15_numbers/Numbers05IntegerTricks).

        line();

        // 6. char + int
        char c = 'a';
        show("'a' + 1", c + 1);
        show("(char) ('a' + 1)", (char) (c + 1));
        // WYNIK: 'a' + 1 → 98
        // WYNIK: (char) ('a' + 1) → b
        // dlaczego: char + int PROMUJE char do int (kod znaku 'a' to 97) — wynik jest LICZBĄ, nie znakiem;
        //   trzeba jawnie rzutować z powrotem na char.

        line();

        // 16. Math.abs(Integer.MIN_VALUE)
        show("Math.abs(Integer.MIN_VALUE)", Math.abs(Integer.MIN_VALUE));
        // WYNIK: Math.abs(Integer.MIN_VALUE) → -2147483648
        // dlaczego: zakres int jest ASYMETRYCZNY (-2147483648..2147483647) — dodatniego odpowiednika MIN_VALUE
        //   nie da się zmieścić w int, więc Math.abs go NIE ZMIENIA (ciche przepełnienie). Math.absExact
        //   (Java 15+) rzuci ArithmeticException zamiast tego.

        line();

        // 17. -7 % 3
        show("-7 % 3", -7 % 3);
        show("Math.floorMod(-7, 3)", Math.floorMod(-7, 3));
        // WYNIK: -7 % 3 → -1
        // WYNIK: Math.floorMod(-7, 3) → 2
        // dlaczego: % w Javie to reszta ZE ZNAKIEM DZIELNEJ (nie matematyczny modulo) — dla ujemnych liczb
        //   wynik bywa ujemny. Gdy potrzebujesz nieujemnego wyniku, użyj Math.floorMod.
    }

    // =================================================================================================
    // 3. STEROWANIE PRZEPŁYWEM
    // =================================================================================================

    static void controlFlow() {
        section("3. Sterowanie: switch bez break, ternary + autounboxing");

        // 7. switch fall-through (przeciekanie) bez break
        int day = 3;
        StringBuilder result = new StringBuilder();
        switch (day) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                result.append("dzień roboczy");
            case 6:
                result.append(" (a tu przeciek!)");
                break;
            default:
                result.append("weekend");
        }
        show("switch bez break dla dnia 3", result.toString());
        // WYNIK: switch bez break dla dnia 3 → dzień roboczy (a tu przeciek!)
        // dlaczego: bez break wykonanie "przecieka" (fall-through) do KOLEJNEGO case, aż napotka break albo koniec.
        //   Nowoczesny switch EXPRESSION z -> (Java 14+, t02_controlflow/Control02Switch) tego problemu nie ma.

        line();

        // 8. ternary + autounboxing → zaskakujący NPE
        Integer maybeNull = null;
        boolean flag = true;
        expectThrows("Integer wynik = flag ? maybeNull : 0", () -> {
            Integer wynik = flag ? maybeNull : 0;
        });
        // WYNIK: ✔ Integer wynik = flag ? maybeNull : 0 → rzucono NullPointerException: Cannot invoke "java.lang.Integer.intValue()" because "maybeNull" is null

        Integer safe = flag ? null : 0;   // null jako LITERAŁ (nie zmienna) → wynik to zwykły Integer, bez unboxingu
        show("Integer safe = flag ? null : 0 (literał null)", safe);
        // WYNIK: Integer safe = flag ? null : 0 (literał null) → null
        // dlaczego: skoro DRUGI operand (0) jest typu int, kompilator stosuje promocję liczbową dla CAŁEGO
        //   wyrażenia warunkowego — maybeNull jest ODPAKOWYWANY (unboxing), mimo że przypisujesz do Integer!
        //   Literał null (bez zmiennej pośredniej) tego problemu nie ma — wtedy wynikiem jest zwykły Integer.
    }

    // =================================================================================================
    // 4. KOLEKCJE
    // =================================================================================================

    /** PointNoEquals = celowo BEZ equals/hashCode — pokazuje pułapkę 12 (porównanie przez tożsamość w HashSet). */
    static class PointNoEquals {
        final int x;
        final int y;

        PointNoEquals(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /** MutableKey = celowo ZMIENNY klucz mapy (antywzorzec) — pokazuje pułapkę 13. */
    static class MutableKey {
        int value;

        MutableKey(int value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof MutableKey other && other.value == value;
        }

        @Override
        public int hashCode() {
            return value;
        }
    }

    static void collectionsPitfalls() {
        section("4. Kolekcje: remove(int), Arrays.asList, CME, equals/hashCode, zmienny klucz");

        // 9. List<Integer>.remove(int) vs remove(Object)
        List<Integer> numbers = new ArrayList<>(List.of(10, 20, 30));
        numbers.remove(1);                       // remove(int index) — usuwa ELEMENT NA INDEKSIE 1, czyli 20!
        show("po remove(1) — to jest INDEKS", numbers);
        List<Integer> numbers2 = new ArrayList<>(List.of(10, 20, 30));
        numbers2.remove(Integer.valueOf(1));     // remove(Object) — usuwa WARTOŚĆ 1 (nie ma jej — bez zmian)
        show("po remove(Integer.valueOf(1)) — to jest WARTOŚĆ", numbers2);
        // WYNIK: po remove(1) — to jest INDEKS → [10, 30]
        // WYNIK: po remove(Integer.valueOf(1)) — to jest WARTOŚĆ → [10, 20, 30]
        // dlaczego: List<Integer> ma DWA przeciążenia: remove(int index) i remove(Object o). Literał 1 wybiera
        //   przeciążenie z indeksem — żeby usunąć WARTOŚĆ, opakuj ją: remove(Integer.valueOf(1)).

        line();

        // 10. Arrays.asList(...).add — stały rozmiar
        List<Integer> fixed = Arrays.asList(1, 2, 3);
        expectThrows("Arrays.asList(1, 2, 3).add(4)", () -> fixed.add(4));
        // WYNIK: ✔ Arrays.asList(1, 2, 3).add(4) → rzucono UnsupportedOperationException: (brak komunikatu)
        // dlaczego: Arrays.asList zwraca listę o STAŁYM rozmiarze (widok na tablicę) — set(i, x) działa, add/remove nie.

        line();

        // 11. ConcurrentModificationException
        List<Integer> toClean = new ArrayList<>(List.of(1, 2, 3));
        expectThrows("usuwanie elementu w pętli for-each", () -> {
            for (Integer n : toClean) {
                if (n == 1) {
                    toClean.remove(n);
                }
            }
        });
        // WYNIK: ✔ usuwanie elementu w pętli for-each → rzucono ConcurrentModificationException: (brak komunikatu)
        // dlaczego: for-each korzysta z Iteratora, który wykrywa modyfikację listy "obok" niego. Użyj
        //   iterator.remove() albo list.removeIf(...) (t12_collections/Collections03IterationModification).

        line();

        // 12. HashSet bez equals/hashCode
        Set<PointNoEquals> points = new HashSet<>();
        points.add(new PointNoEquals(1, 1));
        points.add(new PointNoEquals(1, 1));    // "ten sam" punkt, ale INNY obiekt
        show("liczba punktów w HashSet (bez equals/hashCode)", points.size());
        // WYNIK: liczba punktów w HashSet (bez equals/hashCode) → 2
        // dlaczego: bez equals/hashCode HashSet porównuje przez TOŻSAMOŚĆ (odziedziczone Object.equals). Dwa
        //   punkty o tych samych współrzędnych to dla niego RÓŻNE obiekty. Rekordy generują to automatycznie (t09_records).

        line();

        // 13. zmienny klucz HashMap
        Map<MutableKey, String> map = new HashMap<>();
        MutableKey key = new MutableKey(1);
        map.put(key, "dane");
        show("map.get(key) PRZED zmianą", map.get(key));
        key.value = 2;   // zmieniamy pole klucza PO włożeniu do mapy — antywzorzec
        show("map.get(key) PO zmianie value", map.get(key));
        show("map.containsKey(key) PO zmianie", map.containsKey(key));
        // WYNIK: map.get(key) PRZED zmianą → dane
        // WYNIK: map.get(key) PO zmianie value → null
        // WYNIK: map.containsKey(key) PO zmianie → false
        // dlaczego: HashMap oblicza kubełek (bucket) na podstawie hashCode() W MOMENCIE wstawienia. Zmiana pola
        //   po włożeniu "gubi" wpis — jest w mapie, ale pod STARYM kubełkiem. Klucze mapy powinny być NIEZMIENNE.
    }

    // =================================================================================================
    // 5. BigDecimal
    // =================================================================================================

    static void bigDecimalPitfalls() {
        section("5. BigDecimal: equals vs compareTo, konstruktor z double");

        // 14. equals a skala
        BigDecimal twoZero = new BigDecimal("2.0");
        BigDecimal twoDoubleZero = new BigDecimal("2.00");
        show("twoZero.equals(twoDoubleZero) (2.0 vs 2.00)", twoZero.equals(twoDoubleZero));
        show("twoZero.compareTo(twoDoubleZero) == 0", twoZero.compareTo(twoDoubleZero) == 0);
        // WYNIK: twoZero.equals(twoDoubleZero) (2.0 vs 2.00) → false
        // WYNIK: twoZero.compareTo(twoDoubleZero) == 0 → true
        // dlaczego: equals porównuje WARTOŚĆ I SKALĘ (liczbę cyfr po przecinku) — 2.0 i 2.00 mają różną skalę.
        //   Do porównania samej wartości używaj compareTo (t15_numbers/Numbers01BigDecimal).

        line();

        // 15. BigDecimal z double
        BigDecimal fromDouble = new BigDecimal(0.1);
        BigDecimal fromString = new BigDecimal("0.1");
        show("new BigDecimal(0.1).toPlainString()", fromDouble.toPlainString());
        show("new BigDecimal(\"0.1\")", fromString);
        // WYNIK: new BigDecimal(0.1).toPlainString() → 0.1000000000000000055511151231257827021181583404541015625
        // WYNIK: new BigDecimal("0.1") → 0.1
        // dlaczego: konstruktor z double przenosi CAŁE binarne przybliżenie 0.1 (patrz pułapka 3). Do pieniędzy
        //   twórz BigDecimal Z NAPISU (albo BigDecimal.valueOf(double), które używa Double.toString w środku).
    }

    // =================================================================================================
    // 6. NAPISY I LOCALE
    // =================================================================================================

    static void stringsAndLocale() {
        section("6. Napisy i Locale: zignorowany wynik, turecka lokalizacja, off-by-one");

        // 18. zignorowany wynik toUpperCase
        String s = "abc";
        s.toUpperCase();     // wynik ZIGNOROWANY — String jest niezmienny, oryginał się NIE zmienia
        show("s po (zignorowanym) s.toUpperCase()", s);
        // WYNIK: s po (zignorowanym) s.toUpperCase() → abc
        // dlaczego: String jest NIEZMIENNY — toUpperCase() zwraca NOWY napis. Trzeba przypisać wynik:
        //   s = s.toUpperCase();

        line();

        // 21. turecka lokalizacja — bezkropkowe ı
        String title = "TITLE";
        show("\"TITLE\".toLowerCase(new Locale(\"tr\"))", title.toLowerCase(new Locale("tr")));
        show("\"TITLE\".toLowerCase(Locale.forLanguageTag(\"tr\"))", title.toLowerCase(Locale.forLanguageTag("tr")));
        show("\"TITLE\".toLowerCase(Locale.ROOT)", title.toLowerCase(Locale.ROOT));
        // WYNIK: "TITLE".toLowerCase(new Locale("tr")) → tıtle
        // WYNIK: "TITLE".toLowerCase(Locale.forLanguageTag("tr")) → tıtle
        // WYNIK: "TITLE".toLowerCase(Locale.ROOT) → title
        // dlaczego: w tureckim 'I' zamienia się na BEZKROPKOWE 'ı', nie na zwykłe 'i' — to nie błąd konstrukcji
        //   Locale (forLanguageTag daje TEN SAM wynik co new Locale, bo to ta sama lokalizacja), tylko realna
        //   reguła językowa. Do porównań/kluczy TECHNICZNYCH (nie tekstu dla użytkownika) używaj Locale.ROOT.

        line();

        // 22. substring/indexOf off-by-one
        String text = "Hello, World!";
        show("text.indexOf(\"World\")", text.indexOf("World"));
        show("text.substring(7, 12)", text.substring(7, 12));
        expectThrows("text.substring(20) — poza zakresem", () -> text.substring(20));
        // WYNIK: text.indexOf("World") → 7
        // WYNIK: text.substring(7, 12) → World
        // WYNIK: ✔ text.substring(20) — poza zakresem → rzucono StringIndexOutOfBoundsException: begin 20, end 13, length 13
        // dlaczego: substring(from, to) — `to` jest WYŁĄCZNIE (exclusive), nie ostatnim WŁĄCZONYM indeksem —
        //   klasyczny błąd to policzenie "to" jako ostatni znak zamiast znak+1. indexOf zwraca -1 (nie wyjątek),
        //   gdy nic nie znajdzie.
    }

    // =================================================================================================
    // 7. Optional I STREAM
    // =================================================================================================

    static void optionalAndStream() {
        section("7. Optional i Stream: get() na pustym, ponowne użycie strumienia");

        // 19. Optional.get() na pustym
        Optional<String> empty = Optional.empty();
        expectThrows("empty.get()", empty::get);
        // WYNIK: ✔ empty.get() → rzucono NoSuchElementException: No value present
        // dlaczego: get() bez sprawdzenia rzuca wyjątek, gdy Optional jest pusty. Używaj orElse/orElseGet/
        //   orElseThrow(dostawca)/ifPresent (t14_optional/Optional01Basics).

        line();

        // 20. ponowne użycie strumienia
        Stream<Integer> stream = Stream.of(1, 2, 3);
        show("stream.count()", stream.count());
        expectThrows("ponowne użycie TEGO SAMEGO strumienia", stream::count);
        // WYNIK: stream.count() → 3
        // WYNIK: ✔ ponowne użycie TEGO SAMEGO strumienia → rzucono IllegalStateException: stream has already been operated upon or closed
        // dlaczego: Stream można SKONSUMOWAĆ tylko raz — po operacji terminalnej jest zamknięty. Żeby przetworzyć
        //   dane ponownie, zbuduj NOWY strumień (np. ponownie z tej samej kolekcji).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • == na obiektach (String, Integer) porównuje REFERENCJE — do wartości zawsze equals.
     *   • double to przybliżenie — do pieniędzy BigDecimal Z NAPISU, nie double.
     *   • int/int ucina resztę; int się przepełnia bez wyjątku (Math.addExact wykrywa).
     *   • switch bez break "przecieka"; ternary z mieszanym Integer/int może odpakować null → NPE.
     *   • List<Integer>.remove(int) to INDEKS; Arrays.asList ma stały rozmiar; modyfikacja w for-each → CME.
     *   • equals/hashCode wymagane do sensownego HashSet/HashMap; klucze mapy muszą być niezmienne.
     *   • BigDecimal.equals patrzy na skalę (compareTo — na wartość); BigDecimal(double) przenosi błąd double.
     *   • Locale wpływa na wielkość liter (tr!) — Locale.ROOT dla porównań technicznych.
     *   • Optional.get() i ponowne użycie Stream rzucają wyjątki — sprawdzaj/konsumuj raz.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego `new String("x") == "x"` to false, a `"x" == "x"` to true?
     *   2. Co wypisze:  Integer a = 100, b = 100; System.out.println(a == b);  a co dla 200 i 200?
     *   3. ZNAJDŹ BŁĄD:  List<Integer> l = new ArrayList<>(List.of(5, 10, 15)); l.remove(10);
     *      (programista chciał usunąć WARTOŚĆ 10)
     *   4. Co wypisze:  System.out.println(new BigDecimal("3.0").equals(new BigDecimal("3.00")));
     *   5. Dlaczego `Integer wynik = flag ? maybeNullInteger : 0;` może rzucić NullPointerException, mimo że
     *      przypisujesz do zmiennej typu Integer (nie int)?
     *   6. Dlaczego iterowanie po liście for-each i wywołanie list.remove(x) w środku pętli kończy się wyjątkiem?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");

        Check.equal("ćw. 1: average(7, 2)", 4.5, () -> exercise1(7, 2));
        Check.equal("ćw. 2a: removeValue usuwa wartość 20", List.of(10, 30), () -> {
            List<Integer> list = new ArrayList<>(List.of(10, 20, 30));
            exercise2(list, 20);
            return list;
        });
        Check.equal("ćw. 2b: removeValue — brak wartości, lista bez zmian", List.of(10, 20, 30), () -> {
            List<Integer> list = new ArrayList<>(List.of(10, 20, 30));
            exercise2(list, 99);
            return list;
        });
        Check.equal("ćw. 3a: dayType(3)", "dzień roboczy", () -> exercise3(3));
        Check.equal("ćw. 3b: dayType(6)", "weekend", () -> exercise3(6));
        Check.equal("ćw. 3c: dayType(0)", "nieznany dzień", () -> exercise3(0));
        Check.equal("ćw. 4a: bigDecimalEquals 2.0 vs 2.00", true, () -> exercise4("2.0", "2.00"));
        Check.equal("ćw. 4b: bigDecimalEquals 2.0 vs 3.0", false, () -> exercise4("2.0", "3.0"));
        Check.equal("ćw. 5a: safeTernary(null, true)", null, () -> exercise5(null, true));
        Check.equal("ćw. 5b: safeTernary(null, false)", 0, () -> exercise5(null, false));
        Check.equal("ćw. 5c: safeTernary(7, true)", 7, () -> exercise5(7, true));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 4.5, () -> solution1(7, 2));
        Check.equal("ćw. 2a (wzorzec)", List.of(10, 30), () -> {
            List<Integer> list = new ArrayList<>(List.of(10, 20, 30));
            solution2(list, 20);
            return list;
        });
        Check.equal("ćw. 2b (wzorzec)", List.of(10, 20, 30), () -> {
            List<Integer> list = new ArrayList<>(List.of(10, 20, 30));
            solution2(list, 99);
            return list;
        });
        Check.equal("ćw. 3a (wzorzec)", "dzień roboczy", () -> solution3(3));
        Check.equal("ćw. 3b (wzorzec)", "weekend", () -> solution3(6));
        Check.equal("ćw. 3c (wzorzec)", "nieznany dzień", () -> solution3(0));
        Check.equal("ćw. 4a (wzorzec)", true, () -> solution4("2.0", "2.00"));
        Check.equal("ćw. 4b (wzorzec)", false, () -> solution4("2.0", "3.0"));
        Check.equal("ćw. 5a (wzorzec)", null, () -> solution5(null, true));
        Check.equal("ćw. 5b (wzorzec)", 0, () -> solution5(null, false));
        Check.equal("ćw. 5c (wzorzec)", 7, () -> solution5(7, true));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 11 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): popraw pułapkę 4 (dzielenie całkowitoliczbowe) — zwróć DOKŁADNĄ średnią dwóch int jako double.
     */
    static double exercise1(int a, int b) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (łatwe): popraw pułapkę 9 — usuń z listy element o podanej WARTOŚCI (nie indeksie).
     * Jeśli wartości nie ma na liście, nic nie rób.
     */
    static void exercise2(List<Integer> list, int value) {
        // TODO: twoje rozwiązanie
    }

    /**
     * ĆWICZENIE 3 (średnie, „PRZEPISZ”): stary switch z pułapki 7 ukrywał błąd fall-through:
     * <pre>{@code
     * switch (day) {
     *     case 1: case 2: case 3: case 4: case 5:
     *         result = "dzień roboczy";
     *     case 6: case 7:
     *         result = "weekend";
     * }
     * }</pre>
     * Przepisz jako switch EXPRESSION z -> (Java 14+, bez fall-through): 1-5 → "dzień roboczy", 6-7 → "weekend",
     * inne wartości → "nieznany dzień".
     */
    static String exercise3(int day) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (średnie): popraw pułapkę 14 — porównaj DWA napisy jako BigDecimal WARTOŚCIOWO (ignorując skalę).
     */
    static boolean exercise4(String a, String b) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): popraw pułapkę 8 — zwróć maybeNull, gdy flag == true (NAWET jeśli maybeNull to
     * null), albo 0, gdy flag == false — BEZ rzucania NullPointerException w żadnym przypadku.
     * Podpowiedź: unikaj mieszania Integer z prymitywnym int w tym samym wyrażeniu warunkowym.
     */
    static Integer exercise5(Integer maybeNull, boolean flag) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static double solution1(int a, int b) {
        return (a + b) / 2.0;
    }

    static void solution2(List<Integer> list, int value) {
        list.remove(Integer.valueOf(value));
    }

    static String solution3(int day) {
        return switch (day) {
            case 1, 2, 3, 4, 5 -> "dzień roboczy";
            case 6, 7 -> "weekend";
            default -> "nieznany dzień";
        };
    }

    static boolean solution4(String a, String b) {
        return new BigDecimal(a).compareTo(new BigDecimal(b)) == 0;
    }

    static Integer solution5(Integer maybeNull, boolean flag) {
        return flag ? maybeNull : Integer.valueOf(0);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. `new String("x")` tworzy NOWY obiekt poza pulą stałych — inna referencja niż literał "x" (który
     *      zawsze wskazuje na TEN SAM obiekt w string pool). == porównuje referencje, więc pierwsze to false.
     *   2. Dla 100/100: true (autoboxing korzysta z cache'a -128..127). Dla 200/200: false (poza zakresem cache'a
     *      powstają różne obiekty Integer).
     *   3. l.remove(10) wybiera przeciążenie remove(int index) — usuwa element NA INDEKSIE 10, co rzuci
     *      IndexOutOfBoundsException (lista ma 3 elementy). Trzeba: l.remove(Integer.valueOf(10)).
     *   4. false — equals porównuje też SKALĘ (3.0 ma skalę 1, 3.00 ma skalę 2). compareTo dałoby true.
     *   5. Bo drugi operand ternary (0) jest typu int — kompilator stosuje promocję liczbową do CAŁEGO wyrażenia,
     *      więc maybeNullInteger jest odpakowywany (unboxing) niezależnie od typu zmiennej, do której przypisujesz.
     *   6. for-each korzysta z Iteratora, który przy KAŻDYM next() sprawdza, czy lista nie została zmieniona
     *      "obok niego" (modCount) — bezpośrednie list.remove(x) w pętli tę zgodność psuje i rzuca CME.
     */
    // </editor-fold>
}
