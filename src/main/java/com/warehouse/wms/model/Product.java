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

    // Optimistic locking: previne suprascrierea stocului când doi operatori
    // actualizează același produs simultan (ex: 2 picking-uri validate în paralel)
    @Version
    private Long version;
}