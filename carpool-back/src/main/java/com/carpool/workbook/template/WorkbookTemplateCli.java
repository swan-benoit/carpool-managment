package com.carpool.workbook.template;

import java.io.IOException;
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
        Path outputPath = args.length > 0 ? Path.of(args[0]) : DEFAULT_OUTPUT;

        WorkbookTemplateCli cli = new WorkbookTemplateCli(new WorkbookTemplateGenerator());
        cli.generate(outputPath, defaultFamilies());

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
        return List.of(new FamilyWorkbookTemplateData("Exemple", 4, List.of("Enfant 1")));
    }
}
