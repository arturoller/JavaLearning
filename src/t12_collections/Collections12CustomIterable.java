package t12_collections;

import helpers.Check;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Własne Iterable i Iterator — jak zbudować coś, co działa w for-each
 *        (Iterable = "coś, co da się iterować"; Iterator = "wskaźnik" przesuwający się po elementach)
 *
 * W SKRÓCIE:
 *   for (T x : cos) działa dla KAŻDEGO typu implementującego Iterable<T> — nie tylko dla List/Set/Map. W tej
 *   lekcji piszemy własne: IntRange (liczby od..do z krokiem) i Fibonacci (ciąg Fibonacciego do limitu) — bez
 *   trzymania wszystkich liczb w pamięci naraz, bo Iterator generuje je LENIWIE, jedną na wywołanie next().
 *
 * ANALOGIA: pilot do przewijania filmu.
 *   Iterable to sama płyta DVD — może z niej korzystać wielu widzów naraz. Iterator to pilot KONKRETNEGO widza —
 *   pamięta, na której scenie widz aktualnie jest. Każdy widz (każde wywołanie iterator()) dostaje SWÓJ WŁASNY
 *   pilot od zera, więc mogą oglądać ten sam film niezależnie, każdy w swoim tempie.
 *
 * JAK TO DZIAŁA:
 *   interface Iterable{@code <T>} { Iterator{@code <T>} iterator(); }
 *   interface Iterator{@code <T>} { boolean hasNext(); T next(); default void remove() { throw ...; } }
 *   for (T x : iterowalne) { ... }  ⟶  Iterator{@code <T>} it = iterowalne.iterator();
 *                                     while (it.hasNext()) { T x = it.next(); ... }
 *
 * SŁÓWKA:
 *   lazily = leniwie (liczone dopiero na żądanie, nie z góry); anonymous class = klasa anonimowa; inner class =
 *   klasa wewnętrzna (niestatyczna, związana z konkretnym obiektem); named class = klasa nazwana (osobna);
 *   exhausted = wyczerpany (iterator, który doszedł do końca); fresh = świeży (nowy, nieużywany iterator).
 *
 * ZOBACZ TEŻ: t12_collections/Collections01Overview (hierarchia Iterable → Collection), t11_generics/Generics03Methods
 *             (typy generyczne w metodach), t13_lambdas/Lambda01FromAnonymousToLambda (Iterable jako interfejs
 *             funkcyjny — lambda zamiast anonimowej klasy), t06_oop_basics/Oop07NestedClasses (static nested vs inner).
 * </pre>
 */
public class Collections12CustomIterable {

