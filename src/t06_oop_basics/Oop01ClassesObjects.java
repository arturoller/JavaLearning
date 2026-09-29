package t06_oop_basics;

import helpers.Check;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Klasy i obiekty — plan i egzemplarze
 *        (class = klasa, object = obiekt, instance = egzemplarz, reference = referencja)
 *
 * W SKRÓCIE:
 *   Klasa to plan: opisuje, jakie dane (pola) i jakie zachowania (metody) ma mieć obiekt.
 *   Obiekt to konkretny egzemplarz zbudowany według planu operatorem new.
 *   Zmienna typu klasowego NIE przechowuje obiektu — przechowuje referencję (adres) do niego.
 *
 * ANALOGIA:
 *   Klasa to projekt domu u architekta, obiekt to konkretny dom na działce. Z jednego projektu stawiasz
 *   wiele domów — każdy ma własny kolor ścian i własnych lokatorów. Zmienna to kartka z adresem domu:
 *   możesz ją skserować (dwie kartki, jeden dom), ale kserując kartkę nie budujesz nowego domu.
 *
 * JAK TO DZIAŁA:
 *   Dog rex = new Dog();
 *     1. new Dog() — JVM rezerwuje miejsce na STERCIE (heap) i wypełnia pola wartościami domyślnymi,
 *     2. uruchamia konstruktor (szczegóły: Oop02Constructors),
 *     3. do zmiennej rex na STOSIE (stack) trafia referencja (adres) nowego obiektu.
 *
 *     STOS (zmienne lokalne)          STERTA (obiekty)
 *     ┌─────────────┐                ┌───────────────────────────┐
 *     │ rex   ──────┼───────────────▶│ Dog { name="Rex", age=3 } │
 *     │ burek ──────┼─────┐          └───────────────────────────┘
 *     └─────────────┘     │          ┌─────────────────────────────┐
 *                         └─────────▶│ Dog { name="Burek", age=5 } │
 *                                    └─────────────────────────────┘
 *
 *   Pole (field) = zmienna należąca do obiektu; KAŻDY obiekt ma własną kopię pól.
 *   Metoda instancji = funkcja wywoływana NA obiekcie: rex.bark(); w jej środku this = „ten obiekt”.
 *
 * SŁÓWKA:
 *   class = klasa; object = obiekt; instance = egzemplarz; field = pole; method = metoda;
 *   new = nowy (operator tworzenia obiektu); reference = referencja (adres); heap = sterta; stack = stos;
 *   null = brak obiektu; alias = druga nazwa tego samego obiektu; this = ten (bieżący obiekt); state = stan
 *
 * ZOBACZ TEŻ: t06_oop_basics/Oop02Constructors (wygodne ustawianie stanu startowego),
 *             t01_basics/Basics09PassByValue (przekazywanie referencji do metod),
 *             t06_oop_basics/Oop05ObjectMethods (toString i equals — wypisywanie i porównywanie obiektów)
 * </pre>
 */
public class Oop01ClassesObjects {

