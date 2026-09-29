/**
 * TEMAT: t16_streams — STREAMY (strumienie danych), największy dział kursu.
 *
 * <p>Stream (strumień) to sposób przetwarzania kolekcji „taśmowo”: bierzesz elementy ze źródła,
 * przepuszczasz przez kolejne operacje (filtruj, przekształć, sortuj...) i na końcu zbierasz wynik.
 * Zamiast pisać JAK coś zrobić (pętla, if, lista pomocnicza), piszesz CO chcesz dostać.</p>
 *
 * <p>Zanim zaczniesz, dobrze znać: typy generyczne w podstawowym zakresie (t11_generics), kolekcje
 * (t12_collections), lambdy i referencje do metod (t13_lambdas), Optional (t14_optional) oraz BigDecimal
 * (t15_numbers) — streamy korzystają ze wszystkich tych tematów. W lekcjach i tak każda lambda,
 * referencja do metody i Optional są krótko objaśnione w komentarzu.</p>
 *
 * <p>Kolejność czytania:</p>
 * <ol>
 *   <li>Streams01Intro — czym jest stream, pętla vs stream, budowa potoku, leniwość, jednorazowość</li>
 *   <li>Streams02Creation — skąd wziąć stream (kolekcje, tablice, zakresy, iterate, generate, napisy, pliki)</li>
 *   <li>Streams03FilterMap — filter (filtruj) i map (przekształć), peek, kolejność operacji</li>
 *   <li>Streams04FlatMap — spłaszczanie (lista list → jedna lista)</li>
 *   <li>Streams05SortDistinctLimit — sortowanie, usuwanie duplikatów, limit/skip, paginacja, takeWhile/dropWhile</li>
 *   <li>Streams06TerminalOps — operacje końcowe: count, min/max, findFirst, anyMatch/allMatch/noneMatch, toList</li>
 *   <li>Streams07Reduce — redukcja do jednej wartości (suma, iloczyn, najdłuższy napis)</li>
 *   <li>Streams08PrimitiveStreams — IntStream/LongStream/DoubleStream, sum, average, statystyki</li>
 *   <li>Streams09CollectorsBasic — podstawowe kolektory: toSet, joining, counting, summing, averaging</li>
 *   <li>Streams10CollectorsToMap — zbieranie do mapy, duplikaty kluczy, kolejność</li>
 *   <li>Streams11GroupingBy — grupowanie (najważniejszy kolektor!)</li>
 *   <li>Streams12PartitioningBy — podział na dwie grupy: spełnia / nie spełnia</li>
 *   <li>Streams13AdvancedCollectors — collectingAndThen, filtering, flatMapping, teeing, własny kolektor</li>
 *   <li>Streams14OptionalInStreams — Optional w potokach</li>
 *   <li>Streams15BigDecimalMoney — pieniądze w streamach (sumy, średnie, grupowanie kwot)</li>
 *   <li>Streams16Laziness — leniwość, kolejność „pionowa”, short-circuit, nieskończone strumienie</li>
 *   <li>Streams17SideEffectsPitfalls — skutki uboczne i najczęstsze błędy</li>
 *   <li>Streams18Parallel — strumienie równoległe: kiedy tak, kiedy nie</li>
 *   <li>Streams19Recipes — książka kucharska: 30+ gotowych przepisów</li>
 *   <li>Streams20Exercises — duży zestaw ćwiczeń ze sprawdzaniem</li>
 * </ol>
 *
 * <p>SŁÓWKA: stream = strumień; pipeline = potok; source = źródło; intermediate = pośredni;
 * terminal = końcowy; collector = kolektor (zbieracz); lazy = leniwy.</p>
 */
package t16_streams;
