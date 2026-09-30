package t12_collections;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Customer;
import helpers.model.Employee;
import helpers.model.Product;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.NavigableSet;
import java.util.TreeSet;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Comparable i Comparator — własne reguły sortowania
 *        (comparable = porównywalny (sam ze sobą); comparator = obiekt porównujący (z zewnątrz))
 *
 * W SKRÓCIE:
 *   Comparable to kontrakt „ja, obiekt, wiem, jak porównać się z INNYM obiektem mojego typu” (jedna,
 *   „naturalna” reguła, metoda compareTo wewnątrz klasy). Comparator to ZEWNĘTRZNA reguła porównywania —
 *   możesz mieć ich dowolnie wiele dla tego samego typu (wg ceny, wg nazwy, malejąco, rosnąco...), bez
 *   zmieniania samej klasy.
 *
 * ANALOGIA: dwa sposoby ustawiania ludzi w kolejce.
 *   Comparable to sytuacja, w której KAŻDA osoba sama wie, gdzie stanąć względem innej (np. „jestem starszy,
 *   więc stoję przed tobą” — jedna, wbudowana reguła). Comparator to zewnętrzny organizator z KARTKĄ zasad —
 *   dziś ustawia „wg wzrostu”, jutro „wg alfabetu nazwisk” — ta sama grupa ludzi, różne reguły, bez pytania
 *   ich samych o zdanie.
 *
 * JAK TO DZIAŁA:
 *   compareTo(other) zwraca: ujemną liczbę (this < other), zero (this == other pod względem porządku),
 *   dodatnią liczbę (this > other). Comparator.comparing(Klucz::metoda) buduje Comparator z „ekstraktora”
 *   klucza. .thenComparing(...) dokłada kolejny poziom (przy remisie). .reversed() odwraca CAŁY łańcuch.
 *   Comparator.nullsFirst/nullsLast owija inny Comparator, dodając obsługę null.
 *
 * SŁÓWKA:
 *   natural order = naturalny porządek; contract = kontrakt (zbiór reguł do przestrzegania); ascending =
 *   rosnąco; descending = malejąco; key extractor = funkcja wyciągająca klucz do porównania; tie-break =
 *   rozstrzygnięcie remisu; overflow = przepełnienie (liczba wychodzi poza swój zakres).
 *
 * ZOBACZ TEŻ: t12_collections/Collections04Sets (TreeSet — nawigacja wg porządku), t12_collections/Collections05Maps
 *             (TreeMap — porządek kluczy), t12_collections/Collections06QueuesDeques (PriorityQueue —
 *             Comparator wg priorytetu), t16_streams/Streams05SortDistinctLimit (sorted() w strumieniach),
 *             t09_records/Records01Basics (equals w rekordach — kontrast z compareTo).
 * </pre>
 */
public class Collections07ComparableComparator {

    public static void main(String[] args) {
        title("Collections07 — Comparable i Comparator: własne reguły sortowania");

        comparableNaturalOrder();       // Comparable: naturalny porządek
        compareToVsEqualsContract();    // kontrakt compareTo a equals
        comparatorSingleKey();          // Comparator.comparing: pojedynczy klucz
        thenComparingMultiLevel();      // thenComparing: sortowanie wielopoziomowe
        reversedWholeChainPitfall();    // pułapka: reversed() na całym łańcuchu
        nullsFirstLastDemo();           // nullsFirst / nullsLast
        polishCollatorPitfall();        // sortowanie polskich napisów: Unicode kontra Collator
        subtractionOverflowPitfall();   // pułapka: komparator przez odejmowanie
        exercises();                     // ćwiczenia
    }

    // =================================================================================================
    // 1. Comparable — NATURALNY PORZĄDEK
    // =================================================================================================

    /** Version = wersja (np. semver uproszczony). Naturalny porządek: najpierw major, potem minor. */
    private record Version(int major, int minor) implements Comparable<Version> {
        @Override
        public int compareTo(Version other) {
            int byMajor = Integer.compare(this.major, other.major);
            if (byMajor != 0) {
                return byMajor;
            }
            return Integer.compare(this.minor, other.minor);
        }
    }

