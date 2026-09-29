package t06_oop_basics;

import helpers.Check;
import java.util.Arrays;
import java.util.Locale;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Hermetyzacja — pola prywatne, gettery, settery i niezmienniki
 *        (encapsulation = hermetyzacja/enkapsulacja, getter = metoda pobierająca, setter = metoda ustawiająca)
 *
 * W SKRÓCIE:
 *   Obiekt sam pilnuje swoich danych. Pola są private (niedostępne z zewnątrz), a świat rozmawia
 *   z obiektem tylko przez metody, które sprawdzają dane. Dzięki temu reguła „saldo nigdy nie jest
 *   ujemne” (niezmiennik, invariant) jest prawdziwa ZAWSZE, a nie „jeśli wszyscy będą uważać”.
 *
 * ANALOGIA:
 *   Bankomat. Nie sięgasz ręką do sejfu z pieniędzmi (pole private). Masz przyciski „wpłać” i „wypłać”
 *   (metody publiczne). Bankomat sprawdza, czy masz środki, i odmawia, gdy nie masz. Gdyby sejf był
 *   otwarty dla każdego (pole public), żadna reguła by nie przetrwała.
 *
 * JAK TO DZIAŁA:
 *   class BankAccount {
 *       private int balance;                      ← nikt z zewnątrz nie zmieni pola bezpośrednio
 *       public int getBalance() { ... }           ← odczyt (getter)
 *       public void withdraw(int amount) { ... }  ← zmiana TYLKO przez metodę z walidacją
 *   }
 *   Modyfikatory dostępu (od najszerszego):
 *       public          → wszyscy
 *       protected       → ten sam pakiet + podklasy (dziedziczenie, t07)
 *       (brak)          → tylko ten sam pakiet (package-private)
 *       private         → tylko wnętrze tej samej klasy
 *
 * SŁÓWKA:
 *   encapsulation = hermetyzacja; private = prywatny; public = publiczny; access modifier = modyfikator
 *   dostępu; invariant = niezmiennik (reguła zawsze prawdziwa); balance = saldo; deposit = wpłać;
 *   withdraw = wypłać; amount = kwota; read-only = tylko do odczytu; tell, don't ask = „każ, nie pytaj”
 *
 * ZOBACZ TEŻ: t06_oop_basics/Oop02Constructors (walidacja przy tworzeniu),
 *             t06_oop_basics/Oop06Immutability (kopie obronne, obiekty bez setterów),
 *             t06_oop_basics/Oop08PackagesAccess (pakiety i widoczność pakietowa),
 *             t20_lombok/Lombok01Accessors (gettery i settery generowane automatycznie)
 * </pre>
 */
public class Oop03Encapsulation {

    public static void main(String[] args) {
        title("Oop03 — hermetyzacja");

        publicFieldsProblem();      // public fields problem = problem z publicznymi polami
        privateFieldsAndGetters();  // private fields and getters = pola prywatne i gettery
        operationsKeepInvariant();  // operations keep invariant = operacje pilnują niezmiennika
        settersWithValidation();    // setters with validation = settery z walidacją
        readOnlyProperties();       // read-only properties = właściwości tylko do odczytu
        namingConventions();        // naming conventions = konwencje nazewnicze
        tellDontAsk();              // tell, don't ask = każ, nie pytaj
        leakingInternals();         // leaking internals = wyciek wnętrza obiektu
        accessModifiers();          // access modifiers = modyfikatory dostępu
        exercises();                // exercises = ćwiczenia
    }

    // Klasy przykładowe są statycznymi klasami zagnieżdżonymi, by lekcja była jednym plikiem
    // (wyjaśnienie w t06_oop_basics/Oop01ClassesObjects).
    //
    // UWAGA — WAŻNA SPECYFIKA TEJ LEKCJI: klasa zewnętrzna (Oop03Encapsulation) WIDZI pola private
    // swoich klas zagnieżdżonych. Kompilator pozwoliłby więc napisać tu acc.balance = -500 nawet dla
    // BankAccount. W prawdziwym projekcie (BankAccount.java w osobnym pliku) to błąd kompilacji:
    //     acc.balance = -500;   // błąd kompilacji: balance has private access in BankAccount
    // Dlatego w tej lekcji UDAJEMY, że każda klasa leży w osobnym pliku, i nie dotykamy pól prywatnych.

