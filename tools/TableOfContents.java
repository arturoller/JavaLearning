import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TableOfContents = spis treści. Generuje plik SPIS_TRESCI.md: wszystkie działy i lekcje kursu jako klikalne linki
 * (działają na GitHubie i w podglądzie Markdown w IntelliJ), z jednym zdaniem opisu z nagłówka TEMAT: każdej lekcji.
 *
 * Uruchomienie (z katalogu głównego repozytorium, JDK 17):
 *   java -Dfile.encoding=UTF-8 tools/TableOfContents.java
 *
 * Skąd bierze dane:
 *   - kolejność działów: katalogi src/tNN_* posortowane po nazwie,
 *   - opis działu: linia "TEMAT:" z package-info.java (część po myślniku),
 *   - kolejność lekcji: lista czytania (elementy li) w package-info.java; lekcje spoza listy trafiają na koniec,
 *   - opis lekcji: pierwsza linia "TEMAT:" z nagłówka Javadoc lekcji.
 * Uruchom ponownie po dodaniu lub zmianie nazwy lekcji — plik jest w całości generowany (nie edytuj go ręcznie).
 */
public class TableOfContents {

    private static final Pattern TOPIC = Pattern.compile("TEMAT:\\s*(.+)");
    private static final Pattern LIST_ITEM = Pattern.compile("<li>(.*?)</li>", Pattern.DOTALL);
    private static final Pattern CODE_TAG = Pattern.compile("\\{@(?:code|link)\\s+([^}]*)}");

    public static void main(String[] args) throws IOException {
        Path root = Path.of("").toAbsolutePath();
        Path src = root.resolve("src");
        if (!Files.isDirectory(src.resolve("helpers"))) {
            System.out.println("Uruchom z katalogu głównego repozytorium JavaLearning (brak src/helpers).");
            System.exit(1);
        }

        List<Path> sections;
        try (Stream<Path> s = Files.list(src)) {
            sections = s.filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().matches("t\\d\\d_.+"))
                    .sorted()
                    .collect(Collectors.toList());
        }

        String readme = read(root.resolve("README.md"));
        StringBuilder toc = new StringBuilder();
        StringBuilder body = new StringBuilder();
        int lessonCount = 0;
        for (Path section : sections) {
            String name = section.getFileName().toString();
            String packageInfo = read(section.resolve("package-info.java"));
            String sectionTopic = readmeName(readme, name);
            if (sectionTopic.isEmpty()) {
                sectionTopic = sectionTopic(packageInfo, name);
            }
            List<String> lessons = lessonOrder(section, packageInfo);
            lessonCount += lessons.size();

            toc.append("- [").append(name).append("](#").append(name).append(") — ")
                    .append(sectionTopic).append(" (").append(lessons.size()).append(")\n");

            body.append("\n## ").append(name).append("\n\n");
            body.append(sectionTopic).append(" · [folder z lekcjami](src/").append(name).append(")\n\n");
            int number = 1;
            for (String lesson : lessons) {
                String topic = lessonTopic(read(section.resolve(lesson + ".java")));
                body.append(number++).append(". [").append(lesson).append("](src/").append(name).append('/')
                        .append(lesson).append(".java)");
                if (!topic.isEmpty()) {
                    body.append(" — ").append(topic);
                }
                body.append('\n');
            }
        }

