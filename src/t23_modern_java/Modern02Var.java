package t23_modern_java;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Employee;
import helpers.model.Student;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: var — wnioskowanie typu zmiennych lokalnych (Java 10, JEP 286)
 *        (var = skrót od „variable”, zmienna; type inference = wnioskowanie typu)
 *
 * W SKRÓCIE:
 *   Od Javy 10 zamiast {@code Map<String, List<Integer>> m = new HashMap<String, List<Integer>>();} możesz napisać
 *   {@code var m = new HashMap<String, List<Integer>>();}. Kompilator sam ustala typ na podstawie wyrażenia po prawej
 *   stronie. To NIE jest typowanie dynamiczne (jak w JavaScripcie): typ jest ustalony raz, w czasie kompilacji,
 *   i nie da się go potem zmienić.
 *
 * ANALOGIA:
 *   Piszesz na pudełku z zakupami „zakupy” zamiast „pomidory, makaron, sok, mleko”. Zawartość nadal jest
 *   konkretna i nie zmieni się po zamknięciu pudełka — tylko etykieta jest krótsza, bo i tak widać, co do środka włożyłeś.
 *   Ale jeśli pudełko wręczył Ci ktoś inny i napisał na nim „rzeczy”, to nie wiesz, co w nim jest. Tak samo
 *   {@code var x = service.get();} — krótko, tylko że nikt nie wie, co to za typ.
 *
 * JAK TO DZIAŁA:
 *   1. Kompilator patrzy na prawą stronę przypisania i wstawia jej typ w miejsce słowa var (w bajtkodzie nie ma var).
 *   2. Wolno użyć var tylko dla zmiennych LOKALNYCH z inicjalizatorem:
 *        zwykła zmienna lokalna      var x = 5;
 *        pętla for                   for (var i = 0; ...; i++)
 *        pętla for-each              for (var e : lista)
 *        try-with-resources          try (var reader = ...)
 *        parametry lambdy (Java 11)  (var a, var b) -> a + b     (JEP 323)
 *   3. NIE wolno: pól klas, parametrów metod, typów zwracanych, zmiennych bez inicjalizatora,
 *      inicjalizatora null, inicjalizatora tablicowego {1, 2}, lambd i referencji do metod bez typu docelowego.
 *   4. var nie jest słowem kluczowym, tylko „zarezerwowaną nazwą typu” — dlatego stary kod ze zmienną o nazwie
 *      var nadal się kompiluje, a nazwy klasy var zabroniono.
 *
 * SŁÓWKA:
 *   infer = wnioskować; initializer = inicjalizator (wartość początkowa); local variable = zmienna lokalna;
 *   explicit = jawny; denotable = „da się go nazwać” (typ zapisywalny w kodzie); readability = czytelność;
 *   scope = zasięg; reserved = zarezerwowany; field = pole; parameter = parametr; resource = zasób.
 *
 * ZOBACZ TEŻ: t23_modern_java/Modern01Java8 (lambdy, strumienie), t02_controlflow/Control03Loops (pętle),
 *   t12_collections/Collections05Maps (długie typy generyczne), t23_modern_java/Modern06ApiAdditions (dalsze nowości)
 * </pre>
 */
public class Modern02Var {

    public static void main(String[] args) {
        title("Modern02 — var: wnioskowanie typu");

        basics();                // basics = podstawy
        whereAllowed();          // where allowed = gdzie wolno
        whereNotAllowed();       // where not allowed = gdzie nie wolno
        literalsAndNumbers();    // literals and numbers = literały i liczby
        diamondPitfall();        // diamond pitfall = pułapka rombu
        staticTypeStaysFixed();  // static type stays fixed = typ statyczny pozostaje stały
        readability();           // readability = czytelność
        notAKeyword();           // not a keyword = to nie słowo kluczowe
        nonDenotableTypes();     // non-denotable types = typy, których nie da się nazwać
        exercises();             // exercises = ćwiczenia
    }

    /** Pomocnicza: prosta nazwa klasy z czasu wykonania (getSimpleName = pobierz prostą nazwę). */
    static String typeOf(Object value) {   // type of = typ czegoś
        return value.getClass().getSimpleName();
    }

    // =================================================================================================
    // 1. PODSTAWY
    // =================================================================================================

