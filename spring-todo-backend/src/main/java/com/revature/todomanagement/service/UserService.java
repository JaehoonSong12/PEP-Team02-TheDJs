package com.revature.todomanagement.service;

import com.revature.todomanagement.entity.User;
import com.revature.todomanagement.exception.InvalidCredentialsException;
import com.revature.todomanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Handles user lookup and authentication.
 * Registration logic lives in {@link RegistrationService}.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Authenticates a user by verifying their username and password.
     * Returns the full User entity on success so the controller can generate a JWT.
     * @throws InvalidCredentialsException if credentials are blank, username not found, or password mismatch
     */
    public User login(User credentials) {
        if (credentials.getUsername() == null || credentials.getUsername().isBlank()) {
            throw new InvalidCredentialsException();
        }
        if (credentials.getPassword() == null || credentials.getPassword().isBlank()) {
            throw new InvalidCredentialsException();
        }

        Optional<User> userOptional = userRepository.findByUsername(credentials.getUsername());
        if (userOptional.isEmpty()) {
            throw new InvalidCredentialsException();
        }

        User user = userOptional.get();
        if (!user.getPassword().equals(credentials.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return user;
    }
}
