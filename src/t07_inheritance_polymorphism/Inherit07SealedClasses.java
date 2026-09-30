package t07_inheritance_polymorphism;

import helpers.Check;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Klasy zapieczętowane (sealed) — zamknięty, wyliczalny zbiór podtypów (Java 17)
 *        (sealed = zapieczętowany; permits = zezwala na; non-sealed = odpieczętowany)
 *
 * W SKRÓCIE:
 *   Zwykłą klasę/interfejs może rozszerzyć KTOKOLWIEK, w KAŻDYM pakiecie — to "otwarty" zbiór podtypów.
 *   sealed (Java 17) pozwala z góry WYLICZYĆ, jakie klasy WOLNO z niego dziedziczyć (permits). Każda
 *   wymieniona podklasa musi być final, sealed albo non-sealed — nic nie "wycieknie" poza listę.
 *
 * ANALOGIA: zamknięta lista gości na przyjęciu.
 *   Zwykła klasa to przyjęcie z otwartym wejściem — wpuszczasz każdego, kto przyjdzie (dowolna podklasa).
 *   Klasa sealed to przyjęcie z listą gości przy drzwiach: dokładnie ci, kogo wymieniłeś (permits) — nikt
 *   inny się nie wciśnie. Dzięki temu wiesz z góry, KTO może się pojawić, i możesz się przygotować na każdego.
 *
 * JAK TO DZIAŁA:
 *   sealed interface Payment permits Card, Blik, BankTransfer { }
 *   record Card(String number) implements Payment { }
 *   record Blik(String code) implements Payment { }
 *   record BankTransfer(String iban) implements Payment { }
 *   // Payment MA dokładnie te trzy podklasy — żadna czwarta nie może dopisać się z zewnątrz.
 *
 * SŁÓWKA:
 *   sealed = zapieczętowany (wylicza dozwolone podklasy); permits = zezwala na (lista dozwolonych
 *   podklas); non-sealed = odpieczętowany (podklasa sealed, która z powrotem OTWIERA się na dowolne
 *   dalsze dziedziczenie); closed domain / closed set of variants = zamknięty zbiór wariantów.
 *
 * ZOBACZ TEŻ: t09_records/Records01Basics (rekordy — naturalny partner sealed przy modelowaniu wariantów),
 *             t07_inheritance_polymorphism/Inherit05Polymorphism (instanceof z wzorcem, dynamic dispatch),
 *             t23_modern_java/Modern05RecordsSealedPatterns (sealed + pattern matching w pełnym obrazie).
 * </pre>
 */
public class Inherit07SealedClasses {

    // ---------------------------------------------------------------------------------------------
    // Klasy/interfejsy przykładowe jako statyczne/zagnieżdżone składowe — lekcja ma być samodzielna.
    // ---------------------------------------------------------------------------------------------

    /** Payment = płatność. sealed interfejs: dokładnie TRZY dozwolone sposoby płatności, ani jeden więcej. */
    sealed interface Payment permits Card, Blik, BankTransfer {
    }

    /** Card = karta. record automatycznie jest final — spełnia wymóg "final, sealed albo non-sealed". */
    record Card(String number, boolean premium) implements Payment {
    }

    record Blik(String code) implements Payment {
    }

    record BankTransfer(String iban) implements Payment {
    }

    // ---------------------------------------------------------------------------------------------
    // Klasy (nie interfejsy) też mogą być sealed — tu zamknięta hierarchia kształtów
    // ---------------------------------------------------------------------------------------------

    /** Shape = kształt. sealed KLASA (nie interfejs) — podklasy muszą być final, sealed albo non-sealed. */
    abstract static sealed class Shape permits Circle, Rectangle, FreeformShape {
        abstract double area();
    }

    /** Circle jest final — koniec gałęzi dziedziczenia, zgodnie z wymogiem sealed. */
    static final class Circle extends Shape {
        final double r;

        Circle(double r) {
            this.r = r;
        }

