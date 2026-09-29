package t09_records;

import helpers.Check;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Record — podstawy (Java 16+)
 *        (record = rekord, zapis danych; component = składnik; accessor = metoda dostępu)
 *
 * W SKRÓCIE:
 *   record Point(int x, int y) {} to klasa, którą kompilator uzupełnia za Ciebie: pola private final x i y,
 *   konstruktor Point(int x, int y), metody dostępu x() i y() (BEZ przedrostka get), equals, hashCode i toString.
 *   Rekord jest NIEZMIENNY (pola final, brak setterów) i porównuje się po WARTOŚCIACH. Idealny na dane: punkt,
 *   kwota, wynik zapytania, DTO (obiekt przenoszący dane).
 *
 * ANALOGIA: formularz wydrukowany z gotowymi rubrykami.
 *   Zwykła klasa to pusta kartka — sam rysujesz rubryki, podpisy, ramki (pola, gettery, equals...). Rekord to gotowy
 *   formularz: podajesz tylko nazwy rubryk, a druk (kompilator) robi resztę. Raz wypełnionego nie da się poprawić
 *   długopisem — trzeba wypełnić nowy (niezmienność).
 *
 * JAK TO DZIAŁA:
 *   record Point(int x, int y) {}   ← nagłówek = lista składników (components)
 *   generuje:
 *     private final int x; private final int y;
 *     public Point(int x, int y)    ← konstruktor kanoniczny (canonical = pełny, ze wszystkimi składnikami)
 *     public int x(), public int y() ← metody dostępu (accessors)
 *     equals / hashCode              ← po wartościach WSZYSTKICH składników
 *     toString                       ← "Point[x=1, y=2]"
 *   Rekord jest final (nie da się po nim dziedziczyć) i sam nie może dziedziczyć (niejawnie extends java.lang.Record).
 *
 * SŁÓWKA:
 *   record = rekord; component = składnik; accessor = metoda dostępu; canonical = kanoniczny (pełny, wzorcowy);
 *   immutable = niezmienny; shallow = płytki; DTO (data transfer object) = obiekt przenoszący dane;
 *   point = punkt; book = książka; title = tytuł; author = autor; pages = strony; distance = odległość.
 *
 * ZOBACZ TEŻ: t06_oop_basics/Oop05ObjectMethods (equals/hashCode ręcznie), t06_oop_basics/Oop06Immutability (niezmienność),
 *             t09_records/Records02Constructors (walidacja w rekordzie), t08_enums/Enums02FieldsMethods (enum a record).
 * </pre>
 */
public class Records01Basics {

    // ---------------------------------------------------------------------------------------------
    // PRZED: zwykła klasa z danymi — ok. 40 linii
    // ---------------------------------------------------------------------------------------------

    /** PointClass = punkt jako zwykła klasa. Wszystko pisane ręcznie (albo generowane przez IDE i utrzymywane ręcznie). */
    static final class PointClass {
        private final int x;
        private final int y;

        PointClass(int x, int y) {
            this.x = x;
            this.y = y;
        }

        int getX() {
            return x;
        }

