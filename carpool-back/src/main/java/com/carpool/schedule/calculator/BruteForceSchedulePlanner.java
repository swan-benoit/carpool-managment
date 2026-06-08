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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class BruteForceSchedulePlanner {

    public static final long DEFAULT_MAX_EXPLORED_STATES = 200_000L;
    public static final double DEFAULT_MAX_SECONDS = 10.0;
    public static final long UNLIMITED_MAX_EXPLORED_STATES = Long.MAX_VALUE;
    public static final double UNLIMITED_MAX_SECONDS = Double.POSITIVE_INFINITY;
    public static final int DEFAULT_RESTARTS = 1;
    private static final int CANDIDATE_POOL_FACTOR = 20;

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
        return generateTopSchedules(normalizedFamilies, maxExploredStates, topCount, maxSeconds, 0L, DEFAULT_RESTARTS);
    }

    public SearchSummary generateTopSchedules(
            List<NormalizedWorkbookFamily> normalizedFamilies,
            long maxExploredStates,
            int topCount,
            double maxSeconds,
            long seed,
            int restarts
    ) {
        return generateTopSchedules(normalizedFamilies, maxExploredStates, topCount, maxSeconds, seed, restarts, Map.of());
    }

    public SearchSummary generateTopSchedules(
            List<NormalizedWorkbookFamily> normalizedFamilies,
            long maxExploredStates,
            int topCount,
            double maxSeconds,
            long seed,
            int restarts,
            Map<String, Double> maxFamilyTrips
    ) {
        List<Family> families = normalizedFamilies.stream().map(NormalizedWorkbookFamily::family).toList();
        int candidatePoolSize = Math.max(1, topCount) * CANDIDATE_POOL_FACTOR;
        List<SearchCandidate> mergedCandidates = new ArrayList<>();
        long exploredStates = 0;
        boolean searchCompleted = true;
        long globalDeadlineNanos = Double.isInfinite(maxSeconds)
                ? Long.MAX_VALUE
                : System.nanoTime() + (long) (maxSeconds * 1_000_000_000L);

        for (int restartIndex = 0; restartIndex < Math.max(1, restarts); restartIndex++) {
            double remainingSeconds = Double.isInfinite(maxSeconds)
                    ? Double.POSITIVE_INFINITY
                    : Math.max(0.0, (globalDeadlineNanos - System.nanoTime()) / 1_000_000_000.0);
            if (!Double.isInfinite(maxSeconds) && remainingSeconds <= 0.0) {
                searchCompleted = false;
                break;
            }

            System.err.println("[planner] restart " + (restartIndex + 1) + "/" + Math.max(1, restarts)
                    + " seed=" + (seed + restartIndex)
                    + (Double.isInfinite(remainingSeconds) ? " remaining=unlimited" : " remaining=" + String.format(java.util.Locale.US, "%.1fs", remainingSeconds)));

            SearchState searchState = new SearchState(
                    maxExploredStates,
                    Math.max(1, topCount),
                    candidatePoolSize,
                    orderedSlots(families, seed + restartIndex),
                    remainingSeconds,
                    buildPreferenceMap(normalizedFamilies),
                    maxFamilyTrips,
                    seed + restartIndex,
                    restartIndex + 1,
                    Math.max(1, restarts)
            );
            exploreSchedule(ScheduleResult.empty(families), normalizedFamilies, 0, searchState);
            exploredStates += searchState.exploredStates;
            searchCompleted &= searchState.searchCompleted;
            for (SearchCandidate candidate : searchState.topCandidates) {
                addMergedCandidate(candidate, mergedCandidates, candidatePoolSize);
            }

            System.err.println("[planner] restart " + (restartIndex + 1)
                    + " done explored=" + searchState.exploredStates
                    + " completeCandidates=" + searchState.topCandidates.size()
                    + " finished=" + searchState.searchCompleted);
        }

        List<SearchCandidate> selectedCandidates = selectDiverseCandidates(mergedCandidates, Math.max(1, topCount));

        return new SearchSummary(
                List.copyOf(selectedCandidates),
                exploredStates,
                searchCompleted
        );
    }

    private void exploreSchedule(
            ScheduleResult current,
            List<NormalizedWorkbookFamily> normalizedFamilies,
            int slotIndex,
            SearchState searchState
    ) {
        if (searchState.isStateLimitReached()) {
            searchState.searchCompleted = false;
            return;
        }
        if (searchState.isTimeLimitReached()) {
            searchState.searchCompleted = false;
            return;
        }
        searchState.reportProgressIfNeeded();

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
        if (searchState.isStateLimitReached()) {
            searchState.searchCompleted = false;
            return;
        }
        if (searchState.isTimeLimitReached()) {
            searchState.searchCompleted = false;
            return;
        }
        searchState.reportProgressIfNeeded();

        SlotRef slot = searchState.orderedSlots.get(slotIndex);
        if (current.isTripFull(slot.weekType(), slot.weekDay(), slot.timeSlot())) {
            exploreSchedule(current, normalizedFamilies, slotIndex + 1, searchState);
            return;
        }

        if (searchState.bestScore != null && shouldPruneByOptimisticScore(current, normalizedFamilies, slotIndex, searchState.bestScore)) {
            return;
        }

        if (isCompletenessImpossible(current, slot, remainingDrivers, searchState.maxFamilyTrips)) {
            searchState.exploredStates++;
            evaluate(current, normalizedFamilies, searchState);
            return;
        }

        String slotKey = slot.weekType().name() + "|" + slot.weekDay().name() + "|" + slot.timeSlot().name();
        List<Family> sortedDrivers = remainingDrivers.stream()
                .filter(driver -> canAssignMoreTrips(current.meanTripPerWeek(driver), driver, searchState.maxFamilyTrips))
                .sorted(Comparator
                        .comparingInt((Family f) -> preferenceGroupForSlot(f, slotKey, searchState.preferenceMap))
                        .thenComparingDouble(f -> current.meanTripPerWeek(f) / Math.max(0.001, current.perfectMeanTripPerWeek(f)))
                        .thenComparingInt(f -> preferenceRankForSlot(f, slotKey, searchState.preferenceMap))
                        .thenComparingDouble(f -> searchState.randomOrderFor(f.name, slotKey)))
                .toList();

        boolean branched = false;
        for (Family driver : sortedDrivers) {
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
            nextRemainingDrivers.remove(driver);
            exploreSlot(next, normalizedFamilies, slotIndex, nextRemainingDrivers, searchState);
        }

        if (!branched) {
            searchState.exploredStates++;
            evaluate(current, normalizedFamilies, searchState);
        }
    }

    private void evaluate(ScheduleResult candidate, List<NormalizedWorkbookFamily> normalizedFamilies, SearchState searchState) {
        if (!respectsFamilyTripCaps(candidate, searchState.maxFamilyTrips)) {
            return;
        }
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
        if (searchState.topCandidates.size() > searchState.candidatePoolSize) {
            searchState.topCandidates.removeLast();
        }
    }

    private void addMergedCandidate(SearchCandidate candidate, List<SearchCandidate> mergedCandidates, int topCount) {
        String candidateKey = planningSignature(candidate.scheduleResult());
        boolean alreadyPresent = mergedCandidates.stream()
                .anyMatch(existing -> planningSignature(existing.scheduleResult()).equals(candidateKey));
        if (alreadyPresent) {
            return;
        }

        mergedCandidates.add(candidate);
        mergedCandidates.sort((left, right) -> compareScores(right.planningScore(), left.planningScore()));
        if (mergedCandidates.size() > topCount) {
            mergedCandidates.removeLast();
        }
    }

    private List<SearchCandidate> selectDiverseCandidates(List<SearchCandidate> candidatePool, int topCount) {
        if (candidatePool.isEmpty()) {
            return List.of();
        }

        List<SearchCandidate> sortedPool = candidatePool.stream()
                .sorted((left, right) -> compareScores(right.planningScore(), left.planningScore()))
                .toList();
        List<SearchCandidate> selected = new ArrayList<>();
        selected.add(sortedPool.getFirst());

        List<SearchCandidate> remaining = new ArrayList<>(sortedPool.subList(1, sortedPool.size()));
        while (selected.size() < topCount && !remaining.isEmpty()) {
            SearchCandidate bestCandidate = remaining.stream()
                    .max((left, right) -> compareDiversityAware(left, right, selected))
                    .orElse(null);
            if (bestCandidate == null) {
                break;
            }
            selected.add(bestCandidate);
            remaining.remove(bestCandidate);
        }
        return List.copyOf(selected);
    }

    private int compareDiversityAware(SearchCandidate left, SearchCandidate right, List<SearchCandidate> selected) {
        double leftDistance = minDistanceToSelected(left, selected);
        double rightDistance = minDistanceToSelected(right, selected);
        if (Double.compare(leftDistance, rightDistance) != 0) {
            return Double.compare(leftDistance, rightDistance);
        }
        return compareScores(left.planningScore(), right.planningScore());
    }

    private double minDistanceToSelected(SearchCandidate candidate, List<SearchCandidate> selected) {
        return selected.stream()
                .mapToDouble(existing -> planningDistance(candidate.scheduleResult(), existing.scheduleResult()))
                .min()
                .orElse(1.0);
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
        if (candidate.redundantDrivers() != incumbent.redundantDrivers()) {
            return Integer.compare(incumbent.redundantDrivers(), candidate.redundantDrivers());
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

    public static double planningDistance(ScheduleResult left, ScheduleResult right) {
        List<String> leftSlots = slotSignatures(left);
        List<String> rightSlots = slotSignatures(right);
        int differences = 0;
        for (int index = 0; index < leftSlots.size(); index++) {
            if (!leftSlots.get(index).equals(rightSlots.get(index))) {
                differences++;
            }
        }
        return differences / (double) leftSlots.size();
    }

    private static List<String> slotSignatures(ScheduleResult scheduleResult) {
        List<String> signatures = new ArrayList<>();
        for (WeekType weekType : WeekType.values()) {
            Schedule schedule = weekType == WeekType.EVEN ? scheduleResult.even() : scheduleResult.odd();
            for (WeekDay weekDay : WeekDay.values()) {
                for (TimeSlot timeSlot : TimeSlot.values()) {
                    signatures.add(slotSignature(schedule, weekType, weekDay, timeSlot));
                }
            }
        }
        return signatures;
    }

    private static String slotSignature(Schedule schedule, WeekType weekType, WeekDay weekDay, TimeSlot timeSlot) {
        return schedule.trips().stream()
                .filter(trip -> trip.weekDay() == weekDay && trip.timeSlot() == timeSlot)
                .findFirst()
                .map(trip -> weekType.name() + "|" + weekDay.name() + "|" + timeSlot.name() + "|" + trip.cars().Assignments().stream()
                        .sorted(Comparator.comparing(assignment -> assignment.driverFamily().name))
                        .map(assignment -> assignment.driverFamily().name
                                + ":"
                                + assignment.children().stream().map(child -> child.name).sorted().collect(Collectors.joining(",", "[", "]")))
                        .collect(Collectors.joining(";")))
                .orElse(weekType.name() + "|" + weekDay.name() + "|" + timeSlot.name() + "|");
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

    private boolean isCompletenessImpossible(ScheduleResult current, SlotRef slot, List<Family> remainingDrivers, Map<String, Double> maxFamilyTrips) {
        int unassignedChildren = current.unassignedChildrenCount(slot.weekType(), slot.weekDay(), slot.timeSlot());
        int remainingCapacity = remainingDrivers.stream()
                .filter(driver -> canAssignMoreTrips(current.meanTripPerWeek(driver), driver, maxFamilyTrips))
                .mapToInt(family -> family.carCapacity)
                .sum();
        return remainingCapacity < unassignedChildren;
    }

    private boolean canAssignMoreTrips(double currentMeanTripPerWeek, Family driver, Map<String, Double> maxFamilyTrips) {
        Double maxTrips = maxFamilyTrips.get(driver.name);
        if (maxTrips == null) {
            return true;
        }
        return currentMeanTripPerWeek + 0.5 <= maxTrips + 1.0e-9;
    }

    private boolean respectsFamilyTripCaps(ScheduleResult scheduleResult, Map<String, Double> maxFamilyTrips) {
        for (Family family : scheduleResult.families()) {
            Double maxTrips = maxFamilyTrips.get(family.name);
            if (maxTrips == null) {
                continue;
            }
            if (scheduleResult.meanTripPerWeek(family) > maxTrips + 1.0e-9) {
                return false;
            }
        }
        return true;
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
        if (Double.compare(currentScore.justice().minimumJusticeScore(), incumbent.justice().minimumJusticeScore()) > 0) {
            return false;
        }
        if (currentScore.redundantDrivers() < incumbent.redundantDrivers()) {
            return false;
        }
        if (currentScore.assignedRequiredTransportSlots() == incumbent.assignedRequiredTransportSlots()
                && optimisticTotalScore < incumbent.totalScore()) {
            return true;
        }
        return false;
    }

    private int searchStateOrderedSlotCount(ScheduleResult current, int slotIndex) {
        return WeekType.values().length * WeekDay.values().length * TimeSlot.values().length - slotIndex;
    }

    private static Map<String, Map<String, PreferenceValue>> buildPreferenceMap(List<NormalizedWorkbookFamily> normalizedFamilies) {
        Map<String, Map<String, PreferenceValue>> map = new HashMap<>();
        for (NormalizedWorkbookFamily nf : normalizedFamilies) {
            Map<String, PreferenceValue> slotMap = new HashMap<>();
            for (NormalizedWorkbookFamily.FamilyPreference pref : nf.preferences()) {
                String key = pref.weekType().name() + "|" + pref.weekDay().name() + "|" + pref.timeSlot().name();
                slotMap.put(key, PreferenceValue.fromWorkbookValue(pref.value()));
            }
            map.put(nf.family().name, slotMap);
        }
        return map;
    }

    private static int preferenceGroupForSlot(Family family, String slotKey, Map<String, Map<String, PreferenceValue>> preferenceMap) {
        PreferenceValue pref = preferenceMap.getOrDefault(family.name, Map.of()).getOrDefault(slotKey, PreferenceValue.OK);
        return switch (pref) {
            case PREFERE, OK -> 0;
            case EVITER -> 1;
            case IMPOSSIBLE -> 2;
        };
    }

    private static int preferenceRankForSlot(Family family, String slotKey, Map<String, Map<String, PreferenceValue>> preferenceMap) {
        PreferenceValue pref = preferenceMap.getOrDefault(family.name, Map.of()).getOrDefault(slotKey, PreferenceValue.OK);
        return switch (pref) {
            case PREFERE -> 0;
            case OK -> 1;
            case EVITER -> 2;
            case IMPOSSIBLE -> 3;
        };
    }

    static List<SlotRef> orderedSlots(List<Family> families) {
        return orderedSlots(families, 0L);
    }

    static List<SlotRef> orderedSlots(List<Family> families, long seed) {
        Random random = new Random(seed);
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
                    slots.add(new SlotRef(weekType, weekDay, timeSlot, presentChildren, requiredTrips, slack, random.nextDouble()));
                }
            }
        }

        return slots.stream()
                .sorted(Comparator
                        .comparingInt(SlotRef::requiredTrips).reversed()
                        .thenComparingInt(SlotRef::presentChildren).reversed()
                        .thenComparingInt(SlotRef::slack)
                        .thenComparingDouble(SlotRef::tieBreaker)
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

    static record SlotRef(WeekType weekType, WeekDay weekDay, TimeSlot timeSlot, int presentChildren, int requiredTrips, int slack, double tieBreaker) {
    }

    private static final class SearchState {
        private final long maxExploredStates;
        private final int topCount;
        private final int candidatePoolSize;
        private final List<SlotRef> orderedSlots;
        private final long deadlineNanos;
        private final boolean unlimitedTime;
        private final Map<String, Map<String, PreferenceValue>> preferenceMap;
        private final Map<String, Double> maxFamilyTrips;
        private final Random random;
        private final Map<String, Double> randomOrderCache = new HashMap<>();
        private final int restartNumber;
        private final int totalRestarts;
        private final List<SearchCandidate> topCandidates = new ArrayList<>();
        private long exploredStates;
        private boolean searchCompleted = true;
        private ScheduleResult bestResult;
        private PlanningScore bestScore;
        private long lastProgressNanos;

        private SearchState(long maxExploredStates, int topCount, int candidatePoolSize, List<SlotRef> orderedSlots, double maxSeconds, Map<String, Map<String, PreferenceValue>> preferenceMap, Map<String, Double> maxFamilyTrips, long seed, int restartNumber, int totalRestarts) {
            this.maxExploredStates = maxExploredStates;
            this.topCount = topCount;
            this.candidatePoolSize = candidatePoolSize;
            this.orderedSlots = orderedSlots;
            this.unlimitedTime = Double.isInfinite(maxSeconds);
            this.deadlineNanos = unlimitedTime ? Long.MAX_VALUE : System.nanoTime() + (long) (maxSeconds * 1_000_000_000L);
            this.preferenceMap = preferenceMap;
            this.maxFamilyTrips = maxFamilyTrips;
            this.random = new Random(seed);
            this.restartNumber = restartNumber;
            this.totalRestarts = totalRestarts;
            this.lastProgressNanos = System.nanoTime();
        }

        private boolean isTimeLimitReached() {
            return !unlimitedTime && System.nanoTime() >= deadlineNanos;
        }

        private boolean isStateLimitReached() {
            return exploredStates >= maxExploredStates;
        }

        private double randomOrderFor(String familyName, String slotKey) {
            return randomOrderCache.computeIfAbsent(slotKey + "|" + familyName, ignored -> random.nextDouble());
        }

        private void reportProgressIfNeeded() {
            long now = System.nanoTime();
            if (now - lastProgressNanos < 2_000_000_000L) {
                return;
            }
            lastProgressNanos = now;
            String remaining = unlimitedTime
                    ? "unlimited"
                    : String.format(java.util.Locale.US, "%.1fs", Math.max(0.0, (deadlineNanos - now) / 1_000_000_000.0));
            System.err.println("[planner] progress restart=" + restartNumber + "/" + totalRestarts
                    + " explored=" + exploredStates
                    + " currentTop=" + topCandidates.size()
                    + " remaining=" + remaining);
        }
    }
}
