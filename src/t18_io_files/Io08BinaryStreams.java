package t18_io_files;

import helpers.Check;
import helpers.TempDir;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Supplier;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Strumienie bajtów — InputStream, OutputStream, Data*Stream i własny format binarny
 *        (byte = bajt; stream = strumień; buffer = bufor; decorator = dekorator (opakowanie);
 *         big-endian = najstarszy bajt pierwszy; magic number = znacznik na początku pliku)
 *
 * W SKRÓCIE:
 *   Plik to w głębi ciąg bajtów (liczb 0–255). Tekst to bajty + kodowanie, a obrazek, ZIP czy własny format — bajty
 *   bez znaczenia tekstowego. InputStream czyta bajty, OutputStream je zapisuje. Reader i Writer to ich „tekstowe”
 *   nakładki, które dodają kodowanie (zamianę bajtów na znaki).
 *
 * ANALOGIA: strumień bajtów to taśma montażowa.
 *   Klocki (bajty) płyną jeden za drugim. Możesz je brać pojedynczo (wolno) albo całymi skrzynkami (bufor — szybko).
 *   Dekorator to kolejne stanowisko przy taśmie: jedno pakuje klocki w skrzynki (Buffered), inne zamienia liczby
 *   na klocki i z powrotem (Data). Stanowiska można łączyć w dowolnej kolejności.
 *
 * JAK TO DZIAŁA:
 *   Tekst:   plik ← Writer ← (kodowanie UTF-8) ← String         Bajty: plik ← OutputStream ← bajty
 *   Dekoratory (każdy opakowuje następny, zamykanie zewnętrznego zamyka wszystkie):
 *       DataOutputStream  →  BufferedOutputStream  →  strumień do pliku (Files.newOutputStream)
 *       liczby i napisy         bufor (mniej wywołań)      sam zapis bajtów na dysk
 *
 *   Ważne fakty:
 *   • read() zwraca int 0..255 albo -1 (koniec); byte w Javie ma znak (−128..127) — stąd maska & 0xFF.
 *   • read(byte[]) zwraca, ILE bajtów faktycznie wczytał (mniej niż rozmiar tablicy też jest normalne!), albo -1.
 *   • DataOutputStream zapisuje liczby w porządku big-endian (najstarszy bajt pierwszy): int 258 → 00 00 01 02.
 *   • EOFException (koniec pliku w środku liczby) to IOException — plik jest obcięty lub czytasz w złej kolejności.
 *
 * SŁÓWKA:
 *   input = wejście; output = wyjście; read = czytaj; write = pisz; flush = wypchnij bufor; close = zamknij;
 *   copy = kopiuj; transfer = przenieś; hex = zapis szesnastkowy; magic = znacznik; version = wersja;
 *   record = rekord (też: pozycja w pliku); big/little endian = porządek bajtów; counting = zliczający.
 *
 * ZOBACZ TEŻ: t18_io_files/Io02ReadingText (Reader), t18_io_files/Io03WritingText (Writer, flush i close),
 *   t18_io_files/Io09Serialization (zapis obiektów), t18_io_files/Io12Charsets (kodowania),
 *   t18_io_files/Io13ZipArchives (ZIP to też strumienie bajtów)
 * </pre>
 */
public class Io08BinaryStreams {

