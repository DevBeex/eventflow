package com.eventflow.order;

import com.eventflow.testsupport.TestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
class OrderResourceTest {

    @Test
    void createOrderCalculatesTotal200() {
        String suffix = TestFixtures.uniqueSuffix();
        long customerId = TestFixtures.createCustomer(suffix);
        long productA = TestFixtures.createProduct("Keyboard-" + suffix, 75.00);
        long productB = TestFixtures.createProduct("Mouse-" + suffix, 50.00);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "customerId", customerId,
                        "items", List.of(
                                Map.of("productId", productA, "quantity", 2),
                                Map.of("productId", productB, "quantity", 1)
                        )
                ))
                .post("/api/orders")
                .then()
                .statusCode(201)
                .body("total", equalTo(200.00f))
                .body("status", equalTo("CREATED"))
                .body("items", hasSize(2))
                .header("Location", org.hamcrest.Matchers.containsString("/api/orders/"));
    }

    @Test
    void customerNotFoundReturns404() {
        long productId = TestFixtures.createProduct("Orphan-" + TestFixtures.uniqueSuffix(), 10.00);
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "customerId", 999999999L,
                        "items", List.of(Map.of("productId", productId, "quantity", 1))
                ))
                .post("/api/orders")
                .then()
                .statusCode(404)
                .body("error", equalTo("CUSTOMER_NOT_FOUND"));
    }

    @Test
    void inactiveProductReturns409() {
        String suffix = TestFixtures.uniqueSuffix();
        long customerId = TestFixtures.createCustomer(suffix);
        long inactiveProduct = TestFixtures.createProduct("Inactive-" + suffix, 10.00, false);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "customerId", customerId,
                        "items", List.of(Map.of("productId", inactiveProduct, "quantity", 1))
                ))
                .post("/api/orders")
                .then()
                .statusCode(409)
                .body("error", equalTo("PRODUCT_INACTIVE"));
    }

    @Test
    void duplicateProductIdsReturn400() {
        String suffix = TestFixtures.uniqueSuffix();
        long customerId = TestFixtures.createCustomer(suffix);
        long productId = TestFixtures.createProduct("Dup-" + suffix, 10.00);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "customerId", customerId,
                        "items", List.of(
                                Map.of("productId", productId, "quantity", 1),
                                Map.of("productId", productId, "quantity", 2)
                        )
                ))
                .post("/api/orders")
                .then()
                .statusCode(400)
                .body("error", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void statusTransitionsAndNoOp() {
        String suffix = TestFixtures.uniqueSuffix();
        long customerId = TestFixtures.createCustomer(suffix);
        long productId = TestFixtures.createProduct("Status-" + suffix, 10.00);
        Number orderIdNumber = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "customerId", customerId,
                        "items", List.of(Map.of("productId", productId, "quantity", 1))
                ))
                .post("/api/orders")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
        long orderId = orderIdNumber.longValue();

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("status", "COMPLETED"))
                .patch("/api/orders/" + orderId + "/status")
                .then()
                .statusCode(200)
                .body("status", equalTo("COMPLETED"));

        String updatedAt = given()
                .get("/api/orders/" + orderId)
                .then()
                .statusCode(200)
                .extract()
                .path("updatedAt");

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("status", "CANCELLED"))
                .patch("/api/orders/" + orderId + "/status")
                .then()
                .statusCode(409)
                .body("error", equalTo("INVALID_ORDER_STATUS_TRANSITION"));

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("status", "COMPLETED"))
                .patch("/api/orders/" + orderId + "/status")
                .then()
                .statusCode(200)
                .body("updatedAt", equalTo(updatedAt));
    }

    @Test
    void listOrdersWithFilters() {
        given()
                .get("/api/orders?page=0&size=10&status=CREATED")
                .then()
                .statusCode(200)
                .body("page", equalTo(0));

        given()
                .get("/api/orders?status=INVALID")
                .then()
                .statusCode(400)
                .body("error", equalTo("VALIDATION_ERROR"));
    }
}
