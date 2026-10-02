import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Course verifier (portable: Windows / Linux / macOS). Run from anywhere INSIDE the repository:
 *   java -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 tools/Verify.java [--tag X] [--scope glob ...] [--run all] [--show]
 * Scope globs are relative to <repo>/src with forward slashes, e.g. t16_streams/Streams1[89]*
 * helpers/** is always compiled. No scope = whole course.
 * REQUIRES JDK 17 (all WYNIK lines were recorded on Temurin 17; other JDKs change exception messages and locale data).
 * Libraries (Lombok, later H2/JUnit): jars in <repo>/temp/lib (./mvnw -q dependency:copy-dependencies -DoutputDirectory=temp/lib),
 * fallback: Lombok from ~/.m2.
 */
public class Verify {
    static final Path ROOT = findRoot();
    static final Path SRC_ROOT = ROOT.resolve("src");
    static final Path SRC = SRC_ROOT;
    /** TOOLS = committed tools: lessons.txt (registry), tags.txt. WORK = git-ignored temp/ (build output, libraries). */
    static final Path TOOLS = ROOT.resolve("tools");
    static final Path WORK = ROOT.resolve("temp");
    static final String JAVA_BIN = Path.of(System.getProperty("java.home"), "bin").toString();
    static final List<String> problems = new ArrayList<>();
    static boolean checkOrder = true;

