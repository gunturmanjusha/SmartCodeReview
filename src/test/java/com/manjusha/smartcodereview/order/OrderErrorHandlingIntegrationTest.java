package com.manjusha.smartcodereview.order;

import com.manjusha.smartcodereview.order.controller.OrderController;
import com.manjusha.smartcodereview.order.entity.Order;
import com.manjusha.smartcodereview.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderErrorHandlingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void hidesUnexpectedFailureDetails() throws Exception {
        when(orderService.get(99L)).thenThrow(new IllegalStateException("database password leaked"));

        mockMvc.perform(get("/api/orders/99"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.path").value("/api/orders/99"))
                .andExpect(content().string(not(containsString("database password leaked"))));
    }

    @Test
    void mapsPersistenceConflictToStableResponse() throws Exception {
        when(orderService.get(7L)).thenThrow(new DataIntegrityViolationException("constraint details"));

        mockMvc.perform(get("/api/orders/7"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The request conflicts with persisted data"));
    }

    @Test
    void mapsConcurrentUpdateToStableResponse() throws Exception {
        when(orderService.get(8L)).thenThrow(new ObjectOptimisticLockingFailureException(Order.class, 8L));

        mockMvc.perform(get("/api/orders/8"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("The order was changed by another request; reload and retry"));
    }
}
