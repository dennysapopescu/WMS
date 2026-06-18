package com.warehouse.wms.controller;

import com.warehouse.wms.model.*;
import com.warehouse.wms.repository.*;
import com.warehouse.wms.service.PdfService;
import com.warehouse.wms.model.OutboundOrder;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ViewController {

    @Autowired private ProductRepository productRepository;
    @Autowired private InventoryLogRepository logRepository;
    @Autowired private UserRepository userRepository; // REPO NOU INJECTAT
    @Autowired private PdfService pdfService;
    @Autowired private LocationRepository locationRepository;

    @Autowired private OrderRepository orderRepository;

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping({"/", "/products"})
    public String listProducts(Model model) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

        // 1. incarcam datele brute
        List<Product> products = productRepository.findAll();
        List<Location> allLocations = locationRepository.findAll();
        List<InventoryLog> allLogs = logRepository.findAll(Sort.by(Sort.Direction.DESC, "timestamp"));
        List<User> allUsers = userRepository.findAll();
        List<OutboundOrder> outboundOrders = orderRepository.findAll();

        // 2. calculez ocuparea pentru FIECARE locație (sincronizare fortata)
        for (Location loc : allLocations) {
            int totalQty = 0;
            for (Product p : products) {
                // Verificăm dacă produsul are o locație și dacă ID-urile coincid
                if (p.getLocation() != null && p.getLocation().getId().equals(loc.getId())) {
                    totalQty += (p.getQuantity() != null) ? p.getQuantity() : 0;
                }
            }
            // setez valoarea calculata pe loc
            loc.setCurrentOccupancy(totalQty);

            // salvez în DB pentru a forța actualizarea coloanei current_occupancy
            locationRepository.save(loc);
        }

        // 3. logica pt activitatea utilizatorului
        List<InventoryLog> myActivity = allLogs.stream()
                .filter(l -> l.getPerformedBy().equals(currentUser))
                .limit(5)
                .toList();

        // 4. date pentru grafic
        Map<String, Long> chartData = products.stream()
                .filter(p -> p.getLocation() != null)
                .collect(Collectors.groupingBy(p -> p.getLocation().getCode(), Collectors.counting()));

        // 5. statistici financiare
        double totalValue = products.stream()
                .mapToDouble(p -> (p.getQuantity() != null && p.getPrice() != null) ? p.getQuantity() * p.getPrice() : 0.0)
                .sum();

        // 6. Estimare epuizare stoc
        Map<String, String> predictions = new HashMap<>();
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        for (Product p : products) {
            // calculez cât s-a scos din stoc pentru acest SKU în ultimele 7 zile (doar acțiunile de picking sau reducere)
            long totalConsumed = allLogs.stream()
                    .filter(l -> l.getSku().equals(p.getSku()) &&
                            (l.getAction().contains("PICKING FINALIZAT") || l.getAction().equals("REDUCERE")))
                    .filter(l -> l.getTimestamp().isAfter(sevenDaysAgo))
                    .mapToLong(l -> Math.abs(l.getQuantityChanged()))
                    .sum();

            double dailyRate = totalConsumed / 7.0;

            if (dailyRate > 0) {
                int daysRemaining = (int) (p.getQuantity() / dailyRate);
                predictions.put(p.getSku(), daysRemaining + " zile");
            } else {
                predictions.put(p.getSku(), "Stabil");
            }
        }
        model.addAttribute("predictions", predictions);

        // 7. AI Security Audit (detectare anomalii - activitate nocturnă)
        List<String> aiAlerts = allLogs.stream()
                .filter(l -> l.getTimestamp().getHour() >= 22 || l.getTimestamp().getHour() < 6)
                .map(l -> "⚠️ Anomalie: " + l.getPerformedBy() + " a operat la ora " +
                        l.getTimestamp().getHour() + ":" + String.format("%02d", l.getTimestamp().getMinute()) +
                        " pe SKU " + l.getSku())
                .distinct()
                .limit(5)
                .toList();
        model.addAttribute("aiAlerts", aiAlerts);

        // 8. Trimitere date către Model
        model.addAttribute("products", products);
        model.addAttribute("allLocations", allLocations); // locațiile au ocuparea calculată
        model.addAttribute("allUsers", allUsers);
        model.addAttribute("logs", allLogs.stream().limit(10).toList());
        model.addAttribute("myActivity", myActivity);
        model.addAttribute("totalItems", products.size());
        model.addAttribute("totalQuantity", products.stream().mapToInt(p -> p.getQuantity() != null ? p.getQuantity() : 0).sum());
        model.addAttribute("lowStock", products.stream().filter(p -> p.getQuantity() != null && p.getQuantity() <= 5).count());
        model.addAttribute("totalValue", String.format("%.2f", totalValue));
        model.addAttribute("chartLabels", chartData.keySet());
        model.addAttribute("chartValues", chartData.values());
        model.addAttribute("outboundOrders", outboundOrders);

        return "index";
    }
    @GetMapping("/products/export")
    public void exportToPdf(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=inventar_wms.pdf");
        pdfService.export(response, productRepository.findAll());
    }

    @PostMapping("/products/add")
    public String addProduct(@ModelAttribute Product product, @RequestParam("locationId") Long locationId, RedirectAttributes redirectAttributes) {
        // 1. gasesc locația selectată
        Location loc = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Locația nu a fost găsită"));

        // 2. verif capacitate
        if (loc.getCurrentOccupancy() + product.getQuantity() > loc.getMaxCapacity()) {
            int locuriLibere = loc.getMaxCapacity() - loc.getCurrentOccupancy();

            // caut o recomandare (primul raft care are destul loc)
            Location recomandare = locationRepository.findAll().stream()
                    .filter(l -> (l.getMaxCapacity() - l.getCurrentOccupancy()) >= product.getQuantity())
                    .findFirst()
                    .orElse(null);

            String mesajEroare = "Spațiu insuficient pe " + loc.getCode() + " (mai are doar " + locuriLibere + " locuri). ";
            if (recomandare != null) {
                mesajEroare += "Recomandare: Încearcă pe raftul " + recomandare.getCode() + " (" + recomandare.getDescription() + ").";
            } else {
                mesajEroare += "Nu s-a găsit niciun alt raft cu suficient spațiu!";
            }

            redirectAttributes.addFlashAttribute("error", mesajEroare);
            return "redirect:/products";
        }

        // 3. save produs (dacă e loc)
        Optional<Product> existingProduct = productRepository.findBySku(product.getSku());
        if (existingProduct.isPresent()) {
            Product p = existingProduct.get();
            p.setQuantity(p.getQuantity() + product.getQuantity());
            p.setLocation(loc);
            productRepository.save(p);
        } else {
            product.setLocation(loc);
            productRepository.save(product);
        }

        updateLocationOccupancy(loc, product.getQuantity());
        saveLog(product.getName(), product.getSku(), "ADĂUGARE", product.getQuantity());

        redirectAttributes.addFlashAttribute("success", "Produs adăugat cu succes pe " + loc.getCode());
        return "redirect:/products";
    }
    @PostMapping("/products/update")
    public String updateProduct(@ModelAttribute Product product, @RequestParam("locationId") Long locationId) {
        productRepository.findById(product.getId()).ifPresent(p -> {
            Location newLoc = locationRepository.findById(locationId).orElse(null);
            p.setName(product.getName());
            p.setSku(product.getSku());
            p.setQuantity(product.getQuantity());
            p.setPrice(product.getPrice());
            p.setLocation(newLoc); // Setăm locația găsită după ID
            productRepository.save(p);
            saveLog(p.getName(), p.getSku(), "MODIFICARE", 0);
        });
        return "redirect:/products?success=updated";
    }

    @PostMapping("/products/reduce/{id}")
    public String reduceQuantity(@PathVariable Long id) {
        productRepository.findById(id).ifPresent(p -> {
            if (p.getQuantity() > 0) {
                p.setQuantity(p.getQuantity() - 1);
                productRepository.save(p);

                // ADĂUGĂM ASTA: Eliberăm 1 loc pe hartă
                if (p.getLocation() != null) {
                    updateLocationOccupancy(p.getLocation(), -1);
                }

                saveLog(p.getName(), p.getSku(), "REDUCERE", -1);
            }
        });
        return "redirect:/products?success=reduced";
    }

    @PostMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id) {
        productRepository.findById(id).ifPresent(p -> {
            // ADĂUGĂM ASTA: Eliberăm tot spațiul ocupat de acest produs pe hartă
            if (p.getLocation() != null) {
                updateLocationOccupancy(p.getLocation(), -p.getQuantity());
            }

            saveLog(p.getName(), p.getSku(), "ȘTERGERE", -p.getQuantity());
            productRepository.deleteById(id);
        });
        return "redirect:/products?success=deleted";
    }

    private void saveLog(String name, String sku, String action, Integer qty) {
        String user = SecurityContextHolder.getContext().getAuthentication().getName();
        InventoryLog log = new InventoryLog();
        log.setProductName(name);
        log.setSku(sku);
        log.setAction(action);
        log.setQuantityChanged(qty);
        log.setTimestamp(LocalDateTime.now());
        log.setPerformedBy(user);
        logRepository.save(log);
    }

    // metoda utilitara pentru a actualiza Digital Twin
    private void updateLocationOccupancy(Location loc, int change) {
        loc.setCurrentOccupancy(loc.getCurrentOccupancy() + change);
        locationRepository.save(loc);
    }
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @PostMapping("/products/import")
    public String importCsv(@RequestParam("file") org.springframework.web.multipart.MultipartFile file, RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Te rugăm să selectezi un fișier CSV!");
            return "redirect:/products";
        }

        int importedCount = 0;
        int redirectedCount = 0;
        int skippedCount = 0;

        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(file.getInputStream()))) {
            String line = reader.readLine(); // Header

            // Încarcam toate locațiile o singura data pt performanta
            List<Location> allLocations = locationRepository.findAll();

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] data = line.split(",");
                if (data.length < 5) continue;

                try {
                    String name = data[0].trim();
                    String sku = data[1].trim();
                    int qty = Integer.parseInt(data[2].trim());
                    double price = Double.parseDouble(data[3].trim());
                    String targetCode = data[4].trim();

                    // 1. Găsim locația țintă din CSV
                    Location loc = allLocations.stream()
                            .filter(l -> l.getCode().equalsIgnoreCase(targetCode))
                            .findFirst().orElse(null);

                    // 2. logica de sugestie
                    if (loc == null || (loc.getCurrentOccupancy() + qty > loc.getMaxCapacity())) {
                        // caut ALTA locație care are spațiu
                        Location alternativeLoc = allLocations.stream()
                                .filter(l -> (l.getMaxCapacity() - l.getCurrentOccupancy()) >= qty)
                                .findFirst().orElse(null);

                        if (alternativeLoc != null) {
                            loc = alternativeLoc;
                            redirectedCount++;
                        } else {
                            skippedCount++;
                            continue; // Nicio locație nu are loc
                        }
                    }

                    // 3. Salvare Produs
                    Product p = productRepository.findBySku(sku).orElse(new Product());
                    p.setName(name);
                    p.setSku(sku);
                    p.setQuantity(p.getQuantity() != null ? p.getQuantity() + qty : qty);
                    p.setPrice(price);
                    p.setLocation(loc);

                    productRepository.save(p);

                    // 4. Actualizez ocuparea în obiectul local și în DB
                    loc.setCurrentOccupancy(loc.getCurrentOccupancy() + qty);
                    locationRepository.save(loc);

                    saveLog(name, sku, "IMPORT" + (redirectedCount > 0 ? " (REDIRECȚIONAT)" : ""), qty);
                    importedCount++;

                } catch (Exception e) {
                    skippedCount++;
                }
            }

            StringBuilder msg = new StringBuilder("Procesat: " + importedCount + " produse. ");
            if (redirectedCount > 0) msg.append(redirectedCount).append(" au fost mutate automat pe rafturi libere. ");
            if (skippedCount > 0) msg.append("Ignorate: ").append(skippedCount);

            redirectAttributes.addFlashAttribute("success", msg.toString());

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Eroare: " + e.getMessage());
        }
        return "redirect:/products";
    }
    @GetMapping("/products/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=template_import_wms.csv");

        // Header trebuie să coincidă exact cu ce asteapta metoda de import
        String header = "Nume,SKU,Cantitate,Pret,CodLocatie\n";
        String exemplu = "Exemplu Produs,SKU123,10,100.0,A-01-01\n";

        response.getWriter().write(header);
        response.getWriter().write(exemplu);
        response.getWriter().flush();
    }

    @PostMapping("/products/transfer")
    public String transferProduct(@RequestParam Long productId,
                                  @RequestParam Long newLocationId,
                                  RedirectAttributes redirectAttributes) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produsul nu a fost găsit"));

        Location oldLoc = product.getLocation();
        Location newLoc = locationRepository.findById(newLocationId)
                .orElseThrow(() -> new RuntimeException("Locația nouă nu a fost găsită"));

        // 1. verif daca e loc în noua locatie
        if (newLoc.getCurrentOccupancy() + product.getQuantity() > newLoc.getMaxCapacity()) {
            redirectAttributes.addFlashAttribute("error", "Transfer eșuat: Nu este loc suficient pe " + newLoc.getCode());
            return "redirect:/products";
        }

        // 2. actualizez ocuparea pe harta Digital Twin (sursă -> destinație)
        if (oldLoc != null) {
            updateLocationOccupancy(oldLoc, -product.getQuantity());
        }
        updateLocationOccupancy(newLoc, product.getQuantity());

        // 3. schimb locația produsului
        product.setLocation(newLoc);
        productRepository.save(product);

        saveLog(product.getName(), product.getSku(), "TRANSFER către " + newLoc.getCode(), product.getQuantity());

        redirectAttributes.addFlashAttribute("success", "Produsul " + product.getSku() + " a fost mutat cu succes pe " + newLoc.getCode());
        return "redirect:/products";
    }

}