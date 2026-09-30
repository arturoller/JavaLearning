package t27_clean_code_pitfalls;

import helpers.Check;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Typowe uwagi z code review — PRZED/PO
 *        (code review = przegląd kodu; finding = uwaga, spostrzeżenie)
 *
 * W SKRÓCIE:
 *   To nie są błędy, które wywalą program (kompiluje się i działa) — to uwagi, jakie dostaje kod na PRZEGLĄDZIE
 *   (code review) od bardziej doświadczonego kolegi. Trzynaście typowych znalezisk, każde jako PRZED (kod, który
 *   „działa”, ale ma problem) i PO (poprawiona wersja, ten sam wynik, łatwiejsza w utrzymaniu) — z jednym zdaniem
 *   DLACZEGO to ma znaczenie w większym projekcie, z wieloma współautorami, miesiące/lata później.
 *
 * ANALOGIA: recenzja tekstu przed publikacją.
 *   Tekst bez błędów ortograficznych może być mimo to źle napisany: zdania zbyt długie, akapity bez tezy, słowa
 *   niejasne. Redaktor nie mówi „to się nie czyta” — mówi KONKRETNIE co poprawić i DLACZEGO czytelnik się pogubi.
 *   Code review działa tak samo: kod „działa”, ale ktoś inny (albo Ty za pół roku) będzie miał problem go zrozumieć.
 *
 * JAK TO DZIAŁA:
 *   Każde znalezisko: metoda …Bad(...) (PRZED) i …Good(...) (PO) — obie DAJĄ TEN SAM WYNIK (gdzie to ma sens),
 *   różni je tylko czytelność/bezpieczeństwo/wydajność. Kod się kompiluje w OBU wersjach — to properties jakości,
 *   nie poprawności, dlatego kompilator ich nie wyłapie. Stąd code review i statyczna analiza (linters).
 *
 * SŁÓWKA:
 *   magic number = liczba magiczna (bez nazwy, bez wyjaśnienia); guard clause = klauzula strażnicza (wczesny
 *   return kończący metodę); extracted method = wydzielona metoda; parameter object = obiekt parametrów;
 *   Law of Demeter = zasada Demeter ("nie gadaj z obcymi" — nie łańcuchuj getterów przez wiele obiektów);
 *   swallowed exception = połknięty wyjątek; resource leak = wyciek zasobu (niezamknięty plik/połączenie).
 *
 * ZOBACZ TEŻ: t27_clean_code_pitfalls/Pitfalls01Classic (pułapki językowe Javy), t27_clean_code_pitfalls/CleanCode01Principles
 *             (te same idee jako ZASADY, nie pojedyncze uwagi), t10_exceptions/Exceptions07BestPractices (wyjątki
 *             szczegółowo), t18_io_files (try-with-resources w akcji).
 * </pre>
 */
public class Pitfalls02CodeReview {

