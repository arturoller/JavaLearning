package t03_arrays;

import helpers.Check;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Klasa java.util.Arrays i System.arraycopy — gotowe narzędzia do tablic
 *        (utility = narzędziowy; sort = sortuj; binary search = wyszukiwanie binarne; fill = wypełnij; copy = kopiuj)
 *
 * W SKRÓCIE:
 *   Tablice nie mają „własnych” metod (poza length i clone). Wszystkie wygodne operacje — sortowanie,
 *   wyszukiwanie, wypełnianie, kopiowanie, porównywanie, wypisywanie — są metodami STATYCZNYMI klasy Arrays:
 *   Arrays.sort(tablica), Arrays.copyOf(tablica, n) itd. Do szybkiego przesuwania fragmentów służy System.arraycopy.
 *
 * ANALOGIA: skrzynka z narzędziami obok szafy. Szafa (tablica) sama nic nie umie, ale obok leży
 *   skrzynka (klasa Arrays) z sorterem, lupą do szukania, wałkiem do malowania (fill) i kserokopiarką (copyOf).
 *
 * JAK TO DZIAŁA:
 *   Najważniejsze metody:
 *   Arrays.toString(a) / deepToString(a)      → tekst "[1, 2, 3]" / dla 2D
 *   Arrays.sort(a) / sort(a, od, do)          → sortuje W MIEJSCU (zmienia a), zwraca void
 *   Arrays.binarySearch(a, x)                 → indeks x albo liczba ujemna; TYLKO dla posortowanej tablicy!
 *   Arrays.fill(a, x) / fill(a, od, do, x)    → wypełnia wartością
 *   Arrays.copyOf(a, n) / copyOfRange(a, od, do) → NOWA tablica (dłuższa → zera, krótsza → obcięta)
 *   Arrays.equals(a, b) / deepEquals(a, b)    → porównanie zawartości (1D / 2D)
 *   Arrays.asList(a)                          → lista-widok na tablicę (stały rozmiar!)
 *   System.arraycopy(src, od, dest, od, ile)  → kopiuje fragment (także w obrębie tej samej tablicy)
 *   a.clone()                                 → kopia tablicy (dla 2D — PŁYTKA)
 *   Zakresy „od, do”: od WŁĄCZNIE, do WYŁĄCZNIE (jak w pętli i = od; i {@code <} do).
 *
 * SŁÓWKA:
 *   in place = w miejscu (bez tworzenia nowej tablicy); insertion point = miejsce wstawienia;
 *   range = zakres; inclusive = włącznie; exclusive = wyłącznie; view = widok; shallow copy = płytka kopia;
 *   deep copy = głęboka kopia; natural order = porządek naturalny; collator = narzędzie do porównywania tekstów wg języka
 *
 * ZOBACZ TEŻ: t03_arrays/Arrays04Algorithms (jak te algorytmy działają w środku),
 *   t12_collections/Collections08ImmutableUnmodifiable (listy niemodyfikowalne),
 *   t16_streams/Streams02Creation (Arrays.stream), t04_strings/Strings06CharUnicode (Unicode)
 * </pre>
 */
public class Arrays03Utility {

