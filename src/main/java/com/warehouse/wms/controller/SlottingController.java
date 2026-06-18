package com.warehouse.wms.controller;

import com.warehouse.wms.model.Location;
import com.warehouse.wms.repository.LocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/slotting")
public class SlottingController {

    @Autowired
    private LocationRepository locationRepository;

    @GetMapping("/suggest")
    public Location suggestLocation(@RequestParam int qty) {
        return locationRepository.findAll().stream()
                // 1. Filtrez doar locațiile care au spațiu suficient
                .filter(loc -> (loc.getMaxCapacity() - loc.getCurrentOccupancy()) >= qty)
                // 2. Sortăm: prioritizez locațiile care sunt deja parțial ocupate (pentru a nu risipi rafturi goale)
                // Apoi sortez alfabetic după cod pentru proximitate
                .sorted(Comparator.comparing(Location::getCurrentOccupancy).reversed()
                        .thenComparing(Location::getCode))
                .findFirst()
                .orElse(null);
    }
}