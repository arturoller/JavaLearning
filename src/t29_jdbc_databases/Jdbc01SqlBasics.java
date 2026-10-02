package t29_jdbc_databases;

import helpers.Check;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Podstawy SQL — tabele, ograniczenia, INSERT/SELECT/UPDATE/DELETE i NULL
 *        (SQL = Structured Query Language = strukturalny język zapytań)
 *
 * W SKRÓCIE:
 *   Relacyjna baza danych przechowuje dane w tabelach (wiersze × kolumny). Językiem SQL tworzymy tabele,
 *   wstawiamy, czytamy, zmieniamy i usuwamy wiersze. Baza sama pilnuje reguł (ograniczeń), np. że cena
 *   nie jest ujemna, a e-mail się nie powtarza. W tej lekcji poznajesz SQL; Java (JDBC) służy tylko do
 *   wysłania poleceń i wypisania wyników — szczegóły JDBC są w następnej lekcji.
 *
 * ANALOGIA:
 *   Tabela to arkusz w segregatorze: nagłówki kolumn są wydrukowane na górze (schemat), każdy wiersz to
 *   jedna karta (rekord), a numer karty (klucz główny) jest unikalny. Sekretarka (baza danych) nie przyjmie
 *   karty bez wymaganych pól ani z drugim takim samym numerem.
 *
 * JAK TO DZIAŁA:
 *   baza (database) → schematy (schema) → tabele (table) → wiersze (row) × kolumny (column)
 *   Każda kolumna ma TYP:  INT / BIGINT (liczby całkowite), DECIMAL(10,2) (kwoty: 10 cyfr, 2 po przecinku),
 *                          VARCHAR(n) (tekst do n znaków), BOOLEAN, DATE, TIMESTAMP (data z godziną).
 *   Kategorie poleceń SQL:
 *     DDL (Data Definition Language = definiowanie struktury):  CREATE, ALTER, DROP
 *     DML (Data Manipulation Language = zmiana danych):         INSERT, UPDATE, DELETE
 *     DQL (Data Query Language = odpytywanie):                  SELECT
 *     TCL (Transaction Control = sterowanie transakcjami):      COMMIT, ROLLBACK (lekcja Jdbc04)
 *     DCL (Data Control = uprawnienia):                         GRANT, REVOKE
 *   Logiczna kolejność wykonania SELECT (inna niż kolejność pisania!):
 *     FROM → WHERE → GROUP BY → HAVING → SELECT → ORDER BY → LIMIT/OFFSET
 *
 * SŁÓWKA:
 *   table = tabela; row = wiersz; column = kolumna; primary key = klucz główny; constraint = ograniczenie;
 *   unique = unikalny; check = sprawdź (warunek); default = domyślny; insert into = wstaw do; values = wartości;
 *   select = wybierz; from = z; where = gdzie; order by = uporządkuj według; update = zaktualizuj; set = ustaw;
 *   delete = usuń; null = brak wartości; like = podobny do; between = pomiędzy
 *
 * ZOBACZ TEŻ: t29_jdbc_databases/Jdbc02Connection (jak działa połączenie z bazą),
 *             t29_jdbc_databases/Jdbc06SqlJoins (wiele tabel naraz),
 *             t29_jdbc_databases/Jdbc07SqlAggregationIndexes (grupowanie i indeksy)
 * </pre>
 */
public class Jdbc01SqlBasics {

    /** Adres bazy H2 w pamięci (mem = memory = pamięć); baza znika, gdy zamkniemy ostatnie połączenie. */
    private static final String URL = "jdbc:h2:mem:jdbc01";

