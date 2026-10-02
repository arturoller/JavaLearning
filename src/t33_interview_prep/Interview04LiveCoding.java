package t33_interview_prep;

import helpers.Check;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Kodowanie na żywo (live coding) — jak się zachować i jedno zadanie krok po kroku
 *        (live coding = kodowanie na żywo; requirements = wymagania; think aloud = myśleć na głos)
 *
 * W SKRÓCIE:
 *   Na kodowaniu na żywo oceniany jest PROCES, nie tylko wynik: czy dopytujesz, czy podajesz przykłady, czy mówisz,
 *   co robisz, czy zaczynasz od prostego rozwiązania, czy testujesz i poprawiasz. Ta lekcja pokazuje jedno
 *   realistyczne zadanie w czterech wersjach (V1–V4), typowe błędy, pytania do rozmówcy, listę kontrolną
 *   na 30 punktów i krótko — pytania miękkie.
 *
 * ANALOGIA: pilot przed startem odhacza listę kontrolną, choć lata od lat. Nie dlatego, że nie umie latać, lecz
 *   dlatego, że pod presją ludzie pomijają rzeczy oczywiste. Lista kontrolna kodowania na żywo działa tak samo.
 *
 * JAK TO DZIAŁA:
 *   Sześć faz:  1) wyjaśnij wymagania  2) podaj przykłady  3) zaplanuj i powiedz złożoność  4) napisz najprostszą wersję
 *   5) przetestuj  6) doszlifuj. Kod to tylko jedna trzecia czasu — reszta to komunikacja i sprawdzanie.
 *   Zadanie przykładowe: z tekstu CSV z liniami zamówień policz sumę wydatków na klienta i posortuj klientów malejąco.
 *   V1 = pierwszy szkic (działa na szczęśliwej ścieżce), V2 = przypadki brzegowe, V3 = podział na małe części,
 *   V4 = dopracowanie, testy i raport.
 *
 * SŁÓWKA:
 *   live coding = kodowanie na żywo; requirement = wymaganie; edge case = przypadek brzegowy; refactor = refaktoryzacja
 *   (poprawa struktury bez zmiany działania); checklist = lista kontrolna; silent catch = połknięty wyjątek;
 *   shared state = współdzielony stan; unit = jednostka; ranking = ranking (lista uporządkowana).
 *
 * ZOBACZ TEŻ: t33_interview_prep/Interview03CodingTasks (klasyczne zadania), t27_clean_code_pitfalls/Pitfalls02CodeReview
 *   (błędy w kodzie), t15_numbers/Numbers01BigDecimal (pieniądze), t10_exceptions/Exceptions07BestPractices (wyjątki)
 * </pre>
 */
public class Interview04LiveCoding {

    /** Przykładowe dane (blok tekstowy = text block, Java 15+): nagłówek i pięć poprawnych zamówień. */
    static final String SAMPLE = """
            klient,produkt,ilosc,cena
            Ola,Kawa,2,12.50
            Adam,Herbata,1,8.00
            Ola,Ciastko,3,4.25
            Ewa,Kawa,1,12.50
            Adam,Kawa,2,12.50
            """;

    /** Dane "brudne": pusta linia, spacje, błędna ilość, za mało pól i ujemna ilość. */
    static final String MESSY = """
            klient,produkt,ilosc,cena
            Ola,Kawa,2,12.50

             Adam , Herbata , 1 , 8.00
            Zofia,Kawa,dwa,12.50
            Jan,Kawa
            Ola,Ciastko,3,4.25
            Ewa,Kawa,-1,12.50
            """;

