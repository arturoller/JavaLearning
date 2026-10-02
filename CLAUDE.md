# JavaLearning — instrukcje dla Claude (autor i opiekun kursu)

Ten plik czyta każda sesja Claude Code w tym repozytorium (lokalna i w chmurze). Opisuje, JAK tworzymy kurs.
Opis kursu dla ucznia: `README.md`. Szczegółowa specyfikacja lekcji dla podagentów: `tools/AGENT_KIT.md`.

## 1. Cel i odbiorca
- Kurs Javy SE 17 **po polsku** dla osoby, która uczy się programowania i utrwala wiedzę. Kontynuacja: kurs Springa
  https://github.com/arturoller/SpringLearning (ten sam styl i tagi).
- Uczeń słabo zna angielski: **wszystkie komentarze i wydruki po polsku**, a każda angielska nazwa (metoda JDK, słowo kluczowe,
  własna metoda/zmienna) dostaje polskie tłumaczenie przy pierwszym użyciu w pliku (`// orElseGet = albo pobierz`).
- Polszczyzna naturalna, bez kalk: immutable = „niezmienny” (NIE „niemutowalny”), mutable = „zmienny”, hiding/shadowing =
  „ukrywanie/przesłanianie”, side effect = „efekt uboczny”. Terminologia: `src/t00_start/Start02Glossary.java`.
- Treść od zera, skupiona na zasadach, dobrych praktykach i pułapkach; bez odwołań do prywatnych projektów ucznia.
- Szczegółowość i trafność są celem: uczeń uczy się z komentarzy — fałszywe zdanie w komentarzu to błąd krytyczny.

## 2. Struktura projektu
```
src/helpers/        wspólne narzędzia lekcji (Console, Check, SampleData, model/) — NIE zmieniaj istniejących danych
src/tNN_temat/      działy t00–t35, lekcje bezpośrednio w pakiecie działu (np. src/t08_enums/Enums01Basics.java)
tools/              narzędzia autorów (w gicie): Verify.java, AGENT_KIT.md, lessons.txt (rejestr), tags.txt, ASSIGNMENTS.md
temp/               lokalne pliki robocze (w .gitignore): build/, lib/ (biblioteki), tmp-*/ (eksperymenty), commitmsg.txt
pom.xml, mvnw       Maven (sourceDirectory = src), Java 17, Lombok tylko dla t20; Maven Wrapper — globalny mvn niepotrzebny
```
- Układ `src/tNN_…` (bez `src/main/java`) to świadoma decyzja ucznia — nie zmieniaj go.
- Nazwy klas: `<Temat><NN><Aspekt>` (np. `Streams11GroupingBy`); pakiety `tNN_temat`; nigdy nazw kolidujących z JDK.
- Lekcje nie importują klas z innych lekcji (weryfikator kompiluje dział osobno) — potrzebne typy deklaruj w lekcji.

## 3. Format lekcji (pełna specyfikacja: `tools/AGENT_KIT.md`)
- Nagłówek w Javadoc `<pre>`: `TEMAT:` `W SKRÓCIE:` `ANALOGIA:` `JAK TO DZIAŁA:` `SŁÓWKA:` `ZOBACZ TEŻ:` — tag zawsze z dwukropkiem
  zaraz po słowie. W treści `PUŁAPKA:` / `DOBRA PRAKTYKA:` (zawsze z „dlaczego”), sekcje = metody wołane z `main`.
- Po każdym wydruku `// WYNIK: <dokładna linia>` — skopiowana z PRAWDZIWEGO uruchomienia, nigdy zgadywana.
- Na końcu `ŚCIĄGA:`, `PYTANIA KONTROLNE:` (5–8, min. 2 „Co wypisze?” / „ZNAJDŹ BŁĄD”), ćwiczenia `exerciseN` sprawdzane przez
  `Check.equal(…, () -> exerciseN(…))`, a w zwiniętych blokach (`editor-fold … defaultstate="collapsed"`) rozwiązania wzorcowe
  i `ODPOWIEDZI:`. Zaślepka ćwiczenia, która mogłaby przypadkiem przejść test, rzuca `UnsupportedOperationException("TODO")`.
- Determinizm: Random z ziarnem, jawne Locale, nie wypisuj HashSet/HashMap (szczególnie z kluczami-enumami), `Clock` zamiast
  `now()` w wynikach, wynik zależny od uruchomienia → bez WYNIK, z komentarzem `// (wynik zależy od uruchomienia)`.
- Odpowiedzi „Co wypisze?” sprawdzaj uruchomieniem kodu, nie liczeniem w głowie.

## 4. Weryfikacja
```
./mvnw -q dependency:copy-dependencies -DoutputDirectory=temp/lib     # raz (i po zmianie zależności w pom.xml)
java -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 tools/Verify.java --tag X --scope "t18_io_files/*" --run all
```
- **Wymagany JDK 17** (weryfikator odmawia innego — linie WYNIK nagrano na Temurin 17; inny JDK zmienia komunikaty wyjątków
  i dane Locale). Kompiluje z `--release 17 -g -Xlint:all` (ostrzeżenia = problemy).
