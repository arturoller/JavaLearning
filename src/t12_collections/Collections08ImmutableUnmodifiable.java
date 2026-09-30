package t12_collections;

import helpers.Check;
import helpers.SampleData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Niezmienność i widoki „tylko do odczytu” — List.of, unmodifiableList, copyOf, Arrays.asList
 *        (immutable = niezmienny; unmodifiable = niemodyfikowalny; view = widok; defensive copy = kopia obronna)
 *
 * W SKRÓCIE:
 *   Cztery różne sposoby na „listę, której nie da się (albo nie powinno się) zmieniać” — i to NIE są synonimy.
 *   List.of tworzy naprawdę niezmienną listę. Collections.unmodifiableList to tylko WIDOK na cudzą listę — sam
 *   nie chroni przed zmianą oryginału. List.copyOf robi niezależną, niezmienną kopię. Arrays.asList to specjalna
 *   lista o STAŁYM rozmiarze, spięta z tablicą. Mylenie ich to częste źródło błędów „czemu moja lista się zmieniła”.
 *
 * ANALOGIA: kopia vs. okno.
 *   List.copyOf to zdjęcie pokoju — niezależne od tego, co się w nim później dzieje. Collections.unmodifiableList
 *   to okno do tego samego pokoju — nie możesz nic przestawić RĘKAMI przez okno (metodami widoku), ale jak ktoś
 *   wejdzie do pokoju drzwiami (oryginalną referencją) i przestawi meble, przez okno i tak to zobaczysz.
 *
 * JAK TO DZIAŁA:
 *   List{@code <String>} a = List.of("x", "y");                        ← naprawdę niezmienna, własne dane
 *   List{@code <String>} b = Collections.unmodifiableList(mutableList); ← widok, dane wciąż w mutableList
 *   List{@code <String>} c = List.copyOf(mutableList);                  ← niezależna kopia, niezmienna
 *   List{@code <String>} d = Arrays.asList(tablica);                    ← stały rozmiar, set działa, add/remove nie
 *
 * SŁÓWKA:
 *   immutable = niezmienny (nie da się w ogóle zmienić); unmodifiable = niemodyfikowalny (przez TEN obiekt, ale
 *   dane mogą się zmienić skądinąd); view = widok; defensive copy = kopia obronna; fixed-size = o stałym rozmiarze;
 *   leak = wyciek (referencji na zewnątrz); shallow = płytki (niezmienność nie sięga do wnętrza elementów);
 *   snapshot = migawka, zdjęcie stanu w danej chwili.
 *
 * ZOBACZ TEŻ: t12_collections/Collections01Overview (List.of — pierwsze wprowadzenie), t12_collections/Collections02Lists
 *             (ArrayList, subList jako widok), t06_oop_basics/Oop06Immutability (niezmienność obiektów w ogóle),
 *             t09_records/Records01Basics (rekordy jako naturalnie niezmienne wartości).
 * </pre>
 */
public class Collections08ImmutableUnmodifiable {

