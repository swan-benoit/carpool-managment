package com.carpool.schedule;

import com.carpool.family.*;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.Arrays;
import java.util.Optional;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

@Entity
public class Schedule extends PanacheEntity {

    public WeekType weekType;
    @OneToMany
    public List<Trip> trips;

    @JsonbTransient
    public boolean isFull(List<Family> families) {
        return Arrays.stream(WeekDay.values())
                .allMatch(day -> Arrays.stream(TimeSlot.values())
                        .allMatch(slot -> tripExist(day, slot) && allChildrenCanGoToSchool(day, slot, families))
                );
    }

    @JsonbTransient
    private boolean tripExist(WeekDay day, TimeSlot slot) {
        return tripForSlot(day, slot)
                .isPresent();
    }

    @JsonbTransient
    private Optional<Trip> tripForSlot(WeekDay day, TimeSlot slot) {
        return trips.stream()
                .filter(trip -> trip.weekDay == day && trip.timeSlot == slot)
                .findFirst();
    }

    @JsonbTransient
    private boolean allChildrenCanGoToSchool(WeekDay day, TimeSlot slot, List<Family> families) {
        return tripForSlot(day, slot)
                .map(trip -> trip.complete(families))
                .orElse(false);
    }

    @JsonbTransient
    public List<Trip> trips(Long FamilyId) {
        return trips.stream()
                .filter(trip -> trip.cars.stream()
                        .anyMatch(car -> car.family.id.equals(FamilyId)))
                .toList();
    }

    public List<Child> childrenCandidates(WeekDay weekDay, TimeSlot timeSlot, Family driver, List<Family> families) {
        int leftCapacity = driver.carCapacity - driver.children.size();

        return Stream.concat(
                driver.children.stream(),
                toAssignChildren(weekDay, timeSlot, families).stream()
                        .filter(child -> !driver.children.contains(child))
                        .limit(leftCapacity)
        ).toList();

    }

    private List<Child> toAssignChildren(WeekDay weekDay, TimeSlot timeSlot, List<Family> families) {
        List<Child> assignedChildren = alreadyAssignedChildren(weekDay, timeSlot);

        List<Child> allChildren = families.stream().flatMap(family -> family.children.stream()).toList();

        return allChildren.stream()
                .filter(child -> !assignedChildren.contains(child))
                .toList();
    }

    private List<Child> alreadyAssignedChildren(WeekDay weekDay, TimeSlot timeSlot) {
        return trips.stream().filter(trip -> isCurrentTrip(weekDay, timeSlot, trip))
                .flatMap(trip -> trip.cars.stream())
                .flatMap(cars -> cars.children.stream())
                .toList();
    }
    private static boolean isCurrentTrip(WeekDay weekDay, TimeSlot timeSlot, Trip trip) {
        return trip.weekDay.equals(weekDay) && trip.timeSlot.equals(timeSlot);
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
                .findFirst().orElse(new Trip());

        if (driver.carCapacity < children.size()) {
            throw new IllegalArgumentException("Driver's car capacity is not sufficient for the number of children.");
        }

        List<Trip> newTrips = Stream.concat(
                trips.stream().filter(trip -> !isCurrentTrip(weekDay, timeSlot, trip)),
                Stream.of(currentTrip.withCars(driver, children))

        ).toList();

        return this.withTrips(newTrips);
    }

    private Schedule withTrips(List<Trip> newTrips) {
        Schedule schedule = new Schedule();
        schedule.trips = newTrips;
        schedule.weekType = weekType;
        return schedule;
    }

    private boolean tripsAlreadyAssigned(WeekDay weekDay, TimeSlot timeSlot, Family driver) {
        return trips.stream()
                .anyMatch(trip -> trip.weekDay.equals(weekDay)
                        && trip.timeSlot.equals(timeSlot)
                        && trip.cars.stream().anyMatch(car -> car.equals(driver))
                );
    }

    private boolean childrenAlreadyAssigned(WeekDay weekDay, TimeSlot timeSlot, List<Child> children) {
        return trips.stream()
                .anyMatch(trip -> trip.weekDay.equals(weekDay)
                        && trip.timeSlot.equals(timeSlot)
                        && trip.cars.stream().map(car -> car.children)
                        .anyMatch(children::contains)
                );
    }


    public boolean isTripFull(WeekDay weekDay, TimeSlot timeSlot, List<Family> families) {
        return tripForSlot(weekDay, timeSlot)
                .map(trip -> trip.complete(families))
                .orElse(false);
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
}
