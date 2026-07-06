# 📦 Warehouse Management System (WMS)

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-8-blue)

A full-stack warehouse management system that digitizes core logistics operations — inventory tracking, inbound/outbound flows, and storage optimization — replacing manual, error-prone processes with a real-time, role-based web application.

> Built as a solo project to explore how enterprise logistics software actually works under the hood: concurrency-safe stock updates, role-based security, and a slotting algorithm that mimics real warehouse decision-making.

## 🎥 Demo


<table>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/0246383d-2f81-4cfd-a347-08f2c2535444" width="300" /></td>
    <td><img src="https://github.com/user-attachments/assets/22418b17-0c25-4317-ba6a-b50cd658e2cc" width="300" /></td>
    <td><img src="https://github.com/user-attachments/assets/8acbca33-6b84-465a-b124-a21183b21d8a" width="300" /></td>
  </tr>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/80db957d-8ac8-4735-9c22-f06530accb6f" width="300" /></td>
    <td><img src="https://github.com/user-attachments/assets/5e49729b-df4e-4916-ba65-56a5d1adb735" width="300" /></td>
    <td><img src="https://github.com/user-attachments/assets/bc790d66-db8b-43fa-8650-346463756a71" width="300" /></td>
  </tr>
</table>


🔗 **[Live demo](#)** *(i will add once deployed — e.g. Render + Railway MySQL)*

## Why this project

Most portfolio CRUD apps manage a single entity with basic forms. This one models a real operational problem: multiple concurrent users (Admin/Operator/Viewer), stock that must stay consistent under concurrent updates, and a placement algorithm that has to reason about warehouse capacity — not just "insert row into table."

## Key Features

| Feature | What it does |
|---|---|
| **Inventory Management** | Full product & stock-level tracking |
| **Inbound / Outbound Operations** | Registers goods movement in and out of the warehouse |
| **Role-Based Access Control** | Spring Security — Admin / Operator / Viewer, each with scoped permissions |
| **Smart Slotting Algorithm** | Suggests optimal storage locations based on available capacity |
| **QR Code Integration** | Generates & scans QR codes (ZXing) for product identification |
| **Audit & Traceability** | Every stock movement logged with timestamp + acting user |
| **PDF Reporting** | Exportable inventory reports (iText) |
| **CSV Import/Export** | Bulk data transfer (OpenCSV) |
| **Digital Twin Visualization** | Interactive warehouse map reflecting real-time stock state |

## Architecture

```
  Inbound Goods → [Smart Slotting Engine] → Storage Location
                                                    ↓
  Viewer/Operator/Admin ← [Spring Security RBAC] ← Digital Twin Map
                                                    ↓
                        Outbound Goods ← Stock Update (Audit Logged)
```

**Pattern:** MVC (Model–View–Controller)
- **Model** — JPA entities + business logic (stock rules, slotting, audit)
- **View** — Thymeleaf templates (server-rendered)
- **Controller** — Request handling & flow orchestration

## Tech Stack

- **Backend:** Java 17, Spring Boot, Spring Data JPA
- **Security:** Spring Security (role-based)
- **Database:** MySQL
- **Frontend:** Thymeleaf, HTML, CSS, JavaScript
- **Libraries:** ZXing (QR codes), iText (PDF export), OpenCSV (CSV I/O)

## Getting Started

```bash
git clone https://github.com/dennysapopescu/PopescuDennysa_WMS.git
```

1. Open in IntelliJ IDEA as a Maven project
2. Create a MySQL database and update `application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/your_database
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   ```
3. Run the `@SpringBootApplication` main class
4. Visit `http://localhost:8080`

**Test accounts** 
Admin: username admin, password admin123
Operator: username operator, password operator123
Viewer: username viewer, password viewer123

| Role | Username | Password |
|---|---|---|
| Admin | admin | admin123 |
| Operator | operator | operator123 |
| Viewer | viewer | viewer123 |

## User Roles

| Role | Access |
|---|---|
| Admin | Full access to all features |
| Operator | Warehouse operations (inbound/outbound, slotting) |
| Viewer | Read-only |

## Roadmap

- [ ] Mobile companion app
- [ ] Advanced analytics dashboard
- [ ] Physical barcode scanner integration
- [ ] Split into microservices (inventory / auth / reporting)

## Author

**Dennysa-Maria Popescu**

<p align="left">
  <a href="https://www.linkedin.com/in/dennysa-popescu-4938a9263">
    <img src="https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white" alt="LinkedIn Profile" />
  </a>
  <a href="https://github.com/dennysapopescu">
    <img src="https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white" alt="GitHub Profile" />
  </a>
</p>
