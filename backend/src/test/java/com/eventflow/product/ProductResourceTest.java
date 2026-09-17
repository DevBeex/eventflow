package com.eventflow.product;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class ProductResourceTest {

    @Test
    void createProductWithValidPrice() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Keyboard", "price", 75.00))
                .post("/api/products")
                .then()
                .statusCode(201)
                .body("price", equalTo(75.00f))
                .body("active", equalTo(true))
                .body("currency", equalTo("USD"));
    }

    @Test
    void invalidPriceReturns400() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Bad", "price", 1.501))
                .post("/api/products")
                .then()
                .statusCode(400)
                .body("error", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void activeNullReturns400() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"Inactive\",\"price\":10.00,\"active\":null}")
                .post("/api/products")
                .then()
                .statusCode(400)
                .body("error", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void activeFalseIsStored() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Inactive", "price", 10.00, "active", false))
                .post("/api/products")
                .then()
                .statusCode(201)
                .body("active", equalTo(false));
    }

    @Test
    void getProductNotFoundReturns404() {
        given()
                .get("/api/products/999999999")
                .then()
                .statusCode(404)
                .body("error", equalTo("PRODUCT_NOT_FOUND"));
    }

    @Test
    void listProductsByActiveFilter() {
        given()
                .get("/api/products?active=false")
                .then()
                .statusCode(200);

        given()
                .get("/api/products?active=1")
                .then()
                .statusCode(400)
                .body("error", equalTo("VALIDATION_ERROR"));
    }
}
