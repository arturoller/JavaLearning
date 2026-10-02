package t00_start;

import helpers.Check;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntBinaryOperator;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Refaktoryzacje w IntelliJ IDEA — zmiany kodu wykonane przez narzędzie, nie ręcznie
 *        (refactoring = refaktoryzacja, czyli zmiana STRUKTURY kodu bez zmiany jego ZACHOWANIA;
 *         shortcut = skrót klawiszowy)
 *
 * W SKRÓCIE:
 *   Refaktoryzacja to porządkowanie kodu tak, żeby program robił dokładnie to samo, ale był czytelniejszy.
 *   IntelliJ zna dziesiątki gotowych, bezpiecznych refaktoryzacji (zmiana nazwy, wydzielenie metody...) i
 *   poprawia WSZYSTKIE miejsca użycia naraz. Szybciej i bezpieczniej niż ręczne „znajdź i zamień”.
 *   W tej lekcji każdy wzorzec ma wersję PRZED i PO — uruchamiamy obie i dowodzimy, że wynik jest ten sam.
 *   Skróty podajemy dla domyślnej mapy klawiszy „Windows” (Settings → Keymap).
 *
 * ANALOGIA: remont mieszkania bez wyprowadzki.
 *   Przestawiasz meble i podpisujesz szuflady, ale lodówka dalej chłodzi, a prąd działa. Refaktoryzacja to
 *   zmiana wyglądu środka bez zmiany tego, co dom robi dla mieszkańców. Narzędzie jest jak ekipa, która
 *   przy przenoszeniu szafy sama przepina kable i poprawia wszystkie opisy na planie mieszkania.
 *
 * JAK TO DZIAŁA:
 *   Ustaw kursor na nazwie / zaznacz fragment kodu, naciśnij skrót (albo Refactor This: Ctrl+Alt+Shift+T),
 *   uzupełnij okienko (nowa nazwa, nazwa metody...) i zatwierdź. IntelliJ analizuje kod (zna typy i
 *   powiązania, nie tylko tekst), pokazuje podgląd konfliktów i zmienia wszystkie miejsca. Ctrl+Z cofa całą
 *   refaktoryzację naraz.
 *
 *   skrót             refaktoryzacja             co robi
 *   Shift+F6          Rename                     zmienia nazwę wszędzie, gdzie jest użyta
 *   Ctrl+Alt+V        Extract Variable           wyrażenie → zmienna lokalna
 *   Ctrl+Alt+M        Extract Method             fragment kodu → osobna metoda
 *   Ctrl+Alt+C        Extract Constant           „magiczna liczba” → stała static final
 *   Ctrl+Alt+P        Introduce Parameter        wartość z ciała metody → parametr metody
 *   Ctrl+Alt+N        Inline                     odwrotność: wstaw zmienną / metodę w miejsca użycia
 *   Ctrl+F6           Change Signature           parametry: dodaj, usuń, zmień kolejność i nazwy
 *   F6                Move                       przenieś klasę / metodę do innej klasy lub pakietu
 *   (menu Refactor)   Extract Interface          z klasy wydziel interfejs
 *   Alt+Enter         intencje (intentions)      „Convert to record class”, „Replace with collect” (pętla → stream)
 *
 * SŁÓWKA: rename = zmień nazwę; extract = wydziel; variable = zmienna; method = metoda; constant = stała;
 *   introduce = wprowadź; parameter = parametr; inline = wstaw w miejsce użycia; change signature = zmień
 *   sygnaturę (nazwa + parametry + typ wyniku); move = przenieś; interface = interfejs; intention = podpowiedź
 *   (żarówka); magic number = magiczna liczba; usage = miejsce użycia; side effect = efekt uboczny
 *
 * ZOBACZ TEŻ: t00_start/Start06Git (commit przed refaktoryzacją), t00_start/Start05Debugging (praca w IntelliJ),
 *   t27_clean_code_pitfalls/CleanCode01Principles (po co czytelny kod), t13_lambdas/Lambda01FromAnonymousToLambda (lambdy
 *   z ostatniej sekcji), t06_oop_basics/Oop10Copying (rekordy i kopie)
 * </pre>
 */
public class Start07IntelliJRefactoring {

    public static void main(String[] args) {
        title("Start07 — refaktoryzacje w IntelliJ");

        rename();            // rename = zmiana nazwy
        extractVariable();   // extract variable = wydziel zmienną
        extractMethod();     // extract method = wydziel metodę
        extractConstant();   // extract constant = wydziel stałą
        introduceParameter();// introduce parameter = wprowadź parametr
        inline();            // inline = wstaw w miejsce użycia
        changeSignature();   // change signature = zmień sygnaturę
        moveAndInterface();  // move = przenieś; interface = interfejs
        recordAndStream();   // record = rekord; stream = strumień
        shortcuts();         // shortcuts = skróty klawiszowe
        safeHabits();        // safe habits = bezpieczne nawyki
        exercises();         // exercises = ćwiczenia
    }

    /** Wypisuje wynik wersji PRZED, wynik wersji PO i werdykt „identyczne”. */
    static void proveSame(String label, Object before, Object after) {   // prove = udowodnij
        String verdict = before.equals(after) ? "identyczne" : "RÓŻNE!";  // verdict = werdykt
        show(label, before + " | " + after + " → " + verdict);
    }

    // =================================================================================================
    // 1. RENAME (Shift+F6)
    // =================================================================================================

    // PRZED: nazwy nic nie mówią
    static int calcBefore(int a, int b) {
        return a * b;
    }

    // PO: Shift+F6 na nazwie metody i na parametrach
    static int rectangleAreaAfter(int width, int height) {      // rectangle area = pole prostokąta
        return width * height;
    }

    /**
     * 1. Rename (Shift+F6) zmienia nazwę zmiennej, metody, klasy, pakietu lub pliku i poprawia WSZYSTKIE jej
     * użycia. Najczęstsza i najważniejsza refaktoryzacja — dobre nazwy to połowa czytelnego kodu.
     */
    static void rename() {
        section("1. Rename — Shift+F6");

        proveSame("calc(6, 7) i rectangleArea(6, 7)", calcBefore(6, 7), rectangleAreaAfter(6, 7));
        // WYNIK: calc(6, 7) i rectangleArea(6, 7) → 42 | 42 → identyczne

        // JAK: ustaw kursor na nazwie (w deklaracji ALBO w dowolnym użyciu) → Shift+F6 → wpisz nową nazwę → Enter.
        // Zwykle działa „w miejscu” (nazwa edytowana bezpośrednio w kodzie, cichy podgląd zmian). W oknie dialogowym
        // zobaczysz opcje: „Search in comments and strings” (szukaj też w komentarzach i napisach) i
        // „Search for text occurrences” (szukaj tekstu w innych plikach).
        // Rename na klasie zmienia też nazwę PLIKU (i odwrotnie: Shift+F6 na pliku w oknie Project zmienia klasę),
        // na pakiecie — nazwę katalogu, a na polu — IntelliJ zapyta, czy zmienić także gettery i settery.
        //
        // PUŁAPKA: IntelliJ poprawia to, co WIDZI w kodzie Javy. Nie wie o nazwach użytych jako tekst: w refleksji
        // (Class.forName("pakiet.Klasa")), w plikach .properties, .xml, w zapytaniach SQL, w adresach URL.
        // Po zmianie nazwy takiej klasy program się skompiluje, a wywali dopiero w działaniu. Włącz
        // „Search for text occurrences” i przeszukaj projekt (Ctrl+Shift+F) po starej nazwie.
        //
        // DOBRA PRAKTYKA: nie bój się zmieniać nazw. Gdy po tygodniu pracy widzisz, że zmienna „list” to właściwie
        // „studentsInCity” — zmień od razu (Shift+F6 kosztuje sekundę). Nazwy metod = czasowniki (calculateTotal),
        // zmiennych = rzeczowniki (total), logicznych = pytania (isActive, hasDiscount).
    }

