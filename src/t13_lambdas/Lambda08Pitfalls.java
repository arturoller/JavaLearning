package t13_lambdas;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Employee;
import helpers.model.Product;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Pułapki lambd — wyjątki sprawdzane, przeciążenia, debugowanie, rekurencja, null i var
 *        (pitfalls = pułapki)
 *
 * W SKRÓCIE:
 *   Lambdy są krótkie i wygodne, ale mają kilka ostrych krawędzi. Najczęstsze:
 *   metody z wyjątkami SPRAWDZANYMI nie pasują do Function/Consumer; za długie lambdy są nieczytelne i trudne
 *   do debugowania; efekty uboczne dają niespodzianki; przeciążone metody bywają niejednoznaczne; lambd nie
 *   porównuje się przez == ani equals; lambda nie może wprost wywołać samej siebie; zwrócony null wybucha
 *   daleko od przyczyny; a {@code var f = x -> ...} się nie kompiluje. Ta lekcja pokazuje każdą pułapkę i jej obejście.
 *
 * ANALOGIA: instrukcja obsługi nowego, szybkiego samochodu.
 *   Samochód jest świetny, ale w instrukcji jest rozdział „Czego NIE robić”: nie tankuj diesla do benzyny
 *   (wyjątek sprawdzany w Function), nie zostawiaj kluczyków w stacyjce (współdzielony stan), nie mylą Cię dwa
 *   podobne przyciski (przeciążenia). Kto przeczyta ten rozdział raz, oszczędzi sobie wielu wizyt w warsztacie.
 *
 * JAK TO DZIAŁA:
 *   Metoda apply w {@code Function<T, R>} NIE ma „throws” w nagłówku. Ciało lambdy JEST ciałem tej metody —
 *   więc nie może wyrzucić wyjątku sprawdzanego (IOException...), nawet jeśli metoda, w której lambdę napisano,
 *   ma „throws IOException”. Wyjątek trzeba złapać W lambdzie albo opakować w niesprawdzany (UncheckedIOException),
 *   najlepiej jedną metodą pomocniczą, która zachowuje oryginał jako przyczynę (cause).
 *
 * SŁÓWKA:
 *   pitfall = pułapka; checked = sprawdzany (wyjątek); unchecked = niesprawdzany; throwing = rzucający; wrap = opakuj;
 *   cause = przyczyna; stack trace = ślad stosu; side effect = efekt uboczny; overload = przeciążenie;
 *   ambiguous = niejednoznaczny; submit = przekaż (do wykonania); listener = słuchacz (obiekt powiadamiany o zdarzeniu);
 *   factorial = silnia; digit = cyfra; initialized = zainicjalizowany; infer = wydedukować; config = konfiguracja.
 *
 * ZOBACZ TEŻ: t10_exceptions/Exceptions02CheckedUnchecked (wyjątki sprawdzane), t10_exceptions/Exceptions06ChainingWrapping
 *             (opakowywanie z przyczyną), Lambda06ClosuresScope (efekty uboczne, stan), t14_optional (zamiast null),
 *             t16_streams/Streams17SideEffectsPitfalls (te same pułapki w streamach).
 * </pre>
 */
public class Lambda08Pitfalls {

    /** FILES = pliki. Udajemy system plików (bez prawdziwego dysku — wynik zawsze ten sam). */
    private static final Map<String, String> FILES = Map.of(
            "app.cfg", "port=8080",
            "db.cfg", "url=jdbc:h2:mem");

    /** THOUSAND = tysiąc. */
    private static final BigDecimal THOUSAND = new BigDecimal("1000");

    /**
     * FACTORIAL = silnia (n! = 1·2·…·n) jako pole z lambdą, która wywołuje samą siebie.
     * Działa tylko z nazwą KWALIFIKOWANĄ (Lambda08Pitfalls.FACTORIAL) — sekcja 8.
     */
    private static final IntUnaryOperator FACTORIAL = n -> n <= 1 ? 1 : n * Lambda08Pitfalls.FACTORIAL.applyAsInt(n - 1);

    public static void main(String[] args) {
        title("Lambda08 — pułapki lambd");

        checkedExceptions();         // checked exceptions = wyjątki sprawdzane w lambdzie
        uncheckedWrapper();          // unchecked wrapper = opakowanie w wyjątek niesprawdzany
        overlongLambdas();           // overlong lambdas = za długie lambdy
        sideEffectsAndState();       // side effects and state = efekty uboczne i stan
        readingStackTraces();        // reading stack traces = czytanie śladu stosu
        overloadAmbiguity();         // overload ambiguity = niejednoznaczne przeciążenia
        comparingLambdas();          // comparing lambdas = porównywanie lambd
        recursionInLambda();         // recursion in lambda = rekurencja w lambdzie
        nullFromFunctions();         // null from functions = null zwracany z funkcji
        lambdasAndVar();             // lambdas and var = lambdy i var
        exercises();                 // exercises = ćwiczenia
    }

    // =================================================================================================
    // Metody pomocnicze używane w lekcji
    // =================================================================================================

    /** loadConfig = wczytaj konfigurację. Rzuca wyjątek SPRAWDZANY IOException, gdy „pliku” nie ma. */
    static String loadConfig(String name) throws IOException {
        String content = FILES.get(name);
        if (content == null) {
            throw new IOException("Brak pliku: " + name);
        }
        return content;
    }

    /** mapAll = przekształć wszystkie (mini-wersja map ze streamów). */
    static <T, R> List<R> mapAll(List<T> items, Function<? super T, ? extends R> mapper) {
        List<R> result = new ArrayList<>();
        for (T item : items) {
            result.add(mapper.apply(item));
        }
        return result;
    }

