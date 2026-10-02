/**
 * TEMAT: t22_design_patterns — wzorce projektowe w Javie
 *        (design pattern = wzorzec projektowy, sprawdzony schemat rozwiązania typowego problemu)
 *
 * <p>Wzorzec projektowy to nie biblioteka i nie gotowy kod do skopiowania, tylko nazwany pomysł na
 * rozwiązanie problemu, który wraca w wielu programach. Nazwy wzorców tworzą wspólny język zespołu:
 * zamiast tłumaczyć „klasa, która tworzy obiekty i ukrywa, którą klasę wybrała”, mówisz „fabryka”.
 * Katalog wzorców opisali w 1994 roku czterej autorzy (Gang of Four, GoF — „banda czworga”).
 * Każda lekcja tego działu uczy jednego wzorca na realistycznej domenie (sklep, zamówienia, faktury,
 * powiadomienia, dokumenty), a nie na zabawkowych przykładach.</p>
 *
 * <p>Wymagania wstępne: dział t01–t21, w szczególności klasy i interfejsy (t06–t07), enum i rekordy
 * (t08–t09), wyjątki (t10), generyki (t11), kolekcje (t12), lambdy (t13), Optional (t14), strumienie
 * (t16), refleksja i proxy (t19), podstawy współbieżności (t21).</p>
 *
 * <p>Jak czytać wzorce — każda lekcja ma ten sam układ:</p>
 * <ol>
 *   <li>PROBLEM: kod BEZ wzorca i to, co w nim boli (nowe wymaganie zmusza do zmian w wielu miejscach).</li>
 *   <li>Wzorzec krok po kroku: role (nazwy z książki GoF po angielsku i po polsku) i mały schemat.</li>
 *   <li>Wersja NOWOCZESNA: lambdy, interfejsy funkcyjne, enum, rekordy, sealed — i kiedy klasyczna wersja jest lepsza.</li>
 *   <li>Gdzie już go spotkałeś: w JDK (Comparator, StringBuilder, List.of, Iterator...) i w Springu.</li>
 *   <li>PUŁAPKA: nadużycie (wzorzec tam, gdzie wystarczy if — zasada YAGNI) i typowe błędy.</li>
 *   <li>Testowalność: jak wzorzec ułatwia test jednostkowy (mała atrapa, wynik deterministyczny).</li>
 *   <li>Tabela „kiedy używać, a kiedy nie”.</li>
 * </ol>
 *
 * <p>Zasady lektury: nie ucz się wzorców na pamięć jako „przepisów do zastosowania”. Najpierw poczuj ból
 * problemu, potem poznaj lekarstwo. Wzorzec ma sens dopiero wtedy, gdy ten ból naprawdę istnieje. Jeśli
 * wystarczy zwykła metoda lub if — użyj ich. Wiele wzorców jest dziś zapisywanych krócej (lambda zamiast
 * klasy), ale idea pozostaje ta sama, a nazwa pozwala się z kolegami zrozumieć.</p>
 *
 * <p>Kolejność lektury:</p>
 * <ol>
 *   <li>Patterns01Strategy — wymienny algorytm (rabaty, koszty dostawy, Comparator)</li>
 *   <li>Patterns02Builder — składanie złożonego obiektu krok po kroku (builder, toBuilder, step builder)</li>
 *   <li>Patterns03Factory — kto i jak tworzy obiekty (metody wytwórcze, fabryki, rejestr)</li>
 *   <li>Patterns04Singleton — jedna instancja, jej pułapki i alternatywa w postaci kontenera DI</li>
 *   <li>Patterns05TemplateMethod — stały szkielet algorytmu i zmienne kroki (zasada Hollywood)</li>
 *   <li>Patterns06Observer — powiadamianie zainteresowanych o zmianie (listenery, zdarzenia)</li>
 *   <li>Patterns07Decorator — dokładanie zachowania bez mnożenia podklas (opakowywanie)</li>
 *   <li>Patterns08DependencyInjection — zależności z zewnątrz, kompozycja w main, atrapy w testach</li>
 *   <li>Patterns09Command — operacje jako obiekty (cofnij/ponów, kolejka poleceń)</li>
 *   <li>Patterns10Facade — jedna prosta fasada przed wieloma podsystemami</li>
 *   <li>Patterns11Adapter — dopasowanie niezgodnych interfejsów (granice systemu)</li>
 *   <li>Patterns12Composite — drzewo części traktowane tak samo jak pojedyncza część</li>
 *   <li>Patterns13State — zachowanie zależne od stanu (cykl życia zamówienia)</li>
 *   <li>Patterns14ChainOfResponsibility — łańcuch obsługujących (walidacja, filtry)</li>
 *   <li>Patterns15Visitor — nowe operacje na stałej hierarchii klas; zestawienie wszystkich wzorców</li>
 * </ol>
 *
 * <p>SŁÓWKA: pattern = wzorzec; strategy = strategia; builder = budowniczy; factory = fabryka; singleton =
 * jedyny egzemplarz; template method = metoda szablonowa; observer = obserwator; decorator = dekorator;
 * dependency injection = wstrzykiwanie zależności; command = polecenie; facade = fasada; adapter = adapter
 * (przejściówka); composite = kompozyt; state = stan; chain of responsibility = łańcuch zobowiązań;
 * visitor = odwiedzający; YAGNI = nie będzie ci to potrzebne.</p>
 */
package t22_design_patterns;
