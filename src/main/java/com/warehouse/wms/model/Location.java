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
    private String code; // e.g. R-01-A (Rack 1, Position A)

    private String description;

    private Integer maxCapacity;

    private Integer currentOccupancy = 0; // Current units stored


    @Version
    private Long version;
}
