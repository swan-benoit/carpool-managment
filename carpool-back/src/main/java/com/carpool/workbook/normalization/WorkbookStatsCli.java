package com.carpool.workbook.normalization;

import com.carpool.family.Family;
import com.carpool.schedule.FamilyPlanningStats;
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
        List<Family> families = reader.readWorkbookFamilies(workbookPath).stream()
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
        WorkbookPerfectStatsResponse response = new WorkbookPerfectStatsResponse(
                FamilyPlanningStats.totalRequiredTripsPerWeek(families),
                totalPerfectMeanTripPerWeek,
                stats
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

        for (int index = 0; index < args.length; index++) {
            String argument = args[index];
            if ("--format".equals(argument)) {
                if (index + 1 >= args.length) {
                    throw new IllegalArgumentException("Missing value after --format");
                }
                format = args[++index].toLowerCase(Locale.ROOT);
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

        return new Arguments(positionalArgs.getFirst(), format);
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

        return builder.toString();
    }

    private static String formatNumber(double value) {
        return String.format(Locale.US, "%.3f", value);
    }

    private record Arguments(String workbookPath, String format) {
    }
}
