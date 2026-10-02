package t29_jdbc_databases;

import helpers.Check;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: DAO i repozytorium — SQL schowany za interfejsem
 *        (DAO = Data Access Object = obiekt dostępu do danych; repository = repozytorium)
 *
 * W SKRÓCIE:
 *   Reszta programu nie powinna wiedzieć, że dane leżą w SQL-u. Interfejs ProductRepository mówi CO umie
 *   (znajdź, zapisz, usuń), a klasa JdbcProductRepository — JAK (PreparedStatement, ResultSet). Dzięki temu
 *   logikę biznesową testujesz na prostej implementacji w pamięci, a wyjątki SQL zamieniasz na własny,
 *   niesprawdzany DataAccessException.
 *
 * ANALOGIA:
 *   Okienko w bibliotece. Mówisz bibliotekarce: „poproszę książkę o numerze 7” (findById). Nie wiesz i nie musisz
 *   wiedzieć, czy książka stoi w magazynie w piwnicy, czy w regale za plecami — liczy się umowa przy okienku.
 *   Bibliotekarkę można zastąpić automatem (implementacja w pamięci), jeśli przestrzega tej samej umowy.
 *
 * JAK TO DZIAŁA:
 *   serwis (logika) ──używa──► ProductRepository (interfejs)
 *                                  ├── JdbcProductRepository     → SQL + RowMapper (ResultSet → record)
 *                                  └── InMemoryProductRepository → TreeMap (do testów)
 *   SQLException (sprawdzany) ──tłumaczenie──► DataAccessException (niesprawdzany, zachowuje SQLState)
 *   Połączenie (Connection) przychodzi z ZEWNĄTRZ → kilka repozytoriów może działać w jednej transakcji.
 *
 * SŁÓWKA:
 *   repository = repozytorium; find by id = znajdź po identyfikatorze; save = zapisz; update = zaktualizuj;
 *   delete = usuń; row mapper = mapper wiersza (zamienia wiersz na obiekt); contract = umowa (kontrakt);
 *   fake = atrapa (prosta implementacja do testów); unit of work = jednostka pracy; page = strona;
 *   N+1 queries = problem N+1 zapytań; translate = przetłumacz
 *
 * ZOBACZ TEŻ: t29_jdbc_databases/Jdbc03PreparedStatement (parametry, klucze generowane),
 *             t29_jdbc_databases/Jdbc04Transactions (wzorzec transakcji),
 *             t29_jdbc_databases/Jdbc06SqlJoins (JOIN — lekarstwo na N+1)
 * </pre>
 */
public class Jdbc05Dao {

    private static final String URL = "jdbc:h2:mem:jdbc05";

    public static void main(String[] args) throws SQLException {
        title("Jdbc05 — DAO i repozytorium: interfejs, JDBC, atrapa w pamięci");

        try (Connection con = DriverManager.getConnection(URL)) {
            createShop(con);                    // create shop = utwórz sklep
            JdbcProductRepository repo = new JdbcProductRepository(con);
            repositoryInterface(repo);          // repository interface = interfejs repozytorium
            rowMapper(repo);                    // row mapper = zamiana wiersza na obiekt
            writeOperations(repo);              // write operations = operacje zapisu
            exceptionTranslation(repo);         // exception translation = tłumaczenie wyjątków
            contractTest(repo);                 // contract test = test umowy (obie implementacje)
            unitOfWork(con, repo);              // unit of work = jednostka pracy (transakcja)
            nPlusOneQueries(repo);            // N+1 queries = problem N+1 zapytań
            pagination(repo);                 // pagination = stronicowanie
            springComparison();                 // Spring comparison = porównanie ze Springiem
        }
        exercises();                            // exercises = ćwiczenia
    }

    // =================================================================================================
    // MODEL I INTERFEJS
    // =================================================================================================

    /**
     * Produkt w tej lekcji (własny record — NIE helpers.model.Product): ma techniczne id nadawane przez bazę.
     * {@code id == null} oznacza „jeszcze nie zapisany”.
     */
    record Product(Long id, String sku, String name, String category, BigDecimal price, int stock) {
        Product withId(long newId) {                      // withId = kopia z nowym id (record jest niezmienny)
            return new Product(newId, sku, name, category, price, stock);
        }

        Product withPrice(BigDecimal newPrice) {          // withPrice = kopia z nową ceną
            return new Product(id, sku, name, category, newPrice, stock);
        }
    }

    /** Umowa: co potrafi magazyn produktów. Żadnego SQL, żadnego SQLException w sygnaturach. */
    interface ProductRepository {
        Optional<Product> findById(long id);              // brak → Optional.empty(), nie null
        List<Product> findAll();                          // posortowane po id
        List<Product> findByCategory(String category);    // posortowane po id
        Product save(Product product);                    // INSERT → zwraca kopię z nadanym id
        boolean update(Product product);                  // false = nie ma takiego id
        boolean delete(long id);                          // false = nie ma takiego id
        List<Product> findPage(int page, int size);       // strony od 0, posortowane po id
    }

    /** Własny wyjątek niesprawdzany warstwy danych; zachowuje SQLState przyczyny. */
    static class DataAccessException extends RuntimeException {
        private final String sqlState;

        DataAccessException(String message, String sqlState, Throwable cause) {
            super(message, cause);
            this.sqlState = sqlState;
        }

        String sqlState() {
            return sqlState;
        }

        boolean isDuplicateKey() {                        // is duplicate key = czy duplikat klucza
            return "23505".equals(sqlState);
        }
    }

    // =================================================================================================
    // 1. INTERFEJS REPOZYTORIUM — KOD KLIENTA NIE WIDZI SQL
    // =================================================================================================

