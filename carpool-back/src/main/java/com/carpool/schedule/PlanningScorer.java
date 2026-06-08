package com.carpool.schedule;

import com.carpool.family.Family;
import com.carpool.family.TimeSlot;
import com.carpool.family.WeekDay;
import com.carpool.family.WeekType;
import com.carpool.schedule.calculator.Assignment;
import com.carpool.schedule.calculator.ScheduleResult;
import com.carpool.schedule.calculator.Trip;
import com.carpool.workbook.normalization.NormalizedWorkbookFamily;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class PlanningScorer {

    public PlanningScore score(ScheduleResult scheduleResult, List<NormalizedWorkbookFamily> families) {
        Map<String, Map<SlotKey, PreferenceValue>> preferencesByFamily = preferencesByFamily(families);
        Map<String, MutableFamilyPlanningScore> scoreByFamily = new LinkedHashMap<>();
        families.forEach(family -> scoreByFamily.put(family.family().name, new MutableFamilyPlanningScore(family.family().name)));

        scoreTrips(scheduleResult.even().trips(), WeekType.EVEN, preferencesByFamily, scoreByFamily);
        scoreTrips(scheduleResult.odd().trips(), WeekType.ODD, preferencesByFamily, scoreByFamily);

        List<FamilyPlanningScore> familyScores = scoreByFamily.values().stream()
                .map(MutableFamilyPlanningScore::toRecord)
                .sorted((left, right) -> Integer.compare(left.totalScore(), right.totalScore()))
                .toList();

        return new PlanningScore(
                familyScores.stream().mapToInt(FamilyPlanningScore::totalScore).sum(),
                familyScores.stream().mapToInt(FamilyPlanningScore::impossibleAssignments).sum(),
                familyScores.stream().mapToInt(FamilyPlanningScore::avoidAssignments).sum(),
                familyScores.stream().mapToInt(FamilyPlanningScore::preferredAssignments).sum(),
                familyScores.stream().mapToInt(FamilyPlanningScore::okAssignments).sum(),
                familyScores
        );
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

    private static final class MutableFamilyPlanningScore {
        private final String familyName;
        private int totalScore;
        private int impossibleAssignments;
        private int avoidAssignments;
        private int preferredAssignments;
        private int okAssignments;

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
                    okAssignments
            );
        }
    }
}
