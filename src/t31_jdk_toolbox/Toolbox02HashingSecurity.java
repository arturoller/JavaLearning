package t31_jdk_toolbox;

import helpers.Check;
import helpers.TempDir;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Skróty, hasła, losowość i HMAC — podstawy bezpieczeństwa w JDK
 *        (hash = skrót; salt = sól; token = żeton; integrity = nienaruszalność danych)
 *
 * W SKRÓCIE:
 *   Funkcja skrótu zamienia dane dowolnej długości na krótki "odcisk palca" o stałym rozmiarze.
 *   Ta lekcja pokazuje, jak z niej poprawnie korzystać: sprawdzać integralność plików,
 *   przechowywać hasła (sól + wolna funkcja), losować tokeny i podpisywać wiadomości (HMAC).
 *   Zakres jest OBRONNY: uczymy się robić dobrze, a nie łamać cudze zabezpieczenia.
 *
 * ANALOGIA:
 *   Skrót to odcisk palca: z człowieka da się go zdjąć, ale z odcisku nie odtworzysz człowieka.
 *   Sól to dodatkowa, unikalna kartka dołączona do każdego hasła, żeby dwa takie same hasła
 *   miały różne odciski. Wolna funkcja (PBKDF2) to bardzo staranna kontrola przy bramce:
 *   dla jednego gościa to chwila, ale dla tłumu napastników koszmarnie długa kolejka.
 *   HMAC to pieczęć woskowa z tajnym sygnetem: każdy może sprawdzić list, ale podrobić
 *   pieczęć potrafi tylko ten, kto ma sygnet (klucz).
 *
 * JAK TO DZIAŁA:
 *   dane → [MessageDigest, np. SHA-256] → 32 bajty skrótu (zwykle zapisywane jako 64 znaki hex)
 *   Cechy dobrego skrótu:
 *     • deterministyczny: te same dane → ten sam skrót,
 *     • jednokierunkowy: ze skrótu nie odtworzysz danych,
 *     • "efekt lawiny": zmiana jednego bitu zmienia około połowy bitów skrótu,
 *     • odporny na kolizje: praktycznie nie da się znaleźć dwóch różnych danych o tym samym skrócie.
 *
 *   Hasła:   hasło + losowa sól → PBKDF2 (tysiące powtórzeń HMAC) → klucz pochodny
 *            zapis w bazie:  algorytm$iteracje$sól$skrót
 *   Tokeny:  SecureRandom → 32 losowe bajty → Base64 URL-safe → 43 znaki
 *   HMAC:    klucz tajny + wiadomość → znacznik (tag); weryfikacja = policz jeszcze raz i porównaj
 *
 * SŁÓWKA:
 *   digest = skrót (wynik); update = dodaj dane; algorithm = algorytm; collision = kolizja;
 *   iteration = powtórzenie; key derivation function (KDF) = funkcja wyprowadzania klucza;
 *   constant-time = w stałym czasie; secret = sekret; tag = znacznik; verify = zweryfikuj;
 *   secure random = bezpieczny generator losowy; predictable = przewidywalny.
 *
 * ZOBACZ TEŻ: t31_jdk_toolbox/Toolbox01UuidBase64 (hex, Base64, CRC32),
 *   t18_io_files/Io12Charsets (tekst na bajty), t34_toward_spring/Spring04WhatSpringGives
 *   (Spring Security i PasswordEncoder), t10_exceptions/Exceptions02CheckedUnchecked
 *   (wyjątki sprawdzane w API kryptograficznym)
 * </pre>
 */
public class Toolbox02HashingSecurity {

    /** Hex małymi literami (HexFormat = zapis szesnastkowy, Java 17+). */
    private static final HexFormat HEX = HexFormat.of();

