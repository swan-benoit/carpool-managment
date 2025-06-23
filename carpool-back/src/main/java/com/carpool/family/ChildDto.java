package com.carpool.family;

import io.quarkus.hibernate.orm.panache.common.NestedProjectedClass;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ChildDto {
    public String name;
    public FamilyDto family;

    public ChildDto(String name, FamilyDto family) {
        this.name = name;
        this.family = family;
    }

    @NestedProjectedClass
    public static class FamilyDto {
        public String name;
        public int carCapacity;

        public FamilyDto(String name, int carCapacity) {
            this.name = name;
            this.carCapacity = carCapacity;
        }
    }
}
