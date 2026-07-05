package com.carpool.workbook.normalization;

import com.carpool.family.Child;
import com.carpool.family.TimeSlot;
import com.carpool.family.WeekDay;
import com.carpool.family.WeekType;
import com.carpool.schedule.FamilyPlanningStats;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a live, editable workbook by reopening the source workbook (its original sheets are
 * kept verbatim) and appending one editable sheet per candidate. Each candidate sheet holds an
 * editable assignment list (one car per row, children in their own columns); every statistic
 * (trips/justice, avoid/preferred, completeness, capacity, redundancy) is an Excel formula that
 * recomputes when the list is edited. Preference and capacity are read live from each driver's
 * original family sheet via INDIRECT; presence and ideal trips are baked in as constants because
 * they are immutable inputs. A formula-built weekly grid mirrors the list for daily reading.
 */
public class WorkbookXlsxWriter {

    private static final byte[] COLOR_NAVY   = {(byte) 0x1F, (byte) 0x4E, (byte) 0x79};
    private static final byte[] COLOR_BLUE   = {(byte) 0x2E, (byte) 0x75, (byte) 0xB6};
    private static final byte[] COLOR_STRIPE = {(byte) 0xF2, (byte) 0xF7, (byte) 0xFD};
    private static final byte[] COLOR_LGREY  = {(byte) 0xF2, (byte) 0xF2, (byte) 0xF2};
    private static final byte[] COLOR_BORDER = {(byte) 0xB8, (byte) 0xCC, (byte) 0xE4};
    private static final byte[] COLOR_WHITE  = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    private static final byte[] COLOR_INPUT  = {(byte) 0xFF, (byte) 0xF6, (byte) 0xCC}; // jaune pâle = éditable
    private static final byte[] COLOR_RESULT = {(byte) 0xEA, (byte) 0xF3, (byte) 0xDF}; // vert pâle = calculé

    // Position des données dans les feuilles familles d'origine (cf. WorkbookFamilyReader)
    private static final int FAMILY_NAME_EXCEL_ROW = 4;          // ligne 0-based 3
    private static final String FAMILY_CAPACITY_EXCEL_CELL = "$B$5"; // capacité = ligne 0-based 4, col 1
    private static final int PREFERENCE_EXCEL_ROW = 10;          // ligne 0-based 9
    private static final int FIRST_SLOT_EXCEL_COL = 2;         // col 0-based 1 = colonne B = 2 en 1-based

    private static final List<WeekType> WEEK_TYPES = List.of(WeekType.EVEN, WeekType.ODD);
    private static final List<WeekDay> DAYS = List.of(WeekDay.MONDAY, WeekDay.TUESDAY, WeekDay.THURSDAY, WeekDay.FRIDAY);
    private static final List<TimeSlot> SLOTS = List.of(TimeSlot.MORNING, TimeSlot.EVENING);

    private static final Map<WeekType, String> WEEK_FR = Map.of(WeekType.EVEN, "Paire", WeekType.ODD, "Impaire");
    private static final Map<WeekDay, String> DAY_FR = Map.of(
            WeekDay.MONDAY, "Lundi", WeekDay.TUESDAY, "Mardi", WeekDay.THURSDAY, "Jeudi", WeekDay.FRIDAY, "Vendredi");
    private static final Map<TimeSlot, String> SLOT_FR = Map.of(TimeSlot.MORNING, "Matin", TimeSlot.EVENING, "Soir");

    private static final int SPARE_ROWS = 16;

    private record Slot(WeekType weekType, WeekDay weekDay, TimeSlot timeSlot, String week, String day, String slot, String key) {
    }

    private record ChildRef(String familyName, String childName, boolean[] present) {
    }

    private List<Slot> buildSlots() {
        List<Slot> slots = new ArrayList<>();
        for (WeekType weekType : WEEK_TYPES) {
            for (WeekDay weekDay : DAYS) {
                for (TimeSlot timeSlot : SLOTS) {
                    String week = WEEK_FR.get(weekType);
                    String day = DAY_FR.get(weekDay);
                    String slot = SLOT_FR.get(timeSlot);
                    slots.add(new Slot(weekType, weekDay, timeSlot, week, day, slot, week + "|" + day + "|" + slot));
                }
            }
        }
        return slots;
    }

