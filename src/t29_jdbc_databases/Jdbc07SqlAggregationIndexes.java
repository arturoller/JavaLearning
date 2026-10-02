package t29_jdbc_databases;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Customer;
import helpers.model.Order;
import helpers.model.OrderLine;
import helpers.model.OrderStatus;
import helpers.model.Product;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Agregacja w SQL (COUNT, SUM, GROUP BY, HAVING, funkcje okna) i indeksy
 *        (aggregate = funkcja agregująca, zbiorcza; index = indeks)
 *
 * W SKRÓCIE:
 *   Funkcje agregujące zamieniają wiele wierszy w jedną liczbę (ile, suma, średnia), a GROUP BY robi to osobno
 *   dla każdej grupy — jak groupingBy w strumieniach. Baza liczy to tam, gdzie leżą dane, i odsyła tylko wynik.
 *   Indeks to dodatkowa, posortowana struktura, dzięki której baza znajduje wiersze bez czytania całej tabeli —
 *   kosztem wolniejszych zapisów i miejsca na dysku.
 *
 * ANALOGIA:
 *   GROUP BY to rozkładanie paragonów na kupki według sklepu i liczenie sumy każdej kupki. Indeks to skorowidz
 *   na końcu książki: zamiast kartkować 600 stron, sprawdzasz hasło w alfabetycznym spisie i od razu wiesz,
 *   na której stronie szukać. Ale każdą nową stronę trzeba też dopisać do skorowidza.
 *
 * JAK TO DZIAŁA:
 *   FROM → WHERE (filtr WIERSZY) → GROUP BY (kupki) → HAVING (filtr GRUP) → SELECT → ORDER BY → LIMIT
 *   COUNT(*)  — liczba wierszy;  COUNT(kol) — liczba wartości różnych od NULL;  COUNT(DISTINCT kol) — różnych
 *   SUM / AVG / MIN / MAX — pomijają NULL;  SUM pustego zbioru = NULL (nie 0!)
 *   Indeks B-drzewo (B-tree): posortowane klucze w drzewie → wyszukanie {@code =, <, BETWEEN}, „LIKE 'abc%'”
 *   w kilku krokach zamiast przeglądania wszystkich wierszy (pełny skan tabeli).
 *
 * SŁÓWKA:
 *   count = policz; sum = suma; average (AVG) = średnia; group by = grupuj według; having = mający (warunek grupy);
 *   distinct = różne; case when = w przypadku gdy; window function = funkcja okna; partition = podział (część);
 *   row number = numer wiersza; index = indeks; explain = wyjaśnij (plan zapytania); table scan = pełny skan tabeli;
 *   composite = złożony; unique = unikalny; revenue = przychód
 *
 * ZOBACZ TEŻ: t16_streams/Streams11GroupingBy (to samo grupowanie w Javie),
 *             t29_jdbc_databases/Jdbc06SqlJoins (złączenia, na których liczymy sumy),
 *             t29_jdbc_databases/Jdbc01SqlBasics (kolejność wykonania SELECT, NULL)
 * </pre>
 */
public class Jdbc07SqlAggregationIndexes {

    private static final String URL = "jdbc:h2:mem:jdbc07";

