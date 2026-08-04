package com.manjusha.smartcodereview.exception;

public class StaleOrderVersionException extends RuntimeException {

    public StaleOrderVersionException(Long id) {
        super("Order with id %d was changed; retrieve the latest version and retry".formatted(id));
    }
}
