package com.carpool.workbook.normalization;

import com.carpool.family.Family;
import com.carpool.schedule.FamilyJusticeScore;
import com.carpool.schedule.FamilyPlanningScore;
import com.carpool.schedule.FamilyPlanningStats;
import com.carpool.schedule.PlanningScore;
import com.carpool.schedule.PlanningScorer;
import com.carpool.schedule.calculator.BruteForceSchedulePlanner;
import com.carpool.schedule.calculator.Assignment;
import com.carpool.schedule.calculator.ScheduleResult;
import com.carpool.schedule.calculator.ScheduleService;
import com.carpool.schedule.calculator.Trip;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WorkbookStatsCli {

    private static final String JSON_FORMAT = "json";
    private static final String TEXT_FORMAT = "text";
    private static final String GREEDY_PLANNER = "greedy";
    private static final String BRUTE_FORCE_PLANNER = "brute-force";

    public static void main(String[] args) throws IOException {
        Arguments parsedArguments = parseArguments(args);

        Path workbookPath = Path.of(parsedArguments.workbookPath());
        WorkbookFamilyReader reader = new WorkbookFamilyReader();
        List<NormalizedWorkbookFamily> normalizedFamilies = reader.readWorkbookFamilies(workbookPath);
        List<Family> families = normalizedFamilies.stream()
                .map(NormalizedWorkbookFamily::family)
                .toList();
        List<WorkbookPerfectStat> stats = families.stream()
                .map(family -> new WorkbookPerfectStat(
                        family.name,
                        family.carCapacity,
                        family.children.size(),
                        FamilyPlanningStats.perfectMeanTripPerWeek(family, families)))
                .toList();
        double totalPerfectMeanTripPerWeek = stats.stream()
                .mapToDouble(WorkbookPerfectStat::perfectMeanTripPerWeek)
                .sum();
        PlanningScore planningScore = null;
        WorkbookPlanningMetadata planningMetadata = null;
        WorkbookPlanningView planning = null;
        List<WorkbookPlanCandidateView> planCandidates = null;
        if (parsedArguments.includePlanningScore()) {
            if (BRUTE_FORCE_PLANNER.equals(parsedArguments.planner())) {
                BruteForceSchedulePlanner.SearchSummary searchSummary = new BruteForceSchedulePlanner()
                        .generateTopSchedules(normalizedFamilies, parsedArguments.maxStates(), parsedArguments.top());
                planCandidates = toPlanCandidateViews(searchSummary, parsedArguments.includePlanningOutput());
                if (!searchSummary.candidates().isEmpty()) {
                    BruteForceSchedulePlanner.SearchCandidate best = searchSummary.candidates().getFirst();
                    planning = parsedArguments.includePlanningOutput() ? toPlanningView(best.scheduleResult()) : null;
                    planningScore = best.planningScore();
                    planningMetadata = new WorkbookPlanningMetadata(BRUTE_FORCE_PLANNER, searchSummary.exploredStates(), searchSummary.searchCompleted(), parsedArguments.maxSeconds());
                } else {
                    planningMetadata = new WorkbookPlanningMetadata(BRUTE_FORCE_PLANNER, searchSummary.exploredStates(), searchSummary.searchCompleted(), parsedArguments.maxSeconds());
                }
            } else {
                ScheduleResult scheduleResult = new ScheduleService().generateSchedule(families);
                planning = parsedArguments.includePlanningOutput() ? toPlanningView(scheduleResult) : null;
                planningScore = new PlanningScorer().score(scheduleResult, normalizedFamilies);
                planningMetadata = new WorkbookPlanningMetadata(GREEDY_PLANNER, null, true, null);
                planCandidates = List.of(new WorkbookPlanCandidateView(1, planningMetadata, planningScore, planning));
            }
        }
        WorkbookPerfectStatsResponse response = new WorkbookPerfectStatsResponse(
                FamilyPlanningStats.totalRequiredTripsPerWeek(families),
                totalPerfectMeanTripPerWeek,
                stats,
                planningScore,
                planningMetadata,
                planning,
                planCandidates
        );

        if (TEXT_FORMAT.equals(parsedArguments.format())) {
            System.out.print(formatText(response));
            return;
        }

        Jsonb jsonb = JsonbBuilder.create();
        System.out.println(jsonb.toJson(response));
    }

    private static Arguments parseArguments(String[] args) {
        List<String> positionalArgs = new ArrayList<>();
        String format = JSON_FORMAT;
        boolean includePlanningScore = false;
        boolean includePlanningOutput = false;
        String planner = GREEDY_PLANNER;
        long maxStates = BruteForceSchedulePlanner.DEFAULT_MAX_EXPLORED_STATES;
        double maxSeconds = BruteForceSchedulePlanner.DEFAULT_MAX_SECONDS;
        int top = 1;

        for (int index = 0; index < args.length; index++) {
            String argument = args[index];
            if ("--format".equals(argument)) {
                if (index + 1 >= args.length) {
                    throw new IllegalArgumentException("Missing value after --format");
                }
                format = args[++index].toLowerCase(Locale.ROOT);
                continue;
            }
            if ("--include-planning-score".equals(argument)) {
                includePlanningScore = true;
                continue;
            }
            if ("--include-planning-output".equals(argument)) {
                includePlanningOutput = true;
                continue;
            }
            if ("--planner".equals(argument)) {
                if (index + 1 >= args.length) {
                    throw new IllegalArgumentException("Missing value after --planner");
                }
                planner = args[++index].toLowerCase(Locale.ROOT);
                continue;
            }
            if ("--max-states".equals(argument)) {
                if (index + 1 >= args.length) {
                    throw new IllegalArgumentException("Missing value after --max-states");
                }
                maxStates = Long.parseLong(args[++index]);
                continue;
            }
            if ("--max-seconds".equals(argument)) {
                if (index + 1 >= args.length) {
                    throw new IllegalArgumentException("Missing value after --max-seconds");
                }
                maxSeconds = Double.parseDouble(args[++index]);
                continue;
            }
            if ("--top".equals(argument)) {
                if (index + 1 >= args.length) {
                    throw new IllegalArgumentException("Missing value after --top");
                }
                top = Integer.parseInt(args[++index]);
                continue;
            }
            positionalArgs.add(argument);
        }

        if (positionalArgs.isEmpty()) {
            throw new IllegalArgumentException("Workbook path argument is required");
        }
        if (!JSON_FORMAT.equals(format) && !TEXT_FORMAT.equals(format)) {
            throw new IllegalArgumentException("Unsupported format: %s. Expected json or text".formatted(format));
        }
        if (!GREEDY_PLANNER.equals(planner) && !BRUTE_FORCE_PLANNER.equals(planner)) {
            throw new IllegalArgumentException("Unsupported planner: %s. Expected greedy or brute-force".formatted(planner));
        }
        if (top <= 0) {
            throw new IllegalArgumentException("--top must be greater than 0");
        }
        if (maxSeconds <= 0.0) {
            throw new IllegalArgumentException("--max-seconds must be greater than 0");
        }

        return new Arguments(positionalArgs.getFirst(), format, includePlanningScore, includePlanningOutput, planner, maxStates, maxSeconds, top);
    }

    private static String formatText(WorkbookPerfectStatsResponse response) {
        StringBuilder builder = new StringBuilder();
        builder.append("Total required trips per week: ")
                .append(formatNumber(response.totalRequiredTripsPerWeek()))
                .append('\n');
        builder.append("Total perfect mean trips per week: ")
                .append(formatNumber(response.totalPerfectMeanTripPerWeek()))
                .append('\n');
        builder.append('\n');
        builder.append("Families:")
                .append('\n');

        for (WorkbookPerfectStat family : response.families()) {
            builder.append("- ")
                    .append(family.familyName())
                    .append(" | car capacity: ")
                    .append(family.carCapacity())
                    .append(" | children: ")
                    .append(family.childCount())
                    .append(" | perfect mean trips/week: ")
                    .append(formatNumber(family.perfectMeanTripPerWeek()))
                    .append('\n');
        }

        if (response.planningScore() != null) {
            builder.append('\n');
            builder.append("Planning score:")
                    .append('\n');
            if (response.planningMetadata() != null) {
                builder.append("- planner: ")
                        .append(response.planningMetadata().planner())
                        .append('\n');
                if (response.planningMetadata().exploredStates() != null) {
                    builder.append("- explored states: ")
                            .append(response.planningMetadata().exploredStates())
                            .append('\n');
                }
                builder.append("- search completed: ")
                        .append(response.planningMetadata().searchCompleted())
                        .append('\n');
            }
            builder.append("- total score: ")
                    .append(response.planningScore().totalScore())
                    .append('\n');
            builder.append("- complete: ")
                    .append(response.planningScore().complete())
                    .append('\n');
            builder.append("- completion ratio: ")
                    .append(formatNumber(response.planningScore().completionRatio()))
                    .append('\n');
            builder.append("- required transport slots: ")
                    .append(response.planningScore().totalRequiredTransportSlots())
                    .append('\n');
            builder.append("- assigned required transport slots: ")
                    .append(response.planningScore().assignedRequiredTransportSlots())
                    .append('\n');
            builder.append("- missing required transport slots: ")
                    .append(response.planningScore().missingRequiredTransportSlots())
                    .append('\n');
            builder.append("- impossible assignments: ")
                    .append(response.planningScore().impossibleAssignments())
                    .append('\n');
            builder.append("- avoid assignments: ")
                    .append(response.planningScore().avoidAssignments())
                    .append('\n');
            builder.append("- preferred assignments: ")
                    .append(response.planningScore().preferredAssignments())
                    .append('\n');
            builder.append("- ok assignments: ")
                    .append(response.planningScore().okAssignments())
                    .append('\n');
            builder.append("- justice perfect: ")
                    .append(response.planningScore().justice().perfectJustice())
                    .append('\n');
            builder.append("- justice average: ")
                    .append(formatNumber(response.planningScore().justice().averageJusticeScore()))
                    .append('\n');
            builder.append("- justice minimum: ")
                    .append(formatNumber(response.planningScore().justice().minimumJusticeScore()))
                    .append('\n');
            builder.append("- families:")
                    .append('\n');
            response.planningScore().families().forEach(familyScore -> builder
                    .append("  - ")
                    .append(familyScore.familyName())
                    .append(" | total score: ")
                    .append(familyScore.totalScore())
                    .append(" | impossible: ")
                    .append(familyScore.impossibleAssignments())
                    .append(" | avoid: ")
                    .append(familyScore.avoidAssignments())
                    .append(" | preferred: ")
                    .append(familyScore.preferredAssignments())
                    .append(" | ok: ")
                    .append(familyScore.okAssignments())
                    .append(" | required transports: ")
                    .append(familyScore.requiredTransportSlots())
                    .append(" | assigned transports: ")
                    .append(familyScore.assignedTransportSlots())
                    .append(" | missing transports: ")
                    .append(familyScore.missingTransportSlots())
                    .append('\n'));
            builder.append("- justice by family:")
                    .append('\n');
            response.planningScore().justice().families().forEach(familyJustice -> builder
                    .append("  - ")
                    .append(familyJustice.familyName())
                    .append(" | justice score: ")
                    .append(formatNumber(familyJustice.justiceScore()))
                    .append(" | perfect justice: ")
                    .append(familyJustice.perfectJustice())
                    .append(" | preference compliance: ")
                    .append(familyJustice.preferenceCompliance())
                    .append(" | actual mean: ")
                    .append(formatNumber(familyJustice.actualMeanTripPerWeek()))
                    .append(" | perfect mean: ")
                    .append(formatNumber(familyJustice.perfectMeanTripPerWeek()))
                    .append(" | deviation: ")
                    .append(formatNumber(familyJustice.tripDeviation()))
                    .append('\n'));
        }

        if (response.planCandidates() != null && !response.planCandidates().isEmpty()) {
            builder.append('\n');
            builder.append("Planning candidates:")
                    .append('\n');
            for (WorkbookPlanCandidateView candidate : response.planCandidates()) {
                builder.append("#")
                        .append(candidate.rank())
                        .append(" | planner: ")
                        .append(candidate.planningMetadata().planner())
                        .append(" | total score: ")
                        .append(candidate.planningScore().totalScore())
                        .append(" | justice min: ")
                        .append(formatNumber(candidate.planningScore().justice().minimumJusticeScore()))
                        .append(" | justice avg: ")
                        .append(formatNumber(candidate.planningScore().justice().averageJusticeScore()))
                        .append('\n');
                appendJusticeTable(builder, candidate.planningScore());
                if (candidate.planning() != null) {
                    appendWeek(builder, "Even week", candidate.planning().evenWeek());
                    appendWeek(builder, "Odd week", candidate.planning().oddWeek());
                }
            }
        }

        return builder.toString();
    }

    private static List<WorkbookPlanCandidateView> toPlanCandidateViews(
            BruteForceSchedulePlanner.SearchSummary searchSummary,
            boolean includePlanningOutput
    ) {
        List<WorkbookPlanCandidateView> candidates = new ArrayList<>();
        int rank = 1;
        for (BruteForceSchedulePlanner.SearchCandidate candidate : searchSummary.candidates()) {
            candidates.add(new WorkbookPlanCandidateView(
                    rank++,
                    new WorkbookPlanningMetadata(BRUTE_FORCE_PLANNER, searchSummary.exploredStates(), searchSummary.searchCompleted(), null),
                    candidate.planningScore(),
                    includePlanningOutput ? toPlanningView(candidate.scheduleResult()) : null
            ));
        }
        return candidates;
    }

    private static void appendWeek(StringBuilder builder, String label, List<WorkbookTripView> trips) {
        builder.append(label).append(':').append('\n');
        for (WorkbookTripView trip : trips) {
            builder.append("- ")
                    .append(trip.weekDay())
                    .append(' ')
                    .append(trip.timeSlot())
                    .append('\n');
            if (trip.assignments().isEmpty()) {
                builder.append("  - No trip assigned").append('\n');
                continue;
            }
            for (WorkbookAssignmentView assignment : trip.assignments()) {
                builder.append("  - ")
                        .append(assignment.driverFamilyName())
                        .append(": ")
                        .append(String.join(", ", assignment.childNames()))
                        .append('\n');
            }
        }
    }

    private static WorkbookPlanningView toPlanningView(ScheduleResult scheduleResult) {
        return new WorkbookPlanningView(
                toTripViews(scheduleResult.even().trips()),
                toTripViews(scheduleResult.odd().trips())
        );
    }

    private static List<WorkbookTripView> toTripViews(List<Trip> trips) {
        return trips.stream()
                .sorted(Comparator.comparingInt((Trip t) -> t.weekDay().ordinal())
                        .thenComparingInt(t -> t.timeSlot().ordinal()))
                .map(trip -> new WorkbookTripView(
                        trip.weekDay().name(),
                        trip.timeSlot().name(),
                        trip.cars().Assignments().stream()
                                .map(assignment -> new WorkbookAssignmentView(
                                        assignment.driverFamily().name,
                                        assignment.children().stream().map(child -> child.name).toList()
                                ))
                                .toList()
                ))
                .toList();
    }

    private static void appendJusticeTable(StringBuilder builder, PlanningScore planningScore) {
        Map<String, FamilyPlanningScore> planningByFamily = new HashMap<>();
        for (FamilyPlanningScore fps : planningScore.families()) {
            planningByFamily.put(fps.familyName(), fps);
        }

        List<FamilyJusticeScore> justiceScores = planningScore.justice().families();
        String[] headers = {"Famille", "Réel/sem", "Idéal/sem", "Écart", "Justice", "Impos.", "Éviter"};
        String[][] rows = new String[justiceScores.size()][7];
        for (int i = 0; i < justiceScores.size(); i++) {
            FamilyJusticeScore js = justiceScores.get(i);
            FamilyPlanningScore ps = planningByFamily.get(js.familyName());
            rows[i][0] = js.familyName();
            rows[i][1] = formatNumber(js.actualMeanTripPerWeek());
            rows[i][2] = formatNumber(js.perfectMeanTripPerWeek());
            rows[i][3] = formatNumber(js.tripDeviation());
            rows[i][4] = formatNumber(js.justiceScore());
            rows[i][5] = ps != null ? String.valueOf(ps.impossibleAssignments()) : "0";
            rows[i][6] = ps != null ? String.valueOf(ps.avoidAssignments()) : "0";
        }

        int[] widths = new int[7];
        for (int c = 0; c < 7; c++) widths[c] = headers[c].length();
        for (String[] row : rows) {
            for (int c = 0; c < 7; c++) widths[c] = Math.max(widths[c], row[c].length());
        }

        StringBuilder sep = new StringBuilder("+");
        for (int w : widths) sep.append("-".repeat(w + 2)).append("+");
        String separator = sep.toString();

        builder.append(separator).append('\n').append("|");
        for (int c = 0; c < 7; c++) builder.append(" ").append(padRight(headers[c], widths[c])).append(" |");
        builder.append('\n').append(separator).append('\n');
        for (String[] row : rows) {
            builder.append("|");
            for (int c = 0; c < 7; c++) builder.append(" ").append(padRight(row[c], widths[c])).append(" |");
            builder.append('\n');
        }
        builder.append(separator).append('\n');
    }

    private static String padRight(String s, int width) {
        return s.length() >= width ? s : s + " ".repeat(width - s.length());
    }

    private static String formatNumber(double value) {
        return String.format(Locale.US, "%.3f", value);
    }

    private record Arguments(String workbookPath, String format, boolean includePlanningScore, boolean includePlanningOutput, String planner, long maxStates, double maxSeconds, int top) {
    }
}
