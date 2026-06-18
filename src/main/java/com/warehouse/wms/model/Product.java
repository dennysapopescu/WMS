package com.warehouse.wms.model;

import jakarta.persistence.*;
import lombok.*;

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
    private String sku; // Codul unic (ex: LAPTOP-001)

    private String name;
    private Integer quantity;
    private Double price;    // per unitate

    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;
}