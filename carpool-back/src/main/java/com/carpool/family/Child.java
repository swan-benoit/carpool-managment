package com.carpool.family;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

@Entity
public class Child extends PanacheEntity {
    public String name;
    @ManyToOne
    public Family family;
}
