package t35_capstone;

import helpers.Check;
import helpers.TempDir;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Projekt 1 — wypożyczalnia książek
 *        (library = biblioteka; loan = wypożyczenie; reservation = rezerwacja)
 *
 * W SKRÓCIE:
 *   Budujemy kompletną, małą aplikację: książki, czytelnicy, wypożyczenia z terminem zwrotu, opłaty za
 *   spóźnienie w BigDecimal, kolejka rezerwacji, polecenia tekstowe (jak w konsoli) i zapis stanu do pliku
 *   z ponownym odczytem. Wszystko w jednym pliku, ale w warstwach: model → logika → brzegi.
 *
 * ANALOGIA:
 *   Biblioteka osiedlowa. Bibliotekarka (Library) pilnuje regulaminu (LibraryPolicy), zapisuje w zeszycie,
 *   kto co wziął (Loan), prowadzi listę chętnych na rozchwytywane tytuły (kolejka rezerwacji), a wieczorem
 *   przepisuje zeszyt na czysto do segregatora (zapis do pliku), żeby rano odtworzyć stan.
 *
 * JAK TO DZIAŁA:
 *   polecenie tekstowe ──► CommandShell (parsowanie) ──► Library (reguły, Clock) ──► LibraryState
 *                                                                │                      │
 *                                                       wyjątki domenowe       LibraryStore (plik tekstowy)
 *   Wykorzystane działy kursu:
 *   • rekordy z walidacją w konstruktorze kompaktowym       → t09_records/Records02Constructors
 *   • enum z polami (limit książek dla typu czytelnika)     → t08_enums/Enums02FieldsMethods
 *   • klasy zamknięte (sealed) dla wyjątków i poleceń       → t07_inheritance_polymorphism/Inherit07SealedClasses
 *   • własne wyjątki domenowe                               → t10_exceptions/Exceptions05CustomExceptions
 *   • mapy, kolejka Deque, Comparator                       → t12_collections/Collections06QueuesDeques
 *   • strumienie, Optional                                  → t16_streams/Streams19Recipes, t14_optional/Optional03BestPractices
 *   • pieniądze w BigDecimal                                → t15_numbers/Numbers01BigDecimal
 *   • Clock, Period, ChronoUnit                             → t17_datetime/DateTime02PeriodDuration
 *   • pliki tekstowe, UTF-8                                 → t18_io_files/Io04Csv
 *   • zegar-atrapa w testach                                → t25_testing/Testing03TestableDesign
 *
 * SŁÓWKA:
 *   book = książka; member = członek (tu: czytelnik); due date = termin zwrotu; late fee = opłata za
 *   spóźnienie; borrow = wypożyczyć; return = zwrócić; reserve = zarezerwować; queue = kolejka;
 *   policy = regulamin; store = magazyn (tu: miejsce zapisu); snapshot = migawka stanu; shell = powłoka.
 *
 * ZOBACZ TEŻ: t27_clean_code_pitfalls/CleanCode03Architecture (warstwy i zależności),
 *             t34_toward_spring/Spring02Layers (ta sama aplikacja w warstwach Springa),
 *             t22_design_patterns/Patterns09Command (polecenia jako obiekty)
 * </pre>
 */
public class Capstone01Library {

    /** Stała data startowa symulacji: poniedziałek 2 marca 2026, 9:00 w Warszawie. */
    static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");                       // ZoneId = identyfikator strefy
    static final Instant START = LocalDateTime.of(2026, 3, 2, 9, 0).atZone(WARSAW).toInstant();

    public static void main(String[] args) {
        title("Capstone01 — wypożyczalnia książek");

        requirements();      // requirements = wymagania
        domainModel();       // domain model = model dziedziny
        borrowingRules();    // borrowing rules = reguły wypożyczania
        dueDatesAndFees();   // due dates and fees = terminy i opłaty
        searching();         // searching = wyszukiwanie
        reservations();      // reservations = rezerwacje
        commandShell();      // command shell = powłoka poleceń
        persistence();       // persistence = trwałość (zapis i odczyt)
        behaviourTests();    // behaviour tests = testy zachowania
        whatNext();          // what next = co dalej
        exercises();         // exercises = ćwiczenia
    }

    // =================================================================================================
    // MODEL DZIEDZINY — rekordy, enum, wyjątki (niezmienne wartości, walidacja na wejściu)
    // =================================================================================================

    /** Typ czytelnika decyduje o limicie jednocześnie wypożyczonych książek. */
    enum MemberType {
        STANDARD(2), PREMIUM(4);

        private final int maxLoans;                                                 // max loans = maks. wypożyczeń

        MemberType(int maxLoans) {
            this.maxLoans = maxLoans;
        }

        int maxLoans() {
            return maxLoans;
        }
    }

    /** Książka. Konstruktor kompaktowy sprawdza dane, zanim obiekt w ogóle powstanie. */
    record Book(String id, String title, String author, int year) {
        Book {
            requireText(id, "id");
            requireText(title, "tytuł");
            requireText(author, "autor");
            title = title.strip();                                                  // strip = obetnij białe znaki (Java 11+)
            author = author.strip();
            if (year < 1450 || year > 2100) {
                throw new IllegalArgumentException("nierealny rok wydania: " + year);
            }
        }

        /** matches = pasuje: fraza w tytule albo autorze, bez względu na wielkość liter. */
        boolean matches(String phrase) {
            String needle = phrase.toLowerCase(Locale.ROOT);                        // needle = igła (szukany tekst)
            return title.toLowerCase(Locale.ROOT).contains(needle)
                    || author.toLowerCase(Locale.ROOT).contains(needle);
        }

        @Override
        public String toString() {
            return id + " „" + title + "” (" + author + ", " + year + ")";
        }
    }

    /** Czytelnik. */
    record Member(String id, String name, MemberType type) {
        Member {
            requireText(id, "id");
            requireText(name, "imię i nazwisko");
            Objects.requireNonNull(type, "typ czytelnika");                         // requireNonNull = wymagaj nie-null
        }
    }

    /** Wypożyczenie: która książka, kto, kiedy wzięta i do kiedy oddać. */
    record Loan(String bookId, String memberId, LocalDate borrowedOn, LocalDate dueOn) {
        Loan {
            requireText(bookId, "id książki");
            requireText(memberId, "id czytelnika");
            Objects.requireNonNull(borrowedOn, "data wypożyczenia");
            Objects.requireNonNull(dueOn, "termin zwrotu");
            if (!dueOn.isAfter(borrowedOn)) {                                       // isAfter = jest po
                throw new IllegalArgumentException("termin zwrotu musi być po dacie wypożyczenia");
            }
        }

        /** isOverdue = jest przeterminowane: dziś jest już PO terminie (w dniu terminu jeszcze nie). */
        boolean isOverdue(LocalDate today) {
            return today.isAfter(dueOn);
        }

        /** daysLate = dni spóźnienia; nigdy ujemne. */
        long daysLate(LocalDate returnedOn) {
            return Math.max(0, ChronoUnit.DAYS.between(dueOn, returnedOn));         // between = liczba dni między
        }
    }

