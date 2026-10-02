package t29_jdbc_databases;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Product;

import java.math.BigDecimal;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: PreparedStatement — zapytania z parametrami ?
 *        (prepared statement = przygotowane polecenie; parameter = parametr)
 *
 * W SKRÓCIE:
 *   Każdą wartość, która przychodzi spoza kodu (od użytkownika, z pliku, z formularza, z innego systemu),
 *   przekazujemy do bazy jako PARAMETR: w tekście SQL stoi znak ?, a wartość ustawiamy osobno metodą setXxx.
 *   Tak robimy ZAWSZE — także wtedy, gdy wartość „na pewno jest bezpieczna”. Przy okazji zyskujemy poprawne
 *   typy (daty, kwoty, NULL), ponowne użycie polecenia i paczki (batch) tysięcy wierszy.
 *
 * ANALOGIA:
 *   Formularz przelewu w banku. Wzór formularza (tekst SQL) jest wydrukowany raz i ma puste kratki (?).
 *   Klient wpisuje tylko zawartość kratek (parametry). Bank czyta kratkę „kwota” zawsze jako kwotę — choćby
 *   ktoś wpisał tam cokolwiek, nie zmieni to treści samego formularza.
 *
 * JAK TO DZIAŁA:
 *   1. {@code con.prepareStatement("SELECT ... WHERE category = ? AND price < ?")} — baza analizuje tekst SQL
 *   2. ps.setString(1, "KSIAZKI"); ps.setBigDecimal(2, limit)        — parametry numerujemy OD 1, nie od 0
 *   3. ps.executeQuery() / executeUpdate()                             — tekst i wartości idą do bazy OSOBNO
 *   4. zmieniasz parametry i wykonujesz ponownie — to samo polecenie, nowe wartości
 *   Parametrem może być tylko WARTOŚĆ (liczba, tekst, data, NULL). Nazwa tabeli, kolumny, słowo ASC/DESC
 *   NIE mogą być parametrami → wybierasz je ze stałej listy dozwolonych (biała lista).
 *
 * SŁÓWKA:
 *   prepare = przygotuj; statement = polecenie; parameter = parametr; placeholder = znacznik miejsca (?);
 *   batch = paczka; generated keys = wygenerowane klucze; escape = zabezpiecz znak specjalny (ucieczka);
 *   whitelist = biała lista (lista dozwolonych); clear = wyczyść; identity = tożsamość (kolumna z licznikiem)
 *
 * ZOBACZ TEŻ: t29_jdbc_databases/Jdbc02Connection (Connection, Statement, ResultSet, SQLException),
 *             t29_jdbc_databases/Jdbc04Transactions (paczka w jednej transakcji),
 *             t29_jdbc_databases/Jdbc05Dao (PreparedStatement schowany w repozytorium)
 * </pre>
 */
public class Jdbc03PreparedStatement {

    /** Nazwana baza w pamięci; jedno połączenie na całą lekcję wystarczy. */
    private static final String URL = "jdbc:h2:mem:jdbc03";

    public static void main(String[] args) throws SQLException {
        title("Jdbc03 — PreparedStatement: parametry, paczki, klucze, listy IN, LIKE");

        try (Connection con = DriverManager.getConnection(URL)) {
            createShop(con);           // create shop = utwórz sklep (schemat + dane)
            firstParameters(con);      // first parameters = pierwsze parametry
            parameterTypes(con);       // parameter types = typy parametrów
            nullParameters(con);       // null parameters = NULL jako parametr
            reuseStatement(con);       // reuse statement = ponowne użycie polecenia
            batchInsert(con);          // batch insert = wstawianie paczkami
            generatedKeys(con);        // generated keys = klucze wygenerowane przez bazę
            inListParameters(con);     // in-list parameters = lista IN o zmiennej długości
            likeWithUserText(con);     // like with user text = LIKE z tekstem użytkownika
            identifiersWhitelist(con); // identifiers whitelist = biała lista nazw kolumn
            commonErrors(con);         // common errors = typowe błędy
        }
        exercises();                   // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PIERWSZE PARAMETRY — ? ZAMIAST SKLEJANIA TEKSTU
    // =================================================================================================

    /**
     * 1. Tekst zapytania jest stały i zawiera znaczniki {@code ?}. Wartości ustawiamy metodami setXxx, podając
     * numer znacznika (od 1, licząc od lewej). Sterownik wysyła tekst i wartości osobno.
     */
    static void firstParameters(Connection con) throws SQLException {
        section("1. Pierwsze parametry — ? zamiast sklejania tekstu");

        // Wyobraź sobie, że te dwie wartości wpisał użytkownik w formularzu wyszukiwania
        String category = "KSIAZKI";
        BigDecimal maxPrice = new BigDecimal("100.00");

        String sql = "SELECT sku, name, price FROM products WHERE category = ? AND price < ? ORDER BY price";
        // prepareStatement = przygotuj polecenie; zamykamy je try-with-resources jak każdy zasób JDBC
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, category);        // setString = ustaw tekst w znaczniku nr 1
            ps.setBigDecimal(2, maxPrice);    // setBigDecimal = ustaw kwotę w znaczniku nr 2
            try (ResultSet rs = ps.executeQuery()) {
                printRows(rs);           // printRows = wypisz wiersze (pomocnik na dole pliku)
            }
        }
        // WYNIK: SKU     | NAME              | PRICE
        // WYNIK: --------+-------------------+------
        // WYNIK: KSI-001 | Czysty kod        | 79.00
        // WYNIK: KSI-003 | Wzorce projektowe | 99.00
        // WYNIK: (wierszy: 2)

        // DOBRA PRAKTYKA: każda wartość spoza kodu trafia do SQL wyłącznie przez ? i setXxx — nigdy przez sklejanie
        // tekstu (+, String.format, StringBuilder). Dlaczego? Baza musi dostać wartości osobno od tekstu zapytania —
        // wtedy wartość zawsze pozostaje daną i nigdy nie staje się częścią polecenia (to chroni m.in. przed atakami
        // typu SQL injection).

