package t28_networking_http;

import helpers.Check;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Gniazda (sockets) — adres IP, port, TCP i UDP
 *        (socket = gniazdo; port = port; datagram = datagram, czyli osobna paczka danych)
 *
 * W SKRÓCIE:
 *   Każdy program, który rozmawia przez sieć, w głębi używa gniazd. Gniazdo to „koniec kabla”:
 *   adres IP wskazuje komputer, a numer portu wskazuje program na tym komputerze.
 *   TCP daje wiarygodną rozmowę (jak telefon), UDP — pojedyncze paczki bez gwarancji (jak pocztówki).
 *   HTTP, z którego korzystamy w dalszych lekcjach, to zwykły tekst przesyłany przez TCP.
 *
 * ANALOGIA: blok mieszkalny. Adres IP to adres budynku, port to numer mieszkania.
 *   TCP przypomina rozmowę telefoniczną: najpierw ktoś odbiera (nawiązanie połączenia), potem słowa
 *   dochodzą w tej samej kolejności i nic nie ginie. UDP przypomina wrzucanie pocztówek do skrzynek:
 *   szybko i bez ceregieli, ale nikt nie obiecuje, że pocztówka dotrze ani że dotrą w kolejności.
 *
 * JAK TO DZIAŁA:
 *   Serwer:  ServerSocket(port) → accept() czeka na klienta → dostaje Socket → czyta i pisze strumieniami
 *   Klient:  Socket.connect(adres, port) → ten sam Socket: getInputStream() / getOutputStream()
 *
 *   cecha                | TCP (Socket)            | UDP (DatagramSocket)
 *   ---------------------+-------------------------+----------------------------
 *   połączenie           | tak (uścisk dłoni)      | nie
 *   kolejność danych     | zachowana               | dowolna
 *   gwarancja dostarczenia | tak (ponawia utracone)| nie
 *   jednostka danych     | strumień bajtów         | pojedynczy datagram (limit rozmiaru)
 *   typowe użycie        | HTTP, poczta, bazy      | DNS, gry, strumienie wideo
 *
 *   Port 0 przy tworzeniu gniazda = „system operacyjny, wybierz wolny port”. Dzięki temu testy nie
 *   kłócą się o zajęte porty. Numerów portów w lekcjach NIE wypisujemy — zmieniają się przy każdym uruchomieniu.
 *   Zakresy portów: 0–1023 usługi systemowe, 1024–49151 zarejestrowane, 49152–65535 tymczasowe.
 *
 * SŁÓWKA: address = adres; loopback = pętla zwrotna (własny komputer, 127.0.0.1); accept = przyjmij;
 *   connect = połącz; echo = powtórz to, co usłyszałeś; timeout = limit czasu oczekiwania;
 *   half-close = półzamknięcie (zamykamy tylko jeden kierunek); datagram = datagram;
 *   charset = zestaw znaków (kodowanie); backlog = kolejka oczekujących połączeń
 *
 * ZOBACZ TEŻ: t18_io_files/Io12Charsets (kodowanie znaków), t21_concurrency/Concurrency04Executors (executory),
 *   t28_networking_http/Http01UriUrl (następna lekcja: adresy URI)
 * </pre>
 */
public class Net01SocketsTcpUdp {

    /** Adres pętli zwrotnej: komunikacja nie wychodzi poza ten komputer. */
    private static final InetAddress LOOPBACK = InetAddress.getLoopbackAddress(); // getLoopbackAddress = pobierz adres pętli
    /** Zawsze podajemy kodowanie jawnie: UTF-8 (domyślne kodowanie zależy od systemu!). */
    private static final java.nio.charset.Charset UTF_8 = StandardCharsets.UTF_8;
    /** Limit czasu (ms) — hojny, żeby wolne komputery też przechodziły. */
    private static final int TIMEOUT_MS = 2000;