    public static void main(String[] args) throws SQLException {
        title("Jdbc01 — podstawy SQL: tabele, zapytania, NULL");

        // getConnection = pobierz połączenie; try-with-resources zamknie je na końcu (więcej: Jdbc02Connection)
        try (Connection con = DriverManager.getConnection(URL)) {
            relationalModel(con);      // relational model = model relacyjny
            sqlCategories(con);        // sql categories = kategorie poleceń SQL
            constraints(con);          // constraints = ograniczenia
            selectBasics(con);         // select basics = podstawy SELECT
            filtering(con);            // filtering = filtrowanie (LIKE, IN, BETWEEN)
            nullSemantics(con);        // null semantics = znaczenie NULL
            updateDeletePitfall(con);  // update/delete pitfall = pułapka UPDATE/DELETE
            logicalOrder(con);         // logical order = logiczna kolejność wykonania
        }
        exercises();                   // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. MODEL RELACYJNY — TABELA, WIERSZ, KOLUMNA, KLUCZ GŁÓWNY
    // =================================================================================================

    /**
     * 1. Tabela ma stały schemat (nazwy i typy kolumn), a dane to wiersze. Klucz główny (PRIMARY KEY) jednoznacznie
     * identyfikuje wiersz. Tworzymy tabelę produktów podobną do SampleData i wypisujemy ją pomocniczą metodą
     * {@code printTable}, która formatuje wyniki jak w konsoli bazy danych.
     */
    static void relationalModel(Connection con) throws SQLException {
        section("1. Model relacyjny — tabela, wiersz, kolumna, klucz główny");

        // runScript = uruchom skrypt: dzieli tekst na polecenia po ';' i wykonuje je po kolei
        runScript(con, SHOP_SCHEMA);
        runScript(con, SHOP_DATA);

        printTable(con, "SELECT sku, name, category, price, stock FROM products WHERE category = 'ELEKTRONIKA' ORDER BY sku");
        // WYNIK: SKU     | NAME            | CATEGORY    | PRICE   | STOCK
        // WYNIK: --------+-----------------+-------------+---------+------
        // WYNIK: ELE-001 | Laptop Pro 14   | ELEKTRONIKA | 5499.99 | 7
        // WYNIK: ELE-002 | Smartfon X      | ELEKTRONIKA | 2999.00 | 0
        // WYNIK: ELE-003 | Słuchawki BT    | ELEKTRONIKA | 349.90  | 25
        // WYNIK: ELE-004 | Monitor 27 cali | ELEKTRONIKA | 1299.00 | 4
        // WYNIK: (wierszy: 4)

        // Nazwy kolumn wróciły WIELKIMI literami: standard SQL zamienia identyfikatory bez cudzysłowów
        // na wielkie litery (H2, Oracle). PostgreSQL robi odwrotnie — zamienia je na małe.
        // Wniosek: w SQL-u pisz nazwy bez cudzysłowów i nie polegaj na wielkości liter.

        // DOBRA PRAKTYKA: kwoty trzymaj w DECIMAL(p,s) (w Javie BigDecimal), nigdy w DOUBLE/REAL —
        // dlaczego? DOUBLE jest binarny i 0.1 + 0.2 nie daje dokładnie 0.3 (patrz t15_numbers/Numbers01BigDecimal).
        // DECIMAL(10,2) zawsze pokazuje 2 miejsca po przecinku: 2999 zapisało się jako 2999.00.

        // PUŁAPKA: tabela NIE ma żadnej „naturalnej” kolejności wierszy. Bez ORDER BY baza może zwrócić
        // wiersze w dowolnym porządku (zależnym od indeksów, wersji, planu zapytania). Dlatego w tym dziale
        // KAŻDE wypisywane zapytanie ma ORDER BY.
    }

    // =================================================================================================
    // 2. KATEGORIE POLECEŃ — DDL, DML, DQL
    // =================================================================================================

    /**
     * 2. DDL zmienia strukturę (CREATE/ALTER/DROP), DML zmienia dane (INSERT/UPDATE/DELETE), DQL czyta dane (SELECT).
     * W JDBC polecenia DML zwracają liczbę zmienionych wierszy ({@code executeUpdate}), a DDL zwraca 0.
     */
    static void sqlCategories(Connection con) throws SQLException {
        section("2. Kategorie poleceń — DDL, DML, DQL");

        try (Statement st = con.createStatement()) {   // createStatement = utwórz polecenie
            // executeUpdate = wykonaj zmianę; zwraca liczbę zmienionych wierszy
            int ddl = st.executeUpdate("CREATE TABLE notes (id INT PRIMARY KEY, text VARCHAR(100))");
            show("DDL  CREATE TABLE → wynik", ddl);
            // WYNIK: DDL  CREATE TABLE → wynik → 0

            // INSERT INTO tabela (kolumny) VALUES (...), (...) — wiele wierszy jednym poleceniem
            int inserted = st.executeUpdate("INSERT INTO notes (id, text) VALUES (1, 'pierwsza'), (2, 'druga'), (3, 'trzecia')");
            show("DML  INSERT → wstawiono wierszy", inserted);
            // WYNIK: DML  INSERT → wstawiono wierszy → 3

            // ALTER TABLE ... ADD COLUMN = zmień tabelę, dodaj kolumnę (istniejące wiersze dostaną DEFAULT)
            st.executeUpdate("ALTER TABLE notes ADD COLUMN pinned BOOLEAN DEFAULT FALSE NOT NULL");
            int updated = st.executeUpdate("UPDATE notes SET pinned = TRUE WHERE id = 2");
            show("DML  UPDATE → zmieniono wierszy", updated);
            // WYNIK: DML  UPDATE → zmieniono wierszy → 1
        }
        printTable(con, "SELECT id, text, pinned FROM notes ORDER BY id");
        // WYNIK: ID | TEXT     | PINNED
        // WYNIK: ---+----------+-------
        // WYNIK: 1  | pierwsza | FALSE
        // WYNIK: 2  | druga    | TRUE
        // WYNIK: 3  | trzecia  | FALSE
        // WYNIK: (wierszy: 3)

        try (Statement st = con.createStatement()) {
            st.executeUpdate("DROP TABLE notes");   // DROP TABLE = usuń tabelę RAZEM z danymi
        }
        note("DROP TABLE usuwa strukturę i wszystkie dane — nie ma kosza");
        // WYNIK: ℹ DROP TABLE usuwa strukturę i wszystkie dane — nie ma kosza

        // DOBRA PRAKTYKA: zawsze wymieniaj kolumny w INSERT: INSERT INTO notes (id, text) VALUES (...).
        // Dlaczego? Wersja bez listy kolumn (INSERT INTO notes VALUES (...)) psuje się, gdy ktoś doda kolumnę
        // albo zmieni ich kolejność. Z tego samego powodu w kodzie unikaj SELECT * — wybieraj kolumny po nazwie.
    }

    // =================================================================================================
    // 3. OGRANICZENIA — PRIMARY KEY, NOT NULL, UNIQUE, CHECK, DEFAULT
    // =================================================================================================

    /**
     * 3. Ograniczenia (constraints) to reguły pilnowane przez bazę przy każdym INSERT/UPDATE. Złamanie reguły kończy
     * się {@code SQLException} z kodem SQLState — 5 znaków ze standardu SQL. Klasa 23 oznacza naruszenie spójności
     * danych. Komunikatu wyjątku (getMessage) NIE wypisujemy: H2 dokleja do niego numer wersji i tłumaczy go na
     * język systemu, więc u każdego wyglądałby inaczej.
     */
    static void constraints(Connection con) throws SQLException {
        section("3. Ograniczenia — PRIMARY KEY, NOT NULL, UNIQUE, CHECK, DEFAULT");

        // Tabela customers (z SHOP_SCHEMA) ma: id PRIMARY KEY, name NOT NULL, email UNIQUE,
        // vip BOOLEAN DEFAULT FALSE. Tabela products ma CHECK (price > 0) i CHECK (stock >= 0).
        expectSqlError(con, "drugi klient z id = 1",
                "INSERT INTO customers (id, name, city) VALUES (1, 'Ktoś', 'Łódź')");
        // WYNIK: ✔ drugi klient z id = 1 → SQLState 23505 (duplikat klucza / UNIQUE)

        expectSqlError(con, "klient bez nazwy",
                "INSERT INTO customers (id, city) VALUES (8, 'Łódź')");
        // WYNIK: ✔ klient bez nazwy → SQLState 23502 (NULL w kolumnie NOT NULL)

        expectSqlError(con, "powtórzony e-mail",
                "INSERT INTO customers (id, name, city, email) VALUES (8, 'Jan Bis', 'Łódź', 'jan@example.com')");
        // WYNIK: ✔ powtórzony e-mail → SQLState 23505 (duplikat klucza / UNIQUE)

        expectSqlError(con, "ujemna cena",
                "INSERT INTO products (sku, name, category, price) VALUES ('X-1', 'Błąd', 'DOM', -5.00)");
        // WYNIK: ✔ ujemna cena → SQLState 23513 (naruszenie CHECK)

        // DEFAULT: pominięte kolumny dostają wartość domyślną (stock → 0, vip → FALSE)
        try (Statement st = con.createStatement()) {
            st.executeUpdate("INSERT INTO products (sku, name, category, price) VALUES ('DOM-003', 'Koc', 'DOM', 89.00)");
            st.executeUpdate("INSERT INTO customers (id, name, city) VALUES (8, 'Leon Bąk', 'Łódź')");
        }
        printTable(con, "SELECT sku, name, stock FROM products WHERE sku = 'DOM-003'");
        // WYNIK: SKU     | NAME | STOCK
        // WYNIK: --------+------+------
        // WYNIK: DOM-003 | Koc  | 0
        // WYNIK: (wierszy: 1)
        printTable(con, "SELECT id, name, email, vip FROM customers WHERE id = 8");
        // WYNIK: ID | NAME     | EMAIL | VIP
        // WYNIK: ---+----------+-------+------
        // WYNIK: 8  | Leon Bąk | NULL  | FALSE
        // WYNIK: (wierszy: 1)

        // PUŁAPKA: UNIQUE nie blokuje wielu NULL-i. Klienci 2, 5 i 8 nie mają e-maila i baza to przyjęła,
        // bo NULL ≠ NULL (patrz sekcja 6). Tak działają H2, PostgreSQL i MySQL; SQL Server dopuszcza tylko jeden NULL.

        // DOBRA PRAKTYKA: reguły biznesowe, które MUSZĄ być zawsze prawdziwe (unikalny e-mail, cena > 0),
        // zapisz jako ograniczenia w bazie, nie tylko w Javie. Dlaczego? Do bazy piszą też inne programy,
        // skrypty i ludzie z konsolą SQL — walidacja w Javie ich nie zatrzyma.

        // Sprzątanie: usuwamy dodane wiersze, żeby dalsze przykłady miały dane jak w SampleData
        try (Statement st = con.createStatement()) {
            st.executeUpdate("DELETE FROM products WHERE sku = 'DOM-003'");
            st.executeUpdate("DELETE FROM customers WHERE id = 8");
        }
    }

    // =================================================================================================
    // 4. SELECT — WHERE, ORDER BY, LIMIT/OFFSET
    // =================================================================================================

    /**
     * 4. SELECT wybiera kolumny, WHERE filtruje wiersze, ORDER BY sortuje (ASC rosnąco — domyślnie, DESC malejąco),
     * LIMIT ogranicza liczbę wierszy, OFFSET pomija początkowe. Wyrażenia w SELECT mogą liczyć nowe kolumny,
     * a AS nadaje im nazwę (alias).
     */
    static void selectBasics(Connection con) throws SQLException {
        section("4. SELECT — WHERE, ORDER BY, LIMIT/OFFSET");

        // 3 najdroższe produkty dostępne w magazynie; price * stock = wartość zapasu (wyrażenie z aliasem)
        printTable(con, """
                SELECT name, price, stock, price * stock AS stock_value
                FROM products
                WHERE stock > 0
                ORDER BY price DESC
                LIMIT 3""");
        // WYNIK: NAME            | PRICE   | STOCK | STOCK_VALUE
        // WYNIK: ----------------+---------+-------+------------
        // WYNIK: Laptop Pro 14   | 5499.99 | 7     | 38499.93
        // WYNIK: Ekspres do kawy | 1899.00 | 2     | 3798.00
        // WYNIK: Monitor 27 cali | 1299.00 | 4     | 5196.00
        // WYNIK: (wierszy: 3)

        // Sortowanie po dwóch kolumnach: najpierw kategoria rosnąco, w jej obrębie cena malejąco.
        // OFFSET 2 = pomiń 2 pierwsze wiersze — tak buduje się strony wyników (strona 2 po 2 wiersze).
        printTable(con, """
                SELECT category, name, price
                FROM products
                ORDER BY category, price DESC
                LIMIT 2 OFFSET 2""");
        // WYNIK: CATEGORY    | NAME          | PRICE
        // WYNIK: ------------+---------------+--------
        // WYNIK: ELEKTRONIKA | Laptop Pro 14 | 5499.99
        // WYNIK: ELEKTRONIKA | Smartfon X    | 2999.00
        // WYNIK: (wierszy: 2)

        // LIMIT/OFFSET rozumieją H2, PostgreSQL i MySQL. Standard SQL zapisuje to inaczej:
        //   ... OFFSET 2 ROWS FETCH FIRST 2 ROWS ONLY   (H2, PostgreSQL, Oracle 12c+, SQL Server — z ORDER BY)
        printTable(con, """
                SELECT category, name, price
                FROM products
                ORDER BY category, price DESC
                OFFSET 2 ROWS FETCH FIRST 2 ROWS ONLY""");
        // WYNIK: CATEGORY    | NAME          | PRICE
        // WYNIK: ------------+---------------+--------
        // WYNIK: ELEKTRONIKA | Laptop Pro 14 | 5499.99
        // WYNIK: ELEKTRONIKA | Smartfon X    | 2999.00
        // WYNIK: (wierszy: 2)

        // PUŁAPKA: LIMIT bez ORDER BY zwraca „jakieś” 3 wiersze — nie „pierwsze 3”. Stronicowanie bez ORDER BY
        // potrafi pokazać ten sam wiersz na dwóch stronach albo pominąć inny.
        // PUŁAPKA: sortowanie tekstów. H2 domyślnie porównuje kody znaków Unicode, więc „Łódź” trafi ZA „Zakopane”
        // (Ł ma kod większy niż Z). PostgreSQL/MySQL sortują według kolacji (collation = reguł porównywania)
        // ustawionej dla bazy — z polską kolacją kolejność będzie słownikowa. Porównaj: Collator w Javie.
        printTable(con, "SELECT DISTINCT city FROM customers ORDER BY city");
        // WYNIK: CITY
        // WYNIK: --------
        // WYNIK: Gdańsk
        // WYNIK: Kraków
        // WYNIK: Poznań
        // WYNIK: Warszawa
        // WYNIK: Wrocław
        // WYNIK: (wierszy: 5)
        // DISTINCT = różne — usuwa powtórzone wiersze wyniku (Warszawa i Kraków występują po 2 razy).
    }

    // =================================================================================================
    // 5. FILTROWANIE — LIKE, IN, BETWEEN, AND/OR
    // =================================================================================================

    /**
     * 5. LIKE porównuje z wzorcem: {@code %} = dowolny ciąg znaków (także pusty), {@code _} = dokładnie jeden znak.
     * IN sprawdza przynależność do listy, BETWEEN a AND b — zakres WŁĄCZNIE z obiema granicami.
     */
    static void filtering(Connection con) throws SQLException {
        section("5. Filtrowanie — LIKE, IN, BETWEEN");

        // Polskie znaki w LIKE działają normalnie — porównywane są znaki Unicode
        printTable(con, "SELECT name FROM products WHERE name LIKE '%kaw%' ORDER BY name");
        // WYNIK: NAME
        // WYNIK: ---------------
        // WYNIK: Ekspres do kawy
        // WYNIK: (wierszy: 1)

        // PUŁAPKA: LIKE w H2 i PostgreSQL rozróżnia wielkość liter — „%kaw%” nie znalazł „Kawa ziarnista 1kg”.
        // Rozwiązanie przenośne: LOWER(kolumna) LIKE 'wzorzec małymi literami' (LOWER = małe litery, zna też Ł → ł).
        // H2 i PostgreSQL mają też ILIKE (ignoruj wielkość liter). W MySQL domyślna kolacja i tak ignoruje
        // wielkość liter (a nawet ogonki!) — ten sam SQL daje tam inny wynik.
        printTable(con, "SELECT name FROM products WHERE LOWER(name) LIKE '%kaw%' ORDER BY name");
        // WYNIK: NAME
        // WYNIK: ------------------
        // WYNIK: Ekspres do kawy
        // WYNIK: Kawa ziarnista 1kg
        // WYNIK: (wierszy: 2)

        // _ = dokładnie jeden znak: SKU z kategorii KSI lub ELE kończące się na 1
        printTable(con, "SELECT sku, name FROM products WHERE sku LIKE '___-__1' AND category IN ('KSIAZKI', 'ELEKTRONIKA') ORDER BY sku");
        // WYNIK: SKU     | NAME
        // WYNIK: --------+--------------
        // WYNIK: ELE-001 | Laptop Pro 14
        // WYNIK: KSI-001 | Czysty kod
        // WYNIK: (wierszy: 2)

        // BETWEEN 79 AND 129 = price >= 79 AND price <= 129 — obie granice WŁĄCZONE
        printTable(con, "SELECT name, price FROM products WHERE price BETWEEN 79 AND 129 ORDER BY price, name");
        // WYNIK: NAME              | PRICE
        // WYNIK: ------------------+-------
        // WYNIK: Czysty kod        | 79.00
        // WYNIK: Wzorce projektowe | 99.00
        // WYNIK: Java. Podstawy    | 129.00
        // WYNIK: Lampka biurkowa   | 129.00
        // WYNIK: (wierszy: 4)

        // PUŁAPKA: AND wiąże mocniej niż OR (jak * i + w matematyce). Bez nawiasów:
        //   WHERE category = 'DOM' OR category = 'ODZIEZ' AND price > 100
        // znaczy: DOM (dowolna cena) LUB (ODZIEZ droższa niż 100). Stawiaj nawiasy, nawet gdy „wiesz”.
        printTable(con, "SELECT name, price FROM products WHERE category = 'DOM' OR category = 'ODZIEZ' AND price > 100 ORDER BY name");
        // WYNIK: NAME            | PRICE
        // WYNIK: ----------------+--------
        // WYNIK: Ekspres do kawy | 1899.00
        // WYNIK: Kurtka zimowa   | 459.00
        // WYNIK: Lampka biurkowa | 129.00
        // WYNIK: (wierszy: 3)
        printTable(con, "SELECT name, price FROM products WHERE (category = 'DOM' OR category = 'ODZIEZ') AND price > 200 ORDER BY name");
        // WYNIK: NAME            | PRICE
        // WYNIK: ----------------+--------
        // WYNIK: Ekspres do kawy | 1899.00
        // WYNIK: Kurtka zimowa   | 459.00
        // WYNIK: (wierszy: 2)
    }

    // =================================================================================================
    // 6. NULL — LOGIKA TRÓJWARTOŚCIOWA
    // =================================================================================================

    /**
     * 6. NULL znaczy „nie wiadomo / brak wartości”. Każde porównanie z NULL daje UNKNOWN (nieznane), a WHERE
     * przepuszcza tylko wiersze z wynikiem TRUE. Stąd logika trójwartościowa: TRUE, FALSE, UNKNOWN
     * (w JDBC UNKNOWN wraca jako NULL).
     */
    static void nullSemantics(Connection con) throws SQLException {
        section("6. NULL — logika trójwartościowa");

        // PUŁAPKA: „= NULL” nigdy nie jest prawdą — zapytanie NIE rzuca błędu, po prostu zwraca 0 wierszy
        printTable(con, "SELECT name FROM customers WHERE email = NULL ORDER BY id");
        // WYNIK: NAME
        // WYNIK: ----
        // WYNIK: (wierszy: 0)

        // Poprawnie: IS NULL / IS NOT NULL
        printTable(con, "SELECT id, name, email FROM customers WHERE email IS NULL ORDER BY id");
        // WYNIK: ID | NAME        | EMAIL
        // WYNIK: ---+-------------+------
        // WYNIK: 2  | Maria Nowak | NULL
        // WYNIK: 5  | Ola Pawlak  | NULL
        // WYNIK: (wierszy: 2)

        // Tabela prawdy z NULL (UNKNOWN). Uwaga: SELECT bez FROM działa w H2, PostgreSQL, MySQL; Oracle przed wersją 23 wymaga FROM DUAL.
        printTable(con, """
                SELECT NULL = NULL AS eq, NULL AND FALSE AS and_false, NULL AND TRUE AS and_true,
                       NULL OR TRUE AS or_true, NOT (1 = NULL) AS not_unknown, 1 + NULL AS plus""");
        // WYNIK: EQ   | AND_FALSE | AND_TRUE | OR_TRUE | NOT_UNKNOWN | PLUS
        // WYNIK: -----+-----------+----------+---------+-------------+-----
        // WYNIK: NULL | FALSE     | NULL     | TRUE    | NULL        | NULL
        // WYNIK: (wierszy: 1)
        // Czytaj tak: „nie wiem” AND FALSE = FALSE (bo i tak fałsz), „nie wiem” OR TRUE = TRUE, NOT „nie wiem” = „nie wiem”.
        // Arytmetyka z NULL daje NULL: 1 + NULL = NULL.

        // PUŁAPKA: NOT IN z NULL na liście. city NOT IN ('Kraków', NULL) = city <> 'Kraków' AND city <> NULL,
        // a „city <> NULL” to UNKNOWN → cały warunek nigdy nie jest TRUE → 0 wierszy.
        printTable(con, "SELECT name FROM customers WHERE city NOT IN ('Kraków', NULL) ORDER BY id");
        // WYNIK: NAME
        // WYNIK: ----
        // WYNIK: (wierszy: 0)
        // Ta pułapka zwykle wraca jako NOT IN (podzapytanie), które zwraca choć jeden NULL — patrz Jdbc06SqlJoins (NOT EXISTS).

        // COALESCE(a, b, ...) = pierwsza wartość różna od NULL — zastępuje brak wartością domyślną
        printTable(con, "SELECT name, COALESCE(email, '(brak e-maila)') AS contact FROM customers WHERE city = 'Kraków' ORDER BY id");
        // WYNIK: NAME        | CONTACT
        // WYNIK: ------------+------------------
        // WYNIK: Maria Nowak | (brak e-maila)
        // WYNIK: Marek Król  | marek@example.com
        // WYNIK: (wierszy: 2)

        // PUŁAPKA: gdzie trafiają NULL-e przy ORDER BY, zależy od bazy: H2 i MySQL stawiają je na początku
        // przy ASC, PostgreSQL i Oracle — na końcu. Jeśli to ważne, napisz jawnie: ORDER BY email NULLS LAST
        // (H2, PostgreSQL, Oracle; MySQL tego nie zna).
        printTable(con, "SELECT name, email FROM customers WHERE city IN ('Kraków', 'Poznań') ORDER BY email NULLS LAST, name");
        // WYNIK: NAME        | EMAIL
        // WYNIK: ------------+------------------
        // WYNIK: Marek Król  | marek@example.com
        // WYNIK: Maria Nowak | NULL
        // WYNIK: Ola Pawlak  | NULL
        // WYNIK: (wierszy: 3)
    }

    // =================================================================================================
    // 7. UPDATE I DELETE — PUŁAPKA BRAKUJĄCEGO WHERE
    // =================================================================================================

    /**
     * 7. UPDATE i DELETE bez WHERE działają na WSZYSTKICH wierszach — baza nie pyta „czy na pewno?”.
     * Ćwiczymy na kopii tabeli: {@code CREATE TABLE ... AS SELECT} tworzy tabelę z wyniku zapytania.
     */
    static void updateDeletePitfall(Connection con) throws SQLException {
        section("7. UPDATE i DELETE — pułapka brakującego WHERE");

        try (Statement st = con.createStatement()) {
            st.executeUpdate("CREATE TABLE products_copy AS SELECT * FROM products");

            // Zamierzone: podwyżka 10% tylko dla książek
            int good = st.executeUpdate("UPDATE products_copy SET price = price * 1.10 WHERE category = 'KSIAZKI'");
            show("UPDATE z WHERE → zmieniono wierszy", good);
            // WYNIK: UPDATE z WHERE → zmieniono wierszy → 3

            // PUŁAPKA: zapomniany WHERE — przeceniamy CAŁY sklep
            int bad = st.executeUpdate("UPDATE products_copy SET price = price * 0.5");
            show("UPDATE bez WHERE → zmieniono wierszy", bad);
            // WYNIK: UPDATE bez WHERE → zmieniono wierszy → 14

            int deleted = st.executeUpdate("DELETE FROM products_copy WHERE stock = 0");
            show("DELETE z WHERE → usunięto wierszy", deleted);
            // WYNIK: DELETE z WHERE → usunięto wierszy → 2

            int all = st.executeUpdate("DELETE FROM products_copy");
            show("DELETE bez WHERE → usunięto wierszy", all);
            // WYNIK: DELETE bez WHERE → usunięto wierszy → 12

            st.executeUpdate("DROP TABLE products_copy");
        }
        printTable(con, "SELECT name, price FROM products WHERE category = 'KSIAZKI' ORDER BY sku");
        // WYNIK: NAME              | PRICE
        // WYNIK: ------------------+-------
        // WYNIK: Czysty kod        | 79.00
        // WYNIK: Java. Podstawy    | 129.00
        // WYNIK: Wzorce projektowe | 99.00
        // WYNIK: (wierszy: 3)
        // Oryginał nietknięty — psuliśmy tylko kopię.

        // DOBRA PRAKTYKA: zanim uruchomisz UPDATE/DELETE ręcznie, wykonaj SELECT COUNT(*) z TYM SAMYM WHERE
        // i sprawdź liczbę. Dlaczego? To jedyny moment, w którym pomyłka nic nie kosztuje. Na produkcji pracuj
        // w transakcji (Jdbc04Transactions), żeby móc zrobić ROLLBACK. W kodzie sprawdzaj liczbę zwróconą
        // przez executeUpdate — 0 często oznacza błąd (np. zły identyfikator).
        // TRUNCATE TABLE = szybkie usunięcie wszystkich wierszy (DDL; w części baz nie da się go wycofać).
    }

    // =================================================================================================
    // 8. LOGICZNA KOLEJNOŚĆ WYKONANIA SELECT
    // =================================================================================================

    /**
     * 8. Piszemy SELECT … FROM … WHERE … ORDER BY, ale baza wykonuje: FROM → WHERE → SELECT → ORDER BY → LIMIT.
     * Dlatego alias z SELECT jest znany w ORDER BY, ale NIE w WHERE (WHERE działa wcześniej).
     */
    static void logicalOrder(Connection con) throws SQLException {
        section("8. Logiczna kolejność wykonania SELECT");

        // PUŁAPKA: alias w WHERE — kolumna „STOCK_VALUE” jeszcze nie istnieje, gdy wykonuje się WHERE
        expectSqlError(con, "alias z SELECT użyty w WHERE",
                "SELECT name, price * stock AS stock_value FROM products WHERE stock_value > 5000");
        // WYNIK: ✔ alias z SELECT użyty w WHERE → SQLState 42S22 (nieznana kolumna)

        // Poprawnie: powtórz wyrażenie w WHERE; alias wolno użyć w ORDER BY (działa po SELECT)
        printTable(con, """
                SELECT name, price * stock AS stock_value
                FROM products
                WHERE price * stock > 5000
                ORDER BY stock_value DESC""");
        // WYNIK: NAME               | STOCK_VALUE
        // WYNIK: -------------------+------------
        // WYNIK: Laptop Pro 14      | 38499.93
        // WYNIK: Słuchawki BT       | 8747.50
        // WYNIK: Kawa ziarnista 1kg | 7798.80
        // WYNIK: Kurtka zimowa      | 5508.00
        // WYNIK: Monitor 27 cali    | 5196.00
        // WYNIK: (wierszy: 5)

        // MySQL pozwala użyć aliasu także w HAVING, PostgreSQL — nie. Trzymaj się zasady „alias tylko
        // w ORDER BY”, a zapytanie zadziała w każdej bazie.
        // DOBRA PRAKTYKA: czytaj zapytanie w kolejności wykonania: „z tabeli products (FROM) weź wiersze,
        // gdzie (WHERE)…, wylicz kolumny (SELECT), posortuj (ORDER BY), utnij (LIMIT)”. Łatwiej wtedy
        // przewidzieć wynik i znaleźć błąd.
    }

    // =================================================================================================
    // NARZĘDZIA LEKCJI — schemat, dane, wypisywanie tabel
    // =================================================================================================

    /** Schemat sklepu: CHECK pilnuje ceny i stanu, UNIQUE — e-maila, DEFAULT uzupełnia pominięte kolumny. */
    static final String SHOP_SCHEMA = """
            CREATE TABLE products (
                sku      VARCHAR(10)   PRIMARY KEY,
                name     VARCHAR(100)  NOT NULL,
                category VARCHAR(20)   NOT NULL,
                price    DECIMAL(10,2) NOT NULL CHECK (price > 0),
                stock    INT           DEFAULT 0 NOT NULL CHECK (stock >= 0)
            );
            CREATE TABLE customers (
                id    INT          PRIMARY KEY,
                name  VARCHAR(100) NOT NULL,
                city  VARCHAR(50)  NOT NULL,
                email VARCHAR(100) UNIQUE,
                vip   BOOLEAN      DEFAULT FALSE NOT NULL
            );
            """;

    /** Dane jak w SampleData.products() i SampleData.customers(). */
    static final String SHOP_DATA = """
            INSERT INTO products (sku, name, category, price, stock) VALUES
                ('ELE-001', 'Laptop Pro 14',      'ELEKTRONIKA', 5499.99,   7),
                ('ELE-002', 'Smartfon X',         'ELEKTRONIKA', 2999.00,   0),
                ('ELE-003', 'Słuchawki BT',       'ELEKTRONIKA',  349.90,  25),
                ('ELE-004', 'Monitor 27 cali',    'ELEKTRONIKA', 1299.00,   4),
                ('SPO-001', 'Kawa ziarnista 1kg', 'SPOZYWCZE',     64.99, 120),
                ('SPO-002', 'Czekolada gorzka',   'SPOZYWCZE',      7.49, 300),
                ('SPO-003', 'Oliwa z oliwek',     'SPOZYWCZE',     42.00,   0),
                ('KSI-001', 'Czysty kod',         'KSIAZKI',       79.00,  15),
                ('KSI-002', 'Java. Podstawy',     'KSIAZKI',      129.00,   9),
                ('KSI-003', 'Wzorce projektowe',  'KSIAZKI',       99.00,   3),
                ('ODZ-001', 'Kurtka zimowa',      'ODZIEZ',       459.00,  12),
                ('ODZ-002', 'T-shirt bawełniany', 'ODZIEZ',        49.99,  80),
                ('DOM-001', 'Ekspres do kawy',    'DOM',         1899.00,   2),
                ('DOM-002', 'Lampka biurkowa',    'DOM',          129.00,  18);
            INSERT INTO customers (id, name, city, email, vip) VALUES
                (1, 'Jan Kowalski',  'Warszawa', 'jan@example.com',        TRUE),
                (2, 'Maria Nowak',   'Kraków',   NULL,                     FALSE),
                (3, 'Adam Mazur',    'Gdańsk',   'adam.mazur@example.com', FALSE),
                (4, 'Zofia Krawczyk','Warszawa', 'zofia@example.com',      TRUE),
                (5, 'Ola Pawlak',    'Poznań',   NULL,                     FALSE),
                (6, 'Marek Król',    'Kraków',   'marek@example.com',      FALSE),
                (7, 'Ewa Lis',       'Wrocław',  'ewa.lis@example.com',    FALSE);
            """;

    /**
     * Wykonuje kilka poleceń oddzielonych średnikami. Uproszczenie: zakłada, że ';' nie występuje w tekstach.
     * (Standard JDBC nie gwarantuje, że jedno execute przyjmie wiele poleceń naraz — dlatego dzielimy sami.)
     */
    static void runScript(Connection con, String script) throws SQLException {
        try (Statement st = con.createStatement()) {
            for (String sql : script.split(";")) {
                if (!sql.isBlank()) {          // isBlank = czy pusty lub same spacje (Java 11+)
                    st.execute(sql);           // execute = wykonaj dowolne polecenie
                }
            }
        }
    }

    /** Wypisuje wynik zapytania jako wyrównaną tabelę; NULL pokazuje jako napis NULL. */
    static void printTable(Connection con, String sql) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {          // executeQuery = wykonaj zapytanie (SELECT)
            ResultSetMetaData meta = rs.getMetaData();       // metadata = dane o danych (nazwy kolumn)
            int columns = meta.getColumnCount();
            List<String[]> rows = new ArrayList<>();
            String[] header = new String[columns];
            for (int i = 0; i < columns; i++) {
                header[i] = meta.getColumnLabel(i + 1);      // kolumny w JDBC liczymy od 1!
            }
            rows.add(header);
            while (rs.next()) {                              // next = przejdź do następnego wiersza
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
                    line.append(String.format("%-" + widths[i] + "s", rows.get(r)[i]));
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
    }

    /** Wykonuje polecenie, które MA się nie udać, i wypisuje SQLState z polskim opisem (bez komunikatu H2). */
    static void expectSqlError(Connection con, String label, String sql) {
        try (Statement st = con.createStatement()) {
            st.execute(sql);
            System.out.println("✘ " + label + " → NIE rzucono wyjątku (a spodziewaliśmy się go)");
        } catch (SQLException e) {
            System.out.println("✔ " + label + " → SQLState " + e.getSQLState() + " (" + describe(e.getSQLState()) + ")");
        }
    }

    /** Polskie opisy kilku kodów SQLState (23xxx = naruszenie spójności, 42xxx = błąd w zapytaniu). */
    static String describe(String sqlState) {
        return switch (sqlState) {
            case "23505" -> "duplikat klucza / UNIQUE";
            case "23502" -> "NULL w kolumnie NOT NULL";
            case "23513" -> "naruszenie CHECK";
            case "42S22" -> "nieznana kolumna";
            default -> "inny błąd";
        };
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • DDL: CREATE/ALTER/DROP (struktura) · DML: INSERT/UPDATE/DELETE (dane) · DQL: SELECT · TCL: COMMIT/ROLLBACK
     *   • INSERT INTO t (k1, k2) VALUES (..), (..)  — zawsze z listą kolumn
     *   • Ograniczenia: PRIMARY KEY (unikalny + NOT NULL), NOT NULL, UNIQUE (wiele NULL-i dozwolone), CHECK (warunek),
     *     DEFAULT (wartość dla pominiętej kolumny). Naruszenie → SQLException z SQLState 23xxx.
     *   • Kolejność wykonania: FROM → WHERE → GROUP BY → HAVING → SELECT → ORDER BY → LIMIT. Alias z SELECT: tylko w ORDER BY.
     *   • Bez ORDER BY kolejność wierszy jest NIEOKREŚLONA. LIMIT/OFFSET (H2, PostgreSQL, MySQL) lub
     *     OFFSET n ROWS FETCH FIRST m ROWS ONLY (standard).
     *   • LIKE: % = dowolny ciąg, _ = jeden znak; wielkość liter: LOWER(kol) LIKE ... (MySQL domyślnie jej nie rozróżnia).
     *   • BETWEEN a AND b — włącznie z granicami. AND wiąże mocniej niż OR → nawiasy.
     *   • NULL: IS NULL / IS NOT NULL, nigdy = NULL. Porównanie z NULL = UNKNOWN; WHERE przepuszcza tylko TRUE.
     *     NOT IN z NULL na liście → 0 wierszy. COALESCE(a, b) = pierwsza wartość nie-NULL.
     *   • UPDATE/DELETE bez WHERE → wszystkie wiersze. Najpierw SELECT COUNT(*) z tym samym WHERE.
     *   • Kwoty: DECIMAL(10,2) ↔ BigDecimal; nigdy DOUBLE.
     *
     * PYTANIA KONTROLNE:
     *   1. Do której kategorii (DDL/DML/DQL) należą: ALTER TABLE, DELETE, SELECT, DROP TABLE?
     *   2. Ile wierszy zwróci:  SELECT name FROM customers WHERE email = NULL  — i dlaczego nie jest to błąd składni?
     *   3. Co wypisze (printTable):  SELECT NULL OR FALSE AS a, NULL OR TRUE AS b  ?
     *   4. ZNAJDŹ BŁĄD:  SELECT name, price * 0.77 AS netto FROM products WHERE netto > 100 ORDER BY netto
     *   5. ZNAJDŹ BŁĄD:  UPDATE customers SET vip = TRUE;  -- miało dotyczyć tylko klienta o id 3
     *   6. Dlaczego kolumna email z UNIQUE przyjęła dwa wiersze z NULL?
     *   7. Ile wierszy zwróci  WHERE price BETWEEN 99 AND 129  na danych lekcji? (podpowiedź: granice)
     *   8. Dlaczego zapytanie z LIMIT 10 bez ORDER BY nie nadaje się do stronicowania?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** Funkcja na połączeniu, która może rzucić SQLException (Supplier tego nie potrafi). SqlWork = praca z bazą. */
    @FunctionalInterface
    interface SqlWork<T> {
        T apply(Connection con) throws SQLException;
    }

    /** Tworzy NOWĄ bazę w pamięci z danymi sklepu, wykonuje pracę i zamyka bazę (każde ćwiczenie ma czyste dane). */
    static <T> T inFreshDb(String dbName, SqlWork<T> work) {
        try (Connection con = DriverManager.getConnection("jdbc:h2:mem:" + dbName)) {
            runScript(con, SHOP_SCHEMA);
            runScript(con, SHOP_DATA);
            return work.apply(con);
        } catch (SQLException e) {
            throw new IllegalStateException("błąd SQL, SQLState " + e.getSQLState(), e);
        }
    }

    /** Pomocnik do ćwiczeń: wykonuje zapytanie i zwraca pierwszą kolumnę każdego wiersza jako tekst. */
    static List<String> queryStrings(Connection con, String sql) throws SQLException {
        List<String> result = new ArrayList<>();
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(rs.getString(1));
            }
        }
        return result;
    }

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: produkty tańsze niż 50 zł, alfabetycznie", List.of("Czekolada gorzka", "Oliwa z oliwek", "T-shirt bawełniany"),
                () -> inFreshDb("jdbc01_e1", Jdbc01SqlBasics::exercise1));
        Check.equal("ćw. 2: klienci bez e-maila z Krakowa lub Poznania", List.of("Maria Nowak", "Ola Pawlak"),
                () -> inFreshDb("jdbc01_e2", Jdbc01SqlBasics::exercise2));
        Check.equal("ćw. 3: podwyżka ELEKTRONIKA dostępnej w magazynie → liczba wierszy", 3,
                () -> inFreshDb("jdbc01_e3", Jdbc01SqlBasics::exercise3));
        Check.equal("ćw. 4: strona 2 (po 4) produktów wg ceny malejąco", List.of("Kurtka zimowa", "Słuchawki BT", "Java. Podstawy", "Lampka biurkowa"),
                () -> inFreshDb("jdbc01_e4", c -> exercise4(c, 2, 4)));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of("Czekolada gorzka", "Oliwa z oliwek", "T-shirt bawełniany"),
                () -> inFreshDb("jdbc01_s1", Jdbc01SqlBasics::solution1));
        Check.equal("ćw. 2 (wzorzec)", List.of("Maria Nowak", "Ola Pawlak"),
                () -> inFreshDb("jdbc01_s2", Jdbc01SqlBasics::solution2));
        Check.equal("ćw. 3 (wzorzec)", 3,
                () -> inFreshDb("jdbc01_s3", Jdbc01SqlBasics::solution3));
        Check.equal("ćw. 4 (wzorzec)", List.of("Kurtka zimowa", "Słuchawki BT", "Java. Podstawy", "Lampka biurkowa"),
                () -> inFreshDb("jdbc01_s4", c -> solution4(c, 2, 4)));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć nazwy produktów o cenie mniejszej niż 50 zł, posortowane alfabetycznie.
     * Podpowiedź: queryStrings(con, "SELECT name FROM products WHERE ... ORDER BY ...").
     */
    static List<String> exercise1(Connection con) throws SQLException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (łatwe): nazwy klientów BEZ e-maila, którzy mieszkają w Krakowie lub Poznaniu, posortowane po id.
     * Podpowiedź: IS NULL (nie = NULL) oraz IN ('Kraków', 'Poznań').
     */
    static List<String> exercise2(Connection con) throws SQLException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): podnieś o 5% ceny produktów z kategorii ELEKTRONIKA, które są w magazynie (stock > 0).
     * Zwróć liczbę zmienionych wierszy.
     * Podpowiedź: try (Statement st = con.createStatement()) { return st.executeUpdate("UPDATE ... SET ... WHERE ... AND ..."); }
     */
    static int exercise3(Connection con) throws SQLException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): stronicowanie. Zwróć nazwy produktów ze strony {@code page} (numerowanej od 1)
     * przy {@code size} wierszach na stronę, sortując po cenie malejąco, a przy równej cenie — po nazwie rosnąco.
     * Podpowiedź: OFFSET = (page - 1) * size. Bez drugiego klucza sortowania (name) kolejność „Java. Podstawy”
     * i „Lampka biurkowa” (obie po 129.00) byłaby nieokreślona!
     */
    static List<String> exercise4(Connection con, int page, int size) throws SQLException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solution1(Connection con) throws SQLException {
        return queryStrings(con, "SELECT name FROM products WHERE price < 50 ORDER BY name");
    }

    static List<String> solution2(Connection con) throws SQLException {
        return queryStrings(con, "SELECT name FROM customers WHERE email IS NULL AND city IN ('Kraków', 'Poznań') ORDER BY id");
    }

    static int solution3(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            return st.executeUpdate("UPDATE products SET price = price * 1.05 WHERE category = 'ELEKTRONIKA' AND stock > 0");
        }
    }

    static List<String> solution4(Connection con, int page, int size) throws SQLException {
        int offset = (page - 1) * size;
        // Liczby wklejamy do SQL tylko dlatego, że to int-y z naszego kodu; tekst od użytkownika → PreparedStatement (Jdbc03)
        return queryStrings(con, "SELECT name FROM products ORDER BY price DESC, name LIMIT " + size + " OFFSET " + offset);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. ALTER TABLE i DROP TABLE — DDL (struktura); DELETE — DML (dane); SELECT — DQL (odczyt).
     *   2. 0 wierszy. Składnia jest poprawna — to zwykłe porównanie, tylko jego wynik zawsze jest UNKNOWN,
     *      a WHERE przepuszcza wyłącznie TRUE. Poprawnie: WHERE email IS NULL.
     *   3. A = NULL (UNKNOWN OR FALSE = UNKNOWN), B = TRUE (cokolwiek OR TRUE = TRUE):
     *        A    | B
     *        -----+-----
     *        NULL | TRUE
     *   4. WHERE wykonuje się przed SELECT, więc alias „netto” jeszcze nie istnieje → błąd (SQLState 42S22 w H2).
     *      Poprawnie: WHERE price * 0.77 > 100; w ORDER BY alias jest dozwolony.
     *   5. Brak WHERE → wszyscy klienci zostają VIP-ami (executeUpdate zwróci 7). Poprawnie: ... WHERE id = 3.
     *   6. Bo NULL nie jest równy NULL — dla UNIQUE dwa „nie wiadomo” nie są duplikatem. (SQL Server jest wyjątkiem.)
     *   7. 3 wiersze: Wzorce projektowe (99.00), Java. Podstawy i Lampka biurkowa (po 129.00). Obie granice są
     *      włączone, więc ceny równe 99 i 129 się łapią; Czysty kod (79.00) jest poniżej zakresu.
     *   8. Bez ORDER BY baza może zwracać wiersze w różnej kolejności przy każdym wykonaniu, więc kolejne strony
     *      mogą się nakładać albo pomijać wiersze. Kolejność sortowania musi też być jednoznaczna (np. dodatkowo po id).
     */
    // </editor-fold>
}