    /**
     * 1. compareTo zwraca: ujemną ({@code this < other}), zero (równe pod względem porządku), dodatnią
     * ({@code this > other}). List.sort(null) używa WŁAŚNIE Comparable — „naturalnego” porządku typu.
     */
    static void comparableNaturalOrder() {
        section("1. Comparable: naturalny porządek");

        show("compareTo — major różne (1.5 vs 2.0)", new Version(1, 5).compareTo(new Version(2, 0)));
        // WYNIK: compareTo — major różne (1.5 vs 2.0) → -1
        show("compareTo — major równe, minor różne (1.5 vs 1.2)", new Version(1, 5).compareTo(new Version(1, 2)));
        // WYNIK: compareTo — major równe, minor różne (1.5 vs 1.2) → 1
        show("compareTo — identyczne (1.5 vs 1.5)", new Version(1, 5).compareTo(new Version(1, 5)));
        // WYNIK: compareTo — identyczne (1.5 vs 1.5) → 0

        List<Version> versions = new ArrayList<>(List.of(new Version(1, 5), new Version(2, 0), new Version(1, 2)));
        versions.sort(null);   // null = użyj naturalnego porządku (Comparable), nie osobnego Comparatora
        show("posortowane wg naturalnego porządku (sort(null))", versions);
        // WYNIK: posortowane wg naturalnego porządku (sort(null)) → [Version[major=1, minor=2], Version[major=1, minor=5], Version[major=2, minor=0]]

        // DOBRA PRAKTYKA: implementuj Comparable tylko, gdy klasa ma JEDEN oczywisty, „naturalny” porządek
        //   (jak liczby, daty, wersje). Gdy potrzeba wielu różnych sortowań tego samego typu — Comparator
        //   (sekcja 3), nie kolejne przeciążenia compareTo (compareTo jest tylko JEDNO).
    }

    // =================================================================================================
    // 2. KONTRAKT compareTo: ZGODNOŚĆ Z equals
    // =================================================================================================

    /**
     * 2. TreeSet/TreeMap uznają dwa elementy za „ten sam” wpis, gdy compareTo zwraca 0 — NIEZALEŻNIE
     * od equals()! Jeśli Comparator/compareTo ignoruje część pól, można „zgubić” elementy, które wg
     * equals() są różne.
     */
    static void compareToVsEqualsContract() {
        section("2. Kontrakt: compareTo powinien być zgodny z equals");

        NavigableSet<Version> byMajorOnly = new TreeSet<>(Comparator.comparingInt(Version::major));   // ignoruje minor!
        byMajorOnly.add(new Version(1, 0));
        byMajorOnly.add(new Version(1, 9));   // ten sam major=1 → compareTo zwróci 0 → TreeSet uzna za DUPLIKAT

        show("TreeSet z Comparatorem porównującym TYLKO major — rozmiar", byMajorOnly.size());
        // WYNIK: TreeSet z Comparatorem porównującym TYLKO major — rozmiar → 1
        show("co zostało w zbiorze", byMajorOnly);
        // WYNIK: co zostało w zbiorze → [Version[major=1, minor=0]]

        // PUŁAPKA: new Version(1, 9) „zniknął” — mimo że equals() uznałby go za INNY obiekt niż
        //   new Version(1, 0) (różny minor). TreeSet/TreeMap NIE WOŁAJĄ equals()/hashCode() wcale —
        //   o unikalności decyduje WYŁĄCZNIE compareTo (albo Comparator) zwracający 0. Dlatego kontrakt
        //   compareTo „zgodny z equals” (compareTo == 0 dokładnie wtedy, gdy equals == true) jest ZALECANY —
        //   złamanie go nie jest błędem kompilacji, ale bywa źródłem bardzo mylących błędów.
    }

    // =================================================================================================
    // 3. Comparator.comparing — POJEDYNCZY KLUCZ
    // =================================================================================================

