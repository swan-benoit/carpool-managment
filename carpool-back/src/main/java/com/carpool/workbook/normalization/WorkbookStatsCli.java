package com.carpool.workbook.normalization;

import com.carpool.family.Family;
import com.carpool.schedule.FamilyPlanningStats;
import com.carpool.schedule.PlanningScore;
import com.carpool.schedule.PlanningScorer;
import com.carpool.schedule.calculator.ScheduleResult;
import com.carpool.schedule.calculator.ScheduleService;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WorkbookStatsCli {

    private static final String JSON_FORMAT = "json";
    private static final String TEXT_FORMAT = "text";

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
        if (parsedArguments.includePlanningScore()) {
            ScheduleResult scheduleResult = new ScheduleService().generateSchedule(families);
            planningScore = new PlanningScorer().score(scheduleResult, normalizedFamilies);
        }
        WorkbookPerfectStatsResponse response = new WorkbookPerfectStatsResponse(
                FamilyPlanningStats.totalRequiredTripsPerWeek(families),
                totalPerfectMeanTripPerWeek,
                stats,
                planningScore
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
            positionalArgs.add(argument);
        }

        if (positionalArgs.isEmpty()) {
            throw new IllegalArgumentException("Workbook path argument is required");
        }
        if (!JSON_FORMAT.equals(format) && !TEXT_FORMAT.equals(format)) {
            throw new IllegalArgumentException("Unsupported format: %s. Expected json or text".formatted(format));
        }

        return new Arguments(positionalArgs.getFirst(), format, includePlanningScore);
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
            builder.append("- total score: ")
                    .append(response.planningScore().totalScore())
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
                    .append('\n'));
        }

        return builder.toString();
    }

    private static String formatNumber(double value) {
        return String.format(Locale.US, "%.3f", value);
    }

    private record Arguments(String workbookPath, String format, boolean includePlanningScore) {
    }
}
