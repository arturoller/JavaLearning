# Assignments (one agent at a time; each agent reads AGENT_KIT.md + ONLY its own section here)

## P — t19_annotations_reflection (Opus)   · TAG `refl` · scope `"t19_annotations_reflection/*"`
Folder `E:\java\JavaLearning\src\t19_annotations_reflection\` (create). Package `t19_annotations_reflection`. package-info.java +
7 lessons. Learner knows t01–t18 (OOP, generics, collections, lambdas, streams, exceptions, IO). NO Lombok here.
DETERMINISM: getDeclaredFields/Methods/Constructors order is NOT guaranteed — always sort by name before printing.
-Xlint:all is on: deliberately using a @Deprecated API needs @SuppressWarnings("deprecation") with an explanation, otherwise
the verifier fails on the warning. Use helpers.TempDir for any files (create + deleteRecursively in finally).
1. Annotations01BuiltIn — what an annotation is (metadata, does nothing by itself — someone must READ it: compiler, tools,
   frameworks at runtime); @Override, @Deprecated(since, forRemoval) and the warning it causes, @SuppressWarnings (unchecked,
   rawtypes, deprecation — narrowest scope, with reason), @FunctionalInterface (compile error in a comment), @SafeVarargs;
   meta-annotations @Retention, @Target, @Documented, @Inherited, @Repeatable (overview table).
2. Annotations02Custom — declaring annotations: elements, default values, allowed types (primitives, String, Class, enum,
   annotation, arrays), the special element `value`; retention SOURCE vs CLASS vs RUNTIME (prove: getAnnotation returns null for
   CLASS retention); @Target choices; @Repeatable with a container annotation; @Inherited works for superclasses, not interfaces.
3. Annotations03ReflectionBasics — Class objects (.class, getClass, Class.forName with fully-qualified name incl. $ for nested),
   fields/methods/constructors (sorted), Modifier, Method.invoke (and InvocationTargetException wrapping the real exception),
   reading/writing a private field with setAccessible (works for own classes; JDK internals → InaccessibleObjectException due to
   modules — demo via expectThrows on e.g. String's "value" field), creating instances via getDeclaredConstructor().newInstance(),
   records (getRecordComponents, Java 16+), enums; cost and dangers (no compile-time checks, slower, breaks encapsulation) —
   reflection is for frameworks/tools, not everyday code.
4. Annotations04Validator — mini validation framework: @NotBlank, @Min, @Max, @Size(min, max), @Pattern(regex) on fields;
   Validator.validate(Object) reads fields via reflection and returns a SORTED list of violations "field: message"; validate a
   class and a record; compare with Jakarta Bean Validation (@Valid in Spring — SpringLearning).
5. Annotations05MiniFramework — command router: @Command(name, help) on methods of a handler object; registry built via reflection
   (TreeMap); dispatch("add 2 3"), "help" lists commands sorted, unknown command → message; String → int/double parameter
   conversion by parameter types; this is Spring MVC (@GetMapping) in miniature — explain the analogy.
6. Annotations06DynamicProxy — java.lang.reflect.Proxy + InvocationHandler for interfaces: logging proxy (print method + args),
   call-counting proxy (counts, never timings), caching proxy, "transaction" proxy (begin/commit/rollback around the call);
   Proxy works only for INTERFACES (classes need byte-code libraries like ByteBuddy/CGLIB — what Spring uses); self-invocation
   pitfall (a call inside the target bypasses the proxy — same as @Transactional in Spring); equals/hashCode/toString also go
   through the handler; unwrapping InvocationTargetException.
7. Annotations07Processors — compile-time annotation processing: AbstractProcessor, @SupportedAnnotationTypes, override
   getSupportedSourceVersion() returning SourceVersion.latestSupported(), process(), Messager (compile errors/warnings), Filer
   (generate a source file). RUNNABLE DEMO inside main: write a tiny source (annotated with the lesson's own @ToStringGen or
   @CheckNoArgConstructor annotation) into a TempDir, compile it with ToolProvider.getSystemJavaCompiler() and
   task.setProcessors(List.of(new MyProcessor())), collect diagnostics with DiagnosticCollector and print them (kind + message,
   deterministic), print the generated source file; explain how Lombok differs (modifies the compiler's syntax tree via internal
   APIs) vs MapStruct/AutoValue/Dagger (generate new files); registration via META-INF/services in comments.
Exercises 3–4 per lesson, graded, deterministic.

## Q — t20_lombok (Sonnet)   · TAG `lmb` · scope `"t20_lombok/*"`
Folder `E:\java\JavaLearning\src\t20_lombok\` (create). Package `t20_lombok`; imports `lombok.*` allowed ONLY here (the verifier
compiles with lombok-1.18.38 on the classpath and processor path). package-info.java + 4 lessons. Learner knows t01–t19
(incl. records, equals/hashCode, immutability, builder pattern, annotations and annotation processors).
RULES: show for every Lombok annotation the equivalent hand-written code ("co Lombok generuje" — delombok view) in comments;
prove generated members exist by calling them (or listing method names via reflection, SORTED). Lombok warnings count as
problems (e.g. @Builder on a field with an initializer without @Builder.Default) — fix them or demonstrate them only in comments.
Never let anything print to stderr: @Log (java.util.logging) writes to stderr by default — do not log at runtime, or attach a
handler that writes to System.out. Mention once: in IntelliJ the Lombok plugin is bundled and annotation processing must be enabled.
1. Lombok01Accessors — what Lombok is (annotation processor that adds code during compilation), @Getter/@Setter on fields and
   class, AccessLevel, boolean getter naming (isActive), @ToString (exclude, includeFieldNames, callSuper), @EqualsAndHashCode
   (exclude, callSuper=true needed in subclasses — pitfall demo), PRZED (hand-written, ~40 lines) / PO (Lombok, ~5 lines).
2. Lombok02Constructors — @NoArgsConstructor, @AllArgsConstructor, @RequiredArgsConstructor (final and @NonNull fields), @NonNull
   null checks (copy the real NPE message from a run), staticName = "of" factories, constructor injection style (Spring services
   use @RequiredArgsConstructor), pitfall: reordering fields silently changes the all-args constructor signature.
3. Lombok03DataValueBuilder — @Data (what it bundles) and its dangers (mutable equals/hashCode in HashSet, JPA entities), @Value
   (immutable) vs Java record (comparison table: when record is the better choice), @Builder (fluent creation, @Builder.Default
   pitfall, @Singular for collections, toBuilder = true), @With (withers on immutable objects).
4. Lombok04Other — @Cleanup vs try-with-resources (prefer TWR), @SneakyThrows (hides checked exceptions — pitfall), @Synchronized,
   val/var, @Log family (JUL only; SLF4J needs a dependency), lombok.config (accessors.chain, addLombokGeneratedAnnotation) in
   comments, delombok (IntelliJ action) to see generated code, pros/cons and team policy, records as the modern alternative,
   upgrade risk (Lombok relies on compiler internals — new JDK versions may need a new Lombok version).
Exercises 3–4 per lesson, graded, deterministic.

## O — t24_algorithms ALGORITHMS PART (Algorithms01–08)   · TAG `algo` · scope `"t24_algorithms/Algorithms*"`
Folder `E:\java\JavaLearning\src\t24_algorithms\` — package-info.java and Math01–Math10 ALREADY exist: never modify them.
Write ONLY Algorithms01–08. Learner knows t01–t23 (arrays, recursion, collections, generics, streams). Every count/result in
WYNIK from a real run. NEVER print wall-clock timings — measure work by COUNTING operations (comparisons, swaps, visits).
Each lesson: idea in plain Polish → step-by-step trace on a tiny input → code → complexity O(...) → pitfalls.
Mark classic interview questions with the words "PYTANIE REKRUTACYJNE" in the section comment text.
1. Algorithms01Complexity — what O() means (growth, not seconds), O(1), O(log n), O(n), O(n log n), O(n²), O(2^n) with counted
   examples (array access, binary search steps, single loop, nested loops, naive Fibonacci calls); table of n = 10/1000/10^6;
   best/average/worst case; space complexity; amortized O(1) of ArrayList.add (counted copies); rules (drop constants, dominant term).
2. Algorithms02Sorting — bubble, selection, insertion (count comparisons and swaps on the same 8-element array), merge sort and
   quick sort (recursion trace on small input, pivot choice, worst case on sorted input), stability explained (sorting records by
   two keys), what Arrays.sort/Collections.sort use (dual-pivot quicksort for primitives, TimSort for objects — stable).
3. Algorithms03Searching — linear vs binary search (iterative and recursive, counted steps), the overflow-safe midpoint
   (low + (high - low) / 2 and the famous bug), lower/upper bound (first/last occurrence), binary search on the answer
   (integer square root, minimal capacity), Arrays.binarySearch return value for missing elements (-(insertion point) - 1).
4. Algorithms04DataStructures — hand-written: dynamic array (grow ×2), singly linked list (add/remove/reverse), stack and queue
   on arrays (circular buffer), binary heap (sift up/down, min-heap), binary search tree (insert, contains, in-order traversal),
   simple hash table with chaining; for each: operations and O(); compare with the JDK class to use in real code.
5. Algorithms05Classics — two pointers (pair with given sum in sorted array, remove duplicates in place), sliding window
   (max sum of k consecutive), prefix sums (range sum queries), frequency counting with arrays/maps (anagram check, first unique),
   Kadane's maximum subarray, Dutch national flag, merging two sorted arrays; each with brute force vs better complexity.
6. Algorithms06Backtracking — the backtracking template (choose → explore → un-choose), subsets, permutations, combinations of k,
   N-Queens for n = 4..6 (count solutions: 2, 10, 4), sudoku-like constraint check on a tiny grid or word search, pruning and why it
   matters (count visited states with and without pruning).
7. Algorithms07DynamicProgramming — overlapping subproblems + optimal substructure; Fibonacci top-down (memo) vs bottom-up (table)
   vs O(1) space; climbing stairs; coin change (min coins and number of ways — Polish coins 1, 2, 5); longest common subsequence
   with the table printed; 0/1 knapsack small example; how to recognise a DP problem; counted calls naive vs memo.
8. Algorithms08Graphs — representations (adjacency matrix vs adjacency list with Map<String, List<String>>), BFS (shortest path
   in unweighted graph, levels, path reconstruction) and DFS (recursive and with a stack, connected components, cycle detection),
   topological sort (course prerequisites), Dijkstra on a small weighted graph of Polish cities (distances made up but fixed),
   PriorityQueue usage; complexity O(V + E) and O((V + E) log V).
Exercises 3–5 per lesson, graded; deterministic.

## N — t24_algorithms MATH PART (Math01–Math10)   · TAG `math` · scope `"t24_algorithms/Math*"`
Folder `E:\java\JavaLearning\src\t24_algorithms\` (create if missing). Package `t24_algorithms`. If `package-info.java` does not
exist yet, create it with the reading order: Algorithms01Complexity … Algorithms08Graphs, then Math01 … Math10 (if it exists, do
not touch it). Write ONLY Math01–Math10 (another agent may write Algorithms01–08 later/at the same time — never touch them).
Learner knows t01–t23 (incl. recursion, arrays, BigInteger/BigDecimal, bit operators, collections, streams). Every numeric WYNIK
must come from a real run. Random only with a seed (new Random(42)). Each lesson: explain the MATH idea in plain Polish first
(what, why it works, complexity O(...)), then code, then pitfalls (overflow, floating point, off-by-one, negative numbers).
Mark classic interview questions with "PYTANIE REKRUTACYJNE" in the text of the section comment (not as a tag with a colon).
1. Math01NumberTheory — divisibility, GCD by Euclid (iterative + recursive, why it works), LCM via GCD (overflow-safe order:
   a / gcd * b), prime test by trial division up to √n, sieve of Eratosthenes (primes ≤ 100), prime factorization (360 → 2^3·3^2·5),
   number of divisors; complexity comparison.
2. Math02ModularChecksums — modulo with negative numbers (Math.floorMod vs %), fast modular exponentiation (square-and-multiply),
   check digits in practice: Luhn (credit card test numbers like 4539 1488 0343 6467 — use known-valid TEST numbers only), PESEL
   check digit and birth date/sex decoding (use a fictional valid PESEL computed in code), NIP check digit (weights 6,5,7,2,3,4,5,6,7),
   IBAN mod 97 (move 4 chars, letters → numbers, BigInteger mod 97 == 1; use a published example IBAN such as the standard
   GB82 WEST 1234 5698 7654 32). Say clearly that a correct checksum does not mean the number exists.
3. Math03Combinatorics — factorial, permutations count n!, variations, combinations C(n,k) (multiplicative formula avoiding overflow,
   Pascal triangle rows 0–6 printed), generating all permutations of "ABC" and all 2-element combinations of {1,2,3,4}
   (recursion/backtracking), counting subsets 2^n with bit masks; lottery odds C(49,6) = 13983816.
4. Math04NumberSystems — positional systems, Integer.toString(n, radix)/parseInt(s, radix), manual conversion to base 2 and back,
   hex/octal literals, two's complement for negatives (Integer.toBinaryString(-1)), Roman numerals both directions (MCMXCIV = 1994),
   pitfall: leading zero literal 010 is octal.
5. Math05NumericalMethods — Newton's method for √2 (iterations table), bisection for x^3 - x - 2 = 0, numerical integration of
   x^2 on [0,1] with trapezoid and Simpson (compare with exact 1/3), Monte Carlo π with new Random(42) (print rounded value and
   explain convergence), tolerance/epsilon comparisons, pitfall: == on doubles and accumulated error.
6. Math06Statistics — mean, median (even/odd count), mode, range, variance (population vs sample), standard deviation,
   percentiles (nearest-rank), moving average, IntSummaryStatistics/DoubleSummaryStatistics from streams; use SampleData employee
   salaries where natural; pitfall: mean is sensitive to outliers (show with one huge salary).
7. Math07MatricesGeometry — 2D arrays as matrices: addition, multiplication (dimension rule), transpose, identity, determinant 2x2
   and 3x3 (Sarrus); geometry: distance between points, polygon area by the shoelace formula, point-in-rectangle and
   point-in-polygon (ray casting), orientation/cross product sign; small records Point(double x, double y).
8. Math08BigNumbers — when long overflows (21! > Long.MAX_VALUE, Math.multiplyExact throws), BigInteger factorial of 50,
   Fibonacci(100), BigInteger.pow/mod/gcd/isProbablePrime, BigDecimal sqrt with MathContext (Java 9+) to 30 digits,
   1/3 with MathContext and RoundingMode; pitfall: BigDecimal division without MathContext → ArithmeticException.
9. Math09InterviewClassics — factorial (iterative, recursive, overflow of int at 13! and long at 21!, trailing zeros of n! by
   counting factors of 5), Fibonacci (naive recursion call count vs iteration vs memo), isPrime, perfect numbers (6, 28, 496),
   Armstrong numbers (153, 370, 371, 407), reverse number and digit sum without String, number palindrome, power of two
   (n > 0 && (n & (n - 1)) == 0), Integer.bitCount vs manual count, leap year rule, integer square root by binary search, swap
   without temp (and why not to do it in real code), FizzBuzz variants. Each as a tiny method + demo line.
10. Math10PuzzlesEuler — Project-Euler-style problems solved step by step with brute force first, then a smarter version:
   sum of multiples of 3 or 5 below 1000 (233168; formula with arithmetic series), sum of even Fibonacci ≤ 4,000,000 (4613732),
   largest prime factor of 600851475143 (6857), happy numbers ≤ 50, Collatz longest chain under 10,000 (starting number and length
   from a real run), Tower of Hanoi moves for n = 3 (print) and 2^n − 1 formula. Mention projecteuler.net as a source of practice.
Exercises 3–5 per lesson, graded (ĆWICZENIE 1 łatwe … trudniejsze), deterministic.

## L — t25_testing + t27_clean_code_pitfalls   · TAG `tq` · scopes `"t25_testing/*"` and `"t27_clean_code_pitfalls/*"`
Folders `E:\java\JavaLearning\src\t25_testing\` and `...\src\t27_clean_code_pitfalls\` (create). Packages `t25_testing`,
`t27_clean_code_pitfalls`. Create `package-info.java` for BOTH + 7 lessons. Learner knows t01–t24 (all Java SE basics, OOP,
exceptions, generics, collections, lambdas, Optional, streams, java.time, IO, annotations, concurrency, design patterns).
RULES: example classes are static nested classes/records/interfaces inside the lesson; lessons must not use classes from other
lessons; NO JUnit/Mockito here (real frameworks come in t32_junit_mockito — say so). Deterministic output only (Clock.fixed,
new Random(42), no printing of HashSet/HashMap contents, no timing numbers in WYNIK).
t25_testing (testing concepts WITHOUT a framework):
1. Testing01Concepts — why tests (safety net for changes, documentation); test pyramid (unit / integration / end-to-end);
   Arrange-Act-Assert and given/when/then; FIRST (Fast, Independent, Repeatable, Self-validating, Timely); build a TINY test
   runner inside the lesson: a record TestCase(String name, Runnable body), own assertEquals/assertThrows helpers throwing
   AssertionError, runner counting passed/failed and printing ✔/✘ per test (some tests deliberately failing to show the output);
   edge cases (empty, null, boundaries, negative, huge); the `assert` keyword is disabled by default (needs -ea) — pitfall.
2. Testing02TestDoubles — why dependencies make tests hard (DB, network, time, randomness); hand-written doubles for an
   OrderService: dummy (unused argument), stub (PaymentGateway always approving/declining), fake (in-memory repository),
   spy (Notifier recording calls), mock (expectations verified at the end); time via Clock.fixed, randomness via injected
   Random(42)/Supplier; tests run by the mini runner (copy a compact version of it into this lesson); pointer to Mockito (t32).
3. Testing03TestableDesign — PRZED: class calling LocalDate.now(), creating its own dependencies with `new`, static singleton,
   printing instead of returning → impossible to test; PO: constructor injection, Clock parameter, pure calculation separated
   from IO ("functional core, imperative shell"), return values instead of println; test the PO version with the mini runner;
   checklist of testability smells.
t27_clean_code_pitfalls:
4. Pitfalls01Classic — about 22 classic Java pitfalls, each a tiny numbered sub-demo with the surprising result shown by show()
   or expectThrows and a one-line "dlaczego": == on Strings; Integer cache 127 vs 128; 0.1 + 0.2; integer division 7/2;
   int overflow; char + int ('a' + 1); switch fall-through without break; ternary autounboxing NPE
   (Integer x = flag ? null : 0 style); List<Integer>.remove(int); Arrays.asList(...).add; ConcurrentModificationException;
   HashSet with class lacking equals/hashCode; mutable HashMap key; new BigDecimal("2.0").equals(new BigDecimal("2.00"));
   BigDecimal from double (new BigDecimal(0.1)); Math.abs(Integer.MIN_VALUE); -7 % 3; ignored result of s.toUpperCase();
   Optional.get on empty; reusing a stream; "TITLE".toLowerCase(new Locale("tr")) → dotless ı (use Locale.forLanguageTag("tr"));
   substring/indexOf off-by-one. Keep each demo 3–8 lines.
5. Pitfalls02CodeReview — typical code-review findings, each PRZED (bad) / PO (good) + why, both versions runnable and giving the
   same result where applicable: magic numbers → named constants; deep nesting → guard clauses; boolean flag parameters →
   two methods/enum; returning null collections → empty; swallowed exceptions; poor names; duplicated code → extracted
   method; comments that lie; public mutable fields; long parameter lists → parameter object/record; Law of Demeter
   (a.getB().getC().doIt()); resource not closed → try-with-resources; string concatenation in a loop → StringBuilder.
6. CleanCode01Principles — naming, small functions doing one thing, one level of abstraction per function, DRY / KISS / YAGNI,
   command–query separation, fail fast, prefer immutability, self-documenting code vs comments that explain WHY, consistent
   formatting; each principle with a tiny before/after example.
7. CleanCode02Solid — SOLID revisited on ONE realistic example (invoice/billing module) refactored step by step: SRP split,
   OCP via tax/discount strategy interfaces, LSP check of a subtype, ISP split of a fat interface, DIP with constructor
   injection; say how SOLID supports testing (t25) and Spring DI (t34); reference t07_inheritance_polymorphism/Inherit08Solid
   for the basic version and do NOT repeat its examples.
Exercises 3–5 per lesson, graded; include "PRZEPISZ ..." exercises where natural (they fit t27 very well).

## M — t26_jvm (Opus)   · TAG `jvm` · scope `"t26_jvm/*"`
Folder `E:\java\JavaLearning\src\t26_jvm\` (create). Package `t26_jvm`. Create `package-info.java` + 4 lessons. Learner knows
t01–t25. Accuracy is critical: only state facts true for HotSpot / Java 17; when behavior is JVM- or run-dependent, say so.
DETERMINISM: memory numbers, GC timing, PIDs, uptime, identity hashes are run-dependent → never put their values in WYNIK;
print derived booleans/facts (e.g. maxMemory() > 0 → true) or mark with "// (wynik zależy od uruchomienia)" as the kit says.
Never trigger a real OutOfMemoryError. StackOverflowError may be caught, but print only that it happened (depth varies).
1. Jvm01Memory — stack vs heap (frames, locals, references, objects), primitives inside objects vs local primitives, string
   pool and intern() (deterministic ==/equals demos), Runtime max/total/free memory (facts only), StackOverflowError caught
   in a recursion demo, memory leak patterns (static collection growing, listeners never removed, caches without limit) with a
   small deterministic demo (sizes), -Xss/-Xmx/-Xms in comments.
2. Jvm02ClassLoadingInit — lifecycle: loading, linking (verification, preparation, resolution), initialization; static
   initializer order printed as a trace (static fields and static blocks in textual order, superclass first); class is
   initialized lazily on first active use; reading a compile-time constant (static final int = 5) does NOT initialize the class
   (inlined), while a non-constant static final does — show with traces; Class.forName(name) initializes, X.class does not;
   initialization-on-demand holder idiom; class loaders: String.class.getClassLoader() == null (bootstrap), platform, app
   loader names via getClassLoader().getName().
3. Jvm03GarbageCollection — reachability and GC roots, unreachable ≠ immediately collected, generational hypothesis (young/old),
   G1 as default collector since Java 9, stop-the-world pauses, System.gc() is only a hint; finalize() deprecated → Cleaner;
   WeakReference/SoftReference/WeakHashMap concepts (demos must not depend on GC actually running — print only guaranteed
   facts, e.g. get() before clearing references); common leak causes; GC flags in comments (-XX:+UseG1GC, -Xlog:gc).
4. Jvm04ToolsProfiling — JDK tools: jps, jcmd (VM.flags, GC.heap_info, Thread.print), jstack, jmap/heap dump, jconsole, VisualVM,
   Java Flight Recorder (-XX:StartFlightRecording, jfr print), JIT compilation basics (interpreter → C1 → C2, -XX:+PrintCompilation),
   what to look at when an app is slow or leaks; from code: ManagementFactory.getRuntimeMXBean() (vm name contains "VM" → true),
   Runtime.version().feature() ≥ 17 → true, ProcessHandle.current().pid() > 0 → true, thread count > 0 → true; a short
   "hot loop" program section explaining how to observe it with the tools (runs in < 1 s).
Exercises 3–4 per lesson, graded, deterministic (e.g. predict init order as a string, count reachable objects in a small graph,
parse a fake GC log line, compute object pool behavior).

## J — t12_collections PART 1 (lessons 01–07)   · TAG `col1` · scope `"t12_collections/Collections0[1-7]*"`
Folder `E:\java\JavaLearning\src\t12_collections\` (create it). Package `t12_collections`; imports `helpers.Check`,
`static helpers.Console.*`, `helpers.SampleData` / `helpers.model.*` where natural. Create `package-info.java` (reading order for
ALL 13 lessons: Collections01Overview … Collections13Performance) + lessons 01–07.
Context: learner knows t01–t11 (incl. generics, records, enums, exceptions, equals/hashCode). Lambdas/streams: only simple lambdas
(pointer t13/t16). RULES: example classes = static nested classes / records inside the lesson; lessons must not use classes from
other lessons. DETERMINISM: never print HashSet/HashMap (or Set.of/Map.of) contents directly — print size/contains, or copy into
TreeSet/TreeMap/sorted List first; PriorityQueue iteration order is NOT priority order (print by polling).
1. Collections01Overview — hierarchy Iterable → Collection → List/Set/Queue(Deque); Map separate; interface vs implementation table
   (ArrayList, LinkedList, HashSet, LinkedHashSet, TreeSet, ArrayDeque, PriorityQueue, HashMap, LinkedHashMap, TreeMap) with
   ordering/duplicates/null rules; "program to interfaces" (List<String> x = new ArrayList<>()); decision guide "which collection";
   List.of/Set.of/Map.of intro (immutable; Set.of/Map.of order unspecified → never print them).
2. Collections02Lists — ArrayList: add, add(index), get, set, remove(index) vs remove(Object) (List<Integer> pitfall), contains,
   indexOf, subList is a VIEW (changes visible both ways), sort, replaceAll, removeIf; Arrays.asList fixed-size pitfall;
   ArrayList vs LinkedList (LinkedList rarely the right choice — why).
3. Collections03IterationModification — for-each, Iterator (hasNext/next/remove), ListIterator (set/add, backwards);
   ConcurrentModificationException when removing in for-each (expectThrows); fixes: iterator.remove, removeIf, iterate a copy;
   index-loop removal skipping pitfall (show the wrong result) → iterate backwards; forEach(lambda) preview.
4. Collections04Sets — HashSet basics (no duplicates; order unspecified), LinkedHashSet (insertion order), TreeSet (sorted,
   NavigableSet: first, last, floor, ceiling, headSet, tailSet); set algebra union/intersection/difference (addAll/retainAll/
   removeAll on copies, printed via TreeSet); elements need correct equals/hashCode (record vs class without them — size demo).
5. Collections05Maps — HashMap put/get/getOrDefault/containsKey/remove; iteration over entrySet (print via TreeMap or LinkedHashMap);
   putIfAbsent, computeIfAbsent (map of lists), merge (word counting), compute; TreeMap navigation (firstKey, floorKey, headMap,
   tailMap); LinkedHashMap access order + removeEldestEntry = tiny LRU cache; pitfall: get returns null for missing key AND for
   key mapped to null (containsKey / getOrDefault).
6. Collections06QueuesDeques — Queue offer/poll/peek (return null/false) vs add/remove/element (throw); ArrayDeque as queue (FIFO)
   and as stack (push/pop, LIFO) — prefer it over Stack/LinkedList; PriorityQueue with natural order and with Comparator
   (tasks by priority), printed by polling; pitfall: iterating a PriorityQueue does not give sorted order; undo history example.
7. Collections07ComparableComparator — Comparable (natural order, compareTo contract, consistent with equals); Comparator.comparing,
   comparingInt, thenComparing, reversed, nullsFirst/nullsLast; sorting SampleData employees by department then salary desc;
   Polish words sorting: default Unicode order vs Collator.getInstance(Locale.forLanguageTag("pl-PL")) (ą, ł, ś, ż) — PUŁAPKA;
   pitfall: comparator by subtraction (a - b) overflows — show with Integer.MIN_VALUE; use Integer.compare.
Exercises 3–5 per lesson, graded; include one "PRZEPISZ ..." where natural.

## K — t12_collections PART 2 (lessons 08–13)   · TAG `col2` · scope `"t12_collections/Collections{08,09,10,11,12,13}*"`
Same folder/package/rules as section J. Another agent writes lessons 01–07 AND package-info.java IN PARALLEL in the same folder:
never create, read, modify or delete package-info.java or Collections01–07 files. Write lessons 08–13 only:
8. Collections08ImmutableUnmodifiable — List.of (immutable, rejects null — NPE), Collections.unmodifiableList (read-only VIEW:
   changes to the original are visible!), List.copyOf (independent copy), Arrays.asList (fixed size: set OK, add → exception);
   defensive copies in a class (getter returning internal list); table: which to use when.
9. Collections09CollectionsUtility — Collections.sort, reverse, shuffle(list, new Random(42)), max/min (with Comparator),
   frequency, nCopies, swap, rotate, binarySearch (list must be sorted — show wrong result on unsorted), disjoint, addAll,
   emptyList/singletonList; Arrays vs Collections.
10. Collections10Patterns — recipes with SampleData where natural: counting (merge), grouping Map<K, List<V>> (computeIfAbsent),
    inverting a map, deduplicate preserving order (LinkedHashSet), top-N with PriorityQueue, two-sum with HashMap, sliding window
    maximum with ArrayDeque, first non-repeating character (LinkedHashMap). Each recipe: loop version, deterministic output.
11. Collections11HashingInternals — how HashMap works: hashCode → spread (h ^ h >>> 16) → bucket index (n - 1) & hash (compute
    and print for a few keys), collisions (chain; treeify after 8 in one bucket — mention), load factor 0.75 and resize,
    "Aa"/"BB" same hashCode, constant hashCode = all in one bucket (show via a key class counting equals() calls, not timing),
    mutable key lost after change, String.hashCode formula; HashSet is built on HashMap.
12. Collections12CustomIterable — implement Iterable<Integer> (IntRange from..to with step) and Iterable<Long> (Fibonacci up to
    a limit); Iterator contract (hasNext, next throws NoSuchElementException at the end — expectThrows), use in for-each,
    anonymous/inner iterator class vs separate class, iterator with remove → UnsupportedOperationException by default.
13. Collections13Performance — Big-O table (get/add/contains/remove for ArrayList, LinkedList, HashSet, TreeSet, HashMap, TreeMap,
    ArrayDeque); demonstrate with COUNTED operations, never wall-clock time (e.g. count equals() calls for list.contains vs
    set.contains; count element shifts for remove(0) on ArrayList); pitfalls: contains on List inside a loop (O(n²)) → HashSet;
    LinkedList.get(i) in a loop; removing from the front of ArrayList in a loop → ArrayDeque; initial capacity; mention JMH for
    real benchmarks.
Exercises 3–5 per lesson, graded; include one "PRZEPISZ ..." where natural.

## I — t07_inheritance_polymorphism   · TAG `inh` · scope `"t07_inheritance_polymorphism/*"`
Folder: `E:\java\JavaLearning\src\t07_inheritance_polymorphism\` (create it). Package `t07_inheritance_polymorphism`;
imports `helpers.Check`, `static helpers.Console.*` (+ `helpers.model.*` only if useful). Create `package-info.java` + 8 lessons.
Context: learner knows t01–t06 (basics, arrays, strings, methods, classes/constructors/encapsulation/static/equals-hashCode/
immutability/nested classes/packages/value objects), t08 enums, t09 records, t10 exceptions, t11 generics. Collections: only
List/ArrayList/Map basics (pointer to t12). Lambdas: only as a preview with pointer to t13.
RULES specific to this package:
- Example classes are `static` nested classes inside the lesson class (say once why). Lessons must NOT use classes from
  other lessons (the verifier compiles each scope separately).
- Never print default Object.toString (hash) or HashSet/HashMap contents in WYNIK; print sizes/counts or override toString.
- `-Xlint:all` is on: never call a static method through an instance (lint [static]) — show static "hiding" by calling via
  class names/declared types and explain in comments; equals without hashCode needs @SuppressWarnings("overrides") + comment.
- Java 17: pattern-matching `switch` over types is NOT available (preview) — show it only in comments as "Java 21+";
  use `instanceof` with pattern (Java 16+) in code.
1. Inherit01Basics — extends and "is-a"; what is inherited (public/protected members) and what not (private, constructors);
   super(...) must be first, implicit super(); constructor chain trace printed (base → derived); Object as the root
   (getClass, equals, toString inherited); single inheritance only (compile error in comment); final class (String).
2. Inherit02Override — @Override and why (typo demo in comment); rules: same signature, covariant return, access not narrower,
   checked exceptions not broader; super.method(); overriding vs overloading pitfall (equals(Foo) overload); final methods;
   static methods are hidden not overridden (call via declared types); fields are not polymorphic (field hiding pitfall);
   calling an overridable method from a constructor → subclass field not yet initialized (prints null/0) — classic pitfall.
3. Inherit03AbstractClasses — abstract class/method, cannot instantiate (compile error comment), constructor in abstract class,
   partial implementation shared by subclasses (Shape with abstract area() and concrete describe()), template-method preview
   (pointer t22_design_patterns/Patterns05TemplateMethod), abstract class vs interface (table in ŚCIĄGA).
4. Inherit04Interfaces — interface as a contract; a class implements several; constants; default methods (Java 8), static
   methods, private methods (Java 9); diamond conflict of two defaults → must override and choose X.super.m(); Comparable
   as a JDK example (sort a List of own objects); functional interface link (pointer t13); marker interface mention.
5. Inherit05Polymorphism — declared type vs runtime type; dynamic dispatch; List<Shape> loop; upcasting (implicit),
   downcasting with instanceof pattern; ClassCastException demo via expectThrows; PRZED/PO: if-instanceof chain replaced by
   an overridden method; QUIZ: overload chosen at compile time by declared type vs override at runtime (show output).
6. Inherit06CompositionVsInheritance — "has-a" vs "is-a"; fragile base class: CountingList extends ArrayList overriding add and
   addAll → addAll counts twice (print the count, not the list); fix by composition + delegation (forwarding); JDK mistake
   mention (Stack extends Vector); Liskov violation Square extends Rectangle (setWidth breaks area expectation); rule
   "favor composition over inheritance" with when inheritance IS fine.
7. Inherit07SealedClasses — sealed / permits (Java 17), subclasses must be final, sealed or non-sealed; sealed interface with
   records (Payment: Card, Blik, BankTransfer) and a fee calculation via instanceof patterns; compile error comments for an
   unlisted subclass; Java 21 exhaustive switch shown in a comment; when to use sealed (closed domain variants).
8. Inherit08Solid — SOLID with small PRZED/PO examples each: SRP (report class that computes + formats + saves → split),
   OCP (discounts via interface instead of switch), LSP (Rectangle/Square or read-only list throwing), ISP (fat
   MultiFunctionDevice → Printer/Scanner), DIP (service depends on a Repository interface, in-memory implementation injected
   through the constructor). Keep each example tiny; outputs deterministic.
Exercises 3–5 per lesson, graded; include one "PRZEPISZ ..." where natural (e.g. instanceof chain → polymorphism,
inheritance → composition).

## H2 — t06_oop_basics CONTINUATION   · TAG `oop2` · scope `"t06_oop_basics/Oop0[5-9]*"`
The folder `E:\java\JavaLearning\src\t06_oop_basics\` ALREADY contains `package-info.java` and finished, verified lessons
Oop01ClassesObjects, Oop02Constructors, Oop03Encapsulation, Oop04Static — DO NOT modify or re-read them (except: you may read
Oop04Static's first ~60 lines ONCE if you want to match style). Write ONLY lessons 5–9 using the outlines of section H below
(items 5–9 and the H intro rules: static nested example classes, never print default Object.toString hash in WYNIK).
Package declaration: `package t06_oop_basics;`; imports `helpers.Check`, `static helpers.Console.*`.
Notes from the previous agent (important):
- `-Xlint:all` warns "overrides equals but not hashCode". For the Oop05 demo class with equals but without hashCode put
  `@SuppressWarnings("overrides")` on that class and explain in a comment why (deliberately broken example).
- Reading a static field through an object reference triggers a `[static]` lint warning — only mention it in comments.
- Helpful NPE messages include variable names only because the verifier compiles with `-g` (IntelliJ does too); throwing inside
  a small helper method gives a predictable variable name.
- The verifier rejects a tag followed by anything other than a colon, e.g. `PUŁAPKA (…):` is wrong.
- Oop08PackagesAccess: the course's own packages are top-level (`helpers`, `t01_basics` … directly in `src/`); say that is fine for
  a learning project, while real projects use a reverse-domain prefix (com.company.app...). Verify each lesson with
  `--scope "t06_oop_basics/Oop05*"` etc. before moving on; final run with the H2 scope.

## H — t06_oop_basics   · TAG `oop` · scope `"t06_oop_basics/*"`
Create `package-info.java` + 9 lessons. Context: learner knows t01–t05 (basics, control flow, arrays, strings, methods incl.
overloading/recursion). Collections are NOT known yet (List.of/ArrayList only when unavoidable, with a pointer to t12).
IMPORTANT: every lesson is ONE file — the example classes (Product, BankAccount, Point...) are `static` nested classes inside
the lesson class (explain once why: a real project would put each class in its own file). Default Object.toString prints a
hash code (e.g. Dog@1b6d3586) — NON-deterministic: never put it in WYNIK (print only getClass().getSimpleName() or mark it).
1. Oop01ClassesObjects — class = blueprint, object = instance; fields, instance methods, `new`; reference variables and
   independent state of two objects; alias vs separate object; null reference + NPE; object diagram (stack/heap) in comments;
   calling methods on objects; `this` briefly.
2. Oop02Constructors — default constructor; parameterized; the no-arg one disappears when you declare another (compile error in
   comment); overloading constructors; `this(...)` chaining (must be first statement); `this.name = name` shadowing; validation
   in constructor (IllegalArgumentException); initialization order with printed trace (field initializers → instance init block
   → constructor body); copy constructor.
3. Oop03Encapsulation — private fields + getters/setters; why public fields are dangerous (invariant broken demo: negative
   balance); BankAccount with deposit/withdraw keeping balance ≥ 0; read-only property (getter only); validation in setters;
   access modifiers table (public / protected / package-private / private) in ŚCIĄGA; "tell, don't ask" briefly.
4. Oop04Static — static field shared by all objects (instance counter), static vs instance method (no `this` in static —
   compile error comment), static final constants, static init block with trace, utility class with private constructor,
   static import (Math.max → max), pitfall: static mutable state shared unexpectedly.
5. Oop05ObjectMethods — override toString (with @Override and why), equals contract (reflexive, symmetric, transitive,
   consistent, x.equals(null) == false) implemented step by step, hashCode contract (equal objects → equal hash codes),
   Objects.equals / Objects.hash, getClass vs instanceof in equals (symmetry issue with subclasses — short), HashSet pitfall:
   equals without hashCode → "duplicates" in a set (demo, HashSet preview with pointer t12), mutable field used in hashCode
   → object "lost" in the set (demo).
6. Oop06Immutability — final fields, no setters, final class, defensive copies of arrays/lists in constructor AND getter
   (demo of the leak without them), "with" methods returning a new object (withPrice), benefits (safe sharing, map keys, threads),
   JDK examples (String, LocalDate, BigDecimal — all return new objects), final ≠ immutable (final reference to a mutable array).
7. Oop07NestedClasses — static nested class (independent), inner class (has access to outer instance fields; Outer.this;
   needs an outer object to create), local class (inside a method), anonymous class (implementing an interface/Comparator on
   the spot), when to use which (table), pitfall: inner class keeps a hidden reference to the outer object (prefer static nested).
8. Oop08PackagesAccess — packages as folders and namespaces, import vs fully qualified names, import static, name clashes
   (java.util.List vs java.awt.List — show a fully qualified name in code), package-private visibility (explain with the course's
   own packages: helpers vs lessons), protected preview (t07), default package warning, naming conventions (reverse domain).
9. Oop09ValueObjects — value object vs entity (identity by id vs equality by value); "primitive obsession" (double price, String
   email everywhere); build small value objects: Email (validated, normalized lowercase), Temperature (factory methods
   ofCelsius/ofFahrenheit, conversions), Range (from ≤ to invariant); equals/hashCode by value, static factories `of`, private
   constructor; comparison with records (pointer t09_records — records make this shorter).
Exercises 3–5 per lesson, graded; include one "PRZEPISZ ..." (e.g. public fields → encapsulated class) where natural.

## G — t02_controlflow + t03_arrays   · TAG `ctrlarr` · scopes `"t02_controlflow/*"` and `"t03_arrays/*"`
Create `package-info.java` for BOTH packages (see kit section 2) and 10 lessons. Context: the learner knows ONLY t01_basics
(program structure, primitive types, variables/scope/final/var, operators incl. && || and ternary, casting/overflow, wrappers,
Math/Random, floating point, references/pass-by-value, Scanner, printf). NO collections/streams/lambdas/OOP yet — use arrays,
Strings and simple static methods; ArrayList only when unavoidable with a pointer to t12_collections. SampleData may be used
sparingly (lists → convert or iterate with for-each, which is taught in Control03).
t02_controlflow:
1. Control01IfElse — if / else if / else; ALWAYS braces (dangling-else pitfall shown in a comment); conditions on Strings with
   equals (not ==); combining conditions vs nesting; guard clauses / early return (PRZED/PO nested pyramid); boolean variables
   named isX/hasX, `if (flag == true)` smell; ternary recap; order of else-if (most specific first — grading example).
2. Control02Switch — classic switch with break; fall-through: intended (grouping cases) vs the forgotten-break bug (demo);
   switch on int/char/String/enum (small nested enum); default; switch EXPRESSION (Java 14+) with ->, multiple labels
   `case SAT, SUN ->`, yield in block, returning a value, exhaustiveness for enums (no default needed; adding a constant breaks
   compile — comment); String switch is case-sensitive; null in switch → NPE (demo); pattern matching in switch = Java 21 (comment).
3. Control03Loops — for (anatomy: init; condition; update), while, do-while (runs at least once — menu example with a scripted
   Scanner input), for-each over arrays and SampleData lists; which loop when (table in ŚCIĄGA); off-by-one (< vs <=) demo;
   loop variable scope; infinite loop `while (true)` + break; counting down; step by 2.
4. Control04BreakContinueLabels — break, continue (skip), labeled break/continue for nested loops (find a value in a 2D array —
   arrays are known only by name from Basics09: keep it simple int[][] literal, full arrays in t03), alternative: extract method +
   return (DOBRA PRAKTYKA), flags vs labels, PUŁAPKA: continue in while skipping the increment → infinite loop (explain, don't run).
5. Control05LoopPatterns — accumulators (sum/count/average), min/max with index, first match / any match with break, counting
   occurrences in a String (charAt), building a String in a loop (+= vs StringBuilder preview), nested loops (multiplication
   table with printf alignment, triangle of stars), FizzBuzz, digit sum / reverse number (% 10, / 10), simulated input-validation
   loop with Scanner over a scripted String, a tiny text menu driven by scripted commands.
t03_arrays:
1. Arrays01Basics — declaration forms, new int[5], initializer {1,2,3}, length (field, not method), default values by type
   (0, 0.0, false, null), indexes from 0, ArrayIndexOutOfBoundsException demo, iterating (for with index / for-each), printing
   with Arrays.toString (and why println(array) prints [I@...), arrays are objects (alias vs copy — pointer to
   t01_basics/Basics09PassByValue), array of Strings with nulls → NPE pitfall, fixed size (cannot grow — ArrayList preview).
2. Arrays02MultiDim — int[3][4], initializer, rows = arr.length, cols = arr[i].length, nested loops, jagged arrays,
   Arrays.deepToString (vs toString on 2D), row/column sums, transpose, char[][] board (tic-tac-toe) with winner check,
   pitfall: new int[3][] leaves rows null (NPE demo).
3. Arrays03Utility — java.util.Arrays: toString, sort (primitives; Strings natural order + Unicode note), binarySearch (ONLY on
   sorted arrays — wrong result demo on unsorted), fill, copyOf (grow/shrink), copyOfRange, equals vs == vs deepEquals,
   asList (fixed-size: add → UnsupportedOperationException; set writes through to the array — demo), System.arraycopy,
   clone() and the shallow-copy pitfall for 2D arrays, Arrays.stream preview (pointer t16_streams/Streams02Creation).
4. Arrays04Algorithms — handwritten: sum/avg/min/max, reverse in place (two pointers), linear search, binary search (manual,
   with step trace), bubble sort (with pass trace), counting frequencies with an int[] counter (dice rolls with seeded Random),
   rotate by one, remove duplicates from a sorted array (two pointers, return new length), check if sorted, merge two sorted
   arrays. Mention O(n) / O(log n) / O(n²) intuitively (pointer t24_algorithms/Algorithms01Complexity).
5. Arrays05Varargs — `int... numbers` syntax, calling with 0/1/many args or an existing array, it IS an array inside, must be the
   last parameter, only one varargs, overloading ambiguity pitfall (method(int...) vs method(int, int...)), passing null pitfall,
   varargs in the JDK (printf, Arrays.asList, List.of, String.format), when NOT to use varargs.
Exercises 3–5 per lesson, graded, one PRZEPISZ... where natural.

## B — t14_optional (finish) + t15_numbers   · TAG `optnum`
Files: `t14_optional\Optional01Basics.java` and `t14_optional\package-info.java` EXIST, are COMPLETE and VERIFIED — do NOT
rewrite them (Optional01 references "Optional02Transform, sekcja 9" for comparing Optionals with equals — make sure Optional02
section 9 covers equals). Create `Optional02Transform`, `Optional03BestPractices`,
`t15_numbers\package-info.java`, `Numbers01BigDecimal` … `Numbers05IntegerTricks`.
Scopes: `"t14_optional\*"`, `"t15_numbers\*"` (verify per lesson with narrower patterns).
Context: learner knows basics, OOP, exceptions, generics, collections, lambdas (t13). Streams come AFTER (t16) — avoid pipelines
except tiny previews pointing to t16_streams. Use Customer.findEmail(), Student.averageGrade(), Product.price().
1. Optional01Basics — null problem (NPE demo), Optional as a box 0/1; of (NPE on null, demo) / ofNullable / empty; isPresent,
   isEmpty (11+); get() pitfall (NoSuchElementException demo); orElse vs orElseGet LAZINESS demo (side-effect counter);
   orElseThrow() (10+) and orElseThrow(supplier); ifPresent, ifPresentOrElse (9+); OptionalInt/OptionalDouble with
   Student.averageGrade (Henryk empty).
2. Optional02Transform — map (map returning null → empty), flatMap (why Optional<Optional<T>> appears; customer → findEmail →
   domain), filter, or (9+), stream() (9+) preview, chains (customerById → email → domain → uppercase), wrapping Map.get with
   ofNullable, Optional.equals, toString forms `Optional[x]` / `Optional.empty`.
3. Optional03BestPractices — only as RETURN type; not fields/params/collections; never return null instead of Optional; empty
   collection instead of Optional<List>; isPresent()+get() → map/orElse (PRZED/PO); of vs ofNullable; orElse(null) smell; small
   in-memory repository with findById (nested in lesson); allocation note; checklist in ŚCIĄGA; ZNAJDŹ BŁĄD questions.
4. Numbers01BigDecimal — why not double (0.1+0.2, 1.10−1.00); new BigDecimal("0.1") vs new BigDecimal(0.1) vs valueOf(0.1);
   add/subtract/multiply/divide (immutability — unassigned result demo); divide 1/3 ArithmeticException (demo) + fix with scale &
   RoundingMode or MathContext; setScale; RoundingMode table HALF_UP/HALF_EVEN/HALF_DOWN/UP/DOWN/CEILING/FLOOR on 2.5, 3.5,
   -2.5, 2.45; scale vs precision; equals vs compareTo (2.0 vs 2.00) → HashSet vs TreeSet demo; stripTrailingZeros +
   toPlainString (1E+1 pitfall); signum/negate/abs/max/min; ZERO/ONE/TEN.
5. Numbers02MoneyValueObject — nested `record Money(BigDecimal amount, Currency currency)` designed from scratch: compact
   constructor normalizing scale 2 (explain HALF_UP vs HALF_EVEN choice) + validation; of(String, String), zero(...);
   add/subtract with currency-mismatch exception (demo); times(int); percent/discount; VAT gross↔net with rounding issue;
   isGreaterThan; equals consistency thanks to normalization; split into N installments without losing grosze (remainder to
   first installments + sum check); summing a list in a loop (stream preview); display formatting; why value object > raw BigDecimal.
6. Numbers03BigInteger — long factorial overflow at 21!; 25!/50!; pow, mod, modPow, gcd, isProbablePrime, valueOf;
   longValueExact (ArithmeticException demo); compareTo; bitLength.
7. Numbers04FormattingParsing — String.format/formatted with explicit Locale (ROOT vs pl-PL), %,.2f, padding;
   NumberFormat currency/percent/number for pl-PL (NBSP U+00A0/U+202F → replace, PUŁAPKA); DecimalFormat "#,##0.00", "0.00",
   "#.##" with DecimalFormatSymbols pl-PL; parsing: Integer.parseInt, Double.parseDouble("1,5") failure (demo),
   NumberFormat.parse Polish input, "1 234,56" → BigDecimal safely; NumberFormatException handling.
8. Numbers05IntegerTricks — overflow MAX_VALUE+1; int*int overflow before assignment to long (demo); Math.addExact/
   multiplyExact/toIntExact; Math.abs(Integer.MIN_VALUE); rounding-up division (a+b-1)/b (Math.ceilDiv is Java 18+ → comment);
   / and % vs floorDiv/floorMod with negatives; binary/hex/octal literals, toBinaryString/toHexString/parseInt(s, radix);
   bit tricks (n & 1, n & (n-1), shifts); comparator by subtraction pitfall → Integer.compare; money in grosze as long; underscores.
Exercises: 3–5 per lesson; for BigDecimal use Check.equal (by value) or Check.equalExact when scale matters (say so in hint).

## C — t16_streams Streams04–07   · TAG `streamsC` · scope `"t16_streams\Streams0[4-7]*"`
Context: learner knows collections, lambdas, Optional, BigDecimal and Streams01–03 (intro, creation, filter/map/peek/mapToInt).
1. Streams04FlatMap — List<List<Integer>> problem (map gives Stream<List>/Stream<Stream>); flatMap = „przekształć i spłaszcz”
   (1 → 0..n); orders → lines, total quantity; sentences → words → distinct lowercase; employees → skills distinct sorted;
   flatMap(Optional::stream) with Customer::findEmail (9+); flatMapToInt (words → chars); mapMulti (16+) short; cartesian
   product (sizes × colours); pitfall: null collection → NPE and fix; map vs flatMap table in ŚCIĄGA.
2. Streams05SortDistinctLimit — sorted() natural + Unicode vs Collator pl-PL demo (Ł/Ś, capitals); sorted(Comparator):
   comparing, comparingInt, thenComparing, reversed (and the whole-chain reversed mistake), nullsFirst; sorting by BigDecimal
   price; distinct (equals, keeps first); distinct by key (toMap/TreeSet/filter(seen::add) + side-effect warning); limit, skip,
   page(list, number, size); top-N; takeWhile/dropWhile (9+) vs filter — only sensible on ordered data.
3. Streams06TerminalOps — table of terminal ops; forEach vs forEachOrdered; count (long); min/max with Comparator → Optional
   (empty); findFirst/findAny; anyMatch/allMatch/noneMatch incl. empty stream (allMatch true — explain simply); toArray() and
   toArray(String[]::new); toList (16+, unmodifiable, allows null) vs collect(Collectors.toList()) vs
   Collectors.toUnmodifiableList (10+, rejects null) — expectThrows demos; iterator() brief; previews of reduce/collect.
4. Streams07Reduce — folding idea; reduce(identity, acc) for sum/product/max/join; reduce without identity → Optional (empty);
   identity must be neutral (reduce(10, Integer::sum) +10, worse in parallel); associativity (subtraction pitfall); 3-arg reduce
   with type change (sum of word lengths) + what combiner is for; BigDecimal sum reduce(ZERO, add) incl. order totals; reduce vs
   sum()/max() vs collect; string concat via reduce is O(n²) → joining preview.

## D — t16_streams Streams08–12   · TAG `streamsD` · scope `"t16_streams\Streams0[89]*","t16_streams\Streams1[0-2]*"`
Context: as C plus Streams04–07. groupingBy with enum keys MUST use TreeMap::new or EnumMap.
1. Streams08PrimitiveStreams — why (boxing; explain in words), creation (range, of, chars, mapToInt/Long/Double), sum/average/
   min/max/count, summaryStatistics(), average() OptionalDouble (empty!), boxed/mapToObj, asLongStream/asDoubleStream,
   IntStream.iterate, sum overflow → mapToLong (demo), Stream<Integer> has no sum (compile error in comment).
2. Streams09CollectorsBasic — collect & Collector idea (supplier, accumulator, combiner, finisher — analogy); toList, toSet,
   toCollection(TreeSet::new / ArrayList::new / ArrayDeque::new), toUnmodifiableList/Set (10+); joining ×3 variants; counting
   (Long!), summingInt/Long/Double, averagingInt/Double (empty → 0.0 vs IntStream.average), minBy/maxBy (Optional),
   summarizingInt; dedicated stream ops vs collectors (collectors shine as downstream — preview Streams11).
3. Streams10CollectorsToMap — toMap(k, v); duplicate key IllegalStateException (demo + message); merge fn (first/last/sum/max);
   map supplier LinkedHashMap/TreeMap; Function.identity(); index by sku; null VALUE → NPE (Customer::email demo); frequency via
   toMap(w -> w, w -> 1, Integer::sum) vs groupingBy(counting()); inverting a map; sort entries by value → LinkedHashMap;
   toUnmodifiableMap (10+).
4. Streams11GroupingBy — groupingBy → Map<K, List<V>>; manual computeIfAbsent loop PRZED/PO; map type/order (TreeMap/EnumMap, why
   HashMap with enum keys is random); downstream: counting, summingInt, averagingDouble, mapping(toList/toSet/joining), maxBy/minBy
   (Optional values smell → Streams13), reducing, toSet; multi-level (department → age band); derived keys (first letter,
   YearMonth.from(order.date())); composite key via local record; empty groups not created (LOGISTYKA) + EnumMap prefill;
   orders per customer summed with reducing(BigDecimal.ZERO, Order::total, BigDecimal::add).
5. Streams12PartitioningBy — Map<Boolean, List<V>> always has both keys (vs groupingBy); downstream (counting, mapping,
   averaging); in stock / out; students passed/failed by averageGrade (decide & explain Henryk); expensive/cheap with BigDecimal
   constant; partitioningBy (one pass) vs two filters; partitioning nested in groupingBy.

## E — t16_streams Streams13–16   · TAG `streamsE` · scope `"t16_streams\Streams1[3-6]*"`
Context: as D plus Streams08–12.
1. Streams13AdvancedCollectors — collectingAndThen (unmodifiable; maxBy → unwrap, why get() is safe there); filtering (9+) vs
   filter before groupingBy (keeps empty groups — demo); flatMapping (9+) skills per department; teeing (12+) min&max, count&sum →
   average; reducing with mapper; custom Collector.of with supplier/accumulator/combiner/finisher explained; characteristics brief.
2. Streams14OptionalInStreams — ops returning Optional (findFirst/findAny/min/max/reduce w/o identity); map/filter/orElse/
   orElseThrow instead of get(); flatMap(Optional::stream) (9+) to drop empties (findEmail); OptionalDouble from averageGrade;
   Map<K, Optional<V>> smell from groupingBy(maxBy) → collectingAndThen or toMap(..., BinaryOperator.maxBy(...)); Stream.ofNullable
   (9+); Optional<List> vs empty list; ZNAJDŹ BŁĄD with get() on empty.
3. Streams15BigDecimalMoney — sum reduce(ZERO, add); line totals; average price = sum.divide(valueOf(count), 2, HALF_UP) and the
   exception without scale (demo); per-category sums (EnumMap/TreeMap) via reducing and via toMap(..., BigDecimal::add); revenue per
   customer and per month (YearMonth); max/min by price; rounding per line vs rounding total (demo); compareTo for sums; parse
   salesCsvLines() into totals skipping header/blank/invalid lines with a skipped counter (full CSV in t18_io_files/Io04Csv);
   double-based sum drifting demo.
4. Streams16Laziness — recipe idea; vertical processing for stateless ops and barrier for sorted/distinct with traces;
   short-circuit ops with counters (findFirst, anyMatch, limit, takeWhile); infinite streams + limit/takeWhile; limit early vs
   late; Supplier for expensive defaults; iterate with predicate; consumed-stream recap; peek skipped by count recap; diagram in ŚCIĄGA.

## F — t16_streams Streams18–20   · TAG `streamsF` · scope `"t16_streams/Streams1[89]*" "t16_streams/Streams20*"`
NOTE: Streams17SideEffectsPitfalls ALREADY EXISTS, is complete and verified — do NOT touch it. Write only 18, 19, 20.
Context: as E plus Streams13–16 (whole stream chapter). Parallel/thread output is non-deterministic → no WYNIK there.
1. Streams17SideEffectsPitfalls — catalogue BAD → GOOD (+ expectThrows): external list in forEach vs collect; modifying source →
   ConcurrentModificationException; reuse → IllegalStateException; peek-based logic skipped; toMap duplicates & null values;
   sorted() on non-Comparable → ClassCastException; nulls in sort → NPE, Comparator.nullsFirst; distinct on class without
   equals/hashCode (nested class); boxing math; overlong pipelines → named methods; streams in fields; checked exceptions pointer
   (t13_lambdas/Lambda08Pitfalls); when a loop is better; checklist in ŚCIĄGA.
2. Streams18Parallel — parallelStream/parallel; fork/join & common pool simply; when it helps/hurts (size, CPU vs I/O, splitting
   sources ArrayList/array/range vs LinkedList/iterate, shared state, ordering); forEach vs forEachOrdered, findAny vs findFirst;
   race-condition demo with non-thread-safe list/counter vs collect/sum printing only deterministic facts; non-neutral identity
   differs sequential vs parallel (explain, no WYNIK for varying values); toConcurrentMap/groupingByConcurrent mention; measure
   before parallelizing (JMH comment).
3. Streams19Recipes — 30+ short recipes grouped: Listy i wybieranie, Liczenie i statystyki, Grupowanie, Napisy, Pieniądze, Daty,
   Relacje między danymi. Must include: top N expensive; second highest DISTINCT salary; avg salary per department; most frequent
   word (ties alphabetical); duplicates; total per customer (BigDecimal); best customer; low stock; orders by YearMonth; char
   frequency; List → Map by sku; flatten lines; batches of 3 (IntStream.range); zip two lists; join names ", " + " i " before last;
   missing numbers 1..10; customers without orders (Ewa Lis); departments without employees (LOGISTYKA); students without grades;
   oldest per department; longest word per first letter; words per sentence; palindromes; reverse word order; running total (+ loop
   note); partition by price; distinct skills sorted; hired before date; revenue per status. Exercises: 4–5 extra recipes.
4. Streams20Exercises — 20+ graded exercises for the whole chapter (hint, stub, solution in fold, Check), incl. ≥3 PRZEPISZ PĘTLĘ
   NA STREAM and 2–3 „boss” exercises combining many topics; main mostly exercises + short intro section; all required tags.
