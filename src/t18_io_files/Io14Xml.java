package t18_io_files;

import helpers.Check;
import helpers.TempDir;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: XML — DOM, StAX i XPath w JDK, czytanie, zapis i bezpieczne parsowanie
 *        (XML = Extensible Markup Language, rozszerzalny język znaczników; DOM = Document Object Model, drzewo w pamięci;
 *         StAX = Streaming API for XML, czytanie strumieniowe; XPath = język zapytań do drzewa XML;
 *         parse = przeanalizuj i zbuduj z tekstu; element = węzeł-znacznik; attribute = atrybut)
 *
 * W SKRÓCIE:
 *   XML to tekst, w którym dane są ułożone w drzewo elementów: {@code <ksiazka id="KS-001"><tytul>Czysty kod</tytul></ksiazka>}.
 *   JDK ma trzy style pracy: DOM (całe drzewo w pamięci, wygodne), StAX (czytasz element po elemencie,
 *   mało pamięci) i XPath (zapytania w stylu „wszystkie tytuły książek po 2010 roku”).
 *   Najważniejsze zasady: parsuj BEZPIECZNIE (bez DOCTYPE), podawaj kodowanie, wiedz o węzłach z samymi spacjami.
 *
 * ANALOGIA: dokument XML to drzewo genealogiczne zapisane tekstem.
 *   DOM to rozłożenie całego drzewa na stole — widzisz wszystko, możesz dopisywać i skreślać, ale zajmuje cały stół.
 *   StAX to czytanie księgi rodzinnej strona po stronie: pamiętasz tylko bieżącą stronę, więc mieści się każda księga.
 *   XPath to pytanie do archiwisty: „podaj wszystkich wnuków urodzonych po 2010”.
 *
 * JAK TO DZIAŁA:
 *   Poprawny XML (ang. well-formed = poprawnie sformułowany):
 *     • dokładnie jeden element główny (korzeń),
 *     • każdy znacznik zamknięty i poprawnie zagnieżdżony, wielkość liter ma znaczenie,
 *     • wartości atrybutów w cudzysłowach,
 *     • znaki specjalne w tekście zapisane ucieczką (ang. escape): ciąg zaczynający się od ampersanda i kończący
 *       średnikiem, z nazwą w środku: lt = znak mniejszości, gt = znak większości, amp = sam ampersand,
 *       quot = cudzysłów, apos = apostrof. Np. tytuł „Prawo i porządek” z ampersandem zapisujemy jako ampersand-amp-średnik.
 *   „Poprawny” to nie to samo co „zgodny ze schematem” (valid): schemat (DTD, XSD) opisuje, JAKIE elementy są dozwolone.
 *   Parser sprawdza poprawność składni zawsze, zgodność ze schematem tylko, gdy o to poprosisz (javax.xml.validation).
 *
 *   Styl      API w JDK                       pamięć            kiedy
 *   --------  ------------------------------  ----------------  ---------------------------------------------
 *   DOM       DocumentBuilder, Document       cały dokument     małe pliki, edycja drzewa, swobodne nawigowanie
 *   StAX      XMLStreamReader / Writer        stała (strumień)  duże pliki, jednokrotny przebieg, generowanie XML
 *   XPath     XPathFactory, XPath             jak DOM           zapytania do drzewa DOM
 *   SAX       SAXParser + DefaultHandler      stała (zdarzenia) starszy styl „push” — dziś zwykle StAX
 *
 *   Wyjątki: ParserConfigurationException, SAXException (SAXParseException ma numer linii i kolumny),
 *   XMLStreamException, TransformerException, XPathExpressionException — to NIE są podklasy IOException,
 *   osobne wyjątki sprawdzane. IOException dochodzi przy czytaniu pliku.
 *
 * SŁÓWKA:
 *   element = element (znacznik z zawartością); attribute = atrybut; text node = węzeł tekstowy; root = korzeń;
 *   well-formed = poprawnie sformułowany; valid = zgodny ze schematem; namespace = przestrzeń nazw;
 *   escape = ucieczka (zapis znaku specjalnego); indent = wcięcie; transformer = przekształcacz (zapisuje drzewo do tekstu);
 *   external entity = encja zewnętrzna; XXE = atak przez encje zewnętrzne; pull = ciągnij (sam pytasz o kolejne zdarzenie).
 *
 * ZOBACZ TEŻ: t18_io_files/Io05JsonManual (inny format wymiany danych), t18_io_files/Io12Charsets (kodowanie UTF-8 w nagłówku XML),
 *   t18_io_files/Io06Properties (prostszy format konfiguracji), t10_exceptions/Exceptions06ChainingWrapping (opakowywanie wyjątków)
 * </pre>
 */
public class Io14Xml {

    /** CATALOG = mały katalog biblioteki użyty w całej lekcji (blok tekstowy, Java 15+; text block). */
    private static final String CATALOG = """
            <?xml version="1.0" encoding="UTF-8"?>
            <biblioteka nazwa="Biblioteka Miejska">
              <ksiazka id="KS-001" rok="2008">
                <tytul>Czysty kod</tytul>
                <autor>Anna Nowak</autor>
                <cena>79.00</cena>
              </ksiazka>
              <ksiazka id="KS-002" rok="2019">
                <tytul>Java. Podstawy</tytul>
                <autor>Jan Kowalski</autor>
                <cena>129.00</cena>
              </ksiazka>
              <ksiazka id="KS-003" rok="2012">
                <tytul>Wzorce projektowe</tytul>
                <autor>Maria Wiśniewska</autor>
                <cena>99.00</cena>
              </ksiazka>
              <ksiazka id="KS-004" rok="2021">
                <tytul>Zażółć gęślą jaźń</tytul>
                <autor>Piotr Zieliński</autor>
                <cena>49.50</cena>
              </ksiazka>
              <ksiazka id="KS-005" rok="2005">
                <tytul>Prawo [AMP] porządek</tytul>
                <autor>Anna Nowak</autor>
                <cena>35.00</cena>
              </ksiazka>
            </biblioteka>
            """.replace("[AMP]", "&" + "amp;"); // ucieczka dla ampersanda; sklejona, bo lekcja nie ma jej pokazywać dosłownie

    // throws Exception = „rzuca Exception”: parsery XML mają własne wyjątki sprawdzane (nie IOException),
    // więc dla czytelności lekcji main przekazuje wszystkie wyżej
    public static void main(String[] args) throws Exception {
        title("Io14 — XML: DOM, StAX, XPath");

        Path dir = TempDir.create("io14"); // create = utwórz
        try {
            wellFormed();                       // well-formed = poprawnie sformułowany
            secureParsing(dir.resolve("s2"));   // secure parsing = bezpieczne parsowanie
            domReading();                       // DOM reading = czytanie przez DOM
            whitespaceNodes();                  // whitespace nodes = węzły z samymi spacjami
            domWriting(dir.resolve("s5"));      // DOM writing = zapis przez DOM i Transformer
            staxReading(dir.resolve("s6"));     // StAX reading = czytanie przez StAX
            staxWriting();                      // StAX writing = zapis przez StAX
            xpathQueries();                     // XPath queries = zapytania XPath
            namespacesAndOthers();              // namespaces and others = przestrzenie nazw i inne API
            exercises();                        // exercises = ćwiczenia
        } finally {
            TempDir.deleteRecursively(dir);
        }
    }

    // =================================================================================================
    // Pomocnicze
    // =================================================================================================

