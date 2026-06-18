package com.warehouse.wms.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "locations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code; // Ex: R-01-A (Raft 1, Poziția A)

    private String description;

    private Integer maxCapacity;

    private Integer currentOccupancy = 0; // Câte unități sunt acum
}