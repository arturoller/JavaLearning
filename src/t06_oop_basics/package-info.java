/**
 * TEMAT: t06_oop_basics — podstawy programowania obiektowego (OOP = Object-Oriented Programming,
 * programowanie zorientowane obiektowo).
 *
 * <p>Ten pakiet uczy myśleć obiektami: klasa to plan, obiekt to egzemplarz zbudowany według planu.
 * Poznasz pola i metody, konstruktory, hermetyzację (ukrywanie danych za metodami), składowe statyczne,
 * metody odziedziczone po klasie Object (toString, equals, hashCode), obiekty niezmienne, klasy
 * zagnieżdżone, pakiety z modyfikatorami dostępu oraz obiekty wartości (value objects). Każda lekcja to
 * jeden plik — klasy przykładowe są w niej statycznymi klasami zagnieżdżonymi (w prawdziwym projekcie
 * każda klasa leży w osobnym pliku).</p>
 *
 * <p>Wymagania wstępne: t01_basics (typy, zmienne, przekazywanie przez wartość), t02_controlflow,
 * t03_arrays, t04_strings, t05_methods (metody, przeciążanie, rekurencja). Kolekcji (t12_collections)
 * jeszcze nie znasz — tam, gdzie pojawia się List albo HashSet, jest to krótka zapowiedź.</p>
 *
 * <p>Kolejność czytania:</p>
 * <ol>
 *   <li>Oop01ClassesObjects — klasa i obiekt, pola, metody, referencje, alias, null</li>
 *   <li>Oop02Constructors — konstruktory, this(...), walidacja, kolejność inicjalizacji</li>
 *   <li>Oop03Encapsulation — pola prywatne, gettery i settery, niezmienniki, modyfikatory dostępu</li>
 *   <li>Oop04Static — pola i metody statyczne, stałe, klasy narzędziowe, import statyczny</li>
 *   <li>Oop05ObjectMethods — toString, equals, hashCode i ich kontrakty</li>
 *   <li>Oop06Immutability — obiekty niezmienne, kopie obronne, metody „with”</li>
 *   <li>Oop07NestedClasses — klasy zagnieżdżone, wewnętrzne, lokalne i anonimowe</li>
 *   <li>Oop08PackagesAccess — pakiety, importy, konflikty nazw, widoczność pakietowa</li>
 *   <li>Oop09ValueObjects — obiekty wartości kontra encje, fabryki of, walidacja</li>
 * </ol>
 *
 * <p>SŁÓWKA: class = klasa; object = obiekt; instance = egzemplarz (instancja); field = pole;
 * method = metoda; constructor = konstruktor; reference = referencja; encapsulation = hermetyzacja;
 * getter = metoda pobierająca; setter = metoda ustawiająca; static = statyczny (należy do klasy);
 * immutable = niezmienny; nested class = klasa zagnieżdżona; package = pakiet; access modifier = modyfikator
 * dostępu; value object = obiekt wartości; entity = encja (obiekt z tożsamością).</p>
 */
package t06_oop_basics;
