package t23_modern_java;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Customer;
import helpers.model.Employee;
import helpers.model.Department;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Java 8 — wielka zmiana stylu pisania kodu
 *        (lambda = funkcja anonimowa; default method = metoda domyślna; stream = strumień)
 *
 * W SKRÓCIE:
 *   Java 8 (marzec 2014, wydanie LTS) to najważniejsze wydanie w historii języka. Dodało lambdy, referencje do metod,
 *   metody default i static w interfejsach, Stream API, Optional, nowe java.time i CompletableFuture. Prawie cały
 *   kod w tym kursie korzysta z tych narzędzi — ta lekcja układa je w jedną opowieść: co było PRZED, co jest PO i dlaczego.
 *   Wszystko tutaj jest „Java 8”, więc przy nowościach nie piszemy numeru wydania.
 *
 * ANALOGIA:
 *   Wyobraź sobie kuchnię, w której do każdej czynności (pokrój, zalej, wymieszaj) trzeba było zatrudnić osobnego
 *   kucharza z imieniem, stanowiskiem i regulaminem — to klasa anonimowa. Lambda to kartka z jednym poleceniem
 *   („pokrój w kostkę”), którą wręczasz komukolwiek. Strumień to taśma w fabryce: kładziesz produkty,
 *   stawiasz kolejne stanowiska (filtruj, przekształć), a taśma rusza dopiero, gdy ktoś na końcu naciśnie przycisk.
 *
 * JAK TO DZIAŁA:
 *   1. Interfejs funkcyjny = interfejs z JEDNĄ metodą abstrakcyjną (może mieć też default i static).
 *      Lambda {@code (a, b) -> a + b} to krótki zapis obiektu, który ten interfejs implementuje.
 *   2. Referencja do metody {@code Klasa::metoda} to skrót lambdy, która tylko woła istniejącą metodę. Są 4 rodzaje:
 *        statyczna        Integer::parseInt         (x) -> Integer.parseInt(x)
 *        związana         prefix::concat            (x) -> prefix.concat(x)
 *        niezwiązana      String::toUpperCase       (s) -> s.toUpperCase()
 *        konstruktor      ArrayList::new            () -> new ArrayList<>()
 *   3. Metoda default w interfejsie ma ciało; dzięki niej dodano List.sort czy Iterable.forEach bez psucia
 *      milionów istniejących klas. Konflikt dwóch metod default rozstrzygają trzy reguły (sekcja 3).
 *   4. Strumień: źródło → operacje pośrednie (leniwe) → operacja końcowa (uruchamia całość).
 *   5. Zmienna użyta w lambdzie musi być „efektywnie finalna” — nie wolno jej zmienić po przypisaniu.
 *
 *   Wydania wokół Javy 8:   8 (2014, LTS) → 9 (2017) → 10 → 11 (2018, LTS) → ... → 17 (2021, LTS) → 21 (2023, LTS)
 *
 * SŁÓWKA:
 *   lambda = funkcja anonimowa; functional interface = interfejs funkcyjny; method reference = referencja do metody;
 *   default = domyślna; effectively final = efektywnie finalna; supplier = dostawca; consumer = konsument;
 *   predicate = orzeczenie (test tak/nie); pipeline = potok; lazy = leniwy; encode = zakoduj.
 *
 * ZOBACZ TEŻ: t13_lambdas/Lambda01FromAnonymousToLambda (lambdy w głąb), t13_lambdas/Lambda04MethodReferences (referencje),
 *   t14_optional/Optional01Basics (Optional), t16_streams/Streams11GroupingBy (strumienie),
 *   t17_datetime/DateTime01LocalDateTime (java.time), t21_concurrency/Concurrency04Executors (pule wątków), t21_concurrency/Concurrency05CompletableFuture,
 *   t23_modern_java/Modern02Var (następne wydania)
 * </pre>
 */
public class Modern01Java8 {

