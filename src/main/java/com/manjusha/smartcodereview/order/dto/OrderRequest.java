package com.manjusha.smartcodereview.order.dto;

import com.manjusha.smartcodereview.order.entity.OrderStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record OrderRequest(
        @NotBlank @Size(max = 100) String customerName,
        @NotBlank @Size(max = 100) String productName,
        @NotNull @Min(1) Integer quantity,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal unitPrice,
        @NotNull OrderStatus status
) {
}
