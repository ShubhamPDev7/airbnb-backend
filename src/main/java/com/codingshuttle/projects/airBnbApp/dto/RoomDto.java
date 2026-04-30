package com.codingshuttle.projects.airBnbApp.dto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RoomDto {

    private Long id;

    @NotBlank(message = "Room type cannot be blank")
    private String type;

    @NotNull(message = "Base price cannot be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Base price must be greater than 0")
    private BigDecimal basePrice;

    private String[] photos;

    private String[] amenities;

    @NotNull(message = "Total room count cannot be null")
    @Min(value = 1, message = "Total room count must be at least 1")
    private Integer totalCount;

    @NotNull(message = "Room capacity cannot be null")
    @Min(value = 1, message = "Room capacity must be at least 1")
    private Integer capacity;

}
