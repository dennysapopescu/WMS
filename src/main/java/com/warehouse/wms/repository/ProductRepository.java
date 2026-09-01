package com.warehouse.wms.repository;

import com.warehouse.wms.model.Location;
import com.warehouse.wms.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);
    List<Product> findByLocation(Location location);
    List<Product> findByLocationId(Long locationId);
    Page<Product> findBySkuContainingIgnoreCaseOrNameContainingIgnoreCase(String sku, String name, Pageable pageable);
}
