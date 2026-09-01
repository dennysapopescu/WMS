package com.warehouse.wms.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @NotBlank @Size(max = 100) String sku,
        @NotBlank @Size(max = 255) String name,
        @NotNull @Min(0) Integer quantity,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) @Digits(integer = 17, fraction = 2) BigDecimal price,
        @NotNull Long locationId
) { }
