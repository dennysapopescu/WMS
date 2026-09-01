package com.warehouse.wms.api;

import com.warehouse.wms.dto.user.CreateUserRequest;
import com.warehouse.wms.dto.user.UserDto;
import com.warehouse.wms.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<UserDto> getAllUsers() {
        return userService.findAll();
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {

        UserDto created =
                userService.createUser(request);

        URI uri =
                ServletUriComponentsBuilder
                        .fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(created.id())
                        .toUri();

        return ResponseEntity
                .created(uri)
                .body(created);
    }

    @PatchMapping("/{id}/toggle")
    public UserDto toggleUserStatus(
            @PathVariable Long id
    ) {

        return userService.toggleUserStatus(id);
    }
}