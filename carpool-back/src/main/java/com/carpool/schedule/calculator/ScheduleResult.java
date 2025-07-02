package com.carpool.schedule.calculator;


import com.carpool.family.*;

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
        Long familyId = family.id;

        List<Trip> oddTrips = odd.trips(familyId);
        List<Trip> evenTrips = even.trips(familyId);

        return (oddTrips.size() + evenTrips.size()) / 2.0;
    }
    public Double perfectMeanTripPerWeek(Family family) {
        // 1. Calcul de la capacité moyenne des voitures
        int meanCarCapacity = families.stream()
                .mapToInt(f -> f.carCapacity * f.children.size())
                .sum() / families.stream().mapToInt(f -> f.children.size()).sum();

        // 2. Calcul du nombre total de "présences enfant" sur les 16 créneaux
        double totalChildPresences = families.stream()
                .flatMap(f -> f.children.stream())
                .mapToDouble(child -> {
                    // 16 créneaux possibles (4 jours × 2 trajets × 2 semaines)
                    long absenceCount = child.absenceDays.stream()
                            .filter(ad -> ad.weekDay != null)
                            .count();
                    return 16 - absenceCount;
                })
                .sum();

        // 3. Calcul des présences pour cette famille spécifique
        double familyChildPresences = family.children.stream()
                .mapToDouble(child -> {
                    long absenceCount = child.absenceDays.stream()
                            .filter(ad -> ad.weekDay != null)
                            .count();
                    return 16 - absenceCount;
                })
                .sum();

        if (totalChildPresences == 0) return 0.0;

        // 4. Calcul du nombre de trajets nécessaires
        int tripsPerSlot = (int) Math.ceil(totalChildPresences / (16 * meanCarCapacity));
        int totalTripsPerWeek = tripsPerSlot * TOTAL_TRIPS_PER_WEEK;

        // 5. Répartition proportionnelle
        return totalTripsPerWeek * (familyChildPresences / totalChildPresences);
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
}
