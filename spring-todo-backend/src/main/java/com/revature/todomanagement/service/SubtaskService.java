package com.revature.todomanagement.service;

import com.revature.todomanagement.dto.SubtaskRequest;
import com.revature.todomanagement.dto.SubtaskResponse;
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
     * @param request payload containing the subtask title (and optional completed flag)
     * @return the persisted subtask as a response DTO
     * @throws TaskNotFoundException     if no task with {@code taskId} exists
     * @throws TaskOwnershipException    if the task belongs to a different user
     * @throws IllegalArgumentException  if the title is blank
     */
    public SubtaskResponse createSubtask(UUID userId, UUID taskId, SubtaskRequest request) {
        verifyTaskOwnership(userId, taskId);

        if (request.getTitle() == null || request.getTitle().isBlank())
            throw new IllegalArgumentException("Subtask title must not be blank.");

        boolean completed = request.getCompleted() != null && request.getCompleted();

        Subtask saved = subtaskRepository.save(new Subtask(null, taskId, request.getTitle(), completed));
        return toResponse(saved);
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
     * @throws TaskNotFoundException   if no task with {@code taskId} exists
     * @throws TaskOwnershipException  if the task belongs to a different user
     */
    public List<SubtaskResponse> getSubtasksForTask(UUID userId, UUID taskId) {
        verifyTaskOwnership(userId, taskId);

        return subtaskRepository.findAllByTaskId(taskId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns a single subtask by ID, verifying that the parent task belongs
     * to the requesting user.
     *
     * @param userId    the authenticated user's ID
     * @param taskId    the parent task's UUID
     * @param subtaskId the subtask's UUID
     * @return the matching subtask as a response DTO
     * @throws TaskNotFoundException    if no task with {@code taskId} exists
     * @throws TaskOwnershipException   if the task belongs to a different user
     * @throws SubtaskNotFoundException if no subtask with {@code subtaskId} exists under that task
     */
    public SubtaskResponse getSubtaskById(UUID userId, UUID taskId, UUID subtaskId) {
        verifyTaskOwnership(userId, taskId);
        Subtask subtask = findSubtaskUnderTask(taskId, subtaskId);
        return toResponse(subtask);
    }

    // ------------------------------------------------------------------ //
    //  Update                                                              //
    // ------------------------------------------------------------------ //

    /**
     * Updates a subtask's title and/or completed status.
     * Only non-null fields in the request are applied (partial update).
     *
     * @param userId    the authenticated user's ID
     * @param taskId    the parent task's UUID
     * @param subtaskId the subtask's UUID
     * @param request   payload with fields to update
     * @return the updated subtask as a response DTO
     * @throws TaskNotFoundException     if no task with {@code taskId} exists
     * @throws TaskOwnershipException    if the task belongs to a different user
     * @throws SubtaskNotFoundException  if no subtask with {@code subtaskId} exists under that task
     * @throws IllegalArgumentException  if an explicit blank title is supplied
     */
    public SubtaskResponse updateSubtask(UUID userId, UUID taskId, UUID subtaskId, SubtaskRequest request) {
        verifyTaskOwnership(userId, taskId);
        Subtask subtask = findSubtaskUnderTask(taskId, subtaskId);

        if (request.getTitle() != null) {
            if (request.getTitle().isBlank())
                throw new IllegalArgumentException("Subtask title must not be blank.");
            subtask.setTitle(request.getTitle());
        }

        if (request.getCompleted() != null) {
            subtask.setCompleted(request.getCompleted());
        }

        return toResponse(subtaskRepository.save(subtask));
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
    private void verifyTaskOwnership(UUID userId, UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        if (!task.getUserId().equals(userId))
            throw new TaskOwnershipException(taskId, userId);
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

    /** Maps a {@link Subtask} entity to a {@link SubtaskResponse} DTO. */
    private SubtaskResponse toResponse(Subtask subtask) {
        return new SubtaskResponse(
                subtask.getId(),
                subtask.getTaskId(),
                subtask.getTitle(),
                subtask.isCompleted()
        );
    }
}
