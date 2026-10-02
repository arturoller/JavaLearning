package t19_annotations_reflection;

import helpers.Check;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Mini framework walidacji — adnotacje na polach + refleksja = Bean Validation w miniaturze
 *        (validation = walidacja, sprawdzanie poprawności; constraint = ograniczenie; violation = naruszenie)
 *
 * W SKRÓCIE:
 *   Zamiast pisać dziesiątki if-ów w każdym formularzu, opisujesz reguły adnotacjami na polach: {@code @NotBlank},
 *   {@code @Min(18)}, {@code @Size(min = 3, max = 20)}, {@code @Pattern(regex = "...")}. Jeden uniwersalny Validator
 *   czyta refleksją pola dowolnego obiektu, znajduje adnotacje i zwraca posortowaną listę naruszeń „pole: komunikat”.
 *   Dokładnie tak działa Jakarta Bean Validation, której Spring używa przy {@code @Valid}.
 *
 * ANALOGIA: kontrola na lotnisku.
 *   Na bagażu wiszą naklejki „max 23 kg”, „płyny ≤ 100 ml” (adnotacje). Kontroler (Validator) nie zna Twojej walizki,
 *   ale umie czytać naklejki: waży, mierzy i wypisuje listę zastrzeżeń. Nowa zasada = nowa naklejka i nowy punkt
 *   w instrukcji kontrolera — walizek nikt nie przerabia.
 *
 * JAK TO DZIAŁA:
 *   dla każdego pola obiektu (getDeclaredFields):
 *       wartość = pole.get(obiekt)                    ← setAccessible(true), bo pola są prywatne
 *       dla każdej adnotacji na polu:
 *           reguła = rules.get(adnotacja.annotationType())    ← mapa: typ adnotacji → reguła
 *           komunikat = reguła.check(adnotacja, wartość)      ← null = OK
 *           komunikat != null → dodaj "pole: komunikat"
 *   na końcu: sortuj (kolejność pól z refleksji nie jest gwarantowana)
 *
 * SŁÓWKA:
 *   validator = walidator; validate = sprawdź poprawność; violation = naruszenie; constraint = ograniczenie;
 *   rule = reguła; not blank = niepusty (nie same spacje); min/max = najmniej/najwięcej; size = rozmiar;
 *   pattern = wzorzec; regex = wyrażenie regularne; message = komunikat; form = formularz; allowed values = dozwolone
 *   wartości; valid = poprawny (tu: „waliduj też obiekt w środku”); bean = ziarno (obiekt z polami).
 *
 * ZOBACZ TEŻ: t19_annotations_reflection/Annotations02Custom (deklarowanie adnotacji, rekordy),
 *             t19_annotations_reflection/Annotations03ReflectionBasics (pola, setAccessible),
 *             t09_records/Records02Constructors (walidacja w konstruktorze), t16_streams/Streams11GroupingBy.
 * </pre>
 */
public class Annotations04Validator {

