package com.revature.todomanagement.service;

import com.revature.todomanagement.entity.User;
import com.revature.todomanagement.exception.InvalidCredentialsException;
import com.revature.todomanagement.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserService userService;

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("valid credentials returns stored User entity")
        void login_validCredentials_returnsStoredUserEntity() {
            UUID userId = UUID.randomUUID();
            User storedUser = new User(userId, "testuser", "Password1!");
            User credentials = new User(null, "testuser", "Password1!");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(storedUser));

            User result = userService.login(credentials);

            assertSame(storedUser, result);
        }

        @Test
        @DisplayName("null username throws InvalidCredentialsException")
        void login_nullUsername_throwsInvalidCredentialsException() {
            User credentials = new User(null, null, "Password1!");

            assertThrows(InvalidCredentialsException.class, () -> userService.login(credentials));
            verify(userRepository, never()).findByUsername(any());
        }

        @Test
        @DisplayName("blank username throws InvalidCredentialsException")
        void login_blankUsername_throwsInvalidCredentialsException() {
            User credentials = new User(null, "   ", "Password1!");

            assertThrows(InvalidCredentialsException.class, () -> userService.login(credentials));
            verify(userRepository, never()).findByUsername(any());
        }

        @Test
        @DisplayName("null password throws InvalidCredentialsException")
        void login_nullPassword_throwsInvalidCredentialsException() {
            User credentials = new User(null, "testuser", null);

            assertThrows(InvalidCredentialsException.class, () -> userService.login(credentials));
        }

        @Test
        @DisplayName("blank password throws InvalidCredentialsException")
        void login_blankPassword_throwsInvalidCredentialsException() {
            User credentials = new User(null, "testuser", "   ");

            assertThrows(InvalidCredentialsException.class, () -> userService.login(credentials));
        }

        @Test
        @DisplayName("username not found throws InvalidCredentialsException")
        void login_usernameNotFound_throwsInvalidCredentialsException() {
            User credentials = new User(null, "nonexistent", "Password1!");

            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThrows(InvalidCredentialsException.class, () -> userService.login(credentials));
        }

        @Test
        @DisplayName("password mismatch throws InvalidCredentialsException")
        void login_passwordMismatch_throwsInvalidCredentialsException() {
            UUID userId = UUID.randomUUID();
            User storedUser = new User(userId, "testuser", "CorrectPass1!");
            User credentials = new User(null, "testuser", "WrongPass1!");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(storedUser));

            assertThrows(InvalidCredentialsException.class, () -> userService.login(credentials));
        }

        @Test
        @DisplayName("valid credentials calls findByUsername exactly once")
        void login_validCredentials_callsFindByUsernameExactlyOnce() {
            UUID userId = UUID.randomUUID();
            User storedUser = new User(userId, "testuser", "Password1!");
            User credentials = new User(null, "testuser", "Password1!");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(storedUser));

            userService.login(credentials);

            verify(userRepository, times(1)).findByUsername("testuser");
        }
    }
}
