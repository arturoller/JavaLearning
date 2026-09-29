package t03_arrays;

import helpers.Check;

import java.util.ArrayList;
import java.util.Arrays;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Tablice — podstawy
 *        (array = tablica; element = element; index = indeks, numer pozycji; length = długość)
 *
 * W SKRÓCIE:
 *   Tablica przechowuje wiele wartości TEGO SAMEGO typu pod jedną nazwą. Elementy są ponumerowane
 *   od 0 do length - 1. Rozmiar ustala się przy tworzeniu i NIE da się go później zmienić.
 *   Tablica jest obiektem — zmienna trzyma tylko referencję (adres), a nie same elementy.
 *
 * ANALOGIA: rząd szafek na basenie. Szafki mają numery od 0, jest ich z góry ustalona liczba
 *   (nie dobudujesz szafki w trakcie dnia), w każdej mieści się jedna rzecz tego samego rodzaju.
 *   Zmienna tablicowa to nie szafki, tylko KARTKA z adresem rzędu szafek. Skopiowanie kartki
 *   (int[] b = a;) nie tworzy nowych szafek — obie kartki prowadzą do tych samych.
 *
 * JAK TO DZIAŁA:
 *   int[] a = {7, 3, 9};
 *
 *   zmienna a (na stosie)          obiekt tablicy (na stercie)
 *   ┌──────────┐                   ┌─────┬─────┬─────┐
 *   │ adres ───┼──────────────────►│  7  │  3  │  9  │   length = 3
 *   └──────────┘                   └─────┴─────┴─────┘
 *                                   [0]   [1]   [2]      ← indeksy od 0 do length - 1
 *
 *   Odczyt a[1] → 3.  Zapis a[1] = 5.  Indeks spoza 0..2 → ArrayIndexOutOfBoundsException.
 *
 * SŁÓWKA:
 *   declaration = deklaracja; initializer = inicjalizator { }; default value = wartość domyślna;
 *   out of bounds = poza zakresem; reference = referencja (adres obiektu); alias = druga nazwa tego samego obiektu;
 *   copy = kopia; fixed size = stały rozmiar; heap = sterta (pamięć na obiekty); stack = stos (pamięć na zmienne lokalne)
 *
 * ZOBACZ TEŻ: t01_basics/Basics09PassByValue (referencje i przekazywanie do metod),
 *   t03_arrays/Arrays03Utility (gotowe narzędzia: copyOf, sort, equals),
 *   t12_collections/Collections02Lists (ArrayList — „tablica, która sama rośnie”)
 * </pre>
 */
public class Arrays01Basics {