    /** 3. Comparator.comparing(Klucz::metoda) buduje Comparator z funkcji wyciągającej klucz porównania. */
    static void comparatorSingleKey() {
        section("3. Comparator.comparing: sortowanie wg jednego klucza");

        List<Product> byPrice = new ArrayList<>(SampleData.products());
        byPrice.sort(Comparator.comparing(Product::price));   // comparing = porównaj wg (klucza)

        List<String> namesByPrice = new ArrayList<>();
        for (Product p : byPrice) {
            namesByPrice.add(p.name());
        }
        show("wszystkie produkty posortowane wg ceny rosnąco", namesByPrice);
        // WYNIK: wszystkie produkty posortowane wg ceny rosnąco → [Czekolada gorzka, Oliwa z oliwek, T-shirt bawełniany, Kawa ziarnista 1kg, Czysty kod, Wzorce projektowe, Java. Podstawy, Lampka biurkowa, Słuchawki BT, Kurtka zimowa, Monitor 27 cali, Ekspres do kawy, Smartfon X, Laptop Pro 14]

        note("Java. Podstawy i Lampka biurkowa mają TĘ SAMĄ cenę (129.00) — sort jest STABILNY, więc "
                + "zachowują względną kolejność z listy wejściowej (Java. Podstawy było pierwsze).");
        // WYNIK: ℹ Java. Podstawy i Lampka biurkowa mają TĘ SAMĄ cenę (129.00) — sort jest STABILNY, więc zachowują względną kolejność z listy wejściowej (Java. Podstawy było pierwsze).
    }

    // =================================================================================================
    // 4. thenComparing — SORTOWANIE WIELOPOZIOMOWE
    // =================================================================================================

    /**
     * 4. thenComparing dokłada KOLEJNY poziom porównania, używany TYLKO przy remisie na poprzednim
     * poziomie. Tu: dział rosnąco (naturalny porządek enuma), a w ramach działu — pensja malejąco.
     */
    static void thenComparingMultiLevel() {
        section("4. thenComparing: dział rosnąco, w środku pensja malejąco");

        List<Employee> ranked = new ArrayList<>(SampleData.employees());
        ranked.sort(Comparator.comparing(Employee::department)
                .thenComparing(Comparator.comparingInt(Employee::salary).reversed()));   // reversed TYLKO na tym poziomie

        int rank = 1;
        for (Employee e : ranked) {
            System.out.println(rank + ". " + e);
            rank++;
        }
        // WYNIK: 1. Michał Lewandowski (IT, 17200 zł)
        // WYNIK: 2. Anna Nowak (IT, 14500 zł)
        // WYNIK: 3. Ewa Woźniak (IT, 12100 zł)
        // WYNIK: 4. Piotr Kowalski (IT, 9800 zł)
        // WYNIK: 5. Katarzyna Wiśniewska (HR, 7200 zł)
        // WYNIK: 6. Krzysztof Szymański (SPRZEDAZ, 11200 zł)
        // WYNIK: 7. Tomasz Wójcik (SPRZEDAZ, 8900 zł)
        // WYNIK: 8. Magdalena Kamińska (KSIEGOWOSC, 8100 zł)
        // WYNIK: 9. Paweł Dąbrowski (MARKETING, 9800 zł)
        // WYNIK: 10. Agnieszka Zielińska (MARKETING, 7600 zł)

        // JAK TO DZIAŁA: Comparator.comparing(Employee::department) porównuje wg naturalnego porządku
        //   enuma (ordinal — kolejność deklaracji: IT, HR, SPRZEDAZ, KSIEGOWOSC, MARKETING, LOGISTYKA).
        //   LOGISTYKA nie ma pracowników, więc w ogóle się nie pojawia.
    }

    // =================================================================================================
    // 5. PUŁAPKA: .reversed() na CAŁYM łańcuchu
    // =================================================================================================

