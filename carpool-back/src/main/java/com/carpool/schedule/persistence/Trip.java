package com.carpool.schedule.persistence;

import com.carpool.family.*;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;

import java.util.Set;

@Entity
public class Trip extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String place;
    public WeekDay weekDay;
    public TimeSlot timeSlot;

    @OneToOne
    public Family driver;

    @OneToMany
    public Set<Child> children;

}
