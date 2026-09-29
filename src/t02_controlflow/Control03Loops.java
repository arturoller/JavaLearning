package t02_controlflow;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Department;
import helpers.model.Employee;
import helpers.model.Product;

import java.util.Random;
import java.util.Scanner;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Pętle — for, while, do-while, for-each
 *        (loop = pętla; iteration = iteracja, jeden obrót pętli; for-each = dla każdego)
 *
 * W SKRÓCIE:
 *   Pętla powtarza blok kodu, dopóki warunek jest prawdziwy. for — gdy znasz liczbę powtórzeń,
 *   while — gdy powtarzasz „aż coś się stanie”, do-while — gdy blok ma się wykonać co najmniej raz,
 *   for-each — gdy chcesz przejść po wszystkich elementach tablicy lub listy.
 *
 * ANALOGIA: bieganie po stadionie.
 *   for      = „przebiegnij 5 okrążeń” (liczba znana z góry, trener liczy na palcach),
 *   while    = „biegaj, dopóki nie zadzwoni dzwonek” (może nie zadzwonić wcale — albo od razu),
 *   do-while = „przebiegnij okrążenie, a potem sprawdź, czy dzwonek już dzwonił” (min. 1 okrążenie),
 *   for-each = „przybij piątkę każdemu zawodnikowi z listy” (nie liczysz, po prostu idziesz po kolei).
 *
 * JAK TO DZIAŁA:
 *   for (int i = 0; i {@code <} 3; i++) { ciało }
 *     1. inicjalizacja (init):    int i = 0  — RAZ, na samym początku
 *     2. warunek (condition):     i {@code <} 3      — PRZED każdym obrotem; false = koniec pętli
 *     3. ciało (body):            to, co w klamrach
 *     4. aktualizacja (update):   i++        — PO każdym obrocie, potem znowu punkt 2
 *   Kolejność: init → warunek → ciało → update → warunek → ciało → update → ... → warunek false → koniec.
 *
 *   while (warunek) { ciało }          ← warunek sprawdzany PRZED ciałem (0 lub więcej obrotów)
 *   do { ciało } while (warunek);      ← warunek sprawdzany PO ciele (1 lub więcej obrotów), średnik!
 *   for (Typ element : tablicaLubLista) { ciało }   ← for-each, bez indeksu
 *
 * SŁÓWKA:
 *   init = inicjalizacja; update = aktualizacja; increment = zwiększenie (i++); decrement = zmniejszenie (i--);
 *   infinite loop = pętla nieskończona; off-by-one = błąd o jeden; step = krok; scope = zasięg (widoczność)
 *
 * ZOBACZ TEŻ: t02_controlflow/Control04BreakContinueLabels (przerywanie pętli),
 *   t02_controlflow/Control05LoopPatterns (typowe wzorce: suma, min/max, szukanie),
 *   t03_arrays/Arrays01Basics (tablice), t12_collections/Collections03IterationModification (pętle po kolekcjach)
 * </pre>
 */
public class Control03Loops {

