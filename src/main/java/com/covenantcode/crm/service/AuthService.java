package com.covenantcode.crm.service;

import com.covenantcode.crm.dto.auth.AuthResponse;
import com.covenantcode.crm.dto.auth.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
}