    public static void main(String[] args) throws SQLException {
        title("Jdbc07 — agregacja, GROUP BY, funkcje okna i indeksy");

        try (Connection con = DriverManager.getConnection(URL)) {
            createShop(con);               // create shop = utwórz sklep (schemat + dane z SampleData)
            aggregateFunctions(con);       // aggregate functions = funkcje agregujące
            groupBy(con);                  // group by = grupowanie
            havingVersusWhere(con);        // having versus where = HAVING czy WHERE
            aggregationWithJoins(con);     // aggregation with joins = agregacja na złączeniach
            distinctAndCaseWhen(con);      // distinct and case when = DISTINCT i CASE WHEN
            windowFunctions(con);          // window functions = funkcje okna
            indexesAndExplain(con);        // indexes and explain = indeksy i plan zapytania
            compositeIndexes(con);         // composite indexes = indeksy złożone
            uniqueIndexesAndCost(con);     // unique indexes and cost = indeksy unikalne i koszt
            sqlVersusStreams(con);         // SQL versus streams = SQL kontra strumienie
        }
        exercises();                       // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. FUNKCJE AGREGUJĄCE — COUNT, SUM, AVG, MIN, MAX
    // =================================================================================================

    /**
     * 1. Funkcja agregująca bez GROUP BY zwraca JEDEN wiersz dla całej tabeli. COUNT(*) liczy wiersze,
     * COUNT(kolumna) — tylko wartości różne od NULL. Pozostałe funkcje też pomijają NULL.
     */
    static void aggregateFunctions(Connection con) throws SQLException {
        section("1. Funkcje agregujące — COUNT, SUM, AVG, MIN, MAX");

        // printTable = wypisz tabelę (pomocnik na dole pliku, jak w Jdbc01SqlBasics)

        printTable(con, "SELECT COUNT(*) AS all_rows, COUNT(email) AS with_email, COUNT(DISTINCT city) AS cities FROM customers");
        // WYNIK: ALL_ROWS | WITH_EMAIL | CITIES
        // WYNIK: ---------+------------+-------
        // WYNIK: 7        | 5          | 5
        // WYNIK: (wierszy: 1)
        // PUŁAPKA: COUNT(email) to NIE liczba klientów — Maria i Ola nie mają e-maila (NULL), więc 5, a nie 7.

        printTable(con, """
                SELECT SUM(stock) AS total_stock, MIN(price) AS cheapest, MAX(price) AS most_expensive,
                       CAST(AVG(price) AS DECIMAL(10,2)) AS avg_price
                FROM products""");
        // WYNIK: TOTAL_STOCK | CHEAPEST | MOST_EXPENSIVE | AVG_PRICE
        // WYNIK: ------------+----------+----------------+----------
        // WYNIK: 595         | 7.49     | 5499.99        | 936.17
        // WYNIK: (wierszy: 1)
        // AVG na DECIMAL daje wynik z wieloma miejscami po przecinku — CAST(... AS DECIMAL(10,2)) zaokrągla do groszy
        // (w Javie: BigDecimal.setScale(2, RoundingMode.HALF_UP)). Uwaga: AVG na kolumnie INT w H2/PostgreSQL daje
        // wynik ułamkowy, ale w SQL Server — całkowity (obcięty). Przy średnich sprawdzaj typ wyniku w swojej bazie.

        // PUŁAPKA: SUM ze zbioru bez wierszy to NULL, a nie 0. COALESCE(SUM(...), 0) zamienia go na zero.
        printTable(con, """
                SELECT SUM(stock) AS sum_none, COALESCE(SUM(stock), 0) AS sum_or_zero, COUNT(*) AS count_none
                FROM products WHERE category = 'ZABAWKI'""");
        // WYNIK: SUM_NONE | SUM_OR_ZERO | COUNT_NONE
        // WYNIK: ---------+-------------+-----------
        // WYNIK: NULL     | 0           | 0
        // WYNIK: (wierszy: 1)
        // COUNT pustego zbioru = 0, ale SUM/AVG/MIN/MAX = NULL. W JDBC rs.getBigDecimal zwróci wtedy null.
    }

    // =================================================================================================
    // 2. GROUP BY — OSOBNY WYNIK DLA KAŻDEJ GRUPY
    // =================================================================================================

    /**
     * 2. GROUP BY dzieli wiersze na grupy o tej samej wartości kolumny i liczy funkcje agregujące osobno w każdej
     * grupie. W SELECT mogą stać tylko kolumny z GROUP BY i funkcje agregujące.
     */
    static void groupBy(Connection con) throws SQLException {
        section("2. GROUP BY — osobny wynik dla każdej grupy");

        printTable(con, """
                SELECT category, COUNT(*) AS products, SUM(stock) AS stock, MIN(price) AS min_price, MAX(price) AS max_price
                FROM products
                GROUP BY category
                ORDER BY category""");
        // WYNIK: CATEGORY    | PRODUCTS | STOCK | MIN_PRICE | MAX_PRICE
        // WYNIK: ------------+----------+-------+-----------+----------
        // WYNIK: DOM         | 2        | 20    | 129.00    | 1899.00
        // WYNIK: ELEKTRONIKA | 4        | 36    | 349.90    | 5499.99
        // WYNIK: KSIAZKI     | 3        | 27    | 79.00     | 129.00
        // WYNIK: ODZIEZ      | 2        | 92    | 49.99     | 459.00
        // WYNIK: SPOZYWCZE   | 3        | 420   | 7.49      | 64.99
        // WYNIK: (wierszy: 5)

        // expectSqlError = oczekuj błędu SQL (wypisuje SQLState i polski opis z describe = opisz)
        // PUŁAPKA: kolumna spoza GROUP BY w SELECT — której nazwy produktu z grupy baza ma użyć? Błąd.
        expectSqlError(con, "name spoza GROUP BY", "SELECT category, name FROM products GROUP BY category");
        // WYNIK: ✔ name spoza GROUP BY → SQLState 90016 (H2: kolumna musi być w GROUP BY)
        // Starszy MySQL (bez trybu ONLY_FULL_GROUP_BY) przyjmował takie zapytanie i zwracał „jakąś” nazwę z grupy —
        // wynik przypadkowy. MySQL 5.7+ domyślnie też zgłasza błąd.

        // Grupowanie po wyrażeniu: każdy miesiąc daty zamówienia to osobna grupa
        printTable(con, """
                SELECT EXTRACT(MONTH FROM order_date) AS order_month, COUNT(*) AS orders
                FROM orders
                GROUP BY EXTRACT(MONTH FROM order_date)
                ORDER BY order_month""");
        // WYNIK: ORDER_MONTH | ORDERS
        // WYNIK: ------------+-------
        // WYNIK: 1           | 2
        // WYNIK: 2           | 3
        // WYNIK: 3           | 4
        // WYNIK: 4           | 1
        // WYNIK: (wierszy: 4)
        // EXTRACT(MONTH FROM data) = wyciągnij miesiąc (standard SQL; H2, PostgreSQL, MySQL).
        // PUŁAPKA: alias „month” dałby w H2 błąd składni — MONTH to słowo zastrzeżone (tak jak ORDER, USER, VALUE).
        // Dlatego order_month. Nazwy kolumn i aliasów dobieraj tak, żeby nie były słowami kluczowymi SQL.
    }

    // =================================================================================================
    // 3. HAVING CZY WHERE — FILTR GRUP I FILTR WIERSZY
    // =================================================================================================

    /**
     * 3. WHERE działa PRZED grupowaniem i wybiera wiersze; HAVING działa PO grupowaniu i wybiera grupy (może używać
     * funkcji agregujących). Funkcja agregująca w WHERE to błąd — w chwili WHERE grup jeszcze nie ma.
     */
    static void havingVersusWhere(Connection con) throws SQLException {
        section("3. HAVING czy WHERE — filtr grup i filtr wierszy");

        // Kategorie, w których są co najmniej 3 produkty DOSTĘPNE w magazynie
        printTable(con, """
                SELECT category, COUNT(*) AS available
                FROM products
                WHERE stock > 0
                GROUP BY category
                HAVING COUNT(*) >= 3
                ORDER BY category""");
        // WYNIK: CATEGORY    | AVAILABLE
        // WYNIK: ------------+----------
        // WYNIK: ELEKTRONIKA | 3
        // WYNIK: KSIAZKI     | 3
        // WYNIK: (wierszy: 2)
        // WHERE stock > 0 odrzucił Smartfon X i Oliwę jeszcze przed liczeniem; HAVING odrzucił potem małe grupy.

        expectSqlError(con, "COUNT(*) w WHERE", "SELECT category FROM products WHERE COUNT(*) > 2 GROUP BY category");
        // WYNIK: ✔ COUNT(*) w WHERE → SQLState 90054 (H2: niedozwolone użycie funkcji agregującej)

        // DOBRA PRAKTYKA: warunek na zwykłej kolumnie pisz w WHERE, nie w HAVING (HAVING category = 'DOM' też zadziała).
        // Dlaczego? WHERE odrzuca wiersze wcześnie i może użyć indeksu; HAVING najpierw grupuje wszystko.
        // H2 i MySQL pozwalają użyć w HAVING aliasu z SELECT (HAVING available >= 3), PostgreSQL — nie. Przenośnie:
        // powtórz wyrażenie (HAVING COUNT(*) >= 3).
    }

    // =================================================================================================
    // 4. AGREGACJA NA ZŁĄCZENIACH — PRZYCHÓD KLIENTÓW I KATEGORII
    // =================================================================================================

    /**
     * 4. Najpierw JOIN składa wiersze pozycji z cenami i klientami, potem GROUP BY sumuje. Kwoty w SQL są DECIMAL,
     * w Javie odczytujemy je jako BigDecimal. LEFT JOIN + COALESCE pokazuje także klientów bez zamówień.
     */
    static void aggregationWithJoins(Connection con) throws SQLException {
        section("4. Agregacja na złączeniach — przychód klientów i kategorii");

        // Przychód z zamówień nieanulowanych, malejąco (ORDER BY po funkcji agregującej — przez alias)
        printTable(con, """
                SELECT c.name, COUNT(DISTINCT o.id) AS orders, SUM(l.quantity * p.price) AS revenue
                FROM customers c
                JOIN orders o      ON o.customer_id = c.id
                JOIN order_lines l ON l.order_id = o.id
                JOIN products p    ON p.sku = l.sku
                WHERE o.status <> 'ANULOWANE'
                GROUP BY c.id, c.name
                ORDER BY revenue DESC""");
        // WYNIK: NAME           | ORDERS | REVENUE
        // WYNIK: ---------------+--------+--------
        // WYNIK: Jan Kowalski   | 3      | 6669.79
        // WYNIK: Zofia Krawczyk | 2      | 4755.98
        // WYNIK: Maria Nowak    | 2      | 619.77
        // WYNIK: Ola Pawlak     | 1      | 608.97
        // WYNIK: Marek Król     | 1      | 295.45
        // WYNIK: (wierszy: 5)
        // PUŁAPKA: COUNT(*) policzyłby tu POZYCJE, nie zamówienia (Jan ma 3 zamówienia, ale 7 pozycji) — stąd
        // COUNT(DISTINCT o.id). Złączenie z tabelą „wielu” mnoży wiersze (Jdbc06SqlJoins, sekcja 6).
        // GROUP BY c.id, c.name: grupujemy po kluczu (dwóch klientów może mieć to samo nazwisko), a name dodajemy,
        // żeby wolno było ją wypisać.

        // Wszyscy klienci, także bez zamówień: LEFT JOIN, COUNT(o.id) zamiast COUNT(*), COALESCE dla sumy
        printTable(con, """
                SELECT c.name, COUNT(*) AS count_star, COUNT(o.id) AS orders
                FROM customers c
                LEFT JOIN orders o ON o.customer_id = c.id
                WHERE c.city IN ('Gdańsk', 'Wrocław')
                GROUP BY c.id, c.name
                ORDER BY c.id""");
        // WYNIK: NAME       | COUNT_STAR | ORDERS
        // WYNIK: -----------+------------+-------
        // WYNIK: Adam Mazur | 1          | 1
        // WYNIK: Ewa Lis    | 1          | 0
        // WYNIK: (wierszy: 2)
        // PUŁAPKA: przy LEFT JOIN COUNT(*) liczy też wiersz „bez pary” — Ewa Lis ma 1 zamiast 0. Licz kolumnę z prawej
        // tabeli: COUNT(o.id) pomija NULL.

        // Przychód kategorii w Javie: BigDecimal z getBigDecimal, porównania przez compareTo
        Map<String, BigDecimal> byCategory = new TreeMap<>();
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("""
                     SELECT p.category, SUM(l.quantity * p.price) AS revenue
                     FROM order_lines l
                     JOIN orders o   ON o.id = l.order_id
                     JOIN products p ON p.sku = l.sku
                     WHERE o.status = 'DOSTARCZONE'
                     GROUP BY p.category
                     ORDER BY p.category""")) {
            while (rs.next()) {
                byCategory.put(rs.getString("category"), rs.getBigDecimal("revenue"));
            }
        }
        showEach("przychód z DOSTARCZONYCH wg kategorii", byCategory);
        show("ELEKTRONIKA > 8000 zł", byCategory.get("ELEKTRONIKA").compareTo(new BigDecimal("8000")) > 0);
        // WYNIK: przychód z DOSTARCZONYCH wg kategorii (liczba kluczy: 3):
        // WYNIK: • DOM → 129.00
        // WYNIK: • ELEKTRONIKA → 8797.79
        // WYNIK: • SPOZYWCZE → 269.87
        // WYNIK: ELEKTRONIKA > 8000 zł → true
    }

