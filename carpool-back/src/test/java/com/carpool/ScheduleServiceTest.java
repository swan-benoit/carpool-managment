package com.carpool;

import com.carpool.family.Child;
import com.carpool.family.Family;
import com.carpool.schedule.ScheduleService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@Tag("Schedule")
class ScheduleServiceTest {
    @Inject
    ScheduleService scheduleService;


    @Test
    void generate_schedule_for_1_family_with_1_child() {
        var schedule = scheduleService.generateSchedule(List.of(mael_family()));

        assertThat(schedule.isFull()).isTrue();
        assertThat(schedule.perfectMeanTripPerWeek(mael_family())).isEqualTo(8.0);
        assertThat(schedule.meanTripPerWeek(mael_family())).isEqualTo(8.0);
        assertThat(schedule.toString()).isEqualTo("""
                Even Week Schedule:
                
                -----------------------MONDAY
                 \s
                  MORNING
                   Mael (Mael)
                
                  EVENING
                   Mael (Mael)
                
                -----------------------TUESDAY
                 \s
                  MORNING
                   Mael (Mael)
                
                  EVENING
                   Mael (Mael)
                
                -----------------------THURSDAY
                 \s
                  MORNING
                   Mael (Mael)
                
                  EVENING
                   Mael (Mael)
                
                -----------------------FRIDAY
                 \s
                  MORNING
                   Mael (Mael)
                
                  EVENING
                   Mael (Mael)
                
                Odd Week Schedule:
                
                -----------------------MONDAY
                 \s
                  MORNING
                   Mael (Mael)
                
                  EVENING
                   Mael (Mael)
                
                -----------------------TUESDAY
                 \s
                  MORNING
                   Mael (Mael)
                
                  EVENING
                   Mael (Mael)
                
                -----------------------THURSDAY
                 \s
                  MORNING
                   Mael (Mael)
                
                  EVENING
                   Mael (Mael)
                
                -----------------------FRIDAY
                 \s
                  MORNING
                   Mael (Mael)
                
                  EVENING
                   Mael (Mael)
                
                """);
    }

    public static Family mael_family() {
        return createFamily(1L, List.of(mael()), 6);
    }

    private static Family createFamily(long id, List<Child> children, int carCapacity) {
        Family family = new Family();
        family.id = id;
        family.children = children;
        family.carCapacity = carCapacity;
        return family;
    }

    private static Child createChild(String name, long id) {
        Child child = new Child();
        child.id = id;
        child.name = name;
        return child;
    }

    public static Child mael() {
        return createChild("Mael", 7);
    }

    public static Family luce_family() {
        return createFamily(2L, List.of(luce()), 4);
    }

    public static Child luce() {
        return createChild("Luce", 10);
    }

    public static Family come_rose_family() {
        return createFamily(3L, List.of(
                rose(),
                come()
        ), 4);
    }

    public static Child come() {
        return createChild("Come", 9);
    }

    public static Child rose() {
        return createChild("Rose", 7);
    }

    public Family hedi_issa_family() {
        return createFamily(4L, List.of(
                hedi(),
                issa()
        ), 4);
    }

    public static Child issa() {
        return createChild("Issa", 8);
    }

    public static Child hedi() {
        return createChild("Hedi", 10);
    }


}