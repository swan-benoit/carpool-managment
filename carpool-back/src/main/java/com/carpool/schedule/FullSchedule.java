package com.carpool.schedule;

import com.carpool.schedule.Schedule;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

@Entity
public class FullSchedule extends PanacheEntity {
    @OneToOne
    public Schedule oddSchedule;

    @OneToOne
    public Schedule evenSchedule;

    @Override
    public void persist() {
        super.persist();
    }
}
