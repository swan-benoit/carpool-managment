package com.carpool.workbook.normalization;

import java.util.List;

public record WorkbookPlanningView(
        List<WorkbookTripView> evenWeek,
        List<WorkbookTripView> oddWeek
) {
}