    public static void main(String[] args) {
        title("Pitfalls02 — typowe uwagi z code review");

        namingAndMagicNumbers();   // naming and magic numbers = nazwy i liczby magiczne
        structure();                  // structure = struktura kodu
        methodSignatures();          // method signatures = sygnatury metod
        returnValuesAndErrors();     // return values and errors = wartości zwracane i błędy
        documentationAndEncapsulation(); // documentation and encapsulation = dokumentacja i hermetyzacja
        dependenciesAndResources();  // dependencies and resources = zależności i zasoby
        performance();                 // performance = wydajność
        exercises();                    // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. CZYTELNOŚĆ: NAZWY I LICZBY MAGICZNE
    // =================================================================================================

    // --- 1a. magiczne liczby ---

    static double shippingCostBad(double weightKg) {
        if (weightKg > 10) {                 // 10 czego? kg? progu czego?
            return weightKg * 2.5 + 15;       // 2.5 i 15 — skąd te liczby?
        }
        return weightKg * 2.5;
    }

    static final double FREE_SHIPPING_THRESHOLD_KG = 10;
    static final double COST_PER_KG = 2.5;
    static final double OVERWEIGHT_SURCHARGE = 15;

    static double shippingCostGood(double weightKg) {
        double base = weightKg * COST_PER_KG;
        return weightKg > FREE_SHIPPING_THRESHOLD_KG ? base + OVERWEIGHT_SURCHARGE : base;
    }

    // --- 1b. niejasne nazwy ---

    static int f(List<Integer> l) {
        int s = 0;
        for (int x : l) {
            s += x;
        }
        return s;
    }

    static int sumOf(List<Integer> numbers) {
        int sum = 0;
        for (int number : numbers) {
            sum += number;
        }
        return sum;
    }

    static void namingAndMagicNumbers() {
        section("1. Czytelność: liczby magiczne i niejasne nazwy");

        show("PRZED: shippingCostBad(12)", shippingCostBad(12));
        show("PO: shippingCostGood(12)", shippingCostGood(12));
        // WYNIK: PRZED: shippingCostBad(12) → 45.0
        // WYNIK: PO: shippingCostGood(12) → 45.0

        note("Ten sam wynik — różnica jest w CZYTELNOŚCI: FREE_SHIPPING_THRESHOLD_KG mówi WPROST, co oznacza 10.");
        // WYNIK:    ℹ Ten sam wynik — różnica jest w CZYTELNOŚCI: FREE_SHIPPING_THRESHOLD_KG mówi WPROST, co oznacza 10.
        // dlaczego: nazwana stała to jedno MIEJSCE do zmiany (np. progu) i dokumentacja "w locie" — czytający
        //   nie musi zgadywać, skąd wzięła się liczba 2.5.

        line();

        show("PRZED: f(List.of(1,2,3))", f(List.of(1, 2, 3)));
        show("PO: sumOf(List.of(1,2,3))", sumOf(List.of(1, 2, 3)));
        // WYNIK: PRZED: f(List.of(1,2,3)) → 6
        // WYNIK: PO: sumOf(List.of(1,2,3)) → 6

        // dlaczego: f/l/s/x nic nie mówią — trzeba PRZECZYTAĆ całe ciało, żeby zgadnąć przeznaczenie. sumOf/numbers/
        //   sum/number są SAMOOPISUJĄCE — kod czyta się jak zdanie, bez deszyfrowania skrótów.
    }

    // =================================================================================================
    // 2. STRUKTURA: ZAGNIEŻDŻENIE I DUPLIKACJA
    // =================================================================================================

    // --- 2a. głębokie zagnieżdżenie vs klauzule strażnicze ---

    static String classifyOrderBad(Integer amount, boolean vip, boolean paid) {
        String result;
        if (amount != null) {
            if (amount > 0) {
                if (paid) {
                    if (vip) {
                        result = "VIP priorytet";
                    } else {
                        result = "standardowa realizacja";
                    }
                } else {
                    result = "czeka na płatność";
                }
            } else {
                result = "błędna kwota";
            }
        } else {
            result = "brak kwoty";
        }
        return result;
    }

    static String classifyOrderGood(Integer amount, boolean vip, boolean paid) {
        if (amount == null) {
            return "brak kwoty";
        }
        if (amount <= 0) {
            return "błędna kwota";
        }
        if (!paid) {
            return "czeka na płatność";
        }
        if (vip) {
            return "VIP priorytet";
        }
        return "standardowa realizacja";
    }

    // --- 2b. duplikacja vs wydzielona metoda ---

    static String describeProductBad(String name, double price) {
        String trimmedName = name == null ? "" : name.trim();
        if (trimmedName.isEmpty()) {
            trimmedName = "(bez nazwy)";
        }
        return trimmedName + ": " + String.format(Locale.ROOT, "%.2f zł", price);
    }

    static String describeCustomerBad(String name, double balance) {
        String trimmedName = name == null ? "" : name.trim();     // ten sam kod co wyżej — SKOPIOWANY
        if (trimmedName.isEmpty()) {
            trimmedName = "(bez nazwy)";
        }
        return trimmedName + " (saldo " + String.format(Locale.ROOT, "%.2f zł", balance) + ")";
    }

    static String normalizeName(String name) {
        String trimmed = name == null ? "" : name.trim();
        return trimmed.isEmpty() ? "(bez nazwy)" : trimmed;
    }

    static String describeProductGood(String name, double price) {
        return normalizeName(name) + ": " + String.format(Locale.ROOT, "%.2f zł", price);
    }

    static String describeCustomerGood(String name, double balance) {
        return normalizeName(name) + " (saldo " + String.format(Locale.ROOT, "%.2f zł", balance) + ")";
    }

    static void structure() {
        section("2. Struktura: głębokie zagnieżdżenie, duplikacja kodu");

        show("PRZED: classifyOrderBad(100, true, true)", classifyOrderBad(100, true, true));
        show("PO: classifyOrderGood(100, true, true)", classifyOrderGood(100, true, true));
        show("PO: classifyOrderGood(null, false, false)", classifyOrderGood(null, false, false));
        // WYNIK: PRZED: classifyOrderBad(100, true, true) → VIP priorytet
        // WYNIK: PO: classifyOrderGood(100, true, true) → VIP priorytet
        // WYNIK: PO: classifyOrderGood(null, false, false) → brak kwoty

        // dlaczego: klauzula strażnicza (guard clause = wczesny return dla przypadku brzegowego) SPŁASZCZA kod —
        //   4 poziomy zagnieżdżenia zamieniają się w listę niezależnych warunków, czytanych z góry na dół.

        line();

        show("PRZED: describeProductBad(\"  \", 9.5)", describeProductBad("  ", 9.5));
        show("PO: describeProductGood(\"  \", 9.5)", describeProductGood("  ", 9.5));
        show("PO: describeCustomerGood(\"Ala\", 100.0)", describeCustomerGood("Ala", 100.0));
        // WYNIK: PRZED: describeProductBad("  ", 9.5) → (bez nazwy): 9.50 zł
        // WYNIK: PO: describeProductGood("  ", 9.5) → (bez nazwy): 9.50 zł
        // WYNIK: PO: describeCustomerGood("Ala", 100.0) → Ala (saldo 100.00 zł)

        // dlaczego: logika normalizacji nazwy była SKOPIOWANA w dwóch metodach — poprawka błędu (np. obsługi
        //   samych spacji) wymagałaby zmiany w DWÓCH miejscach. Wydzielona metoda normalizeName to JEDNO miejsce prawdy.
    }

    // =================================================================================================
    // 3. SYGNATURY METOD: FLAGI I DŁUGIE LISTY PARAMETRÓW
    // =================================================================================================

    // --- 3a. parametr-flaga logiczna ---

    static List<Integer> sortNumbersBad(List<Integer> numbers, boolean descending) {
        List<Integer> copy = new ArrayList<>(numbers);
        copy.sort(descending ? Comparator.reverseOrder() : Comparator.naturalOrder());
        return copy;
    }

    static List<Integer> sortAscending(List<Integer> numbers) {
        List<Integer> copy = new ArrayList<>(numbers);
        copy.sort(Comparator.naturalOrder());
        return copy;
    }

    static List<Integer> sortDescending(List<Integer> numbers) {
        List<Integer> copy = new ArrayList<>(numbers);
        copy.sort(Comparator.reverseOrder());
        return copy;
    }

    // --- 3b. długa lista parametrów vs obiekt parametrów ---

    /** InvoiceCustomer = obiekt parametrów (parameter object) — grupuje dane klienta w JEDNĄ całość. */
    record InvoiceCustomer(String name, String city, String email) {
    }

    /** InvoiceLine = obiekt parametrów dla pozycji faktury. */
    record InvoiceLine(String productName, double price, int quantity) {
    }

    static String createInvoiceBad(String customerName, String customerCity, String customerEmail,
                                    String productName, double productPrice, int quantity, boolean vip) {
        double total = productPrice * quantity;
        return customerName + " (" + customerCity + "): " + quantity + "x " + productName
                + " = " + String.format(Locale.ROOT, "%.2f zł", total) + (vip ? " [VIP]" : "");
    }

    static String createInvoiceGood(InvoiceCustomer customer, InvoiceLine line, boolean vip) {
        double total = line.price() * line.quantity();
        return customer.name() + " (" + customer.city() + "): " + line.quantity() + "x " + line.productName()
                + " = " + String.format(Locale.ROOT, "%.2f zł", total) + (vip ? " [VIP]" : "");
    }

    static void methodSignatures() {
        section("3. Sygnatury metod: flagi logiczne, długie listy parametrów");

        show("PRZED: sortNumbersBad(nums, true)  — 'true' czego?", sortNumbersBad(List.of(3, 1, 2), true));
        show("PO: sortDescending(nums)  — nazwa mówi wszystko", sortDescending(List.of(3, 1, 2)));
        // WYNIK: PRZED: sortNumbersBad(nums, true)  — 'true' czego? → [3, 2, 1]
        // WYNIK: PO: sortDescending(nums)  — nazwa mówi wszystko → [3, 2, 1]

        // dlaczego: w miejscu wywołania sortNumbersBad(list, true) NIE WIADOMO, co znaczy "true" bez zaglądania
        //   do sygnatury. sortDescending(list) jest jednoznaczne. (Dla >2 wariantów: enum zamiast wielu boolean.)

        line();

        String badCall = createInvoiceBad("Jan Kowalski", "Warszawa", "jan@example.com", "Laptop Pro 14", 5499.99, 1, true);
        String goodCall = createInvoiceGood(
                new InvoiceCustomer("Jan Kowalski", "Warszawa", "jan@example.com"),
                new InvoiceLine("Laptop Pro 14", 5499.99, 1),
                true);
        show("PRZED: createInvoiceBad(7 pozycyjnych argumentów)", badCall);
        show("PO: createInvoiceGood(customer, line, vip)", goodCall);
        // WYNIK: PRZED: createInvoiceBad(7 pozycyjnych argumentów) → Jan Kowalski (Warszawa): 1x Laptop Pro 14 = 5499.99 zł [VIP]
        // WYNIK: PO: createInvoiceGood(customer, line, vip) → Jan Kowalski (Warszawa): 1x Laptop Pro 14 = 5499.99 zł [VIP]

        // dlaczego: przy 7 argumentach TEGO SAMEGO typu (String, String, String...) łatwo pomylić kolejność —
        //   kompilator tego nie wykryje. Rekordy InvoiceCustomer/InvoiceLine grupują powiązane dane i same się dokumentują.
    }

    // =================================================================================================
    // 4. WARTOŚCI ZWRACANE I BŁĘDY
    // =================================================================================================

    // --- 4a. null zamiast pustej kolekcji ---

    static List<String> findTagsBad(Map<String, List<String>> tagsByCategory, String category) {
        return tagsByCategory.get(category);   // null, gdy category nie istnieje w mapie!
    }

    static List<String> findTagsGood(Map<String, List<String>> tagsByCategory, String category) {
        return tagsByCategory.getOrDefault(category, List.of());
    }

    // --- 4b. połknięty wyjątek ---

    static int parseAllBad(List<String> values) {
        int sum = 0;
        for (String v : values) {
            try {
                sum += Integer.parseInt(v);
            } catch (NumberFormatException e) {
                // puste — błąd znika bez śladu, suma jest CICHO zaniżona
            }
        }
        return sum;
    }

    static int parseAllGood(List<String> values) {
        int sum = 0;
        for (String v : values) {
            try {
                sum += Integer.parseInt(v);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Zła liczba: \"" + v + "\"", e);
            }
        }
        return sum;
    }

