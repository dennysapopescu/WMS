package com.warehouse.wms.api;

import com.warehouse.wms.dto.dashboard.RecentLogDto;
import com.warehouse.wms.dto.order.OrderRequest;
import com.warehouse.wms.dto.order.OrderResponse;
import com.warehouse.wms.dto.order.PickConfirmationRequest;
import com.warehouse.wms.service.OutboundOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/outbound-orders")
@RequiredArgsConstructor
public class OutboundOrderController {

    private final OutboundOrderService orderService;

    @GetMapping
    public List<OrderResponse> findAll() {
        return orderService.findAll();
    }

    @GetMapping("/history")
    public List<RecentLogDto> getHistory() {
        return orderService.getHistory();
    }

    @GetMapping("/{id}")
    public OrderResponse findById(
            @PathVariable Long id
    ) {
        return orderService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<OrderResponse> create(
            @Valid @RequestBody OrderRequest request
    ) {

        OrderResponse created =
                orderService.create(request);

        URI uri = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity
                .created(uri)
                .body(created);
    }

    @PostMapping("/{id}/confirm-pick")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public OrderResponse confirmPick(
            @PathVariable Long id,
            @Valid @RequestBody PickConfirmationRequest request
    ) {
        return orderService.confirmPick(id, request);
    }

    @PostMapping("/scan-confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public OrderResponse scanAndConfirm(
            @RequestParam Long orderId,
            @RequestParam String scannedCode
    ) {
        return orderService.scanAndConfirm(
                orderId,
                scannedCode
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Void> cancel(
            @PathVariable Long id
    ) {

        orderService.cancel(id);

        return ResponseEntity.noContent().build();
    }
}