    /**
     * 1. Podstawy: var skraca zapis, a typ pozostaje taki sam jak przy zapisie jawnym.
     * Zysk jest największy przy długich typach generycznych, w których typ powtarza się po obu stronach znaku =.
     */
    static void basics() {
        section("1. Podstawy");

        // PRZED (Java 9 i starsze): typ napisany dwa razy.
        Map<String, List<Integer>> explicitMap = new HashMap<String, List<Integer>>();   // explicit = jawny
        explicitMap.put("oceny", new ArrayList<Integer>(List.of(5, 4)));

        // PO (Java 10+): typ wnioskowany z prawej strony — ten sam typ, mniej szumu.
        var inferredMap = new HashMap<String, List<Integer>>();          // inferred = wywnioskowany
        inferredMap.put("oceny", new ArrayList<Integer>(List.of(5, 4)));
        show("jawnie", explicitMap);
        // WYNIK: jawnie → {oceny=[5, 4]}
        show("var", inferredMap);
        // WYNIK: var → {oceny=[5, 4]}
        show("ten sam typ?", explicitMap.getClass() == inferredMap.getClass());
        // WYNIK: ten sam typ? → true

        var text = "Ala ma kota";                    // text = tekst → String
        var number = 42;                              // → int
        var employees = SampleData.employees();       // → List<Employee> (typ zwracany przez metodę)
        show("var text → długość", text.length());
        // WYNIK: var text → długość → 11
        show("var number + 1", number + 1);
        // WYNIK: var number + 1 → 43
        show("var employees → pierwszy", employees.get(0));
        // WYNIK: var employees → pierwszy → Anna Nowak (IT, 14500 zł)

        // To nadal statyczne typowanie: IDE i kompilator znają typ, podpowiadają metody i łapią błędy.
        // BŁĄD KOMPILACJI (zakomentowany): var n = 5;  n = "tekst";   // typ int został ustalony na zawsze
        // DOBRA PRAKTYKA: var jest po to, by usunąć POWTÓRZENIE typu, a nie po to, by go ukryć. Jeśli po prawej
        //   stronie typ jest oczywisty (new X(), literał, metoda o wymownej nazwie) — var jest w porządku.
    }

    // =================================================================================================
    // 2. GDZIE WOLNO
    // =================================================================================================

    /**
     * 2. Gdzie wolno użyć var: zmienne lokalne, for, for-each, try-with-resources, parametry lambdy (Java 11).
     */
    static void whereAllowed() {
        section("2. Gdzie wolno użyć var");

        // a) zwykła zmienna lokalna z inicjalizatorem
        var greeting = "Cześć";                       // greeting = powitanie
        show("zmienna lokalna", greeting);
        // WYNIK: zmienna lokalna → Cześć

        // b) klasyczna pętla for — i ma typ int
        var sum = 0;
        for (var i = 1; i <= 4; i++) {
            sum += i;
        }
        show("for (var i ...)", sum);
        // WYNIK: for (var i ...) → 10

        // c) pętla for-each — typ elementu jest wnioskowany z kolekcji
        var names = new ArrayList<String>();
        for (var employee : SampleData.employees().subList(0, 3)) {      // sub list = podlista
            names.add(employee.name());
        }
        show("for-each", names);
        // WYNIK: for-each → [Anna Nowak, Piotr Kowalski, Katarzyna Wiśniewska]

        // d) for-each po mapie: entry (wpis) ma pełny, długi typ Map.Entry<String, Integer> — tu var naprawdę pomaga
        var scores = new TreeMap<String, Integer>(Map.of("Ala", 5, "Ola", 4));   // scores = wyniki
        var total = 0;
        for (var entry : scores.entrySet()) {
            total += entry.getValue();
        }
        show("suma z entrySet", total);
        // WYNIK: suma z entrySet → 9

        // e) try-with-resources: zasób zamykany automatycznie, typ wnioskowany (BufferedReader)
        try (var reader = new BufferedReader(new StringReader("pierwsza linia\ndruga linia"))) {
            show("try-with-resources", reader.readLine());
            // WYNIK: try-with-resources → pierwsza linia
        } catch (IOException e) {
            throw new UncheckedIOException(e);        // opakuj wyjątek sprawdzany w niesprawdzany
        }

        // f) parametry lambdy (Java 11, JEP 323). Po co, skoro lambda i tak ma wnioskowane typy? Żeby można było
        //    dopisać adnotację albo final do parametru bez podawania pełnego typu: (final var a, final var b) -> ...
        BiFunction<Integer, Integer, Integer> multiply = (final var a, final var b) -> a * b;   // multiply = pomnóż
        show("lambda (final var a, final var b)", multiply.apply(6, 7));
        // WYNIK: lambda (final var a, final var b) → 42

        // PUŁAPKA: w jednej liście parametrów lambdy nie wolno mieszać var z jawnym typem ani z pominięciem typu:
        //   (var a, b) -> ...  oraz  (var a, int b) -> ...  to błędy kompilacji. Wszystkie albo żaden.
        // DOBRA PRAKTYKA: w prostych lambdach zostaw (a, b) -> a * b; var w lambdzie ma sens tylko wtedy,
        //   gdy potrzebujesz adnotacji (np. @Nullable) przy parametrze.
    }

