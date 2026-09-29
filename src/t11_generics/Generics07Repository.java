package t11_generics;

import helpers.Check;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Generyki w praktyce — generyczne repozytorium {@code Repository<T, ID>}
 *        (repository = repozytorium (magazyn obiektów); entity = encja (obiekt z identyfikatorem); in-memory = w pamięci)
 *
 * W SKRÓCIE:
 *   Prawie każdy program przechowuje obiekty z identyfikatorem: klientów, produkty, zamówienia. Operacje są zawsze
 *   te same: zapisz, znajdź po id, pobierz wszystkie, usuń, wyszukaj po warunku. Zamiast pisać ClientRepository,
 *   ItemRepository, OrderRepository osobno, piszemy RAZ {@code InMemoryRepository<T, ID>} — z ograniczeniem
 *   {@code T extends Identifiable<ID>}, żeby repozytorium umiało odczytać id każdego obiektu. Kod korzysta
 *   z interfejsu Repository, więc implementację (pamięć, plik, baza) można podmienić bez zmian w reszcie programu.
 *
 * ANALOGIA: szafka na klucz z numerkami w szatni.
 *   Szatnia (repozytorium) działa tak samo dla kurtek, parasoli i plecaków (T): oddajesz rzecz, dostajesz numerek (ID),
 *   po numerku odbierasz. Szatniarz nie musi wiedzieć, CO przechowuje — wystarczy, że każda rzecz ma numerek.
 *
 * JAK TO DZIAŁA:
 *   interface Identifiable{@code <ID>} { ID id(); }
 *   interface Repository{@code <T extends Identifiable<ID>, ID>} { void save(T e); Optional{@code <T>} findById(ID id); ... }
 *   class InMemoryRepository{@code <T extends Identifiable<ID>, ID>} implements Repository{@code <T, ID>} {
 *       private final Map{@code <ID, T>} store = new LinkedHashMap{@code <>}();
 *   }
 *   Repository{@code <Client, Long>} clients = new InMemoryRepository{@code <>}();
 *
 * SŁÓWKA:
 *   repository = repozytorium; entity = encja; identifiable = identyfikowalny (ma id); save = zapisz; find by id = znajdź
 *   po id; find all = znajdź wszystkie; delete = usuń; find where = znajdź, gdzie (warunek); store = magazyn;
 *   client = klient; item = pozycja/towar; decorator = dekorator (obiekt „opakowujący” inny); count = policz.
 *
 * ZOBACZ TEŻ: t11_generics/Generics04Bounded (ograniczenia), t11_generics/Generics05Wildcards (Predicate{@code <? super T>}),
 *             t22_design_patterns/Patterns07Decorator (dekorator), t22_design_patterns/Patterns08DependencyInjection,
 *             t25_testing/Testing02TestDoubles (repozytorium w pamięci jako „fake” w testach).
 * </pre>
 */
public class Generics07Repository {

    // ---------------------------------------------------------------------------------------------
    // Interfejsy
    // ---------------------------------------------------------------------------------------------

    /** Identifiable = obiekt z identyfikatorem typu ID (Long, String, UUID...). */
    interface Identifiable<ID> {
        ID id();
    }

    /** Repository = repozytorium obiektów T o identyfikatorach typu ID. Opisuje CO umie, nie JAK przechowuje. */
    interface Repository<T extends Identifiable<ID>, ID> {
        void save(T entity);                               // dodaj albo zastąp (ten sam id)

        Optional<T> findById(ID id);

        List<T> findAll();

        boolean deleteById(ID id);

        int count();

        List<T> findWhere(Predicate<? super T> condition); // ? super T — PECS: warunek „konsumuje” T