    public static void main(String[] args) {
        title("Arrays01 — tablice: podstawy");

        declareAndCreate();     // declare and create = deklaracja i tworzenie
        lengthField();          // length field = pole length
        defaultValues();        // default values = wartości domyślne
        indexesAndBounds();     // indexes and bounds = indeksy i granice
        iterating();            // iterating = przechodzenie po elementach
        printingArrays();       // printing arrays = wypisywanie tablic
        arraysAreObjects();     // arrays are objects = tablice są obiektami
        nullElementsPitfall();  // null elements pitfall = pułapka elementów null
        fixedSize();            // fixed size = stały rozmiar
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. DEKLARACJA I TWORZENIE
    // =================================================================================================

    /**
     * 1. Trzy sposoby utworzenia tablicy: new z rozmiarem, inicjalizator w klamrach, new z inicjalizatorem.
     */
    static void declareAndCreate() {
        section("1. Deklaracja i tworzenie");

        int[] scores;                  // scores = wyniki; typ int[] czytaj „tablica intów”; tablicy jeszcze NIE MA
        scores = new int[5];           // new = utwórz; 5 elementów, każdy ma wartość 0
        show("new int[5]", Arrays.toString(scores)); // Arrays.toString = zamień tablicę na czytelny tekst
        // WYNIK: new int[5] → [0, 0, 0, 0, 0]

        int[] primes = {2, 3, 5, 7, 11}; // primes = liczby pierwsze; rozmiar (5) policzy kompilator
        show("inicjalizator {2, 3, 5, 7, 11}", Arrays.toString(primes));
        // WYNIK: inicjalizator {2, 3, 5, 7, 11} → [2, 3, 5, 7, 11]

        String[] days = new String[] {"pn", "wt", "śr"}; // days = dni; pełna forma: new Typ[] { ... }
        show("new String[] {...}", Arrays.toString(days));
        // WYNIK: new String[] {...} → [pn, wt, śr]

        // Krótka forma { } działa TYLKO w deklaracji. Przy ponownym przypisaniu trzeba napisać new int[]:
        //   primes = {1, 2};            // błąd kompilacji: inicjalizator tablicy nie jest tu dozwolony
        primes = new int[] {1, 2};       // OK
        show("primes po ponownym przypisaniu", Arrays.toString(primes));
        // WYNIK: primes po ponownym przypisaniu → [1, 2]

        // Rozmiar może być obliczony w czasie działania programu (zmienna), ale nie może być ujemny:
        int size = 3; // size = rozmiar
        double[] prices = new double[size]; // prices = ceny
        show("new double[size], size = 3", Arrays.toString(prices));
        // WYNIK: new double[size], size = 3 → [0.0, 0.0, 0.0]
        expectThrows("new int[-1]", () -> {
            int[] bad = new int[-1];
            System.out.println(bad.length);
        });
        // WYNIK: ✔ new int[-1] → rzucono NegativeArraySizeException: -1

        // PUŁAPKA: styl z języka C „int numbers[]” też się kompiluje, ale myli:
        //   int[] a, b;    → a i b to tablice
        //   int c[], d;    → c to tablica, a d to ZWYKŁY int!
        // DOBRA PRAKTYKA: nawiasy [] zawsze przy typie: int[] numbers.
    }

    // =================================================================================================
    // 2. LENGTH — POLE, NIE METODA
    // =================================================================================================

    /**
     * 2. length mówi, ile elementów ma tablica. Ostatni indeks to zawsze length - 1.
     */
    static void lengthField() {
        section("2. length — długość tablicy");

        int[] temps = {12, 15, 9, 20}; // temps = temperatury
        show("temps.length", temps.length);
        // WYNIK: temps.length → 4
        show("ostatni indeks", temps.length - 1);
        // WYNIK: ostatni indeks → 3
        show("ostatni element", temps[temps.length - 1]);
        // WYNIK: ostatni element → 20

        // PUŁAPKA: w tablicy length to POLE (bez nawiasów), w String — METODA length() (z nawiasami).
        //   temps.length()   → błąd kompilacji
        //   "abc".length     → błąd kompilacji
        String word = "abc"; // word = słowo
        show("word.length()", word.length());
        // WYNIK: word.length() → 3

        int[] empty = new int[0]; // empty = pusta; tablica o długości 0 jest poprawna (i przydatna!)
        show("new int[0]", Arrays.toString(empty) + ", length = " + empty.length);
        // WYNIK: new int[0] → [], length = 0

        // length jest stałe (final): „temps.length = 10;” to błąd kompilacji. Rozmiaru nie da się zmienić.
    }

    // =================================================================================================
    // 3. WARTOŚCI DOMYŚLNE
    // =================================================================================================

    /**
     * 3. Nowa tablica jest wypełniona wartościami domyślnymi: zerami, false albo null.
     */
    static void defaultValues() {
        section("3. Wartości domyślne elementów");

        show("int[3]", Arrays.toString(new int[3]));
        // WYNIK: int[3] → [0, 0, 0]
        show("double[3]", Arrays.toString(new double[3]));
        // WYNIK: double[3] → [0.0, 0.0, 0.0]
        show("boolean[3]", Arrays.toString(new boolean[3]));
        // WYNIK: boolean[3] → [false, false, false]
        show("String[3]", Arrays.toString(new String[3]));
        // WYNIK: String[3] → [null, null, null]
        char[] letters = new char[2]; // letters = litery
        show("char[2]: kod pierwszego znaku", (int) letters[0]);
        // WYNIK: char[2]: kod pierwszego znaku → 0

        // Tabela wartości domyślnych:
        //   byte, short, int, long → 0      float, double → 0.0      boolean → false
        //   char → znak o kodzie 0 (niewidoczny)                       każdy obiekt (String, Integer, tablica...) → null
        // PUŁAPKA: zmienne LOKALNE nie mają wartości domyślnych (int x; System.out.println(x); → błąd kompilacji),
        // ale ELEMENTY tablicy zawsze je mają.
    }

    // =================================================================================================
    // 4. INDEKSY I GRANICE
    // =================================================================================================

    /**
     * 4. Elementy odczytujemy i zapisujemy przez indeks w nawiasach kwadratowych.
     * Indeks spoza zakresu 0..length-1 kończy się wyjątkiem.
     */
    static void indexesAndBounds() {
        section("4. Indeksy od 0 i wyjście poza tablicę");

        String[] colors = {"czerwony", "zielony", "niebieski"}; // colors = kolory
        show("colors[0]", colors[0]);
        // WYNIK: colors[0] → czerwony
        show("colors[2]", colors[2]);
        // WYNIK: colors[2] → niebieski

        colors[1] = "żółty";   // zapis: podmieniamy zawartość szafki nr 1
        show("po colors[1] = \"żółty\"", Arrays.toString(colors));
        // WYNIK: po colors[1] = "żółty" → [czerwony, żółty, niebieski]

        // Dlaczego od 0? Indeks to PRZESUNIĘCIE od początku tablicy: pierwszy element jest 0 kroków od początku.

        // PUŁAPKA: indeks równy length (albo ujemny) nie istnieje.
        expectThrows("colors[3]", () -> System.out.println(colors[3]));
        // WYNIK: ✔ colors[3] → rzucono ArrayIndexOutOfBoundsException: Index 3 out of bounds for length 3
        expectThrows("colors[-1]", () -> System.out.println(colors[-1]));
        // WYNIK: ✔ colors[-1] → rzucono ArrayIndexOutOfBoundsException: Index -1 out of bounds for length 3

        // Kompilator tego NIE wyłapie — sprawdzenie następuje dopiero w czasie działania programu.
        // DOBRA PRAKTYKA: gdy indeks pochodzi „z zewnątrz” (od użytkownika, z pliku), sprawdź go przed użyciem:
        int requested = 7; // requested = żądany indeks
        if (requested >= 0 && requested < colors.length) {
            System.out.println("kolor: " + colors[requested]);
        } else {
            System.out.println("nie ma koloru o numerze " + requested);
        }
        // WYNIK: nie ma koloru o numerze 7
    }

    // =================================================================================================
    // 5. PRZECHODZENIE PO TABLICY
    // =================================================================================================

    /**
     * 5. Zwykły for z indeksem — gdy potrzebujesz pozycji albo zmieniasz elementy.
     * for-each — gdy tylko czytasz wszystkie elementy po kolei.
     */
    static void iterating() {
        section("5. Przechodzenie po tablicy: for i for-each");

        int[] numbers = {4, 8, 15, 16, 23, 42}; // numbers = liczby
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = numbers[i] * 2;         // ZMIANA elementów — potrzebny indeks
        }
        show("po podwojeniu", Arrays.toString(numbers));
        // WYNIK: po podwojeniu → [8, 16, 30, 32, 46, 84]

        int sum = 0;
        for (int n : numbers) {                  // tylko ODCZYT — for-each wystarczy
            sum += n;
        }
        show("suma", sum);
        // WYNIK: suma → 216

        for (int i = numbers.length - 1; i >= 0; i--) { // od końca — tylko zwykły for
            System.out.print(numbers[i] + " ");
        }
        System.out.println();
        // WYNIK: 84 46 32 30 16 8

        for (int i = 0; i < 3; i++) {            // pozycja potrzebna do wypisania
            System.out.println("[" + i + "] = " + numbers[i]);
        }
        // WYNIK: [0] = 8
        // WYNIK: [1] = 16
        // WYNIK: [2] = 30

        // PUŁAPKA: w for-each zmienna n to KOPIA elementu — „n = 0” nie zmieni tablicy (Control03Loops, sekcja 5).
    }

