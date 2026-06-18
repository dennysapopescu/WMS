package com.warehouse.wms.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "outbound_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder 
public class OutboundOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sku;
    private Integer requestedQuantity;

    @Builder.Default
    private Integer pickedQuantity = 0;

    @Builder.Default
    private String status = "PENDING"; // "PENDING", "COMPLETED"

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location suggestedLocation;

    private String assignedTo; // Numele operatorului care a validat
    private LocalDateTime assignedAt;
}