        String out = "# Spis treści kursu JavaLearning\n\n"
                + "Wszystkie działy i lekcje z linkami. Plik jest generowany przez `tools/TableOfContents.java` — nie edytuj go "
                + "ręcznie. Opisy działów ze stanem prac: [README](README.md#spis-treści), hasła od A do Z: "
                + "[indeks](README.md#indeks-haseł-az).\n\n"
                + "Działów: " + sections.size() + " · lekcji: " + lessonCount + "\n\n"
                + "- [helpers](src/helpers) — wspólne narzędzia lekcji (`Console`, `Check`, `TempDir`) i dane przykładowe "
                + "(`SampleData`, `model/`)\n"
                + toc
                + body;
        Files.writeString(root.resolve("SPIS_TRESCI.md"), out, StandardCharsets.UTF_8);
        System.out.println("Zapisano SPIS_TRESCI.md: działów " + sections.size() + ", lekcji " + lessonCount);
    }

    /**
     * Nazwa działu z tabeli „Spis treści” w README: z wiersza "| `tNN_x` | Napisy (9 lekcji): ... |" bierze "Napisy"
     * (tekst do pierwszego nawiasu, dwukropka albo przecinka). Pusty napis, gdy wiersza nie ma.
     */
    private static String readmeName(String readme, String name) {
        for (String row : readme.split("\n")) {
            if (row.startsWith("| `" + name + "`") || row.startsWith("| [`" + name + "`]")) {
                String[] cells = row.split("\\|");
                if (cells.length > 2) {
                    String text = cells[2].replace("*", "").replace("**Nowy.**", "").trim();
                    text = text.replaceFirst("^Nowy\\.\\s*", "");
                    String cut = text.split("[(:,]", 2)[0].trim();
                    return cut;
                }
            }
        }
        return "";
    }

    /** Opis działu: tekst po myślniku w linii TEMAT: pliku package-info.java. */
    private static String sectionTopic(String packageInfo, String name) {
        Matcher m = TOPIC.matcher(packageInfo);
        if (!m.find()) {
            return name;
        }
        String topic = clean(m.group(1));
        int dash = topic.indexOf(" — ");
        return dash >= 0 ? topic.substring(dash + 3).trim() : topic;
    }

    /** Kolejność lekcji: najpierw według listy czytania z package-info, potem pozostałe alfabetycznie. */
    private static List<String> lessonOrder(Path section, String packageInfo) throws IOException {
        List<String> all;
        try (Stream<Path> s = Files.list(section)) {
            all = s.map(p -> p.getFileName().toString())
                    .filter(f -> f.endsWith(".java") && !f.equals("package-info.java"))
                    .map(f -> f.substring(0, f.length() - 5))
                    .sorted()
                    .collect(Collectors.toList());
        }
        Set<String> ordered = new LinkedHashSet<>();
        Matcher item = LIST_ITEM.matcher(packageInfo);
        while (item.find()) {
            String text = item.group(1);
            String best = null;
            int bestPos = Integer.MAX_VALUE;
            for (String lesson : all) {
                Matcher word = Pattern.compile("\\b" + lesson + "\\b").matcher(text);
                if (word.find() && word.start() < bestPos) {
                    bestPos = word.start();
                    best = lesson;
                }
            }
            if (best != null) {
                ordered.add(best);
            }
        }
        ordered.addAll(all);
        return new ArrayList<>(ordered);
    }

    /**
     * Opis lekcji: nagłówek TEMAT: razem z liniami-kontynuacjami — aż do pustej linii komentarza albo linii
     * zaczynającej się nawiasem (tam zwykle są tłumaczenia słówek).
     */
    private static String lessonTopic(String source) {
        Matcher m = TOPIC.matcher(source);
        if (!m.find()) {
            return "";
        }
        StringBuilder topic = new StringBuilder(m.group(1));
        String rest = source.substring(m.end());
        for (String line : rest.split("\\n")) {
            String t = line.replaceFirst("^\\s*\\*", "").trim();
            if (t.isEmpty()) {
                continue;
            }
            if (t.startsWith("(") || t.endsWith(":") || t.contains("W SKRÓCIE:") || t.startsWith("</pre>")) {
                break;
            }
            topic.append(' ').append(t);
            if (t.endsWith(".") || topic.length() > 160) {
                break;
            }
        }
        return clean(topic.toString());
    }

    /** Usuwa znaczniki Javadoc ({@code x} → x) i nadmiarowe spacje; zamienia | (psułby tabele Markdown). */
    private static String clean(String text) {
        String t = CODE_TAG.matcher(text).replaceAll("$1");
        return t.replace("|", "/").replaceAll("\\s+", " ").trim();
    }

    private static String read(Path file) throws IOException {
        return Files.exists(file) ? Files.readString(file, StandardCharsets.UTF_8) : "";
    }
}
