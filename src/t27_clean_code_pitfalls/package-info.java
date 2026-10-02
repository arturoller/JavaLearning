/**
 * TEMAT: t27_clean_code_pitfalls — pułapki Javy i czysty kod
 *
 * <p>Dział w dwóch częściach. Najpierw katalog klasycznych pułapek języka Java — zachowań, które zaskakują nawet
 * doświadczonych programistów (Pitfalls01Classic, Pitfalls02CodeReview). Potem zasady pisania czytelnego,
 * łatwego w utrzymaniu kodu, łącznie z zasadami SOLID na jednym większym przykładzie (CleanCode01Principles,
 * CleanCode02Solid).</p>
 *
 * <p>Wymaga: t01–t24 (podstawy Javy SE, OOP, wyjątki, generyki, kolekcje, lambdy, {@code Optional}, strumienie,
 * {@code java.time}, We/Wy, adnotacje, współbieżność, wzorce projektowe) oraz t25_testing (niektóre przykłady
 * nawiązują do testowania).</p>
 *
 * <p>Kolejność czytania:</p>
 * <ol>
 *   <li>Pitfalls01Classic — ok. 20 klasycznych pułapek Javy: porównania, przepełnienia, kolekcje, BigDecimal, Locale</li>
 *   <li>Pitfalls02CodeReview — typowe uwagi z code review: PRZED/PO z wyjaśnieniem, dlaczego to ma znaczenie</li>
 *   <li>CleanCode01Principles — nazewnictwo, małe funkcje, DRY/KISS/YAGNI, command-query separation, fail fast</li>
 *   <li>CleanCode02Solid — SOLID krok po kroku na jednym przykładzie (moduł fakturowania)</li>
 *   <li>CleanCode03Architecture — od jednej klasy do aplikacji: warstwy, porty i adaptery, podstawy DDD</li>
 * </ol>
 *
 * <p>SŁÓWKA: code review = przegląd kodu; code smell = odór kodu (sygnał problemu); refactoring = refaktoryzacja;
 * guard clause = klauzula strażnicza (wczesny warunek kończący metodę); magic number = liczba magiczna; boolean
 * flag parameter = parametr-flaga logiczna; parameter object = obiekt parametrów; Law of Demeter = zasada
 * Demeter (nie „gadaj” z obcymi obiektami przez łańcuch getterów); SOLID = pięć zasad projektowania obiektowego
 * (SRP, OCP, LSP, ISP, DIP).</p>
 */
package t27_clean_code_pitfalls;
