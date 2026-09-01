package com.warehouse.wms.config;

import com.warehouse.wms.model.InventoryLog;
import com.warehouse.wms.model.Location;
import com.warehouse.wms.model.OutboundOrder;
import com.warehouse.wms.model.Product;
import com.warehouse.wms.repository.InventoryLogRepository;
import com.warehouse.wms.repository.LocationRepository;
import com.warehouse.wms.repository.OrderRepository;
import com.warehouse.wms.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Profile("docker")
public class DemoDataInitializer implements CommandLineRunner {

    private final LocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final InventoryLogRepository inventoryLogRepository;

    public DemoDataInitializer(
            LocationRepository locationRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            InventoryLogRepository inventoryLogRepository
    ) {
        this.locationRepository = locationRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.inventoryLogRepository = inventoryLogRepository;
    }

    @Override
    public void run(String... args) {

        // Do not insert demo data if the database already contains products.
        if (productRepository.count() > 0) {
            return;
        }

        System.out.println("Initializing WMS demo data...");

        List<Location> locations = createLocations();

        createProducts(locations);
        createOrders(locations);
        createInventoryLogs();

        System.out.println("WMS demo data initialized successfully.");
    }

    private List<Location> createLocations() {

        Location r01a = createLocation(
                "R-01-A",
                "Rack 1 - Position A",
                100,
                75
        );

        Location r01b = createLocation(
                "R-01-B",
                "Rack 1 - Position B",
                100,
                42
        );

        Location r02a = createLocation(
                "R-02-A",
                "Rack 2 - Position A",
                150,
                120
        );

        Location r02b = createLocation(
                "R-02-B",
                "Rack 2 - Position B",
                150,
                65
        );

        Location r03a = createLocation(
                "R-03-A",
                "Rack 3 - Position A",
                200,
                180
        );

        Location r03b = createLocation(
                "R-03-B",
                "Rack 3 - Position B",
                200,
                35
        );

        Location r04a = createLocation(
                "R-04-A",
                "Rack 4 - Position A",
                100,
                18
        );

        Location r04b = createLocation(
                "R-04-B",
                "Rack 4 - Position B",
                100,
                90
        );

        Location r05a = createLocation(
                "R-05-A",
                "Rack 5 - Position A",
                250,
                210
        );

        Location r05b = createLocation(
                "R-05-B",
                "Rack 5 - Position B",
                250,
                55
        );

        return locationRepository.saveAll(List.of(
                r01a,
                r01b,
                r02a,
                r02b,
                r03a,
                r03b,
                r04a,
                r04b,
                r05a,
                r05b
        ));
    }

    private Location createLocation(
            String code,
            String description,
            int maxCapacity,
            int currentOccupancy
    ) {
        Location location = new Location();
        location.setCode(code);
        location.setDescription(description);
        location.setMaxCapacity(maxCapacity);
        location.setCurrentOccupancy(currentOccupancy);
        return location;
    }

    private void createProducts(List<Location> locations) {

        Product laptop = createProduct(
                "LAPTOP-001",
                "Business Laptop",
                25,
                "899.99",
                locations.get(0)
        );

        Product monitor = createProduct(
                "MONITOR-002",
                "24-inch Full HD Monitor",
                42,
                "249.99",
                locations.get(1)
        );

        Product keyboard = createProduct(
                "KEYBOARD-003",
                "Mechanical Keyboard",
                78,
                "89.99",
                locations.get(2)
        );

        Product mouse = createProduct(
                "MOUSE-004",
                "Wireless Mouse",
                120,
                "39.99",
                locations.get(3)
        );

        Product headset = createProduct(
                "HEADSET-005",
                "USB Headset",
                15,
                "69.99",
                locations.get(4)
        );

        Product webcam = createProduct(
                "WEBCAM-006",
                "Full HD Webcam",
                8,
                "79.99",
                locations.get(5)
        );

        Product docking = createProduct(
                "DOCK-007",
                "USB-C Docking Station",
                32,
                "159.99",
                locations.get(6)
        );

        Product chair = createProduct(
                "CHAIR-008",
                "Ergonomic Office Chair",
                18,
                "329.99",
                locations.get(7)
        );

        Product desk = createProduct(
                "DESK-009",
                "Adjustable Standing Desk",
                12,
                "499.99",
                locations.get(8)
        );

        Product cable = createProduct(
                "CABLE-010",
                "USB-C Cable 2m",
                150,
                "14.99",
                locations.get(9)
        );

        productRepository.saveAll(List.of(
                laptop,
                monitor,
                keyboard,
                mouse,
                headset,
                webcam,
                docking,
                chair,
                desk,
                cable
        ));
    }

