package t22_design_patterns;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Category;
import helpers.model.Product;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Wzorzec Composite — drzewo, w którym całość i część traktujemy tak samo
 *        (composite = kompozyt, "coś złożonego"; leaf = liść; component = komponent)
 *
 * W SKRÓCIE:
 *   Zestaw produktów może zawierać pojedyncze produkty ORAZ inne zestawy (które znów zawierają produkty...).
 *   Composite pozwala wywołać cenę, liczbę sztuk czy wydruk na dowolnym elemencie drzewa — nie pytając,
 *   czy to liść, czy gałąź. Gałąź po prostu woła tę samą metodę na swoich dzieciach (rekurencja).
 *
 * ANALOGIA: folder na dysku. Folder zawiera pliki i inne foldery. Pytasz "ile to waży?" i nie obchodzi Cię,
 *   czy pytasz o jeden plik, czy o folder z tysiącem podfolderów — odpowiedź to suma. Albo pudełko z prezentami,
 *   w którym jeden z prezentów to mniejsze pudełko z prezentami.
 *
 * JAK TO DZIAŁA:
 *   Role z książki GoF (Gang of Four):
 *
 *                      Component (komponent) — wspólny interfejs: price(), count(), print()
 *                           ▲                       ▲
 *                           │                       │
 *                     Leaf (liść)           Composite (kompozyt)
 *                     robi pracę sam        ma LISTĘ komponentów (dzieci) i deleguje do nich
 *                                                   │
 *                                                   └── children (dzieci): Component, Component, ...
 *
 *   1. Klient zna tylko Component.
 *   2. Leaf liczy odpowiedź bezpośrednio (np. cena produktu).
 *   3. Composite bierze odpowiedź z każdego dziecka i łączy je (suma, maksimum, lista...).
 *   4. Ponieważ dzieckiem może być znów Composite, drzewo ma dowolną głębokość.
 *
 *   Dwa style projektu (sekcja 4): PRZEZROCZYSTY (transparency: add() w Component) kontra BEZPIECZNY
 *   (safety: add() tylko w Composite).
 *
 * SŁÓWKA: tree = drzewo; node = węzeł; children = dzieci; parent = rodzic; depth = głębokość; bundle = zestaw;
 *   total = suma; recursion = rekurencja; traverse = przejść po drzewie; cycle = cykl; flatten = "spłaszczyć" drzewo do listy
 *
 * ZOBACZ TEŻ: t22_design_patterns/Patterns15Visitor (jak dodawać operacje do drzewa bez zmiany jego klas),
 *   t22_design_patterns/Patterns07Decorator (też ma "opakowanie" tego samego interfejsu, ale wokół JEDNEGO elementu),
 *   t18_io_files/Io07WalkingDirectories (prawdziwe drzewo katalogów),
 *   t24_algorithms/Algorithms06Backtracking (rekurencja)
 * </pre>
 */
public class Patterns12Composite {

