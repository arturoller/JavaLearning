package t15_numbers;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Order;
import helpers.model.OrderLine;
import helpers.model.OrderStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Money — własny obiekt-wartość (value object) na pieniądze, zbudowany na BigDecimal
 *        (money = pieniądze; value object = obiekt-wartość; currency = waluta; installment = rata)
 *
 * W SKRÓCIE:
 *   Goły BigDecimal nie wie, że jest kwotą: nie zna waluty, skali ani zasad zaokrąglania. Każdy programista
 *   pilnuje ich osobno — i ktoś w końcu zapomni. Rekord Money zamyka te reguły w JEDNYM miejscu:
 *   zawsze 2 miejsca po przecinku, zawsze ta sama zasada zaokrąglania, zakaz dodawania złotych do euro.
 *
 * ANALOGIA: banknot vs kartka z liczbą.
 *   • Kartka z napisem „100” może znaczyć 100 zł, 100 euro albo 100 jabłek — trzeba pamiętać, co autor miał na myśli.
 *   • Banknot 100 zł „wie”, czym jest: ma walutę, nominał i nikt nie „doda” go do banknotu 100 euro
 *     tak po prostu — kasjer najpierw każe wymienić walutę (u nas: wyjątek).
 *
 * JAK TO DZIAŁA:
 *   record Money(BigDecimal amount, Currency currency)
 *     konstruktor kompaktowy:  sprawdź null → sprawdź walutę → amount.setScale(2, HALF_UP)  (normalizacja)
 *     Money.of("19.99", "PLN"), Money.zero(PLN)       — metody fabryczne (factory methods)
 *     add / subtract            — tylko ta sama waluta, inaczej IllegalArgumentException
 *     times(int), multiply(BigDecimal), percent(int), discount(int) — wynik zaokrągla konstruktor
 *     isGreaterThan(other)      — compareTo w środku
 *     split(n)                  — raty bez gubienia groszy (reszta trafia do pierwszych rat)
 *   Dzięki normalizacji skali equals rekordu działa „po ludzku”: of("10", PLN).equals(of("10.00", PLN)) → true.
 *
 * SŁÓWKA:
 *   amount = kwota; currency = waluta; value object = obiekt-wartość; factory method = metoda fabryczna;
 *   compact constructor = konstruktor kompaktowy; normalize = ujednolicić; times = razy; percent = procent;
 *   discount = rabat; gross = brutto; net = netto; VAT = podatek od towarów i usług; split = podziel;
 *   installment = rata; remainder = reszta; greater than = większy niż; mismatch = niezgodność;
 *   fraction digits = cyfry po przecinku; move point right = przesuń przecinek w prawo; display = wyświetl.
 *
 * ZOBACZ TEŻ: t15_numbers/Numbers01BigDecimal (setScale, RoundingMode, compareTo),
 *             t06_oop_basics/Oop09ValueObjects (idea obiektu-wartości), t09_records/Records02Constructors
 *             (konstruktor kompaktowy), t16_streams/Streams15BigDecimalMoney (sumowanie kwot strumieniem).
 * </pre>
 */
public class Numbers02MoneyValueObject {

    static final Currency PLN = Currency.getInstance("PLN");         // getInstance = pobierz egzemplarz
    static final Currency EUR = Currency.getInstance("EUR");
    static final Locale PL = Locale.forLanguageTag("pl-PL");          // polskie ustawienia regionalne
    /** GROSS_FACTOR = mnożnik brutto dla VAT 23%. */
    static final BigDecimal GROSS_FACTOR = new BigDecimal("1.23");

