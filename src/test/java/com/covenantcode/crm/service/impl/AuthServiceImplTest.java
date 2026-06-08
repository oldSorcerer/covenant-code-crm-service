package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.dto.auth.AuthResponse;
import com.covenantcode.crm.dto.auth.RegisterRequest;
import com.covenantcode.crm.entity.Role;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.RoleName;
import com.covenantcode.crm.exception.ConflictException;
import com.covenantcode.crm.exception.ResourceNotFoundException;
import com.covenantcode.crm.repository.RoleRepository;
import com.covenantcode.crm.repository.UserRepository;
import com.covenantcode.crm.security.JwtService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.BeforeEach;
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
public class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest request;

    @BeforeEach
    void setUp() {
        request = new RegisterRequest(
                "Ivan",
                "Ivanov",
                "ivan@example.com",
                "password123",
                "+79991234567"
        );
    }

    @Test
    public void register_success() {
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);

        Role role = new Role();
        role.setName(RoleName.MANAGER);
        when(roleRepository.findByName(RoleName.MANAGER)).thenReturn(Optional.of(role));

        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setFirstName(request.getFirstName());
        savedUser.setLastName(request.getLastName());
        savedUser.setEmail(request.getEmail());
        savedUser.setPassword("encoded-password");
        savedUser.setRole(role);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("mock.token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mock.token", response.getToken());
        assertEquals("MANAGER", response.getRole());

        verify(passwordEncoder).encode(request.getPassword());
        verify(userRepository).save(any(User.class));
        verify(jwtService).generateToken(any(User.class));
    }

    @Test
    public void register_emailAlreadyExists() {
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(roleRepository, passwordEncoder, jwtService);
    }

    @Test
    public void register_managerRoleNotFound() {
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleName.MANAGER)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
        verify(jwtService, never()).generateToken(any());
    }
}