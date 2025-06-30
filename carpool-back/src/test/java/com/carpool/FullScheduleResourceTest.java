package com.carpool;

import com.carpool.family.TimeSlot;
import com.carpool.family.WeekDay;
import com.carpool.schedule.FullScheduleResource;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@Tag("fullschedule")
class FullScheduleResourceTest {

    private Response getSchedules() {
        return given()
                .when().get("/full-schedule?sort=name");
    }

    @Test
    @DisplayName("Vérifier que la liste des planning est vide")
    void test_get_empty_schedules() {
        getSchedules().then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    @TestTransaction
    @DisplayName("Create full schedule")
    void test_create_full_schedule() {
        Long id = given().contentType(ContentType.JSON)
                .body("""
                        {
                          "name": "Planning Semestre 1",
                          "evenSchedule": {
                            "trips": [
                              {
                                "weekDay": "MONDAY",
                                "timeSlot": "MORNING",
                                "driver": {
                                  "id": 1
                                },
                                "children": [
                                  { "id": 1 },
                                  { "id": 3 }
                                ]
                              },
                              {
                                "weekDay": "MONDAY",
                                "timeSlot": "EVENING",
                                "driver": {
                                  "id": 2
                                },
                                "children": [
                                  { "id": 1 },
                                  { "id": 3 }
                                ]
                              }
                            ]
                          },
                          "oddSchedule": {
                            "trips": [
                              {
                                "weekDay": "FRIDAY",
                                "timeSlot": "MORNING",
                                "driver": {
                                  "id": 3
                                },
                                "children": [
                                  { "id": 2 },
                                  { "id": 4 }
                                ]
                              }
                            ]
                          }
                        }
                        """)
                .when().post("/full-schedule")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Planning Semestre 1"))
                .body("evenSchedule", notNullValue())
                .body("evenSchedule.id", notNullValue())
                .body("evenSchedule.trips.size()", equalTo(2))
                .body("evenSchedule.trips[0].weekDay", equalTo("MONDAY"))
                .body("evenSchedule.trips[0].timeSlot", equalTo("MORNING"))
                .body("evenSchedule.trips[0].driver.id", equalTo(1))
                .body("evenSchedule.trips[0].children.size()", equalTo(2))
                .body("evenSchedule.trips[1].weekDay", equalTo("MONDAY"))
                .body("evenSchedule.trips[1].timeSlot", equalTo("EVENING"))
                .body("oddSchedule", notNullValue())
                .body("oddSchedule.id", notNullValue())
                .body("oddSchedule.trips.size()", equalTo(1))
                .body("oddSchedule.trips[0].weekDay", equalTo("FRIDAY"))
                .body("oddSchedule.trips[0].timeSlot", equalTo("MORNING"))
                .body("oddSchedule.trips[0].driver.id", equalTo(3))
                .body("oddSchedule.trips[0].children.size()", equalTo(2))
                .extract()
                .jsonPath()
                .getLong("id");

        given()
                .when().get("//full-schedule/" + id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id.intValue()))
                .body("name", equalTo("Planning Semestre 1"))
                .body("evenSchedule.trips.size()", equalTo(2))
                .body("oddSchedule.trips.size()", equalTo(1));

        // Vérifier que le planning apparaît dans la liste
        getSchedules().then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("id[0]", equalTo(id.intValue()))
                .body("name[0]", equalTo("Planning Semestre 1"));
    }

