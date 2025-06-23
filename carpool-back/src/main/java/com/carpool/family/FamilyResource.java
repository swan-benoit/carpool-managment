package com.carpool.family;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;

import java.util.List;

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
    public Family createFamily(Family family) {
        family.persist();
        for (Child child : family.children) {
            child.family = family;
            child.persist();
        }
        return family;
    }


}
