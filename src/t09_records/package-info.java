/**
 * TEMAT: t09_records — REKORDY (record, Java 16+): zwięzłe, niezmienne nośniki danych.
 *
 * <p>Record to klasa, której jedynym zadaniem jest przechowywanie danych. Jedna linijka
 * {@code record Point(int x, int y) {}} generuje pola final, konstruktor, metody dostępu, equals, hashCode i toString.
 * To zastępuje dziesiątki linii „ręcznej” klasy z t06_oop_basics.</p>
 *
 * <p>Wymagania: t06_oop_basics (konstruktory, equals/hashCode, niezmienność) i t07 (interfejsy).</p>
 *
 * <p>Kolejność czytania:</p>
 * <ol>
 *   <li>Records01Basics — co generuje record, PRZED/PO, equals, płytka niezmienność, ograniczenia</li>
 *   <li>Records02Constructors — konstruktor kompaktowy (walidacja, normalizacja), kopie obronne, fabryki, „withery”</li>
 *   <li>Records03Advanced — rekordy generyczne, lokalne, z interfejsem, w mapach i streamach, instanceof ze wzorcem</li>
 * </ol>
 *
 * <p>SŁÓWKA: record = rekord (zapis danych); component = składnik; accessor = metoda dostępu; compact constructor =
 * konstruktor kompaktowy; canonical constructor = konstruktor kanoniczny (pełny); defensive copy = kopia obronna.</p>
 */
package t09_records;
