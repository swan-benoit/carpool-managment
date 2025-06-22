package com.carpool.schedule;

import com.carpool.family.*;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ScheduleService {
    public FullSchedule generateFullSchedule() {
        List<Family> familyWithChildren = Family.listAll();

        for (WeekType weekType : WeekType.values()) {
            for (WeekDay weekDay : WeekDay.values()) {
                for (TimeSlot timeSlot : TimeSlot.values()) {

                }
            }
        }

        return new FullSchedule();
    }
}

