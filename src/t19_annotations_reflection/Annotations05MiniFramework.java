package t19_annotations_reflection;

import helpers.Check;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Mini framework — router komend oparty na adnotacjach (Spring MVC w miniaturze)
 *        (router = rozdzielacz; command = komenda; dispatch = przekaż do obsługi; handler = obiekt obsługi)
 *
 * W SKRÓCIE:
 *   Oznaczasz zwykłe metody adnotacją {@code @Command(name = "add", help = "...")}. Router przy starcie przegląda
 *   refleksją obiekt z komendami i buduje rejestr nazwa → metoda (TreeMap). Potem dispatch("add 2 3") znajduje
 *   metodę, zamienia napisy "2" i "3" na typy parametrów (int) i wywołuje ją przez Method.invoke.
 *   Dokładnie tak Spring MVC obsługuje {@code @GetMapping("/add")} — tylko zamiast linii tekstu przychodzi żądanie HTTP.
 *
 * ANALOGIA: recepcja w hotelu.
 *   Pracownicy noszą identyfikatory „bagaż”, „sprzątanie”, „taxi” (adnotacje). Recepcjonista (router) rano spisuje
 *   identyfikatory do zeszytu (rejestr). Gość mówi „taxi na 8:00” — recepcjonista sprawdza w zeszycie, kto to robi,
 *   i przekazuje sprawę. Nowy pracownik = nowy identyfikator; recepcji nikt nie przebudowuje.
 *
 * JAK TO DZIAŁA:
 *   START:     dla każdej metody obiektu: jest @Command? → commands.put(name, metoda)   (TreeMap = alfabetycznie)
 *   DISPATCH:  "add 2 3" → split → nazwa "add", argumenty ["2", "3"]
 *              → metoda = commands.get("add") → typy parametrów [int, int]
 *              → konwersja: "2" → 2, "3" → 3 → method.invoke(obiekt, 2, 3) → 5 → "5"
 *   BŁĘDY:     nieznana komenda / zła liczba argumentów / zły format / wyjątek w komendzie → czytelny komunikat
 *
 * SŁÓWKA:
 *   command = komenda; router = rozdzielacz (kieruje do właściwej metody); registry = rejestr; dispatch = przekaż;
 *   handler = obiekt obsługi; help = pomoc; parameter = parametr; convert = przekształć (konwertuj);
 *   argument = argument; unknown = nieznany; mapping = przypisanie (adresu do metody); controller = kontroler.
 *
 * ZOBACZ TEŻ: t19_annotations_reflection/Annotations03ReflectionBasics (Method.invoke),
 *             t22_design_patterns/Patterns09Command (wzorzec Polecenie), t12_collections/Collections05Maps (TreeMap),
 *             t19_annotations_reflection/Annotations06DynamicProxy (kolejny klocek frameworków).
 * </pre>
 */
public class Annotations05MiniFramework {

    // ---------------------------------------------------------------------------------------------
    // Adnotacja i obiekty z komendami
    // ---------------------------------------------------------------------------------------------

    /** Command = komenda. RUNTIME, bo router czyta ją refleksją; METHOD, bo oznaczamy metody. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Command {
        String name();

        String help() default "";
    }

    /** CalculatorCommands = komendy kalkulatora. Zwykła klasa — nic nie wie o routerze. */
    static final class CalculatorCommands {
        @Command(name = "add", help = "dodaje dwie liczby całkowite")
        public int add(int a, int b) {
            return a + b;
        }

        @Command(name = "avg", help = "średnia dwóch liczb")
        public double avg(double a, double b) {
            return (a + b) / 2;
        }

        @Command(name = "div", help = "dzielenie całkowite")
        public int div(int a, int b) {
            return a / b;
        }

        @Command(name = "greet", help = "powitanie")
        public String greet(String name) {
            return "Cześć, " + name + "!";
        }

        @Command(name = "repeat", help = "powtarza tekst n razy")
        public String repeat(String text, int times) {
            return text.repeat(times);                     // repeat (Java 11+)
        }

        @Command(name = "ping", help = "sprawdza, czy router żyje")
        public void ping() {
        }

        public String helper() {                           // BEZ @Command → router jej nie widzi
            return "nie jestem komendą";
        }
    }