    public static void main(String[] args) {
        title("Collections12 — własne Iterable i Iterator: IntRange, Fibonacci");

        iterableIteratorContractRecap(); // iterable iterator contract recap = przypomnienie kontraktu
        intRangeBasics();                 // int range basics = IntRange: podstawy
        iteratorContractManually();       // iterator contract manually = kontrakt Iteratora ręcznie
        removeUnsupportedByDefault();     // remove unsupported by default = remove domyślnie niewspierane
        freshIteratorEachCall();          // fresh iterator each call = świeży iterator przy każdym wywołaniu
        fibonacciIterable();              // fibonacci iterable = Fibonacci jako Iterable
        anonymousVsNamedComparison();     // anonymous vs named comparison = anonimowa kontra nazwana klasa
        innerAccessingOuterState();       // inner accessing outer state = klasa wewnętrzna sięgająca po stan zewnętrzny
        exercises();                       // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PRZYPOMNIENIE KONTRAKTU Iterable / Iterator
    // =================================================================================================

    /** 1. Iterable ma jedną metodę (iterator()); Iterator ma hasNext/next (wymagane) i remove (opcjonalne, sekcja 4). */
    static void iterableIteratorContractRecap() {
        section("1. Iterable i Iterator — przypomnienie kontraktu (Collections01Overview)");

        note("Iterable<T> to interfejs z JEDNĄ metodą: Iterator<T> iterator(). To dzięki niej działa for-each —");
        note("KAŻDA klasa implementująca Iterable może stanąć po prawej stronie dwukropka w for (T x : ...).");
        note("Iterator<T> ma dwie WYMAGANE metody: boolean hasNext() i T next() (next() rzuca NoSuchElementException,");
        note("gdy elementów już nie ma) — oraz jedną OPCJONALNĄ, z domyślną implementacją: void remove() (sekcja 4).");

        StringBuilder line = new StringBuilder();
        for (int n : List.of(10, 20, 30)) {   // List implementuje Iterable — WŁAŚNIE dzięki temu for-each działa
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(n);
        }
        show("for-each po List.of(10, 20, 30)", line);
        // WYNIK: for-each po List.of(10, 20, 30) → 10 20 30

        note("W tej lekcji piszemy WŁASNE klasy implementujące Iterable — od tej chwili Twój typ będzie mógł");
        note("pojawić się w for-each dokładnie tak samo jak List, Set czy tablica.");
    }

    // =================================================================================================
    // 2. IntRange — WŁASNY Iterable<Integer>
    // =================================================================================================

    /**
     * RangeIterator = OSOBNA, NAZWANA klasa Iteratora dla IntRange. Generuje kolejne liczby LENIWIE —
     * current jest liczone dopiero przy next(), nigdy nie ma w pamięci całego zakresu naraz.
     */
    static final class RangeIterator implements Iterator<Integer> {
        private int current;
        private final int to;      // wyłącznie (exclusive), jak w for (int i = from; i < to; i += step)
        private final int step;

        RangeIterator(int from, int to, int step) {
            this.current = from;
            this.to = to;
            this.step = step;
        }

        @Override
        public boolean hasNext() {
            return current < to;
        }

        @Override
        public Integer next() {
            if (!hasNext()) {
                throw new NoSuchElementException("IntRange: brak kolejnego elementu (current=" + current + ", to=" + to + ")");
            }
            int value = current;
            current += step;
            return value;
        }
    }

    /** IntRange = liczby całkowite od from (włącznie) do to (wyłącznie) z krokiem step. Nie przechowuje ich — generuje. */
    static final class IntRange implements Iterable<Integer> {
        private final int from;
        private final int to;
        private final int step;

        IntRange(int from, int to, int step) {
            if (step <= 0) {
                throw new IllegalArgumentException("step musi być dodatni, było: " + step);
            }
            this.from = from;
            this.to = to;
            this.step = step;
        }

        @Override
        public Iterator<Integer> iterator() {
            return new RangeIterator(from, to, step);   // NOWA instancja za KAŻDYM razem — sekcja 5 pokazuje dlaczego
        }
    }

    /** 2. IntRange w for-each — działa jak każda inna kolekcja, mimo że nigdy nie trzyma listy liczb w pamięci. */
    static void intRangeBasics() {
        section("2. IntRange — własny Iterable<Integer>, użycie w for-each");

        IntRange oddNumbers = new IntRange(1, 10, 2);
        int sum = 0;
        for (int n : oddNumbers) {
            sum += n;
        }
        show("suma IntRange(1, 10, 2) (czyli 1+3+5+7+9)", sum);
        // WYNIK: suma IntRange(1, 10, 2) (czyli 1+3+5+7+9) → 25

        List<Integer> collected = new ArrayList<>();
        for (int n : oddNumbers) {
            collected.add(n);
        }
        show("zebrane do listy", collected);
        // WYNIK: zebrane do listy → [1, 3, 5, 7, 9]

        expectThrows("new IntRange(0, 10, 0) — step musi być dodatni", () -> new IntRange(0, 10, 0));
        // WYNIK: ✔ new IntRange(0, 10, 0) — step musi być dodatni → rzucono IllegalArgumentException: step musi być dodatni, było: 0

        // DOBRA PRAKTYKA: walidacja argumentów konstruktora (step <= 0 → wyjątek NATYCHMIAST) to ta sama zasada
        //   co w Oop02Constructors — lepiej zgłosić błąd w miejscu jego powstania niż dostać dziwne zachowanie
        //   (np. nieskończoną pętlę przy step == 0) gdzieś daleko, wewnątrz next().
    }

    // =================================================================================================
    // 3. KONTRAKT Iterator RĘCZNIE — for-each to tylko "cukier"
    // =================================================================================================

    /** 3. for (x : iterowalne) to skrót kompilatora na dokładnie ten kod, który piszemy tu ręcznie. */
    static void iteratorContractManually() {
        section("3. Kontrakt Iterator z bliska — for-each to tylko cukier składniowy");

        IntRange tiny = new IntRange(1, 4, 1);   // 1, 2, 3
        Iterator<Integer> it = tiny.iterator();
        while (it.hasNext()) {
            System.out.println("element: " + it.next());
        }
        // WYNIK: element: 1
        // WYNIK: element: 2
        // WYNIK: element: 3

        note("Powyższa pętla while (hasNext) / next() to DOKŁADNIE to, na co for-each rozwija się \"pod maską\" —");
        note("for (int v : tiny) jest tylko wygodniejszym zapisem tego samego kodu z jawnym Iteratorem.");

        Iterator<Integer> exhausted = tiny.iterator();   // ŚWIEŻY iterator — ten z pętli wyżej jest już wyczerpany
        while (exhausted.hasNext()) {
            exhausted.next();
        }
        expectThrows("next() po wyczerpaniu iteratora", exhausted::next);
        // WYNIK: ✔ next() po wyczerpaniu iteratora → rzucono NoSuchElementException: IntRange: brak kolejnego elementu (current=4, to=4)

        // PUŁAPKA: next() PRZED sprawdzeniem hasNext() to częsty błąd początkujących — next() ma prawo rzucić
        //   NoSuchElementException, gdy elementów nie ma. Zawsze pytaj hasNext() jako pierwsze (jak w pętli while wyżej).
    }

    // =================================================================================================
    // 4. Iterator.remove() — DOMYŚLNIE NIEWSPIERANE
    // =================================================================================================

    /** 4. remove() ma domyślną implementację (Java 8, default method) w samym interfejsie Iterator — rzuca wyjątek. */
    static void removeUnsupportedByDefault() {
        section("4. Iterator.remove() — domyślnie rzuca UnsupportedOperationException");

        IntRange range = new IntRange(1, 4, 1);
        Iterator<Integer> it = range.iterator();
        it.next();   // "skonsumuj" jeden element — remove() zwykle usuwa OSTATNIO zwrócony przez next()

        expectThrows("it.remove() — RangeIterator nie nadpisuje remove()", it::remove);
        // WYNIK: ✔ it.remove() — RangeIterator nie nadpisuje remove() → rzucono UnsupportedOperationException: remove

        note("Interfejs Iterator wymaga TYLKO next() (hasNext ma sens domyślny \"zawsze pytaj\", ale i tak trzeba go");
        note("dać) — remove() ma gotową implementację wbudowaną w sam interfejs (default method), która po prostu");
        note("rzuca UnsupportedOperationException. Klasy, dla których usuwanie nie ma sensu (jak liczby z zakresu —");
        note("czego niby \"usuwanie\" liczby 3 z IntRange miałoby dotyczyć?), po prostu NIE nadpisują remove()");
        note("i dostają to bezpieczne zachowanie za darmo, zamiast musieć samodzielnie rzucać wyjątek.");
    }

    // =================================================================================================
    // 5. PUŁAPKA: ITERATOR JAKO "TEN SAM OBIEKT" CO Iterable
    // =================================================================================================

    /**
     * BuggySelfIterating = celowo ZŁY przykład: klasa implementuje NARAZ Iterable I Iterator, a iterator()
     * zwraca "this". To działa przy PIERWSZYM przebiegu, ale psuje wszystko przy drugim (i przy równoległym).
     */
    static final class BuggySelfIterating implements Iterable<Integer>, Iterator<Integer> {
        private int current = 0;
        private final int to;

        BuggySelfIterating(int to) {
            this.to = to;
        }

        @Override
        public Iterator<Integer> iterator() {
            return this;   // PUŁAPKA: zwraca SAM SIEBIE zamiast nowej, niezależnej instancji
        }

        @Override
        public boolean hasNext() {
            return current < to;
        }

        @Override
        public Integer next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return current++;
        }
    }

