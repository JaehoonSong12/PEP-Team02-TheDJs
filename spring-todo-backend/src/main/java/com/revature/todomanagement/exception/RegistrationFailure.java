package com.revature.todomanagement.exception;

/**
 * Custom runtime exception for all registration validation failures.
 * Thrown for: blank username, bad username length, bad password, and duplicate username.
 */
public class RegistrationFailure extends RuntimeException {

    public RegistrationFailure(String message) {
        super(message);
    }
}
