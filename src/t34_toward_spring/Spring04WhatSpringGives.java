package t34_toward_spring;

import com.sun.net.httpserver.HttpServer;
import helpers.Check;
import helpers.SampleData;
import helpers.model.Category;
import helpers.model.Product;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Co daje Spring Boot — mapa i mechanizmy zbudowane w czystej Javie
 *        (auto-configuration = automatyczna konfiguracja; starter = zestaw startowy zależności;
 *         profile = profil; actuator = „siłownik”, moduł diagnostyczny; repository = repozytorium)
 *
 * W SKRÓCIE:
 *   Spring Boot to Spring „z gotowymi ustawieniami”: dobiera zależności (startery), sam tworzy typowe beany,
 *   gdy spełnione są warunki (auto-konfiguracja), czyta konfigurację z wielu źródeł (pliki, zmienne
 *   środowiskowe, argumenty, profile), uruchamia wbudowany serwer, wystawia /actuator/health, a Spring Data
 *   pisze zapytania za nas na podstawie NAZW metod. Każdy z tych mechanizmów zbudujemy tu w miniaturze.
 *
 * ANALOGIA:
 *   Mieszkanie „pod klucz”. Spring to materiały budowlane, Spring Boot — ekipa, która urządza mieszkanie
 *   według rozsądnych domysłów: wstawi lodówkę, jeśli jest gniazdko (warunek), ale nie wstawi drugiej,
 *   jeśli przywieziesz własną (@ConditionalOnMissingBean). Kartka z życzeniami (konfiguracja) wygrywa
 *   z domysłami, a życzenia wypowiedziane na głos w ostatniej chwili (argumenty) — z kartką.
 *
 * JAK TO DZIAŁA: start aplikacji Spring Boot, w uproszczeniu
 *   1. SpringApplication.run(App.class, args)
 *   2. Environment: domyślne → application.properties → application-{profil}.properties → zmienne środ. → argumenty
 *   3. Skanowanie Twoich @Component/@Service/... (pakiet klasy App i niżej)
 *   4. Auto-konfiguracje: setki klas z warunkami (@ConditionalOnClass, @ConditionalOnProperty,
 *      @ConditionalOnMissingBean) — tworzą beany tylko tam, gdzie Ty nie dałeś własnych
 *   5. Wbudowany serwer (Tomcat) startuje na server.port; aplikacja gotowa
 *
 * SŁÓWKA:
 *   conditional on property = pod warunkiem właściwości; having value = mająca wartość; match if missing =
 *   pasuje, gdy brak; missing bean = brakujący bean; embedded = wbudowany; health = zdrowie; up/down =
 *   działa/nie działa; slice = wycinek; binding = wiązanie (konfiguracji z obiektem)
 *
 * ZOBACZ TEŻ: t18_io_files/Io06Properties (Properties), t34_toward_spring/Spring01IocContainer (kontener),
 *   t34_toward_spring/Spring03RestConcepts (mini-framework HTTP), t19_annotations_reflection/Annotations06DynamicProxy (Proxy),
 *   t32_junit_mockito/JUnit04Mockito (atrapy w testach)
 * </pre>
 */
public class Spring04WhatSpringGives {

