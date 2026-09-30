package t27_clean_code_pitfalls;

import helpers.Check;

import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Zasady czystego kodu — nazewnictwo, funkcje, DRY/KISS/YAGNI i inne
 *        (clean code = czysty kod)
 *
 * W SKRÓCIE:
 *   Pitfalls02CodeReview pokazywał POJEDYNCZE uwagi z przeglądu kodu. Tutaj są ZASADY — bardziej ogólne reguły,
 *   z których te uwagi wynikają. Dziesięć zasad, każda na małym przykładzie PRZED (kod działający, ale słaby)
 *   i PO (ten sam wynik, lepsza jakość) — z jednym zdaniem DLACZEGO to ma znaczenie.
 *
 * ANALOGIA: przepis kulinarny.
 *   Zły przepis: "wrzuć trochę tego, trochę tamtego, dopraw do smaku" — działa tylko w rękach autora. Dobry
 *   przepis: nazwane składniki z ilościami, kroki w logicznej kolejności, jeden krok = jedna czynność. Kod
 *   działa tak samo: ktoś inny (albo Ty za pół roku) musi go "ugotować" bez pytania autora, co miał na myśli.
 *
 * JAK TO DZIAŁA:
 *   Każda zasada: metoda …Bad(...) / stara wersja (PRZED) i …Good(...) / nowa wersja (PO) — ten sam wynik,
 *   różna jakość. Kompilator akceptuje OBIE wersje — to znowu (jak w Pitfalls02) problem jakości, nie
 *   poprawności składniowej, dlatego te zasady trzeba znać i stosować świadomie, a nie liczyć na kompilator.
 *
 * SŁÓWKA:
 *   DRY = Don't Repeat Yourself (nie powtarzaj się); KISS = Keep It Simple, Stupid (niech będzie prosto);
 *   YAGNI = You Aren't Gonna Need It (nie będzie Ci to potrzebne); CQS = Command-Query Separation (rozdział
 *   poleceń od zapytań); query = zapytanie (zwraca dane, bez efektu ubocznego); command = polecenie (zmienia
 *   stan, nic nie zwraca); fail fast = szybkie ujawnianie błędu; guard clause = klauzula strażnicza (wczesny
 *   return); aliasing = dwie zmienne wskazujące na TEN SAM obiekt; value object = obiekt-wartość (t06_oop_basics).
 *
 * ZOBACZ TEŻ: t27_clean_code_pitfalls/Pitfalls02CodeReview (te same idee jako pojedyncze uwagi z review),
 *             t27_clean_code_pitfalls/CleanCode02Solid (SOLID — kolejny poziom, na jednym większym przykładzie),
 *             t06_oop_basics/Oop06Immutability (niezmienność szczegółowo), t22_design_patterns/Patterns01Strategy
 *             (zamiana if/else na strategię — zobaczysz to znów w CleanCode02).
 * </pre>
 */
public class CleanCode01Principles {

