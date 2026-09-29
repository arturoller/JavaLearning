package t16_streams;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Category;
import helpers.model.Customer;
import helpers.model.Department;
import helpers.model.Employee;
import helpers.model.Order;
import helpers.model.Product;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Collectors.toMap — strumień zamieniony w mapę
 *        (to map = do mapy; key = klucz; value = wartość; merge = scal)
 *
 * W SKRÓCIE:
 *   toMap(jakKlucz, jakWartość) buduje mapę: każdy element daje JEDNĄ parę klucz → wartość.
 *   Gdy dwa elementy dadzą ten sam klucz — wyjątek, chyba że podasz funkcję scalającą (merge).
 *   Czwarty argument wybiera rodzaj mapy (LinkedHashMap, TreeMap, EnumMap).
 *
 * ANALOGIA: Szatnia w teatrze. Każdy płaszcz (element) dostaje numerek (klucz) i wisi na wieszaku (wartość).
 *   Dwa płaszcze z tym samym numerkiem? Szatniarz protestuje (IllegalStateException) — chyba że dostał
 *   instrukcję „wieszaj oba na jednym wieszaku” albo „zostaw pierwszy” (funkcja scalająca).
 *   Pusty wieszak (null jako wartość) jest zabroniony — szatniarz odmawia (NullPointerException).
 *
 * JAK TO DZIAŁA:
 *   toMap(k, v)                    → HashMap; duplikat klucza → IllegalStateException
 *   toMap(k, v, merge)             → HashMap; duplikat → merge(stara, nowa)
 *   toMap(k, v, merge, Mapa::new)  → wybrana mapa (LinkedHashMap / TreeMap / EnumMap)
 *   toUnmodifiableMap(k, v)        → mapa tylko do odczytu (Java 10+), kolejność losowa
 *
 *   Każdy element:  klucz = k(element), wartość = v(element)
 *     klucza nie ma w mapie → wstaw;  klucz jest → merge(stara, nowa)  albo wyjątek
 *     wartość == null → NullPointerException (zawsze!)
 *
 * SŁÓWKA:
 *   key = klucz; value = wartość; merge = scal; identity = tożsamość (funkcja „zwróć to, co dostałeś”);
 *   duplicate = duplikat; supplier = dostawca; entry = wpis (para klucz-wartość); invert = odwróć;
 *   frequency = częstość; comparing by value = porównuj po wartości; unmodifiable = niemodyfikowalny
 *
 * ZOBACZ TEŻ: t12_collections/Collections05Maps (HashMap, TreeMap, LinkedHashMap, merge),
 *   t16_streams/Streams09CollectorsBasic (idea kolektora), t16_streams/Streams11GroupingBy (gdy klucz ma
 *   WIELE elementów), t13_lambdas/Lambda04MethodReferences (Function.identity, referencje do metod)
 * </pre>
 */
public class Streams10CollectorsToMap {

