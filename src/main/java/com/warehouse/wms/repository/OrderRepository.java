package com.warehouse.wms.repository;

import com.warehouse.wms.model.OutboundOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OutboundOrder, Long> {
    List<OutboundOrder> findByStatus(String status);
    Optional<OutboundOrder> findByIdAndStatus(Long id, String status);
}
