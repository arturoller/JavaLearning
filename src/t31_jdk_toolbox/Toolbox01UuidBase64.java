package t31_jdk_toolbox;

import helpers.Check;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: UUID, Base64 i HexFormat — identyfikatory i zapis bajtów jako tekst
 *        (UUID = uniwersalnie unikalny identyfikator; Base64 = zapis bajtów 64 znakami;
 *         hex = zapis szesnastkowy)
 *
 * W SKRÓCIE:
 *   UUID to 128-bitowy numer, który można wygenerować bez pytania o zgodę żadnej bazy danych
 *   i prawie na pewno nikt inny nie wygeneruje takiego samego. Base64 i hex to dwa sposoby
 *   zapisania DOWOLNYCH bajtów jako zwykły, drukowalny tekst (do JSON-a, URL-a, nagłówka HTTP).
 *   To małe narzędzia, ale spotkasz je w każdym projekcie.
 *
 * ANALOGIA:
 *   UUID to numer paczki nadawany przez tysiące kurierów naraz, bez wspólnego rejestru.
 *   Numer jest tak długi i losowy, że kolizja jest praktycznie niemożliwa.
 *   Base64 to alfabet Morse'a dla bajtów: nie ukrywa wiadomości (każdy zna alfabet),
 *   tylko pozwala przesłać ją kanałem, który umie przenosić wyłącznie litery i cyfry.
 *
 * JAK TO DZIAŁA:
 *   UUID ma 128 bitów, zapisanych jako 32 cyfry szesnastkowe w grupach 8-4-4-4-12:
 *       123e4567-e89b-12d3-a456-426614174000
 *                      ^    ^
 *                      |    +-- pierwszy znak tej grupy koduje WARIANT (zwykle 8, 9, a lub b)
 *                      +------- pierwszy znak tej grupy to WERSJA (1, 3, 4, 5, 7 ...)
 *   Wersje: 1 = czas + adres sprzętowy, 3 = z nazwy (MD5), 4 = losowa, 5 = z nazwy (SHA-1),
 *   7 = czas + losowość (nowa; w JDK 17 jej nie ma). W JDK są: randomUUID (v4)
 *   i nameUUIDFromBytes (v3).
 *
 *   Base64: co 3 bajty (24 bity) dzielimy na 4 porcje po 6 bitów; każda porcja to indeks
 *   w alfabecie 64 znaków (A-Z a-z 0-9 + /). Gdy brakuje bajtów, dopisujemy '=' (padding).
 *       "Man" → 01001101 01100001 01101110 → 010011 010110 000101 101110 → T W F u
 *   Wynik jest o około 33% dłuższy od danych (4 znaki na 3 bajty).
 *
 *   Hex: każdy bajt to dokładnie 2 znaki (00 … ff), więc wynik jest 2 razy dłuższy.
 *
 * SŁÓWKA:
 *   UUID = universally unique identifier; random = losowy; name-based = z nazwy;
 *   encode = zakoduj; decode = odkoduj; padding = dopełnienie; charset = zestaw znaków;
 *   URL-safe = bezpieczny w adresie URL; checksum = suma kontrolna; hash = skrót (funkcja skrótu).
 *
 * ZOBACZ TEŻ: t18_io_files/Io12Charsets (dlaczego tekst → bajty wymaga zestawu znaków),
 *   t18_io_files/Io13ZipArchives (CRC32 w archiwach),
 *   t31_jdk_toolbox/Toolbox02HashingSecurity (skróty kryptograficzne, HMAC)
 * </pre>
 */
public class Toolbox01UuidBase64 {

