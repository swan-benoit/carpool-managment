package com.carpool.workbook.template;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkbookTemplateGeneratorTest {

    private final WorkbookTemplateGenerator generator = new WorkbookTemplateGenerator();

    @Test
    void generates_base_workbook_with_readme_and_index() {
        try (XSSFWorkbook workbook = generator.createWorkbook()) {
            assertThat(workbook.getSheet("README")).isNotNull();
            assertThat(workbook.getSheet("Index")).isNotNull();
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void generates_family_sheet_with_helpers_and_grids() throws Exception {
        FamilyWorkbookTemplateData family = new FamilyWorkbookTemplateData("Anne / Swan", 4, List.of("Luce"), null);

        try (XSSFWorkbook workbook = generator.createWorkbook(List.of(family))) {
            String familySheetName = workbook.getSheet("Index").getRow(1).getCell(1).getStringCellValue();
            Sheet sheet = workbook.getSheet(familySheetName);
            assertThat(sheet).isNotNull();

            assertCellValue(sheet, 0, 0, "Onglet famille");
            assertCellValue(sheet, 3, 0, "Nom famille");
            assertCellValue(sheet, 3, 1, "Anne / Swan");
            assertThat(sheet.getRow(4).getCell(1).getNumericCellValue()).isEqualTo(4);

            assertCellValue(sheet, 5, 0, "Preferences famille");
            assertCellValue(sheet, 6, 1, "Semaines paires");
            assertCellValue(sheet, 7, 1, "Lundi");
            assertCellValue(sheet, 8, 1, "Matin");
            assertCellValue(sheet, 9, 0, "Famille");

            assertCellValue(sheet, 11, 0, "Absences enfants");
            assertCellValue(sheet, 15, 0, "Luce");
            assertThat(sheet.getDataValidations()).isNotEmpty();
        }
    }

    @Test
    void writes_index_rows_for_families() throws Exception {
        List<FamilyWorkbookTemplateData> families = List.of(
                new FamilyWorkbookTemplateData("Anne / Swan", 4, List.of("Luce"), null),
                new FamilyWorkbookTemplateData("Virginie et Romain", 6, List.of("Mael", "Matheo"), null)
        );

        try (XSSFWorkbook workbook = generator.createWorkbook(families)) {
            Sheet index = workbook.getSheet("Index");
            assertCellValue(index, 1, 0, "Anne / Swan");
            assertThat(index.getRow(1).getCell(1).getStringCellValue()).startsWith("Famille - Anne");
            assertThat(index.getRow(1).getCell(2).getNumericCellValue()).isEqualTo(4);
            assertThat(index.getRow(2).getCell(3).getNumericCellValue()).isEqualTo(2);
        }
    }

    private void assertCellValue(Sheet sheet, int rowIndex, int columnIndex, String expected) {
        Row row = sheet.getRow(rowIndex);
        assertThat(row).isNotNull();
        assertThat(row.getCell(columnIndex).getStringCellValue()).isEqualTo(expected);
    }
}
