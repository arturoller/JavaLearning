package t04_strings;

import helpers.Check;
import java.math.BigInteger;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Wyrażenia regularne dla zaawansowanych — grupy nazwane, lookaround, flagi, Unicode, pułapki
 *        (regex = wyrażenie regularne; lookahead = spojrzenie w przód; lookbehind = spojrzenie wstecz)
 *
 * W SKRÓCIE:
 *   Strings05Regex uczy podstaw: matches, find, grupy, replaceAll, split. Tu dokładamy narzędzia z "wyższej półki":
 *   grupy nazwane i odwołania wsteczne, kwantyfikatory zachłanne/leniwe/zaborcze, lookaround, flagi, klasy Unicode
 *   (polskie litery!), nowoczesne API (results, replaceAll z funkcją) oraz walidatory (kod pocztowy, PESEL, NIP, IBAN, e-mail).
 *   Uczymy się też, KIEDY regex jest złym narzędziem.
 *
 * ANALOGIA: regex to szablon z wycinanymi otworami, który przykładasz do tekstu.
 *   Grupa nazwana to otwór z etykietką ("tu jest rok"), zamiast "otwór numer 3".
 *   Lookahead to zerknięcie przez ramię: sprawdzasz, co jest dalej, ale nie robisz kroku.
 *   Kwantyfikator zachłanny to łakomczuch (bierze ile się da i oddaje, gdy trzeba), leniwy bierze po trochu,
 *   a zaborczy bierze wszystko i niczego już nie oddaje.
 *
 * JAK TO DZIAŁA:
 *   Silnik regex w Javie to automat z cofaniem (backtracking): próbuje dopasować wzorzec od lewej do prawej,
 *   a gdy utknie, cofa się i próbuje innej drogi.
 *
 *   Kwantyfikatory (ile razy powtórzyć):
 *     zachłanny   leniwy    zaborczy    znaczenie
 *     X*          X*?       X*+         zero lub więcej
 *     X+          X+?       X++         jeden lub więcej
 *     X?          X??       X?+         zero lub jeden
 *     X{n,m}      X{n,m}?   X{n,m}+     od n do m razy
 *
 *   Grupy i spojrzenia (zero-width = zerowa szerokość: nic nie zjadają, tylko sprawdzają):
 *     {@code (...)} grupa zwykła, numerowana     {@code (?<nazwa>...)} grupa nazwana
 *     {@code (?:...)} grupa niezachwytująca      {@code \1}, {@code \k<nazwa>} odwołanie wsteczne
 *     {@code (?=...)} lookahead pozytywny        {@code (?!...)} lookahead negatywny
 *     {@code (?<=...)} lookbehind pozytywny      {@code (?<!...)} lookbehind negatywny
 *
 *   Flagi: CASE_INSENSITIVE (?i), UNICODE_CASE (?u), MULTILINE (?m), DOTALL (?s), COMMENTS (?x), UNICODE_CHARACTER_CLASS (?U).
 *
 * SŁÓWKA: named group = grupa nazwana; backreference = odwołanie wsteczne; capturing = zachwytująca (zapamiętująca);
 *   greedy = zachłanny; reluctant = leniwy; possessive = zaborczy; flag = flaga; quote = ująć dosłownie (zacytować);
 *   backtracking = cofanie; catastrophic = katastrofalne; validator = walidator (sprawdzacz); boundary = granica.
 *
 * ZOBACZ TEŻ: t04_strings/Strings05Regex (podstawy regex), t04_strings/Strings06CharUnicode (znaki i Unicode),
 *   t04_strings/Strings04Formatting (formatowanie liczb zamiast regexów), t04_strings/Strings09FormatterCheatsheet
 * </pre>
 */
public class Strings08RegexAdvanced {

    // Pattern jest niezmienny i bezpieczny wątkowo, więc kompilujemy go RAZ i trzymamy w stałej (patrz sekcja 8).
    // compile = skompiluj (zamień tekst wzorca na obiekt gotowy do użycia)
    private static final Pattern STRONG_PASSWORD = Pattern.compile(
            "(?=.*\\d)(?=.*\\p{Lu})(?=.*\\p{Ll})(?=.*[!@#$%^&*]).{8,}");
    private static final Pattern POSTAL_CODE = Pattern.compile("\\d{2}-\\d{3}");
    private static final Pattern PESEL_FORMAT = Pattern.compile("\\d{11}");
    private static final Pattern NIP_FORMAT = Pattern.compile("\\d{10}|\\d{3}-\\d{3}-\\d{2}-\\d{2}");
    private static final Pattern IBAN_PL_FORMAT = Pattern.compile("PL\\d{2}(?: ?\\d{4}){6}");
    private static final Pattern EMAIL_SIMPLE = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    public static void main(String[] args) {
        title("Strings08 — wyrażenia regularne dla zaawansowanych");

        groups();         // groups = grupy
        quantifiers();    // quantifiers = kwantyfikatory
        lookarounds();    // lookarounds = spojrzenia (lookahead i lookbehind)
        flags();          // flags = flagi
        quoting();        // quoting = ujmowanie dosłowne
        modernApi();      // modern API = nowoczesne API
        unicodeClasses(); // Unicode classes = klasy Unicode
        performance();    // performance = wydajność
        validators();     // validators = walidatory
        whenNotRegex();   // when not regex = kiedy nie używać regexów
        exercises();      // exercises = ćwiczenia
    }

    // =================================================================================================
    // NARZĘDZIA POMOCNICZE LEKCJI
    // =================================================================================================

    /** Zwraca wszystkie dopasowania wzorca w tekście (findAll = znajdź wszystkie). */
    static List<String> findAll(String regex, String text) {
        List<String> found = new ArrayList<>();
        Matcher matcher = Pattern.compile(regex).matcher(text); // matcher = dopasowywacz (obiekt dopasowania)
        while (matcher.find()) {                                // find = znajdź następne dopasowanie
            found.add(matcher.group());                         // group() = cały dopasowany fragment
        }
        return found;
    }

    /** Zwraca opis błędu składni wzorca albo "OK" (description = opis). Bez wielowierszowego komunikatu. */
    static String syntaxError(String regex) {
        try {
            Pattern.compile(regex);
            return "OK";
        } catch (PatternSyntaxException e) {
            return e.getDescription();
        }
    }

    /** Zamienia znaki nowego wiersza na widoczny napis \n, żeby wynik mieścił się w jednej linii. */
    static String visible(Object value) {
        return String.valueOf(value).replace("\n", "\\n");
    }

    // =================================================================================================
    // 1. GRUPY: NAZWANE, ODWOŁANIA WSTECZNE, NIEZACHWYTUJĄCE
    // =================================================================================================