    static Path findRoot() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null && !Files.isDirectory(p.resolve("src").resolve("helpers"))) p = p.getParent();
        if (p == null) throw new IllegalStateException("Run Verify from inside the JavaLearning repository (src/helpers not found)");
        return p;
    }

    /** libraries = jars from temp/lib, or Lombok from the local Maven repository as a fallback. */
    static List<String> libraries() throws IOException {
        List<String> jars = new ArrayList<>();
        Path lib = WORK.resolve("lib");
        if (Files.isDirectory(lib)) {
            try (Stream<Path> s = Files.list(lib)) {
                s.filter(p -> p.toString().endsWith(".jar")).sorted().forEach(p -> jars.add(p.toAbsolutePath().toString()));
            }
        }
        if (jars.stream().noneMatch(j -> j.contains("lombok"))) {
            Path m2 = Path.of(System.getProperty("user.home"), ".m2", "repository", "org", "projectlombok", "lombok", "1.18.38", "lombok-1.18.38.jar");
            if (Files.exists(m2)) jars.add(m2.toString());
        }
        return jars;
    }

    public static void main(String[] args) throws Exception {
        String tag = "main";
        List<String> scopes = new ArrayList<>();
        boolean runAll = false;
        boolean show = false;
        boolean anyJdk = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--tag" -> tag = args[++i];
                case "--scope" -> { while (i + 1 < args.length && !args[i + 1].startsWith("--")) scopes.add(args[++i]); }
                case "--run" -> runAll = "all".equals(args[++i]);
                case "--show" -> show = true;
                case "--any-jdk" -> anyJdk = true;
                case "--no-order" -> checkOrder = false;
                default -> throw new IllegalArgumentException("unknown arg " + args[i]);
            }
        }
        if (Runtime.version().feature() != 17 && !anyJdk) {
            System.out.println("ERROR: JDK 17 required, running on " + Runtime.version()
                    + ". WYNIK lines were recorded on JDK 17 — other versions give false mismatches. Install/select JDK 17.");
            System.exit(2);
        }
        List<String> libs = libraries();
        String classpath = String.join(java.io.File.pathSeparator, libs);
        String lombok = libs.stream().filter(j -> j.contains("lombok")).findFirst().orElse("");
        List<PathMatcher> matchers = new ArrayList<>();
        for (String s : scopes) matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + s.replace('\\', '/')));

        List<Path> all;
        try (Stream<Path> w = Files.walk(SRC)) { all = w.filter(Files::isRegularFile).sorted().collect(Collectors.toList()); }
        List<Path> inScope = new ArrayList<>();
        for (Path p : all) {
            String rel = rel(p);
            boolean ok = matchers.isEmpty() || rel.startsWith("helpers/");
            for (PathMatcher m : matchers) if (m.matches(Path.of(rel))) ok = true;
            if (ok) inScope.add(p);
        }
        List<Path> javaFiles = inScope.stream().filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());

        // 1. compile
        Path out = WORK.resolve("build").resolve("vout-" + tag + "-" + System.nanoTime());
        Files.createDirectories(out);
        Path argFile = WORK.resolve("build").resolve("vsources-" + tag + ".txt");
        Files.write(argFile, javaFiles.stream().map(p -> "\"" + p.toString().replace('\\', '/') + "\"").collect(Collectors.toList()));
        List<String> javac = new ArrayList<>(List.of(Path.of(JAVA_BIN, "javac").toString(),
                "-J-Dfile.encoding=UTF-8", "-J-Dsun.stdout.encoding=UTF-8", "-J-Dsun.stderr.encoding=UTF-8",   // diagnostics in UTF-8
                "--release", "17", "-g", "-encoding", "UTF-8", "-Xlint:all", "-Xlint:-processing", "-d", out.toString()));
        if (!classpath.isEmpty()) javac.addAll(List.of("-cp", classpath));
        if (!lombok.isEmpty()) javac.addAll(List.of("-processorpath", lombok));
        javac.add("@" + argFile);
        ProcResult c = exec(javac, 300);
        if (!c.out.isBlank()) System.out.println(c.out.strip());
        System.out.println("COMPILE EXIT: " + c.exit + "  (files: " + javaFiles.size() + ")");
        if (c.exit != 0) return;
        if (c.out.contains("warning:")) problems.add("WARNING : javac reported warnings - see compiler output above");

        // 2. static checks
        List<String> tagLines = Files.readAllLines(TOOLS.resolve("tags.txt"), StandardCharsets.UTF_8);
        List<String> req = new ArrayList<>(), lint = new ArrayList<>();
        String ex = "";
        for (String t : tagLines) {
            if (t.startsWith("REQ|")) req.add(t.substring(4));
            else if (t.startsWith("EX|")) ex = t.substring(3);
            else if (t.startsWith("LINT|")) lint.add(t.substring(5));
        }
        Set<String> regClasses = new HashSet<>(), regPackages = new HashSet<>();
        for (String r : Files.readAllLines(TOOLS.resolve("lessons.txt"), StandardCharsets.UTF_8)) {
            if (r.isBlank()) continue;
            regClasses.add(r.strip());
            regPackages.add(r.strip().split("/")[0]);
        }
        Pattern ref = Pattern.compile("\\b(t\\d\\d_[a-z_]+)(?:/([A-Z][A-Za-z0-9]+))?");
        Pattern projectImport = Pattern.compile("(?m)^\\s*import\\s+(static\\s+)?(superTaskOne|lambda|io|tasks|exceptions|productcatalog)\\.");
        Pattern lessonImport = Pattern.compile("(?m)^\\s*import\\s+(static\\s+)?t\\d\\d_");
        Pattern entity = Pattern.compile("&(lt|gt|amp);");
        Pattern invisible = Pattern.compile("[\u00A0\u202F\u2007\u200B\uFEFF]");
        for (Path p : inScope) {
            String rel = rel(p);
            boolean isJava = rel.endsWith(".java");
            if (!isJava && !rel.endsWith(".md")) continue;
            String text = Files.readString(p, StandardCharsets.UTF_8);
            if (isJava && entity.matcher(text).find()) problems.add("ENTITY  " + rel + " : HTML entity found (use {@code ...})");
            if (isJava && invisible.matcher(text).find()) problems.add("INVISIBLE " + rel + " : invisible char (NBSP/zero-width) - use a unicode escape");
            if (projectImport.matcher(text).find()) problems.add("IMPORT  " + rel + " : import from project package");
            if (isJava && lessonImport.matcher(text).find()) problems.add("IMPORT  " + rel + " : import from another lesson package");
            Matcher m = ref.matcher(text);
            while (m.find()) {
                if (!regPackages.contains(m.group(1))) { problems.add("REF     " + rel + " : unknown package '" + m.group(1) + "'"); continue; }
                if (m.group(2) != null && !regClasses.contains(m.group(1) + "/" + m.group(2)))
                    problems.add("REF     " + rel + " : unknown lesson '" + m.group(1) + "/" + m.group(2) + "'");
            }
            String[] lines = text.split("\n");
            for (int i = 0; i < lines.length; i++) {
                for (String t : lint) {
                    String q = Pattern.quote(t);
                    if (lines[i].matches("^\\s*(//|\\*)\\s*" + q + "(?!:)(\\s.*|$|[^A-Za-z].*)") && !lines[i].matches("^\\s*(//|\\*)\\s*" + q + ":.*"))
                        problems.add("TAGFORM " + rel + ":" + (i + 1) + " : '" + lines[i].strip() + "'");
                }
            }
            String fileName = p.getFileName().toString();
            if (isJava && rel.matches("t\\d\\d_.*") && !fileName.equals("package-info.java")) {
                for (String t : req) if (!text.contains(t)) problems.add("MISSING " + rel + " : tag '" + t + "'");
                boolean needsEx = !rel.startsWith("t00_") || fileName.equals("Start01HowToUse.java");
                if (needsEx && !text.contains(ex)) problems.add("MISSING " + rel + " : exercises");
                if (needsEx && !text.contains("defaultstate=\"collapsed\"")) problems.add("MISSING " + rel + " : collapsed editor-fold");
                String key = rel.split("/")[0] + "/" + fileName.replace(".java", "");
                if (!regClasses.contains(key)) problems.add("REGISTRY " + rel + " : class not in lessons.txt");
            }
        }

        // 3. run + WYNIK
        int failed = 0;
        if (runAll) {
            Pattern wynik = Pattern.compile("^\\s*//\\s*WYNIK:\\s?(.*)$");
            for (Path p : javaFiles) {
                String text = Files.readString(p, StandardCharsets.UTF_8);
                if (!text.contains("public static void main(")) continue;
                String cls = SRC_ROOT.relativize(p).toString().replace('\\', '.').replace('/', '.').replaceAll("\\.java$", "");
                String runCp = classpath.isEmpty() ? out.toString() : out + java.io.File.pathSeparator + classpath;
                ProcResult r = exec(List.of(Path.of(JAVA_BIN, "java").toString(), "-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8",
                        "-Dsun.stderr.encoding=UTF-8", "-cp", runCp, cls), 30);
                List<String> outList = new ArrayList<>();
                for (String l : r.out.split("\n")) outList.add(l.strip());
                Set<String> outLines = new HashSet<>(outList);
                int checked = 0, missing = 0, pos = 0;
                for (String l : text.split("\n")) {
                    Matcher m = wynik.matcher(l.replace("\r", ""));
                    if (!m.matches()) continue;
                    String expected = m.group(1).replaceAll("\\s{2,}\u2190.*$", "").strip();
                    if (expected.isEmpty() || expected.startsWith("(")) continue;
                    checked++;
                    if (!outLines.contains(expected)) { missing++; problems.add("WYNIK   " + cls + " : not in output: '" + expected + "'"); continue; }
                    // ORDER: WYNIK lines must appear in the output in the same order as in the source (catches a comment that
                    // matches the right text printed in a DIFFERENT place). Search forward from the previous match.
                    int found = outList.subList(pos, outList.size()).indexOf(expected);
                    if (found >= 0) pos = pos + found + 1;
                    else if (checkOrder) problems.add("ORDER   " + cls + " : out of order: '" + expected + "'");
                }
                long crosses = r.out.chars().filter(ch -> ch == '\u2718').count();
                System.out.println("RUN " + cls + " -> exit " + r.exit + ", WYNIK checked: " + checked + ", mismatches: " + missing + ", cross-marks: " + crosses);
                if (show) System.out.println(r.out);
                if (r.exit != 0 || !r.err.isBlank()) { failed++; System.out.println("STDERR: " + r.err); }
            }
        }
        System.out.println();
        System.out.println("PROBLEMS: " + problems.size());
        problems.forEach(pr -> System.out.println("  " + pr));
        if (runAll) System.out.println("FAILED RUNS: " + failed);
    }

    static String rel(Path p) { return SRC.relativize(p).toString().replace('\\', '/'); }

    record ProcResult(int exit, String out, String err) { }

    static ProcResult exec(List<String> cmd, int timeoutSec) throws IOException, InterruptedException {
        Path o = Files.createTempFile("vout", ".txt"), e = Files.createTempFile("verr", ".txt");
        Process pr = new ProcessBuilder(cmd).redirectOutput(o.toFile()).redirectError(e.toFile()).start();
        if (!pr.waitFor(timeoutSec, TimeUnit.SECONDS)) { pr.destroyForcibly(); return new ProcResult(-1, "", "TIMEOUT"); }
        // lenient decoding (malformed bytes → replacement char) — never crash on unexpected console encodings
        return new ProcResult(pr.exitValue(), new String(Files.readAllBytes(o), StandardCharsets.UTF_8),
                new String(Files.readAllBytes(e), StandardCharsets.UTF_8));
    }
}
