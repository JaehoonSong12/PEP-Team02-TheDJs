package com.revature.todomanagement.controller;

import com.revature.todomanagement.dto.AuthResponse;
import com.revature.todomanagement.dto.LoginRequest;
import com.revature.todomanagement.dto.UserRegistrationRequest;
import com.revature.todomanagement.entity.User;
import com.revature.todomanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody UserRegistrationRequest request) {
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(request.getPassword())
                .build();
        
        User registeredUser = userService.register(user);
        
        return ResponseEntity.ok(AuthResponse.builder()
                .accountId(registeredUser.getId())
                .username(registeredUser.getUsername())
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        User user = userService.login(request.getUsername(), request.getPassword());
        
        return ResponseEntity.ok(AuthResponse.builder()
                .accountId(user.getId())
                .username(user.getUsername())
                .build());
    }
}
