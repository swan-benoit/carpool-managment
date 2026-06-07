package com.carpool.workbook.normalization;

import java.util.List;

public record WorkbookPerfectStatsResponse(
        Double totalRequiredTripsPerWeek,
        Double totalPerfectMeanTripPerWeek,
        List<WorkbookPerfectStat> families
) {
}
