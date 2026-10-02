package t29_jdbc_databases;

import helpers.Check;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.sql.DataSource;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: JDBC — połączenie, polecenie, wynik, metadane i SQLException
 *        (JDBC = Java Database Connectivity = łączność Javy z bazami danych)
 *
 * W SKRÓCIE:
 *   JDBC to zestaw INTERFEJSÓW w pakiecie java.sql (Connection, Statement, ResultSet...). Kod piszemy tylko
 *   przeciwko interfejsom, a ich implementację dostarcza sterownik (driver) konkretnej bazy — plik JAR
 *   na classpath. Zmiana bazy = zmiana sterownika i adresu URL, kod zostaje prawie ten sam.
 *
 * ANALOGIA:
 *   Gniazdko elektryczne i wtyczka. java.sql to norma gniazdka (kształt, napięcie), sterownik to zasilacz
 *   konkretnego urządzenia. DriverManager to listwa, która sama dobiera właściwy zasilacz po adresie URL.
 *   Połączenie (Connection) to kabel — drogi w założeniu, więc się go pożycza z puli, a nie kupuje co chwilę.
 *
 * JAK TO DZIAŁA:
 *   Twój kod ──► java.sql (interfejsy) ──► sterownik (np. org.h2.Driver) ──► baza danych
 *   1. DriverManager.getConnection(url)  → Connection   (sesja z bazą; na produkcji: DataSource + pula)
 *   2. con.createStatement()             → Statement    (nośnik jednego polecenia SQL)
 *   3. st.executeQuery("SELECT ...")     → ResultSet    (kursor po wierszach wyniku)
 *   4. while (rs.next()) { rs.getXxx(kolumna) }
 *   5. zamknij w ODWROTNEJ kolejności: ResultSet → Statement → Connection (try-with-resources robi to sam)
 *   URL:  jdbc:h2:mem:jdbc02;DB_CLOSE_DELAY=-1
 *         └┬─┘└┬┘└┬┘└─┬──┘└──────┬───────┘
 *     protokół │ tryb nazwa    ustawienia
 *          sterownik (h2, postgresql, mysql...)
 *
 * SŁÓWKA:
 *   driver = sterownik; connection = połączenie; statement = polecenie; result set = zbiór wyników;
 *   cursor = kursor; metadata = metadane (dane o danych); data source = źródło danych; pool = pula;
 *   was null = czy był NULL; update count = liczba zmienionych wierszy; state = stan; vendor = dostawca
 *
 * ZOBACZ TEŻ: t10_exceptions/Exceptions04TryWithResources (zamykanie zasobów),
 *             t29_jdbc_databases/Jdbc03PreparedStatement (bezpieczne parametry),
 *             t29_jdbc_databases/Jdbc05Dao (gdzie ukryć cały ten kod)
 * </pre>
 */
public class Jdbc02Connection {

    /** DB_CLOSE_DELAY=-1: baza w pamięci żyje do końca programu, nawet gdy na chwilę nie ma żadnego połączenia. */
    private static final String URL = "jdbc:h2:mem:jdbc02;DB_CLOSE_DELAY=-1";
    /** Użytkownik bazy testowej; pierwsze połączenie z nową bazą H2 zakłada tego użytkownika jako administratora. */
    private static final String USER = "sa";
    /** Puste hasło jest dopuszczalne TYLKO dla bazy w pamięci na czas nauki. */
    private static final String PASSWORD = "";

