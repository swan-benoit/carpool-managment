package com.carpool.schedule.calculator;


import com.carpool.family.*;
import com.carpool.schedule.FamilyPlanningStats;

import java.util.List;
import java.util.stream.Stream;

public record ScheduleResult(Schedule odd, Schedule even, List<Family> families) {

    public static final int TOTAL_TRIPS_PER_WEEK = 8;

    public static ScheduleResult empty(List<Family> families) {
        return new ScheduleResult(new Schedule(WeekType.ODD, List.of(), families), new Schedule(WeekType.EVEN, List.of(), families), families);
    }

    public boolean isFull() {
        return odd().isFull() && even().isFull();
    }

    public Double meanTripPerWeek(Family family) {
        List<Trip> oddTrips = odd.trips(family);
        List<Trip> evenTrips = even.trips(family);

        return (oddTrips.size() + evenTrips.size()) / 2.0;
    }
    public Double perfectMeanTripPerWeek(Family family) {
        return FamilyPlanningStats.perfectMeanTripPerWeek(family, families);
    }

//    public Double perfectMeanTripPerWeek(Family family) {
//        int meanCarCapacity = families.stream()
//                .mapToInt(f-> f.carCapacity * f.children.size())
//                .sum() / families.stream().mapToInt(f -> f.children.size()).sum();
//
//        long totalChildren = families.stream()
//                .mapToLong(family1 -> family1.children.size())
//                .sum();
//
//        int tripsPerSlot = (int) Math.ceil(totalChildren / (double) meanCarCapacity);
//
//        int totalTripsPerWeek = tripsPerSlot * TOTAL_TRIPS_PER_WEEK;
//
//        int familyChildren = family.children.size();
//
//
//        return totalTripsPerWeek * familyChildren / (double) totalChildren;
//    }

    @Override
    public String toString() {
        return """
                Even Week Schedule:
                %s
                Odd Week Schedule:
                %s
                """.formatted(even, odd);
    }

    public List<Family> driverOrderByCurrentTripMean() {
        return families.stream()
                .sorted((o1, o2) -> {
                    Double mean1 = meanTripPerWeek(o1) / perfectMeanTripPerWeek(o1);
                    Double mean2 = meanTripPerWeek(o2) / perfectMeanTripPerWeek(o2);
                    return mean1.compareTo(mean2);
                }).toList();
    }

    public List<Child> childrenCandidates(WeekType weekType, WeekDay weekDay, TimeSlot timeSlot, Family driver) {
       return switch (weekType) {
            case EVEN -> even.childrenCandidates(weekDay, timeSlot, driver);
            case ODD -> odd.childrenCandidates(weekDay, timeSlot, driver);
        };
    }

    public ScheduleResult addTrip(WeekType weekType, WeekDay weekDay, TimeSlot timeSlot, Family driver, List<Child> children) {
        return switch (weekType) {
            case EVEN -> new ScheduleResult(odd,even.addTrip(weekDay, timeSlot, driver, children), families);
            case ODD -> new ScheduleResult(odd.addTrip(weekDay, timeSlot, driver, children), even, families);
        };
    }

    public List<Trip> uncompleteTrips() {
        return Stream.concat(
                odd.trips().stream().filter(trip -> !trip.complete()),
                even.trips().stream().filter(trip -> !trip.complete())
        ).toList();
    }

    public boolean isTripFull(WeekType weekType, WeekDay weekDay, TimeSlot timeSlot) {
        return switch (weekType) {
            case EVEN -> even.isTripFull(weekDay, timeSlot);
            case ODD -> odd.isTripFull(weekDay, timeSlot);
        };
    }

    public boolean hasChildrenToTransport(WeekType weekType, WeekDay weekDay, TimeSlot timeSlot) {
        return switch (weekType) {
            case EVEN -> even.hasChildrenToTransport(weekDay, timeSlot);
            case ODD -> odd.hasChildrenToTransport(weekDay, timeSlot);
        };
    }

    public int unassignedChildrenCount(WeekType weekType, WeekDay weekDay, TimeSlot timeSlot) {
        return switch (weekType) {
            case EVEN -> even.unassignedChildrenCount(weekDay, timeSlot);
            case ODD -> odd.unassignedChildrenCount(weekDay, timeSlot);
        };
    }
}
