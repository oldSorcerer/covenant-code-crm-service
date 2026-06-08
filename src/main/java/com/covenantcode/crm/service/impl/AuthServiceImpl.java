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
import com.covenantcode.crm.service.AuthService;
import org.springframework.transaction.annotation.Transactional;


import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Пользователь с email " + request.getEmail() + " уже существует");
        }

        Role managerRole = roleRepository.findByName(RoleName.MANAGER)
                .orElseThrow(() -> new ResourceNotFoundException("Роль MANAGER не найдена"));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(managerRole)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser);

        return AuthResponse.builder()
                .token(token)
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .role(savedUser.getRole().getName().name())
                .build();
    }
}
