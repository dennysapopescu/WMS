package com.warehouse.wms.repository;

import com.warehouse.wms.model.OutboundOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<OutboundOrder, Long> {
    List<OutboundOrder> findByStatus(String status);
}