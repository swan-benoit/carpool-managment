package com.carpool.workbook.normalization;

import com.carpool.workbook.template.FamilyWorkbookTemplateData;
import com.carpool.workbook.template.WorkbookTemplateGenerator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkbookStatsCliTest {

    @TempDir
    Path tempDir;

    @Test
    void outputs_planning_score_in_json_when_requested() throws Exception {
        Path workbookPath = createWorkbook();

        String output = captureStdout(() -> WorkbookStatsCli.main(new String[]{workbookPath.toString(), "--include-planning-score"}));

        assertThat(output).contains("\"planningScore\"");
        assertThat(output).contains("\"totalScore\"");
        assertThat(output).contains("\"families\"");
    }

    @Test
    void outputs_planning_score_in_text_when_requested() throws Exception {
        Path workbookPath = createWorkbook();

        String output = captureStdout(() -> WorkbookStatsCli.main(new String[]{workbookPath.toString(), "--format", "text", "--include-planning-score"}));

        assertThat(output).contains("Planning score:");
        assertThat(output).contains("- total score:");
        assertThat(output).contains("- families:");
    }

    private Path createWorkbook() throws Exception {
        Path workbookPath = tempDir.resolve("requirements.xlsx");
        WorkbookTemplateGenerator generator = new WorkbookTemplateGenerator();
        try (XSSFWorkbook workbook = generator.createWorkbook(List.of(new FamilyWorkbookTemplateData("Famille Test", 4, List.of("Nina"))));
             OutputStream outputStream = Files.newOutputStream(workbookPath)) {
            String familySheetName = workbook.getSheet("Index").getRow(1).getCell(1).getStringCellValue();
            workbook.getSheet(familySheetName).getRow(9).getCell(1).setCellValue("PREFERE");
            workbook.write(outputStream);
        }
        return workbookPath;
    }

    private String captureStdout(ThrowingRunnable runnable) throws Exception {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(outputStream, true, StandardCharsets.UTF_8));
            runnable.run();
        } finally {
            System.setOut(originalOut);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
