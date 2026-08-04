package com.manjusha.smartcodereview.order.repository;

import com.manjusha.smartcodereview.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
