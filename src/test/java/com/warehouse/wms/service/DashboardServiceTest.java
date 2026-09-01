package com.warehouse.wms.service;

import com.warehouse.wms.model.InventoryLog;
import com.warehouse.wms.model.Location;
import com.warehouse.wms.model.Product;
import com.warehouse.wms.repository.InventoryLogRepository;
import com.warehouse.wms.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryLogRepository logRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void dashboardCalculatesSummaryWithoutWritingToDatabase() {

        Location location =
                new Location();

        location.setId(1L);
        location.setCode("A-01");

        Product product =
                new Product();

        product.setId(1L);
        product.setSku("SKU-1");
        product.setName("Keyboard");
        product.setQuantity(10);
        product.setPrice(
                new BigDecimal("25.00")
        );
        product.setLocation(location);

        InventoryLog log =
                new InventoryLog();

        log.setId(1L);
        log.setSku("SKU-1");
        log.setProductName("Keyboard");
        log.setAction("STOCK_REDUCTION");
        log.setQuantityChanged(-2);
        log.setTimestamp(
                LocalDateTime.now()
                        .minusDays(1)
        );
        log.setPerformedBy("operator1");

        when(
                productRepository.findAll()
        ).thenReturn(
                List.of(product)
        );

        when(
                logRepository.findAll(
                        any(Sort.class)
                )
        ).thenReturn(
                List.of(log)
        );

        SecurityContextHolder
                .getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                "operator1",
                                "password"
                        )
                );

        var result =
                dashboardService
                        .getDashboardSummary();

        assertEquals(
                1,
                result.totalItems()
        );

        assertEquals(
                10,
                result.totalQuantity()
        );

        assertEquals(
                250.0,
                result.totalValue()
        );

        assertEquals(
                1,
                result.myActivity().size()
        );

        assertEquals(
                "35 days",
                result.predictions()
                        .get("SKU-1")
        );


        /*
         * Most important architectural assertion:
         * dashboard GET must not write.
         */
        verifyNoMoreInteractions(
                productRepository,
                logRepository
        );
    }
}