        @Override
        double area() {
            return Math.PI * r * r;
        }
    }

    static final class Rectangle extends Shape {
        final double width;
        final double height;

        Rectangle(double width, double height) {
            this.width = width;
            this.height = height;
        }

        @Override
        double area() {
            return width * height;
        }
    }

    /**
     * FreeformShape jest non-sealed — świadomie "odpieczętowuje" tę jedną gałąź z powrotem, żeby
     * DOWOLNY kod (nawet w innym pakiecie) mógł po niej dziedziczyć dalej. Rzadkie, ale legalne.
     */
    static non-sealed class FreeformShape extends Shape {
        final double area;

        FreeformShape(double area) {
            this.area = area;
        }

        @Override
        double area() {
            return area;
        }
    }

    /** Ktoś spoza tej listy MÓGŁBY dziedziczyć po FreeformShape (bo jest non-sealed) — tu przykładowo. */
    static class CustomFreeformShape extends FreeformShape {
        CustomFreeformShape(double area) {
            super(area);
        }
    }

    public static void main(String[] args) {
        title("Inherit07 — klasy zapieczętowane (sealed, Java 17)");

        sealedBasics();          // sealed basics = podstawy sealed
        permitsRule();           // permits rule = reguła permits (final/sealed/non-sealed)
        sealedInterfaceRecords(); // sealed interface records = sealed interfejs + rekordy (Payment)
        feeCalculation();        // fee calculation = liczenie prowizji przez instanceof
        nonSealedEscape();       // non-sealed escape = odpieczętowana gałąź
        java21Preview();         // Java 21 preview = zapowiedź exhaustive switch
        whenToUseSealed();       // when to use sealed = kiedy używać sealed
        exercises();              // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PODSTAWY: sealed OGRANICZA, KTO MOŻE DZIEDZICZYĆ
    // =================================================================================================

    /** 1. Shape ma DOKŁADNIE trzy dozwolone podklasy — wymienione w permits, ani jednej więcej. */
    static void sealedBasics() {
        section("1. sealed ogranicza, kto może dziedziczyć");

        Shape c = new Circle(2.0);
        show("c instanceof Shape", c instanceof Shape);
        show("Shape.class jest sealed?", Shape.class.isSealed());
        show("liczba dozwolonych podklas Shape (getPermittedSubclasses)", Shape.class.getPermittedSubclasses().length);
        // WYNIK: c instanceof Shape → true
        // WYNIK: Shape.class jest sealed? → true
        // WYNIK: liczba dozwolonych podklas Shape (getPermittedSubclasses) → 3

        // class Triangle extends Shape { ... }
        //   // BŁĄD KOMPILACJI: class is not allowed to extend sealed class: Shape (as it is not listed
        //   // in its permits clause) — Triangle nie jest wymieniony w `permits Circle, Rectangle, FreeformShape`.

        // DOBRA PRAKTYKA: sealed ma sens, gdy z góry ZNASZ i KONTROLUJESZ WSZYSTKIE warianty — inaczej
        //   zwykłe (otwarte) dziedziczenie/interfejs jest właściwszym wyborem (patrz sekcja 7).
    }

    // =================================================================================================
    // 2. KAŻDA WYMIENIONA PODKLASA: final, sealed ALBO non-sealed
    // =================================================================================================

    /**
     * 2. Java wymusza, żeby KAŻDA podklasa wymieniona w permits sama zadeklarowała, co dzieje się DALEJ
     * z dziedziczeniem: final (koniec), sealed (dalej ograniczone) albo non-sealed (z powrotem otwarte).
     * Bez jednego z tych trzech słów kluczowych — błąd kompilacji.
     */
    static void permitsRule() {
        section("2. Każda podklasa: final, sealed albo non-sealed");

        show("Circle.class jest final?", java.lang.reflect.Modifier.isFinal(Circle.class.getModifiers()));
        show("FreeformShape.class jest sealed?", FreeformShape.class.isSealed());
        // WYNIK: Circle.class jest final? → true
        // WYNIK: FreeformShape.class jest sealed? → false

        // static class Rectangle extends Shape {   // BEZ final/sealed/non-sealed
        //     ...
        // }
        //   // BŁĄD KOMPILACJI: class Rectangle is not sealed, non-sealed, or final — trzeba dopisać
        //   // jedno z tych trzech słów kluczowych.

        note("record automatycznie JEST final (patrz Payment/Card/Blik/BankTransfer w sekcji 3) — dlatego "
                + "rekordy implementujące sealed interfejs zawsze spełniają ten wymóg bez dodatkowego słowa.");
        // WYNIK: ℹ record automatycznie JEST final (patrz Payment/Card/Blik/BankTransfer w sekcji 3) — dlatego rekordy implementujące sealed interfejs zawsze spełniają ten wymóg bez dodatkowego słowa.
    }

    // =================================================================================================
    // 3. sealed INTERFEJS + REKORDY: NATURALNA PARA
    // =================================================================================================

    /** 3. Payment + Card/Blik/BankTransfer — rekordy są zwięzłe i automatycznie final, idealne dla sealed. */
    static void sealedInterfaceRecords() {
        section("3. sealed interfejs + rekordy: Payment (Card, Blik, BankTransfer)");

        Payment p1 = new Card("4111", true);
        Payment p2 = new Blik("123456");
        Payment p3 = new BankTransfer("PL61109010140000071219812874");
        show("p1", p1);
        show("p2", p2);
        show("p3", p3);
        // WYNIK: p1 → Card[number=4111, premium=true]
        // WYNIK: p2 → Blik[code=123456]
        // WYNIK: p3 → BankTransfer[iban=PL61109010140000071219812874]

        // DOBRA PRAKTYKA: sealed interfejs + kilka rekordów to naturalny sposób modelowania "jednej z kilku
        //   MOŻLIWOŚCI" (suma typów) — każdy wariant niesie WŁASNE dane (Card ma numer, Blik ma kod, ...),
        //   a lista wariantów jest zamknięta i znana z góry.
    }

    // =================================================================================================
    // 4. LICZENIE PROWIZJI PRZEZ instanceof — ZAMKNIĘTY ZBIÓR = BEZPIECZNA OBSŁUGA
    // =================================================================================================

    /**
     * fee = oblicz prowizję. Łańcuch instanceof jest tu bezpieczniejszy niż zwykle (Inherit05, sekcja 7),
     * bo Payment jest sealed — wiemy z góry, że istnieją TYLKO te trzy warianty, więc IllegalStateException
     * na końcu jest czystą formalnością (kompilator sam by nas nie zmusił, ale sealed daje pewność, że lista jest kompletna).
     */
    static BigDecimal fee(Payment payment, BigDecimal amount) {
        if (payment instanceof Card card) {
            BigDecimal rate = card.premium() ? new BigDecimal("0.010") : new BigDecimal("0.020");
            return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        } else if (payment instanceof Blik) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else if (payment instanceof BankTransfer) {
            return new BigDecimal("1.00");
        }
        throw new IllegalStateException("Nieznany wariant Payment: " + payment.getClass());
    }

    static void feeCalculation() {
        section("4. Liczenie prowizji przez instanceof — Payment jest zamknięty (sealed)");

        BigDecimal amount = new BigDecimal("1000.00");
        show("fee(Card premium, 1000.00)", fee(new Card("4111", true), amount));
        show("fee(Card zwykła, 1000.00)", fee(new Card("4111", false), amount));
        show("fee(Blik, 1000.00)", fee(new Blik("123456"), amount));
        show("fee(BankTransfer, 1000.00)", fee(new BankTransfer("PL00"), amount));
        // WYNIK: fee(Card premium, 1000.00) → 10.00
        // WYNIK: fee(Card zwykła, 1000.00) → 20.00
        // WYNIK: fee(Blik, 1000.00) → 0.00
        // WYNIK: fee(BankTransfer, 1000.00) → 1.00

        // DOBRA PRAKTYKA: dzięki sealed, gdy ktoś kiedyś dopisze CZWARTY wariant Payment (np. Crypto),
        //   MUSI go najpierw dopisać do `permits` w Payment — a wtedy naturalnie zajrzy też tutaj i
        //   dopisze brakującą gałąź. Od Javy 21 kompilator wymusiłby to WPROST (patrz sekcja 6).
    }

    // =================================================================================================
    // 5. non-sealed: ŚWIADOME "ODPIECZĘTOWANIE" JEDNEJ GAŁĘZI
    // =================================================================================================

    /** 5. FreeformShape jest non-sealed — CustomFreeformShape mógłby powstać w zupełnie innym pakiecie. */
    static void nonSealedEscape() {
        section("5. non-sealed: świadome odpieczętowanie jednej gałęzi");

        Shape s = new CustomFreeformShape(42.0);
        show("s instanceof Shape", s instanceof Shape);
        show("s instanceof FreeformShape", s instanceof FreeformShape);
        show("s.area()", s.area());
        // WYNIK: s instanceof Shape → true
        // WYNIK: s instanceof FreeformShape → true
        // WYNIK: s.area() → 42.0

        // PUŁAPKA: non-sealed WYŁĄCZA korzyść sealed dla TEJ gałęzi — łańcuch instanceof/switch po Shape
        //   znowu może dostać "niespodziewany" podtyp (jak CustomFreeformShape), bo FreeformShape pozwala
        //   na dowolne dalsze dziedziczenie. Używaj non-sealed świadomie, tylko gdy naprawdę tego potrzebujesz.
        // DOBRA PRAKTYKA: im mniej gałęzi non-sealed w hierarchii sealed, tym więcej realnych korzyści
        //   (kompletność, bezpieczeństwo) faktycznie zostaje.
    }

    // =================================================================================================
    // 6. ZAPOWIEDŹ: WYCZERPUJĄCY switch (JAVA 21+)
    // =================================================================================================

    /**
     * 6. Od Javy 21 (poza zakresem tego kursu na Javie 17) switch nad sealed typem może być WYCZERPUJĄCY
     * BEZ default — kompilator SPRAWDZA, czy obsłużono WSZYSTKIE warianty z permits, i zgłasza błąd
     * kompilacji, jeśli czegoś brakuje. Na Javie 17 (bez preview) musimy nadal używać if/instanceof
     * (jak w sekcji 4) albo klasycznego switch z instanceof w guardach i jawnym default.
     */
    static void java21Preview() {
        section("6. Zapowiedź: wyczerpujący switch nad sealed typem (Java 21+)");

        // (Java 21+, poza zakresem tego kursu na Javie 17):
        // static BigDecimal feeJava21(Payment payment, BigDecimal amount) {
        //     return switch (payment) {
        //         case Card c when c.premium() -> amount.multiply(new BigDecimal("0.010"));
        //         case Card c                  -> amount.multiply(new BigDecimal("0.020"));
        //         case Blik b                  -> BigDecimal.ZERO;
        //         case BankTransfer t           -> new BigDecimal("1.00");
        //         // BRAK default — i to jest OK: kompilator WIE, że Card/Blik/BankTransfer to WSZYSTKIE
        //         // warianty Payment (permits), więc switch jest z definicji kompletny.
        //     };
        // }
        // Gdyby ktoś dopisał czwarty rekord do `permits Payment` i zapomniał o nowej gałęzi case tutaj,
        // Java 21 zgłosiłaby błąd KOMPILACJI: "the switch expression does not cover all input values".

        note("Na Javie 17 pattern matching w switch nad typami jest dopiero w PODGLĄDZIE (preview) — nie "
                + "używamy go w kodzie tego kursu. Pełny obraz (Java 21): t23_modern_java/Modern05RecordsSealedPatterns.");
        // WYNIK: ℹ Na Javie 17 pattern matching w switch nad typami jest dopiero w PODGLĄDZIE (preview) — nie używamy go w kodzie tego kursu. Pełny obraz (Java 21): t23_modern_java/Modern05RecordsSealedPatterns.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • sealed X permits A, B, C — X ma DOKŁADNIE te podtypy, żaden inny nie może dziedziczyć/implementować.
     *   • każdy z A, B, C musi być final (koniec), sealed (dalej ograniczone) albo non-sealed (z powrotem otwarte).
     *   • record implementujący sealed interfejs automatycznie spełnia wymóg final.
     *   • sealed + rekordy to naturalny sposób modelowania "jednej z kilku możliwości" (suma typów).
     *   • non-sealed świadomie wyłącza korzyści sealed dla jednej gałęzi — używaj oszczędnie.
     *   • Java 21+ (poza tym kursem): switch nad sealed typem może być wyczerpujący bez default —
     *     kompilator pilnuje kompletności za Ciebie.
     *   • sealed opłaca się dla ZAMKNIĘTYCH domen (znana z góry, stabilna lista wariantów) — nie dla
     *     API, które inni mają rozszerzać.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się sealed od zwykłego (otwartego) dziedziczenia pod kątem tego, KTO może dziedziczyć?
     *   2. Co muszą zadeklarować WSZYSTKIE podklasy wymienione w permits, i dlaczego?
     *   3. ZNAJDŹ BŁĄD:
     *          sealed interface Shape permits Circle { }
     *          class Circle implements Shape { }   // brak final/sealed/non-sealed!
     *   4. Dlaczego record implementujący sealed interfejs nie musi jawnie pisać "final"?
     *   5. Co daje non-sealed i dlaczego trzeba go używać świadomie/oszczędnie?
     *   6. Dlaczego switch nad sealed typem może być wyczerpujący bez default dopiero od Javy 21, a nie już w 17?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // 7. KIEDY UŻYWAĆ sealed
    // =================================================================================================

    static void whenToUseSealed() {
        section("7. Kiedy używać sealed: zamknięta domena wariantów");

        note("sealed pasuje, gdy WYLICZYĆ warianty da się z góry i są one częścią Twojego modelu domenowego "
                + "(sposoby płatności, wyniki operacji: Sukces/Błąd, węzły AST, kształty w grze planszowej).");
        // WYNIK: ℹ sealed pasuje, gdy WYLICZYĆ warianty da się z góry i są one częścią Twojego modelu domenowego (sposoby płatności, wyniki operacji: Sukces/Błąd, węzły AST, kształty w grze planszowej).

        note("sealed NIE pasuje, gdy piszesz BIBLIOTEKĘ/API, które inni mają swobodnie rozszerzać (np. "
                + "własne implementacje Comparator czy Runnable) — tam otwarty interfejs jest celem, nie problemem.");
        // WYNIK: ℹ sealed NIE pasuje, gdy piszesz BIBLIOTEKĘ/API, które inni mają swobodnie rozszerzać (np. własne implementacje Comparator czy Runnable) — tam otwarty interfejs jest celem, nie problemem.

        // DOBRA PRAKTYKA: enum (t08_enums) i sealed rozwiązują PODOBNY problem (zamknięty zbiór wariantów),
        //   ale enum daje stałe BEZ własnych, różnych danych/typów na wariant, a sealed (zwłaszcza z rekordami)
        //   pozwala każdemu wariantowi nieść WŁASNY, inny zestaw danych (Card ma numer, BankTransfer ma IBAN).
    }

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");

        List<Payment> payments = List.of(
                new Card("1", true), new Blik("000000"), new BankTransfer("PL01"), new Card("2", false));

        Check.equal("ćw. 1: liczba płatności kartą", 2, () -> countCards(payments));
        Check.equal("ćw. 2: suma prowizji dla wszystkich płatności (1000.00 każda)", new BigDecimal("31.00"),
                () -> totalFees(payments, new BigDecimal("1000.00")));
        Check.equal("ćw. 3: opis pierwszej płatności BLIK-iem (albo \"brak\")", "123456", () -> firstBlikCode(payments));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 2, () -> solution1(payments));
        Check.equal("ćw. 2 (wzorzec)", new BigDecimal("31.00"), () -> solution2(payments, new BigDecimal("1000.00")));
        Check.equal("ćw. 3 (wzorzec)", "123456", () -> solution3(List.of(new Blik("123456"), new Card("1", true))));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 3 OK, ✘ 0 BŁĄD
    }

