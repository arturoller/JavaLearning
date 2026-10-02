/**
 * <pre>
 * TEMAT: t28_networking_http — sieć i HTTP w czystej Javie
 *        (networking = praca w sieci; socket = gniazdo; client = klient; server = serwer)
 * </pre>
 *
 * <p>Ten dział pokazuje, jak programy rozmawiają przez sieć: od gniazd TCP i UDP, przez adresy URI,
 * aż po klienta HTTP (Java 11+), własny serwer HTTP i wymianę danych w formacie JSON.
 * Wszystko działa wyłącznie na komputerze lokalnym (adres 127.0.0.1, port wybierany przez system) —
 * lekcje nie potrzebują internetu, a numery portów nigdy nie są wypisywane.</p>
 *
 * <p>Wymagania wstępne: t18_io_files (strumienie, kodowanie znaków), t21_concurrency (wątki, executory,
 * CompletableFuture) oraz t13_lambdas.</p>
 *
 * <p>Kolejność czytania:</p>
 * <ol>
 *   <li>Net01SocketsTcpUdp — adres IP, port, gniazda TCP i UDP, prosty protokół tekstowy</li>
 *   <li>Http01UriUrl — budowa adresu URI, kodowanie znaków w adresie, metody i kody statusu HTTP</li>
 *   <li>Http02HttpClient — klient HttpClient: żądania, odpowiedzi, przekierowania, limity czasu</li>
 *   <li>Http03LocalServer — własny serwer HttpServer i małe API REST w pamięci</li>
 *   <li>Http04JsonApi — JSON między klientem a serwerem, kody 400/404/201, idempotentność</li>
 *   <li>Http05AsyncTimeouts — sendAsync, równoległe żądania, ponawianie, anulowanie, ograniczanie współbieżności</li>
 * </ol>
 *
 * <p>SŁÓWKA: network = sieć; socket = gniazdo; port = port (numer „drzwi” w komputerze);
 * request = żądanie; response = odpowiedź; header = nagłówek; body = treść (ciało); timeout = limit czasu;
 * loopback = pętla zwrotna (adres własnego komputera).</p>
 */
package t28_networking_http;
