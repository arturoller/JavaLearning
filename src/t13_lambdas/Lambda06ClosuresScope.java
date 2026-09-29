package t13_lambdas;

import helpers.Check;
import helpers.SampleData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Domknięcia i zasięg — jakie zmienne „widzi” lambda i dlaczego nie może ich zmieniać
 *        (closure = domknięcie; scope = zasięg; capture = przechwycić)
 *
 * W SKRÓCIE:
 *   Lambda może używać zmiennych lokalnych z metody, w której powstała — mówimy, że je PRZECHWYTUJE (captures).
 *   Warunek: zmienna musi być „effectively final” (w praktyce ostateczna) — przypisana raz i nigdy niezmieniana.
 *   Dzieje się tak, bo lambda dostaje KOPIĘ WARTOŚCI zmiennej, a nie samą zmienną.
 *   Pola klasy (także static) to inna historia — lambda może je zmieniać, bo sięga do nich przez obiekt (this) albo klasę.
 *   W lambdzie „this” oznacza obiekt OTACZAJĄCY, a nie samą lambdę (inaczej niż w klasie anonimowej).
 *
 * ANALOGIA: zdjęcie tablicy w klasie.
 *   Lambda, która przechwytuje zmienną lokalną, robi jej ZDJĘCIE telefonem w chwili powstania. Potem nosi zdjęcie
 *   ze sobą, także gdy lekcja (metoda) się skończy i tablicę wytrą. Java zabrania zmieniać to, co jest na tablicy,
 *   żeby zdjęcie i tablica nigdy się nie rozjechały.
 *   Jeśli na tablicy jest ADRES szafki (referencja do obiektu), zdjęcie ma ten sam adres — a zawartość szafki
 *   (stan obiektu) może się zmieniać i lambda to zobaczy.
 *
 * JAK TO DZIAŁA:
 *   int limit = 10;
 *   {@code Predicate<Integer> big = n -> n > limit;}   ← lambda przechwytuje WARTOŚĆ limit (10)
 *   limit = 20;                                ← BŁĄD KOMPILACJI: limit przestaje być effectively final
 *
 *   Zmienna lokalna żyje na stosie wywołania metody i znika, gdy metoda się kończy. Lambda może zostać wywołana
 *   później (albo w innym wątku) — więc kompilator kopiuje wartość do obiektu lambdy. Zakaz zmian gwarantuje,
 *   że kopia jest zawsze równa oryginałowi.
 *
 * SŁÓWKA:
 *   closure = domknięcie (lambda + przechwycone zmienne); capture = przechwycić; scope = zasięg;
 *   effectively final = w praktyce ostateczna (nigdy nie zmieniana); shadow = przesłonić (nazwę);
 *   field = pole; enclosing = otaczający; snapshot = migawka, zdjęcie stanu; counter = licznik;
 *   workaround = obejście; code smell = „zapach kodu” (sygnał, że coś jest źle zaprojektowane); greeter = witacz.
 *
 * ZOBACZ TEŻ: Lambda04MethodReferences (referencja bound ustala obiekt raz), Lambda07HigherOrderFunctions
 *             (funkcje zwracające funkcje), t16_streams/Streams17SideEffectsPitfalls (efekty uboczne w streamach),
 *             t21_concurrency/Concurrency02RaceConditions (dlaczego współdzielony stan jest groźny).
 * </pre>
 */
public class Lambda06ClosuresScope {

    /** staticClicks = kliknięcia (pole STATYCZNE) — do pokazania, że lambda może zmieniać pola. */
    private static int staticClicks = 0;

    /** currentUser = bieżący użytkownik (pole statyczne) — do sekcji 9 o momencie przechwycenia. */
    private static String currentUser = "anna";

