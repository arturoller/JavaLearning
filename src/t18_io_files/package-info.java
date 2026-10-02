/**
 * TEMAT: t18_io_files — pliki i wejście-wyjście (IO) w Javie
 * <p>
 * Ten dział uczy, jak program czyta i zapisuje pliki: ścieżki i katalogi, tekst, CSV, JSON, właściwości,
 * dane binarne, serializację, logowanie, archiwa ZIP i XML. Głównym narzędziem jest nowoczesne NIO.2
 * (pakiet java.nio.file: Path i Files, od Javy 7). Stare klasy z java.io pokazujemy tam, gdzie nadal są potrzebne
 * (strumienie bajtów, Reader i Writer, serializacja) — zawsze z informacją, co jest stare, a co nowe.
 * <p>
 * Wszystkie lekcje pracują w katalogu tymczasowym (helpers.TempDir) i sprzątają po sobie, więc niczego
 * nie psują w projekcie. Wydruki są takie same w Windows i na Linuksie (ścieżki względne z ukośnikiem "/",
 * jawne kodowanie UTF-8, brak wypisywania komunikatów wyjątków IO zależnych od systemu).
 * <p>
 * Wymagana wiedza: działy t01–t17 (obiekty, rekordy, wyjątki z try-with-resources, kolekcje, lambdy, strumienie,
 * BigDecimal, java.time). Współbieżność i refleksja nie są potrzebne.
 * <p>
 * Kolejność czytania:
 * <ol>
 *   <li>Io01PathFiles — Path i Files: ścieżki, tworzenie, kopiowanie, przenoszenie i usuwanie plików</li>
 *   <li>Io02ReadingText — czytanie tekstu: readString, readAllLines, lines, BufferedReader, Scanner, złe kodowanie</li>
 *   <li>Io03WritingText — zapis tekstu: opcje otwarcia, BufferedWriter, PrintWriter, flush i close, bezpieczny zapis</li>
 *   <li>Io04Csv — CSV: wczytywanie z walidacją i numerami linii, cudzysłowy, raport, polski Excel</li>
 *   <li>Io05JsonManual — JSON: własny zapis i parser rekurencyjny, dlaczego w praktyce używa się Jacksona</li>
 *   <li>Io06Properties — pliki .properties: wczytywanie, wartości domyślne, typowana konfiguracja</li>
 *   <li>Io07WalkingDirectories — przeglądanie katalogów: list, walk, find, glob, walkFileTree, usuwanie i kopiowanie drzew</li>
 *   <li>Io08BinaryStreams — strumienie bajtów: bufory, Data*Stream, własny format binarny, kolejność bajtów</li>
 *   <li>Io09Serialization — serializacja obiektów: transient, serialVersionUID, rekordy, bezpieczeństwo</li>
 *   <li>Io10SimpleLogger — własny prosty logger, poziomy logowania, java.util.logging, SLF4J</li>
 *   <li>Io11IoExceptions — wyjątki IO: hierarchia, UncheckedIOException, wyjątki wyciszone, ponawianie</li>
 *   <li>Io12Charsets — kodowania znaków: UTF-8, windows-1250, BOM, krzaczki (mojibake), Normalizer</li>
 *   <li>Io13ZipArchives — ZIP i GZIP: tworzenie i czytanie archiwów, Zip Slip, system plików wewnątrz ZIP</li>
 *   <li>Io14Xml — XML: DOM, StAX, XPath, bezpieczne parsowanie</li>
 * </ol>
 * <p>
 * SŁÓWKA: path = ścieżka; file = plik; directory = katalog; stream = strumień (bajtów lub znaków); reader = czytnik;
 * writer = zapisywacz; charset = kodowanie znaków; buffer = bufor; temporary = tymczasowy; archive = archiwum.
 */
package t18_io_files;
