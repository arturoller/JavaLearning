package t00_start;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Słowniczek angielsko-polski dla programisty Javy
 *        (glossary = słowniczek, glosariusz)
 *
 * W SKRÓCIE:
 *   Ok. 450 angielskich słów, które spotkasz w Javie: podstawy, słowa kluczowe, nazwy metod z bibliotek, pojęcia.
 *   Każde hasło: słowo angielskie → tłumaczenie → krótkie wyjaśnienie „po co to jest w Javie”.
 *   Na początku jest tabela TERMINOLOGII KURSU — tłumaczenia, których konsekwentnie używamy we wszystkich lekcjach.
 *
 * ANALOGIA:
 *   To kieszonkowy słownik turysty: nie musisz znać całego języka, wystarczy, że szybko sprawdzisz słowo,
 *   które właśnie widzisz. W Javie nazwy są „mówiące” — gdy rozumiesz słowo, rozumiesz metodę.
 *
 * JAK TO DZIAŁA:
 *   • Uruchom main() — wypisze cały słowniczek pogrupowany.
 *   • Chcesz tylko jedno słowo? Wpisz je w stałą SEARCH poniżej (np. "map") i uruchom — zobaczysz tylko pasujące hasła.
 *   • Albo szukaj w tym pliku przez Ctrl+F.
 *
 * DOBRA PRAKTYKA: naucz się przedrostków w nazwach metod — rozszyfrujesz dzięki nim większość API:
 *     get... = pobierz; set... = ustaw; is.../has.../can... = czy jest / czy ma / czy może (zwracają boolean);
 *     to... = zamień na (toString, toList); as... = potraktuj jako (asList); of/from = utwórz z (List.of, Instant.from);
 *     find... = znajdź (może nie znaleźć → Optional); compute... = oblicz; with... = z (nową wartością).
 *
 * SŁÓWKA:
 *   search = szukaj; group = grupa; entry = wpis, hasło; english = angielski; polish = polski;
 *   explanation = wyjaśnienie; width = szerokość.
 *
 * ZOBACZ TEŻ: Start01HowToUse (jak korzystać z kursu), t01_basics (podstawy języka).
 * </pre>
 */
public class Start02Glossary {

    /**
     * SEARCH = szukaj. Wpisz fragment słowa (np. "stream", "lock", "orElse"), żeby wypisać tylko pasujące hasła.
     * Pusty napis "" = wypisz wszystko. Wielkość liter nie ma znaczenia.
     */
    private static final String SEARCH = "";

    /**
     * Entry = hasło słowniczka. To RECORD (rekord) — zwięzła, niezmienna klasa na dane (t09_records).
     * english = słowo angielskie; polish = tłumaczenie; explanation = wyjaśnienie.
     */
    record Entry(String english, String polish, String explanation) {
    }

    /** e = skrót od „entry”. Krótka metoda tworząca hasło — żeby listy niżej były czytelne. */
    private static Entry e(String english, String polish, String explanation) {
        return new Entry(english, polish, explanation);
    }

    public static void main(String[] args) {
        title("Start02 — słowniczek angielsko-polski");

        // LinkedHashMap = mapa, która pamięta kolejność wstawiania — grupy wypiszą się w tej kolejności.
        // Klucz = nazwa grupy, wartość = lista haseł.
        Map<String, List<Entry>> groups = new LinkedHashMap<>();
        groups.put("0. Terminologia kursu — tłumaczenia używane we wszystkich lekcjach", COURSE_TERMS);
        groups.put("1. Podstawy programowania", BASICS);
        groups.put("2. Słowa kluczowe Javy (keywords)", KEYWORDS);
        groups.put("3. Literały i słowa kontekstowe", LITERALS_AND_CONTEXTUAL);
        groups.put("4. Programowanie obiektowe i pojęcia ogólne", OOP_AND_GENERAL);
        groups.put("5. Kolekcje (collections)", COLLECTIONS);
        groups.put("6. Streamy (Stream API)", STREAMS);
        groups.put("7. Lambdy i interfejsy funkcyjne", LAMBDAS);
        groups.put("8. Optional", OPTIONAL);
        groups.put("9. Wyjątki (exceptions)", EXCEPTIONS);
        groups.put("10. Napisy i znaki (strings)", STRINGS);
        groups.put("11. Liczby i matematyka (numbers)", NUMBERS);
        groups.put("12. Data i czas (date/time)", DATE_TIME);
        groups.put("13. Pliki i wejście/wyjście (IO)", IO);
        groups.put("14. Wątki i współbieżność (concurrency)", CONCURRENCY);
        groups.put("15. Adnotacje i refleksja", ANNOTATIONS);
        groups.put("16. Narzędzia, testy i praca zespołowa", TOOLS);

        // Szerokość kolumn liczona z NAJDŁUŻSZEGO hasła — dzięki temu kolumny zawsze są równe.
        int englishWidth = 0;
        int polishWidth = 0;
        for (List<Entry> entries : groups.values()) {
            for (Entry entry : entries) {
                englishWidth = Math.max(englishWidth, entry.english().length());
                polishWidth = Math.max(polishWidth, entry.polish().length());
            }
        }
        // Szablon formatu, np. "%-34s %-38s %s%n":  %-34s = napis wyrównany do lewej na 34 znakach; %n = nowa linia
        String pattern = "%-" + englishWidth + "s  %-" + polishWidth + "s  %s%n";

        int total = 0;
        for (Map.Entry<String, List<Entry>> group : groups.entrySet()) {
            total += printGroup(group.getKey(), group.getValue(), pattern);
        }
        line();
        show("Liczba wypisanych haseł", total);
    }

    /**
     * printGroup = wypisz grupę. Zwraca, ile haseł wypisała (przydaje się przy wyszukiwaniu).
     * <p>
     * printf = print formatted = wypisz sformatowane (t04_strings/Strings04Formatting).
     * toLowerCase(Locale.ROOT) = zamień na małe litery według reguł neutralnych językowo (Locale.ROOT).
     * Bez podania Locale wynik zależy od języka systemu (np. w tureckim „I” zmienia się w „ı”) — stąd ROOT.
     */
    private static int printGroup(String name, List<Entry> entries, String pattern) {
        int printed = 0;
        boolean headerShown = false;
        String query = SEARCH.toLowerCase(Locale.ROOT);
        for (Entry entry : entries) {
            boolean matches = query.isEmpty()
                    || entry.english().toLowerCase(Locale.ROOT).contains(query)     // contains = zawiera
                    || entry.polish().toLowerCase(Locale.ROOT).contains(query);
            if (!matches) {
                continue;                                  // continue = przejdź do następnego obrotu pętli
            }
            if (!headerShown) {
                section(name);
                headerShown = true;
            }
            System.out.printf(pattern, entry.english(), entry.polish(), entry.explanation());
            printed++;
        }
        return printed;
    }

    // =====================================================================================================
    // 0. TERMINOLOGIA KURSU — jedno tłumaczenie na jedno pojęcie (konsekwentnie w całym kursie)
    // =====================================================================================================
    private static final List<Entry> COURSE_TERMS = List.of(
            e("stream (Stream API)", "stream / strumień", "potok przetwarzania danych: list.stream().filter(...) — NIE MYLIĆ z IO"),
            e("stream (IO)", "strumień wejścia/wyjścia", "InputStream, Reader... — czytanie/zapis bajtów lub znaków (pliki, sieć)"),
            e("collection", "kolekcja", "ogólna nazwa: lista, zbiór, kolejka..."),
            e("set", "zbiór", "kolekcja bez duplikatów (Set)"),
            e("map (struktura)", "mapa", "pary klucz → wartość (Map)"),
            e("map (operacja)", "przekształć (mapuj)", "stream.map(...) — zamień każdy element na inny"),
            e("side effect", "efekt uboczny", "zmiana czegoś poza wynikiem funkcji (pole, lista, plik, konsola)"),
            e("iterate", "iterować", "przechodzić po kolei po elementach (albo liczyć kolejne wartości)"),
            e("consumed", "zużyty", "stream po operacji końcowej — nie da się go użyć ponownie"),
            e("close", "zamknij", "zwolnij zasób (plik, połączenie); zużyty stream ≠ zamknięty zasób"),
            e("unmodifiable / immutable", "niemodyfikowalny / niezmienny", "nie da się zmienić przez ten obiekt / w ogóle"),
            e("reference (to a method)", "referencja do metody", "String::length — skrót lambdy")
    );

