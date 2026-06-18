package com.warehouse.wms.controller;

import org.springframework.ui.Model;
import com.warehouse.wms.model.User;
import com.warehouse.wms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/users/add")
    public String addUser(@ModelAttribute User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setActive(true);
        userRepository.save(user);

        return "redirect:/admin/users?success=user_added";
    }

    @PostMapping("/users/toggle/{id}")
    public String toggleUserStatus(@PathVariable Long id) {
        userRepository.findById(id).ifPresent(user -> {
            user.setActive(!user.isActive());
            userRepository.save(user);
        });

        return "redirect:/admin/users?success=status_updated";
    }

    @GetMapping("/users")
    public String showUsersPage(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "users";
    }
}