    // =================================================================================================
    // 1. PROBLEM: PUBLICZNE POLA
    // =================================================================================================

    static class PublicAccount {        // public account = konto z publicznymi polami (ZŁY przykład)
        public String owner;            // owner = właściciel
        public int balance;             // balance = saldo — każdy może wpisać cokolwiek
    }

    /**
     * 1. Publiczne pole to otwarte drzwi: każdy fragment programu może wpisać bzdurę i nikt tego nie
     * zauważy aż do momentu katastrofy.
     */
    static void publicFieldsProblem() {
        section("1. Publiczne pola łamią niezmiennik");

        PublicAccount acc = new PublicAccount();
        acc.owner = "Jan";
        acc.balance = 100;
        acc.balance -= 600;             // „wypłata” bez sprawdzenia środków
        show("saldo", acc.balance);
        // WYNIK: saldo → -500
        acc.owner = "";                 // pusty właściciel — też nikt nie protestuje
        show("właściciel", "[" + acc.owner + "]");
        // WYNIK: właściciel → []

        // PUŁAPKA: reguła „saldo ≥ 0” istnieje tylko w głowie programisty. Każde z setek miejsc
        // w kodzie musiałoby jej pilnować samo. Wystarczy JEDNO zapomniane sprawdzenie.
        // Rozwiązanie: pole private + metody, które pilnują reguły w JEDNYM miejscu.
    }

    // =================================================================================================
    // 2. POLA PRYWATNE I GETTERY
    // =================================================================================================

    /** Konto bankowe z hermetyzacją — saldo nigdy nie spadnie poniżej zera. */
    static class BankAccount {          // bank account = konto bankowe
        private final String number;    // number = numer konta; final = przypisany raz (więcej: Oop06)
        private final String owner;
        private int balance;
        private int operationCount;     // operation count = liczba operacji

        public BankAccount(String number, String owner) {
            if (number == null || number.isBlank() || owner == null || owner.isBlank()) {  // isBlank (Java 11+)
                throw new IllegalArgumentException("numer i właściciel są wymagane");
            }
            this.number = number;
            this.owner = owner;
        }

        public String getNumber() {     // get number = pobierz numer — getter, BRAK settera
            return number;
        }

        public String getOwner() {      // get owner = pobierz właściciela
            return owner;
        }

        public int getBalance() {       // get balance = pobierz saldo
            return balance;
        }

        public int getOperationCount() {    // tylko odczyt — zmienia się wyłącznie „od środka”
            return operationCount;
        }

        public void deposit(int amount) {   // deposit = wpłać; amount = kwota
            requirePositive(amount);
            balance += amount;
            operationCount++;
        }

        public void withdraw(int amount) {  // withdraw = wypłać
            requirePositive(amount);
            if (amount > balance) {
                throw new IllegalStateException("brak środków: saldo " + balance + ", wypłata " + amount);
            }
            balance -= amount;
            operationCount++;
        }

        private static void requirePositive(int amount) {  // require positive = wymagaj dodatniej
            if (amount <= 0) {          // metoda PRYWATNA — szczegół wewnętrzny, nie część „umowy”
                throw new IllegalArgumentException("kwota musi być dodatnia: " + amount);
            }
        }
    }

    /**
     * 2. Pola są private, a do odczytu służą gettery. Klasa decyduje, co pokazuje, a czego nie.
     */
    static void privateFieldsAndGetters() {
        section("2. Pola prywatne + gettery");

        BankAccount acc = new BankAccount("PL-001", "Jan Kowalski");
        show("getNumber()", acc.getNumber());
        // WYNIK: getNumber() → PL-001
        show("getOwner()", acc.getOwner());
        // WYNIK: getOwner() → Jan Kowalski
        show("getBalance()", acc.getBalance());
        // WYNIK: getBalance() → 0

        // Z INNEGO pliku nie da się napisać acc.balance = -500 (błąd kompilacji: balance has private
        // access in BankAccount). Jedyna droga zmiany salda to deposit/withdraw — a te pilnują reguł.
        // DOBRA PRAKTYKA: domyślnie KAŻDE pole private. Otwieraj dostęp tylko wtedy, gdy musisz.
    }

