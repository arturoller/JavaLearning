package t19_annotations_reflection;

import helpers.Check;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Własne adnotacje — elementy, wartości domyślne, retencja, cel, powtarzanie i dziedziczenie
 *        (custom annotation = własna adnotacja; element = element adnotacji; default = wartość domyślna)
 *
 * W SKRÓCIE:
 *   Adnotację deklaruje się słowem {@code @interface}. W środku są ELEMENTY — wyglądają jak metody bez parametrów,
 *   a przy użyciu podaje się je jak pary nazwa = wartość. Meta-adnotacje decydują, jak długo adnotacja żyje
 *   (Retention), gdzie wolno ją przykleić (Target), czy można ją powtórzyć (Repeatable) i czy przechodzi
 *   na podklasy (Inherited).
 *
 * ANALOGIA: formularz z pieczątkami.
 *   Projektujesz pieczątkę (deklaracja adnotacji): ma pola „imię” i „data” (elementy), data domyślnie „nieznana”.
 *   Przybijasz ją na dokumentach (użycie adnotacji). Ktoś w archiwum (refleksja) czyta pieczątki i działa —
 *   ale tylko wtedy, gdy tusz jest trwały (RUNTIME), a nie znika po wyschnięciu (SOURCE).
 *
 * JAK TO DZIAŁA:
 *   {@code @Retention(RetentionPolicy.RUNTIME)}      ← jak długo żyje (domyślnie CLASS!)
 *   {@code @Target(ElementType.TYPE)}                ← gdzie wolno ją przykleić
 *   {@code @interface Author {}}                     ← deklaracja (niejawnie rozszerza java.lang.annotation.Annotation)
 *       String name();                       ← element obowiązkowy
 *       String date() default "nieznana";    ← element z wartością domyślną
 *
 *   użycie:   {@code @Author(name = "Ala")}  →  odczyt:  {@code Invoice.class.getAnnotation(Author.class).name()}
 *
 * SŁÓWKA:
 *   custom = własny; element = element; default = domyślny; value = wartość; retention policy = zasada przechowywania;
 *   source = kod źródłowy; runtime = czas działania; element type = rodzaj elementu (miejsca); repeatable = powtarzalny;
 *   container = kontener (pojemnik); inherited = dziedziczony; declared = zadeklarowany (bez dziedziczenia);
 *   record component = składnik rekordu; accessor = akcesor (metoda odczytu).
 *
 * ZOBACZ TEŻ: t19_annotations_reflection/Annotations01BuiltIn (meta-adnotacje), t09_records/Records01Basics,
 *             t19_annotations_reflection/Annotations03ReflectionBasics (refleksja),
 *             t19_annotations_reflection/Annotations04Validator (adnotacje w praktyce).
 * </pre>
 */
public class Annotations02Custom {

    // ---------------------------------------------------------------------------------------------
    // 1. Pierwsza adnotacja
    // ---------------------------------------------------------------------------------------------

    /** Author = autor. Retencja RUNTIME (widać ją refleksją), cel TYPE (klasa, interfejs, enum, rekord). */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Author {
        String name();                        // element BEZ wartości domyślnej → trzeba go podać

        String date() default "nieznana";     // element z wartością domyślną → można pominąć
    }

    @Author(name = "Ala", date = "2026-01-05")
    static final class Invoice {             // invoice = faktura
    }

    @Author(name = "Olek")
    static final class Receipt {             // receipt = paragon
    }

    static final class Anonymous {           // anonymous = anonimowy (bez adnotacji)
    }

    // ---------------------------------------------------------------------------------------------
    // 2. Typy elementów
    // ---------------------------------------------------------------------------------------------

    enum Level { LOW, NORMAL, HIGH }         // level = poziom

    /** Column = kolumna tabeli. Pokazuje wszystkie dozwolone typy elementów. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Column {
        String value();                                     // specjalna nazwa „value” (patrz sekcja 2)

        int length() default 255;                           // typ prosty

        boolean nullable() default true;                    // nullable = może być null

        Class<?> type() default String.class;               // Class

        Level level() default Level.NORMAL;                 // enum

        String[] tags() default {};                         // tablica (pusta jako wartość domyślna)

        Author owner() default @Author(name = "system");    // inna adnotacja (owner = właściciel)
    }

    /** ProductRow = wiersz produktu (pola tylko z adnotacjami, do odczytu refleksją). */
    static final class ProductRow {
        @Column("sku")
        String sku;

