package t01_basics;

import helpers.Check;

import java.util.ArrayList;
import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Jak powstaje i uruchamia się program w Javie
 *        (hello = cześć; JVM = Java Virtual Machine = wirtualna maszyna Javy)
 *
 * W SKRÓCIE:
 *   Piszesz kod w pliku .java → kompilator javac zamienia go na kod bajtowy (plik .class) → maszyna wirtualna
 *   JVM uruchamia ten kod, zaczynając od metody main. Dzięki JVM ten sam plik .class działa na Windows, Linuksie i macOS.
 *
 * ANALOGIA: przepis kulinarny i tłumacz.
 *   Kod .java to przepis napisany po polsku. Kompilator (javac) tłumaczy go na uniwersalny język „kucharski”
 *   (kod bajtowy). JVM to kucharz, który zna ten uniwersalny język i gotuje w DOWOLNEJ kuchni (na każdym systemie).
 *   Nie musisz pisać osobnego przepisu dla każdej kuchni — wystarczy, że w kuchni jest kucharz (zainstalowana Java).
 *
 * JAK TO DZIAŁA:
 *     Basics01HelloJvm.java  ──javac──▶  Basics01HelloJvm.class  ──java (JVM)──▶  uruchomiony program
 *        (kod źródłowy)                  (kod bajtowy, bytecode)                  (zaczyna od main)
 *
 *   JDK (Java Development Kit = zestaw narzędzi programisty) = kompilator javac + JVM + biblioteki + narzędzia.
 *   JRE (Java Runtime Environment = środowisko uruchomieniowe) = JVM + biblioteki (tylko do uruchamiania).
 *   Programista instaluje JDK. IntelliJ po kliknięciu ▶ sam robi oba kroki: kompiluje i uruchamia.
 *
 * SŁÓWKA:
 *   compile = kompilować (tłumaczyć kod źródłowy); compiler = kompilator; source code = kod źródłowy;
 *   bytecode = kod bajtowy; run = uruchom; main = główny; class = klasa; public = publiczny;
 *   static = statyczny; void = nic (metoda nic nie zwraca); args (arguments) = argumenty;
 *   print = wypisz; println (print line) = wypisz linię; comment = komentarz; identifier = identyfikator (nazwa).
 *
 * ZOBACZ TEŻ: t00_start/Start01HowToUse (jak korzystać z kursu), t01_basics/Basics11ConsoleOutput (wypisywanie),
 *             t01_basics/Basics03Variables (zmienne), t26_jvm/Jvm01Memory (co JVM robi z pamięcią).
 * </pre>
 */
public class Basics01HelloJvm {

