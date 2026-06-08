package com.carpool.schedule.calculator;

import com.carpool.family.Child;
import com.carpool.family.Family;
import com.carpool.family.TimeSlot;
import com.carpool.family.WeekDay;
import com.carpool.family.WeekType;
import com.carpool.schedule.PlanningScore;
import com.carpool.schedule.PlanningScorer;
import com.carpool.workbook.normalization.NormalizedWorkbookFamily;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BruteForceSchedulePlannerTest {

    @Test
    void brute_force_improves_preference_score_over_greedy_on_small_case() {
        NormalizedWorkbookFamily familyA = normalizedFamily("Famille A", "Alice", "IMPOSSIBLE");
        NormalizedWorkbookFamily familyB = normalizedFamily("Famille B", "Bob", "PREFERE");
        List<NormalizedWorkbookFamily> families = List.of(familyA, familyB);

        List<Family> rawFamilies = families.stream().map(NormalizedWorkbookFamily::family).toList();
        PlanningScorer scorer = new PlanningScorer();

        ScheduleResult greedy = new ScheduleService().generateSchedule(rawFamilies);
        PlanningScore greedyScore = scorer.score(greedy, families);

        BruteForceSchedulePlanner.SearchResult bruteForce = new BruteForceSchedulePlanner(scorer)
                .generateBestSchedule(families, 500_000);

        assertThat(bruteForce.searchCompleted()).isTrue();
        assertThat(bruteForce.planningScore().complete()).isTrue();
        assertThat(bruteForce.planningScore().totalScore()).isGreaterThan(greedyScore.totalScore());
        assertThat(bruteForce.planningScore().impossibleAssignments()).isLessThan(greedyScore.impossibleAssignments());
    }

    @Test
    void returns_top_complete_plans_ranked_by_justice() {
        NormalizedWorkbookFamily familyA = normalizedFamily("Famille A", "Alice", "IMPOSSIBLE");
        NormalizedWorkbookFamily familyB = normalizedFamily("Famille B", "Bob", "PREFERE");
        List<NormalizedWorkbookFamily> families = List.of(familyA, familyB);

        BruteForceSchedulePlanner.SearchSummary summary = new BruteForceSchedulePlanner(new PlanningScorer())
                .generateTopSchedules(families, 500_000, 2);

        assertThat(summary.candidates()).hasSizeLessThanOrEqualTo(2);
        assertThat(summary.candidates()).allSatisfy(candidate -> assertThat(candidate.planningScore().complete()).isTrue());
        if (summary.candidates().size() == 2) {
            assertThat(summary.candidates().get(0).planningScore().justice().minimumJusticeScore())
                    .isGreaterThanOrEqualTo(summary.candidates().get(1).planningScore().justice().minimumJusticeScore());
        }
    }

    @Test
    void orders_most_constrained_slots_first() {
        NormalizedWorkbookFamily familyA = normalizedFamilyWithAbsence("Famille A", "Alice", WeekType.EVEN, WeekDay.MONDAY, TimeSlot.MORNING);
        NormalizedWorkbookFamily familyB = normalizedFamily("Famille B", "Bob", "OK");
        List<BruteForceSchedulePlanner.SlotRef> orderedSlots = BruteForceSchedulePlanner.orderedSlots(
                List.of(familyA.family(), familyB.family()),
                0L
        );

        assertThat(orderedSlots.getFirst().presentChildren()).isEqualTo(2);
        assertThat(orderedSlots.getFirst().requiredTrips()).isEqualTo(1);
        assertThat(orderedSlots.getLast().weekType()).isEqualTo(WeekType.EVEN);
        assertThat(orderedSlots.getLast().weekDay()).isEqualTo(WeekDay.MONDAY);
        assertThat(orderedSlots.getLast().timeSlot()).isEqualTo(TimeSlot.MORNING);
    }

    @Test
    void canonical_signature_ignores_assignment_insertion_order() {
        Family driverA = family("Famille A", "Alice");
        Family driverB = family("Famille B", "Bob");
        List<Family> families = List.of(driverA, driverB);

        Trip tripLeft = new Trip(
                WeekDay.MONDAY,
                TimeSlot.MORNING,
                WeekType.EVEN,
                new Cars(List.of(
                        new Assignment(driverA, List.of(driverA.children.getFirst())),
                        new Assignment(driverB, List.of(driverB.children.getFirst()))
                )),
                families
        );
        Trip tripRight = new Trip(
                WeekDay.MONDAY,
                TimeSlot.MORNING,
                WeekType.EVEN,
                new Cars(List.of(
                        new Assignment(driverB, List.of(driverB.children.getFirst())),
                        new Assignment(driverA, List.of(driverA.children.getFirst()))
                )),
                families
        );

        ScheduleResult left = new ScheduleResult(
                new Schedule(WeekType.ODD, List.of(), families),
                new Schedule(WeekType.EVEN, List.of(tripLeft), families),
                families
        );
        ScheduleResult right = new ScheduleResult(
                new Schedule(WeekType.ODD, List.of(), families),
                new Schedule(WeekType.EVEN, List.of(tripRight), families),
                families
        );

        assertThat(BruteForceSchedulePlanner.planningSignature(left)).isEqualTo(BruteForceSchedulePlanner.planningSignature(right));
    }

    @Test
    void respects_family_trip_cap() {
        NormalizedWorkbookFamily familyA = normalizedFamily("Famille A", "Alice", "OK");
        NormalizedWorkbookFamily familyB = normalizedFamily("Famille B", "Bob", "OK");
        List<NormalizedWorkbookFamily> families = List.of(familyA, familyB);

        BruteForceSchedulePlanner.SearchSummary summary = new BruteForceSchedulePlanner(new PlanningScorer())
                .generateTopSchedules(families, 500_000, 1, 10.0, 0L, 1, Map.of("Famille A", 3.0));

        assertThat(summary.candidates()).isNotEmpty();
        PlanningScore score = summary.candidates().getFirst().planningScore();
        assertThat(score.complete()).isTrue();
        assertThat(score.justice().families().stream()
                .filter(family -> family.familyName().equals("Famille A"))
                .findFirst()
                .orElseThrow()
                .actualMeanTripPerWeek()).isLessThanOrEqualTo(3.0);
    }

    private NormalizedWorkbookFamily normalizedFamily(String familyName, String childName, String mondayEvenPreference) {
        Child child = new Child();
        child.id = (long) childName.hashCode();
        child.name = childName;

        Family family = new Family();
        family.id = (long) familyName.hashCode();
        family.name = familyName;
        family.carCapacity = 2;
        family.children = List.of(child);

        return new NormalizedWorkbookFamily(
                family,
                List.of(new NormalizedWorkbookFamily.FamilyPreference(WeekType.EVEN, WeekDay.MONDAY, TimeSlot.MORNING, mondayEvenPreference)),
                NormalizedWorkbookFamily.FamilyNotes.empty()
        );
    }

    private NormalizedWorkbookFamily normalizedFamilyWithAbsence(String familyName, String childName, WeekType weekType, WeekDay weekDay, TimeSlot timeSlot) {
        Child child = new Child();
        child.id = (long) childName.hashCode();
        child.name = childName;
        var absence = new com.carpool.family.AbsenceDays();
        absence.weekType = weekType;
        absence.weekDay = weekDay;
        absence.timeSlot = timeSlot;
        child.absenceDays.add(absence);

        Family family = new Family();
        family.id = (long) familyName.hashCode();
        family.name = familyName;
        family.carCapacity = 2;
        family.children = List.of(child);

        return new NormalizedWorkbookFamily(
                family,
                List.of(),
                NormalizedWorkbookFamily.FamilyNotes.empty()
        );
    }

    private Family family(String familyName, String childName) {
        Child child = new Child();
        child.id = (long) childName.hashCode();
        child.name = childName;

        Family family = new Family();
        family.id = (long) familyName.hashCode();
        family.name = familyName;
        family.carCapacity = 2;
        family.children = new ArrayList<>(List.of(child));
        return family;
    }
}
