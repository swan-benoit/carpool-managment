package com.carpool.workbook.template;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class WorkbookTemplateCli {

    private static final Path DEFAULT_OUTPUT = Path.of("target", "requirements-template.xlsx");

    private final WorkbookTemplateGenerator generator;

    public WorkbookTemplateCli(WorkbookTemplateGenerator generator) {
        this.generator = generator;
    }

    public static void main(String[] args) throws IOException {
        String outputProperty = System.getProperty("workbook.output");
        String familiesProperty = System.getProperty("workbook.familiesJson");

        Path outputPath = outputProperty != null && !outputProperty.isBlank()
                ? Path.of(outputProperty)
                : args.length > 0 ? Path.of(args[0]) : DEFAULT_OUTPUT;
        List<FamilyWorkbookTemplateData> families = familiesProperty != null && !familiesProperty.isBlank()
                ? readFamilies(Path.of(familiesProperty))
                : args.length > 1 ? readFamilies(Path.of(args[1])) : defaultFamilies();

        WorkbookTemplateCli cli = new WorkbookTemplateCli(new WorkbookTemplateGenerator());
        cli.generate(outputPath, families);

        System.out.println("Workbook template generated: " + outputPath.toAbsolutePath());
    }

    public void generate(Path outputPath, List<FamilyWorkbookTemplateData> families) throws IOException {
        Path normalizedOutput = outputPath.toAbsolutePath().normalize();
        Path parent = normalizedOutput.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(normalizedOutput, generator.createWorkbookBytes(families));
    }

    private static List<FamilyWorkbookTemplateData> defaultFamilies() {
        return List.of(new FamilyWorkbookTemplateData("Exemple", 4, List.of("Enfant 1"), null));
    }

    private static List<FamilyWorkbookTemplateData> readFamilies(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            Jsonb jsonb = JsonbBuilder.create();
            FamiliesPayload payload = jsonb.fromJson(reader, FamiliesPayload.class);
            return payload == null || payload.families == null ? List.of() : payload.families;
        }
    }

    public static class FamiliesPayload {
        public List<FamilyWorkbookTemplateData> families;
    }
}