    /**
     * 5. .reversed() wywołane na SKŁADANYM (thenComparing) Comparatorze odwraca WSZYSTKIE poziomy naraz,
     * nie tylko ten, na którym chcieliśmy odwrócić kolejność.
     */
    static void reversedWholeChainPitfall() {
        section("5. Pułapka: reversed() na całym łańcuchu");

        Comparator<Employee> byDeptThenSalary = Comparator.comparing(Employee::department)
                .thenComparingInt(Employee::salary);   // oba poziomy ROSNĄCO

        List<Employee> wrong = new ArrayList<>(SampleData.employees());
        wrong.sort(byDeptThenSalary.reversed());   // odwraca WSZYSTKO — i dział, i pensję!

        show("źle: .reversed() na całym łańcuchu — pierwszy w kolejności", wrong.get(0));
        // WYNIK: źle: .reversed() na całym łańcuchu — pierwszy w kolejności → Paweł Dąbrowski (MARKETING, 9800 zł)

        note("To NIE jest \"dział rosnąco, pensja malejąco\" — dział jest teraz też malejąco "
                + "(MARKETING przed IT). Poprawnie (sekcja 4): odwróć TYLKO pod-comparator pensji, "
                + "PRZED złączeniem przez thenComparing.");
        // WYNIK: ℹ To NIE jest "dział rosnąco, pensja malejąco" — dział jest teraz też malejąco (MARKETING przed IT). Poprawnie (sekcja 4): odwróć TYLKO pod-comparator pensji, PRZED złączeniem przez thenComparing.
    }

    // =================================================================================================
    // 6. nullsFirst / nullsLast
    // =================================================================================================

    /** 6. Comparator.nullsFirst/nullsLast OWIJA inny Comparator, dodając bezpieczną obsługę elementów null. */
    static void nullsFirstLastDemo() {
        section("6. Comparator.nullsFirst — klienci bez adresu e-mail na początku");

        List<Customer> byEmail = new ArrayList<>(SampleData.customers());
        byEmail.sort(Comparator.comparing(Customer::email, Comparator.nullsFirst(Comparator.naturalOrder())));

        for (Customer c : byEmail) {
            System.out.println(c.name() + " → " + (c.email() == null ? "(brak)" : c.email()));
        }
        // WYNIK: Maria Nowak → (brak)
        // WYNIK: Ola Pawlak → (brak)
        // WYNIK: Adam Mazur → adam.mazur@example.com
        // WYNIK: Ewa Lis → ewa.lis@example.com
        // WYNIK: Jan Kowalski → jan@example.com
        // WYNIK: Marek Król → marek@example.com
        // WYNIK: Zofia Krawczyk → zofia@example.com

        // PUŁAPKA: zwykły Comparator.naturalOrder() (bez nullsFirst/nullsLast) rzuca NullPointerException
        //   przy PIERWSZYM napotkanym null — Comparable.compareTo nie umie porównać się z null. Jeśli
        //   klucz sortowania MOŻE być null, zawsze owijaj go w nullsFirst albo nullsLast.
    }

    // =================================================================================================
    // 7. SORTOWANIE POLSKICH NAPISÓW: Unicode kontra Collator
    // =================================================================================================

    /**
     * 7. Domyślny porządek String (compareTo/naturalOrder) porównuje WARTOŚCI UNICODE. Wszystkie polskie
     * znaki diakrytyczne (ą, ć, ę, ł, ń, ó, ś, ź, ż) mają kod WYŻSZY niż każda zwykła litera A-Z, więc
     * w porządku Unicode zawsze lądują na samym końcu — niezależnie od tego, gdzie są w polskim alfabecie.
     */
    static void polishCollatorPitfall() {
        section("7. Pułapka: sortowanie polskich napisów — Unicode kontra Collator");

        List<String> polish = List.of("Zebra", "Śliwka", "Alfa", "Łania");

        List<String> byUnicode = new ArrayList<>(polish);
        byUnicode.sort(Comparator.naturalOrder());
        show("porządek Unicode (domyślny)", byUnicode);
        // WYNIK: porządek Unicode (domyślny) → [Alfa, Zebra, Łania, Śliwka]

        Collator polishCollator = Collator.getInstance(Locale.forLanguageTag("pl-PL"));   // Collator = porównywacz "po ludzku"
        List<String> byCollator = new ArrayList<>(polish);
        byCollator.sort(polishCollator::compare);
        show("porządek polski (Collator pl-PL)", byCollator);
        // WYNIK: porządek polski (Collator pl-PL) → [Alfa, Łania, Śliwka, Zebra]

        // PUŁAPKA: w porządku Unicode „Zebra” (Z) ląduje PRZED „Łania” (Ł) i „Śliwka” (Ś) — bo Z=90,
        //   a Ł/Ś mają kody dużo wyższe (powyżej wszystkich zwykłych liter A-Z). Dla polskiego alfabetu
        //   (A, Ą, B, C, Ć, D, ..., L, Ł, M, ..., S, Ś, T, ..., Z, Ź, Ż) to WYNIK NIEZGODNY z intuicją.
        //   Do sortowania „po polsku” (np. na liście do wydruku dla użytkownika) używaj
        //   Collator.getInstance(Locale.forLanguageTag("pl-PL")).
    }

