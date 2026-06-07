package com.carpool;

import com.carpool.family.FamilyResource;
import com.carpool.family.TimeSlot;
import com.carpool.family.WeekDay;
import com.carpool.family.WeekType;
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
import static org.hamcrest.Matchers.containsInAnyOrder;

@QuarkusTest
@Tag("family")
class FamilyResourceTest {

    private Response getFamilies() {
        return given()
                .when().get("/family?sort=name");
    }

    @Test
    @DisplayName("get families")
    void test_get_families() {
        getFamilies()
                .then()
                .statusCode(200)
                .body(
                        "find { it.name == 'Abdou et Lydia' }.carCapacity", equalTo(4),
                        "find { it.name == 'Abdou et Lydia' }.children.name", containsInAnyOrder("Issa", "Hedi"),
                        "find { it.name == 'Anne et Swan' }.children.name", containsInAnyOrder("Luce"),
                        "find { it.name == 'Axel et Soizic' }.children.name", containsInAnyOrder("Come", "Rose"),
                        "find { it.name == 'Céline' }.children.name", containsInAnyOrder("Anna"),
                        "find { it.name == 'Céline' }.children[0].absenceDays.size()", equalTo(3),
                        "find { it.name == 'Romain et Virginie' }.children.name", containsInAnyOrder("Mael"),
                        "find { it.name == 'Sonia et Jean philippe' }.children.name", containsInAnyOrder("Mélanie")
                );
    }

    @TestTransaction
    @Test
    @DisplayName("create family")
    void test_create_family() {
        long id = given().contentType(ContentType.JSON)
                .body("""
                        {
                          "name": "Jérome et Sonia",
                          "carCapacity": 6,
                          "children": [
                            {
                              "name": "Max",
                              "absenceDays": [
                              {
                                   "timeSlot": "MORNING",
                                    "weekDay": "MONDAY",
                                    "weekType": "EVEN"
                              },
                                                                                    {
                                   "timeSlot": "EVENING",
                                    "weekDay": "FRIDAY",
                                    "weekType": "EVEN"
                              }
                              ]
                            }
                          ],
                          "requirements": [
                            {
                              "timeSlot": "MORNING",
                              "weekDay": "MONDAY",
                              "weekType": "EVEN"
                            }
                          ]
                        }
                        """)
                .when().post("/family")
                .then()
                .statusCode(201)
                .body(
                        "carCapacity", equalTo(6),
                        "name", equalTo("Jérome et Sonia"),
                        "children.name[0]", equalTo("Max"),
                        "children.absenceDays[0].weekType", containsInAnyOrder(WeekType.EVEN.toString(),WeekType.EVEN.toString()),
                        "children.absenceDays[0].weekDay", containsInAnyOrder(WeekDay.MONDAY.toString(), WeekDay.FRIDAY.toString()),
                        "children.absenceDays[0].timeSlot", containsInAnyOrder(TimeSlot.MORNING.toString(), TimeSlot.EVENING.toString()),
                        "requirements.timeSlot[0]", equalTo(TimeSlot.MORNING.toString()),
                        "requirements.weekDay[0]", equalTo(WeekDay.MONDAY.toString()),
                        "requirements.weekType[0]", equalTo(WeekType.EVEN.toString())
                ).extract()
                .jsonPath()
                .getInt("id");

        String famillyPrettyPrint = getFamillyPrettyPrint();
        getFamilies().then().statusCode(200)
                .body(
                        "find { it.id == %s }.carCapacity".formatted(id), equalTo(6),
                        "find { it.id == %s }.children.name[0]".formatted(id), equalTo("Max"),
                        "find { it.id == %s }.requirements.timeSlot[0]".formatted(id), equalTo(TimeSlot.MORNING.toString()),
                        "find { it.id == %s }.requirements.weekDay[0]".formatted(id), equalTo(WeekDay.MONDAY.toString()),
                        "find { it.id == %s }.requirements.weekType[0]".formatted(id), equalTo(WeekType.EVEN.toString()),
                        "find { it.id == %s }.children.absenceDays[0].weekType".formatted(id), containsInAnyOrder(WeekType.EVEN.toString(), WeekType.EVEN.toString()),
                        "find { it.id == %s }.children.absenceDays[0].weekDay".formatted(id), containsInAnyOrder(WeekDay.MONDAY.toString(), WeekDay.FRIDAY.toString()),
                        "find { it.id == %s }.children.absenceDays[0].timeSlot".formatted(id), containsInAnyOrder(TimeSlot.MORNING.toString(), TimeSlot.EVENING.toString())
                );
    }

    @Test
    @TestTransaction
    @DisplayName("update family")
    void test_update_family() {
        JsonPath jsonPath = getFamilies()
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();
        int familyId = jsonPath.getInt("find { it.name == 'Sonia et Jean philippe' }.id");
        int laetitiaId = jsonPath.getInt("find { it.name == 'Sonia et Jean philippe' }.children[0].id");

        given().contentType(ContentType.JSON)
                .body("""
                        {
                          "name": "Sonia et Jean philippe",
                          "carCapacity":9,
                          "children": [
                            {
                              "id": %s,
                              "name": "Laetitia",
                              "absenceDays": [
                                  {
                                       "timeSlot": "MORNING",
                                       "weekDay": "MONDAY",
                                       "weekType": "EVEN"
                                  },
                                                                                        {
                                       "timeSlot": "EVENING",
                                       "weekDay": "FRIDAY",
                                       "weekType": "EVEN"
                                  }
                              ]
                            }
                          ],
                          "requirements": [
                            {
                              "timeSlot": "EVENING",
                              "weekDay": "FRIDAY",
                              "weekType": "ODD"
                            }
                          ]
                        }
                        """.formatted(laetitiaId)).when()
                .put("/family/" + familyId)
                .then()
                .statusCode(204);

        getFamilies().then().statusCode(200)
                .body(
                        "find { it.id == %s }.carCapacity".formatted(familyId), equalTo(9),
                        "find { it.id == %s }.children.size()".formatted(familyId), equalTo(1),
                        "find { it.id == %s }.children.name[0]".formatted(familyId), equalTo("Laetitia"),
                        "find { it.id == %s }.requirements.timeSlot[0]".formatted(familyId), equalTo(TimeSlot.EVENING.toString()),
                        "find { it.id == %s }.requirements.weekDay[0]".formatted(familyId), equalTo(WeekDay.FRIDAY.toString()),
                        "find { it.id == %s }.requirements.weekType[0]".formatted(familyId), equalTo(WeekType.ODD.toString()),
                        "find { it.id == %s }.children.absenceDays[0].weekType".formatted(familyId), containsInAnyOrder(
                                WeekType.EVEN.toString(),WeekType.EVEN.toString()
                        ),
                        "find { it.id == %s }.children.absenceDays[0].weekDay".formatted(familyId), containsInAnyOrder(
                                WeekDay.FRIDAY.toString(), WeekDay.MONDAY.toString()
                        ),
                        "find { it.id == %s }.children.absenceDays[0].timeSlot".formatted(familyId), containsInAnyOrder(
                                TimeSlot.MORNING.toString(), TimeSlot.EVENING.toString()
                        )
                );

    }

    private String getFamillyPrettyPrint() {
        return getFamilies().andReturn().body().prettyPrint();
    }

}