    /** Ścisły wzorzec zapisu UUID: 8-4-4-4-12 cyfr szesnastkowych (Pattern = wzorzec). */
    private static final Pattern STRICT_UUID =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    /** Stały UUID z przykładu z Wikipedii (wersja 1) — do doświadczeń, bo jest zawsze taki sam. */
    private static final UUID FIXED = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    public static void main(String[] args) {
        title("Toolbox01 — UUID, Base64 i HexFormat");

        randomUuid();            // random UUID = losowy UUID
        nameBasedUuid();         // name based = z nazwy
        uuidStructure();         // structure = budowa
        uuidParsing();           // parsing = parsowanie
        uuidAsDatabaseKey();     // database key = klucz w bazie danych
        base64Basics();          // basics = podstawy
        base64Variants();        // variants = warianty
        base64Errors();          // errors = błędy
        base64IsNotEncryption(); // is not encryption = to nie jest szyfrowanie
        hexAndCrc();             // hex and CRC = szesnastkowo i CRC
        exercises();             // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. LOSOWY UUID
    // =================================================================================================

    /**
     * 1. {@code UUID.randomUUID()} (random = losowy) daje UUID wersji 4: 122 bity losowe, 6 bitów
     * stałych (wersja i wariant). Losowość pochodzi z {@code SecureRandom} (bezpieczny generator,
     * patrz Toolbox02). Konkretnych wartości NIE drukujemy — za każdym razem są inne;
     * sprawdzamy tylko ich cechy.
     */
    static void randomUuid() {
        section("1. Losowy UUID (wersja 4)");

        UUID id = UUID.randomUUID(); // randomUUID = losowy UUID
        String text = id.toString(); // toString = jako tekst

        show("długość zapisu", text.length());
        // WYNIK: długość zapisu → 36
        show("wersja (version = wersja)", id.version());
        // WYNIK: wersja (version = wersja) → 4
        show("wariant (variant = odmiana układu bitów)", id.variant());
        // WYNIK: wariant (variant = odmiana układu bitów) → 2
        show("pasuje do wzorca 8-4-4-4-12", STRICT_UUID.matcher(text).matches());
        // WYNIK: pasuje do wzorca 8-4-4-4-12 → true
        show("zapis bez myślników", text.replace("-", "").length());
        // WYNIK: zapis bez myślników → 32
        show("dwa kolejne UUID są różne", !UUID.randomUUID().equals(UUID.randomUUID()));
        // WYNIK: dwa kolejne UUID są różne → true
        show("toString zawsze pisze małymi literami", text.equals(text.toLowerCase(Locale.ROOT)));
        // WYNIK: toString zawsze pisze małymi literami → true

        // JAK DUŻA JEST SZANSA KOLIZJI (dwa takie same UUID)?
        // Losowych bitów jest 122, więc możliwych UUID jest 2^122, czyli około 5 * 10^36.
        // Paradoks urodzin mówi, że szansa 50% na choć jedną kolizję pojawia się dopiero po
        // około 2,7 * 10^18 wygenerowanych identyfikatorów. To tyle, jakbyś generował miliard UUID
        // na sekundę przez około 85 lat. W praktyce kolizja UUID v4 to nie jest realne ryzyko —
        // dużo bardziej prawdopodobne są błędy w kodzie i awarie sprzętu.

        // PUŁAPKA: "unikalny" nie znaczy "tajny" ani "nie do odgadnięcia w każdej sytuacji".
        // Dlaczego: UUID v4 z randomUUID() jest nieprzewidywalny, ale UUID z nazwy (v3) czy
        // z czasu (v1) już tak. Nie traktuj samego UUID jako hasła ani zabezpieczenia dostępu;
        // do tokenów sesji używaj dedykowanych, długich losowych wartości (Toolbox02).

        // DOBRA PRAKTYKA: generuj UUID w aplikacji (randomUUID), nie "wymyślaj" własnych.
        // Dlaczego: ręcznie składane identyfikatory (czas + licznik + numer maszyny) łatwo
        // dają kolizje po restarcie lub skopiowaniu środowiska.
    }

    // =================================================================================================
    // 2. UUID Z NAZWY (WERSJA 3)
    // =================================================================================================

    /**
     * 2. {@code UUID.nameUUIDFromBytes(bytes)} (name UUID from bytes = UUID z bajtów nazwy) liczy
     * skrót MD5 podanych bajtów i ustawia bity wersji (3) i wariantu. Ta sama nazwa daje ZAWSZE
     * ten sam UUID — to przydatne, gdy chcesz stały identyfikator dla znanego klucza naturalnego.
     */
    static void nameBasedUuid() {
        section("2. UUID z nazwy (wersja 3) — deterministyczny");

        UUID a = UUID.nameUUIDFromBytes("kurs-java".getBytes(StandardCharsets.UTF_8));
        UUID b = UUID.nameUUIDFromBytes("kurs-java".getBytes(StandardCharsets.UTF_8));
        UUID c = UUID.nameUUIDFromBytes("kurs-spring".getBytes(StandardCharsets.UTF_8));

        show("UUID dla \"kurs-java\"", a);
        // WYNIK: UUID dla "kurs-java" → 21084440-b850-33d0-a674-49d3c804db89
        show("wersja", a.version());
        // WYNIK: wersja → 3
        show("to samo wejście → ten sam UUID", a.equals(b));
        // WYNIK: to samo wejście → ten sam UUID → true
        show("inne wejście → inny UUID", !a.equals(c));
        // WYNIK: inne wejście → inny UUID → true

        // Zastosowanie: import danych. Wczytujesz plik raz po raz i nie chcesz dublować rekordów.
        // Jeśli identyfikator liczysz z klucza naturalnego (np. z numeru faktury), to powtórny
        // import trafi na ten sam identyfikator — operacja jest idempotentna (powtórzenie nic
        // nie psuje).

        // PUŁAPKA: JDK liczy MD5 samych bajtów nazwy, BEZ identyfikatora przestrzeni nazw (namespace).
        // Dlaczego: standard (RFC) przewiduje, że do skrótu dołącza się jeszcze UUID "przestrzeni",
        // więc uuid3(NAMESPACE_DNS, "x") z Pythona da INNY wynik niż nameUUIDFromBytes("x").
        // Gdy dane wymieniasz z innym systemem, uzgodnij sposób liczenia i go przetestuj.

        // PUŁAPKA: v3 opiera się na MD5, a w JDK nie ma wersji 5 (SHA-1).
        // Dlaczego: MD5 jest złamany pod kątem bezpieczeństwa (Toolbox02). Tu to nie szkodzi,
        // bo UUID z nazwy służy do identyfikacji, a nie do ochrony — ale też nie wolno go używać
        // jako tajnego tokena: każdy, kto zna nazwę, policzy ten sam UUID.
    }

    // =================================================================================================
    // 3. BUDOWA UUID
    // =================================================================================================

    /**
     * 3. UUID to dwie liczby {@code long} (po 64 bity): starsza połowa ("most significant bits")
     * i młodsza ("least significant bits"). Wersja leży w starszej połowie, wariant w młodszej.
     */
    static void uuidStructure() {
        section("3. Budowa UUID: dwie liczby long, wersja i wariant");

        long most = FIXED.getMostSignificantBits();   // most significant = najbardziej znaczące bity
        long least = FIXED.getLeastSignificantBits(); // least significant = najmniej znaczące bity
        show("starsza połowa (hex)", Long.toHexString(most));
        // WYNIK: starsza połowa (hex) → 123e4567e89b12d3
        show("młodsza połowa (hex)", Long.toHexString(least));
        // WYNIK: młodsza połowa (hex) → a456426614174000
        show("odbudowany z dwóch long", new UUID(most, least).equals(FIXED));
        // WYNIK: odbudowany z dwóch long → true

        String text = FIXED.toString();
        show("znak wersji (indeks 14)", text.charAt(14));
        // WYNIK: znak wersji (indeks 14) → 1
        show("version()", FIXED.version());
        // WYNIK: version() → 1
        show("znak wariantu (indeks 19)", text.charAt(19));
        // WYNIK: znak wariantu (indeks 19) → a
        show("variant()", FIXED.variant());
        // WYNIK: variant() → 2
        // Wariant 2 ("Leach–Salz") to zwykły, dzisiejszy układ UUID. W zapisie tekstowym
        // pierwszy znak czwartej grupy to wtedy 8, 9, a albo b.

        UUID random = UUID.randomUUID();
        show("losowy: znak wersji to 4", random.toString().charAt(14) == '4');
        // WYNIK: losowy: znak wersji to 4 → true
        show("losowy: wariant to 8, 9, a lub b", "89ab".indexOf(random.toString().charAt(19)) >= 0);
        // WYNIK: losowy: wariant to 8, 9, a lub b → true

        // Wersja 1 (czas): z UUID da się odczytać znacznik czasu — liczbę setek nanosekund
        // od 15 października 1582 (początek kalendarza gregoriańskiego).
        show("timestamp() dla wersji 1", FIXED.timestamp());
        // WYNIK: timestamp() dla wersji 1 → 203762160885450087
        // PUŁAPKA: timestamp(), clockSequence() i node() działają TYLKO dla wersji 1.
        // Dlaczego: w UUID v4 te bity to losowość, nie czas, więc JDK rzuca wyjątek zamiast
        // zwracać bezsensowną liczbę.
        expectThrows("timestamp() losowego UUID", () -> UUID.randomUUID().timestamp());
        // WYNIK: ✔ timestamp() losowego UUID → rzucono UnsupportedOperationException: Not a time-based UUID

        // WSKAZÓWKA: UUID v1 zdradza adres sprzętowy komputera i czas utworzenia — to wyciek
        // informacji. Dlatego w aplikacjach biznesowych zwykle wybiera się v4 (losowy)
        // albo v7 (czas + losowość, o czym w sekcji 5).
    }

    // =================================================================================================
    // 4. PARSOWANIE I WALIDACJA
    // =================================================================================================

    /**
     * 4. {@code UUID.fromString(text)} (from string = ze znaków) zamienia tekst na UUID.
     * Uwaga: jest zaskakująco "wyrozumiały" i nie nadaje się do ścisłego sprawdzania formatu.
     */
    static void uuidParsing() {
        section("4. fromString i ścisła walidacja");

        UUID upper = UUID.fromString("123E4567-E89B-12D3-A456-426614174000");
        show("wielkie litery są przyjmowane", upper.equals(FIXED));
        // WYNIK: wielkie litery są przyjmowane → true
        show("a toString i tak da małe", upper);
        // WYNIK: a toString i tak da małe → 123e4567-e89b-12d3-a456-426614174000

        // PUŁAPKA: fromString przyjmuje także zapis z "za krótkimi" grupami.
        // Dlaczego: parser szuka tylko pięciu grup oddzielonych myślnikami i dopełnia je zerami,
        // a nie pilnuje długości 8-4-4-4-12. Dane od użytkownika przejdą, choć wcale nie
        // wyglądają jak UUID.
        show("fromString(\"1-1-1-1-1\")", UUID.fromString("1-1-1-1-1"));
        // WYNIK: fromString("1-1-1-1-1") → 00000001-0001-0001-0001-000000000001

        expectThrows("fromString(\"abc\")", () -> UUID.fromString("abc"));
        // WYNIK: ✔ fromString("abc") → rzucono IllegalArgumentException: Invalid UUID string: abc
        expectThrows("fromString z literami spoza hex", () -> UUID.fromString("zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz"));
        // WYNIK: ✔ fromString z literami spoza hex → rzucono NumberFormatException: Error at index 0 in: "zzzzzzzz"
        // Obie niespodzianki to IllegalArgumentException (druga to jego podklasa
        // NumberFormatException). To wyjątki NIESPRAWDZANE (unchecked) — trzeba je łapać
        // samemu, gdy tekst pochodzi z zewnątrz (żądanie HTTP, plik).

        show("ścisła walidacja \"1-1-1-1-1\"", isStrictUuid("1-1-1-1-1"));
        // WYNIK: ścisła walidacja "1-1-1-1-1" → false
        show("ścisła walidacja poprawnego", isStrictUuid("123e4567-e89b-12d3-a456-426614174000"));
        // WYNIK: ścisła walidacja poprawnego → true
        show("ścisła walidacja z dopiskiem", isStrictUuid("123e4567-e89b-12d3-a456-426614174000 "));
        // WYNIK: ścisła walidacja z dopiskiem → false
        show("tryParse(\"nie-uuid\")", tryParse("nie-uuid"));
        // WYNIK: tryParse("nie-uuid") → Optional.empty
        show("tryParse poprawnego", tryParse("123e4567-e89b-12d3-a456-426614174000"));
        // WYNIK: tryParse poprawnego → Optional[123e4567-e89b-12d3-a456-426614174000]

        // DOBRA PRAKTYKA: najpierw sprawdź wzorzec (regex), potem wołaj fromString.
        // Dlaczego: dostajesz jednoznaczną odpowiedź "tak/nie" bez łapania wyjątków,
        // a jednocześnie odrzucasz dziwne zapisy typu 1-1-1-1-1.
    }

    /** Czy tekst to UUID w kanonicznym zapisie 8-4-4-4-12 (matches = pasuje w całości). */
    static boolean isStrictUuid(String text) {
        return text != null && STRICT_UUID.matcher(text).matches();
    }

    /** Bezpieczne parsowanie: Optional.empty() zamiast wyjątku (Optional = coś albo nic). */
    static Optional<UUID> tryParse(String text) {
        return isStrictUuid(text) ? Optional.of(UUID.fromString(text)) : Optional.empty();
    }

    // =================================================================================================
    // 5. UUID JAKO KLUCZ W BAZIE DANYCH
    // =================================================================================================

    /**
     * 5. Rozmiar, kolejność i pułapki porównywania. UUID jako klucz główny ma zalety i wady —
     * warto je znać przed decyzją.
     */
    static void uuidAsDatabaseKey() {
        section("5. UUID jako klucz w bazie danych");

        byte[] bytes = uuidToBytes(FIXED);
        show("rozmiar binarny UUID (bajty)", bytes.length);
        // WYNIK: rozmiar binarny UUID (bajty) → 16
        show("rozmiar zapisu tekstowego (znaki)", FIXED.toString().length());
        // WYNIK: rozmiar zapisu tekstowego (znaki) → 36
        show("rozmiar liczby long (bajty)", Long.BYTES);
        // WYNIK: rozmiar liczby long (bajty) → 8
        show("hex tych 16 bajtów", HexFormat.of().formatHex(bytes));
        // WYNIK: hex tych 16 bajtów → 123e4567e89b12d3a456426614174000

        // ZALETY UUID jako klucza:
        //  + generujesz go po stronie aplikacji, bez zapytania do bazy (łatwe wstawianie wsadowe,
        //    łączenie danych z wielu systemów, praca offline),
        //  + nie zdradza liczby rekordów ani kolejności (id=1001 ujawnia, że masz ~1000 zamówień),
        //  + trudniej "pójść po kolei" po adresach /zamowienia/1, /zamowienia/2 ... UWAGA: to nie
        //    zastępuje sprawdzania uprawnień! Zgadywalność identyfikatora nie jest autoryzacją.
        // WADY:
        //  - 16 bajtów zamiast 8: większe tabele, a każdy indeks i każdy klucz obcy też rośnie,
        //  - zapis jako tekst CHAR(36) to aż 36 bajtów (ponad 2 razy więcej niż binarnie) —
        //    używaj natywnego typu bazy (np. uuid w PostgreSQL) albo BINARY(16),
        //  - UUID v4 jest losowy, więc nowe wiersze trafiają w LOSOWE miejsca indeksu (B-drzewa).
        //    To oznacza dzielenie stron, gorsze wykorzystanie pamięci podręcznej i wolniejsze
        //    wstawianie przy dużych tabelach. Klucz rosnący (sekwencja) dopisuje zawsze na końcu.
        //  - trudniej go przeczytać i przepisać "z ekranu" przy diagnozowaniu błędów.
        //
        // WERSJA 7 (pomysł): pierwsze 48 bitów to czas w milisekundach, reszta losowa. Takie UUID
        // rosną w czasie, więc indeks zachowuje się prawie jak przy sekwencji, a jednocześnie
        // nadal można je generować bez bazy. W JDK 17 wersji 7 NIE MA — używa się biblioteki
        // albo własnego generatora. Warto o tym pamiętać, gdy w projekcie klucze UUID zaczną
        // spowalniać duże tabele.

        // PUŁAPKA: UUID.compareTo porównuje połówki jako liczby ZE ZNAKIEM (signed long).
        // Dlaczego: UUID zaczynający się od 8..f ma w starszej połowie bit znaku równy 1,
        // czyli liczbę ujemną, i według compareTo jest MNIEJSZY niż UUID zaczynający się od 7.
        // Porównanie tekstów daje odwrotny wynik, bo "8" jest po "7". W JDK 17 sortowanie
        // UUID w kolekcji i sortowanie ich zapisów tekstowych to dwie różne kolejności.
        UUID high = UUID.fromString("80000000-0000-0000-0000-000000000000");
        UUID low = UUID.fromString("7fffffff-0000-0000-0000-000000000000");
        show("UUID 8... compareTo UUID 7... (ujemne = mniejszy)", Integer.signum(high.compareTo(low)));
        // WYNIK: UUID 8... compareTo UUID 7... (ujemne = mniejszy) → -1
        show("to samo dla zapisów tekstowych", Integer.signum(high.toString().compareTo(low.toString())));
        // WYNIK: to samo dla zapisów tekstowych → 1
        // Skutek praktyczny: jeśli baza sortuje po kolumnie uuid inaczej niż Java po obiektach
        // UUID, nie opieraj logiki (np. stronicowania) na zgodności tych dwóch porządków.

        // DOBRA PRAKTYKA: kolumna typu uuid/BINARY(16), a w kodzie obiekt UUID — nie String.
        // Dlaczego: oszczędzasz miejsce i dostajesz walidację typu już na wejściu do metody.
    }

    /** UUID → 16 bajtów (ByteBuffer = bufor bajtów; putLong zapisuje 8 bajtów, kolejność big-endian). */
    static byte[] uuidToBytes(UUID id) {
        return ByteBuffer.allocate(16)
                .putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits())
                .array();
    }