    public static void main(String[] args) {
        title("Patterns12 — Composite: drzewo zestawów");

        problemTwoLevelsOnly();        // problem two levels only = problem: obsługa tylko dwóch poziomów
        classicComposite();            // classic composite = klasyczny kompozyt
        uniformOperations();           // uniform operations = jednolite operacje na całym drzewie
        safetyVersusTransparency();    // safety versus transparency = bezpieczeństwo kontra przezroczystość
        sealedRecordsTree();           // sealed records tree = drzewo z sealed i rekordów (niezmienne)
        fileSystemTree();              // file system tree = drzewo plików i katalogów
        cyclesPitfall();               // cycles pitfall = pułapka: cykle
        compositeInTheJdk();           // composite in the jdk = kompozyt w JDK i Springu
        testability();                 // testability = testowalność
        pitfallsAndWhenToUse();        // pitfalls and when to use = pułapki i kiedy stosować
        exercises();                   // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PROBLEM: kod, który zna tylko stałą liczbę poziomów
    // =================================================================================================

    /**
     * Zestaw zapisany "po prostu": lista elementów typu Object — produkt albo lista (zestaw w zestawie).
     * Każdy dodatkowy poziom wymaga kolejnej pętli i kolejnego instanceof.
     */
    static BigDecimal totalWithoutPattern(List<Object> items) {  // total = suma; without pattern = bez wzorca
        BigDecimal sum = BigDecimal.ZERO;                         // BigDecimal.ZERO = zero jako BigDecimal
        for (Object item : items) {
            if (item instanceof Product product) {                // instanceof ze zmienną (Java 16+)
                sum = sum.add(product.price());
            } else if (item instanceof List<?> inner) {            // zestaw wewnątrz zestawu
                for (Object innerItem : inner) {
                    if (innerItem instanceof Product product) {
                        sum = sum.add(product.price());
                    } else {
                        throw new IllegalStateException("obsługujemy tylko 2 poziomy zagnieżdżenia");
                    }
                }
            }
        }
        return sum;
    }

    /**
     * 1. Bez wzorca kod "wie", ile jest poziomów. Dwa poziomy działają, trzeci — wyjątek. Dodajmy teraz drugą
     * operację (np. liczenie sztuk): trzeba skopiować CAŁE drzewo instanceof i pętli. Trzecia operacja (wydruk) —
     * znów kopia. Każdy nowy poziom i każda nowa operacja = edycja wielu miejsc.
     */
    static void problemTwoLevelsOnly() {
        section("1. Problem: kod zna stałą liczbę poziomów");

        Product laptop = SampleData.productBySku("ELE-001");
        Product monitor = SampleData.productBySku("ELE-004");
        Product headphones = SampleData.productBySku("ELE-003");

        List<Object> twoLevels = List.of(laptop, List.of(monitor, headphones));  // List.of = lista niezmienna (Java 9+)
        show("suma dla 2 poziomów", totalWithoutPattern(twoLevels));
        // WYNIK: suma dla 2 poziomów → 7148.89

        List<Object> threeLevels = List.of(laptop, List.of(monitor, List.of(headphones)));  // zestaw w zestawie w zestawie
        expectThrows("suma dla 3 poziomów", () -> totalWithoutPattern(threeLevels));
        // WYNIK: ✔ suma dla 3 poziomów → rzucono IllegalStateException: obsługujemy tylko 2 poziomy zagnieżdżenia

        // PUŁAPKA: typ Object gubi informację o tym, co jest w środku. Kompilator nie pomoże, a każda nowa
        // operacja (liczba sztuk, wydruk, szukanie) powiela cały łańcuch instanceof.
        // DOBRA PRAKTYKA: gdy dane są drzewem o nieznanej głębokości, zamodeluj JEDEN wspólny typ dla liścia i gałęzi.
    }

    // =================================================================================================
    // 2. KLASYCZNY KOMPOZYT
    // =================================================================================================

    /** COMPONENT (komponent) — wspólny interfejs liścia i gałęzi. */
    interface CatalogItem {  // catalog item = pozycja katalogu
        String name();                                   // name = nazwa
        BigDecimal price();                              // price = cena (całkowita)
        int count();                                     // count = liczba pojedynczych produktów
        Stream<Product> products();                      // products = produkty w tym poddrzewie (płaska lista)
        void lines(int level, List<String> out);         // lines = dopisz linie wydruku; level = poziom; out = wyjście
        boolean contains(CatalogItem other);             // contains = czy zawiera (w tym samym obiekcie lub poniżej)
    }

    /** LEAF (liść) — pojedynczy produkt. Odpowiada sam. */
    static class SingleProduct implements CatalogItem {  // single product = pojedynczy produkt
        private final Product product;

        SingleProduct(Product product) {
            this.product = product;
        }

        @Override public String name() { return product.name(); }
        @Override public BigDecimal price() { return product.price(); }
        @Override public int count() { return 1; }
        @Override public Stream<Product> products() { return Stream.of(product); }
        @Override public boolean contains(CatalogItem other) { return this == other; }

        @Override
        public void lines(int level, List<String> out) {
            out.add("  ".repeat(level) + "- " + product.name() + " (" + product.price() + " zł)");  // repeat = powtórz (Java 11+)
        }
    }

    /** COMPOSITE (kompozyt) — zestaw: lista dzieci (dowolnych CatalogItem) i delegowanie do nich. */
    static class Bundle implements CatalogItem {  // bundle = zestaw
        private final String name;
        private final List<CatalogItem> children = new ArrayList<>();  // children = dzieci

        Bundle(String name) {
            this.name = name;
        }

        /** add = dodaj. Zwraca this, żeby łączyć wywołania w ciąg (fluent = płynny interfejs). */
        Bundle add(CatalogItem item) {
            if (item.contains(this)) {  // ochrona przed cyklem (sekcja 7)
                throw new IllegalArgumentException("cykl: '" + item.name() + "' zawiera już '" + name + "'");
            }
            children.add(item);
            return this;
        }

        @Override public String name() { return name; }

        @Override
        public BigDecimal price() {  // suma cen dzieci — dziecko może być liściem albo kolejnym zestawem
            BigDecimal sum = BigDecimal.ZERO;
            for (CatalogItem child : children) {
                sum = sum.add(child.price());
            }
            return sum;
        }

        @Override
        public int count() {
            return children.stream().mapToInt(CatalogItem::count).sum();  // mapToInt = przekształć na liczby int
        }

        @Override
        public Stream<Product> products() {
            return children.stream().flatMap(CatalogItem::products);  // flatMap = "spłaszcz": strumień strumieni → jeden strumień
        }

        @Override
        public boolean contains(CatalogItem other) {
            return this == other || children.stream().anyMatch(child -> child.contains(other));  // anyMatch = czy którekolwiek pasuje
        }

        @Override
        public void lines(int level, List<String> out) {
            out.add("  ".repeat(level) + "[zestaw] " + name + " (" + price() + " zł)");
            for (CatalogItem child : children) {
                child.lines(level + 1, out);  // rekurencja: dziecko dopisuje swoje linie z głębszym wcięciem
            }
        }
    }

    /** Budujemy przykładowe drzewo: zestaw biurowy z podzestawem akcesoriów. */
    static Bundle officeBundle() {  // office bundle = zestaw biurowy
        Bundle accessories = new Bundle("Akcesoria")
                .add(new SingleProduct(SampleData.productBySku("ELE-003")))
                .add(new SingleProduct(SampleData.productBySku("DOM-002")));
        return new Bundle("Zestaw biurowy")
                .add(new SingleProduct(SampleData.productBySku("ELE-001")))
                .add(new SingleProduct(SampleData.productBySku("ELE-004")))
                .add(accessories);
    }

    /**
     * 2. Klasyczny kompozyt krok po kroku. Zwróć uwagę, że Bundle.price() NIE sprawdza typu dzieci —
     * po prostu woła price(). Zagnieżdżony zestaw zrobi to samo na swoich dzieciach. Rekurencja kończy się
     * na liściach (SingleProduct), które nie mają dzieci.
     */
    static void classicComposite() {
        section("2. Klasyczny kompozyt: liść + gałąź + wspólny interfejs");

        Bundle office = officeBundle();
        show("cena zestawu", office.price());
        // WYNIK: cena zestawu → 7277.89
        show("liczba produktów", office.count());
        // WYNIK: liczba produktów → 4

        List<String> lines = new ArrayList<>();
        office.lines(0, lines);
        showEach("wydruk drzewa", lines);
        // WYNIK: wydruk drzewa (liczba elementów: 6):
        // WYNIK: • [zestaw] Zestaw biurowy (7277.89 zł)
        // WYNIK: •   - Laptop Pro 14 (5499.99 zł)
        // WYNIK: •   - Monitor 27 cali (1299.00 zł)
        // WYNIK: •   [zestaw] Akcesoria (478.90 zł)
        // WYNIK: •     - Słuchawki BT (349.90 zł)
        // WYNIK: •     - Lampka biurkowa (129.00 zł)

        // DOBRA PRAKTYKA: rekurencja potrzebuje PRZYPADKU BAZOWEGO (liść) i KROKU (gałąź woła dzieci). Gdy gałąź jest
        // pusta (zero dzieci), też jest poprawna — suma zera elementów to zero.
        show("pusty zestaw", new Bundle("Pusty").price());
        // WYNIK: pusty zestaw → 0
    }

    // =================================================================================================
    // 3. JEDNOLITE OPERACJE
    // =================================================================================================

    /** Klient: nie wie i nie chce wiedzieć, czy dostał liść, czy drzewo. */
    static String summarize(CatalogItem item) {  // summarize = podsumuj
        return item.name() + ": " + item.count() + " szt., " + item.price() + " zł";
    }

    /** Szukanie po nazwie w całym drzewie — rekurencja przez strumień produktów. */
    static Optional<Product> cheapest(CatalogItem item) {  // cheapest = najtańszy; Optional = "może wartość" (t14_optional)
        return item.products().min((a, b) -> a.price().compareTo(b.price()));  // min = najmniejszy wg porównania
    }

    /**
     * 3. Operacje na całości i na części wyglądają tak samo. Strumień {@code products()} "spłaszcza" drzewo:
     * liść oddaje jeden produkt, gałąź — sklejone strumienie dzieci (flatMap). Dzięki temu kolejne operacje
     * (najtańszy, lista nazw, filtr po kategorii) piszemy raz, bez ręcznej rekurencji.
     */
    static void uniformOperations() {
        section("3. Jednolite traktowanie liści i gałęzi");

        Bundle office = officeBundle();
        CatalogItem leaf = new SingleProduct(SampleData.productBySku("KSI-001"));
        CatalogItem subBundle = new Bundle("Mały zestaw")
                .add(new SingleProduct(SampleData.productBySku("SPO-002")))
                .add(new SingleProduct(SampleData.productBySku("SPO-001")));

        show("liść", summarize(leaf));
        // WYNIK: liść → Czysty kod: 1 szt., 79.00 zł
        show("podzestaw", summarize(subBundle));
        // WYNIK: podzestaw → Mały zestaw: 2 szt., 72.48 zł
        show("całe drzewo", summarize(office));
        // WYNIK: całe drzewo → Zestaw biurowy: 4 szt., 7277.89 zł

        show("najtańszy w drzewie", cheapest(office).map(Product::name).orElse("brak"));  // map/orElse = przekształć/lub domyślna
        // WYNIK: najtańszy w drzewie → Lampka biurkowa
        show("nazwy w kolejności dodania", office.products().map(Product::name).toList());  // toList = do listy (Java 16+)
        // WYNIK: nazwy w kolejności dodania → [Laptop Pro 14, Monitor 27 cali, Słuchawki BT, Lampka biurkowa]
        show("tylko elektronika", office.products().filter(p -> p.category() == Category.ELEKTRONIKA).count());
        // WYNIK: tylko elektronika → 3
        show("najtańszy w pustym zestawie", cheapest(new Bundle("Pusty")).isPresent());  // isPresent = czy jest wartość
        // WYNIK: najtańszy w pustym zestawie → false

        // PUŁAPKA: price() w gałęzi liczy sumę za KAŻDYM wywołaniem — dla dużych drzew i wielu pytań to kosztowne.
        // Jeśli drzewo jest niezmienne (sekcja 5), możesz bezpiecznie zapamiętać wynik (cache). Przy zmiennym drzewie
        // cache trzeba unieważniać po każdej zmianie, a to częste źródło błędów.
    }

    // =================================================================================================
    // 4. BEZPIECZEŃSTWO KONTRA PRZEZROCZYSTOŚĆ
    // =================================================================================================

    /** Wariant PRZEZROCZYSTY: add() jest w typie bazowym, więc działa na każdym węźle — ale liść go nie obsłuży. */
    abstract static class TransparentNode {  // transparent = przezroczysty; node = węzeł
        abstract int count();

        void add(TransparentNode child) {
            throw new UnsupportedOperationException("liść nie może mieć dzieci");  // przychodzi dopiero w czasie działania
        }
    }

    static class TransparentLeaf extends TransparentNode {
        @Override int count() { return 1; }
    }

    static class TransparentGroup extends TransparentNode {
        private final List<TransparentNode> children = new ArrayList<>();

        @Override void add(TransparentNode child) { children.add(child); }

        @Override int count() { return children.stream().mapToInt(TransparentNode::count).sum(); }
    }

    /**
     * 4. Pytanie projektowe: gdzie umieścić add()? (a) W Component — klient może wołać add() na dowolnym węźle bez
     * rzutowania (przezroczystość), ale na liściu to BŁĄD wykonania. (b) Tylko w Composite — kompilator pilnuje,
     * że nie dodasz dziecka do liścia (bezpieczeństwo), ale klient musi znać typ konkretny, gdy chce budować drzewo.
     * Nasz CatalogItem z sekcji 2 to wariant (b): {@code SingleProduct} w ogóle nie ma metody add.
     */
    static void safetyVersusTransparency() {
        section("4. Gdzie dać add(): przezroczystość czy bezpieczeństwo");

        TransparentNode group = new TransparentGroup();
        TransparentNode leaf = new TransparentLeaf();
        group.add(leaf);                   // działa — grupa umie przyjąć dziecko
        group.add(new TransparentLeaf());
        show("liczba liści w grupie", group.count());
        // WYNIK: liczba liści w grupie → 2
        expectThrows("leaf.add(...) w wariancie przezroczystym", () -> leaf.add(new TransparentLeaf()));
        // WYNIK: ✔ leaf.add(...) w wariancie przezroczystym → rzucono UnsupportedOperationException: liść nie może mieć dzieci

        // W wariancie bezpiecznym taki kod w ogóle się nie skompiluje: błąd kompilacji "cannot find symbol"
        // (nie ma takiej metody) dla new SingleProduct(p).add(...).
        // DOBRA PRAKTYKA: wybieraj BEZPIECZEŃSTWO, gdy klient buduje drzewo rzadko (jedno miejsce, np. fabryka);
        // PRZEZROCZYSTOŚĆ — gdy klient często przetwarza drzewo generycznie i chce traktować wszystko jednakowo
        // (np. edytor graficzny). Błąd kompilacji jest tańszy niż błąd na produkcji.
        // PUŁAPKA: "gruby" interfejs Component z metodami, które liść musi implementować na pusto lub rzucając
        // wyjątek, łamie zasadę segregacji interfejsów (ISP — t27_clean_code_pitfalls/CleanCode02Solid).
        note("kompilator pilnuje wariantu bezpiecznego; wyjątek pilnuje przezroczystego");
        // WYNIK: ℹ kompilator pilnuje wariantu bezpiecznego; wyjątek pilnuje przezroczystego
    }

    // =================================================================================================
    // 5. NOWOCZESNA WERSJA: sealed + rekordy, niezmienne drzewo
    // =================================================================================================

    /**
     * Zapieczętowany (sealed, Java 17) komponent: kompilator zna WSZYSTKIE rodzaje węzłów.
     * Rekordy są niezmienne (immutable), więc cały taki kompozyt jest niezmienny.
     */
    sealed interface Node permits Item, Group {  // sealed = zapieczętowany; permits = dopuszcza
        String name();
        BigDecimal total();  // total = suma
        int count();
    }

    /** Liść jako rekord. */
    record Item(String name, BigDecimal price) implements Node {  // item = pozycja
        @Override public BigDecimal total() { return price; }
        @Override public int count() { return 1; }
    }

    /** Gałąź jako rekord; lista dzieci kopiowana na wejściu (List.copyOf = niezmienna kopia, Java 10+). */
    record Group(String name, List<Node> children) implements Node {  // group = grupa
        Group {  // konstruktor zwarty (compact): walidacja i kopia obronna
            children = List.copyOf(children);
        }

        /** with = "z": zwraca NOWĄ grupę z dodatkowym dzieckiem, oryginał się nie zmienia. */
        Group with(Node child) {
            List<Node> extended = new ArrayList<>(children);
            extended.add(child);
            return new Group(name, extended);
        }

        @Override
        public BigDecimal total() {
            return children.stream().map(Node::total).reduce(BigDecimal.ZERO, BigDecimal::add);  // reduce = zredukuj (złóż) do jednej wartości
        }

        @Override
        public int count() {
            return children.stream().mapToInt(Node::count).sum();
        }
    }

    /** Operacja ZEWNĘTRZNA (poza klasami) z użyciem instanceof (Java 16+). */
    static String label(Node node) {  // label = etykieta
        if (node instanceof Group group) {
            return group.name() + " [" + group.children().size() + " elem.]";
        }
        if (node instanceof Item item) {
            return item.name() + " " + item.price();
        }
        throw new IllegalStateException("nieznany węzeł");  // kompilator nie sprawdza kompletności łańcucha if, więc musimy sami obsłużyć "resztę"
        // (Java 21+): switch ze wzorcami zastępuje łańcuch if i KOMPILATOR sprawdza kompletność:
        //   return switch (node) { case Group g -> ...; case Item i -> ...; };   // bez gałęzi default!
    }

    /** Buduje drzewo z cen SampleData. */
    static Group sampleTree() {
        Group accessories = new Group("Akcesoria", List.of(
                new Item("Słuchawki BT", SampleData.productBySku("ELE-003").price()),
                new Item("Lampka biurkowa", SampleData.productBySku("DOM-002").price())));
        return new Group("Zestaw biurowy", List.of(
                new Item("Laptop Pro 14", SampleData.productBySku("ELE-001").price()),
                new Item("Monitor 27 cali", SampleData.productBySku("ELE-004").price()),
                accessories));
    }

    /**
     * 5. Wersja nowoczesna: dwa rekordy zamiast dwóch klas z polami i konstruktorami. Operacje {@code total()} i
     * {@code count()} są nadal POLIMORFICZNE (każdy rekord zna swoją wersję), więc nie ma żadnego
     * if-instanceof w rekurencji. Klasyczne klasy (sekcja 2) są lepsze, gdy drzewo ma być zmienne (edytor,
     * wiele dodawań w czasie) albo węzły mają ciężki stan i zachowanie.
     */
    static void sealedRecordsTree() {
        section("5. Sealed + rekordy: niezmienne drzewo");

        Group tree = sampleTree();
        show("suma", tree.total());
        // WYNIK: suma → 7277.89
        show("liczba pozycji", tree.count());
        // WYNIK: liczba pozycji → 4
        show("etykieta korzenia", label(tree));
        // WYNIK: etykieta korzenia → Zestaw biurowy [3 elem.]
        show("etykieta liścia", label(tree.children().get(0)));
        // WYNIK: etykieta liścia → Laptop Pro 14 5499.99

        Group extended = tree.with(new Item("Mysz", new BigDecimal("89.00")));
        show("po with(): nowa suma", extended.total());
        // WYNIK: po with(): nowa suma → 7366.89
        show("oryginał bez zmian", tree.total());
        // WYNIK: oryginał bez zmian → 7277.89
        expectThrows("tree.children().add(...)", () -> tree.children().add(new Item("X", BigDecimal.ONE)));
        // WYNIK: ✔ tree.children().add(...) → rzucono UnsupportedOperationException: (brak komunikatu)

        // DOBRA PRAKTYKA: niezmienne drzewo nie ma pułapki cykli (nie da się dodać rodzica do dziecka po
        // utworzeniu) i jest bezpieczne przy wielu wątkach. Cena: zmiana to budowa nowej ścieżki od korzenia.
        // PUŁAPKA: samo słowo record NIE czyni listy niezmienną — bez List.copyOf w konstruktorze zwarty rekord
        // trzymałby referencję do cudzej, zmiennej listy.
    }

    // =================================================================================================
    // 6. DRZEWO PLIKÓW
    // =================================================================================================

    /** Węzeł systemu plików: plik (liść) albo katalog (gałąź). */
    sealed interface FsNode permits FileNode, DirNode {  // fs = system plików (file system)
        String name();
        long size();  // size = rozmiar w bajtach
    }

    record FileNode(String name, long bytes) implements FsNode {  // bytes = bajty
        @Override public long size() { return bytes; }
    }

    record DirNode(String name, List<FsNode> children) implements FsNode {  // dir = katalog (directory)
        DirNode {
            children = List.copyOf(children);
        }

        @Override
        public long size() {
            return children.stream().mapToLong(FsNode::size).sum();  // mapToLong = przekształć na liczby long
        }
    }

    static DirNode sampleFileSystem() {
        return new DirNode("projekt", List.of(
                new FileNode("README.md", 1200),
                new DirNode("src", List.of(new FileNode("Main.java", 3400), new FileNode("Util.java", 800))),
                new DirNode("docs", List.of(new FileNode("raport.pdf", 250_000))),
                new DirNode("pusty", List.of())));
    }

    /** Wydruk drzewa z wcięciami — klasyczna rekurencja po kompozycie. */
    static void render(FsNode node, int level, List<String> out) {  // render = narysuj
        String indent = "  ".repeat(level);
        if (node instanceof DirNode dir) {
            out.add(indent + dir.name() + "/");
            for (FsNode child : dir.children()) {
                render(child, level + 1, out);
            }
        } else if (node instanceof FileNode file) {
            out.add(indent + file.name() + " (" + file.bytes() + " B)");
        }
    }

    /**
     * 6. System plików to podręcznikowy Composite: katalog zawiera pliki i katalogi, a rozmiar katalogu to suma
     * rozmiarów dzieci. Prawdziwe katalogi przechodzisz przez {@code Files.walk} lub {@code Files.walkFileTree}
     * (t18_io_files/Io07WalkingDirectories); tu drzewo jest w pamięci, więc wynik jest zawsze taki sam.
     */
    static void fileSystemTree() {
        section("6. Drzewo plików: katalog = suma dzieci");

        DirNode root = sampleFileSystem();
        show("rozmiar projektu", root.size() + " B");
        // WYNIK: rozmiar projektu → 255400 B
        show("rozmiar src", ((DirNode) root.children().get(1)).size() + " B");  // rzutowanie: wiemy, że element 1 to src
        // WYNIK: rozmiar src → 4200 B
        show("rozmiar pustego katalogu", root.children().get(3).size() + " B");
        // WYNIK: rozmiar pustego katalogu → 0 B

        List<String> lines = new ArrayList<>();
        render(root, 0, lines);
        showEach("drzewo", lines);
        // WYNIK: drzewo (liczba elementów: 8):
        // WYNIK: • projekt/
        // WYNIK: •   README.md (1200 B)
        // WYNIK: •   src/
        // WYNIK: •     Main.java (3400 B)
        // WYNIK: •     Util.java (800 B)
        // WYNIK: •   docs/
        // WYNIK: •     raport.pdf (250000 B)
        // WYNIK: •   pusty/

        // PUŁAPKA: pusty katalog to poprawna gałąź bez dzieci — kod nie może zakładać, że gałąź ma choć jedno dziecko
        // (np. children.get(0) rzuciłoby IndexOutOfBoundsException).
    }

    // =================================================================================================
    // 7. PUŁAPKA: CYKLE
    // =================================================================================================

    /** Zmienny węzeł BEZ ochrony — do pokazania cyklu. */
    static class LooseNode {  // loose = luźny (bez zabezpieczeń)
        final List<LooseNode> children = new ArrayList<>();

        int countAll() {  // countAll = policz wszystkie
            int total = 1;
            for (LooseNode child : children) {
                total += child.countAll();
            }
            return total;
        }
    }

    /**
     * 7. Drzewo musi być DRZEWEM: żadna gałąź nie może (pośrednio) zawierać samej siebie. Gdy zmienny kompozyt
     * pozwoli dodać rodzica do własnego dziecka, rekurencja nigdy się nie skończy.
     */
    static void cyclesPitfall() {
        section("7. Pułapka: cykl w drzewie");

        LooseNode a = new LooseNode();
        LooseNode b = new LooseNode();
        a.children.add(b);
        show("bez cyklu", a.countAll());
        // WYNIK: bez cyklu → 2

        b.children.add(a);  // cykl: a → b → a → b ...
        try {
            a.countAll();
            note("to się nie powinno wydarzyć");
        } catch (StackOverflowError e) {  // przepełnienie stosu wywołań — Error, nie Exception
            note("cykl: rekurencja bez końca kończy się StackOverflowError");
            // WYNIK: ℹ cykl: rekurencja bez końca kończy się StackOverflowError
        }

        // Nasz Bundle (sekcja 2) pilnuje tego w add(): sprawdza, czy dodawany element już zawiera "nas".
        Bundle outer = new Bundle("Zewnętrzny");
        Bundle inner = new Bundle("Wewnętrzny");
        outer.add(inner);
        expectThrows("inner.add(outer)", () -> inner.add(outer));
        // WYNIK: ✔ inner.add(outer) → rzucono IllegalArgumentException: cykl: 'Zewnętrzny' zawiera już 'Wewnętrzny'
        expectThrows("outer.add(outer)", () -> outer.add(outer));
        // WYNIK: ✔ outer.add(outer) → rzucono IllegalArgumentException: cykl: 'Zewnętrzny' zawiera już 'Zewnętrzny'

        // Ten sam liść w DWÓCH gałęziach (to nie cykl, tylko "graf bez cykli") liczy się dwa razy — w zestawie to
        // pożądane (dwie sztuki tego samego produktu), w systemie plików już nie (dowiązania twarde).
        // DOBRA PRAKTYKA: zmienny kompozyt = sprawdzaj cykle przy dodawaniu; niezmienny kompozyt = cykl nie powstanie.
        // PUŁAPKA: bardzo głębokie, ale poprawne drzewo (kilka tysięcy poziomów) też może wyczerpać stos — wtedy
        // przejdź po drzewie pętlą ze stosem (Deque) zamiast rekurencji (t24_algorithms/Algorithms08Graphs — DFS ze stosem).
    }

    // =================================================================================================
    // 8. KOMPOZYT W JDK I W SPRINGU
    // =================================================================================================

    /** Złożony predykat: "wszystkie części muszą być spełnione". Sam jest Predicate, więc można go zagnieżdżać. */
    static Predicate<Product> allOf(List<Predicate<Product>> parts) {  // allOf = wszystkie z; parts = części
        return product -> parts.stream().allMatch(part -> part.test(product));  // allMatch = czy wszystkie pasują
    }

    /**
     * 8. Kompozyt rozpoznasz po tym, że KONTENER jest tego samego typu co jego ELEMENTY. W JDK: w bibliotece
     * okienkowej AWT/Swing {@code java.awt.Container} jest komponentem i zawiera inne komponenty; predykaty
     * ({@code Predicate.and/or/negate}) i komparatory ({@code thenComparing}) składają się w większe predykaty
     * i komparatory tego samego typu. W Springu: {@code CompositeCacheManager} i {@code CompositePropertySource}
     * łączą wiele menedżerów pamięci podręcznej i źródeł właściwości w jeden obiekt o tym samym interfejsie.
     */
    static void compositeInTheJdk() {
        section("8. Kompozyt w JDK i w Springu");

        BigDecimal limit = new BigDecimal("500");  // stała poza lambdą (limit = granica)
        Predicate<Product> available = Product::inStock;                         // liść: jest na stanie
        Predicate<Product> cheap = p -> p.price().compareTo(limit) < 0;          // liść: cena poniżej 500
        Predicate<Product> book = p -> p.category() == Category.KSIAZKI;         // liść: książka

        Predicate<Product> composite = allOf(List.of(available, cheap));         // gałąź z dwóch liści
        Predicate<Product> nested = allOf(List.of(composite, book));             // gałąź zawierająca gałąź

        List<String> cheapAvailable = SampleData.products().stream().filter(composite).map(Product::name).toList();
        show("dostępne i tanie", cheapAvailable);
        // WYNIK: dostępne i tanie → [Słuchawki BT, Kawa ziarnista 1kg, Czekolada gorzka, Czysty kod, Java. Podstawy, Wzorce projektowe, Kurtka zimowa, T-shirt bawełniany, Lampka biurkowa]
        List<String> cheapBooks = SampleData.products().stream().filter(nested).map(Product::name).toList();
        show("dostępne, tanie i książki", cheapBooks);
        // WYNIK: dostępne, tanie i książki → [Czysty kod, Java. Podstawy, Wzorce projektowe]

        // To samo dostępne wprost w JDK: Predicate.and zwraca Predicate (kompozyt z dwóch liści).
        long viaAnd = SampleData.products().stream().filter(available.and(cheap).and(book)).count();
        show("Predicate.and (liczba)", viaAnd);
        // WYNIK: Predicate.and (liczba) → 3

        // DOBRA PRAKTYKA: gdy w nowym projekcie widzisz "lista czegoś, a to coś samo bywa listą", pomyśl o kompozycie.
        // Przykłady: menu z podmenu, kategorie z podkategoriami, drzewo JSON (t18_io_files/Io05JsonManual), wyrażenia
        // matematyczne (Patterns15Visitor), organizacja firmy (dział zawiera działy i pracowników).
    }

    // =================================================================================================
    // 9. TESTOWALNOŚĆ
    // =================================================================================================

    /** Atrapa liścia do testu: produkt o znanej cenie, bez zależności od SampleData. */
    static CatalogItem fake(String name, String price) {  // fake = atrapa
        return new SingleProduct(new Product("T-" + name, name, Category.DOM, new BigDecimal(price), 1));
    }

    /**
     * 9. Kompozyt testuje się łatwo: buduj małe drzewa z atrap o znanych cenach i sprawdzaj sumę bez uruchamiania
     * całego sklepu. Warto testować przypadki brzegowe: pusta gałąź, jeden liść, zagnieżdżenie na kilka poziomów.
     */
    static void testability() {
        section("9. Testowalność: małe drzewa z atrap");

        Bundle empty = new Bundle("pusty");
        Bundle single = new Bundle("jeden").add(fake("a", "10.00"));
        Bundle deep = new Bundle("głęboki")
                .add(fake("b", "1.50"))
                .add(new Bundle("poziom 2").add(fake("c", "2.50")).add(new Bundle("poziom 3").add(fake("d", "4.00"))));

        Check.equal("pusty: cena 0", new BigDecimal("0"), () -> empty.price());
        Check.equal("jeden: cena 10.00", new BigDecimal("10.00"), () -> single.price());
        Check.equal("głęboki: cena 8.00", new BigDecimal("8.00"), () -> deep.price());
        Check.equal("głęboki: 3 sztuki", 3, () -> deep.count());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
        // DOBRA PRAKTYKA: przypadek "pusty" i "głęboki" łapią 90% błędów kompozytu (zapomniany przypadek bazowy,
        // zła suma w zagnieżdżeniu).
    }

    // =================================================================================================
    // 10. PUŁAPKI I KIEDY STOSOWAĆ
    // =================================================================================================

    /**
     * 10. Typowe błędy kompozytu i tabela decyzji.
     */
    static void pitfallsAndWhenToUse() {
        section("10. Pułapki i kiedy stosować");

        // PUŁAPKA: kompozyt tam, gdzie wystarczy płaska lista (YAGNI = "nie będziesz tego potrzebował"). Jeśli zestawy
        // nigdy nie zawierają zestawów, zwykła List<Product> i pętla są prostsze niż trzy klasy.
        // PUŁAPKA: wskaźnik na rodzica (parent) w węźle. Ułatwia wędrówkę w górę, ale trzeba go utrzymywać przy każdym
        // dodaniu i usunięciu — zmienny stan i łatwe niespójności; w niezmiennych drzewach nie da się go ustawić.
        // PUŁAPKA: liść z metodami, które nic nie znaczą (add/remove) — patrz sekcja 4.
        // PUŁAPKA: logika zależna od typu (instanceof) rozsiana po klientach — wtedy kompozyt traci sens; operację
        // dodaj do interfejsu albo użyj wizytatora (Patterns15Visitor).
        note("kompozyt = ten sam interfejs dla liścia i gałęzi; rekurencja robi resztę");
        // WYNIK: ℹ kompozyt = ten sam interfejs dla liścia i gałęzi; rekurencja robi resztę

        // KIEDY UŻYWAĆ / KIEDY NIE:
        //   używaj:  dane są drzewem o nieznanej głębokości (menu, kategorie, pliki, zestawy, wyrażenia, UI);
        //            chcesz traktować jednostkę i grupę jednakowo; operacje mają naturalną rekurencyjną definicję.
        //   nie używaj: struktura jest płaska albo ma stałą głębokość (np. zawsze 2 poziomy); elementy i grupy
        //            mają zupełnie różne operacje; potrzebujesz dostępu "w górę" i "na boki" — wtedy rozważ graf.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Composite = drzewo, w którym liść i gałąź mają WSPÓLNY interfejs (Component); gałąź deleguje do dzieci.
     *   • Role: Component (komponent), Leaf (liść), Composite (kompozyt z listą dzieci), Client (klient).
     *   • Rekurencja potrzebuje przypadku bazowego (liść) i obsługi pustej gałęzi.
     *   • add() w Component = przezroczystość (błąd wykonania); add() tylko w Composite = bezpieczeństwo (błąd kompilacji).
     *   • Nowocześnie: sealed interface + rekordy = niezmienne drzewo; zmiana tworzy nową gałąź (with...).
     *   • Cykl w zmiennym drzewie = StackOverflowError; pilnuj go w add() albo użyj niezmiennych rekordów.
     *   • W JDK i Springu: java.awt.Container, Predicate.and/or, Comparator.thenComparing, CompositeCacheManager.
     *   • Gdy struktura jest płaska, kompozyt łamie YAGNI; nowe OPERACJE na drzewie — Patterns15Visitor.
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie trzy role występują w kompozycie i czym różni się liść od gałęzi?
     *   2. Dlaczego Bundle.price() nie musi sprawdzać typu swoich dzieci?
     *   3. Co wypisze:  new Bundle("x").count()  ?  A jaka będzie cena takiego zestawu?
     *   4. Co wypisze:  Group g = new Group("g", new ArrayList<>(List.of(new Item("a", BigDecimal.TEN))));
     *      g.children().add(new Item("b", BigDecimal.ONE));   ?   (Group to rekord z sekcji 5)
     *   5. ZNAJDŹ BŁĄD:  gałąź zwraca children.get(0).price() jako cenę całego zestawu. Dla jakiego zestawu to zawiedzie?
     *   6. ZNAJDŹ BŁĄD:  void add(Node child) { children.add(child); }  w zmiennym kompozycie, wywołane jako a.add(a).
     *   7. Czym różni się przezroczysty projekt kompozytu od bezpiecznego? Co wybierzesz dla edytora graficznego?
     *   8. Kiedy lepsza jest zwykła płaska lista?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: liczba pozycji", 4, () -> exercise1(sampleTree()));
        Check.equal("ćw. 2: głębokość", 3, () -> exercise2(sampleTree()));
        Check.equal("ćw. 3: suma 3 poziomów", new BigDecimal("7.00"), () -> exercise3(deepGroup()));
        Check.equal("ćw. 4: wydruk drzewa plików", expectedRender(), () -> exercise4(sampleFileSystem()));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 4, () -> solution1(sampleTree()));
        Check.equal("ćw. 2 (wzorzec)", 3, () -> solution2(sampleTree()));
        Check.equal("ćw. 3 (wzorzec)", new BigDecimal("7.00"), () -> solution3(deepGroup()));
        Check.equal("ćw. 4 (wzorzec)", expectedRender(), () -> solution4(sampleFileSystem()));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /** Drzewo testowe do ćwiczenia 3: trzy poziomy, ceny 1 + 2 + 4 = 7. */
    private static Group deepGroup() {
        return new Group("poziom 1", List.of(
                new Item("a", new BigDecimal("1.00")),
                new Group("poziom 2", List.of(
                        new Item("b", new BigDecimal("2.00")),
                        new Group("poziom 3", List.of(new Item("c", new BigDecimal("4.00"))))))));
    }

    private static List<String> expectedRender() {
        return List.of("projekt/", "  README.md (1200 B)", "  src/", "    Main.java (3400 B)", "    Util.java (800 B)",
                "  docs/", "    raport.pdf (250000 B)", "  pusty/");
    }

    /**
     * ĆWICZENIE 1 (łatwe): policz pozycje (liście) w drzewie Node — Item to 1, Group to suma jej dzieci.
     * Podpowiedź: nie pisz nowej rekurencji, w interfejsie Node jest już gotowa metoda count().
     */
    static int exercise1(Node tree) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): oblicz głębokość drzewa: Item ma głębokość 1, Group ma 1 + największa głębokość
     * jej dzieci (pusta grupa: 1). Dla drzewa z sampleTree() wynik to 3.
     * Podpowiedź: rekurencja po Group.children() z instanceof; maksimum policz pętlą albo strumieniem (mapToInt, max).
     */
    static int exercise2(Node tree) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie, PRZEPISZ): stary kod liczył sumę tylko dla DWÓCH poziomów:
     * <pre>{@code
     * BigDecimal sum = BigDecimal.ZERO;
     * for (Node n : g.children()) {
     *     if (n instanceof Item i) { sum = sum.add(i.price()); }
     *     else if (n instanceof Group inner) {
     *         for (Node x : inner.children()) { if (x instanceof Item j) { sum = sum.add(j.price()); } }
     *     }
     * }
     * }</pre>
     * Przepisz na rekurencję, która działa dla dowolnej głębokości — BEZ wołania g.total().
     * Podpowiedź: osobna metoda pomocnicza total(Node), przypadek bazowy Item, krok Group.
     */
    static BigDecimal exercise3(Group g) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zwróć wydruk drzewa plików jako listę linii: katalog = "nazwa/", plik =
     * "nazwa (N B)", wcięcie 2 spacje na poziom (porównaj z render() z sekcji 6, ale zwróć LISTĘ).
     * Podpowiedź: metoda pomocnicza z parametrem level i listą wyjściową, wywołana rekurencyjnie dla dzieci katalogu.
     */
    static List<String> exercise4(FsNode root) {
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(Node tree) {
        return tree.count();
    }

    static int solution2(Node tree) {
        if (tree instanceof Group group) {
            int deepestChild = group.children().stream().mapToInt(Patterns12Composite::solution2).max().orElse(0);
            return 1 + deepestChild;  // pusta grupa: max().orElse(0) → 1 + 0 = 1
        }
        return 1;
    }

    static BigDecimal solution3(Group g) {
        return total(g);
    }

    private static BigDecimal total(Node node) {
        if (node instanceof Group group) {
            BigDecimal sum = BigDecimal.ZERO;
            for (Node child : group.children()) {
                sum = sum.add(total(child));
            }
            return sum;
        }
        return ((Item) node).price();  // Node jest sealed: poza Group zostaje tylko Item
    }

    static List<String> solution4(FsNode root) {
        List<String> out = new ArrayList<>();
        render(root, 0, out);
        return out;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Component (wspólny interfejs), Leaf (liść — robi pracę sam, nie ma dzieci), Composite (gałąź — ma listę
     *      dzieci typu Component i deleguje do nich). Różnica: liść jest końcem rekurencji, gałąź ją kontynuuje.
     *   2. Bo dziecko jest typu CatalogItem — woła się price() polimorficznie: liść zwróci cenę produktu, a zagnieżdżony
     *      zestaw zsumuje swoje dzieci. Typ konkretny nie jest potrzebny.
     *   3. 0 (suma zera elementów) i cena 0 (BigDecimal.ZERO — wypisze się 0).
     *   4. UnsupportedOperationException — konstruktor zwarty Group robi List.copyOf, a to lista niezmienna, więc
     *      add rzuca wyjątek (bez komunikatu). Zmiana oryginalnej listy też nie wpłynęłaby na rekord.
     *   5. Dla pustego zestawu (IndexOutOfBoundsException) oraz dla zestawu z kilkoma elementami (zła, niepełna cena).
     *   6. Cykl: a zawiera samą siebie, więc count()/price() będzie się wołać w nieskończoność — StackOverflowError.
     *      Poprawka: sprawdzić w add(), czy dodawany element nie zawiera już bieżącej gałęzi.
     *   7. Przezroczysty: add() w Component (każdy węzeł ma add, liść rzuca wyjątek w czasie działania);
     *      bezpieczny: add() tylko w Composite (kompilator pilnuje). W edytorze graficznym, gdzie kod traktuje
     *      wszystkie kształty jednakowo, często wybiera się przezroczysty; gdzie drzewo budujesz w jednym miejscu — bezpieczny.
     *   8. Gdy elementy nigdy nie zawierają elementów tego samego rodzaju (stała, płaska struktura).
     */
    // </editor-fold>
}
