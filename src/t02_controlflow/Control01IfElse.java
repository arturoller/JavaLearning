package t02_controlflow;

import helpers.Check;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Instrukcja if / else if / else — podejmowanie decyzji
 *        (if = jeżeli; else = w przeciwnym razie; else if = a jeżeli nie, to może...)
 *
 * W SKRÓCIE:
 *   if wykonuje blok kodu tylko wtedy, gdy warunek (typu boolean) jest prawdziwy.
 *   else if oraz else dokładają kolejne ścieżki. Pierwszy pasujący warunek wygrywa,
 *   a wszystkie następne są pomijane — dlatego kolejność warunków ma znaczenie.
 *
 * ANALOGIA: skrzyżowanie z drogowskazami. Kierowca czyta tablice po kolei:
 *   „jeśli jedziesz do Krakowa — w lewo”, „a jeśli do Gdańska — w prawo”, „w przeciwnym razie — prosto”.
 *   Skręca przy PIERWSZEJ pasującej tablicy i dalszych już nie czyta.
 *
 * JAK TO DZIAŁA:
 *   if (warunek1) {          ← warunek MUSI być typu boolean (nie int jak w C!)
 *       blok A               ← tylko gdy warunek1 jest true
 *   } else if (warunek2) {   ← sprawdzany TYLKO wtedy, gdy warunek1 był false
 *       blok B
 *   } else {                 ← gdy żaden warunek nie pasował
 *       blok C
 *   }
 *   Wykona się DOKŁADNIE JEDEN z bloków A / B / C (a bez else — co najwyżej jeden).
 *
 *   Warunki łączysz operatorami {@code && || !} (poznane w t01_basics/Basics04Operators).
 *   {@code &&} i {@code ||} są „leniwe” (short-circuit): gdy wynik jest już znany, prawa strona się NIE wykonuje.
 *
 * SŁÓWKA:
 *   condition = warunek; branch = gałąź; nested = zagnieżdżony; braces = nawiasy klamrowe;
 *   guard clause = klauzula strażnika; early return = wczesny powrót; flag = flaga (zmienna boolean);
 *   ternary operator = operator trójargumentowy (?:); short-circuit = skrócone obliczanie
 *
 * ZOBACZ TEŻ: t01_basics/Basics04Operators (operatory logiczne i ?:),
 *   t02_controlflow/Control02Switch (gdy porównujesz jedną wartość z wieloma stałymi),
 *   t04_strings/Strings01Basics (dlaczego teksty porównujemy przez equals)
 * </pre>
 */
public class Control01IfElse {