    public static void main(String[] args) {
        title("Streams10 — Collectors.toMap: strumień → mapa");

        basics();                 // basics = podstawy
        duplicateKey();           // duplicate key = zduplikowany klucz
        mergeFunction();          // merge function = funkcja scalająca
        mapSupplier();            // map supplier = dostawca mapy (rodzaj mapy)
        nullValue();              // null value = wartość null
        wordFrequency();          // word frequency = częstość słów
        invertMap();              // invert map = odwrócenie mapy
        sortByValue();            // sort by value = sortowanie po wartości
        unmodifiableMap();        // unmodifiable map = mapa niemodyfikowalna
        exercises();              // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PODSTAWY: TOMAP(KLUCZ, WARTOŚĆ), FUNCTION.IDENTITY(), INDEKS
    // =================================================================================================

    /**
     * 1. Najczęstsze użycie toMap: „indeks” — szybkie wyszukiwanie obiektu po jego identyfikatorze
     * (sku, id). Function.identity() znaczy „wartością jest sam element”.
     */
    static void basics() {
        section("1. toMap(klucz, wartość) i indeks po sku");

        List<Product> products = SampleData.products();

        Map<String, Product> bySku = products.stream()
                .collect(Collectors.toMap(
                        Product::sku,             // klucz: sku
                        Function.identity()));    // Function.identity = tożsamość, czyli p -> p (sam produkt)
        show("rozmiar indeksu", bySku.size());
        // WYNIK: rozmiar indeksu → 14
        show("bySku.get(\"KSI-002\")", bySku.get("KSI-002"));
        // WYNIK: bySku.get("KSI-002") → Java. Podstawy (129.00 zł)
        show("bySku.get(\"XXX-999\")", bySku.get("XXX-999"));
        // WYNIK: bySku.get("XXX-999") → null

        // PRZED: szukanie w liście = przejście po wszystkich elementach za KAŻDYM razem.
        // PO: mapa = jedno przejście przy budowie, potem get(...) prawie natychmiast. Opłaca się przy wielu wyszukiwaniach.

        Map<String, String> skuToName = products.stream()
                .filter(p -> p.category() == Category.DOM)
                .collect(Collectors.toMap(Product::sku, Product::name));
        // PUŁAPKA: toMap(k, v) daje HashMap — kolejność wpisów NIEOKREŚLONA. Do wypisania kopiujemy do TreeMap
        // (posortowane klucze). Lepszy sposób (od razu właściwa mapa) — sekcja 4.
        show("DOM: sku → nazwa", new TreeMap<>(skuToName));
        // WYNIK: DOM: sku → nazwa → {DOM-001=Ekspres do kawy, DOM-002=Lampka biurkowa}

        Map<Long, Customer> customersById = SampleData.customers().stream()
                .collect(Collectors.toMap(Customer::id, c -> c));   // c -> c robi to samo co Function.identity()
        show("customersById.get(4L)", customersById.get(4L));
        // WYNIK: customersById.get(4L) → Zofia Krawczyk (Warszawa, VIP)

        // PUŁAPKA: klucze to Long, a 4 (bez L) to int → zapakuje się w Integer. Integer(4) ≠ Long(4), więc:
        show("customersById.get(4) — bez L", customersById.get(4));
        // WYNIK: customersById.get(4) — bez L → null    ← kompilator nie ostrzega, bo get przyjmuje Object
    }

    // =================================================================================================
    // 2. DUPLIKAT KLUCZA → ILLEGALSTATEEXCEPTION
    // =================================================================================================

    /**
     * 2. toMap(k, v) zakłada, że klucze są UNIKALNE. Gdy nie są — IllegalStateException z czytelnym
     * komunikatem: który klucz i jakie dwie wartości się zderzyły.
     */
    static void duplicateKey() {
        section("2. Duplikat klucza → IllegalStateException");

        List<Employee> employees = SampleData.employees();

        // Dział → imię? Ale w IT pracują cztery osoby!
        expectThrows("toMap(dział, imię)", () -> employees.stream()
                .collect(Collectors.toMap(Employee::department, Employee::name)));
        // WYNIK: ✔ toMap(dział, imię) → rzucono IllegalStateException: Duplicate key IT (attempted merging values Anna Nowak and Piotr Kowalski)

        // Komunikat po polsku: „Zduplikowany klucz IT (próba scalenia wartości Anna Nowak i Piotr Kowalski)”.
        // To wyjątek w czasie DZIAŁANIA, nie kompilacji — kod z duplikatami kompiluje się bez problemu.

        // Dwie drogi wyjścia:
        //   a) chcesz JEDNĄ wartość na klucz (pierwszą / ostatnią / sumę / max) → funkcja scalająca (sekcja 3),
        //   b) chcesz WSZYSTKIE wartości dla klucza → groupingBy (t16_streams/Streams11GroupingBy).
        // DOBRA PRAKTYKA: toMap(k, v) bez merge stosuj tylko dla kluczy naprawdę unikalnych (sku, id, e-mail).
        // Wtedy wyjątek jest ZALETĄ — głośno powie, że dane są zepsute.
    }

    // =================================================================================================
    // 3. FUNKCJA SCALAJĄCA: PIERWSZY / OSTATNI / SUMA / MAX
    // =================================================================================================

    /**
     * 3. Trzeci argument toMap to {@code BinaryOperator<V>}: dostaje starą i nową wartość dla tego samego
     * klucza i zwraca tę, która ma zostać w mapie.
     */
    static void mergeFunction() {
        section("3. Funkcja scalająca (merge)");

        List<Employee> employees = SampleData.employees();
        // (new TreeMap<>(...) tylko po to, żeby wypisać klucze enum w stałej kolejności — sekcja 4 zrobi to lepiej)

        Map<Department, String> first = employees.stream()
                .collect(Collectors.toMap(Employee::department, Employee::name,
                        (oldName, newName) -> oldName));          // zostaw PIERWSZEGO
        show("pierwszy w dziale", new TreeMap<>(first));
        // WYNIK: pierwszy w dziale → {IT=Anna Nowak, HR=Katarzyna Wiśniewska, SPRZEDAZ=Tomasz Wójcik, KSIEGOWOSC=Magdalena Kamińska, MARKETING=Agnieszka Zielińska}

        Map<Department, String> last = employees.stream()
                .collect(Collectors.toMap(Employee::department, Employee::name,
                        (oldName, newName) -> newName));          // zostaw OSTATNIEGO (nadpisuj)
        show("ostatni w dziale", new TreeMap<>(last));
        // WYNIK: ostatni w dziale → {IT=Ewa Woźniak, HR=Katarzyna Wiśniewska, SPRZEDAZ=Krzysztof Szymański, KSIEGOWOSC=Magdalena Kamińska, MARKETING=Paweł Dąbrowski}

        Map<Department, Integer> sum = employees.stream()
                .collect(Collectors.toMap(Employee::department, Employee::salary, Integer::sum));   // SUMUJ
        show("suma pensji", new TreeMap<>(sum));
        // WYNIK: suma pensji → {IT=53600, HR=7200, SPRZEDAZ=20100, KSIEGOWOSC=8100, MARKETING=17400}

        Map<Department, Integer> max = employees.stream()
                .collect(Collectors.toMap(Employee::department, Employee::salary, Math::max));      // MAKSIMUM
        show("najwyższa pensja", new TreeMap<>(max));
        // WYNIK: najwyższa pensja → {IT=17200, HR=7200, SPRZEDAZ=11200, KSIEGOWOSC=8100, MARKETING=9800}

        // Najlepiej zarabiająca OSOBA w dziale: wartością jest cały Employee, a scala BinaryOperator.maxBy.
        Map<Department, Employee> topEarner = employees.stream()
                .collect(Collectors.toMap(Employee::department, Function.identity(),
                        BinaryOperator.maxBy(Comparator.comparingInt(Employee::salary))));   // maxBy = większy według
        showEach("najlepiej zarabiający w dziale", new TreeMap<>(topEarner));
        // WYNIK: najlepiej zarabiający w dziale (liczba kluczy: 5):
        // WYNIK: • IT → Michał Lewandowski (IT, 17200 zł)
        // WYNIK: • HR → Katarzyna Wiśniewska (HR, 7200 zł)
        // WYNIK: • SPRZEDAZ → Krzysztof Szymański (SPRZEDAZ, 11200 zł)
        // WYNIK: • KSIEGOWOSC → Magdalena Kamińska (KSIEGOWOSC, 8100 zł)
        // WYNIK: • MARKETING → Paweł Dąbrowski (MARKETING, 9800 zł)
        // Zauważ: tu wartości NIE są Optional — w przeciwieństwie do groupingBy + maxBy (Streams11).

        // PUŁAPKA: (a, b) -> a + ", " + b „działa”, ale to sklejanie Stringów w pętli. Do zbierania WIELU
        // wartości pod kluczem służy groupingBy + mapping + joining (Streams11) — czytelniej i szybciej.
    }

    // =================================================================================================
    // 4. DOSTAWCA MAPY: LINKEDHASHMAP / TREEMAP / ENUMMAP
    // =================================================================================================

    /**
     * 4. Czwarty argument toMap to dostawca mapy (np. TreeMap::new). Istnieje tylko wersja z czterema
     * argumentami — więc trzeba też podać funkcję scalającą.
     */
    static void mapSupplier() {
        section("4. Dostawca mapy: LinkedHashMap, TreeMap, EnumMap");

        List<Product> products = SampleData.products();

        // LinkedHashMap — kolejność jak w strumieniu (kolejność „napotkania”)
        Map<String, Integer> lowStockInOrder = products.stream()
                .filter(p -> p.stock() < 10)
                .collect(Collectors.toMap(Product::name, Product::stock,
                        (a, b) -> a,                  // merge — wymagany w tej wersji (tu duplikatów nie ma)
                        LinkedHashMap::new));         // LinkedHashMap = mapa pamiętająca kolejność wstawiania
        show("LinkedHashMap", lowStockInOrder);
        // WYNIK: LinkedHashMap → {Laptop Pro 14=7, Smartfon X=0, Monitor 27 cali=4, Oliwa z oliwek=0, Java. Podstawy=9, Wzorce projektowe=3, Ekspres do kawy=2}

        // TreeMap — posortowane klucze (tu: alfabetycznie wg Unicode)
        Map<String, Integer> lowStockSorted = products.stream()
                .filter(p -> p.stock() < 10)
                .collect(Collectors.toMap(Product::name, Product::stock, (a, b) -> a, TreeMap::new));
        show("TreeMap", lowStockSorted);
        // WYNIK: TreeMap → {Ekspres do kawy=2, Java. Podstawy=9, Laptop Pro 14=7, Monitor 27 cali=4, Oliwa z oliwek=0, Smartfon X=0, Wzorce projektowe=3}

        // EnumMap — najlepsza mapa dla kluczy enum: szybka, zawsze w kolejności deklaracji stałych.
        Map<Department, Integer> salaryByDepartment = SampleData.employees().stream()
                .collect(Collectors.toMap(Employee::department, Employee::salary, Integer::sum,
                        () -> new EnumMap<>(Department.class)));   // EnumMap potrzebuje klasy enuma → lambda, nie ::new
        show("EnumMap", salaryByDepartment);
        // WYNIK: EnumMap → {IT=53600, HR=7200, SPRZEDAZ=20100, KSIEGOWOSC=8100, MARKETING=17400}

        // PUŁAPKA: HashMap z kluczami ENUM wypisuje się w kolejności, która potrafi się ZMIENIAĆ między
        // uruchomieniami programu (hashCode enuma to „hash tożsamości” nadawany przez JVM — przy każdym starcie inny).
        // Dlatego w tym kursie
        // nigdy nie wypisujemy takiej mapy — używamy TreeMap::new albo EnumMap.
        // DOBRA PRAKTYKA: jesteś pewien, że duplikatów nie ma, a musisz podać merge? Niech głośno protestuje:
        //     (a, b) -> { throw new IllegalStateException("duplikat: " + a + " i " + b); }
    }

    // =================================================================================================
    // 5. NULL JAKO WARTOŚĆ → NULLPOINTEREXCEPTION
    // =================================================================================================

    /**
     * 5. toMap nie przyjmuje null jako WARTOŚCI — nawet do HashMap, która sama null-e akceptuje.
     * Maria Nowak i Ola Pawlak nie mają e-maila (email() == null).
     */
    static void nullValue() {
        section("5. null jako wartość → NullPointerException");

        List<Customer> customers = SampleData.customers();

        expectThrows("toMap(imię, email)", () -> customers.stream()
                .collect(Collectors.toMap(Customer::name, Customer::email)));
        // WYNIK: ✔ toMap(imię, email) → rzucono NullPointerException: (brak komunikatu)
        // PUŁAPKA: komunikat jest pusty — nie wiadomo, KTÓRY klient zawinił. W dużych danych szukasz igły w stogu siana.

        // Rozwiązanie 1: zamień null na wartość zastępczą (findEmail() zwraca Optional — t14_optional/Optional01Basics)
        Map<String, String> emails = customers.stream()
                .collect(Collectors.toMap(Customer::name,
                        c -> c.findEmail().orElse("(brak)"),   // findEmail = znajdź e-mail; orElse = albo
                        (a, b) -> a, TreeMap::new));
        showEach("e-maile", emails);
        // WYNIK: e-maile (liczba kluczy: 7):
        // WYNIK: • Adam Mazur → adam.mazur@example.com
        // WYNIK: • Ewa Lis → ewa.lis@example.com
        // WYNIK: • Jan Kowalski → jan@example.com
        // WYNIK: • Marek Król → marek@example.com
        // WYNIK: • Maria Nowak → (brak)
        // WYNIK: • Ola Pawlak → (brak)
        // WYNIK: • Zofia Krawczyk → zofia@example.com

        // Rozwiązanie 2: pomiń tych bez wartości
        Map<String, String> onlyWithEmail = customers.stream()
                .filter(c -> c.email() != null)
                .collect(Collectors.toMap(Customer::name, Customer::email));
        show("tylko z e-mailem: ile", onlyWithEmail.size());
        // WYNIK: tylko z e-mailem: ile → 5

        // DOBRA PRAKTYKA: zanim zbudujesz mapę, zdecyduj, co znaczy „brak wartości”: pominąć, wartość
        // zastępcza, a może Optional w środku? (Optional jako wartość mapy to zwykle zły pomysł — Streams14.)
    }

    // =================================================================================================
    // 6. CZĘSTOŚĆ SŁÓW: TOMAP(…, INTEGER::SUM) KONTRA GROUPINGBY(COUNTING())
    // =================================================================================================

    /**
     * 6. Klasyczne zadanie: ile razy wystąpiło każde słowo. Dwa sposoby, dwa różne typy wartości:
     * Integer i Long — i przez to mapy NIE są sobie równe.
     */
    static void wordFrequency() {
        section("6. Częstość słów: toMap vs groupingBy(counting())");

        List<String> words = SampleData.words();

        // Każde słowo daje parę (słowo → 1); przy powtórce jedynki się sumują.
        Map<String, Integer> byToMap = words.stream()
                .collect(Collectors.toMap(w -> w, w -> 1, Integer::sum, TreeMap::new));
        show("toMap(w -> w, w -> 1, Integer::sum)", byToMap);
        // WYNIK: toMap(w -> w, w -> 1, Integer::sum) → {enum=1, java=3, kolekcja=1, lambda=1, lista=1, mapa=1, optional=1, rekord=1, stream=2}

        // To samo przez groupingBy (Streams11): „pogrupuj takie same słowa i policz grupę”.
        Map<String, Long> byGrouping = words.stream()
                .collect(Collectors.groupingBy(w -> w, TreeMap::new, Collectors.counting()));
        show("groupingBy(w -> w, counting())", byGrouping);
        // WYNIK: groupingBy(w -> w, counting()) → {enum=1, java=3, kolekcja=1, lambda=1, lista=1, mapa=1, optional=1, rekord=1, stream=2}

        // PUŁAPKA: wyglądają identycznie, ale Integer 1 ≠ Long 1 (equals sprawdza też typ). Mapy NIE są równe!
        show("byToMap.equals(byGrouping)", byToMap.equals(byGrouping));
        // WYNIK: byToMap.equals(byGrouping) → false

        // DOBRA PRAKTYKA: do liczenia wystąpień wybieraj groupingBy(..., counting()) — czyta się jak zdanie
        // („pogrupuj i policz”). toMap z Integer::sum przyda się, gdy sumujesz coś innego niż jedynki.
    }

    // =================================================================================================
    // 7. ODWRACANIE MAPY
    // =================================================================================================

    /**
     * 7. Odwrócenie mapy: wartości stają się kluczami. Bezpieczne tylko, gdy wartości są unikalne —
     * w przeciwnym razie potrzebna funkcja scalająca.
     */
    static void invertMap() {
        section("7. Odwracanie mapy (klucz ↔ wartość)");

        Map<String, String> skuToName = SampleData.products().stream()
                .filter(p -> p.category() == Category.KSIAZKI)
                .collect(Collectors.toMap(Product::sku, Product::name, (a, b) -> a, TreeMap::new));

        // Strumień wpisów (entrySet) → nowa mapa: klucz = stara wartość, wartość = stary klucz.
        Map<String, String> nameToSku = skuToName.entrySet().stream()   // entrySet = zbiór wpisów (par)
                .collect(Collectors.toMap(
                        Map.Entry::getValue,          // getValue = pobierz wartość → nowy klucz
                        Map.Entry::getKey,            // getKey = pobierz klucz → nowa wartość
                        (a, b) -> a, TreeMap::new));
        show("nazwa → sku", nameToSku);
        // WYNIK: nazwa → sku → {Czysty kod=KSI-001, Java. Podstawy=KSI-002, Wzorce projektowe=KSI-003}

        // Wartości się POWTARZAJĄ? Odwrócenie częstości słów (słowo → liczba) daje liczba → ...które słowa?
        Map<String, Integer> frequency = SampleData.words().stream()
                .collect(Collectors.toMap(w -> w, w -> 1, Integer::sum, TreeMap::new));
        expectThrows("odwrócenie bez merge", () -> frequency.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey)));
        // WYNIK: ✔ odwrócenie bez merge → rzucono IllegalStateException: Duplicate key 1 (attempted merging values enum and kolekcja)

