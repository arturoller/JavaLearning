package t08_enums;

import helpers.Check;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Enum z polami, konstruktorem i metodami
 *        (field = pole; constructor = konstruktor; lookup = wyszukiwanie; code = kod)
 *
 * W SKRÓCIE:
 *   Enum to pełnoprawna klasa: każda stała może nieść DANE (pola ustawiane w konstruktorze) i mieć METODY.
 *   Zamiast osobnych switchów „jaka jest polska nazwa?”, „ile kosztuje?”, rozrzuconych po programie,
 *   dane siedzą przy stałej: Size.M.getLabel(). Do zapisu w plikach/bazach dajemy stałej własny, niezmienny KOD.
 *
 * ANALOGIA: etykiety na słoikach.
 *   Sam enum to słoiki z nazwami (MALY, SREDNI, DUZY). Pola to etykiety naklejone na słoik: pojemność, cena,
 *   kod kreskowy. Nie trzeba zaglądać do osobnego zeszytu — wszystko jest na słoiku.
 *
 * JAK TO DZIAŁA:
 *   enum Size {
 *       S("mały", 150), M("średni", 250);     ← wywołanie konstruktora dla każdej stałej (lista MUSI być pierwsza)
 *       private final String label;          ← pola najlepiej final (stałe mają być niezmienne)
 *       private final int ml;
 *       Size(String label, int ml) { ... }   ← konstruktor zawsze prywatny (new Size(...) poza enumem niemożliwe)
 *       public String getLabel() { ... }
 *   }
 *
 * SŁÓWKA:
 *   field = pole; constructor = konstruktor; label = etykieta; code = kod; lookup = wyszukiwanie;
 *   from code = z kodu; price = cena; planet = planeta; mass = masa; radius = promień; gravity = grawitacja;
 *   coffee size = rozmiar kawy; currency = waluta; symbol = symbol.
 *
 * ZOBACZ TEŻ: t08_enums/Enums01Basics (podstawy), t08_enums/Enums03ConstantBodies (różne zachowanie stałych),
 *             t06_oop_basics/Oop02Constructors (konstruktory), t14_optional/Optional01Basics (Optional przy wyszukiwaniu).
 * </pre>
 */
public class Enums02FieldsMethods {

    /** CoffeeSize = rozmiar kawy. Każda stała ma polską etykietę, pojemność i dopłatę w groszach. */
    enum CoffeeSize {
        SMALL("mała", 150, 0),        // ← to jest wywołanie konstruktora CoffeeSize("mała", 150, 0)
        MEDIUM("średnia", 250, 200),
        LARGE("duża", 400, 450);      // ← średnik kończy listę stałych, dalej zwykła „klasa”

        private final String label;       // label = etykieta (polska nazwa do wyświetlania)
        private final int milliliters;    // milliliters = mililitry
        private final int surchargeGrosze; // surcharge = dopłata (w groszach, liczba całkowita — bez błędów double)

        CoffeeSize(String label, int milliliters, int surchargeGrosze) {  // konstruktor enuma jest ZAWSZE prywatny
            this.label = label;
            this.milliliters = milliliters;
            this.surchargeGrosze = surchargeGrosze;
        }

        String getLabel() {
            return label;
        }

        int getMilliliters() {
            return milliliters;
        }

        /** priceGrosze = cena w groszach: cena bazowa + dopłata za rozmiar. Metoda enuma korzysta z jego pól. */
        int priceGrosze(int basePriceGrosze) {
            return basePriceGrosze + surchargeGrosze;
        }
    }

    /**
     * Currency = waluta. Ma KOD (np. "PLN") używany w plikach i bazach — niezależny od nazwy stałej w Javie.
     * Stała nazywa się POLISH_ZLOTY, ale na zewnątrz pokazuje się jako "PLN".
     */
    enum Currency {
        POLISH_ZLOTY("PLN", "zł"),
        EURO("EUR", "€"),
        US_DOLLAR("USD", "$");

        private final String code;       // code = kod (stały, trwały identyfikator)
        private final String symbol;     // symbol = symbol waluty

        /** BY_CODE = mapa „kod → stała”, budowana RAZ przy ładowaniu enuma. Szybkie wyszukiwanie zamiast pętli. */
        private static final Map<String, Currency> BY_CODE = Arrays.stream(values())
                .collect(Collectors.toMap(c -> c.code, Function.identity()));

        Currency(String code, String symbol) {
            this.code = code;
            this.symbol = symbol;
        }

        String getCode() {
            return code;
        }

        String getSymbol() {
            return symbol;
        }

        /**
         * fromCode = z kodu. Wyszukiwanie po WŁASNYM kodzie (nie po name()). Zwraca Optional, bo kod może nie istnieć.
         * Wielkość liter i spacje nie mają znaczenia.
         */
        static Optional<Currency> fromCode(String code) {
            if (code == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(BY_CODE.get(code.strip().toUpperCase(Locale.ROOT)));
        }
    }