    // =====================================================================================================
    // 1. PODSTAWY PROGRAMOWANIA
    // =====================================================================================================
    private static final List<Entry> BASICS = List.of(
            e("program / code", "program / kod", "instrukcje dla komputera"),
            e("source code", "kod źródłowy", "to, co piszesz w plikach .java"),
            e("variable", "zmienna", "nazwane miejsce na wartość: int age = 30;"),
            e("value", "wartość", "to, co przechowuje zmienna (np. 30)"),
            e("type", "typ", "rodzaj wartości: int, String, Product..."),
            e("declare / declaration", "zadeklarować / deklaracja", "ogłoszenie zmiennej lub metody: int x;"),
            e("assign / assignment", "przypisać / przypisanie", "nadanie wartości: x = 5; (znak = to przypisanie, nie porównanie!)"),
            e("initialize", "zainicjalizować", "nadać wartość początkową: int x = 0;"),
            e("constant", "stała", "wartość, której nie zmieniasz: static final int MAX = 10;"),
            e("literal", "literał", "wartość wpisana wprost w kodzie: 42, 3.14, \"tekst\", 'a', true"),
            e("expression", "wyrażenie", "fragment kodu, który daje wartość: a + b, x > 5"),
            e("statement", "instrukcja", "polecenie zakończone średnikiem: x = 5;"),
            e("block", "blok", "instrukcje w nawiasach klamrowych { ... }"),
            e("condition", "warunek", "wyrażenie typu boolean: age >= 18"),
            e("loop", "pętla", "powtarzanie kodu: for, while, do-while"),
            e("iteration", "iteracja, obrót (pętli)", "jedno wykonanie wnętrza pętli"),
            e("array", "tablica", "stała liczba elementów tego samego typu: int[] t = new int[5];"),
            e("operator / operand", "operator / argument operatora", "w a + b: + to operator, a i b to operandy"),
            e("increment / decrement", "zwiększ o 1 / zmniejsz o 1", "i++ / i--"),
            e("ternary operator", "operator trójargumentowy", "warunek ? gdyTak : gdyNie"),
            e("comment", "komentarz", "// jedna linia, /* kilka linii */, /** dokumentacja */"),
            e("call / invoke", "wywołać", "uruchomić metodę: print(\"x\")"),
            e("recursion", "rekurencja", "metoda wywołuje samą siebie"),
            e("varargs (variable arguments)", "zmienna liczba argumentów", "int... numbers — dowolnie wiele argumentów"),
            e("print / println / printf", "wypisz / wypisz linię / wypisz sformatowane", "metody System.out"),
            e("System.out / System.in", "wyjście / wejście systemowe", "konsola: wypisywanie / wczytywanie"),
            e("Scanner", "skaner, czytnik", "klasa do wczytywania danych (np. z klawiatury)"),
            e("input / output", "wejście / wyjście", "dane wchodzące do programu / wychodzące z niego"),
            e("equals", "równa się", "porównanie ZAWARTOŚCI obiektów (== porównuje referencje!)"),
            e("toString", "na napis", "tekstowa postać obiektu (używana przy wypisywaniu)"),
            e("hashCode", "kod skrótu", "liczba wyliczona z obiektu — dla HashMap/HashSet"),
            e("generate", "generuj, wytwarzaj", "tworzyć kolejne wartości (np. Stream.generate)"),
            e("main", "główny", "public static void main(String[] args) — punkt startu programu")
    );

    // =====================================================================================================
    // 2. SŁOWA KLUCZOWE — zarezerwowane; NIE można ich użyć jako nazw zmiennych, metod ani klas.
    // =====================================================================================================
    private static final List<Entry> KEYWORDS = List.of(
            e("abstract", "abstrakcyjny", "klasa/metoda niepełna — do uzupełnienia w podklasie; nie da się zrobić new"),
            e("assert", "sprawdź, zapewnij", "sprawdzenie warunku w czasie działania (włączane flagą -ea)"),
            e("boolean", "logiczny", "typ prosty: true (prawda) albo false (fałsz)"),
            e("break", "przerwij", "natychmiast wychodzi z pętli lub switcha"),
            e("byte", "bajt", "typ prosty: liczba całkowita 8 bitów (-128..127)"),
            e("case", "przypadek", "jedna gałąź w switch"),
            e("catch", "złap", "blok obsługi wyjątku po try"),
            e("char", "znak (character)", "typ prosty: jedna jednostka UTF-16, np. 'A' (emoji zajmują dwie!)"),
            e("class", "klasa", "szablon (przepis) na obiekty"),
            e("const", "stała", "zarezerwowane, NIEużywane — stałe robi się przez static final"),
            e("continue", "kontynuuj", "pomija resztę obrotu pętli i przechodzi do następnego"),
            e("default", "domyślny", "gałąź domyślna w switch; metoda z ciałem w interfejsie"),
            e("do", "wykonaj", "pętla do { } while (warunek) — wykona się co najmniej raz"),
            e("double", "podwójny (precyzji)", "typ prosty: liczba zmiennoprzecinkowa 64 bity"),
            e("else", "w przeciwnym razie", "gałąź wykonywana, gdy warunek if jest fałszywy"),
            e("enum", "wyliczenie", "typ z zamkniętą listą stałych, np. dni tygodnia"),
            e("extends", "rozszerza", "dziedziczenie klas; ograniczenie typu w generykach"),
            e("final", "ostateczny, końcowy", "zmienna: nie przypiszesz ponownie (ale obiekt, na który wskazuje, może się zmieniać!); metoda: nie nadpiszesz; klasa: nie odziedziczysz"),
            e("finally", "na koniec", "blok wykonywany zawsze po try/catch (sprzątanie)"),
            e("float", "zmiennoprzecinkowy", "typ prosty: liczba zmiennoprzecinkowa 32 bity (literał 1.5f)"),
            e("for", "dla", "pętla z licznikiem albo for-each (dla każdego elementu)"),
            e("goto", "idź do", "zarezerwowane, NIEużywane"),
            e("if", "jeżeli", "instrukcja warunkowa"),
            e("implements", "implementuje", "klasa realizuje interfejs"),
            e("import", "importuj", "udostępnia klasę z innego pakietu bez pełnej nazwy"),
            e("instanceof", "jest instancją", "sprawdza typ obiektu w czasie działania"),
            e("int", "liczba całkowita (integer)", "typ prosty: 32 bity, ok. ±2,1 miliarda"),
            e("interface", "interfejs", "kontrakt: jakie metody ma mieć klasa"),
            e("long", "długi", "typ prosty: liczba całkowita 64 bity (literał 10L)"),
            e("native", "natywny", "metoda napisana w innym języku (C/C++) — rzadkie"),
            e("new", "nowy", "tworzy nowy obiekt (wywołuje konstruktor)"),
            e("package", "pakiet", "grupa klas (folder); pierwsza linia pliku"),
            e("private", "prywatny", "widoczny tylko w tej samej klasie"),
            e("protected", "chroniony", "widoczny w pakiecie i w podklasach"),
            e("public", "publiczny", "widoczny wszędzie"),
            e("return", "zwróć", "kończy metodę i oddaje wynik"),
            e("short", "krótki", "typ prosty: liczba całkowita 16 bitów"),
            e("static", "statyczny", "należy do klasy, a nie do obiektu (wspólny dla wszystkich)"),
            e("strictfp", "ścisłe zmiennoprzecinkowe", "przestarzałe — od Javy 17 zawsze włączone"),
            e("super", "nad-, nadrzędny", "odwołanie do klasy bazowej: super(...), super.metoda()"),
            e("switch", "przełącznik", "wybór jednej z wielu gałęzi"),
            e("synchronized", "zsynchronizowany", "tylko jeden wątek naraz wykona ten kod"),
            e("this", "ten, to", "bieżący obiekt; this(...) wywołuje inny konstruktor"),
            e("throw", "rzuć", "rzuca wyjątek: throw new X(...)"),
            e("throws", "rzuca", "deklaracja w nagłówku metody: może rzucić ten wyjątek"),
            e("transient", "przejściowy", "pole pomijane przy serializacji"),
            e("try", "spróbuj", "blok kodu, w którym łapiemy wyjątki"),
            e("void", "pustka, nic", "metoda niczego nie zwraca"),
            e("volatile", "ulotny", "zmiany pola od razu widoczne dla innych wątków"),
            e("while", "dopóki", "pętla działająca, dopóki warunek jest prawdziwy"),
            e("_ (podkreślnik)", "podkreślnik", "słowo kluczowe od Javy 9 — nie można go użyć jako nazwy zmiennej")
    );

