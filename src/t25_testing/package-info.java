/**
 * TEMAT: t25_testing — testowanie kodu bez frameworka
 *
 * <p>Dział pokazuje idee testowania automatycznego (piramida testów, Arrange-Act-Assert, zasady FIRST) i uczy
 * budować własny, minimalny „runner” testowy — zanim poznasz prawdziwy framework (JUnit) w dziale
 * t32_junit_mockito. Druga część działu to testy podwójne (dummy/stub/fake/spy/mock) oraz projektowanie klas tak,
 * by dało się je łatwo testować (wstrzykiwanie zależności, {@code Clock} zamiast {@code LocalDate.now()},
 * oddzielenie obliczeń od We/Wy).</p>
 *
 * <p>Wymaga: t01–t24 (podstawy Javy SE, OOP, wyjątki, generyki, kolekcje, lambdy, {@code Optional}, strumienie,
 * {@code java.time}, We/Wy, adnotacje, współbieżność, wzorce projektowe).</p>
 *
 * <p>Kolejność czytania:</p>
 * <ol>
 *   <li>Testing01Concepts — po co testować, piramida testów, Arrange-Act-Assert, zasady FIRST, własny mini-runner</li>
 *   <li>Testing02TestDoubles — testy podwójne: dummy, stub, fake, spy, mock; Clock i Random jako zależności do wstrzyknięcia</li>
 *   <li>Testing03TestableDesign — projektowanie pod testowalność: PRZED (kod trudny do testowania) / PO (łatwy)</li>
 * </ol>
 *
 * <p>SŁÓWKA: unit test = test jednostkowy; integration test = test integracyjny; end-to-end test = test od końca
 * do końca; test double = test podwójny (zamiennik zależności); test runner = program uruchamiający testy;
 * assertion = asercja (sprawdzenie); mock = obiekt sprawdzający oczekiwania; stub = obiekt zwracający ustalone
 * dane; fake = uproszczona, ale działająca implementacja; spy = obiekt zapamiętujący wywołania; dummy = obiekt
 * wypełniający miejsce, nieużywany.</p>
 */
package t25_testing;