    /** Wzorzec tokenu: 43 znaki alfabetu URL-safe Base64. */
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z0-9_-]{43}");

    /** Wzorzec zapisu hasła w bazie (4 pola oddzielone znakiem dolara). */
    private static final Pattern STORED_FORMAT = Pattern.compile("pbkdf2-sha256\\$\\d+\\$[A-Za-z0-9+/]+\\$[A-Za-z0-9+/]+");

    /** Kluczy NIE trzyma się w kodzie! Ten jest wyłącznie demonstracyjny i publiczny. */
    private static final byte[] DEMO_KEY = "klucz-demo-nie-uzywac-w-produkcji".getBytes(StandardCharsets.UTF_8);

    public static void main(String[] args) throws IOException {
        title("Toolbox02 — skróty, hasła, losowość i HMAC");

        digestBasics();          // digest basics = podstawy skrótów
        algorithmChoice();       // algorithm choice = wybór algorytmu
        hashFile();              // hash file = skrót pliku
        verifyDownload();        // verify download = weryfikacja pobranego pliku
        constantTimeCompare();   // constant time compare = porównanie w stałym czasie
        passwordsWrongWay();     // passwords wrong way = hasła: jak NIE robić
        passwordsPbkdf2();       // PBKDF2 = standardowa funkcja do haseł z JDK
        secureRandomAndTokens(); // secure random and tokens = bezpieczna losowość i tokeny
        hmacIntegrity();         // HMAC integrity = integralność z kluczem
        whatNotToDo();           // what not to do = czego nie robić
        exercises();             // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PODSTAWY SKRÓTÓW
    // =================================================================================================

    /**
     * 1. {@code MessageDigest.getInstance("SHA-256")} (message digest = skrót wiadomości) tworzy
     * obiekt liczący skrót. Dane podajemy przez {@code update}, wynik odbieramy przez {@code digest}.
     * Skrót to BAJTY — do wyświetlania zamieniamy je na hex.
     */
    static void digestBasics() {
        section("1. MessageDigest i SHA-256");

        String text = "Ala ma kota";
        String hash = sha256Hex(text);
        show("SHA-256(\"Ala ma kota\")", hash);
        // WYNIK: SHA-256("Ala ma kota") → 124bfb6284d82f3b1105f88e3e7a0ee02d0e525193413c05b75041917022cd6e
        show("długość w bajtach", HEX.parseHex(hash).length);
        // WYNIK: długość w bajtach → 32
        show("długość zapisu hex", hash.length());
        // WYNIK: długość zapisu hex → 64
        show("ten sam tekst → ten sam skrót", sha256Hex(text).equals(hash));
        // WYNIK: ten sam tekst → ten sam skrót → true
        show("SHA-256 pustego tekstu", sha256Hex(""));
        // WYNIK: SHA-256 pustego tekstu → e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855

        // EFEKT LAWINY: zmieniamy jeden znak (a → b) i liczymy, ile bitów skrótu się zmieniło.
        byte[] first = sha256(text.getBytes(StandardCharsets.UTF_8));
        byte[] second = sha256("Ala ma kotb".getBytes(StandardCharsets.UTF_8));
        int differentBits = new BigInteger(1, first).xor(new BigInteger(1, second)).bitCount();
        show("zmiana jednego znaku → zmienionych bitów (z 256)", differentBits);
        // WYNIK: zmiana jednego znaku → zmienionych bitów (z 256) → 140
        show("a skrót \"Ala ma kotb\"", HEX.formatHex(second));
        // WYNIK: a skrót "Ala ma kotb" → e1ed58cb0a1081e2f97ad729dbdf76471da823ff571c8aef5fb7602ef984bf36
        // Wniosek: nie ma "podobnych" skrótów dla "podobnych" danych. Przy losowych danych
        // zmienia się około połowy bitów, czyli około 128 z 256.

        // Ręczne użycie: update można wołać wiele razy — wynik jest taki sam, jakby dane
        // podać naraz. Wygodne dla dużych danych (sekcja 3).
        MessageDigest md = newDigest("SHA-256");
        md.update("Ala ".getBytes(StandardCharsets.UTF_8));
        md.update("ma ".getBytes(StandardCharsets.UTF_8));
        md.update("kota".getBytes(StandardCharsets.UTF_8));
        show("po kawałkach (update x3) == naraz", HEX.formatHex(md.digest()).equals(hash));
        // WYNIK: po kawałkach (update x3) == naraz → true
        // digest() zeruje stan obiektu — po nim obiekt jest gotowy do liczenia następnego skrótu.
        md.update("Ala ma kota".getBytes(StandardCharsets.UTF_8));
        show("ten sam obiekt użyty ponownie", HEX.formatHex(md.digest()).equals(hash));
        // WYNIK: ten sam obiekt użyty ponownie → true

        // PUŁAPKA: MessageDigest NIE jest bezpieczny wątkowo.
        // Dlaczego: obiekt trzyma stan pośredni (dotychczas dodane dane). Dwa wątki wołające
        // update na tym samym obiekcie pomieszają dane. Twórz obiekt na każde użycie
        // (jest tani) albo trzymaj go w ThreadLocal (t21_concurrency/Concurrency08ThreadSafetyPatterns).

        // PUŁAPKA: tekst na bajty zawsze z jawnym zestawem znaków (UTF-8).
        // Dlaczego: skrót liczy się z bajtów, więc "zażółć" w UTF-8 i w Windows-1250 da dwa różne
        // skróty. getBytes() bez argumentu użyłoby zestawu zależnego od komputera.

        expectThrows("nieznany algorytm", () -> MessageDigest.getInstance("FOO"));
        // WYNIK: ✔ nieznany algorytm → rzucono NoSuchAlgorithmException: FOO MessageDigest not available
        // NoSuchAlgorithmException to wyjątek SPRAWDZANY (checked). Dla SHA-256 w praktyce
        // nigdy nie wystąpi (specyfikacja wymaga jego obecności w każdym JDK), dlatego helper
        // newDigest(...) opakowuje go w IllegalStateException — to błąd programisty,
        // nie sytuacja, którą aplikacja ma obsłużyć.

        // DOBRA PRAKTYKA: wynik skrótu to bajty — przechowuj je jako bajty lub hex, nie jako
        // new String(bajty). Dlaczego: bajty skrótu nie są poprawnym tekstem w żadnym zestawie
        // znaków, a konwersja bezpowrotnie je psuje.
    }

    // =================================================================================================
    // 2. WYBÓR ALGORYTMU
    // =================================================================================================

    /**
     * 2. Algorytmy różnią się długością skrótu i... odpornością. MD5 i SHA-1 uznano za złamane
     * pod względem bezpieczeństwa; dziś wybieramy SHA-256 lub nowszy.
     */
    static void algorithmChoice() {
        section("2. Który algorytm? SHA-256, SHA-512, SHA3 — i dlaczego nie MD5 / SHA-1");

        for (String name : List.of("MD5", "SHA-1", "SHA-256", "SHA-512", "SHA3-256")) {
            MessageDigest md = newDigest(name);
            show(String.format(Locale.ROOT, "%-9s długość skrótu (bajty)", name), md.getDigestLength());
        }
        // WYNIK: MD5       długość skrótu (bajty) → 16
        // WYNIK: SHA-1     długość skrótu (bajty) → 20
        // WYNIK: SHA-256   długość skrótu (bajty) → 32
        // WYNIK: SHA-512   długość skrótu (bajty) → 64
        // WYNIK: SHA3-256  długość skrótu (bajty) → 32
        // SHA3-256 to nowsza rodzina (Keccak, JDK 9+), zbudowana inaczej niż SHA-2.

        // PUŁAPKA: MD5 i SHA-1 do celów bezpieczeństwa są niedopuszczalne.
        // Dlaczego: znaleziono praktyczne KOLIZJE: dla MD5 w 2004 roku, dla SHA-1 publicznie
        // w 2017 roku (atak SHAttered). Kolizja to dwa różne dokumenty o tym samym skrócie —
        // można więc przygotować parę: niewinny dokument do podpisania i złośliwy o tym samym
        // skrócie, a podpis pasowałby do obu. Skrót miał gwarantować, że "ten sam skrót =
        // te same dane", a MD5 i SHA-1 tej gwarancji już nie dają.
        show("MD5 w hex (32 znaki)", digestHex("MD5", "Ala ma kota").length());
        // WYNIK: MD5 w hex (32 znaki) → 32
        // Gdzie MD5 / SHA-1 jeszcze występują i są tolerowane: klucze pamięci podręcznej, wykrywanie
        // duplikatów, identyfikatory w starych formatach. Zasada: gdy nikt nie ma interesu w
        // podrobieniu danych, wystarczy. Gdy ktoś mógłby — SHA-256 lub nowszy.

        // MD5 jest też jedyną podstawą UUID wersji 3 (Toolbox01) — dlatego to tylko identyfikator.

        // JAK WYBIERAĆ:
        //   integralność plików, sumy kontrolne w repozytoriach  → SHA-256
        //   podpisy, certyfikaty                                  → SHA-256 lub SHA-384/512
        //   hasła                                                 → NIE zwykły skrót (sekcje 6-7)
        //   błędy transmisji (nie ataki)                          → CRC32 wystarczy (Toolbox01)

        // DOBRA PRAKTYKA: nazwę algorytmu trzymaj w jednej stałej lub konfiguracji.
        // Dlaczego: gdy za kilka lat zalecenia się zmienią, poprawiasz jedno miejsce, a nie dziesięć.
    }

    // =================================================================================================
    // 3. SKRÓT PLIKU PORCJAMI
    // =================================================================================================

    /**
     * 3. Plik może mieć gigabajty. Nie wczytuj go w całości ({@code Files.readAllBytes}) —
     * licz skrót porcjami (bufor 8 KB) przez {@code update} albo przez {@code DigestInputStream}.
     */
    static void hashFile() throws IOException {
        section("3. Skrót pliku porcjami (stała pamięć)");

        Path dir = TempDir.create("tool-hash"); // create = utwórz katalog tymczasowy
        try {
            Path file = dir.resolve("dane.bin"); // resolve = dołącz nazwę do ścieżki
            byte[] content = new byte[200_000];
            for (int i = 0; i < content.length; i++) {
                content[i] = (byte) (i * 31 + 7); // deterministyczna zawartość
            }
            Files.write(file, content);

            String wholeHash = HEX.formatHex(sha256(content));
            show("skrót całej tablicy bajtów", wholeHash);
            // WYNIK: skrót całej tablicy bajtów → 8f9d1bf454d63cd9fc6edbe8f3f2331cc1f9b195c7ec90533717bf243ae966c7

            // SPOSÓB 1: własna pętla z buforem.
            MessageDigest md = newDigest("SHA-256");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192]; // buffer = bufor
                int read;                       // read = ile odczytano
                while ((read = in.read(buffer)) != -1) {
                    md.update(buffer, 0, read); // tylko 'read' bajtów jest ważnych!
                }
            }
            String loopHash = HEX.formatHex(md.digest());
            show("pętla z buforem == całość", loopHash.equals(wholeHash));
            // WYNIK: pętla z buforem == całość → true

            // SPOSÓB 2: DigestInputStream liczy skrót "po drodze" podczas czytania.
            MessageDigest md2 = newDigest("SHA-256");
            try (InputStream in = new DigestInputStream(Files.newInputStream(file), md2)) {
                // transferTo (Java 9+) przepisuje strumień; nullOutputStream (Java 11+) wyrzuca dane
                in.transferTo(OutputStream.nullOutputStream());
            }
            show("DigestInputStream == całość", HEX.formatHex(md2.digest()).equals(wholeHash));
            // WYNIK: DigestInputStream == całość → true

            show("sha256Hex(Path)", sha256Hex(file).equals(wholeHash));
            // WYNIK: sha256Hex(Path) → true

            // PUŁAPKA: w pętli wołaj md.update(buffer, 0, read), a nie md.update(buffer).
            // Dlaczego: ostatnia porcja jest zwykle krótsza od bufora, a reszta bufora to
            // stare dane z poprzedniego obiegu — trafiłyby do skrótu i zepsuły wynik.
            // To samo dotyczy zapisu: out.write(buffer, 0, read).

            // PUŁAPKA: gdy zmienisz choćby jeden bajt, skrót się zmieni — ale gdy zmienisz
            // KOLEJNOŚĆ porcji lub dopiszesz zero na końcu, to też. Skrót zależy od całej
            // zawartości i jej kolejności; dlatego służy do wykrywania zmian.
            byte[] modified = content.clone();
            modified[199_999] ^= 1; // odwróć jeden bit w ostatnim bajcie
            show("jeden bit zmieniony → inny skrót", !HEX.formatHex(sha256(modified)).equals(wholeHash));
            // WYNIK: jeden bit zmieniony → inny skrót → true

            // DOBRA PRAKTYKA: dla dużych plików używaj bufora i pętli (albo DigestInputStream).
            // Dlaczego: pamięć zużyta przez program nie zależy wtedy od rozmiaru pliku.
        } finally {
            TempDir.deleteRecursively(dir); // deleteRecursively = usuń katalog z zawartością
        }
    }

    // =================================================================================================
    // 4. WERYFIKACJA POBRANYCH PLIKÓW
    // =================================================================================================

    /**
     * 4. Wydawca publikuje plik i jego skrót (sumę kontrolną). Po pobraniu liczysz skrót sam
     * i porównujesz. Zgodność oznacza, że plik nie został uszkodzony ani zmieniony po drodze.
     */
    static void verifyDownload() throws IOException {
        section("4. Weryfikacja pobranego pliku (suma kontrolna)");

        Path dir = TempDir.create("tool-verify");
        try {
            Path file = dir.resolve("instalator.bin");
            byte[] original = "To jest zawartość instalatora, wersja 1.0".getBytes(StandardCharsets.UTF_8);
            Files.write(file, original);
            String published = sha256Hex(file); // tak wydawca policzyłby skrót i opublikował go

            show("opublikowany skrót", published);
            // WYNIK: opublikowany skrót → 7f52f5a6d471427cce15ea07e724b486c37a54ac74b06c7f4f46644b0ce53df6
            show("pobrany plik jest poprawny", verifyChecksum(file, published));
            // WYNIK: pobrany plik jest poprawny → true
            show("skrót wielkimi literami (często tak publikowany)", verifyChecksum(file, published.toUpperCase(Locale.ROOT)));
            // WYNIK: skrót wielkimi literami (często tak publikowany) → true

            // Symulujemy zmianę pliku po drodze (uszkodzenie lub podmiana).
            byte[] tampered = original.clone();
            tampered[0] ^= 1;
            Files.write(file, tampered);
            show("plik zmieniony po drodze", verifyChecksum(file, published));
            // WYNIK: plik zmieniony po drodze → false
            show("brak poprawnego zapisu hex", verifyChecksum(file, "to-nie-jest-hex"));
            // WYNIK: brak poprawnego zapisu hex → false
        } finally {
            TempDir.deleteRecursively(dir);
        }

        // PUŁAPKA: suma kontrolna leżąca OBOK pliku na tym samym serwerze niczego nie dowodzi.
        // Dlaczego: kto podmieni plik na serwerze, podmieni też plik z sumą. Skrót chroni przed
        // uszkodzeniem i przed zmianami po drodze tylko wtedy, gdy skrót dostajesz INNĄ, zaufaną
        // drogą (strona wydawcy po HTTPS, podpis cyfrowy, menedżer pakietów z podpisami).
        // Gdy chodzi o autentyczność wydawcy — potrzebny jest podpis cyfrowy, nie sam skrót.

        // DOBRA PRAKTYKA: porównuj skróty po zdekodowaniu do bajtów i bez rozróżniania wielkości
        // liter w zapisie hex; nie wołaj equals na tekstach. Dlaczego: "ABC" i "abc" to ten sam
        // skrót, a equals na tekstach ujawniałby (w teorii) różnice w czasie — patrz sekcja 5.
    }

    // =================================================================================================
    // 5. PORÓWNANIE W STAŁYM CZASIE
    // =================================================================================================

    /**
     * 5. {@code MessageDigest.isEqual(a, b)} porównuje dwie tablice bajtów w czasie, który nie zależy
     * od tego, na której pozycji różnią się dane. Używaj go do porównywania WSZYSTKIEGO, co jest
     * tajne albo pochodzi z tajnego klucza (znaczniki HMAC, tokeny, skróty haseł).
     */
    static void constantTimeCompare() {
        section("5. Porównanie w stałym czasie: MessageDigest.isEqual");

        byte[] expected = sha256("tajny token".getBytes(StandardCharsets.UTF_8));
        byte[] good = expected.clone();
        byte[] wrongAtStart = expected.clone();
        wrongAtStart[0] ^= 1;
        byte[] wrongAtEnd = expected.clone();
        wrongAtEnd[31] ^= 1;

        show("isEqual(takie same)", MessageDigest.isEqual(expected, good));
        // WYNIK: isEqual(takie same) → true
        show("isEqual(różnica na początku)", MessageDigest.isEqual(expected, wrongAtStart));
        // WYNIK: isEqual(różnica na początku) → false
        show("isEqual(różnica na końcu)", MessageDigest.isEqual(expected, wrongAtEnd));
        // WYNIK: isEqual(różnica na końcu) → false
        show("isEqual(różna długość)", MessageDigest.isEqual(expected, new byte[16]));
        // WYNIK: isEqual(różna długość) → false
        // Wyniki są takie same jak z Arrays.equals — różnica jest w CZASIE, a tego w wyniku
        // programu nie widać (i dlatego trzeba o tym pamiętać, a nie "sprawdzić doświadczalnie").
        show("Arrays.equals daje te same odpowiedzi", Arrays.equals(expected, good) && !Arrays.equals(expected, wrongAtStart));
        // WYNIK: Arrays.equals daje te same odpowiedzi → true

        // DLACZEGO TO WAŻNE:
        // Arrays.equals i String.equals kończą pracę przy PIERWSZEJ różnicy. Jeśli różnica jest na
        // pozycji 1, odpowiedź przychodzi odrobinę szybciej niż gdy na pozycji 20. Atakujący, który
        // może wysyłać zapytania i bardzo dokładnie mierzy czas odpowiedzi (zwłaszcza po
        // sieci lokalnej), może po kolei odgadywać bajty poprawnego znacznika (atak czasowy,
        // timing attack). Porównanie w stałym czasie zamyka ten kanał informacji.
        // Od Java 6u17 isEqual jest zaimplementowany tak, żeby czas nie zależał od zawartości
        // (zależy tylko od długości — a o długości znacznika i tak wiadomo publicznie).

        // PUŁAPKA: porównanie tekstów tokenów przez equals lub ==.
        // Dlaczego: == porównuje referencje (zawsze błąd dla tekstów), a equals nie jest w stałym
        // czasie. Zamień na bajty (UTF-8) i użyj isEqual.
        show("porównanie tekstów tokenów", MessageDigest.isEqual(
                "abc".getBytes(StandardCharsets.UTF_8), "abc".getBytes(StandardCharsets.UTF_8)));
        // WYNIK: porównanie tekstów tokenów → true

        // DOBRA PRAKTYKA: ten sam komunikat błędu i podobny czas dla "zły login" i "złe hasło".
        // Dlaczego: inna odpowiedź w obu przypadkach pozwala sprawdzać, jakie konta istnieją
        // (enumeracja użytkowników). Dlatego logowanie zwykle mówi tylko: "nieprawidłowe dane".
    }

    // =================================================================================================
    // 6. HASŁA — JAK NIE ROBIĆ
    // =================================================================================================

    /**
     * 6. Trzy typowe błędy przy przechowywaniu haseł. Pokazujemy je po to, żeby ich NIE powtarzać.
     */
    static void passwordsWrongWay() {
        section("6. Hasła: dlaczego nie tekst jawny ani zwykły SHA-256");

        // BŁĄD 1: hasło zapisane jawnie.
        // Gdy ktoś zdobędzie kopię bazy (wyciek, kopia zapasowa, błąd w zapytaniu), dostaje
        // wszystkie hasła od razu. Użytkownicy używają tych samych haseł w wielu serwisach,
        // więc szkoda wykracza daleko poza Twoją aplikację.

        // BŁĄD 2: zwykły skrót bez soli.
        String hashAla = sha256Hex("haslo123");
        String hashOla = sha256Hex("haslo123");
        show("dwóch użytkowników z tym samym hasłem → ten sam skrót", hashAla.equals(hashOla));
        // WYNIK: dwóch użytkowników z tym samym hasłem → ten sam skrót → true
        // Skutki: po jednym złamanym haśle widać, kto jeszcze go używa; skróty popularnych haseł
        // można policzyć z góry i trzymać w tablicach (tablice tęczowe, rainbow tables).

        // BŁĄD 3: dodanie soli, ale szybka funkcja. Sól rozwiązuje problem z punktu 2:
        byte[] salt1 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
        byte[] salt2 = {16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1};
        String saltedAla = HEX.formatHex(sha256(concat(salt1, "haslo123")));
        String saltedOla = HEX.formatHex(sha256(concat(salt2, "haslo123")));
        show("z solą te same hasła dają różne skróty", !saltedAla.equals(saltedOla));
        // WYNIK: z solą te same hasła dają różne skróty → true
        // ...ale SHA-256 jest zaprojektowany jako SZYBKI. Karty graficzne liczą miliardy skrótów
        // SHA-256 na sekundę, więc słabe hasło (słowo ze słownika, data) da się odgadnąć
        // próbując po kolei, nawet mając sól. Potrzebna jest funkcja CELOWO wolna i regulowana.

        // PUŁAPKA: "szybkie" znaczy tu "złe". Dlaczego: przy sprawdzaniu hasła użytkownika
        // jedno obliczenie trwa mikrosekundy, więc nie przeszkadza, ale napastnik z kopią bazy
        // robi ich biliony. Spowolnienie jednego obliczenia do ~100 ms prawie nie boli
        // logującego się użytkownika, a dla napastnika oznacza miliony razy mniej prób.

        // ZASADA: sól + wolna funkcja wyprowadzania klucza (KDF, key derivation function).
        // W JDK jest PBKDF2; w bibliotekach: bcrypt, scrypt, Argon2 (sekcja 7).

        // DOBRA PRAKTYKA: nigdy nie wymyślaj własnego schematu (np. "SHA-256 pięć razy z solą").
        // Dlaczego: schematy "domowe" mają subtelne wady, które znajdują dopiero specjaliści.
        // Użyj sprawdzonego algorytmu (PBKDF2, bcrypt, scrypt, Argon2) w ich typowej konfiguracji.
    }

    // =================================================================================================
    // 7. PBKDF2 I FORMAT ZAPISU HASŁA
    // =================================================================================================

    /**
     * 7. {@code SecretKeyFactory} z algorytmem {@code PBKDF2WithHmacSHA256} (password-based key
     * derivation function = funkcja wyprowadzania klucza z hasła) powtarza HMAC tysiące razy.
     * Pokazujemy samo wyprowadzenie klucza i kompletny zapis "algorytm$iteracje$sól$skrót".
     */
    static void passwordsPbkdf2() {
        section("7. PBKDF2: sól, iteracje i zapis algorytm$iteracje$sól$skrót");

        // W demonstracji: STAŁA sól i niewielka liczba iteracji, żeby wynik był zawsze taki sam
        // i lekcja działała szybko. W programie produkcyjnym sól jest LOSOWA dla każdego
        // użytkownika (SecureRandom, 16 bajtów lub więcej), a iteracji jest RZĘDU SETEK TYSIĘCY
        // dla PBKDF2-HMAC-SHA256 (aktualne zalecenia, np. OWASP, to kilkaset tysięcy; liczba rośnie
        // z mocą sprzętu, więc sprawdzaj aktualne zalecenia).
        byte[] salt = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
        char[] password = "haslo123".toCharArray(); // char[] zamiast String — da się go wyczyścić
        String stored = PasswordHasher.hash(password, salt, 10_000);
        show("zapis do bazy", stored);
        // WYNIK: zapis do bazy → pbkdf2-sha256$10000$AQIDBAUGBwgJCgsMDQ4PEA$BqZj3BTyL0NAnsGogKcu0RrjgspN0sq0AHXXIN3rQ38
        show("zgodny z formatem", STORED_FORMAT.matcher(stored).matches());
        // WYNIK: zgodny z formatem → true
        show("liczba pól oddzielonych $", stored.split("\\$").length);
        // WYNIK: liczba pól oddzielonych $ → 4

        show("poprawne hasło", PasswordHasher.verify("haslo123".toCharArray(), stored));
        // WYNIK: poprawne hasło → true
        show("błędne hasło", PasswordHasher.verify("Haslo123".toCharArray(), stored));
        // WYNIK: błędne hasło → false
        show("uszkodzony zapis w bazie", PasswordHasher.verify("haslo123".toCharArray(), "to-nie-jest-zapis"));
        // WYNIK: uszkodzony zapis w bazie → false
        Arrays.fill(password, '\0'); // czyścimy hasło z pamięci, gdy już niepotrzebne
        show("po wyczyszczeniu same zera", new String(password).chars().allMatch(c -> c == 0));
        // WYNIK: po wyczyszczeniu same zera → true

        // Prawdziwy zapis: losowa sól. Dwa razy to samo hasło → dwa różne zapisy.
        String s1 = PasswordHasher.hashWithNewSalt("haslo123".toCharArray(), 10_000);
        String s2 = PasswordHasher.hashWithNewSalt("haslo123".toCharArray(), 10_000);
        show("dwa zapisy tego samego hasła są różne", !s1.equals(s2));
        // WYNIK: dwa zapisy tego samego hasła są różne → true
        show("a oba przechodzą weryfikację", PasswordHasher.verify("haslo123".toCharArray(), s1)
                && PasswordHasher.verify("haslo123".toCharArray(), s2));
        // WYNIK: a oba przechodzą weryfikację → true
        show("format zapisu z losową solą", STORED_FORMAT.matcher(s1).matches());
        // WYNIK: format zapisu z losową solą → true

        // DLACZEGO W ZAPISIE JEST ALGORYTM I LICZBA ITERACJI:
        // Za dwa lata zalecenia wzrosną. Gdy parametry są częścią zapisu, stare hasła nadal się
        // sprawdzają (używamy ich zapisanych parametrów), a po udanym logowaniu możesz policzyć
        // nowy zapis z mocniejszymi parametrami i go podmienić ("rehash on login" — ćwiczenie 4).
        // To samo robi DelegatingPasswordEncoder w Springu: zapis zaczyna się od {bcrypt}, {pbkdf2} itp.

        // PUŁAPKA: hasło jako String zostaje w pamięci do odśmiecenia i bywa w zrzutach pamięci.
        // Dlaczego: String jest niezmienny — nie da się go zamazać. char[] możesz wyzerować po
        // użyciu. To nie jest ochrona absolutna (hasło i tak przeszło przez żądanie HTTP), ale
        // dobry nawyk w kodzie, który obsługuje hasła.

        // PUŁAPKA: za mało iteracji albo stała sól dla wszystkich ("dla uproszczenia").
        // Dlaczego: stała sól przywraca problem z sekcji 6, a mała liczba iteracji daje
        // napastnikowi tanie próby. Demonstracja z 10 000 jest TYLKO dla szybkości lekcji.

        // ALTERNATYWY, gdy możesz dodać bibliotekę:
        //   Argon2id — nowoczesny, zużywa dużo pamięci (utrudnia ataki na kartach graficznych),
        //   scrypt   — też pamięciożerny,
        //   bcrypt   — bardzo rozpowszechniony; uwaga na limit 72 bajtów hasła.
        // Spring Security ma gotowe PasswordEncoder: BCryptPasswordEncoder, Argon2PasswordEncoder,
        // Pbkdf2PasswordEncoder i DelegatingPasswordEncoder (wiele algorytmów naraz, z prefiksem
        // w zapisie). W aplikacji Springowej używasz ich zamiast własnego kodu z tej lekcji.

        // DOBRA PRAKTYKA: sól losowa dla każdego hasła, zapisana razem ze skrótem; parametry w zapisie;
        // weryfikacja przez porównanie w stałym czasie; limit długości hasła na wejściu (np. 1000
        // znaków), żeby ktoś nie wysyłał megabajtów do policzenia. Hasła nigdy w logach.
    }

    // =================================================================================================
    // 8. SECURERANDOM I TOKENY
    // =================================================================================================

    /**
     * 8. {@code java.util.Random} służy do symulacji i gier. {@code java.security.SecureRandom}
     * do wszystkiego, co ma być nieprzewidywalne: tokeny, sole, klucze, kody jednorazowe.
     */
    static void secureRandomAndTokens() {
        section("8. SecureRandom, tokeny i kody jednorazowe");

        // Random z ziarnem jest w pełni przewidywalny: to samo ziarno → ta sama sekwencja.
        Random r1 = new Random(42); // 42 = ziarno (seed)
        Random r2 = new Random(42);
        List<Integer> a = List.of(r1.nextInt(100), r1.nextInt(100), r1.nextInt(100));
        List<Integer> b = List.of(r2.nextInt(100), r2.nextInt(100), r2.nextInt(100));
        show("Random(42) pierwsze trzy liczby", a);
        // WYNIK: Random(42) pierwsze trzy liczby → [30, 63, 48]
        show("drugi Random(42) daje to samo", a.equals(b));
        // WYNIK: drugi Random(42) daje to samo → true
        // Nawet bez znajomości ziarna Random da się przewidzieć: to generator liniowy
        // z 48-bitowym stanem, więc po zobaczeniu kilku kolejnych wyników można odtworzyć stan
        // i przewidzieć resztę. Do tokenów, haseł jednorazowych i kluczy jest bezużyteczny.

        // PUŁAPKA: token z Random, Math.random() lub z UUID zbudowanego z czasu.
        // Dlaczego: atakujący może zgadnąć kolejne wartości. SecureRandom korzysta ze źródła
        // losowości systemu operacyjnego (zbieranie entropii z zdarzeń sprzętowych) i jest
        // zaprojektowany tak, by kolejnych wartości NIE dało się przewidzieć.

        // Token: 32 losowe bajty = 256 bitów losowości, zapisane jako URL-safe Base64 (Toolbox01).
        String token1 = newToken();
        String token2 = newToken();
        show("długość tokenu (znaki)", token1.length());
        // WYNIK: długość tokenu (znaki) → 43
        show("pasuje do alfabetu URL-safe", TOKEN.matcher(token1).matches());
        // WYNIK: pasuje do alfabetu URL-safe → true
        show("dwa tokeny są różne", !token1.equals(token2));
        // WYNIK: dwa tokeny są różne → true
        // (wartości tokenów celowo NIE są drukowane — za każdym razem inne)

        // Kod jednorazowy (np. SMS): 6 cyfr z zerami wiodącymi.
        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        show("długość kodu 6-cyfrowego", code.length());
        // WYNIK: długość kodu 6-cyfrowego → 6
        show("same cyfry", code.chars().allMatch(Character::isDigit));
        // WYNIK: same cyfry → true

        // DOBRA PRAKTYKA: dla tokenów resetu hasła, zaproszeń i kluczy API zapisuj w bazie SKRÓT
        // tokenu (SHA-256 wystarczy, bo token ma dużo losowości), a sam token pokaż użytkownikowi
        // tylko raz. Dlaczego: po wycieku bazy napastnik zobaczy skróty, a nie działające tokeny.
        show("skrót tokenu do zapisu w bazie (znaki hex)", sha256Hex(token1).length());
        // WYNIK: skrót tokenu do zapisu w bazie (znaki hex) → 64
        // Token powinien też wygasać i być jednorazowy.

        // PUŁAPKA: SecureRandom.setSeed z przewidywalną wartością (np. czas).
        // Dlaczego: w niektórych implementacjach (np. SHA1PRNG) ustawienie ziarna przed
        // pierwszym użyciem czyni wynik POWTARZALNYM, czyli przewidywalnym. Domyślny
        // new SecureRandom() sam się właściwie zasiewa — nie wołaj setSeed.

        // UWAGA: SecureRandom.getInstanceStrong() wybiera "najsilniejszy" generator platformy, który
        // na niektórych systemach może BLOKOWAĆ w oczekiwaniu na entropię. W zwykłych
        // aplikacjach wystarczy new SecureRandom() — a najlepiej jedna współdzielona instancja
        // (SecureRandom jest bezpieczny wątkowo).

        // UUID.randomUUID() używa SecureRandom, więc jest nieprzewidywalny, ale ma 122 losowe bity
        // i znany układ. Jako token sesji lepsze są surowe losowe bajty jak wyżej.
    }

    // =================================================================================================
    // 9. HMAC
    // =================================================================================================

    /**
     * 9. HMAC (hash-based message authentication code = kod uwierzytelnienia wiadomości oparty na
     * skrócie) łączy skrót z tajnym kluczem. Odbiorca, który zna klucz, sprawdza, że wiadomość
     * pochodzi od właściciela klucza i nie została zmieniona. Zwykły skrót tego nie potrafi:
     * każdy może policzyć skrót zmienionej wiadomości.
     */
    static void hmacIntegrity() {
        section("9. HMAC: integralność i autentyczność z kluczem");

        String message = "kwota=100&konto=42";
        String tag = hmacSha256Hex(DEMO_KEY, message); // tag = znacznik
        show("znacznik HMAC-SHA256", tag);
        // WYNIK: znacznik HMAC-SHA256 → 2cf61c964f2cfc6d37d322c8edb0708fc32edb94f4586d66863b48c203e99c67
        show("długość znacznika (bajty)", HEX.parseHex(tag).length);
        // WYNIK: długość znacznika (bajty) → 32
        show("weryfikacja oryginału", hmacVerify(DEMO_KEY, message, tag));
        // WYNIK: weryfikacja oryginału → true
        show("wiadomość zmieniona po drodze", hmacVerify(DEMO_KEY, "kwota=900&konto=42", tag));
        // WYNIK: wiadomość zmieniona po drodze → false
        show("zły klucz", hmacVerify("inny-klucz".getBytes(StandardCharsets.UTF_8), message, tag));
        // WYNIK: zły klucz → false

        // Poprawność implementacji potwierdzamy wektorem testowym z dokumentu RFC 4231
        // (klucz "Jefe", wiadomość "what do ya want for nothing?").
        String rfc = hmacSha256Hex("Jefe".getBytes(StandardCharsets.UTF_8), "what do ya want for nothing?");
        show("zgodny z wektorem testowym RFC 4231", rfc.equals("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843"));
        // WYNIK: zgodny z wektorem testowym RFC 4231 → true

        // Zastosowania: podpis webhooka (nagłówek z HMAC treści żądania), podpisane ciasteczka,
        // tokeny JWT w wariancie HS256, podpisywanie żądań do API.

        // PUŁAPKA: samo SHA-256(klucz + wiadomość) zamiast HMAC.
        // Dlaczego: SHA-256 i SHA-512 mają budowę, która pozwala (przy tej konstrukcji) dopisać
        // dane do wiadomości i policzyć poprawny nowy skrót BEZ znajomości klucza
        // (atak przedłużenia długości, length extension). HMAC jest zbudowany tak, że ten atak
        // nie działa. Zawsze używaj gotowego Mac, nie własnej konstrukcji.

        // PUŁAPKA: weryfikacja znacznika przez equals na tekstach. Dlaczego: nie jest w stałym
        // czasie (sekcja 5). Dlatego hmacVerify używa MessageDigest.isEqual.

        // PUŁAPKA: ten sam podpis można odtworzyć w innym czasie (atak powtórzeniowy, replay).
        // Dlaczego: HMAC mówi "wiadomość jest autentyczna", ale nie "jest świeża". Dołącz do
        // podpisywanych danych czas (i sprawdzaj go) albo jednorazowy numer (nonce).

        // DOBRA PRAKTYKA: klucz HMAC to co najmniej 32 losowe bajty z SecureRandom lub menedżera sekretów,
        // inny klucz do każdego celu, możliwość rotacji. DEMO_KEY w tej lekcji jest publiczny —
        // dlatego ma w nazwie słowo "demo" i NIGDY nie powinien trafić do prawdziwego programu.
    }

    // =================================================================================================
    // 10. CZEGO NIE ROBIĆ
    // =================================================================================================

    /**
     * 10. Lista błędów, które powtarzają się w kodzie z "domowym zabezpieczeniem", oraz sposób
     * wczytywania sekretów. Szyfrowanie (AES-GCM) opisujemy tylko słowami.
     */
    static void whatNotToDo() {
        section("10. Czego NIE robić: własna kryptografia, ECB, klucze w kodzie");

        // 1. WŁASNA KRYPTOGRAFIA. Nie wymyślaj szyfrów, skrótów ani protokołów. Błędy w takich
        //    konstrukcjach zwykle widać dopiero, gdy ktoś je wykorzysta. Używaj sprawdzonych
        //    algorytmów z JDK lub bibliotek (np. Google Tink), w ich standardowych trybach.
        // 2. KLUCZE I HASŁA W KODZIE LUB W REPOZYTORIUM. Wszystko, co trafi do repozytorium,
        //    zostaje w historii na zawsze i widzi to każdy z dostępem do kodu.
        //    Sekrety czytaj ze zmiennych środowiskowych, menedżera sekretów lub pliku poza repozytorium.
        // 3. TRYB ECB. Przy szyfrowaniu blokowym (AES) tryb ECB szyfruje identyczne bloki tak
        //    samo — struktura danych (np. obrazka) prześwituje przez szyfrogram. Nigdy go nie wybieraj.
        // 4. MD5 / SHA-1 / CRC32 jako zabezpieczenie (sekcja 2).
        // 5. Random lub Math.random() do tokenów (sekcja 8).
        // 6. Hasła jawnie lub zwykłym skrótem (sekcja 6).
        // 7. Porównywanie sekretów przez equals (sekcja 5).
        // 8. Wypisywanie haseł, tokenów i kluczy w logach, wyjątkach i komunikatach błędów.
        // 9. Ignorowanie wyjątków GeneralSecurityException "żeby się kompilowało" — błąd
        //    kryptograficzny to zwykle błąd bezpieczeństwa i aplikacja ma wtedy odmówić działania.

        expectThrows("brak sekretu w środowisku", () -> requireSecret("TOOLBOX_NIE_ISTNIEJE_123"));
        // WYNIK: ✔ brak sekretu w środowisku → rzucono IllegalStateException: Brak sekretu w zmiennej środowiskowej: TOOLBOX_NIE_ISTNIEJE_123
        // requireSecret odczytuje zmienną środowiskową i NIE ma wartości domyślnej: lepiej, żeby
        // aplikacja nie wystartowała, niż żeby uruchomiła się z "domyślnym" kluczem znanym wszystkim.

        // SZYFROWANIE (tylko opis — nie piszemy tego kodu w lekcji):
        //   Do poufności używa się szyfrowania uwierzytelnionego, w JDK: AES w trybie GCM
        //   (Cipher.getInstance("AES/GCM/NoPadding")). GCM jednocześnie szyfruje i sprawdza, że
        //   szyfrogram nie został zmieniony (znacznik 128 bitów).
        //   Zasady: klucz 256-bitowy z KeyGenerator/SecureRandom lub z KDF; dla KAŻDEGO szyfrowania
        //   nowy, losowy wektor początkowy (IV, 12 bajtów), zapisywany razem z szyfrogramem;
        //   NIGDY ten sam IV z tym samym kluczem (to całkowicie łamie bezpieczeństwo GCM).
        //   Klucz trzymasz poza kodem. Gdy to możliwe, używaj biblioteki wyższego poziomu
        //   (np. Tink), która nie pozwala na te pomyłki.
        //   Szyfrowanie "w locie" po sieci robi TLS (HTTPS) — nie wymyślaj własnego.

        // PUŁAPKA: "mamy szyfrowanie, więc jest bezpiecznie". Dlaczego: bezpieczeństwo to cały
        // łańcuch: klucze, losowość, tryby, uprawnienia, logi, kopie zapasowe. Jedno słabe
        // ogniwo wystarczy.

        // DOBRA PRAKTYKA: wybieraj nudne, popularne, aktualne rozwiązania i ucz się zaleceń
        // (OWASP Cheat Sheet Series). Dlaczego: ich słabe punkty są już znane i opisane.
    }

    // =================================================================================================
    // NARZĘDZIA LEKCJI
    // =================================================================================================

    /** Tworzy obiekt skrótu; nieznany algorytm to błąd programisty (IllegalStateException). */
    static MessageDigest newDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Brak algorytmu: " + algorithm, e);
        }
    }

    static byte[] sha256(byte[] data) {
        return newDigest("SHA-256").digest(data);
    }

    static String sha256Hex(String text) {
        return HEX.formatHex(sha256(text.getBytes(StandardCharsets.UTF_8)));
    }

    static String digestHex(String algorithm, String text) {
        return HEX.formatHex(newDigest(algorithm).digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    /** Skrót pliku liczony porcjami (stała pamięć). */
    static String sha256Hex(Path file) throws IOException {
        MessageDigest md = newDigest("SHA-256");
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
        }
        return HEX.formatHex(md.digest());
    }

    /** Sól + hasło jako bajty UTF-8 (tylko do demonstracji złego sposobu). */
    private static byte[] concat(byte[] salt, String password) {
        byte[] pw = password.getBytes(StandardCharsets.UTF_8);
        byte[] all = Arrays.copyOf(salt, salt.length + pw.length);
        System.arraycopy(pw, 0, all, salt.length, pw.length);
        return all;
    }

    /** Czy skrót pliku zgadza się z opublikowanym (hex bez względu na wielkość liter). */
    static boolean verifyChecksum(Path file, String expectedHex) throws IOException {
        byte[] expected;
        try {
            expected = HEX.parseHex(expectedHex.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return false; // to nie jest poprawny zapis hex
        }
        MessageDigest md = newDigest("SHA-256");
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
        }
        return MessageDigest.isEqual(md.digest(), expected);
    }

    /** Token: 32 losowe bajty jako URL-safe Base64 bez dopełnienia (43 znaki). */
    static String newToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes); // nextBytes = wypełnij losowymi bajtami
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** HMAC-SHA256 jako hex (Mac = obiekt liczący kod uwierzytelnienia). */
    static String hmacSha256Hex(byte[] key, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256")); // init = zainicjuj kluczem
            return HEX.formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC niedostępny", e);
        }
    }

    /** Weryfikacja znacznika: liczymy jeszcze raz i porównujemy w stałym czasie. */
    static boolean hmacVerify(byte[] key, String message, String tagHex) {
        byte[] expected = HEX.parseHex(hmacSha256Hex(key, message));
        byte[] given;
        try {
            given = HEX.parseHex(tagHex);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return MessageDigest.isEqual(expected, given);
    }

    /** Sekret ze zmiennej środowiskowej; brak = błąd (bez wartości domyślnej). */
    static String requireSecret(String variableName) {
        String value = System.getenv(variableName); // getenv = pobierz zmienną środowiskową
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Brak sekretu w zmiennej środowiskowej: " + variableName);
        }
        return value;
    }

    /**
     * Zapis hasła w formacie  pbkdf2-sha256$iteracje$sól$skrót  (sól i skrót w Base64).
     * Klasa zagnieżdżona (nested class) — tylko do tej lekcji; w prawdziwym projekcie użyj
     * sprawdzonej biblioteki (np. PasswordEncoder ze Spring Security).
     */
    static final class PasswordHasher {
        private static final String PREFIX = "pbkdf2-sha256";
        private static final int KEY_BITS = 256; // długość klucza w BITACH

        private PasswordHasher() { }

        static String hash(char[] password, byte[] salt, int iterations) {
            byte[] derived = derive(password, salt, iterations);
            Base64.Encoder b64 = Base64.getEncoder().withoutPadding();
            return PREFIX + "$" + iterations + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(derived);
        }

        static String hashWithNewSalt(char[] password, int iterations) {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            return hash(password, salt, iterations);
        }

        static boolean verify(char[] password, String stored) {
            String[] parts = stored.split("\\$");
            if (parts.length != 4 || !PREFIX.equals(parts[0])) {
                return false;
            }
            try {
                int iterations = Integer.parseInt(parts[1]);
                byte[] salt = Base64.getDecoder().decode(parts[2]);
                byte[] expected = Base64.getDecoder().decode(parts[3]);
                byte[] actual = derive(password, salt, iterations);
                return MessageDigest.isEqual(expected, actual);
            } catch (IllegalArgumentException e) { // NumberFormatException to jego podklasa
                return false;
            }
        }

        private static byte[] derive(char[] password, byte[] salt, int iterations) {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS); // spec = opis parametrów
            try {
                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                return factory.generateSecret(spec).getEncoded();
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("PBKDF2 niedostępny", e);
            } finally {
                spec.clearPassword(); // kopia hasła w obiekcie spec jest zerowana
            }
        }
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • MessageDigest.getInstance("SHA-256"): update(...) i digest(); wynik to bajty → HexFormat.
     *   • Zmiana jednego bitu danych zmienia około połowy bitów skrótu (efekt lawiny).
     *   • MD5 i SHA-1: kolizje znalezione — nie do bezpieczeństwa. Wybieraj SHA-256 lub nowszy.
     *   • Duże pliki: pętla z buforem i update(bufor, 0, read) albo DigestInputStream.
     *   • Suma kontrolna chroni tylko wtedy, gdy pochodzi z innego, zaufanego kanału.
     *   • Sekrety porównuj przez MessageDigest.isEqual (stały czas), nie equals.
     *   • Hasła: sól losowa + wolna funkcja (PBKDF2 setki tysięcy iteracji; lepiej Argon2/bcrypt/scrypt
     *     z biblioteki); zapis algorytm$iteracje$sól$skrót; char[] i czyszczenie.
     *   • Tokeny: SecureRandom, 32 bajty, Base64 URL-safe; w bazie tylko skrót tokenu.
     *   • Random jest przewidywalny — nigdy do tajemnic; SecureRandom bez setSeed.
     *   • HMAC (Mac HmacSHA256) = skrót z kluczem: integralność i autentyczność; nie SHA(klucz+dane).
     *   • Nie pisz własnej kryptografii; nie ECB; klucze nie w kodzie; sekrety nie w logach.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego do przechowywania haseł nie wystarczy SHA-256, nawet z solą?
     *   2. Co daje sól? Czy musi być tajna?
     *   3. Co wypisze:  System.out.println(sha256Hex("a").length());  ?
     *   4. ZNAJDŹ BŁĄD:  while ((n = in.read(buf)) != -1) { md.update(buf); }
     *   5. Czym różni się HMAC od zwykłego skrótu? Do czego służy klucz?
     *   6. ZNAJDŹ BŁĄD:  String token = Long.toHexString(new Random().nextLong());  // token resetu hasła
     *   7. Co wypisze:  System.out.println(newToken().length());  ?
     *   8. Dlaczego w zapisie hasła umieszcza się liczbę iteracji i nazwę algorytmu?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: SHA-256 \"abc\"", ABC_HASH, () -> exercise1("abc"));
        Check.equal("ćw. 1: SHA-256 pustego tekstu", EMPTY_HASH, () -> exercise1(""));
        Check.equal("ćw. 2: poprawny skrót", true, () -> exercise2(PAYLOAD, sha256Hex("dane")));
        Check.equal("ćw. 2: wielkie litery", true, () -> exercise2(PAYLOAD, sha256Hex("dane").toUpperCase(Locale.ROOT)));
        Check.equal("ćw. 2: zmienione dane", false, () -> exercise2(PAYLOAD, sha256Hex("inne")));
        Check.equal("ćw. 2: zły zapis hex", false, () -> exercise2(PAYLOAD, "zz"));
        Check.equal("ćw. 3: HMAC (klucz \"key\")", FOX_HMAC, () -> exercise3("key", FOX));
        Check.equal("ćw. 3: HMAC (RFC 4231)", RFC_HMAC, () -> exercise3("Jefe", "what do ya want for nothing?"));
        Check.equal("ćw. 4: ta sama liczba iteracji", false, () -> exercise4(STORED, 10_000));
        Check.equal("ćw. 4: wymagane więcej iteracji", true, () -> exercise4(STORED, 600_000));
        Check.equal("ćw. 4: nieznany format", true, () -> exercise4("to-nie-jest-zapis", 10_000));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1: SHA-256 \"abc\"", ABC_HASH, () -> solution1("abc"));
        Check.equal("ćw. 1: SHA-256 pustego tekstu", EMPTY_HASH, () -> solution1(""));
        Check.equal("ćw. 2: poprawny skrót", true, () -> solution2(PAYLOAD, sha256Hex("dane")));
        Check.equal("ćw. 2: wielkie litery", true, () -> solution2(PAYLOAD, sha256Hex("dane").toUpperCase(Locale.ROOT)));
        Check.equal("ćw. 2: zmienione dane", false, () -> solution2(PAYLOAD, sha256Hex("inne")));
        Check.equal("ćw. 2: zły zapis hex", false, () -> solution2(PAYLOAD, "zz"));
        Check.equal("ćw. 3: HMAC (klucz \"key\")", FOX_HMAC, () -> solution3("key", FOX));
        Check.equal("ćw. 3: HMAC (RFC 4231)", RFC_HMAC, () -> solution3("Jefe", "what do ya want for nothing?"));
        Check.equal("ćw. 4: ta sama liczba iteracji", false, () -> solution4(STORED, 10_000));
        Check.equal("ćw. 4: wymagane więcej iteracji", true, () -> solution4(STORED, 600_000));
        Check.equal("ćw. 4: nieznany format", true, () -> solution4("to-nie-jest-zapis", 10_000));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 11 OK, ✘ 0 BŁĄD
    }

    private static final String ABC_HASH = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
    private static final String EMPTY_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final byte[] PAYLOAD = "dane".getBytes(StandardCharsets.UTF_8);
    private static final String FOX = "The quick brown fox jumps over the lazy dog";
    private static final String FOX_HMAC = "f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8";
    private static final String RFC_HMAC = "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843";
    private static final String STORED = PasswordHasher.hash("haslo123".toCharArray(),
            new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}, 10_000);

    /**
     * ĆWICZENIE 1 (łatwe): zwróć SHA-256 tekstu (UTF-8) jako 64 znaki hex małymi literami.
     * Podpowiedź: MessageDigest.getInstance("SHA-256").digest(bajty) i HexFormat.of().formatHex(...).
     */
    static String exercise1(String text) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ porównanie skrótów na bezpieczne. Stary kod:
     * <pre>{@code
     * return sha256Hex(data).equals(expectedHex);   // wielkość liter ma znaczenie, nie stały czas
     * }</pre>
     * Nowy kod ma obliczyć SHA-256 danych, zdekodować {@code expectedHex} (wielkość liter bez
     * znaczenia), porównać przez MessageDigest.isEqual, a dla błędnego zapisu hex zwrócić false.
     * Podpowiedź: HexFormat.parseHex rzuca IllegalArgumentException dla złego zapisu.
     */
    static boolean exercise2(byte[] data, String expectedHex) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): policz HMAC-SHA256 (klucz i wiadomość jako tekst UTF-8) i zwróć hex.
     * Podpowiedź: Mac.getInstance("HmacSHA256"), mac.init(new SecretKeySpec(klucz, "HmacSHA256")),
     * mac.doFinal(wiadomość). Wektory testowe: dokument RFC 4231 i klasyczny przykład z "lazy dog".
     */
    static String exercise3(String key, String message) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): "rehash on login". Dla zapisu hasła w formacie z sekcji 7
     * ({@code pbkdf2-sha256$iteracje$sól$skrót}) zwróć true, gdy zapis trzeba policzyć od nowa:
     * liczba iteracji jest MNIEJSZA niż minIterations albo zapis ma nieznany format.
     * Podpowiedź: split("\\$"), Integer.parseInt w try/catch dla NumberFormatException.
     */
    static boolean exercise4(String stored, int minIterations) {
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(String text) {
        return HexFormat.of().formatHex(sha256(text.getBytes(StandardCharsets.UTF_8)));
    }

    static boolean solution2(byte[] data, String expectedHex) {
        byte[] expected;
        try {
            expected = HexFormat.of().parseHex(expectedHex.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return false;
        }
        return MessageDigest.isEqual(sha256(data), expected);
    }

    static String solution3(String key, String message) {
        return hmacSha256Hex(key.getBytes(StandardCharsets.UTF_8), message);
    }

    static boolean solution4(String stored, int minIterations) {
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !"pbkdf2-sha256".equals(parts[0])) {
            return true;
        }
        try {
            return Integer.parseInt(parts[1]) < minIterations;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. SHA-256 jest SZYBKI: napastnik z kopią bazy próbuje miliardy haseł na sekundę, więc
     *      słabe hasła szybko odgadnie, nawet gdy zna sól. Potrzebna jest funkcja celowo wolna
     *      (PBKDF2 z wieloma iteracjami, bcrypt, scrypt, Argon2).
     *   2. Sól sprawia, że takie same hasła mają różne skróty i uniemożliwia użycie gotowych
     *      tablic skrótów. Nie musi być tajna — zapisuje się ją obok skrótu. Musi być losowa
     *      i inna dla każdego hasła.
     *   3. 64 (32 bajty zapisane jako 2 znaki hex na bajt).
     *   4. md.update(buf) dodaje CAŁY bufor, także stare dane z końca poprzedniej porcji.
     *      Poprawnie: md.update(buf, 0, n).
     *   5. HMAC wymaga tajnego klucza — bez niego nie da się policzyć poprawnego znacznika,
     *      więc potwierdza autentyczność i integralność. Zwykły skrót może policzyć każdy,
     *      także dla zmienionej wiadomości.
     *   6. Random jest przewidywalny; token resetu hasła musi pochodzić z SecureRandom
     *      (np. 32 bajty w Base64 URL-safe), a w bazie warto trzymać jego skrót.
     *   7. 43 (32 bajty, Base64 bez dopełnienia: ceil(32 * 4 / 3) = 43).
     *   8. Żeby po zmianie zaleceń stare hasła nadal dało się sprawdzić (parametry są w zapisie)
     *      i żeby po udanym logowaniu podmienić zapis na silniejszy (rehash).
     */
    // </editor-fold>
}