    /**
     * 1. Kod korzystający z repozytorium widzi tylko metody interfejsu i obiekty Javy. findById zwraca Optional —
     * brak wiersza to normalna sytuacja, nie błąd (patrz t14_optional/Optional01Basics).
     */
    static void repositoryInterface(ProductRepository repo) {
        section("1. Interfejs repozytorium — kod klienta nie widzi SQL");

        show("findById(1)", repo.findById(1).map(Product::name).orElse("brak"));
        show("findById(99)", repo.findById(99).map(Product::name).orElse("brak"));
        show("findByCategory(KSIAZKI)", repo.findByCategory("KSIAZKI").stream().map(Product::name).toList());   // toList (Java 16+)
        show("findAll().size()", repo.findAll().size());
        // WYNIK: findById(1) → Laptop Pro 14
        // WYNIK: findById(99) → brak
        // WYNIK: findByCategory(KSIAZKI) → [Czysty kod, Java. Podstawy, Wzorce projektowe]
        // WYNIK: findAll().size() → 14

        // DOBRA PRAKTYKA: w sygnaturach repozytorium nie ma Connection, ResultSet ani SQLException. Dlaczego?
        // Bo wtedy każda klasa, która go używa, musiałaby znać JDBC — a zmiana bazy (albo podmiana na atrapę
        // w teście) wymagałaby zmian w całym programie.
        // DAO czy repozytorium? DAO to starsza nazwa (obiekt „pod tabelę”: insert/update/select), repozytorium
        // (z DDD) udaje kolekcję obiektów domenowych. W praktyce, także w Springu, często to to samo.
    }

    // =================================================================================================
    // 2. ROW MAPPER — WIERSZ ResultSet → RECORD
    // =================================================================================================

    /**
     * 2. Mapper wiersza to jedna funkcja „bieżący wiersz ResultSet → obiekt”. Piszemy ją RAZ i używamy we wszystkich
     * zapytaniach — dokładnie tak działa RowMapper w Springowym JdbcTemplate.
     */
    static void rowMapper(JdbcProductRepository repo) {
        section("2. Row mapper — wiersz ResultSet → record");

        Product p = repo.findById(3).orElseThrow();      // orElseThrow (Java 10+) = pobierz albo rzuć wyjątek
        show("zmapowany record", p);
        // WYNIK: zmapowany record → Product[id=3, sku=ELE-003, name=Słuchawki BT, category=ELEKTRONIKA, price=349.90, stock=25]
        // PUŁAPKA: mapper czytający kolumny po NUMERZE (rs.getString(2)) psuje się po zmianie kolejności kolumn
        // w SELECT. Czytaj po nazwie: rs.getString("name") — i wypisuj kolumny w SELECT jawnie, bez SELECT *.
        // PUŁAPKA: kolumna liczbowa z NULL: rs.getInt zwraca 0 (patrz Jdbc02Connection, wasNull). Dla kolumn
        // dopuszczających NULL użyj rs.getObject("kol", Integer.class) — wtedy dostaniesz null.

        // Mapper jest zwykłą wartością — można przekazać inny, np. tylko nazwy (mini-JdbcTemplate z tej lekcji)
        // query = zapytaj (wykonaj SELECT i zmapuj wiersze)
        List<String> names = repo.query("SELECT name FROM products WHERE stock = ? ORDER BY name",
                rs -> rs.getString("name"), 0);
        show("produkty z zerowym stanem", names);
        // WYNIK: produkty z zerowym stanem → [Oliwa z oliwek, Smartfon X]
    }

    // =================================================================================================
    // 3. OPERACJE ZAPISU — save, update, delete
    // =================================================================================================

    /**
     * 3. save robi INSERT i zwraca kopię obiektu z id nadanym przez bazę (getGeneratedKeys). update i delete
     * zwracają boolean — true, gdy executeUpdate zmienił dokładnie 1 wiersz.
     */
    static void writeOperations(ProductRepository repo) {
        section("3. Operacje zapisu — save, update, delete");

        Product saved = repo.save(new Product(null, "DOM-003", "Koc wełniany", "DOM", new BigDecimal("149.00"), 6));
        show("save → id nadane przez bazę", saved.id());
        // WYNIK: save → id nadane przez bazę → 15

        boolean updated = repo.update(saved.withPrice(new BigDecimal("129.00")));
        show("update istniejącego", updated);
        show("cena po update", repo.findById(saved.id()).map(Product::price).orElseThrow());
        show("update nieistniejącego (id 999)", repo.update(saved.withId(999)));
        // WYNIK: update istniejącego → true
        // WYNIK: cena po update → 129.00
        // WYNIK: update nieistniejącego (id 999) → false

        show("delete", repo.delete(saved.id()));
        show("delete ponownie", repo.delete(saved.id()));
        show("findById po usunięciu", repo.findById(saved.id()).isPresent());
        // WYNIK: delete → true
        // WYNIK: delete ponownie → false
        // WYNIK: findById po usunięciu → false
        // DOBRA PRAKTYKA: nie ignoruj wyniku executeUpdate. 0 zmienionych wierszy przy update często oznacza błąd
        // w logice (zły id, wiersz usunięty przez kogoś innego) — repozytorium powinno to zgłosić (tu: false).
    }

    // =================================================================================================
    // 4. TŁUMACZENIE WYJĄTKÓW — SQLException → DataAccessException
    // =================================================================================================

