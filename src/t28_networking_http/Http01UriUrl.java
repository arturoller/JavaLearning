package t28_networking_http;

import helpers.Check;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Adresy URI i URL, kodowanie znaków w adresie, podstawy HTTP
 *        (URI = Uniform Resource Identifier, jednolity identyfikator zasobu; URL = jego odmiana z „lokalizacją”;
 *         encoding = kodowanie; query = zapytanie; fragment = fragment)
 *
 * W SKRÓCIE:
 *   Adres w sieci ma dokładnie określoną budowę: schemat, host, port, ścieżka, zapytanie, fragment.
 *   Klasa URI potrafi go rozebrać, złożyć i porównać — bez żadnego ruchu w sieci. Znaki spoza „bezpiecznego”
 *   zestawu (polskie litery, spacje, {@code &}, ?) trzeba w adresie zakodować, bo inaczej adres znaczy co innego.
 *   Na końcu: metody, kody statusu, nagłówki i typy treści protokołu HTTP — słownik na następne lekcje.
 *
 * ANALOGIA: adres pocztowy. „ul. Długa 5/7, 80-001 Gdańsk” ma ustaloną budowę: miasto, ulica, numer. Jeśli numer
 *   domu wpiszesz w pole ulicy, listonosz zawiezie paczkę gdzie indziej. Podobnie w URI: znak ? oddziela ścieżkę
 *   od zapytania, więc pytajnik w nazwie produktu trzeba „zamaskować” (zakodować jako %3F).
 *
 * JAK TO DZIAŁA:
 *   https://ala:haslo@sklep.example.pl:8443/produkty/laptop?kat=ele{@code &}sort=cena#opis
 *   \___/   \_______/ \_______________/ \__/\_______________/ \______________/ \___/
 *   schemat  user-info       host       port     ścieżka         zapytanie     fragment
 *
 *   • fragment (#...) NIE jest wysyłany do serwera — czyta go tylko przeglądarka.
 *   • znak specjalny kodujemy jako %XX, gdzie XX to bajt w zapisie szesnastkowym (w UTF-8 litera ż to dwa
 *     bajty, więc %C5%BC).
 *   • w ZAPYTANIU i formularzach spację zapisuje się jako + albo %20; w ŚCIEŻCE tylko jako %20.
 *   • URI rozbiera i składa adresy (bez sieci). URL dodatkowo umie otworzyć połączenie — dziś zamiast URL
 *     używamy URI i HttpClient.
 *
 * SŁÓWKA: scheme = schemat (https); user info = dane użytkownika; host = gospodarz (nazwa komputera);
 *   port = port; path = ścieżka; query = zapytanie (po ?); fragment = fragment (po #); resolve = rozwiąż
 *   (względem bazy); relativize = ustal adres względny; normalize = uporządkuj (usuń ./ i ../);
 *   encode = zakoduj; decode = odkoduj; idempotent = idempotentny (powtórzenie nie zmienia wyniku);
 *   safe = bezpieczny (niczego nie zmienia na serwerze); status code = kod statusu; header = nagłówek
 *
 * ZOBACZ TEŻ: t28_networking_http/Net01SocketsTcpUdp (HTTP to tekst przez TCP), t28_networking_http/Http02HttpClient
 *   (wysyłanie żądań), t18_io_files/Io12Charsets (UTF-8 i bajty)
 * </pre>
 */
public class Http01UriUrl {

    private static final java.nio.charset.Charset UTF_8 = StandardCharsets.UTF_8; // UTF_8 = kodowanie UTF-8

    public static void main(String[] args) throws Exception {
        title("Http01 — URI, URL i podstawy HTTP");

        uriAnatomy();         // anatomy = anatomia, budowa
        resolveAndNormalize(); // resolve and normalize = rozwiązywanie względne i porządkowanie
        urlEncoding();        // url encoding = kodowanie w adresie
        buildingQueries();    // building queries = budowanie zapytań
        uriVersusUrl();       // uri versus url = URI kontra URL
        syntaxErrors();       // syntax errors = błędy składni
        httpMethods();        // http methods = metody HTTP
        statusCodes();        // status codes = kody statusu
        headersAndBody();     // headers and body = nagłówki i treść
        exercises();          // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. ANATOMIA URI
    // =================================================================================================

    /**
     * 1. URI.create(tekst) rozbiera tekst na części. Brakująca część to null (a port: -1).
     * Wszystkie gettery działają lokalnie — nic nie jest wysyłane do sieci.
     */
    static void uriAnatomy() {
        section("1. Anatomia adresu URI");

        URI uri = URI.create("https://ala:haslo@sklep.example.pl:8443/produkty/laptop?kat=ele&sort=cena#opis");
        show("schemat", uri.getScheme());            // getScheme = pobierz schemat
        // WYNIK: schemat → https
        show("dane użytkownika", uri.getUserInfo()); // getUserInfo = pobierz dane użytkownika
        // WYNIK: dane użytkownika → ala:haslo
        show("host", uri.getHost());                 // getHost = pobierz host
        // WYNIK: host → sklep.example.pl
        show("port", uri.getPort());                 // getPort = pobierz port
        // WYNIK: port → 8443
        show("ścieżka", uri.getPath());              // getPath = pobierz ścieżkę
        // WYNIK: ścieżka → /produkty/laptop
        show("zapytanie", uri.getQuery());           // getQuery = pobierz zapytanie
        // WYNIK: zapytanie → kat=ele&sort=cena
        show("fragment", uri.getFragment());         // getFragment = pobierz fragment
        // WYNIK: fragment → opis

        URI simple = URI.create("http://localhost/start");
        show("port, gdy go nie podano", simple.getPort());
        // WYNIK: port, gdy go nie podano → -1
        show("zapytanie, gdy go nie ma", simple.getQuery());
        // WYNIK: zapytanie, gdy go nie ma → null
        show("czy adres jest bezwzględny?", simple.isAbsolute()); // isAbsolute = czy ma schemat
        // WYNIK: czy adres jest bezwzględny? → true
        show("czy „../inne” jest bezwzględny?", URI.create("../inne").isAbsolute());
        // WYNIK: czy „../inne” jest bezwzględny? → false

        // PUŁAPKA: port -1 znaczy „nie podano”, a NIE „brak portu”. Domyślny port zależy od schematu
        // (http → 80, https → 443). Nie wpisuj -1 do połączenia — dopisz domyślną wartość samodzielnie.
        int effectivePort = simple.getPort() != -1 ? simple.getPort() : ("https".equals(simple.getScheme()) ? 443 : 80);
        show("port po uzupełnieniu domyślnym", effectivePort);
        // WYNIK: port po uzupełnieniu domyślnym → 80

        // PUŁAPKA: dane użytkownika w adresie (ala:haslo@) trafiają do logów, historii przeglądarki i błędów.
        // DOBRA PRAKTYKA: hasła i tokeny wysyłaj w nagłówku Authorization, nigdy w adresie.
        // Dlaczego: adresy bywają zapisywane i wyświetlane w wielu miejscach, nagłówki dużo rzadziej.
    }

    // =================================================================================================
    // 2. ROZWIĄZYWANIE I PORZĄDKOWANIE ADRESÓW
    // =================================================================================================

    /**
     * 2. resolve składa adres względny z bazą (tak działają odnośniki na stronach), relativize robi odwrotnie,
     * a normalize usuwa „./” i „xxx/../”.
     */
    static void resolveAndNormalize() {
        section("2. resolve, relativize i normalize");

        URI base = URI.create("http://localhost/sklep/produkty/lista.html");
        show("resolve(\"laptop.html\")", base.resolve("laptop.html"));
        // WYNIK: resolve("laptop.html") → http://localhost/sklep/produkty/laptop.html
        show("resolve(\"../kontakt\")", base.resolve("../kontakt"));
        // WYNIK: resolve("../kontakt") → http://localhost/sklep/kontakt
        show("resolve(\"/admin\")", base.resolve("/admin")); // ścieżka od korzenia zastępuje całą ścieżkę bazy
        // WYNIK: resolve("/admin") → http://localhost/admin

        URI root = URI.create("http://localhost/sklep/");
        URI full = URI.create("http://localhost/sklep/produkty/laptop");
        show("relativize", root.relativize(full)); // relativize = „jak dojść od root do full”
        // WYNIK: relativize → produkty/laptop

        URI messy = URI.create("http://localhost/a/./b/../c/");
        show("normalize", messy.normalize());
        // WYNIK: normalize → http://localhost/a/c/

        // PUŁAPKA: resolve „zjada” ostatni segment ścieżki, jeśli baza nie kończy się ukośnikiem.
        // Dla http://localhost/api/v1 ostatnim segmentem jest „v1” (jak plik w katalogu), więc zostanie zastąpiony.
        URI withoutSlash = URI.create("http://localhost/api/v1");
        URI withSlash = URI.create("http://localhost/api/v1/");
        show("baza bez ukośnika", withoutSlash.resolve("produkty"));
        // WYNIK: baza bez ukośnika → http://localhost/api/produkty
        show("baza z ukośnikiem", withSlash.resolve("produkty"));
        // WYNIK: baza z ukośnikiem → http://localhost/api/v1/produkty

        // DOBRA PRAKTYKA: adres bazowy API przechowuj z ukośnikiem na końcu albo składaj adres w jednym
        // miejscu (metoda pomocnicza). Dlaczego: brak jednego ukośnika po cichu zmienia adres, a błąd
        // wychodzi dopiero jako „404 Not Found” z serwera.

        // PUŁAPKA: normalize() nie sprawdza, czy ścieżka istnieje — to wyłącznie operacja na tekście.
        // Nie zastępuje walidacji (np. ochrony przed „../../etc/passwd” w nazwach plików z adresu).
    }

    // =================================================================================================
    // 3. KODOWANIE ZNAKÓW W ADRESIE
    // =================================================================================================

    /**
     * 3. URLEncoder.encode zamienia tekst na postać bezpieczną dla ZAPYTANIA (query) i formularzy.
     * Litery ASCII i cyfry zostają, spacja staje się plusem, reszta — %XX z bajtów UTF-8.
     */
    static void urlEncoding() throws URISyntaxException {
        section("3. Kodowanie: URLEncoder / URLDecoder");

        String text = "Zażółć gęślą jaźń & co?";
        // encode(tekst, kodowanie) — wersja z Charset istnieje od Java 10+ (wcześniej tylko nazwa tekstowa)
        String encoded = URLEncoder.encode(text, UTF_8); // encode = zakoduj
        show("zakodowane", encoded);
        // WYNIK: zakodowane → Za%C5%BC%C3%B3%C5%82%C4%87+g%C4%99%C5%9Bl%C4%85+ja%C5%BA%C5%84+%26+co%3F
        show("odkodowane", URLDecoder.decode(encoded, UTF_8)); // decode = odkoduj (Java 10+ z Charset)
        // WYNIK: odkodowane → Zażółć gęślą jaźń & co?

        // PUŁAPKA: URLEncoder jest do formularzy i zapytań — spacja zamienia się na +, a nie na %20.
        // W ścieżce adresu znak + oznacza zwykły plus, więc ścieżka „kawa+ziarnista” to NIE „kawa ziarnista”.
        show("spacja w URLEncoder", URLEncoder.encode("a b", UTF_8));
        // WYNIK: spacja w URLEncoder → a+b
        show("plus w URLEncoder", URLEncoder.encode("a+b", UTF_8));
        // WYNIK: plus w URLEncoder → a%2Bb
        show("URLDecoder: plus to spacja", URLDecoder.decode("a+b", UTF_8));
        // WYNIK: URLDecoder: plus to spacja → a b

        // DOBRA PRAKTYKA: ścieżkę koduj konstruktorem URI z oddzielnymi częściami — on sam koduje niedozwolone
        // znaki (spacje, polskie litery) zgodnie z regułami dla ścieżki.
        URI path = new URI("http", "localhost", "/produkty/kawa ziarnista/żółć", null);
        show("ścieżka z konstruktora URI", path.toASCIIString()); // toASCIIString = zapis tylko znakami ASCII
        // WYNIK: ścieżka z konstruktora URI → http://localhost/produkty/kawa%20ziarnista/%C5%BC%C3%B3%C5%82%C4%87
        show("ten sam adres po toString", path); // toString: spacje zakodowane, ale polskie litery zostają
        // WYNIK: ten sam adres po toString → http://localhost/produkty/kawa%20ziarnista/żółć

        // PUŁAPKA: kodowanie dwukrotne. Zakodowane „%C5%BC” zakodowane ponownie daje „%25C5%25BC”
        // (znak % to %25) — serwer odczyta tekst „%C5%BC” zamiast „ż”.
        show("podwójne kodowanie", URLEncoder.encode("Za%C5%BC", UTF_8));
        // WYNIK: podwójne kodowanie → Za%25C5%25BC
        // Zasada: koduj RAZ, tuż przed złożeniem adresu, i tylko pojedyncze wartości — nie cały gotowy adres.

        // PUŁAPKA: wersja encode(tekst) bez zestawu znaków jest przestarzała i używa kodowania domyślnego
        // platformy (które na Windows bywało inne niż na Linuksie). Zawsze podawaj UTF_8 jawnie.
    }

    // =================================================================================================
    // 4. BUDOWANIE ZAPYTAŃ
    // =================================================================================================

    /**
     * Składa zapytanie {@code klucz=wartość&klucz2=wartość2} z mapy. Zarówno klucz, jak i wartość są
     * kodowane OSOBNO — dzięki temu znaki {@code &} i {@code =} w wartościach niczego nie psują.
     * Używamy LinkedHashMap, żeby zachować kolejność wstawiania (HashMap miałby kolejność przypadkową).
     */
    static String buildQuery(Map<String, String> params) {
        StringJoiner joiner = new StringJoiner("&"); // StringJoiner = łącznik tekstów z separatorem
        for (Map.Entry<String, String> entry : params.entrySet()) { // entry = wpis (para klucz-wartość)
            joiner.add(URLEncoder.encode(entry.getKey(), UTF_8) + "=" + URLEncoder.encode(entry.getValue(), UTF_8));
        }
        return joiner.toString();
    }

    /** Odwrotność: zapytanie → mapa. Klucz bez „=” dostaje pustą wartość; kodowanie jest cofane. */
    static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> result = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) { // isEmpty = czy pusty
            return result;
        }
        for (String pair : rawQuery.split("&")) { // split = podziel
            String[] parts = pair.split("=", 2); // limit 2: „a=b=c” → klucz „a”, wartość „b=c”
            String key = URLDecoder.decode(parts[0], UTF_8);
            String value = parts.length > 1 ? URLDecoder.decode(parts[1], UTF_8) : "";
            result.put(key, value);
        }
        return result;
    }

    /**
     * 4. Zasada: NIGDY nie sklejaj zapytania z gołych wartości użytkownika ({@code "?q=" + tekst}).
     */
    static void buildingQueries() {
        section("4. Bezpieczne budowanie zapytań");

        String userInput = "kawa & herbata=?";

        // PRZED: sklejanie tekstu — znak & w środku wartości tworzy „dodatkowy parametr”.
        String bad = "http://localhost/szukaj?q=" + userInput + "&strona=1";
        show("PRZED (złe)", bad);
        // WYNIK: PRZED (złe) → http://localhost/szukaj?q=kawa & herbata=?&strona=1

        // PO: kodujemy każdą wartość.
        Map<String, String> params = new LinkedHashMap<>(); // LinkedHashMap = mapa zachowująca kolejność wstawiania
        params.put("q", userInput);
        params.put("strona", "1");
        String good = "http://localhost/szukaj?" + buildQuery(params);
        show("PO (dobre)", good);
        // WYNIK: PO (dobre) → http://localhost/szukaj?q=kawa+%26+herbata%3D%3F&strona=1

        // Serwer odczytuje dokładnie to, co wysłaliśmy:
        URI parsed = URI.create(good);
        show("rawQuery (surowe)", parsed.getRawQuery()); // getRawQuery = zapytanie bez odkodowania
        // WYNIK: rawQuery (surowe) → q=kawa+%26+herbata%3D%3F&strona=1
        show("sparsowane zapytanie", parseQuery(parsed.getRawQuery()));
        // WYNIK: sparsowane zapytanie → {q=kawa & herbata=?, strona=1}

        // PUŁAPKA: URI.getQuery() zwraca zapytanie JUŻ odkodowane: %26 staje się znakiem &, a %3D znakiem =,
        // więc nie da się go odróżnić od prawdziwych separatorów. Parametrów nie rozdzielisz poprawnie.
        // Do parsowania zawsze bierz getRawQuery() i dopiero potem odkoduj każdą wartość osobno.
        // (Zauważ też: getQuery nie zamienia + na spację — to robi dopiero URLDecoder.)
        show("getQuery (odkodowane — nie parsuj tego)", parsed.getQuery());
        // WYNIK: getQuery (odkodowane — nie parsuj tego) → q=kawa+&+herbata=?&strona=1

        // DOBRA PRAKTYKA: jedna metoda budująca zapytania (jak buildQuery) i jedna je czytająca (parseQuery).
        // Dlaczego: kodowanie w jednym miejscu to jedno miejsce do testowania — mniej błędów „czasem działa”.
        // Frameworki (Spring) robią to za Ciebie — t34_toward_spring pokaże ten most.
    }

    // =================================================================================================
    // 5. URI KONTRA URL
    // =================================================================================================

    /**
     * 5. URI to tylko tekst o określonej budowie. URL to adres, który potrafi się połączyć
     * (openStream, openConnection). Z URI do URL: {@code uri.toURL()}.
     */
    static void uriVersusUrl() throws MalformedURLException {
        section("5. URI kontra URL");

        URI uri = URI.create("http://localhost:8080/start?x=1");
        URL url = uri.toURL(); // toURL = zamień na URL (rzuca MalformedURLException, gdy schemat nieznany)
        show("protokół URL", url.getProtocol()); // getProtocol = pobierz protokół
        // WYNIK: protokół URL → http
        show("ścieżka URL", url.getPath());
        // WYNIK: ścieżka URL → /start
        show("URL.getFile (ścieżka + zapytanie)", url.getFile());
        // WYNIK: URL.getFile (ścieżka + zapytanie) → /start?x=1

        // Adres względny nie ma schematu, więc nie da się go zamienić na URL.
        expectThrows("toURL() na adresie względnym", () -> URI.create("../inne").toURL());
        // WYNIK: ✔ toURL() na adresie względnym → rzucono IllegalArgumentException: URI is not absolute

        // PUŁAPKA: URL.equals() i URL.hashCode() mogą pytać DNS o adres IP hosta, żeby sprawdzić, czy dwa
        // adresy wskazują ten sam komputer. Skutki: wolne działanie i zależność wyniku od sieci (np. dwie
        // różne nazwy tego samego komputera mogą zostać uznane za równe). Nie uruchamiamy tego tutaj,
        // bo wymaga sieci. Do porównań i kluczy map używaj URI (porównuje tylko tekst).

        // PUŁAPKA: konstruktor new URL(String) jest przestarzały od Java 20 (w Javie 17 jeszcze działa bez ostrzeżeń).
        // Zamiast niego od dawna zalecany zapis: URI.create(tekst).toURL() — rzuca przy błędzie jawny wyjątek
        // składni, zanim powstanie URL.

        // DOBRA PRAKTYKA: w kodzie przechowuj i porównuj URI; URL twórz tylko przy styku ze starym API.
        // Nowoczesny HttpClient (następna lekcja) przyjmuje URI.
    }

    // =================================================================================================
    // 6. BŁĘDY SKŁADNI
    // =================================================================================================

    /**
     * 6. Dwa sposoby tworzenia URI i dwa rodzaje błędu: konstruktor {@code new URI(...)} rzuca
     * kontrolowany URISyntaxException, a fabryka {@code URI.create(...)} — niekontrolowany
     * IllegalArgumentException (z URISyntaxException jako przyczyną). create służy do stałych,
     * o których wiesz, że są poprawne.
     */
    static void syntaxErrors() {
        section("6. URISyntaxException kontra IllegalArgumentException");

        expectThrows("new URI z spacją", () -> new URI("http://sklep/ala ma kota"));
        // WYNIK: ✔ new URI z spacją → rzucono URISyntaxException: Illegal character in path at index 16: http://sklep/ala ma kota
        expectThrows("URI.create ze spacją", () -> URI.create("http://sklep/ala ma kota"));
        // WYNIK: ✔ URI.create ze spacją → rzucono IllegalArgumentException: Illegal character in path at index 16: http://sklep/ala ma kota

        try {
            URI.create("http://sklep/ala ma kota");
        } catch (IllegalArgumentException e) { // getCause = pobierz przyczynę
            show("przyczyna z URI.create", e.getCause().getClass().getSimpleName());
            // WYNIK: przyczyna z URI.create → URISyntaxException
        }

        // PUŁAPKA: adres wpisany przez użytkownika wolno przepuszczać przez URI.create tylko razem z obsługą
        // IllegalArgumentException — inaczej „zła spacja” w formularzu kończy się nieobsłużonym wyjątkiem.
        // DOBRA PRAKTYKA: dla danych z zewnątrz użyj konstruktora new URI(...) (jawny wyjątek kontrolowany
        // zmusza do obsługi), dla stałych w kodzie — URI.create. Dlaczego: kompilator wymusza tam, gdzie błąd jest realny.
    }

    // =================================================================================================
    // 7. METODY HTTP
    // =================================================================================================

    /** Opis metody HTTP: czy jest bezpieczna, czy idempotentna, czy zwykle ma treść w żądaniu. */
    record HttpMethod(String name, boolean safe, boolean idempotent, boolean hasBody, String purpose) { // record = rekord (Java 16+)
    }

    static List<HttpMethod> methods() {
        return List.of(
                new HttpMethod("GET", true, true, false, "pobierz zasób"),
                new HttpMethod("HEAD", true, true, false, "jak GET, ale bez treści (same nagłówki)"),
                new HttpMethod("POST", false, false, true, "utwórz zasób albo wykonaj akcję"),
                new HttpMethod("PUT", false, true, true, "zastąp zasób całą treścią"),
                new HttpMethod("PATCH", false, false, true, "zmień część zasobu"),
                new HttpMethod("DELETE", false, true, false, "usuń zasób"));
    }

    /**
     * 7. Dwa pojęcia, które trzeba rozumieć: BEZPIECZNA metoda niczego nie zmienia na serwerze,
     * IDEMPOTENTNA metoda wywołana wiele razy daje ten sam skutek co jedno wywołanie.
     */
    static void httpMethods() {
        section("7. Metody HTTP: bezpieczne i idempotentne");

        for (HttpMethod method : methods()) {
            note(String.format(Locale.ROOT, "%-7s bezpieczna=%-5s idempotentna=%-5s treść=%-5s %s",
                    method.name(), method.safe(), method.idempotent(), method.hasBody(), method.purpose()));
        }
        // WYNIK: ℹ GET     bezpieczna=true  idempotentna=true  treść=false pobierz zasób
        // WYNIK: ℹ HEAD    bezpieczna=true  idempotentna=true  treść=false jak GET, ale bez treści (same nagłówki)
        // WYNIK: ℹ POST    bezpieczna=false idempotentna=false treść=true  utwórz zasób albo wykonaj akcję
        // WYNIK: ℹ PUT     bezpieczna=false idempotentna=true  treść=true  zastąp zasób całą treścią
        // WYNIK: ℹ PATCH   bezpieczna=false idempotentna=false treść=true  zmień część zasobu
        // WYNIK: ℹ DELETE  bezpieczna=false idempotentna=true  treść=false usuń zasób

        // Dlaczego idempotentność jest ważna: przy awarii sieci klient nie wie, czy żądanie dotarło.
        // PUT i DELETE można bezpiecznie ponowić (efekt ten sam), POST — nie (powstaną dwa zamówienia!).

        // PUŁAPKA: „bezpieczna” nie znaczy „zabezpieczona”. GET nie może niczego zmieniać (wyszukiwarki i
        // przeglądarki wywołują go samodzielnie), więc operacji „usuń” nigdy nie podpinaj pod GET /usun?id=5.
        // DOBRA PRAKTYKA: dobieraj metodę do znaczenia operacji. Dlaczego: pośrednicy (pamięć podręczna,
        // przeglądarka, mechanizm ponowień) zakładają znaczenie metody i według niego się zachowują.
    }

    // =================================================================================================
    // 8. KODY STATUSU
    // =================================================================================================

    /** Pierwsza cyfra kodu to kategoria odpowiedzi. */
    static String statusCategory(int code) {
        return switch (code / 100) { // switch jako wyrażenie (Java 14+)
            case 1 -> "informacyjny";
            case 2 -> "sukces";
            case 3 -> "przekierowanie";
            case 4 -> "błąd klienta";
            case 5 -> "błąd serwera";
            default -> "nieznany";
        };
    }

    /**
     * 8. Kod statusu mówi, co się stało. Pierwsza cyfra wystarcza do podstawowej decyzji:
     * 2xx — OK, 3xx — idź gdzie indziej, 4xx — ty zrobiłeś błąd, 5xx — ja (serwer) zrobiłem błąd.
     */
    static void statusCodes() {
        section("8. Kody statusu HTTP");

        Map<Integer, String> common = new TreeMap<>(); // TreeMap = mapa posortowana po kluczu
        common.put(200, "OK — wszystko w porządku");
        common.put(201, "Created — utworzono (zwykle po POST, z nagłówkiem Location)");
        common.put(204, "No Content — sukces bez treści (np. po DELETE)");
        common.put(301, "Moved Permanently — adres zmieniony na stałe");
        common.put(302, "Found — chwilowe przekierowanie (nagłówek Location)");
        common.put(304, "Not Modified — użyj kopii z pamięci podręcznej");
        common.put(400, "Bad Request — błędne żądanie (np. zły JSON)");
        common.put(401, "Unauthorized — brak (lub zły) dowód tożsamości");
        common.put(403, "Forbidden — wiem kim jesteś, ale nie wolno");
        common.put(404, "Not Found — nie ma takiego zasobu");
        common.put(405, "Method Not Allowed — ta metoda nie jest tu obsługiwana");
        common.put(409, "Conflict — konflikt ze stanem zasobu");
        common.put(500, "Internal Server Error — awaria po stronie serwera");
        common.put(503, "Service Unavailable — serwer chwilowo niedostępny");
        for (Map.Entry<Integer, String> entry : common.entrySet()) {
            note(entry.getKey() + " [" + statusCategory(entry.getKey()) + "] " + entry.getValue());
        }
        // WYNIK: ℹ 200 [sukces] OK — wszystko w porządku
        // WYNIK: ℹ 201 [sukces] Created — utworzono (zwykle po POST, z nagłówkiem Location)
        // WYNIK: ℹ 204 [sukces] No Content — sukces bez treści (np. po DELETE)
        // WYNIK: ℹ 301 [przekierowanie] Moved Permanently — adres zmieniony na stałe
        // WYNIK: ℹ 302 [przekierowanie] Found — chwilowe przekierowanie (nagłówek Location)
        // WYNIK: ℹ 304 [przekierowanie] Not Modified — użyj kopii z pamięci podręcznej
        // WYNIK: ℹ 400 [błąd klienta] Bad Request — błędne żądanie (np. zły JSON)
        // WYNIK: ℹ 401 [błąd klienta] Unauthorized — brak (lub zły) dowód tożsamości
        // WYNIK: ℹ 403 [błąd klienta] Forbidden — wiem kim jesteś, ale nie wolno
        // WYNIK: ℹ 404 [błąd klienta] Not Found — nie ma takiego zasobu
        // WYNIK: ℹ 405 [błąd klienta] Method Not Allowed — ta metoda nie jest tu obsługiwana
        // WYNIK: ℹ 409 [błąd klienta] Conflict — konflikt ze stanem zasobu
        // WYNIK: ℹ 500 [błąd serwera] Internal Server Error — awaria po stronie serwera
        // WYNIK: ℹ 503 [błąd serwera] Service Unavailable — serwer chwilowo niedostępny

        // PUŁAPKA: 401 nazywa się „Unauthorized”, ale znaczy „nieuwierzytelniony” (nie wiem, kim jesteś).
        // „Wiem, kim jesteś, ale nie wolno” to 403. Mylenie ich utrudnia klientowi właściwą reakcję
        // (przy 401 warto się zalogować ponownie, przy 403 — nie ma to sensu).

        // DOBRA PRAKTYKA: zwracaj kod najlepiej opisujący sytuację (404 dla nieistniejącego zasobu, 400 dla
        // błędnych danych, 201 dla utworzonego) — nie „200 z napisem błąd w treści”. Dlaczego: klienci,
        // pośrednicy i narzędzia monitorujące czytają kod, a nie treść.
    }

    // =================================================================================================
    // 9. NAGŁÓWKI I TREŚĆ
    // =================================================================================================

    /**
     * 9. Żądanie i odpowiedź HTTP mają ten sam kształt: linia startowa, nagłówki, pusta linia, treść.
     * Poniżej przykład w postaci tekstu (text block = blok tekstowy, Java 15+).
     */
    static void headersAndBody() {
        section("9. Nagłówki, treść i typy zawartości");

        String exchange = """
                POST /api/v1/produkty HTTP/1.1
                Host: sklep.example.pl
                Content-Type: application/json; charset=utf-8
                Accept: application/json
                Content-Length: 25

                {"nazwa":"Kawa","cena":9}

                HTTP/1.1 201 Created
                Content-Type: application/json; charset=utf-8
                Location: /api/v1/produkty/SPO-777
                Content-Length: 38
                """;
        for (String line : exchange.lines().toList()) { // lines() = podziel na linie (Java 11+), toList (Java 16+)
            note(line.isEmpty() ? "(pusta linia)" : line);
        }
        // WYNIK: ℹ POST /api/v1/produkty HTTP/1.1
        // WYNIK: ℹ Host: sklep.example.pl
        // WYNIK: ℹ Content-Type: application/json; charset=utf-8
        // WYNIK: ℹ Accept: application/json
        // WYNIK: ℹ Content-Length: 25
        // WYNIK: ℹ (pusta linia)
        // WYNIK: ℹ {"nazwa":"Kawa","cena":9}
        // WYNIK: ℹ (pusta linia)
        // WYNIK: ℹ HTTP/1.1 201 Created
        // WYNIK: ℹ Content-Type: application/json; charset=utf-8
        // WYNIK: ℹ Location: /api/v1/produkty/SPO-777
        // WYNIK: ℹ Content-Length: 38

        Map<String, String> contentTypes = new LinkedHashMap<>();
        contentTypes.put("text/plain; charset=utf-8", "zwykły tekst");
        contentTypes.put("text/html; charset=utf-8", "strona HTML");
        contentTypes.put("application/json", "dane w formacie JSON (kodowanie zawsze UTF-8)");
        contentTypes.put("application/x-www-form-urlencoded", "formularz: klucz=wartość&klucz=wartość (kodowane jak zapytanie)");
        contentTypes.put("multipart/form-data", "formularz z plikami");
        showEach("typy zawartości (Content-Type)", contentTypes);
        // WYNIK: typy zawartości (Content-Type) (liczba kluczy: 5):
        // WYNIK:    • text/plain; charset=utf-8 → zwykły tekst
        // WYNIK:    • text/html; charset=utf-8 → strona HTML
        // WYNIK:    • application/json → dane w formacie JSON (kodowanie zawsze UTF-8)
        // WYNIK:    • application/x-www-form-urlencoded → formularz: klucz=wartość&klucz=wartość (kodowane jak zapytanie)
        // WYNIK:    • multipart/form-data → formularz z plikami

        // Ważne rozróżnienie: Content-Type opisuje treść, KTÓRĄ WYSYŁASZ; Accept mówi, jakiej treści
        // OCZEKUJESZ w odpowiedzi. Content-Length to liczba BAJTÓW treści (nie znaków).
        // W przykładzie powyżej JSON ma 25 bajtów — policz je: nawiasy, cudzysłowy, dwukropki i przecinek.
        show("długość JSON-a w bajtach", "{\"nazwa\":\"Kawa\",\"cena\":9}".getBytes(UTF_8).length);
        // WYNIK: długość JSON-a w bajtach → 25

        // PUŁAPKA: nazwy nagłówków są niewrażliwe na wielkość liter (content-type = Content-Type), a wartości
        // (np. nazwy kodowania) zwykle nie. Nie porównuj nagłówków przez equals na surowym tekście.
        // DOBRA PRAKTYKA: zawsze podawaj Content-Type z charset=utf-8 przy tekście. Dlaczego: bez tego odbiorca
        // zgaduje kodowanie i polskie litery zamieniają się w „krzaczki”.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • URI = schemat://dane@host:port/ścieżka?zapytanie#fragment; brak części → null (port: -1).
     *   • resolve (adres względny + baza), relativize (odwrotnie), normalize (usuń ./ i ../); baza bez ukośnika
     *     na końcu „zjada” ostatni segment.
     *   • URLEncoder/URLDecoder (zawsze z UTF_8): do ZAPYTAŃ i formularzy, spacja ↔ +. Ścieżkę koduj
     *     konstruktorem new URI(schemat, host, ścieżka, fragment).
     *   • Zapytanie buduj z zakodowanych par; parsuj z getRawQuery(), nie z getQuery().
     *   • URL.equals/hashCode mogą pytać DNS — używaj URI; new URL(String) jest przestarzały od Java 20 →
     *     URI.create(...).toURL().
     *   • new URI(...) rzuca URISyntaxException (kontrolowany), URI.create(...) — IllegalArgumentException.
     *   • HTTP: GET/HEAD bezpieczne; GET/HEAD/PUT/DELETE idempotentne; POST i PATCH nie.
     *   • Kody: 2xx sukces, 3xx przekierowanie, 4xx błąd klienta, 5xx błąd serwera; 401 ≠ 403.
     *   • Content-Type opisuje wysyłaną treść, Accept — oczekiwaną; Content-Length w bajtach.
     *
     * PYTANIA KONTROLNE:
     *   1. Którą część adresu serwer NIE otrzymuje i dlaczego?
     *   2. Czym różni się 401 od 403? Który kod zwrócisz dla nieistniejącego produktu, a który dla błędnego JSON-a?
     *   3. Co wypisze:  URI.create("http://x/api/v1").resolve("lista");  ?
     *   4. Co wypisze:  URLEncoder.encode("a b+c", StandardCharsets.UTF_8);  ?
     *   5. ZNAJDŹ BŁĄD:  String url = "http://localhost/szukaj?q=" + tekstUzytkownika;
     *                    (co się stanie dla tekstu „rock&roll”?)
     *   6. Dlaczego POST nie jest idempotentny, a PUT jest? Co z tego wynika przy ponawianiu żądań?
     *   7. Co wypisze:  URI.create("http://localhost").getPort();  ?
     *   8. Dlaczego do porównywania adresów używamy URI, a nie URL?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Map<String, String> input = new LinkedHashMap<>();
        input.put("q", "kawa & herbata");
        input.put("strona", "2");
        Map<String, String> parsedExpected = new LinkedHashMap<>();
        parsedExpected.put("q", "kawa & herbata");
        parsedExpected.put("strona", "2");
        parsedExpected.put("pusty", "");

        Check.equal("ćw. 1: host", "sklep.example.pl", () -> exercise1("https://sklep.example.pl:8443/a?b=c"));
        Check.equal("ćw. 2: GET idempotentny", true, () -> exercise2("GET"));
        Check.equal("ćw. 2: POST nieidempotentny", false, () -> exercise2("POST"));
        Check.equal("ćw. 2: DELETE idempotentny", true, () -> exercise2("DELETE"));
        Check.equal("ćw. 3: zapytanie", "q=kawa+%26+herbata&strona=2", () -> exercise3(input));
        Check.equal("ćw. 4: parsowanie", parsedExpected, () -> exercise4("q=kawa+%26+herbata&strona=2&pusty"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "sklep.example.pl", () -> solution1("https://sklep.example.pl:8443/a?b=c"));
        Check.equal("ćw. 2 (wzorzec): GET", true, () -> solution2("GET"));
        Check.equal("ćw. 2 (wzorzec): POST", false, () -> solution2("POST"));
        Check.equal("ćw. 2 (wzorzec): DELETE", true, () -> solution2("DELETE"));
        Check.equal("ćw. 3 (wzorzec)", "q=kawa+%26+herbata&strona=2", () -> solution3(input));
        Check.equal("ćw. 4 (wzorzec)", parsedExpected, () -> solution4("q=kawa+%26+herbata&strona=2&pusty"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć nazwę hosta z podanego adresu (bez portu i ścieżki).
     * Podpowiedź: URI.create(...).getHost().
     */
    static String exercise1(String address) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (łatwe): czy metoda HTTP o podanej nazwie jest idempotentna?
     * Podpowiedź: skorzystaj z listy {@code methods()} i pola {@code idempotent()}; nieznana metoda → wyjątek.
     */
    static boolean exercise2(String methodName) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): złóż zapytanie z mapy (klucze i wartości zakodowane, rozdzielone znakiem {@code &})
     * — ale tym razem BEZ metody buildQuery, napisz własną wersję.
     * PRZEPISZ:
     * <pre>{@code
     * String query = "";
     * for (var e : params.entrySet()) {
     *     query += e.getKey() + "=" + e.getValue() + "&";   // brak kodowania, zbędny & na końcu
     * }
     * }</pre>
     * Podpowiedź: URLEncoder.encode(..., UTF_8) dla każdej części, a do łączenia — StringJoiner.
     */
    static String exercise3(Map<String, String> params) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): rozbierz surowe zapytanie na mapę (zachowaj kolejność), odkodowując klucze
     * i wartości. Klucz bez znaku „=” ma pustą wartość. Wartość może zawierać „=”.
     * Podpowiedź: split("&"), potem split("=", 2); URLDecoder.decode(..., UTF_8).
     */
    static Map<String, String> exercise4(String rawQuery) {
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(String address) {
        return URI.create(address).getHost();
    }

    static boolean solution2(String methodName) {
        for (HttpMethod method : methods()) {
            if (method.name().equals(methodName)) {
                return method.idempotent();
            }
        }
        throw new IllegalArgumentException("nieznana metoda: " + methodName);
    }

    static String solution3(Map<String, String> params) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            parts.add(URLEncoder.encode(entry.getKey(), UTF_8) + "=" + URLEncoder.encode(entry.getValue(), UTF_8));
        }
        return String.join("&", parts);
    }

    static Map<String, String> solution4(String rawQuery) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            result.put(URLDecoder.decode(parts[0], UTF_8), parts.length > 1 ? URLDecoder.decode(parts[1], UTF_8) : "");
        }
        return result;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Fragment (część po #): jest przeznaczony dla przeglądarki (np. przewinięcie do sekcji strony),
     *      więc klient nie dołącza go do żądania.
     *   2. 401 = nie wiem, kim jesteś (brak lub zły dowód tożsamości); 403 = wiem, kim jesteś, ale nie masz
     *      uprawnień. Nieistniejący produkt → 404, błędny JSON → 400.
     *   3. http://x/api/lista — baza nie kończy się ukośnikiem, więc segment „v1” zostaje zastąpiony.
     *   4. a+b%2Bc — spacja → +, plus → %2B.
     *   5. Znak & zakończy parametr q na słowie „rock”, a „roll” zostanie odczytane jako osobny parametr.
     *      Wartość trzeba zakodować: URLEncoder.encode(tekstUzytkownika, UTF_8) (rock%26roll).
     *   6. Dwa razy POST tworzą dwa zasoby; dwa razy PUT z tą samą treścią zostawia zasób w tym samym stanie.
     *      Przy awarii sieci PUT (i DELETE, GET) można ponowić bez ryzyka; POST — tylko z dodatkowym
     *      zabezpieczeniem (np. klucz idempotentności).
     *   7. -1 (port nie został podany w adresie).
     *   8. URL.equals/hashCode mogą odpytywać DNS (wolno, zależne od sieci); URI porównuje wyłącznie tekst.
     */
    // </editor-fold>
}
