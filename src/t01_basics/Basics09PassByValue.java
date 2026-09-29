package t01_basics;

import helpers.Check;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Stos, sterta, referencje i przekazywanie przez wartość
 *        (stack = stos; heap = sterta; reference = referencja; pass by value = przekazywanie przez wartość)
 *
 * W SKRÓCIE:
 *   Zmienna typu prostego (int, double...) przechowuje SAMĄ wartość. Zmienna typu obiektowego (tablica, lista,
 *   String, Product...) przechowuje REFERENCJĘ — „adres” obiektu leżącego gdzie indziej (na stercie).
 *   Java ZAWSZE przekazuje do metody KOPIĘ zmiennej: kopię liczby albo kopię referencji. Dlatego metoda może
 *   zmienić ZAWARTOŚĆ obiektu (obie kopie wskazują ten sam obiekt), ale nie może podmienić zmiennej wywołującego.
 *
 * ANALOGIA: kartka z adresem domu.
 *   int to kartka z liczbą — dajesz komuś KSEROKOPIĘ, może ją pomazać, Twoja kartka zostaje bez zmian.
 *   Referencja to kartka z ADRESEM domu. Dajesz kserokopię adresu: ktoś pojedzie pod ten adres i przemaluje dom
 *   (zmieni obiekt) — Ty to zobaczysz, bo to ten sam dom. Ale jeśli skreśli adres na swojej kopii i wpisze inny,
 *   Twoja kartka nadal wskazuje stary dom.
 *
 * JAK TO DZIAŁA:
 *   STOS (stack)                         STERTA (heap)
 *   ┌─────────────────────┐              ┌───────────────────────┐
 *   │ int age = 30        │              │                       │
 *   │ int[] data ─────────┼─────────────▶│ [1, 2, 3]  (tablica)  │
 *   │ int[] copy ─────────┼─────────────▶│   ↑ ten SAM obiekt    │
 *   └─────────────────────┘              └───────────────────────┘
 *   Zmienne lokalne i parametry żyją na stosie (znikają po wyjściu z metody); obiekty — na stercie.
 *   Obiekt, do którego nie prowadzi już żadna referencja, sprząta automatycznie Garbage Collector (odśmiecacz).
 *
 * SŁÓWKA:
 *   stack = stos; heap = sterta; reference = referencja („adres” obiektu); pass by value = przekazywanie przez wartość;
 *   mutate = zmieniać (stan obiektu); reassign = przypisać ponownie; garbage collector = odśmiecacz (sprzątacz pamięci);
 *   alias = druga nazwa tego samego obiektu; defensive copy = kopia obronna.
 *
 * ZOBACZ TEŻ: t01_basics/Basics06Wrappers (Integer jako obiekt), t03_arrays/Arrays01Basics (tablice),
 *             t06_oop_basics/Oop06Immutability (obiekty niezmienne), t26_jvm/Jvm01Memory (pamięć JVM dokładniej).
 * </pre>
 */
public class Basics09PassByValue {

