package t00_start;

import helpers.Check;

import java.util.ArrayList;
import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Debugowanie w IntelliJ — breakpointy, krokowanie, podgląd zmiennych, warunki, wyjątki
 *        (debugger = odpluskwiacz, narzędzie do śledzenia programu krok po kroku; breakpoint = punkt zatrzymania)
 *
 * W SKRÓCIE:
 *   Zamiast zgadywać albo wstawiać wszędzie System.out.println, uruchom program w trybie DEBUG (zielony żuczek 🐞).
 *   Program zatrzyma się na breakpoincie (czerwona kropka na marginesie), a Ty zobaczysz WSZYSTKIE zmienne w tej chwili,
 *   możesz iść linia po linii (F8), wejść do metody (F7), policzyć dowolne wyrażenie (Alt+F8) i zatrzymać się tylko
 *   wtedy, gdy spełniony jest warunek (np. i == 37). Ta lekcja zawiera programy z celowymi błędami do znalezienia.
 *
 * ANALOGIA: film puszczany klatka po klatce.
 *   Normalne uruchomienie to film w pełnej prędkości — widzisz tylko zakończenie. Debugger to pilot z pauzą,
 *   przewijaniem klatka po klatce i lupą: zatrzymujesz scenę dokładnie tam, gdzie „coś poszło nie tak”, i oglądasz szczegóły.
 *
 * JAK TO DZIAŁA:
 *   1. Kliknij na marginesie obok numeru linii → pojawi się czerwona kropka (breakpoint).
 *   2. Kliknij prawym na zielony trójkąt obok main → Debug (albo Shift+F9 dla ostatniej konfiguracji).
 *   3. Program staje na breakpoincie. Okno Debug pokazuje: Frames (stos wywołań) i Variables (zmienne).
 *   4. Klawisze: F8 Step Over (następna linia), F7 Step Into (wejdź do metody), Shift+F8 Step Out (wyjdź z metody),
 *      F9 Resume (biegnij do następnego breakpointu), Alt+F9 Run to Cursor, Alt+F8 Evaluate Expression.
 *
 * SŁÓWKA:
 *   debug = odpluskwiać (szukać błędów); breakpoint = punkt zatrzymania; step over = przejdź nad (linię);
 *   step into = wejdź do (metody); step out = wyjdź z (metody); resume = wznów; evaluate = oblicz; watch = obserwuj
 *   (podgląd wyrażenia); frame = ramka (poziom stosu wywołań); condition = warunek; suspend = wstrzymaj;
 *   off-by-one = błąd o jeden (np. {@code <=} zamiast {@code <}); run to cursor = biegnij do kursora.
 *
 * ZOBACZ TEŻ: t00_start/Start01HowToUse (uruchamianie), t05_methods/Methods01Basics (stos wywołań),
 *             t10_exceptions/Exceptions01Basics (ślad stosu), t16_streams/Streams16Laziness (Trace Current Stream Chain).
 * </pre>
 */
public class Start05Debugging {

    public static void main(String[] args) {
        title("Start05 — debugowanie w IntelliJ");

        firstBreakpoint();          // first breakpoint = pierwszy breakpoint
        stepIntoAndStack();         // step into and stack = wejście do metody i stos wywołań
        conditionalBreakpoint();    // conditional breakpoint = breakpoint warunkowy
        evaluateAndWatches();       // evaluate and watches = obliczanie wyrażeń i podgląd
        exceptionBreakpoint();      // exception breakpoint = zatrzymanie na wyjątku
        loggingBreakpoint();        // logging breakpoint = breakpoint, który tylko wypisuje
        debuggingHabits();          // debugging habits = dobre nawyki debugowania
        exercises();                // exercises = ćwiczenia: znajdź błędy debuggerem
    }

    // =================================================================================================
    // 1. PIERWSZY BREAKPOINT
    // =================================================================================================

    /**
     * 1. ZADANIE DLA CIEBIE: postaw breakpoint na linii „int total = 0;” poniżej i uruchom lekcję w trybie Debug.
     * Naciskaj F8 i patrz w okno Variables, jak rośnie total i zmienia się price. IntelliJ pokazuje też wartości
     * zmiennych szarym tekstem na końcu linii (inline values).
     */
    static void firstBreakpoint() {
        section("1. Pierwszy breakpoint i F8 (Step Over)");

        int[] prices = {120, 45, 300};
        int total = 0;                                   // ← tu postaw breakpoint
        for (int price : prices) {
            total += price;                              // F8: obserwuj total po każdym obrocie pętli
        }
        show("suma", total);
        // WYNIK: suma → 465
    }

    // =================================================================================================
    // 2. F7 — WEJŚCIE DO METODY; STOS WYWOŁAŃ
    // =================================================================================================

