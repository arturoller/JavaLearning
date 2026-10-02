package t35_capstone;

import helpers.Check;
import helpers.SampleData;
import helpers.TempDir;
import helpers.model.Category;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Projekt 2 — raport sprzedaży z pliku CSV
 *        (sales report = raport sprzedaży; parse = przetworzyć tekst na dane; summary = podsumowanie)
 *
 * W SKRÓCIE:
 *   Plik CSV → walidacja każdej linii (8 poprawnych, 4 odrzucone z numerem linii i powodem) → agregacje
 *   w BigDecimal (kategorie, dni, najlepsze produkty, średni koszyk) → raport tekstowy po polsku i JSON
 *   → zapis obu plików. Do tego kontrola krzyżowa sum i porównanie wersji strumieniowej z pętlową.
 *
 * ANALOGIA:
 *   Księgowa na koniec tygodnia. Najpierw odkłada na bok paragony nieczytelne (z notatką, czemu), potem
 *   sumuje resztę na kilka sposobów — po działach, po dniach — i sprawdza, czy wszystkie sumy dają ten sam
 *   wynik. Na końcu pisze krótką notatkę dla szefa (raport) i tabelkę dla systemu (JSON).
 *
 * JAK TO DZIAŁA:
 *   plik CSV ─► readLines ─► SaleParser.parse (czysta funkcja) ─► ParseReport(sales, errors)
 *                                                                   │
 *                         SalesSummary ◄─ summarize (strumienie) ◄──┘ (+ wersja pętlowa do porównania)
 *                              │
 *               TextReport.render (pl-PL)  /  Json.write ─► ReportStore (pliki w TempDir)
 *   Wykorzystane działy kursu:
 *   • CSV i pliki tekstowe UTF-8                    → t18_io_files/Io04Csv, t18_io_files/Io02ReadingText
 *   • ręczny JSON                                   → t18_io_files/Io05JsonManual
 *   • BigDecimal: sumy, procenty, zaokrąglanie      → t15_numbers/Numbers01BigDecimal, t16_streams/Streams15BigDecimalMoney
 *   • groupingBy, EnumMap, TreeMap                  → t16_streams/Streams11GroupingBy, t08_enums/Enums04EnumMapSet
 *   • formatowanie z Locale                         → t04_strings/Strings04Formatting, t15_numbers/Numbers04FormattingParsing
 *   • rekordy i typy zamknięte (sealed)             → t23_modern_java/Modern05RecordsSealedPatterns
 *   • daty                                          → t17_datetime/DateTime03Formatting
 *
 * SŁÓWKA:
 *   sale = sprzedaż; revenue = przychód; basket = koszyk; line number = numer linii; reason = powód;
 *   valid = poprawny; invalid/rejected = odrzucony; top = najlepsze; cross-check = kontrola krzyżowa;
 *   render = wygeneruj (tekst); writer = zapisywacz; store = magazyn (miejsce zapisu).
 *
 * ZOBACZ TEŻ: t16_streams/Streams19Recipes (przepisy na agregacje), t25_testing/Testing03TestableDesign
 *             (czyste funkcje łatwo testować), t31_jdk_toolbox/Toolbox03I18n (Locale i formaty)
 * </pre>
 */
public class Capstone02SalesReport {

    static final Locale PL = Locale.forLanguageTag("pl-PL");                       // forLanguageTag = z tagu języka
    static final String HEADER = "data,sku,produkt,kategoria,ilosc,cena";
    static final int FIELDS = 6;

    public static void main(String[] args) {
        title("Capstone02 — raport sprzedaży");

        requirements();        // requirements = wymagania
        domainModel();         // domain model = model dziedziny
        parsingWithErrors();   // parsing with errors = parsowanie z błędami
        aggregations();        // aggregations = agregacje
        streamsVersusLoops();  // streams versus loops = strumienie kontra pętle
        crossCheck();          // cross-check = kontrola krzyżowa
        textReport();          // text report = raport tekstowy
        jsonSummary();         // json summary = podsumowanie w JSON
        savingReports();       // saving reports = zapis raportów
        behaviourTests();      // behaviour tests = testy zachowania
        whatNext();            // what next = co dalej
        exercises();           // exercises = ćwiczenia
    }

    // =================================================================================================
    // MODEL DZIEDZINY
    // =================================================================================================

    /** Jedna sprzedaż (jedna linia CSV = jeden paragon, czyli „koszyk”). */
    record Sale(LocalDate date, String sku, String product, Category category, int quantity, BigDecimal unitPrice) {
        Sale {
            Objects.requireNonNull(date, "data");                                   // requireNonNull = wymagaj nie-null
            Objects.requireNonNull(category, "kategoria");
            if (sku == null || sku.isBlank() || product == null || product.isBlank()) { // isBlank (Java 11+)
                throw new IllegalArgumentException("sku i nazwa produktu są wymagane");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("ilość musi być dodatnia: " + quantity);
            }
            if (unitPrice.signum() <= 0) {                                          // signum = znak liczby
                throw new IllegalArgumentException("cena musi być dodatnia: " + unitPrice);
            }
        }