    /**
     * 4. SQLException jest sprawdzany — każda metoda w górę musiałaby go deklarować. Repozytorium łapie go
     * i rzuca DataAccessException (niesprawdzany), zachowując SQLState i przyczynę (cause).
     */
    static void exceptionTranslation(ProductRepository repo) {
        section("4. Tłumaczenie wyjątków — SQLException → DataAccessException");

        try {
            repo.save(new Product(null, "ELE-001", "Drugi laptop", "ELEKTRONIKA", new BigDecimal("10.00"), 1));
        } catch (DataAccessException e) {
            show("typ wyjątku", e.getClass().getSimpleName());   // getSimpleName = krótka nazwa klasy
            show("SQLState", e.sqlState());
            show("duplikat klucza?", e.isDuplicateKey());
            show("przyczyna (cause) to SQLException", e.getCause() instanceof SQLException);
        }
        // WYNIK: typ wyjątku → DataAccessException
        // WYNIK: SQLState → 23505
        // WYNIK: duplikat klucza? → true
        // WYNIK: przyczyna (cause) to SQLException → true

        expectThrows("produkt z ujemną ceną",
                () -> repo.save(new Product(null, "X-1", "Błąd", "DOM", new BigDecimal("-1"), 0)));
        // WYNIK: ✔ produkt z ujemną ceną → rzucono DataAccessException: nie udało się zapisać produktu X-1 (SQLState 23513)
        Product next = repo.save(new Product(null, "DOM-004", "Wazon", "DOM", new BigDecimal("39.00"), 2));
        show("id następnego udanego zapisu", next.id());
        repo.delete(next.id());
        // WYNIK: id następnego udanego zapisu → 18
        // PUŁAPKA: luki w numeracji są normalne. Ostatni udany zapis dostał 15, a ten — 18: dwa nieudane INSERT-y
        // też pobrały numery z licznika IDENTITY (licznik nie cofa się nawet przy rollback — tak działają H2,
        // PostgreSQL i MySQL). Nie licz produktów po max(id) i nie zakładaj, że id idą „bez dziur”.
        // Komunikat wyjątku jest nasz (po polsku, bez tekstu H2) — dlatego ten wiersz wygląda tak samo u każdego.
        // PUŁAPKA: nie „gub” przyczyny: new DataAccessException("...", state, e) — bez e tracisz stos wywołań
        // i komunikat sterownika, które są bezcenne przy szukaniu błędu w logach.
        // Spring robi to samo automatycznie: SQLException → hierarchia DataAccessException
        // (np. DuplicateKeyException, DataIntegrityViolationException) — rozpoznaje je właśnie po SQLState/kodzie błędu.
    }

    // =================================================================================================
    // 5. PODRÓBKA W PAMIĘCI I TEST UMOWY
    // =================================================================================================

    /**
     * 5. InMemoryProductRepository spełnia ten sam interfejs, ale trzyma dane w TreeMap. Żeby atrapa nie kłamała,
     * uruchamiamy NA OBU implementacjach ten sam zestaw sprawdzeń (test umowy, contract test).
     */
    static void contractTest(ProductRepository jdbcRepo) {
        section("5. Atrapa w pamięci i test umowy");

        InMemoryProductRepository fake = new InMemoryProductRepository();
        for (Product p : jdbcRepo.findAll()) {                 // atrapa startuje z tymi samymi danymi
            fake.save(new Product(null, p.sku(), p.name(), p.category(), p.price(), p.stock()));
        }
        checkContract("JDBC", jdbcRepo);                        // check contract = sprawdź umowę
        checkContract("pamięć", fake);
        Check.summary();
        // WYNIK: ✔ OK    JDBC: findById(1) → Laptop Pro 14
        // WYNIK: ✔ OK    JDBC: findById(999) → pusty
        // WYNIK: ✔ OK    JDBC: KSIAZKI po id
        // WYNIK: ✔ OK    JDBC: save nadaje nowe id (> 14)
        // WYNIK: ✔ OK    JDBC: duplikat SKU → DataAccessException
        // WYNIK: ✔ OK    JDBC: delete
        // WYNIK: ✔ OK    JDBC: strona 2 (po 5)
        // WYNIK: ✔ OK    pamięć: findById(1) → Laptop Pro 14
        // WYNIK: ✔ OK    pamięć: findById(999) → pusty
        // WYNIK: ✔ OK    pamięć: KSIAZKI po id
        // WYNIK: ✔ OK    pamięć: save nadaje nowe id (> 14)
        // WYNIK: ✔ OK    pamięć: duplikat SKU → DataAccessException
        // WYNIK: ✔ OK    pamięć: delete
        // WYNIK: ✔ OK    pamięć: strona 2 (po 5)
        // WYNIK: PODSUMOWANIE: ✔ 14 OK, ✘ 0 BŁĄD
        // DOBRA PRAKTYKA: testy logiki biznesowej (serwisów) uruchamiaj na atrapie — są szybkie i nie potrzebują bazy.
        // Testy samego repozytorium JDBC uruchamiaj na prawdziwej bazie (H2 albo, lepiej, tej samej co na
        // produkcji, np. w kontenerze — Testcontainers). Test umowy pilnuje, żeby obie zachowywały się tak samo.
    }

    /** Ten sam zestaw sprawdzeń dla każdej implementacji. Po teście dane wracają do stanu wyjściowego. */
    static void checkContract(String label, ProductRepository repo) {
        // Dokładnego id nie sprawdzamy: w bazie licznik ma luki po nieudanych zapisach (sekcja 4), w atrapie — nie.
        Check.equal(label + ": findById(1) → Laptop Pro 14", Optional.of("Laptop Pro 14"), repo.findById(1).map(Product::name));
        Check.equal(label + ": findById(999) → pusty", Optional.empty(), repo.findById(999));
        Check.equal(label + ": KSIAZKI po id", List.of("KSI-001", "KSI-002", "KSI-003"),
                repo.findByCategory("KSIAZKI").stream().map(Product::sku).toList());
        Product saved = repo.save(new Product(null, "TST-001", "Test", "DOM", new BigDecimal("1.00"), 1));
        Check.equal(label + ": save nadaje nowe id (> 14)", true, saved.id() > 14);
        Check.throwsException(label + ": duplikat SKU → DataAccessException", DataAccessException.class,
                () -> repo.save(new Product(null, "TST-001", "Inny", "DOM", BigDecimal.ONE, 1)));
        Check.equal(label + ": delete", true, repo.delete(saved.id()));
        Check.equal(label + ": strona 2 (po 5)", List.of(11L, 12L, 13L, 14L),
                repo.findPage(2, 5).stream().map(Product::id).toList());
    }