    public static void main(String[] args) {
        title("Interview04 — kodowanie na żywo");

        phases();         // phases = fazy rozmowy
        clarify();        // clarify = doprecyzuj wymagania
        v1Quick();        // V1 = szybki szkic
        v2EdgeCases();    // V2 = przypadki brzegowe
        v3Refactor();     // V3 = refaktoryzacja
        v4Final();        // V4 = wersja końcowa z testami
        mistakes();       // mistakes = typowe błędy
        checklist();      // checklist = lista kontrolna (30 punktów)
        softQuestions();  // soft questions = pytania miękkie
        exercises();      // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. FAZY ROZMOWY Z KODOWANIEM
    // =================================================================================================

    /**
     * 1. Budżet czasu na 45 minut: ile przeznaczyć na każdą fazę. Pisanie kodu to tylko jedna z sześciu faz.
     */
    static void phases() {
        section("1. Fazy kodowania na żywo i budżet czasu");

        // Rozmowa z kodowaniem zwykle trwa 30–60 minut. Poniżej przykładowy budżet na 45 minut.
        // Najczęstszy błąd: rzucić się od razu do klawiatury. Kandydat, który 5 minut dopytuje i podaje przykłady,
        // zwykle kończy szybciej i bez błędów — a rekruter widzi, że tak pracujesz na co dzień.
        List<String> budzet = new ArrayList<>();
        int suma = 0;
        for (Phase phase : Phase.values()) {
            budzet.add(phase.label + ": " + phase.minutes + " min");
            suma += phase.minutes;
        }
        showEach("budżet czasu na 45 minut", budzet);
        // WYNIK: budżet czasu na 45 minut (liczba elementów: 6):
        // WYNIK: • Wyjaśnij wymagania i dopytaj: 5 min
        // WYNIK: • Podaj przykłady, także brzegowe: 5 min
        // WYNIK: • Zaplanuj podejście i powiedz złożoność: 5 min
        // WYNIK: • Napisz najprostszą działającą wersję: 15 min
        // WYNIK: • Przetestuj przykłady i przypadki brzegowe: 10 min
        // WYNIK: • Doszlifuj, nazwij i omów usprawnienia: 5 min
        show("suma minut", suma);
        // WYNIK: suma minut → 45

        // ZASADY, KTÓRE STOJĄ ZA FAZAMI:
        //   1. WYJAŚNIJ WYMAGANIA: powtórz treść własnymi słowami, dopytaj o wejście, wyjście, rozmiar danych i błędy.
        //   2. PRZYKŁADY NAJPIERW: zanim napiszesz linijkę, zapisz 2–3 przykłady z oczekiwanym wynikiem (i jeden brzegowy).
        //      Przykłady to Twoje pierwsze testy i dowód, że rozumiesz zadanie.
        //   3. MYŚL NA GŁOS: rekruter nie czyta w myślach. Mów, co rozważasz i czemu wybierasz dane podejście — nawet
        //      gdy się mylisz; ocenia się tok rozumowania, a poprawkę po podpowiedzi uznaje się za plus.
        //   4. ZACZNIJ OD PROSTEGO: najpierw wersja, która działa dla przykładu; optymalizuj dopiero na prośbę lub gdy widzisz problem.
        //   5. TESTUJ: przejdź kod ręcznie na przykładzie, potem przypadki brzegowe (puste, jeden element, błędne dane).
        //   6. REFAKTORYZUJ: poprawa nazw, wydzielenie metod, komentarz, co zrobiłbyś z większą ilością czasu.
        // GDY UTKNIESZ: powiedz to głośno ("zastanawiam się, jak tu zachować kolejność"), wróć do przykładu, uprość problem
        //   (np. najpierw dla jednego klienta). Poproś o wskazówkę — to normalne, a milczenie przez 5 minut jest najgorsze.
        // DOBRA PRAKTYKA: ćwicz na głos — nawet sam, przed lustrem. Mówienie podczas pisania to umiejętność, którą trzeba wytrenować.
    }

    // =================================================================================================
    // 2. DOPRECYZOWANIE WYMAGAŃ
    // =================================================================================================

    /**
     * 2. Zadanie z treści i pytania, które zadajemy przed kodowaniem — każde ma wpływ na konkretną linię kodu.
     */
    static void clarify() {
        section("2. Treść zadania i pytania do rozmówcy");

        // TREŚĆ: "Dostajesz tekst CSV z zamówieniami: klient, produkt, ilość, cena jednostkowa (pierwsza linia to nagłówek).
        //   Policz, ile wydał każdy klient, i wypisz klientów od największej kwoty."
        // Zauważ, ile jest niedopowiedzeń! Dobry kandydat nie pisze kodu — zadaje pytania i zapisuje decyzje.
        show("liczba linii przykładu (nagłówek + 5 zamówień)", SAMPLE.lines().count()); // lines = linie (Java 11+)
        // WYNIK: liczba linii przykładu (nagłówek + 5 zamówień) → 6
        Map<String, String> pytania = new LinkedHashMap<>();
        pytania.put("Czy pierwsza linia to nagłówek?", "tak, pomijam ją");
        pytania.put("Co z pustymi liniami?", "ignoruję, to nie błąd");
        pytania.put("Co z błędną linią (zła liczba, brak pola)?", "pomijam, ale ZGŁASZAM na liście błędów");
        pytania.put("Czy ilość może być 0 lub ujemna?", "nie — to błąd linii");
        pytania.put("Jak zapisana jest cena?", "z kropką; liczę w BigDecimal, nie w double");
        pytania.put("Jak sortować i co przy remisie?", "malejąco po kwocie, przy remisie alfabetycznie");
        pytania.put("Czy Ola i ola to ten sam klient?", "nie, nazwy są rozróżniane i obcinam tylko spacje");
        pytania.put("Jak duże są dane?", "mieszczą się w pamięci; złożoność O(n + k log k)");
        showEach("pytania do rozmówcy → decyzja w kodzie", pytania);
        // WYNIK: pytania do rozmówcy → decyzja w kodzie (liczba kluczy: 8):
        // WYNIK: • Czy pierwsza linia to nagłówek? → tak, pomijam ją
        // WYNIK: • Co z pustymi liniami? → ignoruję, to nie błąd
        // WYNIK: • Co z błędną linią (zła liczba, brak pola)? → pomijam, ale ZGŁASZAM na liście błędów
        // WYNIK: • Czy ilość może być 0 lub ujemna? → nie — to błąd linii
        // WYNIK: • Jak zapisana jest cena? → z kropką; liczę w BigDecimal, nie w double
        // WYNIK: • Jak sortować i co przy remisie? → malejąco po kwocie, przy remisie alfabetycznie
        // WYNIK: • Czy Ola i ola to ten sam klient? → nie, nazwy są rozróżniane i obcinam tylko spacje
        // WYNIK: • Jak duże są dane? → mieszczą się w pamięci; złożoność O(n + k log k)
        // PRZYKŁADY NAJPIERW: dla SAMPLE oczekujemy: Ola 37.75 (2 x 12.50 + 3 x 4.25), Adam 33.00 (8.00 + 2 x 12.50), Ewa 12.50.
        //   Kolejność: Ola, Adam, Ewa. Zapisz to na tablicy/w komentarzu PRZED kodem — później posłuży jako test.
        // DOBRA PRAKTYKA: powiedz na głos swoje założenia ("zakładam, że dane mieszczą się w pamięci"). Rozmówca może je
        //   skorygować, a Ty unikasz zbudowania czegoś, o co nikt nie prosił.
    }

    // =================================================================================================
    // 3. V1 — SZYBKI SZKIC
    // =================================================================================================

    /**
     * 3. V1: pierwszy szkic, który działa na przykładzie. Pokazujemy, GDZIE się łamie — to materiał na rozmowę o ulepszeniach.
     */
    static void v1Quick() {
        section("3. V1 — szybki szkic i jego dziury");

        // MYŚL NA GŁOS: "Zrobię najprostszą wersję: dzielę tekst na linie, pomijam nagłówek, w pętli dodaję kwoty do mapy
        //   klient → suma. Potem sprawdzę, co może się nie udać."
        show("V1 na SAMPLE (posortowane alfabetycznie tylko do wydruku)", new TreeMap<>(v1Totals(SAMPLE)));
        // WYNIK: V1 na SAMPLE (posortowane alfabetycznie tylko do wydruku) → {Adam=33.0, Ewa=12.5, Ola=37.75}
        // Działa na szczęśliwej ścieżce. Teraz własny przegląd kodu — na głos — co jest nie tak:
        //   (a) double do pieniędzy,  (b) HashMap nie ma kolejności, a trzeba sortować wg kwoty,
        //   (c) wszystko w jednej metodzie,  (d) zakłada idealne dane: pusta linia, brak pola, tekst zamiast liczby.
        show("(a) 3 * 0.10 w double", 3 * 0.10);
        // WYNIK: (a) 3 * 0.10 w double → 0.30000000000000004
        show("V1 dla trzech sztuk po 0.10", v1Totals("klient,produkt,ilosc,cena\nAla,Kawa,3,0.10"));
        // WYNIK: V1 dla trzech sztuk po 0.10 → {Ala=0.30000000000000004}
        thrown("(d) V1 na MESSY — pusta linia", () -> v1Totals(MESSY));
        // WYNIK: (d) V1 na MESSY — pusta linia → rzucono ArrayIndexOutOfBoundsException
        thrown("(d) V1 — liczba zapisana słowem", () -> v1Totals("n\nZofia,Kawa,dwa,12.50"));
        // WYNIK: (d) V1 — liczba zapisana słowem → rzucono NumberFormatException
        // PUŁAPKA: double nie zapisuje dokładnie 0.10 (zapis dwójkowy), więc suma ma "ogon" 0.30000000000000004 — w rachunku
        //   pieniężnym to błąd. Używaj BigDecimal (zob. t15_numbers/Numbers01BigDecimal).
        // JAK O TYM MÓWIĆ: "To jest wersja robocza, po to, by mieć działający punkt wyjścia. Zaraz poprawię pieniądze
        //   i obsługę błędów." Rekruter ceni, że sam widzisz braki, zanim on je wskaże.
    }

    // =================================================================================================
    // 4. V2 — PRZYPADKI BRZEGOWE
    // =================================================================================================

    /**
     * 4. V2: obsługa brudnych danych, BigDecimal i BŁĘDY JAKO DANE — zamiast połykania wyjątków zbieramy listę błędów.
     */
    static void v2EdgeCases() {
        section("4. V2 — przypadki brzegowe i błędy jako dane");

        // MYŚL NA GŁOS: "Nie chcę, żeby jedna zła linia zabiła całość, ale nie chcę też milczeć o błędach — zbieram je na listę
        //   z numerem linii. Kwoty w BigDecimal. Podział na linie wyrażeniem \\R, żeby działało też z końcami linii Windows."
        ParseResult wynik = v2Parse(MESSY);
        show("V2 sumy (alfabetycznie do wydruku)", new TreeMap<>(wynik.totals()));
        // WYNIK: V2 sumy (alfabetycznie do wydruku) → {Adam=8.00, Ola=37.75}
        showEach("V2 błędy (z numerem linii)", wynik.errors());
        // WYNIK: V2 błędy (z numerem linii) (liczba elementów: 3):
        // WYNIK: • linia 5: ilość lub cena nie jest liczbą
        // WYNIK: • linia 6: oczekiwano 4 pól, jest 2
        // WYNIK: • linia 8: ilość musi być dodatnia, jest -1
        show("V2 dla trzech sztuk po 0.10", v2Parse("klient,produkt,ilosc,cena\nAla,Kawa,3,0.10").totals());
        // WYNIK: V2 dla trzech sztuk po 0.10 → {Ala=0.30}
        // Co się poprawiło: pusta linia jest ignorowana, spacje obcinane (strip, Java 11+), zła ilość i brak pola trafiają do
        // listy błędów, a kwoty są dokładne. Czego jeszcze brakuje: sortowania wg kwoty (to cecha mapy, nie danych),
        // wszystko dalej w jednej metodzie, a reguły (ilość dodatnia) są wymieszane z parsowaniem.
        // DOBRA PRAKTYKA: błąd w danych wejściowych to normalna sytuacja, nie wyjątek "z kosmosu" — zwróć go jako dane
        //   (lista błędów, Optional, wynik z polami) i pozwól wołającemu zdecydować. Wyjątek jest dla błędów programisty.
    }

    // =================================================================================================
    // 5. V3 — REFAKTORYZACJA
    // =================================================================================================

    /**
     * 5. V3: rozdzielamy odpowiedzialności — parsowanie jednej linii, parsowanie całości, sumowanie i sortowanie.
     */
    static void v3Refactor() {
        section("5. V3 — podział na małe części");

        // MYŚL NA GŁOS: "Wyodrębnię trzy rzeczy: parseLine (jedna linia → obiekt albo wyjątek z opisem), parseAll (cały tekst →
        //   obiekty + błędy) i totalsPerCustomer (sumowanie + sortowanie). Zamówienie to rekord, który sam pilnuje reguł."
        Parsed parsed = parseAll(SAMPLE);
        show("V3: liczba wczytanych zamówień / błędów", parsed.lines().size() + " / " + parsed.errors().size());
        // WYNIK: V3: liczba wczytanych zamówień / błędów → 5 / 0
        show("V3: ranking klientów", summary(totalsPerCustomer(parsed.lines())));
        // WYNIK: V3: ranking klientów → Ola=37.75, Adam=33.00, Ewa=12.50
        show("V3: pojedyncza linia", parseLine("Ola,Kawa,2,12.50"));
        // WYNIK: V3: pojedyncza linia → OrderLine[customer=Ola, product=Kawa, quantity=2, unitPrice=12.50]
        expectThrows("V3: rekord pilnuje reguł (ilość 0)", () -> parseLine("Ola,Kawa,0,12.50"));
        // WYNIK: ✔ V3: rekord pilnuje reguł (ilość 0) → rzucono IllegalArgumentException: ilość musi być dodatnia, jest 0
        expectThrows("V3: czytelny błąd (zły format ceny)", () -> parseLine("Ola,Kawa,1,tanio"));
        // WYNIK: ✔ V3: czytelny błąd (zły format ceny) → rzucono IllegalArgumentException: cena 'tanio' nie jest liczbą
        // Zalety: każdą małą część da się przetestować osobno (parseLine na pojedynczej linii), reguły biznesowe są w jednym
        // miejscu (konstruktor rekordu), a sortowanie jest jawne: malejąco po kwocie, a przy remisie alfabetycznie.
        // Koszt: czas O(n) na wczytanie + O(k log k) na sortowanie k klientów; pamięć O(n).
        // DOBRA PRAKTYKA: metody małe, z nazwą mówiącą CO robią; w jednej metodzie jeden poziom abstrakcji.
        // PUŁAPKA: refaktoryzację zacznij dopiero, gdy kod DZIAŁA i masz testy — inaczej nie wiesz, czy czegoś nie zepsułeś.
    }

    // =================================================================================================
    // 6. V4 — WERSJA KOŃCOWA Z TESTAMI
    // =================================================================================================

    /**
     * 6. V4: raport w jednym wywołaniu, polityka dla null i testy przypadków brzegowych (Check).
     */
    static void v4Final() {
        section("6. V4 — raport i testy przypadków brzegowych");

        // MYŚL NA GŁOS: "Składam całość w jedną metodę buildReport. Zdecyduję o null: rzucam NPE z opisem, bo to błąd wołającego.
        //   Na koniec lista testów: przykład z treści, brudne dane, puste dane, remis, końce linii Windows."
        show("raport SAMPLE", buildReport(SAMPLE).format());
        // WYNIK: raport SAMPLE → [1. Ola — 37.75 zł, 2. Adam — 33.00 zł, 3. Ewa — 12.50 zł]
        Check.equal("przykład z treści", List.of("1. Ola — 37.75 zł", "2. Adam — 33.00 zł", "3. Ewa — 12.50 zł"),
                buildReport(SAMPLE).format());
        // WYNIK: ✔ OK    przykład z treści
        Report messy = buildReport(MESSY);
        Check.equal("brudne dane: liczba błędów", 3, messy.errors().size());
        // WYNIK: ✔ OK    brudne dane: liczba błędów
        Check.equal("brudne dane: pierwszy błąd", "linia 5: ilość 'dwa' nie jest liczbą całkowitą", messy.errors().get(0));
        // WYNIK: ✔ OK    brudne dane: pierwszy błąd
        Check.equal("brudne dane: ranking", "Ola=37.75, Adam=8.00", summary(messy.ranking()));
        // WYNIK: ✔ OK    brudne dane: ranking
        Check.equal("tylko nagłówek", List.of(), buildReport("klient,produkt,ilosc,cena\n").ranking());
        // WYNIK: ✔ OK    tylko nagłówek
        Check.equal("pusty tekst", List.of(), buildReport("").ranking());
        // WYNIK: ✔ OK    pusty tekst
        Check.equal("remis: alfabetycznie", "Adam=10, Bob=10", summary(buildReport("h\nBob,x,1,10\nAdam,x,1,10").ranking()));
        // WYNIK: ✔ OK    remis: alfabetycznie
        Check.equal("końce linii Windows (CRLF)", List.of("1. Ala — 2.50 zł"), buildReport("h\r\nAla,x,1,2.50\r\n").format());
        // WYNIK: ✔ OK    końce linii Windows (CRLF)
        expectThrows("null", () -> buildReport(null));
        // WYNIK: ✔ null → rzucono NullPointerException: csv nie może być null
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 8 OK, ✘ 0 BŁĄD
        // JAK O TYM MÓWIĆ NA KONIEC: "Złożoność: czas O(n + k log k), pamięć O(n). Z większą ilością czasu: wczytywanie strumieniowe
        //   z pliku (dla dużych danych), jawna waluta zamiast napisu 'zł', parametr Locale i testy jednostkowe w JUnit
        //   (t32_junit_mockito/JUnit01Basics)." — taki akapit pokazuje dojrzałość.
        // DOBRA PRAKTYKA: sformatuj liczby z jawnym Locale (String.format(Locale.ROOT, ...)), żeby wynik nie zależał od ustawień komputera.
    }

    // =================================================================================================
    // 7. TYPOWE BŁĘDY
    // =================================================================================================

    /**
     * 7. Trzy błędy, które widać na rozmowach najczęściej: połknięty wyjątek, brak obsługi null i zmienny stan współdzielony.
     */
    static void mistakes() {
        section("7. Typowe błędy: połknięty wyjątek, null, stan współdzielony");

        // BŁĄD 1: pusty catch / catch z "return 0". Program działa dalej z BŁĘDNĄ wartością i nikt się nie dowie.
        show("quantitySilent(\"dwa\") — wynik przy złych danych", quantitySilent("dwa"));
        // WYNIK: quantitySilent("dwa") — wynik przy złych danych → 0
        show("cena zamówienia z cichym błędem (0 szt. x 12.50)",
                BigDecimal.valueOf(quantitySilent("dwa")).multiply(new BigDecimal("12.50")));
        // WYNIK: cena zamówienia z cichym błędem (0 szt. x 12.50) → 0.00
        // Zamówienie "znika": suma jest mniejsza, a w logach nic. Porównaj V2/V3, gdzie błąd trafia na listę z numerem linii.
        // DOBRA PRAKTYKA: łap wyjątek tylko wtedy, gdy masz sensowną reakcję (zgłoszenie błędu, wartość domyślna z logiem);
        //   inaczej niech leci wyżej. Pusty catch to najgorsza decyzja — zob. t10_exceptions/Exceptions07BestPractices.

        // BŁĄD 2: brak decyzji o null. Rekruterzy pytają: "co jeśli wejście to null?". Odpowiedź ma być świadoma:
        //   albo rzucasz NPE/IllegalArgumentException z opisem (błąd wołającego), albo zwracasz wynik pusty (gdy to naturalne).
        thrown("parseLine(null) — NPE bez opisu, jak przypadkiem", () -> parseLine(null));
        // WYNIK: parseLine(null) — NPE bez opisu, jak przypadkiem → rzucono NullPointerException
        expectThrows("buildReport(null) — świadomy wyjątek z opisem", () -> buildReport(null));
        // WYNIK: ✔ buildReport(null) — świadomy wyjątek z opisem → rzucono NullPointerException: csv nie może być null

        // BŁĄD 3: zmienny stan współdzielony (pole static). Drugie wywołanie widzi błędy z pierwszego.
        sharedErrors.clear();
        parseWithSharedErrors(MESSY);
        show("błędy po pierwszym wywołaniu", sharedErrors.size());
        // WYNIK: błędy po pierwszym wywołaniu → 3
        parseWithSharedErrors(MESSY);
        show("błędy po DRUGIM wywołaniu na tych samych danych", sharedErrors.size());
        // WYNIK: błędy po DRUGIM wywołaniu na tych samych danych → 6
        // PUŁAPKA: wynik funkcji zależy od historii wywołań — test raz przechodzi, raz nie (zależy od kolejności), a w programach
        //   wielowątkowych dochodzi wyścig. Rozwiązanie: stan lokalny w metodzie albo wynik zwracany (jak w V2 — ParseResult).
        // INNE CZĘSTE BŁĘDY: double do pieniędzy; indeks "o jeden" (i <= length); modyfikacja kolekcji w pętli for-each;
        //   zmienne a, b, tmp zamiast nazw; kopiowanie kodu zamiast metody; brak testu pustego wejścia; milczenie przy utknięciu.
    }

    // =================================================================================================
    // 8. LISTA KONTROLNA NA 30 PUNKTÓW
    // =================================================================================================

    /**
     * 8. Trzydzieści punktów do odhaczenia przed, w trakcie i po kodowaniu na żywo.
     */
    static void checklist() {
        section("8. Lista kontrolna — 30 punktów");

        Map<String, List<String>> grupy = new LinkedHashMap<>();
        grupy.put("Przed kodowaniem", List.of(
                "Powtórz treść własnymi słowami.",
                "Zapytaj o wejście: format, rozmiar, null, puste, duplikaty.",
                "Zapytaj o wyjście: typ wyniku, sortowanie, co przy błędzie.",
                "Podaj 2–3 przykłady, w tym brzegowy.",
                "Zaproponuj podejście i jego złożoność, zanim zaczniesz pisać.",
                "Zapytaj, czy możesz używać standardowej biblioteki."));
        grupy.put("W trakcie", List.of(
                "Myśl na głos: mów, co robisz i dlaczego.",
                "Zacznij od najprostszej działającej wersji.",
                "Nazywaj zmienne i metody znacząco (nie x, tmp, data2).",
                "Dziel kod na małe metody: parsowanie, logika, wypisywanie.",
                "Nie optymalizuj przedwcześnie — najpierw poprawnie.",
                "Pisz czytelnie: wcięcia, nawiasy, krótkie metody.",
                "Zostaw TODO zamiast milczeć nad trudnym fragmentem i wróć do niego.",
                "Używaj standardowej biblioteki (List, Map, stream), nie wymyślaj kół."));
        grupy.put("Testowanie", List.of(
                "Przejdź kod ręcznie na przykładzie z treści.",
                "Sprawdź puste wejście.",
                "Sprawdź jeden element.",
                "Sprawdź duplikaty i powtórzenia.",
                "Sprawdź wartości graniczne: 0, ujemne, maksimum.",
                "Sprawdź niepoprawne dane (zła liczba, brak pola) i null."));
        grupy.put("Błędy i bezpieczeństwo", List.of(
                "Nigdy nie połykaj wyjątku pustym catch.",
                "Nie używaj double do pieniędzy — BigDecimal.",
                "Nie współdziel zmiennego stanu (pola static) między wywołaniami.",
                "Pilnuj indeksów i przepełnień (o jeden za dużo, int overflow).",
                "Nie modyfikuj kolekcji, po której iterujesz."));
        grupy.put("Komunikacja i zakończenie", List.of(
                "Gdy utkniesz — powiedz to i poproś o wskazówkę, nie siedź w ciszy.",
                "Przyjmuj uwagi spokojnie i wprowadzaj poprawki.",
                "Na koniec podsumuj złożoność czasową i pamięciową.",
                "Wymień, co poprawisz, gdybyś miał więcej czasu.",
                "Zapytaj, jak rozmówca rozwiązałby zadanie — to okazja do nauki."));
        List<String> wszystkie = new ArrayList<>();
        int numer = 1;
        for (List<String> grupa : grupy.values()) {
            for (String punkt : grupa) {
                wszystkie.add(numer++ + ". " + punkt);
            }
        }
        show("liczba punktów na liście", wszystkie.size());
        // WYNIK: liczba punktów na liście → 30
        show("grupy i liczba punktów", grupy.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue().size()).collect(Collectors.joining(", ")));
        // WYNIK: grupy i liczba punktów → Przed kodowaniem=6, W trakcie=8, Testowanie=6, Błędy i bezpieczeństwo=5, Komunikacja i zakończenie=5
        showEach("lista kontrolna", wszystkie);
        // WYNIK: lista kontrolna (liczba elementów: 30):
        // WYNIK: • 1. Powtórz treść własnymi słowami.
        // WYNIK: • 2. Zapytaj o wejście: format, rozmiar, null, puste, duplikaty.
        // WYNIK: • 3. Zapytaj o wyjście: typ wyniku, sortowanie, co przy błędzie.
        // WYNIK: • 4. Podaj 2–3 przykłady, w tym brzegowy.
        // WYNIK: • 5. Zaproponuj podejście i jego złożoność, zanim zaczniesz pisać.
        // WYNIK: • 6. Zapytaj, czy możesz używać standardowej biblioteki.
        // WYNIK: • 7. Myśl na głos: mów, co robisz i dlaczego.
        // WYNIK: • 8. Zacznij od najprostszej działającej wersji.
        // WYNIK: • 9. Nazywaj zmienne i metody znacząco (nie x, tmp, data2).
        // WYNIK: • 10. Dziel kod na małe metody: parsowanie, logika, wypisywanie.
        // WYNIK: • 11. Nie optymalizuj przedwcześnie — najpierw poprawnie.
        // WYNIK: • 12. Pisz czytelnie: wcięcia, nawiasy, krótkie metody.
        // WYNIK: • 13. Zostaw TODO zamiast milczeć nad trudnym fragmentem i wróć do niego.
        // WYNIK: • 14. Używaj standardowej biblioteki (List, Map, stream), nie wymyślaj kół.
        // WYNIK: • 15. Przejdź kod ręcznie na przykładzie z treści.
        // WYNIK: • 16. Sprawdź puste wejście.
        // WYNIK: • 17. Sprawdź jeden element.
        // WYNIK: • 18. Sprawdź duplikaty i powtórzenia.
        // WYNIK: • 19. Sprawdź wartości graniczne: 0, ujemne, maksimum.
        // WYNIK: • 20. Sprawdź niepoprawne dane (zła liczba, brak pola) i null.
        // WYNIK: • 21. Nigdy nie połykaj wyjątku pustym catch.
        // WYNIK: • 22. Nie używaj double do pieniędzy — BigDecimal.
        // WYNIK: • 23. Nie współdziel zmiennego stanu (pola static) między wywołaniami.
        // WYNIK: • 24. Pilnuj indeksów i przepełnień (o jeden za dużo, int overflow).
        // WYNIK: • 25. Nie modyfikuj kolekcji, po której iterujesz.
        // WYNIK: • 26. Gdy utkniesz — powiedz to i poproś o wskazówkę, nie siedź w ciszy.
        // WYNIK: • 27. Przyjmuj uwagi spokojnie i wprowadzaj poprawki.
        // WYNIK: • 28. Na koniec podsumuj złożoność czasową i pamięciową.
        // WYNIK: • 29. Wymień, co poprawisz, gdybyś miał więcej czasu.
        // WYNIK: • 30. Zapytaj, jak rozmówca rozwiązałby zadanie — to okazja do nauki.
        // DOBRA PRAKTYKA: wydrukuj listę i przećwicz kodowanie na głos z kimś (albo nagraj się) — dwa-trzy zadania z tą listą
        //   w ręku wystarczą, żeby zaczęła wchodzić w nawyk.
    }

