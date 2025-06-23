package com.carpool.family;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;

import java.util.List;

@Entity
public class Family extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String name;

    public int carCapacity;

    @OneToMany(mappedBy = "family", cascade = CascadeType.PERSIST)
    public List<Child> children;

    static List<Family> familiesWithChildren() {
       return list("SELECT DISTINCT f FROM Family f LEFT JOIN FETCH f.children order by f.name");
    };

}
