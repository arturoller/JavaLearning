package helpers;

import java.util.concurrent.ThreadLocalRandom;

/**
 * <pre>
 * TEMAT: Sleep — bezpieczne „uśpienie” wątku na chwilę (do symulowania wolnych operacji)
 *        (sleep = spać; ms = milliseconds = milisekundy; 1000 ms = 1 sekunda)
 *
 * W SKRÓCIE:
 *   Thread.sleep(ms) rzuca wyjątek sprawdzany InterruptedException (interrupted = przerwany),
 *   więc każde użycie wymaga try/catch. Ta klasa robi to raz, a lekcje wołają po prostu Sleep.ms(200).
 *
 * DOBRA PRAKTYKA: po złapaniu InterruptedException PRZYWRÓĆ flagę przerwania:
 *       {@code Thread.currentThread().interrupt();}
 *   Dlaczego? Ktoś (np. ExecutorService.shutdownNow()) poprosił wątek o zakończenie pracy.
 *   Łapiąc wyjątek, „zjedliśmy” tę prośbę — więc ustawiamy ją z powrotem, żeby kod wyżej też ją zobaczył.
 *   Szczegóły: t21_concurrency/Concurrency01Threads.
 *
 * PUŁAPKA: po przerwaniu Sleep.ms() po cichu WRACA (nie rzuca wyjątku). Jeśli wołasz ją w pętli,
 *   pętla musi sama sprawdzać flagę, inaczej będzie się kręcić dalej — już bez spania:
 *       {@code while (!Thread.currentThread().isInterrupted()) { ...; Sleep.ms(100); }}
 *   (isInterrupted = czy przerwany)
 *
 * PUŁAPKA: nie zamieniaj InterruptedException na przypadkowy wyjątek, np. IllegalArgumentException
 *   (illegal argument = niepoprawny argument) — argument był dobry, to wątek został przerwany.
 *   Zasada: typ wyjątku ma opisywać PRAWDZIWĄ przyczynę.
 *
 * SŁÓWKA:
 *   thread = wątek; current = bieżący; interrupt = przerwij; random = losowy; min/max = najmniejszy/największy.
 * </pre>
 */
public final class Sleep {

    private Sleep() {
    }

    /** ms = milisekundy. Usypia bieżący wątek na podaną liczbę milisekund. */
    public static void ms(long millis) {
        try {
            Thread.sleep(millis);                     // sleep = śpij; wątek zatrzymuje się na „millis” ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();       // przywróć flagę przerwania (patrz DOBRA PRAKTYKA wyżej)
        }
    }

    /**
     * randomMs = losowe milisekundy. Usypia na losowy czas z przedziału [min, max] (obie granice włącznie).
     * <p>
     * ThreadLocalRandom = generator liczb losowych osobny dla każdego wątku (szybki i bezpieczny przy wielu wątkach).
     * PUŁAPKA: {@code nextInt(a, b)} losuje z [a, b) — górna granica NIE jest włączona, dlatego dodajemy +1.
     */
    public static void randomMs(int min, int max) {
        ms(ThreadLocalRandom.current().nextInt(min, max + 1));
    }
}
