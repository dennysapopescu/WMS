package com.warehouse.wms.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.warehouse.wms.dto.product.ProductRequest;
import com.warehouse.wms.dto.product.ProductResponse;
import com.warehouse.wms.dto.product.ProductTransferRequest;
import com.warehouse.wms.dto.product.StockAdjustmentRequest;
import com.warehouse.wms.exception.BusinessRuleException;
import com.warehouse.wms.exception.ResourceNotFoundException;
import com.warehouse.wms.model.Location;
import com.warehouse.wms.model.Product;
import com.warehouse.wms.repository.LocationRepository;
import com.warehouse.wms.repository.ProductRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final LocationService locationService;
    private final LocationRepository locationRepository;
    private final AuditService auditService;
    private final PdfService pdfService;
    private final QrCodeService qrCodeService;

    public List<ProductResponse> findAll() {
        return productRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Page<ProductResponse> search(
            String query,
            Pageable pageable
    ) {
        if (query == null || query.isBlank()) {
            return productRepository
                    .findAll(pageable)
                    .map(this::toResponse);
        }

        String normalized = query.trim();

        return productRepository
                .findBySkuContainingIgnoreCaseOrNameContainingIgnoreCase(
                        normalized,
                        normalized,
                        pageable
                )
                .map(this::toResponse);
    }

    public ProductResponse findById(Long id) {
        return toResponse(requiredProduct(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {

        String sku = request.sku().trim();

        productRepository.findBySku(sku)
                .ifPresent(existing -> {
                    throw new BusinessRuleException(
                            "SKU already exists: " + sku
                    );
                });

        Location location =
                locationService.requiredLocation(request.locationId());

        assertCapacity(
                location,
                request.quantity()
        );

        Product product = new Product();

        apply(
                product,
                request,
                location
        );

        location.setCurrentOccupancy(
                location.getCurrentOccupancy()
                        + request.quantity()
        );

        Product saved =
                productRepository.save(product);

        auditService.inventoryEvent(
                saved.getName(),
                saved.getSku(),
                "ADD",
                request.quantity()
        );


        return toResponse(saved);
    }

    @Transactional
    public ProductResponse update(
            Long id,
            ProductRequest request
    ) {

        Product product = requiredProduct(id);

        String sku = request.sku().trim();

        productRepository.findBySku(sku)
                .filter(existing ->
                        !existing.getId().equals(id)
                )
                .ifPresent(existing -> {
                    throw new BusinessRuleException(
                            "SKU already exists: " + sku
                    );
                });

        Location previous =
                product.getLocation();

        Location target =
                locationService.requiredLocation(
                        request.locationId()
                );

        int oldQuantity =
                product.getQuantity() == null
                        ? 0
                        : product.getQuantity();

        int newQuantity =
                request.quantity();

        /*
         * Same location:
         * only occupancy delta needs to be applied.
         */
        if (previous != null
                && previous.getId().equals(target.getId())) {

            int delta =
                    newQuantity - oldQuantity;

            assertCapacity(
                    target,
                    delta
            );

            target.setCurrentOccupancy(
                    target.getCurrentOccupancy()
                            + delta
            );

        } else {

            /*
             * Different location:
             * remove old quantity from old location
             * and add new quantity to target.
             */
            assertCapacity(
                    target,
                    newQuantity
            );

            if (previous != null) {
                previous.setCurrentOccupancy(
                        Math.max(
                                0,
                                previous.getCurrentOccupancy()
                                        - oldQuantity
                        )
                );
            }

            target.setCurrentOccupancy(
                    target.getCurrentOccupancy()
                            + newQuantity
            );
        }

        apply(
                product,
                request,
                target
        );

        auditService.inventoryEvent(
                product.getName(),
                product.getSku(),
                "UPDATE",
                newQuantity - oldQuantity
        );

        return toResponse(product);
    }

    @Transactional
    public ProductResponse adjustStock(
            Long id,
            StockAdjustmentRequest request
    ) {

        Product product =
                requiredProduct(id);

        int currentQuantity =
                product.getQuantity() == null
                        ? 0
                        : product.getQuantity();

        int newQuantity =
                currentQuantity
                        + request.quantityDelta();

        if (newQuantity < 0) {
            throw new BusinessRuleException(
                    "Stock cannot become negative"
            );
        }

        if (request.quantityDelta() > 0) {
            assertCapacity(
                    product.getLocation(),
                    request.quantityDelta()
            );
        }

        product.setQuantity(newQuantity);

        if (product.getLocation() != null) {

            product.getLocation()
                    .setCurrentOccupancy(
                            Math.max(
                                    0,
                                    product.getLocation()
                                            .getCurrentOccupancy()
                                            + request.quantityDelta()
                            )
                    );
        }

        auditService.inventoryEvent(
                product.getName(),
                product.getSku(),
                request.reason(),
                request.quantityDelta()
        );

        return toResponse(product);
    }

    @Transactional
    public ProductResponse reduceQuantity(Long id) {

        Product product =
                requiredProduct(id);

        int quantity =
                product.getQuantity() == null
                        ? 0
                        : product.getQuantity();

        if (quantity <= 0) {
            throw new BusinessRuleException(
                    "Stock is already 0!"
            );
        }

        product.setQuantity(
                quantity - 1
        );

        if (product.getLocation() != null) {

            product.getLocation()
                    .setCurrentOccupancy(
                            Math.max(
                                    0,
                                    product.getLocation()
                                            .getCurrentOccupancy() - 1
                            )
                    );
        }

        auditService.inventoryEvent(
                product.getName(),
                product.getSku(),
                "STOCK_REDUCTION",
                -1
        );

        return toResponse(product);
    }

    @Transactional
    public ProductResponse transfer(
            ProductTransferRequest request
    ) {

        Product product =
                requiredProduct(
                        request.productId()
                );

        Location oldLocation =
                product.getLocation();

        Location newLocation =
                locationService.requiredLocation(
                        request.newLocationId()
                );

        int quantity =
                product.getQuantity() == null
                        ? 0
                        : product.getQuantity();

        /*
         * Nothing to do if source == target.
         */
        if (oldLocation != null
                && oldLocation.getId()
                .equals(newLocation.getId())) {

            return toResponse(product);
        }

        assertCapacity(
                newLocation,
                quantity
        );

        if (oldLocation != null) {

            oldLocation.setCurrentOccupancy(
                    Math.max(
                            0,
                            oldLocation.getCurrentOccupancy()
                                    - quantity
                    )
            );
        }

        newLocation.setCurrentOccupancy(
                newLocation.getCurrentOccupancy()
                        + quantity
        );

        product.setLocation(
                newLocation
        );

        Product saved =
                productRepository.save(product);

        auditService.inventoryEvent(
                product.getName(),
                product.getSku(),
                "TRANSFER to "
                        + newLocation.getCode(),
                quantity
        );

        return toResponse(saved);
    }


    @Transactional
    public Map<String, Object> importCsv(
            MultipartFile file
    ) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException(
                    "CSV file is empty!"
            );
        }

        int imported = 0;
        int redirected = 0;
        int skipped = 0;

        List<Location> locations =
                locationRepository.findAll();

        try (
                Reader reader =
                        new InputStreamReader(
                                file.getInputStream(),
                                StandardCharsets.UTF_8
                        );

                CSVReader csvReader =
                        new CSVReader(reader)
        ) {

            String[] row =
                    csvReader.readNext();

            if (row == null) {
                throw new BusinessRuleException(
                        "CSV file does not contain a header"
                );
            }

            while ((row = csvReader.readNext()) != null) {

                if (row.length < 5) {
                    skipped++;
                    continue;
                }

                try {

                    String name =
                            row[0].trim();

                    String sku =
                            row[1].trim();

                    int quantity =
                            Integer.parseInt(
                                    row[2].trim()
                            );

                    BigDecimal price =
                            new BigDecimal(
                                    row[3].trim()
                            ).setScale(2);

                    String requestedCode =
                            row[4].trim();

                    if (name.isBlank()
                            || sku.isBlank()
                            || quantity <= 0
                            || price.signum() < 0) {

                        skipped++;
                        continue;
                    }

                    Product existing =
                            productRepository
                                    .findBySku(sku)
                                    .orElse(null);

                    Location target =
                            findLocation(
                                    locations,
                                    requestedCode
                            );

                    int existingQuantity =
                            existing == null
                                    || existing.getQuantity() == null
                                    ? 0
                                    : existing.getQuantity();

                    /*
                     * Existing product already in requested
                     * location.
                     */
                    if (existing != null
                            && existing.getLocation() != null
                            && target != null
                            && existing.getLocation()
                            .getId()
                            .equals(target.getId())) {

                        assertCapacity(
                                target,
                                quantity
                        );

                    } else {

                        int requiredCapacity =
                                existing == null
                                        ? quantity
                                        : existingQuantity
                                        + quantity;

                        if (target == null
                                || locationService
                                .availableCapacity(target)
                                < requiredCapacity) {

                            Location alternative =
                                    locations.stream()
                                            .filter(location ->
                                                    locationService
                                                            .availableCapacity(
                                                                    location
                                                            )
                                                            >= requiredCapacity
                                            )
                                            .findFirst()
                                            .orElse(null);

                            if (alternative == null) {
                                skipped++;
                                continue;
                            }

                            target = alternative;
                            redirected++;
                        }
                    }

                    if (existing == null) {

                        Product product =
                                new Product();

                        product.setName(name);
                        product.setSku(sku);
                        product.setQuantity(quantity);
                        product.setPrice(price);
                        product.setLocation(target);

                        productRepository.save(
                                product
                        );

                        target.setCurrentOccupancy(
                                target.getCurrentOccupancy()
                                        + quantity
                        );

                    } else {

                        Location oldLocation =
                                existing.getLocation();

                        int newQuantity =
                                existingQuantity
                                        + quantity;

                        if (oldLocation != null
                                && !oldLocation.getId()
                                .equals(target.getId())) {

                            oldLocation.setCurrentOccupancy(
                                    Math.max(
                                            0,
                                            oldLocation
                                                    .getCurrentOccupancy()
                                                    - existingQuantity
                                    )
                            );
                        }

                        if (oldLocation == null
                                || !oldLocation.getId()
                                .equals(target.getId())) {

                            target.setCurrentOccupancy(
                                    target.getCurrentOccupancy()
                                            + newQuantity
                            );

                        } else {

                            target.setCurrentOccupancy(
                                    target.getCurrentOccupancy()
                                            + quantity
                            );
                        }

                        existing.setName(name);
                        existing.setQuantity(newQuantity);
                        existing.setPrice(price);
                        existing.setLocation(target);

                        productRepository.save(
                                existing
                        );
                    }

                    auditService.inventoryEvent(
                            name,
                            sku,
                            target.getCode()
                                    .equalsIgnoreCase(
                                            requestedCode
                                    )
                                    ? "IMPORT"
                                    : "IMPORT (REDIRECTED)",
                            quantity
                    );

                    imported++;

                } catch (NumberFormatException ex) {
                    skipped++;
                }
            }

        } catch (CsvValidationException ex) {

            throw new BusinessRuleException(
                    "Invalid CSV: " + ex.getMessage()
            );
        }

        return Map.of(
                "imported",
                imported,

                "redirected",
                redirected,

                "skipped",
                skipped,

                "message",
                "Successfully processed: "
                        + imported
                        + " products ("
                        + redirected
                        + " redirected, "
                        + skipped
                        + " ignored)."
        );
    }

    public void exportPdf(
            HttpServletResponse response
    ) throws IOException {

        response.setContentType(
                "application/pdf"
        );

        response.setHeader(
                "Content-Disposition",
                "attachment; filename=wms_inventory_report.pdf"
        );

        pdfService.export(
                response,
                productRepository.findAll()
        );
    }

    public byte[] generateQr(String sku) {
        return qrCodeService.generateQRCode(
                "Product SKU: " + sku.trim()
        );
    }

    @Transactional
    public void delete(Long id) {

        Product product =
                requiredProduct(id);

        int quantity =
                product.getQuantity() == null
                        ? 0
                        : product.getQuantity();

        if (product.getLocation() != null) {

            product.getLocation()
                    .setCurrentOccupancy(
                            Math.max(
                                    0,
                                    product.getLocation()
                                            .getCurrentOccupancy()
                                            - quantity
                            )
                    );
        }

        auditService.inventoryEvent(
                product.getName(),
                product.getSku(),
                "DELETE",
                -quantity
        );

        productRepository.delete(
                product
        );
    }


    Product requiredProduct(Long id) {

        return productRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product",
                                id
                        )
                );
    }

    private Location findLocation(
            List<Location> locations,
            String code
    ) {

        return locations.stream()
                .filter(location ->
                        location.getCode()
                                .equalsIgnoreCase(code)
                )
                .findFirst()
                .orElse(null);
    }

    private void assertCapacity(
            Location location,
            int additionalQuantity
    ) {

        if (location == null) {
            throw new BusinessRuleException(
                    "Product has no assigned location"
            );
        }

        /*
         * Negative delta means we are freeing capacity,
         * so there is no capacity constraint.
         */
        if (additionalQuantity <= 0) {
            return;
        }

        if (locationService.availableCapacity(location)
                < additionalQuantity) {

            throw new BusinessRuleException(
                    "Insufficient capacity at location "
                            + location.getCode()
            );
        }
    }

    private void apply(
            Product product,
            ProductRequest request,
            Location location
    ) {

        product.setSku(
                request.sku().trim()
        );

        product.setName(
                request.name().trim()
        );

        product.setQuantity(
                request.quantity()
        );

        product.setPrice(
                request.price().setScale(2)
        );

        product.setLocation(
                location
        );
    }

    ProductResponse toResponse(
            Product product
    ) {

        Location location =
                product.getLocation();

        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getQuantity(),
                product.getPrice(),
                location == null
                        ? null
                        : location.getId(),
                location == null
                        ? "-"
                        : location.getCode(),
                product.getVersion()
        );
    }
}