package helpers.model;

import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;

/**
 * Student = student (record — t09_records).
 * <p>
 * Pola: name = imię; city = miasto; year = rok studiów (1, 2, 3); grades = oceny (lista liczb 2–5).
 * <p>
 * Uwaga: Henryk ma PUSTĄ listę ocen — celowo. Średniej z niczego NIE MA, więc averageGrade() zwraca
 * OptionalDouble (może być pusty), a nie 0.0. Gdyby zwracało 0.0, Henryk wyglądałby na gorszego niż
 * ktoś z samymi dwójkami — a on po prostu nie ma ocen. To klasyczna pułapka „zero zamiast braku wartości”.
 */
public record Student(String name, String city, int year, List<Integer> grades) {

    public Student {
        Objects.requireNonNull(name, "name nie może być null");
        grades = List.copyOf(grades);
    }

    /**
     * averageGrade = średnia ocen. Zwraca OptionalDouble: z wartością, gdy są oceny; pusty, gdy ocen brak.
     * <p>
     * mapToInt(Integer::intValue) — zamień Integer (obiekt) na int (typ prosty), żeby policzyć średnią.
     * average() zwraca OptionalDouble — Java sama „wie”, że średniej z pustej listy nie ma.
     * Decyzję, co pokazać przy braku ocen („brak ocen”, „—”, 0.0), podejmuje ten, kto wynik WYŚWIETLA.
     */
    public OptionalDouble averageGrade() {
        return grades.stream().mapToInt(Integer::intValue).average();
    }

    @Override
    public String toString() {
        return name + " " + grades;
    }
}
