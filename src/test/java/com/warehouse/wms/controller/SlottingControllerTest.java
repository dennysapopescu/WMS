package com.warehouse.wms.controller;

import com.warehouse.wms.model.Location;
import com.warehouse.wms.repository.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class SlottingControllerTest {

    @Mock
    private LocationRepository locationRepository;

    @InjectMocks
    private SlottingController slottingController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private Location location(String code, int maxCapacity, int currentOccupancy) {
        Location loc = new Location();
        loc.setCode(code);
        loc.setMaxCapacity(maxCapacity);
        loc.setCurrentOccupancy(currentOccupancy);
        return loc;
    }

    @Test
    void suggestLocation_excludesLocationsWithoutEnoughFreeCapacity() {
        // A-01: 10 max, 8 ocupate -> doar 2 libere, insuficient pentru 5 bucăți
        // B-02: 10 max, 2 ocupate -> 8 libere, suficient
        when(locationRepository.findAll()).thenReturn(List.of(
                location("A-01", 10, 8),
                location("B-02", 10, 2)
        ));

        Location result = slottingController.suggestLocation(5);

        assertNotNull(result);
        assertEquals("B-02", result.getCode());
    }

    @Test
    void suggestLocation_prefersAlreadyPartiallyOccupiedLocation() {
        // Ambele au suficient spațiu liber pentru 3 bucăți, dar C-01 e deja parțial
        // ocupată -> ar trebui preferată, ca să nu "risipim" un raft complet gol.
        when(locationRepository.findAll()).thenReturn(List.of(
                location("D-04", 10, 0),
                location("C-01", 10, 5)
        ));

        Location result = slottingController.suggestLocation(3);

        assertEquals("C-01", result.getCode());
    }

    @Test
    void suggestLocation_returnsNullWhenNoLocationHasEnoughCapacity() {
        when(locationRepository.findAll()).thenReturn(List.of(
                location("A-01", 10, 9),
                location("B-02", 5, 5)
        ));

        Location result = slottingController.suggestLocation(2);

        assertNull(result);
    }

    @Test
    void suggestLocation_acceptsLocationWhereFreeCapacityExactlyMatchesRequest() {
        // 10 max, 7 ocupate -> exact 3 libere, cerem exact 3 -> ar trebui acceptată (>=)
        when(locationRepository.findAll()).thenReturn(List.of(
                location("E-05", 10, 7)
        ));

        Location result = slottingController.suggestLocation(3);

        assertNotNull(result);
        assertEquals("E-05", result.getCode());
    }

    @Test
    void suggestLocation_returnsNullWhenWarehouseHasNoLocations() {
        when(locationRepository.findAll()).thenReturn(List.of());

        Location result = slottingController.suggestLocation(1);

        assertNull(result);
    }

    @Test
    void suggestLocation_breaksTiesAlphabeticallyByCode() {
        // Ambele au aceeași ocupare curentă (0) -> tie-break alfabetic pe cod.
        when(locationRepository.findAll()).thenReturn(List.of(
                location("Z-99", 10, 0),
                location("A-01", 10, 0)
        ));

        Location result = slottingController.suggestLocation(1);

        assertEquals("A-01", result.getCode());
    }
}
