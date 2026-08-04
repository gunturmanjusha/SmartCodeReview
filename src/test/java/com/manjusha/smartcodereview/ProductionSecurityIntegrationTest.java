package com.manjusha.smartcodereview;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:prod-security;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=never",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://identity.example.test"
})
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class ProductionSecurityIntegrationTest {

    private static final String VALID_ORDER = """
            {
              "customerName": "Katherine Johnson",
              "productName": "Engineering Laptop",
              "quantity": 1,
              "unitPrice": 1299.00,
              "status": "PENDING"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void configureTokens() {
        when(jwtDecoder.decode("admin-token")).thenReturn(jwt("admin", "ORDER_ADMIN"));
        when(jwtDecoder.decode("operator-token")).thenReturn(jwt("operator", "OPERATIONS"));
        when(jwtDecoder.decode("reader-token")).thenReturn(jwt("reader", "ORDER_READER"));
    }

    @Test
    void enforcesOrderRoleMatrixWithProductionJwtAuthentication() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer reader-token"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer reader-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER))
                .andExpect(status().isForbidden());

        var created = mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER))
                .andExpect(status().isCreated())
                .andReturn();
        var location = created.getResponse().getHeader("Location");
        var etag = created.getResponse().getHeader("ETag");

        mockMvc.perform(put(location)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer reader-token")
                        .header(HttpHeaders.IF_MATCH, etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete(location)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer reader-token"))
                .andExpect(status().isForbidden());

        var updated = mockMvc.perform(put(location)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
                        .header(HttpHeaders.IF_MATCH, etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER.replace("PENDING", "CONFIRMED")))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(delete(location)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
                        .header(HttpHeaders.IF_MATCH, updated.getResponse().getHeader("ETag")))
                .andExpect(status().isNoContent());

        mockMvc.perform(put("/api/orders/999")
                        .header(HttpHeaders.IF_MATCH, updated.getResponse().getHeader("ETag"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/orders/999"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void restrictsOperationalEndpointsToOperators() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/actuator/info")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/actuator/info")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer operator-token"))
                .andExpect(status().isOk());
    }

    private Jwt jwt(String subject, String role) {
        return Jwt.withTokenValue(subject + "-token")
                .header("alg", "none")
                .subject(subject)
                .claim("roles", List.of(role))
                .build();
    }
}