    // =================================================================================================
    // 6. WYPISYWANIE TABLIC
    // =================================================================================================

    /**
     * 6. System.out.println(tablica) nie wypisuje elementów, tylko typ i „adres”. Do wypisania
     * zawartości służy Arrays.toString.
     */
    static void printingArrays() {
        section("6. Wypisywanie: println(tablica) kontra Arrays.toString");

        int[] data = {1, 2, 3}; // data = dane
        String raw = String.valueOf(data); // valueOf = wartość jako tekst — to samo, co wypisałby println(data)
        show("println(data) daje tekst zaczynający się od \"[I@\"", raw.startsWith("[I@")); // starts with = zaczyna się od
        // WYNIK: println(data) daje tekst zaczynający się od "[I@" → true
        // Pełny tekst to np. [I@1b6d3586 — liczba po @ zmienia się przy każdym uruchomieniu (wynik zależy od uruchomienia).
        // [I oznacza „tablica intów”, a liczba po @ to skrót (hashCode) obiektu w zapisie szesnastkowym.
        // Tablica NIE ma własnego czytelnego toString — dostaje domyślny z klasy Object.

        show("Arrays.toString(data)", Arrays.toString(data));
        // WYNIK: Arrays.toString(data) → [1, 2, 3]

        // Wyjątek: tablica znaków char[] ma osobną wersję println, która wypisuje znaki.
        char[] hello = {'J', 'a', 'v', 'a'}; // hello = cześć
        System.out.println(hello);
        // WYNIK: Java
        // PUŁAPKA: ale sklejenie z tekstem już NIE — wtedy znowu dostajemy typ i adres ([C@...).
        String glued = "tekst: " + hello; // glued = sklejony
        show("\"tekst: \" + hello zaczyna się od \"tekst: [C@\"", glued.startsWith("tekst: [C@"));
        // WYNIK: "tekst: " + hello zaczyna się od "tekst: [C@" → true

        // DOBRA PRAKTYKA: tablicę do wypisania zawsze przepuść przez Arrays.toString
        // (a dwuwymiarową przez Arrays.deepToString — t03_arrays/Arrays02MultiDim).
    }

