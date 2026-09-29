package t01_basics;

import helpers.Check;

import java.util.Locale;
import java.util.Scanner;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Wczytywanie danych — klasa Scanner
 *        (scanner = skaner, czytnik; input = wejście; token = pojedynczy „kawałek” tekstu, np. słowo albo liczba)
 *
 * W SKRÓCIE:
 *   Scanner czyta tekst kawałek po kawałku: nextLine() — całą linię, next() — jedno słowo, nextInt() — liczbę.
 *   W prawdziwym programie czyta z klawiatury: new Scanner(System.in). W tej lekcji czyta z NAPISU
 *   (new Scanner("25\nJan")), żeby dało się ją uruchomić bez wpisywania czegokolwiek — działa identycznie.
 *   Największe pułapki: nextInt() + nextLine() („zjedzona” linia), zły format liczby i przecinek/kropka (Locale).
 *
 * ANALOGIA: taśma z karteczkami.
 *   Wejście to taśma z napisami. nextInt() odrywa z taśmy jedną liczbę, ale ZOSTAWIA koniec linii (Enter).
 *   nextLine() odrywa wszystko do najbliższego Entera — więc jeśli został sam Enter, dostaniesz pustą karteczkę.
 *
 * JAK TO DZIAŁA:
 *   Scanner sc = new Scanner(System.in);   ← klawiatura (w lekcji: new Scanner("tekst"))
 *   int age = sc.nextInt();                ← liczba; zły tekst → InputMismatchException
 *   sc.nextLine();                         ← „zjedz” resztę linii po liczbie (Enter)
 *   String name = sc.nextLine();           ← cała linia
 *   sc.hasNextInt()                        ← czy NASTĘPNY kawałek jest liczbą (sprawdzenie bez pobierania)
 *
 * SŁÓWKA:
 *   scanner = skaner, czytnik; next = następny; nextLine = następna linia; nextInt = następna liczba całkowita;
 *   hasNext = czy jest następny; token = kawałek tekstu; delimiter = separator; input mismatch = niezgodne dane;
 *   locale = ustawienia regionalne; close = zamknij; valid = poprawny.
 *
 * ZOBACZ TEŻ: t01_basics/Basics11ConsoleOutput (wypisywanie), t01_basics/Basics06Wrappers (Integer.parseInt),
 *             t10_exceptions/Exceptions01Basics (wyjątki), t18_io_files/Io02ReadingText (czytanie z plików).
 * </pre>
 */
public class Basics10ScannerInput {

    /** POLISH = polskie ustawienia regionalne (przecinek dziesiętny). */
    private static final Locale POLISH = Locale.forLanguageTag("pl-PL");

