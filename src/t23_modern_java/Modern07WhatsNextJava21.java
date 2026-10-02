package t23_modern_java;

import helpers.Check;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Co dalej? Java 18–21 (i krótko 22–25) oraz jak się do tego przygotować
 *        (release notes = notatki wydania; upgrade = aktualizacja; preview = podgląd)
 *
 * W SKRÓCIE:
 *   Ta lekcja pokazuje, co przyniosły wydania po Javie 17 — i wszystko, co wymaga nowszego JDK, jest w komentarzach
 *   oznaczonych „(Java 21+)” itp., bo ten kurs kompilujemy w Javie 17. Obok każdej nowości stoi działający kod
 *   „na dziś” (Java 17). Dowiesz się też, jak czytać notatki wydań i JEP-y oraz jak bezpiecznie przechodzić
 *   z jednego wydania LTS na następne.
 *
 * ANALOGIA:
 *   Wydania Javy to rozkład jazdy pociągów: co pół roku odjeżdża pociąg (zwykłe wydanie), a co kilka lat „ekspres
 *   z długim wsparciem” (LTS). Możesz jechać każdym, ale większość firm przesiada się tylko na ekspresy. Funkcje „podglądowe”
 *   (preview) to przejażdżka próbna: wolno się przejechać, ale nie wolno na tym budować domu.
 *
 * JAK TO DZIAŁA:
 *   1. Co 6 miesięcy (marzec i wrzesień) wychodzi nowe wydanie. LTS (long-term support = długie wsparcie):
 *      8 (2014), 11 (2018), 17 (2021), 21 (2023), 25 (2025). Pozostałe są wspierane tylko do następnego wydania.
 *   2. Cykl życia funkcji: incubator (inkubator, osobny moduł) / preview (podgląd, włączany --enable-preview) →
 *      kolejne podglądy → wersja ostateczna (final). Podgląd może się zmienić lub zostać wycofany.
 *   3. Każda zmiana ma numer JEP (JDK Enhancement Proposal): openjdk.org/jeps/NNN. Wydanie zbiera listę JEP-ów.
 *   4. Aktualizacja: LTS → LTS, z testami, nowszą wersją narzędzi i bibliotek oraz flagą kompilatora {@code --release N}.
 *
 *   Skrót nowości (JEP w nawiasach; „final” = wersja ostateczna):
 *     Java 18  UTF-8 domyślnie (400), prosty serwer WWW jwebserver (408)
 *     Java 21  LTS: wzorce rekordów (440), switch ze wzorcami (441), wątki wirtualne (444), kolekcje sekwencyjne (431)
 *     Java 22  nienazwane zmienne _ (456)
 *     Java 24  Stream Gatherers (485)
 *     Java 25  LTS: kompaktowe pliki źródłowe i metody main instancji (512), elastyczne konstruktory (513)
 *
 * SŁÓWKA:
 *   release = wydanie; LTS = wydanie z długim wsparciem; preview = podgląd; incubator = inkubator; final = ostateczna;
 *   deprecated = przestarzałe; removal = usunięcie; virtual thread = wątek wirtualny; sequenced = uporządkowany
 *   w sekwencji; gatherer = zbieracz (operacja pośrednia strumienia); unnamed = nienazwany; template = szablon.
 *
 * ZOBACZ TEŻ: t18_io_files/Io12Charsets (kodowania znaków), t21_concurrency/Concurrency04Executors (pule wątków),
 *   t28_networking_http/Http03LocalServer (lokalny serwer HTTP), t23_modern_java/Modern05RecordsSealedPatterns
 *   (rekordy i zapieczętowane typy), t30_build_modules/Build01MavenBasics (budowanie i opcja --release)
 * </pre>
 */
public class Modern07WhatsNextJava21 {

