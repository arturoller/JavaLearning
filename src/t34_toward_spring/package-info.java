/**
 * TEMAT: t34_toward_spring — most do Springa: mechanizmy Springa zbudowane w czystej Javie.
 *
 * <p>Dział buduje w miniaturze to, co Spring i Spring Boot robią automatycznie: kontener IoC
 * z wstrzykiwaniem zależności i proxy transakcyjnym, podział na warstwy (kontroler, serwis, repozytorium),
 * mini-framework REST na HttpServer z JDK oraz auto-konfigurację, konfigurację zewnętrzną, actuator
 * i repozytoria w stylu Spring Data. Bez żadnej zależności od Springa — każda sekcja kończy się komentarzem
 * „W Springu:” z odpowiednikiem w prawdziwym frameworku. Po tym dziale kurs SpringLearning
 * (https://github.com/arturoller/SpringLearning) będzie wyglądał znajomo.</p>
 *
 * <p>Wymagania: adnotacje, refleksja i dynamiczne proxy (t19_annotations_reflection), wzorzec wstrzykiwania
 * zależności (t22_design_patterns), HTTP (t28_networking_http), JDBC i transakcje (t29_jdbc_databases),
 * testy (t32_junit_mockito), Properties (t18_io_files).</p>
 *
 * <p>Kolejność czytania:</p>
 * <ol>
 *   <li>Spring01IocContainer — własny kontener IoC: @Component, wstrzykiwanie przez konstruktor, @Value,
 *       zakresy, cykl życia, @Primary/@Qualifier, wykrywanie cykli, proxy @Transactional</li>
 *   <li>Spring02Layers — warstwy, DTO a encja, walidacja na brzegu, granica transakcji, tłumaczenie wyjątków</li>
 *   <li>Spring03RestConcepts — zasady REST, routing z adnotacji, JSON z rekordów, obsługa błędów, HttpServer + HttpClient</li>
 *   <li>Spring04WhatSpringGives — startery, auto-konfiguracja, warstwy konfiguracji, profile, /actuator/health,
 *       Spring Data z nazw metod, testy, jak zacząć projekt Spring Boot</li>
 * </ol>
 *
 * <p>SŁÓWKA: IoC (Inversion of Control) = odwrócenie sterowania; bean = obiekt zarządzany przez kontener;
 * dependency injection = wstrzykiwanie zależności; scope = zakres; proxy = pośrednik; layer = warstwa;
 * DTO = obiekt do przenoszenia danych; endpoint = punkt końcowy API; auto-configuration = automatyczna
 * konfiguracja; profile = profil; starter = zestaw startowy zależności.</p>
 */
package t34_toward_spring;
