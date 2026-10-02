package t29_jdbc_databases;

import helpers.Check;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Transakcje — commit, rollback, punkty zapisu, izolacja i blokowanie optymistyczne
 *        (transaction = transakcja; commit = zatwierdź; rollback = wycofaj)
 *
 * W SKRÓCIE:
 *   Transakcja to grupa poleceń, które baza wykonuje „wszystko albo nic”. Przelew to dwa UPDATE-y: obciążenie
 *   jednego konta i uznanie drugiego — nie wolno dopuścić, żeby wykonał się tylko jeden. W JDBC domyślnie każde
 *   polecenie jest osobną transakcją (auto-commit); dla kilku poleceń wyłączamy auto-commit i sami wołamy
 *   commit() albo rollback().
 *
 * ANALOGIA:
 *   Zakupy w sklepie internetowym: wkładasz rzeczy do koszyka (polecenia w transakcji), ale nic nie jest kupione,
 *   dopóki nie klikniesz „Zamawiam i płacę” (commit). Możesz też wyrzucić cały koszyk (rollback). Inni klienci
 *   nie widzą Twojego koszyka — widzą tylko złożone zamówienia (izolacja).
 *
 * JAK TO DZIAŁA:
 *   con.setAutoCommit(false);          ← początek: od teraz zmiany czekają na decyzję
 *   try { UPDATE ...; UPDATE ...; con.commit(); }       ← zatwierdź WSZYSTKO
 *   catch (SQLException e) { con.rollback(); throw e; } ← wycofaj WSZYSTKO
 *   finally { con.setAutoCommit(true); }                ← przywróć tryb połączenia
 *   ACID: Atomicity (niepodzielność), Consistency (spójność), Isolation (izolacja), Durability (trwałość).
 *   Poziomy izolacji (od najsłabszego): READ UNCOMMITTED → READ COMMITTED → REPEATABLE READ → SERIALIZABLE.
 *
 * SŁÓWKA:
 *   auto-commit = samozatwierdzanie; savepoint = punkt zapisu; isolation = izolacja; dirty read = brudny odczyt;
 *   non-repeatable read = niepowtarzalny odczyt; phantom = fantom (wiersz-widmo); lost update = utracona
 *   aktualizacja; optimistic locking = blokowanie optymistyczne; pessimistic = pesymistyczne; version = wersja;
 *   balance = saldo; account = konto; transfer = przelew
 *
 * ZOBACZ TEŻ: t29_jdbc_databases/Jdbc03PreparedStatement (parametry, paczki),
 *             t29_jdbc_databases/Jdbc05Dao (transakcja obejmująca kilka repozytoriów),
 *             t21_concurrency/Concurrency02RaceConditions (ten sam problem wyścigu, ale w pamięci Javy)
 * </pre>
 */
public class Jdbc04Transactions {

    /** DB_CLOSE_DELAY=-1: baza żyje do końca programu, więc drugie połączenie widzi te same tabele. */
    private static final String URL = "jdbc:h2:mem:jdbc04;DB_CLOSE_DELAY=-1";

