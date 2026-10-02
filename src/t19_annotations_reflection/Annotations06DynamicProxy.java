package t19_annotations_reflection;

import helpers.Check;
import helpers.SampleData;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Dynamiczne proxy — obiekt-zastępca tworzony w czasie działania programu
 *        (proxy = pełnomocnik, zastępca; invocation handler = obsługa wywołań; dynamic = dynamiczny, w locie)
 *
 * W SKRÓCIE:
 *   Proxy.newProxyInstance tworzy w locie obiekt, który implementuje podany INTERFEJS. Każde wywołanie jego metody
 *   trafia do jednej metody invoke(proxy, metoda, argumenty) w Twoim InvocationHandler. Tam możesz coś zrobić
 *   przed i po wywołaniu prawdziwego obiektu: zalogować, policzyć, zapamiętać wynik, otworzyć transakcję.
 *   Tak działają @Transactional, @Cacheable i repozytoria Spring Data.
 *
 * ANALOGIA: sekretarka prezesa.
 *   Dzwonisz do prezesa, ale odbiera sekretarka (proxy). Zapisuje, kto dzwonił (logowanie), liczy telefony
 *   (licznik), na częste pytania odpowiada sama (cache), a ważne sprawy łączy z prezesem (prawdziwy obiekt).
 *   Prezes nie wie, że sekretarka istnieje. Ale gdy prezes sam do siebie mówi — sekretarka tego nie słyszy.
 *
 * JAK TO DZIAŁA:
 *   klient ──priceOf("X")──▶ [ PROXY ($Proxy..., implements PriceService) ]
 *                                     │ każde wywołanie → handler.invoke(proxy, metoda priceOf, ["X"])
 *                                     ▼
 *                            [ InvocationHandler ]  przed → method.invoke(target, args) → po
 *                                     ▼
 *                            [ prawdziwy obiekt (target) ]
 *
 * SŁÓWKA:
 *   proxy = pełnomocnik (zastępca); target = cel (prawdziwy obiekt); handler = obsługa; invoke = wywołaj;
 *   decorator = dekorator; cache = pamięć podręczna; transaction = transakcja; begin = rozpocznij;
 *   commit = zatwierdź; rollback = wycofaj; self-invocation = wywołanie samego siebie (wewnętrzne);
 *   undeclared = niezadeklarowany; retry = ponów próbę; read-only = tylko do odczytu.
 *
 * ZOBACZ TEŻ: t22_design_patterns/Patterns07Decorator (ręczny odpowiednik proxy),
 *             t19_annotations_reflection/Annotations03ReflectionBasics (Method.invoke),
 *             t10_exceptions/Exceptions06ChainingWrapping (rozpakowywanie przyczyny), t11_generics/Generics03Methods.
 * </pre>
 */
public class Annotations06DynamicProxy {

    // ---------------------------------------------------------------------------------------------
    // Interfejsy i prawdziwe implementacje
    // ---------------------------------------------------------------------------------------------

    /** PriceService = usługa cen. Proxy działa TYLKO dla interfejsów. */
    interface PriceService {
        BigDecimal priceOf(String sku);          // cena produktu o danym SKU
        BigDecimal totalOf(List<String> skus);   // suma cen
        int catalogSize();                       // liczba produktów w katalogu
    }

    /** RealPriceService = prawdziwa usługa. realCalls = ile razy NAPRAWDĘ policzyła cenę. */
    static final class RealPriceService implements PriceService {
        int realCalls;

        @Override
        public BigDecimal priceOf(String sku) {
            realCalls++;
            return SampleData.productBySku(sku).price();
        }

        @Override
        public BigDecimal totalOf(List<String> skus) {
            BigDecimal sum = BigDecimal.ZERO;
            for (String sku : skus) {
                sum = sum.add(priceOf(sku));     // this.priceOf — wywołanie WEWNĘTRZNE (sekcja 8!)
            }
            return sum;
        }

        @Override
        public int catalogSize() {
            return SampleData.products().size();
        }

        @Override
        public String toString() {
            return "RealPriceService";
        }
    }

