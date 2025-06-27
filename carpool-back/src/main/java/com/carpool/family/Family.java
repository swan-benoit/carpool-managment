package com.carpool.family;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
public class Family extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String name;

    public int carCapacity;

    @OneToMany(mappedBy = "family", cascade = CascadeType.ALL)
    public List<Child> children;

    @OneToMany(mappedBy = "family", cascade = CascadeType.MERGE)
    public Set<Requirement> requirements = new HashSet<>();

    static List<Family> familiesWithChildren() {
       return list("SELECT DISTINCT f FROM Family f LEFT JOIN FETCH f.children LEFT JOIN FETCH f.requirements order by f.name");
    };

    static Family familyWithChildren(Long id) {
        List<Family> list = list("SELECT DISTINCT f FROM Family f LEFT JOIN FETCH f.children LEFT JOIN FETCH f.requirements WHERE f.id = ?1 order by f.name", id);
        return list.getFirst();
    };

    static Family updateFamilyWithChildren(Long id, Family updated) {
        updated.id = id;
        Family familyBefore = findById(id);
        for (var child : familyBefore.children) {
            if (updated.children.stream().noneMatch(child1 -> Objects.equals(child1.id, child.id))) {
                child.delete();
            }
        }
        for (Child child : updated.children) {
            child.family = updated;
        }
        for (var requirement : familyBefore.requirements) {
            if (updated.requirements.stream().noneMatch(child1 -> Objects.equals(child1.id, requirement.id))) {
                requirement.delete();
            }
        }
        for (Requirement requirement : updated.requirements) {
            requirement.family = updated;
        }
        getEntityManager().merge(updated);

        return updated;
    }

}
