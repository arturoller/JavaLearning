package t06_oop_basics;

import helpers.Check;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import static java.lang.Math.abs;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Pakiety i modyfikatory dostępu — kto widzi moją klasę, pole, metodę?
 *        (package = pakiet, import = import, access modifier = modyfikator dostępu)
 *
 * W SKRÓCIE:
 *   Pakiet to folder z klasami i jednocześnie „nazwisko” klasy: pełna nazwa ArrayList to
 *   java.util.ArrayList. import pozwala pisać krótką nazwę. Modyfikatory public / protected /
 *   (brak) / private decydują, czy kod z INNEGO pakietu (albo innej klasy) może czegoś użyć.
 *
 * ANALOGIA:
 *   Pakiet to osiedle, klasa to dom. Pełny adres: „Osiedle Słoneczne, dom Kowalskich” — dwie rodziny
 *   Kowalskich na różnych osiedlach to różne domy. import = „mówiąc »Kowalscy«, mam na myśli tych
 *   ze Słonecznego”. public = ogród otwarty dla wszystkich; bez modyfikatora = plac zabaw tylko dla
 *   mieszkańców osiedla; private = sypialnia.
 *
 * JAK TO DZIAŁA:
 *   plik  src/t06_oop_basics/Oop08PackagesAccess.java
 *   linia package t06_oop_basics;          ← MUSI zgadzać się z folderem
 *   pełna nazwa  t06_oop_basics.Oop08PackagesAccess   (tak widzi ją JVM)
 *
 *   modyfikator    | ta klasa | ten pakiet | podklasa w innym pakiecie | wszędzie
 *   public         |   tak    |    tak     |           tak             |   tak
 *   protected      |   tak    |    tak     |           tak             |   nie
 *   (brak)         |   tak    |    tak     |           nie             |   nie
 *   private        |   tak    |    nie     |           nie             |   nie
 *
 * SŁÓWKA:
 *   package = pakiet; import = importuj; fully qualified name = pełna (kwalifikowana) nazwa;
 *   package-private = prywatny w pakiecie (brak modyfikatora); protected = chroniony; namespace =
 *   przestrzeń nazw; default package = pakiet domyślny (bez nazwy); reverse domain = odwrócona domena
 *
 * ZOBACZ TEŻ: t06_oop_basics/Oop03Encapsulation (private + gettery),
 *             t06_oop_basics/Oop04Static (import static),
 *             t07_inheritance_polymorphism/Inherit01Basics (protected i podklasy),
 *             t19_annotations_reflection/Annotations03ReflectionBasics (Class.forName i refleksja)
 * </pre>
 */
public class Oop08PackagesAccess {

