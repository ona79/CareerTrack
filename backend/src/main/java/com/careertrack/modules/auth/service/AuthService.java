package com.careertrack.modules.auth.service;

import com.careertrack.modules.auth.dto.JwtResponse;
import com.careertrack.modules.auth.dto.LoginRequest;
import com.careertrack.modules.auth.dto.RefreshTokenRequest;
import com.careertrack.modules.auth.dto.UserDto;

public interface AuthService {
    JwtResponse login(LoginRequest loginRequest);
    JwtResponse refreshToken(RefreshTokenRequest request);
    void logout(String refreshToken);
    UserDto getCurrentUser();
}
