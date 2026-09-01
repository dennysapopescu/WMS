package com.warehouse.wms.repository;

import com.warehouse.wms.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {
    Optional<Location> findByCodeIgnoreCase(String code);
}
