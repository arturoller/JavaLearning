package t10_exceptions;

import helpers.Check;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Łańcuch wyjątków — przyczyna (cause), opakowywanie i tłumaczenie wyjątków między warstwami
 *        (cause = przyczyna; chaining = łańcuchowanie; wrapping = opakowywanie; root cause = pierwotna przyczyna)
 *
 * W SKRÓCIE:
 *   Program ma warstwy: niska (pliki, sieć) → repozytorium (dane) → serwis (logika) → interfejs. Błąd z niskiej warstwy
 *   (IOException) nic nie mówi warstwie wyższej („jaki plik? po co?”). Dlatego się go OPAKOWUJE: łapiemy niski wyjątek
 *   i rzucamy wyższy, zrozumiały na danym poziomie („Nie można wczytać klienta 7”), przekazując stary jako PRZYCZYNĘ:
 *   new DataAccessException("...", e). Nic nie ginie: e.getCause() prowadzi w dół aż do pierwotnej przyczyny,
 *   a ślad stosu pokazuje całość z liniami „Caused by:”.
 *
 * ANALOGIA: reklamacja w sklepie.
 *   Klient słyszy: „zamówienie nie może być zrealizowane” (wyjątek serwisu). Kierownik wie więcej: „magazyn nie wydał
 *   towaru” (wyjątek repozytorium). Magazynier wie wszystko: „regał 7 się zawalił” (IOException). Każdy poziom mówi
 *   swoim językiem, ale z protokołu (łańcucha cause) da się dojść do regału 7.
 *
 * JAK TO DZIAŁA:
 *   try {
 *       return readFile("klient-7.txt");                 ← niska warstwa: IOException
 *   } catch (IOException e) {
 *       throw new DataAccessException("Nie można wczytać klienta 7", e);   ← nowy wyjątek + stary jako cause
 *   }
 *   wyjątek.getCause() → IOException;  wyjątek.getCause().getCause() → null (koniec łańcucha)
 *
 * SŁÓWKA:
 *   cause = przyczyna; chain = łańcuch; wrap = opakuj; translate = przetłumacz; root cause = pierwotna przyczyna;
 *   layer = warstwa; repository = repozytorium (dostęp do danych); service = serwis (logika); data access = dostęp do danych;
 *   caused by = spowodowany przez; lose = zgubić; stack trace = ślad stosu; find = znajdź.
 *
 * ZOBACZ TEŻ: t10_exceptions/Exceptions03MultiCatch (wyjątek w catch gubi oryginał), t10_exceptions/Exceptions05CustomExceptions,
 *             t10_exceptions/Exceptions07BestPractices, t22_design_patterns/Patterns10Facade (warstwy).
 * </pre>
 */
public class Exceptions06ChainingWrapping {

    // ---------------------------------------------------------------------------------------------
    // Wyjątki warstw
    // ---------------------------------------------------------------------------------------------

    /** DataAccessException = błąd dostępu do danych (warstwa repozytorium). Standardowe konstruktory wyjątku. */
    static final class DataAccessException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        DataAccessException(String message) {
            super(message);
        }

