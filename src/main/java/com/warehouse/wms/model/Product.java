package com.warehouse.wms.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String sku; // Unique SKU identifier (e.g., LAPTOP-001)

    private String name;
    private Integer quantity;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;    // Unit price; exact monetary representation

    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;

    // Optimistic locking: prevents stock overwrites when multiple operators
    // update the same product concurrently (e.g., parallel pick confirmations)
    @Version
    private Long version;

}