    // =================================================================================================
    // 3. GDZIE NIE WOLNO
    // =================================================================================================

    /**
     * 3. Gdzie var jest zabronione. Wszystkie poniższe przykłady są błędami kompilacji, więc stoją w komentarzach;
     * łączy je jedno: kompilator nie ma skąd wziąć typu albo typ musiałby być częścią „kontraktu” klasy.
     */
    static void whereNotAllowed() {
        section("3. Gdzie var jest zabronione");

        // 1. pole klasy:              private var count = 0;                   // typ pola to część API klasy
        // 2. parametr metody:         void print(var x) { }                    // sygnatura musi być jawna
        // 3. typ zwracany:            var compute() { return 1; }              // j.w.
        // 4. brak inicjalizatora:     var x;  x = 5;                           // nie ma z czego wnioskować
        // 5. inicjalizator null:      var x = null;                            // null nie ma typu
        // 6. tablicowy inicjalizator: var a = {1, 2, 3};                       // poprawnie: var a = new int[]{1, 2, 3};
        // 7. lambda bez typu:         var f = () -> 42;                        // jakiego interfejsu funkcyjnego?
        // 8. referencja do metody:    var g = String::length;                  // j.w.
        // 9. kilka zmiennych naraz:   var a = 1, b = 2;                        // „var” nie obsługuje listy deklaracji
        // 10. nawias tablicy:         var arr[] = new int[3];                  // zabronione
        // 11. odwołanie do siebie:    var self = self + 1;                     // zmienna nie istnieje w swoim inicjalizatorze
        // 12. nazwa klasy:            class var { }                            // var to zarezerwowana nazwa typu

        // Co WOLNO zamiast tego (kompiluje się):
        var array = new int[]{1, 2, 3};                                  // array = tablica
        show("var + new int[]{...}", array.length);
        // WYNIK: var + new int[]{...} → 3

        java.util.function.IntSupplier fortyTwo = () -> 42;              // typ docelowy jawny, więc lambda ma sens
        show("lambda z jawnym typem", fortyTwo.getAsInt());
        // WYNIK: lambda z jawnym typem → 42

        // Po co te zakazy? Dla pól, parametrów i wyników typ jest KONTRAKTEM: inne klasy polegają na nim,
        // więc musi być widoczny i stabilny. Zmienna lokalna żyje kilka linii — tam wnioskowanie jest bezpieczne.
        // DOBRA PRAKTYKA: nie próbuj „obchodzić” zakazów — jeśli chcesz var w polu, potrzebujesz jawnego typu.
        note("Zasada: var tylko dla zmiennych lokalnych z inicjalizatorem, którego typ da się ustalić.");
        // WYNIK: ℹ Zasada: var tylko dla zmiennych lokalnych z inicjalizatorem, którego typ da się ustalić.
    }

    // =================================================================================================
    // 4. LITERAŁY I LICZBY
    // =================================================================================================

