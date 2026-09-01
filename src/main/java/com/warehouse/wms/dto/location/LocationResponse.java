package com.warehouse.wms.dto.location;

public record LocationResponse(Long id, String code, String description, Integer maxCapacity,
                               Integer currentOccupancy, Integer availableCapacity, Long version) { }
