package helpers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * <pre>
 * TEMAT: TempDir — katalog tymczasowy dla lekcji o plikach
 *        (temp/temporary = tymczasowy; dir/directory = katalog, folder)
 *
 * W SKRÓCIE:
 *   Lekcje o plikach NIE śmiecą w projekcie. Tworzą katalog w folderze tymczasowym systemu
 *   (Windows: %TEMP%), pracują w nim, a na końcu go kasują.
 *
 * DOBRA PRAKTYKA: zamiana wyjątku sprawdzanego na niesprawdzany z zachowaniem przyczyny.
 *   Files.createTempDirectory rzuca IOException (wyjątek sprawdzany — kompilator zmusza do obsługi).
 *   Tu opakowujemy go w UncheckedIOException (unchecked = niesprawdzany), żeby lekcje nie musiały
 *   wszędzie pisać try/catch. Oryginalny wyjątek zostaje zachowany jako „przyczyna” (cause) — nic nie ginie.
 *   Szczegóły: t10_exceptions/Exceptions06ChainingWrapping.
 *
 * SŁÓWKA:
 *   create = utwórz; delete = usuń; recursively = rekurencyjnie (razem z całą zawartością);
 *   prefix = przedrostek (początek nazwy); walk = przejdź (po drzewie katalogów);
 *   reverse order = odwrotny porządek; resolve = dołącz (część ścieżki).
 * </pre>
 */
public final class TempDir {

    private TempDir() {
    }

    /**
     * create = utwórz. Tworzy nowy, pusty katalog tymczasowy, np. C:\Users\...\Temp\javalearning-io1234567\
     *
     * @param prefix początek nazwy katalogu (reszta jest losowa, żeby nazwy się nie powtarzały)
     * @return ścieżka (Path) do utworzonego katalogu
     */
    public static Path create(String prefix) {
        try {
            return Files.createTempDirectory("javalearning-" + prefix);
        } catch (IOException e) {
            throw new UncheckedIOException("Nie udało się utworzyć katalogu tymczasowego", e);
        }
    }

    /**
     * deleteRecursively = usuń rekurencyjnie. Kasuje katalog RAZEM Z zawartością.
     * <p>
     * Jak to działa:
     * <ol>
     *   <li>Files.walk(dir) — strumień wszystkich ścieżek w drzewie: katalog, jego pliki, podkatalogi...</li>
     *   <li>{@code sorted(Comparator.reverseOrder())} — sortujemy ścieżki MALEJĄCO. Ścieżka pliku
     *       „dir\a\b.txt” jest „większa” od ścieżki jego katalogu „dir\a” (dłuższa, z tym samym początkiem),
     *       więc pliki i podkatalogi trafią PRZED swoimi katalogami. To ważne, bo niepustego katalogu
     *       nie da się usunąć.</li>
     *   <li>try-with-resources zamyka strumień z Files.walk — ten strumień trzyma otwarte uchwyty katalogów.</li>
     * </ol>
     */
    public static void deleteRecursively(Path dir) {
        if (dir == null || !Files.exists(dir)) {   // exists = istnieje
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder())
                 .forEach(path -> {
                     try {
                         Files.delete(path);
                     } catch (IOException e) {
                         throw new UncheckedIOException("Nie udało się usunąć: " + path, e);
                     }
                 });
        } catch (IOException e) {
            throw new UncheckedIOException("Nie udało się przejść po katalogu: " + dir, e);
        }
    }
}