    // =================================================================================================
    // 8. PUŁAPKA: komparator przez odejmowanie — PRZEPEŁNIENIE
    // =================================================================================================

    /**
     * 8. {@code (a, b) -> a - b} jako {@code Comparator<Integer>} „działa” dla typowych liczb, ale dla
     * wartości bliskich granicom int (MIN_VALUE/MAX_VALUE) różnica PRZEPEŁNIA zakres int — cicho, bez wyjątku.
     */
    static void subtractionOverflowPitfall() {
        section("8. Pułapka: komparator przez odejmowanie (przepełnienie)");

        Comparator<Integer> bySubtraction = (a, b) -> a - b;   // ŹLE

        int overflowed = bySubtraction.compare(Integer.MIN_VALUE, 1);
        show("bySubtraction.compare(MIN_VALUE, 1) — PRZEPEŁNIENIE!", overflowed);
        // WYNIK: bySubtraction.compare(MIN_VALUE, 1) — PRZEPEŁNIENIE! → 2147483647

        note("MIN_VALUE - 1 wychodzi poniżej zakresu int i \"zawija się\" na MAX_VALUE (liczbę DODATNIĄ) — "
                + "komparator twierdzi, że MIN_VALUE jest WIĘKSZY od 1. To fałsz.");
        // WYNIK: ℹ MIN_VALUE - 1 wychodzi poniżej zakresu int i "zawija się" na MAX_VALUE (liczbę DODATNIĄ) — komparator twierdzi, że MIN_VALUE jest WIĘKSZY od 1. To fałsz.

        List<Integer> broken = new ArrayList<>(List.of(Integer.MIN_VALUE, 1));
        broken.sort(bySubtraction);
        show("posortowane przez bySubtraction (błędne!)", broken);
        // WYNIK: posortowane przez bySubtraction (błędne!) → [1, -2147483648]

        Comparator<Integer> bySafeCompare = Integer::compare;   // DOBRZE — Integer.compare NIGDY nie przepełnia
        List<Integer> fixed = new ArrayList<>(List.of(Integer.MIN_VALUE, 1));
        fixed.sort(bySafeCompare);
        show("posortowane przez Integer::compare (poprawne)", fixed);
        // WYNIK: posortowane przez Integer::compare (poprawne) → [-2147483648, 1]

        // DOBRA PRAKTYKA: NIGDY nie porównuj liczb całkowitych odejmowaniem w komparatorze. Zawsze używaj
        //   Integer.compare(a, b) (albo Long.compare, Double.compare) — sprawdzone, bezpieczne, i wcale
        //   nie dłuższe do napisania niż (a, b) -> a - b.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Comparable.compareTo(other): ujemna → this < other; zero → równe; dodatnia → this > other.
     *     JEDNA, „naturalna” reguła wbudowana w klasę. List.sort(null) używa jej wprost.
     *   • Comparator: reguła Z ZEWNĄTRZ, może być ich wiele dla tego samego typu. comparing/comparingInt
     *     buduje z „ekstraktora” klucza; thenComparing dokłada poziom przy remisie.
     *   • TreeSet/TreeMap: unikalność wg compareTo == 0, NIE wg equals(). Comparator ignorujący pole może
     *     „zgubić” elementy różne wg equals().
     *   • .reversed() na SKŁADANYM Comparatorze (po thenComparing) odwraca WSZYSTKIE poziomy — żeby
     *     odwrócić TYLKO jeden poziom, odwróć jego pod-comparator PRZED złączeniem.
     *   • Comparator.nullsFirst/nullsLast — bezpieczne sortowanie z elementami null (zwykły naturalOrder
     *     rzuca NPE na null).
     *   • String.compareTo/naturalOrder = porządek UNICODE, nie polski alfabet — polskie znaki
     *     diakrytyczne zawsze lądują po Z. Do sortowania „po polsku”: Collator.getInstance(Locale.forLanguageTag("pl-PL")).
     *   • (a, b) -> a - b w komparatorze PRZEPEŁNIA dla wartości bliskich MIN_VALUE/MAX_VALUE —
     *     zawsze używaj Integer.compare(a, b).
     *
     * PYTANIA KONTROLNE:
     *   1. Co oznaczają wartości ujemna/zero/dodatnia zwracane przez compareTo?
     *   2. Co wypisze:
     *          Comparator<Integer> bySubtraction = (a, b) -> a - b;
     *          System.out.println(bySubtraction.compare(Integer.MIN_VALUE, 1));
     *      ?
     *   3. ZNAJDŹ BŁĄD (programista chce: dział rosnąco, pensja malejąco):
     *          Comparator<Employee> cmp = Comparator.comparing(Employee::department)
     *                  .thenComparingInt(Employee::salary);
     *          list.sort(cmp.reversed());
     *   4. Co wypisze:
     *          List<String> words = new ArrayList<>(List.of("Zebra", "Alfa"));
     *          words.sort(Comparator.naturalOrder());
     *          System.out.println(words);
     *      ?
     *   5. Dlaczego TreeSet może „zgubić” element, którego equals() mówi, że jest różny od innych w zbiorze?
     *   6. Czym różni się Comparator.nullsFirst(cmp) od samego cmp przy elemencie null?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: sortowanie wersji (Comparable)",
                List.of(new Version(1, 9), new Version(2, 0), new Version(2, 1)),
                () -> sortVersions(List.of(new Version(2, 1), new Version(1, 9), new Version(2, 0))));
        Check.equal("ćw. 2: wg kategorii, potem cena malejąco (thenComparing)",
                List.of("Laptop Pro 14", "Słuchawki BT", "Kawa ziarnista 1kg", "Czekolada gorzka"),
                () -> sortByCategoryThenPriceDesc(List.of(SampleData.productBySku("SPO-002"),
                        SampleData.productBySku("ELE-001"), SampleData.productBySku("SPO-001"),
                        SampleData.productBySku("ELE-003"))));
        Check.equal("ćw. 3: sortowanie po polsku (Collator)", List.of("Alfa", "Kot", "Łania", "Śliwka", "Zebra"),
                () -> sortPolish(List.of("Zebra", "Śliwka", "Alfa", "Łania", "Kot")));
        Check.equal("ćw. 4: bezpieczne sortowanie liczb (PRZEPISZ na Integer.compare)",
                List.of(Integer.MIN_VALUE, -3, 0, 5, Integer.MAX_VALUE),
                () -> sortSafely(List.of(Integer.MIN_VALUE, 5, -3, Integer.MAX_VALUE, 0)));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of(new Version(1, 9), new Version(2, 0), new Version(2, 1)),
                () -> solutionSortVersions(List.of(new Version(2, 1), new Version(1, 9), new Version(2, 0))));
        Check.equal("ćw. 2 (wzorzec)", List.of("Laptop Pro 14", "Słuchawki BT", "Kawa ziarnista 1kg", "Czekolada gorzka"),
                () -> solutionSortByCategoryThenPriceDesc(List.of(SampleData.productBySku("SPO-002"),
                        SampleData.productBySku("ELE-001"), SampleData.productBySku("SPO-001"),
                        SampleData.productBySku("ELE-003"))));
        Check.equal("ćw. 3 (wzorzec)", List.of("Alfa", "Kot", "Łania", "Śliwka", "Zebra"),
                () -> solutionSortPolish(List.of("Zebra", "Śliwka", "Alfa", "Łania", "Kot")));
        Check.equal("ćw. 4 (wzorzec)", List.of(Integer.MIN_VALUE, -3, 0, 5, Integer.MAX_VALUE),
                () -> solutionSortSafely(List.of(Integer.MIN_VALUE, 5, -3, Integer.MAX_VALUE, 0)));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć KOPIĘ listy versions posortowaną wg naturalnego porządku (Comparable).
     * Podpowiedź: skopiuj listę, wywołaj sort(null) — sekcja 1.
     */
    static List<Version> sortVersions(List<Version> versions) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć nazwy produktów posortowanych wg kategorii (rosnąco), a w ramach
     * kategorii — wg ceny malejąco. Podpowiedź: {@code Comparator.comparing(Product::category)
     * .thenComparing(Comparator.comparing(Product::price).reversed())} — sekcja 4 i 5.
     */
    static List<String> sortByCategoryThenPriceDesc(List<Product> products) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 3 (średnie): posortuj słowa „po polsku” (Collator, nie Unicode). Podpowiedź:
     * {@code Collator.getInstance(Locale.forLanguageTag("pl-PL"))}, potem {@code sort(collator::compare)} —
     * sekcja 7.
     */
    static List<String> sortPolish(List<String> words) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 4 (trudniejsze, PRZEPISZ): poniższy komparator wygląda niewinnie, ale przepełnia dla
     * wartości bliskich granicom int:
     * <pre>{@code
     * Comparator<Integer> bySubtraction = (a, b) -> a - b;
     * List<Integer> copy = new ArrayList<>(numbers);
     * copy.sort(bySubtraction);
     * return copy;
     * }</pre>
     * Przepisz to, używając {@code Integer::compare} zamiast odejmowania — sekcja 8.
     */
    static List<Integer> sortSafely(List<Integer> numbers) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<Version> solutionSortVersions(List<Version> versions) {
        List<Version> copy = new ArrayList<>(versions);
        copy.sort(null);
        return copy;
    }

    static List<String> solutionSortByCategoryThenPriceDesc(List<Product> products) {
        List<Product> copy = new ArrayList<>(products);
        copy.sort(Comparator.comparing(Product::category).thenComparing(Comparator.comparing(Product::price).reversed()));
        List<String> names = new ArrayList<>();
        for (Product p : copy) {
            names.add(p.name());
        }
        return names;
    }

    static List<String> solutionSortPolish(List<String> words) {
        Collator collator = Collator.getInstance(Locale.forLanguageTag("pl-PL"));
        List<String> copy = new ArrayList<>(words);
        copy.sort(collator::compare);
        return copy;
    }

    static List<Integer> solutionSortSafely(List<Integer> numbers) {
        List<Integer> copy = new ArrayList<>(numbers);
        copy.sort(Integer::compare);
        return copy;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Ujemna oznacza „this jest mniejszy niż other”, zero — „równe pod względem porządku”, dodatnia —
     *      „this jest większy niż other”. Dokładna wartość (poza znakiem) nie ma znaczenia.
     *   2. „2147483647” — Integer.MIN_VALUE - 1 przepełnia zakres int i zawija się na Integer.MAX_VALUE.
     *   3. .reversed() wywołane na CAŁYM złożonym Comparatorze (po thenComparingInt) odwraca OBA poziomy —
     *      dział jest teraz też malejąco, nie tylko pensja. Poprawka: odwrócić TYLKO pod-comparator pensji
     *      przed złączeniem: comparing(dept).thenComparing(comparingInt(salary).reversed()).
     *   4. „[Alfa, Zebra]” — oba słowa to zwykłe litery ASCII, więc porządek Unicode pokrywa się tu ze
     *      zwykłym alfabetem: A przed Z.
     *   5. Bo TreeSet o unikalności decyduje WYŁĄCZNIE przez compareTo (albo Comparator) zwracający 0 —
     *      nigdy nie woła equals()/hashCode(). Jeśli reguła porównania ignoruje jakieś pole, dwa obiekty
     *      różne wg equals() mogą mieć compareTo == 0 i zostać potraktowane jako duplikat.
     *   6. Zwykły cmp (np. naturalOrder()) rzuca NullPointerException, gdy porównywany element to null —
     *      Comparable.compareTo nie umie się porównać z null. nullsFirst(cmp)/nullsLast(cmp) obsługują ten
     *      przypadek bezpiecznie, umieszczając null-e na początku albo końcu, i delegują do cmp tylko dla
     *      pary elementów, które NIE są null.
     */
    // </editor-fold>
}
