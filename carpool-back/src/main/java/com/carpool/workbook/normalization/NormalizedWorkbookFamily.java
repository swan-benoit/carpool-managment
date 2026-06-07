package com.carpool.workbook.normalization;

import com.carpool.family.Family;
import com.carpool.family.TimeSlot;
import com.carpool.family.WeekDay;
import com.carpool.family.WeekType;

import java.util.List;

public record NormalizedWorkbookFamily(
        Family family,
        List<FamilyPreference> preferences,
        FamilyNotes notes
) {
    public record FamilyPreference(WeekType weekType, WeekDay weekDay, TimeSlot timeSlot, String value) {
    }

    public record FamilyNotes(String guardArrangement, String meetingPoint, String whatsapp, String remarks) {
        public static FamilyNotes empty() {
            return new FamilyNotes("", "", "", "");
        }
    }
}
