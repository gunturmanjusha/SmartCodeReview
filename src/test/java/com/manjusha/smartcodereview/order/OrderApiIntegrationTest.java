package com.manjusha.smartcodereview.order;

import com.manjusha.smartcodereview.order.repository.OrderRepository;
import com.manjusha.smartcodereview.config.CorrelationIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithAnonymousUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.hamcrest.Matchers.not;

@SpringBootTest
@AutoConfigureMockMvc
class OrderApiIntegrationTest {

    private static final String VALID_ORDER = """
            {
              "customerName": "Grace Hopper",
              "productName": "Mechanical Keyboard",
              "quantity": 2,
              "unitPrice": 75.50,
              "status": "PENDING"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void clearDatabase() {
        orderRepository.deleteAll();
    }

    @Test
    void supportsCreateRetrieveUpdateAndDelete() throws Exception {
        var result = mockMvc.perform(post("/api/orders")
                        .with(httpBasic("reviewer", "review-demo-only"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.totalPrice").value(151.0))
                .andReturn();

        var location = result.getResponse().getHeader("Location");
        var initialEtag = result.getResponse().getHeader("ETag");

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", initialEtag))
                .andExpect(jsonPath("$.customerName").value("Grace Hopper"));

        var update = VALID_ORDER.replace("PENDING", "SHIPPED").replace("quantity\": 2", "quantity\": 3");
        var updateResult = mockMvc.perform(put(location).with(httpBasic("reviewer", "review-demo-only"))
                        .header("If-Match", initialEtag)
                        .contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"))
                .andExpect(jsonPath("$.totalPrice").value(226.5))
                .andReturn();

        mockMvc.perform(delete(location).with(httpBasic("reviewer", "review-demo-only"))
                        .header("If-Match", updateResult.getResponse().getHeader("ETag")))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(location)).andExpect(status().isNotFound());
    }

    @Test
    void rejectsSequentialStaleUpdate() throws Exception {
        var created = mockMvc.perform(post("/api/orders")
                        .with(httpBasic("reviewer", "review-demo-only"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER))
                .andExpect(status().isCreated())
                .andReturn();
        var location = created.getResponse().getHeader("Location");
        var staleEtag = created.getResponse().getHeader("ETag");
        var orderId = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(put(location).with(httpBasic("reviewer", "review-demo-only"))
                        .header("If-Match", staleEtag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER.replace("PENDING", "CONFIRMED")))
                .andExpect(status().isOk());

        mockMvc.perform(put(location).with(httpBasic("reviewer", "review-demo-only"))
                        .header("If-Match", staleEtag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER.replace("PENDING", "CANCELLED")))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.message")
                        .value("Order with id " + orderId
                                + " was changed; retrieve the latest version and retry"));
    }

    @Test
    void returnsStableNotFoundErrorsForEveryResourceOperation() throws Exception {
        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order with id 999 was not found"))
                .andExpect(jsonPath("$.path").value("/api/orders/999"));

        mockMvc.perform(put("/api/orders/999")
                        .with(httpBasic("reviewer", "review-demo-only"))
                        .header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order with id 999 was not found"))
                .andExpect(jsonPath("$.path").value("/api/orders/999"));

        mockMvc.perform(delete("/api/orders/999")
                        .with(httpBasic("reviewer", "review-demo-only"))
                        .header("If-Match", "\"0\""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order with id 999 was not found"))
                .andExpect(jsonPath("$.path").value("/api/orders/999"));
    }

    @Test
    void returnsValidationDetails() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(httpBasic("reviewer", "review-demo-only"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"", "productName":"", "quantity":0,
                                 "unitPrice":0, "status":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.validationErrors.customerName").exists())
                .andExpect(jsonPath("$.validationErrors.quantity").exists());
    }

    @Test
    void listsCreatedOrdersWithPagination() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(httpBasic("reviewer", "review-demo-only"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].customerName").value("Grace Hopper"))
                .andExpect(jsonPath("$.content[0].totalPrice").value(151.0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void rejectsInvalidEnumAndExcessivePricePrecision() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(httpBasic("reviewer", "review-demo-only"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER.replace("PENDING", "UNKNOWN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request is malformed or contains an invalid value"));

        mockMvc.perform(post("/api/orders")
                        .with(httpBasic("reviewer", "review-demo-only"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER.replace("75.50", "75.501")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.unitPrice").exists());
    }

    @Test
    void rejectsInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/orders?page=-1&size=101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request is malformed or contains an invalid value"));
    }

    @Test
    @WithAnonymousUser
    void protectsWriteEndpointsButAllowsReads() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk());
    }

    @Test
    void propagatesSafeCorrelationIdsAndReplacesUnsafeValues() throws Exception {
        mockMvc.perform(get("/api/orders").header(CorrelationIdFilter.HEADER, "demo-review-123"))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.HEADER, "demo-review-123"));

        mockMvc.perform(get("/api/orders").header(CorrelationIdFilter.HEADER, "unsafe id"))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.HEADER, not("unsafe id")));
    }
}
