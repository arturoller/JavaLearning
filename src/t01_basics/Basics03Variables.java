package t01_basics;

import helpers.Check;
import helpers.SampleData;

import java.util.ArrayList;
import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Zmienne — deklaracja, przypisanie, zasięg, final, stałe i var
 *        (variable = zmienna; scope = zasięg; final = ostateczny; constant = stała)
 *
 * W SKRÓCIE:
 *   Zmienna to nazwane miejsce na wartość określonego typu. Najpierw ją deklarujesz (typ + nazwa), potem
 *   przypisujesz wartość znakiem =. Zmienna istnieje tylko w swoim bloku { }. final zabrania ponownego
 *   przypisania, a var (Java 10+) pozwala kompilatorowi samemu odgadnąć typ.
 *
 * ANALOGIA: pudełko z etykietą.
 *   Deklaracja to postawienie pudełka i naklejenie etykiety („age, tylko liczby całkowite”). Przypisanie to
 *   włożenie do niego wartości — nowa wartość wyrzuca starą. final to pudełko zaklejone taśmą po pierwszym
 *   włożeniu. Zasięg to pokój, w którym pudełko stoi — z innego pokoju go nie widać.
 *
 * JAK TO DZIAŁA:
 *   int age;            ← deklaracja: typ int, nazwa age
 *   age = 30;           ← przypisanie (znak = to PRZYPISANIE, a nie porównanie — porównanie to ==)
 *   int year = 2026;    ← deklaracja + inicjalizacja w jednej linii (tak najczęściej)
 *   final int MAX = 5;  ← nie da się już przypisać nowej wartości
 *   var name = "Ala";   ← kompilator widzi "Ala" i ustala typ String (Java 10+)
 *
 * SŁÓWKA:
 *   declare = zadeklarować; assign = przypisać; initialize = zainicjalizować (nadać pierwszą wartość);
 *   scope = zasięg; block = blok; shadowing = przesłanianie (nazwy); final = ostateczny; constant = stała;
 *   var = zmienna (typ zgadywany przez kompilator); temp (temporary) = tymczasowy; swap = zamień miejscami.
 *
 * ZOBACZ TEŻ: t01_basics/Basics02PrimitiveTypes (typy), t01_basics/Basics04Operators (+=, ++),
 *             t06_oop_basics/Oop04Static (pola static), t13_lambdas/Lambda06ClosuresScope (zmienne w lambdach).
 * </pre>
 */
public class Basics03Variables {

    /** VAT_RATE = stawka VAT w procentach. Stała: static final + WIELKIE_LITERY. */
    private static final int VAT_RATE = 23;

    /** counter = licznik. Pole klasy (static) — widoczne w całej klasie. Do sekcji 3 o przesłanianiu nazw. */
    private static int counter = 100;

