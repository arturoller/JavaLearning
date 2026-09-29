/**
 * TEMAT: t14_optional — OPTIONAL, czyli „pudełko” na wartość, której może nie być.
 *
 * <p>Wiele metod czasem nie ma czego zwrócić: klient może nie mieć e-maila, produktu o danym kodzie może nie
 * być w magazynie, student może nie mieć jeszcze żadnej oceny. Dawniej takie metody zwracały {@code null},
 * a wywołujący często o tym zapominał — i program kończył się wyjątkiem NullPointerException.
 * Optional (opcjonalny) to typ, który mówi wprost: „wynik może być pusty — obsłuż to”.</p>
 *
 * <p>Zanim zaczniesz, dobrze znać: wyjątki (t10_exceptions — NullPointerException, NoSuchElementException),
 * typy generyczne (t11_generics — {@code Optional<String>} to typ generyczny), kolekcje (t12_collections —
 * Map.get zwraca null) oraz lambdy i referencje do metod (t13_lambdas — map, filter i orElseGet przyjmują
 * lambdy). Streamy (t16_streams) są PO tym dziale: tam Optional wraca jako wynik findFirst, max, min.</p>
 *
 * <p>Kolejność czytania:</p>
 * <ol>
 *   <li>Optional01Basics — problem z null, tworzenie (of, ofNullable, empty), sprawdzanie, wyjmowanie wartości
 *       (get, orElse, orElseGet, orElseThrow), ifPresent, OptionalInt i OptionalDouble</li>
 *   <li>Optional02Transform — przekształcanie bez ifów: map, flatMap, filter, or, stream, łańcuchy wywołań,
 *       opakowywanie starych API zwracających null</li>
 *   <li>Optional03BestPractices — kiedy używać Optional, a kiedy nie (pola, parametry, kolekcje),
 *       projektowanie metod find/get, najczęstsze błędy</li>
 * </ol>
 *
 * <p>SŁÓWKA: optional = opcjonalny; present = obecny; empty = pusty; of = z (wartości); nullable = mogący być null;
 * orElse = albo (w przeciwnym razie); get = pobierz; throw = rzuć; if present = jeśli obecny;
 * map = przekształć (mapuj); flatMap = przekształć i spłaszcz; filter = filtruj; find = znajdź.</p>
 */
package t14_optional;