    static void returnValuesAndErrors() {
        section("4. Wartości zwracane i błędy: null zamiast pustej kolekcji, połknięty wyjątek");

        Map<String, List<String>> tags = Map.of("owoce", List.of("słodkie", "zdrowe"));
        expectThrows("PRZED: findTagsBad(tags, \"warzywa\").size()", () -> findTagsBad(tags, "warzywa").size());
        show("PO: findTagsGood(tags, \"warzywa\")", findTagsGood(tags, "warzywa"));
        // WYNIK: ✔ PRZED: findTagsBad(tags, "warzywa").size() → rzucono NullPointerException: Cannot invoke "java.util.List.size()" because the return value of "t27_clean_code_pitfalls.Pitfalls02CodeReview.findTagsBad(java.util.Map, String)" is null
        // WYNIK: PO: findTagsGood(tags, "warzywa") → []

        // dlaczego: caller ZWYKLE zakłada, że kolekcja to kolekcja (może być pusta, ale nie null) — getOrDefault
        //   z List.of() eliminuje CAŁĄ klasę potencjalnych NPE u wywołujących.

        line();

        List<String> prices = List.of("100", "12O", "30");   // "12O" — litera O zamiast zera
        show("PRZED: parseAllBad (zła cena znika po cichu)", parseAllBad(prices));
        expectThrows("PO: parseAllGood (zła cena ujawniona)", () -> parseAllGood(prices));
        // WYNIK: PRZED: parseAllBad (zła cena znika po cichu) → 130
        // WYNIK: ✔ PO: parseAllGood (zła cena ujawniona) → rzucono IllegalArgumentException: Zła liczba: "12O"

        // dlaczego: pusty catch to najgroźniejszy rodzaj błędu — program działa "poprawnie" na złych danych
        //   (t10_exceptions/Exceptions07BestPractices ma to rozwinięte szerzej).
    }

