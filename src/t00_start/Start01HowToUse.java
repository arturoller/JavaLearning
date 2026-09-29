package t00_start;

import helpers.Check;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Jak korzystać z kursu JavaLearning
 *        (how to use = jak używać)
 *
 * W SKRÓCIE:
 *   Każdy plik to jedna lekcja: czytasz komentarze od góry, uruchamiasz main() (zielony trójkąt ▶ obok),
 *   porównujesz wydruk z komentarzami „WYNIK:”, a na końcu sprawdzasz się pytaniami i ćwiczeniami.
 *
 * ANALOGIA:
 *   Lekcja jest jak rozdział podręcznika z zeszytem ćwiczeń: teoria (komentarze) + przykłady (kod)
 *   + sprawdzian (PYTANIA KONTROLNE, ćwiczenia) + karteczka do powtórki (ŚCIĄGA).
 *
 * JAK TO DZIAŁA:
 *   Budowa każdej lekcji (zawsze w tej kolejności):
 *   1. Nagłówek (ten komentarz nad klasą): TEMAT, W SKRÓCIE, ANALOGIA, JAK TO DZIAŁA, SŁÓWKA, ZOBACZ TEŻ.
 *   2. main() — wywołuje po kolei metody-sekcje. Każda sekcja = jedno zagadnienie, od łatwego do trudnego.
 *   3. W sekcjach: kod + komentarze + „WYNIK:” (co zobaczysz) + „PUŁAPKA:” / „DOBRA PRAKTYKA:”.
 *   4. ŚCIĄGA — podsumowanie całej lekcji na jeden ekran (do szybkiej powtórki).
 *   5. PYTANIA KONTROLNE — pytania do samosprawdzenia, także „co wypisze ten kod?”.
 *   6. ĆWICZENIA — metody exerciseN() z „TODO” pokazują ✘, dopóki ich nie rozwiążesz.
 *   7. Na samym końcu, w ZWINIĘTYCH blokach: rozwiązania wzorcowe i odpowiedzi na pytania.
 *      W IntelliJ zwinięty blok wygląda jak jedna szara linia „ROZWIĄZANIA...” — kliknij „+” obok, żeby rozwinąć.
 *      Zaglądaj tam dopiero PO własnej próbie — wtedy nauka działa najlepiej.
 *
 * TAGI — wpisz w IntelliJ Ctrl+Shift+F (szukaj w całym projekcie), żeby zobaczyć wszystkie wystąpienia:
 *   TEMAT:              — nagłówki wszystkich lekcji (lista tematów)
 *   PUŁAPKA:            — wszystkie typowe błędy w jednym miejscu
 *   DOBRA PRAKTYKA:     — wszystkie zasady „jak robić dobrze”
 *   ŚCIĄGA:             — podsumowania do powtórek
 *   PYTANIA KONTROLNE:  — wszystkie pytania do samosprawdzenia
 *   ĆWICZENIE           — wszystkie zadania (bez dwukropka — bo zadania mają numery: „ĆWICZENIE 1:”)
 *   SŁÓWKA:             — angielskie słówka z tłumaczeniem
 *   ANALOGIA:           — porównania z życia codziennego
 *   ZOBACZ TEŻ:         — powiązania między lekcjami
 *   Tagi zawsze mają DOKŁADNIE tę postać (wielkie litery + dwukropek), więc wyszukiwanie znajdzie wszystko.
 *
 * SKRÓTY W INTELLIJ:
 *   Ctrl+N        — szukaj KLASY po nazwie; np. „Streams” → wszystkie lekcje o streamach po kolei
 *   Ctrl+Shift+N  — szukaj PLIKU po nazwie (także README.md, package-info.java)
 *   Ctrl+Shift+F  — szukaj TEKSTU we wszystkich plikach (np. „groupingBy” albo „PUŁAPKA:”)
 *   Ctrl+F12      — lista metod w bieżącym pliku (szybki skok do sekcji)
 *   Ctrl+klik     — przejdź do definicji (np. klik na SampleData.products() otwiera tę metodę)
 *   Ctrl+Q        — pokaż dokumentację (opis nad klasą/metodą) elementu pod kursorem
 *   Ctrl+Shift+—/+ (minus/plus na klawiaturze numerycznej) — zwiń/rozwiń wszystkie bloki w pliku
 *
 * NAZWY PAKIETÓW I KLAS:
 *   Pakiety mają numery t00_, t01_, ... — w drzewie projektu układają się w kolejności kursu.
 *   Klasy: temat + numer + zagadnienie, np. Streams11GroupingBy = streamy, lekcja 11, groupingBy.
 *   Nazwy w kodzie są po angielsku (taki jest standard w programowaniu), ale przy pierwszym użyciu
 *   w pliku jest tłumaczenie, np.  // filter = filtruj
 *
 * PUŁAPKA: IntelliJ przed uruchomieniem kompiluje CAŁY moduł (wszystkie pliki w src/), nie tylko jedną lekcję.
 *   Jeśli GDZIEKOLWIEK w projekcie jest błąd kompilacji, żadna klasa się nie uruchomi.
 *   Rozwiązanie: popraw błąd (IntelliJ pokaże go w oknie „Build”) i uruchom ponownie.
 *
 * SŁÓWKA:
 *   run = uruchom; build = zbuduj (skompiluj); compile = kompilować (zamienić .java na .class);
 *   main = główny; section = sekcja; label = etykieta; expected = oczekiwany; actual = faktyczny;
 *   TODO („to do”) = do zrobienia; helper = pomocnik; sample data = dane przykładowe; exercise = ćwiczenie;
 *   solution = rozwiązanie; editor fold = zwinięcie fragmentu w edytorze.
 *
 * ZOBACZ TEŻ: Start02Glossary (słowniczek), Start03LearningPath (kolejność nauki i powtórki),
 *             Start04ReviewTracker (co dziś powtórzyć), helpers/Console, helpers/Check.
 * </pre>
 */
