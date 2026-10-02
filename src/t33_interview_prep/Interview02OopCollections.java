package t33_interview_prep;

import helpers.Check;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: OOP i kolekcje na rozmowie kwalifikacyjnej — 22 karty
 *        (collection = kolekcja; contract = kontrakt; bucket = koszyk; key = klucz)
 *
 * W SKRÓCIE:
 *   Dwa tematy, o które pyta się na rozmowie niemal zawsze: obiektowość (cztery filary, kompozycja, SOLID)
 *   oraz kolekcje (equals/hashCode, HashMap od środka, wybór kolekcji, iteratory, niezmienność, złożoność).
 *   Każda karta ma pytanie, odpowiedź wzorcową w 2–6 liniach i dowód w kodzie z linią WYNIK.
 *
 * ANALOGIA: HashMap to szafa z szufladami. Hash klucza mówi, w której szufladzie szukać (szybko), a equals — który
 *   przedmiot w szufladzie jest TYM właściwym. Jeśli po włożeniu przedmiotu zmienisz jego etykietę, szukasz go
 *   w złej szufladzie — i choć leży w szafie, "nie ma go" (karta 17).
 *
 * JAK TO DZIAŁA:
 *   Poziom karty:  ★ = rozgrzewka, ★★ = standard, ★★★ = pytanie z haczykiem.
 *   Odpowiadaj schematem: teza → dlaczego → przykład/pułapka. Przy kolekcjach zawsze podaj złożoność i jeden przypadek,
 *   w którym wybrałbyś inną strukturę.
 *
 * SŁÓWKA:
 *   encapsulation = hermetyzacja; inheritance = dziedziczenie; polymorphism = polimorfizm; abstraction = abstrakcja;
 *   composition = kompozycja (zawieranie); equals/hashCode = równość/skrót; bucket = koszyk; collision = kolizja;
 *   fail-fast = zawiodący szybko; view = widok; immutable = niezmienny; comparator = komparator (porównywacz).
 *
 * ZOBACZ TEŻ: t33_interview_prep/Interview01JavaQuestions (pytania o język), t12_collections/Collections11HashingInternals,
 *   t12_collections/Collections13Performance, t07_inheritance_polymorphism/Inherit08Solid
 * </pre>
 */
public class Interview02OopCollections {

