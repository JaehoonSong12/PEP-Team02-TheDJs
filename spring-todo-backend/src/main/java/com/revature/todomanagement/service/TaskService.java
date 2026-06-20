package com.revature.todomanagement.service;

import com.revature.todomanagement.dto.TaskRequest;
import com.revature.todomanagement.dto.TaskResponse;
import com.revature.todomanagement.entity.Task;
import com.revature.todomanagement.exception.TaskNotFoundException;
import com.revature.todomanagement.exception.TaskOwnershipException;
import com.revature.todomanagement.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    // ------------------------------------------------------------------ //
    //  Create                                                              //
    // ------------------------------------------------------------------ //

    /**
     * Creates a new task owned by the given user.
     *
     * @param userId  the authenticated user's ID
     * @param request payload containing the task title (and optional completed flag)
     * @return the persisted task as a response DTO
     * @throws IllegalArgumentException if the title is blank
     */
    public TaskResponse createTask(UUID userId, TaskRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank())
            throw new IllegalArgumentException("Task title must not be blank.");

        boolean completed = request.getCompleted() != null && request.getCompleted();

        Task saved = taskRepository.save(new Task(null, userId, request.getTitle(), completed));
        return toResponse(saved);
    }

    // ------------------------------------------------------------------ //
    //  Retrieve                                                            //
    // ------------------------------------------------------------------ //

    /**
     * Returns all tasks belonging to the given user.
     *
     * @param userId the authenticated user's ID
     * @return list of the user's tasks (may be empty)
     */
    public List<TaskResponse> getTasksForUser(UUID userId) {
        return taskRepository.findAllByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns a single task by ID, enforcing that it belongs to the requesting user.
     *
     * @param userId the authenticated user's ID
     * @param taskId the task's UUID
     * @return the matching task as a response DTO
     * @throws TaskNotFoundException   if no task with that ID exists
     * @throws TaskOwnershipException  if the task exists but belongs to a different user
     */
    public TaskResponse getTaskById(UUID userId, UUID taskId) {
        Task task = findAndVerifyOwnership(userId, taskId);
        return toResponse(task);
    }

    // ------------------------------------------------------------------ //
    //  Update                                                              //
    // ------------------------------------------------------------------ //

    /**
     * Updates a task's title and/or completed status.
     * Only fields that are non-null in the request are applied.
     *
     * @param userId  the authenticated user's ID
     * @param taskId  the task's UUID
     * @param request payload with the fields to update
     * @return the updated task as a response DTO
     * @throws TaskNotFoundException   if no task with that ID exists
     * @throws TaskOwnershipException  if the task belongs to a different user
     * @throws IllegalArgumentException if an explicit blank title is supplied
     */
    public TaskResponse updateTask(UUID userId, UUID taskId, TaskRequest request) {
        Task task = findAndVerifyOwnership(userId, taskId);

        if (request.getTitle() != null) {
            if (request.getTitle().isBlank())
                throw new IllegalArgumentException("Task title must not be blank.");
            task.setTitle(request.getTitle());
        }

        if (request.getCompleted() != null) {
            task.setCompleted(request.getCompleted());
        }

        return toResponse(taskRepository.save(task));
    }

    // ------------------------------------------------------------------ //
    //  Delete                                                              //
    // ------------------------------------------------------------------ //

    /**
     * Deletes a task by ID after verifying ownership.
     *
     * @param userId the authenticated user's ID
     * @param taskId the task's UUID
     * @throws TaskNotFoundException   if no task with that ID exists
     * @throws TaskOwnershipException  if the task belongs to a different user
     */
    public void deleteTask(UUID userId, UUID taskId) {
        Task task = findAndVerifyOwnership(userId, taskId);
        taskRepository.delete(task);
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                             //
    // ------------------------------------------------------------------ //

    /**
     * Loads a task and asserts that it belongs to the requesting user.
     * Centralises the not-found / ownership check used by every mutating operation.
     */
    private Task findAndVerifyOwnership(UUID userId, UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        if (!task.getUserId().equals(userId))
            throw new TaskOwnershipException(taskId, userId);

        return task;
    }

    /** Maps a {@link Task} entity to a {@link TaskResponse} DTO. */
    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getUserId(),
                task.getTitle(),
                task.isCompleted()
        );
    }
}
