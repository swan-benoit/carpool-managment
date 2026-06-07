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

        double familyAvailableSlots = family.children.stream()
                .mapToDouble(FamilyPlanningStats::availableSlots)
                .sum();

        double totalRequiredTripsPerWeek = totalRequiredTripsPerWeek(families);
        return totalRequiredTripsPerWeek * (familyAvailableSlots / totalAvailableSlots);
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