    // =================================================================================================
    // 9. PYTANIA MIĘKKIE
    // =================================================================================================

    /**
     * 9. Pytania o projekty, uczenie się i porażki — krótko, z jedną radą: bądź szczery i konkretny.
     */
    static void softQuestions() {
        section("9. Pytania miękkie — krótko i szczerze");

        Map<String, String> rady = new LinkedHashMap<>();
        rady.put("Opowiedz o swoim projekcie.", "jeden projekt: co robił, jaka była Twoja rola, czego się nauczyłeś; znaj każdą linijkę");
        rady.put("Jaki był Twój największy błąd?", "prawdziwy błąd, jak go naprawiłeś i co zmieniłeś na przyszłość");
        rady.put("Jak się uczysz?", "konkretnie: kursy, własne projekty, czytanie cudzego kodu, ćwiczenia — i jak to utrwalasz");
        rady.put("Czego nie wiesz?", "przyznaj to wprost i powiedz, jak byś się dowiedział; szczerość bije zgadywanie");
        rady.put("Dlaczego chcesz u nas pracować?", "zrób research (produkt, technologie) i powiedz własnymi słowami");
        rady.put("Gdzie widzisz się za pięć lat?", "uczciwie: rozwój techniczny i większa odpowiedzialność, bez przesady");
        showEach("pytanie → rada", rady);
        // WYNIK: pytanie → rada (liczba kluczy: 6):
        // WYNIK: • Opowiedz o swoim projekcie. → jeden projekt: co robił, jaka była Twoja rola, czego się nauczyłeś; znaj każdą linijkę
        // WYNIK: • Jaki był Twój największy błąd? → prawdziwy błąd, jak go naprawiłeś i co zmieniłeś na przyszłość
        // WYNIK: • Jak się uczysz? → konkretnie: kursy, własne projekty, czytanie cudzego kodu, ćwiczenia — i jak to utrwalasz
        // WYNIK: • Czego nie wiesz? → przyznaj to wprost i powiedz, jak byś się dowiedział; szczerość bije zgadywanie
        // WYNIK: • Dlaczego chcesz u nas pracować? → zrób research (produkt, technologie) i powiedz własnymi słowami
        // WYNIK: • Gdzie widzisz się za pięć lat? → uczciwie: rozwój techniczny i większa odpowiedzialność, bez przesady
        // ZASADY: (1) BĄDŹ SZCZERY — nie wpisuj w CV technologii, której nie znasz; jedno pytanie o szczegóły i wszystko się
        //   sypie. (2) Mów o SWOJEJ roli ("zrobiłem"), a nie tylko o zespole ("zrobiliśmy"). (3) Przy porażce pokaż wnioski.
        //   (4) Przygotuj 2–3 własne pytania do firmy (jak wygląda code review, jak wdraża się zmiany, jak wygląda wdrożenie
        //   juniora). (5) Nie krytykuj poprzednich pracodawców. (6) Jeśli dużo się uczysz z kursów — pokaż własny kod na GitHubie.
        // DOBRA PRAKTYKA: napisz odpowiedzi na te sześć pytań, przeczytaj na głos i skróć każdą do minuty.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Fazy: wyjaśnij → przykłady → plan i złożoność → najprostsza wersja → testy → szlify. Kod to ~1/3 czasu.
     *   • Dopytuj o: format wejścia, błędne dane, null, puste, rozmiar, sortowanie i remis, typ wyniku.
     *   • Przykłady zapisz PRZED kodem — to Twoje pierwsze testy.
     *   • Myśl na głos; gdy utkniesz, powiedz to i poproś o wskazówkę.
     *   • V1 szkic → V2 przypadki brzegowe + błędy jako dane → V3 małe części → V4 raport + testy.
     *   • Pieniądze: BigDecimal; błędy danych: lista błędów z numerem linii; wyjątek: tylko gdy masz reakcję.
     *   • Błędy z rozmów: połknięty wyjątek, brak decyzji o null, pole static ze stanem, double do pieniędzy.
     *   • Wynik funkcji nie może zależeć od historii wywołań (współdzielony stan).
     *   • Na koniec: złożoność czasu i pamięci oraz co poprawiłbyś z większą ilością czasu.
     *   • Pytania miękkie: szczerość, konkret, własna rola, wnioski z porażek, własne pytania do firmy.
     *
     * PYTANIA KONTROLNE:
     *   1. Wymień trzy rzeczy, które robisz, zanim napiszesz pierwszą linijkę kodu na rozmowie.
     *   2. Co wypisze:  System.out.println(3 * 0.10);  i dlaczego nie nadaje się to do rachunku pieniędzy?
     *   3. ZNAJDŹ BŁĄD:  try { qty = Integer.parseInt(s); } catch (NumberFormatException e) { qty = 0; }
     *   4. ZNAJDŹ BŁĄD:  static List<String> errors = new ArrayList<>();  używana jako lista błędów w metodzie parse(text).
     *   5. Co wypisze:  new BigDecimal("0.1").multiply(BigDecimal.valueOf(3))  ?
     *   6. Co zrobisz, gdy utkniesz w połowie zadania?
     *   7. Dlaczego warto podać przykłady przed napisaniem kodu?
     *   8. Rekruter pyta o Twój największy błąd. Jak odpowiesz?
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
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    private static void checkAll(boolean sol) {
        List<String> inputs = Arrays.asList("2", " 3 ", "dwa", "-1", "0", "", null);
        Check.equal("ćw. 1: parseQuantity",
                List.of(Optional.of(2), Optional.of(3), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty()),
                () -> {
                    List<Optional<Integer>> out = new ArrayList<>();
                    for (String in : inputs) {
                        out.add(sol ? solution1(in) : exercise1(in));
                    }
                    return out;
                });
        Check.equal("ćw. 2: lineTotal", List.of("0.30", "25.00", "12.75"),
                () -> sol
                        ? List.of(solution2("3", "0.10"), solution2("2", "12.50"), solution2("3", "4.25"))
                        : List.of(exercise2("3", "0.10"), exercise2("2", "12.50"), exercise2("3", "4.25")));
        Map<String, BigDecimal> totals = new HashMap<>();
        totals.put("Ola", new BigDecimal("37.75"));
        totals.put("Adam", new BigDecimal("33.00"));
        totals.put("Ewa", new BigDecimal("12.50"));
        totals.put("Bob", new BigDecimal("33.00"));
        Check.equal("ćw. 3: topCustomers (remis alfabetycznie)", List.of("Ola", "Adam", "Bob"),
                () -> sol ? solution3(totals, 3) : exercise3(totals, 3));
        String input = """
                klient,produkt,ilosc,cena
                Ola,Kawa,2,12.50
                Adam,Herbata,1,8.00
                Ola,Ciastko,3,4.25
                Ewa,Kawa,1,12.50
                Adam,Kawa,2,12.50

                Zofia,Kawa,dwa,12.50
                Jan,Kawa
                """;
        Check.equal("ćw. 4: productQuantities", "Kawa=5;Ciastko=3;Herbata=1;errors=2",
                () -> sol ? solution4(input) : exercise4(input));
    }

    /**
     * ĆWICZENIE 1 (łatwe): zamień tekst na ilość: obcinaj spacje, akceptuj tylko liczby CAŁKOWITE DODATNIE. Dla "dwa", "-1", "0",
     * pustego tekstu i null zwróć Optional.empty() — to JAWNY sygnał "nie da się", a nie połknięty wyjątek.
     * Podpowiedź: Integer.parseInt w try, a w catch NumberFormatException zwróć Optional.empty().
     */
    static Optional<Integer> exercise1(String text) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ rachunek z double na BigDecimal i zwróć kwotę linii jako napis z dwoma miejscami po przecinku
     * (zaokrąglenie HALF_UP).
     * <pre>{@code
     * double total = Integer.parseInt(quantity) * Double.parseDouble(price);   // 3 * 0.10 = 0.30000000000000004
     * return String.valueOf(total);
     * }</pre>
     * Podpowiedź: new BigDecimal(price).multiply(new BigDecimal(quantity)).setScale(2, RoundingMode.HALF_UP).toPlainString().
     */
    static String exercise2(String quantity, String price) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć imiona n klientów o największych sumach (malejąco); przy równych sumach — alfabetycznie.
     * Podpowiedź: strumień wpisów mapy, komparator: {@code Map.Entry.<String, BigDecimal>comparingByValue().reversed()}
     * (typy podane jawnie, bo kompilator sam ich nie odgadnie przy reversed), potem {@code thenComparing(Map.Entry.comparingByKey())},
     * na końcu limit(n) i przekształcenie na imiona.
     */
    static List<String> exercise3(Map<String, BigDecimal> totals, int n) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): to samo zadanie CSV, ale zlicz sztuki per PRODUKT. Zwróć napis w postaci
     * "Kawa=5;Ciastko=3;Herbata=1;errors=2": produkty od największej liczby sztuk (remis alfabetycznie), na końcu liczba
     * błędnych linii. Pomijaj nagłówek i puste linie; błędna linia (nie 4 pola, ilość nie jest dodatnią liczbą) zwiększa licznik błędów.
     * Podpowiedź: wykorzystaj kroki z lekcji — najpierw przykład na papierze, potem najprostsza wersja, potem testy brzegowe.
     */
    static String exercise4(String csv) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static Optional<Integer> solution1(String text) {
        if (text == null) {
            return Optional.empty();
        }
        try {
            int value = Integer.parseInt(text.strip());
            return value > 0 ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    static String solution2(String quantity, String price) {
        return new BigDecimal(price).multiply(new BigDecimal(quantity)).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    static List<String> solution3(Map<String, BigDecimal> totals, int n) {
        return totals.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(n)
                .map(Map.Entry::getKey)
                .toList();
    }

    static String solution4(String csv) {
        Map<String, Integer> quantities = new HashMap<>();
        int errors = 0;
        for (String raw : csv.lines().skip(1).toList()) {
            String line = raw.strip();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split(",");
            if (parts.length != 4) {
                errors++;
                continue;
            }
            Optional<Integer> quantity = solution1(parts[2]);
            if (quantity.isEmpty()) {
                errors++;
            } else {
                quantities.merge(parts[1].strip(), quantity.get(), Integer::sum);
            }
        }
        String body = quantities.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(";"));
        return body + ";errors=" + errors;
    }

    // </editor-fold>

    // =================================================================================================
    // TYPY I METODY POMOCNICZE (dla zadania z lekcji)
    // =================================================================================================

    enum Phase {
        CLARIFY("Wyjaśnij wymagania i dopytaj", 5),
        EXAMPLES("Podaj przykłady, także brzegowe", 5),
        PLAN("Zaplanuj podejście i powiedz złożoność", 5),
        CODE("Napisz najprostszą działającą wersję", 15),
        TEST("Przetestuj przykłady i przypadki brzegowe", 10),
        REFACTOR("Doszlifuj, nazwij i omów usprawnienia", 5);

        final String label;
        final int minutes;

        Phase(String label, int minutes) {
            this.label = label;
            this.minutes = minutes;
        }
    }

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

    // ---- V1: pierwszy szkic (celowo z dziurami) ----

    static Map<String, Double> v1Totals(String csv) {
        Map<String, Double> totals = new HashMap<>();
        String[] lines = csv.split("\n");
        for (int i = 1; i < lines.length; i++) {
            String[] p = lines[i].split(",");
            double total = Integer.parseInt(p[2]) * Double.parseDouble(p[3]);
            totals.put(p[0], totals.getOrDefault(p[0], 0.0) + total);
        }
        return totals;
    }

    // ---- V2: przypadki brzegowe, błędy jako dane ----

    record ParseResult(Map<String, BigDecimal> totals, List<String> errors) { }

    static ParseResult v2Parse(String csv) {
        Map<String, BigDecimal> totals = new HashMap<>();
        List<String> errors = new ArrayList<>();
        String[] lines = csv.split("\\R");
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].strip();
            if (line.isEmpty()) {
                continue;
            }
            String[] p = line.split(",");
            if (p.length != 4) {
                errors.add("linia " + (i + 1) + ": oczekiwano 4 pól, jest " + p.length);
                continue;
            }
            try {
                int quantity = Integer.parseInt(p[2].strip());
                BigDecimal price = new BigDecimal(p[3].strip());
                if (quantity <= 0) {
                    errors.add("linia " + (i + 1) + ": ilość musi być dodatnia, jest " + quantity);
                    continue;
                }
                totals.merge(p[0].strip(), price.multiply(BigDecimal.valueOf(quantity)), BigDecimal::add);
            } catch (NumberFormatException e) {
                errors.add("linia " + (i + 1) + ": ilość lub cena nie jest liczbą");
            }
        }
        return new ParseResult(totals, errors);
    }