    // =================================================================================================
    // 3. OPERACJE PILNUJĄ NIEZMIENNIKA
    // =================================================================================================

    /**
     * 3. Zamiast setBalance(...) mamy operacje biznesowe deposit/withdraw. Każda sprawdza dane, więc
     * saldo nigdy nie będzie ujemne — niezależnie od tego, kto i gdzie wywołuje metody.
     */
    static void operationsKeepInvariant() {
        section("3. deposit/withdraw — saldo zawsze ≥ 0");

        BankAccount acc = new BankAccount("PL-002", "Ola Lis");
        acc.deposit(100);
        acc.withdraw(30);
        show("saldo po +100 i -30", acc.getBalance());
        // WYNIK: saldo po +100 i -30 → 70

        expectThrows("wypłata 500 przy saldzie 70", () -> acc.withdraw(500));
        // WYNIK: ✔ wypłata 500 przy saldzie 70 → rzucono IllegalStateException: brak środków: saldo 70, wypłata 500
        expectThrows("wpłata -20", () -> acc.deposit(-20));
        // WYNIK: ✔ wpłata -20 → rzucono IllegalArgumentException: kwota musi być dodatnia: -20
        show("saldo po nieudanych próbach", acc.getBalance());
        // WYNIK: saldo po nieudanych próbach → 70

        // JAK TO DZIAŁA: najpierw sprawdzenie, dopiero potem zmiana. Gdy sprawdzenie zawiedzie,
        // wyjątek przerywa metodę PRZED modyfikacją pola — stan zostaje poprawny.
        // IllegalArgumentException = zły argument (kwota ≤ 0);
        // IllegalStateException = argument OK, ale obiekt jest w stanie, który na to nie pozwala (za mało środków).
    }

    // =================================================================================================
    // 4. SETTERY Z WALIDACJĄ
    // =================================================================================================

    static class Person {               // person = osoba
        private String name;
        private int age;
        private boolean student;        // student = czy jest studentem

        public Person(String name, int age) {
            setName(name);              // konstruktor korzysta z setterów — walidacja w jednym miejscu
            setAge(age);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {      // set name = ustaw imię
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("imię nie może być puste");
            }
            this.name = name.strip();   // strip = obetnij białe znaki z brzegów (Java 11+)
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {   // set age = ustaw wiek
            if (age < 0 || age > 150) {
                throw new IllegalArgumentException("wiek poza zakresem 0–150: " + age);
            }
            this.age = age;
        }

        public boolean isStudent() {    // getter dla boolean: is... zamiast get...
            return student;
        }

        public void setStudent(boolean student) {
            this.student = student;
        }
    }

    /**
     * 4. Setter to nie „furtka do pola”. Dobry setter sprawdza dane i może je poprawić (strip).
     */
    static void settersWithValidation() {
        section("4. Settery z walidacją");

        Person p = new Person("  Ala  ", 25);
        show("imię po strip", "[" + p.getName() + "]");
        // WYNIK: imię po strip → [Ala]

        p.setAge(26);
        show("nowy wiek", p.getAge());
        // WYNIK: nowy wiek → 26

        expectThrows("setAge(-3)", () -> p.setAge(-3));
        // WYNIK: ✔ setAge(-3) → rzucono IllegalArgumentException: wiek poza zakresem 0–150: -3
        expectThrows("setName(\"   \")", () -> p.setName("   "));
        // WYNIK: ✔ setName("   ") → rzucono IllegalArgumentException: imię nie może być puste
        show("stan po odrzuconych zmianach", p.getName() + ", " + p.getAge());
        // WYNIK: stan po odrzuconych zmianach → Ala, 26

        // PUŁAPKA: setter „wygenerowany i zapomniany” (this.age = age bez sprawdzenia) daje tyle samo
        // ochrony co pole public. Sam private nic nie daje — liczy się walidacja w metodach.
        // DOBRA PRAKTYKA: nie twórz setterów „na zapas”. Brak settera = pole nie zmieni się z zewnątrz.
    }

