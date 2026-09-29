package t10_exceptions;

import helpers.Check;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Wyjątki sprawdzane (checked) i niesprawdzane (unchecked); throws
 *        (checked = sprawdzany przez kompilator; unchecked = niesprawdzany; throws = rzuca — deklaracja w nagłówku)
 *
 * W SKRÓCIE:
 *   Java dzieli wyjątki na dwie grupy. SPRAWDZANE (checked — np. IOException) kompilator każe obsłużyć:
 *   albo złapać (catch), albo zadeklarować w nagłówku metody (throws IOException) i przekazać dalej.
 *   To zasada „złap albo zadeklaruj” (catch or declare). NIESPRAWDZANE (RuntimeException i podklasy, np. NPE,
 *   IllegalArgumentException) można obsłużyć, ale kompilator tego nie wymaga — zwykle oznaczają błąd programisty.
 *
 * ANALOGIA: przesyłka polecona kontra zwykły list.
 *   Wyjątek sprawdzany to list polecony: listonosz (kompilator) nie odejdzie, dopóki ktoś nie pokwituje odbioru
 *   (catch) albo nie wskaże, kto odbierze za niego (throws). Niesprawdzany to zwykły list — wrzucony do skrzynki,
 *   nikt nie pilnuje, czy go przeczytasz.
 *
 * JAK TO DZIAŁA:
 *   String load(String name) throws IOException { ... throw new FileNotFoundException(...); }
 *   Wywołanie load("x") bez try/catch i bez throws w metodzie wywołującej → BŁĄD KOMPILACJI:
 *       unreported exception java.io.IOException; must be caught or declared to be thrown
 *   Sprawdzany = Exception i podklasy, POZA RuntimeException. Niesprawdzany = RuntimeException, Error i ich podklasy.
 *
 * SŁÓWKA:
 *   checked = sprawdzany; unchecked = niesprawdzany; throws = rzuca (deklaracja); declare = zadeklaruj;
 *   catch or declare = złap albo zadeklaruj; propagate = przekaż dalej (w górę); recoverable = do naprawienia;
 *   file not found = plik nie znaleziony; wrap = opakuj; load = wczytaj; assignable from = przypisywalny z (czy jest nadtypem).
 *
 * ZOBACZ TEŻ: t10_exceptions/Exceptions01Basics (podstawy), t10_exceptions/Exceptions06ChainingWrapping (opakowywanie),
 *             t18_io_files/Io11IoExceptions (wyjątki IO w praktyce), t13_lambdas/Lambda08Pitfalls (lambdy i wyjątki).
 * </pre>
 */
public class Exceptions02CheckedUnchecked {

    /** FILES = „dysk” udawany mapą: nazwa pliku → zawartość. Dzięki temu lekcja nie dotyka prawdziwych plików. */
    static final Map<String, String> FILES = Map.of(
            "a.txt", "Ala",
            "b.txt", "ma kota",
            "n.txt", "42");

    /**
     * loadText = wczytaj tekst. throws IOException w nagłówku = „ta metoda może rzucić IOException — wywołujący
     * musi się tym zająć”. FileNotFoundException to podklasa IOException.
     */
    static String loadText(String name) throws IOException {
        String content = FILES.get(name);
        if (content == null) {
            throw new FileNotFoundException("Brak pliku: " + name);
        }
        return content;
    }

