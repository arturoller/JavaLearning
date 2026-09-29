package t06_oop_basics;

import helpers.Check;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Niezmienność — obiekty, których po utworzeniu NIE DA SIĘ zmienić
 *        (immutable = niezmienny, mutable = zmienny, defensive copy = kopia obronna)
 *
 * W SKRÓCIE:
 *   Obiekt niezmienny dostaje wszystkie dane w konstruktorze i już nigdy ich nie zmienia. „Zmiana”
 *   oznacza utworzenie NOWEGO obiektu (withPrice, plusDays, toUpperCase). Taki obiekt można bez strachu
 *   dzielić między metodami, trzymać w HashSet i przekazywać między wątkami.
 *
 * ANALOGIA:
 *   Wydrukowany paragon kontra tablica suchościeralna. Paragonu nikt „po cichu” nie poprawi — żeby
 *   zmienić kwotę, trzeba wydrukować NOWY paragon, a stary dalej mówi prawdę o tym, co było.
 *   Tablicę może zmazać każdy, kto przechodzi obok — i nie wiesz, kto to zrobił.
 *
 * JAK TO DZIAŁA:
 *   Przepis na klasę niezmienną:
 *   1) wszystkie pola private final           → przypisane RAZ, w konstruktorze
 *   2) brak setterów                           → nie ma czym zmienić stanu
 *   3) klasa final                             → nikt nie dopisze zmiennej podklasy
 *   4) kopie obronne tablic/list: w konstruktorze I w getterze
 *   5) „zmiana” = metoda with...() zwracająca NOWY obiekt
 *
 *     Book b1 ──▶ { "Czysty kod", 79 }        b1.withPrice(59)
 *     Book b2 ──▶ { "Czysty kod", 59 }  ◀──── nowy obiekt; b1 bez zmian
 *
 * SŁÓWKA:
 *   immutable = niezmienny; mutable = zmienny; final = ostateczny; defensive copy = kopia obronna;
 *   leak = wyciek (przeciek); with = z (czymś innym); share = współdzielić; thread = wątek
 *
 * ZOBACZ TEŻ: t06_oop_basics/Oop05ObjectMethods (dlaczego zmienny klucz „gubi się” w HashSet),
 *             t06_oop_basics/Oop09ValueObjects (niezmienne obiekty-wartości),
 *             t09_records/Records01Basics (rekordy są niezmienne z definicji),
 *             t12_collections/Collections08ImmutableUnmodifiable (List.copyOf kontra widok tylko do odczytu),
 *             t21_concurrency/Concurrency08ThreadSafetyPatterns (niezmienność a wątki)
 * </pre>
 */
public class Oop06Immutability {

    public static void main(String[] args) {
        title("Oop06 — niezmienność (immutability)");

        sharedMutableProblem();     // shared mutable problem = problem współdzielonego zmiennego obiektu
        immutableRecipe();          // immutable recipe = przepis na klasę niezmienną
        withMethods();              // with methods = metody with...
        defensiveCopyArray();       // defensive copy array = kopia obronna tablicy
        defensiveCopyList();        // defensive copy list = kopia obronna listy
        finalIsNotImmutable();      // final is not immutable = final to nie to samo co niezmienny
        jdkImmutables();            // JDK immutables = niezmienne klasy z JDK
        benefitsAndCosts();         // benefits and costs = zalety i koszty
        exercises();                // exercises = ćwiczenia
    }

    // Klasy przykładowe są zagnieżdżone (static nested — Oop07), żeby lekcja była w jednym pliku.

    // =================================================================================================
    // 1. PROBLEM: WSPÓŁDZIELONY ZMIENNY OBIEKT
    // =================================================================================================

    /** Zmienna cena — ma setter, więc każdy, kto ma referencję, może ją zmienić. */
    static class MutablePrice {                 // mutable price = zmienna cena
        private int amount;                     // amount = kwota (w pełnych zł — upraszczamy, pieniądze: t15)

        MutablePrice(int amount) {
            this.amount = amount;
        }

        void setAmount(int amount) {            // setAmount = ustaw kwotę
            this.amount = amount;
        }

        int getAmount() {                       // getAmount = pobierz kwotę
            return amount;
        }
    }

    /** Oferta trzymająca referencję do (zmiennej) ceny. */
    static class Offer {                        // offer = oferta
        private final String name;
        private final MutablePrice price;