    // =================================================================================================
    // 6. BASE64 — PODSTAWY
    // =================================================================================================

    /**
     * 6. {@code Base64.getEncoder()} (get encoder = pobierz koder) zamienia bajty na tekst.
     * Przyjmuje BAJTY, nie napisy — tekst trzeba najpierw zamienić na bajty z jawnym zestawem znaków.
     */
    static void base64Basics() {
        section("6. Base64 — podstawy");

        Base64.Encoder encoder = Base64.getEncoder();
        for (String s : new String[] {"M", "Ma", "Man"}) {
            show("\"" + s + "\"", encoder.encodeToString(s.getBytes(StandardCharsets.UTF_8)));
        }
        // WYNIK: "M" → TQ==
        // WYNIK: "Ma" → TWE=
        // WYNIK: "Man" → TWFu
        // "M"   = 1 bajt  → 2 znaki + "==" (dwa brakujące bajty)
        // "Ma"  = 2 bajty → 3 znaki + "="  (jeden brakujący bajt)
        // "Man"  = 3 bajty → 4 znaki, bez dopełnienia
        // Wzór na długość wyniku (z paddingiem): 4 * ceil(n / 3), w liczbach całkowitych:
        // 4 * ((n + 2) / 3).
        boolean allMatch = true;
        for (int n = 0; n <= 20; n++) {
            int expected = 4 * ((n + 2) / 3);
            allMatch &= encoder.encodeToString(new byte[n]).length() == expected;
        }
        show("długość = 4 * ((n + 2) / 3) dla n = 0..20", allMatch);
        // WYNIK: długość = 4 * ((n + 2) / 3) dla n = 0..20 → true

        // TEKST → BAJTY → BASE64 → BAJTY → TEKST. Zestaw znaków podajemy ZAWSZE jawnie.
        String text = "Zażółć gęślą jaźń";
        byte[] utf8 = text.getBytes(StandardCharsets.UTF_8); // getBytes = daj bajty
        String encoded = encoder.encodeToString(utf8);
        show("tekst (znaki)", text.length());
        // WYNIK: tekst (znaki) → 17
        show("tekst jako UTF-8 (bajty)", utf8.length);
        // WYNIK: tekst jako UTF-8 (bajty) → 26
        show("po Base64", encoded);
        // WYNIK: po Base64 → WmHFvMOzxYLEhyBnxJnFm2zEhSBqYcW6xYQ=
        String back = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        show("odkodowany tekst", back);
        // WYNIK: odkodowany tekst → Zażółć gęślą jaźń
        show("odkodowany == oryginał", back.equals(text));
        // WYNIK: odkodowany == oryginał → true

        // PUŁAPKA: kodowanie bajtów w innym zestawie znaków daje INNY wynik Base64.
        // Dlaczego: Base64 koduje bajty, a "ż" ma inne bajty w UTF-8 niż w ISO-8859-2 czy Windows-1250.
        // Gdy nadawca użył jednego zestawu, a odbiorca drugiego, polskie litery się zepsują.
        // Najgorzej, gdy użyjesz getBytes() BEZ argumentu: bierze zestaw domyślny, a ten zależy od
        // komputera (w JDK 17 na Windows bywa to Windows-1250) — ten sam kod na dwóch maszynach
        // da dwa różne wyniki. Zawsze: getBytes(StandardCharsets.UTF_8) i new String(bajty, UTF_8).
        Charset latin2 = Charset.forName("ISO-8859-2"); // forName = znajdź po nazwie
        String encodedLatin2 = encoder.encodeToString(text.getBytes(latin2));
        show("Base64 bajtów ISO-8859-2", encodedLatin2);
        // WYNIK: Base64 bajtów ISO-8859-2 → WmG/87PmIGfqtmyxIGphvPE=
        show("czy to samo co dla UTF-8?", encodedLatin2.equals(encoded));
        // WYNIK: czy to samo co dla UTF-8? → false

        // DOBRA PRAKTYKA: protokół (API, plik) powinien jasno mówić "Base64 z bajtów UTF-8".
        // Dlaczego: sam ciąg Base64 nie zawiera informacji o zestawie znaków.
    }

