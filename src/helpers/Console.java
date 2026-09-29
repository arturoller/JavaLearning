package helpers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * <pre>
 * TEMAT: Console — pomocnik do czytelnego drukowania wyników lekcji
 *        (console = konsola, czyli okno, w którym widzisz tekst wypisany przez program)
 *
 * W SKRÓCIE:
 *   Każda lekcja drukuje wyniki przez te metody, żeby wydruk był uporządkowany:
 *   tytuł lekcji → numerowane sekcje → linie „etykieta → wartość”.
 *
 * JAK TO DZIAŁA:
 *   Na górze pliku lekcji jest:  {@code import static helpers.Console.*;}
 *   „import static” (import statyczny) pozwala pisać po prostu  {@code show("x", 5)}
 *   zamiast  {@code Console.show("x", 5)}.  Szczegóły: t06_oop_basics/Oop08PackagesAccess.
 *
 * SŁÓWKA:
 *   title = tytuł; section = sekcja, rozdział; show = pokaż; label = etykieta; value = wartość;
 *   note = notatka; line = linia; each = każdy; expect = oczekiwać; throws = rzuca (wyjątek);
 *   action = akcja, czynność; format = sformatuj (zamień na tekst w określonej postaci);
 *   utility class = klasa narzędziowa (same metody statyczne).
 *
 * DOBRA PRAKTYKA: to jest „klasa narzędziowa” (utility class) — ma tylko metody static i prywatny
 *   konstruktor, więc nie da się (i nie trzeba) tworzyć jej obiektów przez {@code new Console()}.
 * </pre>
 */
public final class Console {   // final = nie można po niej dziedziczyć (nikt nie napisze „extends Console”)

    /** WIDTH = szerokość linii oddzielających. static final = stała wspólna dla klasy i niezmienna. */
    private static final int WIDTH = 78;

    /** Prywatny konstruktor (private = prywatny) = zakaz tworzenia obiektów tej klasy. */
    private Console() {
    }

    /**
     * title = tytuł. Drukuje duży nagłówek lekcji — linia ze znaków „=”, tytuł, znowu linia.
     */
    public static void title(String text) {
        System.out.println();
        System.out.println("=".repeat(WIDTH));        // repeat = powtórz (Java 11+): "=" powtórzone 78 razy
        System.out.println(" " + text);
        System.out.println("=".repeat(WIDTH));
    }

    /**
     * section = sekcja. Drukuje nagłówek jednej części lekcji, np. „--- 1. filter ------”.
     * Math.max(a, b) = większa z dwóch liczb (tu: pilnuje, żeby kresek było co najmniej 3).
     */
    public static void section(String text) {
        System.out.println();
        System.out.println("--- " + text + " " + "-".repeat(Math.max(3, WIDTH - text.length() - 5)));
    }

    /**
     * show = pokaż. Drukuje jedną linię w formacie:  etykieta → wartość
     * <p>
     * Parametr typu Object (obiekt) przyjmie WSZYSTKO: liczbę, napis, listę, mapę, tablicę...
     * Zamianę na tekst robi metoda {@code format} (niżej).
     */
    public static void show(String label, Object value) {
        System.out.println(label + " → " + format(value));
    }

    /**
     * showEach = pokaż każdy (element). Drukuje kolekcję — każdy element w osobnej linii.
     * Przydaje się, gdy lista jest długa i w jednej linii byłaby nieczytelna.
     * <p>
     * {@code Collection<?>} = „kolekcja czegokolwiek” (? to tzw. wildcard, czyli dżoker — t11_generics/Generics05Wildcards).
     */
    public static void showEach(String label, Collection<?> items) {
        System.out.println(label + " (liczba elementów: " + items.size() + "):");
        for (Object item : items) {           // for-each = „dla każdego elementu” (t02_controlflow/Control03Loops)
            System.out.println("   • " + format(item));
        }
    }