    // =================================================================================================
    // 1. WYJĄTKI SPRAWDZANE W LAMBDZIE
    // =================================================================================================

    /**
     * 1. Function.apply, Consumer.accept, Predicate.test... nie deklarują żadnych wyjątków sprawdzanych.
     * Lambda, która woła metodę z „throws IOException”, się nie skompiluje. Pierwsze rozwiązanie: try/catch W lambdzie.
     */
    static void checkedExceptions() {
        section("1. Wyjątki sprawdzane w lambdzie — problem i try/catch w środku");

        //   Function<String, String> loader = name -> loadConfig(name);
        //   → „unreported exception IOException; must be caught or declared to be thrown”
        //     (= „niezgłoszony wyjątek IOException — trzeba go złapać albo zadeklarować”)
        //
        // PUŁAPKA: dopisanie „throws IOException” do metody, W KTÓREJ piszesz lambdę, NIC nie da. Ciało lambdy
        //   to ciało metody apply interfejsu Function — a tam „throws” nie ma i dopisać się go nie da.

        // Rozwiązanie 1: złap wyjątek w lambdzie i zdecyduj, co zrobić (tu: wartość zastępcza):
        Function<String, String> safeLoader = name -> {
            try {
                return loadConfig(name);
            } catch (IOException e) {
                return "(brak: " + e.getMessage() + ")";
            }
        };
        show("wczytane", mapAll(List.of("app.cfg", "nieznany.cfg"), safeLoader));
        // WYNIK: wczytane → [port=8080, (brak: Brak pliku: nieznany.cfg)]

        // Działa, ale: lambda urosła z 1 do 7 linii, a przy każdej kolejnej takiej lambdzie powtarzasz try/catch.
        // PUŁAPKA: nie „połykaj” wyjątku pustym catch { } — błąd zniknie bez śladu. Albo obsłuż go sensownie
        //   (wartość zastępcza, log), albo opakuj i rzuć dalej (sekcja 2).
        note("Zasada: wyjątek sprawdzany trzeba obsłużyć W lambdzie — nie da się go „przepuścić” przez Function.");
        // WYNIK: ℹ Zasada: wyjątek sprawdzany trzeba obsłużyć W lambdzie — nie da się go „przepuścić” przez Function.
    }

    // =================================================================================================
    // 2. POMOCNIK unchecked(ThrowingFunction) — OPAKOWANIE Z ZACHOWANIEM PRZYCZYNY
    // =================================================================================================

    /**
     * {@code ThrowingFunction<T, R>} = funkcja, która może rzucić wyjątek. Własny interfejs funkcyjny — jak Function,
     * ale z „throws Exception”. (Tak samo zbudowany jest Console.ThrowingAction z helpers.)
     */
    @FunctionalInterface
    interface ThrowingFunction<T, R> {
        R apply(T value) throws Exception;
    }