    // =====================================================================================================
    // 3. LITERAŁY I SŁOWA KONTEKSTOWE
    //    Literały true/false/null to WARTOŚCI (nie słowa kluczowe), ale też nie mogą być nazwami.
    //    Słowa kontekstowe mają specjalne znaczenie tylko w określonym miejscu — gdzie indziej są zwykłymi nazwami.
    //    Np. int record = 5;  SKOMPILUJE SIĘ — ale nie rób tego, bo kod staje się mylący.
    // =====================================================================================================
    private static final List<Entry> LITERALS_AND_CONTEXTUAL = List.of(
            e("true / false", "prawda / fałsz", "literały logiczne (wartości typu boolean)"),
            e("null", "nic, brak", "literał: referencja, która nie wskazuje na żaden obiekt"),
            e("var", "zmienna (variable)", "kontekstowe (Java 10+): typ zmiennej lokalnej wydedukuje kompilator"),
            e("record", "rekord, zapis", "kontekstowe (Java 16+): zwięzła niezmienna klasa na dane"),
            e("sealed", "zapieczętowany", "kontekstowe (Java 17): klasa/interfejs z zamkniętą listą podklas"),
            e("non-sealed", "niezapieczętowany", "kontekstowe (Java 17): podklasa klasy sealed, po której znowu można dziedziczyć"),
            e("permits", "zezwala", "kontekstowe (Java 17): lista klas, które mogą dziedziczyć po klasie sealed"),
            e("yield", "oddaj, wydaj", "kontekstowe (Java 14+): zwraca wartość z bloku w wyrażeniu switch")
    );

    // =====================================================================================================
    // 4. PROGRAMOWANIE OBIEKTOWE I POJĘCIA OGÓLNE
    // =====================================================================================================
    private static final List<Entry> OOP_AND_GENERAL = List.of(
            e("object", "obiekt", "konkretny egzemplarz klasy (ma stan i zachowanie)"),
            e("instance", "instancja, egzemplarz", "to samo co obiekt: new Dog() tworzy instancję klasy Dog"),
            e("field", "pole", "zmienna należąca do obiektu lub klasy"),
            e("method", "metoda", "funkcja należąca do klasy"),
            e("constructor", "konstruktor", "specjalny blok kodu (to NIE metoda — nie ma typu zwracanego) wywoływany przy new"),
            e("parameter", "parametr", "zmienna w deklaracji metody: void f(int x)"),
            e("argument", "argument", "konkretna wartość przekazana przy wywołaniu: f(5)"),
            e("return type", "typ zwracany", "typ wyniku metody (void = brak)"),
            e("signature", "sygnatura", "nazwa metody + typy parametrów (bez typu zwracanego)"),
            e("override", "nadpisać", "podklasa podmienia działanie metody z klasy bazowej (@Override)"),
            e("overload", "przeciążyć", "kilka metod o tej samej nazwie, ale innych parametrach"),
            e("inheritance", "dziedziczenie", "klasa przejmuje pola i metody innej (extends)"),
            e("superclass / base class", "nadklasa / klasa bazowa", "klasa, po której się dziedziczy"),
            e("subclass", "podklasa", "klasa, która dziedziczy"),
            e("abstraction", "abstrakcja", "ukrycie szczegółów, pokazanie tylko tego, co ważne"),
            e("encapsulation", "hermetyzacja", "ukrycie danych (private) i dostęp tylko przez metody"),
            e("polymorphism", "polimorfizm", "ta sama metoda działa różnie zależnie od faktycznego typu obiektu"),
            e("composition", "kompozycja", "obiekt składa się z innych obiektów („ma”, has-a)"),
            e("is-a / has-a", "jest / ma", "dziedziczenie („pies JEST zwierzęciem”) vs kompozycja („auto MA silnik”)"),
            e("immutable", "niezmienny", "stanu obiektu nie da się zmienić po jego utworzeniu"),
            e("mutable", "zmienny", "stan obiektu można zmieniać (np. przez settery)"),
            e("getter", "getter (akcesor odczytu)", "metoda zwracająca wartość pola: getName()"),
            e("setter", "setter (akcesor zapisu)", "metoda ustawiająca wartość pola: setName(...)"),
            e("access modifier", "modyfikator dostępu", "public / protected / (brak) / private"),
            e("package-private", "prywatny w pakiecie", "brak modyfikatora = widoczne tylko w tym samym pakiecie"),
            e("scope", "zasięg", "fragment kodu, w którym zmienna istnieje"),
            e("reference", "referencja, odniesienie", "„adres” obiektu przechowywany w zmiennej"),
            e("primitive", "typ prosty (prymitywny)", "int, double, boolean... — przechowują wartość, nie referencję"),
            e("wrapper", "klasa opakowująca", "obiektowa wersja typu prostego: Integer, Double, Boolean"),
            e("boxing / unboxing", "pakowanie / rozpakowanie", "automatyczna zamiana int ↔ Integer"),
            e("cast / casting", "rzutować / rzutowanie", "zmiana typu: (int) 3.7, (Dog) animal"),
            e("generic", "generyczny, ogólny", "klasa/metoda z parametrem typu: List<String>"),
            e("type parameter", "parametr typu", "„zmienna” oznaczająca typ: T w Box<T>"),
            e("bound", "ograniczenie", "<T extends Number> — T musi być liczbą"),
            e("wildcard", "dżoker, symbol wieloznaczny", "? w generykach: List<?>"),
            e("nested class", "klasa zagnieżdżona", "klasa zdefiniowana wewnątrz innej klasy"),
            e("inner class", "klasa wewnętrzna", "niestatyczna klasa zagnieżdżona (zna obiekt zewnętrzny)"),
            e("anonymous class", "klasa anonimowa", "klasa bez nazwy tworzona w miejscu użycia"),
            e("utility class", "klasa narzędziowa", "tylko metody static, prywatny konstruktor (np. Math)"),
            e("factory method", "metoda fabrykująca", "statyczna metoda tworząca obiekt: List.of(...)"),
            e("builder", "budowniczy", "obiekt pomagający krok po kroku zbudować inny obiekt"),
            e("singleton", "singleton (jedynak)", "klasa, która ma tylko jeden obiekt w programie"),
            e("strategy", "strategia", "wymienny algorytm przekazany jako obiekt"),
            e("dependency", "zależność", "obiekt, którego inna klasa potrzebuje do działania"),
            e("injection", "wstrzykiwanie", "podawanie zależności z zewnątrz (np. przez konstruktor)"),
            e("contract", "kontrakt, umowa", "zasady, których musi przestrzegać implementacja"),
            e("invariant", "niezmiennik", "warunek zawsze prawdziwy dla poprawnego obiektu"),
            e("validation", "walidacja, sprawdzenie", "sprawdzenie poprawności danych"),
            e("deprecated", "przestarzały", "nie używaj — może zostać usunięte albo jest lepsza wersja"),
            e("legacy", "dziedzictwo, stary kod", "starsze rozwiązania utrzymywane dla zgodności"),
            e("refactoring", "refaktoryzacja", "poprawa struktury kodu bez zmiany działania"),
            e("boilerplate", "kod szablonowy", "powtarzalny, nudny kod (np. gettery) — Lombok/record go usuwają"),
            e("runtime", "czas działania", "moment, gdy program już działa"),
            e("compile time", "czas kompilacji", "moment, gdy javac sprawdza i tłumaczy kod"),
            e("compiler", "kompilator", "program zamieniający .java na .class (javac)"),
            e("bytecode", "kod bajtowy", "zawartość plików .class — rozumie go JVM"),
            e("value object", "obiekt wartości", "obiekt porównywany po wartości (np. kwota, adres)"),
            e("entity", "encja", "obiekt z tożsamością (id), np. klient o numerze 7"),
            e("DTO (data transfer object)", "obiekt transferu danych", "prosty „pojemnik” na dane między warstwami"),
            e("null object", "obiekt pusty", "obiekt „nic nie rób” zamiast null (np. rabat, który zawsze daje 0 zł)"),
            e("side effect", "efekt uboczny", "metoda zmienia coś poza swoim wynikiem (pole, plik, konsolę)"),
            e("pure function", "czysta funkcja", "wynik zależy tylko od argumentów, brak efektów ubocznych")
    );