    static int factorial(int n) {
        if (n <= 1) {
            return 1;                                    // ← breakpoint tutaj: w panelu Frames zobaczysz 5 poziomów factorial
        }
        return n * factorial(n - 1);
    }

    /**
     * 2. Postaw breakpoint na linii z wywołaniem factorial(5) i naciśnij F7 kilka razy — debugger wejdzie w kolejne
     * wywołania rekurencyjne. Panel Frames (ramki) to STOS WYWOŁAŃ: klikając niższą ramkę, widzisz zmienne tamtego
     * wywołania (n = 5, 4, 3...). Shift+F8 wychodzi z bieżącej metody.
     */
    static void stepIntoAndStack() {
        section("2. F7 (Step Into) i panel Frames");

        int result = factorial(5);                       // ← breakpoint + F7
        show("factorial(5)", result);
        // WYNIK: factorial(5) → 120

        // PUŁAPKA: F7 na linii z wywołaniami JDK (np. String.format) wchodzi też do kodu biblioteki. Zwykle chcesz
        //   tylko swoich metod — IntelliJ domyślnie pomija klasy java.* (Settings → Debugger → Stepping).
        //   Smart Step Into (Shift+F7) pozwala wybrać, do której metody w linii wejść.
    }

    // =================================================================================================
    // 3. BREAKPOINT WARUNKOWY
    // =================================================================================================

    /**
     * 3. W pętli z 1000 obrotów nie chcesz naciskać F9 999 razy. Kliknij PRAWYM na breakpoint → wpisz warunek,
     * np. i == 737. Program zatrzyma się tylko wtedy, gdy warunek jest prawdziwy.
     */
    static void conditionalBreakpoint() {
        section("3. Breakpoint warunkowy (prawy klik na kropce → Condition)");

        long sum = 0;
        for (int i = 0; i < 1000; i++) {
            sum += (long) i * i % 7;                     // ← breakpoint z warunkiem: i == 737
        }
        show("suma reszt", sum);
        // WYNIK: suma reszt → 2001

        // DOBRA PRAKTYKA: warunek może być dowolnym wyrażeniem Javy: name.equals("Ewa"), list.size() > 10, x < 0.
        //   Opcja „Pass count” zatrzymuje dopiero przy N-tym przejściu.
    }

    // =================================================================================================
    // 4. EVALUATE EXPRESSION I WATCHES
    // =================================================================================================

    /**
     * 4. Zatrzymany program to „laboratorium”: Alt+F8 (Evaluate Expression) policzy dowolne wyrażenie na bieżących
     * zmiennych, np. words.stream().filter(w -> w.length() > 4).toList(). Watches (+ w panelu Variables) pokazują
     * wybrane wyrażenia przy każdym zatrzymaniu. Wartość zmiennej można też ZMIENIĆ (F2 na zmiennej → Set Value).
     */
    static void evaluateAndWatches() {
        section("4. Evaluate Expression (Alt+F8) i Watches");

        List<String> words = List.of("debugger", "kod", "breakpoint", "java");
        int longCount = 0;                               // ← breakpoint: spróbuj Alt+F8 → words.get(2).length()
        for (String w : words) {
            if (w.length() > 4) {
                longCount++;
            }
        }
        show("słowa dłuższe niż 4 litery", longCount);
        // WYNIK: słowa dłuższe niż 4 litery → 2

        // PUŁAPKA: Evaluate wykonuje kod NAPRAWDĘ — wywołanie list.remove(0) w oknie Evaluate zmieni listę programu.
        //   Do podglądu używaj wyrażeń bez efektów ubocznych.
    }

    // =================================================================================================
    // 5. BREAKPOINT NA WYJĄTKU
    // =================================================================================================

    /** firstLetter = pierwsza litera; dla pustego napisu rzuci wyjątek — w miejscu, które pokaże exception breakpoint. */
    static char firstLetter(String text) {
        return text.charAt(0);
    }

    /**
     * 5. Run → View Breakpoints (Ctrl+Shift+F8) → „+” → Java Exception Breakpoints → np. StringIndexOutOfBoundsException.
     * Debugger zatrzyma się w chwili RZUCENIA wyjątku — z wszystkimi zmiennymi — zanim ktoś go złapie i „schowa”.
     */
    static void exceptionBreakpoint() {
        section("5. Exception breakpoint — zatrzymaj się, gdy leci wyjątek");

        List<String> names = List.of("Ala", "", "Ola");
        StringBuilder initials = new StringBuilder();
        for (String n : names) {
            try {
                initials.append(firstLetter(n));
            } catch (StringIndexOutOfBoundsException e) {
                initials.append('?');                    // wyjątek złapany — bez exception breakpointu trudno go zauważyć
            }
        }
        show("inicjały", initials);
        // WYNIK: inicjały → A?O

        // DOBRA PRAKTYKA: gdy program „dziwnie” się zachowuje, a w logach nic nie ma — włącz exception breakpoint na
        //   RuntimeException (odznacz „Caught exception” dla klas JDK, żeby nie zatrzymywał się co chwilę w bibliotekach).
    }

