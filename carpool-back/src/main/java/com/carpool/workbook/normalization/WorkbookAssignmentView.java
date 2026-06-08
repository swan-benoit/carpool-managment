package com.carpool.workbook.normalization;

import java.util.List;

public record WorkbookAssignmentView(
        String driverFamilyName,
        List<String> childNames
) {
}
