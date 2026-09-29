/**
 * TEMAT: t13_lambdas — LAMBDY, interfejsy funkcyjne i referencje do metod.
 *
 * <p>Lambda to krótki zapis „kawałka zachowania”, który przekazujesz metodzie jak zwykłą wartość:
 * warunek do filtrowania, sposób porównania do sortowania, akcję do wykonania dla każdego elementu.
 * Zamiast pisać całą klasę (nazwaną albo anonimową) tylko po to, żeby zawierała jedną metodę,
 * piszesz: parametry, strzałka, wynik. Na lambdach stoją całe streamy (t16_streams), Optional (t14_optional),
 * sortowanie z Comparator, a także nowoczesne biblioteki i frameworki.</p>
 *
 * <p>Zanim zaczniesz, dobrze znać: interfejsy i klasy anonimowe (t06_oop_basics/Oop07NestedClasses,
 * t07_inheritance_polymorphism/Inherit04Interfaces), typy generyczne (t11_generics — lambdy prawie zawsze
 * występują w typach takich jak {@code Predicate<Product>}) oraz kolekcje (t12_collections — list.sort,
 * removeIf, forEach, Map.computeIfAbsent). Wyjątki sprawdzane (t10_exceptions) przydadzą się w Lambda08Pitfalls.
 * Streamów NIE musisz jeszcze znać — ten dział celowo używa zwykłych pętli i metod kolekcji.</p>
 *
 * <p>Kolejność czytania:</p>
 * <ol>
 *   <li>Lambda01FromAnonymousToLambda — przekazywanie zachowania: klasa nazwana → anonimowa → lambda, składnia,
 *       typ docelowy, sort/forEach/removeIf</li>
 *   <li>Lambda02FunctionalInterfaces — interfejs funkcyjny, adnotacja {@code @FunctionalInterface}, własne interfejsy,
 *       lambda jako obiekt</li>
 *   <li>Lambda03JavaUtilFunction — gotowe interfejsy: Predicate, Function, Consumer, Supplier, operatory,
 *       wersje dwuargumentowe i dla typów prostych</li>
 *   <li>Lambda04MethodReferences — referencje do metod ({@code Klasa::metoda}): cztery rodzaje i pułapki</li>
 *   <li>Lambda05Composition — łączenie funkcji: and/or/negate, andThen/compose, składanie komparatorów</li>
 *   <li>Lambda06ClosuresScope — domknięcia: zmienne „effectively final”, this w lambdzie, zasięg nazw</li>
 *   <li>Lambda07HigherOrderFunctions — funkcje wyższego rzędu: fabryki funkcji, currying, leniwość z Supplier,
 *       tablica poleceń, memoizacja</li>
 *   <li>Lambda08Pitfalls — pułapki: wyjątki sprawdzane, przeciążenia, debugowanie, rekurencja, null, var</li>
 * </ol>
 *
 * <p>Co dalej: t14_optional (Optional pełen lambd: map, filter, orElseGet), t15_numbers, a potem t16_streams,
 * gdzie lambdy i referencje do metod są na każdym kroku.</p>
 *
 * <p>SŁÓWKA: lambda = funkcja anonimowa; functional interface = interfejs funkcyjny; method reference = referencja
 * do metody; arrow = strzałka; target type = typ docelowy; predicate = predykat (warunek); supplier = dostawca;
 * consumer = konsument; closure = domknięcie; effectively final = w praktyce ostateczna (nigdy niezmieniana);
 * higher-order function = funkcja wyższego rzędu; compose = złóż.</p>
 */
package t13_lambdas;
