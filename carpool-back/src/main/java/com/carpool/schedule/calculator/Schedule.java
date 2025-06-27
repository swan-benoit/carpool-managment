package com.carpool.schedule.calculator;


import com.carpool.family.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

public record Schedule(WeekType weekType, List<Trip> trips, List<Family> families) {

    public boolean isFull() {
        return stream(WeekDay.values())
                .allMatch(day -> stream(TimeSlot.values())
                        .allMatch(slot -> tripExist(day, slot) && allChildrenCanGoToSchool(day, slot))
                );
    }

    private boolean allChildrenCanGoToSchool(WeekDay day, TimeSlot slot) {
        return tripForSlot(day, slot)
                .map(Trip::complete)
                .orElse(false);
    }

    private boolean tripExist(WeekDay day, TimeSlot slot) {
        return tripForSlot(day, slot)
                .isPresent();
    }

    private Optional<Trip> tripForSlot(WeekDay day, TimeSlot slot) {
        return trips.stream()
                .filter(trip -> trip.weekDay() == day && trip.timeSlot() == slot)
                .findFirst();
    }

    public List<Trip> trips(Long FamilyId) {
        return trips().stream()
                .filter(trip -> trip.cars().Assignments().stream()
                        .anyMatch(car -> car.driverFamily().id.equals(FamilyId)))
                .toList();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (var day: WeekDay.values()) {
            sb.append("\n");
            sb.append("-----------------------");
            sb.append(day);
            sb.append("\n  ");
            for (var slot: TimeSlot.values()) {
                sb.append("\n  ").append(slot);
                var trip = tripForSlot(day, slot);
                if (trip.isPresent()) {
                    sb.append("\n   ").append(trip.get());
                } else {
                    sb.append("\n   ").append("No trip assigned");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public List<Child> childrenCandidates(WeekDay weekDay, TimeSlot timeSlot, Family driver) {
        int leftCapacity = driver.carCapacity - driver.children.size();

        return Stream.concat(
                driver.children.stream(),
                toAssignChildren(weekDay, timeSlot).stream()
                        .filter(child -> !driver.children.contains(child))
                        .limit(leftCapacity)
        ).toList();

    }

    private List<Child> toAssignChildren(WeekDay weekDay, TimeSlot timeSlot) {
        List<Child> assignedChildren = alreadyAssignedChildren(weekDay, timeSlot);

        List<Child> allChildren = families.stream().flatMap(family -> family.children.stream()).toList();

        return allChildren.stream()
                .filter(child -> !assignedChildren.contains(child))
                .toList();
    }

    private List<Child> alreadyAssignedChildren(WeekDay weekDay, TimeSlot timeSlot) {
        return trips.stream().filter(trip -> isCurrentTrip(weekDay, timeSlot, trip))
                .flatMap(trip -> trip.cars().Assignments().stream())
                .flatMap(cars -> cars.children().stream())
                .toList();
    }

    public Schedule addTrip(WeekDay weekDay, TimeSlot timeSlot, Family driver, List<Child> children) {
        if (tripsAlreadyAssigned(weekDay, timeSlot, driver)) {
            throw new IllegalArgumentException("Trip already assigned for this family on this day and time slot.");
        }

        if (childrenAlreadyAssigned(weekDay, timeSlot, children)) {
            throw new IllegalArgumentException("Children already assigned for this day and time slot.");
        }

        Trip currentTrip = trips.stream()
                .filter(trip -> isCurrentTrip(weekDay, timeSlot, trip))
                .findFirst().orElse(new Trip(weekDay, timeSlot, weekType, new Cars(List.of()), families));

        if (driver.carCapacity < children.size()) {
            throw new IllegalArgumentException("Driver's car capacity is not sufficient for the number of children.");
        }

        List<Trip> newTrips = Stream.concat(
                trips.stream().filter(trip -> !isCurrentTrip(weekDay, timeSlot, trip)),
                Stream.of(currentTrip.withCars(new Assignment(driver, children)))

        ).toList();
        return this.withTrips(newTrips);
    }

    private Schedule withTrips(List<Trip> newTrips) {
        return new Schedule(weekType, newTrips, families);
    }

    private static boolean isCurrentTrip(WeekDay weekDay, TimeSlot timeSlot, Trip trip) {
        return trip.weekDay().equals(weekDay) && trip.timeSlot().equals(timeSlot);
    }

    private boolean childrenAlreadyAssigned(WeekDay weekDay, TimeSlot timeSlot, List<Child> children) {
        return trips.stream()
                .anyMatch(trip -> trip.weekDay().equals(weekDay)
                        && trip.timeSlot().equals(timeSlot)
                        && trip.cars().Assignments().stream().map(Assignment::children)
                        .anyMatch(children::contains)
                );
    }

    private boolean tripsAlreadyAssigned(WeekDay weekDay, TimeSlot timeSlot, Family driver) {
        return trips.stream()
                .anyMatch(trip -> trip.weekDay().equals(weekDay)
                        && trip.timeSlot().equals(timeSlot)
                        && trip.cars().Assignments().stream().anyMatch(car -> car.driverFamily().equals(driver))
                );
    }

    public boolean isTripFull(WeekDay weekDay, TimeSlot timeSlot) {
        return tripForSlot(weekDay, timeSlot)
                .map(Trip::complete)
                .orElse(false);
    }
}
