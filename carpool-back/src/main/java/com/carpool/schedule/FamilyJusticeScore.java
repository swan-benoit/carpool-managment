package com.carpool.schedule;

public record FamilyJusticeScore(
        String familyName,
        double actualMeanTripPerWeek,
        double perfectMeanTripPerWeek,
        double tripDeviation,
        boolean preferenceCompliance,
        boolean perfectJustice,
        double justiceScore
) {
}