    /**
     * 4. Literały liczbowe: var bierze typ literału, więc 1 to int, 1L to long, 1.0 to double.
     * To częsta pułapka — zapis {@code var x = 10;} to NIE „liczba dowolnego rodzaju”.
     */
    static void literalsAndNumbers() {
        section("4. Literały i liczby");

        var integer = 10;           // int
        var big = 10L;              // long
        var decimal = 10.0;         // double (decimal = dziesiętna)
        var small = 10.0f;          // float
        var letter = 'x';           // char (letter = litera)
        var flag = true;            // boolean
        show("10", typeOf(integer));
        // WYNIK: 10 → Integer
        show("10L", typeOf(big));
        // WYNIK: 10L → Long
        show("10.0", typeOf(decimal));
        // WYNIK: 10.0 → Double
        show("10.0f", typeOf(small));
        // WYNIK: 10.0f → Float
        show("'x'", typeOf(letter));
        // WYNIK: 'x' → Character
        show("true", typeOf(flag));
        // WYNIK: true → Boolean
        // (typeOf przyjmuje Object, więc int zostało zapakowane do Integer — dlatego nazwy klas opakowujących)

        // PUŁAPKA: var total = 0 to int. Dodawanie long operatorem += kompiluje się, ale po cichu obcina wynik,
        //   bo += zawiera ukryte rzutowanie (int) na typ zmiennej.
        var total = 0;                                                   // total = suma, typ int
        total += 3_000_000_000L;                                         // 3 miliardy nie mieści się w int
        show("var total = 0; total += 3 mld", total);
        // WYNIK: var total = 0; total += 3 mld → -1294967296
        var correct = 0L;                                                // correct = poprawna, typ long
        correct += 3_000_000_000L;
        show("var correct = 0L", correct);
        // WYNIK: var correct = 0L → 3000000000

        // PUŁAPKA: zmiana typu zmiennej po fakcie jest niemożliwa — „var x = 10; x = 10L;” to błąd (long → int
        //   traci dokładność). Zapisz od razu właściwy literał (0L, 1.0, 'a'), a gdy trzeba inny typ — rzutuj jawnie:
        //   var b = (byte) 1;  var s = (short) 1;
        var tiny = (byte) 1;                                             // tiny = malutki
        show("(byte) 1", typeOf(tiny));
        // WYNIK: (byte) 1 → Byte

        // PUŁAPKA: dzielenie całkowite. Z var łatwo zapomnieć, że obie strony są int.
        var points = 7;                                                  // points = punkty
        var count = 2;
        var average = points / count;                                    // 3, a nie 3.5
        show("7 / 2 przez var", average);
        // WYNIK: 7 / 2 przez var → 3
        // DOBRA PRAKTYKA: przy liczbach, gdzie typ ma znaczenie (long, double, float), albo użyj jawnego typu,
        //   albo zapisz literał z sufiksem (L, f, d) — wtedy typ widać gołym okiem.
    }

    // =================================================================================================
    // 5. PUŁAPKA ROMBU
    // =================================================================================================

    /**
     * 5. Var + diamond (operator rombu {@code <>}). Romb mówi „wywnioskuj parametry typu z lewej strony”,
     * a var mówi „wywnioskuj typ z prawej strony”. Gdy oba zdają się na drugą stronę, nie ma skąd
     * wziąć informacji i kompilator wybiera Object.
     */
    static void diamondPitfall() {
        section("5. Var + romb <> = ArrayList<Object>");

        // PUŁAPKA: var list = new ArrayList<>();  →  typ to ArrayList<Object>, a nie „ArrayList czegoś”.
        var mixed = new ArrayList<>();                                   // mixed = pomieszana
        mixed.add("tekst");
        mixed.add(123);                                                  // kompiluje się! Object przyjmie wszystko
        show("ArrayList<Object>", mixed);
        // WYNIK: ArrayList<Object> → [tekst, 123]
        // Chciałeś listę napisów? Kompilator tego nie wie — i nie złapie Twojego błędu:
        //   String first = mixed.get(0);   // BŁĄD KOMPILACJI: Object nie jest String (trzeba rzutować)
        String first = (String) mixed.get(0);
        show("po rzutowaniu", first);
        // WYNIK: po rzutowaniu → tekst
        expectThrows("rzutowanie Integer na String", () -> {
            String second = (String) mixed.get(1);
            show("nie dojdzie tutaj", second);
        });
        // WYNIK: ✔ rzutowanie Integer na String → rzucono ClassCastException: class java.lang.Integer cannot be cast to class java.lang.String (java.lang.Integer and java.lang.String are in module java.base of loader 'bootstrap')

        // POPRAWNIE — dwie drogi, wybierz jedną:
        var typedA = new ArrayList<String>();                            // typ jawny po prawej + var
        List<String> typedB = new ArrayList<>();                         // typ jawny po lewej + romb
        typedA.add("tylko napisy");
        typedB.add("tylko napisy");
        // typedA.add(123);   // BŁĄD KOMPILACJI — i bardzo dobrze, o to chodzi
        show("poprawnie A i B", typedA.equals(typedB));
        // WYNIK: poprawnie A i B → true

        // Drugi efekt uboczny: var trzyma typ KONKRETNY, a nie interfejs.
        //   List<String> a = new ArrayList<>();   → zmienna typu List (można później podstawić LinkedList)
        //   var b = new ArrayList<String>();      → zmienna typu ArrayList (LinkedList już nie wejdzie)
        // To zwykle nieistotne (zasięg to kilka linii), ale pamiętaj o tym przy zmiennych używanych w wielu miejscach.
        // DOBRA PRAKTYKA: rozważ var tylko wtedy, gdy po prawej stronie jest pełny typ (new ArrayList<String>()).
        //   Gdy chcesz pracować na interfejsie (List, Map) — zapisz go jawnie po lewej stronie.
    }