    public static void main(String[] args) {
        title("Basics03 — zmienne");

        declareAndAssign();     // declare and assign = deklaruj i przypisz
        scope();                // scope = zasięg
        shadowing();            // shadowing = przesłanianie nazw
        finalVariables();       // final variables = zmienne final
        constants();            // constants = stałe
        varKeyword();           // var keyword = słowo var
        swapValues();           // swap values = zamiana wartości
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. DEKLARACJA I PRZYPISANIE
    // =================================================================================================

    /** 1. Zmienna ma typ i nazwę. Wartość można zmieniać wiele razy — nowa zastępuje starą. */
    static void declareAndAssign() {
        section("1. Deklaracja i przypisanie");

        int score = 10;              // score = wynik (punkty)
        show("na start", score);
        score = 25;                  // stara wartość (10) znika
        show("po przypisaniu", score);
        score = score + 5;           // czytamy od prawej: weź obecne score (25), dodaj 5, zapisz z powrotem
        show("po score = score + 5", score);
        // WYNIK: na start → 10
        // WYNIK: po przypisaniu → 25
        // WYNIK: po score = score + 5 → 30

        // PUŁAPKA: = to PRZYPISANIE, == to PORÓWNANIE. W matematyce „x = x + 5” to sprzeczność, w Javie — zwykła
        //   instrukcja „zwiększ x o 5”.
        // DOBRA PRAKTYKA: deklaruj zmienną tam, gdzie jej pierwszy raz potrzebujesz, i od razu nadawaj wartość.
    }

    // =================================================================================================
    // 2. ZASIĘG
    // =================================================================================================

    /**
     * 2. Zmienna istnieje od miejsca deklaracji do końca bloku { }, w którym ją zadeklarowano.
     * Zmienna pętli for żyje tylko wewnątrz pętli.
     */
    static void scope() {
        section("2. Zasięg — zmienna żyje do końca swojego bloku");

        int total = 0;                           // widoczna w całej metodzie
        for (int i = 1; i <= 3; i++) {           // i widoczne TYLKO w pętli
            int doubled = i * 2;                 // doubled = podwojone; nowa zmienna w każdym obrocie pętli
            total = total + doubled;
        }
        show("suma podwojonych 1..3", total);
        // WYNIK: suma podwojonych 1..3 → 12

        // Tu i oraz doubled już NIE istnieją — użycie = błąd kompilacji „cannot find symbol”.
        // DOBRA PRAKTYKA: im mniejszy zasięg zmiennej, tym lepiej — mniej miejsc, gdzie ktoś może ją zepsuć.
    }

    // =================================================================================================
    // 3. PRZESŁANIANIE NAZW
    // =================================================================================================

    /**
     * 3. Zmienna lokalna może mieć tę samą nazwę co pole klasy — wtedy PRZESŁANIA je (shadowing).
     * Do pola dostaniesz się wtedy przez nazwę klasy (pole static) albo this (pole obiektu, t06_oop_basics).
     */
    static void shadowing() {
        section("3. Przesłanianie nazw (shadowing)");

        int counter = 1;                                            // lokalna — przesłania pole counter
        show("counter (lokalna)", counter);
        show("Basics03Variables.counter (pole)", Basics03Variables.counter);
        // WYNIK: counter (lokalna) → 1
        // WYNIK: Basics03Variables.counter (pole) → 100

        // PUŁAPKA: przesłanianie łatwo przeoczyć — myślisz, że zmieniasz pole, a zmieniasz zmienną lokalną.
        //   Dwie zmienne LOKALNE o tej samej nazwie w jednym zasięgu to błąd kompilacji („already defined”).
    }

    // =================================================================================================
    // 4. ZMIENNE FINAL
    // =================================================================================================

    /**
     * 4. final = po pierwszym przypisaniu nie można przypisać NOWEJ wartości. Ale uwaga: jeśli zmienna final
     * wskazuje na obiekt (np. listę), to sam obiekt nadal MOŻNA zmieniać — zablokowane jest tylko „przepięcie”
     * zmiennej na inny obiekt.
     */
    static void finalVariables() {
        section("4. final — zakaz ponownego przypisania");

        final int maxUsers = 50;                 // maxUsers = 60; → błąd: „cannot assign a value to final variable”
        show("maxUsers", maxUsers);
        // WYNIK: maxUsers → 50

        final List<String> names = new ArrayList<>();   // ArrayList = lista, do której można dodawać (t12_collections)
        names.add("Ala");                        // DOZWOLONE: zmieniamy zawartość listy
        names.add("Olek");
        show("lista final po dodaniu", names);
        // WYNIK: lista final po dodaniu → [Ala, Olek]
        // names = new ArrayList<>();  → błąd kompilacji: nie wolno przypisać NOWEJ listy do zmiennej final.

        // PUŁAPKA: final NIE znaczy „niezmienny obiekt”. To tylko zakaz ponownego przypisania zmiennej.
        //   Niezmienne obiekty to osobny temat (t06_oop_basics/Oop06Immutability, List.of).
    }

    // =================================================================================================
    // 5. STAŁE
    // =================================================================================================

    /**
     * 5. Stała to pole static final z nazwą WIELKIMI_LITERAMI. Zastępuje „magiczne liczby” w kodzie —
     * liczby, o których nie wiadomo, skąd się wzięły.
     */
    static void constants() {
        section("5. Stałe zamiast magicznych liczb");

        int net = 200;                                   // net = netto
        int grossMagic = net + net * 23 / 100;           // ŹLE: co to jest 23? VAT? rabat? procent czego?
        int gross = net + net * VAT_RATE / 100;          // DOBRZE: nazwa mówi, co to za liczba
        show("brutto (magiczna liczba)", grossMagic);
        show("brutto (stała VAT_RATE)", gross);
        // WYNIK: brutto (magiczna liczba) → 246
        // WYNIK: brutto (stała VAT_RATE) → 246

        // DOBRA PRAKTYKA: gdy stawka VAT się zmieni, poprawiasz JEDNO miejsce (stałą), a nie szukasz „23” w całym kodzie.
        // (Do prawdziwych kwot użyj BigDecimal — t15_numbers/Numbers02MoneyValueObject. Tu int dla prostoty.)
    }

    // =================================================================================================
    // 6. VAR
    // =================================================================================================

    /**
     * 6. var (Java 10+) — typ zmiennej LOKALNEJ ustala kompilator na podstawie wartości po prawej stronie.
     * Typ nadal jest stały (Java nie staje się „dynamiczna”) — po prostu nie musisz go pisać.
     */
    static void varKeyword() {
        section("6. var — typ odgadnięty przez kompilator (Java 10+)");

        var city = "Kraków";                     // String
        var year = 2026;                         // int
        var price = 19.99;                       // double
        var bigNumber = 5_000_000_000L;          // long (przez L)
        show("city jest typu", city.getClass().getSimpleName());
        show("price jest typu", ((Object) price).getClass().getSimpleName());
        show("year + 1", year + 1);
        show("bigNumber", bigNumber);
        // WYNIK: city jest typu → String
        // WYNIK: price jest typu → Double
        // WYNIK: year + 1 → 2027
        // WYNIK: bigNumber → 5000000000
        // (Double z wielkiej litery, bo getClass działa na obiekcie — liczba została na chwilę „opakowana”: Basics06Wrappers.)

        // var NIE zadziała: var x; (brak wartości), var y = null; (nie wiadomo, jaki typ), dla pól klasy i parametrów metod.
        // city = 5;  → błąd kompilacji: city jest typu String na zawsze.
        // PUŁAPKA: var list = new ArrayList<>(); (pusty diament) daje ArrayList<Object> — lista „czegokolwiek”.
        //   Pisz: var list = new ArrayList<String>();
        // DOBRA PRAKTYKA: używaj var, gdy typ widać po prawej stronie (var names = new ArrayList<String>()).
        //   Gdy nie widać (var result = service.process();), napisz typ jawnie — kod czyta się łatwiej.
    }

    // =================================================================================================
    // 7. ZAMIANA WARTOŚCI DWÓCH ZMIENNYCH
    // =================================================================================================

    /** 7. Klasyczne zadanie: zamień wartości a i b. Potrzebna jest trzecia, tymczasowa zmienna. */
    static void swapValues() {
        section("7. Zamiana wartości (zmienna tymczasowa)");

        int a = 3;
        int b = 8;
        int temp = a;        // temp = tymczasowa: zapamiętaj a, zanim je nadpiszesz
        a = b;
        b = temp;
        show("a, b po zamianie", a + ", " + b);
        // WYNIK: a, b po zamianie → 8, 3

        // PUŁAPKA: bez temp:  a = b; b = a;  → obie zmienne mają 8 — pierwotna wartość a przepadła.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • int x = 5;  (deklaracja + inicjalizacja);  x = 7;  (przypisanie);  = to przypisanie, == to porównanie.
     *   • Zasięg: od deklaracji do końca bloku { }; zmienna pętli for żyje tylko w pętli.
     *   • Zmienna lokalna o nazwie pola PRZESŁANIA pole; dwie lokalne o tej samej nazwie = błąd.
     *   • final: nie przypiszesz ponownie — ale obiekt (np. lista) może się zmieniać.
     *   • Stała: private static final int VAT_RATE = 23;  — zamiast magicznych liczb.
     *   • var (Java 10+): tylko zmienne lokalne z wartością; typ ustalony raz na zawsze.
     *   • Zamiana wartości: temp = a; a = b; b = temp.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się deklaracja od inicjalizacji?
     *   2. Co wypisze:  int x = 5; x = x * 2; x = x - 3; System.out.println(x);  ?
     *   3. ZNAJDŹ BŁĄD:  for (int i = 0; i < 3; i++) { int sum = i; }  System.out.println(sum);
     *   4. Czy  final List<String> list = new ArrayList<>(); list.add("x");  się skompiluje? Dlaczego?
     *   5. Jaki typ ma  var n = 10;  a jaki  var m = 10L;  ?
     *   6. Co wypisze:  int a = 1, b = 2; a = b; b = a; System.out.println(a + " " + b);  ?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: zamiana miejscami", "prawy lewy", () -> exercise1("lewy", "prawy"));
        Check.equal("ćw. 2: cena brutto 200 zł netto", 246, () -> exercise2(200));
        Check.equal("ćw. 3: długość najdłuższego słowa", 8, () -> exercise3(SampleData.words()));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "prawy lewy", () -> solution1("lewy", "prawy"));
        Check.equal("ćw. 2 (wzorzec)", 246, () -> solution2(200));
        Check.equal("ćw. 3 (wzorzec)", 8, () -> solution3(SampleData.words()));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 3 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zamień wartości zmiennych first i second MIEJSCAMI (użyj zmiennej tymczasowej),
     * a potem zwróć {@code first + " " + second}. Dla ("lewy", "prawy") wynik to "prawy lewy".
     */
    static String exercise1(String first, String second) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 2 (średnie): policz cenę brutto (w pełnych złotych, int) z ceny netto, używając stałej VAT_RATE
     * zamiast liczby 23. Wzór: {@code netto + netto * VAT_RATE / 100}.
     * Podpowiedź: najpierw mnożenie, potem dzielenie — inaczej 23 / 100 w liczbach całkowitych da 0 (Basics04Operators).
     */
    static int exercise2(int net) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): zwróć długość NAJDŁUŻSZEGO słowa z listy. Dla SampleData.words() to 8.
     * Podpowiedź: zmienna {@code int longest = 0;} PRZED pętlą (musi przetrwać wszystkie obroty — zasięg!),
     * w pętli for-each: jeśli {@code word.length() > longest}, to {@code longest = word.length();}.
     */
    static int exercise3(List<String> words) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(String first, String second) {
        String temp = first;
        first = second;
        second = temp;
        return first + " " + second;
    }

    static int solution2(int net) {
        return net + net * VAT_RATE / 100;
    }

    static int solution3(List<String> words) {
        int longest = 0;                      // zadeklarowana przed pętlą — żyje przez wszystkie obroty
        for (String word : words) {
            if (word.length() > longest) {
                longest = word.length();
            }
        }
        return longest;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Deklaracja tworzy zmienną (typ + nazwa); inicjalizacja nadaje jej PIERWSZĄ wartość (często w tej samej linii).
     *   2. 7 (5 → 10 → 7).
     *   3. sum jest zadeklarowane wewnątrz pętli — poza nią nie istnieje (błąd „cannot find symbol”). Zadeklaruj
     *      int sum = 0; przed pętlą, a w pętli rób sum = sum + i;
     *   4. Tak. final blokuje tylko ponowne przypisanie zmiennej list; dodawanie elementów zmienia obiekt, nie zmienną.
     *   5. int oraz long (przez przyrostek L).
     *   6. „2 2” — po a = b obie mają 2, a pierwotne 1 przepadło (do zamiany potrzebna zmienna tymczasowa).
     */
    // </editor-fold>
}