    /** Regulamin jako wartość: łatwo podmienić w teście albo wczytać z konfiguracji. */
    record LibraryPolicy(Period loanPeriod, BigDecimal feePerDay, BigDecimal maxFee) {
        static final LibraryPolicy DEFAULT =
                new LibraryPolicy(Period.ofDays(14), new BigDecimal("0.50"), new BigDecimal("20.00"));

        LibraryPolicy {
            Objects.requireNonNull(loanPeriod, "okres wypożyczenia");
            if (feePerDay.signum() < 0 || maxFee.signum() < 0) {                    // signum = znak liczby (-1, 0, 1)
                throw new IllegalArgumentException("opłaty nie mogą być ujemne");
            }
        }

        /** lateFee = opłata za spóźnienie: stawka × dni, ale nie więcej niż maxFee; zawsze 2 miejsca po przecinku. */
        BigDecimal lateFee(long daysLate) {
            return feePerDay.multiply(BigDecimal.valueOf(daysLate))
                    .min(maxFee)
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }

    /**
     * Wyjątki domenowe: zamknięta rodzina (sealed), więc wiadomo, jakie błędy biznesowe w ogóle istnieją.
     * Dziedziczą z RuntimeException — to błędy reguł, a nie awarie, których wywołujący MUSI się spodziewać.
     */
    abstract static sealed class LibraryException extends RuntimeException
            permits NotFoundException, BookUnavailableException, BorrowingRuleException {
        private static final long serialVersionUID = 1L;                            // wymagane przez -Xlint:serial

        LibraryException(String message) { super(message); }
    }

    /** Nie ma takiej książki / czytelnika / wypożyczenia. */
    static final class NotFoundException extends LibraryException {
        private static final long serialVersionUID = 1L;

        NotFoundException(String message) { super(message); }
    }

    /** Książka istnieje, ale teraz nie można jej wziąć. */
    static final class BookUnavailableException extends LibraryException {
        private static final long serialVersionUID = 1L;

        BookUnavailableException(String message) { super(message); }
    }

    /** Czytelnik łamie regulamin (limit, przeterminowana książka, podwójna rezerwacja). */
    static final class BorrowingRuleException extends LibraryException {
        private static final long serialVersionUID = 1L;

        BorrowingRuleException(String message) { super(message); }
    }

    static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {                                     // isBlank = pusty lub same spacje (Java 11+)
            throw new IllegalArgumentException("pole „" + field + "” nie może być puste");
        }
    }

    // =================================================================================================
    // LOGIKA — Library: jedyne miejsce ze zmiennym stanem, ukrytym za metodami
    // =================================================================================================

    /** Migawka stanu: niezmienne kopie list, gotowe do zapisu i porównania przez equals. */
    record LibraryState(List<Book> books, List<Member> members, List<Loan> loans,
                        Map<String, List<String>> reservations) {
        LibraryState {
            books = List.copyOf(books);                                             // copyOf = niezmienna kopia (Java 10+)
            members = List.copyOf(members);
            loans = List.copyOf(loans);
            Map<String, List<String>> sorted = new TreeMap<>();
            reservations.forEach((bookId, queue) -> sorted.put(bookId, List.copyOf(queue)));
            reservations = Collections.unmodifiableMap(sorted);                     // unmodifiableMap = widok tylko do odczytu
        }
    }

    static final class Library {
        private static final Comparator<Book> BY_YEAR_THEN_TITLE =
                Comparator.comparingInt(Book::year).thenComparing(Book::title);     // thenComparing = następnie porównaj

        private final LibraryPolicy policy;
        private final Clock clock;
        private final Map<String, Book> books = new TreeMap<>();
        private final Map<String, Member> members = new TreeMap<>();
        private final Map<String, Loan> loansByBook = new TreeMap<>();
        private final Map<String, Deque<String>> reservations = new TreeMap<>();

        Library(LibraryPolicy policy, Clock clock) {
            this.policy = Objects.requireNonNull(policy);
            this.clock = Objects.requireNonNull(clock);
        }

        void addBook(Book book) {
            if (books.putIfAbsent(book.id(), book) != null) {                       // putIfAbsent = wstaw, jeśli brak
                throw new IllegalArgumentException("książka " + book.id() + " już istnieje");
            }
        }

        void addMember(Member member) {
            if (members.putIfAbsent(member.id(), member) != null) {
                throw new IllegalArgumentException("czytelnik " + member.id() + " już istnieje");
            }
        }

        Loan borrow(String memberId, String bookId) {
            Member member = member(memberId);
            book(bookId);
            if (loansByBook.containsKey(bookId)) {
                throw new BookUnavailableException("książka " + bookId + " jest wypożyczona");
            }
            Deque<String> queue = reservations.getOrDefault(bookId, new ArrayDeque<>());
            String firstInQueue = queue.peekFirst();                                // peekFirst = podejrzyj pierwszy (null, gdy pusto)
            if (firstInQueue != null && !firstInQueue.equals(memberId)) {
                throw new BookUnavailableException("książka " + bookId + " czeka na " + firstInQueue);
            }
            List<Loan> current = loansOf(memberId);
            LocalDate today = today();
            if (current.stream().anyMatch(loan -> loan.isOverdue(today))) {         // anyMatch = czy którykolwiek pasuje
                throw new BorrowingRuleException(memberId + " ma przeterminowaną książkę");
            }
            if (current.size() >= member.type().maxLoans()) {
                throw new BorrowingRuleException("limit " + member.type().maxLoans() + " książek dla " + memberId);
            }
            if (firstInQueue != null) {
                queue.pollFirst();                                                  // pollFirst = zdejmij pierwszy
                if (queue.isEmpty()) {
                    reservations.remove(bookId);
                }
            }
            Loan loan = new Loan(bookId, memberId, today, today.plus(policy.loanPeriod()));
            loansByBook.put(bookId, loan);
            return loan;
        }

        /** Zwraca opłatę za spóźnienie (0.00, gdy w terminie). */
        BigDecimal returnBook(String bookId) {
            Loan loan = loansByBook.remove(bookId);
            if (loan == null) {
                throw new NotFoundException("książka " + bookId + " nie jest wypożyczona");
            }
            return policy.lateFee(loan.daysLate(today()));
        }

        /** Zwraca miejsce w kolejce (1 = następny w kolejce). */
        int reserve(String memberId, String bookId) {
            member(memberId);
            book(bookId);
            Loan loan = loansByBook.get(bookId);
            if (loan == null) {
                throw new BorrowingRuleException("książka " + bookId + " jest dostępna — wypożycz ją");
            }
            if (loan.memberId().equals(memberId)) {
                throw new BorrowingRuleException(memberId + " już ma książkę " + bookId);
            }
            Deque<String> queue = reservations.computeIfAbsent(bookId, id -> new ArrayDeque<>());
            if (queue.contains(memberId)) {
                throw new BorrowingRuleException(memberId + " już czeka na " + bookId);
            }
            queue.addLast(memberId);                                                // addLast = dodaj na koniec
            return queue.size();
        }

        Optional<Book> findBook(String bookId) {
            return Optional.ofNullable(books.get(bookId));                          // ofNullable = pusty, gdy null
        }

        List<Book> search(String phrase) {
            return books.values().stream()
                    .filter(book -> book.matches(phrase))
                    .sorted(BY_YEAR_THEN_TITLE)
                    .toList();                                                      // toList = do listy (Java 16+)
        }

