package com.carpool.family;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

@Entity
public class Family extends PanacheEntity {
    public String name;
    public int carCapacity;
}
