package t18_io_files;

import helpers.Check;
import helpers.TempDir;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Path i Files — ścieżki, pliki i katalogi w NIO.2
 *        (path = ścieżka; file = plik; directory = katalog; NIO.2 = „New I/O 2”, nowe wejście-wyjście z Javy 7)
 *
 * W SKRÓCIE:
 *   Path to NAZWA miejsca w systemie plików (np. "dane/raport.txt"), a Files to zestaw metod, które na tym
 *   miejscu coś robią: tworzą, kopiują, przenoszą, usuwają, pytają o rozmiar. Zastępują stare java.io.File.
 *   Gdy coś pójdzie źle, Files rzuca wyjątek z powodem, a nie samo „false”.
 *
 * ANALOGIA: Path to adres na kopercie, Files to poczta.
 *   Adres możesz napisać na kopercie dla domu, którego nie ma — nic się nie dzieje, to tylko tekst.
 *   Dopiero poczta (Files) próbuje coś dostarczyć i gdy domu nie ma, odpowiada: „adresat nie istnieje”.
 *   Sklejanie i skracanie adresów (resolve, relativize, normalize) to sama praca na napisie — poczta nie jedzie.
 *
 * JAK TO DZIAŁA:
 *   Path.of("a", "b", "c.txt")      →  ścieżka złożona z elementów: a / b / c.txt
 *   Path jest NIEZMIENNY            →  każda metoda (resolve, normalize...) zwraca NOWĄ ścieżkę
 *   Path niczego nie sprawdza       →  istnienie pliku sprawdza dopiero Files.exists(...)
 *
 *   Stare java.io.File             Nowe NIO.2 (Path + Files)
 *   ------------------------------ ---------------------------------------------
 *   delete() → false (czemu?)      Files.delete(p) → wyjątek z powodem
 *   mkdir() → false                Files.createDirectory(p) → wyjątek z powodem
 *   listFiles() → null             Files.list(p) → strumień albo wyjątek
 *   brak dowiązań symbolicznych    obsługuje dowiązania symboliczne i atrybuty plików
 *
 *   Wyjątek IOException jest SPRAWDZANY (checked): dysk, sieć i uprawnienia są poza kontrolą programu,
 *   więc kompilator zmusza do obsługi (try/catch) albo do deklaracji "throws IOException".
 *   Konkretne przypadki mają podklasy: NoSuchFileException, FileAlreadyExistsException,
 *   DirectoryNotEmptyException (wszystkie to FileSystemException).
 *
 * SŁÓWKA:
 *   resolve = dołącz; resolveSibling = dołącz obok; relativize = zrób ścieżkę względną; normalize = uprość;
 *   parent = rodzic (katalog nadrzędny); root = korzeń; absolute = bezwzględna; relative = względna;
 *   copy = kopiuj; move = przenieś; delete = usuń; temp = tymczasowy; size = rozmiar; exists = istnieje.
 *
 * ZOBACZ TEŻ: t18_io_files/Io02ReadingText (czytanie zawartości), t10_exceptions/Exceptions06ChainingWrapping
 *   (opakowywanie wyjątków), t18_io_files/Io11IoExceptions (hierarchia wyjątków IO)
 * </pre>
 */
public class Io01PathFiles {

    public static void main(String[] args) throws IOException {
        // throws IOException = „rzuca IOException” — main może zwyczajnie przekazać wyjątek wyżej
        title("Io01 — Path i Files: ścieżki, pliki i katalogi");

        // Wszystkie pliki lekcji powstają w katalogu tymczasowym i znikają w finally (blok finally = zawsze się wykona).
        Path dir = TempDir.create("io01"); // create = utwórz
        try {
            pathIsJustAName();                       // Path is just a name = Path to tylko nazwa
            pathParts();                             // path parts = części ścieżki
            resolveRelativizeNormalize();            // resolve = dołącz, relativize = zrób względną, normalize = uprość
            oldFileVersusFiles(dir.resolve("s4"));   // old File versus Files = stary File kontra Files
            creatingAndChecking(dir.resolve("s5"));  // creating and checking = tworzenie i sprawdzanie
            copyMoveDelete(dir.resolve("s6"));       // copy, move, delete = kopiuj, przenieś, usuń
            sizeAndTime(dir.resolve("s7"));          // size and time = rozmiar i czas
            tempFiles(dir.resolve("s8"));            // temp files = pliki tymczasowe
            exercises();                             // exercises = ćwiczenia
        } finally {
            TempDir.deleteRecursively(dir);
        }
    }

    // =================================================================================================
    // Pomocnicze: wspólne dla wszystkich lekcji o plikach
    // =================================================================================================

    /**
     * rel = ścieżka względem katalogu bazowego, zawsze z ukośnikiem "/".
     * Po co? Katalog tymczasowy ma losową nazwę (innego nie wypisujemy), a Windows pisze ścieżki z "\"
     * (np. "a\b.txt"), Linux z "/" (np. "a/b.txt"). Zamiana na "/" sprawia, że wydruk jest taki sam wszędzie.
     */
    private static String rel(Path base, Path p) {
        return base.relativize(p).toString().replace('\\', '/');
    }

