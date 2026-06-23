package com.revature.todomanagement.security;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates password strength against seven independent rules.
 * Each rule is checked independently and all violations are collected
 * so the client receives all failure reasons in one response.
 */
@Component
public class PasswordValidator {

    /**
     * Combined regex for documentation purposes. A password matching this regex
     * satisfies all seven strength rules simultaneously.
     */
    public static final String PASSWORD_REGEX =
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*])\\S{8,72}$";

    /**
     * Checks the given password against all strength rules and returns a list
     * of violation messages. Returns an empty list when all rules pass.
     * This method never throws.
     *
     * @param password the plaintext password to validate
     * @return a list of violation messages (empty if all rules pass)
     */
    public List<String> getViolations(String password) {
        List<String> violations = new ArrayList<>();

        if (password.length() < 8) {
            violations.add("Password must be at least 8 characters long.");
        }

        if (password.length() > 72) {
            violations.add("Password must be no more than 72 characters long.");
        }

        if (!password.chars().anyMatch(Character::isUpperCase)) {
            violations.add("Password must contain at least one uppercase letter.");
        }

        if (!password.chars().anyMatch(Character::isLowerCase)) {
            violations.add("Password must contain at least one lowercase letter.");
        }

        if (!password.chars().anyMatch(Character::isDigit)) {
            violations.add("Password must contain at least one digit.");
        }

        if (password.chars().noneMatch(c -> "!@#$%^&*".indexOf(c) >= 0)) {
            violations.add("Password must contain at least one special character (!@#$%^&*).");
        }

        if (password.chars().anyMatch(Character::isWhitespace)) {
            violations.add("Password must not contain whitespace.");
        }

        return violations;
    }
}