    /** 5. iterator() MUSI zwracać nową instancję za każdym wywołaniem — inaczej drugi przebieg for-each jest pusty. */
    static void freshIteratorEachCall() {
        section("5. Pułapka: iterator() musi zwracać NOWY obiekt za każdym razem");

        BuggySelfIterating buggy = new BuggySelfIterating(3);
        List<Integer> firstPass = new ArrayList<>();
        for (int v : buggy) {
            firstPass.add(v);
        }
        show("BuggySelfIterating: pierwszy przebieg", firstPass);
        // WYNIK: BuggySelfIterating: pierwszy przebieg → [0, 1, 2]

        List<Integer> secondPass = new ArrayList<>();
        for (int v : buggy) {
            secondPass.add(v);
        }
        show("BuggySelfIterating: DRUGI przebieg (ten sam wyczerpany obiekt!)", secondPass);
        // WYNIK: BuggySelfIterating: DRUGI przebieg (ten sam wyczerpany obiekt!) → []

        IntRange range = new IntRange(1, 4, 1);
        List<Integer> rangeFirst = new ArrayList<>();
        for (int v : range) {
            rangeFirst.add(v);
        }
        List<Integer> rangeSecond = new ArrayList<>();
        for (int v : range) {
            rangeSecond.add(v);
        }
        show("IntRange: pierwszy przebieg", rangeFirst);
        show("IntRange: drugi przebieg (ten sam obiekt, ale DZIAŁA)", rangeSecond);
        // WYNIK: IntRange: pierwszy przebieg → [1, 2, 3]
        // WYNIK: IntRange: drugi przebieg (ten sam obiekt, ale DZIAŁA) → [1, 2, 3]

        // PUŁAPKA: BuggySelfIterating "łączy" rolę Iterable (płyta DVD) i Iterator (pilot) w JEDNYM obiekcie —
        //   ma tylko JEDEN wspólny stan (current), więc drugi for-each dostaje pilot, który już jest na końcu
        //   filmu. IntRange.iterator() (sekcja 2) tworzy NOWY RangeIterator za każdym razem — każdy przebieg
        //   startuje od zera, niezależnie od pozostałych (można by nawet iterować RÓWNOLEGLE, na dwóch pilotach naraz).
    }

