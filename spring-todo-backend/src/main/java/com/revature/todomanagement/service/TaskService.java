package com.revature.todomanagement.service;

import com.revature.todomanagement.entity.Task;
import com.revature.todomanagement.exception.TaskNotFoundException;
import com.revature.todomanagement.exception.TaskOwnershipException;
import com.revature.todomanagement.repository.SubtaskRepository;
import com.revature.todomanagement.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final SubtaskRepository subtaskRepository;

    // ------------------------------------------------------------------ //
    //  Create                                                              //
    // ------------------------------------------------------------------ //

    /**
     * Creates a new task owned by the given user.
     *
     * @param userId the authenticated user's ID
     * @param task   task to persist (id should be null; userId will be set here)
     * @return the persisted task
     * @throws IllegalArgumentException if the title is blank
     */
    public Task createTask(UUID userId, Task task) {
        if (task.getTitle() == null || task.getTitle().isBlank())
            throw new IllegalArgumentException("Task title must not be blank.");

        task.setId(null);
        task.setUserId(userId);

        return taskRepository.save(task);
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
    public List<Task> getTasksForUser(UUID userId) {
        return taskRepository.findAllByUserId(userId);
    }

    /**
     * Returns a single task by ID, enforcing that it belongs to the requesting user.
     *
     * @param userId the authenticated user's ID
     * @param taskId the task's UUID
     * @return the matching task
     * @throws TaskNotFoundException  if no task with that ID exists
     * @throws TaskOwnershipException if the task belongs to a different user
     */
    public Task getTaskById(UUID userId, UUID taskId) {
        return findAndVerifyOwnership(userId, taskId);
    }

    // ------------------------------------------------------------------ //
    //  Update                                                              //
    // ------------------------------------------------------------------ //

    /**
     * Updates a task's title and/or completed status.
     * Only non-null / meaningful fields from the incoming task are applied.
     *
     * @param userId  the authenticated user's ID
     * @param taskId  the task's UUID
     * @param updates task object carrying the fields to update
     * @return the updated task
     * @throws TaskNotFoundException    if no task with that ID exists
     * @throws TaskOwnershipException   if the task belongs to a different user
     * @throws IllegalArgumentException if an explicit blank title is supplied
     */
    public Task updateTask(UUID userId, UUID taskId, Task updates) {
        Task existing = findAndVerifyOwnership(userId, taskId);

        if (updates.getTitle() != null) {
            if (updates.getTitle().isBlank())
                throw new IllegalArgumentException("Task title must not be blank.");
            existing.setTitle(updates.getTitle());
        }

        existing.setCompleted(updates.isCompleted());

        return taskRepository.save(existing);
    }

    // ------------------------------------------------------------------ //
    //  Delete                                                              //
    // ------------------------------------------------------------------ //

    /**
     * Deletes a task by ID after verifying ownership.
     * All subtasks belonging to the task are deleted first.
     *
     * @param userId the authenticated user's ID
     * @param taskId the task's UUID
     * @throws TaskNotFoundException  if no task with that ID exists
     * @throws TaskOwnershipException if the task belongs to a different user
     */
    public void deleteTask(UUID userId, UUID taskId) {
        Task task = findAndVerifyOwnership(userId, taskId);
        subtaskRepository.deleteAll(subtaskRepository.findAllByTaskId(taskId));
        taskRepository.delete(task);
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                             //
    // ------------------------------------------------------------------ //

    /**
     * Loads a task and asserts that it belongs to the requesting user.
     * Centralises the not-found / ownership check used by every operation.
     */
    private Task findAndVerifyOwnership(UUID userId, UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        if (!task.getUserId().equals(userId))
            throw new TaskOwnershipException(taskId, userId);

        return task;
    }
}
