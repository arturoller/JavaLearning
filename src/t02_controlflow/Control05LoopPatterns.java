package t02_controlflow;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Product;

import java.util.Locale;
import java.util.Scanner;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Wzorce pętli — gotowe „przepisy”, które wracają w każdym programie
 *        (pattern = wzorzec; accumulator = akumulator, zmienna zbierająca wynik)
 *
 * W SKRÓCIE:
 *   Większość pętli w prawdziwych programach to kilka powtarzalnych schematów: suma i licznik,
 *   minimum/maksimum, szukanie pierwszego pasującego, zliczanie, budowanie tekstu, pętle zagnieżdżone,
 *   przetwarzanie cyfr liczby, walidacja danych i menu. Gdy rozpoznasz schemat, kod pisze się sam.
 *
 * ANALOGIA: kasjerka w sklepie.
 *   Suma = kasa nabija kolejne ceny na jeden licznik. Licznik = „ile produktów”.
 *   Maksimum = zapamiętuje najdroższy produkt, podmieniając go, gdy trafi się droższy.
 *   Pierwszy pasujący = „szukam czegoś z alkoholem — jest! Proszę dowód” i dalej nie szuka.
 *
 * JAK TO DZIAŁA:
 *   Szkielet każdego akumulatora:
 *   1. PRZED pętlą: zmienna startowa (suma = 0, licznik = 0, max = pierwszy element, tekst = "").
 *   2. W pętli:     aktualizacja dla każdego elementu (suma += x, licznik++, if (x {@code >} max) max = x).
 *   3. PO pętli:    wynik gotowy (ewentualnie jeszcze średnia = suma / licznik).
 *
 * SŁÓWKA:
 *   sum = suma; count = licznik; average = średnia; min/max = najmniejszy/największy; index = indeks (pozycja);
 *   first match = pierwszy pasujący; any match = jakikolwiek pasuje; occurrence = wystąpienie;
 *   digit = cyfra; reverse = odwrócić; validation = walidacja (sprawdzenie poprawności); menu = menu
 *
 * ZOBACZ TEŻ: t02_controlflow/Control03Loops (rodzaje pętli), t02_controlflow/Control04BreakContinueLabels,
 *   t03_arrays/Arrays04Algorithms (algorytmy na tablicach), t04_strings/Strings03StringBuilder,
 *   t16_streams/Streams02Creation (te same wzorce zapisane strumieniami)
 * </pre>
 */
public class Control05LoopPatterns {

