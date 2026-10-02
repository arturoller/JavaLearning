package t18_io_files;

import helpers.Check;
import helpers.TempDir;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.io.BufferedReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Kodowanie znaków — bajty, znaki i punkty kodowe, UTF-8, krzaczki, BOM
 *        (charset = zestaw znaków i jego kodowanie; encoding = kodowanie; byte = bajt;
 *         code point = punkt kodowy; mojibake = „krzaczki”, zepsute polskie litery)
 *
 * W SKRÓCIE:
 *   Plik i sieć znają tylko BAJTY (liczby 0–255). Tekst to ZNAKI. Kodowanie (charset) to umowa:
 *   „taki znak zapisujemy takimi bajtami”. Gdy piszący i czytający używają RÓŻNYCH umów,
 *   z „zażółć” robi się „zaĹźĂłĹ‚Ä‡”. Dlatego przy każdym czytaniu i pisaniu podajemy kodowanie jawnie.
 *
 * ANALOGIA: kodowanie to szyfr z książką kodową.
 *   Ktoś zapisuje wiadomość liczbami według książki A. Ty odczytujesz te same liczby według książki B
 *   i dostajesz bełkot, choć żadna liczba się nie zmieniła. Liczby (bajty) są te same, zła jest książka.
 *   Litery łacińskie bez ogonków mają w prawie wszystkich książkach te same numery — dlatego błąd
 *   wychodzi dopiero przy ą, ę, ł, ż i spółce.
 *
 * JAK TO DZIAŁA:
 *   Tekst w Javie (String) = ciąg jednostek char, każda to 16 bitów (UTF-16).
 *   Znak spoza podstawowego zakresu (np. emoji) zajmuje DWA char (para zastępcza, ang. surrogate pair).
 *
 *   znak (punkt kodowy)   UTF-8        ISO-8859-2   windows-1250   UTF-16BE
 *   -------------------   -----------  -----------  -------------  --------
 *   a  (U+0061)           61           61           61             00 61
 *   ł  (U+0142)           C5 82        B3           B3             01 42
 *   ą  (U+0105)           C4 85        B1           B9             01 05
 *   emoji (U+1F600)       F0 9F 98 80  — brak —     — brak —       D8 3D DE 00
 *
 *   UTF-8: 1 bajt dla ASCII, 2 bajty dla polskich liter, 3 dla większości reszty świata, 4 dla emoji.
 *   Zapis: String.getBytes(kodowanie)   Odczyt: new String(bajty, kodowanie)
 *   Pliki: Files.readString / writeString (zawsze UTF-8, gdy nie podasz innego) albo wersje z parametrem Charset.
 *
 *   Domyślne kodowanie systemu: w Javie 17 zależy od systemu (polski Windows: windows-1250),
 *   od Javy 18 domyślnie UTF-8 (JEP 400). Nie polegaj na nim — podawaj kodowanie jawnie.
 *
 * SŁÓWKA:
 *   charset = zestaw znaków z kodowaniem; encode = zakoduj (znaki → bajty); decode = zdekoduj (bajty → znaki);
 *   code point = punkt kodowy; surrogate = zastępczy; malformed = źle uformowany; replacement = zastąpienie;
 *   BOM (byte order mark) = znacznik kolejności bajtów; normalize = ujednolić zapis; report = zgłoś.
 *
 * ZOBACZ TEŻ: t18_io_files/Io02ReadingText (MalformedInputException przy czytaniu), t04_strings/Strings01Basics
 *   (String i char), t18_io_files/Io06Properties (pliki .properties i ISO-8859-1),
 *   t18_io_files/Io11IoExceptions (wyjątki kodowania)
 * </pre>
 */
public class Io12Charsets {

    // main = metoda główna; throws IOException = „rzuca IOException”, przekazujemy wyjątek wyżej
    public static void main(String[] args) throws IOException {
        title("Io12 — kodowanie znaków (charset)");

        Path dir = TempDir.create("io12"); // create = utwórz
        try {
            bytesCharsCodePoints();        // bytes, chars, code points = bajty, znaki, punkty kodowe
            sizesInEncodings();            // sizes in encodings = rozmiary w różnych kodowaniach
            sameLetterDifferentBytes();    // same letter, different bytes = ta sama litera, inne bajty
            mojibake();                    // mojibake = krzaczki
            malformedBytes();              // malformed bytes = błędne bajty
            malformedInFiles(dir.resolve("s5")); // malformed in files = błędne bajty w plikach
            emoji();                       // emoji i punkty kodowe
            byteOrderMark(dir.resolve("s7")); // byte order mark = BOM
            defaultCharset(dir.resolve("s8")); // default charset = kodowanie domyślne
            charsetNames();                // charset names = nazwy kodowań
            normalization();               // normalization = normalizacja Unicode
            exercises();                   // exercises = ćwiczenia
        } finally {
            TempDir.deleteRecursively(dir);
        }
    }

    // =================================================================================================
    // Pomocnicze
    // =================================================================================================

