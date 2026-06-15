package com.revature.todomanagement.service;

import com.revature.todomanagement.entity.User;
import com.revature.todomanagement.exception.ResourceConflictException;
import com.revature.todomanagement.exception.ResourceNotFoundException;
import com.revature.todomanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing user-related operations such as registration and authentication.
 */
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    /**
     * Registers a new user in the system.
     * Checks for existing username or email before persisting.
     *
     * @param user the user entity containing registration details
     * @return the persisted user entity
     * @throws ResourceConflictException if the username or email is already taken
     */
    @Transactional
    public User register(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new ResourceConflictException("Username already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new ResourceConflictException("Email already exists");
        }
        // TODO: Password hashing
        return userRepository.save(user);
    }

    /**
     * Authenticates a user based on username and password.
     *
     * @param username the username of the user
     * @param password the plaintext password (to be compared against hash)
     * @return the authenticated user entity
     * @throws ResourceNotFoundException if the user does not exist
     * @throws RuntimeException if the password does not match
     */
    public User login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // TODO: Password verification
        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("Invalid credentials");
        }
        return user;
    }

    /**
     * Finds a user by their unique identifier.
     *
     * @param id the unique identifier of the user
     * @return the user entity
     * @throws ResourceNotFoundException if the user is not found
     */
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
