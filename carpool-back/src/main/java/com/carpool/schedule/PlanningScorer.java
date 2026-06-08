package com.carpool.schedule;

import com.carpool.family.Family;
import com.carpool.family.Child;
import com.carpool.family.TimeSlot;
import com.carpool.family.WeekDay;
import com.carpool.family.WeekType;
import com.carpool.schedule.calculator.Schedule;
import com.carpool.schedule.calculator.Assignment;
import com.carpool.schedule.calculator.ScheduleResult;
import com.carpool.schedule.calculator.Trip;
import com.carpool.workbook.normalization.NormalizedWorkbookFamily;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class PlanningScorer {

    public PlanningScore score(ScheduleResult scheduleResult, List<NormalizedWorkbookFamily> families) {
        Map<String, Map<SlotKey, PreferenceValue>> preferencesByFamily = preferencesByFamily(families);
        Map<String, MutableFamilyPlanningScore> scoreByFamily = new LinkedHashMap<>();
        families.forEach(family -> scoreByFamily.put(family.family().name, new MutableFamilyPlanningScore(family.family().name)));

        scoreTrips(scheduleResult.even().trips(), WeekType.EVEN, preferencesByFamily, scoreByFamily);
        scoreTrips(scheduleResult.odd().trips(), WeekType.ODD, preferencesByFamily, scoreByFamily);
        PlanningCompleteness completeness = computeCompleteness(scheduleResult, families, scoreByFamily);

        List<FamilyPlanningScore> familyScores = scoreByFamily.values().stream()
                .map(MutableFamilyPlanningScore::toRecord)
                .sorted((left, right) -> Integer.compare(left.totalScore(), right.totalScore()))
                .toList();
        PlanningJusticeScore justice = computeJustice(scheduleResult, familyScores);

        return new PlanningScore(
                familyScores.stream().mapToInt(FamilyPlanningScore::totalScore).sum(),
                familyScores.stream().mapToInt(FamilyPlanningScore::impossibleAssignments).sum(),
                familyScores.stream().mapToInt(FamilyPlanningScore::avoidAssignments).sum(),
                familyScores.stream().mapToInt(FamilyPlanningScore::preferredAssignments).sum(),
                familyScores.stream().mapToInt(FamilyPlanningScore::okAssignments).sum(),
                completeness.complete(),
                completeness.totalRequiredTransportSlots(),
                completeness.assignedRequiredTransportSlots(),
                completeness.missingRequiredTransportSlots(),
                completeness.completionRatio(),
                justice,
                familyScores
        );
    }

    private PlanningJusticeScore computeJustice(ScheduleResult scheduleResult, List<FamilyPlanningScore> familyScores) {
        Map<String, Family> familyByName = scheduleResult.families().stream()
                .collect(LinkedHashMap::new, (map, family) -> map.put(family.name, family), LinkedHashMap::putAll);

        List<FamilyJusticeScore> familyJusticeScores = familyScores.stream()
                .map(score -> toJusticeScore(scheduleResult, familyByName.get(score.familyName()), score))
                .sorted((left, right) -> Double.compare(left.justiceScore(), right.justiceScore()))
                .toList();

        double averageJusticeScore = familyJusticeScores.stream().mapToDouble(FamilyJusticeScore::justiceScore).average().orElse(1.0);
        double minimumJusticeScore = familyJusticeScores.stream().mapToDouble(FamilyJusticeScore::justiceScore).min().orElse(1.0);
        boolean perfectJustice = familyJusticeScores.stream().allMatch(FamilyJusticeScore::perfectJustice);

        return new PlanningJusticeScore(perfectJustice, averageJusticeScore, minimumJusticeScore, familyJusticeScores);
    }

    private FamilyJusticeScore toJusticeScore(ScheduleResult scheduleResult, Family family, FamilyPlanningScore planningScore) {
        double actualMeanTripPerWeek = family == null ? 0.0 : scheduleResult.meanTripPerWeek(family);
        double perfectMeanTripPerWeek = family == null ? 0.0 : scheduleResult.perfectMeanTripPerWeek(family);
        double tripDeviation = Math.abs(actualMeanTripPerWeek - perfectMeanTripPerWeek);

        boolean preferenceCompliance = planningScore.impossibleAssignments() == 0 && planningScore.avoidAssignments() == 0;
        boolean perfectJustice = planningScore.missingTransportSlots() == 0
                && preferenceCompliance
                && Double.compare(tripDeviation, 0.0) == 0;

        double justiceScore = computeJusticeScore(planningScore, actualMeanTripPerWeek, perfectMeanTripPerWeek, tripDeviation);
        return new FamilyJusticeScore(
                planningScore.familyName(),
                actualMeanTripPerWeek,
                perfectMeanTripPerWeek,
                tripDeviation,
                preferenceCompliance,
                perfectJustice,
                justiceScore
        );
    }

    private double computeJusticeScore(
            FamilyPlanningScore planningScore,
            double actualMeanTripPerWeek,
            double perfectMeanTripPerWeek,
            double tripDeviation
    ) {
        if (planningScore.missingTransportSlots() > 0 || planningScore.impossibleAssignments() > 0) {
            return 0.0;
        }

        double normalizedDeviation = perfectMeanTripPerWeek <= 0.0
                ? (actualMeanTripPerWeek == 0.0 ? 0.0 : 1.0)
                : Math.min(1.0, tripDeviation / perfectMeanTripPerWeek);
        double deviationPenalty = normalizedDeviation * 0.8;
        double avoidPenalty = Math.min(0.15, planningScore.avoidAssignments() * 0.05);
        double preferredBonus = planningScore.preferredAssignments() > 0 && planningScore.avoidAssignments() == 0
                ? Math.min(0.05, planningScore.preferredAssignments() * 0.01)
                : 0.0;

        return clamp01(1.0 - deviationPenalty - avoidPenalty + preferredBonus);
    }

    private double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private PlanningCompleteness computeCompleteness(
            ScheduleResult scheduleResult,
            List<NormalizedWorkbookFamily> families,
            Map<String, MutableFamilyPlanningScore> scoreByFamily
    ) {
        Set<TransportSlot> requiredTransports = new LinkedHashSet<>();
        Set<TransportSlot> assignedTransports = new LinkedHashSet<>();
        Map<Child, String> childOwnerByChild = childOwnerByChild(families);

        collectRequiredTransports(WeekType.EVEN, families, requiredTransports);
        collectRequiredTransports(WeekType.ODD, families, requiredTransports);
        collectAssignedTransports(scheduleResult.even(), WeekType.EVEN, assignedTransports, childOwnerByChild);
        collectAssignedTransports(scheduleResult.odd(), WeekType.ODD, assignedTransports, childOwnerByChild);

        assignedTransports.retainAll(requiredTransports);
        requiredTransports.forEach(transport -> scoreByFamily.get(transport.familyName()).requiredTransportSlots++);
        assignedTransports.forEach(transport -> scoreByFamily.get(transport.familyName()).assignedTransportSlots++);
        scoreByFamily.values().forEach(score -> score.missingTransportSlots = score.requiredTransportSlots - score.assignedTransportSlots);

        int totalRequiredTransportSlots = requiredTransports.size();
        int assignedRequiredTransportSlots = assignedTransports.size();
        int missingRequiredTransportSlots = totalRequiredTransportSlots - assignedRequiredTransportSlots;
        double completionRatio = totalRequiredTransportSlots == 0
                ? 1.0
                : assignedRequiredTransportSlots / (double) totalRequiredTransportSlots;

        return new PlanningCompleteness(
                missingRequiredTransportSlots == 0,
                totalRequiredTransportSlots,
                assignedRequiredTransportSlots,
                missingRequiredTransportSlots,
                completionRatio
        );
    }

    private void collectRequiredTransports(WeekType weekType, List<NormalizedWorkbookFamily> families, Set<TransportSlot> requiredTransports) {
        for (WeekDay weekDay : WeekDay.values()) {
            for (TimeSlot timeSlot : TimeSlot.values()) {
                for (NormalizedWorkbookFamily family : families) {
                    for (Child child : family.family().children) {
                        if (!FamilyPlanningStats.isPresent(child, weekType, weekDay, timeSlot)) {
                            continue;
                        }
                        requiredTransports.add(new TransportSlot(weekType, weekDay, timeSlot, family.family().name, child.name));
                    }
                }
            }
        }
    }

    private void collectAssignedTransports(
            Schedule schedule,
            WeekType weekType,
            Set<TransportSlot> assignedTransports,
            Map<Child, String> childOwnerByChild
    ) {
        for (Trip trip : schedule.trips()) {
            for (Assignment assignment : trip.cars().Assignments()) {
                for (Child child : assignment.children()) {
                    String familyName = childOwnerByChild.get(child);
                    if (familyName == null) {
                        continue;
                    }
                    TransportSlot transportSlot = new TransportSlot(weekType, trip.weekDay(), trip.timeSlot(), familyName, child.name);
                    assignedTransports.add(transportSlot);
                }
            }
        }
    }

    private Map<Child, String> childOwnerByChild(List<NormalizedWorkbookFamily> families) {
        Map<Child, String> childOwnerByChild = new LinkedHashMap<>();
        for (NormalizedWorkbookFamily family : families) {
            for (Child child : family.family().children) {
                childOwnerByChild.put(child, family.family().name);
            }
        }
        return childOwnerByChild;
    }

    private void scoreTrips(
            List<Trip> trips,
            WeekType weekType,
            Map<String, Map<SlotKey, PreferenceValue>> preferencesByFamily,
            Map<String, MutableFamilyPlanningScore> scoreByFamily
    ) {
        for (Trip trip : trips) {
            SlotKey slotKey = new SlotKey(weekType, trip.weekDay(), trip.timeSlot());
            for (Assignment assignment : trip.cars().Assignments()) {
                Family driverFamily = assignment.driverFamily();
                MutableFamilyPlanningScore familyScore = scoreByFamily.computeIfAbsent(
                        driverFamily.name,
                        MutableFamilyPlanningScore::new
                );
                PreferenceValue preferenceValue = preferencesByFamily
                        .getOrDefault(driverFamily.name, Map.of())
                        .getOrDefault(slotKey, PreferenceValue.OK);
                familyScore.add(preferenceValue);
            }
        }
    }

    private Map<String, Map<SlotKey, PreferenceValue>> preferencesByFamily(List<NormalizedWorkbookFamily> families) {
        Map<String, Map<SlotKey, PreferenceValue>> preferencesByFamily = new LinkedHashMap<>();
        for (NormalizedWorkbookFamily family : families) {
            Map<SlotKey, PreferenceValue> preferences = new LinkedHashMap<>();
            for (NormalizedWorkbookFamily.FamilyPreference preference : family.preferences()) {
                preferences.put(
                        new SlotKey(preference.weekType(), preference.weekDay(), preference.timeSlot()),
                        PreferenceValue.fromWorkbookValue(preference.value())
                );
            }
            preferencesByFamily.put(family.family().name, preferences);
        }
        return preferencesByFamily;
    }

    private record SlotKey(WeekType weekType, WeekDay weekDay, TimeSlot timeSlot) {
    }

    private record TransportSlot(WeekType weekType, WeekDay weekDay, TimeSlot timeSlot, String familyName, String childName) {
    }

    private record PlanningCompleteness(
            boolean complete,
            int totalRequiredTransportSlots,
            int assignedRequiredTransportSlots,
            int missingRequiredTransportSlots,
            double completionRatio
    ) {
    }

    private static final class MutableFamilyPlanningScore {
        private final String familyName;
        private int totalScore;
        private int impossibleAssignments;
        private int avoidAssignments;
        private int preferredAssignments;
        private int okAssignments;
        private int requiredTransportSlots;
        private int assignedTransportSlots;
        private int missingTransportSlots;

        private MutableFamilyPlanningScore(String familyName) {
            this.familyName = familyName;
        }

        private void add(PreferenceValue preferenceValue) {
            totalScore += preferenceValue.weight();
            switch (preferenceValue) {
                case IMPOSSIBLE -> impossibleAssignments++;
                case EVITER -> avoidAssignments++;
                case PREFERE -> preferredAssignments++;
                case OK -> okAssignments++;
            }
        }

        private FamilyPlanningScore toRecord() {
            return new FamilyPlanningScore(
                    familyName,
                    totalScore,
                    impossibleAssignments,
                    avoidAssignments,
                    preferredAssignments,
                    okAssignments,
                    requiredTransportSlots,
                    assignedTransportSlots,
                    missingTransportSlots
            );
        }
    }
}
