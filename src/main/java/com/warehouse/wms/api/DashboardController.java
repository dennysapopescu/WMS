package com.warehouse.wms.api;

import com.warehouse.wms.dto.dashboard.DashboardSummaryResponse;
import com.warehouse.wms.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardSummaryResponse getDashboardSummary() {
        return dashboardService.getDashboardSummary();
    }
}