        List<Book> availableBooks() {
            return books.values().stream()
                    .filter(book -> !loansByBook.containsKey(book.id()))
                    .sorted(BY_YEAR_THEN_TITLE)
                    .toList();
        }

        List<Loan> loansOf(String memberId) {
            return loansByBook.values().stream()
                    .filter(loan -> loan.memberId().equals(memberId))
                    .sorted(Comparator.comparing(Loan::dueOn).thenComparing(Loan::bookId))
                    .toList();
        }

        LocalDate today() {
            return LocalDate.now(clock);                                            // now(clock) = „teraz” według podanego zegara
        }

        LibraryState snapshot() {
            Map<String, List<String>> queues = new TreeMap<>();
            reservations.forEach((bookId, queue) -> queues.put(bookId, List.copyOf(queue)));
            return new LibraryState(List.copyOf(books.values()), List.copyOf(members.values()),
                    List.copyOf(loansByBook.values()), queues);
        }

        /** restore = odtwórz: fabryka budująca bibliotekę z migawki (np. po odczycie z pliku). */
        static Library restore(LibraryState state, LibraryPolicy policy, Clock clock) {
            Library library = new Library(policy, clock);
            state.books().forEach(library::addBook);
            state.members().forEach(library::addMember);
            state.loans().forEach(loan -> library.loansByBook.put(loan.bookId(), loan));
            state.reservations().forEach((bookId, queue) -> library.reservations.put(bookId, new ArrayDeque<>(queue)));
            return library;
        }

        private Member member(String memberId) {
            Member member = members.get(memberId);
            if (member == null) {
                throw new NotFoundException("brak czytelnika " + memberId);
            }
            return member;
        }

        private Book book(String bookId) {
            return findBook(bookId).orElseThrow(() -> new NotFoundException("brak książki " + bookId)); // orElseThrow = albo rzuć
        }
    }

    /**
     * Zegar-atrapa (fake clock): zwykły {@code Clock}, który przesuwamy ręcznie. Biblioteka nie wie, że to atrapa —
     * dostaje abstrakcję {@code Clock}, więc w produkcji podamy {@code Clock.systemDefaultZone()}.
     */
    static final class FakeClock extends Clock {
        private Instant now;
        private final ZoneId zone;

        FakeClock(Instant start, ZoneId zone) {
            this.now = start;
            this.zone = zone;
        }

        void advanceDays(int days) {                                                // advance = przesuń do przodu
            now = now.plus(days, ChronoUnit.DAYS);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId newZone) {                                     // withZone = z inną strefą
            return new FakeClock(now, newZone);
        }

        @Override
        public Instant instant() {                                                  // instant = chwila na osi czasu
            return now;
        }
    }

    static Library sampleLibrary(Clock clock) {
        Library library = new Library(LibraryPolicy.DEFAULT, clock);
        library.addBook(new Book("K1", "Pan Tadeusz", "Adam Mickiewicz", 1834));
        library.addBook(new Book("K2", "Lalka", "Bolesław Prus", 1890));
        library.addBook(new Book("K3", "Quo vadis", "Henryk Sienkiewicz", 1896));
        library.addBook(new Book("K4", "Solaris", "Stanisław Lem", 1961));
        library.addBook(new Book("K5", "Cyberiada", "Stanisław Lem", 1965));
        library.addBook(new Book("K6", "Faraon", "Bolesław Prus", 1897));
        library.addMember(new Member("C1", "Ala Nowak", MemberType.STANDARD));
        library.addMember(new Member("C2", "Bartek Lis", MemberType.PREMIUM));
        library.addMember(new Member("C3", "Celina Mazur", MemberType.STANDARD));
        return library;
    }

    // =================================================================================================
    // 1. WYMAGANIA I PRZYKŁADY
    // =================================================================================================

    /**
     * 1. Zanim napiszemy kod, spisujemy reguły jak w umowie z klientem. Każda reguła stanie się metodą
     * i testem: wypożyczenie na 14 dni, opłata 0,50 zł za dzień spóźnienia (najwyżej 20 zł), limit książek
     * zależny od typu czytelnika, brak nowych wypożyczeń przy przeterminowanej książce, kolejka chętnych.
     */
    static void requirements() {
        section("1. Wymagania i przykłady");

        LibraryPolicy policy = LibraryPolicy.DEFAULT;
        show("okres wypożyczenia", policy.loanPeriod());
        // WYNIK: okres wypożyczenia → P14D    ← zapis ISO-8601: 14 dni
        show("opłata za dzień / maksimum", policy.feePerDay() + " zł / " + policy.maxFee() + " zł");
        // WYNIK: opłata za dzień / maksimum → 0.50 zł / 20.00 zł
        for (MemberType type : MemberType.values()) {
            show("limit dla " + type, type.maxLoans());
        }
        // WYNIK: limit dla STANDARD → 2
        // WYNIK: limit dla PREMIUM → 4

        // Przykłady (przyjęte z klientem, potem zamienione w testy w sekcji 9):
        note("wypożyczenie 2 marca → zwrot do 16 marca");
        // WYNIK: ℹ wypożyczenie 2 marca → zwrot do 16 marca
        note("zwrot 22 marca → 6 dni spóźnienia → 3.00 zł");
        // WYNIK: ℹ zwrot 22 marca → 6 dni spóźnienia → 3.00 zł

        // DOBRA PRAKTYKA: przykłady z liczbami spisane PRZED kodem. Dlaczego? Wymagania słowne bywają dwuznaczne
        //   („14 dni” — liczone od dziś czy od jutra?), a konkretny przykład rozstrzyga spór i od razu jest testem.
    }

    // =================================================================================================
    // 2. MODEL DZIEDZINY
    // =================================================================================================

    /**
     * 2. Rekordy są niezmienne i same pilnują poprawności. Błędny obiekt nie może powstać, więc reszta kodu
     * nie musi go sprawdzać na każdym kroku („parsuj, nie waliduj w kółko”).
     */
    static void domainModel() {
        section("2. Model dziedziny");

        Book book = new Book("K4", "  Solaris ", "Stanisław Lem", 1961);
        show("książka", book);
        // WYNIK: książka → K4 „Solaris” (Stanisław Lem, 1961)    ← tytuł obcięty w konstruktorze
        show("pasuje do „LEM”?", book.matches("LEM"));
        // WYNIK: pasuje do „LEM”? → true

        expectThrows("pusty tytuł", () -> new Book("K9", " ", "Ktoś", 2000));
        // WYNIK: ✔ pusty tytuł → rzucono IllegalArgumentException: pole „tytuł” nie może być puste
        expectThrows("rok 3000", () -> new Book("K9", "Przyszłość", "Ktoś", 3000));
        // WYNIK: ✔ rok 3000 → rzucono IllegalArgumentException: nierealny rok wydania: 3000
        LocalDate day = LocalDate.of(2026, 3, 2);
        expectThrows("termin = data wypożyczenia", () -> new Loan("K1", "C1", day, day));
        // WYNIK: ✔ termin = data wypożyczenia → rzucono IllegalArgumentException: termin zwrotu musi być po dacie wypożyczenia

        // PUŁAPKA: toLowerCase() BEZ Locale. Dlaczego groźne? Na komputerze z językiem tureckim "TITLE".toLowerCase()
        //   daje „tıtle” (i bez kropki) i wyszukiwanie przestaje działać. W logice zawsze Locale.ROOT.

        // DOBRA PRAKTYKA: identyfikatory jako String "K1", a nie indeks w liście. Dlaczego? Indeks zmienia się po
        //   usunięciu elementu, a identyfikator jest stały — można go zapisać w pliku i odczytać za rok.
    }

