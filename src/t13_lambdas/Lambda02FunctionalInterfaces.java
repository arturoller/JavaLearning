package t13_lambdas;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Interfejsy funkcyjne — „gniazdka”, do których pasuje lambda
 *        (functional interface = interfejs funkcyjny)
 *
 * W SKRÓCIE:
 *   Interfejs funkcyjny to interfejs, który ma DOKŁADNIE JEDNĄ metodę abstrakcyjną (bez ciała).
 *   Tylko w miejsce takiego interfejsu można wstawić lambdę — bo lambda to ciało tej jednej metody.
 *   Metody default i static oraz metody z klasy Object (equals, toString...) się NIE liczą.
 *   Adnotacja {@code @FunctionalInterface} prosi kompilator, żeby tego pilnował.
 *
 * ANALOGIA: gniazdko elektryczne i wtyczka.
 *   Interfejs funkcyjny to gniazdko o konkretnym kształcie (ile parametrów, jakiego typu, co zwraca).
 *   Lambda to wtyczka — pasuje do każdego gniazdka o tym samym kształcie. Gniazdko z DWOMA różnymi otworami
 *   na dwie różne wtyczki (dwie metody abstrakcyjne) nie przyjmie jednej wtyczki.
 *   Adnotacja {@code @FunctionalInterface} to naklejka „tylko jedna wtyczka” — elektryk (kompilator) nie pozwoli
 *   dorobić drugiego otworu.
 *
 * JAK TO DZIAŁA:
 *   {@code @FunctionalInterface}
 *   interface TextTransformer {
 *       String transform(String text);          ← jedyna metoda abstrakcyjna = „kształt” lambdy: String → String
 *       default ... / static ...                ← mogą być, nie liczą się
 *   }
 *   TextTransformer upper = {@code text -> text.toUpperCase()};   ← lambda staje się OBIEKTEM tego interfejsu
 *   upper.transform("java")                         ← wywołanie metody interfejsu uruchamia ciało lambdy → "JAVA"
 *
 *   Nazwa metody (transform, validate, apply...) jest dowolna — lambda jej nie widzi. Liczy się KSZTAŁT:
 *   liczba i typy parametrów oraz typ wyniku.
 *
 * SŁÓWKA:
 *   functional interface = interfejs funkcyjny; abstract method = metoda abstrakcyjna (bez ciała);
 *   SAM (Single Abstract Method) = jedna metoda abstrakcyjna; annotation = adnotacja; transform = przekształć;
 *   validate = sprawdź poprawność; validator = walidator (sprawdzacz); rule = reguła; price = cena;
 *   default = domyślna (metoda z ciałem w interfejsie); unexpected = nieoczekiwany; ambiguous = niejednoznaczny;
 *   overload = przeciążyć; describe = opisz; command = polecenie; step = krok.
 *
 * ZOBACZ TEŻ: Lambda01FromAnonymousToLambda (składnia lambdy), Lambda03JavaUtilFunction (gotowe interfejsy
 *             funkcyjne z Javy), t07_inheritance_polymorphism/Inherit04Interfaces (metody default i static),
 *             Lambda08Pitfalls (niejednoznaczne przeciążenia).
 * </pre>
 */
public class Lambda02FunctionalInterfaces {

    /** NINETY_PERCENT = 90% (mnożnik 0.90). Stała BigDecimal tworzona raz. */
    private static final BigDecimal NINETY_PERCENT = new BigDecimal("0.90");
    /** FIFTY = pięćdziesiąt. */
    private static final BigDecimal FIFTY = new BigDecimal("50");

