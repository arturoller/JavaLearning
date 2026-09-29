package helpers.model;

/**
 * Category = kategoria produktu. To jest ENUM (typ wyliczeniowy) — zamknięta lista możliwych wartości.
 * <p>
 * Każda stała (ELEKTRONIKA, SPOZYWCZE...) ma pole displayName (nazwa do wyświetlenia, z polskimi znakami),
 * bo nazwy stałych w Javie piszemy WIELKIMI_LITERAMI i najlepiej bez polskich znaków.
 * Pełne wyjaśnienie enumów: t08_enums (Enums01Basics, Enums02FieldsMethods).
 * <p>
 * DOBRA PRAKTYKA: używaj enuma zamiast „gołych” Stringów ("ELEKTRONIKA") — kompilator wyłapie literówki,
 * a IDE podpowie wszystkie możliwe wartości.
 * <p>
 * SŁÓWKA: display name = nazwa wyświetlana; get = pobierz.
 */
public enum Category {
    ELEKTRONIKA("Elektronika"),
    SPOZYWCZE("Spożywcze"),
    KSIAZKI("Książki"),
    ODZIEZ("Odzież"),
    DOM("Dom i ogród");

    private final String displayName;

    // Konstruktor enuma jest zawsze prywatny — obiekty (stałe) tworzy sama Java, tylko te wypisane wyżej.
    Category(String displayName) {
        this.displayName = displayName;
    }

    /** getDisplayName = pobierz nazwę wyświetlaną, np. "Książki". */
    public String getDisplayName() {
        return displayName;
    }
}
