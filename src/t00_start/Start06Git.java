package t00_start;

import helpers.Check;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Git od zera — historia projektu, gałęzie, scalanie, praca ze zdalnym repozytorium
 *        (Git = „głupi menedżer treści”, w praktyce: system kontroli wersji;
 *         commit = zatwierdzenie zmian; branch = gałąź; merge = scalenie)
 *
 * W SKRÓCIE:
 *   Git zapisuje kolejne WERSJE (migawki) Twojego projektu i pamięta, kto, kiedy i dlaczego je zmienił.
 *   Dzięki temu możesz wrócić do dowolnej wersji, pracować nad kilkoma pomysłami naraz (gałęzie)
 *   i bezpiecznie łączyć pracę wielu osób. W tej lekcji NIE uruchamiamy gita — budujemy jego
 *   miniaturowy MODEL w czystej Javie (commity, gałęzie, scalanie, konflikty), żebyś zobaczył MECHANIKĘ.
 *   Polecenia gita pokazujemy w komentarzach.
 *
 * ANALOGIA: gra komputerowa z zapisami stanu.
 *   Commit to „zapis gry” z podpisem. Gałąź to równoległa rozgrywka od wybranego zapisu („a co, jeśli
 *   pójdę w lewo?”). Scalenie to połączenie dwóch rozgrywek w jedną. Zdalne repozytorium (GitHub)
 *   to chmura, do której wysyłasz swoje zapisy, żeby koledzy mogli je pobrać.
 *
 * JAK TO DZIAŁA:
 *   Trzy miejsca, między którymi przenosisz zmiany:
 *
 *     katalog roboczy ──git add──▶ poczekalnia ──git commit──▶ repozytorium (historia)
 *     (working tree)               (staging area / index)       (.git)
 *          ◀──────────────────────── git restore / git switch ─────────────┘
 *
 *   Commit = migawka CAŁEGO projektu + wiadomość + wskaźnik na commit-rodzica. Identyfikator commita
 *   to skrót (hash) z jego treści, więc zmiana czegokolwiek w historii zmienia WSZYSTKIE kolejne id.
 *
 *     C1 ◀── C2 ◀── C3 ◀── C5          main  (gałąź = tylko ruchoma ETYKIETA na commicie)
 *                    ▲
 *                    └──── C4          feature
 *     HEAD = „tu teraz jestem” (zwykle wskazuje na gałąź, a ta na commit)
 *
 *   Gałąź to nie kopia plików, tylko mały wskaźnik (nazwa → id commita) — dlatego tworzenie gałęzi
 *   jest błyskawiczne. Commit na bieżącej gałęzi przesuwa jej etykietę do przodu.
 *
 * SŁÓWKA: repository = repozytorium (skład wersji); working tree = katalog roboczy; staging area =
 *   poczekalnia (obszar przygotowania); commit = zatwierdzenie; branch = gałąź; merge = scalenie;
 *   conflict = konflikt; rebase = „przeszczepienie” commitów na nową bazę; remote = zdalne repozytorium;
 *   clone = sklonuj (skopiuj całe repozytorium); fetch = pobierz; pull = pobierz i scal; push = wyślij;
 *   stash = schowek; tag = znacznik (etykieta wersji); fast-forward = „przewiń do przodu”;
 *   revert = cofnij przez nowy commit; reset = przesuń gałąź wstecz; hash = skrót, odcisk palca
 *
 * ZOBACZ TEŻ: t00_start/Start05Debugging (praca w IntelliJ), t00_start/Start07IntelliJRefactoring
 *   (bezpieczne zmiany kodu — pod kontrolą wersji), t00_start/Start03LearningPath (jak się uczyć)
 * </pre>
 */
public class Start06Git {

    public static void main(String[] args) {
        title("Start06 — Git od zera");

        hashing();        // hashing = liczenie skrótu (odcisku palca)
        threeAreas();     // three areas = trzy obszary
        history();        // history = historia
        branches();       // branches = gałęzie
        fastForward();    // fast-forward = przewinięcie do przodu
        mergeCommit();    // merge commit = commit scalający
        conflict();       // conflict = konflikt
        rebase();         // rebase = przeszczepienie commitów
        remote();         // remote = zdalne repozytorium
        undo();           // undo = cofanie zmian
        everyday();       // everyday = na co dzień (.gitignore, wiadomości, schowek, tagi)
        exercises();      // exercises = ćwiczenia
    }

    // =================================================================================================
    // MODEL GITA W PIGUŁCE (to „silnik” lekcji — nie musisz rozumieć każdej linii, ważne są sekcje niżej)
    // =================================================================================================

