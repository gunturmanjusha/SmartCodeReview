package com.manjusha.smartcodereview.order;

import com.manjusha.smartcodereview.exception.OrderNotFoundException;
import com.manjusha.smartcodereview.exception.StaleOrderVersionException;
import com.manjusha.smartcodereview.order.dto.OrderRequest;
import com.manjusha.smartcodereview.order.entity.Order;
import com.manjusha.smartcodereview.order.entity.OrderStatus;
import com.manjusha.smartcodereview.order.repository.OrderRepository;
import com.manjusha.smartcodereview.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createsOrder() {
        var request = new OrderRequest("Ada Lovelace", "Keyboard", 2,
                new BigDecimal("49.99"), OrderStatus.PENDING);
        when(orderRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = orderService.create(request);

        assertThat(response.customerName()).isEqualTo("Ada Lovelace");
        assertThat(response.totalPrice()).isEqualByComparingTo("99.98");
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository).saveAndFlush(org.mockito.ArgumentMatchers.any(Order.class));
    }

    @Test
    void reportsMissingOrder() {
        when(orderRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.get(42L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("Order with id 42 was not found");
    }

    @Test
    void listsOrdersInRepositoryOrder() {
        var first = order("Ada Lovelace", "USB-C Dock", 1, "89.00", OrderStatus.PENDING);
        var second = order("Grace Hopper", "Keyboard", 2, "75.50", OrderStatus.CONFIRMED);
        when(orderRepository.findAll(org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Sort.class)))
                .thenReturn(java.util.List.of(first, second));

        var responses = orderService.getAll(0, 20);

        assertThat(responses.content()).extracting(response -> response.customerName())
                .containsExactly("Ada Lovelace", "Grace Hopper");
        assertThat(responses.totalElements()).isEqualTo(2);
        verify(orderRepository).findAll(org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Sort.class));
    }

    @Test
    void updatesExistingOrder() {
        var existing = order("Ada Lovelace", "Dock", 1, "89.00", OrderStatus.PENDING);
        var request = new OrderRequest("Grace Hopper", "Dock Pro", 2,
                new BigDecimal("99.00"), OrderStatus.CONFIRMED);
        ReflectionTestUtils.setField(existing, "version", 3L);
        when(orderRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(orderRepository.saveAndFlush(existing)).thenReturn(existing);

        var response = orderService.update(7L, 3L, request);

        assertThat(response.customerName()).isEqualTo("Grace Hopper");
        assertThat(response.productName()).isEqualTo("Dock Pro");
        assertThat(response.quantity()).isEqualTo(2);
        assertThat(response.totalPrice()).isEqualByComparingTo("198.00");
        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository).saveAndFlush(existing);
    }

    @Test
    void rejectsStaleUpdate() {
        var existing = order("Ada Lovelace", "Dock", 1, "89.00", OrderStatus.PENDING);
        var request = new OrderRequest("Ada Lovelace", "Dock Pro", 2,
                new BigDecimal("99.00"), OrderStatus.CONFIRMED);
        ReflectionTestUtils.setField(existing, "version", 4L);
        when(orderRepository.findById(7L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> orderService.update(7L, 3L, request))
                .isInstanceOf(StaleOrderVersionException.class)
                .hasMessage("Order with id 7 was changed; retrieve the latest version and retry");
    }

    @Test
    void deletesExistingOrder() {
        var existing = order("Grace Hopper", "Keyboard", 1, "75.50", OrderStatus.PENDING);
        ReflectionTestUtils.setField(existing, "version", 2L);
        when(orderRepository.findById(9L)).thenReturn(Optional.of(existing));

        orderService.delete(9L, 2L);

        verify(orderRepository).delete(existing);
    }

    @Test
    void rejectsStaleDeleteWithoutDeletingOrder() {
        var existing = order("Grace Hopper", "Keyboard", 1, "75.50", OrderStatus.PENDING);
        ReflectionTestUtils.setField(existing, "version", 3L);
        when(orderRepository.findById(9L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> orderService.delete(9L, 2L))
                .isInstanceOf(StaleOrderVersionException.class)
                .hasMessage("Order with id 9 was changed; retrieve the latest version and retry");
        verify(orderRepository, never()).delete(existing);
    }

    @Test
    void reportsMissingOrderDuringDelete() {
        when(orderRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.delete(9L, 0L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("Order with id 9 was not found");
    }

    private Order order(String customerName, String productName, int quantity,
                        String unitPrice, OrderStatus status) {
        return new Order(customerName, productName, quantity, new BigDecimal(unitPrice), status);
    }
}
