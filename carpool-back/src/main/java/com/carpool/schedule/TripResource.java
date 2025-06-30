package com.carpool.schedule;

import com.carpool.schedule.persistence.Trip;
import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;

public interface TripResource extends PanacheEntityResource<Trip, Long> {
}