    /** Transactional = „wykonaj w transakcji” (nasza wersja adnotacji ze Springa). */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Transactional {
    }

    /** AccountService = usługa kont. */
    interface AccountService {
        void transfer(String from, String to, int amount);   // przelew
        int balance(String account);                         // saldo
    }

    /** RealAccountService = prawdziwe konta (mapa nazwa → saldo). */
    static final class RealAccountService implements AccountService {
        private final Map<String, Integer> balances = new LinkedHashMap<>(Map.of("A", 100, "B", 0));

        @Override
        @Transactional
        public void transfer(String from, String to, int amount) {
            if (balances.get(from) < amount) {
                throw new IllegalStateException("Brak środków na koncie " + from + ": " + balances.get(from) + " < " + amount);
            }
            balances.merge(from, -amount, Integer::sum);
            balances.merge(to, amount, Integer::sum);
        }

        @Override
        public int balance(String account) {
            return balances.get(account);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Narzędzia do budowy proxy
    // ---------------------------------------------------------------------------------------------

    /** proxy = utwórz proxy dla interfejsu iface. iface.cast zamiast rzutowania — bez ostrzeżeń kompilatora. */
    static <T> T proxy(Class<T> iface, InvocationHandler handler) {
        return iface.cast(Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, handler));
    }

