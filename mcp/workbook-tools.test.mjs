import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';

import { detectDuplicateChildren, generatePrefilledWorkbook, readWorkbookSnapshot, WEEK_TYPES, WEEK_DAYS, TIME_SLOTS } from './workbook-tools.mjs';

const rootDir = fileURLToPath(new URL('..', import.meta.url));

test('generatePrefilledWorkbook creates workbook with family values', async () => {
  const tempDir = await mkdtemp(join(tmpdir(), 'carpool-mcp-'));
  const outputPath = join(tempDir, 'prefilled.xlsx');
  const relativeOutput = relative(rootDir, outputPath);

  await generatePrefilledWorkbook({
    outputPath: relativeOutput,
    families: [
      {
        familyName: 'Famille MCP',
        carCapacity: 5,
        childNames: ['Nina'],
        preferences: [
          { weekType: 'EVEN', weekDay: 'MONDAY', timeSlot: 'MORNING', value: 'PREFERE' },
        ],
        childAbsences: [
          { childName: 'Nina', weekType: 'EVEN', weekDay: 'MONDAY', timeSlot: 'MORNING', value: 'ABSENT' },
        ],
        notes: {
          meetingPoint: 'Bar du Pont',
          remarks: 'Prefill test',
        },
      },
    ],
  });

  const snapshot = await readWorkbookSnapshot(relativeOutput, 'Famille MCP');
  assert.equal(snapshot.familyName, 'Famille MCP');
  assert.equal(snapshot.carCapacity, 5);
  assert.equal(snapshot.firstPreference, 'PREFERE');
  assert.equal(snapshot.secondPreference, 'OK', 'unspecified preference slot must default to OK');
  assert.equal(snapshot.firstChildName, 'Nina');
  assert.equal(snapshot.firstChildFirstSlot, 'ABSENT');
});

test('detectDuplicateChildren returns empty array when all children are unique', () => {
  const families = [
    { familyName: 'Famille A', childNames: ['Alice'] },
    { familyName: 'Famille B', childNames: ['Bob'] },
  ];
  assert.deepEqual(detectDuplicateChildren(families), []);
});

test('detectDuplicateChildren detects same child in two families', () => {
  const families = [
    { familyName: 'Famille A', childNames: ['Alice', 'Bob'] },
    { familyName: 'Famille B', childNames: ['Bob'] },
  ];
  const result = detectDuplicateChildren(families);
  assert.equal(result.length, 1);
  assert.equal(result[0].childName, 'Bob');
  assert.deepEqual(result[0].familyNames, ['Famille A', 'Famille B']);
});

test('generatePrefilledWorkbook rejects duplicate children across families', async () => {
  const tempDir = await mkdtemp(join(tmpdir(), 'carpool-mcp-'));
  const outputPath = relative(rootDir, join(tempDir, 'dup.xlsx'));

  await assert.rejects(
    () => generatePrefilledWorkbook({
      outputPath,
      families: [
        { familyName: 'Famille A', carCapacity: 4, childNames: ['Luce'] },
        { familyName: 'Famille B', carCapacity: 4, childNames: ['Luce'] },
      ],
    }),
    (err) => {
      assert.ok(err.message.includes('Luce'), `expected "Luce" in error: ${err.message}`);
      assert.ok(err.message.includes('Famille A'), `expected "Famille A" in error: ${err.message}`);
      assert.ok(err.message.includes('Famille B'), `expected "Famille B" in error: ${err.message}`);
      return true;
    }
  );
});

test('co-parent child absences: PRESENT wins over ABSENT for the same slot', async () => {
  const tempDir = await mkdtemp(join(tmpdir(), 'carpool-mcp-'));
  const outputPath = relative(rootDir, join(tempDir, 'coparent.xlsx'));

  // Parent A has Eneour on even weeks (PRESENT even, ABSENT odd)
  // Parent B has Eneour on odd weeks (ABSENT even, PRESENT odd)
  // Result: Eneour should be PRESENT every slot
  const allSlots = (value) => WEEK_TYPES.flatMap((weekType) =>
    WEEK_DAYS.flatMap((weekDay) =>
      TIME_SLOTS.map((timeSlot) => ({ childName: 'Eneour', weekType, weekDay, timeSlot, value }))
    )
  );
  const evenPresent = allSlots('ABSENT').map((s) =>
    s.weekType === 'EVEN' ? { ...s, value: 'PRESENT' } : s
  );
  const oddPresent = allSlots('ABSENT').map((s) =>
    s.weekType === 'ODD' ? { ...s, value: 'PRESENT' } : s
  );

  await generatePrefilledWorkbook({
    outputPath,
    families: [{
      familyName: 'Famille Fusionnée',
      carCapacity: 4,
      childNames: ['Eneour'],
      childAbsences: [...evenPresent, ...oddPresent],
    }],
  });

  const snapshot = await readWorkbookSnapshot(outputPath, 'Famille Fusionnée');
  assert.equal(snapshot.firstChildFirstSlot, 'PRESENT', 'EVEN|MONDAY|MORNING should be PRESENT');
});
