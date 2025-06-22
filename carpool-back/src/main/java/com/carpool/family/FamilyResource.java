package com.carpool.family;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;

public interface FamilyResource extends PanacheEntityResource<Family, Long> {
}
