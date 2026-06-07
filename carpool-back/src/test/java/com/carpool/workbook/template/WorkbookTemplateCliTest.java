package com.carpool.workbook.template;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkbookTemplateCliTest {

    @TempDir
    Path tempDir;

    @Test
    void generates_workbook_file_on_disk() throws Exception {
        Path output = tempDir.resolve("requirements-template.xlsx");
        WorkbookTemplateCli cli = new WorkbookTemplateCli(new WorkbookTemplateGenerator());

        cli.generate(output, List.of(new FamilyWorkbookTemplateData("Demo", 4, List.of("Alice", "Bob"))));

        assertThat(output).exists();
        assertThat(Files.size(output)).isGreaterThan(0);

        try (InputStream inputStream = Files.newInputStream(output); XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            assertThat(workbook.getSheet("README")).isNotNull();
            assertThat(workbook.getSheet("Index")).isNotNull();
            String familySheetName = workbook.getSheet("Index").getRow(1).getCell(1).getStringCellValue();
            assertThat(workbook.getSheet(familySheetName)).isNotNull();
        }
    }
}
