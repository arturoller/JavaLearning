package t07_inheritance_polymorphism;

import helpers.Check;

import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Dziedziczenie klas — extends, "is-a", co dziedziczymy a czego nie
 *        (inheritance = dziedziczenie; extends = rozszerza; is-a = "jest rodzajem")
 *
 * W SKRÓCIE:
 *   Klasa może rozszerzyć (extends) inną klasę i automatycznie dostać jej publiczne i chronione
 *   składowe. Mówimy wtedy, że podklasa "jest rodzajem" (is-a) klasy bazowej: Dog is-a Animal.
 *   Pola prywatne i konstruktory NIE są dziedziczone wprost — każda klasa pisze własny konstruktor.
 *
 * ANALOGIA: formularz rodzinny.
 *   Klasa bazowa to ogólny formularz "Zwierzę" (imię, wiek). Klasa pochodna "Pies" dostaje ten sam
 *   formularz i DOPISUJE swoją rubrykę (rasa) — nie przepisuje od nowa tego, co już było.
 *
 * JAK TO DZIAŁA:
 *   class Animal { protected String name; ... }
 *   class Dog extends Animal { private String breed; ... }
 *   Dog to Animal + coś dodatkowego. Konstruktor podklasy MUSI (jawnie albo niejawnie) wywołać
 *   konstruktor klasy bazowej jako PIERWSZĄ instrukcję — obiekt budujemy od korzenia hierarchii w dół.
 *
 * SŁÓWKA:
 *   extends = rozszerza (dziedziczy po); super = odwołanie do klasy bazowej; constructor chain =
 *   łańcuch konstruktorów; root = korzeń (tu: klasa Object); final class = klasa zamknięta na
 *   dziedziczenie; protected = chroniony (widoczny w podklasach); single inheritance = pojedyncze
 *   dziedziczenie (jedna klasa bazowa).
 *
 * ZOBACZ TEŻ: t06_oop_basics/Oop01ClassesObjects (klasy i obiekty od podstaw),
 *             t06_oop_basics/Oop03Encapsulation (private/protected/public),
 *             t07_inheritance_polymorphism/Inherit02Override (nadpisywanie metod — następny krok).
 * </pre>
 */
public class Inherit01Basics {

    // ---------------------------------------------------------------------------------------------
    // Klasy przykładowe są tu jako statyczne klasy zagnieżdżone (nie osobne pliki), żeby cała lekcja
    // była samodzielna i verifier mógł skompilować ją niezależnie od innych lekcji tego rozdziału.
    // W prawdziwym projekcie każda z nich mieszkałaby we własnym pliku.
    // ---------------------------------------------------------------------------------------------

    /** Animal = zwierzę. Klasa bazowa: wspólne dane i zachowanie dla wszystkich zwierząt. */
    static class Animal {
        protected final String name;   // protected = widoczne w TEJ klasie, w tym pakiecie i we WSZYSTKICH podklasach
        private final int age;         // private = widoczne TYLKO wewnątrz Animal — podklasy go nie widzą wprost

        Animal(String name, int age) {
            System.out.println("    Animal(name=" + name + ")");   // ślad wywołania — widać kolejność konstruktorów
            this.name = name;
            this.age = age;
        }

        /** Jedyna droga dla podklas do odczytu age — pole jest private, ale metoda już nie. */
        int getAge() {
            return age;
        }

        String describe() {
            return name + ", " + age + " lat";
        }
    }

    /** Dog is-a Animal ("pies JEST zwierzęciem") — dziedziczy po Animal i dokłada własną rasę. */
    static class Dog extends Animal {
        private final String breed;   // breed = rasa

        Dog(String name, int age, String breed) {
            super(name, age);          // MUSI być pierwszą instrukcją konstruktora — patrz sekcja 3
            System.out.println("    Dog(breed=" + breed + ")");
            this.breed = breed;
        }

        String bark() {
            return name + " (" + breed + ") szczeka: Hau!";   // "name" odziedziczone jako protected — dostępne wprost
        }
    }

    /** Cat is-a Animal — kolejne "rodzeństwo" Dog: też dziedziczy po Animal, ale niezależnie od Dog. */
    static class Cat extends Animal {
        Cat(String name, int age) {
            super(name, age);
            System.out.println("    Cat()");
        }

