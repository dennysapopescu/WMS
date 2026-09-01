package com.warehouse.wms.service;

import com.warehouse.wms.dto.product.ProductRequest;
import com.warehouse.wms.dto.product.ProductTransferRequest;
import com.warehouse.wms.dto.product.StockAdjustmentRequest;
import com.warehouse.wms.exception.BusinessRuleException;
import com.warehouse.wms.model.Location;
import com.warehouse.wms.model.Product;
import com.warehouse.wms.repository.LocationRepository;
import com.warehouse.wms.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private LocationService locationService;

    @Mock
    private AuditService auditService;

    @Mock
    private PdfService pdfService;

    @Mock
    private QrCodeService qrCodeService;

    @InjectMocks
    private ProductService productService;

    private Location location;

    @BeforeEach
    void setUp() {

        location = new Location();

        location.setId(1L);
        location.setCode("A-01");
        location.setMaxCapacity(10);
        location.setCurrentOccupancy(8);
    }

    @Test
    void createRejectsStockThatDoesNotFit() {

        when(productRepository.findBySku("SKU-1"))
                .thenReturn(Optional.empty());

        when(locationService.requiredLocation(1L))
                .thenReturn(location);

        when(locationService.availableCapacity(location))
                .thenReturn(2);

        assertThrows(
                BusinessRuleException.class,
                () -> productService.create(
                        new ProductRequest(
                                "SKU-1",
                                "Keyboard",
                                3,
                                new BigDecimal("100.00"),
                                1L
                        )
                )
        );

        verify(
                productRepository,
                never()
        ).save(any());
    }

    @Test
    void createUpdatesLocationAndWritesAudit() {

        when(productRepository.findBySku("SKU-1"))
                .thenReturn(Optional.empty());

        when(locationService.requiredLocation(1L))
                .thenReturn(location);

        when(locationService.availableCapacity(location))
                .thenReturn(5);

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> {

                    Product product =
                            invocation.getArgument(0);

                    product.setId(5L);

                    return product;
                });

        var result =
                productService.create(
                        new ProductRequest(
                                "SKU-1",
                                "Keyboard",
                                2,
                                new BigDecimal("100.00"),
                                1L
                        )
                );

        assertEquals(
                2,
                result.quantity()
        );

        assertEquals(
                10,
                location.getCurrentOccupancy()
        );

        verify(auditService)
                .inventoryEvent(
                        "Keyboard",
                        "SKU-1",
                        "ADD",
                        2
                );

    }

    @Test
    void updateSameLocationChangesOccupancyByDelta() {

        Product product =
                new Product();

        product.setId(5L);
        product.setSku("SKU-1");
        product.setName("Keyboard");
        product.setQuantity(5);
        product.setPrice(
                new BigDecimal("100.00")
        );
        product.setLocation(location);

        when(productRepository.findById(5L))
                .thenReturn(Optional.of(product));

        when(productRepository.findBySku("SKU-1"))
                .thenReturn(Optional.of(product));

        when(locationService.requiredLocation(1L))
                .thenReturn(location);

        when(locationService.availableCapacity(location))
                .thenReturn(2);

        productService.update(
                5L,
                new ProductRequest(
                        "SKU-1",
                        "Keyboard",
                        7,
                        new BigDecimal("100.00"),
                        1L
                )
        );

        assertEquals(
                7,
                product.getQuantity()
        );

        assertEquals(
                10,
                location.getCurrentOccupancy()
        );
    }

    @Test
    void adjustStockUpdatesProductAndOccupancy() {

        Product product =
                new Product();

        product.setId(5L);
        product.setSku("SKU-1");
        product.setName("Keyboard");
        product.setQuantity(5);
        product.setLocation(location);

        when(productRepository.findById(5L))
                .thenReturn(Optional.of(product));

        productService.adjustStock(
                5L,
                new StockAdjustmentRequest(
                        -2,
                        "CYCLE_COUNT"
                )
        );

        assertEquals(
                3,
                product.getQuantity()
        );

        assertEquals(
                6,
                location.getCurrentOccupancy()
        );

        verify(auditService)
                .inventoryEvent(
                        "Keyboard",
                        "SKU-1",
                        "CYCLE_COUNT",
                        -2
                );
    }

    @Test
    void adjustStockRejectsNegativeResult() {

        Product product =
                new Product();

        product.setId(5L);
        product.setSku("SKU-1");
        product.setQuantity(2);
        product.setLocation(location);

        when(productRepository.findById(5L))
                .thenReturn(Optional.of(product));

        assertThrows(
                BusinessRuleException.class,
                () -> productService.adjustStock(
                        5L,
                        new StockAdjustmentRequest(
                                -3,
                                "CYCLE_COUNT"
                        )
                )
        );
    }

    @Test
    void transferMovesQuantityBetweenLocations() {

        Location target =
                new Location();

        target.setId(2L);
        target.setCode("B-01");
        target.setMaxCapacity(20);
        target.setCurrentOccupancy(3);

        Product product =
                new Product();

        product.setId(5L);
        product.setSku("SKU-1");
        product.setName("Keyboard");
        product.setQuantity(4);
        product.setLocation(location);

        when(productRepository.findById(5L))
                .thenReturn(Optional.of(product));

        when(locationService.requiredLocation(2L))
                .thenReturn(target);

        when(locationService.availableCapacity(target))
                .thenReturn(17);

        when(productRepository.save(product))
                .thenReturn(product);

        productService.transfer(
                new ProductTransferRequest(
                        5L,
                        2L
                )
        );

        assertEquals(
                4,
                location.getCurrentOccupancy()
        );

        assertEquals(
                7,
                target.getCurrentOccupancy()
        );

        assertEquals(
                target,
                product.getLocation()
        );
    }

    @Test
    void transferRejectsInsufficientCapacity() {

        Location target =
                new Location();

        target.setId(2L);
        target.setCode("B-01");
        target.setMaxCapacity(5);
        target.setCurrentOccupancy(5);

        Product product =
                new Product();

        product.setId(5L);
        product.setSku("SKU-1");
        product.setQuantity(4);
        product.setLocation(location);

        when(productRepository.findById(5L))
                .thenReturn(Optional.of(product));

        when(locationService.requiredLocation(2L))
                .thenReturn(target);

        when(locationService.availableCapacity(target))
                .thenReturn(0);

        assertThrows(
                BusinessRuleException.class,
                () -> productService.transfer(
                        new ProductTransferRequest(
                                5L,
                                2L
                        )
                )
        );

        assertEquals(
                location,
                product.getLocation()
        );
    }
}