    public static void main(String[] args) {
        title("Modern01 — Java 8: wielka zmiana");

        lambdasAndFunctionalInterfaces();   // lambdas and functional interfaces = lambdy i interfejsy funkcyjne
        methodReferences();                 // method references = referencje do metod
        defaultAndStaticMethods();          // default and static methods = metody default i static
        streamsBeforeAfter();               // streams before/after = strumienie przed i po
        optionalInsteadOfNull();            // optional instead of null = Optional zamiast null
        dateTimeInsteadOfDate();            // date-time instead of Date = java.time zamiast Date
        newCollectionMethods();             // new collection methods = nowe metody kolekcji
        joinAndBase64();                    // join and Base64 = łączenie napisów i Base64
        effectivelyFinal();                 // effectively final = efektywnie finalna
        completableFutureIntro();           // CompletableFuture intro = wprowadzenie do CompletableFuture
        exercises();                        // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. LAMBDY I INTERFEJSY FUNKCYJNE
    // =================================================================================================

    /** Własny interfejs funkcyjny: jedna metoda abstrakcyjna + metoda default. */
    @FunctionalInterface
    interface Validator<T> {
        boolean validate(T value);   // validate = sprawdź

        // and = oraz; metoda default składa dwa walidatory w jeden
        default Validator<T> and(Validator<T> other) {
            return value -> validate(value) && other.validate(value);
        }
    }

    /** Klasa pomocnicza do pokazania, czym jest {@code this} w lambdzie i w klasie anonimowej. */
    static class Greeter {
        private final String name = "Greeter";   // name = nazwa

        Supplier<String> viaLambda() {           // via lambda = przez lambdę
            return () -> this.name;              // w lambdzie this = obiekt Greeter (otaczający)
        }

        Supplier<String> viaAnonymous() {        // via anonymous = przez klasę anonimową
            return new Supplier<String>() {
                private final String name = "anonim";

                @Override
                public String get() {            // get = pobierz
                    return this.name;            // w klasie anonimowej this = ten nowy obiekt anonimowy
                }
            };
        }
    }

    /**
     * 1. Klasa anonimowa → lambda. Lambda działa wszędzie tam, gdzie oczekiwany jest interfejs funkcyjny
     * (interfejs z jedną metodą abstrakcyjną). Typ lambdy wynika z kontekstu, w którym ją piszesz.
     */
    @SuppressWarnings("Convert2Lambda")
    static void lambdasAndFunctionalInterfaces() {
        section("1. Lambdy i interfejsy funkcyjne");

        List<String> names = new ArrayList<>(List.of("Zofia", "Adam", "Ola", "Bartosz"));

        // PRZED (Java 7): klasa anonimowa — 5 linii szumu na jedną myśl „porównaj długości”.
        Collections.sort(names, new Comparator<String>() {   // Comparator = porównywacz
            @Override
            public int compare(String a, String b) {          // compare = porównaj
                return Integer.compare(a.length(), b.length());
            }
        });
        show("PRZED (klasa anonimowa)", names);
        // WYNIK: PRZED (klasa anonimowa) → [Ola, Adam, Zofia, Bartosz]

        // PO (Java 8): ta sama logika w jednej linii. List.sort = metoda default dodana w Javie 8.
        names = new ArrayList<>(List.of("Zofia", "Adam", "Ola", "Bartosz"));
        names.sort((a, b) -> Integer.compare(a.length(), b.length()));   // sort = sortuj
        show("PO (lambda)", names);
        // WYNIK: PO (lambda) → [Ola, Adam, Zofia, Bartosz]

        // Najczęstsze interfejsy funkcyjne z java.util.function — warto znać je „z nazwy”:
        //   Function<T,R>     T → R           apply     (przekształcenie, map)
        //   Predicate<T>      T → boolean     test      (warunek, filter)
        //   Consumer<T>       T → nic         accept    (efekt uboczny, forEach)
        //   Supplier<T>       nic → T         get       (dostawca, leniwe tworzenie)
        //   UnaryOperator<T>  T → T           apply     (szczególny przypadek Function)
        //   BiFunction<T,U,R> (T, U) → R      apply     (dwa argumenty)
        Function<String, Integer> length = s -> s.length();
        Predicate<String> isLong = s -> s.length() > 4;
        Consumer<String> printer = s -> System.out.println("   (efekt uboczny) " + s);
        Supplier<String> supplier = () -> "dostarczone";
        UnaryOperator<String> shout = s -> s.toUpperCase(Locale.ROOT);   // shout = krzyknij
        BiFunction<Integer, Integer, Integer> add = (a, b) -> a + b;      // add = dodaj

        show("Function.apply", length.apply("lambda"));
        // WYNIK: Function.apply → 6
        show("Predicate.test", isLong.test("Ola"));
        // WYNIK: Predicate.test → false
        printer.accept("Consumer.accept");
        // WYNIK:    (efekt uboczny) Consumer.accept
        show("Supplier.get", supplier.get());
        // WYNIK: Supplier.get → dostarczone
        show("UnaryOperator.apply", shout.apply("hej"));
        // WYNIK: UnaryOperator.apply → HEJ
        show("BiFunction.apply", add.apply(20, 22));
        // WYNIK: BiFunction.apply → 42

        // Własny interfejs funkcyjny + metoda default and (Validator z początku pliku).
        Validator<String> notEmpty = s -> !s.isEmpty();                 // not empty = niepusty
        Validator<String> shortText = s -> s.length() <= 5;             // short text = krótki tekst
        Validator<String> both = notEmpty.and(shortText);               // both = oba
        show("walidator \"Ola\"", both.validate("Ola"));
        // WYNIK: walidator "Ola" → true
        show("walidator \"Bartosz\"", both.validate("Bartosz"));
        // WYNIK: walidator "Bartosz" → false

        // PUŁAPKA: this w lambdzie i w klasie anonimowej znaczy co innego.
        //   Lambda NIE tworzy nowego zasięgu „this” — to wciąż obiekt, w którym ją napisałeś.
        //   Klasa anonimowa to osobny obiekt, więc this wskazuje na niego (i może mieć własne pola o tej samej nazwie).
        Greeter greeter = new Greeter();
        show("this w lambdzie", greeter.viaLambda().get());
        // WYNIK: this w lambdzie → Greeter
        show("this w klasie anonimowej", greeter.viaAnonymous().get());
        // WYNIK: this w klasie anonimowej → anonim

        // PUŁAPKA: @FunctionalInterface na interfejsie z DWIEMA metodami abstrakcyjnymi to błąd kompilacji
        //   („nie jest interfejsem funkcyjnym”). Adnotacja sama niczego nie włącza — to tylko zabezpieczenie,
        //   które pilnuje, żeby ktoś później nie dopisał drugiej metody i nie zepsuł wszystkich lambd.
        // DOBRA PRAKTYKA: zanim napiszesz własny interfejs funkcyjny, sprawdź java.util.function —
        //   w 90% przypadków jest tam gotowy (Function, Predicate, Supplier...). Własny pisz, gdy nazwa wnosi znaczenie
        //   (np. Validator) albo gdy potrzebujesz wyjątków sprawdzanych w sygnaturze.
        note("Lambda to nie klasa anonimowa „w skrócie”: ma inne this i nie tworzy osobnego pliku .class przy kompilacji.");
        // WYNIK: ℹ Lambda to nie klasa anonimowa „w skrócie”: ma inne this i nie tworzy osobnego pliku .class przy kompilacji.
    }

    // =================================================================================================
    // 2. REFERENCJE DO METOD
    // =================================================================================================

    /**
     * 2. Cztery rodzaje referencji do metod. Zamieniaj lambdę na referencję, gdy lambda tylko woła jedną metodę
     * i nic poza tym — wtedy kod czyta się jak zdanie: {@code map(String::trim)}.
     */
    static void methodReferences() {
        section("2. Referencje do metod");

        // 1) statyczna: Klasa::metodaStatyczna
        Function<String, Integer> parse = Integer::parseInt;              // parse = zamień tekst na liczbę
        show("statyczna Integer::parseInt", parse.apply("123") + 1);
        // WYNIK: statyczna Integer::parseInt → 124

        // 2) związana (bound): obiekt::metoda — obiekt jest „zapamiętany” w momencie tworzenia referencji
        String prefix = "Pan ";                                           // prefix = przedrostek
        Function<String, String> addPrefix = prefix::concat;              // concat = sklej
        show("związana prefix::concat", addPrefix.apply("Adam"));
        // WYNIK: związana prefix::concat → Pan Adam

        // 3) niezwiązana (unbound): Klasa::metodaInstancji — pierwszy argument staje się obiektem, na którym wołamy
        Function<String, String> upper = String::toUpperCase;            // to upper case = na wielkie litery
        show("niezwiązana String::toUpperCase", upper.apply("kot"));
        // WYNIK: niezwiązana String::toUpperCase → KOT
        BiFunction<String, String, Boolean> startsWith = String::startsWith;   // starts with = zaczyna się od
        show("niezwiązana, dwa argumenty", startsWith.apply("Kowalski", "Kow"));
        // WYNIK: niezwiązana, dwa argumenty → true

        // 4) konstruktor: Klasa::new
        Supplier<List<String>> newList = ArrayList::new;                  // new list = nowa lista
        List<String> fresh = newList.get();                               // fresh = świeża
        fresh.add("x");
        show("konstruktor ArrayList::new", fresh);
        // WYNIK: konstruktor ArrayList::new → [x]
        Function<String, StringBuilder> newBuilder = StringBuilder::new;  // builder = budowniczy tekstu
        show("konstruktor z argumentem", newBuilder.apply("abc").reverse());
        // WYNIK: konstruktor z argumentem → cba

        // Po co? Porównaj w strumieniu: lambda vs referencja.
        List<Employee> employees = SampleData.employees();
        List<String> viaLambda = employees.stream().map(e -> e.name()).limit(2).collect(Collectors.toList());
        List<String> viaReference = employees.stream().map(Employee::name).limit(2).collect(Collectors.toList());
        show("lambda == referencja", viaLambda.equals(viaReference));
        // WYNIK: lambda == referencja → true
        show("imiona", viaReference);
        // WYNIK: imiona → [Anna Nowak, Piotr Kowalski]

        // PUŁAPKA: niektóre referencje są niejednoznaczne. Integer::toString ma dwie pasujące wersje do
        //   Function<Integer,String>: statyczną toString(int) i instancyjną toString(). Kompilator odmawia
        //   („odwołanie do toString jest niejednoznaczne”). Wtedy napisz lambdę: i -> Integer.toString(i).
        // PUŁAPKA: referencja związana oblicza obiekt RAZ, przy tworzeniu. Jeśli zmienisz zmienną
        //   (a Java i tak nie pozwoli, bo musi być efektywnie finalna), lambda zachowałaby się inaczej niż referencja.
        // DOBRA PRAKTYKA: jeśli lambda ma więcej niż jedną linię lub nietrywialną logikę, wyciągnij ją do metody
        //   z nazwą i użyj referencji — nazwa metody jest dokumentacją, a metodę można testować osobno.
    }

    // =================================================================================================
    // 3. METODY DEFAULT I STATIC W INTERFEJSACH
    // =================================================================================================

    interface Greeting {
        default String hello() { return "Cześć z Greeting"; }   // hello = witaj
    }

    interface Polite extends Greeting {
        @Override
        default String hello() { return "Dzień dobry z Polite"; }
    }

    interface Loud {
        default String hello() { return "HEJ z Loud"; }          // loud = głośny
    }

    interface MathTools {
        static int twice(int x) { return 2 * x; }                // twice = dwukrotność
    }

    static class Base {
        public String hello() { return "Hello z klasy Base"; }
    }

    /** Reguła 1: klasa wygrywa z interfejsem. */
    static class ClassWins extends Base implements Greeting { }

    /** Reguła 2: bardziej szczegółowy interfejs (Polite rozszerza Greeting) wygrywa. */
    static class MoreSpecificWins implements Greeting, Polite { }

    /** Reguła 3: dwa niezwiązane interfejsy → musisz sam nadpisać metodę i wybrać przez X.super.m(). */
    static class MustOverride implements Greeting, Loud {
        @Override
        public String hello() {
            return Greeting.super.hello() + " + " + Loud.super.hello();
        }
    }

    /**
     * 3. Metody default i static w interfejsach. Po co default? Żeby rozbudować interfejs (np. List dostała sort,
     * Iterable dostał forEach) bez konieczności poprawiania każdej klasy, która go kiedykolwiek zaimplementowała.
     */
    static void defaultAndStaticMethods() {
        section("3. Metody default i static w interfejsach");

        show("klasa wygrywa", new ClassWins().hello());
        // WYNIK: klasa wygrywa → Hello z klasy Base
        show("bardziej szczegółowy interfejs", new MoreSpecificWins().hello());
        // WYNIK: bardziej szczegółowy interfejs → Dzień dobry z Polite
        show("ręczny wybór X.super.m()", new MustOverride().hello());
        // WYNIK: ręczny wybór X.super.m() → Cześć z Greeting + HEJ z Loud
        show("metoda static w interfejsie", MathTools.twice(21));
        // WYNIK: metoda static w interfejsie → 42

        // Zasady rozstrzygania konfliktu (diamentu) — kolejność sprawdzania:
        //   1. Metoda z KLASY (także odziedziczona po nadklasie) zawsze wygrywa z default z interfejsu.
        //   2. Jeśli nie ma takiej metody, wygrywa interfejs „najbliższy” — ten, który rozszerza pozostałe.
        //   3. Jeśli zostają dwa niezwiązane interfejsy z tą samą metodą default, klasa NIE skompiluje się,
        //      dopóki nie nadpiszesz metody (i nie wybierzesz ręcznie przez Interfejs.super.metoda()).

        // PUŁAPKA: metoda default nie może „nadpisać” metod z Object (equals, hashCode, toString) — błąd kompilacji.
        //   Powód: klasa zawsze dziedziczy je z Object, a reguła 1 mówi, że metoda z klasy wygrywa — default byłby martwy.
        // PUŁAPKA: metody static z interfejsu NIE są dziedziczone. Wołasz je tylko przez nazwę interfejsu:
        //   MathTools.twice(2), a nie przez obiekt klasy, która interfejs implementuje.
        // DOBRA PRAKTYKA: używaj default do ewolucji interfejsu i do metod pomocniczych opartych na metodzie
        //   abstrakcyjnej (jak and w Validator). Nie rób z interfejsu „klasy z polami” — stanu w nim trzymać nie można.
        note("W Javie 9 doszły metody prywatne w interfejsach — zobacz Modern06ApiAdditions.");
        // WYNIK: ℹ W Javie 9 doszły metody prywatne w interfejsach — zobacz Modern06ApiAdditions.
    }

    // =================================================================================================
    // 4. STREAM API
    // =================================================================================================

    /**
     * 4. Pętla → strumień. Strumień opisuje CO chcesz uzyskać (filtruj, posortuj, zbierz), a nie JAK iterować.
     * Pełny kurs: t16_streams. Tutaj tylko porównanie PRZED / PO oraz dwie zasady: leniwość i jednorazowość.
     */
    static void streamsBeforeAfter() {
        section("4. Stream API — przed i po");

        List<Employee> employees = SampleData.employees();

        // PRZED: pętla, zmienna pomocnicza, osobne sortowanie.
        List<Employee> highPaidIt = new ArrayList<>();                   // high paid = dobrze opłacany
        for (Employee e : employees) {
            if (e.department() == Department.IT && e.salary() > 10000) {
                highPaidIt.add(e);
            }
        }
        highPaidIt.sort(Comparator.comparingInt(Employee::salary).reversed());   // comparing int = porównaj po int
        List<String> before = new ArrayList<>();
        for (Employee e : highPaidIt) {
            before.add(e.name());
        }
        show("PRZED (pętla)", before);
        // WYNIK: PRZED (pętla) → [Michał Lewandowski, Anna Nowak, Ewa Woźniak]

        // PO: jeden potok. filter = filtruj, sorted = posortowane, map = przekształć, collect = zbierz.
        List<String> after = employees.stream()
                .filter(e -> e.department() == Department.IT)
                .filter(e -> e.salary() > 10000)
                .sorted(Comparator.comparingInt(Employee::salary).reversed())
                .map(Employee::name)
                .collect(Collectors.toList());   // w Javie 16+ krócej: .toList()
        show("PO (strumień)", after);
        // WYNIK: PO (strumień) → [Michał Lewandowski, Anna Nowak, Ewa Woźniak]
        show("to samo?", before.equals(after));
        // WYNIK: to samo? → true

        // Zasada 1: operacje pośrednie są LENIWE — bez operacji końcowej nic się nie dzieje.
        List<String> log = new ArrayList<>();                            // log = dziennik
        Stream<String> lazy = Stream.of("a", "b", "c").filter(s -> {     // lazy = leniwy
            log.add("sprawdzam " + s);                                   // efekt uboczny — tylko do demonstracji
            return true;
        });
        show("po filter (bez końcowej)", log);
        // WYNIK: po filter (bez końcowej) → []
        lazy.collect(Collectors.toList());
        show("po collect", log);
        // WYNIK: po collect → [sprawdzam a, sprawdzam b, sprawdzam c]

        // Zasada 2: strumień jest JEDNORAZOWY — po operacji końcowej jest „zużyty”.
        Stream<String> once = Stream.of("x", "y");                       // once = raz
        once.count();                                                    // count = policz
        expectThrows("drugie użycie strumienia", () -> once.count());
        // WYNIK: ✔ drugie użycie strumienia → rzucono IllegalStateException: stream has already been operated upon or closed

        // PUŁAPKA: println/dodawanie do listy w środku potoku to efekt uboczny. Działa w demo, ale psuje się
        //   przy strumieniach równoległych (parallel) i gdy kompilator pominie operację (np. count() może nie
        //   uruchomić filtrów, jeśli rozmiar da się policzyć wprost ze źródła).
        // DOBRA PRAKTYKA: potok = czyste funkcje (te same dane → ten sam wynik, bez zmian na zewnątrz),
        //   a wynik zbieraj przez collect. Zobacz t16_streams/Streams11GroupingBy.
    }

    // =================================================================================================
    // 5. OPTIONAL
    // =================================================================================================

    /** Pomocnicza: zwraca domenę adresu e-mail albo pusty Optional. */
    static Optional<String> domainOf(Customer customer) {   // domain of = domena z
        return customer.findEmail().map(email -> email.substring(email.indexOf('@') + 1));
    }

    /**
     * 5. Optional zamiast null. Optional to pudełko, które jest albo puste, albo ma wartość. Sygnatura metody
     * mówi wprost „może nie być wyniku” — nie trzeba zgadywać ani czytać dokumentacji. Pełny kurs: t14_optional.
     */
    static void optionalInsteadOfNull() {
        section("5. Optional zamiast null");

        List<Customer> customers = SampleData.customers();
        Customer maria = customers.get(1);   // Maria Nowak — brak e-maila
        Customer jan = customers.get(0);     // Jan Kowalski — ma e-mail

        // PRZED: null i ręczne sprawdzanie; łatwo zapomnieć o ifie i dostać NullPointerException.
        String raw = null;                   // raw = surowy
        if (maria.findEmail().isPresent()) { // is present = jest obecny
            raw = maria.findEmail().get();
        }
        String shownBefore = raw != null ? raw.toUpperCase(Locale.ROOT) : "brak e-maila";
        show("PRZED (null i if)", shownBefore);
        // WYNIK: PRZED (null i if) → brak e-maila

        // PO: map przekształca wartość (jeśli jest), orElse podaje wartość zastępczą (or else = w przeciwnym razie).
        show("PO, Maria", maria.findEmail().map(s -> s.toUpperCase(Locale.ROOT)).orElse("brak e-maila"));
        // WYNIK: PO, Maria → brak e-maila
        show("PO, Jan", jan.findEmail().map(s -> s.toUpperCase(Locale.ROOT)).orElse("brak e-maila"));
        // WYNIK: PO, Jan → JAN@EXAMPLE.COM
        show("domena Jana", domainOf(jan));
        // WYNIK: domena Jana → Optional[example.com]
        show("domena Marii", domainOf(maria));
        // WYNIK: domena Marii → Optional.empty

        // PUŁAPKA: get() na pustym Optional rzuca wyjątek — to ten sam błąd co NullPointerException, tylko w innym miejscu.
        expectThrows("get() na pustym", () -> maria.findEmail().get());
        // WYNIK: ✔ get() na pustym → rzucono NoSuchElementException: No value present

        // PUŁAPKA: orElse(x) oblicza x ZAWSZE (nawet gdy wartość jest). Gdy x jest drogie, użyj orElseGet(Supplier).
        AtomicInteger calls = new AtomicInteger();                       // calls = wywołania
        jan.findEmail().orElse(expensive(calls));                        // expensive = kosztowne
        int afterOrElse = calls.get();
        jan.findEmail().orElseGet(() -> expensive(calls));               // or else get = albo pobierz
        show("wywołania po orElse / orElseGet", afterOrElse + " / " + calls.get());
        // WYNIK: wywołania po orElse / orElseGet → 1 / 1

        // DOBRA PRAKTYKA: Optional jako TYP WYNIKU metody — tak. Jako pole klasy, parametr metody albo element
        //   kolekcji — nie (zbędny narzut, kłopotliwa serializacja, a parametr i tak może być null).
        //   Kolekcji nigdy nie zwracaj jako Optional<List> — pusta lista to już „brak wyniku”.
    }

    private static String expensive(AtomicInteger calls) {
        calls.incrementAndGet();             // increment and get = zwiększ i pobierz
        return "wartość zastępcza";
    }

    // =================================================================================================
    // 6. JAVA.TIME ZAMIAST DATE
    // =================================================================================================

    /**
     * 6. java.time zamiast Date i Calendar. Stare klasy były zmienne, miały miesiące liczone od zera
     * i mieszały datę z godziną oraz strefą. Nowe są niezmienne (każda operacja zwraca NOWY obiekt).
     */
    static void dateTimeInsteadOfDate() {
        section("6. java.time zamiast Date");

        // PRZED: Calendar — miesiące od 0 (styczeń = 0), obiekt zmienny, wiele pułapek.
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ROOT);
        calendar.clear();                                                // clear = wyczyść
        calendar.set(2026, Calendar.JANUARY, 5);                         // January = styczeń
        show("Calendar.MONTH dla stycznia", calendar.get(Calendar.MONTH));
        // WYNIK: Calendar.MONTH dla stycznia → 0

        // PUŁAPKA: Date jest ZMIENNY — przekazanie go do metody pozwala jej zmienić Twoją datę.
        Date date = new Date(0L);                                        // 0 ms od 1970-01-01 UTC
        Date same = date;                                                // ten sam obiekt, nie kopia
        same.setTime(86_400_000L);                                       // set time = ustaw czas (+1 doba)
        show("Date po zmianie przez drugą zmienną", date.getTime());
        // WYNIK: Date po zmianie przez drugą zmienną → 86400000
        // Dodatkowo: new Date(126, 0, 5) liczy rok od 1900 i jest przestarzały (deprecated) — nie używaj.

        // PO: LocalDate — niezmienny, miesiące 1–12, bez strefy i godziny.
        LocalDate start = LocalDate.of(2026, 1, 5);                      // start = początek
        LocalDate later = start.plusDays(30);                            // plus days = dodaj dni
        show("start (bez zmian)", start);
        // WYNIK: start (bez zmian) → 2026-01-05
        show("start + 30 dni", later);
        // WYNIK: start + 30 dni → 2026-02-04
        show("dzień tygodnia startu", start.getDayOfWeek());
        // WYNIK: dzień tygodnia startu → MONDAY
        show("dni między", ChronoUnit.DAYS.between(start, later));
        // WYNIK: dni między → 30
        show("poniedziałek?", start.getDayOfWeek() == DayOfWeek.MONDAY);
        // WYNIK: poniedziałek? → true
        show("miesiąc jako liczba", start.getMonthValue());
        // WYNIK: miesiąc jako liczba → 1

        // PUŁAPKA: LocalDate.of(2026, 2, 30) rzuca wyjątek od razu — stary Calendar po cichu „przeskoczyłby” na marzec.
        expectThrows("nieistniejąca data", () -> LocalDate.of(2026, 2, 30));
        // WYNIK: ✔ nieistniejąca data → rzucono DateTimeException: Invalid date 'FEBRUARY 30'

        // DOBRA PRAKTYKA: w nowym kodzie nie używaj Date/Calendar/SimpleDateFormat (ten ostatni nie jest też
        //   bezpieczny wątkowo). Do granic ze starym API służą Date.from(instant) i date.toInstant().
        //   Wszystko o java.time: t17_datetime/DateTime01LocalDateTime.
    }

