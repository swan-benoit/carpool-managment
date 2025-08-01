package com.carpool.family;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Child extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String name;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<AbsenceDays> absenceDays = new HashSet<>();

}