        Offer(String name, MutablePrice price) {
            this.name = name;
            this.price = price;
        }

        @Override
        public String toString() {
            return name + " za " + price.getAmount() + " zł";
        }
    }

    /**
     * 1. Dwie oferty dzielą JEDEN obiekt ceny. Zmiana „dla kawy” zmienia też herbatę — bo to ta sama
     * cena w pamięci. Takie błędy trudno znaleźć: zmiana dzieje się daleko od miejsca, gdzie widać skutek.
     */
    static void sharedMutableProblem() {
        section("1. Problem: współdzielony zmienny obiekt");

        MutablePrice promo = new MutablePrice(10);
        Offer coffee = new Offer("Kawa", promo);        // coffee = kawa
        Offer tea = new Offer("Herbata", promo);        // tea = herbata; ta sama cena „na skróty”
        //   coffee ──▶ Offer{Kawa} ──┐
        //                            ├──▶ MutablePrice{10}   ← JEDEN obiekt
        //   tea ─────▶ Offer{Herbata}┘

        promo.setAmount(20);                            // chcieliśmy podrożyć tylko KAWĘ...
        show("kawa", coffee);
        // WYNIK: kawa → Kawa za 20 zł
        show("herbata", tea);
        // WYNIK: herbata → Herbata za 20 zł    ← też podrożała!

        // PUŁAPKA: kto ma referencję do zmiennego obiektu, ten może go zmienić — także „cudzy” kod,
        // któremu obiekt tylko pożyczyliśmy. Lekarstwo: obiekt, którego NIE DA SIĘ zmienić.
    }

    // =================================================================================================
    // 2. PRZEPIS NA KLASĘ NIEZMIENNĄ
    // =================================================================================================

    /** Niezmienna książka: final klasa, final pola, walidacja, brak setterów, metody with. */
    static final class Book {                   // final class = klasa, po której nie można dziedziczyć
        private final String title;             // final pole = przypisane RAZ, w konstruktorze
        private final int price;

        Book(String title, int price) {
            if (title == null || title.isBlank()) {          // isBlank (Java 11+) = pusty lub same spacje
                throw new IllegalArgumentException("tytuł jest pusty");
            }
            if (price < 0) {
                throw new IllegalArgumentException("cena ujemna: " + price);
            }
            this.title = title;
            this.price = price;
        }

        String getTitle() {                     // tylko gettery — żadnych setterów
            return title;
        }

        int getPrice() {
            return price;
        }

        Book withPrice(int newPrice) {          // withPrice = „z inną ceną” → NOWY obiekt
            return new Book(title, newPrice);   // konstruktor znowu sprawdzi poprawność!
        }

        Book withTitle(String newTitle) {       // withTitle = „z innym tytułem”
            return new Book(newTitle, price);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Book other = (Book) o;
            return price == other.price && title.equals(other.title);
        }

        @Override
        public int hashCode() {
            return Objects.hash(title, price);
        }

        @Override
        public String toString() {
            return title + " (" + price + " zł)";
        }
    }

    /**
     * 2. Klasa Book spełnia przepis z nagłówka: stan ustalony w konstruktorze (z walidacją) i potem
     * już tylko do odczytu. Próby zmiany kończą się błędem KOMPILACJI — najlepszy rodzaj błędu.
     */
    static void immutableRecipe() {
        section("2. Przepis na klasę niezmienną");

        Book book = new Book("Czysty kod", 79);
        show("książka", book);
        // WYNIK: książka → Czysty kod (79 zł)
        show("tytuł i cena", book.getTitle() + ", " + book.getPrice());
        // WYNIK: tytuł i cena → Czysty kod, 79

        // book.price = 10;        → błąd kompilacji: cannot assign a value to final variable price
        // book.setPrice(10);      → błąd kompilacji: cannot find symbol (nie ma takiej metody)
        // class Hack extends Book → błąd kompilacji: cannot inherit from final Book

        // Konstruktor pilnuje poprawności — niepoprawny obiekt w ogóle nie powstanie:
        expectThrows("new Book(\"  \", 10)", () -> new Book("  ", 10));
        // WYNIK: ✔ new Book("  ", 10) → rzucono IllegalArgumentException: tytuł jest pusty
        expectThrows("new Book(\"Java\", -5)", () -> new Book("Java", -5));
        // WYNIK: ✔ new Book("Java", -5) → rzucono IllegalArgumentException: cena ujemna: -5

        // DLACZEGO final class? Podklasa mogłaby dodać pole z setterem albo nadpisać getPrice() tak, by
        // zwracał co chce — i „niezmienna” książka przestałaby nią być. Dziedziczenie: t07.
        // DOBRA PRAKTYKA: walidacja w konstruktorze + brak setterów = obiekt jest poprawny przez CAŁE życie.
    }

