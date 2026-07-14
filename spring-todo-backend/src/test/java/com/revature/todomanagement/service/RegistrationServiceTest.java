package com.revature.todomanagement.service;

import com.revature.todomanagement.entity.User;
import com.revature.todomanagement.exception.RegistrationFailure;
import com.revature.todomanagement.repository.UserRepository;
import com.revature.todomanagement.security.PasswordValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrationService")
class RegistrationServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordValidator passwordValidator;

    @InjectMocks
    RegistrationService registrationService;

    @Nested
    @DisplayName("Username validation")
    class UsernameValidation {

        @Test
        @DisplayName("null username throws RegistrationFailure with blank message")
        void registerUser_nullUsername_throwsRegistrationFailureWithBlank() {
            User user = new User(UUID.randomUUID(), null, "Password1!");

            RegistrationFailure ex = assertThrows(RegistrationFailure.class,
                    () -> registrationService.registerUser(user));

            assertTrue(ex.getMessage().toLowerCase().contains("blank"));
            verifyNoInteractions(passwordValidator);
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("blank username throws RegistrationFailure with blank message")
        void registerUser_blankUsername_throwsRegistrationFailureWithBlank() {
            User user = new User(UUID.randomUUID(), "   ", "Password1!");

            RegistrationFailure ex = assertThrows(RegistrationFailure.class,
                    () -> registrationService.registerUser(user));

            assertTrue(ex.getMessage().toLowerCase().contains("blank"));
            verifyNoInteractions(passwordValidator);
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("short username (<5) throws RegistrationFailure with length message")
        void registerUser_shortUsername_throwsRegistrationFailureWithLengthMessage() {
            User user = new User(UUID.randomUUID(), "abcd", "Password1!");

            RegistrationFailure ex = assertThrows(RegistrationFailure.class,
                    () -> registrationService.registerUser(user));

            assertTrue(ex.getMessage().contains("between 5 and 18"));
            verifyNoInteractions(passwordValidator);
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("long username (>18) throws RegistrationFailure with length message")
        void registerUser_longUsername_throwsRegistrationFailureWithLengthMessage() {
            User user = new User(UUID.randomUUID(), "a".repeat(19), "Password1!");

            RegistrationFailure ex = assertThrows(RegistrationFailure.class,
                    () -> registrationService.registerUser(user));

            assertTrue(ex.getMessage().contains("between 5 and 18"));
            verifyNoInteractions(passwordValidator);
            verifyNoInteractions(userRepository);
        }
    }

    @Nested
    @DisplayName("Password validation")
    class PasswordValidation {

        @Test
        @DisplayName("null password throws RegistrationFailure with blank message")
        void registerUser_validUsernameNullPassword_throwsRegistrationFailureWithBlank() {
            User user = new User(UUID.randomUUID(), "validuser", null);

            RegistrationFailure ex = assertThrows(RegistrationFailure.class,
                    () -> registrationService.registerUser(user));

            assertTrue(ex.getMessage().toLowerCase().contains("blank"));
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("blank password throws RegistrationFailure with blank message")
        void registerUser_validUsernameBlankPassword_throwsRegistrationFailureWithBlank() {
            User user = new User(UUID.randomUUID(), "validuser", "   ");

            RegistrationFailure ex = assertThrows(RegistrationFailure.class,
                    () -> registrationService.registerUser(user));

            assertTrue(ex.getMessage().toLowerCase().contains("blank"));
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("password violations throws RegistrationFailure with all violation messages")
        void registerUser_passwordViolations_throwsRegistrationFailureWithAllViolations() {
            User user = new User(UUID.randomUUID(), "validuser", "weakpass");
            List<String> violations = List.of(
                    "Password must contain at least one uppercase letter.",
                    "Password must contain at least one digit.",
                    "Password must contain at least one special character (!@#$%^&*)."
            );
            when(passwordValidator.getViolations("weakpass")).thenReturn(violations);

            RegistrationFailure ex = assertThrows(RegistrationFailure.class,
                    () -> registrationService.registerUser(user));

            for (String violation : violations) {
                assertTrue(ex.getMessage().contains(violation),
                        "Expected message to contain: " + violation);
            }
            verifyNoInteractions(userRepository);
        }
    }

    @Nested
    @DisplayName("Duplicate checking")
    class DuplicateChecking {

        @Test
        @DisplayName("duplicate username throws RegistrationFailure with 'already taken'")
        void registerUser_duplicateUsername_throwsRegistrationFailureWithAlreadyTaken() {
            User user = new User(UUID.randomUUID(), "validuser", "Password1!");
            when(passwordValidator.getViolations("Password1!")).thenReturn(List.of());
            when(userRepository.existsByUsername("validuser")).thenReturn(true);

            RegistrationFailure ex = assertThrows(RegistrationFailure.class,
                    () -> registrationService.registerUser(user));

            assertTrue(ex.getMessage().toLowerCase().contains("already taken"));
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Persistence")
    class Persistence {

        @Test
        @DisplayName("valid credentials saves user exactly once")
        void registerUser_validCredentials_savesUserExactlyOnce() {
            User user = new User(UUID.randomUUID(), "validuser", "Password1!");
            when(passwordValidator.getViolations("Password1!")).thenReturn(List.of());
            when(userRepository.existsByUsername("validuser")).thenReturn(false);

            registrationService.registerUser(user);

            verify(userRepository, times(1)).save(user);
        }
    }
}
