package t11_generics;

import helpers.Check;

import java.util.ArrayList;
import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Po co generyki — od surowych typów (Object + rzutowanie) do {@code List<String>}
 *        (generic = generyczny; type parameter = parametr typu; raw type = surowy typ; cast = rzutowanie)
 *
 * W SKRÓCIE:
 *   Przed Javą 5 lista przechowywała Object — można było wrzucić wszystko, a przy wyjmowaniu trzeba było rzutować
 *   i liczyć, że się uda (inaczej ClassCastException w działaniu programu). Generyki przenoszą tę kontrolę na
 *   KOMPILATOR: {@code List<String>} przyjmie tylko String, a get(0) zwróci String bez rzutowania. Błąd typu wychodzi
 *   podczas kompilacji, nie u użytkownika.
 *
 * ANALOGIA: pojemniki z etykietą.
 *   Surowa lista to karton bez opisu — wrzucasz, co chcesz, a przy wyjmowaniu zgadujesz, co to jest.
 *   {@code List<String>} to pojemnik z napisem „tylko napisy”: nic innego się nie zmieści, a wyjmując wiesz, co masz.
 *
 * JAK TO DZIAŁA:
 *   List raw = new ArrayList();                 ← surowy typ: elementy to Object
 *   String s = (String) raw.get(0);             ← rzutowanie; zły typ → ClassCastException (w działaniu)
 *   List{@code <String>} names = new ArrayList{@code <>}();   ← diament {@code <>}: kompilator sam dopisze String (Java 7+)
 *   names.add(42);                              ← BŁĄD KOMPILACJI — a o to chodzi
 *   String first = names.get(0);                ← bez rzutowania
 *
 * SŁÓWKA:
 *   generic = generyczny (ogólny, sparametryzowany typem); type parameter = parametr typu (T w Box{@code <T>});
 *   type argument = argument typu (String w Box{@code <String>}); raw type = surowy typ (List bez {@code <>});
 *   diamond = diament ({@code <>}); cast = rzutowanie; box = pudełko; unchecked = niesprawdzony; element = element.
 *
 * ZOBACZ TEŻ: t11_generics/Generics02Classes (własne klasy generyczne), t01_basics/Basics06Wrappers (Integer, autoboxing),
 *             t12_collections/Collections02Lists (listy), t01_basics/Basics05Casting (rzutowanie).
 * </pre>
 */
public class Generics01Why {

    /** ObjectBox = pudełko na Object (stary styl). Przyjmie wszystko — i to jest problem. */
    static final class ObjectBox {
        private Object value;

        ObjectBox(Object value) {
            this.value = value;
        }

        Object get() {
            return value;
        }

        void set(Object value) {
            this.value = value;
        }
    }

    /**
     * Box = pudełko generyczne. T (type = typ) to PARAMETR TYPU — „dziura” na typ, którą wypełni użytkownik klasy:
     * Box{@code <String>}, Box{@code <Integer>}... Wszędzie w klasie T zachowuje się jak ten konkretny typ.
     */
    static final class Box<T> {
        private T value;

        Box(T value) {
            this.value = value;
        }

        T get() {
            return value;
        }

        void set(T value) {
            this.value = value;
        }
    }