    public static void main(String[] args) throws SQLException {
        title("Jdbc04 — transakcje: commit, rollback, izolacja, wersjonowanie");

        try (Connection con = DriverManager.getConnection(URL)) {
            createAccounts(con, ACCOUNTS);   // create accounts = utwórz konta
            autoCommit(con);                 // auto-commit = samozatwierdzanie
            commitAndRollback(con);          // commit and rollback = zatwierdź i wycofaj
            moneyTransfer(con);              // money transfer = przelew pieniędzy
            savepoints(con);                 // savepoints = punkty zapisu
            acid(con);                       // ACID = cztery cechy transakcji
            isolationAnomalies(con);         // isolation anomalies = anomalie izolacji
            lostUpdateAndVersion(con);       // lost update and version = utracona aktualizacja i wersja
            pessimisticAndSpring();          // pessimistic and Spring = blokada pesymistyczna i Spring
        }
        exercises();                         // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. AUTO-COMMIT — KAŻDE POLECENIE TO OSOBNA TRANSAKCJA
    // =================================================================================================

    /**
     * 1. Nowe połączenie JDBC ma włączony auto-commit: każde polecenie jest zatwierdzane od razu po wykonaniu.
     * Dla pojedynczych zmian to wygodne, ale dla operacji z kilku kroków — niebezpieczne.
     */
    static void autoCommit(Connection con) throws SQLException {
        section("1. Auto-commit — każde polecenie to osobna transakcja");

        show("getAutoCommit() nowego połączenia", con.getAutoCommit());   // getAutoCommit = czy samozatwierdzanie
        // WYNIK: getAutoCommit() nowego połączenia → true

        // PRZED: przelew 300 zł z konta 3 (saldo 200) na konto 1 w trybie auto-commit.
        // Najpierw uznanie (UPDATE konta 1) — zatwierdzone natychmiast; potem obciążenie łamie CHECK (balance >= 0).
        show("suma sald przed", totalBalance(con));   // totalBalance = suma sald (pomocnik na dole pliku)
        // WYNIK: suma sald przed → 1700.00
        try {
            changeBalance(con, 1, new BigDecimal("300"));     // change balance = zmień saldo
            changeBalance(con, 3, new BigDecimal("-300"));
        } catch (SQLException e) {
            show("błąd obciążenia, SQLState", e.getSQLState() + " (" + describe(e.getSQLState()) + ")");
        }
        // WYNIK: błąd obciążenia, SQLState → 23513 (naruszenie CHECK)
        show("suma sald po", totalBalance(con));
        // WYNIK: suma sald po → 2000.00
        // PUŁAPKA: pieniądze „powstały z niczego” — pierwszy UPDATE już był zatwierdzony, drugi się nie udał.
        // W auto-commit nie ma czego wycofać. Naprawiamy ręcznie i przechodzimy do transakcji.
        changeBalance(con, 1, new BigDecimal("-300"));
        show("suma sald po ręcznej naprawie", totalBalance(con));
        // WYNIK: suma sald po ręcznej naprawie → 1700.00
    }

    // =================================================================================================
    // 2. setAutoCommit(false), commit() I rollback()
    // =================================================================================================

    /**
     * 2. Po {@code setAutoCommit(false)} zmiany czekają: commit() zapisuje je na stałe, rollback() cofa
     * wszystkie od ostatniego commit. Drugie połączenie (inny „użytkownik”) widzi tylko zatwierdzone dane.
     */
    static void commitAndRollback(Connection con) throws SQLException {
        section("2. setAutoCommit(false), commit() i rollback()");

        try (Connection other = DriverManager.getConnection(URL)) {   // other = inne połączenie (drugi użytkownik)
            con.setAutoCommit(false);                // setAutoCommit(false) = wyłącz samozatwierdzanie
            changeBalance(con, 2, new BigDecimal("-100"));
            show("saldo konta 2 we własnej transakcji", balanceOf(con, 2));   // balanceOf = saldo konta
            show("saldo konta 2 widziane z innego połączenia", balanceOf(other, 2));
            // WYNIK: saldo konta 2 we własnej transakcji → 400.00
            // WYNIK: saldo konta 2 widziane z innego połączenia → 500.00

            con.rollback();                          // rollback = wycofaj zmiany tej transakcji
            show("po rollback() — własne połączenie", balanceOf(con, 2));
            // WYNIK: po rollback() — własne połączenie → 500.00

            changeBalance(con, 2, new BigDecimal("-100"));   // tym razem wypłata 100 zł naprawdę się odbywa
            con.commit();                            // commit = zatwierdź zmiany
            show("po commit() — inne połączenie", balanceOf(other, 2));
            // WYNIK: po commit() — inne połączenie → 400.00
            con.setAutoCommit(true);                 // przywracamy domyślny tryb
        }
        // PUŁAPKA: zamknięcie połączenia z niezatwierdzoną transakcją — standard JDBC nie mówi, co wtedy: jedne
        // sterowniki robią rollback (większość), inne commit. Nigdy na tym nie polegaj: zawsze jawnie commit/rollback.
        // Uwaga: setAutoCommit(true) w trakcie transakcji ZATWIERDZA ją (tak mówi specyfikacja JDBC).
    }

    // =================================================================================================
    // 3. PRZELEW — WZORZEC try / catch / finally
    // =================================================================================================

    /**
     * 3. Wzorzec transakcji w czystym JDBC: zapamiętaj tryb, wyłącz auto-commit, wykonaj kroki, commit;
     * przy wyjątku rollback i przekaż wyjątek dalej; w finally przywróć poprzedni tryb.
     */
    static void moneyTransfer(Connection con) throws SQLException {
        section("3. Przelew — wzorzec try / catch / finally");

        transfer(con, 1, 2, new BigDecimal("250.00"));    // transfer = przelew (metoda poniżej)
        show("po udanym przelewie 250 z 1 na 2", balances(con));   // balances = salda wszystkich kont
        // WYNIK: po udanym przelewie 250 z 1 na 2 → 1=750.00, 2=650.00, 3=200.00

        // PO: ten sam nieudany przelew co w sekcji 1 — teraz w transakcji
        try {
            transfer(con, 3, 1, new BigDecimal("300.00"));
        } catch (SQLException e) {
            show("przelew odrzucony, SQLState", e.getSQLState() + " (" + describe(e.getSQLState()) + ")");
        }
        // WYNIK: przelew odrzucony, SQLState → 23513 (naruszenie CHECK)
        show("salda bez zmian", balances(con));
        show("suma sald", totalBalance(con));
        show("auto-commit przywrócony", con.getAutoCommit());
        // WYNIK: salda bez zmian → 1=750.00, 2=650.00, 3=200.00
        // WYNIK: suma sald → 1600.00
        // WYNIK: auto-commit przywrócony → true

        // DOBRA PRAKTYKA: o tym, czy przelew jest możliwy, decyduje baza (CHECK), a nie wcześniejszy SELECT w Javie.
        // Dlaczego? Między „sprawdź saldo” a „zmień saldo” ktoś inny mógł wypłacić pieniądze; ograniczenie w bazie
        // sprawdza stan w chwili zmiany.
        // DOBRA PRAKTYKA: w catch rób rollback i RZUĆ wyjątek dalej (albo zamień na własny — Jdbc05Dao).
        // Połknięty wyjątek = wywołujący myśli, że przelew się udał.
    }

    /** Przelew w jednej transakcji: obciąż {@code from}, uznaj {@code to}; błąd → rollback i wyjątek dalej. */
    static void transfer(Connection con, int from, int to, BigDecimal amount) throws SQLException {
        boolean previous = con.getAutoCommit();
        con.setAutoCommit(false);
        try {
            changeBalance(con, to, amount);               // kolejność kroków nie ma znaczenia — i tak wszystko albo nic
            changeBalance(con, from, amount.negate());    // negate = zmień znak
            con.commit();
        } catch (SQLException | RuntimeException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(previous);
        }
    }

    // =================================================================================================
    // 4. PUNKTY ZAPISU — SAVEPOINT
    // =================================================================================================

    /**
     * 4. Punkt zapisu (savepoint) pozwala wycofać tylko część transakcji: {@code rollback(savepoint)} cofa zmiany
     * zrobione PO tym punkcie, a wcześniejsze zostają i czekają na commit.
     */
    static void savepoints(Connection con) throws SQLException {
        section("4. Punkty zapisu — savepoint");

        con.setAutoCommit(false);
        try {
            changeBalance(con, 1, new BigDecimal("10"));            // krok A — chcemy go zachować
            Savepoint afterA = con.setSavepoint("po_kroku_A");      // setSavepoint = ustaw punkt zapisu
            changeBalance(con, 2, new BigDecimal("20"));            // krok B — za chwilę go wycofamy
            show("w transakcji, przed częściowym wycofaniem", balances(con));
            // WYNIK: w transakcji, przed częściowym wycofaniem → 1=760.00, 2=670.00, 3=200.00
            con.rollback(afterA);                                   // rollback(punkt) = wycofaj do punktu
            con.commit();
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
        show("po commit (krok A został, B wycofany)", balances(con));
        // WYNIK: po commit (krok A został, B wycofany) → 1=760.00, 2=650.00, 3=200.00
        // Zastosowanie: import wielu pozycji, gdzie błędną pozycję pomijamy, a resztę zapisujemy (ćwiczenie 3).
        // releaseSavepoint(punkt) = zwolnij punkt zapisu, gdy nie będzie już potrzebny.
    }

    // =================================================================================================
    // 5. ACID — CZTERY GWARANCJE TRANSAKCJI
    // =================================================================================================

    /**
     * 5. ACID to skrót czterech gwarancji. Atomowość i izolację już widzieliśmy; spójność pilnują ograniczenia
     * (CHECK, UNIQUE, klucze obce); trwałość oznacza, że zatwierdzone dane przetrwają awarię.
     */
    static void acid(Connection con) throws SQLException {
        section("5. ACID — cztery gwarancje transakcji");

        note("A — atomowość: przelew wykonał się w całości albo wcale (sekcja 3)");
        note("C — spójność: CHECK (balance >= 0) nie dopuścił ujemnego salda");
        note("I — izolacja: inne połączenie nie widziało niezatwierdzonej zmiany (sekcja 2)");
        note("D — trwałość: po commit dane są zapisane i przetrwają awarię serwera");
        // WYNIK: ℹ A — atomowość: przelew wykonał się w całości albo wcale (sekcja 3)
        // WYNIK: ℹ C — spójność: CHECK (balance >= 0) nie dopuścił ujemnego salda
        // WYNIK: ℹ I — izolacja: inne połączenie nie widziało niezatwierdzonej zmiany (sekcja 2)
        // WYNIK: ℹ D — trwałość: po commit dane są zapisane i przetrwają awarię serwera
        // PUŁAPKA: baza w pamięci (jdbc:h2:mem:) z definicji NIE jest trwała — znika razem z programem.
        // Do nauki i testów to zaleta, w prawdziwej aplikacji używa się bazy z zapisem na dysk (PostgreSQL, MySQL…).
        show("suma sald = 1700 − 100 (wypłata, sekcja 2) + 10 (sekcja 4)", totalBalance(con));
        // WYNIK: suma sald = 1700 − 100 (wypłata, sekcja 2) + 10 (sekcja 4) → 1610.00
    }

    // =================================================================================================
    // 6. POZIOMY IZOLACJI I ANOMALIE — KROK PO KROKU NA DWÓCH POŁĄCZENIACH
    // =================================================================================================

    /**
     * 6. Gdy dwie transakcje działają jednocześnie, mogą wystąpić anomalie: brudny odczyt (widzę niezatwierdzone),
     * niepowtarzalny odczyt (ten sam wiersz czytany dwa razy daje różne wartości), fantom (to samo zapytanie
     * zwraca nowe wiersze). Pokazujemy je bez wątków: dwa połączenia i ręcznie ustalona kolejność kroków.
     * <pre>
     *   poziom             brudny odczyt   niepowtarzalny odczyt   fantom
     *   READ UNCOMMITTED   możliwy         możliwy                 możliwy
     *   READ COMMITTED     NIE             możliwy                 możliwy
     *   REPEATABLE READ    NIE             NIE                     możliwy wg standardu (H2/PostgreSQL: NIE)
     *   SERIALIZABLE       NIE             NIE                     NIE
     * </pre>
     */
    static void isolationAnomalies(Connection con) throws SQLException {
        section("6. Poziomy izolacji i anomalie — krok po kroku");

        // getTransactionIsolation = pobierz poziom izolacji
        show("domyślny poziom w H2", isolationName(con.getTransactionIsolation()));   // isolationName = nazwa poziomu
        // WYNIK: domyślny poziom w H2 → READ COMMITTED
        // Domyślne poziomy: H2, PostgreSQL, Oracle, SQL Server — READ COMMITTED; MySQL (InnoDB) — REPEATABLE READ.

        try (Connection writer = DriverManager.getConnection(URL);     // writer = piszący
             Connection reader = DriverManager.getConnection(URL)) {   // reader = czytający
            writer.setAutoCommit(false);
            reader.setAutoCommit(false);

            // a) BRUDNY ODCZYT na READ UNCOMMITTED
            reader.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            changeBalance(writer, 2, new BigDecimal("1000"));          // krok 1: writer zmienia, BEZ commit
            show("a) reader (READ UNCOMMITTED) widzi saldo 2", balanceOf(reader, 2));
            writer.rollback();                                          // krok 2: writer się wycofuje
            show("a) po rollback writera reader widzi", balanceOf(reader, 2));
            reader.commit();
            // WYNIK: a) reader (READ UNCOMMITTED) widzi saldo 2 → 1650.00
            // WYNIK: a) po rollback writera reader widzi → 650.00
            // Reader podjął decyzję na podstawie kwoty, która NIGDY nie istniała. Dlatego READ UNCOMMITTED
            // prawie nigdy się nie stosuje (PostgreSQL w ogóle go nie ma — traktuje jak READ COMMITTED).

            // b) NIEPOWTARZALNY ODCZYT na READ COMMITTED
            reader.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            show("b) reader (READ COMMITTED), odczyt 1", balanceOf(reader, 3));
            changeBalance(writer, 3, new BigDecimal("50"));
            writer.commit();                                            // writer zatwierdza w trakcie transakcji readera
            show("b) ta sama transakcja readera, odczyt 2", balanceOf(reader, 3));
            reader.commit();
            // WYNIK: b) reader (READ COMMITTED), odczyt 1 → 200.00
            // WYNIK: b) ta sama transakcja readera, odczyt 2 → 250.00

            // c) FANTOM na READ COMMITTED: to samo zapytanie zwraca nowy wiersz
            show("c) reader (READ COMMITTED): kont z saldem > 100", countRich(reader));   // countRich = policz bogatych
            insertAccount(writer, 4, "Dorota", new BigDecimal("700.00"));
            writer.commit();
            show("c) to samo zapytanie w tej samej transakcji", countRich(reader));
            reader.commit();
            // WYNIK: c) reader (READ COMMITTED): kont z saldem > 100 → 3
            // WYNIK: c) to samo zapytanie w tej samej transakcji → 4

            // d) REPEATABLE READ: transakcja readera widzi stały obraz danych z chwili pierwszego odczytu
            reader.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            show("d) reader (REPEATABLE READ): saldo 3", balanceOf(reader, 3));
            show("d) kont z saldem > 100", countRich(reader));
            changeBalance(writer, 3, new BigDecimal("50"));
            insertAccount(writer, 5, "Edward", new BigDecimal("900.00"));
            writer.commit();
            show("d) po commit writera — saldo 3", balanceOf(reader, 3));
            show("d) po commit writera — kont z saldem > 100", countRich(reader));
            reader.commit();                                            // koniec transakcji → nowy obraz danych
            show("d) nowa transakcja readera — kont z saldem > 100", countRich(reader));
            reader.commit();
            // WYNIK: d) reader (REPEATABLE READ): saldo 3 → 250.00
            // WYNIK: d) kont z saldem > 100 → 4
            // WYNIK: d) po commit writera — saldo 3 → 250.00
            // WYNIK: d) po commit writera — kont z saldem > 100 → 4
            // WYNIK: d) nowa transakcja readera — kont z saldem > 100 → 5
        }
        // Sprzątanie: usuwamy konta 4 i 5, cofamy dopłaty do konta 3
        try (Statement st = con.createStatement()) {
            st.executeUpdate("DELETE FROM accounts WHERE id IN (4, 5)");
        }
        changeBalance(con, 3, new BigDecimal("-100"));

        // PUŁAPKA: wyższy poziom izolacji to nie „darmowe bezpieczeństwo”. Na REPEATABLE READ i SERIALIZABLE bazy
        // częściej zgłaszają konflikty (transakcję trzeba powtórzyć) albo dłużej czekają na blokady.
        // DOBRA PRAKTYKA: zostań przy domyślnym READ COMMITTED, krótkich transakcjach i ograniczeniach w bazie,
        // a tam, gdzie liczy się „przeczytaj–zmień–zapisz”, użyj wersjonowania (sekcja 7) albo SELECT ... FOR UPDATE.
    }

    // =================================================================================================
    // 7. UTRACONA AKTUALIZACJA I BLOKOWANIE OPTYMISTYCZNE (KOLUMNA version)
    // =================================================================================================

    /**
     * 7. Utracona aktualizacja: dwóch użytkowników czyta to samo saldo, każdy liczy nową wartość w Javie i zapisuje.
     * Drugi zapis nadpisuje pierwszy. Blokowanie optymistyczne: każdy wiersz ma numer wersji, a UPDATE zmienia
     * wiersz tylko wtedy, gdy wersja się nie zmieniła od odczytu — inaczej zmienia 0 wierszy.
     */
    static void lostUpdateAndVersion(Connection con) throws SQLException {
        section("7. Utracona aktualizacja i blokowanie optymistyczne");

        // PRZED: obaj czytają saldo konta 1, obaj dopłacają, zapisują „saldo z odczytu + dopłata”
        BigDecimal readByAnna = balanceOf(con, 1);
        BigDecimal readByBartek = balanceOf(con, 1);
        setBalance(con, 1, readByAnna.add(new BigDecimal("100")));      // setBalance = ustaw saldo; Anna: +100
        setBalance(con, 1, readByBartek.add(new BigDecimal("40")));     // Bartek: +40 — nadpisuje Annę
        show("odczyt obu", readByAnna);
        show("saldo po dwóch dopłatach (+100 i +40)", balanceOf(con, 1));
        // WYNIK: odczyt obu → 760.00
        // WYNIK: saldo po dwóch dopłatach (+100 i +40) → 800.00
        // PUŁAPKA: oczekiwaliśmy +140, a jest tylko +40. Dopłata Anny przepadła bez żadnego błędu.
        // (Tu najprościej byłoby: UPDATE ... SET balance = balance + ? — baza liczy sama, atomowo.
        // Wersjonowanie przydaje się, gdy nową wartość ustala człowiek albo logika w Javie, np. edycja formularza.)
        setBalance(con, 1, readByAnna);   // przywracamy stan

        // PO: odczyt saldo + wersja, zapis z warunkiem WHERE version = ?
        long[] anna = readWithVersion(con, 1);     // [saldo w groszach, wersja]; updateIfVersion = zmień, jeśli wersja się zgadza
        long[] bartek = readWithVersion(con, 1);
        show("wersja odczytana przez oboje", anna[1] + " i " + bartek[1]);
        // WYNIK: wersja odczytana przez oboje → 0 i 0
        int annaRows = updateIfVersion(con, 1, BigDecimal.valueOf(anna[0] + 10_000, 2), anna[1]);
        int bartekRows = updateIfVersion(con, 1, BigDecimal.valueOf(bartek[0] + 4_000, 2), bartek[1]);
        show("Anna: zmieniono wierszy", annaRows);
        show("Bartek (nieaktualna wersja): zmieniono wierszy", bartekRows);
        // WYNIK: Anna: zmieniono wierszy → 1
        // WYNIK: Bartek (nieaktualna wersja): zmieniono wierszy → 0
        // 0 wierszy = ktoś zmienił dane w międzyczasie. Bartek wczytuje świeże dane i próbuje ponownie:
        bartek = readWithVersion(con, 1);
        bartekRows = updateIfVersion(con, 1, BigDecimal.valueOf(bartek[0] + 4_000, 2), bartek[1]);
        show("Bartek, druga próba: zmieniono wierszy", bartekRows);
        show("saldo i wersja na koniec", balanceOf(con, 1) + ", wersja " + readWithVersion(con, 1)[1]);
        // WYNIK: Bartek, druga próba: zmieniono wierszy → 1
        // WYNIK: saldo i wersja na koniec → 900.00, wersja 2
        // DOBRA PRAKTYKA: zawsze sprawdzaj liczbę zwróconą przez executeUpdate. Przy wersjonowaniu 0 oznacza konflikt:
        // pokaż użytkownikowi „dane zmienił ktoś inny” albo powtórz operację na świeżych danych.
        // JPA/Hibernate robi dokładnie to samo dla pola z adnotacją @Version (SpringLearning).
    }

    // =================================================================================================
    // 8. BLOKADA PESYMISTYCZNA (SELECT ... FOR UPDATE) I TRANSAKCJE W SPRINGU
    // =================================================================================================

    /**
     * 8. Blokowanie pesymistyczne zakłada konflikt z góry: {@code SELECT ... FOR UPDATE} blokuje odczytane wiersze
     * do końca transakcji — inne transakcje, które chcą je zmienić (albo też zablokować), CZEKAJĄ.
     */
    static void pessimisticAndSpring() {
        section("8. Blokada pesymistyczna i transakcje w Springu");

        note("SELECT ... FOR UPDATE: inni czekają, aż zrobisz commit albo rollback");
        note("optymistycznie: nikt nie czeka, konflikt wykrywa UPDATE ... WHERE version = ?");
        // WYNIK: ℹ SELECT ... FOR UPDATE: inni czekają, aż zrobisz commit albo rollback
        // WYNIK: ℹ optymistycznie: nikt nie czeka, konflikt wykrywa UPDATE ... WHERE version = ?
        // Schemat (bez uruchamiania — druga transakcja w tym samym wątku czekałaby na blokadę ~2 s i dostała błąd
        // przekroczenia czasu oczekiwania, w H2 SQLState HYT00):
        //   con.setAutoCommit(false);
        //   SELECT balance FROM accounts WHERE id = ? FOR UPDATE   ← wiersz zablokowany dla innych piszących
        //   ... obliczenia w Javie ...
        //   UPDATE accounts SET balance = ? WHERE id = ?
        //   con.commit();                                          ← blokada zwolniona
        // Kiedy co? Pesymistycznie — gdy konflikty są częste i krótkie (np. stan magazynu ostatniej sztuki);
        // optymistycznie — gdy konflikty są rzadkie, a „transakcja” użytkownika trwa długo (formularz otwarty 5 minut —
        // nie można trzymać blokady w bazie przez ten czas).
        // PUŁAPKA: zakleszczenie (deadlock): transakcja A blokuje konto 1 i czeka na 2, B blokuje 2 i czeka na 1.
        // Baza przerywa jedną z nich (PostgreSQL: SQLState 40P01, MySQL: 40001). Sposób: blokuj wiersze zawsze
        // w tej samej kolejności (np. rosnąco po id) — patrz też t21_concurrency/Concurrency03Locks.

        // W Springu nie piszesz setAutoCommit/commit/rollback ręcznie: metoda z adnotacją @Transactional robi
        // dokładnie wzorzec z sekcji 3 (commit po normalnym zakończeniu, rollback po wyjątku niesprawdzanym),
        // a połączenie jest wspólne dla wszystkich repozytoriów wołanych w tej metodzie (SpringLearning).
        // PUŁAPKA: w Springu domyślnie rollback następuje tylko dla RuntimeException i Error — wyjątek sprawdzany
        // (checked) kończy się COMMITEM, chyba że ustawisz rollbackFor.
    }

    // =================================================================================================
    // NARZĘDZIA LEKCJI
    // =================================================================================================

    /** Konta: saldo nie może być ujemne (CHECK); version służy do blokowania optymistycznego. */
    static final String ACCOUNTS = """
            CREATE TABLE accounts (
                id      INT           PRIMARY KEY,
                owner   VARCHAR(50)   NOT NULL,
                balance DECIMAL(10,2) NOT NULL CHECK (balance >= 0),
                version INT           DEFAULT 0 NOT NULL
            );
            INSERT INTO accounts (id, owner, balance) VALUES
                (1, 'Anna',   1000.00),
                (2, 'Bartek',  500.00),
                (3, 'Celina',  200.00);
            """;

    /** Wykonuje skrypt: polecenia oddzielone średnikami (uproszczenie: ';' nie występuje w tekstach). */
    static void createAccounts(Connection con, String script) throws SQLException {
        try (Statement st = con.createStatement()) {
            for (String sql : script.split(";")) {
                if (!sql.isBlank()) {            // isBlank = czy pusty lub same spacje (Java 11+)
                    st.execute(sql);
                }
            }
        }
    }

    /** UPDATE ... SET balance = balance + ? — baza dodaje kwotę sama (atomowo). */
    static void changeBalance(Connection con, int id, BigDecimal delta) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET balance = balance + ? WHERE id = ?")) {
            ps.setBigDecimal(1, delta);
            ps.setInt(2, id);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("brak konta " + id, "02000");   // 02000 = brak danych (standard SQL)
            }
        }
    }

    /** Ustawia saldo na podaną wartość (bez sprawdzania wersji — podatne na utraconą aktualizację). */
    static void setBalance(Connection con, int id, BigDecimal balance) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET balance = ? WHERE id = ?")) {
            ps.setBigDecimal(1, balance);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /** Odczyt: saldo w groszach i wersja — tablica dwóch liczb dla zwięzłości (read with version = odczytaj z wersją). */
    static long[] readWithVersion(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT balance, version FROM accounts WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                // movePointRight(2) = przesuń przecinek o 2 w prawo (złote → grosze); longValueExact = dokładnie na long
                return new long[] {rs.getBigDecimal(1).movePointRight(2).longValueExact(), rs.getInt(2)};
            }
        }
    }

    /** Zapis z blokowaniem optymistycznym: zmienia wiersz tylko przy zgodnej wersji; zwraca liczbę wierszy (0 = konflikt). */
    static int updateIfVersion(Connection con, int id, BigDecimal newBalance, long expectedVersion) throws SQLException {
        String sql = "UPDATE accounts SET balance = ?, version = version + 1 WHERE id = ? AND version = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBalance);
            ps.setInt(2, id);
            ps.setLong(3, expectedVersion);
            return ps.executeUpdate();
        }
    }

    static void insertAccount(Connection con, int id, String owner, BigDecimal balance) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO accounts (id, owner, balance) VALUES (?, ?, ?)")) {
            ps.setInt(1, id);
            ps.setString(2, owner);
            ps.setBigDecimal(3, balance);
            ps.executeUpdate();
        }
    }

    static BigDecimal balanceOf(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT balance FROM accounts WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }

    /** Liczba kont z saldem większym niż 100 zł (do pokazania fantomów). */
    static int countRich(Connection con) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM accounts WHERE balance > 100")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    static BigDecimal totalBalance(Connection con) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT SUM(balance) FROM accounts")) {
            rs.next();
            return rs.getBigDecimal(1);
        }
    }

    /** Salda wszystkich kont jako tekst „id=saldo”, posortowane po id. */
    static String balances(Connection con) throws SQLException {
        List<String> parts = new ArrayList<>();
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, balance FROM accounts ORDER BY id")) {
            while (rs.next()) {
                parts.add(rs.getInt(1) + "=" + rs.getBigDecimal(2));
            }
        }
        return String.join(", ", parts);
    }

    static String isolationName(int level) {
        return switch (level) {
            case Connection.TRANSACTION_READ_UNCOMMITTED -> "READ UNCOMMITTED";
            case Connection.TRANSACTION_READ_COMMITTED -> "READ COMMITTED";
            case Connection.TRANSACTION_REPEATABLE_READ -> "REPEATABLE READ";
            case Connection.TRANSACTION_SERIALIZABLE -> "SERIALIZABLE";
            default -> "inny (" + level + ")";
        };
    }

    /** Polskie opisy kodów SQLState użytych w lekcji. */
    static String describe(String sqlState) {
        return switch (sqlState) {
            case "23513" -> "naruszenie CHECK";
            case "23505" -> "duplikat klucza / UNIQUE";
            case "02000" -> "brak danych";
            default -> "inny błąd";
        };
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Domyślnie auto-commit = true: każde polecenie zatwierdza się samo. Kilka kroków → setAutoCommit(false).
     *   • Wzorzec: previous = getAutoCommit(); setAutoCommit(false); try { ...; commit(); }
     *     catch (SQLException | RuntimeException e) { rollback(); throw e; } finally { setAutoCommit(previous); }
     *   • Savepoint sp = con.setSavepoint("nazwa"); ... con.rollback(sp) cofa tylko zmiany po punkcie.
     *   • ACID: atomowość, spójność (ograniczenia), izolacja, trwałość (baza mem: nie jest trwała!).
     *   • Anomalie: brudny odczyt (READ UNCOMMITTED), niepowtarzalny odczyt i fantom (READ COMMITTED — domyślny
     *     w H2/PostgreSQL), REPEATABLE READ w H2/PostgreSQL daje stały obraz danych do końca transakcji.
     *   • Utracona aktualizacja: „przeczytaj w Javie → policz → zapisz”. Lekarstwa: SET x = x + ? (baza liczy),
     *     wersja: UPDATE ... SET ..., version = version + 1 WHERE id = ? AND version = ?  → 0 wierszy = konflikt,
     *     albo SELECT ... FOR UPDATE (blokada do końca transakcji; uwaga na zakleszczenia).
     *   • Decyzję „czy wolno” podejmuje baza (CHECK) w chwili zmiany, nie wcześniejszy SELECT.
     *   • Spring: @Transactional = ten sam wzorzec; rollback domyślnie tylko dla RuntimeException/Error.
     *
     * PYTANIA KONTROLNE:
     *   1. Co się stanie z danymi, jeśli w trybie auto-commit pierwszy UPDATE przelewu się uda, a drugi rzuci wyjątek?
     *   2. Co wypisze (saldo konta 7 = 100.00, CHECK balance >= 0, auto-commit wyłączony):
     *        changeBalance(con, 7, new BigDecimal("50"));
     *        Savepoint sp = con.setSavepoint();
     *        changeBalance(con, 7, new BigDecimal("25"));
     *        con.rollback(sp); con.commit();
     *        System.out.println(balanceOf(con, 7));
     *   3. ZNAJDŹ BŁĄD:
     *        con.setAutoCommit(false);
     *        try { debit(con); credit(con); con.commit(); }
     *        catch (SQLException e) { System.out.println("nie udało się"); }
     *   4. Czym różni się niepowtarzalny odczyt od fantomu?
     *   5. Co zwróci executeUpdate dla  UPDATE accounts SET balance = ?, version = version + 1 WHERE id = 1 AND version = 3,
     *      jeśli w bazie konto 1 ma już wersję 4? Co powinien zrobić program?
     *   6. Dlaczego READ UNCOMMITTED jest niebezpieczny? Podaj przykład z lekcji.
     *   7. Kiedy wybrać blokowanie optymistyczne, a kiedy SELECT ... FOR UPDATE?
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

    /** NOWA baza z trzema kontami dla każdego ćwiczenia (bez DB_CLOSE_DELAY — znika po zamknięciu połączenia). */
    static <T> T inFreshDb(String dbName, SqlWork<T> work) {
        try (Connection con = DriverManager.getConnection("jdbc:h2:mem:" + dbName)) {
            createAccounts(con, ACCOUNTS);
            return work.apply(con);
        } catch (SQLException e) {
            throw new IllegalStateException("błąd SQL, SQLState " + e.getSQLState(), e);
        }
    }

    /** Operacja do importu w ćwiczeniu 3: zmiana salda konta o kwotę (BalanceChange = zmiana salda). */
    record BalanceChange(int accountId, BigDecimal delta) {
    }

    static final List<BalanceChange> IMPORT = List.of(
            new BalanceChange(1, new BigDecimal("-100")),
            new BalanceChange(3, new BigDecimal("-500")),    // złamie CHECK (Celina ma 200)
            new BalanceChange(2, new BigDecimal("50")),
            new BalanceChange(9, new BigDecimal("10")),      // nie ma konta 9 → changeBalance rzuca SQLException
            new BalanceChange(3, new BigDecimal("-200")));

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: opłata 250 zł od wszystkich → nic się nie zmienia", "1=1000.00, 2=500.00, 3=200.00",
                () -> inFreshDb("jdbc04_e1", c -> exercise1(c, new BigDecimal("250"))));
        Check.equal("ćw. 1: opłata 150 zł od wszystkich", "1=850.00, 2=350.00, 3=50.00",
                () -> inFreshDb("jdbc04_e1b", c -> exercise1(c, new BigDecimal("150"))));
        Check.equal("ćw. 2: dwa przelewy (udany i nieudany)", List.of(true, false, "1=700.00, 2=500.00, 3=500.00"),
                () -> inFreshDb("jdbc04_e2", Jdbc04Transactions::exercise2));
        Check.equal("ćw. 3: import z pominięciem błędnych pozycji", List.of(3, "1=900.00, 2=550.00, 3=0.00"),
                () -> inFreshDb("jdbc04_e3", c -> exercise3(c, IMPORT)));
        Check.equal("ćw. 4: wypłaty z wersją (aktualna, nieaktualna, świeża)", List.of(true, false, true),
                () -> inFreshDb("jdbc04_e4", c -> List.of(
                        exercise4(c, 2, new BigDecimal("100"), 0),
                        exercise4(c, 2, new BigDecimal("100"), 0),
                        exercise4(c, 2, new BigDecimal("100"), 1))));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "1=1000.00, 2=500.00, 3=200.00",
                () -> inFreshDb("jdbc04_s1", c -> solution1(c, new BigDecimal("250"))));
        Check.equal("ćw. 1 (wzorzec, 150 zł)", "1=850.00, 2=350.00, 3=50.00",
                () -> inFreshDb("jdbc04_s1b", c -> solution1(c, new BigDecimal("150"))));
        Check.equal("ćw. 2 (wzorzec)", List.of(true, false, "1=700.00, 2=500.00, 3=500.00"),
                () -> inFreshDb("jdbc04_s2", Jdbc04Transactions::solution2));
        Check.equal("ćw. 3 (wzorzec)", List.of(3, "1=900.00, 2=550.00, 3=0.00"),
                () -> inFreshDb("jdbc04_s3", c -> solution3(c, IMPORT)));
        Check.equal("ćw. 4 (wzorzec)", List.of(true, false, true),
                () -> inFreshDb("jdbc04_s4", c -> List.of(
                        solution4(c, 2, new BigDecimal("100"), 0),
                        solution4(c, 2, new BigDecimal("100"), 0),
                        solution4(c, 2, new BigDecimal("100"), 1))));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): pobierz opłatę {@code fee} od KAŻDEGO konta (1, 2, 3) w jednej transakcji. Jeśli którekolwiek
     * konto nie ma tyle pieniędzy (CHECK), nie pobieraj od nikogo. Zwróć {@code balances(con)} po operacji
     * i NIE rzucaj wyjątku na zewnątrz.
     * Podpowiedź: setAutoCommit(false); trzy razy changeBalance(con, id, fee.negate()); commit; w catch rollback.
     */
    static String exercise1(Connection con, BigDecimal fee) throws SQLException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): wykonaj dwa przelewy metodą {@code tryTransfer}, którą napiszesz sam(a): zwraca true po
     * udanym przelewie i false po odrzuconym (bez wyjątku). Przelew 1: 300 zł z konta 1 na 3. Przelew 2: 600 zł
     * z konta 2 na 1 (Bartek ma 500 — odrzucony). Zwróć listę: [wynik 1, wynik 2, balances(con)].
     * Podpowiedź: wewnątrz tryTransfer wywołaj transfer(...) z lekcji w try/catch (SQLException e) → return false.
     */
    static List<Object> exercise2(Connection con) throws SQLException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): import zmian sald w JEDNEJ transakcji z punktami zapisu. Przed każdą pozycją ustaw
     * savepoint; gdy pozycja się nie uda (SQLException), wycofaj tylko ją ({@code rollback(sp)}) i idź dalej.
     * Na końcu commit. Zwróć listę: [liczba udanych pozycji, balances(con)].
     * Podpowiedź: pętla po changes, w środku try { changeBalance(...); ok++; } catch (SQLException e) { con.rollback(sp); }.
     */
    static List<Object> exercise3(Connection con, List<BalanceChange> changes) throws SQLException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): wypłata z blokowaniem optymistycznym. Użytkownik wczytał konto, gdy miało wersję
     * {@code readVersion}. Zmniejsz saldo o {@code amount} i zwiększ wersję o 1 — ale tylko wtedy, gdy wersja w bazie
     * wciąż równa się {@code readVersion}. Zwróć true, jeśli wiersz został zmieniony, false przy konflikcie.
     * Jednym poleceniem UPDATE (bez wcześniejszego SELECT).
     * Podpowiedź: UPDATE accounts SET balance = balance - ?, version = version + 1 WHERE id = ? AND version = ?
     */
    static boolean exercise4(Connection con, int id, BigDecimal amount, int readVersion) throws SQLException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(Connection con, BigDecimal fee) throws SQLException {
        con.setAutoCommit(false);
        try {
            for (int id = 1; id <= 3; id++) {
                changeBalance(con, id, fee.negate());
            }
            con.commit();
        } catch (SQLException e) {
            con.rollback();       // świadomie nie rzucamy dalej — tak mówi treść zadania
        } finally {
            con.setAutoCommit(true);
        }
        return balances(con);
    }

    static boolean tryTransfer(Connection con, int from, int to, BigDecimal amount) {
        try {
            transfer(con, from, to, amount);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    static List<Object> solution2(Connection con) throws SQLException {
        boolean first = tryTransfer(con, 1, 3, new BigDecimal("300"));
        boolean second = tryTransfer(con, 2, 1, new BigDecimal("600"));
        return List.of(first, second, balances(con));
    }

    static List<Object> solution3(Connection con, List<BalanceChange> changes) throws SQLException {
        int ok = 0;
        con.setAutoCommit(false);
        try {
            for (BalanceChange change : changes) {
                Savepoint sp = con.setSavepoint();
                try {
                    changeBalance(con, change.accountId(), change.delta());
                    ok++;
                } catch (SQLException e) {
                    con.rollback(sp);
                }
            }
            con.commit();
        } catch (SQLException | RuntimeException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
        return List.of(ok, balances(con));
    }

    static boolean solution4(Connection con, int id, BigDecimal amount, int readVersion) throws SQLException {
        String sql = "UPDATE accounts SET balance = balance - ?, version = version + 1 WHERE id = ? AND version = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setInt(2, id);
            ps.setInt(3, readVersion);
            return ps.executeUpdate() == 1;
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Pierwsza zmiana jest już zatwierdzona i zostaje — dane są niespójne (pieniądze zniknęły albo powstały).
     *      W auto-commit nie da się jej wycofać; kilka kroków trzeba objąć jedną transakcją.
     *   2. 150.00 — zmiana o +50 sprzed punktu zapisu zostaje, +25 po punkcie zostało wycofane.
     *   3. Brak con.rollback() w catch i połknięty wyjątek: transakcja zostaje otwarta z połową zmian (kolejny commit
     *      na tym połączeniu zatwierdzi debit bez credit), a wywołujący nie wie o błędzie. Poza tym brak finally
     *      przywracającego auto-commit. Poprawnie: rollback(); throw e; finally { setAutoCommit(true); }.
     *   4. Niepowtarzalny odczyt: TEN SAM wiersz przeczytany drugi raz ma inną wartość (ktoś go zmienił i zatwierdził).
     *      Fantom: TO SAMO zapytanie zwraca inny ZBIÓR wierszy (ktoś dodał lub usunął pasujące wiersze).
     *   5. 0 — wersja w bazie (4) różni się od oczekiwanej (3), więc WHERE nie pasuje. Program powinien uznać to
     *      za konflikt: wczytać świeże dane (z wersją 4) i powtórzyć operację albo poinformować użytkownika.
     *   6. Bo pozwala czytać zmiany, które mogą zostać wycofane. W lekcji reader zobaczył saldo 1650.00, którego po
     *      rollback writera nigdy nie było w bazie.
     *   7. Optymistycznie — gdy konflikty są rzadkie albo między odczytem a zapisem mija dużo czasu (użytkownik
     *      edytuje formularz). FOR UPDATE — gdy konflikty są częste, a transakcja krótka; pamiętaj o stałej
     *      kolejności blokowania (zakleszczenia).
     */
    // </editor-fold>
}
