/**
 * TEMAT: t15_numbers — liczby w praktyce: pieniądze, bardzo duże liczby, formatowanie i sztuczki z int/long
 * (numbers = liczby).
 *
 * <p>Typy double i float są szybkie, ale liczą w systemie dwójkowym, więc 0.1 + 0.2 nie daje dokładnie 0.3.
 * Do pieniędzy używa się BigDecimal (dokładne ułamki dziesiętne) albo liczby groszy w long. Do liczb większych
 * niż long służy BigInteger. W tym pakiecie zobaczysz też, jak wypisywać i wczytywać liczby w polskim formacie
 * (przecinek, spacje między tysiącami) i jak nie dać się zaskoczyć przepełnieniu int.</p>
 *
 * <p>Wymagania wstępne: typy proste i rzutowanie (t01_basics/Basics02PrimitiveTypes, t01_basics/Basics08FloatingPoint),
 * formatowanie napisów (t04_strings/Strings04Formatting), wyjątki (t10_exceptions), rekordy (t09_records),
 * kolekcje (t12_collections) i lambdy (t13_lambdas).</p>
 *
 * <p>Kolejność czytania:</p>
 * <ol>
 *   <li>Numbers01BigDecimal — dlaczego nie double, dzielenie, zaokrąglanie (RoundingMode), skala, equals kontra compareTo</li>
 *   <li>Numbers02MoneyValueObject — własny typ Money: waluta, VAT, raty bez gubienia groszy</li>
 *   <li>Numbers03BigInteger — silnia, potęgi, reszta z dzielenia, liczby pierwsze</li>
 *   <li>Numbers04FormattingParsing — String.format, NumberFormat, DecimalFormat, wczytywanie „1 234,56”</li>
 *   <li>Numbers05IntegerTricks — przepełnienie, Math.addExact, dzielenie z resztą, systemy liczbowe, bity</li>
 *   <li>Numbers06MathCheatsheet — ściągawka klasy Math: zaokrąglanie, floorMod, wersje Exact, ulp, NaN, tabele wyników</li>
 * </ol>
 *
 * <p>SŁÓWKA: number = liczba; decimal = dziesiętny; big = duży; money = pieniądze; value object = obiekt-wartość;
 * rounding = zaokrąglanie; scale = skala (liczba cyfr po przecinku); precision = precyzja (liczba wszystkich cyfr);
 * format = sformatuj; parse = przetwórz tekst na wartość; overflow = przepełnienie; exact = dokładny;
 * locale = ustawienia regionalne; currency = waluta; percent = procent.</p>
 */
package t15_numbers;
