package t04_strings;

import helpers.Check;

import java.util.Arrays;
import java.util.Locale;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Algorytmy na tekście — palindromy, anagramy, słowa, częstość liter
 *        (algorithm = algorytm; palindrome = palindrom; anagram = anagram; frequency = częstość)
 *
 * W SKRÓCIE:
 *   Typowe zadania tekstowe (z rozmów rekrutacyjnych i z życia) rozwiązuje się kilkoma sztuczkami: normalizacja
 *   tekstu (małe litery, tylko litery), dwa wskaźniki idące z obu końców, sortowanie znaków, tablica liczników
 *   dla liter i dzielenie na słowa (split). Tu rozwiązujemy je „ręcznie”, żeby zrozumieć mechanizm.
 *
 * ANALOGIA: układanie kart.
 *   Anagram to dwa zestawy tych samych kart w innej kolejności — posortuj oba i porównaj. Palindrom to rząd kart
 *   czytany tak samo od lewej i od prawej — porównuj parami od obu końców do środka.
 *
 * JAK TO DZIAŁA:
 *   normalizacja:   text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}]", "")     ← same małe litery
 *   dwa wskaźniki:  left = 0, right = length - 1; porównuj i przesuwaj do środka
 *   anagram:        posortuj char[] obu słów (Arrays.sort) i porównaj (Arrays.equals)
 *   częstość:       int[] counts = new int[26];  counts[c - 'a']++
 *
 * SŁÓWKA:
 *   algorithm = algorytm; normalize = normalizować (ujednolicić); two pointers = dwa wskaźniki; frequency = częstość;
 *   occurrence = wystąpienie; acronym = skrótowiec (np. PNG); rotation = rotacja (przesunięcie cykliczne);
 *   longest = najdłuższy; capitalize = zamień pierwszą literę na wielką.
 *
 * ZOBACZ TEŻ: t04_strings/Strings02Methods (metody String), t03_arrays/Arrays04Algorithms (algorytmy na tablicach),
 *             t12_collections/Collections10Patterns (liczniki w mapie), t16_streams/Streams19Recipes (to samo streamami).
 * </pre>
 */
public class Strings07TextAlgorithms {

