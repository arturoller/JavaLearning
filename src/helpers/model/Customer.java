package helpers.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Customer = klient (record — t09_records).
 * <p>
 * Pola: id = identyfikator; name = imię i nazwisko; city = miasto; email = adres e-mail (MOŻE BYĆ null!);
 * vip = czy klient VIP (Very Important Person = bardzo ważna osoba, np. lepsze rabaty).
 * <p>
 * email celowo bywa null — dzięki temu w lekcjach o Optional (t14_optional) mamy realny przypadek
 * „wartość może nie istnieć”. Metoda findEmail() zwraca Optional zamiast null.
 * <p>
 * DOBRA PRAKTYKA: gdy wartość może nie istnieć, udostępniaj ją przez metodę zwracającą Optional
 * (find... = znajdź — nazwa sugeruje, że wyniku może nie być). Kto używa findEmail(), musi obsłużyć brak
 * wartości i nie dostanie niespodziewanego NullPointerException.
 * <p>
 * PUŁAPKA: record i tak generuje publiczny akcesor email(), który zwraca „gołe” null. Optional chroni tylko
 * tych, którzy używają findEmail(). W prawdziwym kodzie rozważ zwykłą klasę bez takiego akcesora
 * albo walidację, która w ogóle nie dopuszcza null.
 */
public record Customer(long id, String name, String city, String email, boolean vip) {

    public Customer {
        Objects.requireNonNull(name, "name nie może być null");
        Objects.requireNonNull(city, "city nie może być null");
    }

    /**
     * findEmail = znajdź e-mail. Zwraca Optional („pudełko”, które może być puste).
     * ofNullable = „z wartości, która może być null”: null → pusty Optional, cokolwiek innego → Optional z wartością.
     */
    public Optional<String> findEmail() {
        return Optional.ofNullable(email);
    }

    @Override
    public String toString() {
        return name + " (" + city + (vip ? ", VIP" : "") + ")";   // operator trójargumentowy: warunek ? gdy_tak : gdy_nie
    }
}
