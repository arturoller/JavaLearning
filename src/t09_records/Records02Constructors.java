package t09_records;

import helpers.Check;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Konstruktory rekordów — walidacja, normalizacja, kopie obronne, fabryki i „withery”
 *        (compact constructor = konstruktor kompaktowy; canonical = kanoniczny; factory = fabryka; with = z (zmienionym))
 *
 * W SKRÓCIE:
 *   Rekord powinien być POPRAWNY od chwili utworzenia. Służy do tego KONSTRUKTOR KOMPAKTOWY: blok bez listy parametrów,
 *   który wykonuje się PRZED przypisaniem pól. Sprawdzasz w nim dane (rzucasz wyjątek przy złych) albo je poprawiasz
 *   (strip, małe litery, kopia listy) — przypisujesz nową wartość do PARAMETRU, a Java sama zapisze ją w polu.
 *   Dodatkowe konstruktory muszą wywołać konstruktor kanoniczny przez this(...). Czytelne nazwy daje fabryka: Range.of(1, 5).
 *
 * ANALOGIA: bramka na lotnisku.
 *   Każdy pasażer (dane) przechodzi przez jedną bramkę (konstruktor kompaktowy). Bramka odrzuca niebezpiecznych
 *   (walidacja) i każe zdjąć kurtkę (normalizacja). Nie ma innego wejścia — dlatego w samolocie (w programie) są tylko
 *   sprawdzeni pasażerowie, a dalsze kontrole „na pokładzie” są zbędne.
 *
 * JAK TO DZIAŁA:
 *   record Email(String value) {
 *       Email {                                         ← konstruktor kompaktowy: bez (String value)!
 *           value = value.strip().toLowerCase(ROOT);    ← zmieniamy PARAMETR (normalizacja)
 *           if (!value.contains("@")) throw ...;        ← walidacja
 *       }                                               ← tu Java sama robi: this.value = value;
 *   }
 *
 * SŁÓWKA:
 *   compact = kompaktowy (zwięzły); canonical = kanoniczny (pełny); validate = sprawdź poprawność; normalize = ujednolić;
 *   defensive copy = kopia obronna; factory method = metoda fabrykująca; of = z (podanych wartości); with = z (zmienionym);
 *   range = zakres; from = od; to = do; percent = procent; email = adres e-mail; members = członkowie.
 *
 * ZOBACZ TEŻ: t09_records/Records01Basics (podstawy), t06_oop_basics/Oop02Constructors (konstruktory klas),
 *             t06_oop_basics/Oop06Immutability (kopie obronne), t10_exceptions/Exceptions07BestPractices (fail fast),
 *             t15_numbers/Numbers02MoneyValueObject (obiekt wartości z walidacją).
 * </pre>
 */
public class Records02Constructors {

    // ---------------------------------------------------------------------------------------------
    // Rekordy używane w lekcji
    // ---------------------------------------------------------------------------------------------

    /** Email = adres e-mail. Normalizacja (strip, małe litery) + walidacja w konstruktorze kompaktowym. */
    record Email(String value) {
        Email {
            if (value == null) {
                throw new IllegalArgumentException("E-mail nie może być null");
            }
            value = value.strip().toLowerCase(Locale.ROOT);   // przypisanie do PARAMETRU, nie do this.value
            if (!value.contains("@") || value.startsWith("@") || value.endsWith("@")) {
                throw new IllegalArgumentException("Niepoprawny e-mail: " + value);
            }
        }
    }

    /**
     * Range = zakres liczb całkowitych od from do to (włącznie). Pokazuje: walidację relacji między składnikami,
     * dodatkowy konstruktor, fabryki i „withery”.
     */
    record Range(int from, int to) {
        Range {
            if (from > to) {
                throw new IllegalArgumentException("from > to: " + from + " > " + to);
            }
        }

        /** Dodatkowy konstruktor: zakres 0..to. MUSI zacząć się od this(...) — wywołania konstruktora kanonicznego. */
        Range(int to) {
            this(0, to);
        }

        /** of = z. Fabryka o czytelnej nazwie; może też np. zamienić kolejność albo zwracać gotowe obiekty. */
        static Range of(int from, int to) {
            return new Range(from, to);
        }

        /** single = pojedynczy. Nazwa mówi więcej niż new Range(5, 5). */
        static Range single(int value) {
            return new Range(value, value);
        }

        /** withTo = z innym „do”. Rekord jest niezmienny, więc „zmiana” to nowy rekord (i ponowna walidacja!). */
        Range withTo(int newTo) {
            return new Range(from, newTo);
        }