    // =================================================================================================
    // 6. JEDNOSTKA PRACY — POŁĄCZENIE PRZEKAZANE Z ZEWNĄTRZ, JEDNA TRANSAKCJA
    // =================================================================================================

    /**
     * 6. Repozytorium nie otwiera własnych połączeń i nie robi commit — dostaje Connection z zewnątrz. Dzięki temu
     * serwis może objąć jedną transakcją pracę kilku repozytoriów: obniżkę cen i wpisy do dziennika zmian.
     */
    static void unitOfWork(Connection con, JdbcProductRepository repo) throws SQLException {
        section("6. Jednostka pracy — jedna transakcja dla kilku repozytoriów");

        PriceLog log = new PriceLog(con);                       // price log = dziennik zmian cen
        discountCategory(con, repo, log, "KSIAZKI", 10);       // discount category = obniż ceny w kategorii
        show("ceny KSIAZKI po -10%", repo.findByCategory("KSIAZKI").stream().map(Product::price).toList());
        show("wpisów w dzienniku", log.count());
        // WYNIK: ceny KSIAZKI po -10% → [71.10, 116.10, 89.10]
        // WYNIK: wpisów w dzienniku → 3

        // Obniżka o 100% da cenę 0.00 — CHECK (price > 0) odrzuci ją przy PIERWSZYM produkcie z ceną > 0…
        try {
            discountCategory(con, repo, log, "ODZIEZ", 100);
        } catch (DataAccessException e) {
            show("obniżka 100% odrzucona, SQLState", e.sqlState());
        }
        show("ceny ODZIEZ bez zmian", repo.findByCategory("ODZIEZ").stream().map(Product::price).toList());
        show("wpisów w dzienniku nadal", log.count());
        // WYNIK: obniżka 100% odrzucona, SQLState → 23513
        // WYNIK: ceny ODZIEZ bez zmian → [459.00, 49.99]
        // WYNIK: wpisów w dzienniku nadal → 3
        // …a rollback cofnął WSZYSTKO z tej transakcji, także wpisy w dzienniku zrobione przez drugie repozytorium.
        // PUŁAPKA: gdyby każde repozytorium brało własne połączenie z DriverManager, miałoby własną transakcję —
        // rollback w jednym nie cofnąłby zmian w drugim. Wspólne połączenie = wspólna transakcja.
        // W Springu to samo robi @Transactional: wiąże jedno połączenie z bieżącym wątkiem na czas metody.
    }

    /** Serwis: obniża ceny w kategorii o {@code percent}% i zapisuje dziennik — wszystko albo nic. */
    static void discountCategory(Connection con, ProductRepository repo, PriceLog log, String category, int percent)
            throws SQLException {
        boolean previous = con.getAutoCommit();
        con.setAutoCommit(false);
        try {
            BigDecimal factor = BigDecimal.valueOf(100 - percent).movePointLeft(2);   // np. 90 → 0.90
            for (Product p : repo.findByCategory(category)) {
                BigDecimal newPrice = p.price().multiply(factor).setScale(2, RoundingMode.HALF_UP);
                log.add(p.id(), p.price(), newPrice);
                repo.update(p.withPrice(newPrice));
            }
            con.commit();
        } catch (SQLException | RuntimeException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(previous);
        }
    }

    // =================================================================================================
    // 7. PROBLEM N+1 ZAPYTAŃ
    // =================================================================================================

    /**
     * 7. N+1: jedno zapytanie po listę (1), a potem osobne zapytanie dla KAŻDEGO elementu (N). Przy 14 produktach to
     * 15 podróży do bazy; przy 10 000 — aplikacja „nagle” zwalnia. Liczymy zapytania licznikiem w repozytorium.
     */
    static void nPlusOneQueries(JdbcProductRepository repo) {
        section("7. Problem N+1 zapytań");

        repo.resetQueryCount();                                 // reset query count = wyzeruj licznik zapytań
        List<String> lines = new ArrayList<>();
        for (Product p : repo.findAll()) {                                       // 1 zapytanie
            String display = repo.query("SELECT display_name FROM categories WHERE code = ?",
                    rs -> rs.getString("display_name"), p.category()).get(0);    // + 1 zapytanie NA KAŻDY produkt
            lines.add(p.sku() + " " + display);
        }
        show("pierwsze 2 wiersze", lines.subList(0, 2));
        show("zapytań (N+1)", repo.queryCount());   // queryCount = liczba zapytań
        // WYNIK: pierwsze 2 wiersze → [ELE-001 Elektronika, ELE-002 Elektronika]
        // WYNIK: zapytań (N+1) → 15

        // Lekarstwo: JEDNO zapytanie z JOIN (szczegóły złączeń: Jdbc06SqlJoins)
        repo.resetQueryCount();
        List<String> joined = repo.query("""
                SELECT p.sku, c.display_name
                FROM products p JOIN categories c ON c.code = p.category
                ORDER BY p.id""", rs -> rs.getString("sku") + " " + rs.getString("display_name"));
        show("pierwsze 2 wiersze", joined.subList(0, 2));
        show("zapytań (JOIN)", repo.queryCount());
        show("te same wyniki", joined.equals(lines));
        // WYNIK: pierwsze 2 wiersze → [ELE-001 Elektronika, ELE-002 Elektronika]
        // WYNIK: zapytań (JOIN) → 1
        // WYNIK: te same wyniki → true
        // Drugie lekarstwo: 2 zapytania — lista, a potem wszystkie potrzebne kategorie naraz przez IN (?, ?, …)
        // (Jdbc03PreparedStatement) i złożenie w Javie przez Map. Dobre, gdy JOIN mnożyłby wiersze.
        // PUŁAPKA: w JPA/Hibernate N+1 powstaje „samo” przy leniwym ładowaniu relacji (pętla po zamówieniach i
        // order.getCustomer()) — w kodzie nie widać żadnego zapytania. Włączaj logowanie SQL i licz zapytania.
    }

