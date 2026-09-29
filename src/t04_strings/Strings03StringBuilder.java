package t04_strings;

import helpers.Check;

import java.util.StringJoiner;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: StringBuilder i StringJoiner — wydajne składanie napisów
 *        (builder = budowniczy; append = doklej; joiner = łącznik; mutable = zmienny)
 *
 * W SKRÓCIE:
 *   String jest niezmienny, więc każde  text += "x"  tworzy NOWY napis i kopiuje cały dotychczasowy tekst.
 *   W pętli z tysiącami obrotów to setki tysięcy niepotrzebnych kopii. StringBuilder to ZMIENNY bufor tekstu:
 *   dokleja (append) w miejscu, a na końcu toString() daje gotowy String. StringJoiner skleja elementy separatorem.
 *
 * ANALOGIA: pisanie listu.
 *   String += to przepisywanie całego listu od nowa na czystej kartce za każdym razem, gdy chcesz dopisać słowo.
 *   StringBuilder to pisanie na jednej kartce — po prostu dopisujesz na końcu, a gotowy list oddajesz na koniec.
 *
 * JAK TO DZIAŁA:
 *   StringBuilder sb = new StringBuilder();
 *   sb.append("Ala").append(' ').append(7);   ← append zwraca TEN SAM obiekt, więc można łączyć w łańcuch
 *   String result = sb.toString();            ← "Ala 7"
 *   Inne: insert(pozycja, x), reverse(), deleteCharAt(i), setCharAt(i, c), setLength(n), length()
 *
 * SŁÓWKA:
 *   builder = budowniczy; append = doklej (na końcu); insert = wstaw; reverse = odwróć; delete = usuń;
 *   set length = ustaw długość (skróć); joiner = łącznik; prefix / suffix = przedrostek / przyrostek;
 *   mutable = zmienny; synchronized = synchronizowany (bezpieczny dla wielu wątków).
 *
 * ZOBACZ TEŻ: t04_strings/Strings01Basics (niezmienność String), t04_strings/Strings07TextAlgorithms (algorytmy na tekście),
 *             t16_streams/Streams09CollectorsBasic (Collectors.joining — to samo dla strumieni).
 * </pre>
 */
public class Strings03StringBuilder {

    public static void main(String[] args) {
        title("Strings03 — StringBuilder i StringJoiner");

        whyBuilder();           // why builder = po co StringBuilder
        builderBasics();        // builder basics = podstawy StringBuildera
        editingInPlace();       // editing in place = edycja w miejscu
        trailingSeparator();    // trailing separator = separator na końcu
        joiner();               // joiner = łącznik
        builderEqualsPitfall(); // builder equals pitfall = pułapka equals
        whenPlusIsFine();       // when plus is fine = kiedy + wystarczy
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PO CO StringBuilder
    // =================================================================================================

    /**
     * 1. Oba sposoby dają ten sam napis. Różnica jest „w środku”: += w pętli tworzy nowy String w KAŻDYM obrocie
     * (i kopiuje cały dotychczasowy tekst), StringBuilder dokleja do jednego bufora.
     */
    static void whyBuilder() {
        section("1. += w pętli kontra StringBuilder");

        String slow = "";
        for (int i = 1; i <= 5; i++) {
            slow += i;                          // 5 obrotów = 5 nowych obiektów String
        }
        StringBuilder fast = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            fast.append(i);                     // jeden bufor, dopisywanie na końcu
        }
        show("+= w pętli", slow);
        show("StringBuilder", fast.toString());
        // WYNIK: += w pętli → 12345
        // WYNIK: StringBuilder → 12345

        // Przy 5 obrotach różnicy nie odczujesz. Przy 100 000 obrotów += kopiuje łącznie miliardy znaków
        //   (czas rośnie z kwadratem liczby obrotów), a StringBuilder — tylko tyle, ile trzeba.
        // DOBRA PRAKTYKA: sklejanie napisów W PĘTLI → StringBuilder.
    }

    // =================================================================================================
    // 2. PODSTAWY
    // =================================================================================================

    /** 2. append przyjmuje wszystko (napisy, liczby, znaki, obiekty) i zwraca ten sam builder — stąd łańcuchy wywołań. */
    static void builderBasics() {
        section("2. append, łańcuch wywołań, toString, length");

        StringBuilder sb = new StringBuilder("Zamówienie ");
        sb.append("ZAM-").append(7).append(": ").append(3).append(" szt.");
        show("toString()", sb.toString());
        show("length()", sb.length());
        // WYNIK: toString() → Zamówienie ZAM-7: 3 szt.
        // WYNIK: length() → 24
    }

    // =================================================================================================
    // 3. EDYCJA W MIEJSCU
    // =================================================================================================

