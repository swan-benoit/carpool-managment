package com.carpool.schedule;

import com.carpool.family.Child;
import com.carpool.family.Family;
import com.carpool.family.TimeSlot;
import com.carpool.family.WeekDay;
import com.carpool.family.WeekType;
import com.carpool.schedule.calculator.ScheduleResult;
import com.carpool.schedule.calculator.ScheduleService;
import com.carpool.workbook.normalization.NormalizedWorkbookFamily;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlanningScorerTest {

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
        assertThat(planningScore.impossibleAssignments()).isEqualTo(1);
        assertThat(planningScore.avoidAssignments()).isEqualTo(1);
        assertThat(planningScore.preferredAssignments()).isEqualTo(1);
        assertThat(planningScore.okAssignments()).isEqualTo(13);
        assertThat(planningScore.families()).singleElement().satisfies(familyScore -> {
            assertThat(familyScore.familyName()).isEqualTo("Mael");
            assertThat(familyScore.totalScore()).isEqualTo(-105);
            assertThat(familyScore.impossibleAssignments()).isEqualTo(1);
            assertThat(familyScore.avoidAssignments()).isEqualTo(1);
            assertThat(familyScore.preferredAssignments()).isEqualTo(1);
            assertThat(familyScore.okAssignments()).isEqualTo(13);
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
