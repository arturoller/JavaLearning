/**
 * TEMAT: t02_controlflow — sterowanie przepływem programu (control flow = przepływ sterowania)
 *
 * <p>Do tej pory program wykonywał się linijka po linijce, z góry na dół. W tym rozdziale uczysz się
 * decydować (if / else, switch) i powtarzać (pętle for, while, do-while), a także przerywać pętle
 * (break, continue, etykiety). Na końcu poznajesz gotowe „wzorce pętli”, które wracają w każdym
 * programie: suma, licznik, minimum/maksimum, szukanie pierwszego pasującego elementu, walidacja danych.</p>
 *
 * <p>Wymagania wstępne: t01_basics — typy proste, zmienne, operatory (także logiczne i operator ?:),
 * rzutowanie, klasy opakowujące, Math i Random, Scanner, printf. Tablice znasz na razie tylko z nazwy
 * (pełne omówienie w t03_arrays).</p>
 *
 * <p>Kolejność czytania:</p>
 * <ol>
 *   <li>Control01IfElse — if / else if / else, klamry, equals na tekstach, klauzule strażnika</li>
 *   <li>Control02Switch — klasyczny switch z break, fall-through, switch jako wyrażenie (Java 14+)</li>
 *   <li>Control03Loops — for, while, do-while, for-each, błąd o jeden, pętla nieskończona</li>
 *   <li>Control04BreakContinueLabels — break, continue, etykiety w pętlach zagnieżdżonych</li>
 *   <li>Control05LoopPatterns — akumulatory, min/max, zliczanie, FizzBuzz, walidacja wejścia, menu tekstowe</li>
 * </ol>
 *
 * <p>SŁÓWKA: control flow = przepływ sterowania; condition = warunek; branch = gałąź; loop = pętla;
 * iteration = iteracja (jeden obrót pętli); break = przerwij; continue = kontynuuj (pomiń resztę obrotu);
 * label = etykieta; switch = przełącznik; case = przypadek; default = domyślny; guard clause = klauzula strażnika;
 * off-by-one = błąd o jeden; accumulator = akumulator (zmienna zbierająca wynik).</p>
 */
package t02_controlflow;
