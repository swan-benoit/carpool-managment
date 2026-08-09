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

    /**
     * Identifiant de foyer co-parent. Les familles-driver partageant la même valeur non vide
     * forment un seul foyer (parents séparés d'un même enfant). {@code null} ou vide = foyer solo.
     */
    public String householdId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    public List<Child> children ;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<Requirement> requirements;

}