    public static void main(String[] args) {
        title("Oop01 — klasy i obiekty");

        classAndObject();       // class and object = klasa i obiekt
        defaultValues();        // default values = wartości domyślne
        methodsAndState();      // methods and state = metody i stan
        independentObjects();   // independent objects = niezależne obiekty
        aliases();              // aliases = aliasy (dwie zmienne, jeden obiekt)
        objectsAndMethods();    // objects and methods = obiekty przekazywane do metod
        nullReference();        // null reference = pusta referencja
        thisKeyword();          // this keyword = słowo kluczowe this
        defaultToString();      // default toString = domyślny toString
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // KLASY PRZYKŁADOWE
    // Dlaczego „static class” WEWNĄTRZ klasy lekcji? W prawdziwym projekcie KAŻDA klasa leży w osobnym
    // pliku (Dog.java, Cat.java...). Tu chcemy, by cała lekcja była jednym plikiem, więc wkładamy klasy
    // do środka jako statyczne klasy zagnieżdżone (static nested class). Słowo static wyjaśnia
    // t06_oop_basics/Oop04Static, a rodzaje klas zagnieżdżonych — t06_oop_basics/Oop07NestedClasses.
    // Na razie traktuj je dokładnie tak, jakby były osobnymi plikami.
    // =================================================================================================

    /** Pies — plan: jakie pola (dane) i jakie metody (zachowania) ma KAŻDY pies. */
    static class Dog {                  // Dog = pies
        String name;                    // name = imię (pole, ang. field)
        int age;                        // age = wiek

        String bark() {                 // bark = szczekaj
            return name + " robi: hau!";
        }

        void haveBirthday() {           // have birthday = obchodź urodziny
            age++;                      // zmienia STAN tego konkretnego obiektu
        }

        String describe() {             // describe = opisz
            return name + " (" + age + " l.)";
        }

        boolean isOlderThan(Dog other) {        // is older than = czy starszy niż; other = inny
            return this.age > other.age;        // this.age = MÓJ wiek, other.age = wiek TAMTEGO psa
        }
    }

    /** Klasa tylko do pokazania wartości domyślnych pól. */
    static class Defaults {             // defaults = wartości domyślne
        int number;                     // number = liczba
        double price;                   // price = cena
        boolean active;                 // active = aktywny
        String text;                    // text = tekst
        int[] values;                   // values = wartości (tablica też jest obiektem!)
    }

    // =================================================================================================
    // 1. KLASA I OBIEKT
    // =================================================================================================

    /**
     * 1. Klasa Dog to tylko plan. Dopiero {@code new Dog()} tworzy obiekt, a kropka pozwala dobrać się do
     * jego pól i metod.
     */
    static void classAndObject() {
        section("1. Klasa = plan, obiekt = egzemplarz");

        Dog rex = new Dog();            // new = utwórz NOWY obiekt według planu Dog
        rex.name = "Rex";               // kropka = „wejdź do obiektu”: pole name obiektu rex
        rex.age = 3;

        show("rex.name", rex.name);
        // WYNIK: rex.name → Rex
        show("rex.age", rex.age);
        // WYNIK: rex.age → 3
        show("rex.bark()", rex.bark());
        // WYNIK: rex.bark() → Rex robi: hau!
        show("rex.describe()", rex.describe());
        // WYNIK: rex.describe() → Rex (3 l.)

        // Klasa sama w sobie nie ma imienia ani wieku — ma je dopiero obiekt. Zapis Dog.name nie
        // ma sensu (błąd kompilacji: non-static variable name cannot be referenced from a static context).

        // DOBRA PRAKTYKA: nazwy klas piszemy WielkąLiterą (Dog, BankAccount), a pól i metod
        // małąLiterą (name, haveBirthday). Klasa = rzeczownik, metoda = czasownik.

        // UWAGA: bezpośrednie przypisywanie pól (rex.age = 3) robimy tu tylko dla prostoty.
        // Za chwilę poznasz konstruktory (Oop02), a w Oop03 zobaczysz, czemu pola chowa się za metodami.
    }

    // =================================================================================================
    // 2. WARTOŚCI DOMYŚLNE PÓL
    // =================================================================================================

    /**
     * 2. Pola, którym nic nie przypisano, dostają wartości domyślne: 0, 0.0, false, null.
     * Zmienne lokalne w metodzie — NIE (kompilator wymaga przypisania).
     */
    static void defaultValues() {
        section("2. Wartości domyślne pól");

        Defaults d = new Defaults();
        show("int", d.number);
        // WYNIK: int → 0
        show("double", d.price);
        // WYNIK: double → 0.0
        show("boolean", d.active);
        // WYNIK: boolean → false
        show("String", d.text);
        // WYNIK: String → null
        show("int[]", d.values);
        // WYNIK: int[] → null

        // JAK TO DZIAŁA: typy proste (int, double, boolean, char) → „zero” swojego typu;
        // wszystkie typy referencyjne (String, tablice, Dog...) → null, czyli „nie wskazuje na nic”.
        // char dostaje znak o kodzie 0 (niewidoczny, dlatego go tu nie wypisujemy).

        // PUŁAPKA: zmienna LOKALNA nie ma wartości domyślnej:
        //     int x;
        //     System.out.println(x);   // błąd kompilacji: variable x might not have been initialized
        // Pole obiektu — ma. To celowe: pole może ustawić później inna metoda, lokalnej zmiennej — nikt.

        // PUŁAPKA: wartość domyślna bywa „cichym błędem”. Pies bez imienia wypisze „null robi: hau!”.
        Dog noName = new Dog();
        show("pies bez imienia", noName.bark());
        // WYNIK: pies bez imienia → null robi: hau!
        // Rozwiązanie: konstruktor, który WYMUSI podanie imienia (t06_oop_basics/Oop02Constructors).
    }

    // =================================================================================================
    // 3. METODY I STAN
    // =================================================================================================

    /**
     * 3. Metoda instancji działa na polach „swojego” obiektu. Wywołanie może zmienić stan obiektu
     * (haveBirthday) albo tylko coś policzyć i zwrócić (describe).
     */
    static void methodsAndState() {
        section("3. Metody instancji zmieniają stan obiektu");

        Dog burek = new Dog();
        burek.name = "Burek";
        burek.age = 5;

        show("przed urodzinami", burek.describe());
        // WYNIK: przed urodzinami → Burek (5 l.)
        burek.haveBirthday();           // metoda void — nic nie zwraca, zmienia stan
        burek.haveBirthday();
        show("po dwóch urodzinach", burek.describe());
        // WYNIK: po dwóch urodzinach → Burek (7 l.)

        // JAK TO DZIAŁA: w środku haveBirthday() jest „age++”. Które age? Tego obiektu, na którym
        // wywołano metodę (burek). Java po cichu przekazuje metodzie referencję do obiektu — to jest this.

        // Stan = aktualne wartości wszystkich pól. Obiekt = stan + zachowanie (metody).
        // DOBRA PRAKTYKA: metoda, która coś zmienia, ma nazwę-czasownik (haveBirthday, rename);
        // metoda, która tylko odpowiada, ma nazwę-pytanie lub rzeczownik (describe, isOlderThan).
    }

    // =================================================================================================
    // 4. DWA OBIEKTY = DWA NIEZALEŻNE STANY
    // =================================================================================================

    /**
     * 4. Każdy {@code new} tworzy osobny obiekt z własnymi polami. Zmiana jednego nie dotyka drugiego.
     */
    static void independentObjects() {
        section("4. Dwa obiekty — niezależny stan");

        Dog first = new Dog();          // first = pierwszy
        first.name = "Azor";
        first.age = 2;

        Dog second = new Dog();         // second = drugi
        second.name = "Luna";
        second.age = 2;

        first.haveBirthday();
        first.name = "Azorek";

        show("first", first.describe());
        // WYNIK: first → Azorek (3 l.)
        show("second", second.describe());
        // WYNIK: second → Luna (2 l.)

        // ANALOGIA: dwa domy z tego samego projektu. Przemalowanie jednego nie zmienia koloru drugiego.
    }

    // =================================================================================================
    // 5. ALIAS — DWIE ZMIENNE, JEDEN OBIEKT
    // =================================================================================================

    /**
     * 5. Przypisanie {@code b = a} kopiuje REFERENCJĘ, nie obiekt. Obie zmienne wskazują ten sam obiekt.
     * Operator == dla obiektów sprawdza, czy to TEN SAM obiekt (ten sam adres).
     */
    static void aliases() {
        section("5. Alias kontra osobny obiekt");

        Dog a = new Dog();
        a.name = "Szarik";
        Dog b = a;                      // alias: b wskazuje TEN SAM obiekt co a (skopiowano adres)
        b.name = "Szarik II";           // zmiana „przez b”...

        show("a.name", a.name);         // ...widać „przez a” — bo to jeden i ten sam pies
        // WYNIK: a.name → Szarik II
        show("a == b", a == b);
        // WYNIK: a == b → true

        Dog c = new Dog();              // osobny obiekt z identycznym imieniem
        c.name = "Szarik II";
        show("a == c", a == c);
        // WYNIK: a == c → false

        //     STOS              STERTA
        //     a ──┐
        //         ├──────▶ Dog { name="Szarik II" }      ← jeden obiekt, dwie „kartki z adresem”
        //     b ──┘
        //     c ─────────▶ Dog { name="Szarik II" }      ← drugi obiekt (takie same dane, inny adres)

        // PUŁAPKA: == na obiektach NIE porównuje zawartości. Dwa psy o tym samym imieniu to dla == dwa
        // różne psy. Porównanie „po zawartości” to metoda equals — t06_oop_basics/Oop05ObjectMethods.
        // (To samo dotyczy String — dlatego teksty porównujemy equals, patrz t04_strings/Strings01Basics.)

        // Obiekt, na który nie wskazuje już żadna zmienna, staje się „śmieciem” — sprząta go
        // garbage collector (GC = odśmiecacz), patrz t26_jvm/Jvm03GarbageCollection.
        c = null;                       // nikt już nie pamięta adresu drugiego obiektu → GC go kiedyś usunie
        show("c po wyzerowaniu", c);
        // WYNIK: c po wyzerowaniu → null
    }

    // =================================================================================================
    // 6. OBIEKTY PRZEKAZYWANE DO METOD
    // =================================================================================================

    /**
     * 6. Java przekazuje do metody KOPIĘ referencji. Metoda może zmienić obiekt (ten sam adres),
     * ale podmiana parametru na nowy obiekt nie dotyka zmiennej wywołującego.
     */
    static void objectsAndMethods() {
        section("6. Obiekty przekazywane do metod");

        Dog dog = new Dog();
        dog.name = "Kajtek";

        rename(dog, "Kajtuś");          // rename = zmień imię — zmienia obiekt pod tym adresem
        show("po rename", dog.name);
        // WYNIK: po rename → Kajtuś

        replace(dog);                   // replace = podmień — podmienia tylko SWOJĄ kopię referencji
        show("po replace", dog.name);
        // WYNIK: po replace → Kajtuś

        // JAK TO DZIAŁA: parametr d w metodzie to druga „kartka z adresem”. Zmiana d.name idzie pod
        // adres — widać ją wszędzie. Przypisanie d = new Dog() zmienia tylko kartkę w metodzie.
        // Szczegóły: t01_basics/Basics09PassByValue.
    }

    static void rename(Dog d, String newName) {     // new name = nowe imię
        d.name = newName;
    }

    static void replace(Dog d) {
        d = new Dog();                  // d wskazuje teraz NOWY obiekt — oryginał nietknięty
        d.name = "Obcy";
    }

    // =================================================================================================
    // 7. NULL I NULLPOINTEREXCEPTION
    // =================================================================================================

    /**
     * 7. null = zmienna nie wskazuje żadnego obiektu. Próba wywołania metody lub odczytu pola przez
     * null kończy się wyjątkiem NullPointerException (NPE).
     */
    static void nullReference() {
        section("7. null i NullPointerException");

        Dog nobody = null;              // nobody = nikt — kartka z adresem jest pusta
        show("nobody == null", nobody == null);
        // WYNIK: nobody == null → true

        // PUŁAPKA: kropka na null = wyjątek. Nie ma obiektu, więc nie ma czego „szczekać”.
        expectThrows("wywołanie metody na null", () -> barkOf(nobody));
        // WYNIK: ✔ wywołanie metody na null → rzucono NullPointerException: Cannot invoke "t06_oop_basics.Oop01ClassesObjects$Dog.bark()" because "d" is null
        expectThrows("odczyt pola przez null", () -> nameOf(nobody));
        // WYNIK: ✔ odczyt pola przez null → rzucono NullPointerException: Cannot read field "name" because "d" is null
        // Czytelny komunikat („because "d" is null”) to tzw. helpful NPE (Java 14+) — mówi, KTÓRA
        // zmienna była pusta. Nazwę zmiennej widać, gdy kod skompilowano z informacjami dla debuggera (-g).

        // DOBRA PRAKTYKA: sprawdzaj null tam, gdzie null jest dozwolony, zanim użyjesz kropki.
        String safe = (nobody != null) ? nobody.bark() : "brak psa";
        show("bezpiecznie", safe);
        // WYNIK: bezpiecznie → brak psa
        // Lepsze sposoby na „może nie być wartości” poznasz w t14_optional/Optional01Basics.
    }

    static String barkOf(Dog d) {       // bark of = szczekanie (psa) d
        return d.bark();
    }

    static String nameOf(Dog d) {       // name of = imię (psa) d
        return d.name;
    }

    // =================================================================================================
    // 8. THIS — BIEŻĄCY OBIEKT
    // =================================================================================================

    /**
     * 8. W metodzie instancji {@code this} to referencja do obiektu, na którym metodę wywołano.
     * Zwykle można ją pominąć (name to skrót od this.name), ale przydaje się przy porównywaniu
     * „mnie” z innym obiektem i w konstruktorach (Oop02).
     */
    static void thisKeyword() {
        section("8. this — „ten obiekt”");

        Dog rex = new Dog();
        rex.name = "Rex";
        rex.age = 3;
        Dog burek = new Dog();
        burek.name = "Burek";
        burek.age = 5;

        show("rex.isOlderThan(burek)", rex.isOlderThan(burek));     // this = rex, other = burek
        // WYNIK: rex.isOlderThan(burek) → false
        show("burek.isOlderThan(rex)", burek.isOlderThan(rex));     // this = burek, other = rex
        // WYNIK: burek.isOlderThan(rex) → true

        // JAK TO DZIAŁA: ta sama metoda, różne wyniki — bo this wskazuje inny obiekt.
        // W metodzie describe() piszemy po prostu name — kompilator czyta to jako this.name.
        // this jest niezbędne, gdy parametr ma tę samą nazwę co pole (this.name = name) — Oop02.
    }

    // =================================================================================================
    // 9. DOMYŚLNY TOSTRING — CZEMU WYPISANY OBIEKT WYGLĄDA DZIWNIE
    // =================================================================================================

    /**
     * 9. Klasa bez własnego toString dziedziczy wersję z klasy Object: pełna nazwa klasy + „@” +
     * liczba szesnastkowa (skrót adresu, tzw. hash). Ta liczba jest INNA przy każdym uruchomieniu.
     */
    static void defaultToString() {
        section("9. Domyślny toString — nazwa klasy i „hash”");

        Dog rex = new Dog();
        rex.name = "Rex";

        String printed = String.valueOf(rex);   // dokładnie to wypisałby System.out.println(rex)
        // Nie wypisujemy printed w całości — hash zmienia się między uruchomieniami, np.
        // t06_oop_basics.Oop01ClassesObjects$Dog@1b6d3586 (liczba po @ będzie inna).
        show("getClass().getSimpleName()", rex.getClass().getSimpleName());
        // WYNIK: getClass().getSimpleName() → Dog
        show("getClass().getName()", rex.getClass().getName());
        // WYNIK: getClass().getName() → t06_oop_basics.Oop01ClassesObjects$Dog
        show("zaczyna się od nazwy klasy i @", printed.startsWith(rex.getClass().getName() + "@"));
        // WYNIK: zaczyna się od nazwy klasy i @ → true
        // getClass = pobierz klasę; getSimpleName = krótka nazwa; getName = pełna nazwa;
        // znak $ oznacza klasę zagnieżdżoną (Dog wewnątrz Oop01ClassesObjects).

        // DOBRA PRAKTYKA: do czytelnego wypisywania nadpisujemy toString — t06_oop_basics/Oop05ObjectMethods.
        // Na razie korzystamy z własnej metody describe().
        show("describe()", rex.describe());
        // WYNIK: describe() → Rex (0 l.)
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • class Dog { String name; void bark() {...} }   — plan: pola (dane) + metody (zachowanie)
     *   • Dog rex = new Dog();   — new tworzy obiekt na stercie, zmienna trzyma REFERENCJĘ (adres)
     *   • rex.name / rex.bark() — kropka = dostęp do pola/metody konkretnego obiektu
     *   • pola mają wartości domyślne (0, 0.0, false, null); zmienne lokalne — nie
     *   • każdy new = osobny obiekt z własnym stanem
     *   • Dog b = a;  → ALIAS (ten sam obiekt), a == b → true;  == porównuje adresy, nie zawartość
     *   • metoda dostaje kopię referencji: może zmienić obiekt, nie podmieni zmiennej wywołującego
     *   • null.cokolwiek → NullPointerException; sprawdzaj null, gdy jest dozwolony
     *   • this = obiekt, na którym wywołano metodę; name == this.name wewnątrz metody
     *   • domyślny toString = NazwaKlasy@hash (zmienny!) — nadpisz go (Oop05)
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się klasa od obiektu? Podaj własną analogię.
     *   2. Co wypisze:  Dog a = new Dog(); a.age = 1; Dog b = a; b.age = 9; System.out.println(a.age);  ?
     *   3. Co wypisze:  Dog a = new Dog(); Dog b = new Dog(); System.out.println(a == b);  ?
     *   4. ZNAJDŹ BŁĄD:  Dog d = null; if (d.name == null) { d = new Dog(); }
     *   5. Jakie wartości ma nowy obiekt klasy Defaults w polach number, active i text?
     *   6. Metoda void reset(Dog d) { d = new Dog(); } — czy po reset(rex) pies rex ma zresetowany stan?
     *   7. Po co piszemy this.age, skoro samo age też działa?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: pole prostokąta 3 x 4", 12, () -> exercise1());
        Check.equal("ćw. 2: dwa niezależne liczniki", "a=3, b=1", () -> exercise2());
        Check.equal("ćw. 3: kopia punktu, a nie alias", "kopia=(1,2), oryginał=(10,2), ten sam=false",
                () -> exercise3());
        Check.equal("ćw. 4: wartość magazynu z obiektów", "suma=1090, najcenniejsza=Monitor",
                () -> exercise4());
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 12, () -> solution1());
        Check.equal("ćw. 2 (wzorzec)", "a=3, b=1", () -> solution2());
        Check.equal("ćw. 3 (wzorzec)", "kopia=(1,2), oryginał=(10,2), ten sam=false", () -> solution3());
        Check.equal("ćw. 4 (wzorzec)", "suma=1090, najcenniejsza=Monitor", () -> solution4());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /** Klasa do ćwiczenia 1 — uzupełnij metodę area. */
    static class Ex1Rectangle {         // rectangle = prostokąt
        int width;                      // width = szerokość
        int height;                     // height = wysokość

        int area() {                    // area = pole powierzchni
            // TODO: zwróć szerokość razy wysokość
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * ĆWICZENIE 1 (łatwe): uzupełnij metodę area() w klasie Ex1Rectangle, a tutaj utwórz prostokąt
     * 3 x 4 (ustaw pola width i height) i zwróć jego pole.
     * Podpowiedź: w metodzie area() pola width i height to pola „tego” prostokąta (this.width).
     */
    static int exercise1() {
        Ex1Rectangle r = new Ex1Rectangle();
        r.width = 3;
        r.height = 4;
        return r.area();
    }

    /** Klasa do ćwiczenia 2 — licznik kliknięć. */
    static class Ex2Counter {           // counter = licznik
        int value;                      // value = wartość

        void increment() {              // increment = zwiększ o 1
            // TODO: zwiększ value o 1
        }
    }

    /**
     * ĆWICZENIE 2 (łatwe): uzupełnij increment() w Ex2Counter. Metoda exercise2 tworzy DWA liczniki,
     * klika pierwszym 3 razy, drugim raz i zwraca tekst „a=3, b=1”.
     * Podpowiedź: każdy obiekt ma własne pole value — liczniki nie przeszkadzają sobie.
     */
    static String exercise2() {
        Ex2Counter a = new Ex2Counter();
        Ex2Counter b = new Ex2Counter();
        a.increment();
        a.increment();
        a.increment();
        b.increment();
        return "a=" + a.value + ", b=" + b.value;
    }

    /** Punkt do ćwiczeń 3. */
    static class Ex3Point {             // point = punkt
        int x;
        int y;
    }

    /**
     * ĆWICZENIE 3 (średnie): napisz copyOf(p), która zwraca NOWY punkt o tych samych współrzędnych
     * (a nie alias!). Metoda exercise3 kopiuje punkt (1,2), potem zmienia x ORYGINAŁU na 10.
     * Kopia ma zostać (1,2). Oczekiwany tekst: „kopia=(1,2), oryginał=(10,2), ten sam=false”.
     * Podpowiedź: return p; to alias — potrzebujesz new Ex3Point() i przepisania pól.
     */
    static Ex3Point copyOf(Ex3Point p) {            // copy of = kopia (czegoś)
        // TODO: utwórz nowy punkt i przepisz x oraz y
        throw new UnsupportedOperationException("TODO");
    }

    static String exercise3() {
        Ex3Point original = new Ex3Point();         // original = oryginał
        original.x = 1;
        original.y = 2;
        Ex3Point copy = copyOf(original);           // copy = kopia
        original.x = 10;
        return "kopia=(" + copy.x + "," + copy.y + "), oryginał=(" + original.x + "," + original.y
                + "), ten sam=" + (copy == original);
    }

    /** Pozycja magazynowa do ćwiczenia 4. */
    static class Ex4Item {              // item = pozycja (towar)
        String name;
        int price;                      // cena w złotych (całkowita, dla prostoty)
        int quantity;                   // quantity = ilość

        int value() {                   // value = wartość = cena * ilość
            // TODO: zwróć price * quantity
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): PRZEPISZ „tablice równoległe” na tablicę obiektów.
     * Stary kod trzyma dane jednego towaru w trzech tablicach — łatwo pomylić indeksy:
     * <pre>{@code
     * String[] names = {"Kabel", "Monitor", "Mysz"};
     * int[] prices   = {15, 999, 61};
     * int[] qty      = {2, 1, 1};
     * }</pre>
     * Zbuduj tablicę Ex4Item[] (nazwa, cena, ilość w jednym obiekcie), uzupełnij value() i zwróć
     * „suma=S, najcenniejsza=N”, gdzie S = suma value() wszystkich pozycji, N = nazwa pozycji o
     * największym value(). Oczekiwane: „suma=1090, najcenniejsza=Monitor”.
     * Podpowiedź: pętla for-each po Ex4Item[] i zmienna „best” pamiętająca najlepszy dotąd obiekt.
     */
    static String exercise4() {
        // TODO: zbuduj Ex4Item[] z danych powyżej i policz wynik
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static class Sol1Rectangle {
        int width;
        int height;

        int area() {
            return width * height;
        }
    }

    static int solution1() {
        Sol1Rectangle r = new Sol1Rectangle();
        r.width = 3;
        r.height = 4;
        return r.area();
    }

    static class Sol2Counter {
        int value;

        void increment() {
            value++;
        }
    }

    static String solution2() {
        Sol2Counter a = new Sol2Counter();
        Sol2Counter b = new Sol2Counter();
        a.increment();
        a.increment();
        a.increment();
        b.increment();
        return "a=" + a.value + ", b=" + b.value;
    }

    static Ex3Point solutionCopyOf(Ex3Point p) {
        Ex3Point copy = new Ex3Point();             // NOWY obiekt = nowy adres
        copy.x = p.x;                               // przepisujemy wartości, nie referencję
        copy.y = p.y;
        return copy;
    }

    static String solution3() {
        Ex3Point original = new Ex3Point();
        original.x = 1;
        original.y = 2;
        Ex3Point copy = solutionCopyOf(original);
        original.x = 10;
        return "kopia=(" + copy.x + "," + copy.y + "), oryginał=(" + original.x + "," + original.y
                + "), ten sam=" + (copy == original);
    }

    static class Sol4Item {
        String name;
        int price;
        int quantity;

        int value() {
            return price * quantity;
        }
    }

    static Sol4Item item(String name, int price, int quantity) {    // pomocnicza „fabryka” obiektów
        Sol4Item it = new Sol4Item();
        it.name = name;
        it.price = price;
        it.quantity = quantity;
        return it;
    }

    static String solution4() {
        Sol4Item[] items = {item("Kabel", 15, 2), item("Monitor", 999, 1), item("Mysz", 61, 1)};
        int sum = 0;
        Sol4Item best = items[0];
        for (Sol4Item it : items) {
            sum += it.value();
            if (it.value() > best.value()) {
                best = it;
            }
        }
        return "suma=" + sum + ", najcenniejsza=" + best.name;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Klasa to plan (typ): opisuje pola i metody. Obiekt to egzemplarz utworzony przez new, z własnymi
     *      wartościami pól. Analogia: przepis na ciasto (klasa) i konkretne ciasto na stole (obiekt).
     *   2. 9 — b = a kopiuje referencję; a i b to ten sam obiekt, więc zmiana przez b jest widoczna przez a.
     *   3. false — dwa new to dwa różne obiekty (różne adresy), nawet jeśli pola mają te same wartości.
     *   4. d jest null, więc d.name rzuca NullPointerException, zanim dojdziemy do new. Najpierw sprawdź
     *      samą referencję: if (d == null) { d = new Dog(); }.
     *   5. number = 0, active = false, text = null (pola dostają wartości domyślne).
     *   6. Nie. Metoda dostała kopię referencji; przypisanie d = new Dog() zmienia tylko tę kopię.
     *      Obiekt rex i zmienna rex u wywołującego zostają bez zmian.
     *   7. Zwykle nie trzeba — age znaczy this.age. this.age jest potrzebne, gdy nazwa pola jest
     *      zasłonięta przez parametr lub zmienną lokalną o tej samej nazwie, oraz dla czytelności przy
     *      porównaniach z innym obiektem (this.age kontra other.age).
     */
    // </editor-fold>
}