    // =================================================================================================
    // 6. LOGGING BREAKPOINT — println bez zmieniania kodu
    // =================================================================================================

    /**
     * 6. Prawy klik na breakpoint → odznacz „Suspend” (wstrzymaj) → zaznacz „Evaluate and log” i wpisz np.
     * "i=" + i + ", value=" + value. Program NIE staje, a wartości trafiają do konsoli — jak println, ale bez zmiany
     * kodu (i bez ryzyka, że zapomnisz go usunąć przed commitem).
     */
    static void loggingBreakpoint() {
        section("6. Logging breakpoint (bez zatrzymywania)");

        int value = 1;
        for (int i = 1; i <= 5; i++) {
            value = value * 3 % 11;                      // ← breakpoint „Evaluate and log”: "i=" + i + " value=" + value
        }
        show("wartość końcowa", value);
        // WYNIK: wartość końcowa → 1
    }

    // =================================================================================================
    // 7. NAWYKI
    // =================================================================================================

    /** 7. Jak szukać błędu metodycznie, a nie „na ślepo”. */
    static void debuggingHabits() {
        section("7. Metoda szukania błędów");

        note("1) odtwórz błąd na małych danych; 2) postaw hipotezę; 3) breakpoint TAM, gdzie hipoteza da się sprawdzić");
        note("4) porównaj oczekiwaną wartość z rzeczywistą; 5) po poprawce dopisz test/ćwiczenie, które by go złapało");
        // WYNIK:    ℹ 1) odtwórz błąd na małych danych; 2) postaw hipotezę; 3) breakpoint TAM, gdzie hipoteza da się sprawdzić
        // WYNIK:    ℹ 4) porównaj oczekiwaną wartość z rzeczywistą; 5) po poprawce dopisz test/ćwiczenie, które by go złapało

        // Inne narzędzia: „Trace Current Stream Chain” (ikona w panelu Debug przy streamie) pokazuje, co przeszło przez
        //   każdy etap streamu; „Reset Frame” (dawniej Drop Frame) cofa wykonanie do początku bieżącej metody.
        // PUŁAPKA: w trybie Debug program jest wolniejszy, a przy wątkach zatrzymanie jednego zmienia „wyścig” —
        //   błędy współbieżności potrafią „znikać” pod debuggerem (t21_concurrency).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Debug: prawy klik na ▶ przy main → Debug (Shift+F9). Breakpoint: klik na marginesie (Ctrl+F8).
     *   • F8 Step Over · F7 Step Into · Shift+F7 Smart Step Into · Shift+F8 Step Out · F9 Resume · Alt+F9 Run to Cursor.
     *   • Alt+F8 Evaluate Expression; Watches; F2 na zmiennej → Set Value (zmiana wartości w locie).
     *   • Panel Frames = stos wywołań (klik na ramce → jej zmienne). Ctrl+Shift+F8 = lista wszystkich breakpointów.
     *   • Breakpoint warunkowy: prawy klik → Condition (np. i == 737); Pass count.
     *   • Exception breakpoint: zatrzymanie w chwili rzucenia wyjątku (także złapanego).
     *   • Logging breakpoint: Suspend odznaczone + Evaluate and log — „println bez zmiany kodu”.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się F8 (Step Over) od F7 (Step Into)?
     *   2. Pętla ma 10 000 obrotów, błąd pojawia się, gdy klient ma pusty e-mail. Jak zatrzymać się tylko wtedy?
     *   3. Co pokazuje panel Frames i do czego przydaje się przy rekurencji?
     *   4. ZNAJDŹ BŁĄD (w nawykach):  „Dodam 20 printów, potem je usunę przed commitem.” — co może pójść nie tak
     *      i czym to zastąpić?
     *   5. Program łapie wyjątek i po cichu zwraca 0. Jak znaleźć miejsce, w którym wyjątek powstaje?
     *   6. Co wypisze sekcja 3, jeśli w oknie Evaluate przy i == 737 wykonasz  sum = 0 ? (podpowiedź: Evaluate
     *      zmienia prawdziwe zmienne)
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — w każdej metodzie exerciseN jest CELOWY błąd. Znajdź go debuggerem i popraw.
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — napraw błędy (✘ = błąd jeszcze siedzi w kodzie)");
        Check.equal("ćw. 1: średnia ocen", 14 / 3.0, () -> exercise1(new int[]{4, 5, 5}));
        Check.equal("ćw. 2: liczby parzyste do 10", List.of(2, 4, 6, 8, 10), () -> exercise2(10));
        Check.equal("ćw. 3: największa wartość", -2, () -> exercise3(new int[]{-7, -2, -9}));
        Check.equal("ćw. 4: odwrócone słowo", "kod", () -> exercise4("dok"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 14 / 3.0, () -> solution1(new int[]{4, 5, 5}));
        Check.equal("ćw. 2 (wzorzec)", List.of(2, 4, 6, 8, 10), () -> solution2(10));
        Check.equal("ćw. 3 (wzorzec)", -2, () -> solution3(new int[]{-7, -2, -9}));
        Check.equal("ćw. 4 (wzorzec)", "kod", () -> solution4("dok"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): średnia ocen wychodzi zła. Postaw breakpoint na return i sprawdź Alt+F8 wartość sum / grades.length.
     * Podpowiedź: jakiego typu jest wynik dzielenia dwóch intów?
     */
    static double exercise1(int[] grades) {
        int sum = 0;
        for (int g : grades) {
            sum += g;
        }
        return sum / grades.length * 1.0;                // BŁĄD do znalezienia
    }

    /** ĆWICZENIE 2 (łatwe): lista liczb parzystych od 2 do max włącznie. Coś jest nie tak z końcem. Użyj F8 w pętli. */
    static List<Integer> exercise2(int max) {
        List<Integer> evens = new ArrayList<>();
        for (int i = 2; i < max; i += 2) {               // BŁĄD do znalezienia
            evens.add(i);
        }
        return evens;
    }

    /**
     * ĆWICZENIE 3 (średnie): największa wartość tablicy. Dla liczb ujemnych wynik jest zły. Obserwuj zmienną max
     * od PIERWSZEGO obrotu pętli — jaką ma wartość startową?
     */
    static int exercise3(int[] values) {
        int max = 0;                                     // BŁĄD do znalezienia
        for (int v : values) {
            if (v > max) {
                max = v;
            }
        }
        return max;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): odwrócenie słowa kończy się wyjątkiem. Włącz exception breakpoint na
     * StringIndexOutOfBoundsException i sprawdź, jaką wartość ma i w chwili błędu.
     */
    static String exercise4(String word) {
        StringBuilder sb = new StringBuilder();
        for (int i = word.length(); i >= 0; i--) {       // BŁĄD do znalezienia
            sb.append(word.charAt(i));
        }
        return sb.toString();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static double solution1(int[] grades) {
        int sum = 0;
        for (int g : grades) {
            sum += g;
        }
        return (double) sum / grades.length;             // najpierw zamiana na double, potem dzielenie
    }

    static List<Integer> solution2(int max) {
        List<Integer> evens = new ArrayList<>();
        for (int i = 2; i <= max; i += 2) {              // <= — max ma być włącznie
            evens.add(i);
        }
        return evens;
    }

    static int solution3(int[] values) {
        int max = values[0];                             // start od pierwszego elementu, nie od 0
        for (int v : values) {
            if (v > max) {
                max = v;
            }
        }
        return max;
    }

    static String solution4(String word) {
        StringBuilder sb = new StringBuilder();
        for (int i = word.length() - 1; i >= 0; i--) {   // ostatni indeks to length() - 1
            sb.append(word.charAt(i));
        }
        return sb.toString();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. F8 wykonuje całą bieżącą linię (także wywołania metod) i staje na następnej; F7 wchodzi do środka
     *      wywoływanej metody i zatrzymuje się na jej pierwszej linii.
     *   2. Breakpoint warunkowy w pętli z warunkiem, np. customer.email() == null || customer.email().isBlank().
     *   3. Stos wywołań: która metoda wywołała którą. Przy rekurencji każda ramka to jedno wywołanie — klikając ramki,
     *      widzisz wartości parametrów na każdym poziomie (n = 5, 4, 3...).
     *   4. Printy łatwo zostawić w kodzie (śmieci w logach), trzeba przebudowywać program przy każdej zmianie, a i tak
     *      widać tylko to, co przewidziałeś. Lepiej: breakpoint (zwykły albo logujący) — nic nie zmienia w kodzie.
     *   5. Exception breakpoint na typ wyjątku (albo RuntimeException) — debugger zatrzyma się w chwili rzucenia,
     *      zanim catch go połknie.
     *   6. Inną sumę — przypisanie w Evaluate wyzeruje prawdziwą zmienną sum, a pętla będzie liczyć dalej od zera.
     */
    // </editor-fold>
}