        Map<Integer, String> wordsByCount = frequency.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey,
                        (a, b) -> a + ", " + b,       // zderzenie → sklej słowa
                        TreeMap::new));
        showEach("liczba wystąpień → słowa", wordsByCount);
        // WYNIK: liczba wystąpień → słowa (liczba kluczy: 3):
        // WYNIK: • 1 → enum, kolekcja, lambda, lista, mapa, optional, rekord
        // WYNIK: • 2 → stream
        // WYNIK: • 3 → java
        // DOBRA PRAKTYKA: przy wielu wartościach na klucz czytelniej jest groupingBy(Map.Entry::getValue, ...)
        // z mapping(Map.Entry::getKey, toList()) — dostaniesz listy zamiast sklejonych napisów (Streams11).
    }

    // =================================================================================================
    // 8. SORTOWANIE WPISÓW PO WARTOŚCI → LINKEDHASHMAP
    // =================================================================================================

    /**
     * 8. Mapa nie „sortuje się po wartościach”. Trzeba: wziąć wpisy, posortować strumień wpisów
     * i zebrać do LinkedHashMap, która ZAPAMIĘTA tę kolejność.
     */
    static void sortByValue() {
        section("8. Sortowanie po wartości → LinkedHashMap");

        Map<Department, Integer> salaryByDepartment = SampleData.employees().stream()
                .collect(Collectors.toMap(Employee::department, Employee::salary, Integer::sum,
                        () -> new EnumMap<>(Department.class)));

        Map<Department, Integer> ranking = salaryByDepartment.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))   // comparingByValue = porównuj po wartości; reverseOrder = odwrotnie
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new));                                     // ← KLUCZOWE: zapamiętaj kolejność
        show("ranking działów (malejąco)", ranking);
        // WYNIK: ranking działów (malejąco) → {IT=53600, SPRZEDAZ=20100, MARKETING=17400, KSIEGOWOSC=8100, HR=7200}

        Map<Department, Integer> top2 = salaryByDepartment.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(2)                                                         // tylko dwa pierwsze
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
        show("TOP 2", top2);
        // WYNIK: TOP 2 → {IT=53600, SPRZEDAZ=20100}

        // PUŁAPKA: sorted(...) a potem collect(Collectors.toMap(getKey, getValue)) BEZ LinkedHashMap::new
        // → wynik trafia do HashMap i cała praca sortowania przepada (a z kluczami enum kolejność bywa losowa).
        // PUŁAPKA: TreeMap tu nie pomoże — sortuje po KLUCZACH, nie po wartościach.
        // DOBRA PRAKTYKA: czasem wystarczy List wpisów zamiast mapy: sorted(...).toList() — też zachowa kolejność.
    }

    // =================================================================================================
    // 9. TOUNMODIFIABLEMAP (JAVA 10+)
    // =================================================================================================

    /**
     * 9. toUnmodifiableMap zwraca mapę tylko do odczytu. Duplikaty i null traktuje tak samo surowo
     * jak toMap, a kolejności wpisów nie obiecuje wcale.
     */
    static void unmodifiableMap() {
        section("9. toUnmodifiableMap (Java 10+)");

        Map<String, Integer> stockBySku = SampleData.products().stream()
                .filter(p -> p.category() == Category.DOM)
                .collect(Collectors.toUnmodifiableMap(Product::sku, Product::stock));   // toUnmodifiableMap = do mapy niemodyfikowalnej
        show("get(\"DOM-002\")", stockBySku.get("DOM-002"));
        // WYNIK: get("DOM-002") → 18
        expectThrows("put do toUnmodifiableMap", () -> stockBySku.put("DOM-003", 1));
        // WYNIK: ✔ put do toUnmodifiableMap → rzucono UnsupportedOperationException: (brak komunikatu)

        expectThrows("duplikat w toUnmodifiableMap", () -> SampleData.employees().stream()
                .collect(Collectors.toUnmodifiableMap(Employee::department, Employee::name)));
        // WYNIK: ✔ duplikat w toUnmodifiableMap → rzucono IllegalStateException: Duplicate key IT (attempted merging values Anna Nowak and Piotr Kowalski)

        // Wersja z merge istnieje: toUnmodifiableMap(k, v, merge). Wersji z dostawcą mapy — NIE ma.
        Map<Department, Integer> headcount = SampleData.employees().stream()
                .collect(Collectors.toUnmodifiableMap(Employee::department, e -> 1, Integer::sum));
        show("osób w IT", headcount.get(Department.IT));
        // WYNIK: osób w IT → 4

        // PUŁAPKA: kolejność w toUnmodifiableMap (jak w Map.of) jest celowo LOSOWANA przy każdym starcie JVM.
        // Nie wypisuj takiej mapy „w całości” w testach ani raportach. Potrzebujesz porządku i ochrony
        // przed zmianą? Collections.unmodifiableMap(new TreeMap<>(mapa)) albo najpierw TreeMap, potem kopia.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • toMap(k, v) — HashMap; duplikat klucza → IllegalStateException („Duplicate key ... attempted merging ...”).
     *   • toMap(k, v, merge) — merge(stara, nowa): (a, b) -> a pierwszy, (a, b) -> b ostatni, Integer::sum, Math::max,
     *     BinaryOperator.maxBy(comparator) — cały obiekt z największą wartością.
     *   • toMap(k, v, merge, TreeMap::new / LinkedHashMap::new / () -> new EnumMap<>(X.class)) — wybór mapy.
     *   • Function.identity() = element jest wartością (indeks: sku → produkt).
     *   • Wartość null → NullPointerException bez komunikatu. Zamień (orElse) albo odfiltruj.
     *   • Częstość: groupingBy(w -> w, counting()) → Long; toMap(w -> w, w -> 1, Integer::sum) → Integer.
     *   • Odwracanie: entrySet().stream() + toMap(getValue, getKey, merge).
     *   • Sortowanie po wartości: entrySet().stream().sorted(comparingByValue(...)) + LinkedHashMap::new.
     *   • toUnmodifiableMap (Java 10+) — tylko odczyt, kolejność losowa. Nie wypisuj HashMap z kluczami enum.
     *
     * PYTANIA KONTROLNE:
     *   1. Co się stanie, gdy w toMap(k, v) dwa elementy dadzą ten sam klucz? Jak to naprawić na dwa sposoby?
     *   2. Co wypisze:
     *        System.out.println(Stream.of("a", "bb", "cc")
     *                .collect(Collectors.toMap(String::length, s -> s, (x, y) -> x + y, TreeMap::new)));  ?
     *   3. ZNAJDŹ BŁĄD:  Map<String, String> emails = customers.stream()
     *                        .collect(Collectors.toMap(Customer::name, Customer::email));
     *   4. ZNAJDŹ BŁĄD:  map.entrySet().stream().sorted(Map.Entry.comparingByValue())
     *                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));   // „posortowana mapa”
     *   5. Co wypisze:  Map<Long, Customer> byId = ...;  System.out.println(byId.get(1));  ?
     *   6. Czym różni się wynik toMap(w -> w, w -> 1, Integer::sum) od groupingBy(w -> w, counting())?
     *   7. Dlaczego nie wypisujemy w całości HashMap z kluczami enum ani wyniku toUnmodifiableMap?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: id → imię klienta", expectedNamesById(), () -> exercise1(SampleData.customers()));
        Check.equal("ćw. 2: miasto → liczba klientów", expectedCityCounts(), () -> exercise2(SampleData.customers()));
        Check.equal("ćw. 3: kategoria → najwyższa cena", expectedMaxPrices(), () -> exercise3(SampleData.products()));
        Check.equal("ćw. 4: klient → liczba pozycji zamówień", expectedLinesPerCustomer(),
                () -> exercise4(SampleData.orders()));
        Check.equal("ćw. 5: ranking kategorii po sztukach", "{SPOZYWCZE=420, ODZIEZ=92, ELEKTRONIKA=36, KSIAZKI=27, DOM=20}",
                () -> exercise5(SampleData.products()).toString());
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", expectedNamesById(), () -> solution1(SampleData.customers()));
        Check.equal("ćw. 2 (wzorzec)", expectedCityCounts(), () -> solution2(SampleData.customers()));
        Check.equal("ćw. 3 (wzorzec)", expectedMaxPrices(), () -> solution3(SampleData.products()));
        Check.equal("ćw. 4 (wzorzec)", expectedLinesPerCustomer(), () -> solution4(SampleData.orders()));
        Check.equal("ćw. 5 (wzorzec)", "{SPOZYWCZE=420, ODZIEZ=92, ELEKTRONIKA=36, KSIAZKI=27, DOM=20}",
                () -> solution5(SampleData.products()).toString());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    private static Map<Long, String> expectedNamesById() {
        return new TreeMap<>(Map.of(1L, "Jan Kowalski", 2L, "Maria Nowak", 3L, "Adam Mazur", 4L, "Zofia Krawczyk",
                5L, "Ola Pawlak", 6L, "Marek Król", 7L, "Ewa Lis"));
    }

    private static Map<String, Integer> expectedCityCounts() {
        return new TreeMap<>(Map.of("Gdańsk", 1, "Kraków", 2, "Poznań", 1, "Warszawa", 2, "Wrocław", 1));
    }

    private static Map<Category, BigDecimal> expectedMaxPrices() {
        Map<Category, BigDecimal> expected = new EnumMap<>(Category.class);
        expected.put(Category.ELEKTRONIKA, new BigDecimal("5499.99"));
        expected.put(Category.SPOZYWCZE, new BigDecimal("64.99"));
        expected.put(Category.KSIAZKI, new BigDecimal("129.00"));
        expected.put(Category.ODZIEZ, new BigDecimal("459.00"));
        expected.put(Category.DOM, new BigDecimal("1899.00"));
        return expected;
    }

    private static Map<String, Integer> expectedLinesPerCustomer() {
        return new TreeMap<>(Map.of("Adam Mazur", 1, "Jan Kowalski", 7, "Marek Król", 2, "Maria Nowak", 3,
                "Ola Pawlak", 2, "Zofia Krawczyk", 4));
    }

    /**
     * ĆWICZENIE 1 (łatwe): zbuduj mapę id klienta → imię i nazwisko.
     * Podpowiedź: toMap(Customer::id, Customer::name) — id są unikalne, więc merge nie jest potrzebny.
     */
    static Map<Long, String> exercise1(List<Customer> customers) {
        // TODO: twoje rozwiązanie
        return Map.of();
    }

    /**
     * ĆWICZENIE 2 (łatwe): miasto → liczba klientów z tego miasta, użyj toMap (nie groupingBy).
     * Podpowiedź: każdy klient daje parę (miasto → 1), a przy zderzeniu Integer::sum.
     */
    static Map<String, Integer> exercise2(List<Customer> customers) {
        // TODO: twoje rozwiązanie
        return Map.of();
    }

    /**
     * ĆWICZENIE 3 (średnie): kategoria → cena NAJDROŻSZEGO produktu w tej kategorii (BigDecimal), w EnumMap.
     * Podpowiedź: toMap(Product::category, Product::price, BigDecimal::max, () -> new EnumMap<>(Category.class)).
     * BigDecimal::max porównuje przez compareTo — dokładnie tak, jak trzeba dla pieniędzy.
     */
    static Map<Category, BigDecimal> exercise3(List<Product> products) {
        // TODO: twoje rozwiązanie
        return Map.of();
    }

    /**
     * ĆWICZENIE 4 (średnie): PRZEPISZ pętlę z merge na jeden strumień z toMap.
     * <pre>{@code
     * Map<String, Integer> result = new HashMap<>();
     * for (Order o : orders) {
     *     result.merge(o.customer().name(), o.lines().size(), Integer::sum);
     * }
     * return result;
     * }</pre>
     * Podpowiedź: toMap(o -> o.customer().name(), o -> o.lines().size(), Integer::sum).
     */
    static Map<String, Integer> exercise4(List<Order> orders) {
        // TODO: twoje rozwiązanie
        return Map.of();
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): ranking kategorii według łącznej liczby sztuk w magazynie, MALEJĄCO,
     * jako LinkedHashMap (kolejność ma znaczenie — sprawdzamy toString()).
     * Oczekiwany wynik: {SPOZYWCZE=420, ODZIEZ=92, ELEKTRONIKA=36, KSIAZKI=27, DOM=20}.
     * Podpowiedź: krok 1 — toMap(category, stock, Integer::sum); krok 2 — entrySet().stream()
     * .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())) → toMap(..., LinkedHashMap::new).
     */
    static LinkedHashMap<Category, Integer> exercise5(List<Product> products) {
        // TODO: twoje rozwiązanie
        return new LinkedHashMap<>();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static Map<Long, String> solution1(List<Customer> customers) {
        return customers.stream()
                .collect(Collectors.toMap(Customer::id, Customer::name));
    }

    static Map<String, Integer> solution2(List<Customer> customers) {
        return customers.stream()
                .collect(Collectors.toMap(Customer::city, c -> 1, Integer::sum, TreeMap::new));
    }

    static Map<Category, BigDecimal> solution3(List<Product> products) {
        return products.stream()
                .collect(Collectors.toMap(Product::category, Product::price, BigDecimal::max,
                        () -> new EnumMap<>(Category.class)));
    }

    static Map<String, Integer> solution4(List<Order> orders) {
        return orders.stream()
                .collect(Collectors.toMap(o -> o.customer().name(), o -> o.lines().size(), Integer::sum));
    }

    static LinkedHashMap<Category, Integer> solution5(List<Product> products) {
        Map<Category, Integer> stockByCategory = products.stream()
                .collect(Collectors.toMap(Product::category, Product::stock, Integer::sum,
                        () -> new EnumMap<>(Category.class)));
        return stockByCategory.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. IllegalStateException: Duplicate key ... (attempted merging values ... and ...). Naprawa: a) funkcja
     *      scalająca (np. (a, b) -> a albo Integer::sum), gdy chcesz jedną wartość na klucz; b) groupingBy,
     *      gdy chcesz wszystkie wartości dla klucza.
     *   2. {1=a, 2=bbcc}  — "bb" i "cc" mają ten sam klucz 2, więc merge skleja je w "bbcc"; TreeMap sortuje klucze.
     *   3. Maria Nowak i Ola Pawlak mają email == null → NullPointerException (toMap nie przyjmuje null jako wartości).
     *      Naprawa: c -> c.findEmail().orElse("(brak)") albo filter(c -> c.email() != null).
     *   4. toMap(k, v) wrzuca wynik do HashMap, która nie pamięta kolejności — sortowanie przepada. Trzeba
     *      toMap(k, v, (a, b) -> a, LinkedHashMap::new).
     *   5. null — 1 to int, zapakowany w Integer, a klucze są typu Long. Integer.valueOf(1) nie równa się Long.valueOf(1).
     *      Poprawnie: byId.get(1L).
     *   6. Zawartość taka sama, ale wartości to Integer (toMap) kontra Long (counting) — equals takich map zwraca false.
     *      groupingBy + counting czyta się czytelniej („pogrupuj i policz”).
     *   7. HashMap z kluczami enum ma kolejność zależną od hashCode tożsamości (JVM nadaje go na nowo przy każdym
     *      starcie), a toUnmodifiableMap (jak Map.of)
     *      celowo losuje kolejność przy każdym starcie JVM. Wydruk byłby różny między uruchomieniami.
     */
    // </editor-fold>
}
