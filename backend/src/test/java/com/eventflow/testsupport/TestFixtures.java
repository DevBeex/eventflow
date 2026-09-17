package com.eventflow.testsupport;

import io.restassured.http.ContentType;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static long createCustomer(String suffix) {
        Number id = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "John Doe " + suffix,
                        "email", "john+" + suffix + "@example.com"
                ))
                .post("/api/customers")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
        return id.longValue();
    }

    public static long createProduct(String name, double price, boolean active) {
        Number id = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", name,
                        "description", "Test product",
                        "price", price,
                        "active", active
                ))
                .post("/api/products")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
        return id.longValue();
    }

    public static long createProduct(String name, double price) {
        return createProduct(name, price, true);
    }

    public static String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
