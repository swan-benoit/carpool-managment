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
    @Transactional
    @ResponseStatus(201)
    public Family updateFamily(@PathParam("id") Long id, @RequestBody Family updatedFamily) {
//        Family family = Family.findById(id);
//        if (family == null) {
//            throw new WebApplicationException(404);
//        }
//        for (var child : family.children) {
//            if (updatedFamily.children.stream().noneMatch(child1 -> Objects.equals(child1.id, child.id))) {
//                child.delete();
//            }
//        }
//
//        family.name = updatedFamily.name;
//        family.carCapacity = updatedFamily.carCapacity;
//        family.children = updatedFamily.children.stream().map(child -> {
//            child.family = family;
//            return child;
//        }).toList();
//
//        family.persist();
        Family family = Family.findById(id);
        if (family == null) {
            throw new WebApplicationException(404);
        }

        // Supprimer les enfants absents de la nouvelle liste
        for (var child : List.copyOf(family.children)) {
            if (updatedFamily.children.stream().noneMatch(child1 -> Objects.equals(child1.id, child.id))) {
                child.delete();
            }
        }

        // Mettre à jour ou ajouter les enfants
        family.children.clear();
        for (Child updatedChild : updatedFamily.children) {
            updatedChild.family = family;
            if (updatedChild.id == null) {
                updatedChild.persist();
            }
            family.children.add(updatedChild);
        }

        family.name = updatedFamily.name;
        family.carCapacity = updatedFamily.carCapacity;

        family.persist();
        return family;

//        List<Child> all = Child.listAll();
//        family.requirements = updatedFamily.requirements;
//        family.children.removeIf(child -> {
//            boolean toRemove = updatedFamily.children.stream().noneMatch(c -> c.name.equals(child.name));
//            if (!updatedFamily.children.contains(child)) {
//                child.delete();
//            }
//            return toRemove;
//        });
//        for (Child child : family.children) {
//            if (!updatedFamily.children.stream().anyMatch(child1 -> child1.name.equals(child.name))) {
//               child.delete();
//               Child.deleteById(child.id);
//            }
//        }

//        panacheEntityBases = Child.listAll();

//        for (Child child : updatedFamily.children) {
//            child.family = family;
//            child.persist();
//        }
        
//        for (Requirement requirement : family.requirements) {
//            requirement.family = family;
//            requirement.persist();
//        }

//        return updatedFamily;
    }


}
