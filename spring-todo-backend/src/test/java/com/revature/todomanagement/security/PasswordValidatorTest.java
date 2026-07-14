package com.revature.todomanagement.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordValidator")
class PasswordValidatorTest {

    PasswordValidator validator = new PasswordValidator();

    @Nested
    @DisplayName("Individual rules")
    class IndividualRules {

        @Test
        @DisplayName("too short password triggers minimum length violation")
        void getViolations_tooShort_returnsMinLengthViolation() {
            // 7 chars: has uppercase, lowercase, digit, special, no whitespace
            String password = "Abcde1!";
            List<String> violations = validator.getViolations(password);

            assertTrue(violations.contains("Password must be at least 8 characters long."));
            assertEquals(1, violations.size());
        }

        @Test
        @DisplayName("too long password triggers maximum length violation")
        void getViolations_tooLong_returnsMaxLengthViolation() {
            // 73 chars: has uppercase, lowercase, digit, special, no whitespace
            String password = "A" + "a".repeat(64) + "1234567!";
            List<String> violations = validator.getViolations(password);

            assertTrue(violations.contains("Password must be no more than 72 characters long."));
            assertEquals(1, violations.size());
        }

        @Test
        @DisplayName("no uppercase letter triggers uppercase violation")
        void getViolations_noUppercase_returnsUppercaseViolation() {
            // 8 chars: has lowercase, digit, special, no whitespace, but no uppercase
            String password = "abcdef1!";
            List<String> violations = validator.getViolations(password);

            assertTrue(violations.contains("Password must contain at least one uppercase letter."));
            assertEquals(1, violations.size());
        }

        @Test
        @DisplayName("no lowercase letter triggers lowercase violation")
        void getViolations_noLowercase_returnsLowercaseViolation() {
            // 8 chars: has uppercase, digit, special, no whitespace, but no lowercase
            String password = "ABCDEF1!";
            List<String> violations = validator.getViolations(password);

            assertTrue(violations.contains("Password must contain at least one lowercase letter."));
            assertEquals(1, violations.size());
        }

        @Test
        @DisplayName("no digit triggers digit violation")
        void getViolations_noDigit_returnsDigitViolation() {
            // 8 chars: has uppercase, lowercase, special, no whitespace, but no digit
            String password = "Abcdefg!";
            List<String> violations = validator.getViolations(password);

            assertTrue(violations.contains("Password must contain at least one digit."));
            assertEquals(1, violations.size());
        }

        @Test
        @DisplayName("no special character triggers special char violation")
        void getViolations_noSpecialChar_returnsSpecialCharViolation() {
            // 8 chars: has uppercase, lowercase, digit, no whitespace, but no special char
            String password = "Abcdef12";
            List<String> violations = validator.getViolations(password);

            assertTrue(violations.contains("Password must contain at least one special character (!@#$%^&*)."));
            assertEquals(1, violations.size());
        }

        @Test
        @DisplayName("whitespace triggers whitespace violation")
        void getViolations_whitespace_returnsWhitespaceViolation() {
            // 9 chars: has uppercase, lowercase, digit, special, but contains whitespace
            String password = "Abcde1! x";
            List<String> violations = validator.getViolations(password);

            assertTrue(violations.contains("Password must not contain whitespace."));
            assertEquals(1, violations.size());
        }
    }

    @Nested
    @DisplayName("All rules pass")
    class AllRulesPass {

        @Test
        @DisplayName("valid password returns empty list")
        void getViolations_validPassword_returnsEmptyList() {
            String password = "Abcdef1!";
            List<String> violations = validator.getViolations(password);

            assertTrue(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("Multiple violations")
    class MultipleViolations {

        @Test
        @DisplayName("multiple rules violated returns correct count")
        void getViolations_multipleViolations_returnsCorrectCount() {
            // "ab" violates: too short, no uppercase, no digit, no special char = 4 violations
            String password = "ab";
            List<String> violations = validator.getViolations(password);

            assertEquals(4, violations.size());
            assertTrue(violations.contains("Password must be at least 8 characters long."));
            assertTrue(violations.contains("Password must contain at least one uppercase letter."));
            assertTrue(violations.contains("Password must contain at least one digit."));
            assertTrue(violations.contains("Password must contain at least one special character (!@#$%^&*)."));
        }
    }

    @Nested
    @DisplayName("Boundary lengths")
    class BoundaryLengths {

        @ParameterizedTest
        @MethodSource("com.revature.todomanagement.security.PasswordValidatorTest#boundaryLengthPasswords")
        @DisplayName("boundary length passwords pass all rules")
        void getViolations_boundaryLength_returnsEmptyList(String password) {
            List<String> violations = validator.getViolations(password);

            assertTrue(violations.isEmpty(),
                    "Expected no violations for password of length " + password.length() + " but got: " + violations);
        }
    }

    static Stream<Arguments> boundaryLengthPasswords() {
        return Stream.of(
                Arguments.of("Abcde1!x"),                          // exactly 8 chars
                Arguments.of("A" + "a".repeat(63) + "1234567!")    // exactly 72 chars
        );
    }
}
