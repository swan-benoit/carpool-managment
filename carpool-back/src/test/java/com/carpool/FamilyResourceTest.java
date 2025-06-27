package com.carpool;

import com.carpool.family.FamilyResource;
import com.carpool.family.TimeSlot;
import com.carpool.family.WeekDay;
import com.carpool.family.WeekType;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
@TestHTTPEndpoint(FamilyResource.class)
class FamilyResourceTest {

    private Response getFamilies() {
        return given()
                .when().get();
    }

    @Test
    void test_get_families() {
        getFamilies()
                .then()
                .statusCode(200)
                .body(
                        "carCapacity[0]", equalTo(4),
                        "name[0]", equalTo("Abdou et Lydia"),
                        "children[0].name[0]", equalTo("Issa"),
                        "children[0].name[1]", equalTo("Hedi"),

                        "carCapacity[1]", equalTo(4),
                        "name[1]", equalTo("Anne et Swan"),
                        "children[1].name[0]", equalTo("Luce"),

                        "carCapacity[2]", equalTo(4),
                        "name[2]", equalTo("Axel et Soizic"),
                        "children[2].name[0]", equalTo("Come"),
                        "children[2].name[1]", equalTo("Rose"),

                        "carCapacity[3]", equalTo(7),
                        "name[3]", equalTo("Romain et Virginie"),
                        "children[3].name[0]", equalTo("Mael"),

                        "carCapacity[4]", equalTo(4),
                        "name[4]", equalTo("Sonia et Jean philippe"),
                        "children[4].name[0]", equalTo("Laetitia"),
                        "children[4].name[1]", equalTo("Mélanie")
                );
    }

    @TestTransaction
    @Test
    void test_create_family() {
        given().contentType(ContentType.JSON)
                .body("""
                        {
                          "name": "Jérome et Sonia",
                          "carCapacity": 6,
                          "children": [
                            {
                              "name": "Max"
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
                .when().post()
                .then()
                .statusCode(201)
                .body(
                        "carCapacity", equalTo(6),
                        "name", equalTo("Jérome et Sonia"),
                        "children.name[0]", equalTo("Max"),
                        "requirements.timeSlot[0]", equalTo(TimeSlot.MORNING.toString()),
                        "requirements.weekDay[0]", equalTo(WeekDay.MONDAY.toString()),
                        "requirements.weekType[0]", equalTo(WeekType.EVEN.toString())
                );

        getFamilies().then().statusCode(200)
                .body(
                        "find { it.name == 'Jérome et Sonia' }.carCapacity", equalTo(6),
                        "find { it.name == 'Jérome et Sonia' }.children.name[0]", equalTo("Max"),
                        "find { it.name == 'Jérome et Sonia' }.requirements.timeSlot[0]", equalTo(TimeSlot.MORNING.toString()),
                        "find { it.name == 'Jérome et Sonia' }.requirements.weekDay[0]", equalTo(WeekDay.MONDAY.toString()),
                        "find { it.name == 'Jérome et Sonia' }.requirements.weekType[0]", equalTo(WeekType.EVEN.toString())
                );
    }

    @Test
    @TestTransaction
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
                              "name": "Laetitia"
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
                .put("/" + familyId)
                .then()
                .statusCode(201);

        String string = getFamilies().andReturn().asPrettyString();

        getFamilies().then().statusCode(200)
                .body(
                        "find { it.name == 'Sonia et Jean philippe' }.carCapacity", equalTo(9),
                        "find { it.name == 'Sonia et Jean philippe' }.children.size()", equalTo(1),
                        "find { it.name == 'Sonia et Jean philippe' }.children.name[0]", equalTo("Laetitia"),
                        "find { it.name == 'Sonia et Jean philippe' }.requirements.timeSlot[0]", equalTo(TimeSlot.EVENING.toString()),
                        "find { it.name == 'Sonia et Jean philippe' }.requirements.weekDay[0]", equalTo(WeekDay.FRIDAY.toString()),
                        "find { it.name == 'Sonia et Jean philippe' }.requirements.weekType[0]", equalTo(WeekType.ODD.toString())
                );


    }

}