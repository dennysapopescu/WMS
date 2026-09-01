package com.warehouse.wms.api;

import com.warehouse.wms.dto.location.LocationRequest;
import com.warehouse.wms.dto.location.LocationResponse;
import com.warehouse.wms.dto.product.ProductResponse;
import com.warehouse.wms.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping
    public List<LocationResponse> findAll() {
        return locationService.findAll();
    }

    @GetMapping("/{id}")
    public LocationResponse findById(
            @PathVariable Long id
    ) {
        return locationService.findById(id);
    }

    @GetMapping("/{id}/products")
    public List<ProductResponse> getProductsInLocation(
            @PathVariable Long id
    ) {
        return locationService.getProductsInLocation(id);
    }

    @GetMapping(
            value = "/qr/{code}",
            produces = MediaType.IMAGE_PNG_VALUE
    )
    public byte[] getLocationQr(
            @PathVariable String code
    ) {
        return locationService.generateQr(code);
    }

    @GetMapping("/suggestions")
    public LocationResponse suggest(
            @RequestParam int quantity
    ) {
        return locationService.suggest(quantity);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LocationResponse> create(
            @Valid @RequestBody LocationRequest request
    ) {

        LocationResponse created =
                locationService.create(request);

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
    @PreAuthorize("hasRole('ADMIN')")
    public LocationResponse update(
            @PathVariable Long id,
            @Valid @RequestBody LocationRequest request
    ) {
        return locationService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {

        locationService.delete(id);

        return ResponseEntity.noContent().build();
    }
}