    public static void main(String[] args) {
        title("Control03 — pętle: for, while, do-while, for-each");

        forAnatomy();           // for anatomy = budowa pętli for
        countingDownAndSteps(); // counting down and steps = liczenie w dół i kroki
        whileLoop();            // while loop = pętla while
        doWhileLoop();          // do while loop = pętla do-while
        forEachLoop();          // for each loop = pętla for-each
        offByOne();             // off by one = błąd o jeden
        loopVariableScope();    // loop variable scope = zasięg zmiennej pętli
        infiniteLoopWithBreak(); // infinite loop with break = pętla nieskończona z break
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PĘTLA FOR — BUDOWA
    // =================================================================================================

    /**
     * 1. Pętla for ma trzy części w nawiasie: inicjalizacja; warunek; aktualizacja.
     */
    static void forAnatomy() {
        section("1. Pętla for — init; warunek; update");

        for (int i = 1; i <= 5; i++) {       // i = indeks / licznik (tradycyjna nazwa)
            System.out.print(i + " ");       // print = wypisz BEZ przejścia do nowej linii
        }
        System.out.println();                // println() bez argumentu = tylko nowa linia
        // WYNIK: 1 2 3 4 5

        // Ślad wykonania (trace) dla for (int i = 1; i <= 3; i++):
        //   i = 1 → 1 <= 3 true  → ciało → i++ → i = 2
        //   i = 2 → 2 <= 3 true  → ciało → i++ → i = 3
        //   i = 3 → 3 <= 3 true  → ciało → i++ → i = 4
        //   i = 4 → 4 <= 3 false → KONIEC (ciało wykonało się 3 razy, warunek sprawdzono 4 razy)

        // Kwadraty liczb — printf z t01_basics/Basics11ConsoleOutput (%d = liczba całkowita, %n = nowa linia):
        for (int i = 1; i <= 3; i++) {
            System.out.printf("%d do kwadratu = %d%n", i, i * i); // printf = wypisz według wzorca
        }
        // WYNIK: 1 do kwadratu = 1
        // WYNIK: 2 do kwadratu = 4
        // WYNIK: 3 do kwadratu = 9

        // Pętla po znakach tekstu. length() = długość, charAt(i) = znak na pozycji i (pozycje od 0!).
        String word = "Java"; // word = słowo
        for (int i = 0; i < word.length(); i++) {
            System.out.print(word.charAt(i) + "-");
        }
        System.out.println();
        // WYNIK: J-a-v-a-

        // DOBRA PRAKTYKA: licząc od 0, pisz „i < długość”. Licząc od 1, pisz „i <= ilość”.
        // Standard w Javie to liczenie od 0 — tak numerowane są znaki w tekście i elementy tablic.
    }

    // =================================================================================================
    // 2. LICZENIE W DÓŁ I KROKI
    // =================================================================================================

    /**
     * 2. Aktualizacja w for nie musi być i++. Może być i--, i += 2, i *= 2 — dowolne wyrażenie.
     */
    static void countingDownAndSteps() {
        section("2. Liczenie w dół i krok co 2");

        for (int i = 5; i >= 1; i--) {
            System.out.print(i + " ");
        }
        System.out.println("start!");
        // WYNIK: 5 4 3 2 1 start!

        for (int i = 0; i <= 10; i += 2) {   // krok co 2 — liczby parzyste
            System.out.print(i + " ");
        }
        System.out.println();
        // WYNIK: 0 2 4 6 8 10

        for (int i = 9; i > 0; i -= 2) {     // nieparzyste w dół
            System.out.print(i + " ");
        }
        System.out.println();
        // WYNIK: 9 7 5 3 1

        for (int i = 1; i <= 100; i *= 3) {  // potęgi trójki — krok nie musi być stały
            System.out.print(i + " ");
        }
        System.out.println();
        // WYNIK: 1 3 9 27 81

        // PUŁAPKA: kierunek warunku musi pasować do kierunku kroku.
        //   for (int i = 5; i >= 1; i++)  ← i rośnie, a warunek czeka, aż i spadnie — pętla (prawie) nieskończona.
        //   Skończy się dopiero po przepełnieniu int (t01_basics/Basics05Casting) — ponad 2 miliardy obrotów.
    }

    // =================================================================================================
    // 3. PĘTLA WHILE
    // =================================================================================================

    /**
     * 3. while powtarza, DOPÓKI warunek jest prawdziwy. Używaj, gdy nie znasz z góry liczby obrotów.
     */
    static void whileLoop() {
        section("3. Pętla while — „dopóki”");

        // Bakterie podwajają się co godzinę. Po ilu godzinach będzie ich co najmniej 1000?
        int bacteria = 1;  // bacteria = bakterie
        int hours = 0;     // hours = godziny
        while (bacteria < 1000) {
            bacteria *= 2;
            hours++;
        }
        show("godzin do 1000 bakterii", hours);
        // WYNIK: godzin do 1000 bakterii → 10
        show("bakterii po tym czasie", bacteria);
        // WYNIK: bakterii po tym czasie → 1024

        // Ile razy trzeba podzielić liczbę przez 10, żeby została 0? (= liczba cyfr, dla liczby > 0)
        int number = 40721; // number = liczba
        int digits = 0;     // digits = cyfry
        while (number > 0) {
            number /= 10;   // dzielenie całkowite obcina ostatnią cyfrę: 40721 → 4072 → 407 → 40 → 4 → 0
            digits++;
        }
        show("cyfr w 40721", digits);
        // WYNIK: cyfr w 40721 → 5

        // while może nie wykonać się ANI RAZU, jeśli warunek od początku jest false:
        int tries = 0; // tries = próby
        while (tries > 0) {
            System.out.println("To się nigdy nie wypisze");
        }
        show("obroty pętli z warunkiem false", tries);
        // WYNIK: obroty pętli z warunkiem false → 0

        // PUŁAPKA: jeśli nic w ciele while nie zmienia warunku, pętla będzie działać w nieskończoność.
        // Zawsze sprawdź: CO w ciele pętli przybliża warunek do false?
    }

    // =================================================================================================
    // 4. PĘTLA DO-WHILE
    // =================================================================================================

    /**
     * 4. do-while najpierw wykonuje ciało, a dopiero potem sprawdza warunek — ciało wykona się co najmniej raz.
     * Klasyczne zastosowanie: menu, które trzeba pokazać przed pierwszym wyborem.
     */
    static void doWhileLoop() {
        section("4. Pętla do-while — co najmniej raz");

        int counter = 10; // counter = licznik
        while (counter < 5) {
            System.out.println("while: " + counter);
            counter++;
        }
        do {
            System.out.println("do-while: " + counter);
            counter++;
        } while (counter < 5);   // ← średnik po while jest tu OBOWIĄZKOWY
        // WYNIK: do-while: 10
        // while nie wypisał nic (10 < 5 to false), do-while wypisał raz — warunek sprawdził dopiero po ciele.

        // Menu z wczytywaniem. Zamiast klawiatury dajemy Scannerowi gotowy tekst (scripted input = wejście
        // ze skryptu) — dzięki temu program działa sam i zawsze tak samo. Scanner: t01_basics/Basics10ScannerInput.
        Scanner scanner = new Scanner("1\n2\n0\n"); // scanner = czytnik danych; \n = koniec linii
        int choice; // choice = wybór — zadeklarowany PRZED pętlą, bo używa go warunek w while (...)
        do {
            System.out.println("MENU: 1 = saldo, 2 = wpłata, 0 = wyjście");
            choice = scanner.nextInt(); // next int = następna liczba całkowita
            System.out.println("wybrano: " + choice);
            switch (choice) {
                case 1 -> System.out.println("Saldo: 100 zł");
                case 2 -> System.out.println("Wpłacono 50 zł");
                case 0 -> System.out.println("Do widzenia!");
                default -> System.out.println("Nieznana opcja");
            }
        } while (choice != 0);
        scanner.close(); // close = zamknij (zwalniamy zasób; dokładniej w t10_exceptions/Exceptions04TryWithResources)
        // WYNIK: MENU: 1 = saldo, 2 = wpłata, 0 = wyjście
        // WYNIK: wybrano: 1
        // WYNIK: Saldo: 100 zł
        // WYNIK: MENU: 1 = saldo, 2 = wpłata, 0 = wyjście
        // WYNIK: wybrano: 2
        // WYNIK: Wpłacono 50 zł
        // WYNIK: MENU: 1 = saldo, 2 = wpłata, 0 = wyjście
        // WYNIK: wybrano: 0
        // WYNIK: Do widzenia!

        // DOBRA PRAKTYKA: do-while wybieraj tylko wtedy, gdy „najpierw zrób, potem sprawdź” wynika z logiki
        // (menu, pytanie o dane aż będą poprawne). W pozostałych przypadkach while jest czytelniejszy.
    }

    // =================================================================================================
    // 5. PĘTLA FOR-EACH
    // =================================================================================================

    /**
     * 5. for-each przechodzi po wszystkich elementach tablicy lub listy — bez indeksu i bez ryzyka
     * pomyłki w warunku. Czytaj dwukropek jako „z”: „dla każdego price z prices”.
     */
    static void forEachLoop() {
        section("5. Pętla for-each — po każdym elemencie");

        int[] prices = {120, 45, 300, 15}; // prices = ceny; int[] = tablica liczb (szczegóły: t03_arrays)
        int total = 0; // total = suma
        for (int price : prices) {          // price = cena — kolejny element tablicy
            total += price;
        }
        show("suma cen", total);
        // WYNIK: suma cen → 480

        // for-each po liście z SampleData. p.name() i p.stock() to odczyt pól rekordu (t09_records/Records01Basics).
        int inStockCount = 0; // in stock count = liczba dostępnych produktów
        for (Product p : SampleData.products()) {
            if (p.stock() > 0) {
                inStockCount++;
            } else {
                System.out.println("brak w magazynie: " + p.name());
            }
        }
        // WYNIK: brak w magazynie: Smartfon X
        // WYNIK: brak w magazynie: Oliwa z oliwek
        show("produktów dostępnych", inStockCount);
        // WYNIK: produktów dostępnych → 12

        for (Employee e : SampleData.employees()) { // e = employee = pracownik
            if (e.department() == Department.IT) {  // department = dział; enum porównujemy przez ==
                System.out.print(e.name() + "; ");
            }
        }
        System.out.println();
        // WYNIK: Anna Nowak; Piotr Kowalski; Michał Lewandowski; Ewa Woźniak;

        // Ograniczenia for-each:
        //   • nie znasz indeksu (pozycji) elementu — gdy go potrzebujesz, użyj zwykłego for,
        //   • zmienna pętli to KOPIA wartości — przypisanie price = 0 nie zmieni tablicy,
        //   • zawsze idzie od początku do końca, co jeden element.
        for (int price : prices) {
            price = 0; // zmieniamy tylko lokalną kopię!
            System.out.print(price + " ");
        }
        System.out.println();
        // WYNIK: 0 0 0 0
        show("pierwsza cena w tablicy", prices[0]);
        // WYNIK: pierwsza cena w tablicy → 120
    }

    // =================================================================================================
    // 6. BŁĄD O JEDEN (OFF-BY-ONE)
    // =================================================================================================

    /**
     * 6. Off-by-one = pętla wykonuje się o jeden raz za dużo albo za mało. Najczęstszy błąd w pętlach.
     */
    static void offByOne() {
        section("6. Błąd o jeden: < czy <=?");

        int count = 0;
        for (int i = 1; i < 10; i++) {
            count++;
        }
        show("obroty dla i = 1; i < 10", count);
        // WYNIK: obroty dla i = 1; i < 10 → 9

        count = 0;
        for (int i = 1; i <= 10; i++) {
            count++;
        }
        show("obroty dla i = 1; i <= 10", count);
        // WYNIK: obroty dla i = 1; i <= 10 → 10

        count = 0;
        for (int i = 0; i < 10; i++) {
            count++;
        }
        show("obroty dla i = 0; i < 10", count);
        // WYNIK: obroty dla i = 0; i < 10 → 10

        // Wzór: liczba obrotów = koniec - początek (dla <) albo koniec - początek + 1 (dla <=).

        // PUŁAPKA: tablica o długości 3 ma indeksy 0, 1, 2. Warunek <= wyjdzie poza tablicę.
        int[] scores = {5, 3, 4}; // scores = wyniki; scores.length = 3 (długość tablicy)
        expectThrows("pętla z i <= scores.length", () -> {
            int sum = 0;
            for (int i = 0; i <= scores.length; i++) {
                sum += scores[i];
            }
            System.out.println(sum);
        });
        // WYNIK: ✔ pętla z i <= scores.length → rzucono ArrayIndexOutOfBoundsException: Index 3 out of bounds for length 3

        // DOBRA PRAKTYKA: dla tablic i tekstów zawsze „for (int i = 0; i < x.length; i++)” — albo for-each,
        // który w ogóle nie ma indeksu, więc nie da się w nim pomylić o jeden.
    }

    // =================================================================================================
    // 7. ZASIĘG ZMIENNEJ PĘTLI
    // =================================================================================================

    /**
     * 7. Zmienna zadeklarowana w nawiasie for istnieje TYLKO wewnątrz pętli.
     * Jeśli wynik jest potrzebny po pętli, zadeklaruj zmienną przed pętlą.
     */
    static void loopVariableScope() {
        section("7. Zasięg zmiennej pętli");

        for (int i = 0; i < 3; i++) {
            System.out.print("i=" + i + " ");
        }
        System.out.println();
        // WYNIK: i=0 i=1 i=2
        // System.out.println(i);   ← błąd kompilacji: nie można znaleźć symbolu i (już nie istnieje)

        // Kolejna pętla może znowu nazwać licznik „i” — to NOWA zmienna.
        String text = "banan"; // text = tekst
        int lastA = -1;        // last a = ostatnia pozycja litery 'a'; -1 = „nie znaleziono”
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == 'a') {   // char porównujemy przez == (to typ prosty)
                lastA = i;
            }
        }
        show("ostatnie 'a' w \"banan\" na pozycji", lastA);
        // WYNIK: ostatnie 'a' w "banan" na pozycji → 3