    public static void main(String[] args) {
        title("CleanCode01 — zasady czystego kodu");

        naming();                       // naming = nazewnictwo
        smallFunctions();               // small functions = małe funkcje
        dry();                          // DRY = nie powtarzaj się
        kiss();                         // KISS = niech będzie prosto
        yagni();                        // YAGNI = nie będzie Ci to potrzebne
        commandQuerySeparation();       // command-query separation = rozdział poleceń od zapytań
        failFast();                     // fail fast = szybkie ujawnianie błędu
        immutability();                 // immutability = niezmienność
        selfDocumentingCode();          // self-documenting code = kod tłumaczący się sam
        consistentFormatting();         // consistent formatting = spójne formatowanie
        exercises();                    // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. NAZEWNICTWO (NAMING)
    // =================================================================================================

    static int countBad(List<Integer> l, int t) {
        int c = 0;
        for (int x : l) {
            if (x > t) {
                c++;
            }
        }
        return c;
    }

    static int countAboveThreshold(List<Integer> numbers, int threshold) {
        int count = 0;
        for (int number : numbers) {
            if (number > threshold) {
                count++;
            }
        }
        return count;
    }

    static boolean flagBad(int stock) {
        return stock > 0;
    }

    static boolean isInStock(int stock) {
        return stock > 0;
    }

    /**
     * 1. Nazwy zmiennych/metod powinny mówić CO robią i po CO istnieją — czytelnik nie powinien musieć
     * czytać całego ciała metody, żeby zgadnąć jej przeznaczenie. Boolean: prefiks is/has czyni z wywołania
     * zdanie ({@code if (isInStock(stock))} czyta się jak pytanie).
     */
    static void naming() {
        section("1. Nazewnictwo: nazwy mówiące CO i PO CO");

        List<Integer> numbers = List.of(5, 12, 8, 20, 3);
        show("PRZED: countBad(numbers, 10)  — l? t? c? x?", countBad(numbers, 10));
        show("PO: countAboveThreshold(numbers, 10)", countAboveThreshold(numbers, 10));
        // WYNIK: PRZED: countBad(numbers, 10)  — l? t? c? x? → 2
        // WYNIK: PO: countAboveThreshold(numbers, 10) → 2

        // dlaczego: countBad wymaga PRZECZYTANIA ciała, żeby zrozumieć, co liczy i względem czego. Nazwy
        //   numbers/threshold/count w countAboveThreshold odpowiadają na to pytanie BEZ czytania kodu.

        line();

        show("PRZED: flagBad(5)  — 'flag' czego?", flagBad(5));
        show("PO: isInStock(5)  — czyta się jak pytanie", isInStock(5));
        // WYNIK: PRZED: flagBad(5)  — 'flag' czego? → true
        // WYNIK: PO: isInStock(5)  — czyta się jak pytanie → true

        // dlaczego: prefiks is/has dla metod zwracających boolean sprawia, że wywołanie w warunku
        //   (if (isInStock(stock))) czyta się jak zdanie po polsku/angielsku, bez tłumaczenia w głowie.
    }

    // =================================================================================================
    // 2. MAŁE FUNKCJE ROBIĄCE JEDNĄ RZECZ, JEDEN POZIOM ABSTRAKCJI
    // =================================================================================================

    /** OrderLine = wynik sparsowania linii "produkt,ilość,cena" — jeden poziom: same dane, bez logiki. */
    private record OrderLine(String product, int quantity, int unitPrice) {
    }

    static String summarizeOrderBad(String rawLine) {
        String[] parts = rawLine.split(",");                 // poziom NISKI: parsowanie tekstu
        String product = parts[0];
        int quantity = Integer.parseInt(parts[1]);
        int unitPrice = Integer.parseInt(parts[2]);
        if (quantity <= 0) {                                    // poziom WYSOKI: reguła biznesowa
            throw new IllegalArgumentException("Ilość musi być dodatnia");
        }
        int total = quantity * unitPrice;                        // poziom ŚREDNI: wyliczenie
        return product + " x" + quantity + " = " + total + " zł";  // poziom PREZENTACJI: formatowanie tekstu
    }

    private static OrderLine parseOrderLine(String rawLine) {
        String[] parts = rawLine.split(",");
        return new OrderLine(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    private static void validateQuantity(OrderLine line) {
        if (line.quantity() <= 0) {
            throw new IllegalArgumentException("Ilość musi być dodatnia");
        }
    }

    private static int computeTotal(OrderLine line) {
        return line.quantity() * line.unitPrice();
    }

    private static String formatSummary(OrderLine line, int total) {
        return line.product() + " x" + line.quantity() + " = " + total + " zł";
    }

    static String summarizeOrderGood(String rawLine) {
        OrderLine line = parseOrderLine(rawLine);
        validateQuantity(line);
        int total = computeTotal(line);
        return formatSummary(line, total);
    }

    /**
     * 2. summarizeOrderBad miesza w JEDNEJ metodzie trzy różne poziomy abstrakcji: jak rozbić String (nisko),
     * jaka jest reguła biznesowa (wysoko), jak sformatować tekst (prezentacja). summarizeOrderGood to
     * ORKIESTRATOR — czyta się jak przepis (parsuj, waliduj, policz, sformatuj), a SZCZEGÓŁY każdego kroku
     * są w osobnej, małej metodzie robiącej DOKŁADNIE jedną rzecz.
     */
    static void smallFunctions() {
        section("2. Małe funkcje: jedna rzecz, jeden poziom abstrakcji");

        show("PRZED: summarizeOrderBad(\"Kubek,3,12\")", summarizeOrderBad("Kubek,3,12"));
        show("PO: summarizeOrderGood(\"Kubek,3,12\")", summarizeOrderGood("Kubek,3,12"));
        // WYNIK: PRZED: summarizeOrderBad("Kubek,3,12") → Kubek x3 = 36 zł
        // WYNIK: PO: summarizeOrderGood("Kubek,3,12") → Kubek x3 = 36 zł

        // dlaczego: w summarizeOrderGood każda linia orkiestratora to WYWOŁANIE nazwanej metody — nie trzeba
        //   znać SZCZEGÓŁÓW parsowania, żeby zrozumieć CAŁOŚĆ przepływu. To "Single Responsibility" zastosowane
        //   do FUNKCJI, nie tylko do klasy: jedna funkcja = jeden powód do zmiany, jeden poziom szczegółowości.
    }

    // =================================================================================================
    // 3. DRY — DON'T REPEAT YOURSELF
    // =================================================================================================

    static String registerStudentBad(String name, int age) {
        if (name == null || name.isBlank()) {                     // (Java 11+) isBlank = "czy puste/same spacje"
            throw new IllegalArgumentException("Imię i nazwisko nie może być puste");
        }
        if (age < 0 || age > 130) {
            throw new IllegalArgumentException("Nieprawidłowy wiek: " + age);
        }
        return "Student: " + name.trim() + " (" + age + " lat)";
    }

    static String registerEmployeeBad(String name, int age) {
        if (name == null || name.isBlank()) {                     // TEN SAM kod co wyżej — SKOPIOWANY
            throw new IllegalArgumentException("Imię i nazwisko nie może być puste");
        }
        if (age < 0 || age > 130) {
            throw new IllegalArgumentException("Nieprawidłowy wiek: " + age);
        }
        return "Pracownik: " + name.trim() + " (" + age + " lat)";
    }

    private static void validatePersonData(String name, int age) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Imię i nazwisko nie może być puste");
        }
        if (age < 0 || age > 130) {
            throw new IllegalArgumentException("Nieprawidłowy wiek: " + age);
        }
    }

    static String registerStudentGood(String name, int age) {
        validatePersonData(name, age);
        return "Student: " + name.trim() + " (" + age + " lat)";
    }

    static String registerEmployeeGood(String name, int age) {
        validatePersonData(name, age);
        return "Pracownik: " + name.trim() + " (" + age + " lat)";
    }

    /**
     * 3. Ta sama walidacja (imię niepuste, wiek 0-130) była SKOPIOWANA w dwóch miejscach — poprawka reguły
     * (np. górnej granicy wieku) wymagałaby zmiany w OBU. validatePersonData to JEDNO miejsce prawdy.
     */
    static void dry() {
        section("3. DRY: nie powtarzaj tej samej logiki w dwóch miejscach");

        show("PO: registerStudentGood(\"Ala\", 20)", registerStudentGood("Ala", 20));
        show("PO: registerEmployeeGood(\"Jan\", 40)", registerEmployeeGood("Jan", 40));
        // WYNIK: PO: registerStudentGood("Ala", 20) → Student: Ala (20 lat)
        // WYNIK: PO: registerEmployeeGood("Jan", 40) → Pracownik: Jan (40 lat)

        expectThrows("PO: registerStudentGood(\"Ala\", 200) — reguła sprawdzona w JEDNYM miejscu",
                () -> registerStudentGood("Ala", 200));
        expectThrows("PO: registerEmployeeGood(\"Jan\", 200) — ta sama reguła, ten sam efekt",
                () -> registerEmployeeGood("Jan", 200));
        // WYNIK: ✔ PO: registerStudentGood("Ala", 200) — reguła sprawdzona w JEDNYM miejscu → rzucono IllegalArgumentException: Nieprawidłowy wiek: 200
        // WYNIK: ✔ PO: registerEmployeeGood("Jan", 200) — ta sama reguła, ten sam efekt → rzucono IllegalArgumentException: Nieprawidłowy wiek: 200

        // dlaczego: registerStudentBad/registerEmployeeBad DZIAŁAJĄ identycznie (sprawdź samodzielnie) — problem
        //   nie w wyniku, tylko w tym, że naprawa błędu w regule wymagałaby pamiętać o WSZYSTKICH kopiach.
    }

    // =================================================================================================
    // 4. KISS — KEEP IT SIMPLE, STUPID
    // =================================================================================================

    static boolean isLeapBad(int year) {
        return (year % 4 == 0 ? (year % 100 == 0 ? (year % 400 == 0 ? true : false) : true) : false);
    }

    static boolean isLeapGood(int year) {
        if (year % 400 == 0) {
            return true;
        }
        if (year % 100 == 0) {
            return false;
        }
        return year % 4 == 0;
    }

    /**
     * 4. Obie metody dają TEN SAM wynik dla każdego roku — isLeapBad "popisuje się" zagnieżdżonymi trójargumentowymi
     * operatorami (?:) tam, gdzie wystarczą dwie klauzule strażnicze. "Sprytny" kod nie jest lepszy niż prosty,
     * jeśli robi TO SAMO — a jest trudniejszy do przeczytania.
     */
    static void kiss() {
        section("4. KISS: niech będzie prosto, bez sztucznej finezji");

        show("PRZED: isLeapBad(2024)  — zagnieżdżone ?: trudne do prześledzenia", isLeapBad(2024));
        show("PO: isLeapGood(2024)", isLeapGood(2024));
        show("PRZED: isLeapBad(1900)  — 'stulecie', nie podzielne przez 400", isLeapBad(1900));
        show("PO: isLeapGood(1900)", isLeapGood(1900));
        // WYNIK: PRZED: isLeapBad(2024)  — zagnieżdżone ?: trudne do prześledzenia → true
        // WYNIK: PO: isLeapGood(2024) → true
        // WYNIK: PRZED: isLeapBad(1900)  — 'stulecie', nie podzielne przez 400 → false
        // WYNIK: PO: isLeapGood(1900) → false

        // dlaczego: potrójnie zagnieżdżony operator ?: wymaga liczenia nawiasów, żeby zrozumieć, KTÓRY warunek
        //   odpowiada za KTÓRĄ gałąź. Trzy proste instrukcje if czyta się z góry na dół, bez liczenia nawiasów —
        //   a wynik jest identyczny. Prostota to nie brak umiejętności, to świadomy wybór dla czytelnika.
    }

    // =================================================================================================
    // 5. YAGNI — YOU AREN'T GONNA NEED IT
    // =================================================================================================

    static String describeProductBad(String name, String colorFilterUnused, Integer maxLengthUnused, Boolean htmlEscapeUnused) {
        // colorFilterUnused, maxLengthUnused, htmlEscapeUnused — "elastyczność na przyszłość", nikt jej NIE UŻYWA
        return "[" + name + "]";
    }

    static String describeProductGood(String name) {
        return "[" + name + "]";
    }

    /**
     * 5. describeProductBad przewiduje trzy funkcje, których DZIŚ nikt nie potrzebuje (filtr koloru, limit
     * długości, escapowanie HTML) — każde wywołanie musi przekazać puste/null wartości dla nieużywanych
     * parametrów. Dodamy je, GDY faktycznie będą potrzebne (i wtedy będziemy wiedzieć, jak POWINNY działać).
     */
    static void yagni() {
        section("5. YAGNI: nie buduj elastyczności, której nikt jeszcze nie potrzebuje");

        show("PRZED: describeProductBad(\"Laptop\", null, null, null) — trzy zbędne null-e", describeProductBad("Laptop", null, null, null));
        show("PO: describeProductGood(\"Laptop\")", describeProductGood("Laptop"));
        // WYNIK: PRZED: describeProductBad("Laptop", null, null, null) — trzy zbędne null-e → [Laptop]
        // WYNIK: PO: describeProductGood("Laptop") → [Laptop]

        // dlaczego: niewykorzystana "elastyczność" to nie inwestycja w przyszłość — to kod, który trzeba
        //   CZYTAĆ, TESTOWAĆ i UTRZYMYWAĆ już dziś, mimo że nie daje dziś żadnej wartości. Jeśli funkcja
        //   naprawdę będzie potrzebna, doda się ją TWTEDY — z pełną wiedzą, jak powinna działać.
    }

    // =================================================================================================
    // 6. COMMAND-QUERY SEPARATION (CQS)
    // =================================================================================================

    /** StockBad = magazyn, w którym JEDNA metoda pyta ("czy się uda?") i JEDNOCZEŚNIE zmienia stan. */
    static class StockBad {
        private int stock;

        StockBad(int stock) {
            this.stock = stock;
        }

        boolean takeStock(int qty) {              // zapytanie (query) i polecenie (command) naraz!
            if (qty > stock) {
                return false;
            }
            stock -= qty;
            return true;
        }

        int stock() {
            return stock;
        }
    }

    /** StockGood = magazyn z rozdzielonym zapytaniem (hasStock, bez efektu) i poleceniem (takeStock, bez zwrotu). */
    static class StockGood {
        private int stock;

        StockGood(int stock) {
            this.stock = stock;
        }

        boolean hasStock(int qty) {               // QUERY — tylko odpowiada, nic nie zmienia
            return qty <= stock;
        }

        void takeStock(int qty) {                 // COMMAND — tylko zmienia stan, nic nie zwraca
            if (!hasStock(qty)) {
                throw new IllegalStateException("Brak towaru: dostępne " + stock + ", żądane " + qty);
            }
            stock -= qty;
        }

        int stock() {
            return stock;
        }
    }

    /**
     * 6. W StockBad wywołanie takeStock(qty) WYGLĄDA jak pytanie ("czy jest towar?"), ale w rzeczywistości
     * zmienia stan magazynu — ktoś, kto woła je "tylko żeby sprawdzić" (np. do wyświetlenia w UI), przypadkowo
     * pomniejszy zapas. CQS: metoda albo ODPOWIADA na pytanie (query, bez efektu ubocznego), albo WYKONUJE
     * polecenie (command, bez zwracanej wartości) — nigdy oba naraz.
     */
    static void commandQuerySeparation() {
        section("6. Command-Query Separation: pytanie vs polecenie w jednej metodzie");

        StockBad bad = new StockBad(3);
        show("PRZED: bad.takeStock(2)  — wygląda jak pytanie...", bad.takeStock(2));
        show("PRZED: bad.stock() po 'sprawdzeniu'  — a jednak ZMIENIŁO stan!", bad.stock());
        // WYNIK: PRZED: bad.takeStock(2)  — wygląda jak pytanie... → true
        // WYNIK: PRZED: bad.stock() po 'sprawdzeniu'  — a jednak ZMIENIŁO stan! → 1

        line();

        StockGood good = new StockGood(3);
        show("PO: good.hasStock(2)  — QUERY, bez efektu ubocznego", good.hasStock(2));
        show("PO: good.hasStock(2) — można pytać wielokrotnie, stan bez zmian", good.hasStock(2));
        good.takeStock(2);   // COMMAND — jawnie zmienia stan, osobnym wywołaniem
        show("PO: good.stock() po takeStock(2)", good.stock());
        expectThrows("PO: good.takeStock(5) — za mało towaru", () -> good.takeStock(5));
        // WYNIK: PO: good.hasStock(2)  — QUERY, bez efektu ubocznego → true
        // WYNIK: PO: good.hasStock(2) — można pytać wielokrotnie, stan bez zmian → true
        // WYNIK: PO: good.stock() po takeStock(2) → 1
        // WYNIK: ✔ PO: good.takeStock(5) — za mało towaru → rzucono IllegalStateException: Brak towaru: dostępne 1, żądane 5

        // dlaczego: w StockGood hasStock można wołać DOWOLNIE wiele razy bez skutków — bezpieczne dla podglądu,
        //   logowania, warunków. takeStock jawnie ZMIENIA stan i nic nie "udaje" pytania. Czytelnik od razu wie,
        //   które wywołanie jest bezpieczne do powtórzenia, a które zmienia świat.
    }

    // =================================================================================================
    // 7. FAIL FAST
    // =================================================================================================

    static class OrderLineBad {
        final String product;
        final int quantity;

        OrderLineBad(String product, int quantity) {
            this.product = product;
            this.quantity = quantity;          // BRAK WALIDACJI — nawet ujemna ilość zostaje przyjęta bez słowa
        }

        int totalNaive(int unitPrice) {
            return unitPrice * quantity;
        }
    }

    static class OrderLineGood {
        final String product;
        final int quantity;

        OrderLineGood(String product, int quantity) {
            if (quantity <= 0) {
                throw new IllegalArgumentException(
                        "Ilość musi być dodatnia, było: " + quantity + " dla produktu \"" + product + "\"");
            }
            this.product = product;
            this.quantity = quantity;
        }

        int total(int unitPrice) {
            return unitPrice * quantity;
        }
    }

    /**
     * 7. OrderLineBad przyjmuje ujemną ilość bez protestu — błąd ujawnia się DOPIERO w totalNaive, jako
     * cicha, ujemna suma, daleko od PRAWDZIWEJ przyczyny (błędne dane wejściowe). OrderLineGood waliduje
     * w KONSTRUKTORZE — błąd wybucha NATYCHMIAST, z pełnym kontekstem (który produkt, jaka wartość).
     */
    static void failFast() {
        section("7. Fail fast: ujawniaj błędne dane NATYCHMIAST, nie gdzieś dalej");

        OrderLineBad bad = new OrderLineBad("Kubek", -3);
        show("PRZED: total dla quantity=-3  — błąd 'cichy', suma po prostu ujemna", bad.totalNaive(10));
        // WYNIK: PRZED: total dla quantity=-3  — błąd 'cichy', suma po prostu ujemna → -30

        expectThrows("PO: new OrderLineGood(\"Kubek\", -3)  — błąd NATYCHMIAST, z kontekstem",
                () -> new OrderLineGood("Kubek", -3));
        // WYNIK: ✔ PO: new OrderLineGood("Kubek", -3)  — błąd NATYCHMIAST, z kontekstem → rzucono IllegalArgumentException: Ilość musi być dodatnia, było: -3 dla produktu "Kubek"

        // dlaczego: bez walidacji w konstruktorze błędne dane "płyną" dalej przez system i psują coś ZUPEŁNIE
        //   INNEGO (np. sumę na fakturze) — a wtedy trzeba cofać się przez wiele kroków, żeby znaleźć PRAWDZIWE
        //   źródło. Walidacja u ŹRÓDŁA to najkrótsza droga między błędem a jego przyczyną.
    }

    // =================================================================================================
    // 8. PREFERUJ NIEZMIENNOŚĆ (IMMUTABILITY)
    // =================================================================================================

    static class MutablePoint {
        int x;
        int y;

        MutablePoint(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void moveBy(int dx, int dy) {
            x += dx;
            y += dy;
        }
    }

    /** ImmutablePoint = niezmienny punkt — moveBy zwraca NOWY obiekt, nie zmienia istniejącego. */
    record ImmutablePoint(int x, int y) {
        ImmutablePoint moveBy(int dx, int dy) {
            return new ImmutablePoint(x + dx, y + dy);
        }
    }

    /**
     * 8. p2 = p1 nie kopiuje obiektu — obie zmienne wskazują na TEN SAM obiekt w pamięci (aliasing). Przesunięcie
     * p2 "z boku" zmienia też p1, mimo że nikt jawnie go nie dotknął — klasyczne źródło trudnych do znalezienia
     * błędów. Niezmienny ImmutablePoint eliminuje CAŁĄ tę klasę błędów: moveBy zwraca NOWY obiekt, więc ŻADNA
     * istniejąca referencja nigdy nie zmienia się "pod nogami" (pełne omówienie: t06_oop_basics/Oop06Immutability).
     */
    static void immutability() {
        section("8. Preferuj niezmienność: obiekty, które nie zmieniają się 'pod nogami'");

        MutablePoint p1 = new MutablePoint(0, 0);
        MutablePoint p2 = p1;                      // p2 to TA SAMA referencja co p1, nie kopia!
        p2.moveBy(5, 5);
        show("PRZED: p1 po tym, jak KTOŚ INNY przesunął p2 (alias!)", "(" + p1.x + ", " + p1.y + ")");
        // WYNIK: PRZED: p1 po tym, jak KTOŚ INNY przesunął p2 (alias!) → (5, 5)

        line();

        ImmutablePoint q1 = new ImmutablePoint(0, 0);
        ImmutablePoint q2 = q1.moveBy(5, 5);       // NOWY obiekt — q1 pozostaje bez zmian
        show("PO: q1 (nietknięty)", q1);
        show("PO: q2 (nowy obiekt)", q2);
        // WYNIK: PO: q1 (nietknięty) → ImmutablePoint[x=0, y=0]
        // WYNIK: PO: q2 (nowy obiekt) → ImmutablePoint[x=5, y=5]

        // dlaczego: gdy obiekt jest niezmienny, "przekazanie go dalej" nigdy nie jest ryzykowne — nikt, kto go
        //   dostał, nie może zepsuć Twojej kopii. To fundament bezpiecznego współdzielenia danych (też między
        //   wątkami — t21_concurrency) i kluczy w mapach/zbiorach.
    }

    // =================================================================================================
    // 9. SAMO-DOKUMENTUJĄCY KOD VS KOMENTARZE TŁUMACZĄCE DLACZEGO
    // =================================================================================================

    static int nextDisplayIndexBad(int listIndex) {
        // dodaj 1 do listIndex                     ← komentarz tylko POWTARZA to, co i tak widać w kodzie
        int i = listIndex + 1;                        // i = indeks
        return i;                                       // zwróć i
    }

    static int nextDisplayIndex(int listIndex) {
        // UI numeruje elementy od 1, a listIndex jest liczony od 0 — stąd +1 (WHY, nie CO — to widać z kodu)
        return listIndex + 1;
    }

    /**
     * 9. Trzy komentarze w nextDisplayIndexBad opisują to, co kod i tak pokazuje wprost (szum, nie treść) —
     * gorzej, mogą z czasem "skłamać" (kod się zmieni, komentarz zostanie stary, jak w Pitfalls02 sekcja 5a).
     * Jedyny komentarz w nextDisplayIndex tłumaczy DLACZEGO (kontekst biznesowy niewidoczny w samym kodzie),
     * a nie CO (to widać z nazwy metody i argumentu).
     */
    static void selfDocumentingCode() {
        section("9. Kod tłumaczący się sam vs komentarze — CO (szum) kontra DLACZEGO (wartość)");

        show("PRZED: nextDisplayIndexBad(3)  — trzy komentarze-szum", nextDisplayIndexBad(3));
        show("PO: nextDisplayIndex(3)  — jeden komentarz, tłumaczy DLACZEGO", nextDisplayIndex(3));
        // WYNIK: PRZED: nextDisplayIndexBad(3)  — trzy komentarze-szum → 4
        // WYNIK: PO: nextDisplayIndex(3)  — jeden komentarz, tłumaczy DLACZEGO → 4

        // dlaczego: dobra nazwa (nextDisplayIndex) i typ parametru (listIndex) mówią CO robi metoda — komentarz
        //   opisujący to samo jest zbędny. Komentarz ma sens tylko wtedy, gdy dodaje coś, czego kod NIE POKAZUJE
        //   (tu: dlaczego akurat +1, a nie np. formatowanie na inny sposób).
    }

    // =================================================================================================
    // 10. SPÓJNE FORMATOWANIE (CONSISTENT FORMATTING)
    // =================================================================================================

    static int addBad(int a,int b)   {return a+b;}

    static int addGood(int a, int b) {
        return a + b;
    }

    /**
     * 10. addBad i addGood liczą DOKŁADNIE to samo — formatowanie nie wpływa na działanie programu. Wpływa
     * za to na czytelność, szybkość code review i "szum" w historii Git: w dużym pliku niespójne odstępy
     * i brak nowych linii sprawiają, że każda zmiana logiki miesza się wizualnie z przypadkowym stylem.
     */
    static void consistentFormatting() {
        section("10. Spójne formatowanie: ten sam wynik, różna czytelność");

        show("PRZED: addBad(3, 4)  — zbite razem, bez spójnych odstępów", addBad(3, 4));
        show("PO: addGood(3, 4)  — spacje, wcięcia, każda instrukcja w swojej linii", addGood(3, 4));
        // WYNIK: PRZED: addBad(3, 4)  — zbite razem, bez spójnych odstępów → 7
        // WYNIK: PO: addGood(3, 4)  — spacje, wcięcia, każda instrukcja w swojej linii → 7

        note("W realnym projekcie formatowanie ustala AUTOMAT (np. formatter w IDE, Checkstyle, Spotless) —");
        note("nie warto spierać się o spacje kontra taby, gdy narzędzie może to wymuszać jednakowo dla wszystkich.");
        // WYNIK:    ℹ W realnym projekcie formatowanie ustala AUTOMAT (np. formatter w IDE, Checkstyle, Spotless) —
        // WYNIK:    ℹ nie warto spierać się o spacje kontra taby, gdy narzędzie może to wymuszać jednakowo dla wszystkich.

        // dlaczego: niespójne formatowanie generuje FAŁSZYWY szum w diffach (git diff pokazuje zmianę w KAŻDEJ
        //   przeformatowanej linii, nie tylko tam, gdzie faktycznie zmieniła się logika) i utrudnia code review.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • nazewnictwo: nazwa mówi CO i PO CO; boolean → isX/hasX, żeby wywołanie czytało się jak pytanie.
     *   • małe funkcje: jedna rzecz, jeden poziom abstrakcji; orkiestrator woła nazwane kroki, nie miesza
     *     szczegółów niskiego poziomu z regułami biznesowymi.
     *   • DRY: ta sama logika w dwóch miejscach → wydziel wspólną metodę (jedno miejsce prawdy).
     *   • KISS: proste rozwiązanie zamiast "sprytnego", jeśli dają TEN SAM wynik.
     *   • YAGNI: nie buduj elastyczności "na przyszłość", której nikt dziś nie potrzebuje.
     *   • CQS: metoda albo ODPOWIADA (query, bez efektu ubocznego), albo WYKONUJE (command, bez zwrotu) — nie oba.
     *   • fail fast: waliduj w konstruktorze/na wejściu — błąd wybucha NATYCHMIAST, blisko przyczyny.
     *   • niezmienność: obiekty, które nie zmieniają się "pod nogami" (bez aliasingu) — bezpieczne współdzielenie.
     *   • komentarz ma tłumaczyć DLACZEGO, nie CO (to widać z dobrej nazwy) — CO-komentarze to szum i mogą "skłamać".
     *   • spójne formatowanie: ten sam wynik, mniej szumu w diffach i code review; oddaj to narzędziu (formatter).
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego dobra nazwa metody/zmiennej bywa ważniejsza niż komentarz opisujący, co ona robi?
     *   2. Co wypisze:  System.out.println(addBad(2, 3));  ? (sekcja 10 — sprawdza, czy formatowanie wpływa na wynik)
     *   3. ZNAJDŹ BŁĄD (kod kompiluje się i "działa", ale łamie DWIE zasady z tej lekcji):
     *          static String chk(Integer n) {
     *              if (n != null) {
     *                  if (n >= 0) { if (n == 0) { return "zero"; } else { return "dodatnia"; } }
     *                  else { return "ujemna"; }
     *              } else { return "brak"; }
     *          }
     *   4. Co wypisze:  MutablePoint p1 = new MutablePoint(1, 1); MutablePoint p2 = p1; p2.moveBy(2, 2);
     *      System.out.println(p1.x + "," + p1.y);  ?
     *   5. Czym różni się QUERY od COMMAND w zasadzie CQS i dlaczego mieszanie ich w jednej metodzie bywa mylące?
     *   6. Dlaczego YAGNI ostrzega przed dodawaniem "elastyczności na przyszłość", której jeszcze nikt nie potrzebuje?
     *   7. Czym różni się komentarz "zbędny/kłamiący" od komentarza uzasadnionego w tej lekcji?
     *   8. Dlaczego fail fast (walidacja w konstruktorze) ułatwia debugowanie w porównaniu do pozwolenia,
     *      by błędne dane "płynęły" dalej przez system?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** Vector = niezmienny wektor 2D — plus zwraca NOWY obiekt (do ćwiczenia o niezmienności). */
    record Vector(int x, int y) {
        Vector plus(Vector other) {
            return new Vector(x + other.x(), y + other.y());
        }
    }

    /** Percentage = wartość procentowa 0-100, walidowana w konstruktorze (fail fast + niezmienność razem). */
    record Percentage(int value) {
        Percentage {
            if (value < 0 || value > 100) {
                throw new IllegalArgumentException("Procent musi być w zakresie 0-100, było: " + value);
            }
        }

        static Percentage of(int value) {
            return new Percentage(value);
        }

        Percentage complement() {
            return new Percentage(100 - value);
        }
    }

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");

        Check.equal("ćw. 1a: describeDueStatus(null)", "brak terminu", () -> exercise1(null));
        Check.equal("ćw. 1b: describeDueStatus(-1)", "przeterminowane", () -> exercise1(-1));
        Check.equal("ćw. 1c: describeDueStatus(0)", "termin dziś", () -> exercise1(0));
        Check.equal("ćw. 1d: describeDueStatus(2)", "termin blisko", () -> exercise1(2));
        Check.equal("ćw. 1e: describeDueStatus(10)", "termin odległy", () -> exercise1(10));
        Check.equal("ćw. 2a: formatLabel(\"Ala \")", "[Ala]", () -> exercise2("Ala "));
        Check.equal("ćw. 2b: formatLabel(null)", "[]", () -> exercise2(null));
        Check.equal("ćw. 3a: safeDivide(10, 2)", 5, () -> exercise3(10, 2));
        Check.throwsException("ćw. 3b: safeDivide(5, 0) rzuca wyjątek", IllegalArgumentException.class, () -> exercise3(5, 0));
        Check.equal("ćw. 4: Vector(1,2).plus(Vector(3,4))", new Vector(4, 6), () -> exercise4(new Vector(1, 2), new Vector(3, 4)));
        Check.equal("ćw. 5a: Percentage.of(30).complement()", 70, () -> exercise5(30));
        Check.equal("ćw. 5b: Percentage.of(0).complement()", 100, () -> exercise5(0));
        Check.throwsException("ćw. 5c: Percentage.of(150) rzuca wyjątek", IllegalArgumentException.class, () -> exercise5(150));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec, null)", "brak terminu", () -> solution1(null));
        Check.equal("ćw. 1 (wzorzec, 0)", "termin dziś", () -> solution1(0));
        Check.equal("ćw. 2 (wzorzec)", "[Ala]", () -> solution2("Ala "));
        Check.equal("ćw. 3 (wzorzec)", 5, () -> solution3(10, 2));
        Check.equal("ćw. 4 (wzorzec)", new Vector(4, 6), () -> solution4(new Vector(1, 2), new Vector(3, 4)));
        Check.equal("ćw. 5 (wzorzec)", 70, () -> solution5(30));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe, „PRZEPISZ”): stara wersja miała głębokie zagnieżdżenie i nic nieznaczące nazwy:
     * <pre>{@code
     * static String dueOld(Integer d) {
     *     String r;
     *     if (d != null) {
     *         if (d < 0) { r = "przeterminowane"; }
     *         else { if (d == 0) { r = "termin dziś"; } else { if (d <= 3) { r = "termin blisko"; } else { r = "termin odległy"; } } }
     *     } else { r = "brak terminu"; }
     *     return r;
     * }
     * }</pre>
     * Przepisz jako describeDueStatus(Integer daysLeft), używając klauzul strażniczych i dobrych nazw (zasady 1 i 2).
     */
    static String exercise1(Integer daysLeft) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (łatwe, YAGNI/KISS): formatLabel(text) ma zwrócić "[" + text.trim() + "]", a dla null zwrócić
     * "[]" — bez żadnych dodatkowych, "przewidujących przyszłość" parametrów.
     */
    static String exercise2(String text) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie, fail fast): safeDivide(a, b) ma zwrócić a / b, ale dla b == 0 NATYCHMIAST rzucić
     * IllegalArgumentException z komunikatem "Dzielenie przez zero" (zamiast pozwolić ArithmeticException
     * wybuchnąć gdzieś głębiej, bez kontekstu).
     */
    static int exercise3(int a, int b) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (średnie, niezmienność): zwróć NOWY wektor będący sumą a i b, korzystając z Vector.plus
     * (żaden z argumentów nie może się zmienić).
     */
    static Vector exercise4(Vector a, Vector b) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 5 (trudniejsze, fail fast + niezmienność): zwróć wartość dopełnienia procentu do 100
     * (Percentage.of(value).complement().value()) — dla value spoza 0-100 konstruktor Percentage MUSI rzucić
     * IllegalArgumentException (nic nie musisz tu dodatkowo sprawdzać — to zadanie konstruktora rekordu).
     */
    static int exercise5(int value) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(Integer daysLeft) {
        if (daysLeft == null) {
            return "brak terminu";
        }
        if (daysLeft < 0) {
            return "przeterminowane";
        }
        if (daysLeft == 0) {
            return "termin dziś";
        }
        if (daysLeft <= 3) {
            return "termin blisko";
        }
        return "termin odległy";
    }

