package com.warehouse.wms.service;

import com.warehouse.wms.model.User;
import com.warehouse.wms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private User user(String username, String encodedPassword) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(encodedPassword);
        return u;
    }

    @Test
    void updatePassword_returnsFalseWhenUserDoesNotExist() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        boolean result = userService.updatePassword("ghost", "old", "new");

        assertFalse(result);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updatePassword_returnsFalseWhenOldPasswordIsWrong() {
        User existing = user("operator1", "hashed-old");
        when(userRepository.findByUsername("operator1")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("wrong-old", "hashed-old")).thenReturn(false);

        boolean result = userService.updatePassword("operator1", "wrong-old", "newPass");

        assertFalse(result);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updatePassword_encodesAndSavesNewPasswordWhenOldPasswordMatches() {
        User existing = user("operator1", "hashed-old");
        when(userRepository.findByUsername("operator1")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("correct-old", "hashed-old")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("hashed-new");

        boolean result = userService.updatePassword("operator1", "correct-old", "newPass");

        assertTrue(result);
        assertEquals("hashed-new", existing.getPassword());
        verify(userRepository, times(1)).save(existing);
    }

    @Test
    void findByUsername_returnsNullWhenNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertNull(userService.findByUsername("ghost"));
    }

    @Test
    void findByUsername_returnsUserWhenFound() {
        User existing = user("admin", "hashed");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existing));

        assertEquals(existing, userService.findByUsername("admin"));
    }
}
