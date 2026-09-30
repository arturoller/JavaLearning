package t07_inheritance_polymorphism;

import helpers.Check;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Kompozycja kontra dziedziczenie — "has-a" kontra "is-a", krucha klasa bazowa
 *        (composition = kompozycja; fragile base class = krucha klasa bazowa; delegation = delegowanie)
 *
 * W SKRÓCIE:
 *   Dziedziczenie (is-a) wiąże podklasę z klasą bazową NA ZAWSZE i BARDZO MOCNO — każda zmiana w klasie
 *   bazowej może po cichu zepsuć podklasę. Kompozycja (has-a) — trzymanie innego obiektu jako pola
 *   i przekazywanie mu wywołań (delegowanie) — jest luźniejsza i zwykle bezpieczniejsza. Zasada: "favor
 *   composition over inheritance" (wybieraj kompozycję zamiast dziedziczenia), ale dziedziczenie wciąż
 *   ma swoje miejsce, gdy relacja is-a jest naprawdę prawdziwa i stabilna.
 *
 * ANALOGIA: silnik w samochodzie.
 *   Samochód nie JEST silnikiem (is-a byłoby dziwne) — samochód MA silnik (has-a) i mu ZLECA pracę
 *   ("uruchom się", "przyspiesz"). Możesz wymienić silnik na inny (elektryczny) bez przebudowy całego
 *   samochodu — to elastyczność, której dziedziczenie nie daje.
 *
 * JAK TO DZIAŁA:
 *   // dziedziczenie (is-a) — CountingSet JEST HashSet, dziedziczy WSZYSTKO, także błędy założeń
 *   class CountingSet<T> extends HashSet<T> { ... }
 *
 *   // kompozycja (has-a) — CountingList2 MA listę w polu i deleguje do niej wywołania
 *   class CountingList2<T> { private final List<T> inner = new ArrayList<>(); ... }
 *
 * SŁÓWKA:
 *   composition = kompozycja (has-a, obiekt jako pole); delegation = delegowanie (przekazanie wywołania
 *   do "wewnętrznego" obiektu); fragile base class = krucha klasa bazowa (zmiana w bazie psuje podklasy
 *   w nieoczywisty sposób); forwarding = przekazywanie wywołań dalej; Liskov Substitution Principle (LSP) =
 *   zasada podstawienia Liskov (podklasa musi dać się użyć wszędzie tam, gdzie klasa bazowa, bez zaskoczeń).
 *
 * ZOBACZ TEŻ: t07_inheritance_polymorphism/Inherit01Basics (extends, is-a od podstaw),
 *             t07_inheritance_polymorphism/Inherit08Solid (LSP jako jedna z zasad SOLID, pełny obraz),
 *             t12_collections/Collections02Lists (ArrayList i implementacja List w pełnym rozdziale).
 * </pre>
 */
public class Inherit06CompositionVsInheritance {

    // ---------------------------------------------------------------------------------------------
    // Klasy przykładowe jako statyczne klasy zagnieżdżone — lekcja ma być samodzielna (patrz Inherit01).
    // ---------------------------------------------------------------------------------------------

    /**
     * CountingSet is-a HashSet (dziedziczenie). Chce liczyć DODANE elementy, więc nadpisuje add() i
     * addAll(). PUŁAPKA: HashSet SAM nie ma własnego addAll() — dziedziczy je po AbstractCollection,
     * a TO addAll() w pętli woła add() dla każdego elementu. Skoro nasze add() jest nadpisane, ta pętla
     * woła WŁAŚNIE naszą wersję — licznik rośnie RAZ w naszym addAll() i RAZ dla każdego elementu w add().
     */
    static class CountingSet<T> extends HashSet<T> {
        private int addCount = 0;

        @Override
        public boolean add(T element) {
            addCount++;
            return super.add(element);
        }

        @Override
        public boolean addAll(Collection<? extends T> elements) {
            addCount += elements.size();
            return super.addAll(elements);   // PUŁAPKA: super.addAll() to odziedziczone AbstractCollection.addAll(),
        }                                     // które WEWNĘTRZNIE woła add(e) dla KAŻDEGO elementu z osobna —
                                               // a add() jest NASZĄ nadpisaną wersją -> podwójne liczenie!

