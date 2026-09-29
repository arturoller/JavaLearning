package helpers.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * OrderLine = pozycja (linia) zamówienia: jaki produkt i ile sztuk (record — t09_records).
 * <p>
 * Pola: product = produkt; quantity = ilość (liczba sztuk).
 * <p>
 * DOBRA PRAKTYKA: wartości wyliczane (jak total) liczymy w metodzie, a NIE trzymamy w osobnym polu —
 * wtedy nigdy nie „rozjadą się” z danymi, z których powstają.
 */
public record OrderLine(Product product, int quantity) {

    public OrderLine {
        Objects.requireNonNull(product, "product nie może być null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("Ilość musi być dodatnia: " + quantity);
        }
    }

    /** total = suma (wartość pozycji) = cena × ilość. */
    public BigDecimal total() {
        return product.price().multiply(BigDecimal.valueOf(quantity));
    }

    @Override
    public String toString() {
        return product.name() + " x" + quantity;
    }
}