    public static void main(String[] args) {
        title("Control05 — wzorce pętli");

        accumulators();         // accumulators = akumulatory (suma, licznik, średnia)
        minMaxWithIndex();      // min max with index = minimum i maksimum z pozycją
        firstAndAnyMatch();     // first and any match = pierwszy pasujący / czy jakikolwiek
        countingInString();     // counting in string = zliczanie w tekście
        buildingStrings();      // building strings = budowanie tekstu
        nestedLoopsPrinting();  // nested loops printing = wypisywanie pętlami zagnieżdżonymi
        fizzBuzz();             // fizz buzz = FizzBuzz (klasyczne zadanie)
        digitsOfNumber();       // digits of number = cyfry liczby
        inputValidation();      // input validation = walidacja danych wejściowych
        textMenu();             // text menu = menu tekstowe
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. AKUMULATORY: SUMA, LICZNIK, ŚREDNIA
    // =================================================================================================

    /**
     * 1. Akumulator: zmienna ustawiona przed pętlą i aktualizowana w każdym obrocie.
     */
    static void accumulators() {
        section("1. Suma, licznik, średnia");

        int[] scores = {72, 95, 58, 88, 65, 91}; // scores = wyniki punktowe
        int sum = 0;      // sum = suma — start od 0 (element neutralny dodawania)
        int count = 0;    // count = licznik
        int passed = 0;   // passed = zdało (licznik warunkowy)
        for (int score : scores) {
            sum += score;
            count++;
            if (score >= 60) {
                passed++;
            }
        }
        show("suma", sum);
        // WYNIK: suma → 469
        show("liczba wyników", count);
        // WYNIK: liczba wyników → 6
        show("zdało (od 60 pkt)", passed);
        // WYNIK: zdało (od 60 pkt) → 5

        // PUŁAPKA: int / int to dzielenie CAŁKOWITE (t01_basics/Basics04Operators) — ułamek znika.
        show("sum / count (int)", sum / count);
        // WYNIK: sum / count (int) → 78
        double average = (double) sum / count; // average = średnia; rzutowanie PRZED dzieleniem
        System.out.println(String.format(Locale.ROOT, "średnia: %.2f", average)); // format = sformatuj
        // WYNIK: średnia: 78.17

        // PUŁAPKA: pusta tablica → count == 0. Dla int dzielenie przez 0 rzuca ArithmeticException,
        // dla double daje NaN (Not a Number = „nie liczba”). Zawsze obsłuż ten przypadek:
        int[] empty = {};
        double safeAverage = empty.length == 0 ? 0.0 : (double) sum / empty.length; // safe = bezpieczna
        show("średnia z pustej tablicy (zabezpieczona)", safeAverage);
        // WYNIK: średnia z pustej tablicy (zabezpieczona) → 0.0
    }

    // =================================================================================================
    // 2. MINIMUM I MAKSIMUM Z INDEKSEM
    // =================================================================================================

    /**
     * 2. Minimum/maksimum: startujemy od PIERWSZEGO elementu, nie od zera.
     * Zapamiętujemy też indeks, żeby wiedzieć, GDZIE był rekord.
     */
    static void minMaxWithIndex() {
        section("2. Minimum i maksimum z pozycją");

        int[] temps = {3, -2, 7, -5, 4, 7}; // temps = temperatury kolejnych dni
        int min = temps[0];
        int minIndex = 0;  // min index = pozycja minimum
        int max = temps[0];
        int maxIndex = 0;  // max index = pozycja maksimum
        for (int i = 1; i < temps.length; i++) {   // od 1, bo element 0 już „obejrzeliśmy”
            if (temps[i] < min) {
                min = temps[i];
                minIndex = i;
            }
            if (temps[i] > max) {
                max = temps[i];
                maxIndex = i;
            }
        }
        show("minimum", min + " (dzień " + minIndex + ")");
        // WYNIK: minimum → -5 (dzień 3)
        show("maksimum", max + " (dzień " + maxIndex + ")");
        // WYNIK: maksimum → 7 (dzień 2)
        // 7 występuje dwa razy (dzień 2 i 5). Warunek > zapamiętuje PIERWSZE wystąpienie, >= zapamiętałby ostatnie.

        // PUŁAPKA: start od 0 zamiast od pierwszego elementu. Dla samych dodatnich liczb minimum wyjdzie 0,
        // chociaż 0 w ogóle nie ma w danych!
        int[] prices = {30, 80, 50}; // prices = ceny
        int badMin = 0;  // bad min = złe minimum
        for (int p : prices) {
            if (p < badMin) {
                badMin = p;
            }
        }
        show("minimum startujące od 0 (błąd)", badMin);
        // WYNIK: minimum startujące od 0 (błąd) → 0

        // DOBRA PRAKTYKA: start od pierwszego elementu albo od Integer.MAX_VALUE (dla min) / Integer.MIN_VALUE (dla max).
        int goodMin = Integer.MAX_VALUE; // MAX_VALUE = największy możliwy int — każdy element będzie mniejszy
        for (int p : prices) {
            goodMin = Math.min(goodMin, p);   // Math.min = mniejsza z dwóch (t01_basics/Basics07MathRandom)
        }
        show("minimum startujące od MAX_VALUE", goodMin);
        // WYNIK: minimum startujące od MAX_VALUE → 30
    }

    // =================================================================================================
    // 3. PIERWSZY PASUJĄCY, CZY JAKIKOLWIEK, CZY WSZYSTKIE
    // =================================================================================================

    /**
     * 3. Szukanie: zapamiętaj znaleziony element i przerwij pętlę (break) — dalsze szukanie nic nie zmieni.
     */
    static void firstAndAnyMatch() {
        section("3. Pierwszy pasujący, jakikolwiek, wszystkie");

        int[] orderValues = {120, 80, 450, 60, 999}; // order values = wartości zamówień
        int firstBig = -1; // first big = pierwsze duże; -1 = „nie znaleziono” (wartość, która nie wystąpi w danych)
        for (int v : orderValues) {
            if (v > 400) {
                firstBig = v;
                break;
            }
        }
        show("pierwsze zamówienie powyżej 400", firstBig);
        // WYNIK: pierwsze zamówienie powyżej 400 → 450

        boolean anyFree = false; // any free = czy jakiekolwiek darmowe; start: „nie ma” — szukamy przykładu
        for (int v : orderValues) {
            if (v == 0) {
                anyFree = true;
                break;
            }
        }
        show("czy jest zamówienie za 0 zł", anyFree);
        // WYNIK: czy jest zamówienie za 0 zł → false

        boolean allPositive = true; // all positive = czy wszystkie dodatnie; start: „tak” — szukamy KONTRprzykładu
        for (int v : orderValues) {
            if (v <= 0) {
                allPositive = false;
                break;
            }
        }
        show("czy wszystkie dodatnie", allPositive);
        // WYNIK: czy wszystkie dodatnie → true

        // To samo na liście produktów z SampleData — pierwszy produkt, którego nie ma w magazynie:
        String firstMissing = "brak"; // first missing = pierwszy brakujący
        for (Product p : SampleData.products()) {
            if (p.stock() == 0) {
                firstMissing = p.name();
                break;
            }
        }
        show("pierwszy niedostępny produkt", firstMissing);
        // WYNIK: pierwszy niedostępny produkt → Smartfon X

        // DOBRA PRAKTYKA: „any” startuje od false i szuka przykładu; „all” startuje od true i szuka kontrprzykładu.
        // W obu przypadkach break, gdy odpowiedź jest już pewna.
    }

    // =================================================================================================
    // 4. ZLICZANIE WYSTĄPIEŃ W TEKŚCIE
    // =================================================================================================

    /**
     * 4. Tekst to ciąg znaków: pętla po indeksach 0..length()-1 i charAt(i) daje każdy znak.
     */
    static void countingInString() {
        section("4. Zliczanie znaków w tekście (charAt)");

        String sentence = "Ala ma kota, a kot ma Alę"; // sentence = zdanie
        int smallA = 0;    // small a = małe „a”
        int anyA = 0;      // any a = „a” niezależnie od wielkości litery
        int vowels = 0;    // vowels = samogłoski
        for (int i = 0; i < sentence.length(); i++) {
            char c = sentence.charAt(i);                  // c = znak (character)
            if (c == 'a') {
                smallA++;
            }
            char lower = Character.toLowerCase(c);        // to lower case = na małą literę
            if (lower == 'a') {
                anyA++;
            }
            switch (lower) {
                case 'a', 'e', 'i', 'o', 'u', 'y', 'ą', 'ę', 'ó' -> vowels++;
                default -> { }                            // inne znaki pomijamy
            }
        }
        show("małych 'a'", smallA);
        // WYNIK: małych 'a' → 5
        show("wszystkich 'a' (bez względu na wielkość)", anyA);
        // WYNIK: wszystkich 'a' (bez względu na wielkość) → 7
        show("samogłosek", vowels);
        // WYNIK: samogłosek → 10

        // PUŁAPKA: char porównujemy przez == i apostrofy 'a'. "a" w cudzysłowie to String — c == "a" się nie skompiluje.
        // Więcej o znakach i Unicode: t04_strings/Strings06CharUnicode.
    }

    // =================================================================================================
    // 5. BUDOWANIE TEKSTU W PĘTLI: += KONTRA STRINGBUILDER
    // =================================================================================================

    /**
     * 5. Tekst budowany w pętli: += działa, ale przy każdym obrocie tworzy NOWY obiekt String.
     * StringBuilder dopisuje do jednego bufora — dla długich pętli jest dużo szybszy.
     */
    static void buildingStrings() {
        section("5. Budowanie tekstu: += kontra StringBuilder");

        String csv = ""; // csv = wartości rozdzielone przecinkami (comma-separated values)
        for (int i = 1; i <= 5; i++) {
            csv += i;
            if (i < 5) {
                csv += ",";   // separator (rozdzielacz) między elementami, ale nie po ostatnim
            }
        }
        show("+= w pętli", csv);
        // WYNIK: += w pętli → 1,2,3,4,5

        StringBuilder sb = new StringBuilder(); // StringBuilder = budowniczy tekstu (zmienny bufor znaków)
        for (int i = 1; i <= 5; i++) {
            if (sb.length() > 0) {
                sb.append(",");                  // append = dopisz na końcu
            }
            sb.append(i);
        }
        show("StringBuilder", sb.toString());   // toString = zamień na String
        // WYNIK: StringBuilder → 1,2,3,4,5

        // Dlaczego to ważne? String jest NIEZMIENNY (immutable). csv += i nie dopisuje do istniejącego tekstu,
        // tylko tworzy nowy, kopiując cały stary. Dla 100 000 obrotów to 100 000 coraz dłuższych kopii.
        // DOBRA PRAKTYKA: kilka sklejeń — spokojnie +. Pętla z setkami obrotów — StringBuilder.
        // Szczegóły: t04_strings/Strings03StringBuilder.
    }

    // =================================================================================================
    // 6. PĘTLE ZAGNIEŻDŻONE: TABLICZKA MNOŻENIA I TRÓJKĄT
    // =================================================================================================

    /**
     * 6. Pętla w pętli: zewnętrzna wybiera wiersz, wewnętrzna wypełnia kolumny tego wiersza.
     * printf z szerokością (%4d) wyrównuje kolumny.
     */
    static void nestedLoopsPrinting() {
        section("6. Pętle zagnieżdżone: tabliczka mnożenia i trójkąt");

        System.out.print("   |");
        for (int col = 1; col <= 5; col++) {
            System.out.printf(Locale.ROOT, "%4d", col);   // %4d = liczba na 4 znakach, wyrównana do prawej
        }
        System.out.println();
        System.out.println("---+--------------------");
        for (int row = 1; row <= 5; row++) {
            System.out.printf(Locale.ROOT, "%2d |", row);
            for (int col = 1; col <= 5; col++) {
                System.out.printf(Locale.ROOT, "%4d", row * col);
            }
            System.out.println();                       // koniec wiersza — dopiero po pętli wewnętrznej
        }
        // WYNIK:    |   1   2   3   4   5
        // WYNIK: ---+--------------------
        // WYNIK:  1 |   1   2   3   4   5
        // WYNIK:  2 |   2   4   6   8  10
        // WYNIK:  3 |   3   6   9  12  15
        // WYNIK:  4 |   4   8  12  16  20
        // WYNIK:  5 |   5  10  15  20  25
        // Wewnętrzna pętla wykonała się 5 × 5 = 25 razy. Dwie pętle po n → n² obrotów — pamiętaj o tym przy dużym n.

        // Trójkąt: w wierszu r najpierw (4 - r) spacji, potem (2r - 1) gwiazdek.
        int height = 4; // height = wysokość
        for (int r = 1; r <= height; r++) {
            for (int s = 0; s < height - r; s++) {
                System.out.print(" ");
            }
            for (int st = 0; st < 2 * r - 1; st++) {
                System.out.print("*");
            }
            System.out.println();
        }
        // WYNIK:    *
        // WYNIK:   ***
        // WYNIK:  *****
        // WYNIK: *******

        // DOBRA PRAKTYKA: przy wzorach z gwiazdek rozpisz najpierw tabelkę: wiersz → ile spacji → ile gwiazdek.
        // Wzór (height - r, 2r - 1) wynika wprost z tej tabelki.
    }

    // =================================================================================================
    // 7. FIZZBUZZ
    // =================================================================================================

    /**
     * 7. FizzBuzz: liczby 1..15, ale podzielne przez 3 → Fizz, przez 5 → Buzz, przez 3 i 5 → FizzBuzz.
     * Klasyczne zadanie rekrutacyjne — sprawdza pętlę, operator % i kolejność warunków.
     */
    static void fizzBuzz() {
        section("7. FizzBuzz — kolejność warunków ma znaczenie");

        StringBuilder line = new StringBuilder(); // line = linia wyniku
        for (int i = 1; i <= 15; i++) {
            if (i % 15 == 0) {            // najpierw przypadek NAJWĘŻSZY (podzielne przez 3 i przez 5)
                line.append("FizzBuzz");
            } else if (i % 3 == 0) {
                line.append("Fizz");
            } else if (i % 5 == 0) {
                line.append("Buzz");
            } else {
                line.append(i);
            }
            line.append(" ");
        }
        System.out.println(line);
        // WYNIK: 1 2 Fizz 4 Buzz Fizz 7 8 Fizz Buzz 11 Fizz 13 14 FizzBuzz

        // PUŁAPKA: gdy „i % 3 == 0” stoi PIERWSZE, 15 wypisze się jako „Fizz”, a FizzBuzz nie pojawi się nigdy
        // — dokładnie ten sam błąd kolejności co w ocenach z Control01IfElse.
    }

    // =================================================================================================
    // 8. CYFRY LICZBY: % 10 I / 10
    // =================================================================================================

    /**
     * 8. n % 10 daje ostatnią cyfrę, n / 10 ją „odcina”. Pętla while (n {@code >} 0) przechodzi po wszystkich cyfrach.
     */
    static void digitsOfNumber() {
        section("8. Cyfry liczby: suma cyfr i odwracanie");

        int n = 40721;
        int digitSum = 0;   // digit sum = suma cyfr
        int reversed = 0;   // reversed = odwrócona liczba
        while (n > 0) {
            int digit = n % 10;               // digit = cyfra (ostatnia)
            digitSum += digit;
            reversed = reversed * 10 + digit; // przesuń dotychczasowy wynik w lewo i dopisz cyfrę
            n /= 10;                          // odetnij ostatnią cyfrę
        }
        // Ślad (trace):   n       digit   digitSum   reversed
        //                 40721   1       1          1
        //                 4072    2       3          12
        //                 407     7       10         127
        //                 40      0       10         1270
        //                 4       4       14         12704
        show("suma cyfr 40721", digitSum);
        // WYNIK: suma cyfr 40721 → 14
        show("40721 od tyłu", reversed);
        // WYNIK: 40721 od tyłu → 12704

        // PUŁAPKA: pętla zniszczyła n (teraz n == 0). Jeśli oryginał będzie potrzebny — pracuj na kopii.
        show("n po pętli", n);
        // WYNIK: n po pętli → 0
        // PUŁAPKA: odwracanie dużych liczb może przepełnić int (np. 1 999 999 999 → 9 999 999 991 nie mieści się w int).
    }

    // =================================================================================================
    // 9. WALIDACJA DANYCH W PĘTLI (SCANNER)
    // =================================================================================================

    /**
     * 9. Pytamy o dane tak długo, aż będą poprawne. Scanner czyta z przygotowanego tekstu zamiast z klawiatury,
     * więc wynik jest zawsze ten sam. W prawdziwym programie: new Scanner(System.in).
     */
    static void inputValidation() {
        section("9. Walidacja wejścia: pytaj, aż dane będą poprawne");

        Scanner in = new Scanner("abc\n-5\n200\n42\n17\n"); // in = wejście; „użytkownik” wpisał 5 linii
        in.useLocale(Locale.ROOT); // use locale = użyj ustawień regionalnych — liczby czytane zawsze tak samo
        int age = -1;       // age = wiek; -1 = „jeszcze nie mamy poprawnego”
        int attempts = 0;   // attempts = próby
        while (age == -1 && in.hasNext()) {    // has next = czy jest jeszcze coś do przeczytania
            attempts++;
            if (!in.hasNextInt()) {            // has next int = czy następny fragment to liczba całkowita
                String bad = in.next();        // next = weź następny fragment jako tekst (żeby go „zużyć”)
                System.out.println("„" + bad + "” to nie liczba — spróbuj jeszcze raz");
                continue;
            }
            int value = in.nextInt();          // value = wartość
            if (value < 0 || value > 120) {
                System.out.println(value + " — wiek spoza zakresu 0–120");
                continue;
            }
            age = value;                       // poprawna wartość → warunek pętli da false
        }
        in.close();
        // WYNIK: „abc” to nie liczba — spróbuj jeszcze raz
        // WYNIK: -5 — wiek spoza zakresu 0–120
        // WYNIK: 200 — wiek spoza zakresu 0–120
        show("przyjęty wiek", age);
        // WYNIK: przyjęty wiek → 42
        show("liczba prób", attempts);
        // WYNIK: liczba prób → 4
        // Linia „17” nie została przeczytana — pętla skończyła się po pierwszej poprawnej wartości.

        // PUŁAPKA: bez in.next() w gałęzi „to nie liczba” błędny fragment zostałby w Scannerze na zawsze
        // — hasNextInt() w kółko widziałby „abc” i pętla nigdy by się nie skończyła.
    }

    // =================================================================================================
    // 10. MENU TEKSTOWE STEROWANE POLECENIAMI
    // =================================================================================================

    /**
     * 10. Pętla + switch = najprostszy „program interaktywny”. Polecenia pochodzą z przygotowanego tekstu.
     */
    static void textMenu() {
        section("10. Menu tekstowe: pętla + switch");

        Scanner commands = new Scanner("dodaj 5\ndodaj 10\nusuń 3\nsaldo\nxyz\nkoniec\ndodaj 100\n"); // commands = polecenia
        int balance = 0;          // balance = saldo
        boolean running = true;   // running = program działa
        while (running && commands.hasNext()) {
            String cmd = commands.next();        // cmd = polecenie (pierwsze słowo)
            switch (cmd) {
                case "dodaj" -> {
                    int amount = commands.nextInt(); // amount = kwota (drugie słowo)
                    balance += amount;
                    System.out.println("+" + amount + " → saldo " + balance);
                }
                case "usuń" -> {
                    int amount = commands.nextInt();
                    balance -= amount;
                    System.out.println("-" + amount + " → saldo " + balance);
                }
                case "saldo" -> System.out.println("saldo: " + balance);
                case "koniec" -> {
                    System.out.println("koniec pracy");
                    running = false;              // flaga zamiast break — break w switchu kończy tylko switch!
                }
                default -> System.out.println("nieznane polecenie: " + cmd);
            }
        }
        commands.close();
        // WYNIK: +5 → saldo 5
        // WYNIK: +10 → saldo 15
        // WYNIK: -3 → saldo 12
        // WYNIK: saldo: 12
        // WYNIK: nieznane polecenie: xyz
        // WYNIK: koniec pracy
        show("saldo końcowe", balance);
        // WYNIK: saldo końcowe → 12
        // Polecenie „dodaj 100” po „koniec” nie zostało wykonane — pętla już się zakończyła.

        // DOBRA PRAKTYKA: każda gałąź menu to docelowo osobna metoda (t05_methods/Methods01Basics),
        // a pętla tylko czyta polecenie i wybiera metodę. Wtedy menu z 20 opcjami nadal jest czytelne.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Suma: sum = 0; w pętli sum += x.   Licznik: count = 0; w pętli count++ (ew. w if).
     *   • Średnia: (double) sum / count — rzutowanie PRZED dzieleniem; pusty zbiór danych obsłuż osobno.
     *   • Min/max: start od pierwszego elementu (albo Integer.MAX_VALUE / MIN_VALUE), zapamiętaj też indeks.
     *   • Pierwszy pasujący: zapamiętaj + break.  Any: start false.  All: start true, szukaj kontrprzykładu.
     *   • Znaki tekstu: for (int i = 0; i < s.length(); i++) { char c = s.charAt(i); ... }
     *   • Tekst w długiej pętli: StringBuilder.append(...), na końcu toString().
     *   • Pętle zagnieżdżone: zewnętrzna = wiersze, wewnętrzna = kolumny; println PO pętli wewnętrznej.
     *   • Cyfry: n % 10 = ostatnia cyfra, n /= 10 = odetnij ją; odwracanie: rev = rev * 10 + cyfra.
     *   • FizzBuzz i podobne: najwęższy warunek (% 15) jako PIERWSZY.
     *   • Walidacja: pętla „aż poprawne”; błędne dane trzeba „zużyć” (in.next()), inaczej pętla nieskończona.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego maksimum zaczynamy od pierwszego elementu, a nie od 0?
     *   2. Co wypisze:  int s = 0; for (int i = 1; i <= 4; i++) { s += i * i; } System.out.println(s);  ?
     *   3. ZNAJDŹ BŁĄD:  int sum = 7, count = 2;  double avg = sum / count;  System.out.println(avg);   // oczekujemy 3.5
     *   4. Co wypisze:  int n = 907; int r = 0; while (n > 0) { r = r * 10 + n % 10; n /= 10; } System.out.println(r);  ?
     *   5. ZNAJDŹ BŁĄD:  boolean allEven = false; for (int x : t) { if (x % 2 != 0) { allEven = false; break; } }
     *   6. Dlaczego w pętli walidacji trzeba wywołać in.next(), gdy hasNextInt() zwróci false?
     *   7. Kiedy += na String w pętli jest problemem i co zamiast?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: liczby parzyste", "3, 0, 0",
                () -> exercise1(new int[] {1, 2, 3, 4, 6}) + ", " + exercise1(new int[] {}) + ", " + exercise1(new int[] {7}));
        Check.equal("ćw. 2: palindromy liczbowe 12321, 1231, 7, 10", "true, false, true, false",
                () -> exercise2(12321) + ", " + exercise2(1231) + ", " + exercise2(7) + ", " + exercise2(10));
        Check.equal("ćw. 3: liczby z myślnikami", "1-2-3-4-5 | 1 | ",
                () -> exercise3(5) + " | " + exercise3(1) + " | " + exercise3(0));
        Check.equal("ćw. 4: najdłuższa seria znaków", "4, 1, 0, 4",
                () -> exercise4("aaabccdddd") + ", " + exercise4("abc") + ", " + exercise4("") + ", " + exercise4("zzzz"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "3, 0, 0",
                () -> solution1(new int[] {1, 2, 3, 4, 6}) + ", " + solution1(new int[] {}) + ", " + solution1(new int[] {7}));
        Check.equal("ćw. 2 (wzorzec)", "true, false, true, false",
                () -> solution2(12321) + ", " + solution2(1231) + ", " + solution2(7) + ", " + solution2(10));
        Check.equal("ćw. 3 (wzorzec)", "1-2-3-4-5 | 1 | ",
                () -> solution3(5) + " | " + solution3(1) + " | " + solution3(0));
        Check.equal("ćw. 4 (wzorzec)", "4, 1, 0, 4",
                () -> solution4("aaabccdddd") + ", " + solution4("abc") + ", " + solution4("") + ", " + solution4("zzzz"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): policz, ile liczb w tablicy jest parzystych.
     * Podpowiedź: licznik warunkowy — count = 0, for-each, if (x % 2 == 0) count++.
     */
    static int exercise1(int[] numbers) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (średnie): czy liczba (nieujemna) czyta się tak samo od przodu i od tyłu? 12321 → true, 10 → false.
     * Podpowiedź: odwróć liczbę wzorcem z sekcji 8 (na KOPII n!) i porównaj z oryginałem.
     */
    static boolean exercise2(int n) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ na StringBuilder (wynik bez zmian: dla 5 → "1-2-3-4-5", dla 0 → "").
     * <pre>{@code
     * String result = "";
     * for (int i = 1; i <= n; i++) {
     *     if (i > 1) {
     *         result += "-";
     *     }
     *     result += i;
     * }
     * return result;
     * }</pre>
     * Podpowiedź: StringBuilder sb = new StringBuilder(); sb.append(...); na końcu return sb.toString();
     */
    static String exercise3(int n) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): długość najdłuższej serii TAKICH SAMYCH znaków stojących obok siebie.
     * "aaabccdddd" → 4 (dddd), "abc" → 1, "" → 0.
     * Podpowiedź: dwa akumulatory — current (bieżąca seria) i best (najlepsza). Porównuj charAt(i) z charAt(i - 1):
     * ten sam znak → current++, inny → current = 1. Po każdym kroku best = Math.max(best, current).
     */
    static int exercise4(String text) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(int[] numbers) {
        int count = 0;
        for (int x : numbers) {
            if (x % 2 == 0) {
                count++;
            }
        }
        return count;
    }

    static boolean solution2(int n) {
        int rest = n;
        int reversed = 0;
        while (rest > 0) {
            reversed = reversed * 10 + rest % 10;
            rest /= 10;
        }
        return reversed == n;
    }

    static String solution3(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= n; i++) {
            if (i > 1) {
                sb.append("-");
            }
            sb.append(i);
        }
        return sb.toString();
    }

    static int solution4(String text) {
        if (text.isEmpty()) { // isEmpty = czy pusty
            return 0;
        }
        int best = 1;     // best = najlepsza (najdłuższa) seria
        int current = 1;  // current = bieżąca seria
        for (int i = 1; i < text.length(); i++) {
            if (text.charAt(i) == text.charAt(i - 1)) {
                current++;
            } else {
                current = 1;
            }
            best = Math.max(best, current);
        }
        return best;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo 0 może nie występować w danych: dla samych ujemnych liczb „maksimum” wyszłoby 0.
     *      Pierwszy element na pewno jest w danych.
     *   2. 30  (1 + 4 + 9 + 16).
     *   3. sum / count to dzielenie całkowite: 7 / 2 = 3, dopiero potem zamiana na 3.0.
     *      Poprawnie: (double) sum / count  → 3.5.
     *   4. 709 — cyfry w odwrotnej kolejności: 7, 0, 9.
     *   5. Wzorzec „all” musi startować od true (zakładamy „wszystkie parzyste” i szukamy kontrprzykładu).
     *      Ze startem false wynik zawsze będzie false.
     *   6. hasNextInt() tylko podgląda. Błędny fragment zostaje w Scannerze, dopóki go nie zużyjesz przez next()
     *      — bez tego pętla w kółko ogląda to samo „abc”.
     *   7. Gdy obrotów jest dużo (setki, tysiące): każde += kopiuje cały dotychczasowy tekst.
     *      Zamiast tego StringBuilder i append.
     */
    // </editor-fold>
}
