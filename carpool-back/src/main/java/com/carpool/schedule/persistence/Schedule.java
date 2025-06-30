package com.carpool.schedule.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.util.Set;

@Entity
public class Schedule extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;


    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<Trip> trips;
}