    /**
     * rel = ścieżka względem katalogu bazowego, zawsze z ukośnikiem "/".
     * Katalog tymczasowy ma losową nazwę, a Windows pisze ścieżki z "\" — zamiana na "/" daje ten sam wydruk wszędzie.
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
     * ioFails = „IO zawodzi”. Wykonuje akcję i oczekuje wyjątku IO. Wypisuje nazwę klasy wyjątku, a NIE komunikat:
     * komunikaty IO zawierają pełną ścieżkę (losowy katalog) i tekst zależny od systemu, więc wydruk różniłby się
     * na Windows i na Linuksie.
     */
    private static void ioFails(String label, ThrowingIo action) {
        try {
            action.run();
            System.out.println("✘ " + label + " → NIE rzucono wyjątku (a spodziewaliśmy się go)");
        } catch (IOException e) {
            System.out.println("✔ " + label + " → rzucono " + e.getClass().getSimpleName());
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

    /** hex = bajty jako liczby szesnastkowe oddzielone spacjami (HexFormat — Java 17+; format = format). */
    private static String hex(byte[] bytes) {
        return HexFormat.ofDelimiter(" ").formatHex(bytes); // ofDelimiter = z separatorem
    }

    /** hex dla tekstu w danym kodowaniu. */
    private static String hex(String text, Charset charset) {
        return hex(text.getBytes(charset));
    }

    /** cp = zapis punktu kodowego jako "U+0142" (Integer.toHexString = liczba na szesnastkowo). */
    private static String cp(int codePoint) {
        return String.format("U+%04X", codePoint);
    }

    /** REPLACEMENT = numer znaku zastępczego U+FFFD (replacement = zastąpienie), który wstawia dekoder. */
    private static final char REPLACEMENT = (char) 0xFFFD;

    /** emojiText = tekst z jednym emoji U+1F600; budujemy go w kodzie, bo w źródle nie używamy escape'ów. */
    private static String grin() {
        return new String(Character.toChars(0x1F600)); // toChars = na tablicę char (tu: para zastępcza)
    }

    // =================================================================================================
    // 1. BAJT, ZNAK, PUNKT KODOWY
    // =================================================================================================

    /**
     * 1. Trzy różne rzeczy, które łatwo pomylić: bajt (byte, 8 bitów), jednostka char (16 bitów, UTF-16)
     * i punkt kodowy (code point, numer znaku w Unicode, np. U+0142 dla „ł”). Dla polskich liter
     * jeden znak = jeden char = jeden punkt kodowy, ale różna liczba bajtów — zależnie od kodowania.
     */
    static void bytesCharsCodePoints() {
        section("1. Bajt, znak (char), punkt kodowy");

        String text = "zażółć";
        show("length() — liczba jednostek char", text.length());
        // WYNIK: length() — liczba jednostek char → 6
        show("codePointCount — liczba znaków Unicode", text.codePointCount(0, text.length()));
        // WYNIK: codePointCount — liczba znaków Unicode → 6
        show("getBytes(UTF_8).length — liczba bajtów", text.getBytes(StandardCharsets.UTF_8).length);
        // WYNIK: getBytes(UTF_8).length — liczba bajtów → 10

        // Punkt kodowy to po prostu numer znaku w tabeli Unicode. char w Javie da się rzutować na liczbę.
        show("numer litery ł", cp('ł'));
        // WYNIK: numer litery ł → U+0142
        show("numer litery ż", cp('ż'));
        // WYNIK: numer litery ż → U+017C
        show("znak o numerze 0x0105", String.valueOf((char) 0x0105));
        // WYNIK: znak o numerze 0x0105 → ą

        // Zasada: „znak” w rozmowie to nie to samo co „bajt”. length() NIE mówi nic o rozmiarze pliku.
        // PUŁAPKA: rozmiar pola w bazie albo limit „255 znaków” a „255 bajtów” to dwie różne rzeczy.
        //   Dla tekstu polskiego plik UTF-8 jest dłuższy niż liczba znaków (każda ogonkowa litera = 2 bajty).
        // DOBRA PRAKTYKA: gdy liczysz rozmiar do zapisu (limit sieciowy, bufor), liczysz BAJTY
        //   w konkretnym kodowaniu: text.getBytes(UTF_8).length — a nie text.length().
        String slowo = "łódź";
        show("\"łódź\": znaki / bajty UTF-8", slowo.length() + " / " + slowo.getBytes(StandardCharsets.UTF_8).length);
        // WYNIK: "łódź": znaki / bajty UTF-8 → 4 / 7
    }

    // =================================================================================================
    // 2. ROZMIARY W RÓŻNYCH KODOWANIACH
    // =================================================================================================

    /**
     * 2. Ten sam tekst „zażółć” zajmuje różną liczbę bajtów: 6 w kodowaniach jednobajtowych
     * (ISO-8859-2, windows-1250), 10 w UTF-8, 12 w UTF-16 bez BOM i 14 w "UTF-16" (z BOM-em na początku).
     */
    static void sizesInEncodings() {
        section("2. Rozmiar tekstu w różnych kodowaniach");

        String text = "zażółć";
        Charset iso2 = Charset.forName("ISO-8859-2");     // forName = „po nazwie”
        Charset win1250 = Charset.forName("windows-1250");
        show("UTF-8", text.getBytes(StandardCharsets.UTF_8).length);
        // WYNIK: UTF-8 → 10
        show("ISO-8859-2", text.getBytes(iso2).length);
        // WYNIK: ISO-8859-2 → 6
        show("windows-1250", text.getBytes(win1250).length);
        // WYNIK: windows-1250 → 6
        show("UTF-16BE (big endian, bez BOM)", text.getBytes(StandardCharsets.UTF_16BE).length);
        // WYNIK: UTF-16BE (big endian, bez BOM) → 12
        show("UTF-16LE (little endian, bez BOM)", text.getBytes(StandardCharsets.UTF_16LE).length);
        // WYNIK: UTF-16LE (little endian, bez BOM) → 12
        show("UTF-16 (z BOM na początku)", text.getBytes(StandardCharsets.UTF_16).length);
        // WYNIK: UTF-16 (z BOM na początku) → 14

        // Kodowania jednobajtowe są krótkie, ale znają tylko ~256 znaków: polskie ogonki + alfabet łaciński.
        // Emoji czy chińskie znaki w nich nie istnieją. Co robi encoder, gdy znaku nie ma w kodowaniu?
        // Zamienia go na znak zastępczy — najczęściej '?'. To cicha utrata danych.
        String withEmoji = "ok " + grin();
        byte[] latin = withEmoji.getBytes(win1250);
        show("emoji zapisane w windows-1250 (hex)", hex(latin));
        // WYNIK: emoji zapisane w windows-1250 (hex) → 6f 6b 20 3f
        show("odczytane z powrotem", new String(latin, win1250));
        // WYNIK: odczytane z powrotem → ok ?
        // PUŁAPKA: String.getBytes(kodowanie) NIE zgłasza błędu dla znaku, którego kodowanie nie zna — wstawia '?'.
        //   Dane są stracone na zawsze, a program działa dalej bez słowa (Files.writeString jest ścisłe — sekcja 6). UTF-8 zna WSZYSTKIE znaki Unicode.

        byte[] utf16 = text.getBytes(StandardCharsets.UTF_16);
        show("UTF-16: dwa pierwsze bajty (BOM, big endian)", hex(new byte[] {utf16[0], utf16[1]}));
        // WYNIK: UTF-16: dwa pierwsze bajty (BOM, big endian) → fe ff
        // DOBRA PRAKTYKA: do plików, sieci i baz używaj UTF-8. Jest zgodne z ASCII, zajmuje mało miejsca
        //   dla tekstów łacińskich, nie ma problemu kolejności bajtów i obsługuje każdy znak.
    }

    // =================================================================================================
    // 3. TA SAMA LITERA, INNE BAJTY
    // =================================================================================================

    /**
     * 3. Zobaczmy bajty wprost. „ł” to w UTF-8 dwa bajty, a w ISO-8859-2 i windows-1250 jeden (B3).
     * Ale „ą” ma w tych dwóch starych kodowaniach RÓŻNE bajty (B1 kontra B9) — dlatego pomylenie ich
     * psuje tylko niektóre litery i błąd łatwo przeoczyć.
     */
    static void sameLetterDifferentBytes() {
        section("3. Ta sama litera — różne bajty");

        Charset iso2 = Charset.forName("ISO-8859-2");
        Charset win1250 = Charset.forName("windows-1250");

        show("ł w UTF-8", hex("ł", StandardCharsets.UTF_8));
        // WYNIK: ł w UTF-8 → c5 82
        show("ł w ISO-8859-2", hex("ł", iso2));
        // WYNIK: ł w ISO-8859-2 → b3
        show("ł w windows-1250", hex("ł", win1250));
        // WYNIK: ł w windows-1250 → b3
        show("ł w UTF-16BE", hex("ł", StandardCharsets.UTF_16BE));
        // WYNIK: ł w UTF-16BE → 01 42

        show("ą w ISO-8859-2", hex("ą", iso2));
        // WYNIK: ą w ISO-8859-2 → b1
        show("ą w windows-1250", hex("ą", win1250));
        // WYNIK: ą w windows-1250 → b9
        show("Ś w ISO-8859-2", hex("Ś", iso2));
        // WYNIK: Ś w ISO-8859-2 → a6
        show("Ś w windows-1250", hex("Ś", win1250));
        // WYNIK: Ś w windows-1250 → 8c

        // Bajty z ISO-8859-2 odczytane jako windows-1250: "ą" (B1) staje się znakiem ±, a nie ą.
        String pomylone = new String("ą".getBytes(iso2), win1250);
        show("ISO-8859-2 odczytane jako windows-1250", pomylone + " (" + cp(pomylone.charAt(0)) + ")");
        // WYNIK: ISO-8859-2 odczytane jako windows-1250 → ± (U+00B1)

        // Skąd tyle kodowań? Historia: ISO-8859-2 (Unix, internet lat 90.), windows-1250 (Windows),
        // CP852 (DOS, polska konsola Windows), Mazovia i inne. Dziś wszystko zastępuje UTF-8.
        // (To jak z książkami kodowymi z analogii na górze: te same numery, inne litery.)
    }

    // =================================================================================================
    // 4. KRZACZKI (MOJIBAKE)
    // =================================================================================================

    /**
     * 4. Mojibake (japońskie „zepsute znaki”) powstaje, gdy bajty zapisane w jednym kodowaniu
     * odczytamy w innym. Najczęstszy polski przypadek: plik w UTF-8 otwarty jako windows-1250.
     * Dane NIE są zniszczone — bajty są nienaruszone, tylko źle zinterpretowane. Dlatego często da się je naprawić.
     */
    static void mojibake() {
        section("4. Krzaczki (mojibake)");

        Charset win1250 = Charset.forName("windows-1250");
        String original = "zażółć gęślą jaźń";
        byte[] utf8 = original.getBytes(StandardCharsets.UTF_8);

        // Czytający zakłada windows-1250, a piszący użył UTF-8:
        String krzaczki = new String(utf8, win1250);
        show("UTF-8 odczytane jako windows-1250", krzaczki);
        // WYNIK: UTF-8 odczytane jako windows-1250 → zaĹĽĂłĹ‚Ä‡ gÄ™Ĺ›lÄ… jaĹşĹ„
        show("liczba znaków oryginału / krzaczków", original.length() + " / " + krzaczki.length());
        // WYNIK: liczba znaków oryginału / krzaczków → 17 / 26
        // Każda polska litera (2 bajty UTF-8) rozpadła się na DWA znaki — stąd tekst jest dłuższy.

        // Naprawa: cofamy błędne dekodowanie — zamieniamy krzaczki na bajty TYM SAMYM kodowaniem,
        // którym je błędnie odczytano, i czytamy poprawnym.
        String naprawione = new String(krzaczki.getBytes(win1250), StandardCharsets.UTF_8);
        show("naprawa (bajty z powrotem, UTF-8)", naprawione);
        // WYNIK: naprawa (bajty z powrotem, UTF-8) → zażółć gęślą jaźń
        show("naprawione == oryginał", naprawione.equals(original));
        // WYNIK: naprawione == oryginał → true

        // Odwrotny przypadek: plik w windows-1250 odczytany jako UTF-8. Bajty polskich liter (np. BF, F3, B3, E6)
        // nie tworzą poprawnych sekwencji UTF-8, więc dekoder wstawia U+FFFD (znak zastępczy, czarny romb z "?").
        byte[] win = "zażółć".getBytes(win1250);
        String odwrotnie = new String(win, StandardCharsets.UTF_8);
        show("windows-1250 odczytane jako UTF-8 — kody znaków", kodyZnakow(odwrotnie));
        // WYNIK: windows-1250 odczytane jako UTF-8 — kody znaków → U+007A U+0061 U+FFFD U+FFFD U+FFFD
        // Tu naprawy NIE ma: bajty zastąpiono jednym znakiem zastępczym i informacja przepadła.

        // PUŁAPKA: naprawa „bajty z powrotem” działa tylko wtedy, gdy błędne dekodowanie niczego nie zgubiło.
        //   Dekodowanie UTF-8 jako windows-1250 jest odwracalne, o ile żaden bajt nie wpadł w „dziurę” tabeli
        //   (windows-1250 nie ma znaków dla bajtów 81, 83, 88, 90 i 98). Dekodowanie odwrotne — nigdy.
        // DOBRA PRAKTYKA: nie naprawiaj krzaczków w locie na ślepo — znajdź miejsce, w którym podano złe kodowanie
        //   (otwarcie pliku, nagłówek HTTP, ustawienie edytora), i popraw je u źródła.
    }

    /** kodyZnakow = lista punktów kodowych tekstu, np. "U+007A U+0061". */
    private static String kodyZnakow(String text) {
        return text.codePoints().mapToObj(Io12Charsets::cp).collect(Collectors.joining(" "));
        // codePoints = punkty kodowe (strumień int); mapToObj = przekształć na obiekty; joining = sklej
    }

    // =================================================================================================
    // 5. BŁĘDNE BAJTY: ZASTĄPIENIE CZY WYJĄTEK
    // =================================================================================================

    /**
     * 5. Co robi dekoder, gdy bajty nie tworzą poprawnego tekstu? Zależy od API:
     * new String(bajty, kodowanie) po cichu wstawia U+FFFD (REPLACE), natomiast CharsetDecoder można
     * ustawić na CodingErrorAction.REPORT (zgłoś) — wtedy dostajemy MalformedInputException (źle uformowane wejście).
     */
    static void malformedBytes() {
        section("5. Błędne bajty — zastąpienie czy wyjątek");

        Charset win1250 = Charset.forName("windows-1250");
        byte[] zle = "żółw".getBytes(win1250); // bajty windows-1250 — dla UTF-8 to nie jest poprawny tekst

        // (a) new String — ciche zastąpienie
        String cicho = new String(zle, StandardCharsets.UTF_8);
        show("new String(bajty, UTF_8) zawiera U+FFFD", cicho.indexOf('�') >= 0);
        // WYNIK: new String(bajty, UTF_8) zawiera U+FFFD → true
        show("liczba znaków zastępczych", cicho.chars().filter(c -> c == 0xFFFD).count());
        // WYNIK: liczba znaków zastępczych → 2

        // (b) CharsetDecoder z REPORT — wyjątek. newDecoder = nowy dekoder; onMalformedInput = gdy źle uformowane;
        //     onUnmappableCharacter = gdy nie da się odwzorować znaku; decode = zdekoduj.
        CharsetDecoder strict = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            strict.decode(ByteBuffer.wrap(zle));
            System.out.println("✘ strict.decode → NIE rzucono wyjątku");
        } catch (CharacterCodingException e) { // to jest IOException (sprawdzany!)
            System.out.println("✔ strict.decode → rzucono " + e.getClass().getSimpleName()
                    + " (długość błędnego fragmentu: " + ((MalformedInputException) e).getInputLength() + ")");
            // WYNIK: ✔ strict.decode → rzucono MalformedInputException (długość błędnego fragmentu: 1)
        }

        // Poprawny UTF-8 przechodzi bez problemu, a wynik to zwykły String:
        try {
            String ok = strict.decode(ByteBuffer.wrap("żółw".getBytes(StandardCharsets.UTF_8))).toString();
            show("poprawny UTF-8 zdekodowany strict", ok);
            // WYNIK: poprawny UTF-8 zdekodowany strict → żółw
        } catch (CharacterCodingException e) {
            throw new IllegalStateException(e);
        }

        // Test „czy to poprawny UTF-8?” wykorzystuje właśnie ten mechanizm:
        show("czy bajty windows-1250 to poprawny UTF-8", isValidUtf8(zle));
        // WYNIK: czy bajty windows-1250 to poprawny UTF-8 → false
        show("czy bajty UTF-8 to poprawny UTF-8", isValidUtf8("żółw".getBytes(StandardCharsets.UTF_8)));
        // WYNIK: czy bajty UTF-8 to poprawny UTF-8 → true
        show("czy czysty ASCII to poprawny UTF-8", isValidUtf8("abc".getBytes(StandardCharsets.US_ASCII)));
        // WYNIK: czy czysty ASCII to poprawny UTF-8 → true

        // PUŁAPKA: plik w windows-1250 NIE zawsze „wyda się” błędny jako UTF-8: gdy zawiera same litery
        //   bez ogonków, jest identyczny z UTF-8 i nikt nie zauważy problemu do pierwszego „ł”.
        // PUŁAPKA: (odwrotnie) odczyt dowolnych bajtów jako windows-1250 czy ISO-8859-1 NIGDY nie zgłasza błędu
        //   (prawie każdy bajt coś znaczy) — dostajesz krzaczki, nie wyjątek. Błąd daje tylko ścisły UTF-8.
        // DOBRA PRAKTYKA: dane od zewnątrz (pliki użytkownika) czytaj ściśle (REPORT) i pokaż użytkownikowi
        //   „plik nie jest w UTF-8”, zamiast po cichu zepsuć jego dane znakami zastępczymi.
    }

    /** isValidUtf8 = czy bajty są poprawnym UTF-8 (ścisły dekoder zgłasza błąd przy pierwszym problemie). */
    static boolean isValidUtf8(byte[] bytes) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            decoder.decode(ByteBuffer.wrap(bytes)); // wynik nas nie interesuje, liczy się brak wyjątku
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    // =================================================================================================
    // 6. BŁĘDNE BAJTY W PLIKACH
    // =================================================================================================

    /**
     * 6. Różne sposoby czytania pliku mają RÓŻNE zachowanie przy błędnych bajtach.
     * Files.readString, Files.readAllLines i Files.newBufferedReader są ścisłe (wyjątek),
     * a InputStreamReader i FileReader zastępują błędne bajty po cichu.
     */
    static void malformedInFiles(Path dir) throws IOException {
        section("6. Błędne bajty w plikach — kto zgłasza, kto zamiata pod dywan");
        Files.createDirectories(dir);

        Charset win1250 = Charset.forName("windows-1250");
        Path plik = dir.resolve("stary.txt");
        Files.write(plik, "zażółć gęślą jaźń\n".getBytes(win1250)); // plik zapisany „po staremu”, w windows-1250

        // ścisłe API — wyjątek MalformedInputException (to podklasa CharacterCodingException i IOException)
        ioFails("Files.readString(plik, UTF_8)", () -> Files.readString(plik, StandardCharsets.UTF_8));
        // WYNIK: ✔ Files.readString(plik, UTF_8) → rzucono MalformedInputException
        ioFails("Files.readAllLines(plik, UTF_8)", () -> Files.readAllLines(plik, StandardCharsets.UTF_8));
        // WYNIK: ✔ Files.readAllLines(plik, UTF_8) → rzucono MalformedInputException
        ioFails("newBufferedReader(...).readLine()", () -> {
            try (BufferedReader reader = Files.newBufferedReader(plik, StandardCharsets.UTF_8)) {
                reader.readLine(); // wyjątek pojawia się dopiero przy czytaniu, nie przy otwarciu
            }
        });
        // WYNIK: ✔ newBufferedReader(...).readLine() → rzucono MalformedInputException

        // Poprawny odczyt: podajemy kodowanie, w którym plik naprawdę zapisano.
        show("Files.readString(plik, windows-1250)", Files.readString(plik, win1250).trim());
        // WYNIK: Files.readString(plik, windows-1250) → zażółć gęślą jaźń

        // Ciche zastąpienie: InputStreamReader (most bajty → znaki). Nigdy nie rzuci wyjątku kodowania.
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(plik), StandardCharsets.UTF_8)) {
            char[] bufor = new char[100];                      // bufor = tablica na przeczytane znaki
            int ile = reader.read(bufor);                      // read zwraca, ILE znaków naprawdę wczytał
            String przeczytane = new String(bufor, 0, ile);
            show("InputStreamReader (UTF_8) — zawiera U+FFFD", przeczytane.indexOf('�') >= 0);
            // WYNIK: InputStreamReader (UTF_8) — zawiera U+FFFD → true
        }
        // PUŁAPKA: new InputStreamReader(strumień, UTF_8) oraz new FileReader(plik, UTF_8) używają dekodera
        //   w trybie REPLACE, więc zepsuty plik „wczyta się”, tylko z dziurami w tekście. Files.newBufferedReader
        //   z tymi samymi argumentami jest ścisły. Wniosek: Files.* to pierwszy wybór, także ze względu na to.

