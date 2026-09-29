package helpers.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Order = zamówienie (record — t09_records).
 * <p>
 * Pola: id = numer zamówienia, np. "ZAM-001"; customer = klient; date = data złożenia;
 * status = stan (enum OrderStatus); lines = pozycje zamówienia (lista OrderLine).
 * <p>
 * Zamówienie zawiera LISTĘ pozycji — to klasyczny przykład do flatMap
 * („spłaszcz listę zamówień w jedną listę wszystkich pozycji”, t16_streams/Streams04FlatMap).
 */
public record Order(String id, Customer customer, LocalDate date, OrderStatus status, List<OrderLine> lines) {

    public Order {
        Objects.requireNonNull(id, "id nie może być null");
        Objects.requireNonNull(customer, "customer nie może być null");
        Objects.requireNonNull(date, "date nie może być null");
        Objects.requireNonNull(status, "status nie może być null");
        lines = List.copyOf(lines);   // kopia obronna — patrz komentarz w Employee
    }

    /**
     * total = suma zamówienia = suma wartości wszystkich pozycji.
     * <p>
     * To jest mały stream (strumień) — nie przejmuj się, jeśli jeszcze go nie rozumiesz, wszystko w t16_streams:
     * <ol>
     *   <li>lines.stream() — zrób strumień z listy pozycji,</li>
     *   <li>map(OrderLine::total) — zamień każdą pozycję na jej wartość (BigDecimal),</li>
     *   <li>reduce(BigDecimal.ZERO, BigDecimal::add) — zacznij od zera i dodawaj kolejne wartości.</li>
     * </ol>
     */
    public BigDecimal total() {
        return lines.stream()
                .map(OrderLine::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public String toString() {
        return id + " [" + customer.name() + ", " + date + ", " + status + ", " + total() + " zł]";
    }
}