    // =================================================================================================
    // 7. NOWE METODY KOLEKCJI I MAP
    // =================================================================================================

    /**
     * 7. Java 8 dodała do kolekcji i map wygodne metody, które zastępują pisane ręcznie wzorce
     * „sprawdź, czy jest — jeśli nie, utwórz — dodaj”.
     */
    static void newCollectionMethods() {
        section("7. Nowe metody kolekcji i map");

        List<String> words = SampleData.words();   // java, stream, lambda, kolekcja, java, mapa, ...

        // PRZED: zliczanie słów — containsKey + get + put (trzy odwołania do mapy na jedno słowo).
        Map<String, Integer> countsBefore = new TreeMap<>();             // counts = liczniki
        for (String w : words) {
            if (countsBefore.containsKey(w)) {                           // contains key = zawiera klucz
                countsBefore.put(w, countsBefore.get(w) + 1);
            } else {
                countsBefore.put(w, 1);
            }
        }
        show("PRZED (zliczanie)", countsBefore);
        // WYNIK: PRZED (zliczanie) → {enum=1, java=3, kolekcja=1, lambda=1, lista=1, mapa=1, optional=1, rekord=1, stream=2}

        // PO: merge = scal. Gdy klucza nie ma, wstawia 1; gdy jest, wywołuje Integer::sum (starą i nową wartość).
        Map<String, Integer> countsAfter = new TreeMap<>();
        words.forEach(w -> countsAfter.merge(w, 1, Integer::sum));       // for each = dla każdego
        show("PO (merge)", countsAfter);
        // WYNIK: PO (merge) → {enum=1, java=3, kolekcja=1, lambda=1, lista=1, mapa=1, optional=1, rekord=1, stream=2}

        // getOrDefault = pobierz albo domyślną
        show("getOrDefault", countsAfter.getOrDefault("python", 0));
        // WYNIK: getOrDefault → 0

        // computeIfAbsent = oblicz, jeśli brak — idealne do map „klucz → lista” (grupowanie)
        Map<Integer, List<String>> byLength = new TreeMap<>();           // by length = według długości
        for (String w : List.of("java", "mapa", "enum", "lista", "lambda")) {
            byLength.computeIfAbsent(w.length(), k -> new ArrayList<>()).add(w);
        }
        show("computeIfAbsent", byLength);
        // WYNIK: computeIfAbsent → {4=[java, mapa, enum], 5=[lista], 6=[lambda]}

        // removeIf = usuń, jeśli (warunek); replaceAll = zamień wszystkie
        List<String> list = new ArrayList<>(List.of("a", "bb", "ccc", "dd"));
        list.removeIf(s -> s.length() == 2);                             // remove if = usuń jeśli
        list.replaceAll(s -> s + "!");                                   // replace all = zamień wszystkie
        show("removeIf + replaceAll", list);
        // WYNIK: removeIf + replaceAll → [a!, ccc!]

        // Komparatory budowane łańcuchem: comparing (porównaj po), thenComparing (potem po), reversed (odwrócone).
        List<Employee> sorted = new ArrayList<>(SampleData.employees());
        sorted.sort(Comparator.comparing(Employee::department)
                .thenComparing(Employee::salary, Comparator.reverseOrder()));   // reverse order = odwrotna kolejność
        show("dział, potem pensja malejąco",
                sorted.stream().limit(4).map(Employee::name).collect(Collectors.toList()));
        // WYNIK: dział, potem pensja malejąco → [Michał Lewandowski, Anna Nowak, Ewa Woźniak, Piotr Kowalski]

        // PUŁAPKA: jeśli funkcja w merge zwróci null, klucz jest USUWANY z mapy (nie dostajesz wartości null).
        Map<String, Integer> small = new TreeMap<>(Map.of("a", 1, "b", 2));   // small = mała
        small.merge("a", 5, (oldValue, newValue) -> null);               // old = stara, new = nowa
        show("merge zwracający null", small);
        // WYNIK: merge zwracający null → {b=2}

        // PUŁAPKA: Comparator.comparing(e -> e.salary()).reversed() się nie skompiluje — kompilator nie zna typu e
        //   w lambdzie przed reversed(). Użyj referencji (Employee::salary) albo Comparator.comparing(...,
        //   Comparator.reverseOrder()), albo jawnego typu: (Employee e) -> e.salary().
        // DOBRA PRAKTYKA: zamiast get + if + put użyj merge / computeIfAbsent / getOrDefault — krócej i mniej
        //   miejsc na błąd. Mapy w szczegółach: t12_collections/Collections05Maps.
    }

