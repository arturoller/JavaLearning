package helpers.model;

/**
 * OrderStatus = status (stan) zamówienia (enum — typ wyliczeniowy, t08_enums).
 * <p>
 * Zamówienie przechodzi przez kolejne stany: NOWE → OPLACONE → WYSLANE → DOSTARCZONE.
 * Z każdego stanu NIEKOŃCOWEGO może też przejść do ANULOWANE. Jak pilnować dozwolonych przejść —
 * maszyna stanów w t08_enums/Enums04EnumMapSet.
 * <p>
 * SŁÓWKA: order = zamówienie; status = stan; final = końcowy.
 */
public enum OrderStatus {
    NOWE,
    OPLACONE,
    WYSLANE,
    DOSTARCZONE,
    ANULOWANE;

    /**
     * isFinal = czy jest końcowy. Ze stanu końcowego zamówienie już nigdzie nie przejdzie.
     * Enum może mieć własne metody — tak jak zwykła klasa.
     */
    public boolean isFinal() {
        return this == DOSTARCZONE || this == ANULOWANE;   // enumy porównujemy przez == (każda stała istnieje tylko raz)
    }
}
