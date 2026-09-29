package helpers.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Employee = pracownik (record — t09_records).
 * <p>
 * Pola:
 * <ul>
 *   <li>name = imię i nazwisko,</li>
 *   <li>department = dział (enum Department),</li>
 *   <li>salary = pensja miesięczna brutto w zł (int — celowo liczba całkowita, żeby w streamach ćwiczyć
 *       sumowanie i średnie na typach prostych: mapToInt, summingInt, averagingInt),</li>
 *   <li>age = wiek,</li>
 *   <li>hireDate = data zatrudnienia (LocalDate — sama data bez godziny; t17_datetime),</li>
 *   <li>skills = umiejętności (lista napisów — przyda się do flatMap w t16_streams/Streams04FlatMap).</li>
 * </ul>
 */
public record Employee(String name, Department department, int salary, int age, LocalDate hireDate, List<String> skills) {

    public Employee {
        Objects.requireNonNull(name, "name nie może być null");
        Objects.requireNonNull(department, "department nie może być null");
        Objects.requireNonNull(hireDate, "hireDate nie może być null");
        // DOBRA PRAKTYKA: kopia obronna (defensive copy).
        // List.copyOf tworzy NIEMODYFIKOWALNĄ kopię listy — nikt z zewnątrz nie zmieni skills po utworzeniu rekordu.
        // Bez tego ktoś mógłby zrobić lista.add("...") na liście, którą przekazał, i „po cichu” zmienić pracownika.
        skills = List.copyOf(skills);
    }

    @Override
    public String toString() {
        return name + " (" + department + ", " + salary + " zł)";
    }
}
