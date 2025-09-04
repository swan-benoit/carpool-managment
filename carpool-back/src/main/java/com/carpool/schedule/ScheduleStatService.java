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
                        perfectMeanTripPerWeek(family, Families, schedule))
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

    public Double perfectMeanTripPerWeek(Family family, List<Family> families, FullSchedule schedule) {
        // 2. Calcul du nombre total de "présences enfant" sur tous les créneaux
        double totalAvailableSlots = families.stream()
                .flatMap(f -> f.children.stream())
                .mapToDouble(child -> {
                    // 16 créneaux possibles (4 jours × 2 trajets × 2 semaines)
                    // Moins les créneaux où l'enfant est absent
                    return 16 - child.absenceDays.size();
                })
                .sum();

        double familyAvailableSlots = family.children.stream()
                .mapToDouble(child -> 16 - child.absenceDays.size())
                .sum();

        if (totalAvailableSlots == 0) return 0.0;

        var totalTrips = schedule.evenSchedule.trips.size() + schedule.oddSchedule.trips.size();
        int totalTripsPerWeek = totalTrips / 2;

        return totalTripsPerWeek * (familyAvailableSlots / totalAvailableSlots);
    }
}
