package com.carpool.schedule;

import com.carpool.schedule.calculator.ScheduleService;
import com.carpool.schedule.persistence.FullSchedule;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import java.util.List;

@Path("schedule")
public class ScheduleStatResource {

    @Inject
    ScheduleStatService scheduleStatService;
    @GET
    @Path("/{id}/stats")
    public List<Stats> stats(@PathParam("id") long id) {
        return scheduleStatService.getStat(id);
    }
}
