package t26_jvm;

import helpers.Check;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Pamięć JVM — stos, sterta, pula napisów i wycieki pamięci
 *        (memory = pamięć; stack = stos; heap = sterta; string pool = pula napisów; memory leak = wyciek pamięci)
 *
 * W SKRÓCIE:
 *   Każdy wątek ma własny STOS (stack). Każde wywołanie metody dostaje na nim RAMKĘ (frame) ze zmiennymi lokalnymi
 *   i parametrami. Wszystkie obiekty i tablice żyją na wspólnej STERCIE (heap), a zmienne trzymają tylko REFERENCJE
 *   (odnośniki) do nich. Ten podział tłumaczy StackOverflowError, OutOfMemoryError, zachowanie == na napisach
 *   i to, skąd biorą się wycieki pamięci w języku, który ma odśmiecacz (GC).
 *
 * ANALOGIA: biuro z archiwum.
 *   Każdy pracownik (wątek) ma własne biurko (stos) z karteczkami. Na karteczce jest albo liczba (prymityw), albo
 *   NUMER TECZKI (referencja). Same teczki (obiekty) leżą we wspólnym archiwum (sterta). Po skończonym zadaniu
 *   pracownik od razu zgarnia swoje karteczki (ramka znika), a teczkami zajmuje się archiwista (GC): wyrzuca te,
 *   do których nikt już nie ma numeru. Wyciek pamięci = teczka niepotrzebna, ale ktoś wciąż trzyma jej numer.
 *
 * JAK TO DZIAŁA:
 *   Obszary pamięci wg specyfikacji JVM i ich realizacja w HotSpot (Java 17):
 *   obszar                   | ile ich jest | co zawiera                                        | flaga
 *   stos JVM (JVM stack)     | 1 na wątek   | ramki: zmienne lokalne, parametry, wyniki pośrednie | -Xss
 *   sterta (heap)            | 1, wspólna   | WSZYSTKIE obiekty i tablice (także pula napisów)  | -Xms, -Xmx
 *   metaspace (method area)  | 1, wspólna   | metadane klas: bytecode, opisy pól i metod        | -XX:MaxMetaspaceSize
 *   rejestr PC (licznik)     | 1 na wątek   | adres aktualnie wykonywanej instrukcji            | —
 *   code cache               | 1, wspólny   | kod maszynowy wygenerowany przez JIT (Jvm04)      | -XX:ReservedCodeCacheSize
 *
 *   Kod:                            STOS wątku main              STERTA
 *     int count = 3;                ┌───────────────────┐        ┌────────────────────┐
 *     Box box = new Box(7);         │ ramka main:       │        │ obiekt Box         │
 *                                   │   count = 3       │        │   value = 7        │
 *                                   │   box   = ●───────┼───────►│   (int w obiekcie, │
 *                                   └───────────────────┘        │    więc na stercie)│
 *                                                                └────────────────────┘
 *   • Zmienna lokalna typu prostego (int, double...) leży w ramce na stosie — cała wartość.
 *   • Zmienna lokalna typu obiektowego też leży w ramce, ale trzyma tylko REFERENCJĘ; obiekt jest na stercie.
 *   • Pole typu int wewnątrz obiektu jest częścią obiektu, więc leży na stercie razem z nim.
 *   • Pola static: w HotSpot (od Javy 8) są przechowywane w obiekcie Class swojej klasy — też na stercie.
 *   • Ramka znika w chwili powrotu z metody; obiekt znika dopiero wtedy, gdy GC uzna go za nieosiągalny (Jvm03).
 *   Uwaga: kompilator JIT może dzięki analizie ucieczki (escape analysis) w ogóle nie utworzyć obiektu, który nie
 *   „wycieka” poza metodę. To niewidoczna optymalizacja — program zachowuje się tak, jakby obiekt był na stercie.
 *
 * SŁÓWKA:
 *   stack = stos; heap = sterta; frame = ramka; local variable = zmienna lokalna; reference = referencja (odnośnik);
 *   primitive = typ prosty (prymityw); pool = pula; intern = umieść w puli; leak = wyciek; listener = słuchacz;
 *   cache = pamięć podręczna; overflow = przepełnienie; header = nagłówek; alignment = wyrównanie;
 *   metaspace = obszar metadanych klas; runtime = środowisko uruchomieniowe.
 *
 * ZOBACZ TEŻ: t05_methods/Methods03Recursion (StackOverflowError od strony rekurencji),
 *             t04_strings/Strings01Basics (== kontra equals na napisach),
 *             t12_collections/Collections11HashingInternals (equals/hashCode i kubełki HashMap),
 *             t26_jvm/Jvm03GarbageCollection (kto i kiedy sprząta stertę).
 * </pre>
 */
public class Jvm01Memory {

