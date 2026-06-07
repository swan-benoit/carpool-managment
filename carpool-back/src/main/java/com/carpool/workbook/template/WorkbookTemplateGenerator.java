package com.carpool.workbook.template;

import com.carpool.family.TimeSlot;
import com.carpool.family.WeekDay;
import com.carpool.family.WeekType;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class WorkbookTemplateGenerator {

    static final String README_SHEET_NAME = "README";
    static final String INDEX_SHEET_NAME = "Index";

    static final List<String> FAMILY_PREFERENCE_VALUES = List.of("IMPOSSIBLE", "EVITER", "OK", "PREFERE");
    static final List<String> CHILD_ABSENCE_VALUES = List.of("PRESENT", "ABSENT");

    public XSSFWorkbook createWorkbook() {
        return createWorkbook(List.of());
    }

    public XSSFWorkbook createWorkbook(List<FamilyWorkbookTemplateData> families) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        CellStyles styles = createStyles(workbook);

        createReadmeSheet(workbook, styles);
        createIndexSheet(workbook, families, styles);

        Set<String> usedSheetNames = new LinkedHashSet<>(List.of(README_SHEET_NAME, INDEX_SHEET_NAME));
        for (FamilyWorkbookTemplateData family : families) {
            String sheetName = uniqueFamilySheetName(family.familyName(), usedSheetNames);
            usedSheetNames.add(sheetName);
            createFamilySheet(workbook, sheetName, family, styles);
        }

        workbook.setActiveSheet(0);
        return workbook;
    }

    public byte[] createWorkbookBytes(List<FamilyWorkbookTemplateData> families) {
        try (XSSFWorkbook workbook = createWorkbook(families); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to create workbook template bytes", e);
        }
    }

    private void createReadmeSheet(Workbook workbook, CellStyles styles) {
        Sheet sheet = workbook.createSheet(README_SHEET_NAME);
        int rowIndex = 0;

        createCell(sheet.createRow(rowIndex++), 0, "Template de saisie covoiturage", styles.title());
        rowIndex++;
        createCell(sheet.createRow(rowIndex++), 0, "Ce fichier est un support de saisie. Les formules metier restent calculees en Java.", styles.wrap());
        createCell(sheet.createRow(rowIndex++), 0, "Un onglet famille represente un groupe d'enfants. Les parents separes ne creent pas plusieurs familles.", styles.wrap());
        createCell(sheet.createRow(rowIndex++), 0, "Les grilles de saisie sont la source de verite pour la normalisation et le calcul.", styles.wrap());
        createCell(sheet.createRow(rowIndex++), 0, "Preferences famille: IMPOSSIBLE, EVITER, OK, PREFERE.", styles.wrap());
        createCell(sheet.createRow(rowIndex++), 0, "Absences enfants: PRESENT, ABSENT.", styles.wrap());
        createCell(sheet.createRow(rowIndex++), 0, "Les champs informatifs peuvent rester libres. Ils ne sont pas utilises par les metriques initiales.", styles.wrap());

        sheet.setColumnWidth(0, 120 * 256);
    }

    private void createIndexSheet(Workbook workbook, List<FamilyWorkbookTemplateData> families, CellStyles styles) {
        Sheet sheet = workbook.createSheet(INDEX_SHEET_NAME);
        Row header = sheet.createRow(0);
        createCell(header, 0, "Nom famille", styles.header());
        createCell(header, 1, "Nom onglet", styles.header());
        createCell(header, 2, "Capacite voiture", styles.header());
        createCell(header, 3, "Nombre d'enfants", styles.header());

        Set<String> usedSheetNames = new LinkedHashSet<>(List.of(README_SHEET_NAME, INDEX_SHEET_NAME));
        int rowIndex = 1;
        for (FamilyWorkbookTemplateData family : families) {
            String sheetName = uniqueFamilySheetName(family.familyName(), usedSheetNames);
            usedSheetNames.add(sheetName);

            Row row = sheet.createRow(rowIndex++);
            createCell(row, 0, family.familyName(), styles.body());
            createCell(row, 1, sheetName, styles.body());
            row.createCell(2).setCellValue(family.carCapacity());
            row.getCell(2).setCellStyle(styles.body());
            row.createCell(3).setCellValue(family.childNames().size());
            row.getCell(3).setCellStyle(styles.body());
        }

        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createFamilySheet(Workbook workbook, String sheetName, FamilyWorkbookTemplateData family, CellStyles styles) {
        Sheet sheet = workbook.createSheet(sheetName);

        createCell(sheet.createRow(0), 0, "Onglet famille", styles.title());

        Row identityTitle = sheet.createRow(2);
        createCell(identityTitle, 0, "Identite", styles.section());
        Row familyNameRow = sheet.createRow(3);
        createCell(familyNameRow, 0, "Nom famille", styles.label());
        createCell(familyNameRow, 1, family.familyName(), styles.body());
        Row capacityRow = sheet.createRow(4);
        createCell(capacityRow, 0, "Capacite voiture", styles.label());
        capacityRow.createCell(1).setCellValue(family.carCapacity());
        capacityRow.getCell(1).setCellStyle(styles.body());

        int familyPreferenceStartRow = 6;
        createCell(sheet.createRow(familyPreferenceStartRow - 1), 0, "Preferences famille", styles.section());
        createSlotGridHeader(sheet, familyPreferenceStartRow, styles);
        Row familyPreferenceRow = sheet.createRow(familyPreferenceStartRow + 3);
        createCell(familyPreferenceRow, 0, "Famille", styles.label());
        createValidatedSlotCells(sheet, familyPreferenceRow, 1, FAMILY_PREFERENCE_VALUES, styles.body());

        int childAbsenceGridStartRow = familyPreferenceStartRow + 6;
        createCell(sheet.createRow(childAbsenceGridStartRow - 1), 0, "Absences enfants", styles.section());
        createSlotGridHeader(sheet, childAbsenceGridStartRow, styles);

        List<String> childNames = family.childNames().isEmpty() ? List.of("Enfant 1") : family.childNames();
        int childRowIndex = childAbsenceGridStartRow + 3;
        for (String childName : childNames) {
            Row childRow = sheet.createRow(childRowIndex++);
            createCell(childRow, 0, childName, styles.label());
            createValidatedSlotCells(sheet, childRow, 1, CHILD_ABSENCE_VALUES, styles.body());
        }

        int infoRowIndex = childRowIndex + 2;
        createCell(sheet.createRow(infoRowIndex++), 0, "Informations libres", styles.section());
        createLabelValueRow(sheet.createRow(infoRowIndex++), "Garde alternee / contexte", styles);
        createLabelValueRow(sheet.createRow(infoRowIndex++), "Lieu de RDV", styles);
        createLabelValueRow(sheet.createRow(infoRowIndex++), "Whatsapp", styles);
        createLabelValueRow(sheet.createRow(infoRowIndex), "Remarques", styles);

        sheet.setColumnWidth(0, 28 * 256);
        for (int col = 1; col <= 16; col++) {
            sheet.setColumnWidth(col, 12 * 256);
        }
        sheet.setColumnWidth(17, 40 * 256);
    }

    private void createSlotGridHeader(Sheet sheet, int startRow, CellStyles styles) {
        Row weekTypeRow = sheet.createRow(startRow);
        createCell(weekTypeRow, 0, "", styles.header());
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, 1, 8));
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, 9, 16));
        createCell(weekTypeRow, 1, "Semaines paires", styles.header());
        createCell(weekTypeRow, 9, "Semaines impaires", styles.header());

        Row weekDayRow = sheet.createRow(startRow + 1);
        createCell(weekDayRow, 0, "", styles.header());
        int columnIndex = 1;
        for (WeekType ignoredWeekType : WeekType.values()) {
            for (WeekDay weekDay : WeekDay.values()) {
                sheet.addMergedRegion(new CellRangeAddress(startRow + 1, startRow + 1, columnIndex, columnIndex + 1));
                createCell(weekDayRow, columnIndex, dayLabel(weekDay), styles.header());
                columnIndex += 2;
            }
        }

        Row timeSlotRow = sheet.createRow(startRow + 2);
        createCell(timeSlotRow, 0, "", styles.header());
        columnIndex = 1;
        for (WeekType ignored : WeekType.values()) {
            for (WeekDay ignoredDay : WeekDay.values()) {
                for (TimeSlot timeSlot : TimeSlot.values()) {
                    createCell(timeSlotRow, columnIndex++, timeSlotLabel(timeSlot), styles.header());
                }
            }
        }
    }

    private void createLabelValueRow(Row row, String label, CellStyles styles) {
        createCell(row, 0, label, styles.label());
        createCell(row, 1, "", styles.body());
    }

    private void createValidatedSlotCells(Sheet sheet, Row row, int startColumn, List<String> values, CellStyle style) {
        for (int col = startColumn; col < startColumn + 16; col++) {
            Cell cell = row.createCell(col);
            cell.setCellStyle(style);
        }
        addValidation(sheet, row.getRowNum(), row.getRowNum(), startColumn, startColumn + 15, values);
    }

    private void addValidation(Sheet sheet, int firstRow, int lastRow, int firstColumn, int lastColumn, List<String> values) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createExplicitListConstraint(values.toArray(String[]::new));
        CellRangeAddressList addressList = new CellRangeAddressList(firstRow, lastRow, firstColumn, lastColumn);
        DataValidation validation = helper.createValidation(constraint, addressList);
        validation.setSuppressDropDownArrow(true);
        validation.setEmptyCellAllowed(true);
        sheet.addValidationData(validation);
    }

    private Cell createCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
        return cell;
    }

    private String uniqueFamilySheetName(String familyName, Set<String> usedSheetNames) {
        String baseName = WorkbookUtil.createSafeSheetName("Famille - " + (familyName == null || familyName.isBlank() ? "Sans nom" : familyName));
        String candidate = baseName;
        int suffix = 2;
        while (usedSheetNames.contains(candidate)) {
            String numberedBase = baseName;
            if (numberedBase.length() > 28) {
                numberedBase = numberedBase.substring(0, 28);
            }
            candidate = numberedBase + "-" + suffix++;
        }
        return candidate;
    }

    private String dayLabel(WeekDay weekDay) {
        return switch (weekDay) {
            case MONDAY -> "Lundi";
            case TUESDAY -> "Mardi";
            case THURSDAY -> "Jeudi";
            case FRIDAY -> "Vendredi";
        };
    }

    private String timeSlotLabel(TimeSlot timeSlot) {
        return switch (timeSlot) {
            case MORNING -> "Matin";
            case EVENING -> "Soir";
        };
    }

    private CellStyles createStyles(Workbook workbook) {
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);

        Font boldFont = workbook.createFont();
        boldFont.setBold(true);

        CellStyle title = workbook.createCellStyle();
        title.setFont(titleFont);

        CellStyle section = workbook.createCellStyle();
        section.setFont(boldFont);
        section.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        section.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle header = workbook.createCellStyle();
        header.setFont(boldFont);
        header.setAlignment(HorizontalAlignment.CENTER);
        header.setVerticalAlignment(VerticalAlignment.CENTER);
        header.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorders(header);

        CellStyle label = workbook.createCellStyle();
        label.setFont(boldFont);
        setBorders(label);

        CellStyle body = workbook.createCellStyle();
        body.setWrapText(true);
        setBorders(body);

        CellStyle wrap = workbook.createCellStyle();
        wrap.setWrapText(true);

        return new CellStyles(title, section, header, label, body, wrap);
    }

    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    record CellStyles(CellStyle title, CellStyle section, CellStyle header, CellStyle label, CellStyle body, CellStyle wrap) {
    }
}