    static String solution2(String text) {
        if (text == null) {
            return "[]";
        }
        return "[" + text.trim() + "]";
    }

    static int solution3(int a, int b) {
        if (b == 0) {
            throw new IllegalArgumentException("Dzielenie przez zero");
        }
        return a / b;
    }

    static Vector solution4(Vector a, Vector b) {
        return a.plus(b);
    }

    static int solution5(int value) {
        return Percentage.of(value).complement().value();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo komentarz może "skłamać" po kolejnej zmianie kodu (nikt nie pilnuje jego zgodności) — dobra nazwa
     *      jest CZĘŚCIĄ kodu, więc kompiluje się i działa razem z nim; nie może "wyjść z synchronizacji".
     *   2. Wypisze 7 — formatowanie (brak spacji, wszystko w jednej linii) nie wpływa na wynik działania kodu,
     *      tylko na czytelność.
     *   3. Niejasne nazwy (chk, n) — naruszenie zasady 1 (nazewnictwo) — ORAZ głębokie zagnieżdżenie if/else
     *      zamiast klauzul strażniczych — naruszenie zasady 2 (małe funkcje / prosta struktura).
     *   4. Wypisze 3,3 — p2 to TA SAMA referencja co p1 (alias), więc p2.moveBy(2, 2) zmienia obiekt, na który
     *      wskazują OBIE zmienne.
     *   5. Query odpowiada na pytanie i NIE ma efektu ubocznego (bezpieczne do wielokrotnego wywołania); command
     *      wykonuje działanie i zmienia stan, ale nic nie zwraca. Mieszanie ich sprawia, że "tylko sprawdzenie"
     *      (query) niepostrzeżenie zmienia stan systemu.
     *   6. Bo niewykorzystana elastyczność to kod, który trzeba dziś czytać, testować i utrzymywać, mimo że nie
     *      daje dziś żadnej wartości — a prawdopodobieństwo, że zgadniemy przyszłe wymagania, jest niskie.
     *   7. Komentarz zbędny/kłamiący opisuje CO robi kod (to widać z dobrej nazwy, a po zmianie kodu może przestać
     *      być prawdziwy); komentarz uzasadniony tłumaczy DLACZEGO — kontekst niewidoczny w samym kodzie.
     *   8. Bo błąd wybucha BLISKO swojej prawdziwej przyczyny (np. w konstruktorze, z pełnym kontekstem: jaki
     *      produkt, jaka wartość), zamiast "płynąć" dalej i psuć coś innego, gdzie ślad do przyczyny jest długi.
     */
    // </editor-fold>
}
