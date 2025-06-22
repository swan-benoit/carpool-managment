package com.carpool.schedule;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

public interface FullScheduleResource extends PanacheEntityResource<FullSchedule, Long> {

}