    /** invokeTarget = wywołaj prawdziwy obiekt i ROZPAKUJ wyjątek, by wywołujący dostał oryginał. */
    static Object invokeTarget(Method method, Object target, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    /** logging = handler logujący: nazwa metody i argumenty przed, wynik po (println = celowy efekt uboczny). */
    static InvocationHandler logging(Object target) {
        return (self, method, args) -> {
            String argsText = args == null ? "" : Arrays.stream(args).map(String::valueOf).collect(Collectors.joining(", "));
            System.out.println("   → " + method.getName() + "(" + argsText + ")");
            Object result = invokeTarget(method, target, args);
            System.out.println("   ↳ " + method.getName() + " = " + result);
            return result;
        };
    }

    /** counting = handler liczący wywołania każdej metody (TreeMap = stała kolejność wydruku). */
    static InvocationHandler counting(Object target, Map<String, Integer> counts) {
        return (self, method, args) -> {
            counts.merge(method.getName(), 1, Integer::sum);
            return invokeTarget(method, target, args);
        };
    }

    /** caching = handler zapamiętujący wyniki: klucz = nazwa metody + argumenty. */
    static InvocationHandler caching(Object target) {
        Map<List<Object>, Object> cache = new HashMap<>();
        return (self, method, args) -> {
            List<Object> key = new ArrayList<>();
            key.add(method.getName());
            if (args != null) {
                key.addAll(Arrays.asList(args));
            }
            if (cache.containsKey(key)) {
                return cache.get(key);
            }
            Object result = invokeTarget(method, target, args);
            cache.put(key, result);
            return result;
        };
    }

    /** transactional = handler „transakcyjny”: BEGIN, potem COMMIT albo ROLLBACK (zapis do dziennika log). */
    static InvocationHandler transactional(Object target, List<String> log) {
        return (self, method, args) -> {
            log.add("BEGIN " + method.getName());
            try {
                Object result = invokeTarget(method, target, args);
                log.add("COMMIT");
                return result;
            } catch (RuntimeException e) {
                log.add("ROLLBACK (" + e.getClass().getSimpleName() + ")");
                throw e;
            }
        };
    }

    public static void main(String[] args) throws NoSuchMethodException {
        title("Annotations06 — dynamiczne proxy");

        beforeHandWritten();     // before: hand-written = PRZED: ręcznie napisany
        loggingProxy();          // logging proxy = proxy logujące
        countingProxy();         // counting proxy = proxy liczące
        cachingProxy();          // caching proxy = proxy z pamięcią podręczną
        transactionProxy();      // transaction proxy = proxy transakcyjne
        unwrapping();            // unwrapping = rozpakowywanie
        interfacesOnly();        // interfaces only = tylko interfejsy
        selfInvocation();        // self-invocation = wywołanie wewnętrzne
        annotationDriven();      // annotation-driven = sterowane adnotacją
        exercises();             // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PRZED: RĘCZNY DEKORATOR
    // =================================================================================================

    /** LoggingPriceService = ręczny dekorator (rekord z polem target): każda metoda powtarza ten sam schemat. */
    record LoggingPriceService(PriceService target) implements PriceService {
        @Override
        public BigDecimal priceOf(String sku) {
            System.out.println("   → priceOf(" + sku + ")");
            return target.priceOf(sku);
        }

        @Override
        public BigDecimal totalOf(List<String> skus) {
            System.out.println("   → totalOf(" + skus + ")");
            return target.totalOf(skus);
        }

        @Override
        public int catalogSize() {
            System.out.println("   → catalogSize()");
            return target.catalogSize();
        }
    }

    /** 1. Ręczny dekorator działa, ale to kopiuj-wklej: interfejs z 30 metodami = 30 kopii tego samego. */
    static void beforeHandWritten() {
        section("1. PRZED: ręczny dekorator");

        PriceService decorated = new LoggingPriceService(new RealPriceService());
        show("wynik", decorated.priceOf("KSI-001"));
        // WYNIK:    → priceOf(KSI-001)
        // WYNIK: wynik → 79.00

        // PUŁAPKA: dekorator trzeba pisać OSOBNO dla każdego interfejsu i każdej metody. Dodasz metodę do interfejsu —
        //   musisz dopisać ją we wszystkich dekoratorach. Proxy robi to samo JEDNYM handlerem dla dowolnego interfejsu.
    }

    // =================================================================================================
    // 2. PROXY LOGUJĄCE
    // =================================================================================================

    /** 2. Jeden handler logujący pasuje do KAŻDEGO interfejsu — tu do PriceService i do List. */
    static void loggingProxy() {
        section("2. Proxy + InvocationHandler: logowanie");

        PriceService logged = proxy(PriceService.class, logging(new RealPriceService()));
        show("priceOf", logged.priceOf("ELE-001"));
        // WYNIK:    → priceOf(ELE-001)
        // WYNIK:    ↳ priceOf = 5499.99
        // WYNIK: priceOf → 5499.99
        show("catalogSize", logged.catalogSize());
        // WYNIK:    → catalogSize()
        // WYNIK:    ↳ catalogSize = 14
        // WYNIK: catalogSize → 14
        // Ten sam handler logging pasuje do DOWOLNEGO interfejsu — także do List z JDK (ćwiczenie 3).

        Object[][] captured = new Object[1][];
        PriceService spy = proxy(PriceService.class, (self, method, args) -> {
            captured[0] = args;
            return 0;                                     // catalogSize zwraca int → Integer 0
        });
        spy.catalogSize();
        show("args dla metody bez parametrów == null?", captured[0] == null);
        // WYNIK: args dla metody bez parametrów == null? → true

        // PUŁAPKA: dla metody bez parametrów args to null, a NIE pusta tablica. args.length albo Arrays.asList(args)
        //   rzuci NullPointerException — zawsze sprawdzaj args == null (jak w logging i caching).
    }

    // =================================================================================================
    // 3. PROXY LICZĄCE WYWOŁANIA
    // =================================================================================================

    /** 3. Licznik wywołań każdej metody — podstawa metryk (np. „ile razy wołano priceOf?”). */
    static void countingProxy() {
        section("3. Proxy liczące wywołania");

        Map<String, Integer> counts = new TreeMap<>();
        PriceService counted = proxy(PriceService.class, counting(new RealPriceService(), counts));
        counted.priceOf("ELE-001");
        counted.priceOf("SPO-002");
        counted.priceOf("ELE-001");
        counted.catalogSize();
        show("liczniki", counts);
        // WYNIK: liczniki → {catalogSize=1, priceOf=3}

        // DOBRA PRAKTYKA: w prawdziwych systemach takie proxy mierzy też CZAS wywołań (biblioteki metryk). Tu
        //   wypisujemy tylko liczniki — czas zmienia się przy każdym uruchomieniu, więc nie nadaje się do przykładu.
    }

    // =================================================================================================
    // 4. PROXY Z PAMIĘCIĄ PODRĘCZNĄ (cache)
    // =================================================================================================

    /** 4. Cache: te same argumenty → wynik z pamięci, bez wołania prawdziwego obiektu (jak @Cacheable). */
    static void cachingProxy() {
        section("4. Proxy z pamięcią podręczną (cache)");

        RealPriceService real = new RealPriceService();
        PriceService cached = proxy(PriceService.class, caching(real));
        show("priceOf(ELE-001) ×3", cached.priceOf("ELE-001") + " " + cached.priceOf("ELE-001") + " "
                + cached.priceOf("ELE-001"));
        show("priceOf(SPO-001)", cached.priceOf("SPO-001"));
        show("prawdziwych obliczeń", real.realCalls);
        // WYNIK: priceOf(ELE-001) ×3 → 5499.99 5499.99 5499.99
        // WYNIK: priceOf(SPO-001) → 64.99
        // WYNIK: prawdziwych obliczeń → 2    ← 4 wywołania, ale tylko 2 różne klucze

        // PUŁAPKA: cache tylko dla metod „czystych” (ten sam argument → ten sam wynik, bez efektów ubocznych) — cache
        //   na metodzie zapisującej zamówienie zgubi wszystkie zapisy poza pierwszym. Argumenty-klucze muszą mieć
        //   poprawne equals/hashCode i nie mogą się zmieniać (zmieniona lista = „zgubiony” wpis w HashMap).
    }

    // =================================================================================================
    // 5. PROXY „TRANSAKCYJNE”
    // =================================================================================================

    /** 5. BEGIN przed wywołaniem, COMMIT po sukcesie, ROLLBACK po wyjątku — szkielet @Transactional. */
    static void transactionProxy() {
        section("5. Proxy „transakcyjne”: BEGIN / COMMIT / ROLLBACK");

        List<String> log = new ArrayList<>();
        AccountService accounts = proxy(AccountService.class, transactional(new RealAccountService(), log));
        accounts.transfer("A", "B", 30);
        expectThrows("transfer A → B 500", () -> accounts.transfer("A", "B", 500));
        // WYNIK: ✔ transfer A → B 500 → rzucono IllegalStateException: Brak środków na koncie A: 70 < 500
        show("dziennik", log);
        // WYNIK: dziennik → [BEGIN transfer, COMMIT, BEGIN transfer, ROLLBACK (IllegalStateException)]

        // W Springu handler woła menedżera transakcji, a ROLLBACK wykonuje baza danych. Wywołujący dostał ORYGINALNY
        // IllegalStateException, bo invokeTarget go rozpakował (sekcja 6).
        // PUŁAPKA: nasz handler otwiera „transakcję” dla KAŻDEJ metody, także dla balance — w sekcji 9 zawęzimy to
        //   do metod z adnotacją @Transactional.
    }

    // =================================================================================================
    // 6. ROZPAKOWYWANIE InvocationTargetException
    // =================================================================================================

    /** 6. Handler, który nie rozpakowuje wyjątku, zamienia go w UndeclaredThrowableException. */
    static void unwrapping() {
        section("6. Rozpakowywanie InvocationTargetException");

        RealAccountService target = new RealAccountService();
        AccountService naive = proxy(AccountService.class, (self, method, args) -> method.invoke(target, args));
        expectThrows("naiwne proxy: transfer 500", () -> naive.transfer("A", "B", 500));
        // WYNIK: ✔ naiwne proxy: transfer 500 → rzucono UndeclaredThrowableException: (brak komunikatu)
        try {
            naive.transfer("A", "B", 500);
        } catch (RuntimeException e) {
            for (Throwable t = e; t != null; t = t.getCause()) {
                System.out.println("   " + t.getClass().getSimpleName());
            }
        }
        // WYNIK:    UndeclaredThrowableException
        // WYNIK:    InvocationTargetException
        // WYNIK:    IllegalStateException

        // Dlaczego? InvocationTargetException jest SPRAWDZANY, a transfer go nie deklaruje — proxy opakowuje go znowu.
        // DOBRA PRAKTYKA: w handlerze zawsze łap InvocationTargetException i rzucaj e.getCause() (invokeTarget).
        //   Wtedy kod wołający proxy widzi dokładnie te same wyjątki, co przy wołaniu prawdziwego obiektu.
    }

    // =================================================================================================
    // 7. TYLKO INTERFEJSY; equals / hashCode / toString
    // =================================================================================================

    /** 7. Proxy implementuje interfejs, NIE rozszerza klasy. A metody z Object też trafiają do handlera. */
    static void interfacesOnly() {
        section("7. Tylko interfejsy; equals/hashCode/toString przez handler");

        PriceService logged = proxy(PriceService.class, logging(new RealPriceService()));
        show("instanceof PriceService", logged instanceof PriceService);
        show("instanceof RealPriceService", (Object) logged instanceof RealPriceService);
        show("Proxy.isProxyClass(getClass())", Proxy.isProxyClass(logged.getClass()));   // getClass NIE idzie do handlera
        // WYNIK: instanceof PriceService → true
        // WYNIK: instanceof RealPriceService → false
        // WYNIK: Proxy.isProxyClass(getClass()) → true

        expectThrows("proxy dla KLASY", () -> Proxy.newProxyInstance(RealPriceService.class.getClassLoader(),
                new Class<?>[]{RealPriceService.class}, (self, method, args) -> null));
        // WYNIK: ✔ proxy dla KLASY → rzucono IllegalArgumentException: t19_annotations_reflection.Annotations06DynamicProxy$RealPriceService is not an interface

        show("toString() proxy", logged.toString());
        // WYNIK:    → toString()
        // WYNIK:    ↳ toString = RealPriceService
        // WYNIK: toString() proxy → RealPriceService

        PriceService broken = proxy(PriceService.class, (self, method, args) -> null);
        expectThrows("hashCode() gdy handler zwraca null", broken::hashCode);
        // WYNIK: ✔ hashCode() gdy handler zwraca null → rzucono NullPointerException: Cannot invoke "java.lang.Integer.intValue()" because the return value of "java.lang.reflect.InvocationHandler.invoke(Object, java.lang.reflect.Method, Object[])" is null
        // Klasa proxy rozpakowuje wynik handlera (Integer → int) — a null rozpakować się nie da (helpful NPE, Java 14+).

        // PUŁAPKA: equals, hashCode i toString też idą przez handler. Handler „na skróty” (zawsze null) psuje proxy
        //   w HashSet/HashMap i w logach; null dla typu prostego (int hashCode) = NullPointerException.
        // Klasę bez interfejsu „proxują” biblioteki generujące bajtkod podklasy (ByteBuddy, CGLIB — Spring Boot
        // domyślnie używa CGLIB). Dlatego klas i metod final nie da się w ten sposób podmienić.
    }

    // =================================================================================================
    // 8. WYWOŁANIE WEWNĘTRZNE OMIJA PROXY (self-invocation)
    // =================================================================================================

    /** 8. totalOf woła this.priceOf — „this” to prawdziwy obiekt, nie proxy. Handler tego nie widzi. */
    static void selfInvocation() {
        section("8. Wywołanie wewnętrzne omija proxy");

        RealPriceService real = new RealPriceService();
        Map<String, Integer> counts = new TreeMap<>();
        PriceService counted = proxy(PriceService.class, counting(real, counts));
        show("totalOf(3 produkty)", counted.totalOf(List.of("ELE-001", "SPO-001", "KSI-001")));
        show("liczniki proxy", counts);
        show("prawdziwe priceOf", real.realCalls);
        // WYNIK: totalOf(3 produkty) → 5643.98
        // WYNIK: liczniki proxy → {totalOf=1}    ← trzy wywołania priceOf „przeszły bokiem”
        // WYNIK: prawdziwe priceOf → 3

        // PUŁAPKA: to samo w Springu: metoda z @Transactional albo @Cacheable wołana z INNEJ metody tej samej klasy
        //   (this.metoda()) działa bez transakcji i bez cache — adnotacja „nie działa”, bo proxy nie bierze udziału.
        // DOBRA PRAKTYKA: przenieś metodę do osobnego obiektu (beana) i wołaj ją przez jego proxy; ewentualnie
        //   użyj pełnego tkania aspektów (AspectJ), które zmienia sam bajtkod klasy, a nie dokłada proxy.
    }

    // =================================================================================================
    // 9. ADNOTACJA + PROXY = @Transactional
    // =================================================================================================

    /** 9. Handler sprawdza adnotację na metodzie KLASY celu. Parametr method to metoda INTERFEJSU! */
    static void annotationDriven() throws NoSuchMethodException {
        section("9. Adnotacja + proxy: transakcja tylko dla @Transactional");

        Method interfaceMethod = AccountService.class.getMethod("transfer", String.class, String.class, int.class);
        Method classMethod = RealAccountService.class.getMethod("transfer", String.class, String.class, int.class);
        show("@Transactional na metodzie interfejsu?", interfaceMethod.isAnnotationPresent(Transactional.class));
        show("@Transactional na metodzie klasy?", classMethod.isAnnotationPresent(Transactional.class));
        // WYNIK: @Transactional na metodzie interfejsu? → false
        // WYNIK: @Transactional na metodzie klasy? → true

        RealAccountService target = new RealAccountService();
        List<String> log = new ArrayList<>();
        InvocationHandler tx = transactional(target, log);
        AccountService accounts = proxy(AccountService.class, (self, method, args) -> {
            Method implementation = target.getClass().getMethod(method.getName(), method.getParameterTypes());
            return implementation.isAnnotationPresent(Transactional.class)
                    ? tx.invoke(self, method, args)          // z transakcją
                    : invokeTarget(method, target, args);    // bez transakcji
        });
        accounts.transfer("A", "B", 40);
        show("saldo A", accounts.balance("A"));
        show("dziennik", log);
        // WYNIK: saldo A → 60
        // WYNIK: dziennik → [BEGIN transfer, COMMIT]    ← balance poszło bez transakcji

        // PUŁAPKA: handler dostaje metodę z INTERFEJSU, a adnotacja z klasy celu jest na niej niewidoczna — trzeba
        //   odnaleźć metodę w klasie celu (getMethod po nazwie i typach). Spring robi to samo („most specific method”).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Proxy.newProxyInstance(loader, new Class<?>[]{Interfejs.class}, handler) — tylko INTERFEJSY.
     *   • handler: (proxy, method, args) → przed; method.invoke(target, args); po. args == null bez parametrów!
     *   • ZAWSZE rozpakuj InvocationTargetException (throw e.getCause()) — inaczej UndeclaredThrowableException.
     *   • Logowanie, liczniki, cache (metody „czyste”), transakcje, retry. equals/hashCode/toString też przez handler.
     *   • Wywołanie wewnętrzne (this.metoda()) omija proxy — tak samo @Transactional/@Cacheable w Springu.
     *   • method w handlerze to metoda interfejsu — adnotacje z klasy celu odczytaj z target.getClass().getMethod(...).
     *   • Klasy bez interfejsu: ByteBuddy/CGLIB (podklasa w bajtkodzie); final nie da się podmienić.
     *
     * PYTANIA KONTROLNE:
     *   1. Co dzieje się po wywołaniu dowolnej metody na obiekcie zwróconym przez Proxy.newProxyInstance?
     *   2. Co wypisze:  System.out.println(proxy instanceof RealPriceService);  dla proxy interfejsu PriceService?
     *   3. ZNAJDŹ BŁĄD:  handler = (p, m, a) -> m.invoke(target, a);  a wywołujący dostaje UndeclaredThrowableException.
     *   4. Dlaczego metoda z @Transactional wołana przez this.metoda() z tej samej klasy działa bez transakcji?
     *   5. ZNAJDŹ BŁĄD:  handler loguje  "args: " + a.length  — dla metody size() leci NullPointerException.
     *   6. Co wypisze:  System.out.println(proxy.toString());  gdy handler zawsze zwraca "X"?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** Fetcher = pobieracz danych (do ćwiczenia 4). */
    interface Fetcher {
        String fetch(String key);
    }

    /** FlakyFetcher = „chimeryczny” pobieracz: pierwsze failures prób kończy wyjątkiem. attempts = liczba prób. */
    static final class FlakyFetcher implements Fetcher {
        private final int failures;
        int attempts;

        FlakyFetcher(int failures) {
            this.failures = failures;
        }

        @Override
        public String fetch(String key) {
            attempts++;
            if (attempts <= failures) {
                throw new IllegalStateException("awaria nr " + attempts);
            }
            return "dane:" + key;
        }
    }

    static final Set<String> MUTATORS = Set.of("add", "addAll", "remove", "removeAll", "removeIf", "retainAll",
            "replaceAll", "set", "clear", "sort");

    static String loggingScenario(BiFunction<PriceService, List<String>, PriceService> factory) {
        List<String> log = new ArrayList<>();
        PriceService proxied = factory.apply(new RealPriceService(), log);
        return proxied.priceOf("KSI-001") + " | " + proxied.catalogSize() + " | " + log;
    }

    static String cachingScenario(Function<PriceService, PriceService> factory) {
        RealPriceService real = new RealPriceService();
        PriceService proxied = factory.apply(real);
        String prices = proxied.priceOf("ELE-001") + " " + proxied.priceOf("ELE-001") + " "
                + proxied.priceOf("ELE-001") + " " + proxied.priceOf("SPO-001");
        return prices + " | realCalls=" + real.realCalls;
    }

    static String readOnlyScenario(Function<List<String>, List<String>> factory) {
        List<String> view = factory.apply(new ArrayList<>(List.of("a", "b")));
        String result = view.get(0) + ", " + view.size() + ", " + view.contains("b");
        try {
            view.add("c");
            return result + ", dodano";
        } catch (UnsupportedOperationException e) {
            return result + ", " + e.getMessage();
        }
    }

    static String retryScenario(int failures, BiFunction<Fetcher, Integer, Fetcher> factory) {
        FlakyFetcher real = new FlakyFetcher(failures);
        try {
            return factory.apply(real, 3).fetch("x") + " po " + real.attempts + " próbach";
        } catch (IllegalStateException e) {
            return "błąd „" + e.getMessage() + "” po " + real.attempts + " próbach";
        }
    }

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: logowanie nazw", "79.00 | 14 | [priceOf, catalogSize]",
                () -> loggingScenario(Annotations06DynamicProxy::exercise1));
        Check.equal("ćw. 2: cache", "5499.99 5499.99 5499.99 64.99 | realCalls=2",
                () -> cachingScenario(Annotations06DynamicProxy::exercise2));
        Check.equal("ćw. 3: lista tylko do odczytu", "a, 2, true, tylko do odczytu: add",
                () -> readOnlyScenario(Annotations06DynamicProxy::exercise3));
        Check.equal("ćw. 4a: retry — sukces", "dane:x po 3 próbach",
                () -> retryScenario(2, Annotations06DynamicProxy::exercise4));
        Check.equal("ćw. 4b: retry — porażka", "błąd „awaria nr 3” po 3 próbach",
                () -> retryScenario(5, Annotations06DynamicProxy::exercise4));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "79.00 | 14 | [priceOf, catalogSize]",
                () -> loggingScenario(Annotations06DynamicProxy::solution1));
        Check.equal("ćw. 2 (wzorzec)", "5499.99 5499.99 5499.99 64.99 | realCalls=2",
                () -> cachingScenario(Annotations06DynamicProxy::solution2));
        Check.equal("ćw. 3 (wzorzec)", "a, 2, true, tylko do odczytu: add",
                () -> readOnlyScenario(Annotations06DynamicProxy::solution3));
        Check.equal("ćw. 4a (wzorzec)", "dane:x po 3 próbach",
                () -> retryScenario(2, Annotations06DynamicProxy::solution4));
        Check.equal("ćw. 4b (wzorzec)", "błąd „awaria nr 3” po 3 próbach",
                () -> retryScenario(5, Annotations06DynamicProxy::solution4));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć proxy PriceService, które przed każdym wywołaniem dopisuje nazwę metody do log,
     * a potem woła target i zwraca jego wynik.
     * Podpowiedź: proxy(PriceService.class, (self, method, args) -> { log.add(...); return invokeTarget(...); }).
     */
    static PriceService exercise1(PriceService target, List<String> log) {
        // TODO: twoje rozwiązanie
        return target;
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć proxy PriceService z pamięcią podręczną — napisz handler SAM, bez caching().
     * Klucz: nazwa metody + argumenty (List.of(...) z Arrays.asList(args)). Uwaga na args == null.
     * Podpowiedź: Map z kluczem List<Object>; containsKey → get, inaczej invokeTarget + put.
     */
    static PriceService exercise2(PriceService target) {
        // TODO: twoje rozwiązanie
        return target;
    }

    /**
     * ĆWICZENIE 3 (średnie): widok listy tylko do odczytu jako proxy interfejsu List (połącz z t12_collections).
     * Metody z MUTATORS rzucają UnsupportedOperationException("tylko do odczytu: " + nazwa), reszta działa.
     * Podpowiedź: proxy(List.class, ...) zwraca surowe List — rzutowanie z @SuppressWarnings("unchecked") na zmiennej.
     * Zauważ ograniczenie: iterator().remove() i subList() „uciekają” spod kontroli takiego proxy.
     */
    static List<String> exercise3(List<String> list) {
        // TODO: twoje rozwiązanie
        return list;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): proxy ponawiające (retry): gdy target rzuci wyjątek, spróbuj ponownie — łącznie
     * najwyżej maxAttempts prób. Po ostatniej nieudanej próbie rzuć ORYGINALNY wyjątek (rozpakowany).
     * Podpowiedź: pętla for w handlerze; catch (InvocationTargetException e) { last = e.getCause(); }; throw last.
     */
    static Fetcher exercise4(Fetcher target, Integer maxAttempts) {
        // TODO: twoje rozwiązanie
        return target;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static PriceService solution1(PriceService target, List<String> log) {
        return proxy(PriceService.class, (self, method, args) -> {
            log.add(method.getName());
            return invokeTarget(method, target, args);
        });
    }

    static PriceService solution2(PriceService target) {
        Map<List<Object>, Object> cache = new HashMap<>();
        return proxy(PriceService.class, (self, method, args) -> {
            List<Object> key = List.of(method.getName(), args == null ? List.of() : Arrays.asList(args));
            if (!cache.containsKey(key)) {
                cache.put(key, invokeTarget(method, target, args));
            }
            return cache.get(key);
        });
    }

    static List<String> solution3(List<String> list) {
        @SuppressWarnings("unchecked")   // proxy implementuje List, a elementami są Stringi z list
        List<String> view = proxy(List.class, (self, method, args) -> {
            if (MUTATORS.contains(method.getName())) {
                throw new UnsupportedOperationException("tylko do odczytu: " + method.getName());
            }
            return invokeTarget(method, list, args);
        });
        return view;
    }

    static Fetcher solution4(Fetcher target, Integer maxAttempts) {
        return proxy(Fetcher.class, (self, method, args) -> {
            Throwable last = null;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    return method.invoke(target, args);
                } catch (InvocationTargetException e) {
                    last = e.getCause();
                }
            }
            throw last;
        });
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Trafia do handler.invoke(proxy, metoda, argumenty): zwykle „przed”, method.invoke(target, args), „po”.
     *   2. false — proxy implementuje tylko interfejs PriceService; nie jest podklasą RealPriceService.
     *   3. Brak rozpakowania: method.invoke opakowuje wyjątek w InvocationTargetException (sprawdzany, niezadeklarowany),
     *      więc proxy opakowuje go jeszcze raz. Poprawnie: catch (InvocationTargetException e) { throw e.getCause(); }.
     *   4. this.metoda() to wywołanie na prawdziwym obiekcie, nie na proxy — handler (transakcja) w ogóle nie bierze
     *      udziału. Rozwiązanie: metoda w innym beanie, wołana przez jego proxy.
     *   5. Dla metod bez parametrów args == null. Trzeba: args == null ? 0 : args.length.
     *   6. X — toString też przechodzi przez handler.
     */
    // </editor-fold>
}