    public static void main(String[] args) {
        title("Basics10 — Scanner (wczytywanie danych)");

        basics();                   // basics = podstawy
        nextVsNextLine();           // next vs nextLine = słowo kontra linia
        nextIntThenNextLine();      // nextInt then nextLine = pułapka „zjedzonej” linii
        badInput();                 // bad input = złe dane
        validationLoop();           // validation loop = pętla sprawdzająca
        readUntilEnd();             // read until end = czytaj do końca
        decimalSeparator();         // decimal separator = separator dziesiętny
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PODSTAWY
    // =================================================================================================

    /**
     * 1. Scanner z napisu działa jak z klawiatury — „\n” to naciśnięty Enter.
     * try-with-resources (try (...)) automatycznie zamyka Scanner na końcu (dokładnie: t10_exceptions).
     */
    static void basics() {
        section("1. Podstawy: nextLine, nextInt");

        try (Scanner sc = new Scanner("Anna\n30\n")) {
            String name = sc.nextLine();         // nextLine = cała linia: "Anna"
            int age = sc.nextInt();              // nextInt = liczba: 30
            show("imię", name);
            show("wiek za rok", age + 1);
        }
        // WYNIK: imię → Anna
        // WYNIK: wiek za rok → 31

        // W prawdziwym programie:
        //   Scanner sc = new Scanner(System.in);
        //   System.out.print("Podaj imię: ");
        //   String name = sc.nextLine();
        // PUŁAPKA: Scannera na System.in NIE zamykaj w środku programu — sc.close() zamyka też System.in
        //   i kolejny Scanner już nic nie wczyta. Twórz jeden Scanner na cały program.
    }

    // =================================================================================================
    // 2. next() KONTRA nextLine()
    // =================================================================================================

    /** 2. next() czyta jedno SŁOWO (do spacji), nextLine() — CAŁĄ linię (do Entera). */
    static void nextVsNextLine() {
        section("2. next() — słowo, nextLine() — linia");

        try (Scanner sc = new Scanner("Jan Kowalski\nJan Kowalski\n")) {
            show("next()", sc.next());
            sc.nextLine();                               // dokończ pierwszą linię (zostało " Kowalski")
            show("nextLine()", sc.nextLine());
        }
        // WYNIK: next() → Jan
        // WYNIK: nextLine() → Jan Kowalski
    }

    // =================================================================================================
    // 3. PUŁAPKA: nextInt() + nextLine()
    // =================================================================================================

    /**
     * 3. Najsłynniejsza pułapka Scannera: nextInt() czyta liczbę, ale ZOSTAWIA Enter. Następne nextLine() czyta
     * „resztę linii” — czyli pusty napis — zamiast kolejnej linii.
     */
    static void nextIntThenNextLine() {
        section("3. Pułapka: nextInt() zostawia Enter");

        try (Scanner sc = new Scanner("25\nJan Kowalski\n")) {
            int age = sc.nextInt();
            String name = sc.nextLine();                 // ŹLE: dostajemy pustą resztę linii z "25"
            show("wiek", age);
            show("imię (źle)", "[" + name + "]");
        }
        // WYNIK: wiek → 25
        // WYNIK: imię (źle) → []    ← pusty napis!

        try (Scanner sc = new Scanner("25\nJan Kowalski\n")) {
            int age = sc.nextInt();
            sc.nextLine();                               // DOBRZE: „zjedz” Enter po liczbie
            String name = sc.nextLine();
            show("imię (dobrze)", "[" + name + "]");
            show("wiek", age);
        }
        // WYNIK: imię (dobrze) → [Jan Kowalski]
        // WYNIK: wiek → 25

        // DOBRA PRAKTYKA: czytaj ZAWSZE całe linie (nextLine) i sam zamieniaj je na liczby: Integer.parseInt(line.trim()).
        //   Wtedy tej pułapki w ogóle nie ma.
    }

    // =================================================================================================
    // 4. ZŁE DANE
    // =================================================================================================

    /** 4. Gdy użytkownik wpisze tekst zamiast liczby, nextInt() rzuca InputMismatchException (niezgodne dane). */
    static void badInput() {
        section("4. Złe dane → InputMismatchException");

        expectThrows("nextInt() na \"abc\"", () -> {
            try (Scanner sc = new Scanner("abc\n")) {
                sc.nextInt();
            }
        });
        // WYNIK: ✔ nextInt() na "abc" → rzucono InputMismatchException: (brak komunikatu)

        // PUŁAPKA: po takim wyjątku błędny tekst ZOSTAJE na wejściu. Kolejne nextInt() znowu go zobaczy i znowu rzuci
        //   wyjątek — nieskończona pętla. Zły kawałek trzeba zdjąć: sc.next() albo sc.nextLine() (sekcja 5).
        // PUŁAPKA: łapiąc błąd, łap InputMismatchException — NIE NumberFormatException (tę rzuca Integer.parseInt).
    }

    // =================================================================================================
    // 5. PĘTLA SPRAWDZAJĄCA DANE
    // =================================================================================================

    /**
     * 5. Poprawny sposób: najpierw sprawdź hasNextInt() (czy następny kawałek to liczba), a jeśli nie — zdejmij
     * zły kawałek i zapytaj ponownie. Tu symulujemy użytkownika, który wpisuje: "abc", potem "-5", potem "42".
     */
    static void validationLoop() {
        section("5. Pętla: pytaj, dopóki dane nie są poprawne");

        try (Scanner sc = new Scanner("abc\n-5\n42\n")) {
            int age = -1;
            while (age < 0) {
                if (sc.hasNextInt()) {                   // hasNextInt = czy następny kawałek jest liczbą
                    age = sc.nextInt();
                    if (age < 0) {
                        System.out.println("   wiek nie może być ujemny: " + age);
                    }
                } else {
                    System.out.println("   to nie jest liczba: " + sc.next());   // next() zdejmuje zły kawałek
                }
            }
            show("poprawny wiek", age);
        }
        // WYNIK: to nie jest liczba: abc
        // WYNIK: wiek nie może być ujemny: -5
        // WYNIK: poprawny wiek → 42
    }

    // =================================================================================================
    // 6. CZYTANIE DO KOŃCA DANYCH
    // =================================================================================================

    /** 6. hasNextInt() / hasNext() w pętli while — czytaj, dopóki są dane (np. wszystkie liczby z linii). */
    static void readUntilEnd() {
        section("6. Czytaj, dopóki są liczby");

        int sum = 0;
        int count = 0;
        try (Scanner sc = new Scanner("4 8 15 16 23 42")) {
            while (sc.hasNextInt()) {
                sum += sc.nextInt();
                count++;
            }
        }
        show("liczb / suma", count + " / " + sum);
        // WYNIK: liczb / suma → 6 / 108
    }

    // =================================================================================================
    // 7. PRZECINEK CZY KROPKA — Locale
    // =================================================================================================

    /**
     * 7. nextDouble() czyta liczbę według ustawień regionalnych (Locale). Na polskim komputerze oczekuje
     * PRZECINKA („3,5”), na angielskim — KROPKI („3.5”). useLocale ustawia to jawnie.
     */
    static void decimalSeparator() {
        section("7. nextDouble: przecinek czy kropka (Locale)");

        try (Scanner sc = new Scanner("3,5").useLocale(POLISH)) {
            show("\"3,5\" po polsku", sc.nextDouble());
        }
        try (Scanner sc = new Scanner("3.5").useLocale(Locale.ROOT)) {
            show("\"3.5\" z Locale.ROOT", sc.nextDouble());
        }
        // WYNIK: "3,5" po polsku → 3.5
        // WYNIK: "3.5" z Locale.ROOT → 3.5

        expectThrows("\"3.5\" po polsku", () -> {
            try (Scanner sc = new Scanner("3.5").useLocale(POLISH)) {
                sc.nextDouble();
            }
        });
        // WYNIK: ✔ "3.5" po polsku → rzucono InputMismatchException: (brak komunikatu)

        // PUŁAPKA: ten sam program działa u Ciebie (polski Windows, przecinek), a u kolegi z angielskim systemem
        //   wysypuje się na tych samych danych. Gdy format jest ustalony (np. plik), ustaw Locale jawnie.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • new Scanner(System.in) — klawiatura; new Scanner("tekst") — napis (do testów). Jeden Scanner na System.in.
     *   • nextLine() — cała linia; next() — słowo; nextInt()/nextDouble() — liczba.
     *   • nextInt() zostawia Enter → przed nextLine() zrób dodatkowe sc.nextLine(). Lepiej: czytaj linie + parseInt.
     *   • Zły tekst → InputMismatchException; zły kawałek zostaje na wejściu — zdejmij go next()/nextLine().
     *   • hasNextInt() — sprawdź, zanim pobierzesz; pętla while do walidacji.
     *   • nextDouble zależy od Locale (przecinek/kropka) — useLocale(...).
     *   • Nie zamykaj Scannera na System.in w trakcie programu (zamyka System.in).
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się next() od nextLine()?
     *   2. Co wczyta  name  dla wejścia "7\nOla\n":  int n = sc.nextInt(); String name = sc.nextLine();  ?
     *   3. ZNAJDŹ BŁĄD:  while (true) { try { age = sc.nextInt(); break; } catch (InputMismatchException e) { System.out.println("Błąd"); } }
     *   4. Po co jest hasNextInt()?
     *   5. Dlaczego nextDouble() na tych samych danych działa u Ciebie, a u kogoś innego rzuca wyjątek?
     *   6. Dlaczego nie należy wywoływać close() na Scannerze czytającym z System.in w środku programu?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: suma dwóch liczb z \"3 4\"", 7, () -> exercise1("3 4"));
        Check.equal("ćw. 2: suma liczb, pomijając tekst", 60, () -> exercise2("10 20 abc 30 x"));
        Check.equal("ćw. 3: imię i wiek z dwóch linii", "Jan Kowalski (25 lat)", () -> exercise3("Jan Kowalski\n25\n"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 7, () -> solution1("3 4"));
        Check.equal("ćw. 2 (wzorzec)", 60, () -> solution2("10 20 abc 30 x"));
        Check.equal("ćw. 3 (wzorzec)", "Jan Kowalski (25 lat)", () -> solution3("Jan Kowalski\n25\n"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 3 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): z tekstu z dwiema liczbami (np. "3 4") zwróć ich sumę.
     * Podpowiedź: {@code try (Scanner sc = new Scanner(input)) { return sc.nextInt() + sc.nextInt(); }}
     */
    static int exercise1(String input) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (średnie): zsumuj wszystkie liczby całkowite z tekstu, POMIJAJĄC kawałki, które nie są liczbami.
     * "10 20 abc 30 x" → 60.
     * Podpowiedź: {@code while (sc.hasNext())} — jeśli {@code sc.hasNextInt()}, dodaj {@code sc.nextInt()},
     * w przeciwnym razie zdejmij kawałek {@code sc.next()}.
     */
    static int exercise2(String input) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): wejście ma w pierwszej linii imię i nazwisko, w drugiej wiek. Zwróć tekst
     * „IMIĘ NAZWISKO (WIEK lat)”, np. "Jan Kowalski (25 lat)".
     * Podpowiedź: najpierw nextLine() (imię ze spacją!), potem nextInt(). Tutaj kolejność „linia, potem liczba”
     * nie wpada w pułapkę z sekcji 3 — zastanów się dlaczego.
     */
    static String exercise3(String input) {
        // TODO: twoje rozwiązanie
        return "";
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(String input) {
        try (Scanner sc = new Scanner(input)) {
            return sc.nextInt() + sc.nextInt();
        }
    }

    static int solution2(String input) {
        int sum = 0;
        try (Scanner sc = new Scanner(input)) {
            while (sc.hasNext()) {
                if (sc.hasNextInt()) {
                    sum += sc.nextInt();
                } else {
                    sc.next();                 // zdejmij kawałek, który nie jest liczbą
                }
            }
        }
        return sum;
    }

    static String solution3(String input) {
        try (Scanner sc = new Scanner(input)) {
            String name = sc.nextLine();       // nextLine zabiera też Enter, więc nextInt zaczyna od nowej linii
            int age = sc.nextInt();
            return name + " (" + age + " lat)";
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. next() czyta jedno słowo (do spacji/Entera), nextLine() — wszystko do końca linii (także spacje).
     *   2. Pusty napis "" — nextInt() zostawił Enter po 7, a nextLine() przeczytał tylko resztę tej linii.
     *   3. Nieskończona pętla: po wyjątku błędny tekst zostaje na wejściu, więc nextInt() rzuca w kółko.
     *      W catch trzeba zdjąć zły kawałek: sc.nextLine() (albo sc.next()).
     *   4. Pozwala SPRAWDZIĆ, czy następny kawałek jest liczbą, zanim go pobierzesz — bez wyjątku.
     *   5. Bo nextDouble() używa domyślnych ustawień regionalnych komputera: polski system oczekuje przecinka,
     *      angielski — kropki. Rozwiązanie: sc.useLocale(...).
     *   6. Bo close() zamyka też System.in — każdy późniejszy Scanner na System.in już nic nie wczyta.
     */
    // </editor-fold>
}