    // =================================================================================================
    // 8. STRONICOWANIE — LIMIT/OFFSET I KLUCZ (KEYSET)
    // =================================================================================================

    /**
     * 8. Strona = ORDER BY po unikalnym kluczu + LIMIT ? OFFSET ?. Przy dalekich stronach OFFSET jest drogi (baza
     * i tak czyta pomijane wiersze), więc dla „przewijania” lepsze jest stronicowanie po kluczu: {@code WHERE id > ostatnie_id}.
     */
    static void pagination(JdbcProductRepository repo) {
        section("8. Stronicowanie — LIMIT/OFFSET i po kluczu");

        for (int page = 0; page < 3; page++) {
            System.out.println("   strona " + page + ": " + repo.findPage(page, 5).stream().map(Product::id).toList());
        }
        // WYNIK: strona 0: [1, 2, 3, 4, 5]
        // WYNIK: strona 1: [6, 7, 8, 9, 10]
        // WYNIK: strona 2: [11, 12, 13, 14]

        // Po kluczu (keyset): „daj 5 produktów o id większym niż ostatnie z poprzedniej strony”
        List<Long> afterTen = repo.query("SELECT id FROM products WHERE id > ? ORDER BY id LIMIT ?",
                rs -> rs.getLong("id"), 10L, 5);
        show("po kluczu: id > 10", afterTen);
        // WYNIK: po kluczu: id > 10 → [11, 12, 13, 14]
        // PUŁAPKA: przy OFFSET wiersz dodany lub usunięty między pobraniem stron przesuwa wszystko — użytkownik
        // zobaczy jakiś produkt dwa razy albo wcale. Stronicowanie po kluczu jest na to odporne (i szybkie z indeksem).
        // Do „strona 7 z 20” potrzebujesz też łącznej liczby: osobne SELECT COUNT(*) z tym samym WHERE.
    }

    // =================================================================================================
    // 9. TO SAMO W SPRINGU — JdbcTemplate I SPRING DATA
    // =================================================================================================

    /** 9. Ręczny JDBC uczy mechanizmu; w projektach Springa ten sam kod jest kilka razy krótszy. */
    static void springComparison() {
        section("9. To samo w Springu — JdbcTemplate i Spring Data");

        note("JdbcTemplate: Ty piszesz SQL i mapper, Spring — połączenia, zamykanie, wyjątki");
        note("Spring Data: Ty piszesz interfejs, Spring — implementację z nazw metod");
        // WYNIK: ℹ JdbcTemplate: Ty piszesz SQL i mapper, Spring — połączenia, zamykanie, wyjątki
        // WYNIK: ℹ Spring Data: Ty piszesz interfejs, Spring — implementację z nazw metod
        // JdbcTemplate (szablon JDBC) — odpowiednik naszego query(sql, mapper, params...):
        //   jdbc.query("SELECT ... WHERE category = ? ORDER BY id", (rs, rowNum) -> new Product(...), category);
        // Spring Data JDBC / JPA — nawet bez implementacji:
        //   interface ProductRepository extends CrudRepository<Product, Long> {
        //       List<Product> findByCategoryOrderById(String category);   // SQL z NAZWY metody
        //   }
        // JPA/Hibernate idzie dalej: mapuje obiekty na tabele (ORM = mapowanie obiektowo-relacyjne), śledzi zmiany
        // i sam generuje UPDATE. Wygodne, ale ukrywa SQL — stąd pułapki typu N+1. Szczegóły: SpringLearning.
    }

    // =================================================================================================
    // IMPLEMENTACJE
    // =================================================================================================

    /** Mapper wiersza: zamienia BIEŻĄCY wiersz ResultSet na obiekt (nie wołaj w nim rs.next()). */
    @FunctionalInterface
    interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    /** Implementacja JDBC. Połączenie dostaje z zewnątrz i go nie zamyka ani nie zatwierdza. */
    static class JdbcProductRepository implements ProductRepository {
        private static final String COLUMNS = "id, sku, name, category, price, stock";
        /** Jedyne miejsce, które wie, jak wiersz tabeli products zamienia się w Product. */
        static final RowMapper<Product> PRODUCT_MAPPER = rs -> new Product(
                rs.getLong("id"), rs.getString("sku"), rs.getString("name"), rs.getString("category"),
                rs.getBigDecimal("price"), rs.getInt("stock"));

        private final Connection con;
        private int queryCount;                              // licznik zapytań — tylko do demonstracji N+1

        JdbcProductRepository(Connection con) {
            this.con = Objects.requireNonNull(con);          // requireNonNull = wymagaj nie-null
        }