    /**
     * 1. Grupa nazwana {@code (?<nazwa>...)} zastępuje liczenie nawiasów: piszesz {@code group("rok")}, a nie
     * {@code group(3)}. Odwołanie wsteczne {@code \1} (lub {@code \k<nazwa>}) wymaga, by w tym miejscu pojawił
     * się DOKŁADNIE ten sam tekst, który złapała grupa. Grupa {@code (?:...)} służy tylko do grupowania
     * (np. pod kwantyfikator lub alternatywę) i nie zapamiętuje tekstu.
     */
    static void groups() {
        section("1. Grupy: nazwane, odwołania wsteczne, niezachwytujące");

        // --- grupy nazwane ---
        Pattern date = Pattern.compile("(?<year>\\d{4})-(?<month>\\d{2})-(?<day>\\d{2})");
        // year = rok; month = miesiąc; day = dzień
        Matcher m = date.matcher("Termin: 2026-03-15.");
        if (m.find()) {
            show("rok   group(\"year\")", m.group("year"));
            // WYNIK: rok   group("year") → 2026
            show("dzień group(\"day\")", m.group("day"));
            // WYNIK: dzień group("day") → 15
            show("miesiąc zaczyna się na indeksie", m.start("month")); // start = początek
            // WYNIK: miesiąc zaczyna się na indeksie → 13
        }

        // W zamianie odwołujemy się do grupy nazwanej przez ${nazwa}, a do numerowanej przez $1.
        show("zamiana ${day}.${month}.${year}",
                date.matcher("Termin: 2026-03-15.").replaceAll("${day}.${month}.${year}"));
        // WYNIK: zamiana ${day}.${month}.${year} → Termin: 15.03.2026.

        // PUŁAPKA: nazwa grupy może zawierać tylko litery ASCII i cyfry, i musi zaczynać się od litery.
        // Podkreślnik, myślnik lub polska litera w nazwie to błąd składni — i to dopiero w czasie działania programu
        // (wzorzec jest zwykłym napisem, kompilator Javy go nie sprawdza).
        show("nazwa my_year", syntaxError("(?<my_year>\\d{4})"));
        // WYNIK: nazwa my_year → named capturing group is missing trailing '>'
        show("nazwa 1rok", syntaxError("(?<1rok>\\d{4})"));
        // WYNIK: nazwa 1rok → capturing group name does not start with a Latin letter
        // DOBRA PRAKTYKA: nazywaj grupy camelCase-em (np. postalCode), bez podkreślników — a wzorce stałe trzymaj
        // w static final Pattern, żeby taki błąd wyszedł przy pierwszym uruchomieniu, a nie w rzadkiej ścieżce kodu.

        // --- odwołania wsteczne ---
        // \1 = "jeszcze raz to, co złapała grupa 1". W samym wzorcu piszemy \1, w tekście zamiany piszemy $1.
        // (?<![\p{L}]) i (?![\p{L}]) to "nie obok litery" — granice słowa, które znają polskie litery (sekcja 3 i 7).
        Pattern repeatedWord = Pattern.compile("(?<!\\p{L})(\\p{L}+)\\s+\\1(?!\\p{L})",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        // repeated word = powtórzone słowo
        show("bez powtórzeń", repeatedWord.matcher("To jest jest test, ale ale działa. Ten ten kod.").replaceAll("$1"));
        // WYNIK: bez powtórzeń → To jest test, ale działa. Ten kod.

        // \k<nazwa> to odwołanie wsteczne do grupy nazwanej: cudzysłów otwierający musi być taki sam jak zamykający.
        show("cytaty", findAll("(?<q>[\"']).*?\\k<q>", "Powiedział \"cześć\" i 'pa' oraz \"it's\"."));
        // WYNIK: cytaty → ["cześć", 'pa', "it's"]
        // Pierwszy cytat zaczyna się od ", więc kończy go najbliższy " — apostrof w środku nie przeszkadza.
        // Bez odwołania wstecznego wzorzec ["'].*?["'] skończyłby się na pierwszym apostrofie.

        // --- grupy niezachwytujące (?:...) ---
        // groupCount() = liczba grup zachwytujących we wzorcu (capturing = zachwytująca = zapamiętująca)
        show("(foo|bar)+(\\d)   groupCount", Pattern.compile("(foo|bar)+(\\d)").matcher("").groupCount());
        // WYNIK: (foo|bar)+(\d)   groupCount → 2
        show("(?:foo|bar)+(\\d) groupCount", Pattern.compile("(?:foo|bar)+(\\d)").matcher("").groupCount());
        // WYNIK: (?:foo|bar)+(\d) groupCount → 1

        // Prefiks kraju jest opcjonalny, ale nas interesują tylko trzy trójki cyfr: prefiks w (?:...).
        Pattern phone = Pattern.compile("(?:\\+48[ -]?)?(\\d{3})[ -]?(\\d{3})[ -]?(\\d{3})");
        for (String number : List.of("+48 600-700-800", "600700800")) {
            Matcher pm = phone.matcher(number);
            if (pm.matches()) { // matches = pasuje w CAŁOŚCI (od początku do końca tekstu)
                show("numer " + number, pm.group(1) + pm.group(2) + pm.group(3));
            }
        }
        // WYNIK: numer +48 600-700-800 → 600700800
        // WYNIK: numer 600700800 → 600700800

        // PUŁAPKA: grupa pod kwantyfikatorem pamięta tylko OSTATNIE powtórzenie.
        Matcher repeated = Pattern.compile("(\\d)+").matcher("123");
        repeated.matches();
        show("(\\d)+ na 123, group(1)", repeated.group(1));
        // WYNIK: (\d)+ na 123, group(1) → 3
        // Chcesz całość? Przenieś kwantyfikator do środka: (\d+). Jest też drugi cel: grupa niezachwytująca
        // jest odrobinę tańsza, bo silnik nie zapisuje dla niej początku i końca.
        // DOBRA PRAKTYKA: gdy grupa służy tylko do alternatywy lub kwantyfikatora, pisz (?:...) —
        // numery grup zostają stabilne, gdy ktoś później doda kolejny nawias.
    }

    // =================================================================================================
    // 2. KWANTYFIKATORY: ZACHŁANNE, LENIWE, ZABORCZE
    // =================================================================================================

    /**
     * 2. Ten sam wzorzec z trzema "charakterami" kwantyfikatora daje trzy różne wyniki. Pokazujemy to na klasycznym
     * przykładzie z tagami HTML (tylko do nauki — do prawdziwego HTML-a regex się nie nadaje, patrz sekcja 10).
     */
    static void quantifiers() {
        section("2. Kwantyfikatory: zachłanne, leniwe, zaborcze");

        String html = "<b>x</b><b>y</b>";
        // zachłanny: .* zjada CAŁY tekst do końca, potem cofa się, aż znajdzie ostatnie </b>
        show("zachłanny  <b>.*</b>", findAll("<b>.*</b>", html));
        // WYNIK: zachłanny  <b>.*</b> → [<b>x</b><b>y</b>]
        // leniwy: .*? bierze po jednym znaku i po każdym sprawdza, czy da się już zakończyć na </b>
        show("leniwy     <b>.*?</b>", findAll("<b>.*?</b>", html));
        // WYNIK: leniwy     <b>.*?</b> → [<b>x</b>, <b>y</b>]
        // zaborczy: .*+ zjada wszystko i NIE oddaje niczego, więc na końcowe </b> nic nie zostaje
        show("zaborczy   <b>.*+</b>", findAll("<b>.*+</b>", html));
        // WYNIK: zaborczy   <b>.*+</b> → []

        // Najlepsze rozwiązanie: powiedz wprost, czego w środku NIE wolno: [^<]* nie przekroczy granicy tagu.
        show("klasa negowana <b>[^<]*</b>", findAll("<b>[^<]*</b>", html));
        // WYNIK: klasa negowana <b>[^<]*</b> → [<b>x</b>, <b>y</b>]
        // DOBRA PRAKTYKA: klasa z zaprzeczeniem ([^<]*) jest szybsza i czytelniejsza niż .*? — silnik nie musi
        // za każdym znakiem sprawdzać reszty wzorca, a Ty od razu widzisz, co jest dozwolone.

        // Ten sam wybór dotyczy granic {n,m}: zachłanny bierze maksimum, leniwy minimum.
        show("a{2,3}  na aaaa", findAll("a{2,3}", "aaaa"));
        // WYNIK: a{2,3}  na aaaa → [aaa]
        show("a{2,3}? na aaaa", findAll("a{2,3}?", "aaaa"));
        // WYNIK: a{2,3}? na aaaa → [aa, aa]

        // Zaborczy w roli "bezpiecznika": a*+ zabiera wszystkie 'a' i nie odda ani jednego następnemu a.
        show("a*a  na aaa", Pattern.compile("a*a").matcher("aaa").matches());
        // WYNIK: a*a  na aaa → true
        show("a*+a na aaa", Pattern.compile("a*+a").matcher("aaa").matches());
        // WYNIK: a*+a na aaa → false
        // PUŁAPKA: zaborczy kwantyfikator potrafi odebrać dopasowanie, które zachłanny by znalazł. Używaj go
        // tam, gdzie z góry wiesz, że cofanie nie ma sensu: np. \d++ przed literą (kolejny znak i tak nie jest cyfrą).
        show("\\d++[a-z] na 123abc", findAll("\\d++[a-z]", "123abc"));
        // WYNIK: \d++[a-z] na 123abc → [123a]
        // Bliski kuzyn: grupa atomowa (?>...) — silnik po wyjściu z niej nie wraca do jej środka (jak kwantyfikator zaborczy).
    }

    // =================================================================================================
    // 3. LOOKAHEAD I LOOKBEHIND
    // =================================================================================================

    /**
     * 3. Spojrzenia (lookaround) sprawdzają, co jest przed lub za bieżącym miejscem, ale NIC nie zjadają:
     * dopasowanie ma zerową szerokość i nie trafia do wyniku. Dzięki temu wiele warunków można nałożyć na to
     * samo miejsce tekstu (hasło musi mieć cyfrę ORAZ wielką literę ORAZ znak specjalny).
     */
    static void lookarounds() {
        section("3. Lookahead i lookbehind");

        // --- lookahead pozytywny: reguły hasła ---
        // Każde (?=.*warunek) startuje od początku tekstu i sprawdza, czy gdzieś dalej jest dany znak.
        // Na końcu zwykłe .{8,} zjada cały tekst i pilnuje długości. \p{Lu} = wielka litera, \p{Ll} = mała (Unicode).
        for (String password : List.of("Haslo123!", "haslo123!", "Ha1!", "Żółw123!", "BEZCYFR!a")) {
            show("hasło " + password, STRONG_PASSWORD.matcher(password).matches());
        }
        // WYNIK: hasło Haslo123! → true
        // WYNIK: hasło haslo123! → false
        // WYNIK: hasło Ha1! → false
        // WYNIK: hasło Żółw123! → true
        // WYNIK: hasło BEZCYFR!a → false
        // DOBRA PRAKTYKA: reguły hasła w jednym wzorcu są zwięzłe, ale komunikat "hasło niepoprawne" jest
        // kiepski dla użytkownika. W prawdziwym formularzu sprawdzaj każdą regułę osobno i mów, której brakuje.

        // --- lookahead negatywny: "wszystko oprócz" ---
        Pattern login = Pattern.compile("(?!(?:admin|root)$)[a-z]{3,10}");
        for (String name : List.of("admin", "adminka", "root", "ab", "janek")) {
            show("login " + name, login.matcher(name).matches());
        }
        // WYNIK: login admin → false
        // WYNIK: login adminka → true
        // WYNIK: login root → false
        // WYNIK: login ab → false
        // WYNIK: login janek → true

        // --- lookbehind: co jest PRZED, ale nie wchodzi do wyniku ---
        String prices = "Cena: $15, zniżka: 3, koszt: $200";
        show("liczby po znaku $  (?<=\\$)\\d+", findAll("(?<=\\$)\\d+", prices));
        // WYNIK: liczby po znaku $  (?<=\$)\d+ → [15, 200]
        show("liczby BEZ znaku $ (?<!\\$)\\b\\d+", findAll("(?<!\\$)\\b\\d+", prices));
        // WYNIK: liczby BEZ znaku $ (?<!\$)\b\d+ → [3]
        // PUŁAPKA: lookbehind musi mieć ograniczoną długość (np. a{1,5} jest dozwolone). Dokumentacja Javy nie
        // gwarantuje działania z * i +, więc nie polegaj na nich — inne silniki regex bywają jeszcze surowsze.

        // --- separatory tysięcy ---
        // Wstaw przecinek w każdym miejscu, przed którym stoi cyfra, a za którym jest wielokrotność trójek cyfr do końca.
        String thousands = "(?<=\\d)(?=(?:\\d{3})+$)";
        show("1234567", "1234567".replaceAll(thousands, ","));
        // WYNIK: 1234567 → 1,234,567
        show("999", "999".replaceAll(thousands, ","));
        // WYNIK: 999 → 999
        // Zamiana na pustym dopasowaniu (zerowej szerokości) działa: wstawia tekst w danym miejscu.

        // PUŁAPKA: wzorzec działa tylko dla samych cyfr. Część ułamkowa dostaje przecinki w złych miejscach.
        show("popularna wersja \\B(?=(\\d{3})+(?!\\d)) na 1234.5678",
                "1234.5678".replaceAll("\\B(?=(\\d{3})+(?!\\d))", ","));
        // WYNIK: popularna wersja \B(?=(\d{3})+(?!\d)) na 1234.5678 → 1,234.5,678
        // DOBRA PRAKTYKA: liczby formatuj klasami do tego stworzonymi: String.format(Locale, "%,d", n) albo
        // NumberFormat (patrz t04_strings/Strings04Formatting). Sztuczki z regexem są dobre do nauki lookaround,
        // nie do kodu produkcyjnego: nie znają języka (separator tysięcy po polsku to spacja, a ułamek to przecinek).
    }

    // =================================================================================================
    // 4. FLAGI
    // =================================================================================================

    /**
     * 4. Flagi zmieniają zachowanie całego wzorca. Podajemy je jako drugi argument {@code Pattern.compile}
     * (łączone przez {@code |}) albo wewnątrz wzorca: {@code (?i)}, {@code (?im)}, a dla fragmentu {@code (?i:...)}.
     */
    static void flags() {
        section("4. Flagi: CASE_INSENSITIVE, UNICODE_CASE, MULTILINE, DOTALL, COMMENTS");

        // --- wielkość liter ---
        // case insensitive = bez względu na wielkość liter
        Pattern asciiOnly = Pattern.compile("żółw", Pattern.CASE_INSENSITIVE);
        Pattern unicodeToo = Pattern.compile("żółw", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        show("CASE_INSENSITIVE:  żółw ~ ŻÓŁW", asciiOnly.matcher("ŻÓŁW").matches());
        // WYNIK: CASE_INSENSITIVE:  żółw ~ ŻÓŁW → false
        show("+ UNICODE_CASE:    żółw ~ ŻÓŁW", unicodeToo.matcher("ŻÓŁW").matches());
        // WYNIK: + UNICODE_CASE:    żółw ~ ŻÓŁW → true
        show("(?iu) we wzorcu:   żółw ~ ŻÓŁW", "ŻÓŁW".matches("(?iu)żółw"));
        // WYNIK: (?iu) we wzorcu:   żółw ~ ŻÓŁW → true
        // PUŁAPKA: samo CASE_INSENSITIVE ignoruje wielkość tylko liter ASCII. Dla Ż/ż, Ł/ł, Ó/ó potrzebna jest
        // jeszcze UNICODE_CASE. DOBRA PRAKTYKA: dla tekstu po polsku zawsze podawaj obie flagi razem, (?iu).

        // --- MULTILINE: ^ i $ dotyczą linii, a nie całego tekstu ---
        String log = """
                INFO start
                ERROR dysk pełny
                INFO koniec
                """; // text block (Java 15+) = blok tekstowy; kończy się znakiem nowego wiersza
        show("^\\w+ bez MULTILINE", findAll("^\\w+", log));
        // WYNIK: ^\w+ bez MULTILINE → [INFO]
        show("(?m)^\\w+ z MULTILINE", findAll("(?m)^\\w+", log));
        // WYNIK: (?m)^\w+ z MULTILINE → [INFO, ERROR, INFO]
        show("(?m)^ERROR.*$", findAll("(?m)^ERROR.*$", log));
        // WYNIK: (?m)^ERROR.*$ → [ERROR dysk pełny]
        // Bez MULTILINE ^ pasuje tylko na początku całego tekstu. \A i \z oznaczają zawsze początek i koniec tekstu.

        // --- DOTALL: kropka pasuje też do końca linii ---
        String paragraph = "<p>pierwszy\nwiersz</p>";
        show("<p>.*?</p> bez DOTALL", visible(findAll("<p>.*?</p>", paragraph)));
        // WYNIK: <p>.*?</p> bez DOTALL → []
        show("(?s)<p>.*?</p> z DOTALL", visible(findAll("(?s)<p>.*?</p>", paragraph)));
        // WYNIK: (?s)<p>.*?</p> z DOTALL → [<p>pierwszy\nwiersz</p>]
        // Zapamiętaj: m dotyczy ^ i $, s dotyczy kropki. To dwie niezależne flagi, często używane razem: (?ms).

        // --- COMMENTS (?x): wzorzec z komentarzami ---
        // W tym trybie spacje i znaki nowej linii we wzorcu są ignorowane, a # zaczyna komentarz do końca linii.
        // Długi wzorzec rozbijasz na linie i opisujesz — jak zwykły kod.
        Pattern timestamp = Pattern.compile("""
                (?x)
                (?<year>\\d{4}) - (?<month>\\d{2}) - (?<day>\\d{2})   # data RRRR-MM-DD
                T                                                     # litera T jako separator
                (?<hour>\\d{2}) : (?<minute>\\d{2})                   # godzina i minuta
                """);
        Matcher tm = timestamp.matcher("2026-03-15T14:30");
        if (tm.matches()) {
            show("godzina z wzorca (?x)", tm.group("hour") + ":" + tm.group("minute"));
            // WYNIK: godzina z wzorca (?x) → 14:30
        }
        // PUŁAPKA: w trybie (?x) zwykła spacja i znak # we wzorcu NIE oznaczają siebie. Dosłowną spację zapisz
        // jako \\  (ukośnik i spacja) lub \s, a dosłowny # jako \\#.
        show("(?x)a b  na tekście ab ", "ab".matches("(?x)a b"));
        // WYNIK: (?x)a b  na tekście ab  → true
        show("(?x)a b  na tekście a b", "a b".matches("(?x)a b"));
        // WYNIK: (?x)a b  na tekście a b → false
        // DOBRA PRAKTYKA: wzorce dłuższe niż jedna linia zapisuj w trybie (?x) w bloku tekstowym — za pół roku
        // sam będziesz wdzięczny, że każda część ma komentarz.
    }

    // =================================================================================================
    // 5. DOSŁOWNE UJMOWANIE: Pattern.quote I Matcher.quoteReplacement
    // =================================================================================================

    /**
     * 5. Dane od użytkownika wstawiane do wzorca lub do tekstu zamiany trzeba "znieczulić": znaki specjalne
     * ({@code . + * ? ( [ $ \}) mają w regexie znaczenie. Robią to {@code Pattern.quote} (dla wzorca)
     * i {@code Matcher.quoteReplacement} (dla zamiennika).
     */
    static void quoting() {
        section("5. Pattern.quote i Matcher.quoteReplacement");

        // PUŁAPKA: split i replaceAll przyjmują WZORZEC, nie zwykły tekst.
        show("split(\"+\")", syntaxError("+"));
        // WYNIK: split("+") → Dangling meta character '+'
        show("\"a.b.c\".split(\".\").length", "a.b.c".split(".").length);
        // WYNIK: "a.b.c".split(".").length → 0
        // Kropka znaczy "dowolny znak", więc każdy znak jest separatorem, a puste elementy na końcu są usuwane — zostaje 0.

        show("Pattern.quote(\".\")", Pattern.quote("."));
        // WYNIK: Pattern.quote(".") → \Q.\E
        // quote = ująć w \Q...\E: wszystko pomiędzy jest traktowane dosłownie
        show("split(Pattern.quote(\".\"))", Arrays.toString("a.b.c".split(Pattern.quote("."))));
        // WYNIK: split(Pattern.quote(".")) → [a, b, c]
        // Dla pojedynczego znaku wystarczy też klasa: [.] albo ukośnik: \\. — ale quote jest odporny na dowolny napis.

        String userInput = "1+1=2";
        Pattern literal = Pattern.compile(Pattern.quote(userInput)); // literal = dosłowny
        show("szukam dosłownie \"1+1=2\"", literal.matcher("Wiadomo, że 1+1=2, prawda?").find());
        // WYNIK: szukam dosłownie "1+1=2" → true
        show("bez quote ten sam napis jako wzorzec", Pattern.compile(userInput).matcher("Wiadomo, że 1+1=2, prawda?").find());
        // WYNIK: bez quote ten sam napis jako wzorzec → false
        // Bez quote "1+1=2" znaczy: jedna lub więcej jedynek, potem 1, potem =2 — i nie pasuje do tekstu "1+1=2".

        // --- tekst zamiany: $ i \ są specjalne ---
        expectThrows("replaceAll(\"cena\", \"$5\")", () -> "cena".replaceAll("cena", "$5"));
        // WYNIK: ✔ replaceAll("cena", "$5") → rzucono IndexOutOfBoundsException: No group 5
        expectThrows("replaceAll(\"cena\", \"a\\\")", () -> "cena".replaceAll("cena", "a\\"));
        // WYNIK: ✔ replaceAll("cena", "a\") → rzucono IllegalArgumentException: character to be escaped is missing
        // W zamienniku $5 oznacza "grupa numer 5", a \ ucieka następny znak. Cena w dolarach psuje program!
        String safe = Matcher.quoteReplacement("$5");
        show("Matcher.quoteReplacement(\"$5\")", safe);
        // WYNIK: Matcher.quoteReplacement("$5") → \$5
        show("replaceAll z quoteReplacement", "cena".replaceAll("cena", safe));
        // WYNIK: replaceAll z quoteReplacement → $5
        // DOBRA PRAKTYKA: gdy zamiennik pochodzi z zewnątrz (dane, plik, formularz), użyj quoteReplacement.
        // Gdy zamieniasz zwykły tekst na zwykły tekst — w ogóle nie potrzebujesz regexu: String.replace(...)
        // traktuje oba argumenty dosłownie.
        show("\"cena\".replace(\"cena\", \"$5\")", "cena".replace("cena", "$5"));
        // WYNIK: "cena".replace("cena", "$5") → $5
    }

    // =================================================================================================
    // 6. NOWOCZESNE API: results, replaceAll z funkcją, splitAsStream, predykaty
    // =================================================================================================

    /**
     * 6. Od Javy 8 i 9 regex współpracuje ze strumieniami i lambdami: zamiast pętli {@code while (m.find())}
     * piszesz {@code m.results()}, a zamiast sklejania tekstu ręcznie podajesz funkcję do {@code replaceAll}.
     */
    static void modernApi() {
        section("6. results, replaceAll z funkcją, splitAsStream, predykaty");

        // --- results() (Java 9+): strumień wszystkich dopasowań ---
        // results = wyniki; MatchResult = niezmienna migawka jednego dopasowania (group, start, end)
        Pattern capitalized = Pattern.compile("\\p{Lu}\\p{Ll}+"); // capitalized = zaczyna się wielką literą
        String sentence = "Ala ma Kota i Psa";
        List<String> words = capitalized.matcher(sentence).results().map(MatchResult::group).toList(); // toList (Java 16+)
        show("słowa od wielkiej litery", words);
        // WYNIK: słowa od wielkiej litery → [Ala, Kota, Psa]
        show("z pozycjami", capitalized.matcher(sentence).results().map(r -> r.group() + "@" + r.start()).toList());
        // WYNIK: z pozycjami → [Ala@0, Kota@7, Psa@14]
        show("liczba dopasowań (count = policz)", capitalized.matcher(sentence).results().count());
        // WYNIK: liczba dopasowań (count = policz) → 3

        // PRZED (pętla) → PO (strumień):
        //   List<String> out = new ArrayList<>();
        //   while (m.find()) { out.add(m.group()); }
        //   List<String> out = m.results().map(MatchResult::group).toList();

        // --- replaceAll(Function<MatchResult,String>) (Java 9+): zamiennik liczony z dopasowania ---
        Pattern number = Pattern.compile("\\d+");
        show("podwojone liczby", number.matcher("a1 b22 c333").replaceAll(r -> String.valueOf(Integer.parseInt(r.group()) * 2)));
        // WYNIK: podwojone liczby → a2 b44 c666
        Locale polish = Locale.forLanguageTag("pl-PL");
        show("wielka litera na początku słowa",
                Pattern.compile("\\p{L}+").matcher("zażółć gęślą jaźń").replaceAll(
                        r -> r.group().substring(0, 1).toUpperCase(polish) + r.group().substring(1)));
        // WYNIK: wielka litera na początku słowa → Zażółć Gęślą Jaźń

        // PUŁAPKA: tekst zwrócony przez funkcję jest TRAKTOWANY JAK ZAMIENNIK, więc $ i \ znowu są specjalne.
        expectThrows("funkcja zwraca \"$\" + liczba", () -> number.matcher("cena 5").replaceAll(r -> "$" + r.group()));
        // WYNIK: ✔ funkcja zwraca "$" + liczba → rzucono IndexOutOfBoundsException: No group 5
        show("z quoteReplacement", number.matcher("cena 5").replaceAll(r -> Matcher.quoteReplacement("$" + r.group())));
        // WYNIK: z quoteReplacement → cena $5

        // --- splitAsStream: dzielenie leniwe, bez tworzenia całej tablicy ---
        Pattern separator = Pattern.compile("\\s*[,;]\\s*"); // separator = rozdzielacz
        show("splitAsStream", separator.splitAsStream("a , b;c,d").toList());
        // WYNIK: splitAsStream → [a, b, c, d]
        // Jak w split: puste elementy NA KOŃCU są pomijane.
        show("liczba elementów a,b,, (count)", Pattern.compile(",").splitAsStream("a,b,,").count());
        // WYNIK: liczba elementów a,b,, (count) → 2

        // --- asPredicate (Java 8, używa find) i asMatchPredicate (Java 11, używa matches) ---
        Pattern digits = Pattern.compile("\\d+");
        show("asPredicate (zawiera cyfry)", Stream.of("12", "a1", "1a", "").filter(digits.asPredicate()).toList());
        // WYNIK: asPredicate (zawiera cyfry) → [12, a1, 1a]
        show("asMatchPredicate (same cyfry)", Stream.of("12", "a1", "1a", "").filter(digits.asMatchPredicate()).toList());
        // WYNIK: asMatchPredicate (same cyfry) → [12]
        // DOBRA PRAKTYKA: filter(wzorzec.asPredicate()) zamiast lambdy x -> wzorzec.matcher(x).find() —
        // krócej i od razu widać intencję. Pamiętaj o różnicy: find (zawiera) kontra matches (cały tekst).
    }

    // =================================================================================================
    // 7. KLASY UNICODE: POLSKIE LITERY
    // =================================================================================================

    /**
     * 7. Skróty {@code \w}, {@code \d}, {@code \b} i zakres {@code [a-zA-Z]} znają domyślnie TYLKO ASCII.
     * Polskie litery wymagają klas Unicode: {@code \p{L}} (dowolna litera), {@code \p{Lu}} (wielka),
     * {@code \p{Ll}} (mała), {@code \p{M}} (znak łączony, np. kreska nad literą) albo flagi UNICODE_CHARACTER_CLASS.
     */
    static void unicodeClasses() {
        section("7. Klasy Unicode: \\p{L}, \\p{Lu}, \\p{IsAlphabetic}");

        String text = "Zażółć gęślą jaźń";
        show("[a-zA-Z]+", findAll("[a-zA-Z]+", text));
        // WYNIK: [a-zA-Z]+ → [Za, g, l, ja]
        show("\\w+", findAll("\\w+", text));
        // WYNIK: \w+ → [Za, g, l, ja]
        show("\\p{Alpha}+ (POSIX = ASCII!)", findAll("\\p{Alpha}+", text));
        // WYNIK: \p{Alpha}+ (POSIX = ASCII!) → [Za, g, l, ja]
        show("\\p{L}+", findAll("\\p{L}+", text));
        // WYNIK: \p{L}+ → [Zażółć, gęślą, jaźń]
        show("\\p{IsAlphabetic}+", findAll("\\p{IsAlphabetic}+", text));
        // WYNIK: \p{IsAlphabetic}+ → [Zażółć, gęślą, jaźń]
        show("(?U)\\w+ (UNICODE_CHARACTER_CLASS)", findAll("(?U)\\w+", text));
        // WYNIK: (?U)\w+ (UNICODE_CHARACTER_CLASS) → [Zażółć, gęślą, jaźń]
        // PUŁAPKA: [a-zA-Z] i \w rozcinają polskie słowa na kawałki, a program "działa" na testowych danych bez
        // polskich liter. Błąd wychodzi dopiero u klienta o nazwisku Żółtowski.
        // DOBRA PRAKTYKA: litery zapisuj jako \p{L} (lub \p{IsAlphabetic}), wielkie jako \p{Lu}, cyfry jako \p{Nd}.

        show("wielkie litery [A-Z]", findAll("[A-Z]", "Ala Żaneta Łukasz ósmy"));
        // WYNIK: wielkie litery [A-Z] → [A]
        show("wielkie litery \\p{Lu}", findAll("\\p{Lu}", "Ala Żaneta Łukasz ósmy"));
        // WYNIK: wielkie litery \p{Lu} → [A, Ż, Ł]
        show("nie-litery \\P{L}+ (duże P = zaprzeczenie)", findAll("\\P{L}+", "ala,ma;kota"));
        // WYNIK: nie-litery \P{L}+ (duże P = zaprzeczenie) → [,, ;]

        // Różnica między \p{L} a \p{IsAlphabetic}: druga zawiera też np. cyfry rzymskie zapisane jednym znakiem
        // (kategoria Nl). Dla polskiego tekstu oba wzorce dają to samo; \p{L} jest krótsze i powszechniej znane.

        // --- znaki złożone: litera + osobny znak diakrytyczny ---
        // Normalizer.Form.NFD rozkłada "ó" na "o" + kreska (znak z kategorii M). Zobacz t04_strings/Strings06CharUnicode.
        String decomposed = Normalizer.normalize("zażółć", Normalizer.Form.NFD);
        // decomposed = rozłożony; normalize = ujednolić zapis Unicode
        show("długość zażółć (NFC)", "zażółć".length());
        // WYNIK: długość zażółć (NFC) → 6
        show("długość po rozłożeniu (NFD)", decomposed.length());
        // WYNIK: długość po rozłożeniu (NFD) → 9
        show("\\p{L}+ na rozłożonym", findAll("\\p{L}+", decomposed));
        // WYNIK: \p{L}+ na rozłożonym → [zaz, o, łc]
        show("[\\p{L}\\p{M}]+ na rozłożonym", findAll("[\\p{L}\\p{M}]+", decomposed).size() + " słowo");
        // WYNIK: [\p{L}\p{M}]+ na rozłożonym → 1 słowo
        // Popularna sztuczka "usuń ogonki": rozłóż i wytnij znaki \p{M}.
        show("bez ogonków", decomposed.replaceAll("\\p{M}", ""));
        // WYNIK: bez ogonków → zazołc
        // PUŁAPKA: litera "ł" NIE rozkłada się na "l" + znak, więc zostaje. Takie "usuwanie ogonków" nie
        // jest kompletne (wynik: zazołc) — dla ł potrzebna jest osobna zamiana.
    }

    // =================================================================================================
    // 8. WYDAJNOŚĆ: KOMPILACJA RAZ I KATASTROFALNE COFANIE
    // =================================================================================================

    /** Fałszywy tekst zliczający odczyty znaków — miara pracy silnika bez mierzenia czasu (deterministyczna). */
    static final class CountingText implements CharSequence { // counting = liczący
        private final String text;
        int reads; // reads = odczyty

        CountingText(String text) {
            this.text = text;
        }

        @Override
        public int length() {
            return text.length();
        }

        @Override
        public char charAt(int index) {
            reads++;
            return text.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return text.subSequence(start, end);
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /** Ile odczytów znaków potrzebuje silnik, by stwierdzić, że napis "aaa...ac" NIE pasuje do wzorca. */
    static int work(String regex, int n) {
        CountingText input = new CountingText("a".repeat(n) + "c"); // repeat = powtórz (Java 11+)
        Pattern.compile(regex).matcher(input).matches();
        return input.reads;
    }

    /**
     * 8. Kompilacja wzorca kosztuje. {@code String.matches}, {@code replaceAll} i {@code split} kompilują wzorzec
     * przy KAŻDYM wywołaniu. Drugi temat: cofanie. Zły wzorzec na złośliwym wejściu potrafi pracować wieki.
     */
    static void performance() {
        section("8. Kompilacja raz i katastrofalne cofanie");

        // PRZED: w pętli, z kompilacją za każdym razem
        //   for (String code : codes) { if (code.matches("\\d{2}-\\d{3}")) ... }
        // PO: wzorzec skompilowany raz (stała static final POSTAL_CODE na górze klasy)
        //   for (String code : codes) { if (POSTAL_CODE.matcher(code).matches()) ... }
        List<String> codes = List.of("00-950", "30-001", "3-001");
        long good = codes.stream().filter(POSTAL_CODE.asMatchPredicate()).count();
        show("poprawne kody", good);
        // WYNIK: poprawne kody → 2
        // Pattern jest niezmienny — bezpieczny do współdzielenia między wątkami. Matcher ma stan (pozycję,
        // grupy) i NIE jest bezpieczny wątkowo: każdy wątek (a często każde użycie) tworzy własny przez pattern.matcher(...).
        // DOBRA PRAKTYKA: stałe wzorce trzymaj w static final Pattern, a Matchera twórz lokalnie w metodzie.

        // --- katastrofalne cofanie (catastrophic backtracking) ---
        // Wzorzec z zagnieżdżonym kwantyfikatorem, np. (a+)+b, można rozbić ciąg "aaaa" na grupy na wiele sposobów:
        // aaaa, a+aaa, aa+aa, a+a+aa ... Gdy na końcu brakuje "b", silnik próbuje KAŻDEGO rozbicia, zanim się podda.
        // Liczba rozbić ciągu n liter 'a' to 2^(n-1): dodanie JEDNEJ litery podwaja pracę.
        // Nie uruchamiamy tego na prawdziwych danych — liczymy odczyty znaków na maleńkich wejściach (n do 12).
        // Nie zwiększaj n: dla leniwego wariantu każde +2 mnoży pracę razy 4, a dla n = 40 program nie skończyłby się za Twojego życia.
        for (int n : new int[] {4, 6, 8, 10, 12}) {
            show("n = " + n + " (odczyty znaków)", "a+b: " + work("a+b", n)
                    + ", (a+)+b: " + work("(a+)+b", n)
                    + ", (?:a+)+?b: " + work("(?:a+)+?b", n));
        }
        // WYNIK: n = 4 (odczyty znaków) → a+b: 9, (a+)+b: 25, (?:a+)+?b: 46
        // WYNIK: n = 6 (odczyty znaków) → a+b: 13, (a+)+b: 49, (?:a+)+?b: 190
        // WYNIK: n = 8 (odczyty znaków) → a+b: 17, (a+)+b: 81, (?:a+)+?b: 766
        // WYNIK: n = 10 (odczyty znaków) → a+b: 21, (a+)+b: 121, (?:a+)+?b: 3070
        // WYNIK: n = 12 (odczyty znaków) → a+b: 25, (a+)+b: 169, (?:a+)+?b: 12286
        // Wzorzec a+b rośnie liniowo. (a+)+b rośnie wolniej, niż uczą stare podręczniki: nowsze wersje Javy potrafią
        // uciąć najprostsze przypadki — ale i tak rośnie coraz szybciej. Wariant z leniwym
        // kwantyfikatorem (?:a+)+?b (a także z odwołaniem wstecznym) nadal jest wykładniczy: kolumna rośnie ~4 razy co 2 kroki.
        // PUŁAPKA: ReDoS (Regular expression Denial of Service = odmowa usługi przez regex): atakujący wysyła
        // krótki tekst pasujący "prawie" i blokuje wątek serwera na minuty. Java nie ma limitu czasu dla regexu.
        // DOBRA PRAKTYKA: (1) unikaj zagnieżdżonych kwantyfikatorów typu (x+)+, (x*)*, (x|x)+; (2) używaj zaborczych
        // kwantyfikatorów lub grup atomowych, gdy cofanie jest zbędne; (3) ograniczaj długość wejścia PRZED
        // dopasowaniem; (4) przy niezaufanych danych owiń CharSequence obiektem, który rzuca wyjątek po przekroczeniu
        // limitu odczytów (podobnie jak nasz CountingText, tyle że rzucający wyjątek).
    }

    // =================================================================================================
    // 9. WALIDATORY
    // =================================================================================================

    /** PESEL: 11 cyfr, wagi 1-3-7-9-1-3-7-9-1-3, ostatnia cyfra to cyfra kontrolna. */
    static boolean isPesel(String pesel) {
        if (!PESEL_FORMAT.matcher(pesel).matches()) {
            return false; // zły format: nie liczymy sumy kontrolnej
        }
        int[] weights = {1, 3, 7, 9, 1, 3, 7, 9, 1, 3}; // weights = wagi
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += (pesel.charAt(i) - '0') * weights[i];
        }
        int control = (10 - sum % 10) % 10; // control = cyfra kontrolna
        return control == pesel.charAt(10) - '0';
    }

    /** NIP: 10 cyfr (z myślnikami lub bez), wagi 6-5-7-2-3-4-5-6-7, suma modulo 11 równa cyfrze kontrolnej. */
    static boolean isNip(String nip) {
        if (!NIP_FORMAT.matcher(nip).matches()) {
            return false;
        }
        String digits = nip.replace("-", "");
        int[] weights = {6, 5, 7, 2, 3, 4, 5, 6, 7};
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (digits.charAt(i) - '0') * weights[i];
        }
        int control = sum % 11;
        return control != 10 && control == digits.charAt(9) - '0'; // reszta 10 oznacza NIP, który nie istnieje
    }

    /** IBAN polski: format regexem, potem suma kontrolna mod 97 (BigInteger, bo liczba ma ~28 cyfr). */
    static boolean isIbanPl(String iban) {
        if (!IBAN_PL_FORMAT.matcher(iban).matches()) {
            return false;
        }
        String compact = iban.replace(" ", "");                      // compact = zwarty (bez spacji)
        String rearranged = compact.substring(4) + compact.substring(0, 4); // cztery pierwsze znaki na koniec
        StringBuilder numeric = new StringBuilder();                 // numeric = liczbowy
        for (char c : rearranged.toCharArray()) {
            numeric.append(Character.isLetter(c) ? Character.getNumericValue(c) : c - '0'); // A=10, B=11, ..., P=25, L=21
        }
        return new BigInteger(numeric.toString()).mod(BigInteger.valueOf(97)).intValue() == 1;
    }

    /**
     * 9. Wzorzec sprawdza FORMAT (jak to wygląda), a znaczenie (czy numer istnieje) sprawdza zwykły kod Javy.
     * Suma kontrolna to arytmetyka, a nie dopasowywanie tekstu — regex się do niej nie nadaje.
     */
    static void validators() {
        section("9. Walidatory: kod pocztowy, PESEL, NIP, IBAN, e-mail");

        for (String code : List.of("00-950", "00950", "0-0950", "00-950 ")) {
            show("kod pocztowy \"" + code + "\"", POSTAL_CODE.matcher(code).matches());
        }
        // WYNIK: kod pocztowy "00-950" → true
        // WYNIK: kod pocztowy "00950" → false
        // WYNIK: kod pocztowy "0-0950" → false
        // WYNIK: kod pocztowy "00-950 " → false
        // matches() wymaga dopasowania całego tekstu, więc ^ i $ nie są potrzebne (zbędna spacja na końcu już psuje).

        // PESEL (przykładowy numer o poprawnej sumie kontrolnej): format + suma kontrolna
        for (String pesel : List.of("44051401359", "44051401358", "4405140135", "44O51401359")) {
            show("PESEL " + pesel, isPesel(pesel));
        }
        // WYNIK: PESEL 44051401359 → true
        // WYNIK: PESEL 44051401358 → false
        // WYNIK: PESEL 4405140135 → false
        // WYNIK: PESEL 44O51401359 → false
        // Ostatni numer ma literę O zamiast zera — zły format. Poprzedni: dobry format, zła cyfra kontrolna.

        for (String nip : List.of("123-456-32-18", "1234563218", "1234563219", "12-3456-3218")) {
            show("NIP " + nip, isNip(nip));
        }
        // WYNIK: NIP 123-456-32-18 → true
        // WYNIK: NIP 1234563218 → true
        // WYNIK: NIP 1234563219 → false
        // WYNIK: NIP 12-3456-3218 → false

        for (String iban : List.of("PL61109010140000071219812874", "PL61 1090 1014 0000 0712 1981 2874",
                "PL61109010140000071219812875", "DE61109010140000071219812874")) {
            show("IBAN " + iban, isIbanPl(iban));
        }
        // WYNIK: IBAN PL61109010140000071219812874 → true
        // WYNIK: IBAN PL61 1090 1014 0000 0712 1981 2874 → true
        // WYNIK: IBAN PL61109010140000071219812875 → false
        // WYNIK: IBAN DE61109010140000071219812874 → false
        // Trzeci: dobry format, zła suma kontrolna. Czwarty: nie polski (inny kod kraju).

        // --- e-mail: dlaczego "idealny regex" to pułapka ---
        // Prosty, praktyczny wzorzec: coś@coś.coś, bez spacji i bez drugiego @.
        List<String> emails = List.of("jan.kowalski+sklep@example.com", "a@b.pl", "a@@b.pl", "jan kowalski@example.com",
                "a@b", "zażółć@gęślą.pl", "\"jan kowalski\"@example.com");
        for (String email : emails) {
            show("e-mail " + email, EMAIL_SIMPLE.matcher(email).matches());
        }
        // WYNIK: e-mail jan.kowalski+sklep@example.com → true
        // WYNIK: e-mail a@b.pl → true
        // WYNIK: e-mail a@@b.pl → false
        // WYNIK: e-mail jan kowalski@example.com → false
        // WYNIK: e-mail a@b → false
        // WYNIK: e-mail zażółć@gęślą.pl → true
        // WYNIK: e-mail "jan kowalski"@example.com → false
        // Wnioski: "a@b" jest poprawnym adresem w sieci lokalnej, a odrzucamy go. Adres w cudzysłowie ze spacją
        // jest zgodny ze standardem (RFC 5322), a też go odrzucamy. Adresy z polskimi znakami (RFC 6531) przepuszczamy.
        // Pełna gramatyka adresu to kilka stron wzorca, którego nikt nie jest w stanie przeczytać, a i tak nie powie, czy
        // skrzynka istnieje.
        // DOBRA PRAKTYKA: regex tylko odsiewa oczywiste literówki (jest @ i kropka w domenie, brak spacji).
        // Jedyna prawdziwa walidacja to wysłanie wiadomości z linkiem potwierdzającym.
        // PUŁAPKA: nie kopiuj z internetu "doskonałego" wzorca na 300 znaków — nie rozumiesz go, nie przetestujesz go
        // i może zawierać katastrofalne cofanie (sekcja 8).
    }

    // =================================================================================================
    // 10. KIEDY NIE UŻYWAĆ REGEXÓW
    // =================================================================================================

    /**
     * 10. Regex rozpoznaje wzorce płaskie. Struktury zagnieżdżone (HTML, JSON, wyrażenia w nawiasach) i formaty
     * z cudzysłowami i znakami ucieczki (CSV) wymagają PARSERA: programu, który czyta tekst znak po znaku,
     * pamiętając, w jakim miejscu struktury jest.
     */
    static void whenNotRegex() {
        section("10. Kiedy NIE używać regexów");

        // HTML: zagnieżdżone <div> — leniwy wzorzec kończy na pierwszym </div>, więc łapie śmieci
        Matcher div = Pattern.compile("<div>(.*?)</div>").matcher("<div>a<div>b</div></div>");
        if (div.find()) {
            show("HTML <div>(.*?)</div>", div.group(1));
            // WYNIK: HTML <div>(.*?)</div> → a<div>b
        }
        // Oczekiwaliśmy całości "a<div>b</div>". Regex nie umie liczyć nawiasów/tagów do pary.

        // CSV: przecinek w cudzysłowie to NIE separator
        String csv = "\"Kowalski, Jan\",30";
        show("CSV split(\",\") — liczba kawałków", csv.split(",").length);
        // WYNIK: CSV split(",") — liczba kawałków → 3
        show("CSV kawałki (rozdzielone znakiem |)", String.join("|", csv.split(",")));
        // WYNIK: CSV kawałki (rozdzielone znakiem |) → "Kowalski| Jan"|30
        // Dostaliśmy trzy kawałki zamiast dwóch pól: nazwisko zostało rozcięte w środku, a cudzysłowy zostały po obu stronach.

        // JSON: znak ucieczki \" w środku wartości
        String json = """
                {"name":"Ala \\"Ola\\""}""";
        Matcher name = Pattern.compile("\"name\":\"(.*?)\"").matcher(json);
        if (name.find()) {
            show("JSON \"name\":\"(.*?)\"", name.group(1));
            // WYNIK: JSON "name":"(.*?)" → Ala \
        }
        // Wartość to Ala "Ola", a my wyciągnęliśmy samo Ala \ — wzorzec nie zna zasad ucieczki.

        // DOBRA PRAKTYKA: HTML → biblioteka jsoup, JSON → Jackson lub Gson, CSV → OpenCSV lub Commons CSV.
        // Regex zostaw do tego, w czym jest dobry: krótkie, płaskie formaty (kody, identyfikatory, wyszukiwanie fragmentów).
        // Proste przypadki często nie potrzebują nawet regexu: startsWith, contains, split(",") na czystych danych,
        // indexOf + substring — są szybsze, czytelniejsze i bez ryzyka cofania.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • (?<nazwa>...) = grupa nazwana, group("nazwa"), w zamianie ${nazwa}; nazwa: litery ASCII i cyfry, bez _
     *   • \1 i \k<nazwa> = odwołanie wsteczne (ten sam tekst jeszcze raz); w zamienniku $1
     *   • (?:...) = grupa bez zapamiętywania; grupa pod kwantyfikatorem pamięta tylko ostatni obrót
     *   • zachłanny X* (bierze ile się da, oddaje), leniwy X*? (bierze po trochu), zaborczy X*+ (nie oddaje)
     *   • klasa negowana [^<]* zwykle lepsza niż .*? (szybsza i wprost mówi, czego nie wolno)
     *   • (?=..) (?!..) patrzą w przód, (?<=..) (?<!..) wstecz; nic nie zjadają; lookbehind ma ograniczoną długość
     *   • flagi: (?iu) wielkość liter po polsku, (?m) ^ i $ per linia, (?s) kropka łapie \n, (?x) komentarze
     *   • Pattern.quote(tekst) dla wzorca, Matcher.quoteReplacement(tekst) dla zamiennika; $ w zamienniku to grupa!
     *   • results() (Java 9+), replaceAll(Function) (Java 9+), splitAsStream, asPredicate, asMatchPredicate (Java 11+)
     *   • [a-zA-Z], \w, \b = ASCII; polskie litery: \p{L}, \p{Lu}, \p{Ll}, \p{IsAlphabetic} lub flaga (?U)
     *   • static final Pattern + Matcher lokalnie; zagnieżdżone kwantyfikatory (x+)+ = ryzyko ReDoS
     *   • walidacja = format (regex) + znaczenie (kod: suma kontrolna); e-mail: prosty wzorzec + wiadomość potwierdzająca
     *   • HTML, JSON, CSV i wszystko zagnieżdżone → parser, nie regex
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się (?<rok>\d{4}) od (?:\d{4}) i kiedy wybierzesz które?
     *   2. Co wypisze:  System.out.println(Pattern.compile("<b>.*+</b>").matcher("<b>x</b>").find());  ?
     *   3. ZNAJDŹ BŁĄD:  String fixed = text.replaceAll("cena", "$" + price);   (price = "5")
     *   4. Co wypisze:  System.out.println("ŻÓŁW".matches("(?i)żółw"));  ?  A po zmianie na (?iu) ?
     *   5. ZNAJDŹ BŁĄD:  Pattern name = Pattern.compile("[A-Z][a-z]+");  służy do wyszukiwania imion w polskim tekście.
     *   6. Co wypisze:  System.out.println("aaa".replaceAll("a*", "X"));  ?
     *   7. Dlaczego (?<=\$)\d+ zwraca "15", a nie "$15"?
     *   8. Dlaczego dobry regex na e-mail nie zastąpi wiadomości potwierdzającej? A dlaczego nie parsujemy JSON-a regexem?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: daty RRRR-MM-DD na DD.MM.RRRR", "Od 05.01.2026 do 15.03.2026.",
                () -> exercise1("Od 2026-01-05 do 2026-03-15."));
        Check.equal("ćw. 2: hasło", List.of(true, false, false, true),
                () -> List.of(exercise2("Zebra#2026"), exercise2("zebra#2026"), exercise2("Ze#1"), exercise2("Łódź_2026!")));
        Check.equal("ćw. 3: liczby z tekstu", List.of(12, -7, 300),
                () -> exercise3("a 12, b -7, c 300"));
        Check.equal("ćw. 4: NIP", List.of(true, true, false, false),
                () -> List.of(exercise4("1234563218"), exercise4("123-456-32-18"), exercise4("123-4563218"), exercise4("1234563219")));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "Od 05.01.2026 do 15.03.2026.",
                () -> solution1("Od 2026-01-05 do 2026-03-15."));
        Check.equal("ćw. 2 (wzorzec)", List.of(true, false, false, true),
                () -> List.of(solution2("Zebra#2026"), solution2("zebra#2026"), solution2("Ze#1"), solution2("Łódź_2026!")));
        Check.equal("ćw. 3 (wzorzec)", List.of(12, -7, 300),
                () -> solution3("a 12, b -7, c 300"));
        Check.equal("ćw. 4 (wzorzec)", List.of(true, true, false, false),
                () -> List.of(solution4("1234563218"), solution4("123-456-32-18"), solution4("123-4563218"), solution4("1234563219")));
        Check.summary();
        // WYNIK: ✔ OK    ćw. 1 (wzorzec)
        // WYNIK: ✔ OK    ćw. 2 (wzorzec)
        // WYNIK: ✔ OK    ćw. 3 (wzorzec)
        // WYNIK: ✔ OK    ćw. 4 (wzorzec)
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zamień WSZYSTKIE daty w formacie RRRR-MM-DD na DD.MM.RRRR, używając grup nazwanych
     * (year, month, day) i zamiennika z ${...}. Reszta tekstu zostaje bez zmian.
     * Podpowiedź: Pattern.compile(...).matcher(text).replaceAll("${day}.${month}.${year}").
     */
    static String exercise1(String text) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): czy hasło jest silne? Co najmniej 8 znaków, mała litera, wielka litera, cyfra i
     * znak specjalny spośród # _ ! (litery mogą być polskie!).
     * Podpowiedź: cztery lookahead (?=.*...) i .{8,}; polskie litery to \p{Ll} i \p{Lu}.
     */
    static boolean exercise2(String password) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ pętlę na strumień. Zwróć wszystkie liczby całkowite z tekstu (także ujemne).
     * <pre>{@code
     * // PRZED:
     * List<Integer> out = new ArrayList<>();
     * Matcher m = Pattern.compile("-?\\d+").matcher(text);
     * while (m.find()) { out.add(Integer.parseInt(m.group())); }
     * return out;
     * }</pre>
     * Podpowiedź: m.results().map(MatchResult::group).map(Integer::valueOf).toList().
     */
    static List<Integer> exercise3(String text) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): sprawdź NIP bez używania gotowej metody isNip z lekcji: format
     * (10 cyfr albo 3-3-2-2 cyfry z myślnikami; mieszanie jest błędem) oraz suma kontrolna: cyfry 1-9 razy wagi
     * 6,5,7,2,3,4,5,6,7, suma modulo 11 musi równać się dziesiątej cyfrze (reszta 10 = błędny NIP).
     * Podpowiedź: format regexem, myślniki usuń przez replace("-", ""), resztę policz pętlą.
     */
    static boolean exercise4(String nip) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(String text) {
        Pattern date = Pattern.compile("(?<year>\\d{4})-(?<month>\\d{2})-(?<day>\\d{2})");
        return date.matcher(text).replaceAll("${day}.${month}.${year}");
    }

    static boolean solution2(String password) {
        return password.matches("(?=.*\\p{Ll})(?=.*\\p{Lu})(?=.*\\d)(?=.*[#_!]).{8,}");
    }

    static List<Integer> solution3(String text) {
        return Pattern.compile("-?\\d+").matcher(text).results().map(MatchResult::group).map(Integer::valueOf).toList();
    }

    static boolean solution4(String nip) {
        if (!nip.matches("\\d{10}|\\d{3}-\\d{3}-\\d{2}-\\d{2}")) {
            return false;
        }
        String digits = nip.replace("-", "");
        int[] weights = {6, 5, 7, 2, 3, 4, 5, 6, 7};
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += (digits.charAt(i) - '0') * weights[i];
        }
        int control = sum % 11;
        return control != 10 && control == digits.charAt(9) - '0';
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. (?<rok>...) jest grupą zachwytującą z nazwą: tekst można odczytać (group("rok")) i użyć w zamienniku
     *      lub odwołaniu wstecznym. (?:...) tylko grupuje i nic nie zapamiętuje. Nazwaną grupę wybierasz, gdy
     *      potrzebujesz wartości; niezachwytującą, gdy grupa służy samej alternatywie lub kwantyfikatorowi.
     *   2. false — .*+ zjada cały tekst i nie oddaje, więc końcowe </b> nie ma już czego dopasować.
     *   3. "$5" w zamienniku to odwołanie do grupy 5, której nie ma: IndexOutOfBoundsException (No group 5).
     *      Poprawka: Matcher.quoteReplacement("$" + price) albo String.replace("cena", "$" + price).
     *   4. false (samo (?i) zna tylko ASCII), po zmianie na (?iu) wypisze true.
     *   5. [A-Z][a-z]+ zna tylko litery ASCII: "Żaneta" lub "Łukasz" zostaną pocięte lub pominięte.
     *      Poprawnie: \p{Lu}\p{Ll}+.
     *   6. XX — pierwsze dopasowanie to "aaa", a potem * pasuje jeszcze raz do pustego miejsca na końcu tekstu.
     *   7. Lookbehind (?<=\$) tylko sprawdza, że przed liczbą stoi $, ale go nie zjada — nie wchodzi do wyniku.
     *   8. Regex sprawdza tylko składnię, a nie to, czy skrzynka istnieje i czy należy do tej osoby; standard
     *      adresów jest zbyt złożony na jeden wzorzec. JSON jest zagnieżdżony i ma znaki ucieczki — regex nie liczy
     *      nawiasów ani nie zna zasad ucieczki; potrzebny jest parser.
     */
    // </editor-fold>
}
