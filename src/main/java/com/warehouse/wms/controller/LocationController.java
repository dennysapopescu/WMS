package com.warehouse.wms.controller;

import com.warehouse.wms.model.Location;
import com.warehouse.wms.model.Product;
import com.warehouse.wms.repository.LocationRepository;
import com.warehouse.wms.repository.ProductRepository;
import com.warehouse.wms.service.QrCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/locations")
public class LocationController {

    @Autowired private LocationRepository locationRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private QrCodeService qrCodeService;

    @GetMapping
    public String listLocations(Model model) {
        model.addAttribute("locations", locationRepository.findAll());
        return "locations";
    }

    @PostMapping("/add")
    public String addLocation(@ModelAttribute Location location) {
        location.setCurrentOccupancy(0); // Locație nouă, deci e goală
        locationRepository.save(location);
        return "redirect:/locations?success";
    }

    @PostMapping("/update")
    public String updateLocation(@ModelAttribute Location location) {
        locationRepository.findById(location.getId()).ifPresent(loc -> {
            loc.setCode(location.getCode());
            loc.setDescription(location.getDescription());
            loc.setMaxCapacity(location.getMaxCapacity());
            locationRepository.save(loc);
        });
        return "redirect:/locations?success";
    }

    @PostMapping("/delete/{id}")
    public String deleteLocation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Location loc = locationRepository.findById(id).orElse(null);

        if (loc != null) {
            // VERIFICARE DE SIGURANȚĂ: Căutăm produse asociate acestei locații
            List<Product> productsInLocation = productRepository.findByLocation(loc);

            if (!productsInLocation.isEmpty()) {
                // Dacă lista nu e goală, oprim ștergerea și trimitem mesajul de eroare
                redirectAttributes.addFlashAttribute("error",
                        "Eroare: Locația " + loc.getCode() + " nu poate fi ștearsă deoarece conține produse în stoc!");
                return "redirect:/locations";
            }

            locationRepository.deleteById(id);
        }

        return "redirect:/locations?success";
    }

    @GetMapping(value = "/qr/{code}", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public byte[] getLocationQr(@PathVariable String code) {
        return qrCodeService.generateQRCode("LOC:" + code);
    }

    @GetMapping("/map") // Aceasta va deveni adresa /locations/map
    public String showWarehouseMap(Model model) {
        List<Location> locations = locationRepository.findAll();
        locations.sort(Comparator.comparing(Location::getCode));
        model.addAttribute("locations", locations);
        return "warehouse-map";
    }

    @GetMapping("/location-details/{id}")
    @ResponseBody
    public ResponseEntity<List<Product>> getLocationDetails(@PathVariable Long id) {
        // filtrez produsele care aparțin locației respective
        List<Product> products = productRepository.findAll().stream()
                .filter(p -> p.getLocation() != null && p.getLocation().getId().equals(id))
                .toList();
        return ResponseEntity.ok(products);
    }
}