    /** slash = ukośnik. To samo dla ścieżki, która już jest względna (nie ma czego odejmować). */
    private static String slash(Path p) {
        return p.toString().replace('\\', '/');
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

    // =================================================================================================
    // 1. PATH TO TYLKO NAZWA
    // =================================================================================================

    /**
     * 1. Path.of(...) składa ścieżkę z elementów. Nie dotyka dysku: ścieżka może wskazywać coś, czego nie ma.
     */
    static void pathIsJustAName() {
        section("1. Path to tylko nazwa");

        Path p1 = Path.of("projekt", "src", "Main.java"); // Path.of = ścieżka z... (Java 11+)
        Path p2 = Path.of("projekt/src/Main.java");       // jeden napis; "/" działa też w Windows
        Path p3 = Paths.get("projekt", "src", "Main.java"); // Paths.get = starszy zapis tego samego (Java 7+)

        show("Path.of(trzy części)", slash(p1));
        // WYNIK: Path.of(trzy części) → projekt/src/Main.java
        show("p1.equals(p2)", p1.equals(p2));
        // WYNIK: p1.equals(p2) → true
        show("p1.equals(p3)", p1.equals(p3));
        // WYNIK: p1.equals(p3) → true

        // DOBRA PRAKTYKA: używaj Path.of (Java 11+): robi to samo co Paths.get, ale jest krótsze i bez dodatkowej klasy.
        // DOBRA PRAKTYKA: składaj ścieżki z elementów (Path.of("a", "b")) albo przez resolve, nie sklejaj napisów
        //   z "\\" lub "/": separator zależy od systemu (Windows "\", Linux i macOS "/"), a Path zna go sam.

        Path notExisting = Path.of("tego-pliku-na-pewno-nie-ma.txt");
        show("exists (Files.exists)", Files.exists(notExisting));
        // WYNIK: exists (Files.exists) → false
        note("Path utworzył się bez błędu, mimo że pliku nie ma: to była tylko nazwa.");
        // WYNIK: ℹ Path utworzył się bez błędu, mimo że pliku nie ma: to była tylko nazwa.

        // Przejście między starym a nowym światem:
        File oldStyle = p1.toFile();          // toFile = na File (stary typ)
        Path backToNew = oldStyle.toPath();   // toPath = na Path (nowy typ)
        show("File.getName()", oldStyle.getName());
        // WYNIK: File.getName() → Main.java
        show("toPath() daje taką samą ścieżkę", backToNew.equals(p1));
        // WYNIK: toPath() daje taką samą ścieżkę → true

        // toAbsolutePath() = ścieżka bezwzględna: dokleja katalog bieżący, w którym uruchomiono program.
        // toRealPath() = ścieżka „prawdziwa”: bezwzględna, uproszczona, z rozwiązanymi dowiązaniami
        // symbolicznymi; rzuca NoSuchFileException, gdy pliku nie ma. Ich wynik zależy od komputera
        // (np. C:\Users\...\projekt albo /home/.../projekt), dlatego tu go nie wypisujemy.

        // PUŁAPKA: wielkość liter. Windows traktuje "Raport.TXT" i "raport.txt" jako ten sam plik, Linux jako dwa
        //   różne. Program, który działa u Ciebie, może zawieść na serwerze. Nie twórz plików różniących się
        //   tylko wielkością liter i zawsze pisz nazwy dokładnie tak samo.

        // PUŁAPKA: wypisanie Path "na surowo" (System.out.println(p1)) daje "projekt\src\Main.java" w Windows
        //   i "projekt/src/Main.java" na Linuksie. Do porównań w testach i do wydruków w lekcji zamieniamy
        //   "\" na "/" (metoda slash/rel w tym pliku).
    }

    // =================================================================================================
    // 2. CZĘŚCI ŚCIEŻKI
    // =================================================================================================

    /**
     * 2. Ścieżka to lista elementów (nazw). Można ją rozbierać: nazwa pliku, rodzic, kolejne elementy.
     * Wszystko to operacje na napisie — dysk nie jest potrzebny.
     */
    static void pathParts() {
        section("2. Części ścieżki");

        Path p = Path.of("projekt/src/Main.java");

        show("getFileName", p.getFileName()); // getFileName = pobierz nazwę pliku (ostatni element)
        // WYNIK: getFileName → Main.java
        show("getParent", slash(p.getParent())); // getParent = pobierz rodzica (wszystko bez ostatniego elementu)
        // WYNIK: getParent → projekt/src
        show("getNameCount", p.getNameCount()); // getNameCount = liczba elementów
        // WYNIK: getNameCount → 3
        show("getName(0)", p.getName(0)); // getName = element o numerze (od zera)
        // WYNIK: getName(0) → projekt
        show("subpath(1, 3)", slash(p.subpath(1, 3))); // subpath = fragment: od elementu 1 do 3 (bez trzeciego)
        // WYNIK: subpath(1, 3) → src/Main.java

        List<String> elements = new ArrayList<>();
        for (Path part : p) { // Path jest Iterable<Path>: pętla for-each przejdzie po elementach
            elements.add(part.toString());
        }
        show("elementy", elements);
        // WYNIK: elementy → [projekt, src, Main.java]

        show("getRoot (ścieżka względna)", String.valueOf(p.getRoot())); // getRoot = korzeń ("/" albo "C:\")
        // WYNIK: getRoot (ścieżka względna) → null
        show("getParent samej nazwy", String.valueOf(Path.of("plik.txt").getParent()));
        // WYNIK: getParent samej nazwy → null

        // PUŁAPKA: getParent() i getRoot() potrafią zwrócić null (brak rodzica, brak korzenia).
        //   Wołanie na wyniku kolejnej metody (Path.of("plik.txt").getParent().resolve("x")) skończy się
        //   NullPointerException. Sprawdź null albo użyj Optional.ofNullable (t14_optional/Optional01Basics).

        // startsWith / endsWith porównują ELEMENTY ścieżki, nie litery:
        show("startsWith(\"projekt\")", p.startsWith("projekt")); // startsWith = zaczyna się od
        // WYNIK: startsWith("projekt") → true
        show("startsWith(\"proj\")", p.startsWith("proj"));
        // WYNIK: startsWith("proj") → false
        show("Path.of(\"abc\").startsWith(\"ab\")", Path.of("abc").startsWith("ab"));
        // WYNIK: Path.of("abc").startsWith("ab") → false
        show("\"abc\".startsWith(\"ab\") jako napis", "abc".startsWith("ab"));
        // WYNIK: "abc".startsWith("ab") jako napis → true
        show("endsWith(\"Main.java\")", p.endsWith("Main.java")); // endsWith = kończy się na
        // WYNIK: endsWith("Main.java") → true
        show("endsWith(\"java\")", p.endsWith("java"));
        // WYNIK: endsWith("java") → false
        // Dlaczego tak? "ab" to nie pełny element ścieżki "abc". Dzięki temu "/dane/abc" nie jest podkatalogiem "/dane/ab".

        // Rozszerzenia JDK nie zna: nie ma Path.getExtension(). Wyciągamy je z nazwy pliku.
        String name = Path.of("raport.koncowy.txt").getFileName().toString();
        int dot = name.lastIndexOf('.'); // lastIndexOf = ostatni indeks znaku
        show("rozszerzenie", name.substring(dot + 1)); // substring = podnapis
        // WYNIK: rozszerzenie → txt
        show("nazwa bez rozszerzenia", name.substring(0, dot));
        // WYNIK: nazwa bez rozszerzenia → raport.koncowy
        // DOBRA PRAKTYKA: szukaj OSTATNIEJ kropki (lastIndexOf), bo nazwy jak "raport.koncowy.txt" mają ich kilka;
        //   sprawdź też dot >= 0 — plik "Makefile" nie ma kropki wcale (lastIndexOf zwróci -1).
    }

    // =================================================================================================
    // 3. RESOLVE, RELATIVIZE, NORMALIZE
    // =================================================================================================

    /**
     * 3. Trzy operacje na ścieżkach: dołączanie (resolve), liczenie drogi między dwiema ścieżkami (relativize)
     * i upraszczanie kropek (normalize). Każda zwraca NOWĄ ścieżkę, bo Path jest niezmienny.
     */
    static void resolveRelativizeNormalize() {
        section("3. resolve, resolveSibling, relativize, normalize");

        Path base = Path.of("projekt/src");

        Path file = base.resolve("Main.java"); // resolve = dołącz (jak cd + nazwa)
        show("resolve", slash(file));
        // WYNIK: resolve → projekt/src/Main.java
        show("base po resolve (niezmieniony)", slash(base));
        // WYNIK: base po resolve (niezmieniony) → projekt/src

        Path sibling = file.resolveSibling("Test.java"); // resolveSibling = dołącz obok (podmień ostatni element)
        show("resolveSibling", slash(sibling));
        // WYNIK: resolveSibling → projekt/src/Test.java

        // Uwaga: gdy argument resolve jest ścieżką BEZWZGLĘDNĄ, wynikiem jest właśnie ten argument
        // (Path.of("a").resolve("/b") daje "/b"). Dlatego nie dołączaj nazwy podanej przez użytkownika bez
        // sprawdzenia (zobacz „ZIP SLIP” w t18_io_files/Io13ZipArchives).

        Path from = Path.of("a/b");
        Path to = Path.of("a/b/c/dane.txt");
        show("relativize w dół", slash(from.relativize(to))); // relativize = droga z `from` do `to`
        // WYNIK: relativize w dół → c/dane.txt
        show("relativize w bok", slash(Path.of("a/b/c").relativize(Path.of("a/x"))));
        // WYNIK: relativize w bok → ../../x
        // ".." znaczy „katalog wyżej”: z a/b/c wychodzimy dwa razy (do a), potem wchodzimy do x.

        Path messy = Path.of("a/./b/../c"); // "." = ten katalog, ".." = katalog wyżej
        show("normalize", slash(messy.normalize())); // normalize = uprość (usuń "." i "x/..")
        // WYNIK: normalize → a/c
        show("normalize zostawia '..' na początku", slash(Path.of("../x/./y").normalize()));
        // WYNIK: normalize zostawia '..' na początku → ../x/y
        show("a/./b equals a/b", Path.of("a/./b").equals(Path.of("a/b")));
        // WYNIK: a/./b equals a/b → false
        show("po normalize", Path.of("a/./b").normalize().equals(Path.of("a/b")));
        // WYNIK: po normalize → true

        // PUŁAPKA: normalize() pracuje WYŁĄCZNIE na tekście. Nie zagląda na dysk, więc nie wie o dowiązaniach
        //   symbolicznych: jeśli "b" jest dowiązaniem do innego miejsca, "a/b/.." wcale nie musi być "a".
        //   Gdy potrzebujesz prawdy o dysku, użyj toRealPath() (rzuca wyjątek, gdy pliku nie ma).
        // Path z kropkami i bez nich to dwie RÓŻNE ścieżki dla equals (patrz wyżej) — porównuj po normalize().

        // relativize wymaga ścieżek tego samego rodzaju: obie względne albo obie bezwzględne.
        Path absolute = Path.of("x").toAbsolutePath(); // toAbsolutePath = zrób bezwzględną (wynik zależy od komputera)
        try {
            Path.of("x").relativize(absolute);
        } catch (IllegalArgumentException e) {
            System.out.println("✔ relativize(względna, bezwzględna) → rzucono " + e.getClass().getSimpleName());
            // WYNIK: ✔ relativize(względna, bezwzględna) → rzucono IllegalArgumentException
        }
    }

    // =================================================================================================
    // 4. STARY File KONTRA NOWY Files
    // =================================================================================================

    /**
     * 4. PRZED/PO. Stare metody java.io.File oddają samo „false” albo null. Nowe Files.* rzucają wyjątek
     * z konkretnym powodem. Dlatego w nowym kodzie używamy Files.
     */
    static void oldFileVersusFiles(Path base) throws IOException {
        section("4. Stary File kontra Files");
        Files.createDirectory(base); // createDirectory = utwórz katalog (jeden poziom)

        // PRZED (java.io.File): niepowodzenie to tylko false — nie wiadomo dlaczego.
        File ghost = base.resolve("duch.txt").toFile();
        show("File.delete() na nieistniejącym", ghost.delete());
        // WYNIK: File.delete() na nieistniejącym → false
        show("File.mkdir() bez katalogu nadrzędnego", new File(base.toFile(), "x/y").mkdir());
        // WYNIK: File.mkdir() bez katalogu nadrzędnego → false
        show("File.listFiles() nieistniejącego == null", new File(base.toFile(), "brak").listFiles() == null);
        // WYNIK: File.listFiles() nieistniejącego == null → true
        note("false i null nie mówią DLACZEGO: brak pliku? brak uprawnień? plik zajęty?");
        // WYNIK: ℹ false i null nie mówią DLACZEGO: brak pliku? brak uprawnień? plik zajęty?

        // PO (NIO.2): wyjątek mówi, co poszło nie tak.
        ioFails("Files.delete na nieistniejącym", () -> Files.delete(base.resolve("duch.txt")));
        // WYNIK: ✔ Files.delete na nieistniejącym → rzucono NoSuchFileException (plik: duch.txt)
        ioFails("Files.createDirectory bez rodzica", () -> Files.createDirectory(base.resolve("x/y")));
        // WYNIK: ✔ Files.createDirectory bez rodzica → rzucono NoSuchFileException (plik: y)
        ioFails("Files.list nieistniejącego", () -> Files.list(base.resolve("brak")).close());
        // WYNIK: ✔ Files.list nieistniejącego → rzucono NoSuchFileException (plik: brak)

        // Więcej zalet NIO.2: dowiązania symboliczne (LinkOption.NOFOLLOW_LINKS), atrybuty plików (Files.readAttributes),
        // atomowe przenoszenie (ATOMIC_MOVE), przeglądanie drzew (Files.walk) i „wirtualne” systemy plików (wnętrze ZIP).

        // DOBRA PRAKTYKA: w nowym kodzie pisz Path/Files. File zostaw tam, gdzie wymaga go cudze, stare API
        //   (wtedy konwertuj: path.toFile() / file.toPath()). Dlaczego: tylko NIO.2 mówi, co poszło nie tak.

        // Dlaczego IOException jest SPRAWDZANY (checked)? Operacje na plikach zawodzą z powodów, na które program
        // nie ma wpływu (brak dysku, brak uprawnień, ktoś usunął plik). Kompilator zmusza więc do podjęcia
        // decyzji: obsłużyć (try/catch) albo przekazać dalej (throws). Szczegóły: t18_io_files/Io11IoExceptions.
    }

    // =================================================================================================
    // 5. TWORZENIE I SPRAWDZANIE
    // =================================================================================================

    /**
     * 5. createDirectory tworzy jeden poziom, createDirectories całą drogę (jak mkdir -p). createFile tworzy
     * pusty plik i nie nadpisuje istniejącego. exists/isDirectory/isRegularFile odpowiadają na pytania.
     */
    static void creatingAndChecking(Path base) throws IOException {
        section("5. Tworzenie i sprawdzanie");
        Files.createDirectory(base);

        ioFails("createDirectory drugi raz", () -> Files.createDirectory(base));
        // WYNIK: ✔ createDirectory drugi raz → rzucono FileAlreadyExistsException (plik: s5)

        Path deep = base.resolve("a/b/c");
        Files.createDirectories(deep); // createDirectories = utwórz katalogi (całą drogę)
        show("createDirectories — jest katalog", Files.isDirectory(deep)); // isDirectory = czy to katalog
        // WYNIK: createDirectories — jest katalog → true
        Files.createDirectories(deep); // drugi raz: bez błędu (katalog już jest)
        note("createDirectories na istniejącym katalogu nic nie robi i nie rzuca wyjątku.");
        // WYNIK: ℹ createDirectories na istniejącym katalogu nic nie robi i nie rzuca wyjątku.

        Path file = deep.resolve("raport.txt");
        Files.createFile(file); // createFile = utwórz plik (pusty)
        show("istnieje", Files.exists(file));
        // WYNIK: istnieje → true
        show("isRegularFile", Files.isRegularFile(file)); // isRegularFile = czy to zwykły plik
        // WYNIK: isRegularFile → true
        show("isDirectory(plik)", Files.isDirectory(file));
        // WYNIK: isDirectory(plik) → false
        show("rozmiar pustego pliku", Files.size(file));
        // WYNIK: rozmiar pustego pliku → 0
        ioFails("createFile istniejącego", () -> Files.createFile(file));
        // WYNIK: ✔ createFile istniejącego → rzucono FileAlreadyExistsException (plik: raport.txt)
        show("rel(base, file)", rel(base, file));
        // WYNIK: rel(base, file) → a/b/c/raport.txt

        // createFile jest „atomowe”: sprawdzenie „czy już jest” i utworzenie to jedna operacja systemu.
        // „Najpierw if (!exists), potem createFile” ma lukę czasową (ktoś może wejść pomiędzy) — po prostu spróbuj
        // i obsłuż FileAlreadyExistsException (zobacz Io11IoExceptions, TOCTOU).

        // exists i notExists NIE są swoimi zaprzeczeniami!
        //   exists(p)    = true  → na pewno jest
        //   notExists(p) = true  → na pewno nie ma
        //   Gdy system nie potrafi tego ustalić (np. brak uprawnień do katalogu nadrzędnego), OBA zwracają false:
        //   „nie wiem”. Dlatego !exists(p) nie znaczy „na pewno nie ma”.
        Path nothing = base.resolve("nie-ma.txt");
        show("exists(brak)", Files.exists(nothing));
        // WYNIK: exists(brak) → false
        show("notExists(brak)", Files.notExists(nothing));
        // WYNIK: notExists(brak) → true
        show("isDirectory(brak)", Files.isDirectory(nothing));
        // WYNIK: isDirectory(brak) → false
        // Metody pytające (exists, isDirectory, isRegularFile) nie rzucają IOException — przy kłopotach dają false.

        // DOBRA PRAKTYKA: createDirectories przy przygotowaniu katalogu na wyniki (jest idempotentne = wielokrotne
        //   wywołanie daje ten sam skutek). createDirectory tam, gdzie istnienie katalogu to błąd.
    }

    // =================================================================================================
    // 6. KOPIOWANIE, PRZENOSZENIE, USUWANIE
    // =================================================================================================

    /**
     * 6. copy i move domyślnie NIE nadpisują istniejącego celu — rzucają FileAlreadyExistsException.
     * Nadpisanie wymaga jawnej opcji REPLACE_EXISTING. delete rzuca wyjątek, deleteIfExists zwraca boolean.
     */
    static void copyMoveDelete(Path base) throws IOException {
        section("6. copy, move, delete");
        Files.createDirectory(base);

        Path source = base.resolve("zrodlo.txt");
        Files.writeString(source, "zażółć gęślą jaźń\n"); // writeString = zapisz napis (Java 11+), domyślnie UTF-8
        Path copy = base.resolve("kopia.txt");

        Files.copy(source, copy); // copy = kopiuj
        show("kopia ma tę samą treść", Files.readString(copy).equals(Files.readString(source))); // readString = wczytaj napis (Java 11+)
        // WYNIK: kopia ma tę samą treść → true
        ioFails("copy na istniejący cel", () -> Files.copy(source, copy));
        // WYNIK: ✔ copy na istniejący cel → rzucono FileAlreadyExistsException (plik: kopia.txt)

        Files.writeString(source, "nowa treść\n");
        Files.copy(source, copy, StandardCopyOption.REPLACE_EXISTING); // REPLACE_EXISTING = zastąp istniejący
        show("po REPLACE_EXISTING", Files.readString(copy).strip()); // strip = obetnij białe znaki (Java 11+)
        // WYNIK: po REPLACE_EXISTING → nowa treść

        Path moved = base.resolve("przeniesiony.txt");
        Files.move(copy, moved); // move = przenieś (też: zmień nazwę)
        show("stary plik po move istnieje", Files.exists(copy));
        // WYNIK: stary plik po move istnieje → false
        show("nowy plik po move istnieje", Files.exists(moved));
        // WYNIK: nowy plik po move istnieje → true
        ioFails("move nieistniejącego", () -> Files.move(base.resolve("nie-ma.txt"), base.resolve("cel.txt")));
        // WYNIK: ✔ move nieistniejącego → rzucono NoSuchFileException (plik: nie-ma.txt)
        ioFails("move na istniejący cel", () -> Files.move(source, moved));
        // WYNIK: ✔ move na istniejący cel → rzucono FileAlreadyExistsException (plik: przeniesiony.txt)

        // StandardCopyOption.ATOMIC_MOVE = przenieś „atomowo”: albo cały plik jest już pod nowym adresem, albo wciąż
        // pod starym — nigdy „w połowie”. Przydaje się przy bezpiecznym zapisie (zobacz Io03WritingText).
        // Działa tylko w obrębie jednego dysku/partycji; w przeciwnym razie AtomicMoveNotSupportedException.

        // delete kontra deleteIfExists:
        ioFails("delete nieistniejącego", () -> Files.delete(base.resolve("nie-ma.txt")));
        // WYNIK: ✔ delete nieistniejącego → rzucono NoSuchFileException (plik: nie-ma.txt)
        show("deleteIfExists nieistniejącego", Files.deleteIfExists(base.resolve("nie-ma.txt"))); // deleteIfExists = usuń, jeśli istnieje
        // WYNIK: deleteIfExists nieistniejącego → false
        show("deleteIfExists istniejącego", Files.deleteIfExists(moved));
        // WYNIK: deleteIfExists istniejącego → true

        // Niepusty katalog nie da się usunąć jednym delete:
        Path full = base.resolve("pelny");
        Files.createDirectory(full);
        Files.writeString(full.resolve("w-srodku.txt"), "x");
        ioFails("delete niepustego katalogu", () -> Files.delete(full));
        // WYNIK: ✔ delete niepustego katalogu → rzucono DirectoryNotEmptyException (plik: pelny)

        // PUŁAPKA: Files.copy katalogu kopiuje tylko sam (pusty) katalog, bez zawartości.
        Path fullCopy = base.resolve("pelny-kopia");
        Files.copy(full, fullCopy);
        show("kopia katalogu — plik w środku jest", Files.exists(fullCopy.resolve("w-srodku.txt")));
        // WYNIK: kopia katalogu — plik w środku jest → false
        show("kopia katalogu — to katalog", Files.isDirectory(fullCopy));
        // WYNIK: kopia katalogu — to katalog → true

        // PUŁAPKA: w Windows często nie da się usunąć ani przenieść pliku otwartego przez inny program albo przez
        //   Twój niezamknięty strumień (np. FileInputStream), a katalogu z takim plikiem — tym bardziej. Linux na to pozwala. Dlatego ZAWSZE zamykamy strumienie (try-with-resources).
    }

    // =================================================================================================
    // 7. ROZMIAR I CZAS MODYFIKACJI
    // =================================================================================================

    /**
     * 7. Files.size daje rozmiar w BAJTACH (nie w znakach!). Czas ostatniej modyfikacji to FileTime — sami go
     * ustawiamy na stałą wartość, żeby wynik się nie zmieniał.
     */
    static void sizeAndTime(Path base) throws IOException {
        section("7. Rozmiar i czas modyfikacji");
        Files.createDirectory(base);

        Path file = base.resolve("tekst.txt");
        String text = "zażółć\n"; // 6 liter + znak nowej linii
        Files.writeString(file, text);
        show("liczba znaków", text.length()); // length = długość
        // WYNIK: liczba znaków → 7
        show("rozmiar w bajtach (Files.size)", Files.size(file)); // size = rozmiar
        // WYNIK: rozmiar w bajtach (Files.size) → 11
        // Dlaczego 11, a nie 7? Zapisujemy w UTF-8: litery ASCII (z, a) i "\n" zajmują po 1 bajcie, a każda polska
        // litera (ż, ó, ł, ć) po 2 bajty: 2 + 4 * 2 + 1 = 11. Więcej o kodowaniach: t18_io_files/Io12Charsets.
        // Rozmiar z Files.writeString jest przewidywalny, bo sami wpisaliśmy "\n". Pliki zapisane przez
        // Files.write(path, lines) albo println mają separator linii zależny od systemu (Windows 2 bajty "\r\n",
        // Linux 1 bajt "\n"), więc ich rozmiar różni się między systemami.

        // Czas modyfikacji: ustawiamy własny, żeby wynik był stały.
        FileTime fixed = FileTime.from(Instant.parse("2026-01-15T10:30:00Z")); // FileTime.from = czas pliku z chwili (Instant)
        Files.setLastModifiedTime(file, fixed); // setLastModifiedTime = ustaw czas ostatniej modyfikacji
        FileTime read = Files.getLastModifiedTime(file); // getLastModifiedTime = pobierz ten czas
        show("czas modyfikacji", read);
        // WYNIK: czas modyfikacji → 2026-01-15T10:30:00Z
        show("jako Instant", read.toInstant());
        // WYNIK: jako Instant → 2026-01-15T10:30:00Z

        // DOBRA PRAKTYKA: w testach nie porównuj prawdziwych czasów plików (zmieniają się przy każdym uruchomieniu);
        //   ustaw własny czas albo wstrzyknij Clock (t17_datetime/DateTime01LocalDateTime).
        // PUŁAPKA: Files.size na katalogu da wartość zależną od systemu (nie sumę plików w środku!) — zobacz Io07.
    }

    // =================================================================================================
    // 8. PLIKI I KATALOGI TYMCZASOWE
    // =================================================================================================

    /**
     * 8. createTempFile i createTempDirectory dają unikalną, losową nazwę — program nie nadpisze cudzego pliku.
     * Nazwa jest losowa, więc sprawdzamy tylko jej cechy (początek, koniec), nigdy całość.
     */
    static void tempFiles(Path base) throws IOException {
        section("8. Pliki i katalogi tymczasowe");
        Files.createDirectory(base);

        Path tmpFile = Files.createTempFile(base, "raport-", ".txt"); // createTempFile = utwórz plik tymczasowy
        String name = tmpFile.getFileName().toString();
        show("nazwa zaczyna się od raport-", name.startsWith("raport-"));
        // WYNIK: nazwa zaczyna się od raport- → true
        show("nazwa kończy się na .txt", name.endsWith(".txt"));
        // WYNIK: nazwa kończy się na .txt → true
        show("plik istnieje i jest pusty", Files.exists(tmpFile) && Files.size(tmpFile) == 0);
        // WYNIK: plik istnieje i jest pusty → true

        Path tmpDir = Files.createTempDirectory(base, "robocze-"); // createTempDirectory = utwórz katalog tymczasowy
        show("katalog tymczasowy istnieje", Files.isDirectory(tmpDir));
        // WYNIK: katalog tymczasowy istnieje → true

        // Wersje bez argumentu `base` (Files.createTempFile("x", ".tmp")) tworzą plik w katalogu tymczasowym
        // systemu (Windows: %TEMP%, Linux: /tmp). Ich NIE kasuje nikt — musisz to zrobić sam
        // (Files.delete albo, na koniec programu, file.toFile().deleteOnExit() = „usuń przy wyjściu”).

        // DOBRA PRAKTYKA: pliki tymczasowe usuwaj w finally (albo w try-with-resources z własnym AutoCloseable).
        //   Dlaczego: inaczej zostają po każdym uruchomieniu i zapychają dysk.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Path = nazwa (niezmienna, nic nie sprawdza); Files = operacje; Path.of (Java 11+) zamiast Paths.get.
     *   • Części: getFileName, getParent (może być null), getNameCount, getName(i), subpath, getRoot.
     *   • startsWith/endsWith liczą ELEMENTY: Path.of("abc").startsWith("ab") → false.
     *   • resolve (dołącz), resolveSibling (obok), relativize (droga między), normalize (usuń . i ..) — tylko tekst.
     *   • Files.createDirectory (1 poziom) / createDirectories (cała droga, bez błędu gdy jest) / createFile.
     *   • exists i notExists mogą oba dać false („nie wiem”).
     *   • copy i move NIE nadpisują (FileAlreadyExistsException) — dopiero REPLACE_EXISTING; ATOMIC_MOVE = atomowo.
     *   • delete rzuca (NoSuchFileException, DirectoryNotEmptyException), deleteIfExists zwraca boolean.
     *   • Files.size = bajty; czas pliku = FileTime (UTC); nazwy tymczasowe są losowe (createTempFile).
     *
     * PYTANIA KONTROLNE:
     *   1. Czy Path.of("dane/raport.txt") wymaga, żeby plik istniał? Co mówi Files.exists?
     *   2. Czym różni się createDirectory od createDirectories? Który rzuci wyjątek, gdy katalog już jest?
     *   3. Co wypisze:  System.out.println(Path.of("abc").startsWith("ab"));  ?
     *   4. Co wypisze:  System.out.println(Path.of("a/./b/../c").normalize());  (na Linuksie)?
     *   5. ZNAJDŹ BŁĄD:  Files.copy(zrodlo, cel);  — chcemy za każdym razem mieć w "cel" aktualną kopię.
     *   6. ZNAJDŹ BŁĄD:  if (!Files.exists(p)) { ... }  — czy w środku wolno założyć, że pliku na pewno nie ma?
     *   7. Dlaczego Files.delete(katalog) potrafi rzucić DirectoryNotEmptyException i jak sobie z tym poradzić?
     *   8. Dlaczego IOException jest wyjątkiem sprawdzanym?
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
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /** Każde uruchomienie dostaje własny, świeży katalog; reference = czy sprawdzamy rozwiązania wzorcowe. */
    private static void runExercises(boolean reference) {
        Path dir = TempDir.create("io01cw");
        try {
            Check.equal("ćw. 1: rozszerzenie pliku", "txt|",
                    () -> (reference ? solution1(Path.of("raport.final.txt")) : exercise1(Path.of("raport.final.txt")))
                            + "|" + (reference ? solution1(Path.of("bez-rozszerzenia")) : exercise1(Path.of("bez-rozszerzenia"))));

            Check.equal("ćw. 2: zagnieżdżone katalogi i plik", "a/b/c/plik.txt|true", io(() -> {
                Path created = reference ? solution2(dir) : exercise2(dir);
                return rel(dir, created) + "|" + Files.isRegularFile(created);
            }));

            Path src = dir.resolve("nowy.txt");
            Path dst = dir.resolve("stary-cel-dluzszy.txt");
            Check.equal("ćw. 3: kopia z nadpisaniem", "nowa treść\n", io(() -> {
                Files.writeString(src, "nowa treść\n");
                Files.writeString(dst, "stara, dłuższa treść pliku\n");
                if (reference) {
                    solution3(src, dst);
                } else {
                    exercise3(src, dst);
                }
                return Files.readString(dst);
            }));

            Path old = dir.resolve("stary.txt");
            Check.equal("ćw. 4: usuń, jeśli istnieje", "usunięto|nie było", io(() -> {
                Files.writeString(old, "x");
                String first = reference ? solution4(old) : exercise4(old);
                String second = reference ? solution4(old) : exercise4(old);
                return first + "|" + second;
            }));

            Path data = dir.resolve("dane.txt");
            Check.equal("ćw. 5: kopia zapasowa i nowa zawartość", "stare|nowe", io(() -> {
                Files.writeString(data, "stare");
                Files.writeString(dir.resolve("dane.txt.bak"), "bardzo, bardzo stare");
                return reference ? solution5(data) : exercise5(data);
            }));
        } finally {
            TempDir.deleteRecursively(dir);
        }
        Check.summary();
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć rozszerzenie pliku (bez kropki) z jego nazwy, np. "raport.final.txt" → "txt".
     * Dla nazwy bez kropki zwróć pusty napis "".
     * Podpowiedź: getFileName().toString() i lastIndexOf('.').
     */
    static String exercise1(Path file) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 2 (łatwe): w katalogu dir utwórz katalogi a/b/c oraz w nich pusty plik plik.txt.
     * Zwróć ścieżkę utworzonego pliku.
     * Podpowiedź: createDirectories dla całej drogi, potem createFile.
     */
    static Path exercise2(Path dir) throws IOException {
        // TODO: twoje rozwiązanie
        return dir;
    }

    /**
     * ĆWICZENIE 3 (średnie): skopiuj plik source na target tak, żeby istniejący target został nadpisany
     * (bez wyjątku FileAlreadyExistsException).
     * Podpowiedź: StandardCopyOption.REPLACE_EXISTING.
     */
    static void exercise3(Path source, Path target) throws IOException {
        // TODO: twoje rozwiązanie
    }

    /**
     * ĆWICZENIE 4 (średnie): PRZEPISZ ze starego stylu na NIO.2. Stary kod:
     * <pre>{@code
     * File f = new File(path);
     * if (f.exists()) {
     *     if (f.delete()) { return "usunięto"; } else { return "nie udało się"; }
     * }
     * return "nie było";
     * }</pre>
     * Nowa wersja ma zwrócić "usunięto", gdy plik istniał (i usuń go), albo "nie było", gdy go nie było.
     * Podpowiedź: Files.deleteIfExists zwraca boolean — i nie ma luki czasowej między exists a delete.
     */
    static String exercise4(Path path) throws IOException {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): kopia zapasowa przed zapisem. Dla pliku "dane.txt" zrób kopię "dane.txt.bak"
     * obok niego (nadpisując starą kopię, jeśli jest), potem nadpisz dane.txt napisem "nowe". Zwróć
     * zawartość kopii, znak "|" i nową zawartość dane.txt, czyli "stare|nowe".
     * Podpowiedź: resolveSibling(file.getFileName() + ".bak"), copy z REPLACE_EXISTING, writeString, readString.
     */
    static String exercise5(Path file) throws IOException {
        // TODO: twoje rozwiązanie
        return "";
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1);
    }

    static Path solution2(Path dir) throws IOException {
        Path deep = dir.resolve("a/b/c");
        Files.createDirectories(deep);
        return Files.createFile(deep.resolve("plik.txt"));
    }

    static void solution3(Path source, Path target) throws IOException {
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    static String solution4(Path path) throws IOException {
        return Files.deleteIfExists(path) ? "usunięto" : "nie było";
    }

    static String solution5(Path file) throws IOException {
        Path backup = file.resolveSibling(file.getFileName() + ".bak");
        Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(file, "nowe");
        return Files.readString(backup) + "|" + Files.readString(file);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Nie. Path to tylko nazwa; można go zbudować dla pliku, którego nie ma. Files.exists da wtedy false.
     *   2. createDirectory tworzy JEDEN poziom i rzuca FileAlreadyExistsException, gdy katalog już jest (oraz
     *      NoSuchFileException, gdy brakuje rodzica). createDirectories tworzy całą drogę i gdy katalog jest — nic nie robi.
     *   3. false — startsWith na Path porównuje całe elementy, a "ab" to nie cały element "abc".
     *   4. a/c   (na Windows: a\c — dlatego w lekcji zamieniamy "\" na "/")
     *   5. Files.copy bez opcji rzuci FileAlreadyExistsException, gdy "cel" już istnieje. Trzeba dodać
     *      StandardCopyOption.REPLACE_EXISTING.
     *   6. exists() i notExists() mogą oba zwrócić false, gdy system nie potrafi ustalić stanu (np. brak uprawnień).
     *      !exists znaczy więc „nie wiadomo, że jest”, a nie „na pewno nie ma”. Nawet notExists() == true mówi tylko
     *      o chwili sprawdzenia — zaraz potem inny program może plik utworzyć (wyścig „sprawdź, potem użyj”).
     *      Pewniej: od razu wykonaj operację (np. createFile albo CREATE_NEW) i złap FileAlreadyExistsException.
     *   7. Niepusty katalog nie może zostać usunięty jedną operacją. Trzeba najpierw usunąć jego zawartość (od
     *      najgłębszych elementów w górę), np. przez Files.walk + sorted(reverseOrder) albo walkFileTree (Io07).
     *   8. Bo operacje na plikach zawodzą z powodów spoza kontroli programu (dysk, sieć, uprawnienia). Kompilator
     *      wymusza więc obsługę albo jawne przekazanie wyjątku dalej.
     */
    // </editor-fold>
}
