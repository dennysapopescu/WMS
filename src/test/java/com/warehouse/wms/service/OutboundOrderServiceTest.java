package com.warehouse.wms.service;

import com.warehouse.wms.dto.dashboard.RecentLogDto;
import com.warehouse.wms.dto.order.OrderRequest;
import com.warehouse.wms.dto.order.OrderResponse;
import com.warehouse.wms.dto.order.PickConfirmationRequest;
import com.warehouse.wms.exception.BusinessRuleException;
import com.warehouse.wms.exception.ResourceNotFoundException;
import com.warehouse.wms.model.InventoryLog;
import com.warehouse.wms.model.Location;
import com.warehouse.wms.model.OutboundOrder;
import com.warehouse.wms.model.Product;
import com.warehouse.wms.repository.InventoryLogRepository;
import com.warehouse.wms.repository.OrderRepository;
import com.warehouse.wms.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboundOrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryLogRepository inventoryLogRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private OutboundOrderService orderService;

    private Location location;
    private Product product;

    @BeforeEach
    void setUp() {
        location = new Location();
        location.setId(1L);
        location.setCode("A-01");
        location.setMaxCapacity(20);
        location.setCurrentOccupancy(10);

        product = new Product();
        product.setId(5L);
        product.setSku("SKU-100");
        product.setName("Thermal Printer");
        product.setQuantity(10);
        product.setLocation(location);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("operator1", "password", List.of())
        );
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    private OutboundOrder createOrder(Long id, String sku, int qty, String status, Location suggested) {
        OutboundOrder order = new OutboundOrder();
        order.setId(id);
        order.setSku(sku);
        order.setRequestedQuantity(qty);
        order.setPickedQuantity(status.equals("COMPLETED") ? qty : 0);
        order.setStatus(status);
        order.setSuggestedLocation(suggested);
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }

    @Test
    void findAll_returnsSortedOrders() {
        OutboundOrder completed = createOrder(1L, "SKU-1", 2, "COMPLETED", location);
        OutboundOrder pending = createOrder(2L, "SKU-2", 3, "PENDING", location);

        when(orderRepository.findAll()).thenReturn(List.of(completed, pending));

        List<OrderResponse> results = orderService.findAll();

        assertEquals(2, results.size());
        assertEquals("PENDING", results.get(0).status());
        assertEquals("COMPLETED", results.get(1).status());
    }

    @Test
    void getHistory_filtersAndLimitsLogs() {
        InventoryLog log1 = new InventoryLog();
        log1.setId(1L);
        log1.setAction("PICKING_COMPLETED");
        log1.setTimestamp(LocalDateTime.now().minusMinutes(5));

        InventoryLog log2 = new InventoryLog();
        log2.setId(2L);
        log2.setAction("CREARE"); // non-picking log, should be filtered out
        log2.setTimestamp(LocalDateTime.now().minusMinutes(10));

        when(inventoryLogRepository.findAll()).thenReturn(List.of(log1, log2));

        List<RecentLogDto> history = orderService.getHistory();

        assertEquals(1, history.size());
        assertEquals("PICKING_COMPLETED", history.get(0).action());
    }

    @Test
    void findById_returnsOrderWhenFound() {
        OutboundOrder order = createOrder(1L, "SKU-100", 2, "PENDING", location);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.findById(1L);

        assertEquals(1L, response.id());
        assertEquals("SKU-100", response.sku());
        assertEquals("A-01", response.suggestedLocationCode());
    }

    @Test
    void findById_throwsWhenNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.findById(99L));
    }

    @Test
    void create_succeedsWhenStockIsSufficient() {
        OrderRequest request = new OrderRequest("SKU-100", 4);
        when(productRepository.findBySku("SKU-100")).thenReturn(Optional.of(product));
        when(orderRepository.save(any(OutboundOrder.class))).thenAnswer(inv -> {
            OutboundOrder o = inv.getArgument(0);
            o.setId(10L);
            return o;
        });

        OrderResponse response = orderService.create(request);

        assertNotNull(response.id());
        assertEquals("SKU-100", response.sku());
        assertEquals(4, response.requestedQuantity());
        assertEquals("PENDING", response.status());
        assertEquals("A-01", response.suggestedLocationCode());
        verify(auditService).inventoryEvent("Thermal Printer", "SKU-100", "PICKING_CREATED", 4);
    }

    @Test
    void create_throwsWhenSkuNotFound() {
        OrderRequest request = new OrderRequest("NON-EXISTENT", 2);
        when(productRepository.findBySku("NON-EXISTENT")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.create(request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void create_throwsWhenStockIsInsufficient() {
        OrderRequest request = new OrderRequest("SKU-100", 15); // product quantity is 10
        when(productRepository.findBySku("SKU-100")).thenReturn(Optional.of(product));

        assertThrows(BusinessRuleException.class, () -> orderService.create(request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void confirmPick_throwsWhenOrderNotPending() {
        when(orderRepository.findByIdAndStatus(1L, "PENDING")).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class,
                () -> orderService.confirmPick(1L, new PickConfirmationRequest("A-01")));
    }

    @Test
    void confirmPick_throwsWhenScannedCodeDoesNotMatchLocation() {
        OutboundOrder pending = createOrder(1L, "SKU-100", 3, "PENDING", location);
        when(orderRepository.findByIdAndStatus(1L, "PENDING")).thenReturn(Optional.of(pending));

        assertThrows(BusinessRuleException.class,
                () -> orderService.confirmPick(1L, new PickConfirmationRequest("B-99")));
    }

    @Test
    void confirmPick_throwsWhenStockInsufficientAtPickingTime() {
        OutboundOrder pending = createOrder(1L, "SKU-100", 12, "PENDING", location); // demands 12
        product.setQuantity(5); // only 5 left
        when(orderRepository.findByIdAndStatus(1L, "PENDING")).thenReturn(Optional.of(pending));
        when(productRepository.findBySku("SKU-100")).thenReturn(Optional.of(product));

        assertThrows(BusinessRuleException.class,
                () -> orderService.confirmPick(1L, new PickConfirmationRequest("A-01")));
    }

    @Test
    void confirmPick_succeedsAndUpdatesStockOccupancyAndStatus() {
        OutboundOrder pending = createOrder(1L, "SKU-100", 3, "PENDING", location);
        when(orderRepository.findByIdAndStatus(1L, "PENDING")).thenReturn(Optional.of(pending));
        when(productRepository.findBySku("SKU-100")).thenReturn(Optional.of(product));

        OrderResponse response = orderService.confirmPick(1L, new PickConfirmationRequest("LOC:A-01"));

        assertEquals("COMPLETED", response.status());
        assertEquals(3, response.pickedQuantity());
        assertEquals(7, product.getQuantity()); // 10 - 3
        assertEquals(7, location.getCurrentOccupancy()); // 10 - 3
        assertEquals("operator1", response.assignedTo());
        verify(auditService).inventoryEvent("Thermal Printer", "SKU-100", "PICKING_COMPLETED", -3);
    }

    @Test
    void scanAndConfirm_normalizesCodeWithPrefix() {
        OutboundOrder pending = createOrder(1L, "SKU-100", 2, "PENDING", location);
        when(orderRepository.findByIdAndStatus(1L, "PENDING")).thenReturn(Optional.of(pending));
        when(productRepository.findBySku("SKU-100")).thenReturn(Optional.of(product));

        OrderResponse response = orderService.scanAndConfirm(1L, "LOCATION:A-01");

        assertEquals("COMPLETED", response.status());
        assertEquals(8, product.getQuantity());
    }

    @Test
    void cancel_deletesPendingOrder() {
        OutboundOrder pending = createOrder(1L, "SKU-100", 2, "PENDING", location);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pending));

        orderService.cancel(1L);

        verify(orderRepository).delete(pending);
    }

    @Test
    void cancel_throwsWhenOrderNotPending() {
        OutboundOrder completed = createOrder(1L, "SKU-100", 2, "COMPLETED", location);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(completed));

        assertThrows(BusinessRuleException.class, () -> orderService.cancel(1L));
        verify(orderRepository, never()).delete(any());
    }
}
