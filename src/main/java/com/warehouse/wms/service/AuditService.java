package com.warehouse.wms.service;

import com.warehouse.wms.model.InventoryLog;
import com.warehouse.wms.repository.InventoryLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final InventoryLogRepository inventoryLogRepository;

    public void inventoryEvent(String productName, String sku, String action, int quantityChanged) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String actor = authentication == null ? "system" : authentication.getName();
        InventoryLog log = new InventoryLog();
        log.setProductName(productName);
        log.setSku(sku);
        log.setAction(action);
        log.setQuantityChanged(quantityChanged);
        log.setTimestamp(LocalDateTime.now());
        log.setPerformedBy(actor);
        inventoryLogRepository.save(log);
    }
}
