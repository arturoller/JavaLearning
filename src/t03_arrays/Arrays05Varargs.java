package t03_arrays;

import helpers.Check;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Varargs — metody ze zmienną liczbą argumentów
 *        (varargs = variable arguments = zmienna liczba argumentów; int... = „dowolnie wiele intów”)
 *
 * W SKRÓCIE:
 *   Parametr zapisany jako „int... numbers” przyjmuje 0, 1 albo wiele argumentów int, a także gotową tablicę int[].
 *   Wewnątrz metody numbers to zwykła TABLICA. Varargs musi być ostatnim parametrem i może być tylko jeden.
 *   Znasz to już z printf i String.format — to też metody z varargs.
 *
 * ANALOGIA: torba na zakupy. Kasjer nie pyta z góry „ile produktów?” — wkładasz jeden, pięć albo zero.
 *   Na końcu i tak dostajesz jedną torbę (tablicę), w której wszystko leży po kolei.
 *   Możesz też podać od razu gotową, spakowaną torbę (istniejącą tablicę).
 *
 * JAK TO DZIAŁA:
 *   static int sum(int... numbers) { ... }       ← w środku numbers ma typ int[]
 *
 *   sum();              → kompilator zamienia na sum(new int[] {})         (pusta tablica, NIE null)
 *   sum(5);             → sum(new int[] {5})
 *   sum(1, 2, 3);       → sum(new int[] {1, 2, 3})
 *   sum(existingArray); → tablica przekazana bez zmian (ta sama, nie kopia!)
 *
 *   Zasady: varargs zawsze OSTATNI; tylko JEDEN na metodę; przy przeciążeniach wygrywa wersja
 *   o stałej liczbie parametrów (varargs jest wybierany na samym końcu).
 *
 * SŁÓWKA:
 *   variable arity = zmienna liczba argumentów; fixed arity = stała liczba argumentów; overloading = przeciążanie
 *   (kilka metod o tej samej nazwie); ambiguous = niejednoznaczny; boxed = opakowany (Integer zamiast int)
 *
 * ZOBACZ TEŻ: t03_arrays/Arrays01Basics (tablice, alias), t05_methods/Methods02Overloading (przeciążanie metod),
 *   t12_collections/Collections08ImmutableUnmodifiable (List.of), t01_basics/Basics11ConsoleOutput (printf)
 * </pre>
 */
public class Arrays05Varargs {

