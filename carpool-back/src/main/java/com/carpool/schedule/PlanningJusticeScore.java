package com.carpool.schedule;

import java.util.List;

public record PlanningJusticeScore(
        boolean perfectJustice,
        double averageJusticeScore,
        double minimumJusticeScore,
        List<FamilyJusticeScore> families
) {
}