    private Product createProduct(
            String sku,
            String name,
            int quantity,
            String price,
            Location location
    ) {
        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        product.setQuantity(quantity);
        product.setPrice(new BigDecimal(price));
        product.setLocation(location);
        return product;
    }

    private void createOrders(List<Location> locations) {

        OutboundOrder order1 = OutboundOrder.builder()
                .sku("LAPTOP-001")
                .requestedQuantity(5)
                .pickedQuantity(0)
                .status("PENDING")
                .createdAt(LocalDateTime.now().minusHours(2))
                .suggestedLocation(locations.get(0))
                .build();

        OutboundOrder order2 = OutboundOrder.builder()
                .sku("MONITOR-002")
                .requestedQuantity(10)
                .pickedQuantity(0)
                .status("PENDING")
                .createdAt(LocalDateTime.now().minusHours(4))
                .suggestedLocation(locations.get(1))
                .build();

        OutboundOrder order3 = OutboundOrder.builder()
                .sku("KEYBOARD-003")
                .requestedQuantity(15)
                .pickedQuantity(15)
                .status("COMPLETED")
                .createdAt(LocalDateTime.now().minusHours(6))
                .suggestedLocation(locations.get(2))
                .assignedTo("operator")
                .assignedAt(LocalDateTime.now().minusHours(5))
                .build();

        OutboundOrder order4 = OutboundOrder.builder()
                .sku("MOUSE-004")
                .requestedQuantity(20)
                .pickedQuantity(0)
                .status("PENDING")
                .createdAt(LocalDateTime.now().minusHours(1))
                .suggestedLocation(locations.get(3))
                .build();

        OutboundOrder order5 = OutboundOrder.builder()
                .sku("HEADSET-005")
                .requestedQuantity(8)
                .pickedQuantity(8)
                .status("COMPLETED")
                .createdAt(LocalDateTime.now().minusHours(8))
                .suggestedLocation(locations.get(4))
                .assignedTo("operator")
                .assignedAt(LocalDateTime.now().minusHours(7))
                .build();

        orderRepository.saveAll(List.of(
                order1,
                order2,
                order3,
                order4,
                order5
        ));
    }

    private void createInventoryLogs() {

        InventoryLog log1 = createLog(
                "Business Laptop",
                "LAPTOP-001",
                "STOCK_IN",
                25,
                "admin",
                10
        );

        InventoryLog log2 = createLog(
                "24-inch Full HD Monitor",
                "MONITOR-002",
                "STOCK_IN",
                42,
                "admin",
                8
        );

        InventoryLog log3 = createLog(
                "Mechanical Keyboard",
                "KEYBOARD-003",
                "STOCK_IN",
                78,
                "operator",
                6
        );

        InventoryLog log4 = createLog(
                "Wireless Mouse",
                "MOUSE-004",
                "STOCK_OUT",
                -20,
                "operator",
                2
        );

        InventoryLog log5 = createLog(
                "USB Headset",
                "HEADSET-005",
                "STOCK_OUT",
                -8,
                "operator",
                1
        );

        InventoryLog log6 = createLog(
                "Full HD Webcam",
                "WEBCAM-006",
                "STOCK_IN",
                8,
                "admin",
                12
        );

        inventoryLogRepository.saveAll(List.of(
                log1,
                log2,
                log3,
                log4,
                log5,
                log6
        ));
    }

    private InventoryLog createLog(
            String productName,
            String sku,
            String action,
            int quantityChanged,
            String performedBy,
            int hoursAgo
    ) {
        InventoryLog log = new InventoryLog();
        log.setProductName(productName);
        log.setSku(sku);
        log.setAction(action);
        log.setQuantityChanged(quantityChanged);
        log.setTimestamp(LocalDateTime.now().minusHours(hoursAgo));
        log.setPerformedBy(performedBy);
        return log;
    }
}