    // =================================================================================================
    // 7. TABLICE SĄ OBIEKTAMI: ALIAS A KOPIA
    // =================================================================================================

    /**
     * 7. Przypisanie tablicy do innej zmiennej kopiuje REFERENCJĘ — obie zmienne wskazują tę samą tablicę.
     * Prawdziwa kopia wymaga utworzenia nowej tablicy i przepisania elementów.
     */
    static void arraysAreObjects() {
        section("7. Tablica to obiekt: alias a kopia");

        int[] original = {10, 20, 30}; // original = oryginał
        int[] alias = original;        // alias = druga nazwa; kopiujemy tylko ADRES
        alias[0] = 99;
        show("original po zmianie przez alias", Arrays.toString(original));
        // WYNIK: original po zmianie przez alias → [99, 20, 30]
        show("alias == original", alias == original);
        // WYNIK: alias == original → true

        int[] copy = new int[original.length]; // copy = kopia — NOWE szafki
        for (int i = 0; i < original.length; i++) {
            copy[i] = original[i];
        }
        copy[1] = -1;
        show("original", Arrays.toString(original));
        // WYNIK: original → [99, 20, 30]
        show("copy", Arrays.toString(copy));
        // WYNIK: copy → [99, -1, 30]
        // Szybciej: Arrays.copyOf(original, original.length) albo original.clone() — t03_arrays/Arrays03Utility.

        // Metody dostają KOPIĘ REFERENCJI (t01_basics/Basics09PassByValue):
        int[] points = {1, 2, 3}; // points = punkty
        fillWithZeros(points);    // fill with zeros = wypełnij zerami
        show("po fillWithZeros(points)", Arrays.toString(points));
        // WYNIK: po fillWithZeros(points) → [0, 0, 0]
        replaceArray(points);     // replace array = podmień tablicę
        // WYNIK: w środku replaceArray: [7, 7, 7]
        show("po replaceArray(points)", Arrays.toString(points));
        // WYNIK: po replaceArray(points) → [0, 0, 0]
        // Metoda może ZMIENIĆ ZAWARTOŚĆ cudzej tablicy, ale nie może podmienić tablicy, na którą wskazuje wywołujący.

        // == porównuje adresy, nie zawartość. Metoda equals na tablicy — też adresy!
        int[] a = {1, 2};
        int[] b = {1, 2};
        show("a == b", a == b);
        // WYNIK: a == b → false
        show("a.equals(b)", a.equals(b));
        // WYNIK: a.equals(b) → false
        show("Arrays.equals(a, b)", Arrays.equals(a, b));
        // WYNIK: Arrays.equals(a, b) → true
    }

