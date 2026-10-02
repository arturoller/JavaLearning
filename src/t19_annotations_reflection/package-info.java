/**
 * TEMAT: t19_annotations_reflection — adnotacje i refleksja: jak frameworki „czytają” Twój kod
 *
 * <p>Adnotacja (annotation) to etykieta przyklejona do klasy, metody, pola lub parametru. Sama nic nie robi — działa
 * dopiero wtedy, gdy ktoś ją przeczyta: kompilator, procesor adnotacji (podczas kompilacji) albo framework w czasie
 * działania programu za pomocą refleksji (reflection = zaglądanie do budowy klas w trakcie działania programu).
 * W tym dziale piszesz własne adnotacje, czytasz je refleksją, budujesz mini walidator, mini router komend,
 * dynamiczne proxy (zastępcę obiektu) i procesor adnotacji, który generuje kod podczas kompilacji.
 * Dzięki temu Spring, JUnit, Hibernate czy Lombok przestają być „magią”.</p>
 *
 * <p>Wymagania wstępne: programowanie obiektowe (t06, t07), typy generyczne (t11), kolekcje (t12), lambdy (t13),
 * strumienie (t16), wyjątki (t10) i pliki (t18).</p>
 *
 * <p>Kolejność czytania:</p>
 * <ol>
 *   <li>Annotations01BuiltIn — adnotacje wbudowane i meta-adnotacje</li>
 *   <li>Annotations02Custom — własne adnotacje: elementy, retencja, cel, powtarzanie, dziedziczenie</li>
 *   <li>Annotations03ReflectionBasics — refleksja: klasy, pola, metody, konstruktory, rekordy, enumy</li>
 *   <li>Annotations04Validator — mini framework walidacji oparty na adnotacjach</li>
 *   <li>Annotations05MiniFramework — router komend, czyli Spring MVC w miniaturze</li>
 *   <li>Annotations06DynamicProxy — dynamiczne proxy: logowanie, liczniki, cache, transakcje</li>
 *   <li>Annotations07Processors — procesory adnotacji: generowanie kodu podczas kompilacji</li>
 * </ol>
 *
 * <p>SŁÓWKA: annotation = adnotacja; reflection = refleksja; retention = przechowywanie (retencja); target = cel;
 * element = element adnotacji; field = pole; method = metoda; constructor = konstruktor; invoke = wywołaj;
 * accessible = dostępny; proxy = zastępca (pośrednik); handler = obsługa; processor = procesor;
 * metadata = metadane.</p>
 */
package t19_annotations_reflection;