    // =================================================================================================
    // 5. DISTINCT I CASE WHEN — WARTOŚCI WARUNKOWE I AGREGACJA WARUNKOWA
    // =================================================================================================

    /**
     * 5. CASE WHEN warunek THEN wartość … ELSE wartość END to „if” w SQL. W połączeniu z SUM/COUNT daje agregację
     * warunkową: kilka liczników w jednym przebiegu po tabeli.
     */
    static void distinctAndCaseWhen(Connection con) throws SQLException {
        section("5. DISTINCT i CASE WHEN");

        // CASE w SELECT i w GROUP BY: przedziały cenowe
        printTable(con, """
                SELECT CASE WHEN price < 100  THEN 'tanie'
                            WHEN price < 1000 THEN 'średnie'
                            ELSE 'drogie' END AS price_band,
                       COUNT(*) AS products
                FROM products
                GROUP BY price_band
                ORDER BY MIN(price)""");
        // WYNIK: PRICE_BAND | PRODUCTS
        // WYNIK: -----------+---------
        // WYNIK: tanie      | 6
        // WYNIK: średnie    | 4
        // WYNIK: drogie     | 4
        // WYNIK: (wierszy: 3)
        // GROUP BY po aliasie (price_band) akceptują H2, PostgreSQL i MySQL; standard (i np. SQL Server) wymaga
        // powtórzenia całego wyrażenia CASE.

        // Agregacja warunkowa: liczniki statusów w jednym zapytaniu (jeden wiersz na klienta)
        printTable(con, """
                SELECT c.name,
                       SUM(CASE WHEN o.status = 'DOSTARCZONE' THEN 1 ELSE 0 END) AS delivered,
                       SUM(CASE WHEN o.status IN ('NOWE', 'OPLACONE', 'WYSLANE') THEN 1 ELSE 0 END) AS in_progress
                FROM customers c
                JOIN orders o ON o.customer_id = c.id
                GROUP BY c.id, c.name
                HAVING COUNT(*) > 1
                ORDER BY c.id""");
        // WYNIK: NAME           | DELIVERED | IN_PROGRESS
        // WYNIK: ---------------+-----------+------------
        // WYNIK: Jan Kowalski   | 1         | 2
        // WYNIK: Maria Nowak    | 1         | 1
        // WYNIK: Zofia Krawczyk | 1         | 1
        // WYNIK: (wierszy: 3)
        // H2 i PostgreSQL mają też krótszy zapis standardowy: COUNT(*) FILTER (WHERE o.status = 'DOSTARCZONE').

        // DISTINCT na całym wierszu: różne pary (miasto, vip)
        printTable(con, "SELECT DISTINCT city, vip FROM customers WHERE city IN ('Warszawa', 'Kraków') ORDER BY city, vip");
        // WYNIK: CITY     | VIP
        // WYNIK: ---------+------
        // WYNIK: Kraków   | FALSE
        // WYNIK: Warszawa | TRUE
        // WYNIK: (wierszy: 2)
    }

