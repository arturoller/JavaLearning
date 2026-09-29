package helpers.model;

/**
 * Department = dział w firmie (enum — typ wyliczeniowy, t08_enums).
 * <p>
 * Najprostsza postać enuma: same stałe, bez pól i metod. Mimo to każdy enum ma wbudowane metody:
 * name() (nazwa stałej), ordinal() (numer kolejny od 0), values() (tablica wszystkich stałych),
 * valueOf("IT") (stała po nazwie).
 * <p>
 * UWAGA: dział LOGISTYKA celowo NIE MA żadnego pracownika w SampleData — przyda się do pokazania,
 * że groupingBy nie tworzy pustych grup (a EnumMap wypełniony wszystkimi działami — tak).
 * <p>
 * SŁÓWKA: department = dział; IT = dział informatyczny; HR (Human Resources) = kadry;
 * SPRZEDAZ = sprzedaż; KSIEGOWOSC = księgowość; LOGISTYKA = logistyka.
 */
public enum Department {
    IT,
    HR,
    SPRZEDAZ,
    KSIEGOWOSC,
    MARKETING,
    LOGISTYKA
}