    // =================================================================================================
    // 7. WARIANTY BASE64
    // =================================================================================================

    /**
     * 7. JDK ma trzy kodery: podstawowy (basic), bezpieczny w adresach URL (URL-safe)
     * i do poczty (MIME). Różnią się alfabetem i łamaniem wierszy.
     */
    static void base64Variants() {
        section("7. Warianty: basic, URL-safe, MIME, bez paddingu");

        // Bajty, które w Base64 dają znaki + i / (indeksy 62 i 63 w alfabecie).
        byte[] special = {(byte) 0xfb, (byte) 0xff, (byte) 0xfe};
        show("basic    (+ i /)", Base64.getEncoder().encodeToString(special));
        // WYNIK: basic    (+ i /) → +//+
        show("URL-safe (- i _)", Base64.getUrlEncoder().encodeToString(special));
        // WYNIK: URL-safe (- i _) → -__-
        // Dlaczego osobny wariant: w adresie URL znak + oznacza spację (w zapytaniu), / dzieli
        // ścieżkę, a = rozdziela parametry. Zwykły Base64 wstawiony do URL-a bywa przez to
        // zepsuty. Wariant URL-safe zamienia + na - oraz / na _. Używa go np. JWT.

        // Padding (dopełnienie '=') bywa zbędny, bo długość danych da się wyliczyć z długości tekstu.
        byte[] one = "M".getBytes(StandardCharsets.UTF_8);
        show("z paddingiem", Base64.getUrlEncoder().encodeToString(one));
        // WYNIK: z paddingiem → TQ==
        show("withoutPadding (bez dopełnienia)", Base64.getUrlEncoder().withoutPadding().encodeToString(one));
        // WYNIK: withoutPadding (bez dopełnienia) → TQ
        show("dekoder przyjmuje oba zapisy", Base64.getUrlDecoder().decode("TQ").length
                + " i " + Base64.getUrlDecoder().decode("TQ==").length);
        // WYNIK: dekoder przyjmuje oba zapisy → 1 i 1
        // Zwykły dekoder akceptuje brak paddingu, ale NIE akceptuje niepełnego paddingu ("TQ=").

        // MIME: po każdych 76 znakach wstawia znak końca wiersza CRLF (\r\n), bo stare serwery
        // poczty nie znosiły długich wierszy. Dekoder MIME ignoruje znaki spoza alfabetu.
        String mime = Base64.getMimeEncoder().encodeToString(new byte[60]);
        show("MIME (CRLF pokazany jako <CRLF>)", mime.replace("\r\n", "<CRLF>"));
        // WYNIK: MIME (CRLF pokazany jako <CRLF>) → AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA<CRLF>AAAA
        show("długość pierwszego wiersza", mime.split("\r\n")[0].length());
        // WYNIK: długość pierwszego wiersza → 76
        show("MIME-dekoder ignoruje śmieci (TQ==!!)", Base64.getMimeDecoder().decode("TQ==!!").length);
        // WYNIK: MIME-dekoder ignoruje śmieci (TQ==!!) → 1
        // PUŁAPKA: koder MIME dołożony "przez przypadek" łamie wiersze w danych, które trafiają
        // do JSON-a lub nagłówka HTTP. Dlaczego: znak \r\n w nagłówku to błąd lub atak
        // (wstrzyknięcie nagłówka). Do API zawsze używaj getEncoder() albo getUrlEncoder().

        // Kiedy który wariant:
        //   getEncoder()      — pola JSON, XML, ogólne zastosowanie, nagłówek Authorization
        //   getUrlEncoder()   — wszystko, co ląduje w adresie URL, nazwie pliku lub tokenie
        //   getMimeEncoder()  — załączniki e-mail i stare formaty tekstowe (PEM używa wierszy po 64)

        // DOBRA PRAKTYKA: dla tokenów i identyfikatorów używaj getUrlEncoder().withoutPadding().
        // Dlaczego: wynik zawiera tylko znaki A-Z a-z 0-9 - _, więc bezpiecznie wstawisz go
        // w URL, ciasteczko i nazwę pliku bez dodatkowego kodowania.
    }

