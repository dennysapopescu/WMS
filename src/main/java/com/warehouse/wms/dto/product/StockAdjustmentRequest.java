package com.warehouse.wms.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StockAdjustmentRequest(@NotNull Integer quantityDelta, @NotBlank String reason) { }
