package com.revature.todomanagement.controller;

import com.revature.todomanagement.entity.Task;
import com.revature.todomanagement.exception.TaskNotFoundException;
import com.revature.todomanagement.exception.TaskOwnershipException;
import com.revature.todomanagement.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TaskService taskService;

    // ------------------------------------------------------------------ //
    //  Endpoints                                                           //
    // ------------------------------------------------------------------ //

    @PostMapping
    public ResponseEntity<Task> createTask(@RequestAttribute UUID userId,
                                           @RequestBody Task task) {
        Task savedTask = taskService.createTask(userId, task);
        return ResponseEntity.ok(savedTask);
    }

    @GetMapping
    public ResponseEntity<List<Task>> getTasks(@RequestAttribute UUID userId) {
        List<Task> tasks = taskService.getTasksForUser(userId);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@RequestAttribute UUID userId,
                                            @PathVariable UUID id) {
        Task task = taskService.getTaskById(userId, id);
        return ResponseEntity.ok(task);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@RequestAttribute UUID userId,
                                           @PathVariable UUID id,
                                           @RequestBody Task updates) {
        Task updatedTask = taskService.updateTask(userId, id, updates);
        return ResponseEntity.ok(updatedTask);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@RequestAttribute UUID userId,
                                           @PathVariable UUID id) {
        taskService.deleteTask(userId, id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ //
    //  Exception Handlers                                                  //
    // ------------------------------------------------------------------ //

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTaskNotFound(TaskNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(Map.of("status", 404, "message", ex.getMessage()));
    }

    @ExceptionHandler(TaskOwnershipException.class)
    public ResponseEntity<Map<String, Object>> handleTaskOwnership(TaskOwnershipException ex) {
        return ResponseEntity.status(403)
                .body(Map.of("status", 403, "message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(400)
                .body(Map.of("status", 400, "message", ex.getMessage()));
    }
}
