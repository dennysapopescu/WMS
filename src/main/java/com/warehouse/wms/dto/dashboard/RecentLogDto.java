package com.warehouse.wms.dto.dashboard;

import java.time.LocalDateTime;

public record RecentLogDto(
        Long id,
        String productName,
        String sku,
        String action,
        Integer quantityChanged,
        LocalDateTime timestamp,
        String performedBy
) {
}
