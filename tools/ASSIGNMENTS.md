# Assignments (one agent at a time; each agent reads AGENT_KIT.md + ONLY its own section here)

## U — t23_modern_java (Sonnet, 1 batch)   · TAG `mod` · scope `"t23_modern_java/*"`
Folder `src/t23_modern_java/` (create). Package `t23_modern_java`. package-info.java + 7 lessons. Learner knows t01–t22: MOST
features below were already used in earlier lessons — this section is a REVIEW-AND-DEEPEN tour "what changed in Java 8 → 17 and
why", organized by release, each feature shown PRZED (old style) / PO (new style), with its pitfalls and an exact
"(Java N+)" label, plus cross-references to the lesson where it was taught in depth (registry tools/lessons.txt). Do NOT
re-teach whole topics (streams, Optional, records) — summarize in 1 section and link. Accuracy of version numbers is critical:
use the JEP number and the release where the feature became FINAL (preview releases mentioned as "podgląd w Javie N").
Java 18–21 features: ONLY in comments/text blocks as code samples marked "(Java 21+)" etc. — the verifier compiles with
--release 17, so they must not be in compiled code. Release cadence: every 6 months, LTS 8, 11, 17, 21, 25.
Determinism as usual (no now(), no HashMap printing, Locale explicit).
1. Modern01Java8 — the big shift: lambdas and functional interfaces, method references, default and static methods in
   interfaces (diamond problem resolution: class wins, then more specific interface, else override with X.super.m()), Stream API,
   Optional, java.time, CompletableFuture, String.join, Map.getOrDefault/computeIfAbsent/merge/forEach, Iterable.forEach,
   Collection.removeIf, List.sort, Comparator.comparing chains, Base64, effectively final; PRZED/PO for each (anonymous class →
   lambda, loop → stream, null → Optional, Date → LocalDate); links to t13/t14/t16/t17.
2. Modern02Var — local variable type inference (Java 10, JEP 286): where allowed (locals with initializer, for, for-each,
   try-with-resources, lambda parameters in Java 11 JEP 323 for annotations), where not (fields, parameters, return types, null
   initializer, array initializer {1,2}, lambdas without target type), var + diamond pitfall (new ArrayList<>() → ArrayList<Object>),
   var with int literal vs long, readability guidelines (good: long generic types; bad: var x = service.get()),
   var is not a keyword (reserved type name — `var var = 1;` compiles), non-denotable types (anonymous class members visible via var).
3. Modern03SwitchExpressions — Java 14 (JEP 361): arrow labels, no fall-through, multiple labels, switch as expression with
   yield, exhaustiveness for enums (no default needed in expression; adding an enum constant breaks compilation — benefit), old
   switch statement pitfalls (missing break demo), switch on String/enum/int, null → NullPointerException in Java 17 switch
   (case null only Java 21+), pattern matching for switch "(Java 21+)" in comments; PRZED/PO rewrites.