    /** Wartość, którą da się policzyć, ale obliczenie może rzucić dowolny wyjątek sprawdzany (parsery XML mają własne). */
    @FunctionalInterface
    private interface Risky<T> {
        T get() throws Exception;
    }

    /** attempt = „spróbuj”: opakowuje wyjątek sprawdzany w niesprawdzany, żeby pasowało do Supplier w Check.equal. */
    private static <T> Supplier<T> attempt(Risky<T> risky) {
        return () -> {
            try {
                return risky.get();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };
    }

    /**
     * secureBuilder = fabryka parsera DOM z zabezpieczeniami. Wyjaśnienie każdej linii:
     * disallow-doctype-decl = odrzuć dokument z deklaracją DOCTYPE (tędy wchodzą encje zewnętrzne i „bomby”),
     * FEATURE_SECURE_PROCESSING = ograniczenia zasobów, XInclude wyłączone = brak wciągania obcych plików.
     * DefaultHandler jako obsługa błędów: bez niego parser pisałby komunikat "[Fatal Error]" na System.err.
     */
    private static DocumentBuilder secureBuilder(boolean namespaceAware) throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setNamespaceAware(namespaceAware);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new DefaultHandler()); // fatalError rzuca wyjątek, nic nie wypisuje
        return builder;
    }

    /** parse = zamień tekst XML na drzewo DOM (bezpiecznie). InputSource(Reader) = tekst już zdekodowany jako znaki. */
    private static Document parse(String xml) throws Exception {
        return secureBuilder(false).parse(new InputSource(new StringReader(xml)));
    }

    /** text = tekst pierwszego pod-elementu o danej nazwie (np. tytuł książki). */
    private static String text(Element parent, String childName) {
        return parent.getElementsByTagName(childName).item(0).getTextContent().trim();
    }

    /** childElements = tylko dzieci-ELEMENTY (bez węzłów tekstowych ze spacjami między znacznikami). */
    private static List<Element> childElements(Element parent) {
        List<Element> result = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) { // ELEMENT_NODE = węzeł-znacznik
                result.add((Element) children.item(i));                // rzutowanie Node → Element
            }
        }
        return result;
    }

    /** removeWhitespace = usuwa węzły tekstowe złożone wyłącznie ze spacji i znaków nowej linii (rekurencyjnie). */
    private static void removeWhitespace(Node node) {
        NodeList children = node.getChildNodes();
        for (int i = children.getLength() - 1; i >= 0; i--) { // od końca, bo usuwanie zmienia indeksy
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE && child.getTextContent().isBlank()) { // isBlank = same białe znaki (Java 11+)
                node.removeChild(child);
            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                removeWhitespace(child);
            }
        }
    }

    /**
     * serialize = zapisuje drzewo DOM do tekstu przez Transformer. Znaki końca linii normalizujemy do "\n",
     * bo Transformer używa separatora systemu (na Windows "\r\n") — inaczej wydruk różniłby się między systemami.
     */
    private static String serialize(Node node, boolean omitDeclaration) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");        // nie sięgaj po zewnętrzne DTD
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, ""); // ani po zewnętrzne arkusze stylów
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");           // wcięcia
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2"); // 2 spacje (właściwość Xalana w JDK)
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitDeclaration ? "yes" : "no");
        StringWriter out = new StringWriter();
        transformer.transform(new DOMSource(node), new StreamResult(out));
        return out.toString().replace("\r\n", "\n");
    }

    // =================================================================================================
    // 1. POPRAWNY XML
    // =================================================================================================

    /**
     * 1. Parser najpierw sprawdza, czy dokument jest poprawnie sformułowany (well-formed). Jedna usterka
     * (niezamknięty znacznik, goły ampersand) i dostajemy SAXParseException z numerem linii i kolumny.
     * Treść komunikatu zależy od implementacji i języka, więc wypisujemy tylko wynik i miejsce błędu.
     */
    static void wellFormed() {
        section("1. Poprawny (well-formed) XML i błędy składni");

        String[][] cases = {
                {"poprawny element pusty", "<a/>"},
                {"niezamknięty b", "<a><b></a>"},
                {"inna wielkość liter w znaczniku zamykającym", "<a></A>"},
                {"atrybut bez cudzysłowu", "<a x=1/>"},
                {"dwa elementy główne", "<a/><b/>"},
                {"goły znak mniejszości w tekście", "<a>1 < 2</a>"},
                {"goły ampersand w tekście", "<a>R&D</a>"},
                {"ampersand zapisany ucieczką", "<a>R" + "&" + "amp;D</a>"},
                {"pusty dokument", ""}};
        for (String[] c : cases) {
            String result;
            try {
                parse(c[1]);
                result = "poprawny";
            } catch (SAXParseException e) {
                result = "BŁĄD w linii " + e.getLineNumber() + ", kolumnie " + e.getColumnNumber();
            } catch (Exception e) {
                result = "inny wyjątek " + e.getClass().getSimpleName();
            }
            show(c[0], result);
        }
        // WYNIK: poprawny element pusty → poprawny
        // WYNIK: niezamknięty b → BŁĄD w linii 1, kolumnie 9
        // WYNIK: inna wielkość liter w znaczniku zamykającym → BŁĄD w linii 1, kolumnie 6
        // WYNIK: atrybut bez cudzysłowu → BŁĄD w linii 1, kolumnie 6
        // WYNIK: dwa elementy główne → BŁĄD w linii 1, kolumnie 6
        // WYNIK: goły znak mniejszości w tekście → BŁĄD w linii 1, kolumnie 7
        // WYNIK: goły ampersand w tekście → BŁĄD w linii 1, kolumnie 7
        // WYNIK: ampersand zapisany ucieczką → poprawny
        // WYNIK: pusty dokument → BŁĄD w linii 1, kolumnie 1

        // PUŁAPKA: XML jest rygorystyczny — w przeciwieństwie do HTML nie wybacza błędów. Jeden błąd przerywa
        //   parsowanie (to „błąd krytyczny”). Zapisując XML ręcznie sklejanym tekstem łatwo o goły & lub < w danych;
        //   dlatego XML generuj przez API (DOM, StAX), które robi ucieczkę samo (sekcje 5 i 7).
        // Poprawny (well-formed) ≠ zgodny ze schematem (valid): <a/> jest poprawny, ale nie spełni schematu,
        // który wymaga elementu <ksiazka>. Zgodność sprawdza javax.xml.validation.SchemaFactory z plikiem XSD (poza lekcją).
    }

    // =================================================================================================
    // 2. BEZPIECZNE PARSOWANIE
    // =================================================================================================

    /**
     * 2. XXE (XML External Entity) to klasyczna luka: dokument XML w deklaracji DOCTYPE definiuje „encję”,
     * która wskazuje na plik lub adres, a parser wstawia zawartość tego pliku do dokumentu. Atakujący wysyła XML,
     * a serwer odsyła mu np. zawartość swoich plików. Domyślny parser JDK PRZESTAJE takie encje rozwiązywać
     * dopiero wtedy, gdy mu to zabronisz. Pokażemy to na bezpiecznym przykładzie: plik "tajne.txt" w katalogu tymczasowym.
     */
    static void secureParsing(Path dir) throws Exception {
        section("2. Bezpieczne parsowanie — XXE i DOCTYPE");
        Files.createDirectories(dir);
        Path secret = dir.resolve("tajne.txt");
        Files.writeString(secret, "TAJNA ZAWARTOŚĆ\n", StandardCharsets.UTF_8);

        // Złośliwy XML: encja "e" wskazuje na plik. secret.toUri() = adres file:///...; sam adres nie jest wypisywany.
        String evil = "<?xml version=\"1.0\"?>\n<!DOCTYPE x [<!ENTITY e SYSTEM \"" + secret.toUri() + "\">]>\n<x>&e;</x>";

        // NIEBEZPIECZNIE: domyślna fabryka. Tylko demonstracja — tak NIE piszemy kodu produkcyjnego.
        DocumentBuilder unsafe = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        unsafe.setErrorHandler(new DefaultHandler());
        Document stolen = unsafe.parse(new InputSource(new StringReader(evil)));
        show("domyślny parser wczytał plik do dokumentu", stolen.getDocumentElement().getTextContent().trim());
        // WYNIK: domyślny parser wczytał plik do dokumentu → TAJNA ZAWARTOŚĆ

        // BEZPIECZNIE: fabryka z secureBuilder (feature "disallow-doctype-decl" = zabroń DOCTYPE).
        try {
            parse(evil);
            System.out.println("✘ secureBuilder → NIE rzucono wyjątku (a spodziewaliśmy się go)");
        } catch (SAXParseException e) {
            System.out.println("✔ secureBuilder → rzucono " + e.getClass().getSimpleName() + " (linia " + e.getLineNumber() + ")");
            // WYNIK: ✔ secureBuilder → rzucono SAXParseException (linia 2)
        }

        // Zwykły dokument bez DOCTYPE działa bez zmian:
        show("bezpieczny parser, zwykły XML", parse(CATALOG).getDocumentElement().getTagName());
        // WYNIK: bezpieczny parser, zwykły XML → biblioteka

        // Inne ataki z tej rodziny: „miliard śmiechów” (billion laughs) — encje definiowane przez inne encje,
        // 100 bajtów rozwija się do gigabajtów pamięci; SSRF — encja wskazuje adres http://wewnętrzny-serwer, a parser
        // sam „dzwoni” do sieci wewnętrznej. Wszystkie wchodzą przez DOCTYPE, więc jego zakaz zamyka drzwi.
        // PUŁAPKA: domyślna konfiguracja parsera NIE jest bezpieczna — to najczęstsza luka w aplikacjach czytających XML
        //   od użytkowników (pliki importu, SOAP, SAML). Dotyczy też SAXParserFactory, XMLInputFactory, TransformerFactory.
        // DOBRA PRAKTYKA: każdą fabrykę XML konfiguruj w jednym miejscu (jak secureBuilder) i używaj tylko jej.
        //   Dla StAX: SUPPORT_DTD = false (sekcja 6), dla Transformera: ACCESS_EXTERNAL_DTD = "" (sekcja 5).
        // Jeśli MUSISZ obsłużyć DOCTYPE (rzadkie), wyłącz przynajmniej encje zewnętrzne:
        //   external-general-entities = false, external-parameter-entities = false, load-external-dtd = false.
    }

    // =================================================================================================
    // 3. DOM — CZYTANIE
    // =================================================================================================

    /**
     * 3. DOM buduje w pamięci drzewo: Document → Element (znaczniki) → Node (węzły, w tym tekst).
     * getElementsByTagName szuka elementów po nazwie, getAttribute czyta atrybuty, getTextContent — tekst.
     * Wszystkie wartości z XML to TEKST — liczby trzeba sparsować (BigDecimal dla pieniędzy, Integer.parseInt dla roku).
     */
    static void domReading() throws Exception {
        section("3. DOM — czytanie dokumentu");

        Document doc = parse(CATALOG);
        Element root = doc.getDocumentElement();   // getDocumentElement = element główny
        show("element główny", root.getTagName());
        // WYNIK: element główny → biblioteka
        show("atrybut nazwa", root.getAttribute("nazwa")); // getAttribute = wartość atrybutu
        // WYNIK: atrybut nazwa → Biblioteka Miejska

        NodeList books = doc.getElementsByTagName("ksiazka"); // NodeList = „lista” węzłów (nie jest java.util.List!)
        show("liczba książek", books.getLength());
        // WYNIK: liczba książek → 5

        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < books.getLength(); i++) {
            Element book = (Element) books.item(i);
            int year = Integer.parseInt(book.getAttribute("rok"));
            BigDecimal price = new BigDecimal(text(book, "cena"));
            total = total.add(price);
            System.out.println("   • " + book.getAttribute("id") + " | " + year + " | " + text(book, "tytul") + " | " + price);
        }
        // WYNIK: • KS-001 | 2008 | Czysty kod | 79.00
        // WYNIK: • KS-002 | 2019 | Java. Podstawy | 129.00
        // WYNIK: • KS-003 | 2012 | Wzorce projektowe | 99.00
        // WYNIK: • KS-004 | 2021 | Zażółć gęślą jaźń | 49.50
        // WYNIK: • KS-005 | 2005 | Prawo & porządek | 35.00
        show("suma cen (BigDecimal)", total);
        // WYNIK: suma cen (BigDecimal) → 391.50

        // Ucieczka w pliku (ampersand zapisany jako encja) parser zamienia na zwykły znak: w drzewie jest "Prawo & porządek".

        // PUŁAPKA: brakujący atrybut to NIE null, tylko pusty tekst. Odróżnia je hasAttribute.
        Element first = (Element) books.item(0);
        show("getAttribute(\"isbn\") (brak atrybutu)", "[" + first.getAttribute("isbn") + "]");
        // WYNIK: getAttribute("isbn") (brak atrybutu) → []
        show("hasAttribute(\"isbn\")", first.hasAttribute("isbn")); // has = ma
        // WYNIK: hasAttribute("isbn") → false
        // PUŁAPKA: getElementsByTagName("nieistnieje").item(0) to null — wywołanie na nim getTextContent da NullPointerException.
        //   Sprawdzaj getLength() albo użyj XPath (sekcja 8), który po prostu zwróci pusty wynik.
        // PUŁAPKA: getTextContent elementu zwraca tekst WSZYSTKICH potomków sklejony razem (razem ze spacjami z wcięć).
        //   Dla elementu-liścia (tytul, cena) to dokładnie jego tekst; dla książki — mieszanka. Dlatego trim() przy liściach.

        // Kodowanie: gdy XML czytamy z BAJTÓW (plik, sieć), parser sam odczytuje je z deklaracji w pierwszej linii
        // (encoding="..."), a przy braku deklaracji zakłada UTF-8. Tu: te same dane zapisane w ISO-8859-2.
        String latin2Xml = "<?xml version=\"1.0\" encoding=\"ISO-8859-2\"?><t>zażółć</t>";
        byte[] latin2Bytes = latin2Xml.getBytes(Charset.forName("ISO-8859-2"));
        Document fromBytes = secureBuilder(false).parse(new ByteArrayInputStream(latin2Bytes));
        show("odczyt z bajtów ISO-8859-2 wg deklaracji", fromBytes.getDocumentElement().getTextContent());
        // WYNIK: odczyt z bajtów ISO-8859-2 wg deklaracji → zażółć
        // Gdy do parsera dajesz Reader/String (InputSource(Reader)), deklaracja encoding jest IGNOROWANA: dekodowanie
        // zrobił już Reader. DOBRA PRAKTYKA: plik XML czytaj z InputStream/File (parser zdecyduje o kodowaniu sam),
        // a zapisuj w UTF-8 z deklaracją encoding="UTF-8" — patrz t18_io_files/Io12Charsets.
    }

    // =================================================================================================
    // 4. WĘZŁY ZE SPACJAMI
    // =================================================================================================

    /**
     * 4. Wcięcia i nowe linie między znacznikami to też DANE dla parsera: powstają z nich węzły tekstowe
     * (text nodes). Dlatego getChildNodes() zwraca więcej, niż się spodziewasz. Pięć książek to nie pięć dzieci,
     * lecz pięć elementów i sześć węzłów ze spacjami.
     */
    static void whitespaceNodes() throws Exception {
        section("4. Pułapka: węzły tekstowe ze spacjami");

        Element root = parse(CATALOG).getDocumentElement();
        show("getChildNodes().getLength()", root.getChildNodes().getLength());
        // WYNIK: getChildNodes().getLength() → 11
        show("pierwsze dziecko to węzeł tekstowy", root.getFirstChild().getNodeType() == Node.TEXT_NODE);
        // WYNIK: pierwsze dziecko to węzeł tekstowy → true
        show("jego tekst po trim() jest pusty", root.getFirstChild().getTextContent().trim().isEmpty());
        // WYNIK: jego tekst po trim() jest pusty → true
        show("tylko elementy (childElements)", childElements(root).size());
        // WYNIK: tylko elementy (childElements) → 5
        show("elementy wewnątrz jednej książki", childElements(childElements(root).get(0)).size());
        // WYNIK: elementy wewnątrz jednej książki → 3

        // PUŁAPKA: getFirstChild() pierwszej książki to NIE <tytul>, tylko węzeł ze spacjami; getFirstChild().getTextContent()
        //   zwraca więc wcięcie. Kod „pobierz pierwsze dziecko i zrzutuj na Element” kończy się ClassCastException.
        Element book = childElements(root).get(0);
        show("getFirstChild() książki jest Elementem", book.getFirstChild() instanceof Element); // instanceof = „jest typu”
        // WYNIK: getFirstChild() książki jest Elementem → false

        // Rozwiązania: (1) filtrować po getNodeType() == ELEMENT_NODE (childElements); (2) używać getElementsByTagName,
        // które zwraca tylko elementy; (3) usunąć węzły ze spacjami (removeWhitespace) — potrzebne przed zapisem z wcięciami (sekcja 5);
        // (4) XPath (sekcja 8) w ogóle ich nie widzi, gdy pytasz o elementy. Opcja setIgnoringElementContentWhitespace
        // działa wyłącznie dla dokumentów sprawdzanych względem DTD — dla zwykłego XML nic nie robi, więc nie pomoże.
        Element cleaned = parse(CATALOG).getDocumentElement();
        removeWhitespace(cleaned);
        show("po removeWhitespace getChildNodes().getLength()", cleaned.getChildNodes().getLength());
        // WYNIK: po removeWhitespace getChildNodes().getLength() → 5
        // Uwaga: usuwanie spacji psuje tekst, w którym spacje są ważne (np. akapit z wielu linii) — robimy to tylko
        // dla dokumentów „danych”, nie „tekstów” (mixed content).
    }

    // =================================================================================================
    // 5. DOM — ZAPIS I ZMIANY
    // =================================================================================================

    /**
     * 5. Drzewo DOM można zmieniać: createElement, setAttribute, appendChild, setTextContent, removeChild.
     * Zapis do tekstu robi Transformer (OutputKeys.INDENT = wcięcia). Ucieczki znaków specjalnych robi serializator
     * sam — nigdy nie sklejamy XML tekstem. Do pliku piszemy przez strumień bajtów, żeby kodowanie było pod kontrolą.
     */
    static void domWriting(Path dir) throws Exception {
        section("5. DOM — zmiany i zapis przez Transformer");
        Files.createDirectories(dir);

        Document doc = parse(CATALOG);
        Element root = doc.getDocumentElement();

        // Dodajemy książkę. Tytuł zawiera znaki specjalne celowo: < > & oraz cudzysłów.
        Element book = doc.createElement("ksiazka");   // create = utwórz
        book.setAttribute("id", "KS-006");
        book.setAttribute("rok", "2024");
        for (String[] field : new String[][] {{"tytul", "Tom & Jerry <wersja \"B\">"}, {"autor", "Ewa Lis"}, {"cena", "59.90"}}) {
            Element child = doc.createElement(field[0]);
            child.setTextContent(field[1]);            // setTextContent = ustaw tekst (ucieczka dopiero przy zapisie)
            book.appendChild(child);                   // appendChild = dołącz na końcu
        }
        root.appendChild(book);

        // Zmiana ceny istniejącej książki i usunięcie innej:
        for (Element e : childElements(root)) {
            if (e.getAttribute("id").equals("KS-002")) {
                e.getElementsByTagName("cena").item(0).setTextContent("119.00");
            }
            if (e.getAttribute("id").equals("KS-005")) {
                root.removeChild(e);                   // removeChild = usuń dziecko
            }
        }
        show("liczba książek po zmianach", doc.getElementsByTagName("ksiazka").getLength());
        // WYNIK: liczba książek po zmianach → 5

        // Zapis jednego węzła (nowej książki) — widać wcięcia i ucieczki:
        removeWhitespace(doc.getDocumentElement());
        String fragment = serialize(book, true); // true = bez deklaracji <?xml ...?>
        for (String line : fragment.split("\n")) {
            System.out.println("   | " + visible(line));
        }
        // WYNIK: | <ksiazka id="KS-006" rok="2024">
        // WYNIK: |   <tytul>Tom & amp; Jerry & lt;wersja "B"& gt;</tytul>
        // WYNIK: |   <autor>Ewa Lis</autor>
        // WYNIK: |   <cena>59.90</cena>
        // WYNIK: | </ksiazka>
        // Ucieczki zrobił serializator: ampersand, < i > zostały zapisane jako ciągi z ampersandem i średnikiem (amp, lt, gt);
        // cudzysłów w tekście (poza atrybutami) zostaje. W wydruku dodajemy spację po ampersandzie (metoda visible) —
        // tylko po to, by w komentarzach WYNIK ucieczki nie wyglądały jak prawdziwe ucieczki w źródle lekcji.

        // PUŁAPKA: (Java 9+) Transformer z INDENT dodaje wcięcia do każdego węzła, także do węzłów ze spacjami, które
        //   już były w dokumencie — wychodzą dziwne puste linie. Dlatego przed zapisem z wcięciami robimy removeWhitespace.
        Document untouched = parse(CATALOG);
        int ugly = serialize(untouched, true).split("\n").length;
        Document tidy = parse(CATALOG);
        removeWhitespace(tidy.getDocumentElement());
        int nice = serialize(tidy, true).split("\n").length;
        show("linie bez czyszczenia / po czyszczeniu", ugly + " / " + nice);
        // WYNIK: linie bez czyszczenia / po czyszczeniu → 53 / 27

        // Zapis do pliku: strumień bajtów + deklaracja encoding="UTF-8". StreamResult(OutputStream) pozwala nam
        // samym wybrać i zamknąć strumień (a StreamResult(File) ukrywa to przed nami).
        Path file = dir.resolve("katalog.xml");
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        try (OutputStream out = Files.newOutputStream(file)) {
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(doc), new StreamResult(out));
        }
        String written = Files.readString(file, StandardCharsets.UTF_8);
        show("plik zaczyna się od deklaracji UTF-8", written.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\""));
        // WYNIK: plik zaczyna się od deklaracji UTF-8 → true
        show("polskie litery zapisane", written.contains("Zażółć gęślą jaźń"));
        // WYNIK: polskie litery zapisane → true

        // Ponowny odczyt z pliku: parse(File) — kodowanie parser bierze z deklaracji w pliku.
        Document again = secureBuilder(false).parse(file.toFile());
        show("po odczycie z pliku: liczba książek", again.getElementsByTagName("ksiazka").getLength());
        // WYNIK: po odczycie z pliku: liczba książek → 5
        show("zmieniona cena KS-002", priceOf(again, "KS-002"));
        // WYNIK: zmieniona cena KS-002 → 119.00

        // Deklaracja z DOM-a ma domyślnie standalone="no". setXmlStandalone(true) usuwa ten atrybut, ale JDK skleja wtedy
        // deklarację z elementem głównym w jednej linii — nie wpływa to na poprawność, tylko na wygląd.
        // PUŁAPKA: znaki końca linii z Transformera zależą od systemu (Windows "\r\n"), dlatego serialize() je normalizuje.
        // PUŁAPKA: znak niedozwolony w XML 1.0 (np. kod sterujący 0x01) nie da się zapisać nawet jako encja — serializator
        //   zgłosi błąd albo go pominie. Dane z zewnątrz oczyszczaj przed wstawieniem do XML.
    }

    /**
     * visible = do wydruku: po każdym ampersandzie wstawia spację ("& amp;" zamiast ucieczki), żeby w komentarzach
     * WYNIK ucieczki były czytelne i nie myliły się z prawdziwymi ucieczkami w tekście źródłowym lekcji.
     */
    private static String visible(String xml) {
        return xml.replace("&", "& ");
    }

    /** priceOf = cena książki o danym identyfikatorze (przez XPath — patrz sekcja 8). */
    private static String priceOf(Document doc, String id) throws Exception {
        return XPathFactory.newInstance().newXPath()
                .evaluate("//ksiazka[@id='" + id + "']/cena", doc);
    }

    // =================================================================================================
    // 6. STAX — CZYTANIE
    // =================================================================================================

    /**
     * 6. StAX czyta XML jako ciąg zdarzeń (START_ELEMENT, CHARACTERS, END_ELEMENT...) — to Ty „ciągniesz” kolejne
     * zdarzenie (pull), pamiętasz tylko to, co potrzebne, więc zużycie pamięci nie zależy od rozmiaru pliku.
     * XMLStreamReader NIE jest AutoCloseable i jego close() nie zamyka strumienia pod spodem — zamykamy oba osobno.
     */
    static void staxReading(Path dir) throws Exception {
        section("6. StAX — czytanie strumieniowe (XMLStreamReader)");
        Files.createDirectories(dir);

        // Bezpieczna fabryka StAX: bez DOCTYPE i bez encji zewnętrznych.
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

        // Pierwsze zdarzenia — żeby zobaczyć, jak to wygląda (zdarzenia ze spacjami też są, jak węzły w DOM).
        XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(CATALOG));
        List<String> events = new ArrayList<>();
        try {
            while (reader.hasNext() && events.size() < 7) {
                int event = reader.next();             // next = przejdź do następnego zdarzenia
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT -> events.add("START " + reader.getLocalName());
                    case XMLStreamConstants.END_ELEMENT -> events.add("END " + reader.getLocalName());
                    case XMLStreamConstants.CHARACTERS -> events.add(reader.isWhiteSpace() ? "SPACJE" : "TEKST " + reader.getText());
                    default -> events.add("INNE " + event);
                }
            }
        } finally {
            reader.close();
        }
        show("pierwsze zdarzenia", events);
        // WYNIK: pierwsze zdarzenia → [START biblioteka, SPACJE, START ksiazka, SPACJE, START tytul, TEKST Czysty kod, END tytul]

        // Przykład użycia: liczymy książki po 2010 i sumujemy ceny, nie budując drzewa.
        // getAttributeValue(null, "rok") = wartość atrybutu (null = bez przestrzeni nazw); getElementText = tekst do końca elementu.
        List<String> titles = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int books = 0;
        boolean recent = false;
        reader = factory.createXMLStreamReader(new StringReader(CATALOG));
        try {
            while (reader.hasNext()) {
                if (reader.next() == XMLStreamConstants.START_ELEMENT) {
                    switch (reader.getLocalName()) {
                        case "ksiazka" -> {
                            books++;
                            recent = Integer.parseInt(reader.getAttributeValue(null, "rok")) > 2010;
                        }
                        case "tytul" -> {
                            String title = reader.getElementText();
                            if (recent) {
                                titles.add(title);
                            }
                        }
                        case "cena" -> total = total.add(new BigDecimal(reader.getElementText()));
                        default -> { }
                    }
                }
            }
        } finally {
            reader.close();
        }
        show("liczba książek", books);
        // WYNIK: liczba książek → 5
        show("tytuły po 2010", titles);
        // WYNIK: tytuły po 2010 → [Java. Podstawy, Wzorce projektowe, Zażółć gęślą jaźń]
        show("suma cen", total);
        // WYNIK: suma cen → 391.50

        // Z pliku: strumień bajtów + createXMLStreamReader(InputStream) → kodowanie z deklaracji. Zamykamy OBA.
        Path file = dir.resolve("katalog.xml");
        Files.writeString(file, CATALOG, StandardCharsets.UTF_8);
        int count = 0;
        try (InputStream in = Files.newInputStream(file)) {
            XMLStreamReader fileReader = factory.createXMLStreamReader(in);
            try {
                while (fileReader.hasNext()) {
                    if (fileReader.next() == XMLStreamConstants.START_ELEMENT && fileReader.getLocalName().equals("autor")) {
                        count++;
                    }
                }
            } finally {
                fileReader.close();
            }
        }
        show("elementów autor w pliku", count);
        // WYNIK: elementów autor w pliku → 5

        // PUŁAPKA: getText() w zdarzeniu CHARACTERS może przyjść w kilku kawałkach
        //   (przy długim tekście lub encjach parser ma prawo podzielić go na kilka zdarzeń). Dlatego używaj
        //   getElementText() albo włącz IS_COALESCING (sklejaj kawałki).
        // PUŁAPKA: StAX nie zna „rodzica”. Jeśli potrzebujesz kontekstu (np. rok książki przy jej tytule) — pamiętasz go sam
        //   w zmiennych (jak `recent` wyżej) albo używasz stosu elementów.
        // DOBRA PRAKTYKA: pliki rzędu setek MB — StAX (lub SAX); małe pliki konfiguracji — DOM lub XPath, bo prostsze.
        //   SAX (SAXParser + DefaultHandler) to starszy styl „push”: parser sam wywołuje Twoje metody startElement/characters.
    }

    // =================================================================================================
    // 7. STAX — ZAPIS
    // =================================================================================================

    /**
     * 7. XMLStreamWriter zapisuje XML element po elemencie, sam robi ucieczkę znaków specjalnych w tekście i atrybutach.
     * Nie dodaje wcięć ani nowych linii — to jeden długi wiersz (czytelność nie jest jego zadaniem).
     * Zapis do StringWriter pokazuje wynik w pamięci.
     */
    static void staxWriting() throws Exception {
        section("7. StAX — zapis (XMLStreamWriter)");

        StringWriter out = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        XMLStreamWriter writer = factory.createXMLStreamWriter(out);
        try {
            writer.writeStartDocument("UTF-8", "1.0");               // write = zapisz; start document = początek dokumentu
            writer.writeStartElement("biblioteka");                  // otwórz element
            writer.writeStartElement("ksiazka");
            writer.writeAttribute("id", "KS-010");                   // atrybut: ucieczka cudzysłowu i znaków specjalnych
            writer.writeAttribute("uwaga", "cytat \"A\" & <B>");
            writer.writeStartElement("tytul");
            writer.writeCharacters("Tom & Jerry <2>");               // tekst: ucieczka < i &
            writer.writeEndElement();                                // zamknij tytul
            writer.writeEmptyElement("dostepna");                    // element pusty: samozamykający się
            writer.writeEndElement();                                // zamknij ksiazka
            writer.writeEndElement();                                // zamknij biblioteka
            writer.writeEndDocument();                               // domknij wszystko, co zostało otwarte
            writer.flush();                                          // flush = wypchnij dane do strumienia
        } finally {
            writer.close();                                          // close NIE zamyka underlying (tu: StringWriter)
        }
        show("wynik w jednej linii", visible(out.toString()));
        // WYNIK: wynik w jednej linii → <?xml version="1.0" encoding="UTF-8"?><biblioteka><ksiazka id="KS-010" uwaga="cytat & quot;A& quot; & amp; & lt;B& gt;"><tytul>Tom & amp; Jerry & lt;2& gt;</tytul><dostepna/></ksiazka></biblioteka>

        // Można to wczytać z powrotem i sprawdzić, że dane przetrwały:
        Document back = parse(out.toString());
        show("tytuł po odczycie", back.getElementsByTagName("tytul").item(0).getTextContent());
        // WYNIK: tytuł po odczycie → Tom & Jerry <2>
        show("atrybut uwaga po odczycie", ((Element) back.getElementsByTagName("ksiazka").item(0)).getAttribute("uwaga"));
        // WYNIK: atrybut uwaga po odczycie → cytat "A" & <B>

        // Porównanie z DOM: DOM wymaga drzewa w pamięci, ale ma wcięcia (Transformer); StAX pisze od razu, bez drzewa
        // (miliony rekordów do pliku), ale wcięcia musisz dopisać sam (writeCharacters("\n  ")).
        // DOBRA PRAKTYKA: do pliku używaj createXMLStreamWriter(OutputStream, "UTF-8") — kodowanie jest wtedy jawne
        //   i zgodne z deklaracją; wersja z Writer polega na tym, jak Writer zakodował znaki.
        // PUŁAPKA: XMLStreamWriter nie waliduje nazw elementów ani nie pilnuje, czy zamknąłeś wszystko, jeśli pominiesz
        //   writeEndDocument — plik może być niepoprawny. Pamiętaj też o flush/close na końcu.
        // PUŁAPKA: nigdy nie sklejaj XML ręcznie ("<tytul>" + dane + "</tytul>") — pierwszy znak & lub < w danych
        //   psuje dokument, a spreparowane dane pozwalają wstrzyknąć własne elementy (XML injection).
    }

    // =================================================================================================
    // 8. XPATH
    // =================================================================================================

    /**
     * 8. XPath to język zapytań do drzewa XML: ścieżka z warunkami w nawiasach kwadratowych. JDK zna XPath 1.0.
     * evaluate(wyrażenie, węzeł, typ wyniku) zwraca: NODESET (lista węzłów), STRING, NUMBER (zawsze double) lub BOOLEAN.
     * Brak wyniku to pusta lista lub pusty tekst — nie NullPointerException jak w getElementsByTagName(...).item(0).
     */
    static void xpathQueries() throws Exception {
        section("8. XPath — zapytania do drzewa");

        Document doc = parse(CATALOG);
        XPathFactory factory = XPathFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        XPath xpath = factory.newXPath();

        // NODESET: książki po 2010 — warunek na atrybucie (@rok); tytuły przez drugi krok ścieżki.
        NodeList nodes = (NodeList) xpath.evaluate("/biblioteka/ksiazka[@rok>2010]/tytul", doc, XPathConstants.NODESET);
        show("tytuły książek po 2010", textsOf(nodes));
        // WYNIK: tytuły książek po 2010 → [Java. Podstawy, Wzorce projektowe, Zażółć gęślą jaźń]

        show("liczba książek (count)", xpath.evaluate("count(/biblioteka/ksiazka)", doc, XPathConstants.NUMBER));
        // WYNIK: liczba książek (count) → 5.0
        show("suma cen (sum) jako double", xpath.evaluate("sum(//cena)", doc, XPathConstants.NUMBER));
        // WYNIK: suma cen (sum) jako double → 391.5
        // PUŁAPKA: NUMBER to double — dla pieniędzy lepiej pobrać teksty (NODESET) i zsumować BigDecimal-ami (sekcja 3).

        // STRING (domyślny typ, gdy nie podasz trzeciego argumentu): tytuł książki o identyfikatorze KS-003.
        show("tytuł KS-003", xpath.evaluate("//ksiazka[@id='KS-003']/tytul", doc));
        // WYNIK: tytuł KS-003 → Wzorce projektowe
        show("tytuł KS-003 (text())", xpath.evaluate("//ksiazka[@id='KS-003']/tytul/text()", doc));
        // WYNIK: tytuł KS-003 (text()) → Wzorce projektowe
        show("ostatnia książka (last())", xpath.evaluate("/biblioteka/ksiazka[last()]/@id", doc));
        // WYNIK: ostatnia książka (last()) → KS-005
        show("druga książka (indeksy od 1!)", xpath.evaluate("/biblioteka/ksiazka[2]/@id", doc));
        // WYNIK: druga książka (indeksy od 1!) → KS-002
        show("autor zawiera 'Nowak' (contains)", textsOf((NodeList) xpath.evaluate(
                "//ksiazka[contains(autor,'Nowak')]/tytul", doc, XPathConstants.NODESET)));
        // WYNIK: autor zawiera 'Nowak' (contains) → [Czysty kod, Prawo & porządek]

        // BOOLEAN i brak wyniku:
        show("czy jest książka z 2021 (boolean)", xpath.evaluate("boolean(//ksiazka[@rok=2021])", doc, XPathConstants.BOOLEAN));
        // WYNIK: czy jest książka z 2021 (boolean) → true
        show("brak dopasowania jako STRING", "[" + xpath.evaluate("//ksiazka[@id='NIE-MA']/tytul", doc) + "]");
        // WYNIK: brak dopasowania jako STRING → []
        show("brak dopasowania jako NODESET (liczba)",
                ((NodeList) xpath.evaluate("//ksiazka[@id='NIE-MA']", doc, XPathConstants.NODESET)).getLength());
        // WYNIK: brak dopasowania jako NODESET (liczba) → 0

        // Ścieżka: "/" zaczyna od korzenia, "//" szuka na dowolnej głębokości, "@" atrybut, [..] warunek, "." bieżący węzeł,
        // ".." rodzic, "*" dowolny element. Do porównań tekstowych służą cudzysłowy pojedyncze wewnątrz wyrażenia.
        // PUŁAPKA: NodeList z XPath nie jest listą Javy — nie ma stream() ani for-each; przepisujemy ją w pętli (textsOf).
        // PUŁAPKA: wartość z danych użytkownika wklejona w wyrażenie XPath ("//ksiazka[@id='" + id + "']") to XPath injection —
        //   w programie z prawdziwymi danymi użyj XPathVariableResolver albo najpierw sprawdź id wyrażeniem regularnym.
        // XPath i przestrzenie nazw: dla dokumentów z xmlns trzeba podać NamespaceContext — patrz sekcja 9 (komentarz).
    }

    /** textsOf = teksty węzłów z listy NodeList (po trim). */
    private static List<String> textsOf(NodeList nodes) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            result.add(nodes.item(i).getTextContent().trim());
        }
        return result;
    }

    // =================================================================================================
    // 9. PRZESTRZENIE NAZW I INNE API
    // =================================================================================================

    /**
     * 9. Przestrzeń nazw (namespace) to „nazwisko rodowe” elementów, zapisywane atrybutem xmlns: pozwala
     * zmieszać w jednym dokumencie elementy z różnych słowników. Parser DOM domyślnie jej NIE rozpoznaje —
     * trzeba włączyć setNamespaceAware(true) i używać metod z końcówką NS.
     */
    static void namespacesAndOthers() throws Exception {
        section("9. Przestrzenie nazw (namespace) i inne API");

        String ns = "http://example.com/biblioteka";
        String xml = "<b:biblioteka xmlns:b=\"" + ns + "\"><b:ksiazka><b:tytul>Czysty kod</b:tytul></b:ksiazka></b:biblioteka>";

        // Parser bez rozpoznawania przestrzeni nazw: nazwa to cały tekst „b:ksiazka” z prefiksem.
        Document plain = secureBuilder(false).parse(new InputSource(new StringReader(xml)));
        show("bez NS: getElementsByTagName(\"ksiazka\")", plain.getElementsByTagName("ksiazka").getLength());
        // WYNIK: bez NS: getElementsByTagName("ksiazka") → 0
        show("bez NS: getElementsByTagName(\"b:ksiazka\")", plain.getElementsByTagName("b:ksiazka").getLength());
        // WYNIK: bez NS: getElementsByTagName("b:ksiazka") → 1
        show("bez NS: getLocalName()", plain.getDocumentElement().getLocalName());
        // WYNIK: bez NS: getLocalName() → null

        // Parser rozpoznający przestrzenie nazw: nazwa lokalna ("ksiazka") + adres przestrzeni; prefiks "b" to tylko skrót.
        Document aware = secureBuilder(true).parse(new InputSource(new StringReader(xml)));
        Element root = aware.getDocumentElement();
        show("z NS: getLocalName / getPrefix", root.getLocalName() + " / " + root.getPrefix());
        // WYNIK: z NS: getLocalName / getPrefix → biblioteka / b
        show("z NS: getNamespaceURI", root.getNamespaceURI());
        // WYNIK: z NS: getNamespaceURI → http://example.com/biblioteka
        show("z NS: getElementsByTagNameNS(ns, \"ksiazka\")", aware.getElementsByTagNameNS(ns, "ksiazka").getLength());
        // WYNIK: z NS: getElementsByTagNameNS(ns, "ksiazka") → 1
        show("z NS: ten sam element pod innym adresem", aware.getElementsByTagNameNS("http://inna", "ksiazka").getLength());
        // WYNIK: z NS: ten sam element pod innym adresem → 0

        // QName = nazwa kwalifikowana (adres przestrzeni + nazwa lokalna) — używają go m.in. XPath i StAX.
        QName name = new QName(ns, "ksiazka", "b");
        show("QName", name);
        // WYNIK: QName → {http://example.com/biblioteka}ksiazka

        // PUŁAPKA: ważna jest przestrzeń nazw, a NIE prefiks. Dwa dokumenty z innym prefiksem (b: i x:) i tym samym adresem
        //   opisują te same elementy. Element z xmlns="..." bez prefiksu też należy do przestrzeni nazw: getElementsByTagNameNS(ns, ...)
        //   go znajdzie, a getElementsByTagNameNS(null, ...) (szukanie elementów BEZ przestrzeni) już nie.
        // XPath w dokumencie z przestrzeniami nazw wymaga implementacji javax.xml.namespace.NamespaceContext (mapa
        // prefiks → adres) przekazanej do xpath.setNamespaceContext(...); bez niej "//b:ksiazka" zgłosi błąd.

        // Czego w JDK 17 NIE MA: JAXB (powiązanie klas Javy z XML przez adnotacje) i JAX-WS zostały usunięte z JDK w Javie 11
        // (JEP 320). Dziś: Jackson XML (jackson-dataformat-xml, to samo API co Jackson dla JSON) albo Jakarta XML Binding
        // (JAXB) jako osobna zależność Mavena. Spring Boot używa Jacksona także do XML. W JDK zostają: DOM, SAX, StAX,
        // XPath, Transformer, walidacja schematów (XSD) — i na tym kończy się ta lekcja.
        // Kiedy XML? Gdy narzuca go system zewnętrzny (SOAP, SAML, konfiguracje Mavena i Springa, pliki Office, SVG).
        // Dla nowych interfejsów między własnymi usługami zwykle wybiera się JSON (t18_io_files/Io05JsonManual).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • XML musi być poprawny (well-formed): jeden korzeń, domknięte znaczniki, cudzysłowy, ucieczki dla & i <.
     *   • Parser zawsze konfiguruj bezpiecznie: disallow-doctype-decl (DOM), SUPPORT_DTD=false (StAX),
     *     ACCESS_EXTERNAL_DTD="" (Transformer). Domyślny parser JDK czyta pliki z encji zewnętrznych (XXE).
     *   • DOM: całe drzewo; getElementsByTagName, getAttribute (brak atrybutu → ""), getTextContent. Wszystko to tekst.
     *   • Wcięcia w pliku to węzły tekstowe: getChildNodes() liczy je razem z elementami. Filtruj ELEMENT_NODE.
     *   • Zapis: DOM + Transformer (INDENT; przed zapisem removeWhitespace; \r\n → \n w testach) albo StAX XMLStreamWriter.
     *   • StAX: pull, mało pamięci, XMLStreamReader.close() nie zamyka strumienia pod spodem.
     *   • XPath 1.0: /a/b[@x>1]/c, count(), sum(); NUMBER = double; brak wyniku = pusta lista lub "".
     *   • Przestrzenie nazw: setNamespaceAware(true) i metody NS; liczy się adres, nie prefiks.
     *   • JAXB nie ma w JDK 17 (usunięty w Javie 11): Jackson XML albo Jakarta XML Binding jako zależność.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się dokument poprawnie sformułowany (well-formed) od zgodnego ze schematem (valid)?
     *   2. Na czym polega atak XXE i która opcja fabryki parsera mu zapobiega?
     *   3. Co wypisze:  doc.getDocumentElement().getChildNodes().getLength()  dla XML-a z 5 elementami wciętymi w osobnych liniach
     *      (każdy element w nowej linii, wcięty spacjami)?
     *   4. Co zwróci element.getAttribute("nie-ma") — null, wyjątek czy coś innego?
     *   5. ZNAJDŹ BŁĄD:  Element tytul = (Element) ksiazka.getFirstChild();  — dla XML z wcięciami.
     *   6. ZNAJDŹ BŁĄD:  String xml = "<tytul>" + tytulOdUzytkownika + "</tytul>";  — dlaczego to niebezpieczne?
     *   7. Co zwróci XPath count(//ksiazka) — int czy double? A wyrażenie //ksiazka[2]/@id — od którego indeksu liczy XPath?
     *   8. Kiedy wybierzesz StAX zamiast DOM? Dlaczego XMLStreamReader zamykasz osobno od strumienia?
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

    /** reference = czy sprawdzamy rozwiązania wzorcowe. Ćwiczenia pracują na tekstach XML (bez plików). */
    private static void runExercises(boolean reference) {
        Check.equal("ćw. 1: tytuły (DOM)", "[Czysty kod, Java. Podstawy, Wzorce projektowe, Zażółć gęślą jaźń, Prawo & porządek]",
                attempt(() -> (reference ? solution1(CATALOG) : exercise1(CATALOG)).toString()));

        Check.equal("ćw. 2: książki po roku (XPath)", "[Zażółć gęślą jaźń]",
                attempt(() -> (reference ? solution2(CATALOG, 2020) : exercise2(CATALOG, 2020)).toString()));

        String evil = "<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY e \"zły\">]><x>&e;</x>";
        Check.equal("ćw. 3: bezpieczne parsowanie", "biblioteka|ODRZUCONO",
                attempt(() -> (reference ? solution3(CATALOG) : exercise3(CATALOG)) + "|"
                        + (reference ? solution3(evil) : exercise3(evil))));

        Check.equal("ćw. 4: StAX — liczba i suma", "5|391.50",
                attempt(() -> reference ? solution4(CATALOG) : exercise4(CATALOG)));

        Check.equal("ćw. 5: dodanie książki (DOM + Transformer)",
                "[Czysty kod, Java. Podstawy, Wzorce projektowe, Zażółć gęślą jaźń, Prawo & porządek, Chleb & masło <1>]",
                attempt(() -> {
                    String changed = reference ? solution5(CATALOG, "KS-777", "Chleb & masło <1>")
                            : exercise5(CATALOG, "KS-777", "Chleb & masło <1>");
                    return solution1(changed).toString();   // czytamy wynik sprawdzoną wcześniej metodą
                }));
        Check.summary();
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć listę tytułów wszystkich książek (w kolejności dokumentu) przez DOM.
     * Podpowiedź: parse(xml), getElementsByTagName("tytul"), pętla po NodeList, getTextContent().trim().
     */
    static List<String> exercise1(String xml) throws Exception {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (łatwe): zwróć tytuły książek wydanych PO podanym roku, używając XPath.
     * Podpowiedź: wyrażenie "/biblioteka/ksiazka[@rok>" + rok + "]/tytul", typ XPathConstants.NODESET, potem textsOf.
     */
    static List<String> exercise2(String xml, int year) throws Exception {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ na bezpieczne parsowanie i zwróć nazwę elementu głównego albo "ODRZUCONO",
     * gdy dokument zawiera DOCTYPE.
     * <pre>{@code
     * // PRZED: domyślna fabryka — podatna na XXE
     * Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
     *         .parse(new InputSource(new StringReader(xml)));
     * return doc.getDocumentElement().getTagName();
     * }</pre>
     * Podpowiedź: secureBuilder(false) z tej lekcji; SAXParseException łap i zamień na "ODRZUCONO".
     */
    static String exercise3(String xml) throws Exception {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (średnie): przez StAX (bez budowania drzewa) policz książki i zsumuj ceny. Zwróć tekst "5|391.50"
     * (liczba|suma jako BigDecimal).
     * Podpowiedź: XMLInputFactory z SUPPORT_DTD=false, pętla hasNext/next, START_ELEMENT, getLocalName, getElementText.
     */
    static String exercise4(String xml) throws Exception {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): dodaj do katalogu nową książkę (id, tytuł; rok "2025", autor "Nieznany", cena "10.00")
     * i zwróć cały dokument jako tekst XML. Tytuł może zawierać znaki specjalne — masz NIE sklejać tekstu ręcznie.
     * Podpowiedź: DOM (createElement, setAttribute, setTextContent, appendChild), potem Transformer
     * (serialize z tej lekcji; wywołaj wcześniej removeWhitespace, żeby wcięcia były ładne).
     */
    static String exercise5(String xml, String id, String title) throws Exception {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solution1(String xml) throws Exception {
        return textsOf(parse(xml).getElementsByTagName("tytul"));
    }

    static List<String> solution2(String xml, int year) throws Exception {
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes = (NodeList) xpath.evaluate("/biblioteka/ksiazka[@rok>" + year + "]/tytul", parse(xml), XPathConstants.NODESET);
        return textsOf(nodes);
    }

    static String solution3(String xml) throws Exception {
        try {
            return parse(xml).getDocumentElement().getTagName();
        } catch (SAXParseException e) {
            return "ODRZUCONO";
        }
    }

    static String solution4(String xml) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(xml));
        int books = 0;
        BigDecimal total = BigDecimal.ZERO;
        try {
            while (reader.hasNext()) {
                if (reader.next() == XMLStreamConstants.START_ELEMENT) {
                    if (reader.getLocalName().equals("ksiazka")) {
                        books++;
                    } else if (reader.getLocalName().equals("cena")) {
                        total = total.add(new BigDecimal(reader.getElementText()));
                    }
                }
            }
        } finally {
            reader.close();
        }
        return books + "|" + total;
    }

    static String solution5(String xml, String id, String title) throws Exception {
        Document doc = parse(xml);
        Element book = doc.createElement("ksiazka");
        book.setAttribute("id", id);
        book.setAttribute("rok", "2025");
        String[][] fields = {{"tytul", title}, {"autor", "Nieznany"}, {"cena", "10.00"}};
        for (String[] field : fields) {
            Element child = doc.createElement(field[0]);
            child.setTextContent(field[1]);
            book.appendChild(child);
        }
        doc.getDocumentElement().appendChild(book);
        removeWhitespace(doc.getDocumentElement());
        return serialize(doc, false);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Poprawnie sformułowany: składnia jest dobra (jeden korzeń, domknięte znaczniki, cudzysłowy, ucieczki).
     *      Zgodny ze schematem (valid): dodatkowo spełnia reguły DTD/XSD (jakie elementy i atrybuty wolno). Pierwsze
     *      sprawdza każdy parser, drugie tylko na życzenie.
     *   2. XXE: DOCTYPE definiuje encję zewnętrzną (SYSTEM "file:///..." lub adres http), parser wstawia zawartość pliku/odpowiedzi
     *      do dokumentu, więc atakujący może wydobyć dane serwera. Zapobiega zakaz DOCTYPE:
     *      setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) (dla StAX: SUPPORT_DTD = false).
     *   3. 11: pięć elementów i sześć węzłów tekstowych ze spacjami (przed pierwszym, między i po ostatnim).
     *   4. Pusty tekst "" — nie null i nie wyjątek. Obecność atrybutu sprawdza hasAttribute.
     *   5. getFirstChild() to węzeł tekstowy ze spacjami (wcięcie), nie element: ClassCastException. Trzeba
     *      filtrować ELEMENT_NODE albo użyć getElementsByTagName("tytul").item(0).
     *   6. Dane z & lub < psują dokument (niepoprawny XML), a spreparowany tekst (np. zamykający znacznik plus nowe
     *      elementy) pozwala wstrzyknąć własną strukturę. Użyj DOM (setTextContent) albo XMLStreamWriter — robią ucieczkę same.
     *   7. double (5.0); XPath liczy indeksy od 1, więc [2] to druga książka.
     *   8. StAX: duże pliki albo jednorazowy przebieg, gdy nie chcesz trzymać całego drzewa w pamięci. XMLStreamReader.close()
     *      nie zamyka strumienia pod spodem (to inny obiekt), więc strumień zamykasz własnym try-with-resources.
     */
    // </editor-fold>
}