        /** mapAll = przekształć wszystkie. Metoda generyczna w interfejsie generycznym (własne R) z domyślną implementacją. */
        default <R> List<R> mapAll(Function<? super T, ? extends R> mapper) {
            List<R> result = new ArrayList<>();
            for (T entity : findAll()) {
                result.add(mapper.apply(entity));
            }
            return result;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Implementacja w pamięci
    // ---------------------------------------------------------------------------------------------

    /**
     * InMemoryRepository = repozytorium w pamięci. LinkedHashMap zachowuje kolejność dodawania — findAll zwraca
     * obiekty w przewidywalnej kolejności.
     */
    static final class InMemoryRepository<T extends Identifiable<ID>, ID> implements Repository<T, ID> {
        private final Map<ID, T> store = new LinkedHashMap<>();

        @Override
        public void save(T entity) {
            store.put(entity.id(), entity);                // id() dostępne dzięki T extends Identifiable<ID>
        }

        @Override
        public Optional<T> findById(ID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<T> findAll() {
            return List.copyOf(store.values());            // kopia — nikt z zewnątrz nie zmieni magazynu
        }

        @Override
        public boolean deleteById(ID id) {
            return store.remove(id) != null;
        }

        @Override
        public int count() {
            return store.size();
        }

        @Override
        public List<T> findWhere(Predicate<? super T> condition) {
            List<T> result = new ArrayList<>();
            for (T entity : store.values()) {
                if (condition.test(entity)) {
                    result.add(entity);
                }
            }
            return result;
        }
    }

    /**
     * CountingRepository = dekorator: opakowuje DOWOLNE repozytorium i liczy wywołania findById. Działa dla każdego T i ID,
     * bo sam jest generyczny i przekazuje pracę dalej (delegate = pełnomocnik).
     */
    static final class CountingRepository<T extends Identifiable<ID>, ID> implements Repository<T, ID> {
        private final Repository<T, ID> delegate;
        private int lookups;

        CountingRepository(Repository<T, ID> delegate) {
            this.delegate = delegate;
        }

        int getLookups() {
            return lookups;
        }

        @Override
        public void save(T entity) {
            delegate.save(entity);
        }

        @Override
        public Optional<T> findById(ID id) {
            lookups++;
            return delegate.findById(id);
        }

        @Override
        public List<T> findAll() {
            return delegate.findAll();
        }

        @Override
        public boolean deleteById(ID id) {
            return delegate.deleteById(id);
        }

        @Override
        public int count() {
            return delegate.count();
        }

        @Override
        public List<T> findWhere(Predicate<? super T> condition) {
            return delegate.findWhere(condition);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Encje (rekordy — ich metoda dostępu id() spełnia interfejs Identifiable)
    // ---------------------------------------------------------------------------------------------

    /** Client = klient: identyfikator liczbowy Long. */
    record Client(Long id, String name, String city) implements Identifiable<Long> {
    }

    /** Item = towar: identyfikator tekstowy (SKU = kod towaru). */
    record Item(String id, String name, int priceGrosze) implements Identifiable<String> {
    }

    /** sampleClients = przykładowe repozytorium klientów. */
    static Repository<Client, Long> sampleClients() {
        Repository<Client, Long> repo = new InMemoryRepository<>();
        repo.save(new Client(1L, "Anna", "Kraków"));
        repo.save(new Client(2L, "Jan", "Gdańsk"));
        repo.save(new Client(3L, "Ewa", "Kraków"));
        return repo;
    }

    public static void main(String[] args) {
        title("Generics07 — generyczne repozytorium");

        basicOperations();          // basic operations = podstawowe operacje
        sameCodeOtherTypes();       // same code, other types = ten sam kod, inne typy
        searching();                // searching = wyszukiwanie
        mappingResults();           // mapping results = przekształcanie wyników
        swappingImplementation();   // swapping implementation = podmiana implementacji
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PODSTAWOWE OPERACJE
    // =================================================================================================

    /** 1. save, findById, findAll, deleteById, count — wszystko typowane: findById(1L) zwraca {@code Optional<Client>}. */
    static void basicOperations() {
        section("1. Repozytorium klientów: Repository<Client, Long>");

        Repository<Client, Long> clients = sampleClients();
        show("count", clients.count());
        show("findById(2)", clients.findById(2L));
        show("findById(9)", clients.findById(9L));
        // WYNIK: count → 3
        // WYNIK: findById(2) → Optional[Client[id=2, name=Jan, city=Gdańsk]]
        // WYNIK: findById(9) → Optional.empty

        clients.save(new Client(2L, "Jan", "Sopot"));      // ten sam id → zastąpienie (przeprowadzka)
        show("po przeprowadzce", clients.findById(2L).map(Client::city).orElse("?"));
        show("deleteById(1)", clients.deleteById(1L));
        show("deleteById(1) ponownie", clients.deleteById(1L));
        show("findAll", clients.mapAll(Client::name));
        // WYNIK: po przeprowadzce → Sopot
        // WYNIK: deleteById(1) → true
        // WYNIK: deleteById(1) ponownie → false
        // WYNIK: findAll → [Jan, Ewa]

        // clients.save(new Item("K-1", "kawa", 1500));   ← BŁĄD KOMPILACJI: Item to nie Client
        // clients.findById("2");                          ← BŁĄD KOMPILACJI: id klienta to Long, nie String
    }

    // =================================================================================================
    // 2. TEN SAM KOD, INNE TYPY
    // =================================================================================================

    /** 2. Ta sama klasa InMemoryRepository obsługuje towary z identyfikatorem String — zero nowego kodu. */
    static void sameCodeOtherTypes() {
        section("2. Repository<Item, String> — ten sam kod");

        Repository<Item, String> items = new InMemoryRepository<>();
        items.save(new Item("K-1", "kawa", 1500));
        items.save(new Item("H-7", "herbata", 900));
        show("findById(\"H-7\")", items.findById("H-7").map(Item::name).orElse("brak"));
        show("count", items.count());
        // WYNIK: findById("H-7") → herbata
        // WYNIK: count → 2
    }

    // =================================================================================================
    // 3. WYSZUKIWANIE PO WARUNKU
    // =================================================================================================

    /** 3. findWhere(Predicate{@code <? super T>}) — warunek jako lambda. Dzięki ? super działa też Predicate{@code <Object>}. */
    static void searching() {
        section("3. findWhere: warunek jako lambda");

        Repository<Client, Long> clients = sampleClients();
        show("z Krakowa", clients.findWhere(c -> c.city().equals("Kraków")).size());
        // WYNIK: z Krakowa → 2

        Predicate<Object> notNull = o -> o != null;       // warunek dla Object pasuje do Predicate<? super Client>
        show("findWhere(notNull)", clients.findWhere(notNull).size());
        // WYNIK: findWhere(notNull) → 3
    }

    // =================================================================================================
    // 4. PRZEKSZTAŁCANIE WYNIKÓW
    // =================================================================================================

    /** 4. mapAll to metoda generyczna z domyślną implementacją w interfejsie — działa dla każdej implementacji. */
    static void mappingResults() {
        section("4. mapAll: <R> List<R> mapAll(Function<? super T, ? extends R>)");

        Repository<Client, Long> clients = sampleClients();
        List<String> labels = clients.mapAll(c -> c.name() + " (" + c.city() + ")");
        List<Integer> nameLengths = clients.mapAll(c -> c.name().length());
        show("etykiety", labels);
        show("długości imion", nameLengths);
        // WYNIK: etykiety → [Anna (Kraków), Jan (Gdańsk), Ewa (Kraków)]
        // WYNIK: długości imion → [4, 3, 3]
    }

    // =================================================================================================
    // 5. PODMIANA IMPLEMENTACJI
    // =================================================================================================

    /** greetClient = kod „serwisu” — zależy tylko od INTERFEJSU Repository, nie od konkretnej klasy. */
    static String greetClient(Repository<Client, Long> repo, long id) {
        return repo.findById(id).map(c -> "Witaj, " + c.name() + "!").orElse("Nie znamy Cię :(");
    }

    /**
     * 5. Serwis dostaje repozytorium z zewnątrz. Możemy mu podać zwykłe repozytorium albo opakowane w dekorator
     * liczący zapytania — serwis nie wie o różnicy (i nie musi).
     */
    static void swappingImplementation() {
        section("5. Podmiana implementacji: dekorator liczący zapytania");

        CountingRepository<Client, Long> counting = new CountingRepository<>(sampleClients());
        show("greet(1)", greetClient(counting, 1));
        show("greet(7)", greetClient(counting, 7));
        show("liczba zapytań findById", counting.getLookups());
        // WYNIK: greet(1) → Witaj, Anna!
        // WYNIK: greet(7) → Nie znamy Cię :(
        // WYNIK: liczba zapytań findById → 2

        // DOBRA PRAKTYKA: kod zależy od interfejsu (Repository), nie od klasy (InMemoryRepository). W testach podajesz
        //   repozytorium w pamięci, w programie — bazodanowe (t25_testing, t22_design_patterns/Patterns08DependencyInjection).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • interface Identifiable<ID> { ID id(); } — wspólny „numerek” dla encji; rekord spełnia go metodą id().
     *   • interface Repository<T extends Identifiable<ID>, ID> — save, findById → Optional<T>, findAll, deleteById, count.
     *   • InMemoryRepository<T extends Identifiable<ID>, ID> — Map<ID, T> (LinkedHashMap = stała kolejność).
     *   • Ta sama klasa dla Client/Long i Item/String; kompilator pilnuje typu encji i typu id.
     *   • Predicate<? super T>, Function<? super T, ? extends R> — PECS w sygnaturach.
     *   • Metoda default <R> w interfejsie; dekorator generyczny opakowuje dowolne repozytorium.
     *   • Kod zależy od interfejsu → implementację można podmienić (testy, baza, cache).
     *
     * PYTANIA KONTROLNE:
     *   1. Po co ograniczenie T extends Identifiable<ID> w InMemoryRepository?
     *   2. Co wypisze:  Repository<Item, String> r = new InMemoryRepository<>(); r.save(new Item("A", "x", 1));
     *                   r.save(new Item("A", "y", 2)); System.out.println(r.count() + " " + r.findById("A").get().name());
     *   3. ZNAJDŹ BŁĄD:  Repository<Client, String> clients = new InMemoryRepository<>();
     *   4. Dlaczego findAll zwraca List.copyOf(...), a nie bezpośrednio store.values()?
     *   5. Dlaczego greetClient przyjmuje Repository, a nie InMemoryRepository?
     *   6. Co zyskujemy dzięki Predicate<? super T> zamiast Predicate<T> w findWhere?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: imiona klientów z miasta", List.of("Anna", "Ewa"), () -> exercise1(sampleClients(), "Kraków"));
        Check.equal("ćw. 2: saveAll zwraca nowy rozmiar", 5, () -> exercise2(sampleClients(),
                List.of(new Client(4L, "Olek", "Łódź"), new Client(5L, "Iza", "Łódź"))));
        Check.equal("ćw. 3: indeks po id", "{K-1=kawa, H-7=herbata}",
                () -> exercise3(List.of(new Item("K-1", "kawa", 1500), new Item("H-7", "herbata", 900)))
                        .entrySet().stream().map(e -> e.getKey() + "=" + e.getValue().name())
                        .collect(java.util.stream.Collectors.joining(", ", "{", "}")));
        Check.equal("ćw. 4a: zmiana imienia", "true/Janek", () -> {
            Repository<Client, Long> repo = sampleClients();
            boolean ok = exercise4(repo, 2L, "Janek");
            return ok + "/" + repo.findById(2L).map(Client::name).orElse("?");
        });
        Check.equal("ćw. 4b: brak klienta", false, () -> exercise4(sampleClients(), 99L, "Nikt"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of("Anna", "Ewa"), () -> solution1(sampleClients(), "Kraków"));
        Check.equal("ćw. 2 (wzorzec)", 5, () -> solution2(sampleClients(),
                List.of(new Client(4L, "Olek", "Łódź"), new Client(5L, "Iza", "Łódź"))));
        Check.equal("ćw. 3 (wzorzec)", "{K-1=kawa, H-7=herbata}",
                () -> solution3(List.of(new Item("K-1", "kawa", 1500), new Item("H-7", "herbata", 900)))
                        .entrySet().stream().map(e -> e.getKey() + "=" + e.getValue().name())
                        .collect(java.util.stream.Collectors.joining(", ", "{", "}")));
        Check.equal("ćw. 4a (wzorzec)", "true/Janek", () -> {
            Repository<Client, Long> repo = sampleClients();
            boolean ok = solution4(repo, 2L, "Janek");
            return ok + "/" + repo.findById(2L).map(Client::name).orElse("?");
        });
        Check.equal("ćw. 4b (wzorzec)", false, () -> solution4(sampleClients(), 99L, "Nikt"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /** ĆWICZENIE 1 (łatwe): imiona klientów z podanego miasta, w kolejności z repozytorium. Podpowiedź: findWhere. */
    static List<String> exercise1(Repository<Client, Long> repo, String city) {
        // TODO: twoje rozwiązanie
        return new ArrayList<>();
    }

    /**
     * ĆWICZENIE 2 (średnie): zapisz wszystkie encje i zwróć liczbę elementów w repozytorium po zapisie.
     * Metoda generyczna — zadziała dla każdego repozytorium. Zwróć uwagę na {@code List<? extends T>}.
     */
    static <T extends Identifiable<ID>, ID> int exercise2(Repository<T, ID> repo, List<? extends T> entities) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (średnie): zbuduj mapę id → encja (LinkedHashMap, kolejność z listy). Metoda generyczna dla
     * dowolnych encji. Podpowiedź: for (T e : entities) map.put(e.id(), e);
     */
    static <T extends Identifiable<ID>, ID> Map<ID, T> exercise3(List<T> entities) {
        // TODO: twoje rozwiązanie
        return new LinkedHashMap<>();
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zmień imię klienta. Rekord jest niezmienny, więc: znajdź klienta, utwórz NOWY rekord
     * z nowym imieniem (to samo id i miasto) i zapisz (save zastąpi stary). Zwróć true; gdy klienta nie ma — false.
     */
    static boolean exercise4(Repository<Client, Long> repo, Long id, String newName) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solution1(Repository<Client, Long> repo, String city) {
        List<String> names = new ArrayList<>();
        for (Client c : repo.findWhere(c -> c.city().equals(city))) {
            names.add(c.name());
        }
        return names;
    }

    static <T extends Identifiable<ID>, ID> int solution2(Repository<T, ID> repo, List<? extends T> entities) {
        for (T e : entities) {
            repo.save(e);
        }
        return repo.count();
    }

    static <T extends Identifiable<ID>, ID> Map<ID, T> solution3(List<T> entities) {
        Map<ID, T> map = new LinkedHashMap<>();
        for (T e : entities) {
            map.put(e.id(), e);
        }
        return map;
    }

    static boolean solution4(Repository<Client, Long> repo, Long id, String newName) {
        Optional<Client> found = repo.findById(id);
        if (found.isEmpty()) {
            return false;
        }
        Client old = found.get();
        repo.save(new Client(old.id(), newName, old.city()));
        return true;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Żeby repozytorium mogło wywołać entity.id() — bez ograniczenia T byłoby „czymkolwiek” i nie wiadomo,
     *      skąd wziąć klucz do mapy.
     *   2. „1 y” — ten sam id "A", więc drugi save zastąpił pierwszy obiekt.
     *   3. Client implementuje Identifiable<Long>, a repozytorium deklaruje ID = String — T nie spełnia ograniczenia
     *      T extends Identifiable<ID> (błąd kompilacji). Poprawnie: Repository<Client, Long>.
     *   4. Żeby nikt z zewnątrz nie zmienił wnętrza repozytorium przez zwróconą kolekcję; kopia jest niemodyfikowalna
     *      i nie zmienia się, gdy repozytorium się zmienia.
     *   5. Zależność od interfejsu pozwala podać dowolną implementację (w pamięci, bazodanową, dekorator liczący) bez
     *      zmiany kodu serwisu — łatwiej testować i rozwijać.
     *   6. Można podać ogólniejszy warunek, np. Predicate<Object> albo Predicate<Identifiable<Long>>, i używać go
     *      z repozytoriami różnych typów (PECS: warunek konsumuje elementy).
     */
    // </editor-fold>
}
