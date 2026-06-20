package com.revature.todomanagement.exception;

import java.util.UUID;

public class SubtaskNotFoundException extends RuntimeException {

    public SubtaskNotFoundException(UUID subtaskId) {
        super("Subtask not found: " + subtaskId);
    }
}
