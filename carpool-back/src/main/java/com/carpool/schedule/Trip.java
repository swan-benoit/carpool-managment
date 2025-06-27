package com.carpool.schedule;

import com.carpool.family.*;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.List;
import java.util.stream.Stream;

@Entity
public class Trip extends PanacheEntity {
    public WeekDay weekDay;
    public TimeSlot timeSlot;
    public WeekType weekType;
    @OneToMany
    public List<Car> cars = List.of();


    public boolean complete(List<Family> families) {
        return families.stream()
                .flatMap(family -> family.children.stream())
                .allMatch(child -> cars.stream()
                        .anyMatch(car -> car.children.contains(child)));
    }

    public Trip withCars(Family driver, List<Child> children) {
        Car car = new Car();
        car.family = driver;
        car.children = children;

        Trip trip = new Trip();
        trip.weekDay = weekDay;
        trip.timeSlot = timeSlot;
        trip.weekType = weekType;
        trip.cars = Stream.concat(
                filterMovedAssignement(car),
                Stream.of(car)
        ).toList();

        return trip;
    }

    private Stream<Car> filterMovedAssignement(Car newCar) {
        return cars.stream()
                .map(oldCar -> {
                            Car car = new Car();
                            car.family = oldCar.family;
                            car.children = removeAssignementChildren(newCar, oldCar);
                            return car;
                        }
                );
    }

    private List<Child> removeAssignementChildren(Car newCar, Car oldCar) {
        return oldCar.children.stream()
                .filter(child -> !newCar.children.contains(child))
                .toList();
    }
}
