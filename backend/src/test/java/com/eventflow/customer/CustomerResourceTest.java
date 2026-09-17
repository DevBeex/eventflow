package com.eventflow.customer;

import com.eventflow.testsupport.TestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class CustomerResourceTest {

    @Test
    void createCustomerNormalizesEmail() {
        String suffix = TestFixtures.uniqueSuffix();
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", " John Doe ", "email", " John@Example.com "))
                .post("/api/customers")
                .then()
                .statusCode(201)
                .body("email", equalTo("john@example.com"))
                .body("name", equalTo("John Doe"));
    }

    @Test
    void duplicateEmailReturns409() {
        String suffix = TestFixtures.uniqueSuffix();
        String email = "dup+" + suffix + "@example.com";
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "First", "email", email))
                .post("/api/customers")
                .then()
                .statusCode(201);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Second", "email", email.toUpperCase()))
                .post("/api/customers")
                .then()
                .statusCode(409)
                .body("error", equalTo("DUPLICATE_CUSTOMER_EMAIL"));
    }

    @Test
    void invalidCustomerReturns400() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "", "email", "not-an-email"))
                .post("/api/customers")
                .then()
                .statusCode(400)
                .body("error", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void listCustomersSupportsPagination() {
        given()
                .get("/api/customers?page=0&size=5")
                .then()
                .statusCode(200)
                .body("page", equalTo(0))
                .body("size", equalTo(5))
                .body("items.size()", org.hamcrest.Matchers.lessThanOrEqualTo(5));
    }

    @Test
    void invalidPaginationReturns400() {
        given()
                .get("/api/customers?page=-1")
                .then()
                .statusCode(400)
                .body("error", equalTo("VALIDATION_ERROR"));
    }
}