    public static void main(String[] args) {
        title("Generics01 — po co generyki");

        rawTypesProblem();          // raw types problem = problem surowych typów
        genericList();              // generic list = lista generyczna
        objectBoxVsGenericBox();    // Object box vs generic box = pudełko Object kontra generyczne
        diamondAndVar();            // diamond and var = diament i var
        onlyReferenceTypes();       // only reference types = tylko typy referencyjne
        removePitfall();            // remove pitfall = pułapka remove
        naming();                   // naming = nazewnictwo
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PROBLEM SUROWYCH TYPÓW
    // =================================================================================================

    /**
     * 1. PRZED (Java 1.4): lista bez parametru typu. Kompilator nie wie, co w niej jest — przepuszcza „wszystko”.
     * {@code @SuppressWarnings} wycisza ostrzeżenia kompilatora (rawtypes, unchecked) TYLKO tutaj, bo to celowo zły przykład.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static void rawTypesProblem() {
        section("1. PRZED: surowy typ List");

        List names = new ArrayList();            // surowy typ (raw type) — ostrzeżenie kompilatora
        names.add("Ala");
        names.add("Olek");
        names.add(42);                           // pomyłka — kompilator milczy
        show("rozmiar", names.size());
        // WYNIK: rozmiar → 3

        String first = (String) names.get(0);    // rzutowanie potrzebne przy każdym odczycie
        show("pierwszy", first);
        // WYNIK: pierwszy → Ala

        expectThrows("(String) names.get(2)", () -> System.out.println(((String) names.get(2)).length()));
        // WYNIK: ✔ (String) names.get(2) → rzucono ClassCastException: class java.lang.Integer cannot be cast to class java.lang.String (java.lang.Integer and java.lang.String are in module java.base of loader 'bootstrap')

        // PUŁAPKA: błąd („42” w liście napisów) powstał w jednym miejscu, a wybuchł w innym — i dopiero w działaniu
        //   programu. W dużym projekcie znalezienie, KTO wrzucił liczbę, bywa bardzo trudne.
    }

    // =================================================================================================
    // 2. LISTA GENERYCZNA
    // =================================================================================================

    /** 2. PO: {@code List<String>} — kompilator pilnuje typu przy dodawaniu, a odczyt nie wymaga rzutowania. */
    static void genericList() {
        section("2. PO: List<String>");

        List<String> names = new ArrayList<>();
        names.add("Ala");
        names.add("Olek");
        // names.add(42);                        ← BŁĄD KOMPILACJI: incompatible types: int cannot be converted to String

        String first = names.get(0);             // bez rzutowania
        show("pierwszy, wielkimi literami", first.toUpperCase());
        // WYNIK: pierwszy, wielkimi literami → ALA

        int totalLength = 0;
        for (String n : names) {                 // pętla od razu zna typ elementów
            totalLength += n.length();
        }
        show("łączna długość", totalLength);
        // WYNIK: łączna długość → 7

        // DOBRA PRAKTYKA: nigdy nie używaj surowych typów (List, Map bez <...>) w nowym kodzie. IntelliJ podkreśla
        //   je jako ostrzeżenie „Raw use of parameterized class”.
    }

    // =================================================================================================
    // 3. PORÓWNANIE: pudełko Object kontra pudełko generyczne
    // =================================================================================================

    /** 3. Ta sama klasa napisana raz „na Object”, raz z parametrem T. Różnica: kto pilnuje typu — Ty czy kompilator. */
    static void objectBoxVsGenericBox() {
        section("3. ObjectBox kontra Box<T>");

        ObjectBox oldBox = new ObjectBox("tekst");
        oldBox.set(3.14);                                      // pomyłka — przechodzi
        expectThrows("(String) oldBox.get()", () -> System.out.println((String) oldBox.get()));
        // WYNIK: ✔ (String) oldBox.get() → rzucono ClassCastException: class java.lang.Double cannot be cast to class java.lang.String (java.lang.Double and java.lang.String are in module java.base of loader 'bootstrap')

        Box<String> box = new Box<>("tekst");
        // box.set(3.14);                                      ← BŁĄD KOMPILACJI: double cannot be converted to String
        box.set("nowy tekst");
        show("box.get().length()", box.get().length());
        // WYNIK: box.get().length() → 10

        Box<Integer> numberBox = new Box<>(7);                 // ta sama klasa, inny typ
        show("numberBox.get() * 6", numberBox.get() * 6);
        // WYNIK: numberBox.get() * 6 → 42
    }

    // =================================================================================================
    // 4. DIAMENT I var
    // =================================================================================================

    /** 4. Diament {@code <>} (Java 7+) — typ po prawej wywnioskuje kompilator z lewej. var (Java 10+) — odwrotnie. */
    static void diamondAndVar() {
        section("4. Diament <> i var");

        List<String> a = new ArrayList<String>();   // przed Javą 7 — typ powtórzony dwa razy
        List<String> b = new ArrayList<>();         // diament — kompilator wie, że chodzi o String
        var c = new ArrayList<String>();            // var — typ zmiennej z prawej strony (ArrayList<String>)
        a.add("x");
        b.add("y");
        c.add("z");
        show("a + b + c", a.get(0) + b.get(0) + c.get(0));
        // WYNIK: a + b + c → xyz

        // PUŁAPKA: var c = new ArrayList<>();  (var + diament) daje ArrayList<Object> — tracisz kontrolę typu.
        //   Przy var podawaj typ po prawej: new ArrayList<String>().
    }

    // =================================================================================================
    // 5. TYLKO TYPY REFERENCYJNE
    // =================================================================================================

    /** 5. Parametr typu musi być klasą: {@code List<int>} nie istnieje — używamy {@code List<Integer>} (autoboxing). */
    static void onlyReferenceTypes() {
        section("5. List<Integer>, nie List<int>");

        // List<int> numbers;                   ← BŁĄD KOMPILACJI: unexpected type (required: reference, found: int)
        List<Integer> numbers = new ArrayList<>();
        numbers.add(5);                          // autoboxing: int 5 → Integer.valueOf(5)
        numbers.add(10);
        int sum = numbers.get(0) + numbers.get(1);   // unboxing: Integer → int
        show("suma", sum);
        // WYNIK: suma → 15

        // PUŁAPKA: w liście Integer może być null; przy unboxingu int x = list.get(i) → NullPointerException.
        // DOBRA PRAKTYKA: przy dużych ilościach liczb opakowania kosztują pamięć — tablica int[] albo IntStream (t16).
    }

    // =================================================================================================
    // 6. PUŁAPKA: remove(int) kontra remove(Object)
    // =================================================================================================

    /**
     * 6. {@code List<Integer>} ma DWIE metody remove: remove(int index) — usuń po INDEKSIE, i remove(Object o) — usuń
     * WARTOŚĆ. Dla liczby typu int Java wybiera remove(int index) (dokładniejsze dopasowanie, bez autoboxingu).
     */
    static void removePitfall() {
        section("6. Pułapka: list.remove(1) usuwa INDEKS 1");

        List<Integer> numbers = new ArrayList<>(List.of(10, 20, 30, 1));
        numbers.remove(1);                              // usuwa element o indeksie 1 (czyli 20)!
        show("po remove(1)", numbers);
        // WYNIK: po remove(1) → [10, 30, 1]

        numbers.remove(Integer.valueOf(1));             // usuwa WARTOŚĆ 1
        show("po remove(Integer.valueOf(1))", numbers);
        // WYNIK: po remove(Integer.valueOf(1)) → [10, 30]

        // Zasady wyboru przeciążonej metody: t05_methods/Methods02Overloading.
    }

    // =================================================================================================
    // 7. NAZEWNICTWO PARAMETRÓW TYPU
    // =================================================================================================

    /** 7. Parametry typu to zwykle jedna wielka litera — łatwo odróżnić je od nazw klas. */
    static void naming() {
        section("7. Konwencja nazw: T, E, K, V, R");

        note("T — type (typ); E — element (element kolekcji); K — key (klucz); V — value (wartość)");
        note("R — result (wynik funkcji); N — number (liczba); S, U — kolejne typy, gdy T jest już zajęte");
        // WYNIK:    ℹ T — type (typ); E — element (element kolekcji); K — key (klucz); V — value (wartość)
        // WYNIK:    ℹ R — result (wynik funkcji); N — number (liczba); S, U — kolejne typy, gdy T jest już zajęte
        // Przykłady z JDK: List<E>, Map<K, V>, Function<T, R>, Optional<T>.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Surowy typ (List bez <>) = elementy Object, rzutowanie, ClassCastException w działaniu → nie używać.
     *   • List<String>: kompilator pilnuje typu przy add; get zwraca String bez rzutowania.
     *   • class Box<T> { T value; T get(); } — T to parametr typu; Box<String> — argument typu.
     *   • Diament: new ArrayList<>() (Java 7+); var + diament → ArrayList<Object> (pułapka).
     *   • Tylko typy referencyjne: List<Integer>, nie List<int>; uwaga na null przy unboxingu.
     *   • List<Integer>.remove(1) usuwa INDEKS; wartość: remove(Integer.valueOf(1)).
     *   • Nazwy: T, E, K, V, R.
     *
     * PYTANIA KONTROLNE:
     *   1. Jaki problem rozwiązują generyki? Kiedy wychodzi błąd typu z nimi, a kiedy bez nich?
     *   2. Co wypisze:  List<Integer> l = new ArrayList<>(List.of(5, 6, 7)); l.remove(0); System.out.println(l);  ?
     *   3. ZNAJDŹ BŁĄD:  List<int> ages = new ArrayList<>();
     *   4. Czym różni się parametr typu od argumentu typu?
     *   5. Co wypisze:  var list = new ArrayList<>(); list.add(1); list.add("a"); System.out.println(list);  ? Dlaczego to działa?
     *   6. Co oznacza diament <> i od której wersji Javy jest dostępny?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** countStartingWithRaw = STARA wersja do przepisania w ćwiczeniu 3 (surowy typ + rzutowanie). */
    @SuppressWarnings("rawtypes")
    static int countStartingWithRaw(List words, String prefix) {
        int count = 0;
        for (Object o : words) {
            if (((String) o).startsWith(prefix)) {
                count++;
            }
        }
        return count;
    }

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: najdłuższe słowo", "programowanie", () -> exercise1(List.of("java", "programowanie", "kod")));
        Check.equal("ćw. 2a: usuń wartość 20", List.of(10, 30), () -> exercise2(List.of(10, 20, 30), 20));
        Check.equal("ćw. 2b: usuń wartość 1 (nie ma jej)", List.of(10, 20, 30), () -> exercise2(List.of(10, 20, 30), 1));
        Check.equal("ćw. 3: słowa na „pro”", 2, () -> exercise3(List.of("program", "java", "proces", "kod"), "pro"));
        Check.equal("ćw. 4: suma liczb Integer w mieszanej liście", 5,
                () -> exercise4(java.util.Arrays.asList(1, "2", 3.0, 4, null)));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "programowanie", () -> solution1(List.of("java", "programowanie", "kod")));
        Check.equal("ćw. 2a (wzorzec)", List.of(10, 30), () -> solution2(List.of(10, 20, 30), 20));
        Check.equal("ćw. 2b (wzorzec)", List.of(10, 20, 30), () -> solution2(List.of(10, 20, 30), 1));
        Check.equal("ćw. 3 (wzorzec)", 2, () -> solution3(List.of("program", "java", "proces", "kod"), "pro"));
        Check.equal("ćw. 4 (wzorzec)", 5, () -> solution4(java.util.Arrays.asList(1, "2", 3.0, 4, null)));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /** ĆWICZENIE 1 (łatwe): zwróć najdłuższe słowo z listy (bez rzutowania — lista jest generyczna). */
    static String exercise1(List<String> words) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć NOWĄ listę bez pierwszego wystąpienia WARTOŚCI value (lista wejściowa jest
     * niemodyfikowalna — zrób kopię new ArrayList<>(numbers)). Uwaga na pułapkę z sekcji 6!
     */
    static List<Integer> exercise2(List<Integer> numbers, int value) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /** ĆWICZENIE 3 (łatwe, „PRZEPISZ”): przepisz countStartingWithRaw na wersję z {@code List<String>} — bez rzutowania. */
    static int exercise3(List<String> words, String prefix) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): lista {@code List<Object>} ma elementy różnych typów (i null). Zsumuj TYLKO liczby Integer.
     * Podpowiedź: if (o instanceof Integer i) sum += i;  (instanceof ze wzorcem, Java 16+; null nie przejdzie instanceof).
     */
    static int exercise4(List<Object> mixed) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(List<String> words) {
        String longest = words.get(0);
        for (String w : words) {
            if (w.length() > longest.length()) {
                longest = w;
            }
        }
        return longest;
    }

