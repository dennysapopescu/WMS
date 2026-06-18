package com.warehouse.wms.controller;

import org.springframework.ui.Model;
import com.warehouse.wms.model.User;
import com.warehouse.wms.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private UserService userService;

    @GetMapping
    public String viewProfile(Model model, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        model.addAttribute("user", user);
        return "profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Parolele noi nu coincid!");
            return "redirect:/profile";
        }

        boolean success = userService.updatePassword(principal.getName(), oldPassword, newPassword);

        if (success) {
            redirectAttributes.addFlashAttribute("success", "Parola a fost schimbată cu succes!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Parola actuală este incorectă!");
        }

        return "redirect:/profile";
    }
}