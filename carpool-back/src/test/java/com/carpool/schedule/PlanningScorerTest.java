package com.carpool.schedule;

import com.carpool.family.Child;
import com.carpool.family.Family;
import com.carpool.family.TimeSlot;
import com.carpool.family.WeekDay;
import com.carpool.family.WeekType;
import com.carpool.schedule.calculator.Schedule;
import com.carpool.schedule.calculator.ScheduleResult;
import com.carpool.schedule.calculator.ScheduleService;
import com.carpool.workbook.normalization.NormalizedWorkbookFamily;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlanningScorerTest {

    @Test
    void marks_perfect_justice_when_target_and_preferences_are_fully_respected() {
        Family family = singleChildFamily();
        NormalizedWorkbookFamily normalizedFamily = new NormalizedWorkbookFamily(
                family,
                List.of(),
                NormalizedWorkbookFamily.FamilyNotes.empty()
        );

        ScheduleResult scheduleResult = new ScheduleService().generateSchedule(List.of(family));
        PlanningScore planningScore = new PlanningScorer().score(scheduleResult, List.of(normalizedFamily));

        assertThat(planningScore.justice().perfectJustice()).isTrue();
        assertThat(planningScore.justice().averageJusticeScore()).isEqualTo(1.0);
        assertThat(planningScore.justice().minimumJusticeScore()).isEqualTo(1.0);
        assertThat(planningScore.justice().families()).singleElement().satisfies(justiceScore -> {
            assertThat(justiceScore.perfectJustice()).isTrue();
            assertThat(justiceScore.preferenceCompliance()).isTrue();
            assertThat(justiceScore.justiceScore()).isEqualTo(1.0);
        });
    }

    @Test
    void scores_driver_assignments_against_family_preferences() {
        Family family = singleChildFamily();
        NormalizedWorkbookFamily normalizedFamily = new NormalizedWorkbookFamily(
                family,
                List.of(
                        new NormalizedWorkbookFamily.FamilyPreference(WeekType.EVEN, WeekDay.MONDAY, TimeSlot.MORNING, "IMPOSSIBLE"),
                        new NormalizedWorkbookFamily.FamilyPreference(WeekType.EVEN, WeekDay.MONDAY, TimeSlot.EVENING, "EVITER"),
                        new NormalizedWorkbookFamily.FamilyPreference(WeekType.ODD, WeekDay.FRIDAY, TimeSlot.EVENING, "PREFERE")
                ),
                NormalizedWorkbookFamily.FamilyNotes.empty()
        );

        ScheduleResult scheduleResult = new ScheduleService().generateSchedule(List.of(family));
        PlanningScore planningScore = new PlanningScorer().score(scheduleResult, List.of(normalizedFamily));

        assertThat(planningScore.totalScore()).isEqualTo(-105);
        assertThat(planningScore.complete()).isTrue();
        assertThat(planningScore.totalRequiredTransportSlots()).isEqualTo(16);
        assertThat(planningScore.assignedRequiredTransportSlots()).isEqualTo(16);
        assertThat(planningScore.missingRequiredTransportSlots()).isEqualTo(0);
        assertThat(planningScore.completionRatio()).isEqualTo(1.0);
        assertThat(planningScore.impossibleAssignments()).isEqualTo(1);
        assertThat(planningScore.avoidAssignments()).isEqualTo(1);
        assertThat(planningScore.preferredAssignments()).isEqualTo(1);
        assertThat(planningScore.okAssignments()).isEqualTo(13);
        assertThat(planningScore.justice().perfectJustice()).isFalse();
        assertThat(planningScore.justice().minimumJusticeScore()).isEqualTo(0.0);
        assertThat(planningScore.families()).singleElement().satisfies(familyScore -> {
            assertThat(familyScore.familyName()).isEqualTo("Mael");
            assertThat(familyScore.totalScore()).isEqualTo(-105);
            assertThat(familyScore.impossibleAssignments()).isEqualTo(1);
            assertThat(familyScore.avoidAssignments()).isEqualTo(1);
            assertThat(familyScore.preferredAssignments()).isEqualTo(1);
            assertThat(familyScore.okAssignments()).isEqualTo(13);
            assertThat(familyScore.requiredTransportSlots()).isEqualTo(16);
            assertThat(familyScore.assignedTransportSlots()).isEqualTo(16);
            assertThat(familyScore.missingTransportSlots()).isEqualTo(0);
        });
        assertThat(planningScore.justice().families()).singleElement().satisfies(justiceScore -> {
            assertThat(justiceScore.familyName()).isEqualTo("Mael");
            assertThat(justiceScore.preferenceCompliance()).isFalse();
            assertThat(justiceScore.perfectJustice()).isFalse();
            assertThat(justiceScore.justiceScore()).isEqualTo(0.0);
            assertThat(justiceScore.actualMeanTripPerWeek()).isEqualTo(8.0);
            assertThat(justiceScore.perfectMeanTripPerWeek()).isEqualTo(8.0);
            assertThat(justiceScore.tripDeviation()).isEqualTo(0.0);
        });
    }

    @Test
    void marks_planning_incomplete_when_required_transports_are_missing() {
        Family family = singleChildFamily();
        NormalizedWorkbookFamily normalizedFamily = new NormalizedWorkbookFamily(
                family,
                List.of(),
                NormalizedWorkbookFamily.FamilyNotes.empty()
        );

        ScheduleResult scheduleResult = new ScheduleResult(
                new Schedule(WeekType.ODD, List.of(), List.of(family)),
                new Schedule(WeekType.EVEN, List.of(), List.of(family)),
                List.of(family)
        );

        PlanningScore planningScore = new PlanningScorer().score(scheduleResult, List.of(normalizedFamily));

        assertThat(planningScore.complete()).isFalse();
        assertThat(planningScore.totalRequiredTransportSlots()).isEqualTo(16);
        assertThat(planningScore.assignedRequiredTransportSlots()).isEqualTo(0);
        assertThat(planningScore.missingRequiredTransportSlots()).isEqualTo(16);
        assertThat(planningScore.completionRatio()).isEqualTo(0.0);
        assertThat(planningScore.justice().perfectJustice()).isFalse();
        assertThat(planningScore.justice().minimumJusticeScore()).isEqualTo(0.0);
        assertThat(planningScore.families()).singleElement().satisfies(familyScore -> {
            assertThat(familyScore.requiredTransportSlots()).isEqualTo(16);
            assertThat(familyScore.assignedTransportSlots()).isEqualTo(0);
            assertThat(familyScore.missingTransportSlots()).isEqualTo(16);
        });
    }

    private Family singleChildFamily() {
        Child child = new Child();
        child.id = 1L;
        child.name = "Mael";

        Family family = new Family();
        family.id = 1L;
        family.name = "Mael";
        family.carCapacity = 6;
        family.children = List.of(child);
        return family;
    }
}