    // =================================================================================================
    // 8. BŁĘDY DEKODOWANIA
    // =================================================================================================

    /**
     * 8. Dekoder rzuca {@code IllegalArgumentException} (nielegalny argument) dla tekstu, który nie
     * jest poprawnym Base64. Komunikaty różnią się zależnie od rodzaju błędu.
     */
    static void base64Errors() {
        section("8. Błędy dekodowania");

        Base64.Decoder decoder = Base64.getDecoder();
        expectThrows("znak spoza alfabetu ('-' w zwykłym dekoderze)", () -> decoder.decode("-__-"));
        // WYNIK: ✔ znak spoza alfabetu ('-' w zwykłym dekoderze) → rzucono IllegalArgumentException: Illegal base64 character 2d
        expectThrows("niepełne dopełnienie", () -> decoder.decode("TQ="));
        // WYNIK: ✔ niepełne dopełnienie → rzucono IllegalArgumentException: Input byte array has wrong 4-byte ending unit
        expectThrows("pojedynczy znak", () -> decoder.decode("T"));
        // WYNIK: ✔ pojedynczy znak → rzucono IllegalArgumentException: Input byte[] should at least have 2 bytes for base64 bytes
        expectThrows("dane po paddingu", () -> decoder.decode("TQ==TQ=="));
        // WYNIK: ✔ dane po paddingu → rzucono IllegalArgumentException: Input byte array has incorrect ending byte at 4
        // Komunikat "Illegal base64 character 2d" podaje kod znaku szesnastkowo: 2d to myślnik.
        // Najczęstsza przyczyna to pomylenie wariantów: tekst z getUrlEncoder() podany zwykłemu
        // dekoderowi. Dekoder musi pasować do kodera.
        show("poprawny URL-safe przez dekoder URL", Base64.getUrlDecoder().decode("-__-").length);
        // WYNIK: poprawny URL-safe przez dekoder URL → 3

        show("safeDecode(\"TWFu\")", safeDecode("TWFu").map(b -> new String(b, StandardCharsets.UTF_8)));
        // WYNIK: safeDecode("TWFu") → Optional[Man]
        show("safeDecode(\"%%%\")", safeDecode("%%%").isPresent());
        // WYNIK: safeDecode("%%%") → false
        show("safeDecode(null)", safeDecode(null).isPresent());
        // WYNIK: safeDecode(null) → false

        // PUŁAPKA: dekoder nie sprawdza, czy to, co dostałeś, ma sens po odkodowaniu.
        // Dlaczego: "SGVsbG8=" jest poprawnym Base64, ale nie wiesz, czy to tekst, obrazek czy
        // plik wykonywalny. Po odkodowaniu sprawdzaj rozmiar i format, zanim użyjesz danych.
        // Szczególnie ogranicz rozmiar wejścia, zanim je odkodujesz — dane od użytkownika
        // mogą być ogromne.

        // DOBRA PRAKTYKA: dane z zewnątrz dekoduj w jednym miejscu, łap IllegalArgumentException
        // i zamieniaj go na własny, czytelny błąd (np. "nieprawidłowy token").
        // Dlaczego: wewnętrzny komunikat dekodera nie jest przeznaczony dla użytkownika.
    }

