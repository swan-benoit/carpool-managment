package com.carpool.schedule;

import com.carpool.family.*;
import com.carpool.schedule.Schedule;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;

import java.util.List;

@Entity
public class FullSchedule extends PanacheEntity {
    @JsonbTransient
    @Transient
    public static final int TOTAL_TRIPS_PER_WEEK = 8;

    @OneToOne
    public Schedule oddSchedule = new Schedule();

    @OneToOne
    public Schedule evenSchedule;

    @Transient
    public List<Family> families;

    @JsonbTransient
    @Transient
    public boolean isFull() {
        return oddSchedule.isFull(families) && evenSchedule.isFull(families);
    }

    public List<Family> driverOrderByCurrentTripMean() {
        return families.stream()
                .sorted((o1, o2) -> {
                    Double mean1 = meanTripPerWeek(o1) / perfectMeanTripPerWeek(o1);
                    Double mean2 = meanTripPerWeek(o2) / perfectMeanTripPerWeek(o2);
                    return mean1.compareTo(mean2);
                }).toList();
    }

    public Double meanTripPerWeek(Family family) {
        Long familyId = family.id;

        List<Trip> oddTrips = oddSchedule.trips(familyId);
        List<Trip> evenTrips = evenSchedule.trips(familyId);

        return (oddTrips.size() + evenTrips.size()) / 2.0;
    }

    public Double perfectMeanTripPerWeek(Family family) {
        int meanCarCapacity = families.stream()
                .mapToInt(f-> f.carCapacity * f.children.size())
                .sum() / families.stream().mapToInt(f -> f.children.size()).sum();

        long totalChildren = families.stream()
                .mapToLong(family1 -> family1.children.size())
                .sum();

        int tripsPerSlot = (int) Math.ceil(totalChildren / (double) meanCarCapacity);

        int totalTripsPerWeek = tripsPerSlot * TOTAL_TRIPS_PER_WEEK;

        int familyChildren = family.children.size();


        return totalTripsPerWeek * familyChildren / (double) totalChildren;
    }

    public List<Child> childrenCandidates(WeekType weekType, WeekDay weekDay, TimeSlot timeSlot, Family driver) {
        return switch (weekType) {
            case EVEN -> evenSchedule.childrenCandidates(weekDay, timeSlot, driver, families);
            case ODD -> oddSchedule.childrenCandidates(weekDay, timeSlot, driver, families);
        };
    }

    public FullSchedule addTrip(WeekType weekType, WeekDay weekDay, TimeSlot timeSlot, Family driver, List<Child> children) {
        return switch (weekType) {
            case EVEN -> {
                evenSchedule = evenSchedule.addTrip(weekDay, timeSlot, driver, children);
                yield this;
            }
            case ODD -> {
                oddSchedule = oddSchedule.addTrip(weekDay, timeSlot, driver, children);
                yield this;
            }
        };
    }
    public boolean isTripFull(WeekType weekType, WeekDay weekDay, TimeSlot timeSlot) {
        return switch (weekType) {
            case EVEN -> evenSchedule.isTripFull(weekDay, timeSlot, families);
            case ODD -> oddSchedule.isTripFull(weekDay, timeSlot, families);
        };
    }

    @Override
    public String toString() {
        return """
                Even Week Schedule:
                %s
                Odd Week Schedule:
                %s
                """.formatted(evenSchedule, oddSchedule);
    }

}
