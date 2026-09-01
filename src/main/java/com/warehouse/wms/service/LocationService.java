package com.warehouse.wms.service;

import com.warehouse.wms.dto.location.LocationRequest;
import com.warehouse.wms.dto.location.LocationResponse;
import com.warehouse.wms.dto.product.ProductResponse;
import com.warehouse.wms.exception.BusinessRuleException;
import com.warehouse.wms.exception.ResourceNotFoundException;
import com.warehouse.wms.model.Location;
import com.warehouse.wms.repository.LocationRepository;
import com.warehouse.wms.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {

    private final LocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final QrCodeService qrCodeService;

    public List<LocationResponse> findAll() {
        return locationRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public LocationResponse findById(Long id) {
        return toResponse(requiredLocation(id));
    }

    public List<ProductResponse> getProductsInLocation(Long id) {

        requiredLocation(id);

        return productRepository.findByLocationId(id)
                .stream()
                .map(this::toProductResponse)
                .toList();
    }

    public byte[] generateQr(String code) {
        return qrCodeService.generateQRCode("LOC:" + code);
    }

    @Transactional
    public LocationResponse create(LocationRequest request) {

        String code = request.code().trim();

        locationRepository.findByCodeIgnoreCase(code)
                .ifPresent(location -> {
                    throw new BusinessRuleException(
                            "Location code already exists: " + code
                    );
                });

        Location location = new Location();

        location.setCode(code);
        location.setDescription(request.description());
        location.setMaxCapacity(request.maxCapacity());
        location.setCurrentOccupancy(0);

        return toResponse(
                locationRepository.save(location)
        );
    }

    @Transactional
    public LocationResponse update(
            Long id,
            LocationRequest request
    ) {

        Location location = requiredLocation(id);

        String code = request.code().trim();

        locationRepository.findByCodeIgnoreCase(code)
                .filter(found ->
                        !found.getId().equals(id)
                )
                .ifPresent(found -> {
                    throw new BusinessRuleException(
                            "Location code already exists: " + code
                    );
                });

        if (request.maxCapacity()
                < location.getCurrentOccupancy()) {

            throw new BusinessRuleException(
                    "Capacity cannot be less than current occupancy"
            );
        }

        location.setCode(code);
        location.setDescription(request.description());
        location.setMaxCapacity(request.maxCapacity());

        return toResponse(location);
    }

    @Transactional
    public void delete(Long id) {

        Location location = requiredLocation(id);

        if (!productRepository.findByLocationId(id).isEmpty()) {
            throw new BusinessRuleException(
                    "Location cannot be deleted while it contains products"
            );
        }

        locationRepository.delete(location);
    }

    public LocationResponse suggest(int quantity) {

        if (quantity <= 0) {
            throw new BusinessRuleException(
                    "Quantity must be positive"
            );
        }

        return locationRepository.findAll()
                .stream()
                .filter(location ->
                        availableCapacity(location) >= quantity
                )
                .sorted(
                        Comparator
                                .comparing(
                                        Location::getCurrentOccupancy
                                )
                                .reversed()
                                .thenComparing(
                                        Location::getCode
                                )
                )
                .findFirst()
                .map(this::toResponse)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Location with sufficient capacity",
                                quantity
                        )
                );
    }

    Location requiredLocation(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Location",
                                id
                        )
                );
    }

    int availableCapacity(Location location) {
        return location.getMaxCapacity()
                - location.getCurrentOccupancy();
    }

    LocationResponse toResponse(Location location) {

        return new LocationResponse(
                location.getId(),
                location.getCode(),
                location.getDescription(),
                location.getMaxCapacity(),
                location.getCurrentOccupancy(),
                availableCapacity(location),
                location.getVersion()
        );
    }

    private ProductResponse toProductResponse(
            com.warehouse.wms.model.Product product
    ) {

        Location location = product.getLocation();

        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getQuantity(),
                product.getPrice(),
                location != null
                        ? location.getId()
                        : null,
                location != null
                        ? location.getCode()
                        : "-",
                product.getVersion()
        );
    }
}
