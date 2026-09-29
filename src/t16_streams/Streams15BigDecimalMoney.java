package t16_streams;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Category;
import helpers.model.Order;
import helpers.model.OrderLine;
import helpers.model.OrderStatus;
import helpers.model.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Pieniądze w strumieniach — BigDecimal: sumy, średnie, grupowanie, zaokrąglanie
 *        (big decimal = duża liczba dziesiętna; money = pieniądze)
 *
 * W SKRÓCIE:
 *   Kwoty liczymy w BigDecimal, nie w double. Strumienie nie mają „sumBigDecimal()”, więc sumujemy
 *   przez reduce(ZERO, BigDecimal::add), dzielimy ZAWSZE ze skalą i trybem zaokrąglania,
 *   a porównujemy przez compareTo. Na końcu parsujemy mini-CSV i liczymy z niego sumy.
 *
 * ANALOGIA: double to waga łazienkowa — pokazuje „mniej więcej 70 kg” i nikt się nie czepia.
 *   BigDecimal to waga jubilera — każdy miligram się liczy. Rachunków nie waży się łazienkową wagą.
 *
 * JAK TO DZIAŁA:
 *   suma           stream.map(...).reduce(BigDecimal.ZERO, BigDecimal::add)
 *   linia          cena.multiply(BigDecimal.valueOf(ilość))
 *   średnia        suma.divide(BigDecimal.valueOf(liczba), 2, RoundingMode.HALF_UP)
 *   w grupach      groupingBy(klucz, fabryka, reducing(ZERO, mapper, BigDecimal::add))
 *                  toMap(klucz, mapper, BigDecimal::add, fabryka)
 *   max / min      max(Comparator.comparing(Product::price))   (porównuje przez compareTo)
 *   równość        a.compareTo(b) == 0      (NIE equals — equals patrzy też na skalę: 2.5 ≠ 2.50)
 *
 * SŁÓWKA:
 *   add = dodaj; multiply = pomnóż; divide = podziel; scale = skala (liczba cyfr po przecinku);
 *   rounding mode = tryb zaokrąglania; half up = połówki w górę (jak w szkole); set scale = ustaw skalę;
 *   revenue = przychód; total = suma (razem); line = pozycja (wiersz) zamówienia; drift = dryf (pełzający błąd);
 *   year month = rok i miesiąc; skipped = pominięty; parse = przetwórz tekst na wartość
 *
 * ZOBACZ TEŻ: t15_numbers/Numbers01BigDecimal (podstawy BigDecimal, skala, tryby zaokrąglania),
 *   t15_numbers/Numbers02MoneyValueObject (klasa Money), t16_streams/Streams07Reduce (reduce z identity),
 *   t16_streams/Streams11GroupingBy (reducing w grupach), t18_io_files/Io04Csv (pełne CSV z plików)
 * </pre>
 */
public class Streams15BigDecimalMoney {

    /** Stałe poza lambdami — tworzone raz, a nie dla każdego elementu. */
    private static final BigDecimal VAT_RATE = new BigDecimal("0.23");

    /** Jedna pozycja z CSV sprzedaży (typ pomocniczy tej lekcji). */
    record Sale(LocalDate date, String sku, String product, Category category, int quantity, BigDecimal price) {
        BigDecimal total() {
            return price.multiply(BigDecimal.valueOf(quantity));
        }
    }