    public static void main(String[] args) {
        title("Jvm01 — pamięć JVM: stos, sterta, pula napisów, wycieki");

        stackVsHeap();              // stack vs heap = stos kontra sterta
        passingArguments();         // passing arguments = przekazywanie argumentów
        framesAndOverflow();        // frames and overflow = ramki i przepełnienie stosu
        memorySizesAndRuntime();    // memory sizes and runtime = rozmiary pamięci i klasa Runtime
        stringPool();               // string pool = pula napisów
        objectSizes();              // object sizes = rozmiary obiektów
        leaksStaticAndListeners();  // leaks: static and listeners = wycieki: static i słuchacze
        leaksCacheAndKeys();        // leaks: cache and keys = wycieki: cache i klucze
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. STOS A STERTA
    // =================================================================================================

    /** Box = pudełko: najprostszy obiekt z jednym polem int. Pole jest częścią obiektu, więc leży na stercie. */
    static final class Box {
        int value;

        Box(int value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "Box[" + value + "]";
        }
    }

    /**
     * 1. Prymityw w zmiennej lokalnej to sama wartość (kopiowana przy przypisaniu). Zmienna obiektowa to referencja:
     * przypisanie kopiuje REFERENCJĘ, więc dwie zmienne mogą wskazywać ten sam obiekt na stercie (alias).
     */
    static void stackVsHeap() {
        section("1. Stos a sterta — zmienne lokalne, referencje, obiekty");

        int a = 5;                 // wartość 5 leży w ramce tej metody (stos)
        int b = a;                 // KOPIA wartości — dwie niezależne karteczki
        b++;
        show("a / b (prymitywy)", a + " / " + b);
        // WYNIK: a / b (prymitywy) → 5 / 6

        Box first = new Box(5);    // obiekt na stercie; w ramce tylko referencja do niego
        Box second = first;        // KOPIA REFERENCJI — dwie karteczki z numerem TEJ SAMEJ teczki
        second.value++;
        show("first / second", first + " / " + second);
        show("first == second", first == second);
        // WYNIK: first / second → Box[6] / Box[6]
        // WYNIK: first == second → true

        Box third = new Box(6);
        show("first == third (inny obiekt, ta sama wartość)", first == third);
        // WYNIK: first == third (inny obiekt, ta sama wartość) → false

        int copy = first.value;    // odczyt pola: wartość ze sterty KOPIOWANA do zmiennej lokalnej
        copy = 100;
        show("copy / first.value", copy + " / " + first.value);
        // WYNIK: copy / first.value → 100 / 6

        int[] numbers = {1, 2, 3}; // tablica to też obiekt → sterta, nawet gdy trzyma same int-y
        int[] alias = numbers;
        alias[0] = 99;
        show("numbers[0] po zmianie przez alias", numbers[0]);
        // WYNIK: numbers[0] po zmianie przez alias → 99

        // PUŁAPKA: „zmienna to obiekt” — nie. Zmienna obiektowa to tylko numer teczki. Dlatego zmiana przez alias
        //   (second, alias) jest widoczna przez oryginał: obie zmienne prowadzą do jednego obiektu na stercie.
        // DOBRA PRAKTYKA: czytaj == na obiektach jako „czy to TEN SAM obiekt?”, a nie „czy równe wartości?”.
        //   Do porównania wartości służy equals — bo == porównuje referencje (adresy teczek), nie zawartość.
    }

    // =================================================================================================
    // 2. PRZEKAZYWANIE ARGUMENTÓW — ZAWSZE KOPIA
    // =================================================================================================

    /** incrementPrimitive = zwiększ prymityw: zmienia KOPIĘ w swojej ramce — wywołujący nic nie zobaczy. */
    static void incrementPrimitive(int number) {
        number++;
    }

    /** incrementBox = zwiększ pudełko: kopia referencji prowadzi do TEGO SAMEGO obiektu — zmiana jest widoczna. */
    static void incrementBox(Box box) {
        box.value++;
    }

    /** replaceBox = podmień pudełko: przestawia tylko własną kopię referencji; oryginał wywołującego bez zmian. */
    static void replaceBox(Box box) {
        box = new Box(1000);
        box.value++;
    }

    /**
     * 2. Java przekazuje argumenty ZAWSZE przez wartość (pass by value = przekazanie przez wartość). Dla obiektów
     * tą wartością jest referencja — metoda dostaje kopię numeru teczki, nie samą teczkę ani „oryginalną zmienną”.
     */
    static void passingArguments() {
        section("2. Przekazywanie argumentów — zawsze kopia wartości");

        int n = 1;
        incrementPrimitive(n);
        show("n po incrementPrimitive(n)", n);
        // WYNIK: n po incrementPrimitive(n) → 1

        Box box = new Box(1);
        incrementBox(box);
        show("box po incrementBox(box)", box);
        // WYNIK: box po incrementBox(box) → Box[2]

        replaceBox(box);
        show("box po replaceBox(box)", box);
        // WYNIK: box po replaceBox(box) → Box[2]

        // Jak to wygląda w ramkach stosu:  main: box = ●──► Box[2]  ◄──● :replaceBox (kopia) → potem kopia = ●──► Box[1001]
        //   Nowy obiekt Box[1001] jest widoczny tylko z ramki replaceBox. Gdy metoda wraca, ramka znika, nikt nie ma
        //   już referencji do Box[1001] → staje się śmieciem dla GC.
        // PUŁAPKA: metoda NIE może „podmienić” obiektu wywołującemu przez przypisanie do parametru — bo zmienia
        //   tylko swoją kopię referencji. Jeśli ma dać nowy obiekt, niech go ZWRÓCI (return).
    }

    // =================================================================================================
    // 3. RAMKI STOSU I StackOverflowError
    // =================================================================================================

    /** depth = głębokość: licznik wywołań dive — tylko do pokazania, że stos ma koniec. */
    private static int depth;

    /** currentStack = bieżący stos: nazwy metod z 4 najnowszych ramek bieżącego wątku. */
    static List<String> currentStack() {
        // StackWalker (Java 9+) = „przechodzień po stosie”: przegląda ramki bieżącego wątku od najnowszej.
        // getMethodName = pobierz nazwę metody; toList() (Java 16+) = zbierz do niemodyfikowalnej listy.
        return StackWalker.getInstance()
                .walk(frames -> frames.map(StackWalker.StackFrame::getMethodName).limit(4).toList());
    }

    static List<String> level3() {  // level = poziom
        return currentStack();
    }

    static List<String> level2() {
        return level3();
    }

    static List<String> level1() {
        return level2();
    }

    /** dive = nurkuj: BŁĄD CELOWY — rekurencja bez przypadku bazowego. */
    static void dive() {
        depth++;
        dive();
    }

    /**
     * 3. Każde wywołanie kładzie na stos nową ramkę (zmienne lokalne, parametry, adres powrotu), a powrót ją zdejmuje.
     * Stos wątku ma stały, ograniczony rozmiar (-Xss), więc zbyt głęboka rekurencja kończy się StackOverflowError.
     */
    static void framesAndOverflow() {
        section("3. Ramki stosu i StackOverflowError");

        show("ramki (od najnowszej)", level1());
        // WYNIK: ramki (od najnowszej) → [currentStack, level3, level2, level1]

        depth = 0;
        expectThrows("dive() bez przypadku bazowego", Jvm01Memory::dive);
        // WYNIK: ✔ dive() bez przypadku bazowego → rzucono StackOverflowError: (brak komunikatu)
        show("po błędzie stos jest odwinięty, program działa dalej", depth > 0);
        // WYNIK: po błędzie stos jest odwinięty, program działa dalej → true

        // Ile wywołań się zmieściło? To ZALEŻY: od -Xss, od liczby zmiennych lokalnych w metodzie, od tego, czy ramka
        // jest jeszcze interpretowana, czy już skompilowana przez JIT (skompilowane ramki bywają mniejsze), od systemu.
        // Dlatego nie wypisujemy tej liczby jako pewnego wyniku — przy każdym uruchomieniu może być inna.
        // Rozmiar stosu można podać dla jednego wątku: new Thread(null, zadanie, "nazwa", 64L * 1024 * 1024) —
        // ale dokumentacja uprzedza, że na niektórych platformach JVM może ten parametr zignorować.

        // PUŁAPKA: StackOverflowError to Error, nie Exception. Łapiemy go tu tylko do pokazu. W prawdziwym kodzie
        //   złapanie go w środku operacji może zostawić dane w połowie zmiany — lepiej nie dopuścić do przepełnienia.
        // DOBRA PRAKTYKA: głęboką rekurencję po danych o nieznanym rozmiarze (długa lista, zdegenerowane drzewo)
        //   zamień na pętlę z własnym stosem (ArrayDeque). Podbijanie -Xss tylko przesuwa granicę i zwiększa
        //   pamięć KAŻDEGO wątku — przy setkach wątków to setki megabajtów.
    }

    // =================================================================================================
    // 4. ROZMIARY PAMIĘCI: FLAGI -Xms, -Xmx, -Xss I KLASA Runtime
    // =================================================================================================

    /**
     * 4. Rozmiary obszarów ustawia się flagami przy uruchomieniu JVM. Z kodu można podejrzeć stan sterty przez
     * Runtime. Liczby zależą od maszyny, flag i chwili pomiaru — pewne są tylko zależności między nimi.
     */
    static void memorySizesAndRuntime() {
        section("4. Flagi pamięci i klasa Runtime");

        // Flagi (przykład: java -Xms256m -Xmx1g -Xss512k -cp out t26_jvm.Jvm01Memory):
        //   -Xms<rozmiar> = początkowy rozmiar sterty (initial heap). Bez flagi HotSpot dobiera go sam
        //                   (ergonomia) — na typowej maszynie ok. 1/64 pamięci RAM.
        //   -Xmx<rozmiar> = maksymalny rozmiar sterty (max heap). Bez flagi — na typowej maszynie ok. 1/4 RAM.
        //   -Xss<rozmiar> = rozmiar stosu KAŻDEGO wątku. Domyślnie zależy od systemu; na typowym 64-bitowym
        //                   Linuksie i Windows to 1 MB.
        //   -XX:MaxRAMPercentage=75 = maks. sterta jako procent RAM — wygodne w kontenerach (na Linuksie JVM
        //                   od Javy 10 widzi limit pamięci kontenera, a nie całej maszyny).
        //   -XX:MaxMetaspaceSize=256m = limit metadanych klas (domyślnie brak limitu — rośnie w pamięci natywnej).
        // Pamięć procesu Javy to NIE tylko -Xmx: dochodzą stosy wątków (liczba wątków × -Xss), metaspace, code cache,
        // struktury GC, bufory bezpośrednie (direct buffers). Dlatego proces bywa wyraźnie większy niż -Xmx.

        Runtime runtime = Runtime.getRuntime();  // Runtime = środowisko uruchomieniowe (jedna instancja na JVM)
        long max = runtime.maxMemory();          // maxMemory = maks. sterta, której JVM spróbuje użyć (≈ -Xmx)
        long total = runtime.totalMemory();      // totalMemory = sterta zarezerwowana TERAZ (zmienia się w czasie)
        long free = runtime.freeMemory();        // freeMemory = przybliżone wolne miejsce w ramach totalMemory
        long used = total - free;                // used = zajęte

        show("maxMemory() > 0", max > 0);
        show("totalMemory() <= maxMemory()", total <= max);
        show("freeMemory() <= totalMemory()", free <= total);
        show("zajęte (total - free) >= 0", used >= 0);
        show("availableProcessors() >= 1", runtime.availableProcessors() >= 1);
        // WYNIK: maxMemory() > 0 → true
        // WYNIK: totalMemory() <= maxMemory() → true
        // WYNIK: freeMemory() <= totalMemory() → true
        // WYNIK: zajęte (total - free) >= 0 → true
        // WYNIK: availableProcessors() >= 1 → true

        note(String.format(Locale.ROOT, "teraz: zajęte ok. %d MB, zarezerwowane %d MB, maksimum %d MB",
                used >> 20, total >> 20, max >> 20));   // >> 20 = podziel przez 1024 × 1024 (bajty → MB)
        // (wynik zależy od uruchomienia)

        // PUŁAPKA: freeMemory() to wolne miejsce tylko w OBECNIE zarezerwowanej części, a nie „ile jeszcze mogę
        //   zaalokować”. Realny zapas to mniej więcej maxMemory() - (totalMemory() - freeMemory()). A sama liczba
        //   „zajęte” obejmuje też śmieci, których GC jeszcze nie sprzątnął — dlatego skacze w górę i w dół.
        // Uwaga: gdy JVM nie ma limitu sterty, maxMemory() zwraca Long.MAX_VALUE; przy kolektorach Serial/Parallel
        //   wartość bywa nieco mniejsza niż -Xmx (zależy od kolektora).
        // Gdy sterta się skończy, a GC nic nie odzyska → OutOfMemoryError: Java heap space. Gdy zabraknie miejsca
        //   na metadane klas → OutOfMemoryError: Metaspace. Gdy system nie da pamięci na stos nowego wątku →
        //   OutOfMemoryError z komunikatem „unable to create native thread” (stosy są POZA stertą!).
    }

    // =================================================================================================
    // 5. PULA NAPISÓW I intern()
    // =================================================================================================

    /**
     * 5. Literały napisowe (i stałe czasu kompilacji) trafiają do puli napisów — ten sam tekst = ten sam obiekt.
     * Napisy tworzone w czasie działania (new String, sklejanie zmiennych, dane z pliku) to NOWE obiekty.
     * intern() zwraca egzemplarz z puli. Wszystkie poniższe wyniki gwarantuje specyfikacja języka (JLS).
     */
    static void stringPool() {
        section("5. Pula napisów (string pool) i intern()");

        String literal1 = "java";
        String literal2 = "java";                   // ten sam literał → ten sam obiekt z puli
        String created = new String("java");        // new ZAWSZE tworzy nowy obiekt na stercie
        String interned = created.intern();         // intern = „daj mi egzemplarz z puli”
        String constantConcat = "ja" + "va";        // stała czasu kompilacji — javac skleja to już przy kompilacji
        final String finalPart = "ja";              // final + literał = zmienna stała (constant variable)
        String finalConcat = finalPart + "va";      // więc to też stała czasu kompilacji
        String part = "ja";                         // zwykła zmienna — kompilator nie zakłada jej wartości
        String runtimeConcat = part + "va";         // sklejane w czasie działania → NOWY obiekt

        show("literal1 == literal2", literal1 == literal2);
        show("literal1 == new String(\"java\")", literal1 == created);
        show("literal1.equals(new String(\"java\"))", literal1.equals(created));
        show("literal1 == created.intern()", literal1 == interned);
        show("literal1 == \"ja\" + \"va\"", literal1 == constantConcat);
        show("literal1 == finalPart + \"va\"", literal1 == finalConcat);
        show("literal1 == part + \"va\"", literal1 == runtimeConcat);
        show("literal1 == (part + \"va\").intern()", literal1 == runtimeConcat.intern());
        show("literal1.equals(part + \"va\")", literal1.equals(runtimeConcat));
        // WYNIK: literal1 == literal2 → true
        // WYNIK: literal1 == new String("java") → false
        // WYNIK: literal1.equals(new String("java")) → true
        // WYNIK: literal1 == created.intern() → true
        // WYNIK: literal1 == "ja" + "va" → true
        // WYNIK: literal1 == finalPart + "va" → true
        // WYNIK: literal1 == part + "va" → false
        // WYNIK: literal1 == (part + "va").intern() → true
        // WYNIK: literal1.equals(part + "va") → true

        // Gdzie jest pula? W HotSpot od Javy 7 napisy z puli leżą na zwykłej stercie (wcześniej w osobnym obszarze
        // PermGen). Napis z puli, do którego nikt już się nie odwołuje, też może zostać sprzątnięty przez GC.
        // Compact strings (Java 9+) = zwarte napisy: tekst mieszczący się w Latin-1 zajmuje 1 bajt na znak, a tekst
        // z polskimi znakami (ą, ę, ł... są spoza Latin-1) — 2 bajty na KAŻDY znak całego napisu.

        // PUŁAPKA: == „działa” na literałach, więc test z literałami przechodzi, a na produkcji dane przychodzą
        //   z pliku, sieci albo sklejania — to nowe obiekty i == zwraca false. Błąd wychodzi dopiero u klienta.
        // DOBRA PRAKTYKA: napisy porównuj ZAWSZE przez equals (albo "stała".equals(zmienna) — odporne na null).
        //   Ręczne intern() rzadko się opłaca: pula ma swój koszt (tablica mieszająca w JVM). Gdy w pamięci są
        //   miliony powtórzonych napisów, G1 (w Javie 17) ma flagę -XX:+UseStringDeduplication — współdzieli
        //   wtedy tablice bajtów zduplikowanych napisów, ale NIE zmienia ich tożsamości (== dalej daje false).
    }

    // =================================================================================================
    // 6. ILE „WAŻY” OBIEKT — MODEL HotSpot 64-bit
    // =================================================================================================

    /** align8 = wyrównaj do 8: HotSpot domyślnie umieszcza obiekty pod adresami podzielnymi przez 8 bajtów. */
    static long align8(long bytes) {
        return (bytes + 7) / 8 * 8;
    }

    /**
     * 6. Szacunek rozmiaru obiektów dla typowej konfiguracji: 64-bitowy HotSpot z włączonymi skompresowanymi
     * wskaźnikami (compressed oops — domyślnie, gdy sterta jest mniejsza niż ok. 32 GB). To MODEL, a nie pomiar:
     * inna JVM, flagi albo sterta ponad 32 GB dają inne liczby. Dokładny pomiar: biblioteka JOL (Java Object Layout).
     */
    static void objectSizes() {
        section("6. Ile waży obiekt — szacunek dla 64-bitowego HotSpot");

        // Nagłówek obiektu = słowo znacznika (mark word, 8 B: hash tożsamości, stan blokady, wiek dla GC)
        //                  + skompresowany wskaźnik do klasy (4 B)  →  12 B.
        // Tablica ma dodatkowo pole length (4 B)  →  nagłówek tablicy 16 B. Referencja (compressed oop) = 4 B.
        final long header = 12;
        show("new Object()", align8(header) + " B");
        show("Integer (nagłówek + int)", align8(header + 4) + " B");
        show("Long (nagłówek + long)", align8(header + 8) + " B");
        // WYNIK: new Object() → 16 B
        // WYNIK: Integer (nagłówek + int) → 16 B
        // WYNIK: Long (nagłówek + long) → 24 B

        long intArray = align8(header + 4 + 1000L * 4);   // int[1000]: nagłówek tablicy + 1000 × 4 B
        long boxedList = align8(header + 4 + 4 + 4)       // obiekt ArrayList: pola size, modCount, elementData
                + align8(header + 4 + 1000L * 4)          // wewnętrzna Object[1000]: 1000 referencji po 4 B
                + 1000 * align8(header + 4);              // 1000 osobnych obiektów Integer
        show("int[1000]", intArray + " B");
        show("ArrayList<Integer> z 1000 liczb", boxedList + " B");
        show("stosunek", String.format(Locale.ROOT, "%.1f razy więcej", (double) boxedList / intArray));
        // WYNIK: int[1000] → 4016 B
        // WYNIK: ArrayList<Integer> z 1000 liczb → 20040 B
        // WYNIK: stosunek → 5.0 razy więcej

        // Założenia rachunku: pojemność listy dokładnie 1000 i liczby spoza zakresu -128..127 (Integer.valueOf
        // dla -128..127 zwraca obiekty z gotowej puli, więc one nie zajmują dodatkowego miejsca).
        // PUŁAPKA: kolekcja liczb opakowanych (Integer, Long) zużywa kilka razy więcej pamięci niż tablica prymitywów
        //   i rozrzuca dane po stercie (gorzej dla pamięci podręcznej procesora). Przy milionach elementów to
        //   dziesiątki megabajtów różnicy.
        // DOBRA PRAKTYKA: dla dużych zbiorów liczb używaj int[]/long[] albo IntStream (t16_streams) — a kolekcji
        //   obiektów tam, gdzie liczy się wygoda. Najpierw mierz (Jvm04), potem optymalizuj.
    }

    // =================================================================================================
    // 7. WYCIEKI PAMIĘCI: STATYCZNA KOLEKCJA I ZAPOMNIANI SŁUCHACZE
    // =================================================================================================

    /** AUDIT_LOG = dziennik audytu. static = żyje tak długo jak klasa, czyli zwykle do końca programu. */
    private static final List<String> AUDIT_LOG = new ArrayList<>();

    /** handleRequestLeaky = obsłuż żądanie (wersja cieknąca): dopisuje do statycznej listy i nigdy nie usuwa. */
    static void handleRequestLeaky(int requestId) {
        AUDIT_LOG.add("żądanie " + requestId);
    }

    /** Subscription = subskrypcja: „bilet”, którym słuchacz może się wypisać (cancel = anuluj). */
    interface Subscription {
        void cancel();
    }

    /** EventBus = szyna zdarzeń: trzyma słuchaczy (listeners) i woła ich przy publish (opublikuj). */
    static final class EventBus {
        private final List<Runnable> listeners = new ArrayList<>();

        /** subscribe = zapisz się. Szyna trzyma SILNĄ referencję do słuchacza, dopóki ktoś nie wywoła cancel(). */
        Subscription subscribe(Runnable listener) {
            listeners.add(listener);
            return () -> listeners.remove(listener);
        }

        void publish() {
            listeners.forEach(Runnable::run);
        }

        int listenerCount() {
            return listeners.size();
        }
    }

    /** Screen = ekran aplikacji z dużym buforem. Zapomniany słuchacz trzyma w pamięci CAŁY ekran z buforem. */
    static final class Screen {
        private final byte[] buffer = new byte[10_000];

        void refresh() {   // refresh = odśwież
            buffer[0]++;
        }
    }

    /**
     * 7. Wyciek w Javie = obiekt już niepotrzebny, ale wciąż OSIĄGALNY (ktoś trzyma do niego referencję), więc GC
     * nie ma prawa go usunąć. Dwa klasyki: statyczna kolekcja, która tylko rośnie, i słuchacz, którego nikt nie wypisał.
     */
    static void leaksStaticAndListeners() {
        section("7. Wycieki: statyczna kolekcja i zapomniani słuchacze");

        for (int requestId = 1; requestId <= 5_000; requestId++) {
            handleRequestLeaky(requestId);
        }
        show("AUDIT_LOG po 5000 żądaniach", AUDIT_LOG.size());
        // WYNIK: AUDIT_LOG po 5000 żądaniach → 5000

        Deque<String> lastEntries = new ArrayDeque<>();   // Deque = kolejka dwustronna; tu: bufor ostatnich wpisów
        for (int requestId = 1; requestId <= 5_000; requestId++) {
            if (lastEntries.size() == 100) {
                lastEntries.removeFirst();                // najstarszy wypada — rozmiar ma sufit
            }
            lastEntries.addLast("żądanie " + requestId);
        }
        show("bufor z limitem 100 po 5000 żądaniach", lastEntries.size());
        show("najstarszy zachowany wpis", lastEntries.peekFirst());
        // WYNIK: bufor z limitem 100 po 5000 żądaniach → 100
        // WYNIK: najstarszy zachowany wpis → żądanie 4901
        AUDIT_LOG.clear();

        EventBus leakyBus = new EventBus();
        for (int i = 0; i < 3; i++) {
            Screen screen = new Screen();
            leakyBus.subscribe(screen::refresh);          // referencja do metody „przykleja” obiekt screen
            leakyBus.publish();
        }                                                 // ekran „zamknięty”, ale szyna wciąż go trzyma
        show("słuchacze po zamknięciu 3 ekranów (bez cancel)", leakyBus.listenerCount());
        // WYNIK: słuchacze po zamknięciu 3 ekranów (bez cancel) → 3

        EventBus bus = new EventBus();
        for (int i = 0; i < 3; i++) {
            Screen screen = new Screen();
            Subscription subscription = bus.subscribe(screen::refresh);
            try {
                bus.publish();
            } finally {
                subscription.cancel();                    // finally = wypisz się ZAWSZE, nawet po wyjątku
            }
        }
        show("słuchacze po zamknięciu 3 ekranów (z cancel)", bus.listenerCount());
        // WYNIK: słuchacze po zamknięciu 3 ekranów (z cancel) → 0

        // PUŁAPKA: screen::refresh (albo lambda używająca screen) to obiekt, który trzyma referencję do screen.
        //   Dopóki szyna trzyma słuchacza, ekran z buforem 10 000 B jest osiągalny — GC go nie ruszy. Przy
        //   aplikacji otwierającej ekrany tysiące razy pamięć rośnie bez końca.
        // DOBRA PRAKTYKA: każda rejestracja (subscribe, addListener) ma parę (cancel, removeListener) w finally
        //   albo w metodzie zamykającej obiekt. Kolekcje static projektuj z limitem albo z jasnym momentem czyszczenia.
    }

    // =================================================================================================
    // 8. WYCIEKI PAMIĘCI: CACHE BEZ LIMITU I ZMIENNY KLUCZ W HashMap
    // =================================================================================================

    /** expensiveReport = kosztowny raport (udajemy długie liczenie). */
    static String expensiveReport(int requestId) {
        return "raport-" + requestId;
    }

    /** MutableKey = zmienny klucz: equals/hashCode liczone z pola, które można zmienić PO włożeniu do mapy. */
    static final class MutableKey {
        int id;

        MutableKey(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof MutableKey key && key.id == id;   // instanceof ze wzorcem (Java 16+)
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }
    }

    /**
     * 8. Dwa podstępne wycieki w mapach: cache, który tylko rośnie (klucz nigdy się nie powtarza), oraz klucz,
     * którego hashCode zmienił się po włożeniu — wpis „ginie” w złym kubełku i nie da się go usunąć.
     */
    static void leaksCacheAndKeys() {
        section("8. Wycieki: cache bez limitu i zmienny klucz w HashMap");

        Map<Integer, String> reportCache = new HashMap<>();
        for (int requestId = 1; requestId <= 5_000; requestId++) {
            reportCache.computeIfAbsent(requestId, Jvm01Memory::expensiveReport);   // computeIfAbsent = policz, jeśli brak
        }
        show("cache po 5000 żądaniach", reportCache.size());
        // WYNIK: cache po 5000 żądaniach → 5000
        // Klucz = numer żądania, który nigdy się nie powtarza → ani jednego trafienia, za to 5000 wpisów na zawsze.
        // To nie cache, tylko wyciek. Cache z limitem (LRU) zbudujesz w ćwiczeniu 4.

        Map<MutableKey, String> sessions = new HashMap<>();
        MutableKey key = new MutableKey(1);
        sessions.put(key, "sesja Ali");
        key.id = 2;                                         // zmiana pola, z którego liczony jest hashCode!
        show("sessions.containsKey(key)", sessions.containsKey(key));
        show("sessions.get(new MutableKey(1))", sessions.get(new MutableKey(1)));
        show("sessions.remove(key)", sessions.remove(key));
        show("sessions.size()", sessions.size());
        // WYNIK: sessions.containsKey(key) → false
        // WYNIK: sessions.get(new MutableKey(1)) → null
        // WYNIK: sessions.remove(key) → null
        // WYNIK: sessions.size() → 1
        // Dlaczego? Wpis leży w kubełku wyliczonym z hashCode = 1. Szukanie po key liczy hashCode = 2 → inny kubełek,
        // pusty. Szukanie po new MutableKey(1) trafia w dobry kubełek, ale equals porównuje id 1 z id 2 → false.
        // Wpis jest w mapie, ale nieosiągalny przez API → nie da się go usunąć → wyciek.

        // PUŁAPKA: klucze HashMap/HashSet muszą być niezmienne (przynajmniej pola używane w equals/hashCode).
        //   Najprościej: rekord (t09_records) albo klasa z polami final.
        // DOBRA PRAKTYKA: każdy cache ma limit rozmiaru albo czas wygaśnięcia. W projektach używa się gotowych
        //   bibliotek cache (np. Caffeine), a w prostych przypadkach LinkedHashMap z removeEldestEntry (ćwiczenie 4).
        // Inne częste źródła wycieków: ThreadLocal nieczyszczony w puli wątków (wątek żyje wiecznie — t21_concurrency),
        //   niestatyczna klasa wewnętrzna/lambda trzymająca obiekt zewnętrzny, niezamknięte zasoby (pliki, połączenia),
        //   a na serwerach aplikacji — klasy starej wersji aplikacji trzymane przez ich class loader (Metaspace, Jvm02).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Stos: 1 na wątek, ramki z lokalnymi zmiennymi i parametrami; znika przy powrocie z metody; -Xss.
     *   • Sterta: wspólna, WSZYSTKIE obiekty i tablice (też pola int w obiektach, pula napisów); -Xms/-Xmx; sprząta GC.
     *   • Metaspace: metadane klas w pamięci natywnej; code cache: kod z JIT.
     *   • Zmienna obiektowa = referencja. Przypisanie i przekazanie do metody kopiują REFERENCJĘ (zawsze przez wartość).
     *   • Za głęboka rekurencja → StackOverflowError (głębokość zależy od -Xss, ramek, JIT) → zamień na pętlę.
     *   • Runtime: maxMemory ≈ -Xmx, totalMemory = zarezerwowane teraz, freeMemory = wolne w zarezerwowanym.
     *   • Literały i stałe czasu kompilacji → pula (== true); new String i sklejanie zmiennych → nowy obiekt.
     *     intern() zwraca egzemplarz z puli. Napisy porównuj przez equals.
     *   • 64-bit HotSpot z compressed oops: nagłówek 12 B, wyrównanie do 8 B, Integer = 16 B, int[n] ≈ 16 + 4n.
     *   • Wyciek = niepotrzebny, ale osiągalny obiekt: static kolekcje, słuchacze bez wypisania, cache bez limitu,
     *     zmienne klucze w HashMap, ThreadLocal w puli wątków.
     *
     * PYTANIA KONTROLNE:
     *   1. Gdzie leżą: lokalna zmienna int, lokalna zmienna String, pole int obiektu, tablica int[] z metody?
     *   2. Co wypisze:  Box a = new Box(1); Box b = a; b.value = 5; System.out.println(a.value);  ?
     *   3. Co wypisze:  String s = "ab"; String t = "a"; System.out.println((s == t + "b") + " " + (s == "a" + "b"));  ?
     *   4. ZNAJDŹ BŁĄD:  String command = scanner.nextLine();  if (command == "koniec") { stop(); }
     *   5. Dlaczego podbicie -Xss nie jest dobrym pierwszym lekarstwem na StackOverflowError?
     *   6. Java ma GC — jak więc może dojść do wycieku pamięci? Podaj dwa przykłady.
     *   7. ZNAJDŹ BŁĄD:  Set<Point> visited = new HashSet<>(); visited.add(p); p.x++; visited.remove(p);
     *      (Point ma equals/hashCode liczone z x i y, pola nie są final.)
     *   8. Program z -Xmx512m zajmuje w systemie 800 MB. Czy to na pewno błąd? Co jeszcze zużywa pamięć?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: rozmiary tablic", List.of(16L, 24L, 56L, 56L),
                () -> List.of(exercise1(0, 4), exercise1(3, 1), exercise1(10, 4), exercise1(5, 8)));
        Check.equal("ćw. 2: różne obiekty w liście napisów", 2, () -> exercise2(poolTestInput()));
        Check.equal("ćw. 3: sesje po 1000 logowaniach Ali", 1, () -> exercise3());
        Check.equal("ćw. 4: klucze cache LRU po scenariuszu", List.of(5, 3, 6), () -> lruScenario(exercise4(3)));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of(16L, 24L, 56L, 56L),
                () -> List.of(solution1(0, 4), solution1(3, 1), solution1(10, 4), solution1(5, 8)));
        Check.equal("ćw. 2 (wzorzec)", 2, () -> solution2(poolTestInput()));
        Check.equal("ćw. 3 (wzorzec)", 1, () -> solution3());
        Check.equal("ćw. 4 (wzorzec)", List.of(5, 3, 6), () -> lruScenario(solution4(3)));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /** poolTestInput = dane testowe do ćw. 2: pięć napisów „kawa” zdobytych na różne sposoby. */
    static List<String> poolTestInput() {
        String fromPool = "kawa";
        String fresh = new String("kawa");
        return List.of(fromPool, "kawa", fresh, new String("kawa").intern(), "ka" + "wa");
    }

