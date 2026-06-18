package com.warehouse.wms.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class InventoryLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;
    private String sku;
    private String action;
    private Integer quantityChanged;
    private LocalDateTime timestamp;
    private String performedBy; // Numele utilizatorului logat
}