    public static void main(String[] args) {
        title("Basics09 — referencje i przekazywanie przez wartość");

        primitivesAreCopied();      // primitives are copied = typy proste są kopiowane
        referencesShareObject();    // references share object = referencje wskazują ten sam obiekt
        methodGetsCopyOfValue();    // method gets copy of value = metoda dostaje kopię wartości
        methodCanMutateObject();    // method can mutate object = metoda może zmienić obiekt
        methodCannotReassign();     // method cannot reassign = metoda nie podmieni zmiennej
        stringsAreImmutable();      // strings are immutable = napisy są niezmienne
        equalsVsSameObject();       // equals vs same object = równe czy ten sam obiekt
        nullReference();            // null reference = pusta referencja
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. TYPY PROSTE — KOPIOWANIE WARTOŚCI
    // =================================================================================================

    /** 1. Przypisanie int do int kopiuje WARTOŚĆ. Dalej to dwie niezależne zmienne. */
    static void primitivesAreCopied() {
        section("1. Typy proste: przypisanie = kopia wartości");

        int a = 10;
        int b = a;          // b dostaje KOPIĘ liczby 10
        b = 99;             // zmiana b nie dotyka a
        show("a, b", a + ", " + b);
        // WYNIK: a, b → 10, 99
    }

    // =================================================================================================
    // 2. REFERENCJE — DWIE ZMIENNE, JEDEN OBIEKT
    // =================================================================================================

    /** 2. Przypisanie tablicy (obiektu) kopiuje REFERENCJĘ. Obie zmienne wskazują TEN SAM obiekt (alias). */
    static void referencesShareObject() {
        section("2. Referencje: dwie zmienne, jeden obiekt");

        int[] original = {1, 2, 3};
        int[] alias = original;         // kopia ADRESU, a nie tablicy!
        alias[0] = 100;                 // zmieniamy obiekt przez alias...
        show("original", Arrays.toString(original));    // Arrays.toString = tablica jako tekst
        // WYNIK: original → [100, 2, 3]    ← ...i widać to przez original

        int[] realCopy = Arrays.copyOf(original, original.length);   // copyOf = prawdziwa kopia (nowy obiekt)
        realCopy[1] = 200;
        show("original po zmianie kopii", Arrays.toString(original));
        show("realCopy", Arrays.toString(realCopy));
        // WYNIK: original po zmianie kopii → [100, 2, 3]
        // WYNIK: realCopy → [100, 200, 3]

        // PUŁAPKA: „skopiowałem” tablicę/listę przez  b = a  i dziwię się, że zmiany w b psują a. To nie kopia — to alias.
    }

    // =================================================================================================
    // 3. METODA DOSTAJE KOPIĘ WARTOŚCI
    // =================================================================================================

    /** addTen = dodaj dziesięć. Zmienia TYLKO swoją kopię parametru. */
    private static void addTen(int number) {
        number = number + 10;
    }

    /** 3. Parametr metody to KOPIA argumentu. Zmiana parametru nie wpływa na zmienną wywołującego. */
    static void methodGetsCopyOfValue() {
        section("3. Metoda zmienia tylko swoją kopię (typ prosty)");

        int score = 5;
        addTen(score);
        show("score po addTen(score)", score);
        // WYNIK: score po addTen(score) → 5    ← bez zmian!
        // DOBRA PRAKTYKA: jeśli metoda ma „zmienić liczbę”, niech ją ZWRACA: score = addTenAndReturn(score).
    }

    // =================================================================================================
    // 4. METODA MOŻE ZMIENIĆ OBIEKT
    // =================================================================================================

    /** addItem = dodaj element. Dostaje kopię referencji, ale wskazuje ona TĘ SAMĄ listę — więc zmiana jest widoczna. */
    private static void addItem(List<String> list) {
        list.add("mleko");
    }

    /** 4. Kopia referencji wskazuje ten sam obiekt — metoda może zmienić jego zawartość. */
    static void methodCanMutateObject() {
        section("4. Metoda może zmienić ZAWARTOŚĆ obiektu");

        List<String> cart = new ArrayList<>(List.of("chleb"));
        addItem(cart);
        show("koszyk po addItem(cart)", cart);
        // WYNIK: koszyk po addItem(cart) → [chleb, mleko]

        // PUŁAPKA: metoda, która „po cichu” zmienia przekazaną listę, zaskakuje wywołującego. To efekt uboczny.
        // DOBRA PRAKTYKA: z nazwy metody ma wynikać, czy zmienia argument (addItem — tak; calculateTotal — nie).
    }

    // =================================================================================================
    // 5. METODA NIE PODMIENI ZMIENNEJ WYWOŁUJĄCEGO
    // =================================================================================================

    /** replaceList = podmień listę. Przypisuje NOWĄ listę do swojej kopii referencji — wywołujący tego nie zobaczy. */
    private static void replaceList(List<String> list) {
        list = new ArrayList<>(List.of("zupełnie", "nowa"));
        list.add("lista");                       // zmieniamy już NOWĄ listę, nie tę od wywołującego
    }

    /** 5. Dowód, że Java przekazuje przez wartość (a nie „przez referencję”): podmiana parametru nie działa na zewnątrz. */
    static void methodCannotReassign() {
        section("5. Metoda NIE podmieni zmiennej wywołującego");

        List<String> cart = new ArrayList<>(List.of("chleb"));
        replaceList(cart);
        show("koszyk po replaceList(cart)", cart);
        // WYNIK: koszyk po replaceList(cart) → [chleb]    ← bez zmian: metoda przepięła tylko SWOJĄ kopię adresu
    }

    // =================================================================================================
    // 6. STRING JEST NIEZMIENNY
    // =================================================================================================

    /** shout = krzyknij. Wygląda, jakby zmieniało napis — ale tworzy NOWY, a stary zostaje. */
    private static void shout(String text) {
        text = text.toUpperCase();              // nowy napis przypisany do lokalnej kopii
    }

    /** 6. String nie ma metod zmieniających jego treść — każda „zmiana” tworzy nowy obiekt. */
    static void stringsAreImmutable() {
        section("6. String jest niezmienny");

        String greeting = "cześć";
        shout(greeting);
        greeting.toUpperCase();                 // wynik wyrzucony — nigdzie go nie zapisaliśmy!
        show("greeting", greeting);
        // WYNIK: greeting → cześć

        greeting = greeting.toUpperCase();      // tak trzeba: przypisać NOWY napis
        show("greeting po przypisaniu", greeting);
        // WYNIK: greeting po przypisaniu → CZEŚĆ
    }

    // =================================================================================================
    // 7. == KONTRA equals
    // =================================================================================================

    /** 7. == na obiektach porównuje REFERENCJE (czy to ten sam obiekt). equals porównuje ZAWARTOŚĆ. */
    static void equalsVsSameObject() {
        section("7. == (ten sam obiekt?) kontra equals (ta sama treść?)");

        List<Integer> first = new ArrayList<>(List.of(1, 2));
        List<Integer> second = new ArrayList<>(List.of(1, 2));
        List<Integer> alias = first;
        show("first == second", first == second);
        show("first.equals(second)", first.equals(second));
        show("first == alias", first == alias);
        // WYNIK: first == second → false    ← dwa różne obiekty
        // WYNIK: first.equals(second) → true    ← ta sama treść
        // WYNIK: first == alias → true    ← ten sam obiekt
    }

    // =================================================================================================
    // 8. NULL — REFERENCJA DONIKĄD
    // =================================================================================================

    /** 8. null to referencja, która nie wskazuje żadnego obiektu. Wywołanie metody na null → NullPointerException. */
    static void nullReference() {
        section("8. null i NullPointerException");

        List<String> nothing = null;
        expectThrows("nothing.size() na null", () -> nothing.size());
        // WYNIK: ✔ nothing.size() na null → rzucono NullPointerException: Cannot invoke "java.util.List.size()" because "nothing" is null

        // Garbage Collector: gdy do obiektu nie prowadzi już ŻADNA referencja (np. zmienna wyszła z zasięgu albo
        //   przypisałeś jej null), obiekt zostanie kiedyś automatycznie usunięty z pamięci. Nie musisz (i nie możesz)
        //   zwalniać pamięci ręcznie jak w C.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Typ prosty: zmienna trzyma WARTOŚĆ; przypisanie i przekazanie do metody = kopia wartości.
     *   • Obiekt: zmienna trzyma REFERENCJĘ; b = a to alias (ten sam obiekt), a nie kopia. Kopia: Arrays.copyOf, new ArrayList<>(a).
     *   • Java ZAWSZE przekazuje przez wartość: metoda dostaje kopię liczby albo kopię referencji.
     *   • Metoda MOŻE zmienić zawartość obiektu (list.add), ale NIE podmieni zmiennej wywołującego (list = new ...).
     *   • String jest niezmienny: s.toUpperCase() bez przypisania nic nie robi.
     *   • == na obiektach: czy ten sam obiekt; equals: czy ta sama treść.
     *   • null.metoda() → NullPointerException. Nieużywane obiekty sprząta Garbage Collector.
     *
     * PYTANIA KONTROLNE:
     *   1. Co wypisze:  int x = 1; int y = x; y++; System.out.println(x);  ?
     *   2. Co wypisze:  int[] a = {1}; int[] b = a; b[0] = 5; System.out.println(a[0]);  ?
     *   3. ZNAJDŹ BŁĄD:  static void reset(int[] t) { t = new int[t.length]; }  — „metoda nie zeruje tablicy”. Dlaczego?
     *   4. Czy Java przekazuje obiekty „przez referencję”? Jak to dokładnie działa?
     *   5. Co wypisze:  String s = "abc"; s.concat("def"); System.out.println(s);  ?
     *   6. Czym różni się  a == b  od  a.equals(b)  dla dwóch list?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: premia +100 dla każdego (tablica zmieniona w miejscu)", "[1100, 2100, 3100]", () -> {
            int[] salaries = {1000, 2000, 3000};
            exercise1(salaries, 100);
            return Arrays.toString(salaries);
        });
        Check.equal("ćw. 2: zamiana elementów 0 i 2", "[c, b, a]", () -> {
            String[] letters = {"a", "b", "c"};
            exercise2(letters, 0, 2);
            return Arrays.toString(letters);
        });
        Check.equal("ćw. 3: nowa lista ×2, oryginał bez zmian", List.of(List.of(2, 4, 6), List.of(1, 2, 3)), () -> {
            List<Integer> input = new ArrayList<>(List.of(1, 2, 3));
            List<Integer> result = exercise3(input);
            return List.of(result, input);
        });
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "[1100, 2100, 3100]", () -> {
            int[] salaries = {1000, 2000, 3000};
            solution1(salaries, 100);
            return Arrays.toString(salaries);
        });
        Check.equal("ćw. 2 (wzorzec)", "[c, b, a]", () -> {
            String[] letters = {"a", "b", "c"};
            solution2(letters, 0, 2);
            return Arrays.toString(letters);
        });
        Check.equal("ćw. 3 (wzorzec)", List.of(List.of(2, 4, 6), List.of(1, 2, 3)), () -> {
            List<Integer> input = new ArrayList<>(List.of(1, 2, 3));
            List<Integer> result = solution3(input);
            return List.of(result, input);
        });
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 3 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): dodaj premię do KAŻDEJ pensji w tablicy — zmień tablicę „w miejscu” (metoda nic nie zwraca).
     * Podpowiedź: pętla po indeksach i {@code salaries[i] += bonus;} — tablica to obiekt, więc zmiana będzie widoczna.
     */
    static void exercise1(int[] salaries, int bonus) {
        // TODO: twoje rozwiązanie
    }

    /**
     * ĆWICZENIE 2 (średnie): zamień miejscami elementy tablicy o indeksach i oraz j.
     * Podpowiedź: zmienna tymczasowa (Basics03Variables, sekcja 7). Zadziała, bo zmieniasz OBIEKT (tablicę),
     * a nie zmienne wywołującego.
     */
    static void exercise2(String[] items, int i, int j) {
        // TODO: twoje rozwiązanie
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): zwróć NOWĄ listę z podwojonymi wartościami, NIE zmieniając listy wejściowej.
     * [1, 2, 3] → wynik [2, 4, 6], a wejście nadal [1, 2, 3].
     * Podpowiedź: utwórz {@code new ArrayList<Integer>()} i dodawaj do niej {@code value * 2} w pętli for-each.
     * Sprawdzian wykryje, jeśli „po drodze” zmienisz wejście (np. przez set na tej samej liście).
     */
    static List<Integer> exercise3(List<Integer> input) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static void solution1(int[] salaries, int bonus) {
        for (int i = 0; i < salaries.length; i++) {
            salaries[i] += bonus;
        }
    }

    static void solution2(String[] items, int i, int j) {
        String temp = items[i];
        items[i] = items[j];
        items[j] = temp;
    }

    static List<Integer> solution3(List<Integer> input) {
        List<Integer> result = new ArrayList<>();
        for (int value : input) {
            result.add(value * 2);
        }
        return result;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. 1 — y to kopia wartości; y++ nie zmienia x.
     *   2. 5 — a i b wskazują TĘ SAMĄ tablicę.
     *   3. t = new int[...] podmienia tylko KOPIĘ referencji wewnątrz metody; tablica wywołującego zostaje nietknięta.
     *      Żeby wyzerować: zmień zawartość (Arrays.fill(t, 0) albo pętla t[i] = 0).
     *   4. Nie — zawsze przez wartość. Dla obiektów tą wartością jest referencja (adres), więc metoda może zmienić
     *      obiekt, ale nie może podmienić zmiennej wywołującego.
     *   5. abc — concat zwraca NOWY napis, a wynik nie został przypisany (String jest niezmienny).
     *   6. == sprawdza, czy to ten sam obiekt; equals — czy listy mają te same elementy w tej samej kolejności.
     */
    // </editor-fold>
}