        DataAccessException(String message, Throwable cause) {   // ten konstruktor jest najważniejszy
            super(message, cause);
        }
    }

    /** ServiceException = błąd logiki (warstwa serwisu). */
    static final class ServiceException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        ServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Trzy warstwy programu
    // ---------------------------------------------------------------------------------------------

    /** DISK = udawany dysk: nazwa pliku → zawartość. */
    static final Map<String, String> DISK = Map.of("klient-1.txt", "Anna Nowak", "klient-2.txt", "Jan Kowalski");

    /** readFile = warstwa niska: czyta „plik”. Rzuca IOException (sprawdzany). */
    static String readFile(String name) throws IOException {
        String content = DISK.get(name);
        if (content == null) {
            throw new FileNotFoundException("Nie ma pliku " + name);
        }
        return content;
    }

    /** loadCustomer = warstwa repozytorium: tłumaczy IOException na DataAccessException (z przyczyną). */
    static String loadCustomer(int id) {
        try {
            return readFile("klient-" + id + ".txt");
        } catch (IOException e) {
            throw new DataAccessException("Nie można wczytać klienta " + id, e);
        }
    }

    /** greet = warstwa serwisu: używa repozytorium, a błąd danych tłumaczy na ServiceException (z przyczyną). */
    static String greet(int id) {
        try {
            return "Dzień dobry, " + loadCustomer(id) + "!";
        } catch (DataAccessException e) {
            throw new ServiceException("Nie można przywitać klienta " + id, e);
        }
    }

    /** loadCustomerLosingCause = ZŁA wersja: rzuca nowy wyjątek BEZ przyczyny. */
    static String loadCustomerLosingCause(int id) {
        try {
            return readFile("klient-" + id + ".txt");
        } catch (IOException e) {
            throw new DataAccessException("Nie można wczytać klienta " + id);   // brak „, e” — przyczyna przepada
        }
    }

    public static void main(String[] args) {
        title("Exceptions06 — łańcuch wyjątków, opakowywanie");

        wrappingWithCause();    // wrapping with cause = opakowanie z przyczyną
        walkingTheChain();      // walking the chain = przejście po łańcuchu
        stackTraceCausedBy();   // stack trace „Caused by” = ślad stosu z „Caused by”
        losingTheCause();       // losing the cause = gubienie przyczyny
        rootCause();            // root cause = pierwotna przyczyna
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. OPAKOWANIE Z PRZYCZYNĄ
    // =================================================================================================

    /** 1. Każda warstwa rzuca wyjątek w SWOIM języku, ale przekazuje poprzedni jako przyczynę. */
    static void wrappingWithCause() {
        section("1. Opakowanie: nowy wyjątek + stary jako przyczyna");

        show("greet(1)", greet(1));
        // WYNIK: greet(1) → Dzień dobry, Anna Nowak!

        try {
            greet(7);
        } catch (ServiceException e) {
            show("serwis mówi", e.getMessage());
            show("przyczyna", e.getCause().getMessage());
        }
        // WYNIK: serwis mówi → Nie można przywitać klienta 7
        // WYNIK: przyczyna → Nie można wczytać klienta 7
    }

    // =================================================================================================
    // 2. PRZEJŚCIE PO ŁAŃCUCHU
    // =================================================================================================

    /** 2. getCause() prowadzi poziom niżej. Pętla aż do null wypisuje cały łańcuch — od góry do samego dna. */
    static void walkingTheChain() {
        section("2. Przejście po łańcuchu getCause()");

        try {
            greet(7);
        } catch (ServiceException e) {
            int level = 0;
            for (Throwable t = e; t != null; t = t.getCause()) {
                System.out.println("   " + "  ".repeat(level) + "poziom " + level + ": "
                        + t.getClass().getSimpleName() + " — " + t.getMessage());
                level++;
            }
        }
        // WYNIK:    poziom 0: ServiceException — Nie można przywitać klienta 7
        // WYNIK:    poziom 1: DataAccessException — Nie można wczytać klienta 7
        // WYNIK:    poziom 2: FileNotFoundException — Nie ma pliku klient-7.txt
    }

    // =================================================================================================
    // 3. ŚLAD STOSU Z „Caused by:”
    // =================================================================================================

    /**
     * 3. printStackTrace wypisuje łańcuch z liniami „Caused by:”. Tu przechwytujemy wydruk do napisu (StringWriter)
     * i pokazujemy tylko linie nagłówków (bez linii „at ...”, których numery zależą od kodu).
     */
    static void stackTraceCausedBy() {
        section("3. Ślad stosu: linie „Caused by:”");

        try {
            greet(9);
        } catch (ServiceException e) {
            StringWriter buffer = new StringWriter();
            e.printStackTrace(new PrintWriter(buffer));          // wydruk do napisu zamiast na stderr
            for (String line : buffer.toString().split("\\R")) { // \\R = dowolny znak końca linii
                if (!line.isBlank() && !line.startsWith("\t")) {  // pomijamy linie „\tat ...” i „\t... N more”
                    System.out.println("   " + line);
                }
            }
        }
        // WYNIK:    t10_exceptions.Exceptions06ChainingWrapping$ServiceException: Nie można przywitać klienta 9
        // WYNIK:    Caused by: t10_exceptions.Exceptions06ChainingWrapping$DataAccessException: Nie można wczytać klienta 9
        // WYNIK:    Caused by: java.io.FileNotFoundException: Nie ma pliku klient-9.txt

        // DOBRA PRAKTYKA: czytając ślad stosu, przewiń do OSTATNIEGO „Caused by:” — to zwykle pierwotna przyczyna.
    }

    // =================================================================================================
    // 4. PRZED / PO: gubienie przyczyny
    // =================================================================================================

    /** 4. PRZED: new X("...") bez przyczyny — nie wiadomo, CO naprawdę się stało. PO: new X("...", e). */
    static void losingTheCause() {
        section("4. PRZED/PO: zgubiona przyczyna");

        try {
            loadCustomerLosingCause(5);
        } catch (DataAccessException e) {
            show("PRZED: przyczyna", e.getCause());
        }
        // WYNIK: PRZED: przyczyna → null    ← brak pliku? brak uprawnień? dysk pełny? — nie wiadomo

        try {
            loadCustomer(5);
        } catch (DataAccessException e) {
            show("PO: przyczyna", e.getCause());
        }
        // WYNIK: PO: przyczyna → java.io.FileNotFoundException: Nie ma pliku klient-5.txt

        // PUŁAPKA: new X(e.getMessage()) też gubi przyczynę: zostaje tekst, ale znika typ i ślad stosu oryginału.
        // PUŁAPKA: opakowywanie bez potrzeby (w każdej metodzie) tworzy długie, nieczytelne łańcuchy. Opakowuj na
        //   GRANICY warstw, gdy zmienia się „język” błędu.
    }

    // =================================================================================================
    // 5. PIERWOTNA PRZYCZYNA
    // =================================================================================================

    /** findRootCause = znajdź pierwotną przyczynę: idź po getCause(), dopóki jest następna. */
    static Throwable findRootCause(Throwable t) {
        Throwable current = t;
        while (current.getCause() != null && current.getCause() != current) {   // ochrona przed pętlą
            current = current.getCause();
        }
        return current;
    }

    /**
     * 5. Opakowania mogą być też „w drugą stronę”: UncheckedIOException opakowuje IOException, by przejść przez lambdę
     * (Exceptions02). Zawsze można dojść do źródła.
     */
    static void rootCause() {
        section("5. Pierwotna przyczyna (root cause)");

        try {
            greet(3);
        } catch (RuntimeException e) {
            show("root cause", findRootCause(e).getClass().getSimpleName());
        }
        // WYNIK: root cause → FileNotFoundException

        UncheckedIOException wrapped = new UncheckedIOException(new IOException("dysk odłączony"));
        show("UncheckedIOException → cause", wrapped.getCause().getMessage());
        show("komunikat opakowania", wrapped.getMessage());
        // WYNIK: UncheckedIOException → cause → dysk odłączony
        // WYNIK: komunikat opakowania → java.io.IOException: dysk odłączony    ← konstruktor (cause) bierze cause.toString()
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Opakowanie: catch (NiskiWyjątek e) { throw new WysokiWyjątek("kontekst", e); } — ZAWSZE z „, e”.
     *   • Własny wyjątek: konstruktory (message) i (message, cause) — super(message, cause).
     *   • e.getCause() → poziom niżej; pętla do null → cały łańcuch; ostatni = pierwotna przyczyna (root cause).
     *   • Ślad stosu: linie „Caused by:”; najważniejsze zwykle jest OSTATNIE „Caused by:”.
     *   • new X(e.getMessage()) i new X("...") bez e gubią typ i ślad stosu oryginału.
     *   • Opakowuj na granicy warstw (pliki → dane → logika), nie w każdej metodzie.
     *
     * PYTANIA KONTROLNE:
     *   1. Po co opakowywać IOException w DataAccessException, zamiast przepuścić IOException w górę?
     *   2. Co wypisze:  System.out.println(new RuntimeException("A", new IllegalStateException("B")).getCause().getMessage());  ?
     *   3. ZNAJDŹ BŁĄD:  catch (IOException e) { throw new DataAccessException("Błąd: " + e.getMessage()); }
     *   4. Jak znaleźć pierwotną przyczynę w długim łańcuchu?
     *   5. Co wypisze:  System.out.println(new RuntimeException(new IOException("x")).getMessage());  ?
     *   6. Gdzie w śladzie stosu szukać pierwotnej przyczyny?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** causeName = prosta nazwa klasy PRZYCZYNY wyjątku rzuconego przez action (pomocnicza do sprawdzania ćwiczeń). */
    static String causeName(Runnable action) {
        try {
            action.run();
            return "brak wyjątku";
        } catch (RuntimeException e) {
            return e.getCause() == null ? "brak przyczyny" : e.getCause().getClass().getSimpleName();
        }
    }

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Throwable chain = new RuntimeException("a", new IllegalStateException("b", new IOException("c")));
        Check.equal("ćw. 1a: plik istnieje", "Jan Kowalski", () -> exercise1("klient-2.txt"));
        Check.equal("ćw. 1b: przyczyna opakowania", "FileNotFoundException", () -> causeName(() -> exercise1("brak.txt")));
        Check.equal("ćw. 2: pierwotna przyczyna", "c", () -> exercise2(chain).getMessage());
        Check.equal("ćw. 3: łańcuch jako tekst", "RuntimeException: a <- IllegalStateException: b <- IOException: c",
                () -> exercise3(chain));
        Check.equal("ćw. 4a: znajdź IOException", Optional.of("c"),
                () -> exercise4(chain, IOException.class).map(Throwable::getMessage));
        Check.equal("ćw. 4b: znajdź ArithmeticException", Optional.empty(),
                () -> exercise4(chain, ArithmeticException.class).map(Throwable::getMessage));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", "Jan Kowalski", () -> solution1("klient-2.txt"));
        Check.equal("ćw. 1b (wzorzec)", "FileNotFoundException", () -> causeName(() -> solution1("brak.txt")));
        Check.equal("ćw. 2 (wzorzec)", "c", () -> solution2(chain).getMessage());
        Check.equal("ćw. 3 (wzorzec)", "RuntimeException: a <- IllegalStateException: b <- IOException: c",
                () -> solution3(chain));
        Check.equal("ćw. 4a (wzorzec)", Optional.of("c"),
                () -> solution4(chain, IOException.class).map(Throwable::getMessage));
        Check.equal("ćw. 4b (wzorzec)", Optional.empty(),
                () -> solution4(chain, ArithmeticException.class).map(Throwable::getMessage));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć readFile(name); IOException opakuj w DataAccessException("Błąd odczytu " + name, e).
     * Checker sprawdza, czy przyczyna NIE zginęła.
     */
    static String exercise1(String name) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** ĆWICZENIE 2 (łatwe): zwróć pierwotną przyczynę (najgłębszą w łańcuchu). Nie używaj findRootCause — napisz pętlę sam. */
    static Throwable exercise2(Throwable t) {
        // TODO: twoje rozwiązanie
        return t;
    }

    /**
     * ĆWICZENIE 3 (średnie): zamień łańcuch na tekst "Typ: komunikat <- Typ: komunikat <- ..." (proste nazwy klas),
     * od góry do dołu. Podpowiedź: StringBuilder i pętla for (Throwable c = t; c != null; c = c.getCause()).
     */
    static String exercise3(Throwable t) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): znajdź w łańcuchu PIERWSZY wyjątek danego typu (type.isInstance(c)) i zwróć go
     * w Optional (type.cast(c)); gdy brak — Optional.empty(). Metoda generyczna: T to typ szukanego wyjątku (t11_generics).
     */
    static <T extends Throwable> Optional<T> exercise4(Throwable t, Class<T> type) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(String name) {
        try {
            return readFile(name);
        } catch (IOException e) {
            throw new DataAccessException("Błąd odczytu " + name, e);
        }
    }

    static Throwable solution2(Throwable t) {
        Throwable current = t;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    static String solution3(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (sb.length() > 0) {
                sb.append(" <- ");
            }
            sb.append(c.getClass().getSimpleName()).append(": ").append(c.getMessage());
        }
        return sb.toString();
    }

    static <T extends Throwable> Optional<T> solution4(Throwable t, Class<T> type) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (type.isInstance(c)) {
                return Optional.of(type.cast(c));
            }
        }
        return Optional.empty();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Warstwa wyższa mówi swoim językiem („nie można wczytać klienta 7”) i nie musi znać szczegółów (pliki, sieć);
     *      IOException jest sprawdzany i „zaraża” throws wszystkie wyższe metody. Przyczyna zostaje w getCause().
     *   2. „B”.
     *   3. Brak przyczyny: przekazano tylko tekst komunikatu — ginie typ i ślad stosu oryginału.
     *      Poprawnie: throw new DataAccessException("Błąd odczytu", e);
     *   4. Pętlą po getCause(), dopóki następna przyczyna nie jest null (z ochroną przed cyklem) — ostatni element to root cause.
     *   5. „java.io.IOException: x” — konstruktor z samą przyczyną ustawia komunikat na cause.toString().
     *   6. W ostatniej linii „Caused by:” (najgłębsza przyczyna) i w pierwszych liniach „at” pod nią.
     */
    // </editor-fold>
}