    // =====================================================================================================
    // 5. KOLEKCJE
    // =====================================================================================================
    private static final List<Entry> COLLECTIONS = List.of(
            e("collection", "kolekcja", "obiekt przechowujący wiele elementów (lista, zbiór, kolejka...)"),
            e("list", "lista", "uporządkowana, z indeksami, pozwala na duplikaty"),
            e("set", "zbiór", "bez duplikatów"),
            e("map", "mapa, słownik", "pary klucz → wartość"),
            e("queue", "kolejka", "FIFO: pierwszy wszedł, pierwszy wyjdzie"),
            e("deque (double-ended queue)", "kolejka dwustronna", "dodawanie/usuwanie z obu końców"),
            e("stack", "stos", "LIFO: ostatni wszedł, pierwszy wyjdzie"),
            e("priority queue", "kolejka priorytetowa", "wyjmuje najpierw element NAJMNIEJSZY wg porządku (compareTo/Comparator)"),
            e("key", "klucz", "po nim szukamy wartości w mapie"),
            e("value", "wartość", "to, co przechowujemy pod kluczem"),
            e("entry", "wpis", "para klucz + wartość (Map.Entry)"),
            e("element", "element", "pojedynczy obiekt w kolekcji"),
            e("index", "indeks", "numer pozycji w liście/tablicy (od 0!)"),
            e("size", "rozmiar", "liczba elementów: size()"),
            e("capacity", "pojemność", "ile miejsca zarezerwowano w środku (np. w ArrayList)"),
            e("add", "dodaj", "dodaje element"),
            e("remove", "usuń", "usuwa element (po indeksie albo po wartości!)"),
            e("removeIf", "usuń, jeśli", "usuwa elementy spełniające warunek"),
            e("contains", "zawiera", "czy kolekcja ma taki element"),
            e("containsKey", "zawiera klucz", "czy mapa ma taki klucz"),
            e("get", "pobierz", "pobiera element (po indeksie / kluczu)"),
            e("put", "włóż", "wstawia parę do mapy (nadpisuje starą wartość)"),
            e("getOrDefault", "pobierz albo domyślną", "wartość z mapy albo podana domyślna, gdy brak klucza"),
            e("putIfAbsent", "włóż, jeśli brak", "wstawia tylko wtedy, gdy klucza jeszcze nie ma"),
            e("computeIfAbsent", "oblicz, jeśli brak", "gdy brak klucza — tworzy wartość funkcją i wstawia"),
            e("merge", "scal, połącz", "łączy starą i nową wartość funkcją (np. licznik: Integer::sum)"),
            e("keySet", "zbiór kluczy", "wszystkie klucze mapy"),
            e("values", "wartości", "wszystkie wartości mapy"),
            e("entrySet", "zbiór wpisów", "wszystkie pary klucz-wartość"),
            e("iterator", "iterator", "obiekt do przechodzenia po kolekcji element po elemencie"),
            e("hasNext / next", "czy jest następny / następny", "metody iteratora"),
            e("sort", "sortuj", "układa elementy w kolejności"),
            e("comparable", "porównywalny", "obiekt zna swój „naturalny” porządek (compareTo)"),
            e("comparator", "komparator", "osobny obiekt określający kolejność"),
            e("compare / compareTo", "porównaj / porównaj z", "zwraca liczbę <0, 0 lub >0"),
            e("natural order", "porządek naturalny", "liczby rosnąco; napisy wg kodów znaków Unicode: „Z” < „a”, „Ł” po „z” — to NIE polski alfabet (do tego Collator)"),
            e("reverse / reversed", "odwróć / odwrócony", "odwrotna kolejność"),
            e("thenComparing", "następnie porównując", "drugie kryterium sortowania przy remisie"),
            e("unmodifiable", "niemodyfikowalny", "nie można dodawać/usuwać (widok albo kopia)"),
            e("copyOf", "kopia z", "tworzy niemodyfikowalną kopię: List.copyOf(lista)"),
            e("hash / hashCode", "skrót / kod skrótu", "liczba wyliczona z obiektu, używana przez HashMap/HashSet"),
            e("bucket", "kubełek", "„szufladka” w HashMap na elementy trafiające do tego samego miejsca"),
            e("load factor", "współczynnik zapełnienia", "przy jakim zapełnieniu HashMap się powiększa"),
            e("offer / poll / peek", "dodaj / wyjmij / podejrzyj", "kolejka: dodaj / wyjmij pierwszy / zobacz bez wyjmowania"),
            e("push / pop", "wepchnij / zdejmij", "operacje stosu"),
            e("first / last", "pierwszy / ostatni", "skrajne elementy (TreeSet, Deque)"),
            e("head / tail", "głowa / ogon", "początek / koniec; headMap = część mapy przed kluczem"),
            e("floor / ceiling", "podłoga / sufit", "najbliższy element ≤ / ≥ podanego (TreeMap, TreeSet)"),
            e("frequency", "częstość", "ile razy element występuje"),
            e("shuffle", "przetasuj", "losowa kolejność"),
            e("linked", "połączony", "LinkedList/LinkedHashMap — elementy połączone w łańcuch (pamięta kolejność)"),
            e("tree", "drzewo", "TreeMap/TreeSet — posortowane automatycznie"),
            e("hash map / hash set", "mapa / zbiór haszujący", "najszybsze, bez gwarancji kolejności"),
            e("concurrent modification", "równoczesna modyfikacja", "zmiana kolekcji w trakcie przechodzenia po niej (wyjątek!)")
    );

