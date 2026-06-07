import { mkdtemp, readFile, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';

import ExcelJS from 'exceljs';

const execFileAsync = promisify(execFile);

export const WEEK_TYPES = ['EVEN', 'ODD'];
export const WEEK_DAYS = ['MONDAY', 'TUESDAY', 'THURSDAY', 'FRIDAY'];
export const TIME_SLOTS = ['MORNING', 'EVENING'];

const rootDir = resolve(fileURLToPath(new URL('..', import.meta.url)));
const backendDir = join(rootDir, 'carpool-back');
const workbookCliClass = 'com.carpool.workbook.template.WorkbookTemplateCli';

export async function generateWorkbookTemplate({ outputPath, families = [] }) {
  const resolvedOutputPath = resolve(rootDir, outputPath);
  let tempDir;
  let familiesJsonPath;

  try {
    if (families.length > 0) {
      tempDir = await mkdtemp(join(tmpdir(), 'carpool-workbook-'));
      familiesJsonPath = join(tempDir, 'families.json');
      await writeFile(familiesJsonPath, JSON.stringify({ families }, null, 2), 'utf8');
    }

    const args = [
      `-DskipTests`,
      `-Dworkbook.output=${resolvedOutputPath}`,
      `-Dexec.mainClass=${workbookCliClass}`,
    ];

    if (familiesJsonPath) {
      args.push(`-Dworkbook.familiesJson=${familiesJsonPath}`);
    }

    args.push('exec:java');

    await execFileAsync('mvn', args, {
      cwd: backendDir,
      env: process.env,
    });

    return {
      outputPath: resolvedOutputPath,
      familyCount: families.length,
    };
  } finally {
    if (tempDir) {
      await rm(tempDir, { recursive: true, force: true });
    }
  }
}

export function detectDuplicateChildren(families) {
  const childToFamilies = new Map();
  for (const family of families) {
    for (const childName of family.childNames ?? []) {
      if (!childToFamilies.has(childName)) {
        childToFamilies.set(childName, []);
      }
      childToFamilies.get(childName).push(family.familyName);
    }
  }
  return [...childToFamilies.entries()]
    .filter(([, familyNames]) => familyNames.length > 1)
    .map(([childName, familyNames]) => ({ childName, familyNames }));
}

export async function generatePrefilledWorkbook({ outputPath, families }) {
  if (!Array.isArray(families) || families.length === 0) {
    throw new Error('At least one family is required to generate a prefilled workbook.');
  }

  const duplicates = detectDuplicateChildren(families);
  if (duplicates.length > 0) {
    const details = duplicates
      .map(({ childName, familyNames }) => `"${childName}" appears in: ${familyNames.join(', ')}`)
      .join('; ');
    throw new Error(
      `A family is defined by its children, not by a parent. ` +
      `Each child must belong to exactly one family. ` +
      `Duplicate children found — ${details}. ` +
      `Merge the affected families into one before calling this tool.`
    );
  }

  const templateFamilies = families.map((family) => ({
    familyName: family.familyName,
    carCapacity: family.carCapacity,
    childNames: family.childNames,
  }));

  const templateResult = await generateWorkbookTemplate({ outputPath, families: templateFamilies });
  await prefillWorkbook({ workbookPath: templateResult.outputPath, families });

  return templateResult;
}

export async function prefillWorkbook({ workbookPath, families }) {
  const resolvedWorkbookPath = resolve(rootDir, workbookPath);
  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.readFile(resolvedWorkbookPath);

  for (const family of families) {
    const worksheet = findFamilyWorksheet(workbook, family.familyName);
    if (!worksheet) {
      throw new Error(`Family sheet not found for ${family.familyName}`);
    }

    setWorksheetText(worksheet, 4, 2, family.familyName);
    if (family.carCapacity != null) {
      worksheet.getCell(5, 2).value = family.carCapacity;
    }

    if (Array.isArray(family.childNames) && family.childNames.length > 0) {
      family.childNames.forEach((childName, index) => {
        setWorksheetText(worksheet, 16 + index, 1, childName);
      });
    }

    const preferenceMap = new Map(
      (family.preferences ?? []).map((p) => [slotKey(p.weekType, p.weekDay, p.timeSlot), p.value])
    );
    for (const weekType of WEEK_TYPES) {
      for (const weekDay of WEEK_DAYS) {
        for (const timeSlot of TIME_SLOTS) {
          const value = preferenceMap.get(slotKey(weekType, weekDay, timeSlot)) ?? 'OK';
          setWorksheetText(worksheet, 10, slotColumn(weekType, weekDay, timeSlot), value);
        }
      }
    }

    for (const absence of family.childAbsences ?? []) {
      const childRow = findChildRow(worksheet, absence.childName);
      if (!childRow) {
        throw new Error(`Child row not found for ${absence.childName} in family ${family.familyName}`);
      }
      const column = slotColumn(absence.weekType, absence.weekDay, absence.timeSlot);
      const existing = worksheet.getCell(childRow, column).text;
      // PRESENT wins over ABSENT: if any co-parent has the child on this slot, the child needs a seat
      if (existing !== 'PRESENT') {
        setWorksheetText(worksheet, childRow, column, absence.value);
      }
    }

    const notes = family.notes ?? {};
    setOptionalNote(worksheet, 'Garde alternee / contexte', notes.guardArrangement);
    setOptionalNote(worksheet, 'Lieu de RDV', notes.meetingPoint);
    setOptionalNote(worksheet, 'Whatsapp', notes.whatsapp);
    setOptionalNote(worksheet, 'Remarques', notes.remarks);
  }

  await workbook.xlsx.writeFile(resolvedWorkbookPath);

  return { workbookPath: resolvedWorkbookPath, familyCount: families.length };
}

function findFamilyWorksheet(workbook, familyName) {
  const indexSheet = workbook.getWorksheet('Index');
  if (!indexSheet) {
    return undefined;
  }

  for (let rowNumber = 2; rowNumber <= indexSheet.rowCount; rowNumber++) {
    const row = indexSheet.getRow(rowNumber);
    if (row.getCell(1).text === familyName) {
      const sheetName = row.getCell(2).text;
      return workbook.getWorksheet(sheetName);
    }
  }

  return undefined;
}

function findChildRow(worksheet, childName) {
  for (let rowNumber = 16; rowNumber <= worksheet.rowCount; rowNumber++) {
    if (worksheet.getCell(rowNumber, 1).text === childName) {
      return rowNumber;
    }
  }
  return undefined;
}

function findLabelRow(worksheet, label) {
  for (let rowNumber = 1; rowNumber <= worksheet.rowCount; rowNumber++) {
    if (worksheet.getCell(rowNumber, 1).text === label) {
      return rowNumber;
    }
  }
  return undefined;
}

function setOptionalNote(worksheet, label, value) {
  if (value == null) {
    return;
  }
  const row = findLabelRow(worksheet, label);
  if (!row) {
    throw new Error(`Note row not found for label ${label}`);
  }
  setWorksheetText(worksheet, row, 2, String(value));
}

function setWorksheetText(worksheet, row, column, value) {
  worksheet.getCell(row, column).value = value;
}

function slotKey(weekType, weekDay, timeSlot) {
  return `${weekType}|${weekDay}|${timeSlot}`;
}

function slotColumn(weekType, weekDay, timeSlot) {
  const weekTypeIndex = WEEK_TYPES.indexOf(weekType);
  const weekDayIndex = WEEK_DAYS.indexOf(weekDay);
  const timeSlotIndex = TIME_SLOTS.indexOf(timeSlot);

  if (weekTypeIndex === -1 || weekDayIndex === -1 || timeSlotIndex === -1) {
    throw new Error(`Invalid slot coordinates: ${weekType} ${weekDay} ${timeSlot}`);
  }

  return 2 + weekTypeIndex * 8 + weekDayIndex * 2 + timeSlotIndex;
}

export async function readWorkbookSnapshot(workbookPath, familyName) {
  const resolvedWorkbookPath = resolve(rootDir, workbookPath);
  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.readFile(resolvedWorkbookPath);
  const worksheet = findFamilyWorksheet(workbook, familyName);
  if (!worksheet) {
    throw new Error(`Family sheet not found for ${familyName}`);
  }

  return {
    familyName: worksheet.getCell(4, 2).text,
    carCapacity: worksheet.getCell(5, 2).value,
    firstPreference: worksheet.getCell(10, 2).text,
    secondPreference: worksheet.getCell(10, 3).text,  // EVEN|MONDAY|EVENING — default OK if not set
    firstChildName: worksheet.getCell(16, 1).text,
    firstChildFirstSlot: worksheet.getCell(16, 2).text,
  };
}
