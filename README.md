# JavaLearning — kurs Javy po polsku do nauki i powtórek

Samodzielny kurs Javy SE 17 w 36 działach (`t00`–`t35`). Każda lekcja to plik `.java`, który możesz uruchomić. Ma szczegółowe komentarze po polsku, a przy każdej angielskiej nazwie jest tłumaczenie. Każda lekcja zawiera też:
- ćwiczenia sprawdzane automatycznie,
- pytania kontrolne,
- ściągę do powtórek.

> **Zacznij tutaj:** [`t00_start/Start01HowToUse.java`](src/t00_start/Start01HowToUse.java). Tam jest opis budowy lekcji, tagów, wyszukiwania i uruchamiania.
>
> **Co dalej:** po Javie kontynuuj kurs Springa w tym samym stylu — [SpringLearning](https://github.com/arturoller/SpringLearning).

## Szybki start
1. Zainstaluj **JDK 17** (albo nowszy).
2. Sklonuj repozytorium i otwórz folder projektu w IntelliJ IDEA (File → Open → wskaż `pom.xml` → Open as Project).
3. Otwórz dowolną lekcję, np. `src/t01_basics/Basics01HelloJvm.java`, i kliknij zielony trójkąt ▶ obok `main`.

Mavena nie trzeba instalować. IntelliJ pobierze zależności sam (Lombok jest potrzebny tylko w `t20_lombok`). Z linii poleceń służy Maven Wrapper, który sam pobiera właściwą wersję Mavena: `./mvnw compile` (Windows: `mvnw.cmd compile`).

Folder `tools/` zawiera narzędzia autorów kursu (weryfikator lekcji, rejestr lekcji, instrukcje dla agentów AI). Do nauki nie jest potrzebny.

## Struktura
```
src/
├── helpers/          wspólne narzędzia do lekcji (Console, Check, SampleData, model/)
├── t00_start/        jak korzystać z kursu, słowniczek, ścieżka nauki, dziennik powtórek
├── t01_basics/       podstawy
├── ...
└── t35_capstone/     mini-projekty łączące tematy
```
Każdy temat to osobny folder (= pakiet) bezpośrednio w `src/`. Pakiety mają numery `t00_`, `t01_`… Dzięki temu drzewo projektu układa się w kolejności nauki.

## Jak uczyć się z jednej lekcji
1. Przeczytaj nagłówek i ŚCIĄGĘ na końcu pliku.
2. Przed uruchomieniem każdej sekcji **przewidź wynik**. Potem uruchom ▶ i porównaj z `// WYNIK:`.
3. Zmień coś w kodzie i sprawdź, czy rozumiesz, co się stało.
4. Odpowiedz na PYTANIA KONTROLNE bez patrzenia w kod. Odpowiedzi są w zwiniętym bloku na końcu pliku.
5. Rozwiąż ćwiczenia (`exerciseN`). Checker pokazuje ✘, dopóki rozwiązanie nie jest poprawne. Rozwiązania wzorcowe też są zwinięte na końcu pliku.
6. Rób powtórki po 1, 3, 7, 14 i 30 dniach: najpierw pytania z pamięci, dopiero potem ściąga.
   - Plan i metoda: [`t00_start/Start03LearningPath.java`](src/t00_start/Start03LearningPath.java).
   - Co dziś powtórzyć: [`t00_start/Start04ReviewTracker.java`](src/t00_start/Start04ReviewTracker.java).

## Tagi do wyszukiwania (Ctrl+Shift+F)
Tagi zawsze mają dokładnie tę postać (wielkie litery + dwukropek), więc wyszukiwanie znajduje wszystkie wystąpienia.

| Tag | Co znajdziesz |
|---|---|
| `TEMAT:` | nagłówki wszystkich lekcji |
| `W SKRÓCIE:` | streszczenia lekcji |
| `ANALOGIA:` | porównania z życia |
| `JAK TO DZIAŁA:` | mechanizm krok po kroku |
| `SŁÓWKA:` | angielskie słówka z tłumaczeniem |
| `ZOBACZ TEŻ:` | powiązania między lekcjami |
| `PUŁAPKA:` | typowe błędy |
| `DOBRA PRAKTYKA:` | zasady „jak robić dobrze” i dlaczego |
| `WYNIK:` | co wypisze dany fragment kodu |
| `ŚCIĄGA:` | podsumowania do powtórek |
| `PYTANIA KONTROLNE:` | pytania do samosprawdzenia (także „co wypisze?” i „znajdź błąd”) |
| `ĆWICZENIE` | zadania; bez dwukropka, bo mają numery: `ĆWICZENIE 1:` |

## Spis treści
Legenda: ✅ gotowe · 🔶 w trakcie · ⏳ zaplanowane

| Dział | Temat | Stan |
|---|---|---|
| `helpers/` | Wspólne narzędzia (`Console`, `Check`, `Sleep`, `TempDir`) i dane przykładowe (`SampleData`, `model/`) | ✅ |
| `t00_start` | Jak korzystać z kursu, słowniczek, ścieżka nauki, dziennik powtórek, debugowanie w IntelliJ (breakpointy, krokowanie, warunki, wyjątki) (✅); do dopisania: Git od podstaw (commit, branch, merge, rebase, pull request), refaktoryzacje i skróty IntelliJ, JShell (⏳) | 🔶 |
| `t01_basics` | Podstawy (11 lekcji): jak działa program, typy proste, zmienne, operatory, rzutowanie i przepełnienie, klasy opakowujące, Math i liczby losowe, pułapki double, referencje i przekazywanie przez wartość, Scanner, wypisywanie i printf | ✅ |
| `t02_controlflow` | Sterowanie (5 lekcji): if/else i klauzule strażnika, switch (klasyczny i wyrażenie), pętle, break/continue/etykiety, wzorce pętli | ✅ |
| `t03_arrays` | Tablice (5 lekcji): podstawy, tablice wielowymiarowe, klasa `Arrays`, algorytmy pisane ręcznie, varargs | ✅ |
| `t04_strings` | Napisy (7 lekcji): niezmienność i pula, == kontra equals, metody String, StringBuilder/StringJoiner, formatowanie i bloki tekstu, wyrażenia regularne, char i Unicode (polskie litery, emoji), algorytmy na tekście (✅); do dopisania: regex dla zaawansowanych (grupy nazwane, lookahead, flagi, walidatory), pełna ściąga formatowania (Formatter, MessageFormat) (⏳) | 🔶 |
| `t05_methods` | Metody (4 lekcje): budowa i stos wywołań, przeciążanie i wybór wersji, rekurencja (memoizacja, StackOverflowError), dobre praktyki | ✅ |
| `t06_oop_basics` | Obiektowość (9 lekcji): klasy i obiekty, konstruktory i kolejność inicjalizacji, hermetyzacja, static, `toString`/`equals`/`hashCode`, niezmienność i kopie obronne, klasy zagnieżdżone, pakiety i modyfikatory dostępu, obiekty wartości (✅); do dopisania: kopiowanie obiektów — clone() i jego pułapki, konstruktor kopiujący, kopia płytka i głęboka (⏳) | 🔶 |
| `t07_inheritance_polymorphism` | Dziedziczenie (8 lekcji): podstawy i łańcuch konstruktorów, nadpisywanie i jego pułapki (ukrywanie pól i metod statycznych, metoda wołana z konstruktora), klasy abstrakcyjne, interfejsy (default/static/private, konflikt diamentu), polimorfizm i rzutowanie, kompozycja kontra dziedziczenie (kruchość klasy bazowej, LSP), sealed, SOLID | ✅ |
| `t08_enums` | Enumy (4 lekcje): podstawy (values, valueOf, ordinal, switch), pola/konstruktor/metody i wyszukiwanie po kodzie, zachowanie stałych (ciała, lambdy, interfejs, singleton), EnumSet/EnumMap i maszyna stanów | ✅ |
| `t09_records` | Rekordy (3 lekcje): co generuje record, płytka niezmienność, konstruktor kompaktowy (walidacja, normalizacja, kopie obronne), fabryki i „withery”, rekordy generyczne i lokalne, Comparable, klucze map, sealed + instanceof | ✅ |
| `t10_exceptions` | Wyjątki (7 lekcji): try/catch/finally i stos wywołań, checked kontra unchecked i throws, kilka catch i multi-catch, try-with-resources i wyjątki stłumione, własne wyjątki z danymi, łańcuch przyczyn i opakowywanie, dobre praktyki i antywzorce | ✅ |
| `t11_generics` | Typy generyczne (7 lekcji): po co generyki (surowe typy, remove(int)), własne klasy i interfejsy, metody generyczne i wnioskowanie, ograniczenia (extends, &), dżokery ? extends / ? super i PECS, wymazywanie typów i obejścia, generyczne repozytorium | ✅ |
| `t12_collections` | Kolekcje (13 lekcji): przegląd i wybór kolekcji, listy (subList, Arrays.asList), iterowanie i modyfikacja (ConcurrentModificationException), zbiory, mapy (merge, computeIfAbsent, LRU), kolejki i ArrayDeque/PriorityQueue, Comparable/Comparator (polskie sortowanie), kolekcje niemodyfikowalne, klasa Collections, wzorce, jak działa HashMap, własny Iterable, wydajność kolekcji | ✅ |
| `t13_lambdas` | Lambdy: od klasy anonimowej do lambdy, interfejsy funkcyjne, `java.util.function`, referencje do metod, składanie funkcji, domknięcia, funkcje wyższego rzędu, pułapki (8 lekcji) | ✅ |
| `t14_optional` | Optional: podstawy, przekształcanie (map/flatMap/filter/or), dobre praktyki (3 lekcje) | ✅ |
| `t15_numbers` | BigDecimal, kwota jako obiekt wartości (VAT, raty), BigInteger, formatowanie i parsowanie liczb, sztuczki na liczbach całkowitych (5 lekcji ✅); do dopisania: ściąga klasy Math (trygonometria, logarytmy, zaokrąglanie ujemnych, ulp, StrictMath) (⏳) | 🔶 |
| `t16_streams` | **Streamy**, 20 lekcji: wprowadzenie, tworzenie, filter/map, flatMap, sortowanie/distinct/limit, operacje końcowe, reduce, strumienie liczbowe, kolektory, toMap, groupingBy, partitioningBy, zaawansowane kolektory, Optional w streamach, pieniądze (BigDecimal), leniwość, efekty uboczne i pułapki, strumienie równoległe, 39 przepisów, 23 ćwiczenia | ✅ |
| `t17_datetime` | Data i czas, java.time (5 lekcji): LocalDate/LocalTime/LocalDateTime i Clock, Period/Duration/ChronoUnit, DateTimeFormatter (polskie nazwy, tryb STRICT, pułapki YYYY i mm), strefy i Instant (zmiana czasu), praktyka (dni robocze, YearMonth, kolizje spotkań, kalendarz) (✅); do dopisania: DateTimeFormatter dla zaawansowanych (formatery ISO, ofLocalized, DateTimeFormatterBuilder, kilka formatów naraz) (⏳) | 🔶 |
| `t18_io_files` | Pliki (14 lekcji): Path i Files (tworzenie, kopiowanie, przenoszenie, usuwanie), odczyt tekstu (readString, lines, BufferedReader, Scanner), zapis (opcje otwarcia, flush/close, bezpieczny zapis przez plik tymczasowy), CSV z walidacją i cudzysłowami, JSON ręcznie (własny zapis i parser), Properties (UTF-8, warstwy konfiguracji), przechodzenie katalogów (walk, find, glob, walkFileTree), strumienie binarne (DataStream, własny format, HexFormat), serializacja (transient, serialVersionUID, rekordy, ObjectInputFilter), własny logger (poziomy, Clock, rotacja, java.util.logging), wyjątki IO (hierarchia, zasoby stłumione, TOCTOU, ponawianie), kodowanie znaków (UTF-8, windows-1250, BOM, emoji, NFC/NFD), archiwa ZIP i GZIP (zip slip, bomba kompresyjna), XML (DOM, StAX, XPath, ochrona przed XXE) | ✅ |
| `t19_annotations_reflection` | Adnotacje i refleksja (7 lekcji): adnotacje wbudowane i meta-adnotacje, własne adnotacje (retencja, @Repeatable, @Inherited), refleksja (Class, Method.invoke, setAccessible i moduły), mini-walidator, mini-framework komend (jak @GetMapping), dynamiczne proxy (podstawa @Transactional w Springu), procesory adnotacji z działającą kompilacją w lekcji | ✅ |
| `t20_lombok` | Lombok (4 lekcje): gettery/settery, @ToString, @EqualsAndHashCode (callSuper), konstruktory i @NonNull, @Data/@Value/@Builder/@With i porównanie z rekordami, @Cleanup/@SneakyThrows/@Log/lombok.config i ich pułapki; przy każdej adnotacji kod, który Lombok generuje | ✅ |
| `t21_concurrency` | Współbieżność (10 lekcji): wątki (start/run, join, stany, demony, przerwania), wyścigi (count++, sprawdź-potem-działaj, synchronized, Atomic*, LongAdder), blokady (ReentrantLock, Condition, ReadWriteLock, StampedLock, wait/notify, zakleszczenie i jego wykrywanie), pule wątków (ExecutorService, Future, invokeAll, ThreadPoolExecutor i polityki odrzucania), CompletableFuture (thenCompose/thenCombine, allOf, obsługa błędów, limity czasu), kolekcje współbieżne (ConcurrentHashMap, CopyOnWriteArrayList, BlockingQueue, producent–konsument), synchronizatory (CountDownLatch, CyclicBarrier, Semaphore, Phaser), wzorce bezpieczeństwa wątkowego (niezmienność, ThreadLocal, bezstanowe serwisy jak w Springu), zadania cykliczne i ForkJoin, model pamięci Javy (volatile, happens-before, double-checked locking) | ✅ |
| `t22_design_patterns` | Wzorce projektowe (15 lekcji): strategia, budowniczy, fabryka (metody statyczne, metoda wytwórcza, fabryka abstrakcyjna), singleton (holder, enum, double-checked locking), metoda szablonowa, obserwator, dekorator, wstrzykiwanie zależności (z mini-kontenerem), polecenie (undo/redo), fasada, adapter, kompozyt, stan, łańcuch odpowiedzialności, wizytator; w każdej lekcji: problem bez wzorca, wzorzec krok po kroku, wersja z lambdami/rekordami/sealed, przykłady z JDK i Springa, pułapki nadużywania i testowanie; na końcu tabela wszystkich 15 wzorców | ✅ |
| `t23_modern_java` | Nowości Java 8→17 i zapowiedź 21+ (7 lekcji): Java 8 (lambdy, metody default i reguły diamentu, Stream, Optional, java.time, nowe metody map), var (gdzie wolno, pułapki z rombem i literałami), wyrażenia switch (strzałki, yield, wyczerpywalność enumów), bloki tekstowe (wcięcia, \s, końce linii, formatted), rekordy + sealed + instanceof ze wzorcem (typy algebraiczne), drobne nowości API z wydań 9–17 (tabela), co przynoszą Java 18–25 (UTF-8 domyślnie, wzorce w switch, wątki wirtualne, kolekcje sekwencyjne, `_`, gatherers) i strategia aktualizacji | ✅ |
| `t24_algorithms` | Algorytmy i matematyka (18 lekcji). Algorytmy: złożoność, sortowanie, wyszukiwanie, własne struktury danych, klasyki, rekurencja z nawrotami (backtracking), programowanie dynamiczne, grafy (BFS/DFS). Matematyka: teoria liczb (NWD, liczby pierwsze, sito Eratostenesa), arytmetyka modularna i sumy kontrolne (PESEL, NIP, Luhn, IBAN), kombinatoryka, systemy liczbowe i liczby rzymskie, metody numeryczne (Newton, bisekcja, całkowanie, Monte Carlo), statystyka, macierze i geometria, wielkie liczby, klasyki z rozmów kwalifikacyjnych (silnia, Fibonacci, liczby pierwsze, palindromy, sztuczki bitowe), zadania w stylu Project Euler | ✅ |
| `t25_testing` | Testowanie — pojęcia bez frameworka (3 lekcje): po co testy, piramida testów, AAA i FIRST, własny mini-runner testów, dublery (dummy/stub/fake/spy/mock), kod łatwy do testowania (wstrzykiwanie zależności, Clock) | ✅ |
| `t26_jvm` | JVM (4 lekcje): pamięć (stos, sterta, pula napisów, wycieki), ładowanie i inicjalizacja klas (kolejność, stałe wklejane w kod, holder idiom, class loadery), odśmiecanie (osiągalność, generacje, G1, referencje słabe i miękkie, Cleaner), narzędzia diagnostyczne (jcmd, jstack, JFR, VisualVM, wykrywanie zakleszczeń, JIT) | ✅ |
| `t27_clean_code_pitfalls` | Pułapki i czysty kod (4 lekcje): 22 klasyczne pułapki Javy, typowe uwagi z code review (PRZED/PO), zasady czystego kodu (nazwy, DRY/KISS/YAGNI, CQS, fail fast), SOLID na jednym realistycznym przykładzie (moduł faktur) (✅); do dopisania: architektura aplikacji — warstwy, architektura heksagonalna, podstawy DDD (⏳) | 🔶 |
| `t28_networking_http` | Sieć i HTTP (6 lekcji): gniazda TCP i UDP (serwer „echo” z własnym protokołem, wielu klientów, limity czasu), URI/URL i kodowanie parametrów, metody i kody HTTP, HttpClient (GET/POST, przekierowania, 404 to nie wyjątek, limity czasu), własny serwer HTTP z JDK (routing, długość w bajtach, REST na produktach, dziennik dostępu), JSON przez HTTP (201 + Location, 400/404/409/415, PUT kontra POST, wersjonowanie), zapytania asynchroniczne (sendAsync, allOf, obsługa błędów, ponawianie, Semaphore); wszystko lokalnie na localhost | ✅ |
| `t29_jdbc_databases` | Bazy danych (7 lekcji, baza H2 w pamięci): podstawy SQL (tabele, ograniczenia, SELECT/UPDATE/DELETE, NULL i logika trójwartościowa), JDBC (połączenie, ResultSet, metadane, SQLException i SQLState, DataSource), PreparedStatement (parametry zamiast sklejania SQL, typy, NULL, paczki, wygenerowane klucze, IN i LIKE, biała lista kolumn), transakcje (commit/rollback, punkty zapisu, ACID, poziomy izolacji pokazane na dwóch połączeniach, blokowanie optymistyczne), DAO/repozytorium (mapowanie, własny wyjątek, atrapa w pamięci, problem N+1, stronicowanie), złączenia i podzapytania (normalizacja, klucze obce), agregacja, funkcje okna i indeksy (EXPLAIN); porównanie z groupingBy | ✅ |
| `t30_build_modules` | Budowanie i uruchamianie (6 lekcji): Maven od środka (współrzędne, pom.xml, cykl życia, zakresy i zależności przechodnie, wrapper, BOM, Gradle), javac/jar/java -cp i manifest (narzędzia JDK uruchamiane z lekcji), zasoby w JAR-ach, ClassNotFoundException kontra NoClassDefFoundError, moduły JPMS (module-info, ServiceLoader, moduły automatyczne, jdeps), aplikacje konsolowe (argumenty, kody wyjścia, stdin/stdout/stderr), procesy i zmienne środowiskowe (ProcessBuilder, zakleszczenie na potoku, bezpieczeństwo poleceń), Javadoc i narzędzia jakości (doclint, -Xlint, Checkstyle, SpotBugs, PMD, JaCoCo, CI) | ✅ |
| `t31_jdk_toolbox` | Przydatne narzędzia JDK (4 lekcje): UUID (wersje 3 i 4, budowa, walidacja, UUID jako klucz w bazie), Base64 (odmiany, dopełnienie, to nie szyfrowanie), HexFormat i CRC32; skróty SHA-256 (pliki porcjami, weryfikacja pobrań, porównanie w stałym czasie), hasła z solą i PBKDF2 (zapis algorytm$iteracje$sól$skrót), SecureRandom i tokeny, HMAC; wielojęzyczność (Locale, liczby, waluty, daty, ResourceBundle, MessageFormat i apostrofy, polska odmiana liczebników, Collator); logowanie (System.Logger, konfiguracja java.util.logging, hierarchia, filtry, własny Formatter, MDC, logi strukturalne, Logback w Spring Boot) | ✅ |
| `t32_junit_mockito` | Testy w praktyce (5 lekcji; JUnit 5, AssertJ, Mockito jako zależności Maven): testy uruchamiane z `main` przez JUnit Platform Launcher (a w IntelliJ także zieloną strzałką), anatomia testu, asercje, cykl życia, @Nested, @Tag, założenia; testy parametryzowane (@ValueSource, @CsvSource, @MethodSource, @EnumSource, wartości brzegowe, @TestFactory); AssertJ (napisy, liczby, BigDecimal, kolekcje, wyjątki, miękkie asercje, porównanie rekurencyjne); Mockito (atrapy ręczne i z biblioteki, verify, dopasowywacze, ArgumentCaptor, spy, @Mock/@InjectMocks, kiedy fake lepszy niż mock); TDD krok po kroku (czerwony–zielony–refaktoryzacja, triangulacja, zapachy testów, pokrycie a jakość) | ✅ |
| `t33_interview_prep` | Rozmowa kwalifikacyjna (4 lekcje): ok. 25 kart pytań z Javy z odpowiedziami i uruchamianym dowodem (cache Integer, pula napisów, przekazywanie przez wartość, finally, generyki, lambdy, strumienie, pamięć i GC, pytania-pułapki), 22 karty o OOP i kolekcjach od kuchni (equals/hashCode, HashMap i drzewa w koszykach, zmienne klucze, fail-fast, złożoności), zadania programistyczne w wersji naiwnej i ulepszonej (FizzBuzz, palindrom, anagram, two-sum, nawiasy, testowanie różnicowe), live coding krok po kroku (V1→V4) z listą kontrolną | ✅ |
| `t34_toward_spring` | Most do Springa (4 lekcje, czysta Java): własny kontener IoC/DI (adnotacje, wstrzykiwanie przez konstruktor, @Value, zasięgi, cykl życia, @Primary/@Qualifier, wykrywanie cykli, mini-AOP z proxy i pułapka samowywołania), warstwy controller–service–repository (DTO, granica transakcji, walidacja, tłumaczenie wyjątków na kody HTTP, testy warstw), REST od środka (routing z adnotacji, zmienne ścieżki, JSON, obsługa błędów, prawdziwy serwer HTTP), co daje Spring Boot (startery, warstwy konfiguracji, profile, auto-konfiguracja warunkowa, /health, zapytania z nazw metod jak w Spring Data); każdy temat z odpowiednikiem w Springu; pełny kurs: [SpringLearning](https://github.com/arturoller/SpringLearning) | ✅ |
| `t35_capstone` | Mini-projekty łączące tematy (4): biblioteka, raport sprzedaży z CSV, stacje pogodowe (współbieżność), REST-owa lista zadań | ⏳ |

## Indeks haseł A–Z
Indeks jest uzupełniany wraz z kolejnymi lekcjami. Format: *hasło → plik (numer sekcji)*.

- **allMatch / anyMatch / noneMatch** → `t16_streams/Streams06TerminalOps` (6)
- **Arrays.stream** → `t16_streams/Streams02Creation` (3)
- **BigDecimal.compareTo** → `t16_streams/Streams03FilterMap` (1)
- **boxed** → `t16_streams/Streams02Creation` (4)
- **chars()** → `t16_streams/Streams02Creation` (7)
- **Files.lines** → `t16_streams/Streams02Creation` (9)
- **filter** → `t16_streams/Streams03FilterMap` (1)
- **findFirst** → `t16_streams/Streams01Intro` (7)
- **helpful NullPointerException** → `t16_streams/Streams03FilterMap` (8)
- **IllegalStateException (ponowne użycie streamu)** → `t16_streams/Streams01Intro` (8)
- **IntStream.range / rangeClosed** → `t16_streams/Streams02Creation` (4)
- **leniwość (lazy)** → `t16_streams/Streams01Intro` (5)
- **map** → `t16_streams/Streams03FilterMap` (3, 4)
- **mapToInt / mapToDouble / mapToObj** → `t16_streams/Streams03FilterMap` (5)
- **Objects::nonNull** → `t16_streams/Streams03FilterMap` (8)
- **peek** → `t16_streams/Streams03FilterMap` (7)
- **Predicate: and / or / negate / not** → `t16_streams/Streams03FilterMap` (2)
- **przetwarzanie pionowe (i wyjątek: sorted)** → `t16_streams/Streams01Intro` (6)
- **słowa kluczowe, literały, słowa kontekstowe** → `t00_start/Start02Glossary`
- **Stream.builder / concat** → `t16_streams/Streams02Creation` (8)
- **Stream.iterate / generate** → `t16_streams/Streams02Creation` (5, 6)
- **Stream.of / empty / ofNullable** → `t16_streams/Streams02Creation` (2)
- **toList() (niemodyfikowalna, Java 16+)** → `t16_streams/Streams01Intro` (1, 2)

## Uruchamianie i ustawienia IntelliJ
- **Uruchomienie lekcji:** kliknij zielony trójkąt ▶ obok `main`.
- **Błąd kompilacji w innym pliku:** IntelliJ przed uruchomieniem kompiluje cały projekt. Jeśli przy ćwiczeniach zostawisz błąd w jednej lekcji, zablokuje on uruchamianie pozostałych. Najlepiej go poprawić.
  - Awaryjnie: w konfiguracji uruchomienia (Run → Edit Configurations → Modify options → Before launch) zamień „Build” na „Build, no error check”.
- **Kodowanie polskich znaków:** pliki kursu są w UTF-8. Jeśli polskie litery wyświetlają się źle, ustaw kodowanie projektu na UTF-8: File → Settings → Editor → File Encodings → Project Encoding.
- **Zwinięte bloki z rozwiązaniami i odpowiedziami:** rozwiniesz je, klikając „+” na marginesie. Ctrl+Shift+NumPad+ rozwija wszystkie bloki w pliku.
- **Wymagania:** tylko Java 17. Lekcje nie korzystają z bibliotek zewnętrznych. Wyjątki (Maven pobierze je sam): Lombok w `t20_lombok`, baza H2 w `t29_jdbc_databases`, JUnit 5 / AssertJ / Mockito w `t32_junit_mockito`.

## Co dalej — Spring
Ten kurs to czysta Java SE. Następny krok to **[SpringLearning](https://github.com/arturoller/SpringLearning)** — kurs Springa i Spring Boota po polsku, w tym samym stylu: komentarze z tłumaczeniami, ćwiczenia, pytania kontrolne i ściągi.
- Zanim zaczniesz Springa, przerób przede wszystkim działy `t06`–`t16`, `t19_annotations_reflection`, `t22_design_patterns` i `t28`–`t34`.
- Dział `t34_toward_spring` jest mostem: pokazuje w czystej Javie to, co Spring robi „magicznie” (kontener DI, warstwy, REST).
