package com.carpool.schedule;

import com.carpool.family.Child;
import com.carpool.family.Family;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import java.util.List;

@Entity
public class Car extends PanacheEntity {
    @OneToMany
    public List<Child> children;

    @OneToOne
    public Family family;
}
