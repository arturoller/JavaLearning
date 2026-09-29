package helpers;

import helpers.model.Category;
import helpers.model.Customer;
import helpers.model.Department;
import helpers.model.Employee;
import helpers.model.Order;
import helpers.model.OrderLine;
import helpers.model.OrderStatus;
import helpers.model.Product;
import helpers.model.Student;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <pre>
 * TEMAT: SampleData — wspólne dane przykładowe dla wszystkich lekcji
 *        (sample = próbka, przykład; data = dane)
 *
 * W SKRÓCIE:
 *   Zamiast w każdej lekcji wymyślać listę produktów od nowa, bierzemy ją stąd: SampleData.products().
 *   Dzięki temu wyniki w różnych lekcjach są porównywalne (ten sam laptop, ci sami pracownicy),
 *   a komentarze „// WYNIK:” zgadzają się z tym, co widzisz po uruchomieniu.
 *
 * PUŁAPKA: każda metoda zwraca listę NIEMODYFIKOWALNĄ (List.of). Próba {@code list.add(...)} rzuci
 *   UnsupportedOperationException. Jeśli w lekcji chcesz listę do zmieniania, zrób kopię:
 *   {@code new ArrayList<>(SampleData.products())}. Szczegóły: t12_collections/Collections08ImmutableUnmodifiable.
 *
 * ŚCIĄGA: dane w liczbach (przydaje się do sprawdzania wyników w pamięci)
 *   products()  — 14 produktów w 5 kategoriach; 2 mają stan 0 (Smartfon X, Oliwa z oliwek); 2 kosztują 129.00
 *   employees() — 10 pracowników w 5 działach; 4 w IT; dwie osoby zarabiają po 9800 (remis);
 *                 dział LOGISTYKA nie ma nikogo (pusta grupa)
 *   customers() — 7 klientów; 2 VIP; 2 nie mają e-maila (null); Ewa Lis nie złożyła żadnego zamówienia
 *   orders()    — 10 zamówień od stycznia do kwietnia 2026, różne statusy
 *   students()  — 8 studentów; Henryk nie ma żadnych ocen (pusta lista)
 *
 * SŁÓWKA:
 *   products = produkty; employees = pracownicy; customers = klienci; orders = zamówienia; students = studenci;
 *   words = słowa; sentences = zdania; numbers = liczby; lines = linie; find = znajdź; by = według.
 * </pre>
 */
public final class SampleData {

    private SampleData() {
    }

    // =====================================================================================
    // PRODUKTY
    // =====================================================================================

    /** products = produkty. 14 sztuk, kolejność stała (tak jak niżej). */
    public static List<Product> products() {
        return List.of(
                product("ELE-001", "Laptop Pro 14", Category.ELEKTRONIKA, "5499.99", 7),
                product("ELE-002", "Smartfon X", Category.ELEKTRONIKA, "2999.00", 0),
                product("ELE-003", "Słuchawki BT", Category.ELEKTRONIKA, "349.90", 25),
                product("ELE-004", "Monitor 27 cali", Category.ELEKTRONIKA, "1299.00", 4),
                product("SPO-001", "Kawa ziarnista 1kg", Category.SPOZYWCZE, "64.99", 120),
                product("SPO-002", "Czekolada gorzka", Category.SPOZYWCZE, "7.49", 300),
                product("SPO-003", "Oliwa z oliwek", Category.SPOZYWCZE, "42.00", 0),
                product("KSI-001", "Czysty kod", Category.KSIAZKI, "79.00", 15),
                product("KSI-002", "Java. Podstawy", Category.KSIAZKI, "129.00", 9),
                product("KSI-003", "Wzorce projektowe", Category.KSIAZKI, "99.00", 3),
                product("ODZ-001", "Kurtka zimowa", Category.ODZIEZ, "459.00", 12),
                product("ODZ-002", "T-shirt bawełniany", Category.ODZIEZ, "49.99", 80),
                product("DOM-001", "Ekspres do kawy", Category.DOM, "1899.00", 2),
                product("DOM-002", "Lampka biurkowa", Category.DOM, "129.00", 18)
        );
    }

    /**
     * productBySku = produkt po kodzie SKU. Rzuca NoSuchElementException, gdy nie ma takiego produktu.
     * (NoSuchElement = „nie ma takiego elementu”)
     */
    public static Product productBySku(String sku) {
        return products().stream()
                .filter(p -> p.sku().equals(sku))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Brak produktu o SKU: " + sku));
    }

    // Prywatna metoda pomocnicza — skraca zapis. Cenę podajemy jako String, bo new BigDecimal("64.99")
    // jest DOKŁADNE, a new BigDecimal(64.99) (z double) już nie! Szczegóły: t15_numbers/Numbers01BigDecimal.
    private static Product product(String sku, String name, Category category, String price, int stock) {
        return new Product(sku, name, category, new BigDecimal(price), stock);
    }

    // =====================================================================================
    // PRACOWNICY
    // =====================================================================================