    // =================================================================================================
    // 6. FUNKCJE OKNA — ROW_NUMBER I SUM OVER
    // =================================================================================================

    /**
     * 6. Funkcja okna liczy wartość dla każdego wiersza na podstawie „okna” innych wierszy, ale — w odróżnieniu
     * od GROUP BY — NIE skleja wierszy. PARTITION BY dzieli na części, ORDER BY w OVER ustala kolejność w części.
     * H2, PostgreSQL, MySQL 8+, Oracle i SQL Server je obsługują (MySQL 5.7 — nie).
     */
    static void windowFunctions(Connection con) throws SQLException {
        section("6. Funkcje okna — ROW_NUMBER i SUM OVER");

        // Ranking cen w każdej kategorii; podzapytanie wybiera miejsce 1 (najdroższy w kategorii)
        printTable(con, """
                SELECT category, name, price
                FROM (SELECT category, name, price,
                             ROW_NUMBER() OVER (PARTITION BY category ORDER BY price DESC, sku) AS rn
                      FROM products) ranked
                WHERE rn = 1
                ORDER BY category""");
        // WYNIK: CATEGORY    | NAME               | PRICE
        // WYNIK: ------------+--------------------+--------
        // WYNIK: DOM         | Ekspres do kawy    | 1899.00
        // WYNIK: ELEKTRONIKA | Laptop Pro 14      | 5499.99
        // WYNIK: KSIAZKI     | Java. Podstawy     | 129.00
        // WYNIK: ODZIEZ      | Kurtka zimowa      | 459.00
        // WYNIK: SPOZYWCZE   | Kawa ziarnista 1kg | 64.99
        // WYNIK: (wierszy: 5)
        // ROW_NUMBER = numer wiersza w części (1, 2, 3…). Funkcji okna nie wolno użyć w WHERE tego samego zapytania
        // (liczone są po WHERE), stąd podzapytanie. RANK() dałby remisom ten sam numer.

        // Suma narastająca przychodu w kolejności dat (SUM OVER z ORDER BY = od początku do bieżącego wiersza)
        printTable(con, """
                SELECT o.id, o.order_date, t.total, SUM(t.total) OVER (ORDER BY o.order_date, o.id) AS running_total
                FROM orders o
                JOIN (SELECT l.order_id, SUM(l.quantity * p.price) AS total
                      FROM order_lines l JOIN products p ON p.sku = l.sku
                      GROUP BY l.order_id) t ON t.order_id = o.id
                WHERE o.order_date < DATE '2026-02-21'
                ORDER BY o.order_date, o.id""");
        // WYNIK: ID      | ORDER_DATE | TOTAL   | RUNNING_TOTAL
        // WYNIK: --------+------------+---------+--------------
        // WYNIK: ZAM-001 | 2026-01-05 | 6199.79 | 6199.79
        // WYNIK: ZAM-002 | 2026-01-12 | 269.87  | 6469.66
        // WYNIK: ZAM-003 | 2026-02-02 | 307.00  | 6776.66
        // WYNIK: ZAM-004 | 2026-02-14 | 2999.00 | 9775.66
        // WYNIK: ZAM-005 | 2026-02-20 | 2028.98 | 11804.64
        // WYNIK: (wierszy: 5)
        // Podzapytanie t najpierw sumuje każde zamówienie (GROUP BY), a dopiero potem funkcja okna je narasta.
        // To też sposób na pułapkę „SUM po dwóch tabelach wielu”: sumuj w podzapytaniu, a potem łącz.
    }

    // =================================================================================================
    // 7. INDEKSY — B-DRZEWO, CREATE INDEX, EXPLAIN
    // =================================================================================================

    /**
     * 7. Bez indeksu baza musi przejrzeć każdy wiersz (pełny skan tabeli). Indeks B-drzewo trzyma posortowane
     * wartości kolumny ze wskazaniem na wiersze. EXPLAIN pokazuje plan: którędy baza zamierza szukać.
     * Wypisujemy tylko, czy plan wspomina indeks — pełny tekst planu zależy od wersji H2.
     */
    static void indexesAndExplain(Connection con) throws SQLException {
        section("7. Indeksy — B-drzewo, CREATE INDEX, EXPLAIN");

        String query = "SELECT name FROM products WHERE price BETWEEN 100 AND 500";
        // planMentions = czy plan wspomina dany tekst (pomocnik na dole pliku)
        show("przed CREATE INDEX: plan używa IDX_P_PRICE", planMentions(con, query, "IDX_P_PRICE"));
        show("przed CREATE INDEX: pełny skan tabeli", planMentions(con, query, "tableScan"));
        // WYNIK: przed CREATE INDEX: plan używa IDX_P_PRICE → false
        // WYNIK: przed CREATE INDEX: pełny skan tabeli → true

        execute(con, "CREATE INDEX idx_p_price ON products (price)");    // execute = wykonaj polecenie (pomocnik)
        show("po CREATE INDEX: plan używa IDX_P_PRICE", planMentions(con, query, "IDX_P_PRICE"));
        show("po CREATE INDEX: pełny skan tabeli", planMentions(con, query, "tableScan"));
        // WYNIK: po CREATE INDEX: plan używa IDX_P_PRICE → true
        // WYNIK: po CREATE INDEX: pełny skan tabeli → false

        // PUŁAPKA: wyrażenie na kolumnie wyłącza zwykły indeks — baza musiałaby policzyć price * 1.23 dla każdego wiersza
        show("WHERE price * 1.23 > 500 używa indeksu", planMentions(con,
                "SELECT name FROM products WHERE price * 1.23 > 500", "IDX_P_PRICE"));
        show("WHERE price > 500 / 1.23 używa indeksu", planMentions(con,
                "SELECT name FROM products WHERE price > 500 / 1.23", "IDX_P_PRICE"));
        // WYNIK: WHERE price * 1.23 > 500 używa indeksu → false
        // WYNIK: WHERE price > 500 / 1.23 używa indeksu → true
        // Tak samo LOWER(name) = ? albo EXTRACT(YEAR FROM order_date) = 2026: przepisz warunek na „gołą” kolumnę
        // (order_date BETWEEN DATE '2026-01-01' AND DATE '2026-12-31') albo utwórz indeks na wyrażeniu (PostgreSQL).
        // Klucz główny i UNIQUE mają indeks automatycznie. Na 14 wierszach różnicy czasu nie zobaczysz — liczy się
        // przy tysiącach i milionach wierszy, gdzie skan trwa sekundy, a indeks — milisekundy.
    }

