package t10_exceptions;

import helpers.Check;

import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Wyjątki — podstawy: try, catch, finally, throw, stos wywołań, hierarchia
 *        (exception = wyjątek; try = spróbuj; catch = złap; finally = na koniec; throw = rzuć)
 *
 * W SKRÓCIE:
 *   Gdy coś pójdzie źle (dzielenie przez zero, zły format liczby, null), Java tworzy OBIEKT wyjątku i „rzuca” go.
 *   Wykonanie metody natychmiast się przerywa, a wyjątek leci w górę stosu wywołań — do metody, która wywołała,
 *   potem do jej wywołującej itd. — aż trafi na pasujący blok catch. Jeśli nikt go nie złapie, program kończy się
 *   i wypisuje ślad stosu (stack trace). finally wykonuje się ZAWSZE — z błędem czy bez.
 *
 * ANALOGIA: alarm pożarowy w biurowcu.
 *   Pracownik na 5. piętrze (metoda) zauważa pożar i włącza alarm (throw). Przerywa pracę — nie kończy raportu.
 *   Alarm idzie piętro po piętrze w dół (w górę stosu), aż trafi do kogoś przeszkolonego (catch), kto wie, co robić.
 *   Bez względu na wszystko ochrona na koniec zamyka drzwi (finally).
 *
 * JAK TO DZIAŁA:
 *   try {
 *       int n = Integer.parseInt(text);   ← jeśli tu poleci wyjątek...
 *       show("liczba", n);                ← ...ta linia się NIE wykona
 *   } catch (NumberFormatException e) {   ← ...skok tutaj (gdy typ pasuje)
 *       show("zły format", e.getMessage());
 *   } finally {
 *       show("koniec", "zawsze");         ← wykona się zawsze
 *   }
 *   Hierarchia: Throwable → Error (błędy JVM, nie łapiemy) i Exception → RuntimeException (błędy programisty).
 *
 * SŁÓWKA:
 *   exception = wyjątek; throw = rzuć; throws = rzuca (w nagłówku metody, t10/Exceptions02); try = spróbuj;
 *   catch = złap; finally = na koniec; stack trace = ślad stosu; message = komunikat; cause = przyczyna;
 *   throwable = „rzucalny” (wszystko, co można rzucić); error = błąd (poważny, JVM); runtime = czas działania;
 *   unwinding = odwijanie (stosu); parse = odczytaj (z tekstu); arithmetic = arytmetyczny; bounds = granice.
 *
 * ZOBACZ TEŻ: t10_exceptions/Exceptions02CheckedUnchecked (checked/unchecked), t05_methods/Methods01Basics (stos wywołań),
 *             t10_exceptions/Exceptions07BestPractices (dobre praktyki), t14_optional/Optional01Basics (brak wartości bez wyjątku).
 * </pre>
 */
public class Exceptions01Basics {