    public static void main(String[] args) throws SQLException {
        title("Jdbc02 — JDBC: Connection, Statement, ResultSet");

        architecture();        // architecture = architektura
        urlAnatomy();          // url anatomy = budowa adresu URL
        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD)) {
            createShop(con);   // create shop = utwórz sklep (tabele i dane)
            lifecycle(con);    // lifecycle = cykl życia
            navigation(con);   // navigation = poruszanie się po wynikach
            gettersAndNull(con);   // getters and null = gettery i NULL
            metadata(con);     // metadata = metadane
            executeVariants(con);  // execute variants = odmiany execute
            sqlExceptionAnatomy(con);  // sql exception anatomy = budowa SQLException
        }
        dataSourceAndPools();  // data source and pools = źródło danych i pule połączeń
        exercises();           // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. ARCHITEKTURA — API, STEROWNIK, DRIVERMANAGER
    // =================================================================================================

    /**
     * 1. Od JDBC 4.0 (Java 6) sterownik rejestruje się sam: JAR zawiera plik META-INF/services/java.sql.Driver,
     * a DriverManager znajduje go przez ServiceLoader. Stare {@code Class.forName("org.h2.Driver")} jest zbędne.
     * DriverManager pyta po kolei każdy sterownik „czy obsługujesz ten URL?” i bierze pierwszy, który odpowie tak.
     */
    static void architecture() throws SQLException {
        section("1. Architektura — API, sterownik, DriverManager");

        // getDriver = pobierz sterownik obsługujący URL; getClass().getName() = pełna nazwa klasy
        show("sterownik dla jdbc:h2:...", DriverManager.getDriver(URL).getClass().getName());
        // WYNIK: sterownik dla jdbc:h2:... → org.h2.Driver

        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD)) {
            // Connection to interfejs z java.sql; obiekt jest klasą ze sterownika (pakiet org.h2.jdbc)
            show("pakiet klasy połączenia", con.getClass().getPackageName());   // getPackageName = nazwa pakietu (Java 9+)
            // WYNIK: pakiet klasy połączenia → org.h2.jdbc
        }

        // PUŁAPKA: brak sterownika na classpath (albo literówka w URL) → SQLException „No suitable driver”, SQLState 08001
        try {
            DriverManager.getConnection("jdbc:postgresql://localhost:5432/sklep");
        } catch (SQLException e) {
            show("brak sterownika PostgreSQL → SQLState", e.getSQLState());
            // WYNIK: brak sterownika PostgreSQL → SQLState → 08001
        }
        // Klasa 08 w SQLState = problemy z połączeniem. Ten wyjątek rzuca DriverManager, zanim dotrze do jakiejkolwiek bazy.

        // DOBRA PRAKTYKA: w kodzie importuj tylko java.sql / javax.sql, nigdy klas sterownika (org.h2..., org.postgresql...).
        // Dlaczego? Wtedy sterownik jest potrzebny dopiero przy uruchomieniu (w Mavenie: scope runtime), a zmiana bazy
        // nie wymaga zmian w kodzie Javy.
    }

    // =================================================================================================
    // 2. BUDOWA URL — TRYBY H2 I INNE BAZY
    // =================================================================================================

    /**
     * 2. URL ma postać {@code jdbc:<sterownik>:<reszta zależna od sterownika>}. Dla H2 w pamięci ważne jest
     * DB_CLOSE_DELAY: bez niego baza znika razem z ostatnim zamkniętym połączeniem.
     */
    static void urlAnatomy() throws SQLException {
        section("2. Budowa URL — tryby H2 i inne bazy");

        // Przykładowe adresy (tylko tekst — nie łączymy się z nimi):
        //   jdbc:h2:mem:test                         H2 w pamięci, znika po zamknięciu ostatniego połączenia
        //   jdbc:h2:mem:test;DB_CLOSE_DELAY=-1       H2 w pamięci, żyje do końca działania JVM
        //   jdbc:h2:./data/sklep                     H2 w pliku (data/sklep.mv.db) — przetrwa restart
        //   jdbc:postgresql://localhost:5432/sklep   PostgreSQL: host, port, nazwa bazy
        //   jdbc:mysql://localhost:3306/sklep        MySQL
        //   jdbc:oracle:thin:@//localhost:1521/XE    Oracle

        // Baza BEZ DB_CLOSE_DELAY: tworzymy tabelę, zamykamy jedyne połączenie, łączymy się ponownie
        try (Connection first = DriverManager.getConnection("jdbc:h2:mem:jdbc02_short");
             Statement st = first.createStatement()) {
            st.execute("CREATE TABLE temp_notes (id INT)");
        }
        show("bez DB_CLOSE_DELAY: tabela przetrwała?", tableExists("jdbc:h2:mem:jdbc02_short", "TEMP_NOTES"));
        // WYNIK: bez DB_CLOSE_DELAY: tabela przetrwała? → false

        try (Connection first = DriverManager.getConnection("jdbc:h2:mem:jdbc02_long;DB_CLOSE_DELAY=-1");
             Statement st = first.createStatement()) {
            st.execute("CREATE TABLE temp_notes (id INT)");
        }
        show("z DB_CLOSE_DELAY=-1: tabela przetrwała?", tableExists("jdbc:h2:mem:jdbc02_long;DB_CLOSE_DELAY=-1", "TEMP_NOTES"));
        // WYNIK: z DB_CLOSE_DELAY=-1: tabela przetrwała? → true

        // PUŁAPKA: „jdbc:h2:mem:” bez nazwy tworzy prywatną bazę dla JEDNEGO połączenia — drugie połączenie
        // widzi zupełnie inną, pustą bazę. Do testów z kilkoma połączeniami zawsze nadawaj nazwę.
    }

    /** Sprawdza w metadanych bazy, czy tabela istnieje (getTables = pobierz tabele pasujące do wzorca). */
    static boolean tableExists(String url, String table) throws SQLException {
        try (Connection con = DriverManager.getConnection(url);
             ResultSet rs = con.getMetaData().getTables(null, null, table, new String[] {"TABLE"})) {
            return rs.next();
        }
    }

    // =================================================================================================
    // 3. CYKL ŻYCIA — OTWIERANIE I ZAMYKANIE W ODWROTNEJ KOLEJNOŚCI
    // =================================================================================================

    /**
     * 3. Connection, Statement i ResultSet trzymają zasoby po stronie bazy (sesję, kursor, pamięć). Trzeba je zamknąć.
     * try-with-resources zamyka zasoby w kolejności ODWROTNEJ do otwierania — najpierw najmłodszy.
     * Zamknięcie „rodzica” zamyka też „dzieci”: Statement zamyka swój ResultSet, Connection — swoje Statementy.
     */
    static void lifecycle(Connection con) throws SQLException {
        section("3. Cykl życia — zamykanie w odwrotnej kolejności");

        // Tracked = śledzony: zasób-atrapa, który mówi, kiedy go otwarto i zamknięto
        try (Tracked c = new Tracked("Connection");
             Tracked s = new Tracked("Statement");
             Tracked r = new Tracked("ResultSet")) {
            note("praca z: " + c + " → " + s + " → " + r);
        }
        // WYNIK: otwieram Connection
        // WYNIK: otwieram Statement
        // WYNIK: otwieram ResultSet
        // WYNIK: ℹ praca z: Connection → Statement → ResultSet
        // WYNIK: zamykam ResultSet
        // WYNIK: zamykam Statement
        // WYNIK: zamykam Connection

        // Prawdziwe obiekty JDBC: zamykamy tylko Statement i sprawdzamy, co się stało z ResultSetem
        ResultSet leaked;   // leaked = „wyciekły” — trzymamy referencję poza blokiem tylko po to, by ją zbadać
        try (Statement st = con.createStatement()) {
            leaked = st.executeQuery("SELECT id FROM customers ORDER BY id");
            show("ResultSet zamknięty w trakcie?", leaked.isClosed());   // isClosed = czy zamknięty
            // WYNIK: ResultSet zamknięty w trakcie? → false
        }
        show("ResultSet zamknięty po zamknięciu Statement?", leaked.isClosed());
        // WYNIK: ResultSet zamknięty po zamknięciu Statement? → true

        // PUŁAPKA: używanie zamkniętego wyniku → SQLException (H2: kod 90007 „obiekt już zamknięty”)
        try {
            leaked.next();
        } catch (SQLException e) {
            show("next() na zamkniętym ResultSet → SQLState", e.getSQLState());
            // WYNIK: next() na zamkniętym ResultSet → SQLState → 90007
        }
        // SQLState zaczynające się od 9 nie pochodzą ze standardu — to własne kody H2. Inna baza da inny kod.

        // PUŁAPKA: metoda, która ZWRACA ResultSet, a sama zamyka Statement, oddaje martwy obiekt.
        // Dlatego w metodach przepisujemy wiersze do listy/rekordów (patrz Jdbc05Dao), a nie zwracamy ResultSet.
        // DOBRA PRAKTYKA: zawsze try-with-resources dla Connection, Statement i ResultSet. Dlaczego? Niezamknięte
        // połączenia wyczerpują pulę i bazę („too many connections”) — błąd pojawia się dopiero pod obciążeniem.
    }

    /** Zasób-atrapa do pokazania kolejności zamykania. */
    static final class Tracked implements AutoCloseable {
        private final String name;

        Tracked(String name) {
            this.name = name;
            System.out.println("otwieram " + name);
        }

        @Override
        public void close() {
            System.out.println("zamykam " + name);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    // =================================================================================================
    // 4. NAWIGACJA PO RESULTSET — KURSOR
    // =================================================================================================

    /**
     * 4. ResultSet to kursor. Na początku stoi PRZED pierwszym wierszem; {@code next()} przesuwa go i zwraca false,
     * gdy wierszy zabrakło. Domyślny ResultSet jest TYPE_FORWARD_ONLY (tylko do przodu): nie cofniesz się.
     * Kolumny wskazujemy numerem (od 1!) albo etykietą (nazwą lub aliasem z SELECT).
     */
    static void navigation(Connection con) throws SQLException {
        section("4. Nawigacja po ResultSet — kursor");

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, name, city FROM customers WHERE city = 'Warszawa' ORDER BY id")) {

            // PUŁAPKA: odczyt przed pierwszym next() — kursor nie stoi na żadnym wierszu
            try {
                rs.getString("name");
            } catch (SQLException e) {
                show("getString przed next() → SQLState", e.getSQLState());
                // WYNIK: getString przed next() → SQLState → 02000
            }
            // 02000 = standardowy kod „brak danych” (no data).

            while (rs.next()) {
                // getInt(1) = numer kolumny; getString("name") = etykieta (wielkość liter nie ma znaczenia)
                System.out.println("   " + rs.getInt(1) + " | " + rs.getString("name") + " | " + rs.getString("CITY"));
            }
            // WYNIK: 1 | Jan Kowalski | Warszawa
            // WYNIK: 4 | Zofia Krawczyk | Warszawa

            show("rs.next() po ostatnim wierszu", rs.next());
            // WYNIK: rs.next() po ostatnim wierszu → false

            show("typ wyniku to TYPE_FORWARD_ONLY?", rs.getType() == ResultSet.TYPE_FORWARD_ONLY);
            // WYNIK: typ wyniku to TYPE_FORWARD_ONLY? → true
        }

        // PUŁAPKA: kolumna, której nie ma w SELECT → SQLException 42S22. Numer 0 też jest błędny (H2: 90008).
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, name AS full_name FROM customers ORDER BY id")) {
            rs.next();
            try {
                rs.getString("email");   // email istnieje w tabeli, ale nie w tym SELECT
            } catch (SQLException e) {
                show("getString(\"email\") spoza SELECT → SQLState", e.getSQLState());
                // WYNIK: getString("email") spoza SELECT → SQLState → 42S22
            }
            show("getString(\"full_name\")", rs.getString("full_name"));
            // WYNIK: getString("full_name") → Jan Kowalski
        }
        // Specyfikacja JDBC każe szukać kolumny po ETYKIECIE, czyli po aliasie (full_name). H2 znajdzie ją także po
        // oryginalnej nazwie „name”, ale nie każdy sterownik to potrafi — przy aliasie używaj aliasu.
        // DOBRA PRAKTYKA: odczytuj kolumny po nazwie (etykiecie), nie po numerze. Dlaczego? Dodanie kolumny do SELECT
        // przesuwa numery, a nazwy zostają. Numerów używaj tylko w krótkich, oczywistych zapytaniach (SELECT COUNT(*)).
    }

    // =================================================================================================
    // 5. GETTERY I NULL — PUŁAPKA getInt + wasNull
    // =================================================================================================

    /**
     * 5. Gettery typów prostych (getInt, getLong, getDouble, getBoolean) nie mogą zwrócić null, więc dla NULL
     * zwracają 0 / false. Odróżnienie „0” od „brak” wymaga {@code wasNull()} albo {@code getObject(kol, Integer.class)}.
     * Daty czytamy jako java.time: {@code getObject(kol, LocalDate.class)} (JDBC 4.2, Java 8+).
     */
    static void gettersAndNull(Connection con) throws SQLException {
        section("5. Gettery i NULL — pułapka getInt");

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT name, discount FROM customers WHERE id IN (2, 3) ORDER BY id")) {
            while (rs.next()) {
                int asInt = rs.getInt("discount");
                boolean wasNull = rs.wasNull();      // wasNull = czy OSTATNIO odczytana kolumna była NULL
                Integer asObject = rs.getObject("discount", Integer.class);   // getObject = pobierz jako obiekt
                System.out.println("   " + rs.getString("name") + ": getInt=" + asInt + ", wasNull=" + wasNull
                        + ", getObject=" + asObject);
            }
            // WYNIK: Maria Nowak: getInt=0, wasNull=true, getObject=null
            // WYNIK: Adam Mazur: getInt=0, wasNull=false, getObject=0
        }
        // PUŁAPKA: getInt zwróciło 0 dla obu klientów, choć Maria NIE MA rabatu (NULL), a Adam ma rabat 0.
        // wasNull() dotyczy OSTATNIEGO gettera — wywołaj je od razu, zanim odczytasz kolejną kolumnę.

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, customer_id, order_date, total, paid FROM orders WHERE id IN ('ZAM-001', 'ZAM-006') ORDER BY id")) {
            while (rs.next()) {
                LocalDate date = rs.getObject("order_date", LocalDate.class);
                BigDecimal total = rs.getBigDecimal("total");   // getBigDecimal = pobierz jako BigDecimal (NULL → null)
                boolean paid = rs.getBoolean("paid");          // NULL → false!
                System.out.println("   " + rs.getString("id") + ": data=" + date + ", dzień tygodnia=" + date.getDayOfWeek()
                        + ", suma=" + total + ", paid=" + paid + ", paid jako obiekt=" + rs.getObject("paid"));
            }
            // WYNIK: ZAM-001: data=2026-01-05, dzień tygodnia=MONDAY, suma=6199.79, paid=true, paid jako obiekt=true
            // WYNIK: ZAM-006: data=2026-03-01, dzień tygodnia=SUNDAY, suma=608.97, paid=false, paid jako obiekt=null
        }
        // getObject("paid") bez klasy zwraca obiekt typu wybranego przez sterownik (tu Boolean; null dla NULL).

        // PUŁAPKA: getter niepasujący do danych. getInt na tekście „Jan Kowalski” → SQLException 22018
        // (klasa 22 = błąd danych, „nieprawidłowa wartość do konwersji”). getString działa na prawie wszystkim.
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT name FROM customers WHERE id = 1")) {
            rs.next();
            try {
                rs.getInt("name");
            } catch (SQLException e) {
                show("getInt na tekście → SQLState", e.getSQLState());
                // WYNIK: getInt na tekście → SQLState → 22018
            }
        }

        // DOBRA PRAKTYKA: dla kolumn, które mogą być NULL, używaj getObject(kol, Integer.class) / getBigDecimal /
        // getObject(kol, LocalDate.class) — dostajesz null i nie zgubisz informacji. Starych java.sql.Date/Timestamp
        // (getDate, getTimestamp) unikaj: mają kłopotliwe strefy czasowe i są zmienne (mutable).
    }

    // =================================================================================================
    // 6. METADANE — ResultSetMetaData I DatabaseMetaData
    // =================================================================================================

    /**
     * 6. ResultSetMetaData opisuje kolumny wyniku (nazwy, typy, NULL-owalność) — dzięki niemu można napisać
     * uniwersalne wypisywanie tabel. DatabaseMetaData opisuje bazę i sterownik (nazwa produktu, możliwości).
     */
    static void metadata(Connection con) throws SQLException {
        section("6. Metadane — ResultSetMetaData i DatabaseMetaData");

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, order_date, total, paid FROM orders ORDER BY id")) {
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {   // getColumnCount = liczba kolumn
                // getColumnLabel = etykieta; getColumnType = kod z java.sql.Types; JDBCType = enum z czytelną nazwą (Java 8+)
                // getColumnClassName = klasa Javy, którą zwróci getObject; isNullable = czy może być NULL
                System.out.println("   " + meta.getColumnLabel(i)
                        + " | " + JDBCType.valueOf(meta.getColumnType(i)).getName()
                        + " | " + meta.getColumnClassName(i)
                        + " | NULL dozwolony: " + (meta.isNullable(i) == ResultSetMetaData.columnNullable));
            }
            // WYNIK: ID | VARCHAR | java.lang.String | NULL dozwolony: false
            // WYNIK: ORDER_DATE | DATE | java.sql.Date | NULL dozwolony: false
            // WYNIK: TOTAL | DECIMAL | java.math.BigDecimal | NULL dozwolony: false
            // WYNIK: PAID | BOOLEAN | java.lang.Boolean | NULL dozwolony: true
        }
        // Zwróć uwagę: getObject("order_date") bez klasy zwróciłby starą java.sql.Date — dlatego podajemy LocalDate.class.

        DatabaseMetaData db = con.getMetaData();
        // Pełnej wersji nie wypisujemy (zmienia się przy aktualizacji sterownika) — tylko fakt, że to H2
        show("nazwa produktu zawiera H2", db.getDatabaseProductName().contains("H2"));
        // WYNIK: nazwa produktu zawiera H2 → true
        show("obsługuje paczki poleceń (batch)", db.supportsBatchUpdates());
        // WYNIK: obsługuje paczki poleceń (batch) → true
        show("identyfikatory zapisuje WIELKIMI literami", db.storesUpperCaseIdentifiers());
        // WYNIK: identyfikatory zapisuje WIELKIMI literami → true
        show("znak cytowania identyfikatorów", db.getIdentifierQuoteString());
        // WYNIK: znak cytowania identyfikatorów → "
        // W MySQL znakiem cytowania jest odwrócony apostrof ` (backtick), w PostgreSQL — cudzysłów jak tu.

        // DOBRA PRAKTYKA: metadane wykorzystują narzędzia (IntelliJ Database, Flyway, Hibernate). W zwykłym kodzie
        // aplikacji rzadko ich potrzebujesz — znasz swoje tabele.
    }

    // =================================================================================================
    // 7. execute vs executeQuery vs executeUpdate
    // =================================================================================================

    /**
     * 7. Trzy metody Statement:
     * {@code executeQuery} — tylko SELECT, zwraca ResultSet;
     * {@code executeUpdate} — INSERT/UPDATE/DELETE/DDL, zwraca liczbę wierszy (DDL: 0);
     * {@code execute} — cokolwiek, zwraca true, gdy wynikiem jest ResultSet (wtedy getResultSet), false — gdy liczba
     * wierszy (wtedy getUpdateCount).
     */
    static void executeVariants(Connection con) throws SQLException {
        section("7. execute vs executeQuery vs executeUpdate");

        try (Statement st = con.createStatement()) {
            int changed = st.executeUpdate("UPDATE customers SET discount = 20 WHERE city = 'Warszawa'");
            show("executeUpdate(UPDATE)", changed);
            // WYNIK: executeUpdate(UPDATE) → 2

            boolean isQuery = st.execute("SELECT COUNT(*) FROM customers");
            show("execute(SELECT) zwraca", isQuery);
            // WYNIK: execute(SELECT) zwraca → true
            try (ResultSet rs = st.getResultSet()) {   // getResultSet = pobierz wynik ostatniego execute
                rs.next();
                show("  getResultSet → COUNT(*)", rs.getLong(1));
                // WYNIK: getResultSet → COUNT(*) → 7
            }

            isQuery = st.execute("UPDATE customers SET discount = 10 WHERE city = 'Warszawa'");
            show("execute(UPDATE) zwraca", isQuery);
            // WYNIK: execute(UPDATE) zwraca → false
            show("  getUpdateCount", st.getUpdateCount());   // getUpdateCount = liczba zmienionych wierszy
            // WYNIK: getUpdateCount → 2

            // PUŁAPKA: zła metoda do rodzaju polecenia → SQLException (w H2 kody 90002 i 90001)
            try {
                st.executeQuery("DELETE FROM customers WHERE id = 999");
            } catch (SQLException e) {
                show("executeQuery(DELETE) → SQLState", e.getSQLState());
                // WYNIK: executeQuery(DELETE) → SQLState → 90002
            }
            try {
                st.executeUpdate("SELECT * FROM customers");
            } catch (SQLException e) {
                show("executeUpdate(SELECT) → SQLState", e.getSQLState());
                // WYNIK: executeUpdate(SELECT) → SQLState → 90001
            }
        }
        // PUŁAPKA: jeden Statement = jeden otwarty ResultSet. Kolejne execute na tym samym Statement zamyka
        // poprzedni wynik — nie zagnieżdżaj pętli po dwóch wynikach z jednego Statement.
        // DOBRA PRAKTYKA: execute zostaw narzędziom, które nie wiedzą, co dostaną (konsola SQL, skrypty).
        // W aplikacji wybieraj executeQuery albo executeUpdate — kod jest czytelniejszy i błędy wychodzą od razu.
    }

    // =================================================================================================
    // 8. SQLException — SQLState, KOD BŁĘDU, PODKLASY, ŁAŃCUCH
    // =================================================================================================

    /**
     * 8. SQLException niesie: SQLState (5 znaków; 2 pierwsze = klasa, ze standardu SQL/X-Open), kod błędu dostawcy
     * (getErrorCode — inny w każdej bazie), przyczynę (getCause) i łańcuch kolejnych błędów (getNextException).
     * Od JDBC 4.0 sterowniki rzucają też podklasy, np. SQLIntegrityConstraintViolationException, SQLSyntaxErrorException.
     */
    static void sqlExceptionAnatomy(Connection con) throws SQLException {
        section("8. SQLException — SQLState, kod błędu, podklasy, łańcuch");

        try (Statement st = con.createStatement()) {
            st.executeUpdate("INSERT INTO customers (id, name, city) VALUES (1, 'Duplikat', 'Łódź')");
        } catch (SQLIntegrityConstraintViolationException e) {   // podklasa dla klasy SQLState 23
            show("SQLState", e.getSQLState());
            // WYNIK: SQLState → 23505
            show("klasa SQLState (2 znaki)", e.getSQLState().substring(0, 2));
            // WYNIK: klasa SQLState (2 znaki) → 23
            show("kod błędu H2", e.getErrorCode());
            // WYNIK: kod błędu H2 → 23505
            note("złapany przez catch (SQLIntegrityConstraintViolationException e)");
            // WYNIK: ℹ złapany przez catch (SQLIntegrityConstraintViolationException e)
        }

        try (Statement st = con.createStatement()) {
            st.executeQuery("SELECT nieistniejaca_kolumna FROM customers");
        } catch (SQLSyntaxErrorException e) {                    // podklasa dla klasy SQLState 42
            show("nieznana kolumna: SQLState / kod H2", e.getSQLState() + " / " + e.getErrorCode());
            // WYNIK: nieznana kolumna: SQLState / kod H2 → 42S22 / 42122
        }
        // SQLState jest (w miarę) przenośny, kod błędu — nie: ten sam błąd w PostgreSQL ma SQLState 42703,
        // a MySQL zgłasza 42S22 z kodem 1054. Stąd wniosek: decyzje w kodzie opieraj na KLASIE SQLState lub podklasie wyjątku.

        // Łańcuch błędów: paczka (batch) z trzema INSERT-ami, z których drugi łamie klucz główny
        try (Statement st = con.createStatement()) {
            st.addBatch("INSERT INTO customers (id, name, city) VALUES (10, 'Nowy A', 'Łódź')");   // addBatch = dodaj do paczki
            st.addBatch("INSERT INTO customers (id, name, city) VALUES (1, 'Duplikat', 'Łódź')");
            st.addBatch("INSERT INTO customers (id, name, city) VALUES (11, 'Nowy B', 'Łódź')");
            st.executeBatch();   // executeBatch = wykonaj paczkę
        } catch (BatchUpdateException e) {
            show("BatchUpdateException, SQLState", e.getSQLState());
            // WYNIK: BatchUpdateException, SQLState → 23505
            SQLException next = e.getNextException();   // getNextException = następny wyjątek w łańcuchu
            show("getNextException() → SQLState", next == null ? "brak" : next.getSQLState());
            // WYNIK: getNextException() → SQLState → 23505
            // getUpdateCounts = liczby wierszy dla każdego polecenia; -3 = Statement.EXECUTE_FAILED (nie powiodło się)
            show("getUpdateCounts", e.getUpdateCounts());
            // WYNIK: getUpdateCounts → [1, -3, 1]
        }
        // H2 wykonał 1. i 3. polecenie mimo błędu w 2. Inne sterowniki mogą przerwać paczkę na pierwszym błędzie —
        // zachowanie zależy od sterownika. Pewność daje dopiero transakcja (Jdbc04Transactions).
        try (Statement st = con.createStatement()) {
            st.executeUpdate("DELETE FROM customers WHERE id IN (10, 11)");   // sprzątanie po paczce
        }

        // PUŁAPKA: catch (SQLException e) { e.printStackTrace(); } i dalej „jakby nic”. Dlaczego źle? Program
        // pracuje na niepełnych danych, a ślad ginie w logach. Albo obsłuż błąd świadomie (np. 23505 → „login zajęty”),
        // albo opakuj go w wyjątek z kontekstem i rzuć dalej (Jdbc05Dao: własny DataAccessException).
    }

    // =================================================================================================
    // 9. DataSource, PULE POŁĄCZEŃ I HASŁA
    // =================================================================================================

    /**
     * 9. {@code javax.sql.DataSource} to fabryka połączeń: kod prosi o {@code getConnection()} i nie wie, skąd
     * pochodzi połączenie. Produkcyjne DataSource (HikariCP — domyślne w Spring Boot) trzymają PULĘ otwartych
     * połączeń: close() nie zamyka fizycznego połączenia, tylko oddaje je do puli. Poniżej najprostsza własna
     * implementacja — bez puli, tylko pokazuje interfejs.
     */
    static void dataSourceAndPools() throws SQLException {
        section("9. DataSource, pule połączeń i hasła");

        // Dane logowania z ZEWNĄTRZ kodu: zmienna środowiskowa, a gdy jej nie ma — wartość dla lokalnej bazy testowej.
        // getenv = pobierz zmienną środowiskową. Hasła NIGDY nie wypisujemy ani nie logujemy.
        String user = envOr("SHOP_DB_USER", "sa");
        String password = envOr("SHOP_DB_PASSWORD", "");

        DataSource ds = new SimpleDataSource(URL, user, password);
        try (Connection con = ds.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM customers")) {
            rs.next();
            show("klientów przez DataSource", rs.getLong(1));
            // WYNIK: klientów przez DataSource → 7
            show("połączenie żywe (isValid)", con.isValid(1));   // isValid = czy działa (limit czasu w sekundach)
            // WYNIK: połączenie żywe (isValid) → true
        }

        // Dlaczego pula? Otwarcie połączenia z prawdziwą bazą to sieć, uwierzytelnienie, nowa sesja na serwerze —
        // zwykle kilka–kilkadziesiąt milisekund. Pula otwiera np. 10 połączeń raz i pożycza je wątkom.
        // Typowa konfiguracja HikariCP (opis, nie kod): maximumPoolSize (rozmiar puli), connectionTimeout (ile czekać
        // na wolne połączenie), maxLifetime (po jakim czasie wymienić połączenie na nowe).
        // PUŁAPKA: niezamknięte połączenie z puli nigdy do niej nie wraca. Po kilku takich wyciekach kolejne wątki
        // czekają na connectionTimeout i dostają wyjątek — aplikacja „staje”, choć baza jest zdrowa.
        // DOBRA PRAKTYKA: URL, login i hasło trzymaj w konfiguracji (zmienne środowiskowe, plik spoza repozytorium,
        // menedżer sekretów), nigdy w kodzie ani w gicie. W Springu: application.properties + zmienne środowiskowe.
        note("w Spring Boot DataSource z pulą HikariCP tworzy się sam z ustawień spring.datasource.*");
        // WYNIK: ℹ w Spring Boot DataSource z pulą HikariCP tworzy się sam z ustawień spring.datasource.*
    }

    /** Zwraca zmienną środowiskową albo wartość domyślną, gdy jej brak lub jest pusta. */
    static String envOr(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;   // isBlank (Java 11+)
    }

    /** Minimalny DataSource bez puli: każde getConnection() otwiera nowe połączenie przez DriverManager. */
    static final class SimpleDataSource implements DataSource {
        private final String url;
        private final String user;
        private final String password;
        private PrintWriter logWriter;
        private int loginTimeout;

        SimpleDataSource(String url, String user, String password) {
            this.url = url;
            this.user = user;
            this.password = password;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(url, user, password);
        }

        @Override
        public Connection getConnection(String username, String pass) throws SQLException {
            return DriverManager.getConnection(url, username, pass);
        }

        @Override
        public PrintWriter getLogWriter() {
            return logWriter;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
            this.logWriter = out;
        }

        @Override
        public void setLoginTimeout(int seconds) {
            this.loginTimeout = seconds;
        }

        @Override
        public int getLoginTimeout() {
            return loginTimeout;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException("brak loggera");
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {   // unwrap = odpakuj do konkretnego typu
            if (iface.isInstance(this)) {
                return iface.cast(this);
            }
            throw new SQLException("nie jest " + iface.getName());
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {   // is wrapper for = czy opakowuje dany typ
            return iface.isInstance(this);
        }
    }

    // =================================================================================================
    // NARZĘDZIA LEKCJI — dane sklepu
    // =================================================================================================

    static final String SCHEMA_AND_DATA = """
            CREATE TABLE customers (
                id       INT          PRIMARY KEY,
                name     VARCHAR(100) NOT NULL,
                city     VARCHAR(50)  NOT NULL,
                email    VARCHAR(100) UNIQUE,
                discount INT
            );
            INSERT INTO customers (id, name, city, email, discount) VALUES
                (1, 'Jan Kowalski',   'Warszawa', 'jan@example.com',        10),
                (2, 'Maria Nowak',    'Kraków',   NULL,                     NULL),
                (3, 'Adam Mazur',     'Gdańsk',   'adam.mazur@example.com', 0),
                (4, 'Zofia Krawczyk', 'Warszawa', 'zofia@example.com',      15),
                (5, 'Ola Pawlak',     'Poznań',   NULL,                     NULL),
                (6, 'Marek Król',     'Kraków',   'marek@example.com',      5),
                (7, 'Ewa Lis',        'Wrocław',  'ewa.lis@example.com',    0);
            CREATE TABLE orders (
                id          VARCHAR(10)   PRIMARY KEY,
                customer_id INT           NOT NULL REFERENCES customers (id),
                order_date  DATE          NOT NULL,
                status      VARCHAR(20)   NOT NULL,
                total       DECIMAL(10,2) NOT NULL,
                paid        BOOLEAN
            );
            INSERT INTO orders (id, customer_id, order_date, status, total, paid) VALUES
                ('ZAM-001', 1, DATE '2026-01-05', 'DOSTARCZONE', 6199.79, TRUE),
                ('ZAM-002', 2, DATE '2026-01-12', 'DOSTARCZONE',  269.87, TRUE),
                ('ZAM-003', 1, DATE '2026-02-02', 'WYSLANE',      307.00, TRUE),
                ('ZAM-004', 3, DATE '2026-02-14', 'ANULOWANE',   2999.00, FALSE),
                ('ZAM-005', 4, DATE '2026-02-20', 'OPLACONE',    2028.98, TRUE),
                ('ZAM-006', 5, DATE '2026-03-01', 'NOWE',         608.97, NULL),
                ('ZAM-007', 4, DATE '2026-03-03', 'DOSTARCZONE', 2727.00, TRUE),
                ('ZAM-008', 6, DATE '2026-03-15', 'WYSLANE',      295.45, TRUE),
                ('ZAM-009', 2, DATE '2026-03-28', 'NOWE',         349.90, NULL),
                ('ZAM-010', 1, DATE '2026-04-02', 'OPLACONE',     163.00, TRUE);
            """;

    /** Tworzy tabele i dane; DATE '2026-01-05' to standardowy literał daty (format RRRR-MM-DD). */
    static void createShop(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            for (String sql : SCHEMA_AND_DATA.split(";")) {
                if (!sql.isBlank()) {
                    st.execute(sql);
                }
            }
        }
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • java.sql = interfejsy; sterownik (JAR) = implementacja; DriverManager wybiera sterownik po URL (bez Class.forName).
     *   • URL: jdbc:h2:mem:nazwa;DB_CLOSE_DELAY=-1 · jdbc:postgresql://host:5432/baza · jdbc:mysql://host:3306/baza
     *   • try (Connection con = ...; Statement st = ...; ResultSet rs = ...) — zamykanie w odwrotnej kolejności.
     *     Zamknięcie Statement zamyka jego ResultSet. Nie zwracaj ResultSet z metody — przepisz wiersze do listy.
     *   • ResultSet: kursor przed 1. wierszem; while (rs.next()); kolumny od 1 albo po etykiecie (alias!).
     *   • NULL: getInt/getLong/getBoolean → 0/false; sprawdź wasNull() od razu albo użyj getObject(kol, Integer.class).
     *     Daty: getObject(kol, LocalDate.class); kwoty: getBigDecimal.
     *   • executeQuery → ResultSet; executeUpdate → int (DDL: 0); execute → boolean (+ getResultSet / getUpdateCount).
     *   • SQLException: getSQLState() (przenośny, klasa = 2 znaki: 08 połączenie, 22 dane, 23 spójność, 42 składnia/obiekt),
     *     getErrorCode() (kod dostawcy), getNextException() (łańcuch, np. w BatchUpdateException).
     *   • Nie wypisuj getMessage() użytkownikowi — zależy od bazy, wersji i języka systemu.
     *   • Produkcja: DataSource + pula (HikariCP); dane logowania z konfiguracji, nie z kodu.
     *
     * PYTANIA KONTROLNE:
     *   1. Po co sterownik JDBC, skoro Connection i Statement są w JDK?
     *   2. Co wypisze dla klienta z discount = NULL:
     *        int d = rs.getInt("discount"); System.out.println(d + " " + rs.wasNull());  ?
     *   3. ZNAJDŹ BŁĄD:
     *        static ResultSet findAll(Connection con) throws SQLException {
     *            try (Statement st = con.createStatement()) { return st.executeQuery("SELECT * FROM customers"); }
     *        }
     *   4. Co zwróci st.execute("UPDATE customers SET discount = 0 WHERE id = 999") i ile zwróci potem getUpdateCount()?
     *   5. Czym różni się SQLState od getErrorCode()? Na którym opierać logikę programu?
     *   6. ZNAJDŹ BŁĄD:  DriverManager.getConnection("jdbc:h2:mem:")  w dwóch miejscach programu — dlaczego drugie
     *      połączenie nie widzi tabel utworzonych przez pierwsze?
     *   7. Dlaczego w aplikacji serwerowej używa się puli połączeń, a nie DriverManager przy każdym zapytaniu?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: etykiety kolumn tabeli orders", List.of("ID", "CUSTOMER_ID", "ORDER_DATE", "STATUS", "TOTAL", "PAID"),
                () -> inFreshDb("jdbc02_e1", Jdbc02Connection::exercise1));
        Check.equal("ćw. 2: rabaty z rozróżnieniem 0 i NULL", EXPECTED_DISCOUNTS,
                () -> inFreshDb("jdbc02_e2", Jdbc02Connection::exercise2));
        Check.equal("ćw. 3: PRZEPISZ na try-with-resources — liczba opłaconych zamówień", 7,
                () -> inFreshDb("jdbc02_e3", Jdbc02Connection::exercise3));
        Check.equal("ćw. 4: klasyfikacja błędów po SQLState", EXPECTED_CLASSES,
                () -> inFreshDb("jdbc02_e4", con -> exercise4(con, CLASSIFY_SQL)));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of("ID", "CUSTOMER_ID", "ORDER_DATE", "STATUS", "TOTAL", "PAID"),
                () -> inFreshDb("jdbc02_s1", Jdbc02Connection::solution1));
        Check.equal("ćw. 2 (wzorzec)", EXPECTED_DISCOUNTS,
                () -> inFreshDb("jdbc02_s2", Jdbc02Connection::solution2));
        Check.equal("ćw. 3 (wzorzec)", 7,
                () -> inFreshDb("jdbc02_s3", Jdbc02Connection::solution3));
        Check.equal("ćw. 4 (wzorzec)", EXPECTED_CLASSES,
                () -> inFreshDb("jdbc02_s4", con -> solution4(con, CLASSIFY_SQL)));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    static final List<String> EXPECTED_DISCOUNTS = List.of(
            "Jan Kowalski: 10%", "Maria Nowak: brak", "Adam Mazur: 0%", "Zofia Krawczyk: 15%",
            "Ola Pawlak: brak", "Marek Król: 5%", "Ewa Lis: 0%");

    static final List<String> CLASSIFY_SQL = List.of(
            "UPDATE customers SET discount = 1 WHERE id = 1",
            "INSERT INTO customers (id, name, city) VALUES (2, 'X', 'Y')",
            "SELECT brak FROM customers",
            "INSERT INTO customers (id, city) VALUES (50, 'Y')");

    static final List<String> EXPECTED_CLASSES = List.of("OK", "spójność danych", "błąd w zapytaniu", "spójność danych");

    /** Funkcja na połączeniu, która może rzucić SQLException. SqlWork = praca z bazą. */
    @FunctionalInterface
    interface SqlWork<T> {
        T apply(Connection con) throws SQLException;
    }

    /** Nowa baza w pamięci z danymi lekcji dla każdego ćwiczenia; SQLException → wyjątek niesprawdzany z SQLState. */
    static <T> T inFreshDb(String dbName, SqlWork<T> work) {
        try (Connection con = DriverManager.getConnection("jdbc:h2:mem:" + dbName)) {
            createShop(con);
            return work.apply(con);
        } catch (SQLException e) {
            throw new IllegalStateException("błąd SQL, SQLState " + e.getSQLState(), e);
        }
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć etykiety wszystkich kolumn wyniku zapytania {@code SELECT * FROM orders}
     * w kolejności kolumn.
     * Podpowiedź: rs.getMetaData(), pętla od 1 do getColumnCount() włącznie, getColumnLabel(i).
     */
    static List<String> exercise1(Connection con) throws SQLException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): dla każdego klienta (ORDER BY id) zwróć napis „imię nazwisko: N%”, a gdy rabat
     * (discount) jest NULL — „imię nazwisko: brak”. Uwaga: rabat 0 to „0%”, nie „brak”!
     * Podpowiedź: rs.getObject("discount", Integer.class) zwraca null dla NULL; albo getInt + natychmiast wasNull().
     */
    static List<String> exercise2(Connection con) throws SQLException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ kod w starym stylu (sprzed Javy 7) na try-with-resources.
     * Metoda ma zwrócić liczbę zamówień z paid = TRUE.
     * <pre>{@code
     * Statement st = null;
     * ResultSet rs = null;
     * try {
     *     st = con.createStatement();
     *     rs = st.executeQuery("SELECT COUNT(*) FROM orders WHERE paid = TRUE");
     *     rs.next();
     *     return rs.getInt(1);
     * } finally {
     *     if (rs != null) { try { rs.close(); } catch (SQLException ignored) { } }
     *     if (st != null) { try { st.close(); } catch (SQLException ignored) { } }
     * }
     * }</pre>
     * Podpowiedź: oba zasoby w jednym nawiasie try (...), oddzielone średnikiem; finally znika.
     */
    static int exercise3(Connection con) throws SQLException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): wykonaj po kolei każde polecenie z listy (execute) i dla każdego zwróć:
     * „OK” — gdy się udało; „spójność danych” — SQLState zaczyna się od 23; „błąd w zapytaniu” — od 42;
     * „inny błąd” — w pozostałych przypadkach. Każde polecenie wykonaj w osobnym try/catch, żeby błąd jednego
     * nie przerwał pozostałych.
     * Podpowiedź: e.getSQLState().startsWith("23"); pamiętaj, że getSQLState() może zwrócić null (wtedy „inny błąd”).
     */
    static List<String> exercise4(Connection con, List<String> statements) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solution1(Connection con) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM orders")) {
            ResultSetMetaData meta = rs.getMetaData();
            List<String> labels = new ArrayList<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                labels.add(meta.getColumnLabel(i));
            }
            return labels;
        }
    }

    static List<String> solution2(Connection con) throws SQLException {
        List<String> result = new ArrayList<>();
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT name, discount FROM customers ORDER BY id")) {
            while (rs.next()) {
                Integer discount = rs.getObject("discount", Integer.class);
                result.add(rs.getString("name") + ": " + (discount == null ? "brak" : discount + "%"));
            }
        }
        return result;
    }

    static int solution3(Connection con) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM orders WHERE paid = TRUE")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    static List<String> solution4(Connection con, List<String> statements) {
        List<String> result = new ArrayList<>();
        for (String sql : statements) {
            try (Statement st = con.createStatement()) {
                st.execute(sql);
                result.add("OK");
            } catch (SQLException e) {
                String state = e.getSQLState();
                if (state != null && state.startsWith("23")) {
                    result.add("spójność danych");
                } else if (state != null && state.startsWith("42")) {
                    result.add("błąd w zapytaniu");
                } else {
                    result.add("inny błąd");
                }
            }
        }
        return result;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. JDK zawiera tylko interfejsy (umowę). Sterownik danej bazy implementuje je: zna jej protokół sieciowy,
     *      typy i dialekt. Bez sterownika DriverManager rzuca „No suitable driver” (SQLState 08001).
     *   2. „0 true” — getInt nie może zwrócić null, więc dla NULL daje 0, a wasNull() mówi, że to był NULL.
     *   3. try-with-resources zamyka Statement przy wyjściu z metody, a razem z nim ResultSet. Wywołujący dostaje
     *      zamknięty wynik i pierwsze next() rzuci SQLException. Poprawnie: przepisz wiersze do List<...> w środku metody.
     *   4. execute zwróci false (to nie zapytanie), a getUpdateCount() zwróci 0 (żaden wiersz nie ma id 999).
     *   5. SQLState to 5-znakowy kod ze standardu (te same klasy w różnych bazach, np. 23 = naruszenie spójności),
     *      getErrorCode() to numer dostawcy, inny w każdej bazie. Logikę opieraj na SQLState (najlepiej na klasie)
     *      albo na podklasach SQLException.
     *   6. „jdbc:h2:mem:” bez nazwy tworzy osobną, prywatną bazę dla każdego połączenia. Trzeba nadać nazwę
     *      (jdbc:h2:mem:sklep) i — jeśli połączenia nie nakładają się w czasie — dodać DB_CLOSE_DELAY=-1.
     *   7. Otwarcie fizycznego połączenia jest kosztowne (sieć, logowanie, sesja na serwerze), a baza ma limit
     *      połączeń. Pula otwiera je raz, pożycza wątkom i ogranicza ich liczbę; close() oddaje połączenie do puli.
     */
    // </editor-fold>
}
