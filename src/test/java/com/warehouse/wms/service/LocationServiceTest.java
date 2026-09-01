package com.warehouse.wms.service;

import com.warehouse.wms.dto.location.LocationRequest;
import com.warehouse.wms.dto.location.LocationResponse;
import com.warehouse.wms.dto.product.ProductResponse;
import com.warehouse.wms.exception.BusinessRuleException;
import com.warehouse.wms.exception.ResourceNotFoundException;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private QrCodeService qrCodeService;

    @InjectMocks
    private LocationService locationService;

    private Location location(Long id, String code, String description, int maxCapacity, int currentOccupancy) {
        Location loc = new Location();
        loc.setId(id);
        loc.setCode(code);
        loc.setDescription(description);
        loc.setMaxCapacity(maxCapacity);
        loc.setCurrentOccupancy(currentOccupancy);
        return loc;
    }

    @Test
    void findAll_returnsAllLocationsAsResponses() {
        Location loc1 = location(1L, "A-01", "Aisle A", 10, 2);
        Location loc2 = location(2L, "B-01", "Aisle B", 20, 5);
        when(locationRepository.findAll()).thenReturn(List.of(loc1, loc2));

        List<LocationResponse> responses = locationService.findAll();

        assertEquals(2, responses.size());
        assertEquals("A-01", responses.get(0).code());
        assertEquals(8, responses.get(0).availableCapacity());
        assertEquals("B-01", responses.get(1).code());
        assertEquals(15, responses.get(1).availableCapacity());
    }

    @Test
    void findById_returnsLocationWhenFound() {
        Location loc = location(1L, "A-01", "Aisle A", 10, 3);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(loc));

        LocationResponse response = locationService.findById(1L);

        assertEquals(1L, response.id());
        assertEquals("A-01", response.code());
        assertEquals(7, response.availableCapacity());
    }

    @Test
    void findById_throwsWhenNotFound() {
        when(locationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> locationService.findById(99L));
    }

    @Test
    void getProductsInLocation_returnsProducts() {
        Location loc = location(1L, "A-01", "Aisle A", 10, 4);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(loc));

        Product product = new Product();
        product.setId(10L);
        product.setSku("SKU-1");
        product.setName("Scanner");
        product.setQuantity(4);
        product.setPrice(new BigDecimal("150.00"));
        product.setLocation(loc);

        when(productRepository.findByLocationId(1L)).thenReturn(List.of(product));

        List<ProductResponse> products = locationService.getProductsInLocation(1L);

        assertEquals(1, products.size());
        assertEquals("SKU-1", products.get(0).sku());
        assertEquals("A-01", products.get(0).locationCode());
    }

    @Test
    void generateQr_delegatesToQrCodeService() {
        byte[] fakeQr = new byte[]{1, 2, 3};
        when(qrCodeService.generateQRCode("LOC:A-01")).thenReturn(fakeQr);

        byte[] result = locationService.generateQr("A-01");

        assertArrayEquals(fakeQr, result);
        verify(qrCodeService).generateQRCode("LOC:A-01");
    }

    @Test
    void create_savesNewLocationWhenCodeIsUnique() {
        LocationRequest request = new LocationRequest("A-01", "Zone A", 25);
        when(locationRepository.findByCodeIgnoreCase("A-01")).thenReturn(Optional.empty());
        when(locationRepository.save(any(Location.class))).thenAnswer(inv -> {
            Location loc = inv.getArgument(0);
            loc.setId(1L);
            return loc;
        });

        LocationResponse response = locationService.create(request);

        assertNotNull(response.id());
        assertEquals("A-01", response.code());
        assertEquals(25, response.maxCapacity());
        assertEquals(0, response.currentOccupancy());
        assertEquals(25, response.availableCapacity());
    }

    @Test
    void create_rejectsDuplicateCode() {
        LocationRequest request = new LocationRequest("A-01", "Zone A", 25);
        when(locationRepository.findByCodeIgnoreCase("A-01")).thenReturn(Optional.of(location(1L, "A-01", "Existing", 10, 0)));

        assertThrows(BusinessRuleException.class, () -> locationService.create(request));
        verify(locationRepository, never()).save(any());
    }

    @Test
    void update_modifiesExistingLocation() {
        Location existing = location(1L, "A-01", "Old desc", 20, 5);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(locationRepository.findByCodeIgnoreCase("A-01")).thenReturn(Optional.of(existing));

        LocationRequest updateReq = new LocationRequest("A-01", "New desc", 30);
        LocationResponse response = locationService.update(1L, updateReq);

        assertEquals("New desc", response.description());
        assertEquals(30, response.maxCapacity());
        assertEquals(25, response.availableCapacity());
    }

    @Test
    void update_rejectsCapacityLessThanCurrentOccupancy() {
        Location existing = location(1L, "A-01", "Desc", 20, 8);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(locationRepository.findByCodeIgnoreCase("A-01")).thenReturn(Optional.of(existing));

        LocationRequest updateReq = new LocationRequest("A-01", "Desc", 5); // 5 < 8

        assertThrows(BusinessRuleException.class, () -> locationService.update(1L, updateReq));
    }

    @Test
    void delete_removesEmptyLocation() {
        Location existing = location(1L, "A-01", "Desc", 20, 0);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.findByLocationId(1L)).thenReturn(List.of());

        locationService.delete(1L);

        verify(locationRepository).delete(existing);
    }

    @Test
    void delete_rejectsWhenLocationHasProducts() {
        Location existing = location(1L, "A-01", "Desc", 20, 3);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.findByLocationId(1L)).thenReturn(List.of(new Product()));

        assertThrows(BusinessRuleException.class, () -> locationService.delete(1L));
        verify(locationRepository, never()).delete(any());
    }

    // --- Slotting Algorithm Tests ---

    @Test
    void suggest_excludesLocationsWithoutEnoughFreeCapacity() {
        Location locA = location(1L, "A-01", "Desc", 10, 8); // 2 free, not enough for 5
        Location locB = location(2L, "B-02", "Desc", 10, 2); // 8 free, enough
        when(locationRepository.findAll()).thenReturn(List.of(locA, locB));

        LocationResponse result = locationService.suggest(5);

        assertNotNull(result);
        assertEquals("B-02", result.code());
    }

    @Test
    void suggest_prefersAlreadyPartiallyOccupiedLocation() {
        Location locD = location(1L, "D-04", "Desc", 10, 0); // 10 free, empty
        Location locC = location(2L, "C-01", "Desc", 10, 5); // 5 free, partially full
        when(locationRepository.findAll()).thenReturn(List.of(locD, locC));

        LocationResponse result = locationService.suggest(3);

        assertEquals("C-01", result.code());
    }

    @Test
    void suggest_throwsWhenNoLocationHasEnoughCapacity() {
        Location locA = location(1L, "A-01", "Desc", 10, 9); // 1 free
        Location locB = location(2L, "B-02", "Desc", 5, 5);  // 0 free
        when(locationRepository.findAll()).thenReturn(List.of(locA, locB));

        assertThrows(ResourceNotFoundException.class, () -> locationService.suggest(2));
    }

    @Test
    void suggest_acceptsLocationWhereFreeCapacityMatchesExactRequest() {
        Location locE = location(1L, "E-05", "Desc", 10, 7); // 3 free
        when(locationRepository.findAll()).thenReturn(List.of(locE));

        LocationResponse result = locationService.suggest(3);

        assertNotNull(result);
        assertEquals("E-05", result.code());
    }

    @Test
    void suggest_rejectsZeroOrNegativeQuantity() {
        assertThrows(BusinessRuleException.class, () -> locationService.suggest(0));
        assertThrows(BusinessRuleException.class, () -> locationService.suggest(-5));
    }

    @Test
    void suggest_breaksTiesAlphabeticallyByCode() {
        Location locZ = location(1L, "Z-99", "Desc", 10, 0);
        Location locA = location(2L, "A-01", "Desc", 10, 0);
        when(locationRepository.findAll()).thenReturn(List.of(locZ, locA));

        LocationResponse result = locationService.suggest(1);

        assertEquals("A-01", result.code());
    }
}