    // =================================================================================================
    // 3. REGUŁY WYPOŻYCZANIA
    // =================================================================================================

    /**
     * 3. Library to jedyny obiekt ze zmiennym stanem. Każda reguła to jeden {@code if} z czytelnym wyjątkiem
     * domenowym. Kolejność sprawdzeń ma znaczenie: najpierw „czy istnieje”, potem „czy wolna”, na końcu limity.
     */
    static void borrowingRules() {
        section("3. Reguły wypożyczania");

        Library library = sampleLibrary(new FakeClock(START, WARSAW));
        Loan loan = library.borrow("C1", "K4");
        show("wypożyczenie", loan);
        // WYNIK: wypożyczenie → Loan[bookId=K4, memberId=C1, borrowedOn=2026-03-02, dueOn=2026-03-16]
        library.borrow("C1", "K5");

        expectThrows("trzecia książka STANDARD", () -> library.borrow("C1", "K1"));
        // WYNIK: ✔ trzecia książka STANDARD → rzucono BorrowingRuleException: limit 2 książek dla C1
        expectThrows("zajęta książka", () -> library.borrow("C2", "K4"));
        // WYNIK: ✔ zajęta książka → rzucono BookUnavailableException: książka K4 jest wypożyczona
        expectThrows("nieznany czytelnik", () -> library.borrow("C9", "K1"));
        // WYNIK: ✔ nieznany czytelnik → rzucono NotFoundException: brak czytelnika C9
        expectThrows("zwrot niewypożyczonej", () -> library.returnBook("K2"));
        // WYNIK: ✔ zwrot niewypożyczonej → rzucono NotFoundException: książka K2 nie jest wypożyczona

        show("wypożyczenia C1", library.loansOf("C1").stream().map(Loan::bookId).toList());
        // WYNIK: wypożyczenia C1 → [K4, K5]

        // DOBRA PRAKTYKA: osobne klasy wyjątków zamiast jednego IllegalStateException. Dlaczego? Warstwa wyżej
        //   (konsola, REST) może je rozróżnić: NotFoundException → 404, BookUnavailableException → 409.

        // PUŁAPKA: nieudane wypożyczenie nie może zmienić stanu. Dlatego WSZYSTKIE sprawdzenia są przed
        //   pierwszą modyfikacją (pollFirst, put). Gdyby kolejka była skracana wcześniej, błąd limitu
        //   „zjadłby” czyjąś rezerwację.
    }

    // =================================================================================================
    // 4. TERMINY I OPŁATY (Clock, Period, BigDecimal)
    // =================================================================================================

    /**
     * 4. Czas wstrzykujemy jako {@code Clock}. W teście przesuwamy zegar-atrapę o dni i sprawdzamy opłaty —
     * bez czekania i bez zależności od dzisiejszej daty.
     */
    static void dueDatesAndFees() {
        section("4. Terminy i opłaty");

        FakeClock clock = new FakeClock(START, WARSAW);
        Library library = sampleLibrary(clock);
        library.borrow("C2", "K1");
        library.borrow("C2", "K2");
        library.borrow("C2", "K3");

        clock.advanceDays(14);
        show("zwrot K1 w dniu terminu", library.returnBook("K1") + " zł");
        // WYNIK: zwrot K1 w dniu terminu → 0.00 zł
        clock.advanceDays(6);
        show("zwrot K2 po 6 dniach spóźnienia", library.returnBook("K2") + " zł");
        // WYNIK: zwrot K2 po 6 dniach spóźnienia → 3.00 zł
        clock.advanceDays(100);
        show("zwrot K3 po 106 dniach spóźnienia", library.returnBook("K3") + " zł");
        // WYNIK: zwrot K3 po 106 dniach spóźnienia → 20.00 zł    ← limit z regulaminu

        // PUŁAPKA: Period.ofMonths(1) zamiast ofDays(30). Dlaczego różnica? 31 stycznia + 1 miesiąc = 28 lutego,
        //   a 31 stycznia + 30 dni = 2 marca. Regulamin mówi „14 dni”, więc ofDays(14).
        show("31 stycznia + 1 miesiąc", LocalDate.of(2026, 1, 31).plus(Period.ofMonths(1)));
        // WYNIK: 31 stycznia + 1 miesiąc → 2026-02-28

        // DOBRA PRAKTYKA: stawki jako BigDecimal tworzony z tekstu ("0.50"), a nie z double. Dlaczego?
        //   new BigDecimal(0.1) to 0.1000000000000000055511151231257827... — grosze by się rozjechały.
    }

    // =================================================================================================
    // 5. WYSZUKIWANIE (strumienie, Comparator, Optional)
    // =================================================================================================

    /**
     * 5. Wyszukiwanie to czyste zapytania: nie zmieniają stanu, zwracają nowe, niezmienne listy.
     * Brak wyniku dla jednego obiektu wyrażamy przez {@code Optional}, a dla wielu — pustą listą.
     */
    static void searching() {
        section("5. Wyszukiwanie");

        Library library = sampleLibrary(new FakeClock(START, WARSAW));
        library.borrow("C1", "K6");

        showEach("fraza „prus”", library.search("prus"));
        // WYNIK: fraza „prus” (liczba elementów: 2):
        // WYNIK: • K2 „Lalka” (Bolesław Prus, 1890)
        // WYNIK: • K6 „Faraon” (Bolesław Prus, 1897)
        show("dostępne (id)", library.availableBooks().stream().map(Book::id).toList());
        // WYNIK: dostępne (id) → [K1, K2, K3, K4, K5]
        show("K3 — tytuł", library.findBook("K3").map(Book::title).orElse("brak"));
        // WYNIK: K3 — tytuł → Quo vadis
        show("K9 — tytuł", library.findBook("K9").map(Book::title).orElse("brak"));
        // WYNIK: K9 — tytuł → brak

        String authors = library.search("").stream()
                .map(Book::author)
                .distinct()                                                         // distinct = bez powtórzeń
                .sorted()
                .collect(Collectors.joining(", "));                                 // joining = sklej z separatorem
        show("autorzy", authors);
        // WYNIK: autorzy → Adam Mickiewicz, Bolesław Prus, Henryk Sienkiewicz, Stanisław Lem

        // PUŁAPKA: sorted() na polskich napisach porównuje kody Unicode — „Łucja” trafiłaby za „Zenona”.
        //   Tu nazwiska zaczynają się od liter ASCII, ale dla ogólnego przypadku użyj Collator (t04_strings/Strings01Basics).

        // DOBRA PRAKTYKA: zwracaj Optional z findBook, a nie null. Dlaczego? Typ metody sam mówi „może nie być”,
        //   a wywołujący dostaje wygodne map/orElse zamiast zapominanego sprawdzenia == null.
    }

    // =================================================================================================
    // 6. KOLEJKA REZERWACJI (Deque)
    // =================================================================================================

