package t12_collections;

import helpers.Check;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Jak naprawdę działa HashMap — hashCode, rozpraszanie bitów, kubełki, kolizje, resize
 *        (bucket = kubełek; spread = rozproszenie (bitów); collision = kolizja; chain = łańcuch; resize = powiększenie)
 *
 * W SKRÓCIE:
 *   HashMap.put/get w czasie O(1) to nie magia — to hashCode() klucza przepuszczony przez prostą funkcję
 *   rozpraszającą, zamieniony na indeks małej tablicy (kubełków), plus lista/drzewo wewnątrz kubełka na wypadek,
 *   gdy dwa różne klucze trafią w to samo miejsce (kolizja). Zrozumienie tego mechanizmu tłumaczy naraz: dlaczego
 *   klucze MUSZĄ mieć dobry hashCode, dlaczego zmienny klucz jest niebezpieczny, i dlaczego HashSet to "HashMap
 *   przebrany za zbiór".
 *
 * ANALOGIA: szatnia z numerkami.
 *   hashCode(klucza) to numerek na Twoim żetonie. Szatniarz (HashMap) patrzy tylko na OSTATNIE CYFRY numerka,
 *   żeby wybrać odpowiednią półkę (kubełek) — stąd rozpraszanie bitów, żeby "ważne" wysokie cyfry też się liczyły.
 *   Jeśli dwa żetony mają numerki kończące się tak samo, trafiają na TĘ SAMĄ półkę — szatniarz i tak rozpozna
 *   Twoje rzeczy PO PEŁNYM numerku (equals), tylko musi przejrzeć trochę więcej rzeczy na tej półce.
 *
 * JAK TO DZIAŁA:
 *   1. int h = key.hashCode();
 *   2. int spread = h ^ (h >>> 16);       ← miesza górne 16 bitów z dolnymi (inaczej liczą się tylko dolne bity)
 *   3. int index = (capacity - 1) & spread;  ← capacity to potęga dwójki, więc to szybki odpowiednik % capacity
 *   4. w tablicy[index] jest kubełek: pusty, jeden węzeł, LISTA węzłów (kolizje) albo — od 8 węzłów — DRZEWO.
 *
 * SŁÓWKA:
 *   bucket = kubełek (jedna "przegródka" wewnętrznej tablicy); spread = rozproszenie bitów; collision = kolizja
 *   (dwa różne klucze, ten sam kubełek); chain = łańcuch (lista węzłów w kubełku); load factor = współczynnik
 *   wypełnienia; threshold = próg; resize = powiększenie i przepisanie tablicy; treeify = zamiana listy na drzewo.
 *
 * ZOBACZ TEŻ: t06_oop_basics/Oop05ObjectMethods (kontrakt equals/hashCode), t12_collections/Collections04Sets
 *             (HashSet od strony API), t12_collections/Collections05Maps (HashMap od strony API),
 *             t12_collections/Collections13Performance (dlaczego to wszystko daje O(1)), t31_jdk_toolbox/
 *             Toolbox02HashingSecurity (hashCode() to NIE to samo co kryptograficzny hash typu SHA-256!).
 * </pre>
 */
public class Collections11HashingInternals {

