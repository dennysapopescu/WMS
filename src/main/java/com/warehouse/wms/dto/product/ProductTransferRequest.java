package com.warehouse.wms.dto.product;

import jakarta.validation.constraints.NotNull;

public record ProductTransferRequest(
        @NotNull(message = "Product ID is required") Long productId,
        @NotNull(message = "Target location ID is required") Long newLocationId
) {
}
