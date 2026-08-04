package com.manjusha.smartcodereview.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(Long id) {
        super("Order with id %d was not found".formatted(id));
    }
}