    // =====================================================================================================
    // 6. STREAMY (Stream API)
    // =====================================================================================================
    private static final List<Entry> STREAMS = List.of(
            e("stream", "stream, strumień", "sekwencja elementów przetwarzana krok po kroku (nie przechowuje danych!); NIE mylić ze strumieniami IO"),
            e("pipeline", "potok", "łańcuch operacji: źródło → operacje pośrednie → operacja końcowa"),
            e("source", "źródło", "skąd biorą się elementy (lista, tablica, plik...)"),
            e("intermediate operation", "operacja pośrednia", "zwraca nowy stream (filter, map...) — leniwa"),
            e("terminal operation", "operacja końcowa", "uruchamia potok i daje wynik (collect, count...)"),
            e("lazy / laziness", "leniwy / leniwość", "nic się nie dzieje, dopóki nie ma operacji końcowej"),
            e("eager", "gorliwy", "wykonywany od razu (przeciwieństwo leniwego)"),
            e("stateless / stateful", "bezstanowy / stanowy", "filter, map nie pamiętają innych elementów; sorted, distinct — pamiętają"),
            e("filter", "filtruj", "zostawia tylko elementy spełniające warunek"),
            e("map", "przekształć (mapuj)", "zamienia każdy element na inny (1 → 1)"),
            e("flatMap", "przekształć i spłaszcz", "zamienia element na strumień i skleja wszystkie w jeden (1 → 0..wiele)"),
            e("mapToInt / mapToObj", "przekształć na int / na obiekt", "przejście między Stream<T> a IntStream"),
            e("boxed", "opakowany", "IntStream → Stream<Integer>"),
            e("distinct", "odrębny, unikalny", "usuwa duplikaty (według equals)"),
            e("sorted", "posortowany", "sortuje elementy (musi najpierw zebrać WSZYSTKIE)"),
            e("limit", "ogranicz", "bierze tylko pierwsze n elementów"),
            e("skip", "pomiń", "pomija pierwsze n elementów"),
            e("peek", "podejrzyj", "podgląd elementów w trakcie (do debugowania)"),
            e("takeWhile / dropWhile", "bierz, dopóki / porzucaj, dopóki", "bierze / pomija elementy od początku, dopóki warunek jest spełniony (Java 9+)"),
            e("forEach", "dla każdego", "wykonuje akcję dla każdego elementu"),
            e("collect", "zbierz", "zbiera elementy do kolekcji/mapy/napisu (za pomocą kolektora)"),
            e("collector", "kolektor", "przepis, JAK zebrać elementy (Collectors.toList()...)"),
            e("reduce", "zredukuj", "łączy wszystkie elementy w jedną wartość (np. sumę)"),
            e("identity", "element neutralny", "wartość startowa reduce (0 dla sumy, 1 dla iloczynu)"),
            e("accumulator", "akumulator", "funkcja dokładająca kolejny element do wyniku"),
            e("combiner", "łącznik", "funkcja łącząca częściowe wyniki (strumienie równoległe)"),
            e("count", "policz", "liczba elementów (typ long!)"),
            e("min / max", "najmniejszy / największy", "zwracają Optional"),
            e("sum / average", "suma / średnia", "dla IntStream/DoubleStream"),
            e("anyMatch", "czy którykolwiek pasuje", "true, jeśli choć jeden spełnia warunek"),
            e("allMatch", "czy wszystkie pasują", "true, jeśli wszystkie spełniają (pusty stream → true!)"),
            e("noneMatch", "czy żaden nie pasuje", "true, jeśli żaden nie spełnia warunku"),
            e("findFirst / findAny", "znajdź pierwszy / jakikolwiek", "zwracają Optional"),
            e("toList / toSet / toMap", "do listy / zbioru / mapy", "kolektory; Stream.toList() od Javy 16 (lista niemodyfikowalna)"),
            e("joining", "łączenie", "skleja napisy: joining(\", \")"),
            e("groupingBy", "grupuj według", "dzieli elementy na grupy → Map<klucz, lista>"),
            e("partitioningBy", "podziel według", "dzieli na dwie grupy: true / false"),
            e("counting", "liczenie", "kolektor liczący elementy w grupie"),
            e("summingInt / averagingDouble", "sumowanie / uśrednianie", "kolektory sumy i średniej"),
            e("mapping", "przekształcanie", "kolektor: przekształć elementy grupy przed zebraniem"),
            e("downstream", "dalszy (kolektor)", "kolektor stosowany do każdej grupy w groupingBy"),
            e("collectingAndThen", "zbierz, a potem", "zbierz, a następnie przekształć wynik"),
            e("teeing", "rozgałęzienie (jak trójnik rur)", "dwa kolektory naraz + połączenie wyników (Java 12+)"),
            e("summaryStatistics", "statystyki podsumowujące", "min, max, średnia, suma, liczba — naraz"),
            e("parallel", "równoległy", "przetwarzanie na wielu wątkach"),
            e("short-circuit", "skrócone wykonanie", "operacja kończy się wcześniej (findFirst, anyMatch, limit)"),
            e("infinite stream", "strumień nieskończony", "Stream.iterate/generate bez limitu — trzeba ograniczyć!"),
            e("range / rangeClosed", "zakres / zakres domknięty", "IntStream.range(0, 5) = 0..4, rangeClosed(1, 5) = 1..5"),
            e("concat", "sklej (concatenate)", "łączy dwa strumienie w jeden")
    );

    // =====================================================================================================
    // 7. LAMBDY I INTERFEJSY FUNKCYJNE
    // =====================================================================================================
    private static final List<Entry> LAMBDAS = List.of(
            e("lambda", "lambda (funkcja anonimowa)", "zwięzły zapis funkcji: (x) -> x * 2"),
            e("arrow", "strzałka", "znak -> w lambdzie i w switch"),
            e("functional interface", "interfejs funkcyjny", "interfejs z jedną metodą abstrakcyjną — można podać lambdę"),
            e("method reference", "referencja do metody", "skrót lambdy: String::length zamiast s -> s.length()"),
            e("closure", "domknięcie", "lambda „pamięta” zmienne z otoczenia"),
            e("effectively final", "w praktyce ostateczna", "zmienna nigdy nie zmieniana — może jej użyć lambda"),
            e("Predicate", "predykat (warunek)", "T → boolean; metoda test"),
            e("Function", "funkcja", "T → R; metoda apply"),
            e("BiFunction", "funkcja dwuargumentowa", "(T, U) → R"),
            e("Consumer", "konsument", "T → nic; metoda accept (np. wypisz)"),
            e("BiConsumer", "konsument dwuargumentowy", "(T, U) → nic"),
            e("Supplier", "dostawca", "nic → T; metoda get (np. utwórz obiekt)"),
            e("UnaryOperator", "operator jednoargumentowy", "T → T (ten sam typ)"),
            e("BinaryOperator", "operator dwuargumentowy", "(T, T) → T, np. dodawanie"),
            e("Runnable", "do uruchomienia", "nic → nic; metoda run"),
            e("Callable", "do wywołania", "nic → T, może rzucić wyjątek; metoda call"),
            e("test", "sprawdź", "metoda Predicate"),
            e("apply", "zastosuj", "metoda Function"),
            e("accept", "przyjmij", "metoda Consumer"),
            e("andThen", "a potem", "złóż: najpierw ta funkcja, potem następna"),
            e("compose", "złóż", "złóż odwrotnie: najpierw podana funkcja, potem ta"),
            e("and / or / negate", "i / lub / zaprzecz", "składanie predykatów"),
            e("not", "nie", "Predicate.not(...) — zaprzeczenie (Java 11+)"),
            e("higher-order function", "funkcja wyższego rzędu", "przyjmuje lub zwraca inną funkcję"),
            e("currying", "rozwijanie funkcji (currying)", "funkcja wieloargumentowa zamieniona na łańcuch jednoargumentowych")
    );

