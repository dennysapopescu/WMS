package com.warehouse.wms.controller;

import com.warehouse.wms.model.Location;
import com.warehouse.wms.model.OutboundOrder;
import com.warehouse.wms.model.Product;
import com.warehouse.wms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderControllerScanConfirmTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryLogRepository inventoryLogRepository;
    @Mock
    private LocationRepository locationRepository;

    private OrderController orderController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderController = new OrderController(orderRepository, productRepository, inventoryLogRepository, locationRepository);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test-operator", null, List.of())
        );
    }

    private Location location(long id, String code, int occupancy) {
        Location loc = new Location();
        loc.setId(id);
        loc.setCode(code);
        loc.setCurrentOccupancy(occupancy);
        return loc;
    }

    private Product product(String sku, int quantity, Location loc) {
        Product p = new Product();
        p.setSku(sku);
        p.setQuantity(quantity);
        p.setLocation(loc);
        return p;
    }

    private OutboundOrder order(long id, String sku, int qty, Location suggested) {
        OutboundOrder o = new OutboundOrder();
        o.setId(id);
        o.setSku(sku);
        o.setRequestedQuantity(qty);
        o.setStatus("PENDING");
        o.setSuggestedLocation(suggested);
        return o;
    }

    @Test
    void scanConfirm_returns404WhenOrderDoesNotExist() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<String> response = orderController.scanAndConfirm(99L, "A-01");

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void scanConfirm_returns400WhenScannedCodeDoesNotMatchSuggestedLocation() {
        Location suggested = location(1L, "A-01", 5);
        OutboundOrder pendingOrder = order(1L, "SKU-1", 2, suggested);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        ResponseEntity<String> response = orderController.scanAndConfirm(1L, "WRONG-CODE");

        assertEquals(400, response.getStatusCode().value());
        verify(productRepository, never()).save(any());
    }

    @Test
    void scanConfirm_returns500WhenStockIsInsufficientAtValidationTime() {
        Location loc = location(1L, "A-01", 5);
        OutboundOrder pendingOrder = order(1L, "SKU-1", 10, loc);
        Product lowStockProduct = product("SKU-1", 2, loc); // mai puțin decât cere comanda

        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(productRepository.findAll()).thenReturn(List.of(lowStockProduct));

        ResponseEntity<String> response = orderController.scanAndConfirm(1L, "A-01");

        assertEquals(500, response.getStatusCode().value());
    }

    @Test
    void scanConfirm_succeedsAndUpdatesStockAndLocationAndOrderStatus() {
        Location loc = location(1L, "A-01", 5);
        OutboundOrder pendingOrder = order(1L, "SKU-1", 3, loc);
        Product inStockProduct = product("SKU-1", 10, loc);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(productRepository.findAll()).thenReturn(List.of(inStockProduct));

        ResponseEntity<String> response = orderController.scanAndConfirm(1L, "a-01"); // case-insensitive

        assertEquals(200, response.getStatusCode().value());
        assertEquals(7, inStockProduct.getQuantity());           // 10 - 3
        assertEquals(2, loc.getCurrentOccupancy());              // 5 - 3
        assertEquals("COMPLETED", pendingOrder.getStatus());
        verify(inventoryLogRepository, times(1)).save(any());
    }

    @Test
    void scanConfirm_returns409OnConcurrentModificationConflict() {
        Location loc = location(1L, "A-01", 5);
        OutboundOrder pendingOrder = order(1L, "SKU-1", 3, loc);
        Product inStockProduct = product("SKU-1", 10, loc);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(productRepository.findAll()).thenReturn(List.of(inStockProduct));
        // Simulăm exact scenariul pentru care am adăugat @Version: alt operator a
        // modificat produsul între citire și scriere -> Hibernate ar arunca asta la save().
        when(productRepository.save(any())).thenThrow(new OptimisticLockingFailureException("stale version"));

        ResponseEntity<String> response = orderController.scanAndConfirm(1L, "A-01");

        assertEquals(409, response.getStatusCode().value());
        assertEquals("PENDING", pendingOrder.getStatus()); // nu s-a marcat COMPLETED
    }
}