    public static void main(String[] args) {
        title("Streams15 — pieniądze w strumieniach (BigDecimal)");

        whyNotDouble();          // why not double = dlaczego nie double
        summing();               // summing = sumowanie
        lineTotals();            // line totals = wartości pozycji
        averagePrice();          // average price = średnia cena
        sumsPerCategory();       // sums per category = sumy w kategoriach
        revenueReports();        // revenue reports = raporty przychodu
        maxMinByPrice();         // max/min by price = największy/najmniejszy według ceny
        compareToForSums();      // compareTo for sums = compareTo dla sum
        roundingPerLineVsTotal(); // rounding per line vs total = zaokrąglanie pozycji kontra sumy
        csvTotals();             // csv totals = sumy z CSV
        exercises();             // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. DLACZEGO NIE double
    // =================================================================================================

    /**
     * 1. double zapisuje liczby w systemie dwójkowym. 0.1 nie ma w nim dokładnego zapisu (jak 1/3
     * w dziesiętnym), więc każde dodawanie dokłada maleńki błąd — i te błędy się sumują (dryf).
     */
    static void whyNotDouble() {
        section("1. Dlaczego pieniądze nie w double");

        // Dziesięć razy po 10 groszy. generate = generuj; limit = ogranicz; Double::sum = suma double
        double tenTimesDime = Stream.generate(() -> 0.1).limit(10).reduce(0.0, Double::sum);
        show("10 × 0.1 (double, reduce)", tenTimesDime);
        // WYNIK: 10 × 0.1 (double, reduce) → 0.9999999999999999

        show("0.1 + 0.2 (double)", Stream.of(0.1, 0.2).reduce(0.0, Double::sum));
        // WYNIK: 0.1 + 0.2 (double) → 0.30000000000000004

        // To samo w BigDecimal — dokładnie. valueOf(0.1) bierze „ładny” zapis dziesiętny "0.1".
        BigDecimal dime = new BigDecimal("0.10");
        BigDecimal tenDimes = Stream.generate(() -> dime).limit(10).reduce(BigDecimal.ZERO, BigDecimal::add);
        show("10 × 0.10 (BigDecimal)", tenDimes);
        // WYNIK: 10 × 0.10 (BigDecimal) → 1.00

        // Ciekawostka: DoubleStream.sum() stosuje sprytne sumowanie z kompensacją błędu (algorytm Kahana)
        // i tu „trafia” w 1.0. Nie daj się zwieść: to łata na objaw, a nie dokładna arytmetyka.
        show("10 × 0.1 (DoubleStream.sum)", DoubleStream.generate(() -> 0.1).limit(10).sum());
        // WYNIK: 10 × 0.1 (DoubleStream.sum) → 1.0

        // PUŁAPKA: new BigDecimal(0.1) — z double! Kopiuje CAŁY dwójkowy błąd. Używaj tekstu albo valueOf.
        show("new BigDecimal(0.1)", new BigDecimal(0.1));
        // WYNIK: new BigDecimal(0.1) → 0.1000000000000000055511151231257827021181583404541015625
        show("new BigDecimal(\"0.1\")", new BigDecimal("0.1"));
        // WYNIK: new BigDecimal("0.1") → 0.1
    }

    // =================================================================================================
    // 2. SUMOWANIE: reduce(ZERO, add)
    // =================================================================================================

    /**
     * 2. Nie ma {@code mapToBigDecimal().sum()}. Sumujemy przez {@code reduce(BigDecimal.ZERO, BigDecimal::add)}:
     * ZERO to wartość startowa (identity), add to operacja łącząca dwie kwoty.
     */
    static void summing() {
        section("2. Sumowanie: reduce(ZERO, BigDecimal::add)");

        List<Product> products = SampleData.products();

        // ZERO = zero; add = dodaj. Suma cen jednostkowych wszystkich produktów.
        BigDecimal priceSum = products.stream()
                .map(Product::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        show("suma cen", priceSum);
        // WYNIK: suma cen → 13106.36

        // stockValue = wartość magazynu produktu (cena × sztuki)
        BigDecimal warehouse = products.stream()
                .map(Product::stockValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        show("wartość całego magazynu", warehouse);
        // WYNIK: wartość całego magazynu → 80759.43

        // reduce BEZ identity → Optional (Streams14). Dla pieniędzy prawie zawsze chcesz ZERO dla pustego.
        Optional<BigDecimal> maybe = Stream.<BigDecimal>empty().reduce(BigDecimal::add);
        show("pusta suma bez identity", maybe);
        // WYNIK: pusta suma bez identity → Optional.empty
        show("pusta suma z ZERO", Stream.<BigDecimal>empty().reduce(BigDecimal.ZERO, BigDecimal::add));
        // WYNIK: pusta suma z ZERO → 0

        // PRZED (pętla): BigDecimal sum = ZERO; for (...) sum = sum.add(p.price());
        // PUŁAPKA: sum.add(x) bez przypisania NIC nie zmienia — BigDecimal jest niezmienny (immutable).
        BigDecimal lost = BigDecimal.ZERO;
        for (Product p : products.subList(0, 2)) {
            lost.add(p.price());   // wynik wyrzucony do kosza!
        }
        show("BŁĘDNIE: add bez przypisania", lost);
        // WYNIK: BŁĘDNIE: add bez przypisania → 0
    }

    // =================================================================================================
    // 3. WARTOŚCI POZYCJI ZAMÓWIENIA
    // =================================================================================================

    /**
     * 3. Wartość pozycji = cena × ilość. Wartość zamówienia = suma pozycji. Przychód = suma wszystkich
     * pozycji z wielu zamówień → {@code flatMap(o -> o.lines().stream())}.
     */
    static void lineTotals() {
        section("3. Wartości pozycji zamówienia");

        Order first = SampleData.orders().get(0);

        // multiply = pomnóż; valueOf = zamień int na BigDecimal
        List<String> lines = first.lines().stream()
                .map(l -> l + " = " + l.product().price().multiply(BigDecimal.valueOf(l.quantity())))
                .toList();
        showEach("pozycje " + first.id(), lines);
        // WYNIK: pozycje ZAM-001 (liczba elementów: 2):
        // WYNIK: • Laptop Pro 14 x1 = 5499.99
        // WYNIK: • Słuchawki BT x2 = 699.80

        BigDecimal sumOfLines = first.lines().stream()
                .map(OrderLine::total)   // total = wartość pozycji (to samo mnożenie, gotowe w modelu)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        show("suma pozycji " + first.id(), sumOfLines);
        // WYNIK: suma pozycji ZAM-001 → 6199.79
        show("zgodna z Order.total()?", sumOfLines.compareTo(first.total()) == 0);
        // WYNIK: zgodna z Order.total()? → true

        // Przychód ze wszystkich zamówień: spłaszcz pozycje (Streams04) i zsumuj.
        BigDecimal allLines = SampleData.orders().stream()
                .flatMap(o -> o.lines().stream())
                .map(OrderLine::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        show("wszystkie zamówienia", allLines);
        // WYNIK: wszystkie zamówienia → 15948.96

        // Anulowane się nie liczą. isFinal nie wystarczy (DOSTARCZONE też jest finalne) — filtrujemy wprost.
        BigDecimal revenue = SampleData.orders().stream()
                .filter(o -> o.status() != OrderStatus.ANULOWANE)
                .map(Order::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        show("przychód bez anulowanych", revenue);
        // WYNIK: przychód bez anulowanych → 12949.96
    }

    // =================================================================================================
    // 4. ŚREDNIA CENA — dzielenie ze skalą
    // =================================================================================================

    /**
     * 4. Średnia = suma / liczba. BigDecimal chce wynik DOKŁADNY — jeśli dzielenie „nie kończy się”
     * (np. 1/3), rzuca wyjątek. Dlatego dzielimy z podaną skalą i trybem zaokrąglania.
     */
    static void averagePrice() {
        section("4. Średnia cena — dzielenie ze skalą");

        List<Product> products = SampleData.products();
        BigDecimal sum = products.stream().map(Product::price).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal count = BigDecimal.valueOf(products.size());

        // divide(dzielnik, skala, tryb) — 2 miejsca po przecinku, połówki w górę (HALF_UP = jak w szkole)
        BigDecimal average = sum.divide(count, 2, RoundingMode.HALF_UP);
        show("średnia cena (13106.36 / 14)", average);
        // WYNIK: średnia cena (13106.36 / 14) → 936.17

        // PUŁAPKA: divide bez skali → wynik 936.168571428571... nie ma końca → ArithmeticException.
        expectThrows("divide bez skali", () -> sum.divide(count));
        // WYNIK: ✔ divide bez skali → rzucono ArithmeticException: Non-terminating decimal expansion; no exact representable decimal result.

        // Bez skali działa tylko wtedy, gdy wynik jest skończony — a tego zwykle nie wiesz z góry.
        show("10 / 4 bez skali", BigDecimal.TEN.divide(BigDecimal.valueOf(4)));
        // WYNIK: 10 / 4 bez skali → 2.5

        // Inne tryby (np. HALF_EVEN — „bankowe”, połówka do parzystej): t15_numbers/Numbers01BigDecimal.
        // DOBRA PRAKTYKA: dzielenie kwot ZAWSZE ze skalą i trybem. Pusta lista? Sprawdź liczbę PRZED
        // dzieleniem (dzielenie przez ZERO też rzuca ArithmeticException).
    }

    // =================================================================================================
    // 5. SUMY W KATEGORIACH — reducing i toMap
    // =================================================================================================

    /**
     * 5. Dwie drogi do mapy „kategoria → suma”: {@code groupingBy + reducing} albo {@code toMap} z funkcją
     * łączącą {@code BigDecimal::add}. Klucze enum → EnumMap albo TreeMap (stała kolejność).
     */
    static void sumsPerCategory() {
        section("5. Sumy w kategoriach — reducing i toMap");

        List<Product> products = SampleData.products();

        // Droga 1: groupingBy + reducing(identity, mapper, op). EnumMap = mapa dla kluczy enum.
        Map<Category, BigDecimal> viaReducing = products.stream()
                .collect(Collectors.groupingBy(Product::category, () -> new EnumMap<>(Category.class),
                        Collectors.reducing(BigDecimal.ZERO, Product::stockValue, BigDecimal::add)));
        showEach("wartość magazynu (groupingBy + reducing)", viaReducing);
        // WYNIK: wartość magazynu (groupingBy + reducing) (liczba kluczy: 5):
        // WYNIK: • ELEKTRONIKA → 52443.43
        // WYNIK: • SPOZYWCZE → 10045.80
        // WYNIK: • KSIAZKI → 2643.00
        // WYNIK: • ODZIEZ → 9507.20
        // WYNIK: • DOM → 6120.00

        // Droga 2: toMap(klucz, wartość, jak połączyć przy kolizji, fabryka mapy) — często krócej.
        Map<Category, BigDecimal> viaToMap = products.stream()
                .collect(Collectors.toMap(Product::category, Product::stockValue, BigDecimal::add, TreeMap::new));
        show("wartość magazynu (toMap)", viaToMap);
        // WYNIK: wartość magazynu (toMap) → {ELEKTRONIKA=52443.43, SPOZYWCZE=10045.80, KSIAZKI=2643.00, ODZIEZ=9507.20, DOM=6120.00}

        // Mapy porównuje się przez equals wartości — tu skale są identyczne, więc true.
        show("obie drogi równe?", viaReducing.equals(viaToMap));
        // WYNIK: obie drogi równe? → true

        // PUŁAPKA: HashMap z kluczem enum wypisuje się w kolejności zależnej od uruchomienia.
        // Do raportów: EnumMap (kolejność deklaracji) albo TreeMap (porządek naturalny = też deklaracji).
    }

    // =================================================================================================
    // 6. PRZYCHÓD NA KLIENTA I NA MIESIĄC (YearMonth)
    // =================================================================================================

    /**
     * 6. Ten sam wzór, inny klucz: nazwa klienta albo miesiąc. {@code YearMonth.from(data)} obcina datę
     * do „rok-miesiąc” — idealny klucz raportu miesięcznego (TreeMap sortuje chronologicznie).
     */
    static void revenueReports() {
        section("6. Przychód na klienta i na miesiąc (YearMonth)");

        List<Order> paid = SampleData.orders().stream()
                .filter(o -> o.status() != OrderStatus.ANULOWANE)
                .toList();

        Map<String, BigDecimal> perCustomer = paid.stream()
                .collect(Collectors.groupingBy(o -> o.customer().name(), TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Order::total, BigDecimal::add)));
        showEach("przychód na klienta", perCustomer);
        // WYNIK: przychód na klienta (liczba kluczy: 5):
        // WYNIK: • Jan Kowalski → 6669.79
        // WYNIK: • Marek Król → 295.45
        // WYNIK: • Maria Nowak → 619.77
        // WYNIK: • Ola Pawlak → 608.97
        // WYNIK: • Zofia Krawczyk → 4755.98
        // Adama Mazura nie ma — jego jedyne zamówienie było anulowane (filter przed grupowaniem, Streams13).

        // YearMonth = rok i miesiąc (bez dnia); from = utwórz z (daty)
        Map<YearMonth, BigDecimal> perMonth = paid.stream()
                .collect(Collectors.groupingBy(o -> YearMonth.from(o.date()), TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Order::total, BigDecimal::add)));
        showEach("przychód na miesiąc", perMonth);
        // WYNIK: przychód na miesiąc (liczba kluczy: 4):
        // WYNIK: • 2026-01 → 6469.66
        // WYNIK: • 2026-02 → 2335.98
        // WYNIK: • 2026-03 → 3981.32
        // WYNIK: • 2026-04 → 163.00

        // Kontrola krzyżowa: suma miesięcy musi się zgadzać z przychodem z sekcji 3.
        BigDecimal check = perMonth.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        show("suma miesięcy", check);
        // WYNIK: suma miesięcy → 12949.96

        // DOBRA PRAKTYKA: w raportach finansowych zawsze rób taką kontrolę sum — tanio wyłapuje błędy filtrów.
    }

    // =================================================================================================
    // 7. MAX / MIN PO CENIE
    // =================================================================================================

    /**
     * 7. BigDecimal jest Comparable, więc {@code Comparator.comparing(Product::price)} porównuje przez
     * compareTo — bez względu na skalę. Nie ma potrzeby zamieniać na double.
     */
    static void maxMinByPrice() {
        section("7. Max / min po cenie");

        List<Product> products = SampleData.products();

        show("najdroższy", products.stream().max(Comparator.comparing(Product::price)).map(Product::name).orElse("-"));
        // WYNIK: najdroższy → Laptop Pro 14
        show("najtańszy", products.stream().min(Comparator.comparing(Product::price)).map(Product::name).orElse("-"));
        // WYNIK: najtańszy → Czekolada gorzka

        show("największe zamówienie", SampleData.orders().stream().max(Comparator.comparing(Order::total)));
        // WYNIK: największe zamówienie → Optional[ZAM-001 [Jan Kowalski, 2026-01-05, DOSTARCZONE, 6199.79 zł]]

        // PUŁAPKA: comparingDouble(p -> p.price().doubleValue()) też „działa”, ale przepuszcza kwotę przez
        // niedokładny double. Przy bardzo bliskich kwotach może pomylić kolejność. Porównuj BigDecimal wprost.
    }

    // =================================================================================================
    // 8. compareTo, NIE equals — dla sum i kwot
    // =================================================================================================

    /**
     * 8. {@code equals} w BigDecimal porównuje wartość ORAZ skalę: 2.5 i 2.50 są „różne”.
     * W strumieniach wychodzi to przy distinct, Set, Map i przy sprawdzaniu, czy suma to zero.
     */
    static void compareToForSums() {
        section("8. compareTo, nie equals — dla sum i kwot");

        BigDecimal a = new BigDecimal("2.5");
        BigDecimal b = new BigDecimal("2.50");
        show("2.5 equals 2.50", a.equals(b));
        // WYNIK: 2.5 equals 2.50 → false
        show("2.5 compareTo 2.50 == 0", a.compareTo(b) == 0);
        // WYNIK: 2.5 compareTo 2.50 == 0 → true

        // Suma wartości produktów, których NIE MA na stanie: 2999.00 × 0 + 42.00 × 0 = 0.00 (skala 2!)
        BigDecimal outOfStockValue = SampleData.products().stream()
                .filter(p -> !p.inStock())
                .map(Product::stockValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        show("wartość produktów bez stanu", outOfStockValue);
        // WYNIK: wartość produktów bez stanu → 0.00
        show("equals(ZERO)", outOfStockValue.equals(BigDecimal.ZERO));
        // WYNIK: equals(ZERO) → false
        show("compareTo(ZERO) == 0", outOfStockValue.compareTo(BigDecimal.ZERO) == 0);
        // WYNIK: compareTo(ZERO) == 0 → true
        show("signum() == 0", outOfStockValue.signum() == 0);   // signum = znak: -1, 0 albo 1
        // WYNIK: signum() == 0 → true

        // distinct() używa equals → trzy „różne” kwoty 129.
        long distinctCount = Stream.of("129.00", "129.0", "129")
                .map(BigDecimal::new)
                .distinct()
                .count();
        show("distinct() na 129.00 / 129.0 / 129", distinctCount);
        // WYNIK: distinct() na 129.00 / 129.0 / 129 → 3

        // TreeSet używa compareTo → jedna kwota. Alternatywa: najpierw setScale(2) albo stripTrailingZeros.
        TreeSet<BigDecimal> unique = Stream.of("129.00", "129.0", "129")
                .map(BigDecimal::new)
                .collect(Collectors.toCollection(TreeSet::new));
        show("TreeSet na 129.00 / 129.0 / 129", unique.size());
        // WYNIK: TreeSet na 129.00 / 129.0 / 129 → 1

        // DOBRA PRAKTYKA: kwoty porównuj przez compareTo (albo signum dla zera). equals tylko wtedy,
        // gdy świadomie chcesz, by skala miała znaczenie.
    }

    // =================================================================================================
    // 9. ZAOKRĄGLANIE: każda pozycja osobno kontra suma
    // =================================================================================================

    /**
     * 9. VAT (albo rabat) można zaokrąglić na każdej pozycji albo raz na sumie. Wynik bywa różny
     * o grosz — i to nie jest błąd programu, tylko DECYZJA biznesowa, którą trzeba zapisać wprost.
     */
    static void roundingPerLineVsTotal() {
        section("9. Zaokrąglanie: pozycje osobno kontra suma");

        // Paragon: trzy pozycje po 1.01 zł netto, VAT 23%. Dokładnie: 3.03 × 0.23 = 0.6969.
        List<BigDecimal> receipt = List.of(new BigDecimal("1.01"), new BigDecimal("1.01"), new BigDecimal("1.01"));

        // A) VAT liczony i zaokrąglany na KAŻDEJ pozycji: 0.2323 → 0.23, razy trzy = 0.69.
        BigDecimal perLine = receipt.stream()
                .map(net -> net.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        show("VAT od pozycji", perLine);
        // WYNIK: VAT od pozycji → 0.69

        // B) Najpierw suma netto, potem VAT i JEDNO zaokrąglenie: 0.6969 → 0.70.
        BigDecimal fromTotal = receipt.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(VAT_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        show("VAT od sumy", fromTotal);
        // WYNIK: VAT od sumy → 0.70
        show("różnica", fromTotal.subtract(perLine));   // subtract = odejmij
        // WYNIK: różnica → 0.01

        // PUŁAPKA: „u mnie działa” nic nie znaczy. Na pozycjach nieanulowanych zamówień z SampleData
        // oba sposoby dają akurat to samo (2978.49 zł VAT) — błędy się znoszą. Na innym paragonie: grosz różnicy.
        // DOBRA PRAKTYKA: ustal regułę (np. „VAT od sumy”, albo jak każe przepis) i stosuj ją wszędzie tak samo.
        // Zaokrąglaj w JEDNYM, świadomie wybranym miejscu, a nie „po drodze, gdzie popadnie”.
    }

    // =================================================================================================
    // 10. MINI-CSV: SUMY Z POMINIĘCIEM ZŁYCH WIERSZY
    // =================================================================================================

    /**
     * 10. {@code SampleData.salesCsvLines()} to nagłówek + 12 wierszy, z czego 4 są zepsute. Każdy wiersz
     * zamieniamy na {@code Optional<Sale>} (pusty = nie udało się), liczymy pominięte i sumujemy dobre.
     * Pełne CSV z plików: t18_io_files/Io04Csv.
     */
    static void csvTotals() {
        section("10. Mini-CSV: sumy z pominięciem złych wierszy");

        List<String> csv = SampleData.salesCsvLines();
        showEach("surowe wiersze CSV", csv);
        // WYNIK: surowe wiersze CSV (liczba elementów: 13):
        // WYNIK: • data,sku,produkt,kategoria,ilosc,cena
        // WYNIK: • 2026-05-04,KSI-001,Czysty kod,KSIAZKI,2,79.00
        // WYNIK: • 2026-05-04,SPO-001,Kawa ziarnista 1kg,SPOZYWCZE,3,64.99
        // WYNIK: • 2026-05-05,ELE-003,Słuchawki BT,ELEKTRONIKA,1,349.90
        // WYNIK: • 2026-05-05,ODZ-002,T-shirt bawełniany,ODZIEZ,4,49.99
        // WYNIK: • 2026-05-05,KSI-002,Java. Podstawy,KSIAZKI,dwa,129.00
        // WYNIK: • 2026-05-06,SPO-002,Czekolada gorzka
        // WYNIK: •
        // WYNIK: • 2026-13-06,DOM-002,Lampka biurkowa,DOM,1,129.00
        // WYNIK: • 2026-05-06,ELE-001,Laptop Pro 14,ELEKTRONIKA,1,5499.99
        // WYNIK: • 2026-05-07,SPO-002,Czekolada gorzka,SPOZYWCZE,10,7.49
        // WYNIK: • 2026-05-07,KSI-003,Wzorce projektowe,KSIAZKI,1,99.00
        // WYNIK: • 2026-05-07,DOM-001,Ekspres do kawy,DOM,1,1899.00

        // skip(1) = pomiń nagłówek. Każdy wiersz → Optional<Sale> (tryParse nigdy nie rzuca wyjątku).
        List<String> dataLines = csv.stream().skip(1).toList();
        List<Optional<Sale>> parsed = dataLines.stream()
                .map(Streams15BigDecimalMoney::tryParse)
                .toList();

        // Poprawne i pominięte w jednym przejściu — partitioningBy (Streams12).
        Map<Boolean, Long> stats = parsed.stream()
                .collect(Collectors.partitioningBy(Optional::isPresent, Collectors.counting()));
        show("false = pominięte, true = poprawne", stats);
        // WYNIK: false = pominięte, true = poprawne → {false=4, true=8}

        // Które wiersze odpadły? (W prawdziwym programie zapisz je do logu z numerem wiersza.)
        List<String> skippedLines = dataLines.stream()
                .filter(l -> tryParse(l).isEmpty())
                .map(l -> l.isBlank() ? "(pusty wiersz)" : l)
                .toList();
        showEach("pominięte wiersze", skippedLines);
        // WYNIK: pominięte wiersze (liczba elementów: 4):
        // WYNIK: • 2026-05-05,KSI-002,Java. Podstawy,KSIAZKI,dwa,129.00
        // WYNIK: • 2026-05-06,SPO-002,Czekolada gorzka
        // WYNIK: • (pusty wiersz)
        // WYNIK: • 2026-13-06,DOM-002,Lampka biurkowa,DOM,1,129.00

        List<Sale> sales = parsed.stream().flatMap(Optional::stream).toList();   // Optional::stream (Streams14)

        BigDecimal total = sales.stream().map(Sale::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        show("suma sprzedaży z CSV", total);
        // WYNIK: suma sprzedaży z CSV → 8475.72

        Map<Category, BigDecimal> perCategory = sales.stream()
                .collect(Collectors.groupingBy(Sale::category, () -> new EnumMap<>(Category.class),
                        Collectors.reducing(BigDecimal.ZERO, Sale::total, BigDecimal::add)));
        showEach("sprzedaż na kategorię (CSV)", perCategory);
        // WYNIK: sprzedaż na kategorię (CSV) (liczba kluczy: 5):
        // WYNIK: • ELEKTRONIKA → 5849.89
        // WYNIK: • SPOZYWCZE → 269.87
        // WYNIK: • KSIAZKI → 257.00
        // WYNIK: • ODZIEZ → 199.96
        // WYNIK: • DOM → 1899.00

        // PUŁAPKA: licznik pominiętych przez „int skipped = 0; ... skipped++” w lambdzie się NIE skompiluje
        // (zmienna musi być effectively final). AtomicInteger by zadziałał, ale to efekt uboczny.
        // Lepiej: zamień każdy wiersz na Optional i POLICZ puste — zero efektów ubocznych.
    }

    /**
     * Próbuje zamienić wiersz CSV na Sale. Zepsuty wiersz → Optional.empty() (bez wyjątku na zewnątrz).
     * Kolumny: {@code data,sku,produkt,kategoria,ilosc,cena}.
     */
    static Optional<Sale> tryParse(String line) {
        if (line.isBlank()) {                        // isBlank (Java 11+) = pusty albo same spacje
            return Optional.empty();
        }
        String[] f = line.split(",");                 // split = podziel po przecinku
        if (f.length != 6) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Sale(
                    LocalDate.parse(f[0].strip()),        // miesiąc 13 → DateTimeParseException
                    f[1].strip(),                         // strip (Java 11+) = obetnij białe znaki
                    f[2].strip(),
                    Category.valueOf(f[3].strip()),       // nieznana kategoria → IllegalArgumentException
                    Integer.parseInt(f[4].strip()),       // "dwa" → NumberFormatException
                    new BigDecimal(f[5].strip())));       // "12.50zł" → NumberFormatException
        } catch (DateTimeParseException | IllegalArgumentException e) {
            // NumberFormatException dziedziczy po IllegalArgumentException — łapiemy go tym samym catch.
            return Optional.empty();
        }
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • pieniądze = BigDecimal z TEKSTU: new BigDecimal("0.10"); nigdy new BigDecimal(0.1)
     *   • suma:     .map(...).reduce(BigDecimal.ZERO, BigDecimal::add)   (pusty → 0)
     *   • pozycja:  cena.multiply(BigDecimal.valueOf(ilość))
     *   • średnia:  suma.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP) — bez skali grozi wyjątek
     *   • grupy:    groupingBy(k, () -> new EnumMap<>(Category.class), reducing(ZERO, mapper, BigDecimal::add))
     *               toMap(k, mapper, BigDecimal::add, TreeMap::new)
     *   • miesiące: groupingBy(o -> YearMonth.from(o.date()), TreeMap::new, ...)
     *   • max/min:  Comparator.comparing(Product::price) — bez double
     *   • równość:  compareTo(...) == 0 albo signum() == 0; equals patrzy na skalę (2.5 ≠ 2.50),
     *               distinct/HashSet też! TreeSet używa compareTo
     *   • zaokrąglaj w jednym, uzgodnionym miejscu (pozycja albo suma) — wynik może różnić się o grosz
     *   • BigDecimal jest niezmienny: sum.add(x) bez przypisania nic nie robi
     *   • CSV: skip(1) → map(tryParse → Optional) → policz puste → flatMap(Optional::stream) → sumy
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego Stream.generate(() -> 0.1).limit(10).reduce(0.0, Double::sum) nie daje 1.0?
     *   2. Co wypisze:  System.out.println(Stream.of("1.10", "2.20").map(BigDecimal::new)
     *          .reduce(BigDecimal.ZERO, BigDecimal::add));  ?
     *   3. ZNAJDŹ BŁĄD:  BigDecimal avg = sum.divide(BigDecimal.valueOf(3));  — raz działa, raz rzuca wyjątek.
     *   4. Co wypisze:  System.out.println(new BigDecimal("0.00").equals(BigDecimal.ZERO));  ?
     *   5. ZNAJDŹ BŁĄD:  products.stream().map(Product::price).distinct().count() — „mamy 3 różne ceny 129?!”
     *   6. Jakie dwie drogi prowadzą do mapy „kategoria → suma BigDecimal”? Jaką mapę wybrać dla kluczy enum?
     *   7. Dlaczego VAT liczony od każdej pozycji może różnić się o grosz od VAT-u od sumy? Który jest „dobry”?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Map<Category, BigDecimal> expected3 = expectedRevenuePerCategory();
        List<BigDecimal> split100 = List.of(new BigDecimal("33.34"), new BigDecimal("33.33"), new BigDecimal("33.33"));
        List<BigDecimal> split10 = List.of(new BigDecimal("2.50"), new BigDecimal("2.50"),
                new BigDecimal("2.50"), new BigDecimal("2.50"));
        List<BigDecimal> split005 = List.of(new BigDecimal("0.02"), new BigDecimal("0.02"), new BigDecimal("0.01"));

        Check.equal("ćw. 1: suma cen KSIAZKI", new BigDecimal("307.00"),
                () -> exercise1(SampleData.products(), Category.KSIAZKI));
        Check.equal("ćw. 1: suma cen ELEKTRONIKA", new BigDecimal("10147.89"),
                () -> exercise1(SampleData.products(), Category.ELEKTRONIKA));
        Check.equal("ćw. 2: średnia DOSTARCZONE", new BigDecimal("3065.55"),
                () -> exercise2(SampleData.orders(), OrderStatus.DOSTARCZONE));
        Check.equal("ćw. 2: pusta lista → 0", BigDecimal.ZERO, () -> exercise2(List.of(), OrderStatus.NOWE));
        Check.equal("ćw. 3: przychód na kategorię", expected3, () -> exercise3(SampleData.orders()));
        Check.equal("ćw. 4: 100.00 na 3", split100, () -> exercise4(new BigDecimal("100.00"), 3));
        Check.equal("ćw. 4: 10.00 na 4", split10, () -> exercise4(new BigDecimal("10.00"), 4));
        Check.equal("ćw. 4: 0.05 na 3", split005, () -> exercise4(new BigDecimal("0.05"), 3));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 KSIAZKI (wzorzec)", new BigDecimal("307.00"),
                () -> solution1(SampleData.products(), Category.KSIAZKI));
        Check.equal("ćw. 1 ELEKTRONIKA (wzorzec)", new BigDecimal("10147.89"),
                () -> solution1(SampleData.products(), Category.ELEKTRONIKA));
        Check.equal("ćw. 2 DOSTARCZONE (wzorzec)", new BigDecimal("3065.55"),
                () -> solution2(SampleData.orders(), OrderStatus.DOSTARCZONE));
        Check.equal("ćw. 2 pusta (wzorzec)", BigDecimal.ZERO, () -> solution2(List.of(), OrderStatus.NOWE));
        Check.equal("ćw. 3 (wzorzec)", expected3, () -> solution3(SampleData.orders()));
        Check.equal("ćw. 4 100.00/3 (wzorzec)", split100, () -> solution4(new BigDecimal("100.00"), 3));
        Check.equal("ćw. 4 10.00/4 (wzorzec)", split10, () -> solution4(new BigDecimal("10.00"), 4));
        Check.equal("ćw. 4 0.05/3 (wzorzec)", split005, () -> solution4(new BigDecimal("0.05"), 3));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 8 OK, ✘ 0 BŁĄD
    }

    /** Oczekiwany wynik ćw. 3 — EnumMap, żeby kolejność wypisywania była zawsze ta sama. */
    private static Map<Category, BigDecimal> expectedRevenuePerCategory() {
        Map<Category, BigDecimal> expected = new EnumMap<>(Category.class);
        expected.put(Category.ELEKTRONIKA, new BigDecimal("9147.69"));
        expected.put(Category.SPOZYWCZE, new BigDecimal("521.30"));
        expected.put(Category.KSIAZKI, new BigDecimal("644.00"));
        expected.put(Category.ODZIEZ, new BigDecimal("608.97"));
        expected.put(Category.DOM, new BigDecimal("2028.00"));
        return expected;
    }

    /**
     * ĆWICZENIE 1 (łatwe): suma cen jednostkowych produktów z podanej kategorii (np. KSIAZKI → 307.00).
     * Podpowiedź: {@code filter(p -> p.category() == category)} → map(Product::price) → reduce(BigDecimal.ZERO, BigDecimal::add).
     */
    static BigDecimal exercise1(List<Product> products, Category category) {
        // TODO: twoje rozwiązanie
        return BigDecimal.ZERO;
    }

    /**
     * ĆWICZENIE 2 (średnie): średnia wartość zamówień o danym statusie, 2 miejsca po przecinku, HALF_UP.
     * Dla braku zamówień zwróć BigDecimal.ZERO (bez dzielenia przez zero!).
     * DOSTARCZONE: (6199.79 + 269.87 + 2727.00) / 3 = 3065.553... → 3065.55.
     * Podpowiedź: najpierw zbierz pasujące do listy, sprawdź isEmpty(), potem suma.divide(valueOf(size), 2, HALF_UP).
     */
    static BigDecimal exercise2(List<Order> orders, OrderStatus status) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ na strumień. Przychód na kategorię produktu z pozycji zamówień
     * NIEANULOWANYCH, w kolejności kategorii (EnumMap albo TreeMap).
     * <pre>{@code
     * Map<Category, BigDecimal> result = new EnumMap<>(Category.class);
     * for (Order o : orders) {
     *     if (o.status() == OrderStatus.ANULOWANE) {
     *         continue;
     *     }
     *     for (OrderLine line : o.lines()) {
     *         result.merge(line.product().category(), line.total(), BigDecimal::add);
     *     }
     * }
     * return result;
     * }</pre>
     * Podpowiedź: filter → {@code flatMap(o -> o.lines().stream())} → {@code toMap(l -> l.product().category(),
     * OrderLine::total, BigDecimal::add, () -> new EnumMap<>(Category.class))}.
     */
    static Map<Category, BigDecimal> exercise3(List<Order> orders) {
        // TODO: twoje rozwiązanie
        return Map.of();
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): podziel rachunek {@code total} na {@code people} części tak, by każda miała
     * 2 miejsca po przecinku, części różniły się najwyżej o 1 grosz, a ich SUMA była dokładnie równa total.
     * Nadwyżkowe grosze dostają pierwsze osoby: 100.00 na 3 → [33.34, 33.33, 33.33].
     * Podpowiedź: licz w groszach: {@code long cents = total.movePointRight(2).longValueExact();}
     * base = cents / people, rest = cents % people;
     * {@code IntStream.range(0, people).mapToObj(i -> BigDecimal.valueOf(base + (i < rest ? 1 : 0), 2)).toList()}
     * — valueOf(grosze, 2) = grosze / 100.
     */
    static List<BigDecimal> exercise4(BigDecimal total, int people) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static BigDecimal solution1(List<Product> products, Category category) {
        return products.stream()
                .filter(p -> p.category() == category)
                .map(Product::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static BigDecimal solution2(List<Order> orders, OrderStatus status) {
        List<BigDecimal> totals = orders.stream()
                .filter(o -> o.status() == status)
                .map(Order::total)
                .toList();
        if (totals.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = totals.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(totals.size()), 2, RoundingMode.HALF_UP);
    }

    static Map<Category, BigDecimal> solution3(List<Order> orders) {
        return orders.stream()
                .filter(o -> o.status() != OrderStatus.ANULOWANE)
                .flatMap(o -> o.lines().stream())
                .collect(Collectors.toMap(l -> l.product().category(), OrderLine::total, BigDecimal::add,
                        () -> new EnumMap<>(Category.class)));
    }

    static List<BigDecimal> solution4(BigDecimal total, int people) {
        long cents = total.movePointRight(2).longValueExact();   // movePointRight = przesuń przecinek w prawo
        long base = cents / people;
        long rest = cents % people;
        return IntStream.range(0, people)
                .mapToObj(i -> BigDecimal.valueOf(base + (i < rest ? 1 : 0), 2))
                .toList();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. 0.1 nie ma dokładnego zapisu dwójkowego — każde dodawanie wnosi mały błąd, który się kumuluje
     *      (wynik 0.9999999999999999). Do pieniędzy: BigDecimal z tekstu.
     *   2. 3.30  — BigDecimal dodaje dokładnie, skala wyniku = większa skala składników (2).
     *   3. Bez skali divide działa tylko dla skończonego wyniku (np. 9 / 3). Dla 10 / 3 rzuca ArithmeticException.
     *      Poprawnie: sum.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP).
     *   4. false  — equals porównuje też skalę (0.00 ma skalę 2, ZERO skalę 0). Użyj compareTo albo signum().
     *   5. distinct używa equals, więc 129.00 i 129.0 to dla niego różne wartości. Ujednolić skalę
     *      (setScale(2) albo stripTrailingZeros) albo zbierać do TreeSet (compareTo).
     *   6. groupingBy(k, fabryka, reducing(ZERO, mapper, BigDecimal::add)) albo toMap(k, mapper, BigDecimal::add,
     *      fabryka). Dla enum: EnumMap albo TreeMap — stała kolejność (HashMap z enum = losowa kolejność).
     *   7. Każde zaokrąglenie pozycji gubi/dodaje ułamek grosza; przy sumie zaokrąglamy raz. Oba mogą być
     *      „dobre” — to decyzja biznesowa/prawna. Ważne, by reguła była jedna i stosowana konsekwentnie.
     */
    // </editor-fold>
}
