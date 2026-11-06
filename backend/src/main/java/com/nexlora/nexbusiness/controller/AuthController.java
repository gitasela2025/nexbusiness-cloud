package com.nexlora.nexbusiness.controller;

import com.nexlora.nexbusiness.entity.User;
import com.nexlora.nexbusiness.repository.UserRepository;
import com.nexlora.nexbusiness.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> body) {
        String name = body.getOrDefault("name", "").trim();
        String email = body.getOrDefault("email", "").trim().toLowerCase();
        String password = body.getOrDefault("password", "");

        if (name.isEmpty() || !email.contains("@") || password.length() < 8) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Name, a valid email and a password of 8+ characters are required."));
        }
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Email already registered."));
        }

        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setRole(User.Role.CUSTOMER); // signup can never create an ADMIN
        userRepository.save(u);

        return ResponseEntity.ok(authResponse(u));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "").trim().toLowerCase();
        String password = body.getOrDefault("password", "");

        User u = userRepository.findByEmail(email).orElse(null);
        if (u == null || u.getPasswordHash() == null || !passwordEncoder.matches(password, u.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid email or password."));
        }
        return ResponseEntity.ok(authResponse(u));
    }

    private Map<String, Object> authResponse(User u) {
        return Map.of(
                "token", jwtService.create(u.getId(), u.getEmail(), u.getRole().name()),
                "user", Map.of("id", u.getId(), "name", u.getName(),
                               "email", u.getEmail(), "role", u.getRole().name()));
    }
}
