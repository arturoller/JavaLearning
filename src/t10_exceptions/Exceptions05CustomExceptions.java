package t10_exceptions;

import helpers.Check;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Własne wyjątki — kiedy, jak nazwać, jakie konstruktory i pola, hierarchia wyjątków domenowych
 *        (custom exception = własny wyjątek; domain = dziedzina (biznesowa); field = pole)
 *
 * W SKRÓCIE:
 *   Gdy gotowe wyjątki (IllegalArgumentException, IllegalStateException) nie mówią wystarczająco dużo, tworzysz własny:
 *   klasę dziedziczącą po RuntimeException (niesprawdzany — zwykle) albo Exception (sprawdzany). Własny wyjątek
 *   ma nazwę z dziedziny („brak środków”, „brak towaru”), może nieść DANE (kwota, brakująca ilość) i można go łapać
 *   osobno od innych. Kilka wyjątków jednej dziedziny warto zebrać pod wspólną klasą bazową.
 *
 * ANALOGIA: kody usterek w samochodzie.
 *   Kontrolka „check engine” (Exception) mówi tylko, że coś jest nie tak. Konkretny kod usterki („P0420 — katalizator,
 *   sprawność 62%”) to własny wyjątek z danymi: mechanik od razu wie, co naprawić i ile brakuje do normy.
 *
 * JAK TO DZIAŁA:
 *   class InsufficientFundsException extends RuntimeException {
 *       private final long shortfall;                         ← dane o błędzie (final — wyjątek niezmienny)
 *       InsufficientFundsException(long balance, long amount) {
 *           super("Brak środków: saldo " + balance + ", żądano " + amount);   ← komunikat do klasy bazowej
 *           this.shortfall = amount - balance;
 *       }
 *       long getShortfall() { return shortfall; }
 *   }
 *
 * SŁÓWKA:
 *   custom = własny; insufficient funds = niewystarczające środki; shortfall = niedobór (ile brakuje); balance = saldo;
 *   amount = kwota; withdraw = wypłać; validation = walidacja (sprawdzanie poprawności); out of stock = brak na stanie;
 *   payment declined = płatność odrzucona; shop = sklep; order = zamówienie; serial version UID = numer wersji serializacji.
 *
 * ZOBACZ TEŻ: t10_exceptions/Exceptions02CheckedUnchecked (checked/unchecked), t10_exceptions/Exceptions06ChainingWrapping
 *             (konstruktor z cause), t07_inheritance_polymorphism/Inherit01Basics (dziedziczenie), t10_exceptions/Exceptions07BestPractices.
 * </pre>
 */
public class Exceptions05CustomExceptions {

    // ---------------------------------------------------------------------------------------------
    // Własne wyjątki używane w lekcji
    // ---------------------------------------------------------------------------------------------

    /**
     * InsufficientFundsException = wyjątek „brak środków”. Niesprawdzany (extends RuntimeException).
     * Niesie dane: saldo, żądaną kwotę i niedobór — wywołujący może z nich skorzystać, nie parsując komunikatu.
     */
    static final class InsufficientFundsException extends RuntimeException {
        private static final long serialVersionUID = 1L;   // wyjątki są Serializable — bez tego pola -Xlint ostrzega
        private final long balance;
        private final long amount;

        InsufficientFundsException(long balance, long amount) {
            super("Brak środków: saldo " + balance + " zł, żądano " + amount + " zł");
            this.balance = balance;
            this.amount = amount;
        }

        long getBalance() {
            return balance;
        }

        long getAmount() {
            return amount;
        }

        /** getShortfall = pobierz niedobór: ile brakuje do wykonania operacji. */
        long getShortfall() {
            return amount - balance;
        }
    }

    /**
     * ValidationException = wyjątek walidacji. SPRAWDZANY (extends Exception): wywołujący MUSI zareagować na złe dane
     * z formularza. Niesie listę wszystkich błędów naraz — użytkownik poprawi wszystko za jednym razem.
     */
    static final class ValidationException extends Exception {
        private static final long serialVersionUID = 1L;
        private final List<String> errors;

