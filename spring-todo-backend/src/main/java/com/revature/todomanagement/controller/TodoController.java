package com.revature.todomanagement.controller;

import com.revature.todomanagement.dto.TodoResponse;
import com.revature.todomanagement.entity.Subtask;
import com.revature.todomanagement.entity.Task;
import com.revature.todomanagement.entity.User;
import com.revature.todomanagement.service.SubtaskService;
import com.revature.todomanagement.service.TaskService;
import com.revature.todomanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {
    private final TaskService taskService;
    private final SubtaskService subtaskService;
    private final UserService userService;

    // Helper to get authenticated user (Mocking for now as per phase 1 focus)
    private User getAuthenticatedUser() {
        // In a real app, this would come from SecurityContext
        // For testing/initial setup, we might need a way to pass user id
        return userService.findById(1L); 
    }

    @GetMapping
    public ResponseEntity<List<TodoResponse>> getAllTodos() {
        User user = getAuthenticatedUser();
        List<TodoResponse> responses = taskService.getAllTasks(user).stream()
                .map(this::mapToTodoResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<TodoResponse> createTodo(@RequestBody Task task) {
        User user = getAuthenticatedUser();
        Task createdTask = taskService.createTask(task, user);
        return ResponseEntity.ok(mapToTodoResponse(createdTask));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TodoResponse> getTodoById(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        Task task = taskService.getTaskById(id, user);
        return ResponseEntity.ok(mapToTodoResponse(task));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TodoResponse> updateTodo(@PathVariable Long id, @RequestBody Task task) {
        User user = getAuthenticatedUser();
        Task updatedTask = taskService.updateTask(id, task, user);
        return ResponseEntity.ok(mapToTodoResponse(updatedTask));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTodo(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        taskService.deleteTask(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/subtask")
    public ResponseEntity<Subtask> createSubtask(@PathVariable Long id, @RequestBody Subtask subtask) {
        User user = getAuthenticatedUser();
        Subtask createdSubtask = subtaskService.createSubtask(id, subtask, user);
        return ResponseEntity.ok(createdSubtask);
    }

    @PutMapping("/{id}/{subtaskId}")
    public ResponseEntity<Subtask> updateSubtask(@PathVariable Long id, @PathVariable Long subtaskId, @RequestBody Subtask subtask) {
        User user = getAuthenticatedUser();
        Subtask updatedSubtask = subtaskService.updateSubtask(id, subtaskId, subtask, user);
        return ResponseEntity.ok(updatedSubtask);
    }

    @DeleteMapping("/{id}/{subtaskId}")
    public ResponseEntity<Void> deleteSubtask(@PathVariable Long id, @PathVariable Long subtaskId) {
        User user = getAuthenticatedUser();
        subtaskService.deleteSubtask(id, subtaskId, user);
        return ResponseEntity.noContent().build();
    }

    private TodoResponse mapToTodoResponse(Task task) {
        return TodoResponse.builder()
                .todoId(task.getId())
                .accountId(task.getUser().getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .completed(task.isCompleted())
                .build();
    }
}