    // =================================================================================================
    // 6. TYP STATYCZNY POZOSTAJE STAŁY
    // =================================================================================================

    /**
     * 6. var nie robi z Javy języka dynamicznego. Typ ustala się raz; var jest tylko skrótem zapisu.
     * Widać to po tym, że IDE zawsze potrafi wyświetlić wywnioskowany typ.
     */
    static void staticTypeStaysFixed() {
        section("6. Typ statyczny pozostaje stały");

        // Typ wyrażenia warunkowego z różnymi typami to „najmniejszy wspólny nadtyp” (lub typ przecięciowy).
        var mixed = Boolean.parseBoolean("true") ? 1 : "tekst";          // Integer albo String → wspólny nadtyp
        show("wartość", mixed);
        // WYNIK: wartość → 1
        show("typ w czasie wykonania", typeOf(mixed));
        // WYNIK: typ w czasie wykonania → Integer
        // Typ STATYCZNY zmiennej mixed to jednak „Object & Serializable & Comparable<…>” — nie da się go zapisać
        // w kodzie (to „typ niewyrażalny”, sekcja 9), więc z mixed wolno wołać tylko metody wspólne,
        // np. toString(); żadnego mixed.length() ani mixed + 1.

        // var z wynikiem metody generycznej: wnioskowane jest to, co metoda zwraca.
        var students = SampleData.students();                            // List<Student>
        var byCity = students.stream()                                   // by city = według miasta
                .collect(Collectors.groupingBy(Student::city, TreeMap::new, Collectors.counting()));
        // Pełny typ to TreeMap<String, Long> — przy var nie musimy go pisać ani pamiętać.
        show("uczniowie według miast", byCity);
        // WYNIK: uczniowie według miast → {Gdańsk=2, Kraków=2, Poznań=1, Warszawa=3}
        long warsaw = byCity.get("Warszawa");                            // wiemy, że wartość to Long
        show("Warszawa", warsaw);
        // WYNIK: Warszawa → 3

        // PUŁAPKA: gdy zmieni się typ zwracany metody (np. int → long), var po cichu zmieni typ zmiennej,
        //   a kod dalej może się skompilować i zadziałać inaczej (np. dzielenie całkowite zamiast ułamkowego).
        //   Z jawnym typem kompilator od razu pokazałby błąd. Dlatego w kodzie krytycznym typ pisz jawnie.
        // DOBRA PRAKTYKA: nazwy zmiennych niech niosą znaczenie typu: var customerById, var activeEmployees —
        //   przy var nazwa to jedyna dokumentacja.
    }

    // =================================================================================================
    // 7. CZYTELNOŚĆ
    // =================================================================================================

    /** Pomocnicza: metoda o nieinformacyjnej nazwie — typ wyniku nie wynika z wywołania. */
    static Object fetch() {      // fetch = pobierz
        return List.of("a", "b");
    }

