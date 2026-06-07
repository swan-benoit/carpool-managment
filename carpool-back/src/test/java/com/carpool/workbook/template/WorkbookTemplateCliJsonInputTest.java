package com.carpool.workbook.template;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class WorkbookTemplateCliJsonInputTest {

    @TempDir
    Path tempDir;

    @Test
    void reads_families_from_json_payload() throws Exception {
        Path output = tempDir.resolve("requirements-template.xlsx");
        Path json = tempDir.resolve("families.json");
        Files.writeString(json, """
                {
                  "families": [
                    {
                      "familyName": "Famille Test",
                      "carCapacity": 5,
                      "childNames": ["Nina", "Leo"]
                    }
                  ]
                }
                """);

        WorkbookTemplateCli.main(new String[]{output.toString(), json.toString()});

        try (InputStream inputStream = Files.newInputStream(output); XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            assertThat(workbook.getSheet("Index").getRow(1).getCell(0).getStringCellValue()).isEqualTo("Famille Test");
            String familySheetName = workbook.getSheet("Index").getRow(1).getCell(1).getStringCellValue();
            assertThat(workbook.getSheet(familySheetName)).isNotNull();
            assertThat(workbook.getSheet(familySheetName).getRow(15).getCell(0).getStringCellValue()).isEqualTo("Nina");
            assertThat(workbook.getSheet(familySheetName).getRow(16).getCell(0).getStringCellValue()).isEqualTo("Leo");
        }
    }
}
