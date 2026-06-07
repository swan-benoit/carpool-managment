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
        return FamilyPlanningStats.perfectMeanTripPerWeek(family, families);
    }
}