    /**
     * 7. Kiedy var poprawia czytelność, a kiedy ją psuje. Zasada z wytycznych autorów JDK
     * („Style Guidelines for Local Variable Type Inference”): pisz kod dla CZYTELNIKA, nie dla skrócenia.
     */
    static void readability() {
        section("7. Czytelność — dobre i złe użycie");

        // DOBRE użycia (typ widać po prawej stronie albo jest zbyt długi i powtarzany):
        var employeesByDepartment = new LinkedHashMap<String, List<Employee>>();   // by department = według działu
        var firstName = SampleData.employees().get(0).name();            // first name = pierwsze imię
        var counter = new StringBuilder();                               // counter = licznik
        for (var employee : SampleData.employees()) {
            employeesByDepartment.computeIfAbsent(employee.department().name(), key -> new ArrayList<>())
                    .add(employee);
        }
        counter.append(employeesByDepartment.size()).append(" działów z pracownikami");
        show("dobre użycia", firstName + "; " + counter);
        // WYNIK: dobre użycia → Anna Nowak; 5 działów z pracownikami

        // ZŁE użycia (typ niewidoczny, nazwa nic nie mówi):
        var thing = fetch();                                             // thing = coś; co to jest? Object — a z nazwy nie wiadomo
        show("złe: var thing = fetch()", thing);
        // WYNIK: złe: var thing = fetch() → [a, b]
        // Tu typ statyczny to Object (nie List!), więc thing.size() się nie skompiluje —
        // a czytelnik nie ma skąd tego wiedzieć bez zaglądania do metody fetch().

        // Zasady praktyczne:
        //   1. Dobra nazwa zmiennej ważniejsza niż typ: var activeEmployees, nie var list.
        //   2. Prawa strona pokazuje typ: new X(), literał, rzutowanie, metoda fabryczna o jasnej nazwie.
        //   3. Mały zasięg: zmienna używana w 3–5 liniach. Dla dużych metod pisz typ jawnie.
        //   4. Przy typach prostych (int, boolean, String) var oszczędza niewiele — zwykle zostaw jawnie.
        //   5. Nie łącz var z długimi łańcuchami wywołań, gdy typ pośredni jest niejasny.
        // PUŁAPKA: w recenzji kodu (np. na GitHubie) nie masz podpowiedzi IDE — typu po var nie zobaczysz.
        //   To kolejny powód, by prawa strona sama mówiła, co zwraca.
        // DOBRA PRAKTYKA: zespół powinien mieć wspólną regułę (np. „var, gdy typ wynika z new lub literału”)
        //   i jej trzymać się w całym projekcie — spójność jest ważniejsza niż sama decyzja.
    }

    // =================================================================================================
    // 8. VAR TO NIE SŁOWO KLUCZOWE
    // =================================================================================================

    // var jako nazwa metody — dozwolone (stary kod z taką metodą nadal działa).
    static int var(int x) {
        return x + 1;
    }

    /**
     * 8. var jest „zarezerwowaną nazwą typu”, a nie słowem kluczowym. Dzięki temu kod napisany
     * w Javie 9, w którym zmienna lub metoda nazywała się var, nie przestał się kompilować.
     */
    static void notAKeyword() {
        section("8. var nie jest słowem kluczowym");

        var var = 1;                         // zmienna typu int o nazwie var — dziwne, ale legalne
        show("var var = 1", var);
        // WYNIK: var var = 1 → 1
        show("metoda o nazwie var", var(var));
        // WYNIK: metoda o nazwie var → 2

        // Co jest zabronione: nazwa KLASY, interfejsu, enuma, rekordu lub parametru typu o nazwie var:
        //   class var { }       // błąd kompilacji: „var” jest niedozwoloną nazwą typu
        // Nazwa pakietu i zmiennej — wolno. Dla porównania: słowa kluczowe jak class, int, new nie wolno
        // używać jako nazwy NIGDZIE.
        // DOBRA PRAKTYKA: nigdy nie nazywaj niczego var, choć kompilator pozwala — to zagadka dla czytelnika.
        //   Podobnie zachowują się nowsze „słowa kontekstowe”: record, sealed, permits, yield — poza swoim
        //   miejscem użycia są zwykłymi identyfikatorami (zob. Modern03, Modern05).
    }

    // =================================================================================================
    // 9. TYPY, KTÓRYCH NIE DA SIĘ ZAPISAĆ
    // =================================================================================================