public class Start01HowToUse {

    /**
     * main = główna metoda programu (punkt startowy). Java zaczyna wykonywanie programu właśnie od niej.
     * {@code String[] args} = argumenty z linii poleceń (arguments = argumenty) — w tym kursie ich nie używamy.
     */
    public static void main(String[] args) {
        title("Start01 — jak korzystać z kursu");

        showHowOutputLooks();      // show how output looks = pokaż, jak wygląda wydruk
        showHowPitfallsAreShown(); // show how pitfalls are shown = pokaż, jak pokazywane są pułapki
        exercises();               // exercises = ćwiczenia
    }

    /**
     * 1. Jak wygląda wydruk lekcji.
     * Metody title/section/show/note pochodzą z helpers/Console (import static na górze pliku).
     */
    static void showHowOutputLooks() {
        section("1. Jak wygląda wydruk");

        show("etykieta", "wartość");          // show = pokaż
        // WYNIK: etykieta → wartość

        show("2 + 2", 2 + 2);
        // WYNIK: 2 + 2 → 4

        note("Tak wygląda notatka — dodatkowe wyjaśnienie w wydruku");   // note = notatka
        // WYNIK: ℹ Tak wygląda notatka — dodatkowe wyjaśnienie w wydruku

        // System.out.println (system → wyjście → wypisz linię) — „surowe” wypisanie tekstu na konsolę.
        // Console.show robi to samo, tylko dodaje etykietę i strzałkę. Szczegóły: t01_basics/Basics11ConsoleOutput.
        System.out.println("zwykły println");
        // WYNIK: zwykły println
    }