        @Column(value = "price", length = 10, nullable = false, type = BigDecimal.class, level = Level.HIGH)
        BigDecimal price;

        @Column(value = "labels", tags = {"seo", "lista"})
        String labels;

        @Column(value = "note", tags = "jeden", owner = @Author(name = "Ola", date = "2026-02-01"))
        String note;
    }

    // ---------------------------------------------------------------------------------------------
    // 3. Retencja
    // ---------------------------------------------------------------------------------------------

    @Retention(RetentionPolicy.SOURCE)
    @interface SourceOnly {                  // tylko w kodzie źródłowym
    }

    @interface InClassFile {                 // brak Retention → domyślnie CLASS
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface AtRuntime {                   // widoczna w czasie działania
    }

    @SourceOnly
    @InClassFile
    @AtRuntime
    static final class Marked {              // marked = oznaczony trzema adnotacjami
    }

    // ---------------------------------------------------------------------------------------------
    // 4. Cel (Target) i rekordy
    // ---------------------------------------------------------------------------------------------

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
    @interface Positive {                    // positive = dodatni
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.METHOD})
    @interface Audited {                     // audited = audytowany
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface NotEmpty {                    // not empty = niepusty
    }

    /** Price = cena (rekord, Java 16+). Każda adnotacja ma inny Target — sprawdzimy, dokąd „spłynęła”. */
    record Price(@Positive int amount, @Audited String currency, @NotEmpty String code) {
    }

    // ---------------------------------------------------------------------------------------------
    // 5. Powtarzanie
    // ---------------------------------------------------------------------------------------------

    /** Schedule = harmonogram. Repeatable → można ją dać kilka razy; kompilator pakuje powtórzenia w Schedules. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Repeatable(Schedules.class)
    @interface Schedule {
        String day();

        String time();
    }

    /** Schedules = kontener: element value() zwraca tablicę Schedule[]. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Schedules {
        Schedule[] value();
    }

    @Schedule(day = "PN", time = "08:00")
    @Schedule(day = "ŚR", time = "12:30")
    static final class BackupJob {           // backup job = zadanie kopii zapasowej
    }

    @Schedule(day = "PT", time = "18:00")
    static final class ReportJob {           // report job = zadanie raportu
    }

    // ---------------------------------------------------------------------------------------------
    // 6. Dziedziczenie
    // ---------------------------------------------------------------------------------------------

    /** Tracked = śledzony. Inherited → przechodzi z nadklasy na podklasy (ale nie z interfejsu!). */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    @interface Tracked {
        String value() default "domyślny";
    }

    @Tracked("bazowy")
    static class BaseEntity {                // base entity = encja bazowa
        @AtRuntime
        void save() {
        }
    }

    static class CustomerEntity extends BaseEntity {
        @Override
        void save() {
        }
    }

    @Tracked("vip")
    static class VipCustomerEntity extends CustomerEntity {
    }

    @Tracked("audyt")
    interface Auditable {                    // auditable = podlegający audytowi
    }

    static final class AuditLog implements Auditable {   // audit log = dziennik audytu
    }

