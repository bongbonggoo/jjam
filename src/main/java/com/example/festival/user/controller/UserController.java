package com.example.festival.user.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @GetMapping("/user/me")
    public Long me(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
}