    // =================================================================================================
    // 8. STRING.JOIN I BASE64
    // =================================================================================================

    /**
     * 8. Drobne, ale codzienne dodatki: String.join, StringJoiner i Base64 (kodowanie, NIE szyfrowanie).
     */
    static void joinAndBase64() {
        section("8. String.join i Base64");

        // PRZED: ręczne sklejanie z przecinkiem — trzeba pilnować ostatniego elementu.
        List<String> skills = List.of("Java", "SQL", "Docker");          // skills = umiejętności
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < skills.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(skills.get(i));
        }
        show("PRZED (StringBuilder)", builder);
        // WYNIK: PRZED (StringBuilder) → Java, SQL, Docker

        // PO: String.join (join = połącz) i StringJoiner z nawiasami.
        show("String.join", String.join(", ", skills));
        // WYNIK: String.join → Java, SQL, Docker
        StringJoiner joiner = new StringJoiner(", ", "[", "]");          // delimiter, prefix, suffix
        skills.forEach(joiner::add);
        show("StringJoiner", joiner);
        // WYNIK: StringJoiner → [Java, SQL, Docker]
        show("Collectors.joining", skills.stream().collect(Collectors.joining(" | ")));
        // WYNIK: Collectors.joining → Java | SQL | Docker

        // Base64: zamienia bajty na bezpieczne znaki ASCII (np. do wysłania w JSON-ie albo nagłówku).
        String secret = "Cześć, świecie!";                                // secret = sekret
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);          // zawsze podawaj kodowanie jawnie
        String encoded = Base64.getEncoder().encodeToString(bytes);      // encoder = koder
        show("zakodowane Base64", encoded);
        // WYNIK: zakodowane Base64 → Q3plxZvEhywgxZt3aWVjaWUh
        String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        show("odkodowane", decoded);
        // WYNIK: odkodowane → Cześć, świecie!

        // PUŁAPKA: Base64 to KODOWANIE, nie szyfrowanie. Każdy odkoduje je jednym wywołaniem, bez klucza.
        //   Nie chroni haseł ani danych osobowych.
        // DOBRA PRAKTYKA: dla adresów URL użyj Base64.getUrlEncoder() (zamienia + i / na - i _), a bajty
        //   zamieniaj na tekst zawsze z jawnym kodowaniem (StandardCharsets.UTF_8) — domyślne bywa różne w różnych systemach
        //   (od Javy 18 domyślne jest UTF-8 — zobacz Modern07WhatsNextJava21).
    }

    // =================================================================================================
    // 9. EFEKTYWNIE FINALNA
    // =================================================================================================

    /**
     * 9. Lambda może czytać zmienne lokalne z otaczającej metody, ale tylko takie, których wartość
     * nigdy się nie zmienia po pierwszym przypisaniu („efektywnie finalne”, nie trzeba pisać final).
     */
    static void effectivelyFinal() {
        section("9. Efektywnie finalna");

        int base = 10;                                    // base = podstawa; nigdy nie zmieniana → efektywnie finalna
        Function<Integer, Integer> addBase = x -> x + base;
        show("lambda widzi base", addBase.apply(5));
        // WYNIK: lambda widzi base → 15

        // Dlaczego takie ograniczenie? Lambda dostaje KOPIĘ wartości. Zmienna lokalna żyje na stosie metody
        // i może zniknąć, zanim lambda się wykona (np. w innym wątku). Gdyby wolno było ją zmieniać,
        // kopia i oryginał rozjechałyby się, a kod byłby nieprzewidywalny.
        // BŁĄD KOMPILACJI (zakomentowany): int counter = 0; list.forEach(x -> counter++);
        //   komunikat: zmienna użyta w lambdzie musi być finalna lub efektywnie finalna.

        // Obejście 1 (poprawne): licznik atomowy — to obiekt, a zmienia się jego WNĘTRZE, nie referencja.
        AtomicInteger counter = new AtomicInteger();     // counter = licznik
        List.of("a", "b", "c").forEach(x -> counter.incrementAndGet());
        show("AtomicInteger", counter.get());
        // WYNIK: AtomicInteger → 3

        // Obejście 2 (lepsze): nie mutuj w ogóle — zapytaj strumień o wynik.
        long count = Stream.of("a", "b", "c").count();
        show("bez zmiennej pomocniczej", count);
        // WYNIK: bez zmiennej pomocniczej → 3

        // PUŁAPKA: tablica jednoelementowa (int[] box = {0}; ... box[0]++) też „działa”, ale to ukryty zmienny stan
        //   i wyścig danych w strumieniach równoległych. Traktuj to jako zapach kodu.
        // DOBRA PRAKTYKA: jeśli czujesz potrzebę zmiany zmiennej z lambdy, prawie zawsze brakuje Ci
        //   operacji strumienia (count, sum, reduce, collect). Zobacz t16_streams.
    }

    // =================================================================================================
    // 10. COMPLETABLEFUTURE
    // =================================================================================================

    /**
     * 10. CompletableFuture = „obietnica wyniku”, którą można łączyć w łańcuchy bez blokowania wątku.
     * Stary Future miał tylko get() — czekanie. Tu opisujemy kroki: „gdy będzie gotowe, przekształć”.
     * Zadania asynchroniczne (supplyAsync) lecą na wspólnej puli wątków (ForkJoinPool.commonPool).
     */
    static void completableFutureIntro() {
        section("10. CompletableFuture");

        // supplyAsync = dostarcz asynchronicznie; thenApply = potem przekształć; thenCombine = połącz z drugim;
        // join = poczekaj i pobierz wynik.
        CompletableFuture<Integer> six = CompletableFuture.supplyAsync(() -> 6);
        CompletableFuture<Integer> eight = CompletableFuture.supplyAsync(() -> 8);
        int result = six.thenApply(x -> x * 7).thenCombine(eight, Integer::sum).join();   // 6*7 + 8
        show("6*7 + 8", result);
        // WYNIK: 6*7 + 8 → 50

        // exceptionally = w razie wyjątku: zastępuje wynik (tu wartością -1)
        CompletableFuture<Integer> broken = CompletableFuture.supplyAsync(() -> {   // broken = zepsute
            if (true) {
                throw new IllegalStateException("boom");
            }
            return 1;
        });
        show("exceptionally", broken.exceptionally(ex -> -1).join());
        // WYNIK: exceptionally → -1

        // PUŁAPKA: join() / get() bez obsługi błędu opakowują wyjątek: dostajesz CompletionException,
        //   a prawdziwa przyczyna jest w getCause().
        expectThrows("join na zepsutym", () -> broken.join());
        // WYNIK: ✔ join na zepsutym → rzucono CompletionException: java.lang.IllegalStateException: boom
        try {
            broken.join();
        } catch (CompletionException e) {                                 // completion = ukończenie
            show("prawdziwa przyczyna", e.getCause().getMessage());
            // WYNIK: prawdziwa przyczyna → boom
        }

        // DOBRA PRAKTYKA: nie wołaj join()/get() w środku łańcucha (to znowu blokowanie) — łącz kroki przez
        //   thenApply / thenCompose / thenCombine, a czekaj dopiero na samym końcu. Pule wątków i
        //   CompletableFuture w głąb: t21_concurrency/Concurrency05CompletableFuture.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Interfejs funkcyjny = jedna metoda abstrakcyjna; lambda to jego krótka implementacja.
     *   • this w lambdzie = obiekt otaczający; w klasie anonimowej = ona sama.
     *   • Referencje: Klasa::statyczna, obiekt::metoda, Klasa::instancyjna, Klasa::new.
     *   • Konflikt metod default: klasa > bardziej szczegółowy interfejs > ręczne Interfejs.super.metoda().
     *   • Strumień: leniwy do operacji końcowej i jednorazowy; potok bez efektów ubocznych.
     *   • Optional jako wynik metody; orElse liczy zawsze, orElseGet leniwie; get() bez sprawdzenia = ryzyko.
     *   • java.time: niezmienne, miesiące 1–12; Date i Calendar są zmienne i mylące.
     *   • merge / computeIfAbsent / getOrDefault / removeIf / replaceAll zastępują ręczne wzorce.
     *   • Base64 to kodowanie, nie szyfrowanie. Zmienna w lambdzie musi być efektywnie finalna.
     *   • CompletableFuture: łącz kroki bez blokowania; join opakowuje wyjątek w CompletionException.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym jest interfejs funkcyjny i po co adnotacja @FunctionalInterface?
     *   2. Wymień cztery rodzaje referencji do metod i podaj po jednym przykładzie.
     *   3. Klasa C implementuje interfejsy A i B, oba mają metodę default hello(). Co się stanie
     *      i jak to naprawić? A jeśli klasa C dziedziczy też po klasie z metodą hello()?
     *   4. Co wypisze:  Optional<String> o = Optional.empty();  System.out.println(o.map(String::length).orElse(-1));  ?
     *   5. ZNAJDŹ BŁĄD:  int sum = 0;  List.of(1, 2, 3).forEach(x -> sum += x);
     *   6. Co wypisze:  Map<String,Integer> m = new TreeMap<>(Map.of("a", 1));  m.merge("a", 4, Integer::sum);
     *      System.out.println(m);  ?
     *   7. Dlaczego orElse(drogieObliczenie()) bywa błędem, a orElseGet(() -> drogieObliczenie()) nie?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: sortowanie po długości, potem alfabetycznie",
                List.of("Ala", "Ola", "Adam", "Zofia"), () -> exercise1(List.of("Zofia", "Ola", "Adam", "Ala")));
        Check.equal("ćw. 2: zliczanie liter",
                Map.of('a', 3, 'l', 1, 'm', 1), () -> exercise2("malaa"));
        Check.equal("ćw. 3: domena z e-maila (Optional)",
                List.of(Optional.of("example.com"), Optional.empty()),
                () -> List.of(exercise3("jan@example.com"), exercise3(null)));
        Check.equal("ćw. 4: potok funkcji", 10, () -> exercise4(List.of(x -> x + 1, x -> x * 2, x -> x + 4)).apply(2));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)",
                List.of("Ala", "Ola", "Adam", "Zofia"), () -> solution1(List.of("Zofia", "Ola", "Adam", "Ala")));
        Check.equal("ćw. 2 (wzorzec)", Map.of('a', 3, 'l', 1, 'm', 1), () -> solution2("malaa"));
        Check.equal("ćw. 3 (wzorzec)",
                List.of(Optional.of("example.com"), Optional.empty()),
                () -> List.of(solution3("jan@example.com"), solution3(null)));
        Check.equal("ćw. 4 (wzorzec)", 10, () -> solution4(List.of(x -> x + 1, x -> x * 2, x -> x + 4)).apply(2));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): PRZEPISZ na lambdę i łańcuch komparatorów. Zwróć NOWĄ listę posortowaną
     * najpierw po długości, a przy równej długości alfabetycznie.
     * <pre>{@code
     * // PRZED:
     * Collections.sort(copy, new Comparator<String>() {
     *     public int compare(String a, String b) {
     *         int byLength = Integer.compare(a.length(), b.length());
     *         return byLength != 0 ? byLength : a.compareTo(b);
     *     }
     * });
     * }</pre>
     * Podpowiedź: Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder()).
     */
    static List<String> exercise1(List<String> words) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (łatwe): PRZEPISZ zliczanie z containsKey/put na jedno wywołanie merge. Zwróć mapę
     * „litera → ile razy występuje” w podanym tekście.
     * Podpowiedź: text.toCharArray() daje znaki; merge(litera, 1, Integer::sum).
     */
    static Map<Character, Integer> exercise2(String text) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć domenę (część po @) z adresu e-mail albo pusty Optional, gdy adres to null.
     * Podpowiedź: Optional.ofNullable(email).map(...) i substring(indexOf('@') + 1).
     */
    static Optional<String> exercise3(String email) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): z listy funkcji zbuduj jedną funkcję, która stosuje je po kolei
     * (pierwsza z listy działa pierwsza). Dla pustej listy ma zwracać argument bez zmian.
     * Podpowiedź: reduce(Function.identity(), Function::andThen) — andThen = a potem.
     */
    static Function<Integer, Integer> exercise4(List<Function<Integer, Integer>> steps) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solution1(List<String> words) {
        List<String> copy = new ArrayList<>(words);
        copy.sort(Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder()));
        return copy;
    }

    static Map<Character, Integer> solution2(String text) {
        Map<Character, Integer> counts = new TreeMap<>();
        for (char c : text.toCharArray()) {
            counts.merge(c, 1, Integer::sum);
        }
        return counts;
    }

    static Optional<String> solution3(String email) {
        return Optional.ofNullable(email).map(e -> e.substring(e.indexOf('@') + 1));
    }

    static Function<Integer, Integer> solution4(List<Function<Integer, Integer>> steps) {
        return steps.stream().reduce(Function.identity(), Function::andThen);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Interfejs z dokładnie jedną metodą abstrakcyjną (default i static się nie liczą). Adnotacja jest
     *      zabezpieczeniem: kompilator zgłosi błąd, gdy ktoś doda drugą metodę abstrakcyjną i zepsuje lambdy.
     *   2. Statyczna Integer::parseInt; związana prefix::concat; niezwiązana String::toUpperCase;
     *      konstruktor ArrayList::new.
     *   3. Błąd kompilacji: klasa dziedziczy dwie niezwiązane metody default. Naprawa: nadpisz hello() w C
     *      i wybierz A.super.hello() albo B.super.hello(). Gdy C dziedziczy hello() z klasy, wygrywa klasa
     *      (reguła „klasa wygrywa”) i nie ma konfliktu.
     *   4. -1 (pusty Optional: map nic nie robi, orElse zwraca -1).
     *   5. sum nie jest efektywnie finalna (zmieniamy ją w lambdzie) — błąd kompilacji. Poprawka:
     *      int sum = List.of(1, 2, 3).stream().mapToInt(Integer::intValue).sum();
     *   6. {a=5} (merge dla istniejącego klucza woła Integer::sum(1, 4)).
     *   7. orElse przyjmuje już obliczoną wartość, więc drogie obliczenie wykona się ZAWSZE, nawet gdy Optional
     *      ma wartość. orElseGet dostaje Supplier, który jest wołany tylko dla pustego Optional.
     */
    // </editor-fold>
}