    // =================================================================================================
    // 5. DOKUMENTACJA I HERMETYZACJA
    // =================================================================================================

    // --- 5a. komentarz, który kłamie ---

    // zwraca true, jeśli liczba jest parzysta      ← KŁAMSTWO: kod sprawdza coś zupełnie innego!
    static boolean isPositiveBad(int n) {
        return n > 0;
    }

    /** Zwraca true, gdy n jest większe od zera. */
    static boolean isPositiveGood(int n) {
        return n > 0;
    }

    // --- 5b. publiczne zmienne pole ---

    static class AccountBad {
        public double balance;    // KAŻDY kod może ustawić DOWOLNĄ wartość, nawet ujemną
    }

    static class AccountGood {
        private double balance;

        AccountGood(double initialBalance) {
            if (initialBalance < 0) {
                throw new IllegalArgumentException("Saldo początkowe nie może być ujemne: " + initialBalance);
            }
            this.balance = initialBalance;
        }

        void withdraw(double amount) {
            if (amount > balance) {
                throw new IllegalStateException("Brak środków: saldo " + balance + ", próba wypłaty " + amount);
            }
            balance -= amount;
        }

        double balance() {
            return balance;
        }
    }

    static void documentationAndEncapsulation() {
        section("5. Dokumentacja i hermetyzacja: kłamliwy komentarz, publiczne pole");

        show("isPositiveBad(4) — komentarz mówi 'parzysta', kod liczy co innego!", isPositiveBad(4));
        show("isPositiveGood(4)", isPositiveGood(4));
        // WYNIK: isPositiveBad(4) — komentarz mówi 'parzysta', kod liczy co innego! → true
        // WYNIK: isPositiveGood(4) → true

        // dlaczego: kompilator NIE SPRAWDZA komentarzy — z czasem kod się zmienia, a komentarz zostaje stary
        //   (komentarz "gnije"). Lepsza nazwa (isPositiveGood) eliminuje POTRZEBĘ komentarza opisującego CO.
        //   Komentarz ma sens, gdy tłumaczy DLACZEGO, nie CO (to widać z kodu).

        line();

        AccountBad bad = new AccountBad();
        bad.balance = -1_000_000;    // nikt tego nie zablokował — pole jest publiczne
        show("PRZED: AccountBad — dowolne przypisanie do balance", bad.balance);
        // WYNIK: PRZED: AccountBad — dowolne przypisanie do balance → -1000000.0

        expectThrows("PO: new AccountGood(-100) — walidacja w konstruktorze", () -> new AccountGood(-100));
        AccountGood good = new AccountGood(100);
        expectThrows("PO: good.withdraw(500) — walidacja przy wypłacie", () -> good.withdraw(500));
        // WYNIK: ✔ PO: new AccountGood(-100) — walidacja w konstruktorze → rzucono IllegalArgumentException: Saldo początkowe nie może być ujemne: -100.0
        // WYNIK: ✔ PO: good.withdraw(500) — walidacja przy wypłacie → rzucono IllegalStateException: Brak środków: saldo 100.0, próba wypłaty 500.0

        // dlaczego: publiczne pole = ZERO kontroli nad niezmiennikiem (balance ≥ 0). Prywatne pole + metody
        //   pilnujące reguł to jedyny sposób, żeby obiekt NIGDY nie znalazł się w niepoprawnym stanie.
    }