    // ---------------------------------------------------------------------------------------------
    // Adnotacje-ograniczenia (wszystkie RUNTIME — walidator czyta je refleksją)
    // ---------------------------------------------------------------------------------------------

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface NotBlank {
        String message() default "nie może być puste";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Min {
        long value();
        String message() default "";          // "" = komunikat domyślny wygenerowany przez walidator
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Max {
        long value();
        String message() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Size {
        int min() default 0;
        int max() default Integer.MAX_VALUE;
        String message() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Pattern {
        String regex();
        String message() default "ma zły format";
    }

    // ---------------------------------------------------------------------------------------------
    // Formularze do sprawdzania
    // ---------------------------------------------------------------------------------------------

    /** UserForm = formularz użytkownika (zwykła klasa, pola prywatne). */
    static final class UserForm {
        @NotBlank
        @Size(min = 3, max = 20)
        private final String login;

        @NotBlank
        @Pattern(regex = "[\\w.]+@[\\w.]+\\.[a-z]{2,}", message = "to nie jest poprawny e-mail")
        private final String email;

        @Min(18)
        @Max(130)
        private final int age;

        @Size(max = 3, message = "najwyżej 3 zainteresowania")
        private final List<String> interests;            // interests = zainteresowania

        UserForm(String login, String email, int age, List<String> interests) {
            this.login = login;
            this.email = email;
            this.age = age;
            this.interests = interests;
        }
    }

    /** ProductForm = formularz produktu (rekord). Adnotacje ze składników „spływają” na pola (Target = FIELD). */
    record ProductForm(@NotBlank String name, @Min(1) int quantity, @Max(10000) BigDecimal price,
                       @Pattern(regex = "[A-Z]{3}-\\d{3}") String sku) {
    }

    static final UserForm VALID_USER = new UserForm("ala_nowak", "ala@example.com", 34, List.of("java", "góry"));
    static final UserForm BAD_USER = new UserForm("Al", "ala-at-example", 15, List.of("a", "b", "c", "d"));
    static final UserForm EMPTY_USER = new UserForm("  ", null, 200, List.of());

    // ---------------------------------------------------------------------------------------------
    // Walidator
    // ---------------------------------------------------------------------------------------------

    /** Rule = reguła dla adnotacji typu A: zwraca komunikat naruszenia albo null, gdy wartość jest poprawna. */
    @FunctionalInterface
    interface Rule<A extends Annotation> {
        String check(A annotation, Object value);
    }

    /** readField = odczytaj pole (także prywatne) — wyjątek sprawdzany opakowany w IllegalStateException. */
    static Object readField(Field field, Object bean) {
        try {
            field.setAccessible(true);
            return field.get(bean);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Brak dostępu do pola " + field.getName(), e);
        }
    }

    /** Validator = walidator: mapa „typ adnotacji → reguła” + pętla po polach. Nowa reguła = nowy wpis w mapie. */
    static final class Validator {
        private static final Validator STANDARD = standard();

        private final Map<Class<? extends Annotation>, Rule<Annotation>> rules = new LinkedHashMap<>();

        /** with = z (regułą): rejestruje regułę. type.cast daje bezpieczne rzutowanie — bez ostrzeżenia unchecked. */
        <A extends Annotation> Validator with(Class<A> type, Rule<A> rule) {
            rules.put(type, (annotation, value) -> rule.check(type.cast(annotation), value));
            return this;
        }

        /** standard = zestaw standardowych reguł. */
        static Validator standard() {
            return new Validator()
                    .with(NotBlank.class, Validator::checkNotBlank)
                    .with(Min.class, Validator::checkMin)
                    .with(Max.class, Validator::checkMax)
                    .with(Size.class, Validator::checkSize)
                    .with(Pattern.class, Validator::checkPattern);
        }

        /** validate = sprawdź obiekt standardowymi regułami; zwraca POSORTOWANĄ listę "pole: komunikat". */
        static List<String> validate(Object bean) {
            return STANDARD.check(bean);
        }

        List<String> check(Object bean) {
            List<String> violations = new ArrayList<>();
            for (Field field : bean.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;                                        // stałe i pola kompilatora pomijamy
                }
                Object value = readField(field, bean);
                for (Annotation annotation : field.getAnnotations()) {
                    Rule<Annotation> rule = rules.get(annotation.annotationType());
                    if (rule == null) {
                        continue;                                    // nieznana adnotacja — nie nasza sprawa
                    }
                    String message;
                    try {
                        message = rule.check(annotation, value);
                    } catch (IllegalStateException e) {
                        throw new IllegalStateException("Źle użyta adnotacja na polu " + field.getName() + ": "
                                + e.getMessage(), e);
                    }
                    if (message != null) {
                        violations.add(field.getName() + ": " + message);
                    }
                }
            }
            Collections.sort(violations);
            return violations;
        }

        static String checkNotBlank(NotBlank rule, Object value) {
            if (value != null && !(value instanceof String)) {
                throw new IllegalStateException("@NotBlank wymaga tekstu, a pole ma typ " + value.getClass().getSimpleName());
            }
            return value == null || ((String) value).isBlank() ? rule.message() : null;   // isBlank (Java 11+)
        }

        static BigDecimal toNumber(String annotationName, Object value) {
            if (!(value instanceof Number)) {
                throw new IllegalStateException(annotationName + " wymaga liczby, a pole ma typ "
                        + value.getClass().getSimpleName());
            }
            return new BigDecimal(value.toString());   // działa dla int, long, double i BigDecimal
        }

        static String checkMin(Min rule, Object value) {
            if (value == null || toNumber("@Min", value).compareTo(BigDecimal.valueOf(rule.value())) >= 0) {
                return null;                                         // null nie jest „za mały” — jak w Jakarta
            }
            return rule.message().isEmpty() ? "musi być ≥ " + rule.value() : rule.message();
        }

        static String checkMax(Max rule, Object value) {
            if (value == null || toNumber("@Max", value).compareTo(BigDecimal.valueOf(rule.value())) <= 0) {
                return null;
            }
            return rule.message().isEmpty() ? "musi być ≤ " + rule.value() : rule.message();
        }

        static String checkSize(Size rule, Object value) {
            if (value == null) {
                return null;
            }
            int size = value instanceof String text ? text.length()
                    : value instanceof Collection<?> collection ? collection.size() : -1;
            if (size < 0) {
                throw new IllegalStateException("@Size wymaga tekstu lub kolekcji, a pole ma typ "
                        + value.getClass().getSimpleName());
            }
            if (size >= rule.min() && size <= rule.max()) {
                return null;
            }
            String generated = rule.max() == Integer.MAX_VALUE ? "rozmiar musi być ≥ " + rule.min()
                    : "rozmiar musi być od " + rule.min() + " do " + rule.max();
            return rule.message().isEmpty() ? generated : rule.message();
        }

        static String checkPattern(Pattern rule, Object value) {
            if (value == null) {
                return null;
            }
            if (!(value instanceof String text)) {
                throw new IllegalStateException("@Pattern wymaga tekstu, a pole ma typ " + value.getClass().getSimpleName());
            }
            return text.matches(rule.regex()) ? null : rule.message();   // matches = CAŁY tekst pasuje do wzorca
        }
    }

    public static void main(String[] args) {
        title("Annotations04 — mini framework walidacji");

        constraintAnnotations();   // constraint annotations = adnotacje-ograniczenia
        manualValidation();        // manual validation = walidacja ręczna
        stepByStep();              // step by step = krok po kroku
        fullValidator();           // full validator = pełny walidator
        validatingRecords();       // validating records = walidacja rekordów
        nullsAndMisuse();          // nulls and misuse = null i złe użycie
        customRule();              // custom rule = własna reguła
        jakartaComparison();       // Jakarta comparison = porównanie z Jakarta Bean Validation
        exercises();               // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. ADNOTACJE-OGRANICZENIA
    // =================================================================================================

    /** names = posortowane proste nazwy adnotacji (do wypisywania). */
    static List<String> names(Annotation[] annotations) {
        return Arrays.stream(annotations).map(a -> a.annotationType().getSimpleName()).sorted().toList();
    }

    /** 1. Reguły są zapisane PRZY polach. Formularz nie zawiera ani jednej linijki sprawdzającej. */
    static void constraintAnnotations() {
        section("1. Adnotacje-ograniczenia na polach");

        Field[] fields = UserForm.class.getDeclaredFields();
        Arrays.sort(fields, Comparator.comparing(Field::getName));
        for (Field field : fields) {
            show(field.getName(), names(field.getAnnotations()));
        }
        // WYNIK: age → [Max, Min]
        // WYNIK: email → [NotBlank, Pattern]
        // WYNIK: interests → [Size]
        // WYNIK: login → [NotBlank, Size]

        // DOBRA PRAKTYKA: reguła przy polu jest jednocześnie DOKUMENTACJĄ — czytając klasę, od razu widać,
        //   jakie wartości są dozwolone. Przy if-ach rozsianych po serwisach trzeba ich szukać.
    }

    // =================================================================================================
    // 2. PRZED: WALIDACJA RĘCZNA
    // =================================================================================================

    /** validateManually = ręczna walidacja tylko DWÓCH pól. Wyobraź sobie 30 formularzy po 10 pól. */
    static List<String> validateManually(UserForm form) {
        List<String> errors = new ArrayList<>();
        if (form.login == null || form.login.isBlank()) {
            errors.add("login: nie może być puste");
        }
        if (form.login != null && (form.login.length() < 3 || form.login.length() > 20)) {
            errors.add("login: rozmiar musi być od 3 do 20");
        }
        if (form.age < 18) {
            errors.add("age: musi być ≥ 18");
        }
        Collections.sort(errors);
        return errors;
    }

    /** 2. PRZED: if-y piszemy dla każdego pola każdego formularza. Łatwo o pomyłkę i niespójne komunikaty. */
    static void manualValidation() {
        section("2. PRZED: walidacja ręczna if-ami");

        show("ręcznie (BAD_USER, tylko 2 pola)", validateManually(BAD_USER));
        // WYNIK: ręcznie (BAD_USER, tylko 2 pola) → [age: musi być ≥ 18, login: rozmiar musi być od 3 do 20]

        // PUŁAPKA: ręczna walidacja „rozjeżdża się” z kodem — ktoś doda pole phone i zapomni o if-ie, a komunikaty
        //   są niespójne. Reguły w adnotacjach są przy polu, a komunikaty generuje jeden walidator.
    }

    // =================================================================================================
    // 3. KROK PO KROKU
    // =================================================================================================

    /** validateNotBlankOnly = najprostszy walidator refleksyjny: obsługuje tylko @NotBlank. */
    static List<String> validateNotBlankOnly(Object bean) {
        List<String> violations = new ArrayList<>();
        for (Field field : bean.getClass().getDeclaredFields()) {             // 1. wszystkie pola
            NotBlank notBlank = field.getAnnotation(NotBlank.class);          // 2. czy pole ma adnotację?
            if (notBlank == null) {
                continue;
            }
            Object value = readField(field, bean);                            // 3. odczytaj wartość
            if (value == null || value.toString().isBlank()) {                // 4. sprawdź regułę
                violations.add(field.getName() + ": " + notBlank.message());  // 5. zapisz naruszenie
            }
        }
        Collections.sort(violations);                                         // 6. stała kolejność
        return violations;
    }

    /** 3. Sześć kroków walidatora na przykładzie jednej adnotacji. Pełna wersja tylko dokłada reguły. */
    static void stepByStep() {
        section("3. Jak działa walidator — krok po kroku");

        show("tylko @NotBlank (EMPTY_USER)", validateNotBlankOnly(EMPTY_USER));
        show("tylko @NotBlank (VALID_USER)", validateNotBlankOnly(VALID_USER));
        // WYNIK: tylko @NotBlank (EMPTY_USER) → [email: nie może być puste, login: nie może być puste]
        // WYNIK: tylko @NotBlank (VALID_USER) → []

        // Walidator nie zna klasy UserForm — działa dla KAŻDEGO obiektu z adnotacjami. To jest istota frameworka:
        // kod piszesz raz, a działa na klasach, których autor frameworka nigdy nie widział.
    }

    // =================================================================================================
    // 4. PEŁNY WALIDATOR
    // =================================================================================================

    /** 4. Validator.validate(obiekt) — wszystkie reguły, wynik posortowany. */
    static void fullValidator() {
        section("4. PO: Validator.validate(obiekt)");

        show("VALID_USER", Validator.validate(VALID_USER));
        // WYNIK: VALID_USER → []
        showEach("BAD_USER", Validator.validate(BAD_USER));
        // WYNIK: BAD_USER (liczba elementów: 4):
        // WYNIK:    • age: musi być ≥ 18
        // WYNIK:    • email: to nie jest poprawny e-mail
        // WYNIK:    • interests: najwyżej 3 zainteresowania
        // WYNIK:    • login: rozmiar musi być od 3 do 20
        showEach("EMPTY_USER", Validator.validate(EMPTY_USER));
        // WYNIK: EMPTY_USER (liczba elementów: 4):
        // WYNIK:    • age: musi być ≤ 130
        // WYNIK:    • email: nie może być puste
        // WYNIK:    • login: nie może być puste
        // WYNIK:    • login: rozmiar musi być od 3 do 20    ← „  ” ma 2 znaki: jedno pole, dwa naruszenia

        // DOBRA PRAKTYKA: zwracaj WSZYSTKIE naruszenia naraz, a nie tylko pierwsze — użytkownik poprawi formularz
        //   za jednym razem, zamiast wysyłać go pięć razy.
    }

    // =================================================================================================
    // 5. REKORDY
    // =================================================================================================

    /** 5. Rekord też ma pola (prywatne, final). Adnotacja z Target = FIELD trafia ze składnika na pole. */
    static void validatingRecords() {
        section("5. Walidacja rekordów");

        ProductForm good = new ProductForm("Kawa ziarnista 1kg", 2, new BigDecimal("64.99"), "SPO-001");
        ProductForm bad = new ProductForm("", 0, new BigDecimal("12000.00"), "spo-1");
        show("good", Validator.validate(good));
        showEach("bad", Validator.validate(bad));
        // WYNIK: good → []
        // WYNIK: bad (liczba elementów: 4):
        // WYNIK:    • name: nie może być puste
        // WYNIK:    • price: musi być ≤ 10000
        // WYNIK:    • quantity: musi być ≥ 1
        // WYNIK:    • sku: ma zły format

        // Rekord pozwolił UTWORZYĆ niepoprawny obiekt. Walidacja w konstruktorze kompaktowym (t09_records) by temu
        // zapobiegła — ale formularz z sieci chcemy najpierw zbudować, a potem oddać listę WSZYSTKICH błędów.
    }

    // =================================================================================================
    // 6. NULL I ZŁE UŻYCIE
    // =================================================================================================

    /** OptionalAgeForm = wiek opcjonalny (Integer może być null). */
    record OptionalAgeForm(@Min(18) Integer age) {
    }

    /** BrokenForm = źle użyta adnotacja: @Min na polu tekstowym. Kompilator tego NIE wykryje. */
    record BrokenForm(@Min(1) String count) {
    }

    /** 6. null przechodzi przez @Min/@Max/@Size/@Pattern. Adnotacja na złym typie wychodzi dopiero w działaniu. */
    static void nullsAndMisuse() {
        section("6. null i źle użyte adnotacje");

        show("@Min(18) dla age = null", Validator.validate(new OptionalAgeForm(null)));
        show("@Min(18) dla age = 16", Validator.validate(new OptionalAgeForm(16)));
        // WYNIK: @Min(18) dla age = null → []    ← null NIE jest naruszeniem @Min!
        // WYNIK: @Min(18) dla age = 16 → [age: musi być ≥ 18]

        expectThrows("@Min na polu String", () -> Validator.validate(new BrokenForm("5")));
        // WYNIK: ✔ @Min na polu String → rzucono IllegalStateException: Źle użyta adnotacja na polu count: @Min wymaga liczby, a pole ma typ String

        // PUŁAPKA: „mam @Min(18), a null przechodzi” — tak samo jest w Jakarta Bean Validation. Każda reguła sprawdza
        //   JEDNĄ rzecz; brak wartości łapie osobna adnotacja (tam: @NotNull), więc pole opcjonalne może mieć @Min.
        // PUŁAPKA: kompilator nie wie, że @Min ma sens tylko dla liczb — Target mówi „pole”, a nie „pole liczbowe”.
        //   Błąd wychodzi dopiero przy walidacji; wcześniej złapie go tylko procesor adnotacji (Annotations07Processors).
    }

    // =================================================================================================
    // 7. WŁASNA REGUŁA
    // =================================================================================================

    /** AllowedValues = dozwolone wartości (nowa adnotacja, standardowy walidator jej nie zna). */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface AllowedValues {
        String[] value();
    }

    /** PaymentForm = formularz płatności (currency = waluta). */
    record PaymentForm(@NotBlank @AllowedValues({"PLN", "EUR"}) String currency) {
    }

    /** 7. Rozszerzanie: nowa adnotacja + nowa reguła w mapie. Pętla walidatora zostaje bez zmian. */
    static void customRule() {
        section("7. Własna reguła — rozszerzanie walidatora");

        PaymentForm usd = new PaymentForm("USD");
        show("standardowy walidator", Validator.validate(usd));
        // WYNIK: standardowy walidator → []    ← nieznana adnotacja jest po cichu pomijana!

        Validator extended = Validator.standard().with(AllowedValues.class, (rule, value) ->
                value == null || Arrays.asList(rule.value()).contains(value)
                        ? null : "dozwolone wartości: " + Arrays.toString(rule.value()));
        show("z regułą AllowedValues", extended.check(usd));
        show("z regułą AllowedValues (PLN)", extended.check(new PaymentForm("PLN")));
        // WYNIK: z regułą AllowedValues → [currency: dozwolone wartości: [PLN, EUR]]
        // WYNIK: z regułą AllowedValues (PLN) → []

        // PUŁAPKA: adnotacja, której nikt nie obsługuje, NIE daje błędu — po prostu nic nie robi (lekcja 1).
        //   Literówka w nazwie własnej adnotacji albo zapomniana rejestracja reguły = brak walidacji.
        // DOBRA PRAKTYKA: zasada otwarte-zamknięte (open-closed, t07_inheritance_polymorphism/Inherit08Solid):
        //   walidator jest otwarty na nowe reguły (with), a zamknięty na zmiany pętli — nikt jej nie edytuje.
    }

    // =================================================================================================
    // 8. JAKARTA BEAN VALIDATION I SPRING
    // =================================================================================================

    /** 8. Nasz mini framework ↔ prawdziwy standard. Nazwy i zachowanie są celowo takie same. */
    static void jakartaComparison() {
        section("8. Porównanie z Jakarta Bean Validation (Spring @Valid)");

        // Tak (w dużym uproszczeniu) Spring obsługuje @Valid, zanim wywoła metodę kontrolera:
        Function<Object, String> post = form -> Validator.validate(form).isEmpty()
                ? "201 Created" : "400 Bad Request " + Validator.validate(form);
        show("POST /users (VALID_USER)", post.apply(VALID_USER));
        show("POST /users (EMPTY_USER)", post.apply(EMPTY_USER));
        // WYNIK: POST /users (VALID_USER) → 201 Created
        // WYNIK: POST /users (EMPTY_USER) → 400 Bad Request [age: musi być ≤ 130, email: nie może być puste, login: nie może być puste, login: rozmiar musi być od 3 do 20]

        // NASZ MINI FRAMEWORK             JAKARTA BEAN VALIDATION
        // @NotBlank @Min @Max @Size       te same nazwy (pakiet jakarta.validation.constraints)
        // @Pattern(regex = ...)           @Pattern(regexp = ...)
        // Validator.validate(obj)         validator.validate(obj) → zbiór (Set) obiektów ConstraintViolation
        // "pole: komunikat"               v.getPropertyPath() + ": " + v.getMessage()
        // null przechodzi przez @Min      tak samo; brak wartości łapie @NotNull
        // @Valid (ćwiczenie 4)            @Valid — walidacja obiektów zagnieżdżonych (kaskadowa)

        // W Spring Boot (zależność spring-boot-starter-validation, implementacja: Hibernate Validator):
        //   @PostMapping("/users")
        //   ResponseEntity<?> create(@Valid @RequestBody UserForm form) { ... }
        // Spring przed wywołaniem metody robi to, co nasze Validator.validate(form). Przy naruszeniach metoda
        // w ogóle się nie wykona, a klient dostaje odpowiedź 400 Bad Request z listą błędów.

        // DOBRA PRAKTYKA: w prawdziwym projekcie używaj Jakarta Bean Validation, nie własnego walidatora —
        //   ten mini framework jest po to, żebyś rozumiał, co dzieje się „pod maską” @Valid.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Adnotacje-ograniczenia: @Retention(RUNTIME) + @Target(FIELD); elementy: wartości graniczne + message.
     *   • Walidator: getDeclaredFields → setAccessible → get → dla każdej adnotacji reguła z mapy → "pole: komunikat".
     *   • Sortuj wynik (kolejność pól niegwarantowana). Rekordy działają: adnotacja spływa na pole (Target = FIELD).
     *   • null przechodzi przez @Min/@Max/@Size/@Pattern. Nieznana adnotacja = ignorowana; zły typ = błąd w działaniu.
     *   • Rozszerzanie: mapa typ adnotacji → reguła (with) — pętla bez zmian (open-closed).
     *   • W praktyce: Jakarta Bean Validation (Hibernate Validator), w Springu @Valid.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego adnotacje-ograniczenia muszą mieć @Retention(RetentionPolicy.RUNTIME)?
     *   2. Co wypisze:  System.out.println(Validator.validate(new OptionalAgeForm(null)));  ?
     *   3. ZNAJDŹ BŁĄD:  @Min(1) private String quantity;  — co się stanie i kiedy?
     *   4. Co wypisze:  System.out.println(Validator.validate(new PaymentForm("USD")));  ?  Dlaczego?
     *   5. ZNAJDŹ BŁĄD:  w checkMax porównanie  ((Number) value).intValue() <= rule.value()  dla BigDecimal 10000.50.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** Valid = „waliduj też obiekt w tym polu” (do ćwiczenia 4). */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Valid {
    }

    /** AddressForm = adres (city = miasto, zip = kod pocztowy). */
    record AddressForm(@NotBlank String city,
                       @Pattern(regex = "\\d{2}-\\d{3}", message = "kod pocztowy ma format 00-000") String zip) {
    }

    /** CustomerForm = klient z adresem; @Valid = sprawdź także obiekt w polu address. */
    record CustomerForm(@NotBlank String name, @Valid AddressForm address) {
    }

    static void exercises() {
        Pattern emailPattern = Arrays.stream(UserForm.class.getDeclaredFields())
                .filter(f -> f.getName().equals("email")).findFirst().orElseThrow().getAnnotation(Pattern.class);
        Map<String, Long> expectedCounts = new TreeMap<>(Map.of("age", 2L, "email", 2L, "interests", 1L, "login", 3L));
        List<Object> forms = List.of(VALID_USER, BAD_USER, EMPTY_USER);
        CustomerForm badCustomer = new CustomerForm("", new AddressForm(" ", "1234"));
        CustomerForm goodCustomer = new CustomerForm("Ala", new AddressForm("Gdańsk", "80-001"));
        List<String> nested = List.of("address.city: nie może być puste", "address.zip: kod pocztowy ma format 00-000",
                "name: nie może być puste");

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: pola z @NotBlank", List.of("email", "login"), () -> exercise1(UserForm.class));
        Check.equal("ćw. 2a: poprawny e-mail", "null", () -> String.valueOf(exercise2(emailPattern,"ala@example.com")));
        Check.equal("ćw. 2b: zły e-mail", "to nie jest poprawny e-mail", () -> exercise2(emailPattern,"ala@"));
        Check.equal("ćw. 2c: null", "null", () -> String.valueOf(exercise2(emailPattern,null)));
        Check.equal("ćw. 3: naruszenia na pole", expectedCounts,() -> exercise3(forms));
        Check.equal("ćw. 4a: zagnieżdżony zły", nested, () -> exercise4(badCustomer));
        Check.equal("ćw. 4b: zagnieżdżony dobry", List.of(), () -> exercise4(goodCustomer));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of("email", "login"), () -> solution1(UserForm.class));
        Check.equal("ćw. 2a (wzorzec)", "null", () -> String.valueOf(solution2(emailPattern,"ala@example.com")));
        Check.equal("ćw. 2b (wzorzec)", "to nie jest poprawny e-mail", () -> solution2(emailPattern,"ala@"));
        Check.equal("ćw. 2c (wzorzec)", "null", () -> String.valueOf(solution2(emailPattern,null)));
        Check.equal("ćw. 3 (wzorzec)", expectedCounts,() -> solution3(forms));
        Check.equal("ćw. 4a (wzorzec)", nested, () -> solution4(badCustomer));
        Check.equal("ćw. 4b (wzorzec)", List.of(), () -> solution4(goodCustomer));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 7 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć posortowane nazwy pól klasy type, które mają adnotację @NotBlank.
     * Podpowiedź: getDeclaredFields + isAnnotationPresent(NotBlank.class).
     */
    static List<String> exercise1(Class<?> type) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 2 (średnie): napisz regułę @Pattern od zera: null → null (OK); wartość inna niż String →
     * IllegalStateException; tekst niepasujący CAŁY do rule.regex() → rule.message(); w przeciwnym razie null.
     * Podpowiedź: String.matches sprawdza dopasowanie całego tekstu.
     */
    static String exercise2(Pattern rule, Object value) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): policz naruszenia na pole we wszystkich formularzach razem (TreeMap pole → liczba).
     * Połącz z t16_streams: flatMap po Validator.validate, nazwa pola = tekst przed ": ".
     * Podpowiedź: Collectors.groupingBy(klucz, TreeMap::new, Collectors.counting()).
     */
    static Map<String, Long> exercise3(List<Object> forms) {
        // TODO: twoje rozwiązanie
        return new TreeMap<>();
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): walidacja kaskadowa jak w Jakarta. Naruszenia samego obiektu + dla każdego pola
     * z @Valid (i wartością różną od null) naruszenia obiektu w środku z prefiksem "nazwaPola.". Wynik posortowany.
     * Podpowiedź: rekurencja; prefiks dokładaj do każdego naruszenia dziecka: field.getName() + "." + v.
     */
    static List<String> exercise4(Object bean) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solution1(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(NotBlank.class))
                .map(Field::getName)
                .sorted()
                .toList();
    }

    static String solution2(Pattern rule, Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof String text)) {
            throw new IllegalStateException("@Pattern wymaga tekstu");
        }
        return text.matches(rule.regex()) ? null : rule.message();
    }

    static Map<String, Long> solution3(List<Object> forms) {
        return forms.stream()
                .flatMap(form -> Validator.validate(form).stream())
                .collect(Collectors.groupingBy(v -> v.substring(0, v.indexOf(": ")), TreeMap::new, Collectors.counting()));
    }

    static List<String> solution4(Object bean) {
        List<String> violations = new ArrayList<>(Validator.validate(bean));
        for (Field field : bean.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(Valid.class)) {
                continue;
            }
            Object child = readField(field, bean);
            if (child != null) {
                for (String v : solution4(child)) {
                    violations.add(field.getName() + "." + v);
                }
            }
        }
        Collections.sort(violations);
        return violations;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Walidator czyta je refleksją w czasie działania. Z domyślnym CLASS (lub SOURCE) getAnnotation zwróci null
     *      i żadna reguła się nie wykona — bez żadnego błędu.
     *   2. []  — null nie narusza @Min; brak wartości sprawdza się osobną adnotacją (np. @NotNull).
     *   3. Kod się skompiluje (Target = FIELD pozwala na każde pole), ale pierwsza walidacja rzuci
     *      IllegalStateException („@Min wymaga liczby”). Poprawnie: pole liczbowe (int/Integer) albo inna reguła.
     *   4. []  — standardowy walidator nie zna @AllowedValues, więc ją pomija. Trzeba zarejestrować regułę (with).
     *   5. intValue() obcina część ułamkową: 10000.50 → 10000, więc przejdzie @Max(10000). Porównuj dokładnie:
     *      new BigDecimal(value.toString()).compareTo(BigDecimal.valueOf(rule.value())) <= 0.
     */
    // </editor-fold>
}
