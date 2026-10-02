/**
 * TEMAT: t20_lombok — biblioteka Lombok: adnotacje generujące kod podczas kompilacji
 *
 * <p>Lombok to biblioteka działająca jako procesor adnotacji (annotation processor, patrz
 * t19_annotations_reflection/Annotations07Processors). Podczas kompilacji dopisuje do klasy kod,
 * który normalnie trzeba by napisać ręcznie: gettery, settery, konstruktory, {@code toString},
 * {@code equals}/{@code hashCode}, budowniczych (buildery) i inne. Mniej kodu do napisania i
 * utrzymania — ale trzeba rozumieć, CO dokładnie powstaje, żeby nie dać się zaskoczyć gotowym
 * kodem, którego nikt w zespole nie widział na oczy.</p>
 *
 * <p>Wymagania wstępne: t01–t19, w szczególności t09_records (rekordy — alternatywa dla Lomboka),
 * t06_oop_basics (equals/hashCode, niezmienność), wzorzec budowniczego (builder) oraz
 * t19_annotations_reflection (adnotacje, refleksja, procesory adnotacji).</p>
 *
 * <p>Kolejność czytania:</p>
 * <ol>
 *     <li>Lombok01Accessors</li>
 *     <li>Lombok02Constructors</li>
 *     <li>Lombok03DataValueBuilder</li>
 *     <li>Lombok04Other</li>
 * </ol>
 *
 * <p>SŁÓWKA: library = biblioteka; annotation processor = procesor adnotacji; boilerplate = kod
 * szablonowy (powtarzalny); generate = generować; accessor = metoda dostępowa (getter/setter);
 * builder = budowniczy; immutable = niezmienny; mutable = zmienny.</p>
 */
package t20_lombok;
