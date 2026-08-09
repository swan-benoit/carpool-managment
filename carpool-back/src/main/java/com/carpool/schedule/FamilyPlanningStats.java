package com.carpool.schedule;

import com.carpool.family.AbsenceDays;
import com.carpool.family.Child;
import com.carpool.family.Family;
import com.carpool.family.TimeSlot;
import com.carpool.family.WeekDay;
import com.carpool.family.WeekType;

import java.util.List;
import java.util.stream.Stream;

public final class FamilyPlanningStats {

    private FamilyPlanningStats() {
    }

    public static boolean isPresent(Child child, WeekType weekType, WeekDay weekDay, TimeSlot timeSlot) {
        return !isAbsent(child, weekType, weekDay, timeSlot);
    }

    public static boolean isAbsent(Child child, WeekType weekType, WeekDay weekDay, TimeSlot timeSlot) {
        return child.absenceDays.stream().anyMatch(absence -> matches(absence, weekType, weekDay, timeSlot));
    }

    public static long availableSlots(Child child) {
        return totalSlotCount() - child.absenceDays.stream()
                .flatMap(FamilyPlanningStats::expandAbsence)
                .distinct()
                .count();
    }

    public static double perfectMeanTripPerWeek(Family family, List<Family> families) {
        double totalAvailableSlots = families.stream()
                .flatMap(currentFamily -> currentFamily.children.stream())
                .mapToDouble(FamilyPlanningStats::availableSlots)
                .sum();

        if (totalAvailableSlots == 0) {
            return 0.0;
        }

        // La cible d'équité est calculée une fois par foyer (obligation de transport des enfants
        // du foyer, sans dépendre de la famille-driver à laquelle chaque enfant est rattaché),
        // puis répartie entre les co-parents au prorata de leur disponibilité de conduite.
        List<Family> household = householdMembers(family, families);
        double householdAvailableSlots = household.stream()
                .flatMap(currentFamily -> currentFamily.children.stream())
                .mapToDouble(FamilyPlanningStats::availableSlots)
                .sum();

        double totalRequiredTripsPerWeek = totalRequiredTripsPerWeek(families);
        double householdTarget = totalRequiredTripsPerWeek * (householdAvailableSlots / totalAvailableSlots);

        // Les co-parents d'un même foyer partagent l'obligation de transport de l'enfant à parts
        // égales : chaque parent-driver reçoit la même cible d'équité, indépendamment de sa
        // disponibilité ou de sa capacité (une famille solo = foyer d'une seule famille => cible entière).
        return householdTarget / household.size();
    }

    /**
     * Familles du foyer de {@code family}. Une famille sans identifiant de foyer constitue son
     * propre foyer d'une seule famille (rétrocompatibilité : la répartition renvoie alors la cible
     * entière du foyer, identique au calcul historique).
     */
    static List<Family> householdMembers(Family family, List<Family> families) {
        if (family.householdId == null || family.householdId.isBlank()) {
            return List.of(family);
        }
        return families.stream()
                .filter(candidate -> family.householdId.equals(candidate.householdId))
                .toList();
    }

    public static double totalRequiredTripsPerWeek(List<Family> families) {
        return totalRequiredTripsPerCycle(families) / 2.0;
    }

    public static int requiredTripsForSlot(List<Family> families, WeekType weekType, WeekDay weekDay, TimeSlot timeSlot) {
        long presentChildren = families.stream()
                .flatMap(family -> family.children.stream())
                .filter(child -> isPresent(child, weekType, weekDay, timeSlot))
                .count();

        if (presentChildren == 0) {
            return 0;
        }

        int coveredChildren = 0;
        int usedCars = 0;
        for (int carCapacity : families.stream()
                .mapToInt(family -> family.carCapacity)
                .filter(capacity -> capacity > 0)
                .boxed()
                .sorted(java.util.Comparator.reverseOrder())
                .mapToInt(Integer::intValue)
                .toArray()) {
            coveredChildren += carCapacity;
            usedCars++;
            if (coveredChildren >= presentChildren) {
                return usedCars;
            }
        }

        throw new IllegalStateException("Insufficient car capacity to transport all present children for slot %s/%s/%s".formatted(weekType, weekDay, timeSlot));
    }

    public static int totalSlotCount() {
        return WeekType.values().length * WeekDay.values().length * TimeSlot.values().length;
    }

    private static double totalRequiredTripsPerCycle(List<Family> families) {
        double trips = 0;
        for (WeekType weekType : WeekType.values()) {
            for (WeekDay weekDay : WeekDay.values()) {
                for (TimeSlot timeSlot : TimeSlot.values()) {
                    trips += requiredTripsForSlot(families, weekType, weekDay, timeSlot);
                }
            }
        }
        return trips;
    }

    private static boolean matches(AbsenceDays absence, WeekType weekType, WeekDay weekDay, TimeSlot timeSlot) {
        return absence.weekType == weekType
                && absence.weekDay == weekDay
                && (absence.timeSlot == null || absence.timeSlot == timeSlot);
    }

    private static Stream<SlotKey> expandAbsence(AbsenceDays absence) {
        if (absence.weekType == null || absence.weekDay == null) {
            return Stream.empty();
        }
        if (absence.timeSlot != null) {
            return Stream.of(new SlotKey(absence.weekType, absence.weekDay, absence.timeSlot));
        }

        return Stream.of(TimeSlot.values())
                .map(timeSlot -> new SlotKey(absence.weekType, absence.weekDay, timeSlot));
    }

    private record SlotKey(WeekType weekType, WeekDay weekDay, TimeSlot timeSlot) {
    }
}