    public static void main(String[] args) throws Exception {
        title("Net01 — gniazda TCP i UDP");

        addressesAndPorts();     // addresses and ports = adresy i porty
        tcpVsUdpTable();         // table = tabela porównawcza
        tcpEchoServer();         // echo server = serwer echa
        manyClients();           // many clients = wielu klientów
        halfCloseAndTimeouts();  // half close = półzamknięcie
        closedPort();            // closed port = zamknięty port
        udpEcho();               // udp echo = echo przez UDP
        httpIsTextOverTcp();     // http is text over tcp = HTTP to tekst przez TCP
        wildcardVsLoopback();    // wildcard = „wszystkie interfejsy”
        exercises();             // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. ADRESY I PORTY
    // =================================================================================================

    /**
     * 1. InetAddress to adres IP, InetSocketAddress to adres IP razem z portem. Tworzymy je bez
     * zapytań do DNS (DNS = internetowa „książka telefoniczna” zamieniająca nazwy na adresy) —
     * dlatego lekcja działa bez internetu.
     */
    static void addressesAndPorts() throws UnknownHostException {
        section("1. Adres IP, port i pętla zwrotna (loopback)");

        show("to adres pętli zwrotnej?", LOOPBACK.isLoopbackAddress()); // isLoopbackAddress = czy to pętla zwrotna
        // WYNIK: to adres pętli zwrotnej? → true

        // getByAddress = pobierz z bajtów. Cztery bajty to adres IPv4 (127.0.0.1), nic nie jest rozwiązywane przez DNS.
        InetAddress fixed = InetAddress.getByAddress(new byte[] {127, 0, 0, 1});
        show("adres zbudowany z czterech bajtów", fixed.getHostAddress()); // getHostAddress = adres jako tekst
        // WYNIK: adres zbudowany z czterech bajtów → 127.0.0.1

        InetSocketAddress socketAddress = new InetSocketAddress(fixed, 8080); // adres + port
        show("port", socketAddress.getPort()); // getPort = pobierz port
        // WYNIK: port → 8080
        show("host (bez pytania DNS)", socketAddress.getHostString()); // getHostString = nazwa lub adres bez DNS
        // WYNIK: host (bez pytania DNS) → 127.0.0.1

        // createUnresolved = utwórz „nierozwiązany” — nazwa zostaje tekstem, nikt nie pyta DNS.
        InetSocketAddress unresolved = InetSocketAddress.createUnresolved("example.com", 80);
        show("nierozwiązany adres, host", unresolved.getHostString());
        // WYNIK: nierozwiązany adres, host → example.com
        show("czy nierozwiązany?", unresolved.isUnresolved()); // isUnresolved = czy nierozwiązany
        // WYNIK: czy nierozwiązany? → true

        // PUŁAPKA: numer portu to liczba 0–65535 (16 bitów). Większa wartość to błąd programisty,
        // dlatego dostajemy IllegalArgumentException (a nie błąd sieci).
        expectThrows("port 70000", () -> new InetSocketAddress(fixed, 70000));
        // WYNIK: ✔ port 70000 → rzucono IllegalArgumentException: port out of range:70000

        // Port 0 = „wybierz za mnie wolny port”. Prawdziwy numer odczytujemy z getLocalPort().
        try (ServerSocket server = new ServerSocket(0, 5, LOOPBACK)) { // ServerSocket = gniazdo serwera
            show("przydzielony port jest większy od 0?", server.getLocalPort() > 0); // getLocalPort = port lokalny
            // WYNIK: przydzielony port jest większy od 0? → true
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        // DOBRA PRAKTYKA: w testach i lekcjach zawsze wiąż serwer z portem 0 i adresem pętli zwrotnej.
        // Dlaczego: nie ma konfliktu z innymi programami, a serwer nie jest widoczny w sieci.
    }

    // =================================================================================================
    // 2. TCP KONTRA UDP
    // =================================================================================================

    /**
     * 2. Tabela różnic (ta sama co w nagłówku) jako wydruk — zapamiętaj trzy pytania:
     * czy jest połączenie, czy jest kolejność, czy jest gwarancja.
     */
    static void tcpVsUdpTable() {
        section("2. TCP kontra UDP");

        String[][] rows = {
            {"cecha", "TCP", "UDP"},
            {"połączenie", "tak", "nie"},
            {"kolejność", "zachowana", "dowolna"},
            {"gwarancja dostarczenia", "tak", "nie"},
            {"jednostka danych", "strumień bajtów", "datagram"},
        };
        for (String[] row : rows) {
            note(String.format(Locale.ROOT, "%-24s| %-17s| %s", row[0], row[1], row[2])); // format = sformatuj tekst
        }
        // WYNIK: ℹ cecha                   | TCP              | UDP
        // WYNIK: ℹ połączenie              | tak              | nie
        // WYNIK: ℹ kolejność               | zachowana        | dowolna
        // WYNIK: ℹ gwarancja dostarczenia  | tak              | nie
        // WYNIK: ℹ jednostka danych        | strumień bajtów  | datagram

        // PUŁAPKA: TCP nie zachowuje GRANIC wiadomości. Dwa razy write("ab") może dojść jako jedno
        // read() z „abab” albo cztery razy po jednym bajcie. Dlatego potrzebny jest protokół:
        // my użyjemy prostej zasady „jedna wiadomość = jedna linia zakończona znakiem nowej linii”.
        // UDP jest odwrotnie: granice są zachowane (jeden datagram = jedno odebranie), ale paczka może zginąć.

        // DOBRA PRAKTYKA: wybierz TCP, gdy liczy się poprawność (HTTP, bazy danych); UDP — gdy liczy się
        // szybkość i można zgubić pojedyncze paczki (gry, transmisje na żywo).
    }

    // =================================================================================================
    // 3. SERWER ECHA PRZEZ TCP
    // =================================================================================================

    /**
     * Prosty serwer TCP pracujący w tym samym programie. Protokół tekstowy, linia po linii:
     * <ul>
     *   <li>{@code ECHO tekst} → {@code OK tekst}</li>
     *   <li>{@code UPPER tekst} → {@code OK TEKST}</li>
     *   <li>{@code QUIT} → {@code BYE} i serwer zamyka połączenie</li>
     *   <li>cokolwiek innego → {@code BLAD nieznane polecenie}</li>
     * </ul>
     * Wątek „akceptujący” czeka na klientów, a każdego klienta obsługuje wątek z puli (executor).
     */
    static final class EchoServer implements AutoCloseable { // AutoCloseable = można zamknąć w try-with-resources
        private final ServerSocket serverSocket;
        private final ExecutorService workers = Executors.newCachedThreadPool(runnable -> { // puli wątków roboczych
            Thread thread = new Thread(runnable, "echo-worker");
            thread.setDaemon(true); // daemon = wątek-pomocnik: nie blokuje zakończenia programu
            return thread;
        });
        private final Thread acceptThread;
        private final List<String> log = Collections.synchronizedList(new ArrayList<>()); // log = dziennik poleceń

        EchoServer() throws IOException {
            serverSocket = new ServerSocket(0, 50, LOOPBACK); // 0 = wolny port, 50 = kolejka oczekujących (backlog)
            acceptThread = new Thread(this::acceptLoop, "echo-accept");
            acceptThread.setDaemon(true);
            acceptThread.start();
        }

        int port() {
            return serverSocket.getLocalPort();
        }

        List<String> log() {
            synchronized (log) { // iterowanie po synchronizedList wymaga ręcznej synchronizacji — kopiujemy
                return new ArrayList<>(log);
            }
        }

        private void acceptLoop() {
            try {
                while (true) {
                    Socket client = serverSocket.accept(); // accept = przyjmij; blokuje do przyjścia klienta
                    workers.execute(() -> serve(client));
                }
            } catch (IOException closed) {
                // serverSocket.close() przerywa accept() wyjątkiem — to zwykły sposób zakończenia pętli
            }
        }

        private void serve(Socket client) {
            try (Socket socket = client) { // try-with-resources zamknie gniazdo na pewno
                socket.setSoTimeout(TIMEOUT_MS); // setSoTimeout = ustaw limit czekania na dane przy czytaniu
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));
                PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);
                String line;
                while ((line = in.readLine()) != null) { // readLine = czytaj linię; null = klient zamknął nadawanie
                    log.add(line);
                    String reply = handle(line);
                    out.println(reply); // println z autoflush (true) od razu wypycha dane do sieci
                    if (line.equals("QUIT")) {
                        break;
                    }
                }
            } catch (IOException e) {
                // klient się rozłączył albo minął limit czasu — dla serwera to zwykła sytuacja
            }
        }

