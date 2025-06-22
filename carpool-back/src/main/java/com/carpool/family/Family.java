package com.carpool.family;

import com.carpool.Child;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.List;

@Entity
public class Family extends PanacheEntity {
    public String name;
    public int carCapacity;
    @OneToMany
    public List<Child> children;
}