    /**
     * 6. Kolejka FIFO (pierwszy przyszedł — pierwszy obsłużony) na {@code ArrayDeque}. Po zwrocie książka czeka
     * na pierwszą osobę z kolejki: tylko ona może ją wypożyczyć, inni dostają BookUnavailableException.
     */
    static void reservations() {
        section("6. Kolejka rezerwacji");

        Library library = sampleLibrary(new FakeClock(START, WARSAW));
        library.borrow("C1", "K4");
        show("C2 rezerwuje K4 — miejsce", library.reserve("C2", "K4"));
        // WYNIK: C2 rezerwuje K4 — miejsce → 1
        show("C3 rezerwuje K4 — miejsce", library.reserve("C3", "K4"));
        // WYNIK: C3 rezerwuje K4 — miejsce → 2
        expectThrows("C2 rezerwuje drugi raz", () -> library.reserve("C2", "K4"));
        // WYNIK: ✔ C2 rezerwuje drugi raz → rzucono BorrowingRuleException: C2 już czeka na K4
        expectThrows("rezerwacja wolnej K1", () -> library.reserve("C2", "K1"));
        // WYNIK: ✔ rezerwacja wolnej K1 → rzucono BorrowingRuleException: książka K1 jest dostępna — wypożycz ją

        library.returnBook("K4");
        expectThrows("C3 przed C2", () -> library.borrow("C3", "K4"));
        // WYNIK: ✔ C3 przed C2 → rzucono BookUnavailableException: książka K4 czeka na C2
        show("C2 odbiera K4", library.borrow("C2", "K4").memberId());
        // WYNIK: C2 odbiera K4 → C2
        show("kolejka po odbiorze", library.snapshot().reservations());
        // WYNIK: kolejka po odbiorze → {K4=[C3]}

        // PUŁAPKA: Deque ma dwa zestawy metod. push/pop działają jak STOS (na początku), addLast/pollFirst jak
        //   kolejka. Pomyłka (push zamiast addLast) odwraca kolejność i ostatni zapisany dostaje książkę pierwszy.

        // DOBRA PRAKTYKA: ArrayDeque zamiast LinkedList dla kolejek. Dlaczego? Jest szybsza i nie przyjmuje null,
        //   więc błąd (null jako id czytelnika) wychodzi od razu, a nie przy wyjmowaniu.
    }

    // =================================================================================================
    // 7. POWŁOKA POLECEŃ (parsowanie tekstu → polecenie → wynik)
    // =================================================================================================

    /** Polecenie po sparsowaniu. Zamknięta rodzina rekordów: parser tworzy, powłoka wykonuje. */
    sealed interface Command permits Search, Borrow, Return, Reserve, Wait, Status { }

    record Search(String phrase) implements Command { }
    record Borrow(String memberId, String bookId) implements Command { }
    record Return(String bookId) implements Command { }
    record Reserve(String memberId, String bookId) implements Command { }
    record Wait(int days) implements Command { }
    record Status(String memberId) implements Command { }
    /** CommandShell = powłoka poleceń: zamienia linie tekstu na wywołania Library i na odpowiedzi tekstowe. */
    static final class CommandShell {
        private final Library library;
        private final FakeClock clock;                                              // tylko do polecenia „czekaj” (symulacja)

        CommandShell(Library library, FakeClock clock) {
            this.library = library;
            this.clock = clock;
        }

        static Command parse(String line) {
            String[] parts = line.strip().split("\\s+");                            // split = podziel po białych znakach
            String name = parts[0].toLowerCase(Locale.ROOT);
            return switch (name) {                                                  // switch expression (Java 14+)
                case "szukaj" -> new Search(argument(parts, 1));
                case "pożycz" -> new Borrow(argument(parts, 1), argument(parts, 2));
                case "zwróć" -> new Return(argument(parts, 1));
                case "rezerwuj" -> new Reserve(argument(parts, 1), argument(parts, 2));
                case "czekaj" -> new Wait(Integer.parseInt(argument(parts, 1)));     // parseInt = zamień tekst na int
                case "stan" -> new Status(argument(parts, 1));
                default -> throw new IllegalArgumentException("nieznane polecenie: " + parts[0]);
            };
        }

        private static String argument(String[] parts, int index) {
            if (index >= parts.length) {
                throw new IllegalArgumentException("polecenie „" + parts[0] + "” wymaga " + index + ". argumentu");
            }
            return parts[index];
        }

        /** run = uruchom jedną linię; błędy zamienia na odpowiedź „błąd: ...”, więc skrypt idzie dalej. */
        String run(String line) {
            try {
                return execute(parse(line));
            } catch (LibraryException | IllegalArgumentException e) {               // multi-catch (t10_exceptions/Exceptions03MultiCatch)
                return "błąd: " + e.getMessage();
            }
        }

        private String execute(Command command) {
            // W Javie 21 byłby tu switch z wzorcami: case Borrow b -> ... (Java 21+). W Javie 17: instanceof z wzorcem.
            if (command instanceof Search s) {
                return library.search(s.phrase()).stream().map(Book::id).toList().toString();
            }
            if (command instanceof Borrow b) {
                return "wypożyczono " + b.bookId() + " do " + library.borrow(b.memberId(), b.bookId()).dueOn();
            }
            if (command instanceof Return r) {
                return "zwrócono " + r.bookId() + ", opłata " + library.returnBook(r.bookId()) + " zł";
            }
            if (command instanceof Reserve r) {
                return "miejsce w kolejce: " + library.reserve(r.memberId(), r.bookId());
            }
            if (command instanceof Wait w) {
                clock.advanceDays(w.days());
                return "dziś jest " + library.today();
            }
            if (command instanceof Status st) {
                LocalDate today = library.today();
                return library.loansOf(st.memberId()).stream()
                        .map(loan -> loan.bookId() + (loan.isOverdue(today) ? " (po terminie)" : " do " + loan.dueOn()))
                        .collect(Collectors.joining(", ", "[", "]"));
            }
            throw new IllegalStateException("nieobsłużone polecenie: " + command);
        }
    }

    /**
     * 7. Skrypt poleceń zamiast prawdziwego {@code System.in}: ten sam kod obsłużyłby klawiaturę (Scanner),
     * ale skrypt daje powtarzalny wynik. Każda linia: polecenie → odpowiedź.
     */
    static void commandShell() {
        section("7. Powłoka poleceń");

        FakeClock clock = new FakeClock(START, WARSAW);
        CommandShell shell = new CommandShell(sampleLibrary(clock), clock);
        List<String> script = List.of(
                "szukaj lem",
                "pożycz C1 K4",
                "pożycz C1 K5",
                "pożycz C1 K1",
                "rezerwuj C2 K4",
                "czekaj 20",
                "stan C1",
                "zwróć K4",
                "pożycz C3 K4",
                "pożycz C2 K4",
                "pożycz C1 K2",
                "pożycz C2",
                "tańcz C1");
        for (String line : script) {
            show("> " + line, shell.run(line));
        }
        // WYNIK: > szukaj lem → [K4, K5]
        // WYNIK: > pożycz C1 K4 → wypożyczono K4 do 2026-03-16
        // WYNIK: > pożycz C1 K5 → wypożyczono K5 do 2026-03-16
        // WYNIK: > pożycz C1 K1 → błąd: limit 2 książek dla C1
        // WYNIK: > rezerwuj C2 K4 → miejsce w kolejce: 1
        // WYNIK: > czekaj 20 → dziś jest 2026-03-22
        // WYNIK: > stan C1 → [K4 (po terminie), K5 (po terminie)]
        // WYNIK: > zwróć K4 → zwrócono K4, opłata 3.00 zł
        // WYNIK: > pożycz C3 K4 → błąd: książka K4 czeka na C2
        // WYNIK: > pożycz C2 K4 → wypożyczono K4 do 2026-04-05
        // WYNIK: > pożycz C1 K2 → błąd: C1 ma przeterminowaną książkę
        // WYNIK: > pożycz C2 → błąd: polecenie „pożycz” wymaga 2. argumentu
        // WYNIK: > tańcz C1 → błąd: nieznane polecenie: tańcz

        // DOBRA PRAKTYKA: parsowanie oddzielone od wykonania. Dlaczego? parse() da się przetestować bez biblioteki,
        //   a ta sama Library obsłuży później REST albo GUI — zmienia się tylko „brzeg”.

        // PUŁAPKA: run() łapie tylko błędy domenowe i błędne dane. Gdyby łapał Exception, ukryłby prawdziwe
        //   błędy programisty (np. NullPointerException) pod niewinnym „błąd: null”.
    }

