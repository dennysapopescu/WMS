package com.warehouse.wms.api;

import com.warehouse.wms.dto.product.ProductRequest;
import com.warehouse.wms.dto.product.ProductResponse;
import com.warehouse.wms.dto.product.ProductTransferRequest;
import com.warehouse.wms.dto.product.StockAdjustmentRequest;
import com.warehouse.wms.service.ProductService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public Page<ProductResponse> findAll(
            @RequestParam(required = false) String query,
            @ParameterObject Pageable pageable
    ) {
        return productService.search(query, pageable);
    }

    @GetMapping("/all")
    public List<ProductResponse> findAllList() {
        return productService.findAll();
    }

    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable Long id) {
        return productService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody ProductRequest request
    ) {
        ProductResponse created = productService.create(request);

        URI uri = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity
                .created(uri)
                .body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ProductResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        return productService.update(id, request);
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ProductResponse adjustStock(
            @PathVariable Long id,
            @Valid @RequestBody StockAdjustmentRequest request
    ) {
        return productService.adjustStock(id, request);
    }

    @PostMapping("/{id}/reduce")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ProductResponse reduceQuantity(
            @PathVariable Long id
    ) {
        return productService.reduceQuantity(id);
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ProductResponse transfer(
            @Valid @RequestBody ProductTransferRequest request
    ) {
        return productService.transfer(request);
    }

    @PostMapping(
            value = "/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, Object>> importCsv(
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        return ResponseEntity.ok(
                productService.importCsv(file)
        );
    }

    @GetMapping("/template")
    public void downloadTemplate(
            HttpServletResponse response
    ) throws IOException {

        response.setContentType("text/csv");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=wms_import_template.csv"
        );

        response.getWriter().write(
                "Name,SKU,Quantity,Price,LocationCode\n"
        );

        response.getWriter().write(
                "Example Product,SKU123,10,100.0,A-01-01\n"
        );

        response.getWriter().flush();

    }

    @GetMapping("/export-pdf")
    public void exportPdf(
            HttpServletResponse response
    ) throws IOException {

        productService.exportPdf(response);
    }

    @GetMapping(
            value = "/qr/{sku}",
            produces = MediaType.IMAGE_PNG_VALUE
    )
    public byte[] getProductQr(
            @PathVariable String sku
    ) {
        return productService.generateQr(sku);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}