    public static void main(String[] args) {
        title("Exceptions02 — checked kontra unchecked, throws");

        catchIt();              // catch it = złap
        declareIt();            // declare it = zadeklaruj
        whichIsWhich();         // which is which = który jest który
        lambdasAndChecked();    // lambdas and checked = lambdy i wyjątki sprawdzane
        whenToUseWhich();       // when to use which = kiedy którego użyć
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. ZŁAP (catch)
    // =================================================================================================

    /** 1. Pierwsza opcja: obsłużyć wyjątek na miejscu. Sensowne, gdy WIESZ, co zrobić (np. wartość domyślna). */
    static void catchIt() {
        section("1. Złap: try/catch wokół wywołania");

        for (String name : List.of("a.txt", "z.txt")) {
            try {
                show(name, loadText(name));
            } catch (IOException e) {
                show(name, "nie udało się: " + e.getMessage());
            }
        }
        // WYNIK: a.txt → Ala
        // WYNIK: z.txt → nie udało się: Brak pliku: z.txt

        // Bez try/catch (i bez throws) linia show(name, loadText(name)) się NIE skompiluje:
        //   error: unreported exception java.io.IOException; must be caught or declared to be thrown
    }

    // =================================================================================================
    // 2. ZADEKLARUJ (throws)
    // =================================================================================================

    /** loadNumber = wczytaj liczbę. Nie wie, co zrobić z brakiem pliku, więc PRZEKAZUJE wyjątek dalej (throws). */
    static int loadNumber(String name) throws IOException {
        return Integer.parseInt(loadText(name).strip());
    }

    /** loadNumberOrZero = wczytaj liczbę albo 0. Tu, wyżej, już wiadomo, co zrobić — więc tu łapiemy. */
    static int loadNumberOrZero(String name) {
        try {
            return loadNumber(name);
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * 2. Druga opcja: dopisać throws i zostawić decyzję metodzie wyżej. Wyjątek łapie się tam, gdzie wiadomo,
     * jak zareagować — często kilka poziomów wyżej niż miejsce, w którym powstał.
     */
    static void declareIt() {
        section("2. Zadeklaruj: throws i przekazanie wyżej");

        show("loadNumberOrZero(\"n.txt\")", loadNumberOrZero("n.txt"));
        show("loadNumberOrZero(\"brak.txt\")", loadNumberOrZero("brak.txt"));
        // WYNIK: loadNumberOrZero("n.txt") → 42
        // WYNIK: loadNumberOrZero("brak.txt") → 0

        expectThrows("loadNumber(\"a.txt\")", () -> loadNumber("a.txt"));
        // WYNIK: ✔ loadNumber("a.txt") → rzucono NumberFormatException: For input string: "Ala"
        // Uwaga: NumberFormatException jest NIESPRAWDZANY — nie musiał być w throws, a i tak może polecieć.

        // PUŁAPKA: throws Exception w nagłówku „na wszelki wypadek” zmusza WSZYSTKICH wywołujących do łapania Exception
        //   i ukrywa, co naprawdę może pójść źle. Deklaruj możliwie konkretne typy (throws IOException).
    }

    // =================================================================================================
    // 3. KTÓRY JEST KTÓRY
    // =================================================================================================

    /** kind = rodzaj wyjątku. isAssignableFrom = „czy typ po lewej jest nadtypem (albo tym samym typem) co po prawej”. */
    static String kind(Class<? extends Throwable> type) {
        if (RuntimeException.class.isAssignableFrom(type) || Error.class.isAssignableFrom(type)) {
            return "niesprawdzany";
        }
        return "SPRAWDZANY";
    }

    /** 3. Tabelka popularnych wyjątków. Reguła: pod RuntimeException albo Error → niesprawdzany; reszta → sprawdzany. */
    static void whichIsWhich() {
        section("3. Popularne wyjątki: sprawdzane czy nie?");

        List<Class<? extends Throwable>> types = List.of(
                IOException.class, FileNotFoundException.class, InterruptedException.class,
                java.text.ParseException.class, NullPointerException.class, IllegalArgumentException.class,
                ArithmeticException.class, UncheckedIOException.class, StackOverflowError.class);
        for (Class<? extends Throwable> t : types) {
            System.out.println(String.format("%-26s %s", t.getSimpleName(), kind(t)));
        }
        // WYNIK: IOException                SPRAWDZANY
        // WYNIK: FileNotFoundException      SPRAWDZANY
        // WYNIK: InterruptedException       SPRAWDZANY
        // WYNIK: ParseException             SPRAWDZANY
        // WYNIK: NullPointerException       niesprawdzany
        // WYNIK: IllegalArgumentException   niesprawdzany
        // WYNIK: ArithmeticException        niesprawdzany
        // WYNIK: UncheckedIOException       niesprawdzany
        // WYNIK: StackOverflowError         niesprawdzany
    }

    // =================================================================================================
    // 4. LAMBDY I WYJĄTKI SPRAWDZANE
    // =================================================================================================

    /**
     * 4. Interfejsy z java.util.function (Function, Consumer...) NIE deklarują throws. Lambda wywołująca metodę
     * z throws IOException nie skompiluje się w stream.map(...). Rozwiązanie: złapać w lambdzie i opakować
     * w wyjątek niesprawdzany — do IO jest gotowy UncheckedIOException.
     */
    static void lambdasAndChecked() {
        section("4. Lambdy a wyjątki sprawdzane");

        // .map(name -> loadText(name))   ← BŁĄD KOMPILACJI: unreported exception java.io.IOException
        List<String> contents = List.of("a.txt", "b.txt").stream()
                .map(name -> {
                    try {
                        return loadText(name);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);   // opakowanie: sprawdzany → niesprawdzany
                    }
                })
                .collect(Collectors.toList());
        show("zawartości", contents);
        // WYNIK: zawartości → [Ala, ma kota]

        expectThrows("stream z brakującym plikiem", () -> List.of("a.txt", "x.txt").stream()
                .map(Exceptions02CheckedUnchecked::loadOrThrowUnchecked)
                .collect(Collectors.toList()));
        // WYNIK: ✔ stream z brakującym plikiem → rzucono UncheckedIOException: java.io.FileNotFoundException: Brak pliku: x.txt

        // DOBRA PRAKTYKA: długi try/catch w lambdzie wynieś do osobnej metody (jak loadOrThrowUnchecked poniżej) —
        //   stream zostaje czytelny, a referencja do metody (::) krótka.
    }

    /** loadOrThrowUnchecked = wczytaj albo rzuć niesprawdzany. Metoda pomocnicza dla streamów. */
    static String loadOrThrowUnchecked(String name) {
        try {
            return loadText(name);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // =================================================================================================
    // 5. KIEDY KTÓREGO UŻYĆ
    // =================================================================================================

    /** 5. Reguła kciuka przy projektowaniu WŁASNYCH metod i wyjątków. */
    static void whenToUseWhich() {
        section("5. Kiedy checked, a kiedy unchecked");

        note("unchecked: błąd programisty (zły argument, null, zły stan) — poprawia się KOD, nie łapie wyjątku");
        note("checked: sytuacja zewnętrzna, na którą wywołujący może sensownie zareagować (brak pliku, sieć)");
        // WYNIK:    ℹ unchecked: błąd programisty (zły argument, null, zły stan) — poprawia się KOD, nie łapie wyjątku
        // WYNIK:    ℹ checked: sytuacja zewnętrzna, na którą wywołujący może sensownie zareagować (brak pliku, sieć)

        // DOBRA PRAKTYKA: w nowym kodzie większość własnych wyjątków to unchecked (extends RuntimeException) —
        //   tak robi też nowsze API Javy (java.time, streamy) i popularne frameworki (Spring). Checked zostaw na sytuacje,
        //   w których KAŻDY wywołujący naprawdę powinien się zatrzymać i zdecydować.
        // PUŁAPKA: metoda nadpisująca (override) NIE może zadeklarować szerszych wyjątków sprawdzanych niż metoda
        //   w klasie bazowej/interfejsie (np. throws Exception zamiast throws IOException) — błąd kompilacji.
        // PUŁAPKA: Error (OutOfMemoryError, StackOverflowError) nie łapiemy — program zwykle nie jest w stanie się
        //   z nich sensownie podnieść.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Sprawdzane (checked): Exception i podklasy poza RuntimeException — np. IOException, InterruptedException.
     *     Zasada: złap (catch) albo zadeklaruj (throws) — inaczej błąd kompilacji.
     *   • Niesprawdzane (unchecked): RuntimeException, Error i podklasy — NPE, IllegalArgumentException, ArithmeticException.
     *   • throws w nagłówku przekazuje obowiązek obsługi wyżej; łap tam, gdzie wiesz, jak zareagować.
     *   • Deklaruj konkretne typy (throws IOException), nie throws Exception.
     *   • Lambdy (Function, Consumer...) nie przepuszczą checked → złap w środku i opakuj (UncheckedIOException).
     *   • Nowy kod: własne wyjątki zwykle unchecked. Error nie łapiemy.
     *
     * PYTANIA KONTROLNE:
     *   1. Na czym polega zasada „złap albo zadeklaruj”? Kiedy obowiązuje?
     *   2. ZNAJDŹ BŁĄD:  List<String> r = names.stream().map(n -> loadText(n)).toList();
     *   3. Czy NumberFormatException trzeba deklarować w throws? Dlaczego?
     *   4. Co wypisze:  System.out.println(kind(FileNotFoundException.class) + " " + kind(UncheckedIOException.class));  ?
     *   5. Dlaczego throws Exception „na wszelki wypadek” to zły pomysł?
     *   6. Jaki wyjątek (checked/unchecked) zaprojektujesz dla: a) ujemnej kwoty przelewu, b) braku połączenia z bankiem?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: b.txt", "ma kota", () -> exercise1("b.txt"));
        Check.equal("ćw. 1b: brak.txt → BRAK", "BRAK", () -> exercise1("brak.txt"));
        Check.equal("ćw. 2a: InterruptedException sprawdzany?", true, () -> exercise2(InterruptedException.class));
        Check.equal("ćw. 2b: IllegalStateException sprawdzany?", false, () -> exercise2(IllegalStateException.class));
        Check.equal("ćw. 2c: OutOfMemoryError sprawdzany?", false, () -> exercise2(OutOfMemoryError.class));
        Check.equal("ćw. 3: ile plików się wczytało", 2, () -> exercise3(List.of("a.txt", "x.txt", "n.txt", "y.txt")));
        Check.equal("ćw. 4a: długości zawartości", List.of(3, 7), () -> exercise4(List.of("a.txt", "b.txt")));
        Check.throwsException("ćw. 4b: brakujący plik → UncheckedIOException", UncheckedIOException.class,
                () -> exercise4(List.of("a.txt", "zzz.txt")));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", "ma kota", () -> solution1("b.txt"));
        Check.equal("ćw. 1b (wzorzec)", "BRAK", () -> solution1("brak.txt"));
        Check.equal("ćw. 2a (wzorzec)", true, () -> solution2(InterruptedException.class));
        Check.equal("ćw. 2b (wzorzec)", false, () -> solution2(IllegalStateException.class));
        Check.equal("ćw. 2c (wzorzec)", false, () -> solution2(OutOfMemoryError.class));
        Check.equal("ćw. 3 (wzorzec)", 2, () -> solution3(List.of("a.txt", "x.txt", "n.txt", "y.txt")));
        Check.equal("ćw. 4a (wzorzec)", List.of(3, 7), () -> solution4(List.of("a.txt", "b.txt")));
        Check.throwsException("ćw. 4b (wzorzec)", UncheckedIOException.class, () -> solution4(List.of("a.txt", "zzz.txt")));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 8 OK, ✘ 0 BŁĄD
    }

    /** ĆWICZENIE 1 (łatwe): zwróć loadText(name); gdy poleci IOException — zwróć "BRAK". */
    static String exercise1(String name) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 2 (średnie): czy typ wyjątku jest SPRAWDZANY? true dla checked, false dla RuntimeException/Error i podklas.
     * Podpowiedź: X.class.isAssignableFrom(type) (sekcja 3). Nie używaj metody kind — napisz warunek sam.
     */
    static boolean exercise2(Class<? extends Throwable> type) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** ĆWICZENIE 3 (średnie): policz, ile plików z listy udało się wczytać (loadText bez wyjątku). */
    static int exercise3(List<String> names) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze, łączy z t16_streams): zwróć listę DŁUGOŚCI zawartości plików, streamem.
     * Brakujący plik ma skończyć się UncheckedIOException. Podpowiedź: map(Exceptions02CheckedUnchecked::loadOrThrowUnchecked)
     * albo try/catch w lambdzie, potem map(String::length).
     */
    static List<Integer> exercise4(List<String> names) {
        // TODO: twoje rozwiązanie
        return new ArrayList<>();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(String name) {
        try {
            return loadText(name);
        } catch (IOException e) {
            return "BRAK";
        }
    }

    static boolean solution2(Class<? extends Throwable> type) {
        return !RuntimeException.class.isAssignableFrom(type) && !Error.class.isAssignableFrom(type);
    }

    static int solution3(List<String> names) {
        int loaded = 0;
        for (String name : names) {
            try {
                loadText(name);
                loaded++;
            } catch (IOException e) {
                // celowo pomijamy: brak pliku po prostu nie jest liczony
            }
        }
        return loaded;
    }

    static List<Integer> solution4(List<String> names) {
        return names.stream()
                .map(Exceptions02CheckedUnchecked::loadOrThrowUnchecked)
                .map(String::length)
                .collect(Collectors.toList());
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Wyjątek SPRAWDZANY, który może polecieć z wywołania, trzeba albo złapać (try/catch), albo dopisać do throws
     *      metody wywołującej. Obowiązuje tylko dla checked (nie dla RuntimeException ani Error).
     *   2. loadText rzuca IOException (checked), a lambda w map to Function, który nie deklaruje throws — błąd kompilacji.
     *      Trzeba złapać wyjątek w lambdzie i opakować (UncheckedIOException) albo użyć metody pomocniczej.
     *   3. Nie — to RuntimeException (niesprawdzany). Można go wpisać w throws dla dokumentacji, ale kompilator nie wymaga.
     *   4. „SPRAWDZANY niesprawdzany”.
     *   5. Zmusza każdego wywołującego do łapania ogólnego Exception (albo dalszego throws Exception) i ukrywa,
     *      jakie błędy naprawdę mogą wystąpić; przy okazji łapie się też wyjątki, których nikt nie planował obsłużyć.
     *   6. a) unchecked (IllegalArgumentException) — to błąd wywołującego, powinien przekazać poprawną kwotę;
     *      b) zależy od projektu: klasycznie checked (sytuacja zewnętrzna, można spróbować ponownie), we współczesnym kodzie
     *         często własny unchecked obsługiwany centralnie.
     */
    // </editor-fold>
}
