package com.warehouse.wms.service;

import com.warehouse.wms.dto.user.CreateUserRequest;
import com.warehouse.wms.dto.user.UserDto;
import com.warehouse.wms.exception.BusinessRuleException;
import com.warehouse.wms.exception.ResourceNotFoundException;
import com.warehouse.wms.model.User;
import com.warehouse.wms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserDto> findAll() {

        return userRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public UserDto createUser(
            CreateUserRequest request
    ) {

        String username =
                request.username().trim();

        userRepository.findByUsername(username)
                .ifPresent(existing -> {
                    throw new BusinessRuleException(
                            "Username '"
                                    + username
                                    + "' is already in use."
                    );
                });

        User user = new User();

        user.setUsername(username);

        user.setPassword(
                passwordEncoder.encode(
                        request.password()
                )
        );

        user.setFullName(
                request.fullName() == null
                        || request.fullName().isBlank()
                        ? username
                        : request.fullName().trim()
        );

        user.setRole(
                request.role()
                        .trim()
                        .toUpperCase()
        );

        user.setShift(
                request.shift()
        );

        user.setActive(true);

        User saved =
                userRepository.save(user);

        return toDto(saved);
    }

    @Transactional
    public UserDto toggleUserStatus(
            Long id
    ) {

        User user =
                userRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User",
                                        id
                                    )
                        );

        user.setActive(
                !user.isActive()
        );

        return toDto(
                userRepository.save(user)
        );
    }

    @Transactional
    public void changePassword(
            String username,
            String oldPassword,
            String newPassword,
            String confirmPassword
    ) {

        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessRuleException(
                    "New password and confirmation do not match."
            );
        }

        User user =
                userRepository.findByUsername(username)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User",
                                        username
                                )
                        );

        if (!passwordEncoder.matches(
                oldPassword,
                user.getPassword()
        )) {

            throw new BusinessRuleException(
                    "Current password is incorrect."
            );
        }


        user.setPassword(
                passwordEncoder.encode(
                        newPassword
                )
        );

        userRepository.save(user);
    }

    public User findByUsername(
            String username
    ) {

        return userRepository
                .findByUsername(username)
                .orElse(null);
    }

    private UserDto toDto(
            User user
    ) {

        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.getShift(),
                user.isActive(),
                user.getLastLogin()
        );
    }
}