4. Modern04TextBlocks — Java 15 (JEP 378): syntax, incidental indentation removal (position of closing """), trailing spaces
   stripped (\s escape to keep), \<newline> line continuation, escaping """ inside, line endings always \n regardless of
   source file (portability!), formatted() (Java 15), stripIndent/translateEscapes; uses: JSON, SQL, HTML, tests;
   PUŁAPKA: careful — AGENT_KIT unicode-escape rule; build examples whose output is deterministic and print line by line.
5. Modern05RecordsSealedPatterns — records (Java 16, JEP 395) recap with link to t09, instanceof pattern (Java 16, JEP 394:
   scope rules — flow scoping with && and !(x instanceof T t) return), sealed classes/interfaces (Java 17, JEP 409: permits,
   final/sealed/non-sealed subclasses, same package/module rule, getPermittedSubclasses), modelling a domain as a sealed
   hierarchy of records (payment methods, shapes, expression tree), exhaustive handling in Java 17 via if/instanceof chain with
   a final else throwing, and the Java 21 version (switch patterns + record patterns, JEP 440/441) in comments; algebraic data
   types idea; when sealed helps (closed set) vs hurts (plugins).
6. Modern06ApiAdditions — small API gems by release: Java 9 (List/Set/Map.of + immutability and null rejection, Optional.or/
   ifPresentOrElse/stream, Stream.takeWhile/dropWhile/iterate(3 args)/ofNullable, private interface methods, try-with-resources
   on effectively final variables, InputStream.transferTo, ProcessHandle), Java 10 (List.copyOf, Collectors.toUnmodifiableList,
   Optional.orElseThrow()), Java 11 (String isBlank/strip/lines/repeat, Files.readString/writeString, Predicate.not,
   HttpClient — link t28, Optional.isEmpty, Collection.toArray(IntFunction)), Java 12 (Collectors.teeing, String.indent/transform,
   CompactNumberFormat — print with explicit Locale), Java 14 (helpful NullPointerException messages JEP 358 — copy real message),
   Java 16 (Stream.toList, mapMulti), Java 17 (RandomGenerator JEP 356 with a seed, HexFormat); each with a one-line
   PRZED/PO; table at the end.
7. Modern07WhatsNextJava21 — what Java 18–21 (and 22–25 briefly) bring, ALL as commented code samples: UTF-8 by default (18,
   JEP 400 — link t18_io_files/Io12Charsets), simple web server jwebserver (18), record patterns & pattern matching for switch
   (21), virtual threads (21, JEP 444 — link t21_concurrency/Concurrency04Executors; Executors.newVirtualThreadPerTaskExecutor),
   sequenced collections (21: getFirst/getLast/reversed), String templates (preview then withdrawn — say so), structured
   concurrency/scoped values (preview), unnamed variables `_` (22), Stream gatherers (24), instance main methods / compact
   source files (25), flexible constructor bodies (25); how to read release notes / JEPs, upgrade strategy (LTS to LTS),
   --release flag, what the learner's Java 17 code needs to change (nothing breaks; deprecations: SecurityManager, finalization).
   Runnable part: small Java 17 programs showing "today's way", next to the commented future way.

## T — t22_design_patterns (Sonnet, 3 batches: T1 Patterns01–05 + package-info, T2 Patterns06–10, T3 Patterns11–15)   · TAG `pat1`…`pat3` · scope `"t22_design_patterns/*"`
Folder `src/t22_design_patterns/` (create). Package `t22_design_patterns`. package-info.java (batch T1) + 15 lessons. Learner knows
t01–t21 (OOP, inheritance, interfaces, records, enums, sealed, exceptions, generics, collections, lambdas, Optional, streams,
java.time, IO, reflection and dynamic proxies (t19), Lombok, concurrency basics). Patterns are taught on ONE realistic domain per
lesson (shop, orders, invoices, notifications, documents — SampleData products/orders/customers where it fits), never on
"Animal/Dog" toys.
EVERY LESSON follows the same arc (sections):
  (1) PROBLEM first: code WITHOUT the pattern that hurts (if/switch growing, duplicated code, hard-wired dependency) — show why
      (a new requirement forces edits in many places);
  (2) the pattern step by step: roles (names from the GoF book in English + Polish), a small ASCII diagram in the Javadoc;
  (3) the MODERN Java version (lambdas / functional interfaces / enum / records / sealed + switch pattern only as
      "(Java 21+)" comment) and when the classic class-based version is still better;
  (4) where the learner ALREADY MET it in the JDK (Comparator = strategy, Collections.unmodifiableList = decorator/proxy,
      InputStream wrappers = decorator, Runnable = command, Iterator, Arrays.asList = adapter, StringBuilder = builder,
      List.of / Optional.of = static factories) and in Spring (SpringLearning: beans = singletons, @Autowired = DI,
      JdbcTemplate = template method, ApplicationEvent = observer, HandlerInterceptor/filters = chain, proxies = decorator);
  (5) PUŁAPKA: overuse (pattern where an if would do — YAGNI), typical mistakes of that pattern;
  (6) testability: how the pattern makes a unit test easy (show a tiny fake/stub, deterministic);
  (7) "kiedy używać / kiedy nie" table.
Determinism: no HashMap/HashSet printing, Clock instead of now(), no real threads except Patterns04 (singleton thread-safety:
deterministic facts only, e.g. 8 tasks get the same instance → true). Exercises: 3–4 per lesson, last one = implement/refactor
to the pattern with a Check on behaviour (not on class names). Cross-reference t27_clean_code_pitfalls/CleanCode02Solid (SOLID)
where the pattern embodies a principle (OCP for strategy/decorator, DIP for DI).

BATCH T1:
1. Patterns01Strategy — discount/shipping-cost calculation: switch on type → interface DiscountPolicy with implementations →
   lambdas and Map<String, Strategy>/enum with lambda field; choosing a strategy at runtime (config string), Comparator as
   strategy, strategy + DI preview; PUŁAPKA: strategy with one implementation forever.
2. Patterns02Builder — telescoping constructors problem (Pizza/Order with 8 params; boolean traps new X(true, false, true)),
   JavaBeans setters (half-built objects, mutability), Builder as static nested class with fluent methods, validation in
   build(), required params in the builder constructor, immutable result (record + builder), toBuilder/with-copy, step builder
   (compile-time required order) briefly, Lombok @Builder recap (t20_lombok/Lombok03DataValueBuilder), StringBuilder /
   HttpRequest.newBuilder() / Stream.builder() in the JDK.
3. Patterns03Factory — static factory methods (of, from, valueOf, parse — names; can cache, return subtypes, have names: Effective
   Java item 1), Simple Factory (switch on type in one place), Factory Method (subclass decides — e.g. ReportExporter with
   createFormatter()), Abstract Factory (families: PL vs EN invoice parts) briefly, registry of suppliers Map<String,
   Supplier<T>> (sorted keys printed), factory hiding sealed hierarchy; PUŁAPKA: factory that only calls new.
4. Patterns04Singleton — one instance: eager static final, lazy holder idiom, enum singleton (serialization and reflection safe),
   double-checked locking with volatile (link t21_concurrency/Concurrency10MemoryModel), why singletons are hated (global
   state, hidden dependencies, hard to test — demo test pollution between two "tests"), "singleton by DI container" instead
   (Spring bean scope singleton ≠ GoF singleton — explain), stateless singletons are fine; reflection attack on private
   constructor (setAccessible) vs enum (IllegalArgumentException: Cannot reflectively create enum objects — copy real message).
5. Patterns05TemplateMethod — report generation / CSV-JSON exporters / data import with fixed steps: abstract class with final
   template method and abstract/hook steps, Hollywood principle ("nie dzwoń do nas, my zadzwonimy"), modern alternative:
   passing lambdas (template as a method with Function parameters — like JdbcTemplate + RowMapper), inheritance pitfalls
   (fragile base class), AbstractList in the JDK (implement get/size → you get iterator, contains, indexOf...).
package-info.java: reading order of all 15 lessons with one-line descriptions, and a short "jak czytać wzorce" note.

BATCH T2:
6. Patterns06Observer — order status changes notifying email/SMS/stock listeners: tight coupling problem, Subject + listener
   interface (Consumer<Event>), event records, unsubscribe, order of notification (List → deterministic), exception in one
   listener must not stop others (decide and show), memory leak of forgotten listeners, synchronous vs asynchronous (mention
   t21), java.util.Observer deprecated (since 9) — why, PropertyChangeListener, Spring ApplicationEventPublisher /
   @EventListener, observer vs pub/sub (message broker).
7. Patterns07Decorator — adding behaviour without subclass explosion: price calculation / text pipeline / DataSource with
   logging-caching-retry decorators; same interface, wraps a delegate, order matters (demo: compress-then-encrypt vs reverse
   with deterministic toy transforms), functional decorators (Function.andThen, UnaryOperator chains), java.io streams
   (t18_io_files/Io08BinaryStreams), Collections.unmodifiableList / synchronizedList, decorator vs proxy (t19 dynamic proxy)
   vs inheritance; PUŁAPKA: equals/identity of wrapped objects, too many layers.
8. Patterns08DependencyInjection — hard-wired `new` inside a service (untestable: real clock, real mail sender), constructor
   injection (final fields), setter and field injection (why worse), composition root (wiring in main), interfaces + fakes in
   tests (fake Clock, in-memory repository, recording mail sender), DI vs Service Locator, a tiny hand-written container (Map
   of suppliers, singleton vs prototype scope, resolving constructor dependencies — keep reflection minimal; t34 builds the full
   one), what Spring does (@Component, @Autowired, constructor injection by default — SpringLearning), DIP link to CleanCode02Solid.
9. Patterns09Command — operations as objects: text editor or bank account with undo/redo (Deque history), command interface
   execute/undo, macro (composite command), queue of commands (deferred execution), Runnable / lambdas as commands, command
   records + handler (CQRS mention), logging/auditing commands; PUŁAPKA: undo that cannot restore state (store what's needed).
10. Patterns10Facade — ordering process touching inventory, payment, invoice, shipping, notification: client code knowing all
    subsystems (problem) → OrderFacade.placeOrder(...) hiding steps and order; facade does not forbid direct access; facade vs
    "god class"; service layer in Spring as a facade over repositories (link t27_clean_code_pitfalls and t34); SLF4J as a
    facade over logging libraries (name!); java.nio.file.Files as a facade.

BATCH T3:
11. Patterns11Adapter — incompatible interfaces: old payment API / external weather or currency provider returning different
    types/units (Fahrenheit, cents as long, XML-ish strings) adapted to our interface; object adapter (composition) vs class
    adapter (inheritance) — prefer composition; adapters at system boundaries (ports & adapters/hexagonal mention, t27 link),
    JDK: Arrays.asList, InputStreamReader (bytes → chars), Collections.enumeration/list; two-way adapters briefly.
12. Patterns12Composite — tree of parts: menu/categories or file-system-like or product bundles (bundle price = sum of parts);
    Component interface, Leaf, Composite (children list), uniform treatment (total price, count, print indented tree),
    recursion, sealed interface + records version with switch-free recursion (pattern matching switch only "(Java 21+)"),
    safety vs transparency (add() on a leaf), cycles pitfall, link to t18 directories walk and t24 recursion.
13. Patterns13State — order lifecycle (OrderStatus) with behaviour per state: switch everywhere problem → State interface with
    classes per state, transitions returning next state, illegal transition → exception; enum with abstract methods as state
    machine (link t08_enums/Enums04EnumMapSet), state vs strategy (who changes it), table-driven transitions (EnumMap), sealed
    states carrying data (records); PUŁAPKA: state explosion.
14. Patterns14ChainOfResponsibility — request validation / discount rules / support ticket escalation: handlers in a chain,
    each handles or passes on; building chains (linked handlers vs List<Handler> loop), stop vs continue semantics,
    functional chain (Function/Predicate composition), servlet filters / Spring Security filter chain / HandlerInterceptor,
    logging pipelines; PUŁAPKA: request falls off the end unhandled (default handler), order dependence.
15. Patterns15Visitor — operations over a fixed class hierarchy (document elements or shapes or AST of simple expressions
    1 + 2 * x): adding operations without changing classes (export to text, compute price/area, evaluate), double dispatch
    explained, accept/visit boilerplate, the expression problem (easy to add operations, hard to add types), modern
    alternative: sealed interface + records + instanceof pattern (Java 16+) or switch patterns "(Java 21+)" — compare,
    FileVisitor from t18_io_files/Io07WalkingDirectories as a JDK visitor; final summary table of all 15 patterns
    (problem → pattern → JDK/Spring example).


## S — t21_concurrency (Opus, 2 batches: S1 Concurrency01–05 + package-info, S2 Concurrency06–10)   · TAG `conc1`/`conc2` · scope `"t21_concurrency/*"`
Folder `src/t21_concurrency/` (create). Package `t21_concurrency`. package-info.java (batch S1) + 10 lessons. Learner knows t01–t20
(OOP, records, enums, exceptions, generics, collections, lambdas, Optional, streams incl. parallel streams basics, java.time, IO,
reflection, Lombok). Accuracy is critical: state only what the Java Memory Model and the java.util.concurrent Javadoc GUARANTEE;
when something "usually" happens on HotSpot/x86 but is not guaranteed, say exactly that.
DETERMINISM = the hardest rule here (the verifier compares output and its ORDER; timeout 30 s per lesson; any stderr = failure):
- Never print from several threads in a way whose order matters. Pattern: threads write results into a thread-safe structure
  (or return them via Future), main thread joins/awaits, then prints SORTED or in submission order (Future list order).
- Thread names: give your own names (new Thread(r, "pracownik-1"), own ThreadFactory) — default names like pool-3-thread-2 and
  thread ids depend on what ran before. Never print Thread ids, hash codes, timings, nanoTime differences, CPU counts
  (availableProcessors() > 0 → true is fine).
- Race-condition demos: the lost-update count varies and may even be 0 on some runs → print only guaranteed facts
  ("wynik ≤ 200000 → true", "AtomicInteger daje dokładnie → 200000"), or the deterministic "with the fix" result, and
  describe the typical bad number in a comment marked "(wynik zależy od uruchomienia)". To SHOW an interleaving
  deterministically, force it with CountDownLatch/CyclicBarrier/Phaser in a step-by-step scenario.
- Deadlock: never leave a real deadlock hanging. Demonstrate with tryLock(timeout) detecting it, or a real deadlock of two
  DAEMON threads detected via ThreadMXBean.findDeadlockedThreads() and then abandoned (main ends normally) — names sorted.
- Sleeps: helpers.Sleep.ms only to make a scenario readable, total sleeping per lesson < 3 s; correctness must never rely on
  sleep (use latches/join). Timeouts in demos: generous (≥ 2 s) so slow CI machines pass; use orTimeout/get(timeout) with
  tasks that block on a latch that is never released when a timeout must happen.
- Always shut down executors (try/finally shutdown + awaitTermination, or try-with-resources only in a "(Java 19+)" comment),
  otherwise the JVM does not exit (non-daemon threads) and the verifier times out.
- Uncaught exceptions in threads print a stack trace to STDERR → always catch inside the task, or set an
  UncaughtExceptionHandler that prints to System.out a deterministic line. CompletableFuture exceptions: print
  getClass().getSimpleName() + getMessage() of the cause (CompletionException wraps it — show the unwrapping).
- Virtual threads, structured concurrency, scoped values = Java 21+ → comments only, marked "(Java 21+)".
Common content rules: each lesson ties to real use (Spring web server = thread pool per request, @Async, @Scheduled, singleton
beans must be thread-safe — SpringLearning). PUŁAPKA/DOBRA PRAKTYKA with "dlaczego". 3–4 exercises per lesson, deterministic
(return values computed by concurrent code but with a single correct answer, e.g. sum computed by N tasks).

BATCH S1:
1. Concurrency01Threads — process vs thread, why concurrency (waiting on IO, using cores), Thread with Runnable/lambda, start vs
   run (run = same thread — demo with Thread.currentThread().getName()), join (and join(timeout)), thread states (NEW, RUNNABLE,
   TIMED_WAITING, TERMINATED — print getState at deterministic moments), daemon threads (JVM does not wait for them),
   interrupt(): interrupting sleep → InterruptedException, restoring the flag (Thread.currentThread().interrupt()), cooperative
   cancellation (loop checking isInterrupted), never stop()/suspend() (deprecated — why), UncaughtExceptionHandler, sleep vs
   busy waiting, priorities are only hints, cost of threads (memory per stack) → pools in Concurrency04.
2. Concurrency02RaceConditions — shared mutable state, count++ is read-modify-write (three steps — show bytecode idea in a
   comment), lost update (facts only, see DETERMINISM), check-then-act (if (!map.containsKey) put) race, forced interleaving demo
   with latches that deterministically loses an update, fixes: synchronized method/block (monitor, one object = one lock),
   AtomicInteger/AtomicLong (incrementAndGet, compareAndSet loop, updateAndGet), LongAdder for counters, immutability and
   confinement (no sharing = no problem); synchronized on the wrong object (new Object() each time, on a boxed Integer/String
   literal) as PUŁAPKA; visibility issue preview (volatile stop flag) → Concurrency10.
3. Concurrency03Locks — intrinsic lock re-entrancy, ReentrantLock (lock/unlock in finally, tryLock, tryLock(timeout),
   lockInterruptibly, fairness), Condition (await/signal; while-loop for spurious wakeups) in a bounded buffer, ReadWriteLock
   (many readers, one writer), StampedLock briefly (optimistic read), wait/notify classic (always in while, holding the monitor;
   IllegalMonitorStateException demo), deadlock (4 conditions; lock ordering fix; tryLock with timeout detecting it; detection
   via ThreadMXBean — see DETERMINISM), livelock and starvation described.
4. Concurrency04Executors — why pools (thread reuse, limit), Executors.newFixedThreadPool with own ThreadFactory (names),
   submit Runnable vs Callable, Future (get, get(timeout) → TimeoutException, cancel(true), isDone), invokeAll (results in task
   order), invokeAny, shutdown vs shutdownNow vs awaitTermination (lifecycle; RejectedExecutionException after shutdown),
   ThreadPoolExecutor parameters (core, max, queue, rejection policies) and why newCachedThreadPool / unbounded queues are
   dangerous, exception inside a task is captured in Future (ExecutionException.getCause) — and lost with execute()+no handler,
   sizing rule of thumb (CPU-bound ≈ cores, IO-bound more), Spring's ThreadPoolTaskExecutor / @Async mention.
5. Concurrency05CompletableFuture — supplyAsync (always pass own executor — common ForkJoinPool caveat), thenApply / thenAccept /
   thenRun, thenCompose (flatMap) vs thenApply, thenCombine, allOf (collect results in order) / anyOf, exceptionally / handle /
   whenComplete, CompletionException wrapping, orTimeout / completeOnTimeout (Java 9+), join vs get (checked vs unchecked),
   *Async variants and which thread runs callbacks (not guaranteed — never print it), a realistic pipeline: fetch price and
   stock "services" (simulated with latches/short sleeps) in parallel, combine, fallback on failure; comparison with
   synchronous code (PRZED/PO); WebClient/reactive mention.
package-info.java: reading order of all 10 lessons with one-line descriptions.

BATCH S2:
6. Concurrency06ConcurrentCollections — why Collections.synchronizedList is not enough (compound actions, iteration needs
   manual synchronized), ConcurrentHashMap (atomic merge/compute/computeIfAbsent/putIfAbsent, no null keys/values, weakly
   consistent iterators — no ConcurrentModificationException, size is an estimate under contention), word counting with N tasks
   (deterministic result printed via TreeMap), CopyOnWriteArrayList (listeners; cheap reads, expensive writes),
   BlockingQueue (ArrayBlockingQueue, LinkedBlockingQueue; put/take vs offer/poll with timeout) — producer–consumer with poison
   pill, ConcurrentLinkedQueue, ConcurrentSkipListMap (sorted), PUŁAPKA: get-then-put on ConcurrentHashMap is still a race.
7. Concurrency07Synchronizers — CountDownLatch (start gate, finish gate; one-shot), CyclicBarrier (phases, barrier action),
   Semaphore (limit concurrent access — e.g. max 2 connections; count max concurrency observed → deterministic ≤ 2),
   Phaser briefly, Exchanger briefly, which to choose (table), all demos with deterministic printing after the fact.
8. Concurrency08ThreadSafetyPatterns — strategies: immutability (records, final, defensive copies), confinement (local
   variables, ThreadLocal — and its leak in pools; remove() in finally), synchronization, atomic variables, concurrent
   collections; thread-safe lazy init (holder idiom, enum singleton), safe publication (final fields guarantee), stateless
   services (Spring singleton beans — instance fields with request data are a bug, demo), SimpleDateFormat not thread-safe vs
   DateTimeFormatter immutable, documenting thread safety (@ThreadSafe idea in comments), checklist for code review.
9. Concurrency09ScheduledForkJoin — ScheduledExecutorService (schedule, scheduleAtFixedRate vs scheduleWithFixedDelay — explain
   difference; demo with a counter and latch, print counts only), exception in periodic task silently stops the schedule
   (PUŁAPKA — demo), Timer as legacy; ForkJoinPool and RecursiveTask (divide and conquer sum of array, threshold), work
   stealing explained, parallel streams use the common pool (ForkJoinPool.commonPool) — when they help and when they hurt
   (shared state, small data, blocking IO), Spring @Scheduled / cron mention.
10. Concurrency10MemoryModel — visibility, reordering, atomicity as three separate problems; happens-before rules (program order,
    monitor unlock→lock, volatile write→read, Thread.start, Thread.join, executor submit, final fields); volatile stop flag
    (correct) vs non-volatile (may never stop — explain JIT hoisting, do NOT run a loop that could hang: show only the correct
    version running, the broken one in comments), volatile does not make count++ atomic (facts-only demo), double-checked
    locking broken without volatile and correct with it (code + explanation), long/double tearing (non-volatile 64-bit
    writes may be split — JLS 17.7), false sharing mentioned, summary table "co gwarantuje co".

## R — t18_io_files (Sonnet, 4 batches: R1 Io01–04 + package-info, R2 Io05–08, R3 Io09–11, R4 Io12–14)   · TAG `io1`…`io4` · scope `"t18_io_files/*"`
Folder `src/t18_io_files/` (create). Package `t18_io_files`. package-info.java (batch R1) + 14 lessons. Learner knows t01–t17
(OOP, records, enums, exceptions incl. try-with-resources and wrapping, generics, collections, lambdas, Optional, BigDecimal,
streams, java.time). t19+ NOT known (no reflection, no concurrency). java.nio.file is the main API; java.io only where it still
matters (streams of bytes, Reader/Writer wrappers, serialization) — always say which one is "new" (NIO.2, Java 7+).
PORTABILITY = the hardest rule here. WYNIK lines are recorded on Linux, the learner runs Windows 10 (default charset windows-1250
in Java 17, `\` as separator, `\r\n` line ends, case-insensitive file names, open files cannot be deleted). Output MUST be
identical on both:
- All files live in `helpers.TempDir.create("io…")`, deleted in `finally` with `TempDir.deleteRecursively`. Never write into the
  project folder, never print an absolute path (temp folder name is random) → print paths RELATIVE to the temp dir and with `/`:
  declare in the lesson `private static String rel(Path base, Path p) { return base.relativize(p).toString().replace('\\', '/'); }`
  (explain once per lesson why). Path.toString() of a multi-part path printed raw = PUŁAPKA (Windows prints `a\b`).
- Never print `Charset.defaultCharset()`, `System.lineSeparator()`, `File.separator`, absolute/root paths, isAbsolute() of
  "/x", file sizes of files written with platform line separators (Files.write(path, lines), BufferedWriter.newLine(),
  PrintWriter.println, Properties.store) — those differ between systems; explain them in comments instead. Text written with
  explicit "\n" (Files.writeString) has a deterministic size → OK to print.
- ALWAYS pass a charset (StandardCharsets.UTF_8) to every Reader/Writer/InputStreamReader/getBytes/new String; the "no charset"
  variant only in comments as PUŁAPKA (Java 17 uses the system default; Java 18+ defaults to UTF-8 — JEP 400).
- Directory listings (Files.list/walk/find, DirectoryStream) have NO guaranteed order → sort by the rel(...) string. Use only
  lower-case ASCII-or-Polish file names that differ by more than case.
- IO exception messages contain the full path (and FileInputStream/FileReader messages contain an OS-language text like
  "(No such file or directory)" vs Polish Windows text) → never print getMessage() of IO exceptions. Do NOT use expectThrows
  for file operations; declare a small lesson helper, e.g. `ioFails(String label, ThrowingIo action)` printing
  `✔ label → rzucono NoSuchFileException (plik: nie-ma.txt)` using the exception's simple class name and, for
  FileSystemException, `Path.of(e.getFile()).getFileName()`. expectThrows is fine for non-IO exceptions (parsing, validation).
- Never demo permissions/AccessDeniedException at runtime (cloud runs as root, Windows differs), file locking, hidden files,
  probeContentType, POSIX attributes — comments only. Timestamps: set them yourself with FileTime.from(Instant.parse(...)) and
  print the FileTime (UTC ISO) — never print real creation/modification times.
- Close EVERYTHING (try-with-resources), including Files.lines/list/walk/find streams and zip file systems — on Windows an open
  handle makes deleting the temp dir fail.
- Compressed sizes (ZIP/GZIP) depend on the zlib build → print only facts like `skompresowany < oryginał → true`.
- Never print to System.err (the verifier treats any stderr output as a failed run): java.util.logging must use
  setUseParentHandlers(false) + own Handler/Formatter writing to System.out or to a file.
- Multi-line text produced by the JDK (Transformer, Properties.store, printStackTrace into a StringWriter) → normalize
  `\r\n` → `\n` and print line by line; skip the date comment line of Properties.store (explain why).
Common content rules: every lesson explains which exceptions can be thrown and why IOException is checked; PRZED/PO
(java.io.File + manual close → NIO.2 + try-with-resources) where natural; Polish letters in file content (ą, ę, ł, ż) to keep
charset awareness alive. Exercises work on files the exercise method receives as parameters (the exercises() method prepares a
temp dir, passes Paths, deletes it in finally); the checks compare strings/numbers/lists, never paths. 3–5 exercises per lesson.

BATCH R1:
1. Io01PathFiles — Path vs old java.io.File (File.toPath/Path.toFile, why NIO.2: exceptions instead of `false`, symlinks,
   attributes); Path.of (Java 11+) vs Paths.get; resolve, resolveSibling, relativize, normalize ("a/./b/../c" → a/c),
   getFileName, getParent, getNameCount, subpath, startsWith (by path elements, not text: Path.of("abc").startsWith("ab") false),
   endsWith; Path is only a NAME — no file needed (exists false); Files: exists/notExists (both false when unknown — comment),
   isDirectory, isRegularFile, createDirectory vs createDirectories, createFile (FileAlreadyExistsException), copy / move with
   StandardCopyOption.REPLACE_EXISTING (without it → FileAlreadyExistsException), ATOMIC_MOVE in comments, delete vs
   deleteIfExists, DirectoryNotEmptyException, size, get/setLastModifiedTime with a fixed FileTime, createTempFile/Directory
   (names random → only facts); toAbsolutePath / toRealPath only described (output machine-dependent).
2. Io02ReadingText — Files.readString (Java 11+), readAllLines, Files.lines (lazy Stream — must be closed: try-with-resources),
   BufferedReader + readLine loop (classic), newBufferedReader(path, UTF_8); counting words/lines, finding lines, line numbers;
   which to choose (small file → readString/readAllLines; big file → lines/BufferedReader; table: memory vs convenience);
   MalformedInputException when reading a windows-1250 or ISO-8859-2 encoded file as UTF-8 (prepare the bytes with getBytes of
   that charset) — preview of Io12; reading resources from the classpath only mentioned (getResourceAsStream, SpringLearning).
   Use Scanner(Path, UTF_8) briefly (token reading, nextInt) and when NOT to use it (slow, swallows IOException — ioException()).
3. Io03WritingText — Files.writeString with options CREATE, TRUNCATE_EXISTING (default), APPEND, CREATE_NEW; Files.write(lines)
   (adds platform line separator — size differs, explain, print content only); BufferedWriter (newBufferedWriter), PrintWriter
   with printf(Locale.ROOT, ...), flush vs close (data lost when not closed — demo: write via BufferedWriter without close, read
   → empty; then close it properly — must not leave it open!), write-to-temp-then-move atomic save pattern (resistant to crash
   in the middle), StringWriter for building text in memory, overwriting by accident (TRUNCATE default) as PUŁAPKA.
4. Io04Csv — reading SampleData.salesCsvLines() written to a file: header handling, split(",", -1) (why -1: trailing empty
   fields), parsing into a record SaleRow (LocalDate, sku, name, Category, int qty, BigDecimal price) with validation, collecting
   errors with line numbers instead of crashing (4 invalid lines: report "linia N: powód"), summary (total value per category
   via TreeMap or EnumMap, BigDecimal), writing a report CSV back and reading it again; quoting rules of real CSV (commas and
   quotes inside fields, "" escape) — implement a small quote-aware split for one line and show where naive split breaks;
   recommend libraries (OpenCSV, Apache Commons CSV, Jackson CSV) for real projects; Locale/decimal separator pitfall
   ("79,00" in Polish Excel exports, semicolon separator in Polish Excel).
package-info.java: reading order of all 14 lessons with one-line descriptions.

BATCH R2:
5. Io05JsonManual — JSON format (object, array, string, number, true/false/null), why the JDK has no JSON parser (Jackson/Gson in
   real projects; Spring Boot uses Jackson — SpringLearning); WRITE: a small JsonWriter building JSON from Map/List/record with
   correct escaping (", \, newline, control chars), stable key order (LinkedHashMap/TreeMap), pretty print with indent;
   READ: a tiny recursive-descent parser (objects, arrays, strings with escapes incl. \n \" \\ and \uXXXX — careful: AGENT_KIT
   escape rule!, numbers as BigDecimal, true/false/null) returning Map/List/String/BigDecimal/Boolean/null with clear error
   messages including position ("pozycja 17: oczekiwano ':'"); round trip file → objects → file; pitfalls: numbers as double
   (0.1+0.2), trailing commas not allowed, single quotes not allowed, comments not allowed. Keep the parser ≤ 120 lines.
6. Io06Properties — java.util.Properties: load(Reader) with UTF-8 (load(InputStream) assumes ISO-8859-1 → Polish letters
   broken — demo by reading UTF-8 bytes through load(InputStream) and printing the mojibake), getProperty with default,
   key=value / key: value / key value, comments # and !, line continuation \, spaces trimmed around keys, all values are
   String → parse int/boolean/Duration with validation and clear errors, store(Writer, comment) (date comment line skipped
   when printing, platform line separators normalized), keys are unordered (Hashtable) → print sorted
   (stringPropertyNames into TreeSet), layering defaults: new Properties(defaults) (default file + user overrides +
   System.getProperty override — only with a lesson-specific key set via System.setProperty and cleared after), typed config
   record built from Properties (like Spring's @ConfigurationProperties — mention), application.properties in Spring.
7. Io07WalkingDirectories — build a small tree in the temp dir (src/…, docs/…, a few files with known sizes); Files.list
   (one level), Files.walk (depth, maxDepth), Files.find (BiPredicate with attributes), DirectoryStream with glob
   ("*.{java,txt}"), PathMatcher (glob vs regex syntax: "glob:**/*.java"), walkFileTree with SimpleFileVisitor
   (preVisitDirectory, visitFile, postVisitDirectory, visitFileFailed; FileVisitResult CONTINUE/SKIP_SUBTREE/TERMINATE) —
   deleting a tree, copying a tree, computing directory sizes; printing an indented tree (├── └──) sorted; ALWAYS sort and
   close streams; why sorted(Comparator.reverseOrder()) deletes children before parents (as in helpers.TempDir); symlink loops
   (FOLLOW_LINKS) in comments.
8. Io08BinaryStreams — bytes vs characters (InputStream/OutputStream vs Reader/Writer), the decorator idea (FileOutputStream →
   BufferedOutputStream → DataOutputStream), Files.newInputStream/newOutputStream, readAllBytes, read(byte[]) loop and the
   "returns how many bytes it actually read" pitfall (ignoring the return value), transferTo (Java 9+), copying with a buffer,
   DataOutputStream/DataInputStream (writeInt, writeUTF, writeDouble — fixed format, read in the same order; EOFException),
   a tiny custom binary file format (magic number + version + records) with validation, hex dump of bytes (HexFormat is Java 17
   — java.util.HexFormat.of().formatHex(...) — mark (Java 17+)), signed byte pitfall (byte b = (byte) 200 → -56; & 0xFF),
   big-endian order of DataOutputStream vs ByteBuffer.order(LITTLE_ENDIAN), ByteArrayInput/OutputStream for tests in memory,
   why buffering matters (count write calls with a counting wrapper, no timings).

BATCH R3:
9. Io09Serialization — Serializable, ObjectOutputStream/ObjectInputStream to a file and to a byte array, transient fields
   (lost → default value), serialVersionUID (why declare it; InvalidClassException when it differs — demo by serializing with a
   class, then changing the UID in the BYTES is too hacky → instead explain + show ObjectStreamClass.lookup(X.class)
   .getSerialVersionUID() of a class with an explicit UID), static fields not serialized, non-serializable field →
   NotSerializableException (demo, message is the class name — deterministic, ok to print), superclass without Serializable
   needs a no-arg constructor (its fields re-initialized — demo), records serialize via canonical constructor (validation runs
   on deserialization — demo with compact constructor rejecting bad data; normal classes skip constructors), custom
   writeObject/readObject briefly, deep copy via serialization (and why copy constructors are better), SECURITY: deserializing
   untrusted data is dangerous (gadget chains), ObjectInputFilter (Java 9+) with an allow-list demo
   (ObjectInputFilter.Config.createFilter("t18_io_files.*;java.base/*;!*")), modern alternatives (JSON, protobuf); -Xlint:all
   requires serialVersionUID on Serializable classes ([serial] warning) — every Serializable class declares
   `private static final long serialVersionUID = 1L;` (except records/enums where it is not needed — check the compiler).
10. Io10SimpleLogger — why logging instead of System.out (levels, timestamps, destinations, can be switched off); levels
    TRACE/DEBUG/INFO/WARN/ERROR; own SimpleLogger: level filter, injected Clock (Clock.fixed → deterministic timestamps),
    format "2026-05-04 12:00:00.000 [INFO ] Klasa — wiadomość", appending to a log file in the temp dir (Files.writeString
    APPEND, UTF-8), placeholders "{}" like SLF4J (implement), exception logging with the stack trace's first line only (class +
    message; full trace not printed — line numbers would change), lazy messages with Supplier (cost of string building when
    level disabled — count calls), rolling by size (log.1, log.2 — simple rename demo); java.util.logging overview with a
    custom Handler to System.out + Formatter (NEVER to stderr) and setUseParentHandlers(false); SLF4J + Logback in real projects
    and in Spring Boot (comments); don't log passwords/PESEL; log levels in production.
11. Io11IoExceptions — hierarchy: IOException → FileSystemException → NoSuchFileException, FileAlreadyExistsException,
    DirectoryNotEmptyException, AccessDeniedException, NotDirectoryException; FileNotFoundException (old java.io) vs
    NoSuchFileException (NIO.2) — when each appears; MalformedInputException/CharacterCodingException; EOFException;
    UncheckedIOException inside lambdas/streams (Files.lines → map with IO inside → wrap and unwrap getCause), try-with-resources
    with several resources (closing order reversed — trace with own AutoCloseable), suppressed exceptions from close()
    (getSuppressed — own resource throwing in close), what to catch where (low level: throw; top level: one message for the
    user + log), retry with limit for transient errors (simulated with a counter, no sleeps), NEVER swallow (empty catch) —
    PRZED/PO, don't use exists()-then-open (TOCTOU race) — just try and catch NoSuchFileException; Files.newBufferedReader
    vs new FileReader — message language pitfall (why we print exception TYPES here).

BATCH R4:
12. Io12Charsets — bytes vs chars vs code points; what an encoding is; UTF-8 (1–4 bytes; ASCII 1 byte, Polish letters 2 bytes,
    emoji 4 bytes — "zażółć".getBytes(UTF_8).length), ISO-8859-2, windows-1250, UTF-16 (2 or 4 bytes + BOM in "UTF-16");
    hex of "ł" in each; mojibake demos (UTF-8 bytes decoded as windows-1250 / ISO-8859-1 → "zaĹĽĂłĹ‚Ä‡"-like text — copy from a
    real run), malformed bytes: new String replaces with U+FFFD (print as "?" via replace, or print the code point count) vs
    CharsetDecoder with CodingErrorAction.REPORT → MalformedInputException; String.length() vs codePointCount for emoji (build
    emoji via Character.toChars(0x1F600) — no escapes in source!), BOM (EF BB BF) at the start of UTF-8 files from Windows
    Notepad — detect and strip; Java 17 default charset depends on the OS (windows-1250 on Polish Windows) vs Java 18+ UTF-8
    (JEP 400) — explain, never print it; file.encoding, IntelliJ encoding settings, console output; Charset.isSupported,
    availableCharsets (print only contains checks). Normalizer (NFC vs NFD: "ó" as one or two code points) — short.
13. Io13ZipArchives — ZipOutputStream (putNextEntry, closeEntry, directories end with "/"), ZipInputStream (iterate entries,
    read content), ZipFile (random access, entries()), zip file system: FileSystems.newFileSystem(zipPath, Map.of("create",
    "true")) then Files.copy/writeString/walk inside the zip (Java 13+ overload newFileSystem(Path, Map) — check & mark), GZIP
    (GZIPOutputStream/GZIPInputStream) of a text, compression facts only (compressed < original), entry names in UTF-8 (Polish
    names), ZIP SLIP security pitfall (entry "../../evil.txt" → normalize + startsWith(target) check, demo the check rejecting
    it), zip bomb (limit total size), sorting entries for deterministic output, CRC32 (java.util.zip.CRC32 value of a known text
    is deterministic — ok to print), jar = zip (mention).
14. Io14Xml — XML basics (elements, attributes, text, well-formed vs valid), DOM: DocumentBuilderFactory (secure: disallow
    DOCTYPE — feature "http://apache.org/xml/features/disallow-doctype-decl" — XXE explained), parse from a string/file,
    getElementsByTagName, attributes, text content, modify and write back with Transformer (OutputKeys.INDENT; normalize line
    ends; print line by line), whitespace text nodes pitfall (getChildNodes counts them); StAX: XMLStreamReader (pull, events,
    low memory — big files), XMLStreamWriter (writing with escaping of < & "); XPath: XPathFactory, expressions
    ("/biblioteka/ksiazka[@rok>2010]/tytul", count(), text()), NodeList → List<String>; escaping (&lt; &amp; in XML text —
    careful: the lesson Javadoc must not contain HTML entities; put such examples inside string literals or {@code});
    SAX mentioned; JAXB / Jackson XML not in JDK 17 (removed in Java 11) — mention; namespaces briefly. Data: a small
    library catalog (books with Polish titles, years, ISBN-like ids).

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