    public static void main(String[] args) {
        title("Numbers02 — Money: obiekt-wartość na pieniądze");

        rawBigDecimalProblems();    // raw BigDecimal problems = kłopoty z gołym BigDecimal
        designingMoney();           // designing money = projektujemy Money
        addAndSubtract();           // add and subtract = dodawanie i odejmowanie
        timesAndPercent();          // times and percent = mnożenie i procenty
        vatGrossNet();              // VAT gross net = VAT: brutto i netto
        comparingAndEquals();       // comparing and equals = porównywanie i równość
        splittingInstallments();    // splitting installments = podział na raty
        summingList();              // summing list = sumowanie listy
        displayFormatting();        // display formatting = formatowanie do wyświetlenia
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // MONEY — OBIEKT-WARTOŚĆ (czytaj razem z sekcją 2)
    // =================================================================================================

    /**
     * Money = kwota w konkretnej walucie. Niezmienny rekord; każda operacja zwraca NOWY obiekt.
     * <p>
     * Normalizacja: konstruktor kompaktowy ZAWSZE ustawia skalę 2 trybem HALF_UP. Dlaczego HALF_UP, a nie
     * HALF_EVEN? Polska ustawa o VAT każe zaokrąglać do pełnych groszy tak, że końcówki poniżej 0,5 grosza
     * się pomija, a od 0,5 grosza w górę zaokrągla do 1 grosza — to właśnie HALF_UP. HALF_EVEN (bankierski)
     * lepiej sprawdza się przy sumowaniu milionów zaokrągleń (brak systematycznego błędu w jedną stronę),
     * ale na fakturze klient oczekuje „szkolnego” zaokrąglenia.
     */
    record Money(BigDecimal amount, Currency currency) {

        static final int SCALE = 2;                                   // grosze / centy
        static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

        /** Konstruktor kompaktowy: walidacja + normalizacja skali. Pola przypisuje Java po jego zakończeniu. */
        Money {
            Objects.requireNonNull(amount, "amount (kwota) nie może być null");
            Objects.requireNonNull(currency, "currency (waluta) nie może być null");
            if (currency.getDefaultFractionDigits() != SCALE) {       // getDefaultFractionDigits = domyślna liczba cyfr po przecinku
                throw new IllegalArgumentException("Nieobsługiwana waluta " + currency + ": ma "
                        + currency.getDefaultFractionDigits() + " miejsc po przecinku, a Money obsługuje " + SCALE);
            }
            amount = amount.setScale(SCALE, ROUNDING);                // 10 → 10.00, 0.125 → 0.13
        }

        /** of = z (napisu i kodu waluty), np. {@code Money.of("19.99", "PLN")}. */
        static Money of(String amount, String currencyCode) {
            return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
        }

        /** zero = zero w danej walucie — punkt startowy sumowania. */
        static Money zero(Currency currency) {
            return new Money(BigDecimal.ZERO, currency);
        }

        Money add(Money other) {
            requireSameCurrency(other);
            return new Money(amount.add(other.amount), currency);
        }

        Money subtract(Money other) {
            requireSameCurrency(other);
            return new Money(amount.subtract(other.amount), currency);
        }

        /** times = razy (np. cena × ilość). Mnożenie przez int nie wymaga zaokrąglania. */
        Money times(int quantity) {
            return new Money(amount.multiply(BigDecimal.valueOf(quantity)), currency);
        }

        /** multiply = pomnóż przez dowolny współczynnik; zaokrągla konstruktor (HALF_UP do groszy). */
        Money multiply(BigDecimal factor) {
            return new Money(amount.multiply(factor), currency);
        }

        /** percent = ile to jest p procent tej kwoty. */
        Money percent(int p) {
            return multiply(BigDecimal.valueOf(p).movePointLeft(2));  // 23 → 0.23 (przesuń przecinek w lewo)
        }

        /** discount = kwota po rabacie p procent. */
        Money discount(int p) {
            return subtract(percent(p));
        }

        boolean isGreaterThan(Money other) {
            requireSameCurrency(other);
            return amount.compareTo(other.amount) > 0;                // compareTo, nie equals!
        }

        /**
         * split = podziel na {@code parts} rat tak, żeby suma rat była DOKŁADNIE równa kwocie.
         * Liczymy w groszach (long): reszta z dzielenia to liczba groszy, które dostaną pierwsze raty (po 1 gr).
         */
        List<Money> split(int parts) {
            if (parts <= 0) {
                throw new IllegalArgumentException("Liczba rat musi być dodatnia: " + parts);
            }
            if (amount.signum() < 0) {
                throw new IllegalArgumentException("Nie dzielimy kwot ujemnych: " + this);
            }
            long totalCents = amount.movePointRight(SCALE).longValueExact();   // 100.00 → 10000 groszy
            long base = totalCents / parts;                           // 10000 / 3 = 3333
            long remainder = totalCents % parts;                      // 10000 % 3 = 1 → 1 grosz do rozdania
            List<Money> result = new ArrayList<>();
            for (int i = 0; i < parts; i++) {
                long cents = base + (i < remainder ? 1 : 0);
                result.add(new Money(BigDecimal.valueOf(cents, SCALE), currency));
            }
            return result;
        }

        /** display = wyświetl zgodnie z ustawieniami regionalnymi (spacje twarde zamienione na zwykłe). */
        String display(Locale locale) {
            NumberFormat format = NumberFormat.getCurrencyInstance(locale);   // format walutowy
            format.setCurrency(currency);
            return format.format(amount).replace('\u00A0', ' ').replace('\u202F', ' ');
        }

        /** requireSameCurrency = wymagaj tej samej waluty. */
        private void requireSameCurrency(Money other) {
            if (!currency.equals(other.currency)) {
                throw new IllegalArgumentException("Różne waluty: " + currency + " i " + other.currency);
            }
        }

        @Override
        public String toString() {
            return amount.toPlainString() + " " + currency.getCurrencyCode();   // np. 19.99 PLN
        }
    }

    // =================================================================================================
    // 1. KŁOPOTY Z GOŁYM BigDecimal (PRZED)
    // =================================================================================================

    /**
     * 1. Zanim zaprojektujemy Money, zobaczmy, co idzie źle, gdy kwoty są zwykłymi BigDecimal:
     * skala zależy od tego, skąd przyszła liczba; waluta istnieje tylko w nazwie zmiennej; zaokrąglanie
     * każdy robi po swojemu.
     */
    static void rawBigDecimalProblems() {
        section("1. PRZED: kłopoty z gołym BigDecimal");

        BigDecimal fromForm = new BigDecimal("10");                   // z formularza: „10”
        BigDecimal fromDatabase = new BigDecimal("10.00");            // z bazy danych: „10.00”
        show("10 equals 10.00", fromForm.equals(fromDatabase));
        // WYNIK: 10 equals 10.00 → false    ← ta sama kwota, a equals mówi „różne” (Numbers01BigDecimal, sekcja 7)

        BigDecimal pricePln = new BigDecimal("100.00");               // w złotych
        BigDecimal priceEur = new BigDecimal("50.00");                // w euro
        show("100 zł + 50 € (goły BigDecimal)", pricePln.add(priceEur));
        // WYNIK: 100 zł + 50 € (goły BigDecimal) → 150.00    ← „złoto-euro”: kompilator nie ma szans tego wykryć

        BigDecimal third = new BigDecimal("100.00").divide(new BigDecimal("3"), 5, RoundingMode.HALF_EVEN);
        show("ktoś podzielił po swojemu", third);
        // WYNIK: ktoś podzielił po swojemu → 33.33333    ← 5 miejsc po przecinku w kwocie złotówkowej?

        // PUŁAPKA: każda z tych decyzji (skala, waluta, zaokrąglenie) jest rozsiana po całym programie.
        // DOBRA PRAKTYKA: zamknij je w jednym typie — obiekcie-wartości (t06_oop_basics/Oop09ValueObjects).
    }

    // =================================================================================================
    // 2. PROJEKTUJEMY Money: normalizacja i walidacja
    // =================================================================================================

    /**
     * 2. Rekord Money (kod powyżej sekcji 1). Konstruktor kompaktowy (compact constructor) nie ma listy
     * parametrów — działa na parametrach rekordu PRZED przypisaniem ich do pól. Możemy więc je sprawdzić
     * i poprawić (znormalizować).
     */
    static void designingMoney() {
        section("2. Projektujemy Money: normalizacja skali i walidacja");

        show("Money.of(\"10\", \"PLN\")", Money.of("10", "PLN"));
        show("Money.of(\"0.125\", \"PLN\")", Money.of("0.125", "PLN"));
        show("Money.of(\"0.124\", \"EUR\")", Money.of("0.124", "EUR"));
        show("Money.zero(PLN)", Money.zero(PLN));
        // WYNIK: Money.of("10", "PLN") → 10.00 PLN    ← skala uzupełniona do 2
        // WYNIK: Money.of("0.125", "PLN") → 0.13 PLN    ← HALF_UP: pół grosza w górę
        // WYNIK: Money.of("0.124", "EUR") → 0.12 EUR
        // WYNIK: Money.zero(PLN) → 0.00 PLN

        expectThrows("new Money(null, PLN)", () -> new Money(null, PLN));
        // WYNIK: ✔ new Money(null, PLN) → rzucono NullPointerException: amount (kwota) nie może być null
        expectThrows("Money.of(\"100\", \"JPY\")", () -> Money.of("100", "JPY"));
        // WYNIK: ✔ Money.of("100", "JPY") → rzucono IllegalArgumentException: Nieobsługiwana waluta JPY: ma 0 miejsc po przecinku, a Money obsługuje 2
        // Jen japoński nie ma „groszy”. Nasza prosta klasa uczciwie odmawia, zamiast liczyć źle.

        // DOBRA PRAKTYKA: walidacja w konstruktorze = nie da się stworzyć NIEPOPRAWNEGO obiektu Money.
        //   Każda metoda (add, times...) tworzy wynik przez konstruktor, więc też jest znormalizowana.
    }

    // =================================================================================================
    // 3. DODAWANIE I ODEJMOWANIE — TA SAMA WALUTA
    // =================================================================================================

    /** 3. add i subtract sprawdzają walutę. Złote + euro → wyjątek zamiast bzdurnej sumy. */
    static void addAndSubtract() {
        section("3. add i subtract — tylko ta sama waluta");

        Money laptop = Money.of("5499.99", "PLN");
        Money headphones = Money.of("349.90", "PLN");
        Money voucher = Money.of("200", "PLN");                       // voucher = bon rabatowy
        Money total = laptop.add(headphones).subtract(voucher);
        show("laptop + słuchawki − bon", total);
        // WYNIK: laptop + słuchawki − bon → 5649.89 PLN

        Money eur = Money.of("50", "EUR");
        expectThrows("100 PLN + 50 EUR", () -> Money.of("100", "PLN").add(eur));
        // WYNIK: ✔ 100 PLN + 50 EUR → rzucono IllegalArgumentException: Różne waluty: PLN i EUR

        // Money jest niezmienny, tak jak BigDecimal — wynik trzeba przypisać:
        Money balance = Money.zero(PLN);
        balance.add(laptop);                                          // wynik wyrzucony!
        show("saldo po balance.add(...) bez przypisania", balance);
        // WYNIK: saldo po balance.add(...) bez przypisania → 0.00 PLN

        // DOBRA PRAKTYKA: przeliczanie walut to OSOBNA operacja (kurs, data kursu, zaokrąglenie) —
        //   nigdy „ukryte” w add. Lepiej wyjątek niż cicha pomyłka o kilka tysięcy złotych.
    }

    // =================================================================================================
    // 4. MNOŻENIE, PROCENTY, RABATY
    // =================================================================================================

    /** 4. times(int) — cena × ilość. percent(p) i discount(p) — zaokrąglenie robi konstruktor (HALF_UP). */
    static void timesAndPercent() {
        section("4. times, percent, discount");

        Money tshirt = Money.of("49.99", "PLN");
        show("T-shirt × 3", tshirt.times(3));
        // WYNIK: T-shirt × 3 → 149.97 PLN

        Money book = Money.of("129.00", "PLN");
        show("23% z 129.00", book.percent(23));
        show("129.00 po rabacie 15%", book.discount(15));
        show("49.99 po rabacie 10%", tshirt.discount(10));
        // WYNIK: 23% z 129.00 → 29.67 PLN    ← 29.67 dokładnie
        // WYNIK: 129.00 po rabacie 15% → 109.65 PLN
        // WYNIK: 49.99 po rabacie 10% → 44.99 PLN    ← rabat 4.999 → 5.00 (HALF_UP), 49.99 − 5.00

        // Kolejność zaokrągleń ma znaczenie! Rabat liczony od sumy vs suma rabatów od sztuk:
        show("rabat 10% od (49.99 × 3)", tshirt.times(3).discount(10));
        show("(rabat 10% od 49.99) × 3", tshirt.discount(10).times(3));
        // WYNIK: rabat 10% od (49.99 × 3) → 134.97 PLN
        // WYNIK: (rabat 10% od 49.99) × 3 → 134.97 PLN
        // Tu wyszło tak samo, ale nie zawsze tak jest — zobacz VAT w sekcji 5.
    }

    // =================================================================================================
    // 5. VAT: BRUTTO ↔ NETTO I PROBLEM ZAOKRĄGLEŃ
    // =================================================================================================

    /**
     * 5. brutto = netto × 1.23 (zaokrąglone do grosza); netto = brutto / 1.23 (zaokrąglone do grosza).
     * Każde zaokrąglenie to utrata informacji — przeliczenie „tam i z powrotem” nie zawsze wraca do punktu wyjścia,
     * a suma zaokrąglonych pozycji może różnić się od zaokrąglonej sumy.
     */
    static void vatGrossNet() {
        section("5. VAT: brutto ↔ netto i problem zaokrągleń");

        Money net = Money.of("100.00", "PLN");
        show("netto 100.00 → brutto", grossFromNet(net));
        show("brutto 123.00 → netto", netFromGross(Money.of("123.00", "PLN")));
        // WYNIK: netto 100.00 → brutto → 123.00 PLN
        // WYNIK: brutto 123.00 → netto → 100.00 PLN

        // Tam i z powrotem: brutto 10.03 → netto → brutto
        Money gross = Money.of("10.03", "PLN");
        Money back = netFromGross(gross);
        show("brutto 10.03 → netto", back);
        show("   → z powrotem brutto", grossFromNet(back));
        // WYNIK: brutto 10.03 → netto → 8.15 PLN    ← 10.03 / 1.23 = 8.1544... → 8.15
        // WYNIK: → z powrotem brutto → 10.02 PLN    ← 8.15 × 1.23 = 10.0245 → 10.02. Grosz zniknął!
        // Dlaczego? 1 grosz netto to 1.23 grosza brutto — niektórych kwot brutto nie da się uzyskać z ŻADNEGO netto.

        // Suma pozycji vs pozycja sumy: 3 sztuki po 0.99 netto.
        Money lineNet = Money.of("0.99", "PLN");
        Money grossPerLine = grossFromNet(lineNet).times(3);         // zaokrąglamy każdą sztukę
        Money grossOfTotal = grossFromNet(lineNet.times(3));          // zaokrąglamy raz, sumę
        show("brutto liczone od sztuki × 3", grossPerLine);
        show("brutto liczone od sumy", grossOfTotal);
        // WYNIK: brutto liczone od sztuki × 3 → 3.66 PLN    ← 0.99 × 1.23 = 1.2177 → 1.22; × 3
        // WYNIK: brutto liczone od sumy → 3.65 PLN    ← 2.97 × 1.23 = 3.6531 → 3.65

        // PUŁAPKA: oba wyniki są „poprawne”, ale różne. Faktura musi być spójna: przepisy pozwalają liczyć VAT
        //   od sumy wartości netto danej stawki ALBO od pozycji — trzeba wybrać jedną metodę i stosować ją wszędzie.
        // DOBRA PRAKTYKA: przechowuj kwotę ŹRÓDŁOWĄ (np. netto z cennika) i z niej licz pozostałe — nie przeliczaj
        //   w kółko brutto → netto → brutto.
    }

    /** grossFromNet = brutto z netto (× 1.23, zaokrąglenie w konstruktorze Money). */
    static Money grossFromNet(Money net) {
        return net.multiply(GROSS_FACTOR);
    }

    /** netFromGross = netto z brutto (÷ 1.23 do 2 miejsc, HALF_UP). */
    static Money netFromGross(Money gross) {
        return new Money(gross.amount().divide(GROSS_FACTOR, Money.SCALE, Money.ROUNDING), gross.currency());
    }

    // =================================================================================================
    // 6. PORÓWNYWANIE I equals SPÓJNE DZIĘKI NORMALIZACJI
    // =================================================================================================

    /**
     * 6. Rekord generuje equals porównujący wszystkie pola — także BigDecimal przez JEGO equals (ze skalą!).
     * Normalizacja skali w konstruktorze sprawia, że 10 i 10.00 dają identyczne obiekty, więc equals, hashCode
     * i HashSet działają zgodnie z intuicją.
     */
    static void comparingAndEquals() {
        section("6. Porównywanie: isGreaterThan i equals spójne dzięki normalizacji");

        Money a = Money.of("10", "PLN");
        Money b = Money.of("10.00", "PLN");
        show("Money 10 equals Money 10.00", a.equals(b));
        show("hashCode równe?", a.hashCode() == b.hashCode());
        // WYNIK: Money 10 equals Money 10.00 → true    ← w sekcji 1 goły BigDecimal dawał false
        // WYNIK: hashCode równe? → true

        Set<Money> unique = new HashSet<>(List.of(a, b, Money.of("10.0", "PLN")));
        show("HashSet z 10, 10.00, 10.0 — liczba elementów", unique.size());
        // WYNIK: HashSet z 10, 10.00, 10.0 — liczba elementów → 1

        show("10 PLN equals 10 EUR", a.equals(Money.of("10", "EUR")));
        // WYNIK: 10 PLN equals 10 EUR → false    ← inna waluta = inna wartość

        Money budget = Money.of("1000", "PLN");
        Money monitor = Money.of("1299.00", "PLN");
        show("monitor droższy niż budżet?", monitor.isGreaterThan(budget));
        // WYNIK: monitor droższy niż budżet? → true
        expectThrows("10 PLN > 10 EUR ?", () -> a.isGreaterThan(Money.of("10", "EUR")));
        // WYNIK: ✔ 10 PLN > 10 EUR ? → rzucono IllegalArgumentException: Różne waluty: PLN i EUR

        // DOBRA PRAKTYKA: typ z normalizacją można bezpiecznie trzymać w HashSet/HashMap i porównywać przez equals.
        //   Metody typu isGreaterThan czytają się lepiej niż a.amount().compareTo(b.amount()) > 0.
    }

    // =================================================================================================
    // 7. RATY BEZ GUBIENIA GROSZY
    // =================================================================================================

    /**
     * 7. 100 zł na 3 raty to NIE 3 × 33.33 (= 99.99 — grosz zniknął). Poprawnie: 33.34 + 33.33 + 33.33.
     * Metoda split liczy w groszach: część całkowita dla każdego, reszta (po 1 gr) dla pierwszych rat.
     */
    static void splittingInstallments() {
        section("7. Raty bez gubienia groszy");

        Money price = Money.of("100.00", "PLN");

        // PRZED: każda rata = kwota / 3 zaokrąglona
        Money naive = new Money(price.amount().divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP), PLN);
        show("naiwna rata", naive);
        show("naiwna suma 3 rat", naive.times(3));
        // WYNIK: naiwna rata → 33.33 PLN
        // WYNIK: naiwna suma 3 rat → 99.99 PLN    ← sklep stracił grosz (przy milionie zamówień — 10 000 zł)

        // PO: split
        List<Money> installments = price.split(3);
        show("split(3)", installments);
        show("suma rat", sum(installments, PLN));
        // WYNIK: split(3) → [33.34 PLN, 33.33 PLN, 33.33 PLN]
        // WYNIK: suma rat → 100.00 PLN    ← zgadza się co do grosza

        show("0.05 PLN na 3 raty", Money.of("0.05", "PLN").split(3));
        show("2999.00 PLN na 7 rat", Money.of("2999.00", "PLN").split(7));
        // WYNIK: 0.05 PLN na 3 raty → [0.02 PLN, 0.02 PLN, 0.01 PLN]
        // WYNIK: 2999.00 PLN na 7 rat → [428.43 PLN, 428.43 PLN, 428.43 PLN, 428.43 PLN, 428.43 PLN, 428.43 PLN, 428.42 PLN]

        expectThrows("split(0)", () -> price.split(0));
        // WYNIK: ✔ split(0) → rzucono IllegalArgumentException: Liczba rat musi być dodatnia: 0

        // DOBRA PRAKTYKA: przy każdym podziale kwoty (raty, dzielenie rachunku, rozksięgowanie kosztów)
        //   sprawdź, czy SUMA części równa się całości. To prosty test, który łapie zgubione grosze.
    }