    @Test
    @TestTransaction
    @DisplayName("Mettre à jour un planning existant")
    void test_update_full_schedule() {
        Long id = given().contentType(ContentType.JSON)
                .body("""
                        {
                          "name": "Planning Initial",
                          "evenSchedule": {
                            "trips": [
                              {
                                "weekDay": "MONDAY",
                                "timeSlot": "MORNING",
                                "driver": {
                                  "id": 1
                                },
                                "children": [
                                  { "id": 1 }
                                ]
                              }
                            ]
                          },
                          "oddSchedule": {
                            "trips": []
                          }
                        }
                        """)
                .when().post("/full-schedule")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");

        // Récupérer les IDs du Schedule et du Trip créés
        JsonPath jsonPath = given()
                .when().get("/full-schedule/" + id)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();

        Long evenScheduleId = jsonPath.getLong("evenSchedule.id");
        Long tripId = jsonPath.getLong("evenSchedule.trips[0].id");

        // Mettre à jour le planning
        given().contentType(ContentType.JSON)
                .body("""
                        {
                          "id": %d,
                          "name": "Planning Modifié",
                          "evenSchedule": {
                            "id": %d,
                            "trips": [
                              {
                                "id": %d,
                                "weekDay": "MONDAY",
                                "timeSlot": "EVENING",
                                "driver": {
                                  "id": 2
                                },
                                "children": [
                                  { "id": 2 },
                                  { "id": 3 }
                                ]
                              },
                              {
                                "weekDay": "TUESDAY",
                                "timeSlot": "MORNING",
                                "driver": {
                                  "id": 3
                                },
                                "children": [
                                  { "id": 4 }
                                ]
                              }
                            ]
                          },
                          "oddSchedule": {
                            "trips": [
                              {
                                "weekDay": "WEDNESDAY",
                                "timeSlot": "MORNING",
                                "driver": {
                                  "id": 4
                                },
                                "children": [
                                  { "id": 5 }
                                ]
                              }
                            ]
                          }
                        }
                        """.formatted(id, evenScheduleId, tripId))
                .when().put("/full-schedule/" + id)
                .then()
                .statusCode(204);

        // Vérifier que la mise à jour est effective
        given()
                .when().get("/full-schedule/" + id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id.intValue()))
                .body("name", equalTo("Planning Modifié"))
                .body("evenSchedule.trips.size()", equalTo(2))
                .body("evenSchedule.trips[0].weekDay", equalTo("MONDAY"))
                .body("evenSchedule.trips[0].timeSlot", equalTo("EVENING"))
                .body("evenSchedule.trips[0].driver.id", equalTo(2))
                .body("evenSchedule.trips[0].children.size()", equalTo(2))
                .body("evenSchedule.trips[1].weekDay", equalTo("TUESDAY"))
                .body("evenSchedule.trips[1].timeSlot", equalTo("MORNING"))
                .body("oddSchedule.trips.size()", equalTo(1))
                .body("oddSchedule.trips[0].weekDay", equalTo("WEDNESDAY"))
                .body("oddSchedule.trips[0].timeSlot", equalTo("MORNING"))
                .body("oddSchedule.trips[0].driver.id", equalTo(4))
                .body("oddSchedule.trips[0].children.size()", equalTo(1));
    }

    @Test
    @TestTransaction
    @DisplayName("Supprimer un planning")
    void test_delete_full_schedule() {
        // Créer un planning
        Long id = given().contentType(ContentType.JSON)
                .body("""
                        {
                          "name": "Planning à supprimer",
                          "evenSchedule": {
                            "trips": []
                          },
                          "oddSchedule": {
                            "trips": []
                          }
                        }
                        """)
                .when().post("/full-schedule")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");

        // Vérifier qu'il existe
        getSchedules().then()
                .statusCode(200)
                .body("size()", equalTo(1));

        // Le supprimer
        given()
                .when().delete("/full-schedule/" + id)
                .then()
                .statusCode(204);

        // Vérifier qu'il n'existe plus
        getSchedules().then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    @TestTransaction
    @DisplayName("Créer un planning avec des Trip sans driver et sans enfants")
    void test_create_schedule_with_empty_trips() {
        Long id = given().contentType(ContentType.JSON)
                .body("""
                        {
                          "name": "Planning Vide",
                          "evenSchedule": {
                            "trips": [
                              {
                                "weekDay": "MONDAY",
                                "timeSlot": "MORNING"
                              }
                            ]
                          },
                          "oddSchedule": {
                            "trips": []
                          }
                        }
                        """)
                .when().post("/full-schedule")
                .then()
                .statusCode(201)
                .body("evenSchedule.trips[0].weekDay", equalTo("MONDAY"))
                .body("evenSchedule.trips[0].timeSlot", equalTo("MORNING"))
                .body("evenSchedule.trips[0].driver", nullValue())
                .body("evenSchedule.trips[0].children", empty())
                .extract()
                .jsonPath()
                .getLong("id");

        // Vérifier la persistance
        given()
                .when().get("/full-schedule/" + id)
                .then()
                .statusCode(200)
                .body("evenSchedule.trips[0].driver", nullValue())
                .body("evenSchedule.trips[0].children", empty());
    }

    @Test
    @TestTransaction
    @DisplayName("Vérifier une planification complète sur une semaine")
    void test_full_week_schedule() {
        Long id = given().contentType(ContentType.JSON)
                .body("""
                        {
                          "name": "Planning Semaine Complète",
                          "evenSchedule": {
                            "trips": [
                              {
                                "weekDay": "MONDAY",
                                "timeSlot": "MORNING",
                                "driver": { "id": 1 },
                                "children": [{ "id": 2 }, { "id": 3 }]
                              },
                              {
                                "weekDay": "MONDAY",
                                "timeSlot": "EVENING",
                                "driver": { "id": 2 },
                                "children": [{ "id": 1 }, { "id": 3 }]
                              },
                              {
                                "weekDay": "TUESDAY",
                                "timeSlot": "MORNING",
                                "driver": { "id": 3 },
                                "children": [{ "id": 1 }, { "id": 2 }]
                              },
                              {
                                "weekDay": "TUESDAY",
                                "timeSlot": "EVENING",
                                "driver": { "id": 1 },
                                "children": [{ "id": 2 }, { "id": 3 }]
                              },
                              {
                                "weekDay": "THURSDAY",
                                "timeSlot": "MORNING",
                                "driver": { "id": 2 },
                                "children": [{ "id": 1 }, { "id": 3 }]
                              },
                              {
                                "weekDay": "THURSDAY",
                                "timeSlot": "EVENING",
                                "driver": { "id": 3 },
                                "children": [{ "id": 1 }, { "id": 2 }]
                              },
                              {
                                "weekDay": "FRIDAY",
                                "timeSlot": "MORNING",
                                "driver": { "id": 1 },
                                "children": [{ "id": 2 }, { "id": 3 }]
                              },
                              {
                                "weekDay": "FRIDAY",
                                "timeSlot": "EVENING",
                                "driver": { "id": 2 },
                                "children": [{ "id": 1 }, { "id": 3 }]
                              }
                            ]
                          },
                          "oddSchedule": {
                            "trips": [
                              {
                                "weekDay": "MONDAY",
                                "timeSlot": "MORNING",
                                "driver": { "id": 3 },
                                "children": [{ "id": 1 }, { "id": 2 }]
                              },
                              {
                                "weekDay": "MONDAY",
                                "timeSlot": "EVENING",
                                "driver": { "id": 1 },
                                "children": [{ "id": 2 }, { "id": 3 }]
                              },
                              {
                                "weekDay": "TUESDAY",
                                "timeSlot": "MORNING",
                                "driver": { "id": 2 },
                                "children": [{ "id": 1 }, { "id": 3 }]
                              },
                              {
                                "weekDay": "TUESDAY",
                                "timeSlot": "EVENING",
                                "driver": { "id": 3 },
                                "children": [{ "id": 1 }, { "id": 2 }]
                              },
                              {
                                "weekDay": "THURSDAY",
                                "timeSlot": "MORNING",
                                "driver": { "id": 1 },
                                "children": [{ "id": 2 }, { "id": 3 }]
                              },
                              {
                                "weekDay": "THURSDAY",
                                "timeSlot": "EVENING",
                                "driver": { "id": 2 },
                                "children": [{ "id": 1 }, { "id": 3 }]
                              },
                              {
                                "weekDay": "FRIDAY",
                                "timeSlot": "MORNING",
                                "driver": { "id": 3 },
                                "children": [{ "id": 1 }, { "id": 2 }]
                              },
                              {
                                "weekDay": "FRIDAY",
                                "timeSlot": "EVENING",
                                "driver": { "id": 1 },
                                "children": [{ "id": 2 }, { "id": 3 }]
                              }
                            ]
                          }
                        }
                        """)
                .when().post("/full-schedule")
                .then()
                .statusCode(201)
                .body("evenSchedule.trips.size()", equalTo(8))
                .body("oddSchedule.trips.size()", equalTo(8))
                .extract()
                .jsonPath()
                .getLong("id");

        // Vérifier la persistance de tous les trajets
        JsonPath jsonPath = given()
                .when().get("/full-schedule/" + id)
                .then()
                .statusCode(200)
                .body("evenSchedule.trips.size()", equalTo(8))
                .body("oddSchedule.trips.size()", equalTo(8))
                .extract()
                .jsonPath();

        // Vérifier que tous les jours sont couverts dans la semaine paire
        assertWeekDaysAndTimeSlots(jsonPath, "evenSchedule");

        // Vérifier que tous les jours sont couverts dans la semaine impaire
        assertWeekDaysAndTimeSlots(jsonPath, "oddSchedule");
    }

    private void assertWeekDaysAndTimeSlots(JsonPath jsonPath, String scheduleType) {
        WeekDay[] weekDaysToCheck = {WeekDay.MONDAY, WeekDay.TUESDAY, WeekDay.THURSDAY, WeekDay.FRIDAY};
        TimeSlot[] timeSlotsToCheck = {TimeSlot.MORNING, TimeSlot.EVENING};

        for (WeekDay weekDay : weekDaysToCheck) {
            for (TimeSlot timeSlot : timeSlotsToCheck) {
                String path = String.format("%s.trips.find { it.weekDay == '%s' && it.timeSlot == '%s' }",
                                           scheduleType, weekDay, timeSlot);
                given().then()
                    .body(path, notNullValue());
            }
        }
    }
}