    // =================================================================================================
    // 6. ZALEŻNOŚCI I ZASOBY
    // =================================================================================================

    // --- 6a. Prawo Demeter (Law of Demeter) ---

    record Address(String city) {
    }

    record ReviewCustomer(Address address) {
    }

    record ReviewOrder(ReviewCustomer customer) {
        /** cityOfCustomer = JEDEN "skok" zamiast trzech — delegacja ukrywa wewnętrzną strukturę Order. */
        String cityOfCustomer() {
            return customer.address().city();
        }
    }

    static void dependenciesAndResources() {
        section("6. Zależności i zasoby: Prawo Demeter, niezamknięte zasoby");

        ReviewOrder order = new ReviewOrder(new ReviewCustomer(new Address("Kraków")));
        show("PRZED: order.customer().address().city() — trzy 'kropki w głąb'", order.customer().address().city());
        show("PO: order.cityOfCustomer() — jeden skok", order.cityOfCustomer());
        // WYNIK: PRZED: order.customer().address().city() — trzy 'kropki w głąb' → Kraków
        // WYNIK: PO: order.cityOfCustomer() — jeden skok → Kraków

        // dlaczego: kod sięgający przez a.getB().getC().doIt() ZNA wewnętrzną budowę TRZECH klas naraz — zmiana
        //   struktury Customer/Address wymusza zmianę WSZĘDZIE, gdzie ktoś tak "sięgnął w głąb". Metoda delegująca
        //   (cityOfCustomer) chowa tę strukturę za JEDNYM, stabilnym punktem dostępu (Prawo Demeter: "nie gadaj z obcymi").

        line();

        show("PRZED: wasClosedBad() — nikt nie wywołał close()", wasClosedBad());
        show("PO: wasClosedGood() — try-with-resources zamyka automatycznie", wasClosedGood());
        // WYNIK: PRZED: wasClosedBad() — nikt nie wywołał close() → false
        // WYNIK: PO: wasClosedGood() — try-with-resources zamyka automatycznie → true

        // dlaczego: zasoby (pliki, połączenia) trzeba ZAMYKAĆ, inaczej "wyciekają" (system w końcu zabraknie
        //   uchwytów/pamięci). try-with-resources woła close() ZAWSZE, nawet gdy w bloku poleci wyjątek
        //   (t18_io_files ma to szerzej rozwinięte).
    }