    public static void main(String[] args) {
        title("Collections11 — jak działa HashMap: hashCode, kubełki, kolizje, resize");

        stringHashCodeFormula();       // string hash code formula = wzór hashCode dla String
        hashToBucketIndex();           // hash to bucket index = od hashCode do indeksu kubełka
        sameHashCodeDifferentKeys();   // same hash code different keys = ten sam hashCode, różne klucze
        collisionsAndChaining();       // collisions and chaining = kolizje i łańcuch
        loadFactorAndResize();         // load factor and resize = współczynnik wypełnienia i resize
        constantHashCodeCollision();   // constant hash code collision = stały hashCode = jeden kubełek
        mutableKeyLostAfterChange();   // mutable key lost after change = zmienny klucz zgubiony po zmianie
        hashSetBuiltOnHashMap();       // hash set built on hash map = HashSet zbudowany na HashMap
        exercises();                    // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. WZÓR String.hashCode()
    // =================================================================================================

    /**
     * 1. String.hashCode() jest OPISANY w Javadoc jako konkretny wzór — nie szczegół implementacji, tylko
     * gwarancja: s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]. Ten sam wynik w każdej wersji JVM.
     */
    static void stringHashCodeFormula() {
        section("1. Wzór String.hashCode() — s[0]*31^(n-1) + ... + s[n-1]*31^0");

        show("\"\".hashCode() (pusty napis)", "".hashCode());
        // WYNIK: "".hashCode() (pusty napis) → 0

        show("\"a\".hashCode()", "a".hashCode());
        // WYNIK: "a".hashCode() → 97

        show("\"ab\".hashCode()", "ab".hashCode());
        // WYNIK: "ab".hashCode() → 3105

        int manual = 'a' * 31 + 'b';   // 'a'=97, 'b'=98 → 97*31 + 98
        show("ręcznie: 'a' * 31 + 'b'", manual);
        // WYNIK: ręcznie: 'a' * 31 + 'b' → 3105

        note("Ogólny wzór ma mnożnik 31 przy KAŻDYM kolejnym znaku (stąd 31^(n-1) przy pierwszym) — 31 wybrano,");
        note("bo to liczba pierwsza, a x*31 == (x<<5)-x (JIT umie to szybko policzyć), co dobrze rozprasza wyniki.");

        // DOBRA PRAKTYKA: skoro wzór jest gwarantowany przez specyfikację, MOŻNA na nim polegać (np. w testach) —
        //   w przeciwieństwie do Object.hashCode() (domyślny), który jest szczegółem implementacji JVM i może się
        //   różnić między uruchomieniami (t06_oop_basics/Oop05ObjectMethods).
    }

    // =================================================================================================
    // 2. OD hashCode DO INDEKSU KUBEŁKA
    // =================================================================================================

    /**
     * 2. HashMap sama nie ufa hashCode() w 100% — dodatkowo go "rozprasza" (spread), żeby przy MAŁEJ tablicy
     * (np. 16 kubełków, czyli liczy się tylko 4 dolne bity) wysokie bity hashCode też miały jakiś wpływ.
     */
    static void hashToBucketIndex() {
        section("2. hashCode → spread → indeks kubełka (przy capacity = 16)");

        int capacity = 16;   // domyślna pojemność nowo utworzonego HashMap
        for (String key : List.of("Ala", "Bartek", "Celina", "Darek")) {
            int h = key.hashCode();
            int spread = h ^ (h >>> 16);
            int index = (capacity - 1) & spread;
            show(key + ": hash=" + h + ", spread=" + spread + ", kubełek", index);
        }
        // WYNIK: Ala: hash=65910, spread=65911, kubełek → 7
        // WYNIK: Bartek: hash=1982616391, spread=1982604651, kubełek → 11
        // WYNIK: Celina: hash=2014750578, spread=2014764900, kubełek → 4
        // WYNIK: Darek: hash=65801947, spread=65801527, kubełek → 7

        note("\"Ala\" i \"Darek\" trafiły do TEGO SAMEGO kubełka (7) mimo zupełnie różnych hashCode — to zwykła");
        note("kolizja, nieunikniona przy skończonej liczbie kubełków (\"szufladkowanie\": więcej kluczy niż półek");
        note("prędzej czy później da powtórkę). Co się dzieje w takim kubełku — sekcja 4.");

        // DOBRA PRAKTYKA: (capacity - 1) & spread działa TYLKO dlatego, że capacity jest potęgą dwójki — maska
        //   bitowa wtedy odpowiada dokładnie operacji % capacity, ale jest szybsza (bez dzielenia).
    }

    // =================================================================================================
    // 3. "Aa" I "BB" — TEN SAM hashCode, RÓŻNE KLUCZE
    // =================================================================================================

    /** 3. Klasyczny przykład kolizji: krótkie napisy "Aa" i "BB" mają IDENTYCZNY hashCode, ale nie są sobie równe. */
    static void sameHashCodeDifferentKeys() {
        section("3. \"Aa\" i \"BB\" — różne napisy, identyczny hashCode");

        show("\"Aa\".hashCode()", "Aa".hashCode());
        show("\"BB\".hashCode()", "BB".hashCode());
        // WYNIK: "Aa".hashCode() → 2112
        // WYNIK: "BB".hashCode() → 2112

        show("\"Aa\".equals(\"BB\")", "Aa".equals("BB"));
        // WYNIK: "Aa".equals("BB") → false

        Map<String, String> map = new HashMap<>();
        map.put("Aa", "pierwsza");
        map.put("BB", "druga");
        show("rozmiar mapy (oba klucze się zmieściły)", map.size());
        show("map.get(\"Aa\")", map.get("Aa"));
        show("map.get(\"BB\")", map.get("BB"));
        // WYNIK: rozmiar mapy (oba klucze się zmieściły) → 2
        // WYNIK: map.get("Aa") → pierwsza
        // WYNIK: map.get("BB") → druga

        // JAK TO DZIAŁA: identyczny hashCode wysyła oba klucze do TEGO SAMEGO kubełka, ale wewnątrz kubełka
        //   HashMap rozróżnia je przez equals ("Aa".equals("BB") == false) — więc oba wpisy współistnieją
        //   poprawnie. hashCode tylko WSKAZUJE kubełek, to equals decyduje o tożsamości klucza (sekcja 4).
    }

    // =================================================================================================
    // 4. KOLIZJE I ŁAŃCUCH W KUBEŁKU (+ treeify)
    // =================================================================================================

    /** 4. Kubełek z wieloma kluczami to po prostu POŁĄCZONA LISTA węzłów — a od pewnego rozmiaru: drzewo. */
    static void collisionsAndChaining() {
        section("4. Kolizje i łańcuch w kubełku (+ treeify)");

        note("Gdy dwa różne klucze trafią do tego samego kubełka (jak \"Aa\" i \"BB\" w sekcji 3), HashMap trzyma");
        note("je jako POŁĄCZONĄ LISTĘ węzłów w tym kubełku (\"łańcuch\") — get/put przegląda ją węzeł po węźle,");
        note("porównując hashCode (szybko) i dopiero przy zgodności — equals (wolniej, ale rzadziej wywoływane).");
        note("Od Javy 8: jeśli JEDEN kubełek urośnie do 8+ węzłów I cała wewnętrzna tablica ma co najmniej 64");
        note("kubełki, HashMap zamienia tę listę na ZBALANSOWANE DRZEWO CZERWONO-CZARNE (treeify) — wyszukiwanie");
        note("w takim kubełku przyspiesza wtedy z O(n) do O(log n). To siatka bezpieczeństwa na pechowe/złośliwe");
        note("hashCode, NIE normalny tryb pracy dobrze zaprojektowanej mapy.");

        // DOBRA PRAKTYKA: treeify to ochrona przed najgorszym przypadkiem, nie licencja na leniwy hashCode.
        //   Dobry hashCode (rozkładający klucze równomiernie) sprawia, że drzewa w praktyce prawie nigdy nie
        //   powstają — sekcja 6 pokazuje, jak WYMUSIĆ najgorszy przypadek celowo złym hashCode.
    }

    // =================================================================================================
    // 5. WSPÓŁCZYNNIK WYPEŁNIENIA (load factor) I resize
    // =================================================================================================

    /**
     * 5. HashMap nie czeka, aż kubełki się "zapchają" — powiększa się PROFILAKTYCZNIE, gdy zrobi się zbyt
     * gęsto (średnio więcej niż load factor elementów na kubełek), żeby łańcuchy zostały krótkie.
     */
    static void loadFactorAndResize() {
        section("5. Współczynnik wypełnienia (load factor) 0.75 i resize");

        note("Domyślna pojemność nowego HashMap to 16 kubełków, domyślny load factor to 0.75 → próg (threshold)");
        note("= 16 * 0.75 = 12. Gdy rozmiar mapy PRZEKROCZY próg (przy 13. wstawieniu), HashMap tworzy NOWĄ,");
        note("dwa razy większą tablicę (32 kubełki, nowy próg 24) i PRZEKŁADA do niej każdy wpis, licząc jego");
        note("indeks na nowo (bo capacity, czyli maska bitowa z sekcji 2, się zmieniła — rehash).");

        Map<Integer, String> growing = new HashMap<>();
        for (int i = 0; i < 13; i++) {
            growing.put(i, "v" + i);
        }
        show("rozmiar po 13 wstawieniach (13. wstawienie przekroczyło próg 12 → resize już się wydarzył)", growing.size());
        // WYNIK: rozmiar po 13 wstawieniach (13. wstawienie przekroczyło próg 12 → resize już się wydarzył) → 13

        note("Java nie udostępnia publicznie aktualnej pojemności HashMap (to szczegół implementacji) — resize nie");
        note("da się bezpośrednio \"zobaczyć\" bez refleksji, ale próg i podwajanie to udokumentowane fakty JDK.");

        // PUŁAPKA: każdy resize to koszt — cała tablica jest przepisywana i dla każdego wpisu liczony jest nowy
        //   indeks (sam hashCode() nie jest wołany ponownie: HashMap trzyma obliczony hash w węźle). Jeśli z góry wiesz, że wstawisz dużo elementów, podaj initial capacity w konstruktorze
        //   (new HashMap<>(128)) — unikniesz kilku kolejnych resize'ów po drodze (Collections13Performance).
    }

    // =================================================================================================
    // 6. STAŁY hashCode = WSZYSTKO W JEDNYM KUBEŁKU
    // =================================================================================================

    /**
     * BadKey = celowo ZŁY klucz: hashCode() zawsze zwraca tę samą wartość. To NIE łamie kontraktu hashCode
     * (równe obiekty muszą mieć równy hashCode — tu wszystkie mają równy, więc technicznie "spełnione"), ale
     * niszczy wydajność: WSZYSTKIE instancje trafiają do JEDNEGO kubełka, więc HashMap staje się listą.
     */
    static final class BadKey {
        static int equalsCalls = 0;   // licznik do POMIARU liczby porównań — NIE mierzymy czasu (kit: liczymy wywołania)

        final int id;

        BadKey(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return 1;   // celowo stała wartość — demonstracja najgorszego przypadku
        }

        @Override
        public boolean equals(Object o) {
            equalsCalls++;
            return o instanceof BadKey other && other.id == id;
        }
    }

    /** 6. Licząc wywołania equals() (nie czas!) widać różnicę między szukaniem pierwszego a ostatniego klucza w łańcuchu. */
    static void constantHashCodeCollision() {
        section("6. Stały hashCode = wszystko w jednym kubełku (wolne wyszukiwanie)");

        Map<BadKey, String> map = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            map.put(new BadKey(i), "wartość-" + i);
        }

        BadKey.equalsCalls = 0;
        String last = map.get(new BadKey(5));   // ostatni wstawiony klucz — najgorszy przypadek w łańcuchu
        show("znaleziono (szukając OSTATNIEGO wstawionego klucza)", last);
        show("ile razy wywołano equals()", BadKey.equalsCalls);
        // WYNIK: znaleziono (szukając OSTATNIEGO wstawionego klucza) → wartość-5
        // WYNIK: ile razy wywołano equals() → 6

        BadKey.equalsCalls = 0;
        String first = map.get(new BadKey(0));   // pierwszy wstawiony klucz — najlepszy przypadek
        show("znaleziono (szukając PIERWSZEGO wstawionego klucza)", first);
        show("ile razy wywołano equals()", BadKey.equalsCalls);
        // WYNIK: znaleziono (szukając PIERWSZEGO wstawionego klucza) → wartość-0
        // WYNIK: ile razy wywołano equals() → 1

        note("6 kluczy w JEDNYM kubełku = łańcuch długości 6. Szukanie ostatniego wymaga przejścia CAŁEGO");
        note("łańcucha (6 porównań), szukanie pierwszego — tylko jednego. Dla dobrego hashCode (6 kubełków,");
        note("po jednym kluczu) OBA wyszukiwania kosztowałyby ~1 porównanie — właśnie tę różnicę tu widać.");

        // PUŁAPKA: stały hashCode() jest formalnie ZGODNY z kontraktem (równe obiekty → równy hashCode — tu
        //   wszystko jest "równe" pod względem hashCode), ale zamienia HashMap w O(n) listę. Kontrakt hashCode
        //   to warunek KONIECZNY, nie wystarczający — dobry hashCode musi też DOBRZE ROZPRASZAĆ różne obiekty.
    }

