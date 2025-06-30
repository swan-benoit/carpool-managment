package com.carpool.schedule.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
public class FullSchedule extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String name;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    public Schedule evenSchedule;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    public Schedule oddSchedule;
}
