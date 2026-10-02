/**
 * TEMAT: t17_datetime — DATA I CZAS (java.time, Java 8+): LocalDate, LocalTime, LocalDateTime, Period, Duration,
 * formatowanie, strefy czasowe i Instant.
 *
 * <p>Pakiet java.time zastąpił stare klasy Date i Calendar. Jego obiekty są NIEZMIENNE (każda zmiana zwraca nowy obiekt),
 * miesiące liczone są od 1, a nazwy klas mówią, co reprezentują: sama data, sama godzina, data z godziną,
 * punkt na osi czasu (Instant), data z godziną w konkretnej strefie (ZonedDateTime).</p>
 *
 * <p>Wymagania: t01–t06, t08_enums (DayOfWeek, Month to enumy), t10_exceptions (DateTimeException).</p>
 *
 * <p>Kolejność czytania:</p>
 * <ol>
 *   <li>DateTime01LocalDateTime — LocalDate, LocalTime, LocalDateTime, plus/minus/with, porównywanie, Clock</li>
 *   <li>DateTime02PeriodDuration — Period (lata, miesiące, dni), Duration (godziny, minuty), ChronoUnit.between</li>
 *   <li>DateTime03Formatting — DateTimeFormatter: wzorce, polskie nazwy, parsowanie, błędy</li>
 *   <li>DateTime04ZonesInstant — ZoneId, ZonedDateTime, Instant, zmiana czasu letni/zimowy</li>
 *   <li>DateTime05Practical — praktyka: dni robocze, terminy, harmonogramy, raporty</li>
 *   <li>DateTime06FormatterAdvanced — dodatek: formatery ISO i lokalne, Builder, ResolverStyle, parsowanie, Duration/Period, stary kod</li>
 * </ol>
 *
 * <p>SŁÓWKA: date = data; time = czas (godzina); local = lokalny (bez strefy); period = okres; duration = czas trwania;
 * zone = strefa; instant = chwila (punkt na osi czasu); format = format; parse = odczytaj (z tekstu).</p>
 */
package t17_datetime;