    public static void main(String[] args) throws IOException {
        // throws IOException = „rzuca IOException” — main przekazuje wyjątek wyżej
        title("Io08 — Strumienie bajtów i format binarny");

        Path dir = TempDir.create("io08"); // create = utwórz; wszystko znika w finally
        try {
            bytesAndHex();                         // bytes and hex = bajty i zapis szesnastkowy
            signedByte();                          // signed byte = bajt ze znakiem
            readingChunks(dir.resolve("s3"));      // reading chunks = czytanie kawałkami
            copying(dir.resolve("s4"));            // copying = kopiowanie
            decoratorsAndBuffer(dir.resolve("s5")); // decorators and buffer = dekoratory i bufor
            dataStreams();                         // data streams = strumienie danych
            customFormat(dir.resolve("s7"));       // custom format = własny format
            inMemory();                            // in memory = w pamięci
            exercises();                           // exercises = ćwiczenia
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
     */
    private static String rel(Path base, Path p) {
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
            if (e instanceof java.nio.file.FileSystemException fse && fse.getFile() != null) { // instanceof ze wzorcem (Java 16+)
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

    /** hex = bajty jako cyfry szesnastkowe, małymi literami, bez separatorów (HexFormat to Java 17+). */
    private static String hex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes); // formatHex = zapisz jako hex
    }

    /**
     * TrickleInputStream = „sączący się strumień”: oddaje najwyżej 3 bajty naraz, choć prosimy o więcej.
     * Tak potrafi się zachować prawdziwe źródło (sieć, plik, rura) — a my dostajemy to w teście deterministycznie.
     */
    private static final class TrickleInputStream extends FilterInputStream {
        TrickleInputStream(byte[] data) {
            super(new ByteArrayInputStream(data));
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return super.read(b, off, Math.min(len, 3));
        }
    }

    /** CountingOutputStream = zapisuje do pamięci i LICZY wywołania write (nie bajty) — do pokazania sensu bufora. */
    private static final class CountingOutputStream extends OutputStream {
        private final ByteArrayOutputStream sink = new ByteArrayOutputStream();
        private int calls;

        @Override
        public void write(int b) {
            calls++;
            sink.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            calls++;
            sink.write(b, off, len);
        }
    }

    // =================================================================================================
    // 1. BAJTY A ZNAKI
    // =================================================================================================

    /**
     * 1. Bajt to liczba 0–255. Napis zamienia się na bajty według KODOWANIA. Polskie litery w UTF-8 mają 2 bajty,
     * więc liczba znaków i liczba bajtów różnią się. Do podglądu bajtów używamy zapisu szesnastkowego (hex).
     */
    static void bytesAndHex() throws IOException {
        section("1. Bajty a znaki");

        String text = "zażółć";
        byte[] utf8 = text.getBytes(StandardCharsets.UTF_8); // getBytes = zamień napis na bajty w danym kodowaniu
        show("liczba znaków", text.length());
        // WYNIK: liczba znaków → 6
        show("liczba bajtów UTF-8", utf8.length);
        // WYNIK: liczba bajtów UTF-8 → 10
        show("bajty (hex)", hex(utf8));
        // WYNIK: bajty (hex) → 7a61c5bcc3b3c582c487
        // Rozbiór: 7a = z, 61 = a (ASCII, po jednym bajcie), c5bc = ż, c3b3 = ó, c582 = ł, c487 = ć (po dwa bajty).

        show("odwrotnie: hex → bajty → napis", new String(HexFormat.of().parseHex("c582c3b3"), StandardCharsets.UTF_8)); // parseHex = odczytaj hex
        // WYNIK: odwrotnie: hex → bajty → napis → łó

        // Reader/Writer to InputStream/OutputStream + kodowanie. Gdy pliku NIE traktujesz jako tekst (obrazek, ZIP,
        // własny format), używasz strumieni bajtów wprost: przepuszczenie bajtów przez Reader mogłoby je zepsuć.

        // Każdy sposób zapisu i odczytu bajtów całego pliku:
        //   Files.write(path, bajty) / Files.readAllBytes(path)  — małe pliki, wszystko w pamięci (Java 7+)
        //   Files.newOutputStream / newInputStream               — strumieniowo, duże pliki (Java 7+)
        //   InputStream.readAllBytes() (Java 9+), readNBytes(n) (Java 11+) — z istniejącego strumienia
    }

    // =================================================================================================
    // 2. BAJT ZE ZNAKIEM
    // =================================================================================================

    /**
     * 2. byte w Javie jest ZE ZNAKIEM: zakres −128..127. Bajt 200 (0xC8) po rzutowaniu na byte to −56. Żeby odzyskać
     * wartość 0–255, robimy maskę & 0xFF (albo Byte.toUnsignedInt).
     */
    static void signedByte() {
        section("2. Bajt ze znakiem");

        byte b = (byte) 200; // rzutowanie (byte) = przytnij do jednego bajtu
        show("(byte) 200", b);
        // WYNIK: (byte) 200 → -56
        show("b & 0xFF", b & 0xFF); // & 0xFF = zostaw tylko 8 najmłodszych bitów (maska)
        // WYNIK: b & 0xFF → 200
        show("Byte.toUnsignedInt(b)", Byte.toUnsignedInt(b)); // toUnsignedInt = jako liczba bez znaku
        // WYNIK: Byte.toUnsignedInt(b) → 200

        byte ff = (byte) 0xFF;
        show("(byte) 0xFF", ff);
        // WYNIK: (byte) 0xFF → -1
        show("ff == 0xFF", ff == 0xFF); // ff jest rozszerzone do int: −1, a 0xFF to 255
        // WYNIK: ff == 0xFF → false
        show("(ff & 0xFF) == 0xFF", (ff & 0xFF) == 0xFF);
        // WYNIK: (ff & 0xFF) == 0xFF → true

        // Dlatego InputStream.read() zwraca INT 0..255, a nie byte: wartość −1 jest wolna na znak „koniec strumienia”.
        // Gdyby read zwracał byte, bajt 0xFF (−1) byłby nie do odróżnienia od końca pliku.

        // Hex z bajtów ujemnych: Formatter (String.format) dla opakowanego Byte sam „odejmuje znak”, więc "%02x" daje c8.
        // Ale gdy bajt trafi do zmiennej int (np. po rozszerzeniu (int) b), znak zostaje i wychodzi ffffffc8.
        show("format %02x dla byte", String.format("%02x", b));
        // WYNIK: format %02x dla byte → c8
        show("format %02x dla (int) b", String.format("%02x", (int) b));
        // WYNIK: format %02x dla (int) b → ffffffc8
        show("format %02x dla b & 0xFF", String.format("%02x", b & 0xFF));
        // WYNIK: format %02x dla b & 0xFF → c8
        show("HexFormat", hex(new byte[] {b})); // HexFormat sam traktuje bajty jako 0..255
        // WYNIK: HexFormat → c8

        // PUŁAPKA: porównanie byte z literałem większym niż 127 (np. b == 0xC8) nigdy nie jest prawdziwe — b jest
        //   ujemne. Zawsze maskuj (b & 0xFF) przed porównaniem lub obliczeniami.
        // DOBRA PRAKTYKA: zamieniaj bajty na liczby 0–255 od razu po odczycie (Byte.toUnsignedInt). Dlaczego: ujemne
        //   „bajty” dają zaskakujące wyniki w sumach, porównaniach i przesunięciach bitów.
    }

    // =================================================================================================
    // 3. CZYTANIE KAWAŁKAMI
    // =================================================================================================

    /**
     * 3. read(byte[]) wczytuje „coś, najwyżej tyle, ile mieści tablica” i zwraca liczbę wczytanych bajtów. Ta liczba
     * bywa mniejsza niż rozmiar tablicy, nawet gdy dalej są dane (sieć, rura, wolny dysk). Pętla musi ją uwzględniać.
     */
    static void readingChunks(Path base) throws IOException {
        section("3. Czytanie kawałkami: read(byte[])");
        Files.createDirectory(base);

        byte[] data = "0123456789".getBytes(StandardCharsets.US_ASCII); // 10 bajtów
        try (InputStream in = new TrickleInputStream(data)) {
            byte[] buffer = new byte[10];
            int n = in.read(buffer); // PUŁAPKA: jedno wywołanie, bez pętli i bez sprawdzenia wyniku
            show("jedno read(buffer) zwróciło", n);
            // WYNIK: jedno read(buffer) zwróciło → 3
            show("a dane miały bajtów", data.length);
            // WYNIK: a dane miały bajtów → 10
        }

        // Poprawna pętla: czytamy aż read zwróci -1 i zapisujemy tylko n pierwszych bajtów bufora.
        ByteArrayOutputStream all = new ByteArrayOutputStream(); // ByteArrayOutputStream = bajty w pamięci
        List<Integer> sizes = new ArrayList<>();
        try (InputStream in = new TrickleInputStream(data)) {
            byte[] buffer = new byte[4];
            int n;
            while ((n = in.read(buffer)) != -1) { // przypisanie w warunku: czytaj i sprawdź w jednym kroku
                sizes.add(n);
                all.write(buffer, 0, n); // write(tablica, od, ile): tylko faktycznie wczytane bajty
            }
        }
        show("rozmiary kolejnych odczytów", sizes);
        // WYNIK: rozmiary kolejnych odczytów → [3, 3, 3, 1]
        show("złożone w całość", all.toString(StandardCharsets.US_ASCII)); // toString(Charset): Java 10+
        // WYNIK: złożone w całość → 0123456789

        // Gotowce, które robią tę pętlę za Ciebie (i mają ją poprawną):
        try (InputStream in = new TrickleInputStream(data)) {
            show("readAllBytes (Java 9+)", new String(in.readAllBytes(), StandardCharsets.US_ASCII));
            // WYNIK: readAllBytes (Java 9+) → 0123456789
        }
        try (InputStream in = new TrickleInputStream(data)) {
            byte[] first = in.readNBytes(4); // readNBytes(n) (Java 11+) czyta aż do n bajtów lub końca strumienia
            show("readNBytes(4) (Java 11+)", new String(first, StandardCharsets.US_ASCII));
            // WYNIK: readNBytes(4) (Java 11+) → 0123
        }

        // Z plikiem: ten sam wzorzec, strumień z NIO.2 (Files.newInputStream).
        Path file = base.resolve("dane.bin");
        Files.write(file, data); // write = zapisz bajty (tworzy lub zastępuje plik)
        long sum = 0;
        try (InputStream in = Files.newInputStream(file)) { // newInputStream = nowy strumień wejściowy
            int one;
            while ((one = in.read()) != -1) { // read() = jeden bajt jako int 0..255; wolne bez bufora (sekcja 5)
                sum += one;
            }
        }
        show("suma kodów bajtów pliku (48..57)", sum);
        // WYNIK: suma kodów bajtów pliku (48..57) → 525

        // PUŁAPKA: ignorowanie wyniku read(byte[]) — kod „działa na moim komputerze” (plik lokalny zwykle oddaje całość), a
        //   na sieci, w rurze lub przy dużych plikach gubi dane. Nigdy nie zakładaj, że read wypełnił całą tablicę.
        // PUŁAPKA: new String(buffer) po pętli (bez długości n) wstawia do napisu resztki starych danych z bufora.
        // DOBRA PRAKTYKA: gdy chcesz wszystko — readAllBytes lub Files.readAllBytes (małe dane); gdy znasz liczbę bajtów —
        //   readNBytes(n). Dlaczego: własna pętla to miejsce na błąd, a gotowe metody są sprawdzone.
    }

    // =================================================================================================
    // 4. KOPIOWANIE
    // =================================================================================================

    /**
     * 4. Trzy sposoby skopiowania strumienia: pętla z buforem, transferTo (Java 9+) i Files.copy. Dwa ostatnie to
     * jedna linijka. Do sprawdzenia, czy pliki są identyczne, służy Files.mismatch (Java 12+): -1 = identyczne.
     */
    static void copying(Path base) throws IOException {
        section("4. Kopiowanie");
        Files.createDirectory(base);

        Path source = base.resolve("zrodlo.txt");
        Files.writeString(source, "zażółć gęślą jaźń\n", StandardCharsets.UTF_8); // 17 znaków + "\n" → 27 bajtów w UTF-8
        show("rozmiar źródła", Files.size(source));
        // WYNIK: rozmiar źródła → 27

        // 1) pętla z buforem (klasyczny sposób)
        Path copy1 = base.resolve("kopia1.txt");
        long copied = 0;
        try (InputStream in = Files.newInputStream(source);
             OutputStream out = Files.newOutputStream(copy1)) { // dwa zasoby: zamykane w odwrotnej kolejności
            byte[] buffer = new byte[8];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
                copied += n;
            }
        }
        show("pętla: skopiowano bajtów", copied);
        // WYNIK: pętla: skopiowano bajtów → 27

        // 2) transferTo (Java 9+): „przelej wszystko z tego strumienia do tamtego”
        Path copy2 = base.resolve("kopia2.txt");
        try (InputStream in = Files.newInputStream(source); OutputStream out = Files.newOutputStream(copy2)) {
            long moved = in.transferTo(out); // transferTo = przenieś do; zwraca liczbę bajtów
            show("transferTo: bajty", moved);
            // WYNIK: transferTo: bajty → 27
        }

        // 3) Files.copy: ścieżka → ścieżka, strumień → ścieżka, ścieżka → strumień
        Path copy3 = base.resolve("kopia3.txt");
        Files.copy(source, copy3);
        Path copy4 = base.resolve("kopia4.txt");
        try (InputStream in = Files.newInputStream(source)) {
            long n = Files.copy(in, copy4, StandardCopyOption.REPLACE_EXISTING); // strumień → plik; zwraca liczbę bajtów
            show("Files.copy(strumień, plik): bajty", n);
            // WYNIK: Files.copy(strumień, plik): bajty → 27
        }

        for (Path copy : List.of(copy1, copy2, copy3, copy4)) {
            // mismatch = „niezgodność”: indeks pierwszego różnego bajtu albo -1, gdy pliki są identyczne
            show("mismatch(" + rel(base, copy) + ")", Files.mismatch(source, copy)); // Java 12+
        }
        // WYNIK: mismatch(kopia1.txt) → -1
        // WYNIK: mismatch(kopia2.txt) → -1
        // WYNIK: mismatch(kopia3.txt) → -1
        // WYNIK: mismatch(kopia4.txt) → -1

        Files.writeString(copy4, "zażółć gęślą jaźń?", StandardCharsets.UTF_8); // zamiast "\n" na końcu jest "?"
        show("mismatch po zmianie ostatniego znaku", Files.mismatch(source, copy4));
        // WYNIK: mismatch po zmianie ostatniego znaku → 26
        // Pierwsza różnica jest w ostatnim bajcie (indeks 26, liczony od zera): w źródle "\n", w kopii "?".

        // PUŁAPKA: kopiowanie TEKSTU przez Reader/Writer („czytaj linie, pisz linie”) zmienia końce linii i może
        //   zmienić kodowanie. Do kopii pliku — jakiegokolwiek — używaj bajtów (Files.copy).
        // DOBRA PRAKTYKA: Files.copy zamiast własnej pętli. Dlaczego: robi to samo, krócej, bez błędu w pętli
        //   i często szybciej (system może skopiować plik własnymi środkami).
    }

    // =================================================================================================
    // 5. DEKORATORY I BUFOR
    // =================================================================================================

    /**
     * 5. Dekorator opakowuje strumień i dodaje zachowanie, zachowując ten sam interfejs (jest „kolejnym”
     * OutputStream). Buffered zbiera bajty w tablicy (domyślnie 8192) i wypycha je do środka rzadziej.
     * Dlaczego to ważne? Każde wywołanie write na pliku to kosztowne przejście do systemu operacyjnego.
     */
    static void decoratorsAndBuffer(Path base) throws IOException {
        section("5. Dekoratory i bufor");
        Files.createDirectory(base);

        // Łańcuch dekoratorów: liczby i napisy (Data) → bufor (Buffered) → plik. Każdy zasób w try, zamykanie odwrotne.
        Path file = base.resolve("liczby.bin");
        try (OutputStream raw = Files.newOutputStream(file);
             BufferedOutputStream buffered = new BufferedOutputStream(raw);
             DataOutputStream out = new DataOutputStream(buffered)) {
            out.writeInt(42);        // 4 bajty
            out.writeUTF("Zażółć");  // 2 bajty długości + 10 bajtów UTF-8
            out.writeDouble(3.5);    // 8 bajtów
        }
        show("rozmiar pliku (4 + 2 + 10 + 8)", Files.size(file));
        // WYNIK: rozmiar pliku (4 + 2 + 10 + 8) → 24

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            show("odczyt: int", in.readInt());
            // WYNIK: odczyt: int → 42
            show("odczyt: napis", in.readUTF());
            // WYNIK: odczyt: napis → Zażółć
            show("odczyt: double", in.readDouble());
            // WYNIK: odczyt: double → 3.5
        }
        // Zamknięcie zewnętrznego strumienia zamyka wszystkie wewnętrzne. Zapis do pliku kończy się wypchnięciem
        // bufora (flush) przy close — stąd try-with-resources jest tu obowiązkowy (bez close dane mogą zostać w buforze).

        // Dlaczego bufor: liczymy WYWOŁANIA write do „dna” (do strumienia zliczającego), nie czas.
        CountingOutputStream plain = new CountingOutputStream();
        for (int i = 0; i < 1000; i++) {
            plain.write(65); // bez bufora: każdy bajt = jedno wywołanie
        }
        show("bez bufora: wywołań write", plain.calls);
        // WYNIK: bez bufora: wywołań write → 1000

        CountingOutputStream counting = new CountingOutputStream();
        try (BufferedOutputStream buffered = new BufferedOutputStream(counting, 100)) { // 100 = mały bufor, żeby było widać
            for (int i = 0; i < 1000; i++) {
                buffered.write(65);
            }
            show("z buforem 100 B, przed zamknięciem", counting.calls); // 9 wypchnięć, ostatnie 100 bajtów jeszcze w buforze
            // WYNIK: z buforem 100 B, przed zamknięciem → 9
        }
        show("z buforem 100 B, po zamknięciu", counting.calls);
        // WYNIK: z buforem 100 B, po zamknięciu → 10
        show("a bajtów i tak tyle samo", counting.sink.size());
        // WYNIK: a bajtów i tak tyle samo → 1000

        // Przy domyślnym buforze 8192 bajtów byłoby 1000 / 8192 → jedno wypchnięcie przy zamknięciu.
        // PRZED (klasyczny, „na piechotę”): new FileOutputStream(plik) — bez bufora, z wyjątkiem FileNotFoundException.
        // PO: Files.newOutputStream(ścieżka) opakowany w BufferedOutputStream — wyjątki NIO.2 mówią, co poszło nie tak.
        // FileOutputStream(plik, true) dopisuje na końcu pliku (APPEND); bez „true” zaczyna od zera (obcina plik).

        // PUŁAPKA: pojedyncze read()/write(int) na strumieniu pliku BEZ bufora to jedno wywołanie systemowe na bajt —
        //   kopiowanie megabajtów trwa wtedy sekundy zamiast milisekund.
        // DOBRA PRAKTYKA: zapis i odczyt plików przez Buffered*, a zamykaj tylko zewnętrzny strumień (try-with-resources).
        //   Dlaczego: close zewnętrznego wypycha bufor i zamyka resztę; bez close końcówka danych przepada.
    }

