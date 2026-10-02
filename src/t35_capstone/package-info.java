/**
 * TEMAT: t35_capstone — projekty końcowe (capstone = zwieńczenie, projekt dyplomowy)
 *
 * <p>Cztery małe, ale kompletne aplikacje, w których łączysz wszystko, czego nauczył Cię kurs: rekordy i typy
 * zamknięte (sealed), kolekcje, strumienie, Optional, BigDecimal, daty z {@code Clock}, pliki, wątki, HTTP i JSON.
 * Każdy projekt powstaje w tych samych krokach: (1) wymagania i przykłady, (2) model domeny z walidacją,
 * (3) logika jako czyste metody, które łatwo testować, (4) infrastruktura (pliki, HTTP, wątki) na brzegach,
 * schowana za interfejsami, (5) testy zachowania przez {@code Check}, (6) „co dalej” — jak to samo wyglądałoby
 * w Springu (kurs SpringLearning) i które lekcje warto powtórzyć.</p>
 *
 * <p>Wymagania wstępne: cały kurs t01–t34. Szczególnie przydadzą się t09_records, t12_collections, t16_streams,
 * t15_numbers, t17_datetime, t18_io_files, t21_concurrency, t28_networking_http i t34_toward_spring.</p>
 *
 * <p>Kolejność czytania:</p>
 * <ol>
 *   <li>Capstone01Library — wypożyczalnia książek: reguły, terminy i opłaty, kolejka rezerwacji, polecenia tekstowe, zapis do pliku</li>
 *   <li>Capstone02SalesReport — raport sprzedaży z pliku CSV: walidacja, agregacje BigDecimal, raport tekstowy i JSON</li>
 *   <li>Capstone03WeatherStations — stacje pogodowe: producent–konsument, pula wątków, bezpieczne statystyki, alarmy</li>
 *   <li>Capstone04RestTodo — usługa REST „lista zadań” na HttpServer + klient HttpClient i przełożenie na Spring Boot</li>
 * </ol>
 *
 * <p>SŁÓWKA: capstone = zwieńczenie; requirement = wymaganie; domain = dziedzina; infrastructure = infrastruktura;
 * edge = brzeg; round trip = podróż w obie strony (zapis i odczyt); report = raport; endpoint = punkt końcowy usługi.</p>
 */
package t35_capstone;