    // =================================================================================================
    // 3. METODY with... — „ZMIANA” TO NOWY OBIEKT
    // =================================================================================================

    /**
     * 3. Zamiast setPrice mamy withPrice: zwraca NOWĄ książkę, a stara zostaje nietknięta. Metody with
     * można łączyć w łańcuch, bo każda zwraca obiekt tego samego typu.
     */
    static void withMethods() {
        section("3. Metody with... — zmiana to nowy obiekt");

        Book original = new Book("Czysty kod", 79);
        Book cheaper = original.withPrice(59);          // cheaper = tańsza
        show("oryginał", original);
        // WYNIK: oryginał → Czysty kod (79 zł)
        show("tańsza wersja", cheaper);
        // WYNIK: tańsza wersja → Czysty kod (59 zł)
        show("to ten sam obiekt? (original == cheaper)", original == cheaper);
        // WYNIK: to ten sam obiekt? (original == cheaper) → false

        Book second = original.withTitle("Czysty kod, wyd. 2").withPrice(89);   // łańcuch wywołań
        show("łańcuch with", second);
        // WYNIK: łańcuch with → Czysty kod, wyd. 2 (89 zł)

        // PUŁAPKA: wywołanie with BEZ przypisania nic nie daje — wynik ląduje w koszu:
        original.withPrice(1);
        show("po original.withPrice(1) bez przypisania", original);
        // WYNIK: po original.withPrice(1) bez przypisania → Czysty kod (79 zł)

        // Metoda with też waliduje, bo woła konstruktor:
        expectThrows("original.withPrice(-1)", () -> original.withPrice(-1));
        // WYNIK: ✔ original.withPrice(-1) → rzucono IllegalArgumentException: cena ujemna: -1
    }

    // =================================================================================================
    // 4. KOPIE OBRONNE — TABLICE
    // =================================================================================================

    /** Dziurawa klasa: zapamiętuje CUDZĄ tablicę i oddaje SWOJĄ. */
    static final class LeakyGrades {            // leaky = dziurawy (przeciekający); grades = oceny
        private final int[] grades;

        LeakyGrades(int[] grades) {
            this.grades = grades;               // ← zapamiętuje referencję do tablicy wywołującego
        }

        int[] getGrades() {
            return grades;                      // ← oddaje referencję do własnej tablicy
        }
    }

    /** Szczelna klasa: kopia w konstruktorze I w getterze. */
    static final class SafeGrades {             // safe = bezpieczny
        private final int[] grades;

        SafeGrades(int[] grades) {
            this.grades = grades.clone();       // clone = sklonuj; kopia obronna nr 1 (wejście)
        }

        int[] getGrades() {
            return grades.clone();              // kopia obronna nr 2 (wyjście)
        }
    }

    /**
     * 4. {@code final int[]} blokuje tylko przepięcie pola — zawartość tablicy nadal może zmienić każdy,
     * kto ma do niej referencję. Dlatego tablicę kopiujemy przy WEJŚCIU (konstruktor) i WYJŚCIU (getter).
     */
    static void defensiveCopyArray() {
        section("4. Kopie obronne — tablice");

        int[] source = {5, 4, 3};               // source = źródło
        LeakyGrades leaky = new LeakyGrades(source);
        SafeGrades safe = new SafeGrades(source);
        //   source ──┬──▶ [5, 4, 3]  ◀── leaky.grades    (TA SAMA tablica)
        //            │
        //   safe.grades ──▶ [5, 4, 3]                    (własna kopia)

        source[0] = 1;                          // ktoś zmienia SWOJĄ tablicę już po utworzeniu obiektów
        show("leaky po zmianie źródła", leaky.getGrades());
        // WYNIK: leaky po zmianie źródła → [1, 4, 3]
        show("safe po zmianie źródła", safe.getGrades());
        // WYNIK: safe po zmianie źródła → [5, 4, 3]

        leaky.getGrades()[1] = 1;               // ktoś zmienia tablicę otrzymaną z gettera
        safe.getGrades()[1] = 1;                // ...zmienia tylko kopię, która zaraz zniknie
        show("leaky po zmianie przez getter", leaky.getGrades());
        // WYNIK: leaky po zmianie przez getter → [1, 1, 3]
        show("safe po zmianie przez getter", safe.getGrades());
        // WYNIK: safe po zmianie przez getter → [5, 4, 3]

        // DOBRA PRAKTYKA: kopiuj tablicę przez clone() albo Arrays.copyOf(t, t.length) (t03_arrays/Arrays03Utility).
        // Kopia w konstruktorze chroni przed wywołującym, kopia w getterze — przed tym, kto czyta.
    }