    // =================================================================================================
    // 8. ZAPIS I ODCZYT (plik tekstowy w TempDir, podróż w obie strony)
    // =================================================================================================

    /** Port zapisu: logika zna tylko ten interfejs, nie wie, czy pod spodem jest plik, baza czy chmura. */
    interface LibraryStore {
        void save(LibraryState state);

        LibraryState load();
    }

    /** Prosty format tekstowy: jeden rekord w linii, pola rozdzielone średnikiem, UTF-8. */
    static final class TextFileLibraryStore implements LibraryStore {
        private static final String SEPARATOR = ";";
        private final Path file;

        TextFileLibraryStore(Path file) {
            this.file = file;
        }

        @Override
        public void save(LibraryState state) {
            List<String> lines = new ArrayList<>();
            lines.add("# biblioteka v1");
            state.books().forEach(b -> lines.add(join("BOOK", b.id(), b.title(), b.author(), String.valueOf(b.year()))));
            state.members().forEach(m -> lines.add(join("MEMBER", m.id(), m.name(), m.type().name())));
            state.loans().forEach(l -> lines.add(join("LOAN", l.bookId(), l.memberId(),
                    l.borrowedOn().toString(), l.dueOn().toString())));
            state.reservations().forEach((bookId, queue) -> lines.add(join("RESERVATION", bookId, String.join(",", queue))));
            try {
                Files.write(file, lines, StandardCharsets.UTF_8);                   // write = zapisz wszystkie linie
            } catch (IOException e) {
                throw new UncheckedIOException("nie udało się zapisać " + file.getFileName(), e);
            }
        }