    public static void main(String[] args) {
        title("Strings07 — algorytmy na tekście");

        palindrome();           // palindrome = palindrom
        anagram();              // anagram = anagram
        countWord();            // count word = policz słowo
        capitalizeWords();      // capitalize words = wielkie litery na początku słów
        longestWord();          // longest word = najdłuższe słowo
        reverseWords();         // reverse words = odwróć kolejność słów
        letterFrequency();      // letter frequency = częstość liter
        rotation();             // rotation = rotacja
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PALINDROM
    // =================================================================================================

    /** lettersOnly = tylko litery. Normalizacja: małe litery, bez spacji i znaków interpunkcyjnych. */
    static String lettersOnly(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}]", "");
    }

    /** isPalindrome = czy palindrom. Dwa wskaźniki: od lewej i od prawej, aż się spotkają. */
    static boolean isPalindrome(String text) {
        String s = lettersOnly(text);
        int left = 0;
        int right = s.length() - 1;
        while (left < right) {
            if (s.charAt(left) != s.charAt(right)) {
                return false;
            }
            left++;
            right--;
        }
        return true;
    }

    /** 1. Palindrom czyta się tak samo od przodu i od tyłu — po ujednoliceniu (małe litery, bez spacji i znaków). */
    static void palindrome() {
        section("1. Palindrom — dwa wskaźniki");

        show("\"Kobyła ma mały bok.\"", isPalindrome("Kobyła ma mały bok."));
        show("\"Java\"", isPalindrome("Java"));
        // WYNIK: "Kobyła ma mały bok." → true
        // WYNIK: "Java" → false

        // DOBRA PRAKTYKA: najpierw normalizacja, potem algorytm — oddzielnie. Łatwiej zmienić reguły („czy cyfry też?”).
    }

    // =================================================================================================
    // 2. ANAGRAM
    // =================================================================================================

    /** isAnagram = czy anagram. Po posortowaniu liter oba słowa muszą być identyczne. */
    static boolean isAnagram(String a, String b) {
        char[] x = lettersOnly(a).toCharArray();      // toCharArray = zamień na tablicę znaków
        char[] y = lettersOnly(b).toCharArray();
        Arrays.sort(x);
        Arrays.sort(y);
        return Arrays.equals(x, y);
    }

    /** 2. Anagram to te same litery w innej kolejności. Sortowanie liter sprowadza oba słowa do tej samej postaci. */
    static void anagram() {
        section("2. Anagram — posortuj litery i porównaj");

        show("\"listen\" / \"silent\"", isAnagram("listen", "silent"));
        show("\"Dworzec\" / \"Codzwer\"", isAnagram("Dworzec", "Codzwer"));
        show("\"kot\" / \"kto\" / \"tok\"", isAnagram("kot", "kto") && isAnagram("kto", "tok"));
        show("\"java\" / \"kawa\"", isAnagram("java", "kawa"));
        // WYNIK: "listen" / "silent" → true
        // WYNIK: "Dworzec" / "Codzwer" → true
        // WYNIK: "kot" / "kto" / "tok" → true
        // WYNIK: "java" / "kawa" → false
    }

    // =================================================================================================
    // 3. LICZENIE WYSTĄPIEŃ SŁOWA
    // =================================================================================================

    /** countOccurrences = policz wystąpienia słowa (całe słowa, bez względu na wielkość liter). */
    static int countOccurrences(String text, String word) {
        int count = 0;
        for (String w : text.toLowerCase(Locale.ROOT).split("\\s+")) {
            if (w.equals(word.toLowerCase(Locale.ROOT))) {
                count++;
            }
        }
        return count;
    }

    /** 3. Dzielimy tekst na słowa (split) i porównujemy każde — tak liczymy CAŁE słowa, a nie fragmenty. */
    static void countWord() {
        section("3. Ile razy występuje słowo");

        String text = "Ala ma kota a kot ma Alę";
        show("\"ma\"", countOccurrences(text, "ma"));
        show("\"kot\" (całe słowo)", countOccurrences(text, "kot"));
        // WYNIK: "ma" → 2
        // WYNIK: "kot" (całe słowo) → 1    ← „kota” to inne słowo

        // PUŁAPKA: text.indexOf("kot") znalazłby też „kota” i „szkoty” — przy liczeniu SŁÓW dziel tekst na słowa.
        // Częstość WSZYSTKICH słów naraz liczy się mapą (t12_collections/Collections10Patterns).
    }

    // =================================================================================================
    // 4. WIELKA LITERA NA POCZĄTKU KAŻDEGO SŁOWA
    // =================================================================================================

    /** capitalizeEach = każde słowo z wielkiej litery. */
    static String capitalizeEach(String text) {
        StringBuilder sb = new StringBuilder();
        for (String word : text.strip().split("\\s+")) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }

    /** 4. Dziel na słowa, popraw każde, sklej z powrotem (StringBuilder — Strings03StringBuilder). */
    static void capitalizeWords() {
        section("4. Każde słowo z wielkiej litery");

        show("\"  jan   KOWALSKI \"", capitalizeEach("  jan   KOWALSKI "));
        // WYNIK: "  jan   KOWALSKI " → Jan Kowalski
    }

    // =================================================================================================
    // 5. NAJDŁUŻSZE SŁOWO
    // =================================================================================================

    /** 5. Klasyczny wzorzec „szukaj maksimum”: zapamiętaj najlepszego kandydata i porównuj z kolejnymi. */
    static void longestWord() {
        section("5. Najdłuższe słowo");

        String longest = "";
        for (String word : "Programowanie w Javie jest przyjemne".split(" ")) {
            if (word.length() > longest.length()) {         // > (a nie >=) — przy remisie zostaje PIERWSZE
                longest = word;
            }
        }
        show("najdłuższe", longest);
        // WYNIK: najdłuższe → Programowanie
    }

    // =================================================================================================
    // 6. ODWRÓCENIE KOLEJNOŚCI SŁÓW
    // =================================================================================================

    /** 6. Odwracamy kolejność SŁÓW (nie liter): idziemy po tablicy słów od końca. */
    static void reverseWords() {
        section("6. Odwróć kolejność słów");

        String[] words = "Ala ma kota".split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = words.length - 1; i >= 0; i--) {
            sb.append(words[i]);
            if (i > 0) {
                sb.append(' ');
            }
        }
        show("\"Ala ma kota\"", sb);
        // WYNIK: "Ala ma kota" → kota ma Ala
    }

    // =================================================================================================
    // 7. CZĘSTOŚĆ LITER — TABLICA LICZNIKÓW
    // =================================================================================================

    /**
     * 7. Dla liter a–z wystarczy tablica 26 liczników: counts[c - 'a']++ (litera 'a' → indeks 0, 'b' → 1...).
     * To szybkie i proste — nie trzeba mapy. (Polskie litery nie mieszczą się w a–z: wtedy mapa, t12_collections.)
     */
    static void letterFrequency() {
        section("7. Częstość liter a–z (tablica liczników)");

        int[] counts = new int[26];
        for (char c : "banana split".toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                counts[c - 'a']++;
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > 0) {
                sb.append((char) ('a' + i)).append(counts[i]).append(' ');
            }
        }
        show("\"banana split\"", sb.toString().strip());
        // WYNIK: "banana split" → a3 b1 i1 l1 n2 p1 s1 t1
    }

    // =================================================================================================
    // 8. ROTACJA
    // =================================================================================================

    /**
     * 8. Czy b to „przesunięty cyklicznie” a (np. "cdeab" z "abcde")? Sztuczka: każda rotacja a jest fragmentem a + a.
     */
    static void rotation() {
        section("8. Rotacja — sztuczka z a + a");

        String a = "abcde";
        show("\"cdeab\" rotacją \"abcde\"?", a.length() == 5 && (a + a).contains("cdeab"));
        show("\"ceadb\" rotacją \"abcde\"?", (a + a).contains("ceadb"));
        // WYNIK: "cdeab" rotacją "abcde"? → true    ← "abcdeabcde" zawiera "cdeab"
        // WYNIK: "ceadb" rotacją "abcde"? → false

        // PUŁAPKA: trzeba też porównać długości — inaczej "ab" wyszłoby „rotacją” "abcde" (bo "abcdeabcde" zawiera "ab").
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Najpierw normalizacja: toLowerCase(Locale.ROOT), replaceAll("[^\\p{L}]", "") — potem algorytm.
     *   • Palindrom: dwa wskaźniki od końców do środka.
     *   • Anagram: toCharArray + Arrays.sort + Arrays.equals.
     *   • Słowa: strip() + split("\\s+"); licz CAŁE słowa, nie indexOf.
     *   • Maksimum: zapamiętaj najlepszego kandydata; > zachowuje pierwszego przy remisie.
     *   • Częstość a–z: int[26], counts[c - 'a']++.
     *   • Rotacja: równe długości i (a + a).contains(b).
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego przed sprawdzaniem palindromu trzeba „znormalizować” tekst?
     *   2. Co zwróci  isAnagram("abc", "abcc")  z sekcji 2 i dlaczego?
     *   3. ZNAJDŹ BŁĄD:  int count = text.split("kot").length - 1;   // „liczę słowo kot”
     *   4. Jak z tablicy counts[26] odczytać, ile razy wystąpiła litera 'e'?
     *   5. Co zwróci  ("abc" + "abc").contains("ca")  i czy to znaczy, że "ca" jest rotacją "abc"?
     *   6. W sekcji 5: które słowo wygra, gdy dwa najdłuższe mają tę samą długość, i jak to zmienić?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: \"Kajak\" to palindrom", true, () -> exercise1("Kajak"));
        Check.equal("ćw. 2: samogłoski w \"Programowanie\"", 6, () -> exercise2("Programowanie"));
        Check.equal("ćw. 3: skrótowiec", "PNG", () -> exercise3("Portable Network Graphics"));
        Check.equal("ćw. 4: najczęstsza litera w \"programowanie\"", 'a', () -> exercise4("programowanie"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", true, () -> solution1("Kajak"));
        Check.equal("ćw. 2 (wzorzec)", 6, () -> solution2("Programowanie"));
        Check.equal("ćw. 3 (wzorzec)", "PNG", () -> solution3("Portable Network Graphics"));
        Check.equal("ćw. 4 (wzorzec)", 'a', () -> solution4("programowanie"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): czy słowo jest palindromem, bez względu na wielkość liter? ("Kajak" → true)
     * Podpowiedź: porównaj {@code text.toLowerCase(Locale.ROOT)} z jego odwróceniem (StringBuilder.reverse).
     */
    static boolean exercise1(String text) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): policz polskie samogłoski (a ą e ę i o ó u y) w tekście, bez względu na wielkość liter.
     * "Programowanie" → 6. Podpowiedź: pętla po znakach i {@code "aąeęioóuy".indexOf(...) >= 0}.
     */
    static int exercise2(String text) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (średnie): zbuduj skrótowiec z pierwszych liter słów, wielkimi literami:
     * "Portable Network Graphics" → "PNG". Podpowiedź: split("\\s+"), charAt(0), Character.toUpperCase, StringBuilder.
     */
    static String exercise3(String phrase) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zwróć najczęstszą literę a–z w tekście. Przy remisie — tę, która jest WCZEŚNIEJ
     * w alfabecie. "programowanie": r, o, a występują po 2 razy → wynik 'a'.
     * Podpowiedź: tablica int[26] jak w sekcji 7, potem szukanie maksimum od indeksu 0 (warunek > daje „pierwszą”).
     */
    static char exercise4(String text) {
        // TODO: twoje rozwiązanie
        return ' ';
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static boolean solution1(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.equals(new StringBuilder(lower).reverse().toString());
    }

    static int solution2(String text) {
        int vowels = 0;
        for (char c : text.toLowerCase(Locale.ROOT).toCharArray()) {
            if ("aąeęioóuy".indexOf(c) >= 0) {
                vowels++;
            }
        }
        return vowels;
    }

    static String solution3(String phrase) {
        StringBuilder sb = new StringBuilder();
        for (String word : phrase.strip().split("\\s+")) {
            sb.append(Character.toUpperCase(word.charAt(0)));
        }
        return sb.toString();
    }

    static char solution4(String text) {
        int[] counts = new int[26];
        for (char c : text.toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                counts[c - 'a']++;
            }
        }
        int best = 0;
        for (int i = 1; i < counts.length; i++) {
            if (counts[i] > counts[best]) {
                best = i;
            }
        }
        return (char) ('a' + best);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo „Kobyła ma mały bok.” ma wielkie litery, spacje i kropkę — bez ujednolicenia porównanie znaków z obu
     *      końców się nie zgodzi, choć to palindrom.
     *   2. false — po posortowaniu "abc" i "abcc" mają różną długość, więc Arrays.equals zwraca false.
     *   3. split("kot") liczy także fragmenty wewnątrz innych słów („kota”, „szkoty”) i zachowuje się dziwnie na końcach
     *      tekstu. Liczenie słów: podziel po białych znakach i porównuj całe słowa (sekcja 3).
     *   4. counts['e' - 'a'], czyli counts[4].
     *   5. true — ale "ca" NIE jest rotacją "abc", bo ma inną długość. Dlatego trzeba porównać długości.
     *   6. Pierwsze z nich (warunek >). Żeby wygrało ostatnie — użyj >=.
     */
    // </editor-fold>
}
