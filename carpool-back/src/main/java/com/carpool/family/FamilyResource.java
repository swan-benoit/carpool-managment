package com.carpool.family;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;

import java.util.List;
import java.util.Optional;

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
        
        for (Requirement requirement : family.requirements) {
            requirement.family = family;
            requirement.persist();
        }
        
        return family;
    }

    @PUT
    @Path("{id}")
    public Family updateFamily(@PathParam("id") Long id) {
        Family family = Family.findById(id);
        if (family == null) {
            throw new WebApplicationException(404);
        }

        family.persist();

        for (Child child : family.children) {
            child.family = family;
            child.persist();
        }
        
        for (Requirement requirement : family.requirements) {
            requirement.family = family;
            requirement.persist();
        }

        return family;
    }


}
