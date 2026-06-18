package com.warehouse.wms.repository;

import com.warehouse.wms.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
    Location findByCode(String code);
}