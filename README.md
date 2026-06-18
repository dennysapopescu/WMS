# Warehouse Management System (WMS)

## Overview

This project is a **Warehouse Management System (WMS)** designed to streamline and optimize warehouse operations. It provides a modern web-based solution for managing inventory, tracking product movement, and improving overall logistics efficiency.

The application focuses on reducing manual processes, minimizing human errors, and offering real-time visibility over stock and warehouse activities.

---

## Key Features

* **Inventory Management** – Manage products and stock levels efficiently
* **Inbound & Outbound Operations** – Track goods entering and leaving the warehouse
* **Authentication & Role-Based Access Control** – Secure system with different user roles (Admin, Operator, Viewer)
* **Smart Slotting Algorithm** – Suggests optimal storage locations based on warehouse capacity
* **QR Code Integration** – Generate and scan QR codes for product identification
* **Audit & Traceability** – Track all stock movements with timestamps and user actions
* **PDF Reporting** – Export inventory reports using PDF format
* **CSV Import/Export** – Easy data transfer between systems
* **Warehouse Visualization (Digital Twin)** – Interactive warehouse map for real-time monitoring

---

## Tech Stack

* **Backend:** Java, Spring Boot
* **Security:** Spring Security
* **Database:** MySQL, Spring Data JPA
* **Frontend:** Thymeleaf, HTML, CSS, JavaScript
* **Libraries:**

  * ZXing (QR Code generation)
  * iText (PDF export)
  * OpenCSV (CSV processing)

---

## Architecture

The application follows a **client-server architecture** using the **MVC (Model-View-Controller)** pattern:

* **Model:** Handles data and business logic
* **View:** Built with Thymeleaf templates
* **Controller:** Manages user requests and application flow

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/dennysapopescu/PopescuDennysa_WMS.git
```

### 2. Open in IntelliJ IDEA

* Import as a Maven/Gradle project

### 3. Configure the database

* Create a MySQL database
* Update `application.properties` with your credentials:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/your_database
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 4. Run the application

* Run the main class (`@SpringBootApplication`)
* Access the app at: `http://localhost:8080`

---

## User Roles

* **Admin** – Full access to all features
* **Operator** – Handles warehouse operations
* **Viewer** – Read-only access

---

## Use Cases

* Warehouse inventory tracking
* Logistics process optimization
* Stock monitoring and reporting
* Digital transformation of warehouse operations

---

## Future Improvements

* Mobile app integration
* Advanced analytics & dashboards
* Barcode scanner device integration
* Microservices architecture

---

## Author

**Dennysa-Maria Popescu**

---

## Notes

This project demonstrates practical implementation of modern backend technologies and real-world logistics concepts, making it suitable for both learning purposes and real-world adaptation.
