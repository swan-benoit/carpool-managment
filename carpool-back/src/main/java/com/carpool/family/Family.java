package com.carpool.family;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;

import java.util.List;

@Entity
public class Family extends PanacheEntity {
    public String name;
    public int carCapacity;
    @OneToMany
    public List<Child> children;
//
//    public static List<FamilyWithChildren> getFamiliesWithChilren() {
//        return Child.find( "select c.family, c from Child c"
//        ).project(FamilyWithChildren.class).list();
//    }
}