    // =================================================================================================
    // 8. INDEKS ZŁOŻONY — KOLEJNOŚĆ KOLUMN I LIKE Z PREFIKSEM
    // =================================================================================================

    /**
     * 8. Indeks złożony (category, price) jest posortowany najpierw po kategorii, a w niej po cenie — jak książka
     * telefoniczna (nazwisko, imię). Pomoże przy warunku na category albo na category i price, ale nie przy
     * samej cenie — tak jak w książce telefonicznej nie znajdziesz szybko wszystkich „Janów”.
     */
    static void compositeIndexes(Connection con) throws SQLException {
        section("8. Indeks złożony — kolejność kolumn i LIKE z prefiksem");

        execute(con, "DROP INDEX idx_p_price");
        execute(con, "CREATE INDEX idx_p_cat_price ON products (category, price)");
        show("WHERE category = ?", planMentions(con, "SELECT name FROM products WHERE category = 'DOM'", "IDX_P_CAT_PRICE"));
        show("WHERE category = ? AND price < ?", planMentions(con,
                "SELECT name FROM products WHERE category = 'DOM' AND price < 500", "IDX_P_CAT_PRICE"));
        show("WHERE price < ? (sama druga kolumna)", planMentions(con,
                "SELECT name FROM products WHERE price < 500", "IDX_P_CAT_PRICE"));
        // WYNIK: WHERE category = ? → true
        // WYNIK: WHERE category = ? AND price < ? → true
        // WYNIK: WHERE price < ? (sama druga kolumna) → false
        // DOBRA PRAKTYKA: w indeksie złożonym na początek kolumna porównywana przez „=”, za nią ta z zakresem
        // (<, BETWEEN) albo sortowaniem. Zapytania tylko po drugiej kolumnie potrzebują własnego indeksu.

        // LIKE: wzorzec z ustalonym POCZĄTKIEM to zakres w indeksie; wzorzec zaczynający się od % — nie
        execute(con, "CREATE INDEX idx_p_name ON products (name)");
        show("name LIKE 'Ka%' używa indeksu", planMentions(con, "SELECT sku FROM products WHERE name LIKE 'Ka%'", "IDX_P_NAME"));
        show("name LIKE '%kawy' używa indeksu", planMentions(con, "SELECT sku FROM products WHERE name LIKE '%kawy'", "IDX_P_NAME"));
        // WYNIK: name LIKE 'Ka%' używa indeksu → true
        // WYNIK: name LIKE '%kawy' używa indeksu → false
        // Wyszukiwanie „w środku” tekstu w dużych tabelach robi się indeksem pełnotekstowym (full-text) albo
        // osobnym silnikiem wyszukiwania — zwykłe B-drzewo tu nie pomoże.
    }

    // =================================================================================================
    // 9. INDEKSY UNIKALNE I KOSZT INDEKSÓW
    // =================================================================================================

    /**
     * 9. UNIQUE INDEX przyspiesza wyszukiwanie i jednocześnie pilnuje unikalności (jak ograniczenie UNIQUE).
     * Każdy indeks kosztuje: INSERT/UPDATE/DELETE muszą poprawić KAŻDY indeks tabeli.
     */
    static void uniqueIndexesAndCost(Connection con) throws SQLException {
        section("9. Indeksy unikalne i koszt indeksów");

        execute(con, "CREATE UNIQUE INDEX ux_customers_email ON customers (email)");
        expectSqlError(con, "drugi klient z jan@example.com",
                "INSERT INTO customers (id, name, city, email, vip) VALUES (8, 'Jan Bis', 'Łódź', 'jan@example.com', FALSE)");
        // WYNIK: ✔ drugi klient z jan@example.com → SQLState 23505 (duplikat klucza / UNIQUE)
        // NULL-e się nie „gryzą” (NULL ≠ NULL) — Maria i Ola bez e-maila nie łamią indeksu unikalnego.

        note("indeks przyspiesza SELECT, ale spowalnia INSERT/UPDATE/DELETE i zajmuje miejsce");
        note("indeksuj kolumny z WHERE, JOIN (klucze obce) i ORDER BY — na podstawie EXPLAIN, nie na zapas");
        // WYNIK: ℹ indeks przyspiesza SELECT, ale spowalnia INSERT/UPDATE/DELETE i zajmuje miejsce
        // WYNIK: ℹ indeksuj kolumny z WHERE, JOIN (klucze obce) i ORDER BY — na podstawie EXPLAIN, nie na zapas
        // PUŁAPKA: indeks na kolumnie o kilku wartościach (np. vip TRUE/FALSE) zwykle nic nie daje — baza i tak
        // przeczyta połowę tabeli, więc wybierze pełny skan. Indeks pomaga, gdy warunek wybiera MAŁĄ część wierszy.
        // PUŁAPKA: tabela, do której głównie się pisze (np. dziennik zdarzeń), z dziesięcioma indeksami — każdy
        // INSERT poprawia 10 drzew. Usuwaj indeksy, których żadne zapytanie nie używa.
    }

    // =================================================================================================
    // 10. SQL KONTRA STRUMIENIE — TEN SAM WYNIK DWIEMA DROGAMI
    // =================================================================================================

    /**
     * 10. GROUP BY w SQL i groupingBy w Javie robią to samo: grupują i sumują. Liczymy przychód klientów
     * (zamówienia nieanulowane) w bazie i strumieniem na SampleData.orders() — wyniki muszą być identyczne.
     */
    static void sqlVersusStreams(Connection con) throws SQLException {
        section("10. SQL kontra strumienie — ten sam wynik dwiema drogami");

        Map<String, BigDecimal> fromSql = new TreeMap<>();
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("""
                     SELECT c.name, SUM(l.quantity * p.price) AS revenue
                     FROM customers c
                     JOIN orders o      ON o.customer_id = c.id
                     JOIN order_lines l ON l.order_id = o.id
                     JOIN products p    ON p.sku = l.sku
                     WHERE o.status <> 'ANULOWANE'
                     GROUP BY c.id, c.name
                     ORDER BY c.name""")) {
            while (rs.next()) {
                fromSql.put(rs.getString("name"), rs.getBigDecimal("revenue"));
            }
        }