    // =================================================================================================
    // 2. EXTRACT VARIABLE (Ctrl+Alt+V)
    // =================================================================================================

    // PRZED: jedno wielkie wyrażenie (kwoty w groszach; liczymy na liczbach całkowitych — kolejność działań ma znaczenie)
    static long totalBefore(long unitPriceCents, int quantity, int discountPercent) {
        return unitPriceCents * quantity * (100 - discountPercent) / 100 * 123 / 100;
    }

    // PO: Ctrl+Alt+V na kolejnych fragmentach (zaznacz fragment → skrót → nazwa)
    static long totalAfter(long unitPriceCents, int quantity, int discountPercent) {
        long listTotal = unitPriceCents * quantity;                          // list total = suma wg cennika
        long afterDiscount = listTotal * (100 - discountPercent) / 100;      // after discount = po rabacie
        return afterDiscount * 123 / 100;                                    // × 1,23: cena brutto (z VAT 23%)
    }

    /**
     * 2. Extract Variable (Ctrl+Alt+V) wydziela zaznaczone wyrażenie do zmiennej lokalnej i nadaje mu nazwę.
     * Długie wyrażenie zamienia się w kroki, które da się przeczytać i sprawdzić w debuggerze.
     */
    static void extractVariable() {
        section("2. Extract Variable — Ctrl+Alt+V");

        proveSame("cena brutto 25,00 zł × 3, rabat 10%", totalBefore(2500, 3, 10), totalAfter(2500, 3, 10));
        // WYNIK: cena brutto 25,00 zł × 3, rabat 10% → 8302 | 8302 → identyczne

        // JAK: zaznacz wyrażenie (albo ustaw kursor w nim i naciskaj Ctrl+W, żeby rozszerzać zaznaczenie
        // o kolejne poziomy) → Ctrl+Alt+V → jeśli wyrażenie występuje kilka razy, IntelliJ zapyta, czy zastąpić
        // wszystkie wystąpienia → wybierz nazwę (podpowiada sam) → Enter.
        // Bez zaznaczenia IntelliJ pokaże listę wyrażeń do wyboru (od najmniejszego do największego).
        // Wariant: Ctrl+Alt+F (Extract Field) robi z wyrażenia POLE klasy zamiast zmiennej lokalnej.
        //
        // DOBRA PRAKTYKA: kroki pośrednie z nazwami zastępują komentarze („tu liczymy rabat”). Debugger
        // (Start05Debugging) pokaże wartość każdej zmiennej osobno — przy jednym długim wyrażeniu nie zobaczysz kroków.
        //
        // PUŁAPKA: arytmetyka na liczbach CAŁKOWITYCH zaokrągla w dół przy każdym dzieleniu, więc ZMIANA KOLEJNOŚCI
        // działań zmienia wynik (a nie tylko ich grupowanie w zmiennych). IntelliJ zachowuje kolejność, ale jeśli
        // robisz to ręcznie, sprawdź wynik (sekcja 11).
    }

    // =================================================================================================
    // 3. EXTRACT METHOD (Ctrl+Alt+M)
    // =================================================================================================

    // PRZED: ten sam kawałek (formatowanie kwoty) skopiowany dwa razy
    static String lineBefore(String name, int quantity, int unitPriceCents) {
        int total = quantity * unitPriceCents;
        String unit = (unitPriceCents / 100) + "," + (unitPriceCents % 100 < 10 ? "0" : "") + (unitPriceCents % 100) + " zł";
        String sum = (total / 100) + "," + (total % 100 < 10 ? "0" : "") + (total % 100) + " zł";
        return name + ": " + quantity + " × " + unit + " = " + sum;
    }

    // PO: Ctrl+Alt+M na formatowaniu kwoty; IntelliJ sam zaproponował zastąpienie drugiego, takiego samego fragmentu
    static String lineAfter(String name, int quantity, int unitPriceCents) {
        int total = quantity * unitPriceCents;
        return name + ": " + quantity + " × " + formatMoney(unitPriceCents) + " = " + formatMoney(total);
    }

    static String formatMoney(int cents) {              // format money = sformatuj kwotę
        return (cents / 100) + "," + (cents % 100 < 10 ? "0" : "") + (cents % 100) + " zł";
    }

    /**
     * 3. Extract Method (Ctrl+Alt+M) wycina zaznaczony fragment do nowej metody, sam wykrywa potrzebne parametry
     * i wynik, a w miejscu fragmentu wstawia wywołanie. Likwiduje kopiowanie kodu i skraca długie metody.
     */
    static void extractMethod() {
        section("3. Extract Method — Ctrl+Alt+M");

        proveSame("pozycja zamówienia", lineBefore("Kawa", 3, 2599), lineAfter("Kawa", 3, 2599));
        // WYNIK: pozycja zamówienia → Kawa: 3 × 25,99 zł = 77,97 zł | Kawa: 3 × 25,99 zł = 77,97 zł → identyczne
        show("wersja PO", lineAfter("Herbata", 2, 1205));
        // WYNIK: wersja PO → Herbata: 2 × 12,05 zł = 24,10 zł

        // JAK: zaznacz kilka linii (całe instrukcje) lub wyrażenie → Ctrl+Alt+M → wpisz nazwę metody →
        // sprawdź parametry w okienku (IntelliJ policzył je z użytych zmiennych) → Enter.
        // Jeśli ten sam fragment występuje gdzie indziej, IntelliJ pyta „Replace duplicates”: zgódź się.
        // Czego fragment używa z zewnątrz — staje się parametrem; to, co oblicza dla reszty kodu — wynikiem
        // (jeśli trzeba zwrócić kilka wartości, IntelliJ ostrzeże — to znak, że fragment robi za dużo).
        //
        // PUŁAPKA: nazwa „doStuff” (zrób coś) albo „process” (przetwórz) nic nie mówi. Metoda ma nazwę z czasownikiem
        // i opisującą DOKŁADNIE to, co robi. Jeśli nie umiesz jej nazwać, wydzieliłeś zły kawałek.
        //
        // DOBRA PRAKTYKA: wydzielaj, gdy (1) fragment powtarza się, (2) potrzebujesz komentarza, żeby wyjaśnić, co się
        // dzieje w bloku — nazwa metody zastąpi komentarz, (3) metoda nie mieści się na ekranie. Dobra metoda
        // robi JEDNĄ rzecz na jednym poziomie szczegółowości.
    }

    // =================================================================================================
    // 4. EXTRACT CONSTANT (Ctrl+Alt+C)
    // =================================================================================================

