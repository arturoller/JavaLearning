package t10_exceptions;

import helpers.Check;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Wyjątki — dobre praktyki i antywzorce (PRZED/PO)
 *        (best practice = dobra praktyka; anti-pattern = antywzorzec; fail fast = zawiedź szybko)
 *
 * W SKRÓCIE:
 *   Najczęstsze błędy z wyjątkami: „połykanie” (pusty catch — błąd znika, a dane są złe), wyjątki zamiast zwykłego if,
 *   null przepuszczany głęboko (błąd wybucha daleko od przyczyny), ogólnikowe komunikaty i zmiana stanu obiektu
 *   „w połowie” przed wyjątkiem. Dobre zasady: sprawdzaj dane NA POCZĄTKU (fail fast), komunikat z kontekstem,
 *   brak wartości jako Optional, błąd jako wyjątek, najpierw walidacja — potem zmiana stanu.
 *
 * ANALOGIA: czujnik dymu.
 *   Wyjęcie baterii z piszczącego czujnika (pusty catch) nie gasi pożaru — tylko przestajesz o nim wiedzieć.
 *   Dobry czujnik piszczy WCZEŚNIE (fail fast) i mówi, W KTÓRYM pokoju (komunikat z kontekstem).
 *
 * JAK TO DZIAŁA:
 *   Objects.requireNonNull(name, "name nie może być null");   ← fail fast: NPE od razu, z jasnym komunikatem
 *   if (amount <= 0) throw new IllegalArgumentException("Kwota musi być dodatnia: " + amount);
 *   Optional<Customer> findCustomer(id)   ← brak klienta to NORMALNA sytuacja → Optional
 *   Customer getCustomer(id)              ← klient MUSI istnieć → wyjątek, gdy go nie ma
 *
 * SŁÓWKA:
 *   swallow = połknąć (wyjątek); fail fast = zawiedź szybko; control flow = sterowanie przepływem; require non null =
 *   wymagaj nie-nulla; atomic = niepodzielny (wszystko albo nic); invariant = niezmiennik (zasada zawsze prawdziwa);
 *   transfer = przelew; find = znajdź (może nie być); get = pobierz (musi być); context = kontekst.
 *
 * ZOBACZ TEŻ: t10_exceptions/Exceptions01Basics … Exceptions06ChainingWrapping (cały dział), t14_optional/Optional03BestPractices,
 *             t27_clean_code_pitfalls/Pitfalls01Classic (pułapki), t05_methods/Methods04GoodPractices (walidacja argumentów).
 * </pre>
 */
public class Exceptions07BestPractices {

    public static void main(String[] args) {
        title("Exceptions07 — dobre praktyki i antywzorce");

        dontSwallow();              // don't swallow = nie połykaj
        noExceptionsForFlow();      // no exceptions for control flow = nie steruj programem wyjątkami
        failFast();                 // fail fast = zawiedź szybko
        optionalOrException();      // Optional or exception = Optional czy wyjątek
        goodMessages();             // good messages = dobre komunikaty
        validateBeforeChanging();   // validate before changing = najpierw sprawdź, potem zmieniaj
        checklist();                // checklist = lista kontrolna
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. NIE POŁYKAJ WYJĄTKÓW
    // =================================================================================================

    /** sumSwallowing = ZŁA suma: zły wiersz po cichu pomijany. Wynik wygląda poprawnie, ale jest błędny. */
    static int sumSwallowing(List<String> prices) {
        int sum = 0;
        for (String p : prices) {
            try {
                sum += Integer.parseInt(p);
            } catch (NumberFormatException e) {
                // pusto — nikt się nie dowie, że "12O" (z literą O) pominięto
            }
        }
        return sum;
    }

    /** sumStrict = DOBRA suma: zły wiersz → wyjątek z informacją, KTÓRY i DLACZEGO. */
    static int sumStrict(List<String> prices) {
        int sum = 0;
        for (int i = 0; i < prices.size(); i++) {
            try {
                sum += Integer.parseInt(prices.get(i));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Wiersz " + (i + 1) + ": zła cena \"" + prices.get(i) + "\"", e);
            }
        }
        return sum;
    }

