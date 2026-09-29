/**
 * TEMAT: t11_generics — TYPY GENERYCZNE (generics): klasy i metody z parametrem typu, np. {@code List<String>}.
 *
 * <p>Generyki pozwalają napisać kod RAZ, a używać go z różnymi typami — i to bez rzutowania, z kontrolą typów
 * przez kompilator. {@code List<String>} to lista, do której kompilator nie wpuści liczby, a {@code get(0)} od razu
 * zwraca String.</p>
 *
 * <p>Wymagania: t06 (klasy), t07 (dziedziczenie i interfejsy), t06_oop_basics/Oop05ObjectMethods (equals/hashCode).</p>
 *
 * <p>Kolejność czytania:</p>
 * <ol>
 *   <li>Generics01Why — po co generyki: surowe typy, rzutowanie, ClassCastException, diament, pułapka remove(int)</li>
 *   <li>Generics02Classes — własne klasy i interfejsy generyczne, kilka parametrów typu</li>
 *   <li>Generics03Methods — metody generyczne i wnioskowanie typu</li>
 *   <li>Generics04Bounded — ograniczenia: {@code T extends Comparable<T>}, kilka ograniczeń</li>
 *   <li>Generics05Wildcards — symbole wieloznaczne ?, ? extends, ? super i zasada PECS</li>
 *   <li>Generics06ErasureLimits — wymazywanie typów i czego generyki nie potrafią</li>
 *   <li>Generics07Repository — praktyka: generyczne repozytorium</li>
 * </ol>
 *
 * <p>SŁÓWKA: generic = generyczny (ogólny, z parametrem typu); type parameter = parametr typu; type argument = argument typu;
 * raw type = surowy typ; bound = ograniczenie; wildcard = symbol wieloznaczny; erasure = wymazywanie.</p>
 */
package t11_generics;