        /** value = wartość linii: cena × ilość, skala 2. */
        BigDecimal value() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
        }
    }

    /** Odrzucona linia: numer linii w pliku (od 1, z nagłówkiem) i powód po polsku. */
    record ParseError(int lineNumber, String reason) {
        @Override
        public String toString() {
            return "linia " + lineNumber + ": " + reason;
        }
    }

    /** Wynik parsowania jednej linii: albo sprzedaż, albo błąd. Typ zamknięty — trzeciej możliwości nie ma. */
    sealed interface LineResult permits Parsed, Rejected { }
    record Parsed(Sale sale) implements LineResult { }
    record Rejected(ParseError error) implements LineResult { }

    /** Wynik parsowania całego pliku. */
    record ParseReport(List<Sale> sales, List<ParseError> errors) {
        ParseReport {
            sales = List.copyOf(sales);                                             // copyOf = niezmienna kopia (Java 10+)
            errors = List.copyOf(errors);
        }
    }

    /** Przychód produktu — pozycja w rankingu. */
    record ProductRevenue(String product, BigDecimal revenue) { }

    /** Wszystkie liczby raportu w jednym niezmiennym obiekcie. */
    record SalesSummary(int count, BigDecimal total, Map<Category, BigDecimal> byCategory,
                        Map<LocalDate, BigDecimal> byDay, List<ProductRevenue> top, BigDecimal averageBasket) { }

    // =================================================================================================
    // LOGIKA — czyste funkcje (te same dane wejściowe → ten sam wynik, bez efektów ubocznych)
    // =================================================================================================

    static final class SaleParser {
        private SaleParser() {
        }

        static ParseReport parse(List<String> lines) {
            List<Sale> sales = new ArrayList<>();
            List<ParseError> errors = new ArrayList<>();
            if (lines.isEmpty() || !lines.get(0).equals(HEADER)) {
                errors.add(new ParseError(1, "brak nagłówka: " + HEADER));
                return new ParseReport(sales, errors);
            }
            for (int i = 1; i < lines.size(); i++) {
                LineResult result = parseLine(i + 1, lines.get(i));                 // i + 1 = numer linii dla człowieka
                if (result instanceof Parsed p) {                                   // instanceof z wzorcem (Java 16+)
                    sales.add(p.sale());
                } else if (result instanceof Rejected r) {
                    errors.add(r.error());
                }
            }
            return new ParseReport(sales, errors);
        }

        static LineResult parseLine(int lineNumber, String line) {
            if (line.isBlank()) {
                return reject(lineNumber, "pusta linia");
            }
            String[] f = line.split(",", -1);                                       // -1 = zachowaj puste pola na końcu
            if (f.length != FIELDS) {
                return reject(lineNumber, "oczekiwano " + FIELDS + " pól, jest " + f.length);
            }
            try {
                LocalDate date = LocalDate.parse(f[0].strip());                     // parse = zamień tekst ISO na datę
                Category category = Category.valueOf(f[3].strip());                // valueOf = stała enuma po nazwie
                int quantity = Integer.parseInt(f[4].strip());
                BigDecimal price = new BigDecimal(f[5].strip());
                return new Parsed(new Sale(date, f[1].strip(), f[2].strip(), category, quantity, price));
            } catch (DateTimeParseException e) {
                return reject(lineNumber, "błędna data: " + f[0]);
            } catch (NumberFormatException e) {
                return reject(lineNumber, "liczba niepoprawna w polu ilość/cena: " + f[4] + " / " + f[5]);
            } catch (IllegalArgumentException e) {                                  // nieznana kategoria lub reguła z Sale
                return reject(lineNumber, e.getMessage());
            }
        }

        private static LineResult reject(int lineNumber, String reason) {
            return new Rejected(new ParseError(lineNumber, reason));
        }
    }

    /** Wersja strumieniowa — zwięzła, każda liczba to jedno wyrażenie. */
    static SalesSummary summarize(List<Sale> sales) {
        BigDecimal total = sales.stream().map(Sale::value).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<Category, BigDecimal> byCategory = sales.stream().collect(Collectors.groupingBy(
                Sale::category, () -> new EnumMap<>(Category.class),
                Collectors.reducing(BigDecimal.ZERO, Sale::value, BigDecimal::add)));   // reducing = redukuj w grupie
        Map<LocalDate, BigDecimal> byDay = sales.stream().collect(Collectors.groupingBy(
                Sale::date, TreeMap::new, Collectors.reducing(BigDecimal.ZERO, Sale::value, BigDecimal::add)));
        List<ProductRevenue> top = sales.stream()
                .collect(Collectors.groupingBy(Sale::product,
                        Collectors.reducing(BigDecimal.ZERO, Sale::value, BigDecimal::add)))
                .entrySet().stream()
                .map(e -> new ProductRevenue(e.getKey(), e.getValue()))
                .sorted(BY_REVENUE_DESC)
                .limit(3)
                .toList();                                                          // toList (Java 16+)
        return new SalesSummary(sales.size(), total, byCategory, byDay, top, average(total, sales.size()));
    }

    /** Ranking: najpierw większy przychód, przy remisie nazwa — kolejność zawsze ta sama. */
    static final Comparator<ProductRevenue> BY_REVENUE_DESC =
            Comparator.comparing(ProductRevenue::revenue).reversed()                // reversed = odwrócona kolejność
                    .thenComparing(ProductRevenue::product);

    /** Wersja pętlowa — dłuższa, ale bywa czytelniejsza dla początkujących. Wynik MUSI być identyczny. */
    static SalesSummary summarizeWithLoops(List<Sale> sales) {
        BigDecimal total = BigDecimal.ZERO;
        Map<Category, BigDecimal> byCategory = new EnumMap<>(Category.class);
        Map<LocalDate, BigDecimal> byDay = new TreeMap<>();
        Map<String, BigDecimal> byProduct = new HashMap<>();
        for (Sale sale : sales) {
            BigDecimal value = sale.value();
            total = total.add(value);
            byCategory.merge(sale.category(), value, BigDecimal::add);              // merge = dodaj albo połącz
            byDay.merge(sale.date(), value, BigDecimal::add);
            byProduct.merge(sale.product(), value, BigDecimal::add);
        }
        List<ProductRevenue> ranking = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : byProduct.entrySet()) {
            ranking.add(new ProductRevenue(e.getKey(), e.getValue()));
        }
        ranking.sort(BY_REVENUE_DESC);
        List<ProductRevenue> top = List.copyOf(ranking.subList(0, Math.min(3, ranking.size())));
        return new SalesSummary(sales.size(), total, byCategory, byDay, top, average(total, sales.size()));
    }

    /** Średni koszyk; przy braku sprzedaży 0.00 zamiast dzielenia przez zero. */
    static BigDecimal average(BigDecimal total, int count) {
        if (count == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
        }
        return total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    /** share = udział procentowy z jednym miejscem po przecinku. */
    static BigDecimal share(BigDecimal part, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.UNNECESSARY);
        }
        return part.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP);
    }

    /** Wspólne wejście dla sekcji: linie z SampleData → poprawne sprzedaże. */
    static ParseReport sampleReport() {
        return SaleParser.parse(SampleData.salesCsvLines());
    }

    // =================================================================================================
    // 1. WYMAGANIA I DANE WEJŚCIOWE
    // =================================================================================================

    /**
     * 1. Zaczynamy od danych: plik ma nagłówek i 12 linii. Część jest zepsuta — raport ma je pominąć,
     * ale WYPISAĆ z numerem linii, żeby ktoś mógł je poprawić u źródła.
     */
    static void requirements() {
        section("1. Wymagania i dane wejściowe");

        List<String> lines = SampleData.salesCsvLines();
        show("nagłówek", lines.get(0));
        // WYNIK: nagłówek → data,sku,produkt,kategoria,ilosc,cena
        show("linii z danymi", lines.size() - 1);
        // WYNIK: linii z danymi → 12
        show("linia 2", lines.get(1));
        // WYNIK: linia 2 → 2026-05-04,KSI-001,Czysty kod,KSIAZKI,2,79.00
        note("wymagania: przychód wg kategorii i dni, TOP 3 produkty, średni koszyk, raport TXT + JSON");
        // WYNIK: ℹ wymagania: przychód wg kategorii i dni, TOP 3 produkty, średni koszyk, raport TXT + JSON

        // DOBRA PRAKTYKA: numery linii liczone jak w edytorze (od 1, nagłówek = linia 1). Dlaczego? Komunikat
        //   „linia 6: ...” ma prowadzić człowieka prosto do miejsca w pliku, a nie do indeksu tablicy.
    }

    // =================================================================================================
    // 2. MODEL DZIEDZINY
    // =================================================================================================

    /**
     * 2. Sale pilnuje reguł biznesowych (ilość i cena dodatnie). Parser pilnuje formatu (liczba pól, daty).
     * Dwa poziomy walidacji: „czy to w ogóle tekst w dobrym formacie” i „czy to ma sens biznesowy”.
     */
    static void domainModel() {
        section("2. Model dziedziny");

        Sale sale = new Sale(LocalDate.of(2026, 5, 4), "SPO-001", "Kawa ziarnista 1kg", Category.SPOZYWCZE,
                3, new BigDecimal("64.99"));
        show("wartość 3 × 64.99", sale.value());
        // WYNIK: wartość 3 × 64.99 → 194.97
        expectThrows("ilość 0", () -> new Sale(sale.date(), "X", "Y", Category.DOM, 0, BigDecimal.ONE));
        // WYNIK: ✔ ilość 0 → rzucono IllegalArgumentException: ilość musi być dodatnia: 0
        expectThrows("cena ujemna", () -> new Sale(sale.date(), "X", "Y", Category.DOM, 1, new BigDecimal("-1")));
        // WYNIK: ✔ cena ujemna → rzucono IllegalArgumentException: cena musi być dodatnia: -1

        // DOBRA PRAKTYKA: wynik parsowania jako typ zamknięty LineResult (Parsed | Rejected) zamiast null albo
        //   wyjątku dla każdej złej linii. Dlaczego? Zła linia to spodziewany przypadek, a nie awaria — chcemy
        //   ją zebrać, a nie przerywać całe przetwarzanie.
    }

    // =================================================================================================
    // 3. PARSOWANIE Z WALIDACJĄ
    // =================================================================================================

    /**
     * 3. Każda linia osobno: poprawna trafia do listy sprzedaży, błędna do listy błędów z powodem.
     * Wyjątki z parseInt / LocalDate.parse zamieniamy na komunikaty zrozumiałe dla człowieka.
     */
    static void parsingWithErrors() {
        section("3. Parsowanie z walidacją");

        ParseReport report = sampleReport();
        show("poprawnych", report.sales().size());
        // WYNIK: poprawnych → 8
        showEach("odrzucone", report.errors());
        // WYNIK: odrzucone (liczba elementów: 4):
        // WYNIK: • linia 6: liczba niepoprawna w polu ilość/cena: dwa / 129.00
        // WYNIK: • linia 7: oczekiwano 6 pól, jest 3
        // WYNIK: • linia 8: pusta linia
        // WYNIK: • linia 9: błędna data: 2026-13-06

        show("nieznana kategoria", SaleParser.parseLine(20, "2026-05-08,X-1,Coś,ZABAWKI,1,5.00"));
        // WYNIK: nieznana kategoria → Rejected[error=linia 20: No enum constant helpers.model.Category.ZABAWKI]

        // PUŁAPKA: komunikat wyjątku z JDK („No enum constant ...”) jest po angielsku i zdradza nazwy klas.
        //   Lepiej złapać go wcześniej i zbudować własny: „nieznana kategoria: ZABAWKI” — zrób to w ćwiczeniu 4.

        // PUŁAPKA: split(",") BEZ -1 gubi puste pola na końcu: "a,b,,".split(",") ma 2 elementy, a nie 4.
        //   Wtedy linia z pustą ceną dostałaby mylący błąd „za mało pól”.
        show("\"a,b,,\".split(\",\").length", "a,b,,".split(",").length);
        // WYNIK: "a,b,,".split(",").length → 2
    }

    // =================================================================================================
    // 4. AGREGACJE (strumienie, EnumMap, TreeMap, BigDecimal)
    // =================================================================================================

    /**
     * 4. Agregacje liczymy jedną czystą funkcją {@code summarize}. EnumMap trzyma kategorie w kolejności
     * deklaracji enuma, TreeMap — dni rosnąco. Obie kolejności są stałe, więc wydruk jest powtarzalny.
     */
    static void aggregations() {
        section("4. Agregacje");

        SalesSummary summary = summarize(sampleReport().sales());
        show("przychód razem", summary.total());
        // WYNIK: przychód razem → 8475.72
        showEach("wg kategorii", summary.byCategory());
        // WYNIK: wg kategorii (liczba kluczy: 5):
        // WYNIK: • ELEKTRONIKA → 5849.89
        // WYNIK: • SPOZYWCZE → 269.87
        // WYNIK: • KSIAZKI → 257.00
        // WYNIK: • ODZIEZ → 199.96
        // WYNIK: • DOM → 1899.00
        showEach("wg dni", summary.byDay());
        // WYNIK: wg dni (liczba kluczy: 4):
        // WYNIK: • 2026-05-04 → 352.97
        // WYNIK: • 2026-05-05 → 549.86
        // WYNIK: • 2026-05-06 → 5499.99
        // WYNIK: • 2026-05-07 → 2072.90
        showEach("TOP 3", summary.top());
        // WYNIK: TOP 3 (liczba elementów: 3):
        // WYNIK: • ProductRevenue[product=Laptop Pro 14, revenue=5499.99]
        // WYNIK: • ProductRevenue[product=Ekspres do kawy, revenue=1899.00]
        // WYNIK: • ProductRevenue[product=Słuchawki BT, revenue=349.90]
        show("średni koszyk (8475.72 / 8)", summary.averageBasket());
        // WYNIK: średni koszyk (8475.72 / 8) → 1059.47    ← 1059.465 zaokrąglone HALF_UP

        // PUŁAPKA: groupingBy bez fabryki mapy daje HashMap — kolejność kluczy nie jest gwarantowana. Dla
        //   enuma używaj () -> new EnumMap<>(Category.class), dla dat TreeMap::new.

        // DOBRA PRAKTYKA: ranking zawsze z drugim kryterium (thenComparing po nazwie). Dlaczego? Przy remisie
        //   przychodów kolejność zależałaby od kolejności w HashMap, czyli mogłaby się zmienić w innej wersji JDK.
    }

    // =================================================================================================
    // 5. STRUMIENIE KONTRA PĘTLE
    // =================================================================================================

    /**
     * 5. Ta sama logika napisana dwa razy to darmowy test: jeśli wyniki się różnią, w jednej wersji jest błąd.
     * Rekordy porównują wszystkie pola przez equals, więc wystarczy jedno porównanie całych podsumowań.
     */
    static void streamsVersusLoops() {
        section("5. Strumienie kontra pętle");

        List<Sale> sales = sampleReport().sales();
        SalesSummary viaStreams = summarize(sales);
        SalesSummary viaLoops = summarizeWithLoops(sales);
        show("wyniki identyczne?", viaStreams.equals(viaLoops));
        // WYNIK: wyniki identyczne? → true
        show("puste dane — identyczne?", summarize(List.of()).equals(summarizeWithLoops(List.of())));
        // WYNIK: puste dane — identyczne? → true

        // PUŁAPKA: equals rekordu porównuje BigDecimal przez equals, czyli ZE skalą: 0.00 ≠ 0. Tu działa, bo obie
        //   wersje zaczynają od BigDecimal.ZERO i dodają wartości ze skalą 2, a average() zawsze ustawia skalę 2.

        // DOBRA PRAKTYKA: merge w pętli = groupingBy + reducing w strumieniu. Dlaczego warto znać oba? Pętla
        //   lepiej znosi skomplikowane warunki i debugowanie krok po kroku, strumień jest krótszy i bez stanu.
    }

    // =================================================================================================
    // 6. KONTROLA KRZYŻOWA SUM
    // =================================================================================================

    /**
     * 6. Ta sama kwota policzona różnymi drogami musi się zgadzać: suma kategorii = suma dni = suma całości.
     * To tani bezpiecznik — wyłapuje zgubione linie i błędy grupowania.
     */
    static void crossCheck() {
        section("6. Kontrola krzyżowa sum");

        SalesSummary summary = summarize(sampleReport().sales());
        BigDecimal sumOfCategories = sum(summary.byCategory().values());
        BigDecimal sumOfDays = sum(summary.byDay().values());
        show("kategorie = całość?", sumOfCategories.compareTo(summary.total()) == 0);
        // WYNIK: kategorie = całość? → true
        show("dni = całość?", sumOfDays.compareTo(summary.total()) == 0);
        // WYNIK: dni = całość? → true

        BigDecimal shares = summary.byCategory().values().stream()
                .map(v -> share(v, summary.total()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        show("suma udziałów %", shares);
        // WYNIK: suma udziałów % → 100.0
        // (udziały zaokrąglone osobno mogą dać np. 99.9 albo 100.1 — w raporcie to normalne i warto to opisać)

        // PUŁAPKA: equals zamiast compareTo. 8475.72 i 8475.720 to ta sama kwota, ale equals mówi false.
        show("equals 8475.72 vs 8475.720", new BigDecimal("8475.72").equals(new BigDecimal("8475.720")));
        // WYNIK: equals 8475.72 vs 8475.720 → false
        show("compareTo == 0", new BigDecimal("8475.72").compareTo(new BigDecimal("8475.720")) == 0);
        // WYNIK: compareTo == 0 → true
    }

    static BigDecimal sum(java.util.Collection<BigDecimal> values) {
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // =================================================================================================
    // 7. RAPORT TEKSTOWY (String.format z Locale pl-PL)
    // =================================================================================================

    /** TextReport = raport tekstowy: tylko prezentacja, żadnych obliczeń poza formatowaniem. */
    static final class TextReport {
        private TextReport() {
        }

        static List<String> render(SalesSummary s, List<ParseError> errors) {
            List<String> out = new ArrayList<>();
            out.add("RAPORT SPRZEDAŻY");
            out.add(fmt("Sprzedaży: %d, odrzuconych linii: %d", s.count(), errors.size()));
            out.add(fmt("Przychód razem: %,.2f zł", s.total()));
            out.add(fmt("Średni koszyk:  %,.2f zł", s.averageBasket()));
            out.add("Wg kategorii (malejąco):");
            s.byCategory().entrySet().stream()
                    .sorted(Map.Entry.<Category, BigDecimal>comparingByValue().reversed())   // comparingByValue = po wartości
                    .forEach(e -> out.add(fmt("  %-12s %12s zł %6.1f%%", e.getKey().getDisplayName(),
                            fmt("%,.2f", e.getValue()), share(e.getValue(), s.total()))));
            out.add("TOP 3 produkty:");
            for (int i = 0; i < s.top().size(); i++) {
                ProductRevenue p = s.top().get(i);
                out.add(fmt("  %d. %-18s %12s zł", i + 1, p.product(), fmt("%,.2f", p.revenue())));
            }
            return List.copyOf(out);
        }

        /** fmt = sformatuj po polsku i zamień twarde spacje (U+00A0, U+202F) na zwykłe. */
        static String fmt(String pattern, Object... args) {
            return String.format(PL, pattern, args)
                    .replace((char) 0x00A0, ' ')
                    .replace((char) 0x202F, ' ');
        }
    }

    /**
     * 7. Raport dla człowieka: polskie liczby (przecinek dziesiętny, spacja między tysiącami), kolumny
     * wyrównane przez szerokość pola w formacie (%-12s, %12s).
     */
    static void textReport() {
        section("7. Raport tekstowy");

        ParseReport parsed = sampleReport();
        TextReport.render(summarize(parsed.sales()), parsed.errors()).forEach(System.out::println);
        // WYNIK: RAPORT SPRZEDAŻY
        // WYNIK: Sprzedaży: 8, odrzuconych linii: 4
        // WYNIK: Przychód razem: 8 475,72 zł
        // WYNIK: Średni koszyk:  1 059,47 zł
        // WYNIK: Wg kategorii (malejąco):
        // WYNIK: Elektronika      5 849,89 zł   69,0%
        // WYNIK: Dom i ogród      1 899,00 zł   22,4%
        // WYNIK: Spożywcze          269,87 zł    3,2%
        // WYNIK: Książki            257,00 zł    3,0%
        // WYNIK: Odzież             199,96 zł    2,4%
        // WYNIK: TOP 3 produkty:
        // WYNIK: 1. Laptop Pro 14          5 499,99 zł
        // WYNIK: 2. Ekspres do kawy        1 899,00 zł
        // WYNIK: 3. Słuchawki BT             349,90 zł

        // PUŁAPKA: polski format wstawia między tysiące TWARDĄ spację (U+00A0, w nowszych JDK bywa U+202F).
        //   Wygląda jak spacja, ale "8 475,72".equals(...) zwraca false, a w testach różnica jest niewidoczna.
        //   Dlatego fmt() zamienia oba znaki na zwykłą spację.

        // DOBRA PRAKTYKA: Locale podane jawnie (PL). Dlaczego? String.format bez Locale użyje ustawień systemu —
        //   na komputerze z angielskim systemem raport miałby kropki i przecinki zamienione miejscami.
    }

    // =================================================================================================
    // 8. PODSUMOWANIE W JSON (mały zapisywacz)
    // =================================================================================================

    /** Json = minimalny zapisywacz JSON: Map, List, String, liczby, boolean i null — rekurencyjnie. */
    static final class Json {
        private Json() {
        }

        static String write(Object value) {
            StringBuilder sb = new StringBuilder();
            write(value, 0, sb);
            return sb.toString();
        }

        private static void write(Object value, int indent, StringBuilder sb) {
            if (value == null) {
                sb.append("null");
            } else if (value instanceof String s) {
                sb.append('"').append(escape(s)).append('"');
            } else if (value instanceof BigDecimal d) {
                sb.append(d.toPlainString());                                       // toPlainString = bez notacji 1E+3
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else if (value instanceof Map<?, ?> map) {
                writeMap(map, indent, sb);
            } else if (value instanceof List<?> list) {
                sb.append('[');
                for (int i = 0; i < list.size(); i++) {
                    sb.append(i == 0 ? "" : ", ");
                    write(list.get(i), indent, sb);
                }
                sb.append(']');
            } else {
                throw new IllegalArgumentException("nieobsługiwany typ JSON: " + value.getClass().getSimpleName());
            }
        }

        private static void writeMap(Map<?, ?> map, int indent, StringBuilder sb) {
            String pad = "  ".repeat(indent + 1);                                   // repeat = powtórz (Java 11+)
            sb.append("{\n");
            int i = 0;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                sb.append(pad).append('"').append(escape(String.valueOf(e.getKey()))).append("\": ");
                write(e.getValue(), indent + 1, sb);
                sb.append(++i < map.size() ? ",\n" : "\n");
            }
            sb.append("  ".repeat(indent)).append('}');
        }

        static String escape(String s) {
            StringBuilder sb = new StringBuilder();
            for (char c : s.toCharArray()) {
                switch (c) {
                    case '"' -> sb.append("\\\"");
                    case '\\' -> sb.append("\\\\");
                    case '\n' -> sb.append("\\n");
                    default -> sb.append(c < 0x20 ? String.format(Locale.ROOT, "\\u%04x", (int) c) : String.valueOf(c));
                }
            }
            return sb.toString();
        }
    }

    /** Budowa drzewa JSON: LinkedHashMap trzyma kolejność pól taką, jak ją wstawiamy. */
    static Map<String, Object> toJsonTree(SalesSummary s, List<ParseError> errors) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("salesCount", s.count());                                          // sales count = liczba sprzedaży
        root.put("rejectedLines", errors.stream().map(ParseError::lineNumber).toList()); // rejected = odrzucone
        root.put("total", s.total());
        root.put("averageBasket", s.averageBasket());
        Map<String, Object> categories = new LinkedHashMap<>();
        s.byCategory().forEach((category, value) -> categories.put(category.name(), value));
        root.put("byCategory", categories);
        root.put("top3", s.top().stream().map(ProductRevenue::product).toList());
        return root;
    }

    /**
     * 8. JSON dla innego programu. Budujemy drzewo z map i list, a zapisywacz zamienia je na tekst.
     * Klucze po angielsku, bo to identyfikatory czytane przez programy (jak nazwy pól w kodzie).
     */
    static void jsonSummary() {
        section("8. Podsumowanie w JSON");

        ParseReport parsed = sampleReport();
        System.out.println(Json.write(toJsonTree(summarize(parsed.sales()), parsed.errors())));
        // WYNIK: {
        // WYNIK: "salesCount": 8,
        // WYNIK: "rejectedLines": [6, 7, 8, 9],
        // WYNIK: "total": 8475.72,
        // WYNIK: "averageBasket": 1059.47,
        // WYNIK: "byCategory": {
        // WYNIK: "ELEKTRONIKA": 5849.89,
        // WYNIK: "SPOZYWCZE": 269.87,
        // WYNIK: "KSIAZKI": 257.00,
        // WYNIK: "ODZIEZ": 199.96,
        // WYNIK: "DOM": 1899.00
        // WYNIK: },
        // WYNIK: "top3": ["Laptop Pro 14", "Ekspres do kawy", "Słuchawki BT"]
        // WYNIK: }

        show("escape cudzysłowu", Json.escape("Kurtka \"zimowa\""));
        // WYNIK: escape cudzysłowu → Kurtka \"zimowa\"

        // PUŁAPKA: kwoty jako liczby JSON są wygodne, ale JavaScript czyta je jako double. Wiele API wysyła więc
        //   pieniądze jako tekst ("8475.72") albo w groszach (847572). Wybór opisz w dokumentacji API.

        // DOBRA PRAKTYKA: w prawdziwym projekcie użyj biblioteki (Jackson, w Springu jest domyślnie). Ręczny
        //   zapisywacz piszemy raz, żeby zrozumieć format i ucieczkę znaków (escape).
    }

    // =================================================================================================
    // 9. ZAPIS RAPORTÓW (infrastruktura na brzegu, za interfejsem)
    // =================================================================================================

    /** Port wyjścia: logika nie wie, gdzie trafiają raporty (plik, e-mail, chmura). */
    interface ReportStore {
        void save(String name, String content);
    }

    /** Implementacja plikowa: zawsze UTF-8, nazwy plików względem katalogu. */
    record DirectoryReportStore(Path dir) implements ReportStore {
        @Override
        public void save(String name, String content) {
            try {
                Files.writeString(dir.resolve(name), content, StandardCharsets.UTF_8); // writeString (Java 11+)
            } catch (IOException e) {
                throw new UncheckedIOException("nie zapisano " + name, e);
            }
        }
    }

    /**
     * 9. Cały przepływ od pliku do plików: zapis wejścia do TempDir, odczyt, raport, zapis wyników.
     * Na ekran wypisujemy tylko nazwy i liczbę linii — ścieżka TempDir jest inna przy każdym uruchomieniu.
     */
    static void savingReports() {
        section("9. Zapis raportów");

        Path dir = TempDir.create("capstone02-");
        try {
            Path input = dir.resolve("sprzedaz.csv");
            Files.write(input, SampleData.salesCsvLines(), StandardCharsets.UTF_8);
            ParseReport parsed = SaleParser.parse(Files.readAllLines(input, StandardCharsets.UTF_8));
            SalesSummary summary = summarize(parsed.sales());

            ReportStore store = new DirectoryReportStore(dir);
            store.save("raport.txt", String.join(System.lineSeparator(), TextReport.render(summary, parsed.errors())));
            store.save("podsumowanie.json", Json.write(toJsonTree(summary, parsed.errors())));

            try (var files = Files.list(dir)) {                                     // list = elementy katalogu; var (Java 10+)
                files.map(p -> p.getFileName().toString()).sorted().forEach(name -> note(name));
            }
            // WYNIK: ℹ podsumowanie.json
            // WYNIK: ℹ raport.txt
            // WYNIK: ℹ sprzedaz.csv
            List<String> back = Files.readAllLines(dir.resolve("raport.txt"), StandardCharsets.UTF_8);
            show("raport.txt — linii / pierwsza", back.size() + " / " + back.get(0));
            // WYNIK: raport.txt — linii / pierwsza → 14 / RAPORT SPRZEDAŻY
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            TempDir.deleteRecursively(dir);
        }

        // PUŁAPKA: Files.list zwraca strumień, który TRZYMA otwarty katalog — bez try-with-resources na Windows
        //   katalogu nie da się potem usunąć. Kolejność elementów zależy od systemu plików, dlatego sorted().

        // DOBRA PRAKTYKA: obliczenia (summarize) nie wiedzą nic o plikach, a zapis (ReportStore) nic o liczbach.
        //   Dlaczego? Każdą część testujesz osobno, a zmiana „zapisuj do chmury” nie dotyka logiki.
    }

    // =================================================================================================
    // 10. TESTY ZACHOWANIA
    // =================================================================================================

    /** 10. Kilka zachowań i przypadków brzegowych: puste dane, sam nagłówek, brak nagłówka, zaokrąglenie. */
    static void behaviourTests() {
        section("10. Testy zachowania");

        Check.equal("pusty plik → błąd nagłówka", List.of(new ParseError(1, "brak nagłówka: " + HEADER)),
                () -> SaleParser.parse(List.of()).errors());
        Check.equal("sam nagłówek → 0 sprzedaży", 0, () -> SaleParser.parse(List.of(HEADER)).sales().size());
        Check.equal("średnia bez sprzedaży → 0.00", new BigDecimal("0.00"), () -> summarize(List.of()).averageBasket());
        Check.equal("średnia 10 / 3 → 3.33", new BigDecimal("3.33"), () -> average(BigDecimal.TEN, 3));
        Check.equal("udział 1 z 3 → 33.3%", new BigDecimal("33.3"), () -> share(BigDecimal.ONE, new BigDecimal("3")));
        Check.equal("spacje wokół pól są obcinane", Category.DOM, () -> ((Parsed) SaleParser.parseLine(2,
                "2026-05-07 , DOM-001 , Ekspres , DOM , 1 , 1899.00")).sale().category());
        Check.equal("ilość 0 → odrzucona z powodem", new Rejected(new ParseError(3, "ilość musi być dodatnia: 0")),
                () -> SaleParser.parseLine(3, "2026-05-07,DOM-001,Ekspres,DOM,0,1899.00"));
        Check.summary();
        // WYNIK: ✔ OK    pusty plik → błąd nagłówka
        // WYNIK: ✔ OK    sam nagłówek → 0 sprzedaży
        // WYNIK: ✔ OK    średnia bez sprzedaży → 0.00
        // WYNIK: ✔ OK    średnia 10 / 3 → 3.33
        // WYNIK: ✔ OK    udział 1 z 3 → 33.3%
        // WYNIK: ✔ OK    spacje wokół pól są obcinane
        // WYNIK: ✔ OK    ilość 0 → odrzucona z powodem
        // WYNIK: PODSUMOWANIE: ✔ 7 OK, ✘ 0 BŁĄD

        // DOBRA PRAKTYKA: testy na czystych funkcjach (parseLine, average, share) nie potrzebują plików.
        //   Plik sprawdzamy jednym testem przepływu (sekcja 9) — szybkie testy na dole, mało wolnych na górze.
    }

    // =================================================================================================
    // 11. CO DALEJ
    // =================================================================================================

    /** 11. Jak ten sam program wyglądałby w Springu i co warto powtórzyć. */
    static void whatNext() {
        section("11. Co dalej");

        note("SaleParser + summarize → @Service SalesReportService (bez zmian w logice)");
        // WYNIK: ℹ SaleParser + summarize → @Service SalesReportService (bez zmian w logice)
        note("Json → Jackson ObjectMapper; rekordy zamieniają się w JSON same");
        // WYNIK: ℹ Json → Jackson ObjectMapper; rekordy zamieniają się w JSON same
        note("ReportStore → @Component; uruchamianie co noc: @Scheduled(cron = \"0 0 2 * * *\")");
        // WYNIK: ℹ ReportStore → @Component; uruchamianie co noc: @Scheduled(cron = "0 0 2 * * *")
        note("duże pliki → Spring Batch (czytaj po kawałku, a nie readAllLines)");
        // WYNIK: ℹ duże pliki → Spring Batch (czytaj po kawałku, a nie readAllLines)

        // Do powtórki: t16_streams/Streams15BigDecimalMoney, t18_io_files/Io04Csv, t18_io_files/Io05JsonManual,
        //   t34_toward_spring/Spring04WhatSpringGives.
        // PUŁAPKA: readAllLines wczytuje cały plik do pamięci. Przy gigabajtowym CSV użyj Files.lines w
        //   try-with-resources i agreguj w locie (t18_io_files/Io02ReadingText).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Walidacja dwupoziomowa: format w parserze (pola, liczby, daty), sens biznesowy w rekordzie (> 0).
     *   • Zła linia = dane (Rejected z numerem i powodem), a nie przerwanie programu.
     *   • split(",", -1) zachowuje puste pola na końcu; strip() obcina spacje wokół pól.
     *   • Sumy: reduce(BigDecimal.ZERO, BigDecimal::add); grupy: groupingBy(klucz, fabrykaMapy, reducing(...)).
     *   • EnumMap / TreeMap = stała kolejność wydruku; ranking zawsze z drugim kryterium.
     *   • Dzielenie BigDecimal zawsze ze skalą i RoundingMode; zabezpiecz dzielenie przez zero.
     *   • Porównanie kwot: compareTo == 0; equals sprawdza też skalę (również w equals rekordu!).
     *   • Kontrola krzyżowa: suma kategorii = suma dni = całość.
     *   • Format pl-PL: String.format(PL, "%,.2f", kwota) + zamiana U+00A0/U+202F na spację.
     *   • JSON: drzewo LinkedHashMap/List → zapisywacz z ucieczką ", \, \n i znaków sterujących.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego parser zwraca LineResult (Parsed | Rejected), a nie rzuca wyjątku przy pierwszej złej linii?
     *   2. Co wypisze:  System.out.println("2026-05-04,KSI-001,,".split(",").length);  ?
     *   3. Co wypisze:  System.out.println(new BigDecimal("10").divide(new BigDecimal("4")));  ?
     *      A co przy  new BigDecimal("10").divide(new BigDecimal("3"))  ?
     *   4. ZNAJDŹ BŁĄD:  Map<Category, BigDecimal> m = sales.stream().collect(Collectors.groupingBy(
     *          Sale::category, Collectors.reducing(BigDecimal.ZERO, Sale::value, BigDecimal::add)));
     *      showEach("kategorie", m);   // wydruk ma być taki sam przy każdym uruchomieniu
     *   5. Po co kontrola krzyżowa, skoro wszystkie sumy liczy ten sam program?
     *   6. ZNAJDŹ BŁĄD:  if (summary.total().equals(new BigDecimal("8475.720"))) { System.out.println("OK"); }
     *   7. Dlaczego raport polski zamienia U+00A0 na zwykłą spację i jak taki błąd objawia się w teście?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        List<Sale> sales = sampleReport().sales();
        LocalDate may7 = LocalDate.of(2026, 5, 7);
        Check.equal("ćw. 1: przychód 7 maja", new BigDecimal("2072.90"), () -> exercise1(sales, may7));
        Check.equal("ćw. 2: sztuki wg kategorii", expectedUnits(), () -> exercise2(sales));
        Check.equal("ćw. 3: najlepszy dzień", Optional.of(LocalDate.of(2026, 5, 6)), () -> exercise3(sales));
        Check.equal("ćw. 3: brak danych", Optional.empty(), () -> exercise3(List.of()));
        Check.equal("ćw. 4: nowe reguły", expectedReasons(), () -> validationSamples().stream().map(
                Capstone02SalesReport::exercise4).toList());
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", new BigDecimal("2072.90"), () -> solution1(sales, may7));
        Check.equal("ćw. 2 (wzorzec)", expectedUnits(), () -> solution2(sales));
        Check.equal("ćw. 3 (wzorzec)", Optional.of(LocalDate.of(2026, 5, 6)), () -> solution3(sales));
        Check.equal("ćw. 3: brak danych (wzorzec)", Optional.empty(), () -> solution3(List.of()));
        Check.equal("ćw. 4 (wzorzec)", expectedReasons(), () -> validationSamples().stream().map(
                Capstone02SalesReport::solution4).toList());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    static Map<Category, Integer> expectedUnits() {
        Map<Category, Integer> units = new EnumMap<>(Category.class);
        units.put(Category.ELEKTRONIKA, 2);
        units.put(Category.SPOZYWCZE, 13);
        units.put(Category.KSIAZKI, 3);
        units.put(Category.ODZIEZ, 4);
        units.put(Category.DOM, 1);
        return units;
    }

    static List<String> validationSamples() {
        return List.of(
                "2026-05-08,KSI-001,Czysty kod,KSIAZKI,2,79.00",
                "2026-05-08,KSI-001,Czysty kod,KSIAZKI,2,79.005",
                "2026-05-08,SPO-002,Czekolada,SPOZYWCZE,1500,7.49",
                "2026-05-08,X-1,Klocki,ZABAWKI,1,5.00");
    }

    static List<String> expectedReasons() {
        return List.of("OK", "cena ma więcej niż 2 miejsca po przecinku: 79.005",
                "ilość powyżej 1000: 1500", "nieznana kategoria: ZABAWKI");
    }

    /**
     * ĆWICZENIE 1 (łatwe): nowa linia raportu — przychód z jednego dnia. Brak sprzedaży tego dnia → 0.
     * Podpowiedź: filter po dacie (equals na LocalDate jest bezpieczne), map(Sale::value), reduce.
     */
    static BigDecimal exercise1(List<Sale> sales, LocalDate day) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): liczba sprzedanych SZTUK w każdej kategorii, w kolejności enuma (EnumMap).
     * Podpowiedź: groupingBy(Sale::category, () -> new EnumMap<>(Category.class), summingInt(Sale::quantity))
     * albo pętla z merge(kategoria, ilość, Integer::sum).
     */
    static Map<Category, Integer> exercise2(List<Sale> sales) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ pętlę na strumień — dzień o największym przychodzie.
     * <pre>{@code
     * Map<LocalDate, BigDecimal> byDay = new TreeMap<>();
     * for (Sale s : sales) byDay.merge(s.date(), s.value(), BigDecimal::add);
     * LocalDate best = null; BigDecimal max = null;
     * for (var e : byDay.entrySet())
     *     if (max == null || e.getValue().compareTo(max) > 0) { max = e.getValue(); best = e.getKey(); }
     * return Optional.ofNullable(best);
     * }</pre>
     * Podpowiedź: groupingBy(..., reducing(...)) → entrySet().stream() → max(Map.Entry.comparingByValue()) → map(getKey).
     */
    static Optional<LocalDate> exercise3(List<Sale> sales) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): nowe reguły walidacji linii (bez nagłówka). Zwróć "OK" albo powód:
     * cena z więcej niż 2 miejscami po przecinku → "cena ma więcej niż 2 miejsca po przecinku: 79.005";
     * ilość powyżej 1000 → "ilość powyżej 1000: 1500"; nieznana kategoria → "nieznana kategoria: ZABAWKI"
     * (własny komunikat zamiast angielskiego z valueOf). Zakładamy, że daty i liczba pól są poprawne.
     * Podpowiedź: BigDecimal.scale() mówi, ile cyfr jest po przecinku; kategorię sprawdź przez
     * Arrays.stream(Category.values()).anyMatch(c -> c.name().equals(tekst)).
     */
    static String exercise4(String line) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static BigDecimal solution1(List<Sale> sales, LocalDate day) {
        return sales.stream()
                .filter(s -> s.date().equals(day))
                .map(Sale::value)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static Map<Category, Integer> solution2(List<Sale> sales) {
        return sales.stream().collect(Collectors.groupingBy(Sale::category, () -> new EnumMap<>(Category.class),
                Collectors.summingInt(Sale::quantity)));
    }

    static Optional<LocalDate> solution3(List<Sale> sales) {
        return sales.stream()
                .collect(Collectors.groupingBy(Sale::date, TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Sale::value, BigDecimal::add)))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    static String solution4(String line) {
        String[] f = line.split(",", -1);
        String categoryName = f[3].strip();
        boolean knownCategory = java.util.Arrays.stream(Category.values())
                .anyMatch(c -> c.name().equals(categoryName));
        if (!knownCategory) {
            return "nieznana kategoria: " + categoryName;
        }
        int quantity = Integer.parseInt(f[4].strip());
        if (quantity > 1000) {
            return "ilość powyżej 1000: " + quantity;
        }
        BigDecimal price = new BigDecimal(f[5].strip());
        if (price.scale() > 2) {
            return "cena ma więcej niż 2 miejsca po przecinku: " + price;
        }
        return "OK";
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Zła linia to spodziewany przypadek w danych z zewnątrz. Chcemy przetworzyć resztę i zebrać WSZYSTKIE
     *      błędy z numerami linii naraz; wyjątek przerwałby pracę na pierwszym błędzie. Typ zamknięty wymusza
     *      obsłużenie obu przypadków.
     *   2. 2 — split bez limitu usuwa puste pola z końca ("KSI-001" i data zostają). Ze split(",", -1) byłyby 4.
     *   3. 2.5 — dzielenie jest dokładne. Dla 10 / 3 rzuci ArithmeticException (rozwinięcie nieskończone), dlatego
     *      zawsze podajemy skalę i RoundingMode: divide(x, 2, RoundingMode.HALF_UP).
     *   4. groupingBy bez fabryki mapy tworzy HashMap; klucze-enumy mają hashCode zależny od uruchomienia, więc
     *      kolejność wydruku może się zmieniać. Poprawka: groupingBy(Sale::category, () -> new EnumMap<>(Category.class), ...).
     *   5. Bo sumy idą różnymi drogami (inne grupowanie, inne mapy). Zgubiona linia, zły klucz albo błąd w jednej
     *      ścieżce dadzą różne wyniki — tani test, który wyłapuje błędy bez znajomości poprawnej kwoty.
     *   6. equals porównuje także skalę: 8475.72 (skala 2) i 8475.720 (skala 3) nie są „równe”, więc OK się nie
     *      wypisze. Poprawnie: summary.total().compareTo(new BigDecimal("8475.720")) == 0.
     *   7. Polski format oddziela tysiące twardą spacją (U+00A0 lub U+202F). Na ekranie wygląda jak spacja, ale
     *      porównanie z "8 475,72" zwraca false, a komunikat testu pokazuje dwa „identyczne” napisy.
     */
    // </editor-fold>
}