    // PRZED: „magiczna liczba” 123 (a co ona znaczy?)
    static long grossBefore(long netCents) {
        return netCents * 123 / 100;
    }

    // PO: Ctrl+Alt+C na liczbie 123 → stała z nazwą (static final = jedna wspólna wartość, której nie można zmienić)
    private static final int GROSS_PERCENT = 123;       // gross percent = brutto w procentach netto (100% + 23% VAT)

    static long grossAfter(long netCents) {
        return netCents * GROSS_PERCENT / 100;
    }

    /**
     * 4. Extract Constant (Ctrl+Alt+C) zamienia „magiczną liczbę” lub napis w stałą {@code static final} z nazwą.
     * Wartość jest zapisana w jednym miejscu, a nazwa wyjaśnia jej znaczenie.
     */
    static void extractConstant() {
        section("4. Extract Constant — Ctrl+Alt+C");

        proveSame("brutto z netto 100,00 zł (w groszach)", grossBefore(10_000), grossAfter(10_000));
        // WYNIK: brutto z netto 100,00 zł (w groszach) → 12300 | 12300 → identyczne

        // JAK: kursor na liczbie lub napisie → Ctrl+Alt+C → nazwa stałej (NAZWA_WIELKIMI_LITERAMI) → Enter.
        // IntelliJ umieszcza ją na górze klasy i zastępuje użycia.
        //
        // PUŁAPKA: ta sama LICZBA nie zawsze oznacza to samo. W tej lekcji „100” jest raz mianownikiem procentów,
        // a mogłoby być też „maksymalną liczbą punktów”. Gdy IntelliJ pyta „Replace all N occurrences?”, przejrzyj
        // wystąpienia — zamiana wszystkich „na ślepo” połączyłaby dwie różne rzeczy w jedną stałą: zmiana jednej
        // zmieni też drugą. Stałe tworzymy dla ZNACZENIA, nie dla wartości.
        //
        // DOBRA PRAKTYKA: stała dla wszystkiego, co jest regułą biznesową lub ustawieniem (stawka VAT, limit,
        // nazwa pliku). Zostaw bez nazwy liczby oczywiste (0, 1, -1, 2 przy parzystości, 100 przy procentach —
        // o ile jest to jedyne znaczenie).
    }

    // =================================================================================================
    // 5. INTRODUCE PARAMETER (Ctrl+Alt+P)
    // =================================================================================================

    // PRZED: powitanie „zaszyte” w metodzie
    static String greetBefore(String name) {
        return "Cześć, " + name + "!";
    }

    // PO: Ctrl+Alt+P na napisie "Cześć" → parametr greeting; wywołujący dostaje tę wartość jako argument
    static String greetAfter(String name, String greeting) {   // greeting = powitanie
        return greeting + ", " + name + "!";
    }

    /**
     * 5. Introduce Parameter (Ctrl+Alt+P) zamienia wartość „zaszytą” w ciele metody w parametr. IntelliJ dopisuje
     * argument w KAŻDYM wywołaniu, dając tej samej wartości — działanie programu się nie zmienia, ale
     * metoda staje się elastyczna i łatwiejsza do przetestowania.
     */
    static void introduceParameter() {
        section("5. Introduce Parameter — Ctrl+Alt+P");

        proveSame("powitanie Ali", greetBefore("Ala"), greetAfter("Ala", "Cześć"));
        // WYNIK: powitanie Ali → Cześć, Ala! | Cześć, Ala! → identyczne
        show("i teraz po angielsku, bez zmiany metody", greetAfter("Ala", "Hello"));
        // WYNIK: i teraz po angielsku, bez zmiany metody → Hello, Ala!

        // JAK: zaznacz wyrażenie wewnątrz metody → Ctrl+Alt+P → nazwa parametru → Enter.
        // Podobne: Ctrl+Alt+F (do pola klasy) i Ctrl+Alt+V (do zmiennej) — zaznaczasz to samo, wybierasz „dokąd”.
        //
        // DOBRA PRAKTYKA: metoda, która wszystko bierze z parametrów i nic „z powietrza” (stałych ukrytych w środku,
        // zegara systemowego, plików), jest łatwa do sprawdzenia w teście (t32_junit_mockito): podajesz wejście,
        // sprawdzasz wyjście.
    }

    // =================================================================================================
    // 6. INLINE (Ctrl+Alt+N)
    // =================================================================================================

    // PRZED: zbędna zmienna pośrednia
    static int areaBefore(int width, int height) {
        int area = width * height;
        return area;
    }

    // PO: Ctrl+Alt+N na zmiennej „area” wstawia jej wyrażenie w miejsce użycia
    static int areaAfter(int width, int height) {
        return width * height;
    }

    static final int[] COUNTER = {0};                   // licznik „zewnętrzny” (tablica 1-elementowa, żeby zmieniać wartość)

    static int next() {                                 // next = następny: zwraca kolejną liczbę (ma EFEKT UBOCZNY)
        return ++COUNTER[0];
    }

    // PRZED inline: wywołanie wykonane RAZ, wynik użyty dwa razy
    static int doubledIdBefore() {
        int id = next();
        return id + id;
    }

    // „PO” inline — ZŁE: wywołanie wykonuje się DWA razy, więc zachowanie się zmieniło!
    static int doubledIdInlined() {
        return next() + next();
    }

    /**
     * 6. Inline (Ctrl+Alt+N) to odwrotność wydzielania: wstawia zmienną, metodę lub stałą w miejsca użycia i usuwa
     * deklarację. Używamy go, gdy pośrednik nic nie wyjaśnia. Uwaga na wyrażenia z efektem ubocznym!
     */
    static void inline() {
        section("6. Inline — Ctrl+Alt+N");

        proveSame("pole 4 × 5", areaBefore(4, 5), areaAfter(4, 5));
        // WYNIK: pole 4 × 5 → 20 | 20 → identyczne

        COUNTER[0] = 0;
        int before = doubledIdBefore();
        COUNTER[0] = 0;
        int inlined = doubledIdInlined();
        proveSame("podwojony identyfikator z next()", before, inlined);
        // WYNIK: podwojony identyfikator z next() → 2 | 3 → RÓŻNE!

        // PUŁAPKA: inline zmiennej, której wartość pochodzi z wywołania z EFEKTEM UBOCZNYM (next(), odczyt z pliku,
        // losowanie, zapis do bazy), zmienia program, gdy zmienna jest użyta więcej niż raz — wywołanie wykona
        // się wiele razy (tu: 1 + 1 = 2, a po inline 1 + 2 = 3). Nie licz na to, że IDE zawsze Cię ostrzeże:
        // przy wywołaniach metod sprawdź to sam.
        //
        // JAK: kursor na zmiennej / metodzie / stałej → Ctrl+Alt+N. Przy metodzie z wieloma wywołaniami IntelliJ
        // pyta: „Inline all” (wszystkie i usuń metodę) albo „Inline this only” (tylko to wywołanie).
        //
        // DOBRA PRAKTYKA: zostaw zmienną pośrednią, gdy NAZWA coś wyjaśnia (boolean isAdult = age >= 18; if (isAdult)).
        // Wstaw (inline), gdy zmienna tylko powtarza to, co widać w następnej linii.
    }

