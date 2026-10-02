/**
 * TEMAT: t21_concurrency — współbieżność w Javie: wątki, wyścigi, blokady, pule wątków, CompletableFuture,
 * kolekcje współbieżne, synchronizatory, wzorce bezpieczeństwa wątkowego, harmonogramy, Fork/Join i model pamięci.
 *
 * <p>Ten dział uczy, jak program robi kilka rzeczy naraz i jak robić to BEZPIECZNIE. Współbieżność (concurrency =
 * współbieżność) to jeden z najtrudniejszych tematów w programowaniu: błędy pojawiają się rzadko, zależą od maszyny
 * i obciążenia, a test „u mnie działa” niczego nie dowodzi. Dlatego każda lekcja opiera się na tym, co Java
 * GWARANTUJE (model pamięci Javy i dokumentacja pakietu java.util.concurrent), a nie na tym, co „zwykle” się dzieje.
 * Wszystkie przykłady są deterministyczne: wątki zapisują wyniki do bezpiecznej wątkowo struktury, a wątek główny
 * czeka na nie i dopiero wtedy wypisuje je w ustalonej kolejności.</p>
 *
 * <p>Po co to w praktyce: serwer WWW (np. Tomcat w Springu) obsługuje każde żądanie HTTP w wątku z puli, więc
 * jeden obiekt (singletonowy bean Springa) jest używany przez wiele wątków naraz i musi być bezpieczny wątkowo.
 * Adnotacje Springa {@code @Async} i {@code @Scheduled} to nakładki na pule wątków z tego działu.</p>
 *
 * <p>Wymagania wstępne: t06–t10 (obiekty, dziedziczenie, rekordy, wyjątki), t12 (kolekcje), t13 (lambdy),
 * t16 (strumienie, w tym podstawy strumieni równoległych).</p>
 *
 * <p>Kolejność czytania:</p>
 * <ol>
 *   <li>Concurrency01Threads — proces a wątek, start vs run, join, stany wątku, demony, przerywanie (interrupt).</li>
 *   <li>Concurrency02RaceConditions — wyścig, count++ to trzy kroki, utracona aktualizacja, synchronized, klasy Atomic*.</li>
 *   <li>Concurrency03Locks — ReentrantLock, Condition, ReadWriteLock, StampedLock, wait/notify, zakleszczenie.</li>
 *   <li>Concurrency04Executors — pule wątków, Callable i Future, invokeAll, cykl życia puli, ThreadPoolExecutor.</li>
 *   <li>Concurrency05CompletableFuture — łańcuchy zadań asynchronicznych, łączenie wyników, obsługa błędów, limity czasu.</li>
 *   <li>Concurrency06ConcurrentCollections — ConcurrentHashMap, CopyOnWriteArrayList, kolejki blokujące.</li>
 *   <li>Concurrency07Synchronizers — CountDownLatch, CyclicBarrier, Semaphore, Phaser, Exchanger.</li>
 *   <li>Concurrency08ThreadSafetyPatterns — niezmienność, zamknięcie w wątku, ThreadLocal, bezstanowe serwisy.</li>
 *   <li>Concurrency09ScheduledForkJoin — zadania okresowe, ForkJoinPool i RecursiveTask, wspólna pula strumieni.</li>
 *   <li>Concurrency10MemoryModel — widoczność, zmiana kolejności, happens-before, volatile, podwójne sprawdzanie.</li>
 * </ol>
 *
 * <p>SŁÓWKA: thread = wątek; concurrency = współbieżność; parallelism = równoległość; race condition = wyścig
 * (sytuacja wyścigu); lock = blokada (zamek); deadlock = zakleszczenie; thread-safe = bezpieczny wątkowo;
 * executor = wykonawca; pool = pula; future = przyszły wynik; atomic = niepodzielny (atomowy);
 * visibility = widoczność; happens-before = „dzieje się wcześniej” (relacja porządku w modelu pamięci).</p>
 */
package t21_concurrency;
