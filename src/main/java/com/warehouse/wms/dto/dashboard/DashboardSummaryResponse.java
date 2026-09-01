package com.warehouse.wms.dto.dashboard;

import java.util.List;
import java.util.Map;

public record DashboardSummaryResponse(
        long totalItems,
        long totalQuantity,
        long lowStock,
        double totalValue,
        Map<String, Long> stockDistribution,
        Map<String, String> predictions,
        List<String> aiAlerts,
        List<RecentLogDto> myActivity,
        List<RecentLogDto> recentLogs
) {
}
