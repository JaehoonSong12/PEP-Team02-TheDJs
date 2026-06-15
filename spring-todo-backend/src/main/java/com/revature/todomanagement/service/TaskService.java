package com.revature.todomanagement.service;

import com.revature.todomanagement.entity.Task;
import com.revature.todomanagement.entity.User;
import com.revature.todomanagement.exception.ResourceNotFoundException;
import com.revature.todomanagement.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing task-related operations.
 * Enforces ownership to ensure users only access their own todos.
 */
@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;

    /**
     * Retrieves all tasks belonging to a specific user.
     *
     * @param user the authenticated user
     * @return a list of tasks owned by the user
     */
    public List<Task> getAllTasks(User user) {
        return taskRepository.findByUser(user);
    }

    /**
     * Retrieves a single task by ID and verifies ownership.
     *
     * @param id the unique identifier of the task
     * @param user the authenticated user attempting the access
     * @return the task entity
     * @throws ResourceNotFoundException if the task is not found
     * @throws RuntimeException if the user does not own the task
     */
    public Task getTaskById(Long id, User user) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        
        if (!task.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to task");
        }
        return task;
    }

    /**
     * Creates a new task for the specified user.
     *
     * @param task the task details to persist
     * @param user the user who will own the task
     * @return the persisted task entity
     */
    @Transactional
    public Task createTask(Task task, User user) {
        task.setUser(user);
        return taskRepository.save(task);
    }

    /**
     * Updates an existing task's details.
     *
     * @param id the unique identifier of the task to update
     * @param taskDetails the new details to apply
     * @param user the authenticated user performing the update
     * @return the updated task entity
     * @throws ResourceNotFoundException if the task is not found
     */
    @Transactional
    public Task updateTask(Long id, Task taskDetails, User user) {
        Task task = getTaskById(id, user);
        task.setTitle(taskDetails.getTitle());
        task.setDescription(taskDetails.getDescription());
        task.setCompleted(taskDetails.isCompleted());
        return taskRepository.save(task);
    }

    /**
     * Deletes a task and its associated subtasks.
     *
     * @param id the unique identifier of the task to delete
     * @param user the authenticated user performing the deletion
     * @throws ResourceNotFoundException if the task is not found
     */
    @Transactional
    public void deleteTask(Long id, User user) {
        Task task = getTaskById(id, user);
        taskRepository.delete(task);
    }
}
