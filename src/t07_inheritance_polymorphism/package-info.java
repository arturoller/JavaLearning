/**
 * <pre>
 * TEMAT: t07_inheritance_polymorphism — dziedziczenie i polimorfizm
 * </pre>
 *
 * <p>Ten pakiet pokazuje, jak klasy dziedziczą po sobie (extends), co dokładnie "dostają" od klasy bazowej,
 * jak nadpisywać metody (override) i jak polimorfizm pozwala jednym kawałkiem kodu obsłużyć wiele różnych
 * typów bez łańcucha instanceof. Poznasz też klasy abstrakcyjne, interfejsy, klasy zapieczętowane (sealed,
 * Java 17), zasadę "kompozycja zamiast dziedziczenia" oraz pięć zasad SOLID na małych przykładach.</p>
 *
 * <p>Wymagania wstępne: t01_basics–t06_oop_basics (klasy, konstruktory, hermetyzacja, static, equals/hashCode,
 * niemutowalność, klasy zagnieżdżone, pakiety, obiekty wartości), t08_enums (enumy), t09_records (rekordy),
 * t10_exceptions (wyjątki), t11_generics (generyki). Z kolekcji potrzebna jest tylko podstawowa znajomość
 * List/ArrayList/Map (pełny rozdział: t12_collections). Lambdy pojawiają się wyłącznie jako zapowiedź
 * (pełny rozdział: t13_lambdas).</p>
 *
 * <p>Kolejność czytania:</p>
 * <ol>
 *     <li>Inherit01Basics — extends, "is-a", co jest dziedziczone, konstruktory bazowe</li>
 *     <li>Inherit02Override — nadpisywanie metod, @Override, typowe pułapki</li>
 *     <li>Inherit03AbstractClasses — klasy i metody abstrakcyjne</li>
 *     <li>Inherit04Interfaces — interfejsy, metody default/static/private</li>
 *     <li>Inherit05Polymorphism — polimorfizm, rzutowanie w górę i w dół</li>
 *     <li>Inherit06CompositionVsInheritance — kompozycja kontra dziedziczenie</li>
 *     <li>Inherit07SealedClasses — klasy zapieczętowane (Java 17)</li>
 *     <li>Inherit08Solid — zasady SOLID na małych przykładach</li>
 * </ol>
 *
 * <p>SŁÓWKA: inheritance = dziedziczenie; polymorphism = polimorfizm; override = nadpisanie;
 * abstract = abstrakcyjny; interface = interfejs; composition = kompozycja; sealed = zapieczętowany;
 * contract = kontrakt (umowa co do zachowania).</p>
 */
package t07_inheritance_polymorphism;