    // =================================================================================================
    // 5. WŁAŚCIWOŚCI TYLKO DO ODCZYTU
    // =================================================================================================

    /**
     * 5. Getter bez settera = właściwość tylko do odczytu (read-only). Numer konta ustawia konstruktor,
     * a licznik operacji zmieniają wyłącznie metody wewnątrz klasy.
     */
    static void readOnlyProperties() {
        section("5. Właściwości tylko do odczytu");

        BankAccount acc = new BankAccount("PL-003", "Adam Mazur");
        acc.deposit(50);
        acc.deposit(20);
        acc.withdraw(10);
        show("numer", acc.getNumber());
        // WYNIK: numer → PL-003
        show("liczba operacji", acc.getOperationCount());
        // WYNIK: liczba operacji → 3

        // Nie ma setNumber ani setOperationCount — i to jest zaleta: numer konta się nie zmienia,
        // a licznik zawsze zgadza się z liczbą faktycznych operacji.
        // Właściwość może też być WYLICZANA (bez pola), np. getAverage() liczy średnią „w locie”.
    }

    // =================================================================================================
    // 6. KONWENCJE NAZW: GET / IS / SET
    // =================================================================================================

    /**
     * 6. Konwencja JavaBeans: getX() dla odczytu, isX() dla boolean, setX(...) dla zapisu.
     * Biblioteki (JSON, bazy danych, Lombok) rozpoznają właściwości właśnie po tych nazwach.
     */
    static void namingConventions() {
        section("6. Konwencje: getX, isX, setX");

        Person p = new Person("Bartek", 20);
        p.setStudent(true);
        show("p.isStudent()", p.isStudent());
        // WYNIK: p.isStudent() → true
        show("p.getName()", p.getName());
        // WYNIK: p.getName() → Bartek

        // pole name    → getName() / setName(String)
        // pole student → isStudent() / setStudent(boolean)     (NIE getStudent)
        // PUŁAPKA: nazwa isIsStudent() albo pole isStudent → getter isIsStudent — nazywaj pole bez „is”.
    }

    // =================================================================================================
    // 7. TELL, DON'T ASK — KAŻ, NIE PYTAJ
    // =================================================================================================

    static class Wallet {               // wallet = portfel — wersja „pytaj” (ZŁA)
        private int money;              // money = pieniądze

        public int getMoney() {
            return money;
        }

        public void setMoney(int money) {
            this.money = money;
        }
    }

    /**
     * 7. Zamiast wyciągać dane z obiektu, liczyć „na zewnątrz” i wpychać wynik setterem — każ obiektowi
     * wykonać operację. Logika zostaje w jednym miejscu, obok danych.
     */
    static void tellDontAsk() {
        section("7. Tell, don't ask — każ, nie pytaj");

        // PRZED: „pytamy” o dane i sami decydujemy. Ta sama logika powtarza się w wielu miejscach.
        Wallet wallet = new Wallet();
        wallet.setMoney(100);
        if (wallet.getMoney() >= 40) {
            wallet.setMoney(wallet.getMoney() - 40);
        }
        show("portfel (pytaj)", wallet.getMoney());
        // WYNIK: portfel (pytaj) → 60
        wallet.setMoney(-999);          // ...i nic nie broni przed bzdurą
        show("portfel po setMoney(-999)", wallet.getMoney());
        // WYNIK: portfel po setMoney(-999) → -999

        // PO: „każemy” obiektowi — reguła jest w metodzie withdraw.
        BankAccount acc = new BankAccount("PL-004", "Ewa Lis");
        acc.deposit(100);
        acc.withdraw(40);
        show("konto (każ)", acc.getBalance());
        // WYNIK: konto (każ) → 60

        // DOBRA PRAKTYKA: para getX() + setX(getX() - ...) to sygnał, że operacja powinna być metodą
        // w klasie (withdraw, addItem, applyDiscount).
    }