    // =================================================================================================
    // 7. CHANGE SIGNATURE (Ctrl+F6)
    // =================================================================================================

    // PRZED
    static String labelBefore(String name, int number) {
        return name + " #" + number;
    }

    // PO: Ctrl+F6 → zmieniona kolejność parametrów, dodany parametr „prefix” z wartością domyślną dla wywołań ("#")
    static String labelAfter(int number, String name, String prefix) {   // label = etykieta; prefix = przedrostek
        return name + " " + prefix + number;
    }

    /**
     * 7. Change Signature (Ctrl+F6) zmienia sygnaturę metody (nazwa, parametry, typ wyniku, widoczność) i naprawia
     * WSZYSTKIE wywołania. Nowy parametr dostaje „wartość domyślną” wstawianą w wywołaniach — dzięki temu program
     * dalej działa tak samo.
     */
    static void changeSignature() {
        section("7. Change Signature — Ctrl+F6");

        proveSame("etykieta 'Ala' numer 7", labelBefore("Ala", 7), labelAfter(7, "Ala", "#"));
        // WYNIK: etykieta 'Ala' numer 7 → Ala #7 | Ala #7 → identyczne

        // JAK: kursor na nazwie metody → Ctrl+F6 → w oknie: przyciski + / − (dodaj/usuń parametr), strzałki
        // (kolejność), edycja nazw i typów; przy nowym parametrze wpisz „Default value” (wartość do wstawienia
        // w istniejących wywołaniach) → Refactor. Przycisk Preview pokaże zmiany przed wykonaniem.
        //
        // PUŁAPKA: jeśli metoda jest częścią cudzego interfejsu (biblioteka, której używają inne projekty), nie
        // zmienisz wywołań poza swoim kodem. Wtedy dodaj nową metodę (przeciążenie) i oznacz starą jako przestarzałą
        // (adnotacja @Deprecated), zamiast zmieniać sygnaturę.
        //
        // DOBRA PRAKTYKA: metoda z więcej niż 3–4 parametrami to sygnał, że potrzebny jest obiekt (rekord)
        // grupujący dane — Change Signature i Extract Parameter Object (Refactor This) pomogą to zrobić.
    }

    // =================================================================================================
    // 8. MOVE (F6) i EXTRACT INTERFACE
    // =================================================================================================

    // PRZED: obliczenie VAT mieszka w klasie zamówienia, choć dotyczy podatku
    static class OrderBefore {                          // order = zamówienie
        static long vatOf(long netCents) {              // vat of = VAT od
            return netCents * 23 / 100;
        }

        static long grossOf(long netCents) {            // gross of = brutto z
            return netCents + vatOf(netCents);
        }
    }

    // PO: F6 na metodzie vatOf → przeniesiona do klasy Tax, wywołania zmieniły się na Tax.vatOf(...)
    static class Tax {                                  // tax = podatek
        static long vatOf(long netCents) {
            return netCents * 23 / 100;
        }
    }

    static class OrderAfter {
        static long grossOf(long netCents) {
            return netCents + Tax.vatOf(netCents);
        }
    }

    // PRZED: kod zależy od konkretnej klasy
    static class EmailSender {                          // sender = nadawca
        String send(String text) {                      // send = wyślij
            return "e-mail: " + text;
        }
    }

    static String notifyBefore(EmailSender sender, String text) {   // notify = powiadom
        return sender.send(text);
    }

    // PO: Extract Interface wydzielił z klasy interfejs MessageSender; metoda zależy już od interfejsu
    interface MessageSender {                           // message = wiadomość
        String send(String text);
    }

    static class EmailSender2 implements MessageSender {            // ta sama klasa po refaktoryzacji (inna nazwa tylko dla lekcji)
        @Override
        public String send(String text) {
            return "e-mail: " + text;
        }
    }

    static class SmsSender implements MessageSender {   // sms = wiadomość tekstowa; nowa możliwość, której wcześniej nie było
        @Override
        public String send(String text) {
            return "SMS: " + text;
        }
    }

    static String notifyAfter(MessageSender sender, String text) {
        return sender.send(text);
    }

    /**
     * 8. Move (F6) przenosi klasę, metodę lub pole do innej klasy czy pakietu, poprawiając importy i wywołania.
     * Extract Interface wydziela z klasy interfejs — kod, który go używa, może pracować z dowolną implementacją.
     */
    static void moveAndInterface() {
        section("8. Move — F6 i Extract Interface");

        proveSame("brutto z netto 100,00 zł", OrderBefore.grossOf(10_000), OrderAfter.grossOf(10_000));
        // WYNIK: brutto z netto 100,00 zł → 12300 | 12300 → identyczne
        proveSame("powiadomienie e-mail", notifyBefore(new EmailSender(), "Witaj"), notifyAfter(new EmailSender2(), "Witaj"));
        // WYNIK: powiadomienie e-mail → e-mail: Witaj | e-mail: Witaj → identyczne
        show("ta sama metoda, inna implementacja", notifyAfter(new SmsSender(), "Witaj"));
        // WYNIK: ta sama metoda, inna implementacja → SMS: Witaj

        // MOVE (F6): metoda statyczna → IntelliJ pyta, do której klasy (Tax); dla klasy: do którego pakietu
        // (przenosi plik i poprawia wszystkie importy i deklaracje package). Metody instancyjne też da się
        // przenieść. Alternatywa: przeciągnij klasę w oknie Project (drag and drop) — to też Move.
        // Powiązane: F5 (Copy) kopiuje klasę pod nową nazwą, Alt+Delete (Safe Delete) usuwa element TYLKO jeśli
        // nic go nie używa (inaczej pokaże listę użyć).
        //
        // EXTRACT INTERFACE: prawy klik na klasie → Refactor → Extract Interface (albo Ctrl+Alt+Shift+T i wybór z listy)
        // → wybierz metody do interfejsu → Refactor. Pokrewne: Pull Members Up (metody do klasy bazowej).
        // Po co: kod zależy od UMOWY (interfejsu), nie od szczegółów (klasy) — podmienisz e-mail na SMS lub atrapę
        // w teście bez zmiany kodu, który z nich korzysta (zasada DIP, t27_clean_code_pitfalls/CleanCode02Solid).
        //
        // DOBRA PRAKTYKA: Move uruchamiaj, gdy metoda używa więcej danych z innej klasy niż z własnej — jej
        // miejsce jest tam, gdzie leżą dane, których potrzebuje.
    }

    // =================================================================================================
    // 9. CONVERT TO RECORD i PĘTLA → STREAM (Alt+Enter)
    // =================================================================================================

    // PRZED: klasa z danymi, ręcznie napisane equals / hashCode / toString
    static final class PointClass {                     // point = punkt
        private final int x;
        private final int y;

        PointClass(int x, int y) {
            this.x = x;
            this.y = y;
        }

        int x() {
            return x;
        }

        int y() {
            return y;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof PointClass p && p.x == x && p.y == y;   // instanceof ze wzorcem (Java 16+): sprawdź typ i nazwij
        }

        @Override
        public int hashCode() {
            return 31 * x + y;
        }

        @Override
        public String toString() {
            return "PointClass{x=" + x + ", y=" + y + "}";
        }
    }

