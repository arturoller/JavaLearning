/**
 * TEMAT: t12_collections — kolekcje: listy, zbiory, mapy, kolejki
 *
 * <p>Pakiet o Collections Framework z JDK: interfejsy (List, Set, Queue/Deque, Map) i ich najważniejsze
 * implementacje (ArrayList, LinkedList, HashSet, LinkedHashSet, TreeSet, ArrayDeque, PriorityQueue, HashMap,
 * LinkedHashMap, TreeMap), iterowanie i bezpieczna modyfikacja w trakcie iteracji, sortowanie własnymi
 * regułami (Comparable/Comparator), niemodyfikowalność i kopie obronne, gotowe przepisy na typowe zadania,
 * wewnętrzne działanie mieszania (hashingu) w HashMap, pisanie własnych kolekcji iterowalnych oraz
 * złożoność obliczeniowa (Big-O) najważniejszych operacji.</p>
 *
 * <p>Wymagana wiedza: t01_basics — t05_methods (podstawy, tablice, metody), t06_oop_basics (klasy,
 * equals/hashCode), t08_enums (enumy), t09_records (rekordy), t10_exceptions (wyjątki), t11_generics
 * (generyki). Lambdy i strumienie (Stream API) pojawiają się tu tylko w najprostszej postaci — pełne
 * omówienie jest w t13_lambdas i t16_streams.</p>
 *
 * <ol>
 *   <li>Collections01Overview — hierarchia interfejsów, tabela implementacji, „programuj do interfejsu”,
 *       wprowadzenie do List.of/Set.of/Map.of</li>
 *   <li>Collections02Lists — ArrayList: dodawanie, usuwanie, subList, sort, replaceAll, removeIf;
 *       ArrayList kontra LinkedList</li>
 *   <li>Collections03IterationModification — for-each, Iterator, ListIterator,
 *       ConcurrentModificationException i jak jej unikać</li>
 *   <li>Collections04Sets — HashSet, LinkedHashSet, TreeSet/NavigableSet, algebra zbiorów
 *       (suma, przecięcie, różnica)</li>
 *   <li>Collections05Maps — HashMap: put/get/computeIfAbsent/merge, TreeMap (nawigacja),
 *       LinkedHashMap (mini pamięć podręczna LRU)</li>
 *   <li>Collections06QueuesDeques — Queue, ArrayDeque jako kolejka i jako stos, PriorityQueue</li>
 *   <li>Collections07ComparableComparator — Comparable, Comparator, sortowanie wielopoziomowe,
 *       Collator dla polskich napisów</li>
 *   <li>Collections08ImmutableUnmodifiable — List.of, Collections.unmodifiableList, List.copyOf,
 *       Arrays.asList, kopie obronne</li>
 *   <li>Collections09CollectionsUtility — klasa Collections: sort, shuffle, binarySearch,
 *       frequency, rotate i inne</li>
 *   <li>Collections10Patterns — gotowe przepisy: zliczanie, grupowanie, odwracanie mapy,
 *       top-N, dwie sumy, okno przesuwne</li>
 *   <li>Collections11HashingInternals — jak działa HashMap w środku: hashCode, kubełki, kolizje,
 *       współczynnik wypełnienia</li>
 *   <li>Collections12CustomIterable — własna klasa implementująca Iterable/Iterator i kontrakt iteratora</li>
 *   <li>Collections13Performance — złożoność (Big-O) operacji na różnych kolekcjach, pomiar licznikami
 *       zamiast czasem</li>
 * </ol>
 *
 * <p>SŁÓWKA: collection = kolekcja; list = lista; set = zbiór; queue = kolejka;
 * deque = kolejka dwustronna; map = mapa; entry = wpis (para klucz-wartość); iterate = iterować;
 * mutable = modyfikowalny; immutable/unmodifiable = niemodyfikowalny; comparable = porównywalny;
 * comparator = obiekt porównujący; hashing = mieszanie (haszowanie).</p>
 */
package t12_collections;
