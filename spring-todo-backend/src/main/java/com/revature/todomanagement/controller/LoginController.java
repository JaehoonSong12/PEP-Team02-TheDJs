package com.revature.todomanagement.controller;

import com.revature.todomanagement.entity.User;
import com.revature.todomanagement.exception.InvalidCredentialsException;
import com.revature.todomanagement.security.JwtUtil;
import com.revature.todomanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class LoginController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody User credentials) {
        User user = userService.login(credentials);
        String token = jwtUtil.generateToken(user);
        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + token)
                .build();
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<String> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.TEXT_PLAIN)
                .body(ex.getMessage());
    }
}