    // =================================================================================================
    // 7. PUŁAPKA: ZMIENNY KLUCZ GUBI SIĘ PO ZMIANIE
    // =================================================================================================

    /** MutablePoint = punkt (x, y) z hashCode ZALEŻNYM od pól — celowo zmienny, żeby pokazać pułapkę. */
    static final class MutablePoint {
        int x;
        int y;

        MutablePoint(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof MutablePoint other && other.x == x && other.y == y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }

    /**
     * 7. put(key, ...) liczy hashCode key W MOMENCIE wstawiania i zapamiętuje go pośrednio (wybierając kubełek).
     * Jeśli PO wstawieniu zmienisz pole użyte w hashCode(), get(ten sam obiekt) policzy hashCode NA NOWO — inny
     * niż przy put — i będzie szukać w NIEWŁAŚCIWYM kubełku. Wpis fizycznie zostaje w mapie, ale jest "zgubiony".
     */
    static void mutableKeyLostAfterChange() {
        section("7. Pułapka: zmienny klucz \"gubi się\" po zmianie");

        Map<MutablePoint, String> byPoint = new HashMap<>();
        MutablePoint key = new MutablePoint(1, 2);
        byPoint.put(key, "punkt startowy");

        show("map.get(klucz) zaraz po put", byPoint.get(key));
        // WYNIK: map.get(klucz) zaraz po put → punkt startowy

        key.x = 99;   // zmieniamy pole użyte w hashCode() PO wstawieniu do mapy!

        show("map.get(TEN SAM obiekt referencyjnie) po zmianie x", byPoint.get(key));
        // WYNIK: map.get(TEN SAM obiekt referencyjnie) po zmianie x → null

        show("rozmiar mapy (wpis wciąż tam jest — tylko \"zgubiony\")", byPoint.size());
        // WYNIK: rozmiar mapy (wpis wciąż tam jest — tylko "zgubiony") → 1

        show("map.get(nowy obiekt o STARYCH współrzędnych (1, 2))", byPoint.get(new MutablePoint(1, 2)));
        // WYNIK: map.get(nowy obiekt o STARYCH współrzędnych (1, 2)) → null

        // PUŁAPKA: klucz JEST w mapie (size == 1), ale NIE DA SIĘ go już znaleźć — ani przez ten sam obiekt
        //   (referencyjnie!), ani przez nowy obiekt o starych współrzędnych. get liczy hashCode DZISIAJ i szuka
        //   w kubełku, w którym klucz BYŁBY z dzisiejszym hashCode — a wpis fizycznie leży w kubełku sprzed zmiany.
        // DOBRA PRAKTYKA: klucze map / elementy setów powinny być NIEZMIENNE, albo przynajmniej: nie zmieniaj
        //   pól użytych w equals/hashCode, gdy obiekt już jest w kolekcji. Rekordy (t09_records) są tu naturalnym
        //   wyborem — nie da się ich w ogóle zmienić po utworzeniu. Bezpieczny fix bez rekordu: remove przed
        //   zmianą, put po zmianie (ćwiczenie 4).
    }