    /** 1. Pusty catch ukrywa błąd — program liczy dalej na złych danych. Lepiej zawieść głośno albo świadomie obsłużyć. */
    static void dontSwallow() {
        section("1. PRZED/PO: połykanie wyjątku");

        List<String> prices = List.of("100", "12O", "30");     // "12O" — litera O zamiast zera
        show("PRZED: suma", sumSwallowing(prices));
        // WYNIK: PRZED: suma → 130    ← wygląda wiarygodnie, a brakuje 120

        expectThrows("PO: sumStrict", () -> sumStrict(prices));
        // WYNIK: ✔ PO: sumStrict → rzucono IllegalArgumentException: Wiersz 2: zła cena "12O"

        // DOBRA PRAKTYKA: pusty catch jest dopuszczalny TYLKO, gdy „błąd” jest oczekiwaną sytuacją i świadomie go
        //   ignorujesz — wtedy napisz komentarz DLACZEGO (np. // zły format = pomijamy, liczymy tylko poprawne).
    }

    // =================================================================================================
    // 2. NIE STERUJ PROGRAMEM WYJĄTKAMI
    // =================================================================================================

    /** isIntegerByException = ZŁE sprawdzenie: „spróbuj i złap”. Wyjątek jako zwykły if — wolne i nieczytelne. */
    static boolean isIntegerByException(String text) {
        try {
            Integer.parseInt(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** isDigitsOnly = sprawdzenie bez wyjątków: niepusty napis z samych cyfr (Character.isDigit). */
    static boolean isDigitsOnly(String text) {
        if (text.isEmpty()) {
            return false;
        }
        for (char c : text.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 2. Wyjątki są na sytuacje WYJĄTKOWE. Utworzenie wyjątku jest kosztowne (Java zapisuje cały ślad stosu).
     * Jeśli warunek da się sprawdzić zwykłym if — sprawdź go.
     */
    static void noExceptionsForFlow() {
        section("2. PRZED/PO: wyjątek zamiast if");

        int[] data = {3, 5, 7};
        int sum = 0;
        int i = 0;
        try {
            while (true) {                               // PRZED: pętla kończona wyjątkiem — antywzorzec
                sum += data[i++];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            show("PRZED: suma (koniec przez wyjątek)", sum);
        }
        // WYNIK: PRZED: suma (koniec przez wyjątek) → 15

        int sum2 = 0;
        for (int value : data) {                         // PO: zwykła pętla — koniec wynika z warunku
            sum2 += value;
        }
        show("PO: suma (pętla for-each)", sum2);
        // WYNIK: PO: suma (pętla for-each) → 15

        show("isIntegerByException(\"42\") / isDigitsOnly(\"42\")", isIntegerByException("42") + " / " + isDigitsOnly("42"));
        // WYNIK: isIntegerByException("42") / isDigitsOnly("42") → true / true

        // Uwaga: przy parsowaniu liczb „spróbuj i złap” bywa akceptowalne (reguły formatu są złożone: znak, zakres int).
        //   Antywzorcem jest przede wszystkim wyjątek zamiast PROSTEGO warunku (indeks < length, x != null, map.containsKey).
    }

    // =================================================================================================
    // 3. FAIL FAST — ZAWIEDŹ SZYBKO
    // =================================================================================================

    /** Label = etykieta (klasa z polem, które nie może być null). */
    static final class Label {
        private final String text;

        /** PRZED: brak sprawdzenia — null zostanie zapisany i wybuchnie dopiero przy użyciu, daleko stąd. */
        Label(String text, boolean validate) {
            this.text = validate ? Objects.requireNonNull(text, "text etykiety nie może być null") : text;
        }

        int width() {
            return text.length() + 4;               // tu dopiero wybuchnie null w wersji bez walidacji
        }
    }

    /**
     * 3. Objects.requireNonNull(x, "komunikat") rzuca NPE OD RAZU, w konstruktorze — ślad stosu wskazuje miejsce,
     * gdzie null WSZEDŁ, a nie gdzie kiedyś został użyty.
     */
    static void failFast() {
        section("3. Fail fast: Objects.requireNonNull");

        Label lazy = new Label(null, false);                 // PRZED: obiekt powstał „zepsuty”
        expectThrows("PRZED: dopiero lazy.width()", lazy::width);
        // WYNIK: ✔ PRZED: dopiero lazy.width() → rzucono NullPointerException: Cannot invoke "String.length()" because "this.text" is null

        expectThrows("PO: już w konstruktorze", () -> new Label(null, true));
        // WYNIK: ✔ PO: już w konstruktorze → rzucono NullPointerException: text etykiety nie może być null

        // DOBRA PRAKTYKA: sprawdzaj argumenty na POCZĄTKU metod publicznych i w konstruktorach. Im bliżej przyczyny
        //   wybuchnie błąd, tym szybciej go znajdziesz.
    }

    // =================================================================================================
    // 4. Optional CZY WYJĄTEK
    // =================================================================================================

    static final Map<Integer, String> CUSTOMERS = Map.of(1, "Anna", 2, "Jan");

    /** findCustomer = znajdź klienta. Brak to normalna sytuacja (np. wyszukiwarka) → Optional. */
    static Optional<String> findCustomer(int id) {
        return Optional.ofNullable(CUSTOMERS.get(id));
    }

    /** getCustomer = pobierz klienta. Klient MUSI istnieć (np. id z zamówienia) → brak to błąd → wyjątek. */
    static String getCustomer(int id) {
        return findCustomer(id).orElseThrow(() -> new NoSuchElementException("Brak klienta o id " + id));
    }

    /** 4. Konwencja nazw pomaga: find… → Optional (może nie być), get… → wartość albo wyjątek. Nigdy „cichy” null. */
    static void optionalOrException() {
        section("4. find → Optional, get → wyjątek");

        show("findCustomer(9)", findCustomer(9));
        show("findCustomer(9).orElse(\"gość\")", findCustomer(9).orElse("gość"));
        // WYNIK: findCustomer(9) → Optional.empty
        // WYNIK: findCustomer(9).orElse("gość") → gość

        expectThrows("getCustomer(9)", () -> getCustomer(9));
        // WYNIK: ✔ getCustomer(9) → rzucono NoSuchElementException: Brak klienta o id 9

        // PUŁAPKA: zwracanie null „gdy nie ma” przenosi problem na wywołującego, który zapomni sprawdzić — i NPE
        //   wybuchnie gdzie indziej (t14_optional/Optional03BestPractices).
    }

    // =================================================================================================
    // 5. DOBRE KOMUNIKATY
    // =================================================================================================

    /** 5. Komunikat ma mówić: CO się nie udało, Z JAKĄ WARTOŚCIĄ i DLACZEGO. Czytasz go o 3 w nocy z logów. */
    static void goodMessages() {
        section("5. Komunikat z kontekstem");

        show("PRZED", new IllegalArgumentException("Błąd").getMessage());
        show("PO", new IllegalArgumentException(
                "Nie można przelać -50 zł z konta PL-001: kwota musi być dodatnia").getMessage());
        // WYNIK: PRZED → Błąd
        // WYNIK: PO → Nie można przelać -50 zł z konta PL-001: kwota musi być dodatnia

        // PUŁAPKA: nie umieszczaj w komunikatach danych wrażliwych (hasła, pełne numery kart) — komunikaty trafiają do logów.
    }

    // =================================================================================================
    // 6. NAJPIERW SPRAWDŹ, POTEM ZMIENIAJ
    // =================================================================================================

    /** transferBad = ZŁY przelew: odejmuje z konta, a POTEM sprawdza konto docelowe — wyjątek zostawia pieniądze „w próżni”. */
    static void transferBad(int[] accounts, int from, int to, int amount) {
        accounts[from] -= amount;                 // stan już zmieniony...
        accounts[to] += amount;                   // ...a tu wyjątek (zły indeks) — 50 zł znika
    }

    /** transferGood = DOBRY przelew: wszystkie sprawdzenia NAJPIERW, zmiany stanu dopiero na końcu (wszystko albo nic). */
    static void transferGood(int[] accounts, int from, int to, int amount) {
        Objects.checkIndex(from, accounts.length);   // checkIndex = sprawdź indeks (Java 9+) → IndexOutOfBoundsException
        Objects.checkIndex(to, accounts.length);
        if (amount <= 0) {
            throw new IllegalArgumentException("Kwota musi być dodatnia: " + amount);
        }
        if (accounts[from] < amount) {
            throw new IllegalStateException("Brak środków na koncie " + from + ": " + accounts[from] + " < " + amount);
        }
        accounts[from] -= amount;
        accounts[to] += amount;
    }

    /** 6. Wyjątek w połowie operacji nie może zostawić danych w stanie „pół na pół”. */
    static void validateBeforeChanging() {
        section("6. PRZED/PO: walidacja przed zmianą stanu");

        int[] bad = {100, 100};
        expectThrows("PRZED: przelew na konto 5", () -> transferBad(bad, 0, 5, 50));
        show("PRZED: konta po błędzie", Arrays.toString(bad));
        // WYNIK: ✔ PRZED: przelew na konto 5 → rzucono ArrayIndexOutOfBoundsException: Index 5 out of bounds for length 2
        // WYNIK: PRZED: konta po błędzie → [50, 100]    ← 50 zł zniknęło!

        int[] good = {100, 100};
        expectThrows("PO: przelew na konto 5", () -> transferGood(good, 0, 5, 50));
        show("PO: konta po błędzie", Arrays.toString(good));
        // WYNIK: ✔ PO: przelew na konto 5 → rzucono IndexOutOfBoundsException: Index 5 out of bounds for length 2
        // WYNIK: PO: konta po błędzie → [100, 100]    ← nic się nie zmieniło

        // DOBRA PRAKTYKA: „wszystko albo nic” — najpierw sprawdź WSZYSTKIE warunki, potem zmieniaj stan. W bazach danych
        //   to samo zapewniają transakcje.
    }

    // =================================================================================================
    // 7. LISTA KONTROLNA
    // =================================================================================================

    /** 7. Lista kontrolna do code review kodu z wyjątkami. */
    static void checklist() {
        section("7. Lista kontrolna");

        List<String> rules = List.of(
                "łap konkretne typy; catch (Exception e) tylko na najwyższym poziomie",
                "nie połykaj: obsłuż, rzuć dalej albo opakuj z przyczyną (, e)",
                "fail fast: sprawdzaj argumenty na początku (requireNonNull, IllegalArgumentException)",
                "nie steruj programem wyjątkami, gdy wystarczy if",
                "brak wartości jako Optional, błąd jako wyjątek; nie zwracaj null",
                "komunikat z kontekstem i wartościami, bez danych wrażliwych",
                "zasoby w try-with-resources; bez return w finally",
                "najpierw walidacja, potem zmiana stanu (wszystko albo nic)");
        for (int i = 0; i < rules.size(); i++) {
            System.out.println("   " + (i + 1) + ". " + rules.get(i));
        }
        // WYNIK:    1. łap konkretne typy; catch (Exception e) tylko na najwyższym poziomie
        // WYNIK:    2. nie połykaj: obsłuż, rzuć dalej albo opakuj z przyczyną (, e)
        // WYNIK:    3. fail fast: sprawdzaj argumenty na początku (requireNonNull, IllegalArgumentException)
        // WYNIK:    4. nie steruj programem wyjątkami, gdy wystarczy if
        // WYNIK:    5. brak wartości jako Optional, błąd jako wyjątek; nie zwracaj null
        // WYNIK:    6. komunikat z kontekstem i wartościami, bez danych wrażliwych
        // WYNIK:    7. zasoby w try-with-resources; bez return w finally
        // WYNIK:    8. najpierw walidacja, potem zmiana stanu (wszystko albo nic)
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Pusty catch = ukryty błąd. Wyjątek: świadome zignorowanie z komentarzem „dlaczego”.
     *   • Wyjątki nie zastępują if (indeks, null, containsKey). Tworzenie wyjątku jest kosztowne.
     *   • Fail fast: Objects.requireNonNull(x, "komunikat"), IllegalArgumentException na początku metody.
     *   • find… → Optional; get… → wartość albo wyjątek (orElseThrow). Nie zwracaj null.
     *   • Komunikat: co, z jaką wartością, dlaczego — bez danych wrażliwych.
     *   • Najpierw WSZYSTKIE sprawdzenia, potem zmiana stanu (wszystko albo nic); Objects.checkIndex (Java 9+).
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego pusty catch jest groźniejszy niż program, który się wywraca?
     *   2. ZNAJDŹ BŁĄD:
     *          String first(List<String> list) {
     *              try { return list.get(0); } catch (IndexOutOfBoundsException e) { return ""; }
     *          }
     *   3. Co wypisze:  int[] a = {10, 10}; try { transferBad(a, 0, 2, 5); } catch (RuntimeException e) { }
     *                   System.out.println(Arrays.toString(a));
     *   4. Kiedy metoda powinna zwrócić Optional, a kiedy rzucić wyjątek?
     *   5. Co daje Objects.requireNonNull w konstruktorze w porównaniu z brakiem sprawdzenia?
     *   6. Popraw komunikat:  throw new IllegalStateException("Błąd zamówienia");  (zamówienie Z-17 jest już wysłane,
     *      a ktoś próbuje je anulować)
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** accountsAfter = stan kont po próbie przelewu (pomocnicza do sprawdzania ćwiczenia 4). */
    static String accountsAfter(int[] accounts, int from, int to, int amount) {
        try {
            exercise4(accounts, from, to, amount);
        } catch (RuntimeException e) {
            return Arrays.toString(accounts) + " + " + e.getClass().getSimpleName();
        }
        return Arrays.toString(accounts);
    }

    static String accountsAfterSolution(int[] accounts, int from, int to, int amount) {
        try {
            solution4(accounts, from, to, amount);
        } catch (RuntimeException e) {
            return Arrays.toString(accounts) + " + " + e.getClass().getSimpleName();
        }
        return Arrays.toString(accounts);
    }

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: poprawne ceny", 30, () -> exercise1(List.of("10", "20")));
        Check.throwsException("ćw. 1b: zła cena", IllegalArgumentException.class, () -> exercise1(List.of("10", "x")));
        Check.equal("ćw. 2: liczby całkowite bez wyjątków", "true,true,false,false,false",
                () -> exercise2("123") + "," + exercise2("-7") + "," + exercise2("12a") + "," + exercise2("") + "," + exercise2("-"));
        Check.equal("ćw. 3a: \" Ola \"", "Ola", () -> exercise3(" Ola "));
        Check.throwsException("ćw. 3b: null → NPE", NullPointerException.class, () -> exercise3(null));
        Check.throwsException("ćw. 3c: \"  \" → IAE", IllegalArgumentException.class, () -> exercise3("  "));
        Check.equal("ćw. 4a: udany przelew", "[70, 130]", () -> accountsAfter(new int[]{100, 100}, 0, 1, 30));
        Check.equal("ćw. 4b: za mało środków", "[100, 100] + IllegalStateException",
                () -> accountsAfter(new int[]{100, 100}, 0, 1, 500));
        Check.equal("ćw. 4c: złe konto docelowe", "[100, 100] + IndexOutOfBoundsException",
                () -> accountsAfter(new int[]{100, 100}, 0, 3, 30));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", 30, () -> solution1(List.of("10", "20")));
        Check.throwsException("ćw. 1b (wzorzec)", IllegalArgumentException.class, () -> solution1(List.of("10", "x")));
        Check.equal("ćw. 2 (wzorzec)", "true,true,false,false,false",
                () -> solution2("123") + "," + solution2("-7") + "," + solution2("12a") + "," + solution2("") + "," + solution2("-"));
        Check.equal("ćw. 3a (wzorzec)", "Ola", () -> solution3(" Ola "));
        Check.throwsException("ćw. 3b (wzorzec)", NullPointerException.class, () -> solution3(null));
        Check.throwsException("ćw. 3c (wzorzec)", IllegalArgumentException.class, () -> solution3("  "));
        Check.equal("ćw. 4a (wzorzec)", "[70, 130]", () -> accountsAfterSolution(new int[]{100, 100}, 0, 1, 30));
        Check.equal("ćw. 4b (wzorzec)", "[100, 100] + IllegalStateException",
                () -> accountsAfterSolution(new int[]{100, 100}, 0, 1, 500));
        Check.equal("ćw. 4c (wzorzec)", "[100, 100] + IndexOutOfBoundsException",
                () -> accountsAfterSolution(new int[]{100, 100}, 0, 3, 30));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 9 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe, „PRZEPISZ”): przepisz sumSwallowing tak, żeby zła cena nie była połykana — rzuć
     * IllegalArgumentException z komunikatem zawierającym złą wartość (i przyczyną e).
     */
    static int exercise1(List<String> prices) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (średnie): czy napis to liczba całkowita — BEZ wyjątków: opcjonalny '-' na początku,
     * potem co najmniej jedna cyfra i same cyfry. (Zakres int pomijamy.) Podpowiedź: isDigitsOnly z sekcji 2.
     */
    static boolean exercise2(String text) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): fail fast. null → NullPointerException (Objects.requireNonNull z komunikatem);
     * napis pusty/same spacje → IllegalArgumentException; w pozostałych przypadkach zwróć napis bez spacji na brzegach.
     */
    static String exercise3(String name) {
        // TODO: twoje rozwiązanie
        return name;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): przelew „wszystko albo nic”. Zły indeks konta → IndexOutOfBoundsException
     * (Objects.checkIndex), kwota ≤ 0 → IllegalArgumentException, za mało środków → IllegalStateException.
     * Przy KAŻDYM błędzie tablica accounts ma zostać nienaruszona. Podpowiedź: transferGood z sekcji 6.
     */
    static void exercise4(int[] accounts, int from, int to, int amount) {
        // TODO: twoje rozwiązanie
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(List<String> prices) {
        int sum = 0;
        for (String p : prices) {
            try {
                sum += Integer.parseInt(p);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Zła cena: \"" + p + "\"", e);
            }
        }
        return sum;
    }

    static boolean solution2(String text) {
        String digits = text.startsWith("-") ? text.substring(1) : text;
        return isDigitsOnly(digits);
    }

    static String solution3(String name) {
        Objects.requireNonNull(name, "name nie może być null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name nie może być pusty");
        }
        return name.strip();
    }

    static void solution4(int[] accounts, int from, int to, int amount) {
        transferGood(accounts, from, to, amount);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Program, który się wywraca, od razu pokazuje problem. Pusty catch ukrywa błąd: program działa dalej na złych
     *      danych (np. zaniżona suma), a skutki wychodzą później, daleko od przyczyny — albo wcale.
     *   2. Wyjątek użyty zamiast prostego warunku: wystarczy if (list.isEmpty()) return "";. Do tego catch może połknąć
     *      inny, nieoczekiwany IndexOutOfBoundsException.
     *   3. „[5, 10]” — odjęto 5 z konta 0, a dodanie do nieistniejącego konta 2 rzuciło wyjątek; 5 zł zniknęło.
     *   4. Optional — gdy brak wartości to normalny, spodziewany wynik (wyszukiwanie). Wyjątek — gdy brak oznacza błąd
     *      (dane MUSIAŁY istnieć, np. klient z zamówienia).
     *   5. Błąd wychodzi natychmiast, w miejscu, gdzie null wszedł do obiektu, z czytelnym komunikatem — zamiast
     *      NPE gdzieś później, przy pierwszym użyciu pola.
     *   6. Np.: throw new IllegalStateException("Nie można anulować zamówienia Z-17: status WYSLANE (anulować można
     *      tylko NOWE i OPLACONE)");
     */
    // </editor-fold>
}
