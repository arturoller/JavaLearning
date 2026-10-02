package t33_interview_prep;

import helpers.Check;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Klasyczne zadania programistyczne z rozmów kwalifikacyjnych
 *        (coding task = zadanie programistyczne; naive = naiwne; improved = ulepszone; edge case = przypadek brzegowy)
 *
 * W SKRÓCIE:
 *   Siedem klasyków (FizzBuzz, odwracanie napisu, palindrom, anagram, pierwszy niepowtarzalny znak, two-sum,
 *   nawiasy) — każdy w wersji NAIWNEJ i ULEPSZONEJ, ze złożonością, listą przypadków brzegowych, testami (Check)
 *   i wskazówką, jak o rozwiązaniu mówić. Kolejne zadania (liczenie słów, duplikaty, scalanie, wyszukiwanie
 *   binarne, najlepszy zysk) są ćwiczeniami do samodzielnego rozwiązania.
 *
 * ANALOGIA: rozmowa z kodowaniem to egzamin na prawo jazdy, nie wyścig. Egzaminator ocenia, czy rozglądasz się
 *   (przypadki brzegowe), jedziesz zgodnie z przepisami (czytelny kod) i mówisz, co robisz — a nie to, czy pojedziesz
 *   najszybciej. Wolna, ale poprawna jazda wygrywa z szybką, która kończy się na krawężniku.
 *
 * JAK TO DZIAŁA:
 *   Schemat odpowiedzi do KAŻDEGO zadania:
 *     1. Powtórz treść własnymi słowami i dopytaj o ograniczenia (null? puste? duże dane? wielkość liter?).
 *     2. Podaj 2–3 przykłady, w tym brzegowy. 3. Zaproponuj wersję naiwną i jej koszt (czas/pamięć).
 *     4. Ulepsz (inna struktura danych: HashMap, Deque, dwa wskaźniki). 5. Napisz kod, 6. przetestuj na przykładach.
 *   Złożoność: czas = ile kroków rośnie z danymi (n); pamięć = ile dodatkowego miejsca. O(n) czytaj "proporcjonalnie do n".
 *
 * SŁÓWKA:
 *   naive = naiwny; improved = ulepszony; edge case = przypadek brzegowy; two pointers = dwa wskaźniki;
 *   stack = stos; anagram = anagram (te same litery); palindrome = palindrom; complexity = złożoność;
 *   brute force = siłowo (sprawdzenie wszystkiego); code point = punkt kodowy Unicode.
 *
 * ZOBACZ TEŻ: t24_algorithms/Algorithms05Classics, t24_algorithms/Algorithms01Complexity, t04_strings/Strings06CharUnicode,
 *   t12_collections/Collections10Patterns, t33_interview_prep/Interview04LiveCoding
 * </pre>
 */
public class Interview03CodingTasks {

