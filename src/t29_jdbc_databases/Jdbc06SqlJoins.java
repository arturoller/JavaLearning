package t29_jdbc_databases;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Customer;
import helpers.model.Order;
import helpers.model.OrderLine;
import helpers.model.Product;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Złączenia (JOIN), podzapytania i projekt relacyjny
 *        (join = złączenie; subquery = podzapytanie; foreign key = klucz obcy)
 *
 * W SKRÓCIE:
 *   Dane sklepu leżą w kilku tabelach: klienci, zamówienia, pozycje zamówień, produkty. Tabele łączą się
 *   kluczami obcymi (orders.customer_id → customers.id). JOIN składa wiersze z kilku tabel w jeden wynik,
 *   a podzapytanie pozwala zadać pytanie „w środku” innego pytania. Projekt bez powtórzeń (normalizacja)
 *   sprawia, że każdy fakt jest zapisany w JEDNYM miejscu.
 *
 * ANALOGIA:
 *   Segregator klientów i segregator zamówień. Na zamówieniu nie ma adresu klienta — jest tylko jego numer.
 *   Żeby wydrukować „zamówienie + nazwisko”, kładziesz obok siebie dwie kartki o tym samym numerze (JOIN).
 *   LEFT JOIN to „wszystkie kartki klientów, a obok zamówienie, jeśli jest — inaczej puste miejsce”.
 *
 * JAK TO DZIAŁA:
 *   FROM orders o JOIN customers c ON c.id = o.customer_id     ← o, c = aliasy (krótkie nazwy tabel)
 *   INNER JOIN — tylko pary, które pasują po obu stronach
 *   LEFT JOIN  — wszystkie wiersze z LEWEJ tabeli; brak pary → kolumny prawej tabeli = NULL
 *   RIGHT JOIN — lustrzane odbicie LEFT;  FULL JOIN — obie strony (H2 2.x tego NIE obsługuje)
 *   Self-join  — tabela złączona sama ze sobą (pracownik → jego kierownik)
 *   customers 1 ──< orders 1 ──< order_lines >── 1 products   (order_lines = tabela łącząca, wiele-do-wielu)
 *
 * SŁÓWKA:
 *   join = złączenie; inner = wewnętrzny; left/right/full outer = lewy/prawy/pełny zewnętrzny; on = przy warunku;
 *   alias = krótka nazwa; exists = istnieje; subquery = podzapytanie; foreign key = klucz obcy;
 *   references = odwołuje się do; cascade = kaskadowo; junction table = tabela łącząca; manager = kierownik
 *
 * ZOBACZ TEŻ: t29_jdbc_databases/Jdbc01SqlBasics (SELECT, WHERE, NULL),
 *             t29_jdbc_databases/Jdbc05Dao (N+1 zapytań — JOIN jako lekarstwo),
 *             t29_jdbc_databases/Jdbc07SqlAggregationIndexes (GROUP BY na złączonych tabelach)
 * </pre>
 */
public class Jdbc06SqlJoins {

    private static final String URL = "jdbc:h2:mem:jdbc06";

