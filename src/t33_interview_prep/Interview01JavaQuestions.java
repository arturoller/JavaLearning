package t33_interview_prep;

import helpers.Check;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Pytania o język Java i platformę — 25 kart rozmowy kwalifikacyjnej
 *        (interview = rozmowa kwalifikacyjna; card = karta; proof = dowód)
 *
 * W SKRÓCIE:
 *   Na rozmowie na juniora Javy najczęściej pada kilkanaście "klasyków": == kontra equals, pula napisów, cache Integer,
 *   przekazywanie przez wartość, wyjątki, interfejs kontra klasa abstrakcyjna, erasure generyków, pamięć JVM.
 *   Każda karta w tej lekcji ma: pytanie, odpowiedź wzorcową i kod, który ją UDOWADNIA wydrukiem.
 *
 * ANALOGIA: nauka do egzaminu ustnego. Samo wykucie odpowiedzi to ściąga, którą egzaminator rozpozna po trzecim
 *   pytaniu "a dlaczego?". Gdy jednak sam uruchomisz kod i zobaczysz wynik, odpowiadasz pewnie — bo to widziałeś.
 *
 * JAK TO DZIAŁA:
 *   Poziom karty:  ★ = pytanie na rozgrzewkę (musisz znać), ★★ = pytanie standardowe, ★★★ = pytanie z haczykiem.
 *   Układ karty:   KARTA n ★ pytanie (→ lekcja z pełnym wyjaśnieniem), ODPOWIEDŹ w 2–6 liniach, potem dowód z WYNIK.
 *   Jak odpowiadać: najpierw zdanie-teza (np. "Java zawsze przekazuje przez wartość"), potem uzasadnienie,
 *   na końcu jeden przykład lub pułapka. Jeśli nie wiesz — powiedz, jak byś to sprawdził (dokumentacja, mały eksperyment).
 *
 * SŁÓWKA:
 *   JDK = Java Development Kit (zestaw programisty); JRE = Java Runtime Environment (środowisko uruchomieniowe);
 *   JVM = Java Virtual Machine (maszyna wirtualna); bytecode = kod bajtowy; wrapper = typ opakowujący;
 *   autoboxing = automatyczne opakowanie; pool = pula; erasure = wymazywanie (typów); heap = sterta; stack = stos;
 *   garbage collector = zbieracz nieużytków (odśmiecacz); shallow = płytki; unchecked = niesprawdzany.
 *
 * ZOBACZ TEŻ: t33_interview_prep/Interview02OopCollections (OOP i kolekcje), t33_interview_prep/Interview03CodingTasks
 *   (zadania), t24_algorithms/Algorithms01Complexity (złożoność), t26_jvm/Jvm01Memory (pamięć)
 * </pre>
 */
public class Interview01JavaQuestions {