    /**
     * main = główna metoda — PUNKT STARTU programu. JVM szuka dokładnie takiej sygnatury (nagłówka):
     * <pre>
     *   public          — publiczna: JVM musi mieć do niej dostęp z zewnątrz
     *   static          — statyczna: można ją wywołać bez tworzenia obiektu klasy (obiektów jeszcze nie ma!)
     *   void            — nic nie zwraca
     *   main            — nazwa, której szuka JVM (inna nazwa = program się nie uruchomi)
     *   String[] args   — tablica napisów z argumentami programu (sekcja 6)
     * </pre>
     */
    public static void main(String[] args) {
        title("Basics01 — jak powstaje i uruchamia się program");

        firstOutput();              // first output = pierwszy wydruk
        whereIsTheCode(args);       // where is the code = gdzie jest kod
        comments();                 // comments = komentarze
        statementsAndCase();        // statements and case = instrukcje i wielkość liter
        namingConventions();        // naming conventions = konwencje nazewnicze
        programArguments(args);     // program arguments = argumenty programu
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PIERWSZY WYDRUK
    // =================================================================================================

    /**
     * 1. Najprostszy program wypisuje tekst na konsolę. {@code System.out.println(...)} czytamy od lewej:
     * System (system) → out (wyjście, czyli konsola) → println (wypisz linię i przejdź do nowej).
     */
    static void firstOutput() {
        section("1. Pierwszy wydruk: System.out.println");

        System.out.println("Witaj, Java!");
        // WYNIK: Witaj, Java!

        // print (bez „ln”) NIE przechodzi do nowej linii — kolejne wydruki są doklejane:
        System.out.print("Ala ");
        System.out.print("ma ");
        System.out.println("kota");
        // WYNIK: Ala ma kota

        // Tekst w cudzysłowie "..." to napis (String). Liczby i działania wypisujemy bez cudzysłowu:
        System.out.println(2 + 3);          // najpierw działanie, potem wypisanie
        System.out.println("2 + 3");        // to jest napis — Java go NIE liczy
        // WYNIK: 5
        // WYNIK: 2 + 3

        // PUŁAPKA: łączenie napisu z liczbami idzie od lewej. "Wynik: " + 2 + 3 to NAJPIERW napis "Wynik: 2",
        //   potem doklejone "3". Żeby dodać liczby, użyj nawiasów.
        System.out.println("Wynik: " + 2 + 3);
        System.out.println("Wynik: " + (2 + 3));
        // WYNIK: Wynik: 23
        // WYNIK: Wynik: 5
    }

    // =================================================================================================
    // 2. GDZIE JEST KOD — PAKIET, KLASA, PLIK
    // =================================================================================================

    /**
     * 2. Każdy kod w Javie mieszka w KLASIE, a klasa w PAKIECIE (package = pakiet — to po prostu folder).
     * Nazwa pliku MUSI być taka sama jak nazwa publicznej klasy: Basics01HelloJvm → Basics01HelloJvm.java.
     */
    static void whereIsTheCode(String[] args) {
        section("2. Pakiet, klasa, plik");

        // getName = pobierz (pełną) nazwę klasy razem z pakietem; getSimpleName = sama nazwa klasy
        show("pełna nazwa klasy", Basics01HelloJvm.class.getName());
        show("sama nazwa klasy", Basics01HelloJvm.class.getSimpleName());
        show("pakiet", Basics01HelloJvm.class.getPackageName());
        // WYNIK: pełna nazwa klasy → t01_basics.Basics01HelloJvm
        // WYNIK: sama nazwa klasy → Basics01HelloJvm
        // WYNIK: pakiet → t01_basics

        // Pakiet odpowiada folderom: t01_basics = folder src/t01_basics.
        //   Pakiet z kropkami (np. com.firma.sklep) = foldery zagnieżdżone: src/com/firma/sklep.
        // Pierwsza linia pliku (package t01_basics;) MUSI zgadzać się z folderem.

        // Wersja Javy, na której uruchamiasz program (Runtime = środowisko uruchomieniowe; feature = główny numer wersji):
        show("główna wersja Javy", Runtime.version().feature());
        // (wynik zależy od zainstalowanej Javy — w tym kursie: 17)

        // PUŁAPKA: public class Abc w pliku Xyz.java = błąd kompilacji („class Abc is public, should be declared
        //   in a file named Abc.java”). Zmieniasz nazwę klasy? W IntelliJ użyj Shift+F6 (Rename) — zmieni też plik.
    }

    // =================================================================================================
    // 3. KOMENTARZE
    // =================================================================================================

    /**
     * 3. Komentarze to notatki dla ludzi — kompilator je całkowicie pomija. Są trzy rodzaje:
     * <pre>
     *   //   komentarz jednoliniowy — do końca linii
     *   /*   komentarz blokowy — może obejmować wiele linii; kończy się gwiazdką i ukośnikiem
     *   /**  komentarz dokumentacyjny (Javadoc) — nad klasą/metodą; też kończy się gwiazdką i ukośnikiem;
     *        IntelliJ pokazuje go pod Ctrl+Q (dokładnie taki komentarz czytasz teraz)
     * </pre>
     * (Zakończenia komentarza nie da się wpisać WEWNĄTRZ komentarza — zamknęłoby go. Dlatego jest opisane słowami.)
     */
    static void comments() {
        section("3. Komentarze — kompilator je pomija");

        System.out.println("A" /* ten komentarz jest w środku wyrażenia */ + "B");   // a ten do końca linii
        // WYNIK: AB

        // DOBRA PRAKTYKA: komentarz ma wyjaśniać DLACZEGO coś robisz, a nie powtarzać CO robi kod.
        //   Źle:  i = i + 1;   // zwiększ i o 1
        //   Dobrze: retries = retries + 1;   // serwer bywa przeciążony — próbujemy jeszcze raz
        //   (W tym kursie komentarzy jest celowo DUŻO — to podręcznik, nie kod produkcyjny.)

        // DOBRA PRAKTYKA: zamiast kasować fragment kodu „na próbę”, zakomentuj go skrótem Ctrl+/ w IntelliJ.
        //   Ale do kodu, który oddajesz innym, nie zostawiaj zakomentowanych kawałków — od historii zmian jest git.
    }

    // =================================================================================================
    // 4. INSTRUKCJE, BLOKI I WIELKOŚĆ LITER
    // =================================================================================================

    /**
     * 4. Instrukcja (statement) kończy się średnikiem {@code ;}. Blok to instrukcje w nawiasach klamrowych {@code { }}.
     * Java rozróżnia wielkość liter (jest „case sensitive”): value i Value to DWIE różne zmienne.
     */
    static void statementsAndCase() {
        section("4. Instrukcje, bloki, wielkość liter");

        int value = 1;           // instrukcja: deklaracja zmiennej value z wartością 1 (zmienne: Basics03Variables)
        int Value = 2;           // inna zmienna! (tak nie nazywaj zmiennych — patrz sekcja 5)
        show("value", value);
        show("Value", Value);
        // WYNIK: value → 1
        // WYNIK: Value → 2

        {                                         // blok — zmienne w środku żyją tylko do zamykającej klamry
            int insideBlock = 10;                 // inside block = wewnątrz bloku
            show("zmienna z bloku", insideBlock);
        }
        // WYNIK: zmienna z bloku → 10
        // Tu insideBlock już NIE istnieje — próba użycia = błąd kompilacji „cannot find symbol” (nie znaleziono symbolu).

        // PUŁAPKA: brak średnika to najczęstszy błąd początkujących — IntelliJ podkreśla go na czerwono
        //   (komunikat: „';' expected” = oczekiwano średnika). Podobnie System.out.PrintLn — błąd, bo println małymi.
    }

    // =================================================================================================
    // 5. KONWENCJE NAZEWNICZE
    // =================================================================================================

    /** MAX_RETRIES = maksymalna liczba ponowień. Stała: static final + WIELKIE_LITERY_Z_PODKREŚLNIKAMI. */
    private static final int MAX_RETRIES = 3;

    /**
     * 5. Nazwy (identyfikatory) — co WOLNO, a co jest DOBRĄ PRAKTYKĄ.
     * <pre>
     *   Klasa, interfejs, enum, rekord:  PascalCase     → OrderService, ProductCategory
     *   Metoda, zmienna, parametr:       camelCase      → calculateTotal, firstName, orderCount
     *   Stała (static final):            UPPER_SNAKE    → MAX_RETRIES, DEFAULT_PRICE
     *   Pakiet:                          małe litery    → t01_basics
     * </pre>
     * Reguły języka: nazwa zaczyna się od litery, _ albo $ (NIE od cyfry), dalej litery, cyfry, _ i $;
     * nie może być słowem kluczowym (class, int, public...). Polskie litery technicznie są dozwolone, ale
     * DOBRA PRAKTYKA: nazwy po angielsku, bez polskich znaków — tak piszą programiści na całym świecie.
     */
    static void namingConventions() {
        section("5. Konwencje nazewnicze");

        int orderCount = 5;                       // camelCase — zmienna
        String firstName = "Anna";                // camelCase — zmienna
        show("orderCount", orderCount);
        show("firstName", firstName);
        show("MAX_RETRIES", MAX_RETRIES);
        // WYNIK: orderCount → 5
        // WYNIK: firstName → Anna
        // WYNIK: MAX_RETRIES → 3

        // Nazwy, które się NIE skompilują:  int 2nd = 2;  (zaczyna się od cyfry)
        //                                   int my-value = 1;  (myślnik to minus!)
        //                                   int class = 1;  (słowo kluczowe)
        // Nazwy poprawne, ale ZŁE: int a, x1, tmp2 (nic nie mówią), int OrderCount (PascalCase dla zmiennej).

        // DOBRA PRAKTYKA: nazwa ma mówić, CO przechowuje: daysUntilExpiry zamiast d, isActive zamiast flag.
        //   Dłuższa, czytelna nazwa jest lepsza od krótkiej zagadki — IntelliJ i tak ją podpowie (Ctrl+Spacja).
    }

    // =================================================================================================
    // 6. ARGUMENTY PROGRAMU — String[] args
    // =================================================================================================

    /**
     * 6. {@code args} to tablica napisów przekazanych przy uruchomieniu, np. z linii poleceń:
     * {@code java Program Ala 7} → args = ["Ala", "7"]. Uruchamiając ▶ w IntelliJ, zwykle nie podajesz żadnych,
     * więc tablica jest PUSTA (length = 0). Argumenty ustawisz w: Run → Edit Configurations → Program arguments.
     */
    static void programArguments(String[] args) {
        section("6. Argumenty programu (String[] args)");

        show("liczba argumentów", args.length);            // length = długość (liczba elementów tablicy)
        // WYNIK: liczba argumentów → 0

        // PUŁAPKA: args[0] przy pustej tablicy → ArrayIndexOutOfBoundsException (indeks poza zakresem tablicy).
        //   Zawsze najpierw sprawdź args.length (tablice: t03_arrays/Arrays01Basics).
        expectThrows("args[0] przy pustej tablicy", () -> System.out.println(args[0]));
        // WYNIK: ✔ args[0] przy pustej tablicy → rzucono ArrayIndexOutOfBoundsException: Index 0 out of bounds for length 0
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • .java (kod źródłowy) → javac → .class (kod bajtowy) → JVM uruchamia od main.
     *   • JDK = javac + JVM + biblioteki (dla programisty); JRE = JVM + biblioteki (do uruchamiania).
     *   • public static void main(String[] args) — dokładnie taka sygnatura; args bywa puste.
     *   • println = wypisz i nowa linia; print = bez nowej linii; "Wynik: " + 2 + 3 → "Wynik: 23".
     *   • Nazwa pliku = nazwa publicznej klasy; package = folder.
     *   • Komentarze: // (do końca linii), blokowy (od ukośnika z gwiazdką), Javadoc (od ukośnika z dwiema
     *     gwiazdkami) — kompilator je pomija.
     *   • Instrukcje kończą się ; — bloki { }; Java rozróżnia wielkość liter.
     *   • Nazwy: PascalCase (klasy), camelCase (metody, zmienne), UPPER_SNAKE (stałe), małe litery (pakiety).
     *
     * PYTANIA KONTROLNE:
     *   1. Co robi javac, a co JVM?
     *   2. Dlaczego ten sam plik .class działa na Windows i Linuksie?
     *   3. Co wypisze:  System.out.println("Suma: " + 1 + 2);  a co  System.out.println(1 + 2 + " to suma");  ?
     *   4. Co wypisze:  System.out.print("A"); System.out.println("B"); System.out.print("C");  ?
     *   5. ZNAJDŹ BŁĄD:  public class Hello { public static void Main(String[] args) { System.out.println("Hi") } }
     *   6. Które nazwy są poprawne i zgodne z konwencją: totalPrice, TotalPrice, total_price, 2total, MAX_SIZE (dla stałej)?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        List<Boolean> expected3 = List.of(true, false, true, false, true);
        List<String> names3 = List.of("firstName", "2ndPlace", "_temp", "my-value", "cenaŁączna");

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: powitanie", "Cześć, Ania! Witaj w Javie.", () -> exercise1("Ania"));
        Check.equal("ćw. 2: opis argumentów", "Liczba argumentów: 2, pierwszy: start", () -> exercise2(new String[]{"start", "7"}));
        Check.equal("ćw. 2b: brak argumentów", "Brak argumentów", () -> exercise2(new String[0]));
        Check.equal("ćw. 3: poprawne identyfikatory", expected3, () -> exercise3(names3));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "Cześć, Ania! Witaj w Javie.", () -> solution1("Ania"));
        Check.equal("ćw. 2 (wzorzec)", "Liczba argumentów: 2, pierwszy: start", () -> solution2(new String[]{"start", "7"}));
        Check.equal("ćw. 2b (wzorzec)", "Brak argumentów", () -> solution2(new String[0]));
        Check.equal("ćw. 3 (wzorzec)", expected3, () -> solution3(names3));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć powitanie „Cześć, IMIĘ! Witaj w Javie.” — np. dla "Ania": "Cześć, Ania! Witaj w Javie."
     * Podpowiedź: łączenie napisów operatorem +. Uważaj na spacje i kropkę na końcu.
     */
    static String exercise1(String name) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 2 (średnie): opisz argumenty programu. Gdy tablica jest pusta — zwróć "Brak argumentów".
     * W przeciwnym razie: "Liczba argumentów: N, pierwszy: X" (X = pierwszy argument).
     * Podpowiedź: {@code if (args.length == 0) { return ...; }} — najpierw sprawdź długość, dopiero potem args[0]
     * (instrukcja if dokładnie w t02_controlflow/Control01IfElse).
     */
    static String exercise2(String[] args) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): dla każdej nazwy sprawdź, czy jest poprawnym identyfikatorem Javy (pomijamy słowa
     * kluczowe). Zwróć listę wyników true/false w tej samej kolejności.
     * Podpowiedź: {@code Character.isJavaIdentifierStart(c)} — czy znak może ROZPOCZYNAĆ nazwę;
     * {@code Character.isJavaIdentifierPart(c)} — czy może być DALEJ w nazwie. Pierwszy znak: name.charAt(0);
     * pozostałe — pętla od 1 do name.length() - 1. Wyniki dodawaj do {@code new ArrayList<Boolean>()}.
     * Ciekawostka: polskie litery SĄ dozwolone w nazwach (dlatego "cenaŁączna" jest poprawna) — choć ich nie używamy.
     */
    static List<Boolean> exercise3(List<String> names) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(String name) {
        return "Cześć, " + name + "! Witaj w Javie.";
    }

    static String solution2(String[] args) {
        if (args.length == 0) {
            return "Brak argumentów";
        }
        return "Liczba argumentów: " + args.length + ", pierwszy: " + args[0];
    }

    static List<Boolean> solution3(List<String> names) {
        List<Boolean> result = new ArrayList<>();
        for (String name : names) {
            boolean valid = !name.isEmpty() && Character.isJavaIdentifierStart(name.charAt(0));
            for (int i = 1; i < name.length() && valid; i++) {
                valid = Character.isJavaIdentifierPart(name.charAt(i));
            }
            result.add(valid);
        }
        return result;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. javac tłumaczy kod źródłowy .java na kod bajtowy .class; JVM wczytuje .class i go wykonuje, zaczynając od main.
     *   2. Bo kod bajtowy nie jest przeznaczony dla konkretnego procesora — wykonuje go JVM, a JVM istnieje na każdy system.
     *   3. „Suma: 12” (od lewej: napis + 1 → „Suma: 1”, + 2 → „Suma: 12”) oraz „3 to suma” (najpierw 1 + 2 = 3, potem napis).
     *   4. „AB” w pierwszej linii, a „C” na początku drugiej (print nie kończy linii, println tak).
     *   5. Dwa błędy: Main zamiast main (program się skompiluje, ale JVM nie znajdzie metody startowej) oraz brak
     *      średnika po println("Hi") (błąd kompilacji).
     *   6. Poprawne i zgodne z konwencją: totalPrice (zmienna), MAX_SIZE (stała). TotalPrice — poprawne, ale to styl
     *      klasy; total_price — poprawne, ale nie w stylu Javy; 2total — niepoprawne (zaczyna się od cyfry).
     */
    // </editor-fold>
}
