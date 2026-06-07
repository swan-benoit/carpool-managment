package com.carpool.workbook.normalization;

import com.carpool.family.AbsenceDays;
import com.carpool.family.Child;
import com.carpool.family.Family;
import com.carpool.family.TimeSlot;
import com.carpool.family.WeekDay;
import com.carpool.family.WeekType;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class WorkbookFamilyReader {

    private static final String INDEX_SHEET_NAME = "Index";
    private static final int FAMILY_NAME_ROW = 3;
    private static final int FAMILY_CAPACITY_ROW = 4;
    private static final int FAMILY_PREFERENCE_ROW = 9;
    private static final int FIRST_CHILD_ROW = 15;
    private static final int FIRST_SLOT_COLUMN = 1;
    private static final int SLOT_COLUMN_COUNT = 16;
    private static final String ABSENT_VALUE = "ABSENT";
    private static final String INFO_SECTION_LABEL = "Informations libres";
    private static final String GUARD_ARRANGEMENT_LABEL = "Garde alternee / contexte";
    private static final String MEETING_POINT_LABEL = "Lieu de RDV";
    private static final String WHATSAPP_LABEL = "Whatsapp";
    private static final String REMARKS_LABEL = "Remarques";

    public List<Family> readFamilies(Path workbookPath) {
        return readWorkbookFamilies(workbookPath).stream().map(NormalizedWorkbookFamily::family).toList();
    }

    public List<NormalizedWorkbookFamily> readWorkbookFamilies(Path workbookPath) {
        try (InputStream inputStream = Files.newInputStream(workbookPath)) {
            return readWorkbookFamilies(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read workbook: " + workbookPath, e);
        }
    }

    public List<Family> readFamilies(InputStream inputStream) throws IOException {
        return readWorkbookFamilies(inputStream).stream().map(NormalizedWorkbookFamily::family).toList();
    }

    public List<NormalizedWorkbookFamily> readWorkbookFamilies(InputStream inputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet index = workbook.getSheet(INDEX_SHEET_NAME);
            if (index == null) {
                return List.of();
            }

            List<NormalizedWorkbookFamily> families = new ArrayList<>();
            for (int rowIndex = 1; rowIndex <= index.getLastRowNum(); rowIndex++) {
                Row row = index.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                String sheetName = readRawString(row.getCell(1));
                if (sheetName == null || sheetName.isBlank()) {
                    continue;
                }

                Sheet familySheet = findSheet(workbook, sheetName);
                if (familySheet != null) {
                    families.add(readFamily(familySheet));
                }
            }
            return families;
        }
    }

    private NormalizedWorkbookFamily readFamily(Sheet sheet) {
        Family family = new Family();
        family.name = readString(cell(sheet, FAMILY_NAME_ROW, 1));
        family.carCapacity = readInt(cell(sheet, FAMILY_CAPACITY_ROW, 1));
        family.children = readChildren(sheet);
        family.requirements = Set.of();
        return new NormalizedWorkbookFamily(
                family,
                readPreferences(sheet),
                readNotes(sheet)
        );
    }

    private List<Child> readChildren(Sheet sheet) {
        List<Child> children = new ArrayList<>();
        for (int rowIndex = FIRST_CHILD_ROW; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            String childName = readString(row.getCell(0));
            if (childName == null || childName.isBlank()) {
                continue;
            }
            if (INFO_SECTION_LABEL.equals(childName)) {
                break;
            }

            Child child = new Child();
            child.name = childName;
            child.absenceDays = readAbsences(row);
            children.add(child);
        }
        return children;
    }

    private Set<AbsenceDays> readAbsences(Row row) {
        Set<AbsenceDays> absences = new LinkedHashSet<>();
        for (int columnIndex = FIRST_SLOT_COLUMN; columnIndex < FIRST_SLOT_COLUMN + SLOT_COLUMN_COUNT; columnIndex++) {
            if (!ABSENT_VALUE.equalsIgnoreCase(readString(row.getCell(columnIndex)))) {
                continue;
            }

            SlotPosition slot = SlotPosition.fromColumn(columnIndex);
            AbsenceDays absence = new AbsenceDays();
            absence.weekType = slot.weekType();
            absence.weekDay = slot.weekDay();
            absence.timeSlot = slot.timeSlot();
            absences.add(absence);
        }
        return absences;
    }

    private List<NormalizedWorkbookFamily.FamilyPreference> readPreferences(Sheet sheet) {
        Row row = sheet.getRow(FAMILY_PREFERENCE_ROW);
        if (row == null) {
            return List.of();
        }

        List<NormalizedWorkbookFamily.FamilyPreference> preferences = new ArrayList<>();
        for (int columnIndex = FIRST_SLOT_COLUMN; columnIndex < FIRST_SLOT_COLUMN + SLOT_COLUMN_COUNT; columnIndex++) {
            String value = readString(row.getCell(columnIndex));
            if (value == null || value.isBlank()) {
                continue;
            }

            SlotPosition slot = SlotPosition.fromColumn(columnIndex);
            preferences.add(new NormalizedWorkbookFamily.FamilyPreference(
                    slot.weekType(),
                    slot.weekDay(),
                    slot.timeSlot(),
                    value
            ));
        }
        return preferences;
    }

    private NormalizedWorkbookFamily.FamilyNotes readNotes(Sheet sheet) {
        int infoSectionRow = findRowIndex(sheet, INFO_SECTION_LABEL);
        if (infoSectionRow < 0) {
            return NormalizedWorkbookFamily.FamilyNotes.empty();
        }

        Map<String, String> valuesByLabel = new LinkedHashMap<>();
        for (int rowIndex = infoSectionRow + 1; rowIndex <= Math.min(sheet.getLastRowNum(), infoSectionRow + 4); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            String label = readString(row.getCell(0));
            if (label == null || label.isBlank()) {
                continue;
            }
            valuesByLabel.put(label, readString(row.getCell(1)) == null ? "" : readString(row.getCell(1)));
        }

        return new NormalizedWorkbookFamily.FamilyNotes(
                valuesByLabel.getOrDefault(GUARD_ARRANGEMENT_LABEL, ""),
                valuesByLabel.getOrDefault(MEETING_POINT_LABEL, ""),
                valuesByLabel.getOrDefault(WHATSAPP_LABEL, ""),
                valuesByLabel.getOrDefault(REMARKS_LABEL, "")
        );
    }

    private int findRowIndex(Sheet sheet, String expectedLabel) {
        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row != null && expectedLabel.equals(readString(row.getCell(0)))) {
                return rowIndex;
            }
        }
        return -1;
    }

    private Cell cell(Sheet sheet, int rowIndex, int columnIndex) {
        Row row = sheet.getRow(rowIndex);
        return row == null ? null : row.getCell(columnIndex);
    }

    private Sheet findSheet(Workbook workbook, String sheetName) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet != null) {
            return sheet;
        }

        String trimmedSheetName = sheetName.trim();
        for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
            Sheet candidate = workbook.getSheetAt(index);
            if (candidate.getSheetName().trim().equals(trimmedSheetName)) {
                return candidate;
            }
        }
        return null;
    }

    private String readString(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            case BLANK -> null;
            default -> cell.toString().trim();
        };
    }

    private String readRawString(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            case BLANK -> null;
            default -> cell.toString();
        };
    }

    private int readInt(Cell cell) {
        if (cell == null) {
            return 0;
        }
        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> Integer.parseInt(cell.getStringCellValue().trim());
            default -> 0;
        };
    }

    private record SlotPosition(WeekType weekType, WeekDay weekDay, TimeSlot timeSlot) {
        private static SlotPosition fromColumn(int columnIndex) {
            int zeroBasedIndex = columnIndex - FIRST_SLOT_COLUMN;
            int slotsPerWeekType = WeekDay.values().length * TimeSlot.values().length;
            WeekType weekType = WeekType.values()[zeroBasedIndex / slotsPerWeekType];
            int indexWithinWeekType = zeroBasedIndex % slotsPerWeekType;
            WeekDay weekDay = WeekDay.values()[indexWithinWeekType / TimeSlot.values().length];
            TimeSlot timeSlot = TimeSlot.values()[indexWithinWeekType % TimeSlot.values().length];
            return new SlotPosition(weekType, weekDay, timeSlot);
        }
    }
}
