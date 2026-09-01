package com.warehouse.wms.service;

import com.warehouse.wms.dto.user.CreateUserRequest;
import com.warehouse.wms.dto.user.UserDto;
import com.warehouse.wms.exception.BusinessRuleException;
import com.warehouse.wms.model.User;
import com.warehouse.wms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {

        user = new User();

        user.setId(1L);
        user.setUsername("operator1");
        user.setPassword("hashed-old");
        user.setFullName("Operator One");
        user.setRole("ROLE_OPERATOR");
        user.setActive(true);
    }

    @Test
    void createUserCreatesDtoAndHashesPassword() {

        CreateUserRequest request =
                new CreateUserRequest(
                        " operator1 ",
                        "secret",
                        "Operator One",
                        "ROLE_OPERATOR",
                        "A"
                );

        when(userRepository.findByUsername("operator1"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("secret"))
                .thenReturn("hashed-secret");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {

                    User saved =
                            invocation.getArgument(0);

                    saved.setId(1L);

                    return saved;
                });

        UserDto result =
                userService.createUser(request);

        assertEquals(
                "operator1",
                result.username()
        );

        assertEquals(
                "ROLE_OPERATOR",
                result.role()
        );

        assertTrue(
                result.active()
        );

        verify(passwordEncoder)
                .encode("secret");

        verify(userRepository)
                .save(any(User.class));
    }

    @Test
    void createUserRejectsDuplicateUsername() {

        when(userRepository.findByUsername("operator1"))
                .thenReturn(Optional.of(user));

        assertThrows(
                BusinessRuleException.class,
                () -> userService.createUser(
                        new CreateUserRequest(
                                "operator1",
                                "secret",
                                null,
                                "ROLE_OPERATOR",
                                null
                        )
                )
        );

        verify(
                userRepository,
                never()
        ).save(any());
    }

    @Test
    void toggleUserStatusChangesActiveFlag() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userRepository.save(user))
                .thenReturn(user);

        UserDto result =
                userService.toggleUserStatus(1L);

        assertFalse(
                result.active()
        );

        verify(userRepository)
                .save(user);
    }

    @Test
    void changePasswordRejectsDifferentConfirmation() {

        assertThrows(
                BusinessRuleException.class,
                () -> userService.changePassword(
                        "operator1",
                        "old",
                        "new",
                        "different"
                )
        );

        verifyNoInteractions(
                userRepository,
                passwordEncoder
        );
    }

    @Test
    void changePasswordRejectsWrongOldPassword() {

        when(userRepository.findByUsername("operator1"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "wrong",
                "hashed-old"
        )).thenReturn(false);

        assertThrows(
                BusinessRuleException.class,
                () -> userService.changePassword(
                        "operator1",
                        "wrong",
                        "new",
                        "new"
                )
        );

        verify(
                userRepository,
                never()
        ).save(any());
    }

    @Test
    void changePasswordHashesAndSavesNewPassword() {

        when(userRepository.findByUsername("operator1"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "old",
                "hashed-old"
        )).thenReturn(true);

        when(passwordEncoder.encode("new"))
                .thenReturn("hashed-new");

        userService.changePassword(
                "operator1",
                "old",
                "new",
                "new"
        );

        assertEquals(
                "hashed-new",
                user.getPassword()
        );

        verify(userRepository)
                .save(user);
    }
}