    /** Odkodowuje Base64 albo zwraca Optional.empty() dla null i błędnego zapisu. */
    static Optional<byte[]> safeDecode(String text) {
        if (text == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Base64.getDecoder().decode(text));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    // =================================================================================================
    // 9. BASE64 TO NIE SZYFROWANIE
    // =================================================================================================

    /**
     * 9. Base64 nie ma klucza ani tajemnicy: każdy odkoduje go jednym wywołaniem. Pokazujemy to na
     * trzech realnych przykładach: nagłówek Basic, adres data: i token JWT.
     */
    static void base64IsNotEncryption() {
        section("9. Base64 to NIE szyfrowanie");

        // Przykład 1: nagłówek HTTP "Authorization: Basic ...".
        // Format: "Basic " + Base64("login:hasło"). Dane to fikcyjne konto demonstracyjne.
        String credentials = "ala:tajne123";
        String header = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        show("nagłówek Authorization", header);
        // WYNIK: nagłówek Authorization → Basic YWxhOnRham5lMTIz
        String decoded = new String(Base64.getDecoder().decode(header.substring("Basic ".length())),
                StandardCharsets.UTF_8); // substring = wytnij od indeksu
        show("podsłuchujący odkoduje to bez klucza", decoded);
        // WYNIK: podsłuchujący odkoduje to bez klucza → ala:tajne123
        // PUŁAPKA: "zakodowane w Base64, więc bezpieczne". To mit.
        // Dlaczego: Base64 to tylko inny zapis tych samych danych. Nagłówek Basic jest bezpieczny
        // WYŁĄCZNIE wtedy, gdy całe połączenie jest szyfrowane (HTTPS/TLS). Nigdy nie wysyłaj go
        // zwykłym HTTP, nie zapisuj w logach i nie wklejaj do zgłoszeń błędów.

        // Przykład 2: adres danych (data URL) — mały plik wklejony wprost w tekst.
        // Format: data:<typ>;base64,<dane>
        String dataUrl = "data:text/plain;charset=UTF-8;base64,"
                + Base64.getEncoder().encodeToString("Cześć!".getBytes(StandardCharsets.UTF_8));
        show("data URL", dataUrl);
        // WYNIK: data URL → data:text/plain;charset=UTF-8;base64,Q3plxZvEhyE=
        String payload = dataUrl.substring(dataUrl.indexOf("base64,") + "base64,".length()); // indexOf = znajdź
        show("odkodowana zawartość", new String(Base64.getDecoder().decode(payload), StandardCharsets.UTF_8));
        // WYNIK: odkodowana zawartość → Cześć!
        // Takie adresy wygodnie osadzają małe ikony w HTML/CSS (mniej zapytań), ale: dane rosną
        // o 33%, przeglądarka nie zapamięta ich osobno, a duże pliki to zły pomysł.

        // Przykład 3: token JWT składa się z trzech części oddzielonych kropkami; każda to
        // Base64 w wariancie URL-safe bez paddingu. Środkowa część (payload = ładunek) to zwykły JSON.
        String json = "{\"sub\":\"42\",\"rola\":\"admin\"}";
        String part = Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        show("środkowa część tokena", part);
        // WYNIK: środkowa część tokena → eyJzdWIiOiI0MiIsInJvbGEiOiJhZG1pbiJ9
        show("każdy ją odczyta", new String(Base64.getUrlDecoder().decode(part), StandardCharsets.UTF_8));
        // WYNIK: każdy ją odczyta → {"sub":"42","rola":"admin"}
        // Podpis (trzecia część) chroni przed ZMIANĄ treści (HMAC, Toolbox02), ale nie przed jej
        // PODEJRZENIEM. Dlatego w JWT nie umieszcza się haseł ani danych poufnych.

        // Koszt: Base64 powiększa dane o jedną trzecią.
        int rawSize = 3000;
        show("3000 bajtów po Base64 (znaki)", Base64.getEncoder().encodeToString(new byte[rawSize]).length());
        // WYNIK: 3000 bajtów po Base64 (znaki) → 4000
        // Duże pliki wysyłaj jako multipart/binarnie, a nie jako Base64 w JSON-ie.

        // DOBRA PRAKTYKA: do poufności używaj prawdziwego szyfrowania (np. AES-GCM, TLS), do
        // integralności — HMAC lub podpisu; Base64 służy tylko do TRANSPORTU bajtów jako tekstu.
    }

    // =================================================================================================
    // 10. HEXFORMAT I CRC32
    // =================================================================================================

    /**
     * 10. {@code HexFormat} (Java 17+) to oficjalny sposób zapisu bajtów szesnastkowo.
     * Zastępuje ręczne pętle z {@code String.format("%02x")}. CRC32 to szybka suma kontrolna
     * — nie mylić ze skrótem kryptograficznym.
     */
    static void hexAndCrc() {
        section("10. HexFormat (Java 17+) i CRC32 a skrót");

        byte[] bytes = {0, 15, (byte) 255, 16, -1};
        HexFormat hex = HexFormat.of(); // of = "domyślny": małe litery, bez separatora
        show("formatHex (format hex = zapisz szesnastkowo)", hex.formatHex(bytes));
        // WYNIK: formatHex (format hex = zapisz szesnastkowo) → 000fff10ff
        show("wielkie litery", HexFormat.of().withUpperCase().formatHex(bytes));
        // WYNIK: wielkie litery → 000FFF10FF
        show("z separatorem", HexFormat.ofDelimiter(":").formatHex(bytes));
        // WYNIK: z separatorem → 00:0f:ff:10:ff
        show("parseHex (parse = odczytaj)", Arrays.toString(hex.parseHex("cafebabe")));
        // WYNIK: parseHex (parse = odczytaj) → [-54, -2, -70, -66]
        show("toHexDigits(bajt)", hex.toHexDigits((byte) 10));
        // WYNIK: toHexDigits(bajt) → 0a
        show("fromHexDigits(\"ff\")", HexFormat.fromHexDigits("ff"));
        // WYNIK: fromHexDigits("ff") → 255
        expectThrows("parseHex z nieparzystą liczbą znaków", () -> hex.parseHex("abc"));
        // WYNIK: ✔ parseHex z nieparzystą liczbą znaków → rzucono IllegalArgumentException: string length not even: 3

        // PUŁAPKA: stare sposoby zapisu hex mają pułapki, których HexFormat nie ma.
        // 1) Integer.toHexString(bajt) rozszerza bajt ze znakiem do 32 bitów:
        show("toHexString((byte) -1)", Integer.toHexString((byte) -1));
        // WYNIK: toHexString((byte) -1) → ffffffff
        show("toHexString(-1 & 0xFF)", Integer.toHexString((byte) -1 & 0xFF));
        // WYNIK: toHexString(-1 & 0xFF) → ff
        // 2) new BigInteger(1, bytes).toString(16) gubi zera wiodące (bajty 00 0f → "f"):
        show("BigInteger(1, {0, 15}).toString(16)", new BigInteger(1, new byte[] {0, 15}).toString(16));
        // WYNIK: BigInteger(1, {0, 15}).toString(16) → f
        // Dlaczego to groźne: skrót SHA-256 zaczynający się od zera dałby 63 znaki zamiast 64
        // i porównanie z poprawnym zapisem by zawiodło — raz na ~16 skrótów.
        // PRZEPISZ: zamiast ręcznej pętli z %02x użyj HexFormat.of().formatHex(bytes).

        // CRC32 a skrót kryptograficzny:
        //  - CRC32 daje 32 bity (4 bajty), liczy się błyskawicznie i wykrywa PRZYPADKOWE zmiany
        //    (zakłócenia transmisji, uszkodzony plik) — dlatego jest w ZIP, gzip i PNG,
        //  - skrót kryptograficzny (SHA-256, 32 bajty = 256 bitów) wykrywa także CELOWE
        //    modyfikacje: nie da się sensownie dobrać innych danych o tym samym skrócie.
        CRC32 crc = new CRC32(); // CRC32 = cykliczna suma kontrolna (cyclic redundancy check)
        crc.update("Ala ma kota".getBytes(StandardCharsets.UTF_8)); // update = dodaj dane
        show("CRC32 \"Ala ma kota\" (hex)", Long.toHexString(crc.getValue()));
        // WYNIK: CRC32 "Ala ma kota" (hex) → 33b6ab71
        show("CRC32 mieści się w 32 bitach", crc.getValue() <= 0xFFFFFFFFL);
        // WYNIK: CRC32 mieści się w 32 bitach → true
        // PUŁAPKA: CRC32 nie jest zabezpieczeniem. Dlaczego: jest liniowy — dla dowolnej treści
        // da się dopisać kilka bajtów tak, aby suma wyszła taka, jaka ma być. Nie używaj go
        // do sprawdzania pobranych programów, haseł ani podpisów (do tego Toolbox02).

        // DOBRA PRAKTYKA: CRC32 — do wykrywania błędów transmisji i uszkodzeń; SHA-256 — do
        // integralności i bezpieczeństwa; hex — do wyświetlania skrótów i logów; Base64 — do
        // przesyłania bajtów tekstem. Dlaczego: każde narzędzie ma inny cel i inny koszt.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • UUID = 128 bitów; zapis 8-4-4-4-12; randomUUID() to wersja 4 (122 losowe bity).
     *   • nameUUIDFromBytes = wersja 3 (MD5), zawsze ten sam wynik dla tych samych bajtów.
     *   • UUID.fromString bywa zbyt wyrozumiały ("1-1-1-1-1") — waliduj regexem.
     *   • UUID.compareTo porównuje liczby ze znakiem — inna kolejność niż zapis tekstowy.
     *   • W bazie: typ uuid / BINARY(16); losowe v4 psują lokalność indeksu, v7 rośnie w czasie
     *     (nie ma go w JDK 17).
     *   • Base64: 3 bajty → 4 znaki; padding '='; getEncoder / getUrlEncoder / getMimeEncoder.
     *   • URL-safe + withoutPadding dla tokenów; MIME tylko do poczty.
     *   • Tekst → bajty ZAWSZE z jawnym zestawem znaków (UTF-8).
     *   • Dekodowanie błędnych danych: IllegalArgumentException — łap przy danych z zewnątrz.
     *   • Base64 to NIE szyfrowanie; Basic auth i JWT są czytelne dla każdego.
     *   • HexFormat.of() (Java 17): formatHex, parseHex, withUpperCase, ofDelimiter.
     *   • CRC32 = suma kontrolna na wypadek błędów, SHA-256 = skrót kryptograficzny.
     *
     * PYTANIA KONTROLNE:
     *   1. Ile bitów ma UUID i ile z nich jest losowych w wersji 4?
     *   2. Czym różni się UUID v4 od UUID v3 z nameUUIDFromBytes? Kiedy wybierzesz v3?
     *   3. Co wypisze:  System.out.println(UUID.fromString("1-1-1-1-1"));  ?
     *   4. Dlaczego losowy UUID jako klucz główny może spowalniać duże tabele?
     *   5. Co wypisze:  Base64.getEncoder().encodeToString("Ma".getBytes(UTF_8));  ?
     *   6. ZNAJDŹ BŁĄD:  String token = Base64.getEncoder().encodeToString(bytes);
     *                    String url = "https://x.pl/reset?token=" + token;
     *   7. Dlaczego nagłówek "Authorization: Basic ..." wysyłany zwykłym HTTP jest groźny,
     *      skoro dane są "zakodowane"?
     *   8. Co wypisze:  System.out.println(Integer.toHexString((byte) -1));  ?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: \">>>???\" → token URL", "Pj4-Pz8_", () -> exercise1(">>>???"));
        Check.equal("ćw. 1: \"M\" bez paddingu", "TQ", () -> exercise1("M"));
        Check.equal("ćw. 2: poprawny UUID", true, () -> exercise2("123e4567-e89b-12d3-a456-426614174000"));
        Check.equal("ćw. 2: za krótkie grupy", false, () -> exercise2("1-1-1-1-1"));
        Check.equal("ćw. 2: null", false, () -> exercise2(null));
        Check.equal("ćw. 3: login i hasło z dwukropkiem", List.of("ala", "ha:slo"), () -> exercise3(HEADER));
        Check.equal("ćw. 3: zły nagłówek", List.of(), () -> exercise3("Bearer abc"));
        Check.equal("ćw. 4: UUID z hex", FIXED, () -> exercise4("123e4567e89b12d3a456426614174000"));
        Check.equal("ćw. 4: okrągła droga UUID → hex → UUID", FIXED, () -> exercise4(solution4Hex(FIXED)));
        Check.throwsException("ćw. 4: za krótki hex", IllegalArgumentException.class, () -> exercise4("abcd"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1: \">>>???\" → token URL", "Pj4-Pz8_", () -> solution1(">>>???"));
        Check.equal("ćw. 1: \"M\" bez paddingu", "TQ", () -> solution1("M"));
        Check.equal("ćw. 2: poprawny UUID", true, () -> solution2("123e4567-e89b-12d3-a456-426614174000"));
        Check.equal("ćw. 2: za krótkie grupy", false, () -> solution2("1-1-1-1-1"));
        Check.equal("ćw. 2: null", false, () -> solution2(null));
        Check.equal("ćw. 3: login i hasło z dwukropkiem", List.of("ala", "ha:slo"), () -> solution3(HEADER));
        Check.equal("ćw. 3: zły nagłówek", List.of(), () -> solution3("Bearer abc"));
        Check.equal("ćw. 4: UUID z hex", FIXED, () -> solution4("123e4567e89b12d3a456426614174000"));
        Check.equal("ćw. 4: okrągła droga UUID → hex → UUID", FIXED, () -> solution4(solution4Hex(FIXED)));
        Check.throwsException("ćw. 4: za krótki hex", IllegalArgumentException.class, () -> solution4("abcd"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 10 OK, ✘ 0 BŁĄD
    }

    /** Nagłówek Basic dla ćwiczenia 3: Base64 z "ala:ha:slo". */
    private static final String HEADER = "Basic YWxhOmhhOnNsbw==";

    /**
     * ĆWICZENIE 1 (łatwe): PRZEPISZ starą wersję na nową — zamień tekst na token do adresu URL.
     * Stary sposób (ręczne podmiany znaków, łatwo o błąd):
     * <pre>{@code
     * String s = Base64.getEncoder().encodeToString(text.getBytes(UTF_8));
     * return s.replace('+', '-').replace('/', '_').replace("=", "");
     * }</pre>
     * Podpowiedź: Base64.getUrlEncoder().withoutPadding() robi to samo w jednym kroku.
     */
    static String exercise1(String text) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (łatwe): zwróć true tylko dla UUID w zapisie 8-4-4-4-12 (obie wielkości liter
     * dozwolone). null → false. Nie używaj samego UUID.fromString.
     * Podpowiedź: wzorzec STRICT_UUID z sekcji 4 albo własny regex z {@code {8}}, {@code {4}}.
     */
    static boolean exercise2(String text) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): z nagłówka {@code Authorization: Basic ...} odczytaj listę
     * {@code [login, hasło]}. Hasło może zawierać dwukropek — dziel tylko po PIERWSZYM.
     * Nagłówek nie zaczynający się od "Basic " → pusta lista.
     * Podpowiedź: {@code split(":", 2)} — drugi argument to maksymalna liczba części.
     */
    static List<String> exercise3(String header) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zamień 32 znaki hex na UUID (i rzuć IllegalArgumentException,
     * gdy bajtów nie jest dokładnie 16). To połączenie HexFormat z ByteBuffer.
     * Podpowiedź: HexFormat.of().parseHex(...), potem ByteBuffer.wrap(bajty).getLong() dwa razy.
     */
    static UUID exercise4(String hex) {
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(String text) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    static boolean solution2(String text) {
        return text != null && STRICT_UUID.matcher(text).matches();
    }

    static List<String> solution3(String header) {
        String prefix = "Basic ";
        if (header == null || !header.startsWith(prefix)) {
            return List.of();
        }
        String decoded = new String(Base64.getDecoder().decode(header.substring(prefix.length())),
                StandardCharsets.UTF_8);
        return List.of(decoded.split(":", 2));
    }

    static UUID solution4(String hex) {
        byte[] bytes = HexFormat.of().parseHex(hex);
        if (bytes.length != 16) {
            throw new IllegalArgumentException("UUID ma 16 bajtów, a jest " + bytes.length);
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong());
    }

    static String solution4Hex(UUID id) {
        return HexFormat.of().formatHex(uuidToBytes(id));
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. 128 bitów; w wersji 4 losowych jest 122 (6 bitów to wersja i wariant).
     *   2. v4 jest losowy (inny przy każdym wywołaniu), v3 jest liczony z nazwy (MD5) i zawsze
     *      taki sam dla tych samych bajtów. v3 wybierasz, gdy potrzebujesz stałego identyfikatora
     *      z klucza naturalnego (np. idempotentny import).
     *   3. 00000001-0001-0001-0001-000000000001 — parser jest wyrozumiały i dopełnia grupy zerami.
     *   4. Losowe UUID trafiają w losowe miejsca indeksu (B-drzewa): częste dzielenie stron,
     *      gorsze wykorzystanie pamięci podręcznej, a klucz ma 16 bajtów zamiast 8.
     *   5. TWE=  (2 bajty → 3 znaki + jedno '=').
     *   6. Zwykły Base64 może zawierać +, / i =, które w adresie URL mają znaczenie specjalne
     *      (+ oznacza spację, a = oddziela nazwę parametru od wartości). Użyj getUrlEncoder().withoutPadding().
     *   7. Base64 nie szyfruje: każdy, kto zobaczy nagłówek, odkoduje login i hasło bez klucza.
     *      Bez HTTPS dane są widoczne dla każdego na trasie.
     *   8. ffffffff — bajt -1 jest rozszerzany ze znakiem do int; trzeba & 0xFF (wtedy: ff).
     */
    // </editor-fold>
}