    // PO: Alt+Enter na nazwie klasy → „Convert to record class” (Java 16+); całe ciało znika
    record PointRecord(int x, int y) { }

    // PRZED: pętla zbierająca wynik
    static List<String> longNamesBefore(List<String> names) {
        List<String> result = new ArrayList<>();
        for (String name : names) {
            if (name.length() > 3) {
                result.add(name.toUpperCase());
            }
        }
        return result;
    }

    // PO: Alt+Enter na słowie for → „Replace with collect” (zamień na zbieranie do listy)
    static List<String> longNamesAfter(List<String> names) {
        return names.stream()                           // stream = strumień; filter = zostaw pasujące; map = przekształć
                .filter(name -> name.length() > 3)
                .map(String::toUpperCase)
                .toList();                              // toList = do listy (Java 16+)
    }

    /**
     * 9. Alt+Enter pokazuje „intencje” (intentions) — drobne gotowe przekształcenia kodu, także refaktoryzacje
     * stylu: „Convert to record class” (klasa z danymi → rekord) i „Replace with collect/sum/anyMatch” (pętla → stream).
     */
    static void recordAndStream() {
        section("9. Alt+Enter: klasa → record, pętla → stream");

        proveSame("równość punktów (equals)", new PointClass(1, 2).equals(new PointClass(1, 2)),
                new PointRecord(1, 2).equals(new PointRecord(1, 2)));
        // WYNIK: równość punktów (equals) → true | true → identyczne
        proveSame("odczyt współrzędnej x()", new PointClass(1, 2).x(), new PointRecord(1, 2).x());
        // WYNIK: odczyt współrzędnej x() → 1 | 1 → identyczne
        show("toString klasy", new PointClass(1, 2));
        // WYNIK: toString klasy → PointClass{x=1, y=2}
        show("toString rekordu", new PointRecord(1, 2));
        // WYNIK: toString rekordu → PointRecord[x=1, y=2]

        List<String> names = List.of("Ala", "Bartek", "Celina", "Jan", "Henryk");
        proveSame("imiona dłuższe niż 3 litery", longNamesBefore(names), longNamesAfter(names));
        // WYNIK: imiona dłuższe niż 3 litery → [BARTEK, CELINA, HENRYK] | [BARTEK, CELINA, HENRYK] → identyczne

        // UWAGA: refaktoryzacja zachowuje ZNACZENIE programu, ale nie wszystko, co da się zaobserwować. Wynik
        // toString() rekordu ma inny format niż nasz ręczny — jeśli ktoś porównuje teksty w logach albo testach,
        // zobaczy różnicę. Przy „Convert to record class” sprawdź też, czy klasa nie jest używana przez bibliotekę
        // (np. JPA wymaga zwykłych klas) i czy nikt nie dziedziczy po tej klasie (rekord jest final).
        //
        // JAK: kursor na słowie class → Alt+Enter → „Convert to record class”. Na pętli for → Alt+Enter →
        // „Replace with collect” / „Replace with sum()” / „Replace with anyMatch()”. Po przekształceniu zawsze
        // PRZECZYTAJ wynik: stream nie zawsze jest czytelniejszy od pętli (zwłaszcza z wyjątkami i indeksami).
        // Streamy poznasz w dziale t16_streams, rekordy w t06_oop_basics.
        //
        // PUŁAPKA: Alt+Enter na czerwonym kodzie proponuje też „Create method / Create class” — skrót do pisania
        // „od końca” (najpierw wywołanie, potem metoda). Wygodne, ale uważaj, by nie wygenerować metody z literówką
        // w nazwie zamiast poprawić literówkę.
        //
        // PEŁNA LISTA INTENCJI: Settings → Editor → Intentions.
    }

    // =================================================================================================
    // 10. SKRÓTY NA CO DZIEŃ
    // =================================================================================================

    /** Jeden skrót: grupa tematyczna, nazwa akcji i klawisze (mapa „Windows”). */
    record Shortcut(String group, String action, String keys) { }

    static final List<Shortcut> SHORTCUTS = List.of(
            new Shortcut("Szukanie", "Search Everywhere (wszystko)", "dwa razy Shift"),
            new Shortcut("Szukanie", "Go to Class (klasa)", "Ctrl+N"),
            new Shortcut("Szukanie", "Go to File (plik)", "Ctrl+Shift+N"),
            new Shortcut("Szukanie", "Go to Symbol (metoda, pole)", "Ctrl+Alt+Shift+N"),
            new Shortcut("Szukanie", "Find Action (polecenia menu)", "Ctrl+Shift+A"),
            new Shortcut("Szukanie", "Find in Files (tekst w projekcie)", "Ctrl+Shift+F"),
            new Shortcut("Szukanie", "Find Usages (miejsca użycia)", "Alt+F7"),
            new Shortcut("Szukanie", "Replace in Files", "Ctrl+Shift+R"),
            new Shortcut("Nawigacja", "Go to Declaration (do deklaracji)", "Ctrl+B"),
            new Shortcut("Nawigacja", "Go to Implementation (do implementacji)", "Ctrl+Alt+B"),
            new Shortcut("Nawigacja", "Recent Files (ostatnie pliki)", "Ctrl+E"),
            new Shortcut("Nawigacja", "Recent Locations (ostatnie miejsca)", "Ctrl+Shift+E"),
            new Shortcut("Nawigacja", "Back / Forward (wstecz / naprzód)", "Ctrl+Alt+Left / Ctrl+Alt+Right"),
            new Shortcut("Nawigacja", "File Structure (spis metod pliku)", "Ctrl+F12"),
            new Shortcut("Nawigacja", "Last Edit Location (ostatnia zmiana)", "Ctrl+Shift+Backspace"),
            new Shortcut("Edycja", "Reformat Code (formatowanie)", "Ctrl+Alt+L"),
            new Shortcut("Edycja", "Optimize Imports (porządkuj importy)", "Ctrl+Alt+O"),
            new Shortcut("Edycja", "Generate (konstruktor, getter, equals...)", "Alt+Insert"),
            new Shortcut("Edycja", "Surround With (otocz if, try, for...)", "Ctrl+Alt+T"),
            new Shortcut("Edycja", "Show Intention Actions (żarówka)", "Alt+Enter"),
            new Shortcut("Edycja", "Duplicate Line (powiel linię)", "Ctrl+D"),
            new Shortcut("Edycja", "Delete Line (usuń linię)", "Ctrl+Y"),
            new Shortcut("Edycja", "Move Line (przesuń linię)", "Alt+Shift+Up / Alt+Shift+Down"),
            new Shortcut("Edycja", "Comment Line (komentarz)", "Ctrl+/"),
            new Shortcut("Edycja", "Extend Selection (rozszerz zaznaczenie)", "Ctrl+W"),
            new Shortcut("Edycja", "Shrink Selection (zawęź zaznaczenie)", "Ctrl+Shift+W"),
            new Shortcut("Edycja", "Complete Statement (dokończ instrukcję)", "Ctrl+Shift+Enter"),
            new Shortcut("Edycja", "Basic Completion (podpowiedzi)", "Ctrl+Space"),
            new Shortcut("Edycja", "Parameter Info (parametry metody)", "Ctrl+P"),
            new Shortcut("Edycja", "Quick Documentation (dokumentacja)", "Ctrl+Q"),
            new Shortcut("Edycja", "Override Methods / Implement Methods", "Ctrl+O / Ctrl+I"),
            new Shortcut("Wielokursor", "Select Next Occurrence (kolejne takie samo słowo)", "Alt+J"),
            new Shortcut("Wielokursor", "Select All Occurrences (wszystkie)", "Ctrl+Alt+Shift+J"),
            new Shortcut("Wielokursor", "Dodaj kursor w dowolnym miejscu", "Alt+Shift+kliknięcie"),
            new Shortcut("Uruchamianie", "Run (uruchom)", "Shift+F10"),
            new Shortcut("Uruchamianie", "Debug (uruchom z debuggerem)", "Shift+F9"),
            new Shortcut("Uruchamianie", "Stop", "Ctrl+F2"),
            new Shortcut("Git", "Commit", "Ctrl+K"),
            new Shortcut("Git", "Push", "Ctrl+Shift+K"),
            new Shortcut("Git", "Update Project (pull)", "Ctrl+T"),
            new Shortcut("Refaktoryzacja", "Rename", "Shift+F6"),
            new Shortcut("Refaktoryzacja", "Extract Variable", "Ctrl+Alt+V"),
            new Shortcut("Refaktoryzacja", "Extract Method", "Ctrl+Alt+M"),
            new Shortcut("Refaktoryzacja", "Extract Constant", "Ctrl+Alt+C"),
            new Shortcut("Refaktoryzacja", "Extract Field", "Ctrl+Alt+F"),
            new Shortcut("Refaktoryzacja", "Introduce Parameter", "Ctrl+Alt+P"),
            new Shortcut("Refaktoryzacja", "Inline", "Ctrl+Alt+N"),
            new Shortcut("Refaktoryzacja", "Change Signature", "Ctrl+F6"),
            new Shortcut("Refaktoryzacja", "Move", "F6"),
            new Shortcut("Refaktoryzacja", "Safe Delete (bezpieczne usuwanie)", "Alt+Delete"),
            new Shortcut("Refaktoryzacja", "Refactor This (menu wszystkich refaktoryzacji)", "Ctrl+Alt+Shift+T"));