        // Zapis ma podobny podział. Ścisłe: Files.writeString, Files.write i Files.newBufferedWriter — gdy tekstu
        // nie da się zapisać w danym kodowaniu, rzucają UnmappableCharacterException (znak bez odwzorowania).
        // Ciche: String.getBytes oraz new OutputStreamWriter/FileWriter — wstawiają '?' (patrz sekcja 2).
        Path wynik = dir.resolve("emoji.txt");
        ioFails("Files.writeString(emoji, windows-1250)", () -> Files.writeString(wynik, "ok " + grin(), win1250));
        // WYNIK: ✔ Files.writeString(emoji, windows-1250) → rzucono UnmappableCharacterException
        // DOBRA PRAKTYKA: czytasz — podaj kodowanie, w którym plik zapisano. Piszesz — zawsze UTF-8.
    }

    // =================================================================================================
    // 7. EMOJI I PUNKTY KODOWE
    // =================================================================================================

    /**
     * 7. Emoji leży poza „podstawową płaszczyzną” Unicode (powyżej U+FFFF), więc w Stringu zajmuje DWA char
     * (parę zastępczą). Stąd length() liczy je podwójnie. codePointCount liczy znaki Unicode naprawdę.
     */
    static void emoji() {
        section("7. Emoji, para zastępcza i punkty kodowe");

        // W kodzie źródłowym nie używamy escape'ów (zamieniają się na niewidoczne znaki) — budujemy emoji z numeru.
        String smile = grin();
        String text = "A" + smile + "ł";
        show("length() (jednostki char)", text.length());
        // WYNIK: length() (jednostki char) → 4
        show("codePointCount (znaki Unicode)", text.codePointCount(0, text.length()));
        // WYNIK: codePointCount (znaki Unicode) → 3
        show("bajty UTF-8", text.getBytes(StandardCharsets.UTF_8).length);
        // WYNIK: bajty UTF-8 → 7
        show("emoji w UTF-8 (hex)", hex(smile, StandardCharsets.UTF_8));
        // WYNIK: emoji w UTF-8 (hex) → f0 9f 98 80
        show("emoji w UTF-16BE (hex)", hex(smile, StandardCharsets.UTF_16BE));
        // WYNIK: emoji w UTF-16BE (hex) → d8 3d de 00

        // Para zastępcza: pierwszy char (wysoki, 0xD800–0xDBFF) i drugi (niski, 0xDC00–0xDFFF) — razem tworzą jeden znak.
        show("charAt(1) to część pary", Character.isHighSurrogate(text.charAt(1)));
        // WYNIK: charAt(1) to część pary → true
        show("charAt(1) numer", cp(text.charAt(1)));
        // WYNIK: charAt(1) numer → U+D83D
        show("codePointAt(1) numer", cp(text.codePointAt(1)));
        // WYNIK: codePointAt(1) numer → U+1F600
        show("Character.charCount(0x1F600)", Character.charCount(0x1F600)); // charCount = ile char zajmuje
        // WYNIK: Character.charCount(0x1F600) → 2
        show("kody znaków (codePoints)", kodyZnakow(text));
        // WYNIK: kody znaków (codePoints) → U+0041 U+1F600 U+0142

        // PUŁAPKA: pętla for po indeksach i charAt rozetnie emoji na dwie połówki (żaden z nich sam nie jest znakiem).
        //   Wypisanie samej połowy daje znak zastępczy lub krzaczek. substring(0, 2) w środku pary też.
        StringBuilder pojedyncze = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            pojedyncze.append(Character.isSurrogate(text.charAt(i)) ? "[połówka]" : String.valueOf(text.charAt(i)));
        }
        show("pętla po charAt", pojedyncze);
        // WYNIK: pętla po charAt → A[połówka][połówka]ł
        // DOBRA PRAKTYKA: gdy tekst może zawierać emoji lub rzadkie znaki, iteruj po punktach kodowych:
        //   text.codePoints() (strumień). StringBuilder.reverse() radzi sobie z parami poprawnie.
        show("odwrócony tekst", new StringBuilder(text).reverse().toString().codePoints().mapToObj(Io12Charsets::cp)
                .collect(Collectors.joining(" ")));
        // WYNIK: odwrócony tekst → U+0142 U+1F600 U+0041

        // Uwaga: nawet punkt kodowy to nie zawsze „znak widziany przez człowieka”. Flaga albo „e” z akcentem
        // zbudowane z kilku punktów kodowych to jeden obraz na ekranie (klaster grafemów) — patrz sekcja 11.
    }

    // =================================================================================================
    // 8. BOM
    // =================================================================================================

    /**
     * 8. BOM (byte order mark, U+FEFF) to znacznik na początku pliku. W UTF-16 mówi o kolejności bajtów.
     * W UTF-8 kolejność jest stała, ale Notatnik Windows i Excel dopisują BOM (bajty EF BB BF) „na wszelki wypadek”.
     * Java NIE usuwa BOM z UTF-8 — pojawia się jako dziwny niewidoczny pierwszy znak tekstu.
     */
    static void byteOrderMark(Path dir) throws IOException {
        section("8. BOM — niewidoczny znak na początku pliku");
        Files.createDirectories(dir);

        // Budujemy bajty BOM z liczb (w źródle nie wpisujemy niewidocznego znaku).
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = "id;nazwa\n1;Łódź\n".getBytes(StandardCharsets.UTF_8);
        byte[] zBom = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, zBom, 0, bom.length);          // arraycopy = kopiuj tablicę
        System.arraycopy(body, 0, zBom, bom.length, body.length);
        Path plik = dir.resolve("notatnik.csv");
        Files.write(plik, zBom);

        String tekst = Files.readString(plik, StandardCharsets.UTF_8);
        show("pierwszy znak (kod)", cp(tekst.charAt(0)));
        // WYNIK: pierwszy znak (kod) → U+FEFF
        show("długość tekstu (o 1 za dużo)", tekst.length() + " zamiast " + new String(body, StandardCharsets.UTF_8).length());
        // WYNIK: długość tekstu (o 1 za dużo) → 17 zamiast 16
        show("startsWith(\"id\")", tekst.startsWith("id"));
        // WYNIK: startsWith("id") → false
        String naglowek = tekst.lines().findFirst().orElse(""); // lines = linie (Java 11+); findFirst = pierwszy
        show("nagłówek == \"id;nazwa\"", naglowek.equals("id;nazwa"));
        // WYNIK: nagłówek == "id;nazwa" → false
        show("kolumna 1 po split(\";\")", naglowek.split(";")[0].length() + " znaki zamiast 2");
        // WYNIK: kolumna 1 po split(";") → 3 znaki zamiast 2
        // To klasyczny błąd: „kolumny 'id' nie ma w pliku”, choć ewidentnie jest — w nazwie siedzi niewidoczny BOM.

        // Wykrywanie i usuwanie BOM — na bajtach (przed dekodowaniem) albo na tekście (pierwszy znak U+FEFF).
        show("hasUtf8Bom(bajty)", hasUtf8Bom(zBom));
        // WYNIK: hasUtf8Bom(bajty) → true
        show("hasUtf8Bom(bez BOM)", hasUtf8Bom(body));
        // WYNIK: hasUtf8Bom(bez BOM) → false
        String czysty = stripBom(tekst);
        show("po stripBom: startsWith(\"id\")", czysty.startsWith("id"));
        // WYNIK: po stripBom: startsWith("id") → true
        show("stripBom na tekście bez BOM", stripBom("abc"));
        // WYNIK: stripBom na tekście bez BOM → abc

        // UTF-16: BOM jest potrzebny i Java go obsługuje. "UTF-16" przy ZAPISIE dodaje BOM (big endian),
        // a przy ODCZYCIE sam go rozpoznaje i NIE zostawia w tekście. "UTF-16LE"/"UTF-16BE" nie znają BOM:
        // przy odczycie zostawią go jako znak U+FEFF.
        byte[] u16 = "Hej".getBytes(StandardCharsets.UTF_16);
        show("UTF-16 'Hej' (hex)", hex(u16));
        // WYNIK: UTF-16 'Hej' (hex) → fe ff 00 48 00 65 00 6a
        show("odczyt jako UTF-16", new String(u16, StandardCharsets.UTF_16));
        // WYNIK: odczyt jako UTF-16 → Hej
        show("odczyt jako UTF-16BE (kody)", kodyZnakow(new String(u16, StandardCharsets.UTF_16BE)));
        // WYNIK: odczyt jako UTF-16BE (kody) → U+FEFF U+0048 U+0065 U+006A

        // PUŁAPKA: Java (klasa UTF-8) nigdy nie usuwa BOM sama — to znana, utrzymywana od lat decyzja. Pliki z
        //   Notatnika/Excela (CSV, JSON, .properties) trzeba oczyścić samemu albo użyć biblioteki.
        // DOBRA PRAKTYKA: sam zapisuj UTF-8 BEZ BOM (tak robią Files.writeString i IntelliJ).
        //   Excel bez BOM zwykle czyta CSV w kodowaniu systemowym (na polskim Windows: windows-1250) — wtedy wygodniej dać użytkownikowi
        //   osobny eksport „CSV dla Excela” z BOM (świadomie, nie przez przypadek).
    }

    /** hasUtf8Bom = czy bajty zaczynają się od EF BB BF. */
    static boolean hasUtf8Bom(byte[] bytes) {
        return bytes.length >= 3
                && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF;
    }

    /** stripBom = usuń z początku tekstu znak BOM (U+FEFF), jeśli jest. (char) 0xFEFF = znak o tym numerze. */
    static String stripBom(String text) {
        return !text.isEmpty() && text.charAt(0) == (char) 0xFEFF ? text.substring(1) : text;
    }

    // =================================================================================================
    // 9. KODOWANIE DOMYŚLNE
    // =================================================================================================

    /**
     * 9. Co się dzieje, gdy kodowania NIE podamy? Stare API (new String(bajty), getBytes(), FileReader, FileWriter,
     * PrintWriter(String), Scanner(File)) używa kodowania domyślnego systemu. W Javie 17 to np. windows-1250
     * na polskim Windows i UTF-8 na Linuksie — ten sam program daje różne pliki. Od Javy 18 domyślne jest UTF-8 (JEP 400).
     * API NIO.2 (Files.readString, readAllLines, writeString, newBufferedReader...) zawsze domyślnie używa UTF-8.
     */
    static void defaultCharset(Path dir) throws IOException {
        section("9. Kodowanie domyślne, file.encoding i konsola");
        Files.createDirectories(dir);

        // Kodowania domyślnego NIE wypisujemy: u Ciebie i na innym komputerze wynik byłby inny (to cały problem).
        // Zamiast tego pokazujemy, że Files.* ma STAŁE, UTF-8 — niezależne od komputera:
        Path plik = dir.resolve("domyslnie.txt");
        Files.writeString(plik, "zażółć\n"); // bez parametru kodowania: zawsze UTF-8
        show("rozmiar pliku (bajty UTF-8)", Files.size(plik));
        // WYNIK: rozmiar pliku (bajty UTF-8) → 11
        show("Files.readString bez kodowania", Files.readString(plik).trim());
        // WYNIK: Files.readString bez kodowania → zażółć
        byte[] surowe = Files.readAllBytes(plik);
        show("pierwsze bajty pliku", hex(java.util.Arrays.copyOf(surowe, 5)));
        // WYNIK: pierwsze bajty pliku → 7a 61 c5 bc c3
        // (UTF-8: "z" "a" "ż"=c5 bc "ó"=c3 b3 ...). Ten sam wynik na Windows i Linuksie.

        // PRZED (Java 17, kodowanie zależne od systemu):
        //     String s = new String(bytes);                    // domyślne kodowanie systemu!
        //     Writer w = new FileWriter("plik.txt");           // domyślne kodowanie systemu!
        //     Reader r = new FileReader("plik.txt");           // domyślne kodowanie systemu!
        // PO (zawsze jawnie):
        //     String s = new String(bytes, StandardCharsets.UTF_8);
        //     Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
        //     Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8);

        // PUŁAPKA: kod bez jawnego kodowania działa u Ciebie (Windows, windows-1250 — zapis i odczyt zgodne ze sobą),
        //   a po przeniesieniu pliku na serwer z Linuksem (UTF-8) polskie litery się psują.
        //   Albo odwrotnie: plik z serwera psuje się u Ciebie. Błąd widać dopiero „gdzie indziej”.
        // JEP 400 (Java 18+): domyślne kodowanie to UTF-8 niezależnie od systemu; stary tryb przywraca
        //   -Dfile.encoding=COMPAT. Uczysz się na Javie 17, ale kod pisz tak, by działał w obu światach.

        // file.encoding — właściwość systemowa (-Dfile.encoding=UTF-8) ustawiająca kodowanie domyślne JVM.
        // native.encoding (Java 17+) — kodowanie systemu operacyjnego, tylko do odczytu.
        // To NIE jest to samo co kodowanie KONSOLI: znaki z System.out.println przechodzą jeszcze przez
        // kodowanie wyjścia. Windows w konsoli (cmd/PowerShell) używa swojej strony kodowej (np. CP852 lub 1250), więc
        // litery „ł” czy „ż” mogą wyglądać źle nawet wtedy, gdy program jest poprawny. IntelliJ ma własną konsolę
        // i własne ustawienia: Settings → Editor → File Encodings (kodowanie plików projektu: ustaw UTF-8)
        // oraz opcje VM kodowania uruchomienia. Kodowanie pliku ŹRÓDŁOWEGO (.java) to jeszcze osobna sprawa:
        // javac czyta źródła kodowaniem z opcji -encoding (domyślnie systemowym w Javie 17!). Dlatego Maven ma
        // <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding> w pom.xml.
        // DOBRA PRAKTYKA: wszędzie UTF-8: pliki źródłowe, pliki danych, IntelliJ, pom.xml, bazy, nagłówki HTTP
        //   (Content-Type: text/plain; charset=UTF-8).
    }

    // =================================================================================================
    // 10. NAZWY KODOWAŃ
    // =================================================================================================

    /**
     * 10. Kodowanie ma nazwę kanoniczną i aliasy. StandardCharsets daje gwarantowane stałe (UTF_8, ISO_8859_1,
     * US_ASCII, UTF_16...) — nie trzeba łapać wyjątków. Charset.forName("nazwa") sprawdza nazwę w czasie działania.
     */
    static void charsetNames() {
        section("10. Nazwy kodowań i aliasy");

        show("StandardCharsets.UTF_8.name()", StandardCharsets.UTF_8.name());
        // WYNIK: StandardCharsets.UTF_8.name() → UTF-8
        show("forName(\"utf8\")", Charset.forName("utf8").name());
        // WYNIK: forName("utf8") → UTF-8
        show("forName(\"cp1250\")", Charset.forName("cp1250").name());
        // WYNIK: forName("cp1250") → windows-1250
        show("forName(\"latin2\")", Charset.forName("latin2").name());
        // WYNIK: forName("latin2") → ISO-8859-2
        show("forName(\"Windows-1250\") (wielkość liter bez znaczenia)", Charset.forName("Windows-1250").name());
        // WYNIK: forName("Windows-1250") (wielkość liter bez znaczenia) → windows-1250

        show("isSupported(\"windows-1250\")", Charset.isSupported("windows-1250"));
        // WYNIK: isSupported("windows-1250") → true
        show("isSupported(\"klingon\")", Charset.isSupported("klingon"));
        // WYNIK: isSupported("klingon") → false
        show("availableCharsets zawiera UTF-8", Charset.availableCharsets().containsKey("UTF-8"));
        // WYNIK: availableCharsets zawiera UTF-8 → true
        show("availableCharsets zawiera ISO-8859-2", Charset.availableCharsets().containsKey("ISO-8859-2"));
        // WYNIK: availableCharsets zawiera ISO-8859-2 → true
        // Liczby ani kolejności dostępnych kodowań nie wypisujemy: zależą od wersji i dystrybucji JDK
        // (kodowania egzotyczne są w module jdk.charsets).

        // Nieznana nazwa → wyjątek NIESPRAWDZANY (RuntimeException): UnsupportedCharsetException (nieobsługiwane kodowanie)
        // albo IllegalCharsetNameException (niedozwolone znaki w nazwie). Nie IOException!
        expectThrows("forName(\"klingon\")", () -> Charset.forName("klingon"));
        // WYNIK: ✔ forName("klingon") → rzucono UnsupportedCharsetException: klingon
        expectThrows("forName(\"zły nazwa!\")", () -> Charset.forName("zły nazwa!"));
        // WYNIK: ✔ forName("zły nazwa!") → rzucono IllegalCharsetNameException: zły nazwa!

        // DOBRA PRAKTYKA: dla UTF-8, ISO-8859-1, US-ASCII i UTF-16 używaj stałych StandardCharsets.* (nie ma
        //   wyjątku, nie ma literówki w nazwie). Charset.forName("windows-1250") tylko dla kodowań spoza listy stałych.
        //   Nazwy typu "UTF8" (bez myślnika) działają dzięki aliasom, ale oficjalna nazwa to "UTF-8".
        // Metody z nazwą kodowania w Stringu (new String(bytes, "UTF-8"), getBytes("UTF-8")) rzucają
        // UnsupportedEncodingException (sprawdzany) — wersje z obiektem Charset są wygodniejsze.
    }

    // =================================================================================================
    // 11. NORMALIZACJA UNICODE
    // =================================================================================================

    /**
     * 11. Jedna litera może mieć DWA zapisy w Unicode: gotowy znak „ó” (U+00F3, forma NFC — złożona)
     * albo „o” plus osobny akcent U+0301 (forma NFD — rozłożona). Na ekranie wyglądają identycznie,
     * ale equals() mówi „różne”. Takie teksty zdarzają się np. w nazwach plików z macOS.
     * Normalizer.normalize ujednolica zapis.
     */
    static void normalization() {
        section("11. Normalizacja Unicode (NFC i NFD)");

        String zlozone = "ó";                       // jeden punkt kodowy U+00F3
        String rozlozone = "o" + (char) 0x0301;     // 'o' + łączący akcent ostry (U+0301)
        show("zlozone: punkty kodowe", kodyZnakow(zlozone));
        // WYNIK: zlozone: punkty kodowe → U+00F3
        show("rozlozone: punkty kodowe", kodyZnakow(rozlozone));
        // WYNIK: rozlozone: punkty kodowe → U+006F U+0301
        show("equals", zlozone.equals(rozlozone));
        // WYNIK: equals → false
        show("length obu", zlozone.length() + " i " + rozlozone.length());
        // WYNIK: length obu → 1 i 2

        String nfc = Normalizer.normalize(rozlozone, Normalizer.Form.NFC); // Form = postać; NFC = złóż
        String nfd = Normalizer.normalize(zlozone, Normalizer.Form.NFD);   // NFD = rozłóż
        show("po NFC equals złożony", nfc.equals(zlozone));
        // WYNIK: po NFC equals złożony → true
        show("po NFD equals rozłożony", nfd.equals(rozlozone));
        // WYNIK: po NFD equals rozłożony → true

        // Polskie ł NIE ma rozkładu (to osobna litera, nie „l” z kreską), więc NFD zostawia ją w spokoju.
        // A ą, ę, ć, ń, ś, ź, ż rozkładają się (litera + ogonek/kreska/kropka).
        show("ł po NFD", kodyZnakow(Normalizer.normalize("ł", Normalizer.Form.NFD)));
        // WYNIK: ł po NFD → U+0142
        show("ą po NFD", kodyZnakow(Normalizer.normalize("ą", Normalizer.Form.NFD)));
        // WYNIK: ą po NFD → U+0061 U+0328

        // Znana sztuczka: usuwanie ogonków („zażółć” → „zazolc”): rozłóż, wyrzuć znaki łączące.
        // Uwaga: „ł” trzeba zamienić osobno, bo nie ma rozkładu.
        String bezOgonkow = Normalizer.normalize("zażółć gęślą", Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")  // \p{M} = znaki łączące (ang. Mark)
                .replace('ł', 'l');
        show("bez ogonków", bezOgonkow);
        // WYNIK: bez ogonków → zazolc gesla

        // PUŁAPKA: porównywanie tekstów z różnych źródeł (nazwy plików z macOS, tekst ze schowka, bazy danych)
        //   bez normalizacji daje „nie znaleziono”, choć tekst wygląda tak samo. Normalizuj do NFC na wejściu.
        // DOBRA PRAKTYKA: do porównań i kluczy wyszukiwania ujednolicaj formę (zwykle NFC) przed equals i przed zapisem.
        //   Dodatkowo: tekst posortowany przez String.compareTo porównuje punkty kodowe, nie polski alfabet —
        //   do polskiego sortowania służy java.text.Collator z Locale pl-PL (t04_strings).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Plik/sieć = bajty. Tekst = znaki. Kodowanie (charset) zamienia jedno w drugie. PODAWAJ JE ZAWSZE.
     *   • UTF-8: ASCII 1 bajt, polskie litery 2, emoji 4. Domyślne i zalecane do wszystkiego.
     *   • String.length() = liczba jednostek char (UTF-16), nie bajtów i nie zawsze znaków; codePointCount = znaki.
     *   • Krzaczki = bajty odczytane w złym kodowaniu (UTF-8 jako windows-1250 → "Ĺ‚", "Ä‡").
     *   • new String(bajty, cs) i InputStreamReader zastępują błędne bajty U+FFFD po cichu; Files.readString,
     *     readAllLines i newBufferedReader rzucają MalformedInputException; CharsetDecoder.REPORT też.
     *   • BOM (EF BB BF) z Notatnika/Excela: Java go nie usuwa → niewidoczny znak U+FEFF w pierwszym polu.
     *   • "UTF-16" dopisuje BOM przy zapisie i rozpoznaje przy odczycie; UTF-16LE/BE nie znają BOM.
     *   • Java 17: kodowanie domyślne zależy od systemu; Java 18+: UTF-8 (JEP 400). Files.* zawsze UTF-8.
     *   • Normalizer: ó może mieć 1 lub 2 punkty kodowe; do porównań normalizuj do NFC.
     *   • StandardCharsets.* zamiast Charset.forName("...") tam, gdzie się da.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego "zażółć".length() to 6, a getBytes(UTF_8).length to 10?
     *   2. Co się stanie z emoji, gdy zapiszesz je przez getBytes(windows-1250), a potem odczytasz?
     *   3. Co wypisze:  System.out.println("łódź".getBytes(StandardCharsets.UTF_8).length);  ?
     *   4. Co wypisze (grin = tekst z jednym emoji U+1F600):  System.out.println(grin.length() + " " + grin.codePointCount(0, grin.length()));  ?
     *   5. ZNAJDŹ BŁĄD:  Reader r = new FileReader("dane.txt");  — plik zapisano na Windows, program uruchomiono na Linuksie.
     *   6. ZNAJDŹ BŁĄD:  String naglowek = Files.readString(plik, UTF_8).lines().findFirst().get();
     *      if (naglowek.startsWith("id")) ...  — plik pochodzi z Notatnika i nagłówek to "id;nazwa". Dlaczego warunek bywa fałszywy?
     *   7. Czym różni się Files.newBufferedReader(p, UTF_8) od new InputStreamReader(Files.newInputStream(p), UTF_8)
     *      przy błędnych bajtach?
     *   8. Co wypisze:  "o" + "́" równe "ó"? (porównanie equals dla zapisu rozłożonego i złożonego, bez normalizacji)
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
        Path dir = TempDir.create("io12cw");
        try {
            String grin = grin();
            Check.equal("ćw. 1: rozmiar w UTF-8", 15,
                    () -> reference ? solution1("zażółć " + grin) : exercise1("zażółć " + grin));

            Path z = dir.resolve("z-bom.txt");
            Path bez = dir.resolve("bez-bom.txt");
            Files.write(z, concat(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, "Gżegżółka\n".getBytes(StandardCharsets.UTF_8)));
            Files.write(bez, "Gżegżółka\n".getBytes(StandardCharsets.UTF_8));
            Check.equal("ćw. 2: czytanie bez BOM", "Gżegżółka\n|Gżegżółka\n", io(() ->
                    (reference ? solution2(z) : exercise2(z)) + "|" + (reference ? solution2(bez) : exercise2(bez))));

            Path stary = dir.resolve("stary.txt");
            Files.write(stary, "Zażółć gęślą jaźń".getBytes(Charset.forName("windows-1250")));
            Check.equal("ćw. 3: czytanie pliku windows-1250", "Zażółć gęślą jaźń", io(() ->
                    reference ? solution3(stary) : exercise3(stary)));

            Check.equal("ćw. 4: zgadywanie kodowania",
                    "UTF-8 z BOM|UTF-8|windows-1250",
                    () -> {
                        byte[] bomOnly = concat(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, "abc".getBytes(StandardCharsets.UTF_8));
                        byte[] utf8 = "zażółć".getBytes(StandardCharsets.UTF_8);
                        byte[] win = "zażółć".getBytes(Charset.forName("windows-1250"));
                        return (reference ? solution4(bomOnly) : exercise4(bomOnly)) + "|"
                                + (reference ? solution4(utf8) : exercise4(utf8)) + "|"
                                + (reference ? solution4(win) : exercise4(win));
                    });

            Check.equal("ćw. 5: punkty kodowe", "U+0061 U+1F600 U+0142",
                    () -> reference ? solution5("a" + grin + "ł") : exercise5("a" + grin + "ł"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            TempDir.deleteRecursively(dir);
        }
        Check.summary();
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć, ile bajtów zajmie tekst zapisany w UTF-8.
     * Podpowiedź: text.getBytes(StandardCharsets.UTF_8).length (a nie text.length()!).
     */
    static int exercise1(String text) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (łatwe): przeczytaj plik tekstowy w UTF-8 i zwróć jego treść BEZ ewentualnego BOM na początku.
     * Podpowiedź: Files.readString(plik, UTF_8), potem usuń pierwszy znak, jeśli to (char) 0xFEFF.
     */
    static String exercise2(Path file) throws IOException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ na wersję z jawnym kodowaniem. Plik jest zapisany w windows-1250.
     * <pre>{@code
     * // PRZED: kodowanie zależne od systemu — na Linuksie psuje polskie litery
     * String text = new String(Files.readAllBytes(file));
     * }</pre>
     * Podpowiedź: Files.readString(file, Charset.forName("windows-1250")).
     */
    static String exercise3(Path file) throws IOException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (średnie): zgadnij kodowanie bajtów. Zwróć "UTF-8 z BOM", gdy bajty zaczynają się od EF BB BF,
     * "UTF-8", gdy są poprawnym UTF-8 (bez BOM), a w pozostałych przypadkach "windows-1250".
     * Podpowiedź: sprawdź BOM, potem ścisły dekoder (CharsetDecoder z CodingErrorAction.REPORT).
     */
    static String exercise4(byte[] data) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): zwróć punkty kodowe tekstu jako "U+0061 U+1F600 U+0142" (4 do 6 cyfr szesnastkowych
     * wielkimi literami, minimum 4, oddzielone spacją). Emoji ma być jednym punktem kodowym, nie dwoma połówkami.
     * Podpowiedź: text.codePoints(), String.format("U+%04X", cp) i Collectors.joining(" ").
     */
    static String exercise5(String text) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(String text) {
        return text.getBytes(StandardCharsets.UTF_8).length;
    }

    static String solution2(Path file) throws IOException {
        return stripBom(Files.readString(file, StandardCharsets.UTF_8));
    }

    static String solution3(Path file) throws IOException {
        return Files.readString(file, Charset.forName("windows-1250"));
    }

    static String solution4(byte[] data) {
        if (hasUtf8Bom(data)) {
            return "UTF-8 z BOM";
        }
        return isValidUtf8(data) ? "UTF-8" : "windows-1250";
    }

    static String solution5(String text) {
        List<String> codes = new ArrayList<>();
        text.codePoints().forEach(c -> codes.add(cp(c)));
        return String.join(" ", codes);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. length() liczy jednostki char (tu 6 liter). Bajty zależą od kodowania: w UTF-8 litery ż, ó, ł, ć mają po
     *      2 bajty, a z, a po 1 → 2*1 + 4*2 = 10.
     *   2. Windows-1250 nie ma emoji, więc encoder wstawia '?' — po odczycie zostaje "?" i nie da się tego odzyskać.
     *   3. 7 (ł, ó, ź po 2 bajty, d jeden bajt: 2 + 2 + 1 + 2).
     *   4. "2 1": para zastępcza to dwa char, ale jeden punkt kodowy.
     *   5. FileReader bez kodowania używa domyślnego kodowania systemu (Java 17): Windows → windows-1250, Linux → UTF-8,
     *      więc polskie litery się psują. Poprawnie: Files.newBufferedReader(path, StandardCharsets.UTF_8) z kodowaniem
     *      zgodnym z tym, w jakim plik zapisano.
     *   6. Plik z Notatnika zaczyna się od BOM (U+FEFF), którego Java nie usuwa; "nagłówek" to "id;nazwa",
     *      więc startsWith("id") jest false. Trzeba wywołać stripBom albo sprawdzić BOM w bajtach.
     *   7. newBufferedReader jest ścisły: błędne bajty → MalformedInputException. InputStreamReader zastępuje je
     *      po cichu znakiem U+FFFD i czytanie trwa dalej.
     *   8. false: "o" + U+0301 ma dwa punkty kodowe, "ó" jeden. Dopiero po Normalizer.normalize(..., NFC) są równe.
     */
    // </editor-fold>
}