    public static void main(String[] args) {
        title("Control01 — if / else if / else: podejmowanie decyzji");

        basicIfElse();          // basic if else = podstawowe if / else
        elseIfChain();          // else if chain = łańcuch else if
        alwaysBraces();         // always braces = zawsze klamry
        stringConditions();     // string conditions = warunki na tekstach
        combiningConditions();  // combining conditions = łączenie warunków
        guardClauses();         // guard clauses = klauzule strażnika (wczesny return)
        booleanFlags();         // boolean flags = flagi logiczne
        ternaryRecap();         // ternary recap = powtórka operatora ?:
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. IF ORAZ IF / ELSE
    // =================================================================================================

    /**
     * 1. Najprostszy if oraz para if / else. Warunek w nawiasach musi mieć typ boolean.
     */
    static void basicIfElse() {
        section("1. if oraz if / else");

        int temperature = 28; // temperature = temperatura
        if (temperature > 25) {
            System.out.println("Gorąco — weź wodę!"); // println = wypisz linię
        }
        // WYNIK: Gorąco — weź wodę!

        int hour = 21; // hour = godzina
        if (hour < 18) {
            System.out.println("Dzień dobry");
        } else {
            System.out.println("Dobry wieczór");
        }
        // WYNIK: Dobry wieczór

        // Warunek to DOWOLNE wyrażenie typu boolean: porównanie, zmienna boolean, wynik metody.
        boolean raining = true; // raining = pada deszcz
        if (raining) {
            System.out.println("Parasol!");
        }
        // WYNIK: Parasol!

        // PUŁAPKA: w Javie warunek MUSI być typu boolean. Z języka C znasz „if (x)” dla liczby — tu to błąd:
        //   int count = 1;
        //   if (count) { ... }        // błąd kompilacji: int nie może być zamieniony na boolean
        //   if (count != 0) { ... }   // tak trzeba — porównanie daje boolean

        // PUŁAPKA: = to PRZYPISANIE, == to PORÓWNANIE. Dla int „if (x = 5)” się nie skompiluje (i dobrze),
        // ale dla boolean kompiluje się BEZ błędu — i warunek jest zawsze true!
        boolean done = false; // done = zrobione
        if (done = true) {    // ← celowy błąd: przypisanie zamiast porównania
            System.out.println("Weszliśmy do if, chociaż done było false!");
        }
        // WYNIK: Weszliśmy do if, chociaż done było false!
        show("done po tym if-ie", done); // show = pokaż (etykieta → wartość)
        // WYNIK: done po tym if-ie → true
        // Ratunek: dla boolean nie pisz „== true” w ogóle — pisz po prostu „if (done)” (więcej w sekcji 7).
    }

    // =================================================================================================
    // 2. ŁAŃCUCH ELSE IF
    // =================================================================================================

    /**
     * 2. Łańcuch else if: sprawdzamy warunki po kolei, wygrywa pierwszy prawdziwy.
     * Dlatego warunki „najbardziej szczegółowe” (najwęższe) muszą stać NA POCZĄTKU.
     */
    static void elseIfChain() {
        section("2. Łańcuch else if — kolejność ma znaczenie");

        int points = 87;  // points = punkty
        String grade;     // grade = ocena (bez wartości początkowej — patrz komentarz niżej)
        if (points >= 90) {
            grade = "5 (bardzo dobry)";
        } else if (points >= 75) {
            grade = "4 (dobry)";
        } else if (points >= 50) {
            grade = "3 (dostateczny)";
        } else {
            grade = "2 (niedostateczny)";
        }
        show("87 pkt, dobra kolejność", grade);
        // WYNIK: 87 pkt, dobra kolejność → 4 (dobry)

        // Kompilator pilnuje, żeby grade miała wartość w KAŻDEJ gałęzi (definite assignment = pewne przypisanie).
        // Usuń końcowe else, a dostaniesz błąd kompilacji: zmienna grade mogła nie zostać zainicjowana.

        // PUŁAPKA: zła kolejność. Warunek „>= 50” jest prawdziwy także dla 87 i 95, więc „łapie” wszystko.
        show("87 pkt, zła kolejność", gradeWrongOrder(87));
        // WYNIK: 87 pkt, zła kolejność → 3 (dostateczny)
        show("95 pkt, zła kolejność", gradeWrongOrder(95));
        // WYNIK: 95 pkt, zła kolejność → 3 (dostateczny)

        // Alternatywa: pełne przedziały (od–do). Kolejność wtedy nie ma znaczenia, ale kod jest dłuższy
        // i łatwiej o „dziurę” lub „zakładkę” między przedziałami.
        show("95 pkt, pełne przedziały", gradeByRanges(95));
        // WYNIK: 95 pkt, pełne przedziały → 5 (bardzo dobry)
        show("49 pkt, pełne przedziały", gradeByRanges(49));
        // WYNIK: 49 pkt, pełne przedziały → 2 (niedostateczny)

        // DOBRA PRAKTYKA: w łańcuchu progów „od góry” (>= 90, >= 75, >= 50) zaczynaj od NAJWYŻSZEGO progu.
        // Ogólna zasada: najpierw przypadki szczególne, na końcu przypadek ogólny (else).
    }

    /** Błędna kolejność warunków — pierwszy warunek jest zbyt szeroki. */
    static String gradeWrongOrder(int points) {
        if (points >= 50) {
            return "3 (dostateczny)";
        } else if (points >= 75) {        // ← tu nigdy nie dojdziemy dla 75+
            return "4 (dobry)";
        } else if (points >= 90) {        // ← ani tu
            return "5 (bardzo dobry)";
        } else {
            return "2 (niedostateczny)";
        }
    }

    /** Przedziały z obiema granicami — działają w każdej kolejności. */
    static String gradeByRanges(int points) {
        if (points >= 50 && points < 75) {
            return "3 (dostateczny)";
        } else if (points >= 75 && points < 90) {
            return "4 (dobry)";
        } else if (points >= 90) {
            return "5 (bardzo dobry)";
        } else {
            return "2 (niedostateczny)";
        }
    }

    // =================================================================================================
    // 3. ZAWSZE KLAMRY
    // =================================================================================================

    /**
     * 3. Bez klamer if obejmuje tylko JEDNĄ następną instrukcję. Wcięcia nic nie znaczą dla kompilatora.
     */
    static void alwaysBraces() {
        section("3. Zawsze stawiaj klamry { }");

        int stock = 0; // stock = stan magazynowy
        if (stock > 0)
            System.out.println("Produkt dostępny");
            System.out.println("Dodano do koszyka");   // ← wcięte, ale NIE należy do if!
        // WYNIK: Dodano do koszyka
        // Towaru nie ma, a i tak „dodaliśmy do koszyka”. Druga linia wykonuje się ZAWSZE.

        // Ta sama logika z klamrami — teraz obie linie należą do if:
        if (stock > 0) {
            System.out.println("Produkt dostępny");
            System.out.println("Dodano do koszyka");
        } else {
            System.out.println("Brak towaru — nic nie dodano");
        }
        // WYNIK: Brak towaru — nic nie dodano

        // PUŁAPKA: „wiszące else” (dangling else). Do którego if należy to else?
        //   if (a > 0)
        //       if (b > 0)
        //           System.out.println("oba dodatnie");
        //   else
        //       System.out.println("a nie jest dodatnie");   ← wcięcie KŁAMIE!
        // Reguła: else łączy się z NAJBLIŻSZYM wcześniejszym if, który nie ma jeszcze else — tu z if (b > 0).
        // Skutek: dla a = -1 nic się nie wypisze, a dla a = 1, b = -1 wypisze się „a nie jest dodatnie” — bzdura.
        // Z klamrami nie ma żadnych wątpliwości:
        //   if (a > 0) {
        //       if (b > 0) { System.out.println("oba dodatnie"); }
        //   } else {
        //       System.out.println("a nie jest dodatnie");
        //   }

        // PUŁAPKA: średnik zaraz po if tworzy PUSTĄ instrukcję:
        //   if (stock > 0); { System.out.println("Dostępny"); }   ← blok { } wykona się ZAWSZE
        // Kompilator z opcją -Xlint ostrzega o tym („empty statement after if”), ale nie zatrzymuje kompilacji.

        // DOBRA PRAKTYKA: klamry zawsze, nawet dla jednej linii. Dopisanie drugiej linii za pół roku
        // nie zepsuje wtedy logiki, a różnice w systemie kontroli wersji są czytelniejsze.
    }

    // =================================================================================================
    // 4. WARUNKI NA TEKSTACH
    // =================================================================================================

    /**
     * 4. Teksty (String) porównujemy metodą equals, a nie operatorem ==.
     * Operator == sprawdza, czy to TEN SAM obiekt w pamięci, a nie czy ma tę samą treść.
     */
    static void stringConditions() {
        section("4. Warunki na tekstach: equals, a nie ==");

        // Symulujemy tekst wpisany przez użytkownika (np. ze Scannera) — to NOWY obiekt w pamięci.
        String typed = new String("tak"); // typed = wpisany
        show("typed == \"tak\"", typed == "tak");
        // WYNIK: typed == "tak" → false
        show("typed.equals(\"tak\")", typed.equals("tak")); // equals = równa się (ta sama treść)
        // WYNIK: typed.equals("tak") → true
        show("\"TAK\".equalsIgnoreCase(\"tak\")", "TAK".equalsIgnoreCase("tak")); // ignore case = ignoruj wielkość liter
        // WYNIK: "TAK".equalsIgnoreCase("tak") → true

        // Dlaczego == czasem „działa”? Stałe tekstowe w kodzie Java trzyma w jednej puli (string pool),
        // więc "tak" == "tak" bywa true. To przypadek, na którym NIE wolno polegać (szczegóły: t04_strings).

        String command = new String("stop"); // command = polecenie
        if ("stop".equals(command)) {
            System.out.println("Zatrzymuję program");
        } else {
            System.out.println("Nieznane polecenie");
        }
        // WYNIK: Zatrzymuję program

        // PUŁAPKA: gdy tekst może być null, wywołanie metody NA nim rzuca NullPointerException.
        String answer = null; // answer = odpowiedź (np. użytkownik nic nie wpisał)
        show("\"tak\".equals(answer)", "tak".equals(answer));
        // WYNIK: "tak".equals(answer) → false
        expectThrows("answer.equals(\"tak\") gdy answer == null", () -> answer.equals("tak"));
        // WYNIK: ✔ answer.equals("tak") gdy answer == null → rzucono NullPointerException: Cannot invoke "String.equals(Object)" because "answer" is null
        // Komunikat „because "answer" is null” to helpful NPE (Java 14+) = pomocny komunikat: mówi, CO było null.

        // DOBRA PRAKTYKA: stała po lewej stronie — "tak".equals(zmienna). Dla null zwróci false zamiast wyjątku.
    }

    // =================================================================================================
    // 5. ŁĄCZENIE WARUNKÓW A ZAGNIEŻDŻANIE
    // =================================================================================================

    /**
     * 5. Dwa if-y jeden w drugim (bez else) można zastąpić jednym if z operatorem {@code &&}.
     * Zagnieżdżanie ma sens, gdy każdy poziom ma WŁASNĄ gałąź else z innym komunikatem.
     */
    static void combiningConditions() {
        section("5. Łączenie warunków (&&, ||) zamiast zagnieżdżania");

        int age = 20;              // age = wiek
        boolean hasTicket = true;  // has ticket = ma bilet

        // Wersja zagnieżdżona — dwa poziomy wcięć, a logika to po prostu „oba warunki naraz”.
        if (age >= 18) {
            if (hasTicket) {
                System.out.println("Wejście: TAK (wersja zagnieżdżona)");
            }
        }
        // WYNIK: Wejście: TAK (wersja zagnieżdżona)

        // Wersja połączona — czytamy jak zdanie: „pełnoletni I ma bilet”.
        if (age >= 18 && hasTicket) {
            System.out.println("Wejście: TAK (wersja z &&)");
        }
        // WYNIK: Wejście: TAK (wersja z &&)

        // Kiedy zagnieżdżać? Gdy każdy poziom mówi COŚ INNEGO w swoim else:
        int visitorAge = 16;          // visitor age = wiek odwiedzającego
        boolean visitorHasTicket = false;
        if (visitorAge >= 18) {
            if (visitorHasTicket) {
                System.out.println("Zapraszamy!");
            } else {
                System.out.println("Kup bilet w kasie");
            }
        } else {
            System.out.println("Wstęp tylko dla pełnoletnich");
        }
        // WYNIK: Wstęp tylko dla pełnoletnich

        // Leniwość (short-circuit) && chroni przed NullPointerException:
        String name = null; // name = nazwa
        boolean longName = name != null && name.length() > 3; // length = długość; prawa strona się NIE wykona
        show("name != null && name.length() > 3", longName);
        // WYNIK: name != null && name.length() > 3 → false

        // PUŁAPKA: pojedynczy & (bez skracania) liczy OBIE strony — także name.length() na null.
        expectThrows("& zamiast && przy name == null", () -> {
            boolean check = name != null & name.length() > 3; // check = sprawdzenie
            System.out.println(check);
        });
        // WYNIK: ✔ & zamiast && przy name == null → rzucono NullPointerException: Cannot invoke "String.length()" because "name" is null

        // DOBRA PRAKTYKA: w warunkach logicznych używaj && i ||. Najpierw tani/zabezpieczający warunek
        // (name != null), potem ten, który bez niego by się wysypał.
    }

    // =================================================================================================
    // 6. KLAUZULE STRAŻNIKA (WCZESNY RETURN)
    // =================================================================================================

    /**
     * 6. Klauzula strażnika (guard clause): najpierw odrzucamy złe przypadki i od razu robimy return.
     * Zamiast „piramidy” zagnieżdżonych if-ów dostajemy płaską listę warunków.
     */
    static void guardClauses() {
        section("6. Klauzule strażnika — PRZED i PO");

        // PRZED: checkoutNested — trzy poziomy zagnieżdżenia, a komunikat błędu daleko od swojego warunku.
        show("zagnieżdżone, niezalogowany", checkoutNested(false, 2, true));
        // WYNIK: zagnieżdżone, niezalogowany → Zaloguj się
        show("zagnieżdżone, pusty koszyk", checkoutNested(true, 0, true));
        // WYNIK: zagnieżdżone, pusty koszyk → Koszyk jest pusty

        // PO: checkoutGuards — ta sama logika, każdy warunek w jednej linii i od razu z odpowiedzią.
        show("strażnicy, pusty koszyk", checkoutGuards(true, 0, true));
        // WYNIK: strażnicy, pusty koszyk → Koszyk jest pusty
        show("strażnicy, płatność odrzucona", checkoutGuards(true, 3, false));
        // WYNIK: strażnicy, płatność odrzucona → Błąd płatności
        show("strażnicy, wszystko dobrze", checkoutGuards(true, 3, true));
        // WYNIK: strażnicy, wszystko dobrze → Zamówienie złożone

        // DOBRA PRAKTYKA: „szczęśliwa ścieżka” (happy path = główny, poprawny przebieg) na końcu metody,
        // bez wcięć. Błędy obsłużone na początku — czytelnik od razu widzi, co może pójść nie tak.
        // Strażnicy mają zanegowane warunki (!loggedIn, itemsInCart <= 0) — pilnuj, żeby negacja była poprawna.
    }

    /** PRZED: piramida zagnieżdżonych if-ów. */
    static String checkoutNested(boolean loggedIn, int itemsInCart, boolean paymentOk) {
        String result; // result = wynik
        if (loggedIn) {                         // logged in = zalogowany
            if (itemsInCart > 0) {              // items in cart = produkty w koszyku
                if (paymentOk) {                // payment ok = płatność poprawna
                    result = "Zamówienie złożone";
                } else {
                    result = "Błąd płatności";
                }
            } else {
                result = "Koszyk jest pusty";
            }
        } else {
            result = "Zaloguj się";
        }
        return result;
    }

    /** PO: klauzule strażnika — płasko i czytelnie. */
    static String checkoutGuards(boolean loggedIn, int itemsInCart, boolean paymentOk) {
        if (!loggedIn) {
            return "Zaloguj się";
        }
        if (itemsInCart <= 0) {
            return "Koszyk jest pusty";
        }
        if (!paymentOk) {
            return "Błąd płatności";
        }
        return "Zamówienie złożone";
    }

    // =================================================================================================
    // 7. ZMIENNE BOOLEAN (FLAGI)
    // =================================================================================================

    /**
     * 7. Zmienne boolean nazywamy jak pytanie tak/nie: isAdult, hasTicket, canVote.
     * Wynik porównania to już boolean — nie trzeba pisać if, żeby go „przepisać”.
     */
    static void booleanFlags() {
        section("7. Flagi boolean: isX / hasX");

        int age = 17;
        boolean isAdult = age >= 18;              // is adult = jest pełnoletni; porównanie DAJE boolean
        boolean hasParentConsent = true;          // has parent consent = ma zgodę rodzica
        boolean canSignUp = isAdult || hasParentConsent; // can sign up = może się zapisać
        show("isAdult", isAdult);
        // WYNIK: isAdult → false
        show("canSignUp", canSignUp);
        // WYNIK: canSignUp → true

        // Zapach w kodzie (code smell = coś, co działa, ale źle wygląda):
        //   if (isAdult == true) { ... }      →   if (isAdult) { ... }
        //   if (isAdult == false) { ... }     →   if (!isAdult) { ... }
        //   if (age >= 18) {                  →   return age >= 18;
        //       return true;
        //   } else {
        //       return false;
        //   }
        show("isEven(10)", isEven(10)); // is even = czy parzysta
        // WYNIK: isEven(10) → true
        show("isEven(7)", isEven(7));
        // WYNIK: isEven(7) → false

        // DOBRA PRAKTYKA: nazwy POZYTYWNE. „!isNotEmpty” to podwójne przeczenie — mózg się plącze.
        // Lepiej isEmpty i ewentualnie !isEmpty.
    }

    /** Zwraca wynik porównania bezpośrednio — bez if / else. */
    static boolean isEven(int number) {
        return number % 2 == 0;
    }

    // =================================================================================================
    // 8. OPERATOR TRÓJARGUMENTOWY ?: (POWTÓRKA)
    // =================================================================================================

    /**
     * 8. Operator {@code warunek ? wartośćGdyTrue : wartośćGdyFalse} to „if, który zwraca wartość”.
     * Świetny do krótkich wyborów, fatalny, gdy się go zagnieżdża.
     */
    static void ternaryRecap() {
        section("8. Operator ?: — krótki wybór wartości");

        int itemsCount = 1; // items count = liczba produktów
        String word = itemsCount == 1 ? "produkt" : "produkty";
        show("słowo dla 1", word);
        // WYNIK: słowo dla 1 → produkt

        int a = 7;
        int b = 12;
        int bigger = a > b ? a : b; // bigger = większa
        show("większa z 7 i 12", bigger);
        // WYNIK: większa z 7 i 12 → 12

        // Polska odmiana to już 3 przypadki (1 produkt, 2–4 produkty, 5+ produktów, ale 12–14 produktów).
        // Zagnieżdżony ?: działa, ale czyta się go fatalnie:
        int n = 22;
        String nested = n == 1 ? "produkt"
                : (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 12 || n % 100 > 14)) ? "produkty" : "produktów";
        show("22 (zagnieżdżony ?:)", nested);
        // WYNIK: 22 (zagnieżdżony ?:) → produkty

        // Ten sam wybór jako metoda z if / else if — każdy przypadek w osobnej linii:
        System.out.println(productsLabel(1) + ", " + productsLabel(3) + ", " + productsLabel(5) + ", "
                + productsLabel(12) + ", " + productsLabel(22));
        // WYNIK: 1 produkt, 3 produkty, 5 produktów, 12 produktów, 22 produkty

        // PUŁAPKA: obie gałęzie ?: są sprowadzane do WSPÓLNEGO typu. int i double → double:
        System.out.println(true ? 1 : 2.0);
        // WYNIK: 1.0
        // Wybraliśmy „1”, a wypisało się „1.0”, bo typ całego wyrażenia to double.

        // DOBRA PRAKTYKA: ?: tylko dla prostego wyboru JEDNEJ wartości. Dwa i więcej warunków → if / else if.
    }