    /**
     * showEach — wersja dla mapy. To PRZECIĄŻENIE metody (overload = przeciążyć): ta sama nazwa,
     * inne parametry (t05_methods/Methods02Overloading). Każda para klucz → wartość w osobnej linii.
     */
    public static void showEach(String label, Map<?, ?> map) {
        System.out.println(label + " (liczba kluczy: " + map.size() + "):");
        // Map.Entry = wpis mapy (para klucz + wartość); entrySet = zbiór wszystkich wpisów
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            // getKey = pobierz klucz, getValue = pobierz wartość
            System.out.println("   • " + format(entry.getKey()) + " → " + format(entry.getValue()));
        }
    }

    /** note = notatka. Drukuje wyjaśnienie w wyniku programu (z symbolem ℹ). */
    public static void note(String text) {
        System.out.println("   ℹ " + text);
    }

    /** line = linia. Pusta linia odstępu. */
    public static void line() {
        System.out.println();
    }

    /**
     * ThrowingAction = „akcja, która może rzucić wyjątek”.
     * <p>
     * To NASZ WŁASNY interfejs funkcyjny (functional interface = interfejs z jedną metodą abstrakcyjną,
     * dzięki czemu można go zastąpić lambdą). Dlaczego nie zwykły Runnable (do uruchomienia)?
     * Bo {@code Runnable.run()} NIE MOŻE rzucać wyjątków sprawdzanych (checked, np. IOException),
     * a nasza metoda {@code run()} ma dopisek {@code throws Exception}, więc może.
     * Szczegóły: t13_lambdas/Lambda08Pitfalls oraz t10_exceptions/Exceptions02CheckedUnchecked.
     */
    @FunctionalInterface   // adnotacja: kompilator sprawdzi, że jest dokładnie jedna metoda abstrakcyjna
    public interface ThrowingAction {
        void run() throws Exception;   // run = uruchom
    }

    /**
     * expectThrows = oczekuj, że (kod) rzuci wyjątek.
     * <p>
     * Uruchamia podany kod (lambdę) i:
     * <ul>
     *   <li>jeśli kod RZUCI wyjątek — łapie go i drukuje ✔ z typem i komunikatem wyjątku,</li>
     *   <li>jeśli NIE rzuci — drukuje ✘, bo spodziewaliśmy się błędu.</li>
     * </ul>
     * Dzięki temu lekcja może POKAZAĆ pułapkę (np. NullPointerException), a program działa dalej.
     * <p>
     * Przykład: {@code expectThrows("dzielenie przez zero", () -> System.out.println(10 / 0));}
     * <br>wydrukuje: ✔ dzielenie przez zero → rzucono ArithmeticException: / by zero
     */
    public static void expectThrows(String label, ThrowingAction action) {
        try {                                  // try = spróbuj
            action.run();
            System.out.println("✘ " + label + " → NIE rzucono wyjątku (a spodziewaliśmy się go)");
        } catch (Throwable t) {                // catch = złap; Throwable = „coś, co można rzucić” (wyjątki + błędy JVM)
            System.out.println("✔ " + label + " → rzucono " + describe(t));
        }
    }

    /**
     * describe = opisz. Zwraca np. "ArithmeticException: / by zero".
     * getClass().getSimpleName() = pobierz klasę → jej prostą nazwę (bez pakietu).
     * getMessage() = pobierz komunikat wyjątku — BYWA null, wtedy piszemy „(brak komunikatu)”.
     */
    static String describe(Throwable t) {
        String message = t.getMessage();
        return t.getClass().getSimpleName() + ": " + (message == null ? "(brak komunikatu)" : message);
    }

    /**
     * format = sformatuj, zamień na tekst.
     * <p>
     * Zwykle wystarcza String.valueOf(value) — wywołuje toString() obiektu (albo daje "null").
     * WYJĄTEK: tablice. Tablica NIE ma „ładnego” toString — wypisanie {@code new int[]{1, 2}} daje coś w stylu
     * „[I@1b6d3586” (typ + adres w pamięci). Dlatego tablice zamieniamy przez Arrays.deepToString,
     * które daje „[1, 2]” (także dla tablic w tablicach). Szczegóły: t03_arrays/Arrays03Utility.
     */
    static String format(Object value) {
        if (value != null && value.getClass().isArray()) {           // isArray = czy to tablica
            // Sztuczka: pakujemy tablicę w jednoelementową tablicę obiektów, bo deepToString przyjmuje Object[]
            // (a int[] nie jest Object[]). Wynik „[[1, 2]]” — odcinamy zewnętrzne nawiasy.
            String text = Arrays.deepToString(new Object[]{value});
            return text.substring(1, text.length() - 1);
        }
        return String.valueOf(value);
    }
}
