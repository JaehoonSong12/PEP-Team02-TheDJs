package com.revature.todomanagement.service;

import com.revature.todomanagement.entity.User;
import com.revature.todomanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Legacy user service — retained for backward compatibility.
 * Registration logic has moved to {@link RegistrationService}.
 * Login/authentication will be implemented in a future feature.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
