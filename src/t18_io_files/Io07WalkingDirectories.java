package t18_io_files;

import helpers.Check;
import helpers.TempDir;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.FileSystemException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Stream;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Przeglądanie katalogów — list, walk, find, glob, walkFileTree
 *        (walk = spacerować; find = znajdź; glob = wzorzec nazw z gwiazdką; visitor = odwiedzający;
 *         tree = drzewo; directory stream = strumień wpisów katalogu)
 *
 * W SKRÓCIE:
 *   Katalog to drzewo: w środku są pliki i podkatalogi, a w nich kolejne. NIO.2 daje kilka sposobów, by je obejść:
 *   list (jeden poziom), walk (całe drzewo), find (całe drzewo z filtrem na atrybutach), newDirectoryStream (jeden poziom
 *   ze wzorcem nazw) i walkFileTree (całe drzewo z „odwiedzającym”, który reaguje na wejście do katalogu i wyjście z niego).
 *
 * ANALOGIA: spacer po budynku z wieloma piętrami i pokojami.
 *   list = zajrzenie do pokoi na jednym piętrze, walk = obejście całego budynku, find = obejście z listą „szukam
 *   pokoi większych niż 20 m²”, walkFileTree = obejście z notatnikiem: przy wejściu do pokoju i przy wyjściu z niego
 *   możesz coś zapisać (a nawet powiedzieć „tego pokoju nie sprawdzaj”).
 *
 * JAK TO DZIAŁA:
 *   Metoda                      Głębokość        Zwraca                    Uwaga
 *   --------------------------  ---------------  ------------------------  ----------------------------------------
 *   Files.list(dir)             1 poziom         Stream (do zamknięcia)  bez samego dir; kolejność dowolna
 *   Files.walk(dir[, max])      całe drzewo      Stream (do zamknięcia)  zaczyna od dir; najpierw rodzic
 *   Files.find(dir, max, test)  całe drzewo      Stream (do zamknięcia)  test dostaje (ścieżka, atrybuty)
 *   Files.newDirectoryStream    1 poziom         DirectoryStream (zamknąć)     wzorzec glob "*.{java,txt}"
 *   Files.walkFileTree          całe drzewo      — (wywołuje odwiedzającego)   SKIP_SUBTREE, TERMINATE, usuwanie, kopiowanie
 *
 *   Trzy zasady: (1) strumienie z list/walk/find ZAMYKAMY (try-with-resources), bo trzymają otwarte uchwyty do
 *   katalogów — w Windows uniemożliwiają potem usunięcie; (2) kolejność wpisów NIE jest gwarantowana — przed wypisaniem
 *   SORTUJEMY; (3) Path jest tylko nazwą — filtrujemy po typie (isRegularFile) i po atrybutach.
 *
 * SŁÓWKA:
 *   list = wypisz; walk = obejdź; find = znajdź; depth = głębokość; visit = odwiedź; skip = pomiń; terminate = zakończ;
 *   continue = kontynuuj; subtree = poddrzewo; attributes = atrybuty; matcher = dopasowywacz; glob = wzorzec nazw;
 *   regex = wyrażenie regularne; sorted = posortowany; reverse = odwrotny; symlink = dowiązanie symboliczne.
 *
 * ZOBACZ TEŻ: t18_io_files/Io01PathFiles (Path i Files), t16_streams/Streams11GroupingBy (strumienie),
 *   t10_exceptions/Exceptions06ChainingWrapping (UncheckedIOException), t18_io_files/Io11IoExceptions (wyjątki IO)
 * </pre>
 */
public class Io07WalkingDirectories {

    public static void main(String[] args) throws IOException {
        // throws IOException = „rzuca IOException” — main przekazuje wyjątek wyżej
        title("Io07 — Przeglądanie katalogów");

        Path dir = TempDir.create("io07"); // create = utwórz; wszystko znika w finally
        try {
            Path tree = dir.resolve("drzewo");
            buildTree(tree);                 // build tree = zbuduj drzewo (wspólne dla sekcji 1–7)

            listOneLevel(tree);              // list one level = jeden poziom
            walkWholeTree(tree);             // walk whole tree = całe drzewo
            findWithAttributes(tree);        // find with attributes = znajdź po atrybutach
            globStream(tree);                // glob stream = strumień ze wzorcem nazw
            pathMatchers(tree);              // path matchers = dopasowywacze ścieżek
            printTreeSection(tree);          // print tree = wypisz drzewo
            walkFileTreeVisitor(tree);       // walk file tree = odwiedzający
            copyAndDelete(tree, dir.resolve("s8")); // copy and delete = kopiowanie i usuwanie
            exercises();                     // exercises = ćwiczenia
        } finally {
            TempDir.deleteRecursively(dir);
        }
    }

    // =================================================================================================
    // Pomocnicze
    // =================================================================================================

    /**
     * rel = ścieżka względem katalogu bazowego, zawsze z ukośnikiem "/".
     * Po co? Katalog tymczasowy ma losową nazwę (innego nie wypisujemy), a Windows pisze ścieżki z "\"
     * (np. "a\b.txt"), Linux z "/" (np. "a/b.txt"). Zamiana na "/" sprawia, że wydruk jest taki sam wszędzie.
     * Dla samego katalogu bazowego zwracamy ".".
     */
    private static String rel(Path base, Path p) {
        if (base.equals(p)) {
            return ".";
        }
        return base.relativize(p).toString().replace('\\', '/');
    }

    /** Akcja, która może rzucić IOException (zwykły Runnable nie może). */
    @FunctionalInterface
    private interface ThrowingIo {
        void run() throws IOException;
    }

