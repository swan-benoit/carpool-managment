package com.carpool.schedule;

import java.util.List;

public record PlanningScore(
        int totalScore,
        int impossibleAssignments,
        int avoidAssignments,
        int preferredAssignments,
        int okAssignments,
        boolean complete,
        int totalRequiredTransportSlots,
        int assignedRequiredTransportSlots,
        int missingRequiredTransportSlots,
        double completionRatio,
        PlanningJusticeScore justice,
        List<FamilyPlanningScore> families
) {
}
