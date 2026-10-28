package com.aiinterview.controller;

import com.aiinterview.model.LoginRequest;
import com.aiinterview.model.RegisterRequest;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final Map<String, UserAccount> accounts = new ConcurrentHashMap<>();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Passwords do not match."));
        }

        String email = normalizeEmail(request.email());
        UserAccount account = new UserAccount(request.fullName().trim(), passwordEncoder.encode(request.password()));
        if (accounts.putIfAbsent(email, account) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "An account with this email already exists."));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Registration successful."));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest request) {
        UserAccount account = accounts.get(normalizeEmail(request.email()));
        if (account == null || !passwordEncoder.matches(request.password(), account.passwordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Email or password is incorrect."));
        }
        return ResponseEntity.ok(Map.of("message", "Login successful.", "fullName", account.fullName()));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private record UserAccount(String fullName, String passwordHash) {
    }
}