    public static void main(String[] args) {
        title("Interview02 — OOP i kolekcje (22 karty)");

        fourPillars();          // four pillars = cztery filary (karty 1–4)
        compositionSolid();     // composition, SOLID = kompozycja i SOLID (karty 5–6)
        equalsHashCode();       // equals and hashCode = równość i skrót (karty 7–8)
        comparing();            // comparing = porównywanie (karty 9–10)
        chooseCollection();     // choose collection = wybór kolekcji (karty 11–13)
        hashMapInside();        // HashMap inside = HashMap od środka (karty 14–16)
        mutableKeyTreeMap();    // mutable key, TreeMap = zmienny klucz, TreeMap (karty 17–18)
        concurrencyIterators(); // concurrency, iterators = współbieżność, iteratory (karty 19–20)
        immutabilityBigO();     // immutability, Big-O = niezmienność i złożoność (karty 21–22)
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. CZTERY FILARY OOP
    // =================================================================================================

    /**
     * 1. Karty 1–4: hermetyzacja, dziedziczenie, polimorfizm i abstrakcja — każda z krótkim kodem.
     */
    static void fourPillars() {
        section("1. Cztery filary OOP");

        // ---- KARTA 1 ★ Hermetyzacja (encapsulation) — co to i po co?   (→ t06_oop_basics/Oop03Encapsulation)
        // ODPOWIEDŹ: ukrywamy stan (pola private) i wystawiamy tylko metody, które pilnują reguł (niezmienników).
        //   Dzięki temu obiekt nigdy nie wejdzie w niepoprawny stan z zewnątrz, a środek można zmienić bez psucia
        //   użytkowników klasy. To NIE to samo co "pola prywatne + gettery i settery dla wszystkiego" — setter bez walidacji
        //   nie hermetyzuje niczego.
        Account account = new Account();
        account.deposit(100);
        expectThrows("wpłata ujemna", () -> account.deposit(-5));
        // WYNIK: ✔ wpłata ujemna → rzucono IllegalArgumentException: kwota musi być dodatnia
        show("saldo po poprawnej wpłacie", account.balance());
        // WYNIK: saldo po poprawnej wpłacie → 100
        // DOBRA PRAKTYKA: zamiast setBalance(x) udostępnij operację z sensem biznesowym (deposit, withdraw) i waliduj w niej.

        // ---- KARTA 2 ★ Dziedziczenie (inheritance) — kiedy i jakie pułapki?   (→ t07_inheritance_polymorphism/Inherit01Basics)
        // ODPOWIEDŹ: podklasa przejmuje pola i metody nadklasy ("jest rodzajem": Car jest Vehicle). Konstruktor nadklasy
        //   zawsze wykonuje się PIERWSZY (najpierw super(), potem reszta). Dziedziczysz tylko z jednej klasy. Metody
        //   instancji są polimorficzne, ale POLA nie — pole w podklasie o tej samej nazwie tylko ZASŁANIA pole nadklasy,
        //   a odczyt zależy od typu zmiennej.
        Vehicle.log = new ArrayList<>();
        Vehicle pojazd = new Car();
        show("kolejność konstruktorów", Vehicle.log);
        // WYNIK: kolejność konstruktorów → [Vehicle(), Car()]
        show("metoda describe() — po typie obiektu", pojazd.describe());
        // WYNIK: metoda describe() — po typie obiektu → auto
        show("pole name przez zmienną Vehicle", pojazd.name);
        // WYNIK: pole name przez zmienną Vehicle → pojazd
        show("pole name po rzutowaniu na Car", ((Car) pojazd).name);
        // WYNIK: pole name po rzutowaniu na Car → auto
        // PUŁAPKA: ukrywanie pól działa "wbrew intuicji" — to jeden z powodów, by pola były private i dostępne przez metody.

        // ---- KARTA 3 ★ Polimorfizm — wyjaśnij na przykładzie.   (→ t07_inheritance_polymorphism/Inherit05Polymorphism)
        // ODPOWIEDŹ: ten sam kod woła tę samą metodę na różnych obiektach, a JVM wybiera wersję po RZECZYWISTYM typie
        //   obiektu w czasie działania (dynamic dispatch = dynamiczne wiązanie). Dzięki temu dodajesz nowy typ
        //   (np. Triangle) bez zmiany kodu, który operuje na interfejsie Shape.
        List<Shape> shapes = List.of(new Circle(1.0), new Rect(2.0, 3.0));
        List<String> opisy = new ArrayList<>();
        for (Shape s : shapes) {
            opisy.add(s.name() + "=" + String.format(Locale.ROOT, "%.2f", s.area()));
        }
        show("pola figur przez interfejs Shape", opisy);
        // WYNIK: pola figur przez interfejs Shape → [Circle=3.14, Rect=6.00]

        // ---- KARTA 4 ★ Abstrakcja (abstraction) — czym się różni od hermetyzacji?   (→ t07_inheritance_polymorphism/Inherit03AbstractClasses)
        // ODPOWIEDŹ: abstrakcja = pokaż CO obiekt robi (interfejs, klasa abstrakcyjna), ukryj JAK. Hermetyzacja = ukryj
        //   STAN i pilnuj niezmienników. Abstrakcja dotyczy projektu (kontrakt), hermetyzacja — implementacji (dostępu).
        //   Kod wołający zna tylko kontrakt, więc implementację można podmienić (e-mail na SMS, prawdziwą bazę na atrapę w teście).
        Notifier email = msg -> "EMAIL: " + msg;
        Notifier sms = msg -> "SMS: " + msg;
        show("ten sam kod, różne implementacje", send(email, "cześć") + " / " + send(sms, "cześć"));
        // WYNIK: ten sam kod, różne implementacje → EMAIL: cześć / SMS: cześć
    }

    // =================================================================================================
    // 2. KOMPOZYCJA KONTRA DZIEDZICZENIE I SOLID
    // =================================================================================================

    /**
     * 2. Karty 5–6: dlaczego "preferuj kompozycję" i jednozdaniowe wyjaśnienia zasad SOLID z jednym dowodem.
     */
    static void compositionSolid() {
        section("2. Kompozycja kontra dziedziczenie i SOLID");

        // ---- KARTA 5 ★★ Kompozycja czy dziedziczenie?   (→ t07_inheritance_polymorphism/Inherit06CompositionVsInheritance)
        // ODPOWIEDŹ: dziedziczenie wiąże podklasę z DETALAMI implementacji nadklasy (problem kruchej klasy bazowej),
        //   a kompozycja ("ma" zamiast "jest") używa tylko publicznego kontraktu i pozwala podmieniać części w czasie działania.
        //   Reguła: dziedzicz, gdy naprawdę zachodzi relacja "jest" i klasa była zaprojektowana do dziedziczenia; w pozostałych
        //   przypadkach składaj obiekty i deleguj wywołania.
        //   Klasyczny dowód: HashSet.addAll woła wewnętrznie add(), więc licznik w podklasie liczy elementy podwójnie.
        CountingSet dziedziczony = new CountingSet();
        dziedziczony.addAll(List.of("a", "b"));
        show("dziedziczenie: dodano 2 elementy, licznik", dziedziczony.count);
        // WYNIK: dziedziczenie: dodano 2 elementy, licznik → 4
        CountingWrapper skladany = new CountingWrapper();
        skladany.addAll(List.of("a", "b"));
        show("kompozycja: dodano 2 elementy, licznik", skladany.count);
        // WYNIK: kompozycja: dodano 2 elementy, licznik → 2
        // DOBRA PRAKTYKA: klasy, które nie są zaprojektowane do dziedziczenia, oznaczaj jako final (albo sealed) — chronisz się
        //   przed kruchymi podklasami.

        // ---- KARTA 6 ★★ Wyjaśnij SOLID jednym zdaniem na literę.   (→ t07_inheritance_polymorphism/Inherit08Solid, t27_clean_code_pitfalls/CleanCode02Solid)
        // ODPOWIEDŹ:
        //   S — Single Responsibility (jedna odpowiedzialność): klasa ma jeden powód do zmiany.
        //   O — Open/Closed (otwarte/zamknięte): rozszerzasz zachowanie nowym kodem (nowa klasa), nie edytujesz starego.
        //   L — Liskov Substitution (podstawialność): podklasa musi dać się użyć wszędzie, gdzie oczekiwana jest nadklasa,
        //       bez zaskoczeń — nie zawęża kontraktu i nie łamie oczekiwań.
        //   I — Interface Segregation (segregacja interfejsów): wiele małych interfejsów zamiast jednego "tłustego".
        //   D — Dependency Inversion (odwrócenie zależności): zależ od abstrakcji (interfejs), nie od konkretnej klasy;
        //       zależności dostajesz z zewnątrz (konstruktor) — to podstawa wstrzykiwania zależności w Springu.
        //   Dowód dla L: Square (kwadrat) "jest" Rectangle (prostokątem) w geometrii, ale w kodzie łamie oczekiwanie, że
        //   po setW(5) i setH(2) pole wynosi 10.
        show("pole Rectangle po setW(5), setH(2)", resizeAndArea(new Rectangle()));
        // WYNIK: pole Rectangle po setW(5), setH(2) → 10
        show("pole Square po setW(5), setH(2)", resizeAndArea(new Square()));
        // WYNIK: pole Square po setW(5), setH(2) → 4
        // PUŁAPKA: na pytanie "dlaczego" nie odpowiadaj samą rozwinięciem skrótu — podaj problem, który zasada rozwiązuje.
    }

    // =================================================================================================
    // 3. EQUALS I HASHCODE
    // =================================================================================================

    /**
     * 3. Karty 7–8: kontrakt equals/hashCode z zepsutym przykładem w HashSet oraz pułapka z przeciążeniem equals.
     */
    static void equalsHashCode() {
        section("3. equals i hashCode");

        // ---- KARTA 7 ★★ Kontrakt equals i hashCode — co się psuje, gdy nadpiszesz tylko equals?   (→ t12_collections/Collections11HashingInternals)
        // ODPOWIEDŹ: equals musi być: zwrotne (x.equals(x)), symetryczne, przechodnie, spójne w czasie i dla null dawać false.
        //   A hashCode: obiekty RÓWNE w sensie equals MUSZĄ mieć ten sam hashCode (odwrotnie nie — różne obiekty mogą
        //   mieć równy skrót: to kolizja). Kolekcje haszujące najpierw szukają koszyka po hashCode, dopiero potem wołają
        //   equals — więc obiekt z samym equals "gubi się" w HashSet / jako klucz HashMap.
        Set<BadEq> zepsuty = new HashSet<>();
        zepsuty.add(new BadEq("Ala"));
        show("tylko equals: contains(nowy równy)", zepsuty.contains(new BadEq("Ala")));
        // WYNIK: tylko equals: contains(nowy równy) → false
        show("tylko equals: a.equals(b)", new BadEq("Ala").equals(new BadEq("Ala")));
        // WYNIK: tylko equals: a.equals(b) → true
        Set<GoodEq> dobry = new HashSet<>();
        dobry.add(new GoodEq("Ala"));
        show("equals + hashCode: contains(nowy równy)", dobry.contains(new GoodEq("Ala")));
        // WYNIK: equals + hashCode: contains(nowy równy) → true
        dobry.add(new GoodEq("Ala"));
        show("rozmiar po dodaniu duplikatu", dobry.size());
        // WYNIK: rozmiar po dodaniu duplikatu → 1
        show("równy napis, różne obiekty — hashCode takie samo", "Ala".hashCode() == new String("Ala").hashCode());
        // WYNIK: równy napis, różne obiekty — hashCode takie samo → true
        // DOBRA PRAKTYKA: equals i hashCode opieraj na TYCH SAMYCH polach; użyj Objects.hash(...) i Objects.equals(...),
        //   a najlepiej record (generuje oba poprawnie). Pola zmienne w hashCode to ryzyko (karta 17).

        // ---- KARTA 8 ★★★ Pułapki w implementacji equals: przeciążenie zamiast nadpisania i getClass kontra instanceof.   (→ t06_oop_basics/Oop05ObjectMethods)
        // ODPOWIEDŹ: metoda  equals(Point p)  PRZECIĄŻA equals, a nie je nadpisuje — kolekcje wołają equals(Object), więc
        //   widzą wersję z Object (porównanie referencji). Adnotacja @Override wychwytuje taki błąd przy kompilacji.
        //   instanceof w equals pozwala podklasie być równą nadklasie (łatwo złamać symetrię); getClass() wymaga identycznej
        //   klasy (bezpieczniejsze, ale łamie podstawialność). Rekordy: równość po komponentach i dokładnym typie.
        Object jakoObject = new Overloaded(1, 2);
        show("equals(Overloaded) — przeciążona", new Overloaded(1, 2).equals(new Overloaded(1, 2)));
        // WYNIK: equals(Overloaded) — przeciążona → true
        show("equals(Object) — odziedziczona z Object", new Overloaded(1, 2).equals(jakoObject));
        // WYNIK: equals(Object) — odziedziczona z Object → false
        List<Overloaded> lista = List.of(new Overloaded(1, 2));
        show("List.contains woła equals(Object)", lista.contains(new Overloaded(1, 2)));
        // WYNIK: List.contains woła equals(Object) → false
        // PUŁAPKA: tak samo działa  equals(Point p)  w kodzie ze "zgadywaną" nazwą. Zawsze dopisuj @Override.
    }

    // =================================================================================================
    // 4. COMPARABLE I COMPARATOR
    // =================================================================================================

    /**
     * 4. Karty 9–10: porządek naturalny kontra komparator oraz pułapki (odejmowanie, spójność z equals).
     */
    static void comparing() {
        section("4. Comparable i Comparator");

        // ---- KARTA 9 ★★ Comparable kontra Comparator?   (→ t12_collections/Collections07ComparableComparator)
        // ODPOWIEDŹ: Comparable (metoda compareTo w SAMEJ klasie) definiuje jeden "porządek naturalny" — używa go
        //   Collections.sort, TreeSet, TreeMap. Comparator (osobny obiekt, metoda compare) to dowolna liczba zewnętrznych
        //   porządków, także dla klas, których nie możesz zmienić. Zwracana liczba: ujemna = pierwszy mniejszy, 0 = równe,
        //   dodatnia = pierwszy większy. Komparatory składa się: comparing(...).thenComparing(...).reversed().
        List<Version> versions = new ArrayList<>(List.of(new Version(1, 10), new Version(1, 2), new Version(0, 9)));
        Collections.sort(versions);
        show("porządek naturalny Version", versions);
        // WYNIK: porządek naturalny Version → [0.9, 1.2, 1.10]
        List<Person> ludzie = new ArrayList<>(List.of(new Person("Ola", 30), new Person("Adam", 25), new Person("Ewa", 30)));
        ludzie.sort(Comparator.comparingInt(Person::age).thenComparing(Person::name));
        show("wiek rosnąco, potem imię", names(ludzie));
        // WYNIK: wiek rosnąco, potem imię → [Adam, Ewa, Ola]
        ludzie.sort(Comparator.comparingInt(Person::age).reversed().thenComparing(Person::name));
        show("wiek malejąco, potem imię", names(ludzie));
        // WYNIK: wiek malejąco, potem imię → [Ewa, Ola, Adam]
        List<String> napisy = new ArrayList<>(List.of("b", "a", "Z", "ą"));
        Collections.sort(napisy);
        show("naturalny porządek String (Unicode)", napisy);
        // WYNIK: naturalny porządek String (Unicode) → [Z, a, b, ą]
        // PUŁAPKA: String.compareTo porównuje punkty kodowe Unicode: wielkie litery przed małymi, polskie znaki po "z".
        //   Do alfabetu polskiego użyj java.text.Collator z Locale pl-PL.

        // ---- KARTA 10 ★★★ Pułapki komparatorów: odejmowanie i spójność z equals.   (→ t12_collections/Collections07ComparableComparator)
        // ODPOWIEDŹ: (1) komparator  (a, b) -> a - b  jest błędny: odejmowanie może się przepełnić i zwrócić znak przeciwny
        //   — używaj Integer.compare(a, b). (2) compareTo powinno być spójne z equals; jeśli nie jest, TreeSet i HashSet
        //   zachowują się inaczej: TreeSet uważa elementy za równe, gdy compareTo == 0, nie wołając equals.
        //   BigDecimal jest tu podręcznikowym przykładem: 1.0 i 1.00 mają compareTo 0, ale equals false (inna skala).
        Comparator<Integer> zly = (a, b) -> a - b;
        show("zły komparator: compare(MIN_VALUE, 1) > 0", zly.compare(Integer.MIN_VALUE, 1) > 0);
        // WYNIK: zły komparator: compare(MIN_VALUE, 1) > 0 → true
        show("poprawnie: Integer.compare(MIN_VALUE, 1) < 0", Integer.compare(Integer.MIN_VALUE, 1) < 0);
        // WYNIK: poprawnie: Integer.compare(MIN_VALUE, 1) < 0 → true
        BigDecimal jeden = new BigDecimal("1.0");
        BigDecimal jedenDwaZera = new BigDecimal("1.00");
        Set<BigDecimal> hash = new HashSet<>(List.of(jeden, jedenDwaZera));
        Set<BigDecimal> tree = new TreeSet<>(List.of(jeden, jedenDwaZera));
        show("HashSet BigDecimal 1.0 i 1.00 — rozmiar", hash.size());
        // WYNIK: HashSet BigDecimal 1.0 i 1.00 — rozmiar → 2
        show("TreeSet BigDecimal 1.0 i 1.00 — rozmiar", tree.size());
        // WYNIK: TreeSet BigDecimal 1.0 i 1.00 — rozmiar → 1
        // DOBRA PRAKTYKA: BigDecimal porównuj compareTo, a do klucza mapy używaj stripTrailingZeros() albo TreeMap — zob.
        //   t15_numbers/Numbers01BigDecimal.
    }

    // =================================================================================================
    // 5. WYBÓR KOLEKCJI
    // =================================================================================================

    /**
     * 5. Karty 11–13: tabela wyboru List/Set/Map/Queue, kolejki i ArrayList kontra LinkedList.
     */
    static void chooseCollection() {
        section("5. Wybór kolekcji: List, Set, Map, Queue");

        // ---- KARTA 11 ★ Kiedy List, Set, Map, Queue?   (→ t12_collections/Collections01Overview)
        // ODPOWIEDŹ:
        //   List  — uporządkowana, z duplikatami, dostęp po indeksie (ArrayList domyślnie).
        //   Set   — bez duplikatów: HashSet (najszybszy, bez kolejności), LinkedHashSet (kolejność wstawiania),
        //           TreeSet (posortowany, O(log n)).
        //   Map   — klucz → wartość: HashMap, LinkedHashMap, TreeMap (analogicznie do zbiorów). Map NIE jest Collection.
        //   Queue — kolejka FIFO (ArrayDeque, PriorityQueue wg priorytetu); Deque — obie strony, także stos.
        List<String> lista = new ArrayList<>(List.of("b", "a", "b"));
        Set<String> hashSet = new HashSet<>(lista);
        Set<String> linked = new LinkedHashSet<>(List.of("b", "a", "b", "c"));
        Set<String> tree = new TreeSet<>(List.of("b", "a", "b", "c"));
        Map<String, Integer> mapa = new HashMap<>();
        show("List dopuszcza duplikaty", lista);
        // WYNIK: List dopuszcza duplikaty → [b, a, b]
        show("Set odrzuca duplikaty (rozmiar)", hashSet.size());
        // WYNIK: Set odrzuca duplikaty (rozmiar) → 2
        show("LinkedHashSet — kolejność wstawiania", linked);
        // WYNIK: LinkedHashSet — kolejność wstawiania → [b, a, c]
        show("TreeSet — kolejność rosnąca", tree);
        // WYNIK: TreeSet — kolejność rosnąca → [a, b, c]
        mapa.put("klucz", 1);
        show("put(klucz, 2) zwraca poprzednią wartość", mapa.put("klucz", 2));
        // WYNIK: put(klucz, 2) zwraca poprzednią wartość → 1
        show("put(klucz, 3) zwraca poprzednią wartość", mapa.put("klucz", 3));
        // WYNIK: put(klucz, 3) zwraca poprzednią wartość → 2
        // PUŁAPKA: nie zakładaj kolejności elementów HashSet/HashMap — jest nieokreślona i może się zmienić między wersjami Javy.

        // ---- KARTA 12 ★★ Queue, Deque, Stack i PriorityQueue?   (→ t12_collections/Collections06QueuesDeques)
        // ODPOWIEDŹ: Queue = FIFO (pierwszy wszedł, pierwszy wyszedł): offer dodaje na końcu, poll zdejmuje z początku
        //   (zwraca null dla pustej, w odróżnieniu od remove, które rzuca wyjątek). Deque (ArrayDeque) działa z obu stron
        //   i jest zalecanym STOSEM (push/pop = LIFO): klasa Stack jest przestarzała (dziedziczy z synchronizowanego Vector).
        //   PriorityQueue wydaje elementy wg priorytetu (kopiec binarny, O(log n)) — ale jej toString/iteracja NIE jest posortowana.
        Deque<Integer> stos = new ArrayDeque<>();
        stos.push(1);
        stos.push(2);
        stos.push(3);
        show("stos: pop zwraca ostatnio włożony", stos.pop());
        // WYNIK: stos: pop zwraca ostatnio włożony → 3
        Queue<Integer> kolejka = new ArrayDeque<>();
        kolejka.offer(1);
        kolejka.offer(2);
        show("kolejka: poll zwraca pierwszy włożony", kolejka.poll());
        // WYNIK: kolejka: poll zwraca pierwszy włożony → 1
        show("poll z pustej kolejki", new ArrayDeque<Integer>().poll());
        // WYNIK: poll z pustej kolejki → null
        PriorityQueue<Integer> kopiec = new PriorityQueue<>();
        for (int x : new int[]{5, 3, 4, 1, 2}) {
            kopiec.offer(x);
        }
        show("PriorityQueue.toString (kopiec, nie posortowane)", kopiec);
        // WYNIK: PriorityQueue.toString (kopiec, nie posortowane) → [1, 2, 4, 5, 3]
        List<Integer> wydane = new ArrayList<>();
        while (!kopiec.isEmpty()) {
            wydane.add(kopiec.poll());
        }
        show("kolejne poll() — rosnąco", wydane);
        // WYNIK: kolejne poll() — rosnąco → [1, 2, 3, 4, 5]
        thrown("ArrayDeque nie przyjmuje null", () -> new ArrayDeque<String>().add(null));
        // WYNIK: ArrayDeque nie przyjmuje null → rzucono NullPointerException

        // ---- KARTA 13 ★★ ArrayList kontra LinkedList — złożoność i wybór.   (→ t12_collections/Collections02Lists)
        // ODPOWIEDŹ: ArrayList to tablica dynamiczna: get(i) O(1), dodanie na końcu zamortyzowane O(1), wstawienie/usunięcie
        //   w środku lub na początku O(n) (przesunięcie elementów). LinkedList to lista dwukierunkowa: dodanie/usunięcie na końcach
        //   O(1), ale get(i) i wstawienie "w środku" O(n), bo trzeba dojść do węzła; każdy element to osobny obiekt z dwoma
        //   wskaźnikami (więcej pamięci, gorsza lokalność cache). W praktyce prawie zawsze ArrayList; na kolejkę — ArrayDeque.
        //   Pułapka z interfejsem List<Integer>: remove(int index) kontra remove(Object o).
        List<Integer> liczby = new ArrayList<>(List.of(10, 20, 1));
        liczby.remove(1);
        show("remove(1) usuwa element o INDEKSIE 1", liczby);
        // WYNIK: remove(1) usuwa element o INDEKSIE 1 → [10, 1]
        liczby.remove(Integer.valueOf(1));
        show("remove(Integer.valueOf(1)) usuwa WARTOŚĆ 1", liczby);
        // WYNIK: remove(Integer.valueOf(1)) usuwa WARTOŚĆ 1 → [10]
        // DOBRA PRAKTYKA: zadeklaruj zmienną jako List<...> (interfejs), a konkretną klasę wybierz w jednym miejscu — łatwo ją zmienić.
    }

    // =================================================================================================
    // 6. HASHMAP OD ŚRODKA
    // =================================================================================================

    /**
     * 6. Karty 14–16: koszyki i resize, kolizje, null jako klucz oraz dlaczego String i Integer to dobre klucze.
     */
    static void hashMapInside() {
        section("6. HashMap od środka");

        // ---- KARTA 14 ★★★ Jak działa HashMap (koszyki, resize, drzewa)?   (→ t12_collections/Collections11HashingInternals)
        // ODPOWIEDŹ: tablica koszyków (domyślnie 16). Wstawianie: hash = h ^ (h >>> 16) (wymieszanie bitów), indeks =
        //   (n - 1) & hash. W koszyku jest lista wpisów; po zderzeniu wołane jest equals. Gdy liczba wpisów przekroczy
        //   próg = pojemność × współczynnik 0,75 (czyli 12 dla 16), tablica podwaja się (resize) i wpisy trafiają do koszyka
        //   o tym samym indeksie albo przesuniętego o starą pojemność. Gdy do koszyka, który ma już 8 wpisów
        //   (próg TREEIFY_THRESHOLD = 8), trafia kolejny, a tablica ma >= 64 koszyki (inaczej następuje tylko resize), lista
        //   zamienia się w drzewo czerwono-czarne (pesymistycznie O(log n) zamiast O(n)); po spadku do 6 wpisów wraca do listy. Od Javy 8 nowe wpisy są dopisywane na końcu listy.
        //   Dowód: replikujemy algorytm wyliczenia indeksu (szczegół implementacji OpenJDK, stabilny od Javy 8).
        List<String> indeksy = new ArrayList<>();
        for (String klucz : List.of("Ala", "Ola", "Ewa", "Jan")) {
            indeksy.add(klucz + ":" + bucketIndex(klucz, 16) + "->" + bucketIndex(klucz, 32));
        }
        show("koszyk przy pojemności 16 -> 32", indeksy);
        // WYNIK: koszyk przy pojemności 16 -> 32 → [Ala:7->23, Ola:5->5, Ewa:14->14, Jan:6->22]
        // Widać regułę: po podwojeniu tablicy indeks jest taki sam albo większy o 16 (jeden dodatkowy bit hasha decyduje).
        // PUŁAPKA: konstruktor new HashMap<>(100) zaokrągla pojemność do potęgi dwójki (128); gdy znasz liczbę elementów n,
        //   podaj pojemność n / 0.75 + 1, żeby uniknąć kolejnych resize.

        // ---- KARTA 15 ★★ Kolizje, klucz null i inne mapy.   (→ t12_collections/Collections05Maps)
        // ODPOWIEDŹ: kolizja = różne klucze w tym samym koszyku (czasem nawet ten sam hashCode). Mapa je rozróżnia przez equals.
        //   HashMap dopuszcza JEDEN klucz null (trafia do koszyka 0) i wartości null. TreeMap (porównuje kluczami),
        //   Hashtable, ConcurrentHashMap i Map.of — null jako klucz zgłaszają NullPointerException.
        show("\"Aa\".hashCode() == \"BB\".hashCode()", "Aa".hashCode() == "BB".hashCode());
        // WYNIK: "Aa".hashCode() == "BB".hashCode() → true
        show("\"Aa\".equals(\"BB\")", "Aa".equals("BB"));
        // WYNIK: "Aa".equals("BB") → false
        Map<String, String> kolizje = new HashMap<>();
        kolizje.put("Aa", "pierwszy");
        kolizje.put("BB", "drugi");
        show("dwa klucze o tym samym hashu: rozmiar, get(Aa), get(BB)",
                kolizje.size() + ", " + kolizje.get("Aa") + ", " + kolizje.get("BB"));
        // WYNIK: dwa klucze o tym samym hashu: rozmiar, get(Aa), get(BB) → 2, pierwszy, drugi
        Map<String, String> zNullem = new HashMap<>();
        zNullem.put(null, "wartość dla null");
        show("HashMap.get(null)", zNullem.get(null));
        // WYNIK: HashMap.get(null) → wartość dla null
        thrown("TreeMap.put(null, ...)", () -> new TreeMap<String, String>().put(null, "x"));
        // WYNIK: TreeMap.put(null, ...) → rzucono NullPointerException
        thrown("ConcurrentHashMap.put(null, ...)", () -> new ConcurrentHashMap<String, String>().put(null, "x"));
        // WYNIK: ConcurrentHashMap.put(null, ...) → rzucono NullPointerException
        thrown("Hashtable.put(null, ...)", () -> new Hashtable<String, String>().put(null, "x"));
        // WYNIK: Hashtable.put(null, ...) → rzucono NullPointerException
        thrown("Map.of(null, ...)", () -> Map.of(null, "x"));
        // WYNIK: Map.of(null, ...) → rzucono NullPointerException
        // DOBRA PRAKTYKA: przy ConcurrentHashMap null jest zabroniony celowo — get zwracający null mógłby znaczyć "brak"
        //   albo "wartość null" i nie dałoby się tego rozstrzygnąć bez blokady. Dlatego null w mapach unikaj.

        // ---- KARTA 16 ★★ Dlaczego String i Integer są dobrymi kluczami?   (→ t12_collections/Collections05Maps)
        // ODPOWIEDŹ: są NIEZMIENNE (hash nie zmieni się po włożeniu), mają poprawnie nadpisane equals i hashCode,
        //   a hashCode jest tani (String zapamiętuje obliczony skrót). Wzory hashCode są częścią specyfikacji:
        //   Integer → sama wartość, String → s[0]*31^(n-1) + ... + s[n-1], List → analogicznie (31*h + hash elementu).
        show("Integer.valueOf(42).hashCode()", Integer.valueOf(42).hashCode());
        // WYNIK: Integer.valueOf(42).hashCode() → 42
        show("\"\".hashCode() i \"Aa\".hashCode()", "".hashCode() + " i " + "Aa".hashCode());
        // WYNIK: "".hashCode() i "Aa".hashCode() → 0 i 2112
        show("List.of(1, 2).hashCode()", List.of(1, 2).hashCode());
        // WYNIK: List.of(1, 2).hashCode() → 994
        show("Boolean.TRUE.hashCode()", Boolean.TRUE.hashCode());
        // WYNIK: Boolean.TRUE.hashCode() → 1231
        show("Long.valueOf(1L << 32).hashCode()", Long.valueOf(1L << 32).hashCode());
        // WYNIK: Long.valueOf(1L << 32).hashCode() → 1
        // PUŁAPKA: hash Longa to (int) (v ^ (v >>> 32)) — górna połowa liczby jest "składana" z dolną. Dlatego Long 2^32
        //   ma ten sam skrót (1) co Long 1: to inne klucze, ale w mapie trafią do tego samego koszyka (kolizja).
    }

    // =================================================================================================
    // 7. ZMIENNY KLUCZ I TREEMAP
    // =================================================================================================

    /**
     * 7. Karty 17–18: obiekt zmieniony po włożeniu do HashSet "znika" oraz mapa posortowana TreeMap.
     */
    static void mutableKeyTreeMap() {
        section("7. Zmienny klucz i TreeMap");

        // ---- KARTA 17 ★★★ Co się stanie, gdy zmienisz klucz po włożeniu do HashMap/HashSet?   (→ t12_collections/Collections04Sets)
        // ODPOWIEDŹ: obiekt leży w koszyku wyliczonym ze STAREGO hashCode. Po zmianie pola, na którym oparty jest hashCode,
        //   wyszukiwanie liczy już NOWY skrót i zagląda do innego koszyka — contains/get/remove nie znajdują elementu, choć
        //   wciąż jest w kolekcji (rośnie rozmiar, iteracja go widzi). To cichy wyciek pamięci i błąd logiczny.
        MutableKey klucz = new MutableKey("a");
        Set<MutableKey> zbior = new HashSet<>();
        zbior.add(klucz);
        show("przed zmianą: contains", zbior.contains(klucz));
        // WYNIK: przed zmianą: contains → true
        klucz.name = "b";
        show("po zmianie pola: contains", zbior.contains(klucz));
        // WYNIK: po zmianie pola: contains → false
        show("po zmianie pola: rozmiar", zbior.size());
        // WYNIK: po zmianie pola: rozmiar → 1
        show("po zmianie pola: iteracja go widzi", zbior.stream().anyMatch(k -> k == klucz));
        // WYNIK: po zmianie pola: iteracja go widzi → true
        klucz.name = "a";
        show("po cofnięciu zmiany: contains", zbior.contains(klucz));
        // WYNIK: po cofnięciu zmiany: contains → true
        // DOBRA PRAKTYKA: klucze mapy i elementy zbioru mają być niezmienne (String, Integer, rekord z niezmiennymi polami, enum).

        // ---- KARTA 18 ★★ Jak działa TreeMap i czym różni się od HashMap?   (→ t12_collections/Collections05Maps)
        // ODPOWIEDŹ: drzewo czerwono-czarne: klucze zawsze POSORTOWANE (porządek naturalny lub Comparator), operacje O(log n),
        //   dodatkowe metody nawigacji (firstKey, floorKey, ceilingKey, headMap, tailMap). Równość kluczy ustala compareTo/compare
        //   (nie equals!) — dwa klucze dla których komparator zwraca 0 to jeden klucz. Brak kluczy null. HashMap jest szybszy
        //   (O(1)) i nie gwarantuje kolejności.
        Map<String, Integer> bezWielkosci = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        bezWielkosci.put("a", 1);
        bezWielkosci.put("A", 2);
        show("TreeMap(CASE_INSENSITIVE_ORDER): a i A to jeden klucz", bezWielkosci);
        // WYNIK: TreeMap(CASE_INSENSITIVE_ORDER): a i A to jeden klucz → {a=2}
        TreeMap<Integer, String> progi = new TreeMap<>();
        progi.put(10, "dziesięć");
        progi.put(20, "dwadzieścia");
        progi.put(30, "trzydzieści");
        show("firstKey / floorKey(25) / ceilingKey(25)", progi.firstKey() + " / " + progi.floorKey(25) + " / " + progi.ceilingKey(25));
        // WYNIK: firstKey / floorKey(25) / ceilingKey(25) → 10 / 20 / 30
        show("headMap(20) — klucze mniejsze niż 20", progi.headMap(20));
        // WYNIK: headMap(20) — klucze mniejsze niż 20 → {10=dziesięć}
        show("tailMap(20) — klucze od 20 wzwyż", progi.tailMap(20));
        // WYNIK: tailMap(20) — klucze od 20 wzwyż → {20=dwadzieścia, 30=trzydzieści}
        // DOBRA PRAKTYKA: floorKey/ceilingKey to elegancki sposób na "progi" (np. rabat zależny od kwoty) zamiast łańcucha if.
    }

    // =================================================================================================
    // 8. WSPÓŁBIEŻNOŚĆ I ITERATORY
    // =================================================================================================

    /**
     * 8. Karty 19–20: ConcurrentHashMap kontra synchronizedMap oraz iteratory fail-fast.
     */
    static void concurrencyIterators() {
        section("8. ConcurrentHashMap, synchronizedMap, iteratory fail-fast");

        // ---- KARTA 19 ★★ ConcurrentHashMap kontra synchronizedMap i Hashtable?   (→ t21_concurrency/Concurrency06ConcurrentCollections)
        // ODPOWIEDŹ: Hashtable i Collections.synchronizedMap blokują CAŁĄ mapę przy każdej operacji (jeden zamek) — bezpieczne,
        //   ale wąskie gardło. Iterację trzeba dodatkowo objąć synchronized(mapa) ręcznie. ConcurrentHashMap (od Javy 8:
        //   CAS + blokada pojedynczego koszyka) pozwala wielu wątkom pracować równolegle, jej iteratory są
        //   "słabo spójne" (nie rzucają ConcurrentModificationException), a operacje złożone są ATOMOWE:
        //   merge, compute, computeIfAbsent, putIfAbsent. Sam ciąg  if (!containsKey) put  NIE jest atomowy w żadnej z map.
        //   Zakazuje null (karta 15). size() w czasie pracy wielu wątków jest tylko przybliżone.
        ConcurrentHashMap<String, Integer> licznik = new ConcurrentHashMap<>();
        runInTwoThreads(() -> {
            for (int i = 0; i < 10_000; i++) {
                licznik.merge("klucz", 1, Integer::sum);
            }
        });
        show("dwa wątki po 10000 merge — wynik atomowy", licznik.get("klucz"));
        // WYNIK: dwa wątki po 10000 merge — wynik atomowy → 20000
        ConcurrentHashMap<String, String> zmieniana = new ConcurrentHashMap<>(Map.of("a", "1", "b", "2", "c", "3"));
        for (String k : zmieniana.keySet()) {
            if (k.equals("a")) {
                zmieniana.remove("b");
            }
        }
        show("modyfikacja podczas iteracji CHM — bez wyjątku, rozmiar", zmieniana.size());
        // WYNIK: modyfikacja podczas iteracji CHM — bez wyjątku, rozmiar → 2
        // PUŁAPKA: ConcurrentHashMap nie "wyłącza" potrzeby myślenia: dwa osobne wywołania (get, a potem put) nadal mogą się
        //   przeplatać z innym wątkiem. Rób to jedną operacją atomową (merge/compute).

        // ---- KARTA 20 ★★★ Co to jest fail-fast iterator i ConcurrentModificationException?   (→ t12_collections/Collections03IterationModification)
        // ODPOWIEDŹ: iteratory ArrayList, HashMap itd. porównują licznik modyfikacji (modCount) z wartością z chwili utworzenia.
        //   Strukturalna zmiana kolekcji (add/remove) z pominięciem iteratora kończy się ConcurrentModificationException
        //   (to ochrona "best effort", nie gwarancja wielowątkowa). Poprawnie: Iterator.remove(), removeIf (Java 8+),
        //   kopia, albo zbudowanie nowej listy strumieniem. Haczyk: usunięcie PRZEDOSTATNIEGO elementu nie rzuca wyjątku,
        //   bo hasNext() zwraca false zanim modCount zostanie sprawdzony.
        List<String> litery = new ArrayList<>(List.of("a", "b", "c"));
        thrown("usuwanie w pętli for-each (element a)", () -> {
            for (String s : litery) {
                if (s.equals("a")) {
                    litery.remove(s);
                }
            }
        });
        // WYNIK: usuwanie w pętli for-each (element a) → rzucono ConcurrentModificationException
        List<String> przedostatni = new ArrayList<>(List.of("a", "b", "c"));
        for (String s : przedostatni) {
            if (s.equals("b")) {
                przedostatni.remove(s);
            }
        }
        show("usunięcie przedostatniego w for-each — bez wyjątku, lista", przedostatni);
        // WYNIK: usunięcie przedostatniego w for-each — bez wyjątku, lista → [a, c]
        List<String> poprawnie = new ArrayList<>(List.of("a", "b", "c", "b"));
        Iterator<String> it = poprawnie.iterator();
        while (it.hasNext()) {
            if (it.next().equals("b")) {
                it.remove();
            }
        }
        show("Iterator.remove()", poprawnie);
        // WYNIK: Iterator.remove() → [a, c]
        List<String> removeIf = new ArrayList<>(List.of("a", "b", "c", "b"));
        removeIf.removeIf(s -> s.equals("b"));
        show("removeIf (Java 8+)", removeIf);
        // WYNIK: removeIf (Java 8+) → [a, c]
        // PUŁAPKA: brak wyjątku nie znaczy, że kod jest poprawny — przypadek "przedostatniego" to przypadkowa luka w sprawdzaniu.
    }

    // =================================================================================================
    // 9. NIEZMIENNOŚĆ I ZŁOŻONOŚĆ
    // =================================================================================================

    /**
     * 9. Karty 21–22: List.of kontra unmodifiableList kontra Arrays.asList oraz tabela złożoności z dowodem liczenia kroków.
     */
    static void immutabilityBigO() {
        section("9. Niezmienność kolekcji i złożoność");

        // ---- KARTA 21 ★★ List.of, Collections.unmodifiableList, Arrays.asList i List.copyOf — różnice?   (→ t12_collections/Collections08ImmutableUnmodifiable)
        // ODPOWIEDŹ: List.of (Java 9) i List.copyOf (Java 10) tworzą PRAWDZIWIE niezmienną kolekcję: osobne dane, brak nulli
        //   (nawet contains(null) rzuca NPE). Collections.unmodifiableList to tylko WIDOK: nie przepuszcza zmian przez ten
        //   widok, ale zmiany oryginału są widoczne. Arrays.asList ma stały rozmiar i jest "oknem" na tablicę: set działa i
        //   zmienia tablicę, add/remove rzucają wyjątek. Stream.toList() (Java 16) jest niemodyfikowalna, ale dopuszcza null.
        List<String> baza = new ArrayList<>(List.of("a", "b"));
        List<String> widok = Collections.unmodifiableList(baza);
        List<String> kopia = List.copyOf(baza);
        baza.add("c");
        show("widok (unmodifiableList) po zmianie bazy", widok);
        // WYNIK: widok (unmodifiableList) po zmianie bazy → [a, b, c]
        show("kopia (List.copyOf) po zmianie bazy", kopia);
        // WYNIK: kopia (List.copyOf) po zmianie bazy → [a, b]
        thrown("widok.add(...)", () -> widok.add("x"));
        // WYNIK: widok.add(...) → rzucono UnsupportedOperationException
        thrown("List.of(\"a\", null)", () -> List.of("a", null));
        // WYNIK: List.of("a", null) → rzucono NullPointerException
        thrown("List.of(\"a\").contains(null)", () -> List.of("a").contains(null));
        // WYNIK: List.of("a").contains(null) → rzucono NullPointerException
        String[] tablica = {"x", "y"};
        List<String> okno = Arrays.asList(tablica);
        okno.set(0, "ZMIENIONE");
        show("Arrays.asList.set zmienia tablicę", tablica[0]);
        // WYNIK: Arrays.asList.set zmienia tablicę → ZMIENIONE
        thrown("Arrays.asList(...).add", () -> okno.add("z"));
        // WYNIK: Arrays.asList(...).add → rzucono UnsupportedOperationException
        List<String> zNullem = Stream.of("a", null).toList();
        show("Stream.toList() dopuszcza null", zNullem);
        // WYNIK: Stream.toList() dopuszcza null → [a, null]
        // DOBRA PRAKTYKA: z metod publicznych zwracaj List.copyOf(...) albo unmodifiableList (świadomie jako widok) — nigdy własną
        //   zmienną listę, bo wołający zepsuje stan obiektu.

        // ---- KARTA 22 ★★ Tabela złożoności (Big-O) podstawowych kolekcji.   (→ t24_algorithms/Algorithms01Complexity, t12_collections/Collections13Performance)
        // ODPOWIEDŹ:
        //   struktura        | get/contains        | dodanie                    | usunięcie
        //   ArrayList        | get O(1), contains O(n) | koniec O(1)*, środek O(n)  | O(n)
        //   LinkedList       | O(n)                | końce O(1), środek O(n)    | końce O(1)
        //   HashSet/HashMap  | O(1) średnio        | O(1) średnio*              | O(1) średnio
        //   TreeSet/TreeMap  | O(log n)            | O(log n)                   | O(log n)
        //   ArrayDeque       | brak dostępu        | końce O(1)*                | końce O(1)
        //   PriorityQueue    | peek O(1)           | offer O(log n)             | poll O(log n)
        //   (* zamortyzowane — czasem resize; HashMap w najgorszym razie O(log n) dzięki drzewom, przed Javą 8 O(n)).
        //   O(log n) widać po liczbie kroków wyszukiwania binarnego: dla 1024 elementów to najwyżej 11 porównań, a przeglądanie
        //   liniowe potrzebuje do 1024.
        int[] posortowane = new int[1024];
        for (int i = 0; i < posortowane.length; i++) {
            posortowane[i] = i;
        }
        show("kroki wyszukiwania liniowego (szukam ostatniego)", linearSteps(posortowane, 1023));
        // WYNIK: kroki wyszukiwania liniowego (szukam ostatniego) → 1024
        show("kroki wyszukiwania binarnego (szukam ostatniego)", binarySteps(posortowane, 1023));
        // WYNIK: kroki wyszukiwania binarnego (szukam ostatniego) → 11
        // DOBRA PRAKTYKA: na rozmowie nie recytuj tabeli — powiedz, KTÓRA operacja jest kosztowna i dlaczego (przesuwanie elementów,
        //   dojście do węzła, resize), i zaproponuj zamianę struktury.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Hermetyzacja = ukryj stan i pilnuj niezmienników; abstrakcja = pokaż CO, ukryj JAK.
     *   • Pola nie są polimorficzne (ukrywanie pól); konstruktor nadklasy wykonuje się pierwszy.
     *   • Preferuj kompozycję: dziedziczenie wiąże z detalami nadklasy (HashSet.addAll woła add).
     *   • SOLID: S jedna odpowiedzialność, O rozszerzaj bez edycji, L podklasa podstawialna, I małe interfejsy, D zależ od abstrakcji.
     *   • Kontrakt: równe obiekty muszą mieć równy hashCode; nadpisuj oba na tych samych polach; @Override zawsze.
     *   • Komparator: Integer.compare zamiast odejmowania; TreeSet równość = compareTo == 0 (BigDecimal 1.0 i 1.00).
     *   • List = indeks i duplikaty, Set = unikalność, Map = klucz → wartość, Queue/Deque = FIFO i stos (ArrayDeque).
     *   • HashMap: koszyk = (n-1) & (h ^ h>>>16), resize przy 0.75, drzewo, gdy do koszyka z 8 wpisami dojdzie kolejny (i >= 64 koszyków).
     *   • Null jako klucz: tylko HashMap; TreeMap, Hashtable, ConcurrentHashMap, Map.of rzucają NPE.
     *   • Klucze mają być niezmienne — zmiana pola w hashCode "gubi" element.
     *   • ConcurrentHashMap: operacje atomowe (merge, compute), słabo spójne iteratory; synchronizedMap blokuje całość.
     *   • Fail-fast: usuwaj przez Iterator.remove lub removeIf; brak wyjątku (przedostatni) nie dowodzi poprawności.
     *   • List.of / copyOf = niezmienne; unmodifiableList = widok; Arrays.asList = okno na tablicę.
     *   • Big-O: ArrayList get O(1); HashMap O(1); TreeMap O(log n); LinkedList get O(n).
     *
     * PYTANIA KONTROLNE:
     *   1. Co wypisze:  Set<BadEq> s = new HashSet<>(); s.add(new BadEq("Ala")); s.contains(new BadEq("Ala"))
     *      — gdy BadEq nadpisuje tylko equals (po polu name)?
     *   2. ZNAJDŹ BŁĄD:  for (String s : lista) { if (s.isEmpty()) lista.remove(s); }  — co się może stać i jak to naprawić?
     *   3. Co wypisze:  List<Integer> l = new ArrayList<>(List.of(10, 20, 1)); l.remove(1); System.out.println(l);  ?
     *   4. Dlaczego zmiana pola wchodzącego do hashCode po włożeniu obiektu do HashSet jest groźna?
     *   5. Co wypisze:  rozmiar HashSet i TreeSet zawierających BigDecimal("1.0") i BigDecimal("1.00")?
     *   6. Kiedy i po co HashMap zamienia listę w koszyku na drzewo?
     *   7. Czym różni się ConcurrentHashMap od Collections.synchronizedMap?
     *   8. Czym różni się List.of od Collections.unmodifiableList?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        checkAll(false);
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        checkAll(true);
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    private static void checkAll(boolean sol) {
        Check.equal("ćw. 1: hasDuplicates", List.of(false, true, true),
                () -> sol
                        ? List.of(solution1(List.of(1, 2, 3)), solution1(List.of(1, 2, 1)), solution1(List.of("a", "a")))
                        : List.of(exercise1(List.of(1, 2, 3)), exercise1(List.of(1, 2, 1)), exercise1(List.of("a", "a"))));
        Check.equal("ćw. 2: removeEvens", List.of(1, 3, 5),
                () -> sol ? solution2(List.of(1, 2, 3, 4, 5, 6)) : exercise2(List.of(1, 2, 3, 4, 5, 6)));
        List<Person> ludzie = List.of(new Person("Ola", 30), new Person("Adam", 25), new Person("Ewa", 30), new Person("Bartek", 30));
        Check.equal("ćw. 3: sortedNames (wiek malejąco, potem imię)", List.of("Bartek", "Ewa", "Ola", "Adam"),
                () -> sol ? solution3(ludzie) : exercise3(ludzie));
        List<String> slowa = List.of("kot", "pies", "kot", "ala", "kot", "pies");
        Check.equal("ćw. 4: countWords", "{ala=1, kot=3, pies=2}",
                () -> (sol ? solution4(slowa) : exercise4(slowa)).toString());
        Check.equal("ćw. 5: lruCache", "[a, c]",
                () -> {
                    Map<String, Integer> cache = sol ? solution5(2) : exercise5(2);
                    cache.put("a", 1);
                    cache.put("b", 2);
                    cache.get("a");
                    cache.put("c", 3);
                    return cache.keySet().toString();
                });
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć true, gdy lista zawiera duplikaty (dwa równe elementy).
     * Podpowiedź: dodajesz do HashSet — metoda add zwraca false, gdy element już był. Złożoność O(n).
     */
    static <T> boolean exercise1(List<T> list) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ błędną pętlę na wersję poprawną i zwróć NOWĄ listę bez liczb parzystych
     * (wejście jest niezmienne, więc najpierw zrób zmienną kopię).
     * <pre>{@code
     * for (Integer n : numbers) {
     *     if (n % 2 == 0) {
     *         numbers.remove(n);   // ConcurrentModificationException (lub UnsupportedOperationException dla List.of)
     *     }
     * }
     * }</pre>
     * Podpowiedź: new ArrayList<>(in) i removeIf.
     */
    static List<Integer> exercise2(List<Integer> in) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć imiona posortowane wg wieku MALEJĄCO, a przy tym samym wieku alfabetycznie rosnąco.
     * Podpowiedź: Comparator.comparingInt(Person::age).reversed().thenComparing(Person::name).
     */
    static List<String> exercise3(List<Person> people) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (średnie): policz wystąpienia słów w mapie posortowanej alfabetycznie (TreeMap).
     * Podpowiedź: map.merge(słowo, 1, Integer::sum) — jedna atomowa operacja zamiast containsKey + put.
     */
    static Map<String, Integer> exercise4(List<String> words) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): zbuduj pamięć podręczną LRU (Least Recently Used = najdawniej używany) o zadanej
     * pojemności: po przekroczeniu pojemności mapa usuwa element, po który sięgano najdawniej.
     * Podpowiedź: klasa Lru (pod spodem) dziedziczy po LinkedHashMap; konstruktor LinkedHashMap(16, 0.75f, true) włącza
     * kolejność według dostępu, a metoda removeEldestEntry decyduje o usunięciu najstarszego wpisu.
     * Uwaga: dziedziczenie jest tu uzasadnione — LinkedHashMap zaprojektowano do tego (karta 5).
     */
    static Map<String, Integer> exercise5(int capacity) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static <T> boolean solution1(List<T> list) {
        Set<T> seen = new HashSet<>();
        for (T element : list) {
            if (!seen.add(element)) {
                return true;
            }
        }
        return false;
    }

    static List<Integer> solution2(List<Integer> in) {
        List<Integer> copy = new ArrayList<>(in);
        copy.removeIf(n -> n % 2 == 0);
        return copy;
    }

    static List<String> solution3(List<Person> people) {
        return people.stream()
                .sorted(Comparator.comparingInt(Person::age).reversed().thenComparing(Person::name))
                .map(Person::name)
                .toList();
    }

    static Map<String, Integer> solution4(List<String> words) {
        Map<String, Integer> counts = new TreeMap<>();
        for (String word : words) {
            counts.merge(word, 1, Integer::sum);
        }
        return counts;
    }

    static Map<String, Integer> solution5(int capacity) {
        return new Lru<>(capacity);
    }

    // </editor-fold>

    // =================================================================================================
    // TYPY I METODY POMOCNICZE (dla kart)
    // =================================================================================================

    /** Akcja, która może rzucić wyjątek (pomocnik: wypisuje tylko NAZWĘ wyjątku, bez komunikatu zależnego od wersji). */
    @FunctionalInterface
    interface Action {
        void run() throws Exception;
    }

    static void thrown(String label, Action action) {
        try {
            action.run();
            show(label, "NIE rzucono wyjątku");
        } catch (Throwable t) {
            show(label, "rzucono " + t.getClass().getSimpleName());
        }
    }

    static void runInTwoThreads(Runnable task) {
        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        t1.start();
        t2.start();
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Replika wyliczania indeksu koszyka w HashMap (OpenJDK 8–21): {@code (n - 1) & (h ^ (h >>> 16))}. */
    static int bucketIndex(Object key, int capacity) {
        int h = key.hashCode();
        int spread = h ^ (h >>> 16);
        return (capacity - 1) & spread;
    }

    static int linearSteps(int[] sorted, int target) {
        int steps = 0;
        for (int value : sorted) {
            steps++;
            if (value == target) {
                break;
            }
        }
        return steps;
    }

    static int binarySteps(int[] sorted, int target) {
        int low = 0;
        int high = sorted.length - 1;
        int steps = 0;
        while (low <= high) {
            steps++;
            int mid = (low + high) >>> 1;
            if (sorted[mid] == target) {
                break;
            } else if (sorted[mid] < target) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return steps;
    }

    static List<String> names(List<Person> people) {
        List<String> result = new ArrayList<>();
        for (Person p : people) {
            result.add(p.name());
        }
        return result;
    }

    static int resizeAndArea(Rectangle r) {
        r.setW(5);
        r.setH(2);
        return r.area();
    }

    static String send(Notifier notifier, String message) {
        return notifier.send(message);
    }

    static class Account {
        private int balance;

        void deposit(int amount) {
            if (amount <= 0) {
                throw new IllegalArgumentException("kwota musi być dodatnia");
            }
            balance += amount;
        }

        int balance() {
            return balance;
        }
    }

    static class Vehicle {
        static List<String> log = new ArrayList<>();
        String name = "pojazd";

        Vehicle() {
            log.add("Vehicle()");
        }

        String describe() {
            return "pojazd";
        }
    }

    static class Car extends Vehicle {
        String name = "auto";

        Car() {
            super();
            log.add("Car()");
        }

        @Override
        String describe() {
            return "auto";
        }
    }

    interface Shape {
        double area();

        String name();
    }

    record Circle(double r) implements Shape {
        @Override
        public double area() {
            return Math.PI * r * r;
        }

        @Override
        public String name() {
            return "Circle";
        }
    }

    record Rect(double w, double h) implements Shape {
        @Override
        public double area() {
            return w * h;
        }

        @Override
        public String name() {
            return "Rect";
        }
    }

    @FunctionalInterface
    interface Notifier {
        String send(String message);
    }

    static class CountingSet extends HashSet<String> {
        private static final long serialVersionUID = 1L;
        int count;

        @Override
        public boolean add(String s) {
            count++;
            return super.add(s);
        }

        @Override
        public boolean addAll(Collection<? extends String> c) {
            count += c.size();
            return super.addAll(c);
        }
    }

    static class CountingWrapper {
        private final Set<String> inner = new HashSet<>();
        int count;

        boolean add(String s) {
            count++;
            return inner.add(s);
        }

        boolean addAll(List<String> items) {
            boolean changed = false;
            for (String s : items) {
                changed |= add(s);
            }
            return changed;
        }
    }

    static class Rectangle {
        protected int w;
        protected int h;

        void setW(int w) {
            this.w = w;
        }

        void setH(int h) {
            this.h = h;
        }

        int area() {
            return w * h;
        }
    }

    static class Square extends Rectangle {
        @Override
        void setW(int w) {
            this.w = w;
            this.h = w;
        }

        @Override
        void setH(int h) {
            this.h = h;
            this.w = h;
        }
    }

    /** Klasa z samym equals — celowo zepsuta (nie ma hashCode). */
    @SuppressWarnings("overrides")
    static class BadEq {
        final String name;

        BadEq(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof BadEq other && name.equals(other.name);
        }
    }

    static class GoodEq {
        final String name;

        GoodEq(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof GoodEq other && name.equals(other.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    /** equals przeciążone (a nie nadpisane) — nie ma @Override, bo to byłby błąd kompilacji. */
    @SuppressWarnings("overrides")
    static class Overloaded {
        final int x;
        final int y;

        Overloaded(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public boolean equals(Overloaded other) {
            return x == other.x && y == other.y;
        }
    }

    record Version(int major, int minor) implements Comparable<Version> {
        @Override
        public int compareTo(Version other) {
            int byMajor = Integer.compare(major, other.major);
            return byMajor != 0 ? byMajor : Integer.compare(minor, other.minor);
        }

        @Override
        public String toString() {
            return major + "." + minor;
        }
    }

    record Person(String name, int age) { }

    static class MutableKey {
        String name;

        MutableKey(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof MutableKey other && name.equals(other.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    /** Pamięć podręczna LRU: LinkedHashMap z kolejnością dostępu i usuwaniem najstarszego wpisu. */
    static class Lru<K, V> extends LinkedHashMap<K, V> {
        private static final long serialVersionUID = 1L;
        private final int capacity;

        Lru(int capacity) {
            super(16, 0.75f, true);
            this.capacity = capacity;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > capacity;
        }
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. false — obiekt trafia do koszyka po identycznościowym hashCode, nowy równy obiekt ma inny skrót i szuka w innym
     *      koszyku; equals nawet nie zostaje wywołane (karta 7). Dodaj hashCode oparty na polu name.
     *   2. ConcurrentModificationException (lub brak wyjątku, gdy usuwasz przedostatni element — wtedy lista po cichu
     *      pomija ostatni). Napraw: lista.removeIf(String::isEmpty) albo Iterator.remove() (karta 20).
     *   3. [10, 1] — remove(1) to remove(int index): usunięto element o indeksie 1 (20). Wartość usuwa
     *      remove(Integer.valueOf(1)) (karta 13).
     *   4. Obiekt leży w koszyku ze starym skrótem; po zmianie wyszukiwanie liczy nowy skrót i go nie znajduje
     *      (contains false, a size nadal 1) — element "znika" i nie da się go usunąć (karta 17).
     *   5. HashSet: 2 (różny hashCode, bo różna skala, i equals false); TreeSet: 1 (compareTo == 0) (karta 10).
     *   6. Gdy do koszyka z 8 wpisami trafia kolejny, a tablica ma co najmniej 64 koszyki: lista wpisów staje się
     *      drzewem czerwono-czarnym, więc wyszukiwanie w złym przypadku (dużo kolizji) kosztuje O(log n) zamiast O(n) (karta 14).
     *   7. synchronizedMap blokuje całą mapę przy każdej operacji i wymaga ręcznej synchronizacji iteracji; ConcurrentHashMap
     *      blokuje drobno (koszyki + CAS), ma atomowe merge/compute, słabo spójne iteratory i zakazuje null (karta 19).
     *   8. List.of: prawdziwie niezmienna, osobne dane, bez null. unmodifiableList: tylko widok na zmienną listę — zmiany
     *      oryginału są w niej widoczne (karta 21).
     */
    // </editor-fold>
}
