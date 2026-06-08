package com.carpool.schedule.calculator;

import com.carpool.family.*;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ScheduleService {

    public ScheduleResult generateSchedule(List<Family> families) {
        return generateSchedule(families, Map.of());
    }

    public ScheduleResult generateSchedule(List<Family> families, Map<String, Double> maxFamilyTrips) {
       ScheduleResult scheduleResult = ScheduleResult.empty(families);

        for (WeekType weekType : WeekType.values()) {
            for (WeekDay weekDay : WeekDay.values()) {
                for (TimeSlot timeSlot : TimeSlot.values()) {
                    if (!scheduleResult.hasChildrenToTransport(weekType, weekDay, timeSlot)) {
                        continue;
                    }

                    List<Family> potentialDriver = scheduleResult.driverOrderByCurrentTripMean().stream().toList();
                    for (Family driver : potentialDriver) {
                        if (!canAssignMoreTrips(scheduleResult.meanTripPerWeek(driver), driver, maxFamilyTrips)) {
                            continue;
                        }
                        List<Child> children = scheduleResult.childrenCandidates(weekType, weekDay, timeSlot, driver);
                        scheduleResult = scheduleResult.addTrip(weekType, weekDay, timeSlot, driver, children);
                        if (scheduleResult.isTripFull(weekType, weekDay, timeSlot)) {
                            break;
                        }
                    }


                }
            }
        }

        return scheduleResult;
   }

    private boolean canAssignMoreTrips(double currentMeanTripPerWeek, Family driver, Map<String, Double> maxFamilyTrips) {
        Double maxTrips = maxFamilyTrips.get(driver.name);
        if (maxTrips == null) {
            return true;
        }
        return currentMeanTripPerWeek + 0.5 <= maxTrips + 1.0e-9;
    }

}
