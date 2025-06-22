package com.carpool.schedule;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("schedule")
public class ScheduleResource {
    
    @Inject
    ScheduleService scheduleService;
    @GET
    public FullSchedule generateFullSchedule() {
        return scheduleService.generateFullSchedule();
    }
    
}
