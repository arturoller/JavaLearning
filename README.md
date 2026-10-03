# JavaLearning — kurs Javy po polsku do nauki i powtórek

Samodzielny kurs Javy SE 17 w 36 działach (`t00`–`t35`). Każda lekcja to plik `.java`, który możesz uruchomić. Ma szczegółowe komentarze po polsku, a przy każdej angielskiej nazwie jest tłumaczenie. Każda lekcja zawiera też:
- ćwiczenia sprawdzane automatycznie,
- pytania kontrolne,
- ściągę do powtórek.

> **Spis treści z linkami do wszystkich lekcji:** [SPIS_TRESCI.md](SPIS_TRESCI.md).
>
> **Zacznij tutaj:** [`t00_start/Start01HowToUse.java`](src/t00_start/Start01HowToUse.java). Tam jest opis budowy lekcji, tagów, wyszukiwania i uruchamiania.
>
> **Co dalej:** po Javie kontynuuj kurs Springa w tym samym stylu — [SpringLearning](https://github.com/arturoller/SpringLearning).

## Szybki start
1. Zainstaluj **JDK 17** (albo nowszy).
2. Sklonuj repozytorium i otwórz folder projektu w IntelliJ IDEA (File → Open → wskaż `pom.xml` → Open as Project).
3. Otwórz dowolną lekcję, np. [`src/t01_basics/Basics01HelloJvm.java`](src/t01_basics/Basics01HelloJvm.java), i kliknij zielony trójkąt ▶ obok `main`.

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
**Wszystkie lekcje z linkami i krótkim opisem: [SPIS_TRESCI.md](SPIS_TRESCI.md).** Kliknij nazwę działu w tabeli, żeby przejść do listy jego lekcji.

Legenda: ✅ gotowe · 🔶 w trakcie · ⏳ zaplanowane

| Dział | Temat | Stan |
|---|---|---|
| [`helpers/`](src/helpers) | Wspólne narzędzia (`Console`, `Check`, `Sleep`, `TempDir`) i dane przykładowe (`SampleData`, `model/`) | ✅ |
| [`t00_start`](SPIS_TRESCI.md#t00_start) | Start (8 lekcji): jak korzystać z kursu, słowniczek, ścieżka nauki, dziennik powtórek, debugowanie w IntelliJ (breakpointy, krokowanie, warunki, wyjątki), Git od podstaw (poczekalnia, commit, gałęzie, scalanie i konflikty, rebase, zdalne repozytorium i pull request, cofanie zmian — mechanika pokazana na małym modelu Gita w Javie), refaktoryzacje i skróty IntelliJ (wersje PRZED/PO uruchamiane w lekcji, tabela skrótów), JShell (fragmenty kodu bez klasy i main, wykonywane w lekcji przez API jdk.jshell) | ✅ |
| [`t01_basics`](SPIS_TRESCI.md#t01_basics) | Podstawy (11 lekcji): jak działa program, typy proste, zmienne, operatory, rzutowanie i przepełnienie, klasy opakowujące, Math i liczby losowe, pułapki double, referencje i przekazywanie przez wartość, Scanner, wypisywanie i printf | ✅ |
| [`t02_controlflow`](SPIS_TRESCI.md#t02_controlflow) | Sterowanie (5 lekcji): if/else i klauzule strażnika, switch (klasyczny i wyrażenie), pętle, break/continue/etykiety, wzorce pętli | ✅ |
| [`t03_arrays`](SPIS_TRESCI.md#t03_arrays) | Tablice (5 lekcji): podstawy, tablice wielowymiarowe, klasa `Arrays`, algorytmy pisane ręcznie, varargs | ✅ |
| [`t04_strings`](SPIS_TRESCI.md#t04_strings) | Napisy (9 lekcji): niezmienność i pula, == kontra equals, metody String, StringBuilder/StringJoiner, formatowanie i bloki tekstu, wyrażenia regularne, char i Unicode (polskie litery, emoji), algorytmy na tekście, regex dla zaawansowanych (grupy nazwane, odwołania wsteczne, kwantyfikatory leniwe i zaborcze, lookahead/lookbehind, flagi, klasy Unicode, katastrofalne cofanie, walidatory), pełna ściąga Formattera (wszystkie konwersje i flagi, daty, BigDecimal kontra double, wyjątki formatu) | ✅ |
| [`t05_methods`](SPIS_TRESCI.md#t05_methods) | Metody (4 lekcje): budowa i stos wywołań, przeciążanie i wybór wersji, rekurencja (memoizacja, StackOverflowError), dobre praktyki | ✅ |
| [`t06_oop_basics`](SPIS_TRESCI.md#t06_oop_basics) | Obiektowość (10 lekcji): klasy i obiekty, konstruktory i kolejność inicjalizacji, hermetyzacja, static, `toString`/`equals`/`hashCode`, niezmienność i kopie obronne, klasy zagnieżdżone, pakiety i modyfikatory dostępu, obiekty wartości, kopiowanie obiektów (kopia płytka i głęboka, konstruktor kopiujący, tablice i kolekcje, clone() i dlaczego jest zepsute, rekordy z metodami „with”) | ✅ |
| [`t07_inheritance_polymorphism`](SPIS_TRESCI.md#t07_inheritance_polymorphism) | Dziedziczenie (8 lekcji): podstawy i łańcuch konstruktorów, nadpisywanie i jego pułapki (ukrywanie pól i metod statycznych, metoda wołana z konstruktora), klasy abstrakcyjne, interfejsy (default/static/private, konflikt diamentu), polimorfizm i rzutowanie, kompozycja kontra dziedziczenie (kruchość klasy bazowej, LSP), sealed, SOLID | ✅ |
| [`t08_enums`](SPIS_TRESCI.md#t08_enums) | Enumy (4 lekcje): podstawy (values, valueOf, ordinal, switch), pola/konstruktor/metody i wyszukiwanie po kodzie, zachowanie stałych (ciała, lambdy, interfejs, singleton), EnumSet/EnumMap i maszyna stanów | ✅ |
| [`t09_records`](SPIS_TRESCI.md#t09_records) | Rekordy (3 lekcje): co generuje record, płytka niezmienność, konstruktor kompaktowy (walidacja, normalizacja, kopie obronne), fabryki i „withery”, rekordy generyczne i lokalne, Comparable, klucze map, sealed + instanceof | ✅ |
| [`t10_exceptions`](SPIS_TRESCI.md#t10_exceptions) | Wyjątki (7 lekcji): try/catch/finally i stos wywołań, checked kontra unchecked i throws, kilka catch i multi-catch, try-with-resources i wyjątki stłumione, własne wyjątki z danymi, łańcuch przyczyn i opakowywanie, dobre praktyki i antywzorce | ✅ |
| [`t11_generics`](SPIS_TRESCI.md#t11_generics) | Typy generyczne (7 lekcji): po co generyki (surowe typy, remove(int)), własne klasy i interfejsy, metody generyczne i wnioskowanie, ograniczenia (extends, &), dżokery ? extends / ? super i PECS, wymazywanie typów i obejścia, generyczne repozytorium | ✅ |
| [`t12_collections`](SPIS_TRESCI.md#t12_collections) | Kolekcje (13 lekcji): przegląd i wybór kolekcji, listy (subList, Arrays.asList), iterowanie i modyfikacja (ConcurrentModificationException), zbiory, mapy (merge, computeIfAbsent, LRU), kolejki i ArrayDeque/PriorityQueue, Comparable/Comparator (polskie sortowanie), kolekcje niemodyfikowalne, klasa Collections, wzorce, jak działa HashMap, własny Iterable, wydajność kolekcji | ✅ |
| [`t13_lambdas`](SPIS_TRESCI.md#t13_lambdas) | Lambdy: od klasy anonimowej do lambdy, interfejsy funkcyjne, `java.util.function`, referencje do metod, składanie funkcji, domknięcia, funkcje wyższego rzędu, pułapki (8 lekcji) | ✅ |
| [`t14_optional`](SPIS_TRESCI.md#t14_optional) | Optional: podstawy, przekształcanie (map/flatMap/filter/or), dobre praktyki (3 lekcje) | ✅ |
| [`t15_numbers`](SPIS_TRESCI.md#t15_numbers) | Liczby (6 lekcji): BigDecimal, kwota jako obiekt wartości (VAT, raty), BigInteger, formatowanie i parsowanie liczb, sztuczki na liczbach całkowitych, ściąga klasy Math (abs i MIN_VALUE, potęgi i logarytmy, trygonometria w radianach, round/floor/ceil/rint dla liczb ujemnych, floorDiv/floorMod, arytmetyka Exact, ulp, NaN i nieskończoność, Math kontra StrictMath) | ✅ |
| [`t16_streams`](SPIS_TRESCI.md#t16_streams) | **Streamy**, 20 lekcji: wprowadzenie, tworzenie, filter/map, flatMap, sortowanie/distinct/limit, operacje końcowe, reduce, strumienie liczbowe, kolektory, toMap, groupingBy, partitioningBy, zaawansowane kolektory, Optional w streamach, pieniądze (BigDecimal), leniwość, efekty uboczne i pułapki, strumienie równoległe, 39 przepisów, 23 ćwiczenia | ✅ |
| [`t17_datetime`](SPIS_TRESCI.md#t17_datetime) | Data i czas, java.time (6 lekcji): LocalDate/LocalTime/LocalDateTime i Clock, Period/Duration/ChronoUnit, DateTimeFormatter (polskie nazwy, tryb STRICT, pułapki YYYY i mm), strefy i Instant (zmiana czasu), praktyka (dni robocze, YearMonth, kolizje spotkań, kalendarz), DateTimeFormatter dla zaawansowanych (gotowe formatery ISO, ofLocalized w pl-PL i en-US, DateTimeFormatterBuilder, LLLL kontra MMMM, kilka formatów wejściowych naraz, ResolverStyle i uuuu, ręczne formatowanie Duration/Period z polską odmianą) | ✅ |
| [`t18_io_files`](SPIS_TRESCI.md#t18_io_files) | Pliki (14 lekcji): Path i Files (tworzenie, kopiowanie, przenoszenie, usuwanie), odczyt tekstu (readString, lines, BufferedReader, Scanner), zapis (opcje otwarcia, flush/close, bezpieczny zapis przez plik tymczasowy), CSV z walidacją i cudzysłowami, JSON ręcznie (własny zapis i parser), Properties (UTF-8, warstwy konfiguracji), przechodzenie katalogów (walk, find, glob, walkFileTree), strumienie binarne (DataStream, własny format, HexFormat), serializacja (transient, serialVersionUID, rekordy, ObjectInputFilter), własny logger (poziomy, Clock, rotacja, java.util.logging), wyjątki IO (hierarchia, zasoby stłumione, TOCTOU, ponawianie), kodowanie znaków (UTF-8, windows-1250, BOM, emoji, NFC/NFD), archiwa ZIP i GZIP (zip slip, bomba kompresyjna), XML (DOM, StAX, XPath, ochrona przed XXE) | ✅ |
| [`t19_annotations_reflection`](SPIS_TRESCI.md#t19_annotations_reflection) | Adnotacje i refleksja (7 lekcji): adnotacje wbudowane i meta-adnotacje, własne adnotacje (retencja, @Repeatable, @Inherited), refleksja (Class, Method.invoke, setAccessible i moduły), mini-walidator, mini-framework komend (jak @GetMapping), dynamiczne proxy (podstawa @Transactional w Springu), procesory adnotacji z działającą kompilacją w lekcji | ✅ |
| [`t20_lombok`](SPIS_TRESCI.md#t20_lombok) | Lombok (4 lekcje): gettery/settery, @ToString, @EqualsAndHashCode (callSuper), konstruktory i @NonNull, @Data/@Value/@Builder/@With i porównanie z rekordami, @Cleanup/@SneakyThrows/@Log/lombok.config i ich pułapki; przy każdej adnotacji kod, który Lombok generuje | ✅ |
| [`t21_concurrency`](SPIS_TRESCI.md#t21_concurrency) | Współbieżność (10 lekcji): wątki (start/run, join, stany, demony, przerwania), wyścigi (count++, sprawdź-potem-działaj, synchronized, Atomic*, LongAdder), blokady (ReentrantLock, Condition, ReadWriteLock, StampedLock, wait/notify, zakleszczenie i jego wykrywanie), pule wątków (ExecutorService, Future, invokeAll, ThreadPoolExecutor i polityki odrzucania), CompletableFuture (thenCompose/thenCombine, allOf, obsługa błędów, limity czasu), kolekcje współbieżne (ConcurrentHashMap, CopyOnWriteArrayList, BlockingQueue, producent–konsument), synchronizatory (CountDownLatch, CyclicBarrier, Semaphore, Phaser), wzorce bezpieczeństwa wątkowego (niezmienność, ThreadLocal, bezstanowe serwisy jak w Springu), zadania cykliczne i ForkJoin, model pamięci Javy (volatile, happens-before, double-checked locking) | ✅ |
| [`t22_design_patterns`](SPIS_TRESCI.md#t22_design_patterns) | Wzorce projektowe (15 lekcji): strategia, budowniczy, fabryka (metody statyczne, metoda wytwórcza, fabryka abstrakcyjna), singleton (holder, enum, double-checked locking), metoda szablonowa, obserwator, dekorator, wstrzykiwanie zależności (z mini-kontenerem), polecenie (undo/redo), fasada, adapter, kompozyt, stan, łańcuch odpowiedzialności, wizytator; w każdej lekcji: problem bez wzorca, wzorzec krok po kroku, wersja z lambdami/rekordami/sealed, przykłady z JDK i Springa, pułapki nadużywania i testowanie; na końcu tabela wszystkich 15 wzorców | ✅ |
| [`t23_modern_java`](SPIS_TRESCI.md#t23_modern_java) | Nowości Java 8→17 i zapowiedź 21+ (7 lekcji): Java 8 (lambdy, metody default i reguły diamentu, Stream, Optional, java.time, nowe metody map), var (gdzie wolno, pułapki z rombem i literałami), wyrażenia switch (strzałki, yield, wyczerpywalność enumów), bloki tekstowe (wcięcia, \s, końce linii, formatted), rekordy + sealed + instanceof ze wzorcem (typy algebraiczne), drobne nowości API z wydań 9–17 (tabela), co przynoszą Java 18–25 (UTF-8 domyślnie, wzorce w switch, wątki wirtualne, kolekcje sekwencyjne, `_`, gatherers) i strategia aktualizacji | ✅ |
| [`t24_algorithms`](SPIS_TRESCI.md#t24_algorithms) | Algorytmy i matematyka (18 lekcji). Algorytmy: złożoność, sortowanie, wyszukiwanie, własne struktury danych, klasyki, rekurencja z nawrotami (backtracking), programowanie dynamiczne, grafy (BFS/DFS). Matematyka: teoria liczb (NWD, liczby pierwsze, sito Eratostenesa), arytmetyka modularna i sumy kontrolne (PESEL, NIP, Luhn, IBAN), kombinatoryka, systemy liczbowe i liczby rzymskie, metody numeryczne (Newton, bisekcja, całkowanie, Monte Carlo), statystyka, macierze i geometria, wielkie liczby, klasyki z rozmów kwalifikacyjnych (silnia, Fibonacci, liczby pierwsze, palindromy, sztuczki bitowe), zadania w stylu Project Euler | ✅ |
| [`t25_testing`](SPIS_TRESCI.md#t25_testing) | Testowanie — pojęcia bez frameworka (3 lekcje): po co testy, piramida testów, AAA i FIRST, własny mini-runner testów, dublery (dummy/stub/fake/spy/mock), kod łatwy do testowania (wstrzykiwanie zależności, Clock) | ✅ |
| [`t26_jvm`](SPIS_TRESCI.md#t26_jvm) | JVM (4 lekcje): pamięć (stos, sterta, pula napisów, wycieki), ładowanie i inicjalizacja klas (kolejność, stałe wklejane w kod, holder idiom, class loadery), odśmiecanie (osiągalność, generacje, G1, referencje słabe i miękkie, Cleaner), narzędzia diagnostyczne (jcmd, jstack, JFR, VisualVM, wykrywanie zakleszczeń, JIT) | ✅ |
| [`t27_clean_code_pitfalls`](SPIS_TRESCI.md#t27_clean_code_pitfalls) | Pułapki i czysty kod (5 lekcji): 22 klasyczne pułapki Javy, typowe uwagi z code review (PRZED/PO), zasady czystego kodu (nazwy, DRY/KISS/YAGNI, CQS, fail fast), SOLID na jednym realistycznym przykładzie (moduł faktur), architektura aplikacji (warstwy i reguła zależności, pakiety według funkcji, architektura heksagonalna z portami i adapterami, podstawy DDD: encje, obiekty wartości, agregaty, zdarzenia domenowe, konteksty ograniczone; kiedy to przesada i jak to wygląda w Springu) | ✅ |
| [`t28_networking_http`](SPIS_TRESCI.md#t28_networking_http) | Sieć i HTTP (6 lekcji): gniazda TCP i UDP (serwer „echo” z własnym protokołem, wielu klientów, limity czasu), URI/URL i kodowanie parametrów, metody i kody HTTP, HttpClient (GET/POST, przekierowania, 404 to nie wyjątek, limity czasu), własny serwer HTTP z JDK (routing, długość w bajtach, REST na produktach, dziennik dostępu), JSON przez HTTP (201 + Location, 400/404/409/415, PUT kontra POST, wersjonowanie), zapytania asynchroniczne (sendAsync, allOf, obsługa błędów, ponawianie, Semaphore); wszystko lokalnie na localhost | ✅ |
| [`t29_jdbc_databases`](SPIS_TRESCI.md#t29_jdbc_databases) | Bazy danych (7 lekcji, baza H2 w pamięci): podstawy SQL (tabele, ograniczenia, SELECT/UPDATE/DELETE, NULL i logika trójwartościowa), JDBC (połączenie, ResultSet, metadane, SQLException i SQLState, DataSource), PreparedStatement (parametry zamiast sklejania SQL, typy, NULL, paczki, wygenerowane klucze, IN i LIKE, biała lista kolumn), transakcje (commit/rollback, punkty zapisu, ACID, poziomy izolacji pokazane na dwóch połączeniach, blokowanie optymistyczne), DAO/repozytorium (mapowanie, własny wyjątek, atrapa w pamięci, problem N+1, stronicowanie), złączenia i podzapytania (normalizacja, klucze obce), agregacja, funkcje okna i indeksy (EXPLAIN); porównanie z groupingBy | ✅ |
| [`t30_build_modules`](SPIS_TRESCI.md#t30_build_modules) | Budowanie i uruchamianie (6 lekcji): Maven od środka (współrzędne, pom.xml, cykl życia, zakresy i zależności przechodnie, wrapper, BOM, Gradle), javac/jar/java -cp i manifest (narzędzia JDK uruchamiane z lekcji), zasoby w JAR-ach, ClassNotFoundException kontra NoClassDefFoundError, moduły JPMS (module-info, ServiceLoader, moduły automatyczne, jdeps), aplikacje konsolowe (argumenty, kody wyjścia, stdin/stdout/stderr), procesy i zmienne środowiskowe (ProcessBuilder, zakleszczenie na potoku, bezpieczeństwo poleceń), Javadoc i narzędzia jakości (doclint, -Xlint, Checkstyle, SpotBugs, PMD, JaCoCo, CI) | ✅ |
| [`t31_jdk_toolbox`](SPIS_TRESCI.md#t31_jdk_toolbox) | Przydatne narzędzia JDK (4 lekcje): UUID (wersje 3 i 4, budowa, walidacja, UUID jako klucz w bazie), Base64 (odmiany, dopełnienie, to nie szyfrowanie), HexFormat i CRC32; skróty SHA-256 (pliki porcjami, weryfikacja pobrań, porównanie w stałym czasie), hasła z solą i PBKDF2 (zapis algorytm$iteracje$sól$skrót), SecureRandom i tokeny, HMAC; wielojęzyczność (Locale, liczby, waluty, daty, ResourceBundle, MessageFormat i apostrofy, polska odmiana liczebników, Collator); logowanie (System.Logger, konfiguracja java.util.logging, hierarchia, filtry, własny Formatter, MDC, logi strukturalne, Logback w Spring Boot) | ✅ |
| [`t32_junit_mockito`](SPIS_TRESCI.md#t32_junit_mockito) | Testy w praktyce (5 lekcji; JUnit 5, AssertJ, Mockito jako zależności Maven): testy uruchamiane z `main` przez JUnit Platform Launcher (a w IntelliJ także zieloną strzałką), anatomia testu, asercje, cykl życia, @Nested, @Tag, założenia; testy parametryzowane (@ValueSource, @CsvSource, @MethodSource, @EnumSource, wartości brzegowe, @TestFactory); AssertJ (napisy, liczby, BigDecimal, kolekcje, wyjątki, miękkie asercje, porównanie rekurencyjne); Mockito (atrapy ręczne i z biblioteki, verify, dopasowywacze, ArgumentCaptor, spy, @Mock/@InjectMocks, kiedy fake lepszy niż mock); TDD krok po kroku (czerwony–zielony–refaktoryzacja, triangulacja, zapachy testów, pokrycie a jakość) | ✅ |
| [`t33_interview_prep`](SPIS_TRESCI.md#t33_interview_prep) | Rozmowa kwalifikacyjna (4 lekcje): ok. 25 kart pytań z Javy z odpowiedziami i uruchamianym dowodem (cache Integer, pula napisów, przekazywanie przez wartość, finally, generyki, lambdy, strumienie, pamięć i GC, pytania-pułapki), 22 karty o OOP i kolekcjach od kuchni (equals/hashCode, HashMap i drzewa w koszykach, zmienne klucze, fail-fast, złożoności), zadania programistyczne w wersji naiwnej i ulepszonej (FizzBuzz, palindrom, anagram, two-sum, nawiasy, testowanie różnicowe), live coding krok po kroku (V1→V4) z listą kontrolną | ✅ |
| [`t34_toward_spring`](SPIS_TRESCI.md#t34_toward_spring) | Most do Springa (4 lekcje, czysta Java): własny kontener IoC/DI (adnotacje, wstrzykiwanie przez konstruktor, @Value, zasięgi, cykl życia, @Primary/@Qualifier, wykrywanie cykli, mini-AOP z proxy i pułapka samowywołania), warstwy controller–service–repository (DTO, granica transakcji, walidacja, tłumaczenie wyjątków na kody HTTP, testy warstw), REST od środka (routing z adnotacji, zmienne ścieżki, JSON, obsługa błędów, prawdziwy serwer HTTP), co daje Spring Boot (startery, warstwy konfiguracji, profile, auto-konfiguracja warunkowa, /health, zapytania z nazw metod jak w Spring Data); każdy temat z odpowiednikiem w Springu; pełny kurs: [SpringLearning](https://github.com/arturoller/SpringLearning) | ✅ |
| [`t35_capstone`](SPIS_TRESCI.md#t35_capstone) | Mini-projekty łączące cały kurs (4 lekcje): biblioteka (wypożyczenia, terminy i opłaty z Clock i BigDecimal, kolejka rezerwacji, powłoka poleceń, zapis i odczyt stanu z pliku), raport sprzedaży z CSV (walidacja z numerami linii, agregacje EnumMap/TreeMap, kontrola krzyżowa sum, raport pl-PL i JSON), stacje pogodowe (producent–konsument, pula wątków, statystyki bezpieczne wątkowo, alarmy przez obserwatora, strumień równoległy i CompletableFuture), REST-owa lista zadań (model z tabelą przejść, serwis z regułami, routing i kody HTTP, prawdziwy serwer i klient, przełożenie na Spring Boot) | ✅ |

## Indeks haseł A–Z
Format: *hasło → plik (numer sekcji)*; przy kilku miejscach pierwsze jest główne. Brak numeru = temat całej lekcji. Czego tu nie ma, znajdziesz w IntelliJ: Ctrl+Shift+F (szukanie w całym projekcie).

Litery: [A](#a) · [B](#b) · [C](#c) · [D](#d) · [E](#e) · [F](#f) · [G](#g) · [H](#h) · [I](#i) · [J](#j) · [K](#k) · [L](#l) · [Ł](#ł) · [M](#m) · [N](#n) · [O](#o) · [P](#p) · [Q](#q) · [R](#r) · [S](#s) · [Ś](#ś) · [T](#t) · [U](#u) · [V](#v) · [W](#w) · [X](#x) · [Y](#y) · [Z](#z) · [Ź](#ź) · [Ż](#ż)

### A

- **ACID** → [`t29_jdbc_databases/Jdbc04Transactions`](src/t29_jdbc_databases/Jdbc04Transactions.java) (5)
- **/actuator/health** → [`t34_toward_spring/Spring04WhatSpringGives`](src/t34_toward_spring/Spring04WhatSpringGives.java) (7)
- **Adapter (wzorzec)** → [`t22_design_patterns/Patterns11Adapter`](src/t22_design_patterns/Patterns11Adapter.java)
- **adnotacja** → [`t19_annotations_reflection/Annotations01BuiltIn`](src/t19_annotations_reflection/Annotations01BuiltIn.java) (1)
- **agregat (DDD)** → [`t27_clean_code_pitfalls/CleanCode03Architecture`](src/t27_clean_code_pitfalls/CleanCode03Architecture.java) (5)
- **algorytm Dijkstry** → [`t24_algorithms/Algorithms08Graphs`](src/t24_algorithms/Algorithms08Graphs.java) (6)
- **allMatch / anyMatch / noneMatch** → [`t16_streams/Streams06TerminalOps`](src/t16_streams/Streams06TerminalOps.java) (6)
- **allOf / anyOf** → [`t21_concurrency/Concurrency05CompletableFuture`](src/t21_concurrency/Concurrency05CompletableFuture.java) (5); [`t28_networking_http/Http05AsyncTimeouts`](src/t28_networking_http/Http05AsyncTimeouts.java) (2)
- **anagram** → [`t04_strings/Strings07TextAlgorithms`](src/t04_strings/Strings07TextAlgorithms.java) (2); [`t33_interview_prep/Interview03CodingTasks`](src/t33_interview_prep/Interview03CodingTasks.java) (4)
- **analiza statyczna kodu** → [`t30_build_modules/Build06QualityToolsJavadoc`](src/t30_build_modules/Build06QualityToolsJavadoc.java) (5)
- **API JSON (klient i serwer)** → [`t28_networking_http/Http04JsonApi`](src/t28_networking_http/Http04JsonApi.java)
- **aplikacja konsolowa (main, args)** → [`t30_build_modules/Build04CommandLineApps`](src/t30_build_modules/Build04CommandLineApps.java) (1, 2)
- **architektura warstwowa** → [`t27_clean_code_pitfalls/CleanCode03Architecture`](src/t27_clean_code_pitfalls/CleanCode03Architecture.java) (2, 3); [`t34_toward_spring/Spring02Layers`](src/t34_toward_spring/Spring02Layers.java) (1)
- **Arrange-Act-Assert (AAA)** → [`t25_testing/Testing01Concepts`](src/t25_testing/Testing01Concepts.java) (3)
- **ArrayDeque** → [`t12_collections/Collections06QueuesDeques`](src/t12_collections/Collections06QueuesDeques.java) (2, 3)
- **ArrayList** → [`t12_collections/Collections02Lists`](src/t12_collections/Collections02Lists.java) (1, 2)
- **ArrayList kontra LinkedList** → [`t12_collections/Collections02Lists`](src/t12_collections/Collections02Lists.java) (9); [`t12_collections/Collections13Performance`](src/t12_collections/Collections13Performance.java) (5)
- **Arrays.asList** → [`t03_arrays/Arrays03Utility`](src/t03_arrays/Arrays03Utility.java) (7); [`t12_collections/Collections02Lists`](src/t12_collections/Collections02Lists.java) (8); [`t12_collections/Collections08ImmutableUnmodifiable`](src/t12_collections/Collections08ImmutableUnmodifiable.java) (4)
- **Arrays.binarySearch** → [`t03_arrays/Arrays03Utility`](src/t03_arrays/Arrays03Utility.java) (3); [`t24_algorithms/Algorithms03Searching`](src/t24_algorithms/Algorithms03Searching.java) (8)
- **Arrays.deepToString** → [`t03_arrays/Arrays02MultiDim`](src/t03_arrays/Arrays02MultiDim.java) (1)
- **Arrays.equals / deepEquals** → [`t03_arrays/Arrays03Utility`](src/t03_arrays/Arrays03Utility.java) (6)
- **Arrays.sort** → [`t03_arrays/Arrays03Utility`](src/t03_arrays/Arrays03Utility.java) (1, 2)
- **Arrays.stream** → [`t16_streams/Streams02Creation`](src/t16_streams/Streams02Creation.java) (3)
- **assert (domyślnie wyłączony)** → [`t25_testing/Testing01Concepts`](src/t25_testing/Testing01Concepts.java) (8)
- **AssertJ (assertThat)** → [`t32_junit_mockito/JUnit03AssertJ`](src/t32_junit_mockito/JUnit03AssertJ.java)
- **AssertJ: usingRecursiveComparison** → [`t32_junit_mockito/JUnit03AssertJ`](src/t32_junit_mockito/JUnit03AssertJ.java) (8)
- **AtomicInteger / AtomicLong** → [`t21_concurrency/Concurrency02RaceConditions`](src/t21_concurrency/Concurrency02RaceConditions.java) (5)
- **auto-konfiguracja warunkowa** → [`t34_toward_spring/Spring04WhatSpringGives`](src/t34_toward_spring/Spring04WhatSpringGives.java) (6)
- **autoboxing i unboxing** → [`t01_basics/Basics06Wrappers`](src/t01_basics/Basics06Wrappers.java) (2)

### B

- **backoff (wykładnicze ponawianie)** → [`t28_networking_http/Http05AsyncTimeouts`](src/t28_networking_http/Http05AsyncTimeouts.java) (7)
- **Base64** → [`t31_jdk_toolbox/Toolbox01UuidBase64`](src/t31_jdk_toolbox/Toolbox01UuidBase64.java) (6, 7, 9); [`t23_modern_java/Modern01Java8`](src/t23_modern_java/Modern01Java8.java) (8)
- **bezpieczeństwo wątkowe (thread safety)** → [`t21_concurrency/Concurrency08ThreadSafetyPatterns`](src/t21_concurrency/Concurrency08ThreadSafetyPatterns.java)
- **BFS (przeszukiwanie wszerz)** → [`t24_algorithms/Algorithms08Graphs`](src/t24_algorithms/Algorithms08Graphs.java) (2)
- **BigDecimal** → [`t15_numbers/Numbers01BigDecimal`](src/t15_numbers/Numbers01BigDecimal.java)
- **BigDecimal: dzielenie, MathContext** → [`t15_numbers/Numbers01BigDecimal`](src/t15_numbers/Numbers01BigDecimal.java) (4); [`t24_algorithms/Math08BigNumbers`](src/t24_algorithms/Math08BigNumbers.java) (6, 7)
- **BigDecimal: equals kontra compareTo** → [`t15_numbers/Numbers01BigDecimal`](src/t15_numbers/Numbers01BigDecimal.java) (7); [`t16_streams/Streams15BigDecimalMoney`](src/t16_streams/Streams15BigDecimalMoney.java) (8)
- **BigDecimal.compareTo** → [`t15_numbers/Numbers01BigDecimal`](src/t15_numbers/Numbers01BigDecimal.java) (7); [`t16_streams/Streams15BigDecimalMoney`](src/t16_streams/Streams15BigDecimalMoney.java) (8)
- **BigDecimal w strumieniach** → [`t16_streams/Streams15BigDecimalMoney`](src/t16_streams/Streams15BigDecimalMoney.java)
- **BigInteger** → [`t15_numbers/Numbers03BigInteger`](src/t15_numbers/Numbers03BigInteger.java); [`t24_algorithms/Math08BigNumbers`](src/t24_algorithms/Math08BigNumbers.java) (2)
- **BlockingQueue** → [`t21_concurrency/Concurrency06ConcurrentCollections`](src/t21_concurrency/Concurrency06ConcurrentCollections.java) (7)
- **blok tekstu (text block)** → [`t23_modern_java/Modern04TextBlocks`](src/t23_modern_java/Modern04TextBlocks.java); [`t01_basics/Basics11ConsoleOutput`](src/t01_basics/Basics11ConsoleOutput.java) (6)
- **blokowanie optymistyczne** → [`t29_jdbc_databases/Jdbc04Transactions`](src/t29_jdbc_databases/Jdbc04Transactions.java) (7)
- **blokowanie pesymistyczne** → [`t29_jdbc_databases/Jdbc04Transactions`](src/t29_jdbc_databases/Jdbc04Transactions.java) (8)
- **błąd o jeden (off-by-one)** → [`t02_controlflow/Control03Loops`](src/t02_controlflow/Control03Loops.java) (6); [`t00_start/Start05Debugging`](src/t00_start/Start05Debugging.java)
- **BodyHandlers** → [`t28_networking_http/Http02HttpClient`](src/t28_networking_http/Http02HttpClient.java) (4)
- **boxed** → [`t16_streams/Streams02Creation`](src/t16_streams/Streams02Creation.java) (4)
- **break** → [`t02_controlflow/Control04BreakContinueLabels`](src/t02_controlflow/Control04BreakContinueLabels.java) (1)
- **breakpoint (punkt zatrzymania)** → [`t00_start/Start05Debugging`](src/t00_start/Start05Debugging.java) (1)
- **breakpoint warunkowy** → [`t00_start/Start05Debugging`](src/t00_start/Start05Debugging.java) (3)
- **BufferedReader.readLine** → [`t18_io_files/Io02ReadingText`](src/t18_io_files/Io02ReadingText.java) (4)
- **Builder (wzorzec)** → [`t22_design_patterns/Patterns02Builder`](src/t22_design_patterns/Patterns02Builder.java)

### C

- **catch: kolejność od szczegółu do ogółu** → [`t10_exceptions/Exceptions01Basics`](src/t10_exceptions/Exceptions01Basics.java) (7); [`t10_exceptions/Exceptions03MultiCatch`](src/t10_exceptions/Exceptions03MultiCatch.java) (1)
- **Caused by / getCause (przyczyna)** → [`t10_exceptions/Exceptions06ChainingWrapping`](src/t10_exceptions/Exceptions06ChainingWrapping.java) (1, 2, 3)
- **Chain of Responsibility** → [`t22_design_patterns/Patterns14ChainOfResponsibility`](src/t22_design_patterns/Patterns14ChainOfResponsibility.java)
- **char to liczba** → [`t01_basics/Basics02PrimitiveTypes`](src/t01_basics/Basics02PrimitiveTypes.java) (4); [`t04_strings/Strings06CharUnicode`](src/t04_strings/Strings06CharUnicode.java) (1)
- **chars()** → [`t16_streams/Streams02Creation`](src/t16_streams/Streams02Creation.java) (7)
- **checked i unchecked (wyjątki sprawdzane)** → [`t10_exceptions/Exceptions02CheckedUnchecked`](src/t10_exceptions/Exceptions02CheckedUnchecked.java)
- **ciało stałej enuma** → [`t08_enums/Enums03ConstantBodies`](src/t08_enums/Enums03ConstantBodies.java) (1, 2)
- **class loader** → [`t26_jvm/Jvm02ClassLoadingInit`](src/t26_jvm/Jvm02ClassLoadingInit.java) (8)
- **Class<T>** → [`t11_generics/Generics06ErasureLimits`](src/t11_generics/Generics06ErasureLimits.java) (7)
- **ClassCastException** → [`t07_inheritance_polymorphism/Inherit05Polymorphism`](src/t07_inheritance_polymorphism/Inherit05Polymorphism.java) (5)
- **ClassNotFoundException kontra NoClassDefFoundError** → [`t30_build_modules/Build02JarClasspath`](src/t30_build_modules/Build02JarClasspath.java) (7)
- **Clock (testowalne teraz)** → [`t17_datetime/DateTime01LocalDateTime`](src/t17_datetime/DateTime01LocalDateTime.java) (8); [`t25_testing/Testing02TestDoubles`](src/t25_testing/Testing02TestDoubles.java) (8)
- **Cloneable / clone** → [`t06_oop_basics/Oop10Copying`](src/t06_oop_basics/Oop10Copying.java) (7, 8)
- **code review: typowe uwagi** → [`t27_clean_code_pitfalls/Pitfalls02CodeReview`](src/t27_clean_code_pitfalls/Pitfalls02CodeReview.java)
- **Collator (sortowanie po polsku)** → [`t31_jdk_toolbox/Toolbox03I18n`](src/t31_jdk_toolbox/Toolbox03I18n.java) (7); [`t12_collections/Collections07ComparableComparator`](src/t12_collections/Collections07ComparableComparator.java) (7); [`t16_streams/Streams05SortDistinctLimit`](src/t16_streams/Streams05SortDistinctLimit.java) (1)
- **collectingAndThen** → [`t16_streams/Streams13AdvancedCollectors`](src/t16_streams/Streams13AdvancedCollectors.java) (2)
- **Collection i Map — jak wybrać** → [`t12_collections/Collections01Overview`](src/t12_collections/Collections01Overview.java) (2, 7)
- **Collections.binarySearch** → [`t12_collections/Collections09CollectionsUtility`](src/t12_collections/Collections09CollectionsUtility.java) (6)
- **Collections.synchronizedList** → [`t21_concurrency/Concurrency06ConcurrentCollections`](src/t21_concurrency/Concurrency06ConcurrentCollections.java) (1)
- **Collections.unmodifiableList** → [`t12_collections/Collections08ImmutableUnmodifiable`](src/t12_collections/Collections08ImmutableUnmodifiable.java) (2)
- **Collectors.counting / summingInt / averagingInt** → [`t16_streams/Streams09CollectorsBasic`](src/t16_streams/Streams09CollectorsBasic.java) (5, 6)
- **Collectors.joining** → [`t16_streams/Streams09CollectorsBasic`](src/t16_streams/Streams09CollectorsBasic.java) (4)
- **Collectors.teeing** → [`t16_streams/Streams13AdvancedCollectors`](src/t16_streams/Streams13AdvancedCollectors.java) (5); [`t23_modern_java/Modern06ApiAdditions`](src/t23_modern_java/Modern06ApiAdditions.java) (7)
- **Collectors.toMap** → [`t16_streams/Streams10CollectorsToMap`](src/t16_streams/Streams10CollectorsToMap.java)
- **Command (wzorzec)** → [`t22_design_patterns/Patterns09Command`](src/t22_design_patterns/Patterns09Command.java)
- **Command-Query Separation** → [`t27_clean_code_pitfalls/CleanCode01Principles`](src/t27_clean_code_pitfalls/CleanCode01Principles.java) (6)
- **Comparable** → [`t07_inheritance_polymorphism/Inherit04Interfaces`](src/t07_inheritance_polymorphism/Inherit04Interfaces.java) (5); [`t12_collections/Collections07ComparableComparator`](src/t12_collections/Collections07ComparableComparator.java) (1)
- **Comparator** → [`t12_collections/Collections07ComparableComparator`](src/t12_collections/Collections07ComparableComparator.java) (3)
- **Comparator.comparing / thenComparing / reversed** → [`t12_collections/Collections07ComparableComparator`](src/t12_collections/Collections07ComparableComparator.java) (3, 4, 5); [`t13_lambdas/Lambda05Composition`](src/t13_lambdas/Lambda05Composition.java) (6)
- **compareTo (String)** → [`t04_strings/Strings01Basics`](src/t04_strings/Strings01Basics.java) (6)
- **compareTo zgodne z equals** → [`t12_collections/Collections07ComparableComparator`](src/t12_collections/Collections07ComparableComparator.java) (2)
- **CompletableFuture** → [`t21_concurrency/Concurrency05CompletableFuture`](src/t21_concurrency/Concurrency05CompletableFuture.java)
- **Composite (wzorzec)** → [`t22_design_patterns/Patterns12Composite`](src/t22_design_patterns/Patterns12Composite.java)
- **computeIfAbsent** → [`t12_collections/Collections05Maps`](src/t12_collections/Collections05Maps.java) (4); [`t12_collections/Collections10Patterns`](src/t12_collections/Collections10Patterns.java) (2)
- **ConcurrentHashMap** → [`t21_concurrency/Concurrency06ConcurrentCollections`](src/t21_concurrency/Concurrency06ConcurrentCollections.java) (2, 3, 4)
- **ConcurrentModificationException** → [`t12_collections/Collections03IterationModification`](src/t12_collections/Collections03IterationModification.java) (3); [`t16_streams/Streams17SideEffectsPitfalls`](src/t16_streams/Streams17SideEffectsPitfalls.java) (2)
- **Condition (await / signal)** → [`t21_concurrency/Concurrency03Locks`](src/t21_concurrency/Concurrency03Locks.java) (5)
- **contains / indexOf / startsWith** → [`t04_strings/Strings02Methods`](src/t04_strings/Strings02Methods.java) (3)
- **continue** → [`t02_controlflow/Control04BreakContinueLabels`](src/t02_controlflow/Control04BreakContinueLabels.java) (2, 8)
- **CountDownLatch** → [`t21_concurrency/Concurrency07Synchronizers`](src/t21_concurrency/Concurrency07Synchronizers.java) (1, 2)
- **Created (201) i nagłówek Location** → [`t28_networking_http/Http04JsonApi`](src/t28_networking_http/Http04JsonApi.java) (5)
- **CSV** → [`t18_io_files/Io04Csv`](src/t18_io_files/Io04Csv.java)
- **CyclicBarrier** → [`t21_concurrency/Concurrency07Synchronizers`](src/t21_concurrency/Concurrency07Synchronizers.java) (3, 4)
- **cztery filary OOP** → [`t33_interview_prep/Interview02OopCollections`](src/t33_interview_prep/Interview02OopCollections.java) (1)
- **czysta funkcja** → [`t05_methods/Methods04GoodPractices`](src/t05_methods/Methods04GoodPractices.java) (5); [`t25_testing/Testing03TestableDesign`](src/t25_testing/Testing03TestableDesign.java) (3)
- **czysty kod (nazewnictwo, małe funkcje)** → [`t27_clean_code_pitfalls/CleanCode01Principles`](src/t27_clean_code_pitfalls/CleanCode01Principles.java) (1, 2)

### D

- **DAO / repozytorium** → [`t29_jdbc_databases/Jdbc05Dao`](src/t29_jdbc_databases/Jdbc05Dao.java) (1)
- **DateTimeFormatter** → [`t17_datetime/DateTime03Formatting`](src/t17_datetime/DateTime03Formatting.java); [`t17_datetime/DateTime06FormatterAdvanced`](src/t17_datetime/DateTime06FormatterAdvanced.java)
- **DateTimeFormatter: bezpieczeństwo wątkowe** → [`t17_datetime/DateTime06FormatterAdvanced`](src/t17_datetime/DateTime06FormatterAdvanced.java) (10); [`t21_concurrency/Concurrency08ThreadSafetyPatterns`](src/t21_concurrency/Concurrency08ThreadSafetyPatterns.java) (8)
- **DateTimeFormatter: SMART / STRICT / LENIENT** → [`t17_datetime/DateTime03Formatting`](src/t17_datetime/DateTime03Formatting.java) (5); [`t17_datetime/DateTime06FormatterAdvanced`](src/t17_datetime/DateTime06FormatterAdvanced.java) (7)
- **DDD (domain-driven design)** → [`t27_clean_code_pitfalls/CleanCode03Architecture`](src/t27_clean_code_pitfalls/CleanCode03Architecture.java) (4)
- **debugowanie (debugger)** → [`t00_start/Start05Debugging`](src/t00_start/Start05Debugging.java)
- **Decorator (wzorzec)** → [`t22_design_patterns/Patterns07Decorator`](src/t22_design_patterns/Patterns07Decorator.java)
- **Dependency Injection (wstrzykiwanie zależności)** → [`t22_design_patterns/Patterns08DependencyInjection`](src/t22_design_patterns/Patterns08DependencyInjection.java)
- **Deque** → [`t12_collections/Collections06QueuesDeques`](src/t12_collections/Collections06QueuesDeques.java)
- **DFS (przeszukiwanie w głąb)** → [`t24_algorithms/Algorithms08Graphs`](src/t24_algorithms/Algorithms08Graphs.java) (3, 4)
- **diament <>** → [`t11_generics/Generics01Why`](src/t11_generics/Generics01Why.java) (4)
- **do-while** → [`t02_controlflow/Control03Loops`](src/t02_controlflow/Control03Loops.java) (4)
- **domknięcie (closure)** → [`t13_lambdas/Lambda06ClosuresScope`](src/t13_lambdas/Lambda06ClosuresScope.java)
- **double-checked locking** → [`t21_concurrency/Concurrency10MemoryModel`](src/t21_concurrency/Concurrency10MemoryModel.java) (6); [`t22_design_patterns/Patterns04Singleton`](src/t22_design_patterns/Patterns04Singleton.java) (5)
- **DRY** → [`t05_methods/Methods04GoodPractices`](src/t05_methods/Methods04GoodPractices.java) (6); [`t27_clean_code_pitfalls/CleanCode01Principles`](src/t27_clean_code_pitfalls/CleanCode01Principles.java) (3)
- **drzewo poszukiwań binarnych (BST)** → [`t24_algorithms/Algorithms04DataStructures`](src/t24_algorithms/Algorithms04DataStructures.java) (5)
- **dublery testowe (dummy, stub, fake, spy, mock)** → [`t25_testing/Testing02TestDoubles`](src/t25_testing/Testing02TestDoubles.java)
- **Duration** → [`t17_datetime/DateTime02PeriodDuration`](src/t17_datetime/DateTime02PeriodDuration.java) (3, 6)
- **dwa wskaźniki (two pointers)** → [`t24_algorithms/Algorithms05Classics`](src/t24_algorithms/Algorithms05Classics.java) (1, 2)
- **dynamic dispatch** → [`t07_inheritance_polymorphism/Inherit05Polymorphism`](src/t07_inheritance_polymorphism/Inherit05Polymorphism.java) (2)
- **dynamiczne proxy (java.lang.reflect.Proxy)** → [`t19_annotations_reflection/Annotations06DynamicProxy`](src/t19_annotations_reflection/Annotations06DynamicProxy.java)
- **dziedziczenie (extends)** → [`t07_inheritance_polymorphism/Inherit01Basics`](src/t07_inheritance_polymorphism/Inherit01Basics.java) (1, 2)
- **dzielenie całkowite** → [`t01_basics/Basics04Operators`](src/t01_basics/Basics04Operators.java) (1)

### E

- **efekt uboczny (side effect)** → [`t05_methods/Methods04GoodPractices`](src/t05_methods/Methods04GoodPractices.java) (5); [`t13_lambdas/Lambda08Pitfalls`](src/t13_lambdas/Lambda08Pitfalls.java) (4); [`t16_streams/Streams17SideEffectsPitfalls`](src/t16_streams/Streams17SideEffectsPitfalls.java) (1)
- **effectively final (efektywnie finalna)** → [`t13_lambdas/Lambda06ClosuresScope`](src/t13_lambdas/Lambda06ClosuresScope.java) (2, 3); [`t23_modern_java/Modern01Java8`](src/t23_modern_java/Modern01Java8.java) (9)
- **emoji i para zastępcza (surrogate pair)** → [`t04_strings/Strings06CharUnicode`](src/t04_strings/Strings06CharUnicode.java) (6); [`t18_io_files/Io12Charsets`](src/t18_io_files/Io12Charsets.java) (7)
- **enum (typ wyliczeniowy)** → [`t08_enums/Enums01Basics`](src/t08_enums/Enums01Basics.java)
- **enum implementujący interfejs** → [`t08_enums/Enums03ConstantBodies`](src/t08_enums/Enums03ConstantBodies.java) (4)
- **enum jako singleton** → [`t08_enums/Enums03ConstantBodies`](src/t08_enums/Enums03ConstantBodies.java) (6); [`t22_design_patterns/Patterns04Singleton`](src/t22_design_patterns/Patterns04Singleton.java) (4)
- **EnumMap** → [`t08_enums/Enums04EnumMapSet`](src/t08_enums/Enums04EnumMapSet.java) (3, 4)
- **EnumSet** → [`t08_enums/Enums04EnumMapSet`](src/t08_enums/Enums04EnumMapSet.java) (1, 2)
- **equals** → [`t06_oop_basics/Oop05ObjectMethods`](src/t06_oop_basics/Oop05ObjectMethods.java) (4)
- **equals bez hashCode** → [`t06_oop_basics/Oop05ObjectMethods`](src/t06_oop_basics/Oop05ObjectMethods.java) (8); [`t12_collections/Collections04Sets`](src/t12_collections/Collections04Sets.java) (5)
- **etykiety (break / continue z etykietą)** → [`t02_controlflow/Control04BreakContinueLabels`](src/t02_controlflow/Control04BreakContinueLabels.java) (5, 7)
- **Evaluate Expression i Watches** → [`t00_start/Start05Debugging`](src/t00_start/Start05Debugging.java) (4)
- **exception breakpoint** → [`t00_start/Start05Debugging`](src/t00_start/Start05Debugging.java) (5)
- **ExecutorService** → [`t21_concurrency/Concurrency04Executors`](src/t21_concurrency/Concurrency04Executors.java)

### F

- **Facade (wzorzec)** → [`t22_design_patterns/Patterns10Facade`](src/t22_design_patterns/Patterns10Facade.java)
- **Factory (wzorzec)** → [`t22_design_patterns/Patterns03Factory`](src/t22_design_patterns/Patterns03Factory.java)
- **fail fast** → [`t05_methods/Methods04GoodPractices`](src/t05_methods/Methods04GoodPractices.java) (4); [`t10_exceptions/Exceptions07BestPractices`](src/t10_exceptions/Exceptions07BestPractices.java) (3); [`t27_clean_code_pitfalls/CleanCode01Principles`](src/t27_clean_code_pitfalls/CleanCode01Principles.java) (7)
- **fall-through (brak break w switchu)** → [`t02_controlflow/Control02Switch`](src/t02_controlflow/Control02Switch.java) (2)
- **Fibonacci: rekurencja, iteracja, memoizacja** → [`t05_methods/Methods03Recursion`](src/t05_methods/Methods03Recursion.java) (3); [`t24_algorithms/Math09InterviewClassics`](src/t24_algorithms/Math09InterviewClassics.java) (2); [`t24_algorithms/Algorithms07DynamicProgramming`](src/t24_algorithms/Algorithms07DynamicProgramming.java) (1)
- **Files.lines** → [`t16_streams/Streams02Creation`](src/t16_streams/Streams02Creation.java) (9); [`t18_io_files/Io02ReadingText`](src/t18_io_files/Io02ReadingText.java) (3)
- **Files.readString / readAllLines** → [`t18_io_files/Io02ReadingText`](src/t18_io_files/Io02ReadingText.java) (1, 2)
- **Files.walk / list / find** → [`t18_io_files/Io07WalkingDirectories`](src/t18_io_files/Io07WalkingDirectories.java) (1, 2, 3)
- **Files.writeString / write** → [`t18_io_files/Io03WritingText`](src/t18_io_files/Io03WritingText.java) (1, 3)
- **filter** → [`t16_streams/Streams03FilterMap`](src/t16_streams/Streams03FilterMap.java) (1)
- **final / finally / finalize** → [`t33_interview_prep/Interview01JavaQuestions`](src/t33_interview_prep/Interview01JavaQuestions.java) (4)
- **finally** → [`t10_exceptions/Exceptions01Basics`](src/t10_exceptions/Exceptions01Basics.java) (4); [`t10_exceptions/Exceptions03MultiCatch`](src/t10_exceptions/Exceptions03MultiCatch.java) (5)
- **findFirst** → [`t16_streams/Streams01Intro`](src/t16_streams/Streams01Intro.java) (7); [`t16_streams/Streams06TerminalOps`](src/t16_streams/Streams06TerminalOps.java) (5)
- **FizzBuzz** → [`t02_controlflow/Control05LoopPatterns`](src/t02_controlflow/Control05LoopPatterns.java) (7); [`t24_algorithms/Math09InterviewClassics`](src/t24_algorithms/Math09InterviewClassics.java) (8); [`t33_interview_prep/Interview03CodingTasks`](src/t33_interview_prep/Interview03CodingTasks.java) (1)
- **flatMap** → [`t16_streams/Streams04FlatMap`](src/t16_streams/Streams04FlatMap.java)
- **floorDiv / floorMod** → [`t15_numbers/Numbers05IntegerTricks`](src/t15_numbers/Numbers05IntegerTricks.java) (6); [`t15_numbers/Numbers06MathCheatsheet`](src/t15_numbers/Numbers06MathCheatsheet.java) (6); [`t24_algorithms/Math02ModularChecksums`](src/t24_algorithms/Math02ModularChecksums.java) (1)
- **for-each** → [`t02_controlflow/Control03Loops`](src/t02_controlflow/Control03Loops.java) (5); [`t12_collections/Collections03IterationModification`](src/t12_collections/Collections03IterationModification.java) (1)
- **ForkJoinPool / RecursiveTask** → [`t21_concurrency/Concurrency09ScheduledForkJoin`](src/t21_concurrency/Concurrency09ScheduledForkJoin.java) (5, 6)
- **format: flagi i numerowane argumenty** → [`t04_strings/Strings04Formatting`](src/t04_strings/Strings04Formatting.java) (1, 2)
- **Function: andThen / compose / identity** → [`t13_lambdas/Lambda05Composition`](src/t13_lambdas/Lambda05Composition.java) (4)
- **@FunctionalInterface** → [`t13_lambdas/Lambda02FunctionalInterfaces`](src/t13_lambdas/Lambda02FunctionalInterfaces.java) (2); [`t19_annotations_reflection/Annotations01BuiltIn`](src/t19_annotations_reflection/Annotations01BuiltIn.java) (5)
- **funkcja wyższego rzędu** → [`t13_lambdas/Lambda07HigherOrderFunctions`](src/t13_lambdas/Lambda07HigherOrderFunctions.java)
- **funkcje okna (ROW_NUMBER)** → [`t29_jdbc_databases/Jdbc07SqlAggregationIndexes`](src/t29_jdbc_databases/Jdbc07SqlAggregationIndexes.java) (6)
- **funkcyjny rdzeń, imperatywna powłoka** → [`t25_testing/Testing03TestableDesign`](src/t25_testing/Testing03TestableDesign.java) (3, 4)
- **Future** → [`t21_concurrency/Concurrency04Executors`](src/t21_concurrency/Concurrency04Executors.java) (3)

### G

- **generyczne repozytorium** → [`t11_generics/Generics07Repository`](src/t11_generics/Generics07Repository.java)
- **generyki (generics)** → [`t11_generics/Generics01Why`](src/t11_generics/Generics01Why.java)
- **Git — od zera** → [`t00_start/Start06Git`](src/t00_start/Start06Git.java)
- **Git: commit i jego skrót** → [`t00_start/Start06Git`](src/t00_start/Start06Git.java) (1)
- **Git: gałąź (branch)** → [`t00_start/Start06Git`](src/t00_start/Start06Git.java) (4)
- **Git: rebase** → [`t00_start/Start06Git`](src/t00_start/Start06Git.java) (8)
- **Git: scalanie (merge), fast-forward** → [`t00_start/Start06Git`](src/t00_start/Start06Git.java) (5, 6)
- **gniazda (Socket, ServerSocket)** → [`t28_networking_http/Net01SocketsTcpUdp`](src/t28_networking_http/Net01SocketsTcpUdp.java)
- **graf (lista sąsiedztwa)** → [`t24_algorithms/Algorithms08Graphs`](src/t24_algorithms/Algorithms08Graphs.java) (1)
- **granica transakcji** → [`t34_toward_spring/Spring02Layers`](src/t34_toward_spring/Spring02Layers.java) (5)
- **GROUP BY / HAVING** → [`t29_jdbc_databases/Jdbc07SqlAggregationIndexes`](src/t29_jdbc_databases/Jdbc07SqlAggregationIndexes.java) (2, 3)
- **groupingBy** → [`t16_streams/Streams11GroupingBy`](src/t16_streams/Streams11GroupingBy.java)
- **gruby JAR (fat JAR)** → [`t30_build_modules/Build02JarClasspath`](src/t30_build_modules/Build02JarClasspath.java) (8)
- **grupy nazwane i niezachwytujące** → [`t04_strings/Strings08RegexAdvanced`](src/t04_strings/Strings08RegexAdvanced.java) (1)

### H

- **happens-before** → [`t21_concurrency/Concurrency10MemoryModel`](src/t21_concurrency/Concurrency10MemoryModel.java) (2)
- **hashCode (kontrakt)** → [`t06_oop_basics/Oop05ObjectMethods`](src/t06_oop_basics/Oop05ObjectMethods.java) (5); [`t12_collections/Collections11HashingInternals`](src/t12_collections/Collections11HashingInternals.java) (1, 2)
- **HashMap — jak działa** → [`t12_collections/Collections11HashingInternals`](src/t12_collections/Collections11HashingInternals.java); [`t12_collections/Collections05Maps`](src/t12_collections/Collections05Maps.java) (1)
- **HashSet** → [`t12_collections/Collections04Sets`](src/t12_collections/Collections04Sets.java) (1, 6)
- **hasła: PBKDF2, sól** → [`t31_jdk_toolbox/Toolbox02HashingSecurity`](src/t31_jdk_toolbox/Toolbox02HashingSecurity.java) (6, 7)
- **helpful NullPointerException (pomocne komunikaty NPE)** → [`t23_modern_java/Modern06ApiAdditions`](src/t23_modern_java/Modern06ApiAdditions.java) (8); [`t16_streams/Streams03FilterMap`](src/t16_streams/Streams03FilterMap.java) (8)
- **hermetyzacja (encapsulation)** → [`t06_oop_basics/Oop03Encapsulation`](src/t06_oop_basics/Oop03Encapsulation.java)
- **HMAC** → [`t31_jdk_toolbox/Toolbox02HashingSecurity`](src/t31_jdk_toolbox/Toolbox02HashingSecurity.java) (9)
- **HTTP: 404 i 500 to nie wyjątki** → [`t28_networking_http/Http02HttpClient`](src/t28_networking_http/Http02HttpClient.java) (5)
- **HttpClient** → [`t28_networking_http/Http02HttpClient`](src/t28_networking_http/Http02HttpClient.java)
- **HttpServer (com.sun.net.httpserver)** → [`t28_networking_http/Http03LocalServer`](src/t28_networking_http/Http03LocalServer.java)

### I

- **idempotentność** → [`t34_toward_spring/Spring03RestConcepts`](src/t34_toward_spring/Spring03RestConcepts.java) (6); [`t28_networking_http/Http01UriUrl`](src/t28_networking_http/Http01UriUrl.java) (7)
- **if / else if / else** → [`t02_controlflow/Control01IfElse`](src/t02_controlflow/Control01IfElse.java)
- **ifPresent / ifPresentOrElse** → [`t14_optional/Optional01Basics`](src/t14_optional/Optional01Basics.java) (8)
- **IllegalStateException (ponowne użycie streamu)** → [`t16_streams/Streams01Intro`](src/t16_streams/Streams01Intro.java) (8); [`t16_streams/Streams17SideEffectsPitfalls`](src/t16_streams/Streams17SideEffectsPitfalls.java) (3)
- **import static** → [`t06_oop_basics/Oop04Static`](src/t06_oop_basics/Oop04Static.java) (6); [`t06_oop_basics/Oop08PackagesAccess`](src/t06_oop_basics/Oop08PackagesAccess.java) (3)
- **indeks w bazie danych** → [`t29_jdbc_databases/Jdbc07SqlAggregationIndexes`](src/t29_jdbc_databases/Jdbc07SqlAggregationIndexes.java) (7, 8, 9)
- **Infinity i NaN** → [`t01_basics/Basics08FloatingPoint`](src/t01_basics/Basics08FloatingPoint.java) (5); [`t15_numbers/Numbers06MathCheatsheet`](src/t15_numbers/Numbers06MathCheatsheet.java) (10)
- **inicjalizacja klas (leniwa)** → [`t26_jvm/Jvm02ClassLoadingInit`](src/t26_jvm/Jvm02ClassLoadingInit.java) (1, 2)
- **inkrementacja (i++ kontra ++i)** → [`t01_basics/Basics04Operators`](src/t01_basics/Basics04Operators.java) (2)
- **INNER JOIN** → [`t29_jdbc_databases/Jdbc06SqlJoins`](src/t29_jdbc_databases/Jdbc06SqlJoins.java) (1)
- **instanceof ze wzorcem (pattern matching)** → [`t07_inheritance_polymorphism/Inherit05Polymorphism`](src/t07_inheritance_polymorphism/Inherit05Polymorphism.java) (4); [`t23_modern_java/Modern05RecordsSealedPatterns`](src/t23_modern_java/Modern05RecordsSealedPatterns.java) (2, 3)
- **Instant** → [`t17_datetime/DateTime04ZonesInstant`](src/t17_datetime/DateTime04ZonesInstant.java) (1)
- **Integer cache (== na Integer)** → [`t01_basics/Basics06Wrappers`](src/t01_basics/Basics06Wrappers.java) (3); [`t27_clean_code_pitfalls/Pitfalls01Classic`](src/t27_clean_code_pitfalls/Pitfalls01Classic.java) (1); [`t33_interview_prep/Interview01JavaQuestions`](src/t33_interview_prep/Interview01JavaQuestions.java) (2)
- **interfejs** → [`t07_inheritance_polymorphism/Inherit04Interfaces`](src/t07_inheritance_polymorphism/Inherit04Interfaces.java)
- **interfejs funkcyjny** → [`t13_lambdas/Lambda02FunctionalInterfaces`](src/t13_lambdas/Lambda02FunctionalInterfaces.java) (1, 4)
- **interfejs kontra klasa abstrakcyjna** → [`t07_inheritance_polymorphism/Inherit03AbstractClasses`](src/t07_inheritance_polymorphism/Inherit03AbstractClasses.java) (6); [`t07_inheritance_polymorphism/Inherit04Interfaces`](src/t07_inheritance_polymorphism/Inherit04Interfaces.java) (1)
- **interfejs znacznikowy** → [`t07_inheritance_polymorphism/Inherit04Interfaces`](src/t07_inheritance_polymorphism/Inherit04Interfaces.java) (7)
- **intern** → [`t26_jvm/Jvm01Memory`](src/t26_jvm/Jvm01Memory.java) (5)
- **internacjonalizacja (i18n)** → [`t31_jdk_toolbox/Toolbox03I18n`](src/t31_jdk_toolbox/Toolbox03I18n.java)
- **interrupt (przerywanie wątku)** → [`t21_concurrency/Concurrency01Threads`](src/t21_concurrency/Concurrency01Threads.java) (7, 8)
- **IntStream.range / rangeClosed** → [`t16_streams/Streams02Creation`](src/t16_streams/Streams02Creation.java) (4)
- **InvocationTargetException** → [`t19_annotations_reflection/Annotations03ReflectionBasics`](src/t19_annotations_reflection/Annotations03ReflectionBasics.java) (4); [`t19_annotations_reflection/Annotations05MiniFramework`](src/t19_annotations_reflection/Annotations05MiniFramework.java) (7); [`t19_annotations_reflection/Annotations06DynamicProxy`](src/t19_annotations_reflection/Annotations06DynamicProxy.java) (6)
- **Iterable / Iterator (własny)** → [`t12_collections/Collections12CustomIterable`](src/t12_collections/Collections12CustomIterable.java)
- **iterator fail-fast** → [`t12_collections/Collections03IterationModification`](src/t12_collections/Collections03IterationModification.java) (3); [`t33_interview_prep/Interview02OopCollections`](src/t33_interview_prep/Interview02OopCollections.java) (8)

### J

- **Java 9–17: małe perełki API** → [`t23_modern_java/Modern06ApiAdditions`](src/t23_modern_java/Modern06ApiAdditions.java)
- **Java 21: switch ze wzorcami** → [`t23_modern_java/Modern07WhatsNextJava21`](src/t23_modern_java/Modern07WhatsNextJava21.java) (4)
- **java.util.Formatter** → [`t04_strings/Strings09FormatterCheatsheet`](src/t04_strings/Strings09FormatterCheatsheet.java)
- **java.util.function** → [`t13_lambdas/Lambda03JavaUtilFunction`](src/t13_lambdas/Lambda03JavaUtilFunction.java)
- **JavaBeans (gettery i settery)** → [`t06_oop_basics/Oop03Encapsulation`](src/t06_oop_basics/Oop03Encapsulation.java) (6); [`t22_design_patterns/Patterns02Builder`](src/t22_design_patterns/Patterns02Builder.java) (2)
- **Javadoc** → [`t30_build_modules/Build06QualityToolsJavadoc`](src/t30_build_modules/Build06QualityToolsJavadoc.java) (1, 2)
- **jcmd** → [`t26_jvm/Jvm04ToolsProfiling`](src/t26_jvm/Jvm04ToolsProfiling.java) (1)
- **JDBC** → [`t29_jdbc_databases/Jdbc02Connection`](src/t29_jdbc_databases/Jdbc02Connection.java)
- **jedna metoda = jedna odpowiedzialność** → [`t05_methods/Methods04GoodPractices`](src/t05_methods/Methods04GoodPractices.java) (1); [`t07_inheritance_polymorphism/Inherit08Solid`](src/t07_inheritance_polymorphism/Inherit08Solid.java) (1)
- **JIT (kompilacja w czasie działania)** → [`t26_jvm/Jvm04ToolsProfiling`](src/t26_jvm/Jvm04ToolsProfiling.java) (6)
- **JShell** → [`t00_start/Start08JShell`](src/t00_start/Start08JShell.java)
- **JSON ręczny (własny parser)** → [`t18_io_files/Io05JsonManual`](src/t18_io_files/Io05JsonManual.java); [`t28_networking_http/Http04JsonApi`](src/t28_networking_http/Http04JsonApi.java) (1, 2)
- **JUnit 5** → [`t32_junit_mockito/JUnit01Basics`](src/t32_junit_mockito/JUnit01Basics.java)
- **JVM, JDK, JRE** → [`t01_basics/Basics01HelloJvm`](src/t01_basics/Basics01HelloJvm.java); [`t33_interview_prep/Interview01JavaQuestions`](src/t33_interview_prep/Interview01JavaQuestions.java) (1)

### K

- **katastrofalne cofanie (ReDoS)** → [`t04_strings/Strings08RegexAdvanced`](src/t04_strings/Strings08RegexAdvanced.java) (8)
- **klasa abstrakcyjna** → [`t07_inheritance_polymorphism/Inherit03AbstractClasses`](src/t07_inheritance_polymorphism/Inherit03AbstractClasses.java)
- **klasa anonimowa** → [`t06_oop_basics/Oop07NestedClasses`](src/t06_oop_basics/Oop07NestedClasses.java) (5, 6); [`t13_lambdas/Lambda01FromAnonymousToLambda`](src/t13_lambdas/Lambda01FromAnonymousToLambda.java) (3)
- **klasa narzędziowa (utility class)** → [`t06_oop_basics/Oop04Static`](src/t06_oop_basics/Oop04Static.java) (5)
- **klasa wewnętrzna (inner) i Outer.this** → [`t06_oop_basics/Oop07NestedClasses`](src/t06_oop_basics/Oop07NestedClasses.java) (2, 3, 7)
- **klasa zagnieżdżona (nested)** → [`t06_oop_basics/Oop07NestedClasses`](src/t06_oop_basics/Oop07NestedClasses.java)
- **klasyczne pułapki Javy** → [`t27_clean_code_pitfalls/Pitfalls01Classic`](src/t27_clean_code_pitfalls/Pitfalls01Classic.java)
- **kod uzupełnień do dwóch** → [`t24_algorithms/Math04NumberSystems`](src/t24_algorithms/Math04NumberSystems.java) (5)
- **kod wyjścia (exit code)** → [`t30_build_modules/Build04CommandLineApps`](src/t30_build_modules/Build04CommandLineApps.java) (5)
- **kodowanie na żywo (live coding)** → [`t33_interview_prep/Interview04LiveCoding`](src/t33_interview_prep/Interview04LiveCoding.java)
- **kodowanie znaków (charset)** → [`t18_io_files/Io12Charsets`](src/t18_io_files/Io12Charsets.java)
- **kody statusu HTTP** → [`t28_networking_http/Http01UriUrl`](src/t28_networking_http/Http01UriUrl.java) (8); [`t34_toward_spring/Spring03RestConcepts`](src/t34_toward_spring/Spring03RestConcepts.java) (2)
- **kolejność inicjalizacji** → [`t06_oop_basics/Oop02Constructors`](src/t06_oop_basics/Oop02Constructors.java) (8); [`t26_jvm/Jvm02ClassLoadingInit`](src/t26_jvm/Jvm02ClassLoadingInit.java) (2)
- **kolekcje sekwencyjne (Java 21)** → [`t23_modern_java/Modern07WhatsNextJava21`](src/t23_modern_java/Modern07WhatsNextJava21.java) (6)
- **kombinacje (symbol Newtona)** → [`t24_algorithms/Math03Combinatorics`](src/t24_algorithms/Math03Combinatorics.java) (3); [`t24_algorithms/Algorithms06Backtracking`](src/t24_algorithms/Algorithms06Backtracking.java) (3)
- **kombinatoryka** → [`t24_algorithms/Math03Combinatorics`](src/t24_algorithms/Math03Combinatorics.java)
- **komparator przez odejmowanie (błąd)** → [`t12_collections/Collections07ComparableComparator`](src/t12_collections/Collections07ComparableComparator.java) (8); [`t15_numbers/Numbers05IntegerTricks`](src/t15_numbers/Numbers05IntegerTricks.java) (9)
- **kompozycja kontra dziedziczenie** → [`t07_inheritance_polymorphism/Inherit06CompositionVsInheritance`](src/t07_inheritance_polymorphism/Inherit06CompositionVsInheritance.java)
- **konflikt diamentu** → [`t07_inheritance_polymorphism/Inherit04Interfaces`](src/t07_inheritance_polymorphism/Inherit04Interfaces.java) (4)
- **konkatenacja w pętli (+= kontra StringBuilder)** → [`t04_strings/Strings03StringBuilder`](src/t04_strings/Strings03StringBuilder.java) (1); [`t02_controlflow/Control05LoopPatterns`](src/t02_controlflow/Control05LoopPatterns.java) (5); [`t27_clean_code_pitfalls/Pitfalls02CodeReview`](src/t27_clean_code_pitfalls/Pitfalls02CodeReview.java) (7)
- **konstruktor** → [`t06_oop_basics/Oop02Constructors`](src/t06_oop_basics/Oop02Constructors.java)
- **konstruktor kopiujący** → [`t06_oop_basics/Oop02Constructors`](src/t06_oop_basics/Oop02Constructors.java) (9); [`t06_oop_basics/Oop10Copying`](src/t06_oop_basics/Oop10Copying.java) (3)
- **konstruktor w hierarchii (łańcuch konstruktorów)** → [`t07_inheritance_polymorphism/Inherit01Basics`](src/t07_inheritance_polymorphism/Inherit01Basics.java) (4)
- **kontener IoC (mini-Spring)** → [`t34_toward_spring/Spring01IocContainer`](src/t34_toward_spring/Spring01IocContainer.java)
- **konwencje nazewnicze** → [`t01_basics/Basics01HelloJvm`](src/t01_basics/Basics01HelloJvm.java) (5)
- **kopia obronna (defensive copy)** → [`t06_oop_basics/Oop06Immutability`](src/t06_oop_basics/Oop06Immutability.java) (4, 5); [`t09_records/Records02Constructors`](src/t09_records/Records02Constructors.java) (3); [`t12_collections/Collections08ImmutableUnmodifiable`](src/t12_collections/Collections08ImmutableUnmodifiable.java) (7)
- **kopia płytka i głęboka** → [`t06_oop_basics/Oop10Copying`](src/t06_oop_basics/Oop10Copying.java) (2); [`t03_arrays/Arrays03Utility`](src/t03_arrays/Arrays03Utility.java) (9)
- **kopiec binarny (heap)** → [`t24_algorithms/Algorithms04DataStructures`](src/t24_algorithms/Algorithms04DataStructures.java) (4)
- **kopiowanie obiektów** → [`t06_oop_basics/Oop10Copying`](src/t06_oop_basics/Oop10Copying.java)
- **korzeń kompozycji (composition root)** → [`t22_design_patterns/Patterns08DependencyInjection`](src/t22_design_patterns/Patterns08DependencyInjection.java) (4)
- **krucha klasa bazowa** → [`t07_inheritance_polymorphism/Inherit06CompositionVsInheritance`](src/t07_inheritance_polymorphism/Inherit06CompositionVsInheritance.java) (2); [`t22_design_patterns/Patterns05TemplateMethod`](src/t22_design_patterns/Patterns05TemplateMethod.java) (8)
- **kubełek (bucket) i kolizje** → [`t12_collections/Collections11HashingInternals`](src/t12_collections/Collections11HashingInternals.java) (2, 4, 6)
- **kwantyfikatory zachłanne / leniwe / zaborcze** → [`t04_strings/Strings05Regex`](src/t04_strings/Strings05Regex.java) (8); [`t04_strings/Strings08RegexAdvanced`](src/t04_strings/Strings08RegexAdvanced.java) (2)

### L

- **lambda** → [`t13_lambdas/Lambda01FromAnonymousToLambda`](src/t13_lambdas/Lambda01FromAnonymousToLambda.java) (4, 5)
- **LEFT JOIN** → [`t29_jdbc_databases/Jdbc06SqlJoins`](src/t29_jdbc_databases/Jdbc06SqlJoins.java) (2, 3)
- **leniwa inicjalizacja** → [`t21_concurrency/Concurrency08ThreadSafetyPatterns`](src/t21_concurrency/Concurrency08ThreadSafetyPatterns.java) (5); [`t22_design_patterns/Patterns04Singleton`](src/t22_design_patterns/Patterns04Singleton.java) (3)
- **leniwość (lazy)** → [`t16_streams/Streams01Intro`](src/t16_streams/Streams01Intro.java) (5); [`t16_streams/Streams16Laziness`](src/t16_streams/Streams16Laziness.java)
- **liczby magiczne (stałe zamiast nich)** → [`t01_basics/Basics03Variables`](src/t01_basics/Basics03Variables.java) (5); [`t27_clean_code_pitfalls/Pitfalls02CodeReview`](src/t27_clean_code_pitfalls/Pitfalls02CodeReview.java) (1)
- **liczby pierwsze (test pierwszości)** → [`t24_algorithms/Math01NumberTheory`](src/t24_algorithms/Math01NumberTheory.java) (5); [`t24_algorithms/Math09InterviewClassics`](src/t24_algorithms/Math09InterviewClassics.java) (3)
- **LinkedHashMap** → [`t12_collections/Collections05Maps`](src/t12_collections/Collections05Maps.java) (8)
- **List.copyOf** → [`t12_collections/Collections08ImmutableUnmodifiable`](src/t12_collections/Collections08ImmutableUnmodifiable.java) (3); [`t23_modern_java/Modern06ApiAdditions`](src/t23_modern_java/Modern06ApiAdditions.java) (4)
- **List<Integer>: remove(int) kontra remove(Object)** → [`t01_basics/Basics06Wrappers`](src/t01_basics/Basics06Wrappers.java) (7); [`t12_collections/Collections02Lists`](src/t12_collections/Collections02Lists.java) (3); [`t11_generics/Generics01Why`](src/t11_generics/Generics01Why.java) (6)
- **List.of / Set.of / Map.of** → [`t12_collections/Collections01Overview`](src/t12_collections/Collections01Overview.java) (6); [`t12_collections/Collections08ImmutableUnmodifiable`](src/t12_collections/Collections08ImmutableUnmodifiable.java) (1); [`t23_modern_java/Modern06ApiAdditions`](src/t23_modern_java/Modern06ApiAdditions.java) (1)
- **ListIterator** → [`t12_collections/Collections03IterationModification`](src/t12_collections/Collections03IterationModification.java) (6)
- **LocalDate / LocalTime / LocalDateTime** → [`t17_datetime/DateTime01LocalDateTime`](src/t17_datetime/DateTime01LocalDateTime.java)
- **Locale** → [`t15_numbers/Numbers04FormattingParsing`](src/t15_numbers/Numbers04FormattingParsing.java) (1); [`t31_jdk_toolbox/Toolbox03I18n`](src/t31_jdk_toolbox/Toolbox03I18n.java) (1)
- **Lombok** → [`t20_lombok/Lombok01Accessors`](src/t20_lombok/Lombok01Accessors.java) (1); [`t19_annotations_reflection/Annotations07Processors`](src/t19_annotations_reflection/Annotations07Processors.java) (8)
- **Lombok: @Builder / @Singular** → [`t20_lombok/Lombok03DataValueBuilder`](src/t20_lombok/Lombok03DataValueBuilder.java) (5, 6)
- **Lombok: @Data** → [`t20_lombok/Lombok03DataValueBuilder`](src/t20_lombok/Lombok03DataValueBuilder.java) (1)
- **Lombok: @Getter / @Setter** → [`t20_lombok/Lombok01Accessors`](src/t20_lombok/Lombok01Accessors.java) (2, 3)
- **Lombok: @SneakyThrows** → [`t20_lombok/Lombok04Other`](src/t20_lombok/Lombok04Other.java) (2)
- **Lombok: @Value** → [`t20_lombok/Lombok03DataValueBuilder`](src/t20_lombok/Lombok03DataValueBuilder.java) (3, 4)
- **lookahead / lookbehind** → [`t04_strings/Strings08RegexAdvanced`](src/t04_strings/Strings08RegexAdvanced.java) (3)

### Ł

- **ładowanie klas (class loading)** → [`t26_jvm/Jvm02ClassLoadingInit`](src/t26_jvm/Jvm02ClassLoadingInit.java) (1, 8)
- **łańcuch wyjątków (cause)** → [`t10_exceptions/Exceptions06ChainingWrapping`](src/t10_exceptions/Exceptions06ChainingWrapping.java)

### M

- **map** → [`t16_streams/Streams03FilterMap`](src/t16_streams/Streams03FilterMap.java) (3, 4)
- **Map.merge (zliczanie)** → [`t12_collections/Collections05Maps`](src/t12_collections/Collections05Maps.java) (5); [`t12_collections/Collections10Patterns`](src/t12_collections/Collections10Patterns.java) (1)
- **mapMulti** → [`t16_streams/Streams04FlatMap`](src/t16_streams/Streams04FlatMap.java) (8); [`t23_modern_java/Modern06ApiAdditions`](src/t23_modern_java/Modern06ApiAdditions.java) (9)
- **mapToInt / mapToDouble / mapToObj** → [`t16_streams/Streams03FilterMap`](src/t16_streams/Streams03FilterMap.java) (5)
- **maszyna stanów (state machine)** → [`t08_enums/Enums04EnumMapSet`](src/t08_enums/Enums04EnumMapSet.java) (6, 7); [`t22_design_patterns/Patterns13State`](src/t22_design_patterns/Patterns13State.java) (4, 5)
- **Math.addExact / multiplyExact / toIntExact** → [`t01_basics/Basics05Casting`](src/t01_basics/Basics05Casting.java) (7); [`t15_numbers/Numbers05IntegerTricks`](src/t15_numbers/Numbers05IntegerTricks.java) (3)
- **Math kontra StrictMath** → [`t15_numbers/Numbers06MathCheatsheet`](src/t15_numbers/Numbers06MathCheatsheet.java) (9)
- **Math.random** → [`t01_basics/Basics07MathRandom`](src/t01_basics/Basics07MathRandom.java) (5)
- **Math.round / floor / ceil** → [`t01_basics/Basics07MathRandom`](src/t01_basics/Basics07MathRandom.java) (2); [`t15_numbers/Numbers06MathCheatsheet`](src/t15_numbers/Numbers06MathCheatsheet.java) (5)
- **Maven** → [`t30_build_modules/Build01MavenBasics`](src/t30_build_modules/Build01MavenBasics.java)
- **Maven: zakresy zależności (scope)** → [`t30_build_modules/Build01MavenBasics`](src/t30_build_modules/Build01MavenBasics.java) (6)
- **memoizacja** → [`t13_lambdas/Lambda07HigherOrderFunctions`](src/t13_lambdas/Lambda07HigherOrderFunctions.java) (7)
- **MessageDigest (SHA-256)** → [`t31_jdk_toolbox/Toolbox02HashingSecurity`](src/t31_jdk_toolbox/Toolbox02HashingSecurity.java) (1, 2)
- **metoda default w interfejsie** → [`t07_inheritance_polymorphism/Inherit04Interfaces`](src/t07_inheritance_polymorphism/Inherit04Interfaces.java) (3); [`t23_modern_java/Modern01Java8`](src/t23_modern_java/Modern01Java8.java) (3)
- **metoda Newtona** → [`t24_algorithms/Math05NumericalMethods`](src/t24_algorithms/Math05NumericalMethods.java) (2)
- **metody HTTP: bezpieczne i idempotentne** → [`t28_networking_http/Http01UriUrl`](src/t28_networking_http/Http01UriUrl.java) (7); [`t34_toward_spring/Spring03RestConcepts`](src/t34_toward_spring/Spring03RestConcepts.java) (2, 6)
- **metody with... (wither)** → [`t06_oop_basics/Oop06Immutability`](src/t06_oop_basics/Oop06Immutability.java) (3); [`t09_records/Records02Constructors`](src/t09_records/Records02Constructors.java) (6); [`t20_lombok/Lombok03DataValueBuilder`](src/t20_lombok/Lombok03DataValueBuilder.java) (7)
- **mini framework (router komend)** → [`t19_annotations_reflection/Annotations05MiniFramework`](src/t19_annotations_reflection/Annotations05MiniFramework.java)
- **mock** → [`t25_testing/Testing02TestDoubles`](src/t25_testing/Testing02TestDoubles.java) (7); [`t32_junit_mockito/JUnit04Mockito`](src/t32_junit_mockito/JUnit04Mockito.java) (2)
- **Mockito** → [`t32_junit_mockito/JUnit04Mockito`](src/t32_junit_mockito/JUnit04Mockito.java)
- **Mockito: ArgumentCaptor** → [`t32_junit_mockito/JUnit04Mockito`](src/t32_junit_mockito/JUnit04Mockito.java) (5)
- **model anemiczny kontra bogaty** → [`t27_clean_code_pitfalls/CleanCode03Architecture`](src/t27_clean_code_pitfalls/CleanCode03Architecture.java) (6)
- **model pamięci Javy** → [`t21_concurrency/Concurrency10MemoryModel`](src/t21_concurrency/Concurrency10MemoryModel.java)
- **moduły JPMS (module-info.java)** → [`t30_build_modules/Build03Modules`](src/t30_build_modules/Build03Modules.java)
- **modyfikatory dostępu** → [`t06_oop_basics/Oop08PackagesAccess`](src/t06_oop_basics/Oop08PackagesAccess.java) (6); [`t06_oop_basics/Oop03Encapsulation`](src/t06_oop_basics/Oop03Encapsulation.java) (9)
- **Money (pieniądze jako obiekt-wartość)** → [`t15_numbers/Numbers02MoneyValueObject`](src/t15_numbers/Numbers02MoneyValueObject.java)
- **multi-catch** → [`t10_exceptions/Exceptions03MultiCatch`](src/t10_exceptions/Exceptions03MultiCatch.java) (2)

### N

- **nadpisywalna metoda wołana z konstruktora** → [`t07_inheritance_polymorphism/Inherit02Override`](src/t07_inheritance_polymorphism/Inherit02Override.java) (8); [`t22_design_patterns/Patterns05TemplateMethod`](src/t22_design_patterns/Patterns05TemplateMethod.java) (4)
- **nadpisywanie metod (override)** → [`t07_inheritance_polymorphism/Inherit02Override`](src/t07_inheritance_polymorphism/Inherit02Override.java); [`t07_inheritance_polymorphism/Inherit02Override`](src/t07_inheritance_polymorphism/Inherit02Override.java) (2)
- **nawracanie (backtracking)** → [`t24_algorithms/Algorithms06Backtracking`](src/t24_algorithms/Algorithms06Backtracking.java)
- **niezmienność (immutable)** → [`t06_oop_basics/Oop06Immutability`](src/t06_oop_basics/Oop06Immutability.java); [`t09_records/Records01Basics`](src/t09_records/Records01Basics.java) (5); [`t12_collections/Collections08ImmutableUnmodifiable`](src/t12_collections/Collections08ImmutableUnmodifiable.java)
- **non-sealed** → [`t07_inheritance_polymorphism/Inherit07SealedClasses`](src/t07_inheritance_polymorphism/Inherit07SealedClasses.java) (5)
- **normalizacja i klucze obce** → [`t29_jdbc_databases/Jdbc06SqlJoins`](src/t29_jdbc_databases/Jdbc06SqlJoins.java) (10)
- **normalizacja Unicode** → [`t18_io_files/Io12Charsets`](src/t18_io_files/Io12Charsets.java) (11); [`t31_jdk_toolbox/Toolbox03I18n`](src/t31_jdk_toolbox/Toolbox03I18n.java) (8)
- **null — błąd za miliard dolarów** → [`t14_optional/Optional01Basics`](src/t14_optional/Optional01Basics.java) (1)
- **null rozpakowany do int** → [`t01_basics/Basics06Wrappers`](src/t01_basics/Basics06Wrappers.java) (4)
- **null w kolekcjach** → [`t12_collections/Collections01Overview`](src/t12_collections/Collections01Overview.java) (5); [`t08_enums/Enums04EnumMapSet`](src/t08_enums/Enums04EnumMapSet.java) (5)
- **null zamiast pustej kolekcji** → [`t27_clean_code_pitfalls/Pitfalls02CodeReview`](src/t27_clean_code_pitfalls/Pitfalls02CodeReview.java) (4); [`t14_optional/Optional03BestPractices`](src/t14_optional/Optional03BestPractices.java) (3)
- **NullPointerException** → [`t01_basics/Basics09PassByValue`](src/t01_basics/Basics09PassByValue.java) (8); [`t06_oop_basics/Oop01ClassesObjects`](src/t06_oop_basics/Oop01ClassesObjects.java) (7)
- **NumberFormat** → [`t15_numbers/Numbers04FormattingParsing`](src/t15_numbers/Numbers04FormattingParsing.java) (3, 6); [`t31_jdk_toolbox/Toolbox03I18n`](src/t31_jdk_toolbox/Toolbox03I18n.java) (2)
- **NumberFormatException** → [`t01_basics/Basics06Wrappers`](src/t01_basics/Basics06Wrappers.java) (5); [`t15_numbers/Numbers04FormattingParsing`](src/t15_numbers/Numbers04FormattingParsing.java) (8)
- **NWD (algorytm Euklidesa)** → [`t24_algorithms/Math01NumberTheory`](src/t24_algorithms/Math01NumberTheory.java) (2, 3)

### O

- **obiekt-wartość (value object)** → [`t06_oop_basics/Oop09ValueObjects`](src/t06_oop_basics/Oop09ValueObjects.java); [`t15_numbers/Numbers02MoneyValueObject`](src/t15_numbers/Numbers02MoneyValueObject.java)
- **Objects::nonNull** → [`t16_streams/Streams03FilterMap`](src/t16_streams/Streams03FilterMap.java) (8)
- **Objects.requireNonNull** → [`t10_exceptions/Exceptions07BestPractices`](src/t10_exceptions/Exceptions07BestPractices.java) (3)
- **obliczanie na skróty (&&, ||)** → [`t01_basics/Basics04Operators`](src/t01_basics/Basics04Operators.java) (5)
- **Observer (wzorzec)** → [`t22_design_patterns/Patterns06Observer`](src/t22_design_patterns/Patterns06Observer.java)
- **obsesja typów prostych (primitive obsession)** → [`t06_oop_basics/Oop09ValueObjects`](src/t06_oop_basics/Oop09ValueObjects.java) (1)
- **odśmiecanie pamięci (garbage collection)** → [`t26_jvm/Jvm03GarbageCollection`](src/t26_jvm/Jvm03GarbageCollection.java)
- **OffsetDateTime** → [`t17_datetime/DateTime04ZonesInstant`](src/t17_datetime/DateTime04ZonesInstant.java) (5)
- **ograniczenie typu (<T extends ...>)** → [`t11_generics/Generics04Bounded`](src/t11_generics/Generics04Bounded.java)
- **operator trójargumentowy ? :** → [`t01_basics/Basics04Operators`](src/t01_basics/Basics04Operators.java) (6); [`t02_controlflow/Control01IfElse`](src/t02_controlflow/Control01IfElse.java) (8)
- **operatory bitowe** → [`t01_basics/Basics04Operators`](src/t01_basics/Basics04Operators.java) (8); [`t15_numbers/Numbers05IntegerTricks`](src/t15_numbers/Numbers05IntegerTricks.java) (8)
- **Optional** → [`t14_optional/Optional01Basics`](src/t14_optional/Optional01Basics.java)
- **Optional.get (pułapka)** → [`t14_optional/Optional01Basics`](src/t14_optional/Optional01Basics.java) (5); [`t16_streams/Streams14OptionalInStreams`](src/t16_streams/Streams14OptionalInStreams.java) (9)
- **Optional.of / ofNullable / empty** → [`t14_optional/Optional01Basics`](src/t14_optional/Optional01Basics.java) (3)
- **Optional w strumieniach** → [`t16_streams/Streams14OptionalInStreams`](src/t16_streams/Streams14OptionalInStreams.java)
- **orElseThrow** → [`t14_optional/Optional01Basics`](src/t14_optional/Optional01Basics.java) (7)
- **@Override** → [`t06_oop_basics/Oop05ObjectMethods`](src/t06_oop_basics/Oop05ObjectMethods.java) (2); [`t07_inheritance_polymorphism/Inherit02Override`](src/t07_inheritance_polymorphism/Inherit02Override.java) (1); [`t19_annotations_reflection/Annotations01BuiltIn`](src/t19_annotations_reflection/Annotations01BuiltIn.java) (2)

### P

- **pakiet (package)** → [`t06_oop_basics/Oop08PackagesAccess`](src/t06_oop_basics/Oop08PackagesAccess.java) (1)
- **palindrom** → [`t04_strings/Strings07TextAlgorithms`](src/t04_strings/Strings07TextAlgorithms.java) (1); [`t33_interview_prep/Interview03CodingTasks`](src/t33_interview_prep/Interview03CodingTasks.java) (3)
- **pamięć JVM (stos, sterta)** → [`t26_jvm/Jvm01Memory`](src/t26_jvm/Jvm01Memory.java)
- **parallelStream** → [`t16_streams/Streams18Parallel`](src/t16_streams/Streams18Parallel.java)
- **@ParameterizedTest** → [`t32_junit_mockito/JUnit02Parameterized`](src/t32_junit_mockito/JUnit02Parameterized.java) (1)
- **partitioningBy** → [`t16_streams/Streams12PartitioningBy`](src/t16_streams/Streams12PartitioningBy.java)
- **Path** → [`t18_io_files/Io01PathFiles`](src/t18_io_files/Io01PathFiles.java) (1, 2, 3)
- **Pattern.quote / Matcher.quoteReplacement** → [`t04_strings/Strings05Regex`](src/t04_strings/Strings05Regex.java) (7); [`t04_strings/Strings08RegexAdvanced`](src/t04_strings/Strings08RegexAdvanced.java) (5)
- **PECS (Producer Extends, Consumer Super)** → [`t11_generics/Generics05Wildcards`](src/t11_generics/Generics05Wildcards.java) (6, 7)
- **peek** → [`t16_streams/Streams03FilterMap`](src/t16_streams/Streams03FilterMap.java) (7); [`t16_streams/Streams16Laziness`](src/t16_streams/Streams16Laziness.java) (10)
- **Period** → [`t17_datetime/DateTime02PeriodDuration`](src/t17_datetime/DateTime02PeriodDuration.java) (1, 2, 5)
- **permutacje** → [`t24_algorithms/Algorithms06Backtracking`](src/t24_algorithms/Algorithms06Backtracking.java) (2); [`t24_algorithms/Math03Combinatorics`](src/t24_algorithms/Math03Combinatorics.java) (2)
- **PESEL (cyfra kontrolna)** → [`t24_algorithms/Math02ModularChecksums`](src/t24_algorithms/Math02ModularChecksums.java) (4)
- **pętla kontra stream** → [`t16_streams/Streams01Intro`](src/t16_streams/Streams01Intro.java) (1, 9); [`t16_streams/Streams17SideEffectsPitfalls`](src/t16_streams/Streams17SideEffectsPitfalls.java) (10)
- **pętla zwrotna (localhost)** → [`t28_networking_http/Net01SocketsTcpUdp`](src/t28_networking_http/Net01SocketsTcpUdp.java) (1, 9)
- **pieniądze w groszach (long)** → [`t15_numbers/Numbers05IntegerTricks`](src/t15_numbers/Numbers05IntegerTricks.java) (10)
- **plik JAR i manifest** → [`t30_build_modules/Build02JarClasspath`](src/t30_build_modules/Build02JarClasspath.java) (3, 4)
- **podwójna dyspozycja (double dispatch)** → [`t22_design_patterns/Patterns15Visitor`](src/t22_design_patterns/Patterns15Visitor.java) (3)
- **podzapytania (IN, EXISTS)** → [`t29_jdbc_databases/Jdbc06SqlJoins`](src/t29_jdbc_databases/Jdbc06SqlJoins.java) (9)
- **pokrycie kodu testami** → [`t30_build_modules/Build06QualityToolsJavadoc`](src/t30_build_modules/Build06QualityToolsJavadoc.java) (7); [`t32_junit_mockito/JUnit05Tdd`](src/t32_junit_mockito/JUnit05Tdd.java) (7)
- **pola nie są polimorficzne (cieniowanie)** → [`t07_inheritance_polymorphism/Inherit02Override`](src/t07_inheritance_polymorphism/Inherit02Override.java) (7)
- **polimorfizm** → [`t07_inheritance_polymorphism/Inherit05Polymorphism`](src/t07_inheritance_polymorphism/Inherit05Polymorphism.java)
- **połykanie wyjątku** → [`t10_exceptions/Exceptions07BestPractices`](src/t10_exceptions/Exceptions07BestPractices.java) (1); [`t33_interview_prep/Interview04LiveCoding`](src/t33_interview_prep/Interview04LiveCoding.java) (7)
- **pom.xml** → [`t30_build_modules/Build01MavenBasics`](src/t30_build_modules/Build01MavenBasics.java) (3)
- **porównanie: == kontra equals** → [`t01_basics/Basics09PassByValue`](src/t01_basics/Basics09PassByValue.java) (7); [`t04_strings/Strings01Basics`](src/t04_strings/Strings01Basics.java) (4); [`t06_oop_basics/Oop05ObjectMethods`](src/t06_oop_basics/Oop05ObjectMethods.java) (3)
- **porównanie liczb zmiennoprzecinkowych z tolerancją** → [`t01_basics/Basics08FloatingPoint`](src/t01_basics/Basics08FloatingPoint.java) (3); [`t24_algorithms/Math05NumericalMethods`](src/t24_algorithms/Math05NumericalMethods.java) (1)
- **porównanie w stałym czasie** → [`t31_jdk_toolbox/Toolbox02HashingSecurity`](src/t31_jdk_toolbox/Toolbox02HashingSecurity.java) (5)
- **porty i adaptery (architektura heksagonalna)** → [`t22_design_patterns/Patterns11Adapter`](src/t22_design_patterns/Patterns11Adapter.java) (9); [`t27_clean_code_pitfalls/CleanCode03Architecture`](src/t27_clean_code_pitfalls/CleanCode03Architecture.java) (8)
- **potęgowanie modularne** → [`t24_algorithms/Math02ModularChecksums`](src/t24_algorithms/Math02ModularChecksums.java) (2)
- **poziomy izolacji transakcji** → [`t29_jdbc_databases/Jdbc04Transactions`](src/t29_jdbc_databases/Jdbc04Transactions.java) (6)
- **Prawo Demeter** → [`t27_clean_code_pitfalls/Pitfalls02CodeReview`](src/t27_clean_code_pitfalls/Pitfalls02CodeReview.java) (6)
- **Predicate: and / or / negate / not** → [`t16_streams/Streams03FilterMap`](src/t16_streams/Streams03FilterMap.java) (2); [`t13_lambdas/Lambda05Composition`](src/t13_lambdas/Lambda05Composition.java) (1, 3)
- **Predicate.not** → [`t23_modern_java/Modern06ApiAdditions`](src/t23_modern_java/Modern06ApiAdditions.java) (6); [`t13_lambdas/Lambda05Composition`](src/t13_lambdas/Lambda05Composition.java) (3)
- **PreparedStatement** → [`t29_jdbc_databases/Jdbc03PreparedStatement`](src/t29_jdbc_databases/Jdbc03PreparedStatement.java) (1, 8, 9)
- **@Primary / @Qualifier** → [`t34_toward_spring/Spring01IocContainer`](src/t34_toward_spring/Spring01IocContainer.java) (7)
- **printf / String.format** → [`t01_basics/Basics11ConsoleOutput`](src/t01_basics/Basics11ConsoleOutput.java) (3); [`t04_strings/Strings04Formatting`](src/t04_strings/Strings04Formatting.java); [`t04_strings/Strings09FormatterCheatsheet`](src/t04_strings/Strings09FormatterCheatsheet.java)
- **PriorityQueue** → [`t12_collections/Collections06QueuesDeques`](src/t12_collections/Collections06QueuesDeques.java) (4, 5, 6)
- **problem N+1 zapytań** → [`t29_jdbc_databases/Jdbc05Dao`](src/t29_jdbc_databases/Jdbc05Dao.java) (7)
- **problem plecakowy** → [`t24_algorithms/Algorithms07DynamicProgramming`](src/t24_algorithms/Algorithms07DynamicProgramming.java) (6)
- **procesor adnotacji (AbstractProcessor)** → [`t19_annotations_reflection/Annotations07Processors`](src/t19_annotations_reflection/Annotations07Processors.java)
- **ProcessBuilder** → [`t30_build_modules/Build05ProcessesEnv`](src/t30_build_modules/Build05ProcessesEnv.java) (1, 2)
- **ProcessHandle** → [`t30_build_modules/Build05ProcessesEnv`](src/t30_build_modules/Build05ProcessesEnv.java) (4); [`t23_modern_java/Modern06ApiAdditions`](src/t23_modern_java/Modern06ApiAdditions.java) (3)
- **producent–konsument** → [`t21_concurrency/Concurrency06ConcurrentCollections`](src/t21_concurrency/Concurrency06ConcurrentCollections.java) (8); [`t21_concurrency/Concurrency03Locks`](src/t21_concurrency/Concurrency03Locks.java) (5); [`t35_capstone/Capstone03WeatherStations`](src/t35_capstone/Capstone03WeatherStations.java) (5)
- **profile Spring** → [`t34_toward_spring/Spring04WhatSpringGives`](src/t34_toward_spring/Spring04WhatSpringGives.java) (5)
- **programowanie do interfejsu** → [`t12_collections/Collections01Overview`](src/t12_collections/Collections01Overview.java) (3)
- **programowanie dynamiczne** → [`t24_algorithms/Algorithms07DynamicProgramming`](src/t24_algorithms/Algorithms07DynamicProgramming.java)
- **projekt: raport sprzedaży z CSV** → [`t35_capstone/Capstone02SalesReport`](src/t35_capstone/Capstone02SalesReport.java)
- **projekt: REST lista zadań** → [`t35_capstone/Capstone04RestTodo`](src/t35_capstone/Capstone04RestTodo.java)
- **projekt: stacje pogodowe** → [`t35_capstone/Capstone03WeatherStations`](src/t35_capstone/Capstone03WeatherStations.java)
- **projekt: wypożyczalnia książek** → [`t35_capstone/Capstone01Library`](src/t35_capstone/Capstone01Library.java)
- **projektowanie pod testowalność** → [`t25_testing/Testing03TestableDesign`](src/t25_testing/Testing03TestableDesign.java)
- **Properties (plik .properties)** → [`t18_io_files/Io06Properties`](src/t18_io_files/Io06Properties.java)
- **przeciążanie metod (overloading)** → [`t05_methods/Methods02Overloading`](src/t05_methods/Methods02Overloading.java); [`t07_inheritance_polymorphism/Inherit02Override`](src/t07_inheritance_polymorphism/Inherit02Override.java) (4)
- **przekazywanie przez wartość (pass by value)** → [`t01_basics/Basics09PassByValue`](src/t01_basics/Basics09PassByValue.java) (3, 5)
- **przepełnienie (overflow)** → [`t01_basics/Basics05Casting`](src/t01_basics/Basics05Casting.java) (4, 5); [`t15_numbers/Numbers05IntegerTricks`](src/t15_numbers/Numbers05IntegerTricks.java) (1, 2)
- **przesłanianie nazw (shadowing)** → [`t01_basics/Basics03Variables`](src/t01_basics/Basics03Variables.java) (3); [`t06_oop_basics/Oop02Constructors`](src/t06_oop_basics/Oop02Constructors.java) (6)
- **przesuwne okno (sliding window)** → [`t24_algorithms/Algorithms05Classics`](src/t24_algorithms/Algorithms05Classics.java) (3); [`t12_collections/Collections10Patterns`](src/t12_collections/Collections10Patterns.java) (7)
- **przetwarzanie pionowe (i wyjątek: sorted)** → [`t16_streams/Streams01Intro`](src/t16_streams/Streams01Intro.java) (6); [`t16_streams/Streams16Laziness`](src/t16_streams/Streams16Laziness.java) (2, 3)
- **przypadek bazowy rekurencji** → [`t05_methods/Methods03Recursion`](src/t05_methods/Methods03Recursion.java) (6)
- **przypadki brzegowe (edge cases)** → [`t25_testing/Testing01Concepts`](src/t25_testing/Testing01Concepts.java) (7); [`t33_interview_prep/Interview04LiveCoding`](src/t33_interview_prep/Interview04LiveCoding.java) (4)
- **pula napisów (string pool)** → [`t04_strings/Strings01Basics`](src/t04_strings/Strings01Basics.java) (3); [`t26_jvm/Jvm01Memory`](src/t26_jvm/Jvm01Memory.java) (5)
- **pułapki lambd** → [`t13_lambdas/Lambda08Pitfalls`](src/t13_lambdas/Lambda08Pitfalls.java)
- **pytania rekrutacyjne o Javę** → [`t33_interview_prep/Interview01JavaQuestions`](src/t33_interview_prep/Interview01JavaQuestions.java)

### Q

- **Queue: offer / poll / peek** → [`t12_collections/Collections06QueuesDeques`](src/t12_collections/Collections06QueuesDeques.java) (1)

### R

- **ramka stosu** → [`t26_jvm/Jvm01Memory`](src/t26_jvm/Jvm01Memory.java) (3)
- **Random z ziarnem (seed)** → [`t01_basics/Basics07MathRandom`](src/t01_basics/Basics07MathRandom.java) (3)
- **RandomGenerator / HexFormat (Java 17)** → [`t23_modern_java/Modern06ApiAdditions`](src/t23_modern_java/Modern06ApiAdditions.java) (10)
- **record (rekord)** → [`t09_records/Records01Basics`](src/t09_records/Records01Basics.java)
- **reduce** → [`t16_streams/Streams07Reduce`](src/t16_streams/Streams07Reduce.java)
- **ReentrantLock** → [`t21_concurrency/Concurrency03Locks`](src/t21_concurrency/Concurrency03Locks.java) (2)
- **refaktoryzacja w IntelliJ** → [`t00_start/Start07IntelliJRefactoring`](src/t00_start/Start07IntelliJRefactoring.java)
- **referencja do konstruktora (Klasa::new)** → [`t13_lambdas/Lambda04MethodReferences`](src/t13_lambdas/Lambda04MethodReferences.java) (5)
- **referencja do metody (Klasa::metoda)** → [`t13_lambdas/Lambda04MethodReferences`](src/t13_lambdas/Lambda04MethodReferences.java)
- **referencje słabe, miękkie, fantomowe** → [`t26_jvm/Jvm03GarbageCollection`](src/t26_jvm/Jvm03GarbageCollection.java) (6)
- **refleksja (reflection)** → [`t19_annotations_reflection/Annotations03ReflectionBasics`](src/t19_annotations_reflection/Annotations03ReflectionBasics.java)
- **rekord: konstruktor kompaktowy** → [`t09_records/Records02Constructors`](src/t09_records/Records02Constructors.java) (1, 2)
- **rekord: płytka niezmienność** → [`t09_records/Records01Basics`](src/t09_records/Records01Basics.java) (6)
- **rekord jako klucz mapy** → [`t09_records/Records03Advanced`](src/t09_records/Records03Advanced.java) (4)
- **rekurencja** → [`t05_methods/Methods03Recursion`](src/t05_methods/Methods03Recursion.java)
- **removeIf** → [`t12_collections/Collections02Lists`](src/t12_collections/Collections02Lists.java) (7); [`t13_lambdas/Lambda01FromAnonymousToLambda`](src/t13_lambdas/Lambda01FromAnonymousToLambda.java) (8)
- **Rename (Shift+F6)** → [`t00_start/Start07IntelliJRefactoring`](src/t00_start/Start07IntelliJRefactoring.java) (1)
- **ResourceBundle** → [`t31_jdk_toolbox/Toolbox03I18n`](src/t31_jdk_toolbox/Toolbox03I18n.java) (4)
- **REST (zasady)** → [`t34_toward_spring/Spring03RestConcepts`](src/t34_toward_spring/Spring03RestConcepts.java)
- **ResultSet** → [`t29_jdbc_databases/Jdbc02Connection`](src/t29_jdbc_databases/Jdbc02Connection.java) (4, 5)
- **Runnable / Callable** → [`t13_lambdas/Lambda03JavaUtilFunction`](src/t13_lambdas/Lambda03JavaUtilFunction.java) (8); [`t21_concurrency/Concurrency04Executors`](src/t21_concurrency/Concurrency04Executors.java) (2)
- **rzutowanie (cast)** → [`t01_basics/Basics05Casting`](src/t01_basics/Basics05Casting.java) (1, 2)

### S

- **Scanner** → [`t01_basics/Basics10ScannerInput`](src/t01_basics/Basics10ScannerInput.java); [`t18_io_files/Io02ReadingText`](src/t18_io_files/Io02ReadingText.java) (8)
- **ScheduledExecutorService** → [`t21_concurrency/Concurrency09ScheduledForkJoin`](src/t21_concurrency/Concurrency09ScheduledForkJoin.java) (1, 2, 3)
- **sealed (klasy zapieczętowane)** → [`t07_inheritance_polymorphism/Inherit07SealedClasses`](src/t07_inheritance_polymorphism/Inherit07SealedClasses.java); [`t23_modern_java/Modern05RecordsSealedPatterns`](src/t23_modern_java/Modern05RecordsSealedPatterns.java) (4)
- **SecureRandom** → [`t31_jdk_toolbox/Toolbox02HashingSecurity`](src/t31_jdk_toolbox/Toolbox02HashingSecurity.java) (8)
- **Semaphore** → [`t21_concurrency/Concurrency07Synchronizers`](src/t21_concurrency/Concurrency07Synchronizers.java) (5)
- **serializacja (Serializable)** → [`t18_io_files/Io09Serialization`](src/t18_io_files/Io09Serialization.java)
- **serialVersionUID** → [`t18_io_files/Io09Serialization`](src/t18_io_files/Io09Serialization.java) (4)
- **Service Locator (antywzorzec)** → [`t22_design_patterns/Patterns08DependencyInjection`](src/t22_design_patterns/Patterns08DependencyInjection.java) (6)
- **ServiceLoader** → [`t30_build_modules/Build03Modules`](src/t30_build_modules/Build03Modules.java) (7)
- **short-circuit (krótkie spięcie)** → [`t16_streams/Streams01Intro`](src/t16_streams/Streams01Intro.java) (7); [`t16_streams/Streams16Laziness`](src/t16_streams/Streams16Laziness.java) (4)
- **silna enkapsulacja modułów** → [`t30_build_modules/Build03Modules`](src/t30_build_modules/Build03Modules.java) (5)
- **silnia** → [`t05_methods/Methods03Recursion`](src/t05_methods/Methods03Recursion.java) (2); [`t24_algorithms/Math03Combinatorics`](src/t24_algorithms/Math03Combinatorics.java) (1); [`t24_algorithms/Math09InterviewClassics`](src/t24_algorithms/Math09InterviewClassics.java) (1)
- **Singleton: lazy holder** → [`t22_design_patterns/Patterns04Singleton`](src/t22_design_patterns/Patterns04Singleton.java) (3); [`t26_jvm/Jvm02ClassLoadingInit`](src/t26_jvm/Jvm02ClassLoadingInit.java) (7)
- **Singleton (wzorzec)** → [`t22_design_patterns/Patterns04Singleton`](src/t22_design_patterns/Patterns04Singleton.java)
- **sito Eratostenesa** → [`t24_algorithms/Math01NumberTheory`](src/t24_algorithms/Math01NumberTheory.java) (6)
- **słowa kluczowe, literały, słowa kontekstowe** → [`t00_start/Start02Glossary`](src/t00_start/Start02Glossary.java)
- **SOLID** → [`t07_inheritance_polymorphism/Inherit08Solid`](src/t07_inheritance_polymorphism/Inherit08Solid.java); [`t27_clean_code_pitfalls/CleanCode02Solid`](src/t27_clean_code_pitfalls/CleanCode02Solid.java)
- **sortowanie bąbelkowe** → [`t24_algorithms/Algorithms02Sorting`](src/t24_algorithms/Algorithms02Sorting.java) (1); [`t03_arrays/Arrays04Algorithms`](src/t03_arrays/Arrays04Algorithms.java) (5)
- **sortowanie przez scalanie (merge sort)** → [`t24_algorithms/Algorithms02Sorting`](src/t24_algorithms/Algorithms02Sorting.java) (5)
- **split** → [`t04_strings/Strings02Methods`](src/t04_strings/Strings02Methods.java) (7); [`t04_strings/Strings05Regex`](src/t04_strings/Strings05Regex.java) (6)
- **Spring Data (zapytania z nazw metod)** → [`t34_toward_spring/Spring04WhatSpringGives`](src/t34_toward_spring/Spring04WhatSpringGives.java) (8)
- **spy** → [`t25_testing/Testing02TestDoubles`](src/t25_testing/Testing02TestDoubles.java) (6); [`t32_junit_mockito/JUnit04Mockito`](src/t32_junit_mockito/JUnit04Mockito.java) (6)
- **SQL: NULL (logika trójwartościowa)** → [`t29_jdbc_databases/Jdbc01SqlBasics`](src/t29_jdbc_databases/Jdbc01SqlBasics.java) (6)
- **SQL: podstawy** → [`t29_jdbc_databases/Jdbc01SqlBasics`](src/t29_jdbc_databases/Jdbc01SqlBasics.java)
- **SQLException** → [`t29_jdbc_databases/Jdbc02Connection`](src/t29_jdbc_databases/Jdbc02Connection.java) (8)
- **StackOverflowError** → [`t05_methods/Methods03Recursion`](src/t05_methods/Methods03Recursion.java) (6); [`t26_jvm/Jvm01Memory`](src/t26_jvm/Jvm01Memory.java) (3)
- **startery Spring Boot** → [`t34_toward_spring/Spring04WhatSpringGives`](src/t34_toward_spring/Spring04WhatSpringGives.java) (2)
- **State (wzorzec)** → [`t22_design_patterns/Patterns13State`](src/t22_design_patterns/Patterns13State.java)
- **static (pole, metoda, blok)** → [`t06_oop_basics/Oop04Static`](src/t06_oop_basics/Oop04Static.java)
- **statyczna metoda fabrykująca** → [`t06_oop_basics/Oop09ValueObjects`](src/t06_oop_basics/Oop09ValueObjects.java) (4); [`t22_design_patterns/Patterns03Factory`](src/t22_design_patterns/Patterns03Factory.java) (2)
- **statystyka opisowa (średnia, mediana, dominanta)** → [`t24_algorithms/Math06Statistics`](src/t24_algorithms/Math06Statistics.java)
- **Step Over / Step Into (F8 / F7)** → [`t00_start/Start05Debugging`](src/t00_start/Start05Debugging.java) (1, 2)
- **stos i sterta (stack / heap)** → [`t01_basics/Basics09PassByValue`](src/t01_basics/Basics09PassByValue.java); [`t26_jvm/Jvm01Memory`](src/t26_jvm/Jvm01Memory.java) (1)
- **stos wywołań (call stack)** → [`t05_methods/Methods01Basics`](src/t05_methods/Methods01Basics.java) (6); [`t10_exceptions/Exceptions01Basics`](src/t10_exceptions/Exceptions01Basics.java) (3)
- **strategia jako lambda** → [`t13_lambdas/Lambda07HigherOrderFunctions`](src/t13_lambdas/Lambda07HigherOrderFunctions.java) (4); [`t22_design_patterns/Patterns01Strategy`](src/t22_design_patterns/Patterns01Strategy.java) (4)
- **Strategy (wzorzec)** → [`t22_design_patterns/Patterns01Strategy`](src/t22_design_patterns/Patterns01Strategy.java)
- **Stream (strumień) — wprowadzenie** → [`t16_streams/Streams01Intro`](src/t16_streams/Streams01Intro.java) (1, 2, 3)
- **Stream.builder / concat** → [`t16_streams/Streams02Creation`](src/t16_streams/Streams02Creation.java) (8)
- **Stream.iterate / generate** → [`t16_streams/Streams02Creation`](src/t16_streams/Streams02Creation.java) (5, 6)
- **Stream.of / empty / ofNullable** → [`t16_streams/Streams02Creation`](src/t16_streams/Streams02Creation.java) (2)
- **String — niezmienność** → [`t04_strings/Strings01Basics`](src/t04_strings/Strings01Basics.java) (2); [`t01_basics/Basics09PassByValue`](src/t01_basics/Basics09PassByValue.java) (6)
- **StringBuilder** → [`t04_strings/Strings03StringBuilder`](src/t04_strings/Strings03StringBuilder.java) (1, 2, 3)
- **StringJoiner** → [`t04_strings/Strings03StringBuilder`](src/t04_strings/Strings03StringBuilder.java) (5)
- **stronicowanie w SQL (LIMIT / OFFSET)** → [`t29_jdbc_databases/Jdbc05Dao`](src/t29_jdbc_databases/Jdbc05Dao.java) (8); [`t29_jdbc_databases/Jdbc01SqlBasics`](src/t29_jdbc_databases/Jdbc01SqlBasics.java) (4)
- **struktury danych ręcznie** → [`t24_algorithms/Algorithms04DataStructures`](src/t24_algorithms/Algorithms04DataStructures.java)
- **strumień prymitywny (IntStream, LongStream, DoubleStream)** → [`t16_streams/Streams08PrimitiveStreams`](src/t16_streams/Streams08PrimitiveStreams.java)
- **subList (widok)** → [`t12_collections/Collections02Lists`](src/t12_collections/Collections02Lists.java) (4)
- **summaryStatistics** → [`t16_streams/Streams08PrimitiveStreams`](src/t16_streams/Streams08PrimitiveStreams.java) (5); [`t24_algorithms/Math06Statistics`](src/t24_algorithms/Math06Statistics.java) (8)
- **sumy prefiksowe** → [`t24_algorithms/Algorithms05Classics`](src/t24_algorithms/Algorithms05Classics.java) (4)
- **super** → [`t07_inheritance_polymorphism/Inherit01Basics`](src/t07_inheritance_polymorphism/Inherit01Basics.java) (3); [`t07_inheritance_polymorphism/Inherit02Override`](src/t07_inheritance_polymorphism/Inherit02Override.java) (3)
- **Supplier (leniwa wartość domyślna)** → [`t13_lambdas/Lambda07HigherOrderFunctions`](src/t13_lambdas/Lambda07HigherOrderFunctions.java) (5); [`t16_streams/Streams16Laziness`](src/t16_streams/Streams16Laziness.java) (7)
- **surowy typ (raw type)** → [`t11_generics/Generics01Why`](src/t11_generics/Generics01Why.java) (1)
- **switch** → [`t02_controlflow/Control02Switch`](src/t02_controlflow/Control02Switch.java)
- **switch: null** → [`t02_controlflow/Control02Switch`](src/t02_controlflow/Control02Switch.java) (8); [`t23_modern_java/Modern03SwitchExpressions`](src/t23_modern_java/Modern03SwitchExpressions.java) (6)
- **switch: wyczerpywalność (exhaustiveness)** → [`t02_controlflow/Control02Switch`](src/t02_controlflow/Control02Switch.java) (7); [`t23_modern_java/Modern03SwitchExpressions`](src/t23_modern_java/Modern03SwitchExpressions.java) (4)
- **switch jako wyrażenie** → [`t02_controlflow/Control02Switch`](src/t02_controlflow/Control02Switch.java) (5); [`t23_modern_java/Modern03SwitchExpressions`](src/t23_modern_java/Modern03SwitchExpressions.java) (3)
- **symbole wieloznaczne (wildcards)** → [`t11_generics/Generics05Wildcards`](src/t11_generics/Generics05Wildcards.java)
- **synchronized** → [`t21_concurrency/Concurrency02RaceConditions`](src/t21_concurrency/Concurrency02RaceConditions.java) (4, 8); [`t21_concurrency/Concurrency03Locks`](src/t21_concurrency/Concurrency03Locks.java) (1)
- **System.arraycopy** → [`t03_arrays/Arrays03Utility`](src/t03_arrays/Arrays03Utility.java) (8)
- **System.gc / finalize / Cleaner** → [`t26_jvm/Jvm03GarbageCollection`](src/t26_jvm/Jvm03GarbageCollection.java) (5)
- **System.getenv kontra System.getProperty** → [`t30_build_modules/Build05ProcessesEnv`](src/t30_build_modules/Build05ProcessesEnv.java) (6)
- **systemy liczbowe (dwójkowy, szesnastkowy)** → [`t24_algorithms/Math04NumberSystems`](src/t24_algorithms/Math04NumberSystems.java)

### Ś

- **ścieżka nauki, powtórki w odstępach (spaced repetition)** → [`t00_start/Start03LearningPath`](src/t00_start/Start03LearningPath.java); [`t00_start/Start04ReviewTracker`](src/t00_start/Start04ReviewTracker.java) (2)
- **ślad stosu (stack trace)** → [`t10_exceptions/Exceptions01Basics`](src/t10_exceptions/Exceptions01Basics.java) (5); [`t10_exceptions/Exceptions06ChainingWrapping`](src/t10_exceptions/Exceptions06ChainingWrapping.java) (3)

### T

- **tablica (array)** → [`t03_arrays/Arrays01Basics`](src/t03_arrays/Arrays01Basics.java)
- **tablica wielowymiarowa** → [`t03_arrays/Arrays02MultiDim`](src/t03_arrays/Arrays02MultiDim.java)
- **tasowanie listy (shuffle)** → [`t01_basics/Basics07MathRandom`](src/t01_basics/Basics07MathRandom.java) (6); [`t12_collections/Collections09CollectionsUtility`](src/t12_collections/Collections09CollectionsUtility.java) (2)
- **TDD (czerwony, zielony, refaktoryzacja)** → [`t32_junit_mockito/JUnit05Tdd`](src/t32_junit_mockito/JUnit05Tdd.java)
- **Tell, don't ask** → [`t06_oop_basics/Oop03Encapsulation`](src/t06_oop_basics/Oop03Encapsulation.java) (7)
- **TemporalAdjusters** → [`t17_datetime/DateTime01LocalDateTime`](src/t17_datetime/DateTime01LocalDateTime.java) (6)
- **testowanie (po co, piramida testów)** → [`t25_testing/Testing01Concepts`](src/t25_testing/Testing01Concepts.java) (1, 2)
- **thenApply / thenCompose / thenCombine** → [`t21_concurrency/Concurrency05CompletableFuture`](src/t21_concurrency/Concurrency05CompletableFuture.java) (2, 3, 4); [`t28_networking_http/Http05AsyncTimeouts`](src/t28_networking_http/Http05AsyncTimeouts.java) (3)
- **this** → [`t06_oop_basics/Oop01ClassesObjects`](src/t06_oop_basics/Oop01ClassesObjects.java) (8)
- **this(...) — łańcuch konstruktorów** → [`t06_oop_basics/Oop02Constructors`](src/t06_oop_basics/Oop02Constructors.java) (5)
- **ThreadLocal** → [`t21_concurrency/Concurrency08ThreadSafetyPatterns`](src/t21_concurrency/Concurrency08ThreadSafetyPatterns.java) (2, 3)
- **throws** → [`t10_exceptions/Exceptions02CheckedUnchecked`](src/t10_exceptions/Exceptions02CheckedUnchecked.java) (2)
- **toList() (niemodyfikowalna, Java 16+)** → [`t16_streams/Streams06TerminalOps`](src/t16_streams/Streams06TerminalOps.java) (8); [`t23_modern_java/Modern06ApiAdditions`](src/t23_modern_java/Modern06ApiAdditions.java) (9)
- **toMap: duplikat klucza** → [`t16_streams/Streams10CollectorsToMap`](src/t16_streams/Streams10CollectorsToMap.java) (2); [`t16_streams/Streams17SideEffectsPitfalls`](src/t16_streams/Streams17SideEffectsPitfalls.java) (5)
- **toString** → [`t06_oop_basics/Oop05ObjectMethods`](src/t06_oop_basics/Oop05ObjectMethods.java) (1); [`t06_oop_basics/Oop01ClassesObjects`](src/t06_oop_basics/Oop01ClassesObjects.java) (9)
- **toUpperCase / toLowerCase (Locale)** → [`t04_strings/Strings02Methods`](src/t04_strings/Strings02Methods.java) (4); [`t27_clean_code_pitfalls/Pitfalls01Classic`](src/t27_clean_code_pitfalls/Pitfalls01Classic.java) (6)
- **@Transactional (idea)** → [`t19_annotations_reflection/Annotations06DynamicProxy`](src/t19_annotations_reflection/Annotations06DynamicProxy.java) (9); [`t34_toward_spring/Spring01IocContainer`](src/t34_toward_spring/Spring01IocContainer.java) (9)
- **transakcja (commit, rollback)** → [`t29_jdbc_databases/Jdbc04Transactions`](src/t29_jdbc_databases/Jdbc04Transactions.java) (2, 3)
- **transient** → [`t18_io_files/Io09Serialization`](src/t18_io_files/Io09Serialization.java) (3)
- **transpozycja** → [`t03_arrays/Arrays02MultiDim`](src/t03_arrays/Arrays02MultiDim.java) (8); [`t24_algorithms/Math07MatricesGeometry`](src/t24_algorithms/Math07MatricesGeometry.java) (4)
- **trasowanie (routing) z adnotacji** → [`t34_toward_spring/Spring03RestConcepts`](src/t34_toward_spring/Spring03RestConcepts.java) (3)
- **TreeMap** → [`t12_collections/Collections05Maps`](src/t12_collections/Collections05Maps.java) (7)
- **TreeSet** → [`t12_collections/Collections04Sets`](src/t12_collections/Collections04Sets.java) (3)
- **try / catch (obsługa wyjątków)** → [`t10_exceptions/Exceptions01Basics`](src/t10_exceptions/Exceptions01Basics.java) (2)
- **try-with-resources** → [`t10_exceptions/Exceptions04TryWithResources`](src/t10_exceptions/Exceptions04TryWithResources.java); [`t18_io_files/Io11IoExceptions`](src/t18_io_files/Io11IoExceptions.java) (6)
- **turecka lokalizacja (Locale i toUpperCase)** → [`t27_clean_code_pitfalls/Pitfalls01Classic`](src/t27_clean_code_pitfalls/Pitfalls01Classic.java) (6)
- **Two-sum** → [`t12_collections/Collections10Patterns`](src/t12_collections/Collections10Patterns.java) (6); [`t33_interview_prep/Interview03CodingTasks`](src/t33_interview_prep/Interview03CodingTasks.java) (6)
- **typy całkowite: zakresy (byte, short, int, long)** → [`t01_basics/Basics02PrimitiveTypes`](src/t01_basics/Basics02PrimitiveTypes.java) (1, 2)

### U

- **ukrywanie metod statycznych (hiding)** → [`t07_inheritance_polymorphism/Inherit02Override`](src/t07_inheritance_polymorphism/Inherit02Override.java) (6)
- **UncheckedIOException** → [`t18_io_files/Io11IoExceptions`](src/t18_io_files/Io11IoExceptions.java) (5)
- **Unicode** → [`t04_strings/Strings06CharUnicode`](src/t04_strings/Strings06CharUnicode.java); [`t18_io_files/Io12Charsets`](src/t18_io_files/Io12Charsets.java) (1)
- **upcasting i downcasting** → [`t07_inheritance_polymorphism/Inherit05Polymorphism`](src/t07_inheritance_polymorphism/Inherit05Polymorphism.java) (3, 4)
- **URI (anatomia adresu)** → [`t28_networking_http/Http01UriUrl`](src/t28_networking_http/Http01UriUrl.java) (1, 2)
- **URI kontra URL** → [`t28_networking_http/Http01UriUrl`](src/t28_networking_http/Http01UriUrl.java) (5)
- **usuwanie ogonków** → [`t04_strings/Strings06CharUnicode`](src/t04_strings/Strings06CharUnicode.java) (7); [`t31_jdk_toolbox/Toolbox03I18n`](src/t31_jdk_toolbox/Toolbox03I18n.java) (8)
- **UTF-8** → [`t18_io_files/Io12Charsets`](src/t18_io_files/Io12Charsets.java) (1, 2, 9); [`t23_modern_java/Modern07WhatsNextJava21`](src/t23_modern_java/Modern07WhatsNextJava21.java) (2)
- **UUID** → [`t31_jdk_toolbox/Toolbox01UuidBase64`](src/t31_jdk_toolbox/Toolbox01UuidBase64.java) (1, 2, 3)

### V

- **values / name / ordinal** → [`t08_enums/Enums01Basics`](src/t08_enums/Enums01Basics.java) (2)
- **var (typ odgadnięty przez kompilator)** → [`t01_basics/Basics03Variables`](src/t01_basics/Basics03Variables.java) (6); [`t23_modern_java/Modern02Var`](src/t23_modern_java/Modern02Var.java)
- **varargs (zmienna liczba argumentów)** → [`t03_arrays/Arrays05Varargs`](src/t03_arrays/Arrays05Varargs.java)
- **Visitor (wzorzec)** → [`t22_design_patterns/Patterns15Visitor`](src/t22_design_patterns/Patterns15Visitor.java)
- **volatile** → [`t21_concurrency/Concurrency10MemoryModel`](src/t21_concurrency/Concurrency10MemoryModel.java) (3, 4)

### W

- **walidator oparty na adnotacjach** → [`t19_annotations_reflection/Annotations04Validator`](src/t19_annotations_reflection/Annotations04Validator.java)
- **walidatory (kod pocztowy, PESEL, NIP, IBAN)** → [`t04_strings/Strings08RegexAdvanced`](src/t04_strings/Strings08RegexAdvanced.java) (9); [`t24_algorithms/Math02ModularChecksums`](src/t24_algorithms/Math02ModularChecksums.java) (4, 5, 6)
- **walkFileTree / SimpleFileVisitor** → [`t18_io_files/Io07WalkingDirectories`](src/t18_io_files/Io07WalkingDirectories.java) (7); [`t22_design_patterns/Patterns15Visitor`](src/t22_design_patterns/Patterns15Visitor.java) (7)
- **wariancja i odchylenie standardowe** → [`t24_algorithms/Math06Statistics`](src/t24_algorithms/Math06Statistics.java) (5)
- **warstwy: kontroler, serwis, repozytorium** → [`t34_toward_spring/Spring02Layers`](src/t34_toward_spring/Spring02Layers.java)
- **wartości domyślne pól** → [`t01_basics/Basics02PrimitiveTypes`](src/t01_basics/Basics02PrimitiveTypes.java) (6); [`t06_oop_basics/Oop01ClassesObjects`](src/t06_oop_basics/Oop01ClassesObjects.java) (2); [`t03_arrays/Arrays01Basics`](src/t03_arrays/Arrays01Basics.java) (3)
- **wątek (Thread)** → [`t21_concurrency/Concurrency01Threads`](src/t21_concurrency/Concurrency01Threads.java)
- **wątki wirtualne (Java 21)** → [`t23_modern_java/Modern07WhatsNextJava21`](src/t23_modern_java/Modern07WhatsNextJava21.java) (5)
- **WeakHashMap** → [`t26_jvm/Jvm03GarbageCollection`](src/t26_jvm/Jvm03GarbageCollection.java) (7)
- **wersjonowanie API** → [`t28_networking_http/Http04JsonApi`](src/t28_networking_http/Http04JsonApi.java) (8); [`t34_toward_spring/Spring03RestConcepts`](src/t34_toward_spring/Spring03RestConcepts.java) (9)
- **while** → [`t02_controlflow/Control03Loops`](src/t02_controlflow/Control03Loops.java) (3)
- **widok kontra kopia** → [`t12_collections/Collections02Lists`](src/t12_collections/Collections02Lists.java) (4); [`t12_collections/Collections08ImmutableUnmodifiable`](src/t12_collections/Collections08ImmutableUnmodifiable.java) (2); [`t06_oop_basics/Oop10Copying`](src/t06_oop_basics/Oop10Copying.java) (6)
- **withRetry (ponawianie)** → [`t13_lambdas/Lambda07HigherOrderFunctions`](src/t13_lambdas/Lambda07HigherOrderFunctions.java) (9); [`t18_io_files/Io11IoExceptions`](src/t18_io_files/Io11IoExceptions.java) (9); [`t28_networking_http/Http05AsyncTimeouts`](src/t28_networking_http/Http05AsyncTimeouts.java) (7)
- **własna adnotacja** → [`t19_annotations_reflection/Annotations02Custom`](src/t19_annotations_reflection/Annotations02Custom.java) (1, 2)
- **własny wyjątek** → [`t10_exceptions/Exceptions05CustomExceptions`](src/t10_exceptions/Exceptions05CustomExceptions.java)
- **wnioskowanie typu** → [`t11_generics/Generics03Methods`](src/t11_generics/Generics03Methods.java) (2)
- **współczynnik wypełnienia 0.75 i resize** → [`t12_collections/Collections11HashingInternals`](src/t12_collections/Collections11HashingInternals.java) (5)
- **wstrzykiwanie przez konstruktor** → [`t34_toward_spring/Spring01IocContainer`](src/t34_toward_spring/Spring01IocContainer.java) (3); [`t22_design_patterns/Patterns08DependencyInjection`](src/t22_design_patterns/Patterns08DependencyInjection.java) (2)
- **wyciek pamięci** → [`t26_jvm/Jvm01Memory`](src/t26_jvm/Jvm01Memory.java) (7, 8)
- **wyciek wnętrza przez getter** → [`t06_oop_basics/Oop03Encapsulation`](src/t06_oop_basics/Oop03Encapsulation.java) (8)
- **wyjątek: co jest w obiekcie (getMessage, stos)** → [`t10_exceptions/Exceptions01Basics`](src/t10_exceptions/Exceptions01Basics.java) (5)
- **wyjątki sprawdzane w lambdach** → [`t10_exceptions/Exceptions02CheckedUnchecked`](src/t10_exceptions/Exceptions02CheckedUnchecked.java) (4); [`t13_lambdas/Lambda08Pitfalls`](src/t13_lambdas/Lambda08Pitfalls.java) (1, 2)
- **wyjątki stłumione (suppressed)** → [`t10_exceptions/Exceptions04TryWithResources`](src/t10_exceptions/Exceptions04TryWithResources.java) (4); [`t18_io_files/Io11IoExceptions`](src/t18_io_files/Io11IoExceptions.java) (7)
- **wymazywanie typów (type erasure)** → [`t11_generics/Generics06ErasureLimits`](src/t11_generics/Generics06ErasureLimits.java)
- **wyrażenia regularne (regex)** → [`t04_strings/Strings05Regex`](src/t04_strings/Strings05Regex.java); [`t04_strings/Strings08RegexAdvanced`](src/t04_strings/Strings08RegexAdvanced.java)
- **wyszukiwanie binarne** → [`t24_algorithms/Algorithms03Searching`](src/t24_algorithms/Algorithms03Searching.java) (2, 4); [`t03_arrays/Arrays04Algorithms`](src/t03_arrays/Arrays04Algorithms.java) (4)
- **wyszukiwanie binarne po odpowiedzi** → [`t24_algorithms/Algorithms03Searching`](src/t24_algorithms/Algorithms03Searching.java) (6, 7)
- **wyścig (race condition)** → [`t21_concurrency/Concurrency02RaceConditions`](src/t21_concurrency/Concurrency02RaceConditions.java)
- **wywołanie wewnętrzne omija proxy (samowywołanie)** → [`t19_annotations_reflection/Annotations06DynamicProxy`](src/t19_annotations_reflection/Annotations06DynamicProxy.java) (8); [`t34_toward_spring/Spring01IocContainer`](src/t34_toward_spring/Spring01IocContainer.java) (9)
- **wzorce pętli (suma, minimum, zliczanie)** → [`t02_controlflow/Control05LoopPatterns`](src/t02_controlflow/Control05LoopPatterns.java)

### X

- **XML: DOM, StAX, XPath** → [`t18_io_files/Io14Xml`](src/t18_io_files/Io14Xml.java)
- **XXE (bezpieczne parsowanie XML)** → [`t18_io_files/Io14Xml`](src/t18_io_files/Io14Xml.java) (2)

### Y

- **yield** → [`t02_controlflow/Control02Switch`](src/t02_controlflow/Control02Switch.java) (6); [`t23_modern_java/Modern03SwitchExpressions`](src/t23_modern_java/Modern03SwitchExpressions.java) (3)

### Z

- **zadania programistyczne z rozmów** → [`t33_interview_prep/Interview03CodingTasks`](src/t33_interview_prep/Interview03CodingTasks.java)
- **zakleszczenie (deadlock)** → [`t21_concurrency/Concurrency03Locks`](src/t21_concurrency/Concurrency03Locks.java) (9); [`t26_jvm/Jvm04ToolsProfiling`](src/t26_jvm/Jvm04ToolsProfiling.java) (4)
- **zanieczyszczenie sterty (heap pollution)** → [`t11_generics/Generics06ErasureLimits`](src/t11_generics/Generics06ErasureLimits.java) (6)
- **zasada podstawienia Liskov (LSP)** → [`t07_inheritance_polymorphism/Inherit08Solid`](src/t07_inheritance_polymorphism/Inherit08Solid.java) (3); [`t07_inheritance_polymorphism/Inherit06CompositionVsInheritance`](src/t07_inheritance_polymorphism/Inherit06CompositionVsInheritance.java) (5); [`t27_clean_code_pitfalls/CleanCode02Solid`](src/t27_clean_code_pitfalls/CleanCode02Solid.java) (3)
- **zasięg zmiennej** → [`t01_basics/Basics03Variables`](src/t01_basics/Basics03Variables.java) (2); [`t02_controlflow/Control03Loops`](src/t02_controlflow/Control03Loops.java) (7)
- **zasięgi: singleton i prototyp** → [`t34_toward_spring/Spring01IocContainer`](src/t34_toward_spring/Spring01IocContainer.java) (5)
- **zbalansowane nawiasy** → [`t33_interview_prep/Interview03CodingTasks`](src/t33_interview_prep/Interview03CodingTasks.java) (7)
- **ZIP (ZipOutputStream, ZipFile)** → [`t18_io_files/Io13ZipArchives`](src/t18_io_files/Io13ZipArchives.java) (1, 2, 3)
- **zip slip** → [`t18_io_files/Io13ZipArchives`](src/t18_io_files/Io13ZipArchives.java) (7)
- **złożoność kolekcji (Big-O)** → [`t12_collections/Collections13Performance`](src/t12_collections/Collections13Performance.java) (1, 2)
- **złożoność obliczeniowa (notacja O)** → [`t24_algorithms/Algorithms01Complexity`](src/t24_algorithms/Algorithms01Complexity.java)
- **zmiana czasu (DST)** → [`t17_datetime/DateTime04ZonesInstant`](src/t17_datetime/DateTime04ZonesInstant.java) (4)
- **zmienna lokalna (deklaracja i przypisanie)** → [`t01_basics/Basics03Variables`](src/t01_basics/Basics03Variables.java) (1)
- **zmienne pole w hashCode** → [`t06_oop_basics/Oop05ObjectMethods`](src/t06_oop_basics/Oop05ObjectMethods.java) (9); [`t26_jvm/Jvm01Memory`](src/t26_jvm/Jvm01Memory.java) (8)
- **zmiennoprzecinkowe: 0.1 + 0.2 ≠ 0.3** → [`t01_basics/Basics08FloatingPoint`](src/t01_basics/Basics08FloatingPoint.java) (1, 2)
- **ZoneId / ZonedDateTime** → [`t17_datetime/DateTime04ZonesInstant`](src/t17_datetime/DateTime04ZonesInstant.java) (2, 3)
- **zrzut sterty (heap dump)** → [`t26_jvm/Jvm04ToolsProfiling`](src/t26_jvm/Jvm04ToolsProfiling.java) (5)
- **zrzut wątków (thread dump)** → [`t26_jvm/Jvm04ToolsProfiling`](src/t26_jvm/Jvm04ToolsProfiling.java) (3)

### Ź

- **źródła strumieni (kolekcje, tablice, pliki)** → [`t16_streams/Streams02Creation`](src/t16_streams/Streams02Creation.java) (1, 3, 9)

### Ż

- **żądania asynchroniczne i równoległe (sendAsync)** → [`t28_networking_http/Http05AsyncTimeouts`](src/t28_networking_http/Http05AsyncTimeouts.java) (1, 2)

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

## Licencja
Kurs jest udostępniony na licencji **MIT** (plik [`LICENSE`](LICENSE)): możesz go swobodnie kopiować, przerabiać i używać
do nauki — także we własnych projektach. Jedyny warunek: zachowaj informację o licencji. Kod powstał w dużej części
z pomocą AI (Claude) i był sprawdzany weryfikatorem kursu; mimo to traktuj go jako materiał do nauki, bez gwarancji.