    static List<Integer> solution2(List<Integer> numbers, int value) {
        List<Integer> copy = new ArrayList<>(numbers);
        copy.remove(Integer.valueOf(value));      // remove(Object) — usuwa wartość, nie indeks
        return copy;
    }

    static int solution3(List<String> words, String prefix) {
        int count = 0;
        for (String w : words) {
            if (w.startsWith(prefix)) {
                count++;
            }
        }
        return count;
    }

    static int solution4(List<Object> mixed) {
        int sum = 0;
        for (Object o : mixed) {
            if (o instanceof Integer i) {
                sum += i;
            }
        }
        return sum;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Brak kontroli typu elementów i konieczność rzutowania. Bez generyków zły typ wychodzi w DZIAŁANIU
     *      (ClassCastException, często daleko od przyczyny), z generykami — podczas KOMPILACJI.
     *   2. „[6, 7]” — remove(0) z argumentem int usuwa element o indeksie 0.
     *   3. Parametr typu musi być typem referencyjnym: List<Integer> ages = new ArrayList<>();
     *   4. Parametr typu to „zmienna” w definicji klasy (T w class Box<T>); argument typu to konkretny typ podany przy
     *      użyciu (String w Box<String>).
     *   5. „[1, a]” — var + diament daje ArrayList<Object>, więc kompilator przyjmie wszystko (tracimy kontrolę typu).
     *   6. Diament <> każe kompilatorowi wywnioskować argumenty typu z kontekstu (np. z typu zmiennej); Java 7.
     */
    // </editor-fold>
}
