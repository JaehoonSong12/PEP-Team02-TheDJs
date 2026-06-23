package com.revature.todomanagement.service;

import com.revature.todomanagement.entity.User;
import com.revature.todomanagement.exception.RegistrationFailure;
import com.revature.todomanagement.repository.UserRepository;
import com.revature.todomanagement.security.PasswordValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Contains all business logic for user registration.
 * Validates username and password fields in a defined order,
 * checks for duplicate usernames, and persists the new user.
 */
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordValidator passwordValidator;

    /**
     * Registers a new user by applying validations in strict order and persisting on success.
     *
     * <p>Validation order:
     * <ol>
     *   <li>Username not blank</li>
     *   <li>Username length 5–18 characters</li>
     *   <li>Password not blank</li>
     *   <li>Password satisfies all strength rules</li>
     *   <li>Username not already taken (database check)</li>
     *   <li>Persist user</li>
     *   <li>Return void</li>
     * </ol>
     *
     * <p>Steps 1–4 (field validation) complete before any {@code UserRepository} call.
     *
     * @param user the user to register
     * @throws RegistrationFailure if any validation fails
     */
    public void registerUser(User user) {
        // Step 1: Validate username not blank
        String username = user.getUsername();
        if (username == null || username.isBlank()) {
            throw new RegistrationFailure("Username must not be blank.");
        }

        // Step 2: Validate username length 5–18
        if (username.length() < 5 || username.length() > 18) {
            throw new RegistrationFailure("Username must be between 5 and 18 characters.");
        }

        // Step 3: Validate password not blank
        String password = user.getPassword();
        if (password == null || password.isBlank()) {
            throw new RegistrationFailure("Password must not be blank.");
        }

        // Step 4: Validate password satisfies all strength rules
        List<String> violations = passwordValidator.getViolations(password);
        if (!violations.isEmpty()) {
            throw new RegistrationFailure(String.join("\n", violations));
        }

        // Step 5: Check duplicate username
        if (userRepository.existsByUsername(username)) {
            throw new RegistrationFailure("Username '" + username + "' is already taken.");
        }

        // Step 6: Persist
        userRepository.save(user);

        // Step 7: Return (void)
    }
}