    // =====================================================================================================
    // 8. OPTIONAL
    // =====================================================================================================
    private static final List<Entry> OPTIONAL = List.of(
            e("optional", "opcjonalny", "„pudełko”, które zawiera wartość albo jest puste"),
            e("present", "obecny", "wartość jest w środku"),
            e("empty", "pusty", "brak wartości: Optional.empty()"),
            e("of / ofNullable", "z wartości / z wartości, która może być null", "of(null) rzuca NPE; ofNullable(null) daje pusty"),
            e("isPresent / isEmpty", "czy obecny / czy pusty", "sprawdzenie zawartości (isEmpty: Java 11+)"),
            e("orElse", "albo (wartość)", "wartość albo podana domyślna (domyślna liczona ZAWSZE)"),
            e("orElseGet", "albo pobierz", "wartość albo wynik Suppliera (liczony tylko, gdy pusty)"),
            e("orElseThrow", "albo rzuć", "wartość albo wyjątek"),
            e("ifPresent", "jeśli obecny", "wykonaj akcję tylko, gdy wartość jest"),
            e("ifPresentOrElse", "jeśli obecny, w przeciwnym razie", "dwie akcje: dla wartości i dla braku (Java 9+)"),
            e("or", "lub", "gdy pusty — spróbuj innego Optional (Java 9+)"),
            e("map / flatMap / filter", "przekształć / przekształć i spłaszcz / filtruj", "operacje wewnątrz Optional")
    );

    // =====================================================================================================
    // 9. WYJĄTKI
    // =====================================================================================================
    private static final List<Entry> EXCEPTIONS = List.of(
            e("exception", "wyjątek", "sytuacja wyjątkowa (błąd), którą można obsłużyć"),
            e("error", "błąd", "poważny problem JVM (OutOfMemoryError) — zwykle nie obsługujemy"),
            e("throwable", "coś do rzucenia", "wspólny przodek Exception i Error"),
            e("checked", "sprawdzany", "kompilator wymusza obsługę (try/catch lub throws)"),
            e("unchecked", "niesprawdzany", "RuntimeException, Error i ich podklasy — obsługa nieobowiązkowa"),
            e("stack trace", "ślad stosu", "lista wywołań metod prowadząca do błędu (czytaj od góry!)"),
            e("cause", "przyczyna", "wyjątek, który spowodował bieżący (getCause)"),
            e("message", "komunikat", "opis błędu (getMessage) — może być null"),
            e("rethrow", "rzuć ponownie", "złap i rzuć dalej"),
            e("wrap", "opakuj", "rzuć nowy wyjątek z oryginalnym jako przyczyną"),
            e("suppressed", "stłumiony", "dodatkowy wyjątek z zamykania zasobu w try-with-resources"),
            e("resource", "zasób", "coś do zamknięcia: plik, połączenie (AutoCloseable)"),
            e("AutoCloseable", "automatycznie zamykalny", "interfejs zasobów dla try-with-resources"),
            e("NullPointerException", "wyjątek pustej referencji", "użycie null jak obiektu (null.metoda())"),
            e("IllegalArgumentException", "niepoprawny argument", "metoda dostała złą wartość parametru"),
            e("IllegalStateException", "niepoprawny stan", "obiekt jest w złym stanie do tej operacji"),
            e("ArithmeticException", "wyjątek arytmetyczny", "np. dzielenie liczby całkowitej przez 0"),
            e("IndexOutOfBoundsException", "indeks poza zakresem", "indeks < 0 albo ≥ size"),
            e("NumberFormatException", "zły format liczby", "Integer.parseInt(\"abc\")"),
            e("ClassCastException", "błąd rzutowania klasy", "rzutowanie na niepasujący typ"),
            e("ConcurrentModificationException", "wyjątek równoczesnej modyfikacji", "zmiana kolekcji w trakcie iteracji"),
            e("UnsupportedOperationException", "nieobsługiwana operacja", "np. add na List.of(...)"),
            e("NoSuchElementException", "brak takiego elementu", "np. get() na pustym Optional"),
            e("InputMismatchException", "niezgodne dane wejściowe", "Scanner.nextInt() dostał tekst"),
            e("fail fast", "zawiedź szybko", "zgłoś błąd natychmiast, zanim narobi szkód"),
            e("retry", "ponów", "spróbuj jeszcze raz (z limitem prób!)")
    );

    // =====================================================================================================
    // 10. NAPISY
    // =====================================================================================================
    private static final List<Entry> STRINGS = List.of(
            e("string", "napis, łańcuch znaków", "tekst; klasa String jest niezmienna"),
            e("character", "znak", "pojedynczy znak (char)"),
            e("length", "długość", "liczba znaków (dokładniej: jednostek UTF-16)"),
            e("charAt", "znak na pozycji", "znak o podanym indeksie"),
            e("substring", "podciąg", "fragment napisu [od, do)"),
            e("indexOf", "indeks (pozycja) czegoś", "gdzie zaczyna się fragment; -1 gdy brak"),
            e("split", "podziel", "dzieli napis według separatora (to REGEX!)"),
            e("join", "połącz", "String.join(\", \", lista)"),
            e("trim / strip", "przytnij / obierz", "usuwa białe znaki z brzegów (strip zna Unicode, Java 11+)"),
            e("isBlank / isEmpty", "czy pusty lub same białe znaki / czy pusty", "isBlank: \"  \" → true (Java 11+); isEmpty: tylko \"\" → true"),
            e("replace / replaceAll", "zamień / zamień (regex)", "OBA zamieniają WSZYSTKIE wystąpienia; replace = dosłowny tekst, replaceAll = wzorzec REGEX"),
            e("upper case / lower case", "wielkie / małe litery", "toUpperCase / toLowerCase"),
            e("equalsIgnoreCase", "równy, ignorując wielkość liter", "porównanie bez rozróżniania A/a"),
            e("startsWith / endsWith", "zaczyna się od / kończy się na", "sprawdzenie początku/końca"),
            e("repeat", "powtórz", "\"ab\".repeat(3) = \"ababab\" (Java 11+)"),
            e("format / formatted", "formatuj / sformatowany", "wstawia wartości w szablon: %s, %d, %.2f (formatted: Java 15+)"),
            e("placeholder", "symbol zastępczy", "%s, %d — miejsca na wartości w szablonie"),
            e("StringBuilder", "budowniczy napisów", "zmienny napis — szybkie doklejanie w pętli"),
            e("append / insert", "doklej / wstaw", "metody StringBuilder"),
            e("regex (regular expression)", "wyrażenie regularne", "wzorzec opisujący tekst, np. \\d+ = cyfry"),
            e("pattern / matcher", "wzorzec / dopasowywacz", "klasy do regexów"),
            e("match / matches", "dopasuj / pasuje", "czy tekst pasuje do wzorca"),
            e("text block", "blok tekstu", "napis wielowierszowy w \"\"\" ... \"\"\" (Java 15+)"),
            e("escape sequence", "sekwencja specjalna (escape)", "\\n nowa linia, \\t tabulator, \\\" cudzysłów"),
            e("locale", "ustawienia regionalne", "język/kraj: przecinek czy kropka, nazwy miesięcy"),
            e("encoding / charset", "kodowanie / zestaw znaków", "jak znaki zamienić na bajty (UTF-8)"),
            e("string pool", "pula napisów", "wspólne miejsce na literały napisów")
    );

