/**
 * TEMAT: t26_jvm — JVM od środka: pamięć, ładowanie klas, odśmiecanie (GC) i narzędzia diagnostyczne
 *
 * <p>Do tej pory pisałeś kod, który „po prostu działał”. Ten pakiet pokazuje, co robi z nim maszyna wirtualna
 * Javy (JVM = Java Virtual Machine) — konkretnie HotSpot, czyli JVM z OpenJDK/Temurin, na przykładzie Javy 17.
 * Zobaczysz, gdzie leżą zmienne i obiekty (stos, sterta), kiedy dokładnie klasa zostaje wczytana i zainicjalizowana,
 * jak odśmiecacz (GC = garbage collector) decyduje, co usunąć, oraz jakimi narzędziami z JDK zajrzeć do działającego
 * programu, gdy jest wolny albo „puchnie” w pamięci.</p>
 *
 * <p>Ważna zasada tego pakietu: wiele liczb (zajęta pamięć, czas pauz GC, numer procesu, głębokość stosu) zależy od
 * uruchomienia, flag JVM i maszyny. Lekcje wypisują w linii WYNIK tylko fakty pewne (np. {@code maxMemory() > 0 → true}),
 * a wartości zmienne oznaczają komentarzem „wynik zależy od uruchomienia”.</p>
 *
 * <p>Wymagania wstępne: t05_methods (stos wywołań, rekurencja), t06_oop_basics (static, obiekty), t10_exceptions,
 * t12_collections (HashMap, equals/hashCode), t16_streams, t21_concurrency (wątki), t22_design_patterns (Singleton).</p>
 *
 * <p>Kolejność czytania:</p>
 * <ol>
 *   <li>Jvm01Memory — stos a sterta, ramki, StackOverflowError, pula napisów, rozmiary obiektów, wycieki pamięci</li>
 *   <li>Jvm02ClassLoadingInit — cykl życia klasy, kolejność inicjalizacji static, stałe wklejane przez kompilator,
 *       idiom holdera, class loadery</li>
 *   <li>Jvm03GarbageCollection — osiągalność i korzenie GC, pokolenia, G1, pauzy, System.gc(), Cleaner,
 *       referencje słabe/miękkie/fantomowe</li>
 *   <li>Jvm04ToolsProfiling — jps, jcmd, jstack, jmap, jstat, JFR, JConsole/VisualVM, JIT, MXBeany z kodu,
 *       diagnoza wolnej aplikacji i wycieku</li>
 * </ol>
 *
 * <p>SŁÓWKA: JVM (Java Virtual Machine) = maszyna wirtualna Javy; HotSpot = domyślna implementacja JVM w OpenJDK;
 * stack = stos; heap = sterta; frame = ramka; garbage collector (GC) = odśmiecacz; reachable = osiągalny;
 * class loader = ładowacz klas; initialization = inicjalizacja; JIT (just-in-time) compiler = kompilator „w locie”;
 * profiling = profilowanie (mierzenie, gdzie program traci czas i pamięć); heap dump = zrzut sterty;
 * thread dump = zrzut wątków; flag = flaga (opcja uruchomienia JVM).</p>
 */
package t26_jvm;
