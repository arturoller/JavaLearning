package helpers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * <pre>
 * TEMAT: Check — mini-asercje do sprawdzania ćwiczeń (bez biblioteki JUnit)
 *        (check = sprawdź; asercja = „twierdzenie”, które program weryfikuje)
 *
 * W SKRÓCIE:
 *   W ćwiczeniach (tag ĆWICZENIE) porównujemy WYNIK OCZEKIWANY (expected) z WYNIKIEM FAKTYCZNYM (actual).
 *   Check drukuje  ✔ OK  albo  ✘ BŁĄD  i liczy, ile sprawdzeń przeszło.
 *   To uproszczona wersja bibliotek testowych (np. JUnit — t25_testing). Różnica: JUnit PRZERYWA test
 *   przy pierwszym błędzie, a Check wypisuje ✘ i sprawdza dalej — żebyś widział wszystkie ćwiczenia naraz.
 *
 * JAK TO DZIAŁA:
 *   W ćwiczeniach używamy wersji z lambdą:
 *       {@code Check.equal("ćw. 1", oczekiwane, () -> exercise1(dane));}
 *   Lambda {@code () -> exercise1(dane)} to „kod do uruchomienia później”. Check uruchamia go SAM, w try/catch —
 *   więc jeśli Twoje rozwiązanie rzuci wyjątek, zobaczysz ✘ z nazwą wyjątku, a reszta lekcji wykona się normalnie.
 *
 * PORÓWNYWANIE:
 *   • zwykłe obiekty — przez Objects.equals (bezpieczne dla null),
 *   • BigDecimal — przez compareTo (wartość, bez skali: 2.0 == 2.00), także WEWNĄTRZ list i map,
 *     ale gdy skale się różnią, Check dopisze uwagę — bo w pieniądzach skala (liczba groszy) zwykle ma znaczenie,
 *   • equalExact — porównanie ścisłe (equals), gdy skala BigDecimal MA się zgadzać.
 *
 * SŁÓWKA:
 *   equal = równy; exact = dokładny; expected = oczekiwany; actual = faktyczny; condition = warunek;
 *   supplier = dostawca (kod, który „dostarczy” wynik); passed = zaliczony; failed = niezaliczony;
 *   summary = podsumowanie; same = taki sam; scale = skala (liczba cyfr po przecinku).
 * </pre>
 */
public final class Check {

    // static = licznik wspólny dla wszystkich wywołań (należy do klasy, a nie do obiektu)
    private static int passed = 0;   // passed = zaliczone
    private static int failed = 0;   // failed = niezaliczone

    private Check() {
    }

    /**
     * equal = równy. Wersja do ĆWICZEŃ: wynik podajesz jako lambdę (Supplier = dostawca).
     * Check uruchamia ją w try/catch — wyjątek w Twoim rozwiązaniu da ✘, a nie przerwie programu.
     * <p>
     * {@code Supplier<?>} — interfejs funkcyjny „nic nie przyjmuje, coś zwraca” (metoda get = pobierz).
     */
    public static void equal(String label, Object expected, Supplier<?> actual) {
        Object value;
        try {
            value = actual.get();
        } catch (Throwable t) {
            report(false, label, "rzucono wyjątek " + Console.describe(t));
            return;
        }
        equal(label, expected, value);
    }

    /**
     * equal = równy. Wersja z gotową wartością. Porównuje „z sensem” (patrz PORÓWNYWANIE w opisie klasy).
     * <p>
     * PUŁAPKA: liczby różnych typów NIE są równe: Integer 3 ≠ Long 3L (Objects.equals zwraca false).
     * Gdy wyglądają identycznie, Check pokaże w komunikacie typy, np. „oczekiwano: 3 [Integer], jest: 3 [Long]”.
     */
    public static void equal(String label, Object expected, Object actual) {
        boolean ok = same(expected, actual);
        String details = "oczekiwano: " + Console.format(expected) + ", jest: " + Console.format(actual);
        if (!ok && Console.format(expected).equals(Console.format(actual))) {
            details = "oczekiwano: " + Console.format(expected) + " [" + typeName(expected) + "], jest: "
                    + Console.format(actual) + " [" + typeName(actual) + "] — różne typy!";
        }
        report(ok, label, details);
        if (ok && expected instanceof BigDecimal e && actual instanceof BigDecimal a && e.scale() != a.scale()) {
            System.out.println("        ⚠ uwaga: ta sama wartość, ale różna skala (" + e + " vs " + a
                    + ") — przy kwotach zwykle chcesz setScale(2, ...)");
        }
    }