    /** Zmienia elementy tablicy przekazanej przez wywołującego — zmiana będzie widoczna na zewnątrz. */
    static void fillWithZeros(int[] target) {
        for (int i = 0; i < target.length; i++) {
            target[i] = 0;
        }
    }

    /** Podmienia tylko LOKALNĄ kopię referencji — wywołujący tego nie zobaczy. */
    static void replaceArray(int[] target) {
        target = new int[] {7, 7, 7};
        System.out.println("   w środku replaceArray: " + Arrays.toString(target));
    }

    // =================================================================================================
    // 8. PUŁAPKA: NULL W TABLICY OBIEKTÓW
    // =================================================================================================

    /**
     * 8. Tablica obiektów (np. String[]) po utworzeniu zawiera same null. Wywołanie metody na null
     * kończy się NullPointerException.
     */
    static void nullElementsPitfall() {
        section("8. PUŁAPKA: null w tablicy Stringów");

        String[] names = new String[4]; // names = imiona; 4 miejsca, na razie same null
        names[0] = "Ala";
        names[1] = "Olek";
        show("names", Arrays.toString(names));
        // WYNIK: names → [Ala, Olek, null, null]

        expectThrows("suma długości imion (bez sprawdzania null)", () -> {
            int total = 0;
            for (String name : names) {
                total += name.length();
            }
            System.out.println(total);
        });
        // WYNIK: ✔ suma długości imion (bez sprawdzania null) → rzucono NullPointerException: Cannot invoke "String.length()" because "name" is null

        int total = 0; // total = suma
        for (String name : names) {
            if (name == null) {
                continue;             // puste miejsce — pomijamy
            }
            total += name.length();
        }
        show("suma długości (z pominięciem null)", total);
        // WYNIK: suma długości (z pominięciem null) → 7

        // DOBRA PRAKTYKA: jeśli tablica może mieć „dziury”, sprawdzaj null. Jeszcze lepiej: nie zostawiaj dziur
        // — trzymaj osobny licznik zajętych miejsc albo użyj listy (ArrayList), która ma dokładnie tyle elementów, ile dodasz.
    }

    // =================================================================================================
    // 9. STAŁY ROZMIAR — TABLICA NIE ROŚNIE
    // =================================================================================================

