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
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OutboundOrderService {

    private static final String PENDING = "PENDING";
    private static final String COMPLETED = "COMPLETED";

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final AuditService auditService;

    public List<OrderResponse> findAll() {

        return orderRepository.findAll()
                .stream()
                .sorted(
                        Comparator
                                .comparing(
                                        (OutboundOrder order) ->
                                                COMPLETED.equals(
                                                        order.getStatus()
                                                )
                                )
                                .thenComparing(order ->
                                        order.getSuggestedLocation() != null
                                                ? order.getSuggestedLocation().getCode()
                                                : "ZZZ"
                                )
                )
                .map(this::toResponse)
                .toList();
    }

    public List<RecentLogDto> getHistory() {

        return inventoryLogRepository.findAll()
                .stream()
                .filter(log ->
                        log.getAction() != null
                                && (
                                log.getAction()
                                        .contains("PICKING")
                                        || log.getAction()
                                        .contains("COMPLETED")
                        )
                )

                .sorted(
                        Comparator.comparing(
                                InventoryLog::getTimestamp
                        ).reversed()
                )
                .limit(10)
                .map(this::toLogDto)
                .toList();
    }

    public OrderResponse findById(Long id) {
        return toResponse(requiredOrder(id));
    }

    @Transactional
    public OrderResponse create(OrderRequest request) {

        Product product =
                productRepository.findBySku(
                                request.sku().trim()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Product SKU",
                                        request.sku()
                                )
                        );

        if (product.getQuantity()
                < request.requestedQuantity()) {

            throw new BusinessRuleException(
                    "Insufficient available stock"
            );
        }

        OutboundOrder order = new OutboundOrder();

        order.setSku(product.getSku());
        order.setRequestedQuantity(
                request.requestedQuantity()
        );
        order.setPickedQuantity(0);
        order.setStatus(PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setSuggestedLocation(
                product.getLocation()
        );

        OutboundOrder saved =
                orderRepository.save(order);

        auditService.inventoryEvent(
                product.getName(),
                product.getSku(),
                "PICKING_CREATED",
                request.requestedQuantity()
        );

        return toResponse(saved);
    }

    @Transactional
    public OrderResponse confirmPick(
            Long id,
            PickConfirmationRequest request
    ) {

        OutboundOrder order =
                orderRepository.findByIdAndStatus(
                                id,
                                PENDING
                        )
                        .orElseThrow(() ->
                                new BusinessRuleException(
                                        "Only a pending order can be confirmed"
                                )
                        );

        String scannedCode =
                normalizeLocationCode(
                        request.scannedLocationCode()
                );

        Location location =
                order.getSuggestedLocation();

        if (location == null
                || !location.getCode()
                .equalsIgnoreCase(scannedCode)) {

            throw new BusinessRuleException(
                    "Scanned location does not match the assigned location"
            );
        }

        Product product =
                productRepository
                        .findBySku(order.getSku())
                        .filter(found ->
                                found.getLocation() != null
                                        && found.getLocation()
                                        .getId()
                                        .equals(location.getId())
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Product at assigned location",
                                        order.getSku()
                                )
                        );

        if (product.getQuantity()
                < order.getRequestedQuantity()) {

            throw new BusinessRuleException(
                    "Insufficient stock at pick confirmation"
            );
        }

        int quantity =
                order.getRequestedQuantity();

        product.setQuantity(
                product.getQuantity() - quantity
        );

        location.setCurrentOccupancy(
                Math.max(
                        0,
                        location.getCurrentOccupancy()
                                - quantity
                )
        );

        order.setPickedQuantity(quantity);
        order.setStatus(COMPLETED);

        String username =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        order.setAssignedTo(username);
        order.setAssignedAt(LocalDateTime.now());

        auditService.inventoryEvent(
                product.getName(),
                product.getSku(),
                "PICKING_COMPLETED",
                -quantity
        );

        return toResponse(order);
    }

    public OrderResponse scanAndConfirm(
            Long orderId,
            String scannedCode
    ) {

        String cleanCode =
                normalizeLocationCode(scannedCode);

        return confirmPick(
                orderId,
                new PickConfirmationRequest(cleanCode)
        );
    }

    @Transactional
    public void cancel(Long id) {

        OutboundOrder order =
                requiredOrder(id);

        if (!PENDING.equals(order.getStatus())) {

            throw new BusinessRuleException(
                    "Only pending orders can be cancelled"
            );
        }

        orderRepository.delete(order);
    }

    private String normalizeLocationCode(
            String scannedCode
    ) {

        if (scannedCode == null) {
            throw new BusinessRuleException(
                    "Scanned location code is required"
            );
        }

        return scannedCode
                .trim()
                .toUpperCase()
                .replace("LOC:", "")
                .replace("LOCATION:", "");
    }

    private OutboundOrder requiredOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Outbound order",
                                id
                        )
                );
    }

    private OrderResponse toResponse(
            OutboundOrder order
    ) {

        Location location =
                order.getSuggestedLocation();

        return new OrderResponse(
                order.getId(),
                order.getSku(),
                order.getRequestedQuantity(),
                order.getPickedQuantity(),
                order.getStatus(),
                location == null
                        ? null
                        : location.getId(),
                location == null
                        ? null
                        : location.getCode(),
                order.getAssignedTo(),
                order.getCreatedAt(),
                order.getAssignedAt()
        );
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