    // =================================================================================================
    // 8. SUMOWANIE LISTY KWOT
    // =================================================================================================

    /** 8. Suma listy: start od Money.zero(waluta) i add w pętli. W t16 zrobisz to jednym reduce. */
    static void summingList() {
        section("8. Sumowanie listy kwot — pętla (i zapowiedź strumieni)");

        Money revenue = Money.zero(PLN);                              // revenue = przychód
        int counted = 0;
        for (Order order : SampleData.orders()) {
            if (order.status() != OrderStatus.ANULOWANE) {            // anulowanych nie liczymy
                revenue = revenue.add(new Money(order.total(), PLN));
                counted++;
            }
        }
        show("przychód z " + counted + " zamówień (bez anulowanych)", revenue);
        // WYNIK: przychód z 9 zamówień (bez anulowanych) → 12949.96 PLN

        // Zapowiedź t16 — to samo strumieniem (reduce = zredukuj do jednej wartości):
        Money streamRevenue = SampleData.orders().stream()
                .filter(o -> o.status() != OrderStatus.ANULOWANE)
                .map(o -> new Money(o.total(), PLN))
                .reduce(Money.zero(PLN), Money::add);
        show("to samo strumieniem", streamRevenue);
        // WYNIK: to samo strumieniem → 12949.96 PLN

        // DOBRA PRAKTYKA: suma zaczyna się od zera W TEJ SAMEJ WALUCIE. Money.zero(EUR) + kwoty w PLN → wyjątek,
        //   i bardzo dobrze — błąd wychodzi od razu.
    }