        int getAddCount() {
            return addCount;
        }
    }

    /**
     * CountingList2 has-a List (kompozycja). Trzyma "inner" jako pole i SAMA decyduje, jak liczyć —
     * nie zależy od tego, JAK ArrayList wewnętrznie implementuje addAll().
     */
    static class CountingList2<T> {
        private final List<T> inner = new ArrayList<>();   // has-a: kompozycja, nie dziedziczenie
        private int addCount = 0;

        boolean add(T element) {
            addCount++;
            return inner.add(element);       // delegowanie (forwarding): "zrób to, inner"
        }

        boolean addAll(Collection<? extends T> elements) {
            addCount += elements.size();
            return inner.addAll(elements);   // JEDNO zwiększenie licznika, bo nie wołamy własnego add()
        }

        int size() {
            return inner.size();             // delegowanie odczytu — nie "jesteśmy" listą, tylko ją MAMY
        }

        int getAddCount() {
            return addCount;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Liskov Substitution Principle: Square extends Rectangle — klasyczny przykład złamania LSP
    // ---------------------------------------------------------------------------------------------

    /** Rectangle = prostokąt z NIEZALEŻNĄ szerokością i wysokością — to jego "kontrakt". */
    static class Rectangle {
        protected int width;
        protected int height;

        Rectangle(int width, int height) {
            this.width = width;
            this.height = height;
        }

        void setWidth(int width) {
            this.width = width;
        }

        void setHeight(int height) {
            this.height = height;
        }

        int area() {
            return width * height;
        }
    }

    /**
     * Square is-a Rectangle (kwadrat "jest" prostokątem geometrycznie), ale musi WYMUSIĆ width == height —
     * więc setWidth() po cichu zmienia też height. To ŁAMIE oczekiwania kodu, który zna tylko Rectangle
     * i zakłada, że setWidth() zmienia WYŁĄCZNIE szerokość.
     */
    static class Square extends Rectangle {
        Square(int side) {
            super(side, side);
        }

        @Override
        void setWidth(int width) {
            this.width = width;
            this.height = width;   // PUŁAPKA: Rectangle nigdy tego nie obiecywał — zaskoczenie dla wołającego
        }

        @Override
        void setHeight(int height) {
            this.width = height;
            this.height = height;
        }
    }

    public static void main(String[] args) {
        title("Inherit06 — kompozycja kontra dziedziczenie: has-a kontra is-a");

        hasAVsIsA();               // has-a vs is-a = has-a kontra is-a
        fragileBaseClass();        // fragile base class = krucha klasa bazowa (pułapka addAll)
        compositionFix();          // composition fix = naprawa przez kompozycję
        jdkMistake();               // JDK mistake = pomyłka z samego JDK (Stack extends Vector)
        liskovViolation();         // Liskov violation = złamanie zasady Liskov
        favorComposition();         // favor composition = kiedy wybrać kompozycję, kiedy dziedziczenie
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. "HAS-A" KONTRA "IS-A"
    // =================================================================================================

    /** 1. CountingSet JEST HashSet (is-a) — CountingList2 MA ArrayList w polu (has-a). */
    static void hasAVsIsA() {
        section("1. \"has-a\" kontra \"is-a\"");

        CountingSet<String> a = new CountingSet<>();
        CountingList2<String> b = new CountingList2<>();
        show("a instanceof Collection (CountingSet JEST kolekcją)", a instanceof Collection);
        show("b instanceof Collection (CountingList2 NIE jest kolekcją — ma ją)", b instanceof Collection);
        // WYNIK: a instanceof Collection (CountingSet JEST kolekcją) → true
        // WYNIK: b instanceof Collection (CountingList2 NIE jest kolekcją — ma ją) → false

        // DOBRA PRAKTYKA: zadaj pytanie "czy X NAPRAWDĘ JEST Y (is-a), czy tylko UŻYWA Y (has-a)?".
        //   Samochód MA silnik (has-a) — nie JEST silnikiem. Pies JEST zwierzęciem (is-a) — to naprawdę ta sama rzecz.
    }

    // =================================================================================================
    // 2. KRUCHA KLASA BAZOWA: PUŁAPKA PODWÓJNEGO LICZENIA
    // =================================================================================================

    /**
     * 2. CountingSet.addAll(zbiór) woła super.addAll(zbiór) — a HashSet NIE MA własnego addAll(), więc
     * to wywołanie trafia do AbstractCollection.addAll(), które W PĘTLI woła add(e) dla każdego elementu.
     * Ta pętla woła NASZĄ nadpisaną wersję add() (polimorfizm!) — addCount rośnie RAZ w addAll() i RAZ
     * (dla każdego elementu) w add().
     */
    static void fragileBaseClass() {
        section("2. Krucha klasa bazowa: CountingSet liczy DWA razy");

        CountingSet<String> set = new CountingSet<>();
        set.addAll(List.of("a", "b", "c"));
        show("set.size() (elementów naprawdę dodanych)", set.size());
        show("set.getAddCount() (licznik — POWINIEN być 3!)", set.getAddCount());
        // WYNIK: set.size() (elementów naprawdę dodanych) → 3
        // WYNIK: set.getAddCount() (licznik — POWINIEN być 3!) → 6

        // PUŁAPKA: addCount wyszedł 6, nie 3 — bo HashSet w ogóle NIE nadpisuje addAll(): dziedziczy je
        //   po AbstractCollection, gdzie jest ono zaimplementowane PRZEZ wielokrotne wołanie add(...).
        //   To szczegół implementacji, którego nie widać w oficjalnym kontrakcie interfejsu Collection —
        //   nasza podklasa "wie za dużo" o tym, JAK HashSet działa w środku, i się przez to myli.
        // DOBRA PRAKTYKA: nigdy nie zakładaj, że jedna metoda klasy bazowej jest (albo NIE jest)
        //   zaimplementowana przez wywołanie innej — to szczegół, który może się różnić między klasami
        //   kolekcji (np. ArrayList.addAll() NIE woła add() — kopiuje elementy bezpośrednio) i między
        //   wersjami JDK.
    }

    // =================================================================================================
    // 3. NAPRAWA PRZEZ KOMPOZYCJĘ + DELEGOWANIE
    // =================================================================================================

    /** 3. CountingList2 SAMA decyduje, kiedy zwiększyć licznik — nie zależy od wewnętrznej implementacji ArrayList. */
    static void compositionFix() {
        section("3. Naprawa: kompozycja + delegowanie (forwarding)");

        CountingList2<String> list = new CountingList2<>();
        list.addAll(List.of("a", "b", "c"));
        show("list.size()", list.size());
        show("list.getAddCount() (poprawnie: 3)", list.getAddCount());
        // WYNIK: list.size() → 3
        // WYNIK: list.getAddCount() (poprawnie: 3) → 3

        // DOBRA PRAKTYKA: kompozycja + delegowanie daje PEŁNĄ kontrolę nad tym, co się liczy i jak —
        //   kosztem tego, że trzeba samemu napisać (delegować) każdą metodę, której potrzebujemy.
    }

    // =================================================================================================
    // 4. POMYŁKA Z SAMEGO JDK: Stack extends Vector
    // =================================================================================================

    /**
     * 4. java.util.Stack dziedziczy po java.util.Vector — to znany błąd projektowy z wczesnej Javy.
     * Stack POWINIEN był mieć TYLKO push/pop/peek (stos = LIFO), a przez dziedziczenie ma też WSZYSTKIE
     * metody Vector (np. get(index), insertElementAt(...)) — można "zepsuć" kolejność stosu z zewnątrz,
     * mimo że Stack z założenia miał tego zabraniać.
     */
    static void jdkMistake() {
        section("4. Pomyłka z samego JDK: Stack extends Vector");

        java.util.Stack<Integer> stack = new java.util.Stack<>();
        stack.push(1);
        stack.push(2);
        stack.push(3);
        stack.add(0, 99);   // metoda Z VECTOR, nie z "kontraktu stosu" — wstawia na SAM DÓŁ, łamiąc LIFO!
        show("stack (od dołu do góry — 99 nie powinno dać się tak wstawić)", stack);
        show("stack.pop() (powinno zdjąć 3 — i faktycznie zdejmuje)", stack.pop());
        // WYNIK: stack (od dołu do góry — 99 nie powinno dać się tak wstawić) → [99, 1, 2, 3]
        // WYNIK: stack.pop() (powinno zdjąć 3 — i faktycznie zdejmuje) → 3

        // DOBRA PRAKTYKA: gdyby Stack powstawał dziś, użyłby kompozycji (pole typu Deque/ArrayList) i
        //   udostępniał TYLKO push/pop/peek. Współczesna alternatywa: java.util.ArrayDeque jako stos
        //   (pełny rozdział o kolekcjach: t12_collections/Collections06QueuesDeques).
    }

    // =================================================================================================
    // 5. ZŁAMANIE ZASADY LISKOV: Square extends Rectangle
    // =================================================================================================

    /** Kod, który zna TYLKO Rectangle i ufa jego kontraktowi: "setWidth zmienia WYŁĄCZNIE szerokość". */
    static int areaAfterWideningTo(Rectangle r, int newWidth) {
        r.setWidth(newWidth);
        return r.area();
    }

    /**
     * 5. Square jest geometrycznie prostokątem (is-a wydaje się naturalne), ale jako klasa Java ŁAMIE
     * zasadę podstawienia Liskov: kod poprawny dla KAŻDEGO Rectangle (areaAfterWideningTo) daje ZASKAKUJĄCY
     * wynik dla Square, bo setWidth() po cichu zmienia też height.
     */
    static void liskovViolation() {
        section("5. Złamanie zasady Liskov: Square extends Rectangle");

        Rectangle rect = new Rectangle(2, 5);
        show("areaAfterWideningTo(rect, 10) — zwykły Rectangle: 10 × 5", areaAfterWideningTo(rect, 10));
        // WYNIK: areaAfterWideningTo(rect, 10) — zwykły Rectangle: 10 × 5 → 50

        Rectangle square = new Square(5);   // upcasting: kod myśli, że dostał "zwykły" Rectangle
        show("areaAfterWideningTo(square, 10) — Square: OCZEKIWANO 10 × 5 = 50", areaAfterWideningTo(square, 10));
        // WYNIK: areaAfterWideningTo(square, 10) — Square: OCZEKIWANO 10 × 5 = 50 → 100

        // PUŁAPKA: areaAfterWideningTo dostało Square przebrane za Rectangle (upcasting) — kod NIE WIE
        //   i NIE MOŻE wiedzieć, że to Square. Mimo to wynik jest INNY niż dla "zwykłego" Rectangle
        //   (100 zamiast oczekiwanych 50), bo setWidth() dla Square po cichu zmienia też height.
        //   To właśnie jest złamanie LSP: podklasa nie daje się bezpiecznie użyć wszędzie tam, gdzie klasa bazowa.
        // DOBRA PRAKTYKA: gdy podklasa musi ZAWĘZIĆ kontrakt bazowy (Square wymusza width==height), to
        //   sygnał, że relacja is-a jest "geometrycznie" prawdziwa, ale NIE jest prawdziwa jako typ Javy.
        //   Lepiej: osobne klasy Rectangle i Square, obie implementujące wspólny interfejs (np. HasArea),
        //   bez dziedziczenia jednej po drugiej.
    }

    // =================================================================================================
    // 6. KIEDY WYBRAĆ KOMPOZYCJĘ, KIEDY DZIEDZICZENIE
    // =================================================================================================

    /** 6. Podsumowanie zasady "favor composition over inheritance" — ale z zastrzeżeniem, kiedy dziedziczenie jest OK. */
    static void favorComposition() {
        section("6. \"Favor composition over inheritance\" — ale kiedy dziedziczenie jest OK?");

        note("Dziedziczenie ma sens, gdy: (1) relacja is-a jest NAPRAWDĘ prawdziwa i stabilna (Dog is-a "
                + "Animal), (2) podklasa nie ZWĘŻA kontraktu bazy (patrz Square), (3) klasa bazowa była "
                + "ZAPROJEKTOWANA do dziedziczenia (udokumentowana, stabilne API — jak Shape z Inherit03).");
        // WYNIK: ℹ Dziedziczenie ma sens, gdy: (1) relacja is-a jest NAPRAWDĘ prawdziwa i stabilna (Dog is-a Animal), (2) podklasa nie ZWĘŻA kontraktu bazy (patrz Square), (3) klasa bazowa była ZAPROJEKTOWANA do dziedziczenia (udokumentowana, stabilne API — jak Shape z Inherit03).

        note("Kompozycja jest bezpieczniejsza, gdy: chcesz tylko PONOWNIE UŻYĆ kodu (jak ArrayList w "
                + "CountingList2), klasa bazowa może się zmieniać niezależnie od Ciebie, albo potrzebujesz "
                + "WIELU 'ról' naraz (do tego zwykle lepiej pasują interfejsy — Inherit04).");
        // WYNIK: ℹ Kompozycja jest bezpieczniejsza, gdy: chcesz tylko PONOWNIE UŻYĆ kodu (jak ArrayList w CountingList2), klasa bazowa może się zmieniać niezależnie od Ciebie, albo potrzebujesz WIELU 'ról' naraz (do tego zwykle lepiej pasują interfejsy — Inherit04).

        // DOBRA PRAKTYKA: pytanie kontrolne przed napisaniem "extends": "czy KAŻDY możliwy obiekt podklasy
        //   da się bezpiecznie użyć wszędzie tam, gdzie oczekiwany jest obiekt klasy bazowej?" (LSP).
        //   Jeśli odpowiedź brzmi "nie zawsze" — sięgnij po kompozycję.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • is-a (dziedziczenie, extends) — podklasa NAPRAWDĘ JEST rodzajem klasy bazowej, dziedziczy
     *     WSZYSTKO, także błędy i szczegóły implementacji.
     *   • has-a (kompozycja) — klasa TRZYMA inny obiekt jako pole i deleguje mu wywołania (forwarding);
     *     pełna kontrola, ale trzeba samemu napisać każdą delegowaną metodę.
     *   • krucha klasa bazowa: nadpisanie metody, która wewnątrz woła INNĄ nadpisaną metodę (jak
     *     odziedziczone AbstractCollection.addAll wołające add), prowadzi do trudnych do przewidzenia błędów.
     *   • Stack extends Vector to znana pomyłka projektowa z JDK — Stack dostał metody, których stos
     *     nigdy nie powinien mieć.
     *   • Square extends Rectangle łamie zasadę podstawienia Liskov (LSP): kod poprawny dla każdego
     *     Rectangle przestaje działać poprawnie dla Square.
     *   • "Favor composition over inheritance" — ale dziedziczenie nadal ma sens dla stabilnych,
     *     zaprojektowanych do tego hierarchii (jak Shape z Inherit03).
     *
     * PYTANIA KONTROLNE:
     *   1. Jak sprawdzić w kodzie (bez czytania dokumentacji), czy klasa A "ma" B, czy "jest" B?
     *   2. Co wypisze:  CountingSet<String> s = new CountingSet<>(); s.addAll(List.of("x", "y")); System.out.println(s.getAddCount());  ?
     *   3. ZNAJDŹ BŁĄD (projektowy, nie kompilacji): dlaczego java.util.Stack extends Vector jest
     *      uważane za pomyłkę, mimo że kod się kompiluje i działa?
     *   4. Dlaczego Square extends Rectangle łamie zasadę Liskov, skoro geometrycznie kwadrat JEST prostokątem?
     *   5. Co daje CountingList2 (kompozycja), czego nie dawał CountingSet (dziedziczenie), w kontekście addAll()?
     *   6. Podaj przykład sytuacji, w której dziedziczenie JEST dobrym wyborem.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");

        Rectangle plainRect = new Rectangle(3, 4);
        Rectangle square = new Square(6);

        Check.equal("ćw. 1: pole zwykłego prostokąta", 12, () -> rectangleArea(plainRect));
        Check.equal("ćw. 2: czy setWidth Square zmienia też height (LSP violation)", true,
                () -> widthChangesHeight(square));
        Check.equal("ćw. 3 (PRZEPISZ): suma dodanych elementów przez bezpieczne, kompozycyjne opakowanie",
                3, () -> addAllSafely(List.of("x", "y", "z")));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 12, () -> solution1(plainRect));
        Check.equal("ćw. 2 (wzorzec)", true, () -> solution2(square));
        Check.equal("ćw. 3 (wzorzec)", 3, () -> solution3(List.of("x", "y", "z")));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 3 OK, ✘ 0 BŁĄD
    }

    /** ĆWICZENIE 1 (łatwe): zwróć pole prostokąta (area()) — na rozgrzewkę, bez pułapek. */
    static int rectangleArea(Rectangle r) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (średnie): sprawdź, czy dla podanego prostokąta wywołanie setWidth(newWidth) zmienia
     * TAKŻE height (czyli czy to Square, złamanie LSP) — porównaj height przed i po.
     * Podpowiedź: zapamiętaj height PRZED, wywołaj setWidth z dowolną inną wartością, porównaj z height PO.
     */
    static boolean widthChangesHeight(Rectangle r) {
        // stub boolean: neutralny "return false" mógłby przypadkiem zdać test, więc zaczynamy od wyjątku
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze, PRZEPISZ): sekcja 2 pokazała, że dziedziczenie po HashSet i nadpisanie
     * add()/addAll() jest kruche:
     * <pre>{@code
     * // PRZED (krucha klasa bazowa): CountingSet extends HashSet — addAll liczy PODWÓJNIE
     * class CountingSet<T> extends HashSet<T> {
     *     public boolean addAll(Collection<? extends T> c) { addCount += c.size(); return super.addAll(c); }
     *     // odziedziczone addAll() woła WEWNĘTRZNIE add() dla każdego elementu -> addCount rośnie drugi raz
     * }
     * }</pre>
     * PO: użyj CountingList2 (kompozycja) do dodania wszystkich elementów z listy i zwróć jej getAddCount().
     */
    static int addAllSafely(List<String> elements) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(Rectangle r) {
        return r.area();
    }

    static boolean solution2(Rectangle r) {
        int heightBefore = r.height;
        r.setWidth(heightBefore + 1);
        int heightAfter = r.height;
        return heightBefore != heightAfter;
    }

    static int solution3(List<String> elements) {
        CountingList2<String> list = new CountingList2<>();
        list.addAll(elements);
        return list.getAddCount();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. "A jest B" (is-a) zwykle oznacza `class A extends B` — obiekt A przechodzi test
     *      `a instanceof B == true`. "A ma B" (has-a) oznacza, że A trzyma obiekt typu B jako POLE
     *      (kompozycja) — `a instanceof B` jest false, bo A i B to niepowiązane typy.
     *   2. "4" — addAll dodaje addCount += 2 (rozmiar kolekcji), a super.addAll() woła add() DWA razy
     *      wewnętrznie, każde +1 do addCount -> razem 2 + 2 = 4, mimo że naprawdę dodano tylko 2 elementy.
     *   3. Stack, jako struktura LIFO (stos), NIE POWINIEN pozwalać na dostęp w dowolnym miejscu (jak
     *      get(index), insertElementAt) — a przez dziedziczenie po Vector dostał WSZYSTKIE jego metody,
     *      co pozwala złamać kolejność stosu z zewnątrz. Kod się kompiluje i "działa", ale kontrakt "to
     *      jest stos" nie jest w żaden sposób wymuszony przez typ.
     *   4. Bo Rectangle jako TYP JAVY obiecuje (choć niepisanie), że setWidth() zmienia WYŁĄCZNIE szerokość
     *      — Square musi złamać tę obietnicę, żeby zachować własny niezmiennik (width == height). Kod
     *      napisany dla Rectangle (jak areaAfterWideningTo) przestaje działać poprawnie dla Square, mimo
     *      że geometrycznie kwadrat "jest" prostokątem.
     *   5. CountingList2 SAMA decyduje, kiedy zwiększyć licznik (raz w addAll(), bez wołania własnego
     *      add() w pętli wewnątrz), więc nie zależy od tego, JAK dokładnie wewnętrzna lista implementuje
     *      addAll() — licznik zawsze będzie poprawny, nawet gdyby JDK zmieniło tę implementację.
     *   6. Np. Circle extends Shape (Inherit03/Inherit05): Circle NAPRAWDĘ jest Shape, nie zwęża jego
     *      kontraktu (area() zawsze da sensowny wynik dla dowolnego promienia), a Shape zostało od razu
     *      zaprojektowane jako klasa bazowa (konstruktor przyjmujący wspólne dane, metody abstrakcyjne
     *      do zaimplementowania). To bezpieczne, podręcznikowe dziedziczenie.
     */
    // </editor-fold>
}