    // =================================================================================================
    // 5. KOPIE OBRONNE — LISTY
    // =================================================================================================

    /** Drużyna z niezmienną kopią listy członków. */
    static final class Team {                   // team = drużyna
        private final String name;
        private final List<String> members;     // members = członkowie

        Team(String name, List<String> members) {
            this.name = name;
            this.members = List.copyOf(members); // List.copyOf (Java 10+) = niezmienna KOPIA listy
        }

        List<String> getMembers() {
            return members;                     // bezpieczne bez kopii — ta lista i tak jest niezmienna
        }

        @Override
        public String toString() {
            return name + " " + members;
        }
    }

    /**
     * 5. Z listami jest tak samo jak z tablicami. Listy dokładnie poznasz w t12 — tutaj wystarczy:
     * {@code ArrayList} = lista, do której można dodawać; {@code List.copyOf(lista)} = kopia, której
     * NIE da się zmienić (add rzuca wyjątek). Taka kopia załatwia wejście i wyjście jednocześnie.
     */
    static void defensiveCopyList() {
        section("5. Kopie obronne — listy");

        List<String> names = new ArrayList<>(); // ArrayList = lista tablicowa (zmienna) — t12_collections/Collections02Lists
        names.add("Ala");                       // add = dodaj
        names.add("Bartek");
        Team team = new Team("Orły", names);

        names.add("Intruz");                    // intruz dopisuje się do listy źródłowej...
        show("lista źródłowa", names);
        // WYNIK: lista źródłowa → [Ala, Bartek, Intruz]
        show("drużyna", team);
        // WYNIK: drużyna → Orły [Ala, Bartek]

        expectThrows("team.getMembers().add(\"Haker\")", () -> team.getMembers().add("Haker"));
        // WYNIK: ✔ team.getMembers().add("Haker") → rzucono UnsupportedOperationException: (brak komunikatu)

        // PUŁAPKA: Collections.unmodifiableList(lista) to NIE kopia, tylko „okienko tylko do odczytu” na
        // oryginał — zmiany w oryginale nadal będą widoczne (t12_collections/Collections08ImmutableUnmodifiable).
        // PUŁAPKA: List.copyOf nie przyjmuje elementów null — rzuci NullPointerException.
        // DOBRA PRAKTYKA: pole-lista w klasie niezmiennej → List.copyOf(...) w konstruktorze, getter bez kopii.
    }

    // =================================================================================================
    // 6. final ≠ NIEZMIENNY
    // =================================================================================================

    /**
     * 6. final przy zmiennej/polu oznacza „tej strzałki nie przepniesz”. Nie mówi nic o obiekcie na
     * końcu strzałki — ten może być w pełni zmienny (tablica, StringBuilder, ArrayList).
     */
    static void finalIsNotImmutable() {
        section("6. final to nie to samo co niezmienny");

        final int[] numbers = {1, 2, 3};
        numbers[0] = 99;                        // OK! final nie chroni zawartości tablicy
        // numbers = new int[]{7};              → błąd kompilacji: cannot assign a value to final variable numbers
        show("final tablica po numbers[0] = 99", numbers);
        // WYNIK: final tablica po numbers[0] = 99 → [99, 2, 3]

        final StringBuilder sb = new StringBuilder("Ala");   // StringBuilder = zmienny tekst (t04_strings/Strings03StringBuilder)
        sb.append(" ma kota");                  // append = dopisz; obiekt się zmienia mimo final
        show("final StringBuilder po append", sb);
        // WYNIK: final StringBuilder po append → Ala ma kota

        //   final zmienna ══▶ obiekt          (══ przyspawana strzałka — nie przepniesz)
        //                      └── zawartość obiektu: zmienna, jeśli klasa na to pozwala
        //
        //   niezmienny obiekt = final pola + brak setterów + kopie obronne + final klasa (sekcje 2–5)

        // DOBRA PRAKTYKA: final przy polach to dopiero PIERWSZY krok. Pytaj: „czy przez któreś pole
        // da się dosięgnąć czegoś zmiennego?” — jeśli tak, potrzebna kopia obronna.
    }