    /** ĆWICZENIE 1 (łatwe): policz, ile elementów listy to płatność kartą (Card). */
    static int countCards(List<Payment> payments) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (średnie): zsumuj fee(p, amount) dla każdej płatności z listy (ta sama kwota amount
     * dla każdej z nich). Podpowiedź: pętla for-each + BigDecimal.add.
     */
    static BigDecimal totalFees(List<Payment> payments, BigDecimal amount) {
        // TODO: twoje rozwiązanie
        return BigDecimal.ZERO;
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): zwróć kod PIERWSZEJ płatności typu Blik na liście (instanceof z wzorcem),
     * a jeśli żadnej nie ma — zwróć "brak". Zakładamy, że lista może być pusta albo nie zawierać Blika.
     */
    static String firstBlikCode(List<Payment> payments) {
        // TODO: twoje rozwiązanie
        return null;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(List<Payment> payments) {
        int count = 0;
        for (Payment p : payments) {
            if (p instanceof Card) {
                count++;
            }
        }
        return count;
    }

    static BigDecimal solution2(List<Payment> payments, BigDecimal amount) {
        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (Payment p : payments) {
            total = total.add(fee(p, amount));
        }
        return total;
    }

    static String solution3(List<Payment> payments) {
        for (Payment p : payments) {
            if (p instanceof Blik b) {
                return b.code();
            }
        }
        return "brak";
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Zwykłe dziedziczenie jest otwarte — dowolna klasa, w dowolnym pakiecie, może dopisać się jako
     *      podklasa/implementacja. sealed WYLICZA z góry (permits), kto dokładnie może dziedziczyć —
     *      lista jest zamknięta i znana już w momencie kompilacji.
     *   2. Muszą zadeklarować final (koniec dziedziczenia), sealed (dalej ograniczone, z własnym permits)
     *      albo non-sealed (z powrotem w pełni otwarte). Bez jednego z tych trzech słów — błąd kompilacji.
     *      Wymóg istnieje po to, żeby NIGDY nie było niejasne, czy dana gałąź jest nadal "zamknięta".
     *   3. Circle implementuje sealed interfejs Shape, ale nie deklaruje final/sealed/non-sealed — błąd
     *      kompilacji: "class Circle is not sealed, non-sealed, or final". Trzeba dopisać np. "final class Circle...".
     *   4. Bo record jest NIEJAWNIE final zawsze (nie da się po nim dziedziczyć, z definicji tego mechanizmu
     *      języka) — więc automatycznie spełnia wymóg sealed, bez potrzeby pisania tego jawnie.
     *   5. non-sealed pozwala JEDNEJ konkretnej gałęzi hierarchii sealed wrócić do pełnej otwartości —
     *      dowolna klasa może dalej po niej dziedziczyć. Trzeba używać świadomie, bo każda gałąź
     *      non-sealed to miejsce, w którym korzyść "znam WSZYSTKIE warianty" przestaje obowiązywać.
     *   6. Bo pattern matching dla switch nad typami (z wymuszaniem kompletności dla sealed) stał się
     *      stabilną częścią języka dopiero w Javie 21 — w Javie 17 mechanizm ten jest dopiero w statusie
     *      podglądu (preview) i nie jest częścią tego kursu.
     */
    // </editor-fold>
}
