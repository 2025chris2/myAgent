package com.tzl.llongagent.controller;

import com.tzl.llongagent.security.dto.AuthResponse;
import com.tzl.llongagent.security.dto.LoginRequest;
import com.tzl.llongagent.security.dto.RegisterRequest;
import com.tzl.llongagent.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        String token = userService.register(request.getUsername(), request.getPassword());
        // 从 token 解析 userId（或改为 register 返回 userId）
        // 这里简化：注册成功返回 token，userId 前端从 JWT 中解码或调用 /api/auth/me 获取
        return ResponseEntity.ok(new AuthResponse(token, null));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = userService.login(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(new AuthResponse(token, null));
    }
}