        @Override
        public LibraryState load() {
            List<String> lines;
            try {
                lines = Files.readAllLines(file, StandardCharsets.UTF_8);           // readAllLines = wczytaj wszystkie linie
            } catch (IOException e) {
                throw new UncheckedIOException("nie udało się odczytać " + file.getFileName(), e);
            }
            List<Book> books = new ArrayList<>();
            List<Member> members = new ArrayList<>();
            List<Loan> loans = new ArrayList<>();
            Map<String, List<String>> reservations = new TreeMap<>();
            for (String line : lines) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] f = line.split(SEPARATOR, -1);                             // -1 = zachowaj puste pola na końcu
                switch (f[0]) {
                    case "BOOK" -> books.add(new Book(f[1], f[2], f[3], Integer.parseInt(f[4])));
                    case "MEMBER" -> members.add(new Member(f[1], f[2], MemberType.valueOf(f[3])));
                    case "LOAN" -> loans.add(new Loan(f[1], f[2], LocalDate.parse(f[3]), LocalDate.parse(f[4])));
                    case "RESERVATION" -> reservations.put(f[1], Arrays.asList(f[2].split(",")));
                    default -> throw new IllegalArgumentException("nieznany typ linii: " + f[0]);
                }
            }
            return new LibraryState(books, members, loans, reservations);
        }

        private static String join(String... fields) {
            for (String field : fields) {
                if (field.contains(SEPARATOR)) {
                    throw new IllegalArgumentException("separator „;” w danych: " + field);
                }
            }
            return String.join(SEPARATOR, fields);
        }
    }

    /**
     * 8. Podróż w obie strony (round trip): zapis → odczyt → porównanie przez {@code equals}. Rekordy porównują
     * się po wartościach, więc jedno {@code equals} sprawdza cały stan.
     */
    static void persistence() {
        section("8. Zapis i odczyt");

        FakeClock clock = new FakeClock(START, WARSAW);
        Library library = sampleLibrary(clock);
        library.borrow("C1", "K4");
        library.reserve("C3", "K4");
        library.reserve("C2", "K4");

        Path dir = TempDir.create("capstone01-");
        try {
            LibraryStore store = new TextFileLibraryStore(dir.resolve("biblioteka.txt"));
            LibraryState before = library.snapshot();
            store.save(before);
            List<String> fileLines = Files.readAllLines(dir.resolve("biblioteka.txt"), StandardCharsets.UTF_8);
            show("linii w pliku", fileLines.size());
            // WYNIK: linii w pliku → 12
            fileLines.stream().filter(l -> !l.startsWith("BOOK")).forEach(l -> note(l));
            // WYNIK: ℹ # biblioteka v1
            // WYNIK: ℹ MEMBER;C1;Ala Nowak;STANDARD
            // WYNIK: ℹ MEMBER;C2;Bartek Lis;PREMIUM
            // WYNIK: ℹ MEMBER;C3;Celina Mazur;STANDARD
            // WYNIK: ℹ LOAN;K4;C1;2026-03-02;2026-03-16
            // WYNIK: ℹ RESERVATION;K4;C3,C2

            LibraryState after = store.load();
            show("stan po odczycie równy?", after.equals(before));
            // WYNIK: stan po odczycie równy? → true

            Library restored = Library.restore(after, LibraryPolicy.DEFAULT, clock);
            restored.returnBook("K4");
            show("po odtworzeniu: C2 przed C3?", restored.snapshot().reservations());
            // WYNIK: po odtworzeniu: C2 przed C3? → {K4=[C3, C2]}    ← kolejność kolejki przetrwała zapis

            expectThrows("średnik w tytule", () -> store.save(new LibraryState(
                    List.of(new Book("K7", "Tak; nie", "Ktoś", 2000)), List.of(), List.of(), Map.of())));
            // WYNIK: ✔ średnik w tytule → rzucono IllegalArgumentException: separator „;” w danych: Tak; nie
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            TempDir.deleteRecursively(dir);
        }

        // PUŁAPKA: separator w danych. Tytuł „Tak; nie” rozjechałby kolumny przy odczycie. Albo uciekamy znaki
        //   (escape, cudzysłowy jak w CSV — t18_io_files/Io04Csv), albo — jak tu — odrzucamy dane przy zapisie.

        // DOBRA PRAKTYKA: linia nagłówka z wersją formatu („# biblioteka v1”). Dlaczego? Gdy za rok dodasz pole,
        //   odczyt pozna stary plik po wersji i nie wysypie się na „za mało pól”.
    }

    // =================================================================================================
    // 9. TESTY ZACHOWANIA
    // =================================================================================================

    /**
     * 9. Testy opisują zachowanie, a nie implementację: „zwrot w dniu terminu kosztuje 0”, a nie „metoda
     * wywołuje ChronoUnit”. Dzięki Clock i czystym metodom nie potrzebujemy żadnej biblioteki testowej.
     */
    static void behaviourTests() {
        section("9. Testy zachowania");

        LibraryPolicy policy = LibraryPolicy.DEFAULT;
        Check.equal("brak spóźnienia → 0.00", new BigDecimal("0.00"), () -> policy.lateFee(0));
        Check.equal("1 dzień → 0.50", new BigDecimal("0.50"), () -> policy.lateFee(1));
        Check.equal("40 dni → limit 20.00", new BigDecimal("20.00"), () -> policy.lateFee(40));

        LocalDate borrowed = LocalDate.of(2026, 3, 2);
        Loan loan = new Loan("K1", "C1", borrowed, borrowed.plusDays(14));
        Check.equal("w dniu terminu nie jest po terminie", false, () -> loan.isOverdue(loan.dueOn()));
        Check.equal("zwrot przed terminem → 0 dni", 0L, () -> loan.daysLate(borrowed.plusDays(3)));

        FakeClock clock = new FakeClock(START, WARSAW);
        Library library = sampleLibrary(clock);
        library.borrow("C1", "K1");
        clock.advanceDays(15);
        Check.throwsException("przeterminowana blokuje", BorrowingRuleException.class, () -> library.borrow("C1", "K2"));
        Check.equal("nieudane wypożyczenie nie zmienia stanu", 1, () -> library.loansOf("C1").size());
        Check.equal("parser rozpoznaje polecenie", new Borrow("C1", "K2"), () -> CommandShell.parse("  POŻYCZ C1 K2 "));
        Check.summary();
        // WYNIK: ✔ OK    brak spóźnienia → 0.00
        // WYNIK: ✔ OK    1 dzień → 0.50
        // WYNIK: ✔ OK    40 dni → limit 20.00
        // WYNIK: ✔ OK    w dniu terminu nie jest po terminie
        // WYNIK: ✔ OK    zwrot przed terminem → 0 dni
        // WYNIK: ✔ OK    przeterminowana blokuje
        // WYNIK: ✔ OK    nieudane wypożyczenie nie zmienia stanu
        // WYNIK: ✔ OK    parser rozpoznaje polecenie
        // WYNIK: PODSUMOWANIE: ✔ 8 OK, ✘ 0 BŁĄD

        // DOBRA PRAKTYKA: testuj granice — dzień terminu, 0 dni, limit opłaty. Dlaczego? Błędy „o jeden”
        //   (isAfter zamiast !isBefore) mieszkają właśnie na granicach.
    }

    // =================================================================================================
    // 10. CO DALEJ — ta sama aplikacja w Springu
    // =================================================================================================

    /**
     * 10. Podział na warstwy przenosi się do Springa prawie jeden do jednego. Zmienia się „klej”
     * (adnotacje, kontener), a nie reguły biznesowe.
     */
    static void whatNext() {
        section("10. Co dalej");

        Map<String, String> springMapping = new TreeMap<>(Map.of(
                "Book, Member, Loan", "@Entity (JPA) albo rekordy DTO",
                "Library", "@Service LibraryService z @Transactional",
                "LibraryStore", "@Repository / interfejs Spring Data JPA",
                "CommandShell", "@RestController z @PostMapping(\"/loans\")",
                "Clock", "@Bean Clock — w teście Clock.fixed",
                "LibraryException", "@RestControllerAdvice → kody 404/409"));
        showEach("nasza klasa → Spring", springMapping);
        // WYNIK: nasza klasa → Spring (liczba kluczy: 6):
        // WYNIK: • Book, Member, Loan → @Entity (JPA) albo rekordy DTO
        // WYNIK: • Clock → @Bean Clock — w teście Clock.fixed
        // WYNIK: • CommandShell → @RestController z @PostMapping("/loans")
        // WYNIK: • Library → @Service LibraryService z @Transactional
        // WYNIK: • LibraryException → @RestControllerAdvice → kody 404/409
        // WYNIK: • LibraryStore → @Repository / interfejs Spring Data JPA

        // Do powtórki: t34_toward_spring/Spring02Layers (warstwy), t29_jdbc_databases/Jdbc05Dao (zapis w bazie),
        //   t32_junit_mockito/JUnit01Basics (te same testy w JUnit 5), t22_design_patterns/Patterns09Command.
        // DOBRA PRAKTYKA: w kursie SpringLearning zacznij od przeniesienia Library i jej testów BEZ zmian —
        //   jeśli reguły nie zależą od frameworka, migracja dotyczy tylko brzegów.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Kolejność budowy: wymagania z przykładami → model (rekordy + walidacja) → logika → brzegi → testy.
     *   • Rekord z konstruktorem kompaktowym = wartość, która nie może być błędna.
     *   • Jeden obiekt ze zmiennym stanem (Library), stan prywatny, na zewnątrz migawka (List.copyOf).
     *   • Najpierw wszystkie sprawdzenia, potem zmiana stanu — nieudana operacja niczego nie psuje.
     *   • Czas: wstrzyknięty Clock; w testach zegar-atrapa lub Clock.fixed. Terminy: Period.ofDays.
     *   • Pieniądze: BigDecimal z tekstu, min() jako limit, setScale(2, HALF_UP).
     *   • Kolejka: ArrayDeque + addLast/pollFirst/peekFirst; stos: push/pop.
     *   • Wyjątki domenowe w zamkniętej (sealed) hierarchii → brzeg mapuje je na komunikaty / kody HTTP.
     *   • Zapis: interfejs LibraryStore + implementacja plikowa, UTF-8, wersja formatu, test „w obie strony”.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego Library dostaje Clock w konstruktorze zamiast wołać LocalDate.now()?
     *   2. Co wypisze:  System.out.println(new BigDecimal("0.50").multiply(BigDecimal.valueOf(6)));  ?
     *   3. Co wypisze:  System.out.println(ChronoUnit.DAYS.between(LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 10)));  ?
     *      I dlaczego daysLate() używa Math.max(0, ...)?
     *   4. ZNAJDŹ BŁĄD (kolejka rezerwacji):
     *        Deque<String> queue = new ArrayDeque<>();
     *        queue.push("C2");  queue.push("C3");
     *        String next = queue.peekFirst();   // spodziewamy się C2
     *   5. Dlaczego w borrow() kolejka jest skracana (pollFirst) dopiero po sprawdzeniu limitu?
     *   6. ZNAJDŹ BŁĄD:  boolean isOverdue(LocalDate today) { return !today.isBefore(dueOn); }
     *   7. Po co linia „# biblioteka v1” w pliku i dlaczego zapis odrzuca średnik w tytule?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        LocalDate today = LocalDate.of(2026, 3, 20);
        List<Loan> loans = sampleLoans();
        List<Book> books = sampleLibrary(new FakeClock(START, WARSAW)).search("");
        Check.equal("ćw. 1: przeterminowane", List.of("K1", "K2", "K5"), () -> exercise1(loans, today));
        Check.equal("ćw. 2: opłata z karencją", feeExamples(),
                () -> List.of(exercise2(due(), due().plusDays(2)), exercise2(due(), due().plusDays(3)),
                        exercise2(due(), due().plusDays(500))));
        Check.equal("ćw. 3: wypożyczenia na autora", authorCounts(), () -> exercise3(loans, books));
        Check.equal("ćw. 4: linia LOAN", loans.get(0), () -> exercise4("LOAN;K1;C1;2026-03-01;2026-03-15"));
        Check.throwsException("ćw. 4: za mało pól", IllegalArgumentException.class, () -> exercise4("LOAN;K1;C1"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of("K1", "K2", "K5"), () -> solution1(loans, today));
        Check.equal("ćw. 2 (wzorzec)", feeExamples(),
                () -> List.of(solution2(due(), due().plusDays(2)), solution2(due(), due().plusDays(3)),
                        solution2(due(), due().plusDays(500))));
        Check.equal("ćw. 3 (wzorzec)", authorCounts(), () -> solution3(loans, books));
        Check.equal("ćw. 4 (wzorzec)", loans.get(0), () -> solution4("LOAN;K1;C1;2026-03-01;2026-03-15"));
        Check.throwsException("ćw. 4: za mało pól (wzorzec)", IllegalArgumentException.class,
                () -> solution4("LOAN;K1;C1"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    static List<Loan> sampleLoans() {
        return List.of(
                new Loan("K1", "C1", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 15)),
                new Loan("K2", "C2", LocalDate.of(2026, 2, 20), LocalDate.of(2026, 3, 6)),
                new Loan("K4", "C2", LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 24)),
                new Loan("K5", "C3", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 19)),
                new Loan("K6", "C3", LocalDate.of(2026, 3, 6), LocalDate.of(2026, 3, 20)));    // termin = „dziś”
    }

    static LocalDate due() {
        return LocalDate.of(2026, 3, 16);
    }

    static List<BigDecimal> feeExamples() {
        return List.of(new BigDecimal("0.00"), new BigDecimal("0.50"), new BigDecimal("20.00"));
    }

    static Map<String, Long> authorCounts() {
        return new TreeMap<>(Map.of("Adam Mickiewicz", 1L, "Bolesław Prus", 2L, "Stanisław Lem", 2L));
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć posortowane id książek z wypożyczeń przeterminowanych w dniu {@code today}.
     * Uwaga na granicę: wypożyczenie z terminem równym {@code today} NIE jest przeterminowane.
     * Podpowiedź: stream → filter(loan -> loan.isOverdue(today)) → map(Loan::bookId) → sorted() → toList().
     */
    static List<String> exercise1(List<Loan> loans, LocalDate today) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): nowa reguła regulaminu — karencja (okres ulgowy). Pierwsze 2 dni spóźnienia są
     * darmowe, każdy kolejny dzień kosztuje 0.50 zł, łącznie najwyżej 20.00 zł. Wynik ze skalą 2.
     * Przykłady: 2 dni spóźnienia → 0.00; 3 dni → 0.50; 500 dni → 20.00.
     * Podpowiedź: dni = max(0, DAYS.between(due, returned) - 2); potem jak LibraryPolicy.lateFee.
     */
    static BigDecimal exercise2(LocalDate due, LocalDate returned) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie, łączy z t16_streams/Streams11GroupingBy): nowa linia raportu — ile wypożyczeń
     * przypada na każdego autora. Zwróć mapę posortowaną po autorze (TreeMap). Książki szukaj po id w {@code books}.
     * Podpowiedź: najpierw Map id → autor (Collectors.toMap), potem
     * groupingBy(autor, TreeMap::new, counting()).
     */
    static Map<String, Long> exercise3(List<Loan> loans, List<Book> books) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): odczyt jednej linii pliku w formacie {@code LOAN;idKsiążki;idCzytelnika;data;termin}.
     * Zła liczba pól albo inny typ niż LOAN → IllegalArgumentException z czytelnym komunikatem. Błędna data
     * (DateTimeParseException) też zamień na IllegalArgumentException, zachowując przyczynę (cause).
     * Podpowiedź: split(";", -1), sprawdź length == 5 i f[0].equals("LOAN"), LocalDate.parse w try/catch.
     */
    static Loan exercise4(String line) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solution1(List<Loan> loans, LocalDate today) {
        return loans.stream()
                .filter(loan -> loan.isOverdue(today))
                .map(Loan::bookId)
                .sorted()
                .toList();
    }

    static BigDecimal solution2(LocalDate due, LocalDate returned) {
        final long graceDays = 2;                                                   // grace = karencja
        long chargedDays = Math.max(0, ChronoUnit.DAYS.between(due, returned) - graceDays);
        return new BigDecimal("0.50").multiply(BigDecimal.valueOf(chargedDays))
                .min(new BigDecimal("20.00"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    static Map<String, Long> solution3(List<Loan> loans, List<Book> books) {
        Map<String, String> authorById = books.stream().collect(Collectors.toMap(Book::id, Book::author));
        return loans.stream()
                .collect(Collectors.groupingBy(loan -> authorById.get(loan.bookId()), TreeMap::new,
                        Collectors.counting()));
    }

    static Loan solution4(String line) {
        String[] f = line.split(";", -1);
        if (f.length != 5 || !f[0].equals("LOAN")) {
            throw new IllegalArgumentException("oczekiwano LOAN z 5 polami: " + line);
        }
        try {
            return new Loan(f[1], f[2], LocalDate.parse(f[3]), LocalDate.parse(f[4]));
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("błędna data w linii: " + line, e);
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo wtedy czas jest zależnością, którą da się podmienić: w teście zegar-atrapa lub Clock.fixed daje
     *      zawsze ten sam „dziś”, więc testy opłat i terminów są powtarzalne i nie czekają naprawdę 14 dni.
     *   2. 3.00 — skala wyniku mnożenia to suma skal (2 + 0), więc dwa miejsca po przecinku zostają.
     *   3. -6 — between liczy od pierwszego do drugiego argumentu, więc data wcześniejsza na drugim miejscu daje
     *      wynik ujemny. Math.max(0, ...) sprawia, że zwrot przed terminem to 0 dni spóźnienia, a nie „ujemna opłata”.
     *   4. push() wkłada na POCZĄTEK (stos), więc peekFirst() zwróci C3 — ostatnio dodanego. W kolejce FIFO
     *      używamy addLast("C2"), addLast("C3"); wtedy peekFirst() zwraca C2.
     *   5. Bo nieudana operacja nie może zmienić stanu. Gdyby pollFirst był wcześniej, czytelnik z pełnym limitem
     *      straciłby miejsce w kolejce, choć książki nie dostał.
     *   6. !isBefore(dueOn) jest prawdą także W DNIU terminu, więc zwrot w ostatnim dniu byłby „po terminie”.
     *      Poprawnie: today.isAfter(dueOn).
     *   7. Wersja formatu pozwala za rok rozpoznać stare pliki i odczytać je inaczej. Średnik to separator pól —
     *      w tytule rozjechałby kolumny przy odczycie, więc zapis odrzuca takie dane (alternatywa: cudzysłowy i escape).
     */
    // </editor-fold>
}
