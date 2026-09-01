package com.warehouse.wms.dto.order;

import jakarta.validation.constraints.NotBlank;

public record PickConfirmationRequest(@NotBlank String scannedLocationCode) { }