    // =================================================================================================
    // 7. NIEZMIENNE KLASY Z JDK: String, LocalDate, BigDecimal
    // =================================================================================================

    /**
     * 7. Twórcy Javy zrobili niezmiennymi m.in. String, LocalDate i BigDecimal. Ich metody „zmieniające”
     * zwracają NOWY obiekt — wynik trzeba przypisać, inaczej przepada.
     */
    static void jdkImmutables() {
        section("7. Niezmienne klasy z JDK");

        String text = "Ala";
        text.concat(" ma kota");                // concat = połącz; PUŁAPKA: wynik wyrzucony
        show("po text.concat(...) bez przypisania", text);
        // WYNIK: po text.concat(...) bez przypisania → Ala
        text = text.concat(" ma kota");         // przypisujemy NOWY obiekt do zmiennej
        show("po text = text.concat(...)", text);
        // WYNIK: po text = text.concat(...) → Ala ma kota

        LocalDate start = LocalDate.of(2026, 1, 31);         // LocalDate = data bez godziny (t17_datetime/DateTime01LocalDateTime)
        LocalDate next = start.plusDays(1);                   // plusDays = dodaj dni → NOWA data
        show("start", start);
        // WYNIK: start → 2026-01-31
        show("start.plusDays(1)", next);
        // WYNIK: start.plusDays(1) → 2026-02-01

        BigDecimal price = new BigDecimal("10.00");           // BigDecimal = dokładna liczba dziesiętna (t15_numbers/Numbers01BigDecimal)
        price.add(new BigDecimal("5.00"));                    // PUŁAPKA: wynik zignorowany
        show("price po price.add(...) bez przypisania", price);
        // WYNIK: price po price.add(...) bez przypisania → 10.00
        BigDecimal total = price.add(new BigDecimal("5.00")); // total = suma
        show("total = price.add(...)", total);
        // WYNIK: total = price.add(...) → 15.00

        // DOBRA PRAKTYKA: gdy metoda niezmiennej klasy „coś zmienia”, zawsze przypisz wynik:
        //   text = text.trim();   date = date.plusDays(1);   sum = sum.add(x);
        // IntelliJ podkreśla zignorowany wynik takich metod — nie ignoruj tego ostrzeżenia.
    }

    // =================================================================================================
    // 8. ZALETY I KOSZTY
    // =================================================================================================

    /** Stała współdzielona przez cały program — bezpieczna, bo Book jest niezmienna. */
    static final Book BESTSELLER = new Book("Java. Podstawy", 129);

    /** „Podstępna” metoda: dostaje książkę i próbuje ją przecenić. */
    static Book sneakyDiscount(Book book) {     // sneaky discount = podstępna obniżka
        return book.withPrice(1);               // może tylko zwrócić NOWĄ książkę
    }