    public void write(WorkbookPerfectStatsResponse response, List<NormalizedWorkbookFamily> normalizedFamilies, Path inputPath, Path outputPath) throws IOException {
        List<Slot> slots = buildSlots();

        Map<String, Double> idealByFamily = new LinkedHashMap<>();
        if (response.families() != null) {
            for (WorkbookPerfectStat stat : response.families()) {
                idealByFamily.put(stat.familyName(), stat.perfectMeanTripPerWeek());
            }
        }

        List<ChildRef> children = new ArrayList<>();
        int maxCapacity = 1;
        for (NormalizedWorkbookFamily nf : normalizedFamilies) {
            maxCapacity = Math.max(maxCapacity, nf.family().carCapacity);
            for (Child child : nf.family().children) {
                boolean[] present = new boolean[slots.size()];
                for (int i = 0; i < slots.size(); i++) {
                    Slot slot = slots.get(i);
                    present[i] = FamilyPlanningStats.isPresent(child, slot.weekType(), slot.weekDay(), slot.timeSlot());
                }
                children.add(new ChildRef(nf.family().name, child.name, present));
            }
        }

        try (InputStream in = Files.newInputStream(inputPath);
             XSSFWorkbook workbook = new XSSFWorkbook(in)) {

            String familySheetPrefix = detectFamilySheetPrefix(workbook, normalizedFamilies);
            Styles s = new Styles(workbook);

            List<WorkbookPlanCandidateView> candidates = response.planCandidates();
            if (candidates != null) {
                for (WorkbookPlanCandidateView candidate : candidates) {
                    createLivePlanningSheet(workbook, s, candidate, idealByFamily, children, slots, maxCapacity, familySheetPrefix);
                }
            }

            try (OutputStream out = Files.newOutputStream(outputPath)) {
                workbook.write(out);
            }
        }
    }

