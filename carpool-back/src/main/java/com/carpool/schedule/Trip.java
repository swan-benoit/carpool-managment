package com.carpool.schedule;

import com.carpool.family.TimeSlot;
import com.carpool.family.WeekDay;
import com.carpool.family.WeekType;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.sql.Time;
import java.util.List;

@Entity
public class Trip extends PanacheEntity {
    public WeekDay weekDay;
    public TimeSlot timeSlot;
    public WeekType weekType;
    @OneToMany
    public List<Car> cars;
}