    /**
     * 8. Niezmienny obiekt można bez obaw: oddać obcej metodzie, trzymać jako stałą, używać jako klucza
     * w HashSet/HashMap i czytać z wielu wątków naraz. Kosztem jest tworzenie nowych obiektów przy
     * każdej „zmianie” — zwykle pomijalnym.
     */
    static void benefitsAndCosts() {
        section("8. Zalety i koszty niezmienności");

        Book copy = sneakyDiscount(BESTSELLER);
        show("stała BESTSELLER po sneakyDiscount", BESTSELLER);
        // WYNIK: stała BESTSELLER po sneakyDiscount → Java. Podstawy (129 zł)
        show("to, co zwróciła metoda", copy);
        // WYNIK: to, co zwróciła metoda → Java. Podstawy (1 zł)

        Set<Book> catalog = new HashSet<>();    // catalog = katalog; HashSet — Oop05, t12
        Book cleanCode = new Book("Czysty kod", 79);
        catalog.add(cleanCode);
        Book discounted = cleanCode.withPrice(59);           // „zmiana” nie dotyka elementu w zbiorze
        show("catalog.contains(cleanCode)", catalog.contains(cleanCode));
        // WYNIK: catalog.contains(cleanCode) → true
        show("catalog.contains(discounted)", catalog.contains(discounted));
        // WYNIK: catalog.contains(discounted) → false
        note("klucz nie może się „zgubić” jak MutablePoint z Oop05 — jego hashCode nigdy się nie zmieni");
        // WYNIK: ℹ klucz nie może się „zgubić” jak MutablePoint z Oop05 — jego hashCode nigdy się nie zmieni

        // ZALETY:  • bezpieczne współdzielenie (stałe, argumenty metod)  • bezpieczne klucze HashSet/HashMap
        //          • wątki mogą czytać bez synchronizacji (t21)          • łatwe rozumowanie: stan = konstruktor
        // KOSZTY:  • nowy obiekt przy każdej „zmianie” — przy tysiącach zmian w pętli użyj wersji zmiennej
        //            (np. StringBuilder zamiast String), a na końcu zbuduj obiekt niezmienny.
        // DOBRA PRAKTYKA: domyślnie projektuj klasy jako niezmienne; zmienność dodawaj tylko wtedy, gdy
        // naprawdę jej potrzebujesz. Rekordy (t09_records/Records01Basics) robią większość pracy za Ciebie.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Klasa niezmienna: private final pola, brak setterów, final class, walidacja w konstruktorze.
     *   • Tablice: kopia w konstruktorze (clone / Arrays.copyOf) i w getterze.
     *   • Listy: List.copyOf(...) (Java 10+) w konstruktorze — niezmienna kopia, getter może ją oddać.
     *   • „Zmiana” = withX(...) zwraca NOWY obiekt; wynik trzeba przypisać.
     *   • final ≠ niezmienny: final blokuje przepięcie zmiennej, nie zawartość obiektu.
     *   • JDK: String, LocalDate, BigDecimal, Integer — niezmienne; ich metody zwracają nowe obiekty.
     *   • Zalety: bezpieczne współdzielenie, klucze HashSet/HashMap, wątki. Koszt: nowe obiekty.
     *
     * PYTANIA KONTROLNE:
     *   1. Co wypisze:  String s = "Ala"; s.concat("!"); System.out.println(s);
     *   2. Co wypisze:  LocalDate d = LocalDate.of(2026, 1, 1); d.plusDays(5); System.out.println(d);
     *   3. Czy pole  private final int[] data;  sprawia, że obiekt jest niezmienny? Dlaczego?
     *   4. ZNAJDŹ BŁĄD (klasa miała być niezmienna):
     *        Team(List<String> members) { this.members = members; }
     *        List<String> getMembers() { return members; }
     *   5. ZNAJDŹ BŁĄD:  Book withPrice(int p) { this.price = p; return this; }   (pole price jest final)
     *   6. Po co klasie niezmiennej modyfikator final przy class?
     *   7. Dlaczego niezmienne obiekty są dobrymi kluczami w HashSet/HashMap?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: normalizacja kodu", "KOD-RABATOWY", () -> exercise1("  kod rabatowy "));
        Check.equal("ćw. 1: drugi kod", "LATO-2026", () -> exercise1("lato 2026"));
        Check.equal("ćw. 2: dwa razy increment", 2, () -> new Counter(0).increment().increment().getValue());
        Check.equal("ćw. 2: oryginał bez zmian", 5, () -> {
            Counter c = new Counter(5);
            c.increment();
            return c.getValue();
        });
        Check.equal("ćw. 3: zmiana tablicy źródłowej", "Intro", () -> {
            String[] src = {"Intro", "Outro"};
            Playlist p = new Playlist(src);
            src[0] = "HAKER";
            return p.getSongs()[0];
        });
        Check.equal("ćw. 3: zmiana przez getter", "Intro", () -> {
            Playlist p = new Playlist(new String[]{"Intro", "Outro"});
            p.getSongs()[0] = "HAKER";
            return p.getSongs()[0];
        });
        Check.equal("ćw. 4: withItem dodaje pozycję", List.of("kawa", "herbata"),
                () -> new Order("Z1", List.of("kawa")).withItem("herbata").getItems());
        Check.equal("ćw. 4: oryginał bez zmian", List.of("kawa"), () -> {
            Order o = new Order("Z1", List.of("kawa"));
            o.withItem("herbata");
            return o.getItems();
        });
        Check.equal("ćw. 4: zmiana listy źródłowej", 1, () -> {
            List<String> src = new ArrayList<>(List.of("kawa"));   // List.of (Java 9+) = stała lista
            Order o = new Order("Z1", src);
            src.add("HAKER");
            return o.getItems().size();
        });
        Check.throwsException("ćw. 4: getter nie pozwala dodawać", UnsupportedOperationException.class,
                () -> new Order("Z1", new ArrayList<>(List.of("kawa"))).getItems().add("HAKER"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec): normalizacja kodu", "KOD-RABATOWY", () -> solution1("  kod rabatowy "));
        Check.equal("ćw. 1 (wzorzec): drugi kod", "LATO-2026", () -> solution1("lato 2026"));
        Check.equal("ćw. 2 (wzorzec): dwa razy increment", 2,
                () -> new CounterSolution(0).increment().increment().getValue());
        Check.equal("ćw. 2 (wzorzec): oryginał bez zmian", 5, () -> {
            CounterSolution c = new CounterSolution(5);
            c.increment();
            return c.getValue();
        });
        Check.equal("ćw. 3 (wzorzec): zmiana tablicy źródłowej", "Intro", () -> {
            String[] src = {"Intro", "Outro"};
            PlaylistSolution p = new PlaylistSolution(src);
            src[0] = "HAKER";
            return p.getSongs()[0];
        });
        Check.equal("ćw. 3 (wzorzec): zmiana przez getter", "Intro", () -> {
            PlaylistSolution p = new PlaylistSolution(new String[]{"Intro", "Outro"});
            p.getSongs()[0] = "HAKER";
            return p.getSongs()[0];
        });
        Check.equal("ćw. 4 (wzorzec): withItem dodaje pozycję", List.of("kawa", "herbata"),
                () -> new OrderSolution("Z1", List.of("kawa")).withItem("herbata").getItems());
        Check.equal("ćw. 4 (wzorzec): oryginał bez zmian", List.of("kawa"), () -> {
            OrderSolution o = new OrderSolution("Z1", List.of("kawa"));
            o.withItem("herbata");
            return o.getItems();
        });
        Check.equal("ćw. 4 (wzorzec): zmiana listy źródłowej", 1, () -> {
            List<String> src = new ArrayList<>(List.of("kawa"));
            OrderSolution o = new OrderSolution("Z1", src);
            src.add("HAKER");
            return o.getItems().size();
        });
        Check.throwsException("ćw. 4 (wzorzec): getter nie pozwala dodawać", UnsupportedOperationException.class,
                () -> new OrderSolution("Z1", new ArrayList<>(List.of("kawa"))).getItems().add("HAKER"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 10 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): PRZEPISZ poniższy kod tak, by działał. Ma zamienić kod rabatowy na postać
     * „bez spacji na brzegach, spacje w środku → myślnik, wielkie litery” ("  kod rabatowy " → "KOD-RABATOWY"),
     * ale zwraca tekst bez zmian — bo String jest niezmienny, a wyniki metod są wyrzucane:
     * <pre>{@code
     * raw.trim();
     * raw.replace(' ', '-');
     * raw.toUpperCase(Locale.ROOT);
     * return raw;
     * }</pre>
     * Podpowiedź: przypisuj wynik (raw = raw.trim(); ...) albo połącz wywołania w łańcuch.
     * Locale.ROOT = wielkie litery niezależne od języka systemu.
     */
    static String exercise1(String raw) {
        // TODO: twoje rozwiązanie
        return raw;
    }

    /**
     * ĆWICZENIE 2 (łatwe): dokończ niezmienny licznik Counter. Metoda increment() ma zwracać NOWY licznik
     * o wartości większej o 1, a oryginał ma zostać bez zmian.
     * Podpowiedź: wzoruj się na Book.withPrice — jedna linijka z new.
     */
    static final class Counter {                // counter = licznik
        private final int value;

        Counter(int value) {
            this.value = value;
        }

        int getValue() {
            return value;
        }

        Counter increment() {                   // increment = zwiększ o 1
            // TODO: twoje rozwiązanie
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * ĆWICZENIE 3 (średnie): klasa Playlist przecieka jak LeakyGrades z sekcji 4. Popraw konstruktor
     * i getter tak, by ani zmiana tablicy źródłowej, ani zmiana tablicy z gettera nie psuły playlisty.
     * Podpowiedź: dwie kopie obronne — clone() na wejściu i na wyjściu.
     */
    static final class Playlist {               // playlist = lista utworów
        private final String[] songs;           // songs = utwory

        Playlist(String[] songs) {
            this.songs = songs;                 // TODO: twoje rozwiązanie (kopia obronna)
        }

        String[] getSongs() {
            return songs;                       // TODO: twoje rozwiązanie (kopia obronna)
        }
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): dokończ niezmienne zamówienie Order: (a) konstruktor ma zapamiętać
     * niezmienną KOPIĘ listy, (b) withItem(item) ma zwrócić NOWE zamówienie z listą „stare pozycje + item”,
     * nie ruszając oryginału.
     * Podpowiedź: (a) List.copyOf(items); (b) new ArrayList<>(items), add(item), potem new Order(id, nowaLista).
     */
    static final class Order {                  // order = zamówienie
        private final String id;
        private final List<String> items;       // items = pozycje

        Order(String id, List<String> items) {
            this.id = id;
            this.items = items;                 // TODO: twoje rozwiązanie (kopia obronna)
        }

        List<String> getItems() {
            return items;
        }

        Order withItem(String item) {           // withItem = z dodatkową pozycją
            // TODO: twoje rozwiązanie
            throw new UnsupportedOperationException("TODO");
        }

        @Override
        public String toString() {
            return id + " " + items;
        }
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(String raw) {
        return raw.trim().replace(' ', '-').toUpperCase(Locale.ROOT);   // każda metoda zwraca NOWY String
    }

    static final class CounterSolution {
        private final int value;

        CounterSolution(int value) {
            this.value = value;
        }

        int getValue() {
            return value;
        }

        CounterSolution increment() {
            return new CounterSolution(value + 1);
        }
    }

    static final class PlaylistSolution {
        private final String[] songs;

        PlaylistSolution(String[] songs) {
            this.songs = songs.clone();
        }

        String[] getSongs() {
            return songs.clone();
        }
    }

    static final class OrderSolution {
        private final String id;
        private final List<String> items;

        OrderSolution(String id, List<String> items) {
            this.id = id;
            this.items = List.copyOf(items);
        }

        List<String> getItems() {
            return items;
        }

        OrderSolution withItem(String item) {
            List<String> newItems = new ArrayList<>(items);   // robocza, zmienna kopia
            newItems.add(item);
            return new OrderSolution(id, newItems);           // konstruktor i tak zrobi List.copyOf
        }

        @Override
        public String toString() {
            return id + " " + items;
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Ala — concat zwraca NOWY String, a wynik nie został przypisany; s wskazuje stary tekst.
     *   2. 2026-01-01 — plusDays zwraca nową datę, która przepadła. Poprawnie: d = d.plusDays(5);
     *   3. Nie. final blokuje tylko przepięcie pola na inną tablicę; elementy tablicy nadal można zmieniać.
     *      Potrzebne kopie obronne w konstruktorze i getterze.
     *   4. Konstruktor zapamiętuje cudzą listę, a getter oddaje własną — obie strony mogą ją zmienić.
     *      Poprawka: this.members = List.copyOf(members); getter może wtedy zwracać members.
     *   5. Nie skompiluje się (przypisanie do pola final), a idea jest zła: metoda with ma zwrócić NOWY
     *      obiekt: return new Book(title, p);
     *   6. Żeby nikt nie napisał podklasy, która doda zmienne pola albo nadpisze gettery — wtedy obiekt
     *      „niezmiennego” typu mógłby się jednak zmieniać.
     *   7. Bo ich hashCode (liczony z pól) nigdy się nie zmienia — obiekt zawsze leży w „swoim” kubełku
     *      i da się go znaleźć (por. zgubiony MutablePoint z Oop05).
     */
    // </editor-fold>
}
