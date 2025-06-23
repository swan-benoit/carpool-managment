package com.carpool.family;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;

import java.util.List;
import java.util.Set;

@Entity
public class Family extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String name;

    public int carCapacity;

    @OneToMany(mappedBy = "family", cascade = CascadeType.PERSIST)
    public List<Child> children;

    @OneToMany(mappedBy = "family", cascade = CascadeType.PERSIST)
    public Set<Requirement> requirements;

    static List<Family> familiesWithChildren() {
       return list("SELECT DISTINCT f FROM Family f LEFT JOIN FETCH f.children LEFT JOIN FETCH f.requirements order by f.name");
    };

}