    public static void main(String[] args) {
        title("Interview03 — klasyczne zadania programistyczne");

        fizzBuzz();            // fizz buzz = klasyk z modułem (zadanie 1)
        reversing();           // reversing = odwracanie (zadanie 2)
        palindromes();         // palindromes = palindromy (zadanie 3)
        anagrams();            // anagrams = anagramy (zadanie 4)
        firstUnique();         // first unique = pierwszy niepowtarzalny (zadanie 5)
        twoSum();              // two sum = dwie liczby o danej sumie (zadanie 6)
        brackets();            // brackets = nawiasy (zadanie 7)
        differentialTesting(); // differential testing = testowanie jednej wersji drugą
        countingOperations();  // counting operations = zliczanie operacji (złożoność w praktyce)
        testSummary();         // test summary = podsumowanie testów
        exercises();           // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. FIZZBUZZ ★
    // =================================================================================================

    /**
     * 1. ZADANIE 1 ★ FizzBuzz: dla liczb od 1 do n zwróć "Fizz" (podzielna przez 3), "Buzz" (przez 5),
     * "FizzBuzz" (przez oba) albo samą liczbę.
     */
    static void fizzBuzz() {
        section("1. FizzBuzz ★ — kolejność warunków i rozszerzalność");

        // ZADANIE 1 ★ FizzBuzz   (→ t02_controlflow/Control05LoopPatterns)
        // DLACZEGO TO PYTAJĄ: to filtr — sprawdza, czy kandydat w ogóle napisze pętlę, użyje reszty z dzielenia (%)
        //   i zauważy, że warunek "oba" musi być sprawdzony PIERWSZY (inaczej 15 da samo "Fizz").
        // V1 (naiwna): łańcuch if/else if, najpierw "podzielna przez 3 i 5". Czas O(n), pamięć O(n) na wynik.
        // V2 (ulepszona): reguły w mapie (LinkedHashMap = mapa z zachowaną kolejnością) — nowa reguła to nowy wpis,
        //   nie nowy if (zasada otwarte/zamknięte). Napis budujemy z kawałków, więc "FizzBuzz" wychodzi samo.
        // PRZYPADKI BRZEGOWE: n = 0 i n ujemne → pusta lista; n = 1 → ["1"]; bardzo duże n (pamięć na wynik!).
        // JAK O TYM MÓWIĆ: "Zaczynam od najprostszej wersji, sprawdzam 15 przed 3 i 5. Jeśli reguł będzie więcej,
        //   wyniosę je do mapy."
        show("V1 dla n = 15", fizzBuzzNaive(15));
        // WYNIK: V1 dla n = 15 → [1, 2, Fizz, 4, Buzz, Fizz, 7, 8, Fizz, Buzz, 11, Fizz, 13, 14, FizzBuzz]
        show("zła kolejność warunków, ostatni element dla n = 15", fizzBuzzWrongOrder(15).get(14));
        // WYNIK: zła kolejność warunków, ostatni element dla n = 15 → Fizz
        Map<Integer, String> rules = new LinkedHashMap<>();
        rules.put(3, "Fizz");
        rules.put(5, "Buzz");
        rules.put(7, "Bazz");
        show("V2 z regułami 3/5/7 dla n = 15 (nowa reguła bez nowego if)", fizzBuzzRules(15, rules));
        // WYNIK: V2 z regułami 3/5/7 dla n = 15 (nowa reguła bez nowego if) → [1, 2, Fizz, 4, Buzz, Fizz, Bazz, 8, Fizz, Buzz, 11, Fizz, 13, Bazz, FizzBuzz]
        Check.equal("FizzBuzz n = 0", List.of(), fizzBuzzNaive(0));
        // WYNIK: ✔ OK    FizzBuzz n = 0
        Check.equal("FizzBuzz n = -3", List.of(), fizzBuzzNaive(-3));
        // WYNIK: ✔ OK    FizzBuzz n = -3
        Map<Integer, String> classic = new LinkedHashMap<>();
        classic.put(3, "Fizz");
        classic.put(5, "Buzz");
        Check.equal("V1 i V2 zgodne dla n = 100", fizzBuzzNaive(100), fizzBuzzRules(100, classic));
        // WYNIK: ✔ OK    V1 i V2 zgodne dla n = 100
        Check.equal("wersja strumieniowa zgodna", fizzBuzzNaive(30), fizzBuzzStream(30));
        // WYNIK: ✔ OK    wersja strumieniowa zgodna
        // PUŁAPKA: i % 3 == 0 && i % 5 == 0 można zapisać krócej jako i % 15 == 0 — ale tylko dlatego, że 3 i 5 są względnie pierwsze.
        //   Dla reguł 4 i 6 trzeba użyć NWW (12), nie iloczynu (24).
        // DOBRA PRAKTYKA: wynik zwracaj jako dane (List<String>), a nie wypisuj w środku — wtedy da się go przetestować.
    }

    // =================================================================================================
    // 2. ODWRACANIE NAPISU I SŁÓW ★
    // =================================================================================================

    /**
     * 2. ZADANIE 2 ★ Odwróć napis, a potem kolejność słów w zdaniu.
     */
    static void reversing() {
        section("2. Odwracanie napisu i słów ★");

        // ZADANIE 2 ★ Odwróć napis i odwróć słowa   (→ t04_strings/Strings07TextAlgorithms)
        // DLACZEGO TO PYTAJĄ: sprawdza znajomość String (niezmienność), StringBuilder i uwagę na przypadki brzegowe.
        // V1 (naiwna): pętla i  wynik = znak + wynik  — każdy obrót tworzy NOWY napis, więc czas O(n²).
        // V2 (ulepszona): StringBuilder.reverse() — O(n); obsługuje też znaki spoza podstawowej płaszczyzny (pary zastępcze).
        // PUŁAPKA: ręczne odwracanie tablicy char (dwa wskaźniki) działa dla zwykłych liter, ale rozdziela PARĘ ZASTĘPCZĄ
        //   (znak Unicode zapisany w dwóch char, np. emotikona) — wynik jest uszkodzony.
        // PRZYPADKI BRZEGOWE: "" → "", jeden znak, spacje na początku/końcu i podwójne spacje (słowa), null (ustal z rozmówcą:
        //   NPE czy ""?). W tej lekcji null nie jest dozwolony.
        show("V1 \"ala ma\"", reverseNaive("ala ma"));
        // WYNIK: V1 "ala ma" → am ala
        show("V2 \"ala ma\"", reverseBuilder("ala ma"));
        // WYNIK: V2 "ala ma" → am ala
        String emotka = new String(Character.toChars(0x1F600));
        String zEmotka = "a" + emotka + "b";
        show("długość w char / w punktach kodowych", zEmotka.length() + " / " + zEmotka.codePointCount(0, zEmotka.length()));
        // WYNIK: długość w char / w punktach kodowych → 4 / 3
        show("StringBuilder.reverse zachowuje parę zastępczą", reverseBuilder(zEmotka).codePointAt(1) == 0x1F600);
        // WYNIK: StringBuilder.reverse zachowuje parę zastępczą → true
        show("ręczne odwracanie char[] ją rozbija", reverseChars(zEmotka).codePointAt(1) == 0x1F600);
        // WYNIK: ręczne odwracanie char[] ją rozbija → false
        Check.equal("odwrócony pusty napis", "", reverseBuilder(""));
        // WYNIK: ✔ OK    odwrócony pusty napis
        Check.equal("V1 i V2 zgodne", reverseNaive("Kajak i Java"), reverseBuilder("Kajak i Java"));
        // WYNIK: ✔ OK    V1 i V2 zgodne
        // Odwracanie SŁÓW: "ala ma kota" → "kota ma ala". Podział na wyrazy po jednej lub wielu spacjach: split("\\s+").
        // V1: split + Collections.reverse + String.join. V2: pętla od końca i StringBuilder (mniej obiektów pośrednich).
        // Oba O(n). PUŁAPKA: split(" ") dla "a  b" zwraca puste słowo między spacjami; nieobcięty tekst zaczynający
        // się spacją daje puste słowo na początku — dlatego strip() (Java 11+) i wyrażenie regularne \\s+.
        Check.equal("słowa V1", "kota ma ala", reverseWordsList("ala ma kota"));
        // WYNIK: ✔ OK    słowa V1
        Check.equal("słowa V2", "kota ma ala", reverseWordsBuilder("ala ma kota"));
        // WYNIK: ✔ OK    słowa V2
        Check.equal("słowa: nadmiarowe spacje", "kota ma ala", reverseWordsList("  ala   ma kota "));
        // WYNIK: ✔ OK    słowa: nadmiarowe spacje
        Check.equal("słowa: puste", "", reverseWordsBuilder("   "));
        // WYNIK: ✔ OK    słowa: puste
        Check.equal("słowa: jedno słowo", "ala", reverseWordsBuilder("ala"));
        // WYNIK: ✔ OK    słowa: jedno słowo
        // JAK O TYM MÓWIĆ: "W Javie String jest niezmienny, więc 'w miejscu' mogę odwracać tylko tablicę char. Dla napisu
        //   użyję StringBuilder; zwrócę uwagę na punkty kodowe, jeśli tekst może zawierać emoji."
    }

    // =================================================================================================
    // 3. PALINDROM ★★
    // =================================================================================================

    /**
     * 3. ZADANIE 3 ★★ Czy tekst jest palindromem (czyta się tak samo od przodu i od tyłu), ignorując wielkość liter i znaki
     * niebędące literami ani cyframi?
     */
    static void palindromes() {
        section("3. Palindrom ★★ — i dlaczego Unicode komplikuje sprawę");

        // ZADANIE 3 ★★ Palindrom   (→ t04_strings/Strings06CharUnicode)
        // DLACZEGO TO PYTAJĄ: dwa wskaźniki to wzorzec, który wraca w wielu zadaniach; a "ignoruj interpunkcję" sprawdza,
        //   czy kandydat dopyta o wymagania.
        // V1 (naiwna): oczyść tekst, odwróć i porównaj z oryginałem — czas O(n), pamięć O(n) (dwie kopie).
        // V2 (ulepszona): dwa wskaźniki (od lewej i od prawej), omijanie znaków niealfanumerycznych — czas O(n), pamięć O(1).
        // V3 (poprawna dla Unicode): to samo na PUNKTACH KODOWYCH (codePoints), bo znak spoza podstawowej płaszczyzny to dwa char.
        // PRZYPADKI BRZEGOWE: "" i jeden znak → true (z definicji), same znaki specjalne ("?!") → true (po oczyszczeniu pusty),
        //   duże/małe litery ("Kajak"), cyfry, polskie litery ("Kobyła ma mały bok" to znany polski palindrom — po pominięciu
        //   spacji i zignorowaniu wielkości liter czyta się tak samo wspak; zawiera literę ł).
        Check.equal("Kajak", true, palindromeNaive("Kajak"));
        // WYNIK: ✔ OK    Kajak
        Check.equal("A man, a plan, a canal: Panama", true, palindromeTwoPointers("A man, a plan, a canal: Panama"));
        // WYNIK: ✔ OK    A man, a plan, a canal: Panama
        Check.equal("pusty napis", true, palindromeTwoPointers(""));
        // WYNIK: ✔ OK    pusty napis
        Check.equal("?! (same znaki specjalne)", true, palindromeTwoPointers("?!"));
        // WYNIK: ✔ OK    ?! (same znaki specjalne)
        Check.equal("ab", false, palindromeTwoPointers("ab"));
        // WYNIK: ✔ OK    ab
        Check.equal("V1 = V2 = V3 dla \"Kobyła ma mały bok\"", true,
                palindromeNaive("Kobyła ma mały bok")
                        && palindromeTwoPointers("Kobyła ma mały bok")
                        && palindromeCodePoints("Kobyła ma mały bok"));
        // WYNIK: ✔ OK    V1 = V2 = V3 dla "Kobyła ma mały bok"
        // Unicode: A i B w "matematycznej pogrubionej" czcionce to litery spoza podstawowej płaszczyzny (U+1D400, U+1D401).
        String boldA = new String(Character.toChars(0x1D400));
        String boldB = new String(Character.toChars(0x1D401));
        String niePalindrom = boldA + "x" + boldB;
        String palindrom = boldA + "x" + boldA;
        show("V2 (char) dla A-x-B (to NIE palindrom!)", palindromeTwoPointers(niePalindrom));
        // WYNIK: V2 (char) dla A-x-B (to NIE palindrom!) → true
        show("V3 (punkty kodowe) dla A-x-B", palindromeCodePoints(niePalindrom));
        // WYNIK: V3 (punkty kodowe) dla A-x-B → false
        show("V3 (punkty kodowe) dla A-x-A", palindromeCodePoints(palindrom));
        // WYNIK: V3 (punkty kodowe) dla A-x-A → true
        // Dlaczego V2 się myli: Character.isLetterOrDigit(char) dla połówki pary zastępczej zwraca false, więc obie litery
        // zostały pominięte, został tylko "x" — i wyszło "palindrom". Błąd cichy, bez wyjątku.
        // JAK O TYM MÓWIĆ: "Dla zwykłego tekstu dwa wskaźniki wystarczą; dodam uwagę, że przy emoji i rzadkich alfabetach
        //   przeszedłbym na codePoints — i mogę to pokazać." Ta jedna uwaga często odróżnia kandydatów.
        // DOBRA PRAKTYKA: dla wielkości liter w tekstach językowych używaj Character.toLowerCase albo toLowerCase(Locale.ROOT)
        //   — bez parametru Locale tureckie "I" może dać zaskakujący wynik.
    }

    // =================================================================================================
    // 4. ANAGRAM ★★
    // =================================================================================================

    /**
     * 4. ZADANIE 4 ★★ Czy dwa teksty są anagramami (te same litery w innej kolejności), bez względu na wielkość liter i spacje?
     */
    static void anagrams() {
        section("4. Anagram ★★ — sortowanie kontra zliczanie");

        // ZADANIE 4 ★★ Anagram   (→ t24_algorithms/Algorithms05Classics)
        // DLACZEGO TO PYTAJĄ: sprawdza znajomość sortowania i mapy częstości — wzorca "policz wystąpienia", który jest podstawą
        //   dziesiątek zadań (duplikaty, pierwszy unikat, grupowanie anagramów).
        // V1 (naiwna): posortuj litery obu tekstów i porównaj tablice — czas O(n log n), pamięć O(n).
        // V2 (ulepszona): zlicz litery pierwszego tekstu (+1), odlicz drugiego (-1); anagram, gdy wszystkie liczniki to 0 —
        //   czas O(n), pamięć O(k), gdzie k to liczba RÓŻNYCH liter (mapa; dla samego ASCII wystarczy tablica int[26]).
        // PRZYPADKI BRZEGOWE: różne długości (po normalizacji) → false od razu; puste teksty → true; wielkość liter ("Roma"/"Amor");
        //   spacje i interpunkcja (ignorujemy); polskie litery ("żółć"/"ćółż"). Pytanie do rozmówcy: czy "aa" i "a" to anagramy? (nie).
        Check.equal("listen / silent", true, anagramSorted("listen", "silent"));
        // WYNIK: ✔ OK    listen / silent
        Check.equal("Roma / Amor (wielkość liter)", true, anagramCounting("Roma", "Amor"));
        // WYNIK: ✔ OK    Roma / Amor (wielkość liter)
        Check.equal("dormitory / dirty room (spacje)", true, anagramCounting("dormitory", "dirty room"));
        // WYNIK: ✔ OK    dormitory / dirty room (spacje)
        Check.equal("żółć / ćółż (polskie litery)", true, anagramCounting("żółć", "ćółż"));
        // WYNIK: ✔ OK    żółć / ćółż (polskie litery)
        Check.equal("abc / abd", false, anagramCounting("abc", "abd"));
        // WYNIK: ✔ OK    abc / abd
        Check.equal("aa / a (różna liczba liter)", false, anagramCounting("aa", "a"));
        // WYNIK: ✔ OK    aa / a (różna liczba liter)
        Check.equal("puste teksty", true, anagramSorted("", ""));
        // WYNIK: ✔ OK    puste teksty
        // Wersja z tablicą int[26] jest jeszcze szybsza dla angielskiego alfabetu małych liter, ale zawodzi przy polskich
        // znakach — dlatego tu mapa Character → Integer. Wybór struktury zależy od założeń; ZAPYTAJ o alfabet.
        // JAK O TYM MÓWIĆ: "Najprościej: sortuję i porównuję, O(n log n). Jeśli n jest duże — liczę litery w mapie, O(n).
        //   Zaczynam od normalizacji: małe litery, bez spacji."
        // DOBRA PRAKTYKA: wydziel normalizację do osobnej metody — obie wersje używają jej i testy porównują tylko algorytm.
    }

    // =================================================================================================
    // 5. PIERWSZY NIEPOWTARZALNY ZNAK ★★
    // =================================================================================================

    /**
     * 5. ZADANIE 5 ★★ Znajdź pierwszy znak, który występuje w tekście tylko raz.
     */
    static void firstUnique() {
        section("5. Pierwszy niepowtarzalny znak ★★");

        // ZADANIE 5 ★★ Pierwszy znak, który się nie powtarza   (→ t12_collections/Collections10Patterns)
        // DLACZEGO TO PYTAJĄ: "policz, a potem znajdź" — dwa przebiegi i mapa, która pamięta KOLEJNOŚĆ (LinkedHashMap).
        // V1 (naiwna): dla każdego znaku sprawdź, czy indexOf == lastIndexOf — czas O(n²) (każde indexOf to przeszukanie).
        // V2 (ulepszona): przebieg 1 — zlicz w LinkedHashMap (zachowuje kolejność wstawiania), przebieg 2 — pierwszy wpis
        //   z licznikiem 1. Czas O(n), pamięć O(k).
        // PRZYPADKI BRZEGOWE: brak takiego znaku ("aabb"), pusty tekst, wielkość liter ("Aa" — 'A' jest unikalne, jeśli rozróżniamy),
        //   wynik jako Optional (zamiast magicznej wartości '\0' lub null).
        show("swiss", firstUniqueNaive("swiss"));
        // WYNIK: swiss → Optional[w]
        show("swiss (V2)", firstUniqueCounting("swiss"));
        // WYNIK: swiss (V2) → Optional[w]
        show("aabb (brak)", firstUniqueCounting("aabb"));
        // WYNIK: aabb (brak) → Optional.empty
        show("pusty tekst", firstUniqueCounting(""));
        // WYNIK: pusty tekst → Optional.empty
        show("Aa (rozróżniamy wielkość)", firstUniqueCounting("Aa"));
        // WYNIK: Aa (rozróżniamy wielkość) → Optional[A]
        Check.equal("V1 = V2 dla \"programowanie\"", firstUniqueNaive("programowanie"), firstUniqueCounting("programowanie"));
        // WYNIK: ✔ OK    V1 = V2 dla "programowanie"
        // PUŁAPKA: HashMap zamiast LinkedHashMap też policzy znaki, ale NIE zachowa kolejności — "pierwszy" przestałby być pierwszy.
        //   Alternatywa bez LinkedHashMap: drugi przebieg po oryginalnym tekście i sprawdzanie licznika (też O(n)).
        // JAK O TYM MÓWIĆ: "Dwa przebiegi: liczę, potem szukam w kolejności tekstu. Koszt liniowy, pamięć zależna od alfabetu."
    }

    // =================================================================================================
    // 6. TWO-SUM ★★
    // =================================================================================================

    /**
     * 6. ZADANIE 6 ★★ Dla tablicy liczb i sumy docelowej zwróć indeksy dwóch RÓŻNYCH elementów, które dają tę sumę.
     */
    static void twoSum() {
        section("6. Two-sum ★★ — HashMap zamiast dwóch pętli");

        // ZADANIE 6 ★★ Dwie liczby o zadanej sumie (two-sum)   (→ t24_algorithms/Algorithms05Classics)
        // DLACZEGO TO PYTAJĄ: najsłynniejsze zadanie o zamianie czasu na pamięć: O(n²) → O(n) przez HashMap.
        // V1 (naiwna, "brute force" = siłowa): dwie zagnieżdżone pętle, sprawdź każdą parę — czas O(n²), pamięć O(1).
        // V2 (ulepszona): jedna pętla; dla liczby x szukamy w mapie dopełnienia (cel - x); jeśli jest — mamy parę, jeśli nie —
        //   zapisujemy x i jego indeks. Czas O(n), pamięć O(n).
        // PRZYPADKI BRZEGOWE: ten sam element nie może być użyty dwa razy ([3, 2, 4], cel 6 → [1, 2], a nie [0, 0]);
        //   duplikaty ([3, 3], cel 6 → [0, 1]) — dlatego dopełnienie sprawdzamy PRZED wstawieniem bieżącej liczby;
        //   liczby ujemne; brak rozwiązania (pusta tablica); pusta tablica wejściowa; przepełnienie int przy dodawaniu
        //   dużych liczb (cel - x policz jako long, jeśli wartości mogą być duże).
        show("V1 [2, 7, 11, 15], cel 9", Arrays.toString(twoSumNaive(new int[]{2, 7, 11, 15}, 9)));
        // WYNIK: V1 [2, 7, 11, 15], cel 9 → [0, 1]
        show("V2 [2, 7, 11, 15], cel 9", Arrays.toString(twoSumMap(new int[]{2, 7, 11, 15}, 9)));
        // WYNIK: V2 [2, 7, 11, 15], cel 9 → [0, 1]
        Check.equal("[3, 2, 4], cel 6 (nie ten sam element)", "[1, 2]", Arrays.toString(twoSumMap(new int[]{3, 2, 4}, 6)));
        // WYNIK: ✔ OK    [3, 2, 4], cel 6 (nie ten sam element)
        Check.equal("[3, 3], cel 6 (duplikaty)", "[0, 1]", Arrays.toString(twoSumMap(new int[]{3, 3}, 6)));
        // WYNIK: ✔ OK    [3, 3], cel 6 (duplikaty)
        Check.equal("[-1, -2, -3, -4], cel -6 (ujemne)", "[1, 3]", Arrays.toString(twoSumMap(new int[]{-1, -2, -3, -4}, -6)));
        // WYNIK: ✔ OK    [-1, -2, -3, -4], cel -6 (ujemne)
        Check.equal("brak rozwiązania", "[]", Arrays.toString(twoSumMap(new int[]{1, 2, 3}, 100)));
        // WYNIK: ✔ OK    brak rozwiązania
        Check.equal("pusta tablica", "[]", Arrays.toString(twoSumMap(new int[]{}, 0)));
        // WYNIK: ✔ OK    pusta tablica
        Check.equal("V1 i V2 zgodne dla [3, 2, 4]", Arrays.toString(twoSumNaive(new int[]{3, 2, 4}, 6)),
                Arrays.toString(twoSumMap(new int[]{3, 2, 4}, 6)));
        // WYNIK: ✔ OK    V1 i V2 zgodne dla [3, 2, 4]
        // DOBRA PRAKTYKA: jasno ustal, co zwracasz przy braku rozwiązania (pusta tablica, Optional, wyjątek) i opowiedz o tym.
        // JAK O TYM MÓWIĆ: "Brute force to O(n²). Zamieniam pamięć na czas: mapa wartość → indeks, jeden przebieg.
        //   Pilnuję, żeby nie użyć tego samego elementu dwa razy."
    }

    // =================================================================================================
    // 7. NAWIASY ★★
    // =================================================================================================

    /**
     * 7. ZADANIE 7 ★★ Sprawdź, czy nawiasy ()[]{} w tekście są poprawnie zbalansowane (zamknięte w dobrej kolejności).
     */
    static void brackets() {
        section("7. Zbalansowane nawiasy ★★ — stos (Deque)");

        // ZADANIE 7 ★★ Poprawne nawiasy   (→ t12_collections/Collections06QueuesDeques)
        // DLACZEGO TO PYTAJĄ: klasyczne zastosowanie stosu (LIFO — ostatni otwarty nawias musi być zamknięty pierwszy).
        // V1 (naiwna): wielokrotnie usuwaj sąsiednie pary "()", "[]", "{}" aż nic się nie zmienia; poprawne, gdy zostanie pusty
        //   tekst — czas O(n²) (każdy obrót to nowy napis).
        // V2 (ulepszona): stos na otwierające, przy zamykającym zdejmij ze stosu i sprawdź parę; na końcu stos ma być pusty.
        //   Czas O(n), pamięć O(n).
        // PRZYPADKI BRZEGOWE: pusty tekst → true; tylko zamykający (")") → false, a stos jest pusty przy zdejmowaniu — SPRAWDŹ
        //   isEmpty() przed pop() (ArrayDeque.pop na pustym rzuca NoSuchElementException); nieparzysta długość → false;
        //   pozostały otwarte nawiasy ("((") → false; błędna kolejność ("([)]") → false; inne znaki ("a(b)c") ignorujemy.
        // PUŁAPKA: klasa Stack jest przestarzała — używaj ArrayDeque jako stosu (push/pop).
        Check.equal("()[]{}", true, balancedStack("()[]{}"));
        // WYNIK: ✔ OK    ()[]{}
        Check.equal("{[()]}", true, balancedStack("{[()]}"));
        // WYNIK: ✔ OK    {[()]}
        Check.equal("([)] (zła kolejność)", false, balancedStack("([)]"));
        // WYNIK: ✔ OK    ([)] (zła kolejność)
        Check.equal("( (niezamknięty)", false, balancedStack("("));
        // WYNIK: ✔ OK    ( (niezamknięty)
        Check.equal(") (tylko zamykający — stos pusty)", false, balancedStack(")"));
        // WYNIK: ✔ OK    ) (tylko zamykający — stos pusty)
        Check.equal("pusty tekst", true, balancedStack(""));
        // WYNIK: ✔ OK    pusty tekst
        Check.equal("a(b)c (inne znaki ignorowane)", true, balancedStack("a(b)c"));
        // WYNIK: ✔ OK    a(b)c (inne znaki ignorowane)
        Check.equal("V1 = V2 dla {[()()]}", balancedNaive("{[()()]}"), balancedStack("{[()()]}"));
        // WYNIK: ✔ OK    V1 = V2 dla {[()()]}
        // JAK O TYM MÓWIĆ: "Widzę strukturę LIFO, więc stos. Dla otwierającego push, dla zamykającego pop i porównanie.
        //   Przed pop sprawdzam, czy stos nie jest pusty; na końcu stos musi być pusty."
        // DOBRA PRAKTYKA: słownik "zamykający → otwierający" (mapa lub metoda) zamiast trzech powielonych if-ów — dodanie <> to jedna linia.
    }

    // =================================================================================================
    // 8. TESTOWANIE JEDNEJ WERSJI DRUGĄ
    // =================================================================================================

    /**
     * 8. Jak przetestować, że ulepszona wersja robi to samo co naiwna: losowe dane z ziarnem i porównanie wyników.
     */
    static void differentialTesting() {
        section("8. Testowanie różnicowe: naiwna kontra ulepszona");

        // Jeśli masz DWIE wersje (naiwną, oczywiście poprawną, i szybką), nie musisz wymyślać wielu przypadków ręcznie:
        // wygeneruj setki losowych danych i porównaj odpowiedzi. Losowość z ZIARNEM (new Random(42)) jest powtarzalna —
        // ten sam test przy każdym uruchomieniu, więc błąd da się odtworzyć. To technika z lekcji t25_testing/Testing01Concepts.
        Random random = new Random(42);
        int twoSumMismatch = 0;
        int twoSumFound = 0;
        for (int round = 0; round < 500; round++) {
            int[] nums = new int[random.nextInt(9)];
            for (int i = 0; i < nums.length; i++) {
                nums[i] = random.nextInt(11) - 5;
            }
            int target = random.nextInt(17) - 8;
            int[] naive = twoSumNaive(nums, target);
            int[] fast = twoSumMap(nums, target);
            if (naive.length != fast.length || (fast.length == 2 && !validPair(nums, target, fast))) {
                twoSumMismatch++;
            }
            if (fast.length == 2) {
                twoSumFound++;
            }
        }
        show("two-sum: niezgodności w 500 losowych testach", twoSumMismatch);
        // WYNIK: two-sum: niezgodności w 500 losowych testach → 0
        show("two-sum: ile testów miało rozwiązanie (test nie jest pusty)", twoSumFound > 100);
        // WYNIK: two-sum: ile testów miało rozwiązanie (test nie jest pusty) → true
        int bracketsMismatch = 0;
        int balancedCount = 0;
        String alphabet = "()[]{}";
        for (int round = 0; round < 500; round++) {
            StringBuilder sb = new StringBuilder();
            int length = random.nextInt(9);
            for (int i = 0; i < length; i++) {
                sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            boolean a = balancedNaive(sb.toString());
            boolean b = balancedStack(sb.toString());
            if (a != b) {
                bracketsMismatch++;
            }
            if (b) {
                balancedCount++;
            }
        }
        show("nawiasy: niezgodności w 500 losowych testach", bracketsMismatch);
        // WYNIK: nawiasy: niezgodności w 500 losowych testach → 0
        show("nawiasy: czy trafiły się zbalansowane teksty", balancedCount > 10);
        // WYNIK: nawiasy: czy trafiły się zbalansowane teksty → true
        int palindromeMismatch = 0;
        String letters = "aAb ,";
        for (int round = 0; round < 500; round++) {
            StringBuilder sb = new StringBuilder();
            int length = random.nextInt(8);
            for (int i = 0; i < length; i++) {
                sb.append(letters.charAt(random.nextInt(letters.length())));
            }
            String s = sb.toString();
            boolean v1 = palindromeNaive(s);
            if (v1 != palindromeTwoPointers(s) || v1 != palindromeCodePoints(s)) {
                palindromeMismatch++;
            }
        }
        show("palindrom V1/V2/V3: niezgodności w 500 testach", palindromeMismatch);
        // WYNIK: palindrom V1/V2/V3: niezgodności w 500 testach → 0
        int anagramMismatch = 0;
        String abc = "abc d";
        for (int round = 0; round < 500; round++) {
            String x = randomText(random, abc, 6);
            String y = randomText(random, abc, 6);
            if (anagramSorted(x, y) != anagramCounting(x, y)) {
                anagramMismatch++;
            }
        }
        show("anagram V1/V2: niezgodności w 500 testach", anagramMismatch);
        // WYNIK: anagram V1/V2: niezgodności w 500 testach → 0
        // DOBRA PRAKTYKA: dane losowe ograniczaj do MAŁEGO alfabetu i krótkich długości — wtedy szybko trafiają się przypadki
        //   brzegowe (puste, duplikaty, powtórzenia), a gdy test padnie, kontrprzykład da się przeczytać.
        // PUŁAPKA: Random bez ziarna daje inne dane przy każdym uruchomieniu — błąd raz się pojawi, raz nie (flaky test).
    }

    // =================================================================================================
    // 9. ZŁOŻONOŚĆ W PRAKTYCE
    // =================================================================================================

    /**
     * 9. Złożoność nie jest teorią: zliczamy kroki wersji naiwnej i ulepszonej dla rosnącego n.
     */
    static void countingOperations() {
        section("9. Złożoność w praktyce: policzmy kroki");

        // Najgorszy przypadek dla two-sum: brak rozwiązania, więc obie wersje przeglądają WSZYSTKO.
        // V1 sprawdza n(n-1)/2 par, V2 wykonuje n kroków. Przy podwojeniu n: V1 rośnie CZTERY razy, V2 — dwa razy.
        // (O(n²) kontra O(n)). Dlatego na rozmowie mówisz o rzędzie wielkości, nie o sekundach.
        int[] small = new int[1000];
        Arrays.fill(small, 1);
        int[] big = new int[2000];
        Arrays.fill(big, 1);
        long naiveSmall = twoSumNaiveSteps(small, 100);
        long naiveBig = twoSumNaiveSteps(big, 100);
        long mapSmall = twoSumMapSteps(small, 100);
        long mapBig = twoSumMapSteps(big, 100);
        show("V1 kroków dla n = 1000 / n = 2000", naiveSmall + " / " + naiveBig);
        // WYNIK: V1 kroków dla n = 1000 / n = 2000 → 499500 / 1999000
        show("V2 kroków dla n = 1000 / n = 2000", mapSmall + " / " + mapBig);
        // WYNIK: V2 kroków dla n = 1000 / n = 2000 → 1000 / 2000
        show("V1: stosunek kroków przy podwojeniu n (≈ 4)", naiveBig / naiveSmall);
        // WYNIK: V1: stosunek kroków przy podwojeniu n (≈ 4) → 4
        show("V2: stosunek kroków przy podwojeniu n (= 2)", mapBig / mapSmall);
        // WYNIK: V2: stosunek kroków przy podwojeniu n (= 2) → 2
        // JAK O TYM MÓWIĆ: "Siłowo O(n²) — przy tysiącu elementów to pół miliona par, przy milionie pół biliona. Mapa daje O(n)
        //   kosztem O(n) pamięci." Podaj też koszt pamięciowy: złożoność to ZAWSZE para (czas, pamięć).
        // PUŁAPKA: O(1) dla HashMap to wartość ŚREDNIA; pesymistycznie O(log n) (drzewa w koszykach, Java 8+) — zob. kartę
        //   o HashMap w t33_interview_prep/Interview02OopCollections.
    }

    // =================================================================================================
    // 10. PODSUMOWANIE TESTÓW
    // =================================================================================================

    /**
     * 10. Wszystkie testy Check z powyższych zadań w jednym podsumowaniu (licznik zostaje wyzerowany przed ćwiczeniami).
     */
    static void testSummary() {
        section("10. Podsumowanie testów zadań");
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 39 OK, ✘ 0 BŁĄD
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Schemat: powtórz treść → dopytaj → przykłady → wersja naiwna i jej koszt → ulepszenie → kod → testy.
     *   • FizzBuzz: warunek "oba" (15) sprawdzaj pierwszy; reguły wynieś do mapy; wynik zwracaj, nie wypisuj.
     *   • Odwracanie: pętla z + to O(n²); StringBuilder.reverse O(n) i bezpieczny dla par zastępczych.
     *   • Palindrom: dwa wskaźniki O(n) czas, O(1) pamięć; Unicode → codePoints.
     *   • Anagram: sortowanie O(n log n) albo zliczanie O(n); normalizuj (wielkość liter, spacje).
     *   • Pierwszy unikat: LinkedHashMap + dwa przebiegi, O(n); wynik jako Optional.
     *   • Two-sum: mapa wartość → indeks, jeden przebieg, sprawdzaj dopełnienie PRZED wstawieniem; O(n) czas i pamięć.
     *   • Nawiasy: stos (ArrayDeque), isEmpty() przed pop(), na końcu stos pusty.
     *   • Testy: Check.equal na przykładach brzegowych + losowe testy różnicowe z ziarnem (naiwna kontra ulepszona).
     *   • Złożoność podawaj jako parę (czas, pamięć) i uzasadniaj liczeniem kroków, nie sekundami.
     *   • Przypadki brzegowe zawsze: null, puste, jeden element, duplikaty, ujemne, Unicode, duże dane (przepełnienie).
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego w FizzBuzz warunek "podzielna przez 3 i 5" trzeba sprawdzić jako pierwszy?
     *   2. Jaką złożoność ma odwracanie napisu pętlą  wynik = znak + wynik  i dlaczego?
     *   3. Co wypisze:  palindromeTwoPointers("A man, a plan, a canal: Panama")  ?
     *   4. ZNAJDŹ BŁĄD:  for (int j = i; j < n; j++) if (nums[i] + nums[j] == target) return new int[]{i, j};  (two-sum)
     *   5. Co zwróci wersja two-sum z mapą dla tablicy [3, 3] i celu 6? Dlaczego dopełnienie sprawdzamy przed put?
     *   6. Jak sprawdzić zbalansowane nawiasy dla ")"  bez wyjątku NoSuchElementException?
     *   7. Podaj złożoność (czas i pamięć) wersji naiwnej i ulepszonej anagramu.
     *   8. Po co w teście losowym używać Random z ziarnem?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — zadania do samodzielnego rozwiązania
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
        String text = "Ala ma kota, a kot ma Alę. Ala!";
        Check.equal("ćw. 1: countWords", "{a=1, ala=2, alę=1, kot=1, kota=1, ma=2}",
                () -> (sol ? solution1(text) : exercise1(text)).toString());
        Check.equal("ćw. 2: findDuplicates", List.of(List.of(1, 2), List.of(), List.of(5)),
                () -> sol
                        ? List.of(solution2(List.of(1, 2, 3, 2, 4, 1, 5, 1)), solution2(List.of(1, 2, 3)), solution2(List.of(5, 5)))
                        : List.of(exercise2(List.of(1, 2, 3, 2, 4, 1, 5, 1)), exercise2(List.of(1, 2, 3)), exercise2(List.of(5, 5))));
        Check.equal("ćw. 3: mergeSorted", "[1, 2, 3, 4, 5, 6, 8] | [1] | [1, 1, 1]",
                () -> {
                    int[] r1 = sol ? solution3(new int[]{1, 3, 5}, new int[]{2, 4, 6, 8}) : exercise3(new int[]{1, 3, 5}, new int[]{2, 4, 6, 8});
                    int[] r2 = sol ? solution3(new int[]{}, new int[]{1}) : exercise3(new int[]{}, new int[]{1});
                    int[] r3 = sol ? solution3(new int[]{1, 1}, new int[]{1}) : exercise3(new int[]{1, 1}, new int[]{1});
                    return Arrays.toString(r1) + " | " + Arrays.toString(r2) + " | " + Arrays.toString(r3);
                });
        int[] sorted = {1, 3, 5, 7, 9};
        Check.equal("ćw. 4: binarySearch", List.of(3, -1, 0, 4, -1, -1),
                () -> sol
                        ? List.of(solution4(sorted, 7), solution4(sorted, 4), solution4(sorted, 1), solution4(sorted, 9),
                                solution4(new int[]{}, 1), solution4(sorted, 10))
                        : List.of(exercise4(sorted, 7), exercise4(sorted, 4), exercise4(sorted, 1), exercise4(sorted, 9),
                                exercise4(new int[]{}, 1), exercise4(sorted, 10)));
        Check.equal("ćw. 5: maxProfit", List.of(5, 0, 0, 5),
                () -> sol
                        ? List.of(solution5(new int[]{7, 1, 5, 3, 6, 4}), solution5(new int[]{7, 6, 4, 3, 1}),
                                solution5(new int[]{}), solution5(new int[]{2, 4, 1, 6, 3}))
                        : List.of(exercise5(new int[]{7, 1, 5, 3, 6, 4}), exercise5(new int[]{7, 6, 4, 3, 1}),
                                exercise5(new int[]{}), exercise5(new int[]{2, 4, 1, 6, 3})));
    }

    /**
     * ĆWICZENIE 1 (łatwe): PRZEPISZ pętlę zliczającą słowa na strumień. Słowa to ciągi liter (podział po znakach niebędących
     * literami), małymi literami, puste pomijamy; wynik w TreeMap (alfabetycznie).
     * <pre>{@code
     * Map<String, Long> counts = new TreeMap<>();
     * for (String w : text.toLowerCase().split("\\P{L}+")) {
     *     if (!w.isEmpty()) {
     *         counts.put(w, counts.getOrDefault(w, 0L) + 1);
     *     }
     * }
     * }</pre>
     * Podpowiedź: Arrays.stream(...).filter(...).collect(Collectors.groupingBy(w -> w, TreeMap::new, Collectors.counting())).
     * Kolekcjonery grupujące poznasz w t16_streams/Streams11GroupingBy.
     */
    static Map<String, Long> exercise1(String text) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (łatwe): zwróć POSORTOWANĄ listę liczb, które występują w wejściu więcej niż raz (każdą tylko raz).
     * Wersja naiwna to dwie pętle O(n²); celuj w O(n) (plus sortowanie wyniku).
     * Podpowiedź: dwa zbiory — widziane (HashSet) i duplikaty (TreeSet, który sam sortuje).
     */
    static List<Integer> exercise2(List<Integer> numbers) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): scal dwie posortowane rosnąco tablice w jedną posortowaną w czasie O(n + m), BEZ sortowania wyniku.
     * Wersja naiwna: sklej i posortuj, O((n+m) log(n+m)).
     * Podpowiedź: dwa wskaźniki i, j; bierz mniejszy z a[i] i b[j]; na końcu dopisz resztę drugiej tablicy.
     */
    static int[] exercise3(int[] a, int[] b) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (średnie): wyszukiwanie binarne w posortowanej tablicy — zwróć indeks szukanej liczby albo -1.
     * Wersja naiwna: przeglądanie liniowe O(n); celuj w O(log n).
     * Podpowiedź: low, high, mid = (low + high) >>> 1 (zapis odporny na przepełnienie, w przeciwieństwie do (low + high) / 2).
     */
    static int exercise4(int[] sorted, int target) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): dostajesz ceny akcji w kolejnych dniach. Kupujesz w jednym dniu, a sprzedajesz w PÓŹNIEJSZYM.
     * Zwróć największy możliwy zysk (0, gdy nie da się zarobić; 0 dla pustej tablicy).
     * Wersja naiwna: dwie pętle, O(n²). Celuj w jeden przebieg O(n), pamięć O(1).
     * Podpowiedź: idąc od lewej pamiętaj najniższą dotąd cenę i porównuj z nią bieżącą cenę.
     */
    static int exercise5(int[] prices) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static Map<String, Long> solution1(String text) {
        return Arrays.stream(text.toLowerCase().split("\\P{L}+"))
                .filter(w -> !w.isEmpty())
                .collect(Collectors.groupingBy(w -> w, TreeMap::new, Collectors.counting()));
    }

