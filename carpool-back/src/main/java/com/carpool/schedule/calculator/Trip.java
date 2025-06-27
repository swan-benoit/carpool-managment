package com.carpool.schedule.calculator;


import com.carpool.family.*;

import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public record Trip(WeekDay weekDay, TimeSlot timeSlot, WeekType weekType, Cars cars,
                   List<Family> families) {
    public Trip withCars(Assignment carAssignment) {
        Cars mergedCars = new Cars(Stream.concat(
                filterMovedAssignement(carAssignment),
                Stream.of(carAssignment)
        ).toList());
        return new Trip(
                weekDay,
                timeSlot,
                weekType,
                mergedCars,
                families);
    }

    private Stream<Assignment> filterMovedAssignement(Assignment newAssignment) {

        return cars.Assignments().stream()
                .map(oldAssignement -> new Assignment(
                        oldAssignement.driverFamily(),
                        removeAssignementChildren(newAssignment, oldAssignement))
                );
    }

    private List<Child> removeAssignementChildren(Assignment newAssignment, Assignment oldAssignement) {
        return oldAssignement.children().stream()
                .filter(child -> !newAssignment.children().contains(child))
                .toList();
    }

    public boolean complete() {
        return families.stream()
                .flatMap(family -> family.children.stream())
                .allMatch(child -> cars.Assignments().stream()
                        .anyMatch(car -> car.children().contains(child)));
    }

    @Override
    public String toString() {
        return cars.Assignments().stream()
                .map(car -> car.driverFamily().name + " " +  car.children().stream().map(c -> c.name).collect(joining(",", "(", ")")))
                .collect(joining("<--> "));
    }
}