    /** TextCommands = druga paczka komend — z KONFLIKTEM nazwy greet. */
    static final class TextCommands {
        @Command(name = "greet", help = "powitanie po angielsku")
        public String greet(String name) {
            return "Hello, " + name;
        }
    }

    /** DateCommands = komenda z typem parametru, którego router nie umie przekształcić. */
    static final class DateCommands {
        @Command(name = "days", help = "dni do daty")
        public long days(LocalDate date) {
            return date.toEpochDay();
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Router
    // ---------------------------------------------------------------------------------------------

    static final Set<Class<?>> SUPPORTED = Set.of(String.class, int.class, Integer.class, long.class, Long.class,
            double.class, Double.class, boolean.class, Boolean.class);

    /** convert = przekształć napis na typ parametru metody. */
    static Object convert(String text, Class<?> type) {
        if (type == String.class) {
            return text;
        } else if (type == int.class || type == Integer.class) {
            return Integer.parseInt(text);                 // NumberFormatException dla "dwa"
        } else if (type == long.class || type == Long.class) {
            return Long.parseLong(text);
        } else if (type == double.class || type == Double.class) {
            return Double.parseDouble(text);               // zawsze z KROPKĄ, niezależnie od Locale
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(text);             // UWAGA: wszystko poza "true" daje false
        }
        throw new IllegalArgumentException("Nieobsługiwany typ parametru: " + type.getSimpleName());
    }

    /** CommandRouter = router komend: rejestr (TreeMap) + dispatch. */
    static final class CommandRouter {
        /** Handler = obsługa: obiekt, metoda i opis komendy. */
        record Handler(Object target, Method method, String help) {
        }

        private final Map<String, Handler> commands = new TreeMap<>();

        /** register = zarejestruj wszystkie metody z @Command. Błędy konfiguracji wychodzą OD RAZU (przy starcie). */
        CommandRouter register(Object handlerObject) {
            for (Method method : handlerObject.getClass().getDeclaredMethods()) {
                Command command = method.getAnnotation(Command.class);
                if (command == null) {
                    continue;
                }
                if (commands.containsKey(command.name())) {
                    throw new IllegalStateException("Komenda „" + command.name() + "” jest już zarejestrowana");
                }
                for (Class<?> type : method.getParameterTypes()) {
                    if (!SUPPORTED.contains(type)) {
                        throw new IllegalStateException("Komenda „" + command.name() + "”: nieobsługiwany typ parametru "
                                + type.getSimpleName());
                    }
                }
                commands.put(command.name(), new Handler(handlerObject, method, command.help()));
            }
            return this;
        }

        List<String> names() {
            return List.copyOf(commands.keySet());         // TreeMap → klucze już posortowane
        }

        /** dispatch = przekaż linię do właściwej metody i zwróć wynik jako tekst. */
        String dispatch(String line) {
            String[] parts = line.strip().split("\\s+");
            String name = parts[0];
            if (name.equals("help")) {
                return help();
            }
            Handler handler = commands.get(name);
            if (handler == null) {
                return "Nieznana komenda: " + name + " (wpisz help)";
            }
            Class<?>[] types = handler.method().getParameterTypes();
            if (parts.length - 1 != types.length) {
                return "Komenda " + name + " oczekuje " + types.length + " argumentów, podano " + (parts.length - 1);
            }
            Object[] args = new Object[types.length];
            for (int i = 0; i < types.length; i++) {
                try {
                    args[i] = convert(parts[i + 1], types[i]);
                } catch (IllegalArgumentException e) {     // NumberFormatException też tu trafi (to podklasa)
                    return "Zły argument „" + parts[i + 1] + "” — oczekiwano typu " + types[i].getSimpleName();
                }
            }
            try {
                Object result = handler.method().invoke(handler.target(), args);
                return handler.method().getReturnType() == void.class ? "OK" : String.valueOf(result);
            } catch (InvocationTargetException e) {        // komenda sama rzuciła wyjątek → rozpakuj
                Throwable cause = e.getCause();
                return "Błąd w komendzie " + name + ": " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Brak dostępu do metody komendy " + name, e);
            }
        }

        /** help = pomoc: lista komend (alfabetycznie, bo TreeMap) z typami parametrów i opisem. */
        String help() {
            return commands.entrySet().stream()
                    .map(e -> String.format(Locale.ROOT, "  %-7s %-13s %s", e.getKey(),
                            Arrays.stream(e.getValue().method().getParameterTypes())
                                    .map(Class::getSimpleName).collect(Collectors.joining(" ")),
                            e.getValue().help()))
                    .collect(Collectors.joining("\n", "Dostępne komendy:\n", ""));
        }
    }

    public static void main(String[] args) throws NoSuchMethodException {
        title("Annotations05 — mini framework: router komend");

        beforeSwitch();          // before: switch = PRZED: switch
        commandAnnotation();     // command annotation = adnotacja komendy
        registry();              // registry = rejestr
        dispatching();           // dispatching = przekazywanie do obsługi
        helpAndUnknown();        // help and unknown = pomoc i nieznane komendy
        parameterConversion();   // parameter conversion = konwersja parametrów
        errorsInCommands();      // errors in commands = błędy w komendach
        springAnalogy();         // Spring analogy = analogia do Springa
        exercises();             // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PRZED: WIELKI SWITCH
    // =================================================================================================

    /** dispatchWithSwitch = stary sposób: każda komenda to ręcznie dopisany case z ręcznym parsowaniem. */
    static String dispatchWithSwitch(String line) {
        String[] parts = line.split(" ");
        CalculatorCommands calc = new CalculatorCommands();
        switch (parts[0]) {
            case "add":
                return String.valueOf(calc.add(Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
            case "greet":
                return calc.greet(parts[1]);
            default:
                return "Nieznana komenda: " + parts[0];
        }
    }

    /** 1. PRZED: działa, ale każda nowa komenda = edycja switcha w routerze, który powinien być „gotowy”. */
    static void beforeSwitch() {
        section("1. PRZED: wielki switch");

        show("switch: add 2 3", dispatchWithSwitch("add 2 3"));
        show("switch: avg 2 3", dispatchWithSwitch("avg 2 3"));
        // WYNIK: switch: add 2 3 → 5
        // WYNIK: switch: avg 2 3 → Nieznana komenda: avg    ← metoda avg istnieje, ale nikt nie dopisał case

        // PUŁAPKA: switch rośnie z każdą komendą, parsowanie argumentów jest kopiowane w każdym case, a zapomniany
        //   case to komenda, która „istnieje i nie działa”. Router z adnotacjami odkrywa komendy SAM.
    }

    // =================================================================================================
    // 2. ADNOTACJA @Command
    // =================================================================================================

    /** 2. Refleksja znajduje metody z @Command. Sortujemy po nazwie komendy (kolejność metod niegwarantowana). */
    static void commandAnnotation() {
        section("2. Adnotacja @Command na metodach");

        List<String> found = Arrays.stream(CalculatorCommands.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Command.class))
                .sorted(Comparator.comparing(m -> m.getAnnotation(Command.class).name()))
                .map(m -> m.getAnnotation(Command.class).name() + " → metoda " + m.getName()
                        + "(" + m.getParameterCount() + " param.)")
                .toList();
        showEach("metody z @Command", found);
        // WYNIK: metody z @Command (liczba elementów: 6):
        // WYNIK:    • add → metoda add(2 param.)
        // WYNIK:    • avg → metoda avg(2 param.)
        // WYNIK:    • div → metoda div(2 param.)
        // WYNIK:    • greet → metoda greet(1 param.)
        // WYNIK:    • ping → metoda ping(0 param.)
        // WYNIK:    • repeat → metoda repeat(2 param.)

        // helper() nie ma adnotacji, więc go tu nie ma. Nazwa komendy nie musi być nazwą metody — o adresie decyduje
        // adnotacja, a nie Java. Tak samo w Springu: @GetMapping("/sum") może stać nad metodą o nazwie add.
    }

    // =================================================================================================
    // 3. REJESTR
    // =================================================================================================

    /** 3. register: refleksja raz, przy starcie. Potem tylko szybkie commands.get(nazwa). */
    static void registry() {
        section("3. Rejestr komend (TreeMap) i błędy konfiguracji");

        CommandRouter router = new CommandRouter().register(new CalculatorCommands());
        show("zarejestrowane", router.names());
        // WYNIK: zarejestrowane → [add, avg, div, greet, ping, repeat]

        expectThrows("dwie komendy greet", () -> new CommandRouter()
                .register(new CalculatorCommands()).register(new TextCommands()));
        expectThrows("parametr LocalDate", () -> new CommandRouter().register(new DateCommands()));
        // WYNIK: ✔ dwie komendy greet → rzucono IllegalStateException: Komenda „greet” jest już zarejestrowana
        // WYNIK: ✔ parametr LocalDate → rzucono IllegalStateException: Komenda „days”: nieobsługiwany typ parametru LocalDate

        // DOBRA PRAKTYKA: błędy KONFIGURACJI zgłaszaj przy starcie (fail fast = zawiedź szybko). Spring robi tak samo:
        //   dwa kontrolery z tym samym adresem = aplikacja w ogóle nie wstaje, zamiast losowo działać na produkcji.
        // DOBRA PRAKTYKA: refleksję wykonuj RAZ (rejestr), a nie przy każdym wywołaniu — szukanie metod po nazwie
        //   za każdym razem byłoby wolne.
    }

    // =================================================================================================
    // 4. DISPATCH
    // =================================================================================================

    /** 4. dispatch: nazwa → metoda z rejestru → konwersja argumentów → invoke → wynik jako tekst. */
    static void dispatching() {
        section("4. dispatch(\"add 2 3\") — wywołanie komendy");

        CommandRouter router = new CommandRouter().register(new CalculatorCommands());
        for (String line : List.of("add 2 3", "avg 2 3", "greet Ala", "repeat ha 3", "ping", "  add   10   32  ")) {
            show("„" + line + "”", router.dispatch(line));
        }
        // WYNIK: „add 2 3” → 5
        // WYNIK: „avg 2 3” → 2.5
        // WYNIK: „greet Ala” → Cześć, Ala!
        // WYNIK: „repeat ha 3” → hahaha
        // WYNIK: „ping” → OK    ← metoda void: router sam zwraca „OK”
        // WYNIK: „  add   10   32  ” → 42    ← strip + split("\\s+") radzą sobie z nadmiarem spacji

        // Method.invoke dostaje Object[] {2, 3} (Integer), a metoda chce int — invoke sam rozpakowuje (unboxing).
    }

    // =================================================================================================
    // 5. HELP I NIEZNANE KOMENDY
    // =================================================================================================

    /** 5. help generuje się z adnotacji — dokumentacja nie rozjedzie się z kodem. */
    static void helpAndUnknown() {
        section("5. help, nieznana komenda, zła liczba argumentów");

        CommandRouter router = new CommandRouter().register(new CalculatorCommands());
        System.out.println(router.dispatch("help"));
        // WYNIK: Dostępne komendy:
        // WYNIK:   add     int int       dodaje dwie liczby całkowite
        // WYNIK:   avg     double double średnia dwóch liczb
        // WYNIK:   div     int int       dzielenie całkowite
        // WYNIK:   greet   String        powitanie
        // WYNIK:   ping                  sprawdza, czy router żyje
        // WYNIK:   repeat  String int    powtarza tekst n razy

        show("„pow 2 3”", router.dispatch("pow 2 3"));
        show("„add 2”", router.dispatch("add 2"));
        // WYNIK: „pow 2 3” → Nieznana komenda: pow (wpisz help)
        // WYNIK: „add 2” → Komenda add oczekuje 2 argumentów, podano 1

        // PUŁAPKA: bez sprawdzenia liczby argumentów invoke rzuciłby IllegalArgumentException („wrong number of
        //   arguments”) — komunikat dla programisty, a nie dla użytkownika. Waliduj wejście PRZED invoke.
    }

    // =================================================================================================
    // 6. KONWERSJA PARAMETRÓW
    // =================================================================================================

    /** 6. Typ docelowy bierzemy z getParameterTypes(). Każdy napis z wejścia trzeba zamienić na ten typ. */
    static void parameterConversion() {
        section("6. Konwersja parametrów: String → int/double/boolean");

        show("convert(\"42\", int.class)", convert("42", int.class));
        show("convert(\"2.5\", double.class)", convert("2.5", double.class));
        show("convert(\"tak\", boolean.class)", convert("tak", boolean.class));
        // WYNIK: convert("42", int.class) → 42
        // WYNIK: convert("2.5", double.class) → 2.5
        // WYNIK: convert("tak", boolean.class) → false    ← parseBoolean: wszystko poza „true” to false!

        expectThrows("convert(\"2,5\", double.class)", () -> convert("2,5", double.class));
        // WYNIK: ✔ convert("2,5", double.class) → rzucono NumberFormatException: For input string: "2,5"

        CommandRouter router = new CommandRouter().register(new CalculatorCommands());
        show("„add dwa 3”", router.dispatch("add dwa 3"));
        // WYNIK: „add dwa 3” → Zły argument „dwa” — oczekiwano typu int

        // PUŁAPKA: Double.parseDouble nie zna polskiego przecinka ("2,5") — zawsze wymaga kropki. Do liczb wpisanych
        //   „po polsku” użyj NumberFormat z Locale.forLanguageTag("pl-PL") (t15_numbers).
        // PUŁAPKA: Boolean.parseBoolean("tak") po cichu daje false. Ścisła konwersja (ćwiczenie 2) powinna przyjąć
        //   tylko "true"/"false" i rzucić wyjątek dla reszty — cichy false to błąd, którego nikt nie zauważy.
    }

    // =================================================================================================
    // 7. BŁĘDY WEWNĄTRZ KOMEND
    // =================================================================================================

    /** 7. Wyjątek rzucony przez komendę przychodzi jako InvocationTargetException — router go rozpakowuje. */
    static void errorsInCommands() {
        section("7. Wyjątek w komendzie — rozpakowanie InvocationTargetException");

        CommandRouter router = new CommandRouter().register(new CalculatorCommands());
        show("„div 7 2”", router.dispatch("div 7 2"));
        show("„div 1 0”", router.dispatch("div 1 0"));
        // WYNIK: „div 7 2” → 3
        // WYNIK: „div 1 0” → Błąd w komendzie div: ArithmeticException: / by zero

        // Bez rozpakowania użytkownik zobaczyłby „InvocationTargetException: null” — nic nie mówiące opakowanie.
        // DOBRA PRAKTYKA: framework oddziela błędy UŻYTKOWNIKA (zła komenda, zły argument → czytelny komunikat) od
        //   błędów KOMENDY (wyjątek w metodzie → rozpakowana przyczyna) i od błędów KONFIGURACJI (przy starcie).
    }

    // =================================================================================================
    // 8. TO JEST SPRING MVC W MINIATURZE
    // =================================================================================================

    /** 8. Każdy element naszego routera ma odpowiednik w Spring MVC. Plus pułapka z nazwami parametrów. */
    static void springAnalogy() throws NoSuchMethodException {
        section("8. Analogia: Spring MVC (@GetMapping)");

        // NASZ ROUTER                       SPRING MVC
        // @Command(name = "add")            @GetMapping("/add")
        // CalculatorCommands                klasa z @RestController (Spring tworzy ją sam)
        // register(obiekt) przy starcie     skanowanie klas i budowa mapy adres → metoda przy starcie
        // dispatch("add 2 3")               DispatcherServlet obsługuje GET /add?a=2&b=3
        // convert("2", int.class)           konwersja @RequestParam / @PathVariable na typ parametru
        // „Nieznana komenda”                404 Not Found
        // „Zły argument”                    400 Bad Request
        // rozpakowany wyjątek komendy       @ExceptionHandler / 500 Internal Server Error
        // help()                            lista adresów (np. dokumentacja OpenAPI/Swagger)
        //
        //   @RestController
        //   class CalculatorController {
        //       @GetMapping("/add")
        //       int add(@RequestParam int a, @RequestParam int b) { return a + b; }
        //   }

        Method add = CalculatorCommands.class.getMethod("add", int.class, int.class);
        List<String> parameterNames = Arrays.stream(add.getParameters()).map(Parameter::getName).toList();
        show("nazwy parametrów add", parameterNames);
        show("isNamePresent()", add.getParameters()[0].isNamePresent());   // czy nazwa zapisana w .class
        // WYNIK: nazwy parametrów add → [arg0, arg1]    ← bez opcji -parameters nazwy a, b nie trafiają do .class
        // WYNIK: isNamePresent() → false

        // PUŁAPKA: Spring dopasowuje ?a=2 do parametru int a po NAZWIE parametru. Bez opcji kompilatora -parameters
        //   refleksja widzi tylko arg0, arg1. Dlatego projekty Spring Boot kompilują z -parameters (ustawia to
        //   rodzicielski POM Spring Boot), a w razie wątpliwości piszesz nazwę wprost: @RequestParam("a").
        //   (Ten wynik zależy od opcji kompilacji: z -parameters zobaczysz [a, b] i true.)
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @Command: @Retention(RUNTIME) + @Target(METHOD); elementy name i help.
     *   • register: getDeclaredMethods → getAnnotation(Command) → TreeMap nazwa → (obiekt, metoda). Raz, przy starcie.
     *   • Fail fast: konflikt nazw i nieobsługiwane typy parametrów → wyjątek przy rejestracji.
     *   • dispatch: strip + split("\\s+") → commands.get → sprawdź liczbę argumentów → convert → invoke.
     *   • Konwersja wg getParameterTypes(); uwaga na parseBoolean ("tak" → false) i parseDouble ("2,5" → wyjątek).
     *   • InvocationTargetException → getCause() → czytelny komunikat. Metoda void → „OK”.
     *   • To jest Spring MVC w miniaturze: @GetMapping, DispatcherServlet, konwersja @RequestParam, 404/400/500.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego adnotacja @Command musi mieć @Retention(RUNTIME) i @Target(METHOD)?
     *   2. Co wypisze:  router.dispatch("avg 1 2")  ?
     *   3. ZNAJDŹ BŁĄD:  router szuka metody refleksją (getDeclaredMethods + pętla) przy KAŻDYM dispatch.
     *   4. Co wypisze:  System.out.println(Boolean.parseBoolean("TRUE") + " " + Boolean.parseBoolean("tak"));  ?
     *   5. ZNAJDŹ BŁĄD:  catch (InvocationTargetException e) { return "Błąd: " + e.getMessage(); }
     *   6. Dlaczego konflikt nazw komend lepiej wykryć w register niż w dispatch?
     *   7. Skąd Spring wie, że ?a=2 trafia do parametru int a i co ma z tym wspólnego opcja -parameters?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        List<String> helpLines = List.of("add/2: dodaje dwie liczby całkowite", "avg/2: średnia dwóch liczb",
                "div/2: dzielenie całkowite", "greet/1: powitanie", "ping/0: sprawdza, czy router żyje",
                "repeat/2: powtarza tekst n razy");
        CalculatorCommands calc = new CalculatorCommands();

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: nazwy komend", List.of("add", "avg", "div", "greet", "ping", "repeat"),
                () -> exercise1(CalculatorCommands.class));
        Check.equal("ćw. 2a: int", 42, () -> exercise2("42", int.class));
        Check.equal("ćw. 2b: boolean true", true, () -> exercise2("true", boolean.class));
        Check.throwsException("ćw. 2c: boolean „tak”", IllegalArgumentException.class, () -> exercise2("tak", boolean.class));
        Check.equal("ćw. 3: opis komend", helpLines, () -> exercise3(CalculatorCommands.class));
        Check.equal("ćw. 4a: add 20 22", "42", () -> exercise4(calc, "add 20 22"));
        Check.equal("ćw. 4b: div 7 0", "Błąd w komendzie div: ArithmeticException: / by zero", () -> exercise4(calc, "div 7 0"));
        Check.equal("ćw. 4c: pow 2 3", "Nieznana komenda: pow (wpisz help)", () -> exercise4(calc, "pow 2 3"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of("add", "avg", "div", "greet", "ping", "repeat"),
                () -> solution1(CalculatorCommands.class));
        Check.equal("ćw. 2a (wzorzec)", 42, () -> solution2("42", int.class));
        Check.equal("ćw. 2b (wzorzec)", true, () -> solution2("true", boolean.class));
        Check.throwsException("ćw. 2c (wzorzec)", IllegalArgumentException.class, () -> solution2("tak", boolean.class));
        Check.equal("ćw. 3 (wzorzec)", helpLines, () -> solution3(CalculatorCommands.class));
        Check.equal("ćw. 4a (wzorzec)", "42", () -> solution4(calc, "add 20 22"));
        Check.equal("ćw. 4b (wzorzec)", "Błąd w komendzie div: ArithmeticException: / by zero", () -> solution4(calc, "div 7 0"));
        Check.equal("ćw. 4c (wzorzec)", "Nieznana komenda: pow (wpisz help)", () -> solution4(calc, "pow 2 3"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 8 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć posortowane nazwy komend (element name z @Command) zadeklarowanych w klasie.
     * Podpowiedź: getDeclaredMethods, getAnnotation(Command.class) != null, map(... .name()), sorted().
     */
    static List<String> exercise1(Class<?> handlerType) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 2 (średnie): ścisła konwersja: String, int/Integer, double/Double i boolean/Boolean — ale boolean
     * tylko z "true" lub "false" (bez względu na wielkość liter); każdy inny tekst → IllegalArgumentException.
     * Podpowiedź: equalsIgnoreCase; dla nieobsługiwanego typu też IllegalArgumentException.
     */
    static Object exercise2(String text, Class<?> type) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): opis komend dla dokumentacji: "nazwa/liczbaParametrów: help", posortowane po nazwie.
     * Połącz z t16_streams: Arrays.stream(...).filter(...).map(...).sorted().toList().
     * Podpowiedź: m.getParameterCount() zwraca liczbę parametrów.
     */
    static List<String> exercise3(Class<?> handlerType) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): napisz własny dispatch BEZ CommandRouter: znajdź w handler.getClass() metodę
     * z @Command o podanej nazwie, przekształć argumenty (convert), wywołaj i zwróć String.valueOf(wynik).
     * Komunikaty jak w routerze: "Nieznana komenda: X (wpisz help)" oraz "Błąd w komendzie X: Typ: komunikat".
     * Podpowiedź: InvocationTargetException → getCause(); IllegalAccessException opakuj w IllegalStateException.
     */
    static String exercise4(Object handler, String line) {
        // TODO: twoje rozwiązanie
        return null;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solution1(Class<?> handlerType) {
        return Arrays.stream(handlerType.getDeclaredMethods())
                .map(m -> m.getAnnotation(Command.class))
                .filter(c -> c != null)
                .map(Command::name)
                .sorted()
                .toList();
    }

    static Object solution2(String text, Class<?> type) {
        if (type == boolean.class || type == Boolean.class) {
            if (text.equalsIgnoreCase("true") || text.equalsIgnoreCase("false")) {
                return Boolean.parseBoolean(text);
            }
            throw new IllegalArgumentException("„" + text + "” nie jest wartością logiczną");
        }
        return convert(text, type);
    }

    static List<String> solution3(Class<?> handlerType) {
        return Arrays.stream(handlerType.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Command.class))
                .map(m -> m.getAnnotation(Command.class).name() + "/" + m.getParameterCount() + ": "
                        + m.getAnnotation(Command.class).help())
                .sorted()
                .toList();
    }

    static String solution4(Object handler, String line) {
        String[] parts = line.strip().split("\\s+");
        for (Method method : handler.getClass().getDeclaredMethods()) {
            Command command = method.getAnnotation(Command.class);
            if (command == null || !command.name().equals(parts[0])) {
                continue;
            }
            Class<?>[] types = method.getParameterTypes();
            Object[] args = new Object[types.length];
            for (int i = 0; i < types.length; i++) {
                args[i] = convert(parts[i + 1], types[i]);
            }
            try {
                return String.valueOf(method.invoke(handler, args));
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                return "Błąd w komendzie " + parts[0] + ": " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        return "Nieznana komenda: " + parts[0] + " (wpisz help)";
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. RUNTIME — router czyta ją refleksją w czasie działania (z domyślnym CLASS getAnnotation zwróci null
     *      i rejestr będzie pusty). METHOD — kompilator nie pozwoli przykleić jej np. do pola, gdzie nic by nie robiła.
     *   2. 1.5 — oba argumenty zamienione na double, (1.0 + 2.0) / 2 = 1.5, String.valueOf daje „1.5”.
     *   3. Refleksja przy każdym wywołaniu jest wolna i powtarza tę samą pracę. Zbuduj rejestr RAZ (register)
     *      i w dispatch używaj tylko commands.get(nazwa).
     *   4. true false — parseBoolean ignoruje wielkość liter dla „true”, a każdy inny tekst daje false.
     *   5. getMessage() opakowania to null — użytkownik zobaczy „Błąd: null”. Trzeba rozpakować: e.getCause().
     *   6. Błąd konfiguracji wykryty przy starcie zatrzymuje program od razu, w przewidywalnym miejscu. Wykryty
     *      w dispatch zależałby od kolejności rejestracji i ujawniłby się dopiero u użytkownika.
     *   7. Dopasowuje nazwę z adresu do NAZWY parametru metody, którą odczytuje refleksją. Nazwy parametrów trafiają
     *      do pliku .class tylko z opcją -parameters; bez niej są arg0, arg1 — wtedy trzeba @RequestParam("a").
     */
    // </editor-fold>
}