    /** Wartość, którą da się policzyć, ale obliczenie może rzucić IOException. */
    @FunctionalInterface
    private interface IoSupplier<T> {
        T get() throws IOException;
    }

    /**
     * ioFails = „IO zawodzi”. Wykonuje akcję i oczekuje wyjątku IO. Wypisuje nazwę klasy wyjątku i nazwę pliku,
     * a NIE komunikat (getMessage): komunikaty IO zawierają pełną ścieżkę (losowy katalog tymczasowy)
     * oraz tekst zależny od systemu i języka, więc wydruk różniłby się na Windows i na Linuksie.
     */
    private static void ioFails(String label, ThrowingIo action) {
        try {
            action.run();
            System.out.println("✘ " + label + " → NIE rzucono wyjątku (a spodziewaliśmy się go)");
        } catch (IOException e) {
            String file = "";
            if (e instanceof FileSystemException fse && fse.getFile() != null) { // instanceof ze wzorcem (Java 16+)
                file = " (plik: " + Path.of(fse.getFile()).getFileName() + ")";
            }
            System.out.println("✔ " + label + " → rzucono " + e.getClass().getSimpleName() + file);
        }
    }

    /** io = opakowuje IOException w UncheckedIOException, żeby pasowało do Supplier w Check.equal. */
    private static <T> Supplier<T> io(IoSupplier<T> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    /**
     * buildTree = zbuduj drzewo testowe o znanych rozmiarach plików:
     * <pre>
     * drzewo/build.txt (2 B), docs/readme.txt (12 B), docs/img/logo.bin (100 B), empty/ (pusty katalog),
     * src/Main.java (14 B), src/util/Helper.java (16 B), src/util/notes.txt (18 B)
     * </pre>
     * Teksty zapisujemy z jawnym "\n" i UTF-8, więc rozmiary są takie same w Windows i Linuksie.
     */
    private static void buildTree(Path root) throws IOException {
        Files.createDirectories(root.resolve("src/util"));
        Files.createDirectories(root.resolve("docs/img"));
        Files.createDirectories(root.resolve("empty"));
        Files.writeString(root.resolve("src/Main.java"), "class Main {}\n", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("src/util/Helper.java"), "class Helper {}\n", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("src/util/notes.txt"), "uwagi: zażółć\n", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("docs/readme.txt"), "Czytaj mnie\n", StandardCharsets.UTF_8);
        Files.write(root.resolve("docs/img/logo.bin"), new byte[100]); // 100 bajtów zer
        Files.writeString(root.resolve("build.txt"), "b\n", StandardCharsets.UTF_8);
    }

    /** relSorted = zamień strumień ścieżek na posortowaną listę ścieżek względnych (kolejność strumienia jest dowolna). */
    private static List<String> relSorted(Path base, Stream<Path> stream) {
        return stream.map(p -> rel(base, p)).sorted().toList(); // toList = niezmienna lista (Java 16+)
    }

    // =================================================================================================
    // 1. Files.list — JEDEN POZIOM
    // =================================================================================================

    /**
     * 1. Files.list daje zawartość jednego katalogu (bez wchodzenia głębiej i bez samego katalogu). To strumień,
     * więc trzeba go zamknąć. Kolejność jest dowolna: raz może wyjść alfabetycznie, raz nie.
     */
    static void listOneLevel(Path tree) throws IOException {
        section("1. Files.list — jeden poziom");

        try (Stream<Path> stream = Files.list(tree)) { // list = wypisz zawartość; try-with-resources zamyka strumień
            show("zawartość (posortowana)", relSorted(tree, stream));
            // WYNIK: zawartość (posortowana) → [build.txt, docs, empty, src]
        }

        // Tylko katalogi / tylko zwykłe pliki — filtr po typie, bo Path sam nic nie wie o dysku:
        try (Stream<Path> stream = Files.list(tree)) {
            show("tylko katalogi", relSorted(tree, stream.filter(Files::isDirectory)));
            // WYNIK: tylko katalogi → [docs, empty, src]
        }
        try (Stream<Path> stream = Files.list(tree)) {
            show("tylko pliki", relSorted(tree, stream.filter(Files::isRegularFile)));
            // WYNIK: tylko pliki → [build.txt]
        }

        ioFails("list na nieistniejącym katalogu", () -> Files.list(tree.resolve("nie-ma")).close());
        // WYNIK: ✔ list na nieistniejącym katalogu → rzucono NoSuchFileException (plik: nie-ma)

        // PUŁAPKA: Files.list(dir) bez try-with-resources „działa”, ale nie zamyka katalogu. W Linuksie wycieka
        //   uchwyt (po tysiącach wywołań: „Too many open files”), a w Windows nie da się potem usunąć katalogu.
        // PUŁAPKA: kolejność nie jest alfabetyczna ani stała — systemy plików zwracają wpisy po swojemu. Test, który
        //   zakłada kolejność, przejdzie u Ciebie i padnie u kolegi. Zawsze sortuj przed porównaniem lub wypisaniem.
        // DOBRA PRAKTYKA: try (Stream<Path> s = Files.list(dir)) { ... } — zawsze. Dlaczego: zamknięcie zwalnia zasób
        //   systemowy natychmiast, a nie „kiedyś, gdy zbierze to odśmiecacz”.
    }

    // =================================================================================================
    // 2. Files.walk — CAŁE DRZEWO
    // =================================================================================================

    /**
     * 2. Files.walk obchodzi całe drzewo „w głąb”: najpierw katalog, potem jego zawartość. Zwraca także sam katalog
     * startowy. Drugi argument ogranicza głębokość (0 = tylko start, 1 = start i jego dzieci). Błędy w trakcie
     * obchodu wychodzą jako UncheckedIOException (strumień nie może rzucać sprawdzanych wyjątków).
     */
    static void walkWholeTree(Path tree) throws IOException {
        section("2. Files.walk — całe drzewo");

        try (Stream<Path> stream = Files.walk(tree)) { // walk = obejdź drzewo
            showEach("całe drzewo (posortowane)", relSorted(tree, stream));
            // WYNIK: całe drzewo (posortowane) (liczba elementów: 12):
            // WYNIK: • .
            // WYNIK: • build.txt
            // WYNIK: • docs
            // WYNIK: • docs/img
            // WYNIK: • docs/img/logo.bin
            // WYNIK: • docs/readme.txt
            // WYNIK: • empty
            // WYNIK: • src
            // WYNIK: • src/Main.java
            // WYNIK: • src/util
            // WYNIK: • src/util/Helper.java
            // WYNIK: • src/util/notes.txt
        }
        // Kropka na liście to sam katalog startowy (rel zwraca "." dla katalogu bazowego).

        for (int depth : new int[] {0, 1, 2, Integer.MAX_VALUE}) {
            try (Stream<Path> stream = Files.walk(tree, depth)) { // drugi argument = maksymalna głębokość
                show("maxDepth " + (depth == Integer.MAX_VALUE ? "bez limitu" : String.valueOf(depth)), stream.count());
            }
        }
        // WYNIK: maxDepth 0 → 1
        // WYNIK: maxDepth 1 → 5
        // WYNIK: maxDepth 2 → 9
        // WYNIK: maxDepth bez limitu → 12

        // Tylko zwykłe pliki, posortowane, i suma ich rozmiarów:
        try (Stream<Path> stream = Files.walk(tree)) {
            List<Path> files = stream.filter(Files::isRegularFile).toList();
            long total = 0;
            for (Path f : files) {
                total += Files.size(f); // size = rozmiar w bajtach; może rzucić IOException
            }
            show("liczba plików", files.size());
            // WYNIK: liczba plików → 6
            show("suma rozmiarów (bajty)", total);
            // WYNIK: suma rozmiarów (bajty) → 162
        }
        // Dlaczego 162? 2 + 12 + 100 + 14 + 16 + 18. Notatki mają polskie litery (2 bajty każda w UTF-8): "uwagi: zażółć\n" to 18 bajtów.

        // Przekształcenie ścieżki z wnętrza strumienia: Files.size rzuca IOException, której lambda nie może rzucić.
        // Dlatego: albo pętla for jak wyżej, albo opakowanie w UncheckedIOException:
        try (Stream<Path> stream = Files.walk(tree)) {
            long bytes = stream.filter(Files::isRegularFile).mapToLong(p -> {
                try {
                    return Files.size(p);
                } catch (IOException e) {
                    throw new UncheckedIOException(e); // opakowanie wyjątku sprawdzanego w niesprawdzany (t10_exceptions)
                }
            }).sum();
            show("to samo przez mapToLong", bytes);
            // WYNIK: to samo przez mapToLong → 162
        }

        // PUŁAPKA: Files.walk niczego nie sortuje, a katalogi czyta leniwie, w miarę pracy na strumieniu (dlatego trzeba
        //   go zamknąć). Dowiązania symboliczne domyślnie NIE są śledzone (zobacz sekcję 8).
        // DOBRA PRAKTYKA: ogranicz głębokość, gdy wiesz, ile poziomów Cię interesuje (walk(dir, 2)). Dlaczego:
        //   mniej wejść do katalogów, a przy błędnie wskazanym katalogu głównym (np. korzeń dysku) nie przeszukasz całego dysku.
    }

    // =================================================================================================
    // 3. Files.find — FILTR NA ATRYBUTACH
    // =================================================================================================

    /**
     * 3. Files.find(start, maxDepth, test) robi to samo co walk + filter, ale test dostaje także atrybuty pliku
     * (BasicFileAttributes), które system zwrócił już przy obchodzeniu katalogu. Nie trzeba pytać dysku drugi raz.
     */
    static void findWithAttributes(Path tree) throws IOException {
        section("3. Files.find — filtr z atrybutami");

        try (Stream<Path> stream = Files.find(tree, Integer.MAX_VALUE, // find = znajdź; głębokość bez limitu
                (path, attrs) -> attrs.isRegularFile() && attrs.size() > 15)) { // BiPredicate = test z dwoma argumentami
            show("pliki większe niż 15 bajtów", relSorted(tree, stream));
            // WYNIK: pliki większe niż 15 bajtów → [docs/img/logo.bin, src/util/Helper.java, src/util/notes.txt]
        }
        try (Stream<Path> stream = Files.find(tree, Integer.MAX_VALUE,
                (path, attrs) -> attrs.isRegularFile() && path.getFileName().toString().endsWith(".java"))) {
            show("pliki .java", relSorted(tree, stream));
            // WYNIK: pliki .java → [src/Main.java, src/util/Helper.java]
        }
        try (Stream<Path> stream = Files.find(tree, Integer.MAX_VALUE, (path, attrs) -> attrs.isDirectory())) {
            show("katalogi (z korzeniem)", relSorted(tree, stream));
            // WYNIK: katalogi (z korzeniem) → [., docs, docs/img, empty, src, src/util]
        }

        // Atrybuty: isRegularFile, isDirectory, isSymbolicLink, size, lastModifiedTime, creationTime.
        // Czasów prawdziwych nie wypisujemy (zmieniają się przy każdym uruchomieniu — zobacz Io01PathFiles).

        // DOBRA PRAKTYKA: gdy filtrujesz po rozmiarze, czasie lub typie, użyj Files.find zamiast walk + Files.size/isDirectory.
        //   Dlaczego: atrybuty są już odczytane, więc unikasz kolejnego zapytania do systemu dla każdego pliku
        //   (w dużych drzewach to różnica widoczna gołym okiem, zwłaszcza na dysku sieciowym).
        // PUŁAPKA: lambda w find ma DWA argumenty (ścieżka, atrybuty) — to BiPredicate, nie zwykły Predicate.
    }

    // =================================================================================================
    // 4. newDirectoryStream Z WZORCEM GLOB
    // =================================================================================================

    /**
     * 4. newDirectoryStream(katalog, "wzorzec") to jeden poziom z filtrem na nazwach. Wzorzec glob (nie wyrażenie
     * regularne): * = dowolnie wiele znaków w nazwie, ? = jeden znak, [a-c] = jeden z zakresu, {a,b} = jedna z opcji.
     */
    static void globStream(Path tree) throws IOException {
        section("4. newDirectoryStream i wzorzec glob");

        Path util = tree.resolve("src/util");
        List<String> found = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(util, "*.{java,txt}")) { // newDirectoryStream = strumień wpisów katalogu
            for (Path p : stream) { // DirectoryStream jest Iterable, więc działa w for-each
                found.add(rel(tree, p));
            }
        }
        Collections.sort(found); // kolejność dowolna → sortujemy
        show("*.{java,txt} w src/util", found);
        // WYNIK: *.{java,txt} w src/util → [src/util/Helper.java, src/util/notes.txt]

        found.clear();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(util, "*.java")) {
            stream.forEach(p -> found.add(rel(tree, p)));
        }
        show("*.java w src/util", found);
        // WYNIK: *.java w src/util → [src/util/Helper.java]