- Sprawdza: kompilację, encje HTML, niewidoczne znaki, format tagów, wymagane tagi, ćwiczenia, zwinięte bloki, odesłania
  `tNN_pakiet/Klasa` względem `tools/lessons.txt`, uruchamia każdą lekcję i porównuje linie WYNIK — także ich KOLEJNOŚĆ.
- Gotowe, gdy: `COMPILE EXIT: 0`, `PROBLEMS: 0`, `FAILED RUNS: 0`, każda lekcja `mismatches: 0`, wzorcowe rozwiązania `✘ 0 BŁĄD`.
- Bez `--scope` = cały kurs (rób to przed oddaniem pracy i po każdej zmianie w `helpers`).

## 5. Jak pracujemy
- **Plan:** spis treści w README + rejestr `tools/lessons.txt`. Przed pisaniem działu: konspekt w `tools/ASSIGNMENTS.md`
  (wzór: istniejące sekcje A–Q) i nazwy jego lekcji w rejestrze.
- **Podagenci:** proste działy → model Sonnet, trudne (współbieżność, JDBC, moduły/budowanie, testy, most do Springa) → Opus.
  Najwyżej 2 naraz. Agent dostaje `tools/AGENT_KIT.md` + JEDNĄ sekcję konspektu, nie skanuje projektu, weryfikuje każdą lekcję
  od razu po napisaniu. Nowa paczka lekcji = nowy agent; poprawki do jego paczki = wiadomość do tego samego agenta.
- **Odbiór pracy agenta (zawsze przez główną sesję):** weryfikacja całego działu + wyrywkowo merytoryka (PUŁAPKA, ŚCIĄGA,
  ODPOWIEDZI, liczby i fakty) + szukanie kalk językowych. Dopiero potem commit.
- **Git:** 1 dział = 1 commit, w tym samym commicie wiersz działu w README (⏳ → ✅ z opisem). Komunikaty po polsku
  (`t18_io_files: pliki (14 lekcji)`) z liniami Co-Authored-By. Lokalnie: commit na `main`, **push robi użytkownik**.
  **W chmurze: gałąź na dział (np. `t18-io-files`), push gałęzi i pull request do `main` — nigdy push na `main`.**
  Jeden PR = jeden dział (łatwy przegląd).

## 6. Stan i kolejność pracy (aktualizuj po każdym dziale)
- **Gotowe (✅):** helpers, wszystkie działy t00–t35.
- **Do napisania:** nic — wszystkie działy z planu są gotowe. Nazwy lekcji: `tools/lessons.txt`; tematy: README.
- **Dodatki w gotowych działach (🔶 w README):** t00 Start06Git/Start07IntelliJRefactoring/Start08JShell,
  t27 CleanCode03Architecture.
- **Na koniec:** indeks haseł A–Z w README; przegląd wszystkich odpowiedzi „Co wypisze?” (uruchomieniem); decyzje ucznia:
  licencja, `.gitattributes`.
- **Uwagi do działów:** t28 i t34/t35 (HTTP) — tylko localhost, port 0, bez internetu; t29 — H2 w pom.xml, w kodzie tylko
  java.sql; t32 — JUnit 5/AssertJ/Mockito w pom.xml, testy uruchamiane z `main` przez JUnit Platform Launcher. Po każdej
  zmianie zależności w pom.xml ponów copy-dependencies do temp/lib.

## 7. Pułapki środowiska
- Komputer ucznia: Windows 10, PowerShell 5.1, IntelliJ IDEA 2026.1. Bitdefender blokuje pliki `.ps1` (i `.txt` ze skryptami
  PowerShell) — narzędzia pisz w Javie. PowerShell traktuje polskie cudzysłowy „ ” jak `"` — nie używaj ich w stringach
  skryptów (np. komunikatach commitów — zapisuj je do `temp/commitmsg.txt` i `git commit -F`).
- Narzędzie Write/Edit zamienia `\uXXXX` na prawdziwe znaki — unikaj takich escape'ów albo przywracaj je zamianą w powłoce.
- Pliki UTF-8 bez BOM. Chwilowy błąd gita „Unable to write new index file” (IntelliJ/antywirus trzyma plik) → ponów polecenie.
- W chmurze (Linux) powyższe problemy Windows nie występują, ale nadal: JDK 17 i `./mvnw`.
- W chmurze weryfikator uruchamiaj bez zmiennej `JAVA_TOOL_OPTIONS` (JVM wypisuje „Picked up …” na stderr → każde uruchomienie
  liczy się jako FAILED RUN) i z polskim Locale (linie WYNIK nagrano na polskim Windows; np. `IntSummaryStatistics.toString`
  używa Locale domyślnego): `locale-gen pl_PL.UTF-8`, potem `env -u JAVA_TOOL_OPTIONS LANG=pl_PL.UTF-8 java … tools/Verify.java …`.
  Brak JDK 17 → `apt-get install openjdk-17-jdk-headless` (JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64).

## 8. Powiązania z SpringLearning
- SpringLearning trzyma kopię rejestru tego kursu (`tools/javalearning-lessons.txt`) do sprawdzania odesłań. Gdy tu zmienią się
  nazwy lekcji, zaktualizuj tamtą kopię (osobny commit w tamtym repozytorium).
