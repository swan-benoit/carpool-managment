package com.carpool.family;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

@Entity
public class Requirement extends PanacheEntity {
    public TimeSlot timeSlot;
    public WeekDay weekDay;
    public WeekType weekType;
    @ManyToOne
    public Family family;
}
