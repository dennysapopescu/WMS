package com.warehouse.wms.dto.location;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LocationRequest(
        @NotBlank @Size(max = 50) String code,
        @Size(max = 255) String description,
        @Min(1) Integer maxCapacity
) { }