    /** lruScenario = scenariusz testowy do ćw. 4: wkłada klucze 1..5, czyta 3, wkłada 6; zwraca kolejność kluczy. */
    static List<Integer> lruScenario(Map<Integer, String> cache) {
        for (int i = 1; i <= 5; i++) {
            cache.put(i, "wartość " + i);
        }
        cache.get(3);
        cache.put(6, "wartość 6");
        return new ArrayList<>(cache.keySet());
    }

    /** LeakySessionKey = cieknący klucz sesji: BEZ equals/hashCode, więc każdy obiekt jest „inny”. */
    static final class LeakySessionKey {
        private final String user;

        LeakySessionKey(String user) {
            this.user = user;
        }

        @Override
        public String toString() {
            return "Klucz[" + user + "]";
        }
    }

    /**
     * ĆWICZENIE 1 (łatwe): oszacuj rozmiar tablicy w bajtach wg modelu z sekcji 6: nagłówek tablicy 16 B
     * + length × elementBytes, całość wyrównana w górę do wielokrotności 8.
     * Przykład: byte[3] → 16 + 3 = 19 → 24. Podpowiedź: użyj align8 z sekcji 6.
     */
    static long exercise1(int length, int elementBytes) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (średnie): policz, ile RÓŻNYCH OBIEKTÓW (nie różnych wartości!) jest na liście.
     * Dane z poolTestInput: literał, ten sam literał, new String, new String(...).intern(), stała "ka" + "wa".
     * Podpowiedź: zbiór porównujący przez == to {@code Collections.newSetFromMap(new IdentityHashMap<>())}
     * (IdentityHashMap = mapa tożsamościowa). Zwykły HashSet dałby 1, bo porównuje przez equals.
     */
    static int exercise2(List<String> texts) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ klucz sesji tak, żeby mapa przestała „puchnąć”.
     * Stary sposób (poniżej, w metodzie) — każde logowanie tej samej osoby dodaje NOWY wpis:
     * <pre>{@code
     * Map<LeakySessionKey, String> sessions = new HashMap<>();
     * for (int i = 0; i < 1000; i++) {
     *     sessions.put(new LeakySessionKey("ala"), "sesja " + i);   // 1000 wpisów!
     * }
     * }</pre>
     * Nowy sposób: zadeklaruj w metodzie lokalny rekord {@code record SessionKey(String user) {}} i użyj go jako klucza.
     * Podpowiedź: rekord ma equals/hashCode z automatu (t09_records/Records01Basics) — dwa klucze „ala” są równe.
     */
    static int exercise3() {
        // TODO: przepisz na rekord — na razie działa stara, cieknąca wersja
        Map<LeakySessionKey, String> sessions = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            sessions.put(new LeakySessionKey("ala"), "sesja " + i);
        }
        return sessions.size();
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zwróć mapę-cache LRU (least recently used = najdawniej używany) z limitem maxEntries.
     * Po przekroczeniu limitu ma wypadać wpis, którego NAJDAWNIEJ używano (odczyt get też liczy się jako użycie).
     * Scenariusz testu: put 1..5, get(3), put 6 → przy limicie 3 zostają klucze [5, 3, 6] (w kolejności użycia).
     * Podpowiedź: podklasa LinkedHashMap z konstruktorem {@code super(16, 0.75f, true)} (true = kolejność dostępu)
     * i nadpisaną metodą removeEldestEntry (usuń najstarszy wpis), która zwraca {@code size() > maxEntries}.
     * Klasa dziedzicząca po LinkedHashMap jest Serializable — dodaj pole serialVersionUID, bo inaczej -Xlint ostrzeże.
     */
    static Map<Integer, String> exercise4(int maxEntries) {
        // TODO: twoje rozwiązanie (zwykła HashMap nie ma limitu — to właśnie wyciek z sekcji 8)
        return new HashMap<>();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static long solution1(int length, int elementBytes) {
        return align8(16 + (long) length * elementBytes);
    }

    static int solution2(List<String> texts) {
        Set<String> identities = Collections.newSetFromMap(new IdentityHashMap<>());
        identities.addAll(texts);
        return identities.size();
    }

    static int solution3() {
        record SessionKey(String user) {
        }
        Map<SessionKey, String> sessions = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            sessions.put(new SessionKey("ala"), "sesja " + i);
        }
        return sessions.size();
    }

