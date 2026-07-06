package com.warehouse.wms.controller;

import com.warehouse.wms.model.Product;
import com.warehouse.wms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderControllerTest {

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

        // saveLog() (called internally on order creation) reads the current user from
        // the security context, so we need a fake authenticated user in tests.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test-operator", null, java.util.List.of())
        );
    }

    private Product product(String sku, int quantity) {
        Product p = new Product();
        p.setSku(sku);
        p.setQuantity(quantity);
        return p;
    }

    @Test
    void createOrder_rejectsWhenSkuDoesNotExist() {
        when(productRepository.findBySku("MISSING-SKU")).thenReturn(Optional.empty());
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = orderController.createOrder("MISSING-SKU", 5, redirectAttributes);

        assertEquals("redirect:/", view);
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("error"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_rejectsWhenRequestedQuantityExceedsStock() {
        when(productRepository.findBySku("SKU-1")).thenReturn(Optional.of(product("SKU-1", 3)));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = orderController.createOrder("SKU-1", 10, redirectAttributes);

        assertEquals("redirect:/", view);
        assertTrue(redirectAttributes.getFlashAttributes().get("error").toString().contains("Stoc insuficient"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_succeedsWhenStockIsSufficient() {
        when(productRepository.findBySku("SKU-1")).thenReturn(Optional.of(product("SKU-1", 10)));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = orderController.createOrder("SKU-1", 4, redirectAttributes);

        assertEquals("redirect:/orders", view);
        verify(orderRepository, times(1)).save(any());
    }
}