    public static void main(String[] args) {
        title("Arrays05 — varargs: zmienna liczba argumentów");

        syntax();               // syntax = składnia
        callingVariants();      // calling variants = sposoby wywołania
        insideItIsArray();      // inside it is array = w środku to tablica
        lastParameterRules();   // last parameter rules = zasady: ostatni parametr
        overloading();          // overloading = przeciążanie
        nullPitfall();          // null pitfall = pułapka null
        sharedArrayPitfall();   // shared array pitfall = pułapka współdzielonej tablicy
        varargsInJdk();         // varargs in jdk = varargs w bibliotece standardowej
        whenNotToUse();         // when not to use = kiedy nie używać
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. SKŁADNIA
    // =================================================================================================

    /**
     * 1. Trzy kropki po typie parametru: „int... numbers”. Wywołujący podaje liczby po przecinku,
     * bez tworzenia tablicy.
     */
    static void syntax() {
        section("1. Składnia: int... numbers");

        // PRZED: parametr int[] — wywołujący musi sam zbudować tablicę.
        show("sumArray(new int[] {1, 2, 3})", sumArray(new int[] {1, 2, 3})); // sum array = suma tablicy
        // WYNIK: sumArray(new int[] {1, 2, 3}) → 6

        // PO: varargs — tablicę zbuduje kompilator.
        show("sum(1, 2, 3)", sum(1, 2, 3)); // sum = suma
        // WYNIK: sum(1, 2, 3) → 6

        // Inna „stara” droga: osobna metoda na każdą liczbę argumentów (sum2, sum3, sum4...) — dużo powtórzonego kodu.
        // DOBRA PRAKTYKA: varargs, gdy wszystkie argumenty mają TO SAMO znaczenie (lista liczb, lista imion).
    }

    /** PRZED: zwykły parametr tablicowy. */
    static int sumArray(int[] numbers) {
        int total = 0;
        for (int n : numbers) {
            total += n;
        }
        return total;
    }

    /** PO: varargs — w środku identyczny kod, bo numbers i tak jest tablicą. */
    static int sum(int... numbers) {
        int total = 0; // total = suma
        for (int n : numbers) {
            total += n;
        }
        return total;
    }

    // =================================================================================================
    // 2. WYWOŁANIA: 0, 1, WIELE ARGUMENTÓW, GOTOWA TABLICA
    // =================================================================================================

    /**
     * 2. Ta sama metoda przyjmuje dowolną liczbę argumentów — także zero — albo gotową tablicę.
     */
    static void callingVariants() {
        section("2. Wywołanie z 0, 1, wieloma argumentami i z tablicą");

        show("sum()", sum());
        // WYNIK: sum() → 0
        show("sum(5)", sum(5));
        // WYNIK: sum(5) → 5
        show("sum(1, 2, 3, 4, 5)", sum(1, 2, 3, 4, 5));
        // WYNIK: sum(1, 2, 3, 4, 5) → 15

        int[] existing = {10, 20, 30}; // existing = istniejąca
        show("sum(existing)", sum(existing));
        // WYNIK: sum(existing) → 60

        // PUŁAPKA: nie da się MIESZAĆ: sum(existing, 5) to błąd kompilacji (albo same inty, albo jedna tablica int[]).
    }

    // =================================================================================================
    // 3. W ŚRODKU TO TABLICA
    // =================================================================================================

    /**
     * 3. Wewnątrz metody parametr varargs ma wszystko, co tablica: length, indeksy, for-each.
     * Bez argumentów dostaje PUSTĄ tablicę (length == 0), a nie null.
     */
    static void insideItIsArray() {
        section("3. Wewnątrz metody varargs to zwykła tablica");

        inspect();          // inspect = zbadaj, obejrzyj
        // WYNIK: length=0, zawartość=[]
        inspect(7, 8);
        // WYNIK: length=2, zawartość=[7, 8], pierwszy=7
        inspect(new int[] {4, 5, 6});
        // WYNIK: length=3, zawartość=[4, 5, 6], pierwszy=4

        // PUŁAPKA: numbers[0] bez sprawdzenia length — przy wywołaniu bez argumentów rzuci wyjątek:
        expectThrows("firstOf() bez argumentów", () -> firstOf()); // first of = pierwszy z
        // WYNIK: ✔ firstOf() bez argumentów → rzucono ArrayIndexOutOfBoundsException: Index 0 out of bounds for length 0
        // Lepiej wymusić co najmniej jeden argument już w sygnaturze (sekcja 4).
    }

    /** Wypisuje długość, zawartość i (jeśli jest) pierwszy element. */
    static void inspect(int... numbers) {
        String first = numbers.length > 0 ? ", pierwszy=" + numbers[0] : ""; // first = pierwszy
        System.out.println("length=" + numbers.length + ", zawartość=" + Arrays.toString(numbers) + first);
    }

    /** Źle zaprojektowana: zakłada, że zawsze jest co najmniej jeden argument. */
    static int firstOf(int... numbers) {
        return numbers[0];
    }

    // =================================================================================================
    // 4. VARARGS MUSI BYĆ OSTATNI — I TYLKO JEDEN
    // =================================================================================================

    /**
     * 4. Parametry obowiązkowe stoją przed varargs. Kompilator musi wiedzieć, gdzie kończą się
     * „zwykłe” argumenty, więc varargs zawsze jest ostatni i jest co najwyżej jeden.
     */
    static void lastParameterRules() {
        section("4. Varargs zawsze na końcu");

        show("greet(\"Cześć\")", greet("Cześć")); // greet = przywitaj
        // WYNIK: greet("Cześć") → Cześć!
        show("greet(\"Cześć\", \"Ala\")", greet("Cześć", "Ala"));
        // WYNIK: greet("Cześć", "Ala") → Cześć Ala!
        show("greet(\"Hej\", \"Ala\", \"Olek\", \"Ewa\")", greet("Hej", "Ala", "Olek", "Ewa"));
        // WYNIK: greet("Hej", "Ala", "Olek", "Ewa") → Hej Ala, Olek, Ewa!

        // Błędy kompilacji:
        //   static void a(int... numbers, String label) { }    ← varargs nie jest ostatni
        //   static void b(int... xs, String... ys) { }          ← dwa varargs
        // Kompilator nie wiedziałby, które argumenty należą do którego parametru.

        // DOBRA PRAKTYKA: gdy metoda potrzebuje CO NAJMNIEJ jednego argumentu, zapisz to w sygnaturze:
        //   static int max(int first, int... rest)   ← max() się nie skompiluje, więc nie ma pustego przypadku
        // (to jest ćwiczenie 1).
    }

    /** Parametr obowiązkowy + varargs na końcu. */
    static String greet(String greeting, String... names) {
        StringBuilder sb = new StringBuilder(greeting); // greeting = powitanie
        for (int i = 0; i < names.length; i++) {
            sb.append(i == 0 ? " " : ", ").append(names[i]);
        }
        return sb.append("!").toString();
    }

    // =================================================================================================
    // 5. PRZECIĄŻANIE — KTÓRA METODA WYGRA?
    // =================================================================================================

    /**
     * 5. Gdy istnieje wersja o stałej liczbie parametrów i wersja z varargs, kompilator wybiera
     * stałą — varargs to „ostatnia deska ratunku”.
     */
    static void overloading() {
        section("5. Przeciążanie: stała liczba parametrów wygrywa z varargs");

        show("pick(1, 2)", pick(1, 2)); // pick = wybierz
        // WYNIK: pick(1, 2) → wersja (int, int)
        show("pick(1, 2, 3)", pick(1, 2, 3));
        // WYNIK: pick(1, 2, 3) → wersja (int...)
        show("pick()", pick());
        // WYNIK: pick() → wersja (int...)

        // PUŁAPKA: dwie wersje z varargs, które pasują tak samo dobrze → błąd kompilacji:
        //   static void m(int... numbers) { }
        //   static void m(int first, int... rest) { }
        //   m(1, 2);   ← błąd kompilacji: odwołanie do m jest niejednoznaczne (ambiguous)
        // Obie wersje mogą przyjąć (1, 2) i żadna nie jest „bardziej pasująca”. Przeciążanie: t05_methods/Methods02Overloading.

        // DOBRA PRAKTYKA: unikaj przeciążania metod z varargs. Jeśli już musisz — różnicuj je typami, a nie liczbą.
    }

    static String pick(int a, int b) {
        return "wersja (int, int)";
    }

    static String pick(int... numbers) {
        return "wersja (int...)";
    }

    // =================================================================================================
    // 6. PUŁAPKA: NULL ZAMIAST ARGUMENTÓW
    // =================================================================================================

    /**
     * 6. Przekazanie null do varargs to przekazanie null JAKO TABLICY. Metoda, która robi for-each
     * albo sprawdza length, rzuci NullPointerException.
     */
    static void nullPitfall() {
        section("6. PUŁAPKA: null przekazany do varargs");

        expectThrows("sum(null)", () -> sum(null));
        // WYNIK: ✔ sum(null) → rzucono NullPointerException: Cannot read the array length because "<local2>" is null
        // <local2> to ukryta (bez nazwy) zmienna, którą kompilator tworzy dla pętli for-each po tablicy.

        // Dla String... jest jeszcze gorzej: greet("Cześć", null) jest dwuznaczne — null jako CAŁA tablica
        // czy jako JEDEN element? Kompilator ostrzega i wybiera „całą tablicę”.
        // Jawny zapis rozwiewa wątpliwości:
        //   greet("Cześć", (String[]) null)   ← null jako tablica → NullPointerException w środku
        //   greet("Cześć", (String) null)     ← jeden element równy null → "Cześć null!"
        show("greet(\"Cześć\", (String) null)", greet("Cześć", (String) null));
        // WYNIK: greet("Cześć", (String) null) → Cześć null!

        // DOBRA PRAKTYKA: w metodach publicznych (używanych przez innych) obsłuż null na wejściu:
        show("safeSum(null)", safeSum(null)); // safe sum = bezpieczna suma
        // WYNIK: safeSum(null) → 0
    }

    /** Traktuje null jak brak argumentów. */
    static int safeSum(int... numbers) {
        if (numbers == null) {
            return 0;
        }
        return sum(numbers);
    }

    // =================================================================================================
    // 7. PUŁAPKA: PRZEKAZANA TABLICA NIE JEST KOPIOWANA
    // =================================================================================================

    /**
     * 7. Gdy wywołujący przekaże istniejącą tablicę, metoda dostaje TĘ SAMĄ tablicę (alias).
     * Zmiana elementów w metodzie zmienia tablicę wywołującego.
     */
    static void sharedArrayPitfall() {
        section("7. PUŁAPKA: tablica przekazana do varargs to alias");

        int[] prices = {10, 20}; // prices = ceny
        doubleAll(prices);       // double all = podwój wszystkie
        show("prices po doubleAll(prices)", Arrays.toString(prices));
        // WYNIK: prices po doubleAll(prices) → [20, 40]

        int a = 10;
        int b = 20;
        doubleAll(a, b);         // tu kompilator tworzy NOWĄ tablicę {10, 20} — a i b się nie zmienią
        show("a, b po doubleAll(a, b)", a + ", " + b);
        // WYNIK: a, b po doubleAll(a, b) → 10, 20

        // DOBRA PRAKTYKA: metoda z varargs nie powinna zmieniać elementów parametru. Jeśli musi pracować na
        // zmienionych danych — niech zrobi kopię (numbers.clone(), Arrays.copyOf) i zwróci wynik.
    }

    /** Zmienia elementy tablicy-parametru (efekt uboczny widoczny u wywołującego). */
    static void doubleAll(int... numbers) {
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] *= 2;
        }
    }

    // =================================================================================================
    // 8. VARARGS W BIBLIOTECE STANDARDOWEJ
    // =================================================================================================

    /**
     * 8. printf, String.format, Arrays.asList, List.of — wszystkie korzystają z varargs.
     */
    static void varargsInJdk() {
        section("8. Varargs w JDK: printf, format, asList, List.of");

        System.out.printf(Locale.ROOT, "%s ma %d lat i %.1f zł%n", "Ala", 30, 12.5); // printf(format, Object... args)
        // WYNIK: Ala ma 30 lat i 12.5 zł
        show("String.format(\"%d-%d-%d\", 1, 2, 3)", String.format(Locale.ROOT, "%d-%d-%d", 1, 2, 3));
        // WYNIK: String.format("%d-%d-%d", 1, 2, 3) → 1-2-3

        List<String> letters = Arrays.asList("a", "b", "c"); // letters = litery; asList(T... a)
        show("Arrays.asList(\"a\", \"b\", \"c\")", letters);
        // WYNIK: Arrays.asList("a", "b", "c") → [a, b, c]
        List<Integer> numbers = List.of(1, 2, 3); // List.of (Java 9+) = niemodyfikowalna lista z podanych elementów
        show("List.of(1, 2, 3)", numbers);
        // WYNIK: List.of(1, 2, 3) → [1, 2, 3]
        // Ciekawostka: List.of ma osobne wersje dla 0..10 argumentów i dopiero potem varargs — żeby w typowych
        // wywołaniach nie tworzyć za każdym razem dodatkowej tablicy.

        // PUŁAPKA: Arrays.asList z tablicą TYPU PROSTEGO (int[]) daje listę z JEDNYM elementem — całą tablicą!
        // Varargs asList przyjmuje obiekty; int[] jest jednym obiektem, a pojedyncze int nie są obiektami.
        int[] primitives = {1, 2, 3};   // primitives = typy proste
        show("Arrays.asList(int[]).size()", Arrays.asList(primitives).size());
        // WYNIK: Arrays.asList(int[]).size() → 1
        Integer[] boxed = {1, 2, 3};    // boxed = opakowane (Integer, t01_basics/Basics06Wrappers)
        show("Arrays.asList(Integer[]).size()", Arrays.asList(boxed).size());
        // WYNIK: Arrays.asList(Integer[]).size() → 3

        // Math.max NIE jest varargs — ma tylko dwa parametry. Math.max(1, 2, 3) się nie skompiluje.
    }

    // =================================================================================================
    // 9. KIEDY NIE UŻYWAĆ VARARGS
    // =================================================================================================

    /**
     * 9. Varargs pasuje do „listy rzeczy tego samego rodzaju”. Nie pasuje, gdy argumenty mają
     * RÓŻNE znaczenia albo gdy ich liczba jest ściśle określona.
     */
    static void whenNotToUse() {
        section("9. Kiedy NIE używać varargs");

        // ŹLE: data jako varargs — znaczenie argumentu zależy od pozycji, a liczba nie jest pilnowana.
        show("dateBad(2026, 3, 15)", dateBad(2026, 3, 15)); // date bad = zła metoda daty
        // WYNIK: dateBad(2026, 3, 15) → 2026-03-15
        expectThrows("dateBad(2026, 3) — zapomniany dzień", () -> dateBad(2026, 3));
        // WYNIK: ✔ dateBad(2026, 3) — zapomniany dzień → rzucono ArrayIndexOutOfBoundsException: Index 2 out of bounds for length 2
        // Kompilator przepuścił błędne wywołanie. Wersja date(int year, int month, int day) by go zatrzymała.
        show("date(2026, 3, 15)", date(2026, 3, 15));
        // WYNIK: date(2026, 3, 15) → 2026-03-15

        // Kiedy jeszcze NIE:
        //   • metoda wywoływana miliony razy w pętli — każde wywołanie tworzy nową tablicę (koszt pamięci i czasu),
        //   • argumenty różnych typów/znaczeń (nazwa, wiek, miasto) — zwykłe parametry albo obiekt (t06_oop_basics),
        //   • dane i tak są już w tablicy lub liście — przyjmij int[] / List wprost.
        // Kiedy TAK: sum(...), max(...), greet(...), log(format, args...) — lista wartości tego samego rodzaju.
    }

    /** ŹLE zaprojektowana: rok, miesiąc, dzień jako varargs. */
    static String dateBad(int... parts) {
        return String.format(Locale.ROOT, "%04d-%02d-%02d", parts[0], parts[1], parts[2]);
    }

    /** DOBRZE: trzy nazwane parametry — kompilator pilnuje liczby i kolejności. */
    static String date(int year, int month, int day) {
        return String.format(Locale.ROOT, "%04d-%02d-%02d", year, month, day);
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • static int sum(int... numbers) — w środku numbers to int[]; wywołanie: sum(), sum(1), sum(1, 2, 3), sum(tablica).
     *   • Bez argumentów → pusta tablica (length 0), nie null. Sprawdzaj length przed numbers[0].
     *   • Varargs: tylko jeden i zawsze ostatni. Parametry obowiązkowe przed nim: max(int first, int... rest).
     *   • Przeciążanie: wersja o stałej liczbie parametrów wygrywa. m(int...) + m(int, int...) → niejednoznaczność.
     *   • sum(null) przekazuje null jako tablicę → NullPointerException w środku.
     *   • Przekazana tablica to alias — metoda nie powinna zmieniać jej elementów.
     *   • JDK: printf, String.format, Arrays.asList, List.of (Java 9+). Arrays.asList(int[]) → lista z 1 elementem!
     *   • NIE: argumenty o różnych znaczeniach, ściśle określona liczba, gorące pętle.
     *
     * PYTANIA KONTROLNE:
     *   1. Jaki typ ma parametr numbers wewnątrz metody void f(int... numbers)?
     *   2. Co wypisze:  static int count(String... s) { return s.length; }   System.out.println(count() + count("a", "b"));  ?
     *   3. ZNAJDŹ BŁĄD:  static void log(String... messages, int level) { }
     *   4. Co wypisze (pick jak w sekcji 5):  System.out.println(pick(7, 8));  ?
     *   5. ZNAJDŹ BŁĄD:  static int first(int... n) { return n[0]; }   ...   first();
     *   6. Co wypisze:  int[] t = {1, 2};  doubleAll(t);  System.out.println(Arrays.toString(t));  ?
     *   7. Dlaczego Arrays.asList(new int[] {1, 2, 3}).size() zwraca 1?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: max z co najmniej jednej liczby", "3, 9, -2",
                () -> exercise1(3) + ", " + exercise1(3, 9, 2) + ", " + exercise1(-5, -2, -8));
        Check.equal("ćw. 2: najdłuższe słowo", "słoń | () | ab",
                () -> exercise2("kot", "słoń", "mysz") + " | (" + exercise2() + ") | " + exercise2("ab", "cd"));
        Check.equal("ćw. 3: średnia dowolnej liczby ocen", "1.50 | 2.33 | 0.00",
                () -> String.format(Locale.ROOT, "%.2f | %.2f | %.2f", exercise3(1, 2), exercise3(1, 2, 4), exercise3()));
        Check.equal("ćw. 4: sklejanie tablic", "[1, 2, 3, 4, 5] | []",
                () -> Arrays.toString(exercise4(new int[] {1, 2}, new int[] {3}, new int[] {}, new int[] {4, 5}))
                        + " | " + Arrays.toString(exercise4()));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "3, 9, -2",
                () -> solution1(3) + ", " + solution1(3, 9, 2) + ", " + solution1(-5, -2, -8));
        Check.equal("ćw. 2 (wzorzec)", "słoń | () | ab",
                () -> solution2("kot", "słoń", "mysz") + " | (" + solution2() + ") | " + solution2("ab", "cd"));
        Check.equal("ćw. 3 (wzorzec)", "1.50 | 2.33 | 0.00",
                () -> String.format(Locale.ROOT, "%.2f | %.2f | %.2f", solution3(1, 2), solution3(1, 2, 4), solution3()));
        Check.equal("ćw. 4 (wzorzec)", "[1, 2, 3, 4, 5] | []",
                () -> Arrays.toString(solution4(new int[] {1, 2}, new int[] {3}, new int[] {}, new int[] {4, 5}))
                        + " | " + Arrays.toString(solution4()));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć największą z podanych liczb. Sygnatura wymusza co najmniej jedną liczbę.
     * Podpowiedź: int max = first; potem for-each po rest z Math.max.
     */
    static int exercise1(int first, int... rest) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć najdłuższe słowo; przy remisie — pierwsze z nich; bez argumentów — "".
     * Podpowiedź: String best = ""; w pętli zamieniaj tylko, gdy słowo jest ŚCIŚLE dłuższe (większe, nie równe).
     */
    static String exercise2(String... words) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ dwie przeciążone metody na JEDNĄ z varargs. Bez argumentów zwróć 0.0.
     * <pre>{@code
     * static double average(int a, int b) {
     *     return (a + b) / 2.0;
     * }
     * static double average(int a, int b, int c) {
     *     return (a + b + c) / 3.0;
     * }
     * }</pre>
     * Podpowiedź: strażnik dla numbers.length == 0, potem suma w pętli i (double) suma / numbers.length.
     */
    static double exercise3(int... numbers) {
        // TODO: twoje rozwiązanie
        return 0.0;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): sklej dowolną liczbę tablic w jedną (w podanej kolejności).
     * Parametr int[]... arrays to w środku tablica tablic, czyli int[][] (Arrays02MultiDim).
     * Podpowiedź: najpierw policz łączną długość, utwórz wynik, potem kopiuj — pętlą albo System.arraycopy
     * z przesuwanym indeksem „gdzie teraz wpisywać”.
     */
    static int[] exercise4(int[]... arrays) {
        // TODO: twoje rozwiązanie
        return new int[0];
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(int first, int... rest) {
        int max = first;
        for (int n : rest) {
            max = Math.max(max, n);
        }
        return max;
    }

    static String solution2(String... words) {
        String best = "";
        for (String w : words) {
            if (w.length() > best.length()) {
                best = w;
            }
        }
        return best;
    }

    static double solution3(int... numbers) {
        if (numbers.length == 0) {
            return 0.0;
        }
        int total = 0;
        for (int n : numbers) {
            total += n;
        }
        return (double) total / numbers.length;
    }

    static int[] solution4(int[]... arrays) {
        int totalLength = 0; // total length = łączna długość
        for (int[] part : arrays) {
            totalLength += part.length;
        }
        int[] result = new int[totalLength];
        int position = 0; // position = gdzie teraz wpisywać
        for (int[] part : arrays) {
            System.arraycopy(part, 0, result, position, part.length);
            position += part.length;
        }
        return result;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. int[] — zwykła tablica.
     *   2. 2  (count() zwraca 0, count("a", "b") zwraca 2).
     *   3. Varargs musi być ostatnim parametrem. Poprawnie: static void log(int level, String... messages).
     *   4. „wersja (int, int)” — metoda o stałej liczbie parametrów wygrywa z varargs.
     *   5. first() dostaje pustą tablicę → n[0] rzuca ArrayIndexOutOfBoundsException.
     *      Lepsza sygnatura: static int first(int head, int... tail) albo sprawdzenie length.
     *   6. [2, 4] — przekazana tablica to alias, metoda zmieniła jej elementy.
     *   7. Varargs asList(T... a) przyjmuje obiekty. int[] to jeden obiekt, więc lista ma jeden element (całą tablicę).
     *      Dla Integer[] elementy są obiektami i trafiają do listy osobno.
     */
    // </editor-fold>
}