    // =================================================================================================
    // 8. HashSet JEST ZBUDOWANY NA HashMap
    // =================================================================================================

    /** 8. HashSet<E> to cienka nakładka na HashMap<E, Object> — elementy zbioru to klucze tej mapy. */
    static void hashSetBuiltOnHashMap() {
        section("8. HashSet jest zbudowany NA HashMap");

        note("Wewnątrz java.util.HashSet<E> jest prywatne pole: HashMap<E, Object> map. Element zbioru to KLUCZ");
        note("tej mapy, a WARTOŚĆ zawsze to ten sam, jeden, stały obiekt-znacznik (w kodzie JDK: private static");
        note("final Object PRESENT = new Object();). set.add(x) to w środku map.put(x, PRESENT) != null; wcześniej");
        note("już tam było. set.contains(x) to map.containsKey(x). Dlatego HashSet dziedziczy WSZYSTKIE własności");
        note("HashMap z tej lekcji: hashCode → spread → kubełek, kolizje/łańcuch, load factor i resize, podatność");
        note("na stały hashCode (sekcja 6) i na zmienne elementy (sekcja 7 — tam \"klucz\", tu \"element\").");

        Set<String> letters = new HashSet<>();
        letters.add("Aa");
        letters.add("BB");   // ten sam hashCode co "Aa" (sekcja 3) — inny kubełek? NIE, ten sam — ale to nie problem
        show("rozmiar (oba się zmieściły mimo identycznego hashCode)", letters.size());
        show("contains(\"Aa\")", letters.contains("Aa"));
        show("contains(\"CC\")", letters.contains("CC"));
        // WYNIK: rozmiar (oba się zmieściły mimo identycznego hashCode) → 2
        // WYNIK: contains("Aa") → true
        // WYNIK: contains("CC") → false
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Indeks kubełka: spread = h ^ (h >>> 16); index = (capacity - 1) & spread. capacity to zawsze potęga dwójki.
     *   • Kolizja: różne klucze, ten sam kubełek — NORMALNE i nieuniknione, rozróżnia je equals wewnątrz kubełka.
     *   • Kubełek to lista węzłów; od 8 węzłów (i tablicy ≥ 64 kubełków) HashMap zamienia ją na drzewo (treeify).
     *   • load factor 0.75, domyślna capacity 16 → threshold 12. Przekroczenie progu = podwojenie tablicy + rehash.
     *   • Stały hashCode() spełnia kontrakt, ale niszczy wydajność (wszystko w jednym kubełku, O(n) zamiast O(1)).
     *   • Zmienny klucz zmieniony PO wstawieniu do mapy/setu "gubi się" — put i get liczą hashCode w różnych momentach.
     *   • String.hashCode() ma wzór GWARANTOWANY przez Javadoc: s[0]*31^(n-1) + ... + s[n-1].
     *   • HashSet<E> = cienka nakładka na HashMap<E, Object> — dziedziczy wszystkie własności HashMap.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego HashMap miesza górne bity hashCode w dolne (h ^ (h >>> 16)), zamiast po prostu wziąć kilka
     *      dolnych bitów jako indeks?
     *   2. Co wypisze:  System.out.println("Aa".hashCode() == "BB".hashCode());  ?
     *   3. ZNAJDŹ BŁĄD:
     *          Map<List<Integer>, String> cache = new HashMap<>();
     *          List<Integer> key = new ArrayList<>(List.of(1, 2));
     *          cache.put(key, "wynik");
     *          key.add(3);
     *          System.out.println(cache.get(key));
     *   4. Co wypisze:
     *          Map<Integer, String> m = new HashMap<>();
     *          m.put(1, "a");
     *          m.put(17, "b");
     *          System.out.println(m.size());
     *   5. Dlaczego stały hashCode() (np. zawsze return 1;) formalnie SPEŁNIA kontrakt hashCode, a mimo to jest
     *      fatalny dla wydajności?
     *   6. Dlaczego HashSet.add zwraca boolean, skoro w środku to tylko wywołanie map.put?
     *   7. Ile wynosi domyślny próg (threshold) resize dla HashMap z domyślną pojemnością 16 i load factorem 0.75?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: indeks kubełka dla \"Ala\" (capacity 16)", 7, () -> exercise1("Ala", 16));
        Check.equal("ćw. 2a: \"Aa\"/\"BB\" — ten sam hash, różne klucze", true, () -> exercise2("Aa", "BB"));
        Check.equal("ćw. 2b: \"Ala\"/\"Bartek\" — różny hash", false, () -> exercise2("Ala", "Bartek"));
        Check.equal("ćw. 3: equals() dla ostatniego z 6 kluczy o stałym hashCode", 6, () -> exercise3(6, 5));
        Check.equal("ćw. 4: bezpieczna zmiana klucza (remove → zmiana → put)", true, () -> exercise4(1, 2, 99, 99));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 7, () -> solution1("Ala", 16));
        Check.equal("ćw. 2a (wzorzec)", true, () -> solution2("Aa", "BB"));
        Check.equal("ćw. 2b (wzorzec)", false, () -> solution2("Ala", "Bartek"));
        Check.equal("ćw. 3 (wzorzec)", 6, () -> solution3(6, 5));
        Check.equal("ćw. 4 (wzorzec)", true, () -> solution4(1, 2, 99, 99));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć indeks kubełka dla podanego klucza i pojemności — dokładnie wzór z sekcji 2.
     * Podpowiedź: spread = h ^ (h >>> 16); index = (capacity - 1) & spread.
     */
    static int exercise1(String key, int capacity) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 2 (łatwe): zwróć true, jeśli a i b mają TEN SAM hashCode, ale NIE SĄ sobie równe (equals).
     * Podpowiedź: a.hashCode() == b.hashCode() && !a.equals(b).
     */
    static boolean exercise2(String a, String b) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie, wykorzystuje BadKey z sekcji 6): zbuduj mapę z n kluczy BadKey (id 0..n-1) i zwróć,
     * ile razy wywołano equals() szukając klucza o podanym id. Podpowiedź: wyzeruj BadKey.equalsCalls PRZED get.
     */
    static int exercise3(int n, int searchedId) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze, PRZEPISZ bezpiecznie sekcję 7): zbuduj mapę z jednym MutablePoint(oldX, oldY)
     * jako kluczem, a następnie BEZPIECZNIE zmień jego współrzędne na (newX, newY): najpierw remove(key), potem
     * zmień pola, potem put(key, ...) z powrotem. Zwróć true, jeśli po tej procedurze map.get(key) z nowymi
     * współrzędnymi wciąż znajduje wartość (czyli klucz NIE zgubił się, w przeciwieństwie do sekcji 7).
     */
    static boolean exercise4(int oldX, int oldY, int newX, int newY) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(String key, int capacity) {
        int h = key.hashCode();
        int spread = h ^ (h >>> 16);
        return (capacity - 1) & spread;
    }

    static boolean solution2(String a, String b) {
        return a.hashCode() == b.hashCode() && !a.equals(b);
    }

    static int solution3(int n, int searchedId) {
        Map<BadKey, String> map = new HashMap<>();
        for (int i = 0; i < n; i++) {
            map.put(new BadKey(i), "wartość-" + i);
        }
        BadKey.equalsCalls = 0;
        map.get(new BadKey(searchedId));
        return BadKey.equalsCalls;
    }

    static boolean solution4(int oldX, int oldY, int newX, int newY) {
        Map<MutablePoint, String> map = new HashMap<>();
        MutablePoint key = new MutablePoint(oldX, oldY);
        map.put(key, "wartość");

        map.remove(key);   // usuwamy, DOPÓKI hashCode jeszcze pasuje do kubełka, w którym klucz leży
        key.x = newX;
        key.y = newY;
        map.put(key, "wartość");   // wstawiamy ponownie — teraz z NOWYM hashCode, do właściwego kubełka

        return map.get(key) != null;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Przy małej tablicy (np. 16 kubełków) maska (capacity - 1) bierze pod uwagę tylko kilka NAJNIŻSZYCH
     *      bitów hashCode. Gdyby wysokie bity nigdy nie miały wpływu na indeks, hashCode różniące się tylko na
     *      wysokich bitach zawsze trafiałyby w ten sam kubełek. XOR z przesuniętą kopią "wciąga" wysokie bity
     *      do gry.
     *   2. „true” — "Aa" i "BB" mają identyczny hashCode (2112), mimo że to różne napisy.
     *   3. List<Integer> ma hashCode ZALEŻNY od zawartości (podobnie jak MutablePoint w sekcji 7). Po key.add(3)
     *      hashCode klucza się zmienił, więc cache.get(key) szuka w NIEWŁAŚCIWYM kubełku — wypisze null, mimo że
     *      wpis fizycznie wciąż jest w mapie.
     *   4. „2” — 1 i 17 to różne klucze (equals je rozróżnia), niezależnie od tego, czy trafiają do tego samego
     *      kubełka. Kolizja nie jest błędem, tylko normalną sytuacją, którą HashMap poprawnie obsługuje.
     *   5. Kontrakt wymaga tylko: RÓWNE obiekty → RÓWNY hashCode. Stały hashCode() technicznie to spełnia (bo
     *      "wszystko jest sobie równe" pod względem hashCode). Kontrakt NIE wymaga, żeby różne obiekty miały
     *      różny hashCode (to niemożliwe przy nieskończenie wielu obiektach i 32-bitowym int) — ale dobra jakość
     *      hashCode (dobre rozproszenie) jest potrzebna, żeby HashMap działała szybko, a nie tylko "poprawnie".
     *   6. Bo Set.add ma inną semantykę niż Map.put: add informuje, czy element BYŁ NOWY (true) czy już istniał
     *      (false), podczas gdy put zwraca POPRZEDNIĄ WARTOŚĆ (albo null). HashSet.add sprawdza więc wynik
     *      map.put(x, PRESENT) i tłumaczy go na boolean (null poprzednio → true, coś tam było → false).
     *   7. „12” — 16 (domyślna pojemność) razy 0.75 (domyślny load factor).
     */
    // </editor-fold>
}
