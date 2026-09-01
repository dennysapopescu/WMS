package com.warehouse.wms.service;

import com.warehouse.wms.dto.dashboard.DashboardSummaryResponse;
import com.warehouse.wms.dto.dashboard.RecentLogDto;
import com.warehouse.wms.model.InventoryLog;
import com.warehouse.wms.model.Product;
import com.warehouse.wms.repository.InventoryLogRepository;
import com.warehouse.wms.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final ProductRepository productRepository;
    private final InventoryLogRepository logRepository;

    public DashboardSummaryResponse getDashboardSummary() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String currentUser =
                authentication == null
                        ? "system"
                        : authentication.getName();

        List<Product> products =
                productRepository.findAll();

        List<InventoryLog> logs =
                logRepository.findAll(
                        Sort.by(
                                Sort.Direction.DESC,
                                "timestamp"
                        )
                );

        List<RecentLogDto> myActivity =
                logs.stream()
                        .filter(log ->
                                Objects.equals(
                                        log.getPerformedBy(),
                                        currentUser
                                )
                        )
                        .limit(5)
                        .map(this::toLogDto)
                        .toList();

        List<RecentLogDto> recentLogs =
                logs.stream()
                        .limit(10)
                        .map(this::toLogDto)
                        .toList();

        Map<String, Long> stockDistribution =
                products.stream()
                        .filter(product ->
                                product.getLocation() != null
                        )
                        .collect(
                                Collectors.groupingBy(
                                        product ->
                                                product.getLocation()
                                                        .getCode(),
                                        Collectors.counting()
                                )
                        );

        double totalValue =
                products.stream()
                        .mapToDouble(product -> {

                            if (product.getQuantity() == null
                                    || product.getPrice() == null) {

                                return 0.0;
                            }

                            return product.getQuantity()
                                    * product.getPrice()
                                    .doubleValue();
                        })
                        .sum();

        long totalQuantity =
                products.stream()
                        .mapToLong(product ->
                                product.getQuantity() == null
                                        ? 0
                                        : product.getQuantity()
                        )
                        .sum();

        long lowStock =
                products.stream()
                        .filter(product ->
                                product.getQuantity() != null
                                        && product.getQuantity() <= 5
                        )
                        .count();

        return new DashboardSummaryResponse(
                products.size(),
                totalQuantity,
                lowStock,
                Math.round(totalValue * 100.0) / 100.0,
                stockDistribution,
                calculateRestockingPredictions(
                        products,
                        logs
                ),
                detectNightActivity(logs),
                myActivity,
                recentLogs
        );
    }

    private Map<String, String>
    calculateRestockingPredictions(
            List<Product> products,
            List<InventoryLog> logs
    ) {

        Map<String, String> predictions =
                new HashMap<>();

        LocalDateTime sevenDaysAgo =
                LocalDateTime.now()
                        .minusDays(7);

        for (Product product : products) {

            long totalConsumed =
                    logs.stream()
                            .filter(log ->
                                    Objects.equals(
                                            log.getSku(),
                                            product.getSku()
                                    )
                            )
                            .filter(log ->
                                    log.getAction() != null
                            )
                            .filter(log ->
                                    log.getAction()
                                            .contains(
                                                    "PICKING_COMPLETED"
                                            )
                                            || log.getAction()
                                            .contains(
                                                    "PICKING FINALIZAT"
                                            )
                                            || log.getAction()
                                            .equals("STOCK_REDUCTION")
                                            || log.getAction()
                                            .equals("REDUCERE")
                            )
                            .filter(log ->
                                    log.getTimestamp() != null
                                            && log.getTimestamp()
                                            .isAfter(
                                                    sevenDaysAgo
                                            )
                            )
                            .mapToLong(log ->
                                    Math.abs(
                                            log.getQuantityChanged() == null
                                                    ? 0
                                                    : log.getQuantityChanged()
                                    )
                            )
                            .sum();

            double dailyRate =
                    totalConsumed / 7.0;

            if (dailyRate > 0
                    && product.getQuantity() != null) {

                predictions.put(
                        product.getSku(),
                        (int) (
                                product.getQuantity()
                                        / dailyRate
                        ) + " days"
                );

            } else {

                predictions.put(
                        product.getSku(),
                        "Stable"
                );
            }
        }

        return predictions;
    }

    private List<String> detectNightActivity(
            List<InventoryLog> logs
    ) {

        return logs.stream()
                .filter(log ->
                        log.getTimestamp() != null
                                && (
                                log.getTimestamp()
                                        .getHour() >= 22
                                        || log.getTimestamp()
                                        .getHour() < 6
                        )
                )
                .map(log ->
                        "⚠️ Anomaly: "
                                + log.getPerformedBy()
                                + " operated at "
                                + log.getTimestamp()
                                .getHour()
                                + ":"
                                + String.format(
                                "%02d",
                                log.getTimestamp()
                                        .getMinute()
                        )
                                + " on SKU "
                                + log.getSku()
                )
                .distinct()
                .limit(5)
                .toList();
    }


    private RecentLogDto toLogDto(
            InventoryLog log
    ) {

        return new RecentLogDto(
                log.getId(),
                log.getProductName(),
                log.getSku(),
                log.getAction(),
                log.getQuantityChanged(),
                log.getTimestamp(),
                log.getPerformedBy()
        );
    }
}