    /** Liczy SHA-256 (algorytm skrótu: ten sam tekst → zawsze ten sam skrót) i zwraca go jako 64 znaki hex. */
    static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256"); // MessageDigest = „skracacz wiadomości”
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8)); // bytes = bajty
            return HexFormat.of().formatHex(bytes); // HexFormat (Java 17+) = zapis szesnastkowy
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 jest w każdym JDK", e); // algorytm zawsze istnieje
        }
    }

    /** Skrócony identyfikator — tak jak git pokazuje 7 pierwszych znaków skrótu. */
    static String shortHash(String text) {
        return sha256(text).substring(0, 7);
    }

    /**
     * Commit = zatwierdzona migawka. W gicie: drzewo plików + autor + data + wiadomość + rodzice.
     * U nas: treść jednego „pliku”, wiadomość i lista rodziców (1 rodzic = zwykły commit, 2 = scalenie).
     */
    record Commit(String id, List<String> parents, String message, String content) {
        // parents = rodzice; message = wiadomość; content = zawartość pliku (model ma jeden plik)

        /** Fabryka (create = utwórz): id liczone ze WSZYSTKIEGO, także z rodziców. */
        static Commit create(List<String> parents, String message, String content) {
            String text = "parents=" + String.join(",", parents) + "|message=" + message + "|content=" + content;
            return new Commit(shortHash(text), List.copyOf(parents), message, content);
        }
    }

    /** Wynik scalenia trzech wersji: gotowy tekst i informacja, czy powstał konflikt. */
    record MergeResult(String text, boolean conflict) { }

    /**
     * Scalenie trójstronne (3-way merge) „pliku” z TAKĄ SAMĄ liczbą linii (to uproszczenie —
     * prawdziwy git wylicza różnice algorytmem diff i radzi sobie z dodawaniem i usuwaniem linii).
     * Reguła dla każdej linii: jeśli obie strony mają to samo — bierzemy; jeśli zmieniła się tylko jedna
     * strona względem WSPÓLNEGO PRZODKA (base) — bierzemy zmienioną; jeśli zmieniły obie różnie — KONFLIKT.
     */
    static MergeResult merge3(String base, String ours, String theirs, String oursName, String theirsName) {
        // ours = nasza strona; theirs = ich strona (scalana gałąź)
        String[] b = base.split("\n");
        String[] o = ours.split("\n");
        String[] t = theirs.split("\n");
        if (b.length != o.length || b.length != t.length) {
            throw new IllegalArgumentException("uproszczenie modelu: wszystkie wersje mają tyle samo linii");
        }
        List<String> result = new ArrayList<>();
        boolean conflict = false;
        for (int i = 0; i < b.length; i++) {
            if (o[i].equals(t[i])) {
                result.add(o[i]);                       // obie strony zgodne (albo nikt nie ruszał)
            } else if (b[i].equals(o[i])) {
                result.add(t[i]);                       // zmienili tylko oni
            } else if (b[i].equals(t[i])) {
                result.add(o[i]);                       // zmieniliśmy tylko my
            } else {                                    // obie strony zmieniły TĘ SAMĄ linię inaczej
                conflict = true;
                result.add("<<<<<<< " + oursName);      // znacznik początku: nasza wersja
                result.add(o[i]);
                result.add("=======");                  // separator
                result.add(t[i]);
                result.add(">>>>>>> " + theirsName);    // znacznik końca: ich wersja
            }
        }
        return new MergeResult(String.join("\n", result), conflict);
    }

    /** Prosty diff (różnice) dla wersji o tej samej liczbie linii: „-” usunięta, „+” dodana. */
    static List<String> diff(String oldText, String newText) {
        String[] a = oldText.split("\n");
        String[] b = newText.split("\n");
        List<String> result = new ArrayList<>();
        for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(b[i])) {
                result.add("- " + a[i]);
                result.add("+ " + b[i]);
            }
        }
        return result;
    }

    /** Miniaturowe repozytorium: jeden plik, commity, gałęzie, HEAD, poczekalnia i (opcjonalnie) zdalne „origin”. */
    static class MiniRepo {
        final String name;                                              // name = nazwa (do wydruków)
        final Map<String, Commit> commits = new LinkedHashMap<>();      // baza commitów: id → commit
        final Map<String, String> branches = new TreeMap<>();           // gałęzie: nazwa → id commita
        final Map<String, String> remoteBranches = new TreeMap<>();     // zapamiętane gałęzie zdalne: origin/main → id
        final Map<String, String> tags = new TreeMap<>();               // tagi: nazwa → id commita
        final Deque<String> stash = new ArrayDeque<>();                 // schowek (stos): ostatnio schowane na wierzchu
        MiniRepo origin;                                                // zdalne repozytorium (null = brak)
        String head = "main";                                           // HEAD: nazwa bieżącej gałęzi
        String workingTree = "";                                        // katalog roboczy (treść pliku)
        String index = "";                                              // poczekalnia (staging area)
        String mergeHead;                                               // drugi rodzic trwającego scalenia (konflikt)

        MiniRepo(String name) {
            this.name = name;
        }

        /** git clone: kopia całej historii + zapamiętanie zdalnego repozytorium jako „origin”. */
        static MiniRepo clone(String name, MiniRepo source) {
            MiniRepo copy = new MiniRepo(name);
            copy.origin = source;
            copy.commits.putAll(source.commits);
            copy.branches.putAll(source.branches);
            source.branches.forEach((branch, id) -> copy.remoteBranches.put("origin/" + branch, id));
            copy.workingTree = copy.tipContent();
            copy.index = copy.tipContent();
            return copy;
        }

        String tip() {                                  // tip = czubek (ostatni commit bieżącej gałęzi)
            return branches.get(head);
        }

        String tipContent() {
            return tip() == null ? "" : commits.get(tip()).content();
        }

        String describe(String id) {                    // describe = opisz: „id wiadomość”
            return id + " " + commits.get(id).message();
        }

        void edit(String text) {                        // edit = edytuj plik w katalogu roboczym
            workingTree = text;
        }

        void add() {                                    // git add: katalog roboczy → poczekalnia
            index = workingTree;
        }

        /** git commit: poczekalnia → nowy commit na bieżącej gałęzi. */
        String commit(String message) {
            boolean merging = mergeHead != null;
            if (!merging && tip() != null && index.equals(tipContent())) {
                throw new IllegalStateException("no changes added to commit");   // nic w poczekalni
            }
            List<String> parents = new ArrayList<>();
            if (tip() != null) {
                parents.add(tip());
            }
            if (merging) {
                parents.add(mergeHead);                 // commit scalający ma DWÓCH rodziców
                mergeHead = null;
            }
            Commit c = Commit.create(parents, message, index);
            commits.put(c.id(), c);
            branches.put(head, c.id());                 // bieżąca gałąź „jedzie” za nowym commitem
            return c.id();
        }

        /** Skrót myślowy: edit + add + commit (w prawdziwym gicie: git commit -a dla śledzonych plików). */
        String commitAll(String content, String message) {
            edit(content);
            add();
            return commit(message);
        }

        void createBranch(String branchName) {          // git branch nazwa: nowa etykieta na bieżącym commicie
            branches.put(branchName, tip());
        }

        void switchTo(String branchName) {              // git switch nazwa: HEAD → gałąź, katalog = jej treść
            head = branchName;
            workingTree = tipContent();
            index = tipContent();
        }

        String resolve(String refName) {                // resolve = rozwiąż nazwę na id (gałąź lokalna lub origin/...)
            String id = branches.containsKey(refName) ? branches.get(refName) : remoteBranches.get(refName);
            if (id == null) {
                throw new IllegalArgumentException("nieznana gałąź: " + refName);
            }
            return id;
        }

        /** Wszyscy przodkowie commita (łącznie z nim), od najbliższych — przeszukiwanie wszerz (BFS). */
        Set<String> ancestors(String id) {
            Set<String> seen = new LinkedHashSet<>();
            Deque<String> queue = new ArrayDeque<>();
            queue.add(id);
            while (!queue.isEmpty()) {
                String current = queue.poll();
                if (seen.add(current)) {
                    queue.addAll(commits.get(current).parents());
                }
            }
            return seen;
        }

        boolean isAncestor(String maybeAncestor, String id) {
            return ancestors(id).contains(maybeAncestor);
        }

        /** Wspólny przodek (merge base): najbliższy commit dostępny z obu końców. */
        String mergeBase(String a, String b) {
            Set<String> ofA = ancestors(a);
            for (String id : ancestors(b)) {
                if (ofA.contains(id)) {
                    return id;
                }
            }
            return null;                                // historie niepowiązane
        }

        /** Id commitów od czubka bieżącej gałęzi do korzenia (idziemy po pierwszym rodzicu). */
        List<String> chain() {
            List<String> ids = new ArrayList<>();
            String id = tip();
            while (id != null) {
                ids.add(id);
                List<String> parents = commits.get(id).parents();
                id = parents.isEmpty() ? null : parents.get(0);
            }
            return ids;
        }

        List<String> log() {                            // git log --oneline
            return chain().stream().map(this::describe).toList();   // toList (Java 16+) = do listy
        }

        /** git merge: „Already up to date”, „Fast-forward”, commit scalający albo konflikt. */
        String merge(String otherName) {
            String other = resolve(otherName);
            String mine = tip();
            if (isAncestor(other, mine)) {
                return "Already up to date.";           // już zawiera tamtą gałąź
            }
            if (isAncestor(mine, other)) {              // nasza gałąź to „przeszłość” tamtej
                branches.put(head, other);              // wystarczy przesunąć etykietę — bez nowego commita
                workingTree = commits.get(other).content();
                index = workingTree;
                return "Fast-forward";
            }
            String base = mergeBase(mine, other);
            MergeResult r = merge3(commits.get(base).content(), commits.get(mine).content(),
                    commits.get(other).content(), "HEAD", otherName);
            workingTree = r.text();
            if (r.conflict()) {
                mergeHead = other;                      // git zapamiętuje, że trwa scalenie
                return "CONFLICT (content): Merge conflict in notatki.txt";
            }
            index = r.text();
            mergeHead = other;
            commit("Merge branch '" + otherName + "'");
            return "Merge made by the 'ort' strategy.";
        }

        /**
         * git rebase: odtwarza (na nowo) commity bieżącej gałęzi na końcu innej gałęzi. Każdy odtworzony
         * commit ma INNEGO rodzica, więc dostaje NOWE id — to nie są te same commity!
         */
        String rebase(String ontoName) {
            String onto = resolve(ontoName);
            String base = mergeBase(tip(), onto);
            List<String> toReplay = new ArrayList<>();  // replay = odtwórz: commity od bazy do czubka
            for (String id = tip(); !id.equals(base); id = commits.get(id).parents().get(0)) {
                toReplay.add(0, id);                    // od najstarszego
            }
            String newTip = onto;
            for (String id : toReplay) {
                Commit old = commits.get(id);
                String oldParentContent = commits.get(old.parents().get(0)).content();
                MergeResult r = merge3(oldParentContent, commits.get(newTip).content(), old.content(), "HEAD", old.message());
                if (r.conflict()) {
                    throw new IllegalStateException("w modelu pomijamy konflikty podczas rebase");
                }
                Commit copy = Commit.create(List.of(newTip), old.message(), r.text());
                commits.put(copy.id(), copy);           // stary commit zostaje w bazie (można go odzyskać)
                newTip = copy.id();
            }
            branches.put(head, newTip);
            workingTree = commits.get(newTip).content();
            index = workingTree;
            return "Successfully rebased and updated refs/heads/" + head + ".";
        }

        /** git fetch: pobiera commity i zapamiętuje, gdzie stoją gałęzie zdalne. Nie rusza Twoich gałęzi! */
        String fetch() {
            int before = commits.size();
            commits.putAll(origin.commits);
            origin.branches.forEach((branch, id) -> remoteBranches.put("origin/" + branch, id));
            return "pobrano nowych commitów: " + (commits.size() - before);
        }

        /** git push: wysyła commity, ale zdalna gałąź może się przesunąć TYLKO do przodu (fast-forward). */
        String push(String branch) {
            String remoteTip = origin.branches.get(branch);
            String localTip = branches.get(branch);
            if (remoteTip != null && !isAncestor(remoteTip, localTip)) {
                return "! [rejected] " + branch + " -> " + branch + " (non-fast-forward)";   // rejected = odrzucone
            }
            origin.commits.putAll(commits);
            origin.branches.put(branch, localTip);
            remoteBranches.put("origin/" + branch, localTip);
            return branch + " -> " + branch;
        }

        void resetSoft(String id) {                     // git reset --soft: tylko przesuwa gałąź
            branches.put(head, id);
        }

        void resetMixed(String id) {                    // git reset --mixed (domyślny): gałąź + poczekalnia
            resetSoft(id);
            index = commits.get(id).content();
        }

        void resetHard(String id) {                     // git reset --hard: gałąź + poczekalnia + katalog roboczy
            resetMixed(id);
            workingTree = commits.get(id).content();
        }

        void restore() {                                // git restore plik: katalog roboczy ← poczekalnia
            workingTree = index;
        }

        void restoreStaged() {                          // git restore --staged plik: poczekalnia ← ostatni commit
            index = tipContent();
        }

        /** git revert: NOWY commit, który odwraca zmiany wskazanego commita (historia tylko rośnie). */
        String revert(String id) {
            Commit target = commits.get(id);
            String parentContent = commits.get(target.parents().get(0)).content();
            MergeResult r = merge3(target.content(), tipContent(), parentContent, "HEAD", "revert");
            if (r.conflict()) {
                throw new IllegalStateException("w modelu pomijamy konflikty podczas revert");
            }
            index = r.text();
            workingTree = r.text();
            return commit("Revert \"" + target.message() + "\"");
        }

        void stash() {                                  // git stash: schowaj niezatwierdzone zmiany i wyczyść katalog
            stash.push(workingTree);
            workingTree = tipContent();
            index = tipContent();
        }

        void stashPop() {                               // git stash pop: przywróć ostatnio schowane zmiany
            workingTree = stash.pop();
        }

        void tag(String tagName) {                      // git tag nazwa: stała etykieta na bieżącym commicie
            tags.put(tagName, tip());
        }
    }

    // Plik z trzema liniami: do pokazania scalania zmian z różnych linii oraz konfliktów w tej samej linii.
    static final String BASE = "tytuł: Notatki\nkolor: niebieski\nautor: Ala";
    static final String GREEN = "tytuł: Notatki\nkolor: zielony\nautor: Ala";
    static final String GREEN_OLA = "tytuł: Notatki\nkolor: zielony\nautor: Ola";

    /** Repozytorium z trzema commitami na gałęzi main (pokazowe dane do kilku sekcji i ćwiczeń). */
    static MiniRepo sampleRepo() {
        MiniRepo repo = new MiniRepo("lokalne");
        repo.commitAll(BASE, "Dodaj notatki");
        repo.commitAll(GREEN, "Zmień kolor na zielony");
        repo.commitAll(GREEN_OLA, "Zmień autora");
        return repo;
    }

    static String flat(String text) {                   // flat = spłaszcz: nowe linie jako „ | ” (krótszy wydruk)
        return text.replace("\n", " | ");
    }

    static void state(MiniRepo repo) {                  // state = stan trzech obszarów
        note(repo.name + ": katalog=[" + flat(repo.workingTree) + "] poczekalnia=[" + flat(repo.index)
                + "] commit=[" + flat(repo.tipContent()) + "]");
    }

    // =================================================================================================
    // 1. SKRÓT (HASH) — odcisk palca commita
    // =================================================================================================

    /**
     * 1. Każdy commit ma identyfikator liczony ze swojej treści funkcją skrótu (git używa SHA-1/SHA-256).
     * Ten sam tekst daje zawsze ten sam skrót, a zmiana jednej litery — zupełnie inny. Do skrótu wchodzą
     * też rodzice, więc id commita „pamięta” całą historię przed nim.
     */
    static void hashing() {
        section("1. Skrót (hash) = odcisk palca commita");

        show("skrót tekstu 'ala ma kota'", shortHash("ala ma kota"));
        // WYNIK: skrót tekstu 'ala ma kota' → c623e3e
        show("ten sam tekst jeszcze raz", shortHash("ala ma kota"));
        // WYNIK: ten sam tekst jeszcze raz → c623e3e
        show("zmieniona jedna litera ('Ala')", shortHash("Ala ma kota"));
        // WYNIK: zmieniona jedna litera ('Ala') → 124bfb6
        show("pełny skrót SHA-256 ma znaków", sha256("ala ma kota").length());
        // WYNIK: pełny skrót SHA-256 ma znaków → 64

        Commit a = Commit.create(List.of(), "Pierwszy", "treść");
        Commit b = Commit.create(List.of(), "Pierwszy", "treść");
        Commit c = Commit.create(List.of(a.id()), "Pierwszy", "treść");
        show("a i b (to samo, bez rodzica) mają to samo id", a.id().equals(b.id()));
        // WYNIK: a i b (to samo, bez rodzica) mają to samo id → true
        show("c (to samo, ale z rodzicem a) ma to samo id", a.id().equals(c.id()));
        // WYNIK: c (to samo, ale z rodzicem a) ma to samo id → false

        // DOBRA PRAKTYKA: nigdy nie zmieniaj commitów, które już wysłałeś (push) innym. Zmiana choćby jednej
        // wiadomości zmienia id tego commita i wszystkich po nim — koledzy mieliby „inną” historię niż Ty.
        // Dlatego w gicie historię się DOPISUJE (nowy commit, revert), a nie przepisuje.

        // JAK ZACZĄĆ NA WINDOWSIE (jednorazowo):
        //   1. Zainstaluj „Git for Windows” (git-scm.com) — IntelliJ wykryje go sam.
        //   2. Przedstaw się (trafi do każdego Twojego commita):
        //        git config --global user.name  "Imię Nazwisko"
        //        git config --global user.email "twoj@adres.pl"
        //   3. Nowy projekt: w folderze projektu    git init     (albo IntelliJ: VCS → Create Git Repository)
        //      Cudzy projekt:                       git clone https://adres-repozytorium
        //
        // PUŁAPKA: końce linii. Windows zapisuje linie jako CRLF, Linux i Mac jako LF. Git for Windows domyślnie
        // zamienia je tak, by w repozytorium były LF (ustawienie core.autocrlf). Gdy w zespole wszyscy mają to
        // samo ustawienie — problemu nie widać; gdy nie, git pokazuje „zmienione” całe pliki, choć nikt niczego
        // nie zmienił. Zespoły wymuszają jedno zachowanie plikiem .gitattributes w repozytorium.
    }

    // =================================================================================================
    // 2. TRZY OBSZARY: katalog roboczy, poczekalnia, repozytorium
    // =================================================================================================

    /**
     * 2. Zmiana przechodzi trzy etapy: edytujesz plik (katalog roboczy), wybierasz co ma wejść do następnej
     * wersji ({@code git add} → poczekalnia), zatwierdzasz ({@code git commit} → historia). Poczekalnia
     * pozwala zrobić kilka małych, logicznych commitów z jednej sesji pracy.
     */
    static void threeAreas() {
        section("2. Katalog roboczy → poczekalnia → repozytorium");

        MiniRepo repo = new MiniRepo("lokalne");
        state(repo);
        // WYNIK: ℹ lokalne: katalog=[] poczekalnia=[] commit=[]
        repo.edit("wersja 1");
        state(repo);
        // WYNIK: ℹ lokalne: katalog=[wersja 1] poczekalnia=[] commit=[]
        repo.add();
        state(repo);
        // WYNIK: ℹ lokalne: katalog=[wersja 1] poczekalnia=[wersja 1] commit=[]
        repo.commit("Pierwsza wersja");
        state(repo);
        // WYNIK: ℹ lokalne: katalog=[wersja 1] poczekalnia=[wersja 1] commit=[wersja 1]

        // Polecenia, których używasz codziennie:
        //   git status            — co jest zmienione / w poczekalni / nieśledzone (uruchamiaj ZAWSZE przed commitem)
        //   git add plik.txt      — do poczekalni jeden plik        (git add . = wszystko w bieżącym folderze)
        //   git commit -m "Treść" — zatwierdź to, co jest w poczekalni
        //   git log --oneline     — skrócona historia, jeden commit w linii
        //   git diff              — różnice: katalog roboczy vs poczekalnia
        //   git diff --staged     — różnice: poczekalnia vs ostatni commit
        // W IntelliJ: okno Commit (Alt+0) — zaznaczasz pliki lub nawet pojedyncze linie i klikasz Commit.

        repo.edit("wersja 2");
        // PUŁAPKA: commit bez git add nie zatwierdzi zmian z katalogu roboczego. Poczekalnia jest wciąż
        // taka jak ostatni commit, więc nie ma nic do zatwierdzenia (prawdziwy git też odmawia).
        expectThrows("commit bez add", () -> repo.commit("Druga wersja"));
        // WYNIK: ✔ commit bez add → rzucono IllegalStateException: no changes added to commit
        repo.add();
        repo.commit("Druga wersja");
        state(repo);
        // WYNIK: ℹ lokalne: katalog=[wersja 2] poczekalnia=[wersja 2] commit=[wersja 2]

        // DOBRA PRAKTYKA: jeden commit = jedna logiczna zmiana (naprawa błędu, nowa funkcja). Dzięki temu
        // da się ją cofnąć, przejrzeć i opisać jednym zdaniem. Czy możesz opisać commit bez słowa „i”?
    }

    // =================================================================================================
    // 3. HISTORIA: log i diff
    // =================================================================================================

    /**
     * 3. Commity tworzą łańcuch: każdy wskazuje na rodzica. {@code git log} idzie po tym łańcuchu od
     * najnowszego wstecz, a {@code git diff} pokazuje różnice między dwiema wersjami.
     */
    static void history() {
        section("3. Historia: log i diff");

        MiniRepo repo = sampleRepo();
        showEach("git log --oneline", repo.log());
        // WYNIK: git log --oneline (liczba elementów: 3):
        // WYNIK: • 610c5b9 Zmień autora
        // WYNIK: • db9c19d Zmień kolor na zielony
        // WYNIK: • 571018a Dodaj notatki

        List<String> ids = repo.chain();                // od najnowszego: [C3, C2, C1]
        String c1 = ids.get(2);
        String c2 = ids.get(1);
        showEach("git diff (commit 1 → commit 2)", diff(repo.commits.get(c1).content(), repo.commits.get(c2).content()));
        // WYNIK: git diff (commit 1 → commit 2) (liczba elementów: 2):
        // WYNIK: • - kolor: niebieski
        // WYNIK: • + kolor: zielony
        show("rodzic commita 3 to commit 2", repo.commits.get(ids.get(0)).parents().equals(List.of(c2)));
        // WYNIK: rodzic commita 3 to commit 2 → true

        // Przydatne odmiany:
        //   git log --oneline --graph --all   — rysunek gałęzi w terminalu
        //   git show abc1234                  — jeden commit: wiadomość + zmiany
        //   git blame plik.txt                — kto i kiedy zmienił każdą linię (IntelliJ: prawy klik na marginesie → Annotate)
        //   git log -p plik.txt               — historia jednego pliku ze zmianami
        // W IntelliJ: okno Git (Alt+9) → zakładka Log; dwuklik na pliku pokazuje różnice.
        //
        // HEAD~1 = rodzic bieżącego commita, HEAD~2 = dziadek (tylda = „krok wstecz po pierwszym rodzicu”).

        // DOBRA PRAKTYKA: czytaj historię jak książkę. Dobre wiadomości commitów (sekcja 11) sprawiają,
        // że git log opowiada, DLACZEGO projekt wygląda tak, a nie inaczej.
    }

    // =================================================================================================
    // 4. GAŁĘZIE
    // =================================================================================================

    /**
     * 4. Gałąź to etykieta (nazwa → id commita). Nowa gałąź nic nie kopiuje. Commit na bieżącej gałęzi
     * przesuwa tylko jej etykietę; pozostałe gałęzie stoją w miejscu.
     */
    static void branches() {
        section("4. Gałęzie to tylko etykiety");

        MiniRepo repo = sampleRepo();
        repo.createBranch("feature");                   // feature = funkcja (gałąź do nowej funkcji)
        show("gałęzie po 'git branch feature'", repo.branches);
        // WYNIK: gałęzie po 'git branch feature' → {feature=610c5b9, main=610c5b9}
        show("HEAD wskazuje na", repo.head);
        // WYNIK: HEAD wskazuje na → main

        repo.switchTo("feature");
        show("HEAD po 'git switch feature'", repo.head);
        // WYNIK: HEAD po 'git switch feature' → feature
        repo.commitAll("tytuł: Notatki (wersja robocza)\nkolor: zielony\nautor: Ola", "Zmień tytuł");
        show("gałęzie po commicie na feature", repo.branches);
        // WYNIK: gałęzie po commicie na feature → {feature=613c2b1, main=610c5b9}
        show("treść w katalogu na feature", flat(repo.workingTree));
        // WYNIK: treść w katalogu na feature → tytuł: Notatki (wersja robocza) | kolor: zielony | autor: Ola

        repo.switchTo("main");
        show("treść w katalogu po 'git switch main'", flat(repo.workingTree));
        // WYNIK: treść w katalogu po 'git switch main' → tytuł: Notatki | kolor: zielony | autor: Ola

        // Polecenia:
        //   git branch                  — lista gałęzi (gwiazdka przy bieżącej)
        //   git switch -c nazwa         — utwórz gałąź I przełącz się na nią (najczęstsze)
        //   git switch nazwa            — przełącz (starsze polecenie: git checkout nazwa)
        //   git branch -d nazwa         — usuń gałąź po scaleniu (-D = usuń wymuszone, także niescalone)
        // W IntelliJ: widżet gałęzi na górze okna (lub Alt+` czyli VCS Operations) → New Branch / Checkout.
        //
        // PUŁAPKA: przełączając gałąź, zmieniasz pliki w katalogu roboczym na wersję tej gałęzi. Git odmówi
        // przełączenia, jeśli miałbyś stracić niezatwierdzone zmiany — schowaj je (git stash, sekcja 11)
        // albo zatwierdź. Nie „naprawiaj” tego przez reset --hard, bo stracisz pracę.

        // DOBRA PRAKTYKA: jedna gałąź = jedno zadanie, nazwa mówi o co chodzi (feature/koszyk, fix/blad-vat).
        // Gałąź main trzymaj zawsze w stanie, który się kompiluje — praca idzie na gałęziach.
    }

    // =================================================================================================
    // 5. SCALANIE I: fast-forward
    // =================================================================================================

    /**
     * 5. Jeśli bieżąca gałąź jest PRZODKIEM scalanej (nikt nie dopisał do niej nic nowego), git nie musi
     * łączyć niczego — przesuwa etykietę do przodu. To „fast-forward” (przewinięcie do przodu).
     */
    static void fastForward() {
        section("5. Scalanie: fast-forward");

        MiniRepo repo = sampleRepo();
        repo.createBranch("feature");
        repo.switchTo("feature");
        repo.commitAll("tytuł: Notatki (wersja robocza)\nkolor: zielony\nautor: Ola", "Zmień tytuł");
        repo.switchTo("main");

        String mainId = repo.branches.get("main");
        String featureId = repo.branches.get("feature");
        show("czy main jest przodkiem feature?", repo.isAncestor(mainId, featureId));
        // WYNIK: czy main jest przodkiem feature? → true
        int before = repo.commits.size();
        show("git merge feature", repo.merge("feature"));
        // WYNIK: git merge feature → Fast-forward
        show("main wskazuje teraz na ten sam commit co feature", repo.branches.get("main").equals(featureId));
        // WYNIK: main wskazuje teraz na ten sam commit co feature → true
        show("ile NOWYCH commitów powstało", repo.commits.size() - before);
        // WYNIK: ile NOWYCH commitów powstało → 0
        show("git merge feature (drugi raz)", repo.merge("feature"));
        // WYNIK: git merge feature (drugi raz) → Already up to date.

        // Polecenia:
        //   git switch main
        //   git merge feature             — fast-forward, jeśli się da; inaczej commit scalający (sekcja 6)
        //   git merge --no-ff feature     — ZAWSZE zrób commit scalający (zostaje ślad, że była gałąź)
        //   git merge --ff-only feature   — scal tylko, gdy to fast-forward (inaczej błąd) — bezpieczne
        //
        // DOBRA PRAKTYKA: ustaw w zespole jedną politykę (fast-forward czy zawsze commit scalający),
        // bo wpływa na wygląd historii — liniowa jest czytelniejsza, z commitami scalającymi bardziej „wierna”.
    }

    // =================================================================================================
    // 6. SCALANIE II: commit scalający (merge commit)
    // =================================================================================================

    /**
     * 6. Gdy obie gałęzie dostały nowe commity (rozeszły się), git szuka WSPÓLNEGO PRZODKA (merge base) i robi
     * scalanie trójstronne: porównuje bazę z oboma końcami. Zmiany w RÓŻNYCH liniach łączy sam.
     * Powstaje commit scalający z dwoma rodzicami.
     */
    static void mergeCommit() {
        section("6. Scalanie: commit scalający z dwoma rodzicami");

        MiniRepo repo = sampleRepo();
        repo.createBranch("feature");
        repo.switchTo("feature");
        repo.commitAll("tytuł: Notatki (wersja robocza)\nkolor: zielony\nautor: Ola", "Zmień tytuł");
        repo.switchTo("main");
        repo.commitAll("tytuł: Notatki\nkolor: zielony\nautor: Ewa", "Zmień autora na Ewę");

        String base = repo.mergeBase(repo.resolve("main"), repo.resolve("feature"));
        show("wspólny przodek (merge base)", repo.describe(base));
        // WYNIK: wspólny przodek (merge base) → 610c5b9 Zmień autora
        show("git merge feature", repo.merge("feature"));
        // WYNIK: git merge feature → Merge made by the 'ort' strategy.
        show("treść po scaleniu", flat(repo.workingTree));
        // WYNIK: treść po scaleniu → tytuł: Notatki (wersja robocza) | kolor: zielony | autor: Ewa
        show("liczba rodziców commita scalającego", repo.commits.get(repo.tip()).parents().size());
        // WYNIK: liczba rodziców commita scalającego → 2
        show("wiadomość commita scalającego", repo.commits.get(repo.tip()).message());
        // WYNIK: wiadomość commita scalającego → Merge branch 'feature'

        // Zmiana tytułu (feature) i autora (main) dotyczyły RÓŻNYCH linii, więc scalenie poszło samo.
        // Prawdziwy git robi to samo na poziomie całych plików i linii wewnątrz plików.
        // W IntelliJ: prawy klik na gałęzi w widżecie → „Merge into Current”.
    }

    // =================================================================================================
    // 7. KONFLIKTY
    // =================================================================================================

    /**
     * 7. Konflikt powstaje, gdy obie strony zmieniły TĘ SAMĄ linię w różny sposób. Git nie zgaduje —
     * wstawia do pliku znaczniki i czeka, aż Ty zdecydujesz. To normalna sytuacja, a nie awaria.
     */
    static void conflict() {
        section("7. Konflikt: obie strony zmieniły tę samą linię");

        MiniRepo repo = sampleRepo();
        repo.createBranch("feature");
        repo.switchTo("feature");
        repo.commitAll("tytuł: Notatki\nkolor: żółty\nautor: Ola", "Zmień kolor na żółty");
        repo.switchTo("main");
        repo.commitAll("tytuł: Notatki\nkolor: czerwony\nautor: Ola", "Zmień kolor na czerwony");

        show("git merge feature", repo.merge("feature"));
        // WYNIK: git merge feature → CONFLICT (content): Merge conflict in notatki.txt
        showEach("plik w katalogu roboczym", List.of(repo.workingTree.split("\n")));
        // WYNIK: plik w katalogu roboczym (liczba elementów: 7):
        // WYNIK: • tytuł: Notatki
        // WYNIK: • <<<<<<< HEAD
        // WYNIK: • kolor: czerwony
        // WYNIK: • =======
        // WYNIK: • kolor: żółty
        // WYNIK: • >>>>>>> feature
        // WYNIK: • autor: Ola
        show("scalanie trwa (git status pokaże: You have unmerged paths)", repo.mergeHead != null);
        // WYNIK: scalanie trwa (git status pokaże: You have unmerged paths) → true

        // ROZWIĄZANIE: otwórz plik, zostaw jedną wersję (albo połącz obie), USUŃ wszystkie trzy znaczniki
        // (linie z siedmioma znakami < = >), potem git add plik i git commit.
        repo.edit("tytuł: Notatki\nkolor: pomarańczowy\nautor: Ola");
        repo.add();
        repo.commit("Scal feature (kolor: kompromis)");
        show("liczba rodziców po rozwiązaniu", repo.commits.get(repo.tip()).parents().size());
        // WYNIK: liczba rodziców po rozwiązaniu → 2
        show("scalanie trwa?", repo.mergeHead != null);
        // WYNIK: scalanie trwa? → false
        show("w pliku zostały znaczniki?", repo.tipContent().contains("<<<<<<<"));
        // WYNIK: w pliku zostały znaczniki? → false

        // PUŁAPKA: git NIE sprawdza, czy usunąłeś znaczniki. Możesz zatwierdzić plik z liniami „<<<<<<< HEAD”
        // i zepsuć projekt (kod się nie skompiluje). Przed commitem wyszukaj w projekcie „<<<<<<<”
        // (IntelliJ: Ctrl+Shift+F). IntelliJ ułatwia to oknem „Merge”: trzy kolumny — Twoja wersja
        // (lewa), wynik (środek), cudza wersja (prawa); strzałki >> i << przenoszą zmiany, a różdżka
        // („Apply non-conflicting changes”) przenosi te, które się nie kłócą.
        //
        // Przerwanie scalania, gdy się zgubisz:   git merge --abort   (wraca do stanu sprzed scalania)
        //
        // DOBRA PRAKTYKA: konfliktów jest mniej, gdy (1) często pobierasz zmiany z main, (2) gałęzie żyją
        // krótko, (3) commity są małe, (4) ustalacie, kto zmienia które pliki. Przed rozwiązaniem konfliktu
        // zrozum OBIE zmiany — bywa, że poprawne jest połączenie, a nie wybór jednej strony.
    }

    // =================================================================================================
    // 8. REBASE
    // =================================================================================================

    /**
     * 8. Rebase „przeszczepia” Twoje commity na nową bazę: odtwarza je jeden po drugim na końcu innej
     * gałęzi. Efekt to liniowa historia bez commitu scalającego. Cena: odtworzone commity to NOWE commity
     * (inne id), bo mają innego rodzica.
     */
    static void rebase() {
        section("8. Rebase: te same zmiany, nowe commity");

        MiniRepo repo = new MiniRepo("lokalne");
        repo.commitAll(BASE, "Dodaj notatki");
        repo.commitAll(GREEN, "Zmień kolor na zielony");
        repo.createBranch("feature");
        repo.switchTo("feature");
        String oldId = repo.commitAll("tytuł: Notatki (wersja robocza)\nkolor: zielony\nautor: Ala", "Zmień tytuł");
        repo.switchTo("main");
        repo.commitAll(GREEN_OLA, "Zmień autora");
        repo.switchTo("feature");

        show("feature: baza wspólna z main", repo.describe(repo.mergeBase(repo.resolve("feature"), repo.resolve("main"))));
        // WYNIK: feature: baza wspólna z main → db9c19d Zmień kolor na zielony
        show("git rebase main", repo.rebase("main"));
        // WYNIK: git rebase main → Successfully rebased and updated refs/heads/feature.
        showEach("git log --oneline po rebase", repo.log());
        // WYNIK: git log --oneline po rebase (liczba elementów: 4):
        // WYNIK: • 613c2b1 Zmień tytuł
        // WYNIK: • 610c5b9 Zmień autora
        // WYNIK: • db9c19d Zmień kolor na zielony
        // WYNIK: • 571018a Dodaj notatki
        show("treść po rebase", flat(repo.workingTree));
        // WYNIK: treść po rebase → tytuł: Notatki (wersja robocza) | kolor: zielony | autor: Ola
        show("commit 'Zmień tytuł' ma to samo id co przed rebase", repo.tip().equals(oldId));
        // WYNIK: commit 'Zmień tytuł' ma to samo id co przed rebase → false
        show("a stary commit nadal jest w bazie (git reflog go pokaże)", repo.commits.containsKey(oldId));
        // WYNIK: a stary commit nadal jest w bazie (git reflog go pokaże) → true

        // Porównanie z merge:
        //   merge  — zachowuje prawdziwą historię, dodaje commit scalający, bezpieczny
        //   rebase — historia jak prosta linia, ale przepisuje commity
        // Polecenia:   git switch feature   →   git rebase main   →   (konflikty: popraw, git add, git rebase --continue)
        //              git rebase --abort   — wróć do stanu sprzed rebase
        //
        // PUŁAPKA: NIGDY nie rób rebase commitów, które ktoś już pobrał (wypchnięte na wspólną gałąź).
        // Ty masz „nowe” commity, koledzy mają „stare” — ich historia się rozjedzie, a git zacznie
        // pokazywać zdublowane zmiany i konflikty. Po rebase wypchniętej gałęzi push zostanie odrzucony
        // i trzeba by użyć wymuszenia (--force-with-lease), które nadpisuje cudzą pracę, jeśli ktoś coś dopisał.
        //
        // DOBRA PRAKTYKA: rebase to narzędzie do porządkowania SWOJEJ, jeszcze niewysłanej gałęzi
        // (np. git rebase main przed pull requestem). Na wspólnych gałęziach używaj merge albo revert.
    }

    // =================================================================================================
    // 9. ZDALNE REPOZYTORIUM
    // =================================================================================================

    /**
     * 9. Zdalne repozytorium (GitHub, GitLab) to kolejna kopia historii. {@code clone} kopiuje całość,
     * {@code push} wysyła Twoje commity, {@code fetch} pobiera cudze (bez ruszania Twoich gałęzi),
     * {@code pull} to fetch + merge. Push jest odrzucany, jeśli nie byłby fast-forward.
     */
    static void remote() {
        section("9. Zdalne repozytorium: clone, push, fetch, pull");

        MiniRepo origin = new MiniRepo("origin");
        origin.commitAll(BASE, "Dodaj notatki");
        MiniRepo ala = MiniRepo.clone("ala", origin);   // git clone
        MiniRepo ola = MiniRepo.clone("ola", origin);

        show("klon ma te same commity co origin", ala.tip().equals(origin.tip()));
        // WYNIK: klon ma te same commity co origin → true
        ala.commitAll(GREEN, "Zmień kolor na zielony");
        show("ala: git push", ala.push("main"));
        // WYNIK: ala: git push → main -> main
        ola.commitAll("tytuł: Notatki\nkolor: niebieski\nautor: Ola", "Zmień autora");
        show("ola: git push", ola.push("main"));
        // WYNIK: ola: git push → ! [rejected] main -> main (non-fast-forward)
        show("ola: git fetch", ola.fetch());
        // WYNIK: ola: git fetch → pobrano nowych commitów: 1
        show("ola: czy main jest przodkiem origin/main?", ola.isAncestor(ola.tip(), ola.resolve("origin/main")));
        // WYNIK: ola: czy main jest przodkiem origin/main? → false
        show("ola: git merge origin/main (czyli pull)", ola.merge("origin/main"));
        // WYNIK: ola: git merge origin/main (czyli pull) → Merge made by the 'ort' strategy.
        show("ola: git push po scaleniu", ola.push("main"));
        // WYNIK: ola: git push po scaleniu → main -> main
        show("treść na origin", flat(origin.commits.get(origin.tip()).content()));
        // WYNIK: treść na origin → tytuł: Notatki | kolor: zielony | autor: Ola

        // Polecenia:
        //   git remote -v                     — jakie zdalne repozytoria znam (domyślnie: origin)
        //   git push -u origin feature        — wyślij nową gałąź i zapamiętaj powiązanie (-u = upstream)
        //   git fetch                         — pobierz, nic nie zmieniaj w moich gałęziach
        //   git pull                          — fetch + merge   (git pull --rebase = fetch + rebase)
        //   W IntelliJ: Update Project Ctrl+T, Commit Ctrl+K, Push Ctrl+Shift+K.
        //
        // PUŁAPKA: „rejected (non-fast-forward)” nie znaczy awarii. Ktoś wypchnął coś, czego nie masz.
        // Pobierz (fetch/pull), scal, wypchnij ponownie. NIE używaj push --force „na szybko” — nadpiszesz
        // cudzą pracę. (--force-with-lease jest ostrożniejsze, ale też tylko na własnych gałęziach.)
        //
        // PULL REQUEST (PR) na GitHubie — przepływ pracy w zespole:
        //   1. git switch -c feature/koszyk          — nowa gałąź od aktualnego main
        //   2. kilka małych commitów                 — z dobrymi wiadomościami
        //   3. git push -u origin feature/koszyk     — wyślij gałąź
        //   4. na GitHubie: „Compare and pull request” (porównaj i otwórz prośbę o scalenie) → opis, co i dlaczego
        //   5. przegląd kodu (review): koledzy komentują, Ty poprawiasz kolejnymi commitami na tej samej gałęzi
        //   6. testy automatyczne przechodzą → „Merge pull request” (scalenie do main)
        //   7. git switch main  →  git pull  →  git branch -d feature/koszyk
        // To przepływ używany w tym kursie: jeden dział = jedna gałąź = jeden PR.
        //
        // DOBRA PRAKTYKA: nigdy nie wpisuj haseł ani kluczy do kodu — raz wypchnięty sekret zostaje w historii
        // na zawsze (usunięcie pliku go nie kasuje). Do GitHuba loguj się tokenem lub przez przeglądarkę.
    }

    // =================================================================================================
    // 10. COFANIE ZMIAN
    // =================================================================================================

    /** Pomocnik: dwa commity jednoliniowego pliku; zwraca repozytorium (id commita 1 = rodzic czubka). */
    static MiniRepo twoCommits(String name) {
        MiniRepo repo = new MiniRepo(name);
        repo.commitAll("wersja 1", "Wersja 1");
        repo.commitAll("wersja 2", "Wersja 2");
        return repo;
    }

    /**
     * 10. Cofanie zależy od tego, GDZIE jest zmiana. Niezatwierdzona: {@code restore}. Zatwierdzona, ale nie
     * wysłana: {@code reset}. Już wysłana innym: {@code revert} (nowy commit odwracający zmianę).
     */
    static void undo() {
        section("10. Cofanie zmian: restore, reset, revert");

        // restore — cofanie niezatwierdzonych zmian w pliku
        MiniRepo r = twoCommits("restore");
        r.edit("śmieci wpisane przez pomyłkę");
        state(r);
        // WYNIK: ℹ restore: katalog=[śmieci wpisane przez pomyłkę] poczekalnia=[wersja 2] commit=[wersja 2]
        r.restore();                                    // git restore plik
        state(r);
        // WYNIK: ℹ restore: katalog=[wersja 2] poczekalnia=[wersja 2] commit=[wersja 2]

        // reset — przesunięcie gałęzi wstecz; trzy tryby różnią się tym, co dzieje się z poczekalnią i katalogiem
        MiniRepo soft = twoCommits("soft");
        String c1 = soft.commits.get(soft.tip()).parents().get(0);
        String c2 = soft.tip();
        soft.resetSoft(c1);                             // git reset --soft HEAD~1
        state(soft);
        // WYNIK: ℹ soft: katalog=[wersja 2] poczekalnia=[wersja 2] commit=[wersja 1]
        MiniRepo mixed = twoCommits("mixed");
        mixed.resetMixed(c1);                           // git reset --mixed HEAD~1 (to samo co: git reset HEAD~1)
        state(mixed);
        // WYNIK: ℹ mixed: katalog=[wersja 2] poczekalnia=[wersja 1] commit=[wersja 1]
        MiniRepo hard = twoCommits("hard");
        hard.resetHard(c1);                             // git reset --hard HEAD~1  UWAGA: kasuje zmiany!
        state(hard);
        // WYNIK: ℹ hard: katalog=[wersja 1] poczekalnia=[wersja 1] commit=[wersja 1]
        show("po reset --hard commit 2 wciąż jest w bazie", hard.commits.containsKey(c2));
        // WYNIK: po reset --hard commit 2 wciąż jest w bazie → true
        hard.resetHard(c2);                             // ratunek: wróć na stary id (znajdziesz go przez git reflog)
        state(hard);
        // WYNIK: ℹ hard: katalog=[wersja 2] poczekalnia=[wersja 2] commit=[wersja 2]

        // Tabela: co przesuwa który tryb reset
        //                    gałąź (HEAD)   poczekalnia   katalog roboczy
        //   reset --soft         TAK            nie            nie        zmiany zostają „przygotowane do commita”
        //   reset --mixed        TAK            TAK            nie        zmiany zostają w plikach, bez add (domyślny)
        //   reset --hard         TAK            TAK            TAK        zmiany znikają (niezatwierdzone — bezpowrotnie!)
        //
        // PUŁAPKA: reset --hard kasuje niezatwierdzone zmiany BEZPOWROTNIE (git ich nie ma w żadnym commicie).
        // Zatwierdzone commity da się jeszcze odzyskać z git reflog (dziennik ruchów HEAD) przez kilka tygodni.
        // Zasada: przed reset --hard zrób git status, a gdy się wahasz — git stash albo kopia gałęzi (git branch backup).
        //
        // PUŁAPKA: reset przepisuje historię. Na commitach już wysłanych (push) NIE używaj — użyj revert.

        // revert — nowy commit odwracający zmiany wskazanego commita; historia tylko rośnie
        MiniRepo rv = sampleRepo();
        String colorCommit = rv.chain().get(1);         // „Zmień kolor na zielony”
        rv.revert(colorCommit);                         // git revert abc1234
        showEach("git log --oneline po revert", rv.log());
        // WYNIK: git log --oneline po revert (liczba elementów: 4):
        // WYNIK: • 784dd0d Revert "Zmień kolor na zielony"
        // WYNIK: • 610c5b9 Zmień autora
        // WYNIK: • db9c19d Zmień kolor na zielony
        // WYNIK: • 571018a Dodaj notatki
        show("treść po revert (kolor wrócił, autor został)", flat(rv.workingTree));
        // WYNIK: treść po revert (kolor wrócił, autor został) → tytuł: Notatki | kolor: niebieski | autor: Ola

        // DOBRA PRAKTYKA: revert jest bezpieczny dla wspólnych gałęzi, bo niczego nie usuwa — dopisuje.
        // Dostajesz przy tym czytelny ślad „cofnięto, bo ...” w historii.
        //
        // IntelliJ: prawy klik na commicie w oknie Git → Log: „Revert Commit”, „Reset Current Branch to Here”
        // (wybierasz Soft/Mixed/Hard), „Cherry-Pick”. Zmiany w oknie Commit możesz cofnąć: Rollback (Ctrl+Alt+Z).
        // Dodatkowa siatka bezpieczeństwa IntelliJ: Local History (prawy klik na pliku → Local History →
        // Show History) — zapisuje wersje także tych zmian, których nigdy nie zatwierdziłeś.
    }

    // =================================================================================================
    // 11. NA CO DZIEŃ: .gitignore, wiadomości commitów, schowek, tagi
    // =================================================================================================

    /** Sprawdza wiadomość commita wg popularnych zasad; zwraca listę problemów (pusta = dobra). */
    static List<String> commitMessageProblems(String message) {
        List<String> problems = new ArrayList<>();
        String[] lines = message.split("\n");
        String subject = lines[0];                      // subject = temat (pierwsza linia)
        if (subject.isBlank()) {                        // isBlank (Java 11+) = czy pusty lub same spacje
            problems.add("pusty temat");
        }
        if (subject.length() > 50) {
            problems.add("temat dłuższy niż 50 znaków");
        }
        if (subject.endsWith(".")) {
            problems.add("temat kończy się kropką");
        }
        if (lines.length > 1 && !lines[1].isBlank()) {
            problems.add("brak pustej linii po temacie");
        }
        return problems;
    }

    /**
     * 11. Drobiazgi, bez których praca z gitem boli: plik {@code .gitignore} (czego nie śledzić),
     * dobre wiadomości commitów, schowek na niedokończone zmiany i tagi na wydania.
     */
    static void everyday() {
        section("11. Na co dzień: .gitignore, wiadomości, schowek, tagi");

        // --- .gitignore: lista wzorców plików, których git ma nie śledzić (build, logi, sekrety, pliki IDE).
        // Tu wzorce „glob” sprawdzamy prawdziwym API JDK (PathMatcher = dopasowywacz ścieżek).
        PathMatcher ignored = FileSystems.getDefault().getPathMatcher("glob:*.{class,log}");
        for (String file : List.of("Main.class", "Main.java", "app.log", "notatki.txt")) {
            show(file, ignored.matches(Path.of(file)) ? "ignorowany" : "śledzony");
        }
        // WYNIK: Main.class → ignorowany
        // WYNIK: Main.java → śledzony
        // WYNIK: app.log → ignorowany
        // WYNIK: notatki.txt → śledzony
        // Typowy plik .gitignore dla naszego projektu (jedna reguła w linii):
        //   *.class          — skompilowane klasy
        //   out/             — katalog wyjściowy IntelliJ
        //   target/          — katalog Mavena
        //   .idea/           — ustawienia IDE (prywatne)
        //   *.log            — logi
        //   temp/            — pliki robocze
        //   !ważny.log       — wykrzyknik = wyjątek: ten plik jednak śledź
        // (Prawdziwe wzorce .gitignore są bogatsze niż glob JDK — mają np. dopasowanie do katalogów i „**”.)
        //
        // PUŁAPKA: .gitignore działa tylko dla plików JESZCZE NIEśledzonych. Jeśli plik już zatwierdziłeś,
        // dopisanie go do .gitignore nic nie zmieni — trzeba go wyjąć z repozytorium:
        //   git rm --cached plik        (zostaje na dysku, znika z gita)
        // Dotyczy to zwłaszcza haseł i kluczy: po wypchnięciu zostają w historii, więc je trzeba UNIEWAŻNIĆ
        // (zmienić hasło), a nie tylko usunąć z kodu.

        // --- Dobra wiadomość commita: krótki temat (do ok. 50 znaków), pusta linia, potem opis „dlaczego”.
        String longOne = "Zmień kolor tła przycisku logowania na zielony i dodaj animację najechania";
        List<String> messages = List.of("Dodaj walidację adresu e-mail", "poprawki.", longOne, "Napraw błąd VAT\nOpis bez pustej linii");
        for (String message : messages) {
            show(message.length() > 30 ? message.substring(0, 30).replace("\n", " ") + "…" : message, commitMessageProblems(message));
        }
        // WYNIK: Dodaj walidację adresu e-mail → []
        // WYNIK: poprawki. → [temat kończy się kropką]
        // WYNIK: Zmień kolor tła przycisku logo… → [temat dłuższy niż 50 znaków]
        // WYNIK: Napraw błąd VAT Opis bez puste… → [brak pustej linii po temacie]
        // Zasady: tryb rozkazujący („Dodaj”, „Napraw”, nie „Dodałem”), temat bez kropki, w treści: CO i DLACZEGO
        // (CO widać w diffie). Dobry przykład:   Napraw zaokrąglanie VAT w koszyku
        //                                        (pozycje były zaokrąglane osobno, przez co suma różniła się o 1 gr)
        // PUŁAPKA: wiadomości typu „poprawki”, „zmiany”, „asdf” — za pół roku nikt (z Tobą włącznie) nie
        // będzie wiedział, czemu ten commit powstał, a git bisect i git blame tracą sens.

        // --- Schowek (stash): odłóż niedokończone zmiany, żeby przełączyć gałąź, i wróć do nich później.
        MiniRepo repo = twoCommits("schowek");
        repo.edit("wersja 3 (w trakcie pracy)");
        repo.stash();                                   // git stash
        state(repo);
        // WYNIK: ℹ schowek: katalog=[wersja 2] poczekalnia=[wersja 2] commit=[wersja 2]
        repo.stashPop();                                // git stash pop
        state(repo);
        // WYNIK: ℹ schowek: katalog=[wersja 3 (w trakcie pracy)] poczekalnia=[wersja 2] commit=[wersja 2]
        // Polecenia: git stash / git stash list / git stash pop (przywróć i usuń ze schowka) / git stash apply (przywróć, zostaw).
        // IntelliJ ma własny odpowiednik: Shelf (półka) i opcję „Stash Changes” w menu Git.

        // --- Tagi: stała, czytelna etykieta na commicie — zwykle numer wydania.
        repo.tag("v1.0");                               // git tag v1.0   (git push --tags wysyła tagi)
        show("tagi", repo.tags.keySet());
        // WYNIK: tagi → [v1.0]
        show("tag v1.0 wskazuje na czubek main", repo.tags.get("v1.0").equals(repo.tip()));
        // WYNIK: tag v1.0 wskazuje na czubek main → true
        // W odróżnieniu od gałęzi tag NIE przesuwa się przy kolejnych commitach.

        // --- Tabela: IntelliJ a terminal
        //   git status / diff     → okno Commit (Alt+0), podgląd różnic Ctrl+D
        //   git add + commit      → zaznacz pliki, wpisz wiadomość, Commit (Ctrl+K)
        //   git push              → Push (Ctrl+Shift+K)
        //   git pull / fetch      → Update Project (Ctrl+T), menu Git → Fetch
        //   git log / blame       → okno Git (Alt+9) → Log; prawy klik na marginesie → Annotate
        //   git switch / branch   → widżet gałęzi na górze okna
        //   git merge / rebase    → prawy klik na gałęzi → Merge into Current / Rebase Current onto Selected
        //   rozwiązanie konfliktu → okno Merge (lewa: Twoje, prawa: cudze, środek: wynik)
        //   git stash             → menu Git → Stash Changes (lub Shelf)
        // DOBRA PRAKTYKA: ucz się poleceń w terminalu (zakładka Terminal w IntelliJ) — okna IDE są wygodne,
        // ale komunikaty błędów i dokumentacja zawsze mówią językiem poleceń.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Trzy obszary: katalog roboczy → (git add) poczekalnia → (git commit) repozytorium. Zawsze zaczynaj od git status.
     *   • Commit = migawka + wiadomość + rodzic; id to skrót z treści i rodziców — zmiana historii zmienia id.
     *   • Gałąź = etykieta na commicie; HEAD = bieżąca pozycja. git switch -c nazwa tworzy i przełącza.
     *   • merge: fast-forward (tylko przesunięcie etykiety) albo commit scalający z dwoma rodzicami.
     *   • Konflikt = ta sama linia zmieniona po obu stronach; popraw plik, usuń znaczniki, git add, git commit.
     *   • rebase przepisuje commity (nowe id) — nigdy na gałęziach, które ktoś pobrał.
     *   • fetch pobiera, pull = fetch + merge, push wysyła; „non-fast-forward” = najpierw pobierz i scal.
     *   • Cofanie: restore (niezatwierdzone), reset (tylko lokalne: soft/mixed/hard — hard kasuje!), revert (wspólne).
     *   • .gitignore nie działa na pliki już śledzone (git rm --cached). Nigdy nie zatwierdzaj haseł.
     *   • Wiadomość: krótki temat w trybie rozkazującym, bez kropki; w treści DLACZEGO.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym jest gałąź w gicie i dlaczego jej utworzenie jest błyskawiczne?
     *   2. Czym różni się git fetch od git pull?
     *   3. Co wypisze:  MiniRepo repo = sampleRepo();  System.out.println(repo.merge("main"));  ?
     *   4. Co wypisze:  System.out.println(shortHash("x").length());  ?
     *   5. ZNAJDŹ BŁĄD:  Po rebase gałęzi feature, którą koledzy już pobrali, wykonujesz git push --force,
     *      bo zwykły push został odrzucony. Dlaczego to zły pomysł i co zrobić zamiast tego?
     *   6. ZNAJDŹ BŁĄD:  Dopisałeś plik hasla.txt do .gitignore, ale git status nadal go pokazuje jako
     *      zmieniony. Dlaczego? Co trzeba zrobić?
     *   7. Który tryb reset zostawia zmiany „gotowe do commita” w poczekalni, a który je kasuje?
     *   8. Kiedy użyjesz revert, a kiedy reset?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: liczba commitów gałęzi main", 3, () -> exercise1(sampleRepo(), "main"));
        Check.equal("ćw. 2: rodzaj scalenia (fast-forward)", "FAST-FORWARD", () -> exercise2(forwardRepo(), "feature"));
        Check.equal("ćw. 2: rodzaj scalenia (commit scalający)", "MERGE-COMMIT", () -> exercise2(divergedRepo(), "feature"));
        Check.equal("ćw. 3: wybierz wersję przychodzącą", "a\nTHEIRS\nb", () -> exercise3("a\n<<<<<<< HEAD\nOURS\n=======\nTHEIRS\n>>>>>>> feature\nb"));
        Check.equal("ćw. 4: bezpieczne cofnięcie", "tytuł: Notatki | kolor: niebieski | autor: Ola|4", () -> exercise4(sampleRepo()));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 3, () -> solution1(sampleRepo(), "main"));
        Check.equal("ćw. 2 (wzorzec: fast-forward)", "FAST-FORWARD", () -> solution2(forwardRepo(), "feature"));
        Check.equal("ćw. 2 (wzorzec: commit scalający)", "MERGE-COMMIT", () -> solution2(divergedRepo(), "feature"));
        Check.equal("ćw. 3 (wzorzec)", "a\nTHEIRS\nb", () -> solution3("a\n<<<<<<< HEAD\nOURS\n=======\nTHEIRS\n>>>>>>> feature\nb"));
        Check.equal("ćw. 4 (wzorzec)", "tytuł: Notatki | kolor: niebieski | autor: Ola|4", () -> solution4(sampleRepo()));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /** Dane do ćwiczenia 2: main jest przodkiem feature (da się przewinąć do przodu). */
    static MiniRepo forwardRepo() {
        MiniRepo repo = sampleRepo();
        repo.createBranch("feature");
        repo.switchTo("feature");
        repo.commitAll("tytuł: Nowy\nkolor: zielony\nautor: Ola", "Zmień tytuł");
        repo.switchTo("main");
        return repo;
    }

    /** Dane do ćwiczenia 2: obie gałęzie mają własne commity. */
    static MiniRepo divergedRepo() {
        MiniRepo repo = forwardRepo();
        repo.commitAll("tytuł: Notatki\nkolor: zielony\nautor: Ewa", "Zmień autora na Ewę");
        return repo;
    }

    /**
     * ĆWICZENIE 1 (łatwe): policz commity dostępne z czubka podanej gałęzi (liczba wszystkich przodków
     * łącznie z samym czubkiem). Przełącz się na gałąź przez repo.switchTo(branch).
     * Podpowiedź: repo.chain() daje id commitów od czubka do korzenia.
     */
    static int exercise1(MiniRepo repo, String branch) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): bez wykonywania scalenia powiedz, co zrobiłby git merge other na bieżącej gałęzi:
     * "UP-TO-DATE" (nic do zrobienia), "FAST-FORWARD" albo "MERGE-COMMIT".
     * Podpowiedź: repo.resolve(nazwa) daje id commita, repo.isAncestor(a, b) mówi, czy a jest przodkiem b.
     */
    static String exercise2(MiniRepo repo, String other) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): dostajesz tekst z jednym konfliktem w formacie gita (linie "<<<<<<< ...",
     * "=======", ">>>>>>> ..."). Zwróć tekst, w którym konflikt rozwiązano wybierając wersję PRZYCHODZĄCĄ
     * (po "=======", jak „Accept Theirs”), bez znaczników. Linie rozdziel znakiem nowej linii.
     * Podpowiedź: przejdź po liniach ze stanem „w której części konfliktu jestem”.
     */
    static String exercise3(String conflicted) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): PRZEPISZ niebezpieczny sposób na bezpieczny.
     * <pre>{@code
     * // PRZED (przepisuje historię, kasuje OBA ostatnie commity, nie wolno na wspólnych gałęziach):
     * //   git reset --hard HEAD~2
     * // PO (dopisuje commit odwracający, drugi commit zostaje): git revert HEAD~1
     * }</pre>
     * Na repozytorium sampleRepo() cofnij commit „Zmień kolor na zielony” przez repo.revert(...) i zwróć napis
     * "treść spłaszczona funkcją flat" + "|" + liczba commitów na gałęzi (po revert powinno ich być o jeden więcej).
     * Podpowiedź: id commita „Zmień kolor...” to repo.chain().get(1).
     */
    static String exercise4(MiniRepo repo) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(MiniRepo repo, String branch) {
        repo.switchTo(branch);
        return repo.chain().size();
    }

    static String solution2(MiniRepo repo, String other) {
        String mine = repo.tip();
        String theirs = repo.resolve(other);
        if (repo.isAncestor(theirs, mine)) {
            return "UP-TO-DATE";
        }
        return repo.isAncestor(mine, theirs) ? "FAST-FORWARD" : "MERGE-COMMIT";
    }

    static String solution3(String conflicted) {
        List<String> result = new ArrayList<>();
        boolean inOurs = false;                         // jesteśmy w części „nasza” (między <<<<<<< a =======)
        boolean inTheirs = false;                       // jesteśmy w części „ich” (między ======= a >>>>>>>)
        for (String line : conflicted.split("\n")) {
            if (line.startsWith("<<<<<<<")) {
                inOurs = true;
            } else if (line.startsWith("=======") && inOurs) {
                inOurs = false;
                inTheirs = true;
            } else if (line.startsWith(">>>>>>>") && inTheirs) {
                inTheirs = false;
            } else if (!inOurs) {                       // nasze linie pomijamy, resztę i „ich” zostawiamy
                result.add(line);
            }
        }
        return String.join("\n", result);
    }

    static String solution4(MiniRepo repo) {
        repo.revert(repo.chain().get(1));
        return flat(repo.workingTree) + "|" + repo.chain().size();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Gałąź to tylko nazwa wskazująca na jeden commit (etykieta). Tworzenie jej to zapisanie jednego id,
     *      bez kopiowania plików — dlatego trwa ułamek sekundy.
     *   2. fetch pobiera nowe commity i aktualizuje gałęzie zdalne (origin/main), ale NIE rusza Twoich gałęzi
     *      ani plików. pull to fetch + merge (lub rebase) — od razu zmienia Twoją bieżącą gałąź.
     *   3. Already up to date.   (main jest bieżącą gałęzią, więc „scala” samą siebie — nic do zrobienia)
     *   4. 7   (skrót skrócony do siedmiu znaków)
     *   5. Rebase tworzy NOWE commity, więc historia kolegów (stare id) i Twoja (nowe id) się rozjeżdżają.
     *      Wymuszony push nadpisze zdalną gałąź i może skasować cudzą pracę. Zamiast tego: nie rób rebase
     *      wypchniętych gałęzi — scal (merge) albo, jeśli musisz, ustal to z zespołem i użyj --force-with-lease.
     *   6. .gitignore nie działa na pliki, które git już śledzi (były w commicie). Trzeba: git rm --cached hasla.txt,
     *      commit — a jeśli w pliku było prawdziwe hasło, to je zmienić, bo zostało w historii.
     *   7. reset --soft zostawia zmiany w poczekalni (gotowe do commita); reset --hard kasuje je z poczekalni
     *      i z katalogu roboczego (reset --mixed zostawia w plikach, ale zdejmuje z poczekalni).
     *   8. revert — gdy commit jest już wysłany/wspólny (dopisuje commit odwracający, niczego nie kasuje).
     *      reset — tylko na lokalnych, jeszcze niewysłanych commitach (przepisuje historię).
     */
    // </editor-fold>
}