        // groupingBy(klucz, fabryka mapy, reducing(zero, wartość, suma)) — patrz t16_streams/Streams11GroupingBy
        Map<String, BigDecimal> fromStreams = SampleData.orders().stream()
                .filter(o -> o.status() != OrderStatus.ANULOWANE)
                .collect(Collectors.groupingBy(o -> o.customer().name(), TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Order::total, BigDecimal::add)));

        showEach("SQL", fromSql);
        show("strumienie", fromStreams);
        show("te same klucze i kwoty (compareTo)", sameAmounts(fromSql, fromStreams));   // same amounts = te same kwoty
        // WYNIK: SQL (liczba kluczy: 5):
        // WYNIK: • Jan Kowalski → 6669.79
        // WYNIK: • Marek Król → 295.45
        // WYNIK: • Maria Nowak → 619.77
        // WYNIK: • Ola Pawlak → 608.97
        // WYNIK: • Zofia Krawczyk → 4755.98
        // WYNIK: strumienie → {Jan Kowalski=6669.79, Marek Król=295.45, Maria Nowak=619.77, Ola Pawlak=608.97, Zofia Krawczyk=4755.98}
        // WYNIK: te same klucze i kwoty (compareTo) → true
        // Kiedy co? SQL, gdy dane są w bazie: baza odsyła 5 liczb zamiast tysięcy wierszy, które Java musiałaby
        // pobrać, zmapować i dopiero zsumować. Strumienie, gdy dane już są w pamięci (lista z innego źródła,
        // wynik po przekształceniach w Javie).
        // PUŁAPKA: kolejność kluczy. TreeMap sortuje według kodów Unicode — tak samo jak ORDER BY w H2. Polska
        // kolacja w bazie (PostgreSQL/MySQL) albo Collator w Javie ustawią „Ł” obok „L” — wtedy kolejności się rozjadą.
    }

    // =================================================================================================
    // NARZĘDZIA LEKCJI
    // =================================================================================================

    static final String SCHEMA = """
            CREATE TABLE customers (
                id    INT          PRIMARY KEY,
                name  VARCHAR(100) NOT NULL,
                city  VARCHAR(50)  NOT NULL,
                email VARCHAR(100),
                vip   BOOLEAN      NOT NULL
            );
            CREATE TABLE products (
                sku      VARCHAR(10)   PRIMARY KEY,
                name     VARCHAR(100)  NOT NULL,
                category VARCHAR(20)   NOT NULL,
                price    DECIMAL(10,2) NOT NULL,
                stock    INT           NOT NULL
            );
            CREATE TABLE orders (
                id          VARCHAR(10) PRIMARY KEY,
                customer_id INT         NOT NULL REFERENCES customers (id),
                order_date  DATE        NOT NULL,
                status      VARCHAR(20) NOT NULL
            );
            CREATE TABLE order_lines (
                order_id VARCHAR(10) NOT NULL REFERENCES orders (id),
                sku      VARCHAR(10) NOT NULL REFERENCES products (sku),
                quantity INT         NOT NULL,
                PRIMARY KEY (order_id, sku)
            );
            """;

    /** Tworzy schemat i wstawia klientów, produkty i zamówienia z SampleData (paczki PreparedStatement — Jdbc03). */
    static void createShop(Connection con) throws SQLException {
        for (String sql : SCHEMA.split(";")) {
            if (!sql.isBlank()) {                 // isBlank = czy pusty lub same spacje (Java 11+)
                execute(con, sql);
            }
        }
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO customers (id, name, city, email, vip) VALUES (?, ?, ?, ?, ?)")) {
            for (Customer c : SampleData.customers()) {
                ps.setLong(1, c.id());
                ps.setString(2, c.name());
                ps.setString(3, c.city());
                ps.setObject(4, c.email(), Types.VARCHAR);
                ps.setBoolean(5, c.vip());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO products (sku, name, category, price, stock) VALUES (?, ?, ?, ?, ?)")) {
            for (Product p : SampleData.products()) {
                ps.setString(1, p.sku());
                ps.setString(2, p.name());
                ps.setString(3, p.category().name());
                ps.setBigDecimal(4, p.price());
                ps.setInt(5, p.stock());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        try (PreparedStatement order = con.prepareStatement("INSERT INTO orders (id, customer_id, order_date, status) VALUES (?, ?, ?, ?)");
             PreparedStatement line = con.prepareStatement("INSERT INTO order_lines (order_id, sku, quantity) VALUES (?, ?, ?)")) {
            for (Order o : SampleData.orders()) {
                order.setString(1, o.id());
                order.setLong(2, o.customer().id());
                order.setObject(3, o.date());
                order.setString(4, o.status().name());
                order.addBatch();
                for (OrderLine l : o.lines()) {
                    line.setString(1, o.id());
                    line.setString(2, l.product().sku());
                    line.setInt(3, l.quantity());
                    line.addBatch();
                }
            }
            order.executeBatch();
            line.executeBatch();
        }
    }

    static void execute(Connection con, String sql) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.execute(sql);
        }
    }

    /** Czy plan zapytania (EXPLAIN) zawiera dany tekst — np. nazwę indeksu albo „tableScan”. */
    static boolean planMentions(Connection con, String query, String fragment) throws SQLException {
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("EXPLAIN " + query)) {
            rs.next();
            return rs.getString(1).contains(fragment);
        }
    }

    /** Czy dwie mapy mają te same klucze i równe kwoty (compareTo == 0, bez względu na skalę). */
    static boolean sameAmounts(Map<String, BigDecimal> a, Map<String, BigDecimal> b) {
        if (!a.keySet().equals(b.keySet())) {
            return false;
        }
        return a.keySet().stream().allMatch(k -> a.get(k).compareTo(b.get(k)) == 0);
    }

    /** Wypisuje wynik zapytania jako wyrównaną tabelę; NULL pokazuje jako napis NULL. */
    static void printTable(Connection con, String sql) throws SQLException {
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
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
                StringBuilder out = new StringBuilder("   ");
                for (int i = 0; i < columns; i++) {
                    out.append(i > 0 ? " | " : "").append(String.format(Locale.ROOT, "%-" + widths[i] + "s", rows.get(r)[i]));
                }
                System.out.println(out.toString().stripTrailing());    // stripTrailing = usuń końcowe spacje (Java 11+)
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

    /** Polskie opisy kodów SQLState użytych w lekcji. */
    static String describe(String sqlState) {
        return switch (sqlState) {
            case "90016" -> "H2: kolumna musi być w GROUP BY";
            case "90054" -> "H2: niedozwolone użycie funkcji agregującej";
            case "23505" -> "duplikat klucza / UNIQUE";
            default -> "inny błąd";
        };
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • COUNT(*) wiersze; COUNT(kol) bez NULL; COUNT(DISTINCT kol) różne. SUM/AVG/MIN/MAX pomijają NULL.
     *   • SUM/AVG/MIN/MAX pustego zbioru = NULL → COALESCE(SUM(x), 0). COUNT pustego = 0.
     *   • GROUP BY: w SELECT tylko kolumny grupujące i funkcje agregujące. Grupuj po kluczu (c.id, c.name).
     *   • WHERE filtruje wiersze PRZED grupowaniem, HAVING — grupy PO. Agregat w WHERE = błąd.
     *   • JOIN mnoży wiersze: COUNT(DISTINCT o.id); przy LEFT JOIN COUNT(o.id), nie COUNT(*).
     *   • CASE WHEN … THEN … ELSE … END; agregacja warunkowa SUM(CASE WHEN … THEN 1 ELSE 0 END) / COUNT(*) FILTER (WHERE …).
     *   • Funkcje okna: ROW_NUMBER() OVER (PARTITION BY k ORDER BY c DESC), SUM(x) OVER (ORDER BY d) — bez sklejania wierszy;
     *     filtr po wyniku → podzapytanie.
     *   • Indeks B-drzewo: =, <, BETWEEN, LIKE 'abc%'. Nie pomaga: wyrażenie na kolumnie, LIKE '%abc', sama druga
     *     kolumna indeksu złożonego, kolumny o kilku wartościach.
     *   • EXPLAIN SELECT … — plan; szukaj nazwy indeksu albo pełnego skanu.
     *   • Indeks złożony (a, b): najpierw kolumna z „=”, potem zakres/sortowanie. UNIQUE INDEX = szybko + unikalnie.
     *   • Każdy indeks spowalnia zapisy i zajmuje miejsce → indeksuj na podstawie zapytań (WHERE, JOIN, ORDER BY).
     *   • GROUP BY w SQL ≈ groupingBy w Javie; licz tam, gdzie leżą dane.
     *
     * PYTANIA KONTROLNE:
     *   1. Co wypisze (dane lekcji: 7 klientów, 2 bez e-maila):  SELECT COUNT(*), COUNT(email) FROM customers  ?
     *   2. ZNAJDŹ BŁĄD:  SELECT category, COUNT(*) FROM products WHERE COUNT(*) > 2 GROUP BY category
     *   3. Co wypisze:  SELECT SUM(price) FROM products WHERE price > 10000  — 0 czy coś innego?
     *   4. ZNAJDŹ BŁĄD (miało pokazać liczbę zamówień każdego klienta, także 0):
     *        SELECT c.name, COUNT(*) FROM customers c LEFT JOIN orders o ON o.customer_id = c.id GROUP BY c.id, c.name
     *   5. Czym różni się funkcja okna od GROUP BY? Kiedy potrzebujesz podzapytania z ROW_NUMBER?
     *   6. Masz indeks (category, price). Które warunki z niego skorzystają: a) category = ?  b) price < ?
     *      c) category = ? AND price < ?  ?
     *   7. Dlaczego WHERE LOWER(email) = ? nie użyje zwykłego indeksu na email? Jak sobie poradzić?
     *   8. Dlaczego nie warto zakładać indeksu na każdej kolumnie „na zapas”?
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

    /** NOWA baza z danymi lekcji dla każdego ćwiczenia; zamykana po pracy. */
    static <T> T inFreshDb(String dbName, SqlWork<T> work) {
        try (Connection con = DriverManager.getConnection("jdbc:h2:mem:" + dbName)) {
            createShop(con);
            return work.apply(con);
        } catch (SQLException e) {
            throw new IllegalStateException("błąd SQL, SQLState " + e.getSQLState(), e);
        }
    }

    /** deliveredRevenueByCategory = przychód z dostarczonych wg kategorii. Oczekiwany wynik ćwiczenia 3 policzony strumieniem: przychód kategorii z zamówień DOSTARCZONYCH. */
    static Map<String, BigDecimal> deliveredRevenueByCategory() {
        return SampleData.orders().stream()
                .filter(o -> o.status() == OrderStatus.DOSTARCZONE)
                .flatMap(o -> o.lines().stream())
                .collect(Collectors.groupingBy(l -> l.product().category().name(), TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, OrderLine::total, BigDecimal::add)));
    }

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Map<String, Integer> perCategory = new TreeMap<>(Map.of("DOM", 2, "ELEKTRONIKA", 4, "KSIAZKI", 3, "ODZIEZ", 2, "SPOZYWCZE", 3));
        Check.equal("ćw. 1: liczba produktów w kategoriach", perCategory,
                () -> inFreshDb("jdbc07_e1", Jdbc07SqlAggregationIndexes::exercise1));
        Check.equal("ćw. 2: klienci z przychodem > 1000 zł", List.of("Jan Kowalski", "Zofia Krawczyk"),
                () -> inFreshDb("jdbc07_e2", c -> exercise2(c, new BigDecimal("1000"))));
        Check.equal("ćw. 3: przychód kategorii jak w strumieniu", deliveredRevenueByCategory(),
                () -> inFreshDb("jdbc07_e3", Jdbc07SqlAggregationIndexes::exercise3));
        Check.equal("ćw. 4: najdroższy produkt każdej kategorii",
                List.of("DOM: Ekspres do kawy", "ELEKTRONIKA: Laptop Pro 14", "KSIAZKI: Java. Podstawy",
                        "ODZIEZ: Kurtka zimowa", "SPOZYWCZE: Kawa ziarnista 1kg"),
                () -> inFreshDb("jdbc07_e4", Jdbc07SqlAggregationIndexes::exercise4));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", perCategory,
                () -> inFreshDb("jdbc07_s1", Jdbc07SqlAggregationIndexes::solution1));
        Check.equal("ćw. 2 (wzorzec)", List.of("Jan Kowalski", "Zofia Krawczyk"),
                () -> inFreshDb("jdbc07_s2", c -> solution2(c, new BigDecimal("1000"))));
        Check.equal("ćw. 3 (wzorzec)", deliveredRevenueByCategory(),
                () -> inFreshDb("jdbc07_s3", Jdbc07SqlAggregationIndexes::solution3));
        Check.equal("ćw. 4 (wzorzec)",
                List.of("DOM: Ekspres do kawy", "ELEKTRONIKA: Laptop Pro 14", "KSIAZKI: Java. Podstawy",
                        "ODZIEZ: Kurtka zimowa", "SPOZYWCZE: Kawa ziarnista 1kg"),
                () -> inFreshDb("jdbc07_s4", Jdbc07SqlAggregationIndexes::solution4));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć mapę kategoria → liczba produktów (TreeMap), licząc w SQL (GROUP BY).
     * Podpowiedź: SELECT category, COUNT(*) AS n FROM products GROUP BY category; w pętli map.put(…, rs.getInt("n")).
     */
    static Map<String, Integer> exercise1(Connection con) throws SQLException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): nazwy klientów, których łączny przychód z zamówień NIEANULOWANYCH przekracza
     * {@code minRevenue}, malejąco według przychodu. Próg przekaż parametrem.
     * Podpowiedź: JOIN czterech tabel, {@code WHERE o.status <> 'ANULOWANE'}, GROUP BY c.id, c.name,
     * {@code HAVING SUM(l.quantity * p.price) > ?}, ORDER BY SUM(…) DESC.
     */
    static List<String> exercise2(Connection con, BigDecimal minRevenue) throws SQLException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ na SQL (GROUP BY) poniższe grupowanie ze strumieni — przychód każdej kategorii
     * z zamówień DOSTARCZONYCH — i zwróć TreeMap kategoria → kwota:
     * <pre>{@code
     * SampleData.orders().stream()
     *         .filter(o -> o.status() == OrderStatus.DOSTARCZONE)
     *         .flatMap(o -> o.lines().stream())
     *         .collect(Collectors.groupingBy(l -> l.product().category().name(), TreeMap::new,
     *                 Collectors.reducing(BigDecimal.ZERO, OrderLine::total, BigDecimal::add)));
     * }</pre>
     * Podpowiedź: flatMap po pozycjach = JOIN order_lines; filter = WHERE; groupingBy = GROUP BY p.category.
     */
    static Map<String, BigDecimal> exercise3(Connection con) throws SQLException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): dla każdej kategorii (alfabetycznie) zwróć „KATEGORIA: nazwa” najdroższego produktu.
     * Użyj funkcji okna ROW_NUMBER() OVER (PARTITION BY … ORDER BY …) w podzapytaniu.
     * Podpowiedź: jak w sekcji 6; w Javie składaj tekst rs.getString("category") + ": " + rs.getString("name").
     */
    static List<String> exercise4(Connection con) throws SQLException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static Map<String, Integer> solution1(Connection con) throws SQLException {
        Map<String, Integer> result = new TreeMap<>();
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT category, COUNT(*) AS n FROM products GROUP BY category")) {
            while (rs.next()) {
                result.put(rs.getString("category"), rs.getInt("n"));
            }
        }
        return result;
    }

    static List<String> solution2(Connection con, BigDecimal minRevenue) throws SQLException {
        String sql = """
                SELECT c.name
                FROM customers c
                JOIN orders o      ON o.customer_id = c.id
                JOIN order_lines l ON l.order_id = o.id
                JOIN products p    ON p.sku = l.sku
                WHERE o.status <> 'ANULOWANE'
                GROUP BY c.id, c.name
                HAVING SUM(l.quantity * p.price) > ?
                ORDER BY SUM(l.quantity * p.price) DESC""";
        List<String> result = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, minRevenue);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString("name"));
                }
            }
        }
        return result;
    }

    static Map<String, BigDecimal> solution3(Connection con) throws SQLException {
        Map<String, BigDecimal> result = new TreeMap<>();
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("""
                     SELECT p.category, SUM(l.quantity * p.price) AS revenue
                     FROM orders o
                     JOIN order_lines l ON l.order_id = o.id
                     JOIN products p    ON p.sku = l.sku
                     WHERE o.status = 'DOSTARCZONE'
                     GROUP BY p.category""")) {
            while (rs.next()) {
                result.put(rs.getString("category"), rs.getBigDecimal("revenue"));
            }
        }
        return result;
    }

    static List<String> solution4(Connection con) throws SQLException {
        List<String> result = new ArrayList<>();
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("""
                     SELECT category, name
                     FROM (SELECT category, name,
                                  ROW_NUMBER() OVER (PARTITION BY category ORDER BY price DESC, sku) AS rn
                           FROM products) ranked
                     WHERE rn = 1
                     ORDER BY category""")) {
            while (rs.next()) {
                result.add(rs.getString("category") + ": " + rs.getString("name"));
            }
        }
        return result;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. 7 i 5 — COUNT(*) liczy wiersze, COUNT(email) pomija dwa NULL-e.
     *   2. Funkcja agregująca w WHERE (WHERE działa przed grupowaniem; H2: SQLState 90054). Poprawnie:
     *      … GROUP BY category HAVING COUNT(*) > 2.
     *   3. NULL — SUM ze zbioru bez wierszy nie jest zerem. Zero dałoby COALESCE(SUM(price), 0).
     *   4. COUNT(*) policzy też wiersz „bez pary” z LEFT JOIN — klient bez zamówień dostanie 1. Poprawnie: COUNT(o.id).
     *   5. GROUP BY skleja grupę w jeden wiersz; funkcja okna dopisuje wartość do KAŻDEGO wiersza, nic nie sklejając.
     *      Podzapytanie jest potrzebne, gdy chcesz filtrować po wyniku funkcji okna (np. rn = 1), bo liczy się ona
     *      po WHERE tego samego zapytania.
     *   6. a) tak (pierwsza kolumna), b) nie (sama druga kolumna — plan w lekcji: pełny skan), c) tak (obie kolumny).
     *   7. Indeks przechowuje posortowane oryginalne wartości email, a nie LOWER(email) — baza musiałaby policzyć
     *      LOWER dla każdego wiersza. Rozwiązania: zapisuj e-maile już małymi literami i porównuj „gołą” kolumnę,
     *      albo indeks na wyrażeniu (PostgreSQL: CREATE INDEX … ON customers (LOWER(email))).
     *   8. Każdy indeks trzeba aktualizować przy każdym INSERT/UPDATE/DELETE i przechowywać (miejsce, pamięć).
     *      Nieużywany indeks to sam koszt — indeksy dobiera się do zapytań (EXPLAIN).
     */
    // </editor-fold>
}
