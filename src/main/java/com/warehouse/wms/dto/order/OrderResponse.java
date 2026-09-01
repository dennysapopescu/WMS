package com.warehouse.wms.dto.order;

import java.time.LocalDateTime;

public record OrderResponse(Long id, String sku, Integer requestedQuantity, Integer pickedQuantity,
                            String status, Long suggestedLocationId, String suggestedLocationCode,
                            String assignedTo, LocalDateTime createdAt, LocalDateTime assignedAt) { }