    public static void main(String[] args) {
        title("Spring04 — co daje Spring Boot (zbudowane w czystej Javie)");

        bootMap();                 // boot map = mapa Spring Boot
        starters();                // starters = startery
        externalConfig();          // external config = konfiguracja zewnętrzna
        typedConfig();             // typed config = typowana konfiguracja
        profiles();                // profiles = profile
        autoConfiguration();       // auto configuration = auto-konfiguracja
        embeddedServerAndHealth(); // embedded server and health = wbudowany serwer i zdrowie
        springDataIdea();          // Spring Data idea = idea Spring Data
        testingSlices();           // testing slices = testowe wycinki
        whatNext();                // what next = co dalej
        exercises();               // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. MAPA SPRING BOOT
    // =================================================================================================

    /**
     * 1. Wszystko, co tu zobaczysz, stoi na kontenerze IoC ze Spring01. Boot nie jest „innym Springiem” —
     * to warstwa wygody: zależności, domyślne beany, konfiguracja, uruchamianie.
     */
    static void bootMap() {
        section("1. Mapa Spring Boot: z czego się składa");

        Map<String, String> map = new LinkedHashMap<>();
        map.put("startery", "jedna zależność = spójny zestaw bibliotek w zgodnych wersjach");
        map.put("auto-konfiguracja", "beany tworzone warunkowo (klasa na classpath, właściwość, brak Twojego beana)");
        map.put("konfiguracja zewnętrzna", "application.properties/yml, zmienne środowiskowe, argumenty");
        map.put("profile", "dev / test / prod — inne ustawienia bez zmiany kodu");
        map.put("wbudowany serwer", "Tomcat w aplikacji: java -jar app.jar i działa");
        map.put("actuator", "/actuator/health, metryki, informacje o aplikacji");
        map.put("Spring Data", "repozytoria z interfejsów: findByCategory(...) bez SQL");
        map.put("testy", "@SpringBootTest i wycinki: @WebMvcTest, @DataJpaTest");
        showEach("Spring Boot", map);
        // WYNIK: Spring Boot (liczba kluczy: 8):
        // WYNIK: • startery → jedna zależność = spójny zestaw bibliotek w zgodnych wersjach
        // WYNIK: • auto-konfiguracja → beany tworzone warunkowo (klasa na classpath, właściwość, brak Twojego beana)
        // WYNIK: • konfiguracja zewnętrzna → application.properties/yml, zmienne środowiskowe, argumenty
        // WYNIK: • profile → dev / test / prod — inne ustawienia bez zmiany kodu
        // WYNIK: • wbudowany serwer → Tomcat w aplikacji: java -jar app.jar i działa
        // WYNIK: • actuator → /actuator/health, metryki, informacje o aplikacji
        // WYNIK: • Spring Data → repozytoria z interfejsów: findByCategory(...) bez SQL
        // WYNIK: • testy → @SpringBootTest i wycinki: @WebMvcTest, @DataJpaTest

        // W Springu: cała aplikacja startuje tak:
        //   @SpringBootApplication                  // = @Configuration + @EnableAutoConfiguration + @ComponentScan
        //   public class ShopApplication {
        //       public static void main(String[] args) { SpringApplication.run(ShopApplication.class, args); }
        //   }
    }

    // =================================================================================================
    // 2. STARTERY
    // =================================================================================================

    /**
     * 2. Starter to „pusta” zależność Maven, która ciągnie za sobą zestaw bibliotek w wersjach sprawdzonych
     * razem. Wersji nie piszesz — zarządza nimi rodzic spring-boot-starter-parent (BOM = lista materiałów).
     */
    static void starters() {
        section("2. Startery: jedna zależność zamiast dziesięciu");

        Map<String, String> starters = new LinkedHashMap<>();
        starters.put("spring-boot-starter-web", "Spring MVC + Jackson (JSON) + wbudowany Tomcat");
        starters.put("spring-boot-starter-validation", "Bean Validation (Hibernate Validator)");
        starters.put("spring-boot-starter-data-jpa", "Spring Data JPA + Hibernate + pula połączeń HikariCP");
        starters.put("spring-boot-starter-actuator", "/actuator/health, metryki");
        starters.put("spring-boot-starter-test", "JUnit 5 + AssertJ + Mockito + Spring Test");
        showEach("starter → co przynosi", starters);
        // WYNIK: starter → co przynosi (liczba kluczy: 5):
        // WYNIK: • spring-boot-starter-web → Spring MVC + Jackson (JSON) + wbudowany Tomcat
        // WYNIK: • spring-boot-starter-validation → Bean Validation (Hibernate Validator)
        // WYNIK: • spring-boot-starter-data-jpa → Spring Data JPA + Hibernate + pula połączeń HikariCP
        // WYNIK: • spring-boot-starter-actuator → /actuator/health, metryki
        // WYNIK: • spring-boot-starter-test → JUnit 5 + AssertJ + Mockito + Spring Test

        // PUŁAPKA: dopisywanie wersji do bibliotek zarządzanych przez Boot (np. <version> przy Jacksonie)
        // rozjeżdża zestaw sprawdzonych wersji — błędy typu NoSuchMethodError w trakcie działania.
        // DOBRA PRAKTYKA: wersję podajesz raz — przy spring-boot-starter-parent; reszta bez <version>.
        // W Springu: fragment pom.xml —
        //   <parent> org.springframework.boot : spring-boot-starter-parent : 3.x.y </parent>
        //   <dependency> org.springframework.boot : spring-boot-starter-web </dependency>   (bez wersji!)
    }

    // =================================================================================================
    // 3. KONFIGURACJA ZEWNĘTRZNA: WARSTWY
    // =================================================================================================

    /** Jedna warstwa konfiguracji: nazwa źródła + pary klucz=wartość. */
    record Layer(String source, Map<String, String> values) { }

    /** Scala warstwy od najsłabszej do najsilniejszej; pamięta, skąd pochodzi każda wartość. */
    static Map<String, String> mergeWithSources(List<Layer> layers) {
        Map<String, String> result = new TreeMap<>();
        for (Layer layer : layers) {
            layer.values().forEach((k, v) -> result.put(k, v + " [" + layer.source() + "]"));
        }
        return result;
    }

    /** Zmienna środowiskowa → klucz: SERVER_PORT → server.port (uproszczone „relaxed binding”). */
    static String envToKey(String env) {
        return env.toLowerCase(java.util.Locale.ROOT).replace('_', '.');
    }

    static Map<String, String> fromPropertiesText(String text) {
        Properties p = new Properties();
        try {
            p.load(new StringReader(text));                                 // load = wczytaj
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Map<String, String> map = new TreeMap<>();
        p.stringPropertyNames().forEach(k -> map.put(k, p.getProperty(k)));
        return map;
    }

    static Map<String, String> fromArgs(String[] args) {
        Map<String, String> map = new TreeMap<>();
        for (String a : args) {
            if (a.startsWith("--") && a.contains("=")) {
                map.put(a.substring(2, a.indexOf('=')), a.substring(a.indexOf('=') + 1));
            }
        }
        return map;
    }

    static final String APPLICATION_PROPERTIES = """
            sklep.name=Sklep u Ani
            server.port=8081
            sklep.timeout=45s
            """;                                                            // text block (Java 15+)

    /**
     * 3. Ta sama aplikacja działa na laptopie, w teście i na serwerze — różni się tylko konfiguracją.
     * Źródła tworzą warstwy; silniejsza warstwa nadpisuje słabszą. Kod czyta jeden scalony widok.
     */
    static void externalConfig() {
        section("3. Konfiguracja zewnętrzna: domyślne → plik → zmienne środowiskowe → argumenty");

        Map<String, String> defaults = new TreeMap<>(Map.of(
                "sklep.name", "Sklep", "server.port", "8080", "sklep.timeout", "30s", "sklep.mail-enabled", "false"));
        Map<String, String> env = new TreeMap<>();
        Map<String, String> fakeEnv = new LinkedHashMap<>();                // udajemy System.getenv()
        fakeEnv.put("SERVER_PORT", "9000");
        fakeEnv.forEach((k, v) -> env.put(envToKey(k), v));
        String[] args = {"--server.port=9090", "--sklep.mail-enabled=true", "raport.txt"};

        Map<String, String> merged = mergeWithSources(List.of(
                new Layer("domyślne", defaults),
                new Layer("application.properties", fromPropertiesText(APPLICATION_PROPERTIES)),
                new Layer("zmienna środowiskowa", env),
                new Layer("argument", fromArgs(args))));
        showEach("scalona konfiguracja", merged);
        // WYNIK: scalona konfiguracja (liczba kluczy: 4):
        // WYNIK: • server.port → 9090 [argument]
        // WYNIK: • sklep.mail-enabled → true [argument]
        // WYNIK: • sklep.name → Sklep u Ani [application.properties]
        // WYNIK: • sklep.timeout → 45s [application.properties]
        // server.port: 8080 → 8081 (plik) → 9000 (zmienna) → 9090 (argument) — wygrywa najsilniejsza warstwa.
        // "raport.txt" bez "--" to zwykły argument programu, nie właściwość.

        // PUŁAPKA: hasła i klucze w application.properties w repozytorium gita = wyciek. Sekrety podaje się
        // zmiennymi środowiskowymi albo z menedżera sekretów (np. DB_PASSWORD → db.password).
        // W Springu: kolejność (od najsilniejszego) m.in.: argumenty wiersza poleceń → właściwości systemowe Javy
        //            (-D) → zmienne środowiskowe → application-{profil}.properties → application.properties →
        //            domyślne. Zmienna SKLEP_MAIL_ENABLED pasuje do sklep.mail-enabled („relaxed binding” —
        //            Boot usuwa też myślniki przy porównaniu, czego nasz uproszczony envToKey nie robi).
    }

    // =================================================================================================
    // 4. TYPOWANA KONFIGURACJA (rekord zamiast rozsianych @Value)
    // =================================================================================================

    /** Ustawienia sklepu — jeden rekord, typy zamiast tekstów. */
    record ShopProperties(String name, int port, Duration timeout, boolean mailEnabled) {
        ShopProperties {
            if (port < 1 || port > 65535) throw new IllegalArgumentException("port poza zakresem: " + port);
        }
    }

    /** camelCase → kebab-case: mailEnabled → mail-enabled. */
    static String kebab(String camel) {
        return camel.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase(java.util.Locale.ROOT);
    }

    /** "30s", "500ms", "2m" albo ISO "PT30S" → Duration. */
    static Duration parseDuration(String text) {
        if (text.startsWith("PT")) return Duration.parse(text);
        if (text.endsWith("ms")) return Duration.ofMillis(Long.parseLong(text.substring(0, text.length() - 2)));
        if (text.endsWith("s")) return Duration.ofSeconds(Long.parseLong(text.substring(0, text.length() - 1)));
        if (text.endsWith("m")) return Duration.ofMinutes(Long.parseLong(text.substring(0, text.length() - 1)));
        throw new IllegalArgumentException("nieznany format czasu: " + text);
    }

    /** Wiązanie (binding): każdy składnik rekordu szuka klucza prefix + kebab(nazwa); port → server.port. */
    static ShopProperties bind(Map<String, String> config) {
        RecordComponent[] rcs = ShopProperties.class.getRecordComponents();
        Object[] args = new Object[rcs.length];
        Class<?>[] types = new Class<?>[rcs.length];
        for (int i = 0; i < rcs.length; i++) {
            String key = rcs[i].getName().equals("port") ? "server.port" : "sklep." + kebab(rcs[i].getName());
            String raw = config.get(key);
            if (raw == null) throw new IllegalArgumentException("brak klucza " + key);
            types[i] = rcs[i].getType();
            if (types[i] == int.class) args[i] = Integer.parseInt(raw);
            else if (types[i] == boolean.class) args[i] = Boolean.parseBoolean(raw);
            else if (types[i] == Duration.class) args[i] = parseDuration(raw);
            else args[i] = raw;
        }
        try {
            return ShopProperties.class.getDeclaredConstructor(types).newInstance(args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw (RuntimeException) e.getCause();                          // np. walidacja portu z konstruktora
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 4. Zamiast dziesięciu @Value("${...}") w różnych klasach — jeden rekord z typami (int, Duration, boolean),
     * wypełniany automatycznie i SPRAWDZANY przy starcie. Literówka czy zły port kończą start z jasnym błędem.
     */
    static void typedConfig() {
        section("4. Typowana konfiguracja: rekord ShopProperties");

        Map<String, String> config = new TreeMap<>(Map.of(
                "sklep.name", "Sklep u Ani", "server.port", "9090", "sklep.timeout", "45s", "sklep.mail-enabled", "true"));
        ShopProperties props = bind(config);
        show("rekord", props);
        // WYNIK: rekord → ShopProperties[name=Sklep u Ani, port=9090, timeout=PT45S, mailEnabled=true]
        show("timeout w ms", props.timeout().toMillis());
        // WYNIK: timeout w ms → 45000

        config.put("server.port", "70000");
        expectThrows("port 70000", () -> bind(config));
        // WYNIK: ✔ port 70000 → rzucono IllegalArgumentException: port poza zakresem: 70000
        config.put("server.port", "8080");
        config.remove("sklep.timeout");
        expectThrows("brak timeout", () -> bind(config));
        // WYNIK: ✔ brak timeout → rzucono IllegalArgumentException: brak klucza sklep.timeout

        // DOBRA PRAKTYKA: kwoty, czasy i rozmiary jako typy (Duration, DataSize), nie „gołe” liczby — "30" to
        // sekundy czy milisekundy? "30s" nie budzi wątpliwości.
        // W Springu: @ConfigurationProperties(prefix = "sklep") record ShopProperties(String name, Duration timeout,
        //            boolean mailEnabled) { } + @EnableConfigurationProperties(ShopProperties.class) albo
        //            @ConfigurationPropertiesScan; walidacja: @Validated i adnotacje Bean Validation na składnikach.
    }

    // =================================================================================================
    // 5. PROFILE
    // =================================================================================================

    static final Map<String, String> PROFILE_FILES = Map.of(
            "dev", "sklep.mail-enabled=false\nlogging.level=DEBUG\n",
            "prod", "server.port=80\nsklep.mail-enabled=true\nlogging.level=WARN\n");

    /** Plik główny + plik profilu (profil wygrywa). */
    static Map<String, String> configForProfile(String profile) {
        Map<String, String> config = new TreeMap<>(fromPropertiesText(APPLICATION_PROPERTIES + "logging.level=INFO\n"));
        config.putAll(fromPropertiesText(PROFILE_FILES.getOrDefault(profile, "")));
        return config;
    }

    /**
     * 5. Profil = nazwany zestaw ustawień (dev, test, prod). Plik application-{profil}.properties nadpisuje
     * application.properties. Aktywny profil wybiera się konfiguracją, a nie zmianą kodu.
     */
    static void profiles() {
        section("5. Profile: dev, prod — te same klasy, inne ustawienia");

        for (String profile : List.of("dev", "prod", "brak")) {
            Map<String, String> c = configForProfile(profile);
            show(profile, "port=" + c.get("server.port") + ", mail=" + c.getOrDefault("sklep.mail-enabled", "(brak)")
                    + ", logi=" + c.get("logging.level"));
        }
        // WYNIK: dev → port=8081, mail=false, logi=DEBUG
        // WYNIK: prod → port=80, mail=true, logi=WARN
        // WYNIK: brak → port=8081, mail=(brak), logi=INFO

        // PUŁAPKA: logika „if (profil.equals("prod"))” rozsiana po kodzie. Profil powinien zmieniać KONFIGURACJĘ
        // (albo wybierać beany), a nie wplatać się w reguły biznesowe.
        // W Springu: --spring.profiles.active=prod (albo SPRING_PROFILES_ACTIVE=prod); pliki application-prod.properties;
        //            beany tylko dla profilu: @Profile("dev") na klasie; w testach @ActiveProfiles("test").
    }

    // =================================================================================================
    // 6. AUTO-KONFIGURACJA: BEANY Z WARUNKAMI
    // =================================================================================================

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
    @interface ConditionalOnProperty {
        String name();
        String havingValue() default "";
        boolean matchIfMissing() default false;
    }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
    @interface ConditionalOnClass { String value(); }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
    @interface ConditionalOnMissingBean { }

    interface MailSender { String send(String to); }
    interface PaymentGateway { String pay(int amount); }                   // payment gateway = bramka płatności
    interface Cache { String name(); }

    @ConditionalOnProperty(name = "sklep.mail-enabled", havingValue = "true")
    static class SmtpMailSender implements MailSender {
        @Override public String send(String to) { return "SMTP → " + to; }
    }

    @ConditionalOnProperty(name = "sklep.mail-enabled", havingValue = "false", matchIfMissing = true)
    static class NoOpMailSender implements MailSender {                     // no-op = nic nie robi
        @Override public String send(String to) { return "(wysyłka wyłączona) " + to; }
    }

    @ConditionalOnMissingBean
    static class FakePaymentGateway implements PaymentGateway {
        @Override public String pay(int amount) { return "atrapa: zapłacono " + amount; }
    }

    @ConditionalOnClass("io.lettuce.core.RedisClient")
    static class RedisCache implements Cache { @Override public String name() { return "redis"; } }

    @ConditionalOnClass("java.util.concurrent.ConcurrentHashMap")
    static class InMemoryCache implements Cache { @Override public String name() { return "pamięć"; } }

    /** „Twój” bean — zdefiniowany przez programistę aplikacji. */
    static class BankTransferGateway implements PaymentGateway {
        @Override public String pay(int amount) { return "przelew: " + amount; }
    }

    /** Mini Boot: najpierw beany użytkownika, potem auto-konfiguracje (posortowane), z raportem warunków. */
    static final class MiniBoot {
        final Map<String, Object> beans = new TreeMap<>();
        final List<String> report = new ArrayList<>();

        MiniBoot(Map<String, String> config, List<Class<?>> userBeans, List<Class<?>> autoConfigs) {
            userBeans.forEach(c -> beans.put(c.getSimpleName(), newInstance(c)));
            autoConfigs.stream().sorted(Comparator.comparing(Class::getSimpleName)).forEach(c -> {
                String reason = whyNot(c, config);                          // null = warunki spełnione
                if (reason == null) beans.put(c.getSimpleName(), newInstance(c));
                report.add((reason == null ? "✔ " : "✘ ") + c.getSimpleName() + (reason == null ? "" : " — " + reason));
            });
        }

        private String whyNot(Class<?> c, Map<String, String> config) {
            ConditionalOnClass onClass = c.getAnnotation(ConditionalOnClass.class);
            if (onClass != null && !classPresent(onClass.value())) return "brak klasy " + onClass.value();
            ConditionalOnProperty onProp = c.getAnnotation(ConditionalOnProperty.class);
            if (onProp != null && !propertyMatches(config, onProp)) {
                return "właściwość " + onProp.name() + "=" + config.getOrDefault(onProp.name(), "(brak)");
            }
            if (c.isAnnotationPresent(ConditionalOnMissingBean.class)) {
                for (Object existing : beans.values()) {
                    for (Class<?> iface : c.getInterfaces()) {
                        if (iface.isInstance(existing)) return "jest już bean " + existing.getClass().getSimpleName();
                    }
                }
            }
            return null;
        }

        private static boolean propertyMatches(Map<String, String> config, ConditionalOnProperty p) {
            String value = config.get(p.name());
            if (value == null) return p.matchIfMissing();
            if (p.havingValue().isEmpty()) return !value.equalsIgnoreCase("false");
            return value.equalsIgnoreCase(p.havingValue());
        }

        private static boolean classPresent(String name) {
            try {
                Class.forName(name, false, MiniBoot.class.getClassLoader());   // false = nie inicjalizuj klasy
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }

        private static Object newInstance(Class<?> c) {
            try {
                return c.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }

        <T> T get(Class<T> type) {
            return beans.values().stream().filter(type::isInstance).map(type::cast).findFirst()
                    .orElseThrow(() -> new IllegalStateException("brak beana " + type.getSimpleName()));
        }
    }

    static final List<Class<?>> AUTO_CONFIGS = List.of(SmtpMailSender.class, NoOpMailSender.class,
            FakePaymentGateway.class, RedisCache.class, InMemoryCache.class);

    /**
     * 6. Auto-konfiguracja to zwykłe klasy konfiguracyjne z WARUNKAMI. Boot sprawdza: czy biblioteka jest na
     * classpath (@ConditionalOnClass), czy właściwość ma wartość (@ConditionalOnProperty), czy nie zdefiniowałeś
     * własnego beana (@ConditionalOnMissingBean). Twoje beany są przetwarzane PIERWSZE — dlatego wygrywają.
     */
    static void autoConfiguration() {
        section("6. Auto-konfiguracja: @ConditionalOnProperty / OnClass / OnMissingBean");

        MiniBoot plain = new MiniBoot(Map.of(), List.of(), AUTO_CONFIGS);
        showEach("raport (bez konfiguracji)", plain.report);
        // WYNIK: raport (bez konfiguracji) (liczba elementów: 5):
        // WYNIK: • ✔ FakePaymentGateway
        // WYNIK: • ✔ InMemoryCache
        // WYNIK: • ✔ NoOpMailSender
        // WYNIK: • ✘ RedisCache — brak klasy io.lettuce.core.RedisClient
        // WYNIK: • ✘ SmtpMailSender — właściwość sklep.mail-enabled=(brak)
        show("mail", plain.get(MailSender.class).send("ola@example.com"));
        // WYNIK: mail → (wysyłka wyłączona) ola@example.com

        MiniBoot custom = new MiniBoot(Map.of("sklep.mail-enabled", "TRUE"), List.of(BankTransferGateway.class), AUTO_CONFIGS);
        showEach("raport (mail włączony + własna bramka)", custom.report);
        // WYNIK: raport (mail włączony + własna bramka) (liczba elementów: 5):
        // WYNIK: • ✘ FakePaymentGateway — jest już bean BankTransferGateway
        // WYNIK: • ✔ InMemoryCache
        // WYNIK: • ✘ NoOpMailSender — właściwość sklep.mail-enabled=TRUE
        // WYNIK: • ✘ RedisCache — brak klasy io.lettuce.core.RedisClient
        // WYNIK: • ✔ SmtpMailSender
        show("mail", custom.get(MailSender.class).send("ola@example.com"));
        // WYNIK: mail → SMTP → ola@example.com
        show("płatność", custom.get(PaymentGateway.class).pay(100));
        // WYNIK: płatność → przelew: 100

        // PUŁAPKA: „dlaczego Boot nie utworzył mojego DataSource / utworzył nie ten?” — odpowiedź jest w raporcie
        // warunków; bez niego auto-konfiguracja wygląda jak magia.
        // DOBRA PRAKTYKA: chcesz inne zachowanie → zdefiniuj własny bean albo ustaw właściwość; nie walcz z Bootem.
        // W Springu: @AutoConfiguration class MailAutoConfiguration { @Bean @ConditionalOnMissingBean
        //            @ConditionalOnProperty(name = "sklep.mail-enabled", havingValue = "true") MailSender smtp() { ... } };
        //            lista auto-konfiguracji: META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports;
        //            raport warunków: uruchom z --debug (CONDITIONS EVALUATION REPORT) albo /actuator/conditions.
    }

    // =================================================================================================
    // 7. WBUDOWANY SERWER I /actuator/health
    // =================================================================================================

    /** Jeden wskaźnik zdrowia: nazwa + sprawdzenie (BooleanSupplier = dostawca wartości logicznej). */
    record HealthIndicator(String name, BooleanSupplier check) { }

    /** Zbiorczy stan: UP tylko wtedy, gdy wszystkie składniki są UP. */
    static String healthJson(List<HealthIndicator> indicators) {
        Map<String, String> parts = new TreeMap<>();
        indicators.forEach(h -> parts.put(h.name(), h.check().getAsBoolean() ? "UP" : "DOWN"));
        String overall = parts.containsValue("DOWN") ? "DOWN" : "UP";
        List<String> components = new ArrayList<>();
        parts.forEach((k, v) -> components.add("\"" + k + "\":{\"status\":\"" + v + "\"}"));
        return "{\"status\":\"" + overall + "\",\"components\":{" + String.join(",", components) + "}}";
    }

    /**
     * 7. W Boot serwer jest częścią aplikacji (java -jar app.jar), a nie osobnym programem, do którego
     * „wrzuca się” aplikację. Uruchamiamy więc serwer w kodzie (loopback, port 0) i wystawiamy /actuator/health:
     * 200 + UP, gdy wszystko działa; 503 + DOWN, gdy któryś składnik leży (tak robi Boot).
     */
    static void embeddedServerAndHealth() {
        section("7. Wbudowany serwer i /actuator/health");

        AtomicBoolean mailServerUp = new AtomicBoolean(true);               // AtomicBoolean = bezpieczna flaga dla wątków
        List<HealthIndicator> indicators = List.of(
                new HealthIndicator("db", () -> true),
                new HealthIndicator("mail", mailServerUp::get),
                new HealthIndicator("diskSpace", () -> true));

        ExecutorService serverPool = Executors.newSingleThreadExecutor();
        ExecutorService clientPool = Executors.newSingleThreadExecutor();
        HttpServer server = null;
        try {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            server.createContext("/actuator/health", exchange -> {
                try {
                    String json = healthJson(indicators);
                    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                    exchange.sendResponseHeaders(json.contains("\"status\":\"DOWN\"") ? 503 : 200, bytes.length);
                    try (OutputStream out = exchange.getResponseBody()) {
                        out.write(bytes);
                    }
                } finally {
                    exchange.close();
                }
            });
            server.setExecutor(serverPool);
            server.start();

            HttpClient client = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).executor(clientPool).build();
            URI uri = URI.create("http://localhost:" + server.getAddress().getPort() + "/actuator/health");

            HttpResponse<String> ok = client.send(HttpRequest.newBuilder(uri).build(), HttpResponse.BodyHandlers.ofString());
            show("zdrowa", ok.statusCode() + " " + ok.body());
            // WYNIK: zdrowa → 200 {"status":"UP","components":{"db":{"status":"UP"},"diskSpace":{"status":"UP"},"mail":{"status":"UP"}}}

            mailServerUp.set(false);                                         // „serwer pocztowy padł”
            HttpResponse<String> down = client.send(HttpRequest.newBuilder(uri).build(), HttpResponse.BodyHandlers.ofString());
            show("chora", down.statusCode() + " " + down.body());
            // WYNIK: chora → 503 {"status":"DOWN","components":{"db":{"status":"UP"},"diskSpace":{"status":"UP"},"mail":{"status":"DOWN"}}}
        } catch (IOException e) {
            show("błąd IO", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (server != null) server.stop(0);
            serverPool.shutdown();
            clientPool.shutdown();
        }

        // PUŁAPKA: health sprawdzający zbyt wiele (np. zewnętrzne API partnera) — chwilowa awaria partnera
        // oznacza „DOWN”, a orkiestrator (np. Kubernetes) restartuje zdrową aplikację. Rozróżnia się sondy
        // liveness (czy proces żyje) i readiness (czy przyjmować ruch).
        // W Springu: spring-boot-starter-actuator → GET /actuator/health; szczegóły składników:
        //            management.endpoint.health.show-details=always; własny wskaźnik: @Component class MailHealth
        //            implements HealthIndicator { public Health health() { return Health.up().build(); } };
        //            port serwera: server.port=8080 (domyślnie), server.port=0 = losowy wolny port.
    }

    // =================================================================================================
    // 8. IDEA SPRING DATA: ZAPYTANIA Z NAZW METOD
    // =================================================================================================

    /** Repozytorium „bez implementacji” — kod napisze proxy na podstawie nazw metod. */
    interface ProductRepository {
        List<Product> findByCategory(Category category);
        List<Product> findByCategoryAndStockGreaterThan(Category category, int minStock);
        Optional<Product> findBySku(String sku);
        long countByPriceLessThan(BigDecimal max);
    }

    interface BrokenRepository {                                            // broken = zepsute
        List<Product> findByColor(String color);
    }

    /** Warunek wyprowadzony z fragmentu nazwy, np. "StockGreaterThan" → stock > ?. */
    record Criterion(String property, String operator) { }                 // criterion = kryterium

    /** "CategoryAndStockGreaterThan" → [category =, stock >]. */
    static List<Criterion> parseCriteria(String afterBy) {
        List<Criterion> result = new ArrayList<>();
        for (String part : afterBy.split("And")) {
            String op = "=";
            if (part.endsWith("GreaterThan")) { op = ">"; part = part.substring(0, part.length() - "GreaterThan".length()); }
            else if (part.endsWith("LessThan")) { op = "<"; part = part.substring(0, part.length() - "LessThan".length()); }
            result.add(new Criterion(Character.toLowerCase(part.charAt(0)) + part.substring(1), op));
        }
        return result;
    }

    /**
     * Tworzy implementację repozytorium jako dynamiczne proxy. Nazwy metod sprawdzamy OD RAZU (przy tworzeniu),
     * tak jak Spring Data przy starcie aplikacji.
     */
    static <T, R extends Record> T repositoryFor(Class<T> repoType, Class<R> entityType, List<R> data) {
        Map<String, RecordComponent> props = new TreeMap<>();
        for (RecordComponent rc : entityType.getRecordComponents()) props.put(rc.getName(), rc);

        Map<String, List<Criterion>> queries = new TreeMap<>();
        for (Method m : Arrays.stream(repoType.getMethods()).sorted(Comparator.comparing(Method::getName)).toList()) {
            String name = m.getName();
            int by = name.indexOf("By");
            if (by < 0 || !(name.startsWith("find") || name.startsWith("count"))) {
                throw new IllegalStateException("nie rozumiem nazwy metody " + name);
            }
            List<Criterion> criteria = parseCriteria(name.substring(by + 2));
            for (Criterion c : criteria) {
                if (!props.containsKey(c.property())) {
                    throw new IllegalStateException("brak właściwości '" + c.property() + "' w "
                            + entityType.getSimpleName() + " (metoda " + name + ")");
                }
            }
            queries.put(name, criteria);
        }

        InvocationHandler handler = (proxy, method, args) -> {
            List<Criterion> criteria = queries.get(method.getName());
            Predicate<R> filter = r -> true;
            for (int i = 0; i < criteria.size(); i++) {
                Criterion c = criteria.get(i);
                Object expected = args[i];
                RecordComponent rc = props.get(c.property());
                filter = filter.and(r -> matches(read(rc, r), c.operator(), expected));
            }
            List<R> found = data.stream().filter(filter).toList();
            if (method.getName().startsWith("count")) return (long) found.size();
            if (method.getReturnType() == Optional.class) {
                if (found.size() > 1) throw new IllegalStateException("oczekiwano najwyżej 1 wyniku, jest " + found.size());
                return found.stream().findFirst();
            }
            return found;
        };
        return repoType.cast(Proxy.newProxyInstance(repoType.getClassLoader(), new Class<?>[]{repoType}, handler));
    }

    private static Object read(RecordComponent rc, Record r) {
        try {
            return rc.getAccessor().invoke(r);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})                            // compareTo na typach z refleksji
    private static boolean matches(Object actual, String operator, Object expected) {
        if (operator.equals("=")) return Objects.equals(actual, expected);
        int cmp = ((Comparable) actual).compareTo(expected);
        return operator.equals(">") ? cmp > 0 : cmp < 0;
    }

    /**
     * 8. Spring Data: piszesz tylko INTERFEJS, a implementację tworzy framework — dynamiczne proxy (t19), które
     * czyta nazwę metody: find/count + By + właściwości połączone And + operatory (GreaterThan, LessThan...).
     */
    static void springDataIdea() {
        section("8. Idea Spring Data: zapytanie wyprowadzone z nazwy metody");

        ProductRepository repo = repositoryFor(ProductRepository.class, Product.class, SampleData.products());
        show("findByCategory(KSIAZKI)", repo.findByCategory(Category.KSIAZKI).stream().map(Product::name).toList());
        // WYNIK: findByCategory(KSIAZKI) → [Czysty kod, Java. Podstawy, Wzorce projektowe]
        show("findByCategoryAndStockGreaterThan(ELEKTRONIKA, 5)",
                repo.findByCategoryAndStockGreaterThan(Category.ELEKTRONIKA, 5).stream().map(Product::name).toList());
        // WYNIK: findByCategoryAndStockGreaterThan(ELEKTRONIKA, 5) → [Laptop Pro 14, Słuchawki BT]
        show("findBySku(DOM-002)", repo.findBySku("DOM-002").map(Product::name));
        // WYNIK: findBySku(DOM-002) → Optional[Lampka biurkowa]
        show("countByPriceLessThan(100)", repo.countByPriceLessThan(new BigDecimal("100")));
        // WYNIK: countByPriceLessThan(100) → 6
        show("parsowanie nazwy", parseCriteria("CategoryAndStockGreaterThan"));
        // WYNIK: parsowanie nazwy → [Criterion[property=category, operator==], Criterion[property=stock, operator=>]]

        // PUŁAPKA: literówka w nazwie metody (findByColor, a pola color nie ma) — Spring Data wykrywa ją przy
        // STARCIE aplikacji, nie przy pierwszym wywołaniu. Nasze proxy też:
        expectThrows("findByColor", () -> repositoryFor(BrokenRepository.class, Product.class, SampleData.products()));
        // WYNIK: ✔ findByColor → rzucono IllegalStateException: brak właściwości 'color' w Product (metoda findByColor)

        // DOBRA PRAKTYKA: nazwy wyprowadzone są świetne dla prostych warunków; przy 3+ warunkach nazwa robi się
        // nieczytelna (findByCategoryAndStockGreaterThanAndPriceLessThanOrderByNameAsc) — wtedy @Query z JPQL/SQL.
        // W Springu: interface ProductRepository extends JpaRepository<ProductEntity, Long> {
        //                List<ProductEntity> findByCategory(Category c); long countByPriceLessThan(BigDecimal max); }
        //            — zapytania SQL generuje Spring Data JPA (Hibernate) do prawdziwej bazy, nie filtr w pamięci.
    }

    // =================================================================================================
    // 9. TESTOWE WYCINKI (slices)
    // =================================================================================================

    /**
     * 9. Pełny kontekst w każdym teście jest wolny. Boot daje „wycinki”: kontekst tylko z tym, co potrzebne
     * do danej warstwy (por. testy warstw atrapami w Spring02).
     */
    static void testingSlices() {
        section("9. Testy w Boot: pełny kontekst a wycinki");

        Map<String, String> slices = new LinkedHashMap<>();
        slices.put("(bez adnotacji)", "zwykły JUnit + Mockito: serwisy, logika — najszybciej");
        slices.put("@WebMvcTest(OrderController.class)", "tylko warstwa web + MockMvc; serwisy jako atrapy");
        slices.put("@DataJpaTest", "tylko JPA + baza w pamięci; każdy test wycofywany (rollback)");
        slices.put("@JsonTest", "tylko konfiguracja Jacksona (format JSON)");
        slices.put("@SpringBootTest", "cały kontekst (opcjonalnie z prawdziwym serwerem na losowym porcie)");
        showEach("adnotacja → co ładuje", slices);
        // WYNIK: adnotacja → co ładuje (liczba kluczy: 5):
        // WYNIK: • (bez adnotacji) → zwykły JUnit + Mockito: serwisy, logika — najszybciej
        // WYNIK: • @WebMvcTest(OrderController.class) → tylko warstwa web + MockMvc; serwisy jako atrapy
        // WYNIK: • @DataJpaTest → tylko JPA + baza w pamięci; każdy test wycofywany (rollback)
        // WYNIK: • @JsonTest → tylko konfiguracja Jacksona (format JSON)
        // WYNIK: • @SpringBootTest → cały kontekst (opcjonalnie z prawdziwym serwerem na losowym porcie)

        // DOBRA PRAKTYKA: piramida testów — dużo szybkich testów jednostkowych, mniej wycinków, kilka pełnych
        // @SpringBootTest. Wstrzykiwanie przez konstruktor sprawia, że serwis przetestujesz w ogóle bez Springa.
        // W Springu: atrapa beana w kontekście: @MockBean (Boot do 3.3; od Boot 3.4 zalecane @MockitoBean
        //            ze Spring Framework 6.2); profil testowy: @ActiveProfiles("test").
    }

    // =================================================================================================
    // 10. CO JUŻ UMIESZ I JAK ZACZĄĆ PROJEKT SPRING BOOT
    // =================================================================================================

    /**
     * 10. Mapa przejścia: temat z tego kursu → co odpowiada mu w Springu (kurs SpringLearning).
     * Masz już fundament — Spring nazwie i zautomatyzuje to, co znasz.
     */
    static void whatNext() {
        section("10. Co już umiesz → czego nauczy SpringLearning");

        Map<String, String> table = new LinkedHashMap<>();
        table.put("DI ręcznie (t22) + kontener (Spring01)", "@Component, @Service, wstrzykiwanie przez konstruktor");
        table.put("adnotacje i refleksja (t19)", "jak Spring czyta @RestController, @Transactional");
        table.put("dynamiczne proxy (t19, Spring01)", "AOP, @Transactional, repozytoria Spring Data");
        table.put("HttpServer/HttpClient (t28, Spring03)", "Spring MVC, @RestController, RestClient");
        table.put("JDBC i transakcje (t29)", "JdbcTemplate, Spring Data JPA, @Transactional");
        table.put("warstwy, DTO, walidacja (Spring02)", "@Valid, @RestControllerAdvice, ProblemDetail");
        table.put("Properties (t18) + Spring04", "application.properties, @ConfigurationProperties, profile");
        table.put("JUnit 5 i Mockito (t32)", "@SpringBootTest, @WebMvcTest, @DataJpaTest");
        table.put("współbieżność (t21)", "@Async, @Scheduled, pule wątków zarządzane przez Springa");
        showEach("umiesz → w Springu", table);
        // WYNIK: umiesz → w Springu (liczba kluczy: 9):
        // WYNIK: • DI ręcznie (t22) + kontener (Spring01) → @Component, @Service, wstrzykiwanie przez konstruktor
        // WYNIK: • adnotacje i refleksja (t19) → jak Spring czyta @RestController, @Transactional
        // WYNIK: • dynamiczne proxy (t19, Spring01) → AOP, @Transactional, repozytoria Spring Data
        // WYNIK: • HttpServer/HttpClient (t28, Spring03) → Spring MVC, @RestController, RestClient
        // WYNIK: • JDBC i transakcje (t29) → JdbcTemplate, Spring Data JPA, @Transactional
        // WYNIK: • warstwy, DTO, walidacja (Spring02) → @Valid, @RestControllerAdvice, ProblemDetail
        // WYNIK: • Properties (t18) + Spring04 → application.properties, @ConfigurationProperties, profile
        // WYNIK: • JUnit 5 i Mockito (t32) → @SpringBootTest, @WebMvcTest, @DataJpaTest
        // WYNIK: • współbieżność (t21) → @Async, @Scheduled, pule wątków zarządzane przez Springa

        // W Springu: JAK ZACZĄĆ PROJEKT SPRING BOOT (start.spring.io):
        //   1. Wejdź na https://start.spring.io — Project: Maven, Language: Java, Spring Boot: najnowsza stabilna 3.x.
        //   2. Group: pl.nauka, Artifact: sklep, Packaging: Jar, Java: 17 (Boot 3 wymaga co najmniej Javy 17).
        //   3. Dependencies (zależności): Spring Web, Validation, Spring Data JPA, H2 Database, Spring Boot Actuator,
        //      opcjonalnie Spring Boot DevTools (automatyczny restart po zmianie kodu).
        //   4. GENERATE → rozpakuj ZIP → w IntelliJ: File → Open → wskaż pom.xml → „Open as Project”.
        //   5. Uruchom klasę z @SpringBootApplication (zielony trójkąt przy main) albo w terminalu: mvnw spring-boot:run
        //      (Windows: mvnw.cmd spring-boot:run).
        //   6. W logu szukaj „Tomcat started on port 8080”; sprawdź http://localhost:8080/actuator/health → {"status":"UP"}.
        //   7. Paczka do uruchomienia: mvnw package → java -jar target/sklep-0.0.1-SNAPSHOT.jar.
        // PUŁAPKA: klasy z @RestController/@Service muszą leżeć w pakiecie klasy z @SpringBootApplication albo
        // w jego podpakietach — inaczej skanowanie ich nie znajdzie i dostaniesz 404 / „brak beana”.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Boot = Spring + startery + auto-konfiguracja + konfiguracja zewnętrzna + wbudowany serwer + actuator.
     *   • Starter: jedna zależność, wersje z spring-boot-starter-parent (nie dopisuj <version>).
     *   • Warstwy konfiguracji (silniejsza wygrywa): argumenty → -D → zmienne środ. → application-{profil} →
     *     application.properties → domyślne. SERVER_PORT ↔ server.port. Sekrety — poza repozytorium.
     *   • @ConfigurationProperties(prefix) + rekord = typowana, walidowana konfiguracja (Duration "30s").
     *   • Profil: application-prod.properties, --spring.profiles.active=prod, @Profile, @ActiveProfiles.
     *   • Auto-konfiguracja: @ConditionalOnClass / OnProperty(havingValue, matchIfMissing) / OnMissingBean;
     *     Twój bean wygrywa; raport: --debug.
     *   • /actuator/health: UP → 200, DOWN → 503; własny HealthIndicator.
     *   • Spring Data: interfejs + nazwy metod (findByXAndYGreaterThan), błędy nazw wykrywane przy starcie.
     *   • Testy: JUnit bez Springa → @WebMvcTest / @DataJpaTest → @SpringBootTest.
     *
     * PYTANIA KONTROLNE:
     *   1. Co dokładnie robi starter i dlaczego nie podajesz wersji bibliotek, które przynosi?
     *   2. Co wypisze (MiniBoot z sekcji 6)?
     *        MiniBoot b = new MiniBoot(Map.of("sklep.mail-enabled", "nie"), List.of(), AUTO_CONFIGS);
     *        System.out.println(b.beans.keySet());
     *   3. server.port jest w application.properties (8081), w zmiennej SERVER_PORT=9000 i w argumencie
     *      --server.port=9090. Jaki port wybierze Spring Boot i dlaczego?
     *   4. ZNAJDŹ BŁĄD: application.properties w repozytorium gita zawiera  spring.datasource.password=Tajne123!
     *   5. Co wypisze:  System.out.println(parseCriteria("NameAndPriceLessThan"));  ?
     *   6. ZNAJDŹ BŁĄD: interfejs  ProductRepository extends JpaRepository<ProductEntity, Long>  ma metodę
     *        List<ProductEntity> findByCategroy(Category c);  — kiedy i jak objawi się błąd?
     *   7. Dlaczego @ConditionalOnMissingBean musi być sprawdzany PO zarejestrowaniu beanów użytkownika?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        runChecks(false);
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        runChecks(true);
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
    }

    private static void runChecks(boolean ref) {
        String s = ref ? " (wzorzec)" : "";
        Check.equal("ćw. 1: SKLEP_MAIL_ENABLED" + s, "sklep.mail.enabled",
                () -> ref ? solution1("SKLEP_MAIL_ENABLED") : exercise1("SKLEP_MAIL_ENABLED"));
        Check.equal("ćw. 1: SERVER_PORT" + s, "server.port", () -> ref ? solution1("SERVER_PORT") : exercise1("SERVER_PORT"));

        Map<String, String> defaults = new TreeMap<>(Map.of("server.port", "8080", "sklep.name", "Sklep"));
        String file = "sklep.name=Sklep u Ani\nserver.port=8081\n";
        Map<String, String> env = new TreeMap<>(Map.of("SERVER_PORT", "9000", "LOG_LEVEL", "DEBUG"));
        String[] args = {"--sklep.name=Sklep Nocny", "dane.csv"};
        Map<String, String> expected = new TreeMap<>(Map.of(
                "log.level", "DEBUG", "server.port", "9000", "sklep.name", "Sklep Nocny"));
        Check.equal("ćw. 2: scalanie warstw" + s, expected,
                () -> ref ? solution2(defaults, file, env, args) : exercise2(defaults, file, env, args));

        Map<String, String> conf = Map.of("a", "true", "b", "false", "c", "TAK");
        Check.equal("ćw. 3: warunki" + s, List.of(true, false, true, false, true, false),
                () -> List.of(
                        ref ? solution3(conf, "a", "true", false) : exercise3(conf, "a", "true", false),
                        ref ? solution3(conf, "b", "true", false) : exercise3(conf, "b", "true", false),
                        ref ? solution3(conf, "c", "", false) : exercise3(conf, "c", "", false),
                        ref ? solution3(conf, "b", "", false) : exercise3(conf, "b", "", false),
                        ref ? solution3(conf, "x", "true", true) : exercise3(conf, "x", "true", true),
                        ref ? solution3(conf, "x", "", false) : exercise3(conf, "x", "", false)));

        Check.equal("ćw. 4: opis zapytania" + s, "find: category = ?, price < ?",
                () -> ref ? solution4("findByCategoryAndPriceLessThan") : exercise4("findByCategoryAndPriceLessThan"));
        Check.equal("ćw. 4: count" + s, "count: stock > ?",
                () -> ref ? solution4("countByStockGreaterThan") : exercise4("countByStockGreaterThan"));
    }

    /**
     * ĆWICZENIE 1 (łatwe): zamień nazwę zmiennej środowiskowej na klucz właściwości: małe litery, „_” → „.”.
     * Podpowiedź: toLowerCase(Locale.ROOT) — z jawnym Locale (np. tureckie „I” zachowuje się inaczej).
     */
    static String exercise1(String envName) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): połącz warstwy (od najsłabszej): domyślne → tekst pliku .properties → zmienne
     * środowiskowe (nazwy zamień jak w ćw. 1) → argumenty "--klucz=wartość" (inne argumenty pomiń).
     * Zwróć TreeMap. Łączy t18_io_files/Io06Properties z tą lekcją.
     * Podpowiedź: Properties.load(new StringReader(file)), stringPropertyNames(), putAll w odpowiedniej kolejności.
     */
    static Map<String, String> exercise2(Map<String, String> defaults, String file, Map<String, String> env, String[] args) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): reguła @ConditionalOnProperty: brak właściwości → matchIfMissing; puste havingValue →
     * pasuje każda wartość różna od "false"; w przeciwnym razie porównanie bez względu na wielkość liter.
     * Podpowiedź: equalsIgnoreCase.
     */
    static boolean exercise3(Map<String, String> config, String name, String havingValue, boolean matchIfMissing) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): opisz zapytanie wyprowadzone z nazwy metody jak Spring Data:
     * {@code "findByCategoryAndPriceLessThan"} → {@code "find: category = ?, price < ?"};
     * obsłuż prefiksy find/count i operatory GreaterThan (>), LessThan (<), brak (=).
     * Podpowiedź: indexOf("By"), split("And"), endsWith(...), mała pierwsza litera właściwości.
     */
    static String exercise4(String methodName) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(String envName) {
        return envName.toLowerCase(java.util.Locale.ROOT).replace('_', '.');
    }

    static Map<String, String> solution2(Map<String, String> defaults, String file, Map<String, String> env, String[] args) {
        Map<String, String> result = new TreeMap<>(defaults);
        result.putAll(fromPropertiesText(file));
        env.forEach((k, v) -> result.put(solution1(k), v));
        result.putAll(fromArgs(args));
        return result;
    }

    static boolean solution3(Map<String, String> config, String name, String havingValue, boolean matchIfMissing) {
        String value = config.get(name);
        if (value == null) return matchIfMissing;
        if (havingValue.isEmpty()) return !value.equalsIgnoreCase("false");
        return value.equalsIgnoreCase(havingValue);
    }

    static String solution4(String methodName) {
        int by = methodName.indexOf("By");
        String prefix = methodName.substring(0, by);
        List<String> parts = new ArrayList<>();
        for (Criterion c : parseCriteria(methodName.substring(by + 2))) parts.add(c.property() + " " + c.operator() + " ?");
        return prefix + ": " + String.join(", ", parts);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Starter to zależność bez własnego kodu, która dociąga spójny zestaw bibliotek (np. web: Spring MVC,
     *      Jackson, Tomcat). Wersje zarządza spring-boot-starter-parent (BOM) — są przetestowane razem, a
     *      własne wersje grożą konfliktami (NoSuchMethodError, ClassNotFoundException w trakcie działania).
     *   2. „[FakePaymentGateway, InMemoryCache]” — „nie” to ani "true" (SmtpMailSender odpada), ani "false"
     *      (NoOpMailSender też odpada, a matchIfMissing nie ma znaczenia, bo właściwość JEST); RedisCache odpada
     *      (brak klasy). Brak MailSender — próba get(MailSender.class) rzuciłaby wyjątek.
     *   3. 9090 — argumenty wiersza poleceń są silniejsze niż zmienne środowiskowe, a te silniejsze niż plik.
     *   4. Hasło w repozytorium widzi każdy z dostępem do kodu (i zostaje w historii gita nawet po usunięciu).
     *      Naprawa: spring.datasource.password=${DB_PASSWORD} albo sama zmienna SPRING_DATASOURCE_PASSWORD
     *      na serwerze / menedżer sekretów; zmień ujawnione hasło.
     *   5. „[Criterion[property=name, operator==], Criterion[property=price, operator=<]]” — toString rekordu
     *      skleja nazwę składnika i wartość znakiem „=”, stąd podwójne „==” przy operatorze równości.
     *   6. Literówka „Categroy” — Spring Data nie znajdzie właściwości categroy i aplikacja NIE wystartuje
     *      (wyjątek przy tworzeniu repozytorium: brak właściwości categroy w ProductEntity). To dobra wiadomość:
     *      błąd wychodzi od razu, a nie u klienta.
     *   7. Bo warunek pyta „czy już jest bean tego typu?”. Gdyby auto-konfiguracje szły pierwsze, zawsze
     *      powstawałby domyślny bean, a Twój byłby drugim (niejednoznaczność) albo nie miałby szans go zastąpić.
     *      Dlatego Boot przetwarza auto-konfiguracje na końcu.
     */
    // </editor-fold>
}
