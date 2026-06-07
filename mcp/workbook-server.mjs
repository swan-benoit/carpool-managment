import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { z } from 'zod/v4';

import { generatePrefilledWorkbook, generateWorkbookTemplate } from './workbook-tools.mjs';

const slotSchema = {
  weekType: z.enum(['EVEN', 'ODD']).describe('Week type'),
  weekDay: z.enum(['MONDAY', 'TUESDAY', 'THURSDAY', 'FRIDAY']).describe('Week day'),
  timeSlot: z.enum(['MORNING', 'EVENING']).describe('Time slot'),
};

const familyBaseSchema = {
  familyName: z.string().min(1).describe('Family display name'),
  carCapacity: z.number().int().positive().describe('Car capacity'),
  childNames: z.array(z.string().min(1)).describe('Child names for the family tab'),
};

const server = new McpServer({
  name: 'carpool-workbook',
  version: '0.1.0',
});

server.registerTool(
  'generate_workbook_template',
  {
    description: 'Generate the carpool workbook template XLSX. Optionally create one tab per provided family.',
    inputSchema: {
      outputPath: z.string().min(1).describe('Output path relative to the repository root'),
      families: z.array(z.object(familyBaseSchema)).optional().describe('Optional families to pre-create in the workbook'),
    },
  },
  async ({ outputPath, families = [] }) => {
    const result = await generateWorkbookTemplate({ outputPath, families });
    return {
      content: [{ type: 'text', text: `Workbook template generated at ${result.outputPath}` }],
    };
  },
);

server.registerTool(
  'generate_prefilled_workbook',
  {
    description: [
      'Generate and prefill a workbook from structured family data.',
      'A family is defined by its children (not by a parent): each child must appear in exactly one family.',
      'childAbsences records the recurring presence pattern for each child across the 16 weekly slots.',
      'For each child, you MUST provide one entry per slot — either PRESENT or ABSENT.',
      'Omitting childAbsences entirely leaves the presence grid blank, which blocks metrics computation.',
      'Derive the presence pattern from custody arrangements and school schedule before calling this tool.',
    ].join(' '),
    inputSchema: {
      outputPath: z.string().min(1).describe('Output path relative to the repository root'),
      families: z.array(z.object({
        ...familyBaseSchema,
        preferences: z.array(z.object({
          ...slotSchema,
          value: z.enum(['IMPOSSIBLE', 'EVITER', 'OK', 'PREFERE']).describe('Family preference value'),
        })).optional(),
        childAbsences: z.array(z.object({
          childName: z.string().min(1).describe('Child name matching a family child'),
          ...slotSchema,
          value: z.enum(['PRESENT', 'ABSENT']).describe('Child absence value'),
        })).describe('Recurring presence pattern — one entry per child per slot (16 slots × number of children). MUST be provided.'),
        notes: z.object({
          guardArrangement: z.string().optional(),
          meetingPoint: z.string().optional(),
          whatsapp: z.string().optional(),
          remarks: z.string().optional(),
        }).optional(),
      })).min(1),
    },
  },
  async ({ outputPath, families }) => {
    const result = await generatePrefilledWorkbook({ outputPath, families });

    const missingAbsences = families
      .filter((f) => f.childNames.length > 0 && (!f.childAbsences || f.childAbsences.length === 0))
      .map((f) => f.familyName);

    const lines = [`Prefilled workbook generated at ${result.outputPath}`];
    if (missingAbsences.length > 0) {
      lines.push('');
      lines.push('WARNING: the following families have children but no childAbsences data:');
      for (const name of missingAbsences) lines.push(`  • ${name}`);
      lines.push('The child presence grid is blank for these families. Metrics computation will be incomplete.');
      lines.push('Call this tool again with childAbsences filled in for each child and each of the 16 slots.');
    }

    return { content: [{ type: 'text', text: lines.join('\n') }] };
  },
);

const transport = new StdioServerTransport();
await server.connect(transport);