    // =====================================================================================================
    // 11. LICZBY
    // =====================================================================================================
    private static final List<Entry> NUMBERS = List.of(
            e("integer", "liczba całkowita", "bez części ułamkowej"),
            e("floating point", "zmiennoprzecinkowa", "double/float — przybliżone!"),
            e("decimal", "dziesiętny", "BigDecimal — dokładne liczby dziesiętne (pieniądze)"),
            e("parse", "przetwórz (sparsuj)", "zamień tekst na liczbę/datę: Integer.parseInt(\"5\")"),
            e("round", "zaokrąglij", "Math.round"),
            e("scale", "skala", "liczba cyfr po przecinku w BigDecimal"),
            e("rounding mode", "tryb zaokrąglania", "HALF_UP (szkolne), HALF_EVEN (bankierskie)..."),
            e("precision", "precyzja", "liczba cyfr znaczących"),
            e("overflow", "przepełnienie", "wynik nie mieści się w typie (MAX_VALUE + 1 = MIN_VALUE)"),
            e("add / subtract", "dodaj / odejmij", "BigDecimal nie ma + i -, są metody"),
            e("multiply / divide", "pomnóż / podziel", "divide bez skali może rzucić ArithmeticException"),
            e("remainder (%)", "reszta z dzielenia", "operator %: -7 % 3 == -1 (dla ujemnych reszta może być ujemna)"),
            e("modulo (floorMod)", "modulo", "Math.floorMod(-7, 3) == 2 — zawsze nieujemne dla dodatniego dzielnika"),
            e("floor / ceil", "podłoga / sufit", "zaokrąglenie w dół / w górę"),
            e("abs (absolute)", "wartość bezwzględna", "Math.abs(-5) = 5"),
            e("pow (power)", "potęga", "Math.pow(2, 10) = 1024.0"),
            e("sqrt (square root)", "pierwiastek kwadratowy", "Math.sqrt(9) = 3.0"),
            e("random", "losowy", "Random, Math.random()"),
            e("seed", "ziarno", "wartość startowa generatora — ten sam seed = te same liczby"),
            e("exact", "dokładny", "Math.addExact — rzuci wyjątek przy przepełnieniu zamiast „zawinąć”"),
            e("signum", "znak liczby", "-1, 0 albo 1"),
            e("NaN (Not a Number)", "nie-liczba", "wynik 0.0/0.0"),
            e("infinity", "nieskończoność", "wynik 1.0/0.0"),
            e("binary / hex", "dwójkowy / szesnastkowy", "0b1010, 0xFF"),
            e("bitwise", "bitowy", "operatory działające na bitach: & | ^ ~ << >>")
    );

    // =====================================================================================================
    // 12. DATA I CZAS
    // =====================================================================================================
    private static final List<Entry> DATE_TIME = List.of(
            e("date / time", "data / czas", "LocalDate / LocalTime"),
            e("local", "lokalny", "bez strefy czasowej — „jak na kalendarzu na ścianie”"),
            e("zone / time zone", "strefa / strefa czasowa", "ZoneId.of(\"Europe/Warsaw\")"),
            e("instant", "chwila", "punkt na osi czasu (UTC), np. do logów"),
            e("duration", "czas trwania", "ilość czasu w sekundach/godzinach"),
            e("period", "okres", "ilość czasu w dniach/miesiącach/latach"),
            e("formatter", "formater", "DateTimeFormatter — jak zamienić datę na tekst i odwrotnie"),
            e("plus / minus", "dodaj / odejmij", "zwracają NOWĄ datę (daty są niezmienne!)"),
            e("with", "z (zmienionym polem)", "date.withDayOfMonth(1) — nowa data z innym dniem"),
            e("isBefore / isAfter", "czy przed / czy po", "porównanie dat"),
            e("between", "pomiędzy", "ChronoUnit.DAYS.between(a, b)"),
            e("adjuster", "korektor", "TemporalAdjusters.lastDayOfMonth()"),
            e("clock", "zegar", "źródło bieżącego czasu (do testów można podmienić)"),
            e("leap year", "rok przestępny", "29 lutego"),
            e("day of week", "dzień tygodnia", "DayOfWeek.MONDAY... (po polsku: getDisplayName z Locale)"),
            e("timestamp", "znacznik czasu", "moment zdarzenia zapisany np. w logu")
    );

    // =====================================================================================================
    // 13. PLIKI I IO
    // =====================================================================================================
    private static final List<Entry> IO = List.of(
            e("file", "plik", "dane zapisane na dysku"),
            e("directory / folder", "katalog / folder", "miejsce na pliki"),
            e("path", "ścieżka", "położenie pliku: Path.of(\"dane\", \"a.txt\")"),
            e("absolute / relative", "bezwzględna / względna", "od korzenia dysku / od bieżącego katalogu"),
            e("resolve", "dołącz (rozwiąż)", "dir.resolve(\"a.txt\") = dir/a.txt"),
            e("normalize", "znormalizuj", "usuwa . i .. ze ścieżki"),
            e("read / write", "czytaj / pisz", "odczyt / zapis"),
            e("append", "dopisz", "dopisz na końcu pliku zamiast nadpisywać"),
            e("create / delete", "utwórz / usuń", "Files.createDirectories, Files.delete"),
            e("exists", "istnieje", "Files.exists(path)"),
            e("lines", "linie", "Files.lines — stream linii pliku (zamykaj go!)"),
            e("buffer / buffered", "bufor / buforowany", "zbiera dane w pamięci, by czytać/pisać większymi porcjami"),
            e("reader / writer", "czytnik / pisarz", "strumienie IO dla ZNAKÓW (tekst)"),
            e("input / output stream", "strumień wejścia / wyjścia", "strumienie IO dla BAJTÓW — to NIE jest Stream API!"),
            e("flush", "opróżnij (bufor)", "wymuś zapis zbuforowanych danych"),
            e("close", "zamknij", "zwolnij zasób (plik, połączenie)"),
            e("serialize", "serializować", "zamienić obiekt na bajty (i z powrotem: deserializować)"),
            e("properties", "właściwości", "plik klucz=wartość z konfiguracją"),
            e("walk", "przejdź", "Files.walk — przejście po drzewie katalogów (zamykaj!)"),
            e("glob", "wzorzec nazw plików", "*.txt, **/*.java"),
            e("CSV", "wartości rozdzielone przecinkami", "prosty format tabeli w pliku tekstowym"),
            e("JSON", "notacja obiektów JavaScript", "popularny format danych: {\"klucz\": \"wartość\"}"),
            e("header", "nagłówek", "pierwszy wiersz CSV z nazwami kolumn"),
            e("temp / temporary", "tymczasowy", "plik/katalog do chwilowego użytku")
    );

