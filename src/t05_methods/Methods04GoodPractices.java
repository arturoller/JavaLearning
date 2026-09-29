package t05_methods;

import helpers.Check;

import java.util.Locale;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Dobre praktyki pisania metod
 *        (good practices = dobre praktyki; single responsibility = jedna odpowiedzialność; refactor = poprawić strukturę)
 *
 * W SKRÓCIE:
 *   Dobra metoda robi JEDNĄ rzecz, ma nazwę mówiącą co robi, mało parametrów, sprawdza dane wejściowe na początku
 *   (fail fast) i najlepiej ZWRACA wynik zamiast wypisywać. Taki kod łatwo czytać, testować i zmieniać.
 *
 * ANALOGIA: szuflady w kuchni.
 *   Jedna wielka szuflada „na wszystko” (sztućce, baterie, przyprawy) działa, ale szukanie w niej to koszmar.
 *   Kilka małych szuflad z etykietami (metody z dobrymi nazwami) — i od razu wiesz, gdzie co jest.
 *
 * JAK TO DZIAŁA:
 *   • Jedna odpowiedzialność: metoda liczy ALBO formatuje ALBO wypisuje — nie wszystko naraz.
 *   • Nazwa: czasownik (calculateTotal, sendEmail); pytanie → boolean (isEmpty, hasDiscount).
 *   • Parametry: im mniej, tym lepiej; zamiast flagi true/false — dwie metody o jasnych nazwach.
 *   • Walidacja na początku: zły argument → IllegalArgumentException z czytelnym komunikatem.
 *   • Stałe zamiast „magicznych liczb”; wspólny kod w jednej metodzie (DRY).
 *
 * SŁÓWKA:
 *   refactor = refaktoryzować (poprawić strukturę bez zmiany działania); extract method = wydziel metodę;
 *   guard clause = klauzula strażnika (wczesne sprawdzenie); fail fast = zawiedź szybko; pure function = czysta funkcja;
 *   side effect = efekt uboczny; flag = flaga (parametr true/false); threshold = próg; DRY (Don't Repeat Yourself) = nie powtarzaj się.
 *
 * ZOBACZ TEŻ: t05_methods/Methods01Basics (budowa metody), t10_exceptions/Exceptions07BestPractices (wyjątki w praktyce),
 *             t27_clean_code_pitfalls/CleanCode01Principles (czysty kod szerzej).
 * </pre>
 */
public class Methods04GoodPractices {

    /** FREE_SHIPPING_THRESHOLD = próg darmowej dostawy (w zł). */
    private static final double FREE_SHIPPING_THRESHOLD = 200.0;
    /** SHIPPING_COST = koszt dostawy (w zł), gdy próg nie jest osiągnięty. */
    private static final double SHIPPING_COST = 15.0;
    /** VIP_DISCOUNT_PERCENT = rabat dla klienta VIP (w procentach). */
    private static final int VIP_DISCOUNT_PERCENT = 10;

    public static void main(String[] args) {
        title("Methods04 — dobre praktyki pisania metod");

        singleResponsibility();     // single responsibility = jedna odpowiedzialność
        goodNames();                // good names = dobre nazwy
        noBooleanFlags();           // no boolean flags = bez parametrów-flag
        failFast();                 // fail fast = zawiedź szybko
        pureFunctions();            // pure functions = czyste funkcje
        dontRepeatYourself();       // don't repeat yourself = nie powtarzaj się
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. JEDNA ODPOWIEDZIALNOŚĆ
    // =================================================================================================

    /** orderSummaryBad = podsumowanie zamówienia (ŹLE): liczy, stosuje rabat, dolicza dostawę i formatuje — wszystko naraz. */
    static String orderSummaryBad(int quantity, double unitPrice, boolean vip) {
        double total = quantity * unitPrice;
        if (vip) {
            total = total - total * 10 / 100;          // magiczne 10
        }
        if (total < 200) {                             // magiczne 200
            total = total + 15;                        // magiczne 15
        }
        return String.format(Locale.ROOT, "Do zapłaty: %.2f zł", total);
    }

    // ---- PO: każda metoda robi jedną rzecz i ma nazwę, która to mówi ----

    /** subtotal = wartość towarów przed rabatem i dostawą. */
    static double subtotal(int quantity, double unitPrice) {
        return quantity * unitPrice;
    }

    /** applyVipDiscount = zastosuj rabat VIP (albo zwróć kwotę bez zmian). */
    static double applyVipDiscount(double amount, boolean vip) {
        return vip ? amount * (100 - VIP_DISCOUNT_PERCENT) / 100 : amount;
    }

    /** withShipping = kwota z dostawą (darmowa od progu). */
    static double withShipping(double amount) {
        return amount >= FREE_SHIPPING_THRESHOLD ? amount : amount + SHIPPING_COST;
    }

    /** formatToPay = sformatuj kwotę do zapłaty. */
    static String formatToPay(double amount) {
        return String.format(Locale.ROOT, "Do zapłaty: %.2f zł", amount);
    }

    /** orderSummary = podsumowanie zamówienia (DOBRZE): czyta się jak przepis — krok po kroku. */
    static String orderSummary(int quantity, double unitPrice, boolean vip) {
        double amount = subtotal(quantity, unitPrice);
        amount = applyVipDiscount(amount, vip);
        amount = withShipping(amount);
        return formatToPay(amount);
    }

    /**
     * 1. Metoda robiąca wszystko naraz jest trudna do zrozumienia, zmiany i sprawdzenia. Podzielona na małe metody —
     * czyta się jak przepis, a każdy krok da się sprawdzić osobno. Wynik działania jest TAKI SAM (to refaktoryzacja).
     */
    static void singleResponsibility() {
        section("1. Jedna metoda = jedna odpowiedzialność");

        show("PRZED", orderSummaryBad(3, 64.99, false));
        show("PO", orderSummary(3, 64.99, false));
        // WYNIK: PRZED → Do zapłaty: 209.97 zł    ← 3 × 64.99 = 194.97 (poniżej progu) + 15 zł dostawy
        // WYNIK: PO → Do zapłaty: 209.97 zł    ← ten sam wynik: refaktoryzacja NIE zmienia działania

        show("PO: subtotal(3, 64.99)", subtotal(3, 64.99));
        show("PO: withShipping(194.97)", withShipping(194.97));
        // WYNIK: PO: subtotal(3, 64.99) → 194.96999999999997    ← przybliżenie double! (t01_basics/Basics08FloatingPoint)
        // WYNIK: PO: withShipping(194.97) → 209.97
        // Dopiero formatowanie %.2f ukrywa ten „ogon” — dlatego prawdziwe kwoty liczy się na BigDecimal
        //   (t15_numbers/Numbers01BigDecimal). Tu double wystarcza, bo lekcja jest o dzieleniu metod, nie o pieniądzach.
        // Każdy krok da się teraz sprawdzić OSOBNO — w wersji PRZED widać tylko wynik końcowy.

        // DOBRA PRAKTYKA: refaktoryzuj małymi krokami i po każdym sprawdzaj, że wynik się nie zmienił
        //   (do tego służą testy — t25_testing).
    }

    // =================================================================================================
    // 2. DOBRE NAZWY
    // =================================================================================================

    /** isAdult = czy pełnoletni. Metoda zwracająca boolean — nazwa jak pytanie „czy...?”. */
    static boolean isAdult(int age) {
        return age >= 18;
    }

    /** 2. Nazwa ma mówić CO metoda robi, żeby nie trzeba było czytać jej środka. */
    static void goodNames() {
        section("2. Nazwy metod");

        show("isAdult(17)", isAdult(17));
        // WYNIK: isAdult(17) → false

        // ŹLE:  calc(), doStuff(), process2(), check(x)        — nic nie mówią
        // DOBRZE: calculateTotal(), sendInvoice(), isExpired(), hasDiscount(), findCustomerByEmail()
        // Zasady: czasownik na początku (co robi); boolean jako pytanie (is/has/can); pełne słowa zamiast skrótów
        //   (calculateTotal, a nie clcTtl); jeśli trudno wymyślić nazwę — metoda pewnie robi za dużo naraz.
    }

    // =================================================================================================
    // 3. BEZ PARAMETRÓW-FLAG
    // =================================================================================================

    /** formatBad = formatuj (ŹLE): parametr true/false zmienia działanie metody. */
    static String formatBad(double amount, boolean withCurrency) {
        String text = String.format(Locale.ROOT, "%.2f", amount);
        return withCurrency ? text + " zł" : text;
    }

    /** 3. Wywołanie formatBad(49.99, true) nic nie mówi — co znaczy true? Dwie metody o jasnych nazwach są czytelniejsze. */
    static void noBooleanFlags() {
        section("3. Parametr true/false → dwie metody");

        show("formatBad(49.99, true) — co znaczy true?", formatBad(49.99, true));
        // WYNIK: formatBad(49.99, true) — co znaczy true? → 49.99 zł

        // DOBRZE (ćwiczenie 3): formatPrice(49.99) → "49.99"  i  formatPriceWithCurrency(49.99) → "49.99 zł".
        // DOBRA PRAKTYKA: flaga w parametrze to sygnał, że metoda robi DWIE rzeczy. Rozdziel ją.
    }

    // =================================================================================================
    // 4. FAIL FAST — WALIDACJA NA POCZĄTKU
    // =================================================================================================

    /** unitPrice = cena jednostkowa. Najpierw sprawdza dane (klauzule strażnika), potem liczy. */
    static double unitPrice(double total, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Ilość musi być dodatnia, podano: " + quantity);
        }
        if (total < 0) {
            throw new IllegalArgumentException("Kwota nie może być ujemna, podano: " + total);
        }
        return total / quantity;
    }

    /**
     * 4. Zły argument lepiej zatrzymać NATYCHMIAST, z jasnym komunikatem — niż pozwolić, żeby metoda policzyła bzdurę
     * (tu: dzielenie przez zero dałoby Infinity), która wyjdzie na jaw dużo później, w zupełnie innym miejscu.
     * throw = rzuć wyjątek (wyjątki dokładnie: t10_exceptions).
     */
    static void failFast() {
        section("4. Fail fast — sprawdź argumenty na początku");

        show("unitPrice(100.0, 4)", unitPrice(100.0, 4));
        // WYNIK: unitPrice(100.0, 4) → 25.0
        expectThrows("unitPrice(100.0, 0)", () -> unitPrice(100.0, 0));
        // WYNIK: ✔ unitPrice(100.0, 0) → rzucono IllegalArgumentException: Ilość musi być dodatnia, podano: 0

        // DOBRA PRAKTYKA: komunikat wyjątku mówi, CO jest nie tak i JAKA wartość przyszła — oszczędza godziny szukania.
    }

    // =================================================================================================
    // 5. CZYSTE FUNKCJE I EFEKTY UBOCZNE
    // =================================================================================================

    /** callCounter = licznik wywołań — stan „na zewnątrz” metody. */
    private static int callCounter = 0;

    /** addPure = dodaj (czysta funkcja): wynik zależy TYLKO od argumentów, nic poza tym się nie zmienia. */
    static int addPure(int a, int b) {
        return a + b;
    }

    /** addAndCount = dodaj i policz (z efektem ubocznym): przy okazji zmienia licznik poza metodą. */
    static int addAndCount(int a, int b) {
        callCounter++;
        return a + b;
    }

    /**
     * 5. Czysta funkcja (pure function): dla tych samych argumentów zawsze ten sam wynik i żadnych zmian dookoła.
     * Łatwo ją zrozumieć i przetestować. Efekty uboczne (zmiana pól, wypisywanie, zapis do pliku) są czasem
     * potrzebne — ale niech będą wyraźne i skupione w niewielu miejscach.
     */
    static void pureFunctions() {
        section("5. Czyste funkcje kontra efekty uboczne");

        show("addPure(2, 3) dwa razy", addPure(2, 3) + ", " + addPure(2, 3));
        addAndCount(2, 3);
        addAndCount(2, 3);
        show("callCounter po dwóch addAndCount", callCounter);
        // WYNIK: addPure(2, 3) dwa razy → 5, 5
        // WYNIK: callCounter po dwóch addAndCount → 2    ← metoda „przy okazji” zmieniła stan programu
    }

    // =================================================================================================
    // 6. NIE POWTARZAJ SIĘ (DRY)
    // =================================================================================================

    /** receiptLine = linia paragonu. Wspólny kod formatowania w JEDNYM miejscu. */
    static String receiptLine(String name, double price) {
        return String.format(Locale.ROOT, "%-15s %8.2f zł", name, price);
    }

    /** 6. Gdy ten sam kawałek kodu pojawia się w kilku miejscach — wydziel go do metody. Zmienisz raz, zadziała wszędzie. */
    static void dontRepeatYourself() {
        section("6. DRY — wspólny kod w jednej metodzie");

        System.out.println("   " + receiptLine("Kawa", 64.99));
        System.out.println("   " + receiptLine("Czekolada", 7.49));
        // WYNIK: Kawa               64.99 zł
        // WYNIK: Czekolada           7.49 zł
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Jedna metoda = jedna odpowiedzialność; długa metoda → wydziel mniejsze (extract method).
     *   • Nazwy: czasownik (calculateTotal); boolean jako pytanie (isAdult, hasDiscount); bez skrótów.
     *   • Mało parametrów; zamiast flagi true/false → dwie metody o jasnych nazwach.
     *   • Fail fast: walidacja na początku, IllegalArgumentException z komunikatem „co i jaka wartość”.
     *   • Czyste funkcje (bez efektów ubocznych) są najłatwiejsze do testowania; wypisywanie na końcu programu.
     *   • Stałe zamiast magicznych liczb; wspólny kod w jednym miejscu (DRY).
     *
     * PYTANIA KONTROLNE:
     *   1. Po czym poznasz, że metoda robi za dużo?
     *   2. Co jest nie tak z wywołaniem  save(order, true, false);  i jak to poprawić?
     *   3. ZNAJDŹ BŁĄD:  static double average(double sum, int count) { return sum / count; }
     *      — co zwróci average(10.0, 0) i dlaczego to gorsze niż wyjątek?
     *   4. Dlaczego walidacja argumentów powinna być na POCZĄTKU metody?
     *   5. Czym jest czysta funkcja i dlaczego łatwo ją testować?
     *   6. Jak nazwać metodę zwracającą informację, czy zamówienie jest już opłacone?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: dostawa dla 250 zł", 0.0, () -> exercise1(250.0));
        Check.equal("ćw. 1b: dostawa dla 100 zł", 15.0, () -> exercise1(100.0));
        Check.equal("ćw. 2a: 25% z 200", 50.0, () -> exercise2(200.0, 25));
        Check.throwsException("ćw. 2b: 150% → wyjątek", IllegalArgumentException.class, () -> exercise2(200.0, 150));
        Check.equal("ćw. 3a: formatPrice", "49.99", () -> exercise3FormatPrice(49.99));
        Check.equal("ćw. 3b: formatPriceWithCurrency", "49.99 zł", () -> exercise3FormatPriceWithCurrency(49.99));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", 0.0, () -> solution1(250.0));
        Check.equal("ćw. 1b (wzorzec)", 15.0, () -> solution1(100.0));
        Check.equal("ćw. 2a (wzorzec)", 50.0, () -> solution2(200.0, 25));
        Check.throwsException("ćw. 2b (wzorzec)", IllegalArgumentException.class, () -> solution2(200.0, 150));
        Check.equal("ćw. 3a (wzorzec)", "49.99", () -> solution3FormatPrice(49.99));
        Check.equal("ćw. 3b (wzorzec)", "49.99 zł", () -> solution3FormatPriceWithCurrency(49.99));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć koszt dostawy: 0.0, gdy wartość zamówienia jest co najmniej FREE_SHIPPING_THRESHOLD,
     * w przeciwnym razie SHIPPING_COST. Użyj STAŁYCH, nie liczb 200 i 15.
     * Podpowiedź: operator trójargumentowy albo if.
     */
    static double exercise1(double orderValue) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć percent procent z value (200.0, 25 → 50.0). Gdy percent jest spoza 0..100 —
     * rzuć IllegalArgumentException z komunikatem, np. „Procent musi być z zakresu 0–100, podano: 150”.
     * Podpowiedź: klauzula strażnika na początku, potem {@code value * percent / 100}.
     */
    static double exercise2(double value, int percent) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): zamiast formatBad(amount, flag) napisz DWIE metody:
     * formatPrice(49.99) → "49.99" oraz formatPriceWithCurrency(49.99) → "49.99 zł".
     * Druga ma KORZYSTAĆ z pierwszej (DRY), a nie powtarzać String.format.
     */
    static String exercise3FormatPrice(double amount) {
        // TODO: twoje rozwiązanie
        return "";
    }

    static String exercise3FormatPriceWithCurrency(double amount) {
        // TODO: twoje rozwiązanie (wywołaj exercise3FormatPrice)
        return "";
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static double solution1(double orderValue) {
        return orderValue >= FREE_SHIPPING_THRESHOLD ? 0.0 : SHIPPING_COST;
    }

    static double solution2(double value, int percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("Procent musi być z zakresu 0–100, podano: " + percent);
        }
        return value * percent / 100;
    }

    static String solution3FormatPrice(double amount) {
        return String.format(Locale.ROOT, "%.2f", amount);
    }

    static String solution3FormatPriceWithCurrency(double amount) {
        return solution3FormatPrice(amount) + " zł";
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Trudno ją nazwać bez „i” (liczIWypiszIZapisz), jest długa, ma wiele poziomów wcięć, dużo parametrów albo flagi.
     *   2. Nie wiadomo, co znaczą true i false. Lepiej osobne metody (np. saveAndNotify, saveDraft) albo nazwane stałe.
     *   3. Infinity — double dzielone przez 0 nie rzuca wyjątku (t01_basics/Basics08FloatingPoint). Błędna „średnia”
     *      popłynie dalej przez program i wyjdzie na jaw daleko od przyczyny. Lepiej na początku:
     *      if (count <= 0) throw new IllegalArgumentException("Liczba elementów musi być dodatnia: " + count);
     *   4. Żeby zły argument zatrzymać od razu, z jasnym komunikatem, zanim metoda zrobi coś błędnego albo częściowego.
     *   5. Taka, której wynik zależy tylko od argumentów i która niczego nie zmienia dookoła — test to po prostu
     *      „podaj argumenty, porównaj wynik”.
     *   6. isPaid() (albo hasBeenPaid()) — nazwa jak pytanie, zwraca boolean.
     */
    // </editor-fold>
}