    /**
     * 9. Typy niewyrażalne (non-denotable) — takie, których nie da się napisać w kodzie. Najciekawszy
     * przykład: obiekt klasy anonimowej. Jawnym typem mógłbyś podać tylko jego nadtyp (Object),
     * a wtedy nowe pola i metody są niewidoczne. var zachowuje pełny, dokładny typ.
     */
    static void nonDenotableTypes() {
        section("9. Typy niewyrażalne: anonimowy obiekt przez var");

        // Obiekt anonimowy z własnym polem i metodą — zachowany dzięki var.
        var holder = new Object() {                                      // holder = pojemnik
            int counter = 3;                                             // pole, którego Object nie ma
            String describe() {                                          // describe = opisz
                return "licznik=" + counter;
            }
        };
        holder.counter++;                                                // widoczne, bo typ zmiennej to „ta klasa anonimowa”
        show("holder.describe()", holder.describe());
        // WYNIK: holder.describe() → licznik=4

        // Z jawnym typem Object nic z tego nie zadziała:
        //   Object plain = new Object() { int counter = 3; };
        //   plain.counter++;     // BŁĄD KOMPILACJI: Object nie ma pola counter
        Object plain = new Object() {                                    // plain = zwykły
            @Override
            public String toString() {
                return "zwykły Object";
            }
        };
        show("przez Object tylko metody Object", plain);
        // WYNIK: przez Object tylko metody Object → zwykły Object

        // Zastosowanie: lokalny „rekord na szybko” w jednej metodzie, gdy nie warto tworzyć klasy.
        // Od Javy 16 do tego lepiej służy lokalny rekord: record Pair(String a, int b) { } wewnątrz metody
        // (zob. Modern05RecordsSealedPatterns) — ma nazwę, equals i toString.
        // PUŁAPKA: ponieważ nie da się nazwać tego typu, nie przekażesz holder do innej metody ani nie zwrócisz go
        //   z metody inaczej niż jako Object. To rozwiązanie tylko na użytek wewnątrz jednej metody.
        // DOBRA PRAKTYKA: używaj tej sztuczki rzadko; zwykle lokalny rekord (Java 16+) jest czytelniejszy.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • var (Java 10, JEP 286): zmienne lokalne z inicjalizatorem; for, for-each, try-with-resources;
     *     parametry lambdy od Javy 11 (JEP 323), wszystkie albo żaden.
     *   • Nie wolno: pola, parametry, typ zwracany, brak inicjalizatora, null, {1,2}, lambda/referencja bez typu,
     *     deklaracja wielu zmiennych (var a = 1, b = 2), nawias tablicy (var a[]).
     *   • Typ jest wnioskowany RAZ w czasie kompilacji; typowanie pozostaje statyczne.
     *   • var + romb → ArrayList<Object> (czyli new ArrayList<>()): napisz typ jawnie po jednej ze stron.
     *   • Literały: 1 → int, 1L → long, 1.0 → double; += potrafi po cichu obciąć wartość.
     *   • var trzyma typ konkretny (ArrayList), nie interfejs (List).
     *   • Czytelność: znacząca nazwa, typ widoczny po prawej stronie, mały zasięg.
     *   • var nie jest słowem kluczowym; nazwa klasy var jest zabroniona, zmiennej — dozwolona (ale nie rób tego).
     *   • Typy niewyrażalne (klasa anonimowa) zachowują pola i metody tylko dzięki var.
     *
     * PYTANIA KONTROLNE:
     *   1. Czy var oznacza typowanie dynamiczne? Uzasadnij.
     *   2. Wymień pięć miejsc, w których var jest zabronione.
     *   3. Co wypisze:  var x = 7;  var y = 2;  System.out.println(x / y);  ?
     *   4. ZNAJDŹ BŁĄD:  var names = new ArrayList<>();  names.add("Ala");  String n = names.get(0);
     *   5. Co wypisze:  var total = 0;  total += 2_147_483_648L;  System.out.println(total);  ?
     *   6. Dlaczego var nie działa dla parametrów metod, a dla parametrów lambdy (od Javy 11) tak?
     *   7. ZNAJDŹ BŁĄD:  var f = () -> 42;
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: typy wnioskowane",
                List.of("Integer", "Long", "Double", "Character", "String", "Float"), () -> exercise1());
        Check.equal("ćw. 2: grupowanie po pierwszej literze",
                "{a=[ananas, awokado], b=[banan]}", () -> exercise2(List.of("banan", "ananas", "awokado")).toString());
        Check.equal("ćw. 3: suma bez przepełnienia", 4_000_000_000L,
                () -> exercise3(List.of(2_000_000_000, 2_000_000_000)));
        Check.equal("ćw. 4: najczęstsze słowo", "a=2", () -> exercise4(List.of("b", "a", "b", "a", "c")));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)",
                List.of("Integer", "Long", "Double", "Character", "String", "Float"), () -> solution1());
        Check.equal("ćw. 2 (wzorzec)",
                "{a=[ananas, awokado], b=[banan]}", () -> solution2(List.of("banan", "ananas", "awokado")).toString());
        Check.equal("ćw. 3 (wzorzec)", 4_000_000_000L, () -> solution3(List.of(2_000_000_000, 2_000_000_000)));
        Check.equal("ćw. 4 (wzorzec)", "a=2", () -> solution4(List.of("b", "a", "b", "a", "c")));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zadeklaruj przez var zmienne: 7, 7L, 7.0, 'x', "x", 7.0f i zwróć listę nazw
     * ich typów (typeOf) w tej kolejności.
     * Podpowiedź: var a = 7; ... List.of(typeOf(a), ...). Zwróć uwagę na sufiksy L i f.
     */
    static List<String> exercise1() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (łatwe): PRZEPISZ z jawnych typów na var, a potem uzupełnij: pogrupuj słowa według pierwszej litery
     * w posortowanej mapie.
     * <pre>{@code
     * // PRZED:
     * TreeMap<Character, List<String>> groups = new TreeMap<Character, List<String>>();
     * for (String word : words) {
     *     List<String> bucket = groups.get(word.charAt(0));
     *     if (bucket == null) { bucket = new ArrayList<String>(); groups.put(word.charAt(0), bucket); }
     *     bucket.add(word);
     * }
     * }</pre>
     * Podpowiedź: {@code var groups = new TreeMap<Character, List<String>>();} i computeIfAbsent. Słowa w grupie
     * mają zostać w kolejności wejściowej, a nie posortowane — tu: banan trafia do b, ananas i awokado do a.
     */
    static Map<Character, List<String>> exercise2(List<String> words) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): napraw pułapkę z sekcji 4 — zsumuj liczby bez przepełnienia.
     * Podpowiedź: var total = 0L (sufiks L!) — wtedy zmienna ma typ long, a nie int.
     */
    static long exercise3(List<Integer> numbers) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zwróć najczęstsze słowo w formacie „słowo=ile”. Przy remisie wygrywa słowo
     * pierwsze alfabetycznie. Użyj var w pętli for-each po entrySet.
     * Podpowiedź: zlicz merge do TreeMap (sam porządek alfabetyczny rozstrzyga remis, gdy porównujesz „>” a nie „>=”).
     */
    static String exercise4(List<String> words) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solution1() {
        var a = 7;
        var b = 7L;
        var c = 7.0;
        var d = 'x';
        var e = "x";
        var f = 7.0f;
        return List.of(typeOf(a), typeOf(b), typeOf(c), typeOf(d), typeOf(e), typeOf(f));
    }

    static Map<Character, List<String>> solution2(List<String> words) {
        var groups = new TreeMap<Character, List<String>>();
        for (var word : words) {
            groups.computeIfAbsent(word.charAt(0), key -> new ArrayList<>()).add(word);
        }
        return groups;
    }

    static long solution3(List<Integer> numbers) {
        var total = 0L;
        for (var number : numbers) {
            total += number;
        }
        return total;
    }

    static String solution4(List<String> words) {
        var counts = new TreeMap<String, Integer>();
        for (var word : words) {
            counts.merge(word, 1, Integer::sum);
        }
        String bestWord = null;                                          // best word = najlepsze słowo
        var bestCount = 0;
        for (var entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestCount = entry.getValue();
                bestWord = entry.getKey();
            }
        }
        return bestWord + "=" + bestCount;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Nie. Typ jest ustalany raz, w czasie kompilacji, i nie można go zmienić — var to tylko skrót zapisu.
     *   2. Pola klas, parametry metod, typ zwracany metody, zmienna bez inicjalizatora, inicjalizator null,
     *      inicjalizator tablicowy {…}, lambda lub referencja do metody bez typu docelowego, kilka zmiennych w jednej deklaracji.
     *   3. 3 (dzielenie całkowite int / int).
     *   4. var names = new ArrayList<>() to ArrayList<Object>; names.get(0) zwraca Object, więc przypisanie
     *      do String nie skompiluje się. Poprawka: new ArrayList<String>() albo jawny typ po lewej stronie.
     *   5. -2147483648 (total to int; 2_147_483_648L po rzutowaniu na int daje najmniejszą wartość int).
     *   6. Parametry metod są częścią publicznego kontraktu (sygnatury) i muszą być jawne. Parametry lambdy
     *      mają typ wynikający z interfejsu funkcyjnego, więc var jest tylko zapisem zastępującym ich pominięcie —
     *      pozwala dopisać adnotację lub final.
     *   7. Brak typu docelowego: kompilator nie wie, jaki interfejs funkcyjny ma implementować lambda.
     *      Poprawka: Supplier<Integer> f = () -> 42;
     */
    // </editor-fold>
}
