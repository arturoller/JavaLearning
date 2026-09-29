/**
 * TEMAT: t10_exceptions — WYJĄTKI: jak Java zgłasza błędy, jak je łapać, zgłaszać i projektować.
 *
 * <p>Wyjątek (exception) to obiekt opisujący błąd, który „wylatuje” z metody i leci w górę stosu wywołań,
 * aż ktoś go złapie (catch). Jeśli nikt nie złapie — program kończy się z wypisanym śladem stosu (stack trace).</p>
 *
 * <p>Wymagania: t01–t05 (metody, stos wywołań), t06 (klasy, konstruktory), t07 (dziedziczenie — hierarchia wyjątków).</p>
 *
 * <p>Kolejność czytania:</p>
 * <ol>
 *   <li>Exceptions01Basics — try/catch/finally, throw, odwijanie stosu, hierarchia, najczęstsze wyjątki</li>
 *   <li>Exceptions02CheckedUnchecked — sprawdzane i niesprawdzane, throws, kiedy które</li>
 *   <li>Exceptions03MultiCatch — kilka catch, multi-catch, kolejność, ponowne rzucanie</li>
 *   <li>Exceptions04TryWithResources — automatyczne zamykanie zasobów, AutoCloseable, wyjątki stłumione</li>
 *   <li>Exceptions05CustomExceptions — własne wyjątki: kiedy, jak nazwać, jakie pola</li>
 *   <li>Exceptions06ChainingWrapping — przyczyna (cause), opakowywanie, tłumaczenie wyjątków między warstwami</li>
 *   <li>Exceptions07BestPractices — dobre praktyki i antywzorce (połykanie, wyjątki jako sterowanie)</li>
 * </ol>
 *
 * <p>SŁÓWKA: exception = wyjątek; throw = rzuć; catch = złap; try = spróbuj; finally = na koniec (zawsze);
 * stack trace = ślad stosu; cause = przyczyna; checked = sprawdzany; unchecked = niesprawdzany.</p>
 */
package t10_exceptions;
