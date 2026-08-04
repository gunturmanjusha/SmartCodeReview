package com.manjusha.smartcodereview.order.service;

import com.manjusha.smartcodereview.exception.OrderNotFoundException;
import com.manjusha.smartcodereview.exception.StaleOrderVersionException;
import com.manjusha.smartcodereview.order.dto.OrderRequest;
import com.manjusha.smartcodereview.order.dto.OrderResponse;
import com.manjusha.smartcodereview.order.dto.PageResponse;
import com.manjusha.smartcodereview.order.entity.Order;
import com.manjusha.smartcodereview.order.repository.OrderRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderResponse create(OrderRequest request) {
        var order = new Order(request.customerName(), request.productName(), request.quantity(),
                request.unitPrice(), request.status());
        return OrderResponse.from(orderRepository.saveAndFlush(order));
    }

    public OrderResponse get(Long id) {
        return OrderResponse.from(findOrder(id));
    }

    public PageResponse<OrderResponse> getAll(int page, int size) {
        var request = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        return PageResponse.from(orderRepository.findAll(request).map(OrderResponse::from));
    }

    @Transactional
    public OrderResponse update(Long id, Long expectedVersion, OrderRequest request) {
        var order = findOrder(id);
        if (!Objects.equals(expectedVersion, order.getVersion())) {
            throw new StaleOrderVersionException(id);
        }
        order.update(request.customerName(), request.productName(), request.quantity(),
                request.unitPrice(), request.status());
        return OrderResponse.from(orderRepository.saveAndFlush(order));
    }

    @Transactional
    public void delete(Long id, Long expectedVersion) {
        var order = findOrder(id);
        if (!Objects.equals(expectedVersion, order.getVersion())) {
            throw new StaleOrderVersionException(id);
        }
        orderRepository.delete(order);
    }

    private Order findOrder(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }
}
