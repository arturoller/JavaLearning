# Spis treści kursu JavaLearning

Wszystkie działy i lekcje z linkami. Plik jest generowany przez `tools/TableOfContents.java` — nie edytuj go ręcznie. Opisy działów ze stanem prac: [README](README.md#spis-treści), hasła od A do Z: [indeks](README.md#indeks-haseł-az).

Działów: 36 · lekcji: 264

- [helpers](src/helpers) — wspólne narzędzia lekcji (`Console`, `Check`, `TempDir`) i dane przykładowe (`SampleData`, `model/`)
- [t00_start](#t00_start) — Start (8)
- [t01_basics](#t01_basics) — Podstawy (11)
- [t02_controlflow](#t02_controlflow) — Sterowanie (5)
- [t03_arrays](#t03_arrays) — Tablice (5)
- [t04_strings](#t04_strings) — Napisy (9)
- [t05_methods](#t05_methods) — Metody (4)
- [t06_oop_basics](#t06_oop_basics) — Obiektowość (10)
- [t07_inheritance_polymorphism](#t07_inheritance_polymorphism) — Dziedziczenie (8)
- [t08_enums](#t08_enums) — Enumy (4)
- [t09_records](#t09_records) — Rekordy (3)
- [t10_exceptions](#t10_exceptions) — Wyjątki (7)
- [t11_generics](#t11_generics) — Typy generyczne (7)
- [t12_collections](#t12_collections) — Kolekcje (13)
- [t13_lambdas](#t13_lambdas) — Lambdy (8)
- [t14_optional](#t14_optional) — Optional (3)
- [t15_numbers](#t15_numbers) — Liczby (6)
- [t16_streams](#t16_streams) — Streamy (20)
- [t17_datetime](#t17_datetime) — Data i czas (6)
- [t18_io_files](#t18_io_files) — Pliki (14)
- [t19_annotations_reflection](#t19_annotations_reflection) — Adnotacje i refleksja (7)
- [t20_lombok](#t20_lombok) — Lombok (4)
- [t21_concurrency](#t21_concurrency) — Współbieżność (10)
- [t22_design_patterns](#t22_design_patterns) — Wzorce projektowe (15)
- [t23_modern_java](#t23_modern_java) — Nowości Java 8→17 i zapowiedź 21+ (7)
- [t24_algorithms](#t24_algorithms) — Algorytmy i matematyka (18)
- [t25_testing](#t25_testing) — Testowanie — pojęcia bez frameworka (3)
- [t26_jvm](#t26_jvm) — JVM (4)
- [t27_clean_code_pitfalls](#t27_clean_code_pitfalls) — Pułapki i czysty kod (5)
- [t28_networking_http](#t28_networking_http) — Sieć i HTTP (6)
- [t29_jdbc_databases](#t29_jdbc_databases) — Bazy danych (7)
- [t30_build_modules](#t30_build_modules) — Budowanie i uruchamianie (6)
- [t31_jdk_toolbox](#t31_jdk_toolbox) — Przydatne narzędzia JDK (4)
- [t32_junit_mockito](#t32_junit_mockito) — Testy w praktyce (5)
- [t33_interview_prep](#t33_interview_prep) — Rozmowa kwalifikacyjna (4)
- [t34_toward_spring](#t34_toward_spring) — Most do Springa (4)
- [t35_capstone](#t35_capstone) — Mini-projekty łączące cały kurs (4)

## t00_start

Start · [folder z lekcjami](src/t00_start)

1. [Start01HowToUse](src/t00_start/Start01HowToUse.java) — Jak korzystać z kursu JavaLearning
2. [Start02Glossary](src/t00_start/Start02Glossary.java) — Słowniczek angielsko-polski dla programisty Javy
3. [Start03LearningPath](src/t00_start/Start03LearningPath.java) — Ścieżka nauki i plan powtórek
4. [Start04ReviewTracker](src/t00_start/Start04ReviewTracker.java) — Dziennik powtórek — co mam dziś powtórzyć?
5. [Start05Debugging](src/t00_start/Start05Debugging.java) — Debugowanie w IntelliJ — breakpointy, krokowanie, podgląd zmiennych, warunki, wyjątki
6. [Start06Git](src/t00_start/Start06Git.java) — Git od zera — historia projektu, gałęzie, scalanie, praca ze zdalnym repozytorium
7. [Start07IntelliJRefactoring](src/t00_start/Start07IntelliJRefactoring.java) — Refaktoryzacje w IntelliJ IDEA — zmiany kodu wykonane przez narzędzie, nie ręcznie
8. [Start08JShell](src/t00_start/Start08JShell.java) — JShell — Java w trybie „wpisz i zobacz wynik”, bez klasy i bez metody main

## t01_basics

Podstawy · [folder z lekcjami](src/t01_basics)

1. [Basics01HelloJvm](src/t01_basics/Basics01HelloJvm.java) — Jak powstaje i uruchamia się program w Javie
2. [Basics02PrimitiveTypes](src/t01_basics/Basics02PrimitiveTypes.java) — Typy proste (prymitywne) — osiem „cegiełek” danych w Javie
3. [Basics03Variables](src/t01_basics/Basics03Variables.java) — Zmienne — deklaracja, przypisanie, zasięg, final, stałe i var
4. [Basics04Operators](src/t01_basics/Basics04Operators.java) — Operatory — arytmetyczne, przypisania, porównania, logiczne, trójargumentowy, bitowe
5. [Basics05Casting](src/t01_basics/Basics05Casting.java) — Rzutowanie typów i przepełnienie
6. [Basics06Wrappers](src/t01_basics/Basics06Wrappers.java) — Klasy opakowujące (Integer, Double...) i autoboxing
7. [Basics07MathRandom](src/t01_basics/Basics07MathRandom.java) — Klasa Math i liczby losowe (Random, ThreadLocalRandom)
8. [Basics08FloatingPoint](src/t01_basics/Basics08FloatingPoint.java) — Pułapki liczb zmiennoprzecinkowych (double, float)
9. [Basics09PassByValue](src/t01_basics/Basics09PassByValue.java) — Stos, sterta, referencje i przekazywanie przez wartość
10. [Basics10ScannerInput](src/t01_basics/Basics10ScannerInput.java) — Wczytywanie danych — klasa Scanner
11. [Basics11ConsoleOutput](src/t01_basics/Basics11ConsoleOutput.java) — Wypisywanie na konsolę — print, println, printf, formatowanie, znaki specjalne

## t02_controlflow

Sterowanie · [folder z lekcjami](src/t02_controlflow)

1. [Control01IfElse](src/t02_controlflow/Control01IfElse.java) — Instrukcja if / else if / else — podejmowanie decyzji
2. [Control02Switch](src/t02_controlflow/Control02Switch.java) — switch — wybór jednej z wielu ścieżek
3. [Control03Loops](src/t02_controlflow/Control03Loops.java) — Pętle — for, while, do-while, for-each
4. [Control04BreakContinueLabels](src/t02_controlflow/Control04BreakContinueLabels.java) — break, continue i etykiety — sterowanie wnętrzem pętli
5. [Control05LoopPatterns](src/t02_controlflow/Control05LoopPatterns.java) — Wzorce pętli — gotowe „przepisy”, które wracają w każdym programie

## t03_arrays

Tablice · [folder z lekcjami](src/t03_arrays)

1. [Arrays01Basics](src/t03_arrays/Arrays01Basics.java) — Tablice — podstawy
2. [Arrays02MultiDim](src/t03_arrays/Arrays02MultiDim.java) — Tablice wielowymiarowe — tablice tablic
3. [Arrays03Utility](src/t03_arrays/Arrays03Utility.java) — Klasa java.util.Arrays i System.arraycopy — gotowe narzędzia do tablic
4. [Arrays04Algorithms](src/t03_arrays/Arrays04Algorithms.java) — Algorytmy na tablicach pisane ręcznie
5. [Arrays05Varargs](src/t03_arrays/Arrays05Varargs.java) — Varargs — metody ze zmienną liczbą argumentów

## t04_strings

Napisy · [folder z lekcjami](src/t04_strings)

1. [Strings01Basics](src/t04_strings/Strings01Basics.java) — String — niezmienność, pula napisów, == kontra equals
2. [Strings02Methods](src/t04_strings/Strings02Methods.java) — Najważniejsze metody klasy String
3. [Strings03StringBuilder](src/t04_strings/Strings03StringBuilder.java) — StringBuilder i StringJoiner — wydajne składanie napisów
4. [Strings04Formatting](src/t04_strings/Strings04Formatting.java) — Formatowanie napisów — String.format, formatted, flagi, Locale, bloki tekstu
5. [Strings05Regex](src/t04_strings/Strings05Regex.java) — Wyrażenia regularne (regex) — wzorce opisujące tekst
6. [Strings06CharUnicode](src/t04_strings/Strings06CharUnicode.java) — Znaki (char), klasa Character, Unicode i polskie litery
7. [Strings07TextAlgorithms](src/t04_strings/Strings07TextAlgorithms.java) — Algorytmy na tekście — palindromy, anagramy, słowa, częstość liter
8. [Strings08RegexAdvanced](src/t04_strings/Strings08RegexAdvanced.java) — Wyrażenia regularne dla zaawansowanych — grupy nazwane, lookaround, flagi, Unicode, pułapki
9. [Strings09FormatterCheatsheet](src/t04_strings/Strings09FormatterCheatsheet.java) — Ściąga z java.util.Formatter — wszystkie konwersje, flagi, szerokość, precyzja, daty

## t05_methods

Metody · [folder z lekcjami](src/t05_methods)

1. [Methods01Basics](src/t05_methods/Methods01Basics.java) — Metody — budowa, parametry, zwracanie wyniku
2. [Methods02Overloading](src/t05_methods/Methods02Overloading.java) — Przeciążanie metod — ta sama nazwa, różne parametry
3. [Methods03Recursion](src/t05_methods/Methods03Recursion.java) — Rekurencja — metoda, która wywołuje samą siebie
4. [Methods04GoodPractices](src/t05_methods/Methods04GoodPractices.java) — Dobre praktyki pisania metod

## t06_oop_basics

Obiektowość · [folder z lekcjami](src/t06_oop_basics)

1. [Oop01ClassesObjects](src/t06_oop_basics/Oop01ClassesObjects.java) — Klasy i obiekty — plan i egzemplarze
2. [Oop02Constructors](src/t06_oop_basics/Oop02Constructors.java) — Konstruktory — jak obiekt dostaje stan startowy
3. [Oop03Encapsulation](src/t06_oop_basics/Oop03Encapsulation.java) — Hermetyzacja — pola prywatne, gettery, settery i niezmienniki
4. [Oop04Static](src/t06_oop_basics/Oop04Static.java) — static — składowe należące do KLASY, a nie do obiektu
5. [Oop05ObjectMethods](src/t06_oop_basics/Oop05ObjectMethods.java) — toString, equals, hashCode — trzy metody, które KAŻDA klasa dostaje od klasy Object
6. [Oop06Immutability](src/t06_oop_basics/Oop06Immutability.java) — Niezmienność — obiekty, których po utworzeniu NIE DA SIĘ zmienić
7. [Oop07NestedClasses](src/t06_oop_basics/Oop07NestedClasses.java) — Klasy zagnieżdżone — static nested, inner, lokalne i anonimowe
8. [Oop08PackagesAccess](src/t06_oop_basics/Oop08PackagesAccess.java) — Pakiety i modyfikatory dostępu — kto widzi moją klasę, pole, metodę?
9. [Oop09ValueObjects](src/t06_oop_basics/Oop09ValueObjects.java) — Obiekty-wartości (value objects) — małe, niezmienne klasy zamiast „gołych” String i double
10. [Oop10Copying](src/t06_oop_basics/Oop10Copying.java) — Kopiowanie obiektów — kopia płytka i głęboka, konstruktor kopiujący, clone()

## t07_inheritance_polymorphism

Dziedziczenie · [folder z lekcjami](src/t07_inheritance_polymorphism)

1. [Inherit01Basics](src/t07_inheritance_polymorphism/Inherit01Basics.java) — Dziedziczenie klas — extends, "is-a", co dziedziczymy a czego nie
2. [Inherit02Override](src/t07_inheritance_polymorphism/Inherit02Override.java) — Nadpisywanie metod — @Override, reguły, klasyczne pułapki
3. [Inherit03AbstractClasses](src/t07_inheritance_polymorphism/Inherit03AbstractClasses.java) — Klasy i metody abstrakcyjne — wspólny szkielet, różne szczegóły
4. [Inherit04Interfaces](src/t07_inheritance_polymorphism/Inherit04Interfaces.java) — Interfejsy — kontrakt zachowania, metody default/static/private
5. [Inherit05Polymorphism](src/t07_inheritance_polymorphism/Inherit05Polymorphism.java) — Polimorfizm — typ zadeklarowany kontra rzeczywisty, upcasting i downcasting
6. [Inherit06CompositionVsInheritance](src/t07_inheritance_polymorphism/Inherit06CompositionVsInheritance.java) — Kompozycja kontra dziedziczenie — "has-a" kontra "is-a", krucha klasa bazowa
7. [Inherit07SealedClasses](src/t07_inheritance_polymorphism/Inherit07SealedClasses.java) — Klasy zapieczętowane (sealed) — zamknięty, wyliczalny zbiór podtypów (Java 17)
8. [Inherit08Solid](src/t07_inheritance_polymorphism/Inherit08Solid.java) — SOLID — pięć zasad projektowania klas, na małych przykładach PRZED/PO

## t08_enums

Enumy · [folder z lekcjami](src/t08_enums)

1. [Enums01Basics](src/t08_enums/Enums01Basics.java) — Enum — podstawy typu wyliczeniowego
2. [Enums02FieldsMethods](src/t08_enums/Enums02FieldsMethods.java) — Enum z polami, konstruktorem i metodami
3. [Enums03ConstantBodies](src/t08_enums/Enums03ConstantBodies.java) — Własne zachowanie każdej stałej, enum z lambdą, enum implementujący interfejs, enum jako singleton
4. [Enums04EnumMapSet](src/t08_enums/Enums04EnumMapSet.java) — EnumSet i EnumMap — kolekcje dla enumów; maszyna stanów

## t09_records

Rekordy · [folder z lekcjami](src/t09_records)

1. [Records01Basics](src/t09_records/Records01Basics.java) — Record — podstawy (Java 16+)
2. [Records02Constructors](src/t09_records/Records02Constructors.java) — Konstruktory rekordów — walidacja, normalizacja, kopie obronne, fabryki i „withery”
3. [Records03Advanced](src/t09_records/Records03Advanced.java) — Rekordy dla zaawansowanych — generyczne, lokalne, z interfejsem, jako klucze map, z instanceof ze wzorcem

## t10_exceptions

Wyjątki · [folder z lekcjami](src/t10_exceptions)

1. [Exceptions01Basics](src/t10_exceptions/Exceptions01Basics.java) — Wyjątki — podstawy: try, catch, finally, throw, stos wywołań, hierarchia
2. [Exceptions02CheckedUnchecked](src/t10_exceptions/Exceptions02CheckedUnchecked.java) — Wyjątki sprawdzane (checked) i niesprawdzane (unchecked); throws
3. [Exceptions03MultiCatch](src/t10_exceptions/Exceptions03MultiCatch.java) — Kilka bloków catch, multi-catch, ponowne rzucanie, wyjątek w catch/finally, przetwarzanie wsadowe
4. [Exceptions04TryWithResources](src/t10_exceptions/Exceptions04TryWithResources.java) — try-with-resources — automatyczne zamykanie zasobów (Java 7+)
5. [Exceptions05CustomExceptions](src/t10_exceptions/Exceptions05CustomExceptions.java) — Własne wyjątki — kiedy, jak nazwać, jakie konstruktory i pola, hierarchia wyjątków domenowych
6. [Exceptions06ChainingWrapping](src/t10_exceptions/Exceptions06ChainingWrapping.java) — Łańcuch wyjątków — przyczyna (cause), opakowywanie i tłumaczenie wyjątków między warstwami
7. [Exceptions07BestPractices](src/t10_exceptions/Exceptions07BestPractices.java) — Wyjątki — dobre praktyki i antywzorce (PRZED/PO)

## t11_generics

Typy generyczne · [folder z lekcjami](src/t11_generics)

1. [Generics01Why](src/t11_generics/Generics01Why.java) — Po co generyki — od surowych typów (Object + rzutowanie) do List<String>
2. [Generics02Classes](src/t11_generics/Generics02Classes.java) — Własne klasy i interfejsy generyczne — jeden i kilka parametrów typu, dziedziczenie
3. [Generics03Methods](src/t11_generics/Generics03Methods.java) — Metody generyczne i wnioskowanie typu
4. [Generics04Bounded](src/t11_generics/Generics04Bounded.java) — Ograniczenia typu (bounded types): <T extends Comparable<T>>, <N extends Number>, kilka ograniczeń
5. [Generics05Wildcards](src/t11_generics/Generics05Wildcards.java) — Symbole wieloznaczne (wildcards): ?, ? extends T, ? super T i zasada PECS
6. [Generics06ErasureLimits](src/t11_generics/Generics06ErasureLimits.java) — Wymazywanie typów (type erasure) i ograniczenia generyków; Class<T> i Supplier<T> jako obejście
7. [Generics07Repository](src/t11_generics/Generics07Repository.java) — Generyki w praktyce — generyczne repozytorium Repository<T, ID>

## t12_collections

Kolekcje · [folder z lekcjami](src/t12_collections)

1. [Collections01Overview](src/t12_collections/Collections01Overview.java) — Mapa terenu — hierarchia kolekcji i jak wybrać właściwą
2. [Collections02Lists](src/t12_collections/Collections02Lists.java) — ArrayList — dodawanie, usuwanie, widoki, sortowanie w miejscu
3. [Collections03IterationModification](src/t12_collections/Collections03IterationModification.java) — Iterowanie i modyfikacja — for-each, Iterator, ListIterator
4. [Collections04Sets](src/t12_collections/Collections04Sets.java) — Set — HashSet, LinkedHashSet, TreeSet i algebra zbiorów
5. [Collections05Maps](src/t12_collections/Collections05Maps.java) — Map — HashMap, TreeMap, LinkedHashMap: put, get, computeIfAbsent, merge
6. [Collections06QueuesDeques](src/t12_collections/Collections06QueuesDeques.java) — Queue, Deque, PriorityQueue — kolejki, stosy, kolejki priorytetowe
7. [Collections07ComparableComparator](src/t12_collections/Collections07ComparableComparator.java) — Comparable i Comparator — własne reguły sortowania
8. [Collections08ImmutableUnmodifiable](src/t12_collections/Collections08ImmutableUnmodifiable.java) — Niezmienność i widoki „tylko do odczytu” — List.of, unmodifiableList, copyOf, Arrays.asList
9. [Collections09CollectionsUtility](src/t12_collections/Collections09CollectionsUtility.java) — Klasa Collections — statyczne narzędzia do pracy z listami i kolekcjami
10. [Collections10Patterns](src/t12_collections/Collections10Patterns.java) — Przepisy kolekcyjne — gotowe wzorce z pętli, które warto rozpoznawać na pamięć
11. [Collections11HashingInternals](src/t12_collections/Collections11HashingInternals.java) — Jak naprawdę działa HashMap — hashCode, rozpraszanie bitów, kubełki, kolizje, resize
12. [Collections12CustomIterable](src/t12_collections/Collections12CustomIterable.java) — Własne Iterable i Iterator — jak zbudować coś, co działa w for-each
13. [Collections13Performance](src/t12_collections/Collections13Performance.java) — Wydajność kolekcji — Big-O, policzone operacje (nie stoper!), typowe pułapki

## t13_lambdas

Lambdy · [folder z lekcjami](src/t13_lambdas)

1. [Lambda01FromAnonymousToLambda](src/t13_lambdas/Lambda01FromAnonymousToLambda.java) — Od klasy anonimowej do lambdy — jak przekazać metodzie ZACHOWANIE
2. [Lambda02FunctionalInterfaces](src/t13_lambdas/Lambda02FunctionalInterfaces.java) — Interfejsy funkcyjne — „gniazdka”, do których pasuje lambda
3. [Lambda03JavaUtilFunction](src/t13_lambdas/Lambda03JavaUtilFunction.java) — Pakiet java.util.function — gotowe interfejsy funkcyjne na każdą okazję
4. [Lambda04MethodReferences](src/t13_lambdas/Lambda04MethodReferences.java) — Referencje do metod — Klasa::metoda zamiast lambdy, która tylko woła jedną metodę
5. [Lambda05Composition](src/t13_lambdas/Lambda05Composition.java) — Składanie funkcji — łączenie małych lambd w większe zachowania
6. [Lambda06ClosuresScope](src/t13_lambdas/Lambda06ClosuresScope.java) — Domknięcia i zasięg — jakie zmienne „widzi” lambda i dlaczego nie może ich zmieniać
7. [Lambda07HigherOrderFunctions](src/t13_lambdas/Lambda07HigherOrderFunctions.java) — Funkcje wyższego rzędu — funkcje, które przyjmują albo zwracają inne funkcje
8. [Lambda08Pitfalls](src/t13_lambdas/Lambda08Pitfalls.java) — Pułapki lambd — wyjątki sprawdzane, przeciążenia, debugowanie, rekurencja, null i var

## t14_optional

Optional · [folder z lekcjami](src/t14_optional)

1. [Optional01Basics](src/t14_optional/Optional01Basics.java) — Optional — podstawy. Pudełko na wartość, której może nie być
2. [Optional02Transform](src/t14_optional/Optional02Transform.java) — Optional — przekształcanie wartości bez wyjmowania jej z pudełka
3. [Optional03BestPractices](src/t14_optional/Optional03BestPractices.java) — Optional — dobre praktyki. Gdzie używać, a gdzie NIE

## t15_numbers

Liczby · [folder z lekcjami](src/t15_numbers)

1. [Numbers01BigDecimal](src/t15_numbers/Numbers01BigDecimal.java) — BigDecimal — dokładne liczby dziesiętne (pieniądze, stawki, ilości)
2. [Numbers02MoneyValueObject](src/t15_numbers/Numbers02MoneyValueObject.java) — Money — własny obiekt-wartość (value object) na pieniądze, zbudowany na BigDecimal
3. [Numbers03BigInteger](src/t15_numbers/Numbers03BigInteger.java) — BigInteger — liczby całkowite dowolnej wielkości
4. [Numbers04FormattingParsing](src/t15_numbers/Numbers04FormattingParsing.java) — Formatowanie i wczytywanie liczb — kropka czy przecinek, spacje, waluty, procenty
5. [Numbers05IntegerTricks](src/t15_numbers/Numbers05IntegerTricks.java) — Sztuczki i pułapki liczb całkowitych — przepełnienie, dzielenie, systemy liczbowe, bity
6. [Numbers06MathCheatsheet](src/t15_numbers/Numbers06MathCheatsheet.java) — Ściągawka klasy Math — najczęściej używane funkcje w jednym miejscu

## t16_streams

Streamy · [folder z lekcjami](src/t16_streams)

1. [Streams01Intro](src/t16_streams/Streams01Intro.java) — Stream — wprowadzenie. Czym jest strumień i dlaczego warto go używać
2. [Streams02Creation](src/t16_streams/Streams02Creation.java) — Tworzenie streamów — skąd wziąć strumień
3. [Streams03FilterMap](src/t16_streams/Streams03FilterMap.java) — filter i map — dwie najczęściej używane operacje na streamach
4. [Streams04FlatMap](src/t16_streams/Streams04FlatMap.java) — flatMap — przekształć i spłaszcz strumień
5. [Streams05SortDistinctLimit](src/t16_streams/Streams05SortDistinctLimit.java) — sorted, distinct, limit, skip, takeWhile, dropWhile — porządek, unikalność i „wycinanie”
6. [Streams06TerminalOps](src/t16_streams/Streams06TerminalOps.java) — Operacje końcowe (terminal operations) — co można „wyjąć” ze strumienia
7. [Streams07Reduce](src/t16_streams/Streams07Reduce.java) — reduce — składanie strumienia w jeden wynik
8. [Streams08PrimitiveStreams](src/t16_streams/Streams08PrimitiveStreams.java) — Strumienie prymitywne — IntStream, LongStream, DoubleStream
9. [Streams09CollectorsBasic](src/t16_streams/Streams09CollectorsBasic.java) — Kolektory podstawowe — collect(Collectors.xxx)
10. [Streams10CollectorsToMap](src/t16_streams/Streams10CollectorsToMap.java) — Collectors.toMap — strumień zamieniony w mapę
11. [Streams11GroupingBy](src/t16_streams/Streams11GroupingBy.java) — Collectors.groupingBy — grupowanie elementów strumienia
12. [Streams12PartitioningBy](src/t16_streams/Streams12PartitioningBy.java) — Collectors.partitioningBy — podział na dwie grupy: true / false
13. [Streams13AdvancedCollectors](src/t16_streams/Streams13AdvancedCollectors.java) — Zaawansowane kolektory — collectingAndThen, filtering, flatMapping, teeing, Collector.of
14. [Streams14OptionalInStreams](src/t16_streams/Streams14OptionalInStreams.java) — Optional w strumieniach — findFirst, max, flatMap(Optional::stream), ofNullable
15. [Streams15BigDecimalMoney](src/t16_streams/Streams15BigDecimalMoney.java) — Pieniądze w strumieniach — BigDecimal: sumy, średnie, grupowanie, zaokrąglanie
16. [Streams16Laziness](src/t16_streams/Streams16Laziness.java) — Leniwość strumieni — przepis, pionowe przetwarzanie, bariery, skróty, nieskończoność
17. [Streams17SideEffectsPitfalls](src/t16_streams/Streams17SideEffectsPitfalls.java) — Pułapki strumieni i efekty uboczne — katalog ŹLE → DOBRZE
18. [Streams18Parallel](src/t16_streams/Streams18Parallel.java) — Strumienie równoległe — parallelStream() i parallel()
19. [Streams19Recipes](src/t16_streams/Streams19Recipes.java) — Przepisy (recipes) — 39 gotowych rozwiązań typowych zadań na streamach
20. [Streams20Exercises](src/t16_streams/Streams20Exercises.java) — Streams20 — duży zestaw ćwiczeń z całego rozdziału o streamach

## t17_datetime

Data i czas · [folder z lekcjami](src/t17_datetime)

1. [DateTime01LocalDateTime](src/t17_datetime/DateTime01LocalDateTime.java) — LocalDate, LocalTime, LocalDateTime — data i czas bez strefy czasowej
2. [DateTime02PeriodDuration](src/t17_datetime/DateTime02PeriodDuration.java) — Period, Duration, ChronoUnit — ile czasu minęło, ile zostało
3. [DateTime03Formatting](src/t17_datetime/DateTime03Formatting.java) — DateTimeFormatter — formatowanie i parsowanie dat: wzorce, polskie nazwy, tryb ścisły, pułapki
4. [DateTime04ZonesInstant](src/t17_datetime/DateTime04ZonesInstant.java) — Strefy czasowe i Instant — ZoneId, ZonedDateTime, OffsetDateTime, zmiana czasu letni/zimowy
5. [DateTime05Practical](src/t17_datetime/DateTime05Practical.java) — Data i czas w praktyce — dni robocze, terminy płatności, harmonogramy, YearMonth, kolizje terminów, kalendarz
6. [DateTime06FormatterAdvanced](src/t17_datetime/DateTime06FormatterAdvanced.java) — DateTimeFormatter dla zaawansowanych — gotowe formaty ISO, formaty lokalne, Builder, parsowanie

## t18_io_files

Pliki · [folder z lekcjami](src/t18_io_files)

1. [Io01PathFiles](src/t18_io_files/Io01PathFiles.java) — Path i Files — ścieżki, pliki i katalogi w NIO.2
2. [Io02ReadingText](src/t18_io_files/Io02ReadingText.java) — Czytanie plików tekstowych — readString, readAllLines, lines, BufferedReader, Scanner
3. [Io03WritingText](src/t18_io_files/Io03WritingText.java) — Zapisywanie plików tekstowych — writeString, write, BufferedWriter, PrintWriter, flush i close
4. [Io04Csv](src/t18_io_files/Io04Csv.java) — CSV — czytanie i zapis danych tabelarycznych w plikach tekstowych
5. [Io05JsonManual](src/t18_io_files/Io05JsonManual.java) — JSON „na piechotę” — własny zapis i własny odczyt
6. [Io06Properties](src/t18_io_files/Io06Properties.java) — Properties — pliki konfiguracyjne klucz=wartość
7. [Io07WalkingDirectories](src/t18_io_files/Io07WalkingDirectories.java) — Przeglądanie katalogów — list, walk, find, glob, walkFileTree
8. [Io08BinaryStreams](src/t18_io_files/Io08BinaryStreams.java) — Strumienie bajtów — InputStream, OutputStream, Data*Stream i własny format binarny
9. [Io09Serialization](src/t18_io_files/Io09Serialization.java) — Serializacja obiektów — zapis obiektu jako bajtów i odczyt z powrotem
10. [Io10SimpleLogger](src/t18_io_files/Io10SimpleLogger.java) — Prosty logger — dziennik zdarzeń programu zamiast System.out.println
11. [Io11IoExceptions](src/t18_io_files/Io11IoExceptions.java) — Wyjątki wejścia-wyjścia — co może pójść nie tak z plikiem i jak to obsłużyć
12. [Io12Charsets](src/t18_io_files/Io12Charsets.java) — Kodowanie znaków — bajty, znaki i punkty kodowe, UTF-8, krzaczki, BOM
13. [Io13ZipArchives](src/t18_io_files/Io13ZipArchives.java) — Archiwa ZIP i GZIP — pakowanie, rozpakowywanie i bezpieczeństwo
14. [Io14Xml](src/t18_io_files/Io14Xml.java) — XML — DOM, StAX i XPath w JDK, czytanie, zapis i bezpieczne parsowanie

## t19_annotations_reflection

Adnotacje i refleksja · [folder z lekcjami](src/t19_annotations_reflection)

1. [Annotations01BuiltIn](src/t19_annotations_reflection/Annotations01BuiltIn.java) — Adnotacje wbudowane — czym jest adnotacja i kto ją czyta
2. [Annotations02Custom](src/t19_annotations_reflection/Annotations02Custom.java) — Własne adnotacje — elementy, wartości domyślne, retencja, cel, powtarzanie i dziedziczenie
3. [Annotations03ReflectionBasics](src/t19_annotations_reflection/Annotations03ReflectionBasics.java) — Refleksja — zaglądanie do budowy klas w czasie działania programu
4. [Annotations04Validator](src/t19_annotations_reflection/Annotations04Validator.java) — Mini framework walidacji — adnotacje na polach + refleksja = Bean Validation w miniaturze
5. [Annotations05MiniFramework](src/t19_annotations_reflection/Annotations05MiniFramework.java) — Mini framework — router komend oparty na adnotacjach (Spring MVC w miniaturze)
6. [Annotations06DynamicProxy](src/t19_annotations_reflection/Annotations06DynamicProxy.java) — Dynamiczne proxy — obiekt-zastępca tworzony w czasie działania programu
7. [Annotations07Processors](src/t19_annotations_reflection/Annotations07Processors.java) — Procesory adnotacji — kod, który czyta adnotacje PODCZAS KOMPILACJI i generuje nowe pliki

## t20_lombok

Lombok · [folder z lekcjami](src/t20_lombok)

1. [Lombok01Accessors](src/t20_lombok/Lombok01Accessors.java) — Lombok — gettery, settery, toString, equals/hashCode bez pisania ich ręcznie
2. [Lombok02Constructors](src/t20_lombok/Lombok02Constructors.java) — Lombok — konstruktory: @NoArgsConstructor, @AllArgsConstructor, @RequiredArgsConstructor, @NonNull
3. [Lombok03DataValueBuilder](src/t20_lombok/Lombok03DataValueBuilder.java) — Lombok — @Data, @Value, @Builder: gotowe klasy danych, niezmienność, płynne tworzenie obiektów
4. [Lombok04Other](src/t20_lombok/Lombok04Other.java) — Lombok — reszta zestawu: @Cleanup, @SneakyThrows, @Synchronized, val, @Log, konfiguracja

## t21_concurrency

Współbieżność · [folder z lekcjami](src/t21_concurrency)

1. [Concurrency01Threads](src/t21_concurrency/Concurrency01Threads.java) — Wątki — podstawy
2. [Concurrency02RaceConditions](src/t21_concurrency/Concurrency02RaceConditions.java) — Wyścigi (race conditions) i jak je naprawić
3. [Concurrency03Locks](src/t21_concurrency/Concurrency03Locks.java) — Blokady — synchronized, ReentrantLock, Condition, ReadWriteLock, StampedLock, wait/notify, zakleszczenie
4. [Concurrency04Executors](src/t21_concurrency/Concurrency04Executors.java) — Pule wątków — ExecutorService, Callable, Future
5. [Concurrency05CompletableFuture](src/t21_concurrency/Concurrency05CompletableFuture.java) — CompletableFuture — łańcuchy zadań asynchronicznych
6. [Concurrency06ConcurrentCollections](src/t21_concurrency/Concurrency06ConcurrentCollections.java) — Kolekcje współbieżne — ConcurrentHashMap, CopyOnWriteArrayList, BlockingQueue i spółka
7. [Concurrency07Synchronizers](src/t21_concurrency/Concurrency07Synchronizers.java) — Synchronizatory — CountDownLatch, CyclicBarrier, Semaphore, Phaser, Exchanger
8. [Concurrency08ThreadSafetyPatterns](src/t21_concurrency/Concurrency08ThreadSafetyPatterns.java) — Wzorce bezpieczeństwa wątkowego — jak projektować klasy, żeby wyścigi w ogóle nie powstawały
9. [Concurrency09ScheduledForkJoin](src/t21_concurrency/Concurrency09ScheduledForkJoin.java) — Zadania planowane i Fork/Join — ScheduledExecutorService, ForkJoinPool, strumienie równoległe
10. [Concurrency10MemoryModel](src/t21_concurrency/Concurrency10MemoryModel.java) — Model pamięci Javy (JMM) — widoczność, zmiana kolejności, atomowość i relacja happens-before

## t22_design_patterns

Wzorce projektowe · [folder z lekcjami](src/t22_design_patterns)

1. [Patterns01Strategy](src/t22_design_patterns/Patterns01Strategy.java) — Strategy (strategia) — wymienny algorytm za wspólnym interfejsem
2. [Patterns02Builder](src/t22_design_patterns/Patterns02Builder.java) — Builder (budowniczy) — składanie złożonego obiektu krok po kroku
3. [Patterns03Factory](src/t22_design_patterns/Patterns03Factory.java) — Factory (fabryka, metoda wytwórcza) — kto i jak tworzy obiekty
4. [Patterns04Singleton](src/t22_design_patterns/Patterns04Singleton.java) — Singleton (singleton, „jedynak”) — dokładnie jedna instancja klasy
5. [Patterns05TemplateMethod](src/t22_design_patterns/Patterns05TemplateMethod.java) — Template Method (metoda szablonowa) — stały szkielet algorytmu, zmienne kroki
6. [Patterns06Observer](src/t22_design_patterns/Patterns06Observer.java) — Observer (obserwator) — „daj znać wszystkim zainteresowanym”
7. [Patterns07Decorator](src/t22_design_patterns/Patterns07Decorator.java) — Decorator (dekorator) — dokładanie zachowania bez rozrostu podklas
8. [Patterns08DependencyInjection](src/t22_design_patterns/Patterns08DependencyInjection.java) — Dependency Injection (wstrzykiwanie zależności) — „nie twórz, tylko dostań”
9. [Patterns09Command](src/t22_design_patterns/Patterns09Command.java) — Command (polecenie) — operacja jako obiekt
10. [Patterns10Facade](src/t22_design_patterns/Patterns10Facade.java) — Facade (fasada) — jedna prosta „recepcja” przed skomplikowanym zapleczem
11. [Patterns11Adapter](src/t22_design_patterns/Patterns11Adapter.java) — Wzorzec Adapter — dopasowanie niezgodnych interfejsów
12. [Patterns12Composite](src/t22_design_patterns/Patterns12Composite.java) — Wzorzec Composite — drzewo, w którym całość i część traktujemy tak samo
13. [Patterns13State](src/t22_design_patterns/Patterns13State.java) — Wzorzec State — zachowanie zależne od stanu obiektu
14. [Patterns14ChainOfResponsibility](src/t22_design_patterns/Patterns14ChainOfResponsibility.java) — Wzorzec Chain of Responsibility — łańcuch odpowiedzialności
15. [Patterns15Visitor](src/t22_design_patterns/Patterns15Visitor.java) — Wzorzec Visitor — nowe operacje na stałej hierarchii klas

## t23_modern_java

Nowości Java 8→17 i zapowiedź 21+ · [folder z lekcjami](src/t23_modern_java)

1. [Modern01Java8](src/t23_modern_java/Modern01Java8.java) — Java 8 — wielka zmiana stylu pisania kodu
2. [Modern02Var](src/t23_modern_java/Modern02Var.java) — var — wnioskowanie typu zmiennych lokalnych (Java 10, JEP 286)
3. [Modern03SwitchExpressions](src/t23_modern_java/Modern03SwitchExpressions.java) — Wyrażenia switch — strzałki, yield, wyczerpywalność (Java 14, JEP 361)
4. [Modern04TextBlocks](src/t23_modern_java/Modern04TextBlocks.java) — Bloki tekstowe — wieloliniowe napisy bez bałaganu (Java 15, JEP 378)
5. [Modern05RecordsSealedPatterns](src/t23_modern_java/Modern05RecordsSealedPatterns.java) — Rekordy, instanceof ze wzorcem i klasy zapieczętowane (Java 16–17)
6. [Modern06ApiAdditions](src/t23_modern_java/Modern06ApiAdditions.java) — Małe perełki API wydanie po wydaniu (Java 9–17)
7. [Modern07WhatsNextJava21](src/t23_modern_java/Modern07WhatsNextJava21.java) — Co dalej? Java 18–21 (i krótko 22–25) oraz jak się do tego przygotować

## t24_algorithms

Algorytmy i matematyka · [folder z lekcjami](src/t24_algorithms)

1. [Algorithms01Complexity](src/t24_algorithms/Algorithms01Complexity.java) — Złożoność obliczeniowa — notacja O() (Big O)
2. [Algorithms02Sorting](src/t24_algorithms/Algorithms02Sorting.java) — Algorytmy sortowania — od O(n²) do O(n log n)
3. [Algorithms03Searching](src/t24_algorithms/Algorithms03Searching.java) — Wyszukiwanie — liniowe, binarne i "binarne po odpowiedzi"
4. [Algorithms04DataStructures](src/t24_algorithms/Algorithms04DataStructures.java) — Struktury danych napisane ręcznie — jak działają pod spodem klasy z JDK
5. [Algorithms05Classics](src/t24_algorithms/Algorithms05Classics.java) — Klasyczne wzorce algorytmiczne — brute force kontra sprytniejsze O()
6. [Algorithms06Backtracking](src/t24_algorithms/Algorithms06Backtracking.java) — Nawracanie (backtracking) — wybieraj, eksploruj, cofaj
7. [Algorithms07DynamicProgramming](src/t24_algorithms/Algorithms07DynamicProgramming.java) — Programowanie dynamiczne — pamiętaj, zamiast liczyć od nowa
8. [Algorithms08Graphs](src/t24_algorithms/Algorithms08Graphs.java) — Grafy — reprezentacje, BFS, DFS, sortowanie topologiczne, Dijkstra
9. [Math01NumberTheory](src/t24_algorithms/Math01NumberTheory.java) — Teoria liczb — podzielność, NWD, NWW, liczby pierwsze, sito Eratostenesa
10. [Math02ModularChecksums](src/t24_algorithms/Math02ModularChecksums.java) — Arytmetyka modularna i sumy kontrolne — od floorMod po PESEL, NIP i IBAN
11. [Math03Combinatorics](src/t24_algorithms/Math03Combinatorics.java) — Kombinatoryka — silnia, permutacje, wariacje, kombinacje, trójkąt Pascala
12. [Math04NumberSystems](src/t24_algorithms/Math04NumberSystems.java) — Systemy liczbowe — konwersje, kod uzupełnień do dwóch, liczby rzymskie
13. [Math05NumericalMethods](src/t24_algorithms/Math05NumericalMethods.java) — Metody numeryczne — Newton, bisekcja, całkowanie, Monte Carlo
14. [Math06Statistics](src/t24_algorithms/Math06Statistics.java) — Statystyka opisowa — średnia, mediana, dominanta, wariancja, percentyle
15. [Math07MatricesGeometry](src/t24_algorithms/Math07MatricesGeometry.java) — Macierze i geometria obliczeniowa — tablice 2D jako liczby, punkty i wielokąty
16. [Math08BigNumbers](src/t24_algorithms/Math08BigNumbers.java) — Bardzo duże i bardzo dokładne liczby — BigInteger i BigDecimal bez granic
17. [Math09InterviewClassics](src/t24_algorithms/Math09InterviewClassics.java) — Klasyki pytań rekrutacyjnych — silnia, Fibonacci, liczby pierwsze i inne
18. [Math10PuzzlesEuler](src/t24_algorithms/Math10PuzzlesEuler.java) — Łamigłówki w stylu Project Euler — brute force, potem sprytniej

## t25_testing

Testowanie — pojęcia bez frameworka · [folder z lekcjami](src/t25_testing)

1. [Testing01Concepts](src/t25_testing/Testing01Concepts.java) — Testowanie — po co, piramida testów, AAA/FIRST, własny mini-runner
2. [Testing02TestDoubles](src/t25_testing/Testing02TestDoubles.java) — Testy podwójne — dummy, stub, fake, spy, mock
3. [Testing03TestableDesign](src/t25_testing/Testing03TestableDesign.java) — Projektowanie pod testowalność — PRZED (trudny kod) / PO (łatwy kod)

## t26_jvm

JVM · [folder z lekcjami](src/t26_jvm)

1. [Jvm01Memory](src/t26_jvm/Jvm01Memory.java) — Pamięć JVM — stos, sterta, pula napisów i wycieki pamięci
2. [Jvm02ClassLoadingInit](src/t26_jvm/Jvm02ClassLoadingInit.java) — Ładowanie i inicjalizacja klas — kiedy JVM „budzi” klasę
3. [Jvm03GarbageCollection](src/t26_jvm/Jvm03GarbageCollection.java) — Odśmiecanie pamięci (garbage collection, GC) — kto, kiedy i jak sprząta stertę
4. [Jvm04ToolsProfiling](src/t26_jvm/Jvm04ToolsProfiling.java) — Narzędzia JDK i profilowanie — jak zajrzeć do działającej JVM

## t27_clean_code_pitfalls

Pułapki i czysty kod · [folder z lekcjami](src/t27_clean_code_pitfalls)

1. [Pitfalls01Classic](src/t27_clean_code_pitfalls/Pitfalls01Classic.java) — Klasyczne pułapki Javy — katalog 22 zaskoczeń
2. [Pitfalls02CodeReview](src/t27_clean_code_pitfalls/Pitfalls02CodeReview.java) — Typowe uwagi z code review — PRZED/PO
3. [CleanCode01Principles](src/t27_clean_code_pitfalls/CleanCode01Principles.java) — Zasady czystego kodu — nazewnictwo, funkcje, DRY/KISS/YAGNI i inne
4. [CleanCode02Solid](src/t27_clean_code_pitfalls/CleanCode02Solid.java) — SOLID na jednym przykładzie — moduł fakturowania krok po kroku
5. [CleanCode03Architecture](src/t27_clean_code_pitfalls/CleanCode03Architecture.java) — Architektura — od jednej klasy do aplikacji

## t28_networking_http

Sieć i HTTP · [folder z lekcjami](src/t28_networking_http)

1. [Net01SocketsTcpUdp](src/t28_networking_http/Net01SocketsTcpUdp.java) — Gniazda (sockets) — adres IP, port, TCP i UDP
2. [Http01UriUrl](src/t28_networking_http/Http01UriUrl.java) — Adresy URI i URL, kodowanie znaków w adresie, podstawy HTTP
3. [Http02HttpClient](src/t28_networking_http/Http02HttpClient.java) — Klient HTTP — java.net.http.HttpClient (Java 11+)
4. [Http03LocalServer](src/t28_networking_http/Http03LocalServer.java) — Własny serwer HTTP — com.sun.net.httpserver.HttpServer
5. [Http04JsonApi](src/t28_networking_http/Http04JsonApi.java) — API w formacie JSON — klient i serwer
6. [Http05AsyncTimeouts](src/t28_networking_http/Http05AsyncTimeouts.java) — Żądania asynchroniczne, limity czasu, ponawianie i ograniczanie współbieżności

## t29_jdbc_databases

Bazy danych · [folder z lekcjami](src/t29_jdbc_databases)

1. [Jdbc01SqlBasics](src/t29_jdbc_databases/Jdbc01SqlBasics.java) — Podstawy SQL — tabele, ograniczenia, INSERT/SELECT/UPDATE/DELETE i NULL
2. [Jdbc02Connection](src/t29_jdbc_databases/Jdbc02Connection.java) — JDBC — połączenie, polecenie, wynik, metadane i SQLException
3. [Jdbc03PreparedStatement](src/t29_jdbc_databases/Jdbc03PreparedStatement.java) — PreparedStatement — zapytania z parametrami ?
4. [Jdbc04Transactions](src/t29_jdbc_databases/Jdbc04Transactions.java) — Transakcje — commit, rollback, punkty zapisu, izolacja i blokowanie optymistyczne
5. [Jdbc05Dao](src/t29_jdbc_databases/Jdbc05Dao.java) — DAO i repozytorium — SQL schowany za interfejsem
6. [Jdbc06SqlJoins](src/t29_jdbc_databases/Jdbc06SqlJoins.java) — Złączenia (JOIN), podzapytania i projekt relacyjny
7. [Jdbc07SqlAggregationIndexes](src/t29_jdbc_databases/Jdbc07SqlAggregationIndexes.java) — Agregacja w SQL (COUNT, SUM, GROUP BY, HAVING, funkcje okna) i indeksy

## t30_build_modules

Budowanie i uruchamianie · [folder z lekcjami](src/t30_build_modules)

1. [Build01MavenBasics](src/t30_build_modules/Build01MavenBasics.java) — Maven od podstaw — po co narzędzie budowania i jak czytać pom.xml
2. [Build02JarClasspath](src/t30_build_modules/Build02JarClasspath.java) — Kompilacja, ścieżka klas (classpath) i pliki JAR — bez IDE
3. [Build03Modules](src/t30_build_modules/Build03Modules.java) — Moduły JPMS (Java 9+) — module-info.java, ścieżka modułów, silna enkapsulacja
4. [Build04CommandLineApps](src/t30_build_modules/Build04CommandLineApps.java) — Aplikacje konsolowe — argumenty, kody wyjścia, stdin/stdout/stderr
5. [Build05ProcessesEnv](src/t30_build_modules/Build05ProcessesEnv.java) — Procesy potomne, zmienne środowiskowe i właściwości systemowe
6. [Build06QualityToolsJavadoc](src/t30_build_modules/Build06QualityToolsJavadoc.java) — Jakość kodu — Javadoc, doclint, ostrzeżenia kompilatora, analiza statyczna, pokrycie, CI

## t31_jdk_toolbox

Przydatne narzędzia JDK · [folder z lekcjami](src/t31_jdk_toolbox)

1. [Toolbox01UuidBase64](src/t31_jdk_toolbox/Toolbox01UuidBase64.java) — UUID, Base64 i HexFormat — identyfikatory i zapis bajtów jako tekst
2. [Toolbox02HashingSecurity](src/t31_jdk_toolbox/Toolbox02HashingSecurity.java) — Skróty, hasła, losowość i HMAC — podstawy bezpieczeństwa w JDK
3. [Toolbox03I18n](src/t31_jdk_toolbox/Toolbox03I18n.java) — Internacjonalizacja (i18n) — język, liczby, daty, komunikaty, liczba mnoga, sortowanie
4. [Toolbox04Logging](src/t31_jdk_toolbox/Toolbox04Logging.java) — Logowanie w JDK — System.Logger i java.util.logging (JUL) w praktyce

## t32_junit_mockito

Testy w praktyce · [folder z lekcjami](src/t32_junit_mockito)

1. [JUnit01Basics](src/t32_junit_mockito/JUnit01Basics.java) — JUnit 5 — podstawy prawdziwego frameworka testowego
2. [JUnit02Parameterized](src/t32_junit_mockito/JUnit02Parameterized.java) — Testy sparametryzowane — jeden test, wiele zestawów danych
3. [JUnit03AssertJ](src/t32_junit_mockito/JUnit03AssertJ.java) — AssertJ — płynne, czytelne asercje
4. [JUnit04Mockito](src/t32_junit_mockito/JUnit04Mockito.java) — Mockito — zaślepki, atrapy i weryfikacja interakcji
5. [JUnit05Tdd](src/t32_junit_mockito/JUnit05Tdd.java) — TDD — programowanie sterowane testami (czerwony → zielony → refaktoryzacja)

## t33_interview_prep

Rozmowa kwalifikacyjna · [folder z lekcjami](src/t33_interview_prep)

1. [Interview01JavaQuestions](src/t33_interview_prep/Interview01JavaQuestions.java) — Pytania o język Java i platformę — 25 kart rozmowy kwalifikacyjnej
2. [Interview02OopCollections](src/t33_interview_prep/Interview02OopCollections.java) — OOP i kolekcje na rozmowie kwalifikacyjnej — 22 karty
3. [Interview03CodingTasks](src/t33_interview_prep/Interview03CodingTasks.java) — Klasyczne zadania programistyczne z rozmów kwalifikacyjnych
4. [Interview04LiveCoding](src/t33_interview_prep/Interview04LiveCoding.java) — Kodowanie na żywo (live coding) — jak się zachować i jedno zadanie krok po kroku

## t34_toward_spring

Most do Springa · [folder z lekcjami](src/t34_toward_spring)

1. [Spring01IocContainer](src/t34_toward_spring/Spring01IocContainer.java) — Kontener IoC od środka — budujemy własny „mini-Spring” w czystej Javie
2. [Spring02Layers](src/t34_toward_spring/Spring02Layers.java) — Warstwy aplikacji — kontroler, serwis, repozytorium (w czystej Javie, tak jak w Springu)
3. [Spring03RestConcepts](src/t34_toward_spring/Spring03RestConcepts.java) — REST od środka — zasady, mini-framework z adnotacjami, JSON i prawdziwy serwer HTTP
4. [Spring04WhatSpringGives](src/t34_toward_spring/Spring04WhatSpringGives.java) — Co daje Spring Boot — mapa i mechanizmy zbudowane w czystej Javie

## t35_capstone

Mini-projekty łączące cały kurs · [folder z lekcjami](src/t35_capstone)

1. [Capstone01Library](src/t35_capstone/Capstone01Library.java) — Projekt 1 — wypożyczalnia książek
2. [Capstone02SalesReport](src/t35_capstone/Capstone02SalesReport.java) — Projekt 2 — raport sprzedaży z pliku CSV
3. [Capstone03WeatherStations](src/t35_capstone/Capstone03WeatherStations.java) — Projekt 3 — stacje pogodowe przetwarzane współbieżnie
4. [Capstone04RestTodo](src/t35_capstone/Capstone04RestTodo.java) — Projekt 4 — usługa REST „lista zadań” (TODO) na HttpServer + klient HttpClient