    public static void main(String[] args) {
        title("Interview01 — pytania o Javę (25 kart)");

        platform();            // platform = platforma (karty 1–2)
        typesAndBoxing();      // types and boxing = typy i opakowywanie (karty 3–4)
        stringsCards();        // strings = napisy (karty 5–7)
        keywordsCards();       // keywords = słowa kluczowe (karty 8–9)
        methodsAndErrors();    // methods and errors = metody i błędy (karty 10–12)
        oopCards();            // OOP cards = karty o obiektowości (karty 13–16)
        modernCards();         // modern = nowoczesna Java (karty 17–18)
        genericsLambdas();     // generics, lambdas = generyki i lambdy (karty 19–22)
        jvmCards();            // JVM cards = karty o JVM (karty 23–24)
        trapCards();           // trap cards = pytania-pułapki (karta 25)
        exercises();           // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PLATFORMA JAVY I KOMPILACJA
    // =================================================================================================

    /**
     * 1. Karty 1–2: czym są JDK, JRE i JVM oraz czym różni się błąd kompilacji od błędu w czasie działania.
     */
    static void platform() {
        section("1. Platforma: JDK, JRE, JVM, kompilacja kontra uruchomienie");

        // ---- KARTA 1 ★ Czym różnią się JDK, JRE i JVM?   (→ t01_basics/Basics01HelloJvm)
        // ODPOWIEDŹ: JVM wykonuje bytecode (kod bajtowy, plik .class) — to ona jest "wirtualnym komputerem".
        //   JRE = JVM + biblioteki standardowe (java.base itd.), czyli to, co potrzeba do URUCHOMIENIA programu.
        //   JDK = JRE + narzędzia programisty (javac = kompilator, jar, javadoc, jshell...), czyli to, co potrzeba do PISANIA.
        //   Od Javy 11 Oracle nie dostarcza osobnego JRE — używa się JDK (lub własnego obrazu zrobionego narzędziem jlink).
        //   "Napisz raz, uruchom wszędzie": javac zamienia .java na bytecode, a każdy system ma własną JVM, która go rozumie.
        show("wersja Javy (feature)", Runtime.version().feature());
        // WYNIK: wersja Javy (feature) → 17
        show("początek każdego pliku .class (magic number)", classMagic());
        // WYNIK: początek każdego pliku .class (magic number) → CAFEBABE
        // Każdy plik .class zaczyna się od liczby 0xCAFEBABE; zaraz za nią jest numer wersji formatu (61 = Java 17).

        // ---- KARTA 2 ★ Błąd kompilacji a błąd w czasie działania — przykłady?   (→ t10_exceptions/Exceptions01Basics)
        // ODPOWIEDŹ: błąd kompilacji wykrywa javac PRZED uruchomieniem (brak średnika, zły typ, niezainicjowana zmienna,
        //   niezłapany wyjątek sprawdzany) — program w ogóle nie powstaje. Błąd wykonania (runtime) pojawia się dopiero
        //   w trakcie działania: dzielenie przez zero, NullPointerException, ClassCastException, indeks poza tablicą.
        //   Rzutowanie (Integer) na Object-u, który jest napisem, KOMPILUJE SIĘ — kompilator ufa programiście.
        int zero = Integer.parseInt("0");
        expectThrows("10 / zero (zero znane dopiero w czasie działania)", () -> {
            int wynik = 10 / zero;
            System.out.println(wynik);
        });
        // WYNIK: ✔ 10 / zero (zero znane dopiero w czasie działania) → rzucono ArithmeticException: / by zero
        Object napis = "tekst";
        thrown("rzutowanie napisu na Integer", () -> {
            Integer zly = (Integer) napis;
            System.out.println(zly);
        });
        // WYNIK: rzutowanie napisu na Integer → rzucono ClassCastException
        // DOBRA PRAKTYKA: na rozmowie wymień jedną rzecz z KAŻDEJ grupy. Błędy kompilacji są "tanie" (od razu je widać),
        //   dlatego dobry projekt przenosi jak najwięcej kontroli do kompilatora (typy, generyki, Optional, sealed).
    }

    // =================================================================================================
    // 2. PRYMITYWY, OPAKOWANIA I CACHE INTEGER
    // =================================================================================================

    /**
     * 2. Karty 3–4: prymitywy kontra wrappery oraz słynny cache Integer od -128 do 127.
     */
    static void typesAndBoxing() {
        section("2. Prymitywy, wrappery i cache Integer");

        // ---- KARTA 3 ★ Prymityw a wrapper — różnice?   (→ t01_basics/Basics06Wrappers)
        // ODPOWIEDŹ: prymityw (int, double, boolean...) to sama wartość: nie jest obiektem, nie może być null, ma wartość
        //   domyślną (0, false) i jest szybki. Wrapper (Integer, Double, Boolean...) to OBIEKT: może być null, ma metody
        //   i jest wymagany w kolekcjach i generykach (List<Integer>, nie List<int>). Koszt: dodatkowa pamięć i
        //   odwołanie do sterty. Odpakowanie (unboxing) null-a rzuca NullPointerException.
        Defaults d = new Defaults();
        show("domyślne pole int / Integer", d.prymityw + " / " + d.wrapper);
        // WYNIK: domyślne pole int / Integer → 0 / null
        Integer pusty = null;
        thrown("odpakowanie null do int", () -> {
            int x = pusty;
            System.out.println(x);
        });
        // WYNIK: odpakowanie null do int → rzucono NullPointerException
        // PUŁAPKA: wyrażenie  cond ? 1 : pusty  — gdy jeden operand to int, a drugi Integer, wynik jest typu int,
        //   więc null zostanie odpakowany i poleci NPE, choć "nikt nie wołał żadnej metody". Dlaczego: reguły typu operatora ?:.
        //   Uwaga: liczba też bywa kosztowna — suma w pętli po zmiennej typu Long (nie long) tworzy miliony obiektów.

        // ---- KARTA 4 ★★ Dlaczego Integer 127 == 127, ale 128 != 128?   (→ t01_basics/Basics06Wrappers)
        // ODPOWIEDŹ: autoboxing (automatyczne opakowanie) wywołuje Integer.valueOf(int). Ta metoda zwraca OBIEKT Z CACHE
        //   (tablicy gotowych obiektów) dla wartości od -128 do 127; poza zakresem tworzy nowy obiekt. Operator == porównuje
        //   REFERENCJE, więc dla małych liczb to ten sam obiekt, a dla dużych — dwa różne. Do porównywania wartości
        //   zawsze equals. Podobny cache mają Long, Short, Byte (-128..127), Character (0..127) i Boolean.
        //   Górną granicę cache Integera można zmienić opcją JVM -XX:AutoBoxCacheMax, więc nigdy nie polegaj na == !
        Integer a = 127;
        Integer b = 127;
        Integer c = 128;
        Integer dd = 128;
        show("127 == 127", a == b);
        // WYNIK: 127 == 127 → true
        show("128 == 128", c == dd);
        // WYNIK: 128 == 128 → false
        show("128 equals 128", c.equals(dd));
        // WYNIK: 128 equals 128 → true
        show("Integer.valueOf(127) == Integer.valueOf(127)", Integer.valueOf(127) == Integer.valueOf(127));
        // WYNIK: Integer.valueOf(127) == Integer.valueOf(127) → true
        Long l1 = 127L;
        Long l2 = 127L;
        show("Long 127L == 127L", l1 == l2);
        // WYNIK: Long 127L == 127L → true
        show("Long equals Integer (127L kontra 127)", l1.equals(127));
        // WYNIK: Long equals Integer (127L kontra 127) → false
        // PUŁAPKA: Long.equals(Integer) jest false, bo equals sprawdza też TYP obiektu. Literał 127 to int, który
        //   zostaje opakowany do Integer, a nie do Long. Dlatego  new Long(5).equals(5)  też daje false.
        // DOBRA PRAKTYKA: wrappery porównuj przez equals lub Objects.equals (obsługuje null); konstruktor  new Integer(5)
        //   jest przestarzały (deprecated, do usunięcia) — używaj Integer.valueOf albo autoboxingu.
    }

    // =================================================================================================
    // 3. NAPISY: NIEZMIENNOŚĆ, PULA, STRINGBUILDER
    // =================================================================================================

    /**
     * 3. Karty 5–7: == kontra equals na napisach, niezmienność i pula napisów, StringBuilder kontra StringBuffer.
     */
    static void stringsCards() {
        section("3. Napisy: equals, pula, StringBuilder");

        // ---- KARTA 5 ★ == kontra equals?   (→ t04_strings/Strings01Basics)
        // ODPOWIEDŹ: == porównuje REFERENCJE (czy to ten sam obiekt w pamięci), equals porównuje ZAWARTOŚĆ — o ile klasa
        //   nadpisała equals (String, Integer, List, rekordy tak; zwykła klasa dziedziczy z Object równość referencji).
        //   Dla prymitywów == porównuje wartości. Literały napisowe trafiają do puli, więc "java" == "java" bywa true —
        //   to wyjątek wynikający z implementacji, nie reguła do wykorzystania.
        String s1 = "java";
        String s2 = "java";
        String s3 = new String("java");
        show("literał == literał", s1 == s2);
        // WYNIK: literał == literał → true
        show("literał == new String", s1 == s3);
        // WYNIK: literał == new String → false
        show("literał equals new String", s1.equals(s3));
        // WYNIK: literał equals new String → true
        String nic = null;
        thrown("nic.equals(\"a\") — wołanie metody na null", () -> nic.equals("a"));
        // WYNIK: nic.equals("a") — wołanie metody na null → rzucono NullPointerException
        show("\"a\".equals(nic) — bezpieczna kolejność", "a".equals(nic));
        // WYNIK: "a".equals(nic) — bezpieczna kolejność → false
        // DOBRA PRAKTYKA: stały napis po lewej ("a".equals(x)) albo Objects.equals(x, y) — nie rzuca NPE dla null.

        // ---- KARTA 6 ★★ Dlaczego String jest niezmienny (immutable) i co to jest pula napisów?   (→ t04_strings/Strings01Basics)
        // ODPOWIEDŹ: obiekt String nigdy się nie zmienia — metody takie jak toUpperCase zwracają NOWY napis. Powody:
        //   (1) bezpieczeństwo (ścieżki, nazwy klas, hasła nie mogą zmienić się po sprawdzeniu), (2) bezpieczeństwo wątków
        //   bez synchronizacji, (3) hashCode można policzyć raz i zapamiętać — dlatego String jest świetnym kluczem mapy,
        //   (4) możliwość współdzielenia w puli napisów (string pool) — od Javy 7 leży ona na stercie (heap).
        //   Od Javy 9 napisy z samymi znakami Latin-1 zajmują 1 bajt na znak (compact strings).
        String ala = "ala";
        ala.toUpperCase(Locale.ROOT);
        show("po toUpperCase bez przypisania", ala);
        // WYNIK: po toUpperCase bez przypisania → ala
        String czesc = "ja";
        String sklejony = czesc + "va";
        final String stalaCzesc = "ja";
        String sklejonyStala = stalaCzesc + "va";
        show("zmienna + literał == \"java\"", sklejony == "java");
        // WYNIK: zmienna + literał == "java" → false
        show("stała (final) + literał == \"java\"", sklejonyStala == "java");
        // WYNIK: stała (final) + literał == "java" → true
        show("intern() zwraca napis z puli", sklejony.intern() == "java");
        // WYNIK: intern() zwraca napis z puli → true
        // Dlaczego różnica? Kompilator widzi, że "ja" + "va" to stała i skleja ją już przy kompilacji (wynik trafia do puli).
        // Zwykła zmienna jest znana dopiero w czasie działania — powstaje nowy obiekt poza pulą.
        // PUŁAPKA: sklejanie w pętli (s += x) tworzy nowy napis w każdym obrocie — koszt rośnie kwadratowo (O(n²)).
        //   Użyj StringBuilder (karta 7).

        // ---- KARTA 7 ★ StringBuilder, StringBuffer i String — kiedy który?   (→ t04_strings/Strings03StringBuilder)
        // ODPOWIEDŹ: String — niezmienny, do wartości i kluczy. StringBuilder — zmienny, szybki, NIE jest bezpieczny
        //   wątkowo: standardowy wybór do budowania tekstu w jednym wątku. StringBuffer — to samo, ale metody są
        //   synchronizowane (wolniejszy); dziś rzadko potrzebny, bo budowanie napisu współdzielonego między wątkami
        //   to zwykle zły pomysł. Ani StringBuilder, ani StringBuffer nie nadpisują equals.
        StringBuilder sb = new StringBuilder("ab");
        sb.append("c").reverse();
        show("append + reverse", sb);
        // WYNIK: append + reverse → cba
        StringBuilder x1 = new StringBuilder("a");
        StringBuilder x2 = new StringBuilder("a");
        show("StringBuilder.equals (to samo wejście)", x1.equals(x2));
        // WYNIK: StringBuilder.equals (to samo wejście) → false
        show("porównanie przez toString().equals", x1.toString().equals(x2.toString()));
        // WYNIK: porównanie przez toString().equals → true
        // DOBRA PRAKTYKA: pojedyncze sklejenie "a" + b + "c" zostaw kompilatorowi (jest szybkie), pętlę zamień na StringBuilder
        //   lub String.join / Collectors.joining.
    }

    // =================================================================================================
    // 4. FINAL, FINALLY, FINALIZE, STATIC
    // =================================================================================================

    /**
     * 4. Karty 8–9: trzy podobne słowa (final, finally, finalize) oraz co naprawdę znaczy static.
     */
    static void keywordsCards() {
        section("4. final / finally / finalize i static");

        // ---- KARTA 8 ★ Czym różnią się final, finally i finalize?   (→ t10_exceptions/Exceptions01Basics)
        // ODPOWIEDŹ: final — modyfikator: zmienna przypisana raz, metoda bez nadpisania, klasa bez dziedziczenia.
        //   Dla referencji final znaczy "nie zmienię TEJ referencji", a nie "obiekt jest niezmienny".
        //   finally — blok po try/catch wykonywany prawie zawsze (po return, po wyjątku); nie wykona się np. przy
        //   System.exit lub awarii JVM. finalize() — metoda Object wołana przez GC przed usunięciem obiektu; jest
        //   przestarzała od Javy 9 (do usunięcia od Javy 18) — zamiast niej: try-with-resources lub Cleaner.
        final List<String> lista = new ArrayList<>();
        lista.add("dodano mimo final");
        show("lista final, ale zmienna", lista);
        // WYNIK: lista final, ale zmienna → [dodano mimo final]
        show("kolejność try/catch/finally (bez błędu)", tryCatchFinally(false));
        // WYNIK: kolejność try/catch/finally (bez błędu) → try,finally
        show("kolejność try/catch/finally (z błędem)", tryCatchFinally(true));
        // WYNIK: kolejność try/catch/finally (z błędem) → try,catch,finally
        show("finally zmienia lokalne int po return", finallyDoesNotChangeReturn());
        // WYNIK: finally zmienia lokalne int po return → 1
        show("finally zmienia OBIEKT po return", finallyMutatesObject());
        // WYNIK: finally zmienia OBIEKT po return → a!
        // PUŁAPKA: return w try zapamiętuje wartość PRZED wykonaniem finally. Dla int późniejsza zmiana nic nie daje,
        //   ale dla referencji finally może zmienić obiekt, który wróci do wołającego. Nigdy nie pisz return ani throw
        //   w finally — połyka wyjątek z try (kompilator ostrzega: finally clause cannot complete normally).

        // ---- KARTA 9 ★ Co oznacza static?   (→ t06_oop_basics/Oop04Static)
        // ODPOWIEDŹ: element należy do KLASY, nie do obiektu: jedna kopia pola na całą aplikację (wczytanie klasy),
        //   metoda statyczna nie ma this, więc nie widzi pól instancji. Metody statyczne nie są polimorficzne —
        //   nie można ich nadpisać, tylko ZASŁONIĆ (hiding); wybór następuje po typie zmiennej, nie obiektu.
        //   Statyczne pola zmienne to współdzielony stan globalny — źródło błędów wielowątkowych i trudnych testów.
        new Counter();
        new Counter();
        show("licznik statyczny po dwóch obiektach", Counter.created);
        // WYNIK: licznik statyczny po dwóch obiektach → 2
        Animal zwierze = new Dog();
        @SuppressWarnings("static")
        String rodzaj = zwierze.kind();
        show("metoda statyczna przez zmienną typu Animal", rodzaj);
        // WYNIK: metoda statyczna przez zmienną typu Animal → zwierzę
        show("metoda instancji — polimorfizm", zwierze.voice());
        // WYNIK: metoda instancji — polimorfizm → hau
        // DOBRA PRAKTYKA: metody statyczne wołaj przez nazwę klasy (Animal.kind()), bo zapis przez zmienną wprowadza w błąd.
    }

    // =================================================================================================
    // 5. PRZEKAZYWANIE PRZEZ WARTOŚĆ, WYJĄTKI, TRY-WITH-RESOURCES
    // =================================================================================================

    /**
     * 5. Karty 10–12: Java zawsze przekazuje przez wartość, wyjątki sprawdzane kontra niesprawdzane, zamykanie zasobów.
     */
    static void methodsAndErrors() {
        section("5. Przekazywanie parametrów, wyjątki, try-with-resources");

        // ---- KARTA 10 ★★ Czy Java przekazuje obiekty przez referencję?   (→ t01_basics/Basics09PassByValue)
        // ODPOWIEDŹ: NIE. Java zawsze przekazuje przez WARTOŚĆ. Dla obiektów wartością jest KOPIA referencji (adresu).
        //   Metoda dostaje własną kopię "pilota" do tego samego telewizora: może zmienić obiekt (zmiana widoczna na zewnątrz),
        //   ale przypisanie parametrowi nowego obiektu nie zmieni zmiennej wołającego. Dlatego nie da się napisać swap(a, b).
        int liczba = 5;
        increment(liczba);
        show("int po increment(n)", liczba);
        // WYNIK: int po increment(n) → 5
        StringBuilder bufor = new StringBuilder("stary");
        reassign(bufor);
        show("po reassign (nowy obiekt w parametrze)", bufor);
        // WYNIK: po reassign (nowy obiekt w parametrze) → stary
        mutate(bufor);
        show("po mutate (zmiana tego samego obiektu)", bufor);
        // WYNIK: po mutate (zmiana tego samego obiektu) → stary!
        Integer left = 1;
        Integer right = 2;
        swap(left, right);
        show("po swap(left, right)", left + " i " + right);
        // WYNIK: po swap(left, right) → 1 i 2
        // PUŁAPKA: "przekazywanie przez referencję" znaczyłoby, że metoda dostaje ALIAS zmiennej (jak & w C++).
        //   Tego w Javie nie ma. Jeśli rozmówca upiera się inaczej, spokojnie pokaż swap — dowód jest w kodzie.

        // ---- KARTA 11 ★ Wyjątki sprawdzane (checked) kontra niesprawdzane (unchecked)?   (→ t10_exceptions/Exceptions02CheckedUnchecked)
        // ODPOWIEDŹ: checked = podklasy Exception, które NIE są RuntimeException (np. IOException, SQLException): kompilator
        //   wymusza try/catch albo throws, bo to błędy "spodziewane" z zewnątrz (plik, sieć). Unchecked = RuntimeException
        //   i jej podklasy (NPE, IllegalArgumentException, IllegalStateException) oraz Error: błędy programisty
        //   lub awarie, których nie da się sensownie obsłużyć. Współczesny styl (np. Spring) preferuje unchecked.
        show("nadklasa Exception", Exception.class.getSuperclass().getSimpleName());
        // WYNIK: nadklasa Exception → Throwable
        show("nadklasa IllegalArgumentException", IllegalArgumentException.class.getSuperclass().getSimpleName());
        // WYNIK: nadklasa IllegalArgumentException → RuntimeException
        show("nadklasa Error", Error.class.getSuperclass().getSimpleName());
        // WYNIK: nadklasa Error → Throwable
        show("IOException jest RuntimeException", RuntimeException.class.isAssignableFrom(IOException.class));
        // WYNIK: IOException jest RuntimeException → false
        show("IOException jest Exception", Exception.class.isAssignableFrom(IOException.class));
        // WYNIK: IOException jest Exception → true
        // DOBRA PRAKTYKA: nie łap Exception ani Throwable "na zapas" i nie połykaj wyjątku pustym catch — zaloguj lub
        //   przekaż dalej z przyczyną (new X("opis", e)). Bloki catch układaj od najbardziej szczegółowego (inaczej błąd kompilacji).

        // ---- KARTA 12 ★★ Co daje try-with-resources?   (→ t10_exceptions/Exceptions04TryWithResources)
        // ODPOWIEDŹ: zasoby deklarowane w nawiasie try (klasy AutoCloseable) są zamykane automatycznie, w ODWROTNEJ kolejności
        //   niż otwarte, nawet po wyjątku. Jeśli wyjątek poleci i z ciała, i z close(), "wygrywa" wyjątek z ciała, a ten z close()
        //   trafia do tablicy getSuppressed() (wyjątki stłumione). Stary try/finally gubił pierwszy wyjątek.
        //   Od Javy 9 można podać efektywnie finalną zmienną: try (r) {...}.
        List<String> log = new ArrayList<>();
        try (Res a = new Res("A", log, false); Res b = new Res("B", log, false)) {
            log.add("użycie " + a.name + b.name);
        }
        show("kolejność zdarzeń", log);
        // WYNIK: kolejność zdarzeń → [otwarto A, otwarto B, użycie AB, zamknięto B, zamknięto A]
        List<String> log2 = new ArrayList<>();
        try (Res r = new Res("R", log2, true)) {
            throw new IllegalStateException("w ciele " + r.name);
        } catch (IllegalStateException e) {
            show("wyjątek główny", e.getMessage());
            // WYNIK: wyjątek główny → w ciele R
            show("wyjątek stłumiony", e.getSuppressed()[0].getMessage());
            // WYNIK: wyjątek stłumiony → przy zamykaniu R
        }
    }

    // =================================================================================================
    // 6. OOP: PRZECIĄŻANIE, MODYFIKATORY, INTERFEJSY, REKORDY
    // =================================================================================================

    /**
     * 6. Karty 13–16: przeciążanie kontra nadpisywanie, modyfikatory dostępu, interfejs kontra klasa abstrakcyjna, rekordy.
     */
    static void oopCards() {
        section("6. Przeciążanie, dostęp, interfejsy, rekordy");

        // ---- KARTA 13 ★★ Przeciążanie (overloading) kontra nadpisywanie (overriding)?   (→ t05_methods/Methods02Overloading)
        // ODPOWIEDŹ: przeciążanie = ta sama nazwa, INNE parametry, w jednej klasie; wybór metody robi KOMPILATOR po
        //   typach zmiennych (wiązanie wczesne). Nadpisywanie = ta sama sygnatura w podklasie; wybór robi JVM po
        //   RZECZYWISTYM typie obiektu (wiązanie późne = polimorfizm). Samo inny typ zwracany nie przeciąża metody.
        //   Zobacz też t07_inheritance_polymorphism/Inherit02Override.
        Animal zwierze = new Dog();
        show("nadpisywanie: voice()", zwierze.voice());
        // WYNIK: nadpisywanie: voice() → hau
        show("przeciążanie: describe(Animal) po typie zmiennej", describe(zwierze));
        // WYNIK: przeciążanie: describe(Animal) po typie zmiennej → describe(Animal)
        show("przeciążanie: describe(Dog)", describe(new Dog()));
        // WYNIK: przeciążanie: describe(Dog) → describe(Dog)
        show("f(\"tekst\") / f(Object) / f(null)", f("tekst") + " / " + f((Object) "tekst") + " / " + f(null));
        // WYNIK: f("tekst") / f(Object) / f(null) → String / Object / String
        // PUŁAPKA: przy f(null) kompilator wybiera NAJBARDZIEJ szczegółową metodę (String), a gdy dwie są równie
        //   szczegółowe (np. String i Integer) — zgłasza błąd niejednoznaczności. Dla zmiennej typu Object wybrałby f(Object).

        // ---- KARTA 14 ★ Modyfikatory dostępu?   (→ t06_oop_basics/Oop08PackagesAccess)
        // ODPOWIEDŹ (od najwęższego do najszerszego):
        //   private        — tylko ta klasa (nie podklasy, nie pakiet)
        //   (domyślny)     — cały pakiet (nie podklasy z innego pakietu)
        //   protected      — pakiet + podklasy (również z innych pakietów)
        //   public         — wszędzie
        //   Klasa najwyższego poziomu może być tylko public lub pakietowa. Metody interfejsu są domyślnie public.
        //   Zasada: najpierw private, poszerzaj tylko gdy trzeba (hermetyzacja = ukrywanie szczegółów).
        // Tu dowodem jest kompilator: zmień w kodzie private na wywołanie spoza klasy, a dostaniesz błąd kompilacji.
        note("(karta 14 bez wydruku — zasada sprawdzana przez kompilator)");
        // WYNIK: ℹ (karta 14 bez wydruku — zasada sprawdzana przez kompilator)

        // ---- KARTA 15 ★★ Interfejs kontra klasa abstrakcyjna (po Javie 8)?   (→ t07_inheritance_polymorphism/Inherit04Interfaces)
        // ODPOWIEDŹ: klasa abstrakcyjna może mieć STAN (pola), konstruktory i dowolne modyfikatory, ale dziedziczy się
        //   z jednej. Interfejs to KONTRAKT: klasa implementuje wiele interfejsów. Od Javy 8 interfejs ma metody default
        //   (z ciałem) i static, od Javy 9 prywatne; nadal NIE ma stanu instancji (pola są stałymi public static final).
        //   Reguła: interfejs, gdy opisujesz zdolność ("Comparable"); klasa abstrakcyjna, gdy dzielisz kod i stan blisko spokrewnionych klas.
        //   Konflikt dwóch metod default o tej samej sygnaturze kompilator każe rozwiązać jawnie (Interfejs.super.metoda()).
        show("rozwiązanie konfliktu default", new Duck().move());
        // WYNIK: rozwiązanie konfliktu default → idę i płynę
        // PUŁAPKA: "diamentowy problem" w Javie istnieje tylko dla metod default; stanu (pól) interfejs nie dziedziczy.

        // ---- KARTA 16 ★ Co to jest record i czym różni się od klasy?   (→ t09_records/Records01Basics)
        // ODPOWIEDŹ (Java 16+): zwięzła klasa na dane. Kompilator generuje konstruktor, akcesory (x(), nie getX()), equals,
        //   hashCode i toString. Rekord jest final, jego pola są final, nie dziedziczy po innej klasie (może implementować interfejsy).
        //   Niezmienność jest PŁYTKA: jeśli komponent to zmienna lista, rekord nie zrobi jej kopii — zrób to w konstruktorze kompaktowym.
        show("toString rekordu", new Point(1, 2));
        // WYNIK: toString rekordu → Point[x=1, y=2]
        show("equals rekordów po wartości", new Point(1, 2).equals(new Point(1, 2)));
        // WYNIK: equals rekordów po wartości → true
        show("Point.class.isRecord()", Point.class.isRecord());
        // WYNIK: Point.class.isRecord() → true
        List<String> zrodlo = new ArrayList<>(List.of("a"));
        Team team = new Team("zespół", zrodlo);
        SafeTeam bezpieczny = new SafeTeam("zespół", zrodlo);
        zrodlo.add("b");
        show("rekord z płytką kopią (zmienił się)", team.members());
        // WYNIK: rekord z płytką kopią (zmienił się) → [a, b]
        show("rekord z List.copyOf (nie zmienił się)", bezpieczny.members());
        // WYNIK: rekord z List.copyOf (nie zmienił się) → [a]
        // DOBRA PRAKTYKA: komponenty kolekcyjne kopiuj w konstruktorze kompaktowym przez List.copyOf (niezmienna kopia).
    }

    // =================================================================================================
    // 7. NOWOCZESNA JAVA: VAR I SWITCH
    // =================================================================================================

    /**
     * 7. Karty 17–18: słowo var (Java 10) oraz wyrażenie switch (Java 14).
     */
    static void modernCards() {
        section("7. var i switch jako wyrażenie");

        // ---- KARTA 17 ★ Czym jest var? Czy Java stała się dynamicznie typowana?   (→ t23_modern_java/Modern02Var)
        // ODPOWIEDŹ: var (Java 10) to wnioskowanie typu zmiennej LOKALNEJ przez KOMPILATOR w chwili kompilacji. Zmienna ma
        //   nadal jeden stały typ statyczny — po prostu go nie wypisujesz. Nie wolno użyć var dla pól, parametrów, wartości zwracanej
        //   ani przy przypisaniu null, tablicy {..} lub lambdy bez typu docelowego. var to nie słowo kluczowe, tylko
        //   zarezerwowana nazwa typu (nadal możesz nazwać zmienną var).
        var wynik = 10 / 4;
        show("var wynik = 10 / 4 (to int)", wynik);
        // WYNIK: var wynik = 10 / 4 (to int) → 2
        var mieszana = new ArrayList<>();
        mieszana.add(1);
        mieszana.add("a");
        show("var + diamond → ArrayList of Object", mieszana);
        // WYNIK: var + diamond → ArrayList of Object → [1, a]
        // Wniosek: w  var x = new ArrayList<>()  typ elementu to Object, dlatego zapisz  new ArrayList<String>()  albo podaj typ jawnie.
        // DOBRA PRAKTYKA: var używaj, gdy typ widać z prawej strony (var user = new User()), unikaj gdy czytelność cierpi (var x = get()).

        // ---- KARTA 18 ★★ Switch jako wyrażenie — co zmienia?   (→ t23_modern_java/Modern03SwitchExpressions)
        // ODPOWIEDŹ: (Java 14) switch zwraca wartość, używa strzałek (->), NIE ma "przechodzenia" do kolejnego case (fall-through),
        //   pozwala na kilka etykiet (case A, B ->) i słowo yield w bloku. Musi być WYCZERPUJĄCY: dla enuma bez default kompilator
        //   wymusza wszystkie stałe — dodanie nowej stałej daje błąd kompilacji w każdym takim switchu (to zaleta).
        //   Dopasowanie wzorców w switch (case Circle c ->) jest w Javie 17 tylko podglądem; finalne dopiero w Javie 21+.
        show("stary switch (fall-through): oldSwitch(LOW)", oldSwitch(Level.LOW));
        // WYNIK: stary switch (fall-through): oldSwitch(LOW) → średni
        show("score(LOW)", score(Level.LOW));
        // WYNIK: score(LOW) → 1
        show("score(HIGH) — blok z yield", score(Level.HIGH));
        // WYNIK: score(HIGH) — blok z yield → 10
        // PUŁAPKA: w starym switchu brak break powoduje wykonanie następnego case — oldSwitch(LOW) dało napis ze środkowej gałęzi.
    }

    // =================================================================================================
    // 8. GENERYKI, LAMBDY, OPTIONAL, STRUMIENIE
    // =================================================================================================

    /**
     * 8. Karty 19–22: erasure generyków, zmienne efektywnie finalne, nadużycia Optional i leniwość strumieni.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static void genericsLambdas() {
        section("8. Generyki, lambdy, Optional, strumienie");

        // ---- KARTA 19 ★★★ Co to jest type erasure (wymazywanie typów)?   (→ t11_generics/Generics06ErasureLimits)
        // ODPOWIEDŹ: generyki istnieją tylko w czasie KOMPILACJI. Kompilator sprawdza typy, wstawia rzutowania, po czym
        //   "wymazuje" parametry typu — w bytecode List<String> i List<Integer> to ten sam List. Skutki: nie ma
        //   new T(), new T[], instanceof List<String>, a dwie metody f(List<String>) i f(List<Integer>) mają tę samą sygnaturę.
        //   Przez typ surowy (raw type) można do List<String> włożyć Integer ("zanieczyszczenie sterty"), a błąd wyjdzie dopiero
        //   przy odczycie — jako ClassCastException w miejscu odległym od przyczyny.
        List<String> strings = new ArrayList<>();
        List<Integer> ints = new ArrayList<>();
        show("klasy List<String> i List<Integer> są takie same", strings.getClass() == ints.getClass());
        // WYNIK: klasy List<String> i List<Integer> są takie same → true
        List raw = strings;
        raw.add(42);
        thrown("odczyt jako String z zanieczyszczonej listy", () -> {
            String s = strings.get(0);
            System.out.println(s);
        });
        // WYNIK: odczyt jako String z zanieczyszczonej listy → rzucono ClassCastException
        // DOBRA PRAKTYKA: nie używaj typów surowych (kompilator ostrzega: rawtypes/unchecked); gdy musisz znać typ w czasie
        //   działania, przekaż Class<T> (type token).

        // ---- KARTA 20 ★★ Dlaczego zmienna użyta w lambdzie musi być effectively final?   (→ t13_lambdas/Lambda06ClosuresScope)
        // ODPOWIEDŹ: lambda może wykonać się PÓŹNIEJ, a nawet w innym wątku, gdy metoda już dawno skończyła pracę i jej stos zniknął.
        //   Dlatego lambda dostaje KOPIĘ wartości zmiennej lokalnej; żeby kopia i oryginał się nie rozjechały,
        //   język wymaga, by zmienna nie była zmieniana (effectively final = nigdy nie przypisana ponownie). Pól obiektu
        //   to nie dotyczy (lambda trzyma this). Obejścia: tablica jednoelementowa, AtomicInteger — ale lepiej zmienić projekt (reduce, strumień).
        //   Błąd kompilacji dla  int n = 0; run(() -> n++);  brzmi: variable used in lambda should be final or effectively final.
        int[] licznik = {0};
        Runnable r = () -> licznik[0]++;
        r.run();
        r.run();
        show("licznik w tablicy (obejście)", licznik[0]);
        // WYNIK: licznik w tablicy (obejście) → 2
        AtomicInteger atomowy = new AtomicInteger();
        Runnable r2 = atomowy::incrementAndGet;
        r2.run();
        show("AtomicInteger", atomowy.get());
        // WYNIK: AtomicInteger → 1
        Owner owner = new Owner();
        show("this w lambdzie", owner.fromLambda().get());
        // WYNIK: this w lambdzie → obiekt otaczający
        show("this w klasie anonimowej", owner.fromAnonymous().get());
        // WYNIK: this w klasie anonimowej → klasa anonimowa
        // PUŁAPKA: w lambdzie this oznacza obiekt OTACZAJĄCY, w klasie anonimowej — samą tę klasę. Lambda nie tworzy nowego zakresu.

        // ---- KARTA 21 ★★ Jak źle używa się Optional?   (→ t14_optional/Optional03BestPractices)
        // ODPOWIEDŹ: Optional ma być TYPEM ZWRACANYM metody, gdy brak wyniku jest normalny. Nie używaj go w polach, parametrach
        //   ani w kolekcjach (nie jest Serializable, dodaje obiekt, a parametr i tak może być null). Nie wołaj get() bez
        //   sprawdzenia — użyj orElseThrow(), orElse, orElseGet, map. Różnica: orElse(x) oblicza x ZAWSZE (gorliwie),
        //   orElseGet(() -> x) tylko gdy brak wartości (leniwie). Optional.of(null) rzuca NPE, ofNullable(null) daje pusty.
        Optional<String> jest = Optional.of("jest");
        expensiveCalls = 0;
        jest.orElse(expensive());
        show("wywołań expensive() po orElse (jest wartość)", expensiveCalls);
        // WYNIK: wywołań expensive() po orElse (jest wartość) → 1
        jest.orElseGet(Interview01JavaQuestions::expensive);
        show("wywołań expensive() po orElseGet (jest wartość)", expensiveCalls);
        // WYNIK: wywołań expensive() po orElseGet (jest wartość) → 1
        expectThrows("Optional.empty().get()", () -> Optional.empty().get());
        // WYNIK: ✔ Optional.empty().get() → rzucono NoSuchElementException: No value present
        thrown("Optional.of(null)", () -> Optional.of(null));
        // WYNIK: Optional.of(null) → rzucono NullPointerException
        // DOBRA PRAKTYKA: pusty Optional zwracaj zamiast null, a przy odbiorze używaj orElseThrow() (Java 10+) zamiast get().

        // ---- KARTA 22 ★★ Co znaczy, że strumienie są leniwe?   (→ t16_streams/Streams16Laziness)
        // ODPOWIEDŹ: operacje pośrednie (filter, map, peek, sorted...) tylko budują opis potoku — nic się nie liczy, dopóki
        //   nie wywołasz operacji KOŃCOWEJ (toList, count, collect, findFirst...). Elementy płyną pojedynczo przez cały potok,
        //   a operacje skracające (findFirst, limit, anyMatch) przerywają przetwarzanie wcześniej. Strumień można zużyć tylko raz.
        List<String> trace = new ArrayList<>();
        Stream<Integer> potok = Stream.of(1, 2, 3).peek(x -> trace.add("peek " + x)).filter(x -> x > 1);
        show("przed operacją końcową", trace);
        // WYNIK: przed operacją końcową → []
        List<Integer> lista2 = potok.toList();
        show("po toList() (Java 16+)", trace);
        // WYNIK: po toList() (Java 16+) → [peek 1, peek 2, peek 3]
        show("wynik", lista2);
        // WYNIK: wynik → [2, 3]
        List<String> trace2 = new ArrayList<>();
        Stream.of(1, 2, 3, 4).peek(x -> trace2.add("peek " + x)).filter(x -> x > 1).findFirst();
        show("findFirst przerywa po drugim elemencie", trace2);
        // WYNIK: findFirst przerywa po drugim elemencie → [peek 1, peek 2]
        expectThrows("ponowne użycie zużytego strumienia", potok::count);
        // WYNIK: ✔ ponowne użycie zużytego strumienia → rzucono IllegalStateException: stream has already been operated upon or closed
        List<String> trace3 = new ArrayList<>();
        long ile = Stream.of(1, 2, 3).peek(x -> trace3.add("peek " + x)).count();
        show("count() na strumieniu o znanym rozmiarze: ile / ślad", ile + " / " + trace3);
        // WYNIK: count() na strumieniu o znanym rozmiarze: ile / ślad → 3 / []
        // PUŁAPKA: od Javy 9 count() może pominąć cały potok, gdy zna rozmiar źródła — peek/map z efektem ubocznym
        //   się wtedy NIE wykonają. Efekty uboczne w strumieniach to zły pomysł (peek służy do debugowania).
    }

    // =================================================================================================
    // 9. PAMIĘĆ JVM I GARBAGE COLLECTOR
    // =================================================================================================

    /**
     * 9. Karty 23–24: stos kontra sterta oraz podstawy działania odśmiecacza.
     */
    static void jvmCards() {
        section("9. Pamięć JVM i odśmiecacz");

        // ---- KARTA 23 ★★ Stos (stack) kontra sterta (heap)?   (→ t26_jvm/Jvm01Memory)
        // ODPOWIEDŹ: stos jest PRYWATNY dla wątku: każde wywołanie metody dokłada ramkę ze zmiennymi lokalnymi (prymitywy
        //   i referencje) i zdejmuje ją po powrocie — szybko, ale mały. Sterta jest WSPÓLNA dla wątków: tu leżą obiekty (także
        //   tablice i pula napisów) i sprząta je GC. Metadane klas są w Metaspace (od Javy 8, zastąpił PermGen). Skutki:
        //   zbyt głęboka rekurencja → StackOverflowError; zbyt wiele żywych obiektów → OutOfMemoryError.
        //   Zmienna lokalna typu obiektowego to referencja na stosie, a sam obiekt jest na stercie.
        depth = 0;
        try {
            recurse();
        } catch (StackOverflowError e) {
            show("złapano", e.getClass().getSimpleName());
            // WYNIK: złapano → StackOverflowError
        }
        show("głębokość rekurencji większa niż 1000", depth > 1000);
        // WYNIK: głębokość rekurencji większa niż 1000 → true
        // DOBRA PRAKTYKA: nie łap StackOverflowError w kodzie produkcyjnym — popraw rekurencję (warunek stopu, pętla, własny stos).

        // ---- KARTA 24 ★★ Jak działa garbage collector (GC)?   (→ t26_jvm/Jvm03GarbageCollection)
        // ODPOWIEDŹ: GC usuwa obiekty NIEOSIĄGALNE z "korzeni" (zmienne lokalne na stosach, pola statyczne, aktywne wątki) —
        //   nie liczy referencji, tylko sprawdza osiągalność, więc cykle A↔B nie są problemem. Większość obiektów żyje krótko,
        //   dlatego sterta jest dzielona na generacje (młoda/stara). Domyślny odśmiecacz od Javy 9 to zwykle G1;
        //   ZGC i Shenandoah dają bardzo krótkie pauzy (Java 21+: generacyjny ZGC). System.gc() to tylko prośba — nie
        //   gwarancja. "Wycieki pamięci" w Javie to obiekty wciąż osiągalne, np. w statycznej kolekcji, której nikt nie czyści.
        Object silna = new Object();
        WeakReference<Object> slaba = new WeakReference<>(silna);
        show("słaba referencja żyje, dopóki jest silna", slaba.get() != null);
        // WYNIK: słaba referencja żyje, dopóki jest silna → true
        Reference.reachabilityFence(silna);
        // Reference.reachabilityFence (Java 9+) = "ten obiekt ma być uznany za osiągalny aż do tego miejsca".
        // Co by się stało po  silna = null; System.gc();  — zwykle słaba referencja zostanie wyczyszczona, ale nie ma
        // gwarancji, więc nie wolno tego wypisywać jako pewnik (wynik zależy od uruchomienia).
    }

    // =================================================================================================
    // 10. PYTANIA-PUŁAPKI
    // =================================================================================================

    /**
     * 10. Karta 25: "Co wypisze?" — pytania z haczykiem, które rekruterzy uwielbiają.
     */
    static void trapCards() {
        section("10. Pytania-pułapki: Co wypisze?");

        // ---- KARTA 25 ★★★ Seria pułapek z liczbami i napisami.   (→ t01_basics/Basics08FloatingPoint, t15_numbers/Numbers05IntegerTricks)
        // ODPOWIEDŹ: nie zgaduj w głowie — liczą się reguły: (1) + zachowuje kolejność, więc 1 + 2 to 3 DOPÓKI nie
        //   pojawi się napis; (2) liczby zmiennoprzecinkowe są dwójkowe i niedokładne — nie porównuj ich ==; (3) rzutowanie
        //   (int) obcina, nie zaokrągla; (4) int się "zawija" przy przepełnieniu; (5) char w arytmetyce to liczba;
        //   (6) i = i++ zostawia starą wartość, bo przypisanie nadpisuje zwiększenie.
        show("1 + 2 + \"a\" + 1 + 2", 1 + 2 + "a" + 1 + 2);
        // WYNIK: 1 + 2 + "a" + 1 + 2 → 3a12
        show("0.1 + 0.2 == 0.3", 0.1 + 0.2 == 0.3);
        // WYNIK: 0.1 + 0.2 == 0.3 → false
        show("(int) 3.99", (int) 3.99);
        // WYNIK: (int) 3.99 → 3
        show("Math.abs(Integer.MIN_VALUE)", Math.abs(Integer.MIN_VALUE));
        // WYNIK: Math.abs(Integer.MIN_VALUE) → -2147483648
        show("Integer.MAX_VALUE + 1", Integer.MAX_VALUE + 1);
        // WYNIK: Integer.MAX_VALUE + 1 → -2147483648
        char znak = 'a';
        show("znak + 1 (int)", znak + 1);
        // WYNIK: znak + 1 (int) → 98
        show("(char) (znak + 1)", (char) (znak + 1));
        // WYNIK: (char) (znak + 1) → b
        int i = 5;
        i = i++;
        show("i = i++", i);
        // WYNIK: i = i++ → 5
        show("5 / 2, -7 % 3, 5 / 2.0", (5 / 2) + ", " + (-7 % 3) + ", " + (5 / 2.0));
        // WYNIK: 5 / 2, -7 % 3, 5 / 2.0 → 2, -1, 2.5
        // DOBRA PRAKTYKA: na rozmowie powiedz na głos regułę, nie tylko wynik — to pokazuje zrozumienie. Pieniądze licz BigDecimal
        //   (t15_numbers/Numbers01BigDecimal), a nie double.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • JDK = narzędzia + JRE; JRE = JVM + biblioteki; JVM wykonuje bytecode (.class, nagłówek CAFEBABE).
     *   • == porównuje referencje, equals zawartość; wrappery i napisy zawsze przez equals.
     *   • Cache Integer: -128..127, więc 127 == 127, ale 128 != 128. Long.equals(Integer) to false.
     *   • String jest niezmienny; pula napisów na stercie; intern(); sklejanie w pętli → StringBuilder.
     *   • final: zmienna/metoda/klasa; finally: blok "prawie zawsze"; finalize: przestarzała, nie używać.
     *   • Java zawsze przekazuje przez wartość (dla obiektów — kopię referencji).
     *   • checked = Exception bez RuntimeException; unchecked = RuntimeException i Error.
     *   • try-with-resources zamyka w odwrotnej kolejności i zachowuje wyjątek główny (reszta w getSuppressed).
     *   • Przeciążanie wybiera kompilator (po typie zmiennej), nadpisywanie — JVM (po typie obiektu).
     *   • Interfejs = kontrakt (default od Javy 8); klasa abstrakcyjna = stan i wspólny kod.
     *   • record: płytko niezmienny; kopiuj kolekcje w konstruktorze kompaktowym.
     *   • var: wnioskowanie w czasie kompilacji, nie dynamiczne typowanie. switch-wyrażenie: strzałki, yield, wyczerpujący.
     *   • Erasure: generyki znikają w bytecode; typ surowy zanieczyszcza listę → ClassCastException przy odczycie.
     *   • Lambda: zmienna lokalna effectively final; this = obiekt otaczający.
     *   • orElse liczy zawsze, orElseGet leniwie; strumień jest leniwy i jednorazowy.
     *   • Stos prywatny wątku (ramki), sterta wspólna (obiekty); GC usuwa nieosiągalne.
     *
     * PYTANIA KONTROLNE:
     *   1. Co wypisze:  Integer a = 200, b = 200;  System.out.println((a == b) + " " + a.equals(b));  ?
     *   2. Co wypisze:  String s = "x";  s.concat("y");  System.out.println(s);  ?
     *   3. ZNAJDŹ BŁĄD:  int suma = 0;  lista.forEach(x -> suma += x);
     *   4. Czym różnią się JDK, JRE i JVM? Który z nich jest potrzebny, żeby tylko uruchomić gotowy program?
     *   5. Co zwróci:  static int f() { int x = 1; try { return x; } finally { x = 2; } }  ?
     *   6. Czy Java przekazuje obiekty przez referencję? Jak to udowodnisz jednym przykładem?
     *   7. Co się stanie:  List<String> l = new ArrayList<>(); List raw = l; raw.add(1); String s = l.get(0);  ?
     *   8. Czy Optional.orElse(drogaMetoda()) wywoła drogaMetoda(), gdy Optional ma wartość?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        checkAll(false);
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        checkAll(true);
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    private static void checkAll(boolean solutions) {
        Check.equal("ćw. 1: boxedEquals", List.of(true, true, true, false),
                () -> solutions
                        ? List.of(solution1(127, 127), solution1(1000, 1000), solution1(null, null), solution1(null, 5))
                        : List.of(exercise1(127, 127), exercise1(1000, 1000), exercise1(null, null), exercise1(null, 5)));
        Check.equal("ćw. 2: dayTypeNew", List.of("weekend", "weekend", "dzień roboczy"),
                () -> solutions
                        ? List.of(solution2("SOB"), solution2("NIE"), solution2("PON"))
                        : List.of(exercise2("SOB"), exercise2("NIE"), exercise2("PON")));
        Check.equal("ćw. 3: nameOrDefault (leniwie)", List.of("Ala:0", "domyślne:1"),
                () -> {
                    int[] calls1 = {0};
                    int[] calls2 = {0};
                    String r1 = solutions ? solution3(Optional.of("Ala"), calls1) : exercise3(Optional.of("Ala"), calls1);
                    String r2 = solutions ? solution3(Optional.empty(), calls2) : exercise3(Optional.empty(), calls2);
                    return List.of(r1 + ":" + calls1[0], r2 + ":" + calls2[0]);
                });
        Check.equal("ćw. 4: useTwoResources",
                List.of("otwarto A", "otwarto B", "użycie", "zamknięto B", "zamknięto A"),
                () -> solutions ? solution4() : exercise4());
        Check.equal("ćw. 5: makeBasket", "[a]|niezmienna|NPE",
                () -> checkBasket(solutions ? Interview01JavaQuestions::solution5 : Interview01JavaQuestions::exercise5));
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć true, gdy dwa opakowane Integer mają taką samą wartość (dwa null-e też są "równe"),
     * false w pozostałych przypadkach. Nie wolno używać operatora == na Integerach.
     * Podpowiedź: Objects.equals(a, b) obsługuje null.
     */
    static boolean exercise1(Integer a, Integer b) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ stary switch na wyrażenie switch (strzałki, brak break). Zwracaj "weekend" dla
     * "SOB" i "NIE" oraz "dzień roboczy" dla każdego innego dnia.
     * <pre>{@code
     * static String dayType(String day) {
     *     String r;
     *     switch (day) {
     *         case "SOB":
     *         case "NIE":
     *             r = "weekend";
     *             break;
     *         default:
     *             r = "dzień roboczy";
     *     }
     *     return r;
     * }
     * }</pre>
     * Podpowiedź: return switch (day) { case "SOB", "NIE" -> ...; default -> ...; };
     */
    static String exercise2(String day) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć imię z Optional, a gdy go brak — napis zwrócony przez computeDefault(calls).
     * Metoda computeDefault ma zostać wywołana TYLKO wtedy, gdy imienia brak (licznik calls[0] ma zostać 0 dla pełnego Optional).
     * Podpowiedź: orElseGet przyjmuje lambdę wywoływaną leniwie.
     */
    static String exercise3(Optional<String> name, int[] calls) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): otwórz w JEDNYM try-with-resources dwa zasoby Res("A") i Res("B"), w ciele dopisz do logu
     * "użycie" i zwróć log. Spodziewana kolejność: otwarto A, otwarto B, użycie, zamknięto B, zamknięto A.
     * Podpowiedź: konstruktor Res(nazwa, log, false) sam dopisuje "otwarto ..."; close() dopisuje "zamknięto ...".
     * Uwaga: zasób musi być użyty w ciele (np. a.name), inaczej kompilator ostrzega.
     */
    static List<String> exercise4() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): zbuduj Basket tak, by był odporny na późniejszą zmianę listy źródłowej i niezmienny
     * (kopia List.copyOf), a dla owner == null rzucał NullPointerException (Objects.requireNonNull).
     * Podpowiedź: rekord Basket jest płytko niezmienny — obronę robisz Ty, przed wywołaniem konstruktora.
     */
    static Basket exercise5(String owner, List<String> items) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Sprawdza Basket: lista po zmianie źródła, niezmienność i obsługę null (zwraca opis do porównania). */
    private static String checkBasket(BiFunction<String, List<String>, Basket> maker) {
        List<String> src = new ArrayList<>(List.of("a"));
        Basket basket = maker.apply("Ala", src);
        src.add("b");
        String items = basket.items().toString();
        String niezmienna;
        try {
            basket.items().add("x");
            niezmienna = "zmienna";
        } catch (UnsupportedOperationException e) {
            niezmienna = "niezmienna";
        }
        String npe;
        try {
            maker.apply(null, List.of());
            npe = "brak";
        } catch (NullPointerException e) {
            npe = "NPE";
        }
        return items + "|" + niezmienna + "|" + npe;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static boolean solution1(Integer a, Integer b) {
        return Objects.equals(a, b);
    }

    static String solution2(String day) {
        return switch (day) {
            case "SOB", "NIE" -> "weekend";
            default -> "dzień roboczy";
        };
    }

    static String solution3(Optional<String> name, int[] calls) {
        return name.orElseGet(() -> computeDefault(calls));
    }

    static List<String> solution4() {
        List<String> log = new ArrayList<>();
        try (Res a = new Res("A", log, false); Res b = new Res("B", log, false)) {
            Objects.requireNonNull(a);
            Objects.requireNonNull(b);
            log.add("użycie");
        }
        return log;
    }

    static Basket solution5(String owner, List<String> items) {
        return new Basket(Objects.requireNonNull(owner), List.copyOf(items));
    }

    // </editor-fold>

    // =================================================================================================
    // TYPY I METODY POMOCNICZE (dla kart)
    // =================================================================================================

    /** Akcja, która może rzucić wyjątek (pomocnik: wypisuje tylko NAZWĘ wyjątku, bez komunikatu zależnego od wersji). */
    @FunctionalInterface
    interface Action {
        void run() throws Exception;
    }

    static void thrown(String label, Action action) {
        try {
            action.run();
            show(label, "NIE rzucono wyjątku");
        } catch (Throwable t) {
            show(label, "rzucono " + t.getClass().getSimpleName());
        }
    }

    static String classMagic() {
        try (InputStream in = Interview01JavaQuestions.class.getResourceAsStream("Interview01JavaQuestions.class")) {
            DataInputStream data = new DataInputStream(Objects.requireNonNull(in));
            return Integer.toHexString(data.readInt()).toUpperCase(Locale.ROOT);
        } catch (IOException e) {
            return "błąd odczytu: " + e.getMessage();
        }
    }

    static String tryCatchFinally(boolean fail) {
        StringBuilder sb = new StringBuilder("try");
        try {
            if (fail) {
                throw new IllegalStateException("błąd");
            }
        } catch (IllegalStateException e) {
            sb.append(",catch");
        } finally {
            sb.append(",finally");
        }
        return sb.toString();
    }

    static int finallyDoesNotChangeReturn() {
        int x = 1;
        try {
            return x;
        } finally {
            x = 2;
        }
    }

    static StringBuilder finallyMutatesObject() {
        StringBuilder sb = new StringBuilder("a");
        try {
            return sb;
        } finally {
            sb.append("!");
        }
    }

    static void increment(int n) {
        n++;
    }

    static void reassign(StringBuilder sb) {
        sb = new StringBuilder("nowy");
        sb.append("?");
    }

    static void mutate(StringBuilder sb) {
        sb.append("!");
    }

    static void swap(Integer a, Integer b) {
        Integer tmp = a;
        a = b;
        b = tmp;
    }

    static String f(Object o) {
        return "Object";
    }

    static String f(String s) {
        return "String";
    }

    static String describe(Animal a) {
        return "describe(Animal)";
    }

    static String describe(Dog d) {
        return "describe(Dog)";
    }

    @SuppressWarnings("fallthrough")
    static String oldSwitch(Level level) {
        String r;
        switch (level) {
            case LOW:
                r = "niski";
            case MEDIUM:
                r = "średni";
                break;
            default:
                r = "wysoki";
        }
        return r;
    }

    static int score(Level level) {
        return switch (level) {
            case LOW -> 1;
            case MEDIUM -> 5;
            case HIGH -> {
                int base = 5;
                yield base * 2;
            }
        };
    }

    static int expensiveCalls;

    static String expensive() {
        expensiveCalls++;
        return "domyślna";
    }

    static String computeDefault(int[] calls) {
        calls[0]++;
        return "domyślne";
    }

    static int depth;

    static void recurse() {
        depth++;
        recurse();
    }

    static class Defaults {
        int prymityw;
        Integer wrapper;
    }

    static class Counter {
        static int created;
        final int id;

        Counter() {
            id = ++created;
        }
    }

    static class Animal {
        static String kind() {
            return "zwierzę";
        }

        String voice() {
            return "...";
        }
    }

    static class Dog extends Animal {
        static String kind() {
            return "pies";
        }

        @Override
        String voice() {
            return "hau";
        }
    }

    enum Level { LOW, MEDIUM, HIGH }

    interface Walker {
        default String move() {
            return "idę";
        }
    }

    interface Swimmer {
        default String move() {
            return "płynę";
        }
    }

    static class Duck implements Walker, Swimmer {
        @Override
        public String move() {
            return Walker.super.move() + " i " + Swimmer.super.move();
        }
    }

    record Point(int x, int y) { }

    record Team(String name, List<String> members) { }

    record SafeTeam(String name, List<String> members) {
        SafeTeam {
            members = List.copyOf(members);
        }
    }

    record Basket(String owner, List<String> items) { }

    static class Owner {
        final String name = "obiekt otaczający";

        Supplier<String> fromLambda() {
            return () -> this.name;
        }

        Supplier<String> fromAnonymous() {
            return new Supplier<>() {
                final String name = "klasa anonimowa";

                @Override
                public String get() {
                    return this.name;
                }
            };
        }
    }

    static final class Res implements AutoCloseable {
        final String name;
        private final List<String> log;
        private final boolean failOnClose;

        Res(String name, List<String> log, boolean failOnClose) {
            this.name = name;
            this.log = log;
            this.failOnClose = failOnClose;
            log.add("otwarto " + name);
        }

        @Override
        public void close() {
            log.add("zamknięto " + name);
            if (failOnClose) {
                throw new IllegalStateException("przy zamykaniu " + name);
            }
        }
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. false true — 200 jest poza cache (-128..127), więc a i b to dwa obiekty; == porównuje referencje, equals wartości.
     *   2. x — String jest niezmienny; concat zwraca NOWY napis, którego nie przypisano.
     *   3. Zmienna suma jest zmieniana w lambdzie, a musi być effectively final — błąd kompilacji. Użyj
     *      lista.stream().mapToInt(Integer::intValue).sum() (reduce zamiast zmiennej współdzielonej).
     *   4. JVM wykonuje bytecode; JRE = JVM + biblioteki standardowe; JDK = JRE + narzędzia (javac, jar, jshell...).
     *      Do uruchomienia gotowego programu wystarczy JRE (w praktyce dziś — JDK lub obraz z jlink).
     *   5. 1 — return zapamiętuje wartość przed finally; późniejsze x = 2 jej nie zmienia (dla int).
     *   6. Nie — zawsze przez wartość; dla obiektu wartością jest kopia referencji. Dowód: reassign() nie zmienia
     *      zmiennej wołającego, a swap(a, b) nie zamienia argumentów (karta 10).
     *   7. ClassCastException w ostatniej linii: raw.add(1) przeszło (erasure), ale odczyt jako String wstawia rzutowanie (karta 19).
     *   8. Tak — argument orElse jest zwykłym wyrażeniem, obliczanym zawsze. Gdy obliczenie jest kosztowne,
     *      użyj orElseGet(() -> drogaMetoda()) (karta 21).
     */
    // </editor-fold>
}