    /** LruCache = cache LRU oparty na LinkedHashMap w trybie kolejności dostępu (access order). */
    static final class LruCache<K, V> extends LinkedHashMap<K, V> {
        private static final long serialVersionUID = 1L;
        private final int maxEntries;

        LruCache(int maxEntries) {
            super(16, 0.75f, true);
            this.maxEntries = maxEntries;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxEntries;
        }

        @Override
        public boolean equals(Object other) {
            return super.equals(other) && other instanceof LruCache<?, ?> cache && cache.maxEntries == maxEntries;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), maxEntries);
        }
    }

    static Map<Integer, String> solution4(int maxEntries) {
        return new LruCache<>(maxEntries);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. int lokalny — w ramce na stosie (sama wartość). String lokalny — referencja w ramce, obiekt na stercie.
     *      Pole int — wewnątrz obiektu, czyli na stercie. Tablica int[] — to obiekt, więc na stercie
     *      (w ramce tylko referencja do niej).
     *   2. 5 — a i b to dwie kopie referencji do jednego obiektu.
     *   3. "false true" — t + "b" jest sklejane w czasie działania (nowy obiekt), a "a" + "b" to stała czasu
     *      kompilacji, trafia do puli jak literał "ab".
     *   4. == porównuje referencje; tekst z nextLine() to nowy obiekt, więc warunek będzie false nawet dla
     *      „koniec”. Poprawnie: "koniec".equals(command).
     *   5. Bo zwykle przyczyną jest rekurencja bez końca albo zależna od rozmiaru danych — większy stos tylko
     *      przesuwa granicę. Do tego -Xss dotyczy KAŻDEGO wątku, więc rośnie zużycie pamięci całego procesu.
     *      Lepiej naprawić przypadek bazowy albo zamienić rekurencję na pętlę.
     *   6. GC usuwa tylko obiekty NIEOSIĄGALNE. Wyciek to obiekt zbędny, ale wciąż osiągalny, np. statyczna lista,
     *      do której tylko dopisujemy; słuchacz zarejestrowany i nigdy niewypisany; cache bez limitu;
     *      ThreadLocal w puli wątków.
     *   7. Po p.x++ zmienia się hashCode, więc remove(p) szuka w innym kubełku i nic nie usuwa — punkt zostaje
     *      w zbiorze na zawsze (a contains(p) zwraca false). Pola używane w equals/hashCode powinny być final
     *      (albo użyj rekordu).
     *   8. Niekoniecznie. -Xmx ogranicza tylko stertę. Proces zużywa też stosy wątków, metaspace, code cache,
     *      struktury GC, bufory bezpośrednie i pamięć samej JVM. Sprawdzisz to np. przez Native Memory Tracking (Jvm04).
     */
    // </editor-fold>
}
