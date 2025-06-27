package com.carpool.schedule.calculator;


import com.carpool.family.Child;
import com.carpool.family.Family;

import java.util.List;

public record Assignment(Family driverFamily, List<Child> children) {
    public Assignment {
        if (children.size() > driverFamily.carCapacity) {
            throw new IllegalArgumentException("Car capacity exceeded");
        }
    }

    @Override
    public String toString() {
        return "Assignment{" +
                "driverFamily=" + driverFamily +
                ", children=" + children +
                '}';
    }
}
