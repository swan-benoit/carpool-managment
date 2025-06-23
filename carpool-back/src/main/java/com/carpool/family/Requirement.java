package com.carpool.family;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;

@Entity
public class Requirement extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public TimeSlot timeSlot;
    public WeekDay weekDay;
    public WeekType weekType;

    @ManyToOne
    @JsonbTransient
    public Family family;
}
