package com.carpool.schedule;

import java.util.Locale;

public enum PreferenceValue {
    IMPOSSIBLE(-100),
    EVITER(-10),
    OK(0),
    PREFERE(5);

    private final int weight;

    PreferenceValue(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }

    public static PreferenceValue fromWorkbookValue(String value) {
        if (value == null || value.isBlank()) {
            return OK;
        }
        return PreferenceValue.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