        /** Sam protokół jako czysta funkcja — łatwo ją przetestować bez sieci. */
        static String handle(String line) {
            if (line.startsWith("ECHO ")) {
                return "OK " + line.substring(5);
            }
            if (line.startsWith("UPPER ")) {
                return "OK " + line.substring(6).toUpperCase(Locale.ROOT); // ROOT = bez reguł konkretnego języka
            }
            if (line.equals("QUIT")) {
                return "BYE";
            }
            return "BLAD nieznane polecenie";
        }

        @Override
        public void close() {
            try {
                serverSocket.close(); // przerywa accept()
            } catch (IOException ignored) {
                // zamykamy „po cichu” — i tak kończymy pracę
            }
            workers.shutdownNow(); // shutdownNow = zatrzymaj teraz
            try {
                acceptThread.join(TIMEOUT_MS); // join = poczekaj na zakończenie wątku
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // przywracamy flagę przerwania
            }
        }
    }

    /**
     * Klient: łączy się, wysyła komendy (po jednej linii) i po każdej czeka na jedną linię odpowiedzi.
     * Wszystkie operacje mają limity czasu — bez nich błąd sieci mógłby zawiesić program na zawsze.
     */
    static List<String> talk(int port, String... commands) throws IOException {
        List<String> replies = new ArrayList<>();
        try (Socket socket = new Socket()) { // Socket bez argumentów = jeszcze niepołączone
            socket.connect(new InetSocketAddress(LOOPBACK, port), TIMEOUT_MS); // connect z limitem czasu
            socket.setSoTimeout(TIMEOUT_MS);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);
            for (String command : commands) {
                out.println(command);
                replies.add(in.readLine());
            }
        }
        return replies;
    }

    /**
     * 3. Jeden klient rozmawia z serwerem. Polskie litery przechodzą bez uszkodzeń, bo OBIE strony
     * jawnie używają UTF-8.
     */
    static void tcpEchoServer() throws IOException {
        section("3. Serwer echa TCP: protokół ECHO / UPPER / QUIT");

        try (EchoServer server = new EchoServer()) {
            List<String> replies = talk(server.port(), "ECHO Zażółć gęślą jaźń", "UPPER zażółć", "TANIEC", "QUIT");
            showEach("odpowiedzi serwera", replies);
            // WYNIK: odpowiedzi serwera (liczba elementów: 4):
            // WYNIK:    • OK Zażółć gęślą jaźń
            // WYNIK:    • OK ZAŻÓŁĆ
            // WYNIK:    • BLAD nieznane polecenie
            // WYNIK:    • BYE
            show("co serwer zapisał w dzienniku", server.log());
            // WYNIK: co serwer zapisał w dzienniku → [ECHO Zażółć gęślą jaźń, UPPER zażółć, TANIEC, QUIT]
        }

        // PUŁAPKA: znak to nie bajt. „zażółć” ma 6 znaków, ale w UTF-8 zajmuje więcej bajtów
        // (litery z ogonkami to 2 bajty). Jeśli klient i serwer założą różne kodowania, polskie litery się
        // zepsują; dlatego zawsze piszemy InputStreamReader(strumień, UTF_8), nigdy samo InputStreamReader(strumień).
        String word = "zażółć";
        show("liczba znaków", word.length()); // length = długość
        // WYNIK: liczba znaków → 6
        show("liczba bajtów w UTF-8", word.getBytes(UTF_8).length); // getBytes = zamień na bajty
        // WYNIK: liczba bajtów w UTF-8 → 10

        // PUŁAPKA: PrintWriter.println kończy linię znakiem zależnym od systemu (Windows: \r\n, Linux: \n).
        // BufferedReader.readLine rozumie oba, więc u nas działa, ale w „prawdziwym” protokole sieciowym
        // końce linii ustala specyfikacja (HTTP wymaga \r\n) — wtedy piszemy je jawnie.

        // DOBRA PRAKTYKA: w protokole tekstowym ustal: kodowanie (UTF-8), znak końca wiadomości (nowa linia)
        // i limit czasu. Dlaczego: bez tego strony nie wiedzą, kiedy wiadomość się skończyła i jak długo czekać.
    }

    // =================================================================================================
    // 4. WIELU KLIENTÓW NARAZ
    // =================================================================================================

    /**
     * 4. Serwer z jednym wątkiem obsługiwałby klientów po kolei — drugi czekałby, aż pierwszy skończy.
     * Nasz serwer oddaje każde połączenie osobnemu wątkowi z puli, więc trzech klientów rozmawia naraz.
     */
    static void manyClients() throws Exception {
        section("4. Wielu klientów naraz (executor)");

        ExecutorService clients = Executors.newFixedThreadPool(3);
        try (EchoServer server = new EchoServer()) {
            List<Future<List<String>>> futures = new ArrayList<>();
            for (String name : List.of("Ania", "Bartek", "Celina")) {
                Callable<List<String>> task = () -> talk(server.port(), "ECHO " + name, "UPPER " + name, "QUIT");
                futures.add(clients.submit(task)); // submit = zleć zadanie; Future = „odbiór wyniku później”
            }
            for (Future<List<String>> future : futures) { // wyniki odbieramy w kolejności zlecania
                show("odpowiedzi klienta", future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS)); // get = pobierz wynik
            }
            // WYNIK: odpowiedzi klienta → [OK Ania, OK ANIA, BYE]
            // WYNIK: odpowiedzi klienta → [OK Bartek, OK BARTEK, BYE]
            // WYNIK: odpowiedzi klienta → [OK Celina, OK CELINA, BYE]

            List<String> log = server.log();
            Collections.sort(log); // kolejność w dzienniku zależy od wyścigu wątków, więc sortujemy
            show("polecenia w dzienniku", log.size());
            // WYNIK: polecenia w dzienniku → 9
            show("dziennik (posortowany)", log);
            // WYNIK: dziennik (posortowany) → [ECHO Ania, ECHO Bartek, ECHO Celina, QUIT, QUIT, QUIT, UPPER Ania, UPPER Bartek, UPPER Celina]
        } finally {
            clients.shutdownNow(); // zawsze zatrzymujemy pulę — inaczej program mógłby się nie zakończyć
        }

        // PUŁAPKA: kolejność wpisów w dzienniku serwera przy wielu klientach NIE jest stała — wątki
        // ścigają się o dostęp. Jeśli chcesz porównać wynik, posortuj go albo zbieraj wyniki per klient.

        // DOBRA PRAKTYKA: dla każdego klienta osobny wątek (albo pula) i ograniczony rozmiar puli.
        // Dlaczego: nieograniczone tworzenie wątków przy tysiącach połączeń wyczerpie pamięć serwera.
        // (Java 21+: wątki wirtualne pozwalają obsługiwać każde połączenie osobnym, tanim wątkiem.)
    }

    // =================================================================================================
    // 5. PÓŁZAMKNIĘCIE I LIMITY CZASU
    // =================================================================================================

    /**
     * 5. Jak serwer dowie się, że klient skończył pisać, skoro połączenie ma jeszcze zostać otwarte,
     * żeby odebrać odpowiedź? Klient wywołuje shutdownOutput() (zamyka tylko kierunek „do serwera”).
     * Serwer widzi wtedy koniec danych (readLine zwraca null) i odsyła podsumowanie.
     * Bez tego obie strony czekałyby na siebie nawzajem — dopiero limit czasu przerywa to czekanie.
     */
    static void halfCloseAndTimeouts() throws Exception {
        section("5. Półzamknięcie (shutdownOutput) i limity czasu");

        try (ServerSocket listener = new ServerSocket(0, 5, LOOPBACK)) {
            ExecutorService serverThread = Executors.newSingleThreadExecutor();
            try {
                // Serwer obsługuje dwa połączenia: pierwsze „bez półzamknięcia”, drugie „z półzamknięciem”.
                Future<?> serverWork = serverThread.submit(() -> {
                    for (int i = 0; i < 2; i++) {
                        try (Socket client = listener.accept()) {
                            client.setSoTimeout(TIMEOUT_MS);
                            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), UTF_8));
                            int lines = 0;
                            while (in.readLine() != null) { // czyta aż do końca danych albo zamknięcia
                                lines++;
                            }
                            PrintWriter out = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), UTF_8), true);
                            out.println("otrzymano linii: " + lines);
                        } catch (IOException e) {
                            // klient odszedł — kończymy obsługę tego połączenia
                        }
                    }
                    return null;
                });

                // Przypadek A: klient wysyła dane i czeka na odpowiedź BEZ shutdownOutput.
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(LOOPBACK, listener.getLocalPort()), TIMEOUT_MS);
                    socket.setSoTimeout(300); // krótki limit: serwer i tak nic nie odeśle
                    PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));
                    out.println("a");
                    out.println("b");
                    try {
                        in.readLine();
                        note("odpowiedź dotarła (nie powinna)");
                    } catch (SocketTimeoutException e) { // wyjątek: minął limit czekania na dane
                        show("bez shutdownOutput klient czeka, aż minie limit; wyjątek", e.getClass().getSimpleName());
                    }
                }
                // WYNIK: bez shutdownOutput klient czeka, aż minie limit; wyjątek → SocketTimeoutException

                // Przypadek B: ten sam scenariusz z shutdownOutput.
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(LOOPBACK, listener.getLocalPort()), TIMEOUT_MS);
                    socket.setSoTimeout(TIMEOUT_MS);
                    PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));
                    out.println("a");
                    out.println("b");
                    out.println("c");
                    socket.shutdownOutput(); // shutdownOutput = zamknij kierunek wyjściowy (połączenie nadal żyje)
                    show("po shutdownOutput serwer odpowiada", in.readLine());
                    // WYNIK: po shutdownOutput serwer odpowiada → otrzymano linii: 3
                }
                serverWork.get(TIMEOUT_MS, TimeUnit.MILLISECONDS); // upewniamy się, że serwer skończył
            } finally {
                serverThread.shutdownNow();
            }
        }

        // PUŁAPKA: domyślnie odczyt z gniazda NIE ma limitu czasu — readLine() potrafi czekać w nieskończoność,
        // jeśli druga strona milczy (zawieszony serwer, zerwany kabel). To jedna z najczęstszych przyczyn
        // „zawieszonych” aplikacji sieciowych.

        // DOBRA PRAKTYKA: zawsze ustawiaj dwa limity: czas łączenia (connect(adres, limit)) i czas czekania
        // na dane (setSoTimeout). Dlaczego: sieć zawodzi cicho, a program bez limitów nie zauważy awarii.
    }

    // =================================================================================================
    // 6. ZAMKNIĘTY PORT
    // =================================================================================================

    /**
     * 6. Co się dzieje, gdy nikt nie nasłuchuje na porcie? Dla TCP system odrzuca połączenie
     * natychmiast (ConnectException). Wypisujemy TYLKO typ wyjątku — treść komunikatu zależy
     * od systemu i jego języka (inna na Windows, inna na Linuksie).
     */
    static void closedPort() throws IOException {
        section("6. Połączenie z zamkniętym portem");

        int freePort;
        try (ServerSocket temporary = new ServerSocket(0, 1, LOOPBACK)) {
            freePort = temporary.getLocalPort(); // port, który za chwilę przestanie być używany
        }
        try (Socket socket = new Socket()) {
            // Limit 5 s: na niektórych systemach (m.in. Windows) odmowa połączenia bywa zgłaszana wolniej.
            socket.connect(new InetSocketAddress(LOOPBACK, freePort), 5000);
            note("połączono (nie powinno się udać)");
        } catch (IOException e) {
            show("typ wyjątku", e.getClass().getSimpleName()); // getSimpleName = sama nazwa klasy
        }
        // WYNIK: typ wyjątku → ConnectException

        // PUŁAPKA: nie parsuj ani nie porównuj komunikatów wyjątków sieciowych („Connection refused” itd.) —
        // zależą od systemu, wersji i języka. Reaguj na TYP wyjątku (ConnectException, SocketTimeoutException).

        // DOBRA PRAKTYKA: odróżniaj „nikt nie słucha” (ConnectException — od razu) od „nikt nie odpowiada”
        // (SocketTimeoutException — po limicie). Dlaczego: pierwszy błąd zwykle znaczy „serwer wyłączony”,
        // drugi „serwer lub sieć wolne” — to inne decyzje (czy ponawiać, czy zgłosić awarię).
    }

    // =================================================================================================
    // 7. UDP
    // =================================================================================================

    /**
     * Serwer UDP: odbiera datagram, odsyła go z prefiksem {@code UDP:}. Rozmiar bufora odbiorczego
     * jest parametrem, żeby pokazać obcinanie zbyt dużych datagramów.
     */
    static final class UdpEchoServer implements AutoCloseable {
        private final DatagramSocket socket; // DatagramSocket = gniazdo datagramowe (UDP)
        private final Thread thread;

        UdpEchoServer(int bufferSize) throws IOException {
            socket = new DatagramSocket(0, LOOPBACK);
            thread = new Thread(() -> loop(bufferSize), "udp-echo");
            thread.setDaemon(true);
            thread.start();
        }

        int port() {
            return socket.getLocalPort();
        }

        private void loop(int bufferSize) {
            byte[] buffer = new byte[bufferSize];
            while (!socket.isClosed()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length); // packet = paczka (datagram)
                try {
                    socket.receive(packet); // receive = odbierz; blokuje do przyjścia datagramu
                    // getLength = ile bajtów faktycznie przyszło (nie więcej niż bufor!)
                    String text = new String(packet.getData(), packet.getOffset(), packet.getLength(), UTF_8);
                    byte[] reply = ("UDP:" + text).getBytes(UTF_8);
                    socket.send(new DatagramPacket(reply, reply.length, packet.getSocketAddress())); // send = wyślij
                } catch (IOException closed) {
                    break; // gniazdo zamknięte → kończymy
                }
            }
        }

        @Override
        public void close() {
            socket.close(); // przerywa receive() wyjątkiem
            try {
                thread.join(TIMEOUT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /** Klient UDP: wysyła jeden datagram i czeka (z limitem) na jedną odpowiedź. */
    static String udpAsk(int port, String text, int timeoutMs) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) { // bez argumentów: dowolny wolny port lokalny
            socket.setSoTimeout(timeoutMs);
            byte[] data = text.getBytes(UTF_8);
            socket.send(new DatagramPacket(data, data.length, LOOPBACK, port));
            byte[] buffer = new byte[512];
            DatagramPacket answer = new DatagramPacket(buffer, buffer.length);
            socket.receive(answer);
            return new String(answer.getData(), 0, answer.getLength(), UTF_8);
        }
    }

    /**
     * 7. UDP: bez połączenia, każda wiadomość to osobny datagram. Na pętli zwrotnej zwykle nic nie ginie,
     * ale to wyjątek — w prawdziwej sieci datagramy giną, dublują się i zmieniają kolejność.
     */
    static void udpEcho() throws IOException {
        section("7. UDP: datagramy bez gwarancji");

        try (UdpEchoServer server = new UdpEchoServer(512)) {
            show("odpowiedź", udpAsk(server.port(), "Cześć, UDP!", TIMEOUT_MS));
            // WYNIK: odpowiedź → UDP:Cześć, UDP!
        }

        // PUŁAPKA: za mały bufor odbiorczy po cichu OBCINA datagram — bez wyjątku i bez ostrzeżenia.
        // Tu serwer ma bufor 16 bajtów, a wysyłamy 40 bajtów (ASCII: 1 znak = 1 bajt).
        try (UdpEchoServer tiny = new UdpEchoServer(16)) {
            String message = "ABCDEFGHIJ".repeat(4); // repeat = powtórz (Java 11+)
            show("wysłano bajtów", message.getBytes(UTF_8).length);
            // WYNIK: wysłano bajtów → 40
            show("serwer odebrał tylko", udpAsk(tiny.port(), message, TIMEOUT_MS));
            // WYNIK: serwer odebrał tylko → UDP:ABCDEFGHIJABCDEF
        }
        // Teoretyczny maksymalny rozmiar danych w datagramie UDP przez IPv4 to 65507 bajtów
        // (65535 minus 20 bajtów nagłówka IP i 8 bajtów nagłówka UDP). W praktyce trzymaj się
        // około 500–1400 bajtów, żeby datagram nie był dzielony na kawałki w sieci.

        // PUŁAPKA: wysłanie datagramu „w próżnię” nie zgłasza błędu — nadawca nie dowiaduje się, że nikt
        // nie słucha. Jedyna oznaka to brak odpowiedzi w limicie czasu.
        int unusedPort;
        try (DatagramSocket temporary = new DatagramSocket(0, LOOPBACK)) {
            unusedPort = temporary.getLocalPort();
        }
        try {
            udpAsk(unusedPort, "halo?", 300);
            note("odpowiedź nadeszła (nie powinna)");
        } catch (IOException e) {
            show("odpowiedź nie nadeszła w limicie czasu", true);
        }
        // WYNIK: odpowiedź nie nadeszła w limicie czasu → true

        // DOBRA PRAKTYKA: jeśli potrzebujesz niezawodności, a używasz UDP — musisz sam dodać numerowanie,
        // potwierdzenia i ponowienia. Dlaczego: TCP robi to za Ciebie; w UDP odpowiedzialność jest po Twojej stronie.
    }

    // =================================================================================================
    // 8. HTTP TO TEKST PRZEZ TCP
    // =================================================================================================

    /**
     * 8. Protokół HTTP to zwykły tekst: linia żądania, nagłówki, pusta linia, opcjonalna treść.
     * Piszemy „na piechotę” miniaturowy serwer i klienta na samych gniazdach — tak działają
     * przeglądarki i HttpClient (następne lekcje) pod spodem.
     */
    static void httpIsTextOverTcp() throws Exception {
        section("8. HTTP to tekst przesyłany przez TCP");

        List<String> received = Collections.synchronizedList(new ArrayList<>());
        ServerSocket listener = new ServerSocket(0, 5, LOOPBACK);
        Thread serverThread = new Thread(() -> {
            try (Socket client = listener.accept()) {
                client.setSoTimeout(TIMEOUT_MS);
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), UTF_8));
                String requestLine;
                while ((requestLine = in.readLine()) != null && !requestLine.isEmpty()) { // pusta linia kończy nagłówki
                    received.add(requestLine);
                }
                byte[] body = "Cześć, świecie!".getBytes(UTF_8);
                // Content-Length (długość treści) liczymy w BAJTACH, nie w znakach!
                String head = "HTTP/1.1 200 OK\r\n"
                        + "Content-Type: text/plain; charset=utf-8\r\n"
                        + "Content-Length: " + body.length + "\r\n"
                        + "Connection: close\r\n"
                        + "\r\n"; // pusta linia = koniec nagłówków
                OutputStream out = client.getOutputStream();
                out.write(head.getBytes(UTF_8));
                out.write(body);
                out.flush();
            } catch (IOException e) {
                received.add("błąd serwera: " + e.getClass().getSimpleName());
            }
        }, "tiny-http");
        serverThread.setDaemon(true);
        serverThread.start();

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(LOOPBACK, listener.getLocalPort()), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);
            String request = "GET /witaj HTTP/1.1\r\n"      // metoda, ścieżka, wersja protokołu
                    + "Host: localhost\r\n"                 // nagłówek Host (host = gospodarz) jest wymagany w HTTP/1.1
                    + "Connection: close\r\n"
                    + "\r\n";
            OutputStream out = socket.getOutputStream();
            out.write(request.getBytes(UTF_8));
            out.flush();

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));
            show("linia statusu", in.readLine()); // status line = linia statusu
            // WYNIK: linia statusu → HTTP/1.1 200 OK
            List<String> headers = new ArrayList<>();
            String header;
            while ((header = in.readLine()) != null && !header.isEmpty()) {
                headers.add(header);
            }
            showEach("nagłówki odpowiedzi", headers);
            // WYNIK: nagłówki odpowiedzi (liczba elementów: 3):
            // WYNIK:    • Content-Type: text/plain; charset=utf-8
            // WYNIK:    • Content-Length: 18
            // WYNIK:    • Connection: close
            show("treść odpowiedzi", in.readLine()); // serwer zamyka połączenie, więc readLine kończy się po treści
            // WYNIK: treść odpowiedzi → Cześć, świecie!
        } finally {
            listener.close();
            serverThread.join(TIMEOUT_MS);
        }
        showEach("co zobaczył serwer", received);
        // WYNIK: co zobaczył serwer (liczba elementów: 3):
        // WYNIK:    • GET /witaj HTTP/1.1
        // WYNIK:    • Host: localhost
        // WYNIK:    • Connection: close

        // PUŁAPKA: „Cześć, świecie!” to 15 znaków, ale 18 bajtów. Gdyby Content-Length podawał 15,
        // klient odczytałby treść ucięta o 3 bajty. Długość treści zawsze liczymy z tablicy bajtów.

        // DOBRA PRAKTYKA: na co dzień nie pisz HTTP na gniazdach — użyj HttpClient i HttpServer
        // (lekcje Http02HttpClient i Http03LocalServer). Dlaczego: obsługują nagłówki, kodowanie porcjami,
        // ponowne użycie połączeń i setki szczegółów, które łatwo zepsuć ręcznie.
    }

    // =================================================================================================
    // 9. NA KTÓRYM ADRESIE NASŁUCHIWAĆ
    // =================================================================================================

    /**
     * 9. {@code new ServerSocket(port)} bez adresu nasłuchuje na WSZYSTKICH interfejsach sieciowych
     * („wildcard” = znak wieloznaczny, adres 0.0.0.0). Serwer bywa wtedy dostępny z całej sieci —
     * a system Windows pyta o zgodę zapory (firewalla).
     */
    static void wildcardVsLoopback() throws IOException {
        section("9. Nasłuchiwanie: wszystkie interfejsy kontra pętla zwrotna");

        try (ServerSocket everywhere = new ServerSocket(0); // getInetAddress = adres, na którym serwer nasłuchuje
             ServerSocket localOnly = new ServerSocket(0, 5, LOOPBACK)) {
            show("bez adresu: nasłuchuje na wszystkich interfejsach?", everywhere.getInetAddress().isAnyLocalAddress());
            // WYNIK: bez adresu: nasłuchuje na wszystkich interfejsach? → true
            show("z adresem pętli: nasłuchuje na wszystkich interfejsach?", localOnly.getInetAddress().isAnyLocalAddress());
            // WYNIK: z adresem pętli: nasłuchuje na wszystkich interfejsach? → false
        }

        // PUŁAPKA: serwer deweloperski „dla wygody” wystawiony na wszystkie interfejsy może być osiągalny
        // dla innych komputerów w sieci (kawiarnia, biuro) — bez uwierzytelniania to dziura w bezpieczeństwie.

        // DOBRA PRAKTYKA: lokalne narzędzia i testy wiąż z adresem pętli zwrotnej (127.0.0.1).
        // Dlaczego: nikt spoza komputera nie może się połączyć; na zewnątrz wystawiaj tylko świadomie.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • IP = komputer, port = program (0–65535); port 0 przy tworzeniu = „wybierz wolny”.
     *   • TCP: połączenie, kolejność, gwarancja, strumień bajtów BEZ granic wiadomości.
     *     UDP: bez połączenia, bez gwarancji, jeden datagram = jedna wiadomość (limit rozmiaru).
     *   • Serwer: ServerSocket + accept() w pętli; każdego klienta obsługuj w osobnym wątku (puli).
     *   • Zawsze: jawne UTF-8, limity czasu (connect i setSoTimeout), zamykanie w try-with-resources.
     *   • shutdownOutput() mówi „skończyłem pisać”, a odpowiedź nadal można odebrać.
     *   • ConnectException = nikt nie słucha; SocketTimeoutException = nikt nie odpowiada na czas.
     *   • HTTP = tekst: linia żądania, nagłówki, pusta linia, treść; Content-Length liczymy w bajtach.
     *   • Lokalnie wiąż serwery z 127.0.0.1 i portem 0; w komunikatach nie wypisuj numerów portów.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się adres IP od portu? Co oznacza port 0 przy tworzeniu ServerSocket?
     *   2. Dlaczego TCP wymaga protokołu „granic wiadomości”, a UDP nie?
     *   3. Co wypisze:  byte[] b = "łódź".getBytes(StandardCharsets.UTF_8);
     *                   System.out.println("łódź".length() + " " + b.length);  ?
     *   4. ZNAJDŹ BŁĄD:  Socket s = new Socket(host, port);
     *                    BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
     *                    String odpowiedz = in.readLine();   // kilka błędów — wskaż co najmniej dwa
     *   5. Do czego służy shutdownOutput() i co się stanie bez niego, gdy serwer czyta do końca danych?
     *   6. Serwer UDP ma bufor 16 bajtów, a klient wysyła datagram 40 bajtów. Ile bajtów zobaczy
     *      odbiorca i czy poleci wyjątek?
     *   7. Dlaczego nie wypisujemy komunikatów wyjątków sieciowych, tylko ich typy?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** Pomocnik: Check.equal przyjmuje Supplier, który nie może rzucać wyjątków kontrolowanych — opakowujemy je. */
    static <T> T call(Callable<T> action) { // Callable = zadanie zwracające wynik, może rzucić wyjątek
        try {
            return action.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e.getClass().getSimpleName(), e);
        }
    }

    static void exercises() throws IOException {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        try (EchoServer tcp = new EchoServer(); UdpEchoServer udp = new UdpEchoServer(512)) {
            Check.equal("ćw. 1: port 80 nie jest portem użytkownika", false, () -> exercise1(80));
            Check.equal("ćw. 1: port 8080 jest portem użytkownika", true, () -> exercise1(8080));
            Check.equal("ćw. 1: port 60000 nie jest portem użytkownika", false, () -> exercise1(60000));
            Check.equal("ćw. 2: ECHO", "OK Zażółć", () -> call(() -> exercise2(tcp.port(), "ECHO Zażółć")));
            Check.equal("ćw. 2: UPPER", "OK ZAŻÓŁĆ", () -> call(() -> exercise2(tcp.port(), "UPPER zażółć")));
            Check.equal("ćw. 3: UDP", "UDP:Cześć", () -> call(() -> exercise3(udp.port(), "Cześć")));
            Check.equal("ćw. 3: UDP, drugi raz", "UDP:ęąść", () -> call(() -> exercise3(udp.port(), "ęąść")));
            Check.summary();

            section("ĆWICZENIA — rozwiązania wzorcowe");
            Check.equal("ćw. 1 (wzorzec): 80", false, () -> solution1(80));
            Check.equal("ćw. 1 (wzorzec): 8080", true, () -> solution1(8080));
            Check.equal("ćw. 1 (wzorzec): 60000", false, () -> solution1(60000));
            Check.equal("ćw. 2 (wzorzec): ECHO", "OK Zażółć", () -> call(() -> solution2(tcp.port(), "ECHO Zażółć")));
            Check.equal("ćw. 2 (wzorzec): UPPER", "OK ZAŻÓŁĆ", () -> call(() -> solution2(tcp.port(), "UPPER zażółć")));
            Check.equal("ćw. 3 (wzorzec): UDP", "UDP:Cześć", () -> call(() -> solution3(udp.port(), "Cześć")));
            Check.equal("ćw. 3 (wzorzec): UDP, drugi raz", "UDP:ęąść", () -> call(() -> solution3(udp.port(), "ęąść")));
            Check.summary();
            // WYNIK: PODSUMOWANIE: ✔ 7 OK, ✘ 0 BŁĄD
        }
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć true, gdy port należy do zakresu „zarejestrowanych” 1024–49151
     * (włącznie z obiema granicami); w przeciwnym razie false.
     * Podpowiedź: dwa porównania połączone operatorem {@code &&} (i).
     */
    static boolean exercise1(int port) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): połącz się z serwerem echa (adres LOOPBACK, podany port), wyślij jedną linię
     * polecenia i zwróć pierwszą linię odpowiedzi. Użyj UTF-8 i limitów czasu.
     * PRZEPISZ na try-with-resources:
     * <pre>{@code
     * Socket socket = new Socket();
     * try {
     *     socket.connect(new InetSocketAddress(LOOPBACK, port), 2000);
     *     ...
     * } finally {
     *     socket.close();   // trzeba pamiętać; a close() też może rzucić wyjątek
     * }
     * }</pre>
     * Podpowiedź: możesz skorzystać z gotowej metody {@code talk(port, command)}, ale spróbuj napisać
     * połączenie samodzielnie.
     */
    static String exercise2(int port, String command) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): wyślij jeden datagram UDP z tekstem do serwera na podanym porcie
     * i zwróć tekst odpowiedzi. Ustaw limit czasu odbioru i zamknij gniazdo.
     * Podpowiedź: DatagramSocket, DatagramPacket, getBytes(UTF_8), receive(); odpowiedź ma być
     * zbudowana z getLength() bajtów, a nie z całego bufora.
     */
    static String exercise3(int port, String text) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static boolean solution1(int port) {
        return port >= 1024 && port <= 49151;
    }

    static String solution2(int port, String command) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(LOOPBACK, port), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));
            out.println(command);
            return in.readLine();
        }
    }

    static String solution3(int port, String text) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT_MS);
            byte[] data = text.getBytes(UTF_8);
            socket.send(new DatagramPacket(data, data.length, LOOPBACK, port));
            byte[] buffer = new byte[512];
            DatagramPacket answer = new DatagramPacket(buffer, buffer.length);
            socket.receive(answer);
            return new String(answer.getData(), 0, answer.getLength(), UTF_8);
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Adres IP wskazuje komputer (interfejs sieciowy), port — konkretny program (usługę) na nim.
     *      Port 0 = „system operacyjny, wybierz dowolny wolny port”; prawdziwy numer da getLocalPort().
     *   2. TCP to ciągły strumień bajtów — dwa zapisy mogą się zlać w jeden odczyt (i odwrotnie), więc
     *      protokół musi zaznaczać koniec wiadomości (np. znak nowej linii, długość). UDP przekazuje
     *      wiadomości jako osobne datagramy, więc granice są zachowane (ale paczka może zginąć).
     *   3. Wypisze „4 7”: „łódź” ma 4 znaki, a w UTF-8 litery ł, ó i ź zajmują po 2 bajty, a d — 1 bajt
     *      (2 + 2 + 1 + 2 = 7).
     *   4. Błędy: (a) brak jawnego kodowania w InputStreamReader (powinno być , StandardCharsets.UTF_8);
     *      (b) brak limitu czasu (connect z limitem i setSoTimeout); (c) gniazdo nie jest zamykane
     *      (try-with-resources); (d) brak pewności, że serwer odeśle całą linię — trzeba obsłużyć null.
     *   5. shutdownOutput() zamyka tylko kierunek wyjściowy: serwer widzi koniec danych (readLine zwraca
     *      null), a klient nadal może czytać odpowiedź. Bez niego serwer czeka na kolejne dane, a klient
     *      na odpowiedź — wzajemne czekanie, przerwane dopiero limitem czasu (SocketTimeoutException).
     *   6. Odbiorca zobaczy 16 bajtów, wyjątku nie będzie — reszta datagramu jest po cichu odrzucona.
     *   7. Komunikaty zależą od systemu operacyjnego, jego języka i wersji JDK; typ wyjątku jest stabilny
     *      i to na nim należy opierać decyzje (oraz testy).
     */
    // </editor-fold>
}