        // Drugi zysk: tekst z apostrofem po prostu działa — nie trzeba go niczym „otaczać” ani poprawiać
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM products WHERE name = ?")) {
            ps.setString(1, "Herbata O'Neill");
            show("produktów o nazwie z apostrofem", countOf(ps));   // countOf = policz (pomocnik)
        }
        // WYNIK: produktów o nazwie z apostrofem → 0
        // (Takiego produktu nie ma, więc 0 — ważne, że zapytanie wykonało się poprawnie.)
    }

    // =================================================================================================
    // 2. TYPY PARAMETRÓW — TEKST, LICZBY, KWOTY, DATY, BOOLEAN
    // =================================================================================================

    /**
     * 2. Do każdego typu Javy jest pasująca metoda: setInt/setLong (liczby), setBigDecimal (DECIMAL), setBoolean,
     * setString. Daty z java.time przekazujemy przez {@code setObject(i, LocalDate)} (JDBC 4.2 — działa w H2,
     * PostgreSQL, MySQL), a czytamy przez {@code getObject(kolumna, LocalDate.class)}.
     */
    static void parameterTypes(Connection con) throws SQLException {
        section("2. Typy parametrów — tekst, liczby, kwoty, daty, BOOLEAN");

        String sql = """
                SELECT sku, added, price
                FROM products
                WHERE added BETWEEN ? AND ?
                  AND stock >= ?
                  AND discontinued = ?
                ORDER BY added""";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, LocalDate.of(2026, 2, 1));     // setObject = ustaw obiekt (tu: LocalDate → DATE)
            ps.setObject(2, LocalDate.of(2026, 3, 1));
            ps.setInt(3, 1);                               // setInt = ustaw liczbę całkowitą
            ps.setBoolean(4, false);                       // setBoolean = ustaw wartość logiczną
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // getObject(kolumna, LocalDate.class) = pobierz jako LocalDate (bez starego java.sql.Date)
                    LocalDate added = rs.getObject("added", LocalDate.class);
                    BigDecimal price = rs.getBigDecimal("price");    // getBigDecimal = pobierz kwotę
                    System.out.println("   " + rs.getString("sku") + " dodano " + added + ", cena " + price);
                }
            }
        }
        // WYNIK: SPO-001 dodano 2026-02-02, cena 64.99
        // WYNIK: SPO-002 dodano 2026-02-09, cena 7.49
        // WYNIK: KSI-001 dodano 2026-02-23, cena 79.00

        // PUŁAPKA: nie przekazuj dat jako tekstu („2026-02-01” przez setString). Dlaczego? Baza musi wtedy sama
        // zamienić tekst na datę, a format, który rozumie, zależy od bazy i jej ustawień. setObject(LocalDate)
        // przekazuje DATĘ, nie napis — bez zgadywania formatu.
        // PUŁAPKA: kwot nie ustawiaj przez setDouble — tracisz dokładność jeszcze przed bazą (patrz t15_numbers/Numbers01BigDecimal).

        // Ustawienie kwoty i odczyt: DECIMAL(10,2) wraca ze skalą 2, więc porównujemy compareTo, nie equals
        try (PreparedStatement ps = con.prepareStatement("SELECT price FROM products WHERE sku = ?")) {
            ps.setString(1, "SPO-003");
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                BigDecimal price = rs.getBigDecimal(1);
                show("cena oliwy", price);
                show("equals(new BigDecimal(\"42\"))", price.equals(new BigDecimal("42")));
                show("compareTo(new BigDecimal(\"42\")) == 0", price.compareTo(new BigDecimal("42")) == 0);
            }
        }
        // WYNIK: cena oliwy → 42.00
        // WYNIK: equals(new BigDecimal("42")) → false
        // WYNIK: compareTo(new BigDecimal("42")) == 0 → true
    }

    // =================================================================================================
    // 3. NULL JAKO PARAMETR
    // =================================================================================================

    /**
     * 3. Brak wartości przekazujemy przez {@code setNull(i, Types.X)} — z typem SQL, bo część sterowników musi
     * wiedzieć, jaki to NULL. Ale uwaga: {@code kolumna = ?} z NULL-em to nadal porównanie z NULL, czyli UNKNOWN.
     */
    static void nullParameters(Connection con) throws SQLException {
        section("3. NULL jako parametr");

        // Zapis NULL-a: data dodania nieznana (Types = typy SQL; Types.DATE = DATE)
        try (PreparedStatement ps = con.prepareStatement("UPDATE products SET added = ? WHERE sku = ?")) {
            ps.setNull(1, Types.DATE);       // setNull = ustaw NULL danego typu SQL
            ps.setString(2, "DOM-002");
            show("UPDATE added = NULL → zmieniono wierszy", ps.executeUpdate());
        }
        // WYNIK: UPDATE added = NULL → zmieniono wierszy → 1

        // Typowy wzorzec dla wartości, która w Javie bywa null
        LocalDate maybeDate = null;          // np. pole formularza zostawione puste
        try (PreparedStatement ps = con.prepareStatement("UPDATE products SET added = ? WHERE sku = ?")) {
            if (maybeDate == null) {
                ps.setNull(1, Types.DATE);
            } else {
                ps.setObject(1, maybeDate);
            }
            ps.setString(2, "DOM-001");
            ps.executeUpdate();
        }
        // Krócej: ps.setObject(1, maybeDate, Types.DATE) — setObject z typem SQL też przyjmuje null.

        // PUŁAPKA: „WHERE added = ?” z parametrem NULL znajduje 0 wierszy — to wciąż „= NULL” (patrz Jdbc01SqlBasics)
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM products WHERE added = ?")) {
            ps.setNull(1, Types.DATE);
            show("added = NULL → wierszy", countOf(ps));
        }
        // WYNIK: added = NULL → wierszy → 0

        // IS NOT DISTINCT FROM = „nie różni się od” — traktuje dwa NULL-e jak równe (H2, PostgreSQL;
        // w MySQL odpowiednikiem jest operator <=>). Jeden parametr obsłuży wtedy i datę, i NULL.
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT sku FROM products WHERE added IS NOT DISTINCT FROM ? ORDER BY sku")) {
            ps.setNull(1, Types.DATE);
            try (ResultSet rs = ps.executeQuery()) {
                printRows(rs);
            }
        }
        // WYNIK: SKU
        // WYNIK: -------
        // WYNIK: DOM-001
        // WYNIK: DOM-002
        // WYNIK: DOM-003
        // WYNIK: ELE-005
        // WYNIK: ODZ-003
        // WYNIK: (wierszy: 5)
        // DOBRA PRAKTYKA: jeśli nie możesz użyć IS NOT DISTINCT FROM, zbuduj w Javie dwa warianty zapytania:
        // „added = ?” dla wartości i „added IS NULL” dla null. Dlaczego? Wtedy intencja jest jasna w każdej bazie.
    }

    // =================================================================================================
    // 4. PONOWNE UŻYCIE POLECENIA I clearParameters
    // =================================================================================================

    /**
     * 4. Jedno przygotowane polecenie możesz wykonać wiele razy z różnymi wartościami. Parametry ZOSTAJĄ ustawione
     * między wykonaniami, dopóki ich nie zmienisz albo nie wyczyścisz ({@code clearParameters}).
     */
    static void reuseStatement(Connection con) throws SQLException {
        section("4. Ponowne użycie polecenia i clearParameters");

        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM products WHERE category = ? AND price < ?")) {
            for (String category : List.of("ELEKTRONIKA", "SPOZYWCZE", "KSIAZKI")) {
                ps.setString(1, category);
                ps.setBigDecimal(2, new BigDecimal("500"));
                System.out.println("   " + category + " tańsze niż 500 → " + countOf(ps));
            }
            // WYNIK: ELEKTRONIKA tańsze niż 500 → 2
            // WYNIK: SPOZYWCZE tańsze niż 500 → 3
            // WYNIK: KSIAZKI tańsze niż 500 → 3

            // PUŁAPKA: zmieniamy tylko parametr 1 — parametr 2 (500) został z poprzedniego wykonania
            ps.setString(1, "DOM");
            show("DOM (cena z poprzedniego razu!)", countOf(ps));
            // WYNIK: DOM (cena z poprzedniego razu!) → 2

            // expectSqlError = oczekuj błędu SQL (wypisuje SQLState i opis z describe = opisz)
            // clearParameters = wyczyść parametry; teraz wykonanie bez ustawienia wszystkiego to błąd
            ps.clearParameters();
            ps.setString(1, "DOM");
            expectSqlError("wykonanie po clearParameters bez parametru 2", () -> countOf(ps));
            // WYNIK: ✔ wykonanie po clearParameters bez parametru 2 → SQLState 90012 (H2: parametr nie został ustawiony)
        }

        // Dlaczego ponowne użycie się opłaca? Baza analizuje tekst SQL i układa plan wykonania (jak szukać wierszy).
        // Przy tym samym tekście z ? może zrobić to raz i użyć planu ponownie — zmieniają się tylko wartości.
        // Różnice między bazami: H2 trzyma przygotowane polecenie po swojej stronie; sterownik PostgreSQL przechodzi
        // na polecenie przygotowane po stronie serwera dopiero po kilku wykonaniach (ustawienie prepareThreshold);
        // MySQL Connector/J domyślnie wstawia wartości po stronie klienta (useServerPrepStmts=false), ale robi to
        // bezpiecznie, więc zasada „zawsze ?” obowiązuje tak samo.
        // DOBRA PRAKTYKA: przygotowuj polecenie RAZ przed pętlą, a w pętli tylko ustawiaj parametry i wykonuj.
    }

    // =================================================================================================
    // 5. PACZKI — addBatch / executeBatch
    // =================================================================================================

    /**
     * 5. Paczka (batch) zbiera wiele zestawów parametrów i wysyła je do bazy za jednym razem. Przy tysiącach
     * wierszy to dużo mniej podróży sieć ↔ baza niż osobne executeUpdate. Wynik to tablica liczników: ile
     * wierszy zmienił każdy element paczki.
     */
    static void batchInsert(Connection con) throws SQLException {
        section("5. Paczki — addBatch / executeBatch");

        String sql = "INSERT INTO products (sku, name, category, price, stock) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            String[][] rows = {
                    {"KSI-004", "Algorytmy", "KSIAZKI", "119.00", "5"},
                    {"KSI-005", "Bazy danych", "KSIAZKI", "89.00", "7"},
                    {"KSI-006", "Sieci komputerowe", "KSIAZKI", "99.00", "2"}
            };
            for (String[] r : rows) {
                ps.setString(1, r[0]);
                ps.setString(2, r[1]);
                ps.setString(3, r[2]);
                ps.setBigDecimal(4, new BigDecimal(r[3]));
                ps.setInt(5, Integer.parseInt(r[4]));
                ps.addBatch();                       // addBatch = dodaj bieżące parametry do paczki
            }
            int[] counts = ps.executeBatch();        // executeBatch = wykonaj paczkę
            show("liczniki paczki", Arrays.toString(counts));
            show("razem wstawiono", Arrays.stream(counts).sum());
        }
        // WYNIK: liczniki paczki → [1, 1, 1]
        // WYNIK: razem wstawiono → 3

        // PUŁAPKA: błąd w środku paczki. Drugi element łamie NOT NULL (brak nazwy).
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            addBookToBatch(ps, "KSI-007", "Kompilatory");   // add book to batch = dodaj książkę do paczki
            addBookToBatch(ps, "KSI-008", null);
            addBookToBatch(ps, "KSI-009", "Systemy operacyjne");
            ps.executeBatch();
        } catch (BatchUpdateException e) {           // BatchUpdateException = wyjątek wykonania paczki
            show("SQLState", e.getSQLState() + " (" + describe(e.getSQLState()) + ")");
            // getUpdateCounts = pobierz liczniki; -3 = Statement.EXECUTE_FAILED (ten element się nie udał)
            show("liczniki mimo błędu", Arrays.toString(e.getUpdateCounts()));
        }
        // WYNIK: SQLState → 23502 (NULL w kolumnie NOT NULL)
        // WYNIK: liczniki mimo błędu → [1, -3, 1]
        show("czy KSI-009 jest w bazie", existsSku(con, "KSI-009"));   // exists = istnieje
        // WYNIK: czy KSI-009 jest w bazie → true
        // H2 wykonał pozostałe elementy paczki i (w trybie auto-commit) od razu je zapisał. Inny sterownik może
        // przerwać paczkę na pierwszym błędzie i zwrócić krótszą tablicę — standard JDBC dopuszcza oba zachowania.
        // DOBRA PRAKTYKA: paczkę wykonuj w transakcji (Jdbc04Transactions) i przy błędzie rób ROLLBACK — wtedy
        // wynik jest jednoznaczny: wszystko albo nic, w każdej bazie.
        // DOBRA PRAKTYKA: przy bardzo dużych danych wysyłaj paczki np. po 500–1000 wierszy, a nie milion naraz
        // (pamięć). MySQL przyspiesza paczki z rewriteBatchedStatements=true, PostgreSQL z reWriteBatchedInserts=true.
    }

    // =================================================================================================
    // 6. KLUCZE WYGENEROWANE PRZEZ BAZĘ — getGeneratedKeys
    // =================================================================================================

    /**
     * 6. Kolumna {@code id BIGINT GENERATED ALWAYS AS IDENTITY} dostaje kolejną liczbę od bazy. Żeby poznać nadany
     * numer, przekazujemy {@code Statement.RETURN_GENERATED_KEYS} przy przygotowaniu i po wykonaniu czytamy
     * {@code getGeneratedKeys()} — to zwykły ResultSet.
     */
    static void generatedKeys(Connection con) throws SQLException {
        section("6. Klucze wygenerowane przez bazę — getGeneratedKeys");

        String sql = "INSERT INTO reviews (sku, author, stars) VALUES (?, ?, ?)";
        // RETURN_GENERATED_KEYS = zwróć wygenerowane klucze
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "KSI-001");
            ps.setString(2, "Ola");
            ps.setInt(3, 5);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {    // getGeneratedKeys = pobierz wygenerowane klucze
                keys.next();
                show("id pierwszej recenzji", keys.getLong(1));
            }
            // WYNIK: id pierwszej recenzji → 1

            // W paczce: H2 zwraca klucze wszystkich wstawionych wierszy (inne sterowniki — sprawdź dokumentację)
            ps.setString(1, "KSI-002");
            ps.setString(2, "Bartek");
            ps.setInt(3, 4);
            ps.addBatch();
            ps.setString(1, "KSI-001");
            ps.setString(2, "Celina");
            ps.setInt(3, 3);
            ps.addBatch();
            ps.executeBatch();
            List<Long> ids = new ArrayList<>();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                while (keys.next()) {
                    ids.add(keys.getLong(1));
                }
            }
            show("id z paczki", ids);
        }
        // WYNIK: id z paczki → [2, 3]

        // Wariant z nazwą kolumny: con.prepareStatement(sql, new String[] {"id"}) — przydatny, gdy tabela ma kilka
        // kolumn z wartościami domyślnymi albo baza (np. Oracle) bez tego zwraca coś innego niż chcesz.
        // PUŁAPKA: nie zgaduj nowego id przez SELECT MAX(id) po wstawieniu. Dlaczego? W tym samym czasie ktoś inny
        // mógł wstawić swój wiersz — dostaniesz cudzy numer. Klucz z getGeneratedKeys dotyczy TWOJEGO wiersza.
        // W PostgreSQL można też napisać INSERT ... RETURNING id i wykonać to jak zapytanie (executeQuery).
    }

    // =================================================================================================
    // 7. LISTA IN O ZMIENNEJ DŁUGOŚCI
    // =================================================================================================

    /**
     * 7. Jeden znacznik {@code ?} to jedna wartość — nie lista. Dla {@code IN (...)} budujemy tyle znaczników, ile
     * jest elementów: {@code String.join(", ", Collections.nCopies(n, "?"))}. Tekst SQL składamy wtedy wyłącznie
     * z NASZYCH stałych kawałków i znaków ?, a wartości dalej idą przez setXxx.
     */
    static void inListParameters(Connection con) throws SQLException {
        section("7. Lista IN o zmiennej długości");

        List<String> skus = List.of("ELE-003", "KSI-002", "DOM-001");   // np. zawartość koszyka
        // nCopies = n kopii; join = połącz z separatorem → "?, ?, ?"
        String placeholders = String.join(", ", Collections.nCopies(skus.size(), "?"));
        String sql = "SELECT sku, name FROM products WHERE sku IN (" + placeholders + ") ORDER BY sku";
        show("tekst SQL", sql);
        // WYNIK: tekst SQL → SELECT sku, name FROM products WHERE sku IN (?, ?, ?) ORDER BY sku
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < skus.size(); i++) {
                ps.setString(i + 1, skus.get(i));    // i + 1, bo parametry liczymy od 1
            }
            try (ResultSet rs = ps.executeQuery()) {
                printRows(rs);
            }
        }
        // WYNIK: SKU     | NAME
        // WYNIK: --------+----------------
        // WYNIK: DOM-001 | Ekspres do kawy
        // WYNIK: ELE-003 | Słuchawki BT
        // WYNIK: KSI-002 | Java. Podstawy
        // WYNIK: (wierszy: 3)

        // PUŁAPKA: pusta lista. „IN ()” H2 akurat przyjmuje (zwraca 0 wierszy), ale PostgreSQL i MySQL zgłaszają
        // błąd składni. Obsłuż to w Javie, zanim w ogóle zbudujesz zapytanie:
        // namesBySkus = nazwy według SKU (pomocnik na dole pliku)
        show("pusta lista → wynik bez pytania bazy", namesBySkus(con, List.of()));
        // WYNIK: pusta lista → wynik bez pytania bazy → []
        show("dwa SKU", namesBySkus(con, List.of("SPO-002", "ODZ-001")));
        // WYNIK: dwa SKU → [Kurtka zimowa, Czekolada gorzka]

        // Alternatywa w H2 i PostgreSQL: jeden parametr-tablica: WHERE sku = ANY(?) i
        // ps.setArray(1, con.createArrayOf("VARCHAR", tablica)) — tekst SQL się wtedy nie zmienia.
        // PUŁAPKA: bardzo długie listy. Bazy mają limity (np. Oracle: najwyżej 1000 elementów w jednym IN),
        // a każdy inny rozmiar listy to inny tekst SQL (osobny plan). Tysiące identyfikatorów lepiej wstawić
        // do tabeli tymczasowej i złączyć (JOIN — Jdbc06SqlJoins).
    }

    // =================================================================================================
    // 8. LIKE Z TEKSTEM UŻYTKOWNIKA — ZABEZPIECZANIE % I _
    // =================================================================================================

    /**
     * 8. W LIKE znaki {@code %} i {@code _} mają specjalne znaczenie. Parametr chroni tekst zapytania, ale NIE
     * zmienia znaczenia tych znaków we wzorcu. Jeśli użytkownik szuka dosłownie „USB_C”, trzeba poprzedzić
     * {@code _} znakiem ucieczki i wskazać go klauzulą {@code ESCAPE}.
     */
    static void likeWithUserText(Connection con) throws SQLException {
        section("8. LIKE z tekstem użytkownika — zabezpieczanie % i _");

        String userText = "USB_C";
        // Naiwnie: _ znaczy „dowolny jeden znak”, więc pasuje też „USB-C”
        try (PreparedStatement ps = con.prepareStatement("SELECT name FROM products WHERE name LIKE ? ORDER BY name")) {
            ps.setString(1, "%" + userText + "%");
            try (ResultSet rs = ps.executeQuery()) {
                printRows(rs);
            }
        }
        // WYNIK: NAME
        // WYNIK: -------------------
        // WYNIK: Kabel USB_C 1m
        // WYNIK: Ładowarka USB-C 20W
        // WYNIK: (wierszy: 2)

        // Poprawnie: escapeLike (zabezpiecz dla LIKE) zamienia ! → !!, % → !%, _ → !_, a ESCAPE '!' mówi bazie, że ! jest znakiem ucieczki
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT name FROM products WHERE name LIKE ? ESCAPE '!' ORDER BY name")) {
            ps.setString(1, "%" + escapeLike(userText) + "%");
            try (ResultSet rs = ps.executeQuery()) {
                printRows(rs);
            }
            show("escapeLike(\"100%\")", escapeLike("100%"));
            // WYNIK: NAME
            // WYNIK: --------------
            // WYNIK: Kabel USB_C 1m
            // WYNIK: (wierszy: 1)
            // WYNIK: escapeLike("100%") → 100!%

            // Użytkownik wpisał sam znak % — bez zabezpieczenia pasowałby KAŻDY produkt
            ps.setString(1, "%" + escapeLike("%") + "%");
            try (ResultSet rs = ps.executeQuery()) {
                printRows(rs);
            }
        }
        // WYNIK: NAME
        // WYNIK: ---------------------
        // WYNIK: Skarpety 100% bawełny
        // WYNIK: (wierszy: 1)

        // Dlaczego '!' zamiast domyślnego '\'? H2, PostgreSQL i MySQL domyślnie traktują w LIKE jako znak ucieczki
        // odwrotny ukośnik, ale standard SQL nie ma domyślnego znaku (tylko ten z ESCAPE), a w Javie '\' trzeba
        // podwajać. Jawne ESCAPE '!' działa tak samo wszędzie i łatwo się je czyta.
        // DOBRA PRAKTYKA: wyszukiwanie bez względu na wielkość liter: LOWER(name) LIKE ? ESCAPE '!' i parametr
        // zamieniony w Javie na małe litery: text.toLowerCase(Locale.ROOT) (jawne Locale — patrz t04).
        // Uwaga na wydajność: wzorzec zaczynający się od % nie może użyć zwykłego indeksu (Jdbc07SqlAggregationIndexes).
    }

    // =================================================================================================
    // 9. NAZWY KOLUMN I KIERUNEK SORTOWANIA — TYLKO Z BIAŁEJ LISTY
    // =================================================================================================

    /** Dozwolone kolumny sortowania. Każda stała zna DOKŁADNY fragment SQL — tekst od użytkownika nigdy do SQL nie trafia. */
    enum SortColumn {
        NAME("name"), PRICE("price"), STOCK("stock");

        final String sql;   // sql = fragment SQL (nazwa kolumny w tabeli)

        SortColumn(String sql) {
            this.sql = sql;
        }
    }

    /** Mapa „klucz z formularza → stała”; nieznany klucz = błąd (biała lista). */
    static final Map<String, SortColumn> SORT_KEYS = Map.of("nazwa", SortColumn.NAME, "cena", SortColumn.PRICE, "stan", SortColumn.STOCK);

    /**
     * 9. Parametr {@code ?} zastępuje tylko WARTOŚĆ. Nazwa tabeli, nazwa kolumny, ASC/DESC to części struktury
     * zapytania — baza musi je znać przy analizie tekstu. Dlatego wybieramy je ze stałej listy w kodzie (enum/Map),
     * a tekst od użytkownika służy wyłącznie jako klucz do tej listy.
     */
    static void identifiersWhitelist(Connection con) throws SQLException {
        section("9. Nazwy kolumn i sortowanie — tylko z białej listy");

        // PUŁAPKA: ? w miejscu nazwy tabeli — błąd już przy przygotowaniu (składnia)
        expectSqlError("? jako nazwa tabeli", () -> con.prepareStatement("SELECT name FROM ?").close());
        // WYNIK: ✔ ? jako nazwa tabeli → SQLState 42001 (błąd składni SQL)

        // PUŁAPKA: ORDER BY ? z tekstem „price” — to porównanie z WARTOŚCIĄ, nie nazwa kolumny
        try (PreparedStatement ps = con.prepareStatement("SELECT name FROM products ORDER BY ?")) {
            ps.setString(1, "price");
            expectSqlError("ORDER BY ? z tekstem", () -> ps.executeQuery().close());
        }
        // WYNIK: ✔ ORDER BY ? z tekstem → SQLState 22018 (wartości nie da się zamienić na wymagany typ)
        // H2 próbuje odczytać ten parametr jako NUMER kolumny (stąd błąd konwersji tekstu na liczbę), a z ps.setInt(1, 2)
        // posortowałby po 2. kolumnie. To zachowanie specyficzne dla H2 — w innych bazach parametr w ORDER BY może być
        // zwykłą wartością, czyli sortowaniem po stałej (w praktyce: brakiem sortowania). Kod zależny od takich
        // różnic to kolejny powód, by kolumny sortowania nigdy nie przekazywać parametrem.

        // Poprawnie: klucz z formularza → stała enum → zaufany fragment SQL
        // sortedNames = posortowane nazwy (pomocnik na dole pliku)
        show("sortuj wg „cena”, malejąco", sortedNames(con, "cena", true, 3));
        // WYNIK: sortuj wg „cena”, malejąco → [Laptop Pro 14, Smartfon X, Ekspres do kawy]
        show("sortuj wg „nazwa”", sortedNames(con, "nazwa", false, 3));
        // WYNIK: sortuj wg „nazwa” → [Algorytmy, Bazy danych, Czekolada gorzka]
        expectThrows("nieznany klucz sortowania", () -> sortedNames(con, "kolor", false, 3));
        // WYNIK: ✔ nieznany klucz sortowania → rzucono IllegalArgumentException: niedozwolona kolumna sortowania: kolor
        // DOBRA PRAKTYKA: kierunek sortowania też wybieraj w kodzie (boolean → "DESC" albo "ASC"), nie kopiuj tekstu.
        // To samo dotyczy nazw tabel (np. archiwum per rok): wybór ze stałego zestawu, nigdy wklejanie tekstu.
    }

    // =================================================================================================
    // 10. TYPOWE BŁĘDY — INDEKSY, BRAKUJĄCE PARAMETRY, ZŁE TYPY
    // =================================================================================================

    /**
     * 10. Błędy parametrów zgłasza sterownik jako SQLException. Wypisujemy tylko SQLState — H2 używa tu własnych
     * kodów z klasy 90 (klasa zarezerwowana dla rozszerzeń producenta), inne sterowniki mają inne kody.
     */
    static void commonErrors(Connection con) throws SQLException {
        section("10. Typowe błędy — indeksy, brakujące parametry, złe typy");

        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM products WHERE category = ? AND stock > ?")) {
            // PUŁAPKA: parametry liczymy od 1 — indeks 0 nie istnieje
            expectSqlError("setString(0, ...)", () -> ps.setString(0, "DOM"));
            // PUŁAPKA: są tylko 2 znaczniki — indeks 3 nie istnieje
            expectSqlError("setInt(3, ...)", () -> ps.setInt(3, 10));
            // PUŁAPKA: zapomniany parametr nr 2
            ps.setString(1, "DOM");
            expectSqlError("brak parametru nr 2", () -> countOf(ps));
            // PUŁAPKA: tekst, którego nie da się zamienić na liczbę
            ps.setString(2, "dużo");
            expectSqlError("tekst zamiast liczby", () -> countOf(ps));
        }
        // WYNIK: ✔ setString(0, ...) → SQLState 90008 (H2: zły numer parametru)
        // WYNIK: ✔ setInt(3, ...) → SQLState 90008 (H2: zły numer parametru)
        // WYNIK: ✔ brak parametru nr 2 → SQLState 90012 (H2: parametr nie został ustawiony)
        // WYNIK: ✔ tekst zamiast liczby → SQLState 22018 (wartości nie da się zamienić na wymagany typ)

        // PUŁAPKA: '?' w apostrofach to zwykły znak zapytania (tekst), a nie znacznik — polecenie ma 0 parametrów
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM products WHERE name = '?'")) {
            // getParameterMetaData = pobierz dane o parametrach; getParameterCount = liczba parametrów
            show("liczba parametrów", ps.getParameterMetaData().getParameterCount());
            expectSqlError("setString(1, ...) mimo braku znacznika", () -> ps.setString(1, "Kawa"));
        }
        // WYNIK: liczba parametrów → 0
        // WYNIK: ✔ setString(1, ...) mimo braku znacznika → SQLState 90008 (H2: zły numer parametru)
        // DOBRA PRAKTYKA: przy wielu parametrach trzymaj licznik (int i = 1; ps.setString(i++, ...)) albo pomocniczą
        // metodę — ręczne numerowanie 1, 2, 3… łatwo rozjechać po dopisaniu warunku w środku zapytania.
    }

    // =================================================================================================
    // NARZĘDZIA LEKCJI
    // =================================================================================================

    /** Schemat: produkty z kluczem generowanym przez bazę (IDENTITY) i recenzje. */
    static final String SCHEMA = """
            CREATE TABLE products (
                id           BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                sku          VARCHAR(10)   NOT NULL UNIQUE,
                name         VARCHAR(100)  NOT NULL,
                category     VARCHAR(20)   NOT NULL,
                price        DECIMAL(10,2) NOT NULL CHECK (price > 0),
                stock        INT           DEFAULT 0 NOT NULL CHECK (stock >= 0),
                added        DATE,
                discontinued BOOLEAN       DEFAULT FALSE NOT NULL
            );
            CREATE TABLE reviews (
                id     BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                sku    VARCHAR(10) NOT NULL,
                author VARCHAR(50) NOT NULL,
                stars  INT         NOT NULL CHECK (stars BETWEEN 1 AND 5)
            );
            """;

    /**
     * Tworzy schemat i wstawia produkty z SampleData oraz trzy dodatkowe (z %, _ i - w nazwach) — oczywiście
     * paczką PreparedStatement. Data dodania: 2026-01-05 plus tydzień na każdy kolejny produkt.
     */
    static void createShop(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            for (String sql : SCHEMA.split(";")) {
                if (!sql.isBlank()) {            // isBlank = czy pusty lub same spacje (Java 11+)
                    st.execute(sql);
                }
            }
        }
        String sql = "INSERT INTO products (sku, name, category, price, stock, added) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            List<Product> products = SampleData.products();
            for (int i = 0; i < products.size(); i++) {
                Product p = products.get(i);
                ps.setString(1, p.sku());
                ps.setString(2, p.name());
                ps.setString(3, p.category().name());
                ps.setBigDecimal(4, p.price());
                ps.setInt(5, p.stock());
                ps.setObject(6, LocalDate.of(2026, 1, 5).plusWeeks(i));   // plusWeeks = dodaj tygodnie
                ps.addBatch();
            }
            Object[][] extra = {
                    {"ODZ-003", "Skarpety 100% bawełny", "ODZIEZ", "19.99", 50},
                    {"DOM-003", "Kabel USB_C 1m", "DOM", "29.90", 35},
                    {"ELE-005", "Ładowarka USB-C 20W", "ELEKTRONIKA", "79.00", 14}
            };
            for (Object[] e : extra) {
                ps.setString(1, (String) e[0]);
                ps.setString(2, (String) e[1]);
                ps.setString(3, (String) e[2]);
                ps.setBigDecimal(4, new BigDecimal((String) e[3]));
                ps.setInt(5, (Integer) e[4]);
                ps.setNull(6, Types.DATE);       // data dodania nieznana
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /** Dodaje do paczki książkę za 50 zł ze stanem 1 (nazwa może być null — do pokazania błędu). */
    static void addBookToBatch(PreparedStatement ps, String sku, String name) throws SQLException {
        ps.setString(1, sku);
        ps.setString(2, name);
        ps.setString(3, "KSIAZKI");
        ps.setBigDecimal(4, new BigDecimal("50.00"));
        ps.setInt(5, 1);
        ps.addBatch();
    }

    /** Wykonuje zapytanie typu SELECT COUNT(*) i zwraca liczbę. */
    static int countOf(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    static boolean existsSku(Connection con, String sku) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM products WHERE sku = ?")) {
            ps.setString(1, sku);
            return countOf(ps) > 0;
        }
    }

    /** Nazwy produktów o podanych SKU (posortowane po SKU); pusta lista → pusty wynik bez zapytania. */
    static List<String> namesBySkus(Connection con, List<String> skus) throws SQLException {
        if (skus.isEmpty()) {
            return List.of();
        }
        String sql = "SELECT name FROM products WHERE sku IN ("
                + String.join(", ", Collections.nCopies(skus.size(), "?")) + ") ORDER BY sku";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < skus.size(); i++) {
                ps.setString(i + 1, skus.get(i));
            }
            return names(ps);
        }
    }

    /** Zabezpiecza znaki specjalne LIKE: najpierw sam znak ucieczki (!), potem % i _. */
    static String escapeLike(String text) {
        return text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    /** Pierwsze {@code limit} nazw posortowanych wg kolumny z białej listy (sorted names = posortowane nazwy). */
    static List<String> sortedNames(Connection con, String sortKey, boolean descending, int limit) throws SQLException {
        SortColumn column = SORT_KEYS.get(sortKey);
        if (column == null) {
            throw new IllegalArgumentException("niedozwolona kolumna sortowania: " + sortKey);
        }
        // Do tekstu SQL trafiają tylko NASZE stałe: column.sql i "DESC"/"ASC"; limit to wartość → parametr
        String sql = "SELECT name FROM products ORDER BY " + column.sql + (descending ? " DESC" : " ASC") + ", sku LIMIT ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            return names(ps);
        }
    }

    /** Wykonuje zapytanie i zwraca pierwszą kolumnę każdego wiersza jako tekst. */
    static List<String> names(PreparedStatement ps) throws SQLException {
        List<String> result = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(rs.getString(1));
            }
        }
        return result;
    }

    /** Wypisuje wiersze jako wyrównaną tabelę; NULL pokazuje jako napis NULL. */
    static void printRows(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();       // metadata = dane o danych (nazwy kolumn)
        int columns = meta.getColumnCount();
        List<String[]> rows = new ArrayList<>();
        String[] header = new String[columns];
        for (int i = 0; i < columns; i++) {
            header[i] = meta.getColumnLabel(i + 1);
        }
        rows.add(header);
        while (rs.next()) {
            String[] row = new String[columns];
            for (int i = 0; i < columns; i++) {
                String value = rs.getString(i + 1);
                row[i] = value == null ? "NULL" : value;
            }
            rows.add(row);
        }
        int[] widths = new int[columns];
        for (String[] row : rows) {
            for (int i = 0; i < columns; i++) {
                widths[i] = Math.max(widths[i], row[i].length());
            }
        }
        for (int r = 0; r < rows.size(); r++) {
            StringBuilder line = new StringBuilder("   ");
            for (int i = 0; i < columns; i++) {
                if (i > 0) {
                    line.append(" | ");
                }
                line.append(String.format(Locale.ROOT, "%-" + widths[i] + "s", rows.get(r)[i]));
            }
            System.out.println(line.toString().stripTrailing());    // stripTrailing = usuń końcowe spacje (Java 11+)
            if (r == 0) {
                StringBuilder sep = new StringBuilder("   ");
                for (int i = 0; i < columns; i++) {
                    sep.append(i > 0 ? "-+-" : "").append("-".repeat(widths[i]));
                }
                System.out.println(sep);
            }
        }
        System.out.println("   (wierszy: " + (rows.size() - 1) + ")");
    }

    /** Akcja JDBC, która może rzucić SQLException (SqlAction = akcja SQL). */
    @FunctionalInterface
    interface SqlAction {
        void run() throws SQLException;
    }

    /** Wykonuje akcję, która MA się nie udać, i wypisuje SQLState z polskim opisem (bez komunikatu H2). */
    static void expectSqlError(String label, SqlAction action) {
        try {
            action.run();
            System.out.println("✘ " + label + " → NIE rzucono wyjątku (a spodziewaliśmy się go)");
        } catch (SQLException e) {
            System.out.println("✔ " + label + " → SQLState " + e.getSQLState() + " (" + describe(e.getSQLState()) + ")");
        }
    }

    /** Polskie opisy kodów SQLState użytych w lekcji. */
    static String describe(String sqlState) {
        return switch (sqlState) {
            case "23502" -> "NULL w kolumnie NOT NULL";
            case "23505" -> "duplikat klucza / UNIQUE";
            case "22018" -> "wartości nie da się zamienić na wymagany typ";
            case "42001" -> "błąd składni SQL";
            case "90008" -> "H2: zły numer parametru";
            case "90012" -> "H2: parametr nie został ustawiony";
            default -> "inny błąd";
        };
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Każda wartość spoza kodu → ? + setXxx, bez wyjątków.
     *   • Parametry numerujemy OD 1. set: String, Int, Long, BigDecimal, Boolean, Object(LocalDate), Null(i, Types.X).
     *   • Odczyt daty: rs.getObject("kol", LocalDate.class). Kwoty: BigDecimal i compareTo.
     *   • „kol = ?” z NULL → 0 wierszy. IS NOT DISTINCT FROM ? (H2, PostgreSQL) / <=> (MySQL) albo dwa warianty SQL.
     *   • Przygotuj raz, wykonuj wiele razy; parametry zostają między wykonaniami; clearParameters czyści.
     *   • Paczka: addBatch() w pętli, executeBatch() → int[] liczników; błąd → BatchUpdateException
     *     (zachowanie po błędzie zależy od sterownika) → paczka w transakcji.
     *   • prepareStatement(sql, Statement.RETURN_GENERATED_KEYS) + getGeneratedKeys(); nigdy SELECT MAX(id).
     *   • IN: String.join(", ", Collections.nCopies(n, "?")); pusta lista → obsłuż w Javie (IN () to błąd w PostgreSQL/MySQL).
     *   • LIKE: escapeLike (! → !!, % → !%, _ → !_) + ESCAPE '!'.
     *   • Nazwy tabel/kolumn, ASC/DESC nie mogą być parametrami → enum/Map jako biała lista.
     *   • '?' w apostrofach to tekst, nie znacznik.
     *
     * PYTANIA KONTROLNE:
     *   1. Co z tej listy może być parametrem ?: wartość w WHERE, liczba w LIMIT, nazwa kolumny w ORDER BY, nazwa tabeli?
     *   2. Co wypisze (stan bazy jak w lekcji, kategoria DOM ma 2 produkty tańsze niż 500 zł: Lampka i Kabel USB_C):
     *        ps = con.prepareStatement("SELECT COUNT(*) FROM products WHERE category = ? AND price < ?");
     *        ps.setString(1, "DOM"); ps.setBigDecimal(2, new BigDecimal("500")); System.out.println(countOf(ps));
     *        ps.setBigDecimal(2, new BigDecimal("50")); System.out.println(countOf(ps));
     *   3. ZNAJDŹ BŁĄD:  ps = con.prepareStatement("SELECT name FROM products WHERE sku IN (?)");
     *                    ps.setString(1, "ELE-001, ELE-002");   // chcemy dwa produkty
     *   4. ZNAJDŹ BŁĄD:  ps = con.prepareStatement("SELECT name FROM products ORDER BY ? DESC");
     *                    ps.setString(1, sortColumnFromForm);
     *   5. Co zwróci executeBatch() dla paczki trzech poprawnych INSERT-ów po jednym wierszu?
     *   6. Ile wierszy znajdzie  WHERE added = ?  po  ps.setNull(1, Types.DATE)  i dlaczego?
     *   7. Jakim wzorcem LIKE (z ESCAPE '!') znajdziesz nazwy zawierające dosłownie „50%”?
     *   8. Dlaczego po INSERT nie pobiera się nowego id przez SELECT MAX(id)?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** Funkcja na połączeniu, która może rzucić SQLException (SqlWork = praca z bazą). */
    @FunctionalInterface
    interface SqlWork<T> {
        T apply(Connection con) throws SQLException;
    }

    /** Tworzy NOWĄ bazę w pamięci z danymi lekcji, wykonuje pracę i zamyka bazę (każde ćwiczenie ma czyste dane). */
    static <T> T inFreshDb(String dbName, SqlWork<T> work) {
        try (Connection con = DriverManager.getConnection("jdbc:h2:mem:" + dbName)) {
            createShop(con);
            return work.apply(con);
        } catch (SQLException e) {
            throw new IllegalStateException("błąd SQL, SQLState " + e.getSQLState(), e);
        }
    }

    /** Nowy produkt do wstawienia w ćwiczeniu 2 (NewProduct = nowy produkt). */
    record NewProduct(String sku, String name, String category, BigDecimal price, int stock) {
    }

    static final List<NewProduct> NEW_PRODUCTS = List.of(
            new NewProduct("SPO-004", "Herbata zielona", "SPOZYWCZE", new BigDecimal("18.50"), 40),
            new NewProduct("SPO-005", "Miód lipowy", "SPOZYWCZE", new BigDecimal("32.00"), 25),
            new NewProduct("DOM-004", "Kubek termiczny", "DOM", new BigDecimal("59.99"), 12));

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: KSIAZKI tańsze niż 100 zł", List.of("Czysty kod", "Wzorce projektowe"),
                () -> inFreshDb("jdbc03_e1", c -> exercise1(c, "KSIAZKI", new BigDecimal("100"))));
        Check.equal("ćw. 2: paczka 3 produktów → id", List.of(18L, 19L, 20L),
                () -> inFreshDb("jdbc03_e2", c -> exercise2(c, NEW_PRODUCTS)));
        Check.equal("ćw. 3: produkty z Elektroniki ze stanem ≥ 5", List.of("Ładowarka USB-C 20W", "Słuchawki BT", "Laptop Pro 14"),
                () -> inFreshDb("jdbc03_e3", c -> exercise3(c, "ELEKTRONIKA", 5)));
        Check.equal("ćw. 4: szukaj „usb_” w DOM i ELEKTRONIKA wg ceny", List.of("Kabel USB_C 1m"),
                () -> inFreshDb("jdbc03_e4", c -> exercise4(c, List.of("DOM", "ELEKTRONIKA"), "usb_", "cena")));
        Check.equal("ćw. 4: pusta lista kategorii", List.of(),
                () -> inFreshDb("jdbc03_e4b", c -> exercise4(c, List.of(), "a", "nazwa")));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of("Czysty kod", "Wzorce projektowe"),
                () -> inFreshDb("jdbc03_s1", c -> solution1(c, "KSIAZKI", new BigDecimal("100"))));
        Check.equal("ćw. 2 (wzorzec)", List.of(18L, 19L, 20L),
                () -> inFreshDb("jdbc03_s2", c -> solution2(c, NEW_PRODUCTS)));
        Check.equal("ćw. 3 (wzorzec)", List.of("Ładowarka USB-C 20W", "Słuchawki BT", "Laptop Pro 14"),
                () -> inFreshDb("jdbc03_s3", c -> solution3(c, "ELEKTRONIKA", 5)));
        Check.equal("ćw. 4 (wzorzec)", List.of("Kabel USB_C 1m"),
                () -> inFreshDb("jdbc03_s4", c -> solution4(c, List.of("DOM", "ELEKTRONIKA"), "usb_", "cena")));
        Check.equal("ćw. 4 (wzorzec, pusta lista)", List.of(),
                () -> inFreshDb("jdbc03_s4b", c -> solution4(c, List.of(), "a", "nazwa")));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć nazwy produktów z kategorii {@code category} o cenie mniejszej niż {@code maxPrice},
     * posortowane alfabetycznie. Obie wartości przekaż jako parametry.
     * Podpowiedź: {@code "SELECT name FROM products WHERE category = ? AND price < ? ORDER BY name"}, potem setString,
     * setBigDecimal i pomocnicza metoda names(ps).
     */
    static List<String> exercise1(Connection con, String category, BigDecimal maxPrice) throws SQLException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): wstaw wszystkie produkty z listy JEDNĄ paczką (addBatch/executeBatch) i zwróć listę
     * identyfikatorów nadanych przez bazę (w kolejności wstawiania). W świeżej bazie lekcji jest 17 produktów,
     * więc nowe dostaną 18, 19, 20.
     * Podpowiedź: prepareStatement(sql, Statement.RETURN_GENERATED_KEYS), po executeBatch czytaj getGeneratedKeys().
     */
    static List<Long> exercise2(Connection con, List<NewProduct> products) throws SQLException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ na PreparedStatement poniższy kod, który skleja tekst SQL z wartościami
     * (zwraca nazwy produktów z kategorii o stanie co najmniej minStock, rosnąco wg ceny):
     * <pre>{@code
     * String sql = "SELECT name FROM products WHERE category = '" + category
     *         + "' AND stock >= " + minStock + " ORDER BY price";
     * try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) { ... }
     * }</pre>
     * Podpowiedź: dwa znaczniki ?, setString(1, ...), setInt(2, ...), a apostrofy wokół ? znikają.
     */
    static List<String> exercise3(Connection con, String category, int minStock) throws SQLException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): wyszukiwarka. Zwróć nazwy produktów, których kategoria jest na liście
     * {@code categories}, a nazwa zawiera DOSŁOWNIE tekst {@code text} bez względu na wielkość liter (znaki % i _
     * w tekście nie są wzorcem). Sortuj wg klucza {@code sortKey} („nazwa”, „cena”, „stan” — mapa SORT_KEYS),
     * rosnąco, a przy remisie po sku. Pusta lista kategorii → pusta lista bez pytania bazy.
     * Podpowiedź: IN z nCopies, LOWER(name) LIKE ? ESCAPE '!', parametr "%" + escapeLike(text.toLowerCase(Locale.ROOT)) + "%",
     * kolumna sortowania z SORT_KEYS (nieznany klucz → IllegalArgumentException).
     */
    static List<String> exercise4(Connection con, List<String> categories, String text, String sortKey) throws SQLException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solution1(Connection con, String category, BigDecimal maxPrice) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT name FROM products WHERE category = ? AND price < ? ORDER BY name")) {
            ps.setString(1, category);
            ps.setBigDecimal(2, maxPrice);
            return names(ps);
        }
    }

    static List<Long> solution2(Connection con, List<NewProduct> products) throws SQLException {
        String sql = "INSERT INTO products (sku, name, category, price, stock) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (NewProduct p : products) {
                ps.setString(1, p.sku());
                ps.setString(2, p.name());
                ps.setString(3, p.category());
                ps.setBigDecimal(4, p.price());
                ps.setInt(5, p.stock());
                ps.addBatch();
            }
            ps.executeBatch();
            List<Long> ids = new ArrayList<>();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                while (keys.next()) {
                    ids.add(keys.getLong(1));
                }
            }
            return ids;
        }
    }

    static List<String> solution3(Connection con, String category, int minStock) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT name FROM products WHERE category = ? AND stock >= ? ORDER BY price")) {
            ps.setString(1, category);
            ps.setInt(2, minStock);
            return names(ps);
        }
    }

    static List<String> solution4(Connection con, List<String> categories, String text, String sortKey) throws SQLException {
        SortColumn column = SORT_KEYS.get(sortKey);
        if (column == null) {
            throw new IllegalArgumentException("niedozwolona kolumna sortowania: " + sortKey);
        }
        if (categories.isEmpty()) {
            return List.of();
        }
        String sql = "SELECT name FROM products WHERE category IN ("
                + String.join(", ", Collections.nCopies(categories.size(), "?"))
                + ") AND LOWER(name) LIKE ? ESCAPE '!' ORDER BY " + column.sql + ", sku";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            int i = 1;
            for (String category : categories) {
                ps.setString(i++, category);
            }
            ps.setString(i, "%" + escapeLike(text.toLowerCase(Locale.ROOT)) + "%");
            return names(ps);
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Wartość w WHERE i liczba w LIMIT (to wartości — LIMIT ? użyliśmy w sekcji 9). Nazwa kolumny i nazwa tabeli
     *      to części struktury zapytania — nie mogą być parametrami; wybiera się je z białej listy w kodzie.
     *   2. Najpierw 2 (Lampka biurkowa 129.00 i Kabel USB_C 29.90), potem 1 (tylko Kabel USB_C jest tańszy niż 50).
     *      Parametr 1 („DOM”) został ustawiony z poprzedniego wykonania.
     *   3. Jeden ? to JEDNA wartość: baza szuka produktu o SKU równym dokładnie „ELE-001, ELE-002” → 0 wierszy.
     *      Trzeba zbudować „IN (?, ?)” i ustawić każdy element osobno.
     *   4. Nazwa kolumny nie może być parametrem: parametr to zawsze WARTOŚĆ. H2 przy tekście zgłasza SQLState 22018,
     *      inne bazy mogą po cichu sortować po stałej (czyli wcale). Kolumnę wybierz z białej listy (enum/Map)
     *      i wstaw do SQL zaufany fragment z kodu.
     *   5. Tablicę [1, 1, 1] — po jednym zmienionym wierszu na każdy element paczki.
     *   6. 0 wierszy: to nadal porównanie „= NULL”, które daje UNKNOWN. Użyj IS NULL / IS NOT DISTINCT FROM ?.
     *   7. LIKE ? ESCAPE '!' z parametrem "%50!%%" (czyli "%" + escapeLike("50%") + "%").
     *   8. Bo w tym samym czasie inna transakcja mogła wstawić własny wiersz — MAX(id) może być cudzy. Klucz
     *      z getGeneratedKeys dotyczy dokładnie wiersza wstawionego tym poleceniem.
     */
    // </editor-fold>
}