    /**
     * equalExact = dokładnie równy. Porównanie ŚCISŁE przez equals — dla BigDecimal liczy się też skala (2.0 ≠ 2.00).
     */
    public static void equalExact(String label, Object expected, Supplier<?> actual) {
        Object value;
        try {
            value = actual.get();
        } catch (Throwable t) {
            report(false, label, "rzucono wyjątek " + Console.describe(t));
            return;
        }
        report(Objects.equals(expected, value), label,
                "oczekiwano: " + Console.format(expected) + ", jest: " + Console.format(value));
    }

    /** isTrue = czy prawda. Sprawdza, czy warunek (condition) jest spełniony. */
    public static void isTrue(String label, boolean condition) {
        report(condition, label, "warunek nie jest spełniony");
    }

    /**
     * throwsException = rzuca wyjątek. Sprawdza, czy kod rzuca wyjątek podanego typu (albo jego podklasy).
     * <p>
     * {@code Class<? extends Throwable>} = „obiekt Class dowolnej klasy dziedziczącej po Throwable”,
     * np. {@code IllegalArgumentException.class} (.class = literał klasy, t19_annotations_reflection).
     */
    public static void throwsException(String label, Class<? extends Throwable> type, Console.ThrowingAction action) {
        try {
            action.run();
            report(false, label, "nie rzucono wyjątku, oczekiwano " + type.getSimpleName());
        } catch (Throwable t) {
            // isInstance = czy jest instancją; uwzględnia podklasy (np. NumberFormatException to IllegalArgumentException)
            report(type.isInstance(t), label, "rzucono " + t.getClass().getSimpleName() + ", oczekiwano " + type.getSimpleName());
        }
    }

    /**
     * summary = podsumowanie. Drukuje, ile sprawdzeń przeszło, i zeruje liczniki,
     * żeby następna seria liczyła się od nowa.
     */
    public static void summary() {
        System.out.println();
        System.out.println("PODSUMOWANIE: ✔ " + passed + " OK, ✘ " + failed + " BŁĄD");
        passed = 0;
        failed = 0;
    }

    /**
     * same = takie same. Porównanie „z sensem”:
     * BigDecimal przez compareTo; listy — element po elemencie tą samą zasadą; mapy — te same klucze
     * i „takie same” wartości; wszystko inne przez Objects.equals.
     * <p>
     * To metoda REKURENCYJNA (wywołuje samą siebie dla elementów list i wartości map — t05_methods/Methods03Recursion).
     */
    private static boolean same(Object a, Object b) {
        if (a instanceof BigDecimal x && b instanceof BigDecimal y) {
            return x.compareTo(y) == 0;
        }
        if (a instanceof List<?> x && b instanceof List<?> y) {
            if (x.size() != y.size()) {
                return false;
            }
            for (int i = 0; i < x.size(); i++) {
                if (!same(x.get(i), y.get(i))) {
                    return false;
                }
            }
            return true;
        }
        if (a instanceof Map<?, ?> x && b instanceof Map<?, ?> y) {
            if (!x.keySet().equals(y.keySet())) {
                return false;
            }
            for (Object key : x.keySet()) {
                if (!same(x.get(key), y.get(key))) {
                    return false;
                }
            }
            return true;
        }
        return Objects.equals(a, b);
    }

    /** typeName = nazwa typu, np. "Integer"; dla null — "null". */
    private static String typeName(Object o) {
        return o == null ? "null" : o.getClass().getSimpleName();
    }

    /** report = raportuj. Wspólna prywatna metoda — żeby nie powtarzać kodu (zasada DRY: Don't Repeat Yourself). */
    private static void report(boolean ok, String label, String details) {
        if (ok) {
            passed++;
            System.out.println("✔ OK    " + label);
        } else {
            failed++;
            System.out.println("✘ BŁĄD  " + label + " (" + details + ")");
        }
    }
}