    public static void main(String[] args) {
        title("Collections08 — niezmienność: List.of, unmodifiableList, copyOf, Arrays.asList");

        factoryImmutable();      // factory immutable = fabryki niezmienne
        unmodifiableView();      // unmodifiable view = widok niemodyfikowalny
        copyOfIndependent();     // copy of independent = copyOf niezależny
        arraysAsListFixedSize(); // arrays as list fixed size = Arrays.asList o stałym rozmiarze
        shallowImmutability();   // shallow immutability = niezmienność płytka
        comparisonTable();       // comparison table = tabela porównawcza
        defensiveCopies();       // defensive copies = kopie obronne
        exercises();              // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. FABRYKI NIEZMIENNE: List.of / Set.of / Map.of
    // =================================================================================================

    /**
     * 1. List.of (9+) tworzy listę, która jest niezmienna NAPRAWDĘ — nie da się jej zmienić żadną metodą,
     * bo w środku w ogóle nie ma miejsca na zmianę (specjalna, wewnętrzna implementacja JDK). Null jest zabroniony.
     */
    static void factoryImmutable() {
        section("1. List.of — naprawdę niezmienne, null zabroniony");

        List<String> colors = List.of("czerwony", "zielony", "niebieski");
        show("kolory", colors);
        // WYNIK: kolory → [czerwony, zielony, niebieski]

        expectThrows("colors.add(\"żółty\")", () -> colors.add("żółty"));
        // WYNIK: ✔ colors.add("żółty") → rzucono UnsupportedOperationException: (brak komunikatu)

        expectThrows("colors.set(0, \"czarny\")", () -> colors.set(0, "czarny"));
        // WYNIK: ✔ colors.set(0, "czarny") → rzucono UnsupportedOperationException: (brak komunikatu)

        expectThrows("colors.remove(0)", () -> colors.remove(0));
        // WYNIK: ✔ colors.remove(0) → rzucono UnsupportedOperationException: (brak komunikatu)

        expectThrows("List.of(\"a\", null, \"b\")", () -> List.of("a", null, "b"));
        // WYNIK: ✔ List.of("a", null, "b") → rzucono NullPointerException: (brak komunikatu)

        // SampleData korzysta z List.of pod maską (patrz kit projektu) — dlatego jej listy też są niezmienne:
        expectThrows("SampleData.products().add(...)", () -> SampleData.products().add(SampleData.productBySku("ELE-001")));
        // WYNIK: ✔ SampleData.products().add(...) → rzucono UnsupportedOperationException: (brak komunikatu)

        note("Set.of i Map.of działają tak samo: niezmienne, null zabroniony. Ich kolejność jest NIEOKREŚLONA");
        note("(może się różnić między uruchomieniami JVM) — dlatego nigdy nie wypisujemy ich wprost (reguła kursu).");

        // DOBRA PRAKTYKA: List.of/Set.of/Map.of do „stałych” danych w kodzie (np. lista dozwolonych statusów) —
        //   nikt przez pomyłkę ich nie zmodyfikuje, bo próba zmiany od razu wybuchnie wyjątkiem zamiast po cichu
        //   psuć stan gdzieś daleko od miejsca błędu.
    }

    // =================================================================================================
    // 2. Collections.unmodifiableList — WIDOK, NIE KOPIA
    // =================================================================================================

    /**
     * 2. Collections.unmodifiableList (od zawsze, Java 1.2) owija podaną listę w obiekt bez metod modyfikujących.
     * To tylko „doklejona nakładka” — same dane siedzą nadal w oryginalnej liście.
     */
    static void unmodifiableView() {
        section("2. Collections.unmodifiableList — widok, nie kopia");

        List<String> mutable = new ArrayList<>(List.of("Ala", "Bartek", "Celina"));
        List<String> view = Collections.unmodifiableList(mutable);
        show("widok", view);
        // WYNIK: widok → [Ala, Bartek, Celina]

        expectThrows("view.add(\"Darek\")", () -> view.add("Darek"));
        // WYNIK: ✔ view.add("Darek") → rzucono UnsupportedOperationException: (brak komunikatu)

        mutable.add("Darek");   // zmiana PRZEZ ORYGINAŁ, nie przez widok
        show("ten sam widok po zmianie oryginału", view);
        // WYNIK: ten sam widok po zmianie oryginału → [Ala, Bartek, Celina, Darek]

        // PUŁAPKA: unmodifiableList NIE kopiuje danych — to tylko opakowanie bez metod modyfikujących. Zmiana
        //   oryginalnej listy (mutable) jest NATYCHMIAST widoczna przez widok, bo to wciąż te same dane w pamięci.
        //   Jeśli potrzebujesz niezależnej, naprawdę bezpiecznej kopii — użyj List.copyOf (sekcja 3), nie tego.

        // DOBRA PRAKTYKA: unmodifiableList ma sens, gdy CHCESZ dzielić się „żywym” podglądem swojej listy (widz ma
        //   zawsze aktualny stan, ale nie może nim manipulować) — np. getter zwracający widok na listę zdarzeń,
        //   która sama rośnie w tle.
    }

    // =================================================================================================
    // 3. List.copyOf — NIEZALEŻNA KOPIA
    // =================================================================================================

    /**
     * 3. List.copyOf (10+) kopiuje ELEMENTY do nowej, niezmiennej listy. Późniejsze zmiany źródła jej nie dotyczą.
     * Ciekawostka z dokumentacji JDK: jeśli źródło jest już niezmienną listą (np. z List.of), copyOf zazwyczaj
     * nie kopiuje wcale, tylko oddaje tę samą instancję — bo i tak nic jej nie zagraża.
     */
    static void copyOfIndependent() {
        section("3. List.copyOf — niezależna, niezmienna kopia (Java 10+)");

        List<String> mutable = new ArrayList<>(List.of("x", "y"));
        List<String> copy = List.copyOf(mutable);
        show("kopia", copy);
        // WYNIK: kopia → [x, y]

        mutable.add("z");
        show("kopia po zmianie oryginału (bez zmian!)", copy);
        // WYNIK: kopia po zmianie oryginału (bez zmian!) → [x, y]

        expectThrows("copy.add(\"w\")", () -> copy.add("w"));
        // WYNIK: ✔ copy.add("w") → rzucono UnsupportedOperationException: (brak komunikatu)

        List<String> already = List.of("a", "b");
        List<String> copyOfImmutable = List.copyOf(already);
        show("List.copyOf(List.of(...)) to ta sama instancja?", copyOfImmutable == already);
        // WYNIK: List.copyOf(List.of(...)) to ta sama instancja? → true

        expectThrows("List.copyOf z null w środku", () -> List.copyOf(Arrays.asList("a", null)));
        // WYNIK: ✔ List.copyOf z null w środku → rzucono NullPointerException: (brak komunikatu)

        // DOBRA PRAKTYKA: przyjmujesz List<T> jako argument konstruktora i chcesz mieć pewność, że nikt jej
        //   później nie zmieni pod Tobą? Zapisz sobie List.copyOf(argument), nie sam argument (patrz sekcja 7).
    }

    // =================================================================================================
    // 4. Arrays.asList — LISTA O STAŁYM ROZMIARZE
    // =================================================================================================

    /**
     * 4. Arrays.asList zwraca listę „spiętą” z podaną tablicą — to NIE jest ArrayList. set() zapisuje wprost do
     * tablicy, ale add/remove zmieniłyby jej rozmiar, a tablice w Javie mają rozmiar stały — stąd wyjątek.
     */
    static void arraysAsListFixedSize() {
        section("4. Arrays.asList — lista o stałym rozmiarze, spięta z tablicą");

        String[] array = {"Ala", "Bartek", "Celina"};
        List<String> asList = Arrays.asList(array);
        show("asList", asList);
        // WYNIK: asList → [Ala, Bartek, Celina]

        asList.set(1, "Bożena");     // set DZIAŁA — rozmiar się nie zmienia
        show("tablica po asList.set(1, ...)", array);
        // WYNIK: tablica po asList.set(1, ...) → [Ala, Bożena, Celina]

        expectThrows("asList.add(\"Darek\")", () -> asList.add("Darek"));
        // WYNIK: ✔ asList.add("Darek") → rzucono UnsupportedOperationException: (brak komunikatu)

        expectThrows("asList.remove(0)", () -> asList.remove(0));
        // WYNIK: ✔ asList.remove(0) → rzucono UnsupportedOperationException: (brak komunikatu)

        // PUŁAPKA: Arrays.asList to lista „spięta” z tablicą — set zapisuje wprost DO tablicy (widać to wyżej: po
        //   asList.set zmieniła się też tablica array), a add/remove zmieniłyby rozmiar tablicy, czego Java nie
        //   potrafi zrobić w miejscu → UnsupportedOperationException, nie jakiś błąd tablicy.

        // DOBRA PRAKTYKA: potrzebujesz zwykłej, w pełni zmiennej listy z tablicy?
        //   Owiń: new ArrayList<>(Arrays.asList(array)) — wtedy add/remove działają normalnie i tablicy to nie dotyczy.
    }

    // =================================================================================================
    // 5. PUŁAPKA: NIEZMIENNOŚĆ JEST PŁYTKA
    // =================================================================================================

    /**
     * 5. List.of/List.copyOf/unmodifiableList chronią tylko SAMĄ LISTĘ (jej strukturę: dodawanie/usuwanie/
     * podmiana elementów). Jeśli element sam jest zmienny, nic nie broni przed zmianą JEGO wnętrza.
     */
    static void shallowImmutability() {
        section("5. Pułapka: niezmienność jest PŁYTKA (shallow)");

        List<StringBuilder> names = List.of(new StringBuilder("Ala"), new StringBuilder("Bartek"));
        show("lista", names);
        // WYNIK: lista → [Ala, Bartek]

        names.get(0).append("!!!");    // NIE zmieniamy listy — zmieniamy OBIEKT, który w niej siedzi
        show("po append na elemencie (lista wciąż \"ta sama\")", names);
        // WYNIK: po append na elemencie (lista wciąż "ta sama") → [Ala!!!, Bartek]

        expectThrows("names.add(...)", () -> names.add(new StringBuilder("Celina")));
        // WYNIK: ✔ names.add(...) → rzucono UnsupportedOperationException: (brak komunikatu)

        // PUŁAPKA: struktura listy jest chroniona (nie da się dodać/usunąć/podmienić elementu), ale StringBuilder
        //   w środku ma własne metody modyfikujące (append) i lista nic o tym nie wie — nadal jest to "ten sam"
        //   obiekt na tej samej pozycji. Prawdziwa, pełna niezmienność wymaga niezmiennych ELEMENTÓW
        //   (String, opakowania liczbowe, rekordy bez zmiennych pól — t06_oop_basics/Oop06Immutability).
    }

    // =================================================================================================
    // 6. TABELA: KTÓRĄ NIEZMIENNOŚĆ WYBRAĆ?
    // =================================================================================================

    /** 6. Skrócona ściąga „na już” — pełna wersja w ŚCIĄDZE na końcu pliku. */
    static void comparisonTable() {
        section("6. Tabela: którą niezmienność wybrać?");

        note("List.of(...)                        → nowa lista OD ZERA, naprawdę niezmienna, null zabroniony.");
        note("List.copyOf(kolekcja)                → niezależna KOPIA cudzej kolekcji, niezmienna, null zabroniony.");
        note("Collections.unmodifiableList(lista)  → WIDOK na cudzą listę, sam nie kopiuje, oryginał wciąż zmienny.");
        note("Arrays.asList(tablica)                → STAŁY rozmiar, set pisze do tablicy, add/remove wyjątek.");
        note("new ArrayList<>(cokolwiek)            → zwykła, w pełni zmienna lista, niezależna kopia źródła.");
    }

    // =================================================================================================
    // 7. KOPIE OBRONNE (defensive copies)
    // =================================================================================================

    /**
     * 7. Klasa przechowująca kolekcję powinna kopiować ją obronnie w DWÓCH miejscach: w konstruktorze (żeby nikt
     * z zewnątrz nie zmienił jej stanu przez trzymaną gdzieś referencję na wejściu) i w getterze (żeby nikt nie
     * dostał referencji do jej wnętrza i nie zmienił jej bez wiedzy obiektu).
     */
    static void defensiveCopies() {
        section("7. Kopie obronne: konstruktor i getter");

        // --- BEZ obrony: dwa wycieki referencji -----------------------------------------------------
        class LeakyBasket {
            private final List<String> items;

            LeakyBasket(List<String> items) {
                this.items = items;                 // brak kopii przy wejściu — trzymamy CUDZĄ listę wprost
            }

            List<String> getItems() {
                return items;                       // brak kopii na wyjściu — oddajemy referencję do wnętrza
            }
        }

        List<String> source = new ArrayList<>(List.of("jabłko", "gruszka"));
        LeakyBasket leaky = new LeakyBasket(source);
        source.add("śliwka");                       // zmiana Z ZEWNĄTRZ, PO stworzeniu obiektu
        show("leaky.getItems() po zmianie source", leaky.getItems());
        // WYNIK: leaky.getItems() po zmianie source → [jabłko, gruszka, śliwka]

        leaky.getItems().add("brzoskwinia");        // zmiana PRZEZ GETTER — też narusza stan obiektu!
        show("source po zmianie przez getter", source);
        // WYNIK: source po zmianie przez getter → [jabłko, gruszka, śliwka, brzoskwinia]

        note("LeakyBasket miał niby-prywatne pole items, ale DWIE strony mogły je zmieniać bez jego wiedzy.");

        // --- Z obroną: kopiujemy raz, na wejściu, i tyle ----------------------------------------------
        class SafeBasket {
            private final List<String> items;

            SafeBasket(List<String> items) {
                this.items = List.copyOf(items);    // kopia obronna przy wejściu — odcinamy się od cudzej listy
            }

            List<String> getItems() {
                return items;                       // items jest JUŻ niezmienne — bezpiecznie oddać wprost
            }
        }

        List<String> source2 = new ArrayList<>(List.of("jabłko", "gruszka"));
        SafeBasket safe = new SafeBasket(source2);
        source2.add("śliwka");
        show("safe.getItems() po zmianie source2", safe.getItems());
        // WYNIK: safe.getItems() po zmianie source2 → [jabłko, gruszka]

        expectThrows("safe.getItems().add(...)", () -> safe.getItems().add("brzoskwinia"));
        // WYNIK: ✔ safe.getItems().add(...) → rzucono UnsupportedOperationException: (brak komunikatu)

        // DOBRA PRAKTYKA: gdy pole klasy to kolekcja, kopiuj obronnie W KONSTRUKTORZE (odetnij się od cudzej listy)
        //   i pilnuj, żeby GETTER nie oddawał referencji do zmiennego wnętrza. Najprościej: przechowuj od razu
        //   niezmienną kopię (List.copyOf raz, w konstruktorze) i zwracaj ją wprost — nie trzeba kopiować drugi raz.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • List.of(...)               — nowa lista, naprawdę niezmienna, null zabroniony (NPE).
     *   • List.copyOf(kolekcja)      — niezależna KOPIA, niezmienna, null w środku zabroniony (NPE).
     *   • Collections.unmodifiableList(l) — WIDOK na l: sam się nie modyfikuje, ale zmiany w l są przez niego widoczne.
     *   • Arrays.asList(tablica)     — stały rozmiar: set() zapisuje do tablicy, add/remove → UnsupportedOperationException.
     *   • Niezmienność list/kolekcji jest PŁYTKA: chroni strukturę, nie chroni wnętrza zmiennych elementów.
     *   • Kopia obronna: kopiuj w konstruktorze (wejście) i pilnuj, by getter nie oddawał referencji do wnętrza.
     *   • Set.of/Map.of działają jak List.of (niezmienne, null zabroniony) — ich kolejność jest NIEOKREŚLONA.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się List.of od Collections.unmodifiableList — co się dzieje z każdą z nich, gdy zmienisz
     *      oryginalną listę, z której powstały?
     *   2. Co wypisze:
     *          List<Integer> a = new ArrayList<>(List.of(1, 2));
     *          List<Integer> b = Collections.unmodifiableList(a);
     *          a.add(3);
     *          System.out.println(b);
     *   3. ZNAJDŹ BŁĄD:
     *          String[] tab = {"a", "b", "c"};
     *          List<String> lista = Arrays.asList(tab);
     *          lista.add("d");
     *   4. Co wypisze:
     *          List<StringBuilder> l = List.of(new StringBuilder("x"));
     *          l.get(0).append("y");
     *          System.out.println(l);
     *   5. Dlaczego kopia obronna powinna być zrobiona i w konstruktorze, i (potencjalnie) w getterze — czy nie
     *      wystarczy jedno z tych dwóch miejsc?
     *   6. Czy List.copyOf(List.of("a", "b")) na pewno tworzy nową listę w pamięci? Dlaczego to bywa ważne?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: niezależna kopia", List.of("a", "b"), () -> {
            List<String> src = new ArrayList<>(List.of("a", "b"));
            List<String> result = exercise1(src);
            src.add("c");               // zmiana PO wywołaniu — nie powinna wpłynąć na wynik
            return result;
        });
        Check.equal("ćw. 2a: add na zwykłej ArrayList", true, () -> exercise2(new ArrayList<>(List.of("a"))));
        Check.equal("ćw. 2b: add na List.of", false, () -> exercise2(List.of("a")));
        Check.equal("ćw. 2c: add na Arrays.asList", false, () -> exercise2(Arrays.asList("a", "b")));
        Check.equal("ćw. 3: widok odzwierciedla PÓŹNIEJSZE zmiany", List.of("a", "b", "c"), () -> {
            List<String> src = new ArrayList<>(List.of("a", "b"));
            List<String> view = exercise3(src);
            src.add("c");                // zmiana PO wywołaniu — TYM RAZEM ma być widoczna
            return view;
        });
        Check.equal("ćw. 4: kopia bez null-i", List.of("a", "b"), () -> exercise4(Arrays.asList("a", null, "b", null)));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of("a", "b"), () -> {
            List<String> src = new ArrayList<>(List.of("a", "b"));
            List<String> result = solution1(src);
            src.add("c");
            return result;
        });
        Check.equal("ćw. 2a (wzorzec)", true, () -> solution2(new ArrayList<>(List.of("a"))));
        Check.equal("ćw. 2b (wzorzec)", false, () -> solution2(List.of("a")));
        Check.equal("ćw. 2c (wzorzec)", false, () -> solution2(Arrays.asList("a", "b")));
        Check.equal("ćw. 3 (wzorzec)", List.of("a", "b", "c"), () -> {
            List<String> src = new ArrayList<>(List.of("a", "b"));
            List<String> view = solution3(src);
            src.add("c");
            return view;
        });
        Check.equal("ćw. 4 (wzorzec)", List.of("a", "b"), () -> solution4(Arrays.asList("a", null, "b", null)));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć niezależną, niezmienną kopię podanej listy — zmiana source PO wywołaniu
     * metody nie może być widoczna w wyniku. Podpowiedź: List.copyOf.
     */
    static List<String> exercise1(List<String> source) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 2 (łatwe): zwróć true, jeśli do podanej listy da się dodać element (add się uda), false gdy
     * add rzuci UnsupportedOperationException. Nie zostaw śladu — jeśli się uda, usuń dodany element.
     * Podpowiedź: spróbuj list.add(...) w try, w catch UnsupportedOperationException zwróć false.
     */
    static boolean exercise2(List<String> list) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć niemodyfikowalny WIDOK na podaną listę — sam nie pozwala na add/set/remove,
     * ale MA odzwierciedlać późniejsze zmiany oryginału. Podpowiedź: Collections.unmodifiableList.
     */
    static List<String> exercise3(List<String> original) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): podana lista może zawierać null. Zwróć niezmienną kopię BEZ elementów null,
     * zachowując kolejność pozostałych. Podpowiedź: zwykła pętla do nowej ArrayList (albo removeIf), a na końcu
     * List.copyOf — samo List.copyOf nie przepuści null, trzeba je usunąć wcześniej.
     */
    static List<String> exercise4(List<String> withNulls) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solution1(List<String> source) {
        return List.copyOf(source);
    }

    static boolean solution2(List<String> list) {
        try {
            list.add("__sonda__");
            list.remove(list.size() - 1);
            return true;
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }

    static List<String> solution3(List<String> original) {
        return Collections.unmodifiableList(original);
    }

    static List<String> solution4(List<String> withNulls) {
        List<String> cleaned = new ArrayList<>();
        for (String s : withNulls) {
            if (s != null) {
                cleaned.add(s);
            }
        }
        return List.copyOf(cleaned);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. List.of ma WŁASNE dane — nic, co się dzieje z inną listą, jej nie dotyczy. unmodifiableList to tylko
     *      opakowanie na CUDZE dane — zmiana oryginału jest przez nie widoczna (to wciąż te same dane w pamięci).
     *   2. „[1, 2, 3]” — b to widok na a, a a się zmieniło.
     *   3. Arrays.asList zwraca listę o stałym rozmiarze (spiętą z tablicą) — add zmieniłby rozmiar, więc rzuca
     *      UnsupportedOperationException. set(i, x) by zadziałał, bo nie zmienia rozmiaru.
     *   4. „[xy]” — lista jest niezmienna (nie można dodać/usunąć/podmienić elementu), ale element (StringBuilder)
     *      sam jest zmienny i append zmienia jego wnętrze, a nie strukturę listy.
     *   5. Konstruktor chroni przed CUDZĄ referencją trzymaną na wejściu (ktoś zmieni listę, którą Tobie przekazał).
     *      Getter chroni przed oddaniem referencji do WŁASNEGO wnętrza (ktoś zmieni to, co Ty trzymasz). To dwa
     *      różne kierunki wycieku — bez obu obrona jest niepełna. (W tej lekcji wystarczyła jedna kopia w
     *      konstruktorze, bo przechowywaliśmy już niezmienną listę — getter mógł bezpiecznie oddać ją wprost.)
     *   6. Nie zawsze — jeśli źródło jest już niezmienną listą (np. z List.of), copyOf zwraca TĘ SAMĄ instancję
     *      zamiast kopiować (optymalizacja z dokumentacji JDK). To ważne dla wydajności: unikamy zbędnych kopii,
     *      gdy i tak nic nie zagraża oryginałowi.
     */
    // </editor-fold>
}
