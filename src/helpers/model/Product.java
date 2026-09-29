package helpers.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Product = produkt w sklepie. To jest RECORD (rekord) — zwięzła, niezmienna klasa „na dane”.
 * <p>
 * Jedna linijka „record Product(String sku, ...)” sama generuje:
 * <ul>
 *   <li>prywatne, finalne pola (sku, name, category, price, stock),</li>
 *   <li>konstruktor ze wszystkimi polami (tzw. kanoniczny),</li>
 *   <li>metody dostępowe BEZ przedrostka get: sku(), name(), price()... (akcesory),</li>
 *   <li>equals() i hashCode() porównujące wszystkie pola, oraz toString().</li>
 * </ul>
 * Pełne wyjaśnienie: t09_records (Records01Basics, Records02Constructors).
 * <p>
 * Pola:
 * <ul>
 *   <li>sku (Stock Keeping Unit) = kod magazynowy produktu, np. "ELE-001",</li>
 *   <li>name = nazwa,</li>
 *   <li>category = kategoria,</li>
 *   <li>price = cena (BigDecimal — dokładne liczby dziesiętne, idealne do pieniędzy; t15_numbers),</li>
 *   <li>stock = stan magazynowy (liczba sztuk).</li>
 * </ul>
 * DOBRA PRAKTYKA: obiekt powinien być poprawny od chwili utworzenia — dlatego walidujemy dane w konstruktorze
 * (zasada „fail fast” = zgłoś błąd od razu, zanim zły obiekt narobi szkód gdzie indziej).
 */
public record Product(String sku, String name, Category category, BigDecimal price, int stock) {

    /**
     * Compact constructor (konstruktor kompaktowy) — BEZ listy parametrów.
     * Wykonuje się PRZED przypisaniem pól, więc to idealne miejsce na walidację (sprawdzenie poprawności danych).
     * requireNonNull = wymagaj, żeby nie był null — jeśli jest null, rzuca NullPointerException z naszym komunikatem.
     */
    public Product {
        Objects.requireNonNull(sku, "sku nie może być null");
        Objects.requireNonNull(name, "name nie może być null");
        Objects.requireNonNull(category, "category nie może być null");
        Objects.requireNonNull(price, "price nie może być null");
        if (price.signum() < 0) {                       // signum = znak liczby: -1 ujemna, 0 zero, 1 dodatnia
            throw new IllegalArgumentException("Cena nie może być ujemna: " + price);
        }
        if (stock < 0) {
            throw new IllegalArgumentException("Stan magazynowy nie może być ujemny: " + stock);
        }
    }

    /** inStock = czy jest na stanie (w magazynie). Record może mieć dodatkowe metody. */
    public boolean inStock() {
        return stock > 0;
    }

    /** stockValue = wartość zapasu = cena × liczba sztuk. multiply = pomnóż; valueOf = zamień int na BigDecimal. */
    public BigDecimal stockValue() {
        return price.multiply(BigDecimal.valueOf(stock));
    }

    /**
     * toString = zamień na napis. NADPISUJEMY (override) wersję wygenerowaną przez record,
     * żeby w lekcjach listy produktów były krótkie i czytelne:
     * zamiast "Product[sku=ELE-001, name=Laptop Pro 14, category=ELEKTRONIKA, price=5499.99, stock=7]"
     * będzie po prostu "Laptop Pro 14 (5499.99 zł)".
     */
    @Override
    public String toString() {
        return name + " (" + price + " zł)";
    }
}
