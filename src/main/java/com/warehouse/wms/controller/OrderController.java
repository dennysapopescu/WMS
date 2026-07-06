package com.warehouse.wms.controller;

import com.warehouse.wms.model.*;
import com.warehouse.wms.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final LocationRepository locationRepository;

    // În OrderController.java

    @GetMapping
    public String listOrders(Model model) {
        List<OutboundOrder> allOrders = orderRepository.findAll();

        // Sortarea (Drum Minim)
        List<OutboundOrder> sortedOrders = allOrders.stream()
                .sorted(Comparator
                        .comparing((OutboundOrder o) -> o.getStatus().equals("COMPLETED"))
                        .thenComparing(o -> o.getSuggestedLocation() != null ? o.getSuggestedLocation().getCode() : "ZZZ")
                )
                .toList();

        // Căutăm istoricul după "PICKING FINALIZAT" pentru a-l afișa în tabelul de jos
        List<InventoryLog> history = inventoryLogRepository.findTop5ByActionOrderByTimestampDesc("PICKING FINALIZAT");

        model.addAttribute("orders", sortedOrders);
        model.addAttribute("history", history);
        return "orders";
    }

    @PostMapping("/create")
    public String createOrder(@RequestParam String sku,
                              @RequestParam Integer quantity,
                              RedirectAttributes redirectAttributes) {

        Product product = productRepository.findBySku(sku).orElse(null);

        if (product == null) {
            redirectAttributes.addFlashAttribute("error", "Eroare: SKU-ul '" + sku + "' nu există!");
            return "redirect:/";
        }

        if (product.getQuantity() < quantity) {
            redirectAttributes.addFlashAttribute("error", "Stoc insuficient! Disponibil: " + product.getQuantity());
            return "redirect:/";
        }

        OutboundOrder pickingTask = new OutboundOrder();
        pickingTask.setSku(sku);
        pickingTask.setRequestedQuantity(quantity);
        pickingTask.setPickedQuantity(0);
        pickingTask.setStatus("PENDING");
        pickingTask.setCreatedAt(LocalDateTime.now());

        if (product.getLocation() != null) {
            pickingTask.setSuggestedLocation(product.getLocation());
        }

        orderRepository.save(pickingTask);
        saveLog(product.getName(), sku, "PICKING GENERAT", quantity);

        redirectAttributes.addFlashAttribute("success", "Task picking creat pentru " + product.getName());
        return "redirect:/orders";
    }

    // @Transactional: toate operațiile de mai jos (verificare stoc, scădere cantitate,
    // actualizare locație, marcare comandă COMPLETED, log) devin atomice — dacă oricare
    // pas eșuează, se face rollback complet.
    @PostMapping("/scan-confirm")
    @ResponseBody
    @Transactional
    public ResponseEntity<String> scanAndConfirm(@RequestParam Long orderId, @RequestParam String scannedCode) {
        Optional<OutboundOrder> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) return ResponseEntity.status(404).body("Comanda nu există.");

        OutboundOrder order = orderOpt.get();

        if (order.getSuggestedLocation() == null || !order.getSuggestedLocation().getCode().equalsIgnoreCase(scannedCode.trim())) {
            return ResponseEntity.status(400).body("Locație incorectă!");
        }

        Product product = productRepository.findAll().stream()
                .filter(p -> p.getSku().equalsIgnoreCase(order.getSku()) &&
                        p.getLocation().getId().equals(order.getSuggestedLocation().getId()))
                .findFirst().orElse(null);

        if (product == null || product.getQuantity() < order.getRequestedQuantity()) {
            return ResponseEntity.status(500).body("Eroare stoc.");
        }

        try {
            // product.getVersion() e verificat automat de JPA la save(): dacă alt operator
            // a modificat acest produs între citire și scriere, Hibernate detectează
            // mismatch-ul de versiune și aruncă OptimisticLockingFailureException în loc
            // să suprascrie silențios o cantitate deja modificată de altcineva.
            product.setQuantity(product.getQuantity() - order.getRequestedQuantity());
            productRepository.save(product);

            Location loc = product.getLocation();
            loc.setCurrentOccupancy(loc.getCurrentOccupancy() - order.getRequestedQuantity());
            locationRepository.save(loc);

            order.setStatus("COMPLETED");
            orderRepository.save(order);

            saveLog(product.getName(), product.getSku(), "PICKING FINALIZAT", order.getRequestedQuantity());
            return ResponseEntity.ok("Succes");
        } catch (OptimisticLockingFailureException ex) {
            // Alt operator a modificat acest produs chiar înainte de commit-ul nostru.
            // Respingem cererea în loc să corupem stocul; clientul poate rescana/reîncerca.
            return ResponseEntity.status(409).body("Conflict: stocul a fost modificat de alt operator între timp. Reîncearcă.");
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteOrder(@PathVariable Long id, RedirectAttributes ra) {
        orderRepository.findById(id).ifPresent(order -> {
            if ("PENDING".equals(order.getStatus())) {
                orderRepository.deleteById(id);
                ra.addFlashAttribute("success", "Task anulat.");
            }
        });
        return "redirect:/orders";
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
        inventoryLogRepository.save(log);
    }
}