    /** sum = suma listy kwot w danej walucie (pusta lista → zero). */
    static Money sum(List<Money> amounts, Currency currency) {
        Money total = Money.zero(currency);
        for (Money m : amounts) {
            total = total.add(m);
        }
        return total;
    }

    // =================================================================================================
    // 9. WYŚWIETLANIE
    // =================================================================================================

    /**
     * 9. toString Money daje format techniczny („19.99 PLN”) — dobry do logów i testów.
     * Dla ludzi — display(locale): separator dziesiętny, grupowanie tysięcy i symbol waluty zależne od kraju.
     * Szczegóły NumberFormat: t15_numbers/Numbers04FormattingParsing.
     */
    static void displayFormatting() {
        section("9. Wyświetlanie: toString techniczny, display dla ludzi");

        Money price = Money.of("1234.5", "PLN");
        show("toString()", price);
        show("display(pl-PL)", price.display(PL));
        show("display(pl-PL) w euro", Money.of("1234.5", "EUR").display(PL));
        show("display(en-US)", price.display(Locale.US));
        // WYNIK: toString() → 1234.50 PLN
        // WYNIK: display(pl-PL) → 1 234,50 zł
        // WYNIK: display(pl-PL) w euro → 1 234,50 €
        // WYNIK: display(en-US) → PLN1,234.50

        // PUŁAPKA: polski NumberFormat wstawia TWARDE spacje (U+00A0 lub U+202F), a nie zwykłe. Na ekranie
        //   wyglądają tak samo, ale "1 234,50 zł".equals(...) da false. Dlatego display zamienia je na ' '.
        // DOBRA PRAKTYKA: obliczenia — zawsze na Money/BigDecimal; formatowanie — dopiero przy wyświetlaniu,
        //   na samym końcu. Nigdy nie licz na napisach typu "1 234,50 zł".

        note("Dlaczego Money > goły BigDecimal: waluta w typie, zawsze skala 2, jedna zasada zaokrąglania, "
                + "spójne equals, czytelne API (times, discount, split).");
        // WYNIK: ℹ Dlaczego Money > goły BigDecimal: waluta w typie, zawsze skala 2, jedna zasada zaokrąglania, spójne equals, czytelne API (times, discount, split).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   record Money(BigDecimal amount, Currency currency) — niezmienny obiekt-wartość
     *   Konstruktor kompaktowy:  requireNonNull → walidacja waluty → amount.setScale(2, HALF_UP)
     *   Fabryki:                 Money.of("19.99", "PLN"), Money.zero(PLN)
     *   Działania:               add / subtract (ta sama waluta!), times(int), multiply(BigDecimal),
     *                            percent(p), discount(p) — wynik zawsze przez konstruktor (normalizacja)
     *   Porównanie:              isGreaterThan (compareTo w środku); equals działa dzięki stałej skali
     *   Raty:                    licz w groszach (long): base = total / n, reszta po 1 gr do pierwszych rat
     *   VAT:                     przechowuj kwotę źródłową; tam i z powrotem gubi grosze;
     *                            suma zaokrągleń ≠ zaokrąglenie sumy — wybierz jedną metodę
     *   Wyświetlanie:            toString techniczny; display(Locale) dla ludzi (twarde spacje → ' ')
     *   HALF_UP — faktury (tak liczy ustawa o VAT); HALF_EVEN — wielkie sumy statystyczne/bankowe
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie trzy decyzje „zamyka” w sobie typ Money, a które przy gołym BigDecimal są rozsiane po kodzie?
     *   2. Dlaczego bez normalizacji skali equals rekordu Money byłby zdradliwy?
     *   3. Co wypisze:  System.out.println(Money.of("0.005", "PLN") + " / " + Money.of("0.004", "PLN"));  ?
     *   4. ZNAJDŹ BŁĄD:
     *          Money perPerson = new Money(bill.amount().divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP), PLN);
     *          // każda z 4 osób płaci perPerson — rachunek 100.02 zł
     *   5. ZNAJDŹ BŁĄD:
     *          Money total = Money.zero(EUR);
     *          for (Order o : orders) { total = total.add(new Money(o.total(), PLN)); }
     *   6. Co wypisze:  System.out.println(Money.of("10.00", "PLN").split(3));  ?
     *   7. Dlaczego brutto 10.03 → netto → brutto daje 10.02? Jak uniknąć takiego problemu?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** Uruchamia ćwiczenia: najpierw TWOJE rozwiązania (✘ dopóki nie uzupełnisz), potem wzorcowe (✔). */
    static void exercises() {
        Money tshirt = Money.of("19.99", "PLN");
        Money shipping = Money.of("15", "PLN");
        Money budget = Money.of("100.00", "PLN");
        Order zam001 = SampleData.orders().get(0);
        List<Money> netLines = List.of(Money.of("0.99", "PLN"), Money.of("0.99", "PLN"), Money.of("0.99", "PLN"));
        List<Money> expected5a = List.of(Money.of("33.33", "PLN"), Money.of("33.33", "PLN"), Money.of("33.34", "PLN"));
        List<Money> expected5b = List.of(Money.of("0.01", "PLN"), Money.of("0.02", "PLN"), Money.of("0.02", "PLN"));

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: 3 × 19.99 + wysyłka 15", Money.of("74.97", "PLN"), () -> exercise1(tshirt, 3, shipping));
        Check.equal("ćw. 2a: stać mnie na 99.99?", true, () -> exercise2(budget, Money.of("99.99", "PLN")));
        Check.equal("ćw. 2b: stać mnie na 100.00?", true, () -> exercise2(budget, Money.of("100.00", "PLN")));
        Check.equal("ćw. 2c: stać mnie na 100.01?", false, () -> exercise2(budget, Money.of("100.01", "PLN")));
        Check.equal("ćw. 3: suma pozycji ZAM-001", Money.of("6199.79", "PLN"), () -> exercise3(zam001));
        Check.equal("ćw. 4: brutto od pozycji 3 × 0.99", Money.of("3.66", "PLN"), () -> exercise4(netLines));
        Check.equal("ćw. 5a: 100.00 na 3 raty (reszta na koniec)", expected5a,
                () -> exercise5(Money.of("100.00", "PLN"), 3));
        Check.equal("ćw. 5b: 0.05 na 3 raty (reszta na koniec)", expected5b, () -> exercise5(Money.of("0.05", "PLN"), 3));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", Money.of("74.97", "PLN"), () -> solution1(tshirt, 3, shipping));
        Check.equal("ćw. 2a (wzorzec)", true, () -> solution2(budget, Money.of("99.99", "PLN")));
        Check.equal("ćw. 2b (wzorzec)", true, () -> solution2(budget, Money.of("100.00", "PLN")));
        Check.equal("ćw. 2c (wzorzec)", false, () -> solution2(budget, Money.of("100.01", "PLN")));
        Check.equal("ćw. 3 (wzorzec)", Money.of("6199.79", "PLN"), () -> solution3(zam001));
        Check.equal("ćw. 4 (wzorzec)", Money.of("3.66", "PLN"), () -> solution4(netLines));
        Check.equal("ćw. 5a (wzorzec)", expected5a, () -> solution5(Money.of("100.00", "PLN"), 3));
        Check.equal("ćw. 5b (wzorzec)", expected5b, () -> solution5(Money.of("0.05", "PLN"), 3));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 8 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć wartość koszyka: cena × ilość + koszt wysyłki.
     * Podpowiedź: price.times(quantity).add(shipping).
     */
    static Money exercise1(Money price, int quantity, Money shipping) {
        // TODO: twoje rozwiązanie
        return Money.zero(PLN);
    }

    /**
     * ĆWICZENIE 2 (łatwe): czy budżet wystarczy na zakup? true, gdy cena ≤ budżet.
     * Podpowiedź: „cena ≤ budżet” to to samo co „cena NIE jest większa od budżetu” — isGreaterThan i negacja (!).
     */
    static boolean exercise2(Money budget, Money price) {
        // TODO: twoje rozwiązanie
        return false;
    }

    /**
     * ĆWICZENIE 3 (średnie, łączy Money z modelem zamówień): policz wartość zamówienia jako Money w PLN,
     * sumując pozycje (order.lines()). Każda pozycja: {@code line.product().price()} × {@code line.quantity()}.
     * Podpowiedź: Money.zero(PLN), pętla po liniach, {@code new Money(line.product().price(), PLN).times(line.quantity())}.
     */
    static Money exercise3(Order order) {
        // TODO: twoje rozwiązanie
        return Money.zero(PLN);
    }

    /**
     * ĆWICZENIE 4 (średnie): policz sumę brutto faktury metodą „od pozycji”: każdą pozycję netto przelicz na
     * brutto (zaokrąglenie do grosza), a potem zsumuj. Dla 3 × 0.99 netto wynik to 3.66 (a nie 3.65).
     * Podpowiedź: pętla + grossFromNet(line) + add. Zacznij od Money.zero(PLN).
     */
    static Money exercise4(List<Money> netLines) {
        // TODO: twoje rozwiązanie
        return Money.zero(PLN);
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): podziel kwotę na raty jak split, ale dodatkowe grosze mają trafić do OSTATNICH rat:
     * 100.00 na 3 → [33.33, 33.33, 33.34]; 0.05 na 3 → [0.01, 0.02, 0.02]. Suma rat = kwota.
     * Podpowiedź: grosze = {@code total.amount().movePointRight(2).longValueExact()}; reszta = grosze % parts;
     * rata i dostaje +1 gr, gdy {@code i >= parts - reszta}. Kwota z groszy: {@code BigDecimal.valueOf(grosze, 2)}.
     */
    static List<Money> exercise5(Money total, int parts) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static Money solution1(Money price, int quantity, Money shipping) {
        return price.times(quantity).add(shipping);
    }

    static boolean solution2(Money budget, Money price) {
        return !price.isGreaterThan(budget);
    }

    static Money solution3(Order order) {
        Money total = Money.zero(PLN);
        for (OrderLine line : order.lines()) {
            total = total.add(new Money(line.product().price(), PLN).times(line.quantity()));
        }
        return total;
    }

    static Money solution4(List<Money> netLines) {
        Money total = Money.zero(PLN);
        for (Money line : netLines) {
            total = total.add(grossFromNet(line));                    // zaokrąglenie na każdej pozycji
        }
        return total;
    }

    static List<Money> solution5(Money total, int parts) {
        long cents = total.amount().movePointRight(Money.SCALE).longValueExact();
        long base = cents / parts;
        long remainder = cents % parts;
        List<Money> result = new ArrayList<>();
        for (int i = 0; i < parts; i++) {
            long c = base + (i >= parts - remainder ? 1 : 0);        // ostatnie „remainder” rat dostają +1 gr
            result.add(new Money(BigDecimal.valueOf(c, Money.SCALE), total.currency()));
        }
        return result;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Walutę (nie dodasz PLN do EUR), skalę (zawsze 2) i zasadę zaokrąglania (zawsze HALF_UP). Do tego
     *      walidację (brak null) — wszystko w konstruktorze, w jednym miejscu.
     *   2. Rekord porównuje pola przez equals, a BigDecimal.equals uwzględnia skalę: Money(10) i Money(10.00)
     *      byłyby „różne”, HashSet trzymałby duplikaty, a testy porównujące kwoty losowo by padały.
     *   3. „0.01 PLN / 0.00 PLN” — konstruktor zaokrągla HALF_UP: pół grosza w górę, 0.4 grosza w dół.
     *   4. 100.02 / 4 = 25.005 → 25.01; 4 × 25.01 = 100.04 — klienci zapłacą o 2 grosze za dużo (albo, przy innej
     *      kwocie, za mało). Poprawnie: bill.split(4) → [25.01, 25.01, 25.00, 25.00], suma 100.02.
     *   5. Suma startuje od zera w EUR, a dodawane są kwoty w PLN — pierwsze add rzuci IllegalArgumentException
     *      „Różne waluty: EUR i PLN”. Poprawnie: Money.zero(PLN).
     *   6. „[3.34 PLN, 3.33 PLN, 3.33 PLN]” — 1000 groszy / 3 = 333 reszty 1, więc pierwsza rata dostaje +1 gr.
     *   7. 10.03 / 1.23 = 8.1544 → 8.15, a 8.15 × 1.23 = 10.0245 → 10.02. Grosz netto „waży” 1.23 grosza brutto,
     *      więc nie każdą kwotę brutto da się uzyskać z netto. Rozwiązanie: przechowuj kwotę źródłową (np. netto
     *      z cennika) i licz z niej drugą raz, zamiast przeliczać tam i z powrotem.
     */
    // </editor-fold>
}
