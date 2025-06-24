package com.carpool;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class FamilyResourceTest {
    @TestHTTPResource("/family")
    URL familyEndpoint;

    @Test
    void testHelloEndpoint() {
        given()
                .when().get(familyEndpoint)
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

}