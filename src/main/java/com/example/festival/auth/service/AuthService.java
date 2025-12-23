package com.example.festival.auth.service;

import com.example.festival.auth.dto.LoginRequest;
import com.example.festival.auth.dto.LoginResponse;
import com.example.festival.auth.dto.SignupRequest;
import com.example.festival.global.security.JwtProvider;
import com.example.festival.user.entity.User;
import com.example.festival.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    public AuthService(UserRepository userRepository, JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
    }

    public void signup(SignupRequest request) {
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword()); // (나중에 암호화)
        user.setNickname(request.getNickname());

        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("이메일 없음"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("비밀번호 틀림");
        }

        // ✅ 여기 수정됨
        String token = jwtProvider.createToken(user.getId());

        return new LoginResponse(token);
    }
}