    /** Polska odmiana słowa „produkt” — czytelnie, bez zagnieżdżonego ?:. */
    static String productsLabel(int n) {
        int lastDigit = n % 10;   // last digit = ostatnia cyfra
        int lastTwo = n % 100;    // last two = dwie ostatnie cyfry
        if (n == 1) {
            return n + " produkt";
        } else if (lastDigit >= 2 && lastDigit <= 4 && (lastTwo < 12 || lastTwo > 14)) {
            return n + " produkty";
        } else {
            return n + " produktów";
        }
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • if (warunek) { ... } else if (inny) { ... } else { ... } — wykona się najwyżej JEDNA gałąź.
     *   • Warunek musi być typu boolean. „if (x)” dla int to błąd kompilacji.
     *   • = przypisuje, == porównuje. „if (flag = true)” kompiluje się i jest ZAWSZE true.
     *   • Kolejność else if: najpierw przypadki najwęższe / najwyższe progi, na końcu else.
     *   • ZAWSZE klamry { } — bez nich if obejmuje jedną instrukcję, else łączy się z najbliższym if.
     *   • Teksty: "stała".equals(zmienna) — nie ==; equalsIgnoreCase ignoruje wielkość liter.
     *   • && i || są leniwe: „x != null && x.length() > 0” jest bezpieczne; & i | liczą obie strony.
     *   • Klauzule strażnika: błędy na początku z return, szczęśliwa ścieżka na końcu bez wcięć.
     *   • Flagi: isX / hasX / canX; nie pisz „== true”; „return warunek;” zamiast if-return-true-else-false.
     *   • ?: tylko dla prostego wyboru jednej wartości; typ wyniku to wspólny typ obu gałęzi.
     *
     * PYTANIA KONTROLNE:
     *   1. Ile gałęzi wykona się w łańcuchu if / else if / else, gdy prawdziwe są DWA warunki?
     *   2. Co wypisze:  int x = 5;  if (x > 3) { System.out.print("A"); } else if (x > 1) { System.out.print("B"); } else { System.out.print("C"); }  ?
     *   3. ZNAJDŹ BŁĄD:  if (points >= 50) { ocena = 3; } else if (points >= 90) { ocena = 5; }
     *   4. ZNAJDŹ BŁĄD:  String cmd = scanner.nextLine();  if (cmd == "koniec") { ... }
     *   5. Co wypisze:  boolean ok = false;  if (ok = true) { System.out.println("TAK"); } else { System.out.println("NIE"); }  ?
     *   6. Do którego if należy else w kodzie bez klamer:  if (a) if (b) x(); else y();  ?
     *   7. Jak skrócić:  if (isValid == true) { return true; } else { return false; }  ?
     *   8. Co to jest klauzula strażnika i jaki problem rozwiązuje?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: znak liczb 5, -3, 0", "dodatnia, ujemna, zero",
                () -> exercise1(5) + ", " + exercise1(-3) + ", " + exercise1(0));
        Check.equal("ćw. 2: ceny biletów", "0, 15, 15, 25, 12",
                () -> exercise2(2, false) + ", " + exercise2(10, false) + ", " + exercise2(20, true) + ", "
                        + exercise2(30, false) + ", " + exercise2(70, true));
        Check.equal("ćw. 3: wypłata z bankomatu", "Wypłacono 200 zł | Brak środków | Niepoprawna kwota | Karta zablokowana",
                () -> exercise3(500, 200, false) + " | " + exercise3(500, 900, false) + " | "
                        + exercise3(500, -5, false) + " | " + exercise3(500, 200, true));
        Check.equal("ćw. 4: dni w miesiącu", "31, 30, 28, 29, 28, 29, -1",
                () -> exercise4(1, 2026) + ", " + exercise4(4, 2026) + ", " + exercise4(2, 2026) + ", "
                        + exercise4(2, 2024) + ", " + exercise4(2, 1900) + ", " + exercise4(2, 2000) + ", "
                        + exercise4(13, 2026));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "dodatnia, ujemna, zero",
                () -> solution1(5) + ", " + solution1(-3) + ", " + solution1(0));
        Check.equal("ćw. 2 (wzorzec)", "0, 15, 15, 25, 12",
                () -> solution2(2, false) + ", " + solution2(10, false) + ", " + solution2(20, true) + ", "
                        + solution2(30, false) + ", " + solution2(70, true));
        Check.equal("ćw. 3 (wzorzec)", "Wypłacono 200 zł | Brak środków | Niepoprawna kwota | Karta zablokowana",
                () -> solution3(500, 200, false) + " | " + solution3(500, 900, false) + " | "
                        + solution3(500, -5, false) + " | " + solution3(500, 200, true));
        Check.equal("ćw. 4 (wzorzec)", "31, 30, 28, 29, 28, 29, -1",
                () -> solution4(1, 2026) + ", " + solution4(4, 2026) + ", " + solution4(2, 2026) + ", "
                        + solution4(2, 2024) + ", " + solution4(2, 1900) + ", " + solution4(2, 2000) + ", "
                        + solution4(13, 2026));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć "dodatnia", "ujemna" albo "zero" w zależności od znaku liczby.
     * Podpowiedź: if / else if / else — trzy gałęzie, trzy return-y.
     */
    static String exercise1(int number) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 2 (średnie): cena biletu do kina w zł.
     * Dzieci poniżej 3 lat — 0; seniorzy od 65 lat — 12; uczniowie/studenci (isStudent) lub osoby
     * poniżej 18 lat — 15; pozostali — 25. Senior-student płaci 12 (bierzemy tańszą zniżkę).
     * Podpowiedź: ułóż warunki od najtańszego biletu — pierwszy pasujący wygrywa.
     */
    static int exercise2(int age, boolean isStudent) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ piramidę if-ów na klauzule strażnika (ten sam wynik, bez zagnieżdżeń).
     * <pre>{@code
     * if (!cardBlocked) {
     *     if (amount > 0) {
     *         if (amount <= balance) {
     *             return "Wypłacono " + amount + " zł";
     *         } else {
     *             return "Brak środków";
     *         }
     *     } else {
     *         return "Niepoprawna kwota";
     *     }
     * } else {
     *     return "Karta zablokowana";
     * }
     * }</pre>
     * Podpowiedź: zaneguj każdy warunek (cardBlocked, amount mniejsze lub równe 0, amount większe niż balance)
     * i zrób od razu return z komunikatem błędu. Na końcu — szczęśliwa ścieżka.
     */
    static String exercise3(int balance, int amount, boolean cardBlocked) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): liczba dni w miesiącu (1–12) danego roku; dla złego numeru miesiąca zwróć -1.
     * Rok przestępny: podzielny przez 4 i NIE podzielny przez 100 — albo podzielny przez 400
     * (2024 i 2000 są przestępne, 1900 i 2026 nie).
     * Podpowiedź: najpierw strażnik dla złego miesiąca, potem luty, potem miesiące 30-dniowe (4, 6, 9, 11).
     */
    static int exercise4(int month, int year) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(int number) {
        if (number > 0) {
            return "dodatnia";
        } else if (number < 0) {
            return "ujemna";
        } else {
            return "zero";
        }
    }

    static int solution2(int age, boolean isStudent) {
        if (age < 3) {
            return 0;
        } else if (age >= 65) {
            return 12;
        } else if (isStudent || age < 18) {
            return 15;
        } else {
            return 25;
        }
    }

    static String solution3(int balance, int amount, boolean cardBlocked) {
        if (cardBlocked) {
            return "Karta zablokowana";
        }
        if (amount <= 0) {
            return "Niepoprawna kwota";
        }
        if (amount > balance) {
            return "Brak środków";
        }
        return "Wypłacono " + amount + " zł";
    }

    static int solution4(int month, int year) {
        if (month < 1 || month > 12) {
            return -1;
        }
        if (month == 2) {
            boolean isLeap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0; // is leap = czy przestępny
            return isLeap ? 29 : 28;
        }
        if (month == 4 || month == 6 || month == 9 || month == 11) {
            return 30;
        }
        return 31;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Jedna — pierwsza, której warunek jest prawdziwy. Kolejne warunki nie są nawet sprawdzane.
     *   2. Tylko „A”. 5 > 3 jest true, więc else if (x > 1) nie jest już sprawdzany.
     *   3. Warunek >= 50 łapie też 90+, więc ocena 5 nigdy nie padnie. Odwróć kolejność: najpierw >= 90.
     *   4. == porównuje referencje (czy to ten sam obiekt), a tekst ze Scannera to nowy obiekt.
     *      Poprawnie: "koniec".equals(cmd).
     *   5. „TAK” — ok = true to przypisanie; wartością wyrażenia jest true. Pisz po prostu if (ok).
     *   6. Do if (b) — else łączy się z najbliższym wolnym if. Wcięcia nie mają znaczenia. Dlatego klamry!
     *   7. return isValid;  (porównanie z true i if / else są zbędne).
     *   8. To if na początku metody, który dla złego przypadku od razu robi return (albo rzuca wyjątek).
     *      Usuwa „piramidę” zagnieżdżeń, a komunikat błędu stoi tuż obok swojego warunku.
     */
    // </editor-fold>
}
