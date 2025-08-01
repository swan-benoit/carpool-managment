package com.carpool.family;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;

import java.util.List;

@Entity
public class AbsenceDays extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public WeekDay weekDay;
    public WeekType weekType;
}
