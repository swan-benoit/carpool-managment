package com.carpool.workbook.normalization;

import com.carpool.family.Family;
import com.carpool.schedule.Stats;
import com.carpool.schedule.calculator.ScheduleResult;
import com.carpool.schedule.calculator.ScheduleService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.nio.file.Path;
import java.util.List;

@ApplicationScoped
public class WorkbookStatsService {

    @Inject
    WorkbookFamilyReader workbookFamilyReader;

    @Inject
    ScheduleService scheduleService;

    public List<Stats> computeStats(Path workbookPath) {
        List<Family> families = workbookFamilyReader.readWorkbookFamilies(workbookPath).stream()
                .map(NormalizedWorkbookFamily::family)
                .toList();
        ScheduleResult schedule = scheduleService.generateSchedule(families);
        return families.stream()
                .map(family -> new Stats(
                        family,
                        schedule.meanTripPerWeek(family),
                        schedule.perfectMeanTripPerWeek(family)))
                .toList();
    }
}