    /**
     * unchecked = niesprawdzany. Zamienia ThrowingFunction na zwykłą Function: wyjątki sprawdzane opakowuje
     * w niesprawdzane, ZACHOWUJĄC oryginał jako przyczynę (cause) — nic nie ginie.
     */
    static <T, R> Function<T, R> unchecked(ThrowingFunction<T, R> f) {
        return value -> {
            try {
                return f.apply(value);
            } catch (IOException e) {
                throw new UncheckedIOException(e.getMessage(), e);        // specjalny typ dla IOException
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();                        // przywróć flagę przerwania (helpers/Sleep)
                throw new IllegalStateException("Przerwano wątek", e);
            } catch (RuntimeException e) {
                throw e;                                                   // niesprawdzane — przepuść bez zmian
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);             // pozostałe sprawdzane — ogólne opakowanie
            }
        };
    }

    /**
     * 2. Rozwiązanie 2: metoda pomocnicza unchecked(...). Lambda zostaje krótka (albo jest referencją do metody),
     * a opakowanie wyjątku jest napisane RAZ.
     */
    static void uncheckedWrapper() {
        section("2. Pomocnik unchecked(ThrowingFunction) — opakuj, zachowaj przyczynę");

        Function<String, String> loader = unchecked(Lambda08Pitfalls::loadConfig);   // referencja do metody z throws — OK
        show("wczytane przez unchecked", mapAll(List.of("app.cfg", "db.cfg"), loader));
        // WYNIK: wczytane przez unchecked → [port=8080, url=jdbc:h2:mem]

        expectThrows("brakujący plik", () -> loader.apply("brak.cfg"));
        // WYNIK: ✔ brakujący plik → rzucono UncheckedIOException: Brak pliku: brak.cfg

        try {
            loader.apply("brak.cfg");
        } catch (UncheckedIOException e) {
            show("przyczyna (getCause)", e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
        }
        // WYNIK: przyczyna (getCause) → IOException: Brak pliku: brak.cfg
        // Oryginalny wyjątek jest zachowany — w śladzie stosu zobaczysz „Caused by: java.io.IOException: Brak pliku...”.

        // DOBRA PRAKTYKA: typ opakowania ma mówić prawdę — IOException → UncheckedIOException, a nie przypadkowy
        //   IllegalArgumentException. InterruptedException wymaga przywrócenia flagi przerwania (t21_concurrency).
        // PUŁAPKA: czasem lepiej w ogóle nie używać lambdy — zwykła pętla for może mieć „throws IOException”
        //   w metodzie i przepuścić wyjątek bez żadnych sztuczek (t16_streams/Streams01Intro, sekcja 9).
    }

    // =================================================================================================
    // 3. ZA DŁUGIE LAMBDY → METODA PRYWATNA + REFERENCJA
    // =================================================================================================

    /** productLabel = etykieta produktu. Wyciągnięte ciało długiej lambdy — z nazwą, którą można przeczytać i przetestować. */
    private static String productLabel(Product p) {
        String status;
        if (p.stock() == 0) {
            status = "BRAK";
        } else if (p.stock() < 5) {
            status = "OSTATNIE SZTUKI";
        } else {
            status = "DOSTĘPNY";
        }
        String segment = p.price().compareTo(THOUSAND) >= 0 ? "premium" : "standard";   // segment = segment rynku
        return p.name() + " [" + status + ", " + segment + "]";
    }

    /**
     * 3. Lambda na 10+ linii to zły znak: trudno ją przeczytać, nie da się jej osobno przetestować,
     * a w śladzie stosu ma nazwę w stylu lambda$metoda$3 zamiast czytelnej nazwy.
     */
    static void overlongLambdas() {
        section("3. Za długie lambdy — wyciągnij metodę, podaj referencję");

        // PRZED (fragment — tak NIE pisz):
        //   products.forEach(p -> {
        //       String status;
        //       if (p.stock() == 0) { status = "BRAK"; }
        //       else if (p.stock() < 5) { status = "OSTATNIE SZTUKI"; }
        //       else { status = "DOSTĘPNY"; }
        //       String segment = p.price().compareTo(THOUSAND) >= 0 ? "premium" : "standard";
        //       labels.add(p.name() + " [" + status + ", " + segment + "]");
        //   });
        //
        // PO: ciało przeniesione do metody productLabel, a w miejscu użycia — czytelna referencja:
        List<String> labels = mapAll(SampleData.products().subList(0, 4), Lambda08Pitfalls::productLabel);
        showEach("etykiety", labels);
        // WYNIK: etykiety (liczba elementów: 4):
        // WYNIK: • Laptop Pro 14 [DOSTĘPNY, premium]
        // WYNIK: • Smartfon X [BRAK, premium]
        // WYNIK: • Słuchawki BT [DOSTĘPNY, standard]
        // WYNIK: • Monitor 27 cali [OSTATNIE SZTUKI, premium]

        // DOBRA PRAKTYKA: lambda ma 1–3 linie. Dłuższa logika → metoda prywatna z dobrą nazwą + referencja.
        //   Zyskujesz: nazwę, która tłumaczy „co”, możliwość testu, czytelny ślad stosu, a miejsce użycia mieści się w linii.
    }

    // =================================================================================================
    // 4. EFEKTY UBOCZNE I STAN WSPÓŁDZIELONY
    // =================================================================================================

    /** keep = zostaw. Nowa lista z elementami spełniającymi warunek. */
    static <T> List<T> keep(List<T> items, Predicate<? super T> condition) {
        List<T> result = new ArrayList<>();
        for (T item : items) {
            if (condition.test(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 4. Lambda z ukrytym STANEM daje różne wyniki dla tych samych danych — zależnie od tego, ile razy i w jakiej
     * kolejności ją wywołano. Predykat powinien być CZYSTĄ funkcją: ten sam element → zawsze ta sama odpowiedź.
     */
    static void sideEffectsAndState() {
        section("4. Efekty uboczne — predykat ze stanem to tykająca bomba");

        AtomicInteger seen = new AtomicInteger();                   // seen = widziane
        Predicate<String> firstTwo = w -> seen.incrementAndGet() <= 2;   // „przepuść pierwsze dwa”

        show("pierwsze użycie", keep(List.of("a", "b", "c"), firstTwo));
        show("drugie użycie (inne dane)", keep(List.of("x", "y", "z"), firstTwo));
        // WYNIK: pierwsze użycie → [a, b]
        // WYNIK: drugie użycie (inne dane) → []    ← licznik nie wyzerował się — predykat „pamięta” poprzednie użycie

        // Ten sam predykat, podobne dane, zupełnie inny wynik. Gdyby ktoś użył go w kodzie równoległym (wiele wątków),
        // wynik zależałby jeszcze od szczęścia. Takie błędy są bardzo trudne do znalezienia.
        // DOBRA PRAKTYKA: lambdy przekazywane do filtrowania, sortowania, przekształcania — bez stanu i bez efektów ubocznych.
        //   Jeśli potrzebujesz „pierwszych dwóch”, napisz to wprost (subList, pętla z licznikiem; w streamach: limit(2)).
        // Efekt uboczny jest w porządku tam, gdzie jest CELEM: Consumer w forEach (wypisz, zapisz), Runnable.
    }

    // =================================================================================================
    // 5. DEBUGOWANIE: JAK CZYTAĆ ŚLAD STOSU Z LAMBDĄ
    // =================================================================================================

    /**
     * 5. Lambda nie ma własnej nazwy, więc kompilator tworzy dla niej ukrytą metodę o nazwie
     * {@code lambda$nazwaMetody$numer} — gdzie nazwaMetody to metoda, w której lambdę napisano,
     * a numer to kolejny numer lambdy w klasie. Ta nazwa pojawia się w śladzie stosu.
     */
    static void readingStackTraces() {
        section("5. Debugowanie — nazwy lambda$metoda$N w śladzie stosu");

        List<String> input = List.of("10", "20", "x");
        try {
            List<Integer> parsed = new ArrayList<>();
            input.forEach(s -> parsed.add(Integer.parseInt(s)));        // "x" wybuchnie
        } catch (NumberFormatException e) {
            show("komunikat", e.getMessage());
            // getStackTrace = pobierz ślad stosu (tablica ramek — od miejsca błędu w górę)
            for (StackTraceElement frame : e.getStackTrace()) {
                if (frame.getClassName().equals(Lambda08Pitfalls.class.getName())) {   // tylko ramki z naszej klasy
                    // numer lambdy zastępujemy literą N — zależy od liczby lambd w pliku i zmienia się przy edycji
                    System.out.println("   ramka z naszej klasy: " + frame.getMethodName().replaceAll("\\$\\d+$", "\\$N"));
                }
            }
        }
        // WYNIK: komunikat → For input string: "x"
        // WYNIK: ramka z naszej klasy: lambda$readingStackTraces$N
        // WYNIK: ramka z naszej klasy: readingStackTraces
        // WYNIK: ramka z naszej klasy: main    ← main wywołał readingStackTraces

        // Pełny ślad stosu wygląda mniej więcej tak (numery linii i lambdy mogą być inne):
        //   java.lang.NumberFormatException: For input string: "x"
        //       at java.base/java.lang.NumberFormatException.forInputString(NumberFormatException.java:67)
        //       at java.base/java.lang.Integer.parseInt(Integer.java:668)
        //       at java.base/java.lang.Integer.parseInt(Integer.java:786)
        //       at t13_lambdas.Lambda08Pitfalls.lambda$readingStackTraces$4(Lambda08Pitfalls.java:283)  ← CIAŁO lambdy
        //       at java.base/java.lang.Iterable.forEach(Iterable.java:75)                                        ← kto ją wywołał
        //       at t13_lambdas.Lambda08Pitfalls.readingStackTraces(Lambda08Pitfalls.java:283)     ← gdzie ją przekazano
        //       at t13_lambdas.Lambda08Pitfalls.main(Lambda08Pitfalls.java:81)
        //   (Lambda i wywołanie forEach mają TEN SAM numer linii, bo stoją w jednej linii kodu.)
        // JAK CZYTAĆ: od góry szukaj pierwszej linii z TWOJEGO pakietu. lambda$readingStackTraces$4 = „czwarta lambda
        //   w klasie, napisana w metodzie readingStackTraces”; numer linii w nawiasie wskazuje ją dokładnie.
        // DOBRA PRAKTYKA: przy wielu lambdach w jednej linii trudno zgadnąć, która wybuchła — pisz każdą operację
        //   w osobnej linii albo wyciągnij logikę do nazwanej metody (sekcja 3) — wtedy w śladzie zobaczysz jej nazwę.
        //   W IntelliJ breakpoint w linii z lambdą pozwala wybrać: zatrzymaj w linii czy WEWNĄTRZ lambdy.
    }

    // =================================================================================================
    // 6. NIEJEDNOZNACZNE PRZECIĄŻENIA: Runnable kontra Callable
    // =================================================================================================

    /** submit = przekaż do wykonania. Wersja dla Runnable (nic nie zwraca) — jak w ExecutorService. */
    static String submit(Runnable task) {
        return "submit(Runnable)";
    }

    /** submit — wersja dla {@code Callable<T>} (zwraca wynik). */
    static <T> String submit(Callable<T> task) {
        return "submit(Callable)";
    }

    /**
     * 6. ExecutorService (t21_concurrency/Concurrency04Executors) ma dwie metody submit: dla Runnable i dla Callable.
     * Która zostanie wybrana dla danej lambdy? Decyduje KSZTAŁT ciała lambdy — i czasem wynik zaskakuje.
     */
    static void overloadAmbiguity() {
        section("6. Przeciążenia: submit(Runnable) czy submit(Callable)?");

        AtomicInteger counter = new AtomicInteger();
        show("() -> {}", submit(() -> {}));                               // blok bez return — tylko Runnable pasuje
        show("() -> 42", submit(() -> 42));                               // samo wyrażenie-wartość — tylko Callable
        show("() -> counter.incrementAndGet()", submit(() -> counter.incrementAndGet()));
        show("() -> { counter.incrementAndGet(); }", submit(() -> { counter.incrementAndGet(); }));
        // WYNIK: () -> {} → submit(Runnable)
        // WYNIK: () -> 42 → submit(Callable)
        // WYNIK: () -> counter.incrementAndGet() → submit(Callable)    ← niespodzianka!
        // WYNIK: () -> { counter.incrementAndGet(); } → submit(Runnable)
        // Dlaczego trzecia to Callable? Wywołanie metody pasuje do OBU: może być instrukcją (Runnable, wynik wyrzucony)
        // albo wartością (Callable). Gdy pasują obie, Java wybiera wersję zwracającą wartość — uznaje ją za „dokładniejszą”.
        // Ciało blokowe BEZ return (czwarta) pasuje już tylko do Runnable.

        // Gorzej, gdy oba interfejsy zwracają wartość tego samego typu — wtedy błąd kompilacji:
        //   static void process(Supplier<String> s) { }     static void process(Callable<String> c) { }
        //   process(() -> "x");
        //   → „reference to process is ambiguous  both method process(Supplier<String>) and method process(Callable<String>) match”
        // Rozwiązanie: rzutowanie wskazuje wersję jawnie:
        show("(Runnable) () -> counter.incrementAndGet()", submit((Runnable) () -> counter.incrementAndGet()));
        // WYNIK: (Runnable) () -> counter.incrementAndGet() → submit(Runnable)
        // DOBRA PRAKTYKA: projektując własne API, nie przeciążaj metod różnymi interfejsami funkcyjnymi na tej samej
        //   pozycji — nadaj różne nazwy (runTask / callTask). Użytkownik nie będzie musiał znać tych reguł.
    }

    // =================================================================================================
    // 7. NIE PORÓWNUJ LAMBD PRZEZ == ANI equals
    // =================================================================================================

    /** noop = „nic nie rób”. Lambda bez przechwyconych zmiennych. */
    static Runnable noop() {
        return () -> { };
    }

    /** printer = drukarka. Lambda PRZECHWYTUJĄCA parametr text. */
    static Runnable printer(String text) {
        return () -> System.out.println(text);
    }

    /**
     * 7. Lambdy nie nadpisują equals — porównanie przez == albo equals sprawdza tylko, czy to TEN SAM obiekt.
     * A to, czy dwa wyrażenia dadzą ten sam obiekt, jest szczegółem implementacji JVM.
     */
    static void comparingLambdas() {
        section("7. Nie porównuj lambd przez == ani equals");

        Runnable a = () -> System.out.println("x");
        Runnable b = () -> System.out.println("x");                     // identyczny tekst...
        show("a == b / a.equals(b)", (a == b) + " / " + a.equals(b));
        // WYNIK: a == b / a.equals(b) → false / false    ← ...ale dwa różne obiekty

        // Ta sama lambda w kodzie, wywołana dwa razy — wynik zależy od tego, czy lambda coś przechwytuje:
        show("noop() == noop()", noop() == noop());
        show("printer(\"x\") == printer(\"x\")", printer("x") == printer("x"));
        // WYNIK: noop() == noop() → true      ← ta JVM (HotSpot) zwraca jeden wspólny obiekt dla lambdy bez przechwyceń
        // WYNIK: printer("x") == printer("x") → false    ← z przechwyconą wartością — zawsze nowy obiekt
        // Specyfikacja Javy NIE gwarantuje żadnego z tych wyników. Wniosek: nigdy nie opieraj logiki na == lambd.

        // Praktyczna konsekwencja — usuwanie „słuchacza” (listener) z listy:
        List<Runnable> listeners = new ArrayList<>();
        listeners.add(() -> System.out.println("odświeżam ekran"));
        boolean removed = listeners.remove((Runnable) () -> System.out.println("odświeżam ekran"));   // NOWY obiekt
        show("usunięto nową, identyczną lambdę?", removed + ", słuchaczy: " + listeners.size());
        // WYNIK: usunięto nową, identyczną lambdę? → false, słuchaczy: 1

        Runnable refresh = () -> System.out.println("odświeżam ekran");  // refresh = odśwież
        listeners.add(refresh);
        show("usunięto przez tę samą referencję?", listeners.remove(refresh) + ", słuchaczy: " + listeners.size());
        // WYNIK: usunięto przez tę samą referencję? → true, słuchaczy: 1
        // DOBRA PRAKTYKA: lambdę, którą trzeba będzie później usunąć albo rozpoznać, zapisz w zmiennej (albo w mapie pod kluczem).
    }

    // =================================================================================================
    // 8. REKURENCJA W LAMBDZIE
    // =================================================================================================

    /** factorial = silnia — zwykła metoda rekurencyjna. */
    static int factorial(int n) {
        return n <= 1 ? 1 : n * factorial(n - 1);
    }

    /**
     * 8. Lambda nie ma nazwy, więc nie może wprost wywołać samej siebie. Zmienna lokalna z lambdą też odpada —
     * w chwili tworzenia lambdy zmienna nie ma jeszcze wartości.
     */
    static void recursionInLambda() {
        section("8. Rekurencja w lambdzie — pole albo (lepiej) nazwana metoda");

        //   IntUnaryOperator fact = n -> n <= 1 ? 1 : n * fact.applyAsInt(n - 1);
        //   → „variable fact might not have been initialized” (= „zmienna fact może nie być zainicjalizowana”)
        //
        //   static final IntUnaryOperator F = n -> n <= 1 ? 1 : n * F.applyAsInt(n - 1);
        //   → „self-reference in initializer” (= „odwołanie do samego siebie w inicjalizatorze”)

        // Obejście 1: pole z nazwą KWALIFIKOWANĄ (Klasa.POLE) — patrz FACTORIAL na górze klasy.
        // Działa, bo pole jest czytane dopiero przy WYWOŁANIU lambdy, gdy jest już zainicjalizowane (Lambda06ClosuresScope, sekcja 9).
        show("FACTORIAL.applyAsInt(5)", FACTORIAL.applyAsInt(5));
        // WYNIK: FACTORIAL.applyAsInt(5) → 120

        // Obejście 2 (zalecane): zwykła metoda rekurencyjna + referencja do metody:
        IntUnaryOperator viaMethod = Lambda08Pitfalls::factorial;
        show("Lambda08Pitfalls::factorial dla 6", viaMethod.applyAsInt(6));
        // WYNIK: Lambda08Pitfalls::factorial dla 6 → 720
        // DOBRA PRAKTYKA: rekurencja = nazwana metoda. Jest czytelniejsza, ma nazwę w śladzie stosu i nie wymaga sztuczek.
        // PUŁAPKA: int ma ograniczony zakres — silnia 13! już się nie mieści (przepełnienie, t01_basics/Basics04Operators).
    }

    // =================================================================================================
    // 9. NULL ZWRACANY Z Function / Supplier
    // =================================================================================================

    /**
     * 9. Lambda może zwrócić null — kompilator nie protestuje. Wyjątek NullPointerException pojawi się później,
     * w zupełnie innym miejscu kodu, gdy ktoś użyje wyniku. Im dalej od przyczyny, tym trudniej szukać.
     */
    static void nullFromFunctions() {
        section("9. null zwracany z Function i Supplier — wybuch daleko od przyczyny");

        Map<String, String> emails = Map.of("anna", "anna@example.com");
        Function<String, String> findEmail = name -> emails.get(name);       // get zwraca null, gdy klucza brak

        String found = findEmail.apply("piotr");                             // żadnego błędu... na razie
        show("findEmail.apply(\"piotr\")", found);
        // WYNIK: findEmail.apply("piotr") → null

        expectThrows("findEmail.apply(\"piotr\").toUpperCase()", () -> findEmail.apply("piotr").toUpperCase());
        // WYNIK: ✔ findEmail.apply("piotr").toUpperCase() → rzucono NullPointerException: Cannot invoke "String.toUpperCase()" because the return value of "java.util.function.Function.apply(Object)" is null

        Supplier<List<String>> listFactory = () -> null;                     // „fabryka”, która nic nie tworzy
        expectThrows("listFactory.get().add(\"x\")", () -> listFactory.get().add("x"));
        // WYNIK: ✔ listFactory.get().add("x") → rzucono NullPointerException: Cannot invoke "java.util.List.add(Object)" because the return value of "java.util.function.Supplier.get()" is null

        // Rozwiązania:
        //   • wartość domyślna zamiast null — getOrDefault:
        Function<String, String> withDefault = name -> emails.getOrDefault(name, "brak@example.com");
        //   • jawny „brak wartości” — Optional (cały rozdział t14_optional):
        Function<String, Optional<String>> maybeEmail = name -> Optional.ofNullable(emails.get(name));
        show("getOrDefault / Optional", withDefault.apply("piotr") + " / " + maybeEmail.apply("piotr"));
        // WYNIK: getOrDefault / Optional → brak@example.com / Optional.empty
        // DOBRA PRAKTYKA: funkcja, która może „nie znaleźć”, niech zwraca Optional — wtedy typ wyniku ostrzega
        //   każdego, kto jej używa. Supplier i fabryki nie powinny zwracać null nigdy.
    }

    // =================================================================================================
    // 10. LAMBDY I var
    // =================================================================================================

    /**
     * 10. var (Java 10+) każe kompilatorowi wydedukować typ zmiennej z prawej strony. Ale lambda NIE MA własnego
     * typu — dostaje go dopiero od miejsca docelowego. var i lambda czekają na siebie nawzajem, więc to się nie uda.
     */
    static void lambdasAndVar() {
        section("10. Lambdy i var");

        //   var twice = x -> x * 2;
        //   → „cannot infer type for local variable twice
        //      (lambda expression needs an explicit target-type)”
        //     (= „nie można wydedukować typu zmiennej — lambda potrzebuje jawnego typu docelowego”)
        // Nawet z jawnym typem parametru  var twice = (int x) -> x * 2;  — ten sam błąd: to samo „(int) → int” pasuje
        // do wielu interfejsów (IntUnaryOperator, własnych...), a kompilator nie zgaduje.

        IntUnaryOperator twice = x -> x * 2;                                  // typ zapisany jawnie — OK
        show("twice.applyAsInt(21)", twice.applyAsInt(21));
        // WYNIK: twice.applyAsInt(21) → 42

        // var przy PARAMETRACH lambdy — dozwolone (Java 11+); przydaje się, gdy chcesz dopisać adnotację, np. (@NonNull var s):
        BinaryOperator<Integer> add = (var a, var b) -> a + b;
        show("(var a, var b) -> a + b", add.apply(40, 2));
        // WYNIK: (var a, var b) -> a + b → 42

        // var przy kolekcji lambd — OK, bo typ wynika z new ArrayList<Runnable>():
        var tasks = new ArrayList<Runnable>();
        tasks.add(() -> System.out.println("   zadanie z listy var"));
        tasks.forEach(Runnable::run);
        // WYNIK: zadanie z listy var
        // DOBRA PRAKTYKA: przy lambdach zawsze pisz typ zmiennej (Predicate<Product>, Function<String, Integer>...) —
        //   to jedyne miejsce, które mówi czytelnikowi, CZYM ta lambda jest.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Function/Consumer/Predicate nie mają „throws” → wyjątek sprawdzany w lambdzie: try/catch W lambdzie albo
     *     pomocnik unchecked(ThrowingFunction), który opakuje go (IOException → UncheckedIOException) z zachowaniem cause.
     *     „throws” w metodzie otaczającej NIE pomaga. Czasem najprościej: zwykła pętla z throws w metodzie.
     *   • Lambda > 3 linii → metoda prywatna z nazwą + referencja (czytelność, testy, ślad stosu).
     *   • Lambdy bez stanu i bez efektów ubocznych; predykat ze stanem daje różne wyniki dla tych samych danych.
     *   • Ślad stosu: lambda$metoda$N = N-ta lambda w klasie, napisana w „metoda”; szukaj pierwszej linii z Twojego pakietu.
     *   • submit(Runnable) vs submit(Callable): () -> wywołanie() wybiera Callable; blok bez return — Runnable;
     *     dwa interfejsy zwracające wartość → „ambiguous”. Ratunek: rzutowanie; w swoim API — różne nazwy metod.
     *   • Lambd nie porównuj (== / equals). Do usunięcia słuchacza trzymaj referencję w zmiennej.
     *   • Rekurencja: zmienna lokalna → „might not have been initialized”; pole → tylko z nazwą Klasa.POLE;
     *     najlepiej nazwana metoda + referencja.
     *   • null z Function/Supplier wybucha później → getOrDefault, Optional (t14_optional), nigdy null z fabryki.
     *   • var f = x -> ... się nie kompiluje (lambda potrzebuje typu docelowego); (var a, var b) -> ... — tak (Java 11+).
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego  list.forEach(name -> Files.readString(Path.of(name)))  się nie kompiluje, nawet gdy metoda
     *      otaczająca ma „throws IOException”?
     *   2. Po co w unchecked(...) przekazujemy oryginalny wyjątek do konstruktora UncheckedIOException?
     *   3. Co wypisze ten kod?
     *          AtomicInteger n = new AtomicInteger();
     *          Predicate<String> p = s -> n.incrementAndGet() % 2 == 0;
     *          System.out.println(p.test("a") + " " + p.test("a") + " " + p.test("a"));
     *   4. Którą wersję wybierze kompilator:  submit(() -> list.add("x"))  — dla Runnable czy dla Callable? Dlaczego?
     *   5. ZNAJDŹ BŁĄD:
     *          listeners.add(() -> refresh());
     *          ...
     *          listeners.remove(() -> refresh());   // „wyrejestruj słuchacza”
     *   6. ZNAJDŹ BŁĄD:
     *          IntUnaryOperator fib = n -> n < 2 ? n : fib.applyAsInt(n - 1) + fib.applyAsInt(n - 2);
     *   7. Co oznacza w śladzie stosu ramka  lambda$loadAll$2 ?
     *   8. Dlaczego  var f = x -> x + 1;  się nie kompiluje, a  var list = new ArrayList<Runnable>();  — tak?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /**
     * {@code ThrowingConsumer<T>} = konsument, który może rzucić wyjątek (do ćwiczenia 4).
     */
    @FunctionalInterface
    interface ThrowingConsumer<T> {
        void accept(T value) throws Exception;
    }

    /** saveStrict = zapisz (ściśle). Rzuca IOException dla nazw ze znakiem „/” — do ćwiczenia 4. */
    static void saveStrict(String name) throws IOException {
        if (name.contains("/")) {
            throw new IOException("Niedozwolony znak w nazwie: " + name);
        }
    }

    /** describeFailure = opisz porażkę. Uruchamia akcję i opisuje wyjątek: „Typ ← TypPrzyczyny: komunikat”. */
    static String describeFailure(Runnable action) {
        try {
            action.run();
            return "brak wyjątku";
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            return e.getClass().getSimpleName() + " ← "
                    + (cause == null ? "brak przyczyny" : cause.getClass().getSimpleName() + ": " + cause.getMessage());
        }
    }

    static void exercises() {
        List<String> expected1 = List.of("port=8080", "(brak)", "url=jdbc:h2:mem");
        List<String> expected2 = List.of("Anna Nowak (IT) — SENIOR", "Piotr Kowalski (IT) — MID",
                "Katarzyna Wiśniewska (HR) — JUNIOR", "Tomasz Wójcik (SPRZEDAZ) — JUNIOR");
        List<Employee> firstFour = SampleData.employees().subList(0, 4);

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: try/catch w lambdzie", expected1, () -> exercise1(List.of("app.cfg", "brak.cfg", "db.cfg")));
        Check.equal("ćw. 2: długa lambda → metoda + referencja", expected2, () -> exercise2(firstFour));
        Check.equal("ćw. 3: rekurencja — suma cyfr", List.of(35, 7), () -> {
            IntUnaryOperator digitSum = exercise3();
            return List.of(digitSum.applyAsInt(98765), digitSum.applyAsInt(7));
        });
        Check.equal("ćw. 4a: unchecked Consumer — bez błędu", List.of("a.txt", "b.txt"), () -> {
            List<String> saved = new ArrayList<>();
            List.of("a.txt", "b.txt").forEach(exercise4(name -> saved.add(name)));
            return saved;
        });
        Check.equal("ćw. 4b: IOException → UncheckedIOException z przyczyną",
                "UncheckedIOException ← IOException: Niedozwolony znak w nazwie: zły/plik",
                () -> describeFailure(() -> exercise4(Lambda08Pitfalls::saveStrict).accept("zły/plik")));
        Check.equal("ćw. 4c: inny wyjątek sprawdzany → RuntimeException z przyczyną",
                "RuntimeException ← Exception: ogólny błąd",
                () -> describeFailure(() -> exercise4(name -> {
                    throw new Exception("ogólny błąd");
                }).accept("x")));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", expected1, () -> solution1(List.of("app.cfg", "brak.cfg", "db.cfg")));
        Check.equal("ćw. 2 (wzorzec)", expected2, () -> solution2(firstFour));
        Check.equal("ćw. 3 (wzorzec)", List.of(35, 7), () -> {
            IntUnaryOperator digitSum = solution3();
            return List.of(digitSum.applyAsInt(98765), digitSum.applyAsInt(7));
        });
        Check.equal("ćw. 4a (wzorzec)", List.of("a.txt", "b.txt"), () -> {
            List<String> saved = new ArrayList<>();
            List.of("a.txt", "b.txt").forEach(solution4(name -> saved.add(name)));
            return saved;
        });
        Check.equal("ćw. 4b (wzorzec)", "UncheckedIOException ← IOException: Niedozwolony znak w nazwie: zły/plik",
                () -> describeFailure(() -> solution4(Lambda08Pitfalls::saveStrict).accept("zły/plik")));
        Check.equal("ćw. 4c (wzorzec)", "RuntimeException ← Exception: ogólny błąd",
                () -> describeFailure(() -> solution4(name -> {
                    throw new Exception("ogólny błąd");
                }).accept("x")));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): dla każdej nazwy wczytaj konfigurację metodą loadConfig; gdy rzuci IOException,
     * wstaw w to miejsce napis "(brak)". Zwróć listę wyników w kolejności nazw.
     * Podpowiedź: mapAll z lambdą o ciele blokowym: try { return loadConfig(n); } catch (IOException e) { return "(brak)"; }
     */
    static List<String> exercise1(List<String> names) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ DŁUGĄ LAMBDĘ NA METODĘ I REFERENCJĘ. Ten kod działa, ale lambda jest za długa:
     * <pre>{@code
     *   List<String> result = new ArrayList<>();
     *   employees.forEach(e -> {
     *       String level;
     *       if (e.salary() >= 12_000) {
     *           level = "SENIOR";
     *       } else if (e.salary() >= 9_000) {
     *           level = "MID";
     *       } else {
     *           level = "JUNIOR";
     *       }
     *       result.add(e.name() + " (" + e.department() + ") — " + level);
     *   });
     *   return result;
     * }</pre>
     * Przenieś ciało do nowej metody prywatnej (np. {@code private static String describeEmployee(Employee e)})
     * i zwróć {@code mapAll(employees, Lambda08Pitfalls::describeEmployee)}.
     * Podpowiedź: metoda ma ZWRACAĆ napis (return), a nie dodawać go do listy — listę zbuduje mapAll.
     */
    static List<String> exercise2(List<Employee> employees) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 3 (średnie): ten kod miał zwracać funkcję „suma cyfr” (98765 → 9+8+7+6+5 = 35), ale się nie kompiluje:
     * <pre>{@code
     *   IntUnaryOperator digitSum = n -> n < 10 ? n : n % 10 + digitSum.applyAsInt(n / 10);
     *   return digitSum;
     * }</pre>
     * Napraw go: napisz zwykłą metodę rekurencyjną (np. {@code static int sumOfDigits(int n)}) i zwróć referencję do niej.
     * Podpowiedź: sekcja 8. n % 10 = ostatnia cyfra, n / 10 = liczba bez ostatniej cyfry.
     */
    static IntUnaryOperator exercise3() {
        // TODO: twoje rozwiązanie
        return n -> 0;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): napisz odpowiednik unchecked(...) z sekcji 2, ale dla Consumer: zamień
     * ThrowingConsumer na zwykły Consumer. Wymagania:
     * IOException → {@code UncheckedIOException(e.getMessage(), e)}; RuntimeException → przepuść bez zmian;
     * każdy inny wyjątek → {@code RuntimeException(e.getMessage(), e)}. Przyczyna (cause) musi zostać zachowana.
     * Podpowiedź: zwróć lambdę {@code value -> { try { action.accept(value); } catch (...) { ... } }} —
     * kolejność catch: od najbardziej szczegółowego (IOException, RuntimeException) do najogólniejszego (Exception).
     */
    static <T> Consumer<T> exercise4(ThrowingConsumer<T> action) {
        // TODO: twoje rozwiązanie
        return value -> { };
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solution1(List<String> names) {
        return mapAll(names, name -> {
            try {
                return loadConfig(name);
            } catch (IOException e) {
                return "(brak)";
            }
        });
    }

    /** describeEmployee = opisz pracownika. Wyciągnięte ciało długiej lambdy z ćwiczenia 2. */
    private static String describeEmployee(Employee e) {
        String level;
        if (e.salary() >= 12_000) {
            level = "SENIOR";
        } else if (e.salary() >= 9_000) {
            level = "MID";
        } else {
            level = "JUNIOR";
        }
        return e.name() + " (" + e.department() + ") — " + level;
    }

    static List<String> solution2(List<Employee> employees) {
        return mapAll(employees, Lambda08Pitfalls::describeEmployee);
    }

    /** sumOfDigits = suma cyfr — zwykła metoda rekurencyjna (ćwiczenie 3). */
    static int sumOfDigits(int n) {
        return n < 10 ? n : n % 10 + sumOfDigits(n / 10);
    }

    static IntUnaryOperator solution3() {
        return Lambda08Pitfalls::sumOfDigits;
    }

    static <T> Consumer<T> solution4(ThrowingConsumer<T> action) {
        return value -> {
            try {
                action.accept(value);
            } catch (IOException e) {
                throw new UncheckedIOException(e.getMessage(), e);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        };
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Ciało lambdy jest ciałem metody accept interfejsu Consumer, a accept nie deklaruje „throws IOException”.
     *      „throws” metody otaczającej dotyczy jej własnego ciała, nie ciała lambdy. Trzeba złapać wyjątek w lambdzie,
     *      opakować go (unchecked) albo użyć zwykłej pętli.
     *   2. Żeby nie zgubić informacji: oryginał (cause) ma swój typ, komunikat i ślad stosu. W logu zobaczysz
     *      „Caused by: java.io.IOException: ...”. Bez niego zostaje tylko ogólny komunikat opakowania.
     *   3. „false true false” — ten sam argument, a różne wyniki: predykat ma ukryty stan (licznik).
     *   4. Callable — list.add("x") to wywołanie metody, które pasuje i jako instrukcja (Runnable), i jako wartość
     *      (Callable<Boolean>); gdy pasują oba, kompilator wybiera wersję zwracającą wartość.
     *   5. Druga lambda to NOWY obiekt — nie jest równy pierwszemu (lambdy nie nadpisują equals), więc remove nic nie
     *      usunie. Trzeba zapisać lambdę w zmiennej (Runnable r = () -> refresh();) i dodawać/usuwać tę samą referencję.
     *   6. Zmienna lokalna fib nie ma jeszcze wartości w chwili tworzenia lambdy — „variable fib might not have been
     *      initialized”. Rozwiązanie: zwykła metoda rekurencyjna + referencja (albo pole z nazwą kwalifikowaną).
     *   7. Ciało lambdy napisanej w metodzie loadAll; 2 to jej numer kolejny w klasie. Numer linii obok wskazuje miejsce.
     *   8. Lambda nie ma własnego typu — potrzebuje typu docelowego, a var potrzebuje typu z prawej strony; nikt go
     *      nie dostarcza. new ArrayList<Runnable>() ma konkretny typ, więc var może go przejąć.
     */
    // </editor-fold>
}