    /**
     * Family sheets in the template are named "Famille - &lt;name&gt;". We detect the shared prefix
     * by matching each family name against the sheet whose name cell (B4) holds it, so INDIRECT can
     * rebuild the sheet name from an (editable) driver name at recompute time.
     */
    private String detectFamilySheetPrefix(XSSFWorkbook workbook, List<NormalizedWorkbookFamily> normalizedFamilies) {
        for (NormalizedWorkbookFamily nf : normalizedFamilies) {
            String familyName = nf.family().name;
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                Row row = sheet.getRow(FAMILY_NAME_EXCEL_ROW - 1);
                Cell cell = row == null ? null : row.getCell(1);
                String value = cell == null || cell.getCellType() != org.apache.poi.ss.usermodel.CellType.STRING
                        ? null : cell.getStringCellValue().trim();
                if (familyName.equals(value)) {
                    String sheetName = sheet.getSheetName();
                    if (sheetName.endsWith(familyName)) {
                        return sheetName.substring(0, sheetName.length() - familyName.length());
                    }
                    return "";
                }
            }
        }
        return "";
    }

    private void createLivePlanningSheet(
            XSSFWorkbook wb, Styles s, WorkbookPlanCandidateView candidate,
            Map<String, Double> idealByFamily, List<ChildRef> children, List<Slot> slots,
            int maxCapacity, String familySheetPrefix
    ) {
        XSSFSheet sheet = wb.createSheet("#" + candidate.rank());
        List<String> familyNames = new ArrayList<>(idealByFamily.keySet());
        int famCount = familyNames.size();
        int childCount = children.size();
        int slotCount = slots.size();

        int colSemaine = 0;
        int colJour = colSemaine + 1;
        int colCreneau = colSemaine + 2;
        int colDriver = colSemaine + 3;
        int colLieu = colSemaine + 4;
        int colChildFirst = colSemaine + 5;
        int colChildLast = colChildFirst + maxCapacity - 1;
        int gridColFirst = colChildLast + 2;
        int compColFam = colSemaine;
        int compColChild = compColFam + 1;
        int compColSlotFirst = compColFam + 2;
        int compColManq = compColSlotFirst + slotCount;
        int hKey = compColManq + 2;
        int hNb = hKey + 1;
        int hPref = hKey + 2;
        int hCap = hKey + 3;
        int hOver = hKey + 4;
        int hConcat = hKey + 5;
        int hAvailFirst = hConcat + 1;
        int hAvailLast = hAvailFirst + childCount - 1;

        List<String[]> seedRows = new ArrayList<>();
        if (candidate.planning() != null) {
            appendWeekRows(seedRows, "Paire", candidate.planning().evenWeek(), maxCapacity);
            appendWeekRows(seedRows, "Impaire", candidate.planning().oddWeek(), maxCapacity);
        }
        int listTotalRows = seedRows.size() + SPARE_ROWS;

        int row = 0;
        int titleRow = row++;
        int instrRow = row++;
        row++;
        int listHeaderRow = row++;
        int listFirst = row;
        int listLast = listFirst + listTotalRows - 1;
        row = listLast + 2;
        int statsHeaderRow = row++;
        int statsFirst = row;
        int statsLast = statsFirst + famCount - 1;
        row = statsLast + 2;
        int summaryRow = row;
        row += 10;
        int slotHeaderRow = row++;
        int slotFirst = row;
        int slotLast = slotFirst + slotCount - 1;
        row = slotLast + 2;
        int compHeaderRow = row++;
        int compKeysRow = row++;
        int compFirst = row;
        int compLast = compFirst + childCount - 1;

        String driverRange = absRange(colDriver, listFirst, listLast);
        String lieuRange = absRange(colLieu, listFirst, listLast);
        String keyRange = absRange(hKey, listFirst, listLast);
        String nbRange = absRange(hNb, listFirst, listLast);
        String prefRange = absRange(hPref, listFirst, listLast);
        String overRange = absRange(hOver, listFirst, listLast);
        String concatRange = absRange(hConcat, listFirst, listLast);
        String prefixLit = "\"'" + familySheetPrefix + "\"";

        String compChildNames = absRange(compColChild, compFirst, compLast);
        String compSlotMatrix = abs(compColSlotFirst, compFirst) + ":" + abs(compColSlotFirst + slotCount - 1, compLast);
        String compKeysRange = abs(compColSlotFirst, compKeysRow) + ":" + abs(compColSlotFirst + slotCount - 1, compKeysRow);

        title(sheet, s, titleRow, "Planning #" + candidate.rank() + " — éditable", colChildLast);
        XSSFRow instr = sheet.createRow(instrRow);
        cell(instr, 0, "Modifiez la liste (cellules jaunes). Stats, grille et complétude se recalculent. Menus Enfant : enfants du créneau restant à transporter. Préférences/capacités lues dans les feuilles familles.", s.note);
        sheet.addMergedRegion(new CellRangeAddress(instrRow, instrRow, 0, colChildLast));

        // En-tête liste
        XSSFRow lh = sheet.createRow(listHeaderRow);
        cell(lh, colSemaine, "Semaine", s.tableHeader);
        cell(lh, colJour, "Jour", s.tableHeader);
        cell(lh, colCreneau, "Créneau", s.tableHeader);
        cell(lh, colDriver, "Conducteur", s.tableHeaderLeft);
        cell(lh, colLieu, "Lieu", s.tableHeaderLeft);
        for (int k = 0; k < maxCapacity; k++) {
            cell(lh, colChildFirst + k, "Enfant " + (k + 1), s.tableHeaderLeft);
        }
        for (int c = hKey; c <= hAvailLast; c++) {
            cell(lh, c, "·", s.tableHeader);
        }

        // Lignes liste
        for (int i = 0; i < listTotalRows; i++) {
            int r = listFirst + i;
            XSSFRow dr = sheet.createRow(r);
            String[] seed = i < seedRows.size() ? seedRows.get(i) : null;
            cell(dr, colSemaine, seed != null ? seed[0] : "", s.cellInputC);
            cell(dr, colJour, seed != null ? seed[1] : "", s.cellInputC);
            cell(dr, colCreneau, seed != null ? seed[2] : "", s.cellInputC);
            cell(dr, colDriver, seed != null ? seed[3] : "", s.cellInput);
            cell(dr, colLieu, "", s.cellInput);
            for (int k = 0; k < maxCapacity; k++) {
                String childVal = seed != null && (4 + k) < seed.length ? seed[4 + k] : "";
                cell(dr, colChildFirst + k, childVal, s.cellInput);
            }
            String childCells = abs(colChildFirst, r) + ":" + abs(colChildLast, r);
            String driver = abs(colDriver, r);
            String sheetRef = prefixLit + "&" + driver + "&\"'!\"";
            String colNum = "(" + FIRST_SLOT_EXCEL_COL + "+IF(" + abs(colSemaine, r) + "=\"Paire\",0,1)*8"
                    + "+(MATCH(" + abs(colJour, r) + ",{\"Lundi\",\"Mardi\",\"Jeudi\",\"Vendredi\"},0)-1)*2"
                    + "+IF(" + abs(colCreneau, r) + "=\"Matin\",0,1))";
            formula(dr, hKey, "IF(" + abs(colSemaine, r) + "=\"\",\"\"," + abs(colSemaine, r) + "&\"|\"&" + abs(colJour, r) + "&\"|\"&" + abs(colCreneau, r) + ")", s.cellHelper);
            formula(dr, hNb, "COUNTA(" + childCells + ")", s.cellHelper);
            formula(dr, hPref, "IF(" + driver + "=\"\",\"OK\",IFERROR(UPPER(INDIRECT(" + sheetRef + "&ADDRESS(" + PREFERENCE_EXCEL_ROW + "," + colNum + "))),\"OK\"))", s.cellHelper);
            formula(dr, hCap, "IF(" + driver + "=\"\",0,IFERROR(INDIRECT(" + sheetRef + "&\"" + FAMILY_CAPACITY_EXCEL_CELL + "\"),0))", s.cellHelper);
            formula(dr, hOver, "IF(" + abs(hNb, r) + ">" + abs(hCap, r) + "," + abs(hNb, r) + "-" + abs(hCap, r) + ",0)", s.cellHelper);
            formula(dr, hConcat, "\",\"&_xlfn.TEXTJOIN(\",\",TRUE," + childCells + ")&\",\"", s.cellHelper);
            for (int k = 0; k < childCount; k++) {
                int c = hAvailFirst + k;
                String avail = "IFERROR(INDEX(" + compChildNames
                        + ",SMALL(IF(INDEX(" + compSlotMatrix + ",0,MATCH(" + abs(hKey, r) + "," + compKeysRange + ",0))=1"
                        + ",ROW(" + compChildNames + ")-ROW(" + abs(compColChild, compFirst) + ")+1)," + (k + 1) + ")),\"\")";
                sheet.setArrayFormula(avail, new CellRangeAddress(r, r, c, c));
                XSSFCell availCell = dr.getCell(c);
                if (availCell != null) {
                    availCell.setCellStyle(s.cellHelper);
                }
            }
        }
        for (int c = hKey; c <= hAvailLast; c++) {
            sheet.setColumnHidden(c, true);
        }

        // Menu déroulant enfants : par ligne, seuls les enfants du créneau encore non transportés
        XSSFDataValidationHelper dvHelper = new XSSFDataValidationHelper(sheet);
        for (int i = 0; i < listTotalRows; i++) {
            int r = listFirst + i;
            DataValidationConstraint childListConstraint = dvHelper.createFormulaListConstraint(
                    abs(hAvailFirst, r) + ":" + abs(hAvailLast, r));
            CellRangeAddressList childListRegions = new CellRangeAddressList(r, r, colChildFirst, colChildLast);
            DataValidation childValidation = dvHelper.createValidation(childListConstraint, childListRegions);
            childValidation.setEmptyCellAllowed(true);
            childValidation.setSuppressDropDownArrow(true);
            childValidation.setShowErrorBox(false);
            sheet.addValidationData(childValidation);
        }

        // Menu déroulant conducteurs (source : colonne Famille des stats)
        DataValidationConstraint driverConstraint = dvHelper.createFormulaListConstraint(
                absRange(colSemaine, statsFirst, statsLast));
        CellRangeAddressList driverRegions = new CellRangeAddressList(listFirst, listLast, colDriver, colDriver);
        DataValidation driverValidation = dvHelper.createValidation(driverConstraint, driverRegions);
        driverValidation.setEmptyCellAllowed(true);
        driverValidation.setSuppressDropDownArrow(true);
        driverValidation.setShowErrorBox(true);
        sheet.addValidationData(driverValidation);

        // Complétude (présence = constante input ; transport = live)
        section(sheet, s, compHeaderRow, "Complétude (1 = enfant présent non transporté)", compColFam, compColManq);
        XSSFRow keysRow = sheet.createRow(compKeysRow);
        for (int j = 0; j < slotCount; j++) {
            cell(keysRow, compColSlotFirst + j, slots.get(j).key(), s.cellHelper);
        }
        keysRow.setZeroHeight(true);
        for (int i = 0; i < childCount; i++) {
            int r = compFirst + i;
            ChildRef child = children.get(i);
            XSSFRow dr = sheet.createRow(r);
            cell(dr, compColFam, child.familyName(), s.cellHelper);
            cell(dr, compColChild, child.childName(), s.cellHelper);
            for (int j = 0; j < slotCount; j++) {
                int c = compColSlotFirst + j;
                if (!child.present()[j]) {
                    cellNum(dr, c, 0, s.cellHelper);
                    continue;
                }
                String keyLit = "\"" + slots.get(j).key() + "\"";
                String occ = "SUMPRODUCT((" + keyRange + "=" + keyLit + ")*ISNUMBER(SEARCH(\",\"&" + abs(compColChild, r) + "&\",\"," + concatRange + ")))";
                formula(dr, c, "IF(" + occ + ">=1,0,1)", s.cellHelper);
            }
            formula(dr, compColManq, "SUM(" + abs(compColSlotFirst, r) + ":" + abs(compColSlotFirst + slotCount - 1, r) + ")", s.cellHelper);
        }
        String compFamRange = absRange(compColFam, compFirst, compLast);
        String compManqRange = absRange(compColManq, compFirst, compLast);

        // Stats par famille
        String[] statHeaders = {"Famille", "Réel/sem", "Idéal/sem", "Écart", "Justice", "Éviter", "Préféré", "Impossible", "Manquants"};
        XSSFRow sth = sheet.createRow(statsHeaderRow);
        for (int c = 0; c < statHeaders.length; c++) {
            cell(sth, colSemaine + c, statHeaders[c], c == 0 ? s.tableHeaderLeft : s.tableHeader);
        }
        for (int i = 0; i < famCount; i++) {
            int r = statsFirst + i;
            String name = familyNames.get(i);
            boolean stripe = i % 2 == 1;
            XSSFRow dr = sheet.createRow(r);
            String fam = abs(colSemaine, r);
            cell(dr, colSemaine, name, stripe ? s.cellFamilyStripe : s.cellFamily);
            formula(dr, colSemaine + 1, "COUNTIF(" + driverRange + "," + fam + ")/2", stripe ? s.cellNumStripe : s.cellNum);
            cellNum(dr, colSemaine + 2, idealByFamily.getOrDefault(name, 0.0), stripe ? s.cellNumStripe : s.cellNum);
            formula(dr, colSemaine + 3, "ABS(" + abs(colSemaine + 1, r) + "-" + abs(colSemaine + 2, r) + ")", stripe ? s.cellNumStripe : s.cellNum);
            formula(dr, colSemaine + 5, "COUNTIFS(" + driverRange + "," + fam + "," + prefRange + ",\"EVITER\")", stripe ? s.cellIntStripe : s.cellInt);
            formula(dr, colSemaine + 6, "COUNTIFS(" + driverRange + "," + fam + "," + prefRange + ",\"PREFERE\")", stripe ? s.cellIntStripe : s.cellInt);
            formula(dr, colSemaine + 7, "COUNTIFS(" + driverRange + "," + fam + "," + prefRange + ",\"IMPOSSIBLE\")", stripe ? s.cellIntStripe : s.cellInt);
            formula(dr, colSemaine + 8, "SUMIF(" + compFamRange + "," + fam + "," + compManqRange + ")", stripe ? s.cellIntStripe : s.cellInt);
            String reel = abs(colSemaine + 1, r);
            String ideal = abs(colSemaine + 2, r);
            String ecart = abs(colSemaine + 3, r);
            String evit = abs(colSemaine + 5, r);
            String pref = abs(colSemaine + 6, r);
            String impos = abs(colSemaine + 7, r);
            String manq = abs(colSemaine + 8, r);
            String devPen = "MIN(1,IF(" + ideal + "<=0,IF(" + reel + "=0,0,1)," + ecart + "/" + ideal + "))*0.8";
            String avoidPen = "MIN(0.15," + evit + "*0.05)";
            String prefBonus = "IF(AND(" + pref + ">0," + evit + "=0),MIN(0.05," + pref + "*0.01),0)";
            String base = "1-(" + devPen + ")-(" + avoidPen + ")+(" + prefBonus + ")";
            String justice = "IF(OR(" + impos + ">0," + manq + ">0),0,MAX(0,MIN(1," + base + ")))";
            formula(dr, colSemaine + 4, justice, stripe ? s.cellNumStripe : s.cellNum);
        }
        String justiceRange = absRange(colSemaine + 4, statsFirst, statsLast);
        String evitRange = absRange(colSemaine + 5, statsFirst, statsLast);
        String prefStatRange = absRange(colSemaine + 6, statsFirst, statsLast);
        String imposRange = absRange(colSemaine + 7, statsFirst, statsLast);
        String manqRange = absRange(colSemaine + 8, statsFirst, statsLast);

        // Redondance par créneau
        XSSFRow slh = sheet.createRow(slotHeaderRow);
        String[] slotHeaders = {"Créneau", "Voitures", "Enfants", "Min voitures", "Redondants"};
        for (int c = 0; c < slotHeaders.length; c++) {
            cell(slh, colSemaine + c, slotHeaders[c], c == 0 ? s.tableHeaderLeft : s.tableHeader);
        }
        for (int j = 0; j < slotCount; j++) {
            int r = slotFirst + j;
            XSSFRow dr = sheet.createRow(r);
            String keyLit = "\"" + slots.get(j).key() + "\"";
            cell(dr, colSemaine, slots.get(j).key(), s.cellHelper);
            formula(dr, colSemaine + 1, "COUNTIF(" + keyRange + "," + keyLit + ")", s.cellInt);
            formula(dr, colSemaine + 2, "SUMIF(" + keyRange + "," + keyLit + "," + nbRange + ")", s.cellInt);
            formula(dr, colSemaine + 3, "IF(" + abs(colSemaine + 2, r) + "=0,0,ROUNDUP(" + abs(colSemaine + 2, r) + "/" + maxCapacity + ",0))", s.cellInt);
            formula(dr, colSemaine + 4, "MAX(0," + abs(colSemaine + 1, r) + "-" + abs(colSemaine + 3, r) + ")", s.cellInt);
        }
        String redundantRange = absRange(colSemaine + 4, slotFirst, slotLast);

        // Résumé global
        int sr = summaryRow;
        sr = kvFormula(sheet, s, sr, colSemaine, "Justice minimale", "MIN(" + justiceRange + ")");
        sr = kvFormula(sheet, s, sr, colSemaine, "Justice moyenne", "AVERAGE(" + justiceRange + ")");
        sr = kvFormula(sheet, s, sr, colSemaine, "Affectations à éviter", "SUM(" + evitRange + ")");
        sr = kvFormula(sheet, s, sr, colSemaine, "Affectations préférées", "SUM(" + prefStatRange + ")");
        sr = kvFormula(sheet, s, sr, colSemaine, "Affectations impossibles", "SUM(" + imposRange + ")");
        sr = kvFormula(sheet, s, sr, colSemaine, "Transports manquants", "SUM(" + manqRange + ")");
        sr = kvFormula(sheet, s, sr, colSemaine, "Planning complet", "IF(SUM(" + manqRange + ")=0,\"Oui\",\"Non\")");
        sr = kvFormula(sheet, s, sr, colSemaine, "Dépassements de capacité", "COUNTIF(" + overRange + ",\">0\")");
        kvFormula(sheet, s, sr, colSemaine, "Conducteurs redondants (approx.)", "SUM(" + redundantRange + ")");

        // Grille hebdo à droite de la zone de saisie
        int gridRow = buildGrid(sheet, s, listHeaderRow, gridColFirst, "Semaine paire", "Paire", keyRange, driverRange, lieuRange, concatRange);
        buildGrid(sheet, s, gridRow + 1, gridColFirst, "Semaine impaire", "Impaire", keyRange, driverRange, lieuRange, concatRange);

        sheet.setColumnWidth(gridColFirst - 1, 3 * 256);
        sheet.setColumnWidth(gridColFirst, 9 * 256);
        for (int d = 1; d <= DAYS.size(); d++) {
            sheet.setColumnWidth(gridColFirst + d, 40 * 256);
        }
        sheet.setColumnWidth(colSemaine, 16 * 256);
        sheet.setColumnWidth(colJour, 10 * 256);
        sheet.setColumnWidth(colCreneau, 9 * 256);
        sheet.setColumnWidth(colDriver, 22 * 256);
        sheet.setColumnWidth(colLieu, 16 * 256);
        for (int k = 0; k < maxCapacity; k++) {
            sheet.setColumnWidth(colChildFirst + k, 14 * 256);
        }
        sheet.createFreezePane(0, listHeaderRow + 1);
    }

    private int buildGrid(
            XSSFSheet sheet, Styles s, int startRow, int firstCol, String title, String weekLabel,
            String keyRange, String driverRange, String lieuRange, String concatRange
    ) {
        section(sheet, s, startRow++, title, firstCol, firstCol + DAYS.size());
        XSSFRow header = rowOf(sheet, startRow++);
        cell(header, firstCol, "", s.tableHeader);
        for (int d = 0; d < DAYS.size(); d++) {
            cell(header, firstCol + d + 1, DAY_FR.get(DAYS.get(d)), s.tableHeader);
        }
        for (int si = 0; si < SLOTS.size(); si++) {
            String slotFr = SLOT_FR.get(SLOTS.get(si));
            XSSFRow r = rowOf(sheet, startRow);
            r.setHeightInPoints(90);
            cell(r, firstCol, slotFr, s.cellSlotLabel);
            for (int d = 0; d < DAYS.size(); d++) {
                String key = weekLabel + "|" + DAY_FR.get(DAYS.get(d)) + "|" + slotFr;
                String keyLit = "\"" + key + "\"";
                String driverText = driverRange + "&IF(" + lieuRange + "=\"\",\"\",\" @ \"&" + lieuRange + ")"
                        + "&\" (\"&SUBSTITUTE(MID(" + concatRange + ",2,LEN(" + concatRange + ")-2),\",\",\", \")&\")\"";
                String formula = "_xlfn.TEXTJOIN(CHAR(10),TRUE,IF(" + keyRange + "=" + keyLit + "," + driverText + ",\"\"))";
                int colIdx = firstCol + d + 1;
                sheet.setArrayFormula(formula, new CellRangeAddress(startRow, startRow, colIdx, colIdx));
                XSSFCell c = r.getCell(colIdx);
                if (c != null) {
                    c.setCellStyle(s.cellGrid);
                }
            }
            startRow++;
        }
        return startRow;
    }

    private XSSFRow rowOf(XSSFSheet sheet, int rowIndex) {
        XSSFRow row = sheet.getRow(rowIndex);
        return row != null ? row : sheet.createRow(rowIndex);
    }

    private void appendWeekRows(List<String[]> rows, String weekLabel, List<WorkbookTripView> trips, int maxCapacity) {
        if (trips == null) {
            return;
        }
        for (WorkbookTripView trip : trips) {
            String day = DAY_FR.getOrDefault(WeekDay.valueOf(trip.weekDay()), trip.weekDay());
            String slot = SLOT_FR.getOrDefault(TimeSlot.valueOf(trip.timeSlot()), trip.timeSlot());
            for (WorkbookAssignmentView assignment : trip.assignments()) {
                String[] cells = new String[4 + maxCapacity];
                cells[0] = weekLabel;
                cells[1] = day;
                cells[2] = slot;
                cells[3] = assignment.driverFamilyName();
                List<String> kids = assignment.childNames();
                for (int k = 0; k < maxCapacity; k++) {
                    cells[4 + k] = k < kids.size() ? kids.get(k) : "";
                }
                rows.add(cells);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers de mise en page
    // -------------------------------------------------------------------------

    private int title(XSSFSheet sheet, Styles s, int row, String text, int lastCol) {
        XSSFRow r = sheet.createRow(row);
        r.setHeightInPoints(24);
        XSSFCell c = r.createCell(0);
        c.setCellValue(text);
        c.setCellStyle(s.title);
        sheet.addMergedRegion(new CellRangeAddress(row, row, 0, lastCol));
        return row + 1;
    }

    private int section(XSSFSheet sheet, Styles s, int row, String text, int firstCol, int lastCol) {
        XSSFRow r = sheet.getRow(row) != null ? sheet.getRow(row) : sheet.createRow(row);
        r.setHeightInPoints(16);
        XSSFCell c = r.createCell(firstCol);
        c.setCellValue(text);
        c.setCellStyle(s.sectionHeader);
        sheet.addMergedRegion(new CellRangeAddress(row, row, firstCol, lastCol));
        return row + 1;
    }

    private int kvFormula(XSSFSheet sheet, Styles s, int row, int firstCol, String key, String formula) {
        XSSFRow r = sheet.getRow(row) != null ? sheet.getRow(row) : sheet.createRow(row);
        r.setHeightInPoints(15);
        XSSFCell kc = r.createCell(firstCol);
        kc.setCellValue(key);
        kc.setCellStyle(s.label);
        XSSFCell vc = r.createCell(firstCol + 1);
        vc.setCellFormula(formula);
        vc.setCellStyle(s.cellResult);
        return row + 1;
    }

    private void cell(XSSFRow row, int col, String value, XSSFCellStyle style) {
        XSSFCell c = row.createCell(col);
        c.setCellValue(value);
        c.setCellStyle(style);
    }

    private void cellNum(XSSFRow row, int col, double value, XSSFCellStyle style) {
        XSSFCell c = row.createCell(col);
        c.setCellValue(value);
        c.setCellStyle(style);
    }

    private void formula(XSSFRow row, int col, String formula, XSSFCellStyle style) {
        XSSFCell c = row.createCell(col);
        c.setCellFormula(formula);
        c.setCellStyle(style);
    }

    private static String col(int index) {
        return CellReference.convertNumToColString(index);
    }

    private static String abs(int colIndex, int rowIndex) {
        return "$" + col(colIndex) + "$" + (rowIndex + 1);
    }

    private static String absRange(int colIndex, int firstRow, int lastRow) {
        return "$" + col(colIndex) + "$" + (firstRow + 1) + ":$" + col(colIndex) + "$" + (lastRow + 1);
    }

    // -------------------------------------------------------------------------
    // Styles
    // -------------------------------------------------------------------------

    private static final class Styles {
        final XSSFCellStyle title, sectionHeader, label, note;
        final XSSFCellStyle tableHeader, tableHeaderLeft;
        final XSSFCellStyle cellFamily, cellFamilyStripe;
        final XSSFCellStyle cellNum, cellNumStripe;
        final XSSFCellStyle cellInt, cellIntStripe;
        final XSSFCellStyle cellInput, cellInputC;
        final XSSFCellStyle cellResult, cellHelper;
        final XSSFCellStyle cellSlotLabel, cellGrid;

        Styles(XSSFWorkbook wb) {
            short dec2 = wb.createDataFormat().getFormat("0.00");

            XSSFFont fBase = font(wb, false, false, 11, null);
            XSSFFont fBold = font(wb, true, false, 11, null);
            XSSFFont fTitleWhite = font(wb, true, false, 14, COLOR_WHITE);
            XSSFFont fWhite = font(wb, true, false, 11, COLOR_WHITE);
            XSSFFont fGrey = font(wb, false, true, 10, new byte[]{(byte) 0x80, (byte) 0x80, (byte) 0x80});

            title = style(wb, fTitleWhite, COLOR_NAVY, (short) 0, false, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, null);

            sectionHeader = style(wb, fWhite, COLOR_BLUE, (short) 0, true, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, COLOR_BORDER);

            note = style(wb, fGrey, null, (short) 0, false, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, null);
            label = style(wb, fBold, COLOR_LGREY, (short) 0, true, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, COLOR_BORDER);

            tableHeader = style(wb, fWhite, COLOR_NAVY, (short) 0, true, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, COLOR_BORDER);
            tableHeaderLeft = style(wb, fWhite, COLOR_NAVY, (short) 0, true, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, COLOR_BORDER);

            cellFamily = style(wb, fBold, null, (short) 0, true, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, COLOR_BORDER);
            cellFamilyStripe = style(wb, fBold, COLOR_STRIPE, (short) 0, true, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, COLOR_BORDER);
            cellNum = style(wb, fBase, null, dec2, true, HorizontalAlignment.RIGHT, VerticalAlignment.CENTER, COLOR_BORDER);
            cellNumStripe = style(wb, fBase, COLOR_STRIPE, dec2, true, HorizontalAlignment.RIGHT, VerticalAlignment.CENTER, COLOR_BORDER);
            cellInt = style(wb, fBase, null, (short) 0, true, HorizontalAlignment.RIGHT, VerticalAlignment.CENTER, COLOR_BORDER);
            cellIntStripe = style(wb, fBase, COLOR_STRIPE, (short) 0, true, HorizontalAlignment.RIGHT, VerticalAlignment.CENTER, COLOR_BORDER);

            cellInput = style(wb, fBase, COLOR_INPUT, (short) 0, true, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, COLOR_BORDER);
            cellInputC = style(wb, fBase, COLOR_INPUT, (short) 0, true, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, COLOR_BORDER);
            cellResult = style(wb, fBold, COLOR_RESULT, (short) 0, true, HorizontalAlignment.RIGHT, VerticalAlignment.CENTER, COLOR_BORDER);
            cellHelper = style(wb, fBase, COLOR_LGREY, (short) 0, true, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, COLOR_BORDER);

            cellSlotLabel = style(wb, fBold, new byte[]{(byte) 0xD6, (byte) 0xE4, (byte) 0xF0}, (short) 0, true, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, COLOR_BORDER);
            cellGrid = makeGridCell(wb, fBase);
        }

        private static XSSFFont font(XSSFWorkbook wb, boolean bold, boolean italic, int size, byte[] color) {
            XSSFFont f = wb.createFont();
            f.setBold(bold);
            f.setItalic(italic);
            f.setFontHeightInPoints((short) size);
            if (color != null) {
                f.setColor(xc(color));
            }
            return f;
        }

        private static XSSFCellStyle style(XSSFWorkbook wb, XSSFFont font, byte[] bg, short fmt,
                                           boolean borders, HorizontalAlignment ha, VerticalAlignment va, byte[] borderColor) {
            XSSFCellStyle st = wb.createCellStyle();
            st.setFont(font);
            if (bg != null) {
                st.setFillForegroundColor(xc(bg));
                st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            if (fmt != 0) {
                st.setDataFormat(fmt);
            }
            st.setAlignment(ha);
            st.setVerticalAlignment(va);
            if (borders) {
                XSSFColor bc = xc(borderColor != null ? borderColor : COLOR_BORDER);
                st.setBorderTop(BorderStyle.THIN);
                st.setTopBorderColor(bc);
                st.setBorderBottom(BorderStyle.THIN);
                st.setBottomBorderColor(bc);
                st.setBorderLeft(BorderStyle.THIN);
                st.setLeftBorderColor(bc);
                st.setBorderRight(BorderStyle.THIN);
                st.setRightBorderColor(bc);
            }
            return st;
        }

        private static XSSFCellStyle makeGridCell(XSSFWorkbook wb, XSSFFont font) {
            XSSFCellStyle st = wb.createCellStyle();
            st.setFont(font);
            st.setWrapText(true);
            st.setAlignment(HorizontalAlignment.LEFT);
            st.setVerticalAlignment(VerticalAlignment.TOP);
            XSSFColor bc = xc(COLOR_BORDER);
            st.setBorderTop(BorderStyle.THIN);
            st.setTopBorderColor(bc);
            st.setBorderBottom(BorderStyle.THIN);
            st.setBottomBorderColor(bc);
            st.setBorderLeft(BorderStyle.THIN);
            st.setLeftBorderColor(bc);
            st.setBorderRight(BorderStyle.THIN);
            st.setRightBorderColor(bc);
            return st;
        }

        private static XSSFColor xc(byte[] rgb) {
            return new XSSFColor(rgb, null);
        }
    }
}
