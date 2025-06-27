package com.carpool.schedule.calculator;

import com.carpool.family.*;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ScheduleService {

    public ScheduleResult generateSchedule(List<Family> families) {
       ScheduleResult scheduleResult = ScheduleResult.empty(families);

        for (WeekType weekType : WeekType.values()) {
            for (WeekDay weekDay : WeekDay.values()) {
                for (TimeSlot timeSlot : TimeSlot.values()) {
                    List<Family> potentialDriver = scheduleResult.driverOrderByCurrentTripMean();
                    for (Family driver : potentialDriver) {
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

}