    /** employees = pracownicy. 10 osób w 5 działach. */
    public static List<Employee> employees() {
        return List.of(
                new Employee("Anna Nowak", Department.IT, 14500, 34, LocalDate.of(2018, 3, 1), List.of("Java", "Spring", "SQL")),
                new Employee("Piotr Kowalski", Department.IT, 9800, 27, LocalDate.of(2022, 6, 15), List.of("Java", "Docker")),
                new Employee("Katarzyna Wiśniewska", Department.HR, 7200, 41, LocalDate.of(2015, 1, 10), List.of("Rekrutacja", "Excel")),
                new Employee("Tomasz Wójcik", Department.SPRZEDAZ, 8900, 38, LocalDate.of(2019, 9, 1), List.of("Negocjacje", "Excel", "CRM")),
                new Employee("Magdalena Kamińska", Department.KSIEGOWOSC, 8100, 45, LocalDate.of(2012, 4, 20), List.of("Excel", "SAP")),
                new Employee("Michał Lewandowski", Department.IT, 17200, 45, LocalDate.of(2014, 11, 3), List.of("Java", "Kotlin", "AWS", "SQL")),
                new Employee("Agnieszka Zielińska", Department.MARKETING, 7600, 29, LocalDate.of(2021, 2, 1), List.of("SEO", "Canva")),
                new Employee("Krzysztof Szymański", Department.SPRZEDAZ, 11200, 50, LocalDate.of(2010, 7, 12), List.of("Negocjacje", "CRM")),
                new Employee("Ewa Woźniak", Department.IT, 12100, 31, LocalDate.of(2020, 1, 7), List.of("Python", "SQL", "Docker")),
                new Employee("Paweł Dąbrowski", Department.MARKETING, 9800, 36, LocalDate.of(2017, 5, 22), List.of("SEO", "Google Ads", "Excel"))
        );
    }

    // =====================================================================================
    // KLIENCI I ZAMÓWIENIA
    // =====================================================================================

    /**
     * customers = klienci. 7 osób; Maria Nowak i Ola Pawlak nie mają e-maila (null);
     * Ewa Lis nie ma żadnego zamówienia (przydaje się do pokazania „klientów bez zamówień”).
     */
    public static List<Customer> customers() {
        return List.of(
                new Customer(1, "Jan Kowalski", "Warszawa", "jan@example.com", true),
                new Customer(2, "Maria Nowak", "Kraków", null, false),
                new Customer(3, "Adam Mazur", "Gdańsk", "adam.mazur@example.com", false),
                new Customer(4, "Zofia Krawczyk", "Warszawa", "zofia@example.com", true),
                new Customer(5, "Ola Pawlak", "Poznań", null, false),
                new Customer(6, "Marek Król", "Kraków", "marek@example.com", false),
                new Customer(7, "Ewa Lis", "Wrocław", "ewa.lis@example.com", false)
        );
    }

    /**
     * orders = zamówienia. 10 sztuk.
     * <p>
     * Najpierw budujemy mapę id → klient ({@code Map<Long, Customer>}), żeby wygodnie wybierać klientów po numerze.
     * Collectors.toMap(klucz, wartość) — t16_streams/Streams10CollectorsToMap.
     * Function.identity() = „funkcja, która zwraca to, co dostała” (sam obiekt klienta jako wartość).
     */
    public static List<Order> orders() {
        Map<Long, Customer> c = customers().stream()
                .collect(Collectors.toMap(Customer::id, Function.identity()));

        return List.of(
                order("ZAM-001", c.get(1L), LocalDate.of(2026, 1, 5), OrderStatus.DOSTARCZONE, line("ELE-001", 1), line("ELE-003", 2)),
                order("ZAM-002", c.get(2L), LocalDate.of(2026, 1, 12), OrderStatus.DOSTARCZONE, line("SPO-001", 3), line("SPO-002", 10)),
                order("ZAM-003", c.get(1L), LocalDate.of(2026, 2, 2), OrderStatus.WYSLANE, line("KSI-001", 1), line("KSI-002", 1), line("KSI-003", 1)),
                order("ZAM-004", c.get(3L), LocalDate.of(2026, 2, 14), OrderStatus.ANULOWANE, line("ELE-002", 1)),
                order("ZAM-005", c.get(4L), LocalDate.of(2026, 2, 20), OrderStatus.OPLACONE, line("DOM-001", 1), line("SPO-001", 2)),
                order("ZAM-006", c.get(5L), LocalDate.of(2026, 3, 1), OrderStatus.NOWE, line("ODZ-002", 3), line("ODZ-001", 1)),
                order("ZAM-007", c.get(4L), LocalDate.of(2026, 3, 3), OrderStatus.DOSTARCZONE, line("ELE-004", 2), line("DOM-002", 1)),
                order("ZAM-008", c.get(6L), LocalDate.of(2026, 3, 15), OrderStatus.WYSLANE, line("KSI-002", 2), line("SPO-002", 5)),
                order("ZAM-009", c.get(2L), LocalDate.of(2026, 3, 28), OrderStatus.NOWE, line("ELE-003", 1)),
                order("ZAM-010", c.get(1L), LocalDate.of(2026, 4, 2), OrderStatus.OPLACONE, line("SPO-003", 2), line("KSI-001", 1))
        );
    }