    // =================================================================================================
    // 8. WYCIEK WNĘTRZA PRZEZ GETTER
    // =================================================================================================

    static class LeakyScores {          // leaky scores = „dziurawe” wyniki
        private final int[] scores = {80, 90, 70};  // scores = wyniki punktowe

        public int[] getScores() {
            return scores;              // PUŁAPKA: oddajemy referencję do WEWNĘTRZNEJ tablicy
        }
    }

    static class SafeScores {           // safe scores = bezpieczne wyniki
        private final int[] scores = {80, 90, 70};

        public int[] getScores() {
            return Arrays.copyOf(scores, scores.length);    // kopia obronna (defensive copy)
        }
    }

    /**
     * 8. Pole private z tablicą nie jest bezpieczne, jeśli getter oddaje tę samą tablicę. Wywołujący
     * zmieni jej zawartość bez żadnej walidacji.
     */
    static void leakingInternals() {
        section("8. Wyciek wnętrza przez getter");

        LeakyScores leaky = new LeakyScores();
        leaky.getScores()[0] = -1000;   // modyfikujemy „prywatne” dane z zewnątrz!
        show("leaky po modyfikacji", Arrays.toString(leaky.getScores()));
        // WYNIK: leaky po modyfikacji → [-1000, 90, 70]

        SafeScores safe = new SafeScores();
        safe.getScores()[0] = -1000;    // zmieniamy tylko KOPIĘ
        show("safe po modyfikacji", Arrays.toString(safe.getScores()));
        // WYNIK: safe po modyfikacji → [80, 90, 70]

        // DOBRA PRAKTYKA: getter zwracający tablicę (lub listę) oddaje kopię. Pełny temat kopii
        // obronnych: t06_oop_basics/Oop06Immutability.
    }

    // =================================================================================================
    // 9. MODYFIKATORY DOSTĘPU
    // =================================================================================================

    /**
     * 9. Cztery poziomy dostępu. Wypisujemy tabelę: kto widzi składową (pole/metodę) z danym modyfikatorem.
     */
    static void accessModifiers() {
        section("9. Modyfikatory dostępu — tabela");

        String header = String.format(Locale.ROOT, "%-16s %-6s %-7s %-8s %-6s",
                "modyfikator", "klasa", "pakiet", "podklasa", "świat");
        System.out.println(header);
        // WYNIK: modyfikator      klasa  pakiet  podklasa świat
        printRow("public", "tak", "tak", "tak", "tak");
        // WYNIK: public           tak    tak     tak      tak
        printRow("protected", "tak", "tak", "tak", "nie");
        // WYNIK: protected        tak    tak     tak      nie
        printRow("(brak)", "tak", "tak", "nie", "nie");
        // WYNIK: (brak)           tak    tak     nie      nie
        printRow("private", "tak", "nie", "nie", "nie");
        // WYNIK: private          tak    nie     nie      nie

        // „podklasa” = klasa dziedzicząca w INNYM pakiecie (dziedziczenie: t07_inheritance_polymorphism/Inherit01Basics).
        // (brak) = package-private: widoczne w tym samym pakiecie — t06_oop_basics/Oop08PackagesAccess.
        // DOBRA PRAKTYKA: pola → private; metody „dla świata” → public; pomocnicze → private;
        // protected i package-private — świadomie, gdy wiesz, po co.
        // PUŁAPKA: specyfika tego pliku — klasy zagnieżdżone widzą nawzajem swoje private (uwaga na górze).
    }