    // =================================================================================================
    // 6. Fibonacci — Iterable<Long> DO LIMITU
    // =================================================================================================

    /** Fibonacci = kolejne wyrazy ciągu (0, 1, 1, 2, 3, 5, 8, ...) aż do limitu WŁĄCZNIE. Iterator: klasa anonimowa. */
    static final class Fibonacci implements Iterable<Long> {
        private final long limit;

        Fibonacci(long limit) {
            this.limit = limit;
        }

        @Override
        public Iterator<Long> iterator() {
            return new Iterator<Long>() {   // anonimowa klasa — inny styl niż nazwana RangeIterator (porównanie: sekcja 7)
                private long current = 0;
                private long next = 1;

                @Override
                public boolean hasNext() {
                    return current <= limit;
                }

                @Override
                public Long next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException("Fibonacci: brak kolejnego wyrazu ≤ " + limit);
                    }
                    long value = current;
                    long newNext = current + next;
                    current = next;
                    next = newNext;
                    return value;
                }
            };
        }
    }

    /** 6. Fibonacci w for-each — long, bo wyrazy szybko rosną (t15_numbers/Numbers05IntegerTricks: przepełnienie int). */
    static void fibonacciIterable() {
        section("6. Fibonacci — Iterable<Long> generowany leniwie, do limitu");

        List<Long> terms = new ArrayList<>();
        for (long f : new Fibonacci(50)) {
            terms.add(f);
        }
        show("wyrazy Fibonacciego ≤ 50", terms);
        // WYNIK: wyrazy Fibonacciego ≤ 50 → [0, 1, 1, 2, 3, 5, 8, 13, 21, 34]

        note("Fibonacci nie trzyma listy wyrazów w polu — każdy next() WYLICZA kolejny wyraz z dwóch poprzednich");
        note("(current, next) i od razu go zapomina po zwróceniu. Dla limitu = milion wyrazów zajmuje tyle samo");
        note("pamięci co dla limitu = 10 — to sedno \"leniwego\" generowania (podobnie jak Stream, t16_streams).");
    }

    // =================================================================================================
    // 7. ANONIMOWA KLASA ITERATORA KONTRA OSOBNA, NAZWANA KLASA
    // =================================================================================================

    /** MultiplesOfThreeIterator = ta sama logika co niżej (anonimowa), ale jako osobna, nazwana klasa. */
    static final class MultiplesOfThreeIterator implements Iterator<Integer> {
        private int current = 0;
        private final int limit;

        MultiplesOfThreeIterator(int limit) {
            this.limit = limit;
        }

        @Override
        public boolean hasNext() {
            return current <= limit;
        }

        @Override
        public Integer next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            int value = current;
            current += 3;
            return value;
        }
    }

    /** Wersja NAZWANA: Iterable to interfejs funkcyjny (jedna metoda abstrakcyjna) — można go zaimplementować lambdą! */
    static Iterable<Integer> multiplesOfThreeNamed(int limit) {
        return () -> new MultiplesOfThreeIterator(limit);   // lambda = ciało metody iterator() (t13_lambdas)
    }

    /** Wersja ANONIMOWA: Iterable i Iterator zapisane w miejscu użycia, bez osobnych nazw typów. */
    static Iterable<Integer> multiplesOfThreeAnonymous(int limit) {
        return new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    private int current = 0;

                    @Override
                    public boolean hasNext() {
                        return current <= limit;
                    }

                    @Override
                    public Integer next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        int value = current;
                        current += 3;
                        return value;
                    }
                };
            }
        };
    }

    /** 7. Ten sam efekt, dwa różne style zapisu — porównanie czytelności i możliwości ponownego użycia. */
    static void anonymousVsNamedComparison() {
        section("7. Anonimowa klasa Iteratora kontra osobna, nazwana klasa");

        List<Integer> viaNamed = new ArrayList<>();
        for (int v : multiplesOfThreeNamed(9)) {
            viaNamed.add(v);
        }
        show("wielokrotności 3 do 9 (nazwana klasa + lambda)", viaNamed);
        // WYNIK: wielokrotności 3 do 9 (nazwana klasa + lambda) → [0, 3, 6, 9]

        List<Integer> viaAnonymous = new ArrayList<>();
        for (int v : multiplesOfThreeAnonymous(9)) {
            viaAnonymous.add(v);
        }
        show("wielokrotności 3 do 9 (anonimowa klasa)", viaAnonymous);
        // WYNIK: wielokrotności 3 do 9 (anonimowa klasa) → [0, 3, 6, 9]

        note("Ciekawostka: Iterable<T> ma DOKŁADNIE JEDNĄ metodę abstrakcyjną (iterator()) — to interfejs funkcyjny!");
        note("Dlatego multiplesOfThreeNamed mógł go zaimplementować lambdą: () -> new MultiplesOfThreeIterator(limit)");
        note("(pełny wykład lambd i interfejsów funkcyjnych: t13_lambdas).");

        note("NAZWANA klasa (MultiplesOfThreeIterator): więcej kodu, ale MOŻNA ją przetestować osobno, użyć w wielu");
        note("miejscach, dać jej czytelną nazwę (RangeIterator z sekcji 2 — ten sam pomysł). ANONIMOWA: mniej kodu");
        note("w miejscu użycia, ale nie ma własnej nazwy typu — nie da się jej użyć nigdzie indziej ani testować");
        note("bezpośrednio. Zasada: jednorazowa, prosta logika → anonimowa (albo lambda). Reużywalna, złożona,");
        note("warta osobnego testu → nazwana klasa.");
    }

    // =================================================================================================
    // 8. KLASA WEWNĘTRZNA (inner) SIĘGAJĄCA PO STAN OBIEKTU ZEWNĘTRZNEGO
    // =================================================================================================

    /**
     * EvenOnlyView = widok na cudzą listę pokazujący TYLKO parzyste elementy, bez kopiowania danych.
     * EvenIterator to klasa WEWNĘTRZNA (inner, NIE static) — istnieje tylko "przy" konkretnym obiekcie
     * EvenOnlyView i dzięki temu widzi jego pole source BEZ przyjmowania go w swoim konstruktorze
     * (t06_oop_basics/Oop07NestedClasses: inner class ma niejawną referencję do Outer.this).
     */
    static final class EvenOnlyView implements Iterable<Integer> {
        private final List<Integer> source;

        EvenOnlyView(List<Integer> source) {
            this.source = source;
        }

        @Override
        public Iterator<Integer> iterator() {
            return new EvenIterator();
        }

        private final class EvenIterator implements Iterator<Integer> {
            private int index = 0;

            EvenIterator() {
                skipOdds();
            }

            private void skipOdds() {
                while (index < source.size() && source.get(index) % 2 != 0) {   // source = pole Outer.this, bez przekazywania
                    index++;
                }
            }

            @Override
            public boolean hasNext() {
                return index < source.size();
            }

            @Override
            public Integer next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("EvenOnlyView: brak kolejnej parzystej liczby");
                }
                int value = source.get(index);
                index++;
                skipOdds();
                return value;
            }
        }
    }

    /** 8. Klasa wewnętrzna (inner) ma sens, gdy Iterator NAPRAWDĘ potrzebuje dostępu do stanu konkretnego obiektu. */
    static void innerAccessingOuterState() {
        section("8. Klasa wewnętrzna (inner) sięgająca po stan obiektu zewnętrznego");

        EvenOnlyView view = new EvenOnlyView(List.of(1, 2, 3, 4, 5, 6, 7, 8));
        List<Integer> evens = new ArrayList<>();
        for (int v : view) {
            evens.add(v);
        }
        show("tylko parzyste (przez inner class EvenIterator)", evens);
        // WYNIK: tylko parzyste (przez inner class EvenIterator) → [2, 4, 6, 8]

        // JAK TO DZIAŁA: EvenIterator NIE MA pola na source — czyta je bezpośrednio z otaczającego obiektu
        //   EvenOnlyView (niejawne EvenOnlyView.this.source). Gdyby EvenIterator był static nested (jak
        //   RangeIterator w sekcji 2), musiałby dostać source przez konstruktor — co też działa, ale RangeIterator
        //   i tak nie potrzebuje "obiektu IntRange", tylko trzech liczb (from/to/step), więc static nested
        //   wystarczy. Zasada: klasa NIE MUSI być inner tylko dlatego, że jest "w środku" — inner ma sens, gdy
        //   naprawdę potrzebujesz żywego dostępu do POLA konkretnego obiektu zewnętrznego, nie tylko jego danych.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Iterable<T>: jedna metoda — Iterator<T> iterator(). Interfejs funkcyjny → można go dać jako lambdę.
     *   • Iterator<T>: hasNext(), next() (rzuca NoSuchElementException na końcu), remove() (domyślnie: wyjątek).
     *   • iterator() MUSI zwracać NOWY obiekt za każdym wywołaniem — inaczej drugi for-each dostaje wyczerpany stan.
     *   • Anonimowa klasa: krótko, jednorazowo, bez testowania osobno. Nazwana klasa: reużywalna, testowalna.
     *   • Inner (niestatyczna) klasa: gdy Iterator naprawdę potrzebuje pola KONKRETNEGO obiektu zewnętrznego.
     *   • Generowanie "leniwe" (IntRange, Fibonacci): next() liczy JEDEN element na żądanie, nie trzyma wszystkich naraz.
     *   • for (x : iterowalne) to cukier składniowy na: Iterator<T> it = iterowalne.iterator(); while(it.hasNext())...
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego Iterable.iterator() powinno zwracać NOWĄ instancję Iteratora przy każdym wywołaniu, a nie
     *      zawsze ten sam obiekt?
     *   2. Co wypisze:
     *          Iterator<Integer> it = new IntRange(5, 8, 1).iterator();
     *          System.out.println(it.next());
     *          System.out.println(it.next());
     *          System.out.println(it.hasNext());
     *   3. ZNAJDŹ BŁĄD:
     *          class Broken implements Iterable<Integer>, Iterator<Integer> {
     *              private int i = 0;
     *              public Iterator<Integer> iterator() { return this; }
     *              public boolean hasNext() { return i < 3; }
     *              public Integer next() { return i++; }
     *          }
     *          Broken b = new Broken();
     *          for (int x : b) { }
     *          for (int x : b) { System.out.println(x); }   // nic się nie wypisze!
     *   4. Co wypisze:  int count = 0; for (long f : new Fibonacci(1)) count++; System.out.println(count);  ?
     *   5. Dlaczego Iterator.remove() domyślnie rzuca wyjątek, zamiast po prostu nic nie robić?
     *   6. Kiedy warto użyć anonimowej klasy do Iteratora, a kiedy osobnej, nazwanej klasy?
     *   7. Dlaczego EvenIterator (sekcja 8) mógł odwołać się do source, mimo że nie przyjął go w konstruktorze?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: suma IntRange", 25, () -> exercise1(new IntRange(1, 10, 2)));
        Check.equal("ćw. 2: liczba wyrazów Fibonacciego ≤ limit", 10, () -> exercise2(new Fibonacci(50)));
        Check.equal("ćw. 3: remove() na RangeIterator rzuca wyjątek", true, () -> exercise3(new IntRange(0, 5, 1)));
        Check.equal("ćw. 4: kwadraty 1..n (PRZEPISZ anonimową klasę na nazwaną)", List.of(1, 4, 9, 16),
                () -> collectSquares(exercise4(4)));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 25, () -> solution1(new IntRange(1, 10, 2)));
        Check.equal("ćw. 2 (wzorzec)", 10, () -> solution2(new Fibonacci(50)));
        Check.equal("ćw. 3 (wzorzec)", true, () -> solution3(new IntRange(0, 5, 1)));
        Check.equal("ćw. 4 (wzorzec)", List.of(1, 4, 9, 16), () -> collectSquares(solution4(4)));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /** Pomocnicze do ćwiczenia 4: zbiera dowolny Iterable{@code <Integer>} do listy (żeby porównać wynik w Check). */
    static List<Integer> collectSquares(Iterable<Integer> squares) {
        List<Integer> result = new ArrayList<>();
        for (int s : squares) {
            result.add(s);
        }
        return result;
    }

    /** ĆWICZENIE 1 (łatwe): zsumuj wszystkie elementy podanego IntRange za pomocą pętli for-each (nie strumienia). */
    static int exercise1(IntRange range) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /** ĆWICZENIE 2 (łatwe): zwróć liczbę wyrazów podanego Fibonacci (ile razy for-each "wykona" ciało pętli). */
    static int exercise2(Fibonacci fibonacci) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 3 (średnie): pobierz iterator z range, wywołaj next() raz, a potem spróbuj remove() w try/catch.
     * Zwróć true, jeśli remove() rzuciło UnsupportedOperationException (czyli RangeIterator zachował się zgodnie
     * z domyślną implementacją z sekcji 4), false w przeciwnym razie.
     */
    static boolean exercise3(IntRange range) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze, PRZEPISZ): poniższa metoda zwraca Iterable{@code <Integer>} kwadratów liczb
     * 1..n przez ANONIMOWĄ klasę:
     * <pre>{@code
     * static Iterable<Integer> squaresAnonymous(int n) {
     *     return new Iterable<Integer>() {
     *         public Iterator<Integer> iterator() {
     *             return new Iterator<Integer>() {
     *                 private int i = 1;
     *                 public boolean hasNext() { return i <= n; }
     *                 public Integer next() {
     *                     if (!hasNext()) throw new NoSuchElementException();
     *                     int value = i * i;
     *                     i++;
     *                     return value;
     *                 }
     *             };
     *         }
     *     };
     * }
     * }</pre>
     * Przepisz ją tak, by iterator() był OSOBNĄ, NAZWANĄ klasą (jak RangeIterator w sekcji 2) zamiast anonimowej.
     * Podpowiedź: zdefiniuj static final class SquaresIterator implements Iterator{@code <Integer>} obok tej metody
     * (w sekcji ROZWIĄZANIA — tu wystarczy sama metoda zwracająca Iterable poprzez tę klasę).
     */
    static Iterable<Integer> exercise4(int n) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(IntRange range) {
        int sum = 0;
        for (int n : range) {
            sum += n;
        }
        return sum;
    }

    static int solution2(Fibonacci fibonacci) {
        int count = 0;
        for (long ignored : fibonacci) {
            count++;
        }
        return count;
    }

    static boolean solution3(IntRange range) {
        Iterator<Integer> it = range.iterator();
        it.next();
        try {
            it.remove();
            return false;
        } catch (UnsupportedOperationException e) {
            return true;
        }
    }

    /** Nazwana klasa Iteratora — rozwiązanie ćwiczenia 4 (zamiast anonimowej z treści zadania). */
    static final class SquaresIterator implements Iterator<Integer> {
        private int i = 1;
        private final int n;

        SquaresIterator(int n) {
            this.n = n;
        }

        @Override
        public boolean hasNext() {
            return i <= n;
        }

        @Override
        public Integer next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            int value = i * i;
            i++;
            return value;
        }
    }

    static Iterable<Integer> solution4(int n) {
        return () -> new SquaresIterator(n);   // Iterable jako interfejs funkcyjny (sekcja 7) — zwięźle i bez anonimowej klasy
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo różne pętle for-each (albo ta sama pętla wywołana dwa razy) muszą móc iterować NIEZALEŻNIE od
     *      siebie, każda od początku. Jeden wspólny obiekt-stan (jak w BuggySelfIterating) sprawia, że drugie
     *      użycie dostaje iterator już częściowo albo całkowicie wyczerpany przez pierwsze użycie.
     *   2. „5”, „6”, „true” — IntRange(5, 8, 1) to 5, 6, 7; po dwóch next() current = 7, a 7 < 8, więc hasNext
     *      wciąż zwraca true (jest jeszcze jeden element, 7, do pobrania).
     *   3. Broken implementuje NARAZ Iterable i Iterator, a iterator() zwraca "this" — jeden wspólny stan (i).
     *      Pierwszy for-each zużywa go do końca (i dochodzi do 3), drugi dostaje już wyczerpany obiekt (hasNext
     *      od razu zwraca false) — stąd brak wypisania czegokolwiek w drugiej pętli.
     *   4. „3” — Fibonacci(1) generuje wyrazy ≤ 1: 0, 1, 1 (trzeci wyraz to znowu 1, bo 0+1=1).
     *   5. Bo cicha akceptacja remove() na strukturze, która nie obsługuje usuwania (np. IntRange — "usuwanie"
     *      liczby z matematycznego zakresu nie ma sensu), dałaby fałszywe poczucie, że coś się wydarzyło, choć
     *      nic się nie zmieniło. Wyjątek jasno mówi: "ta operacja tu nie działa", zamiast milczeć.
     *   6. Anonimowa: prosta, jednorazowa logika, użyta w JEDNYM miejscu, bez potrzeby testowania jej osobno.
     *      Nazwana: logika reużywalna w wielu miejscach, dość złożona, żeby zasługiwała na własną nazwę i testy,
     *      albo potrzebująca własnego, nietrywialnego konstruktora (jak RangeIterator).
     *   7. EvenIterator to klasa WEWNĘTRZNA (inner, nie static) zagnieżdżona w EvenOnlyView — każda jej instancja
     *      jest nierozerwalnie związana z KONKRETNYM obiektem EvenOnlyView (utworzoną przez view.new EvenIterator()
     *      w praktyce ukryte za return new EvenIterator() wewnątrz metody instancyjnej) i ma niejawną referencję
     *      do jego pól, tak jakby source było też jej własnym polem.
     */
    // </editor-fold>
}