        found.clear();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tree, "?????")) { // pięć dowolnych znaków
            stream.forEach(p -> found.add(rel(tree, p)));
        }
        show("nazwy o pięciu znakach w korzeniu", found);
        // WYNIK: nazwy o pięciu znakach w korzeniu → [empty]

        // Zamiast glob można podać własny filtr (DirectoryStream.Filter) — np. tylko katalogi:
        found.clear();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tree, Files::isDirectory)) {
            stream.forEach(p -> found.add(rel(tree, p)));
        }
        Collections.sort(found);
        show("filtr: tylko katalogi", found);
        // WYNIK: filtr: tylko katalogi → [docs, empty, src]

        // Różnice względem Files.list: DirectoryStream jest leniwy, nie tworzy Stream API (używa się pętli),
        // ma wbudowany glob, a iterator można pobrać tylko raz. Obie wersje trzeba zamykać.

        // PUŁAPKA: glob nie jest regex. "*.java" w glob to "dowolne znaki i .java"; w wyrażeniu regularnym "*.java" jest
        //   błędem. A "*" w glob NIE przekracza granicy katalogu: "*.java" w korzeniu nie znajdzie src/Main.java.
        // PUŁAPKA: wzorzec dotyczy NAZWY pliku (bez katalogów). Do dopasowania całej ścieżki służy PathMatcher (sekcja 5).
        // DOBRA PRAKTYKA: DirectoryStream (jeden poziom + wzorzec nazw) jest lżejszy niż list + filter przy ogromnych
        //   katalogach. Dlaczego: nie tworzy całego strumienia obiektów, tylko podaje wpisy po kolei.
    }

    // =================================================================================================
    // 5. PathMatcher: GLOB KONTRA REGEX
    // =================================================================================================

    /**
     * 5. PathMatcher to gotowy „test” dopasowania ścieżki do wzorca. Składnia podawana jest w przedrostku:
     * "glob:..." albo "regex:...". Wzorzec dotyczy całej ścieżki, jaką mu podamy — dlatego ścieżki do testu
     * najlepiej robić względnymi.
     */
    static void pathMatchers(Path tree) throws IOException {
        section("5. PathMatcher: glob i regex");

        PathMatcher anyJava = FileSystems.getDefault().getPathMatcher("glob:**/*.java"); // ** = także przez katalogi
        PathMatcher topJava = FileSystems.getDefault().getPathMatcher("glob:*.java");     // * = tylko w jednej nazwie
        PathMatcher either = FileSystems.getDefault().getPathMatcher("glob:{*.java,**/*.java}");

        Path deep = Path.of("src/util/Helper.java");
        Path top = Path.of("Main.java");
        show("**/*.java ← src/util/Helper.java", anyJava.matches(deep)); // matches = czy pasuje
        // WYNIK: **/*.java ← src/util/Helper.java → true
        show("**/*.java ← Main.java (bez katalogu)", anyJava.matches(top));
        // WYNIK: **/*.java ← Main.java (bez katalogu) → false
        show("*.java ← Main.java", topJava.matches(top));
        // WYNIK: *.java ← Main.java → true
        show("*.java ← src/util/Helper.java", topJava.matches(deep));
        // WYNIK: *.java ← src/util/Helper.java → false
        show("{*.java,**/*.java} ← oba", either.matches(top) && either.matches(deep));
        // WYNIK: {*.java,**/*.java} ← oba → true

        // Zastosowanie: pliki .java z całego drzewa — dopasowujemy ścieżkę WZGLĘDNĄ.
        try (Stream<Path> stream = Files.walk(tree)) {
            List<String> java = stream.filter(Files::isRegularFile)
                    .filter(p -> either.matches(tree.relativize(p)))
                    .map(p -> rel(tree, p)).sorted().toList();
            show("pliki .java przez PathMatcher", java);
            // WYNIK: pliki .java przez PathMatcher → [src/Main.java, src/util/Helper.java]
        }

        // Regex dopasowuje cały napis ścieżki (z separatorami systemu!):
        PathMatcher regex = FileSystems.getDefault().getPathMatcher("regex:.*\\.(java|txt)"); // kropka musi mieć ukośnik
        show("regex ← notes.txt", regex.matches(Path.of("notes.txt")));
        // WYNIK: regex ← notes.txt → true
        show("regex ← notes.csv", regex.matches(Path.of("notes.csv")));
        // WYNIK: regex ← notes.csv → false

        // Dopasowanie samej nazwy pliku: matches(path.getFileName()).
        PathMatcher txt = FileSystems.getDefault().getPathMatcher("glob:*.txt");
        show("nazwa pliku src/util/notes.txt pasuje do *.txt", txt.matches(Path.of("src/util/notes.txt").getFileName()));
        // WYNIK: nazwa pliku src/util/notes.txt pasuje do *.txt → true

        // PUŁAPKA: "**/*.java" wymaga co najmniej jednego katalogu w ścieżce — plik Main.java w korzeniu NIE pasuje.
        //   Stąd wzorzec {*.java,**/*.java}.
        // PUŁAPKA: w wyrażeniu regularnym separator ścieżki to w Windows "\" (zapisywany w regex jako dwa ukośniki), a w Linuksie
        //   "/" — wzorzec regex przenośny jest trudny. Glob tłumaczy "/" sam na separator systemu, więc preferuj glob.
        // PUŁAPKA: wielkość liter: w Windows dopasowanie do nazw plików jest nieczułe na wielkość liter, w Linuksie czułe.
        // DOBRA PRAKTYKA: wzorzec glob zapisany w konfiguracji (np. "include=**/*.java") zamień na PathMatcher RAZ,
        //   a potem używaj go wielokrotnie. Dlaczego: getPathMatcher kompiluje wzorzec — to kosztuje, gdy robisz to w pętli.
    }

    // =================================================================================================
    // 6. DRZEWO Z GAŁĘZIAMI
    // =================================================================================================

    /** printTree = zbierz linie rysunku drzewa; własna rekurencja z posortowanymi dziećmi (stały wynik). */
    private static void printTree(Path dir, String prefix, List<String> out) throws IOException {
        List<Path> children;
        try (Stream<Path> stream = Files.list(dir)) {
            children = stream.sorted(Comparator.comparing((Path p) -> p.getFileName().toString())).toList();
        } // strumień zamknięty ZANIM zaczniemy schodzić głębiej (mniej otwartych katalogów naraz)
        for (int i = 0; i < children.size(); i++) {
            Path child = children.get(i);
            boolean last = i == children.size() - 1;
            out.add(prefix + (last ? "└── " : "├── ") + child.getFileName());
            if (Files.isDirectory(child)) {
                printTree(child, prefix + (last ? "    " : "│   "), out);
            }
        }
    }

    /**
     * 6. Rysunek drzewa jak polecenie tree. Własna rekurencja (zamiast walk) pozwala SAMEMU wybrać kolejność:
     * sortujemy dzieci każdego katalogu, a prefiks mówi, czy rysujemy pionową kreskę (są jeszcze młodsi bracia).
     */
    static void printTreeSection(Path tree) throws IOException {
        section("6. Drzewo z gałęziami");

        List<String> lines = new ArrayList<>();
        lines.add(tree.getFileName().toString());
        printTree(tree, "", lines);
        for (String line : lines) {
            System.out.println(line);
        }
        // WYNIK: drzewo
        // WYNIK: ├── build.txt
        // WYNIK: ├── docs
        // WYNIK: │   ├── img
        // WYNIK: │   │   └── logo.bin
        // WYNIK: │   └── readme.txt
        // WYNIK: ├── empty
        // WYNIK: └── src
        // WYNIK:     ├── Main.java
        // WYNIK:     └── util
        // WYNIK:         ├── Helper.java
        // WYNIK:         └── notes.txt

        // Jak to działa: ostatnie dziecko dostaje "└── " i jego dzieci rysujemy z prefiksem z samych spacji;
        // pozostałe dostają "├── " i ich dzieci prefiks z pionową kreską "│   ".

        // DOBRA PRAKTYKA: do „ładnych” wydruków i raportów pisz własną rekurencję z sortowaniem; Files.walk zostaw
        //   do zadań, w których kolejność nie gra roli (liczenie, szukanie, sumowanie).
        // PUŁAPKA: rekurencja po bardzo głębokim drzewie (tysiące poziomów) może zakończyć się StackOverflowError —
        //   dla zwykłych projektów nie grozi, ale pętla z walkFileTree jest na to odporna.
    }

    // =================================================================================================
    // 7. walkFileTree I ODWIEDZAJĄCY
    // =================================================================================================

    /**
     * 7. walkFileTree(start, visitor) wywołuje metody odwiedzającego: preVisitDirectory (wchodzimy do katalogu),
     * visitFile (plik), postVisitDirectory (wychodzimy z katalogu — po przejrzeniu zawartości) i
     * visitFileFailed (nie udało się odczytać). Każda zwraca FileVisitResult: CONTINUE (dalej), SKIP_SUBTREE
     * (pomiń zawartość tego katalogu), SKIP_SIBLINGS (pomiń rodzeństwo), TERMINATE (zakończ całe obejście).
     */
    static void walkFileTreeVisitor(Path tree) throws IOException {
        section("7. walkFileTree i SimpleFileVisitor");

        // Rozmiary katalogów: stos sum; wchodzimy → nowa suma 0, plik → dodaj, wychodzimy → dodaj sumę do rodzica.
        Map<String, Long> sizes = new TreeMap<>();
        Deque<Long> stack = new ArrayDeque<>(); // Deque = kolejka dwustronna; tu jako stos (push/pop)
        Files.walkFileTree(tree, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                stack.push(0L);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                stack.push(stack.pop() + attrs.size()); // pop = zdejmij ze stosu, push = połóż
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc; // błąd przy czytaniu katalogu: nie udajemy, że wszystko gra
                }
                long total = stack.pop();
                sizes.put(rel(tree, dir), total);
                if (!stack.isEmpty()) {
                    stack.push(stack.pop() + total);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        showEach("rozmiary katalogów (bajty)", sizes);
        // WYNIK: rozmiary katalogów (bajty) (liczba kluczy: 6):
        // WYNIK: • . → 162
        // WYNIK: • docs → 112
        // WYNIK: • docs/img → 100
        // WYNIK: • empty → 0
        // WYNIK: • src → 48
        // WYNIK: • src/util → 34

        // SKIP_SUBTREE: nie wchodzimy do katalogów "img" i "util".
        List<String> visited = new ArrayList<>();
        Files.walkFileTree(tree, new SimpleFileVisitor<>() { // diament przy klasie anonimowej: Java 9+
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                String name = dir.getFileName().toString();
                return name.equals("img") || name.equals("util") ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                visited.add(rel(tree, file));
                return FileVisitResult.CONTINUE;
            }
        });
        Collections.sort(visited);
        show("po pominięciu img i util", visited);
        // WYNIK: po pominięciu img i util → [build.txt, docs/readme.txt, src/Main.java]

        // TERMINATE: kończymy po trzecim pliku. KTÓRE trzy to kwestia kolejności (nieokreślonej) — liczba jest pewna.
        int[] count = {0}; // tablica jednoelementowa: klasa anonimowa może zmieniać tylko zawartość, nie zmienną
        Files.walkFileTree(tree, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                count[0]++;
                return count[0] == 3 ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
            }
        });
        show("odwiedzone pliki przed TERMINATE", count[0]);
        // WYNIK: odwiedzone pliki przed TERMINATE → 3

        // visitFileFailed: wołane, gdy nie da się odczytać pliku/katalogu (brak uprawnień, plik zniknął w trakcie,
        // pętla dowiązań). Domyślnie SimpleFileVisitor rzuca ten wyjątek dalej — obejście się przerywa.
        Path missing = tree.resolve("nie-ma");
        ioFails("domyślny odwiedzający, brak katalogu startowego",
                () -> Files.walkFileTree(missing, new SimpleFileVisitor<Path>() { }));
        // WYNIK: ✔ domyślny odwiedzający, brak katalogu startowego → rzucono NoSuchFileException (plik: nie-ma)
        List<String> failures = new ArrayList<>();
        Files.walkFileTree(missing, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                failures.add(exc.getClass().getSimpleName() + " (" + file.getFileName() + ")");
                return FileVisitResult.CONTINUE; // zapisujemy błąd i idziemy dalej
            }
        });
        show("własne visitFileFailed", failures);
        // WYNIK: własne visitFileFailed → [NoSuchFileException (nie-ma)]

        // DOBRA PRAKTYKA: w produkcyjnym obejściu nadpisz visitFileFailed: jeden nieczytelny plik (brak uprawnień)
        //   nie powinien przerywać przeglądania całego dysku. Dlaczego: domyślnie wyjątek kończy cały walkFileTree.
        // PUŁAPKA: kolejność wizyt w walkFileTree też jest nieokreślona — nie opieraj się na niej (stąd sortowania wyżej).
    }

    // =================================================================================================
    // 8. KOPIOWANIE I USUWANIE DRZEW
    // =================================================================================================

    /** copyTree = skopiuj całe drzewo: katalogi tworzymy przy wejściu, pliki kopiujemy przy odwiedzinach. */
    private static void copyTree(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file).toString()));
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /** deleteTree = usuń całe drzewo: pliki przy odwiedzinach, katalog dopiero PO opróżnieniu (postVisitDirectory). */
    private static void deleteTree(Path root) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * 8. Files.copy kopiuje tylko JEDEN katalog (pusty) lub plik — drzewo trzeba obejść. Usuwanie wymaga kolejności
     * „dzieci przed rodzicem”: dwa sposoby — walk + sortowanie malejące albo walkFileTree z postVisitDirectory.
     */
    static void copyAndDelete(Path source, Path base) throws IOException {
        section("8. Kopiowanie i usuwanie drzew");
        Files.createDirectory(base);

        Path copy = base.resolve("kopia");
        copyTree(source, copy);
        try (Stream<Path> a = Files.walk(source); Stream<Path> b = Files.walk(copy)) { // dwa zasoby w jednym try
            show("kopia ma tę samą listę wpisów", relSorted(source, a).equals(relSorted(copy, b)));
            // WYNIK: kopia ma tę samą listę wpisów → true
        }
        show("kopia: pusty katalog też jest", Files.isDirectory(copy.resolve("empty")));
        // WYNIK: kopia: pusty katalog też jest → true

        // Usuwanie błędnie: walk zaczyna od rodzica, więc delete trafia na niepusty katalog.
        ioFails("walk + delete bez sortowania", () -> {
            try (Stream<Path> stream = Files.walk(copy)) {
                for (Path p : stream.toList()) {
                    Files.delete(p);
                }
            }
        });
        // WYNIK: ✔ walk + delete bez sortowania → rzucono DirectoryNotEmptyException (plik: kopia)

        // Usuwanie dobrze (1): sortowanie malejące. Path sortuje się tak, że rodzic jest PRZED dziećmi
        // ("src" < "src/Main.java"), więc po odwróceniu dzieci są przed rodzicem. Tak działa helpers.TempDir.
        List<Path> order;
        try (Stream<Path> stream = Files.walk(copy)) {
            order = stream.sorted(Comparator.reverseOrder()).toList(); // reverseOrder = odwrotna kolejność
        } // lista zebrana, strumień zamknięty — dopiero teraz usuwamy
        showEach("kolejność usuwania", order.stream().map(p -> rel(copy, p)).toList());
        // WYNIK: kolejność usuwania (liczba elementów: 12):
        // WYNIK: • src/util/notes.txt
        // WYNIK: • src/util/Helper.java
        // WYNIK: • src/util
        // WYNIK: • src/Main.java
        // WYNIK: • src
        // WYNIK: • empty
        // WYNIK: • docs/readme.txt
        // WYNIK: • docs/img/logo.bin
        // WYNIK: • docs/img
        // WYNIK: • docs
        // WYNIK: • build.txt
        // WYNIK: • .
        for (Path p : order) {
            Files.delete(p);
        }
        show("po usunięciu: kopia istnieje", Files.exists(copy));
        // WYNIK: po usunięciu: kopia istnieje → false

        // Usuwanie dobrze (2): walkFileTree z postVisitDirectory (rodzic po dzieciach z założenia).
        copyTree(source, copy);
        deleteTree(copy);
        show("po deleteTree: kopia istnieje", Files.exists(copy));
        // WYNIK: po deleteTree: kopia istnieje → false

        // Dowiązania symboliczne: domyślnie obejście ich NIE śledzi (dowiązanie do katalogu jest widziane jako samo
        // dowiązanie). Opcja FileVisitOption.FOLLOW_LINKS każe wejść do celu — wtedy dowiązanie wskazujące „w górę”
        // tworzy pętlę, a Java zgłasza FileSystemLoopException (do visitFileFailed). W Windows tworzenie dowiązań
        // wymaga uprawnień, dlatego nie pokazujemy ich w tej lekcji.

        // PUŁAPKA: deleteTree na złym katalogu (np. pustej zmiennej, korzeniu projektu) kasuje bezpowrotnie — nie ma
        //   kosza. Sprawdzaj ścieżkę przed usunięciem (czy leży w oczekiwanym katalogu).
        // PUŁAPKA: usuwanie w trakcie obchodzenia (forEach(delete) na strumieniu walk) jest złym pomysłem: strumień jest
        //   leniwy i czyta katalogi w trakcie usuwania. Najpierw zbierz listę (toList), potem usuwaj.
        // DOBRA PRAKTYKA: kopiowanie i usuwanie drzew trzymaj w jednej małej metodzie z testem. Dlaczego: to miejsca,
        //   w których jedna pomyłka kosztuje całe dane.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • list = jeden poziom; walk = całe drzewo (start pierwszy); find = walk + atrybuty; newDirectoryStream = glob.
     *   • Strumienie list/walk/find i DirectoryStream ZAMYKAJ (try-with-resources); kolejność wpisów jest dowolna → sortuj.
     *   • walk(dir, maxDepth): 0 = tylko start; Files.walk rzuca błędy jako UncheckedIOException.
     *   • glob: * (w jednej nazwie), ** (przez katalogi), ?, [a-c], {a,b}; wzorzec zaczynający się od "**" i ukośnika nie łapie pliku w korzeniu.
     *   • PathMatcher: "glob:..." (preferuj) albo "regex:..." (z separatorami systemu); matches(ścieżka względna).
     *   • walkFileTree: preVisitDirectory / visitFile / postVisitDirectory / visitFileFailed → CONTINUE, SKIP_SUBTREE,
     *     SKIP_SIBLINGS, TERMINATE.
     *   • Usuwanie drzewa: dzieci przed rodzicem (sorted(reverseOrder) po zebraniu do listy albo postVisitDirectory).
     *   • Files.copy katalogu kopiuje tylko sam pusty katalog — drzewo kopiujemy walkFileTree.
     *   • Dowiązania: domyślnie nieśledzone; FOLLOW_LINKS grozi pętlą (FileSystemLoopException).
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego strumień z Files.list trzeba zamknąć i co się dzieje w Windows, gdy tego nie zrobisz?
     *   2. Dlaczego przed wypisaniem wyniku Files.walk sortujemy?
     *   3. Czy plik Main.java leżący w korzeniu pasuje do wzorca glob zapisanego jako: dwie gwiazdki, ukośnik, gwiazdka,
     *      ".java"? Odpowiedz: pasuje czy nie, i dlaczego.
     *   4. Co wypisze:  Files.walk(drzewo, 0).count()  ?
     *   5. ZNAJDŹ BŁĄD:  Files.walk(katalog).forEach(p -> Files.delete(p));  — dwa problemy (kompilator i kolejność).
     *   6. ZNAJDŹ BŁĄD:  Files.list(katalog).map(Path::toString).forEach(System.out::println);  — czego brakuje?
     *   7. Dlaczego sorted(Comparator.reverseOrder()) usuwa dzieci przed rodzicami?
     *   8. Po co Files.find, skoro jest walk + filter?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        runExercises(false);

        section("ĆWICZENIA — rozwiązania wzorcowe");
        runExercises(true);
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /** Każde uruchomienie dostaje własny, świeży katalog; reference = czy sprawdzamy rozwiązania wzorcowe. */
    private static void runExercises(boolean reference) {
        Path dir = TempDir.create("io07cw");
        try {
            Path tree = dir.resolve("t");
            Check.equal("ćw. 1: ile plików .txt", 3L, io(() -> {
                buildTree(tree);
                return reference ? solution1(tree) : exercise1(tree);
            }));

            Check.equal("ćw. 2: największy plik", "docs/img/logo.bin|100", io(() -> {
                return reference ? solution2(tree) : exercise2(tree);
            }));

            Check.equal("ćw. 3: PRZEPISZ liczenie plików", 6L, io(() -> {
                return reference ? solution3(tree) : exercise3(tree);
            }));

            Path tree4 = dir.resolve("t4");
            Check.equal("ćw. 4: usuń puste katalogi", "3|false|false|true", io(() -> {
                buildTree(tree4);
                Files.createDirectories(tree4.resolve("x/y")); // łańcuch pustych katalogów: x/y
                int removed = reference ? solution4(tree4) : exercise4(tree4);
                return removed + "|" + Files.exists(tree4.resolve("empty")) + "|" + Files.exists(tree4.resolve("x"))
                        + "|" + Files.exists(tree4.resolve("src"));
            }));
        } finally {
            TempDir.deleteRecursively(dir);
        }
        Check.summary();
    }

    /**
     * ĆWICZENIE 1 (łatwe): policz zwykłe pliki z rozszerzeniem .txt w całym drzewie (rekurencyjnie).
     * Podpowiedź: try-with-resources, Files.find albo Files.walk + filter(isRegularFile), endsWith(".txt"), count().
     */
    static long exercise1(Path root) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): znajdź największy zwykły plik w drzewie i zwróć "ścieżka-względna|rozmiar",
     * np. "docs/img/logo.bin|100" (ścieżka względem root, z "/" — użyj metody rel).
     * Podpowiedź: Files.walk, filter(isRegularFile), max(Comparator.comparingLong(...)), Files.size (opakuj IOException).
     */
    static String exercise2(Path root) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ stary rekurencyjny kod na NIO.2. Stary kod:
     * <pre>{@code
     * static long count(File dir) {
     *     File[] children = dir.listFiles();   // null, gdy to nie katalog lub błąd
     *     if (children == null) { return 0; }
     *     long n = 0;
     *     for (File f : children) { n += f.isDirectory() ? count(f) : 1; }
     *     return n;
     * }
     * }</pre>
     * Nowa wersja ma policzyć wszystkie zwykłe pliki w drzewie przez Files.walk (i zamknąć strumień).
     * Podpowiedź: {@code try (Stream<Path> s = Files.walk(root)) { return s.filter(Files::isRegularFile).count(); }}
     */
    static long exercise3(Path root) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): usuń WSZYSTKIE puste katalogi pod root (także te, które staną się puste po usunięciu
     * pustych dzieci), ale nie sam root. Zwróć liczbę usuniętych katalogów.
     * Podpowiedź: walkFileTree + postVisitDirectory (dzieci są już przejrzane); katalog jest pusty, gdy
     * Files.list(dir) nie ma żadnego elementu (zamknij strumień!).
     */
    static int exercise4(Path root) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static long solution1(Path root) throws IOException {
        try (Stream<Path> stream = Files.find(root, Integer.MAX_VALUE,
                (p, attrs) -> attrs.isRegularFile() && p.getFileName().toString().endsWith(".txt"))) {
            return stream.count();
        }
    }

    static String solution2(Path root) throws IOException {
        try (Stream<Path> stream = Files.find(root, Integer.MAX_VALUE, (p, attrs) -> attrs.isRegularFile())) {
            List<Path> files = stream.toList();
            Path biggest = null;
            long max = -1;
            for (Path f : files) {
                long size = Files.size(f);
                if (size > max) {
                    max = size;
                    biggest = f;
                }
            }
            return rel(root, biggest) + "|" + max;
        }
    }

    static long solution3(Path root) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile).count();
        }
    }

    static int solution4(Path root) throws IOException {
        int[] removed = {0};
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                boolean empty;
                try (Stream<Path> children = Files.list(dir)) {
                    empty = children.findAny().isEmpty(); // isEmpty = Optional.isEmpty (Java 11+)
                }
                if (empty && !dir.equals(root)) {
                    Files.delete(dir);
                    removed[0]++;
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return removed[0];
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Strumień trzyma otwarty uchwyt do katalogu. Zamknięcie zwalnia go od razu. W Windows otwarty katalog nie
     *      pozwala się usunąć ani zmienić jego nazwy, a w Linuksie przy wielu wywołaniach kończą się uchwyty.
     *   2. Bo kolejność wpisów zależy od systemu plików i nie jest gwarantowana — bez sortowania test lub wydruk
     *      raz przejdzie, raz nie.
     *   3. Nie pasuje (false): "**" + "/" wymaga co najmniej jednego katalogu w ścieżce, a Main.java leży w korzeniu.
     *   4. 1 — głębokość 0 oznacza tylko sam katalog startowy.
     *   5. Files.delete rzuca IOException, której lambda w forEach nie może rzucić (błąd kompilacji). Poza tym walk
     *      zwraca rodzica PRZED dziećmi, więc delete trafiłby na niepusty katalog (DirectoryNotEmptyException).
     *   6. Zamknięcia strumienia (try-with-resources). Bez niego uchwyt do katalogu zostaje otwarty.
     *   7. Path sortuje się tak, że rodzic jest przed swoimi dziećmi ("src" przed "src/Main.java"); po odwróceniu
     *      kolejności dzieci pojawiają się przed rodzicem, więc każdy katalog jest pusty w chwili usuwania.
     *   8. find dostaje atrybuty pliku (rozmiar, typ, czas), które system zwrócił już przy obchodzeniu — nie trzeba
     *      pytać o nie dysku drugi raz dla każdego pliku.
     */
    // </editor-fold>
}
