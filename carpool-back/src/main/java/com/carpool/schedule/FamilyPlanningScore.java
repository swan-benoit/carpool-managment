package com.carpool.schedule;

public record FamilyPlanningScore(
        String familyName,
        int totalScore,
        int impossibleAssignments,
        int avoidAssignments,
        int preferredAssignments,
        int okAssignments
) {
}
