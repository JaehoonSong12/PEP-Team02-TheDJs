package com.revature.todomanagement.exception;

import java.util.UUID;

public class TaskOwnershipException extends RuntimeException {

    public TaskOwnershipException(UUID taskId, UUID userId) {
        super("User " + userId + " does not own task " + taskId);
    }
}
