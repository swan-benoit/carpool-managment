package com.carpool.schedule;

import com.carpool.family.WeekType;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.List;

@Entity
public class Schedule extends PanacheEntity {

    public WeekType weekType;
    @OneToMany
    public List<Trip> trips;
}
