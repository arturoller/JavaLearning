/**
 * TEMAT: t29_jdbc_databases — bazy danych SQL i JDBC (Java Database Connectivity = łączność Javy z bazami danych)
 *
 * <p>Dział uczy dwóch rzeczy naraz: języka SQL (tworzenie tabel, zapytania, złączenia, grupowanie, indeksy) oraz
 * biblioteki JDBC, przez którą program w Javie rozmawia z bazą danych (połączenie, PreparedStatement, transakcje,
 * wzorzec DAO). Wszystkie przykłady działają na bazie H2 uruchamianej w pamięci — nic nie trzeba instalować,
 * a każda lekcja sama tworzy swoje tabele i dane. W kodzie używamy wyłącznie standardowych pakietów
 * java.sql i javax.sql, więc te same zasady działają z PostgreSQL, MySQL czy Oracle.</p>
 *
 * <p>Wymagania: wyjątki i try-with-resources (t10_exceptions), rekordy (t09_records), kolekcje, Optional,
 * strumienie i groupingBy (t16_streams), BigDecimal (t15_numbers), java.time (t17_datetime). Dalej: JdbcTemplate
 * i Spring Data JPA w kursie SpringLearning.</p>
 *
 * <p>Kolejność czytania:</p>
 * <ol>
 *   <li>Jdbc01SqlBasics — tabele, typy, ograniczenia, INSERT/SELECT/UPDATE/DELETE, NULL i logika trójwartościowa</li>
 *   <li>Jdbc02Connection — architektura JDBC, URL połączenia, Connection/Statement/ResultSet, metadane, SQLException</li>
 *   <li>Jdbc03PreparedStatement — parametry ? zamiast sklejania SQL, paczki (batch), wygenerowane klucze, LIKE i IN z danymi od użytkownika</li>
 *   <li>Jdbc04Transactions — auto-commit, commit/rollback, ACID, poziomy izolacji, blokowanie optymistyczne</li>
 *   <li>Jdbc05Dao — wzorzec DAO/repozytorium, mapowanie wierszy na rekordy, własny wyjątek, problem N+1, stronicowanie</li>
 *   <li>Jdbc06SqlJoins — złączenia INNER/LEFT, samozłączenie, wiele-do-wielu, podzapytania, klucze obce i normalizacja</li>
 *   <li>Jdbc07SqlAggregationIndexes — funkcje agregujące, GROUP BY/HAVING, CASE, funkcje okna, indeksy i EXPLAIN</li>
 * </ol>
 *
 * <p>SŁÓWKA: database = baza danych; table = tabela; row = wiersz; column = kolumna; primary key = klucz główny;
 * foreign key = klucz obcy; query = zapytanie; statement = polecenie; result set = zbiór wyników;
 * transaction = transakcja; commit = zatwierdź; rollback = wycofaj; join = złączenie; index = indeks;
 * driver = sterownik; connection = połączenie.</p>
 */
package t29_jdbc_databases;
