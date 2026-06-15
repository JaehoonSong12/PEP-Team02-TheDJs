package com.revature.todomanagement.service;

import com.revature.todomanagement.entity.Subtask;
import com.revature.todomanagement.entity.Task;
import com.revature.todomanagement.entity.User;
import com.revature.todomanagement.exception.ResourceNotFoundException;
import com.revature.todomanagement.repository.SubtaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing subtask-related operations.
 * Subtasks are scoped within a parent Task.
 */
@Service
@RequiredArgsConstructor
public class SubtaskService {
    private final SubtaskRepository subtaskRepository;
    private final TaskService taskService;

    /**
     * Creates a new subtask for a specific parent task.
     *
     * @param taskId the ID of the parent task
     * @param subtask the subtask details to persist
     * @param user the authenticated user performing the action
     * @return the persisted subtask entity
     * @throws ResourceNotFoundException if the parent task is not found
     */
    @Transactional
    public Subtask createSubtask(Long taskId, Subtask subtask, User user) {
        Task task = taskService.getTaskById(taskId, user);
        subtask.setTask(task);
        return subtaskRepository.save(subtask);
    }

    /**
     * Updates an existing subtask.
     *
     * @param taskId the ID of the parent task
     * @param subtaskId the unique identifier of the subtask
     * @param subtaskDetails the new details to apply
     * @param user the authenticated user performing the update
     * @return the updated subtask entity
     * @throws ResourceNotFoundException if the subtask or task is not found
     * @throws RuntimeException if the subtask does not belong to the specified task
     */
    @Transactional
    public Subtask updateSubtask(Long taskId, Long subtaskId, Subtask subtaskDetails, User user) {
        Task task = taskService.getTaskById(taskId, user);
        Subtask subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new ResourceNotFoundException("Subtask not found"));
        
        if (!subtask.getTask().getId().equals(task.getId())) {
            throw new RuntimeException("Subtask does not belong to the specified task");
        }
        
        subtask.setTitle(subtaskDetails.getTitle());
        subtask.setCompleted(subtaskDetails.isCompleted());
        return subtaskRepository.save(subtask);
    }

    /**
     * Deletes a specific subtask.
     *
     * @param taskId the ID of the parent task
     * @param subtaskId the unique identifier of the subtask
     * @param user the authenticated user performing the deletion
     * @throws ResourceNotFoundException if the subtask or task is not found
     * @throws RuntimeException if the subtask does not belong to the specified task
     */
    @Transactional
    public void deleteSubtask(Long taskId, Long subtaskId, User user) {
        Task task = taskService.getTaskById(taskId, user);
        Subtask subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new ResourceNotFoundException("Subtask not found"));

        if (!subtask.getTask().getId().equals(task.getId())) {
            throw new RuntimeException("Subtask does not belong to the specified task");
        }
        
        subtaskRepository.delete(subtask);
    }
}
