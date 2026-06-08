package com.carpool.workbook.normalization;

import java.util.List;

public record WorkbookTripView(
        String weekDay,
        String timeSlot,
        List<WorkbookAssignmentView> assignments
) {
}
