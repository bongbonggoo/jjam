package com.example.festival.auth.controller;

import com.example.festival.auth.dto.LoginRequest;
import com.example.festival.auth.dto.LoginResponse;
import com.example.festival.auth.dto.SignupRequest;
import com.example.festival.auth.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public void signup(@RequestBody SignupRequest request) {
        authService.signup(request);
    }

    // ✅ 로그인 엔드포인트 추가
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }
}