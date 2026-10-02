# AGENT KIT — everything you need to write lessons for JavaLearning (read ONLY this file)

Do NOT read other course files (helpers, gold-standard lessons, glossary) — all facts you need are here. Open only the files you
create/finish. Save tokens: verify each lesson right after writing it (narrow scope), keep the final report short.

## 0. Environment facts — DO NOT check these yourself (no exploration commands!)
- Paths below are RELATIVE TO THE REPOSITORY ROOT (works on the learner's Windows PC and in a Linux cloud session).
- JDK **17** is required (the verifier refuses other versions — WYNIK lines were recorded on Temurin 17). Maven: use the
  wrapper `./mvnw` (Windows: `mvnw.cmd`), never a global `mvn`. On Windows use the PowerShell tool, on Linux bash.
- Libraries (Lombok; later H2/JUnit) come from `temp/lib/` — the main session fills it with
  `./mvnw -q dependency:copy-dependencies -DoutputDirectory=temp/lib`. The verifier compiles with `--release 17 -g -Xlint:all`.
- Existing files you may rely on WITHOUT listing them: `helpers\Console.java, Check.java, Sleep.java, TempDir.java, SampleData.java`,
  `helpers\model\Product, Category, Employee, Department, Customer, Order, OrderLine, OrderStatus, Student` (APIs in sections 3–5).
- FORBIDDEN (wastes tokens): `java -version`, `javac -version`, `ls`/`dir`/`Get-ChildItem`/`find` on the course, reading helpers or
  other lessons, `git` anything, opening `tools/Verify.java`/`tools/tags.txt`, creating any `.ps1` file. The ONLY commands you need: write your file, run the verifier
  (optionally with `--show`), fix, repeat.
- **Unicode escapes:** the file-writing tool silently converts `\uXXXX` sequences into the real characters (e.g. `'A'`
  becomes `'A'`, `" "` becomes an INVISIBLE non-breaking space). The verifier flags invisible chars (INVISIBLE problem).
  If you need a literal escape in the source, restore it afterwards with PowerShell, building the backslash as `[char]92`
  (e.g. `$t.Replace([string][char]0x00A0, ([string][char]92) + 'u00A0')`). Avoid escape-based examples when not essential.
- Scratch experiments: ONLY when you must confirm one specific surprising behaviour (e.g. exact text of a compiler error you quote
  in a comment). Then write one tiny file under `temp/tmp-<your tag>/` (git-ignored) and compile it with `javac` directly. Prefer to avoid it:
  quote compiler errors in a shortened, generic form („błąd kompilacji: ...”) instead of verbatim.

Tools (committed): `tools/Verify.java`, `tools/lessons.txt` (registry), `tools/tags.txt`, `tools/ASSIGNMENTS.md` (outlines).
Layout: topic folders directly in `src/` (e.g. `src/t06_oop_basics/`; package `tNN_name`, helpers in package `helpers`).
Audience: Polish beginner/intermediate, weak English. Goal: effective learning + long-term retention. Java 17 only.

## 1. Rules (the checker enforces most)
1. Create/edit ONLY your assigned files. No git. No sub-agents. Never edit helpers/model/SampleData/README/other lessons.
   Need extra data or helper types? Put them INSIDE your lesson (private static method, nested static class/record/enum).
2. One public top-level class per file (exact name from your assignment). Imports: only `java.*`, `javax.*` (e.g. javax.annotation.processing, javax.tools), `helpers.*`,
   `helpers.model.*`. No other lesson packages, no Lombok, never mention the learner's own projects.
3. Java 17 only (Java 21 features only in comments marked "(Java 21+)"). Mark APIs newer than Java 8 with "(Java N+)" at
   first use: toList 16, isBlank/strip/repeat/lines/Predicate.not/Optional.isEmpty 11, orElseThrow() 10,
   ifPresentOrElse/Optional.or/Optional.stream/Stream.ofNullable/takeWhile/3-arg iterate/List.of 9, teeing 12, mapMulti 16,
   records/instanceof pattern 16, text blocks 15, switch expressions 14, helpful NPE 14, sealed 17.
4. ALL comments and printed text in Polish with diacritics; identifiers in English. EVERY English identifier/API gets a Polish
   translation at first use in the file: `// orElseGet = albo pobierz`; method calls in main too: `basics(); // basics = podstawy`.
   No calques: immutable = „niezmienny” (NOT „niemutowalny”), mutable = „zmienny”, shadowing/hiding = „przesłanianie/ukrywanie”.
   Terminology: „efekt uboczny” (side effect), „iterować”, „przekształć (mapuj)” (map op), „stream/strumień” (Stream API) vs
   „strumień IO”, „kolekcja” vs „zbiór” (Set), „zużyty” (consumed), „mapa” (Map).
5. Tags verbatim WITH colon at the start of a comment line: `TEMAT:` `W SKRÓCIE:` `ANALOGIA:` `JAK TO DZIAŁA:` `SŁÓWKA:`
   `ZOBACZ TEŻ:` (header), `PUŁAPKA:` `DOBRA PRAKTYKA:` `WYNIK:` (body), `ŚCIĄGA:` `PYTANIA KONTROLNE:` `ODPOWIEDZI:` (end),
   `ĆWICZENIE N (łatwe|średnie|trudniejsze):`. Never `PUŁAPKA 2:`, `ANALOGIA — ...`, `DOBRA PRAKTYKA (ważne!):`.
6. Javadoc: class header inside `<pre>...</pre>`; code containing `<` `>` `&` inside Javadoc → `{@code ...}` (balanced braces);
   multi-line code → `<pre>{@code ... }</pre>`. NEVER HTML entities (`&lt;` `&gt;` `&amp;`). No Javadoc line starting with `@`.
7. WYNIK: after printing code, one `// WYNIK: <exact line>` per output line (trimmed comparison). Annotation only after 2+
   spaces and `←`: `// WYNIK: suma → 68    ← komentarz`. Include `ℹ ...` lines from note() and `✔ ... → rzucono ...` from
   expectThrows. NEVER guess — run and copy; if output surprises you, understand and explain (or fix code).
   Non-deterministic output → no WYNIK, write `// (wynik zależy od uruchomienia)`.
8. Determinism: Random only with seed; never print Set.of/Map.of; never print HashMap/HashSet keyed by an ENUM (identity hash →
   order changes between runs) → use `TreeMap::new`, `() -> new EnumMap<>(Category.class)`, LinkedHashMap, or sort; pass Locale
   explicitly when formatting (`String.format(Locale.ROOT, ...)`, `Locale.forLanguageTag("pl-PL")`); Polish NumberFormat/
   DateTimeFormatter output may contain non-breaking spaces U+00A0/U+202F → replace with ' ' before printing (explain as PUŁAPKA);
   never print now()-based values in WYNIK.
9. Pedagogy: explain WHY; ANALOGIA from everyday life; PRZED/PO (bad → good, loop → new way); demonstrate throwing pitfalls with
   expectThrows; BigDecimal: compareTo not equals, constants outside lambdas; Polish string sorting → mention Unicode vs Collator;
   println inside lambdas = efekt uboczny (demo only). Short sentences, concrete examples. 450–750 lines per lesson.
10. Cross-references only as `tNN_package/ClassName` from the registry `tools/lessons.txt` (you may read that one file if you
    need a name; e.g. t12_collections/Collections05Maps, t13_lambdas/Lambda04MethodReferences, t14_optional/Optional01Basics,
    t15_numbers/Numbers01BigDecimal, t16_streams/Streams11GroupingBy, t17_datetime/DateTime01LocalDateTime).

## 2. Lesson skeleton (follow order and formatting exactly)
```java
package tNN_pkg;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Product;
import java.util.List;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Nazwa tematu — krótko
 *        (english words = tłumaczenie)
 *
 * W SKRÓCIE:
 *   1–3 zdania: co to jest i po co.
 *
 * ANALOGIA: obraz z życia codziennego (2–5 linii).
 *
 * JAK TO DZIAŁA:
 *   mechanizm krok po kroku, schemat, mini-tabela.
 *
 * SŁÓWKA:
 *   word = słowo; other = inne; ...
 *
 * ZOBACZ TEŻ: tNN_pkg/ClassName (dlaczego), ...
 * </pre>
 */
public class Topic01Aspect {

    public static void main(String[] args) {
        title("Topic01 — tytuł po polsku");

        basics();          // basics = podstawy
        commonPitfall();   // common pitfall = typowa pułapka
        exercises();       // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PODSTAWY
    // =================================================================================================

    /**
     * 1. Opis sekcji: co pokazujemy i dlaczego. {@code kod<T>} w Javadoc zawsze w {@code ...}.
     */
    static void basics() {
        section("1. Podstawy");

        List<String> names = SampleData.products().stream().map(Product::name).limit(2).toList();
        show("dwie nazwy", names);
        // WYNIK: dwie nazwy → [Laptop Pro 14, Smartfon X]

        // PUŁAPKA: opis błędu + DLACZEGO.
        expectThrows("dodanie do toList()", () -> names.add("X"));
        // WYNIK: ✔ dodanie do toList() → rzucono UnsupportedOperationException: (brak komunikatu)

        // DOBRA PRAKTYKA: zasada + dlaczego.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • punkt 1
     *   • punkt 2
     *
     * PYTANIA KONTROLNE:
     *   1. Pytanie o zrozumienie?
     *   2. Co wypisze:  System.out.println(...);  ?
     *   3. ZNAJDŹ BŁĄD:  ...kod... 
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: opis", List.of("a"), () -> exercise1(SampleData.products()));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of("a"), () -> solution1(SampleData.products()));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 1 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): treść zadania.
     * Podpowiedź: konkretna wskazówka.
     */
    static List<String> exercise1(List<Product> products) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solution1(List<Product> products) {
        return List.of("a");
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. ...
     */
    // </editor-fold>
}
```
Requirements: 7–10 sections easy→hard; 5–8 questions with ≥2 „Co wypisze” / „ZNAJDŹ BŁĄD”; 3–5 exercises easy→hard, each with
a hint, one „PRZEPISZ ...” (old way → new way; code in `<pre>{@code ...}</pre>`) or mixing with an earlier topic, last one hardest.
Expected-value type must match the return type (long → `4L`, double → `4.0`, int → `4`); avoid exact equality of computed doubles.
If a neutral stub return could accidentally PASS a check (boolean, Optional, empty list/map expected), the stub must instead
`throw new UnsupportedOperationException("TODO");` — every exercise must start as ✘.
Features from later lessons: if you must use one (e.g. groupingBy before Streams11), add a short comment pointing to that lesson.
package-info.java (if assigned): Javadoc with `TEMAT: tNN_name — ...`, `<p>` description, prerequisites, `<ol>` reading order
(plain class names, no {@link}), `<p>SŁÓWKA: ...</p>`, then `package tNN_name;`.

## 3. Helpers API (use via `import static helpers.Console.*;` and `Check.`)
- `title(String)` — banner: blank line, 78×"=", " text", 78×"=".
- `section(String)` — blank line + `--- text ------...`.
- `show(String label, Object value)` → prints `label → value` (arrays printed like `[1, 2]`).
- `showEach(String label, Collection<?>)` → `label (liczba elementów: n):` then `   • item` lines;
  `showEach(String label, Map<?,?>)` → `label (liczba kluczy: n):` then `   • key → value` lines.
- `note(String)` → `   ℹ text`.  `line()` → blank line.
- `expectThrows(String label, ThrowingAction)` (lambda may throw checked exceptions) →
  `✔ label → rzucono SimpleClassName: message` (null message → `(brak komunikatu)`), or `✘ label → NIE rzucono wyjątku (a spodziewaliśmy się go)`.
- `Check.equal(String label, Object expected, Supplier<?> actual)` → `✔ OK    label` or `✘ BŁĄD  label (oczekiwano: X, jest: Y)`;
  exceptions in the supplier are caught (✘ with exception); BigDecimal compared by value (also inside List/Map) and a line
  `        ⚠ uwaga: ta sama wartość, ale różna skala (...)` appears if scales differ at top level.
  `Check.equal(label, expected, Object actual)`, `Check.equalExact(label, expected, Supplier)` (strict equals),
  `Check.isTrue(label, boolean)`, `Check.throwsException(label, Class, ThrowingAction)`,
  `Check.summary()` → blank line + `PODSUMOWANIE: ✔ n OK, ✘ m BŁĄD` (and resets counters).
- `Sleep.ms(long)`, `Sleep.randomMs(min, max)` (restores interrupt flag). `TempDir.create(String prefix)` → Path;
  `TempDir.deleteRecursively(Path)` (use in finally).

## 4. Model (package `helpers.model`; all records are immutable, lists copied with List.copyOf)
- `record Product(String sku, String name, Category category, BigDecimal price, int stock)`; `inStock()` (stock > 0),
  `stockValue()` (price × stock); toString → `Laptop Pro 14 (5499.99 zł)`.
- `enum Category { ELEKTRONIKA, SPOZYWCZE, KSIAZKI, ODZIEZ, DOM }`; `getDisplayName()` → Elektronika, Spożywcze, Książki, Odzież, Dom i ogród.
- `record Employee(String name, Department department, int salary, int age, LocalDate hireDate, List<String> skills)`;
  toString → `Anna Nowak (IT, 14500 zł)`.
- `enum Department { IT, HR, SPRZEDAZ, KSIEGOWOSC, MARKETING, LOGISTYKA }` — LOGISTYKA has NO employees.
- `record Customer(long id, String name, String city, String email, boolean vip)`; `findEmail()` → `Optional<String>`;
  toString → `Jan Kowalski (Warszawa, VIP)` or `Maria Nowak (Kraków)`.
- `record OrderLine(Product product, int quantity)`; `total()` (price × qty, scale 2); toString → `Laptop Pro 14 x1`.
- `record Order(String id, Customer customer, LocalDate date, OrderStatus status, List<OrderLine> lines)`; `total()` (sum of lines);
  toString → `ZAM-001 [Jan Kowalski, 2026-01-05, DOSTARCZONE, 6199.79 zł]`.
- `enum OrderStatus { NOWE, OPLACONE, WYSLANE, DOSTARCZONE, ANULOWANE }`; `isFinal()` (DOSTARCZONE or ANULOWANE).
- `record Student(String name, String city, int year, List<Integer> grades)`; `averageGrade()` → `OptionalDouble` (empty for no grades);
  toString → `Ala [5, 4, 5, 3]`.

## 5. SampleData (static methods, each returns a NEW unmodifiable List.of(...) in this exact order)
`products()` — 14 (sku, name, category, price, stock):
ELE-001 Laptop Pro 14 ELEKTRONIKA 5499.99 7 · ELE-002 Smartfon X ELEKTRONIKA 2999.00 0 · ELE-003 Słuchawki BT ELEKTRONIKA 349.90 25 ·
ELE-004 Monitor 27 cali ELEKTRONIKA 1299.00 4 · SPO-001 Kawa ziarnista 1kg SPOZYWCZE 64.99 120 · SPO-002 Czekolada gorzka SPOZYWCZE 7.49 300 ·
SPO-003 Oliwa z oliwek SPOZYWCZE 42.00 0 · KSI-001 Czysty kod KSIAZKI 79.00 15 · KSI-002 Java. Podstawy KSIAZKI 129.00 9 ·
KSI-003 Wzorce projektowe KSIAZKI 99.00 3 · ODZ-001 Kurtka zimowa ODZIEZ 459.00 12 · ODZ-002 T-shirt bawełniany ODZIEZ 49.99 80 ·
DOM-001 Ekspres do kawy DOM 1899.00 2 · DOM-002 Lampka biurkowa DOM 129.00 18.
(Out of stock: Smartfon X, Oliwa z oliwek. Price < 100: Kawa, Czekolada, Oliwa, Czysty kod, Wzorce, T-shirt. Total stock 595.)
`productBySku(String)` → Product or NoSuchElementException.

`employees()` — 10 (name, dept, salary, age, hireDate, skills):
Anna Nowak IT 14500 34 2018-03-01 [Java, Spring, SQL] · Piotr Kowalski IT 9800 27 2022-06-15 [Java, Docker] ·
Katarzyna Wiśniewska HR 7200 41 2015-01-10 [Rekrutacja, Excel] · Tomasz Wójcik SPRZEDAZ 8900 38 2019-09-01 [Negocjacje, Excel, CRM] ·
Magdalena Kamińska KSIEGOWOSC 8100 45 2012-04-20 [Excel, SAP] · Michał Lewandowski IT 17200 45 2014-11-03 [Java, Kotlin, AWS, SQL] ·
Agnieszka Zielińska MARKETING 7600 29 2021-02-01 [SEO, Canva] · Krzysztof Szymański SPRZEDAZ 11200 50 2010-07-12 [Negocjacje, CRM] ·
Ewa Woźniak IT 12100 31 2020-01-07 [Python, SQL, Docker] · Paweł Dąbrowski MARKETING 9800 36 2017-05-22 [SEO, Google Ads, Excel].
(Sum of salaries 106400, average 10640.0; two people earn 9800.)

`customers()` — 7 (id, name, city, email, vip): 1 Jan Kowalski Warszawa jan@example.com VIP · 2 Maria Nowak Kraków null ·
3 Adam Mazur Gdańsk adam.mazur@example.com · 4 Zofia Krawczyk Warszawa zofia@example.com VIP · 5 Ola Pawlak Poznań null ·
6 Marek Król Kraków marek@example.com · 7 Ewa Lis Wrocław ewa.lis@example.com (NO orders).

`orders()` — 10 (id, customerId, date, status, lines → total):
ZAM-001 1 2026-01-05 DOSTARCZONE [ELE-001×1, ELE-003×2] 6199.79 · ZAM-002 2 2026-01-12 DOSTARCZONE [SPO-001×3, SPO-002×10] 269.87 ·
ZAM-003 1 2026-02-02 WYSLANE [KSI-001×1, KSI-002×1, KSI-003×1] 307.00 · ZAM-004 3 2026-02-14 ANULOWANE [ELE-002×1] 2999.00 ·
ZAM-005 4 2026-02-20 OPLACONE [DOM-001×1, SPO-001×2] 2028.98 · ZAM-006 5 2026-03-01 NOWE [ODZ-002×3, ODZ-001×1] 608.97 ·
ZAM-007 4 2026-03-03 DOSTARCZONE [ELE-004×2, DOM-002×1] 2727.00 · ZAM-008 6 2026-03-15 WYSLANE [KSI-002×2, SPO-002×5] 295.45 ·
ZAM-009 2 2026-03-28 NOWE [ELE-003×1] 349.90 · ZAM-010 1 2026-04-02 OPLACONE [SPO-003×2, KSI-001×1] 163.00.
(Totals are pre-computed — still confirm by running.)

`students()` — 8 (name, city, year, grades): Ala Warszawa 1 [5,4,5,3] · Bartek Kraków 2 [3,3,4,2] · Celina Gdańsk 1 [5,5,5,4] ·
Darek Warszawa 3 [2,3,2,3] · Ela Kraków 2 [4,4,5,5] · Filip Poznań 3 [3,4,3,4] · Gosia Warszawa 2 [5,4,4,5] · Henryk Gdańsk 1 [] (no grades).

`words()` — [java, stream, lambda, kolekcja, java, mapa, lista, stream, java, optional, rekord, enum]
`sentences()` — [Java jest językiem obiektowym, Stream to nie jest kolekcja, Lambda to anonimowa funkcja, Kolekcja przechowuje elementy]
`numbers()` — [5, 3, 8, 1, 9, 2, 7, 3, 10, 6, 4, 8]
`salesCsvLines()` — header `data,sku,produkt,kategoria,ilosc,cena` + 12 lines: 8 valid, 4 invalid (text quantity "dwa",
too few fields, empty line, month 13).

## 6. Verification (mandatory)
The verifier is a Java program (single-file launch), run from the repository root:
`java "-Dfile.encoding=UTF-8" "-Dsun.stdout.encoding=UTF-8" tools/Verify.java --tag <YOUR_TAG> --scope "<glob>" --run all`
Globs are relative to src with FORWARD slashes, e.g. `"t16_streams/Streams18*"` (several globs after one --scope);
add `--show` to see program output. NEVER create .ps1 files (they get blocked). Done when: `COMPILE EXIT: 0`, `PROBLEMS: 0`, `FAILED RUNS: 0`,
every lesson `mismatches: 0`, and the reference-solution summary is `✔ N OK, ✘ 0 BŁĄD` (stubs showing ✘ are expected).
javac uses `-Xlint:all -g`; warnings count as problems.

## 7. Final report (short!)
Files created; per lesson one line listing section titles and one line listing exercises; final verifier summary; doubts or
suggested helper changes (max 5 bullets).