        String meow() {
            return name + " miauczy: Miau!";
        }
    }

    /** Puppy is-a Dog is-a Animal — trzeci poziom hierarchii, do pokazania pełnego łańcucha konstruktorów. */
    static class Puppy extends Dog {
        Puppy(String name, String breed) {
            super(name, 0, breed);     // szczeniak zaczyna od 0 lat
            System.out.println("    Puppy()");
        }
    }

    /** MojaKlasa jest final — nikt nie może po niej dziedziczyć (patrz sekcja 7, tak jak java.lang.String). */
    static final class Punkt2D {
        final int x;
        final int y;

        Punkt2D(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }

    public static void main(String[] args) {
        title("Inherit01 — dziedziczenie: extends, is-a, co dziedziczymy");

        basics();               // basics = podstawy
        whatIsInherited();      // what is inherited = co jest dziedziczone
        superConstructor();     // super constructor = konstruktor bazowy przez super
        constructorChain();     // constructor chain = łańcuch konstruktorów
        objectRoot();           // Object root = Object jako korzeń hierarchii
        singleInheritance();    // single inheritance = pojedyncze dziedziczenie
        finalClass();           // final class = klasa zamknięta na dziedziczenie
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PODSTAWY: extends i "is-a"
    // =================================================================================================

    /** 1. Dog i Cat dziedziczą po Animal — obie klasy "są rodzajem" Animal, więc mają jego pola i metody. */
    static void basics() {
        section("1. extends i relacja is-a");

        Dog azor = new Dog("Azor", 3, "Kundelek");
        show("azor.bark()", azor.bark());
        show("azor instanceof Animal", azor instanceof Animal);
        show("azor.describe() (odziedziczone z Animal)", azor.describe());
        // WYNIK:     Animal(name=Azor)
        // WYNIK:     Dog(breed=Kundelek)
        // WYNIK: azor.bark() → Azor (Kundelek) szczeka: Hau!
        // WYNIK: azor instanceof Animal → true
        // WYNIK: azor.describe() (odziedziczone z Animal) → Azor, 3 lat

        // DOBRA PRAKTYKA: dziedzicz tylko wtedy, gdy relacja naprawdę jest "is-a". Pies JEST zwierzęciem —
        //   to naturalne. Gdyby klasa miała tylko UŻYWAĆ zwierzęcia (np. Weterynarz), to byłoby "has-a"
        //   (kompozycja) — patrz Inherit06CompositionVsInheritance.
    }

    // =================================================================================================
    // 2. CO JEST DZIEDZICZONE, A CO NIE
    // =================================================================================================

    /**
     * 2. Dziedziczone: pola i metody public/protected (i package-private w tym samym pakiecie).
     * NIE dziedziczone: pola/metody private (widoczne tylko w klasie, w której są zadeklarowane)
     * oraz konstruktory (podklasa zawsze pisze WŁASNY konstruktor, nawet jeśli tylko woła super(...)).
     */
    static void whatIsInherited() {
        section("2. Co jest dziedziczone, a co nie");

        Dog azor = new Dog("Azor", 3, "Kundelek");
        // "name" (protected) jest widoczne wprost wewnątrz Dog — użyte w bark() bez żadnego gettera.
        // "age" jest private w Animal — Dog go NIE WIDZI bezpośrednio:
        //     int a = azor.age;   // BŁĄD KOMPILACJI: age has private access in Animal
        // Jedyna droga to odziedziczona metoda getAge() (ona JEST dziedziczona, bo nie jest private):
        show("azor.getAge() — dostęp przez odziedziczoną metodę", azor.getAge());
        // WYNIK:     Animal(name=Azor)
        // WYNIK:     Dog(breed=Kundelek)
        // WYNIK: azor.getAge() — dostęp przez odziedziczoną metodę → 3

        note("Konstruktory NIE są dziedziczone: Dog nie dostaje 'za darmo' konstruktora Animal(String,int)."
                + " Dog musi mieć własny konstruktor, choćby tylko wołający super(...).");
        // WYNIK: ℹ Konstruktory NIE są dziedziczone: Dog nie dostaje 'za darmo' konstruktora Animal(String,int). Dog musi mieć własny konstruktor, choćby tylko wołający super(...).
        //   new Dog()   // BŁĄD: Dog nie ma konstruktora bezargumentowego — nie "odziedziczył" żadnego z Animal

        // PUŁAPKA: "protected" bywa mylone z "prywatne dla podklas". W rzeczywistości protected jest też
        //   widoczne dla CAŁEGO pakietu, nie tylko dla podklas — to szersza widoczność niż wielu się spodziewa.
    }

    // =================================================================================================
    // 3. super(...) I NIEJAWNY super()
    // =================================================================================================

    /** Base/Derived tylko na potrzeby tej sekcji — pokazują niejawnie wstawiany super(). */
    static class Base {
        Base() {
            System.out.println("    Base() — konstruktor bezargumentowy");
        }
    }

    static class Derived extends Base {
        Derived() {
            // brak jawnego super() -> kompilator SAM wstawia super() jako pierwszą instrukcję
            System.out.println("    Derived()");
        }
    }

    /**
     * 3. Jeśli konstruktor podklasy nie woła jawnie super(...), kompilator wstawia niejawne super()
     * (wołanie konstruktora bezargumentowego klasy bazowej). Jeśli klasa bazowa NIE MA takiego
     * konstruktora (np. Animal ma tylko Animal(String, int)), trzeba wywołać super(...) jawnie.
     */
    static void superConstructor() {
        section("3. super(...) — jawne i niejawne wywołanie konstruktora bazowego");

        note("Derived() nie ma jawnego super() — kompilator sam wstawia super():");
        new Derived();
        // WYNIK: ℹ Derived() nie ma jawnego super() — kompilator sam wstawia super():
        // WYNIK:     Base() — konstruktor bezargumentowy
        // WYNIK:     Derived()

        // PUŁAPKA: super(...) MUSI być PIERWSZĄ instrukcją konstruktora — nawet przed przypisaniem pól:
        //   Dog(String name, int age, String breed) {
        //       this.breed = breed;   // BŁĄD KOMPILACJI: call to super must be first statement in constructor
        //       super(name, age);
        //   }
        // DOBRA PRAKTYKA: jeśli klasa bazowa NIE MA konstruktora bezargumentowego (jak Animal), podklasa
        //   MUSI jawnie wywołać super(...) z odpowiednimi argumentami — inaczej kod się nie skompiluje.
    }

    // =================================================================================================
    // 4. ŁAŃCUCH KONSTRUKTORÓW
    // =================================================================================================

    /**
     * 4. Obiekt budujemy od korzenia hierarchii w dół: najpierw działa konstruktor Animal, potem Dog,
     * na końcu Puppy. Dzięki temu pola klasy bazowej (name, age) są gotowe, zanim ruszy kod podklasy.
     */
    static void constructorChain() {
        section("4. Łańcuch konstruktorów: baza → pochodna → wnuk");

        note("Tworzymy Puppy — kolejność: Animal(...), potem Dog(...), na końcu Puppy():");
        Puppy reksio = new Puppy("Reksio", "Owczarek");
        show("reksio.describe()", reksio.describe());
        // WYNIK: ℹ Tworzymy Puppy — kolejność: Animal(...), potem Dog(...), na końcu Puppy():
        // WYNIK:     Animal(name=Reksio)
        // WYNIK:     Dog(breed=Owczarek)
        // WYNIK:     Puppy()
        // WYNIK: reksio.describe() → Reksio, 0 lat

        // DOBRA PRAKTYKA: dzięki temu porządkowi konstruktor Dog może bezpiecznie korzystać z pól Animal
        //   (są już ustawione) — ale NIE odwrotnie: konstruktor Animal nie może zakładać niczego o Dog
        //   (jeszcze nie istnieje). Więcej o tej pułapce w Inherit02Override (wywołanie nadpisywalnej metody).
    }

    // =================================================================================================
    // 5. Object JAKO KORZEŃ HIERARCHII
    // =================================================================================================

    /**
     * 5. Każda klasa w Javie, nawet bez jawnego "extends", pośrednio dziedziczy po java.lang.Object.
     * Stąd każdy obiekt ma getClass(), equals(Object), hashCode(), toString() — zanim cokolwiek napiszesz.
     */
    static void objectRoot() {
        section("5. Object jako korzeń każdej hierarchii klas");

        Dog azor = new Dog("Azor", 3, "Kundelek");
        show("azor instanceof Object", azor instanceof Object);
        show("azor.getClass().getSimpleName()", azor.getClass().getSimpleName());
        show("azor.equals(azor) (equals odziedziczone z Object)", azor.equals(azor));
        String prefiks = azor.getClass().getName() + "@";
        show("azor.toString() zaczyna się od nazwy klasy?", azor.toString().startsWith(prefiks));
        // WYNIK:     Animal(name=Azor)
        // WYNIK:     Dog(breed=Kundelek)
        // WYNIK: azor instanceof Object → true
        // WYNIK: azor.getClass().getSimpleName() → Dog
        // WYNIK: azor.equals(azor) (equals odziedziczone z Object) → true
        // WYNIK: azor.toString() zaczyna się od nazwy klasy? → true

        note("Domyślny Object.toString() to 'NazwaKlasy@hex' — hex to hashCode, RÓŻNY między uruchomieniami,"
                + " więc nigdy nie wypisujemy go dosłownie w WYNIK (patrz PUŁAPKA niżej).");
        // WYNIK: ℹ Domyślny Object.toString() to 'NazwaKlasy@hex' — hex to hashCode, RÓŻNY między uruchomieniami, więc nigdy nie wypisujemy go dosłownie w WYNIK (patrz PUŁAPKA niżej).

        // PUŁAPKA: nigdy nie wpisujemy w WYNIK dosłownej wartości Object.toString() (np. "Dog@1b6d3586") —
        //   liczba po @ to hashCode „tożsamości” obiektu (identity hash) — JVM nadaje ją w zasadzie losowo, więc zmienia się między uruchomieniami programu.
        //   Nadpisywanie toString() pokazuje Inherit02Override.
    }

    // =================================================================================================
    // 6. POJEDYNCZE DZIEDZICZENIE KLAS
    // =================================================================================================

    /**
     * 6. Klasa w Javie może rozszerzać TYLKO JEDNĄ klasę bazową (w przeciwieństwie do C++). To ogranicza
     * niejednoznaczności (np. "diamentowy problem"). Interfejsów za to można implementować dowolnie wiele
     * — patrz Inherit04Interfaces.
     */
    static void singleInheritance() {
        section("6. Pojedyncze dziedziczenie: tylko jedna klasa bazowa");

        // class Hybryda extends Dog, Cat { }
        //   // BŁĄD KOMPILACJI: class can only extend one other class ('java' może "extends" tylko RAZ)
        //   // Gdyby Dog i Cat miały tę samą metodę odziedziczoną inaczej, kompilator nie wiedziałby,
        //   // której wersji użyć — to tzw. diamentowy problem. Java unika go, zakazując wielodziedziczenia klas.

        show("Dog i Cat mają WSPÓLNEGO przodka (Animal), ale nie dziedziczą jeden po drugim",
                Dog.class.getSuperclass() == Animal.class && Cat.class.getSuperclass() == Animal.class);
        // WYNIK: Dog i Cat mają WSPÓLNEGO przodka (Animal), ale nie dziedziczą jeden po drugim → true

        // DOBRA PRAKTYKA: gdy potrzebujesz "kilku ról naraz" (np. klasa, która JEST Zwierzęciem i UMIE
        //   Latać), użyj jednej klasy bazowej + wielu interfejsów: class Ptak extends Animal implements Latajacy.
    }

    // =================================================================================================
    // 7. KLASA final — ZAMKNIĘTA NA DZIEDZICZENIE
    // =================================================================================================

    /**
     * 7. Klasa final nie może mieć podklas. java.lang.String jest final właśnie z tego powodu — gdyby
     * dowolny kod mógł podmienić zachowanie String, żaden kod w JVM nie mógłby mu bezpiecznie ufać.
     */
    static void finalClass() {
        section("7. Klasa final — nie da się po niej dziedziczyć");

        Punkt2D p = new Punkt2D(2, 5);
        show("p", p);
        show("String.class jest final?", java.lang.reflect.Modifier.isFinal(String.class.getModifiers()));
        show("Punkt2D.class jest final?", java.lang.reflect.Modifier.isFinal(Punkt2D.class.getModifiers()));
        // WYNIK: p → (2, 5)
        // WYNIK: String.class jest final? → true
        // WYNIK: Punkt2D.class jest final? → true

        // class MojStr extends String { }
        //   // BŁĄD KOMPILACJI: cannot inherit from final 'java.lang.String'
        // class Punkt3D extends Punkt2D { }
        //   // BŁĄD KOMPILACJI: cannot inherit from final 'Inherit01Basics.Punkt2D'

        // DOBRA PRAKTYKA: oznaczaj final klasy, których zachowanie ma być GWARANTOWANE i niezmienne
        //   (typowe dla obiektów wartości, patrz t06_oop_basics/Oop09ValueObjects) — nikt nie podmieni
        //   ich zachowania przez podklasę.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • extends = klasa pochodna dostaje pola/metody public i protected klasy bazowej (is-a).
     *   • NIE dziedziczą się: pola/metody private oraz konstruktory — każda klasa pisze własny konstruktor.
     *   • super(...) musi być PIERWSZĄ instrukcją konstruktora; brak jawnego super() = kompilator wstawia
     *     niejawne super() (tylko gdy klasa bazowa ma konstruktor bezargumentowy).
     *   • Kolejność budowy obiektu: od korzenia hierarchii w dół (Animal → Dog → Puppy).
     *   • Każda klasa pośrednio dziedziczy po Object: getClass(), equals(), hashCode(), toString().
     *   • Klasa w Javie ma TYLKO JEDNĄ klasę bazową (pojedyncze dziedziczenie); interfejsów — wiele.
     *   • Klasa final nie może mieć podklas (np. String).
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się protected od private pod kątem widoczności w podklasie?
     *   2. Co wypisze tworzenie `new Puppy("X", "Y")` — w jakiej kolejności działają konstruktory?
     *   3. ZNAJDŹ BŁĄD:
     *          class Dog extends Animal {
     *              Dog(String name, int age, String breed) {
     *                  this.breed = breed;
     *                  super(name, age);
     *              }
     *          }
     *   4. Dlaczego `new Dog()` (bez argumentów) nie skompiluje się, mimo że Dog dziedziczy po Animal?
     *   5. Co dziedziczy KAŻDA klasa w Javie, nawet bez jawnego "extends"?
     *   6. Dlaczego nie można napisać `class Hybryda extends Dog, Cat`?
     *   7. Dlaczego java.lang.String jest final?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");

        Dog azor = new Dog("Azor", 3, "Kundelek");
        Cat mruczek = new Cat("Mruczek", 1);
        Puppy reksio = new Puppy("Reksio", "Owczarek");
        List<Animal> zwierzeta = List.of(azor, mruczek, reksio);

        Check.equal("ćw. 1: Azor (3 lata) jest dorosły (próg 2)", true, () -> isAdult(azor, 2));
        Check.equal("ćw. 2: opisy wszystkich zwierząt", List.of("Azor, 3 lat", "Mruczek, 1 lat", "Reksio, 0 lat"),
                () -> describeAll(zwierzeta));
        Check.equal("ćw. 3: liczba dorosłych (próg 2)", 1, () -> countAdults(zwierzeta, 2));
        Check.equal("ćw. 4: imię najstarszego zwierzęcia", "Azor", () -> oldestName(zwierzeta));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", true, () -> solution1(azor, 2));
        Check.equal("ćw. 2 (wzorzec)", List.of("Azor, 3 lat", "Mruczek, 1 lat", "Reksio, 0 lat"),
                () -> solution2(zwierzeta));
        Check.equal("ćw. 3 (wzorzec)", 1, () -> solution3(zwierzeta, 2));
        Check.equal("ćw. 4 (wzorzec)", "Azor", () -> solution4(zwierzeta));
        Check.summary();
        // WYNIK:     Animal(name=Azor)
        // WYNIK:     Dog(breed=Kundelek)
        // WYNIK:     Animal(name=Mruczek)
        // WYNIK:     Cat()
        // WYNIK:     Animal(name=Reksio)
        // WYNIK:     Dog(breed=Owczarek)
        // WYNIK:     Puppy()
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć true, jeśli wiek zwierzęcia (getAge()) jest ≥ minAge.
     * Podpowiedź: skorzystaj z odziedziczonej metody getAge() — działa tak samo dla Dog, Cat i Puppy.
     */
    static boolean isAdult(Animal a, int minAge) {
        // stub boolean: neutralny "return false" mógłby przypadkiem zdać test, więc zaczynamy od wyjątku
        throw new UnsupportedOperationException("TODO");
    }

    /** ĆWICZENIE 2 (łatwe): zbuduj listę opisów (describe()) wszystkich zwierząt, w kolejności z listy. */
    static List<String> describeAll(List<Animal> animals) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 3 (średnie, PRZEPISZ): PRZED dziedziczeniem trzeba by liczyć dorosłych osobno dla każdego
     * gatunku, bo pole "wiek" i logika porównania byłyby zduplikowane w każdej klasie:
     * <pre>{@code
     * // PRZED (bez wspólnej klasy bazowej) — duplikacja w każdej klasie zwierzęcia:
     * class DogOld { int age; boolean isAdult(int min) { return age >= min; } }
     * class CatOld { int age; boolean isAdult(int min) { return age >= min; } }   // ta sama logika, drugi raz!
     * }</pre>
     * PO: dzięki wspólnej klasie bazowej Animal, jedna metoda ogólna obsługuje WSZYSTKIE podklasy naraz.
     * Zwróć liczbę zwierząt na liście, których wiek jest ≥ minAge.
     */
    static int countAdults(List<Animal> animals, int minAge) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zwróć imię najstarszego zwierzęcia na liście (przy remisie — pierwsze
     * napotkane). Zakładamy, że lista nie jest pusta.
     * Podpowiedź: pętla for-each, trzymaj bieżącego "rekordzistę" w zmiennej lokalnej.
     */
    static String oldestName(List<Animal> animals) {
        // TODO: twoje rozwiązanie
        return null;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static boolean solution1(Animal a, int minAge) {
        return a.getAge() >= minAge;
    }

    static List<String> solution2(List<Animal> animals) {
        List<String> result = new java.util.ArrayList<>();
        for (Animal a : animals) {
            result.add(a.describe());
        }
        return List.copyOf(result);
    }

    static int solution3(List<Animal> animals, int minAge) {
        int count = 0;
        for (Animal a : animals) {
            if (a.getAge() >= minAge) {
                count++;
            }
        }
        return count;
    }

    static String solution4(List<Animal> animals) {
        Animal oldest = animals.get(0);
        for (Animal a : animals) {
            if (a.getAge() > oldest.getAge()) {
                oldest = a;
            }
        }
        return oldest.name;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. protected jest widoczne w klasie, w CAŁYM jej pakiecie i we WSZYSTKICH podklasach (nawet
     *      w innym pakiecie). private jest widoczne TYLKO wewnątrz klasy, w której pole zadeklarowano
     *      — podklasa go w ogóle nie widzi, nawet jeśli dziedziczy.
     *   2. Kolejność: Animal(...), potem Dog(...), na końcu Puppy() — budowa idzie od korzenia hierarchii
     *      w dół, bo każdy konstruktor podklasy musi najpierw wywołać konstruktor bazowy.
     *   3. `this.breed = breed;` wykonuje się PRZED `super(name, age);`, a super(...) musi być pierwszą
     *      instrukcją konstruktora — błąd kompilacji "call to super must be first statement in constructor".
     *   4. Bo konstruktory nie są dziedziczone — Dog ma tylko konstruktor Dog(String, int, String)
     *      napisany jawnie; nie dostaje konstruktora bezargumentowego "za darmo".
     *   5. Każda klasa pośrednio dziedziczy po java.lang.Object: getClass(), equals(Object), hashCode(),
     *      toString() są dostępne od razu, zanim cokolwiek dopiszesz.
     *   6. Bo klasa w Javie może mieć tylko JEDNĄ klasę bazową (pojedyncze dziedziczenie) — to celowe
     *      ograniczenie, które eliminuje "diamentowy problem" znany z wielodziedziczenia w C++.
     *   7. Żeby zagwarantować, że zachowanie String nigdy nie zostanie podmienione przez podklasę —
     *      cały JVM i biblioteki mogą bezpiecznie zakładać, jak String się zachowuje.
     */
    // </editor-fold>
}