    public static void main(String[] args) {
        title("Exceptions01 — wyjątki: podstawy");

        commonExceptions();     // common exceptions = najczęstsze wyjątki
        tryCatch();             // try-catch = spróbuj-złap
        stackUnwinding();       // stack unwinding = odwijanie stosu
        finallyAlwaysRuns();    // finally always runs = finally zawsze się wykonuje
        exceptionObject();      // exception object = obiekt wyjątku
        throwYourOwn();         // throw your own = rzuć sam
        hierarchy();            // hierarchy = hierarchia
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. NAJCZĘSTSZE WYJĄTKI
    // =================================================================================================

    /** length = długość napisu. Osobna metoda, żeby komunikat NPE miał przewidywalną nazwę zmiennej ("text"). */
    static int length(String text) {
        return text.length();
    }

    /**
     * 1. Te wyjątki zobaczysz najczęściej. Wszystkie to RuntimeException — błędy, których dało się uniknąć sprawdzeniem.
     * expectThrows (z helpers) uruchamia kod i wypisuje, jaki wyjątek poleciał — lekcja nie przerywa się.
     */
    static void commonExceptions() {
        section("1. Najczęstsze wyjątki");

        expectThrows("10 / 0", () -> System.out.println(10 / zero()));
        // WYNIK: ✔ 10 / 0 → rzucono ArithmeticException: / by zero

        int[] numbers = {1, 2, 3};
        expectThrows("numbers[5]", () -> System.out.println(numbers[5]));
        // WYNIK: ✔ numbers[5] → rzucono ArrayIndexOutOfBoundsException: Index 5 out of bounds for length 3

        expectThrows("Integer.parseInt(\"12a\")", () -> Integer.parseInt("12a"));
        // WYNIK: ✔ Integer.parseInt("12a") → rzucono NumberFormatException: For input string: "12a"

        expectThrows("length(null)", () -> length(null));
        // WYNIK: ✔ length(null) → rzucono NullPointerException: Cannot invoke "String.length()" because "text" is null

        expectThrows("List.of(1).add(2)", () -> List.of(1).add(2));
        // WYNIK: ✔ List.of(1).add(2) → rzucono UnsupportedOperationException: (brak komunikatu)

        Object o = "tekst";
        expectThrows("(Integer) \"tekst\"", () -> System.out.println((Integer) o));
        // WYNIK: ✔ (Integer) "tekst" → rzucono ClassCastException: class java.lang.String cannot be cast to class java.lang.Integer (java.lang.String and java.lang.Integer are in module java.base of loader 'bootstrap')

        // DOBRA PRAKTYKA: czytaj komunikat wyjątku — zwykle mówi dokładnie, co się stało („Index 5 out of bounds
        //   for length 3”). Od Javy 14 komunikat NPE mówi nawet, KTÓRA zmienna była null (helpful NPE).
    }

    /** zero = zero. Przez metodę, bo kompilator nie pozwala na dzielenie przez stałą 0 bez ostrzeżenia. */
    static int zero() {
        return 0;
    }

    // =================================================================================================
    // 2. try / catch
    // =================================================================================================

    /** parseOrDefault = odczytaj liczbę albo zwróć wartość domyślną. Klasyczne użycie try/catch. */
    static int parseOrDefault(String text, int defaultValue) {
        try {
            int value = Integer.parseInt(text);
            System.out.println("   try → udało się odczytać: " + value);   // przy błędzie ta linia się NIE wykona
            return value;
        } catch (NumberFormatException e) {
            System.out.println("   catch → zły format: \"" + text + "\" → " + defaultValue);
            return defaultValue;
        }
    }

    /** 2. try/catch: kod „ryzykowny” w try, obsługa błędu w catch. Po catch program działa dalej normalnie. */
    static void tryCatch() {
        section("2. try / catch");

        int a = parseOrDefault("42", 0);
        int b = parseOrDefault("czterdzieści", 0);
        show("suma", a + b);
        // WYNIK:    try → udało się odczytać: 42
        // WYNIK:    catch → zły format: "czterdzieści" → 0
        // WYNIK: suma → 42

        // PUŁAPKA: catch łapie tylko wyjątki PODANEGO typu (i jego podtypów). catch (NumberFormatException e)
        //   nie złapie NullPointerException — ten poleci dalej.
    }

    // =================================================================================================
    // 3. ODWIJANIE STOSU
    // =================================================================================================

    static void level1() {
        System.out.println("   level1: start");
        try {
            level2();
            System.out.println("   level1: po level2");            // nie wykona się
        } catch (IllegalStateException e) {
            System.out.println("   level1: złapano → " + e.getMessage());
            System.out.println("   level1: wyjątek powstał w metodzie " + e.getStackTrace()[0].getMethodName());
        }
    }

    static void level2() {
        System.out.println("   level2: start");
        level3();
        System.out.println("   level2: koniec");                     // nie wykona się
    }

    static void level3() {
        System.out.println("   level3: start");
        throw new IllegalStateException("awaria w level3");
    }

    /**
     * 3. Wyjątek przerywa level3, potem level2 (brak catch), aż trafia do catch w level1.
     * Linie „koniec” i „po level2” się nie wykonują. Ślad stosu (getStackTrace) pamięta, gdzie wyjątek powstał.
     */
    static void stackUnwinding() {
        section("3. Wyjątek leci w górę stosu wywołań");

        level1();
        // WYNIK:    level1: start
        // WYNIK:    level2: start
        // WYNIK:    level3: start
        // WYNIK:    level1: złapano → awaria w level3
        // WYNIK:    level1: wyjątek powstał w metodzie level3

        // Gdyby NIKT nie złapał wyjątku, program by się zakończył, a na konsoli (stderr) pojawiłby się ślad stosu:
        //   Exception in thread "main" java.lang.IllegalStateException: awaria w level3
        //       at t10_exceptions.Exceptions01Basics.level3(Exceptions01Basics.java:...)   ← gdzie powstał
        //       at t10_exceptions.Exceptions01Basics.level2(Exceptions01Basics.java:...)   ← kto wywołał
        //       at t10_exceptions.Exceptions01Basics.level1(Exceptions01Basics.java:...)
        // DOBRA PRAKTYKA: ślad stosu czytaj OD GÓRY: pierwsza linia „at” z Twojego kodu to zwykle miejsce błędu.
    }

    // =================================================================================================
    // 4. finally
    // =================================================================================================

    /** withFinally = z finally. Pokazuje, że finally wykona się i po sukcesie, i po błędzie, i nawet po return. */
    static String withFinally(String text) {
        try {
            System.out.println("   try: " + text);
            return "wynik " + Integer.parseInt(text);
        } catch (NumberFormatException e) {
            System.out.println("   catch: zły format");
            return "brak wyniku";
        } finally {
            System.out.println("   finally: sprzątam");          // wykona się PRZED faktycznym powrotem z metody
        }
    }

    /** 4. finally służy do sprzątania (zamykanie plików, zwalnianie blokad) — wykona się zawsze. */
    static void finallyAlwaysRuns() {
        section("4. finally wykonuje się zawsze");

        show("zwrócono", withFinally("7"));
        // WYNIK:    try: 7
        // WYNIK:    finally: sprzątam
        // WYNIK: zwrócono → wynik 7

        show("zwrócono", withFinally("x"));
        // WYNIK:    try: x
        // WYNIK:    catch: zły format
        // WYNIK:    finally: sprzątam
        // WYNIK: zwrócono → brak wyniku

        // PUŁAPKA: nigdy nie pisz return w finally — nadpisze wynik z try/catch, a nawet „połknie” wyjątek
        //   (kompilator ostrzega: finally clause cannot complete normally).
        // DOBRA PRAKTYKA: do zamykania zasobów zamiast finally używaj try-with-resources (Exceptions04TryWithResources).
    }

    // =================================================================================================
    // 5. OBIEKT WYJĄTKU
    // =================================================================================================

    /** 5. Wyjątek to zwykły obiekt: ma klasę, komunikat (getMessage) i ślad stosu (getStackTrace). */
    static void exceptionObject() {
        section("5. Co jest w obiekcie wyjątku");

        try {
            Integer.parseInt("3,14");
        } catch (NumberFormatException e) {
            show("getClass().getSimpleName()", e.getClass().getSimpleName());
            show("getMessage()", e.getMessage());
            show("toString()", e);
        }
        // WYNIK: getClass().getSimpleName() → NumberFormatException
        // WYNIK: getMessage() → For input string: "3,14"
        // WYNIK: toString() → java.lang.NumberFormatException: For input string: "3,14"

        // e.printStackTrace() wypisuje pełny ślad stosu na stderr (strumień błędów) — do szybkiego debugowania.
        // DOBRA PRAKTYKA: w prawdziwych programach zamiast printStackTrace użyj loggera (t18_io_files/Io10SimpleLogger).
    }

    // =================================================================================================
    // 6. throw — RZUCANIE WŁASNYCH WYJĄTKÓW
    // =================================================================================================

    /** checkAge = sprawdź wiek. Sprawdza argument i rzuca IllegalArgumentException (niepoprawny argument). */
    static int checkAge(int age) {
        if (age < 0 || age > 150) {
            throw new IllegalArgumentException("Wiek poza zakresem 0..150: " + age);
        }
        return age;
    }

    /** 6. throw new Typ("komunikat") — sam zgłaszasz błąd, gdy metoda dostała dane, z którymi nie może pracować. */
    static void throwYourOwn() {
        section("6. throw — zgłaszanie błędu");

        show("checkAge(30)", checkAge(30));
        // WYNIK: checkAge(30) → 30

        expectThrows("checkAge(-5)", () -> checkAge(-5));
        // WYNIK: ✔ checkAge(-5) → rzucono IllegalArgumentException: Wiek poza zakresem 0..150: -5

        // DOBRA PRAKTYKA: najczęściej rzucane gotowe wyjątki:
        //   IllegalArgumentException — zły argument metody; IllegalStateException — obiekt w złym stanie do tej operacji;
        //   NullPointerException (przez Objects.requireNonNull) — niedozwolony null; UnsupportedOperationException — operacja
        //   nieobsługiwana. Komunikat niech mówi, CO jest źle i JAKA była wartość.
    }

    // =================================================================================================
    // 7. HIERARCHIA WYJĄTKÓW
    // =================================================================================================

    /**
     * 7. Wyjątki tworzą drzewo dziedziczenia. catch (IllegalArgumentException e) złapie też NumberFormatException,
     * bo to jego podklasa. Dlatego w kilku catch najpierw piszemy typy SZCZEGÓŁOWE, potem OGÓLNE.
     */
    static void hierarchy() {
        section("7. Hierarchia: od szczegółu do ogółu");

        StringBuilder chain = new StringBuilder();
        Class<?> c = NumberFormatException.class;
        while (c != null) {
            chain.append(c.getSimpleName());
            c = c.getSuperclass();                 // getSuperclass = pobierz klasę nadrzędną (rodzica)
            if (c != null) {
                chain.append(" → ");
            }
        }
        show("NumberFormatException", chain);
        // WYNIK: NumberFormatException → NumberFormatException → IllegalArgumentException → RuntimeException → Exception → Throwable → Object

        try {
            Integer.parseInt("abc");
        } catch (IllegalArgumentException e) {     // ogólniejszy typ łapie też podklasę
            show("złapane jako IllegalArgumentException", e.getClass().getSimpleName());
        }
        // WYNIK: złapane jako IllegalArgumentException → NumberFormatException

        // Drzewo (najważniejsze gałęzie):
        //   Throwable
        //   ├── Error                  — poważne problemy JVM (OutOfMemoryError, StackOverflowError) — NIE łapiemy
        //   └── Exception              — sprawdzane (checked), np. IOException — kompilator wymaga obsługi (Exceptions02)
        //       └── RuntimeException   — niesprawdzane (unchecked): NPE, IllegalArgumentException, ArithmeticException...
        // PUŁAPKA: catch (Exception e) przed catch (NumberFormatException e) to błąd kompilacji — drugi catch byłby
        //   nieosiągalny („exception has already been caught”).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Wyjątek = obiekt błędu; throw go rzuca; przerywa metodę i leci w górę stosu do pasującego catch.
     *   • try { ryzykowne } catch (Typ e) { obsługa } finally { sprzątanie — zawsze }.
     *   • Kod w try PO miejscu wyjątku się nie wykona. Po catch program działa dalej.
     *   • Nikt nie złapał → koniec programu + ślad stosu na stderr (czytaj od góry).
     *   • e.getMessage(), e.getClass().getSimpleName(), e.getStackTrace()[0] — gdzie powstał.
     *   • Hierarchia: Throwable → Error | Exception → RuntimeException. catch łapie typ i podklasy; szczegółowe najpierw.
     *   • Nie pisz return w finally. Gotowe typy: IllegalArgumentException, IllegalStateException, UnsupportedOperationException.
     *
     * PYTANIA KONTROLNE:
     *   1. Co się dzieje z resztą kodu w bloku try po linii, która rzuciła wyjątek?
     *   2. Co wypisze:
     *          try { System.out.print("A"); Integer.parseInt("x"); System.out.print("B"); }
     *          catch (NumberFormatException e) { System.out.print("C"); }
     *          finally { System.out.print("D"); }
     *          System.out.print("E");
     *   3. ZNAJDŹ BŁĄD:
     *          try { ... } catch (Exception e) { ... } catch (NumberFormatException e) { ... }
     *   4. Czy catch (IllegalArgumentException e) złapie NumberFormatException? Dlaczego?
     *   5. Co wypisze:  try { int[] t = new int[2]; t[2] = 1; } catch (ArithmeticException e) { System.out.print("X"); }
     *      (cały program)
     *   6. Jaki wyjątek rzucisz, gdy metoda dostanie ujemną cenę? A gdy ktoś wywoła wypłatę na zamkniętym koncie?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: \"15\" → 15", 15, () -> exercise1("15", -1));
        Check.equal("ćw. 1b: \"x\" → -1", -1, () -> exercise1("x", -1));
        Check.equal("ćw. 2: poprawne liczby w liście", 2, () -> exercise2(List.of("12", "x", "7", "3.5", "")));
        Check.equal("ćw. 3a: cena 19.99", 19.99, () -> exercise3(19.99));
        Check.throwsException("ćw. 3b: cena -1 → IllegalArgumentException", IllegalArgumentException.class, () -> exercise3(-1));
        Check.equal("ćw. 4a: dziennik dla \"12\"", "start, ok 12, koniec", () -> exercise4("12"));
        Check.equal("ćw. 4b: dziennik dla \"x\"", "start, błąd NumberFormatException, koniec", () -> exercise4("x"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", 15, () -> solution1("15", -1));
        Check.equal("ćw. 1b (wzorzec)", -1, () -> solution1("x", -1));
        Check.equal("ćw. 2 (wzorzec)", 2, () -> solution2(List.of("12", "x", "7", "3.5", "")));
        Check.equal("ćw. 3a (wzorzec)", 19.99, () -> solution3(19.99));
        Check.throwsException("ćw. 3b (wzorzec)", IllegalArgumentException.class, () -> solution3(-1));
        Check.equal("ćw. 4a (wzorzec)", "start, ok 12, koniec", () -> solution4("12"));
        Check.equal("ćw. 4b (wzorzec)", "start, błąd NumberFormatException, koniec", () -> solution4("x"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 7 OK, ✘ 0 BŁĄD
    }

    /** ĆWICZENIE 1 (łatwe): zamień tekst na int; przy złym formacie zwróć defaultValue. Podpowiedź: sekcja 2. */
    static int exercise1(String text, int defaultValue) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): policz, ile tekstów z listy to poprawne liczby całkowite (Integer.parseInt się udaje).
     * Podpowiedź: pętla, w środku try { parseInt; licznik++; } catch (NumberFormatException e) { }  — pusty catch jest
     * tu wyjątkowo OK, bo „zły format” to oczekiwana sytuacja; dodaj komentarz, dlaczego jest pusty.
     */
    static int exercise2(List<String> texts) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć cenę, jeśli jest ≥ 0; dla ujemnej rzuć IllegalArgumentException z komunikatem
     * zawierającym złą wartość. Podpowiedź: sekcja 6.
     */
    static double exercise3(double price) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zbuduj „dziennik” (StringBuilder): zawsze najpierw "start", potem "ok N" (N = liczba)
     * albo "błąd TYP" (prosta nazwa klasy wyjątku), a na końcu — w finally — "koniec". Elementy oddziel ", ".
     * Przykład: "12" → "start, ok 12, koniec"; "x" → "start, błąd NumberFormatException, koniec".
     */
    static String exercise4(String text) {
        // TODO: twoje rozwiązanie
        return null;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(String text, int defaultValue) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    static int solution2(List<String> texts) {
        int count = 0;
        for (String t : texts) {
            try {
                Integer.parseInt(t);
                count++;
            } catch (NumberFormatException e) {
                // celowo pusty: zły format to oczekiwany przypadek — po prostu go nie liczymy
            }
        }
        return count;
    }

    static double solution3(double price) {
        if (price < 0) {
            throw new IllegalArgumentException("Cena nie może być ujemna: " + price);
        }
        return price;
    }

    static String solution4(String text) {
        StringBuilder log = new StringBuilder("start");
        try {
            int n = Integer.parseInt(text);
            log.append(", ok ").append(n);
        } catch (NumberFormatException e) {
            log.append(", błąd ").append(e.getClass().getSimpleName());
        } finally {
            log.append(", koniec");
        }
        return log.toString();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Nie wykonuje się — sterowanie od razu skacze do pasującego catch (albo, gdy go brak, wychodzi z metody).
     *   2. „ACDE” — A się wypisze, parseInt rzuca wyjątek (B pominięte), catch wypisuje C, finally D, dalej E.
     *   3. catch (Exception e) łapie wszystko, więc catch (NumberFormatException e) po nim jest nieosiągalny — błąd kompilacji.
     *      Kolejność: najpierw szczegółowy (NumberFormatException), potem ogólny (Exception).
     *   4. Tak — NumberFormatException dziedziczy po IllegalArgumentException, a catch łapie typ i wszystkie jego podklasy.
     *   5. Nic nie wypisze „X” — poleci ArrayIndexOutOfBoundsException, którego ten catch nie łapie; program się zakończy
     *      ze śladem stosu na stderr.
     *   6. Ujemna cena → IllegalArgumentException (zły argument). Wypłata z zamkniętego konta → IllegalStateException
     *      (argument może być dobry, ale obiekt jest w złym stanie do tej operacji).
     */
    // </editor-fold>
}
