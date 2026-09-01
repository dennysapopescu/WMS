package com.warehouse.wms.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderRequest(@NotBlank String sku, @NotNull @Min(1) Integer requestedQuantity) { }