        /** length = długość: ile liczb mieści zakres. */
        int length() {
            return to - from + 1;
        }

        /** contains = zawiera. */
        boolean contains(int value) {
            return value >= from && value <= to;
        }
    }

    /** Team = drużyna. Tym razem z KOPIĄ OBRONNĄ listy — naprawa pułapki z Records01Basics (sekcja 6). */
    record Team(String name, List<String> members) {
        Team {
            members = List.copyOf(members);    // kopia niemodyfikowalna (Java 10+): zmiany oryginału nas nie dotyczą
        }
    }

    /** Percent = procent 0..100. Wersja z JAWNYM konstruktorem kanonicznym (pełna forma) — dla porównania. */
    record Percent(int value) {
        Percent(int value) {                   // pełna forma: z listą parametrów i ręcznym przypisaniem pola
            if (value < 0 || value > 100) {
                throw new IllegalArgumentException("Procent spoza 0..100: " + value);
            }
            this.value = value;                // w pełnej formie TRZEBA przypisać każde pole samodzielnie
        }
    }

    public static void main(String[] args) {
        title("Records02 — konstruktory rekordów");

        validation();           // validation = walidacja
        normalization();        // normalization = normalizacja (ujednolicenie)
        defensiveCopy();        // defensive copy = kopia obronna
        canonicalFullForm();    // canonical full form = pełny konstruktor kanoniczny
        extraConstructors();    // extra constructors = dodatkowe konstruktory
        factoriesAndWithers();  // factories and withers = fabryki i „withery”
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. WALIDACJA
    // =================================================================================================

    /** 1. Zły rekord w ogóle nie powstaje — wyjątek w konstruktorze. Zasada „fail fast” (zawiedź szybko). */
    static void validation() {
        section("1. Walidacja w konstruktorze kompaktowym");

        show("poprawny zakres", new Range(1, 5));
        // WYNIK: poprawny zakres → Range[from=1, to=5]

        expectThrows("new Range(5, 1)", () -> new Range(5, 1));
        // WYNIK: ✔ new Range(5, 1) → rzucono IllegalArgumentException: from > to: 5 > 1

        expectThrows("new Email(\"brak-malpy\")", () -> new Email("brak-malpy"));
        // WYNIK: ✔ new Email("brak-malpy") → rzucono IllegalArgumentException: Niepoprawny e-mail: brak-malpy

        // DOBRA PRAKTYKA: sprawdzaj dane w JEDNYM miejscu — w konstruktorze. Wtedy każdy obiekt Range w programie
        //   na pewno ma from ≤ to i żadna metoda nie musi tego sprawdzać ponownie.
    }

    // =================================================================================================
    // 2. NORMALIZACJA
    // =================================================================================================

    /** 2. W konstruktorze kompaktowym przypisujesz NOWĄ wartość do parametru — Java zapisze w polu już poprawioną. */
    static void normalization() {
        section("2. Normalizacja danych");

        Email a = new Email("  Jan.Kowalski@Example.COM ");
        Email b = new Email("jan.kowalski@example.com");
        show("po normalizacji", a.value());
        show("a.equals(b)", a.equals(b));
        // WYNIK: po normalizacji → jan.kowalski@example.com
        // WYNIK: a.equals(b) → true    ← dzięki normalizacji ten sam adres zapisany różnie jest równy

        // PUŁAPKA: w konstruktorze kompaktowym NIE piszesz this.value = ... (błąd kompilacji: pole final przypisze Java).
        //   Zmieniasz parametr: value = ... .
    }

    // =================================================================================================
    // 3. KOPIA OBRONNA
    // =================================================================================================

    /** 3. List.copyOf robi niemodyfikowalną kopię — rekord jest teraz naprawdę (głęboko) niezmienny. */
    static void defensiveCopy() {
        section("3. Kopia obronna listy");

        List<String> names = new ArrayList<>(List.of("Ala", "Olek"));
        Team team = new Team("Orły", names);
        names.add("Intruz");                                   // zmiana oryginału...
        show("rekord po zmianie oryginału", team);
        // WYNIK: rekord po zmianie oryginału → Team[name=Orły, members=[Ala, Olek]]    ← rekord bez zmian

        expectThrows("team.members().add(...)", () -> team.members().add("X"));
        // WYNIK: ✔ team.members().add(...) → rzucono UnsupportedOperationException: (brak komunikatu)

        expectThrows("lista z null", () -> new Team("Null", java.util.Arrays.asList("Ala", null)));
        // WYNIK: ✔ lista z null → rzucono NullPointerException: (brak komunikatu)
        // PUŁAPKA: List.copyOf nie przyjmuje elementów null. Zwykle to zaleta (brak nulli w danych), ale warto wiedzieć.
    }

    // =================================================================================================
    // 4. PEŁNY KONSTRUKTOR KANONICZNY
    // =================================================================================================

    /**
     * 4. Konstruktor kanoniczny można też napisać w pełnej formie (z parametrami). Wtedy sam przypisujesz pola.
     * Forma kompaktowa robi to samo krócej — używaj jej, chyba że potrzebujesz czegoś nietypowego.
     */
    static void canonicalFullForm() {
        section("4. Pełna forma konstruktora kanonicznego");

        show("new Percent(45)", new Percent(45));
        // WYNIK: new Percent(45) → Percent[value=45]

        expectThrows("new Percent(120)", () -> new Percent(120));
        // WYNIK: ✔ new Percent(120) → rzucono IllegalArgumentException: Procent spoza 0..100: 120
    }

    // =================================================================================================
    // 5. DODATKOWE KONSTRUKTORY
    // =================================================================================================

    /** 5. Dodatkowy konstruktor = wygoda. Zawsze deleguje do kanonicznego przez this(...), więc walidacja nie jest pomijana. */
    static void extraConstructors() {
        section("5. Dodatkowe konstruktory");

        show("new Range(10)", new Range(10));
        // WYNIK: new Range(10) → Range[from=0, to=10]

        expectThrows("new Range(-3)", () -> new Range(-3));
        // WYNIK: ✔ new Range(-3) → rzucono IllegalArgumentException: from > to: 0 > -3    ← walidacja z kanonicznego
    }

    // =================================================================================================
    // 6. FABRYKI I „WITHERY”
    // =================================================================================================

    /** 6. Fabryki (of, single) — czytelne nazwy. Withery (withTo) — „zmiana” niezmiennego rekordu przez nowy obiekt. */
    static void factoriesAndWithers() {
        section("6. Metody fabrykujące i „withery”");

        Range week = Range.of(1, 7);
        Range day = Range.single(3);
        show("Range.of(1, 7)", week + ", długość " + week.length());
        show("Range.single(3)", day + ", długość " + day.length());
        // WYNIK: Range.of(1, 7) → Range[from=1, to=7], długość 7
        // WYNIK: Range.single(3) → Range[from=3, to=3], długość 1

        Range longer = week.withTo(14);
        show("week.withTo(14)", longer);
        show("oryginał bez zmian", week);
        // WYNIK: week.withTo(14) → Range[from=1, to=14]
        // WYNIK: oryginał bez zmian → Range[from=1, to=7]

        expectThrows("week.withTo(0)", () -> week.withTo(0));
        // WYNIK: ✔ week.withTo(0) → rzucono IllegalArgumentException: from > to: 1 > 0

        // DOBRA PRAKTYKA: wither zwraca NOWY rekord przez konstruktor — dzięki temu walidacja działa i przy „zmianach”.
        // Ciekawostka: Java nie ma jeszcze wbudowanych witherów; rozważana jest składnia „with” dla rekordów (propozycja JEP 468).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Konstruktor kompaktowy: Nazwa { ... } — bez parametrów; działa PRZED przypisaniem pól.
     *   • Walidacja: throw new IllegalArgumentException(...) — zły obiekt nie powstaje (fail fast).
     *   • Normalizacja: przypisz do PARAMETRU (value = value.strip()); this.value = ... w wersji kompaktowej to błąd.
     *   • Kopia obronna: members = List.copyOf(members) (niemodyfikowalna, bez null).
     *   • Pełna forma kanoniczna: z parametrami i ręcznym this.pole = pole dla każdego pola.
     *   • Dodatkowy konstruktor zaczyna się od this(...); fabryki static of(...); withery withX(...) → nowy rekord.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się konstruktor kompaktowy od pełnego konstruktora kanonicznego?
     *   2. Co wypisze:  System.out.println(new Email(" A@B.PL ").value());  ?
     *   3. ZNAJDŹ BŁĄD:
     *          record Age(int years) { Age { if (years < 0) throw new IllegalArgumentException(); this.years = years; } }
     *   4. Dlaczego walidacja w konstruktorze rekordu jest lepsza niż sprawdzanie danych w każdej metodzie, która go używa?
     *   5. Co wypisze:  Range r = Range.of(2, 4); r.withTo(9); System.out.println(r);  ?
     *   6. Co musi być pierwszą instrukcją dodatkowego (niekanonicznego) konstruktora rekordu i dlaczego?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: poprawny e-mail", Optional.of(new Email("ola@firma.pl")), () -> exercise1(" Ola@Firma.pl"));
        Check.equal("ćw. 1b: zły e-mail", Optional.empty(), () -> exercise1("ola.firma.pl"));
        Check.equal("ćw. 2: przesunięty zakres", Range.of(13, 17), () -> exercise2(Range.of(3, 7), 10));
        Check.equal("ćw. 3: połączone zakresy", Range.of(1, 9), () -> exercise3(Range.of(1, 5), Range.of(4, 9)));
        Check.equal("ćw. 4a: \" 45% \" → Percent", new Percent(45), () -> exercise4(" 45% "));
        Check.throwsException("ćw. 4b: \"150%\" → wyjątek", IllegalArgumentException.class, () -> exercise4("150%"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", Optional.of(new Email("ola@firma.pl")), () -> solution1(" Ola@Firma.pl"));
        Check.equal("ćw. 1b (wzorzec)", Optional.empty(), () -> solution1("ola.firma.pl"));
        Check.equal("ćw. 2 (wzorzec)", Range.of(13, 17), () -> solution2(Range.of(3, 7), 10));
        Check.equal("ćw. 3 (wzorzec)", Range.of(1, 9), () -> solution3(Range.of(1, 5), Range.of(4, 9)));
        Check.equal("ćw. 4a (wzorzec)", new Percent(45), () -> solution4(" 45% "));
        Check.throwsException("ćw. 4b (wzorzec)", IllegalArgumentException.class, () -> solution4("150%"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): spróbuj utworzyć Email; gdy konstruktor rzuci IllegalArgumentException — zwróć Optional.empty().
     * Podpowiedź: try { return Optional.of(new Email(raw)); } catch (IllegalArgumentException e) { ... }
     */
    static Optional<Email> exercise1(String raw) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /** ĆWICZENIE 2 (łatwe): zwróć zakres przesunięty o delta (oba końce). Range.of(3, 7) przesunięty o 10 → 13..17. */
    static Range exercise2(Range r, int delta) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 3 (średnie): połącz dwa NACHODZĄCE na siebie zakresy w jeden: od mniejszego from do większego to.
     * Podpowiedź: Math.min, Math.max.
     */
    static Range exercise3(Range a, Range b) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zamień tekst typu " 45% " na Percent. Usuń spacje i znak %, zamień na int
     * (Integer.parseInt) i utwórz Percent. Zły procent (np. 150) ma skończyć się wyjątkiem z konstruktora Percent —
     * NIE łap go. Podpowiedź: text.strip().replace("%", "").
     */
    static Percent exercise4(String text) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static Optional<Email> solution1(String raw) {
        try {
            return Optional.of(new Email(raw));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    static Range solution2(Range r, int delta) {
        return Range.of(r.from() + delta, r.to() + delta);
    }

    static Range solution3(Range a, Range b) {
        return Range.of(Math.min(a.from(), b.from()), Math.max(a.to(), b.to()));
    }

    static Percent solution4(String text) {
        return new Percent(Integer.parseInt(text.strip().replace("%", "")));
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Kompaktowy nie ma listy parametrów i nie przypisuje pól — Java robi to sama na końcu (można zmienić parametry).
     *      Pełny ma parametry i musi sam przypisać każde pole: this.x = x.
     *   2. „a@b.pl”.
     *   3. W konstruktorze kompaktowym nie wolno przypisywać this.years — Java przypisze pole sama (błąd kompilacji).
     *      Wystarczy usunąć this.years = years;.
     *   4. Walidacja w jednym miejscu gwarantuje, że KAŻDY obiekt jest poprawny — metody nie muszą niczego sprawdzać,
     *      a zły obiekt nie „wędruje” po programie, powodując błąd daleko od przyczyny.
     *   5. „Range[from=2, to=4]” — withTo zwraca NOWY rekord, a wynik został zignorowany; r się nie zmienia.
     *   6. Wywołanie this(...) — innego konstruktora, ostatecznie kanonicznego. Dzięki temu wszystkie pola są przypisane,
     *      a walidacja z konstruktora kanonicznego zawsze się wykona.
     */
    // </editor-fold>
}