    /** Szuka akcji po fragmencie nazwy (bez rozróżniania wielkości liter). */
    static String keysFor(String actionPart) {          // keys for = klawisze dla
        for (Shortcut s : SHORTCUTS) {
            if (s.action().toLowerCase().contains(actionPart.toLowerCase())) {
                return s.keys();
            }
        }
        return "(brak w tabeli)";
    }

    static long countGroup(String group) {              // count group = policz skróty w grupie
        return SHORTCUTS.stream().filter(s -> s.group().equals(group)).count();
    }

    /**
     * 10. Skróty, których używasz codziennie. Tabela jest w kodzie (lista SHORTCUTS) — możesz ją przeszukać
     * metodą {@code keysFor}. Najlepszy sposób nauki: wybierz TRZY skróty tygodnia i używaj ich za każdym razem,
     * zamiast sięgać po mysz. Gdy nie pamiętasz skrótu: Ctrl+Shift+A (Find Action) i wpisz nazwę polecenia —
     * lista pokaże też jego skrót.
     */
    static void shortcuts() {
        section("10. Skróty na co dzień");

        show("liczba skrótów w tabeli", SHORTCUTS.size());
        // WYNIK: liczba skrótów w tabeli → 51
        show("w tym refaktoryzacji", countGroup("Refaktoryzacja"));
        // WYNIK: w tym refaktoryzacji → 11
        show("skrót: Extract Method", keysFor("Extract Method"));
        // WYNIK: skrót: Extract Method → Ctrl+Alt+M
        show("skrót: Find Usages", keysFor("find usages"));
        // WYNIK: skrót: Find Usages → Alt+F7
        show("skrót: Reformat", keysFor("reformat"));
        // WYNIK: skrót: Reformat → Ctrl+Alt+L
        show("skrót: Generate", keysFor("generate"));
        // WYNIK: skrót: Generate → Alt+Insert

        // SZABLONY „LIVE TEMPLATES” — wpisz skrót i naciśnij Tab:
        //   sout + Tab  → System.out.println();            soutv + Tab → println z nazwą i wartością zmiennej
        //   psvm + Tab  → public static void main(String[] args) { }
        //   fori + Tab  → pętla for z indeksem             iter + Tab → pętla for-each po kolekcji
        //   itar + Tab  → pętla po tablicy                 ifn / inn + Tab → if (x == null) / if (x != null)
        // Ctrl+J pokazuje listę wszystkich szablonów pasujących do miejsca w kodzie; własne dodasz w
        // Settings → Editor → Live Templates.
        //
        // WIELOKURSOR: jedno słowo użyte w wielu miejscach edytujesz naraz — ustaw kursor na słowie,
        // Alt+J (kolejne wystąpienie, powtarzaj) albo Ctrl+Alt+Shift+J (wszystkie), wpisz nowy tekst, Esc.
        // Ale dla zmiany nazw w KODZIE używaj Rename (Shift+F6), który rozumie kod — wielokursor to tylko tekst.
        //
        // PUŁAPKA: skróty zależą od MAPY KLAWIATURY (Settings → Keymap) i systemu. Na macOS są inne (Cmd zamiast Ctrl),
        // a na laptopie klawisze funkcyjne (F6) mogą wymagać Fn. Niektóre skróty (np. Ctrl+Alt+strzałki, Ctrl+Alt+L)
        // przechwytują sterowniki karty graficznej — jeśli skrót „nie działa”, sprawdź Keymap lub wyłącz skróty
        // w sterowniku. Jeśli na Windowsie ktoś zmienił mapę, w Keymap wybierz „Windows” (domyślną).
        //
        // DOBRA PRAKTYKA: nawigacja bez myszy (Ctrl+N, Ctrl+E, Ctrl+B, Alt+F7) skraca dzień pracy o godziny.
    }

    // =================================================================================================
    // 11. BEZPIECZNE NAWYKI
    // =================================================================================================

    /** Mała „siatka bezpieczeństwa”: ile spośród wejść daje RÓŻNE wyniki w starej i nowej wersji. */
    static int countDifferences(IntBinaryOperator oldWay, IntBinaryOperator newWay, int[][] inputs) {
        int differences = 0;
        for (int[] in : inputs) {
            if (oldWay.applyAsInt(in[0], in[1]) != newWay.applyAsInt(in[0], in[1])) {
                differences++;
            }
        }
        return differences;
    }

