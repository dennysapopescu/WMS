package com.warehouse.wms.dto.product;

import java.math.BigDecimal;

public record ProductResponse(Long id, String sku, String name, Integer quantity, BigDecimal price,
                              Long locationId, String locationCode, Long version) { }
