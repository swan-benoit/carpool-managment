package com.carpool.workbook.template;

import java.util.List;

public record FamilyWorkbookTemplateData(String familyName, int carCapacity, List<String> childNames, String householdId) {

    public FamilyWorkbookTemplateData {
        childNames = childNames == null ? List.of() : List.copyOf(childNames);
    }
}