    // =====================================================================================================
    // 14. WĄTKI I WSPÓŁBIEŻNOŚĆ
    // =====================================================================================================
    private static final List<Entry> CONCURRENCY = List.of(
            e("thread", "wątek", "niezależna ścieżka wykonywania programu"),
            e("concurrency", "współbieżność", "wiele zadań postępuje „w tym samym czasie”"),
            e("parallelism", "równoległość", "zadania naprawdę wykonują się jednocześnie (wiele rdzeni)"),
            e("executor / executor service", "wykonawca / usługa wykonawcza", "zarządza pulą wątków"),
            e("thread pool", "pula wątków", "gotowe wątki wielokrotnego użytku"),
            e("submit", "zgłoś, przekaż", "przekaż zadanie do wykonania"),
            e("future", "przyszły wynik", "wynik zadania, który będzie dostępny później"),
            e("shutdown", "wyłącz", "zakończ przyjmowanie zadań"),
            e("await termination", "czekaj na zakończenie", "czekaj, aż zadania się skończą"),
            e("join", "dołącz, poczekaj", "poczekaj na zakończenie wątku"),
            e("sleep", "śpij", "wstrzymaj wątek na czas"),
            e("interrupt / interrupted", "przerwij / przerwany", "prośba do wątku o zakończenie"),
            e("race condition", "wyścig", "wynik zależy od kolejności wątków — błąd!"),
            e("lock / unlock", "zablokuj / odblokuj", "tylko jeden wątek w sekcji krytycznej"),
            e("deadlock", "zakleszczenie", "wątki czekają na siebie nawzajem w nieskończoność"),
            e("atomic", "atomowy (niepodzielny)", "operacja wykonuje się w całości albo wcale"),
            e("thread-safe", "bezpieczny wątkowo", "działa poprawnie przy wielu wątkach"),
            e("latch", "zatrzask", "CountDownLatch — czekaj, aż licznik spadnie do 0"),
            e("semaphore", "semafor", "ogranicza liczbę wątków naraz"),
            e("barrier", "bariera", "wątki czekają na siebie w punkcie zbiórki"),
            e("async / asynchronous", "asynchroniczny", "nie czekamy na wynik — przyjdzie później"),
            e("CompletableFuture", "przyszły wynik do uzupełnienia", "wynik asynchroniczny, który można łączyć w łańcuchy"),
            e("supplyAsync / runAsync", "dostarcz / uruchom asynchronicznie", "start zadania w tle"),
            e("thenApply / thenAccept", "potem zastosuj / potem przyjmij", "kolejny krok po wyniku"),
            e("thenCompose", "potem złóż", "kolejne zadanie asynchroniczne zależne od wyniku"),
            e("thenCombine", "potem połącz", "połącz wyniki dwóch niezależnych zadań"),
            e("exceptionally / handle", "w razie wyjątku / obsłuż", "obsługa błędu w łańcuchu"),
            e("timeout", "limit czasu", "maksymalny czas oczekiwania"),
            e("producer / consumer", "producent / konsument", "jeden wątek wytwarza dane, drugi je zużywa"),
            e("blocking queue", "kolejka blokująca", "czeka, gdy pusta (take) lub pełna (put)")
    );

    // =====================================================================================================
    // 15. ADNOTACJE I REFLEKSJA
    // =====================================================================================================
    private static final List<Entry> ANNOTATIONS = List.of(
            e("annotation", "adnotacja", "etykieta w kodzie: @Override, @Deprecated"),
            e("retention", "zachowanie (retencja)", "jak długo adnotacja istnieje: SOURCE / CLASS / RUNTIME"),
            e("target", "cel", "do czego można przyczepić adnotację: pole, metodę, klasę"),
            e("marker", "znacznik", "adnotacja bez parametrów (samo „oznaczenie”)"),
            e("reflection", "refleksja", "program bada i zmienia sam siebie w czasie działania"),
            e("declared", "zadeklarowany", "getDeclaredFields — pola zadeklarowane w tej klasie"),
            e("accessible", "dostępny", "setAccessible(true) — dostęp do prywatnych pól"),
            e("invoke", "wywołaj", "Method.invoke — uruchom metodę przez refleksję"),
            e("modifier", "modyfikator", "public, static, final... (klasa Modifier)"),
            e("suppress warnings", "wycisz ostrzeżenia", "@SuppressWarnings(\"unchecked\")")
    );

    // =====================================================================================================
    // 16. NARZĘDZIA, TESTY, PRACA ZESPOŁOWA
    // =====================================================================================================
    private static final List<Entry> TOOLS = List.of(
            e("JDK (Java Development Kit)", "zestaw narzędzi programisty", "kompilator + JVM + narzędzia"),
            e("JRE (Java Runtime Environment)", "środowisko uruchomieniowe", "JVM + biblioteki (do uruchamiania)"),
            e("JVM (Java Virtual Machine)", "wirtualna maszyna Javy", "uruchamia bytecode (.class)"),
            e("IDE", "zintegrowane środowisko programistyczne", "IntelliJ, Eclipse — edytor + kompilator + debugger"),
            e("build", "budowanie, zbuduj", "kompilacja i przygotowanie programu"),
            e("debug / debugger", "debugować / debugger", "szukać błędów, zatrzymując program krok po kroku"),
            e("breakpoint", "punkt przerwania", "miejsce, w którym debugger zatrzyma program"),
            e("test / unit test", "test / test jednostkowy", "automatyczne sprawdzenie małego kawałka kodu"),
            e("assert / assertion", "sprawdź / asercja", "sprawdzenie w teście: assertEquals(oczekiwany, faktyczny)"),
            e("expected / actual", "oczekiwany / faktyczny", "wynik, który powinien być / który jest"),
            e("given / when / then", "mając / gdy / wtedy", "schemat testu: przygotuj → wykonaj → sprawdź"),
            e("mock / stub / fake", "atrapa / zaślepka / podróbka", "zastępcze obiekty w testach"),
            e("dependency (Maven)", "zależność", "zewnętrzna biblioteka dodana w pom.xml"),
            e("repository", "repozytorium", "magazyn: kodu (git) albo obiektów (w kodzie)"),
            e("branch", "gałąź", "osobna linia zmian w git"),
            e("commit", "zatwierdzenie", "zapisany zestaw zmian w git"),
            e("merge", "scalenie", "połączenie zmian z dwóch gałęzi"),
            e("pull request / code review", "prośba o scalenie / przegląd kodu", "inni sprawdzają kod przed scaleniem"),
            e("clean code", "czysty kod", "kod czytelny i łatwy do zmiany"),
            e("DRY (Don't Repeat Yourself)", "nie powtarzaj się", "jedna wiedza w jednym miejscu"),
            e("KISS (Keep It Simple, Stupid)", "zachowaj prostotę", "najprostsze działające rozwiązanie"),
            e("YAGNI (You Aren't Gonna Need It)", "nie będziesz tego potrzebować", "nie pisz „na zapas”"),
            e("SOLID", "SOLID", "5 zasad projektowania obiektowego (t27_clean_code_pitfalls)")
    );

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • get = pobierz, set = ustaw, is/has = czy jest/czy ma, to = zamień na, of/from = utwórz z, find = znajdź.
     *   • filter = filtruj, map = przekształć, collect = zbierz, reduce = zredukuj, sorted = posortowany.
     *   • throw = rzuć (instrukcja), throws = rzuca (deklaracja w nagłówku metody).
     *   • override = nadpisać (podklasa), overload = przeciążyć (ta sama nazwa, inne parametry).
     *   • immutable = niezmienny, mutable = zmienny; final = nie przypiszesz ponownie (co innego niż niezmienny!).
     *   • stream (Stream API) ≠ strumień IO (InputStream/Reader).
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się throw od throws?
     *   2. Czym różni się override od overload?
     *   3. Co znaczą przedrostki is..., to..., find... w nazwach metod?
     *   4. Czy  final List<String> list = new ArrayList<>();  pozwala wywołać list.add("x")? Dlaczego?
     *   5. Czy  int record = 5;  się skompiluje? A  int class = 5; ?
     *   6. Co zwróci  "aXa".replace("a", "b")  — "bXa" czy "bXb"?
     *   7. Ile wynosi  -7 % 3  w Javie?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. throw rzuca wyjątek w kodzie; throws w nagłówku metody ogłasza, że metoda może go rzucić.
     *   2. override = podklasa zmienia działanie odziedziczonej metody; overload = kilka metod o tej samej
     *      nazwie z różnymi parametrami.
     *   3. is... zwraca boolean (czy?), to... zamienia na inny typ, find... szuka i może nie znaleźć (zwykle Optional).
     *   4. Tak. final zabrania tylko PONOWNEGO PRZYPISANIA zmiennej (list = ...), a nie zmiany obiektu listy.
     *   5. int record = 5; — tak (record to słowo kontekstowe). int class = 5; — nie (class to słowo kluczowe).
     *   6. "bXb" — replace zamienia WSZYSTKIE wystąpienia (różnica z replaceAll: tamto używa regexu).
     *   7. -1 (operator % to reszta z dzielenia; modulo w sensie matematycznym daje Math.floorMod(-7, 3) = 2).
     */
    // </editor-fold>
}