    /** 3. StringBuilder jest ZMIENNY — metody zmieniają ten sam obiekt (inaczej niż String!). */
    static void editingInPlace() {
        section("3. insert, reverse, deleteCharAt, setCharAt");

        StringBuilder sb = new StringBuilder("Java");
        sb.insert(0, "Kocham ");                // insert = wstaw na pozycji
        show("insert(0, ...)", sb);
        sb.setCharAt(0, 'k');                   // setCharAt = ustaw znak na pozycji
        show("setCharAt(0, 'k')", sb);
        sb.deleteCharAt(sb.length() - 1);       // deleteCharAt = usuń znak (tu ostatni)
        show("deleteCharAt(ostatni)", sb);
        show("reverse()", new StringBuilder("kajak").reverse());
        // WYNIK: insert(0, ...) → Kocham Java
        // WYNIK: setCharAt(0, 'k') → kocham Java
        // WYNIK: deleteCharAt(ostatni) → kocham Jav
        // WYNIK: reverse() → kajak
    }

    // =================================================================================================
    // 4. SEPARATOR NA KOŃCU
    // =================================================================================================

    /** 4. Klasyczny problem: „a, b, c, ” — przecinek za dużo. Rozwiązania: dopisuj separator PRZED elementem albo skróć na końcu. */
    static void trailingSeparator() {
        section("4. Separator bez „wiszącego” przecinka");

        String[] items = {"chleb", "masło", "ser"};

        StringBuilder bad = new StringBuilder();
        for (String item : items) {
            bad.append(item).append(", ");
        }
        show("źle", "[" + bad + "]");
        // WYNIK: źle → [chleb, masło, ser, ]

        StringBuilder good = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            if (i > 0) {
                good.append(", ");              // separator PRZED każdym elementem oprócz pierwszego
            }
            good.append(items[i]);
        }
        show("dobrze", "[" + good + "]");
        // WYNIK: dobrze → [chleb, masło, ser]
        // Jeszcze prościej: String.join(", ", items) albo StringJoiner (sekcja 5).
    }

    // =================================================================================================
    // 5. StringJoiner
    // =================================================================================================

    /** 5. StringJoiner (łącznik) sam pilnuje separatorów; opcjonalnie dodaje przedrostek i przyrostek. */
    static void joiner() {
        section("5. StringJoiner i String.join");

        StringJoiner joiner = new StringJoiner(", ", "[", "]");    // separator, przedrostek, przyrostek
        joiner.add("chleb").add("masło").add("ser");
        show("StringJoiner", joiner);
        show("pusty StringJoiner", new StringJoiner(", ", "[", "]"));
        show("String.join", String.join(" | ", "a", "b", "c"));
        // WYNIK: StringJoiner → [chleb, masło, ser]
        // WYNIK: pusty StringJoiner → []
        // WYNIK: String.join → a | b | c
    }

    // =================================================================================================
    // 6. PUŁAPKA: equals NA StringBuilder
    // =================================================================================================

    /**
     * 6. StringBuilder NIE nadpisuje equals — porównuje obiekty (jak ==), a nie treść. Treść porównasz przez
     * toString().equals(...) albo contentEquals.
     */
    static void builderEqualsPitfall() {
        section("6. Pułapka: equals na StringBuilder porównuje obiekty");

        StringBuilder a = new StringBuilder("abc");
        StringBuilder b = new StringBuilder("abc");
        show("a.equals(b)", a.equals(b));
        show("a.toString().equals(b.toString())", a.toString().equals(b.toString()));
        show("\"abc\".contentEquals(a)", "abc".contentEquals(a));
        // WYNIK: a.equals(b) → false    ← mimo tej samej treści!
        // WYNIK: a.toString().equals(b.toString()) → true
        // WYNIK: "abc".contentEquals(a) → true

        // StringBuffer to starsza, „synchronizowana” wersja StringBuildera (bezpieczna dla wielu wątków, ale wolniejsza).
        //   W zwykłym kodzie używaj StringBuilder.
    }

    // =================================================================================================
    // 7. KIEDY ZWYKŁE + WYSTARCZY
    // =================================================================================================

    /** 7. W JEDNYM wyrażeniu (bez pętli) + jest w porządku — kompilator sam to optymalizuje. Nie komplikuj bez potrzeby. */
    static void whenPlusIsFine() {
        section("7. Kiedy + jest w porządku");

        String name = "Ala";
        int age = 30;
        String sentence = name + " ma " + age + " lat.";      // jedno wyrażenie — czytelne i szybkie
        show("jedno wyrażenie z +", sentence);
        // WYNIK: jedno wyrażenie z + → Ala ma 30 lat.

        // DOBRA PRAKTYKA: + w jednym wyrażeniu; StringBuilder w pętli; String.join/StringJoiner do list z separatorem;
        //   String.format / formatted, gdy ważny jest wygląd (Strings04Formatting).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • String += w pętli → wiele kopii; w pętli używaj StringBuilder.
     *   • sb.append(x).append(y) — łańcuch; na końcu sb.toString().
     *   • insert, reverse, deleteCharAt, setCharAt, setLength — zmieniają TEN SAM obiekt.
     *   • Separator bez „wiszącego” przecinka: dopisuj przed elementem (i > 0), String.join albo StringJoiner.
     *   • new StringJoiner(sep, prefix, suffix).add(...).
     *   • StringBuilder.equals porównuje obiekty — treść: toString().equals(...) / contentEquals.
     *   • + w jednym wyrażeniu jest OK.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego += na String w pętli jest wolne?
     *   2. Co wypisze:  StringBuilder sb = new StringBuilder("ab"); sb.append("c").reverse(); System.out.println(sb);  ?
     *   3. ZNAJDŹ BŁĄD:  if (new StringBuilder("tak").equals(new StringBuilder("tak"))) { ... }
     *   4. Co wypisze:  StringBuilder x = new StringBuilder("A"); StringBuilder y = x; y.append("B"); System.out.println(x);  ?
     *   5. Jak połączyć elementy tablicy przecinkami, bez przecinka na końcu, w jednej linii kodu?
     *   6. Kiedy nie ma sensu używać StringBuildera?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: odwróć \"Java\"", "avaJ", () -> exercise1("Java"));
        Check.equal("ćw. 2: działanie 1..5", "1 + 2 + 3 + 4 + 5 = 15", () -> exercise2(5));
        Check.equal("ćw. 3: kompresja \"aaabccdd\"", "a3b1c2d2", () -> exercise3("aaabccdd"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "avaJ", () -> solution1("Java"));
        Check.equal("ćw. 2 (wzorzec)", "1 + 2 + 3 + 4 + 5 = 15", () -> solution2(5));
        Check.equal("ćw. 3 (wzorzec)", "a3b1c2d2", () -> solution3("aaabccdd"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 3 OK, ✘ 0 BŁĄD
    }

    /** ĆWICZENIE 1 (łatwe): odwróć napis przy pomocy StringBuildera. Podpowiedź: {@code new StringBuilder(text).reverse().toString()}. */
    static String exercise1(String text) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 2 (średnie): zbuduj napis z działaniem dodawania liczb od 1 do n i wynikiem:
     * dla n = 5 → "1 + 2 + 3 + 4 + 5 = 15".
     * Podpowiedź: pętla z StringBuilderem, " + " dopisuj PRZED każdą liczbą oprócz pierwszej (sekcja 4); sumę licz obok.
     */
    static String exercise2(int n) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): „kompresja” — zamień serie tych samych liter na literę i długość serii:
     * "aaabccdd" → "a3b1c2d2".
     * Podpowiedź: idź po znakach, licz powtórzenia; gdy następny znak jest inny (albo to koniec napisu) — dopisz
     * znak i licznik do StringBuildera i wyzeruj licznik.
     */
    static String exercise3(String text) {
        // TODO: twoje rozwiązanie
        return "";
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(String text) {
        return new StringBuilder(text).reverse().toString();
    }

    static String solution2(int n) {
        StringBuilder sb = new StringBuilder();
        int sum = 0;
        for (int i = 1; i <= n; i++) {
            if (i > 1) {
                sb.append(" + ");
            }
            sb.append(i);
            sum += i;
        }
        return sb.append(" = ").append(sum).toString();
    }

    static String solution3(String text) {
        StringBuilder sb = new StringBuilder();
        int count = 1;
        for (int i = 0; i < text.length(); i++) {
            boolean lastOfRun = i == text.length() - 1 || text.charAt(i + 1) != text.charAt(i);
            if (lastOfRun) {
                sb.append(text.charAt(i)).append(count);
                count = 1;
            } else {
                count++;
            }
        }
        return sb.toString();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Każde += tworzy nowy String i kopiuje cały dotychczasowy tekst — łączna praca rośnie z kwadratem liczby obrotów.
     *   2. cba — append dokleja „c” (abc), reverse odwraca w miejscu.
     *   3. StringBuilder nie nadpisuje equals — porównuje obiekty, więc warunek jest zawsze fałszywy.
     *      Poprawnie: sb1.toString().equals(sb2.toString()) albo "tak".contentEquals(sb).
     *   4. AB — x i y to ten sam obiekt (alias), a StringBuilder jest zmienny.
     *   5. String.join(", ", tablica).
     *   6. Gdy sklejasz kilka elementów w jednym wyrażeniu (bez pętli) — zwykłe + jest czytelniejsze, a kompilator
     *      i tak je optymalizuje.
     */
    // </editor-fold>
}
