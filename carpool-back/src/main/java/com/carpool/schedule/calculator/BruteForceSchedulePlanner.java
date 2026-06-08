package com.carpool.schedule.calculator;

import com.carpool.family.Family;
import com.carpool.family.TimeSlot;
import com.carpool.family.WeekDay;
import com.carpool.family.WeekType;
import com.carpool.schedule.FamilyPlanningStats;
import com.carpool.schedule.PlanningScore;
import com.carpool.schedule.PlanningScorer;
import com.carpool.schedule.PreferenceValue;
import com.carpool.workbook.normalization.NormalizedWorkbookFamily;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class BruteForceSchedulePlanner {

    public static final long DEFAULT_MAX_EXPLORED_STATES = 200_000L;
    public static final double DEFAULT_MAX_SECONDS = 10.0;

    private final PlanningScorer planningScorer;

    public BruteForceSchedulePlanner() {
        this(new PlanningScorer());
    }

    BruteForceSchedulePlanner(PlanningScorer planningScorer) {
        this.planningScorer = planningScorer;
    }

    public SearchResult generateBestSchedule(List<NormalizedWorkbookFamily> normalizedFamilies) {
        return generateBestSchedule(normalizedFamilies, DEFAULT_MAX_EXPLORED_STATES);
    }

    public SearchResult generateBestSchedule(List<NormalizedWorkbookFamily> normalizedFamilies, long maxExploredStates) {
        SearchSummary summary = generateTopSchedules(normalizedFamilies, maxExploredStates, 1);
        if (!summary.candidates().isEmpty()) {
            SearchCandidate best = summary.candidates().getFirst();
            return new SearchResult(best.scheduleResult(), best.planningScore(), summary.exploredStates(), summary.searchCompleted());
        }

        List<Family> families = normalizedFamilies.stream().map(NormalizedWorkbookFamily::family).toList();
        ScheduleResult empty = ScheduleResult.empty(families);
        return new SearchResult(empty, planningScorer.score(empty, normalizedFamilies), summary.exploredStates(), summary.searchCompleted());
    }

    public SearchSummary generateTopSchedules(List<NormalizedWorkbookFamily> normalizedFamilies, long maxExploredStates, int topCount) {
        return generateTopSchedules(normalizedFamilies, maxExploredStates, topCount, DEFAULT_MAX_SECONDS);
    }

    public SearchSummary generateTopSchedules(List<NormalizedWorkbookFamily> normalizedFamilies, long maxExploredStates, int topCount, double maxSeconds) {
        List<Family> families = normalizedFamilies.stream().map(NormalizedWorkbookFamily::family).toList();
        SearchState searchState = new SearchState(maxExploredStates, Math.max(1, topCount), orderedSlots(families), maxSeconds);
        exploreSchedule(ScheduleResult.empty(families), normalizedFamilies, 0, searchState);

        return new SearchSummary(
                List.copyOf(searchState.topCandidates),
                searchState.exploredStates,
                searchState.searchCompleted
        );
    }

    private void exploreSchedule(
            ScheduleResult current,
            List<NormalizedWorkbookFamily> normalizedFamilies,
            int slotIndex,
            SearchState searchState
    ) {
        if (searchState.exploredStates >= searchState.maxExploredStates) {
            searchState.searchCompleted = false;
            return;
        }
        if (searchState.isTimeLimitReached()) {
            searchState.searchCompleted = false;
            return;
        }

        if (slotIndex >= searchState.orderedSlots.size()) {
            searchState.exploredStates++;
            evaluate(current, normalizedFamilies, searchState);
            return;
        }

        SlotRef slot = searchState.orderedSlots.get(slotIndex);
        if (!current.hasChildrenToTransport(slot.weekType(), slot.weekDay(), slot.timeSlot())) {
            exploreSchedule(current, normalizedFamilies, slotIndex + 1, searchState);
            return;
        }

        exploreSlot(current, normalizedFamilies, slotIndex, new ArrayList<>(current.families()), searchState);
    }

    private void exploreSlot(
            ScheduleResult current,
            List<NormalizedWorkbookFamily> normalizedFamilies,
            int slotIndex,
            List<Family> remainingDrivers,
            SearchState searchState
    ) {
        if (searchState.exploredStates >= searchState.maxExploredStates) {
            searchState.searchCompleted = false;
            return;
        }
        if (searchState.isTimeLimitReached()) {
            searchState.searchCompleted = false;
            return;
        }

        SlotRef slot = searchState.orderedSlots.get(slotIndex);
        if (current.isTripFull(slot.weekType(), slot.weekDay(), slot.timeSlot())) {
            exploreSchedule(current, normalizedFamilies, slotIndex + 1, searchState);
            return;
        }

        if (searchState.bestScore != null && shouldPruneByOptimisticScore(current, normalizedFamilies, slotIndex, searchState.bestScore)) {
            return;
        }

        if (isCompletenessImpossible(current, slot, remainingDrivers)) {
            searchState.exploredStates++;
            evaluate(current, normalizedFamilies, searchState);
            return;
        }

        boolean branched = false;
        for (int index = 0; index < remainingDrivers.size(); index++) {
            Family driver = remainingDrivers.get(index);
            List<com.carpool.family.Child> children = current.childrenCandidates(slot.weekType(), slot.weekDay(), slot.timeSlot(), driver);
            if (children.isEmpty()) {
                continue;
            }

            ScheduleResult next;
            try {
                next = current.addTrip(slot.weekType(), slot.weekDay(), slot.timeSlot(), driver, children);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            branched = true;
            List<Family> nextRemainingDrivers = new ArrayList<>(remainingDrivers);
            nextRemainingDrivers.remove(index);
            exploreSlot(next, normalizedFamilies, slotIndex, nextRemainingDrivers, searchState);
        }

        if (!branched) {
            searchState.exploredStates++;
            evaluate(current, normalizedFamilies, searchState);
        }
    }

    private void evaluate(ScheduleResult candidate, List<NormalizedWorkbookFamily> normalizedFamilies, SearchState searchState) {
        PlanningScore candidateScore = planningScorer.score(candidate, normalizedFamilies);
        if (searchState.bestScore == null || isBetter(candidateScore, searchState.bestScore)) {
            searchState.bestResult = candidate;
            searchState.bestScore = candidateScore;
        }

        if (candidateScore.complete()) {
            addTopCandidate(new SearchCandidate(candidate, candidateScore), searchState);
        }
    }

    private void addTopCandidate(SearchCandidate candidate, SearchState searchState) {
        String candidateKey = planningSignature(candidate.scheduleResult());
        boolean alreadyPresent = searchState.topCandidates.stream()
                .anyMatch(existing -> planningSignature(existing.scheduleResult()).equals(candidateKey));
        if (alreadyPresent) {
            return;
        }

        searchState.topCandidates.add(candidate);
        searchState.topCandidates.sort((left, right) -> compareScores(right.planningScore(), left.planningScore()));
        if (searchState.topCandidates.size() > searchState.topCount) {
            searchState.topCandidates.removeLast();
        }
    }

    private boolean isBetter(PlanningScore candidate, PlanningScore incumbent) {
        return compareScores(candidate, incumbent) > 0;
    }

    private int compareScores(PlanningScore candidate, PlanningScore incumbent) {
        if (candidate.complete() != incumbent.complete()) {
            return candidate.complete() ? 1 : -1;
        }
        if (candidate.assignedRequiredTransportSlots() != incumbent.assignedRequiredTransportSlots()) {
            return Integer.compare(candidate.assignedRequiredTransportSlots(), incumbent.assignedRequiredTransportSlots());
        }
        if (Double.compare(candidate.justice().minimumJusticeScore(), incumbent.justice().minimumJusticeScore()) != 0) {
            return Double.compare(candidate.justice().minimumJusticeScore(), incumbent.justice().minimumJusticeScore());
        }
        if (Double.compare(candidate.justice().averageJusticeScore(), incumbent.justice().averageJusticeScore()) != 0) {
            return Double.compare(candidate.justice().averageJusticeScore(), incumbent.justice().averageJusticeScore());
        }
        if (candidate.totalScore() != incumbent.totalScore()) {
            return Integer.compare(candidate.totalScore(), incumbent.totalScore());
        }
        if (candidate.impossibleAssignments() != incumbent.impossibleAssignments()) {
            return Integer.compare(incumbent.impossibleAssignments(), candidate.impossibleAssignments());
        }
        if (candidate.avoidAssignments() != incumbent.avoidAssignments()) {
            return Integer.compare(incumbent.avoidAssignments(), candidate.avoidAssignments());
        }
        return Integer.compare(candidate.preferredAssignments(), incumbent.preferredAssignments());
    }

    static String planningSignature(ScheduleResult scheduleResult) {
        return signatureForWeek(WeekType.EVEN, scheduleResult.even().trips())
                + "||"
                + signatureForWeek(WeekType.ODD, scheduleResult.odd().trips());
    }

    private static String signatureForWeek(WeekType weekType, List<Trip> trips) {
        return trips.stream()
                .sorted(Comparator
                        .comparing(Trip::weekDay)
                        .thenComparing(Trip::timeSlot))
                .map(trip -> weekType.name()
                        + "|"
                        + trip.weekDay().name()
                        + "|"
                        + trip.timeSlot().name()
                        + "|"
                        + trip.cars().Assignments().stream()
                                .sorted(Comparator.comparing(assignment -> assignment.driverFamily().name))
                                .map(assignment -> assignment.driverFamily().name
                                        + ":"
                                        + assignment.children().stream()
                                                .map(child -> child.name)
                                                .sorted()
                                                .collect(Collectors.joining(",", "[", "]")))
                                .collect(Collectors.joining(";")))
                .collect(Collectors.joining("#"));
    }

    private boolean isCompletenessImpossible(ScheduleResult current, SlotRef slot, List<Family> remainingDrivers) {
        int unassignedChildren = current.unassignedChildrenCount(slot.weekType(), slot.weekDay(), slot.timeSlot());
        int remainingCapacity = remainingDrivers.stream().mapToInt(family -> family.carCapacity).sum();
        return remainingCapacity < unassignedChildren;
    }

    private boolean shouldPruneByOptimisticScore(
            ScheduleResult current,
            List<NormalizedWorkbookFamily> normalizedFamilies,
            int slotIndex,
            PlanningScore incumbent
    ) {
        PlanningScore currentScore = planningScorer.score(current, normalizedFamilies);
        if (!incumbent.complete() && currentScore.complete()) {
            return false;
        }

        int remainingSlotCount = searchStateOrderedSlotCount(current, slotIndex);
        int familyCount = current.families().size();
        int optimisticAdditionalScore = remainingSlotCount * familyCount * PreferenceValue.PREFERE.weight();
        int optimisticTotalScore = currentScore.totalScore() + optimisticAdditionalScore;

        if (currentScore.assignedRequiredTransportSlots() > incumbent.assignedRequiredTransportSlots()) {
            return false;
        }
        if (currentScore.assignedRequiredTransportSlots() == incumbent.assignedRequiredTransportSlots()
                && optimisticTotalScore <= incumbent.totalScore()) {
            return true;
        }
        return false;
    }

    private int searchStateOrderedSlotCount(ScheduleResult current, int slotIndex) {
        return WeekType.values().length * WeekDay.values().length * TimeSlot.values().length - slotIndex;
    }

    static List<SlotRef> orderedSlots(List<Family> families) {
        List<SlotRef> slots = new ArrayList<>();
        for (WeekType weekType : WeekType.values()) {
            for (WeekDay weekDay : WeekDay.values()) {
                for (TimeSlot timeSlot : TimeSlot.values()) {
                    int presentChildren = (int) families.stream()
                            .flatMap(family -> family.children.stream())
                            .filter(child -> FamilyPlanningStats.isPresent(child, weekType, weekDay, timeSlot))
                            .count();
                    int requiredTrips = FamilyPlanningStats.requiredTripsForSlot(families, weekType, weekDay, timeSlot);
                    int totalCapacity = families.stream().mapToInt(family -> family.carCapacity).sum();
                    int slack = totalCapacity - presentChildren;
                    slots.add(new SlotRef(weekType, weekDay, timeSlot, presentChildren, requiredTrips, slack));
                }
            }
        }

        return slots.stream()
                .sorted(Comparator
                        .comparingInt(SlotRef::requiredTrips).reversed()
                        .thenComparingInt(SlotRef::presentChildren).reversed()
                        .thenComparingInt(SlotRef::slack)
                        .thenComparing(SlotRef::weekType)
                        .thenComparing(SlotRef::weekDay)
                        .thenComparing(SlotRef::timeSlot))
                .toList();
    }

    public record SearchResult(
            ScheduleResult scheduleResult,
            PlanningScore planningScore,
            long exploredStates,
            boolean searchCompleted
    ) {
    }

    public record SearchSummary(
            List<SearchCandidate> candidates,
            long exploredStates,
            boolean searchCompleted
    ) {
    }

    public record SearchCandidate(
            ScheduleResult scheduleResult,
            PlanningScore planningScore
    ) {
    }

    static record SlotRef(WeekType weekType, WeekDay weekDay, TimeSlot timeSlot, int presentChildren, int requiredTrips, int slack) {
    }

    private static final class SearchState {
        private final long maxExploredStates;
        private final int topCount;
        private final List<SlotRef> orderedSlots;
        private final long deadlineNanos;
        private final List<SearchCandidate> topCandidates = new ArrayList<>();
        private long exploredStates;
        private boolean searchCompleted = true;
        private ScheduleResult bestResult;
        private PlanningScore bestScore;

        private SearchState(long maxExploredStates, int topCount, List<SlotRef> orderedSlots, double maxSeconds) {
            this.maxExploredStates = maxExploredStates;
            this.topCount = topCount;
            this.orderedSlots = orderedSlots;
            this.deadlineNanos = System.nanoTime() + (long) (maxSeconds * 1_000_000_000L);
        }

        private boolean isTimeLimitReached() {
            return System.nanoTime() >= deadlineNanos;
        }
    }
}
