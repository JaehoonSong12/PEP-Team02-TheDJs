package com.revature.todomanagement.service;

import com.revature.todomanagement.dto.LoginRequest;
import com.revature.todomanagement.dto.LoginResponse;
import com.revature.todomanagement.dto.RegisterRequest;
import com.revature.todomanagement.dto.RegisterResponse;
import com.revature.todomanagement.entity.User;
import com.revature.todomanagement.exception.DuplicateUsernameException;
import com.revature.todomanagement.exception.InvalidCredentialsException;
import com.revature.todomanagement.repository.UserRepository;
import com.revature.todomanagement.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String PASSWORD_REGEX =
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*])\\S{8,}$";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public RegisterResponse register(RegisterRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Username must not be blank.");

        if (password == null || !password.matches(PASSWORD_REGEX))
            throw new IllegalArgumentException(
                "Password must be 8+ characters with uppercase, lowercase, digit, and special character (!@#$%^&*), and no whitespace.");

        if (userRepository.existsByUsername(username))
            throw new DuplicateUsernameException(username);

        User saved = userRepository.save(new User(null, username, passwordEncoder.encode(password)));
        return new RegisterResponse(saved.getId(), saved.getUsername());
    }

    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Username must not be blank.");

        if (password == null || password.isBlank())
            throw new IllegalArgumentException("Password must not be blank.");

        User user = userRepository.findByUsername(username)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(password, user.getPassword()))
            throw new InvalidCredentialsException();

        return new LoginResponse(user.getId(), user.getUsername(), jwtUtil.generateToken(user));
    }
}