    /** Planet = planeta. Klasyczny przykład: stałe z liczbami i metoda licząca na ich podstawie. */
    enum Planet {
        MERCURY(3.303e+23, 2.4397e6),
        EARTH(5.976e+24, 6.37814e6),
        JUPITER(1.9e+27, 7.1492e7);

        private static final double G = 6.67300E-11;   // stała grawitacji
        private final double mass;                      // mass = masa (kg)
        private final double radius;                    // radius = promień (m)

        Planet(double mass, double radius) {
            this.mass = mass;
            this.radius = radius;
        }

        /** surfaceGravity = przyspieszenie grawitacyjne na powierzchni (m/s²). */
        double surfaceGravity() {
            return G * mass / (radius * radius);
        }

        /** weightOn = ciężar na tej planecie dla masy kg. Na Ziemi ≈ 9.8 * masa. */
        double weightOn(double kilograms) {
            return kilograms * surfaceGravity();
        }
    }

    public static void main(String[] args) {
        title("Enums02 — enum z polami, konstruktorem i metodami");

        fieldsAndConstructor();   // fields and constructor = pola i konstruktor
        methodsUsingFields();     // methods using fields = metody korzystające z pól
        beforeAfterSwitch();      // before/after switch = przed/po: switch kontra pole
        lookupByCode();           // lookup by code = wyszukiwanie po kodzie
        calculations();           // calculations = obliczenia
        toStringVsName();         // toString vs name = toString kontra name
        exercises();              // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. POLA I KONSTRUKTOR
    // =================================================================================================

    /** 1. Każda stała wywołuje konstruktor ze swoimi argumentami. Dane są dostępne przez gettery. */
    static void fieldsAndConstructor() {
        section("1. Pola i konstruktor");

        for (CoffeeSize size : CoffeeSize.values()) {
            System.out.println(size + " = " + size.getLabel() + ", " + size.getMilliliters() + " ml");
        }
        // WYNIK: SMALL = mała, 150 ml
        // WYNIK: MEDIUM = średnia, 250 ml
        // WYNIK: LARGE = duża, 400 ml

        // PUŁAPKA: lista stałych MUSI być na początku ciała enuma. Pole przed listą stałych = błąd kompilacji.
        // PUŁAPKA: nie da się napisać new CoffeeSize(...) — konstruktor enuma jest zawsze prywatny (modyfikator
        //   public lub protected przy konstruktorze to błąd kompilacji). Stałe tworzy sama Java, raz.
        // DOBRA PRAKTYKA: pola enuma rób final. Stała jest współdzielona przez cały program — zmiana pola
        //   „w locie” zmieniłaby ją wszędzie, a to bardzo trudny do wykrycia błąd.
    }

    // =================================================================================================
    // 2. METODY KORZYSTAJĄCE Z PÓL
    // =================================================================================================

    /** 2. Metoda enuma zna dane swojej stałej — nie trzeba ich przekazywać. */
    static void methodsUsingFields() {
        section("2. Metody enuma");

        int base = 1200;   // kawa bazowa 12,00 zł w groszach
        show("SMALL", CoffeeSize.SMALL.priceGrosze(base));
        show("LARGE", CoffeeSize.LARGE.priceGrosze(base));
        // WYNIK: SMALL → 1200
        // WYNIK: LARGE → 1650
    }

    // =================================================================================================
    // 3. PRZED / PO: switch rozrzucony po programie kontra pole w enumie
    // =================================================================================================

    /** labelWithSwitch = etykieta przez switch (PRZED). Każda nowa informacja o rozmiarze = kolejny taki switch gdzieś w kodzie. */
    static String labelWithSwitch(CoffeeSize size) {
        return switch (size) {
            case SMALL -> "mała";
            case MEDIUM -> "średnia";
            case LARGE -> "duża";
        };
    }

    /**
     * 3. PRZED: dane o stałej w osobnych switchach w różnych klasach. PO: dane w polu enuma — jedno miejsce.
     * Dodanie rozmiaru EXTRA_LARGE w wersji PO to jedna linia: EXTRA_LARGE("bardzo duża", 500, 700).
     */
    static void beforeAfterSwitch() {
        section("3. PRZED/PO: switch kontra pole");

        show("PRZED (switch)", labelWithSwitch(CoffeeSize.MEDIUM));
        show("PO (pole)", CoffeeSize.MEDIUM.getLabel());
        // WYNIK: PRZED (switch) → średnia
        // WYNIK: PO (pole) → średnia

        // DOBRA PRAKTYKA: jeśli piszesz switch po stałych enuma tylko po to, by dostać jakąś WARTOŚĆ (tekst, liczbę),
        //   przenieś tę wartość do pola enuma. Switch zostaw na logikę, która do enuma nie należy.
    }

    // =================================================================================================
    // 4. WYSZUKIWANIE PO KODZIE
    // =================================================================================================

    /**
     * 4. name() to nazwa w KODZIE Javy — gdy ktoś przemianuje stałą (refaktoryzacja), zapisane dane przestają pasować.
     * Własny kod (np. "PLN") jest niezależny od nazwy stałej. Szukamy przez mapę zbudowaną raz — {@code Map<String, Currency>}.
     */
    static void lookupByCode() {
        section("4. Wyszukiwanie po kodzie (fromCode)");

        show("fromCode(\"EUR\")", Currency.fromCode("EUR"));
        show("fromCode(\" pln \")", Currency.fromCode(" pln "));
        show("fromCode(\"GBP\")", Currency.fromCode("GBP"));
        // WYNIK: fromCode("EUR") → Optional[EURO]
        // WYNIK: fromCode(" pln ") → Optional[POLISH_ZLOTY]
        // WYNIK: fromCode("GBP") → Optional.empty

        String price = Currency.fromCode("usd")
                .map(c -> "19.99 " + c.getSymbol())
                .orElse("nieznana waluta");
        show("cena z symbolem", price);
        // WYNIK: cena z symbolem → 19.99 $

        // PUŁAPKA: pętla po values() przy KAŻDYM wyszukiwaniu działa, ale values() za każdym razem tworzy NOWĄ kopię
        //   tablicy. Przy częstym wyszukiwaniu lepsza jest statyczna mapa zbudowana raz (jak BY_CODE).
        // PUŁAPKA: w konstruktorze enuma NIE da się wstawić stałej do statycznej mapy (pola statyczne nie są jeszcze
        //   gotowe, gdy tworzą się stałe) — dlatego mapę budujemy w inicjalizacji pola statycznego, po stałych.
    }

    // =================================================================================================
    // 5. OBLICZENIA NA POLACH
    // =================================================================================================

    /** 5. Planet: stałe z danymi liczbowymi i metody obliczeniowe. */
    static void calculations() {
        section("5. Obliczenia: ciężar na planetach");

        double kg = 70;
        for (Planet p : Planet.values()) {
            System.out.println(String.format(Locale.ROOT, "%-8s g = %5.2f m/s², 70 kg waży jak %6.1f kg na Ziemi",
                    p, p.surfaceGravity(), p.weightOn(kg) / Planet.EARTH.surfaceGravity()));
        }
        // WYNIK: MERCURY  g =  3.70 m/s², 70 kg waży jak   26.4 kg na Ziemi
        // WYNIK: EARTH    g =  9.80 m/s², 70 kg waży jak   70.0 kg na Ziemi
        // WYNIK: JUPITER  g = 24.81 m/s², 70 kg waży jak  177.1 kg na Ziemi
    }

    // =================================================================================================
    // 6. toString KONTRA name
    // =================================================================================================

    /** Level = poziom trudności. Nadpisany toString: do wyświetlania polska nazwa. name() zostaje nazwą stałej. */
    enum Level {
        EASY("łatwy"), HARD("trudny");

        private final String polishName;

        Level(String polishName) {
            this.polishName = polishName;
        }

        @Override
        public String toString() {       // toString = na tekst (to, co widzi użytkownik)
            return polishName;
        }
    }

    /**
     * 6. toString() można nadpisać (np. ładna nazwa do wyświetlania), ale name() jest final — zawsze zwraca nazwę stałej.
     * valueOf() szuka po name(), NIE po toString().
     */
    static void toStringVsName() {
        section("6. toString() kontra name()");

        show("toString", Level.HARD);
        show("name()", Level.HARD.name());
        // WYNIK: toString → trudny
        // WYNIK: name() → HARD

        expectThrows("valueOf(\"trudny\")", () -> Level.valueOf("trudny"));
        // WYNIK: ✔ valueOf("trudny") → rzucono IllegalArgumentException: No enum constant t08_enums.Enums02FieldsMethods.Level.trudny

        // DOBRA PRAKTYKA: do zapisu/odczytu używaj name() albo własnego kodu, nigdy toString() — ten może się zmienić
        //   (np. tłumaczenie interfejsu), a dane w plikach przestaną pasować.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Stałe z argumentami: SMALL("mała", 150) — wywołanie konstruktora; lista stałych zawsze na początku, zakończona ;
     *   • Konstruktor enuma jest zawsze prywatny; pola rób final (stała jest wspólna dla całego programu).
     *   • Metody enuma korzystają z pól swojej stałej: size.priceGrosze(base).
     *   • Wartości przypisane do stałych trzymaj w polach, nie w switchach rozsianych po kodzie.
     *   • Własny kod + statyczna mapa BY_CODE + fromCode zwracający Optional — trwałe i szybkie wyszukiwanie.
     *   • toString() — do wyświetlania (można nadpisać); name() — final, nazwa stałej; valueOf szuka po name().
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego konstruktor enuma nie może być publiczny?
     *   2. Co wypisze:  System.out.println(Level.EASY + " / " + Level.EASY.name());  ?
     *   3. ZNAJDŹ BŁĄD:
     *          enum Color { private final String hex; RED("#f00"), GREEN("#0f0"); Color(String h) { hex = h; } }
     *   4. Po co enumowi własny kod (np. "PLN"), skoro ma name()?
     *   5. Dlaczego pola enuma powinny być final?
     *   6. Co wypisze:  System.out.println(Currency.fromCode("eur").map(Currency::getSymbol).orElse("?"));  ?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: łączna pojemność wszystkich rozmiarów", 800, () -> exercise1());
        Check.equal("ćw. 2a: rozmiar dla 250 ml", Optional.of(CoffeeSize.MEDIUM), () -> exercise2(250));
        Check.equal("ćw. 2b: rozmiar dla 300 ml", Optional.empty(), () -> exercise2(300));
        Check.equal("ćw. 3: najmniejszy rozmiar mieszczący 200 ml", CoffeeSize.MEDIUM, () -> exercise3(200));
        Check.equal("ćw. 4: kody walut", "PLN,EUR,USD", () -> exercise4());
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 800, () -> solution1());
        Check.equal("ćw. 2a (wzorzec)", Optional.of(CoffeeSize.MEDIUM), () -> solution2(250));
        Check.equal("ćw. 2b (wzorzec)", Optional.empty(), () -> solution2(300));
        Check.equal("ćw. 3 (wzorzec)", CoffeeSize.MEDIUM, () -> solution3(200));
        Check.equal("ćw. 4 (wzorzec)", "PLN,EUR,USD", () -> solution4());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /** ĆWICZENIE 1 (łatwe): zsumuj pojemności (getMilliliters) wszystkich rozmiarów kawy. Podpowiedź: pętla po values(). */
    static int exercise1() {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (średnie): znajdź rozmiar o DOKŁADNIE podanej pojemności; gdy brak — Optional.empty().
     * Podpowiedź: pętla po values() i return Optional.of(size) przy trafieniu (albo stream + filter + findFirst).
     */
    static Optional<CoffeeSize> exercise2(int milliliters) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć NAJMNIEJSZY rozmiar, który zmieści podaną ilość (pojemność ≥ ml).
     * Rozmiary są zadeklarowane rosnąco, więc wystarczy pierwszy pasujący. Gdy żaden — rzuć IllegalArgumentException.
     */
    static CoffeeSize exercise3(int milliliters) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 4 (łączy z t16_streams): zwróć kody wszystkich walut połączone przecinkiem, w kolejności deklaracji.
     * Podpowiedź: Arrays.stream(Currency.values()).map(Currency::getCode).collect(Collectors.joining(","))
     */
    static String exercise4() {
        // TODO: twoje rozwiązanie
        return null;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1() {
        int sum = 0;
        for (CoffeeSize size : CoffeeSize.values()) {
            sum += size.getMilliliters();
        }
        return sum;
    }

    static Optional<CoffeeSize> solution2(int milliliters) {
        return Arrays.stream(CoffeeSize.values())
                .filter(s -> s.getMilliliters() == milliliters)
                .findFirst();
    }

    static CoffeeSize solution3(int milliliters) {
        for (CoffeeSize size : CoffeeSize.values()) {
            if (size.getMilliliters() >= milliliters) {
                return size;
            }
        }
        throw new IllegalArgumentException("Brak rozmiaru na " + milliliters + " ml");
    }

    static String solution4() {
        return Arrays.stream(Currency.values())
                .map(Currency::getCode)
                .collect(Collectors.joining(","));
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Enum ma mieć zamkniętą listę stałych. Gdyby dało się wywołać new, można by tworzyć nowe „stałe” poza listą.
     *      Dlatego konstruktor jest zawsze prywatny, a stałe tworzy sama Java, raz.
     *   2. „łatwy / EASY” — toString() jest nadpisany, name() zwraca nazwę stałej.
     *   3. Lista stałych musi być pierwsza w ciele enuma: RED("#f00"), GREEN("#0f0"); a dopiero potem pole hex.
     *   4. name() zmieni się przy przemianowaniu stałej w kodzie, a zapisane dane (pliki, baza, API) nie.
     *      Własny kod jest trwałym identyfikatorem niezależnym od nazw w Javie.
     *   5. Każda stała istnieje w jednym egzemplarzu i jest wspólna dla całego programu — zmiana pola w jednym miejscu
     *      zmieniłaby ją wszędzie (ukryty stan globalny).
     *   6. „€”.
     */
    // </editor-fold>
}