    /**
     * 9. Do tablicy nie da się „dodać” elementu. Trzeba utworzyć większą tablicę i przepisać elementy.
     * Dlatego w codziennym kodzie częściej używa się ArrayList.
     */
    static void fixedSize() {
        section("9. Stały rozmiar: „dodawanie” = nowa tablica");

        int[] cart = {5, 7}; // cart = koszyk
        // cart[2] = 9;       ← ArrayIndexOutOfBoundsException; metody cart.add(9) nie ma
        int[] bigger = new int[cart.length + 1]; // bigger = większa
        for (int i = 0; i < cart.length; i++) {
            bigger[i] = cart[i];
        }
        bigger[bigger.length - 1] = 9;
        cart = bigger;       // zmienna cart wskazuje teraz nową, dłuższą tablicę
        show("cart po „dodaniu” 9", Arrays.toString(cart));
        // WYNIK: cart po „dodaniu” 9 → [5, 7, 9]
        // Każde „dodanie” kopiuje całą tablicę. Przy tysiącach elementów to dużo pracy.

        // Zapowiedź: ArrayList sama powiększa się w tle. <Integer> = typ elementów (generyki: t11_generics/Generics01Why).
        ArrayList<Integer> growing = new ArrayList<>(); // growing = rosnąca
        growing.add(5);  // add = dodaj
        growing.add(7);
        growing.add(9);
        show("ArrayList po trzech add", growing);
        // WYNIK: ArrayList po trzech add → [5, 7, 9]
        // Szczegóły: t12_collections/Collections02Lists.

        // DOBRA PRAKTYKA: tablica, gdy rozmiar jest znany i stały (dni tygodnia, plansza 3×3, piksele obrazka);
        // lista, gdy elementów przybywa lub ubywa.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Tworzenie: new int[5] (zera) | int[] a = {1, 2, 3} (tylko w deklaracji) | a = new int[] {1, 2, 3}.
     *   • Indeksy 0 .. length - 1; poza zakresem → ArrayIndexOutOfBoundsException; rozmiar ujemny → NegativeArraySizeException.
     *   • a.length — POLE (bez nawiasów); tekst.length() — METODA.
     *   • Domyślnie: 0 / 0.0 / false / znak o kodzie 0 / null (dla obiektów).
     *   • Wypisywanie: Arrays.toString(a). println(a) daje „[I@...”, wyjątek: println(char[]) wypisuje znaki.
     *   • b = a to ALIAS (ta sama tablica). Kopia: nowa tablica + przepisanie (albo Arrays.copyOf / clone).
     *   • a == b i a.equals(b) porównują adresy; zawartość: Arrays.equals(a, b).
     *   • Metoda może zmienić elementy przekazanej tablicy, ale nie podmieni tablicy wywołującego.
     *   • String[] po new zawiera null — sprawdzaj przed wywołaniem metody.
     *   • Rozmiar jest stały. Potrzebujesz rosnącej kolekcji → ArrayList.
     *
     * PYTANIA KONTROLNE:
     *   1. Jaki jest indeks ostatniego elementu tablicy o długości n?
     *   2. Co wypisze:  int[] a = new int[3];  a[1] = 5;  System.out.println(Arrays.toString(a));  ?
     *   3. Co wypisze:  int[] a = {1, 2};  int[] b = a;  b[0] = 9;  System.out.println(a[0]);  ?
     *   4. ZNAJDŹ BŁĄD:  int[] t = {3, 1, 2};  for (int i = 0; i <= t.length; i++) { System.out.println(t[i]); }
     *   5. ZNAJDŹ BŁĄD:  int[] t;  t = {1, 2, 3};
     *   6. Dlaczego System.out.println(tablica) nie pokazuje elementów? Co zrobić zamiast tego?
     *   7. Co wypisze:  String[] s = new String[2];  System.out.println(s[0].length());  ?
     *   8. Kiedy lepiej użyć tablicy, a kiedy ArrayList?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: kwadraty 1..5 oraz dla 0", "[1, 4, 9, 16, 25] | []",
                () -> Arrays.toString(exercise1(5)) + " | " + Arrays.toString(exercise1(0)));
        Check.equal("ćw. 2: suma długości bez null", 6,
                () -> exercise2(new String[] {"Ala", null, "kot", ""}));
        Check.equal("ćw. 3: kopia zapasowa / oryginał", "[5, 6, 7] / [0, 6, 7]", () -> {
            int[] data = {5, 6, 7};
            int[] backup = exercise3(data);
            return Arrays.toString(backup) + " / " + Arrays.toString(data);
        });
        Check.equal("ćw. 4: dopisanie na końcu", "[1, 2, 3, 4] | [7]",
                () -> Arrays.toString(exercise4(new int[] {1, 2, 3}, 4)) + " | " + Arrays.toString(exercise4(new int[0], 7)));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "[1, 4, 9, 16, 25] | []",
                () -> Arrays.toString(solution1(5)) + " | " + Arrays.toString(solution1(0)));
        Check.equal("ćw. 2 (wzorzec)", 6,
                () -> solution2(new String[] {"Ala", null, "kot", ""}));
        Check.equal("ćw. 3 (wzorzec)", "[5, 6, 7] / [0, 6, 7]", () -> {
            int[] data = {5, 6, 7};
            int[] backup = solution3(data);
            return Arrays.toString(backup) + " / " + Arrays.toString(data);
        });
        Check.equal("ćw. 4 (wzorzec)", "[1, 2, 3, 4] | [7]",
                () -> Arrays.toString(solution4(new int[] {1, 2, 3}, 4)) + " | " + Arrays.toString(solution4(new int[0], 7)));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć tablicę kwadratów liczb od 1 do n: dla 5 → [1, 4, 9, 16, 25], dla 0 → [].
     * Podpowiedź: new int[n], a w pętli po i od 0: element i to (i + 1) * (i + 1).
     */
    static int[] exercise1(int n) {
        // TODO: twoje rozwiązanie
        return new int[0];
    }

