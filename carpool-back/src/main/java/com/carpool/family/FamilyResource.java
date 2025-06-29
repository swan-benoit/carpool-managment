package com.carpool.family;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Path("/family")
public class FamilyResource {

    @GET
    public List<Family> getFamilies() {
        return Family.familiesWithChildren();
    };

    @GET
    @Path("{id}")
    public Family getFamily(@PathParam("id") Long id) {
        return Family.findById(id);
    }

    @POST
    @Transactional
    @ResponseStatus(201)
    public Family createFamily(Family family) {
        return Family.createFamilyWithChildren(family);
    }

    @PUT
    @Path("{id}")
    @Transactional
    @ResponseStatus(201)
    public Family updateFamily(@PathParam("id") Long id, @RequestBody Family updatedFamily) {
        Family family = Family.familyWithChildren(id);
        if (family == null) {
            throw new WebApplicationException(404);
        }

        return Family.updateFamilyWithChildren(id, updatedFamily);

    }


}