    public static void main(String[] args) {
        title("Lambda02 — interfejsy funkcyjne");

        definition();                    // definition = definicja
        functionalInterfaceAnnotation(); // functional interface annotation = adnotacja @FunctionalInterface
        whatDoesNotCount();              // what does not count = co się nie liczy (default, static, metody Object)
        ownGenericInterfaces();          // own generic interfaces = własne interfejsy generyczne
        sameLambdaDifferentInterfaces(); // same lambda, different interfaces = ta sama lambda, różne interfejsy
        lambdaIsAnObject();              // lambda is an object = lambda jest obiektem
        abstractClassNotAllowed();       // abstract class not allowed = klasa abstrakcyjna odpada
        overloadingPreview();            // overloading preview = zapowiedź problemu z przeciążeniami
        exercises();                     // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. DEFINICJA
    // =================================================================================================

    /**
     * TextTransformer = przekształcacz tekstu. Nasz pierwszy interfejs funkcyjny: String → String.
     */
    @FunctionalInterface
    interface TextTransformer {
        String transform(String text);   // transform = przekształć
    }

    /**
     * 1. Interfejs funkcyjny = interfejs z jedną metodą abstrakcyjną. Lambda realizuje właśnie tę metodę.
     */
    static void definition() {
        section("1. Definicja: dokładnie jedna metoda abstrakcyjna");

        TextTransformer upper = text -> text.toUpperCase();                    // upper = wielkie litery
        TextTransformer reverse = text -> new StringBuilder(text).reverse().toString();   // reverse = odwróć
        TextTransformer shout = text -> text + "!!!";                          // shout = krzyknij

        show("upper.transform(\"java\")", upper.transform("java"));
        show("reverse.transform(\"kajak\")", reverse.transform("kajak"));
        show("reverse.transform(\"lambda\")", reverse.transform("lambda"));
        show("shout.transform(\"uwaga\")", shout.transform("uwaga"));
        // WYNIK: upper.transform("java") → JAVA
        // WYNIK: reverse.transform("kajak") → kajak    ← palindrom: czytany od tyłu jest taki sam
        // WYNIK: reverse.transform("lambda") → adbmal
        // WYNIK: shout.transform("uwaga") → uwaga!!!

        // Trzy różne OBIEKTY tego samego interfejsu. Każdy ma inne „ciało” metody transform.
        // To dokładnie to, co robiłaby klasa anonimowa — tylko bez ceremonii (Lambda01FromAnonymousToLambda).
        // Kto woła transform(), nie wie i nie musi wiedzieć, czy pod spodem jest lambda, klasa anonimowa czy nazwana.
    }

    // =================================================================================================
    // 2. ADNOTACJA @FunctionalInterface
    // =================================================================================================

    /** Slugifier = zamieniacz na „slug” (fragment adresu URL, np. "moj-pierwszy-wpis"). */
    @FunctionalInterface
    interface Slugifier {
        String toSlug(String title);   // to slug = na slug; title = tytuł
    }

    /**
     * 2. {@code @FunctionalInterface} nie jest obowiązkowa — lambda zadziała i bez niej. Ale z nią kompilator
     * PILNUJE, żeby interfejs miał dokładnie jedną metodę abstrakcyjną.
     */
    static void functionalInterfaceAnnotation() {
        section("2. Adnotacja @FunctionalInterface — strażnik jednej metody");

        Slugifier slugifier = title -> title.toLowerCase(Locale.ROOT).replace(" ", "-");
        show("slug", slugifier.toSlug("Mój Pierwszy Wpis"));
        // WYNIK: slug → mój-pierwszy-wpis
        // (Locale.ROOT = reguły neutralne językowo — wynik nie zależy od języka systemu; t04_strings/Strings02Methods.)

        // Co by się stało, gdyby ktoś dopisał DRUGĄ metodę abstrakcyjną do interfejsu z adnotacją?
        //
        //   @FunctionalInterface
        //   interface Broken { String a(String s); String b(String s); }
        //
        //   → błąd kompilacji W INTERFEJSIE:
        //     „Unexpected @FunctionalInterface annotation
        //        Broken is not a functional interface
        //          multiple non-overriding abstract methods found in interface Broken”
        //     (= „Nieoczekiwana adnotacja @FunctionalInterface — Broken nie jest interfejsem funkcyjnym:
        //        znaleziono kilka metod abstrakcyjnych”)
        //
        // A gdy nie ma ŻADNEJ metody abstrakcyjnej (np. same metody default):
        //     „... is not a functional interface  no abstract method found in interface Empty”
        //     (= „nie znaleziono metody abstrakcyjnej”)
        //
        // Dlaczego to ważne? Bez adnotacji dopisanie drugiej metody też zepsuje kompilację — ale błąd pojawi się
        // w KAŻDYM miejscu, gdzie ktoś użył lambdy (może ich być 50, w różnych plikach). Z adnotacją błąd jest
        // w JEDNYM miejscu — w interfejsie — i od razu wiadomo, kto złamał umowę.

        // DOBRA PRAKTYKA: każdy własny interfejs przeznaczony dla lambd oznaczaj @FunctionalInterface.
        //   To też dokumentacja: „ten interfejs jest po to, żeby podawać w nim lambdy”.
        note("Wszystkie interfejsy funkcyjne z Javy (Runnable, Comparator, Predicate...) mają tę adnotację.");
        // WYNIK: ℹ Wszystkie interfejsy funkcyjne z Javy (Runnable, Comparator, Predicate...) mają tę adnotację.
    }

    // =================================================================================================
    // 3. CO SIĘ NIE LICZY: default, static, metody z Object
    // =================================================================================================

    /**
     * {@code Validator<T>} = walidator (sprawdzacz poprawności) dowolnego typu T.
     * Ma JEDNĄ metodę abstrakcyjną (validate) i kilka dodatkowych metod, które się nie liczą.
     */
    @FunctionalInterface
    interface Validator<T> {
        boolean validate(T value);                                  // JEDYNA metoda abstrakcyjna

        /** and = i. Metoda default (z ciałem) — NIE liczy się. Łączy dwa walidatory: oba muszą przepuścić. */
        default Validator<T> and(Validator<T> other) {
            return value -> validate(value) && other.validate(value);
        }

        /** negate = zaprzecz. Też default — zwraca walidator o odwrotnym wyniku. */
        default Validator<T> negate() {
            return value -> !validate(value);
        }

        /** notBlank = niepusty (i nie same spacje). Metoda static — też NIE liczy się. isBlank: Java 11+. */
        static Validator<String> notBlank() {
            return text -> text != null && !text.isBlank();
        }
    }

    /**
     * PriceRule = reguła cenowa: cena → nowa cena.
     * Poza apply ma DWIE metody „abstrakcyjne” z klasy Object — a mimo to jest funkcyjny.
     */
    @FunctionalInterface
    interface PriceRule {
        BigDecimal apply(BigDecimal price);    // apply = zastosuj — JEDYNA „prawdziwa” metoda abstrakcyjna

        @Override
        boolean equals(Object other);          // z klasy Object — NIE liczy się (każdy obiekt i tak ją ma)

        @Override
        String toString();                     // z klasy Object — NIE liczy się
    }

    /**
     * 3. Liczą się tylko metody abstrakcyjne, które NIE pochodzą z klasy Object.
     * <ul>
     *   <li>default — mają ciało, więc lambda nie musi ich dostarczać,</li>
     *   <li>static — należą do interfejsu, nie do obiektu,</li>
     *   <li>equals / hashCode / toString — każdy obiekt dziedziczy je z Object, więc nie trzeba ich pisać.</li>
     * </ul>
     */
    static void whatDoesNotCount() {
        section("3. Co się NIE liczy: default, static i metody z Object");

        Validator<String> shortText = text -> text.length() <= 10;             // short text = krótki tekst
        Validator<String> goodTitle = Validator.notBlank().and(shortText);     // static + default w akcji

        show("\"Java\" dobry tytuł?", goodTitle.validate("Java"));
        show("\"   \" dobry tytuł?", goodTitle.validate("   "));
        show("\"Bardzo długi tytuł\" dobry?", goodTitle.validate("Bardzo długi tytuł"));
        show("negate: \"Java\" ZŁY tytuł?", goodTitle.negate().validate("Java"));
        // WYNIK: "Java" dobry tytuł? → true
        // WYNIK: "   " dobry tytuł? → false
        // WYNIK: "Bardzo długi tytuł" dobry? → false
        // WYNIK: negate: "Java" ZŁY tytuł? → false

        // PriceRule ma w kodzie trzy metody bez ciała, ale equals i toString pochodzą z Object — nie liczą się:
        PriceRule minus10 = price -> price.subtract(BigDecimal.TEN);            // subtract = odejmij; TEN = 10
        show("minus10.apply(99.99)", minus10.apply(new BigDecimal("99.99")));
        // WYNIK: minus10.apply(99.99) → 89.99

        // Po co w ogóle deklarować equals w interfejsie? Tak robi java.util.Comparator — tylko po to, żeby dopisać
        // do equals dokumentację (Javadoc) specyficzną dla komparatorów. Na „funkcyjność” to nie wpływa.
        // Metody default and/negate to zapowiedź składania funkcji — gotowe wersje: Lambda05Composition.
    }

    // =================================================================================================
    // 4. WŁASNE INTERFEJSY — TAKŻE GENERYCZNE
    // =================================================================================================

    /**
     * accepted = zaakceptowane. Metoda generyczna (t11_generics/Generics03Methods): zwraca elementy,
     * które przepuszcza walidator. Działa dla {@code Validator<String>}, {@code Validator<Integer>}, {@code Validator<Product>}...
     */
    static <T> List<T> accepted(Validator<T> validator, List<T> items) {
        List<T> result = new ArrayList<>();
        for (T item : items) {
            if (validator.validate(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 4. Jeden generyczny interfejs {@code Validator<T>} obsłuży dowolny typ. Typ T wybierasz przy deklaracji zmiennej,
     * a kompilator na jego podstawie zna typ parametru lambdy.
     */
    static void ownGenericInterfaces() {
        section("4. Własne interfejsy funkcyjne — także generyczne");

        Validator<Integer> positive = n -> n > 0;                              // positive = dodatnia; n to Integer
        show("dodatnie z [5, -3, 0, 12]", accepted(positive, List.of(5, -3, 0, 12)));
        // WYNIK: dodatnie z [5, -3, 0, 12] → [5, 12]

        Validator<Product> lowStock = p -> p.stock() > 0 && p.stock() < 5;     // low stock = mały zapas; p to Product
        show("mały zapas (1–4 szt.)", accepted(lowStock, SampleData.products()));
        // WYNIK: mały zapas (1–4 szt.) → [Monitor 27 cali (1299.00 zł), Wzorce projektowe (99.00 zł), Ekspres do kawy (1899.00 zł)]
        // (Produkty wypisują się przez ich toString(): „nazwa (cena zł)”.)

        // PriceRule: rabat 10% — cena × 0.90, zaokrąglona do groszy.
        // setScale(2, RoundingMode.HALF_UP) = ustaw 2 miejsca po przecinku, zaokrąglając „szkolnie” (t15_numbers).
        PriceRule tenPercentOff = price -> price.multiply(NINETY_PERCENT).setScale(2, RoundingMode.HALF_UP);
        show("rabat 10% od 129.00", tenPercentOff.apply(new BigDecimal("129.00")));
        show("rabat 10% od 49.99", tenPercentOff.apply(new BigDecimal("49.99")));
        // WYNIK: rabat 10% od 129.00 → 116.10
        // WYNIK: rabat 10% od 49.99 → 44.99    ← 44.991 zaokrąglone do groszy

        // DOBRA PRAKTYKA: własny interfejs ma sens, gdy jego NAZWA coś mówi o domenie (PriceRule, Validator).
        //   Gdy chodzi tylko o „kształt” (np. T → boolean), użyj gotowego z java.util.function (Lambda03JavaUtilFunction):
        //   Predicate<T> zamiast Validator<T>, UnaryOperator<BigDecimal> zamiast PriceRule.
    }

    // =================================================================================================
    // 5. TA SAMA LAMBDA — RÓŻNE INTERFEJSY
    // =================================================================================================

    /** NameFormatter = formater imion. Ten SAM kształt co TextTransformer (String → String), inna nazwa. */
    @FunctionalInterface
    interface NameFormatter {
        String format(String name);   // format = sformatuj
    }

    /**
     * 5. Ten sam TEKST lambdy pasuje do każdego interfejsu o pasującym kształcie. Ale gotowy OBIEKT
     * jednego interfejsu nie jest obiektem drugiego — Java patrzy na NAZWĘ typu, nie na kształt.
     */
    static void sameLambdaDifferentInterfaces() {
        section("5. Ta sama lambda, różne interfejsy");

        TextTransformer upperText = s -> s.toUpperCase();
        NameFormatter upperName = s -> s.toUpperCase();                       // identyczny tekst lambdy
        show("TextTransformer", upperText.transform("anna"));
        show("NameFormatter", upperName.format("anna"));
        // WYNIK: TextTransformer → ANNA
        // WYNIK: NameFormatter → ANNA

        // Ale przypisanie jednego do drugiego NIE przejdzie, mimo identycznego kształtu:
        //   NameFormatter nf = upperText;   →  „incompatible types: TextTransformer cannot be converted to NameFormatter”
        //                                       (= „niezgodne typy: nie można zamienić TextTransformer na NameFormatter”)
        // Rozwiązanie — „adapter”: nowa lambda, która woła metodę starego obiektu:
        NameFormatter adapted = s -> upperText.transform(s);                   // adapted = dopasowany
        show("adapter (lambda)", adapted.format("piotr"));
        // WYNIK: adapter (lambda) → PIOTR
        // Krócej: upperText::transform — referencja do metody (Lambda04MethodReferences).

        // PUŁAPKA: dwa interfejsy o tym samym kształcie w jednym projekcie to często zbędny duplikat.
        //   Tu oba robią to samo, co gotowy UnaryOperator<String> (Lambda03JavaUtilFunction).
    }

    // =================================================================================================
    // 6. LAMBDA JEST OBIEKTEM
    // =================================================================================================

    /** applyAll = zastosuj wszystkie. Przepuszcza tekst przez kolejne kroki (lambdy) z listy. */
    static String applyAll(String text, List<TextTransformer> steps) {
        String result = text;
        for (TextTransformer step : steps) {
            result = step.transform(result);     // wynik jednego kroku jest wejściem następnego
        }
        return result;
    }

    /** repeat = powtórz. Metoda, która ZWRACA lambdę (fabryka zachowań; więcej: Lambda07HigherOrderFunctions). */
    static TextTransformer repeat(int times) {
        return text -> text.repeat(times);       // String.repeat — Java 11+
    }

    /**
     * 6. Lambda to zwykły obiekt: można go trzymać w zmiennej, na liście, w mapie, przekazać jako argument
     * i zwrócić z metody. Dzięki temu zachowania można układać jak klocki.
     */
    static void lambdaIsAnObject() {
        section("6. Lambda jest obiektem — zmienna, lista, mapa, argument, wynik metody");

        TextTransformer trim = s -> s.trim();                                  // trim = przytnij (usuń spacje z brzegów)
        TextTransformer upper = s -> s.toUpperCase();
        TextTransformer exclaim = s -> s + "!";                                // exclaim = wykrzyknij

        // Lista zachowań = mini-potok przetwarzania:
        List<TextTransformer> steps = List.of(trim, upper, exclaim);
        show("lista kroków na \"  hej  \"", applyAll("  hej  ", steps));
        // WYNIK: lista kroków na "  hej  " → HEJ!

        // Mapa nazwa → zachowanie = tablica poleceń (LinkedHashMap pamięta kolejność wstawiania):
        Map<String, TextTransformer> commands = new LinkedHashMap<>();
        commands.put("trim", trim);
        commands.put("upper", upper);
        commands.put("exclaim", exclaim);
        commands.put("twice", repeat(2));                                      // lambda zwrócona przez metodę
        show("dostępne polecenia", commands.keySet());
        show("polecenie \"twice\" na \"ha\"", commands.get("twice").transform("ha"));
        // WYNIK: dostępne polecenia → [trim, upper, exclaim, twice]
        // WYNIK: polecenie "twice" na "ha" → haha

        // Lambda ma klasę — generowaną przez JVM w czasie działania programu. Jej nazwa wygląda mniej więcej tak:
        //   Lambda02FunctionalInterfaces$$Lambda$14/0x0000000800c0b000   (liczby zmieniają się między uruchomieniami)
        show("nazwa klasy zawiera \"$$Lambda\"?", trim.getClass().getName().contains("$$Lambda"));
        // WYNIK: nazwa klasy zawiera "$$Lambda"? → true
        // PUŁAPKA: nigdy nie opieraj logiki na tej nazwie ani na toString() lambdy — to szczegół implementacji JVM.
        //   Jeśli zachowanie potrzebuje czytelnej nazwy, trzymaj ją obok (klucz w mapie, pole w rekordzie).
    }

    // =================================================================================================
    // 7. KLASA ABSTRAKCYJNA — LAMBDA ODPADA
    // =================================================================================================

    /** Discount = rabat. Klasa ABSTRAKCYJNA z jedną metodą abstrakcyjną, ale też z polem i konstruktorem. */
    abstract static class Discount {
        private final String name;

        Discount(String name) {
            this.name = name;
        }

        abstract BigDecimal apply(BigDecimal price);

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * 7. Lambda implementuje TYLKO interfejsy — nigdy klasę abstrakcyjną, nawet z jedną metodą abstrakcyjną.
     */
    static void abstractClassNotAllowed() {
        section("7. Lambda nie zaimplementuje klasy abstrakcyjnej");

        //   Discount d = price -> price;   →  „incompatible types: Discount is not a functional interface”
        //                                     (= „Discount nie jest interfejsem funkcyjnym”)
        // Dlaczego? Klasa abstrakcyjna może mieć konstruktor (tu z parametrem!), pola i stan. Lambda nie ma gdzie
        // przekazać argumentu konstruktora ani przechować pól. Interfejs takich rzeczy nie ma — dlatego pasuje.

        // Rozwiązanie: klasa anonimowa (ona MOŻE dziedziczyć po klasie i wywołać jej konstruktor):
        Discount blackFriday = new Discount("Black Friday") {
            @Override
            BigDecimal apply(BigDecimal price) {
                return price.subtract(FIFTY);
            }
        };
        show(blackFriday.toString(), blackFriday.apply(new BigDecimal("199.00")));
        // WYNIK: Black Friday → 149.00

        // DOBRA PRAKTYKA: projektując API dla lambd, zamiast klasy abstrakcyjnej daj interfejs funkcyjny,
        //   a dodatkowe dane (np. nazwę) przekaż osobno — np. record Promo(String name, PriceRule rule).
    }

    // =================================================================================================
    // 8. ZAPOWIEDŹ: PRZECIĄŻENIA A LAMBDY
    // =================================================================================================

    /**
     * describe = opisz. Dwie PRZECIĄŻONE metody (overload = ta sama nazwa, inne parametry), obie przyjmują
     * interfejs funkcyjny z jednym parametrem String.
     * <p>
     * Kompilator z opcją {@code -Xlint:overloads} OSTRZEGA przed taką parą: „[overloads] describe(Validator{@code <String>})
     * ... is potentially ambiguous with describe(TextTransformer)” (= „potencjalnie niejednoznaczna z ...”).
     * Tu wyciszamy to ostrzeżenie adnotacją {@code @SuppressWarnings("overloads")} na obu metodach, bo właśnie
     * ten problem chcemy pokazać.
     */
    @SuppressWarnings("overloads")
    static String describe(TextTransformer transformer) {
        return "TextTransformer → " + transformer.transform("abc");
    }

    /** describe = opisz — druga wersja przeciążenia, dla walidatora. */
    @SuppressWarnings("overloads")
    static String describe(Validator<String> validator) {
        return "Validator → " + validator.validate("abc");
    }

    /**
     * 8. Gdy metoda jest przeciążona dla dwóch interfejsów funkcyjnych o tej samej liczbie parametrów,
     * zwykła lambda bez typów bywa NIEJEDNOZNACZNA — nawet jeśli „na oko” pasuje tylko do jednej wersji.
     */
    static void overloadingPreview() {
        section("8. Zapowiedź: przeciążone metody i lambda");

        //   describe(s -> s.isEmpty());   →  „reference to describe is ambiguous
        //                                     both method describe(TextTransformer) and method describe(Validator<String>) match”
        //                                     (= „odwołanie do describe jest niejednoznaczne — pasują obie metody”)
        // Dlaczego? Lambda bez typów parametrów ma dla kompilatora tylko „kształt” — 1 parametr. Obie wersje go mają.
        // Kompilator nie zagląda do ciała lambdy (że zwraca boolean), żeby wybrać wersję.

        // Rozwiązanie 1: jawny typ parametru — wtedy kompilator sprawdza też typ wyniku:
        show("(String s) -> s.isEmpty()", describe((String s) -> s.isEmpty()));
        // WYNIK: (String s) -> s.isEmpty() → Validator → false

        // Rozwiązanie 2: rzutowanie na konkretny interfejs:
        show("(TextTransformer) s -> ...", describe((TextTransformer) s -> s.toUpperCase()));
        // WYNIK: (TextTransformer) s -> ... → TextTransformer → ABC

        // DOBRA PRAKTYKA: najlepiej NIE przeciążać metod różnymi interfejsami funkcyjnymi na tej samej pozycji.
        //   Nazwij je inaczej: describeTransformer(...), describeValidator(...). Więcej: Lambda08Pitfalls.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Interfejs funkcyjny = DOKŁADNIE JEDNA metoda abstrakcyjna. Tylko tam pasuje lambda.
     *   • NIE liczą się: metody default, metody static, metody z Object (equals, hashCode, toString).
     *   • @FunctionalInterface — nieobowiązkowa, ale kompilator pilnuje „jednej metody”; błąd pojawia się
     *     w interfejsie, a nie w 50 miejscach użycia. Oznaczaj nią własne interfejsy dla lambd.
     *   • Nazwa metody interfejsu jest dowolna; liczy się KSZTAŁT (parametry + wynik).
     *   • Własne interfejsy mogą być generyczne: Validator<T> — typ T ustalasz przy deklaracji zmiennej.
     *   • Ta sama lambda pasuje do różnych interfejsów o tym samym kształcie, ale obiektu A nie przypiszesz do B
     *     (adapter: b = x -> a.metoda(x)  albo  a::metoda).
     *   • Lambda = obiekt: zmienna, lista, mapa, argument, wynik metody.
     *   • Klasa abstrakcyjna (nawet z jedną metodą) → NIE lambda, tylko klasa anonimowa.
     *   • Przeciążenia z interfejsami funkcyjnymi → niejednoznaczność; ratunek: jawny typ parametru, rzutowanie,
     *     a najlepiej różne nazwy metod.
     *   • Własny interfejs, gdy nazwa niesie sens domenowy; w pozostałych przypadkach gotowce z java.util.function.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym jest interfejs funkcyjny? Dlaczego lambda pasuje tylko do takiego interfejsu?
     *   2. Czy ten interfejs jest funkcyjny?
     *          interface Shape { double area(); default String name() { return "?"; } static Shape unit() { return () -> 1; } String toString(); }
     *   3. Po co pisać @FunctionalInterface, skoro lambda działa i bez tej adnotacji?
     *   4. ZNAJDŹ BŁĄD:
     *          abstract class Shape { abstract double area(); }
     *          Shape s = () -> 3.14;
     *   5. Co wypisze ten kod?
     *          TextTransformer twice = s -> s + s;
     *          String r = "a";
     *          for (TextTransformer t : List.of(twice, twice, twice)) { r = t.transform(r); }
     *          System.out.println(r);
     *   6. ZNAJDŹ BŁĄD:
     *          interface A { String go(String s); }
     *          interface B { String go(String s); }
     *          A a = s -> s.trim();
     *          B b = a;
     *   7. Dlaczego  describe(s -> s.isEmpty())  się nie kompiluje, skoro tylko Validator zwraca boolean? Jak to naprawić?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        List<String> skuCandidates = List.of("ELE-001", "ele-001", "ELE-01", "KSI-123", "ABC-12X", "DOM-002");
        List<String> expected2 = List.of("ELE-001", "KSI-123", "DOM-002");
        List<BigDecimal> prices = List.of(new BigDecimal("100.00"), new BigDecimal("15.00"), new BigDecimal("20.00"));
        List<BigDecimal> expected3 = List.of(new BigDecimal("80.00"), BigDecimal.ZERO, BigDecimal.ZERO);

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: slug z tytułu", "lambda-to-funkcja", () -> exercise1().transform("Lambda To Funkcja"));
        Check.equal("ćw. 2: walidator kodów SKU", expected2, () -> accepted(exercise2(), skuCandidates));
        Check.equal("ćw. 3: klasa anonimowa → lambda (minus 20 zł)", expected3, () -> applyRule(exercise3(), prices));
        Check.equal("ćw. 4a: tablica poleceń", "71 AVAJ", () -> exercise4("  Java 17  ", List.of("trim", "upper", "reverse")));
        Check.throwsException("ćw. 4b: nieznane polecenie → wyjątek", IllegalArgumentException.class,
                () -> exercise4("x", List.of("shout")));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "lambda-to-funkcja", () -> solution1().transform("Lambda To Funkcja"));
        Check.equal("ćw. 2 (wzorzec)", expected2, () -> accepted(solution2(), skuCandidates));
        Check.equal("ćw. 3 (wzorzec)", expected3, () -> applyRule(solution3(), prices));
        Check.equal("ćw. 4a (wzorzec)", "71 AVAJ", () -> solution4("  Java 17  ", List.of("trim", "upper", "reverse")));
        Check.throwsException("ćw. 4b (wzorzec)", IllegalArgumentException.class, () -> solution4("x", List.of("shout")));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /** applyRule = zastosuj regułę do każdej ceny z listy (pomocnicza metoda do sprawdzania ćwiczenia 3). */
    static List<BigDecimal> applyRule(PriceRule rule, List<BigDecimal> prices) {
        List<BigDecimal> result = new ArrayList<>();
        for (BigDecimal price : prices) {
            result.add(rule.apply(price));
        }
        return result;
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć TextTransformer, który zamienia tytuł na „slug”: małe litery,
     * a spacje zamienione na myślniki. "Lambda To Funkcja" → "lambda-to-funkcja".
     * Podpowiedź: {@code toLowerCase(Locale.ROOT)}, potem {@code replace(" ", "-")}. Zwróć lambdę z jednym parametrem.
     */
    static TextTransformer exercise1() {
        // TODO: twoje rozwiązanie
        return text -> "";
    }

    /**
     * ĆWICZENIE 2 (łatwe): zwróć {@code Validator<String>}, który przepuszcza tylko poprawne kody SKU:
     * trzy WIELKIE litery, myślnik, trzy cyfry (np. "ELE-001"). Łączy lambdy z wyrażeniami regularnymi (t04_strings/Strings05Regex).
     * Podpowiedź: {@code text.matches("[A-Z]{3}-\\d{3}")} — [A-Z]{3} = trzy wielkie litery, \d{3} = trzy cyfry
     * (w napisie Javy backslash piszemy podwójnie).
     */
    static Validator<String> exercise2() {
        // TODO: twoje rozwiązanie
        return text -> false;
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ KLASĘ ANONIMOWĄ NA LAMBDĘ. Reguła „promocja −20 zł, ale cena nie może spaść
     * poniżej zera” jest zapisana tak:
     * <pre>{@code
     *   BigDecimal twenty = new BigDecimal("20");
     *   return new PriceRule() {
     *       @Override
     *       public BigDecimal apply(BigDecimal price) {
     *           BigDecimal reduced = price.subtract(twenty);
     *           return reduced.max(BigDecimal.ZERO);
     *       }
     *   };
     * }</pre>
     * Zwróć tę samą regułę jako lambdę (może być z ciałem blokowym albo jako jedno wyrażenie).
     * Podpowiedź: max = większa z dwóch wartości; {@code reduced.max(BigDecimal.ZERO)} daje 0, gdy reduced jest ujemne.
     * Interfejs PriceRule deklaruje też equals i toString — czy to przeszkadza lambdzie? (sekcja 3)
     */
    static PriceRule exercise3() {
        // TODO: twoje rozwiązanie
        return price -> BigDecimal.ONE;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zbuduj mapę poleceń (nazwa → TextTransformer): "trim" (przytnij spacje),
     * "upper" (wielkie litery), "lower" (małe litery), "reverse" (odwróć napis). Potem wykonaj na tekście po kolei
     * polecenia z listy commands i zwróć wynik. Dla nieznanego polecenia rzuć
     * {@code IllegalArgumentException("Nieznane polecenie: " + nazwa)}.
     * Przykład: "  Java 17  " + [trim, upper, reverse] → "71 AVAJ".
     * Podpowiedź: sekcja 6 (mapa zachowań). {@code map.get(nazwa)} zwraca null, gdy klucza nie ma — sprawdź to przed wywołaniem.
     */
    static String exercise4(String text, List<String> commands) {
        // TODO: twoje rozwiązanie
        return "";
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static TextTransformer solution1() {
        return text -> text.toLowerCase(Locale.ROOT).replace(" ", "-");
    }

    static Validator<String> solution2() {
        return text -> text.matches("[A-Z]{3}-\\d{3}");
    }

    static PriceRule solution3() {
        BigDecimal twenty = new BigDecimal("20");                          // stała poza lambdą — tworzona raz
        return price -> price.subtract(twenty).max(BigDecimal.ZERO);
        // equals i toString z interfejsu PriceRule pochodzą z Object — lambda ich nie musi (i nie może) dostarczać.
    }

    static String solution4(String text, List<String> commands) {
        Map<String, TextTransformer> available = new LinkedHashMap<>();   // available = dostępne
        available.put("trim", s -> s.trim());
        available.put("upper", s -> s.toUpperCase());
        available.put("lower", s -> s.toLowerCase());
        available.put("reverse", s -> new StringBuilder(s).reverse().toString());

        String result = text;
        for (String command : commands) {
            TextTransformer transformer = available.get(command);
            if (transformer == null) {                                      // fail fast — od razu zgłoś błąd
                throw new IllegalArgumentException("Nieznane polecenie: " + command);
            }
            result = transformer.transform(result);
        }
        return result;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Interfejs z dokładnie jedną metodą abstrakcyjną (nie licząc default, static i metod z Object).
     *      Lambda to tylko ciało jednej metody — kompilator musi jednoznacznie wiedzieć, którą metodę realizuje.
     *   2. Tak. Jedyna metoda abstrakcyjna to area(); name() jest default, unit() jest static, a toString() pochodzi
     *      z Object. (Nawiasem: unit() zwraca lambdę () -> 1 — liczba 1 zamieni się na 1.0, bo area() zwraca double.)
     *   3. Kompilator pilnuje, żeby interfejs miał dokładnie jedną metodę abstrakcyjną. Gdy ktoś doda drugą, błąd
     *      pojawi się w interfejsie, a nie we wszystkich miejscach z lambdami. To też dokumentacja przeznaczenia.
     *   4. Shape to klasa ABSTRAKCYJNA, a lambda implementuje tylko interfejsy („Shape is not a functional interface”).
     *      Trzeba zmienić Shape na interfejs albo użyć klasy anonimowej.
     *   5. aaaaaaaa (8 liter): "a" → "aa" → "aaaa" → "aaaaaaaa".
     *   6. Obiekt typu A nie jest typu B, mimo identycznego kształtu („A cannot be converted to B”).
     *      Adapter: B b = s -> a.go(s);  albo  B b = a::go;
     *   7. Lambda bez typów parametrów jest dla kompilatora tylko „czymś z jednym parametrem” — pasuje do obu
     *      przeciążeń, a ciała do wyboru wersji nie używa. Naprawa: (String s) -> s.isEmpty(), rzutowanie
     *      (Validator<String>) s -> s.isEmpty(), a najlepiej — różne nazwy metod zamiast przeciążenia.
     */
    // </editor-fold>
}