    public static void main(String[] args) {
        title("Arrays03 — klasa Arrays i System.arraycopy");

        sortPrimitives();       // sort primitives = sortowanie typów prostych
        sortStrings();          // sort strings = sortowanie tekstów
        binarySearch();         // binary search = wyszukiwanie binarne
        fill();                 // fill = wypełnianie
        copyOfAndRange();       // copy of and range = kopia i kopia zakresu
        equalsVariants();       // equals variants = odmiany porównywania
        asListView();           // as list view = widok listy na tablicy
        systemArraycopy();      // system arraycopy = kopiowanie fragmentów
        cloneAndShallowCopy();  // clone and shallow copy = clone i płytka kopia
        streamPreview();        // stream preview = zapowiedź strumieni
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. SORTOWANIE TYPÓW PROSTYCH
    // =================================================================================================

    /**
     * 1. Arrays.sort sortuje rosnąco W MIEJSCU — zmienia przekazaną tablicę i niczego nie zwraca.
     */
    static void sortPrimitives() {
        section("1. Arrays.sort — liczby");

        int[] numbers = {42, 7, 19, 3, 25, 7}; // numbers = liczby
        Arrays.sort(numbers);
        show("po Arrays.sort", Arrays.toString(numbers));
        // WYNIK: po Arrays.sort → [3, 7, 7, 19, 25, 42]

        int[] partly = {9, 8, 7, 6, 5, 4}; // partly = częściowo
        Arrays.sort(partly, 1, 4);         // tylko indeksy 1, 2, 3 (4 wyłącznie!)
        show("sort(partly, 1, 4)", Arrays.toString(partly));
        // WYNIK: sort(partly, 1, 4) → [9, 6, 7, 8, 5, 4]

        double[] temps = {3.5, -1.0, 2.25}; // temps = temperatury
        Arrays.sort(temps);
        show("double[] po sort", Arrays.toString(temps));
        // WYNIK: double[] po sort → [-1.0, 2.25, 3.5]

        // PUŁAPKA: sort zwraca void. „int[] sorted = Arrays.sort(numbers);” to błąd kompilacji.
        // Jeśli oryginalna kolejność jest potrzebna — najpierw kopia (sekcja 5), potem sort kopii.
        // Sortowania MALEJĄCEGO dla int[] nie ma w jednym wywołaniu: posortuj i odwróć (t03_arrays/Arrays04Algorithms).
    }

    // =================================================================================================
    // 2. SORTOWANIE TEKSTÓW
    // =================================================================================================

    /**
     * 2. Teksty sortują się w porządku naturalnym = według kodów znaków Unicode, a nie według alfabetu polskiego.
     */
    static void sortStrings() {
        section("2. Arrays.sort — teksty i polskie litery");

        String[] names = sampleNames(); // sample names = przykładowe imiona
        Arrays.sort(names);
        show("porządek Unicode", Arrays.toString(names));
        // WYNIK: porządek Unicode → [Adam, Bartek, Zenon, ewa, ola, Łucja, łukasz]
        // Dlaczego tak? Kody: wielkie A–Z to 65–90, małe a–z to 97–122, a Ł i ł to 321 i 322.
        // Więc najpierw WSZYSTKIE wielkie litery, potem małe, a polskie znaki na samym końcu.

        // Alfabet polski: Collator (narzędzie do porównywania tekstów według reguł języka).
        // Drugi argument sort to „sposób porównywania” (Comparator — t12_collections/Collections07ComparableComparator).
        String[] polish = sampleNames();
        Arrays.sort(polish, Collator.getInstance(Locale.forLanguageTag("pl-PL"))); // get instance = pobierz obiekt
        show("porządek polski (Collator)", Arrays.toString(polish));
        // WYNIK: porządek polski (Collator) → [Adam, Bartek, ewa, Łucja, łukasz, ola, Zenon]

        // DOBRA PRAKTYKA: tekst dla człowieka (lista imion, miast) → Collator z Locale pl-PL.
        // Porządek Unicode wystarcza do „technicznych” napisów (kody produktów, identyfikatory).
    }

    /** Zwraca NOWĄ tablicę przykładowych imion (za każdym razem w tej samej, nieposortowanej kolejności). */
    static String[] sampleNames() {
        return new String[] {"ola", "Zenon", "ewa", "Łucja", "Adam", "łukasz", "Bartek"};
    }

    // =================================================================================================
    // 3. WYSZUKIWANIE BINARNE
    // =================================================================================================

    /**
     * 3. binarySearch w posortowanej tablicy znajduje element błyskawicznie (dzieli zakres na pół).
     * Na NIEposortowanej tablicy daje wyniki bez sensu — i nie ostrzega o tym!
     */
    static void binarySearch() {
        section("3. Arrays.binarySearch — tylko na posortowanej tablicy!");

        int[] sorted = {3, 7, 12, 19, 25, 42}; // sorted = posortowana
        show("binarySearch(sorted, 19)", Arrays.binarySearch(sorted, 19));
        // WYNIK: binarySearch(sorted, 19) → 3
        show("binarySearch(sorted, 20)", Arrays.binarySearch(sorted, 20));
        // WYNIK: binarySearch(sorted, 20) → -5
        // Wynik ujemny = „nie ma”. Liczba zdradza, GDZIE element by pasował: -(miejsce wstawienia) - 1.
        // 20 pasowałoby na indeks 4 (między 19 a 25), więc -(4) - 1 = -5.

        // PUŁAPKA: tablica nieposortowana. 42 JEST w tablicy (na indeksie 0), a binarySearch go nie widzi:
        int[] unsorted = {42, 7, 19, 3, 25, 12}; // unsorted = nieposortowana
        show("binarySearch(unsorted, 42)", Arrays.binarySearch(unsorted, 42));
        // WYNIK: binarySearch(unsorted, 42) → -7
        // Algorytm zakłada porządek: sprawdził środek (19), uznał, że 42 musi być „na prawo”, i szukał tylko tam.

        // DOBRA PRAKTYKA: binarySearch tylko po Arrays.sort (tym samym porządkiem!). Dla małej, nieposortowanej
        // tablicy zwykła pętla (wyszukiwanie liniowe) jest w zupełności wystarczająca.
    }

    // =================================================================================================
    // 4. WYPEŁNIANIE
    // =================================================================================================

    /**
     * 4. Arrays.fill wpisuje tę samą wartość do całej tablicy albo do zakresu.
     * Dla tablic 2D trzeba wypełniać wiersz po wierszu.
     */
    static void fill() {
        section("4. Arrays.fill");

        int[] seats = new int[6]; // seats = miejsca
        Arrays.fill(seats, 1);
        show("fill(seats, 1)", Arrays.toString(seats));
        // WYNIK: fill(seats, 1) → [1, 1, 1, 1, 1, 1]
        Arrays.fill(seats, 2, 4, 0); // indeksy 2 i 3
        show("fill(seats, 2, 4, 0)", Arrays.toString(seats));
        // WYNIK: fill(seats, 2, 4, 0) → [1, 1, 0, 0, 1, 1]

        char[] dashes = new char[10]; // dashes = kreski
        Arrays.fill(dashes, '-');
        System.out.println(new String(dashes)); // new String(char[]) = tekst ze znaków tablicy
        // WYNIK: ----------

        // PUŁAPKA: fill na tablicy 2D próbuje wpisać liczbę w miejsce WIERSZA (a wiersz to tablica).
        int[][] grid = new int[2][3];
        expectThrows("Arrays.fill(grid2D, 7)", () -> Arrays.fill(grid, 7));
        // WYNIK: ✔ Arrays.fill(grid2D, 7) → rzucono ArrayStoreException: java.lang.Integer
        for (int[] row : grid) {
            Arrays.fill(row, 7);   // poprawnie: każdy wiersz osobno
        }
        show("grid po wypełnieniu wierszy", Arrays.deepToString(grid));
        // WYNIK: grid po wypełnieniu wierszy → [[7, 7, 7], [7, 7, 7]]

        // PUŁAPKA: fill(tablica2D, new int[2]) wstawia do wszystkich miejsc TEN SAM wiersz (jeden obiekt!).
        int[][] shared = new int[3][]; // shared = współdzielona
        Arrays.fill(shared, new int[2]);
        shared[0][0] = 5;
        show("shared po shared[0][0] = 5", Arrays.deepToString(shared));
        // WYNIK: shared po shared[0][0] = 5 → [[5, 0], [5, 0], [5, 0]]
    }

    // =================================================================================================
    // 5. COPYOF I COPYOFRANGE
    // =================================================================================================

    /**
     * 5. copyOf tworzy NOWĄ tablicę o podanej długości: dłuższą (dopełnia zerami) albo krótszą (obcina).
     * copyOfRange kopiuje fragment od..do (do wyłącznie).
     */
    static void copyOfAndRange() {
        section("5. Arrays.copyOf i copyOfRange");

        int[] base = {1, 2, 3}; // base = podstawa
        show("copyOf(base, 5)", Arrays.toString(Arrays.copyOf(base, 5)));
        // WYNIK: copyOf(base, 5) → [1, 2, 3, 0, 0]
        show("copyOf(base, 2)", Arrays.toString(Arrays.copyOf(base, 2)));
        // WYNIK: copyOf(base, 2) → [1, 2]

        int[] values = {10, 20, 30, 40, 50}; // values = wartości
        show("copyOfRange(values, 1, 4)", Arrays.toString(Arrays.copyOfRange(values, 1, 4)));
        // WYNIK: copyOfRange(values, 1, 4) → [20, 30, 40]
        show("copyOfRange(base, 1, 5)", Arrays.toString(Arrays.copyOfRange(base, 1, 5)));
        // WYNIK: copyOfRange(base, 1, 5) → [2, 3, 0, 0]
        // „do” może wyjść poza tablicę — brakujące miejsca dostaną wartość domyślną (0).

        // „Rosnąca tablica” z Arrays01Basics w jednej linii:
        int[] cart = {5, 7}; // cart = koszyk
        cart = Arrays.copyOf(cart, cart.length + 1);
        cart[cart.length - 1] = 9;
        show("cart po dopisaniu 9", Arrays.toString(cart));
        // WYNIK: cart po dopisaniu 9 → [5, 7, 9]

        // PUŁAPKA: copyOfRange(a, 3, 1) — „od” większe niż „do” → IllegalArgumentException.
        expectThrows("copyOfRange(values, 3, 1)", () -> Arrays.copyOfRange(values, 3, 1));
        // WYNIK: ✔ copyOfRange(values, 3, 1) → rzucono IllegalArgumentException: 3 > 1
    }

    // =================================================================================================
    // 6. PORÓWNYWANIE: ==, EQUALS, ARRAYS.EQUALS, DEEPEQUALS
    // =================================================================================================

    /**
     * 6. Zawartość tablic 1D porównuje Arrays.equals, a tablic 2D — Arrays.deepEquals.
     */
    static void equalsVariants() {
        section("6. == kontra Arrays.equals kontra Arrays.deepEquals");

        int[] a = {1, 2, 3};
        int[] b = {1, 2, 3};
        System.out.println("a == b: " + (a == b) + ", a.equals(b): " + a.equals(b)
                + ", Arrays.equals(a, b): " + Arrays.equals(a, b));
        // WYNIK: a == b: false, a.equals(b): false, Arrays.equals(a, b): true

        show("Arrays.equals({1, 2}, {2, 1})", Arrays.equals(new int[] {1, 2}, new int[] {2, 1}));
        // WYNIK: Arrays.equals({1, 2}, {2, 1}) → false
        // Kolejność ma znaczenie — to porównanie „element po elemencie na tych samych pozycjach”.

        int[][] g1 = {{1, 2}, {3}};
        int[][] g2 = {{1, 2}, {3}};
        show("Arrays.equals(g1, g2) dla 2D", Arrays.equals(g1, g2));
        // WYNIK: Arrays.equals(g1, g2) dla 2D → false
        show("Arrays.deepEquals(g1, g2)", Arrays.deepEquals(g1, g2)); // deep equals = porównaj „w głąb”
        // WYNIK: Arrays.deepEquals(g1, g2) → true
        // Arrays.equals dla 2D porównuje WIERSZE przez ich equals, czyli przez adresy — różne obiekty → false.
    }

    // =================================================================================================
    // 7. ARRAYS.ASLIST — LISTA-WIDOK NA TABLICY
    // =================================================================================================

    /**
     * 7. Arrays.asList nie kopiuje tablicy — daje listę, która „patrzy” na tę samą tablicę.
     * Nie można jej wydłużyć, a zmiany przechodzą w obie strony.
     */
    static void asListView() {
        section("7. Arrays.asList — widok o stałym rozmiarze");

        String[] fruits = {"jabłko", "gruszka", "śliwka"}; // fruits = owoce
        List<String> view = Arrays.asList(fruits); // view = widok; List = lista (t12_collections/Collections02Lists)
        show("Arrays.asList(fruits)", view);
        // WYNIK: Arrays.asList(fruits) → [jabłko, gruszka, śliwka]

        // PUŁAPKA: lista ma stały rozmiar — tak jak tablica pod spodem.
        expectThrows("view.add(\"kiwi\")", () -> view.add("kiwi"));
        // WYNIK: ✔ view.add("kiwi") → rzucono UnsupportedOperationException: (brak komunikatu)

        // PUŁAPKA: zmiana przez listę zmienia tablicę — i odwrotnie.
        view.set(0, "banan"); // set = ustaw (podmień element)
        show("fruits po view.set(0, \"banan\")", Arrays.toString(fruits));
        // WYNIK: fruits po view.set(0, "banan") → [banan, gruszka, śliwka]
        fruits[1] = "malina";
        show("view po fruits[1] = \"malina\"", view);
        // WYNIK: view po fruits[1] = "malina" → [banan, malina, śliwka]

        // DOBRA PRAKTYKA: potrzebujesz niezależnej, rosnącej listy → skopiuj do ArrayList:
        ArrayList<String> own = new ArrayList<>(Arrays.asList(fruits)); // own = własna
        own.add("kiwi");
        show("ArrayList z kopią", own);
        // WYNIK: ArrayList z kopią → [banan, malina, śliwka, kiwi]
    }

    // =================================================================================================
    // 8. SYSTEM.ARRAYCOPY
    // =================================================================================================

    /**
     * 8. System.arraycopy(źródło, odIndeksu, cel, odIndeksuWCelu, ileElementów) kopiuje fragment
     * do ISTNIEJĄCEJ tablicy. Działa poprawnie nawet wtedy, gdy źródło i cel to ta sama tablica.
     */
    static void systemArraycopy() {
        section("8. System.arraycopy — kopiowanie i przesuwanie fragmentów");

        int[] src = {1, 2, 3, 4, 5};  // src = source = źródło
        int[] dest = new int[7];      // dest = destination = cel
        System.arraycopy(src, 1, dest, 2, 3); // 3 elementy od src[1] trafiają na dest[2], dest[3], dest[4]
        show("dest", Arrays.toString(dest));
        // WYNIK: dest → [0, 0, 2, 3, 4, 0, 0]

        // Wstawianie w środek: przesuń „ogon” o jedno miejsce w prawo i wpisz nową wartość.
        int[] line = {10, 20, 30, 40, 0}; // line = kolejka; ostatnie miejsce wolne
        System.arraycopy(line, 2, line, 3, 2); // 30 i 40 przesuwają się w prawo
        show("po przesunięciu", Arrays.toString(line));
        // WYNIK: po przesunięciu → [10, 20, 30, 30, 40]
        line[2] = 25;
        show("po wstawieniu 25", Arrays.toString(line));
        // WYNIK: po wstawieniu 25 → [10, 20, 25, 30, 40]

        // PUŁAPKA: kopiowanie poza koniec źródła lub celu → wyjątek (nic się nie skopiuje).
        expectThrows("arraycopy 4 elementów od src[3]", () -> System.arraycopy(src, 3, dest, 0, 4));
        // WYNIK: ✔ arraycopy 4 elementów od src[3] → rzucono ArrayIndexOutOfBoundsException: arraycopy: last source index 7 out of bounds for int[5]

        // DOBRA PRAKTYKA: potrzebujesz NOWEJ tablicy → Arrays.copyOf / copyOfRange (czytelniej).
        // Wpisujesz do ISTNIEJĄCEJ tablicy albo przesuwasz fragment → System.arraycopy.
    }

    // =================================================================================================
    // 9. CLONE I PŁYTKA KOPIA TABLIC 2D
    // =================================================================================================

    /**
     * 9. clone() kopiuje tablicę. Dla 1D to pełna kopia. Dla 2D kopiowane są tylko REFERENCJE wierszy
     * — wiersze są wspólne (płytka kopia).
     */
    static void cloneAndShallowCopy() {
        section("9. clone() i płytka kopia tablic 2D");

        int[] orig = {1, 2, 3};  // orig = oryginał
        int[] cl = orig.clone(); // clone = sklonuj (utwórz kopię)
        cl[0] = 99;
        show("orig / cl", Arrays.toString(orig) + " / " + Arrays.toString(cl));
        // WYNIK: orig / cl → [1, 2, 3] / [99, 2, 3]

        // PUŁAPKA: clone tablicy 2D kopiuje tylko „kręgosłup” — wiersze są te same.
        int[][] grid = {{1, 2}, {3, 4}};
        int[][] shallow = grid.clone(); // shallow = płytka kopia
        shallow[0][0] = 99;
        show("grid po shallow[0][0] = 99", Arrays.deepToString(grid));
        // WYNIK: grid po shallow[0][0] = 99 → [[99, 2], [3, 4]]
        show("shallow[0] == grid[0]", shallow[0] == grid[0]);
        // WYNIK: shallow[0] == grid[0] → true

        // Głęboka kopia: klonujemy KAŻDY wiersz osobno.
        int[][] deep = new int[grid.length][]; // deep = głęboka kopia
        for (int r = 0; r < grid.length; r++) {
            deep[r] = grid[r].clone();
        }
        deep[1][1] = -1;
        show("grid", Arrays.deepToString(grid));
        // WYNIK: grid → [[99, 2], [3, 4]]
        show("deep", Arrays.deepToString(deep));
        // WYNIK: deep → [[99, 2], [3, -1]]
        // To samo dotyczy Arrays.copyOf na tablicy 2D — też kopiuje tylko referencje wierszy.
    }

    // =================================================================================================
    // 10. ZAPOWIEDŹ: ARRAYS.STREAM
    // =================================================================================================

    /**
     * 10. Arrays.stream zamienia tablicę w strumień — sumę, maksimum czy średnią dostajesz jednym wywołaniem.
     * Strumienie omawiamy dokładnie w t16_streams (tu tylko przedsmak).
     */
    static void streamPreview() {
        section("10. Zapowiedź: Arrays.stream");

        int[] scores = {72, 95, 58, 88}; // scores = wyniki
        show("Arrays.stream(scores).sum()", Arrays.stream(scores).sum()); // stream = strumień; sum = suma
        // WYNIK: Arrays.stream(scores).sum() → 313
        show("Arrays.stream(scores).max().getAsInt()", Arrays.stream(scores).max().getAsInt()); // get as int = pobierz jako int
        // WYNIK: Arrays.stream(scores).max().getAsInt() → 95
        show("Arrays.stream(scores).average().orElse(0)", Arrays.stream(scores).average().orElse(0)); // or else = albo (gdy pusto)
        // WYNIK: Arrays.stream(scores).average().orElse(0) → 78.25
        // max() i average() zwracają „pudełko, które może być puste” (OptionalInt, OptionalDouble) — bo pusta
        // tablica nie ma maksimum. Szczegóły: t14_optional/Optional01Basics, t16_streams/Streams02Creation.
        // Do tego czasu ćwicz pętle — strumień robi dokładnie to samo, co akumulatory z Control05LoopPatterns.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Arrays.toString(a) — 1D; Arrays.deepToString(a) — 2D.
     *   • Arrays.sort(a) — rosnąco, W MIEJSCU, zwraca void. Teksty: porządek Unicode (A–Z, a–z, potem Ł, ł...).
     *     Polski alfabet: Arrays.sort(a, Collator.getInstance(Locale.forLanguageTag("pl-PL"))).
     *   • Arrays.binarySearch(a, x) — TYLKO na posortowanej; wynik ujemny = brak (-(miejsce wstawienia) - 1).
     *   • Arrays.fill(a, x) / fill(a, od, do, x). Tablica 2D — wypełniaj wiersz po wierszu.
     *   • Arrays.copyOf(a, n) — nowa tablica: dłuższa (zera) / krótsza (obcięta). copyOfRange(a, od, do).
     *   • Zakresy: od WŁĄCZNIE, do WYŁĄCZNIE.
     *   • Porównanie zawartości: Arrays.equals (1D), Arrays.deepEquals (2D). == i a.equals(b) porównują adresy.
     *   • Arrays.asList(a) — widok: add → UnsupportedOperationException, set zmienia tablicę. Kopia: new ArrayList<>(...).
     *   • System.arraycopy(src, od, dest, od, ile) — do istniejącej tablicy, także przesuwanie w tej samej.
     *   • clone() — 1D pełna kopia; 2D PŁYTKA (wspólne wiersze). Głęboka: clone każdego wiersza.
     *
     * PYTANIA KONTROLNE:
     *   1. Co wypisze:  int[] a = {5, 1, 4};  Arrays.sort(a, 0, 2);  System.out.println(Arrays.toString(a));  ?
     *   2. Dlaczego binarySearch na nieposortowanej tablicy może „nie znaleźć” elementu, który w niej jest?
     *   3. Co wypisze:  System.out.println(Arrays.toString(Arrays.copyOf(new int[] {7, 8}, 4)));  ?
     *   4. ZNAJDŹ BŁĄD:  List<String> list = Arrays.asList("a", "b");  list.add("c");
     *   5. ZNAJDŹ BŁĄD:  int[][] copy = original.clone();  copy[0][0] = 0;   // „oryginał się nie zmieni”
     *   6. Co wypisze:  String[] s = {"b", "B", "a"};  Arrays.sort(s);  System.out.println(Arrays.toString(s));  ?
     *   7. Czym różni się Arrays.copyOf od System.arraycopy?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: posortowana kopia | oryginał", "[1, 4, 5] | [5, 1, 4]", () -> {
            int[] data = {5, 1, 4};
            return Arrays.toString(exercise1(data)) + " | " + Arrays.toString(data);
        });
        Check.equal("ćw. 2: te same elementy bez względu na kolejność", "true, false, false",
                () -> exercise2(new int[] {3, 1, 2}, new int[] {2, 3, 1}) + ", "
                        + exercise2(new int[] {1, 1, 2}, new int[] {1, 2, 2}) + ", "
                        + exercise2(new int[] {1, 2}, new int[] {1, 2, 3}));
        Check.equal("ćw. 3: środek tablicy", "[2, 3, 4] | [] | []",
                () -> Arrays.toString(exercise3(new int[] {1, 2, 3, 4, 5})) + " | "
                        + Arrays.toString(exercise3(new int[] {7, 8})) + " | " + Arrays.toString(exercise3(new int[] {9})));
        Check.equal("ćw. 4: wstawianie na pozycję", "[1, 2, 3, 4, 5] | [0, 1, 2] | [1, 2, 3]",
                () -> Arrays.toString(exercise4(new int[] {1, 2, 4, 5}, 2, 3)) + " | "
                        + Arrays.toString(exercise4(new int[] {1, 2}, 0, 0)) + " | "
                        + Arrays.toString(exercise4(new int[] {1, 2}, 2, 3)));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "[1, 4, 5] | [5, 1, 4]", () -> {
            int[] data = {5, 1, 4};
            return Arrays.toString(solution1(data)) + " | " + Arrays.toString(data);
        });
        Check.equal("ćw. 2 (wzorzec)", "true, false, false",
                () -> solution2(new int[] {3, 1, 2}, new int[] {2, 3, 1}) + ", "
                        + solution2(new int[] {1, 1, 2}, new int[] {1, 2, 2}) + ", "
                        + solution2(new int[] {1, 2}, new int[] {1, 2, 3}));
        Check.equal("ćw. 3 (wzorzec)", "[2, 3, 4] | [] | []",
                () -> Arrays.toString(solution3(new int[] {1, 2, 3, 4, 5})) + " | "
                        + Arrays.toString(solution3(new int[] {7, 8})) + " | " + Arrays.toString(solution3(new int[] {9})));
        Check.equal("ćw. 4 (wzorzec)", "[1, 2, 3, 4, 5] | [0, 1, 2] | [1, 2, 3]",
                () -> Arrays.toString(solution4(new int[] {1, 2, 4, 5}, 2, 3)) + " | "
                        + Arrays.toString(solution4(new int[] {1, 2}, 0, 0)) + " | "
                        + Arrays.toString(solution4(new int[] {1, 2}, 2, 3)));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć posortowaną KOPIĘ tablicy. Oryginał ma zostać w starej kolejności.
     * Podpowiedź: Arrays.copyOf(a, a.length) albo a.clone(), potem Arrays.sort na kopii.
     */
    static int[] exercise1(int[] a) {
        // TODO: twoje rozwiązanie
        return new int[0];
    }

    /**
     * ĆWICZENIE 2 (średnie): czy dwie tablice mają te same elementy (z tymi samymi powtórzeniami),
     * niezależnie od kolejności? Nie zmieniaj tablic wejściowych!
     * Podpowiedź: posortuj KOPIE obu tablic i porównaj je Arrays.equals.
     */
    static boolean exercise2(int[] a, int[] b) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ pętlę na jedno wywołanie Arrays.copyOfRange (strażnik zostaje).
     * <pre>{@code
     * if (a.length < 3) {
     *     return new int[0];
     * }
     * int[] result = new int[a.length - 2];
     * for (int i = 1; i < a.length - 1; i++) {
     *     result[i - 1] = a[i];
     * }
     * return result;
     * }</pre>
     * Podpowiedź: „od” to 1, „do” (wyłącznie) to a.length - 1.
     */
    static int[] exercise3(int[] a) {
        // TODO: twoje rozwiązanie
        return new int[0];
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zwróć NOWĄ tablicę z value wstawionym na pozycję index
     * (0 = na początek, a.length = na koniec). Elementy od index przesuwają się w prawo.
     * Podpowiedź: Arrays.copyOf(a, a.length + 1) daje miejsce na końcu; System.arraycopy(result, index,
     * result, index + 1, a.length - index) przesuwa ogon; na koniec result[index] = value.
     */
    static int[] exercise4(int[] a, int index, int value) {
        // TODO: twoje rozwiązanie
        return new int[0];
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int[] solution1(int[] a) {
        int[] copy = Arrays.copyOf(a, a.length);
        Arrays.sort(copy);
        return copy;
    }

    static boolean solution2(int[] a, int[] b) {
        int[] sortedA = a.clone();
        int[] sortedB = b.clone();
        Arrays.sort(sortedA);
        Arrays.sort(sortedB);
        return Arrays.equals(sortedA, sortedB);
    }

    static int[] solution3(int[] a) {
        if (a.length < 3) {
            return new int[0];
        }
        return Arrays.copyOfRange(a, 1, a.length - 1);
    }

    static int[] solution4(int[] a, int index, int value) {
        int[] result = Arrays.copyOf(a, a.length + 1);
        System.arraycopy(result, index, result, index + 1, a.length - index);
        result[index] = value;
        return result;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. [1, 5, 4] — sortowane są tylko indeksy 0 i 1 (2 wyłącznie).
     *   2. Bo algorytm zakłada porządek: porównuje ze środkiem i odrzuca połowę, w której „nie może być” elementu.
     *      W nieposortowanej tablicy odrzuca też połowę, w której element akurat jest.
     *   3. [7, 8, 0, 0]
     *   4. asList daje listę o stałym rozmiarze → add rzuca UnsupportedOperationException.
     *      Potrzebna rosnąca lista: new ArrayList<>(Arrays.asList("a", "b")).
     *   5. clone tablicy 2D jest płytki — copy[0] i original[0] to ten sam wiersz, więc oryginał też się zmieni.
     *      Głęboka kopia: w pętli deep[r] = original[r].clone().
     *   6. [B, a, b] — wielkie litery mają mniejsze kody Unicode niż małe.
     *   7. copyOf tworzy i zwraca NOWĄ tablicę (od początku źródła, o podanej długości);
     *      arraycopy kopiuje dowolny fragment do ISTNIEJĄCEJ tablicy, w dowolne miejsce (także w obrębie tej samej).
     */
    // </editor-fold>
}
