package com.carpool.family;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
public class Family extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String name;

    public int carCapacity;

    @OneToMany(mappedBy = "family", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<Child> children = new CopyOnWriteArrayList<>();

    @OneToMany(mappedBy = "family", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<Requirement> requirements = new HashSet<>();

    static List<Family> familiesWithChildren() {
        Stream<Family> stream = stream("""
                                 SELECT DISTINCT f FROM Family f
                                 LEFT JOIN FETCH f.children c
                                 LEFT JOIN FETCH c.absenceDays
                                 LEFT JOIN FETCH f.requirements order by f.name
                """);
        return stream.peek(f -> f.children = f.children.stream().distinct().toList()).toList();

    }

    static Family familyWithChildren(Long id) {
        List<Family> list = list("""
                                SELECT DISTINCT f FROM Family f
                                LEFT JOIN FETCH f.children c
                                LEFT JOIN FETCH c.absenceDays
                                LEFT JOIN FETCH f.requirements WHERE f.id = ?1 order by f.name
         """, id);
        return list.getFirst();
    }

    static Family updateFamilyWithChildren(Long id, Family updated) {
        Family existingFamily = familyWithChildren(id);
        existingFamily.name = updated.name;
        existingFamily.carCapacity = updated.carCapacity;

        List<Child> children = existingFamily.children;
        children.clear();

        for (Child c : updated.children) {
            Child child = new Child();
            child.name = c.name;
            child.family = existingFamily;
            for (var a: c.absenceDays) {
                a.child = child;
                child.absenceDays.add(a);
            }
            child.absenceDays = c.absenceDays;
            children.add(child);
        }

        existingFamily.requirements.clear();
        updated.requirements.forEach(c -> {
            Requirement requirement = new Requirement();
            requirement.timeSlot = c.timeSlot;
            requirement.weekDay = c.weekDay;
            requirement.weekType = c.weekType;
            requirement.family = existingFamily;
            existingFamily.requirements.add(requirement);

        });
        return updated;
    }


}
