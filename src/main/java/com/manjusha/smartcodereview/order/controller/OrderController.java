package com.manjusha.smartcodereview.order.controller;

import com.manjusha.smartcodereview.order.dto.OrderRequest;
import com.manjusha.smartcodereview.order.dto.OrderResponse;
import com.manjusha.smartcodereview.order.dto.PageResponse;
import com.manjusha.smartcodereview.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
@RestController
@Validated
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        var created = orderService.create(request);
        return ResponseEntity.created(URI.create("/api/orders/" + created.id()))
                .eTag(created.version().toString())
                .body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> get(@PathVariable Long id) {
        var order = orderService.get(id);
        return ResponseEntity.ok().eTag(order.version().toString()).body(order);
    }

    @GetMapping
    public PageResponse<OrderResponse> getAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return orderService.getAll(page, size);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderResponse> update(
            @PathVariable Long id,
            @RequestHeader(HttpHeaders.IF_MATCH) @Pattern(regexp = "\"?[0-9]+\"?") String ifMatch,
            @Valid @RequestBody OrderRequest request) {
        var updated = orderService.update(id, parseVersion(ifMatch), request);
        return ResponseEntity.ok().eTag(updated.version().toString()).body(updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            @RequestHeader(HttpHeaders.IF_MATCH) @Pattern(regexp = "\"?[0-9]+\"?") String ifMatch) {
        orderService.delete(id, parseVersion(ifMatch));
    }

    private Long parseVersion(String ifMatch) {
        return Long.valueOf(ifMatch.replace("\"", ""));
    }
}
