package com.carpool.workbook.normalization;

import com.carpool.workbook.template.FamilyWorkbookTemplateData;
import com.carpool.workbook.template.WorkbookTemplateGenerator;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WorkbookFamilyReaderTest {

    private final WorkbookTemplateGenerator generator = new WorkbookTemplateGenerator();
    private final WorkbookFamilyReader reader = new WorkbookFamilyReader();

    private List<NormalizedWorkbookFamily> read(List<FamilyWorkbookTemplateData> families) throws IOException {
        byte[] bytes = generator.createWorkbookBytes(families);
        try (ByteArrayInputStream input = new ByteArrayInputStream(bytes)) {
            return reader.readWorkbookFamilies(input);
        }
    }

    @Test
    void relie_deux_onglets_partageant_le_meme_foyer() throws IOException {
        List<NormalizedWorkbookFamily> families = read(List.of(
                new FamilyWorkbookTemplateData("Josephine", 4, List.of("Orion"), "foyer-orion"),
                new FamilyWorkbookTemplateData("Mickael", 3, List.of(), "foyer-orion"),
                new FamilyWorkbookTemplateData("Autre", 4, List.of("Kid"), null)
        ));

        var josephine = families.stream().filter(f -> f.family().name.equals("Josephine")).findFirst().orElseThrow();
        var mickael = families.stream().filter(f -> f.family().name.equals("Mickael")).findFirst().orElseThrow();
        var autre = families.stream().filter(f -> f.family().name.equals("Autre")).findFirst().orElseThrow();

        assertEquals("foyer-orion", josephine.family().householdId);
        assertEquals("foyer-orion", mickael.family().householdId);
        assertNull(autre.family().householdId, "colonne Foyer vide => foyer solo");

        long membresDuFoyer = families.stream()
                .filter(f -> "foyer-orion".equals(f.family().householdId))
                .count();
        assertEquals(2, membresDuFoyer, "les deux co-parents forment un seul foyer");
    }
}
