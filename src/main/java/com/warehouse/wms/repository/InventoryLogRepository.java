package com.warehouse.wms.repository;

import com.warehouse.wms.model.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {
    List<InventoryLog> findTop5ByActionOrderByTimestampDesc(String action);
}
