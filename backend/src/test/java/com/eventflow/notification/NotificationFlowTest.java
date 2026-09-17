package com.eventflow.notification;

import com.eventflow.testsupport.TestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@QuarkusTest
class NotificationFlowTest {

    @Test
    void orderCreationEventuallyCreatesNotification() {
        String suffix = TestFixtures.uniqueSuffix();
        long customerId = TestFixtures.createCustomer(suffix);
        long productA = TestFixtures.createProduct("FlowA-" + suffix, 75.00);
        long productB = TestFixtures.createProduct("FlowB-" + suffix, 50.00);

        Number orderIdNumber = given()
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
                .extract()
                .path("id");
        long orderId = orderIdNumber.longValue();

        given()
                .get("/api/notifications?orderId=" + orderId)
                .then()
                .statusCode(200)
                .body("totalItems", equalTo(0));

        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> given()
                        .get("/api/notifications?orderId=" + orderId)
                        .then()
                        .statusCode(200)
                        .body("totalItems", greaterThanOrEqualTo(1))
                        .body("items[0].type", equalTo("ORDER_CREATED"))
                        .body("items[0].message", equalTo("Order " + orderId + " was created successfully")));
    }
}