    // ---- V3: rekordy i małe metody ----

    record OrderLine(String customer, String product, int quantity, BigDecimal unitPrice) {
        OrderLine {
            if (customer.isBlank()) {
                throw new IllegalArgumentException("pusta nazwa klienta");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("ilość musi być dodatnia, jest " + quantity);
            }
            if (unitPrice.signum() < 0) {
                throw new IllegalArgumentException("cena nie może być ujemna");
            }
        }

        BigDecimal total() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    record CustomerTotal(String customer, BigDecimal total) { }

    record Parsed(List<OrderLine> lines, List<String> errors) { }

    static OrderLine parseLine(String line) {
        String[] p = line.split(",");
        if (p.length != 4) {
            throw new IllegalArgumentException("oczekiwano 4 pól, jest " + p.length);
        }
        int quantity;
        try {
            quantity = Integer.parseInt(p[2].strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ilość '" + p[2].strip() + "' nie jest liczbą całkowitą", e);
        }
        BigDecimal price;
        try {
            price = new BigDecimal(p[3].strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("cena '" + p[3].strip() + "' nie jest liczbą", e);
        }
        return new OrderLine(p[0].strip(), p[1].strip(), quantity, price);
    }

    static Parsed parseAll(String csv) {
        List<OrderLine> lines = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int lineNumber = 0;
        for (String raw : csv.lines().toList()) {
            lineNumber++;
            if (lineNumber == 1) {
                continue;
            }
            String line = raw.strip();
            if (line.isEmpty()) {
                continue;
            }
            try {
                lines.add(parseLine(line));
            } catch (IllegalArgumentException e) {
                errors.add("linia " + lineNumber + ": " + e.getMessage());
            }
        }
        return new Parsed(List.copyOf(lines), List.copyOf(errors));
    }

    static List<CustomerTotal> totalsPerCustomer(List<OrderLine> lines) {
        Map<String, BigDecimal> sums = new HashMap<>();
        for (OrderLine line : lines) {
            sums.merge(line.customer(), line.total(), BigDecimal::add);
        }
        return sums.entrySet().stream()
                .map(e -> new CustomerTotal(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(CustomerTotal::total).reversed().thenComparing(CustomerTotal::customer))
                .toList();
    }

    static String summary(List<CustomerTotal> ranking) {
        return ranking.stream().map(t -> t.customer() + "=" + t.total().toPlainString()).collect(Collectors.joining(", "));
    }

    // ---- V4: raport ----

    record Report(List<CustomerTotal> ranking, List<String> errors) {
        List<String> format() {
            List<String> out = new ArrayList<>();
            int rank = 1;
            for (CustomerTotal t : ranking) {
                out.add(String.format(Locale.ROOT, "%d. %s — %s zł", rank++, t.customer(), t.total().toPlainString()));
            }
            if (!errors.isEmpty()) {
                out.add("błędy (" + errors.size() + "): " + errors);
            }
            return out;
        }
    }

    static Report buildReport(String csv) {
        Objects.requireNonNull(csv, "csv nie może być null");
        Parsed parsed = parseAll(csv);
        return new Report(totalsPerCustomer(parsed.lines()), parsed.errors());
    }

    // ---- typowe błędy ----

    static int quantitySilent(String text) {
        try {
            return Integer.parseInt(text.strip());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static final List<String> sharedErrors = new ArrayList<>();

    static void parseWithSharedErrors(String csv) {
        String[] lines = csv.split("\\R");
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            try {
                parseLine(lines[i].strip());
            } catch (IllegalArgumentException e) {
                sharedErrors.add("linia " + (i + 1) + ": " + e.getMessage());
            }
        }
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Na przykład: powtórzyć treść własnymi słowami, dopytać o wymagania (format, błędy, null, rozmiar, sortowanie),
     *      podać przykłady z oczekiwanym wynikiem (także brzegowy), zaproponować podejście i złożoność.
     *   2. 0.30000000000000004 — double zapisuje liczby w układzie dwójkowym i 0.10 nie da się przedstawić dokładnie, więc wynik ma
     *      "ogon" błędu; do pieniędzy służy BigDecimal (widać w sekcji 3).
     *   3. Pusty/łagodny catch ustawia qty = 0 i program dalej liczy z błędną wartością, a nikt się o tym nie dowie
     *      (cichy błąd). Lepiej zgłosić błąd (lista błędów, Optional.empty, wyjątek z opisem) — sekcja 7.
     *   4. Pole static to współdzielony stan: kolejne wywołania dopisują do tej samej listy (w sekcji 7: 3 błędy, potem 6),
     *      więc wynik zależy od historii wywołań i wątków. Błędy zwracaj w wyniku metody lub trzymaj w zmiennej lokalnej.
     *   5. 0.3 — new BigDecimal("0.1") ma skalę 1, a BigDecimal.valueOf(3) skalę 0, iloczyn ma skalę 1 + 0 = 1, czyli 0.3
     *      (dokładnie, bez błędu double).
     *   6. Powiedzieć to głośno, wrócić do przykładu, uprościć problem (np. najpierw jeden klient albo jedna linia),
     *      poprosić o wskazówkę. Nie milczeć.
     *   7. Przykłady dowodzą, że rozumiesz zadanie, wychwytują niedopowiedzenia zanim napiszesz błędny kod i stają się
     *      pierwszymi testami.
     *   8. Opisz prawdziwy błąd, konkretnie: co zrobiłeś, jak go znalazłeś i naprawiłeś oraz co zmieniłeś, żeby się nie
     *      powtórzył (test, code review, lista kontrolna). Nie wymyślaj i nie obwiniaj innych.
     */
    // </editor-fold>
}