    public static void main(String[] args) {
        title("Modern07 — co dalej: Java 18–21 i dalej");

        releaseRhythm();             // release rhythm = rytm wydań
        utf8Default();               // UTF-8 default = UTF-8 domyślnie (Java 18)
        simpleWebServer();           // simple web server = prosty serwer WWW (Java 18)
        patternsInJava21();          // patterns in Java 21 = wzorce w Javie 21
        virtualThreads();            // virtual threads = wątki wirtualne (Java 21)
        sequencedCollections();      // sequenced collections = kolekcje sekwencyjne (Java 21)
        stringTemplates();           // string templates = szablony napisów (podgląd, wycofane)
        unnamedVariables();          // unnamed variables = nienazwane zmienne (Java 22)
        gatherersAndStructured();    // gatherers and structured concurrency = zbieracze i współbieżność strukturalna
        compactSourceFiles();        // compact source files = kompaktowe pliki źródłowe (Java 25)
        readingReleaseNotes();       // reading release notes = czytanie notatek wydań
        upgradeStrategy();           // upgrade strategy = strategia aktualizacji
        exercises();                 // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. RYTM WYDAŃ
    // =================================================================================================

    /** Miesiąc i rok wydania zwykłego numeru Javy (od 9 wzwyż): nieparzyste we wrześniu, parzyste w marcu. */
    static String releaseLabel(int version) {   // release label = etykieta wydania
        int year = 2017 + (version - 8) / 2;
        String month = version % 2 == 1 ? "wrzesień" : "marzec";   // month = miesiąc
        return month + " " + year;
    }

    /**
     * 1. Rytm wydań. Od Javy 9 wydania wychodzą co sześć miesięcy: nieparzyste numery we wrześniu, parzyste w marcu.
     * Odstępy między wydaniami LTS: 8 → 11 to cztery lata, 11 → 17 trzy lata, a 17 → 21 → 25 po dwa lata.
     * Java 8 (2014) przez długi czas była w firmach jedynym „standardem”.
     */
    static void releaseRhythm() {
        section("1. Rytm wydań i LTS");

        for (int version : List.of(9, 11, 17, 18, 21, 25)) {
            show("Java " + version, releaseLabel(version));
        }
        // WYNIK: Java 9 → wrzesień 2017
        // WYNIK: Java 11 → wrzesień 2018
        // WYNIK: Java 17 → wrzesień 2021
        // WYNIK: Java 18 → marzec 2022
        // WYNIK: Java 21 → wrzesień 2023
        // WYNIK: Java 25 → wrzesień 2025
        show("LTS", List.of(8, 11, 17, 21, 25));
        // WYNIK: LTS → [8, 11, 17, 21, 25]
        show("działamy na Javie 17 lub nowszej", Runtime.version().feature() >= 17);   // feature = numer wydania
        // WYNIK: działamy na Javie 17 lub nowszej → true
        // (Dokładny numer wydania celowo nie jest drukowany — wynik zależałby od zainstalowanego JDK.)

        // Cykl życia funkcji:
        //   1. incubator   — osobny moduł (np. jdk.incubator.vector), można zmienić lub usunąć
        //   2. preview     — funkcja języka lub API, wymaga --enable-preview przy kompilacji i uruchomieniu
        //   3. kolejne podglądy (ze zmianami po opiniach użytkowników; typowo 2–3 wydania)
        //   4. final       — stała część Javy, objęta zasadami zgodności wstecznej
        // Kod skompilowany z funkcją podglądową działa tylko na DOKŁADNIE tym wydaniu Javy i jest przez to nieprzenośny.
        // PUŁAPKA: wydanie nie-LTS (np. 22) przestaje być wspierane po 6 miesiącach — nie ma poprawek bezpieczeństwa.
        //   Produkcja na wydaniu nie-LTS wymusza aktualizację co pół roku.
        // DOBRA PRAKTYKA: produkcyjnie trzymaj się wydań LTS (dziś 17 → 21 → 25), a nowsze wydania zwykłe
        //   uruchamiaj w środowisku testowym, żeby wcześnie wiedzieć, co się zmienia.
    }

    // =================================================================================================
    // 2. UTF-8 DOMYŚLNIE (JAVA 18)
    // =================================================================================================

    /**
     * 2. Java 18 (JEP 400): domyślne kodowanie znaków (Charset.defaultCharset()) to zawsze UTF-8 — niezależnie od
     * systemu. Do Javy 17 na Windowsie domyślne bywało windows-1250 lub podobne, więc ten sam kod czytał polskie
     * znaki raz dobrze, a raz źle. W Javie 17 nadal trzeba podawać kodowanie jawnie.
     */
    static void utf8Default() {
        section("2. Java 18: UTF-8 domyślnie (JEP 400)");

        // Na dziś (Java 17): zawsze podawaj kodowanie jawnie.
        byte[] bytes = "Cześć".getBytes(StandardCharsets.UTF_8);                       // bytes = bajty
        String right = new String(bytes, StandardCharsets.UTF_8);                      // right = poprawnie
        String wrong = new String(bytes, StandardCharsets.ISO_8859_1);                 // wrong = błędnie (inne kodowanie)
        show("bajtów w UTF-8", bytes.length);
        // WYNIK: bajtów w UTF-8 → 7
        show("odczyt jako UTF-8: znaków", right.length() + " (" + right + ")");
        // WYNIK: odczyt jako UTF-8: znaków → 5 (Cześć)
        show("odczyt jako ISO-8859-1: znaków", wrong.length());
        // WYNIK: odczyt jako ISO-8859-1: znaków → 7
        // Każdy polski znak zajmuje w UTF-8 dwa bajty, więc odczyt innym kodowaniem daje „krzaczki” (mojibake) i dłuższy napis.

        // (Java 18+) po zmianie domyślne kodowanie jest UTF-8, więc te zapisy zachowują się wszędzie tak samo:
        //   new FileReader("plik.txt");  new InputStreamReader(System.in);  "tekst".getBytes();
        // Nie zmienia to kodowania WYJŚCIA konsoli: stdout.encoding (od Javy 19) i native.encoding to osobne ustawienia.
        // Pliki Files.readString / Files.writeString (Java 11) były UTF-8 już wcześniej (zob. Modern06ApiAdditions).

        // PUŁAPKA: kod, który zakładał stare domyślne kodowanie Windows (np. czytał pliki zapisane w windows-1250
        //   przez FileReader), po przejściu na Javę 18+ zacznie czytać je jako UTF-8 i zepsuje polskie znaki.
        //   Wyjście z tej sytuacji: jawnie podaj Charset.forName("windows-1250") dla starych plików.
        // DOBRA PRAKTYKA: podawaj kodowanie jawnie niezależnie od wersji Javy — kod jest wtedy przenośny
        //   i nie zależy od ustawień komputera. Więcej: t18_io_files/Io12Charsets.
    }

    // =================================================================================================
    // 3. PROSTY SERWER WWW (JAVA 18)
    // =================================================================================================

    /**
     * 3. Java 18 (JEP 408): narzędzie wiersza poleceń {@code jwebserver} serwuje pliki z bieżącego katalogu
     * i bardzo się przydaje do podglądu stron statycznych. To tylko zwykły, minimalny serwer plików — nie nadaje się na produkcję.
     */
    static void simpleWebServer() {
        section("3. Java 18: prosty serwer WWW (jwebserver)");

        // (Java 18+) w terminalu, w katalogu ze stroną:
        //   jwebserver
        //     → Binding to loopback by default. For all interfaces use "-b 0.0.0.0" or "-b ::".
        //     → Serving /twoj/katalog and subdirectories on 127.0.0.1 port 8000
        //   (domyślnie tylko adres lokalny 127.0.0.1 i port 8000; opcje: -p port, -d katalog, -b adres)
        //
        // (Java 18+) to samo z programu — klasa SimpleFileServer w pakiecie com.sun.net.httpserver:
        //   HttpServer server = SimpleFileServer.createFileServer(
        //           new InetSocketAddress(8000), Path.of("/twoj/katalog"), SimpleFileServer.OutputLevel.INFO);
        //   server.start();
        //
        // Na dziś (Java 17): serwer z pakietu com.sun.net.httpserver istnieje od Javy 6 i ma ręczną obsługę ścieżek
        // (HttpServer.create + createContext + handler) — zobacz t28_networking_http/Http03LocalServer.
        // Python nie jest do tego potrzebny, ale wiele osób zna polecenie „python -m http.server” — jwebserver to jego odpowiednik.

        // PUŁAPKA: serwer nasłuchuje wyłącznie na lokalnym adresie, więc inny komputer w sieci się nie połączy,
        //   dopóki wprost nie podasz -b 0.0.0.0 — a wtedy wystawiasz katalog całej sieci. Uważaj na zawartość katalogu.
        // DOBRA PRAKTYKA: używaj jwebserver tylko do pracy lokalnej i demonstracji (np. podgląd wygenerowanej strony),
        //   nigdy jako serwera produkcyjnego: brak HTTPS, uwierzytelniania i limitów.
        note("jwebserver: tylko do pracy lokalnej, domyślnie 127.0.0.1:8000.");
        // WYNIK: ℹ jwebserver: tylko do pracy lokalnej, domyślnie 127.0.0.1:8000.
    }

    // =================================================================================================
    // 4. WZORCE W JAVIE 21
    // =================================================================================================

    /** Zdarzenia aplikacji jako zamknięta lista wariantów (event = zdarzenie). */
    sealed interface Event permits Login, Purchase, Logout { }

    record Login(String user) implements Event { }                 // login = zalogowanie

    record Purchase(String user, int cents) implements Event { }   // purchase = zakup; cents = grosze

    record Logout(String user) implements Event { }                // logout = wylogowanie

    /** Na dziś (Java 17): łańcuch instanceof z końcowym throw. */
    static String describe(Event event) {                          // describe = opisz
        if (event instanceof Login login) {
            return login.user() + " zalogował się";
        } else if (event instanceof Purchase purchase && purchase.cents() > 10_000) {
            return purchase.user() + " zrobił DUŻY zakup za " + purchase.cents() + " gr";
        } else if (event instanceof Purchase purchase) {
            return purchase.user() + " kupił za " + purchase.cents() + " gr";
        } else if (event instanceof Logout logout) {
            return logout.user() + " wylogował się";
        }
        throw new IllegalStateException("nieobsłużone zdarzenie: " + event);
    }

    /**
     * 4. Java 21: wzorce rekordów (JEP 440) i switch ze wzorcami (JEP 441) — final. Razem z rekordami i typami
     * zapieczętowanymi (Modern05) pozwalają pisać obsługę wariantów krótko i bezpiecznie.
     */
    static void patternsInJava21() {
        section("4. Java 21: switch ze wzorcami i wzorce rekordów");

        List<Event> events = List.of(new Login("ala"), new Purchase("ala", 2_500), new Purchase("ola", 25_000),
                new Logout("ala"));
        for (Event event : events) {
            note(describe(event));
        }
        // WYNIK: ℹ ala zalogował się
        // WYNIK: ℹ ala kupił za 2500 gr
        // WYNIK: ℹ ola zrobił DUŻY zakup za 25000 gr
        // WYNIK: ℹ ala wylogował się

        // (Java 21+) ta sama metoda ze switchem — bez default (typ zapieczętowany), z rozbiorem rekordów i strażnikiem:
        //   static String describe(Event event) {
        //       return switch (event) {
        //           case Login(String user) -> user + " zalogował się";
        //           case Purchase(String user, int cents) when cents > 10_000 -> user + " zrobił DUŻY zakup za " + cents + " gr";
        //           case Purchase(String user, int cents) -> user + " kupił za " + cents + " gr";
        //           case Logout(String user) -> user + " wylogował się";
        //       };
        //   }
        // Zalety: (1) brak rzutowań i akcesorów, (2) kompilator sprawdza wyczerpywalność — dopisanie wariantu bez gałęzi
        // to błąd kompilacji, (3) strażnik when zastępuje zagnieżdżone if.
        //
        // (Java 21+) wzorzec rekordu także w instanceof:
        //   if (event instanceof Purchase(String user, int cents) && cents > 10_000) { ... }
        // (Java 21+) wzorce zagnieżdżone:  case Pair(Login(var u), Purchase(var v, var c)) -> ...
        //
        // Historia: wzorce w switch — podgląd w Javie 17 (JEP 406), 18 (420), 19 (427), 20 (433), final w 21 (441);
        // wzorce rekordów — podgląd w 19 (JEP 405), 20 (432), final w 21 (440).
        // PUŁAPKA: kolejność gałęzi ma znaczenie — bardziej szczegółowy wzorzec (z when) musi stać PRZED ogólnym,
        //   inaczej kompilator zgłosi błąd „gałąź nieosiągalna”.
        // DOBRA PRAKTYKA: dopóki projekt jest na Javie 17, trzymaj kod w formie łańcucha instanceof zakończonego throw;
        //   przejście na switch później to mechaniczna zamiana (zob. Modern05RecordsSealedPatterns).
    }

    // =================================================================================================
    // 5. WĄTKI WIRTUALNE
    // =================================================================================================

    /**
     * 5. Java 21 (JEP 444): wątki wirtualne. Wątek wirtualny to bardzo tani wątek zarządzany przez JVM, nie przez system
     * operacyjny — można mieć ich miliony. Kod pisze się „po staremu” (blokujący, prosty), a JVM sama odkłada wątek,
     * gdy ten czeka na wejście/wyjście. Podgląd: Java 19 (JEP 425) i 20 (JEP 436).
     */
    static void virtualThreads() {
        section("5. Java 21: wątki wirtualne");

        // Na dziś (Java 17): pula wątków platformowych. Liczba wątków jest ograniczona (każdy domyślnie rezerwuje ok. 1 MB na stos).
        ExecutorService pool = Executors.newFixedThreadPool(4);    // pool = pula
        try {
            List<Future<Integer>> futures = new ArrayList<>();      // futures = obiekty-obietnice wyników
            for (int i = 1; i <= 4; i++) {
                final int n = i;
                futures.add(pool.submit(() -> n * n));              // submit = zleć zadanie
            }
            List<Integer> results = new ArrayList<>();              // results = wyniki
            for (Future<Integer> future : futures) {
                results.add(future.get());                          // get = poczekaj i pobierz
            }
            show("pula 4 wątków: kwadraty", results);
            // WYNIK: pula 4 wątków: kwadraty → [1, 4, 9, 16]
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();                    // przywróć flagę przerwania
            throw new IllegalStateException(e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        } finally {
            pool.shutdown();                                        // shutdown = zamknij pulę
        }

        // (Java 21+) to samo z wątkami wirtualnymi — jeden NOWY wątek wirtualny na każde zadanie, bez ograniczenia puli:
        //   try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
        //       Future<Integer> f = executor.submit(() -> 6 * 7);
        //       System.out.println(f.get());
        //   }       // ExecutorService jest AutoCloseable od Javy 19: close() czeka na zakończenie zadań
        //   Thread.startVirtualThread(() -> System.out.println("hej z wątku wirtualnego"));
        //   Thread.ofVirtual().name("worker-1").start(task);
        //
        // Kiedy pomagają? Gdy zadań jest bardzo dużo, a większość czasu spędzają na CZEKANIU (zapytania HTTP, bazy danych,
        // pliki). Kiedy NIE: obliczenia obciążające procesor (liczba rdzeni jest ograniczona — wątki wirtualne nie przyspieszą).
        // PUŁAPKA: NIE tworzy się puli wątków wirtualnych — jest „jeden wątek na zadanie”, a ich ograniczanie robi się
        //   semaforem. Pula wątków wirtualnych to antywzorzec.
        // PUŁAPKA: w Javie 21 wątek wirtualny blokujący się wewnątrz synchronized zostaje „przypięty” (pinning) do wątku
        //   systemowego i traci zaletę taniości; Java 24 (JEP 491) zdejmuje to ograniczenie dla synchronized.
        //   Rozwiązanie na Javę 21: ReentrantLock zamiast synchronized w kodzie, który czeka na IO.
        // DOBRA PRAKTYKA: zmiany na wątki wirtualne zacznij od kodu serwerowego „jedno żądanie = jeden wątek”;
        //   zmienne ThreadLocal trzymaj małe (miliony wątków × duży ThreadLocal = pamięć). Więcej o pulach:
        //   t21_concurrency/Concurrency04Executors.
    }

    // =================================================================================================
    // 6. KOLEKCJE SEKWENCYJNE
    // =================================================================================================

    /**
     * 6. Java 21 (JEP 431): interfejsy SequencedCollection, SequencedSet i SequencedMap — kolekcje z określonym
     * porządkiem i pierwszym/ostatnim elementem, dostępnym tą samą metodą w każdej z nich.
     */
    static void sequencedCollections() {
        section("6. Java 21: kolekcje sekwencyjne");

        // Dziś (Java 17): ostatni element listy to get(size() - 1), a dla pustej listy to błąd indeksu.
        List<String> letters = List.of("a", "b", "c");              // letters = litery
        show("pierwszy", letters.get(0));
        // WYNIK: pierwszy → a
        show("ostatni", letters.get(letters.size() - 1));
        // WYNIK: ostatni → c
        List<String> empty = new ArrayList<>();                     // empty = pusta
        expectThrows("get(size() - 1) na pustej", () -> empty.get(empty.size() - 1));
        // WYNIK: ✔ get(size() - 1) na pustej → rzucono IndexOutOfBoundsException: Index -1 out of bounds for length 0

        // Odwrócona kopia: kopiuj i odwracaj.
        List<String> reversed = new ArrayList<>(letters);           // reversed = odwrócona
        Collections.reverse(reversed);
        show("odwrócona kopia", reversed);
        // WYNIK: odwrócona kopia → [c, b, a]

        // Zbiór z zachowaną kolejnością (LinkedHashSet): pierwszy łatwo, ostatni — trzeba przejść po wszystkich.
        Set<String> ordered = new LinkedHashSet<>(letters);         // ordered = uporządkowany
        show("LinkedHashSet: pierwszy", ordered.iterator().next());
        // WYNIK: LinkedHashSet: pierwszy → a
        show("LinkedHashSet: ostatni", ordered.stream().reduce((first, second) -> second).orElseThrow());
        // WYNIK: LinkedHashSet: ostatni → c

        // (Java 21+) wszystkie kolekcje sekwencyjne (List, Deque, LinkedHashSet, SortedSet, LinkedHashMap...) mają:
        //   letters.getFirst();   letters.getLast();                // pobierz pierwszy / ostatni
        //   letters.addFirst("0"); letters.addLast("z");            // dodaj na początek / na koniec (nie dla List.of)
        //   letters.removeFirst(); letters.removeLast();            // usuń pierwszy / ostatni
        //   letters.reversed();                                     // WIDOK w odwrotnej kolejności (nie kopia!)
        //   ordered.reversed();  map.firstEntry(); map.lastEntry(); map.reversed();   // SequencedSet i SequencedMap
        // Pusta kolekcja: getFirst()/getLast() rzucają NoSuchElementException — czytelniej niż IndexOutOfBounds.
        //
        // PUŁAPKA: reversed() to WIDOK — zmiany oryginału są widoczne przez widok (jak subList), w odróżnieniu od
        //   „odwróconej kopii”, którą tworzysz dziś (new ArrayList + Collections.reverse).
        // PUŁAPKA: dodanie metod do List mogło złamać kod źródłowy: własna klasa implementująca List, która już miała
        //   metodę reversed() lub getFirst() z innym typem wyniku, przestaje się kompilować w Javie 21.
        // DOBRA PRAKTYKA: dziś pisz małe metody pomocnicze (firstOrNull, lastOrDefault), które po aktualizacji
        //   zastąpisz getFirst()/getLast() w jednym miejscu.
    }

    // =================================================================================================
    // 7. SZABLONY NAPISÓW
    // =================================================================================================

    /**
     * 7. Szablony napisów (String Templates) — przykład funkcji, która miała podgląd, a potem została wycofana.
     * Miały zastąpić sklejanie napisów i format. Podgląd w Javie 21 (JEP 430) i 22 (JEP 459). Trzeci podgląd (JEP 465)
     * zaproponowano dla Javy 23, ale przed wydaniem go wycofano — od Javy 23 tej funkcji w JDK nie ma (do przeprojektowania).
     */
    static void stringTemplates() {
        section("7. Szablony napisów — podgląd, potem wycofanie");

        String name = "Ola";
        int age = 28;
        // Na dziś (Java 17): formatted (Java 15) albo String.format; sklejanie plusem dla krótkich napisów.
        show("formatted", "%s ma %d lat".formatted(name, age));
        // WYNIK: formatted → Ola ma 28 lat
        show("sklejanie", name + " ma " + age + " lat");
        // WYNIK: sklejanie → Ola ma 28 lat

        // (podgląd w Javie 21 i 22, z --enable-preview; NIE jest dostępne w stabilnej Javie) składnia wyglądała tak:
        //   String text = STR."\{name} ma \{age} lat";
        // Ostatecznie projekt wycofano, więc nie ucz się tej składni jako przyszłości Javy. To dobra lekcja:
        // funkcje podglądowe mogą zniknąć — dlatego nie używa się ich w kodzie produkcyjnym.
        // PUŁAPKA: tutoriale z lat 2023–2024 nadal pokazują STR."..." — sprawdzaj w notatkach wydania, czy funkcja trafiła
        //   do wersji ostatecznej, zanim uznasz ją za część Javy.
        // DOBRA PRAKTYKA: dla większych tekstów łącz bloki tekstowe (Java 15) z formatted (zob. Modern04TextBlocks).
    }

    // =================================================================================================
    // 8. NIENAZWANE ZMIENNE
    // =================================================================================================

    /**
     * 8. Nienazwane zmienne i wzorce {@code _} (Java 22, JEP 456; podgląd w Javie 21, JEP 443). Podkreślnik oznacza
     * „wartość, której nie używam” — czytelnie i bez ostrzeżeń o nieużywanej zmiennej.
     */
    static void unnamedVariables() {
        section("8. Java 22: nienazwane zmienne _");

        List<String> names = List.of("Ala", "Ola", "Ewa");
        // Na dziś (Java 17): zmienna musi mieć nazwę; zwyczajowo „ignored” (ignorowana) albo „unused” (nieużywana).
        int count = 0;
        for (String ignored : names) {                              // pętla tylko liczy elementy
            count++;
        }
        show("liczba elementów", count);
        // WYNIK: liczba elementów → 3
        try {
            Integer.parseInt("abc");
        } catch (NumberFormatException ignored) {                   // wyjątek nas nie interesuje
            note("zły format liczby (wyjątek zignorowany)");
            // WYNIK: ℹ zły format liczby (wyjątek zignorowany)
        }

        // (Java 22+) to samo z podkreślnikiem:
        //   for (String _ : names) { count++; }
        //   catch (NumberFormatException _) { ... }
        //   map.forEach((_, value) -> process(value));              // ignorujemy klucz
        //   case Purchase(String user, _) -> ...                    // wzorzec: nie obchodzi nas drugie pole
        //   try (var _ = lock.acquire()) { ... }                    // zasób, do którego się nie odwołujemy
        // PUŁAPKA: w Javie 9–21 pojedynczy podkreślnik jako nazwa zmiennej jest BŁĘDEM kompilacji („_” to słowo
        //   kluczowe). W Javie 8 działał jeszcze (z ostrzeżeniem). W Javie 21 dostępny tylko jako podgląd.
        // DOBRA PRAKTYKA: nie ignoruj wyjątków bez powodu — pusty catch to ukryty błąd; jeśli ignorujesz, napisz dlaczego.
        //   Podkreślnik tylko podkreśla, że nie używasz wartości; nie zwalnia z pytania, czy powinieneś.
    }

    // =================================================================================================
    // 9. STREAM GATHERERS I STRUKTURALNA WSPÓŁBIEŻNOŚĆ
    // =================================================================================================

    /** Na dziś (Java 17): sumy okien przesuwnych (sliding window) o zadanej szerokości. */
    static List<Integer> slidingSums(int[] data, int size) {       // sliding sums = sumy w oknie przesuwnym
        return IntStream.rangeClosed(0, data.length - size)
                .mapToObj(start -> IntStream.range(start, start + size).map(i -> data[i]).sum())
                .collect(Collectors.toList());
    }

    /**
     * 9. Java 24 (JEP 485): Stream Gatherers — własne operacje pośrednie strumienia (okna, skanowanie, współbieżne mapowanie).
     * Podgląd: Java 22 (JEP 461) i 23 (JEP 473). Obok nich: współbieżność strukturalna (structured concurrency)
     * i wartości zakresowe (scoped values) — funkcje związane z wątkami wirtualnymi.
     */
    static void gatherersAndStructured() {
        section("9. Java 24: Stream Gatherers; podglądy: współbieżność strukturalna");

        int[] data = {1, 2, 3, 4, 5, 6};
        show("sumy okien szerokości 3 (dziś)", slidingSums(data, 3));
        // WYNIK: sumy okien szerokości 3 (dziś) → [6, 9, 12, 15]

        // (Java 24+) to samo gathererem — okno przesuwne to gotowy element biblioteki:
        //   Stream.of(1, 2, 3, 4, 5, 6)
        //         .gather(Gatherers.windowSliding(3))                // [[1,2,3], [2,3,4], [3,4,5], [4,5,6]]
        //         .map(window -> window.stream().mapToInt(Integer::intValue).sum())
        //         .toList();
        // Inne gotowe: windowFixed(n) (okna rozłączne), fold (zwiń do jednego wyniku), scan (sumy narastające),
        // mapConcurrent (mapowanie z limitem współbieżności) oraz własne, pisane jako implementacja interfejsu Gatherer.
        //
        // Structured concurrency (podgląd — Java 21 JEP 453, kolejne podglądy w 22–25; poniższy zapis to API z Javy 21,
        // w późniejszych podglądach zmieniało się): grupa zadań współbieżnych
        // traktowana jak jedna jednostka — gdy jedno zawiedzie, reszta jest anulowana, a wyjątki nie „giną”.
        //   try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        //       var user = scope.fork(() -> findUser());
        //       var order = scope.fork(() -> findOrder());
        //       scope.join().throwIfFailed();
        //       return new Response(user.get(), order.get());
        //   }
        // Scoped values (podgląd w Javie 21 — JEP 446): bezpieczna alternatywa dla ThreadLocal w świecie wątków wirtualnych
        // (dane niezmienne, o jasnym zasięgu); finalne od Javy 25 (JEP 506).
        // PUŁAPKA: funkcje podglądowe (preview) wymagają flagi --enable-preview i dają kod, który działa tylko na tym samym
        //   wydaniu Javy. Nie używaj ich w kodzie produkcyjnym, a przy nauce traktuj numery wydań i JEP-ów jako informację
        //   „stan na dziś” — sprawdź aktualny stan w notatkach wydania.
        // DOBRA PRAKTYKA: zanim napiszesz własną pętlę okien, sprawdź, czy bieżąca wersja Javy nie ma już gotowego gatherera.
    }

    // =================================================================================================
    // 10. KOMPAKTOWE PLIKI ŹRÓDŁOWE I ELASTYCZNE KONSTRUKTORY (JAVA 25)
    // =================================================================================================

    /** Klasa bazowa do demonstracji konstruktora z walidacją (positive = dodatnia). */
    static class PositiveNumber {
        final int value;                                           // value = wartość

        PositiveNumber(int value) {
            this.value = value;
        }
    }

    /** Podklasa: na dziś walidujemy argument wywołaniem statycznej metody wewnątrz super(...). */
    static class Percentage extends PositiveNumber {               // percentage = procent
        Percentage(int value) {
            super(requirePositive(value));                         // walidacja musi się zmieścić w wyrażeniu
        }

        private static int requirePositive(int value) {            // require positive = wymagaj dodatniej
            if (value <= 0) {
                throw new IllegalArgumentException("wartość musi być dodatnia: " + value);
            }
            return value;
        }
    }

    /**
     * 10. Java 25 (finalne): kompaktowe pliki źródłowe z metodą {@code main} instancji (JEP 512) oraz elastyczne ciała
     * konstruktorów (JEP 513). Pierwsze upraszcza „Hello World” dla początkujących, drugie pozwala pisać instrukcje
     * przed wywołaniem {@code super(...)}.
     */
    static void compactSourceFiles() {
        section("10. Java 25: kompaktowy main i elastyczne konstruktory");

        show("Percentage(15).value", new Percentage(15).value);
        // WYNIK: Percentage(15).value → 15
        expectThrows("Percentage(-3)", () -> new Percentage(-3));
        // WYNIK: ✔ Percentage(-3) → rzucono IllegalArgumentException: wartość musi być dodatnia: -3

        // (Java 25+) cały program w jednym pliku, bez klasy i bez „public static void main(String[] args)”:
        //   // plik Hello.java
        //   void main() {
        //       IO.println("Cześć, świecie!");                     // IO = nowa klasa w java.lang (Java 25): println, readln
        //   }
        // Na dziś (Java 17) ten sam program to klasa + statyczny main (jak w pierwszej lekcji kursu, Basics01HelloJvm).
        // Ewolucja: „nienazwane klasy” (podgląd w 21, JEP 445) → „klasy niejawne” (22–23) → kompaktowe pliki źródłowe (final w 25).
        //
        // (Java 25+) elastyczne ciała konstruktorów — instrukcje przed super(...), o ile nie odwołują się do this:
        //   Percentage(int value) {
        //       if (value <= 0) {
        //           throw new IllegalArgumentException("wartość musi być dodatnia: " + value);
        //       }
        //       super(value);
        //   }
        // Na dziś walidację trzeba „wcisnąć” w wyrażenie wewnątrz super(...) — jak requirePositive wyżej.
        // Historia elastycznych konstruktorów: podgląd w Javie 22 (JEP 447, jako „instrukcje przed super”), 23, 24.
        //
        // PUŁAPKA: przed super(...) nie wolno używać this (pól, metod instancji) — obiekt jeszcze nie istnieje.
        // DOBRA PRAKTYKA: walidację argumentów rób jak najwcześniej (przed utworzeniem obiektu), a niezmienność
        //   zapewniaj polami final. Dziś — metoda statyczna w super(...) (jak wyżej) lub fabryka (metoda statyczna tworząca obiekt).
    }

    // =================================================================================================
    // 11. JAK CZYTAĆ NOTATKI WYDAŃ I JEP-Y
    // =================================================================================================

    /**
     * 11. Jak czytać JEP i notatki wydania. Nowości Javy poznajesz u źródła — blogi i filmy bywają nieaktualne
     * (jak w przypadku szablonów napisów).
     */
    static void readingReleaseNotes() {
        section("11. Jak czytać JEP-y i notatki wydań");

        // GDZIE SZUKAĆ:
        //   • openjdk.org/jeps/NNN        — pełna specyfikacja jednej zmiany (motywacja, opis, alternatywy, ryzyka)
        //   • openjdk.org/projects/jdk/N  — lista JEP-ów danego wydania i jego daty
        //   • notatki wydania (Release Notes) — też zmiany bez JEP-ów (poprawki, usunięcia, zmiany zachowania)
        //   • dokumentacja API — znacznik „Since: 17” przy klasie lub metodzie mówi, od kiedy ją mamy
        //   • javac --release N + -Xlint:all — kompilator sam wskaże przestarzałe API
        //
        // JAK CZYTAĆ STATUS JEP-A: Draft → Candidate → Proposed to Target → Targeted → Integrated → Completed.
        //   Tytuł zawiera etap: „(Preview)”, „(Second Preview)”, „(Incubator)” albo bez dopisku — wersja ostateczna.
        //
        // PODSTAWOWE SŁÓWKA Z NOTATEK:
        //   deprecated = przestarzałe (nie używaj); deprecated for removal = zostanie usunięte; removed = usunięte;
        //   behavior change = zmiana zachowania; incubator/preview = jeszcze nie ostateczne.
        //
        // FLAGA --release N (kompilator): kompiluje tak, jakby JDK miał wersję N, i sprawdza użyte API względem tej wersji.
        //   Różni się od starych -source/-target, które zmieniały tylko składnię i format bajtkodu, ale pozwalały użyć API
        //   nowszego JDK (kod kompilował się, a na starszej Javie padał z NoSuchMethodError). Ten kurs jest weryfikowany
        //   z --release 17, dlatego nowsze API nie przejdzie kompilacji.

        show("Java w której uruchomiono lekcję wspiera --release 17", Runtime.version().feature() >= 17);
        // WYNIK: Java w której uruchomiono lekcję wspiera --release 17 → true

        // PUŁAPKA: samo przestawienie „language level” w IntelliJ nie zmienia wersji API — upewnij się, że JDK i
        //   --release (w Mavenie: maven.compiler.release) są zgodne z tym, na czym program faktycznie będzie działał.
        // DOBRA PRAKTYKA: na początku roku przeczytaj notatki wydań i listę JEP-ów ostatniego LTS — trwa to godzinę,
        //   a oszczędza tygodnie szukania „dlaczego to nie działa po aktualizacji”.
        note("Zaglądaj do źródła: openjdk.org/jeps i dokumentacja z polem Since.");
        // WYNIK: ℹ Zaglądaj do źródła: openjdk.org/jeps i dokumentacja z polem Since.
    }

    // =================================================================================================
    // 12. STRATEGIA AKTUALIZACJI
    // =================================================================================================

    /** Zasób zamykany deterministycznie — zamiennik dla finalize() (przestarzałego). */
    static final class Resource implements AutoCloseable {         // resource = zasób
        private final String name;                                 // name = nazwa
        private final StringBuilder log;                           // log = dziennik

        Resource(String name, StringBuilder log) {
            this.name = name;
            this.log = log;
        }

        @Override
        public void close() {                                      // close = zamknij
            log.append("zamknięto ").append(name).append("; ");
        }
    }

    /**
     * 12. Aktualizacja z LTS na LTS. Kod napisany w tym kursie (Java 17) nie wymaga zmian, żeby działać w Javie 21 —
     * Java dba o zgodność wsteczną. Aktualizacja to głównie praca wokół kodu: narzędzia, biblioteki, opcje JVM.
     */
    static void upgradeStrategy() {
        section("12. Strategia aktualizacji: LTS → LTS");

        // Zamiennik finalizacji: finalize() jest przestarzałe do usunięcia od Javy 18 (JEP 421), bo było nieprzewidywalne
        // (nie wiadomo, KIEDY i CZY się wykona). Zasoby zamykaj deterministycznie — try-with-resources (lub Cleaner).
        StringBuilder log = new StringBuilder();
        try (Resource first = new Resource("pierwszy", log); Resource second = new Resource("drugi", log)) {
            log.append("praca z ").append(first.name).append(" i ").append(second.name).append("; ");
        }
        show("kolejność zamykania", log);
        // WYNIK: kolejność zamykania → praca z pierwszy i drugi; zamknięto drugi; zamknięto pierwszy;
        // (Zasoby zamykają się w odwrotnej kolejności niż otwarcie — zob. t10_exceptions/Exceptions04TryWithResources.)

        // PLAN AKTUALIZACJI (np. 17 → 21):
        //   1. Zapisz stan: zielone testy na starej wersji, wersje bibliotek i wtyczek.
        //   2. Zaktualizuj narzędzia: Maven/Gradle, wtyczki kompilatora, IntelliJ, obraz kontenera z JDK.
        //   3. Zaktualizuj biblioteki działające „na bajtkodzie” (Lombok, Mockito/ByteBuddy, Spring, Hibernate, JaCoCo) —
        //      stare wersje często nie czytają nowego formatu klas i zgłaszają błąd przy starcie.
        //   4. Uruchom testy na nowym JDK z wersją kompilacji --release 17, potem 21; napraw błędy i ostrzeżenia
        //      (-Xlint:deprecation, jdeprscan do wyszukiwania użycia usuniętego API, jdeps do znalezienia użycia wnętrza JDK).
        //   5. Dopiero potem używaj nowych funkcji (switch ze wzorcami, wątki wirtualne) — małymi krokami.
        //   6. Sprawdź flagi JVM i logi GC (domyślny odśmiecacz G1 działa od Javy 9; w Javie 21 doszedł pokoleniowy ZGC).
        //
        // CO ZMIENIA SIĘ W STARYM KODZIE (nic się „nie psuje”, ale warto wiedzieć):
        //   • SecurityManager — przestarzały do usunięcia od Javy 17 (JEP 411), w Javie 24 wyłączony na stałe (JEP 486).
        //   • finalize() — przestarzałe do usunięcia od Javy 18 (JEP 421); zastąp try-with-resources lub Cleaner.
        //   • UTF-8 domyślnie od Javy 18 (zob. sekcja 2) — jawnie podawaj kodowanie dla starych plików.
        //   • Silne enkapsulowanie wnętrza JDK od Javy 17 (JEP 403): refleksja po klasach java.* wymaga --add-opens.
        //   • Dynamiczne ładowanie agentów (np. narzędzia testowe) w Javie 21 daje ostrzeżenie (JEP 451).
        //
        // PUŁAPKA: aktualizacja „tylko JDK” bez aktualizacji bibliotek — najczęstsza przyczyna błędów po zmianie wersji.
        // PUŁAPKA: przejście na wydanie nie-LTS „bo nowsze” — za pół roku kolejna aktualizacja albo brak poprawek bezpieczeństwa.
        // DOBRA PRAKTYKA: aktualizuj LTS po LTS zaraz po wydaniu pierwszych poprawek (np. 21.0.1), nie czekaj aż
        //   wsparcie starego się skończy. Mała zmiana co 2–3 lata jest tańsza niż skok o 8 → 21.
        note("Kod z tego kursu (Java 17) działa bez zmian w Javie 21.");
        // WYNIK: ℹ Kod z tego kursu (Java 17) działa bez zmian w Javie 21.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Wydania co 6 miesięcy (marzec, wrzesień); LTS: 8, 11, 17, 21, 25. Preview wymaga --enable-preview i bywa wycofane.
     *   • Java 18: UTF-8 domyślnie (JEP 400), jwebserver (JEP 408). Nadal podawaj kodowanie jawnie.
     *   • Java 21: wzorce rekordów (440), switch ze wzorcami (441), wątki wirtualne (444), kolekcje sekwencyjne (431).
     *   • Wątki wirtualne: jedno zadanie = jeden wątek, bez puli; pomagają przy czekaniu na IO, nie przy obliczeniach.
     *   • getFirst/getLast/reversed (widok!) w kolekcjach sekwencyjnych; dziś get(size() - 1) i kopia + reverse.
     *   • Szablony napisów (STR."...") były podglądem w Javie 21 i 22, a w 23 zostały wycofane — dziś formatted i bloki tekstowe.
     *   • Java 22: nienazwane zmienne _ ; Java 24: Stream Gatherers; Java 25: kompaktowy main (IO.println), konstruktory
     *     z instrukcjami przed super(...); scoped values finalne w 25.
     *   • JEP-y: openjdk.org/jeps/NNN; „Since” w API; opcja kompilatora --release N (sprawdza API, nie tylko składnię).
     *   • Aktualizacja LTS → LTS: narzędzia i biblioteki najpierw; przestarzałe: SecurityManager, finalize().
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie wydania Javy są LTS i co to znaczy, że wydanie jest „LTS”?
     *   2. Czym różni się funkcja w podglądzie (preview) od funkcji w wersji ostatecznej i dlaczego nie używa się
     *      podglądu w produkcji?
     *   3. Co wypisze:  System.out.println(new String("Cześć".getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1).length());  ?
     *   4. ZNAJDŹ BŁĄD (w Javie 21):  ExecutorService pool = Executors.newFixedThreadPool(10_000);  // „bo wątki są tanie”
     *   5. Co wypisze (w Javie 17):  List<String> empty = new ArrayList<>();  empty.get(empty.size() - 1);  ?
     *   6. Do czego służy opcja kompilatora --release 17 i czym różni się od -source 17 -target 17?
     *   7. ZNAJDŹ BŁĄD:  catch (NumberFormatException _) { }  — w kodzie kompilowanym w Javie 17.
     *   8. Dlaczego przed aktualizacją JDK warto zaktualizować biblioteki takie jak Mockito czy Lombok?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: etykiety wydań", List.of("wrzesień 2021", "marzec 2022", "wrzesień 2023", "wrzesień 2025"),
                () -> List.of(exercise1(17), exercise1(18), exercise1(21), exercise1(25)));
        Check.equal("ćw. 2: odwrócona kopia", List.of("c", "b", "a"), () -> exercise2(List.of("a", "b", "c")));
        Check.equal("ćw. 2: oryginał bez zmian", List.of("a", "b", "c"), () -> {
            List<String> original = new ArrayList<>(List.of("a", "b", "c"));
            exercise2(original);
            return original;
        });
        Check.equal("ćw. 3: sumy okien", List.of(6, 9, 12, 15), () -> exercise3(new int[]{1, 2, 3, 4, 5, 6}, 3));
        Check.equal("ćw. 4: wydatki użytkownika", 1250, () -> exercise4(List.of(new Login("ala"),
                new Purchase("ala", 1000), new Purchase("ola", 500), new Purchase("ala", 250), new Logout("ala")), "ala"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of("wrzesień 2021", "marzec 2022", "wrzesień 2023", "wrzesień 2025"),
                () -> List.of(solution1(17), solution1(18), solution1(21), solution1(25)));
        Check.equal("ćw. 2 (wzorzec)", List.of("c", "b", "a"), () -> solution2(List.of("a", "b", "c")));
        Check.equal("ćw. 2 (wzorzec): oryginał bez zmian", List.of("a", "b", "c"), () -> {
            List<String> original = new ArrayList<>(List.of("a", "b", "c"));
            solution2(original);
            return original;
        });
        Check.equal("ćw. 3 (wzorzec)", List.of(6, 9, 12, 15), () -> solution3(new int[]{1, 2, 3, 4, 5, 6}, 3));
        Check.equal("ćw. 4 (wzorzec)", 1250, () -> solution4(List.of(new Login("ala"),
                new Purchase("ala", 1000), new Purchase("ola", 500), new Purchase("ala", 250), new Logout("ala")), "ala"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć etykietę wydania Javy (od 9 wzwyż) w formacie „wrzesień 2021” / „marzec 2022”.
     * Zasada: nieparzyste numery wychodzą we wrześniu, parzyste w marcu; Java 9 to wrzesień 2017.
     * Podpowiedź: rok = 2017 + (wersja - 8) / 2 (dzielenie całkowite).
     */
    static String exercise1(int version) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (łatwe): PRZEPISZ „na dziś” to, co w Javie 21 robi {@code list.reversed()}: zwróć NOWĄ listę
     * w odwrotnej kolejności, nie zmieniając oryginału.
     * <pre>{@code
     * // (Java 21+) PO:  return list.reversed();   // widok, nie kopia
     * }</pre>
     * Podpowiedź: new ArrayList<>(list) i Collections.reverse(kopia).
     */
    static List<String> exercise2(List<String> list) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć sumy okien przesuwnych o podanej szerokości (jak gatherer windowSliding).
     * Dla {1, 2, 3, 4, 5, 6} i szerokości 3 wynik to [6, 9, 12, 15].
     * Podpowiedź: IntStream.rangeClosed(0, data.length - size) i suma elementów od start do start + size.
     */
    static List<Integer> exercise3(int[] data, int size) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zsumuj w groszach zakupy (Purchase) wskazanego użytkownika w liście zdarzeń; inne zdarzenia
     * i innych użytkowników pomiń. Użyj łańcucha instanceof zakończonego throw dla nieobsłużonego zdarzenia
     * (Event jest zapieczętowany).
     * Podpowiedź: pętla po zdarzeniach, wzorzec instanceof Purchase p && p.user().equals(user).
     */
    static int exercise4(List<Event> events, String user) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(int version) {
        return (version % 2 == 1 ? "wrzesień" : "marzec") + " " + (2017 + (version - 8) / 2);
    }

    static List<String> solution2(List<String> list) {
        List<String> copy = new ArrayList<>(list);                 // copy = kopia
        Collections.reverse(copy);
        return copy;
    }

    static List<Integer> solution3(int[] data, int size) {
        List<Integer> sums = new ArrayList<>();                    // sums = sumy
        for (int start = 0; start + size <= data.length; start++) {
            int sum = 0;
            for (int i = start; i < start + size; i++) {
                sum += data[i];
            }
            sums.add(sum);
        }
        return sums;
    }

    static int solution4(List<Event> events, String user) {
        int total = 0;                                             // total = suma
        for (Event event : events) {
            if (event instanceof Purchase purchase) {
                if (purchase.user().equals(user)) {
                    total += purchase.cents();
                }
            } else if (!(event instanceof Login) && !(event instanceof Logout)) {
                throw new IllegalStateException("nieobsłużone zdarzenie: " + event);
            }
        }
        return total;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. LTS: 8, 11, 17, 21, 25. Wydanie z długim wsparciem (long-term support) dostaje poprawki bezpieczeństwa i błędów
     *      przez wiele lat, więc nadaje się na produkcję; zwykłe wydania — tylko do następnego (pół roku).
     *   2. Podgląd jest funkcją jeszcze niegotową: może się zmienić lub zostać wycofana, wymaga --enable-preview, a skompilowany
     *      kod działa tylko na tym samym wydaniu Javy. Wersja ostateczna ma gwarancje zgodności wstecznej.
     *   3. 7 (UTF-8 koduje „Cześć” w 7 bajtach; ISO-8859-1 czyta każdy bajt jako osobny znak).
     *   4. Pula 10 000 wątków platformowych to marnotrawstwo (każdy wątek systemowy kosztuje pamięć). Dla wątków wirtualnych
     *      używa się Executors.newVirtualThreadPerTaskExecutor() — jeden nowy wątek wirtualny na zadanie, bez puli.
     *   5. IndexOutOfBoundsException: Index -1 out of bounds for length 0. (W Javie 21: empty.getLast() rzuciłoby
     *      NoSuchElementException.)
     *   6. --release 17 kompiluje względem API i składni Javy 17 (nowsze klasy i metody nie przejdą kompilacji).
     *      -source/-target zmieniają tylko poziom składni i format bajtkodu, a pozwalają użyć API nowszego JDK.
     *   7. W Javie 17 „_” jako nazwa zmiennej to błąd kompilacji (słowo kluczowe od Javy 9); nienazwane zmienne
     *      są dostępne dopiero od Javy 22 (podgląd w 21). Na Javie 17: catch (NumberFormatException ignored).
     *   8. Biblioteki działające na bajtkodzie (Lombok, Mockito/ByteBuddy, Spring) muszą znać nowy format plików klas
     *      i zmiany w JDK; stare wersje zgłaszają błędy przy starcie lub w testach.
     */
    // </editor-fold>
}