    static List<Integer> solution2(List<Integer> numbers) {
        Set<Integer> seen = new HashSet<>();
        Set<Integer> duplicates = new TreeSet<>();
        for (int n : numbers) {
            if (!seen.add(n)) {
                duplicates.add(n);
            }
        }
        return new ArrayList<>(duplicates);
    }

    static int[] solution3(int[] a, int[] b) {
        int[] result = new int[a.length + b.length];
        int i = 0;
        int j = 0;
        int k = 0;
        while (i < a.length && j < b.length) {
            result[k++] = a[i] <= b[j] ? a[i++] : b[j++];
        }
        while (i < a.length) {
            result[k++] = a[i++];
        }
        while (j < b.length) {
            result[k++] = b[j++];
        }
        return result;
    }

    static int solution4(int[] sorted, int target) {
        int low = 0;
        int high = sorted.length - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (sorted[mid] == target) {
                return mid;
            } else if (sorted[mid] < target) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return -1;
    }

    static int solution5(int[] prices) {
        int best = 0;
        int lowest = Integer.MAX_VALUE;
        for (int price : prices) {
            lowest = Math.min(lowest, price);
            best = Math.max(best, price - lowest);
        }
        return best;
    }

    // </editor-fold>

    // =================================================================================================
    // IMPLEMENTACJE ZADAŃ Z SEKCJI (wersje naiwne i ulepszone)
    // =================================================================================================