    public static void main(String[] args) {
        title("Lambda06 — domknięcia i zasięg zmiennych");

        capturingLocals();          // capturing locals = przechwytywanie zmiennych lokalnych
        effectivelyFinal();         // effectively final = w praktyce ostateczna
        whyValueCopy();             // why value copy = dlaczego kopia wartości
        referenceVsState();         // reference vs state = referencja kontra stan obiektu
        fieldsAndStatics();         // fields and statics = pola i pola statyczne
        workaroundsAreSmells();     // workarounds are smells = obejścia to „zapach kodu”
        thisInLambda();             // this in lambda = this w lambdzie
        noShadowing();              // no shadowing = zakaz przesłaniania nazw
        captureTiming();            // capture timing = moment przechwycenia
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PRZECHWYTYWANIE ZMIENNYCH LOKALNYCH
    // =================================================================================================

    /** greeter = witacz. Zwraca lambdę, która „pamięta” parametr greeting — także PO zakończeniu tej metody. */
    static Function<String, String> greeter(String greeting) {
        return name -> greeting + ", " + name + "!";
    }

    /**
     * 1. Lambda widzi zmienne lokalne i parametry metody, w której powstała. Lambda razem z przechwyconymi
     * wartościami to DOMKNIĘCIE (closure).
     */
    static void capturingLocals() {
        section("1. Lambda przechwytuje zmienne lokalne");

        int limit = 6;                                              // zmienna lokalna
        Predicate<String> isLong = w -> w.length() > limit;         // lambda używa limit z otoczenia
        List<String> words = new ArrayList<>(SampleData.words());
        words.removeIf(isLong.negate());                            // zostaw tylko długie
        show("słowa dłuższe niż " + limit, words);
        // WYNIK: słowa dłuższe niż 6 → [kolekcja, optional]

        // Lambda przeżywa metodę, w której powstała — i nadal pamięta jej parametr:
        Function<String, String> hello = greeter("Dzień dobry");    // greeter już się zakończył...
        Function<String, String> hi = greeter("Cześć");
        show("hello.apply(\"Anno\")", hello.apply("Anno"));         // ...a lambda wciąż zna "Dzień dobry"
        show("hi.apply(\"Piotrze\")", hi.apply("Piotrze"));
        // WYNIK: hello.apply("Anno") → Dzień dobry, Anno!
        // WYNIK: hi.apply("Piotrze") → Cześć, Piotrze!
        // Dwie lambdy z tego samego kodu, ale z RÓŻNYMI przechwyconymi wartościami — każda ma swoje „zdjęcie”.
    }

    // =================================================================================================
    // 2. EFFECTIVELY FINAL
    // =================================================================================================

    /**
     * 2. Zmienna jest „effectively final”, jeśli po pierwszym przypisaniu NIGDY się nie zmienia — ani przed lambdą,
     * ani po niej, ani w niej. Słowo final nie jest potrzebne (ale wolno je dopisać).
     */
    static void effectivelyFinal() {
        section("2. Effectively final — przypisana raz, nigdy niezmieniana");

        String prefix = "SKU-";                                     // nigdy niezmieniana → effectively final
        final String suffix = "-PL";                                // jawnie final — też OK
        Function<String, String> code = s -> prefix + s + suffix;
        show("code.apply(\"001\")", code.apply("001"));
        // WYNIK: code.apply("001") → SKU-001-PL

        // Co NIE przejdzie (każdy z tych fragmentów to błąd kompilacji):
        //
        //   int limit = 10;
        //   Predicate<Integer> big = n -> n > limit;
        //   limit = 20;                                  ← zmiana PO lambdzie też się liczy!
        //
        //   int count = 0;
        //   words.forEach(w -> count++);                 ← zmiana W lambdzie
        //
        //   → „local variables referenced from a lambda expression must be final or effectively final”
        //     (= „zmienne lokalne użyte w lambdzie muszą być final albo effectively final”)
        //
        // To samo dotyczy klas anonimowych (od Javy 8):
        //   int c = 0;
        //   Runnable r = new Runnable() { public void run() { c++; } };
        //   → „local variables referenced from an inner class must be final or effectively final”
        //
        // Parametry metody też są zmiennymi lokalnymi: jeśli metoda gdzieś zmienia parametr, lambda go nie użyje.
        note("Zasada: lambda może CZYTAĆ zmienne lokalne, ale nikt nie może ich ZMIENIAĆ — ani w lambdzie, ani poza nią.");
        // WYNIK: ℹ Zasada: lambda może CZYTAĆ zmienne lokalne, ale nikt nie może ich ZMIENIAĆ — ani w lambdzie, ani poza nią.
    }

    // =================================================================================================
    // 3. DLACZEGO? LAMBDA DOSTAJE KOPIĘ WARTOŚCI
    // =================================================================================================

    /**
     * 3. Zmienne lokalne żyją na stosie wywołania metody i znikają po jej zakończeniu. Lambda może zostać uruchomiona
     * później (albo w innym wątku) — dlatego Java KOPIUJE wartość do wnętrza obiektu lambdy.
     * Gdyby wolno było zmieniać oryginał, kopia i oryginał by się rozjechały — i nie byłoby wiadomo, którą wartość
     * „widzi” lambda. Java ucina problem u źródła: zmieniać nie wolno.
     */
    static void whyValueCopy() {
        section("3. Dlaczego? Lambda dostaje KOPIĘ wartości");

        // Pętla, która tworzy zadania „na później”:
        List<Runnable> tasks = new ArrayList<>();                   // tasks = zadania
        for (int i = 1; i <= 3; i++) {
            int number = i;                                         // NOWA zmienna w każdym obrocie pętli, przypisana raz
            tasks.add(() -> System.out.println("   zadanie nr " + number));
        }
        note("Zadania utworzone, pętla skończona. Uruchamiamy:");
        // WYNIK: ℹ Zadania utworzone, pętla skończona. Uruchamiamy:
        for (Runnable task : tasks) {
            task.run();
        }
        // WYNIK: zadanie nr 1
        // WYNIK: zadanie nr 2
        // WYNIK: zadanie nr 3
        // Każda lambda ma własną kopię „number”. Gdyby użyła samego i:
        //   tasks.add(() -> System.out.println(i));   → błąd: i jest zmieniane przez i++ (nie jest effectively final)
        // Po zakończeniu pętli i ma wartość 4 — gdyby Java na to pozwalała, wszystkie zadania wypisałyby 4
        // (taki błąd naprawdę zdarza się w językach, które na to pozwalają, np. w starym JavaScripcie).

        // Pętla for-each jest OK bez kopii: jej zmienna jest NOWA w każdym obrocie i nikt jej nie zmienia.
        List<Supplier<String>> labels = new ArrayList<>();
        for (String status : List.of("NOWE", "OPŁACONE", "WYSŁANE")) {
            labels.add(() -> "status: " + status);
        }
        show("etykiety", labels.get(0).get() + " | " + labels.get(2).get());
        // WYNIK: etykiety → status: NOWE | status: WYSŁANE
    }

    // =================================================================================================
    // 4. REFERENCJA KONTRA STAN OBIEKTU
    // =================================================================================================

    /**
     * 4. Jeśli zmienna trzyma REFERENCJĘ do obiektu, lambda dostaje kopię referencji — czyli „adres” tego samego
     * obiektu. Zmiennej przypisać ponownie nie wolno, ale STAN obiektu (zawartość listy, StringBuilder) zmieniać można.
     */
    static void referenceVsState() {
        section("4. Referencja jest stała, ale stan obiektu może się zmieniać");

        List<String> cart = new ArrayList<>();                      // cart = koszyk
        Consumer<String> addToCart = item -> cart.add(item);        // cart jest effectively final — nie przypisujemy go ponownie
        addToCart.accept("kawa");
        addToCart.accept("mleko");
        show("koszyk po dwóch accept", cart);
        // WYNIK: koszyk po dwóch accept → [kawa, mleko]
        //   cart = new ArrayList<>();   ← to by już był błąd kompilacji (ponowne przypisanie przechwyconej zmiennej)

        // Lambda widzi AKTUALNY stan obiektu z chwili wywołania, nie z chwili utworzenia:
        StringBuilder log = new StringBuilder("start");
        Supplier<String> readLog = () -> log.toString();
        log.append(" → krok 1");
        show("readLog.get()", readLog.get());
        // WYNIK: readLog.get() → start → krok 1

        // Dlaczego to ryzykowne? Lambda zmieniająca obiekt z zewnątrz ma UKRYTY efekt uboczny.
        //   • Wywołana dwa razy (np. ktoś ponowi operację) — zdubluje dane:
        Runnable addGift = () -> cart.add("gratis");                // gift = prezent
        addGift.run();
        addGift.run();
        show("koszyk po dwóch run()", cart);
        // WYNIK: koszyk po dwóch run() → [kawa, mleko, gratis, gratis]
        //   • Uruchomiona w kilku wątkach naraz — ArrayList nie jest bezpieczny wątkowo: wpisy mogą ginąć albo
        //     może polecieć wyjątek (t21_concurrency/Concurrency02RaceConditions, strumienie równoległe: t16_streams/Streams18Parallel).
        //   • Czytając kod metody, nie widać, że coś zmienia koszyk — zmiana jest schowana w lambdzie.
        // DOBRA PRAKTYKA: lambdy przekazywane „gdzieś dalej” niech ZWRACAJĄ wynik zamiast zmieniać obiekty z zewnątrz.
        //   Efekt uboczny jest OK tam, gdzie jest celem (Consumer w forEach, logowanie) i działa w jednym wątku.
    }

    // =================================================================================================
    // 5. POLA I POLA STATYCZNE — LAMBDA MOŻE JE ZMIENIAĆ
    // =================================================================================================

    /** ClickCounter = licznik kliknięć. Lambda w metodzie instancji zmienia POLE obiektu. */
    static class ClickCounter {
        private int clicks;                                         // clicks = kliknięcia (pole obiektu)

        /** clickHandler = obsługa kliknięcia. Lambda zmienia pole — wolno, bo pole nie jest zmienną lokalną. */
        Runnable clickHandler() {
            return () -> clicks++;                                  // tak naprawdę: this.clicks++
        }

        int clicks() {
            return clicks;
        }
    }

    /**
     * 5. Reguła effectively final dotyczy TYLKO zmiennych lokalnych i parametrów. Pola obiektu i pola static
     * lambda może zmieniać: do pola obiektu sięga przez przechwycone {@code this}, do pola static — przez klasę.
     * Nie ma tu kopii wartości, więc nie ma problemu „rozjechania się” kopii.
     */
    static void fieldsAndStatics() {
        section("5. Pola i pola static — lambda MOŻE je zmieniać (i to bywa groźne)");

        ClickCounter button = new ClickCounter();                   // button = przycisk
        Runnable onClick = button.clickHandler();                   // on click = przy kliknięciu
        onClick.run();
        onClick.run();
        onClick.run();
        show("kliknięcia (pole obiektu)", button.clicks());
        // WYNIK: kliknięcia (pole obiektu) → 3

        Runnable countStatic = () -> staticClicks++;                // pole static — też wolno
        countStatic.run();
        countStatic.run();
        show("kliknięcia (pole static)", staticClicks);
        // WYNIK: kliknięcia (pole static) → 2

        // PUŁAPKA: to, że się kompiluje, nie znaczy, że jest bezpieczne. Pole static to stan WSPÓLNY dla całego
        //   programu: każda lambda i każdy wątek może je zmienić, a clicks++ nie jest operacją niepodzielną
        //   (odczyt, dodanie, zapis). Przy wielu wątkach część kliknięć zginie (t21_concurrency/Concurrency02RaceConditions).
        // DOBRA PRAKTYKA: stan trzymaj w obiekcie, który go „posiada” (jak ClickCounter), a nie w polach static.
    }

    // =================================================================================================
    // 6. OBEJŚCIA: AtomicInteger, TABLICA JEDNOELEMENTOWA — „ZAPACH KODU”
    // =================================================================================================

    /**
     * 6. Popularne sztuczki, żeby „oszukać” regułę effectively final: zmienna trzyma obiekt, a my zmieniamy
     * jego zawartość. Kompilują się — ale zwykle sygnalizują, że lambda jest tu złym narzędziem.
     */
    static void workaroundsAreSmells() {
        section("6. Obejścia (AtomicInteger, int[]) — kompilują się, ale „pachną”");

        List<String> words = SampleData.words();

        // Obejście 1: AtomicInteger (obiekt-licznik; incrementAndGet = zwiększ o 1 i zwróć)
        AtomicInteger longWords = new AtomicInteger();
        words.forEach(w -> {
            if (w.length() > 4) {
                longWords.incrementAndGet();
            }
        });
        // Obejście 2: tablica jednoelementowa — referencja do tablicy się nie zmienia, zmienia się jej element
        int[] totalLength = {0};
        words.forEach(w -> totalLength[0] += w.length());
        show("AtomicInteger / int[]", longWords.get() + " / " + totalLength[0]);
        // WYNIK: AtomicInteger / int[] → 7 / 65

        // To samo zwykłą pętlą — prościej, szybciej, bez sztuczek:
        int count = 0;
        int total = 0;
        for (String w : words) {
            if (w.length() > 4) {
                count++;
            }
            total += w.length();
        }
        show("zwykła pętla", count + " / " + total);
        // WYNIK: zwykła pętla → 7 / 65

        // DOBRA PRAKTYKA: gdy łapiesz się na AtomicInteger albo int[1] tylko po to, żeby liczyć coś w forEach —
        //   użyj pętli. W streamach: count(), sum(), reduce (t16_streams) — one liczą bez zmiennych z zewnątrz.
        // PUŁAPKA: int[] w lambdzie NIE jest bezpieczne wątkowo. AtomicInteger jest, ale nie ratuje czytelności.
        // Kiedy zmienny stan w domknięciu jest w porządku? Gdy jest PRYWATNY dla jednej lambdy i nikt inny go nie widzi
        //   — np. generator kolejnych numerów (ćwiczenie 5 i Lambda07HigherOrderFunctions, memoizacja).
    }

    // =================================================================================================
    // 7. this W LAMBDZIE A W KLASIE ANONIMOWEJ
    // =================================================================================================

    /** Screen = ekran. Klasa z metodami zwracającymi lambdę i klasę anonimową — porównamy, czym jest w nich this. */
    static class Screen {
        private final String name;

        Screen(String name) {
            this.name = name;
        }

        /** lambdaPrinter = drukarka z lambdą. this w lambdzie = obiekt Screen (otaczający). */
        Runnable lambdaPrinter() {
            return () -> System.out.println("   lambda:           this = " + this);
        }

        /** anonymousPrinter = drukarka z klasą anonimową. this = obiekt klasy anonimowej. */
        Runnable anonymousPrinter() {
            return new Runnable() {
                @Override
                public void run() {
                    System.out.println("   klasa anonimowa:  this = " + this);
                    System.out.println("   klasa anonimowa:  Screen.this = " + Screen.this);   // tak sięga się do obiektu zewnętrznego
                }

                @Override
                public String toString() {
                    return "obiekt klasy anonimowej";
                }
            };
        }

        @Override
        public String toString() {
            return "Screen[" + name + "]";
        }
    }

    /**
     * 7. Lambda NIE tworzy nowego „ja” — this, super i nazwy w jej ciele znaczą to samo, co w otaczającej metodzie.
     * Klasa anonimowa to osobny obiekt: jej this to ona sama, a do obiektu zewnętrznego sięga się przez Klasa.this.
     */
    static void thisInLambda() {
        section("7. this w lambdzie kontra this w klasie anonimowej");

        Screen screen = new Screen("główny");
        screen.lambdaPrinter().run();
        screen.anonymousPrinter().run();
        // WYNIK: lambda:           this = Screen[główny]
        // WYNIK: klasa anonimowa:  this = obiekt klasy anonimowej
        // WYNIK: klasa anonimowa:  Screen.this = Screen[główny]

        // W metodzie STATYCZNEJ nie ma żadnego this — więc lambda też go nie ma:
        //   static void m() { Runnable r = () -> System.out.println(this); }
        //   → „non-static variable this cannot be referenced from a static context”
        //     (= „this nie może być użyte w kontekście statycznym”)
        // DOBRA PRAKTYKA: to zachowanie lambdy jest zwykle tym, czego chcesz — w lambdzie w metodzie obiektu
        //   możesz wygodnie używać pól i metod tego obiektu, bez Klasa.this.
    }

    // =================================================================================================
    // 8. ZAKAZ PRZESŁANIANIA NAZW
    // =================================================================================================

    /**
     * 8. Lambda nie tworzy nowego zasięgu nazw. Jej parametry i zmienne w jej ciele NIE MOGĄ mieć takich samych
     * nazw jak zmienne lokalne metody (w klasie anonimowej — mogą, bo to osobna klasa).
     */
    static void noShadowing() {
        section("8. Parametr lambdy nie może przesłonić zmiennej lokalnej");

        String text = "zmienna lokalna";
        // Function<String, Integer> length = text -> text.length();
        //   → „variable text is already defined in method noShadowing()”
        //     (= „zmienna text jest już zdefiniowana w metodzie noShadowing()”)
        // Runnable r = () -> { String text = "inna"; };   ← ten sam błąd: zmienna w ciele lambdy też się liczy
        Function<String, Integer> length = s -> s.length();         // inna nazwa — OK
        show("length.apply(text)", length.apply(text));
        // WYNIK: length.apply(text) → 15

        // Klasa anonimowa MOŻE przesłonić nazwę — to osobna klasa z własnym zasięgiem:
        Function<String, Integer> anonymous = new Function<>() {    // diamond <> przy klasie anonimowej — Java 9+
            @Override
            public Integer apply(String text) {                     // parametr „text” przesłania zmienną lokalną
                return text.length();
            }
        };
        show("klasa anonimowa z parametrem text", anonymous.apply("abc"));
        // WYNIK: klasa anonimowa z parametrem text → 3
        // DOBRA PRAKTYKA: zakaz w lambdzie to zaleta — nie ma „dwóch różnych text” w jednej metodzie, więc nie ma pomyłek.
    }

    // =================================================================================================
    // 9. MOMENT PRZECHWYCENIA: ZMIENNA LOKALNA A POLE
    // =================================================================================================

    /**
     * 9. Kiedy lambda „czyta” wartość?
     * <ul>
     *   <li>zmienna LOKALNA — wartość kopiowana w chwili UTWORZENIA lambdy (i tak się nie zmieni — jest effectively final),</li>
     *   <li>POLE — czytane w chwili WYWOŁANIA lambdy (lambda pamięta tylko, gdzie pole jest, a nie jego wartość).</li>
     * </ul>
     */
    static void captureTiming() {
        section("9. Moment przechwycenia: zmienna lokalna kontra pole");

        String userAtCreation = currentUser;                        // kopia wartości pola do zmiennej lokalnej
        Supplier<String> fromLocal = () -> "lokalna: " + userAtCreation;
        Supplier<String> fromField = () -> "pole: " + currentUser;

        currentUser = "piotr";                                      // zmiana POLA po utworzeniu lambd

        show("fromLocal.get()", fromLocal.get());
        show("fromField.get()", fromField.get());
        // WYNIK: fromLocal.get() → lokalna: anna    ← wartość z chwili utworzenia
        // WYNIK: fromField.get() → pole: piotr      ← wartość z chwili wywołania

        // Wniosek praktyczny: chcesz „zamrozić” wartość pola dla lambdy (np. przed przekazaniem jej do innego wątku)?
        //   Skopiuj ją do zmiennej lokalnej i użyj tej zmiennej. Chcesz zamrozić STAN listy? Skopiuj listę
        //   (List.copyOf — ćwiczenie 4), bo sama referencja nie zamraża zawartości.
        currentUser = "anna";                                       // sprzątamy po sobie (pole static żyje dalej)
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Lambda może CZYTAĆ zmienne lokalne i parametry otaczającej metody (domknięcie = lambda + przechwycone wartości).
     *   • Warunek: zmienna effectively final — przypisana raz, nigdy niezmieniana (ani przed, ani po, ani w lambdzie).
     *     Błąd: „local variables referenced from a lambda expression must be final or effectively final”.
     *   • Powód: lambda dostaje KOPIĘ wartości (może działać później / w innym wątku). Zakaz zmian = kopia zawsze aktualna.
     *   • Pętla for (int i...) — skopiuj i do nowej zmiennej w pętli; for-each — zmienna i tak jest nowa w każdym obrocie.
     *   • Referencja jest stała, STAN obiektu nie: list.add w lambdzie działa — ale to ukryty efekt uboczny
     *     (dublowanie przy ponownym wywołaniu, brak bezpieczeństwa wątkowego).
     *   • Pola obiektu i pola static lambda może zmieniać (przez this / klasę) — kompiluje się, ale to stan współdzielony.
     *   • AtomicInteger / int[1] w forEach = obejście i „zapach kodu”; zwykle lepsza pętla albo stream (count, sum).
     *   • this w lambdzie = obiekt OTACZAJĄCY; w klasie anonimowej = ona sama (zewnętrzny: Klasa.this).
     *   • Parametr i zmienne lambdy nie mogą mieć nazw zmiennych lokalnych metody („already defined”).
     *   • Zmienna lokalna → wartość z chwili utworzenia lambdy; pole → wartość z chwili wywołania.
     *
     * PYTANIA KONTROLNE:
     *   1. Co znaczy „effectively final”? Czy zmienna musi mieć słowo final, żeby użyć jej w lambdzie?
     *   2. ZNAJDŹ BŁĄD:
     *          int sum = 0;
     *          numbers.forEach(n -> sum += n);
     *   3. Dlaczego Java zabrania zmieniać zmienne lokalne przechwycone przez lambdę? Co by się stało, gdyby pozwalała?
     *   4. Co wypisze ten kod?
     *          List<String> list = new ArrayList<>();
     *          Runnable r = () -> list.add("x");
     *          r.run(); r.run();
     *          System.out.println(list);
     *   5. Co wypisze metoda run() lambdy, a co klasy anonimowej, jeśli obie zawierają println(this) i powstały
     *      w metodzie obiektu, którego toString() zwraca "Główny"?
     *   6. ZNAJDŹ BŁĄD:
     *          String name = "Ala";
     *          Function<String, String> f = name -> name.toUpperCase();
     *   7. Dlaczego  for (int i = 0; i < 3; i++) tasks.add(() -> System.out.println(i));  się nie kompiluje,
     *      a  for (String s : list) tasks.add(() -> System.out.println(s));  — tak?
     *   8. Lambda czyta pole static, które zmieniamy po jej utworzeniu. Którą wartość zobaczy: starą czy nową?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: predykat „w przedziale [3, 7]”", List.of(5, 3, 7, 3, 6, 4),
                () -> keepNumbers(SampleData.numbers(), exercise1(3, 7)));
        Check.equal("ćw. 2: napraw total += w lambdzie (suma długości)", 40, () -> exercise2(SampleData.words()));
        Check.equal("ćw. 3: lista Supplierów z numerami", List.of("1. Ala", "2. Ola", "3. Ela"),
                () -> callAll(exercise3(List.of("Ala", "Ola", "Ela"))));
        Check.equal("ćw. 4: migawka listy (zmiana po utworzeniu nie widoczna)", "[kawa, mleko]", () -> {
            List<String> cart = new ArrayList<>(List.of("kawa", "mleko"));
            Supplier<String> snapshot = exercise4(cart);
            cart.add("cukier");                                     // zmiana PO utworzeniu migawki
            return snapshot.get();
        });
        Check.equal("ćw. 5: generatory numerów (niezależne)", "10, 11, 12 | 100", () -> {
            IntSupplier first = exercise5(10);
            IntSupplier second = exercise5(100);
            return first.getAsInt() + ", " + first.getAsInt() + ", " + first.getAsInt() + " | " + second.getAsInt();
        });
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of(5, 3, 7, 3, 6, 4), () -> keepNumbers(SampleData.numbers(), solution1(3, 7)));
        Check.equal("ćw. 2 (wzorzec)", 40, () -> solution2(SampleData.words()));
        Check.equal("ćw. 3 (wzorzec)", List.of("1. Ala", "2. Ola", "3. Ela"), () -> callAll(solution3(List.of("Ala", "Ola", "Ela"))));
        Check.equal("ćw. 4 (wzorzec)", "[kawa, mleko]", () -> {
            List<String> cart = new ArrayList<>(List.of("kawa", "mleko"));
            Supplier<String> snapshot = solution4(cart);
            cart.add("cukier");
            return snapshot.get();
        });
        Check.equal("ćw. 5 (wzorzec)", "10, 11, 12 | 100", () -> {
            IntSupplier first = solution5(10);
            IntSupplier second = solution5(100);
            return first.getAsInt() + ", " + first.getAsInt() + ", " + first.getAsInt() + " | " + second.getAsInt();
        });
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /** keepNumbers = zostaw liczby spełniające warunek (pomocnicza do ćwiczenia 1). */
    static List<Integer> keepNumbers(List<Integer> numbers, Predicate<Integer> condition) {
        List<Integer> result = new ArrayList<>();
        for (Integer n : numbers) {
            if (condition.test(n)) {
                result.add(n);
            }
        }
        return result;
    }