    /**
     * 11. Refaktoryzacja nie może zmienić zachowania — a pewność dają tylko testy lub porównanie starej i nowej
     * wersji na tych samych danych. Pokazujemy to na wersji napisanej RĘCZNIE, z subtelnym błędem.
     */
    static void safeHabits() {
        section("11. Bezpieczne nawyki: testy, małe kroki, kontrola wersji");

        // Wzór: stary kod  (a / b * 3)   — dzielenie całkowite najpierw, potem mnożenie.
        IntBinaryOperator oldWay = (a, b) -> a / b * 3;
        // Ręczne „uporządkowanie”: zmieniona kolejność działań — wygląda niewinnie, ale to inny wzór (a * 3 / b).
        IntBinaryOperator manual = (a, b) -> a * 3 / b;
        // Wersja po Extract Variable: kolejność zachowana.
        IntBinaryOperator extracted = (a, b) -> {
            int quotient = a / b;                       // quotient = iloraz
            return quotient * 3;
        };
        int[][] inputs = {{10, 5}, {7, 2}, {9, 4}, {100, 7}, {3, 3}};

        show("różnice: stary kod vs ręczna zmiana kolejności", countDifferences(oldWay, manual, inputs));
        // WYNIK: różnice: stary kod vs ręczna zmiana kolejności → 1
        show("różnice: stary kod vs Extract Variable", countDifferences(oldWay, extracted, inputs));
        // WYNIK: różnice: stary kod vs Extract Variable → 0
        show("7 / 2 * 3 (stary)", oldWay.applyAsInt(7, 2));
        // WYNIK: 7 / 2 * 3 (stary) → 9
        show("7 * 3 / 2 (ręczny)", manual.applyAsInt(7, 2));
        // WYNIK: 7 * 3 / 2 (ręczny) → 10

        // ZASADY BEZPIECZNEJ REFAKTORYZACJI:
        //   1. ZACZNIJ OD TESTÓW (albo choćby od zapisu obecnych wyników — jak wyżej). Bez nich „refaktoryzujesz”
        //      na wiarę. Testy poznasz w t32_junit_mockito; do tego czasu — metoda main i Check.equal.
        //   2. ZRÓB COMMIT PRZED (Ctrl+K, Start06Git). Gdy coś pójdzie źle: git restore / reset do commita.
        //   3. MAŁE KROKI: jedna refaktoryzacja → uruchom program/testy → commit. Nie mieszaj z dodawaniem funkcji
        //      („kapelusz refaktoryzacji” i „kapelusz funkcji” zakładaj osobno — jeden w danej chwili).
        //   4. UŻYWAJ NARZĘDZIA, nie ręcznych zmian: Shift+F6 zamiast Ctrl+H (zamień tekst) — rozumie typy.
        //   5. SPRAWDŹ PODGLĄD, gdy IntelliJ pokazuje konflikty lub listę zmian w wielu plikach.
        //   6. W razie wątpliwości Ctrl+Z cofa całą refaktoryzację; Local History (prawy klik → Local History)
        //      przywróci stare wersje plików nawet bez gita.
        //
        // PUŁAPKA: „Find and Replace” (Ctrl+R / Ctrl+Shift+R) zamienia TEKST, a nie znaczenie: zamiana „id” na „userId”
        // zmieni też „valid”, „width” i napisy w komentarzach. Do zmian w kodzie używaj refaktoryzacji.
        //
        // PUŁAPKA: refaktoryzacja nie zmienia zachowania PRZY ZAŁOŻENIU, że kod był poprawny. Jeśli zawiera błąd,
        // refaktoryzacja go nie naprawi — i niczego nie ostrzeże. Najpierw testy, potem porządki.
        //
        // DOBRA PRAKTYKA: refaktoryzuj przy okazji pracy („zasada skauta”: zostaw obóz czystszym, niż zastałeś),
        // a nie w osobnych, wielkich akcjach „porządkowania wszystkiego”.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Rename Shift+F6 · Extract Variable Ctrl+Alt+V · Extract Method Ctrl+Alt+M · Extract Constant Ctrl+Alt+C
     *   • Introduce Parameter Ctrl+Alt+P · Inline Ctrl+Alt+N · Change Signature Ctrl+F6 · Move F6
     *   • Menu wszystkich refaktoryzacji: Ctrl+Alt+Shift+T; intencje i szybkie poprawki: Alt+Enter.
     *   • Refaktoryzacja = zmiana struktury BEZ zmiany zachowania; Ctrl+Z cofa ją w całości.
     *   • Nie znasz skrótu? Ctrl+Shift+A i wpisz nazwę polecenia; dwa razy Shift szuka wszystkiego.
     *   • Inline wyrażenia z efektem ubocznym zmienia program (wywołanie wykona się wielokrotnie).
     *   • Rename nie znajdzie nazw użytych jako tekst (refleksja, pliki konfiguracyjne, SQL).
     *   • Najpierw testy i commit, potem małe kroki: jedna refaktoryzacja = uruchomienie + commit.
     *
     * PYTANIA KONTROLNE:
     *   1. Jaki skrót uruchamia Rename, a jaki Extract Method? Gdzie sprawdzisz skrót, którego nie pamiętasz?
     *   2. Czym różni się Extract Variable od Extract Constant i kiedy wybierzesz które?
     *   3. Co wypisze:  int a = 7, b = 2;  System.out.println(a / b * 3);  System.out.println(a * 3 / b);  ?
     *   4. Co wypisze (licznik zaczyna od 0, next() zwraca ++licznik):
     *        int id = next();  System.out.println(id + id);   a po Inline:   System.out.println(next() + next());  ?
     *   5. ZNAJDŹ BŁĄD:  Zmieniasz nazwę klasy UserDao przez Shift+F6. Program się kompiluje, ale po uruchomieniu
     *      rzuca ClassNotFoundException. Skąd błąd i jak go uniknąć?
     *   6. ZNAJDŹ BŁĄD:  Masz stałą int MAX = 100 użytą jako limit punktów i jako mianownik procentów.
     *      Extract Constant zastąpił wszystkie „100” jedną stałą MAX. Co złego może się stać?
     *   7. Dlaczego po refaktoryzacji toString() rekordu daje inny napis niż ręczna klasa — czy to znaczy, że IntelliJ się pomylił?
     *   8. Wymień trzy rzeczy, które robisz PRZED większą refaktoryzacją.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    // Punkt wyjścia do ćwiczeń 1–3: „brudne” wersje (działają poprawnie, ale są nieczytelne).

    /** Brudna wersja do ćwiczenia 1: czas w sekundach → napis „g:mm:ss”. */
    static String messyDuration(int s) {                // duration = czas trwania
        return (s / 3600) + ":" + ((s % 3600) / 60 < 10 ? "0" : "") + ((s % 3600) / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
    }

    /** Brudna wersja do ćwiczenia 2: suma kwadratów parzystych liczb (pętla). */
    static int messySquares(List<Integer> numbers) {
        int sum = 0;
        for (Integer n : numbers) {
            if (n % 2 == 0) {
                sum += n * n;
            }
        }
        return sum;
    }

    /** Brudna wersja do ćwiczenia 3: cena po rabacie (magiczne liczby w ciele). */
    static long messyDiscounted(long cents, boolean vip) {
        return vip ? cents * 80 / 100 : cents * 95 / 100;
    }

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: czas 3725 s", messyDuration(3725), () -> exercise1(3725));
        Check.equal("ćw. 1: czas 59 s", messyDuration(59), () -> exercise1(59));
        Check.equal("ćw. 2: suma kwadratów parzystych", messySquares(List.of(1, 2, 3, 4, 6)), () -> exercise2(List.of(1, 2, 3, 4, 6)));
        Check.equal("ćw. 3: VIP", messyDiscounted(20_000, true), () -> exercise3(20_000, true));
        Check.equal("ćw. 3: zwykły klient", messyDiscounted(20_000, false), () -> exercise3(20_000, false));
        Check.equal("ćw. 4: pierwsza różnica", 1, () -> exercise4((a, b) -> a / b * 3, (a, b) -> a * 3 / b,
                new int[][] {{10, 5}, {7, 2}, {9, 3}}));
        Check.equal("ćw. 4: brak różnic", -1, () -> exercise4((a, b) -> a + b, (a, b) -> b + a, new int[][] {{1, 2}, {3, 4}}));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", messyDuration(3725), () -> solution1(3725));
        Check.equal("ćw. 1 (wzorzec, 59 s)", messyDuration(59), () -> solution1(59));
        Check.equal("ćw. 2 (wzorzec)", messySquares(List.of(1, 2, 3, 4, 6)), () -> solution2(List.of(1, 2, 3, 4, 6)));
        Check.equal("ćw. 3 (wzorzec, VIP)", messyDiscounted(20_000, true), () -> solution3(20_000, true));
        Check.equal("ćw. 3 (wzorzec, zwykły)", messyDiscounted(20_000, false), () -> solution3(20_000, false));
        Check.equal("ćw. 4 (wzorzec)", 1, () -> solution4((a, b) -> a / b * 3, (a, b) -> a * 3 / b,
                new int[][] {{10, 5}, {7, 2}, {9, 3}}));
        Check.equal("ćw. 4 (wzorzec, brak różnic)", -1, () -> solution4((a, b) -> a + b, (a, b) -> b + a, new int[][] {{1, 2}, {3, 4}}));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 7 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): wykonaj w głowie (a potem w IntelliJ) Extract Method i Extract Variable na
     * messyDuration: napisz czytelną wersję z metodą pomocniczą (np. twoDigits) i zmiennymi hours / minutes /
     * seconds. Wynik ma być IDENTYCZNY jak messyDuration dla każdego wejścia.
     * Podpowiedź: godziny = s / 3600, minuty = (s % 3600) / 60, sekundy = s % 60; liczba < 10 dostaje zero z przodu.
     */
    static String exercise1(int seconds) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ pętlę z messySquares na stream (to robi intencja Alt+Enter „Replace with sum()”).
     * <pre>{@code
     * // PRZED:
     * int sum = 0;
     * for (Integer n : numbers) { if (n % 2 == 0) { sum += n * n; } }
     * // PO: numbers.stream() ... filter ... mapToInt ... sum
     * }</pre>
     * Podpowiedź: filter(n -> n % 2 == 0), mapToInt(n -> n * n), sum() (strumienie: t16_streams).
     */
    static int exercise2(List<Integer> numbers) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zrób Extract Constant i Extract Variable na messyDiscounted: stałe
     * VIP_PERCENT i REGULAR_PERCENT (ile procent ceny płaci klient) oraz zmienna pomocnicza z wybranym procentem.
     * Wynik ma być taki sam jak messyDiscounted. Stałe zadeklaruj jako private static final w tej klasie.
     * Podpowiedź: najpierw wybierz procent (vip ? 80 : 95), potem policz cents * percent / 100.
     */
    static long exercise3(long cents, boolean vip) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): napisz „siatkę bezpieczeństwa” — zwróć INDEKS pierwszego wejścia (w tablicy inputs),
     * dla którego stara i nowa wersja dają różny wynik, albo -1, gdy różnic nie ma.
     * Podpowiedź: wzoruj się na countDifferences z sekcji 11, ale zatrzymaj się na pierwszej różnicy.
     */
    static int exercise4(IntBinaryOperator oldWay, IntBinaryOperator newWay, int[][] inputs) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    private static final int SECONDS_PER_HOUR = 3600;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int VIP_PERCENT = 80;
    private static final int REGULAR_PERCENT = 95;

