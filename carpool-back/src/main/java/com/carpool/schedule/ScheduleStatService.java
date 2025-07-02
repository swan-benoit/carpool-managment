package com.carpool.schedule;

import com.carpool.family.Family;
import com.carpool.schedule.persistence.FullSchedule;
import com.carpool.schedule.persistence.Trip;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class ScheduleStatService {
    public static final int TOTAL_TRIPS_PER_WEEK = 8;
    public List<Stats> getStat(long id) {
        FullSchedule schedule = FullSchedule.findById(id);
        List<Family> Families = Family.listAll();
        return Families.stream()
                .map(family -> new Stats(
                        family,
                        meanTripPerWeek(family, schedule),
                        perfectMeanTripPerWeek(family, Families))
                ).toList();
    }

    public Double meanTripPerWeek(Family family, FullSchedule schedule) {
        Long familyId = family.id;

        List<Trip> oddTrips = schedule.oddSchedule.trips.stream()
                .filter(trip -> Objects.equals(trip.driver.id, familyId))
                .toList();
        List<Trip> evenTrips = schedule.evenSchedule.trips.stream()
                .filter(trip -> Objects.equals(trip.driver.id, familyId))
                .toList();

        return (oddTrips.size() + evenTrips.size()) / 2.0;
    }
    public Double perfectMeanTripPerWeek(Family family, List<Family> families) {
        // 1. Calcul de la capacité moyenne pondérée
        int meanCarCapacity = families.stream()
                .mapToInt(f -> f.carCapacity * f.children.size())
                .sum() / families.stream().mapToInt(f -> f.children.size()).sum();

        // 2. Calcul du nombre total de "présences enfant" sur tous les créneaux
        double totalAvailableSlots = families.stream()
                .flatMap(f -> f.children.stream())
                .mapToDouble(child -> {
                    // 16 créneaux possibles (4 jours × 2 trajets × 2 semaines)
                    // Moins les créneaux où l'enfant est absent
                    return 16 - child.absenceDays.size();
                })
                .sum();

        // 3. Calcul des créneaux disponibles pour cette famille
        double familyAvailableSlots = family.children.stream()
                .mapToDouble(child -> 16 - child.absenceDays.size())
                .sum();

        if (totalAvailableSlots == 0) return 0.0;

        // 4. Calcul du nombre de trajets nécessaires
        int tripsPerSlot = (int) Math.ceil(totalAvailableSlots / (16 * meanCarCapacity));
        int totalTripsPerWeek = tripsPerSlot * TOTAL_TRIPS_PER_WEEK;

        // 5. Répartition proportionnelle selon les disponibilités
        return totalTripsPerWeek * (familyAvailableSlots / totalAvailableSlots);
    }
//    public Double perfectMeanTripPerWeek(Family family, List<Family> families) {
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
}