        /** Mini-JdbcTemplate: przygotuj, ustaw parametry po kolei (setObject), zmapuj każdy wiersz, zamknij zasoby. */
        <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
            queryCount++;
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
                List<T> result = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.add(mapper.map(rs));
                    }
                }
                return result;
            } catch (SQLException e) {
                throw translate("zapytanie nie powiodło się", e);
            }
        }

        /** Wykonuje INSERT/UPDATE/DELETE i zwraca liczbę zmienionych wierszy. */
        private int update(String sql, Object... params) {
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
                return ps.executeUpdate();
            } catch (SQLException e) {
                throw translate("zapis nie powiódł się", e);
            }
        }

        /** Tłumaczenie: własny polski komunikat + SQLState + przyczyna. */
        private static DataAccessException translate(String message, SQLException e) {
            return new DataAccessException(message + " (SQLState " + e.getSQLState() + ")", e.getSQLState(), e);
        }

        @Override
        public Optional<Product> findById(long id) {
            // stream().findFirst() = pierwszy element jako Optional (pusty, gdy brak wierszy)
            return query("SELECT " + COLUMNS + " FROM products WHERE id = ?", PRODUCT_MAPPER, id).stream().findFirst();
        }

        @Override
        public List<Product> findAll() {
            return query("SELECT " + COLUMNS + " FROM products ORDER BY id", PRODUCT_MAPPER);
        }

        @Override
        public List<Product> findByCategory(String category) {
            return query("SELECT " + COLUMNS + " FROM products WHERE category = ? ORDER BY id", PRODUCT_MAPPER, category);
        }

        @Override
        public Product save(Product p) {
            String sql = "INSERT INTO products (sku, name, category, price, stock) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, p.sku());
                ps.setString(2, p.name());
                ps.setString(3, p.category());
                ps.setBigDecimal(4, p.price());
                ps.setInt(5, p.stock());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return p.withId(keys.getLong(1));
                }
            } catch (SQLException e) {
                throw translate("nie udało się zapisać produktu " + p.sku(), e);
            }
        }

        @Override
        public boolean update(Product p) {
            return update("UPDATE products SET sku = ?, name = ?, category = ?, price = ?, stock = ? WHERE id = ?",
                    p.sku(), p.name(), p.category(), p.price(), p.stock(), p.id()) == 1;
        }

        @Override
        public boolean delete(long id) {
            return update("DELETE FROM products WHERE id = ?", id) == 1;
        }

        @Override
        public List<Product> findPage(int page, int size) {
            return query("SELECT " + COLUMNS + " FROM products ORDER BY id LIMIT ? OFFSET ?", PRODUCT_MAPPER, size, page * size);
        }

        int queryCount() {
            return queryCount;
        }

        void resetQueryCount() {
            queryCount = 0;
        }
    }

    /**
     * Atrapa w pamięci: TreeMap (klucze posortowane = kolejność po id jak ORDER BY id). Naśladuje też reguły bazy,
     * które są częścią umowy: unikalne SKU (SQLState 23505) i cena dodatnia (23513).
     */
    static class InMemoryProductRepository implements ProductRepository {
        private final Map<Long, Product> rows = new TreeMap<>();
        private long nextId = 1;

        @Override
        public Optional<Product> findById(long id) {
            return Optional.ofNullable(rows.get(id));        // ofNullable = Optional z wartości, która może być null
        }

        @Override
        public List<Product> findAll() {
            return List.copyOf(rows.values());               // copyOf (Java 10+) = niezmienna kopia
        }

        @Override
        public List<Product> findByCategory(String category) {
            return rows.values().stream().filter(p -> p.category().equals(category)).toList();
        }

        @Override
        public Product save(Product p) {
            validate(p, null);
            Product saved = p.withId(nextId++);
            rows.put(saved.id(), saved);
            return saved;
        }

        @Override
        public boolean update(Product p) {
            if (p.id() == null || !rows.containsKey(p.id())) {
                return false;
            }
            validate(p, p.id());
            rows.put(p.id(), p);
            return true;
        }

        @Override
        public boolean delete(long id) {
            return rows.remove(id) != null;
        }

        @Override
        public List<Product> findPage(int page, int size) {
            return rows.values().stream().skip((long) page * size).limit(size).toList();
        }

        private void validate(Product p, Long ownId) {
            boolean duplicate = rows.values().stream()
                    .anyMatch(other -> other.sku().equals(p.sku()) && !other.id().equals(ownId));
            if (duplicate) {
                throw new DataAccessException("nie udało się zapisać produktu " + p.sku() + " (SQLState 23505)", "23505", null);
            }
            if (p.price().signum() <= 0) {                   // signum = znak liczby: -1, 0, 1
                throw new DataAccessException("nie udało się zapisać produktu " + p.sku() + " (SQLState 23513)", "23513", null);
            }
        }
    }

    /** Drugie „repozytorium”: dziennik zmian cen. Używa TEGO SAMEGO połączenia co ProductRepository. */
    static class PriceLog {
        private final Connection con;

        PriceLog(Connection con) {
            this.con = con;
        }

        void add(long productId, BigDecimal oldPrice, BigDecimal newPrice) {
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO price_log (product_id, old_price, new_price) VALUES (?, ?, ?)")) {
                ps.setLong(1, productId);
                ps.setBigDecimal(2, oldPrice);
                ps.setBigDecimal(3, newPrice);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new DataAccessException("nie udało się zapisać dziennika", e.getSQLState(), e);
            }
        }

        int count() throws SQLException {
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM price_log")) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // =================================================================================================
    // SCHEMAT I DANE
    // =================================================================================================

    static final String SCHEMA_AND_DATA = """
            CREATE TABLE categories (
                code         VARCHAR(20)  PRIMARY KEY,
                display_name VARCHAR(50)  NOT NULL
            );
            CREATE TABLE products (
                id       BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                sku      VARCHAR(10)   NOT NULL UNIQUE,
                name     VARCHAR(100)  NOT NULL,
                category VARCHAR(20)   NOT NULL REFERENCES categories (code),
                price    DECIMAL(10,2) NOT NULL CHECK (price > 0),
                stock    INT           DEFAULT 0 NOT NULL CHECK (stock >= 0)
            );
            CREATE TABLE price_log (
                id         BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                product_id BIGINT        NOT NULL,
                old_price  DECIMAL(10,2) NOT NULL,
                new_price  DECIMAL(10,2) NOT NULL
            );
            INSERT INTO categories (code, display_name) VALUES
                ('ELEKTRONIKA', 'Elektronika'), ('SPOZYWCZE', 'Spożywcze'), ('KSIAZKI', 'Książki'),
                ('ODZIEZ', 'Odzież'), ('DOM', 'Dom i ogród');
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
            """;

    /** Wykonuje skrypt: polecenia oddzielone średnikami (uproszczenie: ';' nie występuje w tekstach). */
    static void createShop(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            for (String sql : SCHEMA_AND_DATA.split(";")) {
                if (!sql.isBlank()) {            // isBlank = czy pusty lub same spacje (Java 11+)
                    st.execute(sql);
                }
            }
        }
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Interfejs repozytorium: findById → Optional, findAll/findByCategory → List (zawsze z ORDER BY),
     *     save → obiekt z nadanym id, update/delete → boolean (executeUpdate == 1). Bez typów JDBC w sygnaturach.
     *   • RowMapper: rs → obiekt, czytanie kolumn PO NAZWIE, jedno miejsce mapowania na tabelę.
     *   • SQLException → własny niesprawdzany DataAccessException(komunikat, SQLState, przyczyna).
     *   • Connection z zewnątrz (konstruktor) → kilka repozytoriów w jednej transakcji; commit/rollback robi serwis.
     *   • Atrapa w pamięci (TreeMap) do testów logiki + ten sam zestaw sprawdzeń dla obu implementacji (test umowy).
     *   • N+1: 1 zapytanie po listę + N po szczegóły → JOIN albo IN (?, ?, …). Licz zapytania.
     *   • Strony: ORDER BY unikalny klucz + LIMIT ? OFFSET ?; szybciej i stabilniej: WHERE id > ? ORDER BY id LIMIT ?.
     *   • Spring: JdbcTemplate (SQL + mapper), Spring Data (interfejs), JPA (ORM, @Version, uwaga na N+1).
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego findById zwraca Optional, a nie null albo wyjątek?
     *   2. Po co tłumaczyć SQLException na własny wyjątek niesprawdzany? Co trzeba w nim zachować?
     *   3. ZNAJDŹ BŁĄD (oba repozytoria mają działać w jednej transakcji):
     *        class OrderRepository { void save(Order o) {
     *            try (Connection c = DriverManager.getConnection(URL)) { ... INSERT ... } } }
     *   4. Co wypisze (dane lekcji, 14 produktów o id 1–14):  System.out.println(repo.findPage(2, 6).size());  ?
     *   5. Ile zapytań wykona pętla, która dla każdego z 50 zamówień woła customerRepo.findById(o.customerId()),
     *      jeśli zamówienia pobrano jednym findAll()? Jak to zmniejszyć?
     *   6. ZNAJDŹ BŁĄD w mapperze:  rs -> new Product(rs.getLong(1), rs.getString(3), rs.getString(2), ...)
     *      dla zapytania SELECT id, sku, name, ...
     *   7. Po co uruchamiać te same sprawdzenia na repozytorium JDBC i na atrapie w pamięci?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** Funkcja na repozytorium JDBC, która może rzucić SQLException (SqlWork = praca z bazą). */
    @FunctionalInterface
    interface SqlWork<T> {
        T apply(Connection con, JdbcProductRepository repo) throws SQLException;
    }

    /** NOWA baza z danymi lekcji dla każdego ćwiczenia; zamykana po pracy. */
    static <T> T inFreshDb(String dbName, SqlWork<T> work) {
        try (Connection con = DriverManager.getConnection("jdbc:h2:mem:" + dbName)) {
            createShop(con);
            return work.apply(con, new JdbcProductRepository(con));
        } catch (SQLException e) {
            throw new IllegalStateException("błąd SQL, SQLState " + e.getSQLState(), e);
        }
    }

    /** Atrapa wypełniona tymi samymi 14 produktami (id 1–14); filled fake = wypełniona atrapa. */
    static InMemoryProductRepository filledFake() {
        return inFreshDb("jdbc05_fake", (con, repo) -> {
            InMemoryProductRepository fake = new InMemoryProductRepository();
            for (Product p : repo.findAll()) {
                fake.save(new Product(null, p.sku(), p.name(), p.category(), p.price(), p.stock()));
            }
            return fake;
        });
    }

    // NEW_LAMP = nowa lampka (SKU już zajęte), NEW_KETTLE = nowy czajnik
    static final Product NEW_LAMP = new Product(null, "DOM-002", "Lampka nocna", "DOM", new BigDecimal("59.00"), 3);
    static final Product NEW_KETTLE = new Product(null, "DOM-005", "Czajnik", "DOM", new BigDecimal("119.00"), 4);

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: dostępne produkty DOM i KSIAZKI (pamięć)", List.of("Czysty kod", "Java. Podstawy", "Wzorce projektowe", "Ekspres do kawy", "Lampka biurkowa"),
                () -> exercise1(filledFake()));
        Check.equal("ćw. 2: własny mapper → findById(8)", "Czysty kod|KSIAZKI|79.00",
                () -> inFreshDb("jdbc05_e2", (c, r) -> r.query("SELECT id, sku, name, category, price, stock FROM products WHERE id = ?",
                        Jdbc05Dao::exercise2, 8L).get(0)));
        Check.equal("ćw. 3: zapis z obsługą duplikatu (JDBC)", List.of("DUPLIKAT DOM-002", "ZAPISANO DOM-005"),
                () -> inFreshDb("jdbc05_e3", (c, r) -> List.of(exercise3(r, NEW_LAMP), exercise3(r, NEW_KETTLE))));
        Check.equal("ćw. 3: zapis z obsługą duplikatu (pamięć)", List.of("DUPLIKAT DOM-002", "ZAPISANO DOM-005"),
                () -> List.of(exercise3(filledFake(), NEW_LAMP), exercise3(filledFake(), NEW_KETTLE)));
        Check.equal("ćw. 4: rozmiary stron po kluczu (po 5)", List.of(5, 5, 4),
                () -> inFreshDb("jdbc05_e4", (c, r) -> exercise4(c, 5)));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of("Czysty kod", "Java. Podstawy", "Wzorce projektowe", "Ekspres do kawy", "Lampka biurkowa"),
                () -> solution1(filledFake()));
        Check.equal("ćw. 2 (wzorzec)", "Czysty kod|KSIAZKI|79.00",
                () -> inFreshDb("jdbc05_s2", (c, r) -> r.query("SELECT id, sku, name, category, price, stock FROM products WHERE id = ?",
                        Jdbc05Dao::solution2, 8L).get(0)));
        Check.equal("ćw. 3 (wzorzec, JDBC)", List.of("DUPLIKAT DOM-002", "ZAPISANO DOM-005"),
                () -> inFreshDb("jdbc05_s3", (c, r) -> List.of(solution3(r, NEW_LAMP), solution3(r, NEW_KETTLE))));
        Check.equal("ćw. 3 (wzorzec, pamięć)", List.of("DUPLIKAT DOM-002", "ZAPISANO DOM-005"),
                () -> List.of(solution3(filledFake(), NEW_LAMP), solution3(filledFake(), NEW_KETTLE)));
        Check.equal("ćw. 4 (wzorzec)", List.of(5, 5, 4),
                () -> inFreshDb("jdbc05_s4", (c, r) -> solution4(c, 5)));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): korzystając TYLKO z interfejsu ProductRepository zwróć nazwy produktów z kategorii KSIAZKI,
     * a potem DOM (w tej kolejności kategorii, wewnątrz po id), które są w magazynie ({@code stock > 0}).
     * Podpowiedź: repo.findByCategory(...) dla obu kategorii, Stream.concat albo dwie pętle, filter po stock.
     */
    static List<String> exercise1(ProductRepository repo) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): napisz mapper wiersza, który zamiast Product zwraca tekst „nazwa|kategoria|cena”
     * (np. „Czysty kod|KSIAZKI|79.00”). Czytaj kolumny PO NAZWIE.
     * Podpowiedź: rs.getString("name") + "|" + ... + rs.getBigDecimal("price").
     */
    static String exercise2(ResultSet rs) throws SQLException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zapisz produkt przez repozytorium. Zwróć „ZAPISANO sku” albo — gdy SKU już istnieje —
     * „DUPLIKAT sku”. Inne błędy przepuść dalej. Ma działać tak samo dla JDBC i dla atrapy (test umowy!).
     * Podpowiedź: try { repo.save(p) } catch (DataAccessException e) { if (e.isDuplicateKey()) ...; throw e; }
     */
    static String exercise3(ProductRepository repo, Product product) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): przejdź przez WSZYSTKIE produkty stronami po kluczu (keyset) o rozmiarze {@code size}:
     * {@code SELECT id FROM products WHERE id > ? ORDER BY id LIMIT ?}, zaczynając od {@code id > 0}, a każdą następną stronę
     * od ostatniego id poprzedniej. Zwróć rozmiary kolejnych niepustych stron.
     * Podpowiedź: pętla while; pusta strona = koniec; lastId = ostatni element strony.
     */
    static List<Integer> exercise4(Connection con, int size) throws SQLException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solution1(ProductRepository repo) {
        List<String> result = new ArrayList<>();
        for (String category : List.of("KSIAZKI", "DOM")) {
            repo.findByCategory(category).stream()
                    .filter(p -> p.stock() > 0)
                    .map(Product::name)
                    .forEach(result::add);
        }
        return result;
    }

    static String solution2(ResultSet rs) throws SQLException {
        return rs.getString("name") + "|" + rs.getString("category") + "|" + rs.getBigDecimal("price");
    }

    static String solution3(ProductRepository repo, Product product) {
        try {
            repo.save(product);
            return "ZAPISANO " + product.sku();
        } catch (DataAccessException e) {
            if (e.isDuplicateKey()) {
                return "DUPLIKAT " + product.sku();
            }
            throw e;
        }
    }

    static List<Integer> solution4(Connection con, int size) throws SQLException {
        List<Integer> sizes = new ArrayList<>();
        long lastId = 0;
        try (PreparedStatement ps = con.prepareStatement("SELECT id FROM products WHERE id > ? ORDER BY id LIMIT ?")) {
            while (true) {
                ps.setLong(1, lastId);
                ps.setInt(2, size);
                List<Long> page = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        page.add(rs.getLong("id"));
                    }
                }
                if (page.isEmpty()) {
                    return sizes;
                }
                sizes.add(page.size());
                lastId = page.get(page.size() - 1);
            }
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Brak wiersza to normalny wynik wyszukiwania, nie błąd. Optional wymusza na wywołującym obsłużenie braku
     *      (orElse, orElseThrow), a null łatwo przeoczyć (NullPointerException daleko od przyczyny).
     *   2. Żeby kod wyżej nie musiał deklarować ani znać SQLException/JDBC (warstwa danych jest wymienna). Trzeba
     *      zachować przyczynę (cause — stos wywołań i komunikat sterownika) oraz SQLState/kod błędu, po którym
     *      rozpoznaje się rodzaj problemu (np. 23505 = duplikat).
     *   3. Repozytorium otwiera WŁASNE połączenie, więc ma własną transakcję (i to w auto-commit). Rollback w serwisie
     *      nie cofnie tego INSERT-a. Połączenie trzeba przekazać z zewnątrz (konstruktor/parametr) — jak w lekcji.
     *   4. 2 — strona 2 przy rozmiarze 6 to OFFSET 12, więc zostają produkty o id 13 i 14.
     *   5. 51 (1 + 50). Zmniejszyć: JOIN w jednym zapytaniu albo drugie zapytanie z IN (?, ?, …) po wszystkich
     *      potrzebnych klientów naraz (razem 2 zapytania) i złożenie w Javie przez Map.
     *   6. Kolumny po numerach są zamienione: 2 = sku, 3 = name, więc nazwa trafi do sku i odwrotnie. Kompilator nie
     *      pomoże (oba to String). Czytaj po nazwie: rs.getString("sku"), rs.getString("name").
     *   7. Żeby atrapa zachowywała się jak prawdziwa baza (te same wyniki, te same wyjątki). Inaczej testy logiki
     *      na atrapie przechodzą, a na produkcji kod się sypie.
     */
    // </editor-fold>
}