    // OrderLine... lines = varargs (zmienna liczba argumentów): można podać 1, 2, 3... pozycji.
    // W środku metody „lines” jest zwykłą tablicą OrderLine[]. (t03_arrays/Arrays05Varargs)
    private static Order order(String id, Customer customer, LocalDate date, OrderStatus status, OrderLine... lines) {
        return new Order(id, customer, date, status, List.of(lines));
    }

    private static OrderLine line(String sku, int quantity) {
        return new OrderLine(productBySku(sku), quantity);
    }

    // =====================================================================================
    // STUDENCI
    // =====================================================================================

    /** students = studenci. 8 osób; Henryk ma pustą listę ocen (pułapka ze średnią). */
    public static List<Student> students() {
        return List.of(
                new Student("Ala", "Warszawa", 1, List.of(5, 4, 5, 3)),
                new Student("Bartek", "Kraków", 2, List.of(3, 3, 4, 2)),
                new Student("Celina", "Gdańsk", 1, List.of(5, 5, 5, 4)),
                new Student("Darek", "Warszawa", 3, List.of(2, 3, 2, 3)),
                new Student("Ela", "Kraków", 2, List.of(4, 4, 5, 5)),
                new Student("Filip", "Poznań", 3, List.of(3, 4, 3, 4)),
                new Student("Gosia", "Warszawa", 2, List.of(5, 4, 4, 5)),
                new Student("Henryk", "Gdańsk", 1, List.of())
        );
    }

    // =====================================================================================
    // PROSTE DANE: SŁOWA, ZDANIA, LICZBY, CSV
    // =====================================================================================

    /** words = słowa. Są powtórzenia (java ×3, stream ×2) — do ćwiczeń z distinct i liczeniem wystąpień. */
    public static List<String> words() {
        return List.of("java", "stream", "lambda", "kolekcja", "java", "mapa",
                "lista", "stream", "java", "optional", "rekord", "enum");
    }

    /** sentences = zdania. Do ćwiczeń: zdania → słowa (flatMap), liczenie słów, najdłuższe słowo. */
    public static List<String> sentences() {
        return List.of(
                "Java jest językiem obiektowym",
                "Stream to nie jest kolekcja",
                "Lambda to anonimowa funkcja",
                "Kolekcja przechowuje elementy"
        );
    }

    /** numbers = liczby. 12 liczb całkowitych, z powtórzeniami (3 i 8 występują dwa razy). */
    public static List<Integer> numbers() {
        return List.of(5, 3, 8, 1, 9, 2, 7, 3, 10, 6, 4, 8);
    }

    /**
     * salesCsvLines = linie pliku CSV ze sprzedażą (CSV = Comma-Separated Values = wartości rozdzielone przecinkami).
     * <p>
     * Nagłówek + 8 poprawnych wierszy + 4 CELOWO BŁĘDNE (za mało pól, tekst zamiast liczby, pusta linia, zła data).
     * Prawdziwe pliki prawie zawsze mają błędne wiersze — dobry parser je pomija i liczy, zamiast się wysypać.
     * Używane w lekcjach o plikach (t18_io_files/Io04Csv) i w t28_capstone/Capstone02SalesReport.
     */
    public static List<String> salesCsvLines() {
        return List.of(
                "data,sku,produkt,kategoria,ilosc,cena",
                "2026-05-04,KSI-001,Czysty kod,KSIAZKI,2,79.00",
                "2026-05-04,SPO-001,Kawa ziarnista 1kg,SPOZYWCZE,3,64.99",
                "2026-05-05,ELE-003,Słuchawki BT,ELEKTRONIKA,1,349.90",
                "2026-05-05,ODZ-002,T-shirt bawełniany,ODZIEZ,4,49.99",
                "2026-05-05,KSI-002,Java. Podstawy,KSIAZKI,dwa,129.00",   // BŁĄD: tekst zamiast liczby
                "2026-05-06,SPO-002,Czekolada gorzka",                    // BŁĄD: za mało pól
                "",                                                       // BŁĄD: pusta linia
                "2026-13-06,DOM-002,Lampka biurkowa,DOM,1,129.00",        // BŁĄD: miesiąc 13 nie istnieje
                "2026-05-06,ELE-001,Laptop Pro 14,ELEKTRONIKA,1,5499.99",
                "2026-05-07,SPO-002,Czekolada gorzka,SPOZYWCZE,10,7.49",
                "2026-05-07,KSI-003,Wzorce projektowe,KSIAZKI,1,99.00",
                "2026-05-07,DOM-001,Ekspres do kawy,DOM,1,1899.00"
        );
    }
}
