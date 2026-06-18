package com.warehouse.wms.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String fullName;

    private String role; // ROLE_ADMIN, ROLE_OPERATOR, ROLE_VIEWER

    private String shift;

    private boolean active = true;
    private LocalDateTime lastLogin;
}