    /** callAll = wywołaj wszystkie. Zbiera wyniki get() z listy Supplierów (pomocnicza do ćwiczenia 3). */
    static List<String> callAll(List<Supplier<String>> suppliers) {
        List<String> result = new ArrayList<>();
        for (Supplier<String> s : suppliers) {
            result.add(s.get());
        }
        return result;
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć predykat, który przepuszcza liczby z przedziału [min, max] (obie granice włącznie).
     * Podpowiedź: lambda może użyć parametrów min i max — są effectively final (metoda ich nie zmienia).
     */
    static Predicate<Integer> exercise1(int min, int max) {
        // TODO: twoje rozwiązanie
        return n -> false;
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ KOD, KTÓRY SIĘ NIE KOMPILUJE. Poniższa metoda ma zwrócić sumę długości słów
     * dłuższych niż 5 liter, ale nie przechodzi kompilacji:
     * <pre>{@code
     *   int total = 0;
     *   words.forEach(w -> {
     *       if (w.length() > 5) {
     *           total += w.length();      // błąd: total nie jest effectively final
     *       }
     *   });
     *   return total;
     * }</pre>
     * Napraw ją BEZ AtomicInteger i bez tablicy jednoelementowej.
     * Podpowiedź: to zadanie nie potrzebuje lambdy — zwykła pętla for-each może zmieniać zmienną lokalną.
     */
    static int exercise2(List<String> words) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć listę Supplierów: i-ty Supplier ma zwracać numer (od 1) i imię, np. "1. Ala".
     * Podpowiedź: pętla z indeksem {@code for (int i = 0; ...)} — ale i nie możesz użyć w lambdzie!
     * Skopiuj numer i imię do nowych zmiennych w KAŻDYM obrocie pętli (sekcja 3).
     */
    static List<Supplier<String>> exercise3(List<String> names) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zwróć Supplier, który zwraca {@code toString()} listy W TAKIM STANIE, w jakim była
     * w chwili wywołania exercise4 — późniejsze zmiany listy nie mogą być widoczne.
     * Podpowiedź: lambda {@code () -> list.toString()} zobaczy AKTUALNY stan (sekcja 4) — to błąd.
     * Zrób kopię listy (np. {@code List.copyOf(list)}) i przechwyć KOPIĘ.
     */
    static Supplier<String> exercise4(List<String> list) {
        // TODO: twoje rozwiązanie
        return () -> "";
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): zwróć „generator numerów”: każde wywołanie getAsInt() zwraca kolejną liczbę,
     * zaczynając od start (10, 11, 12...). Dwa generatory utworzone osobno mają być NIEZALEŻNE.
     * Podpowiedź: zmienna lokalna int odpada (effectively final), pole static odpada (byłoby wspólne dla wszystkich
     * generatorów!). Stan musi być PRYWATNY dla jednej lambdy: {@code AtomicInteger} utworzony wewnątrz exercise5
     * i {@code getAndIncrement()} (pobierz, potem zwiększ). To jeden z nielicznych dobrych powodów dla AtomicInteger w lambdzie.
     */
    static IntSupplier exercise5(int start) {
        // TODO: twoje rozwiązanie
        return () -> 0;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static Predicate<Integer> solution1(int min, int max) {
        return n -> n >= min && n <= max;
    }

    static int solution2(List<String> words) {
        int total = 0;
        for (String w : words) {                                    // pętla może zmieniać zmienne lokalne — lambda nie
            if (w.length() > 5) {
                total += w.length();
            }
        }
        return total;
    }

    static List<Supplier<String>> solution3(List<String> names) {
        List<Supplier<String>> result = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            int number = i + 1;                                     // nowa zmienna w każdym obrocie
            String name = names.get(i);
            result.add(() -> number + ". " + name);
        }
        return result;
    }

    static Supplier<String> solution4(List<String> list) {
        List<String> copy = List.copyOf(list);                      // copyOf = niemodyfikowalna KOPIA (Java 10+)
        return () -> copy.toString();
        // Alternatywa: String text = list.toString(); return () -> text;  — też zamraża stan w chwili tworzenia.
    }

    static IntSupplier solution5(int start) {
        AtomicInteger next = new AtomicInteger(start);              // każdy generator ma WŁASNY licznik
        return () -> next.getAndIncrement();                        // getAndIncrement = zwróć obecną wartość, potem zwiększ
        // Krócej: return next::getAndIncrement;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Zmienna przypisana raz i nigdy później niezmieniana. Słowo final nie jest potrzebne — wystarczy, że jej nie zmieniamy
     *      (ani przed lambdą, ani po niej, ani w niej).
     *   2. sum += n zmienia zmienną lokalną w lambdzie → błąd „... must be final or effectively final”.
     *      Rozwiązanie: zwykła pętla for-each (albo w t16_streams: numbers.stream().mapToInt(Integer::intValue).sum()).
     *   3. Lambda dostaje KOPIĘ wartości (zmienna lokalna znika z końcem metody, a lambda może działać później lub
     *      w innym wątku). Gdyby wolno było zmieniać oryginał, kopia i oryginał by się rozjechały — lambda widziałaby
     *      starą wartość, a programista spodziewałby się nowej.
     *   4. [x, x] — referencja list się nie zmienia, ale stan listy tak; każde run() dodaje element.
     *   5. Lambda: "Główny" (this = obiekt otaczający). Klasa anonimowa: jej własny toString() — zwykle coś w stylu
     *      NazwaKlasy$1@1b6d3586 (albo to, co nadpisze). Obiekt zewnętrzny w klasie anonimowej: NazwaKlasy.this.
     *   6. Parametr lambdy nie może mieć nazwy istniejącej zmiennej lokalnej („variable name is already defined”).
     *      Zmień nazwę parametru, np. s -> s.toUpperCase().
     *   7. i jest zmieniane przez i++, więc nie jest effectively final. W for-each zmienna s jest NOWA w każdym obrocie
     *      i nigdy niezmieniana — jest effectively final.
     *   8. Nową — pole jest czytane w chwili wywołania lambdy, a nie kopiowane przy jej tworzeniu.
     */
    // </editor-fold>
}
