# JavaLearning — kurs Javy po polsku do nauki i powtórek

Samodzielny kurs Javy SE 17 w 29 działach (`t00`–`t28`). Każda lekcja to plik `.java`, który możesz uruchomić. Ma szczegółowe komentarze po polsku, a przy każdej angielskiej nazwie jest tłumaczenie. Każda lekcja zawiera też:
- ćwiczenia sprawdzane automatycznie,
- pytania kontrolne,
- ściągę do powtórek.

> **Zacznij tutaj:** [`t00_start/Start01HowToUse.java`](src/t00_start/Start01HowToUse.java). Tam jest opis budowy lekcji, tagów, wyszukiwania i uruchamiania.

## Szybki start
1. Zainstaluj **JDK 17** (albo nowszy).
2. Sklonuj repozytorium i otwórz folder projektu w IntelliJ IDEA (File → Open → wskaż `pom.xml` → Open as Project).
3. Otwórz dowolną lekcję, np. `src/t01_basics/Basics01HelloJvm.java`, i kliknij zielony trójkąt ▶ obok `main`.

Maven jest opcjonalny. IntelliJ pobierze zależności sam (tylko Lombok, potrzebny wyłącznie w `t20_lombok`). Z linii poleceń: `mvn compile`.

## Struktura
```
src/
├── helpers/          wspólne narzędzia do lekcji (Console, Check, SampleData, model/)
├── t00_start/        jak korzystać z kursu, słowniczek, ścieżka nauki, dziennik powtórek
├── t01_basics/       podstawy
├── ...
└── t28_capstone/     mini-projekty łączące tematy
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
| `t00_start` | Jak korzystać z kursu, słowniczek, ścieżka nauki, dziennik powtórek | ✅ |
| `t01_basics` | Podstawy (11 lekcji): jak działa program, typy proste, zmienne, operatory, rzutowanie i przepełnienie, klasy opakowujące, Math i liczby losowe, pułapki double, referencje i przekazywanie przez wartość, Scanner, wypisywanie i printf | ✅ |
| `t02_controlflow` | Sterowanie (5 lekcji): if/else i klauzule strażnika, switch (klasyczny i wyrażenie), pętle, break/continue/etykiety, wzorce pętli | ✅ |
| `t03_arrays` | Tablice (5 lekcji): podstawy, tablice wielowymiarowe, klasa `Arrays`, algorytmy pisane ręcznie, varargs | ✅ |
| `t04_strings` | Napisy (7 lekcji): niezmienność i pula, == kontra equals, metody String, StringBuilder/StringJoiner, formatowanie i bloki tekstu, wyrażenia regularne, char i Unicode (polskie litery, emoji), algorytmy na tekście | ✅ |
| `t05_methods` | Metody (4 lekcje): budowa i stos wywołań, przeciążanie i wybór wersji, rekurencja (memoizacja, StackOverflowError), dobre praktyki | ✅ |
| `t06_oop_basics` | Obiektowość (9 lekcji): klasy i obiekty, konstruktory i kolejność inicjalizacji, hermetyzacja, static, `toString`/`equals`/`hashCode`, niezmienność i kopie obronne, klasy zagnieżdżone, pakiety i modyfikatory dostępu, obiekty wartości | ✅ |
| `t07_inheritance_polymorphism` | Dziedziczenie, klasy abstrakcyjne, interfejsy, polimorfizm, sealed, SOLID | ⏳ |
| `t08_enums` | Enumy (4 lekcje): podstawy (values, valueOf, ordinal, switch), pola/konstruktor/metody i wyszukiwanie po kodzie, zachowanie stałych (ciała, lambdy, interfejs, singleton), EnumSet/EnumMap i maszyna stanów | ✅ |
| `t09_records` | Rekordy (3 lekcje): co generuje record, płytka niezmienność, konstruktor kompaktowy (walidacja, normalizacja, kopie obronne), fabryki i „withery”, rekordy generyczne i lokalne, Comparable, klucze map, sealed + instanceof | ✅ |
| `t10_exceptions` | Wyjątki (7 lekcji): try/catch/finally i stos wywołań, checked kontra unchecked i throws, kilka catch i multi-catch, try-with-resources i wyjątki stłumione, własne wyjątki z danymi, łańcuch przyczyn i opakowywanie, dobre praktyki i antywzorce | ✅ |
| `t11_generics` | Typy generyczne (7 lekcji): po co generyki (surowe typy, remove(int)), własne klasy i interfejsy, metody generyczne i wnioskowanie, ograniczenia (extends, &), dżokery ? extends / ? super i PECS, wymazywanie typów i obejścia, generyczne repozytorium | ✅ |
| `t12_collections` | Kolekcje | ⏳ |
| `t13_lambdas` | Lambdy: od klasy anonimowej do lambdy, interfejsy funkcyjne, `java.util.function`, referencje do metod, składanie funkcji, domknięcia, funkcje wyższego rzędu, pułapki (8 lekcji) | ✅ |
| `t14_optional` | Optional: podstawy, przekształcanie (map/flatMap/filter/or), dobre praktyki (3 lekcje) | ✅ |
| `t15_numbers` | BigDecimal, kwota jako obiekt wartości (VAT, raty), BigInteger, formatowanie i parsowanie liczb, sztuczki na liczbach całkowitych (5 lekcji) | ✅ |
| `t16_streams` | **Streamy**, 20 lekcji: wprowadzenie, tworzenie, filter/map, flatMap, sortowanie/distinct/limit, operacje końcowe, reduce, strumienie liczbowe, kolektory, toMap, groupingBy, partitioningBy, zaawansowane kolektory, Optional w streamach, pieniądze (BigDecimal), leniwość, efekty uboczne i pułapki, strumienie równoległe, 39 przepisów, 23 ćwiczenia | ✅ |
| `t17_datetime` | java.time | ⏳ |
| `t18_io_files` | Pliki, CSV, JSON, Properties, serializacja | ⏳ |
| `t19_annotations_reflection` | Adnotacje i refleksja | ⏳ |
| `t20_lombok` | Lombok | ⏳ |
| `t21_concurrency` | Wątki, locki, ExecutorService, CompletableFuture | ⏳ |
| `t22_design_patterns` | Wzorce projektowe | ⏳ |
| `t23_modern_java` | Nowości Java 8→17 (i zapowiedź 21) | ⏳ |
| `t24_algorithms` | Złożoność, sortowanie, wyszukiwanie, struktury danych | ⏳ |
| `t25_testing` | Testowanie bez frameworka | ⏳ |
| `t26_jvm` | Pamięć, ładowanie klas, GC | ⏳ |
| `t27_clean_code_pitfalls` | Pułapki, code review, czysty kod, SOLID | ⏳ |
| `t28_capstone` | Mini-projekty łączące tematy | ⏳ |

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
- **Wymagania:** tylko Java 17. Lekcje nie korzystają z bibliotek zewnętrznych; wyjątkiem jest Lombok w `t20_lombok`.