    public static void main(String[] args) throws Exception {
        title("Annotations02 — własne adnotacje");

        firstAnnotation();       // first annotation = pierwsza adnotacja
        elementTypes();          // element types = typy elementów
        retentionPolicies();     // retention policies = zasady przechowywania
        targets();               // targets = cele
        repeatable();            // repeatable = powtarzalna
        inherited();             // inherited = dziedziczona
        annotationAsObject();    // annotation as object = adnotacja jako obiekt
        exercises();             // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PIERWSZA ADNOTACJA
    // =================================================================================================

    /** 1. Deklaracja, użycie i odczyt. Elementy odczytujemy jak METODY: a.name(), a.date(). */
    static void firstAnnotation() {
        section("1. Pierwsza adnotacja: deklaracja, użycie, odczyt");

        Author invoiceAuthor = Invoice.class.getAnnotation(Author.class);
        show("Invoice: name()", invoiceAuthor.name());
        show("Invoice: date()", invoiceAuthor.date());
        // WYNIK: Invoice: name() → Ala
        // WYNIK: Invoice: date() → 2026-01-05

        show("Receipt: date() (domyślna)", Receipt.class.getAnnotation(Author.class).date());
        show("Anonymous: getAnnotation", Anonymous.class.getAnnotation(Author.class));
        // WYNIK: Receipt: date() (domyślna) → nieznana
        // WYNIK: Anonymous: getAnnotation → null    ← brak adnotacji = null, nie wyjątek

        show("toString()", invoiceAuthor);
        show("annotationType()", invoiceAuthor.annotationType().getSimpleName());   // annotationType = typ adnotacji
        // WYNIK: toString() → @t19_annotations_reflection.Annotations02Custom$Author(date="2026-01-05", name="Ala")
        // WYNIK: annotationType() → Author
        // Zauważ: w toString elementy z wartością domyślną (date) idą PRZED resztą, a nie w kolejności z deklaracji.
        //   toString adnotacji służy tylko do podglądu — nie parsuj go i nie porównuj w testach.

        // Bez elementu name kompilator protestuje:  @Author()  →  błąd kompilacji: brakuje wartości elementu name.
        // PUŁAPKA: getAnnotation zwraca null, gdy adnotacji nie ma. a.name() na null = NullPointerException —
        //   zawsze sprawdzaj null (albo isAnnotationPresent) przed odczytem elementów.
    }

    // =================================================================================================
    // 2. TYPY ELEMENTÓW I ELEMENT value
    // =================================================================================================

    /** names = posortowane proste nazwy typów adnotacji z tablicy (pomocnicza do wypisywania). */
    static String names(Annotation[] annotations) {
        return Arrays.stream(annotations)
                .map(a -> a.annotationType().getSimpleName())
                .sorted()
                .toList()
                .toString();
    }

    /**
     * 2. Dozwolone typy elementów: typy proste, String, Class, enum, inna adnotacja oraz JEDNOWYMIAROWE tablice
     * tych typów. Wartości muszą być znane w czasie kompilacji (stałe).
     */
    static void elementTypes() {
        section("2. Typy elementów i specjalny element value");

        Field[] fields = ProductRow.class.getDeclaredFields();               // getDeclaredFields = zadeklarowane pola
        Arrays.sort(fields, Comparator.comparing(Field::getName));            // kolejność z refleksji NIE jest gwarantowana
        for (Field field : fields) {
            Column c = field.getAnnotation(Column.class);
            System.out.printf(Locale.ROOT, "   %-6s → value=%s, length=%d, nullable=%b, type=%s, level=%s, tags=%s, owner=%s%n",
                    field.getName(), c.value(), c.length(), c.nullable(), c.type().getSimpleName(), c.level(),
                    Arrays.toString(c.tags()), c.owner().name());
        }
        // WYNIK:    labels → value=labels, length=255, nullable=true, type=String, level=NORMAL, tags=[seo, lista], owner=system
        // WYNIK:    note   → value=note, length=255, nullable=true, type=String, level=NORMAL, tags=[jeden], owner=Ola
        // WYNIK:    price  → value=price, length=10, nullable=false, type=BigDecimal, level=HIGH, tags=[], owner=system
        // WYNIK:    sku    → value=sku, length=255, nullable=true, type=String, level=NORMAL, tags=[], owner=system

        // Skróty składni:
        //   @Column("sku")             = @Column(value = "sku")   ← tylko gdy podajesz WYŁĄCZNIE value
        //   @Column(value = "price", length = 10)                 ← przy kilku elementach „value =” jest obowiązkowe
        //   tags = "jeden"             = tags = {"jeden"}         ← tablica z jednym elementem bez klamer
        //
        // Czego NIE wolno (błędy kompilacji sprawdzone w javac 17):
        //   Integer count();  List<String> names();   → invalid type for annotation type element
        //   String name() default null;              → element value must be a constant expression
        //   @interface Ext extends Annotation { }    → 'extends' not allowed for @interfaces
        //   @Author(name = zmiennaNieFinalna)        → wartość musi być stałą czasu kompilacji

        // PUŁAPKA: null nie jest dozwoloną wartością elementu. Gdy potrzebujesz „brak wartości”, użyj umownej stałej:
        //   pustego napisu "", pustej tablicy {} albo specjalnej klasy/enuma (np. Level.NONE).
        // DOBRA PRAKTYKA: element, który podaje się najczęściej, nazwij value — użytkownik napisze krótko @Column("sku").
    }

    // =================================================================================================
    // 3. RETENCJA: SOURCE, CLASS, RUNTIME
    // =================================================================================================

    /** 3. Dowód, że CLASS to „jest w pliku .class, ale refleksja nie widzi”. Zajrzymy do bajtów pliku .class. */
    static void retentionPolicies() throws IOException {
        section("3. Retencja: SOURCE vs CLASS vs RUNTIME");

        show("getAnnotation(SourceOnly)", Marked.class.getAnnotation(SourceOnly.class));
        show("getAnnotation(InClassFile)", Marked.class.getAnnotation(InClassFile.class));
        show("getAnnotation(AtRuntime)", Marked.class.getAnnotation(AtRuntime.class));
        show("wszystkie widoczne", names(Marked.class.getAnnotations()));
        // WYNIK: getAnnotation(SourceOnly) → null
        // WYNIK: getAnnotation(InClassFile) → null    ← jest w .class, ale JVM nie udostępnia jej refleksji
        // WYNIK: getAnnotation(AtRuntime) → @t19_annotations_reflection.Annotations02Custom$AtRuntime()
        // WYNIK: wszystkie widoczne → [AtRuntime]

        // getResourceAsStream = otwórz zasób (tu: plik .class leżący obok) jako strumień IO bajtów.
        byte[] bytes;
        try (InputStream in = Marked.class.getResourceAsStream("Annotations02Custom$Marked.class")) {
            bytes = in.readAllBytes();                       // readAllBytes = wczytaj wszystkie bajty (Java 9+)
        }
        String raw = new String(bytes, StandardCharsets.ISO_8859_1);   // 1 bajt = 1 znak, nic nie ginie
        show("w .class jest „SourceOnly”?", raw.contains("SourceOnly"));
        show("w .class jest „InClassFile”?", raw.contains("InClassFile"));
        show("w .class jest „AtRuntime”?", raw.contains("AtRuntime"));
        // WYNIK: w .class jest „SourceOnly”? → false
        // WYNIK: w .class jest „InClassFile”? → true
        // WYNIK: w .class jest „AtRuntime”? → true
        show("atrybut RuntimeInvisibleAnnotations?", raw.contains("RuntimeInvisibleAnnotations"));
        show("atrybut RuntimeVisibleAnnotations?", raw.contains("RuntimeVisibleAnnotations"));
        // WYNIK: atrybut RuntimeInvisibleAnnotations? → true    ← tu leży InClassFile (CLASS)
        // WYNIK: atrybut RuntimeVisibleAnnotations? → true      ← tu leży AtRuntime (RUNTIME)

        // RETENCJA   GDZIE ŻYJE                  KTO CZYTA                          PRZYKŁADY
        // SOURCE     tylko plik .java            kompilator, procesory adnotacji    @Override, Lombok
        // CLASS      .java + .class              narzędzia do bajtkodu              (domyślna, rzadko celowo)
        // RUNTIME    .java + .class + JVM        refleksja, frameworki              @Deprecated, Spring, JUnit

        // PUŁAPKA: zapomniane @Retention(RetentionPolicy.RUNTIME) = domyślne CLASS = framework „nie widzi” adnotacji,
        //   a kompilator nie zgłasza żadnego błędu. Własna adnotacja czytana refleksją ZAWSZE potrzebuje RUNTIME.
    }

    // =================================================================================================
    // 4. CEL (Target) — także w rekordach
    // =================================================================================================

    /**
     * 4. Target wymienia miejsca, gdzie wolno przykleić adnotację. W rekordzie adnotacja ze składnika „spływa”
     * na pole, akcesor i parametr konstruktora kanonicznego — ale tylko tam, gdzie pozwala jej Target.
     */
    static void targets() throws NoSuchMethodException, NoSuchFieldException {
        section("4. Target — gdzie wolno przykleić adnotację");

        show("ElementType.values()", Arrays.toString(ElementType.values()));
        // WYNIK: ElementType.values() → [TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE, ANNOTATION_TYPE, PACKAGE, TYPE_PARAMETER, TYPE_USE, MODULE, RECORD_COMPONENT]
        // TYPE_PARAMETER i TYPE_USE (Java 8+), MODULE (Java 9+), RECORD_COMPONENT (Java 16+).

        RecordComponent[] components = Price.class.getRecordComponents();   // kolejność = kolejność w nagłówku rekordu
        Constructor<?> canonical = Price.class.getDeclaredConstructor(int.class, String.class, String.class);
        Annotation[][] parameterAnnotations = canonical.getParameterAnnotations();
        for (int i = 0; i < components.length; i++) {
            RecordComponent component = components[i];
            Field field = Price.class.getDeclaredField(component.getName());
            System.out.printf(Locale.ROOT, "   %-8s składnik=%-12s pole=%-12s akcesor=%-12s parametr=%s%n",
                    component.getName(), names(component.getAnnotations()), names(field.getAnnotations()),
                    names(component.getAccessor().getAnnotations()), names(parameterAnnotations[i]));
        }
        // WYNIK:    amount   składnik=[Positive]   pole=[Positive]   akcesor=[]           parametr=[]
        // WYNIK:    currency składnik=[]           pole=[Audited]    akcesor=[Audited]    parametr=[]
        // WYNIK:    code     składnik=[]           pole=[]           akcesor=[]           parametr=[NotEmpty]

        // Audited ma Target {FIELD, METHOD}. Przyklejona do klasy:
        //   błąd kompilacji: annotation type not applicable to this kind of declaration

        // PUŁAPKA: adnotacje na zmiennych lokalnych (LOCAL_VARIABLE) nigdy nie trafiają do pliku .class —
        //   nawet z RUNTIME refleksja ich nie zobaczy. Czytać je mogą tylko kompilator i procesory adnotacji.
        // DOBRA PRAKTYKA: zawsze podawaj Target. Wąski cel = kompilator złapie adnotację przyklejoną w złe miejsce,
        //   zamiast żeby ta po cichu nic nie robiła.
    }

    // =================================================================================================
    // 5. POWTARZANIE (Repeatable)
    // =================================================================================================

    /** 5. Dwie takie same adnotacje kompilator pakuje w KONTENER. getAnnotation(Schedule) wtedy zwraca null! */
    static void repeatable() {
        section("5. Repeatable — kilka takich samych adnotacji");

        show("BackupJob: getAnnotation(Schedule)", BackupJob.class.getAnnotation(Schedule.class));
        show("BackupJob: kontener Schedules", BackupJob.class.getAnnotation(Schedules.class).value().length);
        // WYNIK: BackupJob: getAnnotation(Schedule) → null    ← dwie adnotacje leżą w kontenerze
        // WYNIK: BackupJob: kontener Schedules → 2

        // getAnnotationsByType = pobierz adnotacje danego typu — „rozpakowuje” kontener za nas.
        for (Class<?> job : List.of(BackupJob.class, ReportJob.class)) {
            List<String> slots = Arrays.stream(job.getAnnotationsByType(Schedule.class))
                    .map(s -> s.day() + " " + s.time())
                    .toList();
            show(job.getSimpleName() + ": getAnnotationsByType", slots);
        }
        // WYNIK: BackupJob: getAnnotationsByType → [PN 08:00, ŚR 12:30]
        // WYNIK: ReportJob: getAnnotationsByType → [PT 18:00]

        show("ReportJob: getAnnotation(Schedule) (jedna)", ReportJob.class.getAnnotation(Schedule.class).day());
        show("ReportJob: kontener", ReportJob.class.getAnnotation(Schedules.class));
        // WYNIK: ReportJob: getAnnotation(Schedule) (jedna) → PT
        // WYNIK: ReportJob: kontener → null    ← pojedyncza adnotacja NIE jest pakowana

        // Wymagania wobec kontenera: element value() zwracający tablicę Schedule[], retencja co najmniej taka jak
        // Schedule, a Target kontenera — podzbiór Target adnotacji Schedule. Inaczej błąd kompilacji.
        // PUŁAPKA: kod czytający getAnnotation(Schedule.class) działa dla jednej adnotacji i „gubi” wszystkie,
        //   gdy ktoś doda drugą. Dla adnotacji powtarzalnych zawsze używaj getAnnotationsByType.
    }

    // =================================================================================================
    // 6. DZIEDZICZENIE (Inherited)
    // =================================================================================================

    /** 6. Inherited działa tylko dla KLAS (łańcuch nadklas). Interfejsy i metody nie przekazują adnotacji. */
    static void inherited() throws NoSuchMethodException {
        section("6. Inherited — dziedziczenie adnotacji");

        show("CustomerEntity: getAnnotation", CustomerEntity.class.getAnnotation(Tracked.class).value());
        // getDeclaredAnnotation = tylko adnotacja zadeklarowana bezpośrednio na tej klasie (bez dziedziczenia)
        show("CustomerEntity: getDeclaredAnnotation", CustomerEntity.class.getDeclaredAnnotation(Tracked.class));
        show("VipCustomerEntity: własna wygrywa", VipCustomerEntity.class.getAnnotation(Tracked.class).value());
        // WYNIK: CustomerEntity: getAnnotation → bazowy    ← odziedziczona z BaseEntity
        // WYNIK: CustomerEntity: getDeclaredAnnotation → null
        // WYNIK: VipCustomerEntity: własna wygrywa → vip

        show("AuditLog (interfejs ma @Tracked)", AuditLog.class.getAnnotation(Tracked.class));
        show("CustomerEntity.save() ma @AtRuntime?",
                CustomerEntity.class.getDeclaredMethod("save").isAnnotationPresent(AtRuntime.class));
        // WYNIK: AuditLog (interfejs ma @Tracked) → null    ← Inherited NIE działa dla interfejsów
        // WYNIK: CustomerEntity.save() ma @AtRuntime? → false    ← nadpisana metoda nie dziedziczy adnotacji

        // PUŁAPKA: oczekiwanie, że adnotacja z interfejsu albo z nadpisanej metody „przejdzie” na implementację.
        //   W czystej Javie nie przechodzi. Spring ma własne narzędzia (np. AnnotatedElementUtils), które przeszukują
        //   także interfejsy i metody nadklas — dlatego tam „działa”, a w Twoim kodzie z getAnnotation już nie.
    }

    // =================================================================================================
    // 7. ADNOTACJA JAKO OBIEKT
    // =================================================================================================

    /**
     * 7. Typ adnotacji to szczególny interfejs. Obiekt adnotacji tworzy biblioteka refleksji — jako dynamiczne proxy
     * (Annotations06DynamicProxy). Ma equals/hashCode po wartościach elementów i jest niezmienny.
     */
    static void annotationAsObject() throws NoSuchFieldException {
        section("7. Adnotacja jako obiekt: proxy, equals, niezmienność");

        Author ala = Invoice.class.getAnnotation(Author.class);
        show("Proxy.isProxyClass(ala.getClass())", Proxy.isProxyClass(ala.getClass()));
        show("ala instanceof Annotation", ala instanceof Annotation);
        // WYNIK: Proxy.isProxyClass(ala.getClass()) → true
        // WYNIK: ala instanceof Annotation → true

        Column skuColumn = ProductRow.class.getDeclaredField("sku").getAnnotation(Column.class);
        Column labelsColumn = ProductRow.class.getDeclaredField("labels").getAnnotation(Column.class);
        show("owner sku == owner labels (equals)", skuColumn.owner().equals(labelsColumn.owner()));
        // WYNIK: owner sku == owner labels (equals) → true    ← te same wartości elementów

        String[] tags = labelsColumn.tags();
        tags[0] = "ZEPSUTE";                                    // zmieniamy KOPIĘ tablicy
        show("tags() po „zepsuciu” kopii", Arrays.toString(labelsColumn.tags()));
        // WYNIK: tags() po „zepsuciu” kopii → [seo, lista]    ← każde wywołanie zwraca nową kopię

        // DOBRA PRAKTYKA: traktuj adnotację jak niezmienny rekord z konfiguracją. Odczytaj wartości raz (np. przy
        //   starcie programu) i trzymaj je w zwykłych obiektach — tak robią frameworki, bo refleksja kosztuje.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Deklaracja: @Retention(RUNTIME) @Target(...) @interface Nazwa { Typ element() default wartość; }
     *   • Typy elementów: proste, String, Class, enum, adnotacja + tablice 1D tych typów. Bez null, tylko stałe.
     *   • value: @Nazwa("x") zamiast @Nazwa(value = "x") — tylko gdy podajesz sam value.
     *   • Retencja: SOURCE (kompilator), CLASS (domyślna! niewidoczna dla refleksji), RUNTIME (refleksja).
     *   • Target: TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE, RECORD_COMPONENT, TYPE_USE, ...
     *     W rekordzie adnotacja spływa na składnik / pole / akcesor / parametr — tam, gdzie pozwala Target.
     *   • Repeatable(Kontener.class): getAnnotation zwraca null przy 2+ powtórzeniach → używaj getAnnotationsByType.
     *   • Inherited: tylko klasy (nadklasy). Nie interfejsy, nie metody. getDeclaredAnnotation ignoruje dziedziczenie.
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie typy mogą mieć elementy adnotacji? Czy  Integer count();  się skompiluje?
     *   2. Co wypisze:  System.out.println(Marked.class.getAnnotation(InClassFile.class));  (brak @Retention)  ?
     *   3. ZNAJDŹ BŁĄD:  @Column("price", length = 10) BigDecimal price;
     *   4. Co wypisze:  System.out.println(BackupJob.class.getAnnotation(Schedule.class));  (dwie @Schedule)  ?
     *   5. ZNAJDŹ BŁĄD:  @Inherited @Tracked na interfejsie Auditable, a kod sprawdza
     *      AuditLog.class.getAnnotation(Tracked.class) != null  i oczekuje true.
     *   6. Czym różni się getAnnotation od getDeclaredAnnotation?
     *   7. Dlaczego adnotacja na zmiennej lokalnej nie jest widoczna przez refleksję nawet z RUNTIME?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** expectedTracked = oczekiwany wynik ćw. 3 jako TreeMap (stała kolejność wydruku, w przeciwieństwie do Map.of). */
    static Map<String, String> expectedTracked() {
        return new TreeMap<>(Map.of("BaseEntity", "bazowy", "CustomerEntity", "bazowy", "VipCustomerEntity", "vip"));
    }

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: Invoice", "Ala (2026-01-05)", () -> exercise1(Invoice.class));
        Check.equal("ćw. 1b: Receipt", "Olek (nieznana)", () -> exercise1(Receipt.class));
        Check.equal("ćw. 1c: Anonymous", "brak autora", () -> exercise1(Anonymous.class));
        Check.equal("ćw. 2a: BackupJob", List.of("PN 08:00", "ŚR 12:30"), () -> exercise2(BackupJob.class));
        Check.equal("ćw. 2b: Anonymous", List.of(), () -> exercise2(Anonymous.class));
        Check.equal("ćw. 3: kto widzi @Tracked", expectedTracked(),
                () -> exercise3(List.of(AuditLog.class, VipCustomerEntity.class, BaseEntity.class, CustomerEntity.class)));
        Check.equal("ćw. 4a: AuditLog (z interfejsu)", Optional.of("audyt"),
                () -> exercise4(AuditLog.class, Tracked.class).map(Tracked::value));
        Check.equal("ćw. 4b: CustomerEntity (z nadklasy)", Optional.of("bazowy"),
                () -> exercise4(CustomerEntity.class, Tracked.class).map(Tracked::value));
        Check.equal("ćw. 4c: Anonymous", Optional.empty(),
                () -> exercise4(Anonymous.class, Tracked.class).map(Tracked::value));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", "Ala (2026-01-05)", () -> solution1(Invoice.class));
        Check.equal("ćw. 1b (wzorzec)", "Olek (nieznana)", () -> solution1(Receipt.class));
        Check.equal("ćw. 1c (wzorzec)", "brak autora", () -> solution1(Anonymous.class));
        Check.equal("ćw. 2a (wzorzec)", List.of("PN 08:00", "ŚR 12:30"), () -> solution2(BackupJob.class));
        Check.equal("ćw. 2b (wzorzec)", List.of(), () -> solution2(Anonymous.class));
        Check.equal("ćw. 3 (wzorzec)", expectedTracked(),
                () -> solution3(List.of(AuditLog.class, VipCustomerEntity.class, BaseEntity.class, CustomerEntity.class)));
        Check.equal("ćw. 4a (wzorzec)", Optional.of("audyt"),
                () -> solution4(AuditLog.class, Tracked.class).map(Tracked::value));
        Check.equal("ćw. 4b (wzorzec)", Optional.of("bazowy"),
                () -> solution4(CustomerEntity.class, Tracked.class).map(Tracked::value));
        Check.equal("ćw. 4c (wzorzec)", Optional.empty(),
                () -> solution4(Anonymous.class, Tracked.class).map(Tracked::value));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 9 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć "imię (data)" z adnotacji Author na klasie type albo "brak autora", gdy jej nie ma.
     * Podpowiedź: getAnnotation zwraca null, gdy adnotacji brak; data ma wartość domyślną.
     */
    static String exercise1(Class<?> type) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć listę "dzień godzina" ze WSZYSTKICH adnotacji Schedule klasy type (w kolejności
     * z kodu). Połącz z t16_streams: Arrays.stream(...).map(...).toList().
     * Podpowiedź: getAnnotationsByType — działa i dla jednej, i dla wielu adnotacji (zwraca pustą tablicę, gdy brak).
     */
    static List<String> exercise2(Class<?> type) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): z podanych klas wybierz te, które WIDZĄ adnotację Tracked (także odziedziczoną),
     * i zwróć mapę (TreeMap) prosta nazwa klasy → value().
     * Podpowiedź: getAnnotation (nie getDeclaredAnnotation!) uwzględnia Inherited.
     */
    static Map<String, String> exercise3(List<Class<?>> classes) {
        // TODO: twoje rozwiązanie
        return new TreeMap<>();
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): znajdź adnotację tak jak Spring: najpierw na samej klasie (getDeclaredAnnotation),
     * potem w jej interfejsach (rekurencyjnie), a na końcu w nadklasie (rekurencyjnie). Zwróć Optional.
     * Podpowiedź: metoda rekurencyjna; type.getInterfaces(), type.getSuperclass() (null dla Object i interfejsów).
     */
    static <A extends Annotation> Optional<A> exercise4(Class<?> type, Class<A> annotationType) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(Class<?> type) {
        Author author = type.getAnnotation(Author.class);
        return author == null ? "brak autora" : author.name() + " (" + author.date() + ")";
    }

    static List<String> solution2(Class<?> type) {
        return Arrays.stream(type.getAnnotationsByType(Schedule.class))
                .map(s -> s.day() + " " + s.time())
                .toList();
    }

    static Map<String, String> solution3(List<Class<?>> classes) {
        Map<String, String> result = new TreeMap<>();
        for (Class<?> type : classes) {
            Tracked tracked = type.getAnnotation(Tracked.class);
            if (tracked != null) {
                result.put(type.getSimpleName(), tracked.value());
            }
        }
        return result;
    }

    static <A extends Annotation> Optional<A> solution4(Class<?> type, Class<A> annotationType) {
        if (type == null) {
            return Optional.empty();
        }
        A direct = type.getDeclaredAnnotation(annotationType);
        if (direct != null) {
            return Optional.of(direct);
        }
        for (Class<?> iface : type.getInterfaces()) {
            Optional<A> fromInterface = solution4(iface, annotationType);
            if (fromInterface.isPresent()) {
                return fromInterface;
            }
        }
        return solution4(type.getSuperclass(), annotationType);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Typy proste, String, Class (także Class<?>), enum, inna adnotacja oraz jednowymiarowe tablice tych typów.
     *      Integer count(); się NIE skompiluje (invalid type for annotation type element) — opakowania są zabronione.
     *   2. null — bez @Retention obowiązuje CLASS: adnotacja jest w pliku .class, ale refleksja jej nie widzi.
     *   3. Skrót bez nazwy działa tylko dla samego value. Przy kilku elementach: @Column(value = "price", length = 10).
     *   4. null — dwie adnotacje kompilator zapakował w kontener Schedules. Użyj getAnnotationsByType(Schedule.class).
     *   5. @Inherited działa tylko w łańcuchu nadklas, nie dla interfejsów — wynik to false. Trzeba samemu przeszukać
     *      interfejsy (jak w ćwiczeniu 4) albo przenieść adnotację na klasę.
     *   6. getAnnotation uwzględnia adnotacje odziedziczone (Inherited) z nadklas; getDeclaredAnnotation zwraca
     *      tylko adnotację zadeklarowaną bezpośrednio na tym elemencie.
     *   7. Bo kompilator w ogóle nie zapisuje adnotacji zmiennych lokalnych do pliku .class (nie ma tam „miejsca”
     *      na zmienne lokalne jako deklaracje). Widzą je tylko kompilator i procesory adnotacji.
     */
    // </editor-fold>
}