        ValidationException(List<String> errors) {
            super("Błędy walidacji: " + errors);
            this.errors = List.copyOf(errors);          // kopia — wyjątek niezmienny
        }

        List<String> getErrors() {
            return errors;
        }
    }

    /** ShopException = wspólna baza wyjątków sklepu. catch (ShopException e) złapie wszystkie poniższe. */
    static class ShopException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        ShopException(String message) {
            super(message);
        }
    }

    /** OutOfStockException = brak towaru na stanie. */
    static final class OutOfStockException extends ShopException {
        private static final long serialVersionUID = 1L;
        private final String product;

        OutOfStockException(String product, int requested, int available) {
            super("Brak towaru: " + product + " (żądano " + requested + ", dostępne " + available + ")");
            this.product = product;
        }

        String getProduct() {
            return product;
        }
    }

    /** PaymentDeclinedException = płatność odrzucona. */
    static final class PaymentDeclinedException extends ShopException {
        private static final long serialVersionUID = 1L;

        PaymentDeclinedException(int cost, int budget) {
            super("Płatność odrzucona: koszt " + cost + " zł, limit " + budget + " zł");
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Kod „biznesowy” rzucający własne wyjątki
    // ---------------------------------------------------------------------------------------------

    /** withdraw = wypłać. Zwraca nowe saldo albo rzuca InsufficientFundsException. */
    static long withdraw(long balance, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Kwota musi być dodatnia: " + amount);   // gotowy wyjątek wystarcza
        }
        if (amount > balance) {
            throw new InsufficientFundsException(balance, amount);                     // własny — ma znaczenie i dane
        }
        return balance - amount;
    }

    /** validateUser = sprawdź dane użytkownika. Zbiera WSZYSTKIE błędy, potem rzuca jeden wyjątek z listą. */
    static void validateUser(String name, int age) throws ValidationException {
        List<String> errors = new ArrayList<>();
        if (name == null || name.isBlank()) {
            errors.add("imię jest puste");
        }
        if (age < 0) {
            errors.add("wiek nie może być ujemny");
        } else if (age > 150) {
            errors.add("wiek jest nierealny");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    /** STOCK = stan magazynu; PRICE = cena sztuki. */
    static final Map<String, Integer> STOCK = Map.of("kawa", 5, "herbata", 0);
    static final int PRICE = 20;

    /** placeOrder = złóż zamówienie. Może rzucić OutOfStockException, PaymentDeclinedException albo ShopException. */
    static String placeOrder(String product, int quantity, int budget) {
        Integer available = STOCK.get(product);
        if (available == null) {
            throw new ShopException("Nieznany produkt: " + product);
        }
        if (quantity > available) {
            throw new OutOfStockException(product, quantity, available);
        }
        int cost = quantity * PRICE;
        if (cost > budget) {
            throw new PaymentDeclinedException(cost, budget);
        }
        return "zamówiono " + quantity + " × " + product + " za " + cost + " zł";
    }

    public static void main(String[] args) {
        title("Exceptions05 — własne wyjątki");

        whyCustom();            // why custom = po co własny
        exceptionWithData();    // exception with data = wyjątek z danymi
        checkedCustom();        // checked custom = własny sprawdzany
        domainHierarchy();      // domain hierarchy = hierarchia wyjątków dziedziny
        rulesOfThumb();         // rules of thumb = reguły kciuka
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PO CO WŁASNY WYJĄTEK
    // =================================================================================================

    /**
     * 1. PRZED: IllegalStateException("brak środków") — trzeba by rozpoznawać błąd po TREŚCI komunikatu (kruche).
     * PO: własny typ — łapiesz dokładnie ten przypadek, a inne IllegalStateException lecą dalej.
     */
    static void whyCustom() {
        section("1. Własny typ = osobna obsługa");

        for (long amount : new long[]{30, 130}) {
            try {
                show("wypłata " + amount + " z 100", "nowe saldo " + withdraw(100, amount));
            } catch (InsufficientFundsException e) {
                show("wypłata " + amount + " z 100", e.getMessage());
            }
        }
        // WYNIK: wypłata 30 z 100 → nowe saldo 70
        // WYNIK: wypłata 130 z 100 → Brak środków: saldo 100 zł, żądano 130 zł

        // PUŁAPKA: if (e.getMessage().contains("brak środków")) — rozpoznawanie błędu po tekście psuje się przy
        //   każdej zmianie komunikatu (np. tłumaczeniu). Typ wyjątku jest stabilny.
    }

    // =================================================================================================
    // 2. WYJĄTEK Z DANYMI
    // =================================================================================================

    /** 2. Pola wyjątku pozwalają wywołującemu zareagować mądrze — np. zaproponować niższą kwotę. */
    static void exceptionWithData() {
        section("2. Wyjątek niesie dane");

        try {
            withdraw(250, 400);
        } catch (InsufficientFundsException e) {
            show("brakuje", e.getShortfall() + " zł");
            show("propozycja", "czy wypłacić " + e.getBalance() + " zł zamiast " + e.getAmount() + " zł?");
        }
        // WYNIK: brakuje → 150 zł
        // WYNIK: propozycja → czy wypłacić 250 zł zamiast 400 zł?
    }

    // =================================================================================================
    // 3. WŁASNY WYJĄTEK SPRAWDZANY
    // =================================================================================================

    /** 3. ValidationException jest sprawdzany — kompilator nie pozwoli zapomnieć o obsłudze złych danych formularza. */
    static void checkedCustom() {
        section("3. Własny wyjątek sprawdzany z listą błędów");

        try {
            validateUser("", -3);
            show("walidacja", "OK");
        } catch (ValidationException e) {
            show("liczba błędów", e.getErrors().size());
            show("błędy", e.getErrors());
        }
        // WYNIK: liczba błędów → 2
        // WYNIK: błędy → [imię jest puste, wiek nie może być ujemny]

        // DOBRA PRAKTYKA: przy walidacji formularza zbierz WSZYSTKIE błędy i zgłoś je razem — użytkownik nie musi
        //   poprawiać pól jedno po drugim, za każdym razem dowiadując się o kolejnym błędzie.
    }

    // =================================================================================================
    // 4. HIERARCHIA WYJĄTKÓW DZIEDZINY
    // =================================================================================================

    /**
     * 4. Wspólna baza (ShopException) pozwala złapać „każdy błąd sklepu” jednym catch, a szczegółowe typy —
     * obsłużyć wybrane przypadki osobno. Szczegółowy catch musi być PRZED ogólnym.
     */
    static void domainHierarchy() {
        section("4. Hierarchia: ShopException i podklasy");

        String[][] orders = {{"kawa", "2", "100"}, {"herbata", "1", "100"}, {"kawa", "5", "50"}, {"kakao", "1", "100"}};
        for (String[] o : orders) {
            try {
                show(o[0], placeOrder(o[0], Integer.parseInt(o[1]), Integer.parseInt(o[2])));
            } catch (OutOfStockException e) {
                show(o[0], "zamów później, powiadomimy o dostawie: " + e.getProduct());
            } catch (ShopException e) {
                show(o[0], e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
        // WYNIK: kawa → zamówiono 2 × kawa za 40 zł
        // WYNIK: herbata → zamów później, powiadomimy o dostawie: herbata
        // WYNIK: kawa → PaymentDeclinedException: Płatność odrzucona: koszt 100 zł, limit 50 zł
        // WYNIK: kakao → ShopException: Nieznany produkt: kakao
    }

    // =================================================================================================
    // 5. REGUŁY KCIUKA
    // =================================================================================================

    /** 5. Najważniejsze zasady projektowania własnych wyjątków. */
    static void rulesOfThumb() {
        section("5. Zasady projektowania");

        note("nazwa kończy się na Exception i mówi, CO się stało: InsufficientFundsException, a nie BankError");
        note("najpierw sprawdź gotowe: IllegalArgumentException, IllegalStateException — często wystarczą");
        note("komunikat z kontekstem (wartości!), pola final z danymi, konstruktor z cause (Exceptions06)");
        // WYNIK:    ℹ nazwa kończy się na Exception i mówi, CO się stało: InsufficientFundsException, a nie BankError
        // WYNIK:    ℹ najpierw sprawdź gotowe: IllegalArgumentException, IllegalStateException — często wystarczą
        // WYNIK:    ℹ komunikat z kontekstem (wartości!), pola final z danymi, konstruktor z cause (Exceptions06)

        // DOBRA PRAKTYKA: private static final long serialVersionUID = 1L; w każdej klasie wyjątku (Throwable jest
        //   Serializable; -Xlint:serial ostrzega o braku tego pola).
        // PUŁAPKA: rekord NIE może być wyjątkiem (rekord nie może dziedziczyć po klasie, a wyjątek musi po Throwable).
        // PUŁAPKA: nie twórz osobnej klasy wyjątku dla każdej metody — wyjątek ma odpowiadać SYTUACJI, którą ktoś
        //   chce osobno obsłużyć. Jeśli nikt nie złapie go inaczej niż ogólny — wystarczy gotowy typ.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • class XxxException extends RuntimeException (zwykle) albo extends Exception (sprawdzany — gdy wywołujący MUSI reagować).
     *   • Konstruktor: super(komunikat) (+ wersja z cause); pola final z danymi + gettery; serialVersionUID.
     *   • Własny typ zamiast rozpoznawania błędu po treści komunikatu.
     *   • Dane w wyjątku (niedobór, lista błędów) → mądra reakcja wywołującego.
     *   • Hierarchia domenowa: XxxException (baza) + podklasy; catch szczegółowy przed ogólnym.
     *   • Najpierw rozważ gotowe wyjątki; nie mnoż klas bez potrzeby. Rekord nie może być wyjątkiem.
     *
     * PYTANIA KONTROLNE:
     *   1. Kiedy warto utworzyć własny wyjątek zamiast użyć IllegalStateException?
     *   2. Co wypisze:
     *          try { withdraw(10, 25); } catch (InsufficientFundsException e) { System.out.println(e.getShortfall()); }
     *   3. ZNAJDŹ BŁĄD:
     *          try { placeOrder("kawa", 9, 500); }
     *          catch (ShopException e) { ... } catch (OutOfStockException e) { ... }
     *   4. Po czym dziedziczy wyjątek sprawdzany, a po czym niesprawdzany?
     *   5. Co wypisze:  placeOrder("herbata", 1, 100)  złapane jako ShopException i wypisane e.getMessage()?
     *   6. Dlaczego warto zebrać wszystkie błędy walidacji w jednym wyjątku?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: przelew 40 z 100", 60L, () -> exercise1(100, 40));
        Check.throwsException("ćw. 1b: przelew 140 z 100", InsufficientFundsException.class, () -> exercise1(100, 140));
        Check.equal("ćw. 2: komunikat o niedoborze", "Brakuje 30 zł", () -> exercise2(50, 80));
        Check.equal("ćw. 3a: dobre dane", "OK", () -> exercise3("Ola", 30));
        Check.equal("ćw. 3b: złe dane", "BŁĘDY: imię jest puste; wiek jest nierealny", () -> exercise3(" ", 200));
        Check.equal("ćw. 4: obsługa zamówień", "zamówiono|brak:herbata|sklep:PaymentDeclinedException",
                () -> exercise4("kawa", 1, 100) + "|" + exercise4("herbata", 1, 100) + "|" + exercise4("kawa", 3, 10));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", 60L, () -> solution1(100, 40));
        Check.throwsException("ćw. 1b (wzorzec)", InsufficientFundsException.class, () -> solution1(100, 140));
        Check.equal("ćw. 2 (wzorzec)", "Brakuje 30 zł", () -> solution2(50, 80));
        Check.equal("ćw. 3a (wzorzec)", "OK", () -> solution3("Ola", 30));
        Check.equal("ćw. 3b (wzorzec)", "BŁĘDY: imię jest puste; wiek jest nierealny", () -> solution3(" ", 200));
        Check.equal("ćw. 4 (wzorzec)", "zamówiono|brak:herbata|sklep:PaymentDeclinedException",
                () -> solution4("kawa", 1, 100) + "|" + solution4("herbata", 1, 100) + "|" + solution4("kawa", 3, 10));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): przelew = wypłata z konta nadawcy. Zwróć nowe saldo; gdy kwota przekracza saldo — rzuć
     * InsufficientFundsException (new InsufficientFundsException(balance, amount)). Nie używaj metody withdraw.
     */
    static long exercise1(long balance, long amount) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** ĆWICZENIE 2 (łatwe): wywołaj withdraw; złap InsufficientFundsException i zwróć "Brakuje N zł" (getShortfall). */
    static String exercise2(long balance, long amount) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 3 (średnie): wywołaj validateUser. Gdy przejdzie — zwróć "OK". Gdy rzuci ValidationException —
     * zwróć "BŁĘDY: " + błędy połączone "; " (String.join("; ", e.getErrors())).
     */
    static String exercise3(String name, int age) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): wywołaj placeOrder i zwróć: "zamówiono" (gdy się udało),
     * "brak:" + produkt (OutOfStockException, getProduct), "sklep:" + prosta nazwa klasy (każdy inny ShopException).
     */
    static String exercise4(String product, int quantity, int budget) {
        // TODO: twoje rozwiązanie
        return null;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static long solution1(long balance, long amount) {
        if (amount > balance) {
            throw new InsufficientFundsException(balance, amount);
        }
        return balance - amount;
    }

    static String solution2(long balance, long amount) {
        try {
            withdraw(balance, amount);
            return "OK";
        } catch (InsufficientFundsException e) {
            return "Brakuje " + e.getShortfall() + " zł";
        }
    }

    static String solution3(String name, int age) {
        try {
            validateUser(name, age);
            return "OK";
        } catch (ValidationException e) {
            return "BŁĘDY: " + String.join("; ", e.getErrors());
        }
    }

    static String solution4(String product, int quantity, int budget) {
        try {
            placeOrder(product, quantity, budget);
            return "zamówiono";
        } catch (OutOfStockException e) {
            return "brak:" + e.getProduct();
        } catch (ShopException e) {
            return "sklep:" + e.getClass().getSimpleName();
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Gdy ktoś będzie chciał obsłużyć tę sytuację OSOBNO (inaczej niż inne błędy) albo gdy wyjątek ma nieść dane
     *      (kwoty, identyfikatory), z których wywołujący skorzysta.
     *   2. „15” — żądano 25, saldo 10, niedobór 15.
     *   3. catch (ShopException e) jest przed catch (OutOfStockException e) — podklasa po nadklasie jest nieosiągalna,
     *      błąd kompilacji. Kolejność: najpierw OutOfStockException, potem ShopException.
     *   4. Sprawdzany: po Exception (ale nie po RuntimeException). Niesprawdzany: po RuntimeException.
     *   5. „Brak towaru: herbata (żądano 1, dostępne 0)” — OutOfStockException to też ShopException.
     *   6. Użytkownik dostaje pełną listę problemów za jednym razem i poprawia wszystko naraz, zamiast odkrywać błędy
     *      pojedynczo przy każdej kolejnej próbie.
     */
    // </editor-fold>
}
