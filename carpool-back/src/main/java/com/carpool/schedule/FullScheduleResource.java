package com.carpool.schedule;

import com.carpool.schedule.persistence.FullSchedule;
import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;

public interface FullScheduleResource extends PanacheEntityResource<FullSchedule, Long> {
}

