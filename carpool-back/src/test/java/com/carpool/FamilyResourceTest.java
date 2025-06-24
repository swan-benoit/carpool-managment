package com.carpool;

import com.carpool.family.TimeSlot;
import com.carpool.family.WeekDay;
import com.carpool.family.WeekType;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class FamilyResourceTest {
    @TestHTTPResource("/family")
    URL familyEndpoint;

    private Response getFamilies() {
        return given()
                .when().get(familyEndpoint);
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
                .when().post(familyEndpoint)
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
                        "find { it.name == 'Jérome et Sonia' }.children.name[0]", equalTo("Max")
                );

    }

}