    /**
     * ĆWICZENIE 2 (średnie): zsumuj długości wszystkich tekstów w tablicy, pomijając elementy null.
     * Podpowiedź: for-each + klauzula strażnika „if (w == null) continue;”.
     */
    static int exercise2(String[] words) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ tak, żeby kopia zapasowa była PRAWDZIWĄ kopią (dziś jest aliasem
     * i zeruje się razem z oryginałem). Wynik: kopia zachowuje stare wartości, oryginał ma 0 na pozycji 0.
     * <pre>{@code
     * int[] backup = original;   // „kopia zapasowa”
     * original[0] = 0;
     * return backup;
     * }</pre>
     * Podpowiedź: new int[original.length] + pętla przepisująca elementy — PRZED wyzerowaniem original[0].
     */
    static int[] exercise3(int[] original) {
        // TODO: twoje rozwiązanie
        return new int[0];
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zwróć NOWĄ tablicę dłuższą o 1, z value dopisanym na końcu.
     * Oryginał ma zostać bez zmian. Dla pustej tablicy i 7 → [7].
     * Podpowiedź: jak w sekcji 9 — new int[array.length + 1], przepisz elementy, ostatni ustaw na value.
     */
    static int[] exercise4(int[] array, int value) {
        // TODO: twoje rozwiązanie
        return new int[0];
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int[] solution1(int n) {
        int[] result = new int[n];
        for (int i = 0; i < n; i++) {
            result[i] = (i + 1) * (i + 1);
        }
        return result;
    }

    static int solution2(String[] words) {
        int total = 0;
        for (String w : words) {
            if (w == null) {
                continue;
            }
            total += w.length();
        }
        return total;
    }

    static int[] solution3(int[] original) {
        int[] backup = new int[original.length];
        for (int i = 0; i < original.length; i++) {
            backup[i] = original[i];
        }
        original[0] = 0;
        return backup;
    }

    static int[] solution4(int[] array, int value) {
        int[] result = new int[array.length + 1];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        result[result.length - 1] = value;
        return result;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. n - 1 (indeksy liczymy od 0).
     *   2. [0, 5, 0] — pozostałe elementy mają wartość domyślną 0.
     *   3. 9 — b to alias tej samej tablicy (skopiowany został adres, nie elementy).
     *   4. Warunek i <= t.length sięga do t[3], którego nie ma → ArrayIndexOutOfBoundsException. Poprawnie: i < t.length.
     *   5. Krótki inicjalizator { } działa tylko w deklaracji. Poprawnie: t = new int[] {1, 2, 3};
     *   6. Tablica nie ma własnego toString, więc wypisuje się typ i skrót adresu (np. [I@1b6d3586).
     *      Zawartość: Arrays.toString(tablica), dla 2D — Arrays.deepToString.
     *   7. Nic — poleci NullPointerException, bo s[0] to null (wartość domyślna dla obiektów).
     *   8. Tablica: stały, znany rozmiar, prymitywy, wydajność (np. plansza, miesiące roku).
     *      ArrayList: gdy liczba elementów się zmienia (dodawanie, usuwanie).
     */
    // </editor-fold>
}