    public static void main(String[] args) throws SQLException {
        title("Jdbc06 — JOIN, podzapytania, klucze obce");

        try (Connection con = DriverManager.getConnection(URL)) {
            createShop(con);               // create shop = utwórz sklep (schemat + dane z SampleData)
            innerJoin(con);                // inner join = złączenie wewnętrzne
            leftJoin(con);                 // left join = złączenie lewostronne
            onVersusWhere(con);            // on versus where = warunek w ON czy w WHERE
            rightAndFullJoin(con);         // right and full join = złączenie prawe i pełne
            manyToMany(con);               // many-to-many = wiele-do-wielu
            duplicatesFromJoins(con);      // duplicates from joins = powtórzenia ze złączeń
            selfJoin(con);                 // self-join = złączenie tabeli z samą sobą
            nestedObjects(con);            // nested objects = obiekty zagnieżdżone w Javie
            subqueries(con);               // subqueries = podzapytania
            designAndForeignKeys(con);     // design and foreign keys = projekt i klucze obce
        }
        exercises();                       // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. INNER JOIN — TYLKO PASUJĄCE PARY
    // =================================================================================================

    /**
     * 1. INNER JOIN (samo JOIN znaczy to samo) zwraca wiersz dla każdej pary wierszy spełniającej warunek ON.
     * Aliasy (o, c) skracają zapis i są OBOWIĄZKOWE, gdy dwie tabele mają kolumnę o tej samej nazwie.
     */
    static void innerJoin(Connection con) throws SQLException {
        section("1. INNER JOIN — tylko pasujące pary");

        // printTable = wypisz tabelę (pomocnik na dole pliku, jak w Jdbc01SqlBasics)

        printTable(con, """
                SELECT o.id, o.order_date, c.name, c.city
                FROM orders o
                JOIN customers c ON c.id = o.customer_id
                WHERE o.status = 'DOSTARCZONE'
                ORDER BY o.id""");
        // WYNIK: ID      | ORDER_DATE | NAME           | CITY
        // WYNIK: --------+------------+----------------+---------
        // WYNIK: ZAM-001 | 2026-01-05 | Jan Kowalski   | Warszawa
        // WYNIK: ZAM-002 | 2026-01-12 | Maria Nowak    | Kraków
        // WYNIK: ZAM-007 | 2026-03-03 | Zofia Krawczyk | Warszawa
        // WYNIK: (wierszy: 3)

        // Zamówienie pamięta tylko customer_id — nazwisko i miasto przyszły z tabeli customers.
        // expectSqlError = oczekuj błędu SQL (wypisuje SQLState i polski opis z describe = opisz)
        // PUŁAPKA: kolumna bez aliasu, która występuje w obu tabelach (np. id), daje błąd „niejednoznaczna kolumna”
        expectSqlError(con, "kolumna id bez aliasu", "SELECT id FROM orders o JOIN customers c ON c.id = o.customer_id");
        // WYNIK: ✔ kolumna id bez aliasu → SQLState 90059 (H2: niejednoznaczna nazwa kolumny)
        // Stary zapis „FROM orders o, customers c WHERE c.id = o.customer_id” działa tak samo, ale zapomniany warunek
        // daje iloczyn kartezjański (każdy z każdym: 10 × 7 = 70 wierszy). DOBRA PRAKTYKA: pisz JOIN … ON — warunek
        // złączenia stoi wtedy przy tabeli i trudniej go zgubić.
    }

    // =================================================================================================
    // 2. LEFT JOIN — WSZYSCY Z LEWEJ, TAKŻE BEZ PARY
    // =================================================================================================

    /**
     * 2. LEFT JOIN zachowuje każdy wiersz lewej tabeli. Gdy nie ma pary, kolumny prawej tabeli są NULL — tak
     * znajdujemy „klientów bez zamówień” (warunek {@code o.id IS NULL}).
     */
    static void leftJoin(Connection con) throws SQLException {
        section("2. LEFT JOIN — wszyscy z lewej, także bez pary");

        printTable(con, """
                SELECT c.name, o.id AS order_id
                FROM customers c
                LEFT JOIN orders o ON o.customer_id = c.id
                WHERE c.city IN ('Warszawa', 'Wrocław')
                ORDER BY c.id, o.id""");
        // WYNIK: NAME           | ORDER_ID
        // WYNIK: ---------------+---------
        // WYNIK: Jan Kowalski   | ZAM-001
        // WYNIK: Jan Kowalski   | ZAM-003
        // WYNIK: Jan Kowalski   | ZAM-010
        // WYNIK: Zofia Krawczyk | ZAM-005
        // WYNIK: Zofia Krawczyk | ZAM-007
        // WYNIK: Ewa Lis        | NULL
        // WYNIK: (wierszy: 6)

        // Klienci BEZ zamówień: LEFT JOIN + IS NULL na kolumnie z prawej tabeli (tzw. anty-złączenie)
        printTable(con, """
                SELECT c.id, c.name
                FROM customers c
                LEFT JOIN orders o ON o.customer_id = c.id
                WHERE o.id IS NULL
                ORDER BY c.id""");
        // WYNIK: ID | NAME
        // WYNIK: ---+--------
        // WYNIK: 7  | Ewa Lis
        // WYNIK: (wierszy: 1)
        // Ewa Lis nie ma zamówień — INNER JOIN w ogóle by jej nie pokazał. Ten sam wynik daje NOT EXISTS (sekcja 9).
    }

    // =================================================================================================
    // 3. WARUNEK W ON CZY W WHERE — PUŁAPKA LEFT JOIN
    // =================================================================================================

    /**
     * 3. Przy LEFT JOIN warunek w ON decyduje, KTÓRE wiersze prawej tabeli dołączyć; warunek w WHERE filtruje
     * gotowy wynik. Warunek na kolumnie prawej tabeli w WHERE odrzuca wiersze z NULL — LEFT staje się INNER.
     */
    static void onVersusWhere(Connection con) throws SQLException {
        section("3. Warunek w ON czy w WHERE — pułapka LEFT JOIN");

        // Cel: WSZYSCY klienci i ich ewentualne NOWE zamówienia
        // PUŁAPKA: status w WHERE — klienci bez NOWYCH zamówień znikają (NULL = 'NOWE' to UNKNOWN)
        printTable(con, """
                SELECT c.name, o.id AS new_order
                FROM customers c
                LEFT JOIN orders o ON o.customer_id = c.id
                WHERE o.status = 'NOWE'
                ORDER BY c.id""");
        // WYNIK: NAME        | NEW_ORDER
        // WYNIK: ------------+----------
        // WYNIK: Maria Nowak | ZAM-009
        // WYNIK: Ola Pawlak  | ZAM-006
        // WYNIK: (wierszy: 2)

        // Poprawnie: status w ON — dołączamy tylko NOWE zamówienia, a klienci zostają wszyscy
        printTable(con, """
                SELECT c.name, o.id AS new_order
                FROM customers c
                LEFT JOIN orders o ON o.customer_id = c.id AND o.status = 'NOWE'
                ORDER BY c.id""");
        // WYNIK: NAME           | NEW_ORDER
        // WYNIK: ---------------+----------
        // WYNIK: Jan Kowalski   | NULL
        // WYNIK: Maria Nowak    | ZAM-009
        // WYNIK: Adam Mazur     | NULL
        // WYNIK: Zofia Krawczyk | NULL
        // WYNIK: Ola Pawlak     | ZAM-006
        // WYNIK: Marek Król     | NULL
        // WYNIK: Ewa Lis        | NULL
        // WYNIK: (wierszy: 7)
        // Reguła: przy LEFT JOIN warunki na tabelę z PRAWEJ piszemy w ON; warunki na tabelę z LEWEJ — w WHERE.
        // Przy INNER JOIN nie ma różnicy, gdzie postawisz warunek (wynik ten sam).
    }

    // =================================================================================================
    // 4. RIGHT JOIN I FULL JOIN
    // =================================================================================================

    /**
     * 4. RIGHT JOIN to LEFT JOIN z zamienioną kolejnością tabel — w praktyce prawie zawsze piszemy LEFT.
     * FULL (OUTER) JOIN zachowuje wiersze bez pary z OBU stron; obsługuje go PostgreSQL, ale nie H2 2.x i nie MySQL.
     */
    static void rightAndFullJoin(Connection con) throws SQLException {
        section("4. RIGHT JOIN i FULL JOIN");

        printTable(con, """
                SELECT c.name, o.id AS order_id
                FROM orders o
                RIGHT JOIN customers c ON o.customer_id = c.id
                WHERE c.city = 'Wrocław'
                ORDER BY c.id""");
        // WYNIK: NAME    | ORDER_ID
        // WYNIK: --------+---------
        // WYNIK: Ewa Lis | NULL
        // WYNIK: (wierszy: 1)
        // To samo co „customers c LEFT JOIN orders o” — DOBRA PRAKTYKA: zawsze LEFT, „główna” tabela na początku.

        expectSqlError(con, "FULL JOIN w H2", "SELECT c.name, o.id FROM customers c FULL JOIN orders o ON o.customer_id = c.id");
        // WYNIK: ✔ FULL JOIN w H2 → SQLState 42000 (błąd składni / nieobsługiwana konstrukcja)
        // Zamiennik FULL JOIN: LEFT JOIN … UNION … RIGHT JOIN (UNION = suma wyników bez powtórzeń).
    }

    // =================================================================================================
    // 5. WIELE-DO-WIELU — TABELA ŁĄCZĄCA
    // =================================================================================================

    /**
     * 5. Zamówienie ma wiele produktów, a produkt bywa w wielu zamówieniach. Taką relację zapisuje tabela łącząca
     * order_lines (order_id, sku, quantity) z kluczem głównym złożonym z obu kluczy obcych.
     */
    static void manyToMany(Connection con) throws SQLException {
        section("5. Wiele-do-wielu — tabela łącząca");

        // Trzy tabele: zamówienie → pozycje → produkty; quantity * price = wartość pozycji
        printTable(con, """
                SELECT o.id, p.name, l.quantity, l.quantity * p.price AS line_total
                FROM orders o
                JOIN order_lines l ON l.order_id = o.id
                JOIN products p    ON p.sku = l.sku
                WHERE o.customer_id = 1
                ORDER BY o.id, p.name""");
        // WYNIK: ID      | NAME              | QUANTITY | LINE_TOTAL
        // WYNIK: --------+-------------------+----------+-----------
        // WYNIK: ZAM-001 | Laptop Pro 14     | 1        | 5499.99
        // WYNIK: ZAM-001 | Słuchawki BT      | 2        | 699.80
        // WYNIK: ZAM-003 | Czysty kod        | 1        | 79.00
        // WYNIK: ZAM-003 | Java. Podstawy    | 1        | 129.00
        // WYNIK: ZAM-003 | Wzorce projektowe | 1        | 99.00
        // WYNIK: ZAM-010 | Czysty kod        | 1        | 79.00
        // WYNIK: ZAM-010 | Oliwa z oliwek    | 2        | 84.00
        // WYNIK: (wierszy: 7)

        // Druga strona relacji: kto kupił Słuchawki BT?
        printTable(con, """
                SELECT c.name, o.id, o.order_date
                FROM products p
                JOIN order_lines l ON l.sku = p.sku
                JOIN orders o      ON o.id = l.order_id
                JOIN customers c   ON c.id = o.customer_id
                WHERE p.name = 'Słuchawki BT'
                ORDER BY o.order_date""");
        // WYNIK: NAME         | ID      | ORDER_DATE
        // WYNIK: -------------+---------+-----------
        // WYNIK: Jan Kowalski | ZAM-001 | 2026-01-05
        // WYNIK: Maria Nowak  | ZAM-009 | 2026-03-28
        // WYNIK: (wierszy: 2)
        // PUŁAPKA: cena w pozycji. Tu liczymy quantity * AKTUALNA cena produktu. Prawdziwy sklep zapisuje w order_lines
        // cenę z chwili zakupu (unit_price) — inaczej zmiana cennika zmieniłaby wartość starych zamówień.
    }

    // =================================================================================================
    // 6. POWTÓRZENIA ZE ZŁĄCZEŃ
    // =================================================================================================

    /**
     * 6. JOIN z tabelą „po stronie wielu” powtarza wiersz z „jednej” strony tyle razy, ile ma dopasowań.
     * Kto pyta o klientów, a dostaje pary klient–pozycja, widzi duplikaty.
     */
    static void duplicatesFromJoins(Connection con) throws SQLException {
        section("6. Powtórzenia ze złączeń");

        // Pytanie: którzy klienci kupili coś z ELEKTRONIKI?
        printTable(con, """
                SELECT c.name
                FROM customers c
                JOIN orders o      ON o.customer_id = c.id
                JOIN order_lines l ON l.order_id = o.id
                JOIN products p    ON p.sku = l.sku
                WHERE p.category = 'ELEKTRONIKA'
                ORDER BY c.name""");
        // WYNIK: NAME
        // WYNIK: --------------
        // WYNIK: Adam Mazur
        // WYNIK: Jan Kowalski
        // WYNIK: Jan Kowalski
        // WYNIK: Maria Nowak
        // WYNIK: Zofia Krawczyk
        // WYNIK: (wierszy: 5)
        // PUŁAPKA: Jan Kowalski dwa razy (laptop i słuchawki — dwie pozycje jednego zamówienia). Zofia kupiła dwa
        // monitory, ale to JEDNA pozycja (quantity = 2), więc jest raz. Liczy się liczba pasujących wierszy, nie sztuk.
        // (Adam Mazur trafił tu przez zamówienie ANULOWANE — pytanie nie wykluczało statusów.)

        // Lekarstwo 1: SELECT DISTINCT (= bez powtórzeń). Lekarstwo 2 (często czytelniejsze): EXISTS — „klient, dla którego istnieje…”
        printTable(con, """
                SELECT c.name
                FROM customers c
                WHERE EXISTS (SELECT 1
                              FROM orders o
                              JOIN order_lines l ON l.order_id = o.id
                              JOIN products p    ON p.sku = l.sku
                              WHERE o.customer_id = c.id AND p.category = 'ELEKTRONIKA')
                ORDER BY c.name""");
        // WYNIK: NAME
        // WYNIK: --------------
        // WYNIK: Adam Mazur
        // WYNIK: Jan Kowalski
        // WYNIK: Maria Nowak
        // WYNIK: Zofia Krawczyk
        // WYNIK: (wierszy: 4)
        // PUŁAPKA: SUM po złączeniu z dwiema tabelami „wielu” naraz (np. pozycje i płatności zamówienia) mnoży
        // kwoty: każda pozycja łączy się z każdą płatnością. Sumuj w podzapytaniach osobno (Jdbc07SqlAggregationIndexes).
    }

    // =================================================================================================
    // 7. SELF-JOIN — PRACOWNIK I KIEROWNIK
    // =================================================================================================

    /**
     * 7. Tabela employees ma kolumnę manager_id wskazującą na inny wiersz TEJ SAMEJ tabeli. Łączymy ją samą ze sobą
     * pod dwoma aliasami: e (pracownik) i m (kierownik). LEFT JOIN — żeby dyrektor (bez kierownika) nie zniknął.
     */
    static void selfJoin(Connection con) throws SQLException {
        section("7. Self-join — pracownik i kierownik");

        // COALESCE(a, b) = pierwsza wartość różna od NULL — zastępuje brak kierownika opisem

        printTable(con, """
                SELECT e.name AS employee, COALESCE(m.name, '(brak — dyrektor)') AS manager
                FROM employees e
                LEFT JOIN employees m ON m.id = e.manager_id
                ORDER BY e.id""");
        // WYNIK: EMPLOYEE            | MANAGER
        // WYNIK: --------------------+--------------------
        // WYNIK: Michał Lewandowski  | (brak — dyrektor)
        // WYNIK: Anna Nowak          | Michał Lewandowski
        // WYNIK: Ewa Woźniak         | Anna Nowak
        // WYNIK: Piotr Kowalski      | Anna Nowak
        // WYNIK: Krzysztof Szymański | Michał Lewandowski
        // WYNIK: Tomasz Wójcik       | Krzysztof Szymański
        // WYNIK: (wierszy: 6)

        // Podwładni Anny Nowak (pracownicy, których kierownik nazywa się Anna Nowak)
        printTable(con, """
                SELECT e.name
                FROM employees e
                JOIN employees m ON m.id = e.manager_id
                WHERE m.name = 'Anna Nowak'
                ORDER BY e.name""");
        // WYNIK: NAME
        // WYNIK: --------------
        // WYNIK: Ewa Woźniak
        // WYNIK: Piotr Kowalski
        // WYNIK: (wierszy: 2)
        // Całe drzewo (podwładni podwładnych…) wymaga zapytania rekurencyjnego WITH RECURSIVE (H2, PostgreSQL, MySQL 8).
    }

    // =================================================================================================
    // 8. Z WIERSZY ZŁĄCZENIA DO OBIEKTÓW ZAGNIEŻDŻONYCH
    // =================================================================================================

    /** Pozycja zamówienia w Javie (LineView = widok pozycji). */
    record LineView(String product, int quantity) {
        @Override
        public String toString() {
            return product + " x" + quantity;
        }
    }

    /** Zamówienie z listą pozycji (OrderView = widok zamówienia). */
    record OrderView(String id, String customer, List<LineView> lines) {
    }

    /**
     * 8. JOIN zwraca płaską tabelę: dane zamówienia powtarzają się w każdym wierszu pozycji. W Javie składamy z tego
     * obiekty: LinkedHashMap po id zamówienia (zachowuje kolejność z ORDER BY), a pozycje dopisujemy do listy.
     */
    static void nestedObjects(Connection con) throws SQLException {
        section("8. Z wierszy złączenia do obiektów zagnieżdżonych");

        List<OrderView> orders = loadOrders(con, "Warszawa");     // load orders = wczytaj zamówienia
        for (OrderView o : orders) {
            System.out.println("   " + o.id() + " (" + o.customer() + "): " + o.lines());
        }
        // WYNIK: ZAM-001 (Jan Kowalski): [Laptop Pro 14 x1, Słuchawki BT x2]
        // WYNIK: ZAM-003 (Jan Kowalski): [Czysty kod x1, Java. Podstawy x1, Wzorce projektowe x1]
        // WYNIK: ZAM-005 (Zofia Krawczyk): [Ekspres do kawy x1, Kawa ziarnista 1kg x2]
        // WYNIK: ZAM-007 (Zofia Krawczyk): [Lampka biurkowa x1, Monitor 27 cali x2]
        // WYNIK: ZAM-010 (Jan Kowalski): [Czysty kod x1, Oliwa z oliwek x2]
        // DOBRA PRAKTYKA: jedno zapytanie z JOIN i ORDER BY id zamówienia zamiast zapytania o pozycje dla każdego
        // zamówienia osobno (to byłby problem N+1 — Jdbc05Dao). Tak samo robią mapowania w JPA (JOIN FETCH).
        // PUŁAPKA: zamówienie BEZ pozycji zniknie przy JOIN — gdy to możliwe, użyj LEFT JOIN i pomiń wiersz z NULL.
    }

    /** Wczytuje zamówienia klientów z miasta, z pozycjami, jednym zapytaniem. */
    static List<OrderView> loadOrders(Connection con, String city) throws SQLException {
        String sql = """
                SELECT o.id, c.name AS customer, p.name AS product, l.quantity
                FROM orders o
                JOIN customers c   ON c.id = o.customer_id
                JOIN order_lines l ON l.order_id = o.id
                JOIN products p    ON p.sku = l.sku
                WHERE c.city = ?
                ORDER BY o.id, p.name""";
        Map<String, OrderView> byId = new LinkedHashMap<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, city);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // computeIfAbsent = utwórz, jeśli brak — pierwszy wiersz zamówienia tworzy OrderView
                    OrderView order = byId.computeIfAbsent(rs.getString("id"),
                            id -> new OrderView(id, rsString(rs, "customer"), new ArrayList<>()));
                    order.lines().add(new LineView(rs.getString("product"), rs.getInt("quantity")));
                }
            }
        }
        return List.copyOf(byId.values());     // copyOf (Java 10+) = niezmienna kopia listy
    }

    /** rsString = tekst z ResultSet: getString bez wyjątku sprawdzanego — lambda w computeIfAbsent nie może rzucić SQLException. */
    static String rsString(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (SQLException e) {
            throw new IllegalStateException("SQLState " + e.getSQLState(), e);
        }
    }

    // =================================================================================================
    // 9. PODZAPYTANIA — IN, EXISTS, NOT EXISTS, SKALARNE
    // =================================================================================================

    /**
     * 9. Podzapytanie to SELECT w nawiasach wewnątrz innego zapytania. IN porównuje z listą wyników, EXISTS
     * sprawdza, czy podzapytanie zwraca choć jeden wiersz, a podzapytanie skalarne zwraca jedną wartość.
     */
    static void subqueries(Connection con) throws SQLException {
        section("9. Podzapytania — IN, EXISTS, NOT EXISTS, skalarne");

        // IN: produkty, które pojawiły się w zamówieniach ANULOWANYCH
        printTable(con, """
                SELECT name FROM products
                WHERE sku IN (SELECT l.sku FROM order_lines l JOIN orders o ON o.id = l.order_id
                              WHERE o.status = 'ANULOWANE')
                ORDER BY name""");
        // WYNIK: NAME
        // WYNIK: ----------
        // WYNIK: Smartfon X
        // WYNIK: (wierszy: 1)

        // Podzapytanie skalarne w WHERE: produkty droższe niż średnia cena (AVG = średnia; więcej w Jdbc07)
        printTable(con, """
                SELECT name, price FROM products
                WHERE price > (SELECT AVG(price) FROM products)
                ORDER BY price DESC""");
        // WYNIK: NAME            | PRICE
        // WYNIK: ----------------+--------
        // WYNIK: Laptop Pro 14   | 5499.99
        // WYNIK: Smartfon X      | 2999.00
        // WYNIK: Ekspres do kawy | 1899.00
        // WYNIK: Monitor 27 cali | 1299.00
        // WYNIK: (wierszy: 4)

        // Podzapytanie skalarne SKORELOWANE w SELECT (wykonywane dla każdego klienta): liczba zamówień
        printTable(con, """
                SELECT c.name,
                       (SELECT COUNT(*) FROM orders o WHERE o.customer_id = c.id) AS orders_count
                FROM customers c
                WHERE c.city = 'Kraków' OR c.id = 7
                ORDER BY c.id""");
        // WYNIK: NAME        | ORDERS_COUNT
        // WYNIK: ------------+-------------
        // WYNIK: Maria Nowak | 2
        // WYNIK: Marek Król  | 1
        // WYNIK: Ewa Lis     | 0
        // WYNIK: (wierszy: 3)

        // PUŁAPKA: NOT IN z podzapytaniem, które zwraca NULL. Kierownicy = manager_id; dyrektor ma NULL.
        // „Kto nie jest niczyim kierownikiem?” przez NOT IN zwraca 0 wierszy (patrz Jdbc01SqlBasics, NOT IN z NULL).
        printTable(con, "SELECT name FROM employees WHERE id NOT IN (SELECT manager_id FROM employees) ORDER BY id");
        // WYNIK: NAME
        // WYNIK: ----
        // WYNIK: (wierszy: 0)
        // Poprawnie: NOT EXISTS — NULL-e mu nie przeszkadzają
        printTable(con, """
                SELECT e.name FROM employees e
                WHERE NOT EXISTS (SELECT 1 FROM employees s WHERE s.manager_id = e.id)
                ORDER BY e.id""");
        // WYNIK: NAME
        // WYNIK: --------------
        // WYNIK: Ewa Woźniak
        // WYNIK: Piotr Kowalski
        // WYNIK: Tomasz Wójcik
        // WYNIK: (wierszy: 3)
        // DOBRA PRAKTYKA: „brak powiązanych wierszy” pisz jako NOT EXISTS (albo LEFT JOIN … IS NULL), nie NOT IN.
    }

    // =================================================================================================
    // 10. PROJEKT RELACYJNY — NORMALIZACJA I KLUCZE OBCE (ON DELETE)
    // =================================================================================================

    /**
     * 10. Normalizacja w skrócie: 1NF — w komórce jedna wartość (nie „Laptop, Mysz” w jednym polu; lista = osobna
     * tabela); 2NF — każda kolumna zależy od CAŁEGO klucza (nazwa produktu nie w order_lines, bo zależy tylko od sku);
     * 3NF — kolumny nie zależą od innych kolumn niebędących kluczem (miasto klienta nie w orders, bo zależy
     * od customer_id). Klucze obce pilnują, by odwołania wskazywały istniejące wiersze.
     */
    static void designAndForeignKeys(Connection con) throws SQLException {
        section("10. Projekt relacyjny — normalizacja i klucze obce");

        // orders.customer_id REFERENCES customers (id) — domyślnie ON DELETE RESTRICT/NO ACTION: nie usuniesz
        // klienta, który ma zamówienia, ani nie dodasz zamówienia nieistniejącego klienta
        expectSqlError(con, "usunięcie klienta z zamówieniami", "DELETE FROM customers WHERE id = 1");
        expectSqlError(con, "zamówienie dla klienta 99",
                "INSERT INTO orders (id, customer_id, order_date, status) VALUES ('ZAM-099', 99, DATE '2026-05-01', 'NOWE')");
        // WYNIK: ✔ usunięcie klienta z zamówieniami → SQLState 23503 (naruszenie klucza obcego)
        // WYNIK: ✔ zamówienie dla klienta 99 → SQLState 23506 (naruszenie klucza obcego (brak rodzica))

        // order_lines.order_id REFERENCES orders (id) ON DELETE CASCADE — usunięcie zamówienia usuwa jego pozycje
        show("pozycji zamówienia ZAM-003 przed", countLines(con, "ZAM-003"));    // countLines = policz pozycje
        try (Statement st = con.createStatement()) {
            show("usunięto zamówień", st.executeUpdate("DELETE FROM orders WHERE id = 'ZAM-003'"));
        }
        show("pozycji zamówienia ZAM-003 po (CASCADE)", countLines(con, "ZAM-003"));
        // WYNIK: pozycji zamówienia ZAM-003 przed → 3
        // WYNIK: usunięto zamówień → 1
        // WYNIK: pozycji zamówienia ZAM-003 po (CASCADE) → 0
        // Inne opcje: ON DELETE SET NULL (odwołanie staje się NULL, np. produkt bez kategorii) i SET DEFAULT.
        // PUŁAPKA: CASCADE jest wygodne, ale niebezpieczne na „ważnych” danych — jedno DELETE klienta mogłoby
        // skasować całą historię zamówień. Dla klientów lepsza jest blokada (RESTRICT) albo „miękkie usunięcie”
        // (kolumna active = FALSE). CASCADE pasuje do części, które bez rodzica nie mają sensu (pozycje zamówienia).
        // DOBRA PRAKTYKA: klucze obce zawsze deklaruj w bazie, a kolumny z kluczem obcym indeksuj (MySQL robi to
        // sam, PostgreSQL — nie): JOIN i sprawdzanie przy DELETE rodzica są wtedy szybkie (Jdbc07SqlAggregationIndexes).
    }

    // =================================================================================================
    // NARZĘDZIA LEKCJI — schemat, dane, wypisywanie
    // =================================================================================================

    static final String SCHEMA = """
            CREATE TABLE customers (
                id    INT          PRIMARY KEY,
                name  VARCHAR(100) NOT NULL,
                city  VARCHAR(50)  NOT NULL,
                email VARCHAR(100) UNIQUE,
                vip   BOOLEAN      NOT NULL
            );
            CREATE TABLE products (
                sku      VARCHAR(10)   PRIMARY KEY,
                name     VARCHAR(100)  NOT NULL,
                category VARCHAR(20)   NOT NULL,
                price    DECIMAL(10,2) NOT NULL
            );
            CREATE TABLE orders (
                id          VARCHAR(10) PRIMARY KEY,
                customer_id INT         NOT NULL REFERENCES customers (id),
                order_date  DATE        NOT NULL,
                status      VARCHAR(20) NOT NULL
            );
            CREATE TABLE order_lines (
                order_id VARCHAR(10) NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
                sku      VARCHAR(10) NOT NULL REFERENCES products (sku),
                quantity INT         NOT NULL CHECK (quantity > 0),
                PRIMARY KEY (order_id, sku)
            );
            CREATE TABLE employees (
                id         INT          PRIMARY KEY,
                name       VARCHAR(100) NOT NULL,
                manager_id INT          REFERENCES employees (id)
            );
            INSERT INTO employees (id, name, manager_id) VALUES
                (1, 'Michał Lewandowski', NULL),
                (2, 'Anna Nowak', 1),
                (3, 'Ewa Woźniak', 2),
                (4, 'Piotr Kowalski', 2),
                (5, 'Krzysztof Szymański', 1),
                (6, 'Tomasz Wójcik', 5);
            """;

    /** Tworzy schemat i wstawia klientów, produkty i zamówienia z SampleData (PreparedStatement — Jdbc03). */
    static void createShop(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            for (String sql : SCHEMA.split(";")) {
                if (!sql.isBlank()) {               // isBlank = czy pusty lub same spacje (Java 11+)
                    st.execute(sql);
                }
            }
        }
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO customers (id, name, city, email, vip) VALUES (?, ?, ?, ?, ?)")) {
            for (Customer c : SampleData.customers()) {
                ps.setLong(1, c.id());
                ps.setString(2, c.name());
                ps.setString(3, c.city());
                ps.setObject(4, c.email(), Types.VARCHAR);   // e-mail bywa null → setObject z typem SQL
                ps.setBoolean(5, c.vip());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO products (sku, name, category, price) VALUES (?, ?, ?, ?)")) {
            for (Product p : SampleData.products()) {
                ps.setString(1, p.sku());
                ps.setString(2, p.name());
                ps.setString(3, p.category().name());
                ps.setBigDecimal(4, p.price());
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
            order.executeBatch();      // najpierw zamówienia (rodzice), potem pozycje — inaczej klucz obcy zaprotestuje
            line.executeBatch();
        }
    }

    static int countLines(Connection con, String orderId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM order_lines WHERE order_id = ?")) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
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
            case "90059" -> "H2: niejednoznaczna nazwa kolumny";
            case "42000" -> "błąd składni / nieobsługiwana konstrukcja";
            case "23503" -> "naruszenie klucza obcego";
            case "23506" -> "naruszenie klucza obcego (brak rodzica)";
            default -> "inny błąd";
        };
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • FROM a JOIN b ON b.a_id = a.id — INNER: tylko pary. LEFT JOIN: wszystkie z lewej, brak pary → NULL.
     *   • Klient bez zamówień: LEFT JOIN … WHERE o.id IS NULL  albo  NOT EXISTS (SELECT 1 … WHERE o.customer_id = c.id).
     *   • LEFT JOIN: warunki na PRAWĄ tabelę w ON (w WHERE zamieniają LEFT w INNER).
     *   • RIGHT = lustrzany LEFT (pisz LEFT). FULL JOIN: PostgreSQL tak, H2 2.x i MySQL nie (LEFT … UNION … RIGHT).
     *   • Wiele-do-wielu = tabela łącząca z PRIMARY KEY (oba klucze obce).
     *   • JOIN z „wieloma” powtarza wiersze → DISTINCT albo EXISTS. Dwie tabele „wielu” naraz mnożą SUM.
     *   • Self-join: ta sama tabela pod dwoma aliasami (e, m); LEFT, żeby nie zgubić korzenia (NULL).
     *   • Płaskie wiersze → obiekty: LinkedHashMap + computeIfAbsent, ORDER BY id rodzica.
     *   • Podzapytania: IN (SELECT …), EXISTS, skalarne (jedna wartość), skorelowane (odwołują się do zewnętrznego wiersza).
     *     NOT IN z NULL w podzapytaniu → 0 wierszy; używaj NOT EXISTS.
     *   • 1NF jedna wartość w komórce; 2NF zależność od całego klucza; 3NF bez zależności między zwykłymi kolumnami.
     *   • FOREIGN KEY: RESTRICT/NO ACTION (domyślnie) blokuje usunięcie rodzica; CASCADE usuwa dzieci; SET NULL.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się INNER JOIN od LEFT JOIN? Kiedy wynik obu jest taki sam?
     *   2. Ile wierszy zwróci (dane lekcji: 7 klientów, Ewa Lis bez zamówień, NOWE mają tylko Ola i Maria):
     *        SELECT c.name FROM customers c LEFT JOIN orders o ON o.customer_id = c.id AND o.status = 'NOWE'  ?
     *   3. ZNAJDŹ BŁĄD (miało pokazać WSZYSTKICH pracowników z nazwą kierownika):
     *        SELECT e.name, m.name FROM employees e JOIN employees m ON m.id = e.manager_id
     *   4. Co wypisze printTable dla:  SELECT COUNT(*) AS n FROM customers c JOIN orders o ON o.customer_id = c.id
     *      — liczbę klientów czy zamówień? (10 zamówień, 6 klientów z zamówieniami)
     *   5. ZNAJDŹ BŁĄD:  SELECT name FROM employees WHERE id NOT IN (SELECT manager_id FROM employees)
     *   6. Dlaczego nazwa produktu nie powinna być kolumną w order_lines? Którą postać normalną to narusza?
     *   7. Co się stanie przy DELETE FROM orders WHERE id = 'ZAM-001', jeśli order_lines.order_id ma ON DELETE CASCADE,
     *      a co — przy DELETE klienta 1, gdy orders.customer_id nie ma żadnej opcji ON DELETE?
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

    /** queryStrings = zapytaj o teksty. Wykonuje zapytanie z parametrami (setObject po kolei) i zwraca pierwszą kolumnę jako tekst. */
    static List<String> queryStrings(Connection con, String sql, Object... params) throws SQLException {
        List<String> result = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString(1));
                }
            }
        }
        return result;
    }

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: klienci z dostarczonym zamówieniem", List.of("Jan Kowalski", "Maria Nowak", "Zofia Krawczyk"),
                () -> inFreshDb("jdbc06_e1", Jdbc06SqlJoins::exercise1));
        Check.equal("ćw. 2: klienci bez zamówień w marcu 2026", List.of("Jan Kowalski", "Adam Mazur", "Ewa Lis"),
                () -> inFreshDb("jdbc06_e2", Jdbc06SqlJoins::exercise2));
        Check.equal("ćw. 3: zamówienia klienta 1 z pozycjami",
                List.of("ZAM-001: Laptop Pro 14 x1, Słuchawki BT x2", "ZAM-003: Czysty kod x1, Java. Podstawy x1, Wzorce projektowe x1",
                        "ZAM-010: Czysty kod x1, Oliwa z oliwek x2"),
                () -> inFreshDb("jdbc06_e3", c -> exercise3(c, 1)));
        Check.equal("ćw. 4: kupili coś, co kupiła Maria Nowak (id 2)", List.of("Jan Kowalski", "Zofia Krawczyk", "Marek Król"),
                () -> inFreshDb("jdbc06_e4", c -> exercise4(c, 2)));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of("Jan Kowalski", "Maria Nowak", "Zofia Krawczyk"),
                () -> inFreshDb("jdbc06_s1", Jdbc06SqlJoins::solution1));
        Check.equal("ćw. 2 (wzorzec)", List.of("Jan Kowalski", "Adam Mazur", "Ewa Lis"),
                () -> inFreshDb("jdbc06_s2", Jdbc06SqlJoins::solution2));
        Check.equal("ćw. 3 (wzorzec)",
                List.of("ZAM-001: Laptop Pro 14 x1, Słuchawki BT x2", "ZAM-003: Czysty kod x1, Java. Podstawy x1, Wzorce projektowe x1",
                        "ZAM-010: Czysty kod x1, Oliwa z oliwek x2"),
                () -> inFreshDb("jdbc06_s3", c -> solution3(c, 1)));
        Check.equal("ćw. 4 (wzorzec)", List.of("Jan Kowalski", "Zofia Krawczyk", "Marek Król"),
                () -> inFreshDb("jdbc06_s4", c -> solution4(c, 2)));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): nazwy klientów (bez powtórzeń, alfabetycznie), którzy mają choć jedno zamówienie
     * o statusie DOSTARCZONE.
     * Podpowiedź: JOIN customers–orders, WHERE o.status = 'DOSTARCZONE', SELECT DISTINCT, ORDER BY c.name.
     */
    static List<String> exercise1(Connection con) throws SQLException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): nazwy klientów (po id), którzy NIE złożyli żadnego zamówienia w marcu 2026
     * (order_date od 2026-03-01 do 2026-03-31). Użyj LEFT JOIN — i uważaj, gdzie stawiasz warunek na datę.
     * Podpowiedź: LEFT JOIN orders o ON o.customer_id = c.id AND o.order_date BETWEEN DATE '2026-03-01' AND
     * DATE '2026-03-31'  … WHERE o.id IS NULL.
     */
    static List<String> exercise2(Connection con) throws SQLException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): JEDNYM zapytaniem wczytaj zamówienia klienta {@code customerId} z pozycjami i zwróć listę
     * tekstów „ZAM-xxx: produkt xN, produkt xN” (zamówienia po id, pozycje po nazwie produktu).
     * Podpowiedź: jak loadOrders w sekcji 8 (LinkedHashMap + computeIfAbsent), potem String.join(", ", …) z LineView.toString.
     */
    static List<String> exercise3(Connection con, int customerId) throws SQLException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): nazwy INNYCH klientów (po id), którzy kupili choć jeden produkt, który kupił też
     * klient {@code customerId}. Bez powtórzeń.
     * Podpowiedź: {@code WHERE c.id <> ?} AND EXISTS (pozycja zamówienia klienta c, której sku jest IN (sku z zamówień
     * klienta ?)). Parametr customerId ustaw dwa razy.
     */
    static List<String> exercise4(Connection con, int customerId) throws SQLException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solution1(Connection con) throws SQLException {
        return queryStrings(con, """
                SELECT DISTINCT c.name
                FROM customers c JOIN orders o ON o.customer_id = c.id
                WHERE o.status = 'DOSTARCZONE'
                ORDER BY c.name""");
    }

    static List<String> solution2(Connection con) throws SQLException {
        return queryStrings(con, """
                SELECT c.name
                FROM customers c
                LEFT JOIN orders o ON o.customer_id = c.id
                                  AND o.order_date BETWEEN DATE '2026-03-01' AND DATE '2026-03-31'
                WHERE o.id IS NULL
                ORDER BY c.id""");
    }

    static List<String> solution3(Connection con, int customerId) throws SQLException {
        String sql = """
                SELECT o.id, p.name, l.quantity
                FROM orders o
                JOIN order_lines l ON l.order_id = o.id
                JOIN products p    ON p.sku = l.sku
                WHERE o.customer_id = ?
                ORDER BY o.id, p.name""";
        Map<String, List<LineView>> byOrder = new LinkedHashMap<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    byOrder.computeIfAbsent(rs.getString("id"), id -> new ArrayList<>())
                            .add(new LineView(rs.getString("name"), rs.getInt("quantity")));
                }
            }
        }
        List<String> result = new ArrayList<>();
        byOrder.forEach((id, lines) -> result.add(id + ": " + String.join(", ", lines.stream().map(LineView::toString).toList())));
        return result;
    }

    static List<String> solution4(Connection con, int customerId) throws SQLException {
        return queryStrings(con, """
                SELECT c.name
                FROM customers c
                WHERE c.id <> ?
                  AND EXISTS (SELECT 1
                              FROM orders o JOIN order_lines l ON l.order_id = o.id
                              WHERE o.customer_id = c.id
                                AND l.sku IN (SELECT l2.sku
                                              FROM orders o2 JOIN order_lines l2 ON l2.order_id = o2.id
                                              WHERE o2.customer_id = ?))
                ORDER BY c.id""", customerId, customerId);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. INNER zwraca tylko pasujące pary; LEFT dodatkowo wiersze z lewej tabeli bez pary (z NULL po prawej).
     *      Wyniki są równe, gdy każdy wiersz lewej tabeli ma co najmniej jedną parę.
     *   2. 7 — każdy klient raz: Ola i Maria z dołączonym NOWYM zamówieniem (każda ma jedno), reszta z NULL.
     *      Warunek na status jest w ON, więc nikogo nie odrzuca.
     *   3. INNER JOIN gubi dyrektora (manager_id = NULL nie ma pary). Trzeba LEFT JOIN employees m … .
     *   4. N = 10 — JOIN daje jeden wiersz na każde zamówienie (pary klient–zamówienie), nie na klienta.
     *      Liczbę klientów z zamówieniami dałoby COUNT(DISTINCT c.id) = 6.
     *   5. Podzapytanie zwraca też NULL (dyrektor nie ma kierownika), więc NOT IN daje UNKNOWN dla każdego wiersza
     *      → 0 wyników. Poprawnie: NOT EXISTS (SELECT 1 FROM employees s WHERE s.manager_id = e.id)
     *      albo … NOT IN (SELECT manager_id FROM employees WHERE manager_id IS NOT NULL).
     *   6. Nazwa zależy tylko od sku, a klucz order_lines to (order_id, sku) — zależność od części klucza łamie 2NF.
     *      Skutek: ta sama nazwa powtarza się w wielu wierszach i można ją zmienić tylko w części z nich.
     *   7. Usunięcie zamówienia ZAM-001 usunie też jego 2 pozycje (CASCADE). DELETE klienta 1 się nie uda
     *      (naruszenie klucza obcego, SQLState 23503) — domyślne zachowanie blokuje usunięcie rodzica z dziećmi.
     */
    // </editor-fold>
}