    static List<String> fizzBuzzNaive(int n) {
        List<String> result = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            if (i % 3 == 0 && i % 5 == 0) {
                result.add("FizzBuzz");
            } else if (i % 3 == 0) {
                result.add("Fizz");
            } else if (i % 5 == 0) {
                result.add("Buzz");
            } else {
                result.add(String.valueOf(i));
            }
        }
        return result;
    }

    /** Błędna kolejność: "oba" sprawdzane na końcu, więc nigdy nie zostanie osiągnięte. */
    static List<String> fizzBuzzWrongOrder(int n) {
        List<String> result = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            if (i % 3 == 0) {
                result.add("Fizz");
            } else if (i % 5 == 0) {
                result.add("Buzz");
            } else if (i % 15 == 0) {
                result.add("FizzBuzz");
            } else {
                result.add(String.valueOf(i));
            }
        }
        return result;
    }

    static List<String> fizzBuzzRules(int n, Map<Integer, String> rules) {
        List<String> result = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<Integer, String> rule : rules.entrySet()) {
                if (i % rule.getKey() == 0) {
                    sb.append(rule.getValue());
                }
            }
            result.add(sb.isEmpty() ? String.valueOf(i) : sb.toString()); // isEmpty = czy pusty (Java 15+)
        }
        return result;
    }

    static List<String> fizzBuzzStream(int n) {
        return IntStream.rangeClosed(1, n)
                .mapToObj(i -> i % 15 == 0 ? "FizzBuzz" : i % 3 == 0 ? "Fizz" : i % 5 == 0 ? "Buzz" : String.valueOf(i))
                .toList();
    }

    static String reverseNaive(String s) {
        String result = "";
        for (int i = 0; i < s.length(); i++) {
            result = s.charAt(i) + result;
        }
        return result;
    }

    static String reverseBuilder(String s) {
        return new StringBuilder(s).reverse().toString();
    }

    static String reverseChars(String s) {
        char[] chars = s.toCharArray();
        for (int i = 0, j = chars.length - 1; i < j; i++, j--) {
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }

    static String reverseWordsList(String s) {
        List<String> words = new ArrayList<>(Arrays.asList(s.strip().split("\\s+")));
        Collections.reverse(words);
        return String.join(" ", words);
    }

    static String reverseWordsBuilder(String s) {
        String[] words = s.strip().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = words.length - 1; i >= 0; i--) {
            sb.append(words[i]);
            if (i > 0) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    static String cleaned(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    static boolean palindromeNaive(String s) {
        String clean = cleaned(s);
        return clean.equals(new StringBuilder(clean).reverse().toString());
    }

    static boolean palindromeTwoPointers(String s) {
        int i = 0;
        int j = s.length() - 1;
        while (i < j) {
            if (!Character.isLetterOrDigit(s.charAt(i))) {
                i++;
            } else if (!Character.isLetterOrDigit(s.charAt(j))) {
                j--;
            } else if (Character.toLowerCase(s.charAt(i)) != Character.toLowerCase(s.charAt(j))) {
                return false;
            } else {
                i++;
                j--;
            }
        }
        return true;
    }

    static boolean palindromeCodePoints(String s) {
        int[] cps = s.codePoints().filter(Character::isLetterOrDigit).map(Character::toLowerCase).toArray();
        for (int i = 0, j = cps.length - 1; i < j; i++, j--) {
            if (cps[i] != cps[j]) {
                return false;
            }
        }
        return true;
    }

    static String lettersOnly(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (Character.isLetter(c)) {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    static boolean anagramSorted(String a, String b) {
        char[] x = lettersOnly(a).toCharArray();
        char[] y = lettersOnly(b).toCharArray();
        Arrays.sort(x);
        Arrays.sort(y);
        return Arrays.equals(x, y);
    }

    static boolean anagramCounting(String a, String b) {
        String x = lettersOnly(a);
        String y = lettersOnly(b);
        if (x.length() != y.length()) {
            return false;
        }
        Map<Character, Integer> counts = new HashMap<>();
        for (char c : x.toCharArray()) {
            counts.merge(c, 1, Integer::sum);
        }
        for (char c : y.toCharArray()) {
            counts.merge(c, -1, Integer::sum);
        }
        return counts.values().stream().allMatch(v -> v == 0);
    }

    static Optional<Character> firstUniqueNaive(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (s.indexOf(c) == s.lastIndexOf(c)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    static Optional<Character> firstUniqueCounting(String s) {
        Map<Character, Integer> counts = new LinkedHashMap<>();
        for (char c : s.toCharArray()) {
            counts.merge(c, 1, Integer::sum);
        }
        for (Map.Entry<Character, Integer> entry : counts.entrySet()) {
            if (entry.getValue() == 1) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    static int[] twoSumNaive(int[] nums, int target) {
        for (int i = 0; i < nums.length; i++) {
            for (int j = i + 1; j < nums.length; j++) {
                if (nums[i] + nums[j] == target) {
                    return new int[]{i, j};
                }
            }
        }
        return new int[0];
    }

    static int[] twoSumMap(int[] nums, int target) {
        Map<Integer, Integer> seen = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            Integer partner = seen.get(target - nums[i]);
            if (partner != null) {
                return new int[]{partner, i};
            }
            seen.put(nums[i], i);
        }
        return new int[0];
    }

    static long twoSumNaiveSteps(int[] nums, int target) {
        long steps = 0;
        for (int i = 0; i < nums.length; i++) {
            for (int j = i + 1; j < nums.length; j++) {
                steps++;
                if (nums[i] + nums[j] == target) {
                    return steps;
                }
            }
        }
        return steps;
    }

    static long twoSumMapSteps(int[] nums, int target) {
        long steps = 0;
        Map<Integer, Integer> seen = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            steps++;
            if (seen.containsKey(target - nums[i])) {
                return steps;
            }
            seen.put(nums[i], i);
        }
        return steps;
    }

    static boolean validPair(int[] nums, int target, int[] pair) {
        return pair[0] < pair[1] && nums[pair[0]] + nums[pair[1]] == target;
    }

    static boolean balancedNaive(String s) {
        String current = s.replaceAll("[^()\\[\\]{}]", "");
        String previous;
        do {
            previous = current;
            current = current.replace("()", "").replace("[]", "").replace("{}", "");
        } while (!current.equals(previous));
        return current.isEmpty();
    }

    static char opening(char closing) {
        return switch (closing) {
            case ')' -> '(';
            case ']' -> '[';
            default -> '{';
        };
    }

    static boolean balancedStack(String s) {
        Deque<Character> stack = new ArrayDeque<>();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '(', '[', '{' -> stack.push(c);
                case ')', ']', '}' -> {
                    if (stack.isEmpty() || stack.pop() != opening(c)) {
                        return false;
                    }
                }
                default -> { }
            }
        }
        return stack.isEmpty();
    }

    static String randomText(Random random, String alphabet, int maxLength) {
        StringBuilder sb = new StringBuilder();
        int length = random.nextInt(maxLength + 1);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Liczba podzielna przez 15 jest też podzielna przez 3 i przez 5 — gdyby najpierw sprawdzić "przez 3", gałąź
     *      "FizzBuzz" nigdy by się nie wykonała (dla 15 wyszłoby samo "Fizz", co widać w wydruku zadania 1).
     *   2. O(n²): każde  znak + wynik  tworzy NOWY napis (String jest niezmienny) i kopiuje wszystkie dotychczasowe znaki,
     *      czyli 1 + 2 + ... + n kopii. Rozwiązanie: StringBuilder, O(n).
     *   3. true — po pominięciu spacji, przecinków i dwukropka oraz zignorowaniu wielkości liter tekst czyta się tak samo
     *      w obie strony (zadanie 3 ma ten test w Check).
     *   4. Pętla wewnętrzna zaczyna od j = i, więc element można sparować sam ze sobą ([3], cel 6 da [0, 0]). Poprawnie: j = i + 1.
     *   5. [0, 1]. Przy pierwszej 3 mapa jest pusta (dopełnienie 3 nie istnieje), więc zapisujemy 3 → 0; przy drugiej 3
     *      dopełnienie (3) już jest w mapie. Gdyby wstawić bieżącą liczbę PRZED sprawdzeniem, dla [3] i celu 6 wyszłaby
     *      para z samym sobą.
     *   6. Przed pop() sprawdź stack.isEmpty() (albo użyj pollFirst(), które zwraca null) — pusty stos dla zamykającego nawiasu
     *      oznacza od razu false.
     *   7. Naiwna (sortowanie): czas O(n log n), pamięć O(n) (kopie tablic); ulepszona (zliczanie w mapie): czas O(n),
     *      pamięć O(k), gdzie k to liczba różnych liter.
     *   8. Z ziarnem test jest powtarzalny: ten sam ciąg danych przy każdym uruchomieniu, więc znaleziony błąd da się odtworzyć
     *      i naprawić; bez ziarna powstają testy "flaky" (raz przechodzą, raz nie).
     */
    // </editor-fold>
}