    static void printRow(String modifier, String cls, String pkg, String sub, String world) {  // print row = wypisz wiersz
        System.out.println(String.format(Locale.ROOT, "%-16s %-6s %-7s %-8s %-6s", modifier, cls, pkg, sub, world));
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • pola private; dostęp przez metody → obiekt sam pilnuje swoich reguł (niezmienników)
     *   • getter: getX() / isX() dla boolean; setter: setX(value) — Z WALIDACJĄ albo wcale
     *   • zamiast setBalance → operacje biznesowe: deposit, withdraw (tell, don't ask)
     *   • najpierw sprawdź, potem zmień — wyjątek nie zostawi obiektu w połowie zmiany
     *   • IllegalArgumentException = zły argument; IllegalStateException = zły stan obiektu
     *   • brak settera = właściwość tylko do odczytu
     *   • getter tablicy/listy → zwróć kopię (inaczej wnętrze „wycieka”)
     *   • modyfikatory:  public (wszyscy) > protected (pakiet + podklasy) > (brak) (pakiet) > private (klasa)
     *   • w tym kursie klasy są zagnieżdżone i widzą swoje private — w projekcie osobne pliki tego nie pozwolą
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego publiczne pole balance jest niebezpieczne, skoro „wszyscy wiedzą”, że saldo ≥ 0?
     *   2. ZNAJDŹ BŁĄD:  public void setAge(int age) { if (age < 0) throw new IllegalArgumentException(); age = age; }
     *   3. Co wypisze:  BankAccount a = new BankAccount("PL", "Ala"); a.deposit(100);
     *                   try { a.withdraw(150); } catch (IllegalStateException e) { }
     *                   System.out.println(a.getBalance());  ?
     *   4. Jak nazwiesz getter dla pola boolean active?
     *   5. ZNAJDŹ BŁĄD:  private int[] scores;  public int[] getScores() { return scores; }
     *   6. Czym różni się pole private od pola bez modyfikatora?
     *   7. Co znaczy „tell, don't ask”? Podaj przykład PRZED/PO.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: gettery produktu", "Kawa 65", () -> exercise1());
        Check.throwsException("ćw. 1b: setPrice(-1) → wyjątek", IllegalArgumentException.class,
                () -> new Ex1Product("Kawa", 65).setPrice(-1));
        Check.equal("ćw. 2: termostat w granicach 16–26", "26,16,21", () -> exercise2());
        Check.equal("ćw. 3: przelew między kontami", "a=70, b=130, odrzucono=true", () -> exercise3());
        Check.equal("ćw. 4: tablica wyników", "liczba=3, średnia=80.0, pierwszy=80, odrzucono=true",
                () -> exercise4());
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", "Kawa 65", () -> solution1());
        Check.throwsException("ćw. 1b (wzorzec)", IllegalArgumentException.class,
                () -> new Sol1Product("Kawa", 65).setPrice(-1));
        Check.equal("ćw. 2 (wzorzec)", "26,16,21", () -> solution2());
        Check.equal("ćw. 3 (wzorzec)", "a=70, b=130, odrzucono=true", () -> solution3());
        Check.equal("ćw. 4 (wzorzec)", "liczba=3, średnia=80.0, pierwszy=80, odrzucono=true", () -> solution4());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): PRZEPISZ klasę z publicznymi polami na klasę z hermetyzacją:
     * <pre>{@code
     * class Product { public String name; public int price; }
     * }</pre>
     * Ex1Product ma już pola private i konstruktor. Dopisz getName(), getPrice() i setPrice(int),
     * który dla ceny ujemnej rzuca IllegalArgumentException.
     * Podpowiedź: w setterze najpierw if + throw, potem this.price = price.
     */
    static class Ex1Product {           // product = produkt
        private String name;
        private int price;

        Ex1Product(String name, int price) {
            this.name = name;
            this.price = price;
        }

        public String getName() {
            // TODO
            throw new UnsupportedOperationException("TODO");
        }

        public int getPrice() {
            // TODO
            throw new UnsupportedOperationException("TODO");
        }

        public void setPrice(int price) {
            // TODO: walidacja (price < 0 → wyjątek) i przypisanie
        }
    }

    static String exercise1() {
        Ex1Product p = new Ex1Product("Kawa", 65);
        return p.getName() + " " + p.getPrice();
    }

    /**
     * ĆWICZENIE 2 (średnie): termostat przyjmuje dowolną liczbę w setTarget, ale PRZYCINA ją do zakresu
     * 16–26 (za dużo → 26, za mało → 16). exercise2 ustawia 30, 10 i 21 i zwraca „26,16,21”.
     * Podpowiedź: Math.max(16, Math.min(26, value)).
     */
    static class Ex2Thermostat {        // thermostat = termostat
        private int target = 20;        // target = temperatura docelowa

        public int getTarget() {
            return target;
        }

        public void setTarget(int value) {
            // TODO: przytnij value do zakresu 16–26 i zapisz w target
        }
    }

    static String exercise2() {
        Ex2Thermostat t = new Ex2Thermostat();
        t.setTarget(30);
        int first = t.getTarget();
        t.setTarget(10);
        int second = t.getTarget();
        t.setTarget(21);
        return first + "," + second + "," + t.getTarget();
    }

    /**
     * ĆWICZENIE 3 (średnie): „każ, nie pytaj” — dopisz transferTo(other, amount): gdy kwota nie przekracza
     * salda, zmniejsz własne saldo i zwiększ saldo other; gdy środków brak — rzuć IllegalStateException
     * i NICZEGO nie zmieniaj. Oczekiwane: „a=70, b=130, odrzucono=true”.
     * Podpowiedź: sprawdzenie na samym początku metody, przed jakąkolwiek zmianą.
     */
    static class Ex3Account {           // account = konto
        private int balance;

        Ex3Account(int balance) {
            this.balance = balance;
        }

        public int getBalance() {
            return balance;
        }

        public void transferTo(Ex3Account other, int amount) {  // transfer to = przelej do
            // TODO
            throw new UnsupportedOperationException("TODO");
        }
    }

    static String exercise3() {
        Ex3Account a = new Ex3Account(100);
        Ex3Account b = new Ex3Account(100);
        a.transferTo(b, 30);
        boolean rejected = false;       // rejected = odrzucono
        try {
            a.transferTo(b, 500);
        } catch (IllegalStateException e) {
            rejected = true;
        }
        return "a=" + a.getBalance() + ", b=" + b.getBalance() + ", odrzucono=" + rejected;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): tablica wyników na maks. 10 wpisów. addScore(int) przyjmuje 0–100
     * (inaczej IllegalArgumentException), getCount() zwraca liczbę wpisów, getAverage() liczy średnią
     * (double), a getScores() zwraca KOPIĘ tylko zapisanych wyników. exercise4 próbuje „zepsuć” dane
     * przez getScores()[0] = 0 — średnia ma zostać 80.0.
     * Oczekiwane: „liczba=3, średnia=80.0, pierwszy=80, odrzucono=true”.
     * Podpowiedź: pola private int[] scores = new int[10] i private int count; Arrays.copyOf(scores, count).
     */
    static class Ex4ScoreBoard {        // score board = tablica wyników
        public void addScore(int score) {
            // TODO
            throw new UnsupportedOperationException("TODO");
        }

        public int getCount() {
            // TODO
            throw new UnsupportedOperationException("TODO");
        }

        public double getAverage() {
            // TODO
            throw new UnsupportedOperationException("TODO");
        }

        public int[] getScores() {
            // TODO
            throw new UnsupportedOperationException("TODO");
        }
    }

    static String exercise4() {
        Ex4ScoreBoard board = new Ex4ScoreBoard();
        board.addScore(80);
        board.addScore(90);
        board.addScore(70);
        board.getScores()[0] = 0;       // próba modyfikacji z zewnątrz — nie może zadziałać
        boolean rejected = false;
        try {
            board.addScore(150);
        } catch (IllegalArgumentException e) {
            rejected = true;
        }
        return "liczba=" + board.getCount() + ", średnia=" + board.getAverage()
                + ", pierwszy=" + board.getScores()[0] + ", odrzucono=" + rejected;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static class Sol1Product {
        private String name;
        private int price;

        Sol1Product(String name, int price) {
            this.name = name;
            this.price = price;
        }

        public String getName() {
            return name;
        }

        public int getPrice() {
            return price;
        }

        public void setPrice(int price) {
            if (price < 0) {
                throw new IllegalArgumentException("cena nie może być ujemna: " + price);
            }
            this.price = price;
        }
    }

    static String solution1() {
        Sol1Product p = new Sol1Product("Kawa", 65);
        return p.getName() + " " + p.getPrice();
    }

    static class Sol2Thermostat {
        private int target = 20;

        public int getTarget() {
            return target;
        }

        public void setTarget(int value) {
            this.target = Math.max(16, Math.min(26, value));
        }
    }

    static String solution2() {
        Sol2Thermostat t = new Sol2Thermostat();
        t.setTarget(30);
        int first = t.getTarget();
        t.setTarget(10);
        int second = t.getTarget();
        t.setTarget(21);
        return first + "," + second + "," + t.getTarget();
    }

    static class Sol3Account {
        private int balance;

        Sol3Account(int balance) {
            this.balance = balance;
        }

        public int getBalance() {
            return balance;
        }

        public void transferTo(Sol3Account other, int amount) {
            if (amount <= 0) {
                throw new IllegalArgumentException("kwota musi być dodatnia: " + amount);
            }
            if (amount > balance) {
                throw new IllegalStateException("brak środków: " + balance + " < " + amount);
            }
            this.balance -= amount;     // obie zmiany dopiero PO wszystkich sprawdzeniach
            other.balance += amount;    // (ta sama klasa → widzimy private pole innego obiektu)
        }
    }

    static String solution3() {
        Sol3Account a = new Sol3Account(100);
        Sol3Account b = new Sol3Account(100);
        a.transferTo(b, 30);
        boolean rejected = false;
        try {
            a.transferTo(b, 500);
        } catch (IllegalStateException e) {
            rejected = true;
        }
        return "a=" + a.getBalance() + ", b=" + b.getBalance() + ", odrzucono=" + rejected;
    }

    static class Sol4ScoreBoard {
        private final int[] scores = new int[10];
        private int count;

        public void addScore(int score) {
            if (score < 0 || score > 100) {
                throw new IllegalArgumentException("wynik spoza 0–100: " + score);
            }
            if (count == scores.length) {
                throw new IllegalStateException("tablica wyników pełna");
            }
            scores[count] = score;
            count++;
        }

        public int getCount() {
            return count;
        }

        public double getAverage() {    // właściwość WYLICZANA — nie ma pola average
            if (count == 0) {
                return 0.0;
            }
            int sum = 0;
            for (int i = 0; i < count; i++) {
                sum += scores[i];
            }
            return (double) sum / count;
        }

        public int[] getScores() {
            return Arrays.copyOf(scores, count);
        }
    }

    static String solution4() {
        Sol4ScoreBoard board = new Sol4ScoreBoard();
        board.addScore(80);
        board.addScore(90);
        board.addScore(70);
        board.getScores()[0] = 0;
        boolean rejected = false;
        try {
            board.addScore(150);
        } catch (IllegalArgumentException e) {
            rejected = true;
        }
        return "liczba=" + board.getCount() + ", średnia=" + board.getAverage()
                + ", pierwszy=" + board.getScores()[0] + ", odrzucono=" + rejected;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo reguła żyje tylko w głowach. Wystarczy jedno miejsce w kodzie, które zapomni o sprawdzeniu,
     *      i saldo staje się ujemne. Z polem private i metodą withdraw reguła jest sprawdzana zawsze.
     *   2. age = age przypisuje parametr sam do siebie — pole się nie zmienia. Poprawnie: this.age = age;
     *   3. 100 — withdraw(150) rzuca wyjątek PRZED zmianą salda, więc saldo zostaje 100.
     *   4. isActive() (a setter: setActive(boolean)).
     *   5. Getter oddaje referencję do wewnętrznej tablicy; ktoś może zmienić jej elementy z zewnątrz.
     *      Poprawnie: return Arrays.copyOf(scores, scores.length);
     *   6. private — widoczne tylko w tej klasie. Bez modyfikatora (package-private) — widoczne w całym
     *      pakiecie, czyli także dla innych klas z tego samego katalogu.
     *   7. Zamiast pytać obiekt o dane i decydować za niego, każ mu wykonać operację.
     *      PRZED: if (w.getMoney() >= 40) w.setMoney(w.getMoney() - 40);   PO: acc.withdraw(40);
     */
    // </editor-fold>
}