    /** FakeResource = udawany zasób (plik/połączenie) na potrzeby demo — implementuje AutoCloseable. */
    static class FakeResource implements AutoCloseable {
        boolean closed = false;

        void use() {
            // w realu: odczyt/zapis
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    static boolean wasClosedBad() {
        FakeResource r = new FakeResource();
        r.use();
        return r.closed;              // nikt nie wywołał close() — zasób "wycieka"
    }

    static boolean wasClosedGood() {
        FakeResource captured;
        try (FakeResource r = new FakeResource()) {
            captured = r;
            r.use();
        }   // <- tu automatycznie wywołane r.close(), NAWET gdyby powyżej poleciał wyjątek
        return captured.closed;
    }

    // =================================================================================================
    // 7. WYDAJNOŚĆ: KONKATENACJA W PĘTLI
    // =================================================================================================

    static String joinBad(List<String> words) {
        String result = "";
        for (String w : words) {
            result += w + ",";     // KAŻDA iteracja tworzy NOWY obiekt String (niezmienność!)
        }
        return result;
    }

    static String joinGood(List<String> words) {
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            sb.append(w).append(",");
        }
        return sb.toString();
    }

    static void performance() {
        section("7. Wydajność: konkatenacja Stringów w pętli");

        List<String> words = List.of("java", "stream", "lambda");
        show("PRZED: joinBad(words)", joinBad(words));
        show("PO: joinGood(words)", joinGood(words));
        // WYNIK: PRZED: joinBad(words) → java,stream,lambda,
        // WYNIK: PO: joinGood(words) → java,stream,lambda,

        note("Ten sam wynik dla 3 słów — różnica ujawnia się przy TYSIĄCACH iteracji: += kopiuje CAŁY napis");
        note("za każdym razem (rząd O(n²)), StringBuilder rośnie amortyzowanie w rzędzie O(n).");
        // WYNIK:    ℹ Ten sam wynik dla 3 słów — różnica ujawnia się przy TYSIĄCACH iteracji: += kopiuje CAŁY napis
        // WYNIK:    ℹ za każdym razem (rząd O(n²)), StringBuilder rośnie amortyzowanie w rzędzie O(n).

        // dlaczego: String jest NIEZMIENNY — result += w tworzy NOWY String i KOPIUJE do niego stary. Dla n
        //   elementów to n kopii coraz dłuższego napisu. StringBuilder.append modyfikuje wewnętrzny bufor w miejscu.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • liczba magiczna → nazwana stała (static final); niejasna nazwa → nazwa mówiąca CO i PO CO.
     *   • głębokie zagnieżdżenie → klauzule strażnicze (wczesny return); duplikacja → wydzielona metoda.
     *   • parametr-flaga logiczna → dwie nazwane metody (albo enum przy >2 wariantach).
     *   • długa lista parametrów tego samego typu → obiekt parametrów (record).
     *   • null zamiast pustej kolekcji → Collections/List.of()/getOrDefault; połknięty wyjątek → obsłuż albo rzuć dalej.
     *   • komentarz opisujący CO (może skłamać) → dobra nazwa; komentarz ma tłumaczyć DLACZEGO.
     *   • publiczne zmienne pole → prywatne pole + metody pilnujące niezmiennika.
     *   • a.getB().getC().doIt() (Prawo Demeter) → metoda delegująca, jeden stabilny punkt dostępu.
     *   • zasób bez close() → try-with-resources (AutoCloseable).
     *   • += Stringów w pętli → StringBuilder.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego kompilator NIE wykrywa żadnego z tych 13 problemów, skoro kod z nimi działa poprawnie?
     *   2. Co wypisze:  System.out.println(findTagsBad(Map.of("a", List.of("x")), "b"));  ?
     *   3. ZNAJDŹ BŁĄD (kod kompiluje się i "działa", ale ma DWA znaleziska z tej lekcji):
     *          static String desc(String n, double p, boolean d) {
     *              if (n != null) { if (!n.isEmpty()) { return n + ": " + p; } else { return "?: " + p; } }
     *              return "brak: " + p;
     *          }
     *   4. Dlaczego sortNumbersBad(list, true) jest gorsze od sortDescending(list), skoro robią to samo?
     *   5. Jaką regułę łamie order.customer().address().city() i jak nazywa się ta zasada?
     *   6. Dlaczego try-with-resources gwarantuje wywołanie close() NAWET, gdy w bloku poleci wyjątek?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");

        Check.equal("ćw. 1a: classifyGrade(null)", "brak oceny", () -> exercise1(null));
        Check.equal("ćw. 1b: classifyGrade(-5)", "nieprawidłowy wynik", () -> exercise1(-5));
        Check.equal("ćw. 1c: classifyGrade(95)", "celujący", () -> exercise1(95));
        Check.equal("ćw. 1d: classifyGrade(80)", "dobry", () -> exercise1(80));
        Check.equal("ćw. 1e: classifyGrade(50)", "wystarczający", () -> exercise1(50));
        Check.equal("ćw. 2a: findTags — istniejąca kategoria", List.of("x"),
                () -> exercise2(Map.of("a", List.of("x")), "a"));
        Check.equal("ćw. 2b: findTags — brak kategorii → pusta lista", List.of(),
                () -> exercise2(Map.of("a", List.of("x")), "b"));
        Check.equal("ćw. 3: join z separatorem, bez końcowego separatora", "a-b-c",
                () -> exercise3(List.of("a", "b", "c"), "-"));
        Check.equal("ćw. 4: cityOf — jeden skok zamiast trzech", "Poznań",
                () -> exercise4(new ReviewOrder(new ReviewCustomer(new Address("Poznań")))));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec, null)", "brak oceny", () -> solution1(null));
        Check.equal("ćw. 1 (wzorzec, 95)", "celujący", () -> solution1(95));
        Check.equal("ćw. 2a (wzorzec)", List.of("x"), () -> solution2(Map.of("a", List.of("x")), "a"));
        Check.equal("ćw. 2b (wzorzec)", List.of(), () -> solution2(Map.of("a", List.of("x")), "b"));
        Check.equal("ćw. 3 (wzorzec)", "a-b-c", () -> solution3(List.of("a", "b", "c"), "-"));
        Check.equal("ćw. 4 (wzorzec)", "Poznań", () -> solution4(new ReviewOrder(new ReviewCustomer(new Address("Poznań")))));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe, „PRZEPISZ”): stara wersja miała głębokie zagnieżdżenie:
     * <pre>{@code
     * static String classifyGradeOld(Integer score) {
     *     String result;
     *     if (score != null) {
     *         if (score >= 0 && score <= 100) {
     *             if (score >= 90) { result = "celujący"; }
     *             else { if (score >= 75) { result = "dobry"; } else { result = "wystarczający"; } }
     *         } else { result = "nieprawidłowy wynik"; }
     *     } else { result = "brak oceny"; }
     *     return result;
     * }
     * }</pre>
     * Przepisz jako classifyGrade używając klauzul strażniczych (guard clauses, wczesny return) — bez zagnieżdżania.
     */
    static String exercise1(Integer score) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (łatwe): popraw znalezisko „null zamiast pustej kolekcji” — findTags(...) dla nieznanej kategorii
     * ma zwrócić PUSTĄ listę, nie null.
     */
    static List<String> exercise2(Map<String, List<String>> tagsByCategory, String category) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): popraw znalezisko „konkatenacja w pętli” — połącz słowa podanym separatorem,
     * używając StringBuilder, BEZ końcowego separatora po ostatnim słowie (trudniejsze niż joinGood z sekcji 7!).
     * Podpowiedź: dodawaj separator PRZED słowem, ale nie przed PIERWSZYM (sprawdź indeks albo sb.length() == 0).
     */
    static String exercise3(List<String> words, String separator) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (średnie): popraw złamanie Prawa Demeter — zwróć miasto klienta w JEDNYM wywołaniu metody
     * na order (analogicznie do ReviewOrder.cityOfCustomer() z sekcji 6), zamiast order.customer().address().city().
     */
    static String exercise4(ReviewOrder order) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(Integer score) {
        if (score == null) {
            return "brak oceny";
        }
        if (score < 0 || score > 100) {
            return "nieprawidłowy wynik";
        }
        if (score >= 90) {
            return "celujący";
        }
        if (score >= 75) {
            return "dobry";
        }
        return "wystarczający";
    }

    static List<String> solution2(Map<String, List<String>> tagsByCategory, String category) {
        return tagsByCategory.getOrDefault(category, List.of());
    }

    static String solution3(List<String> words, String separator) {
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (sb.length() > 0) {
                sb.append(separator);
            }
            sb.append(w);
        }
        return sb.toString();
    }

    static String solution4(ReviewOrder order) {
        return order.cityOfCustomer();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo to są problemy JAKOŚCI (czytelność, bezpieczeństwo na przyszłość, wydajność), nie POPRAWNOŚCI
     *      składniowej czy typów — kompilator sprawdza gramatykę języka, nie "dobry styl".
     *   2. Wypisze null — mapa nie ma klucza "b", get(...) zwraca null, a println(null) wypisuje dosłownie "null".
     *   3. Głęboka (podwójna) zagnieżdżona struktura if/else zamiast klauzul strażniczych ORAZ niejasne nazwy
     *      parametrów (n, p, d) zamiast np. name, price, discounted.
     *   4. sortNumbersBad(list, true) wymaga zajrzenia do sygnatury metody, żeby zrozumieć, co znaczy "true" w
     *      miejscu wywołania — sortDescending(list) jest jednoznaczne bez dodatkowego kontekstu.
     *   5. Prawo Demeter (Law of Demeter, „nie gadaj z obcymi") — kod zna wewnętrzną strukturę aż TRZECH klas
     *      naraz (Order, Customer, Address) zamiast rozmawiać tylko z bezpośrednim sąsiadem.
     *   6. Blok try-with-resources jest w rzeczywistości cukrem składniowym na try/finally — close() jest
     *      wywoływane w NIEJAWNYM finally, które wykonuje się ZAWSZE, niezależnie od tego, czy poleciał wyjątek.
     */
    // </editor-fold>
}