    static String twoDigits(int number) {               // two digits = dwie cyfry
        return (number < 10 ? "0" : "") + number;
    }

    static String solution1(int seconds) {
        int hours = seconds / SECONDS_PER_HOUR;
        int minutes = (seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE;
        int rest = seconds % SECONDS_PER_MINUTE;
        return hours + ":" + twoDigits(minutes) + ":" + twoDigits(rest);
    }

    static int solution2(List<Integer> numbers) {
        return numbers.stream()
                .filter(n -> n % 2 == 0)
                .mapToInt(n -> n * n)
                .sum();
    }

    static long solution3(long cents, boolean vip) {
        int percent = vip ? VIP_PERCENT : REGULAR_PERCENT;
        return cents * percent / 100;
    }

    static int solution4(IntBinaryOperator oldWay, IntBinaryOperator newWay, int[][] inputs) {
        for (int i = 0; i < inputs.length; i++) {
            if (oldWay.applyAsInt(inputs[i][0], inputs[i][1]) != newWay.applyAsInt(inputs[i][0], inputs[i][1])) {
                return i;
            }
        }
        return -1;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Rename: Shift+F6, Extract Method: Ctrl+Alt+M. Nie pamiętasz skrótu: Ctrl+Shift+A (Find Action) i nazwa
     *      polecenia (lista pokazuje też skrót), albo dwa razy Shift (Search Everywhere).
     *   2. Extract Variable (Ctrl+Alt+V) nazywa wyrażenie lokalne w jednej metodzie. Extract Constant (Ctrl+Alt+C)
     *      robi z wartości stałą klasy (static final), wspólną dla wszystkich użyć — dla reguł i ustawień
     *      (stawka VAT, limit), a nie dla wyników pośrednich obliczeń.
     *   3. 9 i 10. (7 / 2 = 3 w dzieleniu całkowitym, razy 3 = 9; a 7 * 3 = 21, 21 / 2 = 10.)
     *   4. 2 (id = 1, 1 + 1), a po Inline: 3 (next() zwraca 1, potem 2: 1 + 2). Inline zmienił program, bo
     *      wywołanie z efektem ubocznym wykonało się dwa razy.
     *   5. Nazwa klasy została użyta jako TEKST (np. Class.forName("pakiet.UserDao"), plik .xml / .properties,
     *      konfiguracja). Rename widzi tylko kod Javy. Uniknięcie: włącz „Search for text occurrences”, przeszukaj
     *      projekt (Ctrl+Shift+F) po starej nazwie, napisz test, który ładuje taką klasę.
     *   6. Dwie różne rzeczy (limit punktów i mianownik procentów) zostały połączone w jedną stałą. Zmiana limitu
     *      punktów na 50 zmieniłaby też obliczanie procentów — program zacząłby liczyć błędnie. Stałe tworzymy
     *      dla ZNACZENIA, nie dla wartości; przeglądaj zamieniane wystąpienia.
     *   7. IntelliJ nie pomylił się: refaktoryzacja zachowuje ZACHOWANIE (znaczenie), ale format toString()
     *      jest generowany przez język (rekord: Nazwa[x=1, y=2]). Zmieni się tekst, nie wartości ani equals.
     *      Jeśli ktoś zależy od dokładnego napisu, napisz toString ręcznie.
     *   8. Na przykład: (1) napisz testy lub zapisz obecne wyniki, (2) zrób commit (Ctrl+K), (3) zaplanuj małe kroki
     *      i po każdym uruchom program lub testy.
     */
    // </editor-fold>
}
