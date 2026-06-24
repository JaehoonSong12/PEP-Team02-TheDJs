package com.revature.todomanagement.exception;

/**
 * Thrown when login credentials are invalid.
 * The message is intentionally generic to prevent username enumeration.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}
