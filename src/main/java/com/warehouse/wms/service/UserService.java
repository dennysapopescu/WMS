package com.warehouse.wms.service;

import com.warehouse.wms.model.User;
import com.warehouse.wms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public boolean updatePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            return false;
        }

        if (passwordEncoder.matches(oldPassword, user.getPassword())) {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return true;
        }

        return false;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }
}