    public static void main(String[] args) {
        title("Oop08 — pakiety i modyfikatory dostępu");

        packagesAsFolders();        // packages as folders = pakiety jako foldery
        importVsFullName();         // import vs full name = import kontra pełna nazwa
        staticImport();             // static import = import statyczny
        nameClashes();              // name clashes = konflikty nazw
        packagePrivate();           // package-private = widoczne w pakiecie
        accessTable();              // access table = tabela dostępu
        protectedPreview();         // protected preview = zapowiedź protected
        conventions();              // conventions = konwencje (pakiet domyślny, nazwy)
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PAKIET = FOLDER + PRZESTRZEŃ NAZW
    // =================================================================================================

    /**
     * 1. Pierwsza linia pliku {@code package t06_oop_basics;} mówi, w jakim pakiecie jest klasa. Pakiet
     * odpowiada folderowi. Pełna nazwa klasy = pakiet + kropka + nazwa klasy — tak widzi ją JVM.
     */
    static void packagesAsFolders() {
        section("1. Pakiet = folder + przestrzeń nazw");

        show("pakiet tej lekcji", Oop08PackagesAccess.class.getPackageName());   // getPackageName (Java 9+) = nazwa pakietu
        // WYNIK: pakiet tej lekcji → t06_oop_basics
        show("pełna nazwa tej klasy", Oop08PackagesAccess.class.getName());       // getName = pełna nazwa
        // WYNIK: pełna nazwa tej klasy → t06_oop_basics.Oop08PackagesAccess
        show("pełna nazwa String", String.class.getName());
        // WYNIK: pełna nazwa String → java.lang.String
        show("pełna nazwa ArrayList", ArrayList.class.getName());
        // WYNIK: pełna nazwa ArrayList → java.util.ArrayList
        show("pakiet klasy Check z kursu", Check.class.getPackageName());
        // WYNIK: pakiet klasy Check z kursu → helpers

        // Struktura tego kursu:
        //   src/
        //    ├─ helpers/                 package helpers;         ← Console, Check, SampleData...
        //    │   └─ model/               package helpers.model;   ← Product, Customer... (podfolder = osobny pakiet)
        //    ├─ t06_oop_basics/          package t06_oop_basics;  ← ta lekcja
        //    └─ t07_inheritance_polymorphism/ ...
        //
        // W kursie pakiety leżą od razu w src/ (helpers, t01_basics...) — w projekcie do nauki to w porządku.
        // W prawdziwych projektach pakiety zaczynają się od odwróconej domeny firmy: com.firma.aplikacja...
        //
        // PUŁAPKA: linia package musi pasować do folderu. Plik w src/t06_oop_basics/ z linią
        // "package helpers;" → IntelliJ zgłasza błąd, a skompilowana klasa trafia w złe miejsce.
        // DOBRA PRAKTYKA: w pliku jest co najwyżej JEDNA klasa public najwyższego poziomu i nazywa się
        // dokładnie tak jak plik (Oop08PackagesAccess ↔ Oop08PackagesAccess.java).
    }

    // =================================================================================================
    // 2. import KONTRA PEŁNA NAZWA
    // =================================================================================================

    /**
     * 2. import to tylko skrót nazwy dla kompilatora — niczego nie „ładuje” i nie spowalnia programu.
     * Bez importu zawsze można napisać pełną nazwę. Pakiet {@code java.lang} (String, Math, System)
     * jest importowany automatycznie.
     */
    static void importVsFullName() {
        section("2. import kontra pełna nazwa");

        ArrayList<String> names = new ArrayList<>();            // krótka nazwa — dzięki import java.util.ArrayList
        names.add("Ala");
        show("ArrayList z importem", names);
        // WYNIK: ArrayList z importem → [Ala]

        java.time.LocalDate date = java.time.LocalDate.of(2026, 3, 1);   // PEŁNA nazwa — bez importu
        show("LocalDate bez importu (pełna nazwa)", date);
        // WYNIK: LocalDate bez importu (pełna nazwa) → 2026-03-01

        show("Math.max bez importu (java.lang)", Math.max(3, 7));
        // WYNIK: Math.max bez importu (java.lang) → 7

        // JVM szuka klas ZAWSZE po pełnej nazwie — sama krótka nazwa to za mało:
        try {                                                   // try/catch = spróbuj/złap (wyjątki: t10)
            show("Class.forName(\"java.util.ArrayList\")", Class.forName("java.util.ArrayList").getSimpleName());
        } catch (ClassNotFoundException e) {
            show("nie znaleziono", e.getMessage());
        }
        // WYNIK: Class.forName("java.util.ArrayList") → ArrayList
        expectThrows("Class.forName(\"ArrayList\")", () -> Class.forName("ArrayList"));   // forName = znajdź po nazwie
        // WYNIK: ✔ Class.forName("ArrayList") → rzucono ClassNotFoundException: ArrayList

        // import java.util.*;  ← import „z gwiazdką”: wszystkie klasy z java.util (ale NIE z podpakietów,
        //                        np. java.util.function trzeba zaimportować osobno).
        // DOBRA PRAKTYKA: importuj konkretne klasy (IntelliJ robi to sam: Alt+Enter, a Ctrl+Alt+O usuwa
        // nieużywane importy). Pełnej nazwy używaj tylko przy konflikcie nazw (sekcja 4).
    }

    // =================================================================================================
    // 3. import static
    // =================================================================================================

    /**
     * 3. {@code import static} importuje składowe static (metody, stałe), żeby pisać je bez nazwy klasy.
     * Ta lekcja od początku używa {@code import static helpers.Console.*} — dlatego piszemy show(...),
     * a nie Console.show(...).
     */
    static void staticImport() {
        section("3. import static");

        show("abs(-7) dzięki import static java.lang.Math.abs", abs(-7));   // abs = wartość bezwzględna
        // WYNIK: abs(-7) dzięki import static java.lang.Math.abs → 7
        show("to samo z nazwą klasy: Math.abs(-7)", Math.abs(-7));
        // WYNIK: to samo z nazwą klasy: Math.abs(-7) → 7

        // PUŁAPKA: przy wielu importach static czytelnik nie wie, skąd pochodzi metoda (abs? z Math?
        // z naszej klasy?). Dwa importy static tej samej nazwy z różnych klas → błąd kompilacji
        // „reference to ... is ambiguous” (niejednoznaczne odwołanie) — dopiero przy użyciu tej nazwy.
        // DOBRA PRAKTYKA: import static tylko dla bardzo znanych rzeczy (Math, asercje w testach,
        // pomocnicy jak Console w tym kursie).
    }

    // =================================================================================================
    // 4. KONFLIKT NAZW: java.util.List KONTRA java.awt.List
    // =================================================================================================

    /**
     * 4. Dwie różne klasy mogą mieć tę samą krótką nazwę, jeśli leżą w różnych pakietach. Wtedy jedną
     * importujemy, a drugą piszemy pełną nazwą.
     */
    static void nameClashes() {
        section("4. Konflikt nazw: java.util.List kontra java.awt.List");

        List<String> fruits = List.of("jabłko", "gruszka");     // java.util.List z importu; List.of (Java 9+)
        Class<?> awtList = java.awt.List.class;                  // java.awt.List (lista w okienku) — PEŁNA nazwa
        show("List (z importu)", List.class.getName());
        // WYNIK: List (z importu) → java.util.List
        show("java.awt.List (pełna nazwa)", awtList.getName());
        // WYNIK: java.awt.List (pełna nazwa) → java.awt.List
        show("to ta sama klasa?", List.class.equals(awtList));
        // WYNIK: to ta sama klasa? → false
        show("fruits", fruits);
        // WYNIK: fruits → [jabłko, gruszka]

        // import java.util.List;
        // import java.awt.List;   → błąd kompilacji: a type with the same simple name List is already
        //                           defined by the single-type-import of java.util.List
        // Inny znany konflikt: java.util.Date i java.sql.Date.
        //
        // PUŁAPKA: nie nazywaj własnych klas tak jak klasy z java.lang (String, Integer, Object)
        // — Twoja klasa „zasłoni” klasę z JDK w całym pakiecie i zrobi się bardzo dziwnie.
    }

    // =================================================================================================
    // 5. PACKAGE-PRIVATE — WIDOCZNE TYLKO W PAKIECIE
    // =================================================================================================

    /** Klasa BEZ modyfikatora dostępu (package-private) — widoczna tylko w pakiecie t06_oop_basics. */
    static class Workshop {                     // workshop = warsztat
    }

    /**
     * 5. Brak modyfikatora = package-private: widzą to wszystkie klasy z TEGO SAMEGO pakietu i nikt poza
     * nim. Lekcje wołają Check i show z pakietu helpers tylko dlatego, że tamte klasy i metody są public.
     */
    static void packagePrivate() {
        section("5. package-private — widoczne tylko w pakiecie");

        // Modifier (refleksja, t19) potrafi odczytać modyfikatory klasy w trakcie działania programu:
        show("helpers.Check — public?", Modifier.isPublic(Check.class.getModifiers()));   // isPublic = czy publiczna
        // WYNIK: helpers.Check — public? → true
        show("nasza Workshop — public?", Modifier.isPublic(Workshop.class.getModifiers()));
        // WYNIK: nasza Workshop — public? → false
        show("modyfikatory Workshop", Modifier.toString(Workshop.class.getModifiers()));
        // WYNIK: modyfikatory Workshop → static
        note("brak public/protected/private na liście = package-private");
        // WYNIK: ℹ brak public/protected/private na liście = package-private

        // Na przykładzie kursu:
        //   • Z lekcji w t06_oop_basics można użyć package-private klasy z INNEJ lekcji tego pakietu,
        //     np. new Oop07NestedClasses.Car.Engine(120) — ten sam pakiet, więc wolno.
        //   • Ta sama linia w klasie z pakietu helpers → błąd kompilacji: Car is not public ...;
        //     cannot be accessed from outside package.
        //   • W drugą stronę: lekcje wołają show(...), section(...), Check.equal(...) z pakietu helpers.
        //     Gdyby show nie miała modyfikatora, widziałyby ją tylko klasy z pakietu helpers.

        // PUŁAPKA: podpakiet to OSOBNY pakiet. helpers.model NIE widzi package-private rzeczy z helpers
        // (i odwrotnie) — kropka w nazwie nie daje żadnych przywilejów.
        // DOBRA PRAKTYKA: package-private jest świetne dla klas pomocniczych, których inne pakiety nie
        // powinny używać — mniej public = mniej rzeczy, które ktoś może zepsuć, używając ich źle.
    }

    // =================================================================================================
    // 6. TABELA MODYFIKATORÓW DOSTĘPU
    // =================================================================================================

    /** Wiersze tabeli: modyfikator, ta klasa, ten pakiet, podklasa w innym pakiecie, wszędzie. */
    private static final String[][] ACCESS_ROWS = {             // access rows = wiersze dostępu
        {"public", "tak", "tak", "tak", "tak"},
        {"protected", "tak", "tak", "tak", "nie"},
        {"(brak)", "tak", "tak", "nie", "nie"},
        {"private", "tak", "nie", "nie", "nie"},
    };

    /**
     * 6. Cztery poziomy dostępu od najszerszego do najwęższego. Drukujemy tabelę przez
     * {@code String.format} z wyrównaniem kolumn ({@code %-12s} = tekst do lewej na 12 znakach).
     */
    static void accessTable() {
        section("6. Tabela modyfikatorów dostępu");

        String header = String.format(Locale.ROOT, "%-12s %-6s %-7s %-9s %s",
                "modyfikator", "klasa", "pakiet", "podklasa", "wszędzie");
        System.out.println(header);
        // WYNIK: modyfikator  klasa  pakiet  podklasa  wszędzie
        for (String[] row : ACCESS_ROWS) {
            System.out.println(String.format(Locale.ROOT, "%-12s %-6s %-7s %-9s %s",
                    row[0], row[1], row[2], row[3], row[4]));
        }
        // WYNIK: public       tak    tak     tak       tak
        // WYNIK: protected    tak    tak     tak       nie
        // WYNIK: (brak)       tak    tak     nie       nie
        // WYNIK: private      tak    nie     nie       nie

        // Klasa NAJWYŻSZEGO poziomu (nie zagnieżdżona) może być tylko public albo (brak):
        //   private class Secret { }   → błąd kompilacji: modifier private not allowed here
        // Klasy zagnieżdżone mogą mieć każdy z czterech modyfikatorów.
        //
        // DOBRA PRAKTYKA: zaczynaj od NAJWĘŻSZEGO dostępu (private) i poszerzaj tylko, gdy trzeba.
        // Pola: prawie zawsze private (Oop03). Metody: public tylko te, które są „usługą” klasy.
    }

    // =================================================================================================
    // 7. protected — ZAPOWIEDŹ (dziedziczenie w t07)
    // =================================================================================================

    /** Klasa bazowa z metodą protected (dziedziczenie: t07). */
    static class Animal {                       // Animal = zwierzę
        protected String sound() {              // sound = dźwięk; protected = pakiet + podklasy
            return "...";
        }
    }

    /** Podklasa: extends = rozszerza (t07). Może nadpisać i wywołać metodę protected. */
    static class Dog extends Animal {           // Dog = pies
        @Override
        protected String sound() {
            return "Hau! (a przodek mówił: " + super.sound() + ")";
        }
    }

    /**
     * 7. protected = wszystko, co daje package-private, PLUS dostęp dla podklas w innych pakietach.
     * Zaskoczenie dla wielu początkujących: protected jest SZERSZY niż brak modyfikatora.
     */
    static void protectedPreview() {
        section("7. protected — zapowiedź");

        Animal animal = new Animal();
        show("animal.sound() — wołamy protected z tego samego pakietu", animal.sound());
        // WYNIK: animal.sound() — wołamy protected z tego samego pakietu → ...
        show("new Dog().sound()", new Dog().sound());
        // WYNIK: new Dog().sound() → Hau! (a przodek mówił: ...)

        // PUŁAPKA: „protected = prawie private” to mit. Każda klasa z tego samego pakietu widzi protected,
        // a do tego podklasy z dowolnego pakietu. Szczegóły i ograniczenia: t07_inheritance_polymorphism.
        // DOBRA PRAKTYKA: protected stosuj świadomie — dla „haczyków” przeznaczonych do nadpisania
        // w podklasach, a nie jako „trochę bezpieczniejsze public”.
    }

    // =================================================================================================
    // 8. PAKIET DOMYŚLNY I KONWENCJE NAZW
    // =================================================================================================

    /**
     * 8. Klasa bez linii package trafia do pakietu domyślnego (bez nazwy) — nie da się jej zaimportować
     * z żadnego nazwanego pakietu. Nazwy pakietów: małe litery, odwrócona domena, bez myślników.
     */
    static void conventions() {
        section("8. Pakiet domyślny i konwencje nazw");

        note("przykład: domena firmy sklep.example.com → pakiet com.example.sklep");
        // WYNIK: ℹ przykład: domena firmy sklep.example.com → pakiet com.example.sklep
        note("moduły aplikacji to kolejne człony: com.example.sklep.zamowienia, com.example.sklep.platnosci");
        // WYNIK: ℹ moduły aplikacji to kolejne człony: com.example.sklep.zamowienia, com.example.sklep.platnosci
        show("pakiet LocalDate z JDK", java.time.LocalDate.class.getPackageName());
        // WYNIK: pakiet LocalDate z JDK → java.time

        // PAKIET DOMYŚLNY: plik Hello.java bez linii package. Wystarczy na jednoplikowy eksperyment
        // (java Hello.java), ale w projekcie to błąd: takiej klasy nie zaimportujesz z żadnego pakietu.
        //
        // KONWENCJE:
        //   • tylko małe litery ASCII i cyfry: com.example.sklep (nie Com.Example, nie zamówienia z ó)
        //   • myślnik jest niedozwolony (moja-firma.pl → pl.moja_firma albo pl.mojafirma)
        //   • człon nie może zaczynać się cyfrą (2shop → _2shop) ani być słowem kluczowym (int, class)
        //   • java.* i javax.* są zarezerwowane dla JDK
        //       — klasa w pakiecie java.cos → SecurityException: Prohibited package name
        //   • od ogółu do szczegółu: com → example → sklep → zamowienia
        // DOBRA PRAKTYKA: jeden pakiet = jedna dziedzina (zamowienia, platnosci), a nie „wszystkie
        // kontrolery tu, wszystkie encje tam” — łatwiej wtedy chronić szczegóły przez package-private.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • package x.y;  = pierwsza linia pliku; musi pasować do folderu x/y/.
     *   • Pełna nazwa = pakiet.Klasa (java.util.ArrayList); import = skrót nazwy, nic nie kosztuje.
     *   • java.lang.* importuje się sam. import static = składowe static bez nazwy klasy.
     *   • Konflikt nazw → jedna klasa z importu, druga pełną nazwą (java.awt.List).
     *   • public > protected > (brak = package-private) > private.
     *   • Podpakiet to OSOBNY pakiet — żadnych przywilejów.
     *   • Klasa najwyższego poziomu: tylko public albo (brak). Jedna public na plik, nazwa = nazwa pliku.
     *   • Nazwy pakietów: małe litery, odwrócona domena (com.firma.app), bez myślników; nie java.*.
     *
     * PYTANIA KONTROLNE:
     *   1. Co wypisze:  System.out.println(java.util.ArrayList.class.getPackageName());
     *   2. Czy import java.util.*; spowalnia program albo „ładuje” wszystkie klasy z java.util?
     *   3. ZNAJDŹ BŁĄD:  import java.util.List;  import java.awt.List;
     *   4. Metoda bez modyfikatora w klasie z pakietu helpers. Czy lekcja z pakietu t06_oop_basics
     *      może ją wywołać? A klasa z pakietu helpers.model?
     *   5. Co jest szersze: protected czy brak modyfikatora? Dlaczego?
     *   6. ZNAJDŹ BŁĄD (plik src/sklep/Order.java):  package com.moja-firma.Sklep;  private class Order { }
     *   7. Co wypisze:  Class.forName("String")  — i dlaczego?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: nazwa z pakietem", "ArrayList", () -> exercise1("java.util.ArrayList"));
        Check.equal("ćw. 1: nazwa bez pakietu", "Main", () -> exercise1("Main"));
        Check.equal("ćw. 2: pakiet ArrayList", "java.util", () -> exercise2("java.util.ArrayList"));
        Check.equal("ćw. 2: pakiet tej lekcji", "t06_oop_basics",
                () -> exercise2("t06_oop_basics.Oop08PackagesAccess"));
        Check.equal("ćw. 2: pakiet domyślny", "", () -> exercise2("Main"));
        Check.equal("ćw. 3: odwrócona domena", "com.example.sklep.zamowienia",
                () -> exercise3("sklep.example.com", "Zamowienia"));
        Check.equal("ćw. 3: myślnik i wielkie litery", "pl.moja_firma.magazyn",
                () -> exercise3("Moja-Firma.pl", "magazyn"));
        Check.equal("ćw. 4: poprawne nazwy", true,
                () -> exercise4("com.example.shop") && exercise4("pl.firma.sklep_online2"));
        Check.equal("ćw. 4: wielka litera", false, () -> exercise4("Com.example"));
        Check.equal("ćw. 4: pusty człon / cyfra na początku", false,
                () -> exercise4("com..shop") || exercise4("com.2shop"));
        Check.equal("ćw. 4: myślnik / polska litera / java.*", false,
                () -> exercise4("com.moja-firma") || exercise4("pl.sklep.zamówienia") || exercise4("java.util.extra"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec): nazwa z pakietem", "ArrayList", () -> solution1("java.util.ArrayList"));
        Check.equal("ćw. 1 (wzorzec): nazwa bez pakietu", "Main", () -> solution1("Main"));
        Check.equal("ćw. 2 (wzorzec): pakiet ArrayList", "java.util", () -> solution2("java.util.ArrayList"));
        Check.equal("ćw. 2 (wzorzec): pakiet tej lekcji", "t06_oop_basics",
                () -> solution2("t06_oop_basics.Oop08PackagesAccess"));
        Check.equal("ćw. 2 (wzorzec): pakiet domyślny", "", () -> solution2("Main"));
        Check.equal("ćw. 3 (wzorzec): odwrócona domena", "com.example.sklep.zamowienia",
                () -> solution3("sklep.example.com", "Zamowienia"));
        Check.equal("ćw. 3 (wzorzec): myślnik i wielkie litery", "pl.moja_firma.magazyn",
                () -> solution3("Moja-Firma.pl", "magazyn"));
        Check.equal("ćw. 4 (wzorzec): poprawne nazwy", true,
                () -> solution4("com.example.shop") && solution4("pl.firma.sklep_online2"));
        Check.equal("ćw. 4 (wzorzec): wielka litera", false, () -> solution4("Com.example"));
        Check.equal("ćw. 4 (wzorzec): pusty człon / cyfra na początku", false,
                () -> solution4("com..shop") || solution4("com.2shop"));
        Check.equal("ćw. 4 (wzorzec): myślnik / polska litera / java.*", false,
                () -> solution4("com.moja-firma") || solution4("pl.sklep.zamówienia") || solution4("java.util.extra"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 11 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): PRZEPISZ wyciąganie krótkiej nazwy klasy z pełnej nazwy
     * ("java.util.ArrayList" → "ArrayList", "Main" → "Main"). Stary sposób — pętla od końca:
     * <pre>{@code
     * String result = "";
     * for (int i = fullName.length() - 1; i >= 0; i--) {
     *     if (fullName.charAt(i) == '.') break;
     *     result = fullName.charAt(i) + result;
     * }
     * return result;
     * }</pre>
     * Nowy sposób: jedna linijka z lastIndexOf i substring.
     * Podpowiedź: lastIndexOf('.') zwraca -1, gdy kropki nie ma — a -1 + 1 = 0.
     */
    static String exercise1(String fullName) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 2 (łatwe): zwróć nazwę pakietu z pełnej nazwy klasy ("java.util.ArrayList" → "java.util").
     * Klasa w pakiecie domyślnym ("Main") → pusty tekst "".
     * Podpowiedź: lastIndexOf('.'); gdy wynik jest ujemny, zwróć ""; w przeciwnym razie substring(0, kropka).
     */
    static String exercise2(String fullName) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zbuduj nazwę pakietu z domeny firmy i nazwy modułu według konwencji:
     * człony domeny w ODWROTNEJ kolejności, małe litery, myślnik zamieniony na podkreślenie, na końcu moduł
     * małymi literami. ("sklep.example.com", "Zamowienia") → "com.example.sklep.zamowienia".
     * Podpowiedź: {@code domain.toLowerCase(Locale.ROOT).replace('-', '_').split("\\.")} (kropka w regex
     * trzeba poprzedzić \\ — t04_strings/Strings05Regex), potem pętla od końca i StringBuilder.
     */
    static String exercise3(String domain, String module) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): sprawdź, czy nazwa pakietu spełnia konwencje z sekcji 8: każdy człon
     * niepusty, zaczyna się literą a–z, zawiera tylko a–z, 0–9 i _, a cała nazwa nie zaczyna się od "java.".
     * Podpowiedź: {@code split("\\.", -1)} — limit -1 zachowuje puste człony (np. "com..shop");
     * znaki sprawdzaj zakresami ({@code c >= 'a' && c <= 'z'}), bo Character.isLetter przepuściłby „ó”.
     */
    static boolean exercise4(String packageName) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(String fullName) {
        return fullName.substring(fullName.lastIndexOf('.') + 1);
    }

    static String solution2(String fullName) {
        int lastDot = fullName.lastIndexOf('.');
        return lastDot < 0 ? "" : fullName.substring(0, lastDot);
    }

    static String solution3(String domain, String module) {
        String[] parts = domain.toLowerCase(Locale.ROOT).replace('-', '_').split("\\.");
        StringBuilder result = new StringBuilder();
        for (int i = parts.length - 1; i >= 0; i--) {
            result.append(parts[i]).append('.');
        }
        return result.append(module.toLowerCase(Locale.ROOT)).toString();
    }

    static boolean solution4(String packageName) {
        if (packageName.startsWith("java.")) {
            return false;
        }
        for (String part : packageName.split("\\.", -1)) {
            if (part.isEmpty() || part.charAt(0) < 'a' || part.charAt(0) > 'z') {
                return false;
            }
            for (char c : part.toCharArray()) {
                boolean allowed = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_';
                if (!allowed) {
                    return false;
                }
            }
        }
        return true;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. java.util
     *   2. Nie. import to informacja tylko dla kompilatora („krótka nazwa X oznacza pakiet.X”). Klasy są
     *      ładowane dopiero, gdy program ich naprawdę użyje. Gwiazdka jest odradzana z powodu
     *      czytelności i ryzyka konfliktów nazw, a nie wydajności.
     *   3. Dwa importy tej samej krótkiej nazwy List → błąd kompilacji. Zaimportuj jedną, drugą pisz pełną
     *      nazwą (java.awt.List).
     *   4. Nie i nie. Brak modyfikatora = tylko ten sam pakiet (helpers). t06_oop_basics to inny pakiet,
     *      a helpers.model — mimo nazwy — też jest OSOBNYM pakietem.
     *   5. protected. Daje wszystko to, co package-private (cały pakiet), plus dostęp dla podklas
     *      w innych pakietach.
     *   6. Trzy błędy: myślnik i wielka litera w nazwie pakietu (com.moja_firma.sklep), pakiet nie
     *      pasuje do folderu src/sklep/, a klasa najwyższego poziomu nie może być private.
     *   7. Rzuci ClassNotFoundException: String — JVM szuka po PEŁNEJ nazwie; poprawnie "java.lang.String".
     *      Automatyczny import java.lang działa tylko w kodzie źródłowym, dla kompilatora.
     */
    // </editor-fold>
}
