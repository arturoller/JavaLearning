/**
 * TEMAT: t30_build_modules — jak kod Javy staje się działającym programem poza IDE
 *
 * <p>Dział pokazuje drogę od plików .java do programu, który uruchomisz w konsoli, na serwerze albo w CI
 * (ciągłej integracji). Narzędzia JDK (javac, jar, javadoc, jdeps) wołamy prosto z lekcji przez
 * java.util.spi.ToolProvider (dostawca narzędzi), a osobne JVM-y (procesy potomne) uruchamiamy przez
 * ProcessBuilder (budowniczy procesów). Maven jest tu opisany i rozebrany na części, ale lekcje go nie uruchamiają —
 * polecenia ./mvnw znajdziesz w komentarzach.</p>
 *
 * <p>Wymagania wstępne: t18_io_files (pliki, Path, strumienie IO), t19_annotations_reflection (refleksja, javax.tools),
 * t10_exceptions (wyjątki), t13_lambdas i t16_streams (lambdy, strumienie).</p>
 *
 * <ol>
 *   <li>Build01MavenBasics — po co narzędzia budowania, pom.xml, cykl życia, zakresy zależności, Wrapper, Gradle</li>
 *   <li>Build02JarClasspath — javac -d, ścieżka klas (classpath), pliki JAR, manifest, zasoby w JAR-ach</li>
 *   <li>Build03Modules — moduły JPMS: module-info, ścieżka modułów, silna enkapsulacja, ServiceLoader, jdeps</li>
 *   <li>Build04CommandLineApps — aplikacje konsolowe: argumenty, kody wyjścia, stdin/stdout/stderr</li>
 *   <li>Build05ProcessesEnv — procesy potomne, zmienne środowiskowe, właściwości systemowe, ProcessHandle</li>
 *   <li>Build06QualityToolsJavadoc — Javadoc, doclint, ostrzeżenia -Xlint, analiza statyczna, pokrycie kodu, CI</li>
 * </ol>
 *
 * <p>SŁÓWKA: build = budowanie; dependency = zależność; artifact = artefakt (plik wynikowy, np. JAR);
 * classpath = ścieżka klas; module path = ścieżka modułów; manifest = manifest (plik opisu JAR-a);
 * lifecycle = cykl życia; phase = faza; goal = cel (zadanie wtyczki); plugin = wtyczka; scope = zakres;
 * exit code = kod wyjścia; process = proces; environment variable = zmienna środowiskowa;
 * static analysis = analiza statyczna; coverage = pokrycie (kodu testami).</p>
 */
package t30_build_modules;
