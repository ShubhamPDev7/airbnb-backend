package com.codingshuttle.projects.airBnbApp.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateInventoryRequestDto {
    @NotNull(message = "Start date cannot be null")
    private LocalDate startDate;

    @NotNull(message = "End date cannot be null")
    private LocalDate endDate;

    @NotNull(message = "Surge factor cannot be null")
    @DecimalMin(value = "1.0", inclusive = true, message = "Surge factor must be at least 1.0")
    @DecimalMax(value = "5.0", inclusive = true, message = "Surge factor cannot exceed 5.0")
    private BigDecimal surgeFactor;

    @NotNull(message = "Closed status cannot be null")
    private Boolean closed;
}