        // Zmienne zadeklarowane W CIELE pętli powstają od nowa w każdym obrocie:
        for (int i = 0; i < 3; i++) {
            int doubled = i * 2; // doubled = podwojone; żyje tylko w tym jednym obrocie
            System.out.print(doubled + " ");
        }
        System.out.println();
        // WYNIK: 0 2 4

        // DOBRA PRAKTYKA: deklaruj zmienną jak najbliżej miejsca użycia i w jak najwęższym zasięgu.
        // Mniej miejsc, w których można ją przypadkiem zmienić.
    }

    // =================================================================================================
    // 8. PĘTLA NIESKOŃCZONA Z BREAK
    // =================================================================================================

    /**
     * 8. while (true) to pętla, z której wychodzi się tylko przez break (albo return).
     * Przydaje się, gdy warunek wyjścia wygodniej sprawdzić w środku ciała.
     */
    static void infiniteLoopWithBreak() {
        section("8. while (true) + break");

        // Rzucamy kostką, aż wypadnie szóstka. Random z ziarnem (seed) daje zawsze te same liczby
        // — dzięki temu wynik jest powtarzalny (t01_basics/Basics07MathRandom).
        Random random = new Random(7); // random = generator liczb losowych
        int rolls = 0; // rolls = rzuty
        while (true) {
            int dice = random.nextInt(6) + 1; // dice = kostka; nextInt(6) daje 0..5, więc +1
            rolls++;
            System.out.print(dice + " ");
            if (dice == 6) {
                break; // break = przerwij pętlę natychmiast
            }
        }
        System.out.println();
        // WYNIK: 5 3 4 5 5 5 5 6
        show("rzutów do pierwszej szóstki", rolls);
        // WYNIK: rzutów do pierwszej szóstki → 8

        // for (;;) { ... } to to samo co while (true) — spotkasz w starszym kodzie.

        // DOBRA PRAKTYKA: przy while (true) dodaj „bezpiecznik”, np. limit prób,
        // żeby błąd w danych nie zawiesił programu na zawsze:
        int attempts = 0; // attempts = próby
        while (true) {
            attempts++;
            if (attempts >= 1000) {
                System.out.println("Bezpiecznik: przerwano po " + attempts + " próbach");
                break;
            }
        }
        // WYNIK: Bezpiecznik: przerwano po 1000 próbach

        // Szczegóły break, continue i przerywania pętli zagnieżdżonych: t02_controlflow/Control04BreakContinueLabels.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   Która pętla kiedy?
     *   ┌─────────────┬──────────────────────────────────────────────┬──────────────────────────────┐
     *   │ pętla       │ kiedy                                        │ przykład                     │
     *   ├─────────────┼──────────────────────────────────────────────┼──────────────────────────────┤
     *   │ for         │ znana liczba obrotów, potrzebny indeks       │ i od 0 do length - 1         │
     *   │ for-each    │ każdy element tablicy/listy, indeks zbędny   │ suma cen, wypisanie nazw     │
     *   │ while       │ „dopóki warunek”, liczba obrotów nieznana    │ dziel przez 10, aż zostanie 0│
     *   │ do-while    │ ciało co najmniej raz, sprawdzenie po nim    │ menu, pytanie o poprawne dane│
     *   │ while(true) │ wyjście najwygodniej sprawdzić w środku      │ rzucaj, aż wypadnie 6        │
     *   └─────────────┴──────────────────────────────────────────────┴──────────────────────────────┘
     *   • for (init; warunek; update): init raz, warunek przed każdym obrotem, update po każdym obrocie.
     *   • Od 0: „i < n” (n obrotów). Od 1: „i <= n” (n obrotów). Indeksy tablicy: 0 .. length - 1.
     *   • Zmienna z nawiasu for istnieje tylko w pętli. Wynik potrzebny później → deklaracja przed pętlą.
     *   • do { ... } while (warunek);  ← średnik na końcu.
     *   • for-each daje KOPIĘ elementu — przypisanie do zmiennej pętli nie zmienia tablicy.
     *   • W ciele pętli coś MUSI przybliżać warunek do false — inaczej pętla nieskończona.
     *
     * PYTANIA KONTROLNE:
     *   1. Ile razy wykona się ciało pętli:  for (int i = 3; i < 8; i++)  ? A ile razy sprawdzony zostanie warunek?
     *   2. Co wypisze:  int x = 5;  do { System.out.print(x + " "); x++; } while (x < 3);  ?
     *   3. Co wypisze:  for (int i = 10; i > 0; i -= 3) { System.out.print(i + " "); }  ?
     *   4. ZNAJDŹ BŁĄD:  int[] t = {1, 2, 3};  for (int i = 1; i <= t.length; i++) { suma += t[i]; }
     *   5. ZNAJDŹ BŁĄD:  for (int i = 0; i < 5; i++) { ... }  System.out.println("ostatnie i: " + i);
     *   6. Dlaczego w pętli menu zmienna choice jest zadeklarowana PRZED do-while, a nie w jego ciele?
     *   7. Kiedy for-each nie wystarczy i trzeba wrócić do zwykłego for?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: suma 1..100, 1..5, 1..0", "5050, 15, 0",
                () -> exercise1(100) + ", " + exercise1(5) + ", " + exercise1(0));
        Check.equal("ćw. 2: liczba cyfr 12345, 7, 0, -450", "5, 1, 1, 3",
                () -> exercise2(12345) + ", " + exercise2(7) + ", " + exercise2(0) + ", " + exercise2(-450));
        Check.equal("ćw. 3: odliczanie co 3 w pętli for", "10 7 4 1 ", () -> exercise3());
        Check.equal("ćw. 4: kroki Collatza dla 6, 27, 1", "8, 111, 0",
                () -> exercise4(6) + ", " + exercise4(27) + ", " + exercise4(1));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "5050, 15, 0",
                () -> solution1(100) + ", " + solution1(5) + ", " + solution1(0));
        Check.equal("ćw. 2 (wzorzec)", "5, 1, 1, 3",
                () -> solution2(12345) + ", " + solution2(7) + ", " + solution2(0) + ", " + solution2(-450));
        Check.equal("ćw. 3 (wzorzec)", "10 7 4 1 ", () -> solution3());
        Check.equal("ćw. 4 (wzorzec)", "8, 111, 0",
                () -> solution4(6) + ", " + solution4(27) + ", " + solution4(1));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć sumę liczb od 1 do n (włącznie). Dla n mniejszego od 1 suma wynosi 0.
     * Podpowiedź: zmienna sum = 0 przed pętlą, for (int i = 1; i <= n; i++) i w ciele sum += i.
     */
    static int exercise1(int n) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (średnie): policz cyfry liczby całkowitej. 0 ma jedną cyfrę, minus się nie liczy (-450 ma 3).
     * Podpowiedź: Math.abs(n) usuwa minus; do-while (a nie while!) sprawi, że 0 też da wynik 1.
     */
    static int exercise2(int n) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ pętlę while na pętlę for (wynik ma być identyczny).
     * <pre>{@code
     * String result = "";
     * int i = 10;
     * while (i >= 0) {
     *     result += i + " ";
     *     i -= 3;
     * }
     * return result;
     * }</pre>
     * Podpowiedź: trzy części for to dokładnie: int i = 10; i >= 0; i -= 3.
     */
    static String exercise3() {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): problem Collatza. Dopóki n jest różne od 1: parzyste n dziel przez 2,
     * nieparzyste zamień na 3 * n + 1. Zwróć, ile kroków trzeba, żeby dojść do 1 (dla 1 — zero kroków).
     * Przykład dla 6: 6 → 3 → 10 → 5 → 16 → 8 → 4 → 2 → 1, czyli 8 kroków.
     * Podpowiedź: while (n != 1) z if / else w środku i licznikiem kroków. Zakładamy n większe od 0.
     */
    static int exercise4(int n) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(int n) {
        int sum = 0;
        for (int i = 1; i <= n; i++) {
            sum += i;
        }
        return sum;
    }

    static int solution2(int n) {
        int rest = Math.abs(n); // rest = reszta (to, co zostało do przetworzenia)
        int count = 0;
        do {
            rest /= 10;
            count++;
        } while (rest > 0);
        return count;
    }

    static String solution3() {
        String result = "";
        for (int i = 10; i >= 0; i -= 3) {
            result += i + " ";
        }
        return result;
    }

    static int solution4(int n) {
        int steps = 0; // steps = kroki
        while (n != 1) {
            if (n % 2 == 0) {
                n /= 2;
            } else {
                n = 3 * n + 1;
            }
            steps++;
        }
        return steps;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Ciało 5 razy (i = 3, 4, 5, 6, 7), warunek 6 razy (ostatni raz dla i = 8 daje false).
     *   2. „5 ” — do-while wykonuje ciało raz, dopiero potem sprawdza 6 < 3 (false) i kończy.
     *   3. „10 7 4 1 ” — po 1 następuje -2, a -2 > 0 to false.
     *   4. Indeksy tablicy to 0..2. Start od 1 pomija pierwszy element, a i <= t.length sięga do t[3]
     *      → ArrayIndexOutOfBoundsException. Poprawnie: for (int i = 0; i < t.length; i++).
     *   5. i istnieje tylko w pętli — błąd kompilacji. Jeśli potrzebujesz i po pętli, zadeklaruj je przed nią.
     *   6. Bo warunek while (choice != 0) stoi POZA klamrami ciała — zmienna z ciała nie byłaby tam widoczna.
     *   7. Gdy potrzebujesz indeksu (pozycji), chcesz zmieniać elementy tablicy, iść od końca albo co drugi element.
     */
    // </editor-fold>
}
