/**
 * TEMAT: t32_junit_mockito — JUnit 5, AssertJ, Mockito i TDD w praktyce.
 *
 * <p>Dział przenosi ręczne testy z t25_testing na prawdziwe narzędzia: JUnit 5 (struktura i uruchamianie testów),
 * testy sparametryzowane, AssertJ (czytelne asercje), Mockito (atrapy zależności) oraz pracę metodą TDD
 * (czerwony → zielony → refaktoryzacja). Klasy testowe są zagnieżdżone w lekcjach, a main uruchamia je
 * programowo przez JUnit Platform Launcher i wypisuje po jednej linii na test — każdą klasę testową można też
 * uruchomić w IntelliJ zieloną strzałką. W prawdziwym projekcie testy leżą w src/test/java i uruchamia je
 * Maven Surefire (mvn test).</p>
 *
 * <p>Wymagania wstępne: t06–t07 (klasy, interfejsy), t13_lambdas, t14_optional, t15_numbers (BigDecimal),
 * t16_streams, t25_testing (po co testy, ręczne asercje, dublery), t22_design_patterns (wstrzykiwanie zależności),
 * t30_build_modules (Maven).</p>
 *
 * <p>Kolejność czytania:</p>
 * <ol>
 *   <li>JUnit01Basics — budowa testu, asercje, cykl życia, @Nested, @Tag, założenia, niezależność testów</li>
 *   <li>JUnit02Parameterized — @ParameterizedTest i źródła danych, wartości brzegowe, testy dynamiczne</li>
 *   <li>JUnit03AssertJ — płynne asercje: teksty, liczby, BigDecimal, kolekcje, wyjątki, SoftAssertions</li>
 *   <li>JUnit04Mockito — dublery testowe, when/verify, dopasowywacze, ArgumentCaptor, spy, MockitoExtension</li>
 *   <li>JUnit05Tdd — cykl TDD krok po kroku, zapachy testów, pokrycie kodu, testy w Spring Boot</li>
 * </ol>
 *
 * <p>SŁÓWKA: test = test; assertion = asercja (sprawdzenie); parameterized = sparametryzowany; mock = atrapa;
 * stub = zaślepka; fake = podróbka; spy = szpieg; verify = zweryfikuj; launcher = „odpalacz” testów;
 * listener = słuchacz; red/green = czerwony/zielony; refactor = refaktoryzować; coverage = pokrycie kodu.</p>
 */
package t32_junit_mockito;