    // =================================================================================================
    // 6. DataOutputStream I DataInputStream
    // =================================================================================================

    /** Zapisywacz danych do DataOutputStream (może rzucić IOException). */
    @FunctionalInterface
    private interface DataWriter {
        void write(DataOutputStream out) throws IOException;
    }

    /** bytesOf = zapisz coś przez DataOutputStream do pamięci i oddaj zapisane bajty. */
    private static byte[] bytesOf(DataWriter writer) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            writer.write(out);
        }
        return bytes.toByteArray(); // toByteArray = kopia zapisanych bajtów
    }

    /**
     * 6. DataOutputStream ma stały, zdefiniowany format: int = 4 bajty (big-endian), long = 8, double = 8 (IEEE 754),
     * boolean = 1, writeUTF = 2 bajty długości + napis w „zmodyfikowanym UTF-8”. Czytamy w TEJ SAMEJ kolejności.
     */
    static void dataStreams() throws IOException {
        section("6. DataOutputStream i DataInputStream");

        show("writeInt(258)", hex(bytesOf(out -> out.writeInt(258))));
        // WYNIK: writeInt(258) → 00000102
        show("writeBoolean(true)", hex(bytesOf(out -> out.writeBoolean(true))));
        // WYNIK: writeBoolean(true) → 01
        show("writeDouble(1.0)", hex(bytesOf(out -> out.writeDouble(1.0))));
        // WYNIK: writeDouble(1.0) → 3ff0000000000000
        show("writeUTF(\"ł\")", hex(bytesOf(out -> out.writeUTF("ł"))));
        // WYNIK: writeUTF("ł") → 0002c582
        show("writeLong(-1)", hex(bytesOf(out -> out.writeLong(-1L))));
        // WYNIK: writeLong(-1) → ffffffffffffffff

        // Porządek bajtów: DataOutputStream i sieć używają big-endian (najstarszy bajt pierwszy). Procesory x86
        // (komputer ucznia) liczą wewnętrznie little-endian (najmłodszy pierwszy), a pliki z programów w C często
        // też. ByteBuffer pozwala wybrać porządek jawnie:
        byte[] big = ByteBuffer.allocate(4).putInt(258).array(); // domyślnie big-endian
        byte[] little = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(258).array();
        show("ByteBuffer big-endian", hex(big));
        // WYNIK: ByteBuffer big-endian → 00000102
        show("ByteBuffer little-endian", hex(little));
        // WYNIK: ByteBuffer little-endian → 02010000
        show("odczyt little-endian jako big", ByteBuffer.wrap(little).getInt()); // wrap = opakuj tablicę; getInt = odczytaj int
        // WYNIK: odczyt little-endian jako big → 33619968

        // Odczyt w złej kolejności daje śmieci, a przy końcu danych — EOFException:
        byte[] oneInt = bytesOf(out -> out.writeInt(7));
        ioFails("readInt dwa razy z czterech bajtów", () -> {
            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(oneInt))) {
                in.readInt();
                in.readInt(); // drugiego inta już nie ma
            }
        });
        // WYNIK: ✔ readInt dwa razy z czterech bajtów → rzucono EOFException

        byte[] mixed = bytesOf(out -> {
            out.writeInt(1);
            out.writeUTF("tekst");
        });
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(mixed))) {
            show("zła kolejność: UTF zamiast int", in.readInt() + " / " + in.readShort()); // czytamy 4 bajty jako int, potem 2 jako short
            // WYNIK: zła kolejność: UTF zamiast int → 1 / 5
        }
        // Powyższe „5” to długość napisu "tekst" odczytana jako short — program nie zauważy błędu, jeśli typy pasują.

        // Granice: writeUTF zapisze najwyżej 65535 bajtów napisu (długość w 2 bajtach), dłuższy → UTFDataFormatException.
        // "Zmodyfikowany UTF-8": znak o kodzie 0 ma zapis 2-bajtowy (c0 80) — różni się od standardowego UTF-8, więc napisy
        // zapisane writeUTF czytaj tylko przez readUTF. Dla zwykłych plików tekstowych używaj Writer i kodowania UTF-8.

        // PUŁAPKA: format Data*Stream nie ma żadnych znaczników typu: plik to same bajty, a znaczenie wynika WYŁĄCZNIE
        //   z kolejności odczytu. Zmiana kolejności, dodanie pola lub inna wersja programu psują stare pliki.
        // DOBRA PRAKTYKA: zapisuj na początku znacznik formatu i NUMER WERSJI (sekcja 7). Dlaczego: dopiero wtedy
        //   nowy program potrafi rozpoznać stary plik (i odmówić czytania zepsutego).
    }

    // =================================================================================================
    // 7. WŁASNY FORMAT BINARNY
    // =================================================================================================

    /** Item = pozycja w pliku binarnym: numer, nazwa, cena. */
    record Item(int id, String name, double price) { }

    private static final byte[] MAGIC = {'P', 'R', 'D', 'X'}; // znacznik formatu: cztery znaki ASCII
    private static final int VERSION = 1;

    /**
     * writeItems = zapisz listę pozycji w naszym formacie: MAGIC (4 B), wersja (1 B), liczba pozycji (int),
     * potem dla każdej: id (int), nazwa (writeUTF), cena (double). Strumienia NIE zamykamy: należy do wywołującego.
     */
    private static void writeItems(OutputStream raw, List<Item> items) throws IOException {
        DataOutputStream out = new DataOutputStream(raw);
        out.write(MAGIC);
        out.writeByte(VERSION);
        out.writeInt(items.size());
        for (Item item : items) {
            out.writeInt(item.id());
            out.writeUTF(item.name());
            out.writeDouble(item.price());
        }
        out.flush(); // flush = wypchnij; close zostawiamy właścicielowi strumienia
    }

    /** readItems = odczyt z walidacją: znacznik, wersja, rozsądna liczba pozycji, brak nadmiarowych bajtów. */
    private static List<Item> readItems(InputStream raw) throws IOException {
        DataInputStream in = new DataInputStream(raw);
        byte[] magic = in.readNBytes(4);
        if (!Arrays.equals(magic, MAGIC)) {
            throw new IOException("to nie jest plik PRDX (zły znacznik)");
        }
        int version = in.readUnsignedByte(); // readUnsignedByte = bajt jako 0..255
        if (version != VERSION) {
            throw new IOException("nieobsługiwana wersja formatu: " + version);
        }
        int count = in.readInt();
        if (count < 0 || count > 1_000_000) { // nie ufamy liczbie z pliku: nie rezerwujemy pamięci na ślepo
            throw new IOException("podejrzana liczba pozycji: " + count);
        }
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(new Item(in.readInt(), in.readUTF(), in.readDouble())); // argumenty liczone od lewej do prawej
        }
        if (in.read() != -1) {
            throw new IOException("nadmiarowe bajty po ostatniej pozycji");
        }
        return items;
    }

    /** formatFails = spodziewamy się błędu odczytu; wypisujemy klasę wyjątku i (nasz własny) komunikat. */
    private static void formatFails(String label, byte[] bytes) {
        try {
            readItems(new ByteArrayInputStream(bytes));
            System.out.println("✘ " + label + " → NIE rzucono wyjątku (a spodziewaliśmy się go)");
        } catch (IOException e) {
            String message = e.getMessage() == null ? "" : ": " + e.getMessage();
            System.out.println("✔ " + label + " → rzucono " + e.getClass().getSimpleName() + message);
        }
    }

    /**
     * 7. Własny format: znacznik (magic number) pozwala szybko poznać, że to nasz plik; wersja — że potrafimy go
     * czytać; liczba pozycji — ile ich oczekiwać. Walidacja chroni przed plikami uszkodzonymi i cudzymi.
     */
    static void customFormat(Path base) throws IOException {
        section("7. Własny format binarny: znacznik, wersja, pozycje");
        Files.createDirectory(base);

        List<Item> items = List.of(new Item(1, "Kawa", 64.99), new Item(2, "Herbata zażółć", 12.5), new Item(3, "Czekolada", 7.49));
        Path file = base.resolve("pozycje.prdx");
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(file))) {
            writeItems(out, items);
        }
        show("rozmiar pliku (9 + 18 + 32 + 23)", Files.size(file));
        // WYNIK: rozmiar pliku (9 + 18 + 32 + 23) → 82

        byte[] bytes = Files.readAllBytes(file);
        show("nagłówek (znacznik, wersja, liczba)", hex(Arrays.copyOf(bytes, 9)));
        // WYNIK: nagłówek (znacznik, wersja, liczba) → 505244580100000003
        show("początek jako tekst", new String(bytes, 0, 4, StandardCharsets.US_ASCII));
        // WYNIK: początek jako tekst → PRDX

        // Zrzut szesnastkowy (hex dump): przesunięcie, bajty w hex, a po prawej znaki ASCII (kropka = niedrukowalny).
        for (String line : hexDump(bytes, 32)) {
            System.out.println(line);
        }
        // WYNIK: 00000000  50 52 44 58 01 00 00 00 03 00 00 00 01 00 04 4b  |PRDX...........K|
        // WYNIK: 00000010  61 77 61 40 50 3f 5c 28 f5 c2 8f 00 00 00 02 00  |awa@P?\(........|

        try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
            List<Item> back = readItems(in);
            show("odczytano pozycji", back.size());
            // WYNIK: odczytano pozycji → 3
            show("trzecia", back.get(2));
            // WYNIK: trzecia → Item[id=3, name=Czekolada, price=7.49]
            show("identyczne z zapisanymi (double bez strat)", back.equals(items));
            // WYNIK: identyczne z zapisanymi (double bez strat) → true
        }

        // Walidacja: każdy zły plik daje czytelny błąd, a nie dziwne dane.
        byte[] wrongMagic = bytes.clone();
        wrongMagic[0] = 'X';
        formatFails("zły znacznik", wrongMagic);
        // WYNIK: ✔ zły znacznik → rzucono IOException: to nie jest plik PRDX (zły znacznik)
        byte[] wrongVersion = bytes.clone();
        wrongVersion[4] = 2;
        formatFails("zła wersja", wrongVersion);
        // WYNIK: ✔ zła wersja → rzucono IOException: nieobsługiwana wersja formatu: 2
        byte[] hugeCount = bytes.clone();
        hugeCount[5] = 0x7f; // pierwszy bajt liczby pozycji: 0x7f000003 ≈ 2 miliardy
        formatFails("zmyślona liczba pozycji", hugeCount);
        // WYNIK: ✔ zmyślona liczba pozycji → rzucono IOException: podejrzana liczba pozycji: 2130706435
        formatFails("plik obcięty", Arrays.copyOf(bytes, 40));
        // WYNIK: ✔ plik obcięty → rzucono EOFException
        formatFails("pusty plik", new byte[0]);
        // WYNIK: ✔ pusty plik → rzucono IOException: to nie jest plik PRDX (zły znacznik)
        byte[] extra = Arrays.copyOf(bytes, bytes.length + 1);
        formatFails("bajt za dużo na końcu", extra);
        // WYNIK: ✔ bajt za dużo na końcu → rzucono IOException: nadmiarowe bajty po ostatniej pozycji

        // Prawdziwe formaty działają tak samo: PNG zaczyna się od bajtów 89 50 4E 47, ZIP od 50 4B ("PK"), PDF od "%PDF".
        // Programy rozpoznają typ pliku po znaczniku, a nie po rozszerzeniu.

        // PUŁAPKA: ślepe zaufanie do liczby z pliku (new byte[count], new ArrayList<>(count)) to prosty atak: mały plik z
        //   „count = 2 miliardy” wyczerpuje pamięć. Sprawdzaj zakres przed alokacją.
        // DOBRA PRAKTYKA: własny format → znacznik + wersja + walidacja; a jeśli nie musisz — nie wymyślaj formatu:
        //   użyj JSON (Io05JsonManual), CSV (Io04Csv) lub biblioteki (protobuf, Avro). Dlaczego: gotowe formaty mają
        //   narzędzia, dokumentację i zgodność między językami.
    }

    /** hexDump = linie zrzutu: 16 bajtów na linię; maksymalnie `limit` bajtów. */
    private static List<String> hexDump(byte[] bytes, int limit) {
        List<String> lines = new ArrayList<>();
        int end = Math.min(bytes.length, limit);
        for (int start = 0; start < end; start += 16) {
            int stop = Math.min(start + 16, end);
            StringBuilder ascii = new StringBuilder();
            for (int i = start; i < stop; i++) {
                int value = bytes[i] & 0xFF;
                ascii.append(value >= 32 && value < 127 ? (char) value : '.');
            }
            String hexPart = HexFormat.ofDelimiter(" ").formatHex(bytes, start, stop); // Java 17+: hex z odstępami
            lines.add(String.format("%08x  %-47s  |%s|", start, hexPart, ascii));
        }
        return lines;
    }

    // =================================================================================================
    // 8. STRUMIENIE W PAMIĘCI
    // =================================================================================================

    /**
     * 8. ByteArrayOutputStream i ByteArrayInputStream to strumienie „do tablicy” i „z tablicy”. Dzięki temu
     * kod, który przyjmuje InputStream/OutputStream (jak writeItems i readItems wyżej), testujemy bez plików.
     */
    static void inMemory() throws IOException {
        section("8. Strumienie w pamięci");

        List<Item> items = List.of(new Item(10, "Test", 1.5), new Item(11, "Łódź", 2.25));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        writeItems(bytes, items); // ta sama metoda co dla pliku — tylko inny strumień
        show("rozmiar w pamięci (9 + 18 + 21)", bytes.size());
        // WYNIK: rozmiar w pamięci (9 + 18 + 21) → 48
        List<Item> back = readItems(new ByteArrayInputStream(bytes.toByteArray())); // ByteArrayInputStream = czytaj z tablicy
        show("odczyt z pamięci", back);
        // WYNIK: odczyt z pamięci → [Item[id=10, name=Test, price=1.5], Item[id=11, name=Łódź, price=2.25]]

        // close() na strumieniach tablicowych nic nie robi — nie trzeba, ale try-with-resources też nie szkodzi.
        // Ten sam wzorzec w teście jednostkowym: przekaż ByteArrayInputStream zamiast pliku i sprawdź wynik — bez dysku,
        // bez katalogów tymczasowych, bez problemów systemu (Windows kontra Linux).

        // PUŁAPKA: ByteArrayOutputStream rośnie w pamięci bez limitu — nie używaj go do dużych danych.
        // DOBRA PRAKTYKA: metody przyjmujące InputStream/OutputStream (a nie Path) łatwiej testować i używać z siecią,
        //   plikiem i pamięcią. Dlaczego: zależą od abstrakcji, a nie od konkretnego źródła (t06_oop_basics).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Plik = bajty; InputStream/OutputStream = bajty, Reader/Writer = znaki (bajty + kodowanie).
     *   • byte ma znak (−128..127): (byte) 200 = −56; odzyskaj b & 0xFF; read() zwraca int 0..255 albo -1.
     *   • read(byte[]) zwraca liczbę wczytanych bajtów (może być mniejsza niż tablica!) lub -1; zapisuj write(buf, 0, n).
     *   • Gotowce: readAllBytes (9), readNBytes (11), transferTo (9), Files.copy, Files.mismatch (12; -1 = identyczne).
     *   • Dekoratory: Data → Buffered → plik; zamykaj tylko zewnętrzny (try-with-resources); bufor = mniej wywołań systemu.
     *   • Data*Stream: int = 4 B, long = 8 B, double = 8 B, boolean = 1 B, writeUTF = 2 B długości + napis; big-endian;
     *     czytaj w tej samej kolejności; koniec danych w środku → EOFException.
     *   • Własny format: znacznik + wersja + walidacja (zakres liczności!); HexFormat (Java 17) do podglądu bajtów.
     *   • ByteArrayInput/OutputStream = testy bez plików; metody na strumieniach są łatwiejsze do testowania.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się InputStream od Readera i kiedy używasz którego?
     *   2. Dlaczego read() zwraca int, a nie byte?
     *   3. Co wypisze:  byte b = (byte) 200;  System.out.println(b + " " + (b & 0xFF));  ?
     *   4. Co wypisze:  System.out.println(HexFormat.of().formatHex(new byte[] {0, 15, (byte) 255}));  ?
     *   5. ZNAJDŹ BŁĄD:  byte[] buf = new byte[8192];  in.read(buf);  String s = new String(buf, UTF_8);  — wymień dwa błędy.
     *   6. ZNAJDŹ BŁĄD:  DataOutputStream out = new DataOutputStream(Files.newOutputStream(p));  out.writeInt(1);  (bez close)
     *      — co tu jest nie tak i co się stanie, gdy dołożysz BufferedOutputStream, ale dalej nie zamkniesz?
     *   7. Co wypisze:  zapisujemy writeInt(1), writeInt(2), a potem czytamy readInt() trzy razy?
     *   8. Po co w własnym formacie binarnym znacznik na początku i numer wersji?
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
        Path dir = TempDir.create("io08cw");
        try {
            Check.equal("ćw. 1: bajty jako hex", "000fc87fff",
                    () -> reference ? solution1(new byte[] {0, 15, (byte) 200, 127, -1}) : exercise1(new byte[] {0, 15, (byte) 200, 127, -1}));

            byte[] ten = "0123456789".getBytes(StandardCharsets.US_ASCII);
            Check.equal("ćw. 2: PRZEPISZ read na pętlę", "10|0123456789", io(() -> {
                byte[] result = reference ? solution2(new TrickleInputStream(ten)) : exercise2(new TrickleInputStream(ten));
                return result.length + "|" + new String(result, StandardCharsets.US_ASCII);
            }));

            Check.equal("ćw. 3: kopiowanie buforem 4 B", "27|true", io(() -> {
                Path src = dir.resolve("src.txt");
                Path dst = dir.resolve("dst.txt");
                Files.writeString(src, "zażółć gęślą jaźń\n", StandardCharsets.UTF_8);
                long n = reference ? solution3(src, dst) : exercise3(src, dst);
                return n + "|" + (Files.mismatch(src, dst) == -1L);
            }));

            Check.equal("ćw. 4: własny kodek", "28|Zażółć:6", io(() -> {
                byte[] encoded = reference ? solution4Encode("Zażółć", new int[] {1, 2, 3}) : exercise4Encode("Zażółć", new int[] {1, 2, 3});
                String decoded = reference ? solution4Decode(encoded) : exercise4Decode(encoded);
                return encoded.length + "|" + decoded;
            }));
        } finally {
            TempDir.deleteRecursively(dir);
        }
        Check.summary();
    }

    /**
     * ĆWICZENIE 1 (łatwe): zamień bajty na napis szesnastkowy małymi literami, bez separatorów (bajt 200 → "c8",
     * bajt 15 → "0f"). Uwaga na bajty ujemne! Nie używaj HexFormat — zrób to ręcznie.
     * Podpowiedź: StringBuilder, String.format("%02x", b & 0xFF).
     */
    static String exercise1(byte[] bytes) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ błędny odczyt na poprawną pętlę. Stary kod (gubi dane, gdy read zwróci mniej):
     * <pre>{@code
     * byte[] buf = new byte[10];
     * in.read(buf);          // wynik ignorowany!
     * return buf;
     * }</pre>
     * Nowa wersja ma wczytać CAŁY strumień i zwrócić dokładnie wczytane bajty.
     * Podpowiedź: ByteArrayOutputStream, pętla while ((n = in.read(buffer)) != -1) i write(buffer, 0, n) — albo readAllBytes.
     */
    static byte[] exercise2(InputStream in) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): skopiuj plik bajt po bajcie buforem 4-bajtowym (BEZ Files.copy i transferTo) i zwróć
     * liczbę skopiowanych bajtów.
     * Podpowiedź: Files.newInputStream, Files.newOutputStream (oba w try), byte[4], write(buffer, 0, n).
     */
    static long exercise3(Path source, Path target) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): własny kodek. Encode: writeUTF(nazwa), writeInt(liczba wyników), potem każdy wynik
     * jako int. Decode: odczytaj to samo i zwróć "nazwa:suma wyników", np. "Zażółć:6". Użyj DataOutputStream
     * i DataInputStream na strumieniach w pamięci.
     * Podpowiedź: bytesOf (ma gotowe opakowanie), ByteArrayInputStream, pętla po liczbie wyników.
     */
    static byte[] exercise4Encode(String name, int[] scores) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    static String exercise4Decode(byte[] encoded) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }

    static byte[] solution2(InputStream in) throws IOException {
        ByteArrayOutputStream all = new ByteArrayOutputStream();
        byte[] buffer = new byte[4];
        int n;
        while ((n = in.read(buffer)) != -1) {
            all.write(buffer, 0, n);
        }
        return all.toByteArray();
    }

    static long solution3(Path source, Path target) throws IOException {
        long copied = 0;
        try (InputStream in = Files.newInputStream(source); OutputStream out = Files.newOutputStream(target)) {
            byte[] buffer = new byte[4];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
                copied += n;
            }
        }
        return copied;
    }

    static byte[] solution4Encode(String name, int[] scores) throws IOException {
        return bytesOf(out -> {
            out.writeUTF(name);
            out.writeInt(scores.length);
            for (int score : scores) {
                out.writeInt(score);
            }
        });
    }

    static String solution4Decode(byte[] encoded) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(encoded))) {
            String name = in.readUTF();
            int count = in.readInt();
            int sum = 0;
            for (int i = 0; i < count; i++) {
                sum += in.readInt();
            }
            return name + ":" + sum;
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. InputStream czyta bajty (każdy plik; obrazy, ZIP, własne formaty), Reader czyta znaki — to InputStream
     *      plus kodowanie (pliki tekstowe). Do tekstu: Reader z UTF-8; do reszty: strumień bajtów.
     *   2. Bo byte ma znak (−128..127), a potrzebna jest wartość 0..255 oraz wolny kod na „koniec strumienia”.
     *      int pozwala zwrócić −1 jako koniec, a bajt 255 jako zwykłą daną.
     *   3. -56 200   — (byte) 200 to −56, a b & 0xFF przywraca 200.
     *   4. 000fff   — bajty 0x00, 0x0f, 0xff (255 zapisane jako ff).
     *   5. (1) wynik read(buf) ignorowany: wczytane mogło być mniej niż 8192 bajty, a nawet -1; (2) new String(buf, UTF_8)
     *      bierze cały bufor, także nieużyte bajty (zera i resztki), zamiast new String(buf, 0, n, UTF_8).
     *   6. Files.newOutputStream nie ma bufora po stronie Javy, więc writeInt od razu oddaje 4 bajty systemowi — dane
     *      zwykle trafią do pliku. Błędem jest niezamknięty plik: wyciek uchwytu, a w Windows plik często zostaje
     *      zablokowany (nie da się go usunąć). Z BufferedOutputStream bez close jest GORZEJ: bajty utkną w buforze
     *      w pamięci Javy i przepadną przy końcu programu. Lekarstwo w obu wersjach: try-with-resources.
     *   7. Dwie pierwsze liczby (1 i 2) odczytają się poprawnie, a trzecie readInt() rzuci EOFException (koniec danych).
     *   8. Znacznik pozwala poznać, że to nasz format (i odrzucić cudzy plik), a wersja — że potrafimy go czytać
     *      (nowszy program rozpozna stary format, a stary — odmówi czytania nowego zamiast czytać śmieci).
     */
    // </editor-fold>
}
