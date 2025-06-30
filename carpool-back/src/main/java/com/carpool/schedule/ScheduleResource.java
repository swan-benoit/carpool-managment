package com.carpool.schedule;

import com.carpool.schedule.persistence.Schedule;
import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;

public interface ScheduleResource extends PanacheEntityResource<Schedule, Long> {
}
