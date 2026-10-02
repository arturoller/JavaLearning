/**
 * TEMAT: t31_jdk_toolbox — mała skrzynka narzędziowa JDK, która przydaje się w każdym projekcie
 * <p>
 * Cztery krótkie dziedziny, o których nie mówi żaden większy dział, a które wracają w każdej
 * prawdziwej aplikacji: identyfikatory i kodowanie bajtów jako tekst (UUID, Base64, hex),
 * skróty i podstawy bezpieczeństwa (SHA-256, hasła, tokeny, HMAC), internacjonalizacja
 * (Locale, liczby, daty, komunikaty, polska liczba mnoga, sortowanie) oraz logowanie
 * (System.Logger i java.util.logging). Zakres bezpieczeństwa jest wyłącznie OBRONNY:
 * uczymy się robić dobrze, a nie łamać cudze zabezpieczenia.
 * <p>
 * Wymagane: t10_exceptions (wyjątki), t12_collections, t15_numbers (BigDecimal), t17_datetime,
 * t18_io_files (pliki, kodowanie znaków, prosty logger), t21_concurrency (podstawy wątków).
 * <ol>
 *   <li>Toolbox01UuidBase64 — UUID, Base64, HexFormat, CRC32</li>
 *   <li>Toolbox02HashingSecurity — MessageDigest, hasła (PBKDF2), SecureRandom, HMAC</li>
 *   <li>Toolbox03I18n — Locale, NumberFormat, ResourceBundle, MessageFormat, Collator</li>
 *   <li>Toolbox04Logging — System.Logger, JUL, formatery, MDC, logi strukturalne, Logback</li>
 * </ol>
 * <p>SŁÓWKA: toolbox = skrzynka narzędziowa; hash = skrót; salt = sól; token = żeton;
 * locale = ustawienia regionalne; bundle = pakiet zasobów; logger = dziennik zdarzeń.</p>
 */
package t31_jdk_toolbox;