        int getY() {
            return y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof PointClass)) {
                return false;
            }
            PointClass other = (PointClass) o;
            return x == other.x && y == other.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }

        @Override
        public String toString() {
            return "PointClass[x=" + x + ", y=" + y + "]";
        }
    }

    // ---------------------------------------------------------------------------------------------
    // PO: to samo jako rekord — jedna linia
    // ---------------------------------------------------------------------------------------------

    /** Point = punkt. Ten jeden wiersz daje to samo co cała klasa PointClass powyżej. */
    record Point(int x, int y) {

        /** Rekord może mieć własne metody — tak jak klasa. distanceTo = odległość do innego punktu. */
        double distanceTo(Point other) {
            int dx = x - other.x;          // w środku rekordu można używać pól bezpośrednio
            int dy = y - other.y();        // ...albo metod dostępu — to samo
            return Math.sqrt(dx * dx + dy * dy);
        }

        /** Pola i metody STATYCZNE są dozwolone. ORIGIN = początek układu współrzędnych. */
        static final Point ORIGIN = new Point(0, 0);
    }

    /** Book = książka. Rekord ze składnikami różnych typów. */
    record Book(String title, String author, int pages) {
    }

    /** Team = drużyna. Rekord z LISTĄ — pokaże pułapkę płytkiej niezmienności. */
    record Team(String name, List<String> members) {
    }

    public static void main(String[] args) {
        title("Records01 — record: podstawy");

        beforeAfter();              // before/after = przed/po
        accessorsAndToString();     // accessors and toString = metody dostępu i toString
        equalsByValue();            // equals by value = równość po wartościach
        ownMethods();               // own methods = własne metody
        immutability();             // immutability = niezmienność
        shallowImmutability();      // shallow immutability = płytka niezmienność
        limits();                   // limits = ograniczenia
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PRZED / PO
    // =================================================================================================

    /** 1. Klasa ~40 linii i rekord 1 linia zachowują się tak samo — różnią się nazwami metod dostępu i toString. */
    static void beforeAfter() {
        section("1. PRZED (klasa) / PO (record)");

        PointClass before = new PointClass(3, 4);
        Point after = new Point(3, 4);
        show("PRZED", before + ", x=" + before.getX());
        show("PO", after + ", x=" + after.x());
        // WYNIK: PRZED → PointClass[x=3, y=4], x=3
        // WYNIK: PO → Point[x=3, y=4], x=3    ← metoda dostępu to x(), nie getX()

        // DOBRA PRAKTYKA: klasa, która tylko przechowuje niezmienne dane (bez ukrytej logiki i stanu) = record.
        //   Mniej kodu = mniej miejsc na błąd (np. zapomniane pole w equals po dodaniu nowego pola).
    }

    // =================================================================================================
    // 2. METODY DOSTĘPU I toString
    // =================================================================================================

    /** 2. Metody dostępu nazywają się jak składniki: title(), author(), pages(). toString wypisuje wszystkie składniki. */
    static void accessorsAndToString() {
        section("2. Metody dostępu i toString");

        Book book = new Book("Pan Tadeusz", "Adam Mickiewicz", 344);
        show("title()", book.title());
        show("pages()", book.pages());
        show("toString", book);
        // WYNIK: title() → Pan Tadeusz
        // WYNIK: pages() → 344
        // WYNIK: toString → Book[title=Pan Tadeusz, author=Adam Mickiewicz, pages=344]

        // PUŁAPKA: book.getTitle() nie istnieje — to błąd kompilacji. Biblioteki oczekujące getterów „getX”
        //   (starsze frameworki) mogą rekordów nie rozumieć; nowsze (np. Jackson 2.12+) je obsługują.
    }

    // =================================================================================================
    // 3. equals I hashCode PO WARTOŚCIACH
    // =================================================================================================

    /** 3. Dwa rekordy z tymi samymi wartościami są równe (equals) i mają ten sam hashCode — działają w HashSet/HashMap. */
    static void equalsByValue() {
        section("3. equals/hashCode po wartościach");

        Point a = new Point(1, 2);
        Point b = new Point(1, 2);
        show("a == b", a == b);
        show("a.equals(b)", a.equals(b));
        show("ten sam hashCode", a.hashCode() == b.hashCode());
        // WYNIK: a == b → false    ← dwa różne obiekty
        // WYNIK: a.equals(b) → true    ← ale te same wartości
        // WYNIK: ten sam hashCode → true

        Set<Point> visited = new HashSet<>();
        visited.add(new Point(1, 2));
        visited.add(new Point(1, 2));      // duplikat — nie zostanie dodany
        visited.add(new Point(5, 5));
        show("unikalne punkty", visited.size());
        // WYNIK: unikalne punkty → 2
    }

    // =================================================================================================
    // 4. WŁASNE METODY
    // =================================================================================================

    /** 4. Rekord może mieć metody instancji i statyczne. Metody „liczące” coś z danych pasują do rekordu idealnie. */
    static void ownMethods() {
        section("4. Własne metody i pola statyczne");

        show("odległość (3,4) od ORIGIN", new Point(3, 4).distanceTo(Point.ORIGIN));
        // WYNIK: odległość (3,4) od ORIGIN → 5.0
    }

    // =================================================================================================
    // 5. NIEZMIENNOŚĆ
    // =================================================================================================

    /**
     * 5. Pola rekordu są final, nie ma setterów. „Zmiana” = nowy rekord z innymi wartościami.
     * Tak samo działa String: toUpperCase() nie zmienia napisu, tylko zwraca nowy.
     */
    static void immutability() {
        section("5. Niezmienność: „zmiana” = nowy obiekt");

        Point p = new Point(1, 1);
        Point moved = new Point(p.x() + 10, p.y());     // p.x = 11 — błąd kompilacji (pole final)
        show("oryginał", p);
        show("przesunięty", moved);
        // WYNIK: oryginał → Point[x=1, y=1]
        // WYNIK: przesunięty → Point[x=11, y=1]

        // DOBRA PRAKTYKA: obiekty niezmienne można bezpiecznie przekazywać dalej i używać z wielu wątków —
        //   nikt nie zmieni ich „za Twoimi plecami”. Metody typu withX(...) — w Records02Constructors.
    }

    // =================================================================================================
    // 6. PUŁAPKA: PŁYTKA NIEZMIENNOŚĆ
    // =================================================================================================

    /**
     * 6. final chroni tylko REFERENCJĘ (nie da się podmienić listy), ale nie ZAWARTOŚĆ listy.
     * Jeśli ktoś trzyma referencję do tej samej listy, może zmienić „niezmienny” rekord.
     */
    static void shallowImmutability() {
        section("6. Pułapka: płytka niezmienność");

        List<String> names = new ArrayList<>(List.of("Ala", "Olek"));
        Team team = new Team("Orły", names);
        names.add("Intruz");                         // zmieniamy ORYGINALNĄ listę...
        show("rekord po zmianie listy", team);
        // WYNIK: rekord po zmianie listy → Team[name=Orły, members=[Ala, Olek, Intruz]]    ← rekord „zmienił się”!

        team.members().add("Drugi intruz");          // ...albo listę pobraną z rekordu
        show("liczba członków", team.members().size());
        // WYNIK: liczba członków → 4

        // DOBRA PRAKTYKA: w konstruktorze rekordu rób kopię obronną: members = List.copyOf(members);
        //   (niemodyfikowalna kopia) — pokazane w Records02Constructors.
    }

    // =================================================================================================
    // 7. OGRANICZENIA
    // =================================================================================================

    /** 7. Czego rekord NIE może. Każda linia w komentarzu poniżej to błąd kompilacji. */
    static void limits() {
        section("7. Ograniczenia rekordów");

        // record A(int x) extends Object {}        — rekord nie może dziedziczyć (niejawnie extends java.lang.Record)
        // class B extends Point {}                  — po rekordzie nie da się dziedziczyć (jest final)
        // record C(int x) { int extra; }            — nie wolno dodawać pól instancji (tylko static)
        // record D(int x) { void set(int v) { this.x = v; } }  — pola są final
        // Rekord MOŻE: implementować interfejsy, mieć metody, pola i metody statyczne, konstruktory, adnotacje.

        show("Point to podklasa java.lang.Record", new Point(0, 0) instanceof Record);
        show("klasa Point jest final", java.lang.reflect.Modifier.isFinal(Point.class.getModifiers()));
        // WYNIK: Point to podklasa java.lang.Record → true
        // WYNIK: klasa Point jest final → true

        // DOBRA PRAKTYKA: rekord to dane. Jeśli potrzebujesz zmiennego stanu, dziedziczenia albo ukrycia danych
        //   (np. hasło w toString) — użyj zwykłej klasy albo nadpisz toString.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • record R(typ a, typ b) {} → pola private final, konstruktor kanoniczny, a(), b(), equals, hashCode, toString.
     *   • Metody dostępu: a() — bez przedrostka get.
     *   • toString: "R[a=..., b=...]"; equals/hashCode po wartościach wszystkich składników.
     *   • Niezmienny: brak setterów; „zmiana” = nowy rekord.
     *   • Płytka niezmienność: lista w rekordzie może się zmienić — rób List.copyOf w konstruktorze.
     *   • Nie dziedziczy i nie da się po nim dziedziczyć; brak dodatkowych pól instancji; interfejsy i metody — tak.
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie elementy kompilator generuje dla record Book(String title, int pages) {} ?
     *   2. Co wypisze:  System.out.println(new Point(2, 3));  ?
     *   3. ZNAJDŹ BŁĄD:  Book b = new Book("Lalka", "Prus", 680);  System.out.println(b.getTitle());
     *   4. Co wypisze:  System.out.println(new Point(1, 1).equals(new Point(1, 1)) + " " + (new Point(1, 1) == new Point(1, 1)));  ?
     *   5. Czy rekord z polem List jest w pełni niezmienny? Jak to naprawić?
     *   6. Wymień dwie rzeczy, których rekord NIE może, a zwykła klasa może.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        List<Book> books = List.of(new Book("Lalka", "Bolesław Prus", 680),
                new Book("Quo vadis", "Henryk Sienkiewicz", 580),
                new Book("Potop", "Henryk Sienkiewicz", 1200));
        Check.equal("ćw. 1: suma stron", 2460, () -> exercise1(books));
        Check.equal("ćw. 2: najgrubsza książka", "Potop", () -> exercise2(books));
        Check.equal("ćw. 3: przesunięty punkt", new Point(4, 7), () -> exercise3(new Point(1, 2), 3, 5));
        Check.equal("ćw. 4: unikalne punkty", 3, () -> exercise4(List.of(new Point(1, 1), new Point(2, 2),
                new Point(1, 1), new Point(3, 3), new Point(2, 2))));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 2460, () -> solution1(books));
        Check.equal("ćw. 2 (wzorzec)", "Potop", () -> solution2(books));
        Check.equal("ćw. 3 (wzorzec)", new Point(4, 7), () -> solution3(new Point(1, 2), 3, 5));
        Check.equal("ćw. 4 (wzorzec)", 3, () -> solution4(List.of(new Point(1, 1), new Point(2, 2),
                new Point(1, 1), new Point(3, 3), new Point(2, 2))));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /** ĆWICZENIE 1 (łatwe): zsumuj strony wszystkich książek. Podpowiedź: pętla i book.pages(). */
    static int exercise1(List<Book> books) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /** ĆWICZENIE 2 (łatwe): zwróć TYTUŁ książki z największą liczbą stron. Podpowiedź: zapamiętuj „dotychczas najgrubszą”. */
    static String exercise2(List<Book> books) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /** ĆWICZENIE 3 (średnie): zwróć NOWY punkt przesunięty o dx, dy (oryginał bez zmian — rekord jest niezmienny). */
    static Point exercise3(Point p, int dx, int dy) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 4 (średnie): policz unikalne punkty. Podpowiedź: new HashSet<>(points).size() — działa, bo rekord
     * ma equals/hashCode po wartościach. (Sprawdź: z klasą bez equals/hashCode wynik byłby 5.)
     */
    static int exercise4(List<Point> points) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(List<Book> books) {
        int sum = 0;
        for (Book b : books) {
            sum += b.pages();
        }
        return sum;
    }

    static String solution2(List<Book> books) {
        Book thickest = books.get(0);
        for (Book b : books) {
            if (b.pages() > thickest.pages()) {
                thickest = b;
            }
        }
        return thickest.title();
    }

    static Point solution3(Point p, int dx, int dy) {
        return new Point(p.x() + dx, p.y() + dy);
    }

    static int solution4(List<Point> points) {
        return new HashSet<>(points).size();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Pola private final title i pages, konstruktor Book(String title, int pages), metody title() i pages(),
     *      equals, hashCode i toString. Klasa jest final i dziedziczy po java.lang.Record.
     *   2. „Point[x=2, y=3]”.
     *   3. Rekord nie ma getterów „get...” — metoda dostępu to b.title().
     *   4. „true false” — equals porównuje wartości, == porównuje referencje (dwa różne obiekty).
     *   5. Nie — final chroni referencję do listy, ale nie jej zawartość. Naprawa: w konstruktorze kompaktowym
     *      members = List.copyOf(members); (kopia niemodyfikowalna).
     *   6. Np.: dziedziczyć po innej klasie; mieć zmienne pola instancji (settery); mieć dodatkowe pola instancji
     *      poza składnikami; być klasą bazową dla innych klas.
     */
    // </editor-fold>
}
