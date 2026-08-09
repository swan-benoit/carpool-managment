package com.carpool.schedule;

import com.carpool.family.Child;
import com.carpool.family.Family;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.carpool.schedule.FamilyPlanningStats.perfectMeanTripPerWeek;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FamilyPlanningStatsTest {

    private Family family(String name, String householdId, int carCapacity, String... childNames) {
        Family family = new Family();
        family.name = name;
        family.householdId = householdId;
        family.carCapacity = carCapacity;
        List<Child> children = new ArrayList<>();
        for (String childName : childNames) {
            Child child = new Child();
            child.name = childName;
            child.absenceDays = new HashSet<>();
            children.add(child);
        }
        family.children = children;
        return family;
    }

    @Test
    void repartit_la_cible_du_foyer_a_parts_egales_entre_co_parents() {
        Family josephine = family("Josephine", "H", 4, "Orion");
        Family mickael = family("Mickael", "H", 3);
        Family autre = family("Autre", null, 4, "Kid");
        List<Family> families = List.of(josephine, mickael, autre);

        double partJosephine = perfectMeanTripPerWeek(josephine, families);
        double partMickael = perfectMeanTripPerWeek(mickael, families);

        assertTrue(partMickael > 0, "le co-parent sans enfant rattache doit avoir une cible non nulle");
        assertEquals(partJosephine, partMickael, 1e-9, "les deux co-parents partagent la meme cible d'equite");
    }

    @Test
    void la_cible_du_foyer_est_independante_du_rattachement_de_l_enfant() {
        Family autre = family("Autre", null, 4, "Kid");

        Family joseAvecEnfant = family("Josephine", "H", 4, "Orion");
        Family mikaSansEnfant = family("Mickael", "H", 3);
        List<Family> rattacheJose = List.of(joseAvecEnfant, mikaSansEnfant, autre);
        double sommeA = perfectMeanTripPerWeek(joseAvecEnfant, rattacheJose)
                + perfectMeanTripPerWeek(mikaSansEnfant, rattacheJose);

        Family joseSansEnfant = family("Josephine", "H", 4);
        Family mikaAvecEnfant = family("Mickael", "H", 3, "Orion");
        List<Family> rattacheMika = List.of(joseSansEnfant, mikaAvecEnfant, autre);
        double sommeB = perfectMeanTripPerWeek(joseSansEnfant, rattacheMika)
                + perfectMeanTripPerWeek(mikaAvecEnfant, rattacheMika);

        assertEquals(sommeA, sommeB, 1e-9, "deplacer l'enfant entre co-parents ne change pas la cible du foyer");
    }

    @Test
    void famille_solo_inchangee_et_cible_foyer_egale_cible_solo() {
        Family autre = family("Autre", null, 4, "Kid");

        Family josephine = family("Josephine", "H", 4, "Orion");
        Family mickael = family("Mickael", "H", 3);
        List<Family> avecFoyer = List.of(josephine, mickael, autre);

        Family foyerSolo = family("Foyer", null, 4, "Orion");
        List<Family> collapse = List.of(foyerSolo, autre);

        // Une famille non liee a un foyer garde exactement sa cible historique.
        assertEquals(
                perfectMeanTripPerWeek(autre, collapse),
                perfectMeanTripPerWeek(autre, avecFoyer),
                1e-9,
                "aucune regression pour les familles solo");

        // La somme des parts co-parentes egale la cible d'un foyer solo equivalent.
        double sommeCoparents = perfectMeanTripPerWeek(josephine, avecFoyer)
                + perfectMeanTripPerWeek(mickael, avecFoyer);
        assertEquals(
                perfectMeanTripPerWeek(foyerSolo, collapse),
                sommeCoparents,
                1e-9,
                "la cible du foyer = somme des parts des co-parents");
    }
}
