package com.carpool.workbook.normalization;

import com.carpool.family.TimeSlot;
import com.carpool.family.WeekDay;
import com.carpool.family.WeekType;
import com.carpool.schedule.Stats;
import com.carpool.workbook.template.FamilyWorkbookTemplateData;
import com.carpool.workbook.template.WorkbookTemplateGenerator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkbookStatsServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void reads_slot_level_absences_from_workbook() throws Exception {
        Path workbookPath = createWorkbookWithSingleAbsentSlot();

        WorkbookFamilyReader reader = new WorkbookFamilyReader();
        var families = reader.readFamilies(workbookPath);

        assertThat(families).hasSize(1);
        assertThat(families.getFirst().name).isEqualTo("Famille Test");
        assertThat(families.getFirst().children).hasSize(1);
        assertThat(families.getFirst().children.getFirst().absenceDays).singleElement().satisfies(absence -> {
            assertThat(absence.weekType).isEqualTo(WeekType.EVEN);
            assertThat(absence.weekDay).isEqualTo(WeekDay.MONDAY);
            assertThat(absence.timeSlot).isEqualTo(TimeSlot.MORNING);
        });
    }

    @Test
    void preserves_preferences_and_informational_notes_from_workbook() throws Exception {
        Path workbookPath = createWorkbookWithSingleAbsentSlot();

        WorkbookFamilyReader reader = new WorkbookFamilyReader();
        List<NormalizedWorkbookFamily> families = reader.readWorkbookFamilies(workbookPath);

        assertThat(families).hasSize(1);
        assertThat(families.getFirst().preferences()).singleElement().satisfies(preference -> {
            assertThat(preference.weekType()).isEqualTo(WeekType.EVEN);
            assertThat(preference.weekDay()).isEqualTo(WeekDay.MONDAY);
            assertThat(preference.timeSlot()).isEqualTo(TimeSlot.MORNING);
            assertThat(preference.value()).isEqualTo("PREFERE");
        });
        assertThat(families.getFirst().notes().guardArrangement()).isEqualTo("Semaine alternee");
        assertThat(families.getFirst().notes().meetingPoint()).isEqualTo("Parking ecole");
        assertThat(families.getFirst().notes().whatsapp()).isEqualTo("oui");
        assertThat(families.getFirst().notes().remarks()).isEqualTo("Besoin siege auto");
    }

    @Test
    void computes_stats_from_workbook_with_slot_absence() throws Exception {
        Path workbookPath = createWorkbookWithSingleAbsentSlot();

        WorkbookStatsService service = new WorkbookStatsService();
        service.workbookFamilyReader = new WorkbookFamilyReader();
        service.scheduleService = new com.carpool.schedule.calculator.ScheduleService();

        List<Stats> stats = service.computeStats(workbookPath);

        assertThat(stats).hasSize(1);
        assertThat(stats.getFirst().meanTripPerWeek()).isEqualTo(7.5);
        assertThat(stats.getFirst().perfectMeanTripPerWeek()).isEqualTo(7.5);
    }

    private Path createWorkbookWithSingleAbsentSlot() throws Exception {
        Path workbookPath = tempDir.resolve("requirements.xlsx");
        WorkbookTemplateGenerator generator = new WorkbookTemplateGenerator();
        try (XSSFWorkbook workbook = generator.createWorkbook(List.of(new FamilyWorkbookTemplateData("Famille Test", 4, List.of("Nina"))));
             OutputStream outputStream = Files.newOutputStream(workbookPath)) {
            String familySheetName = workbook.getSheet("Index").getRow(1).getCell(1).getStringCellValue();
            workbook.getSheet(familySheetName).getRow(9).getCell(1).setCellValue("PREFERE");
            workbook.getSheet(familySheetName).getRow(15).getCell(1).setCellValue("ABSENT");
            workbook.getSheet(familySheetName).getRow(19).getCell(1).setCellValue("Semaine alternee");
            workbook.getSheet(familySheetName).getRow(20).getCell(1).setCellValue("Parking ecole");
            workbook.getSheet(familySheetName).getRow(21).getCell(1).setCellValue("oui");
            workbook.getSheet(familySheetName).getRow(22).getCell(1).setCellValue("Besoin siege auto");
            workbook.write(outputStream);
        }

        try (InputStream ignored = Files.newInputStream(workbookPath)) {
            assertThat(workbookPath).exists();
        }
        return workbookPath;
    }
}