    /**
     * 2. Jak lekcje pokazują pułapki, nie przerywając programu.
     * <p>
     * expectThrows (oczekuj wyjątku) uruchamia kod, który MA rzucić wyjątek. Łapie go i wypisuje ✔ z nazwą wyjątku.
     * Bez tego pierwszy błąd zatrzymałby cały program i nie zobaczyłbyś dalszych sekcji.
     * <p>
     * Zapis {@code () -> ...} to LAMBDA — „kawałek kodu przekazany jak wartość”. Na razie wystarczy wiedzieć,
     * że expectThrows dostaje kod do uruchomienia. Wszystko o lambdach: t13_lambdas.
     */
    static void showHowPitfallsAreShown() {
        section("2. Jak pokazujemy pułapki (expectThrows)");

        expectThrows("dzielenie liczby całkowitej przez zero", () -> {
            int zero = 0;
            System.out.println(10 / zero);
        });
        // WYNIK: ✔ dzielenie liczby całkowitej przez zero → rzucono ArithmeticException: / by zero

        // parseInt = przetwórz (tekst) na int
        expectThrows("zamiana tekstu 'abc' na liczbę", () -> Integer.parseInt("abc"));
        // WYNIK: ✔ zamiana tekstu 'abc' na liczbę → rzucono NumberFormatException: For input string: "abc"
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Lekcja = nagłówek → sekcje w main() → ŚCIĄGA → PYTANIA KONTROLNE → ĆWICZENIA → (zwinięte) rozwiązania i odpowiedzi.
     *   • Przed uruchomieniem przewidź wynik, potem uruchom ▶ i porównaj z „WYNIK:”.
     *   • Ctrl+Shift+F „PUŁAPKA:” = wszystkie pułapki; Ctrl+N „Streams” = wszystkie lekcje o streamach.
     *   • exerciseN() = Twoje zadanie (✘ dopóki nie rozwiążesz); rozwiązania są zwinięte na końcu pliku.
     *   • Błąd kompilacji gdziekolwiek w src/ blokuje uruchomienie każdej klasy w IntelliJ.
     *
     * PYTANIA KONTROLNE:
     *   1. Jakim skrótem znajdziesz wszystkie pułapki opisane w kursie?
     *   2. Po co jest metoda expectThrows?
     *   3. Gdzie w pliku lekcji są rozwiązania ćwiczeń i dlaczego są zwinięte?
     *   4. Dlaczego pakiety mają numery t00_, t01_...?
     *   5. Dlaczego klasa może się nie uruchomić, choć sama nie ma błędów?
     *   6. Co wypisze:  show("wynik", 7 + 3);  ?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /**
     * Uruchamia ćwiczenia. Check.equal dostaje LAMBDĘ {@code () -> exercise1(...)} — dzięki temu, jeśli Twoje
     * rozwiązanie rzuci wyjątek, zobaczysz ✘ z nazwą wyjątku, a program będzie działał dalej.
     */
    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: suma 3 + 4", 7, () -> exercise1(3, 4));
        Check.equal("ćw. 2: napis powitalny", "Cześć, Ania!", () -> exercise2("Ania"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 7, () -> solution1(3, 4));
        Check.equal("ćw. 2 (wzorzec)", "Cześć, Ania!", () -> solution2("Ania"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 2 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1: zwróć sumę dwóch liczb a i b.
     * Podpowiedź: {@code return a + b;}  (tak, to naprawdę takie proste — chodzi o poznanie mechanizmu ćwiczeń).
     */
    static int exercise1(int a, int b) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2: zwróć powitanie w postaci „Cześć, IMIĘ!” — np. dla "Ania" ma być "Cześć, Ania!".
     * Podpowiedź: napisy łączymy operatorem +, np. {@code "a" + zmienna + "b"}.
     */
    static String exercise2(String name) {
        // TODO: twoje rozwiązanie
        return "";
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(int a, int b) {
        return a + b;
    }

    static String solution2(String name) {
        return "Cześć, " + name + "!";
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Ctrl+Shift+F i wpisz „PUŁAPKA:”.
     *   2. Uruchamia kod, który ma rzucić wyjątek, łapie go i wypisuje — program działa dalej.
     *   3. Na samym końcu pliku, w zwiniętych blokach — żeby nie podglądać przed własną próbą.
     *   4. Żeby drzewo projektu układało się w kolejności kursu.
     *   5. IntelliJ kompiluje cały moduł — błąd w innym pliku blokuje uruchomienie każdej klasy.
     *   6. wynik → 10
     */
    // </editor-fold>
}
