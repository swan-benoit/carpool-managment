package com.carpool.schedule.calculator;

import com.carpool.family.Child;
import com.carpool.family.Family;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ScheduleServiceTest {

    @Test
    void generate_schedule_for_1_family_with_1_child() {
        var scheduleService = new ScheduleService();
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

    @Test
    void generate_schedule_for_2_family_with_2_child() {
        var scheduleService = new ScheduleService();
        var schedule = scheduleService.generateSchedule(List.of(
                mael_family(),
                luce_family()
        ));

        assertThat(schedule.isFull()).isTrue();
        assertThat(schedule.perfectMeanTripPerWeek(mael_family())).isEqualTo(4.0);
        assertThat(schedule.meanTripPerWeek(mael_family())).isEqualTo(4.0);
        assertThat(schedule.meanTripPerWeek(luce_family())).isEqualTo(4.0);

    }

    @Test
    void generate_schedule_for_3_family_with_2_child() {
        var scheduleService = new ScheduleService();
        var schedule = scheduleService.generateSchedule(List.of(
                mael_family(),
                luce_family(),
                come_rose_family()
        ));

        assertThat(schedule.perfectMeanTripPerWeek(mael_family())).isEqualTo(2.0);
        assertThat(schedule.perfectMeanTripPerWeek(luce_family())).isEqualTo(2.0);
        assertThat(schedule.perfectMeanTripPerWeek(come_rose_family())).isEqualTo(4.0);

        assertThat(schedule.isFull()).isTrue();

        Double maelMean = schedule.meanTripPerWeek(mael_family());
        Double comeMean = schedule.meanTripPerWeek(come_rose_family());
        Double luceMean = schedule.meanTripPerWeek(luce_family());

        assertThat(maelMean).isEqualTo(2.0);
        assertThat(luceMean).isEqualTo(2.0);
        assertThat(comeMean).isEqualTo(4.0);

    }

    @Test
    public void generate_schedule_for_4_family_with_2_child() {
        var scheduleService = new ScheduleService();
        var schedule = scheduleService.generateSchedule(List.of(
                mael_family(),
                luce_family(),
                come_rose_family(),
                hedi_issa_family()
        ));

        assertThat(schedule.perfectMeanTripPerWeek(mael_family())).isEqualTo(2.6666666666666665);
        assertThat(schedule.perfectMeanTripPerWeek(luce_family())).isEqualTo(2.6666666666666665);
        assertThat(schedule.perfectMeanTripPerWeek(come_rose_family())).isEqualTo(5.333333333333333);
        assertThat(schedule.perfectMeanTripPerWeek(hedi_issa_family())).isEqualTo(5.333333333333333);

        assertThat(schedule.isFull()).isTrue();

        Double maelMean = schedule.meanTripPerWeek(mael_family());
        Double comeMean = schedule.meanTripPerWeek(come_rose_family());
        Double luceMean = schedule.meanTripPerWeek(luce_family());
        Double hediMean = schedule.meanTripPerWeek(hedi_issa_family());

        assertThat(schedule.toString()).isEqualTo("""
                Even Week Schedule:
                
                -----------------------MONDAY
                 \s
                  MORNING
                   Mael (Mael,Luce,Rose,Come,Hedi,Issa)
                
                  EVENING
                   Luce (Luce,Mael)<--> Rose, Come (Rose,Come,Hedi,Issa)
                
                -----------------------TUESDAY
                 \s
                  MORNING
                   Hedi, Issa (Hedi,Issa,Mael,Luce)<--> Rose, Come (Rose,Come)
                
                  EVENING
                   Hedi, Issa (Hedi,Issa,Luce)<--> Mael (Mael,Rose,Come)
                
                -----------------------THURSDAY
                 \s
                  MORNING
                   Luce (Luce,Mael)<--> Rose, Come (Rose,Come,Hedi,Issa)
                
                  EVENING
                   Hedi, Issa (Hedi,Issa,Mael,Luce)<--> Rose, Come (Rose,Come)
                
                -----------------------FRIDAY
                 \s
                  MORNING
                   Hedi, Issa (Hedi,Issa,Luce)<--> Mael (Mael,Rose,Come)
                
                  EVENING
                   Luce (Luce,Mael)<--> Rose, Come (Rose,Come,Hedi,Issa)
                
                Odd Week Schedule:
                
                -----------------------MONDAY
                 \s
                  MORNING
                   Hedi, Issa (Hedi,Issa,Mael,Luce)<--> Rose, Come (Rose,Come)
                
                  EVENING
                   Hedi, Issa (Hedi,Issa,Luce)<--> Mael (Mael,Rose,Come)
                
                -----------------------TUESDAY
                 \s
                  MORNING
                   Luce (Luce,Mael)<--> Rose, Come (Rose,Come,Hedi,Issa)
                
                  EVENING
                   Hedi, Issa (Hedi,Issa,Mael,Luce)<--> Rose, Come (Rose,Come)
                
                -----------------------THURSDAY
                 \s
                  MORNING
                   Hedi, Issa (Hedi,Issa,Luce)<--> Mael (Mael,Rose,Come)
                
                  EVENING
                   Luce (Luce,Mael)<--> Rose, Come (Rose,Come,Hedi,Issa)
                
                -----------------------FRIDAY
                 \s
                  MORNING
                   Hedi, Issa (Hedi,Issa,Mael,Luce)<--> Rose, Come (Rose,Come)
                
                  EVENING
                   Hedi, Issa (Hedi,Issa,Luce)<--> Mael (Mael,Rose,Come)
                
                """);

        assertThat(maelMean).isEqualTo(3.0);
        assertThat(luceMean).isEqualTo(2.5);
        assertThat(comeMean).isEqualTo(5.0);
        assertThat(hediMean).isEqualTo(5.0);
    }

    public static Family mael_family() {
        return createFamily(1L, List.of(mael()), 6);
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

    public static Child Anna() {
        return createChild("Anna", 11);
    }

    public static Family anna_family() {
        return createFamily(5L, List.of(Anna()), 4);
    }

    public static Family come_rose_family() {
        return createFamily(3L, List.of(
                rose(),
                come()
        ), 4);
    }

    private static Family createFamily(long id, List<Child> children, int carCapacity) {
        Family family = new Family();
        family.id = id;
        family.name = children.stream().map(c -> c.name).collect(Collectors.joining(", "));
        family.children = children;
        family.carCapacity = carCapacity;
        return family;
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

    private static Child createChild(String name, int id) {
        Child child = new Child();
        child.name = name;
        child.id = Long.valueOf(id);
        return child;
    }


}