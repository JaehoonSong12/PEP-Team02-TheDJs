package com.revature.todomanagement.service;

import com.revature.todomanagement.entity.Subtask;
import com.revature.todomanagement.entity.Task;
import com.revature.todomanagement.exception.SubtaskNotFoundException;
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
public class SubtaskService {

    private final SubtaskRepository subtaskRepository;
    private final TaskRepository taskRepository;

    // ------------------------------------------------------------------ //
    //  Create                                                              //
    // ------------------------------------------------------------------ //

    /**
     * Creates a new subtask under the given parent task, verifying that the
     * parent task exists and belongs to the requesting user before persisting.
     *
     * @param userId  the authenticated user's ID
     * @param taskId  the parent task's UUID
     * @param subtask subtask to persist (id should be null; taskId will be set here)
     * @return the persisted subtask
     * @throws TaskNotFoundException    if no task with {@code taskId} exists
     * @throws TaskOwnershipException   if the task belongs to a different user
     * @throws IllegalArgumentException if the title is blank
     */
    public Subtask createSubtask(UUID userId, UUID taskId, Subtask subtask) {
        Task task = verifyTaskOwnership(userId, taskId);

        if (Boolean.TRUE.equals(task.getCompleted()))
            throw new IllegalArgumentException("Cannot add subtasks to a completed task.");

        if (subtask.getTitle() == null || subtask.getTitle().isBlank())
            throw new IllegalArgumentException("Subtask title must not be blank.");

        subtask.setId(null);
        subtask.setTaskId(taskId);

        return subtaskRepository.save(subtask);
    }

    // ------------------------------------------------------------------ //
    //  Retrieve                                                            //
    // ------------------------------------------------------------------ //

    /**
     * Returns all subtasks for the given parent task, verifying ownership first.
     *
     * @param userId the authenticated user's ID
     * @param taskId the parent task's UUID
     * @return list of subtasks belonging to the task (may be empty)
     * @throws TaskNotFoundException  if no task with {@code taskId} exists
     * @throws TaskOwnershipException if the task belongs to a different user
     */
    public List<Subtask> getSubtasksForTask(UUID userId, UUID taskId) {
        verifyTaskOwnership(userId, taskId);
        return subtaskRepository.findAllByTaskId(taskId);
    }

    /**
     * Returns a single subtask by ID, verifying that the parent task belongs
     * to the requesting user.
     *
     * @param userId    the authenticated user's ID
     * @param taskId    the parent task's UUID
     * @param subtaskId the subtask's UUID
     * @return the matching subtask
     * @throws TaskNotFoundException    if no task with {@code taskId} exists
     * @throws TaskOwnershipException   if the task belongs to a different user
     * @throws SubtaskNotFoundException if no subtask with {@code subtaskId} exists under that task
     */
    public Subtask getSubtaskById(UUID userId, UUID taskId, UUID subtaskId) {
        verifyTaskOwnership(userId, taskId);
        return findSubtaskUnderTask(taskId, subtaskId);
    }

    // ------------------------------------------------------------------ //
    //  Update                                                              //
    // ------------------------------------------------------------------ //

    /**
     * Updates a subtask's title and/or completed status.
     *
     * @param userId    the authenticated user's ID
     * @param taskId    the parent task's UUID
     * @param subtaskId the subtask's UUID
     * @param updates   subtask object carrying the fields to update
     * @return the updated subtask
     * @throws TaskNotFoundException    if no task with {@code taskId} exists
     * @throws TaskOwnershipException   if the task belongs to a different user
     * @throws SubtaskNotFoundException if no subtask with {@code subtaskId} exists under that task
     * @throws IllegalArgumentException if an explicit blank title is supplied
     */
    public Subtask updateSubtask(UUID userId, UUID taskId, UUID subtaskId, Subtask updates) {
        Task task = verifyTaskOwnership(userId, taskId);

        if (Boolean.TRUE.equals(task.getCompleted()))
            throw new IllegalArgumentException("Cannot edit subtasks of a completed task.");

        Subtask existing = findSubtaskUnderTask(taskId, subtaskId);

        if (updates.getTitle() != null) {
            if (updates.getTitle().isBlank())
                throw new IllegalArgumentException("Subtask title must not be blank.");
            existing.setTitle(updates.getTitle());
        }

        existing.setCompleted(updates.isCompleted());

        return subtaskRepository.save(existing);
    }

    // ------------------------------------------------------------------ //
    //  Delete                                                              //
    // ------------------------------------------------------------------ //

    /**
     * Deletes a subtask by ID after verifying that the parent task belongs
     * to the requesting user.
     *
     * @param userId    the authenticated user's ID
     * @param taskId    the parent task's UUID
     * @param subtaskId the subtask's UUID
     * @throws TaskNotFoundException    if no task with {@code taskId} exists
     * @throws TaskOwnershipException   if the task belongs to a different user
     * @throws SubtaskNotFoundException if no subtask with {@code subtaskId} exists under that task
     */
    public void deleteSubtask(UUID userId, UUID taskId, UUID subtaskId) {
        verifyTaskOwnership(userId, taskId);
        Subtask subtask = findSubtaskUnderTask(taskId, subtaskId);
        subtaskRepository.delete(subtask);
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                             //
    // ------------------------------------------------------------------ //

    /**
     * Verifies that the task exists and is owned by the given user.
     * Centralises the not-found / ownership check required before any subtask operation.
     */
    private Task verifyTaskOwnership(UUID userId, UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        if (!task.getUserId().equals(userId))
            throw new TaskOwnershipException(taskId, userId);

        return task;
    }

    /**
     * Loads a subtask and confirms it belongs to the given parent task.
     * Throws {@link SubtaskNotFoundException} if the subtask doesn't exist or
     * is not associated with {@code taskId}.
     */
    private Subtask findSubtaskUnderTask(UUID taskId, UUID subtaskId) {
        Subtask subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new SubtaskNotFoundException(subtaskId));

        if (!subtask.getTaskId().equals(taskId))
            throw new SubtaskNotFoundException(subtaskId);

        return subtask;
    }
}
